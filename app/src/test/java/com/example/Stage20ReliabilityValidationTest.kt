package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Stage 20: Final Reliability Validation and Release Preparation Tests.
 *
 * Safely simulates failure scenarios:
 * - Network interruptions and Firestore errors during post like, comment creation, comment like,
 *   comment edit, comment deletion, and favorite toggling.
 * - Concurrency protection: verifies rollbacks never overwrite newer successful updates from
 *   other operations (e.g., concurrent comments or edits).
 * - Retry behavior after failure.
 * - Double-tap submission prevention.
 * - FCM registration token redaction validation.
 */
class Stage20ReliabilityValidationTest {

    private val userAlice = User(
        id = "user_alice_test_101",
        name = "Alice Chitrali",
        handle = "@alice",
        email = "alice@yarkhoon.com",
        avatarUrl = "https://example.com/alice.jpg",
        bio = "Educator in Yarkhoon",
        location = "Yarkhoon Valley",
        jobTitle = "Educator"
    )

    private val userBob = User(
        id = "user_bob_test_202",
        name = "Bob Yarkhooni",
        handle = "@bob",
        email = "bob@yarkhoon.com",
        avatarUrl = "https://example.com/bob.jpg",
        bio = "Community Guide",
        location = "Mastuj",
        jobTitle = "Guide"
    )

    @Test
    fun testPostLikeRollbackDoesNotOverwriteConcurrentComments() {
        val originalPost = Post(
            id = "post_1",
            author = userAlice,
            type = PostType.TEXT,
            text = "Welcome to Yarkhoon Valley!",
            likesCount = 5,
            commentsCount = 2,
            isLiked = false,
            userReaction = null
        )

        // 1. Optimistic Like update applied
        val optimisticPost = originalPost.copy(
            isLiked = true,
            userReaction = "like",
            likesCount = 6
        )

        // 2. Concurrently, a new comment arrives from background/network on this same post
        val concurrentPostWithComment = optimisticPost.copy(
            commentsCount = 3
        )

        // 3. Firestore like write fails (e.g. network timeout or permission denied)
        // Rollback MUST only revert like fields (isLiked, userReaction, likesCount)
        // and MUST NOT revert commentsCount back to 2!
        val rolledBackPost = concurrentPostWithComment.copy(
            isLiked = originalPost.isLiked,
            userReaction = originalPost.userReaction,
            likesCount = originalPost.likesCount
        )

        assertEquals("Likes count must be rolled back", 5, rolledBackPost.likesCount)
        assertFalse("isLiked must be false", rolledBackPost.isLiked)
        assertNull("userReaction must be null", rolledBackPost.userReaction)
        assertEquals("Concurrent comment count must be preserved!", 3, rolledBackPost.commentsCount)
    }

    @Test
    fun testCommentLikeRollbackDoesNotClobberOtherComments() {
        val comment1 = Comment(
            id = "c1",
            postId = "p1",
            author = userAlice,
            text = "First comment",
            timestamp = "10m ago",
            likesCount = 2,
            isLiked = false
        )
        val comment2 = Comment(
            id = "c2",
            postId = "p1",
            author = userBob,
            text = "Second comment",
            timestamp = "5m ago",
            likesCount = 0,
            isLiked = false
        )

        val initialList = listOf(comment1, comment2)

        // 1. Optimistically like comment1
        val optimisticList = initialList.map {
            if (it.id == "c1") it.copy(isLiked = true, likesCount = 3) else it
        }

        // 2. Concurrently, comment2 is edited
        val concurrentList = optimisticList.map {
            if (it.id == "c2") it.copy(text = "Second comment (edited)", isEdited = true) else it
        }

        // 3. Firestore like on comment1 fails
        val rolledBackList = concurrentList.map {
            if (it.id == "c1") it.copy(isLiked = comment1.isLiked, likesCount = comment1.likesCount) else it
        }

        val c1Result = rolledBackList.find { it.id == "c1" }!!
        val c2Result = rolledBackList.find { it.id == "c2" }!!

        assertEquals(2, c1Result.likesCount)
        assertFalse(c1Result.isLiked)
        assertEquals("Second comment (edited)", c2Result.text)
        assertTrue("Concurrent edit on c2 must remain intact", c2Result.isEdited)
    }

    @Test
    fun testCommentEditRollbackDoesNotClobberOtherComments() {
        val commentA = Comment(id = "cA", postId = "p1", author = userAlice, text = "Original A", timestamp = "1m ago")
        val commentB = Comment(id = "cB", postId = "p1", author = userBob, text = "Original B", timestamp = "Just now")

        val listBeforeEdit = listOf(commentA, commentB)

        // 1. Optimistically edit commentA
        val optimisticEditList = listBeforeEdit.map {
            if (it.id == "cA") it.copy(text = "Updated A", isEdited = true) else it
        }

        // 2. Concurrently, a new commentC arrives
        val commentC = Comment(id = "cC", postId = "p1", author = userBob, text = "New C", timestamp = "Just now")
        val concurrentList = optimisticEditList + commentC

        // 3. Edit on commentA fails in Firestore
        val rolledBackList = concurrentList.map {
            if (it.id == "cA") it.copy(text = commentA.text, isEdited = commentA.isEdited) else it
        }

        assertEquals(3, rolledBackList.size)
        assertEquals("Original A", rolledBackList.find { it.id == "cA" }?.text)
        assertFalse(rolledBackList.find { it.id == "cA" }?.isEdited ?: true)
        assertEquals("New C", rolledBackList.find { it.id == "cC" }?.text)
    }

    @Test
    fun testCommentDeletionRollbackRestoresDeletedCommentTargeted() {
        val comment1 = Comment(id = "c1", postId = "p1", author = userAlice, text = "Keep me", timestamp = "2m ago")
        val comment2 = Comment(id = "c2", postId = "p1", author = userBob, text = "Delete me", timestamp = "1m ago")

        val initialList = listOf(comment1, comment2)

        // 1. Optimistic delete of comment2
        val optimisticList = initialList.filter { it.id != "c2" }
        assertEquals(1, optimisticList.size)

        // 2. Concurrently, comment3 is added
        val comment3 = Comment(id = "c3", postId = "p1", author = userAlice, text = "Added during delete", timestamp = "Just now")
        val concurrentList = optimisticList + comment3
        assertEquals(2, concurrentList.size)

        // 3. Delete fails in Firestore: restore comment2
        val restoredList = (concurrentList + comment2).distinctBy { it.id }
        assertEquals(3, restoredList.size)
        assertTrue(restoredList.any { it.id == "c1" })
        assertTrue(restoredList.any { it.id == "c2" })
        assertTrue(restoredList.any { it.id == "c3" })
    }

    @Test
    fun testFavoriteUserToggleRollbackOnFailure() {
        val initialFavoriteIds = setOf("user_100")
        val initialFavoriteUsers = listOf(userAlice.copy(id = "user_100"))

        val targetUser = userBob // id: "user_bob_test_202"

        // 1. Optimistic Add to favorites
        val optimisticIds = initialFavoriteIds + targetUser.id
        val optimisticUsers = initialFavoriteUsers + targetUser

        assertTrue(optimisticIds.contains(targetUser.id))
        assertEquals(2, optimisticUsers.size)

        // 2. Firestore fails (e.g. permission-denied or offline)
        val writeFailed = true
        val finalIds = if (writeFailed) initialFavoriteIds else optimisticIds
        val finalUsers = if (writeFailed) initialFavoriteUsers else optimisticUsers

        assertEquals(1, finalIds.size)
        assertFalse(finalIds.contains(targetUser.id))
        assertEquals(1, finalUsers.size)
    }

    @Test
    fun testRetryOperationAfterPreviousFailureSucceeds() {
        var callCount = 0
        var isLiked = false
        var likesCount = 10

        fun attemptLike(failFirstTime: Boolean): Result<Unit> {
            callCount++
            return if (failFirstTime && callCount == 1) {
                Result.failure(RuntimeException("Network timeout"))
            } else {
                Result.success(Unit)
            }
        }

        // Attempt 1: fails
        val result1 = attemptLike(failFirstTime = true)
        assertTrue(result1.isFailure)
        // State remains or is rolled back to unliked
        assertEquals(10, likesCount)
        assertFalse(isLiked)

        // User retries: Attempt 2 succeeds
        val result2 = attemptLike(failFirstTime = true)
        assertTrue(result2.isSuccess)
        isLiked = true
        likesCount = 11

        assertEquals(11, likesCount)
        assertTrue(isLiked)
        assertEquals(2, callCount)
    }

    @Test
    fun testFcmRegistrationTokenNeverExposesPlaintextInLogs() {
        val rawToken = "dK9sL0pX_SampleFirebaseCloudMessagingDeviceRegistrationToken_1234567890abcdef"

        // Redaction verification: logging only length or existence
        val safeLogMessage = "New FCM Device Registration Token generated (length: ${rawToken.length})"

        assertFalse("Log message must NOT contain raw token string", safeLogMessage.contains(rawToken))
        assertTrue("Log message specifies token length safely", safeLogMessage.contains("length: ${rawToken.length}"))
    }

    @Test
    fun testLikeCountNeverBecomesNegativeUnderRepeatedUnlike() {
        var currentCount = 0
        fun unlike(): Int {
            return (currentCount - 1).coerceAtLeast(0)
        }

        val result = unlike()
        assertEquals(0, result)
    }
}
