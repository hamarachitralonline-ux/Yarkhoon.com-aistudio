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

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<User?>

    @Query("SELECT COUNT(*) FROM users")
    fun getCachedUsersCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    // Posts Schema
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<Post>>

    @Query("SELECT COUNT(*) FROM posts")
    fun getCachedPostsCount(): Flow<Int>

    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY timestamp DESC")
    fun getPostsByAuthor(authorId: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE content LIKE '%' || :query || '%' OR authorName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchPosts(query: String): Flow<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: Int): Post?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Update
    suspend fun updatePost(post: Post)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: Int)

    @Query("UPDATE posts SET mediaUrl = :newUrl WHERE mediaUrl LIKE '%gtv-videos-bucket%' OR mediaUrl LIKE '%ForBiggerBlazes%'")
    suspend fun fixLegacyVideoUrls(newUrl: String)

    // Post Comments Schema
    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getPostComments(postId: Int): Flow<List<PostComment>>

    @Query("SELECT * FROM post_comments ORDER BY timestamp ASC")
    fun getAllPostComments(): Flow<List<PostComment>>

    @Query("SELECT * FROM post_comments WHERE id = :commentId LIMIT 1")
    suspend fun getPostCommentById(commentId: Int): PostComment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostComment(comment: PostComment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostComments(comments: List<PostComment>)

    @Update
    suspend fun updatePostComment(comment: PostComment)

    @Query("DELETE FROM post_comments WHERE id = :commentId")
    suspend fun deletePostCommentById(commentId: Int)

    @Query("DELETE FROM post_comments WHERE postId = :postId")
    suspend fun deleteAllPostComments(postId: Int)

    // Comment Likes & Reports
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentLike(like: CommentLike)

    @Query("DELETE FROM comment_likes WHERE commentId = :commentId AND userId = :userId")
    suspend fun deleteCommentLike(commentId: Int, userId: String)

    @Query("SELECT * FROM comment_likes WHERE userId = :userId")
    fun getCommentLikesForUser(userId: String): Flow<List<CommentLike>>

    // Post Reactions & Sentiments Schema
    @Query("SELECT * FROM post_reactions ORDER BY timestamp DESC")
    fun getAllPostReactions(): Flow<List<PostReaction>>

    @Query("SELECT * FROM post_reactions WHERE postId = :postId ORDER BY timestamp DESC")
    fun getReactionsForPost(postId: Int): Flow<List<PostReaction>>

    @Query("SELECT * FROM post_reactions WHERE postId = :postId AND userId = :userId LIMIT 1")
    suspend fun getPostReaction(postId: Int, userId: String): PostReaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostReaction(reaction: PostReaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostReactions(reactions: List<PostReaction>)

    @Query("DELETE FROM post_reactions WHERE postId = :postId AND userId = :userId")
    suspend fun deletePostReaction(postId: Int, userId: String)

    @Query("DELETE FROM post_reactions WHERE postId = :postId")
    suspend fun deleteAllReactionsForPost(postId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommentReport(report: CommentReport)

    @Query("SELECT * FROM comment_reports ORDER BY timestamp DESC")
    fun getAllCommentReports(): Flow<List<CommentReport>>

    // Groups Schema
    @Query("SELECT * FROM groups ORDER BY memberCount DESC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    fun getGroupById(groupId: Int): Flow<Group?>

    @Query("SELECT * FROM groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupByIdOnce(groupId: Int): Group?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<Group>)

    @Update
    suspend fun updateGroup(group: Group)

    @Query("DELETE FROM groups WHERE id = :groupId")
    suspend fun deleteGroupById(groupId: Int)

    // Group Members Schema
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY CASE role WHEN 'OWNER' THEN 1 WHEN 'ADMIN' THEN 2 WHEN 'MODERATOR' THEN 3 ELSE 4 END, joinedAt ASC")
    fun getGroupMembers(groupId: Int): Flow<List<GroupMember>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getGroupMember(groupId: Int, userId: String): GroupMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: GroupMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<GroupMember>)

    @Update
    suspend fun updateGroupMember(member: GroupMember)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteGroupMember(groupId: Int, userId: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteAllGroupMembers(groupId: Int)

    // Group Join Requests Schema
    @Query("SELECT * FROM group_join_requests WHERE groupId = :groupId AND status = 'PENDING' ORDER BY timestamp DESC")
    fun getGroupJoinRequests(groupId: Int): Flow<List<GroupJoinRequest>>

    @Query("SELECT * FROM group_join_requests WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getGroupJoinRequestByUser(groupId: Int, userId: String): GroupJoinRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupJoinRequest(request: GroupJoinRequest)

    @Update
    suspend fun updateGroupJoinRequest(request: GroupJoinRequest)

    @Query("DELETE FROM group_join_requests WHERE id = :requestId")
    suspend fun deleteGroupJoinRequestById(requestId: Int)

    @Query("DELETE FROM group_join_requests WHERE groupId = :groupId")
    suspend fun deleteAllGroupJoinRequests(groupId: Int)

    // Group Invites Schema
    @Query("SELECT * FROM group_invites WHERE inviteeId = :userId AND status = 'PENDING' ORDER BY timestamp DESC")
    fun getGroupInvitesForUser(userId: String): Flow<List<GroupInvite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupInvite(invite: GroupInvite)

    @Update
    suspend fun updateGroupInvite(invite: GroupInvite)

    @Query("DELETE FROM group_invites WHERE id = :inviteId")
    suspend fun deleteGroupInviteById(inviteId: Int)

    // Group Posts Schema
    @Query("SELECT * FROM group_posts WHERE groupId = :groupId ORDER BY isPinned DESC, timestamp DESC")
    fun getGroupPosts(groupId: Int): Flow<List<GroupPost>>

    @Query("SELECT * FROM group_posts WHERE id = :postId LIMIT 1")
    suspend fun getGroupPostById(postId: Int): GroupPost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupPost(post: GroupPost)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupPosts(posts: List<GroupPost>)

    @Update
    suspend fun updateGroupPost(post: GroupPost)

    @Query("DELETE FROM group_posts WHERE id = :postId")
    suspend fun deleteGroupPostById(postId: Int)

    @Query("DELETE FROM group_posts WHERE groupId = :groupId")
    suspend fun deleteAllGroupPosts(groupId: Int)

    // Group Post Comments Schema
    @Query("SELECT * FROM group_post_comments WHERE groupPostId = :groupPostId ORDER BY timestamp ASC")
    fun getGroupPostComments(groupPostId: Int): Flow<List<GroupPostComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupPostComment(comment: GroupPostComment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupPostComments(comments: List<GroupPostComment>)

    @Query("DELETE FROM group_post_comments WHERE id = :commentId")
    suspend fun deleteGroupPostCommentById(commentId: Int)

    @Query("DELETE FROM group_post_comments WHERE groupPostId = :groupPostId")
    suspend fun deleteAllGroupPostComments(groupPostId: Int)

    // Group Reports Schema
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupReport(report: GroupReport)

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

    @Query("DELETE FROM marketplace_items WHERE id = :itemId")
    suspend fun deleteMarketplaceItemById(itemId: Int)

    // Services Schema
    @Query("SELECT * FROM service_listings ORDER BY timestamp DESC")
    fun getAllServiceListings(): Flow<List<ServiceListing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceListing(listing: ServiceListing)

    @Query("DELETE FROM service_listings WHERE id = :listingId")
    suspend fun deleteServiceListingById(listingId: Int)

    // Stories Schema
    @Query("SELECT * FROM stories WHERE expiresAt > :currentTime ORDER BY timestamp ASC")
    fun getActiveStories(currentTime: Long): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: String): Story?

    @Query("SELECT * FROM stories WHERE authorId = :authorId AND expiresAt > :currentTime ORDER BY timestamp ASC")
    fun getStoriesByAuthor(authorId: String, currentTime: Long): Flow<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<Story>)

    @Update
    suspend fun updateStory(story: Story)

    @Query("DELETE FROM stories WHERE id = :storyId")
    suspend fun deleteStoryById(storyId: String)

    @Query("DELETE FROM stories WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredStories(currentTime: Long)

    @Query("UPDATE stories SET mediaUrl = :newUrl WHERE mediaUrl LIKE '%gtv-videos-bucket%' OR mediaUrl LIKE '%ForBiggerBlazes%'")
    suspend fun fixLegacyStoryVideoUrls(newUrl: String)

    // Friend Connections Schema
    @Query("SELECT * FROM friend_connections ORDER BY updatedAt DESC")
    fun getAllFriendConnections(): Flow<List<FriendConnection>>

    @Query("SELECT * FROM friend_connections WHERE senderId = :userId OR receiverId = :userId ORDER BY updatedAt DESC")
    fun getFriendConnectionsForUser(userId: String): Flow<List<FriendConnection>>

    @Query("SELECT * FROM friend_connections WHERE (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA) LIMIT 1")
    fun getFriendConnectionBetween(userA: String, userB: String): Flow<FriendConnection?>

    @Query("SELECT * FROM friend_connections WHERE (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA) LIMIT 1")
    suspend fun getFriendConnectionBetweenOnce(userA: String, userB: String): FriendConnection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendConnection(conn: FriendConnection)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendConnections(conns: List<FriendConnection>)

    @Update
    suspend fun updateFriendConnection(conn: FriendConnection)

    @Query("DELETE FROM friend_connections WHERE id = :connId")
    suspend fun deleteFriendConnectionById(connId: String)

    @Query("DELETE FROM friend_connections WHERE (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA)")
    suspend fun deleteFriendConnectionBetween(userA: String, userB: String)

    // Search users by query
    @Query("SELECT * FROM users WHERE fullName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR bio LIKE '%' || :query || '%' OR occupation LIKE '%' || :query || '%' OR location LIKE '%' || :query || '%' ORDER BY fullName ASC")
    fun searchUsers(query: String): Flow<List<User>>

    // Notifications Schema
    @Query("SELECT * FROM notifications WHERE recipientId = :userId OR recipientId = 'currentUser' OR recipientId = 'all' ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String): Flow<List<AppNotification>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<AppNotification>)

    @Update
    suspend fun updateNotification(notification: AppNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationAsRead(notificationId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientId = :userId OR recipientId = 'currentUser' OR recipientId = 'all'")
    suspend fun markAllNotificationsAsRead(userId: String)

    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: String)

    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()

    // Khowar Linguistic Dataset Schema
    @Query("SELECT * FROM khowar_dataset ORDER BY timestamp DESC")
    fun getAllKhowarDatasetEntries(): Flow<List<KhowarDatasetEntry>>

    @Query("SELECT * FROM khowar_dataset ORDER BY timestamp DESC")
    suspend fun getKhowarDatasetEntriesOnce(): List<KhowarDatasetEntry>

    @Query("SELECT * FROM khowar_dataset WHERE category = :category ORDER BY timestamp DESC")
    fun getKhowarDatasetByCategory(category: String): Flow<List<KhowarDatasetEntry>>

    @Query("SELECT * FROM khowar_dataset WHERE isVerified = 1 ORDER BY timestamp DESC")
    fun getVerifiedKhowarDataset(): Flow<List<KhowarDatasetEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhowarDatasetEntry(entry: KhowarDatasetEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhowarDatasetEntries(entries: List<KhowarDatasetEntry>)

    @Update
    suspend fun updateKhowarDatasetEntry(entry: KhowarDatasetEntry)

    @Query("DELETE FROM khowar_dataset WHERE id = :entryId")
    suspend fun deleteKhowarDatasetEntryById(entryId: String)

    @Query("SELECT * FROM khowar_dataset WHERE khowarText LIKE '%' || :query || '%' OR khowarRomanText LIKE '%' || :query || '%' OR urduTranslation LIKE '%' || :query || '%' OR englishTranslation LIKE '%' || :query || '%'")
    fun searchKhowarDataset(query: String): Flow<List<KhowarDatasetEntry>>

    // ==================== PAGES SYSTEM DAO ====================

    // Pages Queries
    @Query("SELECT * FROM pages ORDER BY followersCount DESC, createdAt DESC")
    fun getAllPages(): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE status = 'APPROVED' ORDER BY followersCount DESC, createdAt DESC")
    fun getApprovedPages(): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    fun getPageById(pageId: Int): Flow<Page?>

    @Query("SELECT * FROM pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageByIdOnce(pageId: Int): Page?

    @Query("SELECT * FROM pages WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getPageByUsernameOnce(username: String): Page?

    @Query("SELECT * FROM pages WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun getPagesByOwner(ownerId: String): Flow<List<Page>>

    @Query("""
        SELECT * FROM pages 
        WHERE (name LIKE '%' || :query || '%' 
           OR username LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%' 
           OR location LIKE '%' || :query || '%' 
           OR bio LIKE '%' || :query || '%')
        ORDER BY followersCount DESC
    """)
    fun searchPages(query: String): Flow<List<Page>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: Page): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<Page>)

    @Update
    suspend fun updatePage(page: Page)

    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: Int)

    // Page Members / Roles Queries
    @Query("SELECT * FROM page_members WHERE pageId = :pageId ORDER BY CASE role WHEN 'OWNER' THEN 1 WHEN 'ADMIN' THEN 2 ELSE 3 END, addedAt ASC")
    fun getPageMembers(pageId: Int): Flow<List<PageMember>>

    @Query("SELECT * FROM page_members WHERE pageId = :pageId AND userId = :userId LIMIT 1")
    suspend fun getPageMember(pageId: Int, userId: String): PageMember?

    @Query("SELECT * FROM page_members WHERE userId = :userId")
    fun getPagesManagedByUser(userId: String): Flow<List<PageMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageMember(member: PageMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageMembers(members: List<PageMember>)

    @Update
    suspend fun updatePageMember(member: PageMember)

    @Query("DELETE FROM page_members WHERE pageId = :pageId AND userId = :userId")
    suspend fun deletePageMember(pageId: Int, userId: String)

    @Query("DELETE FROM page_members WHERE pageId = :pageId")
    suspend fun deleteAllPageMembers(pageId: Int)

    // Page Followers Queries
    @Query("SELECT * FROM page_followers WHERE pageId = :pageId ORDER BY followedAt DESC")
    fun getPageFollowers(pageId: Int): Flow<List<PageFollower>>

    @Query("SELECT * FROM page_followers WHERE pageId = :pageId AND userId = :userId LIMIT 1")
    suspend fun getPageFollower(pageId: Int, userId: String): PageFollower?

    @Query("SELECT * FROM page_followers WHERE userId = :userId")
    fun getFollowedPagesForUser(userId: String): Flow<List<PageFollower>>

    @Query("SELECT COUNT(*) FROM page_followers WHERE pageId = :pageId")
    suspend fun getFollowerCount(pageId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageFollower(follower: PageFollower)

    @Query("DELETE FROM page_followers WHERE pageId = :pageId AND userId = :userId")
    suspend fun deletePageFollower(pageId: Int, userId: String)

    @Query("DELETE FROM page_followers WHERE pageId = :pageId")
    suspend fun deleteAllPageFollowers(pageId: Int)

    // Page Posts Queries
    @Query("SELECT * FROM page_posts WHERE pageId = :pageId ORDER BY isPinned DESC, timestamp DESC")
    fun getPagePosts(pageId: Int): Flow<List<PagePost>>

    @Query("SELECT * FROM page_posts ORDER BY timestamp DESC")
    fun getAllPagePosts(): Flow<List<PagePost>>

    @Query("SELECT * FROM page_posts WHERE id = :postId LIMIT 1")
    suspend fun getPagePostById(postId: Int): PagePost?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagePost(post: PagePost): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagePosts(posts: List<PagePost>)

    @Update
    suspend fun updatePagePost(post: PagePost)

    @Query("DELETE FROM page_posts WHERE id = :postId")
    suspend fun deletePagePostById(postId: Int)

    @Query("DELETE FROM page_posts WHERE pageId = :pageId")
    suspend fun deleteAllPagePosts(pageId: Int)

    // Page Post Comments Queries
    @Query("SELECT * FROM page_post_comments WHERE pagePostId = :pagePostId ORDER BY timestamp ASC")
    fun getPagePostComments(pagePostId: Int): Flow<List<PagePostComment>>

    @Query("SELECT * FROM page_post_comments WHERE pageId = :pageId ORDER BY timestamp DESC")
    fun getAllCommentsForPage(pageId: Int): Flow<List<PagePostComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagePostComment(comment: PagePostComment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPagePostComments(comments: List<PagePostComment>)

    @Update
    suspend fun updatePagePostComment(comment: PagePostComment)

    @Query("DELETE FROM page_post_comments WHERE id = :commentId")
    suspend fun deletePagePostCommentById(commentId: Int)

    @Query("DELETE FROM page_post_comments WHERE pagePostId = :pagePostId")
    suspend fun deleteAllPagePostComments(pagePostId: Int)

    @Query("DELETE FROM page_post_comments WHERE pageId = :pageId")
    suspend fun deleteAllCommentsForPage(pageId: Int)

    // Page Reports Queries
    @Query("SELECT * FROM page_reports ORDER BY timestamp DESC")
    fun getAllPageReports(): Flow<List<PageReport>>

    @Query("SELECT * FROM page_reports WHERE pageId = :pageId ORDER BY timestamp DESC")
    fun getPageReports(pageId: Int): Flow<List<PageReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageReport(report: PageReport)

    @Update
    suspend fun updatePageReport(report: PageReport)

    @Query("DELETE FROM page_reports WHERE id = :reportId")
    suspend fun deletePageReportById(reportId: Int)
}
