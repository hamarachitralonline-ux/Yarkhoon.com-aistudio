package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YarkhoonFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "YarkhoonFCM"

        // Direct Messages Channel
        const val CHANNEL_ID_MESSAGES = "yarkhoon_direct_messages"
        const val CHANNEL_NAME_MESSAGES = "Direct Messages"
        const val CHANNEL_DESCRIPTION_MESSAGES = "Notifications for private messages and chat updates on YARKHOON.COM"

        // Friend Requests Channel
        const val CHANNEL_ID_FRIEND_REQUESTS = "yarkhoon_friend_requests"
        const val CHANNEL_NAME_FRIEND_REQUESTS = "Friend & Follow Requests"
        const val CHANNEL_DESCRIPTION_FRIEND_REQUESTS = "Notifications for friend and follow requests on YARKHOON.COM"

        // Backward compatibility
        const val CHANNEL_ID = CHANNEL_ID_MESSAGES
        const val CHANNEL_NAME = CHANNEL_NAME_MESSAGES
        const val CHANNEL_DESCRIPTION = CHANNEL_DESCRIPTION_MESSAGES
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Device Registration Token generated (length: ${token.length})")
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    FirestoreUserRepository().updateFcmToken(currentUid, token)
                    Log.d(TAG, "Updated FCM token for authenticated user: $currentUid")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update FCM token for user: ${e.message}", e)
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Received FCM message from: ${remoteMessage.from}, data=${remoteMessage.data}")

        val type = remoteMessage.data["type"]
            ?: if (remoteMessage.data.containsKey("conversationId")) "direct_message"
               else if (remoteMessage.data.containsKey("requesterId")) "friend_request"
               else "general"

        when (type) {
            "friend_request", "follow_request" -> {
                val title = remoteMessage.notification?.title
                    ?: remoteMessage.data["title"]
                    ?: "New Friend Request"

                val requesterName = remoteMessage.data["requesterName"] ?: "A member"
                val body = remoteMessage.notification?.body
                    ?: remoteMessage.data["body"]
                    ?: "$requesterName sent you a friend request on YARKHOON.COM."

                val requesterId = remoteMessage.data["requesterId"]
                    ?: remoteMessage.data["senderId"]
                    ?: ""

                showFriendRequestNotification(title, body, requesterId)
            }

            "direct_message", "message", "chat" -> {
                val senderName = remoteMessage.data["senderName"] ?: "Someone"
                val title = remoteMessage.notification?.title
                    ?: remoteMessage.data["title"]
                    ?: "Message from $senderName"

                val body = remoteMessage.notification?.body
                    ?: remoteMessage.data["body"]
                    ?: remoteMessage.data["text"]
                    ?: "You received a new message."

                val conversationId = remoteMessage.data["conversationId"]
                    ?: remoteMessage.data["id"]
                    ?: ""

                showDirectMessageNotification(title, body, conversationId)
            }

            else -> {
                val conversationId = remoteMessage.data["conversationId"].orEmpty()
                if (conversationId.isNotEmpty()) {
                    val title = remoteMessage.notification?.title
                        ?: remoteMessage.data["title"]
                        ?: "New Message on YARKHOON.COM"
                    val body = remoteMessage.notification?.body
                        ?: remoteMessage.data["body"]
                        ?: remoteMessage.data["text"]
                        ?: "You received a new message."
                    showDirectMessageNotification(title, body, conversationId)
                } else {
                    val title = remoteMessage.notification?.title
                        ?: remoteMessage.data["title"]
                        ?: "New Notification"
                    val body = remoteMessage.notification?.body
                        ?: remoteMessage.data["body"]
                        ?: "You have a new social update on YARKHOON.COM."
                    showFriendRequestNotification(title, body, "")
                }
            }
        }
    }

    private fun showDirectMessageNotification(title: String, message: String, conversationId: String) {
        createNotificationChannels()

        // Verify runtime notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted; suppressing system notification.")
                return
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", conversationId)
            putExtra("notificationType", "direct_message")
            putExtra("fromNotification", true)
        }

        val requestCode = if (conversationId.isNotEmpty()) conversationId.hashCode() else System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(requestCode, notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while notifying DM: ${e.message}")
        }
    }

    private fun showFriendRequestNotification(title: String, message: String, requesterId: String) {
        createNotificationChannels()

        // Verify runtime notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted; suppressing system notification.")
                return
            }
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openScreen", "notifications")
            putExtra("notificationType", "friend_request")
            putExtra("requesterId", requesterId)
            putExtra("fromNotification", true)
        }

        val requestCode = if (requesterId.isNotEmpty()) ("freq_$requesterId").hashCode() else System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_FRIEND_REQUESTS)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this)
        try {
            notificationManager.notify(requestCode, notificationBuilder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException while notifying friend request: ${e.message}")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Direct Messages Channel
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_MESSAGES
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(messagesChannel)

            // 2. Friend Requests Channel
            val friendRequestsChannel = NotificationChannel(
                CHANNEL_ID_FRIEND_REQUESTS,
                CHANNEL_NAME_FRIEND_REQUESTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_FRIEND_REQUESTS
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(friendRequestsChannel)
        }
    }
}
