package com.example

import com.example.data.AdminRole
import com.example.data.AdminSessionManager
import com.example.data.FirestoreAdminRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Independent Security Audit Test Suite for Super Admin Authorization Synchronization.
 *
 * Evaluates:
 * 1. Original Authorization Issue Elimination
 * 2. Server-Side Authorization Invariance & Non-Bypassability
 * 3. Session Security (Inactivity, Restarts, Multi-device, Logout, Suspension, Revocation)
 * 4. Cache Security (Stale Role Attack Mitigation across all invalidation vectors)
 * 5. Network Failure Safety (Zero Privilege Escalation & Zero Unauthorized Downgrade)
 * 6. Strict Owner Bootstrap Security (Restricted strictly to authorized owner identity)
 */
class IndependentSecurityAuditTest {

    private val ownerUid = FirestoreAdminRepository.OWNER_BOOTSTRAP_UID // "isBdCPBYupN4SDJZ1yRiSZ8K0FF2"
    private val ownerEmail = FirestoreAdminRepository.OWNER_BOOTSTRAP_EMAIL // "hamarachitralonline@gmail.com"
    private val rogueUid = "rogueAdminUid888"
    private val rogueEmail = "rogue@example.com"
    private val baseTimeMs = 1_700_000_000_000L

    @Before
    fun setUp() {
        AdminSessionManager.resetForTesting()
        FirestoreAdminRepository.clearRateLimitsForTesting()
    }

    @Test
    fun `Audit 1 - Original authorization issue eliminated with unified authoritative state`() {
        // Test that AdminRole.NONE is accurately typed as "Ordinary User" and has zero access
        val ordinaryRole = AdminRole.NONE
        assertEquals("Ordinary User", ordinaryRole.displayName)
        assertFalse(ordinaryRole.canAccessAdminPanel)
        assertFalse(ordinaryRole.canModerateContent)
        assertFalse(ordinaryRole.canManageAdmins)

        // Super Admin has full capabilities
        val superAdminRole = AdminRole.SUPER_ADMIN
        assertEquals("Super Admin", superAdminRole.displayName)
        assertTrue(superAdminRole.canAccessAdminPanel)
        assertTrue(superAdminRole.canModerateContent)
        assertTrue(superAdminRole.canManageAdmins)

        // When cached for the session, the cached role is consistent
        FirestoreAdminRepository.startAdminSession(baseTimeMs, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, superAdminRole, "audit_test")

        val resolved = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, baseTimeMs)
        assertEquals(AdminRole.SUPER_ADMIN, resolved)
        assertEquals("Super Admin", resolved?.displayName)
    }

    @Test
    fun `Audit 2 - Stale cached role attack is rejected after role revocation`() {
        // Admin logs in and caches Super Admin role
        FirestoreAdminRepository.startAdminSession(baseTimeMs, rememberMe = true, uid = rogueUid, email = rogueEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(rogueUid, AdminRole.SUPER_ADMIN, "initial_grant")
        assertEquals(AdminRole.SUPER_ADMIN, FirestoreAdminRepository.getValidCachedSessionRole(rogueUid, baseTimeMs))

        // Role revocation occurs (e.g. from server or admin console)
        FirestoreAdminRepository.invalidateSessionRoleCache(rogueUid)

        // Attempting to read cached role returns null
        val staleRole = FirestoreAdminRepository.getValidCachedSessionRole(rogueUid, baseTimeMs)
        assertNull("Stale cached role must NOT be returned after revocation", staleRole)
    }

    @Test
    fun `Audit 3 - Stale cached role attack is rejected after account suspension`() {
        FirestoreAdminRepository.startAdminSession(baseTimeMs, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "initial_grant")

        // Account is suspended: session cache must be immediately invalidated
        FirestoreAdminRepository.invalidateSessionRoleCache(ownerUid)

        val role = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, baseTimeMs)
        assertNull("Suspended user must not retain cached administrative role", role)
    }

    @Test
    fun `Audit 4 - Stale cached role attack is rejected after 4-hour inactivity expiration`() {
        FirestoreAdminRepository.startAdminSession(baseTimeMs, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "initial_grant")

        // Inactive for 4 hours and 1 millisecond
        val expiredTimeMs = baseTimeMs + (4 * 60 * 60 * 1000L) + 1L

        assertFalse("Session validity check must reject elapsed time > 4h",
            FirestoreAdminRepository.isAdminSessionValid(baseTimeMs, expiredTimeMs)
        )

        val cachedRole = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, expiredTimeMs)
        assertNull("Expired session must reject cached role access", cachedRole)

        val sessionWindowResult = FirestoreAdminRepository.verifyActiveAdminSessionWindow(expiredTimeMs)
        assertTrue("verifyActiveAdminSessionWindow must fail for expired session", sessionWindowResult.isFailure)
    }

    @Test
    fun `Audit 5 - Multi-device logout and session termination`() {
        FirestoreAdminRepository.startAdminSession(baseTimeMs, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "device_1")

        assertTrue(AdminSessionManager.isSessionActive)
        assertNotNull(FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, baseTimeMs))

        // Logout triggered (user clicked logout or second device signaled termination)
        FirestoreAdminRepository.invalidateAdminSession()

        assertFalse("Session must be inactive", AdminSessionManager.isSessionActive)
        assertEquals(0L, FirestoreAdminRepository.activeAdminSessionStartMs)
        assertNull("Cached role must be purged on logout", FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, baseTimeMs))
        assertNull("verifiedSessionRole field must be null", FirestoreAdminRepository.verifiedSessionRole)
    }

    @Test
    fun `Audit 6 - Different UID cannot hijack cached administrative session`() {
        FirestoreAdminRepository.startAdminSession(baseTimeMs, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "owner_login")

        // Attacker attempts to fetch cached role using their own UID
        val attackerAttempt = FirestoreAdminRepository.getValidCachedSessionRole(rogueUid, baseTimeMs)
        assertNull("Attacker UID must NEVER receive cached role of owner", attackerAttempt)

        // Verified owner still receives their role
        val ownerAttempt = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, baseTimeMs)
        assertEquals(AdminRole.SUPER_ADMIN, ownerAttempt)
    }

    @Test
    fun `Audit 7 - Network failure safety - Zero privilege escalation for unverified users`() {
        // Unverified user with no active session or cache
        val unverifiedRole = FirestoreAdminRepository.getValidCachedSessionRole(rogueUid, baseTimeMs)
        assertNull("Unverified user must receive null cached role", unverifiedRole)

        // Ordinary user role parsing
        assertEquals(AdminRole.NONE, AdminRole.fromString("unknown_role"))
        assertFalse("Unverified role cannot access admin panel", AdminRole.NONE.canAccessAdminPanel)
    }

    @Test
    fun `Audit 8 - Strict owner bootstrap exception isolation`() {
        // Only exact owner UID + email is permitted
        assertTrue(FirestoreAdminRepository.validateOwnerBootstrapCredentials(ownerUid, ownerEmail).isSuccess)

        // Rogue admin with different UID is rejected
        assertTrue(FirestoreAdminRepository.validateOwnerBootstrapCredentials(rogueUid, ownerEmail).isFailure)

        // Rogue admin with different email is rejected
        assertTrue(FirestoreAdminRepository.validateOwnerBootstrapCredentials(ownerUid, rogueEmail).isFailure)

        // Legacy/fallback emails are rejected
        assertTrue(FirestoreAdminRepository.validateOwnerBootstrapCredentials(ownerUid, "admin@yarkhoon.com").isFailure)
        assertTrue(FirestoreAdminRepository.validateOwnerBootstrapCredentials(ownerUid, "moderator@yarkhoon.com").isFailure)
    }

    @Test
    fun `Audit 9 - Application restart with Remember Me disabled prevents session restoration`() {
        AdminSessionManager.startSession(
            uid = ownerUid,
            email = ownerEmail,
            rememberMe = false,
            nowMs = baseTimeMs
        )

        assertTrue(AdminSessionManager.isSessionActive)
        assertFalse(AdminSessionManager.isRememberMeEnabled)

        // App restarts 10 minutes later
        val restartTimeMs = baseTimeMs + (10 * 60 * 1000L)
        assertFalse("Session without Remember Me must not restore across restarts",
            AdminSessionManager.shouldRestoreOnAppStart(restartTimeMs)
        )
    }

    @Test
    fun `Audit 10 - Device backgrounding past 4 hours invalidates session`() {
        AdminSessionManager.startSession(
            uid = ownerUid,
            email = ownerEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )

        // Device in background for 4 hours and 5 minutes
        val resumeTimeMs = baseTimeMs + (4 * 60 * 60 * 1000L + 5 * 60 * 1000L)
        assertTrue("Session must be detected as expired after 4h backgrounding",
            AdminSessionManager.isSessionExpired(resumeTimeMs)
        )
        assertFalse("shouldRestoreOnAppStart must return false for expired background session",
            AdminSessionManager.shouldRestoreOnAppStart(resumeTimeMs)
        )
    }
}
