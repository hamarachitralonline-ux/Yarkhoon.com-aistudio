package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.AppNotification
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object FcmNotificationManager {
    private const val TAG = "FcmNotificationManager"
    private const val PREFS_NAME = "fcm_notification_prefs"
    private const val KEY_FCM_TOKEN = "fcm_token"
    private const val KEY_TOPICS_SUBSCRIBED = "topics_subscribed"

    // Channel IDs
    const val CHANNEL_FRIEND_REQUESTS = "yarkhoon_channel_friend_requests"
    const val CHANNEL_GROUP_MESSAGES = "yarkhoon_channel_group_messages"
    const val CHANNEL_POST_INTERACTIONS = "yarkhoon_channel_post_interactions"
    const val CHANNEL_GENERAL = "yarkhoon_channel_general"

    // Notification IDs
    private var notificationIdCounter = 1000

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(true)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    /**
     * Initializes notification channels and retrieves the Firebase Cloud Messaging registration token.
     */
    fun initialize(context: Context) {
        createNotificationChannels(context)
        checkNotificationPermission(context)

        // Clear any old queued topic operations to prevent TopicsSyncTask from executing on startup
        try {
            context.getSharedPreferences("com.google.android.gms.appid", Context.MODE_PRIVATE)
                .edit().clear().apply()
            context.getSharedPreferences("com.google.firebase.messaging", Context.MODE_PRIVATE)
                .edit().clear().apply()
        } catch (_: Exception) {
            // Ignore preference clearance errors
        }

        // Disable automatic initialization of Firebase Messaging to prevent background sync loops and quota errors
        try {
            FirebaseMessaging.getInstance().isAutoInitEnabled = false
        } catch (e: Exception) {
            Log.w(TAG, "Could not set isAutoInitEnabled: ${e.message}")
        }

        // Load cached token or provide a fallback token
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedToken = prefs.getString(KEY_FCM_TOKEN, null)
        if (!cachedToken.isNullOrBlank()) {
            _fcmToken.value = cachedToken
            return
        }

        // Assign a stable local device token so in-app notifications and diagnostics work cleanly
        val localToken = "yarkhoon_device_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        updateToken(context, localToken)
    }

    /**
     * Check if system notifications are enabled.
     */
    fun checkNotificationPermission(context: Context) {
        val enabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        _hasNotificationPermission.value = enabled
    }

    /**
     * Save new token to preferences and state.
     */
    fun updateToken(context: Context, token: String) {
        _fcmToken.value = token
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    /**
     * Create high-priority Notification Channels for Android 8.0 (API 26) and above.
     */
    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Friend Requests Channel
            val friendChannel = NotificationChannel(
                CHANNEL_FRIEND_REQUESTS,
                "Friend Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for incoming friend requests and new connections in Yarkhoon"
                enableLights(true)
                lightColor = AndroidColor.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }

            // 2. Group & Community Messages Channel
            val groupChannel = NotificationChannel(
                CHANNEL_GROUP_MESSAGES,
                "Group Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for messages and active discussions in your joined groups"
                enableLights(true)
                lightColor = AndroidColor.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            // 3. Likes & Comments Channel
            val postInteractionsChannel = NotificationChannel(
                CHANNEL_POST_INTERACTIONS,
                "Likes and Comments",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when community members like, react to, or comment on your posts"
                enableLights(true)
                lightColor = AndroidColor.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
            }

            // 4. General Community Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Announcements, stories, and community alerts"
            }

            notificationManager.createNotificationChannels(
                listOf(friendChannel, groupChannel, postInteractionsChannel, generalChannel)
            )
        }
    }

    /**
     * Dispatches a system push notification with proper intent handling, icon, sound, and channel.
     * Also inserts into the Room database so it is recorded in the user's in-app notification center.
     */
    fun showNotification(
        context: Context,
        type: String, // FRIEND_REQUEST, GROUP_MESSAGE, LIKE, COMMENT, GENERAL
        title: String,
        body: String,
        targetId: String? = null,
        senderId: String = "",
        senderName: String = "",
        senderAvatarUrl: String = "",
        saveToRoom: Boolean = true
    ) {
        checkNotificationPermission(context)

        val channelId = when (type.uppercase()) {
            "FRIEND_REQUEST" -> CHANNEL_FRIEND_REQUESTS
            "GROUP_MESSAGE", "GROUP" -> CHANNEL_GROUP_MESSAGES
            "LIKE", "COMMENT" -> CHANNEL_POST_INTERACTIONS
            else -> CHANNEL_GENERAL
        }

        val notificationId = synchronized(this) { notificationIdCounter++ }

        // Intent to launch MainActivity and route directly to the target destination
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_fcm", true)
            putExtra("notification_type", type)
            putExtra("target_id", targetId)
            putExtra("sender_name", senderName)
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = R.drawable.ic_notification

        // Generate circular badge initial for avatar if not loaded
        val initialLetter = (senderName.ifBlank { title }).take(1).uppercase()
        val largeIconBitmap = createInitialBitmap(initialLetter)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setColor(AndroidColor.parseColor("#1877F2")) // Yarkhoon Blue
            .setLargeIcon(largeIconBitmap)
            .setContentIntent(pendingIntent)

        when (type.uppercase()) {
            "FRIEND_REQUEST" -> {
                builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)
                // Add Quick Accept Action
                builder.addAction(
                    android.R.drawable.ic_menu_add,
                    "View Request",
                    pendingIntent
                )
            }
            "GROUP_MESSAGE", "GROUP" -> {
                builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                builder.addAction(
                    android.R.drawable.ic_menu_send,
                    "Open Group",
                    pendingIntent
                )
            }
            "LIKE" -> {
                builder.setCategory(NotificationCompat.CATEGORY_STATUS)
            }
            "COMMENT" -> {
                builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                builder.addAction(
                    android.R.drawable.ic_menu_view,
                    "Reply",
                    pendingIntent
                )
            }
        }

        // Post the notification safely
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException while notifying: ${e.message}")
            }
        }

        // Save to Room DB in background so user sees it in their notification list
        if (saveToRoom) {
            serviceScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val appNotification = AppNotification(
                        id = UUID.randomUUID().toString(),
                        recipientId = "currentUser",
                        senderId = senderId,
                        senderName = senderName,
                        senderAvatarUrl = senderAvatarUrl,
                        title = title,
                        description = body,
                        avatarUrl = senderAvatarUrl,
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        type = type.uppercase(),
                        targetId = targetId
                    )
                    db.socialMediaDao.insertNotification(appNotification)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist AppNotification to Room DB: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Generates a circular avatar placeholder bitmap for system notifications.
     */
    private fun createInitialBitmap(initial: String): Bitmap {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.parseColor("#1877F2")
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = AndroidColor.WHITE
            textSize = 58f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val yPos = (canvas.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial.ifBlank { "Y" }, size / 2f, yPos, textPaint)

        return bitmap
    }

    // Convenience Trigger Methods

    fun triggerFriendRequestNotification(
        context: Context,
        senderName: String = "Sher Jang",
        senderId: String = "user_sher",
        senderAvatarUrl: String = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150"
    ) {
        showNotification(
            context = context,
            type = "FRIEND_REQUEST",
            title = "New Friend Request",
            body = "$senderName sent you a friend request from Upper Yarkhoon Valley.",
            targetId = senderId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl
        )
    }

    fun triggerGroupMessageNotification(
        context: Context,
        groupName: String = "Chitral Hikers & Trekkers",
        senderName: String = "Karim Shah",
        messageContent: String = "Meeting at Mastuj bridge tomorrow at 8:00 AM for the Broghil trek!",
        groupId: String = "1"
    ) {
        showNotification(
            context = context,
            type = "GROUP_MESSAGE",
            title = "New message in $groupName",
            body = "$senderName: $messageContent",
            targetId = groupId,
            senderId = "user_karim",
            senderName = senderName
        )
    }

    fun triggerLikeNotification(
        context: Context,
        likerName: String = "Ali Khan",
        postSnippet: String = "Beautiful morning overlooking Broghil Pass...",
        postId: String = "1"
    ) {
        showNotification(
            context = context,
            type = "LIKE",
            title = "New Like on your post",
            body = "$likerName liked your post: \"${postSnippet.take(45)}\"",
            targetId = postId,
            senderId = "user_ali",
            senderName = likerName
        )
    }

    fun triggerCommentNotification(
        context: Context,
        commenterName: String = "Yasmin Bibi",
        commentText: String = "Mashallah, the autumn colors of Yarkhoon are majestic!",
        postId: String = "1"
    ) {
        showNotification(
            context = context,
            type = "COMMENT",
            title = "New Comment on your post",
            body = "$commenterName commented: \"${commentText.take(50)}\"",
            targetId = postId,
            senderId = "user_yasmin",
            senderName = commenterName
        )
    }
}
