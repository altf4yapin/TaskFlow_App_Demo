package com.example.data.security

import android.content.Context
import android.util.Base64
import com.example.data.model.TaskItem
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecurityBackupManager {

    private const val ITERATIONS = 65536
    private const val KEY_LENGTH = 256
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12
    private const val SALT_LENGTH = 16

    private const val PREFS_NAME = "taskflow_security_prefs"
    private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_time"

    /**
     * Serializes task list to JSON format.
     */
    fun serializeTasks(tasks: List<TaskItem>): String {
        val root = JSONObject()
        val array = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("category", task.category)
                put("priority", task.priority)
                put("completed", task.completed)
                put("reminderMinutesBefore", task.reminderMinutesBefore)
                put("sharedWithEmails", JSONArray(task.sharedWithEmails))
                put("dueDateSeconds", task.dueDate?.seconds ?: -1L)
                put("createdAtSeconds", task.createdAt?.seconds ?: -1L)
            }
            array.put(obj)
        }
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("tasks", array)
        return root.toString()
    }

    /**
     * Encrypts plain text using AES-256-GCM and password-derived key.
     */
    fun encryptData(plainText: String, passphrase: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        val key = SecretKeySpec(secretKey.encoded, "AES")

        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        // Concatenate salt + iv + cipherText
        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts AES-256-GCM payload using password.
     */
    fun decryptData(encryptedBase64: String, passphrase: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

        val salt = ByteArray(SALT_LENGTH)
        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)

        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH)

        val cipherTextLength = combined.size - SALT_LENGTH - IV_LENGTH
        val cipherText = ByteArray(cipherTextLength)
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherTextLength)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        val key = SecretKeySpec(secretKey.encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, StandardCharsets.UTF_8)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
    }

    fun recordBackupTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, System.currentTimeMillis()).apply()
    }
}
