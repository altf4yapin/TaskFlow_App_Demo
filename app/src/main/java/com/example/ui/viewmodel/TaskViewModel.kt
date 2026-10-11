package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import com.example.data.security.SecurityBackupManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TaskFilterTab(val title: String) {
    ALL("Tümü"),
    PENDING("Bekleyen"),
    TODAY("Bugün"),
    SHARED("Paylaşılanlar"),
    COMPLETED("Tamamlanan")
}

enum class TaskSortOrder(val title: String) {
    PRIORITY_DESC("Öncelik (En Yüksek)"),
    DUE_DATE_ASC("Bitiş Tarihi (En Yakın)"),
    CREATED_DESC("Eklenme Tarihi (Yeni)"),
    TITLE_ASC("Alfabetik (A-Z)")
}

data class DayCompletion(
    val dayLabel: String,
    val count: Int
)

data class TaskStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val urgentPendingCount: Int = 0,
    val completionRate: Float = 0f,
    val productivityScore: Int = 0,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val priorityBreakdown: Map<TaskPriority, Int> = emptyMap(),
    val weeklyHistory: List<DayCompletion> = emptyList()
)

data class TaskUiState(
    val selectedTab: TaskFilterTab = TaskFilterTab.ALL,
    val selectedCategory: String? = null,
    val selectedPriority: TaskPriority? = null,
    val searchQuery: String = "",
    val sortOrder: TaskSortOrder = TaskSortOrder.PRIORITY_DESC,
    val isDarkMode: Boolean? = null, // null = system, true = dark, false = light
    val isAutoBackupEnabled: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(
        TaskUiState(isAutoBackupEnabled = SecurityBackupManager.isAutoBackupEnabled(context))
    )
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    val rawTasks: StateFlow<List<TaskItem>> = repository.observeAllAccessibleTasks()
        .catch { error ->
            _uiState.update { it.copy(errorMessage = error.localizedMessage ?: "Veri senkronizasyonu hatası") }
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val customCategories: StateFlow<List<CategoryItem>> = repository.observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Filtered and sorted tasks flow
    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        rawTasks,
        _uiState
    ) { tasks, state ->
        filterAndSortTasks(tasks, state)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Statistics flow
    val stats: StateFlow<TaskStats> = rawTasks.combine(_uiState) { tasks, _ ->
        computeStats(tasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TaskStats()
    )

    init {
        // Sync current user profile to Firestore
        val user = auth.currentUser
        if (user != null) {
            viewModelScope.launch {
                repository.syncUserProfile(
                    email = user.email ?: "",
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Kullanıcı",
                    photoUrl = user.photoUrl?.toString()
                )
            }
        }
    }

    private fun filterAndSortTasks(tasks: List<TaskItem>, state: TaskUiState): List<TaskItem> {
        val currentUid = auth.currentUser?.uid ?: ""
        val nowSeconds = System.currentTimeMillis() / 1000

        var list = tasks

        // Tab filter
        list = when (state.selectedTab) {
            TaskFilterTab.ALL -> list
            TaskFilterTab.PENDING -> list.filter { !it.completed }
            TaskFilterTab.COMPLETED -> list.filter { it.completed }
            TaskFilterTab.SHARED -> list.filter {
                it.sharedWithUids.contains(currentUid) || it.sharedWithEmails.isNotEmpty()
            }
            TaskFilterTab.TODAY -> list.filter { task ->
                val due = task.dueDate?.seconds ?: return@filter false
                val calNow = Calendar.getInstance()
                val calDue = Calendar.getInstance().apply { timeInMillis = due * 1000 }
                calNow.get(Calendar.YEAR) == calDue.get(Calendar.YEAR) &&
                        calNow.get(Calendar.DAY_OF_YEAR) == calDue.get(Calendar.DAY_OF_YEAR)
            }
        }

        // Category filter
        if (!state.selectedCategory.isNullOrBlank()) {
            list = list.filter { it.category.equals(state.selectedCategory, ignoreCase = true) }
        }

        // Priority filter
        if (state.selectedPriority != null) {
            list = list.filter { it.priority.equals(state.selectedPriority.name, ignoreCase = true) }
        }

        // Search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase(Locale.getDefault())
            list = list.filter {
                it.title.lowercase(Locale.getDefault()).contains(query) ||
                        it.description.lowercase(Locale.getDefault()).contains(query) ||
                        it.category.lowercase(Locale.getDefault()).contains(query)
            }
        }

        // Sorting
        return when (state.sortOrder) {
            TaskSortOrder.PRIORITY_DESC -> list.sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenByDescending { TaskPriority.fromString(it.priority).level }
                    .thenBy { it.dueDate?.seconds ?: Long.MAX_VALUE }
            )
            TaskSortOrder.DUE_DATE_ASC -> list.sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenBy { it.dueDate?.seconds ?: Long.MAX_VALUE }
            )
            TaskSortOrder.CREATED_DESC -> list.sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
            TaskSortOrder.TITLE_ASC -> list.sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenBy { it.title.lowercase(Locale.getDefault()) }
            )
        }
    }

    private fun computeStats(tasks: List<TaskItem>): TaskStats {
        val total = tasks.size
        val completed = tasks.count { it.completed }
        val pending = total - completed
        val urgentPending = tasks.count { !it.completed && it.priority == "URGENT" }
        val rate = if (total > 0) (completed.toFloat() / total.toFloat()) else 0f
        val productivityScore = ((rate * 70f) + if (urgentPending == 0 && total > 0) 30f else 0f).toInt().coerceIn(0, 100)

        val catMap = mutableMapOf<String, Int>()
        val prioMap = mutableMapOf<TaskPriority, Int>()
        for (p in TaskPriority.entries) {
            prioMap[p] = 0
        }

        for (task in tasks) {
            catMap[task.category] = (catMap[task.category] ?: 0) + 1
            val prio = TaskPriority.fromString(task.priority)
            prioMap[prio] = (prioMap[prio] ?: 0) + 1
        }

        // Last 7 days history
        val dayFormat = SimpleDateFormat("EEE", Locale("tr"))
        val history = mutableListOf<DayCompletion>()
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val targetCal = Calendar.getInstance()
            targetCal.add(Calendar.DAY_OF_YEAR, -i)
            val label = dayFormat.format(targetCal.time)

            val countOnDay = tasks.count { task ->
                if (!task.completed || task.completedAt == null) return@count false
                val taskCal = Calendar.getInstance().apply { timeInMillis = task.completedAt.seconds * 1000 }
                taskCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                        taskCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
            }
            history.add(DayCompletion(label, countOnDay))
        }

        return TaskStats(
            totalCount = total,
            completedCount = completed,
            pendingCount = pending,
            urgentPendingCount = urgentPending,
            completionRate = rate,
            productivityScore = productivityScore,
            categoryBreakdown = catMap,
            priorityBreakdown = prioMap,
            weeklyHistory = history
        )
    }

    fun setFilterTab(tab: TaskFilterTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setCategoryFilter(category: String?) {
        _uiState.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _uiState.update { it.copy(selectedPriority = if (it.selectedPriority == priority) null else priority) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOrder(order: TaskSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun toggleDarkMode(enable: Boolean?) {
        _uiState.update { it.copy(isDarkMode = enable) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun createTask(
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Date?,
        reminderMinutesBefore: Int,
        collaboratorEmails: List<String>
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Görev başlığı boş bırakılamaz.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val timestamp = dueDate?.let { Timestamp(it) }
            val result = repository.createTask(
                title = title,
                description = description,
                category = category,
                priority = priority.name,
                dueDate = timestamp,
                reminderMinutesBefore = reminderMinutesBefore,
                sharedWithEmails = collaboratorEmails
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Görev başarıyla eklendi!" else null,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun updateTask(
        taskId: String,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Date?,
        reminderMinutesBefore: Int
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val timestamp = dueDate?.let { Timestamp(it) }
            val result = repository.updateTask(
                taskId = taskId,
                title = title,
                description = description,
                category = category,
                priority = priority.name,
                dueDate = timestamp,
                reminderMinutesBefore = reminderMinutesBefore
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Görev güncellendi" else null,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun toggleTaskComplete(task: TaskItem) {
        viewModelScope.launch {
            val newStatus = !task.completed
            repository.toggleTaskComplete(task.id, newStatus)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.deleteTask(taskId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "Görev silindi" else null,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun addCollaborator(taskId: String, email: String) {
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Lütfen geçerli bir e-posta adresi girin.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.addCollaborator(taskId, email)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    successMessage = if (result.isSuccess) "$email ile paylaşıldı" else null,
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun createCategory(name: String, colorHex: String, iconName: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCategory(name, colorHex, iconName)
        }
    }

    fun exportEncryptedBackup(passphrase: String): String? {
        if (passphrase.length < 4) {
            _uiState.update { it.copy(errorMessage = "Şifre en az 4 karakter olmalıdır.") }
            return null
        }
        return try {
            val json = SecurityBackupManager.serializeTasks(rawTasks.value)
            val encrypted = SecurityBackupManager.encryptData(json, passphrase)
            SecurityBackupManager.recordBackupTime(context)
            _uiState.update { it.copy(successMessage = "Uçtan uca şifreli yedek oluşturuldu.") }
            encrypted
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "Yedekleme hatası: ${e.message}") }
            null
        }
    }

    fun restoreEncryptedBackup(encryptedData: String, passphrase: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val decryptedJson = SecurityBackupManager.decryptData(encryptedData.trim(), passphrase)
                val root = JSONObject(decryptedJson)
                val array = root.getJSONArray("tasks")
                var restoredCount = 0

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val description = obj.optString("description", "")
                    val category = obj.optString("category", "Genel")
                    val priority = obj.optString("priority", "MEDIUM")
                    val dueSeconds = obj.optLong("dueDateSeconds", -1L)
                    val dueDate = if (dueSeconds > 0) Timestamp(Date(dueSeconds * 1000)) else null

                    if (title.isNotBlank()) {
                        repository.createTask(
                            title = title,
                            description = description,
                            category = category,
                            priority = priority,
                            dueDate = dueDate
                        )
                        restoredCount++
                    }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "$restoredCount görev başarıyla geri yüklendi!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Şifre çözme hatası! Parolanızı kontrol edin."
                    )
                }
            }
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        SecurityBackupManager.setAutoBackupEnabled(context, enabled)
        _uiState.update { it.copy(isAutoBackupEnabled = enabled) }
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
