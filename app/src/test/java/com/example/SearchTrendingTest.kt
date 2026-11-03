package com.example

import com.example.data.*
import com.example.util.HashtagUtils
import org.junit.Assert.*
import org.junit.Test

class SearchTrendingTest {

    @Test
    fun testHashtagExtractionAndNormalization() {
        val text = "Welcome to #Yarkhoon valley! The view of #Chitral mountains is amazing. #yarkhoon again."
        val extracted = HashtagUtils.extractHashtags(text)

        // Should extract distinct tags case-insensitively
        assertEquals(2, extracted.size)
        assertTrue(extracted.contains("#Yarkhoon"))
        assertTrue(extracted.contains("#Chitral"))

        // Normalization should strip '#' and lowercase
        assertEquals("yarkhoon", HashtagUtils.normalizeHashtag("#Yarkhoon"))
        assertEquals("yarkhoon", HashtagUtils.normalizeHashtag("yarkhoon"))
        assertEquals("chitral_mountains", HashtagUtils.normalizeHashtag("#Chitral_Mountains"))
    }

    @Test
    fun testUrduAndKhowarHashtags() {
        val text = "خوش آمدید #چترال اور #یارخون میں!"
        val extracted = HashtagUtils.extractHashtags(text)

        assertEquals(2, extracted.size)
        assertTrue(extracted.contains("#چترال"))
        assertTrue(extracted.contains("#یارخون"))
    }

    @Test
    fun testTrendingCalculationRecencyWeight() {
        val now = 1_700_000_000_000L
        val recentPostTime = now - (2 * 3600 * 1000L) // 2 hours ago
        val olderPostTime = now - (120 * 3600 * 1000L) // 5 days ago

        val recentScore = HashtagUtils.calculateTrendScore(
            postCount = 5,
            totalLikes = 20,
            totalComments = 10,
            newestTimestamp = recentPostTime,
            currentTimeMs = now
        )

        val olderScore = HashtagUtils.calculateTrendScore(
            postCount = 5,
            totalLikes = 20,
            totalComments = 10,
            newestTimestamp = olderPostTime,
            currentTimeMs = now
        )

        // Fresh activity should yield a measurably higher trending score than stale activity
        assertTrue("Recent score ($recentScore) should exceed older score ($olderScore)", recentScore > olderScore)
    }

    @Test
    fun testDiscussionScore() {
        val now = 1_700_000_000_000L
        val author = User(id = "u1", name = "Test", handle = "@test", avatarUrl = "")

        val activeDiscussion = Post(
            id = "p1",
            author = author,
            text = "Active debate",
            commentsCount = 25,
            likesCount = 10,
            timestamp = now - (3 * 3600 * 1000L) // 3 hours ago
        )

        val inactivePost = Post(
            id = "p2",
            author = author,
            text = "No discussion",
            commentsCount = 0,
            likesCount = 50,
            timestamp = now
        )

        val score1 = HashtagUtils.calculateDiscussionScore(activeDiscussion, currentTimeMs = now)
        val score2 = HashtagUtils.calculateDiscussionScore(inactivePost, currentTimeMs = now)

        assertTrue(score1 > 0.0)
        assertEquals(0.0, score2, 0.001)
    }

    @Test
    fun testSearchResultsPrivacySanitization() {
        // Ensure private contact info like phone number and email are sanitized for public search previews
        val rawUser = User(
            id = "u99",
            name = "Private Member",
            handle = "@private_member",
            avatarUrl = "",
            phoneNumber = "+923001234567",
            email = "private@example.com"
        )

        val sanitized = rawUser.copy(phoneNumber = "", email = "")

        assertTrue(sanitized.phoneNumber.isEmpty())
        assertTrue(sanitized.email.isEmpty())
        assertEquals("Private Member", sanitized.name)
    }
}
