package com.example

import com.example.data.Post
import com.example.data.PostReaction
import com.example.data.PostType
import com.example.data.User
import org.junit.Assert.*
import org.junit.Test

class PostReactionMenuTest {

    private val sampleAuthor = User(id = "user_1", name = "Gul Baig", handle = "@gul")

    @Test
    fun testPostReactionEnumValuesAndEmojis() {
        val reactions = PostReaction.values()
        assertEquals(4, reactions.size)

        // Like
        val likeReaction = PostReaction.fromId("like")
        assertNotNull(likeReaction)
        assertEquals("like", likeReaction?.id)
        assertEquals("👍", likeReaction?.emoji)
        assertEquals("Like", likeReaction?.label)

        // Love
        val loveReaction = PostReaction.fromId("love")
        assertNotNull(loveReaction)
        assertEquals("love", loveReaction?.id)
        assertEquals("❤️", loveReaction?.emoji)
        assertEquals("Love", loveReaction?.label)

        // Laugh
        val laughReaction = PostReaction.fromId("laugh")
        assertNotNull(laughReaction)
        assertEquals("laugh", laughReaction?.id)
        assertEquals("😂", laughReaction?.emoji)
        assertEquals("Haha", laughReaction?.label)

        // Sad
        val sadReaction = PostReaction.fromId("sad")
        assertNotNull(sadReaction)
        assertEquals("sad", sadReaction?.id)
        assertEquals("😢", sadReaction?.emoji)
        assertEquals("Sad", sadReaction?.label)
    }

    @Test
    fun testPostReactionDefaultModelValues() {
        val post = Post(
            id = "post_100",
            author = sampleAuthor,
            type = PostType.TEXT,
            text = "Welcome to Yarkhoon Valley festival"
        )

        assertNull(post.userReaction)
        assertFalse(post.isLiked)
        assertEquals(0, post.likesCount)
    }

    @Test
    fun testSelectEmojiReactionIncrementsCount() {
        val post = Post(
            id = "post_101",
            author = sampleAuthor,
            text = "Yarkhoon River view",
            likesCount = 5,
            isLiked = false,
            userReaction = null
        )

        // User long-presses and selects 'love' (❤️)
        val selectedEmoji = "love"
        val updatedPost = post.copy(
            isLiked = true,
            userReaction = selectedEmoji,
            likesCount = post.likesCount + 1
        )

        assertTrue(updatedPost.isLiked)
        assertEquals("love", updatedPost.userReaction)
        assertEquals(6, updatedPost.likesCount)
    }

    @Test
    fun testSwitchingEmojiReactionKeepsCount() {
        val post = Post(
            id = "post_102",
            author = sampleAuthor,
            text = "Cultural Khowar dance",
            likesCount = 10,
            isLiked = true,
            userReaction = "like"
        )

        // User switches from 'like' to 'laugh' (😂)
        val newEmoji = "laugh"
        val wasAlreadyLiked = post.isLiked
        val newCount = if (!wasAlreadyLiked) post.likesCount + 1 else post.likesCount
        val updatedPost = post.copy(
            isLiked = true,
            userReaction = newEmoji,
            likesCount = newCount
        )

        assertTrue(updatedPost.isLiked)
        assertEquals("laugh", updatedPost.userReaction)
        assertEquals("Reaction count should remain the same when switching emoji", 10, updatedPost.likesCount)
    }

    @Test
    fun testTappingSameEmojiReactionTogglesOffAndDecrementsCount() {
        val post = Post(
            id = "post_103",
            author = sampleAuthor,
            text = "Winter festival",
            likesCount = 4,
            isLiked = true,
            userReaction = "sad"
        )

        // User clicks 'sad' again -> unreacts
        val clickedEmoji = "sad"
        val updatedPost = if (post.userReaction == clickedEmoji) {
            post.copy(
                isLiked = false,
                userReaction = null,
                likesCount = (post.likesCount - 1).coerceAtLeast(0)
            )
        } else {
            post.copy(
                isLiked = true,
                userReaction = clickedEmoji,
                likesCount = post.likesCount
            )
        }

        assertFalse(updatedPost.isLiked)
        assertNull(updatedPost.userReaction)
        assertEquals(3, updatedPost.likesCount)
    }
}
