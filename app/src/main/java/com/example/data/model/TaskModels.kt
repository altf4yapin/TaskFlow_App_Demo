package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ServerTimestamp

enum class TaskPriority(val displayName: String, val level: Int) {
    LOW("Düşük", 1),
    MEDIUM("Orta", 2),
    HIGH("Yüksek", 3),
    URGENT("Acil", 4);

    companion object {
        fun fromString(value: String?): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

data class TaskItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Genel",
    val priority: String = "MEDIUM",
    val dueDate: Timestamp? = null,
    val reminderMinutesBefore: Int = 15,
    val completed: Boolean = false,
    val completedAt: Timestamp? = null,
    val sharedWithUids: List<String> = emptyList(),
    val sharedWithEmails: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "title" to title,
            "description" to description,
            "category" to category,
            "priority" to priority,
            "dueDate" to dueDate,
            "reminderMinutesBefore" to reminderMinutesBefore,
            "completed" to completed,
            "completedAt" to completedAt,
            "sharedWithUids" to sharedWithUids,
            "sharedWithEmails" to sharedWithEmails,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): TaskItem? {
            val data = doc.data ?: return null
            return TaskItem(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                category = data["category"] as? String ?: "Genel",
                priority = data["priority"] as? String ?: "MEDIUM",
                dueDate = data["dueDate"] as? Timestamp,
                reminderMinutesBefore = (data["reminderMinutesBefore"] as? Number)?.toInt() ?: 15,
                completed = data["completed"] as? Boolean ?: false,
                completedAt = data["completedAt"] as? Timestamp,
                sharedWithUids = (data["sharedWithUids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                sharedWithEmails = (data["sharedWithEmails"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = data["createdAt"] as? Timestamp,
                updatedAt = data["updatedAt"] as? Timestamp
            )
        }
    }
}

data class CategoryItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val colorHex: String = "#6366F1",
    val iconName: String = "folder",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "colorHex" to colorHex,
            "iconName" to iconName,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromDocument(doc: DocumentSnapshot): CategoryItem? {
            val data = doc.data ?: return null
            return CategoryItem(
                id = doc.id,
                userId = data["userId"] as? String ?: "",
                name = data["name"] as? String ?: "",
                colorHex = data["colorHex"] as? String ?: "#6366F1",
                iconName = data["iconName"] as? String ?: "folder",
                createdAt = data["createdAt"] as? Timestamp
            )
        }
    }
}

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
