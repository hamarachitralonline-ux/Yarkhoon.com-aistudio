package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.components.PostCard
import com.example.ui.theme.BusinessBadge
import com.example.ui.theme.FbBlue
import com.example.ui.theme.VerifiedBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    activeSearchTab: SearchTab,
    onSearchTabSelected: (SearchTab) -> Unit,
    searchResults: SearchResults?,
    isSearching: Boolean,
    searchError: String?,
    onRetrySearch: () -> Unit,
    exploreContent: ExploreContent,
    isExploreLoading: Boolean,
    exploreError: String?,
    onRefreshExplore: () -> Unit,
    onHashtagClick: (String) -> Unit,
    onUserClick: (User) -> Unit,
    onPostClick: (Post) -> Unit,
    onReelClick: (Reel) -> Unit,
    onLikePost: (String) -> Unit,
    blockedUserIds: Set<String> = emptySet(),
    followingUserIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val isSearchMode = searchQuery.isNotBlank()

    val filteredResults = remember(searchResults, blockedUserIds, followingUserIds) {
        searchResults?.let { res ->
            res.copy(
                creators = res.creators.filter { !blockedUserIds.contains(it.id) },
                posts = res.posts.filter { post ->
                    !blockedUserIds.contains(post.author.id) &&
                    (!post.author.isPrivate || followingUserIds.contains(post.author.id))
                }
            )
        }
    }

    val filteredExploreContent = remember(exploreContent, blockedUserIds, followingUserIds) {
        exploreContent.copy(
            recommendedCreators = exploreContent.recommendedCreators.filter { !blockedUserIds.contains(it.id) },
            popularPosts = exploreContent.popularPosts.filter { post ->
                !blockedUserIds.contains(post.author.id) &&
                (!post.author.isPrivate || followingUserIds.contains(post.author.id))
            },
            activeDiscussions = exploreContent.activeDiscussions.filter { post ->
                !blockedUserIds.contains(post.author.id) &&
                (!post.author.isPrivate || followingUserIds.contains(post.author.id))
            },
            mediaGridPosts = exploreContent.mediaGridPosts.filter { post ->
                !blockedUserIds.contains(post.author.id) &&
                (!post.author.isPrivate || followingUserIds.contains(post.author.id))
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("explore_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sticky/Top Search Bar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            "Search people, posts, hashtags, reels...",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = FbBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = onClearSearch,
                                modifier = Modifier.testTag("clear_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_bar"),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FbBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Search Result Filter Tabs (Visible when user has typed a query)
                AnimatedVisibility(visible = isSearchMode) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(SearchTab.values()) { tab ->
                                val isSelected = activeSearchTab == tab
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSearchTabSelected(tab) },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FbBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // SEARCH RESULTS MODE
        // -------------------------------------------------------------
        if (isSearchMode) {
            if (isSearching) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = FbBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Searching across YARKHOON.COM...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (searchError != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Search failed: $searchError",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRetrySearch,
                                colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                            ) {
                                Text("Retry Search")
                            }
                        }
                    }
                }
            } else if (filteredResults != null) {
                val results = filteredResults
                val hasAnyResults = results.creators.isNotEmpty() || results.hashtags.isNotEmpty() || results.posts.isNotEmpty() || results.videos.isNotEmpty() || results.reels.isNotEmpty() || results.people.isNotEmpty()

                if (!hasAnyResults) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder(enabled = true)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SearchOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No results found for \"$searchQuery\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try searching for a different name, #hashtag, topic, or keyword.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Filter: ALL or PEOPLE
                    if ((activeSearchTab == SearchTab.ALL || activeSearchTab == SearchTab.PEOPLE) && results.people.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "People (${results.people.size})",
                                icon = Icons.Default.People
                            )
                        }
                        items(results.people, key = { "user_${it.id}" }) { user ->
                            UserResultCard(
                                user = user,
                                onUserClick = { onUserClick(user) }
                            )
                        }
                    }

                    // Filter: ALL or CREATORS
                    if ((activeSearchTab == SearchTab.ALL || activeSearchTab == SearchTab.CREATORS) && results.creators.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Creators & Leaders (${results.creators.size})",
                                icon = Icons.Default.Star
                            )
                        }
                        items(results.creators, key = { "creator_${it.id}" }) { creator ->
                            UserResultCard(
                                user = creator,
                                onUserClick = { onUserClick(creator) }
                            )
                        }
                    }

                    // Filter: ALL or HASHTAGS
                    if ((activeSearchTab == SearchTab.ALL || activeSearchTab == SearchTab.HASHTAGS) && results.hashtags.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Hashtags (${results.hashtags.size})",
                                icon = Icons.Default.Tag
                            )
                        }
                        items(results.hashtags, key = { "tag_${it.normalizedTag}" }) { hashtag ->
                            HashtagResultCard(
                                hashtag = hashtag,
                                onHashtagClick = { onHashtagClick(hashtag.tag) }
                            )
                        }
                    }

                    // Filter: ALL or POSTS
                    if ((activeSearchTab == SearchTab.ALL || activeSearchTab == SearchTab.POSTS) && results.posts.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Public Posts (${results.posts.size})",
                                icon = Icons.Default.Article
                            )
                        }
                        items(results.posts, key = { "post_${it.id}" }) { post ->
                            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { onLikePost(post.id) },
                                    onRepostClick = { },
                                    onCommentClick = { onPostClick(post) },
                                    onShareClick = { },
                                    onBookmarkClick = { },
                                    onAuthorClick = { onUserClick(post.author) },
                                    onHashtagClick = onHashtagClick
                                )
                            }
                        }
                    }

                    // Filter: ALL or VIDEOS
                    if ((activeSearchTab == SearchTab.ALL || activeSearchTab == SearchTab.VIDEOS) && (results.videos.isNotEmpty() || results.reels.isNotEmpty())) {
                        item {
                            SectionHeader(
                                title = "Videos & Reels (${results.videos.size + results.reels.size})",
                                icon = Icons.Default.Videocam
                            )
                        }
                        // Reels row
                        if (results.reels.isNotEmpty()) {
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(results.reels, key = { "reel_${it.id}" }) { reel ->
                                        ReelThumbnailCard(
                                            reel = reel,
                                            onReelClick = { onReelClick(reel) }
                                        )
                                    }
                                }
                            }
                        }
                        // Video Posts
                        items(results.videos, key = { "video_post_${it.id}" }) { post ->
                            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                                PostCard(
                                    post = post,
                                    onLikeClick = { onLikePost(post.id) },
                                    onRepostClick = { },
                                    onCommentClick = { onPostClick(post) },
                                    onShareClick = { },
                                    onBookmarkClick = { },
                                    onAuthorClick = { onUserClick(post.author) },
                                    onHashtagClick = onHashtagClick
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // -------------------------------------------------------------
            // DISCOVER & EXPLORE MODE (Empty search query)
            // -------------------------------------------------------------

            // Refresh & Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = FbBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trending on YARKHOON.COM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    IconButton(
                        onClick = onRefreshExplore,
                        modifier = Modifier.testTag("refresh_explore_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh trending",
                            tint = FbBlue
                        )
                    }
                }
            }

            // Loading, Error, and Empty States for Explore Content
            if (isExploreLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = FbBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            } else if (!exploreError.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Could not load trending content: $exploreError",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRefreshExplore,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Loading")
                            }
                        }
                    }
                }
            } else if (exploreContent.trendingHashtags.isEmpty() &&
                exploreContent.recommendedCreators.isEmpty() &&
                exploreContent.popularPosts.isEmpty() &&
                exploreContent.popularReels.isEmpty()
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(enabled = true)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = FbBlue,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Trending Content Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Discoveries, trending hashtags, and top creators from Yarkhoon Valley will appear here as the community shares posts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Section 1: Measurable Trending Hashtags
            if (exploreContent.trendingHashtags.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exploreContent.trendingHashtags.take(5).forEachIndexed { index, hashtag ->
                            TrendingRankCard(
                                rank = index + 1,
                                hashtag = hashtag,
                                onHashtagClick = { onHashtagClick(hashtag.tag) }
                            )
                        }
                    }
                }
            } else if (isExploreLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FbBlue, modifier = Modifier.size(32.dp))
                    }
                }
            }

            // Section 2: Recommended Creators & Community Leaders
            if (filteredExploreContent.recommendedCreators.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(
                            title = "Creators & Community Voices",
                            icon = Icons.Default.People
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredExploreContent.recommendedCreators, key = { it.id }) { creator ->
                                CreatorCard(
                                    creator = creator,
                                    onCreatorClick = { onUserClick(creator) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Popular Reels
            if (exploreContent.popularReels.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(
                            title = "Popular Reels",
                            icon = Icons.Default.PlayArrow
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(exploreContent.popularReels, key = { it.id }) { reel ->
                                ReelThumbnailCard(
                                    reel = reel,
                                    onReelClick = { onReelClick(reel) }
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Trending Discussions
            if (exploreContent.activeDiscussions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Active Discussions",
                        icon = Icons.Outlined.ChatBubbleOutline
                    )
                }
                items(exploreContent.activeDiscussions, key = { "disc_${it.id}" }) { post ->
                    DiscussionCard(
                        post = post,
                        onPostClick = { onPostClick(post) },
                        onUserClick = { onUserClick(post.author) },
                        onHashtagClick = onHashtagClick
                    )
                }
            }

            // Section 5: Photo & Video Discovery Grid
            if (exploreContent.mediaGridPosts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Valley Moments & Photos",
                        icon = Icons.Default.PhotoLibrary
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exploreContent.mediaGridPosts.chunked(3).forEach { rowPosts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowPosts.forEach { post ->
                                    val mediaUrl = post.mediaUrls.firstOrNull().orEmpty()
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(110.dp)
                                            .clickable { onPostClick(post) }
                                            .testTag("media_grid_item_${post.id}"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = mediaUrl,
                                                contentDescription = "Media post",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            if (post.type == PostType.VIDEO || post.type == PostType.REEL) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = "Video",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                // Padding if row has less than 3
                                repeat(3 - rowPosts.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Section 6: Popular Public Posts
            if (exploreContent.popularPosts.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Popular Across YARKHOON.COM",
                        icon = Icons.Default.LocalFireDepartment
                    )
                }
                items(exploreContent.popularPosts, key = { "pop_post_${it.id}" }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                        PostCard(
                            post = post,
                            onLikeClick = { onLikePost(post.id) },
                            onRepostClick = { },
                            onCommentClick = { onPostClick(post) },
                            onShareClick = { },
                            onBookmarkClick = { },
                            onAuthorClick = { onUserClick(post.author) },
                            onHashtagClick = onHashtagClick
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Helper Components ----------------

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FbBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TrendingRankCard(
    rank: Int,
    hashtag: TrendingHashtag,
    onHashtagClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHashtagClick() }
            .testTag("trending_rank_$rank"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (rank <= 3) FbBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$rank",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rank <= 3) FbBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hashtag.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = hashtag.tag,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = FbBlue
                )
                Text(
                    text = "${hashtag.postCount} public post${if (hashtag.postCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hashtag.totalEngagement > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${hashtag.totalEngagement} interactions",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorCard(
    creator: User,
    onCreatorClick: () -> Unit
) {
    var isFollowing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onCreatorClick() }
            .testTag("creator_card_${creator.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = creator.avatarUrl,
                contentDescription = creator.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = creator.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (creator.isVerified) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = VerifiedBadge,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = creator.handle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { isFollowing = !isFollowing },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else FbBlue,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
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

@Composable
private fun ReelThumbnailCard(
    reel: Reel,
    onReelClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .clickable { onReelClick() }
            .testTag("explore_reel_${reel.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = reel.thumbnailMediaUrl,
                contentDescription = reel.caption,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 100f
                        )
                    )
            )

            // Play Icon
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )

            // Caption & author
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = reel.author.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${reel.likesCount} likes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DiscussionCard(
    post: Post,
    onPostClick: () -> Unit,
    onUserClick: () -> Unit,
    onHashtagClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable { onPostClick() }
            .testTag("discussion_card_${post.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onUserClick() }
                ) {
                    AsyncImage(
                        model = post.author.avatarUrl,
                        contentDescription = post.author.name,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = post.author.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FbBlue.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = FbBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${post.commentsCount} comments",
                            style = MaterialTheme.typography.labelSmall,
                            color = FbBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (post.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.tags.take(3).forEach { tag ->
                        Text(
                            text = if (tag.startsWith("#")) tag else "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = FbBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onHashtagClick(tag) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserResultCard(
    user: User,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable { onUserClick() }
            .testTag("user_result_${user.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (user.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = VerifiedBadge,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    if (user.isBusinessAccount) {
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
                }
                Text(
                    text = user.handle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (user.jobTitle.isNotBlank()) {
                    Text(
                        text = user.jobTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HashtagResultCard(
    hashtag: TrendingHashtag,
    onHashtagClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable { onHashtagClick() }
            .testTag("hashtag_result_${hashtag.normalizedTag}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = FbBlue.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = null,
                            tint = FbBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = hashtag.tag,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = FbBlue
                    )
                    Text(
                        text = "${hashtag.postCount} public post${if (hashtag.postCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Explore tag",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
