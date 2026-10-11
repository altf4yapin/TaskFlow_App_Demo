package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.model.TaskItem
import com.example.ui.auth.signOut
import com.example.ui.components.AddCategoryDialog
import com.example.ui.components.AddEditTaskDialog
import com.example.ui.components.AnalyticsDashboard
import com.example.ui.components.EncryptedBackupDialog
import com.example.ui.components.FilterSortBar
import com.example.ui.components.ShareTaskDialog
import com.example.ui.components.TaskCard
import com.example.ui.viewmodel.TaskFilterTab
import com.example.ui.viewmodel.TaskViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class MainNavDestination(val title: String, val icon: ImageVector) {
    TASKS("Görevler", Icons.Default.ListAlt),
    SHARED("Paylaşılanlar", Icons.Default.Group),
    ANALYTICS("İstatistik", Icons.Default.BarChart),
    SETTINGS("Ayarlar", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: TaskViewModel,
    onSignOutRequested: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var currentNav by remember { mutableStateOf(MainNavDestination.TASKS) }

    // Dialog States
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }
    var taskToShare by remember { mutableStateOf<TaskItem?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    // Show error/success in snackbar
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TaskFlow",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            // Cloud Sync badge
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Bulut Senkronize",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Bulut Aktif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    },
                    actions = {
                        // Dark mode toggle icon
                        IconButton(
                            onClick = {
                                val nextMode = when (uiState.isDarkMode) {
                                    null -> true
                                    true -> false
                                    false -> null
                                }
                                viewModel.toggleDarkMode(nextMode)
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkMode == false) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Tema Değiştir",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Encrypted Backup Action
                        IconButton(onClick = { showBackupDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Şifreli Yedek",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        MainNavDestination.entries.forEach { dest ->
                            NavigationBarItem(
                                selected = currentNav == dest,
                                onClick = {
                                    currentNav = dest
                                    if (dest == MainNavDestination.SHARED) {
                                        viewModel.setFilterTab(TaskFilterTab.SHARED)
                                    } else if (dest == MainNavDestination.TASKS && uiState.selectedTab == TaskFilterTab.SHARED) {
                                        viewModel.setFilterTab(TaskFilterTab.ALL)
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title, fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentNav == MainNavDestination.TASKS || currentNav == MainNavDestination.SHARED) {
                    FloatingActionButton(
                        onClick = { showAddTaskDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.testTag("fab_add_task")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Yeni Görev Ekle", modifier = Modifier.size(28.dp))
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Wide Screen Navigation Rail (Desktop / Tablet)
                if (isWideScreen) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        MainNavDestination.entries.forEach { dest ->
                            NavigationRailItem(
                                selected = currentNav == dest,
                                onClick = {
                                    currentNav = dest
                                    if (dest == MainNavDestination.SHARED) {
                                        viewModel.setFilterTab(TaskFilterTab.SHARED)
                                    } else if (dest == MainNavDestination.TASKS && uiState.selectedTab == TaskFilterTab.SHARED) {
                                        viewModel.setFilterTab(TaskFilterTab.ALL)
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = dest.title) },
                                label = { Text(dest.title) },
                                modifier = Modifier.testTag("rail_nav_${dest.name.lowercase()}")
                            )
                        }
                    }
                }

                // Main Content Area with fluid max width constraints
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 900.dp)
                    ) {
                        AnimatedContent(
                            targetState = currentNav,
                            transitionSpec = {
                                (fadeIn() + slideInVertically()).togetherWith(fadeOut() + slideOutVertically())
                            },
                            label = "nav_transition"
                        ) { targetNav ->
                            when (targetNav) {
                                MainNavDestination.TASKS, MainNavDestination.SHARED -> {
                                    TasksListContent(
                                        viewModel = viewModel,
                                        filteredTasks = filteredTasks,
                                        customCategories = customCategories,
                                        onEditTask = { taskToEdit = it },
                                        onShareTask = { taskToShare = it },
                                        onAddTask = { showAddTaskDialog = true }
                                    )
                                }
                                MainNavDestination.ANALYTICS -> {
                                    AnalyticsDashboard(stats = stats)
                                }
                                MainNavDestination.SETTINGS -> {
                                    SettingsContent(
                                        userEmail = auth.currentUser?.email ?: "Kullanıcı",
                                        userDisplayName = auth.currentUser?.displayName ?: "TaskFlow Kullanıcısı",
                                        isDarkMode = uiState.isDarkMode,
                                        isAutoBackupEnabled = uiState.isAutoBackupEnabled,
                                        onToggleDarkMode = { viewModel.toggleDarkMode(it) },
                                        onToggleAutoBackup = { viewModel.toggleAutoBackup(it) },
                                        onOpenBackupDialog = { showBackupDialog = true },
                                        onSignOut = {
                                            signOut(
                                                context = context,
                                                credentialManager = credentialManager,
                                                onSignOutComplete = onSignOutRequested,
                                                scope = scope
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Task Dialog
    if (showAddTaskDialog || taskToEdit != null) {
        AddEditTaskDialog(
            taskToEdit = taskToEdit,
            categories = customCategories,
            onDismiss = {
                showAddTaskDialog = false
                taskToEdit = null
            },
            onSave = { title, desc, cat, prio, due, reminder, collabs ->
                if (taskToEdit != null) {
                    viewModel.updateTask(
                        taskId = taskToEdit!!.id,
                        title = title,
                        description = desc,
                        category = cat,
                        priority = prio,
                        dueDate = due,
                        reminderMinutesBefore = reminder
                    )
                } else {
                    viewModel.createTask(
                        title = title,
                        description = desc,
                        category = cat,
                        priority = prio,
                        dueDate = due,
                        reminderMinutesBefore = reminder,
                        collaboratorEmails = collabs
                    )
                }
                showAddTaskDialog = false
                taskToEdit = null
            },
            onAddNewCategory = {
                showAddCategoryDialog = true
            }
        )
    }

    // Share Task Dialog
    if (taskToShare != null) {
        ShareTaskDialog(
            task = taskToShare!!,
            onDismiss = { taskToShare = null },
            onAddCollaborator = { email ->
                viewModel.addCollaborator(taskToShare!!.id, email)
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onCreate = { name, color, icon ->
                viewModel.createCategory(name, color, icon)
            }
        )
    }

    // Encrypted Backup Dialog
    if (showBackupDialog) {
        EncryptedBackupDialog(
            isAutoBackupEnabled = uiState.isAutoBackupEnabled,
            onToggleAutoBackup = { viewModel.toggleAutoBackup(it) },
            onExportBackup = { passphrase -> viewModel.exportEncryptedBackup(passphrase) },
            onRestoreBackup = { data, pass -> viewModel.restoreEncryptedBackup(data, pass) },
            onDismiss = { showBackupDialog = false }
        )
    }
}

@Composable
private fun TasksListContent(
    viewModel: TaskViewModel,
    filteredTasks: List<TaskItem>,
    customCategories: List<com.example.data.model.CategoryItem>,
    onEditTask: (TaskItem) -> Unit,
    onShareTask: (TaskItem) -> Unit,
    onAddTask: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        FilterSortBar(
            selectedTab = uiState.selectedTab,
            onTabSelected = { viewModel.setFilterTab(it) },
            searchQuery = uiState.searchQuery,
            onSearchQueryChanged = { viewModel.setSearchQuery(it) },
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.setCategoryFilter(it) },
            selectedPriority = uiState.selectedPriority,
            onPrioritySelected = { viewModel.setPriorityFilter(it) },
            sortOrder = uiState.sortOrder,
            onSortOrderSelected = { viewModel.setSortOrder(it) },
            customCategories = customCategories
        )

        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Görev Bulunmuyor",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bu filtre için henüz görev yok. Yeni bir görev ekleyerek başlayın!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskComplete(task) },
                        onEdit = { onEditTask(task) },
                        onShare = { onShareTask(task) },
                        onDelete = { viewModel.deleteTask(task.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    userEmail: String,
    userDisplayName: String,
    isDarkMode: Boolean?,
    isAutoBackupEnabled: Boolean,
    onToggleDarkMode: (Boolean?) -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onOpenBackupDialog: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // User Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userDisplayName.take(1).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userDisplayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userEmail,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Çıkış Yap",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Appearance & Dark Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Görünüm ve Tema",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Karanlık Mod", fontSize = 14.sp)
                            Text(
                                text = when (isDarkMode) {
                                    null -> "Sistem Teması (Otomatik)"
                                    true -> "Açık (Karanlık Mod)"
                                    false -> "Kapalı (Açık Mod)"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkMode ?: true,
                            onCheckedChange = { checked ->
                                onToggleDarkMode(checked)
                            }
                        )
                    }
                }
            }
        }

        // Security & Backups
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Güvenlik & Yedekleme",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Otomatik Periyodik Yedek", fontSize = 14.sp)
                            Text("Verilerinizi yerel olarak korur", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = onToggleAutoBackup
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        onClick = onOpenBackupDialog,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Uçtan Uca Şifreli Yedekleme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("AES-256 ile verileri dışa aktar veya geri yükle", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Cloud Infrastructure Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Bulut & Senkronizasyon Altyapısı",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "• Firebase Firestore Enterprise: Gerçek zamanlı snapshot dinleyicileri ile çok kullanıcılı senkronizasyon.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Çevrimdışı (Offline) Mod: Yerel önbellek sayesinde internet yokken yapılan tüm ekleme ve güncellemeler otomatik saklanır ve bağlantı kurulduğunda senkronize edilir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Düşük Enerji Tüketimi: Verimli Coroutine Flow & StateFlow mimarisi ile gereksiz recomposition ve CPU yükü engellenmiştir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
