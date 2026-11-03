package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreReelRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val REELS_COLLECTION = "reels"
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    suspend fun getReels(): Result<List<Reel>> {
        return try {
            val snapshot = firestore.collection(REELS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val uid = currentAuthUid
            val reels = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val authorId = data["authorId"] as? String ?: return@mapNotNull null
                val authorName = data["authorName"] as? String ?: "Member"
                val authorAvatar = (data["authorAvatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
                val videoUrl = data["videoMediaUrl"] as? String ?: return@mapNotNull null
                val thumbUrl = (data["thumbnailMediaUrl"] as? String) ?: videoUrl
                val likedBy = (data["likedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val isPrivateAuthor = (data["isPrivate"] as? Boolean) ?: ((data["authorIsPrivate"] as? Boolean) ?: false)

                Reel(
                    id = doc.id,
                    author = User(
                        id = authorId,
                        name = authorName,
                        handle = (data["authorUsername"] as? String) ?: "@$authorId",
                        avatarUrl = authorAvatar,
                        isPrivate = isPrivateAuthor
                    ),
                    videoMediaUrl = videoUrl,
                    thumbnailMediaUrl = thumbUrl,
                    caption = (data["caption"] as? String).orEmpty(),
                    audioTrackTitle = (data["audioTrackTitle"] as? String) ?: "Original Audio • Yarkhoon",
                    likesCount = (data["likesCount"] as? Number)?.toInt() ?: likedBy.size,
                    commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0,
                    sharesCount = (data["sharesCount"] as? Number)?.toInt() ?: 0,
                    isLiked = uid != null && likedBy.contains(uid),
                    isSaved = false,
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            Result.success(reels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReel(reel: Reel): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to upload a reel."))
        return try {
            val docRef = firestore.collection(REELS_COLLECTION).document(reel.id)
            val data = hashMapOf<String, Any?>(
                "reelId" to reel.id,
                "authorId" to uid,
                "authorName" to reel.author.name,
                "authorUsername" to reel.author.handle,
                "authorAvatarUrl" to reel.author.avatarUrl,
                "videoMediaUrl" to reel.videoMediaUrl,
                "thumbnailMediaUrl" to reel.thumbnailMediaUrl,
                "caption" to reel.caption,
                "audioTrackTitle" to reel.audioTrackTitle,
                "likesCount" to 0,
                "commentsCount" to 0,
                "sharesCount" to 0,
                "likedBy" to emptyList<String>(),
                "isPrivate" to reel.author.isPrivate,
                "authorIsPrivate" to reel.author.isPrivate,
                "timestamp" to reel.timestamp
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLikeReel(reelId: String, isLikedNow: Boolean): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            val docRef = firestore.collection(REELS_COLLECTION).document(reelId)
            if (isLikedNow) {
                docRef.update(
                    mapOf(
                        "likedBy" to FieldValue.arrayUnion(uid),
                        "likesCount" to FieldValue.increment(1)
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "likedBy" to FieldValue.arrayRemove(uid),
                        "likesCount" to FieldValue.increment(-1)
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
