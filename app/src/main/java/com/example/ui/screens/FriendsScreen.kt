package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FriendConnection
import com.example.data.User
import java.text.SimpleDateFormat
import java.util.*

private val EmeraldGreen = Color(0xFF10B981)
private val AmberWarning = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    users: List<User>,
    currentUser: User?,
    friendConnections: List<FriendConnection>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    activeTab: String,
    onTabChange: (String) -> Unit,
    onSendFriendRequest: (User, String) -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onDeclineFriendRequest: (String) -> Unit,
    onCancelFriendRequest: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onShareProfile: (User) -> Unit = {},
    onOpenChatWithUser: (User) -> Unit = {},
    onViewUserProfile: (User) -> Unit = {},
    feedbackMessage: String? = null,
    onDismissFeedback: () -> Unit = {}
) {
    val currentUserId = currentUser?.id ?: "currentUser"

    // Filter lists - robust to currentUser ID variants and immediate UI reaction
    val myFriends = remember(users, friendConnections, currentUserId) {
        users.filter { user ->
            !user.isCurrentUser && (
                user.friendStatus == "FRIENDS" ||
                friendConnections.any {
                    (((it.senderId == currentUserId || it.senderId == "currentUser") && it.receiverId == user.id) ||
                     (it.senderId == user.id && (it.receiverId == currentUserId || it.receiverId == "currentUser"))) &&
                    it.status == "ACCEPTED"
                }
            )
        }
    }

    val receivedRequests = remember(users, friendConnections, currentUserId, myFriends) {
        users.filter { user ->
            !user.isCurrentUser &&
            user.friendStatus != "FRIENDS" &&
            !myFriends.any { it.id == user.id } &&
            (
                user.friendStatus == "RECEIVED" ||
                friendConnections.any {
                    it.senderId == user.id &&
                    (it.receiverId == currentUserId || it.receiverId == "currentUser") &&
                    it.status == "PENDING"
                }
            )
        }
    }

    val sentRequests = remember(users, friendConnections, currentUserId, myFriends) {
        users.filter { user ->
            !user.isCurrentUser &&
            user.friendStatus != "FRIENDS" &&
            !myFriends.any { it.id == user.id } &&
            (
                user.friendStatus == "SENT" ||
                friendConnections.any {
                    (it.senderId == currentUserId || it.senderId == "currentUser") &&
                    it.receiverId == user.id &&
                    it.status == "PENDING"
                }
            )
        }
    }

    val suggestedUsers = remember(users, friendConnections, currentUserId, receivedRequests, sentRequests, myFriends) {
        users.filter { user ->
            !user.isCurrentUser &&
            user.friendStatus != "FRIENDS" &&
            !myFriends.any { it.id == user.id } &&
            !receivedRequests.any { it.id == user.id } &&
            !sentRequests.any { it.id == user.id }
        }
    }

    // Filtered by Search Query
    val queryTrimmed = searchQuery.trim().lowercase()
    val filteredUsers = remember(queryTrimmed, users, activeTab, suggestedUsers, receivedRequests, sentRequests, myFriends) {
        val baseList = when (activeTab) {
            "SUGGESTIONS" -> suggestedUsers
            "RECEIVED" -> receivedRequests
            "SENT" -> sentRequests
            "FRIENDS" -> myFriends
            else -> users.filter { !it.isCurrentUser }
        }
        if (queryTrimmed.isEmpty()) {
            baseList
        } else {
            baseList.filter { u ->
                u.fullName.lowercase().contains(queryTrimmed) ||
                u.username.lowercase().contains(queryTrimmed) ||
                u.bio.lowercase().contains(queryTrimmed) ||
                u.location.lowercase().contains(queryTrimmed) ||
                u.occupation.lowercase().contains(queryTrimmed)
            }
        }
    }

    // Modal Dialog States
    var userToSendRequestTo by remember { mutableStateOf<User?>(null) }
    var userToUnfriend by remember { mutableStateOf<User?>(null) }
    var selectedConnectionForDetails by remember { mutableStateOf<FriendConnection?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("friends_tab_view")
    ) {
        // Feedback message banner if present
        AnimatedVisibility(visible = feedbackMessage != null) {
            feedbackMessage?.let { msg ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.CloudDone, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(20.dp))
                            Text(msg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = onDismissFeedback, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Live Firestore Sync Status Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Text(
                        "Firestore Realtime Connection Sync",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${myFriends.size} Friends • ${receivedRequests.size} Pending",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar with clear button
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by name, @handle, bio or location...", fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = FacebookBlue)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FacebookBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("friends_search_input")
        )

        // Filter Tabs Row
        val tabs = listOf(
            Triple("SUGGESTIONS", "Discover", suggestedUsers.size),
            Triple("RECEIVED", "Requests", receivedRequests.size),
            Triple("SENT", "Sent", sentRequests.size),
            Triple("FRIENDS", "My Friends", myFriends.size),
            Triple("ALL", "All Directory", users.filter { !it.isCurrentUser }.size)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tabs) { (tabKey, label, count) ->
                val isSelected = activeTab == tabKey
                FilterChip(
                    selected = isSelected,
                    onClick = { onTabChange(tabKey) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (count > 0) {
                                Surface(
                                    color = if (isSelected) FacebookBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        count.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FacebookBlue.copy(alpha = 0.12f),
                        selectedLabelColor = FacebookBlue
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("friend_tab_$tabKey")
                )
            }
        }

        Divider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Content Area
        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (searchQuery.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.PeopleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        if (searchQuery.isNotEmpty()) "No community members found for \"$searchQuery\""
                        else when (activeTab) {
                            "RECEIVED" -> "No pending friend requests received."
                            "SENT" -> "No outgoing friend requests pending."
                            "FRIENDS" -> "No friends connected yet. Connect with people in Discover!"
                            else -> "No people available."
                        },
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (searchQuery.isNotEmpty()) {
                        Button(
                            onClick = { onSearchQueryChange("") },
                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                        ) {
                            Text("Clear Search")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("friends_list_column"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredUsers, key = { it.id }) { user ->
                    val connection = friendConnections.firstOrNull {
                        (it.senderId == currentUserId && it.receiverId == user.id) ||
                        (it.senderId == user.id && it.receiverId == currentUserId)
                    }

                    when {
                        // Already connected friends - immediate UI rendering
                        myFriends.any { it.id == user.id } || user.friendStatus == "FRIENDS" -> {
                            FriendConnectedCard(
                                user = user,
                                connection = connection,
                                onMessage = { onOpenChatWithUser(user) },
                                onRemove = { userToUnfriend = user },
                                onInspectStatus = {
                                    connection?.let { selectedConnectionForDetails = it }
                                },
                                onShare = { onShareProfile(user) },
                                onViewProfile = { onViewUserProfile(user) }
                            )
                        }

                        // User received request from this person
                        receivedRequests.any { it.id == user.id } || user.friendStatus == "RECEIVED" -> {
                            FriendRequestCardDetailed(
                                user = user,
                                connection = connection,
                                onAccept = { onAcceptFriendRequest(user.id) },
                                onDecline = { onDeclineFriendRequest(user.id) },
                                onInspectStatus = {
                                    connection?.let { selectedConnectionForDetails = it }
                                },
                                onShare = { onShareProfile(user) },
                                onViewProfile = { onViewUserProfile(user) }
                            )
                        }

                        // Current user sent a request to this person
                        sentRequests.any { it.id == user.id } || user.friendStatus == "SENT" -> {
                            FriendSentRequestCard(
                                user = user,
                                connection = connection,
                                onCancel = { onCancelFriendRequest(user.id) },
                                onInspectStatus = {
                                    connection?.let { selectedConnectionForDetails = it }
                                },
                                onShare = { onShareProfile(user) },
                                onViewProfile = { onViewUserProfile(user) }
                            )
                        }

                        // Suggestions / General Directory
                        else -> {
                            FriendSuggestionCardDetailed(
                                user = user,
                                onAddClick = { userToSendRequestTo = user },
                                onShare = { onShareProfile(user) },
                                onViewProfile = { onViewUserProfile(user) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Send Friend Request with Optional Note
    userToSendRequestTo?.let { targetUser ->
        SendFriendRequestDialog(
            targetUser = targetUser,
            onDismiss = { userToSendRequestTo = null },
            onSend = { introNote ->
                onSendFriendRequest(targetUser, introNote)
                userToSendRequestTo = null
            }
        )
    }

    // Dialog: Unfriend Confirmation
    userToUnfriend?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { userToUnfriend = null },
            icon = { Icon(Icons.Filled.PersonRemove, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Remove Friend?") },
            text = { Text("Are you sure you want to remove ${targetUser.fullName} from your friends list? This will update the connection in Firestore.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveFriend(targetUser.id)
                        userToUnfriend = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToUnfriend = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Firestore Connection Status Details
    selectedConnectionForDetails?.let { conn ->
        FirestoreConnectionStatusDialog(
            connection = conn,
            onDismiss = { selectedConnectionForDetails = null }
        )
    }
}

// -------------------------------------------------------------
// Component Cards
// -------------------------------------------------------------

@Composable
fun FriendRequestCardDetailed(
    user: User,
    connection: FriendConnection?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onInspectStatus: () -> Unit,
    onShare: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_request_card_${user.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                ProfileAvatar(imageUrl = user.avatarUrl, size = 64)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            user.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { onViewProfile() }
                                .weight(1f, fill = false)
                        )
                        Surface(
                            color = AmberWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "RECEIVED",
                                color = AmberWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (user.username.isNotBlank()) {
                        Text("@${user.username}", fontSize = 12.sp, color = FacebookBlue)
                    }

                    if (user.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(user.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (user.bio.isNotBlank()) {
                        Text(
                            user.bio,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Attached Intro Message if present
            if (!connection?.introMessage.isNullOrBlank()) {
                Surface(
                    color = FacebookBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.FormatQuote, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(16.dp))
                        Text(
                            connection?.introMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Status tracking footer & buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(38.dp)
                        .testTag("accept_friend_${user.id}")
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDecline,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("decline_friend_${user.id}")
                ) {
                    Text("Delete", fontSize = 13.sp)
                }

                IconButton(
                    onClick = onInspectStatus,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("inspect_status_${user.id}")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Status Tracking", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun FriendSentRequestCard(
    user: User,
    connection: FriendConnection?,
    onCancel: () -> Unit,
    onInspectStatus: () -> Unit,
    onShare: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_sent_card_${user.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(imageUrl = user.avatarUrl, size = 56)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            user.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.clickable { onViewProfile() }
                        )
                        Surface(
                            color = FacebookBlue.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "PENDING",
                                color = FacebookBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (user.username.isNotBlank()) {
                        Text("@${user.username}", fontSize = 12.sp, color = FacebookBlue)
                    }
                    if (user.location.isNotBlank()) {
                        Text(user.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (!connection?.introMessage.isNullOrBlank()) {
                Text(
                    "Note: \"${connection?.introMessage}\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.CloudDone, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(14.dp))
                    Text("Synced in Firestore", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onInspectStatus, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Filled.Info, contentDescription = "Status Tracking", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("cancel_friend_${user.id}")
                    ) {
                        Text("Cancel Request", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendConnectedCard(
    user: User,
    connection: FriendConnection?,
    onMessage: () -> Unit,
    onRemove: () -> Unit,
    onInspectStatus: () -> Unit,
    onShare: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_connected_card_${user.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(imageUrl = user.avatarUrl, size = 52)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        user.fullName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.clickable { onViewProfile() }
                    )
                    if (user.isVerified) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Verified", tint = FacebookBlue, modifier = Modifier.size(14.dp))
                    }
                }
                if (user.occupation.isNotBlank()) {
                    Text(user.occupation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Text("Connected", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                    Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onMessage, modifier = Modifier.size(36.dp).testTag("message_friend_${user.id}")) {
                    Icon(Icons.Filled.Chat, contentDescription = "Chat", tint = FacebookBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onInspectStatus, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = "Status Tracking", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp).testTag("unfriend_${user.id}")) {
                    Icon(Icons.Filled.PersonRemove, contentDescription = "Unfriend", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun FriendSuggestionCardDetailed(
    user: User,
    onAddClick: () -> Unit,
    onShare: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("friend_suggestion_card_${user.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(imageUrl = user.avatarUrl, size = 52)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable { onViewProfile() }
                )
                if (user.occupation.isNotBlank()) {
                    Text(user.occupation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (user.username.isNotBlank()) {
                    Text("@${user.username}", fontSize = 11.sp, color = FacebookBlue)
                }

                if (user.location.isNotBlank()) {
                    Text(user.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (user.mutualFriendsCount > 0) {
                    Text("${user.mutualFriendsCount} mutual connections", fontSize = 11.sp, color = FacebookBlue, fontWeight = FontWeight.Medium)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = "Share Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("add_friend_${user.id}")
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Dialogs
// -------------------------------------------------------------

@Composable
fun SendFriendRequestDialog(
    targetUser: User,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var introNote by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileAvatar(imageUrl = targetUser.avatarUrl, size = 64)
                Text(
                    "Connect with ${targetUser.fullName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "This connection will be registered and tracked in real-time in Firestore.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = introNote,
                    onValueChange = { introNote = it },
                    label = { Text("Intro message (Optional)") },
                    placeholder = { Text("e.g. Salam! Let's connect on Yarkhoon.") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("intro_message_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSend(introNote) },
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_send_friend_request")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Request", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FirestoreConnectionStatusDialog(
    connection: FriendConnection,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.CloudSync, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Firestore Connection Status", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connection Status:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    val (badgeBg, badgeFg) = when (connection.status) {
                        "ACCEPTED" -> Pair(EmeraldGreen.copy(alpha = 0.15f), EmeraldGreen)
                        "PENDING" -> Pair(FacebookBlue.copy(alpha = 0.15f), FacebookBlue)
                        "DECLINED" -> Pair(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
                        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            connection.status,
                            color = badgeFg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Text("Firestore Path: /friend_requests/${connection.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sender: ${connection.senderName} (@${connection.senderUsername})", fontSize = 12.sp)
                Text("Receiver: ${connection.receiverName} (@${connection.receiverUsername})", fontSize = 12.sp)

                if (connection.introMessage.isNotBlank()) {
                    Text("Intro Note: \"${connection.introMessage}\"", fontSize = 12.sp, color = FacebookBlue)
                }

                Text("Created: ${dateFormat.format(Date(connection.createdAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Last Synced: ${dateFormat.format(Date(connection.updatedAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)) {
                Text("Close")
            }
        }
    )
}
