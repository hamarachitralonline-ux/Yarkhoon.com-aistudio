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
    val isOnline: Boolean = false
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
    val commentsCount: Int = 0
) : Serializable

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val coverUrl: String,
    val category: String,
    val memberCount: Int = 1,
    val isJoined: Boolean = false
) : Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
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

