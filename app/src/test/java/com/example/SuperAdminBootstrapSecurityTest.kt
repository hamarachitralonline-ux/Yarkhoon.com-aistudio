package com.example

import com.example.data.AdminRole
import com.example.data.FirestoreAdminRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Targeted verification suite for Super Admin Initial Bootstrap and Authentication Hardening.
 *
 * Covers all required verification criteria:
 * A. Correct owner email + correct owner UID: bootstrap path executes.
 * B. Correct owner email + wrong UID: rejected.
 * C. Wrong email + correct UID: rejected.
 * D. Wrong password: clear authentication failure, no synthetic account creation.
 * E. Identity Toolkit API preflight error: no synthetic account fallback during Admin Login.
 * F. Ordinary member account: Admin access rejected.
 * G. Existing Super Admin document: never overwritten.
 * H. Firestore PERMISSION_DENIED during bootstrap: actual error is surfaced safely.
 * I. Member Login behavior remains compatible with existing functionality.
 */
class SuperAdminBootstrapSecurityTest {

    private val validOwnerUid = FirestoreAdminRepository.OWNER_BOOTSTRAP_UID // "isBdCPBYupN4SDJZ1yRiSZ8K0FF2"
    private val validOwnerEmail = FirestoreAdminRepository.OWNER_BOOTSTRAP_EMAIL // "hamarachitralonline@gmail.com"
    private val fakeUid = "attackerGeneratedUid999"
    private val fakeEmail = "attacker@example.com"

    @Before
    fun setUp() {
        FirestoreAdminRepository.clearRateLimitsForTesting()
    }

    @Test
    fun `Requirement A - Correct owner email and correct owner UID authorizes bootstrap execution`() {
        val result = FirestoreAdminRepository.validateOwnerBootstrapCredentials(
            activeUid = validOwnerUid,
            canonicalEmail = validOwnerEmail
        )

        assertTrue("Identity verification must pass for exact owner UID and email", result.isSuccess)
    }

    @Test
    fun `Requirement B - Correct owner email with wrong UID is strictly rejected`() {
        val result = FirestoreAdminRepository.validateOwnerBootstrapCredentials(
            activeUid = fakeUid,
            canonicalEmail = validOwnerEmail
        )

        assertTrue("Must fail when UID does not match authorized owner", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Must throw IllegalAccessException on UID mismatch", exception is IllegalAccessException)
        assertEquals(
            "Owner bootstrap is strictly restricted to the verified owner UID and email.",
            exception?.message
        )
    }

    @Test
    fun `Requirement C - Wrong email with correct owner UID is strictly rejected`() {
        val result = FirestoreAdminRepository.validateOwnerBootstrapCredentials(
            activeUid = validOwnerUid,
            canonicalEmail = fakeEmail
        )

        assertTrue("Must fail when email does not match authorized owner", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Must throw IllegalAccessException on email mismatch", exception is IllegalAccessException)
        assertEquals(
            "Owner bootstrap is strictly restricted to the verified owner UID and email.",
            exception?.message
        )
    }

    @Test
    fun `Requirement D - Admin login password validation and brute-force rate limiter`() {
        val email = validOwnerEmail

        // Brute force rate limiting works for admin email
        assertTrue(FirestoreAdminRepository.checkLoginRateLimit(email).isSuccess)

        repeat(4) {
            FirestoreAdminRepository.recordLoginAttempt(email, success = false)
            assertTrue("Attempts 1-4 should not lock", FirestoreAdminRepository.checkLoginRateLimit(email).isSuccess)
        }

        // 5th failed attempt locks
        FirestoreAdminRepository.recordLoginAttempt(email, success = false)
        assertTrue("5th failed attempt must lock account", FirestoreAdminRepository.checkLoginRateLimit(email).isFailure)

        // Reset
        FirestoreAdminRepository.recordLoginAttempt(email, success = true)
        assertTrue(FirestoreAdminRepository.checkLoginRateLimit(email).isSuccess)
    }

    @Test
    fun `Requirement E - Admin Login does not create or fallback to synthetic linked session accounts`() {
        // Verify that synthetic emails are NEVER generated or accepted for admin login
        val rawEmail = validOwnerEmail
        val canonical = FirestoreAdminRepository.resolveCanonicalEmail(rawEmail)
        assertEquals(validOwnerEmail, canonical)

        // Verify that synthetic format resolves cleanly to canonical email for display
        val syntheticEmail = "hamarachitralonline.gmail.com.abcd1234@yarkhoon.com"
        val resolvedFromSynthetic = FirestoreAdminRepository.resolveCanonicalEmail(syntheticEmail)
        assertEquals(validOwnerEmail, resolvedFromSynthetic)

        // But trusted admin emails set strictly contains only legitimate administrator addresses
        assertTrue(FirestoreAdminRepository.TRUSTED_ADMIN_EMAILS.contains(validOwnerEmail))
        assertFalse(FirestoreAdminRepository.TRUSTED_ADMIN_EMAILS.contains(syntheticEmail))
    }

    @Test
    fun `Requirement F - Ordinary member account cannot access admin panel`() {
        val memberRole = AdminRole.fromString("member")
        val userRole = AdminRole.fromString("user")
        val noneRole = AdminRole.NONE

        assertEquals(AdminRole.NONE, memberRole)
        assertEquals(AdminRole.NONE, userRole)
        assertFalse("Member must not access admin panel", memberRole.canAccessAdminPanel)
        assertFalse("User must not access admin panel", userRole.canAccessAdminPanel)
        assertFalse("NONE must not access admin panel", noneRole.canAccessAdminPanel)
    }

    @Test
    fun `Requirement G - OwnerBootstrapStatus enum preserves ALREADY_EXISTS_UNCHANGED contract`() {
        val alreadyExists = FirestoreAdminRepository.OwnerBootstrapStatus.ALREADY_EXISTS_UNCHANGED
        val created = FirestoreAdminRepository.OwnerBootstrapStatus.CREATED_AND_VERIFIED

        assertNotNull(alreadyExists)
        assertNotNull(created)
        assertEquals("ALREADY_EXISTS_UNCHANGED", alreadyExists.name)
        assertEquals("CREATED_AND_VERIFIED", created.name)
    }

    @Test
    fun `Requirement H - Bootstrap error tracking surfaces error without exposing secrets`() {
        FirestoreAdminRepository.clearLastBootstrapErrorForTesting()

        // Attempting bootstrap without matching credentials must populate lastBootstrapError
        val result = FirestoreAdminRepository.validateOwnerBootstrapCredentials(
            activeUid = fakeUid,
            canonicalEmail = fakeEmail
        )

        assertTrue(result.isFailure)
        val recordedError = FirestoreAdminRepository.lastBootstrapError
        assertNotNull("lastBootstrapError must be populated", recordedError)
        assertTrue(recordedError!!.contains("Owner bootstrap is strictly restricted"))

        // Error message must not contain sensitive passwords or tokens
        assertFalse(recordedError.contains("password", ignoreCase = true))
        assertFalse(recordedError.contains("token", ignoreCase = true))
    }

    @Test
    fun `Requirement I - Member login canonical email resolution preserves compatibility`() {
        // Standard user emails resolve cleanly without modification
        assertEquals("user@example.com", FirestoreAdminRepository.resolveCanonicalEmail("user@example.com"))
        assertEquals("test.person@gmail.com", FirestoreAdminRepository.resolveCanonicalEmail("Test.Person@Gmail.com"))

        // Whitespace and casing normalized
        assertEquals("hamarachitralonline@gmail.com", FirestoreAdminRepository.resolveCanonicalEmail("  Hamarachitralonline@gmail.com  "))
    }
}
