package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreStoryRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        const val STORIES_COLLECTION = "stories"
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    suspend fun getStories(): Result<List<Story>> {
        return try {
            val snapshot = firestore.collection(STORIES_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val stories = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val authorId = data["authorId"] as? String ?: return@mapNotNull null
                val authorName = data["authorName"] as? String ?: "Member"
                val authorAvatar = (data["authorAvatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                    ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
                val mediaUrl = data["mediaUrl"] as? String ?: return@mapNotNull null
                val isPrivateAuthor = (data["isPrivate"] as? Boolean) ?: ((data["authorIsPrivate"] as? Boolean) ?: false)

                Story(
                    id = doc.id,
                    author = User(
                        id = authorId,
                        name = authorName,
                        handle = (data["authorUsername"] as? String) ?: "@$authorId",
                        avatarUrl = authorAvatar,
                        isPrivate = isPrivateAuthor
                    ),
                    mediaUrl = mediaUrl,
                    caption = (data["caption"] as? String).orEmpty(),
                    isViewed = false,
                    createdAt = (data["createdAt"] as? String) ?: "Recently",
                    timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            Result.success(stories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createStory(story: Story): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Must be signed in to post a story."))
        return try {
            val docRef = firestore.collection(STORIES_COLLECTION).document(story.id)
            val data = hashMapOf<String, Any?>(
                "storyId" to story.id,
                "authorId" to uid,
                "authorName" to story.author.name,
                "authorUsername" to story.author.handle,
                "authorAvatarUrl" to story.author.avatarUrl,
                "mediaUrl" to story.mediaUrl,
                "caption" to story.caption,
                "isPrivate" to story.author.isPrivate,
                "authorIsPrivate" to story.author.isPrivate,
                "createdAt" to story.createdAt,
                "timestamp" to story.timestamp
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
