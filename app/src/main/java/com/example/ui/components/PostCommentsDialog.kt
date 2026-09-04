package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Post
import com.example.data.PostComment
import com.example.data.User
import java.text.SimpleDateFormat
import java.util.*

private val BrandBlue = Color(0xFF1877F2)
private val HeartRed = Color(0xFFE11D48)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCommentsDialog(
    post: Post,
    comments: List<PostComment>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onAddComment: (content: String, parentCommentId: Int?, replyToAuthorName: String?) -> Unit,
    onEditComment: (comment: PostComment, newContent: String) -> Unit,
    onDeleteComment: (commentId: Int) -> Unit,
    onToggleLike: (comment: PostComment) -> Unit,
    onReportComment: (comment: PostComment, reason: String, details: String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<PostComment?>(null) }
    var editingComment by remember { mutableStateOf<PostComment?>(null) }
    var commentToDelete by remember { mutableStateOf<PostComment?>(null) }
    var commentToReport by remember { mutableStateOf<PostComment?>(null) }

    val quickEmojis = listOf("🔥", "❤️", "🏔️", "👏", "😍", "🙌", "😂", "👍", "✨")

    // Split into top-level and replies mapping
    val topLevelComments = remember(comments) {
        comments.filter { it.parentCommentId == null }
    }
    val repliesMap = remember(comments) {
        comments.filter { it.parentCommentId != null }.groupBy { it.parentCommentId!! }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .fillMaxHeight(0.88f)
            .testTag("post_comments_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = CircleShape,
                            color = BrandBlue.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = comments.size.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (post.authorName.isNotBlank()) {
                        Text(
                            text = "on ${post.authorName}'s ${if (post.mediaType == "VIDEO") "video" else "post"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_comments_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Comments List
            Box(modifier = Modifier.weight(1f)) {
                if (comments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No comments yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Be the first to share your thoughts!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(topLevelComments, key = { it.id }) { comment ->
                            val childReplies = repliesMap[comment.id] ?: emptyList()
                            CommentItemView(
                                comment = comment,
                                currentUserId = currentUser?.id ?: "currentUser",
                                onLikeClick = { onToggleLike(comment) },
                                onReplyClick = {
                                    replyingToComment = comment
                                    editingComment = null
                                    inputText = ""
                                },
                                onEditClick = {
                                    editingComment = comment
                                    replyingToComment = null
                                    inputText = comment.content
                                },
                                onDeleteClick = { commentToDelete = comment },
                                onReportClick = { commentToReport = comment }
                            )

                            // Nested replies
                            if (childReplies.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .padding(start = 36.dp, top = 8.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    childReplies.forEach { reply ->
                                        CommentItemView(
                                            comment = reply,
                                            isReply = true,
                                            currentUserId = currentUser?.id ?: "currentUser",
                                            onLikeClick = { onToggleLike(reply) },
                                            onReplyClick = {
                                                replyingToComment = comment // reply to parent thread
                                                editingComment = null
                                            },
                                            onEditClick = {
                                                editingComment = reply
                                                replyingToComment = null
                                                inputText = reply.content
                                            },
                                            onDeleteClick = { commentToDelete = reply },
                                            onReportClick = { commentToReport = reply }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Emoji Reaction Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickEmojis) { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { inputText += emoji }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Replying or Editing State Banner
            AnimatedVisibility(
                visible = replyingToComment != null || editingComment != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = BrandBlue.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (editingComment != null) Icons.Outlined.Edit else Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (editingComment != null) "Editing comment..." else "Replying to ${replyingToComment?.authorName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandBlue
                            )
                        }
                        IconButton(
                            onClick = {
                                replyingToComment = null
                                editingComment = null
                                inputText = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Bottom Input Row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Current User Avatar
                    Surface(
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        AsyncImage(
                            model = currentUser?.avatarUrl?.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120" },
                            contentDescription = "My Avatar",
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Input Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = when {
                                    editingComment != null -> "Edit comment..."
                                    replyingToComment != null -> "Reply to ${replyingToComment?.authorName}..."
                                    else -> "Add a comment on this ${if (post.mediaType == "VIDEO") "video" else "post"}..."
                                },
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("post_comment_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = BrandBlue
                        ),
                        maxLines = 4
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                if (editingComment != null) {
                                    onEditComment(editingComment!!, inputText.trim())
                                    editingComment = null
                                    Toast.makeText(context, "Comment updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    onAddComment(
                                        inputText.trim(),
                                        replyingToComment?.id,
                                        replyingToComment?.authorName
                                    )
                                    replyingToComment = null
                                }
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (inputText.isNotBlank()) BrandBlue else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .testTag("send_comment_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (commentToDelete != null) {
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Delete Comment?") },
            text = { Text("Are you sure you want to remove this comment? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteComment(commentToDelete!!.id)
                        commentToDelete = null
                        Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_comment_btn")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Report Comment Dialog
    if (commentToReport != null) {
        ReportCommentDialog(
            comment = commentToReport!!,
            onDismiss = { commentToReport = null },
            onSubmitReport = { reason, details ->
                onReportComment(commentToReport!!, reason, details)
                commentToReport = null
                Toast.makeText(context, "Thank you. Your report has been submitted for review.", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
private fun CommentItemView(
    comment: PostComment,
    isReply: Boolean = false,
    currentUserId: String,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isMyComment = comment.authorId == currentUserId || comment.authorId == "currentUser"

    val timeFormatted = remember(comment.timestamp) {
        val diff = System.currentTimeMillis() - comment.timestamp
        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m"
            diff < 86400_000 -> "${diff / 3600_000}h"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(comment.timestamp))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comment_item_${comment.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Surface(
            shape = CircleShape,
            modifier = Modifier.size(if (isReply) 28.dp else 36.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            AsyncImage(
                model = comment.authorAvatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=120" },
                contentDescription = comment.authorName,
                contentScale = ContentScale.Crop
            )
        }

        // Comment Content Box
        Column(modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = comment.authorName.ifBlank { "Community Member" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (comment.replyToAuthorName != null && isReply) {
                                Text(
                                    text = "▸ @${comment.replyToAuthorName}",
                                    fontSize = 11.sp,
                                    color = BrandBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // More options menu
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("comment_options_${comment.id}")
                            ) {
                                Icon(
                                    Icons.Default.MoreHoriz,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                if (isMyComment) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Comment") },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onEditClick()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete Comment", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuExpanded = false
                                            onDeleteClick()
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("Report Inappropriate") },
                                        leadingIcon = { Icon(Icons.Outlined.Report, contentDescription = null, tint = Color(0xFFEA580C)) },
                                        onClick = {
                                            menuExpanded = false
                                            onReportClick()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Content
                    Text(
                        text = comment.content,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Comment Actions Row (Time, Like button, Reply button, Edited label)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (comment.isEdited) {
                    Text(
                        text = "• edited",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Like button & count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onLikeClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("comment_like_${comment.id}")
                ) {
                    Icon(
                        if (comment.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (comment.isLikedByMe) HeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (comment.likesCount > 0) comment.likesCount.toString() else "Like",
                        fontSize = 11.sp,
                        fontWeight = if (comment.isLikedByMe) FontWeight.Bold else FontWeight.Normal,
                        color = if (comment.isLikedByMe) HeartRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reply button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onReplyClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("comment_reply_${comment.id}")
                ) {
                    Text(
                        text = "Reply",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportCommentDialog(
    comment: PostComment,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String) -> Unit
) {
    val reportReasons = listOf(
        "Spam or misleading information",
        "Harassment or hate speech",
        "Inappropriate or graphic content",
        "Violence or dangerous behavior",
        "Intellectual property violation",
        "Other violation"
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }
    var detailsText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("report_comment_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report Comment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Why are you reporting this comment by ${comment.authorName}?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Radio Options
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reportReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reason, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = detailsText,
                    onValueChange = { detailsText = it },
                    label = { Text("Additional details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSubmitReport(selectedReason, detailsText) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_comment_report"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Submit Report")
                    }
                }
            }
        }
    }
}
