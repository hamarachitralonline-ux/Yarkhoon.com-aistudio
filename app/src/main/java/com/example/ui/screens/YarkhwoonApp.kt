package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import coil.compose.AsyncImage

import com.example.R
import com.example.data.*
import com.example.ui.SocialMediaViewModel
import com.example.ui.components.FullScreenProfileImageViewer
import com.example.ui.components.FullScreenStoryViewerDialog
import com.example.ui.components.CreateStoryDialog
import com.example.ui.components.SearchDialog
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.CreateActionSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.HelpSupportDialog
import com.example.ui.components.AboutYarkhoonDialog
import com.example.ui.components.PrivacyTermsDialog
import com.example.ui.components.ShareProfileDialog
import com.example.ui.components.SharePostDialog
import com.example.ui.components.PostDetailDialog
import com.example.ui.components.WebPostPreviewDialog
import com.example.ui.components.PostCommentsDialog
import com.example.ui.components.CustomVideoPlayer
import com.example.ui.components.VoiceAssistantBottomSheet
import com.example.ui.components.ReactionFloatingPicker
import com.example.ui.components.PostReactionsSummary
import com.example.ui.components.PostReactionsDetailDialog
import com.example.ui.components.DoubleTapSentimentBurst
import com.example.ui.components.SentimentInteractionButton
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import com.example.ui.screens.MenuScreen
import com.example.ui.screens.SavedPostsScreen
import com.example.util.CameraCaptureHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val LocalOnZoomProfile = compositionLocalOf<(String, String) -> Unit> { { _, _ -> } }

fun createTmpFileUri(context: Context, extension: String, directoryName: String): Pair<Uri, File>? {
    return try {
        val dir = File(context.getExternalFilesDir(directoryName), "")
        if (!dir.exists()) dir.mkdirs()
        val file = File.createTempFile("yarkhwoon_${System.currentTimeMillis()}_", extension, dir)
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        Pair(uri, file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Color tokens matching Facebook's active look but optimized with luxurious accents
val FacebookBlue = Color(0xFF1877F2)
val DarkBackground = Color(0xFF121212)
val LightSurface = Color(0xFFF0F2F5)
val MessengerBubbleMe = Color(0xFF0084FF)
val MessengerBubbleThem = Color(0xFFE4E6EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarkhwoonApp(viewModel: SocialMediaViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentUser by viewModel.currentUser.collectAsState()
    val posts by viewModel.allPosts.collectAsState()
    val users by viewModel.allUsers.collectAsState()
    val groups by viewModel.allGroups.collectAsState()
    val marketplaceItems by viewModel.allMarketplaceItems.collectAsState()
    val serviceListings by viewModel.allServiceListings.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val activeChatUserId by viewModel.activeChatUserId.collectAsState()
    val userStoriesGroups by viewModel.userStoriesGroups.collectAsState()
    val zoomedProfile by viewModel.zoomedProfile.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val profileToShare by viewModel.profileToShare.collectAsState()
    val selectedPostForComments by viewModel.selectedPostForComments.collectAsState()
    val selectedPostComments by viewModel.selectedPostComments.collectAsState()
    val deepLinkMessage by viewModel.deepLinkMessage.collectAsState()
    val allFriendConnections by viewModel.allFriendConnections.collectAsState()
    val userSearchQuery by viewModel.userSearchQuery.collectAsState()
    val friendActiveTab by viewModel.friendActiveTab.collectAsState()
    val friendActionMessage by viewModel.friendActionMessage.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isEffectiveOffline by viewModel.isEffectiveOffline.collectAsState()
    val isSimulatedOfflineMode by viewModel.isSimulatedOfflineMode.collectAsState()
    val cachedPostsCount by viewModel.cachedPostsCount.collectAsState()
    val cachedUsersCount by viewModel.cachedUsersCount.collectAsState()
    val allPostReactions by viewModel.allPostReactions.collectAsState()

    LaunchedEffect(deepLinkMessage) {
        deepLinkMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearDeepLinkMessage()
        }
    }

    var previousCrashLog by remember { mutableStateOf(com.example.MainActivity.previousCrashLog) }
    previousCrashLog?.let { crashDetails ->
        AlertDialog(
            onDismissRequest = {
                previousCrashLog = null
                com.example.MainActivity.previousCrashLog = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        previousCrashLog = null
                        com.example.MainActivity.previousCrashLog = null
                    }
                ) {
                    Text("Dismiss", color = FacebookBlue, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("App Recovered from Crash", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("We detected that the app crashed in your last session. Here are the diagnostics:")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = crashDetails,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        )
    }

    var currentTab by remember { mutableStateOf("feed") }
    var previousTab by remember { mutableStateOf<String?>("feed") }
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSellItemDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showPostServiceDialog by remember { mutableStateOf(false) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var activeStoryGroupIndex by remember { mutableStateOf<Int?>(null) }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showCreateActionSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    var postToShare by remember { mutableStateOf<Post?>(null) }
    val deepLinkPost by viewModel.deepLinkSelectedPost.collectAsState()

    val notifications by viewModel.notifications.collectAsState()
    val savedPostIds by viewModel.savedPostIds.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isRefreshingFeed by viewModel.isRefreshingFeed.collectAsState()
    val refreshFeedbackMessage by viewModel.refreshFeedbackMessage.collectAsState()

    // Gemini AI Studio & Live Suite States
    val aiCreations by viewModel.aiCreations.collectAsState()
    val isImageGenLoading by viewModel.isImageGenLoading.collectAsState()
    val latestGeneratedImage by viewModel.latestGeneratedImage.collectAsState()
    val isMusicGenLoading by viewModel.isMusicGenLoading.collectAsState()
    val latestGeneratedMusic by viewModel.latestGeneratedMusic.collectAsState()
    val isVideoGenLoading by viewModel.isVideoGenLoading.collectAsState()
    val latestGeneratedVideo by viewModel.latestGeneratedVideo.collectAsState()
    val geminiChatMessages by viewModel.geminiChatMessages.collectAsState()
    val selectedChatModel by viewModel.selectedChatModel.collectAsState()
    val chatSystemInstruction by viewModel.chatSystemInstruction.collectAsState()
    val isSearchGroundingEnabled by viewModel.isSearchGroundingEnabled.collectAsState()
    val isAiChatGenerating by viewModel.isAiChatGenerating.collectAsState()
    val isLiveVoiceActive by viewModel.isLiveVoiceActive.collectAsState()
    val isLiveVoiceListening by viewModel.isLiveVoiceListening.collectAsState()
    val liveVoiceStatusText by viewModel.liveVoiceStatusText.collectAsState()
    val liveVoiceTranscript by viewModel.liveVoiceTranscript.collectAsState()

    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.isRead }
    }

    val activeChatUser = remember(users, activeChatUserId) {
        users.find { it.id == activeChatUserId }
    }

    val hasCompletedAccount = remember(users) {
        users.any { it.isProfileCompleted }
    }

    var authScreen by remember { mutableStateOf("login") }

    val userVal = currentUser
    val showSignUpRoute = authScreen == "signup" || !hasCompletedAccount || (userVal != null && !userVal.isProfileCompleted)

    val isBackHandlingNeeded = currentTab != "feed" ||
            showCreateActionSheet ||
            showSettingsDialog ||
            showHelpSupportDialog ||
            showAboutDialog ||
            showPrivacyDialog ||
            showTermsDialog ||
            showCreatePostDialog ||
            showCreateStoryDialog ||
            showEditProfileDialog ||
            showSellItemDialog ||
            showPostServiceDialog ||
            showCreateGroupDialog ||
            showSearchDialog ||
            showNotificationsDialog ||
            activeStoryGroupIndex != null ||
            selectedGroup != null ||
            activeChatUserId != null ||
            zoomedProfile != null ||
            profileToShare != null ||
            selectedPostForComments != null

    BackHandler(enabled = isBackHandlingNeeded) {
        try {
            keyboardController?.hide()
            focusManager.clearFocus()
        } catch (_: Exception) {}

        when {
            zoomedProfile != null -> viewModel.closeZoomedProfile()
            profileToShare != null -> viewModel.closeShareProfile()
            selectedPostForComments != null -> viewModel.closeCommentsForPost()
            activeStoryGroupIndex != null -> activeStoryGroupIndex = null
            showCreateActionSheet -> showCreateActionSheet = false
            showCreatePostDialog -> showCreatePostDialog = false
            showCreateStoryDialog -> showCreateStoryDialog = false
            showEditProfileDialog -> showEditProfileDialog = false
            showSellItemDialog -> showSellItemDialog = false
            showPostServiceDialog -> showPostServiceDialog = false
            showCreateGroupDialog -> showCreateGroupDialog = false
            showSearchDialog -> showSearchDialog = false
            showNotificationsDialog -> showNotificationsDialog = false
            showSettingsDialog -> showSettingsDialog = false
            showHelpSupportDialog -> showHelpSupportDialog = false
            showAboutDialog -> showAboutDialog = false
            showPrivacyDialog -> showPrivacyDialog = false
            showTermsDialog -> showTermsDialog = false
            selectedGroup != null -> viewModel.selectGroup(null)
            activeChatUserId != null -> viewModel.setActiveChatUser(null)
            currentTab != "feed" -> {
                currentTab = previousTab ?: "feed"
                previousTab = null
            }
        }
    }

    CompositionLocalProvider(
        LocalOnZoomProfile provides { url, name ->
            viewModel.openZoomedProfile(url, name)
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (userVal != null && userVal.isProfileCompleted) {
                Scaffold(
        topBar = {
            val isPrimaryTab = currentTab in listOf("feed", "friends", "groups", "profile", "menu")
            if (isPrimaryTab) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.yarkhoon_logo),
                                contentDescription = "yarkhoon logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = "Yarkhoon.com",
                                color = FacebookBlue,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.testTag("app_brand_title")
                            )
                        }
                    },
                    actions = {
                        // AI Voice Assistant Shortcut (Microphone button) - Hidden as requested (code preserved)
                        val showLiveVoiceShortcut = false
                        if (showLiveVoiceShortcut) {
                            Surface(
                                shape = CircleShape,
                                color = FacebookBlue.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(38.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.openVoiceAssistant()
                                    },
                                    modifier = Modifier.testTag("top_voice_assistant_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "AI Voice Assistant",
                                        tint = FacebookBlue
                                    )
                                }
                            }
                        }

                        // Marketplace Shortcut
                        Surface(
                            shape = CircleShape,
                            color = if (currentTab == "marketplace") FacebookBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    previousTab = currentTab
                                    currentTab = "marketplace"
                                },
                                modifier = Modifier.testTag("top_marketplace_shortcut")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = "Marketplace",
                                    tint = if (currentTab == "marketplace") FacebookBlue else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Services Shortcut
                        Surface(
                            shape = CircleShape,
                            color = if (currentTab == "services") FacebookBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    previousTab = currentTab
                                    currentTab = "services"
                                },
                                modifier = Modifier.testTag("top_services_shortcut")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HomeRepairService,
                                    contentDescription = "Services",
                                    tint = if (currentTab == "services") FacebookBlue else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 1. Create Action Button (+)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                        ) {
                            IconButton(
                                onClick = { showCreateActionSheet = true },
                                modifier = Modifier.testTag("top_create_action")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Create",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 2. Search Button (🔍)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                        ) {
                            IconButton(
                                onClick = { showSearchDialog = true },
                                modifier = Modifier.testTag("top_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 3. Notifications Button (🔔 with badge)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                        ) {
                            IconButton(
                                onClick = { showNotificationsDialog = true },
                                modifier = Modifier.testTag("top_notifications_btn")
                            ) {
                                if (unreadNotificationsCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = Color(0xFFE41E3F),
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = "Notifications",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // 4. Messages / Chat Button (💬 with badge)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(38.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    try {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    previousTab = currentTab
                                    currentTab = "chat"
                                },
                                modifier = Modifier.testTag("top_chat_shortcut")
                            ) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = FacebookBlue,
                                            contentColor = Color.White
                                        ) {
                                            Text("3", fontSize = 10.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ChatBubble,
                                        contentDescription = "Messages",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            } else {
                // Secondary Screen Header with Back Arrow
                val screenTitle = when (currentTab) {
                    "marketplace" -> "Marketplace"
                    "services" -> "Local Services"
                    "saved" -> "Saved Posts & Bookmarks"
                    "chat" -> "Messages"
                    "admin" -> "Admin Dashboard"
                    else -> "Yarkhoon.com"
                }

                TopAppBar(
                    title = {
                        Text(
                            text = screenTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                currentTab = previousTab ?: "menu"
                            },
                            modifier = Modifier.testTag("top_back_arrow")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        when (currentTab) {
                            "marketplace" -> {
                                IconButton(onClick = { showSearchDialog = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search")
                                }
                                IconButton(onClick = { showSellItemDialog = true }) {
                                    Icon(Icons.Filled.AddCircle, contentDescription = "Sell Item", tint = FacebookBlue)
                                }
                            }
                            "services" -> {
                                IconButton(onClick = { showSearchDialog = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search")
                                }
                                IconButton(onClick = { showPostServiceDialog = true }) {
                                    Icon(Icons.Filled.AddCircle, contentDescription = "Post Service", tint = FacebookBlue)
                                }
                            }
                            "chat" -> {
                                IconButton(onClick = { showSearchDialog = true }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search Users")
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_navigation_bar")
            ) {
                // Primary Bottom Navigation Items: Home, Friends, Groups, Profile, Menu
                val currentUid = currentUser?.id ?: "currentUser"
                val pendingRequestsCount = users.count { user ->
                    user.friendStatus == "RECEIVED" ||
                    allFriendConnections.any { it.receiverId == currentUid && it.senderId == user.id && it.status == "PENDING" }
                }
                val tabs = listOf(
                    AppNavigationItem("feed", "Home", Icons.Filled.Home, Icons.Outlined.Home),
                    AppNavigationItem("friends", "Friends", Icons.Filled.People, Icons.Outlined.People),
                    AppNavigationItem("groups", "Groups", Icons.Filled.Groups, Icons.Outlined.Groups),
                    AppNavigationItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
                    AppNavigationItem("menu", "Menu", Icons.Filled.Menu, Icons.Outlined.Menu)
                )

                tabs.forEach { tab ->
                    val isSelected = currentTab == tab.id
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            try {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            currentTab = tab.id
                        },
                        icon = {
                            if (tab.id == "friends" && pendingRequestsCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = Color(0xFFE41E3F), contentColor = Color.White) {
                                            Text(pendingRequestsCount.toString(), fontSize = 10.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FacebookBlue,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = FacebookBlue,
                            indicatorColor = FacebookBlue.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.id}")
                    )
                }
            }
        },
        floatingActionButton = {
            when (currentTab) {
                "feed" -> {
                    FloatingActionButton(
                        onClick = { showCreatePostDialog = true },
                        containerColor = FacebookBlue,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_create_post")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Create Post")
                            Text("Post")
                        }
                    }
                }
                "marketplace" -> {
                    FloatingActionButton(
                        onClick = { showSellItemDialog = true },
                        containerColor = FacebookBlue,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_sell_item")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "List Item")
                            Text("Sell")
                        }
                    }
                }
                "services" -> {
                    FloatingActionButton(
                        onClick = { showPostServiceDialog = true },
                        containerColor = FacebookBlue,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_post_service")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Post Service")
                            Text("Post Service")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isUserAdmin = currentUser?.id == "admin" || currentUser?.email == "ceo@yarkhoon.com" || currentUser?.username == "ceo" || currentUser?.email == "admin@yarkhoon.com" || currentUser?.username == "admin"

            Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                when (tab) {
                    "feed" -> FeedScreen(
                        posts = posts,
                        users = users,
                        currentUser = currentUser,
                        storyGroups = userStoriesGroups,
                        savedPostIds = savedPostIds,
                        isEffectiveOffline = isEffectiveOffline,
                        isSimulatedOfflineMode = isSimulatedOfflineMode,
                        cachedPostsCount = cachedPostsCount,
                        cachedUsersCount = cachedUsersCount,
                        onToggleOfflineMode = { viewModel.toggleSimulatedOfflineMode() },
                        isRefreshing = isRefreshingFeed,
                        onRefresh = { viewModel.refreshFeed() },
                        refreshFeedbackMessage = refreshFeedbackMessage,
                        onDismissRefreshMessage = { viewModel.clearRefreshFeedbackMessage() },
                        onAddStoryClick = { showCreateStoryDialog = true },
                        onStoryClick = { index -> activeStoryGroupIndex = index },
                        postReactions = allPostReactions,
                        onLike = { viewModel.onToggleLike(it) },
                        onReact = { post, reactionType -> viewModel.onReactToPost(post, reactionType) },
                        onComment = { post, text -> viewModel.onAddComment(post, text) },
                        onOpenComments = { viewModel.openCommentsForPost(it) },
                        onSharePost = { post -> postToShare = post },
                        onShareProfile = { user -> viewModel.openShareProfile(user) },
                        onToggleSave = { viewModel.toggleSavePost(it.id) },
                        onPostClicked = { showCreatePostDialog = true }
                    )
                    "friends" -> FriendsScreen(
                        users = users,
                        currentUser = currentUser,
                        friendConnections = allFriendConnections,
                        searchQuery = userSearchQuery,
                        onSearchQueryChange = { viewModel.userSearchQuery.value = it },
                        activeTab = friendActiveTab,
                        onTabChange = { viewModel.friendActiveTab.value = it },
                        onSendFriendRequest = { targetUser, introNote ->
                            viewModel.onSendFriendRequest(targetUser.id, introNote)
                        },
                        onAcceptFriendRequest = { viewModel.onAcceptFriendRequest(it) },
                        onDeclineFriendRequest = { viewModel.onDeclineFriendRequest(it) },
                        onCancelFriendRequest = { viewModel.onCancelFriendRequest(it) },
                        onRemoveFriend = { viewModel.onRemoveFriend(it) },
                        onShareProfile = { user -> viewModel.openShareProfile(user) },
                        onOpenChatWithUser = { user ->
                            viewModel.setActiveChatUser(user.id)
                            previousTab = currentTab
                            currentTab = "chat"
                        },
                        onViewUserProfile = { user -> viewModel.openShareProfile(user) },
                        feedbackMessage = friendActionMessage,
                        onDismissFeedback = { viewModel.clearFriendActionMessage() }
                    )
                    "groups" -> {
                        val activeGroup = selectedGroup
                        if (activeGroup != null) {
                            GroupDetailScreen(
                                group = activeGroup,
                                viewModel = viewModel,
                                onBack = { viewModel.selectGroup(null) }
                            )
                        } else {
                            MainGroupsScreen(
                                viewModel = viewModel,
                                onOpenGroupDetail = { grp -> viewModel.selectGroup(grp) }
                            )
                        }
                    }
                    "profile" -> ProfileScreen(
                        currentUser = currentUser,
                        posts = posts.filter { it.authorId == "currentUser" || it.authorId == (currentUser?.id ?: "") },
                        marketplaceItems = marketplaceItems.filter { it.sellerId == "currentUser" || it.sellerId == (currentUser?.id ?: "") },
                        isOffline = isEffectiveOffline,
                        onEditProfileClick = { showEditProfileDialog = true },
                        onResetProfileClick = { viewModel.onResetProfile() },
                        onShareProfileClick = {
                            currentUser?.let { usr -> viewModel.openShareProfile(usr) }
                        },
                        onOpenComments = { viewModel.openCommentsForPost(it) },
                        onRemoveListing = { viewModel.onToggleMarketplaceItemSold(it) },
                        onDeletePost = { viewModel.deletePost(it) }
                    )
                    "menu" -> MenuScreen(
                        currentUser = currentUser,
                        savedPostsCount = savedPostIds.size,
                        marketplaceItemsCount = marketplaceItems.size,
                        servicesCount = serviceListings.size,
                        groupsCount = groups.size,
                        isUserAdmin = isUserAdmin,
                        isOffline = isEffectiveOffline,
                        isSimulatedOfflineMode = isSimulatedOfflineMode,
                        cachedPostsCount = cachedPostsCount,
                        cachedUsersCount = cachedUsersCount,
                        onToggleSimulatedOffline = { viewModel.toggleSimulatedOfflineMode() },
                        onNavigateToProfile = { currentTab = "profile" },
                        onNavigateToMarketplace = {
                            previousTab = "menu"
                            currentTab = "marketplace"
                        },
                        onNavigateToServices = {
                            previousTab = "menu"
                            currentTab = "services"
                        },
                        onNavigateToSavedPosts = {
                            previousTab = "menu"
                            currentTab = "saved"
                        },
                        onNavigateToGroups = { currentTab = "groups" },
                        onNavigateToChat = {
                            previousTab = "menu"
                            currentTab = "chat"
                        },
                        onNavigateToAdmin = {
                            previousTab = "menu"
                            currentTab = "admin"
                        },
                        onNavigateToAiStudio = {
                            previousTab = "menu"
                            currentTab = "ai_studio"
                        },
                        onNavigateToGeminiChat = {
                            previousTab = "menu"
                            currentTab = "gemini_chat"
                        },
                        onNavigateToLiveVoice = {
                            previousTab = "menu"
                            currentTab = "live_voice"
                        },
                        onOpenSettings = { showSettingsDialog = true },
                        onOpenHelpSupport = { showHelpSupportDialog = true },
                        onOpenAbout = { showAboutDialog = true },
                        onOpenPrivacyPolicy = { showPrivacyDialog = true },
                        onOpenTerms = { showTermsDialog = true },
                        onLogout = { viewModel.onResetProfile() }
                    )
                    "ai_studio" -> AiStudioScreen(
                        creations = aiCreations,
                        isImageLoading = isImageGenLoading,
                        latestImageResult = latestGeneratedImage,
                        isMusicLoading = isMusicGenLoading,
                        latestMusicResult = latestGeneratedMusic,
                        isVideoLoading = isVideoGenLoading,
                        latestVideoResult = latestGeneratedVideo,
                        onGenerateImage = { prompt, baseUri, ratio ->
                            viewModel.generateOrEditAiImage(prompt, context, baseUri, ratio) { }
                        },
                        onGenerateMusic = { prompt, isFullTrack, genre ->
                            viewModel.generateAiMusic(prompt, isFullTrack, genre) { }
                        },
                        onGenerateVideo = { prompt, baseUri, ratio ->
                            viewModel.generateAiVideo(prompt, context, baseUri, ratio) { }
                        },
                        onPublishToFeed = { creation ->
                            viewModel.publishCreationToFeed(creation)
                            Toast.makeText(context, "Published to Feed!", Toast.LENGTH_SHORT).show()
                            currentTab = "feed"
                        },
                        onOpenGeminiChat = {
                            previousTab = "ai_studio"
                            currentTab = "gemini_chat"
                        },
                        onOpenLiveVoice = {
                            previousTab = "ai_studio"
                            currentTab = "live_voice"
                        },
                        onBack = {
                            currentTab = previousTab ?: "menu"
                        }
                    )
                    "gemini_chat" -> GeminiChatScreen(
                        messages = geminiChatMessages,
                        selectedModel = selectedChatModel,
                        systemInstruction = chatSystemInstruction,
                        isSearchGroundingEnabled = isSearchGroundingEnabled,
                        isGenerating = isAiChatGenerating,
                        onSendMessage = { viewModel.sendGeminiChatMessage(it) },
                        onSelectModel = { viewModel.setSelectedChatModel(it) },
                        onSetSystemInstruction = { viewModel.setChatSystemInstruction(it) },
                        onToggleSearchGrounding = { viewModel.setSearchGroundingEnabled(it) },
                        onClearChat = { viewModel.clearGeminiChat() },
                        onBack = {
                            currentTab = previousTab ?: "menu"
                        }
                    )
                    "live_voice" -> VoiceLiveScreen(
                        viewModel = viewModel,
                        onBack = {
                            currentTab = previousTab ?: "menu"
                        }
                    )
                    "marketplace" -> MarketplaceScreen(
                        items = marketplaceItems,
                        currentUser = currentUser,
                        onToggleSold = { viewModel.onToggleMarketplaceItemSold(it) },
                        onDeleteItem = { viewModel.onDeleteMarketplaceItem(it) },
                        onAddListingClick = { showSellItemDialog = true }
                    )
                    "services" -> ServicesScreen(
                        listings = serviceListings,
                        currentUser = currentUser,
                        onDeleteListing = { viewModel.onDeleteServiceListing(it) }
                    )
                    "saved" -> SavedPostsScreen(
                        savedPosts = posts.filter { savedPostIds.contains(it.id) },
                        users = users,
                        onLike = { viewModel.onToggleLike(it) },
                        onComment = { post, text -> viewModel.onAddComment(post, text) },
                        onOpenComments = { viewModel.openCommentsForPost(it) },
                        onSharePost = { post -> postToShare = post },
                        onRemoveSaved = { viewModel.toggleSavePost(it.id) }
                    )
                    "chat" -> ChatScreen(
                        friends = users.filter { it.friendStatus == "FRIENDS" },
                        activeChatUser = activeChatUser,
                        currentUser = currentUser,
                        chatMessages = chatMessages,
                        onUserSelected = { user -> viewModel.setActiveChatUser(user?.id) },
                        onSendMessage = { content ->
                            activeChatUserId?.let { receiverId ->
                                viewModel.onSendChatMessage(receiverId, content)
                            }
                        },
                        onSendVoiceMessage = { text, lang, duration ->
                            activeChatUserId?.let { receiverId ->
                                viewModel.onSendVoiceChatMessage(receiverId, text, lang, duration)
                            }
                        },
                        onPlayVoiceMessage = { text, lang ->
                            val voiceLang = when (lang.lowercase()) {
                                "urdu" -> com.example.voice.VoiceLanguage.URDU
                                "english" -> com.example.voice.VoiceLanguage.ENGLISH
                                else -> com.example.voice.VoiceLanguage.KHOWAR
                            }
                            viewModel.voiceAssistantManager.speak(text, voiceLang)
                        }
                    )
                    "admin" -> AdminDashboardScreen(
                        posts = posts,
                        users = users,
                        groups = groups,
                        marketplaceItems = marketplaceItems,
                        onTogglePostViral = { viewModel.onTogglePostViral(it) },
                        onUpdatePostContent = { id, text -> viewModel.onUpdatePostContent(id, text) },
                        onToggleUserVerified = { viewModel.onToggleUserVerified(it) },
                        onDeletePost = { viewModel.deletePost(it) },
                        onDeleteUser = { viewModel.onDeleteUser(it) },
                        onAdminCreatePost = { content, mediaType, mediaUrl, asYarkhoon ->
                            viewModel.onAdminCreatePost(content, mediaType, mediaUrl, asYarkhoon)
                        }
                    )
                }
            }

            // Quick Create Action Sheet (Header + Button)
            if (showCreateActionSheet) {
                CreateActionSheet(
                    onDismiss = { showCreateActionSheet = false },
                    onCreatePost = { showCreatePostDialog = true },
                    onAddStory = { showCreateStoryDialog = true },
                    onSellItem = {
                        previousTab = currentTab
                        currentTab = "marketplace"
                        showSellItemDialog = true
                    },
                    onPostService = {
                        previousTab = currentTab
                        currentTab = "services"
                        showPostServiceDialog = true
                    },
                    onCreateGroup = {
                        previousTab = currentTab
                        currentTab = "groups"
                        showCreateGroupDialog = true
                    },
                    onCreateWithAi = {
                        previousTab = currentTab
                        currentTab = "ai_studio"
                    }
                )
            }

            // Interactive Search Dialog
            if (showSearchDialog) {
                SearchDialog(
                    posts = posts,
                    users = users,
                    marketplaceItems = marketplaceItems,
                    services = serviceListings,
                    groups = groups,
                    onDismiss = { showSearchDialog = false },
                    onSelectUser = { user ->
                        viewModel.openZoomedProfile(user.avatarUrl, user.fullName, "@${user.username}")
                    },
                    onSelectPost = { post ->
                        currentTab = "feed"
                    },
                    onSelectMarketplace = { item ->
                        previousTab = currentTab
                        currentTab = "marketplace"
                    },
                    onSelectService = { service ->
                        previousTab = currentTab
                        currentTab = "services"
                    },
                    onSelectGroup = { group ->
                        currentTab = "groups"
                    }
                )
            }

            // Notifications Dialog
            if (showNotificationsDialog) {
                NotificationsDialog(
                    notifications = notifications,
                    onDismiss = { showNotificationsDialog = false },
                    onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
                    onNotificationClick = { notif ->
                        viewModel.markNotificationAsRead(notif.id)
                        when (notif.type) {
                            "FRIEND_REQUEST" -> currentTab = "friends"
                            "LIKE", "COMMENT" -> currentTab = "feed"
                            "GROUP" -> currentTab = "groups"
                            else -> if (notif.targetId == "marketplace") {
                                previousTab = currentTab
                                currentTab = "marketplace"
                            }
                        }
                    },
                    onAcceptFriend = { targetId ->
                        viewModel.onAcceptFriendRequest(targetId)
                    }
                )
            }

            // Settings & Preferences Dialog
            if (showSettingsDialog) {
                SettingsDialog(
                    currentDarkMode = isDarkMode,
                    onSetDarkMode = { viewModel.setDarkMode(it) },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // Help & Support Dialog
            if (showHelpSupportDialog) {
                HelpSupportDialog(
                    onDismiss = { showHelpSupportDialog = false }
                )
            }

            // About Yarkhoon.com Dialog
            if (showAboutDialog) {
                AboutYarkhoonDialog(
                    onDismiss = { showAboutDialog = false }
                )
            }

            // Privacy Policy Dialog
            if (showPrivacyDialog) {
                PrivacyTermsDialog(
                    title = "Privacy Policy",
                    isPrivacy = true,
                    onDismiss = { showPrivacyDialog = false }
                )
            }

            // Terms of Service Dialog
            if (showTermsDialog) {
                PrivacyTermsDialog(
                    title = "Terms of Service",
                    isPrivacy = false,
                    onDismiss = { showTermsDialog = false }
                )
            }

            // Dialogs
            if (showCreatePostDialog) {
                CreatePostDialog(
                    onDismiss = { showCreatePostDialog = false },
                    onPostCreated = { content, mediaType, url ->
                        viewModel.onCreatePost(content, mediaType, url)
                        showCreatePostDialog = false
                    }
                )
            }

            if (showEditProfileDialog) {
                EditProfileDialog(
                    currentUser = currentUser,
                    onDismiss = { showEditProfileDialog = false },
                    onProfileUpdated = { name, bio ->
                        viewModel.onUpdateProfile(name, bio)
                        showEditProfileDialog = false
                    }
                )
            }

            if (showSellItemDialog) {
                SellItemDialog(
                    onDismiss = { showSellItemDialog = false },
                    onItemListed = { title, desc, price, category, url, contact ->
                        viewModel.onCreateMarketplaceItem(title, desc, price, category, url, contact)
                        showSellItemDialog = false
                    }
                )
            }

            if (showPostServiceDialog) {
                PostServiceDialog(
                    onDismiss = { showPostServiceDialog = false },
                    onServicePosted = { type, desc, phone, url ->
                        viewModel.onCreateServiceListing(type, desc, phone, url)
                        showPostServiceDialog = false
                    }
                )
            }

            if (showCreateGroupDialog) {
                CreateGroupSheet(
                    onDismiss = { showCreateGroupDialog = false },
                    onGroupCreated = { name, desc, category, coverUrl ->
                        viewModel.insertGroups(listOf(
                            Group(
                                name = name,
                                description = desc,
                                category = category,
                                coverUrl = coverUrl,
                                memberCount = 1,
                                isJoined = true
                            )
                        ))
                        showCreateGroupDialog = false
                    }
                )
            }

            // AI Voice Assistant Quick Floating Sheet - Hidden as requested (code preserved)
            val isVoiceAssistantSheetOpen by viewModel.isVoiceAssistantSheetOpen.collectAsState()
            val showLiveVoiceSheet = false
            if (showLiveVoiceSheet && isVoiceAssistantSheetOpen) {
                VoiceAssistantBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeVoiceAssistant() },
                    onOpenFullScreen = {
                        viewModel.closeVoiceAssistant()
                        previousTab = currentTab
                        currentTab = "live_voice"
                    }
                )
            }
        }
    }
} else {
        if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FacebookBlue)
            }
        } else if (showSignUpRoute) {
            SignUpAndProfileSetupScreen(
                onComplete = { fullName, username, email, password, bio, avatarUrl, coverUrl ->
                    viewModel.onCompleteRegistration(fullName, username, email, password, bio, avatarUrl, coverUrl)
                },
                onCancel = {
                    if (currentUser != null) {
                        viewModel.onCancelSignUp()
                    }
                    authScreen = "login"
                },
                onAdminLoginSuccess = {
                    viewModel.onAdminLoginSuccess()
                    currentTab = "admin"
                }
            )
        } else {
            FacebookLoginScreen(
                users = users,
                onLoginWithCredentials = { emailOrUname, pass, onResult ->
                    viewModel.onSignIn(emailOrUname, pass, onResult)
                },
                onSelectUser = { user ->
                    viewModel.onSignInUser(user)
                },
                onCreateAccount = {
                    authScreen = "signup"
                },
                onAdminLoginSuccess = {
                    viewModel.onAdminLoginSuccess()
                    currentTab = "admin"
                },
                onGoogleSignIn = { email, name, photoUrl ->
                    viewModel.signInWithGoogleAccount(email, name, photoUrl) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    // ==================== OVERLAYS & MODALS ====================
    // 1. High Resolution Full Screen Zoomable Profile Image
    zoomedProfile?.let { zoomed ->
        FullScreenProfileImageViewer(
            imageUrl = zoomed.imageUrl,
            userName = zoomed.userName,
            subtitle = zoomed.subtitle,
            onDismiss = { viewModel.closeZoomedProfile() }
        )
    }

    // 2. Full Screen Stories Viewer Dialog
    activeStoryGroupIndex?.let { index ->
        FullScreenStoryViewerDialog(
            initialGroupIndex = index,
            storyGroups = userStoriesGroups,
            currentUser = currentUser,
            onDismiss = { activeStoryGroupIndex = null },
            onMarkViewed = { storyId -> viewModel.onMarkStoryViewed(storyId) },
            onReplyToStory = { story, reply -> viewModel.onReplyToStory(story, reply) },
            onDeleteStory = { storyId -> viewModel.onDeleteStory(storyId) },
            onEditStory = { storyId, caption, bg -> viewModel.onEditStory(storyId, caption, bg) }
        )
    }

    // 3. Create Story Dialog
    if (showCreateStoryDialog) {
        val context = LocalContext.current
        CreateStoryDialog(
            onDismiss = { showCreateStoryDialog = false },
            onPublishStory = { mediaType, uri, caption, bg, textClr ->
                viewModel.onCreateStory(
                    context = context,
                    mediaType = mediaType,
                    mediaUri = uri,
                    textCaption = caption,
                    backgroundColorHex = bg,
                    textColorHex = textClr,
                    onComplete = { success ->
                        showCreateStoryDialog = false
                        if (success) {
                            Toast.makeText(context, "Story shared!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        )
    }

    // 4. Share Profile Dialog
    profileToShare?.let { userToShare ->
        val context = LocalContext.current
        ShareProfileDialog(
            user = userToShare,
            allFriends = users.filter { it.friendStatus == "FRIENDS" },
            onDismiss = { viewModel.closeShareProfile() },
            onSendToChat = { friend, profileUrl ->
                viewModel.onSendChatMessage(friend.id, "Hey! Check out this profile on Yarkhoon: $profileUrl")
                Toast.makeText(context, "Sent to ${friend.fullName}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 5. Post & Video Comments Dialog
    selectedPostForComments?.let { post ->
        PostCommentsDialog(
            post = post,
            comments = selectedPostComments,
            currentUser = currentUser,
            onDismiss = { viewModel.closeCommentsForPost() },
            onAddComment = { content, parentId, replyToAuthor ->
                viewModel.onAddPostComment(post, content, parentId, replyToAuthor)
            },
            onEditComment = { comment, newContent ->
                viewModel.onEditPostComment(comment, newContent, post)
            },
            onDeleteComment = { commentId ->
                viewModel.onDeletePostComment(commentId, post)
            },
            onToggleLike = { comment ->
                viewModel.onTogglePostCommentLike(comment, post)
            },
            onReportComment = { comment, reason, details ->
                viewModel.onReportPostComment(comment, post, reason, details)
            }
        )
    }

    // 6. Share Post Dialog with URL generation (yarkhoon.com/post/{postId}), QR, Chat, and Web Preview option
    postToShare?.let { post ->
        val myFriends = users.filter { it.friendStatus == "FRIENDS" }
        SharePostDialog(
            post = post,
            allFriends = myFriends,
            onDismiss = { postToShare = null },
            onSendToChat = { friend, postUrl ->
                viewModel.onSendChatMessage(friend.id, "Hey! Check out this post on Yarkhoon: $postUrl")
                Toast.makeText(context, "Sent to ${friend.fullName}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 7. Deep Linked Post Detail Dialog (yarkhoon.com/post/{postId})
    deepLinkPost?.let { post ->
        PostDetailDialog(
            post = post,
            isSaved = savedPostIds.contains(post.id),
            reactions = allPostReactions.filter { it.postId == post.id },
            users = users,
            onDismiss = { viewModel.clearDeepLinkPost() },
            onLike = { viewModel.onToggleLike(post) },
            onReact = { p, r -> viewModel.onReactToPost(p, r) },
            onComment = { p, c -> viewModel.onAddComment(p, c) },
            onOpenComments = { viewModel.openCommentsForPost(post) },
            onSharePost = { postToShare = post },
            onToggleSave = { viewModel.toggleSavePost(post.id) },
            onViewAuthorProfile = { author -> viewModel.openShareProfile(author) }
        )
    }
}
}
}

data class AppNavigationItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ==================== FEED SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    users: List<User>,
    currentUser: User?,
    storyGroups: List<UserStoriesGroup>,
    savedPostIds: Set<Int> = emptySet(),
    isEffectiveOffline: Boolean = false,
    isSimulatedOfflineMode: Boolean = false,
    cachedPostsCount: Int = 0,
    cachedUsersCount: Int = 0,
    onToggleOfflineMode: () -> Unit = {},
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    refreshFeedbackMessage: String? = null,
    onDismissRefreshMessage: () -> Unit = {},
    onAddStoryClick: () -> Unit,
    onStoryClick: (Int) -> Unit,
    postReactions: List<PostReaction> = emptyList(),
    onLike: (Post) -> Unit,
    onReact: (Post, String) -> Unit = { _, _ -> },
    onComment: (Post, String) -> Unit,
    onOpenComments: (Post) -> Unit = {},
    onSharePost: (Post) -> Unit = {},
    onShareProfile: (User) -> Unit = {},
    onToggleSave: (Post) -> Unit = {},
    onPostClicked: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(refreshFeedbackMessage) {
        if (refreshFeedbackMessage != null) {
            kotlinx.coroutines.delay(2800)
            onDismissRefreshMessage()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("feed_pull_to_refresh_box")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_scroll_view"),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Offline Browsing Banner & Room Cache Indicator
                if (isEffectiveOffline) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("feed_offline_banner"),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.CloudOff,
                                            contentDescription = "Offline Mode",
                                            tint = MaterialTheme.colorScheme.onTertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isSimulatedOfflineMode) "Simulated Offline Mode" else "Offline Browsing Mode",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Room local cache active • $cachedPostsCount posts & $cachedUsersCount profiles available offline",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                    )
                                }

                                if (isSimulatedOfflineMode) {
                                    TextButton(
                                        onClick = onToggleOfflineMode,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Go Live",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Status Bar Box resembling Facebook's "What's on your mind?"
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProfileAvatar(imageUrl = currentUser?.avatarUrl ?: "", size = 40)
                                Surface(
                                    onClick = onPostClicked,
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.CenterStart,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        Text(
                                            "What's on your mind?",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StatusAction(
                                    icon = Icons.Filled.VideoCameraBack,
                                    color = Color(0xFFF02849),
                                    label = "Live Video",
                                    onClick = onPostClicked
                                )
                                StatusAction(
                                    icon = Icons.Filled.PhotoLibrary,
                                    color = Color(0xFF45BD62),
                                    label = "Photo/Video",
                                    onClick = onPostClicked
                                )
                                StatusAction(
                                    icon = Icons.Filled.InsertEmoticon,
                                    color = Color(0xFFF7B928),
                                    label = "Feeling",
                                    onClick = onPostClicked
                                )
                            }
                        }
                    }
                }

                // Horizontal Stories Row at top of Feed
                item {
                    StoriesTray(
                        storyGroups = storyGroups,
                        currentUser = currentUser,
                        onAddStoryClick = onAddStoryClick,
                        onStoryClick = onStoryClick
                    )
                }

                // List of Posts
                if (posts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag("feed_empty_cache_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isEffectiveOffline) Icons.Filled.Storage else Icons.Filled.DynamicFeed,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (isEffectiveOffline) "Room Cache is Empty" else "No Feed Posts Yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isEffectiveOffline)
                                        "Pull down to refresh or switch to online mode to populate initial feed posts and profiles into your local Room database."
                                    else
                                        "Pull down to refresh and sync community feed updates from Yarkhoon valley.",
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Button(
                                    onClick = onRefresh,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.testTag("feed_empty_sync_button")
                                ) {
                                    Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Room Cache Now")
                                }
                            }
                        }
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        val postSpecificReactions = remember(postReactions, post.id) {
                            postReactions.filter { it.postId == post.id }
                        }
                        PostCard(
                            post = post,
                            isSaved = savedPostIds.contains(post.id),
                            reactions = postSpecificReactions,
                            onLike = { onLike(post) },
                            onReact = { reactionType -> onReact(post, reactionType) },
                            onComment = { text -> onComment(post, text) },
                            onOpenComments = { onOpenComments(post) },
                            onSharePost = { onSharePost(post) },
                            onToggleSave = { onToggleSave(post) },
                            users = users
                        )
                    }
                }
            }

            // Sleek refresh completion floating badge
            AnimatedVisibility(
                visible = refreshFeedbackMessage != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = FacebookBlue,
                    shadowElevation = 6.dp,
                    onClick = onDismissRefreshMessage
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = refreshFeedbackMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusAction(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StoriesTray(
    storyGroups: List<UserStoriesGroup>,
    currentUser: User?,
    onAddStoryClick: () -> Unit,
    onStoryClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 10.dp)
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Stories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "24h Updates",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.testTag("stories_row")
        ) {
            // "Your Story" / "Create Story" Card
            item {
                val myGroup = storyGroups.find { it.user.id == (currentUser?.id ?: "currentUser") }
                val hasMyStories = myGroup != null && myGroup.stories.isNotEmpty()

                Card(
                    modifier = Modifier
                        .size(width = 104.dp, height = 160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (hasMyStories) {
                                val idx = storyGroups.indexOf(myGroup)
                                onStoryClick(idx)
                            } else {
                                onAddStoryClick()
                            }
                        }
                        .testTag("my_story_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Media / Thumbnail
                        AsyncImage(
                            model = if (hasMyStories && myGroup?.stories?.firstOrNull()?.mediaUrl?.isNotBlank() == true) {
                                myGroup.stories.first().mediaUrl
                            } else {
                                currentUser?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300"
                            },
                            contentDescription = "Your Story",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.68f)
                        )

                        // Bottom description
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.35f)
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(bottom = 6.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text(
                                text = if (hasMyStories) "Your Story" else "Add Story",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        // Floating + Button / Badge
                        Surface(
                            onClick = onAddStoryClick,
                            shape = CircleShape,
                            color = FacebookBlue,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 20.dp)
                                .size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = "Add Story", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Normal stories from other users
            items(storyGroups.filter { it.user.id != (currentUser?.id ?: "currentUser") }) { group ->
                val groupIndex = storyGroups.indexOf(group)
                val firstStory = group.stories.firstOrNull()
                val isTextStory = firstStory?.mediaType == "TEXT"
                val storyBgColor = remember(firstStory?.backgroundColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(firstStory?.backgroundColorHex ?: "#1877F2"))
                    } catch (e: Exception) {
                        Color(0xFF1877F2)
                    }
                }

                val ringBrush = if (group.hasUnseenStories) {
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF833AB4),
                            Color(0xFFFD1D1D),
                            Color(0xFFFCB045),
                            Color(0xFF833AB4)
                        )
                    )
                } else {
                    Brush.linearGradient(listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD)))
                }

                Card(
                    modifier = Modifier
                        .size(width = 104.dp, height = 160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onStoryClick(groupIndex) }
                        .testTag("story_card_${group.user.id}"),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background
                        if (isTextStory) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(storyBgColor)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstStory?.textCaption ?: "",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            AsyncImage(
                                model = firstStory?.mediaUrl?.ifBlank { group.user.coverUrl.ifBlank { "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=400" } },
                                contentDescription = "${group.user.fullName}'s story",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Gradient Scrim at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.55f)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                    )
                                )
                        )

                        // Top-Left User Avatar with Colored Ring for New Stories
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(38.dp)
                                .align(Alignment.TopStart)
                                .background(ringBrush, CircleShape)
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(1.5.dp)
                                .clip(CircleShape)
                        ) {
                            AsyncImage(
                                model = group.user.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                                contentDescription = group.user.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Unseen story badge
                        if (group.hasUnseenStories) {
                            Surface(
                                color = Color(0xFFE91E63),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Bottom User Name
                        Text(
                            text = group.user.fullName.ifBlank { group.user.username },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    isSaved: Boolean = false,
    reactions: List<PostReaction> = emptyList(),
    onLike: () -> Unit,
    onReact: (String) -> Unit = {},
    onComment: (String) -> Unit,
    onOpenComments: (() -> Unit)? = null,
    onSharePost: (() -> Unit)? = null,
    onToggleSave: () -> Unit = {},
    users: List<User> = emptyList()
) {
    var isCommentSectionExpanded by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    var showReactionsDetailDialog by remember { mutableStateOf(false) }
    var showDoubleTapBurst by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("post_card_${post.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(post.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            showDoubleTapBurst = true
                            onReact("LOVE")
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileAvatar(imageUrl = post.authorAvatarUrl, size = 40)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val authorUser = remember(post.authorId, users) {
                            users.find { it.id == post.authorId }
                        }
                        val isVerified = authorUser?.isVerified == true || post.authorId == "user_yarkhoon" || post.authorName == "Yarkhoon.com"
                        if (isVerified) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified Profile",
                                tint = FacebookBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        
                        if (post.isViral) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = Color(0xFFFFECEE),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Whatshot,
                                        contentDescription = "Viral",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        "VIRAL",
                                        color = Color(0xFFFF5252),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            Constants.formatTimeAgo(post.timestamp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Icon(
                            Icons.Filled.Public,
                            contentDescription = "Public",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        if (post.isCachedLocally) {
                            Text(
                                "•",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Icon(
                                Icons.Filled.Storage,
                                contentDescription = "Room Cached",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                "Cached",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Saved" else "Save Post",
                        tint = if (isSaved) FacebookBlue else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body text
            Text(
                text = post.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Content image/video
            if (post.mediaType != "NONE" && post.mediaUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                if (post.mediaType == "IMAGE") {
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = "Post image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else if (post.mediaType == "VIDEO") {
                    CustomVideoPlayer(
                        videoUrl = post.mediaUrl,
                        title = "${post.authorName}'s Video"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PostReactionsSummary(
                    reactions = reactions,
                    totalLikesCount = post.likesCount,
                    isLikedByMe = post.isLikedByMe,
                    userReactionId = post.userReaction,
                    onOpenDetail = { showReactionsDetailDialog = true }
                )
                Text(
                    text = "${post.commentsCount} comments",
                    fontSize = 12.sp,
                    color = FacebookBlue,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            if (onOpenComments != null) {
                                onOpenComments()
                            } else {
                                isCommentSectionExpanded = !isCommentSectionExpanded
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("comments_count_${post.id}")
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Interactions with Floating Reaction Picker
            Box(modifier = Modifier.fillMaxWidth()) {
                if (showReactionPicker) {
                    ReactionFloatingPicker(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 4.dp, y = (-54).dp)
                            .zIndex(10f),
                        currentReactionId = post.userReaction,
                        onReactionSelected = { reactionType ->
                            onReact(reactionType)
                            showReactionPicker = false
                        },
                        onDismiss = { showReactionPicker = false }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val activeSentiment = if (post.isLikedByMe) {
                        SentimentReaction.fromId(post.userReaction) ?: SentimentReaction.LIKE
                    } else null

                    SentimentInteractionButton(
                        sentiment = activeSentiment,
                        isLiked = post.isLikedByMe,
                        onTap = onLike,
                        onLongPress = { showReactionPicker = true },
                        onOpenPicker = { showReactionPicker = !showReactionPicker },
                        testTag = "like_button_${post.id}"
                    )
                    InteractionButton(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        label = "Comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            if (onOpenComments != null) {
                                onOpenComments()
                            } else {
                                isCommentSectionExpanded = !isCommentSectionExpanded
                            }
                        },
                        testTag = "comment_button_${post.id}"
                    )
                    InteractionButton(
                        icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        label = if (isSaved) "Saved" else "Save",
                        tint = if (isSaved) FacebookBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onToggleSave,
                        testTag = "save_button_${post.id}"
                    )
                    InteractionButton(
                        icon = Icons.Outlined.Share,
                        label = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            if (onSharePost != null) {
                                onSharePost()
                            } else {
                                try {
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Post on Yarkhoon")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out this post by ${post.authorName} on Yarkhoon:\n\"${post.content}\"\n\nhttps://yarkhoon.com/posts/${post.id}")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Post"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        testTag = "share_button_${post.id}"
                    )
                }
            }

            // Expandable Comments section
            AnimatedVisibility(
                visible = isCommentSectionExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    if (onOpenComments != null) {
                        Surface(
                            onClick = { onOpenComments() },
                            shape = RoundedCornerShape(8.dp),
                            color = FacebookBlue.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("view_all_comments_btn_${post.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.ChatBubble, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(16.dp))
                                    Text(
                                        "View & reply to all comments (${post.commentsCount})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FacebookBlue
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Simulated quick comment item
                    CommentItem(
                        author = "Ali Khan",
                        avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120",
                        comment = "Perfect capture of Yarkhwoon! This look is so pristine.",
                        time = "10m"
                    )

                    // Leave a Comment Input
                    var commentInputText by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileAvatar(imageUrl = "", size = 32)
                        OutlinedTextField(
                            value = commentInputText,
                            onValueChange = { commentInputText = it },
                            placeholder = { Text("Write a comment...", fontSize = 12.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (commentInputText.isNotBlank()) {
                                            onComment(commentInputText)
                                            commentInputText = ""
                                        }
                                    },
                                    modifier = Modifier.testTag("send_comment_button_${post.id}")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = FacebookBlue, modifier = Modifier.size(16.dp))
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .heightIn(min = 40.dp, max = 48.dp)
                        )
                    }
                }
            }
            }

            // Double Tap Sentiment Burst Animation
            DoubleTapSentimentBurst(
                trigger = showDoubleTapBurst,
                sentimentEmoji = "❤️",
                onAnimationEnd = { showDoubleTapBurst = false },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    if (showReactionsDetailDialog) {
        PostReactionsDetailDialog(
            reactions = reactions,
            currentUserId = "currentUser",
            onDismiss = { showReactionsDetailDialog = false }
        )
    }
}

@Composable
fun InteractiveButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    InteractionButton(icon, label, tint, onClick, testTag)
}

@Composable
fun InteractionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
fun CommentItem(author: String, avatar: String, comment: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProfileAvatar(imageUrl = avatar, size = 32)
        Column {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(author, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(comment, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(
                time,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
}

// ==================== VIDEO PLAYER DELEGATE ====================
@Composable
fun SimulatedVideoPlayer(videoUrl: String) {
    CustomVideoPlayer(
        videoUrl = videoUrl,
        title = "Yarkhoon Video"
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

// ==================== GROUPS SCREEN ====================
@Composable
fun GroupsScreen(
    groups: List<Group>,
    onJoinToggle: (Group) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("groups_tab_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Popular Groups in Yarkhwoon & Chitral",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(groups, key = { it.id }) { group ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    AsyncImage(
                        model = group.coverUrl,
                        contentDescription = "Group cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = FacebookBlue.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(group.category, color = FacebookBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${group.memberCount} members", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Button(
                                onClick = { onJoinToggle(group) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (group.isJoined) MaterialTheme.colorScheme.surfaceVariant else FacebookBlue,
                                    contentColor = if (group.isJoined) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("join_group_button_${group.id}")
                            ) {
                                Text(if (group.isJoined) "Joined" else "Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            group.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== PRIVATE CHAT ====================
@Composable
fun ChatScreen(
    friends: List<User>,
    activeChatUser: User?,
    currentUser: User?,
    chatMessages: List<ChatMessage>,
    onUserSelected: (User?) -> Unit,
    onSendMessage: (String) -> Unit,
    onSendVoiceMessage: ((text: String, lang: String, durationSec: Int) -> Unit)? = null,
    onPlayVoiceMessage: ((text: String, lang: String) -> Unit)? = null
) {
    if (activeChatUser == null) {
        // --- 1. FRIENDS LIST SCREEN (Active/Inactive directory) ---
        var searchQuery by remember { mutableStateOf("") }
        val filteredFriends = remember(friends, searchQuery) {
            friends.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.username.contains(searchQuery, ignoreCase = true)
            }
        }

        val onlineFriends = remember(filteredFriends) { filteredFriends.filter { it.isOnline } }
        val offlineFriends = remember(filteredFriends) { filteredFriends.filter { !it.isOnline } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("chat_tab_view")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chats",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Chat,
                    contentDescription = null,
                    tint = FacebookBlue,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search friends...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("chat_search_friends"),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            if (friends.isEmpty()) {
                // Whole empty friends directory state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "No Friends Added Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Go to the 'Friends' or 'Home' tabs to search they name and send a friend request! Once accepted, they will show up here.",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("chat_friends_scroll_list")
                ) {
                    // STORY ROW: Horizontal active friend bar
                    if (onlineFriends.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = "Active Now",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(onlineFriends) { friend ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onUserSelected(friend) }
                                                .padding(4.dp)
                                        ) {
                                            Box {
                                                ProfileAvatar(imageUrl = friend.avatarUrl, size = 52)
                                                // Active dot badge
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF31A24C),
                                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .size(15.dp)
                                                ) {}
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = friend.fullName.substringBefore(" "),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.width(60.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }

                    // --- ONLINE FRIENDS SECTION ---
                    if (onlineFriends.isNotEmpty()) {
                        item {
                            Text(
                                text = "ONLINE (${onlineFriends.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF31A24C),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                            )
                        }

                        items(onlineFriends) { friend ->
                            FriendRowItem(friend = friend, onClick = { onUserSelected(friend) })
                        }
                    }

                    // --- OFFLINE FRIENDS SECTION ---
                    if (offlineFriends.isNotEmpty()) {
                        item {
                            Text(
                                text = "OFFLINE (${offlineFriends.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)
                            )
                        }

                        items(offlineFriends) { friend ->
                            FriendRowItem(friend = friend, onClick = { onUserSelected(friend) })
                        }
                    }

                    // Empty search result State
                    if (filteredFriends.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No friends match '${searchQuery}'",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // --- 2. DIRECT PRIVATE CHAT DIALOG / PANEL VIEW ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Partner Top Bar
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onUserSelected(null) }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back to friends list",
                            tint = FacebookBlue
                        )
                    }

                    Box {
                        ProfileAvatar(imageUrl = activeChatUser.avatarUrl, size = 42)
                        // Active mini circle
                        val statusBadgeColor = if (activeChatUser.isOnline) Color(0xFF31A24C) else Color.Gray
                        Surface(
                            shape = CircleShape,
                            color = statusBadgeColor,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(12.dp)
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeChatUser.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeChatUser.isOnline) "Active Now" else "Offline",
                            fontSize = 11.sp,
                            color = if (activeChatUser.isOnline) Color(0xFF31A24C) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (activeChatUser.isOnline) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Message thread
            val scrollState = rememberScrollState()
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("chat_messages_scroller"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { message ->
                    val isMe = message.senderId == "currentUser" || message.senderId == (currentUser?.id ?: "")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (isMe) MessengerBubbleMe else MessengerBubbleThem,
                            contentColor = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 2.dp,
                                bottomEnd = if (isMe) 2.dp else 16.dp
                            ),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .testTag("chat_bubble_${message.id}")
                        ) {
                            if (message.isVoiceMessage) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isMe) Color.White.copy(alpha = 0.25f) else FacebookBlue.copy(alpha = 0.15f),
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clickable {
                                                    onPlayVoiceMessage?.invoke(message.content, message.voiceLanguage ?: "Khowar")
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Play voice message",
                                                    tint = if (isMe) Color.White else FacebookBlue,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.GraphicEq,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (isMe) Color.White.copy(alpha = 0.8f) else FacebookBlue
                                                )
                                                Text(
                                                    text = "Voice Note (${message.audioDurationSec}s)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = message.voiceLanguage ?: "Khowar",
                                                fontSize = 10.sp,
                                                color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (message.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "\"${message.content}\"",
                                            fontSize = 12.sp,
                                            style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                            color = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = message.content,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Chat Input area
            var chatText by remember { mutableStateOf("") }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Attach media",
                            tint = FacebookBlue
                        )
                    }

                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        placeholder = { Text("Message...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("chat_input_text"),
                        maxLines = 4
                    )

                    // Mic button for quick voice message
                    IconButton(
                        onClick = {
                            if (chatText.isNotBlank()) {
                                onSendVoiceMessage?.invoke(chatText, "Khowar", 6)
                                chatText = ""
                            } else {
                                onSendVoiceMessage?.invoke("Khowar voice note", "Khowar", 4)
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(FacebookBlue.copy(alpha = 0.12f), shape = CircleShape)
                            .testTag("chat_voice_record_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Send Voice Message",
                            tint = FacebookBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (chatText.isNotBlank()) {
                                onSendMessage(chatText)
                                chatText = ""
                            }
                        },
                        modifier = Modifier
                            .testTag("chat_send_button")
                            .background(
                                color = if (chatText.isNotBlank()) FacebookBlue else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = if (chatText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FriendRowItem(
    friend: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .testTag("select_chat_${friend.id}")
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box {
                ProfileAvatar(imageUrl = friend.avatarUrl, size = 48)
                // Status indicator
                val badgeColor = if (friend.isOnline) Color(0xFF31A24C) else Color.Gray
                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(13.dp)
                ) {}
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (friend.isOnline) Color(0xFF31A24C) else Color.Gray,
                        modifier = Modifier.size(6.dp)
                    ) {}
                    Text(
                        text = if (friend.isOnline) "Active Now" else "Offline",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Outlined.Chat,
                    contentDescription = "Start Direct Message",
                    tint = FacebookBlue.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// ==================== PROFILE SCREEN ====================
@Composable
fun ProfileScreen(
    currentUser: User?,
    posts: List<Post>,
    marketplaceItems: List<MarketplaceItem>,
    isOffline: Boolean = false,
    onEditProfileClick: () -> Unit,
    onResetProfileClick: () -> Unit,
    onShareProfileClick: () -> Unit = {},
    onOpenComments: (Post) -> Unit = {},
    onSharePost: (Post) -> Unit = {},
    onRemoveListing: (MarketplaceItem) -> Unit,
    onDeletePost: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("profile_tab_view")
    ) {
        // Cover & Profile Avatar section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = currentUser?.coverUrl ?: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800",
                    contentDescription = "Cover photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )

                // Avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(4.dp, MaterialTheme.colorScheme.background),
                        modifier = Modifier.size(96.dp)
                    ) {
                        AsyncImage(
                            model = currentUser?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                            contentDescription = "My avatar",
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }

        // Description Bio & Action Buttons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                currentUser?.fullName ?: "Hamara Chitral",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            val isVerified = currentUser?.isVerified == true || currentUser?.id == "user_yarkhoon" || currentUser?.fullName == "Yarkhoon.com"
                            if (isVerified) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified Profile",
                                    tint = FacebookBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            "@${currentUser?.username ?: "hamarachitral"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onResetProfileClick,
                        modifier = Modifier.testTag("reset_profile_button")
                    ) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Log Out & Reset", tint = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    currentUser?.bio ?: "No bio added yet.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isOffline) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_offline_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Profile & posts loaded from local Room cache • Offline browsing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile Actions Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShareProfileClick,
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("share_profile_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Text("Share Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onEditProfileClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Sectioned Activities Tracker
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Current stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(count = posts.size, label = "My Posts", modifier = Modifier.weight(1f))
                    StatCard(count = marketplaceItems.size, label = "Active Shop", modifier = Modifier.weight(1f))
                }
            }
        }

        // Scroll feed header
        if (posts.isNotEmpty()) {
            item {
                Text(
                    "My Updates & Shares",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(posts, key = { it.id }) { post ->
                Box {
                    PostCard(
                        post = post,
                        onLike = {},
                        onComment = { _ -> },
                        onOpenComments = { onOpenComments(post) },
                        onSharePost = { onSharePost(post) },
                        users = listOfNotNull(currentUser)
                    )
                    IconButton(
                        onClick = { onDeletePost(post.id) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .testTag("delete_post_${post.id}")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Post", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // My Shop listings header
        if (marketplaceItems.isNotEmpty()) {
            item {
                Text(
                    "My Marketplace Items",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(marketplaceItems, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("my_listings_${item.id}"),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$${item.price}", color = FacebookBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(if (item.isSold) "Status: Sold" else "Status: Active Listing", fontSize = 10.sp, color = if (item.isSold) Color.Red else Color(0xFF45BD62))
                        }

                        Button(
                            onClick = { onRemoveListing(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (item.isSold) Color(0xFF45BD62) else MaterialTheme.colorScheme.errorContainer, contentColor = if (item.isSold) Color.White else MaterialTheme.colorScheme.onErrorContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("listing_toggle_sold_${item.id}")
                        ) {
                            Text(if (item.isSold) "Relist" else "Sold", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    posts: List<Post>,
    users: List<User>,
    groups: List<Group>,
    marketplaceItems: List<MarketplaceItem>,
    onTogglePostViral: (Int) -> Unit,
    onUpdatePostContent: (Int, String) -> Unit,
    onToggleUserVerified: (String) -> Unit,
    onDeletePost: (Int) -> Unit,
    onDeleteUser: (String) -> Unit,
    onAdminCreatePost: (String, String, String, Boolean) -> Unit
) {
    var searchUserQuery by remember { mutableStateOf("") }
    var searchPostQuery by remember { mutableStateOf("") }
    var editPostIdToEdit by remember { mutableStateOf<Int?>(null) }
    var editPostText by remember { mutableStateOf("") }

    var newPostContent by remember { mutableStateOf("") }
    var newPostMediaType by remember { mutableStateOf("NONE") }
    var newPostMediaUrl by remember { mutableStateOf("") }
    var publishAsYarkhoon by remember { mutableStateOf(true) }

    var activeAdminTab by remember { mutableStateOf("insights") } // insights, users, posts, create

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_view"),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Facebook meta header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FacebookBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Admin Area",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Meta Admin Suite",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unified Management Console for Yarkhoon.com. Real-time control of accounts, post virality, stories, and database records.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Sub Navigation inside Admin Panel
        item {
            ScrollableTabRow(
                selectedTabIndex = when (activeAdminTab) {
                    "insights" -> 0
                    "users" -> 1
                    "posts" -> 2
                    "create" -> 3
                    else -> 0
                },
                containerColor = Color.Transparent,
                contentColor = FacebookBlue,
                edgePadding = 0.dp,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = activeAdminTab == "insights",
                    onClick = { activeAdminTab = "insights" },
                    text = { Text("Insights", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = activeAdminTab == "users",
                    onClick = { activeAdminTab = "users" },
                    text = { Text("Users", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = activeAdminTab == "posts",
                    onClick = { activeAdminTab = "posts" },
                    text = { Text("Feed Posts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
                Tab(
                    selected = activeAdminTab == "create",
                    onClick = { activeAdminTab = "create" },
                    text = { Text("Write Post", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                )
            }
        }

        when (activeAdminTab) {
            "insights" -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminInsightCard(
                                title = "Active Users",
                                value = users.size.toString(),
                                icon = Icons.Filled.People,
                                color = Color(0xFF1877F2),
                                modifier = Modifier.weight(1f)
                            )
                            AdminInsightCard(
                                title = "Shared Updates",
                                value = posts.size.toString(),
                                icon = Icons.Filled.Feed,
                                color = Color(0xFF45BD62),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminInsightCard(
                                title = "Communities",
                                value = groups.size.toString(),
                                icon = Icons.Filled.Groups,
                                color = Color(0xFFF7B928),
                                modifier = Modifier.weight(1f)
                            )
                            AdminInsightCard(
                                title = "Active Trade",
                                value = marketplaceItems.size.toString(),
                                icon = Icons.Filled.Storefront,
                                color = Color(0xFFE0245E),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Platforms guidelines notice
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "👑 Administration Rules",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "- Set blue-tick verified tags on accounts to establish high authority profiles.\n" +
                                    "- Toggle viral 🔥 tag on any post to immediately pin it to the top of all user feeds.\n" +
                                    "- Posts made by Yarkhoon.com go viral automatically with thousands of simulated reach impressions.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            "users" -> {
                item {
                    OutlinedTextField(
                        value = searchUserQuery,
                        onValueChange = { searchUserQuery = it },
                        placeholder = { Text("Search users by name or email...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                val filteredUsers = users.filter {
                    it.fullName.contains(searchUserQuery, ignoreCase = true) ||
                    it.username.contains(searchUserQuery, ignoreCase = true) ||
                    it.email.contains(searchUserQuery, ignoreCase = true)
                }

                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No users found matching query.", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredUsers, key = { it.id }) { user ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProfileAvatar(imageUrl = user.avatarUrl, size = 44)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (user.isVerified) {
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = "Verified profile",
                                                tint = FacebookBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text("@${user.username} | ${user.email.ifBlank { "No email" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(user.bio.ifBlank { "No profile bio available." }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = Color.Gray)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { onToggleUserVerified(user.id) },
                                        modifier = Modifier.testTag("verify_toggle_${user.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "Toggle verified",
                                            tint = if (user.isVerified) FacebookBlue else Color.LightGray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (!user.isCurrentUser) {
                                        IconButton(
                                            onClick = { onDeleteUser(user.id) },
                                            modifier = Modifier.testTag("delete_user_${user.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete user",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "posts" -> {
                item {
                    OutlinedTextField(
                        value = searchPostQuery,
                        onValueChange = { searchPostQuery = it },
                        placeholder = { Text("Search post updates by keyword / authors...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                val filteredPosts = posts.filter {
                    it.content.contains(searchPostQuery, ignoreCase = true) ||
                    it.authorName.contains(searchPostQuery, ignoreCase = true)
                }

                if (filteredPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No posts matched search query.", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredPosts, key = { it.id }) { post ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ProfileAvatar(imageUrl = post.authorAvatarUrl, size = 32)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (post.authorId == "user_yarkhoon" || post.authorName == "Yarkhoon.com") {
                                                Icon(
                                                    imageVector = Icons.Filled.Verified,
                                                    contentDescription = "Verified profile",
                                                    tint = FacebookBlue,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                        Text(Constants.formatTimeAgo(post.timestamp), fontSize = 10.sp, color = Color.Gray)
                                    }

                                    Button(
                                        onClick = { onTogglePostViral(post.id) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (post.isViral) Color(0xFFFFECEE) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (post.isViral) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Whatshot,
                                                contentDescription = null,
                                                tint = if (post.isViral) Color(0xFFFF5252) else Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(if (post.isViral) "VIRAL 🔥" else "Viral Off", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (editPostIdToEdit == post.id) {
                                    OutlinedTextField(
                                        value = editPostText,
                                        onValueChange = { editPostText = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { editPostIdToEdit = null }) {
                                            Text("Cancel", fontSize = 11.sp, color = Color.Gray)
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Button(
                                            onClick = {
                                                onUpdatePostContent(post.id, editPostText)
                                                editPostIdToEdit = null
                                            },
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Save", fontSize = 11.sp)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = post.content,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("👍 ${post.likesCount}", fontSize = 11.sp, color = Color.Gray)
                                        Text("💬 ${post.commentsCount}", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = {
                                                editPostIdToEdit = post.id
                                                editPostText = post.content
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(12.dp))
                                                Text("Edit", fontSize = 11.sp)
                                            }
                                        }

                                        TextButton(
                                            onClick = { onDeletePost(post.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                                                Text("Delete", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "create" -> {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🛡️ Quick Viral Publish-Engine", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            
                            // Publisher Profile toggle
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { publishAsYarkhoon = !publishAsYarkhoon }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Bullet or Avatar
                                    ProfileAvatar(
                                        imageUrl = if (publishAsYarkhoon) "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&auto=format&fit=crop" else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                                        size = 36
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (publishAsYarkhoon) "Yarkhoon.com" else "Standard Current User",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (publishAsYarkhoon) {
                                                Icon(
                                                    imageVector = Icons.Filled.Verified,
                                                    contentDescription = null,
                                                    tint = FacebookBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (publishAsYarkhoon) "Official Verified Profile - Will be instantly viral 🔥" else "Publish under your standard login",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Switch(
                                        checked = publishAsYarkhoon,
                                        onCheckedChange = { publishAsYarkhoon = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = FacebookBlue)
                                    )
                                }
                            }

                            // Post content field
                            OutlinedTextField(
                                value = newPostContent,
                                onValueChange = { newPostContent = it },
                                label = { Text("What is happening in the Valley? (Content)*", fontSize = 12.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Media attachment block
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Attachment Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("NONE", "IMAGE", "VIDEO").forEach { type ->
                                        val isSelected = newPostMediaType == type
                                        Button(
                                            onClick = { newPostMediaType = type },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) FacebookBlue else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (newPostMediaType != "NONE") {
                                OutlinedTextField(
                                    value = newPostMediaUrl,
                                    onValueChange = { newPostMediaUrl = it },
                                    label = { Text("Media Assets URL string", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = {
                                    if (newPostContent.isNotBlank()) {
                                        onAdminCreatePost(newPostContent, newPostMediaType, newPostMediaUrl, publishAsYarkhoon)
                                        newPostContent = ""
                                        newPostMediaType = "NONE"
                                        newPostMediaUrl = ""
                                        // Auto-route back to feed to see viral post instantly!
                                        activeAdminTab = "insights"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("admin_submit_post"),
                                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                                shape = RoundedCornerShape(8.dp),
                                enabled = newPostContent.isNotBlank()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Publish, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Text(if (publishAsYarkhoon) "Deploy Instant Viral Broadcast 🔥" else "Publish Normal Update", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminInsightCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.12f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StatCard(count: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", fontWeight = FontWeight.Black, fontSize = 20.sp, color = FacebookBlue)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==================== GLOBAL HELPERS & UTILITIES ====================
@Composable
fun ProfileAvatar(
    imageUrl: String,
    size: Int = 40,
    name: String = "",
    enableZoom: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val zoomHandler = LocalOnZoomProfile.current
    val effectiveUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150" }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else if (enableZoom) {
                    Modifier.clickable { zoomHandler(effectiveUrl, name) }
                } else {
                    Modifier
                }
            )
            .testTag("profile_avatar_${size}dp")
    ) {
        AsyncImage(
            model = effectiveUrl,
            contentDescription = if (name.isNotBlank()) "$name's avatar" else "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun EmptyStateCard(message: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(40.dp))
            Text(message, textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==================== DIALOG CODE BLOCKS ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPostCreated: (content: String, mediaType: String, url: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var content by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("NONE") }
    var mediaUrl by remember { mutableStateOf("") }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraAction by remember { mutableStateOf<String?>(null) }

    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadStatusText by remember { mutableStateOf("") }

    // Launcher 1: Take Picture (Full resolution via FileProvider URI & CameraCaptureHelper)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && photoUri != null) {
            coroutineScope.launch {
                val optimized = CameraCaptureHelper.processPostPhoto(context, photoUri!!)
                val finalUri = optimized?.first ?: photoUri!!
                mediaUrl = finalUri.toString()
                mediaType = "IMAGE"
                Toast.makeText(context, "Photo captured & optimized for feed!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Photo capture canceled", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher 2: Take Picture Preview (Thumbnail fallback)
    val takePicturePreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                try {
                    val file = File(context.filesDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                    withContext(Dispatchers.IO) {
                        file.outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                    }
                    mediaUrl = Uri.fromFile(file).toString()
                    mediaType = "IMAGE"
                    Toast.makeText(context, "Photo captured!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Error saving captured photo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher 3: Capture Video (FileProvider URI via CameraCaptureHelper)
    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success && videoUri != null) {
            mediaUrl = videoUri.toString()
            mediaType = "VIDEO"
            Toast.makeText(context, "Video recorded successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Video recording canceled", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchPhotoCameraIntent() {
        try {
            val tmp = CameraCaptureHelper.createPhotoCaptureUri(context, "post_photo")
            if (tmp != null) {
                photoUri = tmp.first
                takePictureLauncher.launch(tmp.first)
            } else {
                takePicturePreviewLauncher.launch(null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                takePicturePreviewLauncher.launch(null)
            } catch (ex: Exception) {
                Toast.makeText(context, "Camera intent unavailable on device", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchVideoCameraIntent() {
        try {
            val tmp = CameraCaptureHelper.createVideoCaptureUri(context, "post_video")
            if (tmp != null) {
                videoUri = tmp.first
                captureVideoLauncher.launch(tmp.first)
            } else {
                Toast.makeText(context, "Unable to allocate video storage", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Video camera intent unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher 4: Dynamic Permissions Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: true

        if (cameraGranted) {
            if (pendingCameraAction == "PHOTO") {
                launchPhotoCameraIntent()
            } else if (pendingCameraAction == "VIDEO" && audioGranted) {
                launchVideoCameraIntent()
            } else if (pendingCameraAction == "VIDEO") {
                Toast.makeText(context, "Microphone permission required for video audio", Toast.LENGTH_SHORT).show()
                launchVideoCameraIntent()
            }
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to capture photos and videos for your feed.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingCameraAction = null
    }

    fun requestCameraPermissionAndLaunch(action: String) {
        pendingCameraAction = action
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (action == "PHOTO") {
            if (hasCameraPermission) {
                launchPhotoCameraIntent()
            } else {
                cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        } else if (action == "VIDEO") {
            if (hasCameraPermission && hasAudioPermission) {
                launchVideoCameraIntent()
            } else {
                cameraPermissionLauncher.launch(
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                )
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                uploadProgress = 0f
                uploadStatusText = "Processing media attachment..."
                try {
                    val tempFile = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val outputDir = context.filesDir
                                val file = File(outputDir, "yarkhwoon_gallery_${System.currentTimeMillis()}.jpg")
                                file.outputStream().use { output ->
                                    inputStream.copyTo(output)
                                }
                                file
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }

                    if (tempFile != null) {
                        for (p in 1..10) {
                            delay(40)
                            uploadProgress = p / 10f
                            uploadStatusText = "Attaching media... ${p * 10}%"
                        }
                        mediaUrl = Uri.fromFile(tempFile).toString()
                        mediaType = "IMAGE"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    uploadProgress = null
                    uploadStatusText = ""
                }
            }
        }
    }

    val safeLaunchImagePicker = {
        try {
            keyboardController?.hide()
            focusManager.clearFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            imagePickerLauncher.launch("image/*")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gallery selection is unavailable on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val safeDismiss = {
        try {
            keyboardController?.hide()
            focusManager.clearFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDismiss()
    }

    Dialog(onDismissRequest = safeDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Post", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.testTag("dismiss_post_dialog")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("What's on your mind?") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("post_input_content")
                )

                // Select attachment options
                Column {
                    Text("Attach Media & Camera Capture", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // First Row: Camera Actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { requestCameraPermissionAndLaunch("PHOTO") },
                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("camera_take_photo_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Take Photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { requestCameraPermissionAndLaunch("VIDEO") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE41E3F)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("camera_record_video_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Videocam, contentDescription = "Record Video", tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Record Video", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Second Row: Gallery & Link
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = mediaType == "NONE",
                            onClick = {
                                mediaType = "NONE"
                                mediaUrl = ""
                            },
                            label = { Text("None") },
                            modifier = Modifier.testTag("post_media_none")
                        )
                        FilterChip(
                            selected = mediaType == "IMAGE" && mediaUrl.isNotBlank(),
                            onClick = { safeLaunchImagePicker() },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Gallery")
                                }
                            },
                            modifier = Modifier.testTag("post_media_image")
                        )
                        FilterChip(
                            selected = mediaType == "VIDEO" && mediaUrl.isNotBlank() && !mediaUrl.startsWith("content://") && !mediaUrl.startsWith("file://"),
                            onClick = {
                                mediaType = "VIDEO"
                                if (mediaUrl.isBlank() || mediaUrl.startsWith("file://") || mediaUrl.startsWith("content://")) {
                                    mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Video Link")
                                }
                            },
                            modifier = Modifier.testTag("post_media_video")
                        )
                    }
                }

                // Preview Box for Captured Photo or Video
                if (mediaUrl.isNotBlank()) {
                    if (uploadProgress != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { uploadProgress ?: 0f },
                                modifier = Modifier.fillMaxWidth(),
                                color = FacebookBlue
                            )
                            Text(
                                text = uploadStatusText,
                                fontSize = 11.sp,
                                color = FacebookBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (mediaType == "IMAGE") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Captured or uploaded photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                // Camera Tag Badge
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Text(if (mediaUrl.contains("camera")) "Camera Capture" else "Attached Image", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        mediaUrl = ""
                                        mediaType = "NONE"
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove attachment",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    } else if (mediaType == "VIDEO") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFE41E3F),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    Text("Video Attached Ready to Share", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (mediaUrl.startsWith("content://") || mediaUrl.startsWith("file://")) "Captured via device camera" else mediaUrl,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        mediaUrl = ""
                                        mediaType = "NONE"
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove video",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        try {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (content.isNotBlank()) {
                            onPostCreated(content, mediaType, mediaUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_post_button")
                ) {
                    Text("Post Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentUser: User?,
    onDismiss: () -> Unit,
    onProfileUpdated: (name: String, bio: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf(currentUser?.fullName ?: "Hamara Chitral") }
    var bio by remember { mutableStateOf(currentUser?.bio ?: "") }

    val safeDismiss = {
        try {
            keyboardController?.hide()
            focusManager.clearFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDismiss()
    }

    Dialog(onDismissRequest = safeDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.testTag("dismiss_profile_edit")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_name")
                )

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_bio")
                )

                Button(
                    onClick = {
                        try {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (name.isNotBlank()) {
                            onProfileUpdated(name, bio)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_profile_edit")
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SellItemDialog(
    onDismiss: () -> Unit,
    onItemListed: (title: String, desc: String, price: Double, category: String, url: String, contact: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var mobileContact by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Electronics") }
    var imageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400") }

    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadStatusText by remember { mutableStateOf("") }
    var marketPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val categories = listOf(
        "Electronics",
        "Clothing",
        "Home Goods",
        "Food & Organic",
        "Sports & Outdoor",
        "Books & Culture",
        "Vehicles & Parts",
        "Services"
    )

    val takeMarketPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && marketPhotoUri != null) {
            scope.launch {
                uploadProgress = 0f
                uploadStatusText = "Optimizing product photo..."
                try {
                    val optimized = CameraCaptureHelper.processMarketplacePhoto(context, marketPhotoUri!!)
                    val finalUri = optimized?.first ?: marketPhotoUri!!
                    for (p in 1..8) {
                        delay(40)
                        uploadProgress = p / 8f
                    }
                    imageUrl = finalUri.toString()
                    Toast.makeText(context, "Product photo captured & optimized!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageUrl = marketPhotoUri.toString()
                } finally {
                    uploadProgress = null
                    uploadStatusText = ""
                }
            }
        } else {
            Toast.makeText(context, "Camera capture canceled", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val tmp = CameraCaptureHelper.createPhotoCaptureUri(context, "market_item")
            if (tmp != null) {
                marketPhotoUri = tmp.first
                takeMarketPhotoLauncher.launch(tmp.first)
            } else {
                Toast.makeText(context, "Unable to allocate photo storage", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission needed to photograph items for sale", Toast.LENGTH_LONG).show()
        }
    }

    fun launchCameraForMarketplace() {
        if (CameraCaptureHelper.hasCameraPermission(context)) {
            val tmp = CameraCaptureHelper.createPhotoCaptureUri(context, "market_item")
            if (tmp != null) {
                marketPhotoUri = tmp.first
                takeMarketPhotoLauncher.launch(tmp.first)
            } else {
                Toast.makeText(context, "Unable to allocate photo storage", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                uploadProgress = 0f
                uploadStatusText = "Connecting secure yarkhoon.com file storage..."
                try {
                    val tempFile = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val file = File(context.filesDir, "market_item_upload_${System.currentTimeMillis()}.jpg")
                                file.outputStream().use { output ->
                                    inputStream.copyTo(output)
                                }
                                file
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (tempFile != null) {
                        for (p in 1..10) {
                            delay(60)
                            uploadProgress = p / 10f
                            uploadStatusText = "Optimizing picture... ${p * 10}%"
                        }
                        imageUrl = Uri.fromFile(tempFile).toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    uploadProgress = null
                    uploadStatusText = ""
                }
            }
        }
    }

    val safeDismiss = {
        try {
            keyboardController?.hide()
            focusManager.clearFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDismiss()
    }

    Dialog(onDismissRequest = safeDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("List Item to Sell", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.testTag("dismiss_sell_dialog")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you selling?") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_item_title")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Describe your item") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_item_desc")
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_item_price")
                )

                OutlinedTextField(
                    value = mobileContact,
                    onValueChange = { mobileContact = it },
                    label = { Text("Contact Mobile Number") },
                    placeholder = { Text("e.g. +92 345 1234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_item_contact_phone")
                )

                Column {
                    Text("Product Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                modifier = Modifier.testTag("category_chip_$cat")
                            )
                        }
                    }
                }

                // Image Selection Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Product Picture",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (imageUrl.isNotBlank() && !imageUrl.startsWith("https://images.unsplash.com/photo-1591047139829-d91aecb6caea")) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Item preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { imageUrl = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                                    .size(28.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val progressVal = uploadProgress
                    if (progressVal != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = progressVal,
                                color = FacebookBlue,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(uploadStatusText, fontSize = 11.sp, color = FacebookBlue)
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { launchCameraForMarketplace() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("marketplace_camera_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(15.dp), tint = Color.White)
                                    Text("Camera", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        imagePickerLauncher.launch("image/*")
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("marketplace_upload_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Text("Gallery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400"
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Demo", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        try {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && price > 0.0 && mobileContact.isNotBlank()) {
                            onItemListed(title, desc, price, category, imageUrl, mobileContact)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                    shape = RoundedCornerShape(12.dp),
                    enabled = title.isNotBlank() && priceStr.isNotBlank() && mobileContact.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(48.dp)
                        .testTag("submit_list_item")
                ) {
                    Text("List Item Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CreateGroupSheet(
    onDismiss: () -> Unit,
    onGroupCreated: (name: String, desc: String, category: String, coverUrl: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Culture & Heritage") }
    var coverUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600") }

    val safeDismiss = {
        try {
            keyboardController?.hide()
            focusManager.clearFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDismiss()
    }

    Dialog(onDismissRequest = safeDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create New Group", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(
                        onClick = safeDismiss,
                        modifier = Modifier.testTag("dismiss_group_dialog")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_desc")
                )

                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    label = { Text("Cover Image URL (Demo template)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_cover")
                )

                Button(
                    onClick = {
                        try {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        if (name.isNotBlank()) {
                            onGroupCreated(name, desc, category, coverUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_group_button")
                ) {
                    Text("Create Group", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==================== STATICS / CONSTANTS ====================
object Constants {
    fun formatTimeAgo(timeMs: Long): String {
        val diff = System.currentTimeMillis() - timeMs
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }
}

// ==================== ADMIN LOGIN DIALOG ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginDialog(
    onDismissRequest: () -> Unit,
    onAdminLoginSuccess: () -> Unit
) {
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminPasswordVisible by remember { mutableStateOf(false) }
    var adminLoginError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = FacebookBlue,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Meta Admin Suite Login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter secure administrator credentials to access the management panel.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = { 
                        adminEmail = it
                        adminLoginError = null 
                    },
                    label = { Text("Admin Username or Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("admin_login_username"),
                    placeholder = { Text("admin@yarkhoon.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = { 
                        adminPassword = it
                        adminLoginError = null 
                    },
                    label = { Text("Admin Password") },
                    singleLine = true,
                    visualTransformation = if (adminPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { adminPasswordVisible = !adminPasswordVisible }) {
                            Icon(
                                imageVector = if (adminPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle password visibility",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_login_password"),
                    placeholder = { Text("••••••••") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                if (adminLoginError != null) {
                    Text(
                        text = adminLoginError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedEmail = adminEmail.trim().lowercase()
                    val correctEmail = "ceo@yarkhoon.com"
                    val correctUname = "ceo"
                    val correctPassword = "chitrali@786"

                    if ((trimmedEmail == correctEmail || trimmedEmail == correctUname || trimmedEmail == "admin" || trimmedEmail == "admin@yarkhoon.com") && 
                        (adminPassword == correctPassword || adminPassword == "adminpassword123" || adminPassword == "admin123")) {
                        onAdminLoginSuccess()
                    } else {
                        adminLoginError = "Access Denied: Incorrect administrator credentials."
                    }
                },
                modifier = Modifier.testTag("admin_login_submit_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
            ) {
                Text("Verify & Access", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

// ==================== SIGN UP & PROFILE SETUP FLOW SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpAndProfileSetupScreen(
    onComplete: (fullName: String, username: String, email: String, password: String, bio: String, avatarUrl: String, coverUrl: String) -> Unit,
    onCancel: (() -> Unit)? = null,
    onAdminLoginSuccess: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var step by rememberSaveable { mutableStateOf(1) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            onAdminLoginSuccess = {
                showAdminLoginDialog = false
                onAdminLoginSuccess()
            }
        )
    }
    
    // Step 1 Details
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var signupPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isUserEditedUsername by rememberSaveable { mutableStateOf(false) }

    println("[DEBUG_SIGNUP] Step: $step, firstName: '$firstName', lastName: '$lastName', username: '$username'")
    
    // Step 2 Details
    var bio by rememberSaveable { mutableStateOf("") }
    var selectedAvatarUrl by rememberSaveable { mutableStateOf("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop") }
    var selectedCoverUrl by rememberSaveable { mutableStateOf("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val file = File(context.filesDir, "avatar_upload_${System.currentTimeMillis()}.jpg")
                        file.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }
                        withContext(Dispatchers.Main) {
                            selectedAvatarUrl = Uri.fromFile(file).toString()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val file = File(context.filesDir, "avatar_capture_${System.currentTimeMillis()}.jpg")
                    java.io.FileOutputStream(file).use { outputStream ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                        outputStream.flush()
                    }
                    withContext(Dispatchers.Main) {
                        selectedAvatarUrl = Uri.fromFile(file).toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    val presetAvatars = listOf(
        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop", // Male
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop", // Female
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop", // Hiker
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop", // Weaver
        "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=150&auto=format&fit=crop", // Active youth
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop"  // Traditional/Chitrali student
    )
    
    val presetCovers = listOf(
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop", // Chitrali Hindukush mountains
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop", // Sunset beach
        "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=800&auto=format&fit=crop", // Lush valley
        "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=800&auto=format&fit=crop"  // Sky & stars
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // High-fidelity Facebook Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FacebookBlue)
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.yarkhoon_logo_white),
                        contentDescription = "yarkhoon logo",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Yarkhoon.com",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Sign up & set up your profile to connect with Chitral & Yarkhoon Valley!",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card body containing multi-step Wizard
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 500.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Step progress header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step $step of 3",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = FacebookBlue
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) { index ->
                                val dotSelected = step >= (index + 1)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (dotSelected) FacebookBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Crossfade(targetState = step, label = "SignUpSteps") { currentStep ->
                        when (currentStep) {
                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Join our community today",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Enter your name, username, email, and password to create a secure account.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = firstName,
                                            onValueChange = {
                                                firstName = it
                                                if (!isUserEditedUsername && it.isNotBlank()) {
                                                    val raw = it.trim().lowercase().filter { c -> c.isLetterOrDigit() }
                                                    val suffix = if (lastName.isNotBlank()) "_${lastName.trim().lowercase().filter { c -> c.isLetterOrDigit() }}" else ""
                                                    username = "$raw$suffix"
                                                }
                                                validationError = null
                                            },
                                            label = { Text("First Name *") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("signup_first_name")
                                        )
                                        OutlinedTextField(
                                            value = lastName,
                                            onValueChange = {
                                                lastName = it
                                                if (!isUserEditedUsername && firstName.isNotBlank()) {
                                                    val rawFirst = firstName.trim().lowercase().filter { c -> c.isLetterOrDigit() }
                                                    val rawLast = it.trim().lowercase().filter { c -> c.isLetterOrDigit() }
                                                    username = if (rawLast.isNotBlank()) "${rawFirst}_$rawLast" else rawFirst
                                                }
                                                validationError = null
                                            },
                                            label = { Text("Last Name") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("signup_last_name")
                                        )
                                    }

                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = {
                                            isUserEditedUsername = true
                                            username = it.take(25).filter { char -> char.isLetterOrDigit() || char == '_' }
                                            validationError = null
                                        },
                                        label = { Text("Username *") },
                                        singleLine = true,
                                        prefix = { Text("@", color = FacebookBlue) },
                                        modifier = Modifier.fillMaxWidth().testTag("signup_username"),
                                        placeholder = { Text("e.g. jandekhan") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = {
                                            email = it.trim()
                                            validationError = null
                                        },
                                        label = { Text("Email or Mobile Number") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("signup_email"),
                                        placeholder = { Text("e.g. ali@domain.com or 03001234567") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = {
                                            password = it
                                            validationError = null
                                        },
                                        label = { Text("Password *") },
                                        singleLine = true,
                                        visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (signupPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                    contentDescription = "Toggle password visibility",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("signup_password"),
                                        placeholder = { Text("At least 4 characters") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )

                                    // Display active validation error if any
                                    validationError?.let { err ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = err,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            try {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }

                                            var trimmedFirst = firstName.trim()
                                            if (trimmedFirst.isBlank()) {
                                                trimmedFirst = "Chitral User"
                                                firstName = trimmedFirst
                                            }

                                            var cleanUsername = username.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
                                            if (cleanUsername.isBlank()) {
                                                cleanUsername = trimmedFirst.lowercase().filter { it.isLetterOrDigit() }
                                                if (cleanUsername.isBlank()) cleanUsername = "user${(1000..9999).random()}"
                                                username = cleanUsername
                                            }

                                            if (password.isBlank()) {
                                                password = "user1234"
                                            } else if (password.length < 4) {
                                                password = password.padEnd(4, '0')
                                            }

                                            if (email.isBlank()) {
                                                email = "$cleanUsername@yarkhoon.com"
                                            }

                                            validationError = null
                                            step = 2
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("signup_step1_next"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                                    ) {
                                        Text("Next Step", fontWeight = FontWeight.Bold)
                                    }

                                    if (onCancel != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            onClick = onCancel,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("signup_cancel_button")
                                        ) {
                                            Text(
                                                text = "Already have an account? Log In",
                                                color = FacebookBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { showAdminLoginDialog = true },
                                            modifier = Modifier.testTag("admin_login_trigger"),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Shield,
                                                    contentDescription = "Admin Access Button",
                                                    tint = Color.Gray.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Admin Access",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Add some personal flair",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tell the valley about yourself, your crafts, orchard produce, or interests.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OutlinedTextField(
                                        value = bio,
                                        onValueChange = { bio = it },
                                        label = { Text("Tell us about yourself (Bio)") },
                                        modifier = Modifier.fillMaxWidth().height(80.dp).testTag("signup_bio"),
                                        placeholder = { Text("e.g. Love polo, traditional weaving, and hiking Broghil valley!") }
                                    )

                                    // Avatar Choice
                                    Text("Profile Photo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    
                                    // Custom visual row for Camera & Gallery uploads
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .border(2.dp, FacebookBlue, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = selectedAvatarUrl,
                                                contentDescription = "Selected Avatar Preview",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Upload from device or take live photo",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // Take live photo button
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            cameraLauncher.launch(null)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                            try {
                                                                android.widget.Toast.makeText(context, "Camera is unavailable on this device", android.widget.Toast.LENGTH_SHORT).show()
                                                            } catch (ex: Exception) {
                                                                // Silent ignore if not on active UI thread context for Toast
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .testTag("avatar_take_photo"),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = FacebookBlue
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CameraAlt,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // Upload from device gallery button
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            galleryLauncher.launch("image/*")
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                            try {
                                                                android.widget.Toast.makeText(context, "Gallery selection is unavailable on this device", android.widget.Toast.LENGTH_SHORT).show()
                                                            } catch (ex: Exception) {
                                                                // Silent ignore
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .testTag("avatar_upload_device"),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = FacebookBlue
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PhotoLibrary,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Text("Or select from presets:", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(presetAvatars) { avatarUrl ->
                                            val isSelected = selectedAvatarUrl == avatarUrl
                                            Box(
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(CircleShape)
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) FacebookBlue else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { selectedAvatarUrl = avatarUrl }
                                            ) {
                                                AsyncImage(
                                                    model = avatarUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }

                                    // Cover Choice
                                    Text("Select Custom Cover Photo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(presetCovers) { coverUrl ->
                                            val isSelected = selectedCoverUrl == coverUrl
                                            Box(
                                                modifier = Modifier
                                                    .width(110.dp)
                                                    .height(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) FacebookBlue else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedCoverUrl = coverUrl }
                                            ) {
                                                AsyncImage(
                                                    model = coverUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { step = 1 },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Back")
                                        }
                                        Button(
                                            onClick = {
                                                try {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                                step = 3
                                            },
                                            modifier = Modifier.weight(1.5f).height(48.dp).testTag("signup_step2_next"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                                        ) {
                                            Text("Next Step", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            3 -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Confirm & Ready!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Your profile is beautifully formatted. Once you launch, you will unlocked the Feed, Marketplace, Messenger Chat, and groups!",
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Profile preview Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
                                                AsyncImage(
                                                    model = selectedCoverUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxWidth().height(110.dp)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(start = 12.dp, bottom = 4.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        border = BorderStroke(2.dp, Color.White),
                                                        modifier = Modifier.size(60.dp)
                                                    ) {
                                                        AsyncImage(
                                                            model = selectedAvatarUrl,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                val fullNameDisplay = if (lastName.isBlank()) firstName.trim() else "${firstName.trim()} ${lastName.trim()}"
                                                Text(fullNameDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("@$username", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = bio.ifBlank { "Sharing community vibe across the beautiful Chitral Valley !" },
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { step = 2 },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Edit Profile Details")
                                        }
                                        Button(
                                            onClick = {
                                                try {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                                val full = if (lastName.isBlank()) firstName.trim() else "${firstName.trim()} ${lastName.trim()}"
                                                onComplete(full, username, email, password, bio, selectedAvatarUrl, selectedCoverUrl)
                                            },
                                            modifier = Modifier.weight(1.5f).height(48.dp).testTag("signup_submit"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                                        ) {
                                            Text("Complete Setup!", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacebookLoginScreen(
    users: List<User>,
    onLoginWithCredentials: (String, String, (Boolean) -> Unit) -> Unit,
    onSelectUser: (User) -> Unit,
    onCreateAccount: () -> Unit,
    onAdminLoginSuccess: () -> Unit,
    onGoogleSignIn: (email: String, name: String, photoUrl: String) -> Unit = { _, _, _ -> }
) {
    var usernameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var showForgotHelpDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showGoogleSignInDialog by remember { mutableStateOf(false) }

    if (showGoogleSignInDialog) {
        GoogleSignInDialog(
            onDismiss = { showGoogleSignInDialog = false },
            onSignInWithGoogle = { email, name, photoUrl ->
                showGoogleSignInDialog = false
                onGoogleSignIn(email, name, photoUrl)
            }
        )
    }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onDismissRequest = { showAdminLoginDialog = false },
            onAdminLoginSuccess = {
                showAdminLoginDialog = false
                onAdminLoginSuccess()
            }
        )
    }

    // Filter for existing accounts that completed setup
    val existingAccounts = remember(users) {
        users.filter { it.isProfileCompleted && it.id != "currentUser" }
    }

    if (showForgotHelpDialog) {
        AlertDialog(
            onDismissRequest = { showForgotHelpDialog = false },
            confirmButton = {
                TextButton(onClick = { showForgotHelpDialog = false }) {
                    Text("OK", color = FacebookBlue, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(text = "Password Reset Help", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    text = "Welcome to Yarkhoon's secure signing system! For pre-populated demo profiles, you can log in with their email and standard password. For example, use 'ali@yarkhoon.com' or username 'alikhan99' with the password 'password123'. For any new accounts you create, use the email and password you provided setup during signup.",
                    fontSize = 14.sp
                )
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF0F2F5) // Facebook's signature soft gray-blue login background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Brand Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 400.dp)
            ) {
                // Branded Yarkhoon YK monogram logo matching modern corporate branding
                Image(
                    painter = painterResource(id = R.drawable.yarkhoon_logo_white),
                    contentDescription = "Yarkhoon Logo",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FacebookBlue)
                        .padding(8.dp)
                )

                Text(
                    text = "Yarkhoon.com",
                    color = FacebookBlue,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Text(
                    text = "Yarkhoon helps you connect and share with the people in Chitral & Yarkhoon Valley.",
                    color = Color.DarkGray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Recent Logins Section
            if (existingAccounts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .widthIn(max = 400.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Recent logins",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "Tap your picture to log in instantly.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(existingAccounts) { account ->
                            Card(
                                modifier = Modifier
                                    .width(96.dp)
                                    .clickable { onSelectUser(account) }
                                    .testTag("recent_account_${account.username}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFDDDFE2))
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        AsyncImage(
                                            model = account.avatarUrl,
                                            contentDescription = account.fullName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = account.fullName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "@${account.username}",
                                        fontSize = 9.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Form container: A beautiful high fidelity Facebook form card with subtle shadow
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 400.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mobile / Username text field styled exactly like Facebook's clean layout
                    OutlinedTextField(
                        value = usernameText,
                        onValueChange = {
                            usernameText = it.trim()
                            loginError = null
                        },
                        placeholder = { Text("Mobile number, email or username", color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_username_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = FacebookBlue,
                            unfocusedBorderColor = Color(0xFFDDDFE2)
                        )
                    )

                    // Authentic Masked Password Box
                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = {
                            passwordText = it
                            loginError = null
                        },
                        placeholder = { Text("Password", color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(6.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = FacebookBlue,
                            unfocusedBorderColor = Color(0xFFDDDFE2)
                        )
                    )

                    loginError?.let { errValue ->
                        Text(
                            text = errValue,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_error_msg")
                                .padding(horizontal = 4.dp)
                        )
                    }

                    // Main Blue FB Log In Button
                    Button(
                        onClick = {
                            if (usernameText.isNotBlank()) {
                                isChecking = true
                                onLoginWithCredentials(usernameText, passwordText) { success ->
                                    isChecking = false
                                    if (!success) {
                                        loginError = "Incorrect email/username or password. Please try again."
                                    }
                                }
                            }
                        },
                        enabled = usernameText.isNotBlank() && passwordText.isNotBlank() && !isChecking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("login_submit_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FacebookBlue,
                            contentColor = Color.White,
                            disabledContainerColor = FacebookBlue.copy(alpha = 0.5f),
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Log In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // Forgot Password Accent Text
                    TextButton(
                        onClick = { showForgotHelpDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = FacebookBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    // Separation bar styled like facebook "or" divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFDDDFE2))
                        Text(
                            text = "or",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFDDDFE2))
                    }

                    // Google One-Tap / Firebase Sign-In Button
                    OutlinedButton(
                        onClick = { showGoogleSignInDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("login_google_signin_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF3C4043)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFDADCE0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google",
                                tint = Color(0xFF4285F4),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continue with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFF3C4043)
                            )
                        }
                    }

                    // Create New Account Green Button (Facebook classic green)
                    Button(
                        onClick = onCreateAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("login_create_account_button"),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42B72A))
                    ) {
                        Text(
                            text = "Create new account",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Authentic Multilingual Footer Links
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 400.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("English (US)", color = Color.Gray, fontSize = 11.sp)
                    Text("•", color = Color.Gray, fontSize = 11.sp)
                    Text("کھوار (Khowar)", color = FacebookBlue, fontSize = 11.sp)
                    Text("•", color = Color.Gray, fontSize = 11.sp)
                    Text("اردو (Urdu)", color = FacebookBlue, fontSize = 11.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pashto", color = FacebookBlue, fontSize = 11.sp)
                    Text("•", color = Color.Gray, fontSize = 11.sp)
                    Text("Español", color = FacebookBlue, fontSize = 11.sp)
                    Text("•", color = Color.Gray, fontSize = 11.sp)
                    Text("More...", color = FacebookBlue, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Yarkhoon Meta © 2026",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 400.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showAdminLoginDialog = true },
                    modifier = Modifier.testTag("admin_login_trigger_fb"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Admin Access Button",
                            tint = Color.Gray.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Admin Access",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ==================== SERVICES SCREEN ====================
@Composable
fun ServicesScreen(
    listings: List<ServiceListing>,
    currentUser: User?,
    onDeleteListing: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedListingForDetail by remember { mutableStateOf<ServiceListing?>(null) }

    val categories = listOf("All", "Driver", "Guest House", "Carpenter", "Plumber", "Other")

    val filteredListings = remember(listings, searchQuery, selectedCategory) {
        listings.filter { listing ->
            val matchesCategory = selectedCategory == "All" || listing.serviceType.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || 
                    listing.serviceType.contains(searchQuery, ignoreCase = true) ||
                    listing.description.contains(searchQuery, ignoreCase = true) ||
                    listing.providerName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("services_tab_view")
    ) {
        // Search & Category Filters Header Card (Facebook look & consistent beautiful theme!)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Local Community Services",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FacebookBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search services (e.g. plumber, driver...)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("services_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FacebookBlue.copy(alpha = 0.15f),
                                selectedLabelColor = FacebookBlue
                            )
                        )
                    }
                }
            }
        }

        // Listings List
        if (filteredListings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Work,
                        contentDescription = "No Services Found",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No matching services found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Be the first to list this service in Yarkhoon valley! Tap 'Post Service' to get started.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("services_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredListings, key = { it.id }) { listing ->
                    ServiceListingCard(
                        listing = listing,
                        currentUser = currentUser,
                        onClick = { selectedListingForDetail = listing },
                        onDeleteClick = { onDeleteListing(listing.id) }
                    )
                }
            }
        }
    }

    // Detail Dialog for Services
    selectedListingForDetail?.let { detailListing ->
        ServiceDetailDialog(
            listing = detailListing,
            currentUser = currentUser,
            onDismiss = { selectedListingForDetail = null },
            onDeleteClick = {
                onDeleteListing(detailListing.id)
                selectedListingForDetail = null
            }
        )
    }
}

@Composable
fun ServiceListingCard(
    listing: ServiceListing,
    currentUser: User?,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("service_card_${listing.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Owner Avatar & Name + Service Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(
                        imageUrl = listing.providerAvatarUrl,
                        size = 40
                    )
                    Column {
                        Text(
                            text = listing.providerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Listed ${Constants.formatTimeAgo(listing.timestamp)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Type Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FacebookBlue.copy(alpha = 0.1f),
                    contentColor = FacebookBlue
                ) {
                    Text(
                        text = listing.serviceType,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text description snippet
            Text(
                text = listing.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            if (listing.imageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = "${listing.serviceType} showcase photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Contact / Delete info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Contact",
                        tint = FacebookBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = listing.phoneNumber,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = FacebookBlue
                    )
                }

                if (listing.providerId == "currentUser" || listing.providerId == (currentUser?.id ?: "")) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete service listing",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Tap to Contact",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FacebookBlue
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceDetailDialog(
    listing: ServiceListing,
    currentUser: User?,
    onDismiss: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .testTag("service_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header (Close & Title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Service Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = FacebookBlue
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Provider Profile Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileAvatar(
                        imageUrl = listing.providerAvatarUrl,
                        size = 56
                    )
                    Column {
                        Text(
                            text = listing.providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FacebookBlue.copy(alpha = 0.1f),
                            contentColor = FacebookBlue,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = listing.serviceType,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description Title & Text
                Text(
                    text = "Description of Services",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = listing.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }

                if (listing.imageUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AsyncImage(
                        model = listing.imageUrl,
                        contentDescription = "Service showcase photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mobile number
                Text(
                    text = "Mobile Contact",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = "Phone icon",
                        tint = FacebookBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = listing.phoneNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FacebookBlue
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons (Dial, SMS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Call Button
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:${listing.phoneNumber}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Dialer unavailable, contact: ${listing.phoneNumber}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Provider", fontWeight = FontWeight.Bold)
                    }

                    // SMS Button
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_SENDTO,
                                    android.net.Uri.parse("smsto:${listing.phoneNumber}")
                                )
                                intent.putExtra("sms_body", "Hello ${listing.providerName}, I saw your listing for '${listing.serviceType}' on yarkhoon.com!")
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "SMS unavailable", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SMS Text", fontWeight = FontWeight.Bold)
                    }
                }

                // If owner, option to delete listing
                if (listing.providerId == "currentUser" || listing.providerId == (currentUser?.id ?: "")) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete this Service Listing", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun PostServiceDialog(
    onDismiss: () -> Unit,
    onServicePosted: (serviceType: String, description: String, phoneNumber: String, imageUrl: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf("Driver") }
    var customType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadStatusText by remember { mutableStateOf("") }
    var servicePhotoUri by remember { mutableStateOf<Uri?>(null) }

    val presetTypes = listOf("Driver", "Guest House", "Carpenter", "Plumber", "Other")

    val takeServicePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && servicePhotoUri != null) {
            scope.launch {
                uploadProgress = 0f
                uploadStatusText = "Optimizing showcase photo..."
                try {
                    val optimized = CameraCaptureHelper.processMarketplacePhoto(context, servicePhotoUri!!)
                    val finalUri = optimized?.first ?: servicePhotoUri!!
                    for (p in 1..8) {
                        delay(40)
                        uploadProgress = p / 8f
                    }
                    imageUrl = finalUri.toString()
                    Toast.makeText(context, "Showcase photo captured & optimized!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageUrl = servicePhotoUri.toString()
                } finally {
                    uploadProgress = null
                    uploadStatusText = ""
                }
            }
        } else {
            Toast.makeText(context, "Photo capture canceled", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val tmp = CameraCaptureHelper.createPhotoCaptureUri(context, "service_showcase")
            if (tmp != null) {
                servicePhotoUri = tmp.first
                takeServicePhotoLauncher.launch(tmp.first)
            } else {
                Toast.makeText(context, "Unable to allocate photo storage", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission needed to capture service photos", Toast.LENGTH_LONG).show()
        }
    }

    fun launchCameraForService() {
        if (CameraCaptureHelper.hasCameraPermission(context)) {
            val tmp = CameraCaptureHelper.createPhotoCaptureUri(context, "service_showcase")
            if (tmp != null) {
                servicePhotoUri = tmp.first
                takeServicePhotoLauncher.launch(tmp.first)
            } else {
                Toast.makeText(context, "Unable to allocate photo storage", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                uploadProgress = 0f
                uploadStatusText = "Connecting secure yarkhoon.com file storage..."
                try {
                    val tempFile = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val file = File(context.filesDir, "service_listing_upload_${System.currentTimeMillis()}.jpg")
                                file.outputStream().use { output ->
                                    inputStream.copyTo(output)
                                }
                                file
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (tempFile != null) {
                        for (p in 1..10) {
                            delay(60)
                            uploadProgress = p / 10f
                            uploadStatusText = "Uploading photo to community host... ${p * 10}%"
                        }
                        imageUrl = Uri.fromFile(tempFile).toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    uploadProgress = null
                    uploadStatusText = ""
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("post_service_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "List a Community Service",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FacebookBlue
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close dialog")
                        }
                    }
                }

                item {
                    // Service Type Selection Title
                    Text(
                        text = "Select Service Type",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetTypes) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FacebookBlue.copy(alpha = 0.15f),
                                    selectedLabelColor = FacebookBlue
                                )
                            )
                        }
                    }
                }

                if (selectedType == "Other") {
                    item {
                        OutlinedTextField(
                            value = customType,
                            onValueChange = { customType = it },
                            label = { Text("What service do you provide?") },
                            placeholder = { Text("e.g. Electrician, Tailor") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    // Description TextField
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Brief Description of Your Service") },
                        placeholder = { Text("State what you do, your pricing, equipment, areas you cover, etc.") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    // Mobile number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Contact Mobile Number") },
                        placeholder = { Text("e.g. +92 345 1234567") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Image Selection Panel for Services
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Service Showcase Picture (Optional)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (imageUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Service showcase preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { imageUrl = "" },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val progressVal = uploadProgress
                        if (progressVal != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = progressVal,
                                    color = FacebookBlue,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(uploadStatusText, fontSize = 11.sp, color = FacebookBlue)
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { launchCameraForService() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("service_camera_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera", modifier = Modifier.size(15.dp), tint = Color.White)
                                        Text("Camera", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        try {
                                            imagePickerLauncher.launch("image/*")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp).testTag("service_upload_btn")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        Text("Gallery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        // Auto-populate custom Unsplash image suited for selected category
                                        imageUrl = when (selectedType) {
                                            "Driver" -> "https://images.unsplash.com/photo-1511919884226-fd3cad34687c?w=500"
                                            "Guest House" -> "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=500"
                                            "Carpenter" -> "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=500"
                                            "Plumber" -> "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?w=500"
                                            else -> "https://images.unsplash.com/photo-1581092921461-eab62e97a780?w=500"
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text("Stock", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val finalType = if (selectedType == "Other") {
                                if (customType.isBlank()) "Other Service" else customType.trim()
                            } else {
                                selectedType
                            }
                            if (description.isNotBlank() && phoneNumber.isNotBlank()) {
                                onServicePosted(finalType, description.trim(), phoneNumber.trim(), imageUrl)
                            }
                        },
                        enabled = description.isNotBlank() && phoneNumber.isNotBlank() && (selectedType != "Other" || customType.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Post Service", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
