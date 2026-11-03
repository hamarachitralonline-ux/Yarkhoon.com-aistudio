package com.example.data

enum class ReactionType(val emoji: String, val label: String) {
    LIKE("👍", "Like"),
    LOVE("❤️", "Love"),
    CARE("🤗", "Care"),
    HAHA("😂", "Haha"),
    WOW("😮", "Wow"),
    SAD("😢", "Sad"),
    ANGRY("😡", "Angry")
}

data class Reel(
    val id: String,
    val author: User,
    val videoMediaUrl: String,
    val thumbnailMediaUrl: String,
    val caption: String,
    val audioTrackTitle: String = "Original Sound - Yarkhoon Mountain Beats",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PostType {
    TEXT,
    PHOTO,
    REEL,
    VIDEO,
    BUSINESS_PROMO
}

data class User(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String = "",
    val coverUrl: String? = null,
    val bio: String = "",
    val location: String = "",
    val phoneNumber: String = "",
    val education: String = "",
    val jobTitle: String = "",
    val interests: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val isBusinessAccount: Boolean = false,
    val businessCategory: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val email: String = "",
    val website: String = "",
    val isPrivate: Boolean = false,
    val isSuspended: Boolean = false,
    val suspendedUntil: Long = 0L,
    val suspensionReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String,
    val author: User,
    val text: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val postId: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
    val parentId: String? = null,
    val replyToUsername: String? = null,
    val likedBy: List<String> = emptyList(),
    val isLiked: Boolean = false,
    val replies: List<Comment> = emptyList(),
    val repliesCount: Int = 0,
    val isEdited: Boolean = false
)

data class Post(
    val id: String,
    val author: User,
    val type: PostType = PostType.TEXT,
    val text: String,
    val mediaUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val location: String? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val repostsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isSaved: Boolean = false,
    val isPrivate: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val originalPost: Post? = null,
    val repostThoughts: String? = null,
    val createdAt: String = "Just now",
    val timestamp: Long = System.currentTimeMillis(),
    val userReaction: String? = null, // "like", "love", "laugh", "sad"
    val reactionsCount: Map<String, Int> = emptyMap(),
    // Admin Moderation, Featured, Viral & Promotion fields
    val moderationStatus: String = "APPROVED", // "PENDING", "APPROVED", "REJECTED"
    val isModeratedHidden: Boolean = false,
    val isPinned: Boolean = false,
    val isFeatured: Boolean = false,
    val featuredScheduledAt: Long? = null,
    val featuredUntil: Long? = null,
    val isTrendingLabeled: Boolean = false,
    val isPromoted: Boolean = false,
    val promotionStartDate: Long? = null,
    val promotionEndDate: Long? = null,
    val promotionTargetCategory: String? = null,
    val promotionTargetLocation: String? = null,
    val impressionsCount: Int = 0
) {
    val isCurrentlyFeatured: Boolean
        get() = isCurrentlyFeatured(System.currentTimeMillis())

    fun isCurrentlyFeatured(now: Long): Boolean {
        if (!isFeatured) return false
        val afterStart = featuredScheduledAt == null || featuredScheduledAt <= 0L || now >= featuredScheduledAt
        val beforeEnd = featuredUntil == null || featuredUntil <= 0L || now <= featuredUntil
        return afterStart && beforeEnd
    }

    val isCurrentlyPromoted: Boolean
        get() = isCurrentlyPromoted(System.currentTimeMillis())

    fun isCurrentlyPromoted(now: Long): Boolean {
        if (!isPromoted && type != PostType.BUSINESS_PROMO) return false
        val afterStart = promotionStartDate == null || promotionStartDate <= 0L || now >= promotionStartDate
        val beforeEnd = promotionEndDate == null || promotionEndDate <= 0L || now <= promotionEndDate
        return afterStart && beforeEnd
    }
}

enum class PostReaction(val id: String, val emoji: String, val label: String) {
    LIKE("like", "👍", "Like"),
    LOVE("love", "❤️", "Love"),
    LAUGH("laugh", "😂", "Haha"),
    SAD("sad", "😢", "Sad");

    companion object {
        fun fromId(id: String?): PostReaction? = values().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

// ---------------- Stage 12: Following System & Privacy Models ----------------

data class FollowRequest(
    val id: String,
    val requester: User,
    val targetUserId: String,
    val status: String = "pending", // "pending", "approved", "rejected"
    val timestamp: Long = System.currentTimeMillis()
)

data class PrivacyRelationship(
    val isFollowing: Boolean = false,
    val isFollowRequested: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isBlocked: Boolean = false,
    val isBlockedBy: Boolean = false,
    val isMuted: Boolean = false,
    val isFavorite: Boolean = false
)

data class Story(
    val id: String,
    val author: User,
    val mediaUrl: String,
    val caption: String = "",
    val isViewed: Boolean = false,
    val createdAt: String = "2h ago",
    val timestamp: Long = System.currentTimeMillis()
)

enum class BusinessCategory(val displayName: String) {
    ALL("All"),
    HOTELS_HOSPITALITY("Hotels & Guest Houses"),
    RESTAURANTS_FOOD("Restaurants & Food"),
    SHOPS_RETAIL("Shops & Retail"),
    DRIVERS_TRANSPORT("Drivers & Transport"),
    ELECTRICIANS("Electricians"),
    PLUMBERS("Plumbers"),
    CARPENTERS("Carpenters"),
    PAINTERS("Painters"),
    MECHANICS("Mechanics"),
    SOLAR_TECH("Solar Technicians"),
    TEACHERS_TUTORS("Teachers & Tutors"),
    FREELANCE_TECH("Tech & Freelancers"),
    CONSTRUCTION("Construction Workers"),
    AGRICULTURE_SERVICES("Agriculture Services"),
    TOURISM_GUIDES("Tourism & Guides"),
    TRADES("Skilled Trades"),
    CRAFTS("Artisans & Crafts"),
    EDUCATION("Education"),
    HOSPITALITY("Hospitality"),
    SERVICES("General Services"),
    OTHER("Other Services")
}

data class BusinessProfile(
    val id: String,
    val owner: User,
    val businessName: String,
    val category: BusinessCategory,
    val serviceHighlight: String,
    val description: String,
    val location: String,
    val phone: String,
    val whatsapp: String = "",
    val priceRange: String = "",
    val rating: Float = 5.0f,
    val reviewsCount: Int = 0,
    val isVerified: Boolean = false,
    val status: String = "active", // "active", "inactive"
    val portfolioImages: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MarketplaceCategory(val displayName: String) {
    ALL("All Items"),
    CARS("Cars"),
    MOTORBIKES("Motorbikes"),
    MOBILE_PHONES("Mobile Phones"),
    VEGETABLES("Vegetables"),
    FRUITS("Fruits"),
    AGRICULTURE("Agricultural Products"),
    FURNITURE("Furniture"),
    CLOTHING("Clothing"),
    OTHER("Other Products")
}

data class MarketplaceItem(
    val id: String,
    val seller: User,
    val title: String,
    val category: MarketplaceCategory,
    val price: String, // e.g. "PKR 45,000" or "$150"
    val description: String,
    val phone: String,
    val whatsapp: String = "",
    val location: String = "Yarkhoon Valley",
    val condition: String = "Good",
    val status: String = "active", // "active", "sold"
    val imageUrls: List<String> = emptyList(),
    val createdAt: String = "Just now",
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class BusinessHubSection(val title: String) {
    SERVICES("Business & Services"),
    MARKETPLACE("Buy and Sell")
}

data class NotificationItem(
    val id: String,
    val sender: User,
    val type: String, // "like", "comment", "reply", "mention", "follow", "follow_request", "follow_accepted", "repost", "inquiry"
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val recipientId: String = "",
    val targetPostId: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String = "",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class Conversation(
    val id: String,
    val recipient: User,
    val lastMessage: String = "",
    val timestamp: String = "Just now",
    val unreadCount: Int = 0,
    val participantIds: List<String> = emptyList(),
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis()
)

// ---------------- Stage 10: Search, Explore & Trending Models ----------------

enum class SearchTab(val label: String) {
    ALL("All"),
    PEOPLE("People"),
    POSTS("Posts"),
    HASHTAGS("Hashtags"),
    VIDEOS("Videos"),
    CREATORS("Creators")
}

data class TrendingHashtag(
    val tag: String,
    val normalizedTag: String,
    val postCount: Int,
    val totalEngagement: Int = 0,
    val score: Double = 0.0,
    val category: String = "Trending in Chitral"
)

data class HashtagDetail(
    val tag: String,
    val normalizedTag: String,
    val postCount: Int,
    val totalLikes: Int = 0,
    val totalComments: Int = 0,
    val posts: List<Post> = emptyList()
)

data class SearchResults(
    val query: String = "",
    val people: List<User> = emptyList(),
    val posts: List<Post> = emptyList(),
    val hashtags: List<TrendingHashtag> = emptyList(),
    val videos: List<Post> = emptyList(),
    val reels: List<Reel> = emptyList(),
    val creators: List<User> = emptyList(),
    val totalResultsCount: Int = 0
)

data class ExploreContent(
    val recommendedCreators: List<User> = emptyList(),
    val trendingHashtags: List<TrendingHashtag> = emptyList(),
    val popularPosts: List<Post> = emptyList(),
    val popularReels: List<Reel> = emptyList(),
    val activeDiscussions: List<Post> = emptyList(),
    val mediaGridPosts: List<Post> = emptyList()
)

// ---------------- Stage 13: Admin Dashboard & Content Moderation Models ----------------

enum class ReportContentType(val displayName: String) {
    POST("Post"),
    REEL("Reel"),
    COMMENT("Comment"),
    USER_PROFILE("User Profile"),
    BUSINESS_LISTING("Business Hub Listing"),
    MARKETPLACE_LISTING("Marketplace Listing");

    val label: String get() = displayName
}

enum class ReportReason(val displayName: String) {
    SPAM("Spam or misleading"),
    HARASSMENT("Harassment or bullying"),
    IMPERSONATION("Impersonation or fake account"),
    SCAM("Scam, fraud or unauthorized sale"),
    INAPPROPRIATE_CONTENT("Inappropriate, adult or violent content"),
    HATE_SPEECH("Hate speech or discrimination"),
    OTHER("Other violation");

    val label: String get() = displayName
}

enum class ReportStatus(val displayName: String) {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    REVIEWED("Reviewed"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed");

    val label: String get() = displayName
}

data class ContentReport(
    val id: String,
    val reportId: String = id,
    val reporterId: String,
    val reporterName: String = "Community Member",
    val reporterHandle: String = "",
    val reportedUserId: String? = null,
    val reportedUserName: String? = null,
    val contentType: ReportContentType,
    val contentId: String,
    val contentSummary: String = "",
    val reason: ReportReason,
    val description: String = "",
    val status: ReportStatus = ReportStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val resolutionNotes: String? = null
)

enum class ModerationActionType(val displayName: String) {
    DISMISS_REPORT("Dismiss Report"),
    HIDE_CONTENT("Hide Content"),
    RESTORE_CONTENT("Restore Content"),
    SUSPEND_USER("Suspend Account"),
    REINSTATE_USER("Reinstate Account"),
    APPROVE_POST("Approve Post"),
    REJECT_POST("Reject Post"),
    FEATURE_POST("Feature Post"),
    UNFEATURE_POST("Remove Featured Post"),
    PIN_POST("Pin Post to Feed"),
    UNPIN_POST("Unpin Post"),
    SET_TRENDING_POST("Update Trending Label"),
    PROMOTE_POST("Promote Post"),
    END_PROMOTION("End Post Promotion"),
    APPROVE_VERIFICATION("Approve Blue Tick Verification"),
    REJECT_VERIFICATION("Reject Verification Request"),
    REVOKE_VERIFICATION("Revoke Blue Tick Verification"),
    ADD_VILLAGE("Add Village Entry"),
    UPDATE_VILLAGE("Update Village Entry"),
    DELETE_VILLAGE("Delete Village Entry"),
    SEND_ANNOUNCEMENT("Publish Official Announcement"),
    BROADCAST_ANNOUNCEMENT("Broadcast Announcement"),
    GRANT_BLUE_TICK("Grant Blue Tick"),
    ASSIGN_ADMIN_ROLE("Assign Admin Role"),
    REVOKE_ADMIN_ROLE("Revoke Admin Role"),
    ADMIN_LOGIN("Admin Portal Login");

    val label: String get() = displayName
}

data class ModerationAuditLog(
    val id: String,
    val adminId: String,
    val adminEmail: String,
    val actionType: ModerationActionType,
    val targetType: String, // "post", "reel", "comment", "user", "business", "marketplace", "report"
    val targetId: String,
    val targetUserId: String? = null,
    val reason: String,
    val durationDays: Int? = null, // for suspensions
    val timestamp: Long = System.currentTimeMillis()
)

data class UserSuspensionInfo(
    val userId: String,
    val isSuspended: Boolean = false,
    val suspensionReason: String = "",
    val suspendedAt: Long = 0L,
    val suspendedUntil: Long = 0L, // 0 means indefinite or until reinstated
    val suspendedBy: String = ""
) {
    val isCurrentlySuspended: Boolean
        get() = isCurrentlySuspended(System.currentTimeMillis())

    fun isCurrentlySuspended(now: Long = System.currentTimeMillis()): Boolean =
        isSuspended && (suspendedUntil == 0L || suspendedUntil > now)
}

