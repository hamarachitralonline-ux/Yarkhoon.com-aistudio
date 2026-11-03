package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileNotFoundException

class FirestorePostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val notificationRepository: FirestoreNotificationRepository = FirestoreNotificationRepository(firestore, auth),
    private val storageRepository: FirebaseStorageRepository = FirebaseStorageRepository()
) {
    companion object {
        const val POSTS_COLLECTION = "posts"
        const val COMMENTS_SUBCOLLECTION = "comments"
        private const val TAG = "FirestorePostRepo"

        /**
         * Safely parses legacy timestamps across various schema iterations:
         * - Numbers (Long / Double / Int epoch millis)
         * - com.google.firebase.Timestamp objects
         * - java.util.Date objects
         * - Map representations (e.g. {"_seconds": 1718000000, "_nanoseconds": 0} or {"seconds": ...})
         * - String numeric epoch millis
         * - ISO-8601 date strings ("2024-05-10T14:30:00Z", "2024-05-10T14:30:00.000Z", etc.)
         * - Common date formats ("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")
         *
         * If the timestamp field is completely missing or unrecognizable, returns 0L instead of
         * silently injecting the current time. This preserves valid historical dates, ensures
         * posts with missing timestamps do not displace new posts at the top of the feed,
         * and keeps feed sorting deterministic.
         */
        fun parsePostTimestamp(raw: Any?): Long {
            if (raw == null) return 0L
            return when (raw) {
                is Number -> raw.toLong()
                is com.google.firebase.Timestamp -> raw.toDate().time
                is java.util.Date -> raw.time
                is Map<*, *> -> {
                    val sec = (raw["seconds"] as? Number)?.toLong()
                        ?: (raw["_seconds"] as? Number)?.toLong()
                    if (sec != null) sec * 1000L else 0L
                }
                is String -> {
                    val trimmed = raw.trim()
                    if (trimmed.isBlank()) return 0L
                    trimmed.toLongOrNull() ?: run {
                        try {
                            java.time.Instant.parse(trimmed).toEpochMilli()
                        } catch (_: Exception) {
                            try {
                                java.time.OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
                            } catch (_: Exception) {
                                val patterns = arrayOf(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                    "yyyy-MM-dd'T'HH:mm:ss",
                                    "yyyy-MM-dd HH:mm:ss",
                                    "yyyy-MM-dd"
                                )
                                var matched = 0L
                                for (pattern in patterns) {
                                    try {
                                        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                        val parsed = sdf.parse(trimmed)
                                        if (parsed != null) {
                                            matched = parsed.time
                                            break
                                        }
                                    } catch (_: Exception) {}
                                }
                                matched
                            }
                        }
                    }
                }
                else -> 0L
            }
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
     * Resolves a local content:// or file:// URI into a valid, readable temporary File in the app's cache directory.
     * Guarantees that the file exists, has content (> 0 bytes), and can be safely passed to background upload tasks.
     */
    fun resolveContentUriToTempFile(
        context: Context,
        uri: Uri,
        suggestedFileName: String? = null
    ): Result<File> {
        return try {
            if (uri.scheme == "file") {
                val path = uri.path ?: uri.toString().removePrefix("file://")
                val file = File(path)
                if (!file.exists() || !file.canRead()) {
                    return Result.failure(FileNotFoundException("Local file does not exist or cannot be read at path: $path"))
                }
                return Result.success(file)
            }

            // Detect MIME type and extension
            val mimeType = try { context.contentResolver.getType(uri).orEmpty() } catch (_: Exception) { "" }
            val extension = when {
                mimeType.startsWith("image/png") -> "png"
                mimeType.startsWith("image/webp") -> "webp"
                mimeType.startsWith("image/gif") -> "gif"
                mimeType.startsWith("image/") -> "jpg"
                mimeType.startsWith("video/mp4") -> "mp4"
                mimeType.startsWith("video/quicktime") -> "mov"
                mimeType.startsWith("video/") -> "mp4"
                uri.toString().endsWith(".png", true) -> "png"
                uri.toString().endsWith(".webp", true) -> "webp"
                uri.toString().endsWith(".mp4", true) -> "mp4"
                uri.toString().endsWith(".mov", true) -> "mov"
                else -> "jpg"
            }

            val prefix = suggestedFileName?.substringBeforeLast('.')
                ?.takeIf { it.isNotBlank() }
                ?: "post_media_${System.currentTimeMillis()}"
            val tempFile = File(context.cacheDir, "${prefix}.${extension}")

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(FileNotFoundException("Cannot open input stream for URI: $uri"))

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() <= 0L) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Resolved temporary file is empty (0 bytes) for URI: $uri"))
            }

            Result.success(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed resolving content URI to temp file: $uri", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves local media URI to a temporary file if needed, then uploads it to Firebase Storage
     * under the user-scoped post path: users/{authorId}/{mediaScope}/posts/{postId}/{fileName}.
     */
    suspend fun uploadPostMedia(
        localUriString: String,
        authorId: String,
        postId: String,
        postType: PostType,
        isPrivate: Boolean,
        context: Context? = getApplicationContext(),
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        // If it's already a remote HTTP/HTTPS URL, no upload needed
        if (localUriString.startsWith("http://") || localUriString.startsWith("https://")) {
            onProgress(1.0f)
            return Result.success(localUriString)
        }

        val uri = try {
            Uri.parse(localUriString)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid media URI: $localUriString", e))
        }

        var tempFileToDelete: File? = null
        return try {
            val resolvedUriToUpload: String
            if (uri.scheme == "content") {
                if (context == null) {
                    return Result.failure(IllegalStateException("Application context is required to resolve content:// URI."))
                }
                val ext = if (postType == PostType.VIDEO || postType == PostType.REEL) "mp4" else "jpg"
                val resolveResult = resolveContentUriToTempFile(
                    context = context,
                    uri = uri,
                    suggestedFileName = "post_${postId}_upload.$ext"
                )
                if (resolveResult.isFailure) {
                    return Result.failure(resolveResult.exceptionOrNull() ?: Exception("Failed to resolve content URI to temp file."))
                }
                val tempFile = resolveResult.getOrNull()!!
                tempFileToDelete = tempFile
                resolvedUriToUpload = Uri.fromFile(tempFile).toString()
            } else {
                resolvedUriToUpload = localUriString
            }

            val mediaScope = if (isPrivate) "protected" else "public"
            val extension = when {
                postType == PostType.VIDEO || postType == PostType.REEL -> "mp4"
                localUriString.endsWith(".png", ignoreCase = true) -> "png"
                localUriString.endsWith(".webp", ignoreCase = true) -> "webp"
                else -> "jpg"
            }
            val fileName = when (postType) {
                PostType.VIDEO -> "video_${System.currentTimeMillis()}.$extension"
                PostType.REEL -> "reel_${System.currentTimeMillis()}.$extension"
                else -> "photo_${System.currentTimeMillis()}.$extension"
            }
            val storagePath = "users/$authorId/$mediaScope/posts/$postId/$fileName"

            storageRepository.uploadMedia(
                uriString = resolvedUriToUpload,
                destinationPath = storagePath,
                isProtected = isPrivate,
                onProgress = onProgress
            )
        } finally {
            tempFileToDelete?.let { file ->
                try {
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}
            }
        }
    }

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    suspend fun getPosts(): Result<List<Post>> {
        val user = auth.currentUser
        if (user == null || user.uid.isBlank()) {
            Log.w(TAG, "getPosts rejected: User is not authenticated.")
            return Result.failure(IllegalStateException("Authentication required: user must be signed in to load posts from Cloud Firestore."))
        }

        val projectId = try { firestore.app.options.projectId } catch (_: Exception) { "unknown" }
        Log.d(TAG, "getPosts initiating query for UID: ${user.uid} against project: $projectId")

        return try {
            // Proactively ensure ID token is refreshed to eliminate session-restoration race where Firestore sends unauthenticated request
            try {
                user.getIdToken(false).await()
            } catch (tokenEx: Exception) {
                Log.w(TAG, "getIdToken(false) validation failed: ${tokenEx.message}")
                return Result.failure(IllegalStateException("Authentication session expired or invalid. Please sign in again.", tokenEx))
            }

            // Retrieve all posts from Firestore collection directly to avoid server-side exclusion of documents lacking standard timestamp fields
            val snapshot = try {
                firestore.collection(POSTS_COLLECTION).get().await()
            } catch (firstEx: FirebaseFirestoreException) {
                if (firstEx.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "getPosts initial query returned PERMISSION_DENIED. Forcing token refresh and retrying...")
                    try {
                        user.getIdToken(true).await()
                        firestore.collection(POSTS_COLLECTION).get().await()
                    } catch (retryEx: Exception) {
                        Log.e(TAG, "getPosts retry failed: ${retryEx.message}", retryEx)
                        throw retryEx
                    }
                } else {
                    throw firstEx
                }
            }

            val uid = user.uid
            val posts = snapshot.documents
                .mapNotNull { doc -> parsePostDocument(doc, uid) }
                .filter { !it.isModeratedHidden && !it.moderationStatus.equals("REJECTED", ignoreCase = true) }
                .sortedWith(compareByDescending<Post> { it.isPinned }.thenByDescending { it.timestamp })

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getPost(postId: String): Result<Post?> {
        val user = auth.currentUser
        val uid = user?.uid.orEmpty()
        return try {
            val doc = firestore.collection(POSTS_COLLECTION).document(postId).get().await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                val post = parsePostDocument(doc, uid)
                Result.success(post)
            }
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getUserPosts(targetUserId: String): Result<List<Post>> {
        val user = auth.currentUser
        if (user == null || user.uid.isBlank()) {
            return Result.failure(IllegalStateException("Authentication required."))
        }
        return try {
            // 1. Query Firestore for posts authored by targetUserId (both standard authorId and legacy userId fields)
            val authorIdPosts = try {
                firestore.collection(POSTS_COLLECTION)
                    .whereEqualTo("authorId", targetUserId)
                    .get()
                    .await()
                    .documents
            } catch (_: Exception) {
                emptyList()
            }

            val userIdPosts = try {
                firestore.collection(POSTS_COLLECTION)
                    .whereEqualTo("userId", targetUserId)
                    .get()
                    .await()
                    .documents
            } catch (_: Exception) {
                emptyList()
            }

            val combinedDocs = (authorIdPosts + userIdPosts).distinctBy { it.id }
            val parsedPosts = combinedDocs.mapNotNull { parsePostDocument(it, user.uid) }

            // 2. Also check if the global feed has in-memory posts matching targetUserId
            val feedPosts = getPosts().getOrDefault(emptyList()).filter { it.author.id == targetUserId }

            // 3. If targetUserId is a community/dummy creator from SampleData, preserve sample posts if no Firestore posts exist
            val samplePosts = SampleData.initialPosts.filter { it.author.id == targetUserId }

            val totalPosts = (parsedPosts + feedPosts + samplePosts)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }

            Result.success(totalPosts)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    private fun parsePostDocument(doc: com.google.firebase.firestore.DocumentSnapshot, currentUid: String): Post? {
        val data = doc.data ?: return null

        @Suppress("UNCHECKED_CAST")
        val authorMap = data["author"] as? Map<String, Any?>
        val authorStr = data["author"] as? String
        val rawAuthorId = (data["authorId"] ?: data["userId"] ?: data["author_id"] ?: data["user_id"]
            ?: data["uid"] ?: data["creatorId"] ?: data["ownerId"] ?: authorMap?.get("id")
            ?: authorMap?.get("uid") ?: authorStr)?.toString()?.takeIf { it.isNotBlank() }
            ?: (data["authorName"] as? String)?.let { name ->
                SampleData.users.find { it.name.equals(name, ignoreCase = true) }?.id
            }

        val rawAuthorName = (data["authorName"] as? String)
            ?: (data["name"] as? String)
            ?: (data["displayName"] as? String)
            ?: (authorMap?.get("name") as? String)
            ?: (authorMap?.get("displayName") as? String)

        val rawAuthorUsername = (data["authorUsername"] as? String)
            ?: (data["authorHandle"] as? String)
            ?: (data["username"] as? String)
            ?: (data["handle"] as? String)
            ?: (authorMap?.get("handle") as? String)
            ?: (authorMap?.get("username") as? String)

        val matchedSampleUser = SampleData.users.find { sample ->
            (rawAuthorId != null && (sample.id == rawAuthorId || sample.handle.equals(rawAuthorId, ignoreCase = true))) ||
            (rawAuthorUsername != null && sample.handle.equals(rawAuthorUsername, ignoreCase = true)) ||
            (rawAuthorName != null && sample.name.equals(rawAuthorName, ignoreCase = true))
        }

        val authorId = rawAuthorId ?: matchedSampleUser?.id ?: "user_${doc.id.take(8)}"
        val authorName = rawAuthorName ?: matchedSampleUser?.name ?: "Member"
        val authorUsername = rawAuthorUsername ?: matchedSampleUser?.handle ?: "@$authorId"
        val authorAvatar = (data["authorAvatarUrl"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() }
            ?: (data["authorAvatar"] as? String)?.takeIf { it.isNotBlank() }
            ?: (authorMap?.get("avatarUrl") as? String)?.takeIf { it.isNotBlank() }
            ?: matchedSampleUser?.avatarUrl
            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
        val authorBio = (data["authorBio"] as? String)
            ?: (data["bio"] as? String)
            ?: (authorMap?.get("bio") as? String)
            ?: matchedSampleUser?.bio.orEmpty()
        val isPrivateAuthor = (data["isPrivate"] as? Boolean)
            ?: (data["authorIsPrivate"] as? Boolean)
            ?: ((authorMap?.get("isPrivate") as? Boolean) ?: (matchedSampleUser?.isPrivate ?: false))
        val isVerifiedAuthor = (data["authorIsVerified"] as? Boolean)
            ?: (data["isVerified"] as? Boolean)
            ?: ((authorMap?.get("isVerified") as? Boolean) ?: (matchedSampleUser?.isVerified ?: false))

        val author = User(
            id = authorId,
            name = authorName,
            handle = authorUsername,
            avatarUrl = authorAvatar,
            bio = authorBio,
            isPrivate = isPrivateAuthor,
            isVerified = isVerifiedAuthor
        )

        val rawMedia = data["mediaUrls"] as? List<*>
        val mediaUrls = rawMedia?.mapNotNull { it?.toString() }?.filter { it.isNotBlank() }
            ?: listOfNotNull(
                data["mediaUrl"]?.toString(),
                data["imageUrl"]?.toString(),
                data["videoUrl"]?.toString(),
                data["photoUrl"]?.toString(),
                data["media"]?.toString(),
                data["url"]?.toString()
            ).filter { it.isNotBlank() }

        val typeStr = (data["type"] as? String) ?: (data["postType"] as? String) ?: (data["contentType"] as? String) ?: PostType.TEXT.name
        val type = try {
            PostType.valueOf(typeStr.uppercase())
        } catch (_: Exception) {
            if (mediaUrls.any { it.endsWith(".mp4", ignoreCase = true) }) PostType.VIDEO
            else if (mediaUrls.isNotEmpty()) PostType.PHOTO
            else PostType.TEXT
        }

        val postText = (data["text"] as? String)
            ?: (data["content"] as? String)
            ?: (data["caption"] as? String)
            ?: (data["title"] as? String)
            ?: (data["body"] as? String)
            ?: (data["description"] as? String)
            ?: (data["message"] as? String).orEmpty()
        val rawTags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val textHashtags = com.example.util.HashtagUtils.extractHashtags(postText)
        val tags = (rawTags + textHashtags).distinct()
        val likedBy = (data["likedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        val commentsData = (data["comments"] as? List<*>)?.mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            val cId = (map["id"] as? String) ?: "c_${System.currentTimeMillis()}"
            val cAuthorId = (map["authorId"] as? String) ?: (map["userId"] as? String) ?: "anon"
            val cAuthorName = (map["authorName"] as? String) ?: (map["name"] as? String) ?: "User"
            val cAuthorAvatar = (map["authorAvatarUrl"] as? String) ?: (map["avatarUrl"] as? String).orEmpty()
            val cText = (map["text"] as? String) ?: (map["comment"] as? String).orEmpty()
            val cTime = (map["timestamp"] as? String) ?: "Recently"
            Comment(
                id = cId,
                author = User(id = cAuthorId, name = cAuthorName, handle = "@$cAuthorId", avatarUrl = cAuthorAvatar),
                text = cText,
                timestamp = cTime
            )
        } ?: emptyList()

        val origMap = data["originalPostData"] as? Map<*, *>
        val originalPost = if (origMap != null) {
            val oId = (origMap["id"] as? String).orEmpty()
            val oAuthorId = (origMap["authorId"] as? String).orEmpty()
            val oAuthorName = (origMap["authorName"] as? String) ?: "Member"
            val oAuthorHandle = (origMap["authorUsername"] as? String) ?: "@$oAuthorId"
            val oAuthorAvatar = (origMap["authorAvatarUrl"] as? String).orEmpty()
            val oText = (origMap["text"] as? String).orEmpty()
            val oMediaUrls = (origMap["mediaUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val oCreatedAt = (origMap["createdAt"] as? String) ?: "Recently"
            Post(
                id = oId,
                author = User(id = oAuthorId, name = oAuthorName, handle = oAuthorHandle, avatarUrl = oAuthorAvatar),
                text = oText,
                mediaUrls = oMediaUrls,
                createdAt = oCreatedAt
            )
        } else null

        val repostThoughts = data["repostThoughts"] as? String

        val rawTimestamp = data["timestamp"]
            ?: data["createdAtMillis"]
            ?: data["created_at_millis"]
            ?: data["timestampMillis"]
            ?: data["createdAt"]
            ?: data["created_at"]
            ?: data["time"]
            ?: data["date"]
            ?: data["postDate"]

        val parsedTimestamp: Long = parsePostTimestamp(rawTimestamp)

        // Preserve non-numeric createdAt string if present, or format readable string if timestamp is valid
        val rawCreatedAt = (data["createdAt"] as? String)?.takeIf { it.isNotBlank() && it.toLongOrNull() == null }
            ?: (data["created_at"] as? String)?.takeIf { it.isNotBlank() && it.toLongOrNull() == null }
            ?: (data["date"] as? String)?.takeIf { it.isNotBlank() && it.toLongOrNull() == null }

        val displayCreatedAt = rawCreatedAt ?: when {
            parsedTimestamp > 0L -> {
                val diff = System.currentTimeMillis() - parsedTimestamp
                val minutes = diff / (1000 * 60)
                val hours = minutes / 60
                val days = hours / 24
                when {
                    minutes < 1 -> "Just now"
                    minutes < 60 -> "${minutes}m ago"
                    hours < 24 -> "${hours}h ago"
                    days < 7 -> "${days}d ago"
                    else -> {
                        val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                        sdf.format(java.util.Date(parsedTimestamp))
                    }
                }
            }
            else -> "Historical post"
        }

        return Post(
            id = doc.id,
            author = author,
            type = type,
            text = postText,
            mediaUrls = mediaUrls,
            tags = tags,
            location = data["location"] as? String,
            likesCount = (data["likesCount"] as? Number)?.toInt() ?: likedBy.size,
            commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: commentsData.size,
            repostsCount = (data["repostsCount"] as? Number)?.toInt() ?: 0,
            sharesCount = (data["sharesCount"] as? Number)?.toInt() ?: 0,
            isLiked = likedBy.contains(currentUid),
            isReposted = originalPost != null,
            isSaved = false,
            comments = commentsData,
            originalPost = originalPost,
            repostThoughts = repostThoughts,
            createdAt = displayCreatedAt,
            timestamp = parsedTimestamp,
            userReaction = if (likedBy.contains(currentUid)) ((data["userReaction"] as? String) ?: "like") else null,
            moderationStatus = (data["moderationStatus"] as? String) ?: "APPROVED",
            isModeratedHidden = (data["isModeratedHidden"] as? Boolean) ?: false,
            isPinned = (data["isPinned"] as? Boolean) ?: false,
            isFeatured = (data["isFeatured"] as? Boolean) ?: false,
            featuredScheduledAt = (data["featuredScheduledAt"] as? Number)?.toLong() ?: 0L,
            featuredUntil = (data["featuredUntil"] as? Number)?.toLong() ?: 0L,
            isTrendingLabeled = (data["isTrendingLabeled"] as? Boolean) ?: false,
            isPromoted = (data["isPromoted"] as? Boolean) ?: false,
            promotionStartDate = (data["promotionStartDate"] as? Number)?.toLong() ?: 0L,
            promotionEndDate = (data["promotionEndDate"] as? Number)?.toLong() ?: 0L,
            promotionTargetCategory = (data["promotionTargetCategory"] as? String).orEmpty(),
            promotionTargetLocation = (data["promotionTargetLocation"] as? String).orEmpty(),
            impressionsCount = (data["impressionsCount"] as? Number)?.toInt() ?: 0
        )
    }

    suspend fun createPost(post: Post): Result<Unit> {
        val uid = currentAuthUid
        if (uid == null) {
            return Result.failure(IllegalStateException("Must be signed in to create a post."))
        }

        return try {
            val docRef = firestore.collection(POSTS_COLLECTION).document(post.id)
            val extracted = com.example.util.HashtagUtils.extractHashtags(post.text)
            val combinedTags = (post.tags + extracted).distinct()
            val normalizedTags = combinedTags.map { com.example.util.HashtagUtils.normalizeHashtag(it) }.distinct()

            val data = hashMapOf<String, Any?>(
                "postId" to post.id,
                "authorId" to uid,
                "authorName" to post.author.name,
                "authorUsername" to post.author.handle,
                "authorAvatarUrl" to post.author.avatarUrl,
                "authorBio" to post.author.bio,
                "text" to post.text,
                "type" to post.type.name,
                "mediaUrls" to post.mediaUrls,
                "tags" to combinedTags,
                "normalizedTags" to normalizedTags,
                "likesCount" to 0,
                "commentsCount" to 0,
                "repostsCount" to 0,
                "sharesCount" to 0,
                "likedBy" to emptyList<String>(),
                "comments" to emptyList<Map<String, Any>>(),
                "isPrivate" to post.author.isPrivate,
                "authorIsPrivate" to post.author.isPrivate,
                "authorIsVerified" to post.author.isVerified,
                "moderationStatus" to post.moderationStatus,
                "isModeratedHidden" to post.isModeratedHidden,
                "createdAt" to post.createdAt,
                "timestamp" to post.timestamp
            )

            docRef.set(data).await()

            // Also increment author user document postsCount if exists
            try {
                firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                    .document(uid)
                    .update("postsCount", FieldValue.increment(1))
                    .await()
            } catch (_: Exception) {
                // Non-critical if user document is not ready
            }

            // Also trigger mention notifications from post text if any
            val postMentions = com.example.util.MentionUtils.extractMentions(post.text)
            for (mentionedUsername in postMentions) {
                notifyMention(
                    mentionedUsername = mentionedUsername,
                    sender = post.author,
                    contextMessage = "mentioned you in a post: \"${post.text.take(60)}\"",
                    targetPostId = post.id
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun toggleLikePost(postId: String, isLikedNow: Boolean): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            val docRef = firestore.collection(POSTS_COLLECTION).document(postId)
            if (isLikedNow) {
                docRef.update(
                    mapOf(
                        "likedBy" to FieldValue.arrayUnion(uid),
                        "likesCount" to FieldValue.increment(1)
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "likedBy" to FieldValue.arrayRemove(uid),
                        "likesCount" to FieldValue.increment(-1)
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    /**
     * Updates post emoji reaction in Firestore, modifying likedBy and likesCount atomically.
     */
    suspend fun reactToPost(postId: String, reaction: String, isReacting: Boolean): Result<Unit> {
        return toggleLikePost(postId, isReacting)
    }

    suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val uid = currentAuthUid
            val snapshot = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection(COMMENTS_SUBCOLLECTION)
                .orderBy("timestampMs", Query.Direction.ASCENDING)
                .get()
                .await()

            val rawComments = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val cId = doc.id
                val authorId = data["authorId"] as? String ?: return@mapNotNull null
                val authorName = data["authorName"] as? String ?: "User"
                val authorHandle = data["authorUsername"] as? String ?: "@$authorId"
                val authorAvatar = (data["authorAvatarUrl"] as? String).orEmpty()
                val text = (data["text"] as? String).orEmpty()
                val timestamp = (data["timestamp"] as? String) ?: "Just now"
                val timestampMs = (data["timestampMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val parentId = data["parentId"] as? String
                val replyToUsername = data["replyToUsername"] as? String
                val likedBy = (data["likedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                val likesCount = (data["likesCount"] as? Number)?.toInt() ?: likedBy.size
                val isEdited = (data["isEdited"] as? Boolean) ?: false
                val authorIsVerified = (data["authorIsVerified"] as? Boolean) ?: false

                Comment(
                    id = cId,
                    postId = postId,
                    author = User(id = authorId, name = authorName, handle = authorHandle, avatarUrl = authorAvatar, isVerified = authorIsVerified),
                    text = text,
                    timestamp = timestamp,
                    timestampMs = timestampMs,
                    parentId = parentId,
                    replyToUsername = replyToUsername,
                    likesCount = likesCount,
                    likedBy = likedBy,
                    isLiked = uid != null && likedBy.contains(uid),
                    isEdited = isEdited
                )
            }

            // Separate top-level comments and replies
            val topLevel = rawComments.filter { it.parentId.isNullOrBlank() }
            val replies = rawComments.filter { !it.parentId.isNullOrBlank() }

            val grouped = topLevel.map { parent ->
                val parentReplies = replies.filter { it.parentId == parent.id }
                parent.copy(
                    replies = parentReplies,
                    repliesCount = parentReplies.size
                )
            }

            Result.success(grouped)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            val commentMap = hashMapOf<String, Any?>(
                "id" to comment.id,
                "postId" to postId,
                "authorId" to uid,
                "authorName" to comment.author.name,
                "authorUsername" to comment.author.handle,
                "authorAvatarUrl" to comment.author.avatarUrl,
                "authorIsVerified" to comment.author.isVerified,
                "text" to comment.text,
                "timestamp" to comment.timestamp,
                "timestampMs" to comment.timestampMs,
                "parentId" to comment.parentId,
                "replyToUsername" to comment.replyToUsername,
                "likesCount" to 0,
                "likedBy" to emptyList<String>(),
                "isEdited" to false
            )

            // 1. Save in comments subcollection
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection(COMMENTS_SUBCOLLECTION)
                .document(comment.id)
                .set(commentMap)
                .await()

            // 2. Increment commentsCount on the post
            val docRef = firestore.collection(POSTS_COLLECTION).document(postId)
            docRef.update("commentsCount", FieldValue.increment(1)).await()

            // 3. Trigger mention notifications if any
            val mentions = com.example.util.MentionUtils.extractMentions(comment.text)
            for (mentionedUsername in mentions) {
                notifyMention(
                    mentionedUsername = mentionedUsername,
                    sender = comment.author,
                    contextMessage = "mentioned you in a comment: \"${comment.text.take(60)}\"",
                    targetPostId = postId
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection(COMMENTS_SUBCOLLECTION)
                .document(commentId)
                .delete()
                .await()

            // Decrement post commentsCount
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .update("commentsCount", FieldValue.increment(-1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun editComment(postId: String, commentId: String, newText: String): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection(COMMENTS_SUBCOLLECTION)
                .document(commentId)
                .update(
                    mapOf(
                        "text" to newText.trim(),
                        "isEdited" to true,
                        "editedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun toggleLikeComment(postId: String, commentId: String, isLikedNow: Boolean): Result<Unit> {
        val uid = currentAuthUid ?: return Result.failure(IllegalStateException("Sign in required."))
        return try {
            val docRef = firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .collection(COMMENTS_SUBCOLLECTION)
                .document(commentId)

            if (isLikedNow) {
                docRef.update(
                    mapOf(
                        "likedBy" to FieldValue.arrayUnion(uid),
                        "likesCount" to FieldValue.increment(1)
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "likedBy" to FieldValue.arrayRemove(uid),
                        "likesCount" to FieldValue.increment(-1)
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun toggleSavePost(userId: String, postId: String, isSavedNow: Boolean): Result<Unit> {
        return try {
            val docRef = firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                .document(userId)
                .collection("savedPosts")
                .document(postId)

            if (isSavedNow) {
                docRef.set(
                    mapOf(
                        "postId" to postId,
                        "savedAt" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun getSavedPostIds(userId: String): Result<Set<String>> {
        return try {
            val snapshot = firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                .document(userId)
                .collection("savedPosts")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun incrementShareCount(postId: String): Result<Unit> {
        return try {
            firestore.collection(POSTS_COLLECTION)
                .document(postId)
                .update("sharesCount", FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    suspend fun repostPost(
        currentUid: String,
        currentUser: User,
        originalPost: Post,
        thoughts: String?
    ): Result<Post> {
        return try {
            val newPostId = "repost_${System.currentTimeMillis()}"
            val postText = thoughts?.trim().orEmpty()
            val docRef = firestore.collection(POSTS_COLLECTION).document(newPostId)

            val originalPostMap = hashMapOf<String, Any?>(
                "id" to originalPost.id,
                "authorId" to originalPost.author.id,
                "authorName" to originalPost.author.name,
                "authorUsername" to originalPost.author.handle,
                "authorAvatarUrl" to originalPost.author.avatarUrl,
                "text" to originalPost.text,
                "mediaUrls" to originalPost.mediaUrls,
                "createdAt" to originalPost.createdAt
            )

            val data = hashMapOf<String, Any?>(
                "postId" to newPostId,
                "authorId" to currentUid,
                "authorName" to currentUser.name,
                "authorUsername" to currentUser.handle,
                "authorAvatarUrl" to currentUser.avatarUrl,
                "authorBio" to currentUser.bio,
                "text" to postText,
                "type" to PostType.TEXT.name,
                "mediaUrls" to emptyList<String>(),
                "tags" to originalPost.tags,
                "normalizedTags" to originalPost.tags.map { it.lowercase() },
                "likesCount" to 0,
                "commentsCount" to 0,
                "repostsCount" to 0,
                "sharesCount" to 0,
                "likedBy" to emptyList<String>(),
                "originalPostData" to originalPostMap,
                "repostThoughts" to postText,
                "createdAt" to "Just now",
                "timestamp" to System.currentTimeMillis()
            )

            docRef.set(data).await()

            // Increment repostsCount and sharesCount on original post
            firestore.collection(POSTS_COLLECTION)
                .document(originalPost.id)
                .update(
                    mapOf(
                        "repostsCount" to FieldValue.increment(1),
                        "sharesCount" to FieldValue.increment(1)
                    )
                ).await()

            val created = Post(
                id = newPostId,
                author = currentUser,
                type = PostType.TEXT,
                text = postText,
                tags = originalPost.tags,
                originalPost = originalPost,
                repostThoughts = postText,
                createdAt = "Just now",
                timestamp = System.currentTimeMillis()
            )

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(mapFirestoreException(e))
        }
    }

    private suspend fun notifyMention(
        mentionedUsername: String,
        sender: User,
        contextMessage: String,
        targetPostId: String
    ) {
        try {
            notificationRepository.sendMentionNotification(
                mentionedUsername = mentionedUsername,
                sender = sender,
                contextMessage = "${sender.name} $contextMessage",
                targetPostId = targetPostId
            )
        } catch (_: Exception) {
            // Non-critical background notification failure
        }
    }

    private fun mapFirestoreException(e: Exception): Exception {
        if (e is FirebaseFirestoreException) {
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Permission denied: Access was not granted by Firestore security rules."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Firestore is temporarily unavailable. Check your internet connection."
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "Connection timed out while contacting Firestore."
                else -> e.localizedMessage ?: "Firestore error: ${e.code}"
            }
            return Exception(message, e)
        }
        return Exception(e.localizedMessage ?: "An error occurred with Firestore posts.", e)
    }
}
