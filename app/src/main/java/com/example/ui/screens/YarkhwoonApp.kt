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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.io.File
import coil.compose.AsyncImage
import com.example.R
import com.example.data.*
import com.example.ui.SocialMediaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Color tokens matching Facebook's active look but optimized with luxurious accents
val FacebookBlue = Color(0xFF1877F2)
val DarkBackground = Color(0xFF121212)
val LightSurface = Color(0xFFF0F2F5)
val MessengerBubbleMe = Color(0xFF0084FF)
val MessengerBubbleThem = Color(0xFFE4E6EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarkhwoonApp(viewModel: SocialMediaViewModel) {
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
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showSellItemDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showPostServiceDialog by remember { mutableStateOf(false) }

    val activeChatUser = remember(users, activeChatUserId) {
        users.find { it.id == activeChatUserId }
    }

    val hasCompletedAccount = remember(users) {
        users.any { it.isProfileCompleted }
    }

    var authScreen by remember { mutableStateOf("login") }

    val userVal = currentUser
    val showSignUpRoute = authScreen == "signup" || !hasCompletedAccount || (userVal != null && !userVal.isProfileCompleted)

    if (userVal != null && userVal.isProfileCompleted) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.yarkhoon_logo),
                            contentDescription = "yarkhoon logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = "Yarkhoon.com",
                            color = FacebookBlue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("app_brand_title")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            currentTab = "chat"
                        },
                        modifier = Modifier.testTag("top_chat_shortcut")
                    ) {
                        BadgedBox(
                            badge = {
                                Badge { Text("3") }
                            }
                        ) {
                            Icon(Icons.Filled.ChatBubble, contentDescription = "Messages")
                        }
                    }
                    IconButton(
                        onClick = {
                            try {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            currentTab = "profile"
                        },
                        modifier = Modifier.testTag("top_profile_shortcut")
                    ) {
                        ProfileAvatar(
                            imageUrl = currentUser?.avatarUrl ?: "",
                            size = 32
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_navigation_bar")
            ) {
                val isUserAdmin = currentUser?.id == "admin" || currentUser?.email == "ceo@yarkhoon.com" || currentUser?.username == "ceo" || currentUser?.email == "admin@yarkhoon.com" || currentUser?.username == "admin"
                val tabs = buildList {
                    add(NavigationItem("feed", "Home", Icons.Filled.Home, Icons.Outlined.Home))
                    add(NavigationItem("friends", "Friends", Icons.Filled.People, Icons.Outlined.People))
                    add(NavigationItem("marketplace", "Marketplace", Icons.Filled.Storefront, Icons.Outlined.Storefront))
                    add(NavigationItem("services", "Services", Icons.Filled.Build, Icons.Outlined.Build))
                    add(NavigationItem("groups", "Groups", Icons.Filled.Groups, Icons.Outlined.Groups))
                    add(NavigationItem("chat", "Chat", Icons.Filled.Chat, Icons.Outlined.Chat))
                    add(NavigationItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person))
                    if (isUserAdmin) {
                        add(NavigationItem("admin", "Admin", Icons.Filled.Shield, Icons.Outlined.Shield))
                    }
                }

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
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FacebookBlue,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = FacebookBlue,
                            indicatorColor = FacebookBlue.copy(alpha = 0.08f)
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
                "groups" -> {
                    FloatingActionButton(
                        onClick = { showCreateGroupDialog = true },
                        containerColor = FacebookBlue,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_create_group")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Create Group")
                            Text("Group")
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
            Crossfade(targetState = currentTab, label = "TabSwitch") { tab ->
                when (tab) {
                    "feed" -> FeedScreen(
                        posts = posts,
                        users = users,
                        currentUser = currentUser,
                        onLike = { viewModel.onToggleLike(it) },
                        onComment = { post, text -> viewModel.onAddComment(post, text) },
                        onPostClicked = { showCreatePostDialog = true }
                    )
                    "friends" -> FriendsScreen(
                        users = users,
                        onAddFriend = { viewModel.onSendFriendRequest(it) },
                        onAcceptFriend = { viewModel.onAcceptFriendRequest(it) },
                        onRemoveFriend = { viewModel.onRemoveFriend(it) }
                    )
                    "marketplace" -> MarketplaceScreen(
                        items = marketplaceItems,
                        currentUser = currentUser,
                        onToggleSold = { viewModel.onToggleMarketplaceItemSold(it) }
                    )
                    "services" -> ServicesScreen(
                        listings = serviceListings,
                        currentUser = currentUser,
                        onDeleteListing = { viewModel.onDeleteServiceListing(it) }
                    )
                    "groups" -> GroupsScreen(
                        groups = groups,
                        onJoinToggle = { viewModel.onJoinGroup(it) }
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
                        }
                    )
                    "profile" -> ProfileScreen(
                        currentUser = currentUser,
                        posts = posts.filter { it.authorId == "currentUser" || it.authorId == (currentUser?.id ?: "") },
                        marketplaceItems = marketplaceItems.filter { it.sellerId == "currentUser" || it.sellerId == (currentUser?.id ?: "") },
                        onEditProfileClick = { showEditProfileDialog = true },
                        onResetProfileClick = { viewModel.onResetProfile() },
                        onRemoveListing = { viewModel.onToggleMarketplaceItemSold(it) },
                        onDeletePost = { viewModel.deletePost(it) }
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
                }
            )
        }
    }
}

data class NavigationItem(
    val id: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// ==================== FEED SCREEN ====================
@Composable
fun FeedScreen(
    posts: List<Post>,
    users: List<User>,
    currentUser: User?,
    onLike: (Post) -> Unit,
    onComment: (Post, String) -> Unit,
    onPostClicked: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("feed_scroll_view"),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
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

        // Horizontal Stories Widget
        item {
            StorySection(users = users, currentUser = currentUser)
        }

        // List of Posts
        items(posts, key = { it.id }) { post ->
            PostCard(
                post = post,
                onLike = { onLike(post) },
                onComment = { text -> onComment(post, text) },
                users = users
            )
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
fun StorySection(users: List<User>, currentUser: User?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .padding(bottom = 8.dp)
    ) {
        Text("Stories", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("stories_row")
        ) {
            // Create user story card
            item {
                Card(
                    modifier = Modifier
                        .size(width = 100.dp, height = 150.dp)
                        .clickable { },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = currentUser?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                            contentDescription = "Create Story",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                        startY = 80f
                                    )
                                )
                        )
                        Surface(
                            shape = CircleShape,
                            color = FacebookBlue,
                            border = BorderStroke(2.dp, Color.White),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 15.dp)
                                .size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            "Create\nStory",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                        )
                    }
                }
            }

            // Normal stories from other users
            items(users.filter { !it.isCurrentUser }) { user ->
                val storyBg = remember(user.id) {
                    when (user.id) {
                        "user_ali" -> "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=200&auto=format&fit=crop"
                        "user_zara" -> "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=200&auto=format&fit=crop"
                        "user_sher" -> "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=200&auto=format&fit=crop"
                        else -> "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=200&auto=format&fit=crop"
                    }
                }
                Card(
                    modifier = Modifier
                        .size(width = 100.dp, height = 150.dp)
                        .clickable { },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = storyBg,
                            contentDescription = "${user.fullName}'s Story",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                        startY = 100f
                                    )
                                )
                        )
                        // User Avatar atop story
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, FacebookBlue),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(32.dp)
                                .align(Alignment.TopStart)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            user.fullName,
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
    onLike: () -> Unit,
    onComment: (String) -> Unit,
    users: List<User> = emptyList()
) {
    var isCommentSectionExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("post_card_${post.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    }
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
                    SimulatedVideoPlayer(videoUrl = post.mediaUrl)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (post.likesCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = FacebookBlue,
                            modifier = Modifier.size(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                        Text(
                            "${post.likesCount} ${if (post.likesCount == 1) "Like" else "Likes"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "${post.commentsCount} comments",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Interactions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val isLiked = post.isLikedByMe
                InteractionButton(
                    icon = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    label = "Like",
                    tint = if (isLiked) FacebookBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onLike,
                    testTag = "like_button_${post.id}"
                )
                InteractionButton(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = "Comment",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { isCommentSectionExpanded = !isCommentSectionExpanded },
                    testTag = "comment_button_${post.id}"
                )
                InteractionButton(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { /* Share simulated callback */ },
                    testTag = "share_button_${post.id}"
                )
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

                    // Simulated static comment list for UI premium feel
                    CommentItem(
                        author = "Ali Khan",
                        avatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120",
                        comment = "Perfect capture of Yarkhwoon! This look is so pristine.",
                        time = "10m"
                    )
                    if (post.commentsCount > 1) {
                        CommentItem(
                            author = "Zara Shah",
                            avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120",
                            comment = "Awesome update. Keep moving forward! 🏔️✨",
                            time = "3m"
                        )
                    }

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

// ==================== SIMULATED VIDEO PLAYER ====================
@Composable
fun SimulatedVideoPlayer(videoUrl: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.4f) }
    var isMuted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Simulate progress update when playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(1000)
                progress += 0.05f
                if (progress >= 1.0f) {
                    progress = 0f
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .testTag("simulated_video_player")
    ) {
        // High fidelity placeholder background with play/video theme
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = "Simulated high-contrast video thumbnail",
                modifier = Modifier.size(96.dp),
                tint = Color.White.copy(alpha = 0.15f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        radius = 400f
                    )
                )
        )

        // Valley Glaciers and Trekking visual overlay
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = { isPlaying = !isPlaying },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = FacebookBlue.copy(alpha = 0.85f),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("video_play_toggle")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = if (isPlaying) "Streaming: Broghil Pass Expedition" else "Video Paused",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Controllers Bottom Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "0:18", // Simulated current runtime
                color = Color.White,
                fontSize = 11.sp
            )

            Slider(
                value = progress,
                onValueChange = { progress = it },
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = FacebookBlue,
                    activeTrackColor = FacebookBlue,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )

            Text(
                "1:45", // Total simulated duration
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )

            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==================== FRIENDS SCREEN ====================
@Composable
fun FriendsScreen(
    users: List<User>,
    onAddFriend: (String) -> Unit,
    onAcceptFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit
) {
    val pendingRequests = remember(users) {
        users.filter { it.friendStatus == "RECEIVED" }
    }
    val suggestedFriends = remember(users) {
        users.filter { it.friendStatus == "NONE" && !it.isCurrentUser }
    }
    val currentFriends = remember(users) {
        users.filter { it.friendStatus == "FRIENDS" && !it.isCurrentUser }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("friends_tab_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Friend Requests Received Panel
        if (pendingRequests.isNotEmpty()) {
            item {
                SectionHeader("Friend Requests (${pendingRequests.size})")
            }
            items(pendingRequests, key = { it.id }) { user ->
                FriendRequestCard(
                    user = user,
                    onAccept = { onAcceptFriend(user.id) },
                    onDecline = { onRemoveFriend(user.id) }
                )
            }
        }

        // Suggestions panel
        item {
            SectionHeader("People You May Know")
        }
        if (suggestedFriends.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "All users are already connected with you!",
                    icon = Icons.Outlined.CheckCircle
                )
            }
        } else {
            items(suggestedFriends, key = { it.id }) { user ->
                FriendSuggestionCard(
                    user = user,
                    onAdd = { onAddFriend(user.id) }
                )
            }
        }

        // Contacts list
        item {
            SectionHeader("My Friends (${currentFriends.size})")
        }
        if (currentFriends.isEmpty()) {
            item {
                EmptyStateCard(
                    message = "No friends connected yet. Connect with people from suggestions!",
                    icon = Icons.Outlined.People
                )
            }
        } else {
            items(currentFriends, key = { it.id }) { user ->
                FriendContactItem(
                    user = user,
                    onRemove = { onRemoveFriend(user.id) }
                )
            }
        }
    }
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

@Composable
fun FriendRequestCard(user: User, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(imageUrl = user.avatarUrl, size = 64)
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(user.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("accept_friend_${user.id}")
                    ) {
                        Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("decline_friend_${user.id}")
                    ) {
                        Text("Delete", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendSuggestionCard(user: User, onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(imageUrl = user.avatarUrl, size = 52)
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(user.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue.copy(alpha = 0.1f), contentColor = FacebookBlue),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("add_friend_${user.id}")
            ) {
                Text("Add Friend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FriendContactItem(user: User, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(imageUrl = user.avatarUrl, size = 44)
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Friends", fontSize = 11.sp, color = Color(0xFF45BD62), fontWeight = FontWeight.SemiBold)
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.testTag("remove_friend_${user.id}")
            ) {
                Icon(Icons.Filled.PersonRemove, contentDescription = "Unfriend", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ==================== MARKETPLACE SCREEN ====================
@Composable
fun MarketplaceScreen(
    items: List<MarketplaceItem>,
    currentUser: User?,
    onToggleSold: (MarketplaceItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Apparel", "Home Decor", "Food & Edibles", "Services")

    val filteredItems = remember(items, searchQuery, selectedCategory) {
        items.filter { item ->
            val matchSearch = item.title.contains(searchQuery, ignoreCase = true) || item.description.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            matchSearch && matchCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("marketplace_tab_view")
    ) {
        // Search & Filters Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Marketplace") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
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
                        .height(48.dp)
                        .testTag("marketplace_search")
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

        // Listings Grid
        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    message = "No marketplace listings match search filters.",
                    icon = Icons.Outlined.Storefront
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("marketplace_grid")
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    MarketplaceItemCard(
                        item = item,
                        currentUser = currentUser,
                        onSoldClick = { onToggleSold(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun MarketplaceItemCard(item: MarketplaceItem, currentUser: User?, onSoldClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.testTag("market_card_${item.id}")
    ) {
        val context = LocalContext.current
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Price badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        "$${item.price}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Category tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(FacebookBlue, shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(item.category, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                if (item.isSold) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SOLD",
                            color = Color.Red,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    item.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "Seller: ${item.sellerName}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FacebookBlue
                )

                Text(
                    "Contact: ${item.sellerContact}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (item.sellerId == "currentUser" || item.sellerId == (currentUser?.id ?: "")) {
                    OutlinedButton(
                        onClick = onSoldClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(32.dp)
                            .testTag("mark_sold_button_${item.id}")
                    ) {
                        Text(if (item.isSold) "Relist" else "Mark Sold", fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:${item.sellerContact}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.widget.Toast.makeText(context, "Contact number: ${item.sellerContact}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(32.dp)
                            .testTag("contact_seller_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Text("Contact", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
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
    onSendMessage: (String) -> Unit
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
                                .widthIn(max = 260.dp)
                                .testTag("chat_bubble_${message.id}")
                        ) {
                            Text(
                                text = message.content,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            )
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
    onEditProfileClick: () -> Unit,
    onResetProfileClick: () -> Unit,
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

        // Description Bio
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
                    Column {
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEditProfileClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Edit", fontSize = 12.sp)
                            }
                        }
                        IconButton(
                            onClick = onResetProfileClick,
                            modifier = Modifier.testTag("reset_profile_button")
                        ) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Log Out & Reset", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    currentUser?.bio ?: "No bio added yet.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
fun ProfileAvatar(imageUrl: String, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
        AsyncImage(
            model = imageUrl.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150" },
            contentDescription = "Avatar",
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

    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadStatusText by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                uploadProgress = 0f
                uploadStatusText = "Connecting to yarkhoon.com secure host..."
                try {
                    val tempFile = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val outputDir = context.filesDir
                                val file = File(outputDir, "yarkhwoon_upload_${System.currentTimeMillis()}.jpg")
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
                            delay(80)
                            uploadProgress = p / 10f
                            uploadStatusText = "Uploading photo to cloud server... ${p * 10}%"
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
            try {
                android.widget.Toast.makeText(context, "Gallery selection is unavailable on this device", android.widget.Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                // Silent ignore
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
                    Text("Attach Media", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            selected = mediaType == "IMAGE",
                            onClick = {
                                mediaType = "IMAGE"
                                if (!mediaUrl.startsWith("file://")) {
                                    safeLaunchImagePicker()
                                }
                            },
                            label = { Text("+ Upload Photo") },
                            modifier = Modifier.testTag("post_media_image")
                        )
                        FilterChip(
                            selected = mediaType == "VIDEO",
                            onClick = {
                                mediaType = "VIDEO"
                                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                            },
                            label = { Text("+ Video (Link)") },
                            modifier = Modifier.testTag("post_media_video")
                        )
                    }
                }

                // Custom media layouts based on state
                if (mediaType == "IMAGE") {
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
                    } else if (mediaUrl.startsWith("file://")) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Uploaded content preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        mediaUrl = ""
                                        mediaType = "NONE"
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { safeLaunchImagePicker() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Choose Image from Device",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else if (mediaType == "VIDEO") {
                    OutlinedTextField(
                        value = mediaUrl,
                        onValueChange = { mediaUrl = it },
                        label = { Text("Video Link URL", fontSize = 11.sp) },
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_media_url_input")
                    )
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
    var category by remember { mutableStateOf("Apparel") }
    var imageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400") }

    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    var uploadStatusText by remember { mutableStateOf("") }

    val categories = listOf("Apparel", "Home Decor", "Food & Edibles", "Services")

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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
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
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                                    Text("Upload Picture", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    imageUrl = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400"
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Use Demo Image", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                                            onValueChange = { firstName = it },
                                            label = { Text("First Name") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("signup_first_name")
                                        )
                                        OutlinedTextField(
                                            value = lastName,
                                            onValueChange = { lastName = it },
                                            label = { Text("Last Name") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("signup_last_name")
                                        )
                                    }

                                    OutlinedTextField(
                                        value = username,
                                        onValueChange = { username = it.take(20).filter { char -> char.isLetterOrDigit() || char == '_' } },
                                        label = { Text("Username") },
                                        singleLine = true,
                                        prefix = { Text("@", color = FacebookBlue) },
                                        modifier = Modifier.fillMaxWidth().testTag("signup_username"),
                                        placeholder = { Text("e.g. jandekhan") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                                    )

                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it.trim() },
                                        label = { Text("Email Address") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("signup_email"),
                                        placeholder = { Text("e.g. ali@domain.com") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                    )

                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("New Password") },
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
                                        placeholder = { Text("At least 6 characters") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )

                                    val isEmailValid = email.contains("@") && email.contains(".")
                                    val isPasswordValid = password.length >= 6
                                    val isFormValid = firstName.isNotBlank() && lastName.isNotBlank() && username.isNotBlank() && isEmailValid && isPasswordValid

                                    if (email.isNotBlank() && !isEmailValid) {
                                        Text("Please enter a valid email address.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                                    }
                                    if (password.isNotBlank() && !isPasswordValid) {
                                        Text("Password must be at least 6 characters.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            println("[DEBUG_CLICK] Button clicked. firstName: '$firstName', lastName: '$lastName', username: '$username'")
                                            try {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            if (isFormValid) {
                                                println("[DEBUG_CLICK] Conditions met. Setting step = 2")
                                                step = 2
                                            } else {
                                                println("[DEBUG_CLICK] Conditions NOT met!")
                                            }
                                        },
                                        enabled = isFormValid,
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
                                                Text("$firstName $lastName", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                                                onComplete("$firstName $lastName", username, email, password, bio, selectedAvatarUrl, selectedCoverUrl)
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
    onAdminLoginSuccess: () -> Unit
) {
    var usernameText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var showForgotHelpDialog by remember { mutableStateOf(false) }
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

                    // Create New Account Green Button (Facebook classic green)
                    Button(
                        onClick = onCreateAccount,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
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

    val presetTypes = listOf("Driver", "Guest House", "Carpenter", "Plumber", "Other")

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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
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
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        Text("Upload Image", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Text("Pick Stock Photo", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
