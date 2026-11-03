package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainNavTab(val title: String) {
    HOME("Home"),
    REELS("Reels"),
    EXPLORE("Explore"),
    BUSINESS("Business Hub"),
    PROFILE("Profile")
}

enum class FeedFilter(val label: String) {
    ALL("All"),
    FOLLOWING("Following"),
    TRENDING("Trending"),
    FAVORITES("Favorites"),
    @Deprecated("Use ALL instead", ReplaceWith("ALL"))
    FOR_YOU("All");

    companion object {
        val DEFAULT = ALL
    }
}

enum class ProfileTab(val label: String) {
    POSTS("Posts"),
    REELS("Reels"),
    MEDIA("Photos"),
    BOOKMARKS("Saved"),
    ABOUT("About")
}

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    PROFILE_SETUP,
    FORGOT_PASSWORD,
    ADMIN_LOGIN
}

data class YarkhoonUiState(
    // Authentication - defaults to Firebase session check
    val isAuthenticated: Boolean = false,
    val authMode: AuthMode = AuthMode.SIGN_IN,
    val authLoading: Boolean = false,
    val authError: String? = null,
    val currentUser: User = SampleData.sampleCurrentUser,

    // Navigation
    val currentTab: MainNavTab = MainNavTab.HOME,
    val feedFilter: FeedFilter = FeedFilter.ALL,
    val profileTab: ProfileTab = ProfileTab.POSTS,

    // Data Collections
    val posts: List<Post> = SampleData.initialPosts,
    val stories: List<Story> = SampleData.stories,
    val reels: List<Reel> = SampleData.sampleReels,
    val activeReelIndex: Int = 0,
    val businessProfiles: List<BusinessProfile> = SampleData.businessProfiles,
    val selectedBusinessCategory: BusinessCategory = BusinessCategory.ALL,
    val businessSearchQuery: String = "",
    val activeHubSection: BusinessHubSection = BusinessHubSection.SERVICES,
    val marketplaceItems: List<MarketplaceItem> = SampleData.sampleMarketplaceItems,
    val selectedMarketplaceCategory: MarketplaceCategory = MarketplaceCategory.ALL,
    val marketplaceSearchQuery: String = "",
    val isSellItemDialogOpen: Boolean = false,
    val itemToEdit: MarketplaceItem? = null,
    val exploreSearchQuery: String = "",
    val notifications: List<NotificationItem> = emptyList(),
    val isNotificationsLoading: Boolean = false,
    val notificationError: String? = null,
    val conversations: List<Conversation> = SampleData.conversations,

    // Dialogs & Sheets
    val activeCommentsPost: Post? = null,
    val activeStory: Story? = null,
    val isCreatePostOpen: Boolean = false,
    val isMessagesOpen: Boolean = false,
    val isNotificationsOpen: Boolean = false,
    val feedbackMessage: String? = null,

    // Cloud Firestore Profile Synchronization (Stage 5)
    val isProfileLoading: Boolean = false,
    val profileError: String? = null,

    // Real Social Feed, Posts, Stories & Reels (Stage 7)
    val isFeedLoading: Boolean = false,
    val feedError: String? = null,
    val isPublishingPost: Boolean = false,
    val uploadProgress: Float = 0f,
    val publishError: String? = null,
    val isCreateStoryOpen: Boolean = false,
    val isCreateReelOpen: Boolean = false,

    // Real-Time Private Messaging & User Discovery (Stage 8)
    val activeConversation: Conversation? = null,
    val activeMessages: List<ChatMessage> = emptyList(),
    val isMessagesLoading: Boolean = false,
    val isSendingMessage: Boolean = false,
    val messagingError: String? = null,
    val discoveredUsers: List<User> = emptyList(),
    val isUserDiscoveryOpen: Boolean = false,
    val userSearchQuery: String = "",
    val isSearchingUsers: Boolean = false,
    val messagesLimit: Long = 40L,

    // Firebase Business Hub & Marketplace (Stage 9)
    val isBusinessLoading: Boolean = false,
    val businessError: String? = null,
    val isMarketplaceLoading: Boolean = false,
    val marketplaceError: String? = null,
    val isSavingListing: Boolean = false,
    val listingUploadProgress: Float = 0f,
    val listingUploadError: String? = null,

    // Search, Explore & Trending (Stage 10)
    val searchActiveTab: SearchTab = SearchTab.ALL,
    val searchResults: SearchResults? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val exploreContent: ExploreContent = ExploreContent(),
    val isExploreLoading: Boolean = false,
    val exploreError: String? = null,
    val activeHashtagDetail: HashtagDetail? = null,
    val selectedUserProfile: User? = null,
    val selectedUserPosts: List<Post> = emptyList(),
    val createPostInitialType: PostType = PostType.TEXT,
    val createPostInitialFeeling: String? = null,
    val createPostInitialMediaUri: String? = null,

    // Social Interactions (Stage 11)
    val followingUserIds: Set<String> = emptySet(),
    val savedPostIds: Set<String> = emptySet(),
    val activePostComments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val commentError: String? = null,
    val activeSharePost: Post? = null,
    val isSavedPostsOpen: Boolean = false,

    // Following System & Privacy (Stage 12)
    val sentFollowRequestUserIds: Set<String> = emptySet(),
    val pendingFollowRequests: List<FollowRequest> = emptyList(),
    val isFollowRequestsOpen: Boolean = false,
    val isLoadingFollowRequests: Boolean = false,
    val blockedUserIds: Set<String> = emptySet(),
    val blockedUsers: List<User> = emptyList(),
    val isBlockedAccountsOpen: Boolean = false,
    val mutedUserIds: Set<String> = emptySet(),
    val mutedUsers: List<User> = emptyList(),
    val isMutedAccountsOpen: Boolean = false,
    val favoriteUserIds: Set<String> = emptySet(),
    val favoriteUsers: List<User> = emptyList(),
    val isFavoritesOpen: Boolean = false,
    val isPrivacySettingsOpen: Boolean = false,

    // Stage 13 & Complete Admin Control Panel
    val isAdmin: Boolean = false,
    val adminRole: AdminRole = AdminRole.NONE,
    val isAdminDashboardOpen: Boolean = false,
    val activeAdminSection: AdminControlPanelSection = AdminControlPanelSection.DASHBOARD,
    val adminStats: AdminDashboardStats = AdminDashboardStats(),
    val adminUsersList: List<User> = emptyList(),
    val adminPostsList: List<Post> = emptyList(),
    val verificationRequests: List<VerificationRequest> = emptyList(),
    val villagesList: List<YarkhoonVillage> = emptyList(),
    val officialAnnouncements: List<OfficialAnnouncement> = emptyList(),
    val curatedTrendingTopics: List<CuratedTrendingTopic> = emptyList(),
    val adminAccountsList: List<AdminAccountRecord> = emptyList(),
    val adminSessionStartedAt: Long = 0L,
    val reports: List<ContentReport> = emptyList(),
    val auditLogs: List<ModerationAuditLog> = emptyList(),
    val isAdminLoading: Boolean = false,
    val adminError: String? = null,
    val adminDiagnosticInfo: String? = null,
    val isReportDialogOpen: Boolean = false,
    val pendingReportTarget: ContentReport? = null,
    val isUserSuspended: Boolean = false,
    val suspensionInfo: UserSuspensionInfo? = null,

    // Yarkhoon Madad Center
    val isMadadCenterOpen: Boolean = false,
    val activeMadadCategory: MadadCategory = MadadCategory.JOBS,
    val jobSeekerProfiles: List<JobSeekerProfile> = emptyList(),
    val jobVacancies: List<JobVacancy> = emptyList(),
    val bloodDonors: List<BloodDonor> = emptyList(),
    val bloodRequests: List<BloodRequest> = emptyList(),
    val communityAidRequests: List<CommunityAidRequest> = emptyList(),
    val legalGuidanceTopics: List<LegalGuidanceTopic> = emptyList(),
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val isMadadLoading: Boolean = false,
    val madadError: String? = null,
    val isPublishingMadadItem: Boolean = false,
    val madadJobCityFilter: String = "All Cities",
    val madadJobSkillQuery: String = "",
    val madadBloodGroupFilter: String = "All Groups",
    val madadBloodCityFilter: String = "All Cities",
    val madadAidCategoryFilter: String = "All Categories"
)

class YarkhoonViewModel(
    private val authRepository: FirebaseAuthRepository = FirebaseAuthRepository(),
    private val userRepository: FirestoreUserRepository = FirestoreUserRepository(),
    private val postRepository: FirestorePostRepository = FirestorePostRepository(),
    private val storyRepository: FirestoreStoryRepository = FirestoreStoryRepository(),
    private val reelRepository: FirestoreReelRepository = FirestoreReelRepository(),
    private val storageRepository: FirebaseStorageRepository = FirebaseStorageRepository(),
    private val messagingRepository: FirestoreMessagingRepository = FirestoreMessagingRepository(),
    private val businessRepository: FirestoreBusinessRepository = FirestoreBusinessRepository(),
    private val marketplaceRepository: FirestoreMarketplaceRepository = FirestoreMarketplaceRepository(),
    private val searchRepository: FirestoreSearchRepository = FirestoreSearchRepository(),
    private val adminRepository: FirestoreAdminRepository = FirestoreAdminRepository(),
    private val notificationRepository: FirestoreNotificationRepository = FirestoreNotificationRepository(),
    private val madadRepository: FirestoreMadadRepository = FirestoreMadadRepository()
) : ViewModel() {

    private var conversationsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var authStateListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null
    private var sessionInitializationJob: Job? = null
    private var feedInitializationJob: Job? = null

    @Volatile
    private var isPerformingExplicitAuth: Boolean = false

    private val _uiState = MutableStateFlow(YarkhoonUiState())
    val uiState: StateFlow<YarkhoonUiState> = _uiState.asStateFlow()

    init {
        try {
            val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
            AdminSessionManager.initialize(context)
        } catch (_: Exception) {}
        setupAuthStateListener()
    }

    private fun setupAuthStateListener() {
        authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            Log.i(
                "YarkhoonAuthDebug",
                "[AUTH_STATE_LISTENER] Fired: currentUser.uid=${user?.uid ?: "null"}, " +
                    "isPerformingExplicitAuth=$isPerformingExplicitAuth, " +
                    "uiState.isAuthenticated=${_uiState.value.isAuthenticated}, " +
                    "uiState.isAdmin=${_uiState.value.isAdmin}, " +
                    "authInstance=${System.identityHashCode(firebaseAuth)}"
            )
            if (user != null) {
                if (isPerformingExplicitAuth) {
                    Log.i("YarkhoonAuthDebug", "[AUTH_STATE_LISTENER] Skipping restoreFirebaseSession() because explicit login is currently in progress.")
                } else if (!_uiState.value.isAuthenticated || _uiState.value.currentUser.id != user.uid) {
                    restoreFirebaseSession()
                }
            } else {
                sessionInitializationJob?.cancel()
                feedInitializationJob?.cancel()
                conversationsListener?.remove()
                conversationsListener = null
                notificationsListener?.remove()
                notificationsListener = null
                detachMessagesListener()
                if (_uiState.value.isAuthenticated || _uiState.value.isAdminDashboardOpen || _uiState.value.isAdmin) {
                    val wasAdmin = _uiState.value.isAdmin || _uiState.value.isAdminDashboardOpen
                    Log.w("YarkhoonAuthDebug", "[AUTH_STATE_LISTENER] currentUser became null while authenticated/admin! Closing dashboard and returning to login screen.")
                    _uiState.update {
                        it.copy(
                            isAuthenticated = false,
                            isAdmin = false,
                            adminRole = AdminRole.NONE,
                            isAdminDashboardOpen = false,
                            adminSessionStartedAt = 0L,
                            isProfileLoading = false,
                            isFeedLoading = false,
                            feedError = null,
                            authMode = if (wasAdmin) AuthMode.ADMIN_LOGIN else it.authMode,
                            authError = if (wasAdmin && it.authError == null) {
                                "Administrator authentication session ended. Please sign in again."
                            } else {
                                it.authError
                            }
                        )
                    }
                }
            }
        }
        authRepository.addAuthStateListener(authStateListener!!)
    }

    private fun restoreFirebaseSession() {
        val fbUser = authRepository.currentUser
        if (fbUser != null) {
            if (isPerformingExplicitAuth) {
                Log.i("YarkhoonAuthDebug", "[RESTORE_SESSION] Aborting restoreFirebaseSession because explicit login is active.")
                return
            }
            Log.i("YarkhoonAuthDebug", "[RESTORE_SESSION] Starting cold-start session restoration for uid=${fbUser.uid}")
            val userEmail = FirestoreAdminRepository.resolveCanonicalEmail(fbUser.email)
            val userDisplayName = fbUser.displayName
                ?.takeIf { it.isNotBlank() }
                ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Member" }

            val initialUser = User(
                id = fbUser.uid,
                name = userDisplayName,
                handle = "@${userEmail.substringBefore("@").ifBlank { fbUser.uid.take(8) }}",
                email = userEmail,
                bio = "Active member of YARKHOON.COM.",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                isVerified = false,
                followersCount = 0,
                followingCount = 0,
                postsCount = 0
            )
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    currentUser = initialUser,
                    isProfileLoading = true
                )
            }

            sessionInitializationJob?.cancel()
            sessionInitializationJob = viewModelScope.launch {
                val profileResult = userRepository.getUserProfile(fbUser.uid)
                val emailProfileResult = if (profileResult.getOrNull() == null && userEmail.isNotBlank()) {
                    userRepository.findUserByEmail(userEmail)
                } else {
                    null
                }
                val loadedProfile = profileResult.getOrNull() ?: emailProfileResult?.getOrNull()
                if (loadedProfile != null) {
                    val syncedProfile = loadedProfile.copy(
                        id = fbUser.uid,
                        email = userEmail.ifBlank { loadedProfile.email }
                    )
                    if (profileResult.getOrNull() == null) {
                        userRepository.saveUserProfile(syncedProfile)
                    }
                    _uiState.update {
                        it.copy(
                            currentUser = syncedProfile,
                            isProfileLoading = false,
                            profileError = null
                        )
                    }
                } else {
                    // Save initial user profile in Firestore if document does not exist yet
                    userRepository.saveUserProfile(initialUser)
                    _uiState.update {
                        it.copy(
                            currentUser = initialUser,
                            isProfileLoading = false,
                            profileError = null
                        )
                    }
                }
                // Check account suspension status
                val suspensionResult = adminRepository.checkUserSuspension(fbUser.uid)
                if (suspensionResult.isSuccess) {
                    val info = suspensionResult.getOrNull()
                    if (info != null && info.isCurrentlySuspended) {
                        _uiState.update {
                            it.copy(
                                isUserSuspended = true,
                                suspensionInfo = info,
                                feedbackMessage = "Account Suspended: ${info.suspensionReason}"
                            )
                        }
                    }
                }

                // If an explicit login occurred while profile/suspension was loading, do not interfere
                if (isPerformingExplicitAuth) {
                    Log.i("YarkhoonAuthDebug", "[RESTORE_SESSION] Yielding to active explicit login.")
                    return@launch
                }

                // Verify admin status via custom claims / adminRoles collection, Remember Me, and 4-hour inactivity window
                val resolvedRole = adminRepository.getAdminRole()
                val now = System.currentTimeMillis()
                val isPrivilegedAdmin = resolvedRole.canAccessAdminPanel
                if (isPrivilegedAdmin) {
                    // If an active unexpired session was already established in this process, preserve it
                    val hasActiveInProcessSession = AdminSessionManager.isSessionActive && !AdminSessionManager.isSessionExpired(now) &&
                        FirestoreAdminRepository.isAdminSessionValid(FirestoreAdminRepository.activeAdminSessionStartMs, now)
                    val canRestore = hasActiveInProcessSession || AdminSessionManager.shouldRestoreOnAppStart(now)
                    Log.i("YarkhoonAuthDebug", "[RESTORE_SESSION] Admin role=${resolvedRole.displayName}, hasActiveInProcessSession=$hasActiveInProcessSession, canRestore=$canRestore")
                    if (canRestore) {
                        AdminSessionManager.recordActivity(now)
                        FirestoreAdminRepository.startAdminSession(
                            sessionStartMs = AdminSessionManager.sessionStartTimeMs.takeIf { it > 0L } ?: now,
                            rememberMe = AdminSessionManager.isRememberMeEnabled,
                            uid = fbUser.uid,
                            email = userEmail
                        )
                        _uiState.update {
                            it.copy(
                                isAdmin = true,
                                adminRole = resolvedRole,
                                adminSessionStartedAt = AdminSessionManager.sessionStartTimeMs
                            )
                        }
                        startAdminInactivityWatcher()
                    } else {
                        // Admin session cannot be restored across cold app restart: Remember Me was disabled or 4h inactivity expired
                        val wasExpired = AdminSessionManager.isSessionActive && AdminSessionManager.isSessionExpired(now)
                        Log.w("YarkhoonAuthDebug", "[RESTORE_SESSION] Cold-start admin session cannot be restored (wasExpired=$wasExpired, rememberMe=${AdminSessionManager.isRememberMeEnabled}). Signing out to require fresh Admin Login.")
                        AdminSessionManager.invalidateAndClear()
                        FirestoreAdminRepository.invalidateAdminSession()
                        authRepository.signOut()
                        _uiState.update {
                            it.copy(
                                isAuthenticated = false,
                                isAdmin = false,
                                adminRole = AdminRole.NONE,
                                isAdminDashboardOpen = false,
                                adminSessionStartedAt = 0L,
                                authMode = AuthMode.ADMIN_LOGIN,
                                authError = if (wasExpired) {
                                    "Admin session expired due to 4 hours of inactivity. Please log in again."
                                } else {
                                    "Admin session closed. Please sign in with administrator credentials."
                                }
                            )
                        }
                        return@launch
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isAdmin = false,
                            adminRole = AdminRole.NONE,
                            adminSessionStartedAt = 0L
                        )
                    }
                }

                // Load real Firestore posts, stories, and reels for authenticated session
                loadFeedAndSocialContent()

                // Attach real-time notification listener for authenticated user
                attachNotificationsListener(fbUser.uid)
            }
        } else {
            sessionInitializationJob?.cancel()
            feedInitializationJob?.cancel()
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    isAdmin = false,
                    adminRole = AdminRole.NONE,
                    isAdminDashboardOpen = false,
                    isProfileLoading = false,
                    isFeedLoading = false,
                    feedError = null
                )
            }
        }
    }

    // ---------------- Navigation ----------------
    fun selectTab(tab: MainNavTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleReelLike(reelId: String) {
        _uiState.update { state ->
            val updatedReels = state.reels.map { reel ->
                if (reel.id == reelId) {
                    val newLiked = !reel.isLiked
                    val newCount = if (newLiked) reel.likesCount + 1 else (reel.likesCount - 1).coerceAtLeast(0)
                    reel.copy(isLiked = newLiked, likesCount = newCount)
                } else reel
            }
            state.copy(reels = updatedReels)
        }
    }

    fun toggleReelBookmark(reelId: String) {
        _uiState.update { state ->
            val updatedReels = state.reels.map { reel ->
                if (reel.id == reelId) reel.copy(isSaved = !reel.isSaved) else reel
            }
            val target = updatedReels.find { it.id == reelId }
            val msg = if (target?.isSaved == true) "Reel saved to bookmarks" else "Reel removed from bookmarks"
            state.copy(reels = updatedReels, feedbackMessage = msg)
        }
    }

    fun shareReel(reelId: String) {
        _uiState.update { state ->
            val updated = state.reels.map { reel ->
                if (reel.id == reelId) reel.copy(sharesCount = reel.sharesCount + 1) else reel
            }
            state.copy(reels = updated, feedbackMessage = "Reel link copied to clipboard!")
        }
    }

    fun setFeedFilter(filter: FeedFilter) {
        _uiState.update { it.copy(feedFilter = filter) }
    }

    fun setProfileTab(tab: ProfileTab) {
        _uiState.update { it.copy(profileTab = tab) }
    }

    // ---------------- Real Firebase Authentication (Stage 4A) ----------------
    fun setAuthMode(mode: AuthMode) {
        _uiState.update { it.copy(authMode = mode, authError = null) }
    }

    fun signIn(identifier: String, pass: String) {
        val trimmedEmail = identifier.trim()
        val trimmedPass = pass.trim()

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter both your registered email and password.") }
            return
        }

        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            _uiState.update { it.copy(authError = "Please enter a valid email address.") }
            return
        }

        if (trimmedPass.length < 6) {
            _uiState.update { it.copy(authError = "Password must be at least 6 characters.") }
            return
        }

        // Prevent duplicate submissions
        if (_uiState.value.authLoading) return

        _uiState.update { it.copy(authLoading = true, authError = null) }

        viewModelScope.launch {
            isPerformingExplicitAuth = true
            sessionInitializationJob?.cancel()
            try {
                when (val result = authRepository.signInWithEmail(trimmedEmail, trimmedPass)) {
                    is AuthResult.Success -> {
                        val canonicalEmail = FirestoreAdminRepository.resolveCanonicalEmail(result.email)
                        val displayName = result.displayName?.takeIf { it.isNotBlank() }
                            ?: trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val initialUser = User(
                            id = result.uid,
                            name = displayName,
                            handle = "@${trimmedEmail.substringBefore("@")}",
                            email = result.email,
                            bio = "Active member of YARKHOON.COM.",
                            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                            isVerified = false,
                            followersCount = 0,
                            followingCount = 0,
                            postsCount = 0
                        )
                        _uiState.update {
                            it.copy(
                                authLoading = false,
                                isAuthenticated = true,
                                currentUser = initialUser,
                                authError = null,
                                isProfileLoading = true,
                                feedbackMessage = "Welcome back, $displayName!"
                            )
                        }

                        // Load user profile from Cloud Firestore (check UID first, then canonical email)
                        val profileResult = userRepository.getUserProfile(result.uid)
                        val emailProfileResult = if (profileResult.getOrNull() == null && canonicalEmail.isNotBlank()) {
                            userRepository.findUserByEmail(canonicalEmail)
                        } else {
                            null
                        }
                        val loadedProfile = profileResult.getOrNull() ?: emailProfileResult?.getOrNull()
                        if (loadedProfile != null) {
                            val syncedProfile = loadedProfile.copy(
                                id = result.uid,
                                email = canonicalEmail.ifBlank { loadedProfile.email }
                            )
                            if (profileResult.getOrNull() == null) {
                                userRepository.saveUserProfile(syncedProfile)
                            }
                            _uiState.update {
                                it.copy(
                                    currentUser = syncedProfile,
                                    isProfileLoading = false,
                                    profileError = null
                                )
                            }
                        } else {
                            // User has no Firestore document yet: save initial baseline profile
                            userRepository.saveUserProfile(initialUser)
                            _uiState.update {
                                it.copy(
                                    isProfileLoading = false,
                                    profileError = null
                                )
                            }
                        }
                        // Check account suspension status
                        val suspensionResult = adminRepository.checkUserSuspension(result.uid)
                        if (suspensionResult.isSuccess) {
                            val info = suspensionResult.getOrNull()
                            if (info != null && info.isCurrentlySuspended) {
                                _uiState.update {
                                    it.copy(
                                        isUserSuspended = true,
                                        suspensionInfo = info,
                                        feedbackMessage = "Account Suspended: ${info.suspensionReason}"
                                    )
                                }
                            } else {
                                _uiState.update { it.copy(isUserSuspended = false, suspensionInfo = null) }
                            }
                        }

                        // Verify admin status and initialize 4-hour session window
                        val resolvedRole = adminRepository.getAdminRole()
                        val adminVerified = resolvedRole.canAccessAdminPanel
                        val sessionStartMs = if (adminVerified) System.currentTimeMillis() else 0L
                        if (adminVerified) {
                            FirestoreAdminRepository.startAdminSession(
                                sessionStartMs = sessionStartMs,
                                rememberMe = false,
                                uid = result.uid,
                                email = canonicalEmail
                            )
                            adminRepository.createOrUpdateServerAdminSession(
                                uid = result.uid,
                                email = canonicalEmail,
                                rememberMe = false
                            )
                            startAdminInactivityWatcher()
                        } else {
                            FirestoreAdminRepository.invalidateAdminSession()
                        }
                        _uiState.update {
                            it.copy(
                                isAdmin = adminVerified,
                                adminRole = resolvedRole,
                                adminSessionStartedAt = sessionStartMs
                            )
                        }

                        // Load real Firestore posts, stories, and reels
                        loadFeedAndSocialContent()
                    }
                    is AuthResult.Error -> {
                        _uiState.update {
                            it.copy(
                                authLoading = false,
                                authError = result.message
                            )
                        }
                    }
                }
            } finally {
                isPerformingExplicitAuth = false
            }
        }
    }

    /**
     * Dedicated Admin Authentication Entrypoint (with diagnostic logging):
     * Strictly verifies real Firebase Auth credentials, Owner UID/email match, and role resolution.
     */
    fun authenticateAdminUser(email: String, pass: String, rememberMe: Boolean = false) {
        Log.i("YarkhoonAuthDebug", "[TRACE 1: authenticateAdminUser] Entry: email=$email, rememberMe=$rememberMe")
        signInAsAdmin(email, pass, rememberMe)
    }

    /**
     * Dedicated Admin Portal Login:
     * Enforces brute-force rate limiting, authenticates credentials against Firebase Auth,
     * and strictly verifies that the account holds an authorized AdminRole (SUPER_ADMIN, ADMIN, or MODERATOR).
     * Ordinary user accounts are immediately signed out and rejected.
     */
    fun signInAsAdmin(identifier: String, pass: String, rememberMe: Boolean = false) {
        val trimmedEmail = identifier.trim()
        val trimmedPass = pass.trim()
        Log.i("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] Initiating admin portal login for: $trimmedEmail, rememberMe=$rememberMe")

        if (trimmedEmail.isBlank() || trimmedPass.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter both administrator email and password.") }
            return
        }

        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            _uiState.update { it.copy(authError = "Please enter a valid administrator email address.") }
            return
        }

        if (trimmedPass.length < 6) {
            _uiState.update { it.copy(authError = "Administrator password must be at least 6 characters.") }
            return
        }

        val rateLimitCheck = FirestoreAdminRepository.checkLoginRateLimit(trimmedEmail)
        if (rateLimitCheck.isFailure) {
            _uiState.update { it.copy(authError = rateLimitCheck.exceptionOrNull()?.message) }
            return
        }

        if (_uiState.value.authLoading) return
        _uiState.update { it.copy(authLoading = true, authError = null) }

        viewModelScope.launch {
            isPerformingExplicitAuth = true
            sessionInitializationJob?.cancel()
            try {
                when (val result = authRepository.signInAdminWithEmail(trimmedEmail, trimmedPass)) {
                    is AuthResult.Success -> {
                        Log.i("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] Firebase Auth SUCCESS: uid=${result.uid}, email=${result.email}")
                        // Strictly verify actual Firebase currentUser identity on the singleton FirebaseAuth instance
                        val actualUser = authRepository.currentUser
                        if (actualUser == null) {
                            Log.e("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] REJECTED: authRepository.currentUser is null immediately after signInAdminWithEmail!")
                            FirestoreAdminRepository.recordLoginAttempt(trimmedEmail, success = false)
                            _uiState.update {
                                it.copy(
                                    authLoading = false,
                                    isAuthenticated = false,
                                    isAdmin = false,
                                    adminRole = AdminRole.NONE,
                                    isAdminDashboardOpen = false,
                                    authMode = AuthMode.ADMIN_LOGIN,
                                    authError = "Firebase Authentication session could not be established (currentUser is null). Please sign in again."
                                )
                            }
                            return@launch
                        }
                        val actualUid = actualUser.uid
                        val actualEmail = actualUser.email ?: result.email
                        val normalizedEmail = FirestoreAdminRepository.resolveCanonicalEmail(actualEmail)

                        val isOwnerEmail = normalizedEmail.equals(FirestoreAdminRepository.OWNER_BOOTSTRAP_EMAIL, ignoreCase = true)
                        val isOwnerUid = actualUid == FirestoreAdminRepository.OWNER_BOOTSTRAP_UID
                        Log.i("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] Owner Check: actualUid=$actualUid (isOwnerUid=$isOwnerUid), actualEmail=$actualEmail (isOwnerEmail=$isOwnerEmail)")

                        // If the email matches the authorized owner but the UID differs, reject the session
                        if (isOwnerEmail && !isOwnerUid) {
                            Log.e("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] REJECTED: UID mismatch for owner email. Got $actualUid, expected ${FirestoreAdminRepository.OWNER_BOOTSTRAP_UID}")
                            FirestoreAdminRepository.recordLoginAttempt(trimmedEmail, success = false)
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    authLoading = false,
                                    isAuthenticated = false,
                                    isAdmin = false,
                                    adminRole = AdminRole.NONE,
                                    isAdminDashboardOpen = false,
                                    authMode = AuthMode.ADMIN_LOGIN,
                                    authError = "Authentication mismatch: Account UID does not match the authorized project owner UID."
                                )
                            }
                            return@launch
                        }

                        // Force fresh server role verification immediately after login
                        // Note: getAdminRoleResult reads existing adminRoles/{uid} on server first without repeating bootstrap
                        val resolvedRole = adminRepository.getAdminRole(forceRefreshToken = true)
                        Log.i("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] Resolved role: ${resolvedRole.displayName} (canAccess=${resolvedRole.canAccessAdminPanel})")

                        // Verify that for the designated owner email, currentUser.uid strictly matches OWNER_BOOTSTRAP_UID before resolving SUPER_ADMIN
                        if (resolvedRole == AdminRole.SUPER_ADMIN && isOwnerEmail && authRepository.currentUser?.uid != FirestoreAdminRepository.OWNER_BOOTSTRAP_UID) {
                            Log.e("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] REJECTED: Attempted to resolve SUPER_ADMIN for owner without matching currentUser.uid!")
                            FirestoreAdminRepository.recordLoginAttempt(trimmedEmail, success = false)
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    authLoading = false,
                                    isAuthenticated = false,
                                    isAdmin = false,
                                    adminRole = AdminRole.NONE,
                                    isAdminDashboardOpen = false,
                                    authMode = AuthMode.ADMIN_LOGIN,
                                    authError = "Security verification failed: Authenticated UID does not match designated owner UID."
                                )
                            }
                            return@launch
                        }

                        if (!resolvedRole.canAccessAdminPanel) {
                            // Ordinary user attempted to log in via Admin Portal -> reject immediately and return to Admin Login screen
                            Log.w("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] REJECTED: Resolved role is ${resolvedRole.displayName}. Access denied.")
                            FirestoreAdminRepository.recordLoginAttempt(trimmedEmail, success = false)
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    authLoading = false,
                                    isAuthenticated = false,
                                    isAdmin = false,
                                    adminRole = AdminRole.NONE,
                                    isAdminDashboardOpen = false,
                                    authMode = AuthMode.ADMIN_LOGIN,
                                    authError = "Access Denied: Ordinary user accounts cannot log into the Admin Control Panel."
                                )
                            }
                            return@launch
                        }

                        FirestoreAdminRepository.recordLoginAttempt(trimmedEmail, success = true)
                        val canonicalEmail = FirestoreAdminRepository.resolveCanonicalEmail(actualEmail)
                        val sessionStartMs = System.currentTimeMillis()
                        // Start the local & server admin session BEFORE profile network I/O so no background listener can see an inactive session
                        FirestoreAdminRepository.startAdminSession(
                            sessionStartMs = sessionStartMs,
                            rememberMe = rememberMe,
                            uid = actualUid,
                            email = canonicalEmail
                        )
                        FirestoreAdminRepository.updateVerifiedSessionRole(actualUid, resolvedRole, "signInAsAdmin_verified")

                        val displayName = result.displayName?.takeIf { it.isNotBlank() }
                            ?: canonicalEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val adminUser = User(
                            id = actualUid,
                            name = displayName,
                            handle = "@${canonicalEmail.substringBefore("@")}",
                            email = canonicalEmail,
                            bio = "Yarkhoon ${resolvedRole.displayName}",
                            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                            isVerified = true
                        )

                        val profileResult = userRepository.getUserProfile(actualUid)
                        val emailProfileResult = if (profileResult.getOrNull() == null && canonicalEmail.isNotBlank()) {
                            userRepository.findUserByEmail(canonicalEmail)
                        } else {
                            null
                        }
                        val existingProfile = profileResult.getOrNull() ?: emailProfileResult?.getOrNull()
                        val loadedUser = existingProfile?.copy(
                            id = actualUid,
                            email = canonicalEmail.ifBlank { existingProfile.email }
                        ) ?: adminUser
                        if (profileResult.getOrNull() == null) {
                            userRepository.saveUserProfile(loadedUser)
                        }

                        adminRepository.createOrUpdateServerAdminSession(
                            uid = actualUid,
                            email = canonicalEmail,
                            rememberMe = rememberMe
                        )

                        // Final pre-dashboard guard: verify Firebase currentUser is still active and matches actualUid
                        val finalCheckUser = authRepository.currentUser
                        if (finalCheckUser == null || finalCheckUser.uid != actualUid) {
                            Log.e("YarkhoonAuthDebug", "[TRACE 2: signInAsAdmin] ABORTING DASHBOARD OPEN: currentUser became ${finalCheckUser?.uid ?: "NULL"} before opening dashboard!")
                            AdminSessionManager.invalidateAndClear()
                            FirestoreAdminRepository.invalidateAdminSession()
                            _uiState.update {
                                it.copy(
                                    authLoading = false,
                                    isAuthenticated = false,
                                    isAdmin = false,
                                    adminRole = AdminRole.NONE,
                                    isAdminDashboardOpen = false,
                                    authMode = AuthMode.ADMIN_LOGIN,
                                    authError = "Authentication session was interrupted before opening the dashboard. Please sign in again."
                                )
                            }
                            return@launch
                        }

                        startAdminInactivityWatcher()

                        _uiState.update {
                            it.copy(
                                authLoading = false,
                                isAuthenticated = true,
                                currentUser = loadedUser,
                                isAdmin = true,
                                adminRole = resolvedRole,
                                isAdminDashboardOpen = true,
                                activeAdminSection = AdminControlPanelSection.DASHBOARD,
                                adminSessionStartedAt = sessionStartMs,
                                authError = null,
                                feedbackMessage = "Authenticated as ${resolvedRole.displayName} ($canonicalEmail)."
                            )
                        }

                        adminRepository.recordAdminLoginAudit()
                        loadFullAdminControlPanelData()
                        loadFeedAndSocialContent()
                    }
                    is AuthResult.Error -> {
                        FirestoreAdminRepository.recordLoginAttempt(trimmedEmail, success = false)
                        _uiState.update {
                            it.copy(
                                authLoading = false,
                                authError = result.message
                            )
                        }
                    }
                }
            } finally {
                isPerformingExplicitAuth = false
            }
        }
    }

    fun registerWithOnboarding(
        name: String,
        handle: String,
        email: String,
        phoneNumber: String,
        pass: String,
        bio: String,
        education: String,
        jobTitle: String,
        location: String,
        interests: List<String>,
        isBusiness: Boolean,
        category: String,
        avatarUriString: String? = null
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        val trimmedHandle = handle.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter your full name.") }
            return
        }

        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            _uiState.update { it.copy(authError = "Please enter a valid email address.") }
            return
        }

        if (trimmedPass.length < 6) {
            _uiState.update { it.copy(authError = "Firebase requires a password with at least 6 characters.") }
            return
        }

        // Prevent duplicate submission while in progress
        if (_uiState.value.authLoading) return

        _uiState.update { it.copy(authLoading = true, authError = null) }

        viewModelScope.launch {
            isPerformingExplicitAuth = true
            sessionInitializationJob?.cancel()
            try {
                when (val result = authRepository.registerWithEmail(trimmedEmail, trimmedPass, trimmedName)) {
                    is AuthResult.Success -> {
                        val canonicalEmail = FirestoreAdminRepository.resolveCanonicalEmail(result.email.ifBlank { trimmedEmail })
                        val cleanHandle = if (trimmedHandle.startsWith("@")) trimmedHandle else "@${trimmedHandle.ifBlank { canonicalEmail.substringBefore("@") }}"
                        val defaultAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
                        val newUser = User(
                            id = result.uid,
                            name = trimmedName,
                            handle = cleanHandle,
                            email = canonicalEmail,
                            avatarUrl = avatarUriString ?: defaultAvatar,
                            bio = bio.ifBlank { "Member of YARKHOON.COM social community." },
                            location = location.ifBlank { "Yarkhoon Valley" },
                            phoneNumber = phoneNumber.trim(),
                            education = education.trim(),
                            jobTitle = jobTitle.trim(),
                            interests = interests,
                            isBusinessAccount = isBusiness,
                            businessCategory = if (isBusiness) category else null,
                            createdAt = System.currentTimeMillis()
                        )

                        // Check if user profile already exists in Cloud Firestore by UID or email (do not overwrite existing profiles)
                        val existingByUid = userRepository.getUserProfile(result.uid).getOrNull()
                        val existingByEmail = if (existingByUid == null && canonicalEmail.isNotBlank()) {
                            userRepository.findUserByEmail(canonicalEmail).getOrNull()
                        } else {
                            null
                        }
                        val existingProfile = existingByUid ?: existingByEmail
                        val resolvedUser: User
                        val feedback: String
                        if (existingProfile != null) {
                            resolvedUser = existingProfile.copy(
                                id = result.uid,
                                email = canonicalEmail.ifBlank { existingProfile.email }
                            )
                            if (existingByUid == null) {
                                userRepository.saveUserProfile(resolvedUser)
                            }
                            feedback = "Welcome back to YARKHOON.COM, ${resolvedUser.name}!"
                        } else {
                            val saveResult = userRepository.saveUserProfile(newUser)
                            resolvedUser = newUser
                            feedback = if (saveResult.isSuccess) {
                                "Account created & profile saved to Cloud Firestore! Welcome to YARKHOON.COM, ${newUser.name}!"
                            } else {
                                "Account created! Welcome to YARKHOON.COM, ${newUser.name}!"
                            }
                        }

                        // Check account suspension status
                        val suspensionResult = adminRepository.checkUserSuspension(result.uid)
                        val suspensionInfo = suspensionResult.getOrNull()
                        val isSuspended = suspensionInfo != null && suspensionInfo.isCurrentlySuspended

                        // Verify admin status and initialize 4-hour session window
                        val resolvedRole = adminRepository.getAdminRole()
                        val adminVerified = resolvedRole.canAccessAdminPanel && !isSuspended
                        val sessionStartMs = if (adminVerified) System.currentTimeMillis() else 0L
                        if (adminVerified) {
                            FirestoreAdminRepository.startAdminSession(
                                sessionStartMs = sessionStartMs,
                                rememberMe = false,
                                uid = result.uid,
                                email = canonicalEmail
                            )
                        } else {
                            FirestoreAdminRepository.invalidateAdminSession()
                        }

                        _uiState.update {
                            it.copy(
                                currentUser = resolvedUser,
                                isAuthenticated = true,
                                authLoading = false,
                                authError = null,
                                isUserSuspended = isSuspended,
                                suspensionInfo = if (isSuspended) suspensionInfo else null,
                                isAdmin = adminVerified,
                                adminRole = if (adminVerified) resolvedRole else AdminRole.NONE,
                                adminSessionStartedAt = sessionStartMs,
                                feedbackMessage = if (isSuspended) "Account Suspended: ${suspensionInfo?.suspensionReason}" else feedback
                            )
                        }
                        // Load real Firestore posts, stories, and reels
                        loadFeedAndSocialContent()
                        attachNotificationsListener(result.uid)
                    }
                    is AuthResult.Error -> {
                        val alreadyExists = result.message.contains("already exists", ignoreCase = true) ||
                            result.message.contains("already in use", ignoreCase = true)
                        _uiState.update {
                            it.copy(
                                authLoading = false,
                                authMode = if (alreadyExists) AuthMode.SIGN_IN else it.authMode,
                                authError = result.message
                            )
                        }
                    }
                }
            } finally {
                isPerformingExplicitAuth = false
            }
        }
    }

    fun updateUserProfile(
        name: String,
        bio: String,
        location: String,
        jobTitle: String,
        education: String,
        avatarUri: String?,
        coverUri: String?,
        phoneNumber: String = _uiState.value.currentUser.phoneNumber
    ) {
        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(feedbackMessage = "Action restricted: Your account is currently suspended.") }
            return
        }

        val current = _uiState.value.currentUser
        val trimmedName = name.trim().ifBlank { current.name }
        val updatedUser = current.copy(
            name = trimmedName,
            bio = bio.trim(),
            location = location.trim(),
            jobTitle = jobTitle.trim(),
            education = education.trim(),
            avatarUrl = avatarUri ?: current.avatarUrl,
            coverUrl = coverUri ?: current.coverUrl,
            phoneNumber = phoneNumber.trim().ifBlank { current.phoneNumber }
        )

        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                isProfileLoading = true,
                profileError = null
            )
        }

        viewModelScope.launch {
            if (trimmedName != current.name) {
                authRepository.updateDisplayName(trimmedName)
            }

            var finalAvatar = current.avatarUrl
            var finalCover = current.coverUrl
            var avatarUploadPendingOrFailed = false
            var coverUploadPendingOrFailed = false

            // Upload new local avatar image to Firebase Storage users/{uid}/profile/avatar.jpg
            if (avatarUri != null) {
                if (avatarUri.startsWith("http://") || avatarUri.startsWith("https://")) {
                    finalAvatar = avatarUri
                } else {
                    val uploadResult = storageRepository.uploadMedia(
                        uriString = avatarUri,
                        destinationPath = "users/${current.id}/profile/avatar_${System.currentTimeMillis()}.jpg"
                    )
                    if (uploadResult.isSuccess && !uploadResult.getOrNull().isNullOrBlank()) {
                        finalAvatar = uploadResult.getOrNull()!!
                    } else {
                        avatarUploadPendingOrFailed = true
                        android.util.Log.w("YarkhoonViewModel", "Avatar upload paused/failed: ${uploadResult.exceptionOrNull()?.message}")
                    }
                }
            }

            // Upload new local cover image to Firebase Storage users/{uid}/profile/cover.jpg
            if (coverUri != null) {
                if (coverUri.startsWith("http://") || coverUri.startsWith("https://")) {
                    finalCover = coverUri
                } else {
                    val uploadResult = storageRepository.uploadMedia(
                        uriString = coverUri,
                        destinationPath = "users/${current.id}/profile/cover_${System.currentTimeMillis()}.jpg"
                    )
                    if (uploadResult.isSuccess && !uploadResult.getOrNull().isNullOrBlank()) {
                        finalCover = uploadResult.getOrNull()!!
                    } else {
                        coverUploadPendingOrFailed = true
                        android.util.Log.w("YarkhoonViewModel", "Cover upload paused/failed: ${uploadResult.exceptionOrNull()?.message}")
                    }
                }
            }

            val persistedUser = updatedUser.copy(
                avatarUrl = finalAvatar,
                coverUrl = finalCover
            )

            val result = userRepository.saveUserProfile(persistedUser)
            if (result.isSuccess) {
                val feedback = if (avatarUploadPendingOrFailed || coverUploadPendingOrFailed) {
                    "Profile text information saved to Firestore! Remote photo upload is paused pending Cloud Storage plan activation."
                } else {
                    "Profile updated and saved to Cloud Firestore!"
                }
                _uiState.update {
                    it.copy(
                        currentUser = persistedUser,
                        isProfileLoading = false,
                        feedbackMessage = feedback
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile changes to Firestore."
                _uiState.update {
                    it.copy(
                        isProfileLoading = false,
                        profileError = err,
                        feedbackMessage = "Profile updated locally. Cloud sync: $err"
                    )
                }
            }
        }
    }

    fun dismissProfileError() {
        _uiState.update { it.copy(profileError = null) }
    }

    fun addStoryFromGallery(mediaUri: String, caption: String) {
        publishStory(mediaUri, caption)
    }

    fun addReelFromGallery(videoUri: String, caption: String, audioTrack: String) {
        publishReel(videoUri, caption, audioTrack)
    }

    fun addBusinessListing(
        businessName: String,
        category: BusinessCategory,
        serviceHighlight: String,
        description: String,
        location: String,
        phone: String,
        portfolioImages: List<String>
    ) {
        val currentUser = _uiState.value.currentUser
        val trimmedName = businessName.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please enter a valid business name.") }
            return
        }

        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(feedbackMessage = "Action restricted: Your account is currently suspended.") }
            return
        }

        if (_uiState.value.isSavingListing) return
        _uiState.update {
            it.copy(
                isSavingListing = true,
                listingUploadProgress = 0.1f,
                listingUploadError = null
            )
        }

        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()
            val bizId = "biz_${System.currentTimeMillis()}"

            // 1. Upload portfolio images to Firebase Storage under users/{uid}/businessListings/{bizId}/
            val targetUid = authRepository.currentUser?.uid ?: currentUser.id
            for ((index, uri) in portfolioImages.withIndex()) {
                if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    uploadedUrls.add(uri)
                } else {
                    val destPath = "users/$targetUid/businessListings/$bizId/photo_$index.jpg"
                    val uploadResult = storageRepository.uploadMedia(
                        uriString = uri,
                        destinationPath = destPath,
                        onProgress = { p ->
                            val totalProgress = 0.1f + (p * 0.5f / portfolioImages.size.coerceAtLeast(1))
                            _uiState.update { it.copy(listingUploadProgress = totalProgress) }
                        }
                    )
                    if (uploadResult.isSuccess && !uploadResult.getOrNull().isNullOrBlank()) {
                        uploadedUrls.add(uploadResult.getOrNull()!!)
                    } else {
                        val err = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                        android.util.Log.e("YarkhoonViewModel", "Business image upload failed for $destPath: $err")
                    }
                }
            }

            // 2. Persist business listing to Cloud Firestore collection "businessListings"
            val newBiz = BusinessProfile(
                id = bizId,
                owner = currentUser,
                businessName = trimmedName,
                category = category,
                serviceHighlight = serviceHighlight.trim(),
                description = description.trim(),
                location = location.trim().ifBlank { "Yarkhoon Valley" },
                phone = trimmedPhone,
                whatsapp = trimmedPhone,
                rating = 5.0f,
                reviewsCount = 1,
                isVerified = true,
                status = "active",
                portfolioImages = uploadedUrls,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val createResult = businessRepository.createBusinessListing(newBiz)
            if (createResult.isSuccess) {
                val created = createResult.getOrNull() ?: newBiz
                _uiState.update { state ->
                    state.copy(
                        businessProfiles = listOf(created) + state.businessProfiles.filterNot { it.id == created.id },
                        isSavingListing = false,
                        listingUploadProgress = 1.0f,
                        feedbackMessage = "Business profile for '${created.businessName}' registered in Business Hub!"
                    )
                }
            } else {
                val err = createResult.exceptionOrNull()?.message ?: "Failed to save business listing to Cloud Firestore."
                // Fallback: still add to local state with feedback notice
                _uiState.update { state ->
                    state.copy(
                        businessProfiles = listOf(newBiz) + state.businessProfiles,
                        isSavingListing = false,
                        feedbackMessage = "Business profile saved locally. Notice: $err"
                    )
                }
            }
        }
    }

    fun completeProfileSetup(bio: String, location: String) {
        _uiState.update { state ->
            val updatedUser = state.currentUser.copy(
                bio = bio.ifBlank { state.currentUser.bio },
                location = location.ifBlank { state.currentUser.location }
            )
            state.copy(
                currentUser = updatedUser,
                isAuthenticated = true,
                feedbackMessage = "Profile setup complete! Welcome to YARKHOON.COM."
            )
        }
    }

    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter your registered email address.") }
            return
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            _uiState.update { it.copy(authError = "Please enter a valid email address.") }
            return
        }

        if (_uiState.value.authLoading) return

        _uiState.update { it.copy(authLoading = true, authError = null) }

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(trimmedEmail)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        authLoading = false,
                        authMode = AuthMode.SIGN_IN,
                        authError = null,
                        feedbackMessage = "Password reset instructions sent to $trimmedEmail. Check your inbox."
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to send password reset email."
                _uiState.update {
                    it.copy(
                        authLoading = false,
                        authError = err
                    )
                }
            }
        }
    }

    fun logout() {
        val adminUid = _uiState.value.currentUser.id.takeIf { _uiState.value.isAdmin }
        adminInactivityWatcherJob?.cancel()
        adminInactivityWatcherJob = null
        conversationsListener?.remove()
        conversationsListener = null
        notificationsListener?.remove()
        notificationsListener = null
        detachMessagesListener()
        FirestoreAdminRepository.invalidateAdminSession()
        AdminSessionManager.invalidateAndClear()
        if (adminUid != null) {
            viewModelScope.launch {
                try {
                    adminRepository.terminateServerAdminSession(adminUid)
                } catch (_: Exception) {}
            }
        }
        authRepository.signOut()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                authMode = AuthMode.SIGN_IN,
                currentTab = MainNavTab.HOME,
                authError = null,
                isAdmin = false,
                adminRole = AdminRole.NONE,
                isAdminDashboardOpen = false,
                adminSessionStartedAt = 0L,
                isMessagesOpen = false,
                activeConversation = null,
                activeMessages = emptyList(),
                conversations = emptyList(),
                notifications = emptyList(),
                feedbackMessage = "Signed out of YARKHOON.COM."
            )
        }
    }

    // ---------------- Social Feed Interactions & Cloud Firestore Synchronization (Stage 7) ----------------
    fun loadFeedAndSocialContent() {
        val currentUid = authRepository.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            _uiState.update { state ->
                state.copy(
                    isFeedLoading = false,
                    feedError = null
                )
            }
            return
        }

        feedInitializationJob?.cancel()
        feedInitializationJob = viewModelScope.launch {
            _uiState.update { it.copy(isFeedLoading = true, feedError = null) }
            val postsResult = postRepository.getPosts()
            val savedIds = postRepository.getSavedPostIds(currentUid).getOrDefault(emptySet())
            val followingIds = userRepository.getFollowingUserIds(currentUid).getOrDefault(emptySet())

            if (postsResult.isSuccess) {
                val blockedIds = _uiState.value.blockedUserIds
                val rawPosts = postsResult.getOrNull().orEmpty()
                val firestorePosts = rawPosts.map { post ->
                    val isAuthorApproved = post.author.id == currentUid || followingIds.contains(post.author.id)
                    val isAuthorBlocked = blockedIds.contains(post.author.id)
                    val resolvedMedia = post.mediaUrls.map { url ->
                        storageRepository.resolveAuthorizedMediaUrl(
                            targetAuthorId = post.author.id,
                            targetAuthorIsPrivate = post.author.isPrivate,
                            rawMediaUrl = url,
                            isApprovedFollower = isAuthorApproved,
                            isBlocked = isAuthorBlocked,
                            contentType = "post",
                            contentId = post.id
                        )
                    }.filter { it.isNotBlank() }

                    post.copy(
                        mediaUrls = resolvedMedia,
                        isSaved = savedIds.contains(post.id)
                    )
                }

                // Preserve community dummy account posts from SampleData if they do not yet exist in Firestore
                val firestoreIds = firestorePosts.map { it.id }.toSet()
                val firestoreTexts = firestorePosts.map { it.text }.toSet()
                val communitySeedPosts = SampleData.initialPosts.filter { seedPost ->
                    !firestoreIds.contains(seedPost.id) && !firestoreTexts.contains(seedPost.text)
                }
                val allDisplayPosts = (firestorePosts + communitySeedPosts)
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestamp }

                _uiState.update { state ->
                    state.copy(
                        posts = allDisplayPosts,
                        savedPostIds = savedIds,
                        followingUserIds = followingIds,
                        isFeedLoading = false,
                        feedError = null
                    )
                }
            } else {
                val err = postsResult.exceptionOrNull()?.localizedMessage ?: "Failed to load posts from Cloud Firestore."
                _uiState.update { state ->
                    state.copy(
                        posts = SampleData.initialPosts,
                        savedPostIds = savedIds,
                        followingUserIds = followingIds,
                        isFeedLoading = false,
                        feedError = err
                    )
                }
            }

            // Load registered creators from Firestore and combine with community creators
            launch {
                val usersResult = userRepository.searchRegisteredUsers(currentUid, "", limit = 30)
                val registeredUsers = usersResult.getOrDefault(emptyList())
                val sampleCreators = SampleData.users.filter { it.id != currentUid }
                val combinedCreators = (registeredUsers + sampleCreators).distinctBy { it.id }

                _uiState.update { state ->
                    state.copy(
                        discoveredUsers = combinedCreators,
                        exploreContent = state.exploreContent.copy(recommendedCreators = combinedCreators)
                    )
                }
            }

            // Sync FCM registration token and attach conversations listener for messaging
            syncFcmToken()
            attachConversationsListener()

            // Real Stories from Firestore with authorized media resolution
            launch {
                val storiesResult = storyRepository.getStories()
                if (storiesResult.isSuccess) {
                    val rawStories = storiesResult.getOrNull().orEmpty()
                    val blockedIds = _uiState.value.blockedUserIds
                    val resolvedStories = rawStories.map { story ->
                        val isAuthorApproved = story.author.id == currentUid || followingIds.contains(story.author.id)
                        val isAuthorBlocked = blockedIds.contains(story.author.id)
                        val resolvedUrl = storageRepository.resolveAuthorizedMediaUrl(
                            targetAuthorId = story.author.id,
                            targetAuthorIsPrivate = story.author.isPrivate,
                            rawMediaUrl = story.mediaUrl,
                            isApprovedFollower = isAuthorApproved,
                            isBlocked = isAuthorBlocked,
                            contentType = "story",
                            contentId = story.id
                        )
                        story.copy(mediaUrl = resolvedUrl)
                    }.filter { it.mediaUrl.isNotBlank() }

                    if (resolvedStories.isNotEmpty()) {
                        _uiState.update { it.copy(stories = resolvedStories) }
                    }
                }
            }

            // Real Reels from Firestore with authorized media resolution
            launch {
                val reelsResult = reelRepository.getReels()
                if (reelsResult.isSuccess) {
                    val rawReels = reelsResult.getOrNull().orEmpty()
                    val blockedIds = _uiState.value.blockedUserIds
                    val resolvedReels = rawReels.map { reel ->
                        val isAuthorApproved = reel.author.id == currentUid || followingIds.contains(reel.author.id)
                        val isAuthorBlocked = blockedIds.contains(reel.author.id)
                        val resolvedUrl = storageRepository.resolveAuthorizedMediaUrl(
                            targetAuthorId = reel.author.id,
                            targetAuthorIsPrivate = reel.author.isPrivate,
                            rawMediaUrl = reel.videoMediaUrl,
                            isApprovedFollower = isAuthorApproved,
                            isBlocked = isAuthorBlocked,
                            contentType = "reel",
                            contentId = reel.id
                        )
                        reel.copy(videoMediaUrl = resolvedUrl)
                    }.filter { it.videoMediaUrl.isNotBlank() }

                    if (resolvedReels.isNotEmpty()) {
                        _uiState.update { it.copy(reels = resolvedReels) }
                    }
                }
            }

            // Real Business & Services from Firestore (Stage 9)
            launch {
                loadBusinessListings()
            }

            // Real Following, Privacy, Blocked, Muted & Favorites (Stage 12)
            launch {
                loadPrivacyRelationships()
            }

            // Real Buy & Sell Marketplace from Firestore (Stage 9)
            launch {
                loadMarketplaceListings()
            }

            // Real Explore & Trending Content from Firestore (Stage 10)
            launch {
                loadExploreContent()
            }
        }
    }

    fun refreshSocialFeed() {
        loadFeedAndSocialContent()
    }

    fun dismissFeedError() {
        _uiState.update { it.copy(feedError = null) }
    }

    fun dismissPublishError() {
        _uiState.update { it.copy(publishError = null) }
    }

    fun toggleLike(postId: String) {
        val post = _uiState.value.posts.find { it.id == postId } ?: return
        if (post.isLiked) {
            setReaction(postId, post.userReaction ?: "like")
        } else {
            setReaction(postId, "like")
        }
    }

    fun setReaction(postId: String, reaction: String) {
        val originalPost = _uiState.value.posts.find { it.id == postId }
        var firestoreLikeChange: Boolean? = null
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    val currentReaction = post.userReaction
                    if (currentReaction == reaction) {
                        // User clicked same emoji: toggle off reaction
                        firestoreLikeChange = false
                        val newCount = (post.likesCount - 1).coerceAtLeast(0)
                        post.copy(
                            isLiked = false,
                            userReaction = null,
                            likesCount = newCount
                        )
                    } else {
                        // User selected new emoji reaction
                        val wasAlreadyLiked = post.isLiked
                        if (!wasAlreadyLiked) {
                            firestoreLikeChange = true
                        }
                        val newCount = if (!wasAlreadyLiked) post.likesCount + 1 else post.likesCount
                        post.copy(
                            isLiked = true,
                            userReaction = reaction,
                            likesCount = newCount
                        )
                    }
                } else post
            }
            state.copy(posts = updatedPosts)
        }

        viewModelScope.launch {
            if (firestoreLikeChange != null) {
                val result = postRepository.toggleLikePost(postId, firestoreLikeChange!!)
                if (result.isFailure) {
                    val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to update like"
                    _uiState.update { state ->
                        val rolledBack = state.posts.map { post ->
                            if (post.id == postId && originalPost != null) {
                                post.copy(
                                    isLiked = originalPost.isLiked,
                                    userReaction = originalPost.userReaction,
                                    likesCount = originalPost.likesCount
                                )
                            } else post
                        }
                        val updatedActive = if (state.activeCommentsPost?.id == postId && originalPost != null) {
                            state.activeCommentsPost.copy(
                                isLiked = originalPost.isLiked,
                                userReaction = originalPost.userReaction,
                                likesCount = originalPost.likesCount
                            )
                        } else state.activeCommentsPost
                        state.copy(
                            posts = rolledBack,
                            activeCommentsPost = updatedActive,
                            feedbackMessage = "Like update failed: $err"
                        )
                    }
                    return@launch
                }
            }
            if (firestoreLikeChange == true) {
                val post = _uiState.value.posts.find { it.id == postId }
                val currentUser = _uiState.value.currentUser
                if (post != null && post.author.id != currentUser.id && currentUser.id.isNotBlank() && post.author.id.isNotBlank()) {
                    notificationRepository.sendLikeNotification(
                        recipientId = post.author.id,
                        sender = currentUser,
                        postId = postId,
                        postSnippet = post.text
                    )
                }
            }
        }
    }

    fun toggleRepost(postId: String) {
        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    val newRepost = !post.isReposted
                    val newCount = if (newRepost) post.repostsCount + 1 else (post.repostsCount - 1).coerceAtLeast(0)
                    post.copy(isReposted = newRepost, repostsCount = newCount)
                } else post
            }
            val target = updatedPosts.find { it.id == postId }
            val msg = if (target?.isReposted == true) "Reposted to your profile!" else "Repost undone."
            state.copy(posts = updatedPosts, feedbackMessage = msg)
        }
    }

    fun toggleBookmark(postId: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        var isSavedNow = false
        _uiState.update { state ->
            val updatedSaved = state.savedPostIds.toMutableSet()
            val currentlySaved = updatedSaved.contains(postId)
            isSavedNow = !currentlySaved
            if (isSavedNow) {
                updatedSaved.add(postId)
            } else {
                updatedSaved.remove(postId)
            }

            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) post.copy(isSaved = isSavedNow) else post
            }

            val msg = if (isSavedNow) "Post saved to your bookmarks" else "Post removed from bookmarks"
            state.copy(
                posts = updatedPosts,
                savedPostIds = updatedSaved,
                feedbackMessage = msg
            )
        }

        viewModelScope.launch {
            val result = postRepository.toggleSavePost(currentUid, postId, isSavedNow)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to update bookmark"
                _uiState.update { state ->
                    val rollbackSaved = state.savedPostIds.toMutableSet()
                    if (isSavedNow) rollbackSaved.remove(postId) else rollbackSaved.add(postId)
                    state.copy(
                        posts = state.posts.map { post ->
                            if (post.id == postId) post.copy(isSaved = !isSavedNow) else post
                        },
                        savedPostIds = rollbackSaved,
                        feedbackMessage = "Bookmark error: $err"
                    )
                }
            }
        }
    }

    // ---------------- Stage 11: Sharing & Reposts ----------------
    fun openSharePost(post: Post) {
        _uiState.update { it.copy(activeSharePost = post) }
    }

    fun closeSharePost() {
        _uiState.update { it.copy(activeSharePost = null) }
    }

    fun sharePostToFeed(post: Post, thoughts: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        val currentUser = _uiState.value.currentUser
        viewModelScope.launch {
            val result = postRepository.repostPost(currentUid, currentUser, post, thoughts)
            if (result.isSuccess) {
                val createdRepost = result.getOrNull()
                _uiState.update { state ->
                    val updatedPosts = if (createdRepost != null) {
                        listOf(createdRepost) + state.posts.map { p ->
                            if (p.id == post.id) p.copy(sharesCount = p.sharesCount + 1, repostsCount = p.repostsCount + 1) else p
                        }
                    } else state.posts
                    state.copy(posts = updatedPosts, feedbackMessage = "Post shared to your feed!", activeSharePost = null)
                }
            } else {
                _uiState.update { it.copy(feedbackMessage = "Could not share post: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun copyPostLink(context: Context, postId: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val postUrl = "https://yarkhoon.com/post/$postId"
            val clip = ClipData.newPlainText("YARKHOON Post", postUrl)
            clipboard?.setPrimaryClip(clip)

            _uiState.update { state ->
                val updatedPosts = state.posts.map { post ->
                    if (post.id == postId) post.copy(sharesCount = post.sharesCount + 1) else post
                }
                state.copy(posts = updatedPosts, feedbackMessage = "Post link copied to clipboard!", activeSharePost = null)
            }

            viewModelScope.launch {
                postRepository.incrementShareCount(postId)
            }
        } catch (_: Exception) {}
    }

    fun sharePostExternal(context: Context, post: Post) {
        try {
            val postUrl = "https://yarkhoon.com/post/${post.id}"
            val shareText = "${post.author.name} on YARKHOON.COM:\n\"${post.text.take(120)}\"\n\n$postUrl"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "Post by ${post.author.name} on YARKHOON.COM")
                type = "text/plain"
            }
            val shareChooser = Intent.createChooser(sendIntent, "Share Post via")
            shareChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareChooser)

            _uiState.update { state ->
                val updatedPosts = state.posts.map { p ->
                    if (p.id == post.id) p.copy(sharesCount = p.sharesCount + 1) else p
                }
                state.copy(posts = updatedPosts, activeSharePost = null)
            }

            viewModelScope.launch {
                postRepository.incrementShareCount(post.id)
            }
        } catch (_: Exception) {}
    }

    // ---------------- Stage 11: Threaded Comments, Replies & Likes ----------------
    fun openPostById(postId: String) {
        val existing = _uiState.value.posts.find { it.id == postId }
        if (existing != null) {
            openComments(existing)
        } else {
            viewModelScope.launch {
                val result = postRepository.getPost(postId)
                val post = result.getOrNull()
                if (post != null) {
                    _uiState.update { state ->
                        state.copy(posts = (listOf(post) + state.posts).distinctBy { it.id })
                    }
                    openComments(post)
                } else {
                    _uiState.update { it.copy(feedbackMessage = "Post could not be loaded or was removed.") }
                }
            }
        }
    }

    fun openComments(post: Post) {
        _uiState.update {
            it.copy(
                activeCommentsPost = post,
                activePostComments = post.comments,
                isLoadingComments = true,
                commentError = null
            )
        }

        viewModelScope.launch {
            val result = postRepository.getComments(post.id)
            if (result.isSuccess) {
                val fetched = result.getOrNull().orEmpty()
                _uiState.update { state ->
                    val updatedPosts = state.posts.map { p ->
                        if (p.id == post.id) p.copy(comments = fetched, commentsCount = fetched.size + fetched.sumOf { it.repliesCount }) else p
                    }
                    val updatedActive = updatedPosts.find { it.id == post.id } ?: post.copy(comments = fetched)
                    state.copy(
                        posts = updatedPosts,
                        activeCommentsPost = updatedActive,
                        activePostComments = fetched,
                        isLoadingComments = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    fun closeComments() {
        _uiState.update {
            it.copy(
                activeCommentsPost = null,
                activePostComments = emptyList(),
                isLoadingComments = false,
                commentError = null
            )
        }
    }

    fun addComment(
        postId: String,
        text: String,
        parentId: String? = null,
        replyToUsername: String? = null
    ) {
        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(feedbackMessage = "Comment failed: Your account is currently suspended.") }
            return
        }
        if (text.isBlank()) return
        val currentUser = _uiState.value.currentUser
        val newComment = Comment(
            id = "comment_${System.currentTimeMillis()}",
            postId = postId,
            author = currentUser,
            text = text.trim(),
            timestamp = "Just now",
            timestampMs = System.currentTimeMillis(),
            parentId = parentId,
            replyToUsername = replyToUsername
        )

        _uiState.update { state ->
            val updatedComments: List<Comment> = if (parentId == null) {
                state.activePostComments + newComment
            } else {
                state.activePostComments.map { topComment ->
                    if (topComment.id == parentId) {
                        val newReplies = topComment.replies + newComment
                        topComment.copy(replies = newReplies, repliesCount = newReplies.size)
                    } else topComment
                }
            }

            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    val currentCount = post.commentsCount + 1
                    post.copy(comments = updatedComments, commentsCount = currentCount)
                } else post
            }
            val updatedActive = updatedPosts.find { it.id == postId }
            state.copy(
                posts = updatedPosts,
                activePostComments = updatedComments,
                activeCommentsPost = updatedActive
            )
        }

        viewModelScope.launch {
            val result = postRepository.addComment(postId, newComment)
            if (result.isFailure) {
                _uiState.update { state ->
                    val rolledBackComments = if (parentId == null) {
                        state.activePostComments.filter { it.id != newComment.id }
                    } else {
                        state.activePostComments.map { top ->
                            if (top.id == parentId) {
                                val remainingReplies = top.replies.filter { it.id != newComment.id }
                                top.copy(replies = remainingReplies, repliesCount = remainingReplies.size)
                            } else top
                        }
                    }
                    val rolledBackPosts = state.posts.map { post ->
                        if (post.id == postId) {
                            val count = (post.commentsCount - 1).coerceAtLeast(0)
                            post.copy(comments = rolledBackComments, commentsCount = count)
                        } else post
                    }
                    state.copy(
                        posts = rolledBackPosts,
                        activePostComments = rolledBackComments,
                        activeCommentsPost = rolledBackPosts.find { it.id == postId },
                        commentError = "Failed to submit comment: ${result.exceptionOrNull()?.localizedMessage}"
                    )
                }
            } else {
                val post = _uiState.value.posts.find { it.id == postId } ?: _uiState.value.activeCommentsPost
                if (parentId != null) {
                    val parentComment = _uiState.value.activePostComments.find { it.id == parentId }
                    if (parentComment != null && parentComment.author.id != currentUser.id && parentComment.author.id.isNotBlank()) {
                        notificationRepository.sendReplyNotification(
                            recipientId = parentComment.author.id,
                            sender = currentUser,
                            postId = postId,
                            replyText = text,
                            commentId = newComment.id
                        )
                    }
                    if (post != null && post.author.id != currentUser.id && post.author.id != parentComment?.author?.id && post.author.id.isNotBlank()) {
                        notificationRepository.sendCommentNotification(
                            recipientId = post.author.id,
                            sender = currentUser,
                            postId = postId,
                            commentText = text,
                            commentId = newComment.id
                        )
                    }
                } else {
                    if (post != null && post.author.id != currentUser.id && post.author.id.isNotBlank()) {
                        notificationRepository.sendCommentNotification(
                            recipientId = post.author.id,
                            sender = currentUser,
                            postId = postId,
                            commentText = text,
                            commentId = newComment.id
                        )
                    }
                }
            }
        }
    }

    fun toggleLikeComment(postId: String, commentId: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        var originalIsLiked: Boolean? = null
        var originalLikesCount: Int = 0
        for (c in _uiState.value.activePostComments) {
            if (c.id == commentId) {
                originalIsLiked = c.isLiked
                originalLikesCount = c.likesCount
                break
            }
            for (r in c.replies) {
                if (r.id == commentId) {
                    originalIsLiked = r.isLiked
                    originalLikesCount = r.likesCount
                    break
                }
            }
            if (originalIsLiked != null) break
        }
        var isLikedNow = false

        _uiState.update { state ->
            val updatedComments = state.activePostComments.map { comment ->
                if (comment.id == commentId) {
                    val nextLiked = !comment.isLiked
                    isLikedNow = nextLiked
                    val nextCount = if (nextLiked) comment.likesCount + 1 else (comment.likesCount - 1).coerceAtLeast(0)
                    comment.copy(isLiked = nextLiked, likesCount = nextCount)
                } else {
                    // Check inside replies
                    val updatedReplies = comment.replies.map { reply ->
                        if (reply.id == commentId) {
                            val nextLiked = !reply.isLiked
                            isLikedNow = nextLiked
                            val nextCount = if (nextLiked) reply.likesCount + 1 else (reply.likesCount - 1).coerceAtLeast(0)
                            reply.copy(isLiked = nextLiked, likesCount = nextCount)
                        } else reply
                    }
                    comment.copy(replies = updatedReplies)
                }
            }
            state.copy(activePostComments = updatedComments)
        }

        viewModelScope.launch {
            val result = postRepository.toggleLikeComment(postId, commentId, isLikedNow)
            if (result.isFailure) {
                _uiState.update { state ->
                    val rolledBackComments = state.activePostComments.map { comment ->
                        if (comment.id == commentId && originalIsLiked != null) {
                            comment.copy(isLiked = originalIsLiked, likesCount = originalLikesCount)
                        } else {
                            val rolledBackReplies = comment.replies.map { reply ->
                                if (reply.id == commentId && originalIsLiked != null) {
                                    reply.copy(isLiked = originalIsLiked, likesCount = originalLikesCount)
                                } else reply
                            }
                            comment.copy(replies = rolledBackReplies)
                        }
                    }
                    state.copy(
                        activePostComments = rolledBackComments,
                        commentError = "Failed to update comment like: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun editComment(postId: String, commentId: String, newText: String) {
        if (newText.isBlank()) return
        var originalText: String? = null
        var originalIsEdited: Boolean = false
        for (c in _uiState.value.activePostComments) {
            if (c.id == commentId) {
                originalText = c.text
                originalIsEdited = c.isEdited
                break
            }
            for (r in c.replies) {
                if (r.id == commentId) {
                    originalText = r.text
                    originalIsEdited = r.isEdited
                    break
                }
            }
            if (originalText != null) break
        }

        _uiState.update { state ->
            val updatedComments = state.activePostComments.map { comment ->
                if (comment.id == commentId) {
                    comment.copy(text = newText.trim(), isEdited = true)
                } else {
                    val updatedReplies = comment.replies.map { reply ->
                        if (reply.id == commentId) reply.copy(text = newText.trim(), isEdited = true) else reply
                    }
                    comment.copy(replies = updatedReplies)
                }
            }
            state.copy(activePostComments = updatedComments)
        }

        viewModelScope.launch {
            val result = postRepository.editComment(postId, commentId, newText)
            if (result.isFailure) {
                _uiState.update { state ->
                    val rolledBackComments = state.activePostComments.map { comment ->
                        if (comment.id == commentId && originalText != null) {
                            comment.copy(text = originalText, isEdited = originalIsEdited)
                        } else {
                            val rolledBackReplies = comment.replies.map { reply ->
                                if (reply.id == commentId && originalText != null) {
                                    reply.copy(text = originalText, isEdited = originalIsEdited)
                                } else reply
                            }
                            comment.copy(replies = rolledBackReplies)
                        }
                    }
                    state.copy(
                        activePostComments = rolledBackComments,
                        commentError = "Failed to edit comment: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        var targetComment: Comment? = null
        for (c in _uiState.value.activePostComments) {
            if (c.id == commentId) {
                targetComment = c
                break
            }
            for (r in c.replies) {
                if (r.id == commentId) {
                    targetComment = r
                    break
                }
            }
            if (targetComment != null) break
        }

        _uiState.update { state ->
            val updatedComments = state.activePostComments.filter { it.id != commentId }.map { comment ->
                val remainingReplies = comment.replies.filter { it.id != commentId }
                comment.copy(replies = remainingReplies, repliesCount = remainingReplies.size)
            }
            val updatedPosts = state.posts.map { post ->
                if (post.id == postId) {
                    val newCount = (post.commentsCount - 1).coerceAtLeast(0)
                    post.copy(comments = updatedComments, commentsCount = newCount)
                } else post
            }
            val updatedActive = updatedPosts.find { it.id == postId }
            state.copy(
                posts = updatedPosts,
                activePostComments = updatedComments,
                activeCommentsPost = updatedActive
            )
        }

        viewModelScope.launch {
            val result = postRepository.deleteComment(postId, commentId)
            if (result.isFailure) {
                _uiState.update { state ->
                    val restoredComments = if (targetComment != null) {
                        if (targetComment.parentId == null) {
                            (state.activePostComments + targetComment).distinctBy { it.id }.sortedBy { it.timestampMs }
                        } else {
                            state.activePostComments.map { top ->
                                if (top.id == targetComment.parentId) {
                                    val restoredReplies = (top.replies + targetComment).distinctBy { it.id }.sortedBy { it.timestampMs }
                                    top.copy(replies = restoredReplies, repliesCount = restoredReplies.size)
                                } else top
                            }
                        }
                    } else state.activePostComments

                    val rolledBackPosts = state.posts.map { post ->
                        if (post.id == postId) {
                            val count = post.commentsCount + 1
                            post.copy(comments = restoredComments, commentsCount = count)
                        } else post
                    }
                    state.copy(
                        posts = rolledBackPosts,
                        activePostComments = restoredComments,
                        activeCommentsPost = rolledBackPosts.find { it.id == postId },
                        commentError = "Failed to delete comment: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    // ---------------- Stage 12: Following System, Privacy, Blocking, Muting & Favorites ----------------

    fun loadPrivacyRelationships() {
        val currentUid = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val sentReqs = userRepository.getSentFollowRequestUserIds(currentUid).getOrDefault(emptySet())
            val pendingReqs = userRepository.getPendingFollowRequests(currentUid).getOrDefault(emptyList())
            val blockedIds = userRepository.getBlockedUserIds(currentUid).getOrDefault(emptySet())
            val blockedList = userRepository.getBlockedUsers(currentUid).getOrDefault(emptyList())
            val mutedIds = userRepository.getMutedUserIds(currentUid).getOrDefault(emptySet())
            val mutedList = userRepository.getMutedUsers(currentUid).getOrDefault(emptyList())
            val favIds = userRepository.getFavoriteUserIds(currentUid).getOrDefault(emptySet())
            val favList = userRepository.getFavoriteUsers(currentUid).getOrDefault(emptyList())

            _uiState.update { state ->
                state.copy(
                    sentFollowRequestUserIds = sentReqs,
                    pendingFollowRequests = pendingReqs,
                    blockedUserIds = blockedIds,
                    blockedUsers = blockedList,
                    mutedUserIds = mutedIds,
                    mutedUsers = mutedList,
                    favoriteUserIds = favIds,
                    favoriteUsers = favList
                )
            }
        }
    }

    fun updateAccountPrivacy(isPrivate: Boolean) {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            state.copy(
                currentUser = state.currentUser.copy(isPrivate = isPrivate),
                feedbackMessage = if (isPrivate) "Account set to Private. Only approved followers can view your content." else "Account set to Public."
            )
        }
        viewModelScope.launch {
            userRepository.updateAccountPrivacy(currentUid, isPrivate)
        }
    }

    fun toggleFollowUser(targetUser: User) {
        val currentUid = authRepository.currentUser?.uid ?: return
        if (currentUid == targetUser.id) return // Cannot follow self

        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(feedbackMessage = "Action restricted: Your account is currently suspended.") }
            return
        }

        if (_uiState.value.blockedUserIds.contains(targetUser.id)) {
            _uiState.update { it.copy(feedbackMessage = "Cannot follow a blocked account.") }
            return
        }

        val isCurrentlyFollowing = _uiState.value.followingUserIds.contains(targetUser.id)
        val isCurrentlyRequested = _uiState.value.sentFollowRequestUserIds.contains(targetUser.id)

        if (isCurrentlyFollowing) {
            val prevFollowing = _uiState.value.followingUserIds
            val prevFollowingCount = _uiState.value.currentUser.followingCount
            val prevFollowersCount = _uiState.value.selectedUserProfile?.takeIf { it.id == targetUser.id }?.followersCount
            // Unfollow
            _uiState.update { state ->
                val nextFollowing = state.followingUserIds - targetUser.id
                val updatedCurr = state.currentUser.copy(followingCount = (state.currentUser.followingCount - 1).coerceAtLeast(0))
                val updatedSelected = if (state.selectedUserProfile?.id == targetUser.id) {
                    state.selectedUserProfile.copy(followersCount = (state.selectedUserProfile.followersCount - 1).coerceAtLeast(0))
                } else state.selectedUserProfile

                state.copy(
                    followingUserIds = nextFollowing,
                    currentUser = updatedCurr,
                    selectedUserProfile = updatedSelected,
                    feedbackMessage = "Unfollowed ${targetUser.name}"
                )
            }
            viewModelScope.launch {
                val result = userRepository.toggleFollowUser(currentUid, targetUser.id, false)
                if (result.isFailure) {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to unfollow ${targetUser.name}. Please check connection or permissions."
                    _uiState.update { state ->
                        state.copy(
                            followingUserIds = prevFollowing,
                            currentUser = state.currentUser.copy(followingCount = prevFollowingCount),
                            selectedUserProfile = if (state.selectedUserProfile?.id == targetUser.id && prevFollowersCount != null) {
                                state.selectedUserProfile.copy(followersCount = prevFollowersCount)
                            } else state.selectedUserProfile,
                            feedbackMessage = errorMsg
                        )
                    }
                }
            }
        } else if (targetUser.isPrivate) {
            // Private account
            if (isCurrentlyRequested) {
                cancelFollowRequest(targetUser.id)
            } else {
                sendFollowRequest(targetUser)
            }
        } else {
            val prevFollowing = _uiState.value.followingUserIds
            val prevFollowingCount = _uiState.value.currentUser.followingCount
            val prevFollowersCount = _uiState.value.selectedUserProfile?.takeIf { it.id == targetUser.id }?.followersCount
            // Public account - immediate follow
            _uiState.update { state ->
                val nextFollowing = state.followingUserIds + targetUser.id
                val updatedCurr = state.currentUser.copy(followingCount = state.currentUser.followingCount + 1)
                val updatedSelected = if (state.selectedUserProfile?.id == targetUser.id) {
                    state.selectedUserProfile.copy(followersCount = state.selectedUserProfile.followersCount + 1)
                } else state.selectedUserProfile

                state.copy(
                    followingUserIds = nextFollowing,
                    currentUser = updatedCurr,
                    selectedUserProfile = updatedSelected,
                    feedbackMessage = "You are now following ${targetUser.name}"
                )
            }
            viewModelScope.launch {
                val result = userRepository.toggleFollowUser(currentUid, targetUser.id, true)
                if (result.isFailure) {
                    val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to follow ${targetUser.name}. Please check connection or permissions."
                    _uiState.update { state ->
                        state.copy(
                            followingUserIds = prevFollowing,
                            currentUser = state.currentUser.copy(followingCount = prevFollowingCount),
                            selectedUserProfile = if (state.selectedUserProfile?.id == targetUser.id && prevFollowersCount != null) {
                                state.selectedUserProfile.copy(followersCount = prevFollowersCount)
                            } else state.selectedUserProfile,
                            feedbackMessage = errorMsg
                        )
                    }
                } else {
                    // Following succeeded: populate Following feed with newly followed creator's posts immediately
                    val authorPostsRes = postRepository.getUserPosts(targetUser.id)
                    val authorPosts = authorPostsRes.getOrNull().orEmpty()
                    if (authorPosts.isNotEmpty()) {
                        _uiState.update { state ->
                            val combined = (authorPosts + state.posts).distinctBy { it.id }.sortedByDescending { it.timestamp }
                            state.copy(posts = combined)
                        }
                    }
                }
            }
        }
    }

    // Overload for compatibility with ID-only callers
    fun toggleFollowUser(targetUserId: String) {
        val targetUser = _uiState.value.selectedUserProfile?.takeIf { it.id == targetUserId }
            ?: _uiState.value.discoveredUsers.find { it.id == targetUserId }
            ?: _uiState.value.exploreContent.recommendedCreators.find { it.id == targetUserId }
            ?: _uiState.value.posts.map { it.author }.find { it.id == targetUserId }
            ?: User(id = targetUserId, name = "Member", handle = "@$targetUserId", avatarUrl = "")

        toggleFollowUser(targetUser)
    }

    fun sendFollowRequest(targetUser: User) {
        val currentUid = authRepository.currentUser?.uid ?: return
        val prevRequests = _uiState.value.sentFollowRequestUserIds
        _uiState.update { state ->
            state.copy(
                sentFollowRequestUserIds = state.sentFollowRequestUserIds + targetUser.id,
                feedbackMessage = "Follow request sent to ${targetUser.name}"
            )
        }
        viewModelScope.launch {
            val result = userRepository.sendFollowRequest(_uiState.value.currentUser, targetUser.id)
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to send follow request."
                _uiState.update { state ->
                    state.copy(
                        sentFollowRequestUserIds = prevRequests,
                        feedbackMessage = errorMsg
                    )
                }
            }
        }
    }

    fun cancelFollowRequest(targetUserId: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        val prevRequests = _uiState.value.sentFollowRequestUserIds
        _uiState.update { state ->
            state.copy(
                sentFollowRequestUserIds = state.sentFollowRequestUserIds - targetUserId,
                feedbackMessage = "Follow request canceled"
            )
        }
        viewModelScope.launch {
            val result = userRepository.cancelFollowRequest(currentUid, targetUserId)
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to cancel follow request."
                _uiState.update { state ->
                    state.copy(
                        sentFollowRequestUserIds = prevRequests,
                        feedbackMessage = errorMsg
                    )
                }
            }
        }
    }

    fun approveFollowRequest(requester: User) {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            val updatedReqs = state.pendingFollowRequests.filter { it.requester.id != requester.id }
            val updatedUser = state.currentUser.copy(followersCount = state.currentUser.followersCount + 1)
            state.copy(
                pendingFollowRequests = updatedReqs,
                currentUser = updatedUser,
                feedbackMessage = "Approved ${requester.name}'s follow request."
            )
        }
        viewModelScope.launch {
            userRepository.approveFollowRequest(_uiState.value.currentUser, requester)
        }
    }

    fun rejectFollowRequest(requesterId: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            val updatedReqs = state.pendingFollowRequests.filter { it.requester.id != requesterId }
            state.copy(
                pendingFollowRequests = updatedReqs,
                feedbackMessage = "Declined follow request."
            )
        }
        viewModelScope.launch {
            userRepository.rejectFollowRequest(currentUid, requesterId)
        }
    }

    fun blockUser(targetUser: User) {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            val updatedBlockedIds = state.blockedUserIds + targetUser.id
            val updatedBlockedList = state.blockedUsers.filter { it.id != targetUser.id } + targetUser
            val updatedFollowing = state.followingUserIds - targetUser.id
            val updatedSentReqs = state.sentFollowRequestUserIds - targetUser.id
            val updatedPendingReqs = state.pendingFollowRequests.filter { it.requester.id != targetUser.id }
            val updatedFavs = state.favoriteUserIds - targetUser.id
            val updatedFavList = state.favoriteUsers.filter { it.id != targetUser.id }

            state.copy(
                blockedUserIds = updatedBlockedIds,
                blockedUsers = updatedBlockedList,
                followingUserIds = updatedFollowing,
                sentFollowRequestUserIds = updatedSentReqs,
                pendingFollowRequests = updatedPendingReqs,
                favoriteUserIds = updatedFavs,
                favoriteUsers = updatedFavList,
                feedbackMessage = "Blocked @${targetUser.handle.removePrefix("@")}. Their content and messages are now hidden."
            )
        }
        viewModelScope.launch {
            userRepository.blockUser(currentUid, targetUser.id)
        }
    }

    fun unblockUser(targetUserId: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            val updatedBlockedIds = state.blockedUserIds - targetUserId
            val updatedBlockedList = state.blockedUsers.filter { it.id != targetUserId }
            state.copy(
                blockedUserIds = updatedBlockedIds,
                blockedUsers = updatedBlockedList,
                feedbackMessage = "Account unblocked."
            )
        }
        viewModelScope.launch {
            userRepository.unblockUser(currentUid, targetUserId)
        }
    }

    fun toggleMuteUser(targetUser: User) {
        val currentUid = authRepository.currentUser?.uid ?: return
        val isMuted = _uiState.value.mutedUserIds.contains(targetUser.id)
        _uiState.update { state ->
            val nextMutedIds = if (isMuted) state.mutedUserIds - targetUser.id else state.mutedUserIds + targetUser.id
            val nextMutedList = if (isMuted) state.mutedUsers.filter { it.id != targetUser.id } else state.mutedUsers.filter { it.id != targetUser.id } + targetUser
            val msg = if (isMuted) "Unmuted @${targetUser.handle.removePrefix("@")}" else "Muted @${targetUser.handle.removePrefix("@")}. Their posts are hidden from your feed."
            state.copy(
                mutedUserIds = nextMutedIds,
                mutedUsers = nextMutedList,
                feedbackMessage = msg
            )
        }
        viewModelScope.launch {
            if (isMuted) userRepository.unmuteUser(currentUid, targetUser.id)
            else userRepository.muteUser(currentUid, targetUser.id)
        }
    }

    fun toggleFavoriteUser(targetUser: User) {
        val currentUid = authRepository.currentUser?.uid ?: return
        val isFav = _uiState.value.favoriteUserIds.contains(targetUser.id)
        val prevFavIds = _uiState.value.favoriteUserIds
        val prevFavList = _uiState.value.favoriteUsers
        _uiState.update { state ->
            val nextFavIds = if (isFav) state.favoriteUserIds - targetUser.id else state.favoriteUserIds + targetUser.id
            val nextFavList = if (isFav) state.favoriteUsers.filter { it.id != targetUser.id } else state.favoriteUsers.filter { it.id != targetUser.id } + targetUser
            val msg = if (isFav) "Removed ${targetUser.name} from Favorites" else "Added ${targetUser.name} to Favorites"
            state.copy(
                favoriteUserIds = nextFavIds,
                favoriteUsers = nextFavList,
                feedbackMessage = msg
            )
        }
        viewModelScope.launch {
            val result = if (isFav) userRepository.removeFavorite(currentUid, targetUser.id)
            else userRepository.addFavorite(currentUid, targetUser.id)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to update favorites"
                _uiState.update { state ->
                    state.copy(
                        favoriteUserIds = prevFavIds,
                        favoriteUsers = prevFavList,
                        feedbackMessage = "Favorites error: $err"
                    )
                }
            }
        }
    }

    fun openPrivacySettings() {
        loadPrivacyRelationships()
        _uiState.update { it.copy(isPrivacySettingsOpen = true) }
    }

    fun closePrivacySettings() {
        _uiState.update { it.copy(isPrivacySettingsOpen = false) }
    }

    fun openFollowRequests() {
        loadPrivacyRelationships()
        _uiState.update { it.copy(isFollowRequestsOpen = true) }
    }

    fun closeFollowRequests() {
        _uiState.update { it.copy(isFollowRequestsOpen = false) }
    }

    fun openUserProfileByHandle(handle: String) {
        val cleanHandle = handle.removePrefix("@").lowercase()
        // Check discovered users, explore creators, or post authors
        val existing = _uiState.value.discoveredUsers.find { it.handle.removePrefix("@").lowercase() == cleanHandle }
            ?: _uiState.value.exploreContent.recommendedCreators.find { it.handle.removePrefix("@").lowercase() == cleanHandle }
            ?: _uiState.value.posts.map { it.author }.find { it.handle.removePrefix("@").lowercase() == cleanHandle }

        if (existing != null) {
            openUserProfile(existing)
        } else {
            val currentUid = authRepository.currentUser?.uid ?: return
            viewModelScope.launch {
                val result = userRepository.searchRegisteredUsers(currentUid, cleanHandle, limit = 5)
                val found = result.getOrNull()?.find { it.handle.removePrefix("@").lowercase() == cleanHandle }
                if (found != null) {
                    openUserProfile(found)
                } else {
                    val profileLookup = userRepository.getUserProfile(cleanHandle)
                    val lookedUp = profileLookup.getOrNull()
                    if (lookedUp != null) {
                        openUserProfile(lookedUp)
                    } else {
                        _uiState.update { it.copy(feedbackMessage = "Member @$cleanHandle not found.") }
                    }
                }
            }
        }
    }

    fun createPost(
        text: String,
        type: PostType,
        mediaUrl: String?,
        tags: List<String>,
        location: String? = null
    ) {
        val currentUid = authRepository.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            _uiState.update { it.copy(publishError = "Must be signed in to create a post.") }
            return
        }

        if (_uiState.value.isPublishingPost) {
            return
        }

        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(publishError = "Action restricted: Your account is currently suspended.") }
            return
        }

        if (text.isBlank() && mediaUrl.isNullOrBlank()) {
            _uiState.update { it.copy(publishError = "Please write a post or select a photo/video.") }
            return
        }

        val currentUser = _uiState.value.currentUser
        val authorUser = if (currentUser.id == currentUid) currentUser else currentUser.copy(id = currentUid)
        val postId = "post_${System.currentTimeMillis()}"

        _uiState.update {
            it.copy(
                isPublishingPost = true,
                uploadProgress = 0f,
                publishError = null
            )
        }

        viewModelScope.launch {
            var finalMediaUrl: String? = null
            if (!mediaUrl.isNullOrBlank()) {
                val uploadResult = postRepository.uploadPostMedia(
                    localUriString = mediaUrl,
                    authorId = currentUid,
                    postId = postId,
                    postType = type,
                    isPrivate = currentUser.isPrivate,
                    onProgress = { progress ->
                        _uiState.update { it.copy(uploadProgress = progress) }
                    }
                )

                if (uploadResult.isSuccess && !uploadResult.getOrNull().isNullOrBlank()) {
                    finalMediaUrl = uploadResult.getOrNull()
                } else {
                    val err = uploadResult.exceptionOrNull()?.message ?: "Media upload failed."
                    android.util.Log.e("YarkhoonViewModel", "Post media upload failed: $err")
                    _uiState.update {
                        it.copy(
                            isPublishingPost = false,
                            publishError = err
                        )
                    }
                    return@launch
                }
            }

            val newPost = Post(
                id = postId,
                author = authorUser,
                type = type,
                text = text.trim(),
                mediaUrls = if (!finalMediaUrl.isNullOrBlank()) listOf(finalMediaUrl) else emptyList(),
                tags = tags,
                location = location,
                likesCount = 0,
                commentsCount = 0,
                repostsCount = 0,
                sharesCount = 0,
                isLiked = false,
                isReposted = false,
                isSaved = false,
                comments = emptyList(),
                createdAt = "Just now",
                timestamp = System.currentTimeMillis()
            )

            val firestoreResult = postRepository.createPost(newPost)
            if (firestoreResult.isSuccess) {
                // If this is a reel post created from composer, also publish Reel entry for synchronization
                if (type == PostType.REEL && !finalMediaUrl.isNullOrBlank()) {
                    try {
                        val synchronizedReel = Reel(
                            id = "reel_${postId.removePrefix("post_")}",
                            author = authorUser,
                            videoMediaUrl = finalMediaUrl,
                            thumbnailMediaUrl = finalMediaUrl,
                            caption = text.trim().ifBlank { "Reel by ${authorUser.name}" },
                            audioTrackTitle = "Original Audio • ${authorUser.name}",
                            likesCount = 0,
                            commentsCount = 0,
                            sharesCount = 0,
                            isLiked = false,
                            isSaved = false,
                            timestamp = System.currentTimeMillis()
                        )
                        reelRepository.createReel(synchronizedReel)
                        _uiState.update { state ->
                            state.copy(reels = listOf(synchronizedReel) + state.reels)
                        }
                    } catch (_: Exception) {}
                }

                _uiState.update { state ->
                    state.copy(
                        posts = listOf(newPost) + state.posts,
                        isCreatePostOpen = false,
                        isPublishingPost = false,
                        uploadProgress = 0f,
                        publishError = null,
                        feedbackMessage = "Your post has been published to YARKHOON.COM!"
                    )
                }
            } else {
                val err = firestoreResult.exceptionOrNull()?.message ?: "Failed to save post to Cloud Firestore."
                android.util.Log.e("YarkhoonViewModel", "Firestore post creation failed after upload: $err")
                _uiState.update {
                    it.copy(
                        isPublishingPost = false,
                        publishError = "Firestore post creation failed: $err"
                    )
                }
            }
        }
    }

    fun openCreatePost(
        initialType: PostType = PostType.TEXT,
        initialFeeling: String? = null,
        initialMediaUri: String? = null
    ) {
        _uiState.update {
            it.copy(
                isCreatePostOpen = true,
                publishError = null,
                uploadProgress = 0f,
                createPostInitialType = initialType,
                createPostInitialFeeling = initialFeeling,
                createPostInitialMediaUri = initialMediaUri
            )
        }
    }

    fun closeCreatePost() {
        if (!_uiState.value.isPublishingPost) {
            _uiState.update {
                it.copy(
                    isCreatePostOpen = false,
                    publishError = null,
                    createPostInitialType = PostType.TEXT,
                    createPostInitialFeeling = null,
                    createPostInitialMediaUri = null
                )
            }
        }
    }

    fun openCreateStory() {
        _uiState.update { it.copy(isCreateStoryOpen = true, publishError = null, uploadProgress = 0f) }
    }

    fun closeCreateStory() {
        if (!_uiState.value.isPublishingPost) {
            _uiState.update { it.copy(isCreateStoryOpen = false, publishError = null) }
        }
    }

    fun publishStory(mediaUri: String, caption: String) {
        val currentUid = authRepository.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            _uiState.update { it.copy(publishError = "Must be signed in to post a story.") }
            return
        }

        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(publishError = "Action restricted: Your account is currently suspended.") }
            return
        }

        val currentUser = _uiState.value.currentUser
        val authorUser = if (currentUser.id == currentUid) currentUser else currentUser.copy(id = currentUid)
        val storyId = "story_${System.currentTimeMillis()}"

        _uiState.update {
            it.copy(
                isPublishingPost = true,
                uploadProgress = 0f,
                publishError = null
            )
        }

        viewModelScope.launch {
            val mediaScope = if (currentUser.isPrivate) "protected" else "public"
            val isVideo = mediaUri.endsWith(".mp4", ignoreCase = true)
            val ext = if (isVideo) "mp4" else "jpg"
            val fileName = "story_${System.currentTimeMillis()}.$ext"
            val storagePath = "users/$currentUid/$mediaScope/stories/$storyId/$fileName"

            val uploadResult = storageRepository.uploadMedia(
                uriString = mediaUri,
                destinationPath = storagePath,
                isProtected = currentUser.isPrivate,
                onProgress = { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }
            )

            if (!uploadResult.isSuccess || uploadResult.getOrNull().isNullOrBlank()) {
                val err = uploadResult.exceptionOrNull()?.message ?: "Story media upload failed."
                android.util.Log.e("YarkhoonViewModel", "Story media upload failed: $err")
                _uiState.update {
                    it.copy(
                        isPublishingPost = false,
                        publishError = err
                    )
                }
                return@launch
            }

            val finalUrl = uploadResult.getOrNull()!!

            val newStory = Story(
                id = storyId,
                author = authorUser,
                mediaUrl = finalUrl,
                caption = caption.ifBlank { "Moments from Yarkhoon" },
                createdAt = "Just now",
                isViewed = false,
                timestamp = System.currentTimeMillis()
            )

            val firestoreResult = storyRepository.createStory(newStory)
            if (firestoreResult.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        stories = listOf(newStory) + state.stories,
                        isCreateStoryOpen = false,
                        isPublishingPost = false,
                        uploadProgress = 0f,
                        publishError = null,
                        feedbackMessage = "Story published to your feed!"
                    )
                }
            } else {
                val err = firestoreResult.exceptionOrNull()?.message ?: "Failed to save story to Cloud Firestore."
                android.util.Log.e("YarkhoonViewModel", "Firestore story creation failed after upload: $err")
                _uiState.update {
                    it.copy(
                        isPublishingPost = false,
                        publishError = "Firestore story creation failed: $err"
                    )
                }
            }
        }
    }

    fun openCreateReel() {
        _uiState.update { it.copy(isCreateReelOpen = true, publishError = null, uploadProgress = 0f) }
    }

    fun closeCreateReel() {
        if (!_uiState.value.isPublishingPost) {
            _uiState.update { it.copy(isCreateReelOpen = false, publishError = null) }
        }
    }

    fun publishReel(videoUri: String, caption: String, audioTrack: String) {
        val currentUid = authRepository.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            _uiState.update { it.copy(publishError = "Must be signed in to post a reel.") }
            return
        }

        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(publishError = "Action restricted: Your account is currently suspended.") }
            return
        }

        val currentUser = _uiState.value.currentUser
        val authorUser = if (currentUser.id == currentUid) currentUser else currentUser.copy(id = currentUid)
        val reelId = "reel_${System.currentTimeMillis()}"

        _uiState.update {
            it.copy(
                isPublishingPost = true,
                uploadProgress = 0f,
                publishError = null
            )
        }

        viewModelScope.launch {
            val mediaScope = if (currentUser.isPrivate) "protected" else "public"
            val fileName = "reel_${System.currentTimeMillis()}.mp4"
            val storagePath = "users/$currentUid/$mediaScope/reels/$reelId/$fileName"

            val uploadResult = storageRepository.uploadMedia(
                uriString = videoUri,
                destinationPath = storagePath,
                isProtected = currentUser.isPrivate,
                onProgress = { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }
            )

            if (!uploadResult.isSuccess || uploadResult.getOrNull().isNullOrBlank()) {
                val err = uploadResult.exceptionOrNull()?.message ?: "Reel video upload failed."
                android.util.Log.e("YarkhoonViewModel", "Reel video upload failed: $err")
                _uiState.update {
                    it.copy(
                        isPublishingPost = false,
                        publishError = err
                    )
                }
                return@launch
            }

            val finalUrl = uploadResult.getOrNull()!!

            val newReel = Reel(
                id = reelId,
                author = authorUser,
                videoMediaUrl = finalUrl,
                thumbnailMediaUrl = finalUrl,
                caption = caption.ifBlank { "New Reel by ${authorUser.name}" },
                audioTrackTitle = audioTrack.ifBlank { "Original Audio • ${authorUser.name}" },
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                isLiked = false,
                isSaved = false,
                timestamp = System.currentTimeMillis()
            )

            val firestoreResult = reelRepository.createReel(newReel)
            if (firestoreResult.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        reels = listOf(newReel) + state.reels,
                        isCreateReelOpen = false,
                        isPublishingPost = false,
                        uploadProgress = 0f,
                        publishError = null,
                        feedbackMessage = "Reel video published successfully!"
                    )
                }
            } else {
                val err = firestoreResult.exceptionOrNull()?.message ?: "Failed to save reel to Cloud Firestore."
                android.util.Log.e("YarkhoonViewModel", "Firestore reel creation failed after upload: $err")
                _uiState.update {
                    it.copy(
                        isPublishingPost = false,
                        publishError = "Firestore reel creation failed: $err"
                    )
                }
            }
        }
    }

    fun openStory(story: Story) {
        _uiState.update { state ->
            val updatedStories = state.stories.map {
                if (it.id == story.id) it.copy(isViewed = true) else it
            }
            state.copy(stories = updatedStories, activeStory = story)
        }
    }

    fun closeStory() {
        _uiState.update { it.copy(activeStory = null) }
    }

    // ---------------- Explore & Business Hub & Marketplace ----------------
    fun switchHubSection(section: BusinessHubSection) {
        _uiState.update { it.copy(activeHubSection = section) }
    }

    // ---------------- Stage 10: Search, Explore & Trending ----------------
    private var searchDebounceJob: Job? = null

    fun updateExploreSearch(query: String) {
        _uiState.update { it.copy(exploreSearchQuery = query, searchError = null) }
        searchDebounceJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }

        searchDebounceJob = viewModelScope.launch {
            delay(350) // 350ms sensible debounce to avoid excessive Firebase queries
            performSearch(query)
        }
    }

    fun clearExploreSearch() {
        searchDebounceJob?.cancel()
        _uiState.update {
            it.copy(
                exploreSearchQuery = "",
                searchResults = null,
                isSearching = false,
                searchError = null
            )
        }
    }

    fun setSearchTab(tab: SearchTab) {
        _uiState.update { it.copy(searchActiveTab = tab) }
    }

    fun performSearch(queryToSearch: String? = null) {
        val query = queryToSearch ?: _uiState.value.exploreSearchQuery
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = null, isSearching = false) }
            return
        }

        viewModelScope.launch {
            val currentUserId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            val result = searchRepository.search(query, currentUserId)
            if (result.isSuccess) {
                val results = result.getOrNull() ?: SearchResults()
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false,
                        searchError = null
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to perform search. Please check connection."
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = err
                    )
                }
            }
        }
    }

    fun loadExploreContent() {
        viewModelScope.launch {
            val currentUserId = authRepository.currentUser?.uid ?: return@launch
            _uiState.update { it.copy(isExploreLoading = true, exploreError = null) }
            val result = searchRepository.getExploreContent(currentUserId)
            if (result.isSuccess) {
                val content = result.getOrNull() ?: ExploreContent()
                _uiState.update {
                    it.copy(
                        exploreContent = content,
                        isExploreLoading = false,
                        exploreError = null
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to load Explore content."
                _uiState.update {
                    it.copy(
                        isExploreLoading = false,
                        exploreError = err
                    )
                }
            }
        }
    }

    fun openHashtagDetail(tag: String) {
        val allPosts = _uiState.value.posts
        val detail = searchRepository.getHashtagDetail(tag, allPosts)
        _uiState.update { it.copy(activeHashtagDetail = detail) }
    }

    fun closeHashtagDetail() {
        _uiState.update { it.copy(activeHashtagDetail = null) }
    }

    fun openUserProfile(user: User) {
        if (user.id.isBlank()) return
        val cachedUserPosts = _uiState.value.posts.filter { it.author.id == user.id }
        _uiState.update { it.copy(selectedUserProfile = user, selectedUserPosts = cachedUserPosts) }

        viewModelScope.launch {
            val fullProfileResult = userRepository.getUserProfile(user.id)
            if (fullProfileResult.isFailure) {
                val err = fullProfileResult.exceptionOrNull()?.localizedMessage ?: "Failed to load profile for ${user.name}."
                _uiState.update { it.copy(feedbackMessage = err) }
            }

            val fullProfile = fullProfileResult.getOrNull()
            val userPostsResult = postRepository.getUserPosts(user.id)
            val userPosts = userPostsResult.getOrNull().orEmpty()

            _uiState.update { state ->
                if (state.selectedUserProfile?.id == user.id) {
                    state.copy(
                        selectedUserProfile = fullProfile ?: state.selectedUserProfile,
                        selectedUserPosts = if (userPosts.isNotEmpty()) userPosts else state.selectedUserPosts
                    )
                } else state
            }
        }
    }

    fun closeUserProfile() {
        _uiState.update { it.copy(selectedUserProfile = null, selectedUserPosts = emptyList()) }
    }

    fun openReelFromExplore(reel: Reel) {
        val reelIndex = _uiState.value.reels.indexOfFirst { it.id == reel.id }.let { if (it >= 0) it else 0 }
        _uiState.update {
            it.copy(
                activeReelIndex = reelIndex,
                currentTab = MainNavTab.REELS
            )
        }
    }

    fun updateBusinessSearch(query: String) {
        _uiState.update { it.copy(businessSearchQuery = query) }
    }

    fun selectBusinessCategory(category: BusinessCategory) {
        _uiState.update { it.copy(selectedBusinessCategory = category) }
    }

    // ---------------- Business Hub & Buy & Sell Marketplace (Stage 9) ----------------
    fun loadBusinessListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusinessLoading = true, businessError = null) }
            val result = businessRepository.getBusinessListings()
            if (result.isSuccess) {
                val realList = result.getOrNull().orEmpty()
                if (realList.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            businessProfiles = realList,
                            isBusinessLoading = false,
                            businessError = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isBusinessLoading = false) }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isBusinessLoading = false,
                        businessError = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun loadMarketplaceListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMarketplaceLoading = true, marketplaceError = null) }
            val result = marketplaceRepository.getMarketplaceListings()
            if (result.isSuccess) {
                val realList = result.getOrNull().orEmpty()
                if (realList.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            marketplaceItems = realList,
                            isMarketplaceLoading = false,
                            marketplaceError = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isMarketplaceLoading = false) }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isMarketplaceLoading = false,
                        marketplaceError = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun contactBusiness(business: BusinessProfile) {
        val currentUser = _uiState.value.currentUser
        // If owner is another registered user, provide option to start direct conversation or view contact
        if (business.owner.id.isNotBlank() && business.owner.id != currentUser.id && business.owner.id != "unknown") {
            startConversationWithUser(business.owner)
            openMessages()
            _uiState.update {
                it.copy(feedbackMessage = "Chat started with ${business.businessName}. You can also call: ${business.phone}")
            }
        } else {
            _uiState.update {
                it.copy(feedbackMessage = "Contact ${business.businessName}: ${business.phone}")
            }
        }
    }

    fun updateMarketplaceSearch(query: String) {
        _uiState.update { it.copy(marketplaceSearchQuery = query) }
    }

    fun selectMarketplaceCategory(category: MarketplaceCategory) {
        _uiState.update { it.copy(selectedMarketplaceCategory = category) }
    }

    fun openSellItemDialog(item: MarketplaceItem? = null) {
        _uiState.update { it.copy(isSellItemDialogOpen = true, itemToEdit = item) }
    }

    fun closeSellItemDialog() {
        _uiState.update { it.copy(isSellItemDialogOpen = false, itemToEdit = null) }
    }

    fun saveMarketplaceItem(
        itemId: String?, // null if new, id if edit
        title: String,
        category: MarketplaceCategory,
        price: String,
        description: String,
        phone: String,
        location: String,
        imageUrls: List<String>
    ) {
        val currentUser = _uiState.value.currentUser
        val trimmedTitle = title.trim()
        val trimmedPrice = price.trim()

        if (trimmedTitle.isBlank() || trimmedPrice.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please provide both an item title and price.") }
            return
        }

        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(feedbackMessage = "Action restricted: Your account is currently suspended.") }
            return
        }

        if (_uiState.value.isSavingListing) return
        _uiState.update {
            it.copy(
                isSavingListing = true,
                listingUploadProgress = 0.1f,
                listingUploadError = null
            )
        }

        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()
            val targetId = itemId ?: "item_${System.currentTimeMillis()}"

            // 1. Upload gallery photos to Firebase Storage under users/{uid}/marketplaceListings/{targetId}/
            for ((index, uri) in imageUrls.withIndex()) {
                if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    uploadedUrls.add(uri)
                } else {
                    val destPath = "users/${currentUser.id}/marketplaceListings/$targetId/photo_$index.jpg"
                    val uploadResult = storageRepository.uploadMedia(
                        uriString = uri,
                        destinationPath = destPath,
                        onProgress = { p ->
                            val totalProgress = 0.1f + (p * 0.5f / imageUrls.size.coerceAtLeast(1))
                            _uiState.update { it.copy(listingUploadProgress = totalProgress) }
                        }
                    )
                    if (uploadResult.isSuccess && !uploadResult.getOrNull().isNullOrBlank()) {
                        uploadedUrls.add(uploadResult.getOrNull()!!)
                    } else {
                        // Omit local content URI from remote Firestore persistence
                        android.util.Log.w("YarkhoonViewModel", "Marketplace image upload paused/skipped for $destPath: ${uploadResult.exceptionOrNull()?.message}")
                    }
                }
            }

            // 2. Persist to Cloud Firestore collection "marketplaceListings"
            if (itemId != null) {
                // Editing existing listing
                val existing = _uiState.value.marketplaceItems.find { it.id == itemId }
                val updatedItem = (existing ?: MarketplaceItem(
                    id = itemId,
                    seller = currentUser,
                    title = trimmedTitle,
                    category = category,
                    price = trimmedPrice,
                    description = description.trim(),
                    phone = phone.trim(),
                    location = location.trim().ifBlank { "Yarkhoon Valley" }
                )).copy(
                    title = trimmedTitle,
                    category = category,
                    price = trimmedPrice,
                    description = description.trim(),
                    phone = phone.trim(),
                    location = location.trim().ifBlank { "Yarkhoon Valley" },
                    imageUrls = uploadedUrls,
                    updatedAt = System.currentTimeMillis()
                )

                val updateResult = marketplaceRepository.updateMarketplaceListing(updatedItem)
                if (updateResult.isSuccess) {
                    _uiState.update { state ->
                        val updatedList = state.marketplaceItems.map { if (it.id == itemId) updatedItem else it }
                        state.copy(
                            marketplaceItems = updatedList,
                            isSellItemDialogOpen = false,
                            itemToEdit = null,
                            isSavingListing = false,
                            listingUploadProgress = 1.0f,
                            feedbackMessage = "Listing '$trimmedTitle' updated in Cloud Firestore!"
                        )
                    }
                } else {
                    val err = updateResult.exceptionOrNull()?.message ?: "Failed to update listing."
                    _uiState.update { state ->
                        val updatedList = state.marketplaceItems.map { if (it.id == itemId) updatedItem else it }
                        state.copy(
                            marketplaceItems = updatedList,
                            isSellItemDialogOpen = false,
                            itemToEdit = null,
                            isSavingListing = false,
                            feedbackMessage = "Notice: $err (changes applied locally)"
                        )
                    }
                }
            } else {
                // Creating new listing
                val newItem = MarketplaceItem(
                    id = targetId,
                    seller = currentUser,
                    title = trimmedTitle,
                    category = category,
                    price = trimmedPrice,
                    description = description.trim(),
                    phone = phone.trim(),
                    location = location.trim().ifBlank { "Yarkhoon Valley" },
                    imageUrls = uploadedUrls,
                    status = "active",
                    createdAt = "Just now",
                    timestamp = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val createResult = marketplaceRepository.createMarketplaceListing(newItem)
                if (createResult.isSuccess) {
                    val created = createResult.getOrNull() ?: newItem
                    _uiState.update { state ->
                        state.copy(
                            marketplaceItems = listOf(created) + state.marketplaceItems.filterNot { it.id == created.id },
                            isSellItemDialogOpen = false,
                            itemToEdit = null,
                            isSavingListing = false,
                            listingUploadProgress = 1.0f,
                            feedbackMessage = "Item '$trimmedTitle' is live on Yarkhoon Buy & Sell!"
                        )
                    }
                } else {
                    val err = createResult.exceptionOrNull()?.message ?: "Failed to publish listing."
                    _uiState.update { state ->
                        state.copy(
                            marketplaceItems = listOf(newItem) + state.marketplaceItems,
                            isSellItemDialogOpen = false,
                            itemToEdit = null,
                            isSavingListing = false,
                            feedbackMessage = "Item saved locally. Notice: $err"
                        )
                    }
                }
            }
        }
    }

    fun markMarketplaceItemSold(itemId: String) {
        val currentUserId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val result = marketplaceRepository.markListingSold(itemId, currentUserId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = state.marketplaceItems.map {
                        if (it.id == itemId) it.copy(status = "sold") else it
                    }
                    state.copy(marketplaceItems = updated, feedbackMessage = "Listing marked as SOLD!")
                }
            } else {
                _uiState.update { state ->
                    val updated = state.marketplaceItems.map {
                        if (it.id == itemId) it.copy(status = "sold") else it
                    }
                    state.copy(marketplaceItems = updated, feedbackMessage = "Listing marked as SOLD locally.")
                }
            }
        }
    }

    fun deleteMarketplaceItem(itemId: String) {
        val currentUserId = authRepository.currentUser?.uid ?: return
        val itemToDelete = _uiState.value.marketplaceItems.find { it.id == itemId }
        if (itemToDelete == null) return

        if (itemToDelete.seller.id != currentUserId) {
            _uiState.update {
                it.copy(feedbackMessage = "Only the seller can delete this listing.")
            }
            return
        }

        viewModelScope.launch {
            val result = marketplaceRepository.deleteMarketplaceListing(itemId)
            _uiState.update { state ->
                val remaining = state.marketplaceItems.filterNot { it.id == itemId }
                state.copy(
                    marketplaceItems = remaining,
                    feedbackMessage = "Listing '${itemToDelete.title}' has been deleted."
                )
            }
        }
    }

    fun contactSeller(item: MarketplaceItem) {
        val currentUser = _uiState.value.currentUser
        // If seller is a registered user other than current user, open direct 1-to-1 conversation
        if (item.seller.id.isNotBlank() && item.seller.id != currentUser.id && item.seller.id != "unknown") {
            startConversationWithUser(item.seller)
            openMessages()
            _uiState.update {
                it.copy(feedbackMessage = "Chat started with seller ${item.seller.name}. You can also call: ${item.phone}")
            }
        } else {
            _uiState.update {
                it.copy(feedbackMessage = "Contacting ${item.seller.name} for '${item.title}' at ${item.phone}")
            }
        }
    }

    // ---------------- Messages & Notifications (Stage 8) ----------------
    fun openMessages() {
        _uiState.update { it.copy(isMessagesOpen = true, messagingError = null) }
        attachConversationsListener()
    }

    fun closeMessages() {
        _uiState.update {
            it.copy(
                isMessagesOpen = false,
                activeConversation = null,
                activeMessages = emptyList(),
                isUserDiscoveryOpen = false,
                messagingError = null
            )
        }
        detachMessagesListener()
    }

    private var fcmTokenSyncAttempted = false

    private fun syncFcmToken() {
        if (fcmTokenSyncAttempted) return
        fcmTokenSyncAttempted = true

        val currentUid = authRepository.currentUser?.uid ?: return

        val appContext = try {
            com.google.firebase.FirebaseApp.getInstance().applicationContext
        } catch (_: Exception) {
            null
        }

        val prefs = appContext?.getSharedPreferences("yarkhoon_fcm_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs?.getBoolean("fcm_registration_disabled", false) == true) {
            android.util.Log.d("YarkhoonViewModel", "FCM token sync skipped: device registration limit was previously reached.")
            return
        }

        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    if (!token.isNullOrBlank()) {
                        viewModelScope.launch {
                            userRepository.updateFcmToken(currentUid, token)
                        }
                    }
                } else {
                    val ex = task.exception
                    val errString = "${ex?.message} ${ex?.cause?.message}"
                    android.util.Log.w("YarkhoonViewModel", "FCM token retrieval notice: ${ex?.message}")

                    if (errString.contains("TOO_MANY_REGISTRATIONS", ignoreCase = true)) {
                        // Remember that this device/emulator reached its registration capacity
                        // so we never retry or trigger background hard-failure exceptions
                        prefs?.edit()?.putBoolean("fcm_registration_disabled", true)?.apply()
                        android.util.Log.i("YarkhoonViewModel", "Device reached FCM registration limit; suppressing further token requests gracefully.")
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("YarkhoonViewModel", "FCM service unavailable: ${e.message}")
        }
    }

    fun attachConversationsListener() {
        val currentUserId = authRepository.currentUser?.uid ?: return

        conversationsListener?.remove()
        conversationsListener = messagingRepository.listenConversations(
            currentUserId = currentUserId,
            onUpdate = { convs ->
                _uiState.update { state ->
                    state.copy(
                        conversations = convs,
                        messagingError = null
                    )
                }
            },
            onError = { ex ->
                _uiState.update { state ->
                    state.copy(messagingError = ex.message)
                }
            }
        )
    }

    fun selectConversation(conversation: Conversation) {
        _uiState.update {
            it.copy(
                activeConversation = conversation,
                activeMessages = emptyList(),
                isMessagesLoading = true,
                messagingError = null,
                isUserDiscoveryOpen = false
            )
        }
        attachMessagesListener(conversation.id)

        // Mark as read in background
        val currentUserId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            messagingRepository.markConversationAsRead(conversation.id, currentUserId)
        }
    }

    fun closeActiveConversation() {
        detachMessagesListener()
        _uiState.update {
            it.copy(
                activeConversation = null,
                activeMessages = emptyList(),
                messagingError = null
            )
        }
    }

    private fun attachMessagesListener(conversationId: String) {
        detachMessagesListener()
        val limit = _uiState.value.messagesLimit
        messagesListener = messagingRepository.listenMessages(
            conversationId = conversationId,
            limit = limit,
            onUpdate = { msgs ->
                _uiState.update { state ->
                    state.copy(
                        activeMessages = msgs,
                        isMessagesLoading = false,
                        messagingError = null
                    )
                }
            },
            onError = { ex ->
                _uiState.update { state ->
                    state.copy(
                        isMessagesLoading = false,
                        messagingError = ex.message
                    )
                }
            }
        )
    }

    private fun detachMessagesListener() {
        messagesListener?.remove()
        messagesListener = null
    }

    fun sendDirectMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        if (_uiState.value.isUserSuspended) {
            _uiState.update { it.copy(messagingError = "Action restricted: Your account is currently suspended.") }
            return
        }

        val activeConv = _uiState.value.activeConversation ?: return
        val currentUser = _uiState.value.currentUser

        if (_uiState.value.blockedUserIds.contains(activeConv.recipient.id)) {
            _uiState.update { it.copy(messagingError = "Cannot send messages to a blocked account. Unblock them first.") }
            return
        }

        if (_uiState.value.isSendingMessage) return

        _uiState.update { it.copy(isSendingMessage = true, messagingError = null) }

        viewModelScope.launch {
            val result = messagingRepository.sendMessage(
                conversationId = activeConv.id,
                senderUser = currentUser,
                recipientUser = activeConv.recipient,
                text = trimmed
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isSendingMessage = false) }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to send message."
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        messagingError = err
                    )
                }
            }
        }
    }

    fun openUserDiscovery() {
        _uiState.update {
            it.copy(
                isUserDiscoveryOpen = true,
                userSearchQuery = "",
                discoveredUsers = emptyList()
            )
        }
        searchUsers("")
    }

    fun closeUserDiscovery() {
        _uiState.update { it.copy(isUserDiscoveryOpen = false) }
    }

    fun onUserSearchQueryChanged(query: String) {
        _uiState.update { it.copy(userSearchQuery = query) }
        searchUsers(query)
    }

    fun searchUsers(query: String) {
        val currentUserId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUsers = true) }
            val result = userRepository.searchRegisteredUsers(currentUserId, query)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        discoveredUsers = result.getOrNull().orEmpty(),
                        isSearchingUsers = false
                    )
                }
            } else {
                _uiState.update { it.copy(isSearchingUsers = false) }
            }
        }
    }

    fun startConversationWithUser(targetUser: User) {
        val currentUserId = authRepository.currentUser?.uid ?: return
        if (targetUser.id == currentUserId) return

        val currentUser = _uiState.value.currentUser
        val deterministicId = FirestoreMessagingRepository.generateConversationId(currentUserId, targetUser.id)

        // Find existing conversation in list if present (check deterministic ID, legacy ID, or recipient match)
        val existing = _uiState.value.conversations.find {
            it.id == deterministicId ||
            (it.participantIds.contains(currentUserId) && it.participantIds.contains(targetUser.id)) ||
            (it.recipient.id == targetUser.id)
        }
        val conversationId = existing?.id ?: deterministicId
        val targetConv = existing ?: Conversation(
            id = conversationId,
            participantIds = listOf(currentUserId, targetUser.id).sorted(),
            recipient = targetUser,
            lastMessage = "Start a conversation",
            timestamp = "Just now",
            unreadCount = 0
        )

        _uiState.update {
            it.copy(
                isUserDiscoveryOpen = false,
                activeConversation = targetConv,
                activeMessages = emptyList(),
                isMessagesLoading = true,
                messagingError = null
            )
        }

        viewModelScope.launch {
            messagingRepository.ensureConversationCreated(conversationId, currentUser, targetUser)
            attachMessagesListener(conversationId)
        }
    }

    fun openConversationById(conversationId: String) {
        if (conversationId.isBlank()) return
        val conv = _uiState.value.conversations.find { it.id == conversationId }
        if (conv != null) {
            selectConversation(conv)
            openMessages()
        }
    }

    fun loadMoreMessages() {
        val newLimit = _uiState.value.messagesLimit + 30L
        _uiState.update { it.copy(messagesLimit = newLimit) }
        _uiState.value.activeConversation?.let {
            attachMessagesListener(it.id)
        }
    }

    fun openNotifications() {
        val currentUid = authRepository.currentUser?.uid ?: return
        if (notificationsListener == null) {
            attachNotificationsListener(currentUid)
        }
        _uiState.update { it.copy(isNotificationsOpen = true) }
    }

    fun closeNotifications() {
        _uiState.update { it.copy(isNotificationsOpen = false) }
    }

    fun attachNotificationsListener(userId: String? = null) {
        val targetUid = userId ?: authRepository.currentUser?.uid ?: return

        notificationsListener?.remove()
        _uiState.update { it.copy(isNotificationsLoading = true) }
        notificationsListener = notificationRepository.listenNotifications(
            recipientId = targetUid,
            onUpdate = { notifs ->
                _uiState.update {
                    it.copy(
                        notifications = notifs,
                        isNotificationsLoading = false,
                        notificationError = null
                    )
                }
            },
            onError = { e ->
                _uiState.update {
                    it.copy(
                        isNotificationsLoading = false,
                        notificationError = e.localizedMessage
                    )
                }
            }
        )
    }

    fun markAllNotificationsRead() {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            val readList = state.notifications.map { it.copy(isRead = true) }
            state.copy(notifications = readList)
        }
        viewModelScope.launch {
            notificationRepository.markAllAsRead(currentUid)
        }
    }

    fun markNotificationRead(notificationId: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            val updated = state.notifications.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
            state.copy(notifications = updated)
        }
        viewModelScope.launch {
            notificationRepository.markAsRead(currentUid, notificationId)
        }
    }

    fun deleteNotification(notificationId: String) {
        val currentUid = authRepository.currentUser?.uid ?: return
        _uiState.update { state ->
            state.copy(notifications = state.notifications.filter { it.id != notificationId })
        }
        viewModelScope.launch {
            notificationRepository.deleteNotification(currentUid, notificationId)
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    // ---------------- Stage 13: Content Reporting & Admin Dashboard ----------------

    fun openReportDialog(
        contentType: ReportContentType,
        contentId: String,
        contentSummary: String = "",
        reportedUserId: String? = null,
        reportedUserName: String? = null
    ) {
        val reporterUid = authRepository.currentUser?.uid ?: _uiState.value.currentUser.id
        val target = ContentReport(
            id = "pending_rep_${System.currentTimeMillis()}",
            reporterId = reporterUid,
            reportedUserId = reportedUserId,
            reportedUserName = reportedUserName,
            contentType = contentType,
            contentId = contentId,
            contentSummary = contentSummary,
            reason = ReportReason.SPAM
        )
        _uiState.update {
            it.copy(
                isReportDialogOpen = true,
                pendingReportTarget = target
            )
        }
    }

    fun closeReportDialog() {
        _uiState.update {
            it.copy(
                isReportDialogOpen = false,
                pendingReportTarget = null
            )
        }
    }

    fun submitReport(reason: ReportReason, explanation: String) {
        val target = _uiState.value.pendingReportTarget ?: return

        viewModelScope.launch {
            val result = adminRepository.submitReport(
                contentType = target.contentType,
                contentId = target.contentId,
                reason = reason,
                description = explanation,
                reportedUserId = target.reportedUserId,
                reportedUserName = target.reportedUserName,
                contentSummary = target.contentSummary
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isReportDialogOpen = false,
                        pendingReportTarget = null,
                        feedbackMessage = "Report submitted. Thank you for keeping YARKHOON.COM safe!"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit report."
                _uiState.update {
                    it.copy(
                        isReportDialogOpen = false,
                        pendingReportTarget = null,
                        feedbackMessage = "Report error: $err"
                    )
                }
            }
        }
    }

    private fun returnToAdminLoginOnMissingAuth(reason: String) {
        Log.w("YarkhoonAuthDebug", "[returnToAdminLoginOnMissingAuth] Redirecting to Admin Login: $reason")
        adminInactivityWatcherJob?.cancel()
        adminInactivityWatcherJob = null
        AdminSessionManager.invalidateAndClear()
        FirestoreAdminRepository.invalidateAdminSession()
        _uiState.update {
            it.copy(
                isAdminLoading = false,
                isAuthenticated = false,
                isAdmin = false,
                adminRole = AdminRole.NONE,
                isAdminDashboardOpen = false,
                adminSessionStartedAt = 0L,
                authMode = AuthMode.ADMIN_LOGIN,
                authError = reason
            )
        }
    }

    fun openAdminDashboard(initialSection: AdminControlPanelSection = AdminControlPanelSection.DASHBOARD) {
        if (authRepository.currentUser == null) {
            returnToAdminLoginOnMissingAuth("Authentication required: Please sign in to the Admin Portal.")
            return
        }
        if (!_uiState.value.isAdmin || !_uiState.value.adminRole.canAccessAdminPanel) {
            _uiState.update { it.copy(feedbackMessage = "Access restricted: Authorized administrators only.") }
            return
        }
        val sessionStart = _uiState.value.adminSessionStartedAt
        if (sessionStart > 0L && !FirestoreAdminRepository.isAdminSessionValid(sessionStart)) {
            logoutFromAdminPanel(expired = true)
            return
        }
        _uiState.update {
            it.copy(
                isAdminDashboardOpen = true,
                activeAdminSection = initialSection,
                adminSessionStartedAt = if (sessionStart == 0L) System.currentTimeMillis() else sessionStart
            )
        }
        loadFullAdminControlPanelData()
    }

    fun closeAdminDashboard() {
        _uiState.update { it.copy(isAdminDashboardOpen = false) }
    }

    fun selectAdminSection(section: AdminControlPanelSection) {
        if (authRepository.currentUser == null) {
            returnToAdminLoginOnMissingAuth("Authentication required: Please sign in to the Admin Portal.")
            return
        }
        if (!_uiState.value.isAdmin || !_uiState.value.adminRole.canAccessAdminPanel) {
            _uiState.update { it.copy(feedbackMessage = "Unauthorized: Admin session required.") }
            return
        }
        val sessionStart = _uiState.value.adminSessionStartedAt
        if (sessionStart > 0L && !FirestoreAdminRepository.isAdminSessionValid(sessionStart)) {
            logoutFromAdminPanel(expired = true)
            return
        }
        _uiState.update { it.copy(activeAdminSection = section, adminError = null) }
    }

    fun logoutFromAdminPanel(expired: Boolean = false) {
        logout()
        if (expired) {
            _uiState.update {
                it.copy(
                    authMode = AuthMode.ADMIN_LOGIN,
                    authError = "Your administrative session has expired for security. Please sign in again."
                )
            }
        }
    }

    private var adminInactivityWatcherJob: Job? = null

    private fun startAdminInactivityWatcher() {
        adminInactivityWatcherJob?.cancel()
        adminInactivityWatcherJob = viewModelScope.launch {
            while (true) {
                delay(30_000L) // Check every 30 seconds
                if (_uiState.value.isAdmin) {
                    val now = System.currentTimeMillis()
                    if (AdminSessionManager.isSessionExpired(now)) {
                        handleAdminSessionExpired()
                        break
                    }
                } else {
                    break
                }
            }
        }
    }

    fun handleAdminSessionExpired() {
        val adminUid = _uiState.value.currentUser.id.takeIf { _uiState.value.isAdmin }
        adminInactivityWatcherJob?.cancel()
        adminInactivityWatcherJob = null
        AdminSessionManager.invalidateAndClear()
        FirestoreAdminRepository.invalidateAdminSession()
        if (adminUid != null) {
            viewModelScope.launch {
                try {
                    adminRepository.terminateServerAdminSession(adminUid)
                } catch (_: Exception) {}
            }
        }
        authRepository.signOut()
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                isAdmin = false,
                adminRole = AdminRole.NONE,
                isAdminDashboardOpen = false,
                adminSessionStartedAt = 0L,
                authMode = AuthMode.ADMIN_LOGIN,
                authError = "Admin session expired due to 4 hours of inactivity. Please log in again."
            )
        }
    }

    fun onAppResumed() {
        checkAdminSessionValidity()
    }

    fun checkAdminSessionValidity(nowMs: Long = System.currentTimeMillis()) {
        if (_uiState.value.isAdmin) {
            if (AdminSessionManager.isSessionExpired(nowMs)) {
                handleAdminSessionExpired()
            }
        }
    }

    fun recordAdminActivity(nowMs: Long = System.currentTimeMillis()) {
        if (_uiState.value.isAdmin) {
            AdminSessionManager.recordActivity(nowMs)
        }
    }

    private fun ensureActiveAdminSessionForAction(): Boolean {
        if (authRepository.currentUser == null) {
            returnToAdminLoginOnMissingAuth("Authentication required: Firebase session is missing. Please sign in to the Admin Portal.")
            return false
        }
        if (!_uiState.value.isAdmin || !_uiState.value.adminRole.canAccessAdminPanel) {
            _uiState.update { it.copy(feedbackMessage = "Unauthorized: Administrative privileges required.") }
            return false
        }
        val sessionStart = _uiState.value.adminSessionStartedAt
        if (sessionStart <= 0L || !FirestoreAdminRepository.isAdminSessionValid(sessionStart) || AdminSessionManager.isSessionExpired()) {
            logoutFromAdminPanel(expired = true)
            return false
        }
        recordAdminActivity()
        return true
    }

    fun loadAdminReportsAndAuditLogs() {
        loadFullAdminControlPanelData()
    }

    fun loadFullAdminControlPanelData() {
        if (!ensureActiveAdminSessionForAction()) return
        _uiState.update { it.copy(isAdminLoading = true, adminError = null) }

        viewModelScope.launch {
            val preflightUser = authRepository.currentUser
            if (preflightUser == null) {
                returnToAdminLoginOnMissingAuth("Authentication required: Firebase currentUser is null. Please sign in to the Admin Portal.")
                return@launch
            }

            // Authoritative session preflight: verifies session and caches verified role for the batch load
            val sessionCheck = adminRepository.requireAuthorizedAdminSession("Admin Dashboard sync") { it.canAccessAdminPanel }
            val currentAuthUser = authRepository.currentUser
            if (currentAuthUser == null) {
                returnToAdminLoginOnMissingAuth("Authentication session lost during dashboard authorization. Please sign in again.")
                return@launch
            }

            val diagReport = buildString {
                appendLine("• Connected Project: yarkhoondotcom (#10059570758)")
                appendLine("• Package: com.aistudio.yarkhoon.scbqdm (versionCode: 4, v1.0.3)")
                appendLine("• Auth UID: ${currentAuthUser.uid}")
                appendLine("• Auth Email: ${currentAuthUser.email ?: "None"}")
                appendLine("• Target Owner UID: ${FirestoreAdminRepository.OWNER_BOOTSTRAP_UID}")
                appendLine("• Target Owner Email: ${FirestoreAdminRepository.OWNER_BOOTSTRAP_EMAIL}")
                appendLine("• UID Match: ${currentAuthUser.uid == FirestoreAdminRepository.OWNER_BOOTSTRAP_UID}")
                appendLine("• Email Match: ${currentAuthUser.email?.equals(FirestoreAdminRepository.OWNER_BOOTSTRAP_EMAIL, ignoreCase = true)}")
                appendLine("• adminRoles on Server: ${FirestoreAdminRepository.lastAdminDocExistsOnServer ?: "Pending read"}")
                appendLine("• Resolution Trace: ${FirestoreAdminRepository.lastRoleResolutionTrace}")
                if (FirestoreAdminRepository.lastBootstrapError != null) {
                    appendLine("• Last Bootstrap Error: ${FirestoreAdminRepository.lastBootstrapError}")
                }
            }

            if (sessionCheck.isFailure) {
                val exception = sessionCheck.exceptionOrNull()
                if (exception is IllegalAccessException) {
                    // Genuine authorization failure or revoked role -> close dashboard and return to Admin Login screen instead of displaying Ordinary User
                    returnToAdminLoginOnMissingAuth(exception.message ?: "Unauthorized: Administrative access required.")
                } else {
                    // Network or connection error: do NOT downgrade role or log out the administrator
                    _uiState.update {
                        it.copy(
                            isAdminLoading = false,
                            adminError = "Network error communicating with database: ${exception?.localizedMessage ?: exception?.message}",
                            adminDiagnosticInfo = diagReport
                        )
                    }
                }
                return@launch
            }

            val authoritativeRole = sessionCheck.getOrNull() ?: AdminRole.NONE
            if (!authoritativeRole.canAccessAdminPanel) {
                returnToAdminLoginOnMissingAuth("Unauthorized: Administrative access required.")
                return@launch
            }
            // Ensure UI state strictly mirrors the authoritative backend role
            _uiState.update {
                it.copy(
                    adminRole = authoritativeRole,
                    adminDiagnosticInfo = diagReport
                )
            }

            val statsResult = adminRepository.getDashboardStats()
            val usersResult = adminRepository.getAllUsersForAdmin()
            val postsResult = adminRepository.getAllPostsForAdmin(postRepository)
            val verificationsResult = adminRepository.getVerificationRequests()
            val villagesResult = adminRepository.getVillages()
            val announcementsResult = adminRepository.getOfficialAnnouncements()
            val trendingTopicsResult = adminRepository.getCuratedTrendingTopics()
            val adminAccountsResult = adminRepository.getAdminAccounts()
            val reportsResult = adminRepository.getReports()
            val auditLogsResult = adminRepository.getAuditLogs(limit = 80)

            val loadedUsers = usersResult.getOrNull().orEmpty()
            val loadedPosts = postsResult.getOrNull().let { if (it.isNullOrEmpty()) _uiState.value.posts else it }
            val loadedVerifications = verificationsResult.getOrNull().orEmpty()
            val loadedVillages = villagesResult.getOrNull().orEmpty()
            val loadedAnnouncements = announcementsResult.getOrNull().orEmpty()
            val loadedTrending = trendingTopicsResult.getOrNull().orEmpty()
            val loadedAdminAccounts = adminAccountsResult.getOrNull().orEmpty()
            val loadedReports = reportsResult.getOrNull().orEmpty()
            val loadedLogs = auditLogsResult.getOrNull().orEmpty()

            val computedStats = statsResult.getOrNull() ?: AdminDashboardStats(
                totalRegisteredUsers = loadedUsers.size,
                activeUsers = loadedUsers.count { !it.isSuspended },
                suspendedUsers = loadedUsers.count { it.isSuspended },
                totalPosts = loadedPosts.size,
                pendingPosts = loadedPosts.count { it.moderationStatus.equals("PENDING", ignoreCase = true) },
                approvedPosts = loadedPosts.count { !it.isModeratedHidden && it.moderationStatus.equals("APPROVED", ignoreCase = true) },
                rejectedPosts = loadedPosts.count { it.isModeratedHidden || it.moderationStatus.equals("REJECTED", ignoreCase = true) },
                totalVerifiedAccounts = loadedUsers.count { it.isVerified },
                totalFeaturedPosts = loadedPosts.count { it.isCurrentlyFeatured },
                totalPromotedPosts = loadedPosts.count { it.isCurrentlyPromoted },
                trendingPostsCount = loadedPosts.count { it.isTrendingLabeled },
                pendingVerificationRequests = loadedVerifications.count { it.status == VerificationRequestStatus.PENDING },
                reportedContentCount = loadedReports.count { it.status == ReportStatus.PENDING || it.status == ReportStatus.UNDER_REVIEW },
                totalVillagesCount = loadedVillages.size
            )

            val errorMessage = when {
                reportsResult.isFailure && reportsResult.exceptionOrNull() is IllegalAccessException ->
                    reportsResult.exceptionOrNull()?.localizedMessage
                reportsResult.isFailure ->
                    "Unable to load reports: ${reportsResult.exceptionOrNull()?.localizedMessage}"
                adminAccountsResult.isFailure && adminAccountsResult.exceptionOrNull() is IllegalAccessException ->
                    adminAccountsResult.exceptionOrNull()?.localizedMessage
                statsResult.isFailure && statsResult.exceptionOrNull() is IllegalAccessException ->
                    statsResult.exceptionOrNull()?.localizedMessage
                else -> null
            }

            _uiState.update {
                it.copy(
                    adminStats = computedStats,
                    adminUsersList = loadedUsers,
                    adminPostsList = loadedPosts,
                    verificationRequests = loadedVerifications,
                    villagesList = loadedVillages,
                    officialAnnouncements = loadedAnnouncements,
                    curatedTrendingTopics = loadedTrending,
                    adminAccountsList = loadedAdminAccounts,
                    reports = loadedReports,
                    auditLogs = loadedLogs,
                    isAdminLoading = false,
                    adminError = errorMessage
                )
            }
        }
    }

    fun approvePostFromAdmin(postId: String, reason: String = "Approved by administrator") {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.setPostModerationStatus(postId, "APPROVED", reason)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedAdminPosts = state.adminPostsList.map {
                        if (it.id == postId) it.copy(moderationStatus = "APPROVED", isModeratedHidden = false) else it
                    }
                    state.copy(
                        adminPostsList = updatedAdminPosts,
                        feedbackMessage = "Post approved and visible in feed."
                    )
                }
                loadFeedAndSocialContent()
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun rejectPostFromAdmin(postId: String, reason: String = "Rejected by administrator") {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.setPostModerationStatus(postId, "REJECTED", reason)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedAdminPosts = state.adminPostsList.map {
                        if (it.id == postId) it.copy(moderationStatus = "REJECTED", isModeratedHidden = true) else it
                    }
                    val updatedFeedPosts = state.posts.filter { it.id != postId }
                    state.copy(
                        adminPostsList = updatedAdminPosts,
                        posts = updatedFeedPosts,
                        feedbackMessage = "Post rejected and removed from public feed."
                    )
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun togglePinPostFromAdmin(postId: String, isPinned: Boolean) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.setPostPinned(postId, isPinned)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedPosts = state.posts.map {
                        if (it.id == postId) it.copy(isPinned = isPinned) else it
                    }.sortedWith(compareByDescending<Post> { it.isPinned }.thenByDescending { it.timestamp })
                    val updatedAdminPosts = state.adminPostsList.map {
                        if (it.id == postId) it.copy(isPinned = isPinned) else it
                    }
                    state.copy(
                        posts = updatedPosts,
                        adminPostsList = updatedAdminPosts,
                        feedbackMessage = if (isPinned) "Post pinned to top of feed." else "Post unpinned."
                    )
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun featurePostFromAdmin(postId: String, isFeatured: Boolean, durationDays: Int = 7, scheduledStartDelayHours: Int = 0) {
        if (!ensureActiveAdminSessionForAction()) return
        val now = System.currentTimeMillis()
        val scheduledAt = if (isFeatured && scheduledStartDelayHours > 0) now + (scheduledStartDelayHours * 3600_000L) else now
        val featuredUntil = if (isFeatured && durationDays > 0) scheduledAt + (durationDays * 24L * 3600_000L) else 0L
        viewModelScope.launch {
            val result = adminRepository.setPostFeatured(
                postId = postId,
                isFeatured = isFeatured,
                scheduledAt = scheduledAt,
                featuredUntil = featuredUntil,
                reason = if (isFeatured) "Featured for $durationDays days" else "Removed from Featured"
            )
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedPosts = state.posts.map {
                        if (it.id == postId) it.copy(isFeatured = isFeatured, featuredScheduledAt = scheduledAt, featuredUntil = featuredUntil) else it
                    }
                    val updatedAdminPosts = state.adminPostsList.map {
                        if (it.id == postId) it.copy(isFeatured = isFeatured, featuredScheduledAt = scheduledAt, featuredUntil = featuredUntil) else it
                    }
                    state.copy(
                        posts = updatedPosts,
                        adminPostsList = updatedAdminPosts,
                        feedbackMessage = if (isFeatured) "Post marked as Featured ($durationDays days)." else "Post removed from Featured."
                    )
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun toggleTrendingPostFromAdmin(postId: String, isTrending: Boolean) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.setPostTrendingLabel(postId, isTrending)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedPosts = state.posts.map {
                        if (it.id == postId) it.copy(isTrendingLabeled = isTrending) else it
                    }
                    val updatedAdminPosts = state.adminPostsList.map {
                        if (it.id == postId) it.copy(isTrendingLabeled = isTrending) else it
                    }
                    state.copy(
                        posts = updatedPosts,
                        adminPostsList = updatedAdminPosts,
                        feedbackMessage = if (isTrending) "Trending label added to post." else "Trending label removed."
                    )
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun promotePostFromAdmin(
        postId: String,
        isPromoted: Boolean,
        durationDays: Int = 7,
        targetCategory: String = "Community",
        targetLocation: String = "All Yarkhoon & Chitral"
    ) {
        if (!ensureActiveAdminSessionForAction()) return
        val now = System.currentTimeMillis()
        val endDate = if (isPromoted && durationDays > 0) now + (durationDays * 24L * 3600_000L) else 0L
        viewModelScope.launch {
            val result = adminRepository.setPostPromoted(
                postId = postId,
                isPromoted = isPromoted,
                startDate = now,
                endDate = endDate,
                targetCategory = targetCategory,
                targetLocation = targetLocation
            )
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedPosts = state.posts.map {
                        if (it.id == postId) it.copy(
                            isPromoted = isPromoted,
                            promotionStartDate = if (isPromoted) now else 0L,
                            promotionEndDate = endDate,
                            promotionTargetCategory = if (isPromoted) targetCategory else "",
                            promotionTargetLocation = if (isPromoted) targetLocation else ""
                        ) else it
                    }
                    val updatedAdminPosts = state.adminPostsList.map {
                        if (it.id == postId) it.copy(
                            isPromoted = isPromoted,
                            promotionStartDate = if (isPromoted) now else 0L,
                            promotionEndDate = endDate,
                            promotionTargetCategory = if (isPromoted) targetCategory else "",
                            promotionTargetLocation = if (isPromoted) targetLocation else ""
                        ) else it
                    }
                    state.copy(
                        posts = updatedPosts,
                        adminPostsList = updatedAdminPosts,
                        feedbackMessage = if (isPromoted) "Post promoted ($targetLocation • $durationDays days)." else "Post promotion ended."
                    )
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun setUserVerificationFromAdmin(userId: String, isVerified: Boolean, reason: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.setUserVerification(userId, isVerified, reason)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updatedUsers = state.adminUsersList.map {
                        if (it.id == userId) it.copy(isVerified = isVerified) else it
                    }
                    val updatedPosts = state.posts.map {
                        if (it.author.id == userId) it.copy(author = it.author.copy(isVerified = isVerified)) else it
                    }
                    val updatedCurrent = if (state.currentUser.id == userId) state.currentUser.copy(isVerified = isVerified) else state.currentUser
                    state.copy(
                        adminUsersList = updatedUsers,
                        posts = updatedPosts,
                        currentUser = updatedCurrent,
                        feedbackMessage = if (isVerified) "Blue Tick verification badge granted!" else "Blue Tick verification badge removed."
                    )
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun requestBlueTickVerification(category: String, supportingInfo: String) {
        val current = _uiState.value.currentUser
        if (supportingInfo.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please provide supporting details for your Blue Tick verification request.") }
            return
        }
        viewModelScope.launch {
            val result = adminRepository.submitVerificationRequest(
                category = category,
                supportingInfo = supportingInfo,
                userName = current.name,
                userHandle = current.handle,
                userAvatarUrl = current.avatarUrl
            )
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(feedbackMessage = "Blue Tick verification request submitted for administrator review!")
                }
            } else {
                _uiState.update {
                    it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}")
                }
            }
        }
    }

    fun reviewVerificationRequestFromAdmin(requestId: String, userId: String, approve: Boolean, notes: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.reviewVerificationRequest(requestId, userId, approve, notes)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        feedbackMessage = if (approve) "Verification request approved and Blue Tick granted!" else "Verification request rejected."
                    )
                }
                loadFullAdminControlPanelData()
                loadFeedAndSocialContent()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun saveTrendingTopicFromAdmin(
        id: String?,
        tag: String,
        headline: String,
        category: String,
        villageName: String,
        isPinned: Boolean
    ) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.saveCuratedTrendingTopic(id, tag, headline, category, villageName, isPinned)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Trending topic saved.") }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun deleteTrendingTopicFromAdmin(topicId: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.deleteCuratedTrendingTopic(topicId)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Trending topic removed.") }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun saveVillageFromAdmin(
        villageId: String?,
        name: String,
        tehsil: String,
        district: String,
        description: String,
        populationEstimate: String
    ) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.saveVillage(villageId, name, tehsil, district, description, populationEstimate)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Village '${name.trim()}' saved successfully.") }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun deleteVillageFromAdmin(villageId: String, villageName: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.deleteVillage(villageId, villageName)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Village '$villageName' deleted.") }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun publishOfficialAnnouncementFromAdmin(
        title: String,
        body: String,
        category: String,
        targetLocation: String,
        priority: String
    ) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.publishOfficialAnnouncement(title, body, category, targetLocation, priority)
            if (result.isSuccess) {
                val created = result.getOrNull()
                if (created != null) {
                    val notifItem = NotificationItem(
                        id = created.id,
                        sender = _uiState.value.currentUser.copy(
                            name = "Yarkhoon Official (${created.category.replace("_", " ")})",
                            isVerified = true
                        ),
                        type = "official_announcement",
                        message = "${created.title}: ${created.body}",
                        timestamp = "Just now",
                        isRead = false
                    )
                    _uiState.update { state ->
                        state.copy(
                            notifications = listOf(notifItem) + state.notifications,
                            feedbackMessage = "Official announcement broadcasted to ${created.targetVillageOrLocation}!"
                        )
                    }
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun assignAdminRoleFromAdmin(
        targetUid: String,
        targetEmail: String,
        targetName: String,
        role: AdminRole,
        permissions: List<String> = emptyList()
    ) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.assignAdminRole(targetUid, targetEmail, targetName, role, permissions)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(feedbackMessage = "Assigned ${role.displayName} role to $targetEmail.")
                }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun revokeAdminRoleFromAdmin(targetUid: String, reason: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.revokeAdminRole(targetUid, reason)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Administrative privileges revoked.") }
                loadFullAdminControlPanelData()
            } else {
                _uiState.update { it.copy(feedbackMessage = "Error: ${result.exceptionOrNull()?.localizedMessage}") }
            }
        }
    }

    fun dismissReport(reportId: String, reason: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.dismissReport(reportId, reason)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = state.reports.map {
                        if (it.id == reportId) it.copy(status = ReportStatus.DISMISSED, resolutionNotes = reason)
                        else it
                    }
                    state.copy(reports = updated, feedbackMessage = "Report dismissed.")
                }
                loadFullAdminControlPanelData()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to dismiss report."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun hideContentFromAdmin(reportId: String?, contentType: ReportContentType, contentId: String, reason: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.hideReportedContent(reportId, contentType, contentId, reason)
            if (result.isSuccess) {
                // If it's a post, filter out from local post feed
                if (contentType == ReportContentType.POST) {
                    _uiState.update { state ->
                        val updated = state.posts.filter { it.id != contentId }
                        state.copy(posts = updated)
                    }
                } else if (contentType == ReportContentType.REEL) {
                    _uiState.update { state ->
                        val updated = state.reels.filter { it.id != contentId }
                        state.copy(reels = updated)
                    }
                } else if (contentType == ReportContentType.BUSINESS_LISTING) {
                    _uiState.update { state ->
                        val updated = state.businessProfiles.filter { it.id != contentId }
                        state.copy(businessProfiles = updated)
                    }
                } else if (contentType == ReportContentType.MARKETPLACE_LISTING) {
                    _uiState.update { state ->
                        val updated = state.marketplaceItems.filter { it.id != contentId }
                        state.copy(marketplaceItems = updated)
                    }
                }
                _uiState.update { it.copy(feedbackMessage = "Content hidden from community.") }
                loadFullAdminControlPanelData()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to hide content."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun restoreContentFromAdmin(contentType: ReportContentType, contentId: String, reason: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.restoreContent(contentType, contentId, reason)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Content restored.") }
                loadFeedAndSocialContent()
                loadFullAdminControlPanelData()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to restore content."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun suspendUserFromAdmin(userId: String, days: Int, reason: String, reportId: String?) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.suspendUserAccount(userId, days, reason, reportId)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Account suspended for $days days.") }
                loadFullAdminControlPanelData()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to suspend account."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun reinstateUserFromAdmin(userId: String, reason: String) {
        if (!ensureActiveAdminSessionForAction()) return
        viewModelScope.launch {
            val result = adminRepository.reinstateUserAccount(userId, reason)
            if (result.isSuccess) {
                _uiState.update { it.copy(feedbackMessage = "Account successfully reinstated.") }
                loadFullAdminControlPanelData()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to reinstate account."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    // ---------------- Yarkhoon Madad Center Operations ----------------

    fun openMadadCenter(category: MadadCategory = MadadCategory.JOBS) {
        _uiState.update {
            it.copy(
                isMadadCenterOpen = true,
                activeMadadCategory = category,
                madadError = null
            )
        }
        loadMadadCenterContent()
    }

    fun closeMadadCenter() {
        _uiState.update { it.copy(isMadadCenterOpen = false) }
    }

    fun selectMadadCategory(category: MadadCategory) {
        _uiState.update { it.copy(activeMadadCategory = category) }
        loadMadadCenterContent()
    }

    fun setMadadJobCityFilter(city: String) {
        _uiState.update { it.copy(madadJobCityFilter = city) }
        loadMadadCenterContent()
    }

    fun setMadadJobSkillQuery(query: String) {
        _uiState.update { it.copy(madadJobSkillQuery = query) }
        loadMadadCenterContent()
    }

    fun setMadadBloodGroupFilter(group: String) {
        _uiState.update { it.copy(madadBloodGroupFilter = group) }
        loadMadadCenterContent()
    }

    fun setMadadBloodCityFilter(city: String) {
        _uiState.update { it.copy(madadBloodCityFilter = city) }
        loadMadadCenterContent()
    }

    fun setMadadAidCategoryFilter(category: String) {
        _uiState.update { it.copy(madadAidCategoryFilter = category) }
        loadMadadCenterContent()
    }

    fun loadMadadCenterContent() {
        val category = _uiState.value.activeMadadCategory
        _uiState.update {
            it.copy(
                isMadadLoading = true,
                madadError = null,
                legalGuidanceTopics = madadRepository.getPredefinedLegalTopics(),
                emergencyContacts = madadRepository.getEmergencyContacts()
            )
        }
        viewModelScope.launch {
            when (category) {
                MadadCategory.JOBS -> {
                    val seekersResult = madadRepository.getJobSeekerProfiles(
                        city = _uiState.value.madadJobCityFilter,
                        skillQuery = _uiState.value.madadJobSkillQuery
                    )
                    val vacanciesResult = madadRepository.getJobVacancies(
                        city = _uiState.value.madadJobCityFilter,
                        query = _uiState.value.madadJobSkillQuery
                    )
                    _uiState.update {
                        it.copy(
                            isMadadLoading = false,
                            jobSeekerProfiles = seekersResult.getOrDefault(emptyList()),
                            jobVacancies = vacanciesResult.getOrDefault(emptyList())
                        )
                    }
                }
                MadadCategory.BLOOD_DONATION -> {
                    val donorsResult = madadRepository.searchBloodDonors(
                        bloodGroup = _uiState.value.madadBloodGroupFilter,
                        city = _uiState.value.madadBloodCityFilter
                    )
                    val requestsResult = madadRepository.getActiveBloodRequests(
                        city = _uiState.value.madadBloodCityFilter,
                        bloodGroup = _uiState.value.madadBloodGroupFilter
                    )
                    _uiState.update {
                        it.copy(
                            isMadadLoading = false,
                            bloodDonors = donorsResult.getOrDefault(emptyList()),
                            bloodRequests = requestsResult.getOrDefault(emptyList())
                        )
                    }
                }
                MadadCategory.COMMUNITY_AID -> {
                    val aidResult = madadRepository.getCommunityAidRequests(
                        category = _uiState.value.madadAidCategoryFilter
                    )
                    _uiState.update {
                        it.copy(
                            isMadadLoading = false,
                            communityAidRequests = aidResult.getOrDefault(emptyList())
                        )
                    }
                }
                MadadCategory.LEGAL_EMERGENCY -> {
                    _uiState.update {
                        it.copy(
                            isMadadLoading = false,
                            legalGuidanceTopics = madadRepository.getPredefinedLegalTopics(),
                            emergencyContacts = madadRepository.getEmergencyContacts()
                        )
                    }
                }
            }
        }
    }

    fun saveJobSeekerProfile(
        qualifications: String,
        skills: List<String>,
        experienceYears: Int,
        preferredRoles: List<String>,
        preferredCities: List<String>,
        summary: String,
        contactPreference: String,
        isPhonePublic: Boolean,
        phoneNumber: String?
    ) {
        val user = _uiState.value.currentUser
        if (qualifications.isBlank() || skills.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "Please enter your qualifications and at least one skill.") }
            return
        }
        if (_uiState.value.isPublishingMadadItem) return
        _uiState.update { it.copy(isPublishingMadadItem = true) }

        val profile = JobSeekerProfile(
            id = user.id,
            userId = user.id,
            name = user.name,
            handle = user.handle,
            avatarUrl = user.avatarUrl,
            qualifications = qualifications.trim(),
            skills = skills,
            experienceYears = experienceYears,
            preferredRoles = preferredRoles,
            preferredCities = preferredCities,
            summary = summary.trim(),
            contactPreference = contactPreference,
            phoneNumber = phoneNumber?.trim() ?: user.phoneNumber,
            isPhonePublic = isPhonePublic,
            isActivelySeeking = true,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = madadRepository.saveJobSeekerProfile(profile)
            _uiState.update { it.copy(isPublishingMadadItem = false) }
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = (listOf(profile) + state.jobSeekerProfiles).distinctBy { it.id }
                    state.copy(
                        jobSeekerProfiles = updated,
                        feedbackMessage = "Your job seeker profile is now active on Madad Center!"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to save job profile."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun publishJobVacancy(
        title: String,
        companyName: String,
        city: String,
        location: String,
        jobType: String,
        salaryRange: String?,
        requiredSkills: List<String>,
        description: String,
        applicationInstructions: String,
        contactEmail: String?,
        contactPhone: String?
    ) {
        val user = _uiState.value.currentUser
        if (title.isBlank() || description.isBlank() || companyName.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please fill in all required job vacancy fields.") }
            return
        }
        if (_uiState.value.isPublishingMadadItem) return
        _uiState.update { it.copy(isPublishingMadadItem = true) }

        val vacancy = JobVacancy(
            id = "vac_${System.currentTimeMillis()}",
            employerId = user.id,
            employerName = user.name,
            employerHandle = user.handle,
            employerAvatarUrl = user.avatarUrl,
            title = title.trim(),
            companyName = companyName.trim(),
            city = city.trim(),
            location = location.trim(),
            jobType = jobType,
            salaryRange = salaryRange?.trim(),
            requiredSkills = requiredSkills,
            description = description.trim(),
            applicationInstructions = applicationInstructions.trim(),
            contactEmail = contactEmail?.trim(),
            contactPhone = contactPhone?.trim(),
            status = "active",
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = madadRepository.createJobVacancy(vacancy)
            _uiState.update { it.copy(isPublishingMadadItem = false) }
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = listOf(vacancy) + state.jobVacancies
                    state.copy(
                        jobVacancies = updated,
                        feedbackMessage = "Job vacancy published successfully on Madad Center!"
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to publish vacancy."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun registerOrUpdateBloodDonor(
        bloodGroup: String,
        city: String,
        area: String,
        isAvailable: Boolean,
        lastDonationDate: String?,
        contactPreference: String,
        isPhonePublic: Boolean,
        phone: String?
    ) {
        val user = _uiState.value.currentUser
        if (bloodGroup.isBlank() || city.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please select your blood group and city.") }
            return
        }
        if (_uiState.value.isPublishingMadadItem) return
        _uiState.update { it.copy(isPublishingMadadItem = true) }

        val donor = BloodDonor(
            id = user.id,
            userId = user.id,
            name = user.name,
            handle = user.handle,
            avatarUrl = user.avatarUrl,
            bloodGroup = bloodGroup,
            city = city,
            area = area.trim(),
            isAvailable = isAvailable,
            lastDonationDate = lastDonationDate,
            contactPreference = contactPreference,
            phoneNumber = phone?.trim() ?: user.phoneNumber,
            isPhonePublic = isPhonePublic,
            registeredAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = madadRepository.registerOrUpdateBloodDonor(donor)
            _uiState.update { it.copy(isPublishingMadadItem = false) }
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = (listOf(donor) + state.bloodDonors).distinctBy { it.id }
                    state.copy(
                        bloodDonors = updated,
                        feedbackMessage = "Thank you! You are now registered as a voluntary blood donor."
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to update donor status."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun createBloodRequest(
        patientName: String,
        bloodGroup: String,
        hospitalName: String,
        city: String,
        requiredUnits: Int,
        urgency: String,
        contactPhone: String,
        notes: String
    ) {
        val user = _uiState.value.currentUser
        if (patientName.isBlank() || hospitalName.isBlank() || contactPhone.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please fill in patient name, hospital, and contact phone.") }
            return
        }
        if (_uiState.value.isPublishingMadadItem) return
        _uiState.update { it.copy(isPublishingMadadItem = true) }

        val request = BloodRequest(
            id = "blood_req_${System.currentTimeMillis()}",
            requesterId = user.id,
            requesterName = user.name,
            requesterHandle = user.handle,
            requesterAvatarUrl = user.avatarUrl,
            patientName = patientName.trim(),
            bloodGroup = bloodGroup,
            hospitalName = hospitalName.trim(),
            city = city.trim(),
            requiredUnits = requiredUnits.coerceAtLeast(1),
            urgency = urgency,
            contactPhone = contactPhone.trim(),
            notes = notes.trim(),
            status = "active",
            createdAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (48 * 3600 * 1000L)
        )

        viewModelScope.launch {
            val result = madadRepository.createBloodRequest(request)
            _uiState.update { it.copy(isPublishingMadadItem = false) }
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = listOf(request) + state.bloodRequests
                    state.copy(
                        bloodRequests = updated,
                        feedbackMessage = "Urgent blood request posted to Madad Center network."
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to post blood request."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun createCommunityAidRequest(
        beneficiaryName: String,
        category: String,
        title: String,
        description: String,
        city: String,
        location: String,
        assistanceType: String,
        targetAmount: Double?,
        contactMethod: String
    ) {
        val user = _uiState.value.currentUser
        if (title.isBlank() || description.isBlank() || beneficiaryName.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please fill in beneficiary name, title, and detailed description.") }
            return
        }
        if (_uiState.value.isPublishingMadadItem) return
        _uiState.update { it.copy(isPublishingMadadItem = true) }

        val request = CommunityAidRequest(
            id = "aid_req_${System.currentTimeMillis()}",
            requesterId = user.id,
            requesterName = user.name,
            requesterHandle = user.handle,
            requesterAvatarUrl = user.avatarUrl,
            beneficiaryName = beneficiaryName.trim(),
            category = category,
            title = title.trim(),
            description = description.trim(),
            city = city.trim(),
            location = location.trim(),
            assistanceType = assistanceType,
            targetAmount = targetAmount,
            status = "active",
            verificationStatus = "pending_review",
            contactMethod = contactMethod,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = madadRepository.createCommunityAidRequest(request)
            _uiState.update { it.copy(isPublishingMadadItem = false) }
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = listOf(request) + state.communityAidRequests
                    state.copy(
                        communityAidRequests = updated,
                        feedbackMessage = "Assistance request submitted. It will be reviewed by community moderators."
                    )
                }
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to submit aid request."
                _uiState.update { it.copy(feedbackMessage = "Error: $err") }
            }
        }
    }

    fun markBloodRequestFulfilled(requestId: String) {
        viewModelScope.launch {
            val result = madadRepository.markBloodRequestFulfilled(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = state.bloodRequests.map {
                        if (it.id == requestId) it.copy(status = "fulfilled") else it
                    }
                    state.copy(bloodRequests = updated, feedbackMessage = "Blood request marked as fulfilled! Thank you.")
                }
            }
        }
    }

    fun markCommunityAidFulfilled(requestId: String) {
        viewModelScope.launch {
            val result = madadRepository.markCommunityAidFulfilled(requestId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = state.communityAidRequests.map {
                        if (it.id == requestId) it.copy(status = "fulfilled") else it
                    }
                    state.copy(communityAidRequests = updated, feedbackMessage = "Aid request marked as fulfilled. Thank you!")
                }
            }
        }
    }

    fun closeJobVacancy(vacancyId: String) {
        viewModelScope.launch {
            val result = madadRepository.closeJobVacancy(vacancyId)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val updated = state.jobVacancies.map {
                        if (it.id == vacancyId) it.copy(status = "closed") else it
                    }
                    state.copy(jobVacancies = updated, feedbackMessage = "Job vacancy closed.")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { authRepository.removeAuthStateListener(it) }
        sessionInitializationJob?.cancel()
        feedInitializationJob?.cancel()
        conversationsListener?.remove()
        messagesListener?.remove()
        notificationsListener?.remove()
    }
}

