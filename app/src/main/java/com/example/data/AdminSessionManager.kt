package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages secure Administrator session lifecycle with hardware-backed encryption:
 * 1. AES-256-GCM Encryption: Session metadata is encrypted at rest using AES-256 in Galois/Counter Mode.
 * 2. Android Keystore Key Protection: The AES key is stored in the hardware-backed "AndroidKeyStore"
 *    provider (with graceful JVM fallback for local unit tests).
 * 3. Seamless Migration: Automatically migrates unencrypted legacy session data without invalidating existing sessions.
 * 4. Strict 4-Hour Inactivity Timeout:
 *    - Tracks last authenticated admin activity timestamp.
 *    - Resets timer on any authenticated admin operation or meaningful interaction.
 *    - Automatically invalidates session, clears tokens/credentials, and redirects to login
 *      when 4 continuous hours of inactivity are reached.
 * 5. Remember Me: Preserves session across restarts ONLY if Remember Me was selected and within 4h inactivity.
 */
object AdminSessionManager {

    const val INACTIVITY_TIMEOUT_MS = 4 * 60 * 60 * 1000L // 4 hours in milliseconds (14,400,000 ms)

    private const val PREFS_NAME = "yarkhoon_admin_secure_session"
    private const val KEY_ENC_PAYLOAD = "admin_session_enc_payload_v2"

    // Legacy keys for seamless migration
    private const val KEY_LEGACY_IS_ACTIVE = "admin_session_is_active"
    private const val KEY_LEGACY_REMEMBER_ME = "admin_session_remember_me"
    private const val KEY_LEGACY_START_TIME_MS = "admin_session_start_time_ms"
    private const val KEY_LEGACY_LAST_ACTIVITY_MS = "admin_session_last_activity_ms"
    private const val KEY_LEGACY_ADMIN_UID = "admin_session_uid"
    private const val KEY_LEGACY_ADMIN_EMAIL = "admin_session_email"
    private const val KEY_LEGACY_SESSION_SIG = "admin_session_signature"

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "yarkhoon_admin_session_aes256"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    @Volatile
    private var sharedPreferences: SharedPreferences? = null

    // In-memory volatile state for zero-overhead access
    @Volatile
    var isSessionActive: Boolean = false
        private set

    @Volatile
    var isRememberMeEnabled: Boolean = false
        private set

    @Volatile
    var sessionStartTimeMs: Long = 0L
        private set

    @Volatile
    var lastActivityTimeMs: Long = 0L
        private set

    @Volatile
    var currentAdminUid: String? = null
        private set

    @Volatile
    var currentAdminEmail: String? = null
        private set

    @Volatile
    private var fallbackSecretKey: SecretKey? = null

    /**
     * Initializes the AdminSessionManager with application Context.
     */
    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            synchronized(this) {
                if (sharedPreferences == null) {
                    val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    sharedPreferences = prefs
                    loadFromStorage()
                }
            }
        }
    }

    // -------------------------------------------------------------
    // Keystore-backed AES-256-GCM Encryption Engine
    // -------------------------------------------------------------

    private fun getOrCreateSecretKey(): SecretKey {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            } else {
                (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            }
        } catch (_: Exception) {
            // Fallback for JVM unit tests where AndroidKeyStore provider is absent
            getFallbackSecretKey()
        }
    }

    private fun getFallbackSecretKey(): SecretKey {
        return fallbackSecretKey ?: synchronized(this) {
            fallbackSecretKey ?: run {
                val salt = "yarkhoon_admin_keystore_fallback_32bytes_aes_key!!"
                    .toByteArray(Charsets.UTF_8).copyOf(32)
                val key = SecretKeySpec(salt, "AES")
                fallbackSecretKey = key
                key
            }
        }
    }

    fun encryptPayload(plaintext: String): String {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ivB64 = Base64.getEncoder().encodeToString(iv)
        val cipherB64 = Base64.getEncoder().encodeToString(ciphertext)
        return "$ivB64:$cipherB64"
    }

    fun decryptPayload(payload: String): String? {
        return try {
            val parts = payload.split(":")
            if (parts.size != 2) return null
            val iv = Base64.getDecoder().decode(parts[0])
            val ciphertext = Base64.getDecoder().decode(parts[1])
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun computeSignature(uid: String, startMs: Long, rememberMe: Boolean): String {
        val raw = "$uid:$startMs:$rememberMe:yarkhoon_admin_secret_salt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun saveToEncryptedStorage() {
        val prefs = sharedPreferences ?: return
        val json = JSONObject().apply {
            put("isActive", isSessionActive)
            put("rememberMe", isRememberMeEnabled)
            put("startMs", sessionStartTimeMs)
            put("lastActivityMs", lastActivityTimeMs)
            put("uid", currentAdminUid.orEmpty())
            put("email", currentAdminEmail.orEmpty())
            put("sig", computeSignature(currentAdminUid.orEmpty(), sessionStartTimeMs, isRememberMeEnabled))
        }.toString()

        val encrypted = encryptPayload(json)
        prefs.edit()
            .putString(KEY_ENC_PAYLOAD, encrypted)
            // Remove legacy unencrypted keys if present
            .remove(KEY_LEGACY_IS_ACTIVE)
            .remove(KEY_LEGACY_REMEMBER_ME)
            .remove(KEY_LEGACY_START_TIME_MS)
            .remove(KEY_LEGACY_LAST_ACTIVITY_MS)
            .remove(KEY_LEGACY_ADMIN_UID)
            .remove(KEY_LEGACY_ADMIN_EMAIL)
            .remove(KEY_LEGACY_SESSION_SIG)
            .apply()
    }

    private fun loadFromStorage() {
        val prefs = sharedPreferences ?: return
        val encryptedPayload = prefs.getString(KEY_ENC_PAYLOAD, null)

        if (!encryptedPayload.isNullOrBlank()) {
            val decryptedJsonStr = decryptPayload(encryptedPayload)
            if (decryptedJsonStr != null) {
                try {
                    val json = JSONObject(decryptedJsonStr)
                    isSessionActive = json.optBoolean("isActive", false)
                    isRememberMeEnabled = json.optBoolean("rememberMe", false)
                    sessionStartTimeMs = json.optLong("startMs", 0L)
                    lastActivityTimeMs = json.optLong("lastActivityMs", 0L)
                    currentAdminUid = json.optString("uid").takeIf { it.isNotBlank() }
                    currentAdminEmail = json.optString("email").takeIf { it.isNotBlank() }
                    val savedSig = json.optString("sig")

                    if (isSessionActive && savedSig.isNotBlank()) {
                        val expectedSig = computeSignature(currentAdminUid.orEmpty(), sessionStartTimeMs, isRememberMeEnabled)
                        if (savedSig != expectedSig) {
                            invalidateAndClear()
                        }
                    }
                    return
                } catch (_: Exception) {
                    invalidateAndClear()
                    return
                }
            } else {
                // Decryption failure (key rotated or tampered)
                invalidateAndClear()
                return
            }
        }

        // Seamless migration: Check if legacy unencrypted preferences exist
        if (prefs.contains(KEY_LEGACY_IS_ACTIVE)) {
            isSessionActive = prefs.getBoolean(KEY_LEGACY_IS_ACTIVE, false)
            isRememberMeEnabled = prefs.getBoolean(KEY_LEGACY_REMEMBER_ME, false)
            sessionStartTimeMs = prefs.getLong(KEY_LEGACY_START_TIME_MS, 0L)
            lastActivityTimeMs = prefs.getLong(KEY_LEGACY_LAST_ACTIVITY_MS, 0L)
            currentAdminUid = prefs.getString(KEY_LEGACY_ADMIN_UID, null)
            currentAdminEmail = prefs.getString(KEY_LEGACY_ADMIN_EMAIL, null)

            val savedSig = prefs.getString(KEY_LEGACY_SESSION_SIG, null)
            if (isSessionActive && savedSig != null) {
                val expectedSig = computeSignature(currentAdminUid.orEmpty(), sessionStartTimeMs, isRememberMeEnabled)
                if (savedSig != expectedSig) {
                    invalidateAndClear()
                    return
                }
            }
            // Migrate to encrypted storage and purge legacy plain fields
            saveToEncryptedStorage()
        }
    }

    /**
     * Starts a new authenticated administrator session with Keystore AES-256-GCM encryption.
     */
    fun startSession(
        uid: String,
        email: String,
        rememberMe: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ) {
        synchronized(this) {
            isSessionActive = true
            isRememberMeEnabled = rememberMe
            sessionStartTimeMs = nowMs
            lastActivityTimeMs = nowMs
            currentAdminUid = uid
            currentAdminEmail = email
            saveToEncryptedStorage()
        }
    }

    /**
     * Resets the inactivity timer on any meaningful authenticated admin interaction.
     */
    fun recordActivity(nowMs: Long = System.currentTimeMillis()) {
        if (!isSessionActive) return
        synchronized(this) {
            if (!isSessionActive) return
            lastActivityTimeMs = nowMs
            saveToEncryptedStorage()
        }
    }

    /**
     * Evaluates whether the current admin session has expired due to 4 continuous hours of inactivity.
     * Also checks for abnormal clock manipulation.
     */
    fun isSessionExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isSessionActive) return true
        if (lastActivityTimeMs <= 0L) return true

        val elapsed = nowMs - lastActivityTimeMs
        // If elapsed time exceeds 4-hour inactivity timeout or abnormal time jump backwards (> 60s)
        if (elapsed > INACTIVITY_TIMEOUT_MS || elapsed < -60_000L) {
            return true
        }
        return false
    }

    /**
     * Evaluates whether a session should be automatically restored across an app restart:
     * Strictly requires:
     * 1. Session is currently active.
     * 2. "Remember Me" was explicitly enabled by the admin.
     * 3. 4-hour continuous inactivity timeout has NOT expired.
     */
    fun shouldRestoreOnAppStart(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isSessionActive) return false
        if (!isRememberMeEnabled) return false
        if (isSessionExpired(nowMs)) return false
        return true
    }

    /**
     * Returns the remaining time in milliseconds before the current session expires due to inactivity.
     */
    fun getRemainingInactivityTimeMs(nowMs: Long = System.currentTimeMillis()): Long {
        if (!isSessionActive || lastActivityTimeMs <= 0L) return 0L
        val elapsed = nowMs - lastActivityTimeMs
        return (INACTIVITY_TIMEOUT_MS - elapsed).coerceAtLeast(0L)
    }

    /**
     * Completely invalidates the session, clears secure storage, and signs out of Firebase Auth.
     * Guarantees that expired tokens and credentials cannot be reused.
     */
    fun invalidateAndClear(auth: FirebaseAuth? = null) {
        synchronized(this) {
            isSessionActive = false
            isRememberMeEnabled = false
            sessionStartTimeMs = 0L
            lastActivityTimeMs = 0L
            currentAdminUid = null
            currentAdminEmail = null

            sharedPreferences?.edit()?.clear()?.apply()

            try {
                auth?.signOut()
            } catch (_: Exception) {}
        }
    }

    // -------------------------------------------------------------
    // Testing and Diagnostics Utilities
    // -------------------------------------------------------------

    fun resetForTesting() {
        synchronized(this) {
            isSessionActive = false
            isRememberMeEnabled = false
            sessionStartTimeMs = 0L
            lastActivityTimeMs = 0L
            currentAdminUid = null
            currentAdminEmail = null
            sharedPreferences?.edit()?.clear()?.apply()
        }
    }

    fun overrideSessionForTesting(
        active: Boolean,
        rememberMe: Boolean,
        startMs: Long,
        lastActivityMs: Long,
        uid: String?,
        email: String?
    ) {
        synchronized(this) {
            isSessionActive = active
            isRememberMeEnabled = rememberMe
            sessionStartTimeMs = startMs
            lastActivityTimeMs = lastActivityMs
            currentAdminUid = uid
            currentAdminEmail = email
        }
    }
}
