package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.MainNavTab
import com.example.ui.YarkhoonViewModel
import androidx.activity.compose.BackHandler
import com.example.data.User
import com.example.ui.components.CreateBusinessListingDialog
import com.example.ui.components.CreatePostDialog
import com.example.ui.components.CreateReelDialog
import com.example.ui.components.CreateStoryDialog
import com.example.ui.components.FollowRequestsDialog
import com.example.ui.components.HashtagDetailDialog
import com.example.ui.components.PostCommentsDialog
import com.example.ui.components.PrivacySettingsDialog
import com.example.ui.components.ReportContentDialog
import com.example.ui.components.SellItemDialog
import com.example.ui.components.SharePostDialog
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.screens.AdminDashboardDialog
import com.example.ui.theme.FbBackground
import com.example.ui.theme.FbBlue
import com.example.ui.theme.FbBlueLight
import com.example.ui.theme.FbBorder
import com.example.ui.theme.FbSurface
import com.example.ui.theme.FbTextPrimary
import com.example.ui.theme.FbTextSecondary
import com.example.ui.theme.NavIconActive
import com.example.ui.theme.NavIconInactive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarkhoonMainApp(
    viewModel: YarkhoonViewModel = viewModel(),
    initialConversationId: String? = null,
    initialOpenScreen: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRegisterBusinessOpen by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAppResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Request POST_NOTIFICATIONS permission on Android 13+ (Tiramisu)
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!initialConversationId.isNullOrBlank()) {
            viewModel.openConversationById(initialConversationId)
        } else if (initialOpenScreen == "notifications" || initialOpenScreen == "friend_requests") {
            viewModel.openNotifications()
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissFeedback()
        }
    }

    // Check Authentication State
    if (!uiState.isAuthenticated) {
        AuthScreen(
            authMode = uiState.authMode,
            isLoading = uiState.authLoading,
            errorMessage = uiState.authError,
            onSwitchMode = { viewModel.setAuthMode(it) },
            onSignIn = { id, pass -> viewModel.signIn(id, pass) },
            onCompleteOnboarding = { name, handle, email, phone, pass, bio, edu, job, loc, interests, isBiz, cat, avatar ->
                viewModel.registerWithOnboarding(name, handle, email, phone, pass, bio, edu, job, loc, interests, isBiz, cat, avatar)
            },
            onForgotPassword = { email -> viewModel.sendPasswordReset(email) },
            onAdminSignIn = { email, pass, rememberMe -> viewModel.signInAsAdmin(email, pass, rememberMe) }
        )
        return
    }

    BackHandler(enabled = uiState.isMadadCenterOpen) {
        viewModel.closeMadadCenter()
    }
    BackHandler(enabled = uiState.activeHashtagDetail != null) {
        viewModel.closeHashtagDetail()
    }
    BackHandler(enabled = uiState.selectedUserProfile != null) {
        viewModel.closeUserProfile()
    }
    BackHandler(enabled = uiState.currentTab == MainNavTab.EXPLORE && uiState.exploreSearchQuery.isNotBlank()) {
        viewModel.clearExploreSearch()
    }
    BackHandler(enabled = uiState.currentTab != MainNavTab.HOME && uiState.activeHashtagDetail == null && uiState.selectedUserProfile == null) {
        viewModel.selectTab(MainNavTab.HOME)
    }

    // Authenticated Social Platform View
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_brand_header")
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground_asset),
                            contentDescription = "YARKHOON.COM Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "YARKHOON.COM",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FbBlue,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = "Social Network & Business Hub",
                                style = MaterialTheme.typography.labelSmall,
                                color = FbTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.isAdmin) {
                        IconButton(
                            onClick = { viewModel.openAdminDashboard() },
                            modifier = Modifier.testTag("topbar_admin_panel_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Control Panel",
                                tint = FbBlue
                            )
                        }
                    }

                    // Yarkhoon Madad Center Action (Community Relief & Mutual Aid)
                    IconButton(
                        onClick = { viewModel.openMadadCenter() },
                        modifier = Modifier.testTag("topbar_madad_center_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = "Yarkhoon Madad Center",
                            tint = FbBlue
                        )
                    }

                    // Search & Explore Action (Stage 10)
                    IconButton(
                        onClick = { viewModel.selectTab(MainNavTab.EXPLORE) },
                        modifier = Modifier.testTag("topbar_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search & Explore",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Notifications Action
                    IconButton(
                        onClick = { viewModel.openNotifications() },
                        modifier = Modifier.testTag("topbar_notifications_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                val unread = uiState.notifications.count { !it.isRead }
                                if (unread > 0) {
                                    Badge(containerColor = FbBlue) {
                                        Text("$unread", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Direct Messages Action
                    IconButton(
                        onClick = { viewModel.openMessages() },
                        modifier = Modifier.testTag("topbar_messages_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                val unreadMessages = uiState.conversations.sumOf { it.unreadCount }
                                if (unreadMessages > 0) {
                                    Badge(containerColor = FbBlue) {
                                        Text("$unreadMessages", color = Color.White)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Direct Messages",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // 1. Home (Social Feed)
                NavigationBarItem(
                    selected = uiState.currentTab == MainNavTab.HOME,
                    onClick = { viewModel.selectTab(MainNavTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == MainNavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home Feed"
                        )
                    },
                    label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_home"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavIconActive,
                        unselectedIconColor = NavIconInactive,
                        selectedTextColor = NavIconActive,
                        unselectedTextColor = NavIconInactive,
                        indicatorColor = FbBlueLight
                    )
                )

                // 2. Reels (Vertical Video)
                NavigationBarItem(
                    selected = uiState.currentTab == MainNavTab.REELS,
                    onClick = { viewModel.selectTab(MainNavTab.REELS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == MainNavTab.REELS) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow,
                            contentDescription = "Reels"
                        )
                    },
                    label = { Text("Reels", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_reels"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavIconActive,
                        unselectedIconColor = NavIconInactive,
                        selectedTextColor = NavIconActive,
                        unselectedTextColor = NavIconInactive,
                        indicatorColor = FbBlueLight
                    )
                )

                // 3. Explore (Creators & Trending)
                NavigationBarItem(
                    selected = uiState.currentTab == MainNavTab.EXPLORE,
                    onClick = { viewModel.selectTab(MainNavTab.EXPLORE) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == MainNavTab.EXPLORE) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Explore"
                        )
                    },
                    label = { Text("Explore", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_explore"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavIconActive,
                        unselectedIconColor = NavIconInactive,
                        selectedTextColor = NavIconActive,
                        unselectedTextColor = NavIconInactive,
                        indicatorColor = FbBlueLight
                    )
                )

                // 4. Business Hub
                NavigationBarItem(
                    selected = uiState.currentTab == MainNavTab.BUSINESS,
                    onClick = { viewModel.selectTab(MainNavTab.BUSINESS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == MainNavTab.BUSINESS) Icons.Filled.BusinessCenter else Icons.Outlined.BusinessCenter,
                            contentDescription = "Business Hub"
                        )
                    },
                    label = { Text("Hub", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_business"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavIconActive,
                        unselectedIconColor = NavIconInactive,
                        selectedTextColor = NavIconActive,
                        unselectedTextColor = NavIconInactive,
                        indicatorColor = FbBlueLight
                    )
                )

                // 5. Profile
                NavigationBarItem(
                    selected = uiState.currentTab == MainNavTab.PROFILE,
                    onClick = { viewModel.selectTab(MainNavTab.PROFILE) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == MainNavTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_tab_profile"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavIconActive,
                        unselectedIconColor = NavIconInactive,
                        selectedTextColor = NavIconActive,
                        unselectedTextColor = NavIconInactive,
                        indicatorColor = FbBlueLight
                    )
                )
            }
        },
        floatingActionButton = {
            if (uiState.currentTab == MainNavTab.HOME || uiState.currentTab == MainNavTab.EXPLORE) {
                FloatingActionButton(
                    onClick = { viewModel.openCreatePost() },
                    containerColor = FbBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_create_post")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "New Post")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Persistent Account Suspension Warning Banner
                if (uiState.isUserSuspended) {
                    val suspension = uiState.suspensionInfo
                    val daysRemaining = if (suspension != null && suspension.suspendedUntil > 0L) {
                        val diff = suspension.suspendedUntil - System.currentTimeMillis()
                        if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1) else 0
                    } else null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .testTag("account_suspension_banner"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GppBad,
                                contentDescription = "Suspended",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Account Suspended by Moderation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = if (daysRemaining != null) {
                                        "Suspension expires in ~$daysRemaining day(s). Reason: ${suspension?.suspensionReason ?: "Policy violation"}"
                                    } else {
                                        "Indefinite suspension. Reason: ${suspension?.suspensionReason ?: "Policy violation"}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Creating posts, comments, stories, and listings is temporarily restricted.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.currentTab) {
                MainNavTab.HOME -> HomeScreen(
                    currentUser = uiState.currentUser,
                    posts = uiState.posts,
                    stories = uiState.stories,
                    currentFilter = uiState.feedFilter,
                    onFilterSelected = { viewModel.setFeedFilter(it) },
                    onStoryClick = { viewModel.openStory(it) },
                    onAddStoryClick = { viewModel.openCreateStory() },
                    onCreatePostClick = { type, feeling, mediaUri -> viewModel.openCreatePost(type, feeling, mediaUri) },
                    onLikeClick = { viewModel.toggleLike(it) },
                    onRepostClick = { post ->
                        val matching = uiState.posts.find { it.id == post }
                        if (matching != null) viewModel.openSharePost(matching)
                    },
                    onCommentClick = { viewModel.openComments(it) },
                    onShareClick = { postId ->
                        val matching = uiState.posts.find { it.id == postId }
                        if (matching != null) viewModel.openSharePost(matching)
                    },
                    onBookmarkClick = { viewModel.toggleBookmark(it) },
                    isFeedLoading = uiState.isFeedLoading,
                    feedError = uiState.feedError,
                    onRefreshFeed = { viewModel.refreshSocialFeed() },
                    onHashtagClick = { viewModel.openHashtagDetail(it) },
                    onAuthorClick = { viewModel.openUserProfile(it) },
                    onMentionClick = { viewModel.openUserProfileByHandle(it) },
                    followingUserIds = uiState.followingUserIds,
                    blockedUserIds = uiState.blockedUserIds,
                    mutedUserIds = uiState.mutedUserIds,
                    favoriteUserIds = uiState.favoriteUserIds,
                    suggestedCreators = uiState.discoveredUsers.ifEmpty { uiState.exploreContent.recommendedCreators },
                    onToggleFollow = { viewModel.toggleFollowUser(it) },
                    onToggleFavorite = { viewModel.toggleFavoriteUser(it) },
                    onReactionSelect = { postId, reaction -> viewModel.setReaction(postId, reaction) },
                    onOpenMadadCenter = { viewModel.openMadadCenter() },
                    onReportPost = { post ->
                        viewModel.openReportDialog(
                            contentType = com.example.data.ReportContentType.POST,
                            contentId = post.id,
                            contentSummary = post.text.take(80),
                            reportedUserId = post.author.id,
                            reportedUserName = post.author.name
                        )
                    }
                )
                MainNavTab.REELS -> ReelsScreen(
                    reels = uiState.reels,
                    onLikeClick = { viewModel.toggleReelLike(it) },
                    onCommentClick = { reel ->
                        val matching = uiState.posts.find { it.id == reel.id }
                            ?: com.example.data.Post(
                                id = reel.id,
                                author = reel.author,
                                text = reel.caption,
                                likesCount = reel.likesCount,
                                commentsCount = reel.commentsCount
                            )
                        viewModel.openComments(matching)
                    },
                    onShareClick = { viewModel.shareReel(it) },
                    onBookmarkClick = { viewModel.toggleReelBookmark(it) },
                    onUploadReelClick = { viewModel.openCreateReel() },
                    blockedUserIds = uiState.blockedUserIds,
                    mutedUserIds = uiState.mutedUserIds,
                    followingUserIds = uiState.followingUserIds,
                    onAuthorClick = { viewModel.openUserProfile(it) },
                    onToggleFollow = { viewModel.toggleFollowUser(it) },
                    onReportReel = { reel ->
                        viewModel.openReportDialog(
                            contentType = com.example.data.ReportContentType.REEL,
                            contentId = reel.id,
                            contentSummary = reel.caption.take(80),
                            reportedUserId = reel.author.id,
                            reportedUserName = reel.author.name
                        )
                    }
                )
                MainNavTab.EXPLORE -> ExploreScreen(
                    searchQuery = uiState.exploreSearchQuery,
                    onSearchQueryChange = { viewModel.updateExploreSearch(it) },
                    onClearSearch = { viewModel.clearExploreSearch() },
                    activeSearchTab = uiState.searchActiveTab,
                    onSearchTabSelected = { viewModel.setSearchTab(it) },
                    searchResults = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    searchError = uiState.searchError,
                    onRetrySearch = { viewModel.performSearch() },
                    exploreContent = uiState.exploreContent,
                    isExploreLoading = uiState.isExploreLoading,
                    exploreError = uiState.exploreError,
                    onRefreshExplore = { viewModel.loadExploreContent() },
                    onHashtagClick = { viewModel.openHashtagDetail(it) },
                    onUserClick = { viewModel.openUserProfile(it) },
                    onPostClick = { viewModel.openComments(it) },
                    onReelClick = { viewModel.openReelFromExplore(it) },
                    onLikePost = { viewModel.toggleLike(it) },
                    blockedUserIds = uiState.blockedUserIds,
                    followingUserIds = uiState.followingUserIds
                )
                MainNavTab.BUSINESS -> BusinessHubScreen(
                    currentUser = uiState.currentUser,
                    activeSection = uiState.activeHubSection,
                    onSectionSelected = { viewModel.switchHubSection(it) },
                    businesses = uiState.businessProfiles,
                    selectedBusinessCategory = uiState.selectedBusinessCategory,
                    onBusinessCategorySelected = { viewModel.selectBusinessCategory(it) },
                    businessSearchQuery = uiState.businessSearchQuery,
                    onBusinessSearchQueryChange = { viewModel.updateBusinessSearch(it) },
                    onContactBusiness = { viewModel.contactBusiness(it) },
                    onOpenRegisterBusiness = { isRegisterBusinessOpen = true },
                    marketplaceItems = uiState.marketplaceItems,
                    selectedMarketplaceCategory = uiState.selectedMarketplaceCategory,
                    onMarketplaceCategorySelected = { viewModel.selectMarketplaceCategory(it) },
                    marketplaceSearchQuery = uiState.marketplaceSearchQuery,
                    onMarketplaceSearchQueryChange = { viewModel.updateMarketplaceSearch(it) },
                    onContactSeller = { viewModel.contactSeller(it) },
                    onOpenSellItem = { viewModel.openSellItemDialog() },
                    onEditMarketplaceItem = { viewModel.openSellItemDialog(it) },
                    onDeleteMarketplaceItem = { viewModel.deleteMarketplaceItem(it) },
                    onMarkMarketplaceItemSold = { viewModel.markMarketplaceItemSold(it) },
                    onReportBusiness = { biz ->
                        viewModel.openReportDialog(
                            contentType = com.example.data.ReportContentType.BUSINESS_LISTING,
                            contentId = biz.id,
                            contentSummary = "${biz.businessName} - ${biz.serviceHighlight}".take(80),
                            reportedUserId = biz.owner.id,
                            reportedUserName = biz.businessName
                        )
                    },
                    onReportMarketplaceItem = { item ->
                        viewModel.openReportDialog(
                            contentType = com.example.data.ReportContentType.MARKETPLACE_LISTING,
                            contentId = item.id,
                            contentSummary = "${item.title} (${item.price})".take(80),
                            reportedUserId = item.seller.id,
                            reportedUserName = item.seller.name
                        )
                    }
                )
                MainNavTab.PROFILE -> ProfileScreen(
                    user = uiState.currentUser,
                    allPosts = uiState.posts,
                    currentTab = uiState.profileTab,
                    onTabSelected = { viewModel.setProfileTab(it) },
                    onLikeClick = { viewModel.toggleLike(it) },
                    onRepostClick = { post ->
                        val matching = uiState.posts.find { it.id == post }
                        if (matching != null) viewModel.openSharePost(matching)
                    },
                    onCommentClick = { viewModel.openComments(it) },
                    onShareClick = { postId ->
                        val matching = uiState.posts.find { it.id == postId }
                        if (matching != null) viewModel.openSharePost(matching)
                    },
                    onBookmarkClick = { viewModel.toggleBookmark(it) },
                    onLogoutClick = { viewModel.logout() },
                    isProfileLoading = uiState.isProfileLoading,
                    profileError = uiState.profileError,
                    onDismissProfileError = { viewModel.dismissProfileError() },
                    onUpdateProfile = { name, bio, loc, job, edu, avatar, cover, phone ->
                        viewModel.updateUserProfile(name, bio, loc, job, edu, avatar, cover, phone)
                    },
                    onHashtagClick = { viewModel.openHashtagDetail(it) },
                    onMentionClick = { viewModel.openUserProfileByHandle(it) },
                    pendingFollowRequestsCount = uiState.pendingFollowRequests.size,
                    onOpenPrivacySettings = { viewModel.openPrivacySettings() },
                    onOpenFollowRequests = { viewModel.openFollowRequests() },
                    isAdmin = uiState.isAdmin,
                    onOpenAdminDashboard = { viewModel.openAdminDashboard() },
                    onRequestBlueTickVerification = { cat, info -> viewModel.requestBlueTickVerification(cat, info) }
                )
            }
        }
    }
}
}

    // Modal: Ephemeral Story Viewer
    uiState.activeStory?.let { story ->
        StoryViewerDialog(
            story = story,
            onDismiss = { viewModel.closeStory() }
        )
    }

    // Modal: Threaded Post Comments (Stage 11)
    uiState.activeCommentsPost?.let { post ->
        PostCommentsDialog(
            post = post,
            currentUser = uiState.currentUser,
            comments = uiState.activePostComments,
            isLoadingComments = uiState.isLoadingComments,
            commentError = uiState.commentError,
            registeredUsers = uiState.discoveredUsers.ifEmpty { uiState.exploreContent.recommendedCreators },
            onDismiss = { viewModel.closeComments() },
            onAddComment = { text, parentId, replyToUsername ->
                viewModel.addComment(post.id, text, parentId, replyToUsername)
            },
            onLikeComment = { viewModel.toggleLikeComment(post.id, it) },
            onEditComment = { commentId, newText -> viewModel.editComment(post.id, commentId, newText) },
            onDeleteComment = { viewModel.deleteComment(post.id, it) },
            onAuthorClick = { viewModel.openUserProfile(it) },
            onHashtagClick = { viewModel.openHashtagDetail(it) },
            onMentionClick = { viewModel.openUserProfileByHandle(it) },
            onReportComment = { comment ->
                viewModel.openReportDialog(
                    contentType = com.example.data.ReportContentType.COMMENT,
                    contentId = comment.id,
                    contentSummary = comment.text.take(80),
                    reportedUserId = comment.author.id,
                    reportedUserName = comment.author.name
                )
            }
        )
    }

    // Modal: Share Post Bottom Sheet / Dialog (Stage 11)
    uiState.activeSharePost?.let { post ->
        val context = androidx.compose.ui.platform.LocalContext.current
        SharePostDialog(
            post = post,
            onDismiss = { viewModel.closeSharePost() },
            onShareToFeed = { thoughts -> viewModel.sharePostToFeed(post, thoughts) },
            onCopyLink = { viewModel.copyPostLink(context, post.id) },
            onExternalShare = { viewModel.sharePostExternal(context, post) }
        )
    }

    // Modal: Create Post Composer
    if (uiState.isCreatePostOpen) {
        CreatePostDialog(
            currentUser = uiState.currentUser,
            onDismiss = { viewModel.closeCreatePost() },
            isPublishing = uiState.isPublishingPost,
            uploadProgress = uiState.uploadProgress,
            publishError = uiState.publishError,
            onDismissError = { viewModel.dismissPublishError() },
            initialPostType = uiState.createPostInitialType,
            initialFeeling = uiState.createPostInitialFeeling,
            initialMediaUri = uiState.createPostInitialMediaUri,
            onPublish = { text, type, mediaUrl, tags ->
                viewModel.createPost(text, type, mediaUrl, tags)
            }
        )
    }

    // Modal: Create Ephemeral Story Composer
    if (uiState.isCreateStoryOpen) {
        CreateStoryDialog(
            currentUser = uiState.currentUser,
            onDismiss = { viewModel.closeCreateStory() },
            isPublishing = uiState.isPublishingPost,
            uploadProgress = uiState.uploadProgress,
            publishError = uiState.publishError,
            onDismissError = { viewModel.dismissPublishError() },
            onPublishStory = { mediaUri, caption ->
                viewModel.publishStory(mediaUri, caption)
            }
        )
    }

    // Modal: Create Reel Video Composer
    if (uiState.isCreateReelOpen) {
        CreateReelDialog(
            onDismiss = { viewModel.closeCreateReel() },
            isPublishing = uiState.isPublishingPost,
            uploadProgress = uiState.uploadProgress,
            publishError = uiState.publishError,
            onDismissError = { viewModel.dismissPublishError() },
            onPublishReel = { videoUri, caption, audioTrack ->
                viewModel.publishReel(videoUri, caption, audioTrack)
            }
        )
    }

    // Modal: Direct Messages & Real-Time Chat (Stage 8)
    if (uiState.isMessagesOpen) {
        MessagesDialog(
            currentUser = uiState.currentUser,
            conversations = uiState.conversations,
            activeConversation = uiState.activeConversation,
            activeMessages = uiState.activeMessages,
            isMessagesLoading = uiState.isMessagesLoading,
            isSendingMessage = uiState.isSendingMessage,
            messagingError = uiState.messagingError,
            discoveredUsers = uiState.discoveredUsers,
            isUserDiscoveryOpen = uiState.isUserDiscoveryOpen,
            userSearchQuery = uiState.userSearchQuery,
            isSearchingUsers = uiState.isSearchingUsers,
            blockedUserIds = uiState.blockedUserIds,
            onUnblockUser = { viewModel.unblockUser(it) },
            onSelectConversation = { viewModel.selectConversation(it) },
            onCloseActiveConversation = { viewModel.closeActiveConversation() },
            onSendMessage = { viewModel.sendDirectMessage(it) },
            onOpenUserDiscovery = { viewModel.openUserDiscovery() },
            onCloseUserDiscovery = { viewModel.closeUserDiscovery() },
            onSearchQueryChanged = { viewModel.onUserSearchQueryChanged(it) },
            onStartConversation = { viewModel.startConversationWithUser(it) },
            onLoadMoreMessages = { viewModel.loadMoreMessages() },
            onDismiss = { viewModel.closeMessages() }
        )
    }

    // Modal: Notifications Dialog
    if (uiState.isNotificationsOpen) {
        NotificationsDialog(
            notifications = uiState.notifications,
            isLoading = uiState.isNotificationsLoading,
            errorMessage = uiState.notificationError,
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onNotificationClick = { notif ->
                viewModel.markNotificationRead(notif.id)
                if (notif.targetPostId != null) {
                    viewModel.openPostById(notif.targetPostId)
                } else if (notif.type == "follow" || notif.type == "follow_request" || notif.type == "follow_accepted") {
                    viewModel.openUserProfile(notif.sender)
                }
            },
            onDeleteNotification = { notif ->
                viewModel.deleteNotification(notif.id)
            },
            onDismiss = { viewModel.closeNotifications() }
        )
    }

    // Modal: Sell an Item / Edit Listing in Marketplace
    if (uiState.isSellItemDialogOpen) {
        SellItemDialog(
            itemToEdit = uiState.itemToEdit,
            onDismiss = { viewModel.closeSellItemDialog() },
            onSubmit = { itemId, title, cat, price, desc, phone, loc, images ->
                viewModel.saveMarketplaceItem(itemId, title, cat, price, desc, phone, loc, images)
            }
        )
    }

    // Modal: Register Business Profile in Hub
    if (isRegisterBusinessOpen) {
        CreateBusinessListingDialog(
            onDismiss = { isRegisterBusinessOpen = false },
            onSubmit = { name, cat, highlight, desc, loc, phone, images ->
                viewModel.addBusinessListing(name, cat, highlight, desc, loc, phone, images)
                isRegisterBusinessOpen = false
            }
        )
    }

    // Modal: Hashtag Detail Viewer (Stage 10)
    uiState.activeHashtagDetail?.let { detail ->
        HashtagDetailDialog(
            detail = detail,
            onDismiss = { viewModel.closeHashtagDetail() },
            onLikePost = { viewModel.toggleLike(it) },
            onCommentPost = { viewModel.openComments(it) },
            onAuthorClick = { viewModel.openUserProfile(it) },
            onHashtagClick = { viewModel.openHashtagDetail(it) }
        )
    }

    // Modal: User Profile Viewer (Stage 10, 11 & 12)
    uiState.selectedUserProfile?.let { user ->
        UserProfileDialog(
            user = user,
            posts = (uiState.selectedUserPosts + uiState.posts).distinctBy { it.id },
            isFollowing = uiState.followingUserIds.contains(user.id),
            isFollowRequested = uiState.sentFollowRequestUserIds.contains(user.id),
            isBlocked = uiState.blockedUserIds.contains(user.id),
            isMuted = uiState.mutedUserIds.contains(user.id),
            isFavorite = uiState.favoriteUserIds.contains(user.id),
            currentUserId = uiState.currentUser.id,
            onDismiss = { viewModel.closeUserProfile() },
            onSendMessage = {
                viewModel.startConversationWithUser(it)
                viewModel.openMessages()
            },
            onLikePost = { viewModel.toggleLike(it) },
            onCommentPost = { viewModel.openComments(it) },
            onHashtagClick = { viewModel.openHashtagDetail(it) },
            onToggleFollow = { viewModel.toggleFollowUser(it) },
            onCancelFollowRequest = { viewModel.cancelFollowRequest(it.id) },
            onBlockUser = { viewModel.blockUser(it) },
            onUnblockUser = { viewModel.unblockUser(it.id) },
            onToggleMute = { viewModel.toggleMuteUser(it) },
            onToggleFavorite = { viewModel.toggleFavoriteUser(it) },
            onBookmarkPost = { viewModel.toggleBookmark(it) },
            onSharePost = { viewModel.openSharePost(it) },
            onMentionClick = { viewModel.openUserProfileByHandle(it) },
            onReportUser = { targetUser ->
                viewModel.openReportDialog(
                    contentType = com.example.data.ReportContentType.USER_PROFILE,
                    contentId = targetUser.id,
                    contentSummary = "${targetUser.name} (${targetUser.handle})",
                    reportedUserId = targetUser.id,
                    reportedUserName = targetUser.name
                )
            }
        )
    }

    // Modal: Privacy & Safety Settings (Stage 12)
    if (uiState.isPrivacySettingsOpen) {
        PrivacySettingsDialog(
            currentUser = uiState.currentUser,
            isAccountPrivate = uiState.currentUser.isPrivate,
            pendingFollowRequests = uiState.pendingFollowRequests,
            blockedUsers = uiState.blockedUsers,
            mutedUsers = uiState.mutedUsers,
            favoriteUsers = uiState.favoriteUsers,
            onToggleAccountPrivacy = { viewModel.updateAccountPrivacy(it) },
            onOpenFollowRequests = { viewModel.openFollowRequests() },
            onUnblockUser = { viewModel.unblockUser(it) },
            onUnmuteUser = { uid ->
                val target = uiState.mutedUsers.find { it.id == uid } ?: User(id = uid, name = "Member", handle = "@$uid", avatarUrl = "")
                viewModel.toggleMuteUser(target)
            },
            onRemoveFavorite = { uid ->
                val target = uiState.favoriteUsers.find { it.id == uid } ?: User(id = uid, name = "Member", handle = "@$uid", avatarUrl = "")
                viewModel.toggleFavoriteUser(target)
            },
            onUserClick = { viewModel.openUserProfile(it) },
            onDismiss = { viewModel.closePrivacySettings() }
        )
    }

    // Modal: Follow Requests Management (Stage 12)
    if (uiState.isFollowRequestsOpen) {
        FollowRequestsDialog(
            requests = uiState.pendingFollowRequests,
            isLoading = uiState.isLoadingFollowRequests,
            onApprove = { viewModel.approveFollowRequest(it) },
            onReject = { viewModel.rejectFollowRequest(it) },
            onUserClick = { viewModel.openUserProfile(it) },
            onDismiss = { viewModel.closeFollowRequests() }
        )
    }

    // Modal: Report Content / User / Listing (Stage 13)
    if (uiState.isReportDialogOpen && uiState.pendingReportTarget != null) {
        val target = uiState.pendingReportTarget!!
        ReportContentDialog(
            contentType = target.contentType,
            contentId = target.contentId,
            contentSummary = target.contentSummary,
            reportedUserId = target.reportedUserId,
            reportedUserName = target.reportedUserName,
            onDismiss = { viewModel.closeReportDialog() },
            onSubmitReport = { reason, explanation ->
                viewModel.submitReport(reason, explanation)
            }
        )
    }

    // Modal: Complete 15-Section Admin Control Panel & Moderation
    if (uiState.isAdminDashboardOpen) {
        AdminDashboardDialog(
            reports = uiState.reports,
            auditLogs = uiState.auditLogs,
            isLoading = uiState.isAdminLoading,
            errorMessage = uiState.adminError,
            diagnosticInfo = uiState.adminDiagnosticInfo,
            onRefresh = { viewModel.loadFullAdminControlPanelData() },
            onDismissReport = { reportId, reason ->
                viewModel.dismissReport(reportId, reason)
            },
            onHideContent = { reportId, contentType, contentId, reason ->
                viewModel.hideContentFromAdmin(reportId, contentType, contentId, reason)
            },
            onRestoreContent = { contentType, contentId, reason ->
                viewModel.restoreContentFromAdmin(contentType, contentId, reason)
            },
            onSuspendUser = { userId, days, reason, reportId ->
                viewModel.suspendUserFromAdmin(userId, days, reason, reportId)
            },
            onReinstateUser = { userId, reason ->
                viewModel.reinstateUserFromAdmin(userId, reason)
            },
            onDismiss = { viewModel.closeAdminDashboard() },
            adminRole = uiState.adminRole,
            currentUser = uiState.currentUser,
            activeSection = uiState.activeAdminSection,
            onSelectSection = { viewModel.selectAdminSection(it) },
            stats = uiState.adminStats,
            registeredUsers = uiState.adminUsersList,
            allPosts = uiState.adminPostsList.ifEmpty { uiState.posts },
            verificationRequests = uiState.verificationRequests,
            villages = uiState.villagesList,
            officialAnnouncements = uiState.officialAnnouncements,
            curatedTrendingTopics = uiState.curatedTrendingTopics,
            adminAccounts = uiState.adminAccountsList,
            sessionStartedAt = uiState.adminSessionStartedAt,
            onApprovePost = { postId, reason -> viewModel.approvePostFromAdmin(postId, reason) },
            onRejectPost = { postId, reason -> viewModel.rejectPostFromAdmin(postId, reason) },
            onTogglePinPost = { postId, isPinned -> viewModel.togglePinPostFromAdmin(postId, isPinned) },
            onFeaturePost = { postId, isFeatured, days, delayHours ->
                viewModel.featurePostFromAdmin(postId, isFeatured, days, delayHours)
            },
            onToggleTrendingPost = { postId, isTrending -> viewModel.toggleTrendingPostFromAdmin(postId, isTrending) },
            onPromotePost = { postId, isPromoted, days, cat, loc ->
                viewModel.promotePostFromAdmin(postId, isPromoted, days, cat, loc)
            },
            onSetUserVerification = { userId, isVerified, reason ->
                viewModel.setUserVerificationFromAdmin(userId, isVerified, reason)
            },
            onReviewVerificationRequest = { reqId, userId, approve, notes ->
                viewModel.reviewVerificationRequestFromAdmin(reqId, userId, approve, notes)
            },
            onSaveTrendingTopic = { id, tag, headline, cat, village, isPinned ->
                viewModel.saveTrendingTopicFromAdmin(id, tag, headline, cat, village, isPinned)
            },
            onDeleteTrendingTopic = { viewModel.deleteTrendingTopicFromAdmin(it) },
            onSaveVillage = { id, name, tehsil, district, desc, pop ->
                viewModel.saveVillageFromAdmin(id, name, tehsil, district, desc, pop)
            },
            onDeleteVillage = { id, name -> viewModel.deleteVillageFromAdmin(id, name) },
            onPublishAnnouncement = { title, body, cat, loc, prio ->
                viewModel.publishOfficialAnnouncementFromAdmin(title, body, cat, loc, prio)
            },
            onAssignAdminRole = { uid, email, name, role, perms ->
                viewModel.assignAdminRoleFromAdmin(uid, email, name, role, perms)
            },
            onRevokeAdminRole = { uid, reason -> viewModel.revokeAdminRoleFromAdmin(uid, reason) },
            onViewUserProfile = { user ->
                viewModel.closeAdminDashboard()
                viewModel.openUserProfile(user)
            },
            onAdminLogout = { viewModel.logoutFromAdminPanel(expired = false) }
        )
    }

    // Modal: Yarkhoon Madad Center (Community Relief, Jobs, Donors & Aid)
    if (uiState.isMadadCenterOpen) {
        MadadCenterScreen(
            uiState = uiState,
            currentUser = uiState.currentUser,
            onDismiss = { viewModel.closeMadadCenter() },
            onSelectCategory = { viewModel.selectMadadCategory(it) },
            onSearchQueryChange = { viewModel.setMadadJobSkillQuery(it) },
            onJobCityFilterChange = { viewModel.setMadadJobCityFilter(it) },
            onBloodGroupFilterChange = { viewModel.setMadadBloodGroupFilter(it) },
            onBloodCityFilterChange = { viewModel.setMadadBloodCityFilter(it) },
            onAidCategoryFilterChange = { viewModel.setMadadAidCategoryFilter(it) },
            onRefresh = { viewModel.loadMadadCenterContent() },
            onContactUser = { user ->
                viewModel.startConversationWithUser(user)
                viewModel.openMessages()
            },
            onSaveJobSeekerProfile = { qual, skills, exp, roles, cities, sum, pref, isPub, phone ->
                viewModel.saveJobSeekerProfile(qual, skills, exp, roles, cities, sum, pref, isPub, phone)
            },
            onPublishJobVacancy = { title, comp, city, loc, role, salary, skills, desc, inst, email, phone ->
                viewModel.publishJobVacancy(title, comp, city, loc, role, salary, skills, desc, inst, email, phone)
            },
            onRegisterBloodDonor = { group, city, area, avail, lastDate, pref, isPub, phone ->
                viewModel.registerOrUpdateBloodDonor(group, city, area, avail, lastDate, pref, isPub, phone)
            },
            onCreateBloodRequest = { name, group, hosp, city, units, urg, phone, notes ->
                viewModel.createBloodRequest(name, group, hosp, city, units, urg, phone, notes)
            },
            onCreateCommunityAidRequest = { ben, cat, title, desc, city, loc, assist, amount, contactMethod ->
                viewModel.createCommunityAidRequest(ben, cat, title, desc, city, loc, assist, amount, contactMethod)
            },
            onMarkBloodRequestFulfilled = { viewModel.markBloodRequestFulfilled(it) },
            onMarkCommunityAidFulfilled = { viewModel.markCommunityAidFulfilled(it) },
            onCloseJobVacancy = { viewModel.closeJobVacancy(it) }
        )
    }
}

