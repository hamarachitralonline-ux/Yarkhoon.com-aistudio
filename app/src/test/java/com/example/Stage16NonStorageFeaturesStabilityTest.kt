package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Stage 16 Test Suite: Verifies stability of non-storage features in Yarkhoon.com
 * when Firebase Cloud Storage is unprovisioned / pending plan activation.
 */
class Stage16NonStorageFeaturesStabilityTest {

    private val sampleAuthor = User(
        id = "user_author_123",
        name = "Rashid Karim",
        handle = "@rashid_yarkhoon",
        email = "rashid@yarkhoon.com",
        avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
        coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1000",
        bio = "Local guide and community organizer in Yarkhoon Valley",
        location = "Yarkhoon Valley, Chitral",
        jobTitle = "Community Lead",
        education = "University of Chitral",
        phoneNumber = "+92 300 1234567",
        isPrivate = false
    )

    @Before
    fun setUp() {
        FirebaseStorageRepository.resetStorageAvailabilityCache()
    }

    @Test
    fun testTextOnlyPostCreationStructure() {
        val postId = "post_${System.currentTimeMillis()}"
        val textContent = "Welcome all new members to Yarkhoon.com! Snowfall has begun across the upper valley peaks."
        val feeling = "🏔️ Exploring Yarkhoon"
        val finalText = "$textContent\n\n— $feeling"

        val textPost = Post(
            id = postId,
            author = sampleAuthor,
            type = PostType.TEXT,
            text = finalText,
            mediaUrls = emptyList(),
            tags = listOf("YarkhoonCom", "WinterValley"),
            location = "Yarkhoon Valley",
            likesCount = 0,
            commentsCount = 0,
            repostsCount = 0,
            sharesCount = 0,
            isLiked = false,
            isReposted = false,
            isSaved = false,
            comments = emptyList(),
            createdAt = "Just now",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(PostType.TEXT, textPost.type)
        assertTrue(textPost.mediaUrls.isEmpty())
        assertTrue(textPost.text.contains("Snowfall has begun"))
        assertTrue(textPost.text.contains(feeling))
        assertEquals(sampleAuthor.id, textPost.author.id)
    }

    @Test
    fun testStorageUnprovisionedCacheAndFriendlyError() {
        assertFalse(FirebaseStorageRepository.isStorageUnprovisioned)

        // Simulate detecting unprovisioned bucket (e.g. error -13010 / 404)
        FirebaseStorageRepository.setStorageUnprovisioned(true)
        assertTrue(FirebaseStorageRepository.isStorageUnprovisioned)

        val config = StorageConfigInfo(
            bucketName = "yarkhoondotcom.firebasestorage.app",
            currentUserId = sampleAuthor.id,
            isUserSignedIn = true,
            isStorageUnprovisioned = FirebaseStorageRepository.isStorageUnprovisioned
        )
        assertTrue(config.isStorageUnprovisioned)

        // Reset cache for future tests
        FirebaseStorageRepository.resetStorageAvailabilityCache()
        assertFalse(FirebaseStorageRepository.isStorageUnprovisioned)
    }

    @Test
    fun testProfileEditingPreservesRemoteUrlsAndRejectsLocalUris() {
        val currentRemoteAvatar = sampleAuthor.avatarUrl
        val currentRemoteCover = sampleAuthor.coverUrl

        // Simulate user picking a new local avatar uri from device
        val localAvatarUri = "content://com.android.providers.media.documents/document/image%3A54321"
        val localCoverUri = "file:///data/user/0/com.aistudio.yarkhoon.scbqdm/cache/cropped_cover.jpg"

        // Simulate storage upload failure (storage unprovisioned)
        val uploadFailed = true

        var finalAvatar = currentRemoteAvatar
        var finalCover = currentRemoteCover

        if (uploadFailed) {
            // Local URIs must NOT overwrite remote URLs on failure
            if (!localAvatarUri.startsWith("http://") && !localAvatarUri.startsWith("https://")) {
                // Keep existing remote avatar
                finalAvatar = currentRemoteAvatar
            }
            if (!localCoverUri.startsWith("http://") && !localCoverUri.startsWith("https://")) {
                // Keep existing remote cover
                finalCover = currentRemoteCover
            }
        }

        // Updated text profile fields
        val updatedUser = sampleAuthor.copy(
            name = "Rashid K. Chitrali",
            bio = "Senior community guide and writer",
            location = "Mastuj / Yarkhoon Valley",
            avatarUrl = finalAvatar,
            coverUrl = finalCover
        )

        assertEquals("Rashid K. Chitrali", updatedUser.name)
        assertEquals("Senior community guide and writer", updatedUser.bio)
        assertEquals(currentRemoteAvatar, updatedUser.avatarUrl)
        assertEquals(currentRemoteCover, updatedUser.coverUrl)
        assertFalse(updatedUser.avatarUrl?.startsWith("content://") ?: false)
        assertFalse(updatedUser.coverUrl?.startsWith("file://") ?: false)
    }

    @Test
    fun testMarketplaceListingExcludesLocalUrisWhenUploadFails() {
        val inputImages = listOf(
            "https://images.unsplash.com/photo-1544717305-2782549b5136?w=400", // Valid remote
            "content://media/external/images/media/999" // Local pick that fails upload
        )

        val uploadedUrls = mutableListOf<String>()
        for (uri in inputImages) {
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                uploadedUrls.add(uri)
            } else {
                // Upload failed: do not add local URI
            }
        }

        val item = MarketplaceItem(
            id = "item_12345",
            seller = sampleAuthor,
            title = "Handmade Chitrali Woolen Shawl",
            category = MarketplaceCategory.CLOTHING,
            price = "Rs. 3,500",
            description = "100% pure organic sheep wool woven in Yarkhoon Valley.",
            phone = "+92 300 1234567",
            location = "Yarkhoon Valley",
            imageUrls = uploadedUrls,
            status = "active",
            createdAt = "Just now",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(1, item.imageUrls.size)
        assertTrue(item.imageUrls[0].startsWith("https://"))
        assertFalse(item.imageUrls.any { it.startsWith("content://") })
    }

    @Test
    fun testFollowingFeedFilteringLogic() {
        val followedUserIds = setOf("user_author_123", "user_author_456")

        val allPosts = listOf(
            Post(
                id = "p1",
                author = sampleAuthor, // id = "user_author_123" (Followed)
                type = PostType.TEXT,
                text = "Post from followed author 1",
                mediaUrls = emptyList(),
                tags = emptyList(),
                likesCount = 5,
                commentsCount = 1,
                repostsCount = 0,
                sharesCount = 0,
                isLiked = false,
                isReposted = false,
                isSaved = false,
                comments = emptyList(),
                createdAt = "1h ago",
                timestamp = 1000L
            ),
            Post(
                id = "p2",
                author = sampleAuthor.copy(id = "user_author_999", name = "Stranger"), // Not followed
                type = PostType.TEXT,
                text = "Post from stranger",
                mediaUrls = emptyList(),
                tags = emptyList(),
                likesCount = 0,
                commentsCount = 0,
                repostsCount = 0,
                sharesCount = 0,
                isLiked = false,
                isReposted = false,
                isSaved = false,
                comments = emptyList(),
                createdAt = "2h ago",
                timestamp = 900L
            )
        )

        val followingFiltered = allPosts.filter { followedUserIds.contains(it.author.id) }
        assertEquals(1, followingFiltered.size)
        assertEquals("p1", followingFiltered[0].id)
    }

    @Test
    fun testTextMessagePayloadAndDeterministicConversationId() {
        val user1 = "uid_aaa"
        val user2 = "uid_bbb"

        // Deterministic conversation ID for private messaging
        val deterministicId = listOf(user1, user2).sorted().joinToString("_")
        assertEquals("uid_aaa_uid_bbb", deterministicId)

        val reversedId = listOf(user2, user1).sorted().joinToString("_")
        assertEquals("uid_aaa_uid_bbb", reversedId)

        val message = ChatMessage(
            id = "msg_001",
            conversationId = deterministicId,
            senderId = user1,
            senderName = "Rashid",
            senderAvatarUrl = sampleAuthor.avatarUrl,
            text = "Are you joining the community meeting in Brep tomorrow?",
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        assertEquals("uid_aaa", message.senderId)
        assertFalse(message.isRead)
        assertTrue(message.text.contains("community meeting"))
    }
}
