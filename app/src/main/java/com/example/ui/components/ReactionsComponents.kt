package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.PostReaction
import com.example.data.SentimentReaction
import kotlinx.coroutines.delay

private val FacebookBlue = Color(0xFF1877F2)

/**
 * Floating Sentiment Reaction Picker Bar
 * Displays expressive animated emojis (Like, Love, Care, Haha, Wow, Sad, Angry)
 * with spring scaling and active feedback.
 */
@Composable
fun ReactionFloatingPicker(
    modifier: Modifier = Modifier,
    currentReactionId: String? = null,
    onReactionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.25f))
            .testTag("reaction_floating_picker"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .wrapContentSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SentimentReaction.entries.forEach { sentiment ->
                ReactionPickerItem(
                    sentiment = sentiment,
                    isSelected = currentReactionId.equals(sentiment.id, ignoreCase = true),
                    onClick = {
                        onReactionSelected(sentiment.id)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionPickerItem(
    sentiment: SentimentReaction,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (isHovered || isSelected) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "reaction_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("reaction_item_${sentiment.id.lowercase()}")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHovered = true
                        tryAwaitRelease()
                        isHovered = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .scale(scaleAnim)
        ) {
            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = Color(sentiment.colorHex).copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {}
            }
            Text(
                text = sentiment.emoji,
                fontSize = 26.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * Summary badge showing overlapping sentiment reaction emojis and formatted count.
 */
@Composable
fun PostReactionsSummary(
    reactions: List<PostReaction>,
    totalLikesCount: Int,
    isLikedByMe: Boolean,
    userReactionId: String?,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distinctSentiments = remember(reactions, userReactionId) {
        val set = mutableListOf<SentimentReaction>()
        // If current user reacted, ensure user sentiment is prominent
        SentimentReaction.fromId(userReactionId)?.let { set.add(it) }
        reactions.forEach { r ->
            SentimentReaction.fromId(r.reactionType)?.let { s ->
                if (!set.contains(s)) set.add(s)
            }
        }
        if (set.isEmpty() && totalLikesCount > 0) {
            set.add(SentimentReaction.LIKE)
        }
        set.take(3)
    }

    val displayCount = maxOf(totalLikesCount, reactions.size)

    if (displayCount <= 0 && !isLikedByMe) {
        return
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenDetail)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("post_reactions_summary"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Overlapping sentiment circles
        Box(contentAlignment = Alignment.CenterStart) {
            distinctSentiments.forEachIndexed { index, sentiment ->
                Surface(
                    shape = CircleShape,
                    color = Color(sentiment.colorHex),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .padding(start = (index * 14).dp)
                        .size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = sentiment.emoji,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        val paddingStart = if (distinctSentiments.size > 1) ((distinctSentiments.size - 1) * 14 + 4).dp else 4.dp
        Spacer(modifier = Modifier.width(paddingStart))

        val summaryText = remember(displayCount, isLikedByMe, reactions) {
            when {
                isLikedByMe && displayCount <= 1 -> "You"
                isLikedByMe && displayCount == 2 -> "You and 1 other"
                isLikedByMe && displayCount > 2 -> "You and ${displayCount - 1} others"
                reactions.isNotEmpty() -> {
                    val firstName = reactions.first().userName.split(" ").firstOrNull() ?: "Member"
                    if (displayCount == 1) firstName else "$firstName and ${displayCount - 1} others"
                }
                displayCount == 1 -> "1 reaction"
                else -> "$displayCount reactions"
            }
        }

        Text(
            text = summaryText,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Detailed Dialog displaying all sentiment reactions categorized by type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostReactionsDetailDialog(
    reactions: List<PostReaction>,
    currentUserId: String = "currentUser",
    onDismiss: () -> Unit
) {
    var selectedFilterTab by remember { mutableStateOf<String?>("ALL") }

    val sentimentCounts = remember(reactions) {
        val map = mutableMapOf<String, Int>()
        reactions.forEach { r ->
            val type = r.reactionType.uppercase()
            map[type] = (map[type] ?: 0) + 1
        }
        map
    }

    val filteredReactions = remember(reactions, selectedFilterTab) {
        if (selectedFilterTab == "ALL" || selectedFilterTab == null) {
            reactions
        } else {
            reactions.filter { it.reactionType.equals(selectedFilterTab, ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.70f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("post_reactions_detail_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = FacebookBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Reactions (${reactions.size})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Filter Tabs (All, Like, Love, Care, etc.)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterTab == "ALL",
                            onClick = { selectedFilterTab = "ALL" },
                            label = { Text("All ${reactions.size}") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("reaction_filter_tab_all")
                        )
                    }

                    SentimentReaction.entries.forEach { sentiment ->
                        val count = sentimentCounts[sentiment.id] ?: 0
                        if (count > 0) {
                            item {
                                FilterChip(
                                    selected = selectedFilterTab == sentiment.id,
                                    onClick = { selectedFilterTab = sentiment.id },
                                    leadingIcon = {
                                        Text(sentiment.emoji, fontSize = 14.sp)
                                    },
                                    label = { Text("$count") },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("reaction_filter_tab_${sentiment.id.lowercase()}")
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                // List of users who reacted
                if (filteredReactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No reactions in this category",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredReactions, key = { "${it.postId}_${it.userId}" }) { reaction ->
                            val sentiment = SentimentReaction.fromId(reaction.reactionType) ?: SentimentReaction.LIKE
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .testTag("reaction_user_row_${reaction.userId}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar with badge overlay
                                Box(modifier = Modifier.size(44.dp)) {
                                    if (reaction.userAvatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = reaction.userAvatarUrl,
                                            contentDescription = reaction.userName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = FacebookBlue.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = reaction.userName.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = FacebookBlue
                                                )
                                            }
                                        }
                                    }

                                    // Small reaction sentiment emoji badge in corner
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(sentiment.colorHex),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.BottomEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = sentiment.emoji,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (reaction.userId == currentUserId) "${reaction.userName} (You)" else reaction.userName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sentiment.label} • ${sentiment.emoji}",
                                        fontSize = 12.sp,
                                        color = Color(sentiment.colorHex),
                                        fontWeight = FontWeight.Medium
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
 * Double Tap Heart / Sentiment Pop Animation Overlay
 */
@Composable
fun DoubleTapSentimentBurst(
    trigger: Boolean,
    sentimentEmoji: String = "❤️",
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!trigger) return

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger) {
            visible = true
            delay(800)
            visible = false
            onAnimationEnd()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.3f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn(),
        exit = scaleOut(targetScale = 1.3f) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.35f),
            modifier = Modifier.size(90.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = sentimentEmoji,
                    fontSize = 52.sp
                )
            }
        }
    }
}

/**
 * Interactive button that reflects the active sentiment reaction
 * and supports tap, long-press to open reaction picker.
 */
@Composable
fun SentimentInteractionButton(
    sentiment: SentimentReaction?,
    isLiked: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onOpenPicker: () -> Unit,
    testTag: String
) {
    val activeColor = sentiment?.let { Color(it.colorHex) } ?: FacebookBlue
    val textColor = if (isLiked) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (isLiked) (sentiment?.label ?: "Like") else "Like"
    val emoji = if (isLiked) (sentiment?.emoji ?: "👍") else null

    Row(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(isLiked, sentiment) {
                detectTapGestures(
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                fontSize = 17.sp,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.ThumbUp,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        // Subtle reaction picker affordance indicator
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onOpenPicker() }
                .padding(2.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Reactions menu",
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

