package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.TaskItem
import com.example.data.model.TaskPriority
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskDialog(
    taskToEdit: TaskItem? = null,
    categories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Date?,
        reminderMinutes: Int,
        collaborators: List<String>
    ) -> Unit,
    onAddNewCategory: () -> Unit
) {
    var title by remember { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(taskToEdit?.category ?: "Genel") }
    var selectedPriority by remember {
        mutableStateOf(taskToEdit?.let { TaskPriority.fromString(it.priority) } ?: TaskPriority.MEDIUM)
    }

    // Due date offset options
    var dueDateOffsetDays by remember {
        mutableStateOf<Int?>(
            taskToEdit?.dueDate?.let {
                val calNow = Calendar.getInstance()
                val calDue = Calendar.getInstance().apply { timeInMillis = it.seconds * 1000 }
                val diff = calDue.get(Calendar.DAY_OF_YEAR) - calNow.get(Calendar.DAY_OF_YEAR)
                if (diff in 0..7) diff else 0
            }
        )
    }

    var reminderMinutes by remember { mutableIntStateOf(taskToEdit?.reminderMinutesBefore ?: 15) }
    var collaboratorInput by remember { mutableStateOf("") }
    val collaboratorList = remember {
        mutableStateListOf<String>().apply {
            taskToEdit?.sharedWithEmails?.let { addAll(it) }
        }
    }

    val defaultCategories = listOf("Genel", "İş", "Kişisel", "Proje", "Sağlık", "Alışveriş")
    val allCategoryNames = (defaultCategories + categories.map { it.name }).distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (taskToEdit == null) "Yeni Görev Ekle" else "Görevi Düzenle",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Görev Başlığı *") },
                    placeholder = { Text("Örn: Sprint planlama toplantısı") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama / Detaylar (İsteğe Bağlı)") },
                    placeholder = { Text("Toplantı notları veya yapılacaklar...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_description_input")
                )

                // Priority Selection
                Text(
                    text = "Öncelik Derecesi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskPriority.entries.forEach { prio ->
                        val isSelected = selectedPriority == prio
                        val color = when (prio) {
                            TaskPriority.LOW -> PriorityLow
                            TaskPriority.MEDIUM -> PriorityMedium
                            TaskPriority.HIGH -> PriorityHigh
                            TaskPriority.URGENT -> PriorityUrgent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) color else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedPriority = prio }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Category Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kategori",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onAddNewCategory) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kategori Ekle", fontSize = 12.sp)
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCategoryNames.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat, ignoreCase = true),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Due Date Quick Select
                Text(
                    text = "Bitiş Tarihi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        null to "Yok",
                        0 to "Bugün",
                        1 to "Yarın",
                        3 to "3 Gün",
                        7 to "1 Hafta"
                    ).forEach { (offset, label) ->
                        val isSelected = dueDateOffsetDays == offset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { dueDateOffsetDays = offset }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Collaborators Input
                Text(
                    text = "İşbirlikçiler (E-posta ile Paylaş)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = collaboratorInput,
                        onValueChange = { collaboratorInput = it },
                        placeholder = { Text("arkadas@ornek.com", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (collaboratorInput.contains("@") && !collaboratorList.contains(collaboratorInput.trim())) {
                                collaboratorList.add(collaboratorInput.trim())
                                collaboratorInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (collaboratorList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        collaboratorList.forEach { email ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kaldır",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { collaboratorList.remove(email) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalDate = dueDateOffsetDays?.let { offset ->
                            Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, offset)
                                set(Calendar.HOUR_OF_DAY, 18)
                                set(Calendar.MINUTE, 0)
                            }.time
                        }
                        onSave(
                            title,
                            description,
                            selectedCategory,
                            selectedPriority,
                            finalDate,
                            reminderMinutes,
                            collaboratorList.toList()
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Composable
fun ShareTaskDialog(
    task: TaskItem,
    onDismiss: () -> Unit,
    onAddCollaborator: (String) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Görevi Paylaş", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "\"${task.title}\" görevine işbirlikçi ekleyerek birlikte çalışın.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Kullanıcı E-posta Adresi") },
                    placeholder = { Text("kullanici@ornek.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (task.sharedWithEmails.isNotEmpty()) {
                    Text(
                        text = "Mevcut İşbirlikçiler:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    task.sharedWithEmails.forEach { email ->
                        Text(
                            text = "• $email",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (emailInput.contains("@")) {
                        onAddCollaborator(emailInput.trim())
                        onDismiss()
                    }
                },
                enabled = emailInput.contains("@")
            ) {
                Text("Paylaş")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@Composable
fun EncryptedBackupDialog(
    isAutoBackupEnabled: Boolean,
    onToggleAutoBackup: (Boolean) -> Unit,
    onExportBackup: (String) -> String?,
    onRestoreBackup: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var passphrase by remember { mutableStateOf("") }
    var exportedResult by remember { mutableStateOf<String?>(null) }
    var importData by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uçtan Uca Şifreli Yedek", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Auto-backup toggle switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Otomatik Periyodik Yedekleme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Verileriniz yerel depolamada periyodik korunur", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isAutoBackupEnabled,
                        onCheckedChange = { onToggleAutoBackup(it) }
                    )
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Dışa Aktar", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Geri Yükle", fontSize = 12.sp) }
                    )
                }

                if (selectedTab == 0) {
                    // Export
                    Text(
                        text = "Verileriniz AES-256-GCM protokolü ile şifrelenir. Şifrenizi unutursanız veriler kurtarılamaz.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Şifreleme Parolası (En az 4 karakter)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            exportedResult = onExportBackup(passphrase)
                        },
                        enabled = passphrase.length >= 4,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Şifrele ve Yedek Oluştur")
                    }

                    if (exportedResult != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(10.dp)
                        ) {
                            Text("Şifrelenmiş Yedek Verisi:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = exportedResult?.take(60) + "...",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(exportedResult ?: ""))
                                    Toast.makeText(context, "Yedek kopyalandı!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Panoya Kopyala", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Import
                    Text(
                        text = "Daha önce oluşturulmuş şifreli yedeği yapıştırın ve parolasını girin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = importData,
                        onValueChange = { importData = it },
                        label = { Text("Şifreli Yedek Metni") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Çözme Parolası") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (importData.isNotBlank() && passphrase.isNotBlank()) {
                                onRestoreBackup(importData, passphrase)
                                onDismiss()
                            }
                        },
                        enabled = importData.isNotBlank() && passphrase.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yedekten Geri Yükle")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val colors = listOf("#6366F1", "#06B6D4", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Kategori Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Kategori Adı") },
                    placeholder = { Text("Örn: Finans, Spor...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Renk Seçimi", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val isSelected = selectedColor == hex
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedColor, "folder")
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
