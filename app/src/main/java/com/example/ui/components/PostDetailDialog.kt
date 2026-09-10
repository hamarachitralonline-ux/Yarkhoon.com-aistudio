package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Post
import com.example.data.PostReaction
import com.example.data.User

private val BrandBlue = Color(0xFF1877F2)
private val EmeraldGreen = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailDialog(
    post: Post,
    isSaved: Boolean = false,
    reactions: List<PostReaction> = emptyList(),
    users: List<User> = emptyList(),
    onDismiss: () -> Unit,
    onLike: (Post) -> Unit,
    onReact: (Post, String) -> Unit,
    onComment: (Post, String) -> Unit,
    onOpenComments: (Post) -> Unit,
    onSharePost: (Post) -> Unit,
    onToggleSave: (Int) -> Unit = {},
    onViewAuthorProfile: (User) -> Unit = {}
) {
    var showWebPreview by remember { mutableStateOf(false) }

    if (showWebPreview) {
        WebPostPreviewDialog(
            post = post,
            onDismiss = { showWebPreview = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Post #${post.id}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Deep Link Verified • yarkhoon.com/post/${post.id}",
                                    fontSize = 11.sp,
                                    color = EmeraldGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Post")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showWebPreview = true }) {
                            Icon(Icons.Filled.Language, contentDescription = "Web Preview", tint = BrandBlue)
                        }
                        IconButton(onClick = { onSharePost(post) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = BrandBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .testTag("deep_link_post_detail_view")
            ) {
                // App Link Header Notification
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Opened directly inside Yarkhoon App via Android App Link",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Render the post using the app's full PostCard
                com.example.ui.screens.PostCard(
                    post = post,
                    isSaved = isSaved,
                    reactions = reactions,
                    onLike = { onLike(post) },
                    onReact = { reaction -> onReact(post, reaction) },
                    onComment = { text -> onComment(post, text) },
                    onOpenComments = { onOpenComments(post) },
                    onSharePost = { onSharePost(post) },
                    onToggleSave = { onToggleSave(post.id) },
                    users = users
                )

                Spacer(Modifier.height(16.dp))

                // Deep Link & Share Information Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Shareable URL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "https://yarkhoon.com/post/${post.id}",
                            fontSize = 12.sp,
                            color = BrandBlue,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "When shared, recipients with the Yarkhoon app installed open this post directly. Recipients without the app see the web preview page with an option to download.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showWebPreview = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Web Preview", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { onSharePost(post) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share Post", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
