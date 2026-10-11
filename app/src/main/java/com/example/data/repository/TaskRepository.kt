package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.CategoryItem
import com.example.data.model.TaskItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TaskRepository(private val db: FirebaseFirestore) {

    // Convenience constructor resolving R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    /**
     * Observes all tasks owned by the current user as a real-time Flow.
     */
    fun observeUserTasks(): Flow<List<TaskItem>> = flow {
        val uid = requireUserId()
        val path = "tasks"
        val query = db.collection(path).whereEqualTo("userId", uid)
        
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc -> TaskItem.fromDocument(doc) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    /**
     * Observes all tasks shared with the current user as collaborator.
     */
    fun observeSharedWithMeTasks(): Flow<List<TaskItem>> = flow {
        val uid = requireUserId()
        val path = "tasks"
        val query = db.collection(path).whereArrayContains("sharedWithUids", uid)

        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc -> TaskItem.fromDocument(doc) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    /**
     * Combines user owned tasks and shared tasks into a single distinct list.
     */
    fun observeAllAccessibleTasks(): Flow<List<TaskItem>> {
        return combine(observeUserTasks(), observeSharedWithMeTasks()) { own, shared ->
            (own + shared).distinctBy { it.id }.sortedWith(
                compareBy<TaskItem> { it.completed }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }
    }

    /**
     * Creates a new task.
     */
    suspend fun createTask(
        title: String,
        description: String = "",
        category: String = "Genel",
        priority: String = "MEDIUM",
        dueDate: Timestamp? = null,
        reminderMinutesBefore: Int = 15,
        sharedWithEmails: List<String> = emptyList(),
        sharedWithUids: List<String> = emptyList()
    ): Result<String> {
        return try {
            val uid = requireUserId()
            val taskId = UUID.randomUUID().toString()
            val taskRef = db.collection("tasks").document(taskId)

            val payload = mutableMapOf<String, Any?>(
                "id" to taskId,
                "userId" to uid,
                "title" to title.trim(),
                "description" to description.trim(),
                "category" to category.trim(),
                "priority" to priority,
                "completed" to false,
                "reminderMinutesBefore" to reminderMinutesBefore,
                "sharedWithUids" to sharedWithUids,
                "sharedWithEmails" to sharedWithEmails,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (dueDate != null) {
                payload["dueDate"] = dueDate
            }

            val sanitizedPayload = payload.filterValues { it != null }
            taskRef.set(sanitizedPayload).await()
            Result.success(taskId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "tasks")
            Result.failure(e)
        }
    }

    /**
     * Toggles the completion status of a task.
     */
    suspend fun toggleTaskComplete(taskId: String, completed: Boolean): Result<Unit> {
        return try {
            val taskRef = db.collection("tasks").document(taskId)
            val updates = mutableMapOf<String, Any?>(
                "completed" to completed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (completed) {
                updates["completedAt"] = FieldValue.serverTimestamp()
            } else {
                updates["completedAt"] = null
            }
            taskRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    /**
     * Updates an existing task.
     */
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: Timestamp?,
        reminderMinutesBefore: Int
    ): Result<Unit> {
        return try {
            val taskRef = db.collection("tasks").document(taskId)
            val updates = mutableMapOf<String, Any?>(
                "title" to title.trim(),
                "description" to description.trim(),
                "category" to category.trim(),
                "priority" to priority,
                "reminderMinutesBefore" to reminderMinutesBefore,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (dueDate != null) {
                updates["dueDate"] = dueDate
            } else {
                updates["dueDate"] = null
            }
            taskRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    /**
     * Deletes a task by ID.
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            val taskRef = db.collection("tasks").document(taskId)
            taskRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    /**
     * Shares a task with a collaborator email/uid.
     */
    suspend fun addCollaborator(taskId: String, collaboratorEmail: String, collaboratorUid: String? = null): Result<Unit> {
        return try {
            val taskRef = db.collection("tasks").document(taskId)
            val updates = mutableMapOf<String, Any>(
                "sharedWithEmails" to FieldValue.arrayUnion(collaboratorEmail.trim()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!collaboratorUid.isNullOrBlank()) {
                updates["sharedWithUids"] = FieldValue.arrayUnion(collaboratorUid)
            }
            taskRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    /**
     * Observe categories for current user.
     */
    fun observeCategories(): Flow<List<CategoryItem>> = flow {
        val uid = requireUserId()
        val path = "categories"
        val query = db.collection(path).whereEqualTo("userId", uid)

        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { CategoryItem.fromDocument(it) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    /**
     * Creates a custom category.
     */
    suspend fun createCategory(name: String, colorHex: String, iconName: String): Result<String> {
        return try {
            val uid = requireUserId()
            val categoryId = UUID.randomUUID().toString()
            val catRef = db.collection("categories").document(categoryId)

            val payload = mapOf(
                "id" to categoryId,
                "userId" to uid,
                "name" to name.trim(),
                "colorHex" to colorHex,
                "iconName" to iconName,
                "createdAt" to FieldValue.serverTimestamp()
            )
            catRef.set(payload).await()
            Result.success(categoryId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "categories")
            Result.failure(e)
        }
    }

    /**
     * Saves or updates current user profile.
     */
    suspend fun syncUserProfile(email: String, displayName: String, photoUrl: String?): Result<Unit> {
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "email" to email,
                "displayName" to displayName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (photoUrl != null) {
                payload["photoUrl"] = photoUrl
            }
            val existing = userRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            userRef.set(payload.filterValues { it != null }, com.google.firebase.firestore.SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users")
            Result.failure(e)
        }
    }
}
