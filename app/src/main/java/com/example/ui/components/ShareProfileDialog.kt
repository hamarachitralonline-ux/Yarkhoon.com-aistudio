package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.User

private val BrandBlue = Color(0xFF1877F2)
private val DarkSlate = Color(0xFF0F172A)
private val LightSurface = Color(0xFFF8FAFC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProfileDialog(
    user: User,
    allFriends: List<User> = emptyList(),
    onDismiss: () -> Unit,
    onSendToChat: ((User, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var copiedRecently by remember { mutableStateOf(false) }
    var sentToUserIds by remember { mutableStateOf(setOf<String>()) }
    var showQrView by remember { mutableStateOf(false) }

    val profileUrl = remember(user) {
        val identifier = user.username.ifBlank { user.id }
        "https://yarkhoon.com/user/$identifier"
    }

    val shareText = remember(user, profileUrl) {
        "Check out ${user.fullName}'s profile (@${user.username}) on Yarkhoon Social:\n$profileUrl"
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Yarkhoon Profile Link", profileUrl)
        clipboard.setPrimaryClip(clip)
        copiedRecently = true
        Toast.makeText(context, "Profile link copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun launchNativeShare() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_TITLE, "Share ${user.fullName}'s Profile")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Profile via")
        context.startActivity(shareIntent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        modifier = Modifier.testTag("share_profile_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_share_profile_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Profile Card Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_share_card_preview"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BrandBlue.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Avatar
                        Surface(
                            shape = CircleShape,
                            border = BorderStroke(2.dp, BrandBlue.copy(alpha = 0.4f)),
                            modifier = Modifier.size(60.dp)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = user.fullName.ifBlank { "Chitrali Member" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.isVerified || user.id == "user_yarkhoon") {
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = "Verified",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "@${user.username.ifBlank { "member" }}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (user.bio.isNotBlank()) {
                                Text(
                                    text = user.bio,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Public Profile Link Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("public_profile_url_box"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = "Link",
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = profileUrl,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = { copyToClipboard() },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("copy_profile_link_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                if (copiedRecently) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(16.dp),
                                tint = if (copiedRecently) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (copiedRecently) "Copied" else "Copy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Actions: Native Share & Show QR Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { launchNativeShare() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("native_share_profile_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Text("Share via...", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showQrView = true },
                    modifier = Modifier.testTag("show_profile_qr_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = "QR Code", modifier = Modifier.size(18.dp))
                        Text("QR Card", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Send Directly in Yarkhoon Chat
            if (allFriends.isNotEmpty() && onSendToChat != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Send via Direct Message",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(allFriends, key = { it.id }) { friend ->
                            val isSent = sentToUserIds.contains(friend.id)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!isSent) {
                                            onSendToChat(friend, shareText)
                                            sentToUserIds = sentToUserIds + friend.id
                                            Toast.makeText(context, "Profile sent to ${friend.fullName}!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .testTag("send_profile_to_${friend.id}")
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Surface(
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        AsyncImage(
                                            model = friend.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120" },
                                            contentDescription = friend.fullName,
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    if (isSent) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Sent",
                                                tint = Color.White,
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = friend.fullName.split(" ").firstOrNull() ?: friend.fullName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = if (isSent) "Sent" else "Send",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSent) Color(0xFF10B981) else BrandBlue
                                )
                            }
                        }
                    }
                }
            }

            // Quick App Share Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAppShareIcon(
                    label = "WhatsApp",
                    icon = Icons.Outlined.Message,
                    color = Color(0xFF25D366),
                    onClick = { launchNativeShare() }
                )
                QuickAppShareIcon(
                    label = "SMS",
                    icon = Icons.Outlined.Sms,
                    color = Color(0xFF3B82F6),
                    onClick = { launchNativeShare() }
                )
                QuickAppShareIcon(
                    label = "Email",
                    icon = Icons.Outlined.Email,
                    color = Color(0xFFEA4335),
                    onClick = { launchNativeShare() }
                )
                QuickAppShareIcon(
                    label = "More",
                    icon = Icons.Outlined.MoreHoriz,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { launchNativeShare() }
                )
            }
        }
    }

    // QR Code Dialog Preview
    if (showQrView) {
        Dialog(
            onDismissRequest = { showQrView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
                    .testTag("profile_qr_code_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile QR Card", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { showQrView = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Simulated Clean QR Code Visual Card
                    Card(
                        modifier = Modifier
                            .size(220.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.QrCode2,
                                contentDescription = "QR Code",
                                tint = DarkSlate,
                                modifier = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "yarkhoon.com/user/@${user.username}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = "Point camera to scan & open @${user.username}'s profile",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            copyToClipboard()
                            showQrView = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Copy Link & Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAppShareIcon(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
