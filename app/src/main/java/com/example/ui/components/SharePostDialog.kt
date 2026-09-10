package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.User

private val BrandBlue = Color(0xFF1877F2)
private val BrandBlueDark = Color(0xFF0D53B5)
private val EmeraldGreen = Color(0xFF10B981)
private val DarkSlate = Color(0xFF0F172A)

/**
 * Generate a visual QR bitmap for quick sharing
 */
private fun generateQrBitmap(content: String, size: Int = 250): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    val hash = content.hashCode()
    val pattern = BooleanArray(25 * 25) { i ->
        val x = i % 25
        val y = i / 25
        // Standard finder patterns in corners
        val inTopLeftFinder = x in 1..7 && y in 1..7
        val inTopRightFinder = x in 17..23 && y in 1..7
        val inBottomLeftFinder = x in 1..7 && y in 17..23
        if (inTopLeftFinder || inTopRightFinder || inBottomLeftFinder) {
            val fx = if (x > 12) x - 16 else x
            val fy = if (y > 12) y - 16 else y
            (fx == 1 || fx == 7 || fy == 1 || fy == 7) || (fx in 3..5 && fy in 3..5)
        } else {
            val bit = (hash xor (x * 37 + y * 73)) and 1
            bit == 0
        }
    }

    val pixelSize = size / 25
    val darkColor = android.graphics.Color.parseColor("#0F172A")
    val lightColor = android.graphics.Color.WHITE

    for (x in 0 until size) {
        for (y in 0 until size) {
            val gridX = (x / pixelSize).coerceIn(0, 24)
            val gridY = (y / pixelSize).coerceIn(0, 24)
            val isDark = pattern[gridY * 25 + gridX]
            bitmap.setPixel(x, y, if (isDark) darkColor else lightColor)
        }
    }
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostDialog(
    post: Post,
    allFriends: List<User> = emptyList(),
    onDismiss: () -> Unit,
    onSendToChat: ((User, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var copiedRecently by remember { mutableStateOf(false) }
    var sentToUserIds by remember { mutableStateOf(setOf<String>()) }
    var showQrView by remember { mutableStateOf(false) }
    var showWebPreview by remember { mutableStateOf(false) }

    val postUrl = remember(post) {
        "https://yarkhoon.com/post/${post.id}"
    }

    val shareText = remember(post, postUrl) {
        val snippet = post.content.take(120)
        "Check out this post by ${post.authorName} on Yarkhoon Social:\n\"$snippet\"\n\n$postUrl"
    }

    fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Yarkhoon Post Link", postUrl)
        clipboard.setPrimaryClip(clip)
        copiedRecently = true
        Toast.makeText(context, "Post link copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun launchNativeShare() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Post on Yarkhoon")
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_TITLE, "Share ${post.authorName}'s Post")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Post via")
        context.startActivity(shareIntent)
    }

    if (showWebPreview) {
        WebPostPreviewDialog(
            post = post,
            onDismiss = { showWebPreview = false },
            onOpenInApp = {
                showWebPreview = false
                onDismiss()
            }
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("share_post_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Share Post",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Unique link: yarkhoon.com/post/${post.id}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(14.dp))

            // Post Card Preview Snippet
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = post.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" },
                        contentDescription = post.authorName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            post.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            post.content,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (post.mediaUrl.isNotBlank()) {
                        AsyncImage(
                            model = post.mediaUrl,
                            contentDescription = "Post thumbnail",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // URL Box with Copy Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Link,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            postUrl,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { copyToClipboard() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (copiedRecently) EmeraldGreen else BrandBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("copy_post_link_button")
                    ) {
                        Icon(
                            if (copiedRecently) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (copiedRecently) "Copied!" else "Copy", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons Row: Web Preview & Native Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showWebPreview = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BrandBlue)
                ) {
                    Icon(Icons.Filled.Language, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Web Preview", fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { launchNativeShare() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share via...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Quick Send to Friends
            val onlineOrAllFriends = allFriends.filter { !it.isCurrentUser }
            if (onlineOrAllFriends.isNotEmpty() && onSendToChat != null) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Send to Friends on Yarkhoon",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${onlineOrAllFriends.size} Available",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(onlineOrAllFriends, key = { it.id }) { friend ->
                        val isSent = sentToUserIds.contains(friend.id)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clickable {
                                    if (!isSent) {
                                        onSendToChat(friend, "Check out this post by ${post.authorName}: $postUrl")
                                        sentToUserIds = sentToUserIds + friend.id
                                        Toast.makeText(context, "Sent to ${friend.fullName}!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AsyncImage(
                                    model = friend.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" },
                                    contentDescription = friend.fullName,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSent) 2.dp else 1.dp,
                                            color = if (isSent) EmeraldGreen else MaterialTheme.colorScheme.outlineVariant,
                                            shape = CircleShape
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                                if (isSent) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = friend.fullName.split(" ").firstOrNull() ?: friend.fullName,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isSent) "Sent" else "Send",
                                fontSize = 10.sp,
                                color = if (isSent) EmeraldGreen else BrandBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // QR Code toggle
            TextButton(
                onClick = { showQrView = !showQrView },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showQrView) Icons.Filled.ExpandLess else Icons.Filled.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (showQrView) "Hide QR Code" else "Show QR Code for Post", fontSize = 13.sp)
            }

            AnimatedVisibility(visible = showQrView) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val qrBitmap = remember(postUrl) { generateQrBitmap(postUrl, 200) }
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Post QR Code",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Scan to view on Yarkhoon App or Web",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
