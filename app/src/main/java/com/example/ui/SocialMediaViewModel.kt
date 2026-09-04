package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.MediaUtils
import com.example.voice.*
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseApp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ZoomedProfileState(
    val imageUrl: String,
    val userName: String = "",
    val subtitle: String = "",
    val userId: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SocialMediaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = SocialMediaRepository(database.socialMediaDao)

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firebase initialization skipped (google-services.json missing or offline): ${e.localizedMessage}")
            null
        }
    }

    private val firebaseFirestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firestore initialization skipped: ${e.localizedMessage}")
            null
        }
    }

    // Exposed Flows
    val currentUser: StateFlow<User?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPosts: StateFlow<List<Post>> = repository.allPosts
        .map { list ->
            list.sortedWith(
                compareByDescending<Post> { it.isViral }
                    .thenByDescending { it.timestamp }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGroups: StateFlow<List<Group>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMarketplaceItems: StateFlow<List<MarketplaceItem>> = repository.allMarketplaceItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allServiceListings: StateFlow<List<ServiceListing>> = repository.allServiceListings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFriendConnections: StateFlow<List<FriendConnection>> = repository.allFriendConnections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Khowar Linguistic Dataset Flow
    val allKhowarDatasetEntries: StateFlow<List<KhowarDatasetEntry>> = repository.allKhowarDatasetEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice Assistant Manager & State
    val voiceAssistantManager: VoiceAssistantManager by lazy {
        VoiceAssistantManager(getApplication(), viewModelScope)
    }

    val isVoiceAssistantSheetOpen = MutableStateFlow(false)
    val voiceSearchResult = MutableStateFlow<VoiceSearchResult?>(null)
    val isVoiceSearching = MutableStateFlow(false)
    val voiceTranslationResult = MutableStateFlow<VoiceTranslationResult?>(null)
    val isVoiceTranslating = MutableStateFlow(false)
    val khowarDatasetFilterCategory = MutableStateFlow("ALL")
    val khowarDatasetSearchQuery = MutableStateFlow("")

    // Friend Search & Tab State
    val userSearchQuery = MutableStateFlow("")
    val friendActiveTab = MutableStateFlow("SUGGESTIONS") // SUGGESTIONS, RECEIVED, SENT, FRIENDS, ALL

    private val _selectedConnectionForStatusTracking = MutableStateFlow<FriendConnection?>(null)
    val selectedConnectionForStatusTracking: StateFlow<FriendConnection?> = _selectedConnectionForStatusTracking.asStateFlow()

    private val _friendActionMessage = MutableStateFlow<String?>(null)
    val friendActionMessage: StateFlow<String?> = _friendActionMessage.asStateFlow()

    fun clearFriendActionMessage() {
        _friendActionMessage.value = null
    }

    fun openStatusTracking(connection: FriendConnection) {
        _selectedConnectionForStatusTracking.value = connection
    }

    fun closeStatusTracking() {
        _selectedConnectionForStatusTracking.value = null
    }

    // Stories flows & state
    val allActiveStories: StateFlow<List<Story>> = repository.getActiveStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStoriesGroups: StateFlow<List<UserStoriesGroup>> = combine(
        allActiveStories,
        allUsers,
        currentUser
    ) { stories, users, currUser ->
        val currentUserId = currUser?.id ?: "currentUser"
        val storiesByAuthor = stories.groupBy { it.authorId }

        val groups = storiesByAuthor.mapNotNull { (authorId, userStoriesList) ->
            val user = users.find { it.id == authorId } ?: User(
                id = authorId,
                username = userStoriesList.firstOrNull()?.authorName?.lowercase()?.replace(" ", "") ?: authorId,
                fullName = userStoriesList.firstOrNull()?.authorName ?: "Community Member",
                avatarUrl = userStoriesList.firstOrNull()?.authorAvatarUrl ?: "",
                coverUrl = "",
                bio = ""
            )

            // Check if current user has viewed ALL stories in this group
            val hasUnseen = userStoriesList.any { story ->
                !story.viewersJson.contains("\"userId\":\"$currentUserId\"")
            }

            val latestTimestamp = userStoriesList.maxOfOrNull { it.timestamp } ?: 0L

            UserStoriesGroup(
                user = user,
                stories = userStoriesList.sortedBy { it.timestamp },
                hasUnseenStories = hasUnseen,
                lastTimestamp = latestTimestamp
            )
        }

        // Sort: Current user first (if they have stories), then unseen stories descending by time, then seen stories descending
        groups.sortedWith(
            compareByDescending<UserStoriesGroup> { it.user.id == currentUserId }
                .thenByDescending { it.hasUnseenStories }
                .thenByDescending { it.lastTimestamp }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Zoomed Profile Picture state
    private val _zoomedProfile = MutableStateFlow<ZoomedProfileState?>(null)
    val zoomedProfile: StateFlow<ZoomedProfileState?> = _zoomedProfile.asStateFlow()

    // Chat handling
    private val _activeChatUserId = MutableStateFlow<String?>(null)
    val activeChatUserId: StateFlow<String?> = _activeChatUserId.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = combine(_activeChatUserId, repository.currentUser) { activeId, currUser ->
        Pair(activeId, currUser)
    }.flatMapLatest { (userId, currUser) ->
        val myId = currUser?.id ?: "currentUser"
        if (userId != null) {
            repository.getChatMessages(myId, userId)
        } else {
            flowOf(emptyList())
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Group Detail & Management Flows
    private val _selectedGroupId = MutableStateFlow<Int?>(null)
    val selectedGroupId: StateFlow<Int?> = _selectedGroupId.asStateFlow()

    val selectedGroup: StateFlow<Group?> = _selectedGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getGroupById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedGroupMembers: StateFlow<List<GroupMember>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getGroupMembers(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroupPosts: StateFlow<List<GroupPost>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getGroupPosts(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroupJoinRequests: StateFlow<List<GroupJoinRequest>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getGroupJoinRequests(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myGroupInvites: StateFlow<List<GroupInvite>> = currentUser
        .flatMapLatest { user ->
            val uid = user?.id ?: "currentUser"
            repository.getGroupInvitesForUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Group Post Comments flow
    private val _selectedGroupPostId = MutableStateFlow<Int?>(null)
    val selectedGroupPostId: StateFlow<Int?> = _selectedGroupPostId.asStateFlow()

    val selectedGroupPostComments: StateFlow<List<GroupPostComment>> = _selectedGroupPostId
        .flatMapLatest { id ->
            if (id != null) repository.getGroupPostComments(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Feed / Video Post Comments flow
    private val _selectedPostForComments = MutableStateFlow<Post?>(null)
    val selectedPostForComments: StateFlow<Post?> = _selectedPostForComments.asStateFlow()

    val selectedPostComments: StateFlow<List<PostComment>> = _selectedPostForComments
        .flatMapLatest { post ->
            if (post != null) repository.getPostComments(post.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Profile Sharing & Deep Linking State
    private val _profileToShare = MutableStateFlow<User?>(null)
    val profileToShare: StateFlow<User?> = _profileToShare.asStateFlow()

    private val _selectedUserForProfileSheet = MutableStateFlow<User?>(null)
    val selectedUserForProfileSheet: StateFlow<User?> = _selectedUserForProfileSheet.asStateFlow()

    private val _deepLinkMessage = MutableStateFlow<String?>(null)
    val deepLinkMessage: StateFlow<String?> = _deepLinkMessage.asStateFlow()

    // Saved / Bookmarked Posts
    private val _savedPostIds = MutableStateFlow<Set<Int>>(setOf(1, 2))
    val savedPostIds: StateFlow<Set<Int>> = _savedPostIds.asStateFlow()

    // Notifications State - Persisted via Room & Synced via Firestore
    val notifications: StateFlow<List<AppNotification>> = currentUser
        .flatMapLatest { user ->
            val uid = user?.id ?: "currentUser"
            repository.getNotificationsForUser(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pull-To-Refresh State
    private val _isRefreshingFeed = MutableStateFlow(false)
    val isRefreshingFeed: StateFlow<Boolean> = _isRefreshingFeed.asStateFlow()

    // ==================== GEMINI AI STUDIO & CREATION STATES ====================
    val selectedChatModel = MutableStateFlow("gemini-2.5-flash") // gemini-2.5-flash, gemini-3.5-flash, gemini-3.1-pro-preview
    val chatSystemInstruction = MutableStateFlow("You are Yarkhoon AI, a knowledgeable and friendly community assistant for Chitral and Yarkhoon Valley.")
    val isSearchGroundingEnabled = MutableStateFlow(false)

    private val _geminiChatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                id = "welcome_ai_1",
                role = "model",
                content = "Salam & Welcome! I am Yarkhoon AI. You can chat with me using Gemini models, toggle Google Search Grounding for real-time information, generate images with Gemini, compose music, or talk naturally with live voice!",
                timestamp = System.currentTimeMillis(),
                modelUsed = "gemini-2.5-flash"
            )
        )
    )
    val geminiChatMessages: StateFlow<List<AiChatMessage>> = _geminiChatMessages.asStateFlow()
    val isAiChatGenerating = MutableStateFlow(false)

    // Voice Live Conversation State (gemini-3.1-flash-live-preview)
    val isLiveVoiceActive = MutableStateFlow(false)
    val isLiveVoiceListening = MutableStateFlow(false)
    val liveVoiceStatusText = MutableStateFlow("Tap to start conversation with Gemini Live API")
    val liveVoiceTranscript = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    // Media Studio Loading & Current Output States
    val isImageGenLoading = MutableStateFlow(false)
    val latestGeneratedImage = MutableStateFlow<GeneratedMediaResult?>(null)

    val isMusicGenLoading = MutableStateFlow(false)
    val latestGeneratedMusic = MutableStateFlow<GeneratedMediaResult?>(null)

    val isVideoGenLoading = MutableStateFlow(false)
    val latestGeneratedVideo = MutableStateFlow<GeneratedMediaResult?>(null)

    private val _aiCreations = MutableStateFlow<List<AiCreationItem>>(
        listOf(
            AiCreationItem(
                id = "ai_sample_1",
                type = "IMAGE",
                prompt = "Tirich Mir peak shining during golden hour sunrise with traditional Chitrali stone cottage and blooming apricot trees",
                mediaUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop",
                title = "Tirich Mir Golden Sunrise",
                modelUsed = "gemini-3.1-flash-image-preview",
                aspectRatio = "16:9"
            ),
            AiCreationItem(
                id = "ai_sample_2",
                type = "MUSIC",
                prompt = "Traditional Chitrali Sitar and flute melody with rhythmic folk dholak",
                mediaUrl = "https://actions.google.com/sounds/v1/ambiences/outdoor_ambience.ogg",
                title = "Valley Folk Sitar Melody",
                modelUsed = "lyria-3-clip-preview",
                metadata = "Traditional Folk • 30s Clip"
            ),
            AiCreationItem(
                id = "ai_sample_3",
                type = "VIDEO",
                prompt = "Cinematic aerial camera flying over Yarkhoon river through the Karakoram and Hindu Kush valleys",
                mediaUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                title = "Yarkhoon River Aerial Flight",
                modelUsed = "veo-3.1-fast-generate-preview",
                aspectRatio = "16:9"
            )
        )
    )
    val aiCreations: StateFlow<List<AiCreationItem>> = _aiCreations.asStateFlow()

    private val _lastRefreshedTime = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshedTime: StateFlow<Long> = _lastRefreshedTime.asStateFlow()

    private val _refreshFeedbackMessage = MutableStateFlow<String?>(null)
    val refreshFeedbackMessage: StateFlow<String?> = _refreshFeedbackMessage.asStateFlow()

    fun clearRefreshFeedbackMessage() {
        _refreshFeedbackMessage.value = null
    }

    fun refreshFeed(onFinished: ((String) -> Unit)? = null) {
        if (_isRefreshingFeed.value) return
        viewModelScope.launch {
            _isRefreshingFeed.value = true
            try {
                withContext(Dispatchers.IO) {
                    // 1. Clean up expired stories
                    repository.deleteExpiredStories()

                    // 2. Sync from Firestore if available
                    try {
                        firebaseFirestore?.collection("posts")
                            ?.limit(20)
                            ?.get()
                            ?.addOnSuccessListener { snapshot ->
                                if (snapshot != null && !snapshot.isEmpty) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        for (doc in snapshot.documents) {
                                            try {
                                                val id = doc.getLong("id")?.toInt() ?: doc.id.hashCode()
                                                val authorId = doc.getString("authorId") ?: "currentUser"
                                                val authorName = doc.getString("authorName") ?: "Community Member"
                                                val authorAvatarUrl = doc.getString("authorAvatarUrl") ?: ""
                                                val content = doc.getString("content") ?: ""
                                                val mediaType = doc.getString("mediaType") ?: "IMAGE"
                                                val mediaUrl = doc.getString("mediaUrl") ?: ""
                                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                                val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                                                val commentsCount = doc.getLong("commentsCount")?.toInt() ?: 0
                                                val isViral = doc.getBoolean("isViral") ?: false

                                                val post = Post(
                                                    id = id,
                                                    authorId = authorId,
                                                    authorName = authorName,
                                                    authorAvatarUrl = authorAvatarUrl,
                                                    content = content,
                                                    mediaType = mediaType,
                                                    mediaUrl = mediaUrl,
                                                    timestamp = timestamp,
                                                    likesCount = likesCount,
                                                    commentsCount = commentsCount,
                                                    isViral = isViral
                                                )
                                                repository.insertPost(post)
                                            } catch (e: Exception) {
                                                // ignore individual item parse error
                                            }
                                        }
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        android.util.Log.w("SocialMediaViewModel", "Firestore posts fetch error: ${e.localizedMessage}")
                    }

                    // 3. Dynamic local feed updates / new simulated valley updates if refreshed
                    val existingPosts = repository.allPosts.firstOrNull() ?: emptyList()
                    val candidateFreshPosts = listOf(
                        Post(
                            authorId = "user_yarkhoon",
                            authorName = "Yarkhoon.com Official",
                            authorAvatarUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=150&auto=format&fit=crop",
                            content = "🏔️✨ Good morning from Upper Chitral! Weather forecast: Clear blue skies over Yarkhoon Lasht and Broghil Valley today. Road conditions are fully operational. Have a blessed day!",
                            mediaType = "IMAGE",
                            mediaUrl = "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800&auto=format&fit=crop",
                            timestamp = System.currentTimeMillis(),
                            likesCount = 89,
                            commentsCount = 14,
                            isViral = true
                        ),
                        Post(
                            authorId = "user_ali",
                            authorName = "Ali Khan",
                            authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop",
                            content = "Fresh harvest of organic Yarkhoon walnuts and dried mulberries is ready! Sending warm greetings to everyone connecting on Yarkhoon.com 🌿🌰",
                            mediaType = "IMAGE",
                            mediaUrl = "https://images.unsplash.com/photo-1586348943529-beaae6c28db9?w=800&auto=format&fit=crop",
                            timestamp = System.currentTimeMillis() - 1000L,
                            likesCount = 42,
                            commentsCount = 6
                        ),
                        Post(
                            authorId = "user_zara",
                            authorName = "Zara Noor",
                            authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop",
                            content = "Traditional Khowar poetry evening planned for this weekend in Booni! Who from the valley is joining us? Let's celebrate our rich heritage! 🎻📜",
                            mediaType = "IMAGE",
                            mediaUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop",
                            timestamp = System.currentTimeMillis() - 2000L,
                            likesCount = 67,
                            commentsCount = 19
                        )
                    )

                    // Insert a fresh post if not yet in database
                    var newPostAdded = false
                    for (candidate in candidateFreshPosts) {
                        val exists = existingPosts.any { it.content.take(30) == candidate.content.take(30) }
                        if (!exists) {
                            repository.insertPost(candidate)
                            newPostAdded = true
                            break
                        }
                    }

                    // Also randomly refresh engagement slightly on top post to give a live feeling
                    existingPosts.firstOrNull()?.let { topPost ->
                        repository.updatePost(
                            topPost.copy(
                                likesCount = topPost.likesCount + (1..3).random(),
                                commentsCount = topPost.commentsCount + (if (Math.random() > 0.5) 1 else 0)
                            )
                        )
                    }
                }

                // Smooth delay so the pull indicator is visible and feels responsive
                delay(900)
                _lastRefreshedTime.value = System.currentTimeMillis()
                val msg = "Feed updated • You're all caught up!"
                _refreshFeedbackMessage.value = msg
                onFinished?.invoke(msg)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Feed refresh error: ${e.localizedMessage}", e)
                val msg = "Feed refreshed"
                _refreshFeedbackMessage.value = msg
                onFinished?.invoke(msg)
            } finally {
                _isRefreshingFeed.value = false
            }
        }
    }

    // Dark Mode Override: null = System, true = Force Dark, false = Force Light
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean?) {
        _isDarkMode.value = enabled
    }

    fun toggleSavePost(postId: Int) {
        val current = _savedPostIds.value.toMutableSet()
        if (current.contains(postId)) {
            current.remove(postId)
        } else {
            current.add(postId)
        }
        _savedPostIds.value = current
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markNotificationAsRead(notificationId)
            try {
                firebaseFirestore?.collection("notifications")?.document(notificationId)?.update("isRead", true)
            } catch (e: Exception) {
                android.util.Log.w("SocialMediaViewModel", "Firestore mark notif read note: ${e.localizedMessage}")
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = currentUser.value?.id ?: "currentUser"
            repository.markAllNotificationsAsRead(uid)
            try {
                firebaseFirestore?.collection("notifications")
                    ?.whereEqualTo("recipientId", uid)
                    ?.get()
                    ?.addOnSuccessListener { snapshot ->
                        if (snapshot != null && !snapshot.isEmpty) {
                            for (doc in snapshot.documents) {
                                doc.reference.update("isRead", true)
                            }
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.w("SocialMediaViewModel", "Firestore mark all notifs read note: ${e.localizedMessage}")
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNotification(notificationId)
            try {
                firebaseFirestore?.collection("notifications")?.document(notificationId)?.delete()
            } catch (e: Exception) {
                android.util.Log.w("SocialMediaViewModel", "Firestore delete notification note: ${e.localizedMessage}")
            }
        }
    }

    fun sendNotification(
        recipientId: String,
        title: String,
        description: String,
        avatarUrl: String = "",
        type: String = "SYSTEM",
        targetId: String? = null,
        senderId: String = currentUser.value?.id ?: "currentUser",
        senderName: String = currentUser.value?.fullName ?: "Community Member",
        senderAvatarUrl: String = currentUser.value?.avatarUrl ?: ""
    ) {
        val notifId = "notif_${UUID.randomUUID().toString().take(8)}_${System.currentTimeMillis()}"
        val notif = AppNotification(
            id = notifId,
            recipientId = recipientId,
            senderId = senderId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            title = title,
            description = description,
            avatarUrl = avatarUrl.ifBlank { senderAvatarUrl },
            timestamp = System.currentTimeMillis(),
            isRead = false,
            type = type,
            targetId = targetId
        )

        viewModelScope.launch(Dispatchers.IO) {
            val myUid = currentUser.value?.id ?: "currentUser"
            if (recipientId == myUid || recipientId == "currentUser" || recipientId == "all") {
                repository.insertNotification(notif)
            }

            try {
                val notifMap = hashMapOf(
                    "id" to notif.id,
                    "recipientId" to notif.recipientId,
                    "senderId" to notif.senderId,
                    "senderName" to notif.senderName,
                    "senderAvatarUrl" to notif.senderAvatarUrl,
                    "title" to notif.title,
                    "description" to notif.description,
                    "avatarUrl" to notif.avatarUrl,
                    "timestamp" to notif.timestamp,
                    "isRead" to notif.isRead,
                    "type" to notif.type,
                    "targetId" to notif.targetId
                )
                firebaseFirestore?.collection("notifications")?.document(notif.id)?.set(notifMap)
            } catch (e: Exception) {
                android.util.Log.w("SocialMediaViewModel", "Firestore send notification error: ${e.localizedMessage}")
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.prepopulateIfEmpty()
                repository.deleteExpiredStories()
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Failed to prepopulate database: ${e.localizedMessage}", e)
            }
        }

        // Real-time Firestore Stories Listener
        try {
            firebaseFirestore?.collection("stories")
                ?.whereGreaterThan("expiresAt", System.currentTimeMillis())
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    viewModelScope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val story = Story(
                                    id = doc.getString("id") ?: doc.id,
                                    authorId = doc.getString("authorId") ?: "",
                                    authorName = doc.getString("authorName") ?: "",
                                    authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                                    mediaType = doc.getString("mediaType") ?: "IMAGE",
                                    mediaUrl = doc.getString("mediaUrl") ?: "",
                                    textCaption = doc.getString("textCaption") ?: "",
                                    backgroundColorHex = doc.getString("backgroundColorHex") ?: "#1877F2",
                                    textColorHex = doc.getString("textColorHex") ?: "#FFFFFF",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    expiresAt = doc.getLong("expiresAt") ?: (System.currentTimeMillis() + 86400000L),
                                    viewersCount = doc.getLong("viewersCount")?.toInt() ?: 0,
                                    viewersJson = doc.getString("viewersJson") ?: "[]"
                                )
                                if (story.authorId.isNotBlank()) {
                                    repository.insertStory(story)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("SocialMediaViewModel", "Error parsing story document: ${e.localizedMessage}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firestore stories listener error: ${e.localizedMessage}")
        }

        // Real-time Firestore Notifications Listener
        try {
            firebaseFirestore?.collection("notifications")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    viewModelScope.launch(Dispatchers.IO) {
                        val currentUid = currentUser.value?.id ?: "currentUser"
                        for (doc in snapshot.documents) {
                            try {
                                val recipientId = doc.getString("recipientId") ?: "currentUser"
                                if (recipientId == currentUid || recipientId == "currentUser" || recipientId == "all") {
                                    val notif = AppNotification(
                                        id = doc.getString("id") ?: doc.id,
                                        recipientId = recipientId,
                                        senderId = doc.getString("senderId") ?: "",
                                        senderName = doc.getString("senderName") ?: "",
                                        senderAvatarUrl = doc.getString("senderAvatarUrl") ?: "",
                                        title = doc.getString("title") ?: "Notification",
                                        description = doc.getString("description") ?: "",
                                        avatarUrl = doc.getString("avatarUrl") ?: "",
                                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                        isRead = doc.getBoolean("isRead") ?: false,
                                        type = doc.getString("type") ?: "SYSTEM",
                                        targetId = doc.getString("targetId")
                                    )
                                    repository.insertNotification(notif)
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("SocialMediaViewModel", "Error parsing notification doc: ${e.localizedMessage}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firestore notifications listener error: ${e.localizedMessage}")
        }

        // Real-time Firestore Chat Messages Listener (alerts recipient in real-time)
        try {
            firebaseFirestore?.collection("chat_messages")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    viewModelScope.launch(Dispatchers.IO) {
                        val currentUid = currentUser.value?.id ?: "currentUser"
                        for (doc in snapshot.documents) {
                            try {
                                val senderId = doc.getString("senderId") ?: ""
                                val receiverId = doc.getString("receiverId") ?: ""
                                val content = doc.getString("content") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                if (senderId.isNotBlank() && receiverId.isNotBlank() && content.isNotBlank()) {
                                    val msg = ChatMessage(
                                        senderId = senderId,
                                        receiverId = receiverId,
                                        content = content,
                                        timestamp = timestamp
                                    )
                                    repository.insertChatMessage(msg)

                                    if ((receiverId == currentUid || receiverId == "currentUser") && senderId != currentUid) {
                                        val allU = repository.allUsers.first()
                                        val sender = allU.firstOrNull { it.id == senderId }
                                        val senderName = sender?.fullName?.ifBlank { sender.username } ?: "Friend"
                                        val senderAvatar = sender?.avatarUrl ?: ""
                                        val notifId = "msg_notif_${senderId}_${timestamp}"

                                        val notif = AppNotification(
                                            id = notifId,
                                            recipientId = currentUid,
                                            senderId = senderId,
                                            senderName = senderName,
                                            senderAvatarUrl = senderAvatar,
                                            title = "New Message from $senderName",
                                            description = if (content.length > 60) content.take(57) + "..." else content,
                                            avatarUrl = senderAvatar,
                                            timestamp = timestamp,
                                            isRead = false,
                                            type = "MESSAGE",
                                            targetId = senderId
                                        )
                                        repository.insertNotification(notif)
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("SocialMediaViewModel", "Error parsing chat message doc: ${e.localizedMessage}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firestore chat_messages listener error: ${e.localizedMessage}")
        }

        // Real-time Firestore Friend Requests / Connections Listener
        try {
            firebaseFirestore?.collection("friend_requests")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    viewModelScope.launch(Dispatchers.IO) {
                        val currentUid = currentUser.value?.id ?: "currentUser"
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val senderId = doc.getString("senderId") ?: ""
                                val senderName = doc.getString("senderName") ?: ""
                                val senderUsername = doc.getString("senderUsername") ?: ""
                                val senderAvatarUrl = doc.getString("senderAvatarUrl") ?: ""
                                val receiverId = doc.getString("receiverId") ?: ""
                                val receiverName = doc.getString("receiverName") ?: ""
                                val receiverUsername = doc.getString("receiverUsername") ?: ""
                                val receiverAvatarUrl = doc.getString("receiverAvatarUrl") ?: ""
                                val status = doc.getString("status") ?: "PENDING"
                                val introMessage = doc.getString("introMessage") ?: ""
                                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                                val connection = FriendConnection(
                                    id = id,
                                    senderId = senderId,
                                    senderName = senderName,
                                    senderUsername = senderUsername,
                                    senderAvatarUrl = senderAvatarUrl,
                                    receiverId = receiverId,
                                    receiverName = receiverName,
                                    receiverUsername = receiverUsername,
                                    receiverAvatarUrl = receiverAvatarUrl,
                                    status = status,
                                    introMessage = introMessage,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt,
                                    isSyncedWithFirestore = true
                                )
                                repository.insertFriendConnection(connection)

                                // Create Real-Time Friend Request Notifications
                                if ((receiverId == currentUid || receiverId == "currentUser") && senderId != currentUid && status == "PENDING") {
                                    val notifId = "fr_notif_${senderId}_${createdAt}"
                                    val notif = AppNotification(
                                        id = notifId,
                                        recipientId = currentUid,
                                        senderId = senderId,
                                        senderName = senderName,
                                        senderAvatarUrl = senderAvatarUrl,
                                        title = "$senderName sent you a friend request",
                                        description = if (introMessage.isNotBlank()) introMessage else "$senderName wants to connect with you on Yarkhoon.com",
                                        avatarUrl = senderAvatarUrl,
                                        timestamp = createdAt,
                                        isRead = false,
                                        type = "FRIEND_REQUEST",
                                        targetId = senderId
                                    )
                                    repository.insertNotification(notif)
                                } else if ((senderId == currentUid || senderId == "currentUser") && status == "ACCEPTED") {
                                    val notifId = "fr_accepted_${receiverId}_${updatedAt}"
                                    val notif = AppNotification(
                                        id = notifId,
                                        recipientId = currentUid,
                                        senderId = receiverId,
                                        senderName = receiverName,
                                        senderAvatarUrl = receiverAvatarUrl,
                                        title = "$receiverName accepted your friend request",
                                        description = "You are now connected with $receiverName on Yarkhoon.com",
                                        avatarUrl = receiverAvatarUrl,
                                        timestamp = updatedAt,
                                        isRead = false,
                                        type = "FRIEND_REQUEST",
                                        targetId = receiverId
                                    )
                                    repository.insertNotification(notif)
                                }

                                // Also update the other user's friendStatus in User table
                                val otherId = if (senderId == currentUid) receiverId else if (receiverId == currentUid) senderId else null
                                if (otherId != null) {
                                    val otherUser = repository.allUsers.first().firstOrNull { it.id == otherId }
                                    if (otherUser != null) {
                                        val newStatus = when (status) {
                                            "ACCEPTED" -> "FRIENDS"
                                            "PENDING" -> if (senderId == currentUid) "SENT" else "RECEIVED"
                                            else -> "NONE"
                                        }
                                        if (otherUser.friendStatus != newStatus) {
                                            repository.updateUser(otherUser.copy(friendStatus = newStatus))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("SocialMediaViewModel", "Error parsing friend request doc: ${e.localizedMessage}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.w("SocialMediaViewModel", "Firestore friend requests listener error: ${e.localizedMessage}")
        }
    }

    // --- Actions ---

    fun setActiveChatUser(userId: String?) {
        _activeChatUserId.value = userId
    }

    fun onCreatePost(content: String, mediaType: String, mediaUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val authorName = user?.fullName ?: "Hamara Chitral"
            val authorAvatarUrl = user?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop"

            val isUserYarkhoon = user?.id == "user_yarkhoon" || user?.fullName == "Yarkhoon.com"

            val newPost = Post(
                authorId = user?.id ?: "currentUser",
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                content = content,
                mediaType = mediaType,
                mediaUrl = mediaUrl,
                timestamp = System.currentTimeMillis(),
                likesCount = if (isUserYarkhoon) 2450 else 0,
                isLikedByMe = if (isUserYarkhoon) true else false,
                commentsCount = if (isUserYarkhoon) 115 else 0,
                isViral = isUserYarkhoon
            )
            repository.insertPost(newPost)

            // Firestore sync
            try {
                val postMap = hashMapOf(
                    "authorId" to newPost.authorId,
                    "authorName" to newPost.authorName,
                    "authorAvatarUrl" to newPost.authorAvatarUrl,
                    "content" to newPost.content,
                    "mediaType" to newPost.mediaType,
                    "mediaUrl" to newPost.mediaUrl,
                    "timestamp" to newPost.timestamp,
                    "likesCount" to newPost.likesCount,
                    "commentsCount" to newPost.commentsCount,
                    "isViral" to newPost.isViral
                )
                firebaseFirestore?.collection("posts")?.add(postMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore post creation error: ${e.localizedMessage}")
            }
        }
    }

    fun onToggleLike(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPost = post.copy(
                isLikedByMe = !post.isLikedByMe,
                likesCount = if (post.isLikedByMe) maxOf(0, post.likesCount - 1) else post.likesCount + 1
            )
            repository.updatePost(updatedPost)
        }
    }

    fun openCommentsForPost(post: Post) {
        _selectedPostForComments.value = post
    }

    fun closeCommentsForPost() {
        _selectedPostForComments.value = null
    }

    fun onAddComment(post: Post, commentText: String) {
        onAddPostComment(post, commentText)
    }

    fun onAddPostComment(
        post: Post,
        content: String,
        parentCommentId: Int? = null,
        replyToAuthorName: String? = null
    ) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val comment = PostComment(
                postId = post.id,
                authorId = user?.id ?: "currentUser",
                authorName = user?.fullName ?: "Community Member",
                authorAvatarUrl = user?.avatarUrl ?: "",
                content = content.trim(),
                parentCommentId = parentCommentId,
                replyToAuthorName = replyToAuthorName,
                timestamp = System.currentTimeMillis(),
                likesCount = 0,
                isLikedByMe = false
            )
            val generatedId = repository.insertPostComment(comment)

            val updatedPost = post.copy(commentsCount = post.commentsCount + 1)
            repository.updatePost(updatedPost)
            _selectedPostForComments.value = updatedPost

            // Firestore real-time sync
            try {
                val commentMap = hashMapOf(
                    "id" to generatedId,
                    "postId" to post.id,
                    "authorId" to comment.authorId,
                    "authorName" to comment.authorName,
                    "authorAvatarUrl" to comment.authorAvatarUrl,
                    "content" to comment.content,
                    "parentCommentId" to comment.parentCommentId,
                    "replyToAuthorName" to comment.replyToAuthorName,
                    "timestamp" to comment.timestamp,
                    "likesCount" to 0,
                    "isEdited" to false
                )
                firebaseFirestore?.collection("posts")
                    ?.document(post.id.toString())
                    ?.collection("comments")
                    ?.document(generatedId.toString())
                    ?.set(commentMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore comment sync error: ${e.localizedMessage}")
            }

            // Notification for post author
            if (post.authorId != user?.id && post.authorId.isNotBlank()) {
                sendNotification(
                    recipientId = post.authorId,
                    title = "New Comment on Your Post",
                    description = "${user?.fullName ?: "Someone"} commented: \"${content.trim().take(35)}\"",
                    avatarUrl = user?.avatarUrl ?: "",
                    type = "COMMENT",
                    targetId = post.id.toString()
                )
            }
        }
    }

    fun onEditPostComment(comment: PostComment, newContent: String, post: Post) {
        if (newContent.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedComment = comment.copy(
                content = newContent.trim(),
                isEdited = true
            )
            repository.updatePostComment(updatedComment)

            // Firestore sync
            try {
                firebaseFirestore?.collection("posts")
                    ?.document(post.id.toString())
                    ?.collection("comments")
                    ?.document(comment.id.toString())
                    ?.update(mapOf("content" to newContent.trim(), "isEdited" to true))
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore edit comment sync error: ${e.localizedMessage}")
            }
        }
    }

    fun onDeletePostComment(commentId: Int, post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePostComment(commentId)
            val updatedPost = post.copy(commentsCount = maxOf(0, post.commentsCount - 1))
            repository.updatePost(updatedPost)
            _selectedPostForComments.value = updatedPost

            // Firestore sync
            try {
                firebaseFirestore?.collection("posts")
                    ?.document(post.id.toString())
                    ?.collection("comments")
                    ?.document(commentId.toString())
                    ?.delete()
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore delete comment error: ${e.localizedMessage}")
            }
        }
    }

    fun onTogglePostCommentLike(comment: PostComment, post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val newIsLiked = !comment.isLikedByMe
            val newLikesCount = if (newIsLiked) comment.likesCount + 1 else maxOf(0, comment.likesCount - 1)
            val updated = comment.copy(
                isLikedByMe = newIsLiked,
                likesCount = newLikesCount
            )
            repository.updatePostComment(updated)

            val currentUid = currentUser.value?.id ?: "currentUser"
            if (newIsLiked) {
                repository.insertCommentLike(CommentLike(commentId = comment.id, userId = currentUid))
            } else {
                repository.deleteCommentLike(comment.id, currentUid)
            }

            // Firestore sync
            try {
                firebaseFirestore?.collection("posts")
                    ?.document(post.id.toString())
                    ?.collection("comments")
                    ?.document(comment.id.toString())
                    ?.update(mapOf("likesCount" to newLikesCount))
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore comment like update error: ${e.localizedMessage}")
            }
        }
    }

    fun onReportPostComment(comment: PostComment, post: Post, reason: String, details: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val report = CommentReport(
                commentId = comment.id,
                postId = post.id,
                reporterId = user?.id ?: "currentUser",
                reporterName = user?.fullName ?: "Community Member",
                reason = reason,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            repository.insertCommentReport(report)
            val updatedComment = comment.copy(isReported = true)
            repository.updatePostComment(updatedComment)

            // Firestore sync
            try {
                val reportMap = hashMapOf(
                    "commentId" to comment.id,
                    "postId" to post.id,
                    "reporterId" to report.reporterId,
                    "reporterName" to report.reporterName,
                    "reason" to report.reason,
                    "details" to report.details,
                    "timestamp" to report.timestamp
                )
                firebaseFirestore?.collection("comment_reports")?.add(reportMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore report comment sync error: ${e.localizedMessage}")
            }
        }
    }

    // Profile Sharing & Deep Linking methods
    fun openShareProfile(user: User) {
        _profileToShare.value = user
    }

    fun closeShareProfile() {
        _profileToShare.value = null
    }

    fun openUserProfile(user: User) {
        _selectedUserForProfileSheet.value = user
    }

    fun closeUserProfile() {
        _selectedUserForProfileSheet.value = null
    }

    fun clearDeepLinkMessage() {
        _deepLinkMessage.value = null
    }

    fun generateProfileUrl(user: User): String {
        val identifier = user.username.ifBlank { user.id }
        return "https://yarkhoon.com/user/$identifier"
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch(Dispatchers.IO) {
            val host = uri.host ?: ""
            val path = uri.path ?: ""

            val users = repository.allUsers.first()
            val posts = repository.allPosts.first()

            // Match user profile deep links: https://yarkhoon.com/user/{username}, yarkhoon://user/{username}
            if (path.contains("/user/") || path.contains("/profile/") || path.contains("/u/") || host == "user" || host == "profile") {
                val identifier = uri.lastPathSegment?.trim()?.removePrefix("@") ?: ""
                val foundUser = users.firstOrNull {
                    it.username.equals(identifier, ignoreCase = true) ||
                    it.id.equals(identifier, ignoreCase = true) ||
                    it.fullName.equals(identifier, ignoreCase = true)
                }
                if (foundUser != null) {
                    _selectedUserForProfileSheet.value = foundUser
                    _deepLinkMessage.value = "Opened profile: @${foundUser.username} via deep link"
                } else {
                    _deepLinkMessage.value = "User '@$identifier' not found"
                }
            } else if (path.contains("/post/") || host == "post") {
                val postId = uri.lastPathSegment?.toIntOrNull()
                val foundPost = posts.firstOrNull { it.id == postId }
                if (foundPost != null) {
                    _selectedPostForComments.value = foundPost
                    _deepLinkMessage.value = "Opened post #${foundPost.id} via deep link"
                }
            }
        }
    }

    fun onSendFriendRequest(otherUserId: String, introMessage: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            val current = currentUser.value
            val currentUid = current?.id ?: "currentUser"
            val currentName = current?.fullName.takeIf { !it.isNullOrBlank() } ?: "Hamara Chitral"
            val currentUname = current?.username.takeIf { !it.isNullOrBlank() } ?: "chitral_user"
            val currentAvatar = current?.avatarUrl ?: ""

            if (user != null) {
                val updatedUser = user.copy(friendStatus = "SENT")
                repository.updateUser(updatedUser)

                val connId = "req_${currentUid}_${user.id}"
                val now = System.currentTimeMillis()
                val connection = FriendConnection(
                    id = connId,
                    senderId = currentUid,
                    senderName = currentName,
                    senderUsername = currentUname,
                    senderAvatarUrl = currentAvatar,
                    receiverId = user.id,
                    receiverName = user.fullName,
                    receiverUsername = user.username,
                    receiverAvatarUrl = user.avatarUrl,
                    status = "PENDING",
                    introMessage = introMessage.trim(),
                    createdAt = now,
                    updatedAt = now,
                    isSyncedWithFirestore = true
                )
                repository.insertFriendConnection(connection)

                // Sync to Firestore
                try {
                    val map = hashMapOf(
                        "id" to connId,
                        "senderId" to currentUid,
                        "senderName" to currentName,
                        "senderUsername" to currentUname,
                        "senderAvatarUrl" to currentAvatar,
                        "receiverId" to user.id,
                        "receiverName" to user.fullName,
                        "receiverUsername" to user.username,
                        "receiverAvatarUrl" to user.avatarUrl,
                        "status" to "PENDING",
                        "introMessage" to introMessage.trim(),
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                    firebaseFirestore?.collection("friend_requests")?.document(connId)?.set(map)
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore send friend request error: ${e.localizedMessage}")
                }

                _friendActionMessage.value = "Friend request sent to ${user.fullName} (Tracked in Firestore)"

                // Add in-app notification & send real-time notification to recipient via Firestore
                val notif = AppNotification(
                    id = "notif_sent_${System.currentTimeMillis()}",
                    recipientId = currentUid,
                    senderId = currentUid,
                    senderName = currentName,
                    senderAvatarUrl = currentAvatar,
                    title = "Friend Request Sent",
                    description = "You sent a connection request to ${user.fullName}",
                    avatarUrl = user.avatarUrl,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    type = "FRIEND_REQUEST",
                    targetId = user.id
                )
                repository.insertNotification(notif)

                // Send real-time notification to target user
                sendNotification(
                    recipientId = user.id,
                    title = "$currentName sent you a friend request",
                    description = if (introMessage.isNotBlank()) introMessage else "$currentName wants to connect with you on Yarkhoon.com",
                    avatarUrl = currentAvatar,
                    type = "FRIEND_REQUEST",
                    targetId = currentUid,
                    senderId = currentUid,
                    senderName = currentName,
                    senderAvatarUrl = currentAvatar
                )
            }
        }
    }

    fun onAcceptFriendRequest(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            val current = currentUser.value
            val currentUid = current?.id ?: "currentUser"
            val currentName = current?.fullName.takeIf { !it.isNullOrBlank() } ?: "Hamara Chitral"
            val currentAvatar = current?.avatarUrl ?: ""
            val now = System.currentTimeMillis()

            if (user != null) {
                val updatedUser = user.copy(friendStatus = "FRIENDS")
                repository.updateUser(updatedUser)

                // Find connection record
                val existingConn = repository.getFriendConnectionBetweenOnce(currentUid, otherUserId)
                val connId = existingConn?.id ?: "req_${otherUserId}_${currentUid}"
                val updatedConn = (existingConn ?: FriendConnection(
                    id = connId,
                    senderId = otherUserId,
                    senderName = user.fullName,
                    senderUsername = user.username,
                    senderAvatarUrl = user.avatarUrl,
                    receiverId = currentUid,
                    receiverName = current?.fullName ?: "Hamara Chitral",
                    receiverUsername = current?.username ?: "chitral_user",
                    receiverAvatarUrl = current?.avatarUrl ?: "",
                    status = "ACCEPTED",
                    createdAt = now,
                    updatedAt = now,
                    isSyncedWithFirestore = true
                )).copy(status = "ACCEPTED", updatedAt = now)
                repository.insertFriendConnection(updatedConn)

                // Sync to Firestore
                try {
                    val map = hashMapOf(
                        "id" to connId,
                        "status" to "ACCEPTED",
                        "updatedAt" to now
                    )
                    firebaseFirestore?.collection("friend_requests")?.document(connId)?.set(map, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore accept request error: ${e.localizedMessage}")
                }

                _friendActionMessage.value = "You are now connected with ${user.fullName}!"

                // Notification for current user
                val notif = AppNotification(
                    id = "notif_accepted_${System.currentTimeMillis()}",
                    recipientId = currentUid,
                    senderId = otherUserId,
                    senderName = user.fullName,
                    senderAvatarUrl = user.avatarUrl,
                    title = "New Friend Connected",
                    description = "You are now connected with ${user.fullName} on Yarkhoon.com",
                    avatarUrl = user.avatarUrl,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    type = "FRIEND_REQUEST",
                    targetId = user.id
                )
                repository.insertNotification(notif)

                // Real-time alert to the other user
                sendNotification(
                    recipientId = otherUserId,
                    title = "$currentName accepted your friend request",
                    description = "You and $currentName are now friends on Yarkhoon.com!",
                    avatarUrl = currentAvatar,
                    type = "FRIEND_REQUEST",
                    targetId = currentUid,
                    senderId = currentUid,
                    senderName = currentName,
                    senderAvatarUrl = currentAvatar
                )
            }
        }
    }

    fun onDeclineFriendRequest(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            val current = currentUser.value
            val currentUid = current?.id ?: "currentUser"
            val now = System.currentTimeMillis()

            if (user != null) {
                val updatedUser = user.copy(friendStatus = "NONE")
                repository.updateUser(updatedUser)

                val existingConn = repository.getFriendConnectionBetweenOnce(currentUid, otherUserId)
                val connId = existingConn?.id ?: "req_${otherUserId}_${currentUid}"
                if (existingConn != null) {
                    repository.updateFriendConnection(existingConn.copy(status = "DECLINED", updatedAt = now))
                }

                try {
                    val map = hashMapOf(
                        "id" to connId,
                        "status" to "DECLINED",
                        "updatedAt" to now
                    )
                    firebaseFirestore?.collection("friend_requests")?.document(connId)?.set(map, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore decline request error: ${e.localizedMessage}")
                }

                _friendActionMessage.value = "Declined friend request from ${user.fullName}"
            }
        }
    }

    fun onCancelFriendRequest(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            val current = currentUser.value
            val currentUid = current?.id ?: "currentUser"
            val now = System.currentTimeMillis()

            if (user != null) {
                val updatedUser = user.copy(friendStatus = "NONE")
                repository.updateUser(updatedUser)

                val existingConn = repository.getFriendConnectionBetweenOnce(currentUid, otherUserId)
                val connId = existingConn?.id ?: "req_${currentUid}_${otherUserId}"
                if (existingConn != null) {
                    repository.updateFriendConnection(existingConn.copy(status = "CANCELLED", updatedAt = now))
                }

                try {
                    val map = hashMapOf(
                        "id" to connId,
                        "status" to "CANCELLED",
                        "updatedAt" to now
                    )
                    firebaseFirestore?.collection("friend_requests")?.document(connId)?.set(map, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore cancel request error: ${e.localizedMessage}")
                }

                _friendActionMessage.value = "Cancelled friend request to ${user.fullName}"
            }
        }
    }

    fun onRemoveFriend(otherUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.allUsers.first().firstOrNull { it.id == otherUserId }
            val current = currentUser.value
            val currentUid = current?.id ?: "currentUser"
            val now = System.currentTimeMillis()

            if (user != null) {
                val updatedUser = user.copy(friendStatus = "NONE")
                repository.updateUser(updatedUser)

                val existingConn = repository.getFriendConnectionBetweenOnce(currentUid, otherUserId)
                val connId = existingConn?.id ?: "req_${currentUid}_${otherUserId}"
                if (existingConn != null) {
                    repository.updateFriendConnection(existingConn.copy(status = "UNFRIENDED", updatedAt = now))
                }

                try {
                    val map = hashMapOf(
                        "id" to connId,
                        "status" to "UNFRIENDED",
                        "updatedAt" to now
                    )
                    firebaseFirestore?.collection("friend_requests")?.document(connId)?.set(map, com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore remove friend error: ${e.localizedMessage}")
                }

                _friendActionMessage.value = "Removed ${user.fullName} from friends"
            }
        }
    }

    fun selectGroup(group: Group?) {
        _selectedGroupId.value = group?.id
    }

    fun selectGroupId(groupId: Int?) {
        _selectedGroupId.value = groupId
    }

    fun selectGroupPostForComments(postId: Int?) {
        _selectedGroupPostId.value = postId
    }

    fun onCreateGroup(
        name: String,
        description: String,
        avatarUrl: String,
        coverUrl: String,
        category: String,
        location: String,
        isPrivate: Boolean,
        onlyAdminsCanPost: Boolean,
        onSuccess: (Group) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val userId = user?.id ?: "currentUser"
            val userName = user?.fullName ?: "Community Member"
            val userAvatar = user?.avatarUrl ?: ""

            val newGroup = Group(
                name = name.trim(),
                description = description.trim(),
                avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150" },
                coverUrl = coverUrl.ifBlank { "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800" },
                category = category.ifBlank { "General" },
                location = location.ifBlank { "Chitral, Pakistan" },
                isPrivate = isPrivate,
                onlyAdminsCanPost = onlyAdminsCanPost,
                ownerId = userId,
                memberCount = 1,
                isJoined = true,
                joinStatus = "JOINED",
                createdAt = System.currentTimeMillis()
            )

            val generatedId = repository.insertGroup(newGroup).toInt()
            val insertedGroup = newGroup.copy(id = generatedId)

            // Insert Creator as OWNER in GroupMembers
            val ownerMember = GroupMember(
                groupId = generatedId,
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatar,
                role = "OWNER",
                joinedAt = System.currentTimeMillis()
            )
            repository.insertGroupMember(ownerMember)

            // Add Welcome Pinned Post
            val welcomePost = GroupPost(
                groupId = generatedId,
                authorId = userId,
                authorName = userName,
                authorAvatarUrl = userAvatar,
                authorRole = "OWNER",
                content = "🎉 Welcome to $name! Feel free to introduce yourself, share updates, and connect with fellow community members.",
                mediaType = if (coverUrl.isNotBlank()) "IMAGE" else "NONE",
                mediaUrlsJson = if (coverUrl.isNotBlank()) "[\"$coverUrl\"]" else "[]",
                location = location,
                isPinned = true,
                likesCount = 1,
                isLikedByMe = true,
                commentsCount = 0,
                timestamp = System.currentTimeMillis()
            )
            repository.insertGroupPost(welcomePost)

            // Firestore sync
            try {
                val groupMap = hashMapOf(
                    "id" to generatedId,
                    "name" to newGroup.name,
                    "description" to newGroup.description,
                    "avatarUrl" to newGroup.avatarUrl,
                    "coverUrl" to newGroup.coverUrl,
                    "category" to newGroup.category,
                    "location" to newGroup.location,
                    "isPrivate" to newGroup.isPrivate,
                    "onlyAdminsCanPost" to newGroup.onlyAdminsCanPost,
                    "ownerId" to userId,
                    "memberCount" to 1,
                    "createdAt" to newGroup.createdAt
                )
                firebaseFirestore?.collection("groups")?.document(generatedId.toString())?.set(groupMap)
            } catch (e: Exception) {
                android.util.Log.w("SocialMediaViewModel", "Firestore createGroup skipped: ${e.localizedMessage}")
            }

            // Notification
            val notif = AppNotification(
                id = "notif_group_created_${System.currentTimeMillis()}",
                recipientId = userId,
                senderId = userId,
                senderName = userName,
                senderAvatarUrl = user?.avatarUrl ?: "",
                title = "Group Created: $name",
                description = "Your group '$name' is now live! Invite members to get started.",
                avatarUrl = newGroup.avatarUrl,
                timestamp = System.currentTimeMillis(),
                type = "GROUP",
                targetId = generatedId.toString()
            )
            repository.insertNotification(notif)

            withContext(Dispatchers.Main) {
                _selectedGroupId.value = generatedId
                onSuccess(insertedGroup)
            }
        }
    }

    fun onJoinGroup(group: Group) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val userId = user?.id ?: "currentUser"
            val userName = user?.fullName ?: "Community Member"
            val userAvatar = user?.avatarUrl ?: ""

            if (group.isJoined) {
                // Leaving group
                val updatedGroup = group.copy(
                    isJoined = false,
                    joinStatus = "NONE",
                    memberCount = maxOf(0, group.memberCount - 1)
                )
                repository.updateGroup(updatedGroup)
                repository.deleteGroupMember(group.id, userId)
            } else {
                if (group.isPrivate) {
                    // Send Join Request
                    val existingReq = repository.getGroupJoinRequests(group.id).first().find { it.userId == userId }
                    if (existingReq == null) {
                        val req = GroupJoinRequest(
                            groupId = group.id,
                            groupName = group.name,
                            userId = userId,
                            userName = userName,
                            userAvatarUrl = userAvatar,
                            userBio = user?.bio ?: "",
                            timestamp = System.currentTimeMillis(),
                            status = "PENDING"
                        )
                        repository.insertGroupJoinRequest(req)
                        val updatedGroup = group.copy(joinStatus = "REQUESTED")
                        repository.updateGroup(updatedGroup)

                        // Notification to user
                        val notif = AppNotification(
                            id = "notif_join_req_${System.currentTimeMillis()}",
                            recipientId = userId,
                            senderId = userId,
                            senderName = userName,
                            senderAvatarUrl = userAvatar,
                            title = "Join Request Sent",
                            description = "Your request to join '${group.name}' was sent to the group admins for approval.",
                            avatarUrl = group.avatarUrl.ifBlank { group.coverUrl },
                            timestamp = System.currentTimeMillis(),
                            type = "GROUP",
                            targetId = group.id.toString()
                        )
                        repository.insertNotification(notif)

                        // Also alert group owner if known
                        if (group.ownerId.isNotBlank() && group.ownerId != userId) {
                            sendNotification(
                                recipientId = group.ownerId,
                                title = "New Join Request for ${group.name}",
                                description = "$userName requested to join your group '${group.name}'.",
                                avatarUrl = userAvatar,
                                type = "GROUP",
                                targetId = group.id.toString(),
                                senderId = userId,
                                senderName = userName,
                                senderAvatarUrl = userAvatar
                            )
                        }
                    } else {
                        // Cancel request
                        repository.deleteGroupJoinRequest(existingReq.id)
                        val updatedGroup = group.copy(joinStatus = "NONE")
                        repository.updateGroup(updatedGroup)
                    }
                } else {
                    // Public Group - Instant Join
                    val updatedGroup = group.copy(
                        isJoined = true,
                        joinStatus = "JOINED",
                        memberCount = group.memberCount + 1
                    )
                    repository.updateGroup(updatedGroup)
                    val member = GroupMember(
                        groupId = group.id,
                        userId = userId,
                        userName = userName,
                        userAvatarUrl = userAvatar,
                        role = "MEMBER",
                        joinedAt = System.currentTimeMillis()
                    )
                    repository.insertGroupMember(member)

                    // Notification
                    val notif = AppNotification(
                        id = "notif_joined_${System.currentTimeMillis()}",
                        recipientId = userId,
                        senderId = userId,
                        senderName = userName,
                        senderAvatarUrl = userAvatar,
                        title = "Joined ${group.name}",
                        description = "You are now a member of ${group.name}. Explore discussions and share posts!",
                        avatarUrl = group.avatarUrl.ifBlank { group.coverUrl },
                        timestamp = System.currentTimeMillis(),
                        type = "GROUP",
                        targetId = group.id.toString()
                    )
                    repository.insertNotification(notif)
                }
            }
        }
    }

    fun onApproveJoinRequest(request: GroupJoinRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroupJoinRequest(request.id)
            val newMember = GroupMember(
                groupId = request.groupId,
                userId = request.userId,
                userName = request.userName,
                userAvatarUrl = request.userAvatarUrl,
                role = "MEMBER",
                joinedAt = System.currentTimeMillis()
            )
            repository.insertGroupMember(newMember)

            val group = repository.getGroupByIdOnce(request.groupId)
            if (group != null) {
                val updatedGroup = group.copy(memberCount = group.memberCount + 1)
                repository.updateGroup(updatedGroup)
            }

            val currentUid = currentUser.value?.id ?: "currentUser"
            val currentName = currentUser.value?.fullName ?: "Group Admin"

            // Local Notification
            val notif = AppNotification(
                id = "notif_approved_${System.currentTimeMillis()}",
                recipientId = currentUid,
                senderId = request.userId,
                senderName = request.userName,
                senderAvatarUrl = request.userAvatarUrl,
                title = "Join Request Approved",
                description = "Approved ${request.userName} to join the group.",
                avatarUrl = request.userAvatarUrl,
                timestamp = System.currentTimeMillis(),
                type = "GROUP",
                targetId = request.groupId.toString()
            )
            repository.insertNotification(notif)

            // Alert the accepted user via Firestore
            sendNotification(
                recipientId = request.userId,
                title = "Join Request Approved!",
                description = "Your request to join '${group?.name ?: "the group"}' was approved.",
                avatarUrl = group?.avatarUrl ?: "",
                type = "GROUP",
                targetId = request.groupId.toString(),
                senderId = currentUid,
                senderName = currentName,
                senderAvatarUrl = currentUser.value?.avatarUrl ?: ""
            )
        }
    }

    fun onRejectJoinRequest(request: GroupJoinRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroupJoinRequest(request.id)
        }
    }

    fun onInviteUserToGroup(groupId: Int, targetUser: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val group = repository.getGroupByIdOnce(groupId) ?: return@launch

            val invite = GroupInvite(
                groupId = groupId,
                groupName = group.name,
                groupAvatarUrl = group.avatarUrl.ifBlank { group.coverUrl },
                inviterId = user?.id ?: "currentUser",
                inviterName = user?.fullName ?: "Community Member",
                inviteeId = targetUser.id,
                timestamp = System.currentTimeMillis(),
                status = "PENDING"
            )
            repository.insertGroupInvite(invite)

            val currentUid = user?.id ?: "currentUser"
            val currentName = user?.fullName ?: "Community Member"

            val notif = AppNotification(
                id = "notif_invite_sent_${System.currentTimeMillis()}",
                recipientId = currentUid,
                senderId = currentUid,
                senderName = currentName,
                senderAvatarUrl = user?.avatarUrl ?: "",
                title = "Invitation Sent",
                description = "Invited ${targetUser.fullName} to join '${group.name}'.",
                avatarUrl = targetUser.avatarUrl,
                timestamp = System.currentTimeMillis(),
                type = "GROUP",
                targetId = groupId.toString()
            )
            repository.insertNotification(notif)

            // Send real-time Firestore notification to invited user
            sendNotification(
                recipientId = targetUser.id,
                title = "Group Invitation: ${group.name}",
                description = "$currentName invited you to join '${group.name}'.",
                avatarUrl = group.avatarUrl.ifBlank { group.coverUrl },
                type = "GROUP",
                targetId = groupId.toString(),
                senderId = currentUid,
                senderName = currentName,
                senderAvatarUrl = user?.avatarUrl ?: ""
            )
        }
    }

    fun onRespondToGroupInvite(invite: GroupInvite, accept: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroupInvite(invite.id)
            if (accept) {
                val user = currentUser.value
                val newMember = GroupMember(
                    groupId = invite.groupId,
                    userId = user?.id ?: "currentUser",
                    userName = user?.fullName ?: "Community Member",
                    userAvatarUrl = user?.avatarUrl ?: "",
                    role = "MEMBER",
                    joinedAt = System.currentTimeMillis()
                )
                repository.insertGroupMember(newMember)

                val group = repository.getGroupByIdOnce(invite.groupId)
                if (group != null) {
                    val updated = group.copy(
                        isJoined = true,
                        joinStatus = "JOINED",
                        memberCount = group.memberCount + 1
                    )
                    repository.updateGroup(updated)
                }

                val currentUid = user?.id ?: "currentUser"
                val notif = AppNotification(
                    id = "notif_accepted_invite_${System.currentTimeMillis()}",
                    recipientId = currentUid,
                    senderId = invite.inviterId,
                    senderName = invite.inviterName,
                    senderAvatarUrl = invite.groupAvatarUrl,
                    title = "Joined ${invite.groupName}",
                    description = "You accepted the invitation and joined ${invite.groupName}.",
                    avatarUrl = invite.groupAvatarUrl,
                    timestamp = System.currentTimeMillis(),
                    type = "GROUP",
                    targetId = invite.groupId.toString()
                )
                repository.insertNotification(notif)
            }
        }
    }

    fun onPromoteDemoteMember(groupId: Int, member: GroupMember, newRole: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = member.copy(role = newRole)
            repository.updateGroupMember(updated)

            val currentUid = currentUser.value?.id ?: "currentUser"
            val notif = AppNotification(
                id = "notif_role_change_${System.currentTimeMillis()}",
                recipientId = currentUid,
                senderId = member.userId,
                senderName = member.userName,
                senderAvatarUrl = member.userAvatarUrl,
                title = "Role Updated",
                description = "Changed ${member.userName}'s role to $newRole.",
                avatarUrl = member.userAvatarUrl,
                timestamp = System.currentTimeMillis(),
                type = "GROUP",
                targetId = groupId.toString()
            )
            repository.insertNotification(notif)

            // Alert member
            sendNotification(
                recipientId = member.userId,
                title = "Role Updated in Group",
                description = "Your role in the group has been changed to $newRole.",
                avatarUrl = member.userAvatarUrl,
                type = "GROUP",
                targetId = groupId.toString()
            )
        }
    }

    fun onRemoveGroupMember(groupId: Int, member: GroupMember) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroupMember(groupId, member.userId)
            val group = repository.getGroupByIdOnce(groupId)
            if (group != null) {
                repository.updateGroup(group.copy(memberCount = maxOf(0, group.memberCount - 1)))
            }
        }
    }

    fun onBlockGroupMember(groupId: Int, member: GroupMember) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGroupMember(member.copy(isBlocked = true))
        }
    }

    fun onUpdateGroupDetails(
        groupId: Int,
        name: String,
        description: String,
        avatarUrl: String,
        coverUrl: String,
        category: String,
        location: String,
        isPrivate: Boolean,
        onlyAdminsCanPost: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val group = repository.getGroupByIdOnce(groupId) ?: return@launch
            val updated = group.copy(
                name = name.trim(),
                description = description.trim(),
                avatarUrl = avatarUrl.trim(),
                coverUrl = coverUrl.trim(),
                category = category.trim(),
                location = location.trim(),
                isPrivate = isPrivate,
                onlyAdminsCanPost = onlyAdminsCanPost
            )
            repository.updateGroup(updated)
        }
    }

    fun onDeleteGroup(groupId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroup(groupId)
            withContext(Dispatchers.Main) {
                _selectedGroupId.value = null
            }
        }
    }

    fun onReportGroup(groupId: Int, reason: String, details: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val currentUid = user?.id ?: "currentUser"
            val report = GroupReport(
                groupId = groupId,
                reporterId = currentUid,
                reason = reason,
                details = details,
                timestamp = System.currentTimeMillis()
            )
            repository.insertGroupReport(report)

            val notif = AppNotification(
                id = "notif_report_${System.currentTimeMillis()}",
                recipientId = currentUid,
                senderId = "system",
                senderName = "Yarkhoon Support",
                senderAvatarUrl = "",
                title = "Group Reported",
                description = "Thank you. Our moderators will review this group within 24 hours.",
                avatarUrl = "",
                timestamp = System.currentTimeMillis(),
                type = "SYSTEM",
                targetId = groupId.toString()
            )
            repository.insertNotification(notif)
        }
    }

    fun onCreateGroupPost(
        groupId: Int,
        content: String,
        mediaType: String,
        mediaUrls: List<String>,
        location: String
    ) {
        if (content.isBlank() && mediaUrls.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val userId = user?.id ?: "currentUser"
            val member = repository.getGroupMember(groupId, userId)
            val authorRole = member?.role ?: if (user?.isVerified == true) "ADMIN" else "MEMBER"

            val jsonUrls = JSONArray(mediaUrls).toString()
            val newPost = GroupPost(
                groupId = groupId,
                authorId = userId,
                authorName = user?.fullName ?: "Community Member",
                authorAvatarUrl = user?.avatarUrl ?: "",
                authorRole = authorRole,
                content = content.trim(),
                mediaType = mediaType,
                mediaUrlsJson = jsonUrls,
                location = location.trim(),
                isPinned = false,
                likesCount = 0,
                isLikedByMe = false,
                commentsCount = 0,
                timestamp = System.currentTimeMillis()
            )
            repository.insertGroupPost(newPost)

            // Notification
            val group = repository.getGroupByIdOnce(groupId)
            val notif = AppNotification(
                id = "notif_group_post_${System.currentTimeMillis()}",
                recipientId = userId,
                senderId = userId,
                senderName = user?.fullName ?: "Community Member",
                senderAvatarUrl = user?.avatarUrl ?: "",
                title = "Post Published in ${group?.name ?: "Group"}",
                description = "Your post is now visible to group members.",
                avatarUrl = user?.avatarUrl ?: "",
                timestamp = System.currentTimeMillis(),
                type = "GROUP",
                targetId = groupId.toString()
            )
            repository.insertNotification(notif)

            // Broadcast notification for group activity
            sendNotification(
                recipientId = "all",
                title = "New post in ${group?.name ?: "Group"}",
                description = "${user?.fullName ?: "A member"} posted in ${group?.name ?: "the group"}.",
                avatarUrl = user?.avatarUrl ?: "",
                type = "GROUP",
                targetId = groupId.toString(),
                senderId = userId,
                senderName = user?.fullName ?: "Community Member",
                senderAvatarUrl = user?.avatarUrl ?: ""
            )
        }
    }

    fun onToggleLikeGroupPost(post: GroupPost) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = post.copy(
                isLikedByMe = !post.isLikedByMe,
                likesCount = if (post.isLikedByMe) post.likesCount - 1 else post.likesCount + 1
            )
            repository.updateGroupPost(updated)
        }
    }

    fun onTogglePinGroupPost(post: GroupPost) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = post.copy(isPinned = !post.isPinned)
            repository.updateGroupPost(updated)
        }
    }

    fun onDeleteGroupPost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroupPost(postId)
        }
    }

    fun onAddGroupPostComment(
        groupPost: GroupPost,
        content: String,
        parentCommentId: Int? = null,
        replyToAuthorName: String? = null
    ) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val currentUid = user?.id ?: "currentUser"
            val comment = GroupPostComment(
                groupPostId = groupPost.id,
                authorId = currentUid,
                authorName = user?.fullName ?: "Community Member",
                authorAvatarUrl = user?.avatarUrl ?: "",
                content = content.trim(),
                parentCommentId = parentCommentId,
                replyToAuthorName = replyToAuthorName,
                timestamp = System.currentTimeMillis()
            )
            repository.insertGroupPostComment(comment)

            val updatedPost = groupPost.copy(commentsCount = groupPost.commentsCount + 1)
            repository.updateGroupPost(updatedPost)

            // Local Notification
            val notif = AppNotification(
                id = "notif_comment_${System.currentTimeMillis()}",
                recipientId = currentUid,
                senderId = currentUid,
                senderName = user?.fullName ?: "Community Member",
                senderAvatarUrl = user?.avatarUrl ?: "",
                title = "New Comment in Group",
                description = "${user?.fullName ?: "Someone"} commented on a group post.",
                avatarUrl = user?.avatarUrl ?: "",
                timestamp = System.currentTimeMillis(),
                type = "COMMENT",
                targetId = groupPost.groupId.toString()
            )
            repository.insertNotification(notif)

            // If comment on someone else's group post, alert author
            if (groupPost.authorId != currentUid && groupPost.authorId.isNotBlank()) {
                sendNotification(
                    recipientId = groupPost.authorId,
                    title = "New Comment on your Group Post",
                    description = "${user?.fullName ?: "Someone"} commented on your post: \"${if (content.length > 50) content.take(47) + "..." else content}\"",
                    avatarUrl = user?.avatarUrl ?: "",
                    type = "COMMENT",
                    targetId = groupPost.groupId.toString(),
                    senderId = currentUid,
                    senderName = user?.fullName ?: "Community Member",
                    senderAvatarUrl = user?.avatarUrl ?: ""
                )
            }
        }
    }

    fun onDeleteGroupPostComment(commentId: Int, groupPost: GroupPost) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroupPostComment(commentId)
            val updatedPost = groupPost.copy(commentsCount = maxOf(0, groupPost.commentsCount - 1))
            repository.updateGroupPost(updatedPost)
        }
    }

    fun onSendChatMessage(theirId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val myId = user?.id ?: "currentUser"
            val myName = user?.fullName?.ifBlank { user.username } ?: "Community Member"
            val myAvatar = user?.avatarUrl ?: ""
            val now = System.currentTimeMillis()

            val newMessage = ChatMessage(
                senderId = myId,
                receiverId = theirId,
                content = content,
                timestamp = now,
                isVoiceMessage = false
            )
            repository.insertChatMessage(newMessage)

            // Firestore sync for chat messages
            try {
                val messageMap = hashMapOf(
                    "senderId" to newMessage.senderId,
                    "receiverId" to newMessage.receiverId,
                    "content" to newMessage.content,
                    "timestamp" to newMessage.timestamp,
                    "isVoiceMessage" to false
                )
                firebaseFirestore?.collection("chat_messages")?.add(messageMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore chat message sync error: ${e.localizedMessage}")
            }

            // Real-time Firestore notification alert to recipient
            sendNotification(
                recipientId = theirId,
                title = "New Message from $myName",
                description = if (content.length > 80) content.take(77) + "..." else content,
                avatarUrl = myAvatar,
                type = "MESSAGE",
                targetId = myId,
                senderId = myId,
                senderName = myName,
                senderAvatarUrl = myAvatar
            )
        }
    }

    fun onSendVoiceChatMessage(
        theirId: String,
        transcription: String,
        voiceLang: String = "Khowar",
        durationSec: Int = 5,
        audioFilePath: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val myId = user?.id ?: "currentUser"
            val myName = user?.fullName?.ifBlank { user.username } ?: "Community Member"
            val myAvatar = user?.avatarUrl ?: ""
            val now = System.currentTimeMillis()

            val newMessage = ChatMessage(
                senderId = myId,
                receiverId = theirId,
                content = transcription.ifBlank { "🎤 Voice Note (${durationSec}s)" },
                timestamp = now,
                isVoiceMessage = true,
                audioDurationSec = durationSec,
                audioUrl = audioFilePath,
                voiceLanguage = voiceLang
            )
            repository.insertChatMessage(newMessage)

            // Firestore sync for voice messages
            try {
                val messageMap = hashMapOf(
                    "senderId" to newMessage.senderId,
                    "receiverId" to newMessage.receiverId,
                    "content" to newMessage.content,
                    "timestamp" to newMessage.timestamp,
                    "isVoiceMessage" to true,
                    "audioDurationSec" to durationSec,
                    "audioUrl" to audioFilePath,
                    "voiceLanguage" to voiceLang
                )
                firebaseFirestore?.collection("chat_messages")?.add(messageMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore voice message sync error: ${e.localizedMessage}")
            }

            sendNotification(
                recipientId = theirId,
                title = "🎤 Voice Message from $myName",
                description = "Sent an audio message in ${if (voiceLang.lowercase() == "khowar" || voiceLang == "kho") "Khowar" else if (voiceLang.lowercase() == "urdu" || voiceLang == "ur") "Urdu" else "English"} (${durationSec}s)",
                avatarUrl = myAvatar,
                type = "MESSAGE",
                targetId = myId,
                senderId = myId,
                senderName = myName,
                senderAvatarUrl = myAvatar
            )
        }
    }

    fun onCreateMarketplaceItem(title: String, description: String, price: Double, category: String, imageUrl: String, sellerContact: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val newItem = MarketplaceItem(
                title = title,
                description = description,
                price = price,
                imageUrl = imageUrl,
                category = category,
                sellerId = user?.id ?: "currentUser",
                sellerName = user?.fullName ?: "Hamara Chitral",
                sellerContact = sellerContact.ifBlank { "+92-345-1234567" },
                isSold = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertMarketplaceItem(newItem)
        }
    }

    fun onCreateServiceListing(serviceType: String, description: String, phoneNumber: String, imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val newService = ServiceListing(
                providerId = user?.id ?: "currentUser",
                providerName = user?.fullName ?: "Hamara Chitral User",
                providerAvatarUrl = user?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop",
                serviceType = serviceType,
                description = description,
                phoneNumber = phoneNumber.ifBlank { "+92-345-1234567" },
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )
            repository.insertServiceListing(newService)
        }
    }

    fun onDeleteServiceListing(listingId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteServiceListing(listingId)
        }
    }

    fun onToggleMarketplaceItemSold(item: MarketplaceItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = item.copy(isSold = !item.isSold)
            repository.updateMarketplaceItem(updated)
        }
    }

    fun onDeleteMarketplaceItem(itemId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMarketplaceItem(itemId)
        }
    }

    fun onUpdateProfile(fullName: String, bio: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            if (user != null) {
                val updated = user.copy(
                    fullName = fullName,
                    bio = bio
                )
                repository.updateUser(updated)

                try {
                    val userMap = hashMapOf(
                        "id" to updated.id,
                        "username" to updated.username,
                        "fullName" to updated.fullName,
                        "bio" to updated.bio,
                        "avatarUrl" to updated.avatarUrl,
                        "coverUrl" to updated.coverUrl,
                        "email" to updated.email
                    )
                    firebaseFirestore?.collection("users")?.document(updated.id)?.set(userMap)
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firestore update profile sync error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onCompleteRegistration(fullName: String, username: String, email: String, password: String, bio: String, avatarUrl: String, coverUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val cleanEmail = email.trim().lowercase()

            // Firebase Auth Integration: create user in Firebase Auth
            if (cleanEmail.contains("@") && password.length >= 6) {
                try {
                    firebaseAuth?.createUserWithEmailAndPassword(cleanEmail, password)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                android.util.Log.d("SocialMediaViewModel", "Firebase Auth registered successfully: $cleanEmail")
                            } else {
                                // If account exists in Firebase Auth, attempt sign-in
                                firebaseAuth?.signInWithEmailAndPassword(cleanEmail, password)
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firebase Auth registration exception: ${e.localizedMessage}")
                }
            }

            // Set all other users as not current
            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }

            // Create a persistent completed user with username as ID
            val cleanUsername = username.trim().lowercase()
            val completeUser = User(
                id = cleanUsername,
                username = cleanUsername,
                fullName = fullName.trim(),
                bio = bio.trim(),
                avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop" },
                coverUrl = coverUrl.ifBlank { "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop" },
                isCurrentUser = true,
                isProfileCompleted = true,
                email = cleanEmail,
                password = password
            )
            repository.insertUsers(listOf(completeUser))

            // Sync user profile to Firestore
            try {
                val userMap = hashMapOf(
                    "id" to completeUser.id,
                    "username" to completeUser.username,
                    "fullName" to completeUser.fullName,
                    "bio" to completeUser.bio,
                    "avatarUrl" to completeUser.avatarUrl,
                    "coverUrl" to completeUser.coverUrl,
                    "email" to completeUser.email
                )
                firebaseFirestore?.collection("users")?.document(completeUser.id)?.set(userMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore complete registration sync error: ${e.localizedMessage}")
            }

            // Delete the placeholder user "currentUser" if that was the one being set up
            if (user != null && user.id == "currentUser") {
                repository.deleteUser("currentUser")
            }
        }
    }

    fun onAdminLoginSuccess() {
        viewModelScope.launch(Dispatchers.IO) {
            // Firebase Auth: sync admin account with Firebase Auth
            try {
                firebaseAuth?.signInWithEmailAndPassword("ceo@yarkhoon.com", "chitrali@786")
                    ?.addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            firebaseAuth?.createUserWithEmailAndPassword("ceo@yarkhoon.com", "chitrali@786")
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firebase Auth admin login exception: ${e.localizedMessage}")
            }

            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }

            // Ensure admin user exists and is current
            val adminUser = User(
                id = "admin",
                username = "ceo",
                fullName = "Valley Administrator",
                bio = "Valley Administrator",
                avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop",
                isCurrentUser = true,
                isProfileCompleted = true,
                email = "ceo@yarkhoon.com",
                password = "chitrali@786",
                isVerified = true
            )
            repository.insertUsers(listOf(adminUser))
        }
    }

    fun onSignOut() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firebase Auth sign out exception: ${e.localizedMessage}")
            }

            val user = currentUser.value
            if (user != null) {
                val updated = user.copy(isCurrentUser = false)
                repository.updateUser(updated)
                _activeChatUserId.value = null
            }
        }
    }

    fun onSignIn(emailOrUsername: String, passwordText: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanInput = emailOrUsername.trim().lowercase()
            val cleanPass = passwordText.trim()

            // Firebase Auth Sign In Attempt (if email format provided)
            if (cleanInput.contains("@") && cleanPass.isNotEmpty()) {
                try {
                    firebaseAuth?.signInWithEmailAndPassword(cleanInput, cleanPass)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                android.util.Log.d("SocialMediaViewModel", "Firebase Auth sign-in successful for $cleanInput")
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firebase Auth sign-in exception: ${e.localizedMessage}")
                }
            }

            // Match against Room users database
            val all = repository.allUsers.first()
            val matchedUser = all.find { 
                (it.username.lowercase() == cleanInput || it.email.lowercase() == cleanInput) && 
                (it.password == cleanPass || (it.id == "admin" && (cleanPass == "chitrali@786" || cleanPass == "adminpassword123" || cleanPass == "admin123"))) && 
                it.isProfileCompleted 
            }

            if (matchedUser != null) {
                // Set all other users as not current
                for (u in all) {
                    if (u.isCurrentUser) {
                        repository.updateUser(u.copy(isCurrentUser = false))
                    }
                }
                // Sign in this matching user
                repository.updateUser(matchedUser.copy(isCurrentUser = true))
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } else if ((cleanInput == "ceo@yarkhoon.com" || cleanInput == "admin@yarkhoon.com" || cleanInput == "ceo" || cleanInput == "admin") && 
                       (cleanPass == "chitrali@786" || cleanPass == "adminpassword123" || cleanPass == "admin123")) {
                // Compatibility for admin credentials
                onAdminLoginSuccess()
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun onSignInUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }
            repository.updateUser(user.copy(isCurrentUser = true))
        }
    }

    fun initiateNewSignUp() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allUsers.first()
            for (u in all) {
                if (u.isCurrentUser) {
                    repository.updateUser(u.copy(isCurrentUser = false))
                }
            }
            val defaultCurrentUser = User(
                id = "currentUser",
                username = "",
                fullName = "",
                avatarUrl = "",
                coverUrl = "",
                bio = "",
                friendStatus = "NONE",
                isCurrentUser = true,
                isProfileCompleted = false
            )
            repository.insertUsers(listOf(defaultCurrentUser))
        }
    }

    fun onCancelSignUp() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUser("currentUser")
        }
    }

    fun onResetProfile() {
        onSignOut()
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePost(postId)
        }
    }

    fun insertGroups(groups: List<Group>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGroups(groups)
        }
    }

    // --- Admin Dashboard Controller Methods ---

    fun onTogglePostViral(postId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allPosts.first()
            val post = all.find { it.id == postId }
            if (post != null) {
                repository.updatePost(post.copy(isViral = !post.isViral))
            }
        }
    }

    fun onUpdatePostContent(postId: Int, newContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allPosts.first()
            val post = all.find { it.id == postId }
            if (post != null) {
                repository.updatePost(post.copy(content = newContent))
            }
        }
    }

    fun onToggleUserVerified(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.allUsers.first()
            val user = all.find { it.id == userId }
            if (user != null) {
                repository.updateUser(user.copy(isVerified = !user.isVerified))
            }
        }
    }

    fun onDeleteUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUser(userId)
        }
    }

    fun onAdminCreatePost(content: String, mediaType: String, mediaUrl: String, asYarkhoon: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (asYarkhoon) {
                val newPost = Post(
                    authorId = "user_yarkhoon",
                    authorName = "Yarkhoon.com",
                    authorAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop",
                    content = content,
                    mediaType = mediaType,
                    mediaUrl = mediaUrl,
                    timestamp = System.currentTimeMillis(),
                    likesCount = 3890,
                    isLikedByMe = true,
                    commentsCount = 472,
                    isViral = true // Goes viral reaching all users immediately
                )
                repository.insertPost(newPost)
            } else {
                onCreatePost(content, mediaType, mediaUrl)
            }
        }
    }

    // --- Profile Zoom Controller ---
    fun openZoomedProfile(imageUrl: String, userName: String = "", subtitle: String = "", userId: String? = null) {
        if (imageUrl.isNotBlank()) {
            _zoomedProfile.value = ZoomedProfileState(imageUrl, userName, subtitle, userId)
        }
    }

    fun closeZoomedProfile() {
        _zoomedProfile.value = null
    }

    // --- Story Operations ---
    fun onCreateStory(
        context: Context,
        mediaType: String,
        mediaUri: Uri?,
        textCaption: String,
        backgroundColorHex: String,
        textColorHex: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val user = currentUser.value
                val authorId = user?.id ?: "currentUser"
                val authorName = user?.fullName ?: "Hamara Chitral"
                val authorAvatar = user?.avatarUrl ?: ""
                val storyId = "story_${UUID.randomUUID().toString().take(12)}"
                val now = System.currentTimeMillis()
                val expiresAt = now + 86400000L // 24 hours

                var finalMediaUrl = ""

                if (mediaUri != null && (mediaType == "IMAGE" || mediaType == "VIDEO")) {
                    if (mediaType == "IMAGE") {
                        val (compressedBytes, compressedFile) = MediaUtils.compressImage(context, mediaUri)
                        if (compressedBytes.isNotEmpty()) {
                            finalMediaUrl = MediaUtils.uploadToFirebaseStorage(
                                context = context,
                                bytes = compressedBytes,
                                folder = "stories",
                                contentType = "image/jpeg",
                                fallbackFile = compressedFile
                            )
                        }
                        if (finalMediaUrl.isBlank()) {
                            finalMediaUrl = mediaUri.toString()
                        }
                    } else {
                        try {
                            val inputStream = context.contentResolver.openInputStream(mediaUri)
                            val bytes = inputStream?.readBytes() ?: ByteArray(0)
                            inputStream?.close()
                            if (bytes.isNotEmpty()) {
                                finalMediaUrl = MediaUtils.uploadToFirebaseStorage(
                                    context = context,
                                    bytes = bytes,
                                    folder = "stories",
                                    contentType = "video/mp4",
                                    fallbackFile = null
                                )
                            }
                            if (finalMediaUrl.isBlank()) {
                                finalMediaUrl = mediaUri.toString()
                            }
                        } catch (e: Exception) {
                            finalMediaUrl = mediaUri.toString()
                        }
                    }
                }

                val newStory = Story(
                    id = storyId,
                    authorId = authorId,
                    authorName = authorName,
                    authorAvatarUrl = authorAvatar,
                    mediaType = mediaType,
                    mediaUrl = finalMediaUrl,
                    textCaption = textCaption,
                    backgroundColorHex = backgroundColorHex,
                    textColorHex = textColorHex,
                    timestamp = now,
                    expiresAt = expiresAt,
                    viewersCount = 0,
                    viewersJson = "[]"
                )

                repository.insertStory(newStory)

                // Sync to Firestore
                try {
                    val storyMap = hashMapOf(
                        "id" to newStory.id,
                        "authorId" to newStory.authorId,
                        "authorName" to newStory.authorName,
                        "authorAvatarUrl" to newStory.authorAvatarUrl,
                        "mediaType" to newStory.mediaType,
                        "mediaUrl" to newStory.mediaUrl,
                        "textCaption" to newStory.textCaption,
                        "backgroundColorHex" to newStory.backgroundColorHex,
                        "textColorHex" to newStory.textColorHex,
                        "timestamp" to newStory.timestamp,
                        "expiresAt" to newStory.expiresAt,
                        "viewersCount" to newStory.viewersCount,
                        "viewersJson" to newStory.viewersJson
                    )
                    firebaseFirestore?.collection("stories")?.document(newStory.id)?.set(storyMap)
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firestore story upload error: ${e.localizedMessage}")
                }

                withContext(Dispatchers.Main) {
                    onComplete(true)
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Failed to create story: ${e.localizedMessage}", e)
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }

    fun onEditStory(storyId: String, newCaption: String, newBgColor: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val story = repository.getStoryById(storyId)
            if (story != null) {
                val updated = story.copy(
                    textCaption = newCaption,
                    backgroundColorHex = newBgColor
                )
                repository.updateStory(updated)

                try {
                    firebaseFirestore?.collection("stories")?.document(storyId)
                        ?.update(mapOf("textCaption" to newCaption, "backgroundColorHex" to newBgColor))
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firestore story edit error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onDeleteStory(storyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteStory(storyId)
            try {
                firebaseFirestore?.collection("stories")?.document(storyId)?.delete()
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore story deletion error: ${e.localizedMessage}")
            }
        }
    }

    fun onMarkStoryViewed(storyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value ?: return@launch
            val story = repository.getStoryById(storyId) ?: return@launch

            if (story.authorId == user.id) return@launch

            val viewersArray = try {
                JSONArray(story.viewersJson)
            } catch (e: Exception) {
                JSONArray()
            }

            var alreadyViewed = false
            for (i in 0 until viewersArray.length()) {
                val obj = viewersArray.getJSONObject(i)
                if (obj.optString("userId") == user.id) {
                    alreadyViewed = true
                    break
                }
            }

            if (!alreadyViewed) {
                val newViewerObj = JSONObject().apply {
                    put("userId", user.id)
                    put("userName", user.fullName.ifBlank { user.username })
                    put("userAvatarUrl", user.avatarUrl)
                    put("timestamp", System.currentTimeMillis())
                }
                viewersArray.put(newViewerObj)
                val updatedJson = viewersArray.toString()
                val updatedCount = viewersArray.length()

                val updatedStory = story.copy(
                    viewersCount = updatedCount,
                    viewersJson = updatedJson
                )
                repository.updateStory(updatedStory)

                try {
                    firebaseFirestore?.collection("stories")?.document(storyId)?.update(
                        mapOf(
                            "viewersCount" to updatedCount,
                            "viewersJson" to updatedJson
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firestore mark story viewed error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onReplyToStory(story: Story, replyText: String) {
        if (replyText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val myId = user?.id ?: "currentUser"
            val messageContent = if (story.mediaType == "TEXT") {
                "Replied to story: \"${story.textCaption.take(40)}\"\n💬 $replyText"
            } else {
                "Replied to your story 📸:\n💬 $replyText"
            }

            val newMessage = ChatMessage(
                senderId = myId,
                receiverId = story.authorId,
                content = messageContent,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(newMessage)

            try {
                val messageMap = hashMapOf(
                    "senderId" to newMessage.senderId,
                    "receiverId" to newMessage.receiverId,
                    "content" to newMessage.content,
                    "timestamp" to newMessage.timestamp
                )
                firebaseFirestore?.collection("chat_messages")?.add(messageMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore story reply sync error: ${e.localizedMessage}")
            }
        }
    }

    // ==================== GOOGLE SIGN-IN WITH FIREBASE AUTH & FIRESTORE ====================

    fun signInWithGoogleAccount(
        email: String,
        displayName: String,
        photoUrl: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cleanEmail = email.trim().lowercase()
                val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
                val cleanUsername = cleanEmail.substringBefore("@").replace(".", "_")
                val cleanPhoto = photoUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" }
                val userId = "user_${cleanUsername.take(16)}"

                // 1. Firebase Auth Attempt or account linking
                try {
                    // Try to sign in or create user in Firebase Auth
                    firebaseAuth?.signInWithEmailAndPassword(cleanEmail, "GoogleAuth123#")
                        ?.addOnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                firebaseAuth?.createUserWithEmailAndPassword(cleanEmail, "GoogleAuth123#")
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firebase Auth Google flow note: ${e.localizedMessage}")
                }

                // 2. Set current user in Room Database
                val all = repository.allUsers.first()
                for (u in all) {
                    if (u.isCurrentUser) {
                        repository.updateUser(u.copy(isCurrentUser = false))
                    }
                }

                val existingUser = all.find { it.email.lowercase() == cleanEmail || it.id == userId }
                val authenticatedUser = if (existingUser != null) {
                    existingUser.copy(
                        fullName = cleanName,
                        avatarUrl = cleanPhoto,
                        isCurrentUser = true,
                        isProfileCompleted = true
                    )
                } else {
                    User(
                        id = userId,
                        username = cleanUsername,
                        fullName = cleanName,
                        avatarUrl = cleanPhoto,
                        coverUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop",
                        bio = "Chitral Valley Explorer | Connected via Google Sign-In",
                        email = cleanEmail,
                        friendStatus = "NONE",
                        isCurrentUser = true,
                        isProfileCompleted = true,
                        isVerified = true
                    )
                }

                repository.insertUsers(listOf(authenticatedUser))

                // 3. Persist and Sync to Firestore
                try {
                    val userDoc = hashMapOf(
                        "id" to authenticatedUser.id,
                        "username" to authenticatedUser.username,
                        "fullName" to authenticatedUser.fullName,
                        "email" to authenticatedUser.email,
                        "avatarUrl" to authenticatedUser.avatarUrl,
                        "coverUrl" to authenticatedUser.coverUrl,
                        "bio" to authenticatedUser.bio,
                        "authProvider" to "google.com",
                        "isVerified" to authenticatedUser.isVerified,
                        "lastLogin" to System.currentTimeMillis()
                    )
                    firebaseFirestore?.collection("users")?.document(authenticatedUser.id)?.set(userDoc)
                } catch (e: Exception) {
                    android.util.Log.e("SocialMediaViewModel", "Firestore user persistence error: ${e.localizedMessage}")
                }

                withContext(Dispatchers.Main) {
                    onResult(true, "Signed in successfully as $cleanName via Google & Firebase Auth")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Authentication error: ${e.localizedMessage}")
                }
            }
        }
    }

    // ==================== GEMINI AI ASSISTANT & CHATBOT ====================

    fun sendGeminiChatMessage(text: String) {
        val prompt = text.trim()
        if (prompt.isBlank()) return

        val userMessage = AiChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            role = "user",
            content = prompt,
            timestamp = System.currentTimeMillis(),
            modelUsed = selectedChatModel.value
        )
        _geminiChatMessages.value = _geminiChatMessages.value + userMessage
        isAiChatGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentHistory = _geminiChatMessages.value.map { Pair(it.role, it.content) }
                val response = GeminiService.generateChatResponse(
                    model = selectedChatModel.value,
                    systemInstruction = chatSystemInstruction.value,
                    conversationHistory = currentHistory,
                    enableSearchGrounding = isSearchGroundingEnabled.value
                )

                val modelMessage = AiChatMessage(
                    id = "msg_res_${System.currentTimeMillis()}",
                    role = "model",
                    content = response.text,
                    timestamp = System.currentTimeMillis(),
                    modelUsed = selectedChatModel.value,
                    searchCitations = response.searchCitations
                )
                _geminiChatMessages.value = _geminiChatMessages.value + modelMessage

                // Persist chat to Firestore
                try {
                    val currentUid = currentUser.value?.id ?: "guest"
                    val chatDoc = hashMapOf(
                        "userId" to currentUid,
                        "prompt" to prompt,
                        "response" to response.text,
                        "model" to selectedChatModel.value,
                        "searchGrounding" to isSearchGroundingEnabled.value,
                        "timestamp" to System.currentTimeMillis()
                    )
                    firebaseFirestore?.collection("ai_chats")?.add(chatDoc)
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore chat log skipped: ${e.localizedMessage}")
                }
            } catch (e: Exception) {
                val errorMsg = AiChatMessage(
                    id = "msg_err_${System.currentTimeMillis()}",
                    role = "model",
                    content = "Unable to process request right now: ${e.localizedMessage}",
                    timestamp = System.currentTimeMillis(),
                    modelUsed = selectedChatModel.value
                )
                _geminiChatMessages.value = _geminiChatMessages.value + errorMsg
            } finally {
                isAiChatGenerating.value = false
            }
        }
    }

    fun setSelectedChatModel(model: String) {
        selectedChatModel.value = model
    }

    fun setChatSystemInstruction(instruction: String) {
        chatSystemInstruction.value = instruction
    }

    fun setSearchGroundingEnabled(enabled: Boolean) {
        isSearchGroundingEnabled.value = enabled
    }

    fun clearGeminiChat() {
        _geminiChatMessages.value = listOf(
            AiChatMessage(
                id = "welcome_ai_new_${System.currentTimeMillis()}",
                role = "model",
                content = "Conversation restarted. How can I assist you with Chitral Valley queries or creative generation?",
                timestamp = System.currentTimeMillis(),
                modelUsed = selectedChatModel.value
            )
        )
    }

    // ==================== ADVANCED AI VOICE ASSISTANT (KHOWAR / URDU / ENGLISH) ====================

    fun openVoiceAssistant(mode: VoiceAssistantMode = VoiceAssistantMode.CONVERSATION) {
        voiceAssistantManager.setMode(mode)
        isLiveVoiceActive.value = true
        isVoiceAssistantSheetOpen.value = true
    }

    fun closeVoiceAssistant() {
        isVoiceAssistantSheetOpen.value = false
        isLiveVoiceActive.value = false
        isLiveVoiceListening.value = false
        voiceAssistantManager.stopListening()
        voiceAssistantManager.stopSpeaking()
        voiceAssistantManager.stopAudioPlayback()
    }

    fun toggleLiveVoiceSession() {
        if (isLiveVoiceActive.value) {
            isLiveVoiceActive.value = false
            isLiveVoiceListening.value = false
            liveVoiceStatusText.value = "Voice session ended"
            voiceAssistantManager.stopListening()
            voiceAssistantManager.stopSpeaking()
        } else {
            isLiveVoiceActive.value = true
            isLiveVoiceListening.value = true
            val lang = voiceAssistantManager.selectedLanguage.value
            val targetLocale = voiceAssistantManager.selectedLocale.value
            liveVoiceStatusText.value = "Listening in ${lang.displayName} (${lang.nativeName}) [${targetLocale.toLanguageTag()}]... Speak now!"
            if (liveVoiceTranscript.value.isEmpty()) {
                val greeting = when (lang) {
                    VoiceLanguage.KHOWAR -> "سلام! جوشپہ کوسوری؟ مہ نام یارخون AI شیر۔ تہ کیا کمک کوروم؟"
                    VoiceLanguage.URDU -> "السلام علیکم! میں یارخون اے آئی ہوں، آپ کی کیا مدد کر سکتا ہوں؟"
                    VoiceLanguage.ENGLISH -> "Hello! I am Yarkhoon AI Voice Assistant. How can I help you today?"
                }
                liveVoiceTranscript.value = listOf("AI" to greeting)
                voiceAssistantManager.speakText(greeting, lang)
            }
        }
    }

    fun setVoiceLanguage(language: VoiceLanguage, locale: java.util.Locale? = null) {
        voiceAssistantManager.setLanguage(language, locale)
        val targetLocale = locale ?: language.locale
        liveVoiceStatusText.value = "Language set to ${language.displayName} (${language.nativeName}) [${targetLocale.toLanguageTag()}]"
    }

    fun toggleVoiceLanguage() {
        val newLang = voiceAssistantManager.toggleLanguage()
        val targetLocale = voiceAssistantManager.selectedLocale.value
        liveVoiceStatusText.value = "Toggled to ${newLang.displayName} (${newLang.nativeName}) [${targetLocale.toLanguageTag()}]"
    }

    fun submitLiveVoiceTurn(spokenInput: String) {
        val input = spokenInput.trim()
        if (input.isBlank()) return

        liveVoiceTranscript.value = liveVoiceTranscript.value + Pair("You", input)
        isLiveVoiceListening.value = false
        val activeLang = voiceAssistantManager.selectedLanguage.value
        liveVoiceStatusText.value = "Thinking in ${activeLang.displayName} & Gemini AI..."
        voiceAssistantManager.setProcessing(true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reply = voiceAssistantManager.processVoiceDialogueTurn(
                    spokenText = input,
                    conversationHistory = liveVoiceTranscript.value,
                    targetLang = activeLang
                )
                withContext(Dispatchers.Main) {
                    voiceAssistantManager.setProcessing(false)
                    liveVoiceTranscript.value = liveVoiceTranscript.value + Pair("Yarkhoon AI", reply)
                    liveVoiceStatusText.value = "Speaking response in ${activeLang.displayName}..."
                    voiceAssistantManager.speakText(reply, activeLang) {
                        // After speaking completes, automatically listen again if continuous conversation is enabled
                        val shouldContinue = voiceAssistantManager.isContinuousConversation.value &&
                                (isLiveVoiceActive.value || isVoiceAssistantSheetOpen.value)

                        if (shouldContinue) {
                            liveVoiceStatusText.value = "Listening in ${activeLang.displayName}... Speak now!"
                            isLiveVoiceListening.value = true
                            voiceAssistantManager.startListening(
                                lang = activeLang,
                                onFinalResult = { nextSpokenInput ->
                                    if (nextSpokenInput.isNotBlank()) {
                                        submitLiveVoiceTurn(nextSpokenInput)
                                    }
                                }
                            )
                        } else {
                            liveVoiceStatusText.value = "Tap microphone to speak"
                            isLiveVoiceListening.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    voiceAssistantManager.setProcessing(false)
                    val errMsg = "Sorry, unable to process audio dialogue: ${e.localizedMessage}"
                    liveVoiceTranscript.value = liveVoiceTranscript.value + Pair("Yarkhoon AI", errMsg)
                    liveVoiceStatusText.value = errMsg
                    isLiveVoiceListening.value = false
                }
            }
        }
    }

    fun executeUniversalVoiceSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        isVoiceSearching.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = voiceAssistantManager.executeVoiceSearch(
                voiceQuery = q,
                allPosts = repository.allPosts.first(),
                allGroups = repository.allGroups.first(),
                allUsers = repository.allUsers.first(),
                allMarketplace = repository.allMarketplaceItems.first(),
                allServices = repository.allServiceListings.first()
            )
            withContext(Dispatchers.Main) {
                voiceSearchResult.value = result
                isVoiceSearching.value = false
                if (result.summarySpeech.isNotBlank()) {
                    voiceAssistantManager.speakText(result.summarySpeech, result.detectedLanguage)
                }
            }
        }
    }

    fun executeVoiceTranslation(text: String, sourceLang: VoiceLanguage, targetLang: VoiceLanguage) {
        val t = text.trim()
        if (t.isBlank()) return
        isVoiceTranslating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = voiceAssistantManager.translateSpokenText(t, sourceLang, targetLang)
            withContext(Dispatchers.Main) {
                voiceTranslationResult.value = result
                isVoiceTranslating.value = false
                if (result.translatedText.isNotBlank()) {
                    voiceAssistantManager.speakText(result.translatedText, targetLang)
                }
            }
        }
    }

    fun addKhowarDatasetContribution(
        khowarText: String,
        khowarRoman: String,
        urduTranslation: String,
        englishTranslation: String,
        audioFilePath: String,
        audioDurationMs: Long,
        dialect: String,
        category: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val entryId = "khowar_user_${UUID.randomUUID().toString().take(10)}"
            val entry = KhowarDatasetEntry(
                id = entryId,
                khowarText = khowarText.trim(),
                khowarRomanText = khowarRoman.trim(),
                urduTranslation = urduTranslation.trim(),
                englishTranslation = englishTranslation.trim(),
                audioFilePath = audioFilePath,
                audioDurationMs = audioDurationMs,
                dialect = dialect,
                category = category,
                contributorId = user?.id ?: "currentUser",
                contributorName = user?.fullName?.ifBlank { user.username } ?: "Community Contributor",
                isVerified = false,
                votesCount = 1,
                timestamp = System.currentTimeMillis(),
                exportStatus = "SUBMITTED"
            )
            repository.insertKhowarDatasetEntry(entry)

            // Sync to Firestore for cloud dataset aggregation
            try {
                val map = hashMapOf(
                    "id" to entry.id,
                    "khowarText" to entry.khowarText,
                    "khowarRomanText" to entry.khowarRomanText,
                    "urduTranslation" to entry.urduTranslation,
                    "englishTranslation" to entry.englishTranslation,
                    "audioFilePath" to entry.audioFilePath,
                    "audioDurationMs" to entry.audioDurationMs,
                    "dialect" to entry.dialect,
                    "category" to entry.category,
                    "contributorId" to entry.contributorId,
                    "contributorName" to entry.contributorName,
                    "isVerified" to false,
                    "votesCount" to 1,
                    "timestamp" to entry.timestamp
                )
                firebaseFirestore?.collection("khowar_dataset")?.document(entry.id)?.set(map)
            } catch (e: Exception) {
                android.util.Log.w("SocialMediaViewModel", "Firestore dataset sync note: ${e.localizedMessage}")
            }
        }
    }

    fun voteKhowarDatasetEntry(entry: KhowarDatasetEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = entry.copy(votesCount = entry.votesCount + 1)
            repository.updateKhowarDatasetEntry(updated)
            try {
                firebaseFirestore?.collection("khowar_dataset")?.document(entry.id)?.update("votesCount", updated.votesCount)
            } catch (e: Exception) {
                // Ignore transient errors
            }
        }
    }

    fun deleteKhowarDatasetEntry(entryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteKhowarDatasetEntry(entryId)
            try {
                firebaseFirestore?.collection("khowar_dataset")?.document(entryId)?.delete()
            } catch (e: Exception) {
                // Ignore transient errors
            }
        }
    }

    fun exportKhowarDatasetJsonl(context: Context, onComplete: (File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entries = repository.getKhowarDatasetEntriesOnce()
                val jsonl = KhowarLanguageEngine.exportDatasetToJsonl(entries)
                val file = KhowarLanguageEngine.saveDatasetExportFile(context, jsonl)
                withContext(Dispatchers.Main) {
                    onComplete(file)
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Error exporting dataset: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onComplete(null)
                }
            }
        }
    }

    // ==================== IMAGE GENERATION & EDITING ====================

    fun generateOrEditAiImage(
        prompt: String,
        context: Context,
        baseImageUri: Uri? = null,
        aspectRatio: String = "1:1",
        onComplete: (GeneratedMediaResult) -> Unit
    ) {
        if (prompt.isBlank()) return
        isImageGenLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val baseImageBase64 = if (baseImageUri != null) {
                GeminiService.uriToBase64(context, baseImageUri)
            } else null

            val result = GeminiService.generateOrEditImage(
                prompt = prompt,
                baseImageBase64 = baseImageBase64,
                aspectRatio = aspectRatio
            )

            latestGeneratedImage.value = result

            if (result.success && result.mediaUrl != null) {
                val newCreation = AiCreationItem(
                    id = "img_${System.currentTimeMillis()}",
                    type = "IMAGE",
                    prompt = prompt,
                    mediaUrl = result.mediaUrl,
                    title = if (baseImageUri != null) "Edited Image" else "AI Generated Image",
                    modelUsed = "gemini-3.1-flash-image-preview",
                    aspectRatio = aspectRatio
                )
                _aiCreations.value = listOf(newCreation) + _aiCreations.value

                // Save creation to Firestore
                try {
                    val uid = currentUser.value?.id ?: "user"
                    val creationMap = hashMapOf(
                        "userId" to uid,
                        "type" to "IMAGE",
                        "prompt" to prompt,
                        "model" to "gemini-3.1-flash-image-preview",
                        "mediaUrl" to result.mediaUrl,
                        "timestamp" to System.currentTimeMillis()
                    )
                    firebaseFirestore?.collection("ai_creations")?.add(creationMap)
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore image creation sync note: ${e.localizedMessage}")
                }
            }

            withContext(Dispatchers.Main) {
                isImageGenLoading.value = false
                onComplete(result)
            }
        }
    }

    // ==================== MUSIC GENERATION (LYRIA) ====================

    fun generateAiMusic(
        prompt: String,
        isFullTrack: Boolean = false,
        genre: String = "Traditional Folk & Ambient",
        onComplete: (GeneratedMediaResult) -> Unit
    ) {
        if (prompt.isBlank()) return
        isMusicGenLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val result = GeminiService.generateMusic(
                prompt = prompt,
                isFullTrack = isFullTrack,
                genre = genre
            )

            latestGeneratedMusic.value = result

            if (result.success && result.mediaUrl != null) {
                val newCreation = AiCreationItem(
                    id = "mus_${System.currentTimeMillis()}",
                    type = "MUSIC",
                    prompt = prompt,
                    mediaUrl = result.mediaUrl,
                    title = result.title,
                    modelUsed = if (isFullTrack) "lyria-3-pro-preview" else "lyria-3-clip-preview",
                    metadata = "$genre • ${if (isFullTrack) "Full Track" else "30s Clip"}"
                )
                _aiCreations.value = listOf(newCreation) + _aiCreations.value

                try {
                    val uid = currentUser.value?.id ?: "user"
                    val musicDoc = hashMapOf(
                        "userId" to uid,
                        "type" to "MUSIC",
                        "prompt" to prompt,
                        "model" to (if (isFullTrack) "lyria-3-pro-preview" else "lyria-3-clip-preview"),
                        "genre" to genre,
                        "timestamp" to System.currentTimeMillis()
                    )
                    firebaseFirestore?.collection("ai_creations")?.add(musicDoc)
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore music creation sync note: ${e.localizedMessage}")
                }
            }

            withContext(Dispatchers.Main) {
                isMusicGenLoading.value = false
                onComplete(result)
            }
        }
    }

    // ==================== VIDEO GENERATION & ANIMATION (VEO 3) ====================

    fun generateAiVideo(
        prompt: String,
        context: Context,
        baseImageUri: Uri? = null,
        aspectRatio: String = "16:9",
        onComplete: (GeneratedMediaResult) -> Unit
    ) {
        if (prompt.isBlank()) return
        isVideoGenLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val baseImageBase64 = if (baseImageUri != null) {
                GeminiService.uriToBase64(context, baseImageUri)
            } else null

            val result = GeminiService.generateVideo(
                prompt = prompt,
                baseImageBase64 = baseImageBase64,
                aspectRatio = aspectRatio
            )

            latestGeneratedVideo.value = result

            if (result.success && result.mediaUrl != null) {
                val newCreation = AiCreationItem(
                    id = "vid_${System.currentTimeMillis()}",
                    type = "VIDEO",
                    prompt = prompt,
                    mediaUrl = result.mediaUrl,
                    title = result.title,
                    modelUsed = "veo-3.1-fast-generate-preview",
                    aspectRatio = aspectRatio,
                    metadata = if (baseImageUri != null) "Photo Animation • $aspectRatio" else "Text-to-Video • $aspectRatio"
                )
                _aiCreations.value = listOf(newCreation) + _aiCreations.value

                try {
                    val uid = currentUser.value?.id ?: "user"
                    val videoDoc = hashMapOf(
                        "userId" to uid,
                        "type" to "VIDEO",
                        "prompt" to prompt,
                        "model" to "veo-3.1-fast-generate-preview",
                        "aspectRatio" to aspectRatio,
                        "isImageToVideo" to (baseImageUri != null),
                        "timestamp" to System.currentTimeMillis()
                    )
                    firebaseFirestore?.collection("ai_creations")?.add(videoDoc)
                } catch (e: Exception) {
                    android.util.Log.w("SocialMediaViewModel", "Firestore video creation sync note: ${e.localizedMessage}")
                }
            }

            withContext(Dispatchers.Main) {
                isVideoGenLoading.value = false
                onComplete(result)
            }
        }
    }

    // ==================== PUBLISH AI CREATION DIRECTLY TO FEED ====================

    fun publishCreationToFeed(
        creation: AiCreationItem,
        captionText: String = "",
        asViral: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = currentUser.value
            val authorId = user?.id ?: "currentUser"
            val authorName = user?.fullName?.ifBlank { user.username } ?: "Yarkhoon Creator"
            val authorAvatar = user?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"

            val mediaType = when (creation.type) {
                "IMAGE" -> "IMAGE"
                "VIDEO" -> "VIDEO"
                "MUSIC" -> "IMAGE" // Music track with cover & caption
                else -> "IMAGE"
            }

            val finalCaption = if (captionText.isNotBlank()) {
                "$captionText\n\n✨ Created with ${creation.modelUsed}\nPrompt: \"${creation.prompt}\""
            } else {
                "✨ Created with ${creation.modelUsed} on Yarkhoon AI Studio\nPrompt: \"${creation.prompt}\""
            }

            val newPost = Post(
                authorId = authorId,
                authorName = authorName,
                authorAvatarUrl = authorAvatar,
                content = finalCaption,
                mediaType = mediaType,
                mediaUrl = creation.mediaUrl,
                timestamp = System.currentTimeMillis(),
                likesCount = 1,
                isLikedByMe = true,
                commentsCount = 0,
                isViral = asViral
            )

            repository.insertPost(newPost)

            try {
                val postMap = hashMapOf(
                    "authorId" to newPost.authorId,
                    "authorName" to newPost.authorName,
                    "authorAvatarUrl" to newPost.authorAvatarUrl,
                    "content" to newPost.content,
                    "mediaType" to newPost.mediaType,
                    "mediaUrl" to newPost.mediaUrl,
                    "timestamp" to newPost.timestamp,
                    "likesCount" to newPost.likesCount,
                    "commentsCount" to newPost.commentsCount,
                    "isViral" to newPost.isViral,
                    "source" to "Yarkhoon AI Studio"
                )
                firebaseFirestore?.collection("posts")?.add(postMap)
            } catch (e: Exception) {
                android.util.Log.e("SocialMediaViewModel", "Firestore post creation sync error: ${e.localizedMessage}")
            }
        }
    }
}

