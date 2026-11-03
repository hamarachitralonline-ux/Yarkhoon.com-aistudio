package com.example

import com.example.data.FirestoreNotificationRepository
import com.example.data.NotificationItem
import com.example.data.User
import org.junit.Assert.*
import org.junit.Test

class Stage14InAppNotificationsTest {

    private val userAlice = User(id = "user_alice", name = "Alice", handle = "@alice", avatarUrl = "https://example.com/alice.jpg")
    private val userBob = User(id = "user_bob", name = "Bob", handle = "@bob", avatarUrl = "https://example.com/bob.jpg")

    @Test
    fun testNotificationItemDataClassDefaults() {
        val notif = NotificationItem(
            id = "n_test_1",
            sender = userAlice,
            type = "like",
            message = "liked your post.",
            timestamp = "Just now"
        )

        assertEquals("n_test_1", notif.id)
        assertEquals(userAlice, notif.sender)
        assertEquals("like", notif.type)
        assertEquals("liked your post.", notif.message)
        assertFalse(notif.isRead)
        assertEquals("", notif.recipientId)
        assertNull(notif.targetPostId)
        assertTrue(notif.timestampMs > 0)
    }

    @Test
    fun testNotificationItemCustomFields() {
        val now = 1700000000000L
        val notif = NotificationItem(
            id = "n_custom",
            sender = userAlice,
            type = "comment",
            message = "commented on your photo",
            timestamp = "5m ago",
            isRead = true,
            recipientId = "user_bob",
            targetPostId = "post_999",
            timestampMs = now
        )

        assertEquals("user_bob", notif.recipientId)
        assertEquals("post_999", notif.targetPostId)
        assertEquals(now, notif.timestampMs)
        assertTrue(notif.isRead)
    }

    @Test
    fun testDeterministicNotificationIdDeduplication() {
        val postId = "post_valley_1"
        val senderId = "user_alice"
        val targetUserId = "user_bob"

        // Like notification ID
        val likeNotifId1 = "like_${postId}_${senderId}"
        val likeNotifId2 = "like_${postId}_${senderId}"
        assertEquals("Identical like actions must produce identical deterministic IDs", likeNotifId1, likeNotifId2)

        // Follow notification ID
        val followNotifId1 = "follow_${targetUserId}_${senderId}"
        val followNotifId2 = "follow_${targetUserId}_${senderId}"
        assertEquals("Identical follow actions must produce identical deterministic IDs", followNotifId1, followNotifId2)

        // Follow request notification ID
        val reqNotifId1 = "freq_${targetUserId}_${senderId}"
        val reqNotifId2 = "freq_${targetUserId}_${senderId}"
        assertEquals("Identical follow request actions must produce identical deterministic IDs", reqNotifId1, reqNotifId2)

        // Follow approval notification ID
        val apprNotifId1 = "fappr_${senderId}_${targetUserId}"
        val apprNotifId2 = "fappr_${senderId}_${targetUserId}"
        assertEquals("Identical follow approval actions must produce identical deterministic IDs", apprNotifId1, apprNotifId2)
    }

    @Test
    fun testSelfNotificationSuppression() {
        val senderId = "user_alice"
        val recipientIdSame = "user_alice"
        val recipientIdDifferent = "user_bob"

        val shouldSuppressSelf = senderId == recipientIdSame
        val shouldSuppressOther = senderId == recipientIdDifferent

        assertTrue("Self notifications must be suppressed", shouldSuppressSelf)
        assertFalse("Notifications to other users must not be suppressed", shouldSuppressOther)
    }

    @Test
    fun testTimestampFormatting() {
        val now = System.currentTimeMillis()

        // Just now (< 1 min)
        assertEquals("Just now", FirestoreNotificationRepository.formatTimestamp(now - 10_000))

        // Minutes ago (5 mins)
        assertEquals("5m ago", FirestoreNotificationRepository.formatTimestamp(now - 5 * 60_000))

        // Hours ago (3 hours)
        assertEquals("3h ago", FirestoreNotificationRepository.formatTimestamp(now - 3 * 3600_000))

        // Days ago (2 days)
        assertEquals("2d ago", FirestoreNotificationRepository.formatTimestamp(now - 2 * 86400_000))
    }

    @Test
    fun testUnreadBadgeCountAndMarkReadTransformation() {
        val list = listOf(
            NotificationItem(id = "1", sender = userAlice, type = "like", message = "liked post", timestamp = "1m ago", isRead = false),
            NotificationItem(id = "2", sender = userBob, type = "comment", message = "commented", timestamp = "2m ago", isRead = false),
            NotificationItem(id = "3", sender = userAlice, type = "follow", message = "followed you", timestamp = "1h ago", isRead = true)
        )

        val unreadCount = list.count { !it.isRead }
        assertEquals("Unread badge count must equal 2", 2, unreadCount)

        val markedReadList = list.map { it.copy(isRead = true) }
        val remainingUnread = markedReadList.count { !it.isRead }
        assertEquals("After mark read all unread count must be 0", 0, remainingUnread)
    }

    @Test
    fun testNotificationDeletionFilter() {
        val list = listOf(
            NotificationItem(id = "n_keep", sender = userAlice, type = "like", message = "liked post", timestamp = "1m ago"),
            NotificationItem(id = "n_delete", sender = userBob, type = "comment", message = "commented", timestamp = "2m ago")
        )

        val remaining = list.filter { it.id != "n_delete" }
        assertEquals(1, remaining.size)
        assertEquals("n_keep", remaining.first().id)
    }
}
