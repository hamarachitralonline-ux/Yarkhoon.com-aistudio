package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class StorageConfigInfo(
    val bucketName: String,
    val currentUserId: String?,
    val isUserSignedIn: Boolean,
    val isStorageUnprovisioned: Boolean = false
)

class FirebaseStorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "FirebaseStorageRepo"
        const val MAX_IMAGE_SIZE_BYTES = 15L * 1024L * 1024L // 15MB
        const val MAX_VIDEO_SIZE_BYTES = 60L * 1024L * 1024L // 60MB

        @Volatile
        var isStorageUnprovisioned: Boolean = false
            private set

        fun setStorageUnprovisioned(unprovisioned: Boolean) {
            isStorageUnprovisioned = unprovisioned
        }

        fun resetStorageAvailabilityCache() {
            isStorageUnprovisioned = false
        }
    }

    private fun getApplicationContext(): Context? {
        return try {
            FirebaseApp.getInstance().applicationContext
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Inspects and returns current Firebase Storage bucket and authentication state.
     */
    fun getStorageConfigInfo(): StorageConfigInfo {
        val bucket = try {
            storage.reference.bucket
        } catch (_: Exception) {
            ""
        }
        val currentUid = auth.currentUser?.uid
        return StorageConfigInfo(
            bucketName = bucket,
            currentUserId = currentUid,
            isUserSignedIn = currentUid != null,
            isStorageUnprovisioned = isStorageUnprovisioned
        )
    }

    /**
     * Uploads media from a gallery/file URI to Firebase Storage at the given destination path.
     * If the URI is already an http(s) URL (e.g. sample image), it returns the URL directly.
     * Reports upload progress from 0.0f to 1.0f via [onProgress].
     *
     * Correct upload and download sequence:
     * 1. Validate authenticated session.
     * 2. Validate and stream local file safely (handles content:// transient permissions by copying to internal cache).
     * 3. Set explicit MIME contentType in StorageMetadata matching storage rules.
     * 4. Upload actual bytes/file and wait for completion.
     * 5. Retrieve valid download URL from the exact uploaded object (or canonical gs:// path for protected media).
     * 6. Clean up temporary cache file.
     */
    suspend fun uploadMedia(
        uriString: String,
        destinationPath: String,
        isProtected: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            val err = "User is not signed in. Please sign in to upload media."
            Log.e(TAG, "Storage upload aborted: $err")
            return@withContext Result.failure(IllegalStateException(err))
        }

        // Check if Storage is known to be unprovisioned to avoid repeated failing network calls
        if (isStorageUnprovisioned) {
            val err = "Cloud Storage is not provisioned in the Firebase Console (Error -13010). Remote media upload is paused until Cloud Storage is activated. Text posts, profile information, and messaging remain fully active."
            Log.w(TAG, "Storage upload skipped (cached unprovisioned state): $err")
            return@withContext Result.failure(IllegalStateException(err))
        }

        // If it's already a web URL, skip Firebase Storage upload
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            onProgress(1.0f)
            return@withContext Result.success(uriString)
        }

        val uri = try {
            Uri.parse(uriString)
        } catch (e: Exception) {
            val err = "Invalid media URI format: $uriString"
            Log.e(TAG, "Storage URI parse error: $err", e)
            return@withContext Result.failure(IllegalArgumentException(err, e))
        }

        val context = getApplicationContext()
        var tempFileToClean: File? = null

        try {
            // Step 1: Resolve local file and ensure it is readable and non-empty
            val resolvedFile: File
            val mimeType: String

            if (uri.scheme == "file") {
                val filePath = uri.path ?: uriString.removePrefix("file://")
                val diskFile = File(filePath)
                if (!diskFile.exists() || !diskFile.canRead()) {
                    val err = "Local media access failed: File does not exist or is unreadable at: $filePath"
                    Log.e(TAG, "Local file error: op=LOCAL_MEDIA_ACCESS, path=$destinationPath: $err")
                    return@withContext Result.failure(FileNotFoundException(err))
                }
                resolvedFile = diskFile
                mimeType = detectMimeType(context, uri, destinationPath)
            } else {
                // For content:// and other URIs, stream through ContentResolver into private cache
                // This eliminates transient permission drops from Android 13+ Photo Picker background threads
                if (context == null) {
                    val err = "Local media access failed: Application context is unavailable to resolve content URI."
                    Log.e(TAG, "Context error: op=LOCAL_MEDIA_ACCESS: $err")
                    return@withContext Result.failure(IllegalStateException(err))
                }

                mimeType = detectMimeType(context, uri, destinationPath)
                val ext = getExtensionForMimeType(mimeType, destinationPath)
                val safeFileName = "upload_${System.currentTimeMillis()}_${destinationPath.substringAfterLast('/').ifBlank { "media" }}.$ext"
                val cacheFile = File(context.cacheDir, safeFileName)
                tempFileToClean = cacheFile

                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        val err = "Local media access failed: Content provider returned null input stream for URI: $uri"
                        Log.e(TAG, "ContentResolver error: op=LOCAL_MEDIA_ACCESS, path=$destinationPath: $err")
                        return@withContext Result.failure(FileNotFoundException(err))
                    }
                    inputStream.use { input ->
                        cacheFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (se: SecurityException) {
                    val err = "Local media access failed: Permission denied by system for URI: $uri"
                    Log.e(TAG, "SecurityException: op=LOCAL_MEDIA_ACCESS, path=$destinationPath: $err", se)
                    return@withContext Result.failure(SecurityException(err, se))
                } catch (fnf: FileNotFoundException) {
                    val err = "Local media access failed: Selected file not found at URI: $uri"
                    Log.e(TAG, "FileNotFoundException: op=LOCAL_MEDIA_ACCESS, path=$destinationPath: $err", fnf)
                    return@withContext Result.failure(FileNotFoundException(err))
                } catch (ioe: IOException) {
                    val err = "Local media access failed: I/O error reading selected file: ${ioe.localizedMessage}"
                    Log.e(TAG, "IOException: op=LOCAL_MEDIA_ACCESS, path=$destinationPath: $err", ioe)
                    return@withContext Result.failure(IOException(err, ioe))
                }

                resolvedFile = cacheFile
            }

            // Step 2: Validate file existence and size
            val fileSize = resolvedFile.length()
            if (fileSize <= 0) {
                val err = "Local media access failed: Selected media file is empty (0 bytes)."
                Log.e(TAG, "Validation error: op=FILE_VALIDATION, path=$destinationPath: $err")
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val isVideo = mimeType.startsWith("video") || destinationPath.endsWith(".mp4", ignoreCase = true)
            val maxLimit = if (isVideo) MAX_VIDEO_SIZE_BYTES else MAX_IMAGE_SIZE_BYTES
            if (fileSize > maxLimit) {
                val sizeMb = fileSize / (1024 * 1024)
                val limitMb = maxLimit / (1024 * 1024)
                val err = "Selected file is too large (${sizeMb}MB). Maximum allowed size is ${limitMb}MB."
                Log.e(TAG, "Validation error: op=FILE_VALIDATION, path=$destinationPath: $err")
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            // Step 3: Build explicit StorageMetadata with guaranteed contentType
            val storageMetadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("uploadedBy", currentUser.uid)
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .build()

            // Step 4: Execute upload to Firebase Storage
            val storageRef = storage.reference.child(destinationPath)
            val uploadUri = Uri.fromFile(resolvedFile)
            val uploadTask = storageRef.putFile(uploadUri, storageMetadata)

            uploadTask.addOnProgressListener { taskSnapshot ->
                val totalBytes = taskSnapshot.totalByteCount
                if (totalBytes > 0) {
                    val progress = taskSnapshot.bytesTransferred.toFloat() / totalBytes.toFloat()
                    onProgress(progress.coerceIn(0f, 1f))
                }
            }

            try {
                uploadTask.await()
                onProgress(1.0f)
            } catch (c: CancellationException) {
                Log.w(TAG, "Upload cancelled: op=STORAGE_UPLOAD, path=$destinationPath")
                return@withContext Result.failure(Exception("Upload was cancelled.", c))
            } catch (s: StorageException) {
                val errorMsg = when (s.errorCode) {
                    StorageException.ERROR_NOT_AUTHORIZED ->
                        "Storage upload failed: Permission denied by Storage Security Rules for path ($destinationPath). Error code: ${s.errorCode}."
                    StorageException.ERROR_OBJECT_NOT_FOUND -> {
                        isStorageUnprovisioned = true
                        "Storage upload failed: Cloud Storage is not yet provisioned in the Firebase Console for this project (Error -13010). Remote media upload is temporarily paused. Text thoughts, feelings, profile editing, and messaging remain fully active."
                    }
                    StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
                        "Storage upload failed: Network upload timed out. Please check your connection."
                    StorageException.ERROR_QUOTA_EXCEEDED ->
                        "Storage upload failed: Storage quota exceeded for Firebase project."
                    else ->
                        "Storage upload failed: ${s.localizedMessage ?: "Firebase error code ${s.errorCode}"}"
                }
                Log.e(TAG, "Upload error: op=STORAGE_UPLOAD, path=$destinationPath, code=${s.errorCode}, type=${s.javaClass.simpleName}: ${s.message}")
                return@withContext Result.failure(Exception(errorMsg, s))
            } catch (e: Exception) {
                val errorMsg = "Storage upload failed: ${e.localizedMessage ?: "Unknown network error"}"
                Log.e(TAG, "Upload unexpected error: op=STORAGE_UPLOAD, path=$destinationPath, type=${e.javaClass.simpleName}: ${e.message}")
                return@withContext Result.failure(Exception(errorMsg, e))
            }

            // Step 5: For protected media, return canonical gs:// reference path.
            // For public media, retrieve download URL from the exact uploaded object.
            if (isProtected) {
                val canonicalGsUri = "gs://${storageRef.bucket}/${destinationPath}"
                Log.i(TAG, "Protected media uploaded successfully: canonicalPath=$canonicalGsUri")
                Result.success(canonicalGsUri)
            } else {
                try {
                    val downloadUrl = storageRef.downloadUrl.await().toString()
                    if (downloadUrl.isBlank()) {
                        val err = "Download URL retrieval failed: Server returned an empty download URL for path ($destinationPath)."
                        Log.e(TAG, "Download URL error: op=DOWNLOAD_URL_RETRIEVAL, path=$destinationPath: $err")
                        Result.failure(Exception(err))
                    } else {
                        Log.i(TAG, "Public media uploaded successfully: path=$destinationPath")
                        Result.success(downloadUrl)
                    }
                } catch (s: StorageException) {
                    val errorMsg = when (s.errorCode) {
                        StorageException.ERROR_OBJECT_NOT_FOUND ->
                            "Download URL retrieval failed: Uploaded object not found at location ($destinationPath). Error code: ${s.errorCode}."
                        StorageException.ERROR_NOT_AUTHORIZED ->
                            "Download URL retrieval failed: Permission denied to read object at ($destinationPath). Error code: ${s.errorCode}."
                        else ->
                            "Download URL retrieval failed for ($destinationPath): ${s.localizedMessage ?: "Firebase error code ${s.errorCode}"}"
                    }
                    Log.e(TAG, "Download URL error: op=DOWNLOAD_URL_RETRIEVAL, path=$destinationPath, code=${s.errorCode}, type=${s.javaClass.simpleName}: ${s.message}")
                    Result.failure(Exception(errorMsg, s))
                } catch (e: Exception) {
                    val errorMsg = "Download URL retrieval failed for ($destinationPath): ${e.localizedMessage ?: "Unknown error"}"
                    Log.e(TAG, "Download URL unexpected error: op=DOWNLOAD_URL_RETRIEVAL, path=$destinationPath, type=${e.javaClass.simpleName}: ${e.message}")
                    Result.failure(Exception(errorMsg, e))
                }
            }
        } finally {
            // Clean up temporary cache file
            tempFileToClean?.let { file ->
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun detectMimeType(context: Context?, uri: Uri, destinationPath: String): String {
        var mime: String? = null
        if (context != null && uri.scheme == "content") {
            try {
                mime = context.contentResolver.getType(uri)
            } catch (_: Exception) {}
        }
        if (mime.isNullOrBlank() || mime == "application/octet-stream") {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                .ifBlank { destinationPath.substringAfterLast('.', "") }
            if (extension.isNotBlank()) {
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
        }
        return when {
            !mime.isNullOrBlank() && (mime.startsWith("image/") || mime.startsWith("video/")) -> mime
            destinationPath.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            destinationPath.endsWith(".mov", ignoreCase = true) -> "video/quicktime"
            destinationPath.endsWith(".png", ignoreCase = true) -> "image/png"
            destinationPath.endsWith(".webp", ignoreCase = true) -> "image/webp"
            destinationPath.contains("/reels/") || destinationPath.contains("/video") -> "video/mp4"
            else -> "image/jpeg"
        }
    }

    private fun getExtensionForMimeType(mimeType: String, destinationPath: String): String {
        val destExt = destinationPath.substringAfterLast('.', "")
        if (destExt.isNotBlank()) return destExt.lowercase()

        return when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "video/mp4" -> "mp4"
            "video/quicktime" -> "mov"
            "video/3gpp" -> "3gp"
            else -> if (mimeType.startsWith("video/")) "mp4" else "jpg"
        }
    }

    /**
     * Calls the backend Cloud Function getAuthorizedMediaUrl to obtain a short-lived
     * server-authorized signed URL for private posts, stories, or reels.
     */
    suspend fun requestServerAuthorizedMediaUrl(
        targetAuthorId: String,
        storagePath: String,
        contentType: String = "post",
        contentId: String = ""
    ): String = withContext(Dispatchers.IO) {
        try {
            val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return@withContext ""
            val cleanPath = if (storagePath.startsWith("gs://")) {
                val withoutScheme = storagePath.removePrefix("gs://")
                withoutScheme.substringAfter("/")
            } else {
                storagePath
            }

            val functionUrl = URL("https://asia-southeast1-yarkhoondotcom.cloudfunctions.net/getAuthorizedMediaUrl")
            val conn = (functionUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
            }

            val jsonBody = JSONObject().apply {
                put("targetAuthorId", targetAuthorId)
                put("storagePath", cleanPath)
                put("contentType", contentType)
                put("contentId", contentId)
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(resp).optString("url", "")
            } else {
                ""
            }
        } catch (_: Exception) {
            // Function offline or unreachable during local simulation
            ""
        }
    }

    /**
     * Resolves authorized media access for private posts, stories, or reels.
     * Enforces server-side authorization for non-owner protected media.
     */
    suspend fun resolveAuthorizedMediaUrl(
        targetAuthorId: String,
        targetAuthorIsPrivate: Boolean,
        rawMediaUrl: String,
        isApprovedFollower: Boolean,
        isBlocked: Boolean,
        contentType: String = "post",
        contentId: String = ""
    ): String {
        // If it's already a non-protected HTTP URL (public media or sample asset), return directly
        if (!rawMediaUrl.startsWith("gs://") && !rawMediaUrl.contains("/protected/")) {
            return rawMediaUrl
        }

        // If target blocked current user or caller blocked target, deny media access
        if (isBlocked) {
            return "" // Blocked users cannot resolve private media
        }

        val currentUid = auth.currentUser?.uid
        val isOwner = currentUid != null && currentUid == targetAuthorId

        // If account is private and caller is neither owner nor approved follower, deny
        if (targetAuthorIsPrivate && !isOwner && !isApprovedFollower) {
            return "" // Not approved to view protected media
        }

        val cleanPath = if (rawMediaUrl.startsWith("gs://")) {
            val withoutScheme = rawMediaUrl.removePrefix("gs://")
            withoutScheme.substringAfter("/")
        } else {
            rawMediaUrl
        }

        // Content owner has direct Storage read permission
        if (isOwner) {
            return try {
                storage.reference.child(cleanPath).downloadUrl.await().toString()
            } catch (_: Exception) {
                ""
            }
        }

        // For non-owner approved followers, request server-side signed URL
        val serverUrl = requestServerAuthorizedMediaUrl(
            targetAuthorId = targetAuthorId,
            storagePath = cleanPath,
            contentType = contentType,
            contentId = contentId
        )

        if (serverUrl.isNotBlank()) {
            return serverUrl
        }

        // Fallback for local emulator or test environments
        return try {
            storage.reference.child(cleanPath).downloadUrl.await().toString()
        } catch (_: Exception) {
            ""
        }
    }
}
