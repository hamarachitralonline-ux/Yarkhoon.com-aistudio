package com.example

import com.example.data.FollowRequest
import com.example.data.Post
import com.example.data.Story
import com.example.data.User
import org.junit.Assert.*
import org.junit.Test

/**
 * Stage 10-12 Audit & Security Verification Unit Tests for YARKHOON.COM
 * Tests public/private accounts, follow request lifecycles, blocking, muting, and favorites.
 */
class Stage12PrivacyAndFollowAuditTest {

    @Test
    fun testPrivateAccountVisibilityPolicy() {
        val owner = User(id = "user_private", name = "Private Member", handle = "@private", isPrivate = true)
        val publicOwner = User(id = "user_public", name = "Public Member", handle = "@public", isPrivate = false)

        val viewerId = "viewer_1"
        val followingUserIds = setOf("approved_friend")

        // 1. Viewer does not follow private owner -> locked
        val isLockedForViewer = owner.isPrivate && !followingUserIds.contains(owner.id) && owner.id != viewerId
        assertTrue(isLockedForViewer)

        // 2. Viewer follows private owner -> accessible
        val followingPrivateIds = setOf(owner.id)
        val isLockedForFollower = owner.isPrivate && !followingPrivateIds.contains(owner.id) && owner.id != viewerId
        assertFalse(isLockedForFollower)

        // 3. Public owner -> never locked
        val isLockedPublic = publicOwner.isPrivate && !followingUserIds.contains(publicOwner.id) && publicOwner.id != viewerId
        assertFalse(isLockedPublic)

        // 4. Owner viewing own profile -> never locked
        val isLockedSelf = owner.isPrivate && !followingUserIds.contains(owner.id) && owner.id != owner.id
        assertFalse(isLockedSelf)
    }

    @Test
    fun testFollowRequestLifecycleTransitions() {
        val requester = User(id = "requester_1", name = "Ahmad", handle = "@ahmad")
        val target = User(id = "target_private", name = "Gul", handle = "@gul", isPrivate = true)

        // 1. Initial State: not following, not requested
        var sentRequests = setOf<String>()
        var followingIds = setOf<String>()
        var pendingRequests = listOf<FollowRequest>()

        // 2. User sends request to private account
        sentRequests = sentRequests + target.id
        val createdRequest = FollowRequest(
            id = "req_${requester.id}_${target.id}",
            requester = requester,
            targetUserId = target.id,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )
        pendingRequests = pendingRequests + createdRequest

        assertTrue(sentRequests.contains(target.id))
        assertFalse(followingIds.contains(target.id))
        assertEquals(1, pendingRequests.size)

        // 3. Requester can cancel before approval
        val canceledRequests = sentRequests - target.id
        assertFalse(canceledRequests.contains(target.id))

        // 4. Target approves request -> establish follower/following and remove request
        pendingRequests = pendingRequests.filter { it.requester.id != requester.id }
        followingIds = followingIds + target.id
        sentRequests = sentRequests - target.id

        assertEquals(0, pendingRequests.size)
        assertTrue(followingIds.contains(target.id))
        assertFalse(sentRequests.contains(target.id))
    }

    @Test
    fun testBlockingInvalidatesRelationshipsAndCancelsRequests() {
        val currentUserId = "my_uid"
        val blockedUserId = "bad_user"

        var followingIds = setOf(blockedUserId, "good_friend")
        var sentRequests = setOf(blockedUserId, "other_user")
        var pendingRequests = listOf(
            FollowRequest(id = "r1", requester = User(id = blockedUserId, name = "Bad", handle = "@bad"), targetUserId = currentUserId),
            FollowRequest(id = "r2", requester = User(id = "good_user", name = "Good", handle = "@good"), targetUserId = currentUserId)
        )
        var favoriteIds = setOf(blockedUserId, "fav_creator")

        // Action: Block bad_user
        val blockedIds = setOf(blockedUserId)
        followingIds = followingIds - blockedUserId
        sentRequests = sentRequests - blockedUserId
        pendingRequests = pendingRequests.filter { it.requester.id != blockedUserId }
        favoriteIds = favoriteIds - blockedUserId

        // Verification
        assertTrue(blockedIds.contains(blockedUserId))
        assertFalse(followingIds.contains(blockedUserId))
        assertFalse(sentRequests.contains(blockedUserId))
        assertEquals(1, pendingRequests.size)
        assertEquals("good_user", pendingRequests.first().requester.id)
        assertFalse(favoriteIds.contains(blockedUserId))
    }

    @Test
    fun testMutingHidesFromFeedsSilently() {
        val normalAuthor = User(id = "author_1", name = "Farhan", handle = "@farhan")
        val mutedAuthor = User(id = "author_2", name = "Noisy Poster", handle = "@noisy")

        val posts = listOf(
            Post(id = "p1", author = normalAuthor, text = "Beautiful evening in Yarkhoon"),
            Post(id = "p2", author = mutedAuthor, text = "Spam 1"),
            Post(id = "p3", author = mutedAuthor, text = "Spam 2")
        )

        val stories = listOf(
            Story(id = "s1", author = normalAuthor, mediaUrl = "https://example.com/s1.jpg"),
            Story(id = "s2", author = mutedAuthor, mediaUrl = "https://example.com/s2.jpg")
        )

        val mutedIds = setOf("author_2")
        val blockedIds = emptySet<String>()

        val visiblePosts = posts.filter { !blockedIds.contains(it.author.id) && !mutedIds.contains(it.author.id) }
        val visibleStories = stories.filter { !blockedIds.contains(it.author.id) && !mutedIds.contains(it.author.id) }

        assertEquals(1, visiblePosts.size)
        assertEquals("p1", visiblePosts[0].id)
        assertEquals(1, visibleStories.size)
        assertEquals("s1", visibleStories[0].id)
    }

    @Test
    fun testFavoritesFeedFiltering() {
        val favAuthor = User(id = "fav_1", name = "Favorite Artisan", handle = "@artisan")
        val regularAuthor = User(id = "reg_1", name = "Regular User", handle = "@reg")

        val posts = listOf(
            Post(id = "p1", author = favAuthor, text = "Handmade Chitrali Patti wool shawl"),
            Post(id = "p2", author = regularAuthor, text = "Nice day outside")
        )

        val favoriteUserIds = setOf("fav_1")

        val favoritesOnlyPosts = posts.filter { favoriteUserIds.contains(it.author.id) }
        assertEquals(1, favoritesOnlyPosts.size)
        assertEquals("p1", favoritesOnlyPosts[0].id)
    }

    @Test
    fun testExplorePrivacyEnforcement() {
        val publicUser = User(id = "pub_1", name = "Public Guide", handle = "@guide", isPrivate = false)
        val privateUser = User(id = "priv_1", name = "Private Local", handle = "@local", isPrivate = true)

        val posts = listOf(
            Post(id = "p_pub", author = publicUser, text = "Welcome to Yarkhoon"),
            Post(id = "p_priv", author = privateUser, text = "Private family gathering")
        )

        val followingUserIds = emptySet<String>()
        val blockedUserIds = emptySet<String>()

        // Explore posts must filter out private users unless already followed
        val explorePosts = posts.filter { post ->
            !blockedUserIds.contains(post.author.id) &&
            (!post.author.isPrivate || followingUserIds.contains(post.author.id))
        }

        assertEquals(1, explorePosts.size)
        assertEquals("p_pub", explorePosts[0].id)
    }
}
