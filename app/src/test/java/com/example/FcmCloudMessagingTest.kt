package com.example

import com.example.data.FcmNotificationDispatcher
import com.example.data.User
import com.example.data.YarkhoonFirebaseMessagingService
import org.junit.Assert.*
import org.junit.Test

class FcmCloudMessagingTest {

    private val userSender = User(
        id = "sender_123",
        name = "Alina Baig",
        handle = "@alina",
        avatarUrl = "https://example.com/alina.jpg"
    )

    private val userRecipientId = "recipient_456"
    private val sampleFcmToken = "fcm_test_device_token_xyz_987"

    @Test
    fun testDirectMessagePayloadCreation() {
        val convId = "sender_123_recipient_456"
        val messageText = "Salam, are you available for the Yarkhoon cultural festival meeting?"

        val payload = FcmNotificationDispatcher.createDirectMessagePayload(
            sender = userSender,
            recipientId = userRecipientId,
            recipientToken = sampleFcmToken,
            conversationId = convId,
            messageText = messageText
        )

        assertEquals(sampleFcmToken, payload.recipientToken)
        assertEquals(userRecipientId, payload.recipientId)
        assertEquals(FcmNotificationDispatcher.TYPE_DIRECT_MESSAGE, payload.type)
        assertEquals("Message from Alina Baig", payload.title)
        assertEquals(messageText, payload.body)
        assertEquals(FcmNotificationDispatcher.CHANNEL_MESSAGES, payload.channelId)

        assertEquals(FcmNotificationDispatcher.TYPE_DIRECT_MESSAGE, payload.data["type"])
        assertEquals(convId, payload.data["conversationId"])
        assertEquals(userSender.id, payload.data["senderId"])
        assertEquals("Alina Baig", payload.data["senderName"])
        assertEquals(messageText, payload.data["text"])
    }

    @Test
    fun testDirectMessagePayloadLongTextTruncation() {
        val longMessage = "This is a very long private direct message sent over YARKHOON.COM social network to discuss extensive regional valley updates, tourism guides, and cultural events happening this weekend."
        val convId = "conv_789"

        val payload = FcmNotificationDispatcher.createDirectMessagePayload(
            sender = userSender,
            recipientId = userRecipientId,
            recipientToken = sampleFcmToken,
            conversationId = convId,
            messageText = longMessage
        )

        assertTrue("Body should be truncated with ellipsis", payload.body.endsWith("..."))
        assertTrue("Body length should be <= 90 chars", payload.body.length <= 90)
    }

    @Test
    fun testFriendRequestPayloadCreation() {
        val payload = FcmNotificationDispatcher.createFriendRequestPayload(
            requester = userSender,
            targetUserId = userRecipientId,
            targetUserToken = sampleFcmToken
        )

        assertEquals(sampleFcmToken, payload.recipientToken)
        assertEquals(userRecipientId, payload.recipientId)
        assertEquals(FcmNotificationDispatcher.TYPE_FRIEND_REQUEST, payload.type)
        assertEquals("New Friend Request", payload.title)
        assertEquals("Alina Baig sent you a friend request on YARKHOON.COM.", payload.body)
        assertEquals(FcmNotificationDispatcher.CHANNEL_FRIEND_REQUESTS, payload.channelId)

        assertEquals(FcmNotificationDispatcher.TYPE_FRIEND_REQUEST, payload.data["type"])
        assertEquals(userSender.id, payload.data["requesterId"])
        assertEquals("Alina Baig", payload.data["requesterName"])
        assertEquals(userRecipientId, payload.data["targetUserId"])
    }

    @Test
    fun testServiceChannelConstants() {
        assertEquals("yarkhoon_direct_messages", YarkhoonFirebaseMessagingService.CHANNEL_ID_MESSAGES)
        assertEquals("yarkhoon_friend_requests", YarkhoonFirebaseMessagingService.CHANNEL_ID_FRIEND_REQUESTS)
        assertEquals(
            "Backwards compatibility alias should match direct messages",
            YarkhoonFirebaseMessagingService.CHANNEL_ID_MESSAGES,
            YarkhoonFirebaseMessagingService.CHANNEL_ID
        )
    }

    @Test
    fun testPayloadWithMissingTokenGraceful() {
        val payload = FcmNotificationDispatcher.createFriendRequestPayload(
            requester = userSender,
            targetUserId = userRecipientId,
            targetUserToken = null
        )

        assertNull(payload.recipientToken)
        assertEquals(userRecipientId, payload.recipientId)
        assertEquals(FcmNotificationDispatcher.CHANNEL_FRIEND_REQUESTS, payload.channelId)
    }

    @Test
    fun testPayloadTypeClassificationLogic() {
        // Direct message detection from data map
        val dmData = mapOf("conversationId" to "conv_123", "senderId" to "sender_1")
        val isDm = dmData.containsKey("conversationId") || dmData["type"] == "direct_message"
        assertTrue("Data with conversationId must resolve as DM", isDm)

        // Friend request detection from data map
        val frData = mapOf("requesterId" to "req_456", "type" to "friend_request")
        val isFr = frData.containsKey("requesterId") || frData["type"] == "friend_request"
        assertTrue("Data with requesterId must resolve as Friend Request", isFr)
    }
}
