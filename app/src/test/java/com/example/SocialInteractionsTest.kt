package com.example

import com.example.data.Comment
import com.example.data.Post
import com.example.data.PostType
import com.example.data.User
import com.example.util.MentionUtils
import org.junit.Assert.*
import org.junit.Test

class SocialInteractionsTest {

    @Test
    fun testMentionExtraction() {
        val text = "Salam @ali_chitrali and @gul_yarkhooni! Check out this update @ali_chitrali."
        val mentions = MentionUtils.extractMentions(text)

        assertEquals(2, mentions.size)
        assertTrue(mentions.contains("ali_chitrali"))
        assertTrue(mentions.contains("gul_yarkhooni"))
    }

    @Test
    fun testContainsMention() {
        val text = "Great photography @sarah_traveler!"
        assertTrue(MentionUtils.containsMention(text, "sarah_traveler"))
        assertTrue(MentionUtils.containsMention(text, "@sarah_traveler"))
        assertFalse(MentionUtils.containsMention(text, "unknown_user"))
    }

    @Test
    fun testCommentRepliesHierarchy() {
        val user1 = User(id = "u1", name = "User One", handle = "@user1", avatarUrl = "")
        val user2 = User(id = "u2", name = "User Two", handle = "@user2", avatarUrl = "")

        val parentComment = Comment(
            id = "c_parent",
            author = user1,
            text = "Is the road open to Yarkhoon?",
            timestamp = "10m ago"
        )

        val reply1 = Comment(
            id = "c_reply1",
            author = user2,
            text = "Yes, fully cleared this morning!",
            timestamp = "5m ago",
            parentId = "c_parent",
            replyToUsername = "@user1"
        )

        val reply2 = Comment(
            id = "c_reply2",
            author = user1,
            text = "Thank you brother!",
            timestamp = "Just now",
            parentId = "c_parent",
            replyToUsername = "@user2"
        )

        val replies = listOf(reply1, reply2)
        val fullComment = parentComment.copy(replies = replies, repliesCount = replies.size)

        assertEquals(2, fullComment.replies.size)
        assertEquals("c_parent", fullComment.replies[0].parentId)
        assertEquals("@user1", fullComment.replies[0].replyToUsername)
    }

    @Test
    fun testFeedFilterFollowingLogic() {
        val author1 = User(id = "auth_1", name = "Gul", handle = "@gul", avatarUrl = "")
        val author2 = User(id = "auth_2", name = "Ali", handle = "@ali", avatarUrl = "")
        val author3 = User(id = "auth_3", name = "Rehman", handle = "@rehman", avatarUrl = "")

        val posts = listOf(
            Post(id = "p1", author = author1, text = "Post 1"),
            Post(id = "p2", author = author2, text = "Post 2"),
            Post(id = "p3", author = author3, text = "Post 3")
        )

        val followingUserIds = setOf("auth_1", "auth_3")

        val followingPosts = posts.filter { followingUserIds.contains(it.author.id) }
        assertEquals(2, followingPosts.size)
        assertTrue(followingPosts.any { it.id == "p1" })
        assertTrue(followingPosts.any { it.id == "p3" })
        assertFalse(followingPosts.any { it.id == "p2" })
    }

    @Test
    fun testRepostQuotePreviewEmbedding() {
        val originalAuthor = User(id = "oa", name = "Original", handle = "@original", avatarUrl = "")
        val originalPost = Post(id = "orig_1", author = originalAuthor, text = "Shandur Festival dates announced!")

        val reposter = User(id = "rep", name = "Reposter", handle = "@reposter", avatarUrl = "")
        val repost = Post(
            id = "repost_1",
            author = reposter,
            text = "Who is going with us?",
            originalPost = originalPost,
            repostThoughts = "Who is going with us?",
            isReposted = true
        )

        assertNotNull(repost.originalPost)
        assertEquals("orig_1", repost.originalPost?.id)
        assertEquals("Shandur Festival dates announced!", repost.originalPost?.text)
        assertTrue(repost.isReposted)
    }
}
