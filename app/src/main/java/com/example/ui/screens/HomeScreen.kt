package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.PostType
import com.example.data.Story
import com.example.data.User
import com.example.ui.FeedFilter
import com.example.ui.components.PostCard
import com.example.ui.components.StoryTray
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.example.ui.theme.FbBlue
import com.example.ui.theme.FbBlueLight
import com.example.ui.theme.FbBlueHover
import com.example.ui.theme.FbTextSecondary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.testTag("swipe_refresh_feed")
    ) {
        content()
    }
}

/**
 * Pinned Feed Filter Mechanism allowing users to toggle between 'All', 'Following',
 * and 'Trending' posts (plus 'Favorites') to improve content discovery.
 */
@Composable
fun FeedFilterHeader(
    currentFilter: FeedFilter,
    onFilterSelected: (FeedFilter) -> Unit,
    onRefreshFeed: () -> Unit,
    isFeedLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("feed_filter_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 'All' Filter Tab
                val isAllSelected = currentFilter == FeedFilter.ALL || currentFilter == FeedFilter.FOR_YOU
                FilterChip(
                    selected = isAllSelected,
                    onClick = { onFilterSelected(FeedFilter.ALL) },
                    label = {
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("feed_filter_all"),
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FbBlue,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )

                // 2. 'Following' Filter Tab
                val isFollowingSelected = currentFilter == FeedFilter.FOLLOWING
                FilterChip(
                    selected = isFollowingSelected,
                    onClick = { onFilterSelected(FeedFilter.FOLLOWING) },
                    label = {
                        Text(
                            text = "Following",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isFollowingSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("feed_filter_following"),
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FbBlue,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )

                // 3. 'Trending' Filter Tab
                val isTrendingSelected = currentFilter == FeedFilter.TRENDING
                FilterChip(
                    selected = isTrendingSelected,
                    onClick = { onFilterSelected(FeedFilter.TRENDING) },
                    label = {
                        Text(
                            text = "Trending",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isTrendingSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("feed_filter_trending"),
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FbBlue,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )

                // 4. 'Favorites' Filter Tab
                val isFavoritesSelected = currentFilter == FeedFilter.FAVORITES
                FilterChip(
                    selected = isFavoritesSelected,
                    onClick = { onFilterSelected(FeedFilter.FAVORITES) },
                    label = {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isFavoritesSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("feed_filter_favorites"),
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FbBlue,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }

            IconButton(
                onClick = onRefreshFeed,
                modifier = Modifier.testTag("refresh_feed_btn")
            ) {
                if (isFeedLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = FbBlue
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh feed",
                        tint = FbBlue
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUser: User,
    posts: List<Post>,
    stories: List<Story>,
    currentFilter: FeedFilter,
    onFilterSelected: (FeedFilter) -> Unit,
    onStoryClick: (Story) -> Unit,
    onAddStoryClick: () -> Unit,
    onCreatePostClick: (PostType, String?, String?) -> Unit = { _, _, _ -> },
    onLikeClick: (String) -> Unit,
    onRepostClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (String) -> Unit,
    onBookmarkClick: (String) -> Unit,
    isFeedLoading: Boolean = false,
    feedError: String? = null,
    onRefreshFeed: () -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    onAuthorClick: (User) -> Unit = {},
    onMentionClick: (String) -> Unit = {},
    followingUserIds: Set<String> = emptySet(),
    blockedUserIds: Set<String> = emptySet(),
    mutedUserIds: Set<String> = emptySet(),
    favoriteUserIds: Set<String> = emptySet(),
    suggestedCreators: List<User> = emptyList(),
    onToggleFollow: (String) -> Unit = {},
    onToggleFavorite: ((User) -> Unit)? = null,
    onReportPost: ((Post) -> Unit)? = null,
    onReactionSelect: (String, String) -> Unit = { _, _ -> },
    onOpenMadadCenter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCreatePostClick(PostType.PHOTO, null, uri.toString())
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCreatePostClick(PostType.VIDEO, null, uri.toString())
        }
    }

    var showFeelingSelector by remember { mutableStateOf(false) }

    val visibleStories = remember(stories, blockedUserIds, mutedUserIds) {
        stories.filter { !blockedUserIds.contains(it.author.id) && !mutedUserIds.contains(it.author.id) }
    }

    val visiblePosts = remember(posts, blockedUserIds, mutedUserIds) {
        posts.filter { !blockedUserIds.contains(it.author.id) && !mutedUserIds.contains(it.author.id) }
    }

    val filteredPosts = when (currentFilter) {
        FeedFilter.ALL, FeedFilter.FOR_YOU -> visiblePosts
        FeedFilter.FOLLOWING -> visiblePosts.filter { followingUserIds.contains(it.author.id) }
        FeedFilter.TRENDING -> visiblePosts.sortedByDescending { it.likesCount + it.repostsCount + it.commentsCount }
        FeedFilter.FAVORITES -> visiblePosts.filter { favoriteUserIds.contains(it.author.id) || it.isSaved }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Pinned Filtering Mechanism at the top of the feed (All / Following / Trending)
        FeedFilterHeader(
            currentFilter = currentFilter,
            onFilterSelected = onFilterSelected,
            onRefreshFeed = onRefreshFeed,
            isFeedLoading = isFeedLoading
        )

        SwipeRefresh(
            isRefreshing = isFeedLoading,
            onRefresh = onRefreshFeed,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_social_feed"),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
        // Ephemeral Stories Bar
        item {
            StoryTray(
                currentUser = currentUser,
                stories = visibleStories,
                onStoryClick = onStoryClick,
                onAddStoryClick = onAddStoryClick
            )
        }

        // Quick Post Compose Box (Facebook Style)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("quick_compose_card"),
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
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCreatePostClick(PostType.TEXT, null, null) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = currentUser.avatarUrl,
                            contentDescription = "Your avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onCreatePostClick(PostType.TEXT, null, null) },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "What's on your mind, ${currentUser.name.split(" ").firstOrNull() ?: currentUser.name}?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Dedicated Photo Shortcut: Opens image picker directly
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .testTag("shortcut_photo_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Photo",
                                tint = Color(0xFF45BD62),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Photo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }

                        // 2. Dedicated Video Shortcut: Opens video picker directly
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .testTag("shortcut_video_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Video",
                                tint = Color(0xFFF02849),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Video", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }

                        // 3. Dedicated Feeling Shortcut: Opens dedicated feeling/activity selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showFeelingSelector = true }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .testTag("shortcut_feeling_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Feeling",
                                tint = Color(0xFFF7B125),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Feeling", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Community Relief & Madad Center Banner
        item(key = "madad_center_discovery_banner") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clickable { onOpenMadadCenter() }
                    .testTag("home_madad_center_banner"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FbBlueLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FbBlue,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.VolunteerActivism,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yarkhoon Madad Center",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = FbBlueHover
                        )
                        Text(
                            text = "Job Support • Blood Donors • Mutual Aid • Legal Help",
                            style = MaterialTheme.typography.bodySmall,
                            color = FbTextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Madad Center",
                        tint = FbBlue
                    )
                }
            }
        }

        // Trending Feed Discovery Banner (shown when Trending filter is active)
        if (currentFilter == FeedFilter.TRENDING) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .testTag("trending_feed_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FbBlue.copy(alpha = 0.08f)),
                    border = CardDefaults.outlinedCardBorder(enabled = true)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🔥", style = MaterialTheme.typography.titleMedium)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trending in Yarkhoon Valley",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ranked by community likes, comments, and shares",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Loading State
        if (isFeedLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = FbBlue,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading real posts from YARKHOON.COM...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Error State
        if (feedError != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Could not load posts: $feedError",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onRefreshFeed,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Loading Feed")
                        }
                    }
                }
            }
        }

        // Posts Stream
        if (!isFeedLoading && feedError == null && filteredPosts.isEmpty()) {
            item {
                if (currentFilter == FeedFilter.FOLLOWING) {
                    // Following empty state with Suggested Creators
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = FbBlue.copy(alpha = 0.1f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PeopleOutline,
                                        contentDescription = null,
                                        tint = FbBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Following Feed is Quiet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Follow community members and creators from Yarkhoon Valley & Chitral to see their posts here:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            if (suggestedCreators.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    suggestedCreators.take(4).forEach { creator ->
                                        val isFollowing = followingUserIds.contains(creator.id)
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = creator.avatarUrl,
                                                    contentDescription = creator.name,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .clickable { onAuthorClick(creator) },
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { onAuthorClick(creator) }
                                                ) {
                                                    Text(
                                                        text = creator.name,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = creator.jobTitle.ifBlank { creator.location.ifBlank { creator.handle } },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = { onToggleFollow(creator.id) },
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else FbBlue,
                                                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isFollowing) "Following" else "Follow",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onFilterSelected(FeedFilter.ALL) },
                                colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("explore_all_feed_btn")
                            ) {
                                Text("Explore 'All' Feed")
                            }
                        }
                    }
                } else {
                    // Standard empty feed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PostAdd,
                                contentDescription = null,
                                tint = FbBlue,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (currentFilter == FeedFilter.FAVORITES) "No Favorite Posts Yet" else "No posts in this feed yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (currentFilter == FeedFilter.FAVORITES) {
                                    "Add creators to Favorites from their profile (Three dots > Add to Favorites) to see their posts here."
                                } else {
                                    "Be the first to share news, photos, or business updates in Yarkhoon Valley!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onCreatePostClick(PostType.TEXT, null, null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create First Post")
                                }
                                OutlinedButton(
                                    onClick = onRefreshFeed,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text("Refresh")
                                }
                            }
                        }
                    }
                }
            }
        } else if (!isFeedLoading && feedError == null) {
            items(filteredPosts, key = { it.id }) { post ->
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    PostCard(
                        post = post,
                        onLikeClick = { onLikeClick(post.id) },
                        onRepostClick = { onRepostClick(post.id) },
                        onCommentClick = { onCommentClick(post) },
                        onShareClick = { onShareClick(post.id) },
                        onBookmarkClick = { onBookmarkClick(post.id) },
                        isFavorite = favoriteUserIds.contains(post.author.id),
                        onToggleFavorite = onToggleFavorite?.let { cb -> { cb(post.author) } },
                        onAuthorClick = { onAuthorClick(post.author) },
                        onHashtagClick = onHashtagClick,
                        onMentionClick = onMentionClick,
                        onReportClick = onReportPost?.let { { it(post) } },
                        onReactionSelect = { reaction -> onReactionSelect(post.id, reaction) }
                    )
                }
            }
        }
    }
    }

    // Dedicated Feeling & Activity Selector Dialog
    if (showFeelingSelector) {
        Dialog(onDismissRequest = { showFeelingSelector = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .testTag("feeling_selector_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder(enabled = true),
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "How are you feeling?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showFeelingSelector = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val feelings = listOf(
                        "😊 Feeling Happy",
                        "🌟 Feeling Blessed",
                        "🏔️ Exploring Chitral",
                        "❤️ Feeling Loved",
                        "🎉 Celebrating",
                        "☕ Feeling Relaxed",
                        "💡 Feeling Inspired",
                        "🙏 Feeling Grateful",
                        "🍽️ Eating Delicious Food",
                        "🎧 Listening to Khowar Music",
                        "💼 Working hard",
                        "⚽ Feeling Excited"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        feelings.chunked(2).forEach { rowFeelings ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowFeelings.forEach { feeling ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                showFeelingSelector = false
                                                onCreatePostClick(PostType.TEXT, feeling, null)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = CardDefaults.outlinedCardBorder(enabled = true)
                                    ) {
                                        Text(
                                            text = feeling,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                                if (rowFeelings.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
