package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val coverUrl: String,
    val bio: String,
    val friendStatus: String = "NONE", // NONE, SENT, RECEIVED, FRIENDS
    val isCurrentUser: Boolean = false,
    val isProfileCompleted: Boolean = false,
    val isOnline: Boolean = false,
    val email: String = "",
    val password: String = "",
    val isVerified: Boolean = false,
    val location: String = "Chitral, Pakistan",
    val occupation: String = "",
    val mutualFriendsCount: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "friend_connections")
data class FriendConnection(
    @PrimaryKey val id: String, // e.g. "req_userA_userB"
    val senderId: String,
    val senderName: String,
    val senderUsername: String = "",
    val senderAvatarUrl: String = "",
    val receiverId: String,
    val receiverName: String,
    val receiverUsername: String = "",
    val receiverAvatarUrl: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, CANCELLED
    val introMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSyncedWithFirestore: Boolean = true
) : Serializable

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val mediaType: String = "NONE", // NONE, IMAGE, VIDEO
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val userReaction: String? = null, // LIKE, LOVE, CARE, HAHA, WOW, SAD, ANGRY
    val commentsCount: Int = 0,
    val isViral: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),
    val isCachedLocally: Boolean = true
) : Serializable

enum class SentimentReaction(
    val id: String,
    val label: String,
    val emoji: String,
    val colorHex: Long
) {
    LIKE("LIKE", "Like", "👍", 0xFF1877F2),
    LOVE("LOVE", "Love", "❤️", 0xFFFA3E3E),
    CARE("CARE", "Care", "🤗", 0xFFF7B125),
    HAHA("HAHA", "Haha", "😆", 0xFFF7B125),
    WOW("WOW", "Wow", "😮", 0xFFF7B125),
    SAD("SAD", "Sad", "😢", 0xFF5C7CFA),
    ANGRY("ANGRY", "Angry", "😡", 0xFFE53E3E);

    companion object {
        fun fromId(id: String?): SentimentReaction? = entries.find { it.id.equals(id, ignoreCase = true) }
    }
}

@Entity(tableName = "post_reactions", primaryKeys = ["postId", "userId"])
data class PostReaction(
    val postId: Int,
    val userId: String,
    val userName: String = "",
    val userAvatarUrl: String = "",
    val reactionType: String = "LIKE", // LIKE, LOVE, CARE, HAHA, WOW, SAD, ANGRY
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "post_comments")
data class PostComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val parentCommentId: Int? = null,
    val replyToAuthorName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isEdited: Boolean = false,
    val isReported: Boolean = false
) : Serializable

@Entity(tableName = "comment_likes")
data class CommentLike(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commentId: Int,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "comment_reports")
data class CommentReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commentId: Int,
    val postId: Int,
    val reporterId: String,
    val reporterName: String = "",
    val reason: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val category: String = "General",
    val location: String = "Chitral, Pakistan",
    val isPrivate: Boolean = false,
    val onlyAdminsCanPost: Boolean = false,
    val ownerId: String = "currentUser",
    val memberCount: Int = 1,
    val isJoined: Boolean = false,
    val joinStatus: String = "NONE", // NONE, JOINED, REQUESTED, INVITED
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "group_members")
data class GroupMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val role: String = "MEMBER", // OWNER, ADMIN, MODERATOR, MEMBER
    val joinedAt: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
) : Serializable

@Entity(tableName = "group_join_requests")
data class GroupJoinRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val groupName: String = "",
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val userBio: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
) : Serializable

@Entity(tableName = "group_invites")
data class GroupInvite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val groupName: String,
    val groupAvatarUrl: String,
    val inviterId: String,
    val inviterName: String,
    val inviteeId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // PENDING, ACCEPTED, DECLINED
) : Serializable

@Entity(tableName = "group_posts")
data class GroupPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val authorRole: String = "MEMBER", // OWNER, ADMIN, MODERATOR, MEMBER
    val content: String,
    val mediaType: String = "NONE", // NONE, IMAGE, MULTI_IMAGE, VIDEO
    val mediaUrlsJson: String = "[]",
    val location: String = "",
    val isPinned: Boolean = false,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "group_post_comments")
data class GroupPostComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupPostId: Int,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val parentCommentId: Int? = null,
    val replyToAuthorName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isEdited: Boolean = false,
    val isReported: Boolean = false
) : Serializable

@Entity(tableName = "group_reports")
data class GroupReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val reporterId: String,
    val reason: String,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

// ==================== PAGES SYSTEM MODELS ====================

@Entity(tableName = "pages")
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val username: String, // unique @handle without spaces (e.g. "hamarachitral")
    val category: String = "Community", // Business, News & Media, Community, Organization, Public Figure, Creator, Education, Services, Shop, Local Business, Other
    val bio: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val location: String = "Chitral, Pakistan",
    val ownerId: String = "currentUser",
    val followersCount: Int = 0,
    val isFollowedByMe: Boolean = false,
    val status: String = "APPROVED", // PENDING, APPROVED, REJECTED, SUSPENDED
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "page_members")
data class PageMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val role: String = "ADMIN", // OWNER, ADMIN
    val isBlocked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "page_followers")
data class PageFollower(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int,
    val userId: String,
    val followedAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "page_posts")
data class PagePost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int,
    val pageName: String,
    val pageUsername: String,
    val pageAvatarUrl: String,
    val publisherUserId: String = "currentUser",
    val content: String,
    val mediaType: String = "NONE", // NONE, IMAGE, MULTI_IMAGE, VIDEO
    val mediaUrlsJson: String = "[]",
    val linkUrl: String = "",
    val isPinned: Boolean = false,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "page_post_comments")
data class PagePostComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pagePostId: Int,
    val pageId: Int,
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val content: String,
    val parentCommentId: Int? = null,
    val replyToAuthorName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isHidden: Boolean = false,
    val isReported: Boolean = false
) : Serializable

@Entity(tableName = "page_reports")
data class PageReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int,
    val pageName: String,
    val reporterId: String,
    val reason: String, // Spam, Fake Page, Harassment, Inappropriate content, Misleading information, Impersonation, Other
    val details: String = "",
    val status: String = "PENDING", // PENDING, REVIEWED, ACTION_TAKEN
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoiceMessage: Boolean = false,
    val audioDurationSec: Int = 0,
    val audioUrl: String = "",
    val voiceLanguage: String = "kho" // kho, ur, en
) : Serializable

@Entity(tableName = "khowar_dataset")
data class KhowarDatasetEntry(
    @PrimaryKey val id: String,
    val khowarText: String,
    val khowarRomanText: String = "",
    val urduTranslation: String = "",
    val englishTranslation: String = "",
    val audioFilePath: String = "",
    val audioDurationMs: Long = 0L,
    val dialect: String = "Standard Khowar",
    val category: String = "General",
    val contributorId: String = "system",
    val contributorName: String = "Community Contributor",
    val isVerified: Boolean = false,
    val votesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val exportStatus: String = "LOCAL" // LOCAL, SUBMITTED, READY_FOR_TRAINING
) : Serializable

@Entity(tableName = "marketplace_items")
data class MarketplaceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val category: String,
    val sellerId: String,
    val sellerName: String,
    val sellerContact: String,
    val isSold: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "service_listings")
data class ServiceListing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: String,
    val providerName: String,
    val providerAvatarUrl: String,
    val serviceType: String, // e.g., Driver, Plumber, Carpenter, Guest House Owner, Other
    val description: String,
    val phoneNumber: String,
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String = "",
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val mediaType: String = "IMAGE", // TEXT, IMAGE, VIDEO
    val mediaUrl: String = "",
    val textCaption: String = "",
    val backgroundColorHex: String = "#1877F2",
    val textColorHex: String = "#FFFFFF",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L, // 24 hours
    val viewersCount: Int = 0,
    val viewersJson: String = "[]" // Serialized list of StoryViewer
) : Serializable

data class StoryViewer(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

data class UserStoriesGroup(
    val user: User,
    val stories: List<Story>,
    val hasUnseenStories: Boolean,
    val lastTimestamp: Long
) : Serializable

data class AiChatMessage(
    val id: String = "msg_${System.currentTimeMillis()}",
    val role: String, // "user", "model", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = "gemini-3.5-flash",
    val searchCitations: List<GroundingCitation> = emptyList(),
    val isAudioVoiceTurn: Boolean = false
) : Serializable

data class AiCreationItem(
    val id: String = "ai_${System.currentTimeMillis()}",
    val type: String, // "IMAGE", "MUSIC", "VIDEO", "VOICE"
    val prompt: String,
    val mediaUrl: String,
    val title: String,
    val modelUsed: String,
    val timestamp: Long = System.currentTimeMillis(),
    val aspectRatio: String = "1:1",
    val metadata: String = ""
) : Serializable

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String,
    val recipientId: String = "currentUser",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val title: String,
    val description: String,
    val avatarUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "FRIEND_REQUEST", // FRIEND_REQUEST, MESSAGE, GROUP, LIKE, COMMENT, SYSTEM
    val targetId: String? = null
) : Serializable

typealias Notification = AppNotification



