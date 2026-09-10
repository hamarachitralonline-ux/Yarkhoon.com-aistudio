package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FacebookBlue = Color(0xFF1877F2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActionSheet(
    onDismiss: () -> Unit,
    onCreatePost: () -> Unit,
    onAddStory: () -> Unit,
    onSellItem: () -> Unit,
    onPostService: () -> Unit,
    onCreateGroup: () -> Unit,
    onCreateWithAi: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.testTag("create_action_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Create",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // AI Creation Option - Hidden as requested: Media Studio (code preserved)
            val showMediaStudioOption = false
            if (showMediaStudioOption) {
                CreateOptionItem(
                    icon = Icons.Filled.AutoAwesome,
                    iconBg = Color(0xFF6366F1),
                    title = "Create with AI (Images, Music, Video)",
                    subtitle = "Generate art, Lyria music, or Veo 3 animations",
                    onClick = {
                        onDismiss()
                        onCreateWithAi()
                    }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            CreateOptionItem(
                icon = Icons.Filled.EditNote,
                iconBg = FacebookBlue,
                title = "Post",
                subtitle = "Share what's happening with the Yarkhoon community",
                onClick = {
                    onDismiss()
                    onCreatePost()
                }
            )


            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            CreateOptionItem(
                icon = Icons.Filled.AddAPhoto,
                iconBg = Color(0xFFE91E63),
                title = "Story",
                subtitle = "Share a photo, video, or text story for 24 hours",
                onClick = {
                    onDismiss()
                    onAddStory()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            CreateOptionItem(
                icon = Icons.Filled.Storefront,
                iconBg = Color(0xFF45BD62),
                title = "Marketplace Listing",
                subtitle = "Sell goods, cars, livestock, or crafts in Chitral",
                onClick = {
                    onDismiss()
                    onSellItem()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            CreateOptionItem(
                icon = Icons.Filled.Build,
                iconBg = Color(0xFFF7B928),
                title = "Service Listing",
                subtitle = "Offer your skills, transport, tuition, or repairs",
                onClick = {
                    onDismiss()
                    onPostService()
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            CreateOptionItem(
                icon = Icons.Filled.Groups,
                iconBg = Color(0xFF7B1FA2),
                title = "Community Group",
                subtitle = "Create a group for your village, school, or interest",
                onClick = {
                    onDismiss()
                    onCreateGroup()
                }
            )
        }
    }
}

@Composable
private fun CreateOptionItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = iconBg,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}
