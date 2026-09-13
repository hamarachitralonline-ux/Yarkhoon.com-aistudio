package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.AppNotification
import com.example.ui.screens.Constants

private val FacebookBlue = Color(0xFF1877F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsDialog(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onNotificationClick: (AppNotification) -> Unit,
    onAcceptFriend: ((String) -> Unit)? = null,
    fcmToken: String? = null,
    hasNotificationPermission: Boolean = true,
    onRequestNotificationPermission: (() -> Unit)? = null,
    onTriggerTestNotification: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showTestMenu by remember { mutableStateOf(false) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "FRIEND" -> notifications.filter { it.type.equals("FRIEND_REQUEST", ignoreCase = true) }
            "GROUP" -> notifications.filter {
                it.type.equals("GROUP_MESSAGE", ignoreCase = true) || it.type.equals("GROUP", ignoreCase = true)
            }
            "INTERACTION" -> notifications.filter {
                it.type.equals("LIKE", ignoreCase = true) || it.type.equals("COMMENT", ignoreCase = true)
            }
            else -> notifications
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = {
                        Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (notifications.isNotEmpty()) {
                            TextButton(onClick = onMarkAllAsRead) {
                                Text("Mark all read", fontSize = 13.sp, color = FacebookBlue)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // FCM Cloud Messaging Status & Quick Test Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF45BD62))
                                )
                                Text(
                                    text = "Firebase Cloud Messaging Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Test Triggers Menu Button
                            Box {
                                FilledTonalButton(
                                    onClick = { showTestMenu = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp).testTag("test_fcm_alerts_button")
                                ) {
                                    Icon(
                                        Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Alerts", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }

                                DropdownMenu(
                                    expanded = showTestMenu,
                                    onDismissRequest = { showTestMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("🤝 Friend Request Alert", fontSize = 13.sp) },
                                        onClick = {
                                            showTestMenu = false
                                            onTriggerTestNotification?.invoke("FRIEND_REQUEST")
                                            Toast.makeText(context, "Sent Friend Request FCM Alert", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("💬 Group Message Alert", fontSize = 13.sp) },
                                        onClick = {
                                            showTestMenu = false
                                            onTriggerTestNotification?.invoke("GROUP_MESSAGE")
                                            Toast.makeText(context, "Sent Group Message FCM Alert", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("❤️ Post Like Alert", fontSize = 13.sp) },
                                        onClick = {
                                            showTestMenu = false
                                            onTriggerTestNotification?.invoke("LIKE")
                                            Toast.makeText(context, "Sent Post Like FCM Alert", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("💭 Post Comment Alert", fontSize = 13.sp) },
                                        onClick = {
                                            showTestMenu = false
                                            onTriggerTestNotification?.invoke("COMMENT")
                                            Toast.makeText(context, "Sent Post Comment FCM Alert", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }

                        // Token Info & Copy
                        if (!fcmToken.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "FCM Token: ${fcmToken.take(16)}...${fcmToken.takeLast(6)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("FCM Token", fcmToken))
                                        Toast.makeText(context, "FCM Token copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Permission Warning Banner if notifications are disabled
                if (!hasNotificationPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsOff,
                                    contentDescription = null,
                                    tint = Color(0xFF856404),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Notifications disabled. Enable to receive live social alerts.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF856404)
                                )
                            }
                            Button(
                                onClick = { onRequestNotificationPermission?.invoke() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF856404)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Enable", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${notifications.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "FRIEND",
                        onClick = { selectedFilter = "FRIEND" },
                        label = {
                            val count = notifications.count { it.type.equals("FRIEND_REQUEST", ignoreCase = true) }
                            Text("Friends ($count)", fontSize = 12.sp)
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == "GROUP",
                        onClick = { selectedFilter = "GROUP" },
                        label = {
                            val count = notifications.count {
                                it.type.equals("GROUP_MESSAGE", ignoreCase = true) || it.type.equals("GROUP", ignoreCase = true)
                            }
                            Text("Groups ($count)", fontSize = 12.sp)
                        }
                    )
                    FilterChip(
                        selected = selectedFilter == "INTERACTION",
                        onClick = { selectedFilter = "INTERACTION" },
                        label = {
                            val count = notifications.count {
                                it.type.equals("LIKE", ignoreCase = true) || it.type.equals("COMMENT", ignoreCase = true)
                            }
                            Text("Likes & Comments ($count)", fontSize = 12.sp)
                        }
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // List of Notifications
                if (filteredNotifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                if (selectedFilter == "ALL") "No notifications yet" else "No matching notifications",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Firebase Cloud Messaging will alert you about friend requests, group messages, or likes/comments.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { showTestMenu = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Test FCM Alert", fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredNotifications, key = { it.id }) { notif ->
                            NotificationItemRow(
                                notification = notif,
                                onClick = {
                                    onNotificationClick(notif)
                                    onDismiss()
                                },
                                onAcceptFriend = { targetId ->
                                    onAcceptFriend?.invoke(targetId)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemRow(
    notification: AppNotification,
    onClick: () -> Unit,
    onAcceptFriend: (String) -> Unit
) {
    val bgColor = if (!notification.isRead) {
        FacebookBlue.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_item_${notification.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with type badge
            Box {
                AsyncImage(
                    model = notification.avatarUrl.ifBlank {
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                    },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                val badgeIcon = when (notification.type.uppercase()) {
                    "FRIEND_REQUEST" -> Icons.Filled.PersonAdd
                    "LIKE" -> Icons.Filled.ThumbUp
                    "COMMENT" -> Icons.Filled.ChatBubble
                    "GROUP_MESSAGE", "GROUP" -> Icons.Filled.Groups
                    else -> Icons.Filled.Notifications
                }
                val badgeColor = when (notification.type.uppercase()) {
                    "FRIEND_REQUEST" -> FacebookBlue
                    "LIKE" -> Color(0xFF1877F2)
                    "COMMENT" -> Color(0xFF45BD62)
                    "GROUP_MESSAGE", "GROUP" -> Color(0xFFF7B928)
                    else -> Color(0xFF7B1FA2)
                }

                Surface(
                    shape = CircleShape,
                    color = badgeColor,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            badgeIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Constants.formatTimeAgo(notification.timestamp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                // Type-specific action buttons
                when (notification.type.uppercase()) {
                    "FRIEND_REQUEST" -> {
                        if (!notification.targetId.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onAcceptFriend(notification.targetId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Confirm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onClick,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("View Profile", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    "GROUP_MESSAGE", "GROUP" -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Group", fontSize = 11.sp)
                        }
                    }
                    "LIKE", "COMMENT" -> {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Post", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Unread Dot Indicator
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FacebookBlue)
                )
            }
        }
    }
}
