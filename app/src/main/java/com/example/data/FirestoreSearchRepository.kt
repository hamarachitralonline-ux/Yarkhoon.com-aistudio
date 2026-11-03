package com.example.data

import com.example.util.HashtagUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirestoreSearchRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val postRepository: FirestorePostRepository = FirestorePostRepository(),
    private val reelRepository: FirestoreReelRepository = FirestoreReelRepository()
) {

    private val currentAuthUid: String?
        get() = auth.currentUser?.uid

    /**
     * Executes a global bounded search across Users, Posts, Reels, Hashtags, Videos, and Creators.
     * Enforces privacy by redacting sensitive user information (phone numbers, emails).
     */
    suspend fun search(
        query: String,
        currentUserId: String = currentAuthUid.orEmpty(),
        limit: Long = 40L
    ): Result<SearchResults> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return Result.success(SearchResults())
        }

        val normalizedQuery = HashtagUtils.normalizeHashtag(cleanQuery)
        val queryLower = cleanQuery.lowercase()

        return try {
            // 1. Search Users (People & Creators) with privacy redacting
            val usersSnapshot = firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                .limit(limit)
                .get()
                .await()

            val matchingUsers = usersSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val name = (data["name"] as? String).orEmpty()
                val handle = (data["username"] as? String) ?: (data["handle"] as? String) ?: "@${doc.id.take(8)}"
                val jobTitle = (data["profession"] as? String) ?: (data["jobTitle"] as? String).orEmpty()
                val location = (data["location"] as? String).orEmpty()
                val bio = (data["bio"] as? String).orEmpty()

                val matches = name.lowercase().contains(queryLower) ||
                        handle.lowercase().contains(queryLower) ||
                        jobTitle.lowercase().contains(queryLower) ||
                        location.lowercase().contains(queryLower) ||
                        bio.lowercase().contains(queryLower)

                if (!matches) return@mapNotNull null

                // PRIVACY SANITIZATION: Never expose phone or email in public search results
                User(
                    id = doc.id,
                    name = name.ifBlank { "Member" },
                    handle = handle,
                    avatarUrl = (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                        ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                    coverUrl = data["coverUrl"] as? String,
                    bio = bio,
                    location = location,
                    phoneNumber = "", // Redacted for privacy
                    email = "",       // Redacted for privacy
                    jobTitle = jobTitle,
                    isVerified = (data["isVerified"] as? Boolean) ?: false,
                    isBusinessAccount = (data["isBusinessAccount"] as? Boolean) ?: ((data["accountType"] as? String) == "business"),
                    followersCount = (data["followersCount"] as? Number)?.toInt() ?: 0,
                    followingCount = (data["followingCount"] as? Number)?.toInt() ?: 0,
                    postsCount = (data["postsCount"] as? Number)?.toInt() ?: 0,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }

            val people = matchingUsers.filter { it.id != currentUserId }
            val creators = matchingUsers.filter { it.isVerified || it.isBusinessAccount || it.followersCount > 5 }

            // 2. Search Posts & Videos
            val postsResult = postRepository.getPosts()
            val allPosts = postsResult.getOrNull().orEmpty()

            val matchingPosts = allPosts.filter { post ->
                val textMatch = post.text.lowercase().contains(queryLower)
                val authorMatch = post.author.name.lowercase().contains(queryLower) ||
                        post.author.handle.lowercase().contains(queryLower)
                val tagMatch = post.tags.any { tag ->
                    HashtagUtils.normalizeHashtag(tag).contains(normalizedQuery)
                }
                textMatch || authorMatch || tagMatch
            }

            // Videos: Posts of type VIDEO or with media, or Reels
            val videoPosts = matchingPosts.filter { post ->
                post.type == PostType.VIDEO || post.type == PostType.REEL || post.mediaUrls.isNotEmpty()
            }

            // 3. Search Reels
            val reelsResult = reelRepository.getReels()
            val allReels = reelsResult.getOrNull().orEmpty()

            val matchingReels = allReels.filter { reel ->
                reel.caption.lowercase().contains(queryLower) ||
                        reel.author.name.lowercase().contains(queryLower) ||
                        reel.author.handle.lowercase().contains(queryLower)
            }

            // 4. Calculate Matching Hashtags
            val hashtagMap = mutableMapOf<String, MutableList<Post>>()
            allPosts.forEach { post ->
                val extracted = (post.tags + HashtagUtils.extractHashtags(post.text)).distinct()
                extracted.forEach { tag ->
                    val norm = HashtagUtils.normalizeHashtag(tag)
                    if (norm.isNotBlank()) {
                        hashtagMap.getOrPut(norm) { mutableListOf() }.add(post)
                    }
                }
            }

            val matchingHashtags = hashtagMap.filterKeys { it.contains(normalizedQuery) }
                .map { (normTag, postsWithTag) ->
                    val displayTag = postsWithTag.firstOrNull()?.tags?.firstOrNull {
                        HashtagUtils.normalizeHashtag(it) == normTag
                    } ?: HashtagUtils.formatDisplayHashtag(normTag)

                    val totalLikes = postsWithTag.sumOf { it.likesCount }
                    val totalComments = postsWithTag.sumOf { it.commentsCount }
                    val newestTime = postsWithTag.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
                    val score = HashtagUtils.calculateTrendScore(
                        postCount = postsWithTag.size,
                        totalLikes = totalLikes,
                        totalComments = totalComments,
                        newestTimestamp = newestTime
                    )

                    TrendingHashtag(
                        tag = displayTag,
                        normalizedTag = normTag,
                        postCount = postsWithTag.size,
                        totalEngagement = totalLikes + totalComments,
                        score = score,
                        category = "Trending in Chitral & Yarkhoon"
                    )
                }
                .sortedByDescending { it.score }

            val totalCount = people.size + matchingPosts.size + matchingHashtags.size + matchingReels.size

            Result.success(
                SearchResults(
                    query = cleanQuery,
                    people = people,
                    posts = matchingPosts,
                    hashtags = matchingHashtags,
                    videos = videoPosts,
                    reels = matchingReels,
                    creators = creators,
                    totalResultsCount = totalCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Loads structured Explore content including recommended creators, trending hashtags,
     * popular posts, active discussions, and popular reels from real Firestore collections.
     */
    suspend fun getExploreContent(
        currentUserId: String = currentAuthUid.orEmpty()
    ): Result<ExploreContent> {
        return try {
            val postsResult = postRepository.getPosts()
            if (postsResult.isFailure) {
                return Result.failure(postsResult.exceptionOrNull() ?: Exception("Failed to load posts from Cloud Firestore."))
            }
            val allPosts = postsResult.getOrNull().orEmpty()

            val reelsResult = reelRepository.getReels()
            val allReels = reelsResult.getOrNull().orEmpty()

            // 1. Fetch Users for Recommended Creators
            val usersSnapshot = try {
                firestore.collection(FirestoreUserRepository.USERS_COLLECTION)
                    .limit(30)
                    .get()
                    .await()
            } catch (e: Exception) {
                null
            }

            val creators = if (usersSnapshot != null && !usersSnapshot.isEmpty) {
                usersSnapshot.documents.mapNotNull { doc ->
                    if (doc.id == currentUserId) return@mapNotNull null
                    val data = doc.data ?: return@mapNotNull null
                    val isVerified = (data["isVerified"] as? Boolean) ?: false
                    val isBusiness = (data["isBusinessAccount"] as? Boolean) ?: ((data["accountType"] as? String) == "business")
                    val followers = (data["followersCount"] as? Number)?.toInt() ?: 0

                    User(
                        id = doc.id,
                        name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: "Member",
                        handle = (data["username"] as? String) ?: (data["handle"] as? String) ?: "@${doc.id.take(8)}",
                        avatarUrl = (data["avatarUrl"] as? String)?.takeIf { it.isNotBlank() }
                            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                        bio = (data["bio"] as? String).orEmpty(),
                        location = (data["location"] as? String).orEmpty(),
                        isVerified = isVerified,
                        isBusinessAccount = isBusiness,
                        followersCount = followers
                    )
                }.sortedWith(
                    compareByDescending<User> { it.isVerified }
                        .thenByDescending { it.isBusinessAccount }
                        .thenByDescending { it.followersCount }
                )
            } else {
                emptyList()
            }

            // 2. Compute Measurable Trending Hashtags from Posts
            val hashtagMap = mutableMapOf<String, MutableList<Post>>()
            allPosts.forEach { post ->
                val extracted = (post.tags + HashtagUtils.extractHashtags(post.text)).distinct()
                extracted.forEach { tag ->
                    val norm = HashtagUtils.normalizeHashtag(tag)
                    if (norm.isNotBlank()) {
                        hashtagMap.getOrPut(norm) { mutableListOf() }.add(post)
                    }
                }
            }

            val trendingHashtags = hashtagMap.map { (normTag, postsWithTag) ->
                val displayTag = postsWithTag.firstOrNull()?.tags?.firstOrNull {
                    HashtagUtils.normalizeHashtag(it) == normTag
                } ?: HashtagUtils.formatDisplayHashtag(normTag)

                val totalLikes = postsWithTag.sumOf { it.likesCount }
                val totalComments = postsWithTag.sumOf { it.commentsCount }
                val newestTime = postsWithTag.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()

                val score = HashtagUtils.calculateTrendScore(
                    postCount = postsWithTag.size,
                    totalLikes = totalLikes,
                    totalComments = totalComments,
                    newestTimestamp = newestTime
                )

                TrendingHashtag(
                    tag = displayTag,
                    normalizedTag = normTag,
                    postCount = postsWithTag.size,
                    totalEngagement = totalLikes + totalComments,
                    score = score,
                    category = if (normTag in listOf("yarkhoon", "chitral", "shandur")) "Local Culture & Heritage" else "Trending in Chitral"
                )
            }.sortedByDescending { it.score }
                .take(15)

            // 3. Active Discussions (posts with lively comment threads and freshness)
            val activeDiscussions = allPosts.filter { it.commentsCount > 0 }
                .sortedByDescending { HashtagUtils.calculateDiscussionScore(it) }
                .take(6)

            // 4. Popular Posts (overall engagement)
            val popularPosts = allPosts.sortedByDescending {
                (it.likesCount * 2) + (it.commentsCount * 3) + (it.sharesCount * 2)
            }.take(10)

            // 5. Popular Reels
            val popularReels = allReels.sortedByDescending {
                (it.likesCount * 2) + (it.commentsCount * 3) + it.sharesCount
            }.take(10)

            // 6. Media Grid Posts (Photo & Video Discovery)
            val mediaGrid = allPosts.filter { it.mediaUrls.isNotEmpty() }
                .sortedByDescending { it.timestamp }
                .take(18)

            Result.success(
                ExploreContent(
                    recommendedCreators = creators,
                    trendingHashtags = trendingHashtags,
                    popularPosts = popularPosts,
                    popularReels = popularReels,
                    activeDiscussions = activeDiscussions,
                    mediaGridPosts = mediaGrid
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves complete detail and related posts for a specific hashtag.
     */
    fun getHashtagDetail(tag: String, allPosts: List<Post>): HashtagDetail {
        val normalized = HashtagUtils.normalizeHashtag(tag)
        val matchingPosts = allPosts.filter { post ->
            val allTags = (post.tags + HashtagUtils.extractHashtags(post.text)).distinct()
            allTags.any { HashtagUtils.normalizeHashtag(it) == normalized }
        }

        val totalLikes = matchingPosts.sumOf { it.likesCount }
        val totalComments = matchingPosts.sumOf { it.commentsCount }

        return HashtagDetail(
            tag = HashtagUtils.formatDisplayHashtag(tag),
            normalizedTag = normalized,
            postCount = matchingPosts.size,
            totalLikes = totalLikes,
            totalComments = totalComments,
            posts = matchingPosts
        )
    }
}
