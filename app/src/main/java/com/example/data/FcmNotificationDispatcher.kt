package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * FcmNotificationDispatcher handles formatting, token resolution, and dispatching
 * Firebase Cloud Messaging (FCM) notifications for friend requests and direct messages.
 */
object FcmNotificationDispatcher {

    private const val TAG = "FcmNotificationDispatcher"

    const val TYPE_FRIEND_REQUEST = "friend_request"
    const val TYPE_DIRECT_MESSAGE = "direct_message"

    const val CHANNEL_MESSAGES = "yarkhoon_direct_messages"
    const val CHANNEL_FRIEND_REQUESTS = "yarkhoon_friend_requests"

    data class NotificationPayload(
        val recipientToken: String?,
        val recipientId: String,
        val type: String,
        val title: String,
        val body: String,
        val channelId: String,
        val data: Map<String, String>
    )

    /**
     * Builds standard FCM payload for direct messages.
     */
    fun createDirectMessagePayload(
        sender: User,
        recipientId: String,
        recipientToken: String?,
        conversationId: String,
        messageText: String
    ): NotificationPayload {
        val snippet = if (messageText.length > 90) {
            "${messageText.take(87)}..."
        } else {
            messageText
        }

        return NotificationPayload(
            recipientToken = recipientToken,
            recipientId = recipientId,
            type = TYPE_DIRECT_MESSAGE,
            title = "Message from ${sender.name}",
            body = snippet,
            channelId = CHANNEL_MESSAGES,
            data = mapOf(
                "type" to TYPE_DIRECT_MESSAGE,
                "conversationId" to conversationId,
                "senderId" to sender.id,
                "senderName" to sender.name,
                "text" to snippet
            )
        )
    }

    /**
     * Builds standard FCM payload for friend / follow requests.
     */
    fun createFriendRequestPayload(
        requester: User,
        targetUserId: String,
        targetUserToken: String?
    ): NotificationPayload {
        return NotificationPayload(
            recipientToken = targetUserToken,
            recipientId = targetUserId,
            type = TYPE_FRIEND_REQUEST,
            title = "New Friend Request",
            body = "${requester.name} sent you a friend request on YARKHOON.COM.",
            channelId = CHANNEL_FRIEND_REQUESTS,
            data = mapOf(
                "type" to TYPE_FRIEND_REQUEST,
                "requesterId" to requester.id,
                "requesterName" to requester.name,
                "targetUserId" to targetUserId
            )
        )
    }

    /**
     * Dispatches direct message notification after saving a chat message.
     */
    suspend fun dispatchDirectMessageNotification(
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
        sender: User,
        recipientId: String,
        conversationId: String,
        messageText: String
    ): Result<NotificationPayload> {
        return try {
            val token = resolveFcmToken(firestore, recipientId)
            val payload = createDirectMessagePayload(
                sender = sender,
                recipientId = recipientId,
                recipientToken = token,
                conversationId = conversationId,
                messageText = messageText
            )

            Log.d(TAG, "Prepared DM FCM notification for $recipientId (token available: ${token != null})")
            Result.success(payload)
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving recipient FCM token for DM: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Dispatches friend / follow request notification.
     */
    suspend fun dispatchFriendRequestNotification(
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
        requester: User,
        targetUserId: String
    ): Result<NotificationPayload> {
        return try {
            val token = resolveFcmToken(firestore, targetUserId)
            val payload = createFriendRequestPayload(
                requester = requester,
                targetUserId = targetUserId,
                targetUserToken = token
            )

            Log.d(TAG, "Prepared Friend Request FCM notification for $targetUserId (token available: ${token != null})")
            Result.success(payload)
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving target FCM token for friend request: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun resolveFcmToken(firestore: FirebaseFirestore, userId: String): String? {
        if (userId.isBlank()) return null
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            doc.getString("fcmToken")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch FCM token for $userId: ${e.message}")
            null
        }
    }
}
