package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Story
import com.example.data.StoryViewer
import com.example.data.User
import com.example.data.UserStoriesGroup
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenStoryViewerDialog(
    initialGroupIndex: Int,
    storyGroups: List<UserStoriesGroup>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onMarkViewed: (String) -> Unit,
    onReplyToStory: (Story, String) -> Unit,
    onDeleteStory: (String) -> Unit,
    onEditStory: (String, String, String) -> Unit
) {
    if (storyGroups.isEmpty()) {
        onDismiss()
        return
    }

    var groupIndex by remember { mutableIntStateOf(initialGroupIndex.coerceIn(0, storyGroups.size - 1)) }
    val currentGroup = storyGroups.getOrNull(groupIndex) ?: run {
        onDismiss()
        return
    }

    var storyIndex by remember { mutableIntStateOf(0) }
    val currentStory = currentGroup.stories.getOrNull(storyIndex) ?: currentGroup.stories.firstOrNull() ?: run {
        onDismiss()
        return
    }

    var isPaused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editCaptionText by remember { mutableStateOf(currentStory.textCaption) }
    var replyText by remember { mutableStateOf("") }
    var showReplySentToast by remember { mutableStateOf(false) }

    val isMyStory = currentUser?.id == currentStory.authorId || currentStory.authorId == "currentUser"

    // Mark current story as viewed
    LaunchedEffect(currentStory.id) {
        progress = 0f
        editCaptionText = currentStory.textCaption
        onMarkViewed(currentStory.id)
    }

    // Story progress timer (5 seconds duration)
    val storyDurationMs = 5000L
    val stepIntervalMs = 50L

    LaunchedEffect(currentStory.id, isPaused, showViewersSheet, showEditDialog) {
        if (!isPaused && !showViewersSheet && !showEditDialog) {
            val totalSteps = storyDurationMs / stepIntervalMs
            while (progress < 1f) {
                delay(stepIntervalMs)
                if (!isPaused && !showViewersSheet && !showEditDialog) {
                    progress += (1f / totalSteps)
                }
            }
            // Auto advance to next story or next group
            if (storyIndex < currentGroup.stories.size - 1) {
                storyIndex++
                progress = 0f
            } else if (groupIndex < storyGroups.size - 1) {
                groupIndex++
                storyIndex = 0
                progress = 0f
            } else {
                onDismiss()
            }
        }
    }

    // Parse viewers list
    val viewersList = remember(currentStory.viewersJson) {
        val list = mutableListOf<StoryViewer>()
        try {
            val array = JSONArray(currentStory.viewersJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    StoryViewer(
                        userId = obj.optString("userId"),
                        userName = obj.optString("userName", "User"),
                        userAvatarUrl = obj.optString("userAvatarUrl", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("full_screen_story_viewer")
        ) {
            // Main Media / Content View
            val bgColor = try {
                Color(android.graphics.Color.parseColor(currentStory.backgroundColorHex))
            } catch (e: Exception) {
                Color(0xFF1877F2)
            }
            val textColor = try {
                Color(android.graphics.Color.parseColor(currentStory.textColorHex))
            } catch (e: Exception) {
                Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (currentStory.mediaType == "TEXT") bgColor
                        else Color.Black
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (currentStory.mediaType) {
                    "IMAGE" -> {
                        AsyncImage(
                            model = currentStory.mediaUrl.ifBlank { "https://images.unsplash.com/photo-1542224566-6e85f2e6772f?w=1080" },
                            contentDescription = "Story Media",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (currentStory.textCaption.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = if (isMyStory) 70.dp else 120.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                        )
                                    )
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = currentStory.textCaption,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    "VIDEO" -> {
                        // Simulated / Video container with media presentation
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=1080",
                                contentDescription = "Video Story Poster",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                Icons.Default.PlayCircle,
                                contentDescription = "Playing Video",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(72.dp)
                            )
                            if (currentStory.textCaption.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = if (isMyStory) 70.dp else 120.dp)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = currentStory.textCaption,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    else -> { // TEXT story
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentStory.textCaption,
                                color = textColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp
                            )
                        }
                    }
                }
            }

            // Gesture Tap Zones (Left: Prev, Right: Next, Long Press: Pause)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 100.dp)
            ) {
                // Left 40% tap zone -> Previous
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(currentStory.id) {
                            detectTapGestures(
                                onPress = {
                                    isPaused = true
                                    tryAwaitRelease()
                                    isPaused = false
                                },
                                onTap = {
                                    if (storyIndex > 0) {
                                        storyIndex--
                                        progress = 0f
                                    } else if (groupIndex > 0) {
                                        groupIndex--
                                        storyIndex = storyGroups[groupIndex].stories.size - 1
                                        progress = 0f
                                    }
                                }
                            )
                        }
                )

                // Right 60% tap zone -> Next
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight()
                        .pointerInput(currentStory.id) {
                            detectTapGestures(
                                onPress = {
                                    isPaused = true
                                    tryAwaitRelease()
                                    isPaused = false
                                },
                                onTap = {
                                    if (storyIndex < currentGroup.stories.size - 1) {
                                        storyIndex++
                                        progress = 0f
                                    } else if (groupIndex < storyGroups.size - 1) {
                                        groupIndex++
                                        storyIndex = 0
                                        progress = 0f
                                    } else {
                                        onDismiss()
                                    }
                                }
                            )
                        }
                )
            }

            // Top Header: Segmented Progress Bars & User Profile Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Segmented Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentGroup.stories.forEachIndexed { idx, _ ->
                        val segmentProgress = when {
                            idx < storyIndex -> 1f
                            idx == storyIndex -> progress
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f),
                        )
                    }
                }

                // Author Info Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = currentStory.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                            contentDescription = currentStory.authorName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                        )

                        Column {
                            Text(
                                text = currentStory.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            val timeAgo = remember(currentStory.timestamp) {
                                val diff = System.currentTimeMillis() - currentStory.timestamp
                                when {
                                    diff < 60000 -> "Just now"
                                    diff < 3600000 -> "${diff / 60000}m ago"
                                    diff < 86400000 -> "${diff / 3600000}h ago"
                                    else -> "Yesterday"
                                }
                            }
                            Text(
                                text = "$timeAgo • Expires in 24h",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Actions (Menu / Close)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isMyStory) {
                            Box {
                                IconButton(
                                    onClick = {
                                        isPaused = true
                                        showMenuDropdown = true
                                    },
                                    modifier = Modifier.testTag("story_menu_button")
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Story Options", tint = Color.White)
                                }

                                DropdownMenu(
                                    expanded = showMenuDropdown,
                                    onDismissRequest = {
                                        showMenuDropdown = false
                                        isPaused = false
                                    }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Story Caption") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = {
                                            showMenuDropdown = false
                                            showEditDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Story", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showMenuDropdown = false
                                            onDeleteStory(currentStory.id)
                                            onDismiss()
                                        },
                                        modifier = Modifier.testTag("delete_story_item")
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("story_viewer_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close Story", tint = Color.White)
                        }
                    }
                }
            }

            // Bottom Controls Bar:
            // - If My Story: Viewer list button
            // - If Other's Story: Reply text field & quick reactions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (isMyStory) {
                    // Own Story: Viewers Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isPaused = true
                                showViewersSheet = true
                            }
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("story_viewers_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(
                                text = "${currentStory.viewersCount} Viewers",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Open Viewers", tint = Color.White)
                    }
                } else {
                    // Other's Story: Reply Input & Quick Reactions
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Emoji Reactions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("❤️", "🔥", "👏", "😂", "😮", "😍").forEach { emoji ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable {
                                            onReplyToStory(currentStory, emoji)
                                            showReplySentToast = true
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }

                        // Reply text input field
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = replyText,
                                onValueChange = {
                                    replyText = it
                                    isPaused = it.isNotBlank()
                                },
                                placeholder = {
                                    Text("Send message to ${currentStory.authorName.take(12)}...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("story_reply_input"),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                                ),
                                maxLines = 2
                            )

                            IconButton(
                                onClick = {
                                    if (replyText.isNotBlank()) {
                                        onReplyToStory(currentStory, replyText)
                                        replyText = ""
                                        isPaused = false
                                        showReplySentToast = true
                                    }
                                },
                                enabled = replyText.isNotBlank(),
                                modifier = Modifier
                                    .background(
                                        if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .testTag("story_send_reply_button")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send Reply", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Reply Sent Toast Banner
            AnimatedVisibility(
                visible = showReplySentToast,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                LaunchedEffect(showReplySentToast) {
                    delay(1500)
                    showReplySentToast = false
                }
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                        Text("Reply sent via Direct Message!", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Viewers List Bottom Sheet
    if (showViewersSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showViewersSheet = false
                isPaused = false
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .testTag("story_viewers_sheet"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Story Viewers (${viewersList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "24h Expiry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (viewersList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                "No viewers yet. Friends will appear here when they view your story.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewersList) { viewer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = viewer.userAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150" },
                                    contentDescription = viewer.userName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = viewer.userName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    val dateStr = remember(viewer.timestamp) {
                                        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                                        sdf.format(Date(viewer.timestamp))
                                    }
                                    Text(
                                        text = "Viewed at $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Edit Story Caption Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                isPaused = false
            },
            title = { Text("Edit Story Caption") },
            text = {
                OutlinedTextField(
                    value = editCaptionText,
                    onValueChange = { editCaptionText = it },
                    label = { Text("Story Caption / Text") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_story_caption_field"),
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditStory(currentStory.id, editCaptionText, currentStory.backgroundColorHex)
                        showEditDialog = false
                        isPaused = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        isPaused = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
