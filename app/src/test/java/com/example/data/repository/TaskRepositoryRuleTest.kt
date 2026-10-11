package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(title = TASK_TITLE, description = "Test Desc", category = "Work", priority = "HIGH")
        }
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertTrue(taskId.isNotEmpty())
    }

    @Test
    fun observeUserTasks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(title = "Realtime Task", category = "Work").getOrThrow()
        }

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserTasks().first { list -> list.any { it.id == taskId } }
        }
        assertTrue(emittedTasks.any { it.id == taskId })
    }

    @Test
    fun observeUserTasks_unauthenticatedUser_fails() = runBlocking {
        auth.signOut()
        val repository = TaskRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserTasks().first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException || e.message?.contains("User must be signed in") == true)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val TASK_TITLE = "Proje Görevi"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
