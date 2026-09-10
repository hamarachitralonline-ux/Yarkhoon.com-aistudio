package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AiCreationItem
import com.example.data.AppColors.FacebookBlue
import com.example.data.GeneratedMediaResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    creations: List<AiCreationItem>,
    isImageLoading: Boolean,
    latestImageResult: GeneratedMediaResult?,
    isMusicLoading: Boolean,
    latestMusicResult: GeneratedMediaResult?,
    isVideoLoading: Boolean,
    latestVideoResult: GeneratedMediaResult?,
    onGenerateImage: (prompt: String, baseImageUri: Uri?, aspectRatio: String) -> Unit,
    onGenerateMusic: (prompt: String, isFullTrack: Boolean, genre: String) -> Unit,
    onGenerateVideo: (prompt: String, baseImageUri: Uri?, aspectRatio: String) -> Unit,
    onPublishToFeed: (AiCreationItem) -> Unit,
    onOpenGeminiChat: () -> Unit,
    onOpenLiveVoice: () -> Unit,
    onBack: () -> Unit
) {
    // Hidden as requested: AI Image and Video generation options (code preserved)
    val showImageGenOption = false
    val showVideoGenOption = false

    val activeTabs = remember(showImageGenOption, showVideoGenOption) {
        buildList {
            if (showImageGenOption) add(0 to "Images")
            add(1 to "Music (Lyria)")
            if (showVideoGenOption) add(2 to "Video (Veo 3)")
            add(3 to "My Creations")
        }
    }
    var selectedTabId by remember { mutableIntStateOf(if (showImageGenOption) 0 else 1) }
    val context = LocalContext.current

    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Yarkhoon AI Studio", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FacebookBlue.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(11.dp))
                                    Text("Multimodal Suite", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FacebookBlue)
                                }
                            }
                        }
                        Text(
                            text = if (showImageGenOption || showVideoGenOption) "Gemini 3.1 Flash Image • Lyria • Veo 3" else "Lyria Audio & Music Suite",
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
                    // Hidden as requested: Live Voice and Gemini Chat (code preserved)
                    val showLiveVoiceAction = false
                    val showGeminiChatAction = false
                    if (showLiveVoiceAction) {
                        IconButton(onClick = onOpenLiveVoice, modifier = Modifier.testTag("ai_studio_open_voice_btn")) {
                            Icon(Icons.Filled.Mic, contentDescription = "Live Voice", tint = Color(0xFF10B981))
                        }
                    }
                    if (showGeminiChatAction) {
                        IconButton(onClick = onOpenGeminiChat, modifier = Modifier.testTag("ai_studio_open_chat_btn")) {
                            Icon(Icons.Filled.Chat, contentDescription = "Gemini Chat", tint = FacebookBlue)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Row
            val currentTabIndex = activeTabs.indexOfFirst { it.first == selectedTabId }.coerceAtLeast(0)
            PrimaryTabRow(
                selectedTabIndex = currentTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FacebookBlue
            ) {
                activeTabs.forEach { (tabId, title) ->
                    Tab(
                        selected = selectedTabId == tabId,
                        onClick = { selectedTabId = tabId },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTabId == tabId) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabId) {
                0 -> if (showImageGenOption) {
                    ImageStudioTab(
                        isLoading = isImageLoading,
                        latestResult = latestImageResult,
                        onGenerate = onGenerateImage,
                        onPublish = { creation ->
                            onPublishToFeed(creation)
                            snackbarMessage = "Published image creation to Feed successfully!"
                        }
                    )
                }
                1 -> MusicStudioTab(
                    isLoading = isMusicLoading,
                    latestResult = latestMusicResult,
                    onGenerate = onGenerateMusic,
                    onPublish = { creation ->
                        onPublishToFeed(creation)
                        snackbarMessage = "Published Lyria music track to Feed successfully!"
                    }
                )
                2 -> if (showVideoGenOption) {
                    VideoStudioTab(
                        isLoading = isVideoLoading,
                        latestResult = latestVideoResult,
                        onGenerate = onGenerateVideo,
                        onPublish = { creation ->
                            onPublishToFeed(creation)
                            snackbarMessage = "Published Veo 3 Video reel to Feed successfully!"
                        }
                    )
                }
                else -> MyCreationsTab(
                    creations = creations,
                    onPublish = { creation ->
                        onPublishToFeed(creation)
                        snackbarMessage = "Published creation to Feed!"
                    }
                )
            }
        }
    }
}

@Composable
fun ImageStudioTab(
    isLoading: Boolean,
    latestResult: GeneratedMediaResult?,
    onGenerate: (prompt: String, baseImageUri: Uri?, aspectRatio: String) -> Unit,
    onPublish: (AiCreationItem) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val aspectRatios = listOf("1:1", "16:9", "9:16", "4:3")
    val presetStyles = listOf(
        "Tirich Mir Peak in golden hour sunlight with alpine wildflowers",
        "Traditional Chitrali cap and embroidered waistcoat in cinematic portrait",
        "Scenic Yarkhoon river rushing through rugged Hindu Kush canyon",
        "Ancient Mastuj Fort under starry night sky with Milky Way"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Image, contentDescription = null, tint = FacebookBlue)
                        Column {
                            Text("Create & Edit Images", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Powered by gemini-3.1-flash-image-preview", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Describe image or editing instruction...") },
                        placeholder = { Text("e.g. 'A blooming apricot orchard in Upper Chitral with snow peaks in background'") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("image_gen_prompt_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Base Image Upload for Image-to-Image editing
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { photoPickerLauncher.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Image selected for editing", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Tap to change photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { selectedImageUri = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = FacebookBlue)
                                Column {
                                    Text("Upload Photo for Image Editing (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Edit existing photos with text prompts", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Aspect ratio chips
                    Text("Aspect Ratio:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        aspectRatios.forEach { ratio ->
                            FilterChip(
                                selected = selectedAspectRatio == ratio,
                                onClick = { selectedAspectRatio = ratio },
                                label = { Text(ratio, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (prompt.isNotBlank() && !isLoading) {
                                onGenerate(prompt, selectedImageUri, selectedAspectRatio)
                            }
                        },
                        enabled = prompt.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_image_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Image with Gemini...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageUri != null) "Edit Photo" else "Generate Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Preset Prompt Ideas
        item {
            Text("Inspiration Prompts:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            presetStyles.forEach { styleText ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { prompt = styleText }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(16.dp))
                        Text(styleText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Latest Generated Image Preview Card
        if (latestResult?.mediaUrl != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Latest AI Creation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Text("Ready", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AsyncImage(
                            model = latestResult.mediaUrl,
                            contentDescription = latestResult.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(latestResult.prompt, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("Generated with ${latestResult.modelUsed}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val item = AiCreationItem(
                                    type = "IMAGE",
                                    prompt = latestResult.prompt,
                                    mediaUrl = latestResult.mediaUrl,
                                    title = latestResult.title,
                                    modelUsed = latestResult.modelUsed
                                )
                                onPublish(item)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share to Yarkhoon Feed as Post")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicStudioTab(
    isLoading: Boolean,
    latestResult: GeneratedMediaResult?,
    onGenerate: (prompt: String, isFullTrack: Boolean, genre: String) -> Unit,
    onPublish: (AiCreationItem) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var isFullTrack by remember { mutableStateOf(false) } // false = lyria-3-clip-preview, true = lyria-3-pro-preview
    var selectedGenre by remember { mutableStateOf("Traditional Chitrali Sitar") }
    var isPlaying by remember { mutableStateOf(false) }

    val genres = listOf(
        "Traditional Chitrali Sitar",
        "Mountain Folk & Flute",
        "Acoustic Guitar Harmony",
        "Cinematic Valley Soundscape",
        "Lo-Fi Chill Beats"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color(0xFF9333EA))
                        Column {
                            Text("Generate AI Music (Lyria)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (isFullTrack) "Model: lyria-3-pro-preview (Full Track)" else "Model: lyria-3-clip-preview (30s Clip)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Music composition prompt...") },
                        placeholder = { Text("e.g. 'Melodic Chitrali sitar accompanied by gentle mountain stream acoustic rhythm'") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("music_gen_prompt_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Track length switch (Clip vs Pro)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (isFullTrack) "Full Track (lyria-3-pro-preview)" else "Short Clip (lyria-3-clip-preview)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(if (isFullTrack) "Generates complete instrumental piece" else "Generates fast 30s audio loop", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = isFullTrack,
                            onCheckedChange = { isFullTrack = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF9333EA))
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Genre / Style:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(genres) { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = { selectedGenre = genre },
                                label = { Text(genre, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (prompt.isNotBlank() && !isLoading) {
                                onGenerate(prompt, isFullTrack, selectedGenre)
                            }
                        },
                        enabled = prompt.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_music_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Composing with Lyria...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Music Track", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Music Player & Feed Share Card
        if (latestResult != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF9333EA).copy(alpha = 0.15f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    IconButton(onClick = { isPlaying = !isPlaying }) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color(0xFF9333EA),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(latestResult.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Model: ${latestResult.modelUsed}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Waveform simulation bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(24) { i ->
                                val heightPercent = if (isPlaying) ((i * 17) % 24 + 6) else 8
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(heightPercent.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF9333EA).copy(alpha = if (isPlaying) 0.8f else 0.4f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val item = AiCreationItem(
                                    type = "MUSIC",
                                    prompt = latestResult.prompt,
                                    mediaUrl = latestResult.mediaUrl ?: "",
                                    title = latestResult.title,
                                    modelUsed = latestResult.modelUsed
                                )
                                onPublish(item)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post Music Track to Community Feed")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoStudioTab(
    isLoading: Boolean,
    latestResult: GeneratedMediaResult?,
    onGenerate: (prompt: String, baseImageUri: Uri?, aspectRatio: String) -> Unit,
    onPublish: (AiCreationItem) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") } // "16:9" or "9:16"
    var selectedPhotoToAnimate by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedPhotoToAnimate = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, tint = Color(0xFFEA580C))
                        Column {
                            Text("Generate AI Video & Animate (Veo 3)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Powered by veo-3.1-fast-generate-preview", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Video scene or camera animation direction...") },
                        placeholder = { Text("e.g. 'Cinematic drone tracking shot through Shandur Pass with galloping polo horses'") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp)
                            .testTag("video_gen_prompt_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Image-to-Video Animation Upload
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { photoPickerLauncher.launch("image/*") }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (selectedPhotoToAnimate != null) {
                                AsyncImage(
                                    model = selectedPhotoToAnimate,
                                    contentDescription = "Selected Photo",
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Animate this photo into video", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Veo 3 will add realistic motion", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { selectedPhotoToAnimate = null }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Icon(Icons.Filled.MotionPhotosAuto, contentDescription = null, tint = Color(0xFFEA580C))
                                Column {
                                    Text("Animate Photo to Video (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Select an image to bring it to life", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Aspect Ratio (Veo 3 supported):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedAspectRatio == "16:9",
                            onClick = { selectedAspectRatio = "16:9" },
                            label = { Text("16:9 Landscape (Widescreen)", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = selectedAspectRatio == "9:16",
                            onClick = { selectedAspectRatio = "9:16" },
                            label = { Text("9:16 Portrait (Reel)", fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (prompt.isNotBlank() && !isLoading) {
                                onGenerate(prompt, selectedPhotoToAnimate, selectedAspectRatio)
                            }
                        },
                        enabled = prompt.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_video_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rendering Veo 3 Video...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedPhotoToAnimate != null) "Animate Image to Video" else "Generate Video from Text", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Latest Video Result Card
        if (latestResult?.mediaUrl != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(latestResult.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(latestResult.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = "Play Video", tint = Color.White, modifier = Modifier.size(54.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val item = AiCreationItem(
                                    type = "VIDEO",
                                    prompt = latestResult.prompt,
                                    mediaUrl = latestResult.mediaUrl,
                                    title = latestResult.title,
                                    modelUsed = latestResult.modelUsed,
                                    aspectRatio = selectedAspectRatio
                                )
                                onPublish(item)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share as Video Reel to Feed")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyCreationsTab(
    creations: List<AiCreationItem>,
    onPublish: (AiCreationItem) -> Unit
) {
    if (creations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No creations yet. Generate images, music, or video to see them here!", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(creations, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        ) {
                            if (item.type == "MUSIC") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFF4F46E5)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            } else {
                                AsyncImage(
                                    model = item.mediaUrl,
                                    contentDescription = item.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Type badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = item.type,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.prompt, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Model: ${item.modelUsed}", fontSize = 9.sp, color = FacebookBlue, fontWeight = FontWeight.Medium)

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = { onPublish(item) },
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Post to Feed", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
