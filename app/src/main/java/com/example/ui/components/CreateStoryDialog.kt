package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryDialog(
    onDismiss: () -> Unit,
    onPublishStory: (mediaType: String, mediaUri: Uri?, textCaption: String, bgColorHex: String, textColorHex: String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Photo, 1: Text, 2: Video
    var captionText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isPublishing by remember { mutableStateOf(false) }

    val backgroundColors = listOf(
        "#1877F2" to "Chitral Blue",
        "#833AB4" to "Valley Purple",
        "#11998E" to "Pine Green",
        "#FF512F" to "Sunset Coral",
        "#F77737" to "Golden Apricot",
        "#1A1A2E" to "Midnight Sky",
        "#E91E63" to "Rose Berry",
        "#4A00E0" to "Deep Violet"
    )
    var selectedBgColorHex by remember { mutableStateOf(backgroundColors[0].first) }
    var selectedTextColorHex by remember { mutableStateOf("#FFFFFF") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
        }
    }

    Dialog(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Add to Story",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { if (!isPublishing) onDismiss() },
                            modifier = Modifier.testTag("create_story_close_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                isPublishing = true
                                val mediaType = when (selectedTab) {
                                    0 -> "IMAGE"
                                    1 -> "TEXT"
                                    else -> "VIDEO"
                                }
                                val mediaUri = if (selectedTab == 0) selectedImageUri else if (selectedTab == 2) selectedVideoUri else null
                                onPublishStory(
                                    mediaType,
                                    mediaUri,
                                    captionText,
                                    selectedBgColorHex,
                                    selectedTextColorHex
                                )
                            },
                            enabled = !isPublishing && when (selectedTab) {
                                0 -> selectedImageUri != null || captionText.isNotBlank()
                                1 -> captionText.isNotBlank()
                                2 -> selectedVideoUri != null
                                else -> false
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("publish_story_button")
                        ) {
                            if (isPublishing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Share", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Selector (Photo, Text, Video)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Photo")
                            }
                        },
                        modifier = Modifier.testTag("tab_photo_story")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Text")
                            }
                        },
                        modifier = Modifier.testTag("tab_text_story")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Video")
                            }
                        },
                        modifier = Modifier.testTag("tab_video_story")
                    )
                }

                // Story Preview Canvas
                val currentBgColor = try {
                    Color(android.graphics.Color.parseColor(selectedBgColorHex))
                } catch (e: Exception) {
                    Color(0xFF1877F2)
                }
                val currentTextColor = try {
                    Color(android.graphics.Color.parseColor(selectedTextColorHex))
                } catch (e: Exception) {
                    Color.White
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (selectedTab == 1) currentBgColor
                                else Color.Black
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (selectedTab) {
                            0 -> {
                                if (selectedImageUri != null) {
                                    AsyncImage(
                                        model = selectedImageUri,
                                        contentDescription = "Selected Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (captionText.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .align(Alignment.BottomCenter)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = captionText,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Choose Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            "Tap to choose photo from gallery",
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        AssistChip(
                                            onClick = { photoPickerLauncher.launch("image/*") },
                                            label = { Text("Select Image") },
                                            leadingIcon = {
                                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }
                            }
                            1 -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = captionText.ifBlank { "Type your story thoughts here..." },
                                        color = if (captionText.isBlank()) currentTextColor.copy(alpha = 0.6f) else currentTextColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 30.sp
                                    )
                                }
                            }
                            2 -> {
                                if (selectedVideoUri != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = "Video Ready",
                                            tint = Color.White,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text("Video ready to upload", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(
                                            selectedVideoUri?.lastPathSegment ?: "video.mp4",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.clickable { videoPickerLauncher.launch("video/*") }
                                    ) {
                                        Icon(
                                            Icons.Default.VideoLibrary,
                                            contentDescription = "Choose Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Text(
                                            "Tap to choose video from gallery",
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                        AssistChip(
                                            onClick = { videoPickerLauncher.launch("video/*") },
                                            label = { Text("Select Video") },
                                            leadingIcon = {
                                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 24 Hour Notice Badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("Disappears in 24h", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Controls based on tab
                if (selectedTab == 1) {
                    // Color Palette Selector for Text Story
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Background Color:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(backgroundColors) { (hex, name) ->
                                val color = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (e: Exception) {
                                    Color.Blue
                                }
                                val isSelected = selectedBgColorHex == hex

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(color, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedBgColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Caption / Story Text Input
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = {
                        Text(if (selectedTab == 1) "Story Text *" else "Add a caption... (optional)")
                    },
                    placeholder = {
                        Text(if (selectedTab == 1) "Write something exciting..." else "Say something about this...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("story_caption_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4,
                    leadingIcon = {
                        Icon(
                            if (selectedTab == 1) Icons.Default.EditNote else Icons.Default.ChatBubbleOutline,
                            contentDescription = null
                        )
                    }
                )

                // Pick/Change buttons for media
                if (selectedTab == 0 && selectedImageUri != null) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Change Photo")
                    }
                } else if (selectedTab == 2 && selectedVideoUri != null) {
                    OutlinedButton(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Change Video")
                    }
                }

                // Auto Compression / Firebase Storage Info banner
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Media is automatically compressed and synced with Firebase in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
