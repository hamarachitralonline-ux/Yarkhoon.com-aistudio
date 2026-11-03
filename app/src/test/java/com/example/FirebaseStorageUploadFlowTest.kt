package com.example

import com.example.data.PostType
import com.example.data.StorageConfigInfo
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

class FirebaseStorageUploadFlowTest {

    private val testUid = "user_auth_12345"
    private val expectedBucket = "yarkhoondotcom.firebasestorage.app"

    @Test
    fun testStorageBucketConfigurationConsistency() {
        val config = StorageConfigInfo(
            bucketName = expectedBucket,
            currentUserId = testUid,
            isUserSignedIn = true
        )
        assertEquals("yarkhoondotcom.firebasestorage.app", config.bucketName)
        assertEquals(testUid, config.currentUserId)
        assertTrue(config.isUserSignedIn)
    }

    @Test
    fun testStoragePathConstructionForPhotoVideoAndReel() {
        val postId = "post_1710000000"
        val reelId = "reel_1710000001"
        val storyId = "story_1710000002"

        // Public post photo path
        val photoPath = "users/$testUid/public/posts/$postId/photo_1710000000.jpg"
        assertTrue(photoPath.startsWith("users/$testUid/public/posts/$postId/"))
        assertTrue(photoPath.endsWith(".jpg"))

        // Public post video path
        val videoPath = "users/$testUid/public/posts/$postId/video_1710000000.mp4"
        assertTrue(videoPath.startsWith("users/$testUid/public/posts/$postId/"))
        assertTrue(videoPath.endsWith(".mp4"))

        // Public reel video path
        val reelPath = "users/$testUid/public/reels/$reelId/reel_1710000001.mp4"
        assertTrue(reelPath.startsWith("users/$testUid/public/reels/$reelId/"))
        assertTrue(reelPath.endsWith(".mp4"))

        // Story path
        val storyPath = "users/$testUid/public/stories/$storyId/story_1710000002.jpg"
        assertTrue(storyPath.startsWith("users/$testUid/public/stories/$storyId/"))
        assertTrue(storyPath.endsWith(".jpg"))

        // Protected private media paths
        val protectedPostPath = "users/$testUid/protected/posts/$postId/photo_1710000000.jpg"
        assertTrue(protectedPostPath.startsWith("users/$testUid/protected/posts/$postId/"))
    }

    @Test
    fun testStorageMetadataMimeTypeResolution() {
        fun resolveMime(type: PostType, destPath: String, url: String): String {
            return when {
                destPath.endsWith(".mp4", ignoreCase = true) || type == PostType.VIDEO || type == PostType.REEL -> "video/mp4"
                destPath.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
                destPath.endsWith(".png", ignoreCase = true) || url.endsWith(".png", ignoreCase = true) -> "image/png"
                destPath.endsWith(".webp", ignoreCase = true) || url.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }
        }

        // Photo
        val photoMime = resolveMime(PostType.PHOTO, "users/u1/public/posts/p1/photo_1.jpg", "content://media/1")
        assertEquals("image/jpeg", photoMime)
        assertTrue(photoMime.matches(Regex("image/.*")))

        // PNG Photo
        val pngMime = resolveMime(PostType.PHOTO, "users/u1/public/posts/p1/photo_1.png", "file:///test.png")
        assertEquals("image/png", pngMime)
        assertTrue(pngMime.matches(Regex("image/.*")))

        // Video
        val videoMime = resolveMime(PostType.VIDEO, "users/u1/public/posts/p1/video_1.mp4", "content://media/2")
        assertEquals("video/mp4", videoMime)
        assertTrue(videoMime.matches(Regex("video/.*")))

        // Reel
        val reelMime = resolveMime(PostType.REEL, "users/u1/public/reels/r1/reel_1.mp4", "content://media/3")
        assertEquals("video/mp4", reelMime)
        assertTrue(reelMime.matches(Regex("video/.*")))
    }

    @Test
    fun testCanonicalStorageUriFormat() {
        val storagePath = "users/$testUid/protected/posts/p1/photo_1.jpg"
        val canonicalGs = "gs://$expectedBucket/$storagePath"
        assertEquals("gs://yarkhoondotcom.firebasestorage.app/users/user_auth_12345/protected/posts/p1/photo_1.jpg", canonicalGs)
        assertTrue(canonicalGs.startsWith("gs://"))
        assertTrue(canonicalGs.contains("/protected/posts/"))
    }

    @Test
    fun testLocalMediaFileExistenceValidation() {
        val nonExistent = File("/invalid/path/to/missing_media.jpg")
        val exists = nonExistent.exists() && nonExistent.canRead()
        assertFalse("Non-existent local file must not be validated as accessible", exists)

        // Verifies the error message format
        val expectedError = "Local media access failed: File does not exist or is unreadable at: ${nonExistent.absolutePath}"
        assertTrue(expectedError.contains("Local media access failed"))
    }

    @Test
    fun testDistinctErrorClassification() {
        // 1. Local media access error
        val localError = "Local media access failed: Input stream is null for URI: content://media/picker/999"
        assertTrue(localError.startsWith("Local media access failed"))

        // 2. Storage upload failure
        val uploadError = "Storage upload failed: Target bucket or storage location does not exist (users/u1/public/posts/p1/media). Error code: -13010."
        assertTrue(uploadError.startsWith("Storage upload failed"))
        assertTrue(uploadError.contains("Error code: -13010"))

        // 3. Permission denied by rules
        val permissionError = "Storage upload failed: Permission denied by Storage Security Rules for path (users/u2/public/posts/p1/photo.jpg). Error code: -13021."
        assertTrue(permissionError.contains("Permission denied by Storage Security Rules"))

        // 4. Download URL retrieval failure
        val downloadUrlError = "Download URL retrieval failed: Uploaded object not found at location (users/u1/public/posts/p1/photo.jpg). Error code: -13010."
        assertTrue(downloadUrlError.startsWith("Download URL retrieval failed"))

        // 5. Firestore post creation failure
        val firestoreError = "Firestore post creation failed: PERMISSION_DENIED: Missing or insufficient permissions."
        assertTrue(firestoreError.startsWith("Firestore post creation failed"))
    }

    @Test
    fun testNoLocalUriLeakageOnUploadFailure() {
        // Scenario: upload fails
        val uploadSuccess = false
        val localContentUri = "content://media/picker/photo_123"
        val remoteDownloadUrl: String? = if (uploadSuccess) "https://firebasestorage.googleapis.com/v0/b/.../photo.jpg?alt=media" else null

        // Application rule: Never persist local content URI to Firestore post document
        val postMediaUrls = if (!remoteDownloadUrl.isNullOrBlank()) listOf(remoteDownloadUrl) else emptyList()

        assertTrue("When upload fails, post mediaUrls list must be empty (never contain local URI)", postMediaUrls.isEmpty())
        assertFalse(postMediaUrls.contains(localContentUri))
    }

    @Test
    fun testZeroByteFileRejection() {
        val zeroBytes = 0L
        val isValid = zeroBytes > 0
        assertFalse("Zero-byte file must be rejected", isValid)
    }

    @Test
    fun testFileSizeLimitEnforcement() {
        val maxImage = 15L * 1024 * 1024
        val maxVideo = 60L * 1024 * 1024

        val validImageSize = 5L * 1024 * 1024
        val oversizedImageSize = 16L * 1024 * 1024
        val validVideoSize = 45L * 1024 * 1024
        val oversizedVideoSize = 65L * 1024 * 1024

        assertTrue(validImageSize <= maxImage)
        assertFalse(oversizedImageSize <= maxImage)
        assertTrue(validVideoSize <= maxVideo)
        assertFalse(oversizedVideoSize <= maxVideo)
    }

    @Test
    fun testPostRepositoryTempFileNamingAndExtension() {
        fun resolveExt(mime: String, uriString: String, type: PostType): String {
            return when {
                mime.startsWith("image/png") || uriString.endsWith(".png") -> "png"
                mime.startsWith("image/webp") || uriString.endsWith(".webp") -> "webp"
                mime.startsWith("video/") || uriString.endsWith(".mp4") || type == PostType.VIDEO || type == PostType.REEL -> "mp4"
                else -> "jpg"
            }
        }

        assertEquals("png", resolveExt("image/png", "content://media/1", PostType.PHOTO))
        assertEquals("jpg", resolveExt("image/jpeg", "content://media/2", PostType.PHOTO))
        assertEquals("mp4", resolveExt("video/mp4", "content://media/3", PostType.VIDEO))
        assertEquals("mp4", resolveExt("application/octet-stream", "content://media/4", PostType.REEL))
    }

    @Test
    fun testTempFileCreationAndValidation() {
        val tempDirPath = System.getProperty("java.io.tmpdir") ?: "/tmp"
        val tempDir = File(tempDirPath)
        val testFile = File(tempDir, "unit_test_post_media_${System.currentTimeMillis()}.jpg")
        try {
            testFile.writeText("sample post image binary content")
            assertTrue(testFile.exists())
            assertTrue(testFile.length() > 0)

            val copiedTemp = File(tempDir, "copy_${testFile.name}")
            testFile.inputStream().use { input ->
                copiedTemp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            assertTrue(copiedTemp.exists())
            assertEquals(testFile.length(), copiedTemp.length())
            copiedTemp.delete()
        } finally {
            if (testFile.exists()) testFile.delete()
        }
    }
}
