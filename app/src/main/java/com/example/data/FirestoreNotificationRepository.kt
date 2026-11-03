package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreNotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val USERS_COLLECTION = "users"
        const val NOTIFICATIONS_SUBCOLLECTION = "notifications"

        fun formatTimestamp(timestampMs: Long): String {
            val now = System.currentTimeMillis()
            val diff = (now - timestampMs).coerceAtLeast(0)
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 604800_000 -> "${diff / 86400_000}d ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))
            }
        }
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    /**
     * Real-time listener for user's in-app notifications.
     */
    fun listenNotifications(
        recipientId: String,
        onUpdate: (List<NotificationItem>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return firestore.collection(USERS_COLLECTION)
            .document(recipientId)
            .collection(NOTIFICATIONS_SUBCOLLECTION)
            .orderBy("timestampMs", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(mapFirestoreException(error))
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot.documents.mapNotNull { doc ->
                    mapDocToNotification(doc)
                }
                onUpdate(items)
            }
    }

    /**
     * Send in-app notification to a recipient user.
     * Prevents self-notifications and deduplicates based on notification ID.
     */
    suspend fun sendNotification(
        notificationId: String,
        recipientId: String,
        sender: User,
        type: String, // "like", "comment", "reply", "mention", "follow", "follow_request", "follow_accepted"
        message: String,
        targetPostId: String? = null
    ): Result<Unit> {
        // Guard: Never notify users about their own actions
        if (sender.id == recipientId || recipientId.isBlank()) {
            return Result.success(Unit)
        }

        return try {
            val now = System.currentTimeMillis()
            val data = hashMapOf(
                "id" to notificationId,
                "recipientId" to recipientId,
                "senderId" to sender.id,
                "senderName" to sender.name,
                "senderUsername" to sender.handle,
                "senderAvatarUrl" to sender.avatarUrl,
                "type" to type,
                "message" to message,
                "targetPostId" to targetPostId,
                "timestamp" to "Just now",
                "timestampMs" to now,
                "isRead" to false
            )

            // Write to recipient's subcollection users/{recipientId}/notifications/{notificationId}
            firestore.collection(USERS_COLLECTION)
                .document(recipientId)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .set(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Send like notification with deterministic ID to prevent duplicates.
     */
    suspend fun sendLikeNotification(
        recipientId: String,
        sender: User,
        postId: String,
        postSnippet: String
    ): Result<Unit> {
        val snippet = if (postSnippet.isNotBlank()) " on: \"${postSnippet.take(40)}\"" else ""
        val notifId = "like_${postId}_${sender.id}"
        return sendNotification(
            notificationId = notifId,
            recipientId = recipientId,
            sender = sender,
            type = "like",
            message = "liked your post$snippet",
            targetPostId = postId
        )
    }

    /**
     * Send comment notification.
     */
    suspend fun sendCommentNotification(
        recipientId: String,
        sender: User,
        postId: String,
        commentText: String,
        commentId: String
    ): Result<Unit> {
        val notifId = "cmt_${commentId}_${sender.id}"
        return sendNotification(
            notificationId = notifId,
            recipientId = recipientId,
            sender = sender,
            type = "comment",
            message = "commented: \"${commentText.take(50)}\"",
            targetPostId = postId
        )
    }

    /**
     * Send reply notification.
     */
    suspend fun sendReplyNotification(
        recipientId: String,
        sender: User,
        postId: String,
        replyText: String,
        commentId: String
    ): Result<Unit> {
        val notifId = "reply_${commentId}_${sender.id}"
        return sendNotification(
            notificationId = notifId,
            recipientId = recipientId,
            sender = sender,
            type = "reply",
            message = "replied to your comment: \"${replyText.take(50)}\"",
            targetPostId = postId
        )
    }

    /**
     * Send mention notification.
     */
    suspend fun sendMentionNotification(
        mentionedUsername: String,
        sender: User,
        contextMessage: String,
        targetPostId: String? = null
    ): Result<Unit> {
        val cleanHandle = mentionedUsername.removePrefix("@")
        return try {
            // Find target user by username
            val snapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", "@$cleanHandle")
                .limit(1)
                .get()
                .await()

            val targetDoc = snapshot.documents.firstOrNull() ?: return Result.success(Unit)
            val targetUid = targetDoc.id
            if (targetUid == sender.id) return Result.success(Unit)

            val notifId = "mention_${System.currentTimeMillis()}_${sender.id.take(6)}"
            sendNotification(
                notificationId = notifId,
                recipientId = targetUid,
                sender = sender,
                type = "mention",
                message = contextMessage,
                targetPostId = targetPostId
            )
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Send follow notification.
     */
    suspend fun sendFollowNotification(
        recipientId: String,
        sender: User
    ): Result<Unit> {
        val notifId = "follow_${recipientId}_${sender.id}"
        return sendNotification(
            notificationId = notifId,
            recipientId = recipientId,
            sender = sender,
            type = "follow",
            message = "started following you."
        )
    }

    /**
     * Send follow request notification.
     */
    suspend fun sendFollowRequestNotification(
        recipientId: String,
        sender: User
    ): Result<Unit> {
        val notifId = "freq_${recipientId}_${sender.id}"
        return sendNotification(
            notificationId = notifId,
            recipientId = recipientId,
            sender = sender,
            type = "follow_request",
            message = "requested to follow you."
        )
    }

    /**
     * Send follow approval notification.
     */
    suspend fun sendFollowApprovalNotification(
        recipientId: String,
        sender: User
    ): Result<Unit> {
        val notifId = "fappr_${recipientId}_${sender.id}"
        return sendNotification(
            notificationId = notifId,
            recipientId = recipientId,
            sender = sender,
            type = "follow_accepted",
            message = "approved your follow request."
        )
    }

    /**
     * Mark a single notification as read.
     */
    suspend fun markAsRead(recipientId: String, notificationId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(recipientId)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Mark all unread notifications as read.
     */
    suspend fun markAllAsRead(recipientId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection(USERS_COLLECTION)
                .document(recipientId)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Delete a single notification.
     */
    suspend fun deleteNotification(recipientId: String, notificationId: String): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(recipientId)
                .collection(NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    private fun mapDocToNotification(doc: com.google.firebase.firestore.DocumentSnapshot): NotificationItem? {
        val data = doc.data ?: return null
        val id = doc.id
        val recipientId = data["recipientId"] as? String ?: ""
        val senderId = data["senderId"] as? String ?: return null
        val senderName = data["senderName"] as? String ?: "Member"
        val senderHandle = data["senderUsername"] as? String ?: "@$senderId"
        val senderAvatarUrl = (data["senderAvatarUrl"] as? String).orEmpty()
        val type = (data["type"] as? String) ?: "general"
        val message = (data["message"] as? String).orEmpty()
        val isRead = (data["isRead"] as? Boolean) ?: false
        val timestampMs = (data["timestampMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val timestampStr = (data["timestamp"] as? String)?.takeIf { it != "Just now" } ?: formatTimestamp(timestampMs)
        val targetPostId = data["targetPostId"] as? String

        val sender = User(
            id = senderId,
            name = senderName,
            handle = senderHandle,
            avatarUrl = senderAvatarUrl
        )

        return NotificationItem(
            id = id,
            sender = sender,
            type = type,
            message = message,
            timestamp = timestampStr,
            isRead = isRead,
            recipientId = recipientId,
            targetPostId = targetPostId,
            timestampMs = timestampMs
        )
    }

    private fun mapFirestoreException(e: Exception): Exception {
        if (e is FirebaseFirestoreException) {
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied: Please ensure you are signed in."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Network unavailable: Please check your internet connection."
                else -> e.message ?: "Firestore notification error."
            }
            return Exception(message, e)
        }
        return e
    }
}
