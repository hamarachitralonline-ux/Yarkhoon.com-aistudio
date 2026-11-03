package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.annotation.OptIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.PostType
import com.example.ui.theme.*

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onRepostClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onAuthorClick: (() -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onReactionSelect: ((String) -> Unit)? = null
) {
    var showPostMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showReactionMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (post.isPinned || post.isCurrentlyFeatured || post.isTrendingLabeled || post.isCurrentlyPromoted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.isPinned) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "📌 Pinned to Top",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (post.isCurrentlyFeatured) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "⭐ Featured",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (post.isTrendingLabeled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "🔥 Trending",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (post.isCurrentlyPromoted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDBEAFE)
                        ) {
                            Text(
                                text = "📢 Promoted",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Header: Author Avatar, Name, Handle, Badges, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.author.avatarUrl,
                    contentDescription = post.author.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable(enabled = onAuthorClick != null) { onAuthorClick?.invoke() },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = onAuthorClick != null) { onAuthorClick?.invoke() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.author.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (post.author.isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified Member",
                                tint = VerifiedBadge,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        if (post.author.isBusinessAccount) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BusinessBadge.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BusinessBadge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite Creator",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.author.handle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = post.createdAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!post.location.isNullOrBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = post.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (post.type == PostType.BUSINESS_PROMO) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FbBlueLight
                    ) {
                        Text(
                            text = "Promoted",
                            style = MaterialTheme.typography.labelSmall,
                            color = FbBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                if (onReportClick != null || onToggleFavorite != null) {
                    Box {
                        IconButton(
                            onClick = { showPostMenu = true },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("post_menu_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Post options",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showPostMenu,
                            onDismissRequest = { showPostMenu = false }
                        ) {
                            if (onToggleFavorite != null) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = null,
                                            tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showPostMenu = false
                                        onToggleFavorite()
                                    },
                                    modifier = Modifier.testTag("favorite_post_menu_item_${post.id}")
                                )
                            }

                            DropdownMenuItem(
                                text = { Text(if (post.isSaved) "Remove from Saved" else "Save Post") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (post.isSaved) SocialBookmark else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showPostMenu = false
                                    onBookmarkClick()
                                },
                                modifier = Modifier.testTag("bookmark_post_menu_item_${post.id}")
                            )

                            if (onReportClick != null) {
                                DropdownMenuItem(
                                    text = { Text("Report Post", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showPostMenu = false
                                        onReportClick()
                                    },
                                    modifier = Modifier.testTag("report_post_menu_item_${post.id}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post Text Content
            if (post.text.isNotBlank()) {
                com.example.util.InteractivePostText(
                    text = post.text,
                    style = MaterialTheme.typography.bodyLarge,
                    onMentionClick = onMentionClick,
                    onHashtagClick = onHashtagClick
                )
            }

            // Embedded Original Post Preview if this is a repost
            if (post.originalPost != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = post.originalPost.author.avatarUrl,
                                contentDescription = post.originalPost.author.name,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = post.originalPost.author.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = post.originalPost.author.handle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (post.originalPost.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            com.example.util.InteractivePostText(
                                text = post.originalPost.text,
                                style = MaterialTheme.typography.bodyMedium,
                                onMentionClick = onMentionClick,
                                onHashtagClick = onHashtagClick
                            )
                        }
                        if (post.originalPost.mediaUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = post.originalPost.mediaUrls.first(),
                                contentDescription = "Original post media",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Hashtags
            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    post.tags.take(4).forEach { tag ->
                        val displayTag = if (tag.startsWith("#")) tag else "#$tag"
                        Text(
                            text = displayTag,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = FbBlue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = onHashtagClick != null) {
                                    onHashtagClick?.invoke(tag)
                                }
                                .padding(vertical = 2.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            // Media Container
            if (post.mediaUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val firstMedia = post.mediaUrls.first()
                val isVideo = post.type == PostType.VIDEO || post.type == PostType.REEL ||
                    firstMedia.endsWith(".mp4", ignoreCase = true) || firstMedia.contains("video", ignoreCase = true)

                if (isVideo) {
                    VideoMediaCard(mediaUrl = firstMedia)
                } else {
                    AsyncImage(
                        model = firstMedia,
                        contentDescription = "Post media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Social Interaction Bar (Like, Comment, Repost, Share, Bookmark)
            val likeTint by animateColorAsState(
                targetValue = if (post.isLiked) SocialHeart else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "likeTint"
            )
            val repostTint by animateColorAsState(
                targetValue = if (post.isReposted) SocialRepost else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "repostTint"
            )
            val bookmarkTint by animateColorAsState(
                targetValue = if (post.isSaved) SocialBookmark else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "bookmarkTint"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like & Long-Press Reaction Button
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .pointerInput(post.id, post.userReaction) {
                                detectTapGestures(
                                    onTap = {
                                        if (onReactionSelect != null) {
                                            onReactionSelect(post.userReaction ?: "like")
                                        } else {
                                            onLikeClick()
                                        }
                                    },
                                    onLongPress = {
                                        showReactionMenu = true
                                    }
                                )
                            }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                            .testTag("like_btn_${post.id}")
                    ) {
                        val reactionEmoji = when (post.userReaction) {
                            "like" -> "👍"
                            "love" -> "❤️"
                            "laugh" -> "😂"
                            "sad" -> "😢"
                            else -> null
                        }

                        if (reactionEmoji != null) {
                            Text(
                                text = reactionEmoji,
                                fontSize = 16.sp,
                                modifier = Modifier.testTag("active_reaction_${post.id}")
                            )
                        } else {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Reaction",
                                tint = likeTint,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${post.likesCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (post.userReaction != null || post.isLiked) FbBlue else likeTint,
                            modifier = Modifier.testTag("reaction_count_${post.id}")
                        )
                    }

                    // Floating Long-Press Reaction Menu
                    DropdownMenu(
                        expanded = showReactionMenu,
                        onDismissRequest = { showReactionMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .testTag("reaction_menu_${post.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val reactions = listOf(
                                Triple("like", "👍", "Like"),
                                Triple("love", "❤️", "Love"),
                                Triple("laugh", "😂", "Laugh"),
                                Triple("sad", "😢", "Sad")
                            )

                            reactions.forEach { (key, emoji, label) ->
                                val isSelected = post.userReaction == key
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) FbBlue.copy(alpha = 0.18f) else Color.Transparent
                                        )
                                        .clickable {
                                            showReactionMenu = false
                                            if (onReactionSelect != null) {
                                                onReactionSelect(key)
                                            } else {
                                                onLikeClick()
                                            }
                                        }
                                        .testTag("reaction_${key}_${post.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = if (isSelected) 24.sp else 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Comment Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCommentClick() }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                        .testTag("comment_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Repost Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onRepostClick() }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                        .testTag("repost_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Repeat,
                        contentDescription = "Repost",
                        tint = repostTint,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.repostsCount}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = repostTint
                    )
                }

                // Share Button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("share_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Bookmark Button
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("bookmark_btn_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save Bookmark",
                        tint = bookmarkTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoMediaCard(mediaUrl: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var isBuffering by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var playbackError by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var retryCount by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    var exoPlayer by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ExoPlayer?>(null) }

    androidx.compose.runtime.DisposableEffect(isPlaying, mediaUrl, retryCount) {
        if (isPlaying && mediaUrl.isNotBlank()) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(mediaUrl))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                prepare()
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                        if (playbackState == Player.STATE_READY) {
                            playbackError = null
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        playbackError = error.localizedMessage ?: "Failed to play video"
                        isBuffering = false
                    }
                })
            }
            exoPlayer = player
        }
        onDispose {
            exoPlayer?.release()
            exoPlayer = null
            isBuffering = false
            playbackError = null
        }
    }

    // Lifecycle observer to pause when backgrounded
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer?.playWhenReady = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) {
                        exoPlayer?.playWhenReady = true
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
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying && exoPlayer != null && playbackError == null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Buffering Spinner
            if (isBuffering) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            IconButton(
                onClick = { isPlaying = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Stop Video",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else if (playbackError != null) {
            // Playback Error Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unable to play video",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playbackError ?: "Network or playback error",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                playbackError = null
                                retryCount++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Retry")
                        }
                        OutlinedButton(
                            onClick = {
                                isPlaying = false
                                playbackError = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        } else {
            // Video Thumbnail / Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    )
                    .clickable { isPlaying = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FbBlue.copy(alpha = 0.95f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to play video",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIDEO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

