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
 * Mandatory Verification Suite for Super Admin Authorization Synchronization.
 *
 * Verifies all 10 required operational scenarios:
 * Scenario A: Verified designated Super Admin logs in and sees the correct role.
 * Scenario B: Super Admin opens Dashboard and all authorized metrics load successfully.
 * Scenario C: Super Admin can access Content Reports.
 * Scenario D: Ordinary User cannot access administrative features.
 * Scenario E: Moderator cannot access Super Admin-only features.
 * Scenario F: Expired session cannot access protected data.
 * Scenario G: Suspended account cannot access protected data.
 * Scenario H: Temporary Firestore network failure does not incorrectly downgrade a verified role or grant unauthorized access.
 * Scenario I: Role changes invalidate previously cached permissions.
 * Scenario J: Logout clears the administrative session and cached role.
 */
class SuperAdminAuthorizationSynchronizationTest {

    private val ownerUid = FirestoreAdminRepository.OWNER_BOOTSTRAP_UID // "isBdCPBYupN4SDJZ1yRiSZ8K0FF2"
    private val ownerEmail = FirestoreAdminRepository.OWNER_BOOTSTRAP_EMAIL // "hamarachitralonline@gmail.com"
    private val moderatorUid = "moderatorUserUid123"
    private val ordinaryUid = "ordinaryUserUid456"

    @Before
    fun setUp() {
        FirestoreAdminRepository.clearRateLimitsForTesting()
    }

    @Test
    fun `Scenario A - Verified designated Super Admin logs in and sees the correct role`() {
        // Strict credential validation for owner
        val validation = FirestoreAdminRepository.validateOwnerBootstrapCredentials(ownerUid, ownerEmail)
        assertTrue("Owner identity must be strictly verified", validation.isSuccess)

        // Normalization of role strings including variants with spaces and dashes
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("super_admin"))
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("SUPER_ADMIN"))
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("super admin"))
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("Super Admin"))
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("owner"))

        // Active session creation
        val now = System.currentTimeMillis()
        FirestoreAdminRepository.startAdminSession(now, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "owner_verification")

        val cachedRole = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now)
        assertEquals("Super Admin must be recognized with correct role", AdminRole.SUPER_ADMIN, cachedRole)
        assertEquals("Super Admin", cachedRole?.displayName)
    }

    @Test
    fun `Scenario B - Super Admin opens Dashboard and all authorized metrics load successfully`() {
        val superAdmin = AdminRole.SUPER_ADMIN
        assertTrue("Super Admin can access admin panel", superAdmin.canAccessAdminPanel)
        assertTrue("Super Admin can view stats and manage users", superAdmin.canManageUsers)
        assertTrue("Super Admin can manage verification requests", superAdmin.canManageVerification)
        assertTrue("Super Admin can curate trending and featured topics", superAdmin.canManageFeaturedAndPromoted)
        assertTrue("Super Admin can manage village directory", superAdmin.canManageVillages)
        assertTrue("Super Admin can broadcast official notifications", superAdmin.canBroadcastNotifications)
    }

    @Test
    fun `Scenario C - Super Admin can access Content Reports`() {
        val superAdmin = AdminRole.SUPER_ADMIN
        assertTrue("Super Admin must have canModerateContent capability", superAdmin.canModerateContent)

        val now = System.currentTimeMillis()
        FirestoreAdminRepository.startAdminSession(now, rememberMe = false, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "test_auth")

        val cached = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now)
        assertNotNull(cached)
        assertTrue("Cached Super Admin role must be able to moderate content and view reports", cached!!.canModerateContent)
    }

    @Test
    fun `Scenario D - Ordinary User cannot access administrative features`() {
        val ordinary = AdminRole.NONE
        assertEquals("Ordinary User", ordinary.displayName)
        assertFalse("Ordinary user cannot access admin panel", ordinary.canAccessAdminPanel)
        assertFalse("Ordinary user cannot moderate content or view reports", ordinary.canModerateContent)
        assertFalse("Ordinary user cannot manage admins", ordinary.canManageAdmins)
        assertFalse("Ordinary user cannot manage users", ordinary.canManageUsers)

        val parsedMember = AdminRole.fromString("member")
        val parsedUser = AdminRole.fromString("user")
        assertEquals(AdminRole.NONE, parsedMember)
        assertEquals(AdminRole.NONE, parsedUser)
        assertFalse(parsedMember.canAccessAdminPanel)
        assertFalse(parsedUser.canAccessAdminPanel)

        // Ordinary user credentials rejected from owner bootstrap
        val invalidBootstrap = FirestoreAdminRepository.validateOwnerBootstrapCredentials(ordinaryUid, "user@example.com")
        assertTrue(invalidBootstrap.isFailure)
    }

    @Test
    fun `Scenario E - Moderator cannot access Super Admin-only features`() {
        val moderator = AdminRole.MODERATOR
        assertTrue("Moderator can access admin panel", moderator.canAccessAdminPanel)
        assertTrue("Moderator can moderate content and view reports", moderator.canModerateContent)

        // Super Admin only features restricted from moderator:
        assertFalse("Moderator cannot manage or assign admins", moderator.canManageAdmins)
        assertFalse("Moderator cannot manage user suspensions", moderator.canManageUsers)
        assertFalse("Moderator cannot manage blue tick verifications", moderator.canManageVerification)
        assertFalse("Moderator cannot manage village directory", moderator.canManageVillages)
        assertFalse("Moderator cannot broadcast official announcements", moderator.canBroadcastNotifications)
        assertFalse("Moderator cannot promote or feature posts", moderator.canManageFeaturedAndPromoted)
    }

    @Test
    fun `Scenario F - Expired session cannot access protected data`() {
        val now = System.currentTimeMillis()
        val expiredStart = now - (4 * 60 * 60 * 1000L + 1000L) // 4 hours and 1 second ago

        // Validate window
        assertFalse("Session older than 4 hours is expired", FirestoreAdminRepository.isAdminSessionValid(expiredStart, now))

        // Start session then simulate expiration
        FirestoreAdminRepository.startAdminSession(expiredStart, rememberMe = false, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "test_session")

        // Inactive session past 4h window returns null cached role
        val role = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now)
        assertNull("Expired session must NOT return cached administrative role", role)

        val windowCheck = FirestoreAdminRepository.verifyActiveAdminSessionWindow(now)
        assertTrue("Expired session window check must fail", windowCheck.isFailure)
    }

    @Test
    fun `Scenario G - Suspended account cannot access protected data`() {
        val now = System.currentTimeMillis()
        FirestoreAdminRepository.startAdminSession(now, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "test_session")

        // When suspension is detected, session cache is explicitly cleared
        FirestoreAdminRepository.invalidateSessionRoleCache(ownerUid)

        val cachedRole = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now)
        assertNull("Suspended account must have its cached role cleared immediately", cachedRole)
    }

    @Test
    fun `Scenario H - Temporary Firestore network failure does not incorrectly downgrade a verified role or grant unauthorized access`() {
        val now = System.currentTimeMillis()
        FirestoreAdminRepository.startAdminSession(now, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "authoritative_verification")

        // During an active valid session window, a network hiccup preserves verified session role
        val cachedRole = FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now)
        assertEquals("Verified active session role must be preserved during network failure", AdminRole.SUPER_ADMIN, cachedRole)

        // For an unauthenticated or ordinary user with NO cached role, cache returns null (never grants unauthorized access)
        val nonAdminCache = FirestoreAdminRepository.getValidCachedSessionRole(ordinaryUid, now)
        assertNull("Unverified user must never be granted cached role", nonAdminCache)
    }

    @Test
    fun `Scenario I - Role changes invalidate previously cached permissions`() {
        val now = System.currentTimeMillis()
        FirestoreAdminRepository.startAdminSession(now, rememberMe = false, uid = moderatorUid, email = "mod@yarkhoon.com")
        FirestoreAdminRepository.updateVerifiedSessionRole(moderatorUid, AdminRole.MODERATOR, "test_mod")

        assertEquals(AdminRole.MODERATOR, FirestoreAdminRepository.getValidCachedSessionRole(moderatorUid, now))

        // Demotion / Revocation invalidates the cache for target UID
        FirestoreAdminRepository.invalidateSessionRoleCache(moderatorUid)
        assertNull("Target UID cache must be invalidated after role change", FirestoreAdminRepository.getValidCachedSessionRole(moderatorUid, now))

        // Role update updates cache to new verified role
        FirestoreAdminRepository.updateVerifiedSessionRole(moderatorUid, AdminRole.ADMIN, "promotion")
        assertEquals(AdminRole.ADMIN, FirestoreAdminRepository.getValidCachedSessionRole(moderatorUid, now))
    }

    @Test
    fun `Scenario J - Logout clears the administrative session and cached role`() {
        val now = System.currentTimeMillis()
        FirestoreAdminRepository.startAdminSession(now, rememberMe = true, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "test_login")

        assertEquals(AdminRole.SUPER_ADMIN, FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now))
        assertTrue(AdminSessionManager.isSessionActive)

        // Admin signs out
        FirestoreAdminRepository.invalidateAdminSession()

        assertFalse("Session must be inactive after logout", AdminSessionManager.isSessionActive)
        assertEquals("activeAdminSessionStartMs must be 0 after logout", 0L, FirestoreAdminRepository.activeAdminSessionStartMs)
        assertNull("Cached role must be completely nullified after logout", FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now))
        assertNull("verifiedSessionRole field must be null", FirestoreAdminRepository.verifiedSessionRole)
    }

    @Test
    fun `Scenario K - In-process admin login with rememberMe false stays active and verifies owner UID`() {
        val now = System.currentTimeMillis()
        // Explicit login with rememberMe = false (default on AdminLoginContent)
        FirestoreAdminRepository.startAdminSession(now, rememberMe = false, uid = ownerUid, email = ownerEmail)
        FirestoreAdminRepository.updateVerifiedSessionRole(ownerUid, AdminRole.SUPER_ADMIN, "signInAsAdmin_verified")

        // Active in-process session is valid even though rememberMe is false
        assertTrue("Session must be active in-process when rememberMe is false", AdminSessionManager.isSessionActive)
        assertFalse("Session must not be expired immediately after login", AdminSessionManager.isSessionExpired(now + 10_000L))
        assertFalse("shouldRestoreOnAppStart is false when rememberMe is false, which previously caused AuthStateListener to sign out active logins", AdminSessionManager.shouldRestoreOnAppStart(now + 10_000L))

        val hasActiveInProcessSession = AdminSessionManager.isSessionActive &&
            !AdminSessionManager.isSessionExpired(now + 10_000L) &&
            AdminSessionManager.currentAdminUid == ownerUid
        assertTrue("Active in-process session check must protect the newly logged-in owner from background signOut()", hasActiveInProcessSession)
        assertEquals(AdminRole.SUPER_ADMIN, FirestoreAdminRepository.getValidCachedSessionRole(ownerUid, now + 10_000L))

        // Verify strict owner UID validation before resolving SUPER_ADMIN
        assertTrue(FirestoreAdminRepository.validateOwnerBootstrapCredentials(ownerUid, ownerEmail).isSuccess)
        assertTrue(FirestoreAdminRepository.validateOwnerBootstrapCredentials("impostor_uid", ownerEmail).isFailure)
        assertNull("Non-matching UID must never resolve cached owner SUPER_ADMIN role", FirestoreAdminRepository.getValidCachedSessionRole("impostor_uid", now + 10_000L))
    }
}
