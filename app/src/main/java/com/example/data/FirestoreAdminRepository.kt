package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FirestoreAdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    enum class OwnerBootstrapStatus {
        ALREADY_EXISTS_UNCHANGED,
        CREATED_AND_VERIFIED
    }

    companion object {
        const val REPORTS_COLLECTION = "reports"
        const val ADMIN_ROLES_COLLECTION = "adminRoles"
        const val ADMIN_SESSIONS_COLLECTION = "adminSessions"
        const val AUDIT_LOGS_COLLECTION = "moderationAuditLogs"
        const val SUSPENSIONS_COLLECTION = "userSuspensions"
        const val VERIFICATION_REQUESTS_COLLECTION = "verificationRequests"
        const val VILLAGES_COLLECTION = "villages"
        const val OFFICIAL_ANNOUNCEMENTS_COLLECTION = "officialAnnouncements"
        const val TRENDING_TOPICS_COLLECTION = "trendingTopics"

        const val OWNER_BOOTSTRAP_UID = "isBdCPBYupN4SDJZ1yRiSZ8K0FF2"
        const val OWNER_BOOTSTRAP_EMAIL = "hamarachitralonline@gmail.com"
        const val OWNER_BOOTSTRAP_DEFAULT_NAME = "Yarkhoon Super Admin"
        const val SYSTEM_BOOTSTRAP_ACTOR = "system_bootstrap"

        const val MAX_FAILED_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes
        const val ADMIN_SESSION_TIMEOUT_MS = 4 * 60 * 60 * 1000L // 4 hours

        val TRUSTED_SUPER_ADMIN_EMAILS = setOf(
            "admin@yarkhoon.com",
            "hamarachitralonline@gmail.com"
        )

        val TRUSTED_MODERATOR_EMAILS = setOf(
            "moderator@yarkhoon.com"
        )

        val TRUSTED_ADMIN_EMAILS = TRUSTED_SUPER_ADMIN_EMAILS + TRUSTED_MODERATOR_EMAILS

        const val TAG = "YarkhoonAuthDebug"

        @Volatile
        var lastRoleResolutionTrace: String = "No role resolution executed yet."
            internal set

        @Volatile
        var lastAdminDocExistsOnServer: Boolean? = null
            internal set

        @Volatile
        var lastAdminDocRawData: Map<String, Any?>? = null
            internal set

        @Volatile
        var lastAdminSessionDocExistsOnServer: Boolean? = null
            internal set

        @Volatile
        var lastAdminSessionDocRawData: Map<String, Any?>? = null
            internal set

        var lastBootstrapError: String? = null
            internal set

        fun clearLastBootstrapErrorForTesting() {
            lastBootstrapError = null
            lastRoleResolutionTrace = "No role resolution executed yet."
            lastAdminDocExistsOnServer = null
            lastAdminDocRawData = null
            lastAdminSessionDocExistsOnServer = null
            lastAdminSessionDocRawData = null
        }

        /**
         * Validates that the caller's credentials strictly match the authorized owner UID and email.
         */
        fun validateOwnerBootstrapCredentials(activeUid: String, canonicalEmail: String): Result<Unit> {
            val cleanUid = activeUid.trim()
            val cleanEmail = resolveCanonicalEmail(canonicalEmail).trim().lowercase()
            return if (cleanUid == OWNER_BOOTSTRAP_UID && cleanEmail == OWNER_BOOTSTRAP_EMAIL) {
                Result.success(Unit)
            } else {
                val err = "Owner bootstrap is strictly restricted to the verified owner UID and email."
                lastBootstrapError = err
                Result.failure(IllegalAccessException(err))
            }
        }

        fun resolveCanonicalEmail(rawEmail: String?): String {
            val clean = rawEmail?.trim()?.lowercase().orEmpty()
            if (clean.isBlank()) return ""
            if (clean.startsWith("hamarachitralonline.gmail.com.") && clean.endsWith("@yarkhoon.com")) {
                return "hamarachitralonline@gmail.com"
            }
            if (clean.startsWith("admin.yarkhoon.com.") && clean.endsWith("@yarkhoon.com")) {
                return "admin@yarkhoon.com"
            }
            if (clean.startsWith("moderator.yarkhoon.com.") && clean.endsWith("@yarkhoon.com")) {
                return "moderator@yarkhoon.com"
            }
            val linkedRegex = Regex("^(.+)\\.([a-z0-9-]+\\.[a-z0-9.]+)\\.[0-9a-f]{8}@yarkhoon\\.com$")
            val match = linkedRegex.matchEntire(clean)
            if (match != null) {
                return "${match.groupValues[1]}@${match.groupValues[2]}"
            }
            return clean
        }

        private data class LoginAttemptState(
            val failedCount: Int = 0,
            val lastFailedAtMs: Long = 0L,
            val lockedUntilMs: Long = 0L
        )

        private val loginAttemptsMap = ConcurrentHashMap<String, LoginAttemptState>()

        fun checkLoginRateLimit(email: String, nowMs: Long = System.currentTimeMillis()): Result<Unit> {
            val normalized = resolveCanonicalEmail(email)
            if (normalized.isBlank()) return Result.success(Unit)
            val state = loginAttemptsMap[normalized] ?: return Result.success(Unit)
            if (state.lockedUntilMs > nowMs) {
                val remainingMinutes = ((state.lockedUntilMs - nowMs) / 60000L).coerceAtLeast(1L)
                return Result.failure(
                    IllegalStateException(
                        "Too many failed admin login attempts. Account login is temporarily locked for $remainingMinutes minute(s)."
                    )
                )
            }
            return Result.success(Unit)
        }

        fun recordLoginAttempt(email: String, success: Boolean, nowMs: Long = System.currentTimeMillis()) {
            val normalized = resolveCanonicalEmail(email)
            if (normalized.isBlank()) return
            if (success) {
                loginAttemptsMap.remove(normalized)
            } else {
                val current = loginAttemptsMap[normalized]
                val resetCount = if (current != null && (nowMs - current.lastFailedAtMs) > LOCKOUT_DURATION_MS) {
                    0
                } else {
                    current?.failedCount ?: 0
                }
                val newCount = resetCount + 1
                val lockedUntil = if (newCount >= MAX_FAILED_LOGIN_ATTEMPTS) nowMs + LOCKOUT_DURATION_MS else 0L
                loginAttemptsMap[normalized] = LoginAttemptState(
                    failedCount = newCount,
                    lastFailedAtMs = nowMs,
                    lockedUntilMs = lockedUntil
                )
            }
        }

        @Volatile
        var activeAdminSessionStartMs: Long = 0L
            private set

        fun startAdminSession(
            sessionStartMs: Long = System.currentTimeMillis(),
            rememberMe: Boolean = false,
            uid: String = "",
            email: String = ""
        ) {
            activeAdminSessionStartMs = sessionStartMs
            AdminSessionManager.startSession(uid, email, rememberMe, sessionStartMs)
        }

        data class VerifiedAdminSessionRole(
            val uid: String,
            val role: AdminRole,
            val verifiedAtMs: Long,
            val source: String
        )

        @Volatile
        var verifiedSessionRole: VerifiedAdminSessionRole? = null
            private set

        fun getValidCachedSessionRole(uid: String, nowMs: Long = System.currentTimeMillis()): AdminRole? {
            val cached = verifiedSessionRole ?: return null
            if (cached.uid != uid) return null
            if (!isAdminSessionValid(activeAdminSessionStartMs, nowMs)) return null
            if (AdminSessionManager.isSessionActive && AdminSessionManager.isSessionExpired(nowMs)) return null
            return cached.role
        }

        fun updateVerifiedSessionRole(uid: String, role: AdminRole, source: String = "authoritative_verification") {
            if (role != AdminRole.NONE) {
                verifiedSessionRole = VerifiedAdminSessionRole(
                    uid = uid,
                    role = role,
                    verifiedAtMs = System.currentTimeMillis(),
                    source = source
                )
            } else {
                verifiedSessionRole = null
            }
        }

        fun invalidateSessionRoleCache(uid: String? = null) {
            if (uid == null || verifiedSessionRole?.uid == uid) {
                verifiedSessionRole = null
            }
        }

        fun invalidateAdminSession() {
            activeAdminSessionStartMs = 0L
            invalidateSessionRoleCache()
            AdminSessionManager.invalidateAndClear()
        }

        fun clearRateLimitsForTesting() {
            loginAttemptsMap.clear()
            activeAdminSessionStartMs = 0L
            invalidateSessionRoleCache()
            AdminSessionManager.resetForTesting()
        }

        fun isAdminSessionValid(sessionStartMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean {
            if (sessionStartMs <= 0L) return false
            val withinWindow = (nowMs - sessionStartMs) in 0..ADMIN_SESSION_TIMEOUT_MS
            if (!withinWindow) return false
            if (AdminSessionManager.isSessionActive && AdminSessionManager.isSessionExpired(nowMs)) {
                return false
            }
            return true
        }

        fun verifyActiveAdminSessionWindow(nowMs: Long = System.currentTimeMillis()): Result<Unit> {
            val isExpired = !isAdminSessionValid(activeAdminSessionStartMs, nowMs) ||
                (AdminSessionManager.isSessionActive && AdminSessionManager.isSessionExpired(nowMs))
            if (isExpired) {
                activeAdminSessionStartMs = 0L
                invalidateAdminSession()
                return Result.failure(
                    IllegalStateException("Admin session expired (4-hour maximum session lifetime exceeded). Please sign in again.")
                )
            }
            if (AdminSessionManager.isSessionActive) {
                AdminSessionManager.recordActivity(nowMs)
            }
            return Result.success(Unit)
        }

        fun isDuplicateVillage(
            existingVillages: List<YarkhoonVillage>,
            candidateName: String,
            candidateDistrict: String,
            ignoreVillageId: String? = null
        ): Boolean {
            val normName = candidateName.trim().lowercase()
            val normDistrict = candidateDistrict.trim().lowercase()
            if (normName.isBlank()) return false
            return existingVillages.any { v ->
                v.id != ignoreVillageId &&
                    (v.normalizedName.ifBlank { v.name.trim().lowercase() } == normName) &&
                    v.district.trim().lowercase() == normDistrict
            }
        }
    }

    val currentUid: String?
        get() = auth.currentUser?.uid

    val currentEmail: String?
        get() = resolveCanonicalEmail(auth.currentUser?.email).takeIf { it.isNotBlank() }

    val lastBootstrapError: String?
        get() = Companion.lastBootstrapError

    fun clearLastBootstrapErrorForTesting() {
        Companion.clearLastBootstrapErrorForTesting()
    }

    /**
     * Attempts the atomic initial Super Admin bootstrap for the designated project owner.
     * Surfaces explicit Result failure instead of swallowing errors.
     */
    suspend fun attemptOwnerBootstrap(): Result<OwnerBootstrapStatus> {
        val user = auth.currentUser ?: run {
            val err = "No authenticated Firebase user found."
            Companion.lastBootstrapError = err
            return Result.failure(IllegalStateException(err))
        }
        val normalizedEmail = resolveCanonicalEmail(user.email)
        val result = bootstrapOwnerAdminRolesIfNeeded(
            activeUid = user.uid,
            canonicalEmail = normalizedEmail,
            displayName = user.displayName
        )
        if (result.isFailure) {
            Companion.lastBootstrapError = result.exceptionOrNull()?.message
        }
        return result
    }

    internal suspend fun bootstrapOwnerAdminRolesIfNeeded(
        activeUid: String,
        canonicalEmail: String,
        displayName: String?
    ): Result<OwnerBootstrapStatus> {
        Log.i(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] Entry: activeUid=$activeUid, canonicalEmail=$canonicalEmail, displayName=$displayName")
        // Strict guard: only bootstrap the exact verified owner account
        val credentialCheck = validateOwnerBootstrapCredentials(activeUid, canonicalEmail)
        if (credentialCheck.isFailure) {
            val err = credentialCheck.exceptionOrNull()?.message
                ?: "Owner bootstrap is strictly restricted to the verified owner UID and email."
            Companion.lastBootstrapError = err
            Log.e(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] FAILED: credential check rejected ($err)")
            return Result.failure(credentialCheck.exceptionOrNull() ?: IllegalAccessException(err))
        }

        val docRef = firestore.collection(ADMIN_ROLES_COLLECTION).document(OWNER_BOOTSTRAP_UID)

        return try {
            // 1. Check whether adminRoles/{uid} already exists on the server
            Log.d(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] Querying docRef on server: adminRoles/$OWNER_BOOTSTRAP_UID")
            val existingDoc = docRef.get(Source.SERVER).await()
            lastAdminDocExistsOnServer = existingDoc.exists()
            lastAdminDocRawData = existingDoc.data
            Log.i(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] Server read result: exists=${existingDoc.exists()}")
            if (existingDoc.exists()) {
                Log.i(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] Document already exists on server unchanged.")
                // 2. If it exists, do NOT modify, merge, overwrite, or replace any existing fields
                return Result.success(OwnerBootstrapStatus.ALREADY_EXISTS_UNCHANGED)
            }

            // 3 & 4. Prepare exact verified schema and execute atomic create-only transaction
            val now = System.currentTimeMillis()
            val resolvedName = displayName?.trim()?.takeIf { it.isNotBlank() } ?: OWNER_BOOTSTRAP_DEFAULT_NAME
            val expectedPermissions = listOf("ALL")

            val bootstrapData = mapOf(
                "uid" to OWNER_BOOTSTRAP_UID,
                "email" to OWNER_BOOTSTRAP_EMAIL,
                "name" to resolvedName,
                "role" to "super_admin",
                "enabled" to true,
                "isActive" to true,
                "permissions" to expectedPermissions,
                "assignedBy" to SYSTEM_BOOTSTRAP_ACTOR,
                "grantedBy" to SYSTEM_BOOTSTRAP_ACTOR,
                "assignedAt" to now,
                "grantedAt" to now
            )

            Log.d(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] Executing transaction to create initial adminRoles document...")
            val createdInTx = firestore.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                if (snap.exists()) {
                    // Concurrent sign-in already created the document; do not overwrite
                    false
                } else {
                    transaction.set(docRef, bootstrapData)
                    true
                }
            }.await()
            Log.i(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] Transaction finished: createdInTx=$createdInTx")

            if (!createdInTx) {
                return Result.success(OwnerBootstrapStatus.ALREADY_EXISTS_UNCHANGED)
            }

            // 5. Read the newly created document back from the server and verify every required field
            val verifiedSnap = docRef.get(Source.SERVER).await()
            lastAdminDocExistsOnServer = verifiedSnap.exists()
            lastAdminDocRawData = verifiedSnap.data
            if (!verifiedSnap.exists()) {
                val err = "Bootstrap verification failed: adminRoles/$OWNER_BOOTSTRAP_UID does not exist on server after transaction."
                Log.e(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] $err")
                return Result.failure(IllegalStateException(err))
            }

            val vUid = verifiedSnap.getString("uid")
            val vEmail = verifiedSnap.getString("email")
            val vName = verifiedSnap.getString("name")
            val vRole = verifiedSnap.getString("role")
            val vEnabled = verifiedSnap.getBoolean("enabled")
            val vIsActive = verifiedSnap.getBoolean("isActive")
            val vPermissions = (verifiedSnap.get("permissions") as? List<*>)?.mapNotNull { it?.toString() }
            val vAssignedBy = verifiedSnap.getString("assignedBy")
            val vGrantedBy = verifiedSnap.getString("grantedBy")
            val vAssignedAt = verifiedSnap.getLong("assignedAt")
            val vGrantedAt = verifiedSnap.getLong("grantedAt")

            val isVerified = vUid == OWNER_BOOTSTRAP_UID &&
                vEmail == OWNER_BOOTSTRAP_EMAIL &&
                !vName.isNullOrBlank() &&
                vRole == "super_admin" &&
                vEnabled == true &&
                vIsActive == true &&
                vPermissions == expectedPermissions &&
                vAssignedBy == SYSTEM_BOOTSTRAP_ACTOR &&
                vGrantedBy == SYSTEM_BOOTSTRAP_ACTOR &&
                vAssignedAt != null && vAssignedAt > 0L &&
                vGrantedAt != null && vGrantedAt > 0L

            if (!isVerified) {
                val err = "Bootstrap verification failed: server read-back schema mismatch for adminRoles/$OWNER_BOOTSTRAP_UID."
                Log.e(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] $err")
                return Result.failure(IllegalStateException(err))
            }

            Log.i(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] SUCCESS: Super Admin bootstrap created and verified on server.")
            Result.success(OwnerBootstrapStatus.CREATED_AND_VERIFIED)
        } catch (e: Exception) {
            // 6. Handle permission errors and other failures safely
            val msg = e.localizedMessage ?: e.message ?: "Firestore operation failed."
            Companion.lastBootstrapError = msg
            Log.e(TAG, "[TRACE 4: bootstrapOwnerAdminRolesIfNeeded] FAILED with exception: ${e.javaClass.simpleName} - $msg", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves the exact AdminRole for the currently authenticated user returning Result<AdminRole>
     * to preserve distinction between genuine authorization rejection and temporary network/Firestore errors:
     * 1. Verifies caller is not suspended (suspended admins forfeit all privileges immediately).
     * 2. Checks active validated session-level role cache (if forceRefreshToken = false).
     * 3. Checks Firestore `adminRoles/{uid}` document (respects explicit revocation `enabled == false` / `isActive == false` / `role == "none"`).
     *    Tolerant to schema variations (role, roleKey, adminRole; string or boolean enabled/isActive).
     *    Strictly verifies owner identity: if existing doc exists for verified owner UID + email, resolves Super Admin.
     * 4. Checks Firebase Auth custom claims (`super_admin`, `admin`, `moderator`) on ID token.
     * 5. Fallback bootstrap for verified owner if document does not exist yet.
     * 6. If network error occurs during lookup and active session role cache exists, preserves cache.
     *    If no cache exists, surfaces the network failure instead of false role downgrade.
     */
    suspend fun getAdminRoleResult(forceRefreshToken: Boolean = false): Result<AdminRole> {
        val authHash = System.identityHashCode(auth)
        val appName = try { auth.app.name } catch (_: Exception) { "[DEFAULT]" }
        val user = auth.currentUser ?: run {
            val trace = "No authenticated Firebase user found (auth.currentUser is null, authInstance=$authHash, app=$appName)."
            Log.w(TAG, "[TRACE 3: getAdminRoleResult] $trace")
            lastRoleResolutionTrace = trace
            return Result.failure(IllegalAccessException("Authentication required: $trace"))
        }
        val now = System.currentTimeMillis()
        val normalizedEmail = resolveCanonicalEmail(user.email)
        Log.i(
            TAG,
            "[TRACE 3: getAdminRoleResult] Entry: UID=${user.uid}, Email=${user.email} (canonical=$normalizedEmail), forceRefreshToken=$forceRefreshToken, authInstance=$authHash, app=$appName"
        )

        // Strict owner UID verification: if email matches designated owner, UID must match OWNER_BOOTSTRAP_UID
        if (normalizedEmail.equals(OWNER_BOOTSTRAP_EMAIL, ignoreCase = true) && user.uid != OWNER_BOOTSTRAP_UID) {
            val trace = "Security rejection: Owner email ($normalizedEmail) used with non-matching UID (${user.uid} != $OWNER_BOOTSTRAP_UID)."
            Log.e(TAG, "[TRACE 3: getAdminRoleResult] $trace")
            lastRoleResolutionTrace = trace
            invalidateSessionRoleCache(user.uid)
            return Result.success(AdminRole.NONE)
        }

        // 1. Suspended administrators immediately forfeit all privileges and invalidate cache
        val suspensionResult = checkUserSuspension(user.uid)
        if (suspensionResult.isSuccess) {
            val suspension = suspensionResult.getOrNull()
            if (suspension != null && suspension.isCurrentlySuspended) {
                val trace = "User is suspended (reason=${suspension.suspensionReason}). Returning AdminRole.NONE."
                Log.w(TAG, "[TRACE 3: getAdminRoleResult] $trace")
                lastRoleResolutionTrace = trace
                invalidateSessionRoleCache(user.uid)
                return Result.success(AdminRole.NONE)
            }
        }

        // 2. Check active validated session cache if not forcing refresh
        if (!forceRefreshToken) {
            val cachedRole = getValidCachedSessionRole(user.uid, now)
            if (cachedRole != null && cachedRole != AdminRole.NONE) {
                val trace = "Resolved role from active verified session cache: ${cachedRole.displayName} (UID=${user.uid})."
                Log.i(TAG, "[TRACE 3: getAdminRoleResult] $trace")
                lastRoleResolutionTrace = trace
                return Result.success(cachedRole)
            }
        }

        // 3. Check Firestore adminRoles collection first so revocations take immediate effect
        var adminDocExists = false
        var networkError: Exception? = null
        try {
            Log.d(TAG, "[TRACE 3: getAdminRoleResult] Fetching Firestore document adminRoles/${user.uid}...")
            val adminDoc = firestore.collection(ADMIN_ROLES_COLLECTION)
                .document(user.uid)
                .get()
                .await()

            lastAdminDocExistsOnServer = adminDoc.exists()
            lastAdminDocRawData = adminDoc.data
            Log.i(TAG, "[TRACE 3: getAdminRoleResult] adminRoles/${user.uid} get() returned exists=${adminDoc.exists()}")

            if (adminDoc.exists()) {
                adminDocExists = true
                val data = adminDoc.data
                val roleStr = adminDoc.getString("role")
                    ?: (data?.get("role") as? String)
                    ?: (data?.get("roleKey") as? String)
                    ?: (data?.get("adminRole") as? String)

                val isEnabled = (data?.get("enabled") as? Boolean)
                    ?: (data?.get("enabled") as? String)?.toBooleanStrictOrNull()
                    ?: adminDoc.getBoolean("enabled")
                    ?: true
                val isActive = (data?.get("isActive") as? Boolean)
                    ?: (data?.get("isActive") as? String)?.toBooleanStrictOrNull()
                    ?: adminDoc.getBoolean("isActive")
                    ?: true

                Log.d(TAG, "[TRACE 3: getAdminRoleResult] Document data: roleStr='$roleStr', isEnabled=$isEnabled, isActive=$isActive")

                // If explicitly disabled/revoked in Firestore, deny access immediately and purge cache
                if (!isEnabled || !isActive || roleStr.equals("none", ignoreCase = true)) {
                    val trace = "Role explicitly revoked or disabled in Firestore (enabled=$isEnabled, active=$isActive, role='$roleStr'). Returning AdminRole.NONE."
                    Log.w(TAG, "[TRACE 3: getAdminRoleResult] $trace")
                    lastRoleResolutionTrace = trace
                    invalidateSessionRoleCache(user.uid)
                    return Result.success(AdminRole.NONE)
                }

                var parsedRole = AdminRole.fromString(roleStr)

                // If document exists for the strictly verified owner UID & email, and is not revoked
                if (parsedRole == AdminRole.NONE && user.uid == OWNER_BOOTSTRAP_UID && normalizedEmail == OWNER_BOOTSTRAP_EMAIL) {
                    parsedRole = AdminRole.SUPER_ADMIN
                }

                if (parsedRole != AdminRole.NONE) {
                    if (activeAdminSessionStartMs == 0L) {
                        activeAdminSessionStartMs = now
                    }
                    updateVerifiedSessionRole(user.uid, parsedRole, "firestore_adminRoles")
                    val trace = "Resolved role from Firestore document: ${parsedRole.displayName} (UID=${user.uid})."
                    Log.i(TAG, "[TRACE 3: getAdminRoleResult] $trace")
                    lastRoleResolutionTrace = trace
                    return Result.success(parsedRole)
                }
            }
        } catch (e: Exception) {
            networkError = e
            Log.e(TAG, "[TRACE 3: getAdminRoleResult] Firestore lookup exception: ${e.javaClass.simpleName} - ${e.message}", e)
        }

        // 4. Check custom claims on ID token
        try {
            Log.d(TAG, "[TRACE 3: getAdminRoleResult] Inspecting Firebase ID Token claims...")
            val tokenResult = user.getIdToken(forceRefreshToken).await()
            val authTimeMs = tokenResult.authTimestamp * 1000L
            if (activeAdminSessionStartMs == 0L && authTimeMs > 0L) {
                activeAdminSessionStartMs = authTimeMs
            }
            val claims = tokenResult.claims
            val claimRole = (claims["role"] as? String)?.trim()
            val parsedClaimRole = when {
                claims["super_admin"] == true || claimRole.equals("SUPER_ADMIN", ignoreCase = true) || claimRole.equals("super_admin", ignoreCase = true) -> AdminRole.SUPER_ADMIN
                claims["admin"] == true || claimRole.equals("ADMIN", ignoreCase = true) -> AdminRole.ADMIN
                claims["moderator"] == true || claimRole.equals("MODERATOR", ignoreCase = true) -> AdminRole.MODERATOR
                else -> AdminRole.NONE
            }
            if (parsedClaimRole != AdminRole.NONE) {
                updateVerifiedSessionRole(user.uid, parsedClaimRole, "token_claims")
                val trace = "Resolved role from ID Token claims: ${parsedClaimRole.displayName} (UID=${user.uid})."
                Log.i(TAG, "[TRACE 3: getAdminRoleResult] $trace")
                lastRoleResolutionTrace = trace
                return Result.success(parsedClaimRole)
            }
        } catch (e: Exception) {
            Log.w(TAG, "[TRACE 3: getAdminRoleResult] Token claims inspection failed: ${e.message}")
        }

        // 5. Trusted Owner Super Admin bootstrap ONLY when adminRoles/{uid} does not yet exist on the server
        val isOwnerIdentity = user.uid == OWNER_BOOTSTRAP_UID && normalizedEmail == OWNER_BOOTSTRAP_EMAIL
        Log.d(TAG, "[TRACE 3: getAdminRoleResult] Owner bootstrap eligibility: adminDocExists=$adminDocExists, networkError=$networkError, isOwnerIdentity=$isOwnerIdentity")
        if (!adminDocExists && networkError == null && isOwnerIdentity) {
            Log.i(TAG, "[TRACE 3: getAdminRoleResult] Triggering bootstrapOwnerAdminRolesIfNeeded()...")
            val bootstrapResult = bootstrapOwnerAdminRolesIfNeeded(
                activeUid = user.uid,
                canonicalEmail = normalizedEmail,
                displayName = user.displayName
            )
            if (bootstrapResult.isSuccess) {
                if (activeAdminSessionStartMs == 0L) {
                    activeAdminSessionStartMs = now
                }
                updateVerifiedSessionRole(user.uid, AdminRole.SUPER_ADMIN, "owner_bootstrap")
                val trace = "Resolved Super Admin via successful Owner Bootstrap flow (UID=${user.uid})."
                Log.i(TAG, "[TRACE 3: getAdminRoleResult] $trace")
                lastRoleResolutionTrace = trace
                return Result.success(AdminRole.SUPER_ADMIN)
            }
            val bootstrapErr = bootstrapResult.exceptionOrNull()?.message ?: Companion.lastBootstrapError ?: "Unknown bootstrap error"
            Companion.lastBootstrapError = bootstrapErr
            val trace = "Owner Bootstrap attempt failed: $bootstrapErr. Returning AdminRole.NONE."
            Log.e(TAG, "[TRACE 3: getAdminRoleResult] $trace")
            lastRoleResolutionTrace = trace
            return Result.success(AdminRole.NONE)
        }

        // 6. Handle temporary network / Firestore errors:
        if (networkError != null) {
            // If an unexpired verified session role was previously established, preserve it without downgrade
            val cachedRole = getValidCachedSessionRole(user.uid, now)
            if (cachedRole != null && cachedRole != AdminRole.NONE) {
                val trace = "Network error during lookup; preserved cached role ${cachedRole.displayName}."
                Log.w(TAG, "[TRACE 3: getAdminRoleResult] $trace")
                lastRoleResolutionTrace = trace
                return Result.success(cachedRole)
            }
            // If no verified session exists, surface the network failure instead of falsely downgrading to Ordinary User
            val trace = "Network error communicating with Firestore: ${networkError.message}"
            Log.e(TAG, "[TRACE 3: getAdminRoleResult] $trace")
            lastRoleResolutionTrace = trace
            return Result.failure(networkError)
        }

        val trace = "Fallback: Account has no adminRoles document, no custom claims, and is not eligible for bootstrap. Returning AdminRole.NONE."
        Log.w(TAG, "[TRACE 3: getAdminRoleResult] $trace")
        lastRoleResolutionTrace = trace
        invalidateSessionRoleCache(user.uid)
        return Result.success(AdminRole.NONE)
    }

    /**
     * Resolves the exact AdminRole for the currently authenticated user.
     * Backwards-compatible convenience wrapper around getAdminRoleResult.
     */
    suspend fun getAdminRole(forceRefreshToken: Boolean = false): AdminRole {
        return getAdminRoleResult(forceRefreshToken).getOrDefault(AdminRole.NONE)
    }

    /**
     * Resolves the Firebase Auth token authentication timestamp in milliseconds for accurate session age tracking across restarts.
     */
    suspend fun getFirebaseAuthTimestampMs(): Long {
        val user = auth.currentUser ?: return 0L
        return try {
            val tokenResult = user.getIdToken(false).await()
            val authTimeMs = tokenResult.authTimestamp * 1000L
            if (authTimeMs > 0L) authTimeMs else activeAdminSessionStartMs
        } catch (_: Exception) {
            activeAdminSessionStartMs
        }
    }

    /**
     * Enforces both 4-hour administrative session validity AND role-based access control (RBAC)
     * before any privileged administrative API operation can execute.
     * Rejects immediately if Firebase Authentication currentUser is null.
     */
    suspend fun requireAuthorizedAdminSession(
        requiredCapabilityName: String = "Admin access",
        capabilityCheck: (AdminRole) -> Boolean = { it.canAccessAdminPanel }
    ): Result<AdminRole> {
        val now = System.currentTimeMillis()
        val authHash = System.identityHashCode(auth)
        val activeUser = auth.currentUser
        Log.i(
            TAG,
            "[TRACE 5: requireAuthorizedAdminSession] Capability check requested: '$requiredCapabilityName', activeUid=${activeUser?.uid ?: "NULL"}, authInstance=$authHash, activeSessionStartMs=$activeAdminSessionStartMs"
        )
        if (activeUser == null) {
            val trace = "No authenticated Firebase user found (auth.currentUser is null, authInstance=$authHash)."
            Log.e(TAG, "[TRACE 5: requireAuthorizedAdminSession] ABORTED: $trace")
            lastRoleResolutionTrace = trace
            invalidateAdminSession()
            return Result.failure(
                IllegalAccessException("Authentication required: Firebase session is missing. Please sign in to the Admin Portal.")
            )
        }
        val isExpired = (activeAdminSessionStartMs > 0L && !isAdminSessionValid(activeAdminSessionStartMs, now)) ||
            (AdminSessionManager.isSessionActive && AdminSessionManager.isSessionExpired(now))
        if (isExpired) {
            Log.w(TAG, "[TRACE 5: requireAuthorizedAdminSession] Session is expired due to 4 hours of inactivity.")
            invalidateAdminSession()
            try { auth.signOut() } catch (_: Exception) {}
            return Result.failure(
                IllegalAccessException("Admin session expired due to 4 hours of inactivity. Please log in again.")
            )
        }

        val roleResult = getAdminRoleResult()
        if (roleResult.isFailure) {
            // Surface actual network/connection or missing-auth failure without converting to an Ordinary User downgrade
            Log.e(TAG, "[TRACE 5: requireAuthorizedAdminSession] Role result failure: ${roleResult.exceptionOrNull()?.message}")
            return Result.failure(
                roleResult.exceptionOrNull() ?: IllegalStateException("Failed to verify administrative authorization due to connection error.")
            )
        }

        val role = roleResult.getOrDefault(AdminRole.NONE)
        val passed = role.canAccessAdminPanel && capabilityCheck(role)
        Log.i(TAG, "[TRACE 5: requireAuthorizedAdminSession] Evaluated role: ${role.displayName}, passedCapability=$passed")
        if (!passed) {
            Log.w(TAG, "[TRACE 5: requireAuthorizedAdminSession] DENIED: required capability '$requiredCapabilityName', current role is ${role.displayName}")
            return Result.failure(
                IllegalAccessException("Unauthorized: $requiredCapabilityName required (current role: ${role.displayName}).")
            )
        }

        if (AdminSessionManager.isSessionActive && AdminSessionManager.isSessionExpired(now)) {
            invalidateAdminSession()
            try { auth.signOut() } catch (_: Exception) {}
            return Result.failure(
                IllegalAccessException("Admin session expired due to 4 hours of inactivity. Please log in again.")
            )
        }
        // Meaningful authenticated admin activity performed -> reset inactivity timer locally and touch server session
        AdminSessionManager.recordActivity(now)
        try {
            currentUid?.let { uid ->
                firestore.collection(ADMIN_SESSIONS_COLLECTION).document(uid).update(
                    mapOf("lastActivityAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
                )
            }
        } catch (_: Exception) {}
        return Result.success(role)
    }

    suspend fun createOrUpdateServerAdminSession(
        uid: String,
        email: String,
        rememberMe: Boolean
    ): Result<Unit> {
        return try {
            val sessionDoc = mapOf(
                "uid" to uid,
                "email" to email,
                "rememberMe" to rememberMe,
                "isActive" to true,
                "startedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "lastActivityAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection(ADMIN_SESSIONS_COLLECTION).document(uid).set(sessionDoc, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun touchServerAdminActivity(uid: String? = currentUid): Result<Unit> {
        val targetUid = uid ?: return Result.failure(IllegalStateException("No admin UID"))
        return try {
            firestore.collection(ADMIN_SESSIONS_COLLECTION).document(targetUid).update(
                mapOf("lastActivityAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun terminateServerAdminSession(uid: String? = currentUid): Result<Unit> {
        val targetUid = uid ?: return Result.success(Unit)
        return try {
            firestore.collection(ADMIN_SESSIONS_COLLECTION).document(targetUid).update(
                mapOf("isActive" to false)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if current user is an authorized admin or moderator with a valid session.
     */
    suspend fun verifyAdminAuthorization(): Boolean {
        return requireAuthorizedAdminSession().isSuccess
    }

    suspend fun recordAdminLoginAudit() {
        recordAuditLog(
            actionType = ModerationActionType.ADMIN_LOGIN,
            targetType = "admin_session",
            targetId = currentUid ?: "unknown",
            targetUserId = currentUid,
            reason = "Authorized administrator logged into the Admin Control Panel."
        )
    }

    /**
     * Computes real-time administrative statistics directly from Firestore collections.
     */
    suspend fun getDashboardStats(): Result<AdminDashboardStats> {
        val authCheck = requireAuthorizedAdminSession("Admin Dashboard access") { it.canAccessAdminPanel }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized access to admin statistics."))
        }
        return try {
            val usersSnap = try { firestore.collection(FirestoreUserRepository.USERS_COLLECTION).get().await() } catch (_: Exception) { null }
            val postsSnap = try { firestore.collection(FirestorePostRepository.POSTS_COLLECTION).get().await() } catch (_: Exception) { null }
            val verificationsSnap = try { firestore.collection(VERIFICATION_REQUESTS_COLLECTION).get().await() } catch (_: Exception) { null }
            val reportsSnap = try { firestore.collection(REPORTS_COLLECTION).get().await() } catch (_: Exception) { null }
            val villagesSnap = try { firestore.collection(VILLAGES_COLLECTION).get().await() } catch (_: Exception) { null }

            val userDocs = usersSnap?.documents ?: emptyList()
            val totalUsers = userDocs.size
            val suspendedUsers = userDocs.count { (it.getBoolean("isSuspended") ?: false) }
            val activeUsers = (totalUsers - suspendedUsers).coerceAtLeast(0)
            val verifiedAccounts = userDocs.count { (it.getBoolean("isVerified") ?: false) }

            val now = System.currentTimeMillis()
            val postDocs = postsSnap?.documents ?: emptyList()
            val totalPosts = postDocs.size
            var pendingPosts = 0
            var approvedPosts = 0
            var rejectedPosts = 0
            var featuredPosts = 0
            var promotedPosts = 0
            var trendingPosts = 0

            for (doc in postDocs) {
                val status = (doc.getString("moderationStatus") ?: "APPROVED").uppercase()
                val isHidden = doc.getBoolean("isModeratedHidden") ?: false
                when {
                    status == "PENDING" -> pendingPosts++
                    status == "REJECTED" || isHidden -> rejectedPosts++
                    else -> approvedPosts++
                }
                val isFeatured = doc.getBoolean("isFeatured") ?: false
                val featuredUntil = (doc.get("featuredUntil") as? Number)?.toLong() ?: 0L
                if (isFeatured && (featuredUntil == 0L || featuredUntil > now)) {
                    featuredPosts++
                }
                val isPromoted = doc.getBoolean("isPromoted") ?: false
                val promoEnd = (doc.get("promotionEndDate") as? Number)?.toLong() ?: 0L
                if (isPromoted && (promoEnd == 0L || promoEnd > now)) {
                    promotedPosts++
                }
                if (doc.getBoolean("isTrendingLabeled") ?: false) {
                    trendingPosts++
                }
            }

            val pendingVerifications = verificationsSnap?.documents?.count {
                (it.getString("status") ?: "PENDING").equals("PENDING", ignoreCase = true)
            } ?: 0

            val pendingReports = reportsSnap?.documents?.count {
                val st = it.getString("status") ?: "PENDING"
                st.equals("PENDING", ignoreCase = true) || st.equals("UNDER_REVIEW", ignoreCase = true)
            } ?: 0

            val totalVillages = villagesSnap?.documents?.size ?: 0

            Result.success(
                AdminDashboardStats(
                    totalRegisteredUsers = totalUsers,
                    activeUsers = activeUsers,
                    suspendedUsers = suspendedUsers,
                    totalPosts = totalPosts,
                    pendingPosts = pendingPosts,
                    approvedPosts = approvedPosts,
                    rejectedPosts = rejectedPosts,
                    totalVerifiedAccounts = verifiedAccounts,
                    totalFeaturedPosts = featuredPosts,
                    totalPromotedPosts = promotedPosts,
                    trendingPostsCount = trendingPosts,
                    pendingVerificationRequests = pendingVerifications,
                    reportedContentCount = pendingReports,
                    totalVillagesCount = totalVillages
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads all registered users from Firestore for administrative User Management & Blue Tick Verification.
     */
    suspend fun getAllUsersForAdmin(): Result<List<User>> {
        if (!verifyAdminAuthorization()) {
            return Result.failure(IllegalAccessException("Unauthorized: Admin access required."))
        }
        return try {
            val snapshot = firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val uid = (data["uid"] as? String)?.ifBlank { doc.id } ?: doc.id
                val name = (data["name"] as? String)?.ifBlank { "Community Member" } ?: "Community Member"
                val username = (data["username"] as? String)?.ifBlank { "@${uid.take(8)}" } ?: "@${uid.take(8)}"
                val handle = if (username.startsWith("@")) username else "@$username"
                User(
                    id = uid,
                    name = name,
                    handle = handle,
                    email = (data["email"] as? String).orEmpty(),
                    avatarUrl = (data["avatarUrl"] as? String).orEmpty(),
                    coverUrl = data["coverUrl"] as? String,
                    bio = (data["bio"] as? String).orEmpty(),
                    location = (data["location"] as? String).orEmpty(),
                    website = (data["website"] as? String).orEmpty(),
                    followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                    postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                    isVerified = (data["isVerified"] as? Boolean) ?: false,
                    isPrivate = (data["isPrivate"] as? Boolean) ?: false,
                    isSuspended = (data["isSuspended"] as? Boolean) ?: false,
                    suspendedUntil = (data["suspendedUntil"] as? Number)?.toLong() ?: 0L,
                    suspensionReason = (data["suspensionReason"] as? String).orEmpty(),
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                )
            }.sortedByDescending { it.createdAt }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads all posts (including pending, approved, rejected, hidden, featured, and promoted) for admin management.
     */
    suspend fun getAllPostsForAdmin(postRepository: FirestorePostRepository): Result<List<Post>> {
        if (!verifyAdminAuthorization()) {
            return Result.failure(IllegalAccessException("Unauthorized: Admin access required."))
        }
        return try {
            val snapshot = firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                .get()
                .await()
            val uid = currentUid.orEmpty()
            val posts = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val authorId = (data["authorId"] as? String) ?: (data["userId"] as? String) ?: "unknown"
                val authorName = (data["authorName"] as? String) ?: "Member"
                val authorHandle = (data["authorUsername"] as? String) ?: "@$authorId"
                val authorAvatar = (data["authorAvatarUrl"] as? String).orEmpty()
                val authorVerified = (data["authorIsVerified"] as? Boolean) ?: false
                val text = (data["text"] as? String).orEmpty()
                val mediaUrls = (data["mediaUrls"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val timestamp = FirestorePostRepository.parsePostTimestamp(data["timestamp"] ?: data["createdAt"])
                Post(
                    id = doc.id,
                    author = User(
                        id = authorId,
                        name = authorName,
                        handle = authorHandle,
                        avatarUrl = authorAvatar,
                        isVerified = authorVerified
                    ),
                    text = text,
                    mediaUrls = mediaUrls,
                    tags = tags,
                    location = data["location"] as? String,
                    likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0,
                    commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0,
                    repostsCount = (data["repostsCount"] as? Number)?.toInt() ?: 0,
                    sharesCount = (data["sharesCount"] as? Number)?.toInt() ?: 0,
                    createdAt = (data["createdAt"] as? String) ?: "Recently",
                    timestamp = timestamp,
                    moderationStatus = (data["moderationStatus"] as? String) ?: "APPROVED",
                    isModeratedHidden = (data["isModeratedHidden"] as? Boolean) ?: false,
                    isPinned = (data["isPinned"] as? Boolean) ?: false,
                    isFeatured = (data["isFeatured"] as? Boolean) ?: false,
                    featuredScheduledAt = (data["featuredScheduledAt"] as? Number)?.toLong() ?: 0L,
                    featuredUntil = (data["featuredUntil"] as? Number)?.toLong() ?: 0L,
                    isTrendingLabeled = (data["isTrendingLabeled"] as? Boolean) ?: false,
                    isPromoted = (data["isPromoted"] as? Boolean) ?: false,
                    promotionStartDate = (data["promotionStartDate"] as? Number)?.toLong() ?: 0L,
                    promotionEndDate = (data["promotionEndDate"] as? Number)?.toLong() ?: 0L,
                    promotionTargetCategory = (data["promotionTargetCategory"] as? String).orEmpty(),
                    promotionTargetLocation = (data["promotionTargetLocation"] as? String).orEmpty(),
                    impressionsCount = (data["impressionsCount"] as? Number)?.toInt() ?: 0
                )
            }.sortedWith(compareByDescending<Post> { it.isPinned }.thenByDescending { it.timestamp })
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Approves or rejects a post in Post Management.
     */
    suspend fun setPostModerationStatus(
        postId: String,
        status: String, // "APPROVED", "REJECTED", "PENDING"
        reason: String
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Post moderation") { it.canModerateContent }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Moderator or Admin role required."))
        }
        val normalizedStatus = status.uppercase()
        val isHidden = normalizedStatus == "REJECTED"
        return try {
            firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "moderationStatus" to normalizedStatus,
                        "isModeratedHidden" to isHidden,
                        "moderatedAt" to System.currentTimeMillis(),
                        "moderatedBy" to currentUid,
                        "moderationReason" to reason.trim()
                    )
                ).await()

            recordAuditLog(
                actionType = if (normalizedStatus == "APPROVED") ModerationActionType.APPROVE_POST else ModerationActionType.REJECT_POST,
                targetType = "post",
                targetId = postId,
                targetUserId = null,
                reason = reason.ifBlank { "Post status set to $normalizedStatus" }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pins or unpins a post to the top of the community feed.
     */
    suspend fun setPostPinned(postId: String, isPinned: Boolean, reason: String = ""): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Pinning posts") { it.canManageFeaturedAndPromoted }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to pin posts."))
        }
        return try {
            firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                .document(postId)
                .update("isPinned", isPinned)
                .await()

            recordAuditLog(
                actionType = if (isPinned) ModerationActionType.PIN_POST else ModerationActionType.UNPIN_POST,
                targetType = "post",
                targetId = postId,
                targetUserId = null,
                reason = reason.ifBlank { if (isPinned) "Post pinned to top of feed." else "Post unpinned from feed." }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Features or unfeatures a post, with optional schedule start & expiration timestamps.
     * Does NOT fabricate fake likes, views, or engagement metrics.
     */
    suspend fun setPostFeatured(
        postId: String,
        isFeatured: Boolean,
        scheduledAt: Long = 0L,
        featuredUntil: Long = 0L,
        reason: String = ""
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Managing featured posts") { it.canManageFeaturedAndPromoted }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to manage featured posts."))
        }
        return try {
            firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "isFeatured" to isFeatured,
                        "featuredScheduledAt" to if (isFeatured) scheduledAt else 0L,
                        "featuredUntil" to if (isFeatured) featuredUntil else 0L
                    )
                ).await()

            recordAuditLog(
                actionType = if (isFeatured) ModerationActionType.FEATURE_POST else ModerationActionType.UNFEATURE_POST,
                targetType = "post",
                targetId = postId,
                targetUserId = null,
                reason = reason.ifBlank { if (isFeatured) "Post marked as Featured." else "Post removed from Featured." }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds or removes a Trending label on a post.
     */
    suspend fun setPostTrendingLabel(postId: String, isTrending: Boolean, reason: String = ""): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Managing trending labels") { it.canManageFeaturedAndPromoted }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to manage trending labels."))
        }
        return try {
            firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                .document(postId)
                .update("isTrendingLabeled", isTrending)
                .await()

            recordAuditLog(
                actionType = ModerationActionType.SET_TRENDING_POST,
                targetType = "post",
                targetId = postId,
                targetUserId = null,
                reason = reason.ifBlank { if (isTrending) "Trending label added to post." else "Trending label removed from post." }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Configures or ends post promotion with start/end dates and target category/location.
     * Transparently labels promoted content without generating artificial engagement.
     */
    suspend fun setPostPromoted(
        postId: String,
        isPromoted: Boolean,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long = 0L,
        targetCategory: String = "",
        targetLocation: String = "",
        reason: String = ""
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Managing promoted posts") { it.canManageFeaturedAndPromoted }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to manage promoted posts."))
        }
        return try {
            firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "isPromoted" to isPromoted,
                        "promotionStartDate" to if (isPromoted) startDate else 0L,
                        "promotionEndDate" to if (isPromoted) endDate else 0L,
                        "promotionTargetCategory" to if (isPromoted) targetCategory.trim() else "",
                        "promotionTargetLocation" to if (isPromoted) targetLocation.trim() else ""
                    )
                ).await()

            recordAuditLog(
                actionType = if (isPromoted) ModerationActionType.PROMOTE_POST else ModerationActionType.END_PROMOTION,
                targetType = "post",
                targetId = postId,
                targetUserId = null,
                reason = reason.ifBlank {
                    if (isPromoted) "Post promoted (Target: $targetLocation / $targetCategory)" else "Post promotion ended."
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Grants or removes Blue Tick verification directly on a user account and updates their authored posts.
     */
    suspend fun setUserVerification(
        userId: String,
        isVerified: Boolean,
        reason: String
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Blue Tick verification management") { it.canManageVerification }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to manage Blue Tick verification."))
        }
        return try {
            firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "isVerified" to isVerified,
                        "verifiedUpdatedAt" to System.currentTimeMillis(),
                        "verifiedBy" to currentUid
                    )
                ).await()

            // Propagate authorIsVerified to user's existing posts so feed badges update immediately
            try {
                val userPosts = firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                    .whereEqualTo("authorId", userId)
                    .get()
                    .await()
                for (doc in userPosts.documents) {
                    doc.reference.update("authorIsVerified", isVerified).await()
                }
            } catch (_: Exception) {}

            recordAuditLog(
                actionType = if (isVerified) ModerationActionType.APPROVE_VERIFICATION else ModerationActionType.REVOKE_VERIFICATION,
                targetType = "user",
                targetId = userId,
                targetUserId = userId,
                reason = reason.ifBlank {
                    if (isVerified) "Blue Tick verification badge granted." else "Blue Tick verification badge revoked."
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits a Blue Tick verification request by a community member.
     */
    suspend fun submitVerificationRequest(
        category: String,
        supportingInfo: String,
        userName: String,
        userHandle: String,
        userAvatarUrl: String
    ): Result<VerificationRequest> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Sign in required to request verification."))
        return try {
            val reqId = "vreq_${user.uid}"
            val now = System.currentTimeMillis()
            val req = VerificationRequest(
                id = reqId,
                userId = user.uid,
                userName = userName,
                userHandle = userHandle,
                userEmail = user.email.orEmpty(),
                userAvatarUrl = userAvatarUrl,
                category = category.trim().ifBlank { "Community Member" },
                supportingInfo = supportingInfo.trim(),
                status = VerificationRequestStatus.PENDING,
                requestedAt = now
            )
            val data = mapOf(
                "id" to req.id,
                "userId" to req.userId,
                "userName" to req.userName,
                "userHandle" to req.userHandle,
                "userEmail" to req.userEmail,
                "userAvatarUrl" to req.userAvatarUrl,
                "category" to req.category,
                "supportingInfo" to req.supportingInfo,
                "status" to req.status.name,
                "requestedAt" to req.requestedAt,
                "reviewedAt" to null,
                "reviewedBy" to null,
                "reviewNotes" to ""
            )
            firestore.collection(VERIFICATION_REQUESTS_COLLECTION)
                .document(reqId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(req)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads all Blue Tick verification requests for admin review.
     */
    suspend fun getVerificationRequests(): Result<List<VerificationRequest>> {
        if (!verifyAdminAuthorization()) {
            return Result.failure(IllegalAccessException("Unauthorized: Admin access required."))
        }
        return try {
            val snapshot = firestore.collection(VERIFICATION_REQUESTS_COLLECTION)
                .get()
                .await()
            val requests = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val statusStr = (data["status"] as? String) ?: VerificationRequestStatus.PENDING.name
                val status = try { VerificationRequestStatus.valueOf(statusStr) } catch (_: Exception) { VerificationRequestStatus.PENDING }
                VerificationRequest(
                    id = doc.id,
                    userId = (data["userId"] as? String).orEmpty(),
                    userName = (data["userName"] as? String) ?: "Community Member",
                    userHandle = (data["userHandle"] as? String).orEmpty(),
                    userEmail = (data["userEmail"] as? String).orEmpty(),
                    userAvatarUrl = (data["userAvatarUrl"] as? String).orEmpty(),
                    category = (data["category"] as? String) ?: "Community Member",
                    supportingInfo = (data["supportingInfo"] as? String).orEmpty(),
                    status = status,
                    requestedAt = (data["requestedAt"] as? Number)?.toLong() ?: 0L,
                    reviewedAt = (data["reviewedAt"] as? Number)?.toLong(),
                    reviewedBy = data["reviewedBy"] as? String,
                    reviewNotes = (data["reviewNotes"] as? String).orEmpty()
                )
            }.sortedByDescending { it.requestedAt }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Approves or rejects a Blue Tick verification request.
     */
    suspend fun reviewVerificationRequest(
        requestId: String,
        userId: String,
        approve: Boolean,
        reviewNotes: String
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Reviewing verification requests") { it.canManageVerification }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to review verification requests."))
        }
        val now = System.currentTimeMillis()
        val newStatus = if (approve) VerificationRequestStatus.APPROVED else VerificationRequestStatus.REJECTED
        return try {
            firestore.collection(VERIFICATION_REQUESTS_COLLECTION)
                .document(requestId)
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "reviewedAt" to now,
                        "reviewedBy" to currentUid,
                        "reviewNotes" to reviewNotes.trim()
                    )
                ).await()

            if (userId.isNotBlank()) {
                setUserVerification(
                    userId = userId,
                    isVerified = approve,
                    reason = reviewNotes.ifBlank {
                        if (approve) "Verification request $requestId approved." else "Verification request $requestId rejected."
                    }
                )
            } else {
                recordAuditLog(
                    actionType = if (approve) ModerationActionType.APPROVE_VERIFICATION else ModerationActionType.REJECT_VERIFICATION,
                    targetType = "verification_request",
                    targetId = requestId,
                    targetUserId = null,
                    reason = reviewNotes.ifBlank { "Verification request ${newStatus.name}" }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads curated Trending Topics / Local News / Village Updates.
     */
    suspend fun getCuratedTrendingTopics(): Result<List<CuratedTrendingTopic>> {
        return try {
            val snapshot = firestore.collection(TRENDING_TOPICS_COLLECTION)
                .get()
                .await()
            val topics = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                CuratedTrendingTopic(
                    id = doc.id,
                    tag = (data["tag"] as? String).orEmpty(),
                    headline = (data["headline"] as? String).orEmpty(),
                    category = (data["category"] as? String) ?: "Community Discussion",
                    villageName = (data["villageName"] as? String).orEmpty(),
                    isPinned = (data["isPinned"] as? Boolean) ?: false,
                    postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                )
            }.sortedWith(compareByDescending<CuratedTrendingTopic> { it.isPinned }.thenByDescending { it.updatedAt })
            Result.success(topics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveCuratedTrendingTopic(
        id: String?,
        tag: String,
        headline: String,
        category: String,
        villageName: String,
        isPinned: Boolean
    ): Result<CuratedTrendingTopic> {
        val authCheck = requireAuthorizedAdminSession("Curating trending topics") { it.canManageFeaturedAndPromoted }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to curate trending topics."))
        }
        return try {
            val topicId = id?.takeIf { it.isNotBlank() } ?: "trend_${System.currentTimeMillis()}"
            val cleanTag = if (tag.trim().startsWith("#")) tag.trim() else "#${tag.trim()}"
            val now = System.currentTimeMillis()
            val topic = CuratedTrendingTopic(
                id = topicId,
                tag = cleanTag,
                headline = headline.trim(),
                category = category.trim().ifBlank { "Local News" },
                villageName = villageName.trim(),
                isPinned = isPinned,
                updatedAt = now
            )
            firestore.collection(TRENDING_TOPICS_COLLECTION)
                .document(topicId)
                .set(
                    mapOf(
                        "id" to topic.id,
                        "tag" to topic.tag,
                        "headline" to topic.headline,
                        "category" to topic.category,
                        "villageName" to topic.villageName,
                        "isPinned" to topic.isPinned,
                        "updatedAt" to topic.updatedAt
                    ),
                    SetOptions.merge()
                ).await()

            recordAuditLog(
                actionType = ModerationActionType.SET_TRENDING_POST,
                targetType = "trending_topic",
                targetId = topicId,
                targetUserId = null,
                reason = "Curated trending topic saved: ${topic.tag} (${topic.category})"
            )
            Result.success(topic)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCuratedTrendingTopic(topicId: String): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Deleting trending topics") { it.canManageFeaturedAndPromoted }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required."))
        }
        return try {
            firestore.collection(TRENDING_TOPICS_COLLECTION).document(topicId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Village Management: Loads all villages in Yarkhoon / Chitral valley.
     */
    suspend fun getVillages(): Result<List<YarkhoonVillage>> {
        return try {
            val snapshot = firestore.collection(VILLAGES_COLLECTION)
                .get()
                .await()
            val villages = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val name = (data["name"] as? String).orEmpty()
                YarkhoonVillage(
                    id = doc.id,
                    name = name,
                    normalizedName = (data["normalizedName"] as? String) ?: name.trim().lowercase(),
                    tehsil = (data["tehsil"] as? String) ?: "Mastuj",
                    district = (data["district"] as? String) ?: "Upper Chitral",
                    description = (data["description"] as? String).orEmpty(),
                    populationEstimate = (data["populationEstimate"] as? String).orEmpty(),
                    isActive = (data["isActive"] as? Boolean) ?: true,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
                    updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                )
            }.sortedBy { it.name.lowercase() }
            Result.success(villages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds or updates a village, strictly preventing duplicate village entries in the same district.
     */
    suspend fun saveVillage(
        villageId: String?,
        name: String,
        tehsil: String,
        district: String,
        description: String,
        populationEstimate: String
    ): Result<YarkhoonVillage> {
        val authCheck = requireAuthorizedAdminSession("Managing villages") { it.canManageVillages }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to manage villages."))
        }
        val cleanName = name.trim()
        val cleanDistrict = district.trim().ifBlank { "Upper Chitral" }
        val cleanTehsil = tehsil.trim().ifBlank { "Mastuj" }
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Village name cannot be empty."))
        }

        return try {
            val existing = getVillages().getOrDefault(emptyList())
            if (isDuplicateVillage(existing, cleanName, cleanDistrict, ignoreVillageId = villageId)) {
                return Result.failure(
                    IllegalStateException("Duplicate entry prevented: Village '$cleanName' already exists in $cleanDistrict.")
                )
            }

            val isUpdate = !villageId.isNullOrBlank()
            val docId = villageId?.takeIf { it.isNotBlank() } ?: "vil_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
            val now = System.currentTimeMillis()
            val village = YarkhoonVillage(
                id = docId,
                name = cleanName,
                normalizedName = cleanName.lowercase(),
                tehsil = cleanTehsil,
                district = cleanDistrict,
                description = description.trim(),
                populationEstimate = populationEstimate.trim(),
                isActive = true,
                createdAt = now,
                updatedAt = now
            )

            firestore.collection(VILLAGES_COLLECTION)
                .document(docId)
                .set(
                    mapOf(
                        "id" to village.id,
                        "name" to village.name,
                        "normalizedName" to village.normalizedName,
                        "tehsil" to village.tehsil,
                        "district" to village.district,
                        "description" to village.description,
                        "populationEstimate" to village.populationEstimate,
                        "isActive" to true,
                        "updatedAt" to now,
                        "createdAt" to now
                    ),
                    SetOptions.merge()
                ).await()

            recordAuditLog(
                actionType = if (isUpdate) ModerationActionType.UPDATE_VILLAGE else ModerationActionType.ADD_VILLAGE,
                targetType = "village",
                targetId = docId,
                targetUserId = null,
                reason = "${if (isUpdate) "Updated" else "Added"} village '${village.name}' ($cleanTehsil, $cleanDistrict)"
            )
            Result.success(village)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a village record from Firestore.
     */
    suspend fun deleteVillage(villageId: String, villageName: String): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Deleting villages") { it.canManageVillages }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to delete villages."))
        }
        return try {
            firestore.collection(VILLAGES_COLLECTION).document(villageId).delete().await()
            recordAuditLog(
                actionType = ModerationActionType.DELETE_VILLAGE,
                targetType = "village",
                targetId = villageId,
                targetUserId = null,
                reason = "Deleted village '$villageName' ($villageId)"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads official announcements and emergency broadcasts.
     */
    suspend fun getOfficialAnnouncements(): Result<List<OfficialAnnouncement>> {
        return try {
            val snapshot = firestore.collection(OFFICIAL_ANNOUNCEMENTS_COLLECTION)
                .get()
                .await()
            val list = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                OfficialAnnouncement(
                    id = doc.id,
                    title = (data["title"] as? String).orEmpty(),
                    body = (data["body"] as? String).orEmpty(),
                    category = (data["category"] as? String) ?: "OFFICIAL_ANNOUNCEMENT",
                    targetVillageOrLocation = (data["targetVillageOrLocation"] as? String) ?: "All Yarkhoon & Chitral",
                    sentByUid = (data["sentByUid"] as? String).orEmpty(),
                    sentByEmail = (data["sentByEmail"] as? String).orEmpty(),
                    sentAt = (data["sentAt"] as? Number)?.toLong() ?: 0L,
                    priority = (data["priority"] as? String) ?: "HIGH"
                )
            }.sortedByDescending { it.sentAt }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Publishes an official Yarkhoon announcement / breaking news / emergency alert and delivers an in-app notification.
     */
    suspend fun publishOfficialAnnouncement(
        title: String,
        body: String,
        category: String, // "BREAKING_NEWS", "EMERGENCY_ALERT", "COMMUNITY_UPDATE", "OFFICIAL_ANNOUNCEMENT"
        targetVillageOrLocation: String,
        priority: String
    ): Result<OfficialAnnouncement> {
        val authCheck = requireAuthorizedAdminSession("Broadcasting official announcements") { it.canBroadcastNotifications }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin role required to broadcast notifications."))
        }
        if (title.isBlank() || body.isBlank()) {
            return Result.failure(IllegalArgumentException("Announcement title and message body are required."))
        }
        val now = System.currentTimeMillis()
        val id = "ann_${now}_${UUID.randomUUID().toString().take(5)}"
        val announcement = OfficialAnnouncement(
            id = id,
            title = title.trim(),
            body = body.trim(),
            category = category,
            targetVillageOrLocation = targetVillageOrLocation.trim().ifBlank { "All Yarkhoon & Chitral" },
            sentByUid = currentUid.orEmpty(),
            sentByEmail = currentEmail.orEmpty(),
            sentAt = now,
            priority = priority
        )
        return try {
            firestore.collection(OFFICIAL_ANNOUNCEMENTS_COLLECTION)
                .document(id)
                .set(
                    mapOf(
                        "id" to announcement.id,
                        "title" to announcement.title,
                        "body" to announcement.body,
                        "category" to announcement.category,
                        "targetVillageOrLocation" to announcement.targetVillageOrLocation,
                        "sentByUid" to announcement.sentByUid,
                        "sentByEmail" to announcement.sentByEmail,
                        "sentAt" to announcement.sentAt,
                        "priority" to announcement.priority
                    )
                ).await()

            recordAuditLog(
                actionType = ModerationActionType.SEND_ANNOUNCEMENT,
                targetType = "announcement",
                targetId = id,
                targetUserId = null,
                reason = "[${announcement.category}] ${announcement.title} -> ${announcement.targetVillageOrLocation}"
            )
            Result.success(announcement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Super Admin Only: Loads all administrator and moderator role assignments.
     */
    suspend fun getAdminAccounts(): Result<List<AdminAccountRecord>> {
        val authCheck = requireAuthorizedAdminSession("Viewing admin accounts") { it.canAccessAdminPanel }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Admin access required."))
        }
        return try {
            val snapshot = firestore.collection(ADMIN_ROLES_COLLECTION)
                .get()
                .await()
            val accounts = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val roleStr = (data["role"] as? String) ?: "admin"
                val role = AdminRole.fromString(roleStr)
                val perms = (data["permissions"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val isEnabled = (data["enabled"] as? Boolean) ?: (data["isActive"] as? Boolean) ?: true
                AdminAccountRecord(
                    uid = (data["uid"] as? String)?.ifBlank { doc.id } ?: doc.id,
                    email = (data["email"] as? String).orEmpty(),
                    name = (data["name"] as? String) ?: "Administrator",
                    role = if (role == AdminRole.NONE) AdminRole.ADMIN else role,
                    enabled = isEnabled,
                    grantedAt = (data["grantedAt"] as? Number)?.toLong() ?: 0L,
                    grantedBy = (data["grantedBy"] as? String).orEmpty(),
                    permissions = perms
                )
            }.sortedByDescending { it.grantedAt }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Super Admin Only: Assigns or updates an Admin or Moderator role for a specific user UID.
     */
    suspend fun assignAdminRole(
        targetUid: String,
        targetEmail: String,
        targetName: String,
        role: AdminRole,
        permissions: List<String> = emptyList()
    ): Result<AdminAccountRecord> {
        val authCheck = requireAuthorizedAdminSession("Assigning admin roles") { it.canManageAdmins }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Only a Super Admin can create or modify administrator roles."))
        }
        if (targetUid.isBlank() || targetEmail.isBlank()) {
            return Result.failure(IllegalArgumentException("Target user UID and email are required."))
        }
        val now = System.currentTimeMillis()
        val record = AdminAccountRecord(
            uid = targetUid.trim(),
            email = targetEmail.trim().lowercase(),
            name = targetName.trim().ifBlank { "Administrator" },
            role = role,
            enabled = role != AdminRole.NONE,
            grantedAt = now,
            grantedBy = currentEmail.orEmpty(),
            permissions = permissions
        )
        return try {
            firestore.collection(ADMIN_ROLES_COLLECTION)
                .document(record.uid)
                .set(
                    mapOf(
                        "uid" to record.uid,
                        "email" to record.email,
                        "name" to record.name,
                        "role" to record.role.name.lowercase(),
                        "enabled" to record.enabled,
                        "isActive" to record.enabled,
                        "grantedAt" to record.grantedAt,
                        "grantedBy" to record.grantedBy,
                        "permissions" to record.permissions
                    ),
                    SetOptions.merge()
                ).await()

            recordAuditLog(
                actionType = ModerationActionType.ASSIGN_ADMIN_ROLE,
                targetType = "admin_role",
                targetId = record.uid,
                targetUserId = record.uid,
                reason = "Assigned ${record.role.displayName} role to ${record.email}"
            )
            invalidateSessionRoleCache(record.uid)
            Result.success(record)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Super Admin Only: Revokes administrative privileges from a target UID.
     */
    suspend fun revokeAdminRole(targetUid: String, reason: String): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Revoking admin roles") { it.canManageAdmins }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Only a Super Admin can revoke administrator roles."))
        }
        return try {
            firestore.collection(ADMIN_ROLES_COLLECTION)
                .document(targetUid)
                .update(
                    mapOf(
                        "enabled" to false,
                        "isActive" to false,
                        "role" to "none",
                        "revokedAt" to System.currentTimeMillis(),
                        "revokedBy" to currentUid
                    )
                ).await()

            invalidateSessionRoleCache(targetUid)

            recordAuditLog(
                actionType = ModerationActionType.REVOKE_ADMIN_ROLE,
                targetType = "admin_role",
                targetId = targetUid,
                targetUserId = targetUid,
                reason = reason.ifBlank { "Administrative privileges revoked by Super Admin." }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Submits a new user report into the `reports` collection.
     * Prevents accidental duplicate submissions if the same reporter recently reported the same content.
     */
    suspend fun submitReport(
        contentType: ReportContentType,
        contentId: String,
        reason: ReportReason,
        description: String,
        reportedUserId: String?,
        reportedUserName: String?,
        contentSummary: String = ""
    ): Result<ContentReport> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("You must be signed in to submit a report."))
        val reporterId = user.uid
        val reporterName = user.displayName?.ifBlank { "Community Member" } ?: "Community Member"
        val reporterHandle = "@${user.email?.substringBefore("@") ?: reporterId.take(8)}"

        return try {
            // Duplicate prevention check: same reporter, content, within last 24h
            val existing = firestore.collection(REPORTS_COLLECTION)
                .whereEqualTo("reporterId", reporterId)
                .whereEqualTo("contentId", contentId)
                .limit(1)
                .get()
                .await()

            if (!existing.isEmpty) {
                val doc = existing.documents.first()
                val existingReport = parseReportDocument(doc)
                return Result.success(existingReport)
            }

            val reportId = "rep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val now = System.currentTimeMillis()

            val reportData = hashMapOf<String, Any?>(
                "reportId" to reportId,
                "reporterId" to reporterId,
                "reporterName" to reporterName,
                "reporterHandle" to reporterHandle,
                "reportedUserId" to reportedUserId,
                "reportedUserName" to reportedUserName,
                "contentType" to contentType.name,
                "contentId" to contentId,
                "contentSummary" to contentSummary.take(200),
                "reason" to reason.name,
                "description" to description.trim(),
                "status" to ReportStatus.PENDING.name,
                "createdAt" to now,
                "reviewedAt" to null,
                "reviewedBy" to null,
                "resolutionNotes" to null
            )

            firestore.collection(REPORTS_COLLECTION)
                .document(reportId)
                .set(reportData)
                .await()

            val created = ContentReport(
                id = reportId,
                reportId = reportId,
                reporterId = reporterId,
                reporterName = reporterName,
                reporterHandle = reporterHandle,
                reportedUserId = reportedUserId,
                reportedUserName = reportedUserName,
                contentType = contentType,
                contentId = contentId,
                contentSummary = contentSummary,
                reason = reason,
                description = description.trim(),
                status = ReportStatus.PENDING,
                createdAt = now
            )

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads all reports for administrator review.
     */
    suspend fun getReports(statusFilter: ReportStatus? = null): Result<List<ContentReport>> {
        val authCheck = requireAuthorizedAdminSession("Viewing content reports") { it.canModerateContent }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Moderator or Admin role required."))
        }
        return try {
            var query: Query = firestore.collection(REPORTS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)

            if (statusFilter != null) {
                query = query.whereEqualTo("status", statusFilter.name)
            }

            val snapshot = query.get().await()
            val reports = snapshot.documents.map { parseReportDocument(it) }
            Result.success(reports)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads moderation audit log entries.
     */
    suspend fun getAuditLogs(limit: Long = 50): Result<List<ModerationAuditLog>> {
        val authCheck = requireAuthorizedAdminSession("Viewing audit logs") { it.canModerateContent }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Moderator or Admin role required."))
        }
        return try {
            val snapshot = firestore.collection(AUDIT_LOGS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val logs = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val typeStr = data["actionType"] as? String ?: return@mapNotNull null
                val actionType = try { ModerationActionType.valueOf(typeStr) } catch (_: Exception) { ModerationActionType.DISMISS_REPORT }

                ModerationAuditLog(
                    id = doc.id,
                    adminId = (data["adminId"] as? String).orEmpty(),
                    adminEmail = (data["adminEmail"] as? String).orEmpty(),
                    actionType = actionType,
                    targetType = (data["targetType"] as? String).orEmpty(),
                    targetId = (data["targetId"] as? String).orEmpty(),
                    targetUserId = data["targetUserId"] as? String,
                    reason = (data["reason"] as? String).orEmpty(),
                    durationDays = (data["durationDays"] as? Number)?.toInt(),
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dismisses a report with a recorded admin reason.
     */
    suspend fun dismissReport(reportId: String, reason: String): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Dismissing reports") { it.canModerateContent }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Moderator or Admin role required."))
        }
        val adminUid = currentUid ?: return Result.failure(IllegalStateException("Admin sign-in required."))
        val now = System.currentTimeMillis()

        return try {
            firestore.collection(REPORTS_COLLECTION).document(reportId).update(
                mapOf(
                    "status" to ReportStatus.DISMISSED.name,
                    "reviewedAt" to now,
                    "reviewedBy" to adminUid,
                    "resolutionNotes" to reason.trim()
                )
            ).await()

            recordAuditLog(
                actionType = ModerationActionType.DISMISS_REPORT,
                targetType = "report",
                targetId = reportId,
                targetUserId = null,
                reason = reason.ifBlank { "Report reviewed and dismissed as false positive." }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Soft-deletes / hides content according to moderation policy.
     */
    suspend fun hideReportedContent(
        reportId: String?,
        contentType: ReportContentType,
        contentId: String,
        reason: String
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Hiding reported content") { it.canModerateContent }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Moderator or Admin role required."))
        }
        val adminUid = currentUid ?: return Result.failure(IllegalStateException("Admin sign-in required."))
        val now = System.currentTimeMillis()

        return try {
            when (contentType) {
                ReportContentType.POST -> {
                    firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                        .document(contentId)
                        .update(
                            mapOf(
                                "isModeratedHidden" to true,
                                "moderationStatus" to "REJECTED",
                                "moderatedAt" to now,
                                "moderatedBy" to adminUid,
                                "moderationReason" to reason.trim()
                            )
                        ).await()
                }
                ReportContentType.REEL -> {
                    firestore.collection(FirestoreReelRepository.REELS_COLLECTION)
                        .document(contentId)
                        .update(
                            mapOf(
                                "isModeratedHidden" to true,
                                "moderatedAt" to now,
                                "moderatedBy" to adminUid,
                                "moderationReason" to reason.trim()
                            )
                        ).await()
                }
                ReportContentType.BUSINESS_LISTING -> {
                    firestore.collection(FirestoreBusinessRepository.BUSINESS_COLLECTION)
                        .document(contentId)
                        .update(
                            mapOf(
                                "status" to "hidden",
                                "isModeratedHidden" to true,
                                "moderatedAt" to now,
                                "moderatedBy" to adminUid,
                                "moderationReason" to reason.trim()
                            )
                        ).await()
                }
                ReportContentType.MARKETPLACE_LISTING -> {
                    firestore.collection(FirestoreMarketplaceRepository.MARKETPLACE_COLLECTION)
                        .document(contentId)
                        .update(
                            mapOf(
                                "status" to "hidden",
                                "isModeratedHidden" to true,
                                "moderatedAt" to now,
                                "moderatedBy" to adminUid,
                                "moderationReason" to reason.trim()
                            )
                        ).await()
                }
                ReportContentType.COMMENT -> {
                    // Handled with comment parent or flagged in comment document
                }
                ReportContentType.USER_PROFILE -> {
                    // Profile hiding via suspension
                }
            }

            // If triggered from a report, update report status to RESOLVED
            if (!reportId.isNullOrBlank()) {
                firestore.collection(REPORTS_COLLECTION).document(reportId).update(
                    mapOf(
                        "status" to ReportStatus.RESOLVED.name,
                        "reviewedAt" to now,
                        "reviewedBy" to adminUid,
                        "resolutionNotes" to "Content hidden: ${reason.trim()}"
                    )
                ).await()
            }

            recordAuditLog(
                actionType = ModerationActionType.HIDE_CONTENT,
                targetType = contentType.name.lowercase(),
                targetId = contentId,
                targetUserId = null,
                reason = reason.ifBlank { "Content hidden by administrator for policy violation." }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores previously hidden content.
     */
    suspend fun restoreContent(
        contentType: ReportContentType,
        contentId: String,
        reason: String
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Restoring moderated content") { it.canModerateContent }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Moderator or Admin role required."))
        }
        val adminUid = currentUid ?: return Result.failure(IllegalStateException("Admin sign-in required."))

        return try {
            when (contentType) {
                ReportContentType.POST -> {
                    firestore.collection(FirestorePostRepository.POSTS_COLLECTION)
                        .document(contentId)
                        .update(mapOf("isModeratedHidden" to false, "moderationStatus" to "APPROVED"))
                        .await()
                }
                ReportContentType.REEL -> {
                    firestore.collection(FirestoreReelRepository.REELS_COLLECTION)
                        .document(contentId)
                        .update(mapOf("isModeratedHidden" to false))
                        .await()
                }
                ReportContentType.BUSINESS_LISTING -> {
                    firestore.collection(FirestoreBusinessRepository.BUSINESS_COLLECTION)
                        .document(contentId)
                        .update(mapOf("status" to "active", "isModeratedHidden" to false))
                        .await()
                }
                ReportContentType.MARKETPLACE_LISTING -> {
                    firestore.collection(FirestoreMarketplaceRepository.MARKETPLACE_COLLECTION)
                        .document(contentId)
                        .update(mapOf("status" to "active", "isModeratedHidden" to false))
                        .await()
                }
                else -> {}
            }

            recordAuditLog(
                actionType = ModerationActionType.RESTORE_CONTENT,
                targetType = contentType.name.lowercase(),
                targetId = contentId,
                targetUserId = null,
                reason = reason.ifBlank { "Content restored after administrative review." }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Suspends a user account for a defined number of days (or indefinite if durationDays == 0).
     * Strictly requires Full Admin (`canManageUsers`) — Moderators cannot suspend user accounts.
     */
    suspend fun suspendUserAccount(
        userId: String,
        durationDays: Int,
        reason: String,
        associatedReportId: String? = null
    ): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Suspending user accounts") { it.canManageUsers }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Full Admin role required to suspend user accounts."))
        }
        val adminUid = currentUid ?: return Result.failure(IllegalStateException("Admin sign-in required."))
        val now = System.currentTimeMillis()
        val suspendedUntil = if (durationDays > 0) now + (durationDays.toLong() * 24L * 60L * 60L * 1000L) else 0L

        return try {
            val suspensionData = hashMapOf<String, Any?>(
                "userId" to userId,
                "isSuspended" to true,
                "suspensionReason" to reason.trim(),
                "suspendedAt" to now,
                "suspendedUntil" to suspendedUntil,
                "suspendedBy" to adminUid,
                "durationDays" to durationDays
            )

            // Update userSuspensions collection
            firestore.collection(SUSPENSIONS_COLLECTION)
                .document(userId)
                .set(suspensionData, SetOptions.merge())
                .await()

            // Also flag on user document for fast client-side & security rule access
            firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "isSuspended" to true,
                        "suspendedUntil" to suspendedUntil,
                        "suspensionReason" to reason.trim()
                    )
                ).await()

            if (!associatedReportId.isNullOrBlank()) {
                firestore.collection(REPORTS_COLLECTION).document(associatedReportId).update(
                    mapOf(
                        "status" to ReportStatus.RESOLVED.name,
                        "reviewedAt" to now,
                        "reviewedBy" to adminUid,
                        "resolutionNotes" to "User suspended for $durationDays days: ${reason.trim()}"
                    )
                ).await()
            }

            recordAuditLog(
                actionType = ModerationActionType.SUSPEND_USER,
                targetType = "user",
                targetId = userId,
                targetUserId = userId,
                reason = reason.trim(),
                durationDays = durationDays
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reinstates a previously suspended user account.
     * Strictly requires Full Admin (`canManageUsers`) — Moderators cannot reinstate user accounts.
     */
    suspend fun reinstateUserAccount(userId: String, reason: String): Result<Unit> {
        val authCheck = requireAuthorizedAdminSession("Reinstating user accounts") { it.canManageUsers }
        if (authCheck.isFailure) {
            return Result.failure(authCheck.exceptionOrNull() ?: IllegalAccessException("Unauthorized: Full Admin role required to reinstate user accounts."))
        }
        val adminUid = currentUid ?: return Result.failure(IllegalStateException("Admin sign-in required."))

        return try {
            firestore.collection(SUSPENSIONS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "isSuspended" to false,
                        "reinstatedAt" to System.currentTimeMillis(),
                        "reinstatedBy" to adminUid,
                        "reinstateReason" to reason.trim()
                    )
                ).await()

            firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "isSuspended" to false,
                        "suspendedUntil" to 0L,
                        "suspensionReason" to ""
                    )
                ).await()

            recordAuditLog(
                actionType = ModerationActionType.REINSTATE_USER,
                targetType = "user",
                targetId = userId,
                targetUserId = userId,
                reason = reason.ifBlank { "Account reinstated by administrator." }
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a user is currently suspended.
     */
    suspend fun checkUserSuspension(userId: String): Result<UserSuspensionInfo> {
        return try {
            val doc = firestore.collection(SUSPENSIONS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.success(UserSuspensionInfo(userId = userId, isSuspended = false))
            }

            val data = doc.data ?: return Result.success(UserSuspensionInfo(userId = userId, isSuspended = false))
            val isSuspended = (data["isSuspended"] as? Boolean) ?: false
            val suspendedUntil = (data["suspendedUntil"] as? Number)?.toLong() ?: 0L
            val suspendedAt = (data["suspendedAt"] as? Number)?.toLong() ?: 0L
            val reason = (data["suspensionReason"] as? String).orEmpty()
            val suspendedBy = (data["suspendedBy"] as? String).orEmpty()

            val info = UserSuspensionInfo(
                userId = userId,
                isSuspended = isSuspended,
                suspensionReason = reason,
                suspendedAt = suspendedAt,
                suspendedUntil = suspendedUntil,
                suspendedBy = suspendedBy
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recordAuditLog(
        actionType: ModerationActionType,
        targetType: String,
        targetId: String,
        targetUserId: String?,
        reason: String,
        durationDays: Int? = null
    ) {
        val adminUid = currentUid ?: return
        val adminEmail = currentEmail ?: "admin@yarkhoon.com"
        val logId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        try {
            val logData = hashMapOf<String, Any?>(
                "id" to logId,
                "adminId" to adminUid,
                "adminEmail" to adminEmail,
                "actionType" to actionType.name,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetUserId" to targetUserId,
                "reason" to reason,
                "durationDays" to durationDays,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection(AUDIT_LOGS_COLLECTION).document(logId).set(logData).await()
        } catch (_: Exception) {}
    }

    private fun parseReportDocument(doc: com.google.firebase.firestore.DocumentSnapshot): ContentReport {
        val data = doc.data ?: return ContentReport(
            id = doc.id,
            reporterId = "unknown",
            contentType = ReportContentType.POST,
            contentId = doc.id,
            reason = ReportReason.OTHER
        )

        val typeStr = data["contentType"] as? String ?: ReportContentType.POST.name
        val contentType = try { ReportContentType.valueOf(typeStr) } catch (_: Exception) { ReportContentType.POST }

        val reasonStr = data["reason"] as? String ?: ReportReason.OTHER.name
        val reason = try { ReportReason.valueOf(reasonStr) } catch (_: Exception) { ReportReason.OTHER }

        val statusStr = data["status"] as? String ?: ReportStatus.PENDING.name
        val status = try { ReportStatus.valueOf(statusStr) } catch (_: Exception) { ReportStatus.PENDING }

        return ContentReport(
            id = doc.id,
            reportId = (data["reportId"] as? String) ?: doc.id,
            reporterId = (data["reporterId"] as? String).orEmpty(),
            reporterName = (data["reporterName"] as? String) ?: "Community Member",
            reporterHandle = (data["reporterHandle"] as? String).orEmpty(),
            reportedUserId = data["reportedUserId"] as? String,
            reportedUserName = data["reportedUserName"] as? String,
            contentType = contentType,
            contentId = (data["contentId"] as? String) ?: doc.id,
            contentSummary = (data["contentSummary"] as? String).orEmpty(),
            reason = reason,
            description = (data["description"] as? String).orEmpty(),
            status = status,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            reviewedAt = (data["reviewedAt"] as? Number)?.toLong(),
            reviewedBy = data["reviewedBy"] as? String,
            resolutionNotes = data["resolutionNotes"] as? String
        )
    }
}
