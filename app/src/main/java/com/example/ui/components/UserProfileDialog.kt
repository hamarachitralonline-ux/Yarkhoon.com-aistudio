package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.User
import com.example.ui.theme.BusinessBadge
import com.example.ui.theme.FbBlue
import com.example.ui.theme.VerifiedBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    user: User,
    posts: List<Post>,
    isFollowing: Boolean = false,
    isFollowRequested: Boolean = false,
    isBlocked: Boolean = false,
    isMuted: Boolean = false,
    isFavorite: Boolean = false,
    currentUserId: String = "",
    onDismiss: () -> Unit,
    onSendMessage: (User) -> Unit,
    onLikePost: (String) -> Unit,
    onCommentPost: (Post) -> Unit,
    onHashtagClick: (String) -> Unit,
    onToggleFollow: ((User) -> Unit)? = null,
    onCancelFollowRequest: ((User) -> Unit)? = null,
    onBlockUser: ((User) -> Unit)? = null,
    onUnblockUser: ((User) -> Unit)? = null,
    onToggleMute: ((User) -> Unit)? = null,
    onToggleFavorite: ((User) -> Unit)? = null,
    onBookmarkPost: ((String) -> Unit)? = null,
    onSharePost: ((Post) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onReportUser: ((User) -> Unit)? = null
) {
    val isSelf = remember(user.id, currentUserId) { user.id == currentUserId }
    val isLockedPrivate = remember(user.isPrivate, isFollowing, isSelf) {
        user.isPrivate && !isFollowing && !isSelf
    }

    val userPosts = remember(user.id, posts, isLockedPrivate, isBlocked) {
        if (isLockedPrivate || isBlocked) emptyList()
        else posts.filter { it.author.id == user.id }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirmation by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("user_profile_dialog"),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified Account",
                                        tint = VerifiedBadge,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (user.isPrivate) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Private Account",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = user.handle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_user_profile_btn")) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close Profile")
                        }
                    },
                    actions = {
                        if (!isSelf) {
                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.testTag("user_profile_menu_btn")
                                ) {
                                    Icon(Icons.Outlined.MoreVert, contentDescription = "More Options")
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    if (!isBlocked) {
                                        // Favorite toggle
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites")
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B)
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onToggleFavorite?.invoke(user)
                                            },
                                            modifier = Modifier.testTag("menu_toggle_favorite")
                                        )

                                        // Mute toggle
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (isMuted) "Unmute @${user.handle.removePrefix("@")}" else "Mute @${user.handle.removePrefix("@")}")
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (isMuted) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onToggleMute?.invoke(user)
                                            },
                                            modifier = Modifier.testTag("menu_toggle_mute")
                                        )

                                        HorizontalDivider()

                                        // Block user
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "Block @${user.handle.removePrefix("@")}",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                showBlockConfirmation = true
                                            },
                                            modifier = Modifier.testTag("menu_block_user")
                                        )

                                        if (onReportUser != null) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Report Profile",
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Flag,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                onClick = {
                                                    showMenu = false
                                                    onReportUser(user)
                                                },
                                                modifier = Modifier.testTag("menu_report_user")
                                            )
                                        }
                                    } else {
                                        // Unblock user
                                        DropdownMenuItem(
                                            text = {
                                                Text("Unblock @${user.handle.removePrefix("@")}")
                                            },
                                            leadingIcon = {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = FbBlue)
                                            },
                                            onClick = {
                                                showMenu = false
                                                onUnblockUser?.invoke(user)
                                            },
                                            modifier = Modifier.testTag("menu_unblock_user")
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Header: Cover and Avatar
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Cover Photo
                        val cover = user.coverUrl
                            ?: "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800"
                        AsyncImage(
                            model = cover,
                            contentDescription = "Cover Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Crop
                        )

                        // Avatar
                        AsyncImage(
                            model = user.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                            contentDescription = user.name,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 90.dp)
                                .size(88.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.isVerified) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Creator",
                                    tint = VerifiedBadge,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (user.isBusinessAccount) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BusinessBadge.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BusinessBadge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (user.isPrivate) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Private",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            if (isFavorite) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Favorite",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = user.handle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (user.bio.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = user.bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Details (Job title & location)
                        if (user.jobTitle.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = user.jobTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (user.location.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = user.location,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (user.education.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = user.education,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (user.isBusinessAccount && !user.businessCategory.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = user.businessCategory,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (user.interests.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                user.interests.take(4).forEach { interest ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(text = interest, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${user.followersCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Followers",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text(
                                    text = "${user.followingCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Following",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column {
                                Text(
                                    text = "${if (userPosts.isNotEmpty()) userPosts.size else user.postsCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Posts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Follow & Message
                        if (!isSelf) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isBlocked) {
                                    // Blocked State -> Unblock button
                                    OutlinedButton(
                                        onClick = { onUnblockUser?.invoke(user) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("unblock_user_btn"),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Unblock", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // Follow / Following / Requested button
                                    Button(
                                        onClick = {
                                            if (isFollowRequested) {
                                                onCancelFollowRequest?.invoke(user)
                                            } else {
                                                onToggleFollow?.invoke(user)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("follow_user_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                isFollowing -> MaterialTheme.colorScheme.surfaceVariant
                                                isFollowRequested -> MaterialTheme.colorScheme.surfaceVariant
                                                else -> FbBlue
                                            },
                                            contentColor = when {
                                                isFollowing -> MaterialTheme.colorScheme.onSurfaceVariant
                                                isFollowRequested -> MaterialTheme.colorScheme.onSurfaceVariant
                                                else -> Color.White
                                            }
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        when {
                                            isFollowing -> {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Following", fontWeight = FontWeight.Bold)
                                            }
                                            isFollowRequested -> {
                                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Requested", fontWeight = FontWeight.Bold)
                                            }
                                            else -> {
                                                Text("Follow", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onSendMessage(user)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("message_user_btn"),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Message")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isLockedPrivate && !isBlocked) {
                            Text(
                                text = "Public Posts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // If user is Blocked: show blocked notice
                if (isBlocked) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                                .testTag("blocked_profile_notice"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "You Blocked This Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "You cannot see their posts or messages. Unblock them to restore access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                // If Account is Private and not following: show Private Shield Card
                else if (isLockedPrivate) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("private_account_locked_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = CardDefaults.outlinedCardBorder(enabled = true)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(FbBlue.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "Private Account",
                                        tint = FbBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "This Account is Private",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Follow this account to see their photos, videos, stories, and updates.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isFollowRequested) {
                                    FilledTonalButton(
                                        onClick = { onCancelFollowRequest?.invoke(user) },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.testTag("cancel_follow_request_btn")
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Request Pending (Tap to Cancel)", fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    Button(
                                        onClick = { onToggleFollow?.invoke(user) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                                        modifier = Modifier.testTag("request_follow_btn")
                                    ) {
                                        Text("Request to Follow", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                // Posts by User
                else if (userPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No public posts yet from ${user.name}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(userPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            PostCard(
                                post = post,
                                onLikeClick = { onLikePost(post.id) },
                                onRepostClick = { onSharePost?.invoke(post) },
                                onCommentClick = { onCommentPost(post) },
                                onShareClick = { onSharePost?.invoke(post) },
                                onBookmarkClick = { onBookmarkPost?.invoke(post.id) },
                                onHashtagClick = onHashtagClick,
                                onMentionClick = onMentionClick
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Blocking User
    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text("Block @${user.handle.removePrefix("@")}?") },
            text = {
                Text("They won't be able to find your profile, posts, or message you on YARKHOON.COM. We won't let them know you blocked them.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBlockConfirmation = false
                        onBlockUser?.invoke(user)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
