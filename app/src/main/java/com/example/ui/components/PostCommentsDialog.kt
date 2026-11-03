package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Comment
import com.example.data.Post
import com.example.data.User
import com.example.ui.theme.FbBlue
import com.example.ui.theme.SocialHeart
import com.example.ui.theme.VerifiedBadge
import com.example.util.InteractivePostText

@Composable
fun PostCommentsDialog(
    post: Post,
    currentUser: User,
    comments: List<Comment>,
    isLoadingComments: Boolean = false,
    commentError: String? = null,
    registeredUsers: List<User> = emptyList(),
    onDismiss: () -> Unit,
    onAddComment: (text: String, parentId: String?, replyToUsername: String?) -> Unit,
    onLikeComment: (commentId: String) -> Unit,
    onEditComment: (commentId: String, newText: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onAuthorClick: ((User) -> Unit)? = null,
    onHashtagClick: ((String) -> Unit)? = null,
    onMentionClick: ((String) -> Unit)? = null,
    onReportComment: ((Comment) -> Unit)? = null
) {
    var commentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }
    var editingComment by remember { mutableStateOf<Comment?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Mention suggestions: check if last word starts with @
    val mentionQuery = remember(commentText) {
        val lastWord = commentText.split(" ", "\n").lastOrNull().orEmpty()
        if (lastWord.startsWith("@") && lastWord.length > 1) {
            lastWord.removePrefix("@").lowercase()
        } else null
    }

    val suggestedUsers = remember(mentionQuery, registeredUsers) {
        if (mentionQuery.isNullOrBlank()) emptyList()
        else registeredUsers.filter {
            it.handle.removePrefix("@").lowercase().contains(mentionQuery) ||
            it.name.lowercase().contains(mentionQuery)
        }.take(5)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("comments_sheet_dialog"),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = CardDefaults.outlinedCardBorder(enabled = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${post.commentsCount} comments & replies",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_comments_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Post Context Snippet
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = post.author.avatarUrl,
                            contentDescription = post.author.name,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.author.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = post.text.ifBlank { "Media post" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Error State if any
                if (commentError != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = commentError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Loading State
                if (isLoadingComments) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = FbBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else if (comments.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No comments yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Be the first to share your thoughts on this post!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Threaded Comments List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentItem(
                                comment = comment,
                                isReply = false,
                                currentUser = currentUser,
                                postAuthorId = post.author.id,
                                onReplyClick = {
                                    replyingToComment = comment
                                    editingComment = null
                                },
                                onLikeClick = { onLikeComment(comment.id) },
                                onEditClick = {
                                    editingComment = comment
                                    commentText = comment.text
                                    replyingToComment = null
                                },
                                onDeleteClick = { onDeleteComment(comment.id) },
                                onAuthorClick = onAuthorClick,
                                onHashtagClick = onHashtagClick,
                                onMentionClick = onMentionClick,
                                onReportClick = onReportComment?.let { callback -> { callback(comment) } }
                            )

                            // Nested replies
                            if (comment.replies.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 38.dp, top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    comment.replies.forEach { reply ->
                                        CommentItem(
                                            comment = reply,
                                            isReply = true,
                                            currentUser = currentUser,
                                            postAuthorId = post.author.id,
                                            onReplyClick = {
                                                // Replying to a reply still sets parent as the top-level comment
                                                replyingToComment = comment
                                                editingComment = null
                                            },
                                            onLikeClick = { onLikeComment(reply.id) },
                                            onEditClick = {
                                                editingComment = reply
                                                commentText = reply.text
                                                replyingToComment = null
                                            },
                                            onDeleteClick = { onDeleteComment(reply.id) },
                                            onAuthorClick = onAuthorClick,
                                            onHashtagClick = onHashtagClick,
                                            onMentionClick = onMentionClick,
                                            onReportClick = onReportComment?.let { callback -> { callback(reply) } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mention Autocomplete Suggestions
                if (suggestedUsers.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestedUsers, key = { it.id }) { user ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable {
                                        // Replace last @query with @handle
                                        val words = commentText.split(" ", "\n").toMutableList()
                                        if (words.isNotEmpty()) {
                                            words[words.size - 1] = "${user.handle} "
                                            commentText = words.joinToString(" ")
                                        }
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        AsyncImage(
                                            model = user.avatarUrl,
                                            contentDescription = user.name,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = user.handle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = FbBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Replying-to Banner
                AnimatedVisibility(visible = replyingToComment != null) {
                    replyingToComment?.let { target ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = FbBlue.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = null,
                                        tint = FbBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Replying to ${target.author.name} (${target.author.handle})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FbBlue
                                    )
                                }
                                IconButton(
                                    onClick = { replyingToComment = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel reply",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Editing-mode Banner
                AnimatedVisibility(visible = editingComment != null) {
                    editingComment?.let {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Editing your comment",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        editingComment = null
                                        commentText = ""
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel edit",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Field Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = currentUser.avatarUrl,
                        contentDescription = currentUser.name,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = {
                            Text(
                                if (replyingToComment != null) "Write a reply... (use @name to mention)"
                                else if (editingComment != null) "Update your comment..."
                                else "Add a comment... (use @name to mention)"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("comment_input_box"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank() && !isSubmitting) {
                                isSubmitting = true
                                val trimmed = commentText.trim()
                                if (editingComment != null) {
                                    onEditComment(editingComment!!.id, trimmed)
                                    editingComment = null
                                    commentText = ""
                                    isSubmitting = false
                                } else {
                                    val parent = replyingToComment
                                    onAddComment(
                                        trimmed,
                                        parent?.id,
                                        parent?.author?.handle
                                    )
                                    replyingToComment = null
                                    commentText = ""
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = commentText.isNotBlank() && !isSubmitting,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = FbBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("send_comment_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (editingComment != null) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Comment"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    isReply: Boolean,
    currentUser: User,
    postAuthorId: String,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAuthorClick: ((User) -> Unit)?,
    onHashtagClick: ((String) -> Unit)?,
    onMentionClick: ((String) -> Unit)?,
    onReportClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val isMyComment = comment.author.id == currentUser.id
    val isPostOwner = postAuthorId == currentUser.id
    val canDelete = isMyComment || isPostOwner

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.author.avatarUrl,
            contentDescription = comment.author.name,
            modifier = Modifier
                .size(if (isReply) 28.dp else 36.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable(enabled = onAuthorClick != null) { onAuthorClick?.invoke(comment.author) },
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isReply) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = comment.author.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = onAuthorClick != null) {
                                    onAuthorClick?.invoke(comment.author)
                                }
                            )

                            if (comment.author.isVerified) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Account",
                                    tint = VerifiedBadge,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (comment.replyToUsername != null) {
                                Text(
                                    text = "replying to ${comment.replyToUsername}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = FbBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(20.dp)
                                    .testTag("comment_menu_btn_${comment.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (isMyComment) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Comment") },
                                        onClick = {
                                            showMenu = false
                                            onEditClick()
                                        },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                                if (canDelete) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (isMyComment) "Delete Comment" else "Moderate (Remove)",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onDeleteClick()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                                if (!isMyComment && onReportClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Report Comment", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onReportClick()
                                        },
                                        modifier = Modifier.testTag("report_comment_btn_${comment.id}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Comment Body with interactive mentions and hashtags
                    InteractivePostText(
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        onMentionClick = onMentionClick,
                        onHashtagClick = onHashtagClick
                    )

                    if (comment.isEdited) {
                        Text(
                            text = "(edited)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions row: Timestamp, Like, Reply, Likes Count
            Row(
                modifier = Modifier
                    .padding(start = 6.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = comment.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Like Comment Action
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLikeClick() }
                        .padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like comment",
                        tint = if (comment.isLiked) SocialHeart else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    if (comment.likesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likesCount}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (comment.isLiked) SocialHeart else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Reply Action
                Text(
                    text = "Reply",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onReplyClick() }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}
