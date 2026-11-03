package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.User
import com.example.ui.theme.FbBlue

@Composable
fun EditProfileDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    onToggleAccountPrivacy: ((Boolean) -> Unit)? = null,
    onSaveProfile: (
        name: String,
        bio: String,
        location: String,
        jobTitle: String,
        education: String,
        avatarUri: String?,
        coverUri: String?,
        phoneNumber: String
    ) -> Unit
) {
    var isAccountPrivate by remember { mutableStateOf(currentUser.isPrivate) }
    var name by remember { mutableStateOf(currentUser.name) }
    var bio by remember { mutableStateOf(currentUser.bio) }
    var location by remember { mutableStateOf(currentUser.location) }
    var jobTitle by remember { mutableStateOf(currentUser.jobTitle) }
    var education by remember { mutableStateOf(currentUser.education) }
    var phoneNumber by remember { mutableStateOf(currentUser.phoneNumber) }
    var avatarUri by remember { mutableStateOf<String?>(currentUser.avatarUrl) }
    var coverUri by remember { mutableStateOf<String?>(currentUser.coverUrl) }
    var cropAvatarTargetUri by remember { mutableStateOf<Uri?>(null) }
    var cropCoverTargetUri by remember { mutableStateOf<Uri?>(null) }

    // Android Photo Picker for Profile Picture
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cropAvatarTargetUri = uri
        }
    }

    // Android Photo Picker for Cover Photo
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            cropCoverTargetUri = uri
        }
    }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("edit_profile_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = CardDefaults.outlinedCardBorder(enabled = true),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_edit_profile_dialog")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Informational notice on Cloud Firestore vs Storage
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
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
                                text = "Profile details (name, bio, location, job, education, phone, privacy) save directly to Cloud Firestore. Photo uploads require Cloud Storage plan activation.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 1. Cover Photo Section
                    Text(
                        text = "Cover Photo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = coverUri ?: "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1000",
                            contentDescription = "Cover preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        FilledTonalButton(
                            onClick = {
                                coverPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .testTag("pick_cover_photo_btn"),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.7f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change Cover", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // 2. Profile Avatar Section
                    Text(
                        text = "Profile Picture",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = avatarUri ?: currentUser.avatarUrl,
                            contentDescription = "Avatar preview",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, FbBlue, CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        FilledTonalButton(
                            onClick = {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = FbBlue.copy(alpha = 0.12f),
                                contentColor = FbBlue
                            ),
                            modifier = Modifier.testTag("pick_avatar_photo_btn")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose from Gallery", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // 3. Name & Bio Inputs
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location / Village") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it },
                        label = { Text("Profession / Work") },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = education,
                        onValueChange = { education = it },
                        label = { Text("Education / Institution") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Account Privacy Setting (Stage 12)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAccountPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = FbBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAccountPrivate) "Private Account" else "Public Account",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isAccountPrivate)
                                        "Only approved followers can view your posts and activity."
                                    else
                                        "Anyone on Yarkhoon can discover and view your profile.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isAccountPrivate,
                                onCheckedChange = { isAccountPrivate = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = FbBlue
                                ),
                                modifier = Modifier.testTag("edit_profile_privacy_switch")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (isAccountPrivate != currentUser.isPrivate) {
                            onToggleAccountPrivacy?.invoke(isAccountPrivate)
                        }
                        onSaveProfile(name, bio, location, jobTitle, education, avatarUri, coverUri, phoneNumber)
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("save_profile_changes_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saving to Firestore...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Save Profile",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Modal: Avatar Photo Cropper
    cropAvatarTargetUri?.let { uriToCrop ->
        ImageCropperDialog(
            imageUri = uriToCrop,
            initialAspectRatio = CropAspectRatio.CIRCLE,
            allowedAspectRatios = listOf(
                CropAspectRatio.CIRCLE,
                CropAspectRatio.SQUARE,
                CropAspectRatio.FREE
            ),
            title = "Crop Profile Picture",
            onDismiss = { cropAvatarTargetUri = null },
            onCropSuccess = { croppedUri ->
                avatarUri = croppedUri.toString()
                cropAvatarTargetUri = null
            }
        )
    }

    // Modal: Cover Photo Cropper
    cropCoverTargetUri?.let { uriToCrop ->
        ImageCropperDialog(
            imageUri = uriToCrop,
            initialAspectRatio = CropAspectRatio.LANDSCAPE_16_9,
            allowedAspectRatios = listOf(
                CropAspectRatio.LANDSCAPE_16_9,
                CropAspectRatio.FREE,
                CropAspectRatio.SQUARE
            ),
            title = "Crop Cover Photo",
            onDismiss = { cropCoverTargetUri = null },
            onCropSuccess = { croppedUri ->
                coverUri = croppedUri.toString()
                cropCoverTargetUri = null
            }
        )
    }
}
