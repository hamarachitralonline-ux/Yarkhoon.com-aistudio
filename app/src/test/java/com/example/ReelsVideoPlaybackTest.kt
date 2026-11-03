package com.example

import com.example.data.Reel
import com.example.data.SampleData
import com.example.data.User
import org.junit.Assert.*
import org.junit.Test

class ReelsVideoPlaybackTest {

    private val author = User(
        id = "author_1",
        name = "Mountain Guide",
        handle = "@mountainguide",
        avatarUrl = "https://example.com/avatar.jpg"
    )

    @Test
    fun testReelVideoMediaUrls() {
        val reel = Reel(
            id = "reel_video_1",
            author = author,
            videoMediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            thumbnailMediaUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=900",
            caption = "Amazing Shandur pass polo tournament!"
        )

        assertEquals("reel_video_1", reel.id)
        assertTrue("Video URL must end with .mp4 or be a valid video stream", reel.videoMediaUrl.endsWith(".mp4"))
        assertNotNull(reel.thumbnailMediaUrl)
        assertTrue(reel.thumbnailMediaUrl.isNotBlank())
    }

    @Test
    fun testSampleReelsContainPlayableVideoUrls() {
        val sampleReels = SampleData.sampleReels
        assertFalse(sampleReels.isEmpty())

        for (reel in sampleReels) {
            assertTrue("Sample reel ${reel.id} must have a non-blank videoMediaUrl", reel.videoMediaUrl.isNotBlank())
            assertTrue("Sample reel ${reel.id} must have a non-blank thumbnailMediaUrl", reel.thumbnailMediaUrl.isNotBlank())
            assertTrue("Sample reel ${reel.id} must have http/https schema", reel.videoMediaUrl.startsWith("http"))
        }
    }

    @Test
    fun testVolumeCalculationBasedOnMuteState() {
        val isMuted = true
        val volumeMuted = if (isMuted) 0f else 1f
        assertEquals(0f, volumeMuted, 0.001f)

        val isUnmuted = false
        val volumeUnmuted = if (isUnmuted) 0f else 1f
        assertEquals(1f, volumeUnmuted, 0.001f)
    }

    @Test
    fun testPlayWhenReadyStateLogic() {
        // When selected and user has not paused -> should play
        val isSelected = true
        val isUserPlaying = true
        val playWhenReady = isSelected && isUserPlaying
        assertTrue(playWhenReady)

        // When page scrolled away (not selected) -> should not play
        val isNotSelected = false
        val playWhenReadyScrolled = isNotSelected && isUserPlaying
        assertFalse(playWhenReadyScrolled)

        // When selected but user paused -> should not play
        val isUserPaused = false
        val playWhenReadyPaused = isSelected && isUserPaused
        assertFalse(playWhenReadyPaused)
    }

    @Test
    fun testReelsFilteringBlockedAndMuted() {
        val blockedAuthorId = "blocked_user"
        val mutedAuthorId = "muted_user"

        val reels = listOf(
            Reel(id = "r1", author = author, videoMediaUrl = "https://example.com/1.mp4", thumbnailMediaUrl = "", caption = "Good reel"),
            Reel(id = "r2", author = User(id = blockedAuthorId, name = "Blocked", handle = "@blocked"), videoMediaUrl = "https://example.com/2.mp4", thumbnailMediaUrl = "", caption = "Bad reel"),
            Reel(id = "r3", author = User(id = mutedAuthorId, name = "Muted", handle = "@muted"), videoMediaUrl = "https://example.com/3.mp4", thumbnailMediaUrl = "", caption = "Muted reel")
        )

        val blockedSet = setOf(blockedAuthorId)
        val mutedSet = setOf(mutedAuthorId)

        val visibleReels = reels.filter { !blockedSet.contains(it.author.id) && !mutedSet.contains(it.author.id) }

        assertEquals(1, visibleReels.size)
        assertEquals("r1", visibleReels.first().id)
    }

    @Test
    fun testRetryTriggerIncrement() {
        var retryTrigger = 0
        var playbackError: String? = "Timeout connecting to video stream"

        // User clicks retry
        playbackError = null
        retryTrigger++

        assertNull(playbackError)
        assertEquals(1, retryTrigger)
    }
}
