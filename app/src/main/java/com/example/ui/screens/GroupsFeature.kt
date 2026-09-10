package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.SocialMediaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

private val BrandBlue = Color(0xFF1877F2)
private val EmeraldGreen = Color(0xFF10B981)
private val AmberOrange = Color(0xFFF59E0B)
private val RoseRed = Color(0xFFEF4444)

// Preset Scenic Covers for Group Creation
val GroupCoverPresets = listOf(
    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop" to "Hindukush Mountains",
    "https://images.unsplash.com/photo-1501183007986-d0d080b147f9?w=800&auto=format&fit=crop" to "Chitral Bazaar",
    "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=800&auto=format&fit=crop" to "Culture & Arts",
    "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?w=800&auto=format&fit=crop" to "Technology & Code",
    "https://images.unsplash.com/photo-1531415074868-036b1c5d53ec?w=800&auto=format&fit=crop" to "Sports & Polo",
    "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800&auto=format&fit=crop" to "River Valley"
)

val GroupAvatarPresets = listOf(
    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150&auto=format&fit=crop",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150&auto=format&fit=crop"
)

val GroupCategories = listOf(
    "All",
    "Active & Travel",
    "Marketplace & Trade",
    "Art & Culture",
    "Education & Tech",
    "Sports & Fitness",
    "News & Community",
    "General"
)

// =========================================================================
// MAIN GROUPS HUB SCREEN
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGroupsScreen(
    viewModel: SocialMediaViewModel,
    onOpenGroupDetail: (Group) -> Unit
) {
    val groups by viewModel.allGroups.collectAsState()
    val invites by viewModel.myGroupInvites.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Discover, 1: My Groups, 2: Invites
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val filteredGroups = remember(groups, searchQuery, selectedCategory, selectedTab, currentUser) {
        groups.filter { group ->
            val matchesCategory = selectedCategory == "All" || group.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    group.name.contains(searchQuery, ignoreCase = true) ||
                    group.description.contains(searchQuery, ignoreCase = true) ||
                    group.location.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                0 -> true // Discover
                1 -> group.isJoined || group.ownerId == (currentUser?.id ?: "currentUser")
                else -> true
            }

            matchesCategory && matchesSearch && matchesTab
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateGroupDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_group_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Group")
                    Text("Create Group", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("groups_main_list"),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar & Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Groups",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Connect with communities across Yarkhoon & Chitral",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showCreateGroupDialog = true },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Group", tint = BrandBlue)
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_search_input"),
                        placeholder = { Text("Search groups by name, topic or location...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Navigation Tabs: Discover, My Groups, Invites
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Discover", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                val joinedCount = groups.count { it.isJoined || it.ownerId == (currentUser?.id ?: "currentUser") }
                                Text("My Groups ($joinedCount)", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                BadgedBox(
                                    badge = {
                                        if (invites.isNotEmpty()) {
                                            Badge { Text("${invites.size}") }
                                        }
                                    }
                                ) {
                                    Text("Invites", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }

                    // Category Chips
                    if (selectedTab != 2) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(GroupCategories) { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Invites Tab Content
            if (selectedTab == 2) {
                if (invites.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Mail,
                            title = "No Pending Invitations",
                            subtitle = "When friends or group admins invite you to private or public groups, they will appear right here."
                        )
                    }
                } else {
                    items(invites, key = { it.id }) { invite ->
                        GroupInviteCard(
                            invite = invite,
                            onAccept = { viewModel.onRespondToGroupInvite(invite, true) },
                            onDecline = { viewModel.onRespondToGroupInvite(invite, false) },
                            onOpenGroup = {
                                val grp = groups.find { it.id == invite.groupId }
                                if (grp != null) onOpenGroupDetail(grp)
                            }
                        )
                    }
                }
            } else {
                // Group List Content
                if (filteredGroups.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Groups,
                            title = if (selectedTab == 1) "You Haven't Joined Any Groups Yet" else "No Groups Found",
                            subtitle = if (selectedTab == 1) "Explore the Discover tab or tap 'Create Group' to start your own community!" else "Try a different search query or category filter."
                        )
                    }
                } else {
                    items(filteredGroups, key = { it.id }) { group ->
                        GroupHubCard(
                            group = group,
                            currentUserId = currentUser?.id ?: "currentUser",
                            onJoinToggle = { viewModel.onJoinGroup(group) },
                            onClick = { onOpenGroupDetail(group) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, avatar, cover, cat, loc, isPriv, onlyAdminPost ->
                viewModel.onCreateGroup(
                    name = name,
                    description = desc,
                    avatarUrl = avatar,
                    coverUrl = cover,
                    category = cat,
                    location = loc,
                    isPrivate = isPriv,
                    onlyAdminsCanPost = onlyAdminPost,
                    onSuccess = { createdGroup ->
                        showCreateGroupDialog = false
                        onOpenGroupDetail(createdGroup)
                    }
                )
            }
        )
    }
}

// =========================================================================
// GROUP HUB CARD
// =========================================================================
@Composable
fun GroupHubCard(
    group: Group,
    currentUserId: String,
    onJoinToggle: () -> Unit,
    onClick: () -> Unit
) {
    val isOwner = group.ownerId == currentUserId

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("group_card_${group.id}")
    ) {
        Column {
            // Cover Image with Gradient & Privacy Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = group.coverUrl.ifBlank { "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800" },
                    contentDescription = "Group cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )

                // Top Tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Chip
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = group.category,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Privacy Pill
                    Surface(
                        color = if (group.isPrivate) AmberOrange.copy(alpha = 0.9f) else EmeraldGreen.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (group.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (group.isPrivate) "Private" else "Public",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Avatar overlay at bottom left
                AsyncImage(
                    model = group.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150" },
                    contentDescription = "Group Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 8.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                )
            }

            // Body info
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${group.memberCount} members",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (group.location.isNotBlank()) {
                                Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = group.location,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (isOwner) {
                        Surface(
                            color = BrandBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "👑 Admin/Owner",
                                color = BrandBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (group.onlyAdminsCanPost) "🔒 Admins only post" else "✍️ All members can post",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isOwner) {
                        val isJoined = group.isJoined
                        val isRequested = group.joinStatus == "REQUESTED"

                        Button(
                            onClick = onJoinToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isJoined -> MaterialTheme.colorScheme.surfaceVariant
                                    isRequested -> AmberOrange.copy(alpha = 0.2f)
                                    else -> BrandBlue
                                },
                                contentColor = when {
                                    isJoined -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isRequested -> AmberOrange
                                    else -> Color.White
                                }
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isJoined -> Icons.Default.Check
                                    isRequested -> Icons.Default.HourglassTop
                                    else -> Icons.Default.Add
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isJoined -> "Joined"
                                    isRequested -> "Requested"
                                    group.isPrivate -> "Request Join"
                                    else -> "Join Group"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// GROUP INVITE CARD
// =========================================================================
@Composable
fun GroupInviteCard(
    invite: GroupInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenGroup: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = invite.groupAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150" },
                contentDescription = "Group Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenGroup() }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invite.groupName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onOpenGroup() }
                )
                Text(
                    text = "${invite.inviterName} invited you to join",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onDecline,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Decline", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// =========================================================================
// DETAILED GROUP VIEW SCREEN
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: Group,
    viewModel: SocialMediaViewModel,
    onBack: () -> Unit,
    onNavigateToUser: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val members by viewModel.selectedGroupMembers.collectAsState()
    val posts by viewModel.selectedGroupPosts.collectAsState()
    val joinRequests by viewModel.selectedGroupJoinRequests.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    val currentUserId = currentUser?.id ?: "currentUser"
    val myMembership = members.find { it.userId == currentUserId }
    val myRole = myMembership?.role ?: if (group.ownerId == currentUserId) "OWNER" else "NONE"
    val isOwner = myRole == "OWNER" || group.ownerId == currentUserId
    val isAdmin = isOwner || myRole == "ADMIN"
    val isModerator = isAdmin || myRole == "MODERATOR"
    val isMember = myRole != "NONE" || group.isJoined

    var selectedDetailTab by remember { mutableIntStateOf(0) } // 0: Discussions, 1: Members, 2: About, 3: Admin Tools
    var showCreatePostDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }

    // Comment modal state
    var activePostForComments by remember { mutableStateOf<GroupPost?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = group.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.memberCount} members • ${if (group.isPrivate) "Private Group" else "Public Group"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInviteDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Invite Friends", tint = BrandBlue)
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Invite Friends") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                            onClick = {
                                showMenu = false
                                showInviteDialog = true
                            }
                        )
                        if (isAdmin) {
                            DropdownMenuItem(
                                text = { Text("Edit Group Settings") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showMenu = false
                                    showEditGroupDialog = true
                                }
                            )
                        }
                        if (isMember && !isOwner) {
                            DropdownMenuItem(
                                text = { Text("Leave Group") },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, null) },
                                onClick = {
                                    showMenu = false
                                    showLeaveConfirmDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Report Group") },
                            leadingIcon = { Icon(Icons.Default.Report, null, tint = RoseRed) },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            }
                        )
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text("Delete Group", color = RoseRed) },
                                leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = RoseRed) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            val canPost = isMember && (!group.onlyAdminsCanPost || isModerator)
            if (canPost && selectedDetailTab == 0) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = BrandBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("group_detail_create_post_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Post")
                        Text("Post to Group", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("group_detail_scroll_view"),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Group Hero Header
            item {
                GroupHeroHeader(
                    group = group,
                    myRole = myRole,
                    isMember = isMember,
                    onJoinToggle = { viewModel.onJoinGroup(group) },
                    onInvite = { showInviteDialog = true },
                    onOpenAdminTools = { selectedDetailTab = 3 },
                    pendingRequestsCount = joinRequests.size,
                    isAdmin = isAdmin
                )
            }

            // Tab Bar: Discussions, Members, About, (Admin Tools)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedDetailTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedDetailTab == 0,
                            onClick = { selectedDetailTab = 0 },
                            text = { Text("Discussions", fontWeight = if (selectedDetailTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedDetailTab == 1,
                            onClick = { selectedDetailTab = 1 },
                            text = { Text("Members (${members.size.coerceAtLeast(group.memberCount)})", fontWeight = if (selectedDetailTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedDetailTab == 2,
                            onClick = { selectedDetailTab = 2 },
                            text = { Text("About", fontWeight = if (selectedDetailTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                        if (isModerator) {
                            Tab(
                                selected = selectedDetailTab == 3,
                                onClick = { selectedDetailTab = 3 },
                                text = {
                                    BadgedBox(
                                        badge = {
                                            if (joinRequests.isNotEmpty()) {
                                                Badge { Text("${joinRequests.size}") }
                                            }
                                        }
                                    ) {
                                        Text("Admin Hub", fontWeight = if (selectedDetailTab == 3) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // TAB 0: DISCUSSIONS (FEED)
            if (selectedDetailTab == 0) {
                // Post Creation Bar (if allowed)
                val canPost = isMember && (!group.onlyAdminsCanPost || isModerator)
                if (canPost) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { showCreatePostDialog = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = currentUser?.avatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                    contentDescription = "My Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text("Write something in ${group.name}...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.PhotoLibrary, contentDescription = "Add Media", tint = EmeraldGreen)
                            }
                        }
                    }
                } else if (group.onlyAdminsCanPost && !isModerator) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AmberOrange)
                                Text(
                                    text = "Only admins and moderators are permitted to create posts in this group.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (!isMember) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandBlue.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Join this community", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Join ${group.name} to interact with posts, share photos, and comment.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = { viewModel.onJoinGroup(group) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(if (group.isPrivate) "Request" else "Join", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // If private group and user is not member, hide feed contents
                if (group.isPrivate && !isMember) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Lock,
                            title = "This Group is Private",
                            subtitle = "Join this group to view member discussions, photos, videos, and community updates."
                        )
                    }
                } else if (posts.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Chat,
                            title = "No Posts Yet",
                            subtitle = "Be the first to start a conversation in ${group.name}!"
                        )
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        GroupPostCard(
                            post = post,
                            isModerator = isModerator,
                            currentUserId = currentUserId,
                            onLike = { viewModel.onToggleLikeGroupPost(post) },
                            onPinToggle = { viewModel.onTogglePinGroupPost(post) },
                            onDelete = { viewModel.onDeleteGroupPost(post.id) },
                            onCommentClick = {
                                activePostForComments = post
                                viewModel.selectGroupPostForComments(post.id)
                            }
                        )
                    }
                }
            }

            // TAB 1: MEMBERS & ROLES
            if (selectedDetailTab == 1) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Group Leadership (${members.count { it.role in listOf("OWNER", "ADMIN", "MODERATOR") }})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandBlue
                        )
                    }
                }

                val leadership = members.filter { it.role in listOf("OWNER", "ADMIN", "MODERATOR") }
                items(leadership, key = { "${it.groupId}_${it.userId}" }) { member ->
                    GroupMemberRow(
                        member = member,
                        isCurrentUserAdmin = isAdmin,
                        currentUserId = currentUserId,
                        onPromote = { role -> viewModel.onPromoteDemoteMember(group.id, member, role) },
                        onRemove = { viewModel.onRemoveGroupMember(group.id, member) },
                        onBlock = { viewModel.onBlockGroupMember(group.id, member) }
                    )
                }

                item {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "All Members (${members.count { it.role == "MEMBER" }})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                val generalMembers = members.filter { it.role == "MEMBER" }
                if (generalMembers.isEmpty()) {
                    item {
                        Text(
                            text = "No general members listed yet.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(generalMembers, key = { "${it.groupId}_${it.userId}" }) { member ->
                        GroupMemberRow(
                            member = member,
                            isCurrentUserAdmin = isAdmin,
                            currentUserId = currentUserId,
                            onPromote = { role -> viewModel.onPromoteDemoteMember(group.id, member, role) },
                            onRemove = { viewModel.onRemoveGroupMember(group.id, member) },
                            onBlock = { viewModel.onBlockGroupMember(group.id, member) }
                        )
                    }
                }
            }

            // TAB 2: ABOUT & RULES
            if (selectedDetailTab == 2) {
                item {
                    GroupAboutCard(
                        group = group,
                        memberCount = members.size.coerceAtLeast(group.memberCount),
                        leadershipCount = members.count { it.role in listOf("OWNER", "ADMIN", "MODERATOR") }
                    )
                }
            }

            // TAB 3: ADMIN HUB
            if (selectedDetailTab == 3 && isModerator) {
                item {
                    GroupAdminHubCard(
                        group = group,
                        joinRequests = joinRequests,
                        onApproveRequest = { viewModel.onApproveJoinRequest(it) },
                        onRejectRequest = { viewModel.onRejectJoinRequest(it) },
                        onEditSettings = { showEditGroupDialog = true },
                        onDeleteGroup = { showDeleteConfirmDialog = true },
                        isOwner = isOwner
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreatePostDialog) {
        CreateGroupPostDialog(
            group = group,
            onDismiss = { showCreatePostDialog = false },
            onPost = { content, mediaType, mediaUrls, location ->
                viewModel.onCreateGroupPost(
                    groupId = group.id,
                    content = content,
                    mediaType = mediaType,
                    mediaUrls = mediaUrls,
                    location = location
                )
                showCreatePostDialog = false
            }
        )
    }

    if (activePostForComments != null) {
        val activeComments by viewModel.selectedGroupPostComments.collectAsState()
        GroupPostCommentsDialog(
            post = activePostForComments!!,
            comments = activeComments,
            isModerator = isModerator,
            currentUserId = currentUserId,
            onDismiss = {
                activePostForComments = null
                viewModel.selectGroupPostForComments(null)
            },
            onAddComment = { content, parentId, replyToName ->
                viewModel.onAddGroupPostComment(activePostForComments!!, content, parentId, replyToName)
            },
            onDeleteComment = { commentId ->
                viewModel.onDeleteGroupPostComment(commentId, activePostForComments!!)
            }
        )
    }

    if (showInviteDialog) {
        InviteFriendsDialog(
            group = group,
            users = allUsers.filter { it.id != currentUserId && members.none { m -> m.userId == it.id } },
            onDismiss = { showInviteDialog = false },
            onInvite = { targetUser ->
                viewModel.onInviteUserToGroup(group.id, targetUser)
                Toast.makeText(context, "Invitation sent to ${targetUser.fullName}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showReportDialog) {
        ReportGroupDialog(
            group = group,
            onDismiss = { showReportDialog = false },
            onReport = { reason, details ->
                viewModel.onReportGroup(group.id, reason, details)
                showReportDialog = false
                Toast.makeText(context, "Report submitted. Thank you for keeping our community safe.", Toast.LENGTH_LONG).show()
            }
        )
    }

    if (showEditGroupDialog) {
        EditGroupDialog(
            group = group,
            onDismiss = { showEditGroupDialog = false },
            onSave = { name, desc, avatar, cover, cat, loc, isPriv, onlyAdminPost ->
                viewModel.onUpdateGroupDetails(
                    groupId = group.id,
                    name = name,
                    description = desc,
                    avatarUrl = avatar,
                    coverUrl = cover,
                    category = cat,
                    location = loc,
                    isPrivate = isPriv,
                    onlyAdminsCanPost = onlyAdminPost
                )
                showEditGroupDialog = false
                Toast.makeText(context, "Group settings updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Group?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${group.name}'? This action cannot be undone and will delete all group posts and memberships.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.onDeleteGroup(group.id)
                        onBack()
                        Toast.makeText(context, "Group deleted.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLeaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirmDialog = false },
            title = { Text("Leave Group?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave '${group.name}'? You can rejoin anytime if it is public.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirmDialog = false
                        viewModel.onJoinGroup(group)
                        Toast.makeText(context, "You left the group.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed)
                ) {
                    Text("Leave Group")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================================
// GROUP HERO HEADER COMPOSABLE
// =========================================================================
@Composable
fun GroupHeroHeader(
    group: Group,
    myRole: String,
    isMember: Boolean,
    onJoinToggle: () -> Unit,
    onInvite: () -> Unit,
    onOpenAdminTools: () -> Unit,
    pendingRequestsCount: Int,
    isAdmin: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                AsyncImage(
                    model = group.coverUrl.ifBlank { "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800" },
                    contentDescription = "Group Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // Category & Privacy badges on top right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = group.category,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = if (group.isPrivate) AmberOrange.copy(alpha = 0.9f) else EmeraldGreen.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                if (group.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                if (group.isPrivate) "Private" else "Public",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Avatar overlay at bottom
                AsyncImage(
                    model = group.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150" },
                    contentDescription = "Group Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 12.dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                )
            }

            // Info & Primary Actions
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "${group.memberCount} members",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (group.location.isNotBlank()) {
                                Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.Default.LocationOn, null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                                Text(
                                    text = group.location,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // My Role Badge
                    if (myRole != "NONE") {
                        Surface(
                            color = when (myRole) {
                                "OWNER" -> AmberOrange.copy(alpha = 0.15f)
                                "ADMIN" -> BrandBlue.copy(alpha = 0.15f)
                                "MODERATOR" -> EmeraldGreen.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when (myRole) {
                                    "OWNER" -> "👑 Owner"
                                    "ADMIN" -> "🛡️ Admin"
                                    "MODERATOR" -> "⚔️ Moderator"
                                    else -> "👤 Member"
                                },
                                color = when (myRole) {
                                    "OWNER" -> AmberOrange
                                    "ADMIN" -> BrandBlue
                                    "MODERATOR" -> EmeraldGreen
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (group.description.isNotBlank()) {
                    Text(
                        text = group.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isMember) {
                        Button(
                            onClick = onJoinToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Joined", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        val isRequested = group.joinStatus == "REQUESTED"
                        Button(
                            onClick = onJoinToggle,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRequested) AmberOrange.copy(alpha = 0.2f) else BrandBlue,
                                contentColor = if (isRequested) AmberOrange else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isRequested) Icons.Default.HourglassTop else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isRequested -> "Requested"
                                    group.isPrivate -> "Request to Join"
                                    else -> "Join Group"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = onInvite,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Invite", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    if (isAdmin) {
                        BadgedBox(
                            badge = {
                                if (pendingRequestsCount > 0) {
                                    Badge { Text("$pendingRequestsCount") }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = onOpenAdminTools,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Tools", tint = BrandBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// GROUP POST CARD
// =========================================================================
@Composable
fun GroupPostCard(
    post: GroupPost,
    isModerator: Boolean,
    currentUserId: String,
    onLike: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit,
    onCommentClick: () -> Unit
) {
    val isMyPost = post.authorId == currentUserId
    val mediaUrls = remember(post.mediaUrlsJson) {
        try {
            val arr = JSONArray(post.mediaUrlsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("group_post_${post.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Pinned Notice Tag
            if (post.isPinned) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandBlue.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = BrandBlue, modifier = Modifier.size(16.dp))
                    Text("PINNED POST", color = BrandBlue, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }

            // Post Header (Author, Badge, Location, Options)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = post.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                        contentDescription = "Author Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (post.authorRole in listOf("OWNER", "ADMIN", "MODERATOR")) {
                                Surface(
                                    color = when (post.authorRole) {
                                        "OWNER" -> AmberOrange.copy(alpha = 0.15f)
                                        "ADMIN" -> BrandBlue.copy(alpha = 0.15f)
                                        else -> EmeraldGreen.copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = post.authorRole,
                                        color = when (post.authorRole) {
                                            "OWNER" -> AmberOrange
                                            "ADMIN" -> BrandBlue
                                            else -> EmeraldGreen
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = formatPostTimestamp(post.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (post.location.isNotBlank()) {
                                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Icon(Icons.Default.LocationOn, null, tint = BrandBlue, modifier = Modifier.size(12.dp))
                                Text(post.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 3-dots menu
                var showPostMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showPostMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Post Options")
                }
                DropdownMenu(expanded = showPostMenu, onDismissRequest = { showPostMenu = false }) {
                    if (isModerator) {
                        DropdownMenuItem(
                            text = { Text(if (post.isPinned) "Unpin from Group" else "Pin to Top of Group") },
                            leadingIcon = { Icon(Icons.Default.PushPin, null) },
                            onClick = {
                                showPostMenu = false
                                onPinToggle()
                            }
                        )
                    }
                    if (isMyPost || isModerator) {
                        DropdownMenuItem(
                            text = { Text("Delete Post", color = RoseRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = RoseRed) },
                            onClick = {
                                showPostMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Post Content Text
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            // Rich Media Grid
            if (mediaUrls.isNotEmpty()) {
                when (mediaUrls.size) {
                    1 -> {
                        AsyncImage(
                            model = mediaUrls[0],
                            contentDescription = "Post Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    2 -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            mediaUrls.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Post Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AsyncImage(
                                model = mediaUrls[0],
                                contentDescription = "Post Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                mediaUrls.drop(1).take(2).forEach { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Post Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

            // Action counts & buttons (Like, Comment, Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                TextButton(
                    onClick = onLike,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (post.isLikedByMe) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (post.likesCount > 0) "${post.likesCount} Like${if (post.likesCount > 1) "s" else ""}" else "Like",
                        fontWeight = if (post.isLikedByMe) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                }

                // Comment Button
                TextButton(
                    onClick = onCommentClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (post.commentsCount > 0) "${post.commentsCount} Comment${if (post.commentsCount > 1) "s" else ""}" else "Comment",
                        fontSize = 12.sp
                    )
                }

                // Share Button
                TextButton(
                    onClick = onCommentClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", fontSize = 12.sp)
                }
            }
        }
    }
}

// =========================================================================
// GROUP MEMBER ROW
// =========================================================================
@Composable
fun GroupMemberRow(
    member: GroupMember,
    isCurrentUserAdmin: Boolean,
    currentUserId: String,
    onPromote: (String) -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    val isSelf = member.userId == currentUserId
    val isOwner = member.role == "OWNER"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                model = member.userAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                contentDescription = "Member Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(member.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    if (isSelf) {
                        Text("(You)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(
                    color = when (member.role) {
                        "OWNER" -> AmberOrange.copy(alpha = 0.15f)
                        "ADMIN" -> BrandBlue.copy(alpha = 0.15f)
                        "MODERATOR" -> EmeraldGreen.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (member.role) {
                            "OWNER" -> "👑 Group Owner"
                            "ADMIN" -> "🛡️ Admin"
                            "MODERATOR" -> "⚔️ Moderator"
                            else -> "👤 Member"
                        },
                        color = when (member.role) {
                            "OWNER" -> AmberOrange
                            "ADMIN" -> BrandBlue
                            "MODERATOR" -> EmeraldGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (isCurrentUserAdmin && !isSelf && !isOwner) {
            var showMemberMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showMemberMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Manage Member")
            }
            DropdownMenu(expanded = showMemberMenu, onDismissRequest = { showMemberMenu = false }) {
                if (member.role != "MODERATOR") {
                    DropdownMenuItem(
                        text = { Text("Promote to Moderator") },
                        leadingIcon = { Icon(Icons.Default.Shield, null) },
                        onClick = {
                            showMemberMenu = false
                            onPromote("MODERATOR")
                        }
                    )
                }
                if (member.role != "ADMIN") {
                    DropdownMenuItem(
                        text = { Text("Promote to Admin") },
                        leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) },
                        onClick = {
                            showMemberMenu = false
                            onPromote("ADMIN")
                        }
                    )
                }
                if (member.role != "MEMBER") {
                    DropdownMenuItem(
                        text = { Text("Demote to Member") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        onClick = {
                            showMemberMenu = false
                            onPromote("MEMBER")
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Remove from Group", color = RoseRed) },
                    leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = RoseRed) },
                    onClick = {
                        showMemberMenu = false
                        onRemove()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Block Member", color = RoseRed) },
                    leadingIcon = { Icon(Icons.Default.Block, null, tint = RoseRed) },
                    onClick = {
                        showMemberMenu = false
                        onBlock()
                    }
                )
            }
        }
    }
}

// =========================================================================
// GROUP ABOUT CARD
// =========================================================================
@Composable
fun GroupAboutCard(
    group: Group,
    memberCount: Int,
    leadershipCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("About this group", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Text(
                text = group.description.ifBlank { "No description provided for this group." },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Details rows
            AboutDetailRow(
                icon = if (group.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                title = if (group.isPrivate) "Private Group" else "Public Group",
                subtitle = if (group.isPrivate) "Only approved members can view group posts, images, and activities." else "Anyone on Yarkhwoon & Chitral can view who's in the group and see posts."
            )

            AboutDetailRow(
                icon = Icons.Default.Category,
                title = "Category: ${group.category}",
                subtitle = "Categorized under ${group.category} discussions and activities."
            )

            if (group.location.isNotBlank()) {
                AboutDetailRow(
                    icon = Icons.Default.LocationOn,
                    title = "Location: ${group.location}",
                    subtitle = "Community centered around ${group.location}."
                )
            }

            AboutDetailRow(
                icon = Icons.Default.Security,
                title = if (group.onlyAdminsCanPost) "Posting: Admins & Moderators Only" else "Posting: All Members Permitted",
                subtitle = if (group.onlyAdminsCanPost) "Only appointed leadership can post updates." else "Any active member can share thoughts, photos, and updates."
            )

            AboutDetailRow(
                icon = Icons.Default.Groups,
                title = "$memberCount total members",
                subtitle = "Includes $leadershipCount admins and moderators managing the space."
            )
        }
    }
}

@Composable
fun AboutDetailRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier
                .size(22.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// =========================================================================
// GROUP ADMIN HUB CARD
// =========================================================================
@Composable
fun GroupAdminHubCard(
    group: Group,
    joinRequests: List<GroupJoinRequest>,
    onApproveRequest: (GroupJoinRequest) -> Unit,
    onRejectRequest: (GroupJoinRequest) -> Unit,
    onEditSettings: () -> Unit,
    onDeleteGroup: () -> Unit,
    isOwner: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pending Join Requests Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pending Member Requests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Surface(
                        color = if (joinRequests.isNotEmpty()) AmberOrange else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${joinRequests.size}",
                            color = if (joinRequests.isNotEmpty()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                if (joinRequests.isEmpty()) {
                    Text(
                        "No pending join requests at this time.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    joinRequests.forEach { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                AsyncImage(
                                    model = req.userAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                                    contentDescription = "Applicant",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                                Column {
                                    Text(req.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (req.userBio.isNotBlank()) {
                                        Text(req.userBio, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onApproveRequest(req) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRejectRequest(req) },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Decline", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin Management Tools Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Group Management & Privacy Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedButton(
                    onClick = onEditSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = BrandBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Group Info & Permissions")
                }

                if (isOwner) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    Text("Danger Zone", color = RoseRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Button(
                        onClick = onDeleteGroup,
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed.copy(alpha = 0.15f), contentColor = RoseRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RoseRed)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Entire Group", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// CREATE GROUP DIALOG
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, avatar: String, cover: String, cat: String, loc: String, isPriv: Boolean, onlyAdminPost: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf(GroupAvatarPresets[0]) }
    var coverUrl by remember { mutableStateOf(GroupCoverPresets[0].first) }
    var isCustomCover by remember { mutableStateOf(false) }
    var isCustomAvatar by remember { mutableStateOf(false) }
    var isProcessingCover by remember { mutableStateOf(false) }
    var isProcessingAvatar by remember { mutableStateOf(false) }

    var category by remember { mutableStateOf("Active & Travel") }
    var location by remember { mutableStateOf("Yarkhoon Valley, Chitral") }
    var isPrivate by remember { mutableStateOf(false) }
    var onlyAdminsCanPost by remember { mutableStateOf(false) }

    // Cover Photo Picker Launcher
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingCover = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "group_cover_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        coverUrl = Uri.fromFile(savedFile).toString()
                        isCustomCover = true
                        Toast.makeText(context, "Group cover photo chosen!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not attach cover image", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingCover = false
                }
            }
        }
    }

    // Group Icon / Avatar Picker Launcher
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingAvatar = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "group_avatar_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        avatarUrl = Uri.fromFile(savedFile).toString()
                        isCustomAvatar = true
                        Toast.makeText(context, "Group icon chosen!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not attach group icon", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingAvatar = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("create_group_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create New Group", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable Form
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Group Name
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Group Name *") },
                            placeholder = { Text("e.g. Chitral Trekking Club") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_group_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Description
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Group Description *") },
                            placeholder = { Text("What is this group about? Share guidelines and goals...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Category Selector
                    item {
                        Text("Category", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(GroupCategories.filter { it != "All" }) { cat ->
                                val isSelected = category == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { category = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Location
                    item {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Optional Location") },
                            placeholder = { Text("e.g. Booni, Mastuj, Chitral") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // 1. GROUP COVER PHOTO (Device Upload + Preview + Preset Option)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Group Cover Photo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (isCustomCover) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "✓ Device Upload",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Cover Photo Preview Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, if (isCustomCover) BrandBlue else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = "Cover photo preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isProcessingCover) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Cover Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { coverPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("upload_cover_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isCustomCover) "Change Cover Photo" else "Upload Cover from Device", fontSize = 12.sp)
                                }
                                if (isCustomCover) {
                                    OutlinedButton(
                                        onClick = {
                                            coverUrl = GroupCoverPresets[0].first
                                            isCustomCover = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("reset_cover_preset_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Use Preset", fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Or pick from scenic presets:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(GroupCoverPresets) { (presetUrl, label) ->
                                    val isSelected = !isCustomCover && coverUrl == presetUrl
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            coverUrl = presetUrl
                                            isCustomCover = false
                                        }
                                    ) {
                                        AsyncImage(
                                            model = presetUrl,
                                            contentDescription = label,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(width = 90.dp, height = 55.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) BrandBlue else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                        )
                                        Text(label, fontSize = 9.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // 2. GROUP ICON / AVATAR (Device Upload + Preview + Preset Option)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Group Icon / Profile Photo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (isCustomAvatar) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "✓ Device Upload",
                                            color = Color(0xFF2E7D32),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Icon Preview
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, if (isCustomAvatar) BrandBlue else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Avatar preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isProcessingAvatar) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }

                                // Icon Action Buttons
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { avatarPickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("upload_avatar_button")
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isCustomAvatar) "Change Group Icon" else "Upload Icon from Device", fontSize = 12.sp)
                                    }
                                    if (isCustomAvatar) {
                                        OutlinedButton(
                                            onClick = {
                                                avatarUrl = GroupAvatarPresets[0]
                                                isCustomAvatar = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("reset_avatar_preset_button")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Use Preset Icon", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Or pick an icon preset:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(GroupAvatarPresets) { presetUrl ->
                                    val isSelected = !isCustomAvatar && avatarUrl == presetUrl
                                    AsyncImage(
                                        model = presetUrl,
                                        contentDescription = "Avatar Preset",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                avatarUrl = presetUrl
                                                isCustomAvatar = false
                                            }
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) BrandBlue else Color.Transparent,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Privacy Settings
                    item {
                        Text("Privacy Options", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isPrivate) "Private Group 🔒" else "Public Group 🌐",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isPrivate) "Only approved members can see posts and members." else "Anyone can see who's in the group and what they post.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        }
                    }

                    // Posting Permission
                    item {
                        Text("Posting Permissions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (onlyAdminsCanPost) "Admins Only Post 🛡️" else "All Members Post ✍️",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (onlyAdminsCanPost) "Only group admins and moderators can post updates." else "All joined members can create posts, share photos and media.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = onlyAdminsCanPost, onCheckedChange = { onlyAdminsCanPost = it })
                        }
                    }
                }

                // Create Action Button
                Button(
                    onClick = {
                        if (name.isNotBlank() && description.isNotBlank()) {
                            onCreate(name, description, avatarUrl, coverUrl, category, location, isPrivate, onlyAdminsCanPost)
                        }
                    },
                    enabled = name.isNotBlank() && description.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("submit_create_group_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create & Publish Group", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// =========================================================================
// EDIT GROUP SETTINGS DIALOG
// =========================================================================
@Composable
fun EditGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String, avatar: String, cover: String, cat: String, loc: String, isPriv: Boolean, onlyAdminPost: Boolean) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(group.name) }
    var description by remember { mutableStateOf(group.description) }
    var avatarUrl by remember { mutableStateOf(group.avatarUrl) }
    var coverUrl by remember { mutableStateOf(group.coverUrl) }
    var isCustomCover by remember { mutableStateOf(group.coverUrl.startsWith("file:") || group.coverUrl.startsWith("content:")) }
    var isCustomAvatar by remember { mutableStateOf(group.avatarUrl.startsWith("file:") || group.avatarUrl.startsWith("content:")) }
    var isProcessingCover by remember { mutableStateOf(false) }
    var isProcessingAvatar by remember { mutableStateOf(false) }

    var category by remember { mutableStateOf(group.category) }
    var location by remember { mutableStateOf(group.location) }
    var isPrivate by remember { mutableStateOf(group.isPrivate) }
    var onlyAdminsCanPost by remember { mutableStateOf(group.onlyAdminsCanPost) }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingCover = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "group_cover_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        coverUrl = Uri.fromFile(savedFile).toString()
                        isCustomCover = true
                        Toast.makeText(context, "Cover photo updated!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not update cover photo", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingCover = false
                }
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessingAvatar = true
                try {
                    val savedFile = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val file = File(context.filesDir, "group_avatar_${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { output -> input.copyTo(output) }
                            file
                        }
                    }
                    if (savedFile != null) {
                        avatarUrl = Uri.fromFile(savedFile).toString()
                        isCustomAvatar = true
                        Toast.makeText(context, "Group icon updated!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not update group icon", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingAvatar = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Group Settings", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Group Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Cover Photo Uploader in Edit
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Group Cover Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = coverUrl,
                                    contentDescription = "Cover preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isProcessingCover) {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { coverPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Cover from Device", fontSize = 12.sp)
                            }
                        }
                    }

                    // Group Icon Uploader in Edit
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Group Icon / Avatar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, BrandBlue, CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Avatar preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isProcessingAvatar) {
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                Button(
                                    onClick = { avatarPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change Icon from Device", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Private Group 🔒", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Only approved members can access posts.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Admins Only Post 🛡️", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Restrict posting permissions to leadership.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = onlyAdminsCanPost, onCheckedChange = { onlyAdminsCanPost = it })
                        }
                    }
                }

                Button(
                    onClick = {
                        onSave(name, description, avatarUrl, coverUrl, category, location, isPrivate, onlyAdminsCanPost)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// CREATE GROUP POST DIALOG
// =========================================================================
@Composable
fun CreateGroupPostDialog(
    group: Group,
    onDismiss: () -> Unit,
    onPost: (content: String, mediaType: String, mediaUrls: List<String>, location: String) -> Unit
) {
    var textContent by remember { mutableStateOf("") }
    var mediaUrls by remember { mutableStateOf(listOf<String>()) }
    var locationTag by remember { mutableStateOf(group.location) }
    var showUrlInput by remember { mutableStateOf(false) }
    var newMediaUrl by remember { mutableStateOf("") }

    val presetImages = listOf(
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800",
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=800",
        "https://images.unsplash.com/photo-1501183007986-d0d080b147f9?w=800",
        "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=800"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("create_group_post_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Create Post", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Posting in ${group.name}", fontSize = 12.sp, color = BrandBlue)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Post Text Field
                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    placeholder = { Text("What's on your mind regarding ${group.name}?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("group_post_content_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Attached Media Preview
                if (mediaUrls.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mediaUrls) { url ->
                            Box(modifier = Modifier.size(70.dp)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Attached",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                )
                                IconButton(
                                    onClick = { mediaUrls = mediaUrls.filter { it != url } },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Add Photos Preset Row
                Text("Quick Add Landscape / Valley Photos", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    items(presetImages) { pUrl ->
                        AsyncImage(
                            model = pUrl,
                            contentDescription = "Preset Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (!mediaUrls.contains(pUrl)) {
                                        mediaUrls = mediaUrls + pUrl
                                    }
                                }
                        )
                    }
                }

                // Emoji Quick Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("🏔️", "🏕️", "📸", "🌲", "🦅", "🏏", "🎶", "❤️").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier.clickable { textContent += emoji }
                        )
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        val mType = if (mediaUrls.isNotEmpty()) "IMAGE" else "NONE"
                        onPost(textContent, mType, mediaUrls, locationTag)
                    },
                    enabled = textContent.isNotBlank() || mediaUrls.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("publish_group_post_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Publish Post", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// GROUP POST COMMENTS DIALOG / SHEET
// =========================================================================
@Composable
fun GroupPostCommentsDialog(
    post: GroupPost,
    comments: List<GroupPostComment>,
    isModerator: Boolean,
    currentUserId: String,
    onDismiss: () -> Unit,
    onAddComment: (content: String, parentId: Int?, replyToName: String?) -> Unit,
    onDeleteComment: (Int) -> Unit
) {
    var commentInput by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<GroupPostComment?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Comments (${comments.size})", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Comments List
                if (comments.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No comments yet. Start the conversation!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            val isMyComment = comment.authorId == currentUserId
                            val isReply = comment.parentCommentId != null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = if (isReply) 28.dp else 0.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AsyncImage(
                                    model = comment.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                                    contentDescription = "Commenter",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(comment.authorName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            if (comment.replyToAuthorName != null) {
                                                Text("Replying to @${comment.replyToAuthorName}", fontSize = 10.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                                            }
                                            Text(comment.content, fontSize = 13.sp)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    ) {
                                        Text(
                                            text = formatPostTimestamp(comment.timestamp),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Reply",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlue,
                                            modifier = Modifier.clickable { replyingToComment = comment }
                                        )
                                        if (isMyComment || isModerator) {
                                            Text(
                                                text = "Delete",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RoseRed,
                                                modifier = Modifier.clickable { onDeleteComment(comment.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Reply Banner
                if (replyingToComment != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replying to ${replyingToComment!!.authorName}", fontSize = 11.sp, color = BrandBlue)
                        IconButton(onClick = { replyingToComment = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = BrandBlue, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        placeholder = { Text("Write a comment...", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (commentInput.isNotBlank()) {
                                onAddComment(
                                    commentInput,
                                    replyingToComment?.id,
                                    replyingToComment?.authorName
                                )
                                commentInput = ""
                                replyingToComment = null
                            }
                        },
                        modifier = Modifier
                            .background(BrandBlue, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// INVITE FRIENDS DIALOG
// =========================================================================
@Composable
fun InviteFriendsDialog(
    group: Group,
    users: List<User>,
    onDismiss: () -> Unit,
    onInvite: (User) -> Unit
) {
    var invitedUserIds by remember { mutableStateOf(setOf<String>()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Invite Friends", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("to '${group.name}'", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (users.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("All available community friends are already in this group!", textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users, key = { it.id }) { user ->
                            val isInvited = invitedUserIds.contains(user.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                    )
                                    Column {
                                        Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(user.bio.ifBlank { "@${user.username}" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (!isInvited) {
                                            invitedUserIds = invitedUserIds + user.id
                                            onInvite(user)
                                        }
                                    },
                                    enabled = !isInvited,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isInvited) EmeraldGreen else BrandBlue
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(if (isInvited) "Invited ✓" else "Invite", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// REPORT GROUP DIALOG
// =========================================================================
@Composable
fun ReportGroupDialog(
    group: Group,
    onDismiss: () -> Unit,
    onReport: (reason: String, details: String) -> Unit
) {
    val reportReasons = listOf(
        "Spam or fake community",
        "Harassment or hate speech",
        "Inappropriate media content",
        "Misinformation or scams",
        "Intellectual property violation",
        "Other safety concern"
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }
    var details by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Report Group", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = RoseRed)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Select a reason:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    items(reportReasons) { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(reason, fontSize = 13.sp)
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            placeholder = { Text("Optional: provide extra details for moderators...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Button(
                    onClick = { onReport(selectedReason, details) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Confidential Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// HELPER COMPONENTS & UTILS
// =========================================================================
@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

private fun formatPostTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
