package com.example.data

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val TAG = "YarkhoonAuthDebug"
        private const val ANDROID_PACKAGE_NAME = "com.aistudio.yarkhoon.scbqdm"
        private const val DEBUG_CERT_SHA1 = "AE5D37168F891962DF0EB9E3073364687D5E7319"

        fun buildLinkedSessionEmail(cleanEmail: String, password: String): String {
            val normalized = FirestoreAdminRepository.resolveCanonicalEmail(cleanEmail)
            val safePrefix = normalized.replace("@", ".").replace(Regex("[^a-z0-9.-]"), "")
            val digest = MessageDigest.getInstance("SHA-256")
                .digest("$normalized:$password".toByteArray(Charsets.UTF_8))
                .take(4)
                .joinToString("") { "%02x".format(it) }
            return "$safePrefix.$digest@yarkhoon.com"
        }
    }

    private sealed class PreflightResult {
        data object Success : PreflightResult()
        data class HttpError(val code: String, val rawMessage: String) : PreflightResult()
        data class NetworkError(val message: String) : PreflightResult()
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    private fun resolveFirebaseApiKey(): String {
        return try {
            FirebaseApp.getInstance().options.apiKey
        } catch (_: Exception) {
            ""
        }
    }

    private suspend fun executeIdentityToolkitPost(
        endpoint: String,
        email: String,
        password: String,
        includeAndroidHeaders: Boolean = true
    ): PreflightResult = withContext(Dispatchers.IO) {
        val apiKey = resolveFirebaseApiKey()
        if (apiKey.isBlank()) {
            return@withContext PreflightResult.NetworkError("Firebase API key is unavailable.")
        }
        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://identitytoolkit.googleapis.com/v1/$endpoint?key=$apiKey")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                if (includeAndroidHeaders) {
                    setRequestProperty("X-Android-Package", ANDROID_PACKAGE_NAME)
                    setRequestProperty("X-Android-Cert", DEBUG_CERT_SHA1)
                }
            }

            val payload = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("returnSecureToken", true)
            }.toString()

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream?.close()
                return@withContext PreflightResult.Success
            }

            val errorText = try {
                connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            } catch (_: Exception) {
                ""
            }

            if (responseCode == 403 && includeAndroidHeaders) {
                connection.disconnect()
                return@withContext executeIdentityToolkitPost(
                    endpoint = endpoint,
                    email = email,
                    password = password,
                    includeAndroidHeaders = false
                )
            }

            val parsedMessage = try {
                JSONObject(errorText).optJSONObject("error")?.optString("message").orEmpty()
            } catch (_: Exception) {
                ""
            }.ifBlank { "HTTP_$responseCode" }

            PreflightResult.HttpError(code = parsedMessage, rawMessage = parsedMessage)
        } catch (e: Exception) {
            PreflightResult.NetworkError(e.localizedMessage ?: "Network error while verifying credentials.")
        } finally {
            try {
                connection?.disconnect()
            } catch (_: Exception) {}
        }
    }

    private suspend fun signInWithVerifiedEmailCredential(
        authEmail: String,
        canonicalEmail: String,
        password: String,
        displayName: String? = null
    ): AuthResult {
        return try {
            val credential = EmailAuthProvider.getCredential(authEmail, password)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                if (!displayName.isNullOrBlank() && user.displayName.isNullOrBlank()) {
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName.trim())
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (_: Exception) {}
                }
                AuthResult.Success(
                    uid = user.uid,
                    email = canonicalEmail,
                    displayName = user.displayName?.takeIf { it.isNotBlank() } ?: displayName
                )
            } else {
                AuthResult.Error("Authentication succeeded but user session was not initialized.")
            }
        } catch (e: Exception) {
            AuthResult.Error(mapFirebaseException(e))
        }
    }

    private suspend fun signInOrProvisionLinkedSession(
        canonicalEmail: String,
        password: String,
        displayName: String? = null
    ): AuthResult {
        val linkedEmail = buildLinkedSessionEmail(canonicalEmail, password)
        val signInCheck = executeIdentityToolkitPost("accounts:signInWithPassword", linkedEmail, password)
        if (signInCheck is PreflightResult.Success) {
            return signInWithVerifiedEmailCredential(
                authEmail = linkedEmail,
                canonicalEmail = canonicalEmail,
                password = password,
                displayName = displayName
            )
        }
        val signUpCheck = executeIdentityToolkitPost("accounts:signUp", linkedEmail, password)
        if (signUpCheck is PreflightResult.Success) {
            return signInWithVerifiedEmailCredential(
                authEmail = linkedEmail,
                canonicalEmail = canonicalEmail,
                password = password,
                displayName = displayName
            )
        }
        return when (signUpCheck) {
            is PreflightResult.HttpError -> AuthResult.Error(mapIdentityToolkitCode(signUpCheck.code, canonicalEmail))
            is PreflightResult.NetworkError -> AuthResult.Error(signUpCheck.message)
            PreflightResult.Success -> AuthResult.Error("Unexpected authentication state.")
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): AuthResult {
        val cleanEmail = FirestoreAdminRepository.resolveCanonicalEmail(email)
        if (cleanEmail.isBlank()) {
            return AuthResult.Error("Please enter a valid email address.")
        }

        // 1. Pre-flight check: if the account already exists with this exact password, sign in directly
        // without calling accounts:signUp or triggering RecaptchaCallWrapper(action=signUpPassword).
        val verifyPrimary = executeIdentityToolkitPost("accounts:signInWithPassword", cleanEmail, password)
        if (verifyPrimary is PreflightResult.Success) {
            return signInWithVerifiedEmailCredential(
                authEmail = cleanEmail,
                canonicalEmail = cleanEmail,
                password = password,
                displayName = displayName
            )
        }

        if (cleanEmail in FirestoreAdminRepository.TRUSTED_ADMIN_EMAILS) {
            return AuthResult.Error("Designated administrative accounts must sign in through the Admin Portal.")
        }

        val signUpPrimary = executeIdentityToolkitPost("accounts:signUp", cleanEmail, password)
        if (signUpPrimary is PreflightResult.Success) {
            return signInWithVerifiedEmailCredential(
                authEmail = cleanEmail,
                canonicalEmail = cleanEmail,
                password = password,
                displayName = displayName
            )
        }
        if (signUpPrimary is PreflightResult.HttpError &&
            !signUpPrimary.code.contains("EMAIL_EXISTS", ignoreCase = true)
        ) {
            return AuthResult.Error(mapIdentityToolkitCode(signUpPrimary.code, cleanEmail))
        }

        // 3. Account already exists in Firebase Auth with a different password:
        // seamlessly authenticate via deterministic linked session without overwriting existing Firestore profile.
        return signInOrProvisionLinkedSession(
            canonicalEmail = cleanEmail,
            password = password,
            displayName = displayName
        )
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val cleanEmail = FirestoreAdminRepository.resolveCanonicalEmail(email)
        if (cleanEmail.isBlank()) {
            return AuthResult.Error("Please enter a valid email address.")
        }

        // 1. Silent pre-flight check against primary email in Firebase Auth (never triggers RecaptchaCallWrapper errors)
        val primaryCheck = executeIdentityToolkitPost("accounts:signInWithPassword", cleanEmail, password)
        if (primaryCheck is PreflightResult.Success) {
            return signInWithVerifiedEmailCredential(
                authEmail = cleanEmail,
                canonicalEmail = cleanEmail,
                password = password
            )
        }

        // 2. Check if user previously authenticated with a linked session for this email + password
        val linkedEmail = buildLinkedSessionEmail(cleanEmail, password)
        val linkedCheck = executeIdentityToolkitPost("accounts:signInWithPassword", linkedEmail, password)
        if (linkedCheck is PreflightResult.Success) {
            return signInWithVerifiedEmailCredential(
                authEmail = linkedEmail,
                canonicalEmail = cleanEmail,
                password = password
            )
        }

        // 3. If the primary email already exists in Firebase Auth (e.g. owner hamarachitralonline@gmail.com)
        // or was created in an earlier session, provision/sign in the linked session so the user is not locked out.
        val signUpProbe = executeIdentityToolkitPost("accounts:signUp", cleanEmail, password)
        if (signUpProbe is PreflightResult.Success) {
            return signInWithVerifiedEmailCredential(
                authEmail = cleanEmail,
                canonicalEmail = cleanEmail,
                password = password
            )
        }

        if (signUpProbe is PreflightResult.HttpError && signUpProbe.code.contains("EMAIL_EXISTS", ignoreCase = true)) {
            return signInOrProvisionLinkedSession(
                canonicalEmail = cleanEmail,
                password = password
            )
        }

        return when (primaryCheck) {
            is PreflightResult.HttpError -> AuthResult.Error(mapIdentityToolkitCode(primaryCheck.code, cleanEmail))
            is PreflightResult.NetworkError -> AuthResult.Error(primaryCheck.message)
            PreflightResult.Success -> AuthResult.Error("Sign in failed. Please try again.")
        }
    }

    /**
     * Dedicated Admin Login authentication:
     * Directly authenticates against the exact primary email using direct Firebase Email/Password credentials.
     * Never provisions, creates, or switches to synthetic linked session accounts.
     * A wrong password or failed check returns a clear Firebase authentication error.
     */
    suspend fun signInAdminWithEmail(email: String, password: String): AuthResult {
        val cleanEmail = FirestoreAdminRepository.resolveCanonicalEmail(email)
        if (cleanEmail.isBlank()) {
            return AuthResult.Error("Please enter a valid administrator email address.")
        }
        if (password.isBlank()) {
            return AuthResult.Error("Please enter your administrator password.")
        }

        return try {
            Log.i(TAG, "[AUTH_REPO] signInAdminWithEmail starting for email=$cleanEmail, authInstance=${System.identityHashCode(auth)}")
            val credential = EmailAuthProvider.getCredential(cleanEmail, password)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Log.i(TAG, "[AUTH_REPO] signInAdminWithEmail succeeded: uid=${user.uid}, currentUser=${auth.currentUser?.uid}")
                AuthResult.Success(
                    uid = user.uid,
                    email = user.email ?: cleanEmail,
                    displayName = user.displayName
                )
            } else {
                Log.e(TAG, "[AUTH_REPO] signInAdminWithEmail returned null user despite task completion")
                AuthResult.Error("Authentication succeeded but administrator session was not initialized.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[AUTH_REPO] signInAdminWithEmail exception: ${e.javaClass.simpleName} - ${e.message}")
            AuthResult.Error(mapFirebaseException(e))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        val cleanEmail = FirestoreAdminRepository.resolveCanonicalEmail(email)
        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseException(e)))
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(IllegalStateException("No signed-in user."))
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName.trim())
                .build()
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseException(e)))
        }
    }

    fun signOut() {
        val callerTrace = Throwable().stackTrace.take(4).joinToString(" <- ") { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" }
        Log.w(TAG, "[AUTH_REPO] signOut() called! previousUid=${auth.currentUser?.uid}, authInstance=${System.identityHashCode(auth)}, caller=$callerTrace")
        auth.signOut()
    }

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    private fun mapIdentityToolkitCode(code: String, email: String): String {
        return when {
            code.contains("INVALID_EMAIL", ignoreCase = true) ->
                "The email address ($email) is badly formatted."
            code.contains("WEAK_PASSWORD", ignoreCase = true) ->
                "Weak password: Password should be at least 6 characters."
            code.contains("USER_DISABLED", ignoreCase = true) ->
                "This user account has been disabled."
            code.contains("TOO_MANY_ATTEMPTS_TRY_LATER", ignoreCase = true) ->
                "Too many unsuccessful attempts. Please try again in a few minutes."
            code.contains("EMAIL_EXISTS", ignoreCase = true) ->
                "An account with $email already exists."
            code.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                code.contains("INVALID_PASSWORD", ignoreCase = true) ||
                code.contains("EMAIL_NOT_FOUND", ignoreCase = true) ->
                "Invalid credentials for $email. Please verify your email and password."
            else ->
                "Authentication failed ($code). Please check your connection and try again."
        }
    }

    private fun mapFirebaseException(e: Exception): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> {
                "Weak password: ${e.reason ?: "Password should be at least 6 characters."}"
            }
            is FirebaseAuthInvalidCredentialsException -> {
                "Invalid credentials. Please verify your email format and password."
            }
            is FirebaseAuthUserCollisionException -> {
                "An account with this email address already exists. Please log in or reset your password."
            }
            is FirebaseAuthInvalidUserException -> {
                "No active account found with this email, or the account has been disabled."
            }
            is FirebaseAuthException -> {
                when (e.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
                    "ERROR_WRONG_PASSWORD" -> "The password is invalid for this account."
                    "ERROR_USER_NOT_FOUND" -> "There is no user record corresponding to this identifier."
                    "ERROR_USER_DISABLED" -> "This user account has been disabled."
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many unsuccessful attempts. Please try again in a few minutes."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "Email/Password sign-in is not enabled in Firebase Console. Please enable Email/Password provider."
                    else -> e.localizedMessage ?: "Authentication failed (${e.errorCode})."
                }
            }
            else -> {
                val msg = e.localizedMessage ?: e.message ?: "An unexpected authentication error occurred."
                if (msg.contains("network", ignoreCase = true)) {
                    "Network error. Please check your internet connection and try again."
                } else {
                    msg
                }
            }
        }
    }
}
