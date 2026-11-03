package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.Reel
import com.example.data.User
import com.example.ui.theme.FbBlue
import com.example.ui.theme.SocialBookmark
import com.example.ui.theme.SocialHeart
import com.example.ui.theme.VerifiedBadge
import kotlinx.coroutines.delay

@Composable
fun ReelsScreen(
    reels: List<Reel>,
    onLikeClick: (String) -> Unit,
    onCommentClick: (Reel) -> Unit,
    onShareClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    onUploadReelClick: () -> Unit = {},
    blockedUserIds: Set<String> = emptySet(),
    mutedUserIds: Set<String> = emptySet(),
    followingUserIds: Set<String> = emptySet(),
    onAuthorClick: (User) -> Unit = {},
    onToggleFollow: (User) -> Unit = {},
    onReportReel: ((Reel) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visibleReels = remember(reels, blockedUserIds, mutedUserIds) {
        reels.filter { !blockedUserIds.contains(it.author.id) && !mutedUserIds.contains(it.author.id) }
    }

    if (visibleReels.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SlowMotionVideo,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Reels available right now.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onUploadReelClick,
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                    modifier = Modifier.testTag("upload_reel_empty_btn")
                ) {
                    Icon(imageVector = Icons.Default.VideoCall, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Reel from Gallery", color = Color.White)
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { visibleReels.size })
    var isGlobalMuted by remember { mutableStateOf(false) }

    VerticalPager(
        state = pagerState,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_vertical_pager")
    ) { page ->
        val reel = visibleReels[page]
        val isPageSelected = pagerState.currentPage == page

        ReelVideoItem(
            reel = reel,
            isSelected = isPageSelected,
            isMuted = isGlobalMuted,
            onToggleMute = { isGlobalMuted = !isGlobalMuted },
            isFollowing = followingUserIds.contains(reel.author.id),
            onLikeClick = { onLikeClick(reel.id) },
            onCommentClick = { onCommentClick(reel) },
            onShareClick = { onShareClick(reel.id) },
            onBookmarkClick = { onBookmarkClick(reel.id) },
            onAuthorClick = { onAuthorClick(reel.author) },
            onToggleFollow = { onToggleFollow(reel.author) },
            onUploadReelClick = onUploadReelClick,
            onReportClick = onReportReel?.let { callback -> { callback(reel) } }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelVideoItem(
    reel: Reel,
    isSelected: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isFollowing: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onUploadReelClick: () -> Unit = {},
    onReportClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isUserPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showPauseOverlay by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }
    var showReelMenu by remember { mutableStateOf(false) }
    var isVideoReady by remember { mutableStateOf(false) }

    // ExoPlayer reference
    var playerInstance by remember { mutableStateOf<ExoPlayer?>(null) }

    // Initialize or release ExoPlayer based on selection state
    DisposableEffect(isSelected, retryTrigger) {
        if (isSelected && reel.videoMediaUrl.isNotBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(reel.videoMediaUrl)
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ONE
                volume = if (isMuted) 0f else 1f
                playWhenReady = isUserPlaying
                prepare()
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                        if (playbackState == Player.STATE_READY) {
                            isVideoReady = true
                            playbackError = null
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        playbackError = error.message ?: "Failed to play video"
                        isBuffering = false
                    }
                })
            }
            playerInstance = player
        }

        onDispose {
            playerInstance?.release()
            playerInstance = null
            isVideoReady = false
            isBuffering = false
            playbackError = null
        }
    }

    // Keep volume in sync with global mute
    LaunchedEffect(isMuted) {
        playerInstance?.volume = if (isMuted) 0f else 1f
    }

    // Keep playWhenReady synced when selected
    LaunchedEffect(isSelected, isUserPlaying) {
        playerInstance?.playWhenReady = isSelected && isUserPlaying
    }

    // Lifecycle observer to pause when leaving screen or backgrounding
    DisposableEffect(lifecycleOwner, playerInstance) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    playerInstance?.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isSelected && isUserPlaying) {
                        playerInstance?.playWhenReady = true
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reel_item_${reel.id}")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Tap to toggle play/pause
                val player = playerInstance
                if (player != null) {
                    val nextPlayState = !isUserPlaying
                    isUserPlaying = nextPlayState
                    player.playWhenReady = nextPlayState
                    showPauseOverlay = !nextPlayState
                }
            }
    ) {
        // 1. Poster / Fallback Thumbnail Image (visible while loading or if error occurs)
        AsyncImage(
            model = reel.thumbnailMediaUrl.ifBlank { reel.videoMediaUrl },
            contentDescription = reel.caption,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Real Video Player View (Media3 PlayerView via AndroidView)
        if (isSelected && playerInstance != null && playbackError == null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerInstance
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    view.player = playerInstance
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("reel_player_view_${reel.id}")
            )
        }

        // 3. Play/Pause Tap Overlay Indicator
        if (!isUserPlaying && isSelected && playbackError == null) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .align(Alignment.Center)
                    .testTag("reel_play_pause_btn_${reel.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // 4. Loading / Buffering Spinner
        if (isBuffering && isSelected && playbackError == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("reel_buffering_indicator_${reel.id}"),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // 5. Playback Error with Retry Option
        if (playbackError != null && isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp)
                    .align(Alignment.Center)
                    .testTag("reel_error_overlay_${reel.id}"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Playback Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Unable to stream reel video",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playbackError ?: "Network or playback error",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            playbackError = null
                            isBuffering = true
                            retryTrigger++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                        modifier = Modifier.testTag("reel_retry_btn_${reel.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Video", color = Color.White)
                    }
                }
            }
        }

        // 6. Bottom Gradient shadow for contrast
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // 7. Top bar: Title, Upload Button, Mute/Unmute Toggle, Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reels",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Upload Reel Button
                FilledTonalButton(
                    onClick = onUploadReelClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("upload_reel_topbar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCall,
                        contentDescription = "Upload Reel",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Upload",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Mute / Unmute Button
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("reel_mute_btn")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (onReportClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box {
                        IconButton(
                            onClick = { showReelMenu = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("reel_menu_btn_${reel.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showReelMenu,
                            onDismissRequest = { showReelMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Reel", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showReelMenu = false
                                    onReportClick()
                                },
                                modifier = Modifier.testTag("report_reel_btn_${reel.id}")
                            )
                        }
                    }
                }
            }
        }

        // 8. Right-Side Interaction Overlay (Like, Comment, Share, Bookmark, Vinyl)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("reel_like_btn_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (reel.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (reel.isLiked) SocialHeart else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "${reel.likesCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Comment Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onCommentClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("reel_comment_btn_${reel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(
                    text = "${reel.commentsCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Share Action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("reel_share_btn_${reel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "${reel.sharesCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Bookmark Action
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("reel_bookmark_btn_${reel.id}")
            ) {
                Icon(
                    imageVector = if (reel.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (reel.isSaved) SocialBookmark else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Animated Rotating Disc / Vinyl
            val infiniteTransition = rememberInfiniteTransition(label = "disc")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .border(2.dp, Color.White, CircleShape)
                    .rotate(if (isUserPlaying && isSelected) rotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 9. Bottom-Left Overlay: Author info, caption, audio track
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.82f)
                .padding(start = 14.dp, bottom = 90.dp)
        ) {
            // Author row: Avatar, Name, Handle, Follow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = reel.author.avatarUrl,
                    contentDescription = reel.author.name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable { onAuthorClick() },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.clickable { onAuthorClick() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reel.author.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (reel.author.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = VerifiedBadge,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = reel.author.handle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isFollowing) Color.White.copy(alpha = 0.25f) else FbBlue,
                    modifier = Modifier.clickable { onToggleFollow() }
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption
            Text(
                text = reel.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Audio track banner
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reel.audioTrackTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
