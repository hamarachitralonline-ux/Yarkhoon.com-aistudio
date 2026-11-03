package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.PostType
import com.example.data.User
import com.example.ui.theme.FbBlue

@Composable
fun CreatePostDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    isPublishing: Boolean = false,
    uploadProgress: Float = 0f,
    publishError: String? = null,
    onDismissError: () -> Unit = {},
    initialPostType: PostType = PostType.TEXT,
    initialFeeling: String? = null,
    initialMediaUri: String? = null,
    onPublish: (text: String, type: PostType, mediaUrl: String?, tags: List<String>) -> Unit
) {
    var contentText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialPostType) }
    var selectedFeeling by remember { mutableStateOf(initialFeeling) }
    var showFeelingPicker by remember { mutableStateOf(false) }
    var hashtagText by remember { mutableStateOf("YarkhoonCom") }
    var selectedMediaUrl by remember(initialMediaUri) {
        mutableStateOf<String?>(initialMediaUri)
    }
    var cropTargetUri by remember { mutableStateOf<Uri?>(null) }

    // Android Photo/Video Picker Launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            if (selectedType == PostType.REEL || selectedType == PostType.VIDEO) {
                selectedMediaUrl = uri.toString()
            } else {
                cropTargetUri = uri
            }
        }
    }

    // Auto-launch gallery picker if user clicked Photo or Video shortcut and hasn't selected media yet
    LaunchedEffect(initialPostType) {
        if (initialMediaUri == null) {
            if (initialPostType == PostType.PHOTO) {
                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else if (initialPostType == PostType.VIDEO || initialPostType == PostType.REEL) {
                mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isPublishing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .testTag("create_post_sheet"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = CardDefaults.outlinedCardBorder(enabled = true),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Create New Post",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isPublishing,
                        modifier = Modifier.testTag("close_create_post_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Error Banner with one-tap text publish fallback
                if (publishError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = publishError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // If post has text or feeling, allow user to publish text-only immediately without retyping
                            if (contentText.isNotBlank() || selectedFeeling != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                FilledTonalButton(
                                    onClick = {
                                        selectedMediaUrl = null
                                        selectedType = PostType.TEXT
                                        onDismissError()
                                        val tags = if (hashtagText.isNotBlank()) listOf(hashtagText) else emptyList()
                                        val finalText = if (selectedFeeling != null && !contentText.contains(selectedFeeling!!)) {
                                            if (contentText.isBlank()) selectedFeeling!! else "$contentText\n\n— $selectedFeeling"
                                        } else {
                                            contentText
                                        }
                                        onPublish(finalText, PostType.TEXT, null, tags)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Publish as Text Post Now (Without Media)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Publishing Progress Indicator
                if (isPublishing) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uploadProgress > 0f) "Uploading media to Firebase Storage..." else "Publishing post to Cloud Firestore...",
                                style = MaterialTheme.typography.labelSmall,
                                color = FbBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uploadProgress > 0f) {
                                Text(
                                    text = "${(uploadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FbBlue,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (uploadProgress > 0f) {
                            LinearProgressIndicator(
                                progress = { uploadProgress.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = FbBlue
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = FbBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        PostType.TEXT to "Thought",
                        PostType.PHOTO to "Photo",
                        PostType.VIDEO to "Video",
                        PostType.REEL to "Reel",
                        PostType.BUSINESS_PROMO to "Business"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FbBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Author Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = currentUser.avatarUrl,
                        contentDescription = currentUser.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUser.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedFeeling != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "is $selectedFeeling",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FbBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = currentUser.handle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedFeeling != null) {
                        IconButton(onClick = { selectedFeeling = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear feeling", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Post Content Input
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = {
                        Text(
                            if (selectedType == PostType.BUSINESS_PROMO) {
                                "Describe your service offering, availability, portfolio or promotional announcement..."
                            } else {
                                "What's happening? Share thoughts, news, or community updates..."
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("post_text_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feeling / Activity Quick Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedFeeling != null) "Activity: $selectedFeeling" else "Add Feeling / Activity:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedFeeling != null) FbBlue else MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showFeelingPicker = !showFeelingPicker }) {
                        Text(if (showFeelingPicker) "Close" else "Choose", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (showFeelingPicker || selectedFeeling != null) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf(
                            "😊 Feeling Happy", "🏔️ Exploring Yarkhoon", "☕ Relaxing in Chitral",
                            "🎉 Celebrating", "🐎 At Shandur Polo", "💼 Working"
                        )) { feeling ->
                            FilterChip(
                                selected = selectedFeeling == feeling,
                                onClick = {
                                    selectedFeeling = if (selectedFeeling == feeling) null else feeling
                                },
                                label = { Text(feeling, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Media selection: Direct Gallery Upload button + samples
                if (selectedType != PostType.TEXT) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedType == PostType.REEL) "Reel Video:" else "Post Media:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Button to trigger Android Photo/Video Picker
                        FilledTonalButton(
                            onClick = {
                                val typeFilter = if (selectedType == PostType.REEL) {
                                    ActivityResultContracts.PickVisualMedia.VideoOnly
                                } else {
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                }
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(typeFilter)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = FbBlue.copy(alpha = 0.12f),
                                contentColor = FbBlue
                            ),
                            modifier = Modifier.testTag("choose_media_from_gallery_btn")
                        ) {
                            Icon(
                                imageVector = if (selectedType == PostType.REEL) Icons.Default.VideoFile else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Choose from Gallery",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Media preview container if selected
                    if (selectedMediaUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, FbBlue, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = selectedMediaUrl,
                                contentDescription = "Selected media preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Crop / Adjust button if it's a content image
                                if (selectedType != PostType.REEL) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable {
                                                try {
                                                    cropTargetUri = Uri.parse(selectedMediaUrl)
                                                } catch (e: Exception) {
                                                    // Ignore parsing error
                                                }
                                            }
                                            .testTag("crop_post_media_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Crop,
                                                contentDescription = "Crop photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { selectedMediaUrl = null },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove media",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Badge distinguishing local device media from remote storage
                            Surface(
                                shape = RoundedCornerShape(bottomStart = 10.dp, topEnd = 8.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier.align(Alignment.BottomStart)
                            ) {
                                Text(
                                    text = "Selected from device • Pending Cloud Storage upload",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedMediaUrl == null) {
                        OutlinedButton(
                            onClick = {
                                if (selectedType == PostType.VIDEO || selectedType == PostType.REEL) {
                                    mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                } else {
                                    mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedType == PostType.VIDEO || selectedType == PostType.REEL) Icons.Default.VideoFile else Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedType == PostType.VIDEO || selectedType == PostType.REEL) "Choose Video from Device" else "Choose Photo from Gallery")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = FbBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notice: Remote media upload requires Cloud Storage plan activation. You can publish text thoughts, feelings, and status updates immediately to Cloud Firestore.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Publish Button
                Button(
                    onClick = {
                        val media = if (selectedType != PostType.TEXT) selectedMediaUrl else null
                        val tags = if (hashtagText.isNotBlank()) listOf(hashtagText) else emptyList()
                        val finalText = if (selectedFeeling != null && !contentText.contains(selectedFeeling!!)) {
                            if (contentText.isBlank()) selectedFeeling!! else "$contentText\n\n— $selectedFeeling"
                        } else {
                            contentText
                        }
                        onPublish(finalText, selectedType, media, tags)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("confirm_publish_post_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FbBlue
                    ),
                    enabled = !isPublishing && (contentText.isNotBlank() || selectedFeeling != null || (selectedType != PostType.TEXT && selectedMediaUrl != null))
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Publishing to YARKHOON...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Publish to YARKHOON.COM",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Modal: Image Cropper & Aspect Ratio Adjuster
    cropTargetUri?.let { uriToCrop ->
        ImageCropperDialog(
            imageUri = uriToCrop,
            initialAspectRatio = CropAspectRatio.PORTRAIT_4_5,
            allowedAspectRatios = listOf(
                CropAspectRatio.SQUARE,
                CropAspectRatio.PORTRAIT_4_5,
                CropAspectRatio.LANDSCAPE_16_9,
                CropAspectRatio.FREE
            ),
            title = "Crop Post Photo",
            onDismiss = { cropTargetUri = null },
            onCropSuccess = { croppedUri ->
                selectedMediaUrl = croppedUri.toString()
                cropTargetUri = null
            }
        )
    }
}
