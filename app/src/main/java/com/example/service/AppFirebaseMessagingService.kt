package com.example.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Firebase Cloud Messaging token refreshed: $token")
        FcmNotificationManager.updateToken(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM message received from: ${remoteMessage.from}")

        // 1. Extract payload data
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val type = data["type"] ?: data["notification_type"] ?: "GENERAL"
        val targetId = data["target_id"] ?: data["targetId"]
        val senderId = data["sender_id"] ?: data["senderId"] ?: ""
        val senderName = data["sender_name"] ?: data["senderName"] ?: ""
        val senderAvatarUrl = data["sender_avatar"] ?: data["senderAvatarUrl"] ?: ""

        val title = notification?.title
            ?: data["title"]
            ?: when (type.uppercase()) {
                "FRIEND_REQUEST" -> "New Friend Request"
                "GROUP_MESSAGE", "GROUP" -> "New Group Message"
                "LIKE" -> "New Like"
                "COMMENT" -> "New Comment"
                else -> "Yarkhoon Notification"
            }

        val body = notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: when (type.uppercase()) {
                "FRIEND_REQUEST" -> "$senderName sent you a friend request"
                "GROUP_MESSAGE", "GROUP" -> "You have a new message in your group"
                "LIKE" -> "$senderName liked your post"
                "COMMENT" -> "$senderName commented on your post"
                else -> "You have a new update in Yarkhoon"
            }

        Log.d(TAG, "Dispatching notification - Type: $type, Title: $title, Target: $targetId")

        // 2. Dispatch system push notification and persist to Room database
        FcmNotificationManager.showNotification(
            context = applicationContext,
            type = type,
            title = title,
            body = body,
            targetId = targetId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            saveToRoom = true
        )
    }

    companion object {
        private const val TAG = "AppFirebaseMsgService"
    }
}
