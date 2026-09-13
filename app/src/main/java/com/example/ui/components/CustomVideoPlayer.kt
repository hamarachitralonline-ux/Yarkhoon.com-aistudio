package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private val FacebookBlue = Color(0xFF1877F2)

private const val DEFAULT_FALLBACK_VIDEO = "https://raw.githubusercontent.com/mediaelement/mediaelement-files/master/big_buck_bunny.mp4"

/**
 * Sanitizes video URLs, replacing defunct or access-denied URLs (e.g. gtv-videos-bucket)
 * with verified high-performance sample media.
 */
fun sanitizeVideoUrl(rawUrl: String?): String {
    if (rawUrl.isNullOrBlank()) return DEFAULT_FALLBACK_VIDEO
    val trimmed = rawUrl.trim()
    if (trimmed.contains("gtv-videos-bucket") || trimmed.contains("ForBiggerBlazes")) {
        return DEFAULT_FALLBACK_VIDEO
    }
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
        return trimmed
    }
    return DEFAULT_FALLBACK_VIDEO
}

/**
 * Helper to build an ExoPlayer instance with a robust User-Agent, redirects enabled,
 * and reliable timeouts to prevent 403 Forbidden errors from strict media CDNs.
 */
fun buildConfiguredExoPlayer(context: android.content.Context): ExoPlayer {
    val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)
    val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
}

/**
 * Modern custom video player component for feed and details view
 * powered by AndroidX Media3 (ExoPlayer).
 */
@OptIn(UnstableApi::class)
@Composable
fun CustomVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    initialMuted: Boolean = false,
    title: String? = null,
    onFullScreenClicked: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Resolve URL with safety sanitizer
    var activeVideoUrl by remember(videoUrl) {
        mutableStateOf(sanitizeVideoUrl(videoUrl))
    }

    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(initialMuted) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgressRatio by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showFullScreenModal by remember { mutableStateOf(false) }

    // Double tap feedback state
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }

    // Create and configure ExoPlayer with custom User-Agent and redirect support
    val exoPlayer = remember {
        buildConfiguredExoPlayer(context).apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = if (initialMuted) 0f else 1f
            playWhenReady = autoPlay
        }
    }

    // Set media item
    LaunchedEffect(activeVideoUrl) {
        errorMessage = null
        try {
            val mediaItem = MediaItem.fromUri(activeVideoUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load video"
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        isEnded = false
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        isEnded = false
                        totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isEnded = true
                        isPlaying = false
                        controlsVisible = true
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isEnded = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                android.util.Log.e("CustomVideoPlayer", "Playback error for $activeVideoUrl: ${error.message}", error)
                if (activeVideoUrl != DEFAULT_FALLBACK_VIDEO) {
                    // Gracefully fallback to verified safe media stream without breaking UI
                    activeVideoUrl = DEFAULT_FALLBACK_VIDEO
                    try {
                        val mediaItem = MediaItem.fromUri(DEFAULT_FALLBACK_VIDEO)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        if (isPlaying) {
                            exoPlayer.play()
                        }
                    } catch (_: Exception) {
                        errorMessage = "Playback error. Tap to retry."
                    }
                } else {
                    errorMessage = "Playback error. Tap to retry."
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Synchronize volume
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Synchronize playback speed
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Update current progress loop
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Clear double-tap indicator
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(800)
            doubleTapFeedback = null
        }
    }

    // Lifecycle handling: pause when leaving screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Fullscreen Dialog Viewer
    if (showFullScreenModal) {
        FullscreenVideoPlayerDialog(
            videoUrl = activeVideoUrl,
            title = title ?: "Video Player",
            initialPositionMs = currentPositionMs,
            isInitiallyMuted = isMuted,
            onDismiss = { resumePositionMs ->
                showFullScreenModal = false
                exoPlayer.seekTo(resumePositionMs)
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .testTag("custom_video_player_container")
    ) {
        // ExoPlayer Surface View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { offset ->
                            val isRightSide = offset.x > (size.width / 2)
                            if (isRightSide) {
                                val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                doubleTapFeedback = "+10s"
                            } else {
                                val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(target)
                                currentPositionMs = target
                                doubleTapFeedback = "-10s"
                            }
                            controlsVisible = true
                        }
                    )
                }
        )

        // Double tap ripple animation indicator
        AnimatedVisibility(
            visible = doubleTapFeedback != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.75f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (doubleTapFeedback == "+10s") Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = doubleTapFeedback ?: "",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = FacebookBlue,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Error message overlay with Retry
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = errorMessage ?: "Playback error",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Button(
                        onClick = {
                            errorMessage = null
                            val fallbackItem = MediaItem.fromUri(DEFAULT_FALLBACK_VIDEO)
                            exoPlayer.setMediaItem(fallbackItem)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Demo Video", fontSize = 12.sp)
                    }
                }
            }
        }

        // Custom Overlay Controls
        AnimatedVisibility(
            visible = controlsVisible && errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            ) {
                // Top Action Bar (Title & Settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = FacebookBlue
                        ) {
                            Icon(
                                Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(4.dp).size(14.dp)
                            )
                        }
                        Text(
                            text = title ?: "Yarkhwoon Video",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    // Speed Pill Selector
                    Box {
                        Surface(
                            onClick = { showSpeedMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.testTag("video_speed_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                                            fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal,
                                            color = if (playbackSpeed == speed) FacebookBlue else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        playbackSpeed = speed
                                        showSpeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Center Play/Pause / Replay Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Rewind 10s button
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            doubleTapFeedback = "-10s"
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Main Center Toggle
                    FilledIconButton(
                        onClick = {
                            if (isEnded) {
                                exoPlayer.seekTo(0)
                                exoPlayer.play()
                                isEnded = false
                                isPlaying = true
                            } else if (isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = FacebookBlue.copy(alpha = 0.9f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("video_play_pause_toggle")
                    ) {
                        Icon(
                            imageVector = when {
                                isEnded -> Icons.Filled.Replay
                                isPlaying -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Forward 10s button
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(target)
                            currentPositionMs = target
                            doubleTapFeedback = "+10s"
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Timeline & Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Slider / Seekbar
                    val progressValue = if (isSeeking) {
                        seekProgressRatio
                    } else if (totalDurationMs > 0) {
                        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progressValue,
                        onValueChange = { ratio ->
                            isSeeking = true
                            seekProgressRatio = ratio
                        },
                        onValueChangeFinished = {
                            if (totalDurationMs > 0) {
                                val targetPosition = (seekProgressRatio * totalDurationMs).toLong()
                                exoPlayer.seekTo(targetPosition)
                                currentPositionMs = targetPosition
                            }
                            isSeeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = FacebookBlue,
                            activeTrackColor = FacebookBlue,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .testTag("video_progress_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Current time / Total time
                        Text(
                            text = "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Mute / Unmute
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("video_mute_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Fullscreen Button
                            IconButton(
                                onClick = {
                                    if (onFullScreenClicked != null) {
                                        onFullScreenClicked()
                                    } else {
                                        showFullScreenModal = true
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("video_fullscreen_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Immersive Fullscreen Video Player Dialog
 */
@OptIn(UnstableApi::class)
@Composable
fun FullscreenVideoPlayerDialog(
    videoUrl: String,
    title: String,
    initialPositionMs: Long = 0L,
    isInitiallyMuted: Boolean = false,
    onDismiss: (resumePositionMs: Long) -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var isEnded by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var isMuted by remember { mutableStateOf(isInitiallyMuted) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgressRatio by remember { mutableFloatStateOf(0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    val safeVideoUrl = remember(videoUrl) { sanitizeVideoUrl(videoUrl) }

    val fullPlayer = remember {
        buildConfiguredExoPlayer(context).apply {
            val mediaItem = MediaItem.fromUri(safeVideoUrl)
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_OFF
            volume = if (isInitiallyMuted) 0f else 1f
            seekTo(initialPositionMs)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(fullPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        totalDurationMs = fullPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isEnded = true
                        isPlaying = false
                        controlsVisible = true
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                android.util.Log.e("FullscreenVideoPlayer", "Playback error for $safeVideoUrl: ${error.message}", error)
                if (safeVideoUrl != DEFAULT_FALLBACK_VIDEO) {
                    try {
                        val fallbackItem = MediaItem.fromUri(DEFAULT_FALLBACK_VIDEO)
                        fullPlayer.setMediaItem(fallbackItem)
                        fullPlayer.prepare()
                        fullPlayer.play()
                    } catch (_: Exception) {}
                }
            }
        }
        fullPlayer.addListener(listener)

        onDispose {
            fullPlayer.removeListener(listener)
            fullPlayer.stop()
            fullPlayer.release()
        }
    }

    // Synchronize volume & speed
    LaunchedEffect(isMuted) {
        fullPlayer.volume = if (isMuted) 0f else 1f
    }
    LaunchedEffect(playbackSpeed) {
        fullPlayer.setPlaybackSpeed(playbackSpeed)
    }

    // Progress update loop
    LaunchedEffect(isPlaying, isSeeking) {
        while (isPlaying && !isSeeking) {
            currentPositionMs = fullPlayer.currentPosition.coerceAtLeast(0L)
            totalDurationMs = fullPlayer.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    // Controls auto-hide
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    Dialog(
        onDismissRequest = { onDismiss(fullPlayer.currentPosition) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("fullscreen_video_dialog")
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = fullPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { controlsVisible = !controlsVisible }
                        )
                    }
            )

            if (isBuffering) {
                CircularProgressIndicator(
                    color = FacebookBlue,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(50.dp)
                )
            }

            // Fullscreen Overlay Controls
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { onDismiss(fullPlayer.currentPosition) }
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // Speed Pill
                        Box {
                            Surface(
                                onClick = { showSpeedMenu = true },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${playbackSpeed}x",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            playbackSpeed = speed
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Center Playback Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val target = (fullPlayer.currentPosition - 10000).coerceAtLeast(0)
                                fullPlayer.seekTo(target)
                                currentPositionMs = target
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (isEnded) {
                                    fullPlayer.seekTo(0)
                                    fullPlayer.play()
                                    isEnded = false
                                    isPlaying = true
                                } else if (isPlaying) {
                                    fullPlayer.pause()
                                    isPlaying = false
                                } else {
                                    fullPlayer.play()
                                    isPlaying = true
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = FacebookBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    isEnded -> Icons.Filled.Replay
                                    isPlaying -> Icons.Filled.Pause
                                    else -> Icons.Filled.PlayArrow
                                },
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val target = (fullPlayer.currentPosition + 10000).coerceAtMost(fullPlayer.duration)
                                fullPlayer.seekTo(target)
                                currentPositionMs = target
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Bottom Bar with Timeline & Exit Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        val progressValue = if (isSeeking) {
                            seekProgressRatio
                        } else if (totalDurationMs > 0) {
                            (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = progressValue,
                            onValueChange = { ratio ->
                                isSeeking = true
                                seekProgressRatio = ratio
                            },
                            onValueChangeFinished = {
                                if (totalDurationMs > 0) {
                                    val target = (seekProgressRatio * totalDurationMs).toLong()
                                    fullPlayer.seekTo(target)
                                    currentPositionMs = target
                                }
                                isSeeking = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = FacebookBlue,
                                activeTrackColor = FacebookBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${formatDuration(currentPositionMs)} / ${formatDuration(totalDurationMs)}",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { isMuted = !isMuted }
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                        contentDescription = if (isMuted) "Unmute" else "Mute",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDismiss(fullPlayer.currentPosition) }
                                ) {
                                    Icon(
                                        Icons.Filled.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format milliseconds into standard duration (mm:ss or hh:mm:ss)
 */
private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = durationMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
