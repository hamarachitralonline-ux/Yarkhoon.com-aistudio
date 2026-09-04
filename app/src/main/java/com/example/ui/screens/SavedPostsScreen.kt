package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppColors.FacebookBlue
import com.example.data.Post
import com.example.data.User

@Composable
fun SavedPostsScreen(
    savedPosts: List<Post>,
    users: List<User>,
    onLike: (Post) -> Unit,
    onComment: (Post, String) -> Unit,
    onOpenComments: (Post) -> Unit = {},
    onSharePost: (Post) -> Unit = {},
    onRemoveSaved: (Post) -> Unit
) {
    if (savedPosts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .testTag("empty_saved_posts_view"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = FacebookBlue.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No saved posts yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Save posts from your feed to easily view them anytime in your bookmarks.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("saved_posts_scroll_view"),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = FacebookBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Saved Posts (${savedPosts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(savedPosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onLike = { onLike(post) },
                    onComment = { text -> onComment(post, text) },
                    onOpenComments = { onOpenComments(post) },
                    onSharePost = { onSharePost(post) },
                    users = users,
                    isSaved = true,
                    onToggleSave = { onRemoveSaved(post) }
                )
            }
        }
    }
}
