package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
    onAcceptFriend: ((String) -> Unit)? = null
) {
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
                        TextButton(onClick = onMarkAllAsRead) {
                            Text("Mark all read", fontSize = 13.sp, color = FacebookBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (notifications.isEmpty()) {
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
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No notifications yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "We will notify you when friends interact with you or post updates in Yarkhoon.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
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
                    model = notification.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                val badgeIcon = when (notification.type) {
                    "FRIEND_REQUEST" -> Icons.Filled.PersonAdd
                    "LIKE" -> Icons.Filled.ThumbUp
                    "COMMENT" -> Icons.Filled.ChatBubble
                    "GROUP" -> Icons.Filled.Groups
                    else -> Icons.Filled.Notifications
                }
                val badgeColor = when (notification.type) {
                    "FRIEND_REQUEST" -> FacebookBlue
                    "LIKE" -> Color(0xFF1877F2)
                    "COMMENT" -> Color(0xFF45BD62)
                    "GROUP" -> Color(0xFFF7B928)
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

                // Friend Request Action Buttons
                if (notification.type == "FRIEND_REQUEST" && notification.targetId != null) {
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
                            onClick = { /* Dismiss */ },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Delete", fontSize = 12.sp)
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
