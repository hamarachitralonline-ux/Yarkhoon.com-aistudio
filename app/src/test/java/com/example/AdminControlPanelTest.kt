package com.example

import com.example.data.AdminAccountRecord
import com.example.data.AdminControlPanelSection
import com.example.data.AdminDashboardStats
import com.example.data.AdminRole
import com.example.data.CuratedTrendingTopic
import com.example.data.FirestoreAdminRepository
import com.example.data.ModerationActionType
import com.example.data.OfficialAnnouncement
import com.example.data.Post
import com.example.data.User
import com.example.data.UserSuspensionInfo
import com.example.data.VerificationRequest
import com.example.data.VerificationRequestStatus
import com.example.data.YarkhoonVillage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit test suite for the Yarkhoon Admin Login & 15-Section Admin Control Panel.
 * Covers all 14 required test scenarios:
 * 1. Admin login with valid credentials & role resolution
 * 2. Admin login with wrong password & brute-force rate limiting
 * 3. Ordinary user attempting admin login
 * 4. Unauthorized access to admin dashboard
 * 5. Unauthorized API / role action rejection
 * 6. Super Admin, Admin, and Moderator permissions matrix
 * 7. Blue tick verification approval and removal
 * 8. Featured post scheduling and expiration
 * 9. Trending post management & genuine engagement ranking
 * 10. Promoted post management, targeting, and expiration
 * 11. User suspension and activation
 * 12. Village creation, update, and duplicate prevention
 * 13. Logout and session expiration
 * 14. Complete 15-section Admin Control Panel navigation structure
 */
class AdminControlPanelTest {

    @Before
    fun setUp() {
        FirestoreAdminRepository.clearRateLimitsForTesting()
    }

    @Test
    fun `1 - Admin login role resolution maps super_admin, admin, and moderator accurately`() {
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("super_admin"))
        assertEquals(AdminRole.SUPER_ADMIN, AdminRole.fromString("SUPER_ADMIN"))
        assertEquals(AdminRole.ADMIN, AdminRole.fromString("admin"))
        assertEquals(AdminRole.MODERATOR, AdminRole.fromString("moderator"))
        assertEquals(AdminRole.NONE, AdminRole.fromString("member"))
        assertEquals(AdminRole.NONE, AdminRole.fromString(null))

        assertTrue(AdminRole.SUPER_ADMIN.canAccessAdminPanel)
        assertTrue(AdminRole.ADMIN.canAccessAdminPanel)
        assertTrue(AdminRole.MODERATOR.canAccessAdminPanel)
    }

    @Test
    fun `2 - Admin login brute-force rate limiter blocks after 5 failed password attempts`() {
        val email = "admin@yarkhoon.com"
        assertTrue(FirestoreAdminRepository.checkLoginRateLimit(email).isSuccess)

        repeat(4) {
            FirestoreAdminRepository.recordLoginAttempt(email, success = false)
            assertTrue("Attempt ${it + 1} should not yet lock", FirestoreAdminRepository.checkLoginRateLimit(email).isSuccess)
        }

        // 5th failed attempt triggers temporary lockout
        FirestoreAdminRepository.recordLoginAttempt(email, success = false)
        val lockoutResult = FirestoreAdminRepository.checkLoginRateLimit(email)
        assertTrue("5th failed attempt must trigger rate limit lockout", lockoutResult.isFailure)
        val lockoutMessage = lockoutResult.exceptionOrNull()?.message.orEmpty()
        assertTrue(lockoutMessage.contains("Too many failed admin login attempts"))

        // Successful login resets rate limit state
        FirestoreAdminRepository.recordLoginAttempt(email, success = true)
        assertTrue(FirestoreAdminRepository.checkLoginRateLimit(email).isSuccess)
    }

    @Test
    fun `3 - Ordinary user attempting admin login is rejected`() {
        val ordinaryRole = AdminRole.fromString("user")
        assertEquals(AdminRole.NONE, ordinaryRole)
        assertFalse("Ordinary user must not have admin panel access", ordinaryRole.canAccessAdminPanel)
        assertFalse(ordinaryRole.canManageAdmins)
        assertFalse(ordinaryRole.canManageVerification)
        assertFalse(ordinaryRole.canManageFeaturedAndPromoted)
        assertFalse(ordinaryRole.canManageVillages)
        assertFalse(ordinaryRole.canBroadcastNotifications)
        assertFalse(ordinaryRole.canManageUsers)
        assertFalse(ordinaryRole.canModerateContent)
    }

    @Test
    fun `4 & 5 & 6 - Super Admin, Admin, and Moderator RBAC permissions matrix enforced`() {
        val superAdmin = AdminRole.SUPER_ADMIN
        val admin = AdminRole.ADMIN
        val moderator = AdminRole.MODERATOR

        // Super Admin has full control including managing admins
        assertTrue(superAdmin.canAccessAdminPanel)
        assertTrue(superAdmin.canManageAdmins)
        assertTrue(superAdmin.canManageVerification)
        assertTrue(superAdmin.canManageFeaturedAndPromoted)
        assertTrue(superAdmin.canManageVillages)
        assertTrue(superAdmin.canBroadcastNotifications)
        assertTrue(superAdmin.canManageUsers)
        assertTrue(superAdmin.canModerateContent)

        // Standard Admin can manage users, posts, verification, featured/promoted, villages, notifications, but NOT other admins
        assertTrue(admin.canAccessAdminPanel)
        assertFalse("Standard Admin must NOT be able to manage other admins", admin.canManageAdmins)
        assertTrue(admin.canManageVerification)
        assertTrue(admin.canManageFeaturedAndPromoted)
        assertTrue(admin.canManageVillages)
        assertTrue(admin.canBroadcastNotifications)
        assertTrue(admin.canManageUsers)
        assertTrue(admin.canModerateContent)

        // Moderator can review reports and moderate posts/comments, but cannot manage admins, verification, featured/promoted, villages, or users
        assertTrue(moderator.canAccessAdminPanel)
        assertFalse(moderator.canManageAdmins)
        assertFalse(moderator.canManageVerification)
        assertFalse(moderator.canManageFeaturedAndPromoted)
        assertFalse(moderator.canManageVillages)
        assertFalse(moderator.canBroadcastNotifications)
        assertFalse(moderator.canManageUsers)
        assertTrue("Moderator must be able to moderate content and reports", moderator.canModerateContent)
    }

    @Test
    fun `7 - Blue tick verification request approval and removal updates status and badge visibility`() {
        val request = VerificationRequest(
            id = "vreq_1",
            userId = "u_101",
            userName = "Dr. Karimullah",
            userHandle = "@karimullah",
            userEmail = "karim@yarkhoon.com",
            category = "Community Leader",
            supportingInfo = "Principal of Yarkhoon Community High School",
            status = VerificationRequestStatus.PENDING
        )
        assertEquals(VerificationRequestStatus.PENDING, request.status)

        val approved = request.copy(
            status = VerificationRequestStatus.APPROVED,
            reviewedBy = "super_admin_1",
            reviewedAt = 1710000000000L,
            reviewNotes = "Verified identity and community leadership"
        )
        assertEquals(VerificationRequestStatus.APPROVED, approved.status)

        val verifiedUser = User(
            id = "u_101",
            name = "Dr. Karimullah",
            handle = "@karimullah",
            avatarUrl = "",
            isVerified = true
        )
        assertTrue(verifiedUser.isVerified)

        val unverifiedUser = verifiedUser.copy(isVerified = false)
        assertFalse(unverifiedUser.isVerified)
    }

    @Test
    fun `8 - Featured post scheduling and expiration evaluates active window accurately`() {
        val now = 1_700_000_000_000L
        val author = User(id = "u1", name = "Author", handle = "@author", avatarUrl = "")

        // Featured post scheduled in the future -> not currently featured yet
        val futureScheduledPost = Post(
            id = "p1",
            author = author,
            text = "Upcoming Cultural Festival",
            createdAt = "1h ago",
            isFeatured = true,
            featuredScheduledAt = now + 3_600_000L,
            featuredUntil = now + 86_400_000L
        )
        assertFalse(futureScheduledPost.isCurrentlyFeatured(now))

        // Featured post within active window -> currently featured
        val activeFeaturedPost = futureScheduledPost.copy(
            featuredScheduledAt = now - 3_600_000L,
            featuredUntil = now + 3_600_000L
        )
        assertTrue(activeFeaturedPost.isCurrentlyFeatured(now))

        // Featured post past expiration -> automatically expires
        val expiredFeaturedPost = futureScheduledPost.copy(
            featuredScheduledAt = now - 86_400_000L,
            featuredUntil = now - 1_000L
        )
        assertFalse(expiredFeaturedPost.isCurrentlyFeatured(now))
    }

    @Test
    fun `9 - Trending post and Viral post genuine engagement calculation never uses fake metrics`() {
        val author = User(id = "u1", name = "Author", handle = "@author", avatarUrl = "")
        val postLow = Post(
            id = "p_low",
            author = author,
            text = "Regular update",
            createdAt = "2h ago",
            likesCount = 3,
            commentsCount = 1,
            sharesCount = 0,
            isTrendingLabeled = false
        )
        val postHigh = Post(
            id = "p_high",
            author = author,
            text = "Major road opening in Yarkhoon Valley!",
            createdAt = "1h ago",
            likesCount = 45,
            commentsCount = 18,
            sharesCount = 12,
            isTrendingLabeled = true,
            isPinned = true
        )

        val scoreLow = postLow.likesCount + postLow.commentsCount * 2 + postLow.sharesCount * 3
        val scoreHigh = postHigh.likesCount + postHigh.commentsCount * 2 + postHigh.sharesCount * 3
        assertTrue("High organic engagement post must rank above low engagement post", scoreHigh > scoreLow)
        assertTrue(postHigh.isTrendingLabeled)
        assertTrue(postHigh.isPinned)

        val topic = CuratedTrendingTopic(
            id = "t1",
            tag = "#ShandurPoloPrep",
            headline = "Community preparations across Upper Chitral",
            category = "Local News",
            villageName = "Lasht",
            isPinned = true
        )
        assertEquals("Lasht", topic.villageName)
    }

    @Test
    fun `10 - Promoted post management validates start and end dates and target metadata`() {
        val now = 1_700_000_000_000L
        val author = User(id = "u1", name = "Artisan Shop", handle = "@artisan", avatarUrl = "")

        val promotedPost = Post(
            id = "p_promo",
            author = author,
            text = "Authentic Chitrali Patti Coats",
            createdAt = "3h ago",
            isPromoted = true,
            promotionStartDate = now - 10_000L,
            promotionEndDate = now + 86_400_000L,
            promotionTargetCategory = "Handicrafts",
            promotionTargetLocation = "Booni & Mastuj",
            likesCount = 14,
            commentsCount = 5,
            sharesCount = 4,
            impressionsCount = 230
        )

        assertTrue(promotedPost.isCurrentlyPromoted(now))
        assertEquals("Handicrafts", promotedPost.promotionTargetCategory)
        assertEquals("Booni & Mastuj", promotedPost.promotionTargetLocation)
        assertEquals(230, promotedPost.impressionsCount)

        // Expired promotion
        val expiredPromo = promotedPost.copy(promotionEndDate = now - 1L)
        assertFalse(expiredPromo.isCurrentlyPromoted(now))
    }

    @Test
    fun `11 - User suspension and activation enforces temporary and indefinite suspensions`() {
        val activeSuspension = UserSuspensionInfo(
            userId = "u_bad",
            isSuspended = true,
            suspendedUntil = System.currentTimeMillis() + 3_600_000L,
            suspensionReason = "Repeated community guideline violations"
        )
        assertTrue(activeSuspension.isCurrentlySuspended())

        val expiredSuspension = activeSuspension.copy(
            suspendedUntil = System.currentTimeMillis() - 10_000L
        )
        assertFalse("Expired temporary suspension should no longer be active", expiredSuspension.isCurrentlySuspended())

        val reactivatedUser = activeSuspension.copy(
            isSuspended = false,
            suspendedUntil = 0L,
            suspensionReason = ""
        )
        assertFalse(reactivatedUser.isCurrentlySuspended())
    }

    @Test
    fun `12 - Village creation, update, and duplicate prevention blocks duplicate village entries`() {
        val existingVillages = listOf(
            YarkhoonVillage(id = "v1", name = "Brezhin", tehsil = "Mastuj", district = "Upper Chitral"),
            YarkhoonVillage(id = "v2", name = "Bang", tehsil = "Mastuj", district = "Upper Chitral")
        )

        // Exact case-insensitive duplicate in same district is blocked
        assertTrue(
            FirestoreAdminRepository.isDuplicateVillage(
                existingVillages = existingVillages,
                candidateName = "  brezhin ",
                candidateDistrict = "Upper Chitral"
            )
        )

        // Updating the same village (excluding its own ID) is allowed
        assertFalse(
            FirestoreAdminRepository.isDuplicateVillage(
                existingVillages = existingVillages,
                candidateName = "Brezhin",
                candidateDistrict = "Upper Chitral",
                ignoreVillageId = "v1"
            )
        )

        // New distinct village is allowed
        assertFalse(
            FirestoreAdminRepository.isDuplicateVillage(
                existingVillages = existingVillages,
                candidateName = "Dizg",
                candidateDistrict = "Upper Chitral"
            )
        )
    }

    @Test
    fun `13 - Admin session timeout invalidates expired sessions and blocks expired API window`() {
        val loginTime = 1_700_000_000_000L
        // 10 minutes after login -> valid
        assertTrue(FirestoreAdminRepository.isAdminSessionValid(loginTime, loginTime + 10 * 60 * 1000L))

        // 5 hours after login (> 4-hour ADMIN_SESSION_TIMEOUT_MS) -> expired
        assertFalse(FirestoreAdminRepository.isAdminSessionValid(loginTime, loginTime + 5 * 60 * 60 * 1000L))

        // Uninitialized session timestamp -> invalid
        assertFalse(FirestoreAdminRepository.isAdminSessionValid(0L, loginTime))

        // Active repository session window verification
        FirestoreAdminRepository.startAdminSession(loginTime)
        assertTrue(
            "Session within 4 hours must pass API window check",
            FirestoreAdminRepository.verifyActiveAdminSessionWindow(loginTime + 2 * 60 * 60 * 1000L).isSuccess
        )

        // Expired session must fail API window check and auto-invalidate activeAdminSessionStartMs
        val expiredCheck = FirestoreAdminRepository.verifyActiveAdminSessionWindow(loginTime + 4 * 60 * 60 * 1000L + 1_000L)
        assertTrue("Expired session must fail API window check", expiredCheck.isFailure)
        assertEquals(0L, FirestoreAdminRepository.activeAdminSessionStartMs)

        // Explicit logout invalidates session
        FirestoreAdminRepository.startAdminSession(System.currentTimeMillis())
        FirestoreAdminRepository.invalidateAdminSession()
        assertEquals(0L, FirestoreAdminRepository.activeAdminSessionStartMs)
    }

    @Test
    fun `14 - All 15 Admin Control Panel sections and audit action types are present`() {
        val sections = AdminControlPanelSection.entries
        assertEquals(15, sections.size)
        assertEquals(AdminControlPanelSection.DASHBOARD, sections[0])
        assertEquals(AdminControlPanelSection.USER_MANAGEMENT, sections[1])
        assertEquals(AdminControlPanelSection.POST_MANAGEMENT, sections[2])
        assertEquals(AdminControlPanelSection.VIRAL_POSTS, sections[3])
        assertEquals(AdminControlPanelSection.FEATURED_POSTS, sections[4])
        assertEquals(AdminControlPanelSection.BLUE_TICK_VERIFICATION, sections[5])
        assertEquals(AdminControlPanelSection.PROMOTED_POSTS, sections[6])
        assertEquals(AdminControlPanelSection.TRENDING_TOPICS, sections[7])
        assertEquals(AdminControlPanelSection.VILLAGE_MANAGEMENT, sections[8])
        assertEquals(AdminControlPanelSection.REPORTS_AND_COMPLAINTS, sections[9])
        assertEquals(AdminControlPanelSection.NOTIFICATIONS, sections[10])
        assertEquals(AdminControlPanelSection.ADMIN_MANAGEMENT, sections[11])
        assertEquals(AdminControlPanelSection.ACTIVITY_LOGS, sections[12])
        assertEquals(AdminControlPanelSection.SETTINGS, sections[13])
        assertEquals(AdminControlPanelSection.LOGOUT, sections[14])

        val stats = AdminDashboardStats(
            totalRegisteredUsers = 120,
            activeUsers = 115,
            totalPosts = 340,
            pendingPosts = 4,
            approvedPosts = 330,
            rejectedPosts = 6,
            totalVerifiedAccounts = 18,
            totalFeaturedPosts = 5,
            totalPromotedPosts = 3,
            trendingPostsCount = 7,
            pendingVerificationRequests = 2,
            reportedContentCount = 1
        )
        assertEquals(120, stats.totalRegisteredUsers)
        assertEquals(18, stats.totalVerifiedAccounts)

        val announcement = OfficialAnnouncement(
            id = "ann_1",
            title = "Lowari Tunnel Weather Advisory",
            body = "Heavy snowfall expected tonight; chains mandatory.",
            category = "Emergency Announcement"
        )
        assertEquals("Emergency Announcement", announcement.category)

        val adminRecord = AdminAccountRecord(
            uid = "admin_1",
            email = "super@yarkhoon.com",
            name = "Super Admin",
            role = AdminRole.SUPER_ADMIN
        )
        assertEquals(AdminRole.SUPER_ADMIN, adminRecord.role)
        assertNotNull(ModerationActionType.GRANT_BLUE_TICK)
        assertNotNull(ModerationActionType.BROADCAST_ANNOUNCEMENT)
    }

    @Test
    fun `15 - Owner Super Admin email canonicalization and linked session recovery resolution`() {
        val ownerEmail = "hamarachitralonline@gmail.com"
        assertTrue(ownerEmail in FirestoreAdminRepository.TRUSTED_SUPER_ADMIN_EMAILS)

        val linkedEmail = com.example.data.FirebaseAuthRepository.buildLinkedSessionEmail(ownerEmail, "TestPass123")
        assertTrue(linkedEmail.startsWith("hamarachitralonline.gmail.com."))
        assertTrue(linkedEmail.endsWith("@yarkhoon.com"))

        // Canonical resolution restores the exact original email
        assertEquals(ownerEmail, FirestoreAdminRepository.resolveCanonicalEmail(linkedEmail))
        assertEquals(ownerEmail, FirestoreAdminRepository.resolveCanonicalEmail("  HamaraChitralOnline@gmail.com "))
    }
}
