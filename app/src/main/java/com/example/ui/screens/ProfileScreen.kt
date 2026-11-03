package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.User
import com.example.ui.ProfileTab
import com.example.ui.components.CropAspectRatio
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.ImageCropperDialog
import com.example.ui.components.PostCard
import com.example.ui.theme.BusinessBadge
import com.example.ui.theme.FbBlue
import com.example.ui.theme.VerifiedBadge

@Composable
fun ProfileScreen(
    user: User,
    allPosts: List<Post>,
    currentTab: ProfileTab,
    onTabSelected: (ProfileTab) -> Unit,
    onLikeClick: (String) -> Unit,
    onRepostClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
    isProfileLoading: Boolean = false,
    profileError: String? = null,
    onDismissProfileError: () -> Unit = {},
    onUpdateProfile: (
        name: String,
        bio: String,
        location: String,
        jobTitle: String,
        education: String,
        avatarUri: String?,
        coverUri: String?,
        phoneNumber: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onHashtagClick: (String) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    pendingFollowRequestsCount: Int = 0,
    onOpenPrivacySettings: () -> Unit = {},
    onOpenFollowRequests: () -> Unit = {},
    onToggleAccountPrivacy: (Boolean) -> Unit = {},
    isAdmin: Boolean = false,
    onOpenAdminDashboard: (() -> Unit)? = null,
    onRequestBlueTickVerification: ((category: String, supportingInfo: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isEditProfileOpen by remember { mutableStateOf(false) }
    var isVerificationDialogOpen by remember { mutableStateOf(false) }
    var verificationCategory by remember { mutableStateOf("Community Leader / Public Figure") }
    var verificationSupportingInfo by remember { mutableStateOf("") }

    // Cropper states for profile avatar and cover photo
    var croppingAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var croppingCoverUri by remember { mutableStateOf<Uri?>(null) }

    // Direct Gallery Photo Pickers for Cover and Profile Avatar
    val coverPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            croppingCoverUri = uri
        }
    }

    val avatarPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            croppingAvatarUri = uri
        }
    }

    val userPosts = allPosts.filter { it.author.id == user.id }
    val savedPosts = allPosts.filter { it.isSaved }
    val mediaPosts = allPosts.filter { it.author.id == user.id && it.mediaUrls.isNotEmpty() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isProfileLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_loading_bar"),
                    color = FbBlue
                )
            }
        }

        if (profileError != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = profileError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissProfileError,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Cover and Avatar Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Cover Image
                AsyncImage(
                    model = user.coverUrl ?: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1000",
                    contentDescription = "Cover Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable {
                            coverPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentScale = ContentScale.Crop
                )

                // Cover photo camera badge
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clickable {
                            coverPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("change_cover_badge_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Cover",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Cover", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }

                // Avatar with Camera Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                ) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable {
                                avatarPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentScale = ContentScale.Crop
                    )

                    Surface(
                        shape = CircleShape,
                        color = FbBlue,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clickable {
                                avatarPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("change_avatar_badge_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Profile Picture",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        // Profile Details
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Member",
                                    tint = VerifiedBadge,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (user.isBusinessAccount) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BusinessBadge.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BusinessBadge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (user.isPrivate) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Private Account",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "PRIVATE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            text = user.handle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Logout Action button
                    OutlinedButton(
                        onClick = onLogoutClick,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("logout_profile_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Sign Out", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bio
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location
                if (user.location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = user.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Follower / Following Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row {
                        Text(
                            text = "${user.followingCount} ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Following",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        Text(
                            text = "${user.followersCount} ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Followers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        Text(
                            text = "${user.postsCount} ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Posts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile Actions Row: Edit Profile, Privacy & Safety, and Sign Out
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditProfileOpen = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("edit_profile_btn"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    FilledTonalButton(
                        onClick = onOpenPrivacySettings,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("privacy_settings_profile_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = FbBlue.copy(alpha = 0.12f),
                            contentColor = FbBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy & Safety",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Privacy",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(38.dp)
                            .testTag("sign_out_profile_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (isAdmin && onOpenAdminDashboard != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onOpenAdminDashboard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("open_admin_dashboard_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Control Panel & Moderation",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!user.isVerified && onRequestBlueTickVerification != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { isVerificationDialogOpen = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("request_blue_tick_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VerifiedBadge.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VerifiedBadge)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VerifiedBadge,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Request Blue Tick Verification",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Follow Requests Banner if any pending
                if (pendingFollowRequestsCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFollowRequests() }
                            .testTag("profile_pending_requests_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FbBlue.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, FbBlue.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = FbBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$pendingFollowRequestsCount Follow Requests",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = FbBlue
                                    )
                                    Text(
                                        text = "Tap to review and approve",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Button(
                                onClick = onOpenFollowRequests,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Review", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Tab Navigation (Posts, Media, Bookmarks, About)
        item {
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FbBlue
            ) {
                ProfileTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Tab Content
        when (currentTab) {
            ProfileTab.POSTS -> {
                if (userPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You haven't published any posts yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(userPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                            PostCard(
                                post = post,
                                onLikeClick = { onLikeClick(post.id) },
                                onRepostClick = { onRepostClick(post.id) },
                                onCommentClick = { onCommentClick(post) },
                                onShareClick = { onShareClick(post.id) },
                                onBookmarkClick = { onBookmarkClick(post.id) },
                                onHashtagClick = onHashtagClick,
                                onMentionClick = onMentionClick
                            )
                        }
                    }
                }
            }
            ProfileTab.REELS -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your published Reels will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            ProfileTab.BOOKMARKS -> {
                if (savedPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No saved posts yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bookmark posts in your feed to read them here later.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(savedPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                            PostCard(
                                post = post,
                                onLikeClick = { onLikeClick(post.id) },
                                onRepostClick = { onRepostClick(post.id) },
                                onCommentClick = { onCommentClick(post) },
                                onShareClick = { onShareClick(post.id) },
                                onBookmarkClick = { onBookmarkClick(post.id) },
                                onHashtagClick = onHashtagClick,
                                onMentionClick = onMentionClick
                            )
                        }
                    }
                }
            }
            ProfileTab.MEDIA -> {
                if (mediaPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No media posts uploaded yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(mediaPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                            PostCard(
                                post = post,
                                onLikeClick = { onLikeClick(post.id) },
                                onRepostClick = { onRepostClick(post.id) },
                                onCommentClick = { onCommentClick(post) },
                                onShareClick = { onShareClick(post.id) },
                                onBookmarkClick = { onBookmarkClick(post.id) }
                            )
                        }
                    }
                }
            }
            ProfileTab.ABOUT -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Account & Platform Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Platform: YARKHOON.COM (Android Social Media)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "👤 Full Name: ${user.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "🏷️ Username: ${user.handle}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (user.email.isNotBlank()) {
                                Text(
                                    text = "📧 Email: ${user.email}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = "🔑 Firebase UID: ${user.id}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (isAdmin) {
                                Text(
                                    text = "🛡️ Administrative Status: Authorized Super Admin / Admin",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FbBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Account Type: ${if (user.isBusinessAccount) "Professional / Business Profile" else "Personal Member"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (user.location.isNotBlank()) {
                                Text(
                                    text = "📍 Location: ${user.location}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (user.jobTitle.isNotBlank()) {
                                Text(
                                    text = "💼 Profession / Work: ${user.jobTitle}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (user.education.isNotBlank()) {
                                Text(
                                    text = "🎓 Education: ${user.education}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (user.phoneNumber.isNotBlank()) {
                                Text(
                                    text = "📱 Phone: ${user.phoneNumber}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (user.interests.isNotEmpty()) {
                                Text(
                                    text = "🌟 Interests: ${user.interests.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            val memberSinceText = remember(user.createdAt) {
                                try {
                                    java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                                        .format(java.util.Date(user.createdAt))
                                } catch (_: Exception) {
                                    "October 2026"
                                }
                            }
                            Text(
                                text = "Member Since: $memberSinceText",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Community Guidelines & Privacy Policy: Adheres to Google Play standards with zero-tolerance for abuse.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = onLogoutClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("about_tab_sign_out_btn"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign Out of YARKHOON.COM",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isEditProfileOpen) {
        EditProfileDialog(
            currentUser = user,
            isLoading = isProfileLoading,
            onDismiss = { isEditProfileOpen = false },
            onToggleAccountPrivacy = onToggleAccountPrivacy,
            onSaveProfile = { name, bio, loc, job, edu, avatar, cover, phone ->
                onUpdateProfile(name, bio, loc, job, edu, avatar, cover, phone)
                isEditProfileOpen = false
            }
        )
    }

    // Modal: Avatar Photo Cropper
    croppingAvatarUri?.let { avatarUri ->
        ImageCropperDialog(
            imageUri = avatarUri,
            initialAspectRatio = CropAspectRatio.CIRCLE,
            allowedAspectRatios = listOf(
                CropAspectRatio.CIRCLE,
                CropAspectRatio.SQUARE,
                CropAspectRatio.FREE
            ),
            title = "Crop Profile Picture",
            onDismiss = { croppingAvatarUri = null },
            onCropSuccess = { croppedUri ->
                onUpdateProfile(
                    user.name,
                    user.bio,
                    user.location,
                    user.jobTitle,
                    user.education,
                    croppedUri.toString(),
                    user.coverUrl,
                    user.phoneNumber
                )
                croppingAvatarUri = null
            }
        )
    }

    // Modal: Cover Photo Cropper
    croppingCoverUri?.let { coverUri ->
        ImageCropperDialog(
            imageUri = coverUri,
            initialAspectRatio = CropAspectRatio.LANDSCAPE_16_9,
            allowedAspectRatios = listOf(
                CropAspectRatio.LANDSCAPE_16_9,
                CropAspectRatio.FREE,
                CropAspectRatio.SQUARE
            ),
            title = "Crop Cover Photo",
            onDismiss = { croppingCoverUri = null },
            onCropSuccess = { croppedUri ->
                onUpdateProfile(
                    user.name,
                    user.bio,
                    user.location,
                    user.jobTitle,
                    user.education,
                    user.avatarUrl,
                    croppedUri.toString(),
                    user.phoneNumber
                )
                croppingCoverUri = null
            }
        )
    }

    if (isVerificationDialogOpen && onRequestBlueTickVerification != null) {
        val categories = listOf(
            "Community Leader / Public Figure",
            "Verified Local Business",
            "Journalist / Media",
            "Educator / Social Worker",
            "Community Member"
        )
        AlertDialog(
            onDismissRequest = { isVerificationDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerifiedBadge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply for Blue Tick Verification", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Verified accounts display an official Blue Tick next to their name on profiles, posts, and comments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Select Category:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { verificationCategory = cat }
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = verificationCategory == cat,
                                onClick = { verificationCategory = cat }
                            )
                            Text(cat, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = verificationSupportingInfo,
                        onValueChange = { verificationSupportingInfo = it },
                        label = { Text("Supporting Information / Role in Yarkhoon") },
                        placeholder = { Text("Describe your official role, organization, or community standing...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verification_supporting_info_input"),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRequestBlueTickVerification(verificationCategory, verificationSupportingInfo)
                        verificationSupportingInfo = ""
                        isVerificationDialogOpen = false
                    },
                    enabled = verificationSupportingInfo.isNotBlank(),
                    modifier = Modifier.testTag("submit_verification_request_btn")
                ) {
                    Text("Submit Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { isVerificationDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
