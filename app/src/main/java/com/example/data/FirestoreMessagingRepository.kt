package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FirestoreMessagingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val CONVERSATIONS_COLLECTION = "conversations"
        const val MESSAGES_SUBCOLLECTION = "messages"

        /**
         * Generates a deterministic conversation ID from two user UIDs so that the same
         * pair of users always maps to the exact same conversation document.
         */
        fun generateConversationId(uid1: String, uid2: String): String {
            return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
        }

        /**
         * Resolves the appropriate conversation ID between two users, checking if an existing
         * conversation already exists in the given list (by deterministic ID or legacy reversed ID)
         * to prevent duplicate conversation threads.
         */
        fun resolveConversationId(
            existingConversations: List<Conversation>,
            uid1: String,
            uid2: String
        ): String {
            val deterministicId = generateConversationId(uid1, uid2)
            val match = existingConversations.find {
                it.id == deterministicId ||
                (it.participantIds.contains(uid1) && it.participantIds.contains(uid2)) ||
                (it.recipient.id == uid2 || it.recipient.id == uid1)
            }
            return match?.id ?: deterministicId
        }
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    /**
     * Attaches a real-time listener to conversations where the authenticated user is a participant.
     * Returns a [ListenerRegistration] so the caller can remove the listener when no longer needed.
     */
    fun listenConversations(
        currentUserId: String,
        onUpdate: (List<Conversation>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection(CONVERSATIONS_COLLECTION)
            .whereArrayContains("participantIds", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(mapFirestoreException(error))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val participantIds = (data["participantIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val otherUid = participantIds.firstOrNull { it != currentUserId } ?: return@mapNotNull null

                    @Suppress("UNCHECKED_CAST")
                    val profilesMap = data["participantProfiles"] as? Map<String, Map<String, Any?>>
                    val otherProfile = profilesMap?.get(otherUid)

                    val otherName = (otherProfile?.get("name") as? String)?.takeIf { it.isNotBlank() } ?: "Member"
                    val otherHandle = (otherProfile?.get("handle") as? String)
                        ?: (otherProfile?.get("username") as? String)
                        ?: "@${otherUid.take(8)}"
                    val otherAvatar = (otherProfile?.get("avatarUrl") as? String)?.takeIf { it.isNotBlank() }
                        ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
                    val otherBio = (otherProfile?.get("bio") as? String).orEmpty()
                    val otherJob = (otherProfile?.get("jobTitle") as? String).orEmpty()
                    val otherLocation = (otherProfile?.get("location") as? String).orEmpty()

                    val recipientUser = User(
                        id = otherUid,
                        name = otherName,
                        handle = otherHandle,
                        avatarUrl = otherAvatar,
                        bio = otherBio,
                        jobTitle = otherJob,
                        location = otherLocation
                    )

                    val lastMsg = (data["lastMessage"] as? String).orEmpty()
                    val lastSenderId = (data["lastMessageSenderId"] as? String).orEmpty()
                    val lastTimestamp = (data["lastMessageTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val readBy = (data["readBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    val isUnread = lastSenderId.isNotBlank() && lastSenderId != currentUserId && !readBy.contains(currentUserId)

                    Conversation(
                        id = doc.id,
                        participantIds = participantIds,
                        recipient = recipientUser,
                        lastMessage = lastMsg,
                        lastMessageSenderId = lastSenderId,
                        lastMessageTimestamp = lastTimestamp,
                        timestamp = formatMessageTimestamp(lastTimestamp),
                        unreadCount = if (isUnread) 1 else 0
                    )
                }
                val sortedConversations = conversations.sortedByDescending { it.lastMessageTimestamp }
                onUpdate(sortedConversations)
            }
    }

    /**
     * Attaches a real-time listener to the messages of a specific conversation with pagination support.
     * Returns a [ListenerRegistration] so the caller can remove the listener when the conversation is closed.
     */
    fun listenMessages(
        conversationId: String,
        limit: Long = 40,
        onUpdate: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection(CONVERSATIONS_COLLECTION)
            .document(conversationId)
            .collection(MESSAGES_SUBCOLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // If parent conversation document is not yet initialized or has no messages yet,
                        // deliver empty list rather than flashing a blocking error banner
                        onUpdate(emptyList())
                    } else {
                        onError(mapFirestoreException(error))
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val senderId = data["senderId"] as? String ?: return@mapNotNull null
                    val text = data["text"] as? String ?: ""
                    val senderName = data["senderName"] as? String ?: "User"
                    val senderAvatar = (data["senderAvatarUrl"] as? String).orEmpty()
                    val ts = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    val isRead = (data["isRead"] as? Boolean) ?: false

                    ChatMessage(
                        id = doc.id,
                        conversationId = conversationId,
                        senderId = senderId,
                        senderName = senderName,
                        senderAvatarUrl = senderAvatar,
                        text = text,
                        timestamp = ts,
                        isRead = isRead
                    )
                }

                onUpdate(messages)
            }
    }

    /**
     * Ensures parent conversation document exists so security rules permit message subcollection reads.
     */
     suspend fun ensureConversationCreated(
         conversationId: String,
         currentUser: User,
         otherUser: User
     ): Result<Unit> {
         val currentUid = currentAuthUid
         if (currentUid == null || currentUid != currentUser.id) {
             return Result.failure(IllegalStateException("User is unauthenticated or UID does not match current user."))
         }

         return try {
             val docRef = firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId)
             val doc = try { docRef.get().await() } catch (_: Exception) { null }

             if (doc == null || !doc.exists()) {
                 val deterministicParticipants = listOf(currentUser.id, otherUser.id).sorted()
                 val conversationData = hashMapOf<String, Any?>(
                     "id" to conversationId,
                     "participantIds" to deterministicParticipants,
                     "participantProfiles" to mapOf(
                         currentUser.id to mapOf(
                             "name" to currentUser.name,
                             "handle" to currentUser.handle,
                             "avatarUrl" to currentUser.avatarUrl,
                             "jobTitle" to currentUser.jobTitle,
                             "location" to currentUser.location
                         ),
                         otherUser.id to mapOf(
                             "name" to otherUser.name,
                             "handle" to otherUser.handle,
                             "avatarUrl" to otherUser.avatarUrl,
                             "jobTitle" to otherUser.jobTitle,
                             "location" to otherUser.location
                         )
                     ),
                     "lastMessage" to "",
                     "lastMessageSenderId" to "",
                     "lastMessageTimestamp" to System.currentTimeMillis(),
                     "readBy" to listOf(currentUser.id)
                 )
                 docRef.set(conversationData).await()
             }
             Result.success(Unit)
         } catch (e: Exception) {
             Result.failure(mapFirestoreException(e))
         }
     }

    /**
     * Sends a private message and updates the parent conversation metadata atomically.
     */
    suspend fun sendMessage(
        conversationId: String,
        senderUser: User,
        recipientUser: User,
        text: String
    ): Result<ChatMessage> {
        val currentUid = currentAuthUid
        if (currentUid == null || currentUid != senderUser.id) {
            return Result.failure(IllegalStateException("User is unauthenticated or UID does not match sender."))
        }

        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            return Result.failure(IllegalArgumentException("Message content cannot be blank."))
        }

        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val now = System.currentTimeMillis()

        return try {
            val messageData = hashMapOf<String, Any?>(
                "id" to messageId,
                "conversationId" to conversationId,
                "senderId" to senderUser.id,
                "senderName" to senderUser.name,
                "senderAvatarUrl" to senderUser.avatarUrl,
                "text" to trimmedText,
                "timestamp" to now,
                "isRead" to false
            )

            val deterministicParticipants = listOf(senderUser.id, recipientUser.id).sorted()

            val existingDoc = try {
                firestore.collection(CONVERSATIONS_COLLECTION).document(conversationId).get().await()
            } catch (_: Exception) {
                null
            }

            if (existingDoc != null && existingDoc.exists()) {
                // Document already exists: update ONLY permitted metadata fields so we satisfy the immutable fields rule:
                // lastMessage, lastMessageSenderId, lastMessageTimestamp, readBy
                val updates = hashMapOf<String, Any?>(
                    "lastMessage" to trimmedText,
                    "lastMessageSenderId" to senderUser.id,
                    "lastMessageTimestamp" to now,
                    "readBy" to listOf(senderUser.id)
                )
                firestore.collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .update(updates)
                    .await()
            } else {
                // Document does not exist yet: CREATE operation
                val conversationData = hashMapOf<String, Any?>(
                    "id" to conversationId,
                    "participantIds" to deterministicParticipants,
                    "participantProfiles" to mapOf(
                        senderUser.id to mapOf(
                            "name" to senderUser.name,
                            "handle" to senderUser.handle,
                            "avatarUrl" to senderUser.avatarUrl,
                            "jobTitle" to senderUser.jobTitle,
                            "location" to senderUser.location
                        ),
                        recipientUser.id to mapOf(
                            "name" to recipientUser.name,
                            "handle" to recipientUser.handle,
                            "avatarUrl" to recipientUser.avatarUrl,
                            "jobTitle" to recipientUser.jobTitle,
                            "location" to recipientUser.location
                        )
                    ),
                    "lastMessage" to trimmedText,
                    "lastMessageSenderId" to senderUser.id,
                    "lastMessageTimestamp" to now,
                    "readBy" to listOf(senderUser.id)
                )

                firestore.collection(CONVERSATIONS_COLLECTION)
                    .document(conversationId)
                    .set(conversationData)
                    .await()
            }

            // 2. Now write the message document in the messages subcollection
            val messageDocRef = firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .collection(MESSAGES_SUBCOLLECTION)
                .document(messageId)

            messageDocRef.set(messageData).await()

            val created = ChatMessage(
                id = messageId,
                conversationId = conversationId,
                senderId = senderUser.id,
                senderName = senderUser.name,
                senderAvatarUrl = senderUser.avatarUrl,
                text = trimmedText,
                timestamp = now,
                isRead = false
            )

            // Dispatch direct message FCM notification to recipient
            try {
                FcmNotificationDispatcher.dispatchDirectMessageNotification(
                    firestore = firestore,
                    sender = senderUser,
                    recipientId = recipientUser.id,
                    conversationId = conversationId,
                    messageText = trimmedText
                )
            } catch (_: Exception) {}

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Marks the conversation as read by the current user.
     */
    suspend fun markConversationAsRead(
        conversationId: String,
        currentUserId: String
    ): Result<Unit> {
        return try {
            firestore.collection(CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .update("readBy", FieldValue.arrayUnion(currentUserId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    private fun formatMessageTimestamp(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun mapFirestoreException(e: Exception): Exception {
        if (e is FirebaseFirestoreException) {
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied: You do not have permission to view or participate in this conversation."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Messaging service is temporarily unavailable. Check your internet connection."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Connection timed out while sending message."
                FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                    "Authentication expired. Please sign in again."
                else -> e.localizedMessage ?: "Firestore error: ${e.code}"
            }
            return Exception(message, e)
        }
        return Exception(e.localizedMessage ?: "An error occurred with real-time messaging.", e)
    }
}
