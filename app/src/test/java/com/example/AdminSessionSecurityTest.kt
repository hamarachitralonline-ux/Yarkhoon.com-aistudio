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
 * Comprehensive verification test suite for Yarkhoon Admin Security:
 * - Remember Me Toggle (persistent session vs. session-only)
 * - 4-Hour Inactivity Timeout (timer reset on authenticated activity & automatic expiration)
 * - Background expiration and safe app restart / device restart handling
 * - Zero plain-text password storage guarantee
 */
class AdminSessionSecurityTest {

    private val adminUid = "isBdCPBYupN4SDJZ1yRiSZ8K0FF2"
    private val adminEmail = "hamarachitralonline@gmail.com"
    private val baseTimeMs = 1_700_000_000_000L
    private val fourHoursMs = AdminSessionManager.INACTIVITY_TIMEOUT_MS // 14,400,000 ms

    @Before
    fun setUp() {
        AdminSessionManager.resetForTesting()
        FirestoreAdminRepository.clearRateLimitsForTesting()
    }

    @Test
    fun `Scenario 1 - Remember Me enabled preserves session within 4 hours of inactivity`() {
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )

        assertTrue("Session must be active", AdminSessionManager.isSessionActive)
        assertTrue("Remember Me must be true", AdminSessionManager.isRememberMeEnabled)
        assertEquals(adminUid, AdminSessionManager.currentAdminUid)
        assertEquals(adminEmail, AdminSessionManager.currentAdminEmail)

        // Simulate app restart 2 hours later (< 4 hours)
        val restartTimeMs = baseTimeMs + (2 * 60 * 60 * 1000L)
        assertFalse("Session should NOT be expired after 2 hours", AdminSessionManager.isSessionExpired(restartTimeMs))
        assertTrue("Session should be restored on app start when Remember Me is enabled and valid",
            AdminSessionManager.shouldRestoreOnAppStart(restartTimeMs)
        )
    }

    @Test
    fun `Scenario 2 - Remember Me disabled does NOT allow automatic session restoration on app restart`() {
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = false,
            nowMs = baseTimeMs
        )

        assertTrue(AdminSessionManager.isSessionActive)
        assertFalse(AdminSessionManager.isRememberMeEnabled)

        // Even though time is within 1 hour, Remember Me is false, so it must NOT restore on restart
        val restartTimeMs = baseTimeMs + (1 * 60 * 60 * 1000L)
        assertFalse(
            "Session must NOT restore on app start if Remember Me was disabled",
            AdminSessionManager.shouldRestoreOnAppStart(restartTimeMs)
        )
    }

    @Test
    fun `Scenario 3 - App restart with valid session restored successfully and remaining time accurate`() {
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )

        val checkTimeMs = baseTimeMs + (3 * 60 * 60 * 1000L) // 3 hours elapsed
        val remainingMs = AdminSessionManager.getRemainingInactivityTimeMs(checkTimeMs)
        assertEquals("Remaining time should be exactly 1 hour", 1 * 60 * 60 * 1000L, remainingMs)
        assertTrue(AdminSessionManager.shouldRestoreOnAppStart(checkTimeMs))
    }

    @Test
    fun `Scenario 4 - Four continuous hours of inactivity invalidates the session`() {
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )

        // Exactly 4 hours + 1 millisecond elapsed with zero activity
        val expiredTimeMs = baseTimeMs + fourHoursMs + 1L
        assertTrue("Session must be marked expired after 4 hours of inactivity",
            AdminSessionManager.isSessionExpired(expiredTimeMs)
        )
        assertFalse("Session cannot be restored once expired",
            AdminSessionManager.shouldRestoreOnAppStart(expiredTimeMs)
        )
        assertEquals("Remaining inactivity time should be 0",
            0L, AdminSessionManager.getRemainingInactivityTimeMs(expiredTimeMs)
        )
    }

    @Test
    fun `Scenario 5 - User activity before 4-hour threshold resets the inactivity timer`() {
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )

        // After 3 hours and 30 minutes, admin performs an authenticated operation (e.g. reviewing reports)
        val activity1Ms = baseTimeMs + (3 * 60 * 60 * 1000L) + (30 * 60 * 1000L)
        assertFalse(AdminSessionManager.isSessionExpired(activity1Ms))
        AdminSessionManager.recordActivity(activity1Ms)

        assertEquals("lastActivityTimeMs must be updated to activity timestamp",
            activity1Ms, AdminSessionManager.lastActivityTimeMs
        )

        // Now test 2 hours after the activity (5.5 hours from start, but only 2 hours from last activity)
        val checkTimeMs = activity1Ms + (2 * 60 * 60 * 1000L)
        assertFalse(
            "Session must still be valid because activity reset the inactivity timer",
            AdminSessionManager.isSessionExpired(checkTimeMs)
        )
        val remainingMs = AdminSessionManager.getRemainingInactivityTimeMs(checkTimeMs)
        assertEquals(2 * 60 * 60 * 1000L, remainingMs)
    }

    @Test
    fun `Scenario 6 - App remaining in background for four hours expires session`() {
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )

        // App backgrounded for 4 hours and 15 minutes
        val resumeTimeMs = baseTimeMs + fourHoursMs + (15 * 60 * 1000L)
        assertTrue("App resuming after 4 hours in background must detect expiration",
            AdminSessionManager.isSessionExpired(resumeTimeMs)
        )

        // Invalidate and clear upon detecting expiration
        AdminSessionManager.invalidateAndClear()
        assertFalse(AdminSessionManager.isSessionActive)
        assertNull(AdminSessionManager.currentAdminUid)
        assertNull(AdminSessionManager.currentAdminEmail)
    }

    @Test
    fun `Scenario 7 - Expired session fails repository API check and prevents credential reuse`() {
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )
        FirestoreAdminRepository.startAdminSession(baseTimeMs, rememberMe = true, uid = adminUid, email = adminEmail)

        // Verify within active window
        val validTimeMs = baseTimeMs + (1 * 60 * 60 * 1000L)
        assertTrue(FirestoreAdminRepository.verifyActiveAdminSessionWindow(validTimeMs).isSuccess)

        // Jump to 4 hours + 10 seconds without activity
        val expiredTimeMs = baseTimeMs + fourHoursMs + 10_000L
        val expiredWindowCheck = FirestoreAdminRepository.verifyActiveAdminSessionWindow(expiredTimeMs)
        assertTrue("API check must fail on expired session", expiredWindowCheck.isFailure)

        // Ensure session state was immediately cleared to prevent token reuse
        assertFalse("Session must be cleared", AdminSessionManager.isSessionActive)
        assertEquals(0L, FirestoreAdminRepository.activeAdminSessionStartMs)
    }

    @Test
    fun `Scenario 8 - Device restart after session expiration rejects automatic login`() {
        // Session started before device shutdown
        AdminSessionManager.startSession(
            uid = adminUid,
            email = adminEmail,
            rememberMe = true,
            nowMs = baseTimeMs
        )

        // Device rebooted after 5 hours
        val rebootTimeMs = baseTimeMs + (5 * 60 * 60 * 1000L)
        assertFalse(
            "Device reboot after expiration must reject automatic restoration",
            AdminSessionManager.shouldRestoreOnAppStart(rebootTimeMs)
        )
    }

    @Test
    fun `Scenario 9 - Zero plain-text password storage verification`() {
        // Assert that AdminSessionManager does not declare, accept, or store any password field
        val fields = AdminSessionManager::class.java.declaredFields.map { it.name }
        assertFalse("Must never contain password fields", fields.any { it.contains("password", ignoreCase = true) })
        assertFalse("Must never contain pass fields", fields.any { it.equals("pass", ignoreCase = true) })
    }

    @Test
    fun `Scenario 10 - AES-256-GCM encryption and decryption round-trip verification`() {
        val testPayload = """{"uid":"$adminUid","email":"$adminEmail","active":true}"""
        val encrypted = AdminSessionManager.encryptPayload(testPayload)

        assertNotNull("Ciphertext payload must not be null", encrypted)
        assertTrue("Ciphertext format must be iv:ciphertext", encrypted.contains(":"))
        assertFalse("Plaintext must not be visible in encrypted payload", encrypted.contains(adminUid))

        val decrypted = AdminSessionManager.decryptPayload(encrypted)
        assertEquals("Decrypted plaintext must match original payload exactly", testPayload, decrypted)
    }

    @Test
    fun `Scenario 11 - AES-256-GCM authenticated decryption rejects tampered payload`() {
        val testPayload = """{"uid":"$adminUid","role":"super_admin"}"""
        val encrypted = AdminSessionManager.encryptPayload(testPayload)
        val parts = encrypted.split(":")
        val iv = parts[0]
        val ciphertext = parts[1]

        // Tamper with ciphertext by altering last byte
        val tamperedCiphertext = if (ciphertext.endsWith("A")) ciphertext.dropLast(1) + "B" else ciphertext.dropLast(1) + "A"
        val tamperedPayload = "$iv:$tamperedCiphertext"

        val decrypted = AdminSessionManager.decryptPayload(tamperedPayload)
        assertNull("AES-GCM authentication tag mismatch must cause decryption to fail and return null", decrypted)
    }
}
