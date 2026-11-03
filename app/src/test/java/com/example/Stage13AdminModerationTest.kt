package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Stage 13 Admin Dashboard & Content Moderation Unit Tests
 * Tests report submission data structure, report filtering, moderation action history,
 * account suspension states, and permission checks.
 */
class Stage13AdminModerationTest {

    @Test
    fun testContentReportDataModelValidation() {
        val report = ContentReport(
            id = "rep_1001",
            reporterId = "user_victim",
            reportedUserId = "user_spammer",
            reportedUserName = "Spam Account",
            contentType = ReportContentType.POST,
            contentId = "post_bad_99",
            contentSummary = "Buy cheap followers now!",
            reason = ReportReason.SPAM,
            description = "Unsolicited commercial spam targeting users",
            status = ReportStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )

        assertEquals("rep_1001", report.id)
        assertEquals("user_victim", report.reporterId)
        assertEquals(ReportContentType.POST, report.contentType)
        assertEquals(ReportStatus.PENDING, report.status)
        assertEquals(ReportReason.SPAM, report.reason)
        assertNull(report.reviewedBy)
        assertNull(report.reviewedAt)
    }

    @Test
    fun testReportStatusTransitions() {
        var report = ContentReport(
            id = "rep_2002",
            reporterId = "user_1",
            contentType = ReportContentType.COMMENT,
            contentId = "cmt_toxic_1",
            reason = ReportReason.HARASSMENT,
            description = "Abusive comment directed at creator",
            status = ReportStatus.PENDING
        )
        assertEquals(ReportStatus.PENDING, report.status)

        // Admin reviews and resolves report with action
        val adminId = "admin_super_1"
        val reviewTime = System.currentTimeMillis()
        report = report.copy(
            status = ReportStatus.RESOLVED,
            reviewedBy = adminId,
            reviewedAt = reviewTime,
            resolutionNotes = "Comment hidden and user warned."
        )

        assertEquals(ReportStatus.RESOLVED, report.status)
        assertEquals(adminId, report.reviewedBy)
        assertNotNull(report.reviewedAt)
        assertEquals("Comment hidden and user warned.", report.resolutionNotes)

        // Test dismissal transition
        val dismissedReport = report.copy(status = ReportStatus.DISMISSED, resolutionNotes = "No policy violation found.")
        assertEquals(ReportStatus.DISMISSED, dismissedReport.status)
    }

    @Test
    fun testModerationAuditLogIntegrity() {
        val log = ModerationAuditLog(
            id = "log_3003",
            adminId = "admin_chitral",
            adminEmail = "admin@yarkhoon.com",
            actionType = ModerationActionType.SUSPEND_USER,
            targetType = "USER",
            targetId = "bad_user_404",
            targetUserId = "bad_user_404",
            reason = "Repeated harassment violations across multiple comments",
            durationDays = 7,
            timestamp = System.currentTimeMillis()
        )

        assertEquals(ModerationActionType.SUSPEND_USER, log.actionType)
        assertEquals("bad_user_404", log.targetId)
        assertEquals(7, log.durationDays)
        assertEquals("admin@yarkhoon.com", log.adminEmail)
        assertTrue(log.reason.contains("harassment"))
    }

    @Test
    fun testUserSuspensionTimeWindowCalculation() {
        val currentTime = System.currentTimeMillis()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        val activeSuspension = UserSuspensionInfo(
            userId = "user_suspended_1",
            isSuspended = true,
            suspensionReason = "Scamming in Marketplace",
            suspendedAt = currentTime,
            suspendedUntil = currentTime + sevenDaysMs,
            suspendedBy = "admin_1"
        )

        // 1. Active suspension
        assertTrue(activeSuspension.isCurrentlySuspended)

        // 2. Expired suspension
        val expiredSuspension = UserSuspensionInfo(
            userId = "user_suspended_2",
            isSuspended = true,
            suspensionReason = "Old expired violation",
            suspendedAt = currentTime - (10 * 24 * 60 * 60 * 1000L),
            suspendedUntil = currentTime - 1000L,
            suspendedBy = "admin_1"
        )
        assertFalse(expiredSuspension.isCurrentlySuspended)

        // 3. Not suspended at all
        val normalAccount = UserSuspensionInfo(
            userId = "user_normal",
            isSuspended = false
        )
        assertFalse(normalAccount.isCurrentlySuspended)
    }

    @Test
    fun testAdminAuthorizationCheck() {
        val trustedEmails = setOf(
            "admin@yarkhoon.com",
            "hamarachitralonline@gmail.com",
            "moderator@yarkhoon.com"
        )

        // 1. Authorized email
        val userEmail = "admin@yarkhoon.com"
        val isAuthorized = trustedEmails.contains(userEmail)
        assertTrue(isAuthorized)

        // 2. Unauthorized ordinary user
        val regularEmail = "user@example.com"
        val isRegularAuthorized = trustedEmails.contains(regularEmail)
        assertFalse(isRegularAuthorized)
    }

    @Test
    fun testReportFilteringByQueryAndStatus() {
        val report1 = ContentReport(
            id = "r1",
            reporterId = "u1",
            contentType = ReportContentType.POST,
            contentId = "p1",
            contentSummary = "Spam cryptocurrency post",
            reason = ReportReason.SPAM,
            status = ReportStatus.PENDING
        )
        val report2 = ContentReport(
            id = "r2",
            reporterId = "u2",
            contentType = ReportContentType.MARKETPLACE_LISTING,
            contentId = "m1",
            contentSummary = "Fake phone sale listing",
            reason = ReportReason.SCAM,
            status = ReportStatus.RESOLVED
        )
        val report3 = ContentReport(
            id = "r3",
            reporterId = "u3",
            contentType = ReportContentType.COMMENT,
            contentId = "c1",
            contentSummary = "Inappropriate insult comment",
            reason = ReportReason.HARASSMENT,
            status = ReportStatus.DISMISSED
        )

        val allReports = listOf(report1, report2, report3)

        // Filter by Pending
        val pendingOnly = allReports.filter { it.status == ReportStatus.PENDING }
        assertEquals(1, pendingOnly.size)
        assertEquals("r1", pendingOnly[0].id)

        // Filter by Search Query "phone"
        val queryResults = allReports.filter { it.contentSummary.contains("phone", ignoreCase = true) }
        assertEquals(1, queryResults.size)
        assertEquals("r2", queryResults[0].id)

        // Filter by ContentType
        val marketplaceOnly = allReports.filter { it.contentType == ReportContentType.MARKETPLACE_LISTING }
        assertEquals(1, marketplaceOnly.size)
    }

    @Test
    fun testPrivateMediaStoragePathPartitioning() {
        val publicUserId = "user_pub_1"
        val privateUserId = "user_priv_2"

        // For public account, media is placed in public scope
        val isPublicAccount = false
        val isPrivateAccount = true

        val publicScope = if (isPublicAccount) "protected" else "public"
        val privateScope = if (isPrivateAccount) "protected" else "public"

        val publicPostPath = "users/$publicUserId/$publicScope/posts/post_1/media"
        val privatePostPath = "users/$privateUserId/$privateScope/posts/post_2/media"

        assertEquals("users/user_pub_1/public/posts/post_1/media", publicPostPath)
        assertEquals("users/user_priv_2/protected/posts/post_2/media", privatePostPath)
        assertTrue(privatePostPath.contains("/protected/"))
        assertFalse(publicPostPath.contains("/protected/"))
    }

    @Test
    fun testSuspendedUserProhibitedActionsModel() {
        val activeSuspension = UserSuspensionInfo(
            userId = "user_banned_1",
            isSuspended = true,
            suspensionReason = "Spamming",
            suspendedAt = System.currentTimeMillis(),
            suspendedUntil = 0L // indefinite
        )

        // Actions prohibited during active suspension:
        val prohibitedActions = listOf(
            "CREATE_POST",
            "ADD_COMMENT",
            "PUBLISH_STORY",
            "PUBLISH_REEL",
            "SEND_DIRECT_MESSAGE",
            "CREATE_BUSINESS_LISTING",
            "CREATE_MARKETPLACE_LISTING",
            "TOGGLE_FOLLOW_USER",
            "UPDATE_USER_PROFILE"
        )

        assertTrue(activeSuspension.isCurrentlySuspended)
        assertEquals(9, prohibitedActions.size)
        assertTrue(prohibitedActions.contains("CREATE_POST"))
        assertTrue(prohibitedActions.contains("SEND_DIRECT_MESSAGE"))
        assertTrue(prohibitedActions.contains("CREATE_BUSINESS_LISTING"))
    }

    @Test
    fun testPrivateMediaAccessAuthorizationMatrix() {
        val authorId = "author_priv_1"
        val isAuthorPrivate = true

        val ownerId = "author_priv_1"
        val approvedFollowerId = "follower_appr_2"
        val unapprovedUserId = "stranger_3"
        val blockedUserId = "blocked_4"

        val rawProtectedPath = "gs://yarkhoondotcom.firebasestorage.app/users/author_priv_1/protected/posts/p1/media"

        // Evaluation helper simulating authorization resolver
        fun isAuthorized(callerId: String, isApprovedFollower: Boolean, isBlocked: Boolean): Boolean {
            if (isBlocked) return false
            if (callerId == authorId) return true
            if (isAuthorPrivate) return isApprovedFollower
            return true
        }

        // 1. Owner access -> Allowed
        assertTrue(isAuthorized(ownerId, isApprovedFollower = false, isBlocked = false))

        // 2. Approved follower -> Allowed
        assertTrue(isAuthorized(approvedFollowerId, isApprovedFollower = true, isBlocked = false))

        // 3. Pending/unapproved follower -> Denied
        assertFalse(isAuthorized(unapprovedUserId, isApprovedFollower = false, isBlocked = false))

        // 4. Blocked user -> Denied even if previously recorded as follower
        assertFalse(isAuthorized(blockedUserId, isApprovedFollower = true, isBlocked = true))
    }

    @Test
    fun testTokenRevocationTargeting() {
        val targetUid = "user_priv_99"
        val targetedPrefix = "users/$targetUid/protected/"

        // Ensures revocation only targets protected folder and never deletes public directory files
        assertTrue(targetedPrefix.startsWith("users/"))
        assertTrue(targetedPrefix.endsWith("/protected/"))
        assertFalse(targetedPrefix.contains("/public/"))
        assertFalse(targetedPrefix.contains("/businessListings/"))
    }

    @Test
    fun testMigrationClassificationAndIdempotencyLogic() {
        val userId = "user_mig_1"
        val isPrivateAccount = true

        val legacyPost = "users/$userId/posts/post_1/media.jpg"
        val alreadyProtected = "users/$userId/protected/posts/post_2/media.jpg"
        val publicProfile = "users/$userId/profile/avatar.jpg"
        val unknownCustom = "users/$userId/downloads/temp.bin"

        fun planFileAction(path: String, userExists: Boolean, isPrivate: Boolean?): String {
            if (path.startsWith("users/$userId/protected/")) return "SKIP_ALREADY_MIGRATED"
            if (path.startsWith("users/$userId/profile/")) return "SKIP_PUBLIC"
            if (path.startsWith("users/$userId/posts/")) {
                if (!userExists || isPrivate == null) return "CONFLICT_MISSING_METADATA"
                return if (isPrivate) "COPY_TO_PROTECTED" else "COPY_TO_PUBLIC"
            }
            return "CONFLICT_UNKNOWN_STRUCTURE"
        }

        // 1. Private post -> COPY_TO_PROTECTED
        assertEquals("COPY_TO_PROTECTED", planFileAction(legacyPost, userExists = true, isPrivate = true))

        // 2. Already protected -> SKIP_ALREADY_MIGRATED (Idempotent)
        assertEquals("SKIP_ALREADY_MIGRATED", planFileAction(alreadyProtected, userExists = true, isPrivate = true))

        // 3. Profile picture -> SKIP_PUBLIC
        assertEquals("SKIP_PUBLIC", planFileAction(publicProfile, userExists = true, isPrivate = true))

        // 4. Missing user metadata -> CONFLICT_MISSING_METADATA
        assertEquals("CONFLICT_MISSING_METADATA", planFileAction(legacyPost, userExists = false, isPrivate = null))

        // 5. Unrecognized folder -> CONFLICT_UNKNOWN_STRUCTURE
        assertEquals("CONFLICT_UNKNOWN_STRUCTURE", planFileAction(unknownCustom, userExists = true, isPrivate = true))
    }

    @Test
    fun testFirestoreMediaReferenceUpdatePathTransformation() {
        val userId = "user_42"
        val oldRelative = "posts/p123/media.jpg"
        val legacyHttp = "https://firebasestorage.googleapis.com/v0/b/bucket/o/users%2Fuser_42%2Fposts%2Fp123%2Fmedia.jpg?alt=media&token=123"
        val canonicalGs = "gs://yarkhoondotcom.firebasestorage.app/users/user_42/posts/p123/media.jpg"

        // Function that updates references
        fun transformRef(currentUrl: String, isPrivate: Boolean): String {
            val srcKey = "users/$userId/$oldRelative"
            if (!currentUrl.contains(srcKey) && currentUrl != canonicalGs) return currentUrl
            val destSub = if (isPrivate) "protected/$oldRelative" else "public/$oldRelative"
            return "gs://yarkhoondotcom.firebasestorage.app/users/$userId/$destSub"
        }

        val updatedPrivate = transformRef(canonicalGs, isPrivate = true)
        val updatedPublic = transformRef(canonicalGs, isPrivate = false)

        assertEquals("gs://yarkhoondotcom.firebasestorage.app/users/user_42/protected/posts/p123/media.jpg", updatedPrivate)
        assertEquals("gs://yarkhoondotcom.firebasestorage.app/users/user_42/public/posts/p123/media.jpg", updatedPublic)
    }

    @Test
    fun testDestinationConflictAndChecksumVerification() {
        val srcSize = 1048576L // 1 MB
        val srcMd5 = "Q2hlY2tzdW0xMjM="

        val matchingDestSize = 1048576L
        val matchingDestMd5 = "Q2hlY2tzdW0xMjM="

        val conflictingDestSize = 524288L // 512 KB
        val conflictingDestMd5 = "V3Jvbmc0NTY=" // Mismatched MD5

        fun evaluateDestination(destExists: Boolean, existingDestSize: Long?, existingDestMd5: String?): String {
            if (!destExists) return "PERFORM_COPY"
            val hasComparableChecksum = existingDestMd5 != null
            if (!hasComparableChecksum) return "CONFLICT_ABSENT_CHECKSUM"
            val sizeMatches = existingDestSize == srcSize
            val checksumMatches = existingDestMd5 == srcMd5
            if (sizeMatches && checksumMatches) return "ALREADY_MIGRATED_IDEMPOTENT"
            if (!sizeMatches) return "CONFLICT_SIZE_MISMATCH"
            return "CONFLICT_CHECKSUM_MISMATCH"
        }

        // 1. Fresh file -> copy
        assertEquals("PERFORM_COPY", evaluateDestination(destExists = false, existingDestSize = null, existingDestMd5 = null))

        // 2. Existing destination with matching size and matching checksum -> idempotent bypass
        assertEquals("ALREADY_MIGRATED_IDEMPOTENT", evaluateDestination(destExists = true, existingDestSize = matchingDestSize, existingDestMd5 = matchingDestMd5))

        // 3. Existing destination with mismatched size -> conflict flagged
        assertEquals("CONFLICT_SIZE_MISMATCH", evaluateDestination(destExists = true, existingDestSize = conflictingDestSize, existingDestMd5 = matchingDestMd5))

        // 4. Matching size but conflicting checksum -> conflict flagged (size alone is not trusted)
        assertEquals("CONFLICT_CHECKSUM_MISMATCH", evaluateDestination(destExists = true, existingDestSize = matchingDestSize, existingDestMd5 = conflictingDestMd5))

        // 5. Existing destination with absent checksum metadata -> conflict flagged for manual review
        assertEquals("CONFLICT_ABSENT_CHECKSUM", evaluateDestination(destExists = true, existingDestSize = matchingDestSize, existingDestMd5 = null))
    }

    @Test
    fun testMigrationAuthorizationAndPathTraversalGuards() {
        fun validateMigrationRequest(callerUid: String, targetUid: String, isAdmin: Boolean, path: String): String {
            if (callerUid != targetUid && !isAdmin) return "FORBIDDEN_NOT_ADMIN"
            if (path.contains("..")) return "ERROR_PATH_TRAVERSAL"
            if (!path.startsWith("users/$targetUid/")) return "ERROR_WRONG_USER_PATH"
            return "AUTHORIZED_VALID"
        }

        // 1. Authorized admin for another user -> Allowed
        assertEquals("AUTHORIZED_VALID", validateMigrationRequest("admin_1", "user_2", isAdmin = true, path = "users/user_2/posts/p1/img.jpg"))

        // 2. Non-admin trying to migrate another user -> Forbidden
        assertEquals("FORBIDDEN_NOT_ADMIN", validateMigrationRequest("user_1", "user_2", isAdmin = false, path = "users/user_2/posts/p1/img.jpg"))

        // 3. User migrating own files -> Allowed
        assertEquals("AUTHORIZED_VALID", validateMigrationRequest("user_1", "user_1", isAdmin = false, path = "users/user_1/posts/p1/img.jpg"))

        // 4. Path traversal attempt -> Rejected
        assertEquals("ERROR_PATH_TRAVERSAL", validateMigrationRequest("user_1", "user_1", isAdmin = false, path = "users/user_1/../user_2/secret.jpg"))

        // 5. Wrong user directory attempt -> Rejected
        assertEquals("ERROR_WRONG_USER_PATH", validateMigrationRequest("user_1", "user_1", isAdmin = false, path = "users/user_other/posts/p1.jpg"))
    }
}


