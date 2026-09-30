package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val bio: String = "",
    val friendStatus: String = "NONE",
    val isCurrentUser: Boolean = false,
    val isProfileCompleted: Boolean = true,
    val isOnline: Boolean = false,
    val email: String = "",
    val password: String = "",
    val isVerified: Boolean = false,
    val location: String = "",
    val occupation: String = "",
    val mutualFriendsCount: Int = 0,
    val cachedAt: Long = 0L
) : Serializable

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val mediaType: String = "text",
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val userReaction: String? = null,
    val commentsCount: Int = 0,
    val isViral: Boolean = false,
    val cachedAt: Long = 0L,
    val isCachedLocally: Boolean = true
)

@Entity(tableName = "post_comments")
data class PostComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int = 0,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val parentCommentId: Int? = null,
    val replyToAuthorName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isEdited: Boolean = false,
    val isReported: Boolean = false
)

@Entity(tableName = "comment_likes")
data class CommentLike(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commentId: Int = 0,
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "comment_reports")
data class CommentReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commentId: Int = 0,
    val postId: Int = 0,
    val reporterId: String = "",
    val reporterName: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val category: String = "General",
    val location: String = "",
    val isPrivate: Boolean = false,
    val onlyAdminsCanPost: Boolean = false,
    val ownerId: String = "",
    val memberCount: Int = 1,
    val isJoined: Boolean = false,
    val joinStatus: String = "NOT_JOINED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members")
data class GroupMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val role: String = "MEMBER",
    val joinedAt: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
)

@Entity(tableName = "group_join_requests")
data class GroupJoinRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 0,
    val groupName: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val userBio: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING"
)

@Entity(tableName = "group_invites")
data class GroupInvite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 0,
    val groupName: String = "",
    val groupAvatarUrl: String = "",
    val inviterId: String = "",
    val inviterName: String = "",
    val inviteeId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING"
)

@Entity(tableName = "group_posts")
data class GroupPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 0,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val authorRole: String = "MEMBER",
    val content: String = "",
    val mediaType: String = "text",
    val mediaUrlsJson: String = "[]",
    val location: String = "",
    val isPinned: Boolean = false,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_post_comments")
data class GroupPostComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupPostId: Int = 0,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val parentCommentId: Int? = null,
    val replyToAuthorName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isEdited: Boolean = false,
    val isReported: Boolean = false
)

@Entity(tableName = "group_reports")
data class GroupReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int = 0,
    val reporterId: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isVoiceMessage: Boolean = false,
    val audioDurationSec: Int = 0,
    val audioUrl: String = "",
    val voiceLanguage: String = ""
)

@Entity(tableName = "marketplace_items")
data class MarketplaceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val category: String = "General",
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerContact: String = "",
    val isSold: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "service_listings")
data class ServiceListing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: String = "",
    val providerName: String = "",
    val providerAvatarUrl: String = "",
    val serviceType: String = "",
    val description: String = "",
    val phoneNumber: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val mediaType: String = "image",
    val mediaUrl: String = "",
    val textCaption: String = "",
    val backgroundColorHex: String = "#000000",
    val textColorHex: String = "#FFFFFF",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L,
    val viewersCount: Int = 0,
    val viewersJson: String = "[]"
)

@Entity(tableName = "friend_connections")
data class FriendConnection(
    @PrimaryKey val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderUsername: String = "",
    val senderAvatarUrl: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverUsername: String = "",
    val receiverAvatarUrl: String = "",
    val status: String = "PENDING",
    val introMessage: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSyncedWithFirestore: Boolean = false
)

@Entity(tableName = "notifications")
data class AppNotification(
    @PrimaryKey val id: String = "",
    val recipientId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarUrl: String = "",
    val title: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = "GENERAL",
    val targetId: String? = null
)

@Entity(tableName = "khowar_dataset")
data class KhowarDatasetEntry(
    @PrimaryKey val id: String = "",
    val khowarText: String = "",
    val khowarRomanText: String = "",
    val urduTranslation: String = "",
    val englishTranslation: String = "",
    val audioFilePath: String = "",
    val audioDurationMs: Long = 0L,
    val dialect: String = "",
    val category: String = "",
    val contributorId: String = "",
    val contributorName: String = "",
    val isVerified: Boolean = false,
    val votesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val exportStatus: String = "PENDING"
)

@Entity(tableName = "post_reactions", primaryKeys = ["postId", "userId"])
data class PostReaction(
    val postId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val reactionType: String = "LIKE",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pages")
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val username: String = "",
    val category: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val location: String = "",
    val ownerId: String = "",
    val followersCount: Int = 0,
    val isFollowedByMe: Boolean = false,
    val status: String = "ACTIVE",
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "page_members")
data class PageMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val role: String = "MEMBER",
    val isBlocked: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "page_followers")
data class PageFollower(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int = 0,
    val userId: String = "",
    val followedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "page_posts")
data class PagePost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int = 0,
    val pageName: String = "",
    val pageUsername: String = "",
    val pageAvatarUrl: String = "",
    val publisherUserId: String = "",
    val content: String = "",
    val mediaType: String = "text",
    val mediaUrlsJson: String = "[]",
    val linkUrl: String = "",
    val isPinned: Boolean = false,
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "page_post_comments")
data class PagePostComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pagePostId: Int = 0,
    val pageId: Int = 0,
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val content: String = "",
    val parentCommentId: Int? = null,
    val replyToAuthorName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val isHidden: Boolean = false,
    val isReported: Boolean = false
)

@Entity(tableName = "page_reports")
data class PageReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int = 0,
    val pageName: String = "",
    val reporterId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)

data class StoryViewer(
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class UserStoriesGroup(
    val user: User = User(id = ""),
    val stories: List<Story> = emptyList(),
    val hasUnseenStories: Boolean = false,
    val lastTimestamp: Long = 0L
)

data class AiChatMessage(
    val id: String = "",
    val role: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = "",
    val searchCitations: List<GroundingCitation> = emptyList(),
    val isAudioVoiceTurn: Boolean = false
)

data class AiCreationItem(
    val id: String = "",
    val title: String = "",
    val prompt: String = "",
    val type: String = "image",
    val mediaUrl: String = "",
    val modelUsed: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val aspectRatio: String = "1:1",
    val metadata: String = ""
)

data class Article(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = "",
    val date: String = "",
    val author: String = "",
    val imageUrl: String? = null
)

data class ValleyPlace(
    val id: String = "",
    val name: String = "",
    val elevation: String = "",
    val description: String = "",
    val highlights: List<String> = emptyList()
)

data class CulturalFeature(
    val id: String = "",
    val title: String = "",
    val language: String = "",
    val description: String = "",
    val traditionType: String = ""
)

enum class SentimentReaction(
    val id: String,
    val label: String,
    val emoji: String,
    val colorHex: Long
) {
    LIKE("LIKE", "Like", "👍", 0xFF1877F2),
    LOVE("LOVE", "Love", "❤️", 0xFFF33E5B),
    CARE("CARE", "Care", "🥰", 0xFFF7B125),
    HAHA("HAHA", "Haha", "😆", 0xFFF7B125),
    WOW("WOW", "Wow", "😮", 0xFFF7B125),
    SAD("SAD", "Sad", "😢", 0xFFF7B125),
    ANGRY("ANGRY", "Angry", "😡", 0xFFE9710F);

    companion object {
        fun fromId(id: String?): SentimentReaction? {
            if (id == null) return null
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }
    }
}

