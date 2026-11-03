package com.example.util

import com.example.data.Post
import kotlin.math.max

object HashtagUtils {

    // Regex matching hashtags containing alphanumeric characters, underscores, and Arabic/Urdu/Khowar script
    private val HASHTAG_REGEX = Regex("""#([a-zA-Z0-9_\u0600-\u06FF]+)""")

    /**
     * Extracts distinct hashtags from a text string.
     * Prevents duplicate entries caused by capitalization differences while preserving first seen casing.
     */
    fun extractHashtags(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val seenNormalized = mutableSetOf<String>()
        val result = mutableListOf<String>()

        HASHTAG_REGEX.findAll(text).forEach { matchResult ->
            val rawTag = matchResult.value
            val normalized = normalizeHashtag(rawTag)
            if (normalized.isNotBlank() && seenNormalized.add(normalized)) {
                result.add(formatDisplayHashtag(matchResult.groupValues[1]))
            }
        }

        return result
    }

    /**
     * Normalizes a hashtag into lowercase without the leading '#' symbol for consistent index & lookup.
     */
    fun normalizeHashtag(tag: String): String {
        return tag.trim()
            .removePrefix("#")
            .lowercase()
    }

    /**
     * Formats a tag for UI presentation with a '#' prefix.
     */
    fun formatDisplayHashtag(tag: String): String {
        val clean = tag.trim().removePrefix("#")
        return if (clean.isEmpty()) "" else "#$clean"
    }

    /**
     * Calculates a measurable trending score for a hashtag based on:
     * - Distinct post count
     * - Cumulative likes
     * - Cumulative comments
     * - Recency weighting: posts within the last 24-72 hours provide a strong momentum multiplier.
     */
    fun calculateTrendScore(
        postCount: Int,
        totalLikes: Int,
        totalComments: Int,
        newestTimestamp: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Double {
        if (postCount <= 0) return 0.0

        val ageHours = max(0.0, (currentTimeMs - newestTimestamp).toDouble() / (1000.0 * 60.0 * 60.0))
        // Half-life recency decay: activity in the last 24h has ~1.0 multiplier, decaying after 72h
        val recencyMultiplier = 1.0 / (1.0 + (ageHours / 24.0))

        val engagementPoints = (totalLikes * 2.0) + (totalComments * 3.5)
        val volumePoints = postCount * 15.0

        return (volumePoints + engagementPoints) * (0.4 + 0.6 * recencyMultiplier)
    }

    /**
     * Scores a discussion post by comment volume and conversation freshness.
     */
    fun calculateDiscussionScore(
        post: Post,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Double {
        if (post.commentsCount <= 0) return 0.0

        val ageHours = max(0.0, (currentTimeMs - post.timestamp).toDouble() / (1000.0 * 60.0 * 60.0))
        val recencyMultiplier = 1.0 / (1.0 + (ageHours / 36.0))

        val discussionVolume = (post.commentsCount * 4.0) + (post.likesCount * 1.5)
        return discussionVolume * recencyMultiplier
    }
}
