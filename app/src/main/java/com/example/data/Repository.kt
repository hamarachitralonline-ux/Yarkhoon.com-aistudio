package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class SocialMediaRepository(private val dao: SocialMediaDao) {

    val allPosts: Flow<List<Post>> = dao.getAllPosts()
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val currentUser: Flow<User?> = dao.getCurrentUser()
    val allGroups: Flow<List<Group>> = dao.getAllGroups()
    val allMarketplaceItems: Flow<List<MarketplaceItem>> = dao.getAllMarketplaceItems()
    val allServiceListings: Flow<List<ServiceListing>> = dao.getAllServiceListings()
    val allFriendConnections: Flow<List<FriendConnection>> = dao.getAllFriendConnections()
    val allNotifications: Flow<List<AppNotification>> = dao.getAllNotifications()

    // Local Room Cache Management (Offline-First Architecture)
    val cachedPostsCount: Flow<Int> = dao.getCachedPostsCount()
    val cachedUsersCount: Flow<Int> = dao.getCachedUsersCount()

    fun getCachedUserProfile(userId: String): Flow<User?> = dao.getUserByIdFlow(userId)
    fun getPostsByAuthor(authorId: String): Flow<List<Post>> = dao.getPostsByAuthor(authorId)
    fun searchPosts(query: String): Flow<List<Post>> = dao.searchPosts(query)
    suspend fun getPostById(postId: Int): Post? = dao.getPostById(postId)

    suspend fun cachePost(post: Post) = withContext(Dispatchers.IO) {
        dao.insertPost(post.copy(cachedAt = System.currentTimeMillis(), isCachedLocally = true))
    }

    suspend fun cachePosts(posts: List<Post>) = withContext(Dispatchers.IO) {
        dao.insertPosts(posts.map { it.copy(cachedAt = System.currentTimeMillis(), isCachedLocally = true) })
    }

    suspend fun cacheUser(user: User) = withContext(Dispatchers.IO) {
        dao.insertUser(user.copy(cachedAt = System.currentTimeMillis()))
    }

    suspend fun cacheUsers(users: List<User>) = withContext(Dispatchers.IO) {
        dao.insertUsers(users.map { it.copy(cachedAt = System.currentTimeMillis()) })
    }

    fun getNotificationsForUser(userId: String): Flow<List<AppNotification>> = dao.getNotificationsForUser(userId)
    suspend fun insertNotification(notification: AppNotification) = dao.insertNotification(notification)
    suspend fun insertNotifications(notifications: List<AppNotification>) = dao.insertNotifications(notifications)
    suspend fun updateNotification(notification: AppNotification) = dao.updateNotification(notification)
    suspend fun markNotificationAsRead(notificationId: String) = dao.markNotificationAsRead(notificationId)
    suspend fun markAllNotificationsAsRead(userId: String = "currentUser") = dao.markAllNotificationsAsRead(userId)
    suspend fun deleteNotification(notificationId: String) = dao.deleteNotificationById(notificationId)
    suspend fun deleteAllNotifications() = dao.deleteAllNotifications()

    fun getFriendConnectionsForUser(userId: String): Flow<List<FriendConnection>> =
        dao.getFriendConnectionsForUser(userId)

    fun getFriendConnectionBetween(userA: String, userB: String): Flow<FriendConnection?> =
        dao.getFriendConnectionBetween(userA, userB)

    suspend fun getFriendConnectionBetweenOnce(userA: String, userB: String): FriendConnection? =
        dao.getFriendConnectionBetweenOnce(userA, userB)

    suspend fun insertFriendConnection(conn: FriendConnection) = dao.insertFriendConnection(conn)
    suspend fun insertFriendConnections(conns: List<FriendConnection>) = dao.insertFriendConnections(conns)
    suspend fun updateFriendConnection(conn: FriendConnection) = dao.updateFriendConnection(conn)
    suspend fun deleteFriendConnection(connId: String) = dao.deleteFriendConnectionById(connId)
    suspend fun deleteFriendConnectionBetween(userA: String, userB: String) = dao.deleteFriendConnectionBetween(userA, userB)

    fun searchUsers(query: String): Flow<List<User>> = dao.searchUsers(query)

    fun getActiveStories(currentTime: Long = System.currentTimeMillis()): Flow<List<Story>> =
        dao.getActiveStories(currentTime)

    fun getStoriesByAuthor(authorId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<Story>> =
        dao.getStoriesByAuthor(authorId, currentTime)

    suspend fun getStoryById(storyId: String): Story? = dao.getStoryById(storyId)
    suspend fun insertStory(story: Story) = dao.insertStory(story)
    suspend fun insertStories(stories: List<Story>) = dao.insertStories(stories)
    suspend fun updateStory(story: Story) = dao.updateStory(story)
    suspend fun deleteStory(storyId: String) = dao.deleteStoryById(storyId)
    suspend fun deleteExpiredStories(currentTime: Long = System.currentTimeMillis()) = dao.deleteExpiredStories(currentTime)

    fun getChatMessages(myId: String, theirId: String): Flow<List<ChatMessage>> =
        dao.getChatMessages(myId, theirId)

    suspend fun insertPost(post: Post) = dao.insertPost(post)
    suspend fun updatePost(post: Post) = dao.updatePost(post)
    suspend fun deletePost(postId: Int) {
        dao.deletePostById(postId)
        dao.deleteAllPostComments(postId)
        dao.deleteAllReactionsForPost(postId)
    }

    // Post Reactions & Sentiments
    val allPostReactions: Flow<List<PostReaction>> = dao.getAllPostReactions()
    fun getReactionsForPost(postId: Int): Flow<List<PostReaction>> = dao.getReactionsForPost(postId)
    suspend fun getPostReaction(postId: Int, userId: String): PostReaction? = dao.getPostReaction(postId, userId)
    suspend fun setPostReaction(postId: Int, user: User, reactionType: String) {
        val reaction = PostReaction(
            postId = postId,
            userId = user.id,
            userName = user.fullName,
            userAvatarUrl = user.avatarUrl,
            reactionType = reactionType,
            timestamp = System.currentTimeMillis()
        )
        dao.insertPostReaction(reaction)
    }
    suspend fun removePostReaction(postId: Int, userId: String) {
        dao.deletePostReaction(postId, userId)
    }

    // Post Comments
    fun getPostComments(postId: Int): Flow<List<PostComment>> = dao.getPostComments(postId)
    fun getAllPostComments(): Flow<List<PostComment>> = dao.getAllPostComments()
    suspend fun getPostCommentById(commentId: Int): PostComment? = dao.getPostCommentById(commentId)
    suspend fun insertPostComment(comment: PostComment): Long = dao.insertPostComment(comment)
    suspend fun insertPostComments(comments: List<PostComment>) = dao.insertPostComments(comments)
    suspend fun updatePostComment(comment: PostComment) = dao.updatePostComment(comment)
    suspend fun deletePostComment(commentId: Int) = dao.deletePostCommentById(commentId)
    suspend fun deleteAllPostComments(postId: Int) = dao.deleteAllPostComments(postId)

    // Comment Likes & Reports
    suspend fun insertCommentLike(like: CommentLike) = dao.insertCommentLike(like)
    suspend fun deleteCommentLike(commentId: Int, userId: String) = dao.deleteCommentLike(commentId, userId)
    fun getCommentLikesForUser(userId: String): Flow<List<CommentLike>> = dao.getCommentLikesForUser(userId)
    suspend fun insertCommentReport(report: CommentReport) = dao.insertCommentReport(report)
    fun getAllCommentReports(): Flow<List<CommentReport>> = dao.getAllCommentReports()

    suspend fun updateUser(user: User) = dao.updateUser(user)
    suspend fun insertUsers(users: List<User>) = dao.insertUsers(users)

    suspend fun deleteUser(userId: String) = dao.deleteUserById(userId)

    suspend fun insertChatMessage(message: ChatMessage) = dao.insertChatMessage(message)

    suspend fun insertGroups(groups: List<Group>) = dao.insertGroups(groups)
    suspend fun insertGroup(group: Group): Long = dao.insertGroup(group)
    suspend fun updateGroup(group: Group) = dao.updateGroup(group)
    suspend fun deleteGroup(groupId: Int) {
        dao.deleteGroupById(groupId)
        dao.deleteAllGroupMembers(groupId)
        dao.deleteAllGroupPosts(groupId)
        dao.deleteAllGroupJoinRequests(groupId)
    }
    fun getGroupById(groupId: Int): Flow<Group?> = dao.getGroupById(groupId)
    suspend fun getGroupByIdOnce(groupId: Int): Group? = dao.getGroupByIdOnce(groupId)

    // Group Members
    fun getGroupMembers(groupId: Int): Flow<List<GroupMember>> = dao.getGroupMembers(groupId)
    suspend fun getGroupMember(groupId: Int, userId: String): GroupMember? = dao.getGroupMember(groupId, userId)
    suspend fun insertGroupMember(member: GroupMember) = dao.insertGroupMember(member)
    suspend fun updateGroupMember(member: GroupMember) = dao.updateGroupMember(member)
    suspend fun deleteGroupMember(groupId: Int, userId: String) = dao.deleteGroupMember(groupId, userId)

    // Group Join Requests
    fun getGroupJoinRequests(groupId: Int): Flow<List<GroupJoinRequest>> = dao.getGroupJoinRequests(groupId)
    suspend fun insertGroupJoinRequest(req: GroupJoinRequest) = dao.insertGroupJoinRequest(req)
    suspend fun updateGroupJoinRequest(req: GroupJoinRequest) = dao.updateGroupJoinRequest(req)
    suspend fun deleteGroupJoinRequest(requestId: Int) = dao.deleteGroupJoinRequestById(requestId)

    // Group Invites
    fun getGroupInvitesForUser(userId: String): Flow<List<GroupInvite>> = dao.getGroupInvitesForUser(userId)
    suspend fun insertGroupInvite(invite: GroupInvite) = dao.insertGroupInvite(invite)
    suspend fun updateGroupInvite(invite: GroupInvite) = dao.updateGroupInvite(invite)
    suspend fun deleteGroupInvite(inviteId: Int) = dao.deleteGroupInviteById(inviteId)

    // Group Posts
    fun getGroupPosts(groupId: Int): Flow<List<GroupPost>> = dao.getGroupPosts(groupId)
    suspend fun insertGroupPost(post: GroupPost) = dao.insertGroupPost(post)
    suspend fun updateGroupPost(post: GroupPost) = dao.updateGroupPost(post)
    suspend fun deleteGroupPost(postId: Int) {
        dao.deleteGroupPostById(postId)
        dao.deleteAllGroupPostComments(postId)
    }

    // Group Post Comments
    fun getGroupPostComments(groupPostId: Int): Flow<List<GroupPostComment>> = dao.getGroupPostComments(groupPostId)
    suspend fun insertGroupPostComment(comment: GroupPostComment) = dao.insertGroupPostComment(comment)
    suspend fun deleteGroupPostComment(commentId: Int) = dao.deleteGroupPostCommentById(commentId)

    // Group Reports
    suspend fun insertGroupReport(report: GroupReport) = dao.insertGroupReport(report)

    suspend fun insertMarketplaceItem(item: MarketplaceItem) = dao.insertMarketplaceItem(item)
    suspend fun updateMarketplaceItem(item: MarketplaceItem) = dao.updateMarketplaceItem(item)
    suspend fun deleteMarketplaceItem(itemId: Int) = dao.deleteMarketplaceItemById(itemId)

    suspend fun insertServiceListing(listing: ServiceListing) = dao.insertServiceListing(listing)
    suspend fun deleteServiceListing(listingId: Int) = dao.deleteServiceListingById(listingId)

    // ==================== PAGES SYSTEM REPOSITORY ====================
    val allPages: Flow<List<Page>> = dao.getAllPages()
    val approvedPages: Flow<List<Page>> = dao.getApprovedPages()
    val allPagePosts: Flow<List<PagePost>> = dao.getAllPagePosts()
    val allPageReports: Flow<List<PageReport>> = dao.getAllPageReports()

    fun getPageById(id: Int): Flow<Page?> = dao.getPageById(id)
    suspend fun getPageByIdOnce(id: Int): Page? = dao.getPageByIdOnce(id)
    suspend fun getPageByUsernameOnce(username: String): Page? = dao.getPageByUsernameOnce(username)
    fun getPagesByOwner(ownerId: String): Flow<List<Page>> = dao.getPagesByOwner(ownerId)
    fun searchPages(query: String): Flow<List<Page>> = dao.searchPages(query)

    suspend fun insertPage(page: Page): Long = dao.insertPage(page)
    suspend fun updatePage(page: Page) = dao.updatePage(page)
    suspend fun deletePage(pageId: Int) {
        dao.deletePageById(pageId)
        dao.deleteAllPageMembers(pageId)
        dao.deleteAllPageFollowers(pageId)
        dao.deleteAllPagePosts(pageId)
        dao.deleteAllCommentsForPage(pageId)
    }

    // Page Members
    fun getPageMembers(pageId: Int): Flow<List<PageMember>> = dao.getPageMembers(pageId)
    suspend fun getPageMember(pageId: Int, userId: String): PageMember? = dao.getPageMember(pageId, userId)
    fun getPagesManagedByUser(userId: String): Flow<List<PageMember>> = dao.getPagesManagedByUser(userId)
    suspend fun insertPageMember(member: PageMember) = dao.insertPageMember(member)
    suspend fun updatePageMember(member: PageMember) = dao.updatePageMember(member)
    suspend fun deletePageMember(pageId: Int, userId: String) = dao.deletePageMember(pageId, userId)

    // Page Followers
    fun getPageFollowers(pageId: Int): Flow<List<PageFollower>> = dao.getPageFollowers(pageId)
    suspend fun getPageFollower(pageId: Int, userId: String): PageFollower? = dao.getPageFollower(pageId, userId)
    fun getFollowedPagesForUser(userId: String): Flow<List<PageFollower>> = dao.getFollowedPagesForUser(userId)
    suspend fun insertPageFollower(follower: PageFollower) = dao.insertPageFollower(follower)
    suspend fun deletePageFollower(pageId: Int, userId: String) = dao.deletePageFollower(pageId, userId)
    suspend fun getPageFollowerCount(pageId: Int): Int = dao.getFollowerCount(pageId)

    // Page Posts
    fun getPagePosts(pageId: Int): Flow<List<PagePost>> = dao.getPagePosts(pageId)
    suspend fun getPagePostById(postId: Int): PagePost? = dao.getPagePostById(postId)
    suspend fun insertPagePost(post: PagePost): Long = dao.insertPagePost(post)
    suspend fun updatePagePost(post: PagePost) = dao.updatePagePost(post)
    suspend fun deletePagePost(postId: Int) {
        dao.deletePagePostById(postId)
        dao.deleteAllPagePostComments(postId)
    }

    // Page Post Comments
    fun getPagePostComments(pagePostId: Int): Flow<List<PagePostComment>> = dao.getPagePostComments(pagePostId)
    fun getAllCommentsForPage(pageId: Int): Flow<List<PagePostComment>> = dao.getAllCommentsForPage(pageId)
    suspend fun insertPagePostComment(comment: PagePostComment) = dao.insertPagePostComment(comment)
    suspend fun updatePagePostComment(comment: PagePostComment) = dao.updatePagePostComment(comment)
    suspend fun deletePagePostComment(commentId: Int) = dao.deletePagePostCommentById(commentId)

    // Page Reports
    suspend fun insertPageReport(report: PageReport) = dao.insertPageReport(report)
    suspend fun updatePageReport(report: PageReport) = dao.updatePageReport(report)
    suspend fun deletePageReport(reportId: Int) = dao.deletePageReportById(reportId)

    // Check if empty and prepopulate with realistic social media data
    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        // Always fix any legacy defunct URLs in posts and stories
        try {
            dao.fixLegacyVideoUrls("https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/big_buck_bunny.mp4")
            dao.fixLegacyStoryVideoUrls("https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/echo-hereweare.mp4")
        } catch (_: Exception) {}

        val allUsers = dao.getAllUsers().first()
        if (allUsers.isEmpty()) {
            val defaultCurrentUser = User(
                id = "currentUser",
                username = "",
                fullName = "",
                avatarUrl = "",
                coverUrl = "",
                bio = "",
                friendStatus = "NONE",
                isCurrentUser = true,
                isProfileCompleted = false,
                email = "",
                password = "",
                isVerified = false
            )

            val initialUsers = listOf(
                defaultCurrentUser,
                User(
                    id = "user_yarkhoon",
                    username = "yarkhoon",
                    fullName = "Yarkhoon.com",
                    avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=800&auto=format&fit=crop",
                    bio = "Connecting & empowering Chitral. The official verified profile of Hamara Yarkhoon 🏔️✨. Follow us to configure and secure local networks.",
                    friendStatus = "FRIENDS",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "yarkhoon@yarkhoon.com",
                    password = "password123",
                    isVerified = true
                ),
                User(
                    id = "user_ali",
                    username = "alikhan99",
                    fullName = "Ali Khan",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=500&auto=format&fit=crop",
                    bio = "Tech enthusiast, Android developer, and hiker. Let's build community tech solutions!",
                    friendStatus = "FRIENDS",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "ali@yarkhoon.com",
                    password = "password123",
                    isVerified = false
                ),
                User(
                    id = "user_zara",
                    username = "zarashah",
                    fullName = "Zara Shah",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=500&auto=format&fit=crop",
                    bio = "Founder of Valley Threads. Empowering female weavers & sharing Chitrali walnut woodwork globally 🧵✨",
                    friendStatus = "FRIENDS",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = false,
                    email = "zara@yarkhoon.com",
                    password = "password123",
                    isVerified = false
                ),
                User(
                    id = "user_sher",
                    username = "sherfilm",
                    fullName = "Sher Jang",
                    avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=500&auto=format&fit=crop",
                    bio = "Filmmaker & mountaineer. Documenting untouched glaciers and peaks of High Yarkhoon Valley 🏔️🎥",
                    friendStatus = "FRIENDS",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "sher@yarkhoon.com",
                    password = "password123",
                    isVerified = false
                ),
                User(
                    id = "user_shazia",
                    username = "shazia_edu",
                    fullName = "Shazia Parveen",
                    avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=500&auto=format&fit=crop",
                    bio = "Teacher at Yarkhoon Public Academy. Passionate about literacy, poetry, and children's welfare.",
                    friendStatus = "NONE",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = false,
                    email = "shazia@yarkhoon.com",
                    password = "password123",
                    isVerified = false,
                    location = "Yarkhoon Valley",
                    occupation = "Educator & Writer",
                    mutualFriendsCount = 4
                ),
                User(
                    id = "user_tariq",
                    username = "tariq_guide",
                    fullName = "Tariq Aziz",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=500&auto=format&fit=crop",
                    bio = "Licensed mountain trek guide for Broghil National Park, Karambar Lake & Darkot Pass 🧗‍♂️⛺",
                    friendStatus = "RECEIVED",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "tariq@yarkhoon.com",
                    password = "password123",
                    isVerified = true,
                    location = "Mastuj, Chitral",
                    occupation = "Alpine Trekking Guide",
                    mutualFriendsCount = 7
                ),
                User(
                    id = "user_farhana",
                    username = "dr_farhana",
                    fullName = "Dr. Farhana Begum",
                    avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1519494026892-80bbd2d6fd0d?w=500&auto=format&fit=crop",
                    bio = "Community health physician serving Upper Chitral. Dedicated to maternal healthcare in remote valleys 🩺❤️",
                    friendStatus = "NONE",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = false,
                    email = "farhana@yarkhoon.com",
                    password = "password123",
                    isVerified = true,
                    location = "Booni, Upper Chitral",
                    occupation = "Medical Doctor",
                    mutualFriendsCount = 3
                ),
                User(
                    id = "user_sultan",
                    username = "sultan_heritage",
                    fullName = "Sultan Mehmood",
                    avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=500&auto=format&fit=crop",
                    bio = "Khowar linguistic researcher and folklorist. Preserving indigenous Chitrali stories and music 🎻📚",
                    friendStatus = "SENT",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "sultan@yarkhoon.com",
                    password = "password123",
                    isVerified = false,
                    location = "Chitral Town",
                    occupation = "Cultural Historian",
                    mutualFriendsCount = 5
                ),
                User(
                    id = "user_amina",
                    username = "amina_crafts",
                    fullName = "Amina Bibi",
                    avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1528459801416-a9e53bbf4e17?w=500&auto=format&fit=crop",
                    bio = "Master artisan of Chitrali embroidery, woolen caps (Pakol), and organic walnut oil 🌿🧵",
                    friendStatus = "NONE",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = false,
                    email = "amina@yarkhoon.com",
                    password = "password123",
                    isVerified = false,
                    location = "Yarkhoon Lasht",
                    occupation = "Artisan & Entrepreneur",
                    mutualFriendsCount = 2
                ),
                User(
                    id = "user_rashid",
                    username = "rashid_solar",
                    fullName = "Rashid Minhas",
                    avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1509391365360-2e959784a276?w=500&auto=format&fit=crop",
                    bio = "Micro-hydro & solar energy specialist installing clean off-grid power across Hindu Kush ☀️⚡",
                    friendStatus = "NONE",
                    isCurrentUser = false,
                    isProfileCompleted = true,
                    isOnline = true,
                    email = "rashid@yarkhoon.com",
                    password = "password123",
                    isVerified = true,
                    location = "Garam Chashma, Chitral",
                    occupation = "Renewable Energy Engineer",
                    mutualFriendsCount = 6
                )
            )
            dao.insertUsers(initialUsers)

            // Seed Initial Friend Connections
            val initialConnections = listOf(
                FriendConnection(
                    id = "req_user_ali_currentUser",
                    senderId = "user_ali",
                    senderName = "Ali Khan",
                    senderUsername = "alikhan99",
                    senderAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    receiverId = "currentUser",
                    receiverName = "Hamara Chitral",
                    status = "ACCEPTED",
                    introMessage = "Salam! Delighted to connect on Yarkhoon.com!",
                    createdAt = System.currentTimeMillis() - 86400000L * 5,
                    updatedAt = System.currentTimeMillis() - 86400000L * 4,
                    isSyncedWithFirestore = true
                ),
                FriendConnection(
                    id = "req_user_zara_currentUser",
                    senderId = "user_zara",
                    senderName = "Zara Shah",
                    senderUsername = "zarashah",
                    senderAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    receiverId = "currentUser",
                    receiverName = "Hamara Chitral",
                    status = "ACCEPTED",
                    introMessage = "Hello from Valley Threads! Looking forward to trading and sharing.",
                    createdAt = System.currentTimeMillis() - 86400000L * 3,
                    updatedAt = System.currentTimeMillis() - 86400000L * 2,
                    isSyncedWithFirestore = true
                ),
                FriendConnection(
                    id = "req_user_tariq_currentUser",
                    senderId = "user_tariq",
                    senderName = "Tariq Aziz",
                    senderUsername = "tariq_guide",
                    senderAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    receiverId = "currentUser",
                    receiverName = "Hamara Chitral",
                    status = "PENDING",
                    introMessage = "Salam! Saw your posts on Chitral treks. Let's stay connected for alpine expeditions.",
                    createdAt = System.currentTimeMillis() - 3600000L * 4,
                    updatedAt = System.currentTimeMillis() - 3600000L * 4,
                    isSyncedWithFirestore = true
                ),
                FriendConnection(
                    id = "req_currentUser_user_sultan",
                    senderId = "currentUser",
                    senderName = "Hamara Chitral",
                    senderAvatarUrl = "",
                    receiverId = "user_sultan",
                    receiverName = "Sultan Mehmood",
                    receiverUsername = "sultan_heritage",
                    receiverAvatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150",
                    status = "PENDING",
                    introMessage = "Salam Sultan Sahib! Love your historical research on Khowar traditions.",
                    createdAt = System.currentTimeMillis() - 3600000L * 12,
                    updatedAt = System.currentTimeMillis() - 3600000L * 12,
                    isSyncedWithFirestore = true
                )
            )
            dao.insertFriendConnections(initialConnections)

            // Feed posts
            val initialPosts = listOf(
                Post(
                    authorId = "user_yarkhoon",
                    authorName = "Yarkhoon.com",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                    content = "Welcome Chitral and Yarkhoon Valley! We are proud to present our verified social portal. From this dashboard and social feed, you can explore traditional threads, coordinate alpine treks near Broghil Pass, and safely establish community connections. Together, let's bridge our gorgeous mountain valleys with tech and sustainable trade! 🏔️✨",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1454496522488-7a8e488e8606?w=800&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 1800000, // 30 mins ago
                    likesCount = 1420,
                    isLikedByMe = true,
                    commentsCount = 284,
                    isViral = true
                ),
                Post(
                    authorId = "user_sher",
                    authorName = "Sher Jang",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    content = "Exploring the upper heights of Yarkhoon Valley near the Broghil Pass! Check out this short video update of our trek. The scenery is absolutely breathtaking. Hindukush holds so many hidden wonders!",
                    mediaType = "VIDEO",
                    mediaUrl = "https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/big_buck_bunny.mp4",
                    timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                    likesCount = 34,
                    isLikedByMe = true,
                    commentsCount = 8,
                    isViral = false
                ),
                Post(
                    authorId = "user_zara",
                    authorName = "Zara Shah",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    content = "Proudly launching our premium handwoven winter woolen blankets (Shu) on the yarkhoon.com Marketplace! Check the Marketplace tab for more details. Supporting hand-weaving families directly in our valley.",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1580301762395-21ce84d00bc6?w=600&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                    likesCount = 28,
                    isLikedByMe = false,
                    commentsCount = 3
                ),
                Post(
                    authorId = "user_shazia",
                    authorName = "Shazia Parveen",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop",
                    content = "Today in the Yarkhoon Public Academy library, we received new textbooks donated by the online community! Thank you all who participated in our book drive. Education is the ultimate bridge to a brighter future. 📚🎓",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 14400000, // 4 hours ago
                    likesCount = 42,
                    isLikedByMe = false,
                    commentsCount = 12
                ),
                Post(
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    content = "Hello world! Welcome to yarkhoon.com - our customized social network. Built this platform so we can connect, post, share hiking groups, trade items locally, and build secure communication pathways for everyone in the valley and beyond! Let me know if you want to collaborate on open-source code.",
                    mediaType = "NONE",
                    mediaUrl = "",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                    likesCount = 95,
                    isLikedByMe = true,
                    commentsCount = 24
                )
            )
            for (post in initialPosts) {
                dao.insertPost(post)
            }

            // Seed initial interactive Post Comments
            val initialPostComments = listOf(
                PostComment(
                    id = 1,
                    postId = 1,
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    content = "The autumn colors around Yarkhoon river are breathtaking this season! SubhanAllah 🏔️🍂",
                    likesCount = 5,
                    isLikedByMe = true,
                    timestamp = System.currentTimeMillis() - 7000000
                ),
                PostComment(
                    id = 2,
                    postId = 1,
                    authorId = "user_zara",
                    authorName = "Zara Shah",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    content = "Which trail did you take to get this angle? Looks like above Meragram village!",
                    parentCommentId = 1,
                    replyToAuthorName = "Ali Khan",
                    likesCount = 2,
                    timestamp = System.currentTimeMillis() - 6000000
                ),
                PostComment(
                    id = 3,
                    postId = 1,
                    authorId = "user_rashid",
                    authorName = "Rashid Minhas",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150",
                    content = "Yes Zara, that is indeed above Meragram towards Brep! Spectacular view.",
                    parentCommentId = 1,
                    replyToAuthorName = "Zara Shah",
                    likesCount = 1,
                    timestamp = System.currentTimeMillis() - 5000000
                ),
                PostComment(
                    id = 4,
                    postId = 1,
                    authorId = "user_sher",
                    authorName = "Sher Jang",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150",
                    content = "Planning our next high altitude photography tour there next week. Great capture!",
                    likesCount = 4,
                    timestamp = System.currentTimeMillis() - 4000000
                ),
                PostComment(
                    id = 5,
                    postId = 2,
                    authorId = "user_farhan",
                    authorName = "Farhan Ahmad",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                    content = "These handwoven woolen caps and patti shawls are genuine treasures of Chitral artisan heritage.",
                    likesCount = 3,
                    timestamp = System.currentTimeMillis() - 8000000
                ),
                PostComment(
                    id = 6,
                    postId = 3,
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    content = "Incredible initiative Shazia! The community is proud of your educational contribution.",
                    likesCount = 6,
                    timestamp = System.currentTimeMillis() - 3000000
                ),
                PostComment(
                    id = 7,
                    postId = 4,
                    authorId = "user_zara",
                    authorName = "Zara Shah",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    content = "Welcome everyone to Yarkhoon.com! Excited to connect with all of you.",
                    likesCount = 9,
                    isLikedByMe = true,
                    timestamp = System.currentTimeMillis() - 20000000
                )
            )
            dao.insertPostComments(initialPostComments)

            // Seed initial Post Reactions across diverse sentiments
            val initialPostReactions = listOf(
                PostReaction(
                    postId = 1,
                    userId = "currentUser",
                    userName = "Me",
                    userAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    reactionType = "LOVE",
                    timestamp = System.currentTimeMillis() - 1000000
                ),
                PostReaction(
                    postId = 1,
                    userId = "user_ali",
                    userName = "Ali Khan",
                    userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    reactionType = "LIKE",
                    timestamp = System.currentTimeMillis() - 2000000
                ),
                PostReaction(
                    postId = 1,
                    userId = "user_zara",
                    userName = "Zara Shah",
                    userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    reactionType = "CARE",
                    timestamp = System.currentTimeMillis() - 3000000
                ),
                PostReaction(
                    postId = 1,
                    userId = "user_rashid",
                    userName = "Rashid Minhas",
                    userAvatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150",
                    reactionType = "WOW",
                    timestamp = System.currentTimeMillis() - 4000000
                ),
                PostReaction(
                    postId = 2,
                    userId = "user_farhan",
                    userName = "Farhan Ahmad",
                    userAvatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                    reactionType = "LOVE",
                    timestamp = System.currentTimeMillis() - 5000000
                ),
                PostReaction(
                    postId = 2,
                    userId = "user_ali",
                    userName = "Ali Khan",
                    userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    reactionType = "LIKE",
                    timestamp = System.currentTimeMillis() - 6000000
                ),
                PostReaction(
                    postId = 3,
                    userId = "user_zara",
                    userName = "Zara Shah",
                    userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    reactionType = "HAHA",
                    timestamp = System.currentTimeMillis() - 7000000
                )
            )
            dao.insertPostReactions(initialPostReactions)

            // Groups
            val initialGroups = listOf(
                Group(
                    id = 1,
                    name = "Hindukush & Yarkhoon Explorers",
                    description = "A community for mountain trekkers, hikers, climbers, and landscape photographers coordinating expeditions across Yarkhoon Valley & Broghil Pass.",
                    avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop",
                    category = "Active & Travel",
                    location = "Yarkhoon & Broghil, Chitral",
                    isPrivate = false,
                    onlyAdminsCanPost = false,
                    ownerId = "currentUser",
                    memberCount = 1240,
                    isJoined = true,
                    joinStatus = "JOINED"
                ),
                Group(
                    id = 2,
                    name = "Chitral Artisan & Trade Guild",
                    description = "Private cooperative network for Chitrali handwoven woolen artisans (Shu, Patti), pakol makers, and dried fruit exporters.",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1501183007986-d0d080b147f9?w=800&auto=format&fit=crop",
                    category = "Marketplace & Trade",
                    location = "Chitral Bazaar, KP",
                    isPrivate = true,
                    onlyAdminsCanPost = false,
                    ownerId = "user_zara",
                    memberCount = 385,
                    isJoined = false,
                    joinStatus = "NONE"
                ),
                Group(
                    id = 3,
                    name = "Khowar Language & Culture Preservation",
                    description = "Preserving ancient Chitrali folk poetry, traditional sitar tunes, Khowar vocabulary, historic folklore, and cultural festivals.",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=800&auto=format&fit=crop",
                    category = "Art & Culture",
                    location = "Chitral & Global Diaspora",
                    isPrivate = false,
                    onlyAdminsCanPost = false,
                    ownerId = "user_ali",
                    memberCount = 670,
                    isJoined = true,
                    joinStatus = "JOINED"
                ),
                Group(
                    id = 4,
                    name = "Hindukush Tech & Coding Circle",
                    description = "Official hub for Chitrali developers, software engineers, Kotlin builders, and youth learning modern AI and web development.",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?w=800&auto=format&fit=crop",
                    category = "Education & Tech",
                    location = "Booni & Mastuj Hub",
                    isPrivate = false,
                    onlyAdminsCanPost = false,
                    ownerId = "user_ali",
                    memberCount = 420,
                    isJoined = false,
                    joinStatus = "NONE"
                ),
                Group(
                    id = 5,
                    name = "Chitral Youth Cricket League",
                    description = "Private group for tournament schedules, team registrations, player stats, and match highlights for Upper & Lower Chitral cricket leagues.",
                    avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    coverUrl = "https://images.unsplash.com/photo-1531415074868-036b1c5d53ec?w=800&auto=format&fit=crop",
                    category = "Sports & Fitness",
                    location = "Chitral Town Stadium",
                    isPrivate = true,
                    onlyAdminsCanPost = true,
                    ownerId = "user_sher",
                    memberCount = 180,
                    isJoined = false,
                    joinStatus = "NONE"
                )
            )
            dao.insertGroups(initialGroups)

            // Initial Group Members
            val initialMembers = listOf(
                GroupMember(groupId = 1, userId = "currentUser", userName = "CurrentUser", userAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150", role = "OWNER"),
                GroupMember(groupId = 1, userId = "user_sher", userName = "Sher Jang", userAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150", role = "ADMIN"),
                GroupMember(groupId = 1, userId = "user_ali", userName = "Ali Khan", userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", role = "MODERATOR"),
                GroupMember(groupId = 1, userId = "user_zara", userName = "Zara Shah", userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", role = "MEMBER"),
                GroupMember(groupId = 1, userId = "user_shazia", userName = "Shazia Parveen", userAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150", role = "MEMBER"),
                
                GroupMember(groupId = 2, userId = "user_zara", userName = "Zara Shah", userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", role = "OWNER"),
                GroupMember(groupId = 2, userId = "user_ali", userName = "Ali Khan", userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", role = "ADMIN"),

                GroupMember(groupId = 3, userId = "user_ali", userName = "Ali Khan", userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", role = "OWNER"),
                GroupMember(groupId = 3, userId = "currentUser", userName = "CurrentUser", userAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150", role = "MEMBER"),
                GroupMember(groupId = 3, userId = "user_shazia", userName = "Shazia Parveen", userAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150", role = "MODERATOR")
            )
            dao.insertGroupMembers(initialMembers)

            // Initial Group Posts
            val initialGroupPosts = listOf(
                GroupPost(
                    id = 1,
                    groupId = 1,
                    authorId = "currentUser",
                    authorName = "Community Admin",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    authorRole = "OWNER",
                    content = "📌 [PINNED NOTICE] Welcome to all adventurers and mountaineers joining Hindukush & Yarkhoon Explorers! Please review our safety guidelines when planning treks towards Karambar Lake, Broghil National Park, or Darkot Pass. Ensure you travel with local registered guides.",
                    mediaType = "IMAGE",
                    mediaUrlsJson = "[\"https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800\"]",
                    location = "Yarkhoon Valley HQ",
                    isPinned = true,
                    likesCount = 48,
                    isLikedByMe = true,
                    commentsCount = 6,
                    timestamp = System.currentTimeMillis() - 7200000
                ),
                GroupPost(
                    id = 2,
                    groupId = 1,
                    authorId = "user_sher",
                    authorName = "Sher Jang",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150",
                    authorRole = "ADMIN",
                    content = "Trek group departure scheduled this Saturday from Mastuj to Shost and Gazin! We have 4 open slots for photography enthusiasts. DM or comment below if you want to join our gear checklist briefing.",
                    mediaType = "IMAGE",
                    mediaUrlsJson = "[\"https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800\"]",
                    location = "Mastuj to Shost",
                    isPinned = false,
                    likesCount = 29,
                    isLikedByMe = false,
                    commentsCount = 4,
                    timestamp = System.currentTimeMillis() - 3600000
                ),
                GroupPost(
                    id = 3,
                    groupId = 3,
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    authorRole = "OWNER",
                    content = "Shared an audio clip of traditional Chitrali sitar playing the melody of 'Dani' recorded in upper Booni. Preserving these historic rhythms for the next generation! 🎶🏔️",
                    mediaType = "NONE",
                    mediaUrlsJson = "[]",
                    location = "Booni, Upper Chitral",
                    isPinned = false,
                    likesCount = 37,
                    isLikedByMe = true,
                    commentsCount = 3,
                    timestamp = System.currentTimeMillis() - 5400000
                )
            )
            dao.insertGroupPosts(initialGroupPosts)

            // Initial Group Post Comments
            val initialGroupComments = listOf(
                GroupPostComment(
                    groupPostId = 1,
                    authorId = "user_sher",
                    authorName = "Sher Jang",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150",
                    content = "Excellent rules! We should also remind travelers to keep the trails clean and carry reusable water bottles.",
                    timestamp = System.currentTimeMillis() - 5000000
                ),
                GroupPostComment(
                    groupPostId = 1,
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    content = "Agreed Sher! Let's schedule a community clean-up drive next month.",
                    parentCommentId = 1,
                    replyToAuthorName = "Sher Jang",
                    timestamp = System.currentTimeMillis() - 4000000
                ),
                GroupPostComment(
                    groupPostId = 2,
                    authorId = "user_zara",
                    authorName = "Zara Shah",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    content = "I would love to join! Count me in for the photography gear checklist.",
                    timestamp = System.currentTimeMillis() - 2000000
                )
            )
            dao.insertGroupPostComments(initialGroupComments)

            // Seed Join Requests for Group 2 & Group 5 (Private groups)
            val initialJoinRequests = listOf(
                GroupJoinRequest(
                    groupId = 1,
                    groupName = "Hindukush & Yarkhoon Explorers",
                    userId = "user_rashid",
                    userName = "Rashid Minhas",
                    userAvatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150",
                    userBio = "Local trek guide & mountain photographer from Garam Chashma.",
                    timestamp = System.currentTimeMillis() - 1800000
                ),
                GroupJoinRequest(
                    groupId = 1,
                    groupName = "Hindukush & Yarkhoon Explorers",
                    userId = "user_farhan",
                    userName = "Farhan Ahmad",
                    userAvatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=150",
                    userBio = "Outdoor camper & drone pilot based in Chitral Town.",
                    timestamp = System.currentTimeMillis() - 900000
                )
            )
            for (req in initialJoinRequests) {
                dao.insertGroupJoinRequest(req)
            }

            // Seed Group Invites for currentUser
            val initialInvites = listOf(
                GroupInvite(
                    groupId = 2,
                    groupName = "Chitral Artisan & Trade Guild",
                    groupAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    inviterId = "user_zara",
                    inviterName = "Zara Shah",
                    inviteeId = "currentUser",
                    timestamp = System.currentTimeMillis() - 3600000
                )
            )
            for (inv in initialInvites) {
                dao.insertGroupInvite(inv)
            }

            // Chat initial messages
            val chatPartner = "user_ali"
            val initialMessages = listOf(
                ChatMessage(
                    senderId = chatPartner,
                    receiverId = "currentUser",
                    content = "Hey! This app looks fantastic. Is the private chat working smoothly?",
                    timestamp = System.currentTimeMillis() - 36000000 // 10 hours ago
                ),
                ChatMessage(
                    senderId = "currentUser",
                    receiverId = chatPartner,
                    content = "Yes, Ali! It connects directly to our local Room database. You can send messages anytime.",
                    timestamp = System.currentTimeMillis() - 32000000 // 9 hours ago
                ),
                ChatMessage(
                    senderId = chatPartner,
                    receiverId = "currentUser",
                    content = "That's awesome! Offline-first persistence makes it very reliable.",
                    timestamp = System.currentTimeMillis() - 28000000 // 8 hours ago
                )
            )
            for (msg in initialMessages) {
                dao.insertChatMessage(msg)
            }

            // Marketplace items
            val initialItems = listOf(
                // Electronics
                MarketplaceItem(
                    title = "Solar 20,000mAh Rugged Fast-Charging Power Bank",
                    description = "Heavy duty water-resistant dual-panel solar power bank with built-in LED flashlight and compass. Essential for multi-day expeditions in upper Yarkhoon, Broghil, and remote passes.",
                    price = 45.00,
                    imageUrl = "https://images.unsplash.com/photo-1609592426868-dcf9fe0f4967?w=400&auto=format&fit=crop",
                    category = "Electronics",
                    sellerId = "user_ali",
                    sellerName = "Ali Khan",
                    sellerContact = "+92-300-9876543",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Long-Range Dual-Band UHF/VHF Mountain Walkie-Talkies (Pair)",
                    description = "Pair of 8W handheld two-way radios with 15km line-of-sight range. Includes charging docks, high-gain antennas, and belt clips. Ideal for rescue teams and alpine shepherds.",
                    price = 68.00,
                    imageUrl = "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=400&auto=format&fit=crop",
                    category = "Electronics",
                    sellerId = "user_sher",
                    sellerName = "Sher Jang",
                    sellerContact = "sher@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Canon EOS DSLR Camera with 18-55mm & 75-300mm Zoom Lenses",
                    description = "Gently used DSLR in excellent working condition. Comes with 2 lenses, camera strap, 64GB high-speed SD card, and padded travel bag. Perfect for landscape and wildlife photography.",
                    price = 280.00,
                    imageUrl = "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=400&auto=format&fit=crop",
                    category = "Electronics",
                    sellerId = "user_sher",
                    sellerName = "Sher Jang",
                    sellerContact = "+92-345-4433221",
                    isSold = false
                ),

                // Clothing
                MarketplaceItem(
                    title = "Premium Handwoven Chitrali Woolen Blazer (Shu)",
                    description = "Gorgeous traditional overcoat made of 100% pure mountain sheep wool, handwoven by master weavers in Yarkhoon Valley. Thick, super-warm, wind-resistant, and styled to last a lifetime.",
                    price = 85.00,
                    imageUrl = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400&auto=format&fit=crop",
                    category = "Clothing",
                    sellerId = "user_zara",
                    sellerName = "Zara Shah",
                    sellerContact = "zara@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Authentic Brown Chitrali Pakol Cap",
                    description = "Authentic round rolled woolen pakol cap. Crafted from fine local wool, incredibly soft, and perfect for cold mountain evenings.",
                    price = 15.00,
                    imageUrl = "https://images.unsplash.com/photo-1521369909029-2afed882baee?w=400&auto=format&fit=crop",
                    category = "Clothing",
                    sellerId = "user_zara",
                    sellerName = "Zara Shah",
                    sellerContact = "+92-345-XXXXXXX",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Traditional Embroidered Chitrali Patti Waistcoat",
                    description = "Hand-tailored woolen waistcoat with intricate traditional neckline embroidery. Lined with silky interior fabric. Size Medium/Large.",
                    price = 38.00,
                    imageUrl = "https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=400&auto=format&fit=crop",
                    category = "Clothing",
                    sellerId = "user_zara",
                    sellerName = "Zara Shah",
                    sellerContact = "zara@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Waterproof Thermal Alpine Trekking Parka",
                    description = "Windproof, snow-proof heavy winter mountaineering jacket with removable fleece inner lining. Rated down to -20°C.",
                    price = 95.00,
                    imageUrl = "https://images.unsplash.com/photo-1544923246-77307dd654cb?w=400&auto=format&fit=crop",
                    category = "Clothing",
                    sellerId = "user_ali",
                    sellerName = "Ali Khan",
                    sellerContact = "ali@yarkhoon.com",
                    isSold = false
                ),

                // Home Goods
                MarketplaceItem(
                    title = "Premium Handcarved Walnut Wood Bowl",
                    description = "Exquisitely finished decorative and functional serving bowl, carved out of single-piece mature walnut wood from lower Chitral orchards. Excellent natural grains with protective food-grade finish.",
                    price = 40.00,
                    imageUrl = "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400&auto=format&fit=crop",
                    category = "Home Goods",
                    sellerId = "user_ali",
                    sellerName = "Ali Khan",
                    sellerContact = "ali@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Handwoven Mountain Wool Kilim Rug (5x7 ft)",
                    description = "Authentic tribal geometric pattern rug handmade from natural sheep wool and vegetable dyes. Durable, warm, and adds timeless mountain charm to living spaces.",
                    price = 120.00,
                    imageUrl = "https://images.unsplash.com/photo-1600121848594-d8644e57abab?w=400&auto=format&fit=crop",
                    category = "Home Goods",
                    sellerId = "user_zara",
                    sellerName = "Zara Shah",
                    sellerContact = "zara@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Handmade Cedar Keepsake Box with Brass Latches",
                    description = "Aromatic Deodar cedar wood jewelry and keepsake box with engraved floral border and antique brass latch.",
                    price = 28.00,
                    imageUrl = "https://images.unsplash.com/photo-1544816155-12df9643f363?w=400&auto=format&fit=crop",
                    category = "Home Goods",
                    sellerId = "user_shazia",
                    sellerName = "Shazia Parveen",
                    sellerContact = "+92-321-5556677",
                    isSold = false
                ),

                // Food & Organic
                MarketplaceItem(
                    title = "Organic Sun-Dried Apricots (1kg)",
                    description = "Naturally sun-dried organic sweet apricots harvested from pure water orchards in Yarkhoon. Unsulfured, high fiber, completely sweet and natural mountain energy booster.",
                    price = 12.00,
                    imageUrl = "https://images.unsplash.com/photo-1596515109352-00df2180ec62?w=400&auto=format&fit=crop",
                    category = "Food & Organic",
                    sellerId = "user_shazia",
                    sellerName = "Shazia Parveen",
                    sellerContact = "school-office@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "Raw High-Altitude Wild Mountain Honey (500g Jar)",
                    description = "Pure unpasteurized multi-flora mountain honey gathered from wild bees in Yarkhoon Alpine flora. Rich in enzymes and antioxidants.",
                    price = 22.00,
                    imageUrl = "https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=400&auto=format&fit=crop",
                    category = "Food & Organic",
                    sellerId = "user_ali",
                    sellerName = "Ali Khan",
                    sellerContact = "+92-300-9876543",
                    isSold = false
                ),

                // Sports & Outdoor
                MarketplaceItem(
                    title = "Ultralight Carbon Fiber Trekking Poles (Pair)",
                    description = "Telescopic 3-section trekking poles with ergonomic cork handles and tungsten carbide tips. Quick-lock mechanism for rapid height adjustments.",
                    price = 35.00,
                    imageUrl = "https://images.unsplash.com/photo-1551632811-561732d1e306?w=400&auto=format&fit=crop",
                    category = "Sports & Outdoor",
                    sellerId = "user_sher",
                    sellerName = "Sher Jang",
                    sellerContact = "sher@yarkhoon.com",
                    isSold = false
                ),
                MarketplaceItem(
                    title = "4-Season Geodesic High-Altitude 3-Person Tent",
                    description = "Extreme weather expedition tent with dual vestibules, snow skirts, and aluminum poles. Tested in high-wind conditions near Broghil.",
                    price = 195.00,
                    imageUrl = "https://images.unsplash.com/photo-1510312305653-8ed496efae75?w=400&auto=format&fit=crop",
                    category = "Sports & Outdoor",
                    sellerId = "user_sher",
                    sellerName = "Sher Jang",
                    sellerContact = "sher@yarkhoon.com",
                    isSold = false
                )
            )
            for (item in initialItems) {
                dao.insertMarketplaceItem(item)
            }

            // Default Service Listings
            val initialServices = listOf(
                ServiceListing(
                    providerId = "user_ali",
                    providerName = "Ali Khan",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    serviceType = "Plumber",
                    description = "Professional plumbing and water pipe installation services. Over 8 years of experience working in Chitral and Yarkhoon Valley. Specialty in solar water heater mounting and repairs.",
                    phoneNumber = "+92-300-9876543",
                    imageUrl = "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 7200000
                ),
                ServiceListing(
                    providerId = "user_sher",
                    providerName = "Sher Jang",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    serviceType = "Driver",
                    description = "Reliable private 4x4 Jeep driver for off-road mountain trips. Safe driving to Broghil Pass, Mastuj, Chitral town, and Gilgit. Native guide who knows wilderness routes & accommodations.",
                    phoneNumber = "+92-345-4433221",
                    imageUrl = "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 36000000
                ),
                ServiceListing(
                    providerId = "user_zara",
                    providerName = "Zara Shah",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    serviceType = "Guest House",
                    description = "Welcome to Valley Crest Guest House located in scenic upper Yarkhoon. Traditional wooden architecture, hot organic tea, fresh garden apricots, and panoramic Hindukush views. Wi-Fi available.",
                    phoneNumber = "+92-333-1122334",
                    imageUrl = "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 14400000
                ),
                ServiceListing(
                    providerId = "user_shazia",
                    providerName = "Shazia Parveen",
                    providerAvatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&auto=format&fit=crop",
                    serviceType = "Carpenter",
                    description = "Chitrali traditional walnut wood engraving, door, window design, and furniture crafting. Specialist in local ceiling geometric patterns (Khatamband). Beautiful artisan work.",
                    phoneNumber = "+92-321-5556677",
                    imageUrl = "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=500&auto=format&fit=crop",
                    timestamp = System.currentTimeMillis() - 86400000
                )
            )
            for (service in initialServices) {
                dao.insertServiceListing(service)
            }

            // Initial 24-hour Stories
            val now = System.currentTimeMillis()
            val initialStories = listOf(
                Story(
                    id = "story_yarkhoon_1",
                    authorId = "user_yarkhoon",
                    authorName = "Yarkhoon.com",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=1080&auto=format&fit=crop",
                    textCaption = "Morning sunrise overlooking Tirich Mir & Upper Yarkhoon Valley ☀️🏔️",
                    backgroundColorHex = "#1877F2",
                    timestamp = now - 7200000, // 2h ago
                    expiresAt = now + 79200000L,
                    viewersCount = 38,
                    viewersJson = """[{"userId":"user_ali","userName":"Ali Khan","userAvatarUrl":"https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop","timestamp":${now - 3600000}},{"userId":"user_zara","userName":"Zara Shah","userAvatarUrl":"https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop","timestamp":${now - 1800000}}]"""
                ),
                Story(
                    id = "story_yarkhoon_2",
                    authorId = "user_yarkhoon",
                    authorName = "Yarkhoon.com",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                    mediaType = "TEXT",
                    mediaUrl = "",
                    textCaption = "📢 Reminder: Community digital literacy workshop this Saturday at Mastuj Center. Everyone is welcome!",
                    backgroundColorHex = "#833AB4",
                    textColorHex = "#FFFFFF",
                    timestamp = now - 3600000, // 1h ago
                    expiresAt = now + 82800000L,
                    viewersCount = 24,
                    viewersJson = """[{"userId":"user_sher","userName":"Sher Jang","userAvatarUrl":"https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop","timestamp":${now - 1200000}}]"""
                ),
                Story(
                    id = "story_sher_1",
                    authorId = "user_sher",
                    authorName = "Sher Jang",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    mediaType = "VIDEO",
                    mediaUrl = "https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/echo-hereweare.mp4",
                    textCaption = "Fresh snow on Karambar Lake trail! Trekking high altitude passes ❄️🥾",
                    backgroundColorHex = "#11998E",
                    timestamp = now - 10800000, // 3h ago
                    expiresAt = now + 75600000L,
                    viewersCount = 45,
                    viewersJson = """[{"userId":"user_ali","userName":"Ali Khan","userAvatarUrl":"https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop","timestamp":${now - 7200000}}]"""
                ),
                Story(
                    id = "story_zara_1",
                    authorId = "user_zara",
                    authorName = "Zara Shah",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1580301762395-21ce84d00bc6?w=1080&auto=format&fit=crop",
                    textCaption = "Finishing the final touches on natural dyed Chitrali wool shawl 🧵🌸",
                    backgroundColorHex = "#FF512F",
                    timestamp = now - 14400000, // 4h ago
                    expiresAt = now + 72000000L,
                    viewersCount = 29,
                    viewersJson = "[]"
                ),
                Story(
                    id = "story_ali_1",
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop",
                    mediaType = "IMAGE",
                    mediaUrl = "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?w=1080&auto=format&fit=crop",
                    textCaption = "Finished solar water line installation today in Mastuj! ☀️💧",
                    backgroundColorHex = "#2193B0",
                    timestamp = now - 18000000, // 5h ago
                    expiresAt = now + 68400000L,
                    viewersCount = 18,
                    viewersJson = "[]"
                )
            )
            for (story in initialStories) {
                dao.insertStory(story)
            }

            // Initial Notifications
            val initialNotifications = listOf(
                AppNotification(
                    id = "notif_1",
                    recipientId = "currentUser",
                    senderId = "user_ali",
                    senderName = "Ali Khan",
                    senderAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop",
                    title = "Ali Khan sent you a friend request",
                    description = "Ali Khan from Yarkhoon Valley wants to connect with you.",
                    avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop",
                    timestamp = now - 15 * 60 * 1000L,
                    isRead = false,
                    type = "FRIEND_REQUEST",
                    targetId = "user_ali"
                ),
                AppNotification(
                    id = "notif_2",
                    recipientId = "currentUser",
                    senderId = "user_zara",
                    senderName = "Zara Noor",
                    senderAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    title = "Zara Noor sent you a message",
                    description = "Salam! Are you going to the Shandur Polo festival this year?",
                    avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                    timestamp = now - 45 * 60 * 1000L,
                    isRead = false,
                    type = "MESSAGE",
                    targetId = "user_zara"
                ),
                AppNotification(
                    id = "notif_3",
                    recipientId = "currentUser",
                    senderId = "user_sher",
                    senderName = "Sher Jang",
                    senderAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    title = "New activity in Yarkhoon Trekkers Club",
                    description = "Sher Jang shared a new route update for Karambar Lake pass.",
                    avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150&auto=format&fit=crop",
                    timestamp = now - 2 * 3600 * 1000L,
                    isRead = false,
                    type = "GROUP",
                    targetId = "1"
                ),
                AppNotification(
                    id = "notif_4",
                    recipientId = "currentUser",
                    senderId = "system",
                    senderName = "Yarkhoon.com",
                    senderAvatarUrl = "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=150&auto=format&fit=crop",
                    title = "Welcome to Yarkhoon.com!",
                    description = "Explore community posts, discover local services, and stay in touch with friends.",
                    avatarUrl = "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=150&auto=format&fit=crop",
                    timestamp = now - 24 * 3600 * 1000L,
                    isRead = true,
                    type = "SYSTEM",
                    targetId = "feed"
                )
            )
            dao.insertNotifications(initialNotifications)
        }

        // Seed Pages if empty
        val existingPages = dao.getAllPages().first()
        if (existingPages.isEmpty()) {
            val initialPages = listOf(
                Page(
                    id = 1,
                    name = "Hamara Chitral News & Media",
                    username = "hamarachitral",
                    category = "News & Media",
                    bio = "Official media portal for Upper & Lower Chitral. Real-time news, weather advisories, mountain road alerts, and community stories 📰🏔️",
                    avatarUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200",
                    coverUrl = "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=800",
                    phone = "+92-943-412233",
                    email = "contact@hamarachitral.com",
                    website = "https://yarkhoon.com/page/hamarachitral",
                    location = "Chitral Bazaar, KP, Pakistan",
                    ownerId = "currentUser",
                    followersCount = 2450,
                    isFollowedByMe = true,
                    status = "APPROVED",
                    isVerified = true,
                    createdAt = System.currentTimeMillis() - 86400000L * 60
                ),
                Page(
                    id = 2,
                    name = "Chitral Heritage & Culture",
                    username = "chitralheritage",
                    category = "Community",
                    bio = "Preserving indigenous Chitrali arts, Khowar poetry, traditional crafts, Kalash traditions, and historical archives 🏛️✨",
                    avatarUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=200",
                    coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800",
                    phone = "+92-301-8899776",
                    email = "heritage@yarkhoon.com",
                    website = "https://yarkhoon.com/page/chitralheritage",
                    location = "Ayun & Mastuj, Chitral",
                    ownerId = "user_sher",
                    followersCount = 1820,
                    isFollowedByMe = true,
                    status = "APPROVED",
                    isVerified = true,
                    createdAt = System.currentTimeMillis() - 86400000L * 45
                ),
                Page(
                    id = 3,
                    name = "Yarkhoon Valley Explorers",
                    username = "yarkhoonexplorers",
                    category = "Organization",
                    bio = "Adventure guides, high-altitude trekking, Karambar lake expeditions, and eco-tourism across the Yarkhoon corridor 🥾⛺",
                    avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200",
                    coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
                    phone = "+92-345-7766554",
                    email = "explorers@yarkhoon.com",
                    website = "https://yarkhoon.com/page/yarkhoonexplorers",
                    location = "Yarkhoon Lasht, Upper Chitral",
                    ownerId = "user_ali",
                    followersCount = 960,
                    isFollowedByMe = false,
                    status = "APPROVED",
                    isVerified = false,
                    createdAt = System.currentTimeMillis() - 86400000L * 25
                ),
                Page(
                    id = 4,
                    name = "Khowar Language Academy",
                    username = "khowaracademy",
                    category = "Education",
                    bio = "Dedicated to the documentation, digital preservation, and education of Khowar language, dictionaries, and folklore 📚✨",
                    avatarUrl = "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=200",
                    coverUrl = "https://images.unsplash.com/photo-1455390582262-044cdead277a?w=800",
                    phone = "+92-943-421100",
                    email = "info@khowaracademy.org",
                    website = "https://yarkhoon.com/page/khowaracademy",
                    location = "Booni, Upper Chitral",
                    ownerId = "user_zara",
                    followersCount = 1430,
                    isFollowedByMe = false,
                    status = "APPROVED",
                    isVerified = true,
                    createdAt = System.currentTimeMillis() - 86400000L * 15
                )
            )
            dao.insertPages(initialPages)

            val initialPageMembers = listOf(
                PageMember(pageId = 1, userId = "currentUser", userName = "CurrentUser", userAvatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150", role = "OWNER"),
                PageMember(pageId = 1, userId = "user_sher", userName = "Sher Jang", userAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150", role = "ADMIN"),
                PageMember(pageId = 2, userId = "user_sher", userName = "Sher Jang", userAvatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150", role = "OWNER"),
                PageMember(pageId = 3, userId = "user_ali", userName = "Ali Khan", userAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150", role = "OWNER"),
                PageMember(pageId = 4, userId = "user_zara", userName = "Zara Shah", userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", role = "OWNER")
            )
            dao.insertPageMembers(initialPageMembers)

            val initialPageFollowers = listOf(
                PageFollower(pageId = 1, userId = "currentUser"),
                PageFollower(pageId = 1, userId = "user_sher"),
                PageFollower(pageId = 1, userId = "user_ali"),
                PageFollower(pageId = 2, userId = "currentUser"),
                PageFollower(pageId = 2, userId = "user_zara")
            )
            for (f in initialPageFollowers) {
                dao.insertPageFollower(f)
            }

            val initialPagePosts = listOf(
                PagePost(
                    id = 1,
                    pageId = 1,
                    pageName = "Hamara Chitral News & Media",
                    pageUsername = "hamarachitral",
                    pageAvatarUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200",
                    publisherUserId = "currentUser",
                    content = "Road Advisory: Shandur Pass is currently open for light passenger vehicles. Please drive with winter chains and carry emergency warmth kits. Safe journeys to all mountain travelers! 🏔️🚗",
                    mediaType = "IMAGE",
                    mediaUrlsJson = """["https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=800"]""",
                    isPinned = true,
                    likesCount = 142,
                    isLikedByMe = true,
                    commentsCount = 2,
                    timestamp = System.currentTimeMillis() - 3600000L * 4
                ),
                PagePost(
                    id = 2,
                    pageId = 1,
                    pageName = "Hamara Chitral News & Media",
                    pageUsername = "hamarachitral",
                    pageAvatarUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200",
                    publisherUserId = "currentUser",
                    content = "The annual Booni Spring cultural festival dates announced! Traditional Polo matches, Sitari music concerts, and local handicrafts bazaar from April 15th.",
                    mediaType = "NONE",
                    isPinned = false,
                    likesCount = 88,
                    commentsCount = 1,
                    timestamp = System.currentTimeMillis() - 86400000L * 2
                ),
                PagePost(
                    id = 3,
                    pageId = 2,
                    pageName = "Chitral Heritage & Culture",
                    pageUsername = "chitralheritage",
                    pageAvatarUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=200",
                    publisherUserId = "user_sher",
                    content = "The ancient craftsmanship of 'Shu' (Chitrali homespun wool fabric). Hand spun on takhti drop spindles by village artisans across Yarkhoon Valley.",
                    mediaType = "IMAGE",
                    mediaUrlsJson = """["https://images.unsplash.com/photo-1580301762395-21ce84d00bc6?w=800"]""",
                    likesCount = 95,
                    commentsCount = 0,
                    timestamp = System.currentTimeMillis() - 86400000L * 3
                )
            )
            dao.insertPagePosts(initialPagePosts)

            val initialPageComments = listOf(
                PagePostComment(
                    pagePostId = 1,
                    pageId = 1,
                    authorId = "user_ali",
                    authorName = "Ali Khan",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                    content = "Thank you for the timely update! Very helpful for people travelling towards Gilgit.",
                    timestamp = System.currentTimeMillis() - 3600000L * 2
                ),
                PagePostComment(
                    pagePostId = 1,
                    pageId = 1,
                    authorId = "user_zara",
                    authorName = "Zara Shah",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    content = "Drive safely everyone! The mountain scenery looks breathtaking today.",
                    timestamp = System.currentTimeMillis() - 3600000L * 1
                )
            )
            dao.insertPagePostComments(initialPageComments)
        }

        // Seed Khowar linguistic dataset for AI voice assistant if empty
        com.example.voice.KhowarLanguageEngine.seedInitialKhowarDataset(this@SocialMediaRepository)
    }

    // ==================== KHOWAR LINGUISTIC DATASET ====================
    val allKhowarDatasetEntries: Flow<List<KhowarDatasetEntry>> = dao.getAllKhowarDatasetEntries()

    suspend fun getKhowarDatasetEntriesOnce(): List<KhowarDatasetEntry> = dao.getKhowarDatasetEntriesOnce()

    fun getKhowarDatasetByCategory(category: String): Flow<List<KhowarDatasetEntry>> =
        dao.getKhowarDatasetByCategory(category)

    fun getVerifiedKhowarDataset(): Flow<List<KhowarDatasetEntry>> =
        dao.getVerifiedKhowarDataset()

    suspend fun insertKhowarDatasetEntry(entry: KhowarDatasetEntry) =
        dao.insertKhowarDatasetEntry(entry)

    suspend fun insertKhowarDatasetEntries(entries: List<KhowarDatasetEntry>) =
        dao.insertKhowarDatasetEntries(entries)

    suspend fun updateKhowarDatasetEntry(entry: KhowarDatasetEntry) =
        dao.updateKhowarDatasetEntry(entry)

    suspend fun deleteKhowarDatasetEntry(entryId: String) =
        dao.deleteKhowarDatasetEntryById(entryId)

    fun searchKhowarDataset(query: String): Flow<List<KhowarDatasetEntry>> =
        dao.searchKhowarDataset(query)
}

