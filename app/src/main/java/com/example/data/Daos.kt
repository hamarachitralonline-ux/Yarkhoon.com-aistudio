package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialMediaDao {
    // Users Schema
    @Query("SELECT * FROM users ORDER BY fullName ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun getCurrentUser(): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    // Posts Schema
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Update
    suspend fun updatePost(post: Post)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: Int)

    // Groups Schema
    @Query("SELECT * FROM groups ORDER BY memberCount DESC")
    fun getAllGroups(): Flow<List<Group>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<Group>)

    @Update
    suspend fun updateGroup(group: Group)

    // Chats Schema
    @Query("""
        SELECT * FROM chat_messages 
        WHERE (senderId = :myId AND receiverId = :theirId) 
           OR (senderId = :theirId AND receiverId = :myId) 
        ORDER BY timestamp ASC
    """)
    fun getChatMessages(myId: String, theirId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // Marketplace Schema
    @Query("SELECT * FROM marketplace_items ORDER BY timestamp DESC")
    fun getAllMarketplaceItems(): Flow<List<MarketplaceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketplaceItem(item: MarketplaceItem)

    @Update
    suspend fun updateMarketplaceItem(item: MarketplaceItem)

    // Services Schema
    @Query("SELECT * FROM service_listings ORDER BY timestamp DESC")
    fun getAllServiceListings(): Flow<List<ServiceListing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceListing(listing: ServiceListing)

    @Query("DELETE FROM service_listings WHERE id = :listingId")
    suspend fun deleteServiceListingById(listingId: Int)
}
