package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FullScreenProfileImageViewer(
    imageUrl: String,
    userName: String = "",
    subtitle: String = "",
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dragDismissOffsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scaleAnim"
    )

    // Calculate background alpha based on swipe-down dismissal distance
    val dismissAlpha = (1f - (dragDismissOffsetY / 600f).coerceIn(0f, 0.8f))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dismissAlpha))
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("full_screen_profile_zoom_dialog")
        ) {
            // Main Zoomable & Pannable Image Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, dragDismissOffsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                scale = if (scale > 1.2f) 1f else 2.5f
                                offset = Offset.Zero
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale

                            if (scale > 1.05f) {
                                // Pan when zoomed in
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                // Swipe down to dismiss when at normal zoom
                                if (pan.y > 0 || dragDismissOffsetY > 0) {
                                    dragDismissOffsetY = (dragDismissOffsetY + pan.y).coerceAtLeast(0f)
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                if (dragDismissOffsetY > 180f) {
                                    onDismiss()
                                } else {
                                    coroutineScope.launch {
                                        dragDismissOffsetY = 0f
                                    }
                                }
                            },
                            onDragCancel = {
                                dragDismissOffsetY = 0f
                            },
                            onDrag = { _, dragAmount ->
                                if (scale <= 1.05f) {
                                    dragDismissOffsetY = (dragDismissOffsetY + dragAmount.y).coerceAtLeast(0f)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl.ifBlank { "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=800" },
                    contentDescription = "Zoomed Profile Picture",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }

            // Top Header Overlay: User Info and Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("zoom_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    if (userName.isNotBlank()) {
                        Column {
                            Text(
                                text = userName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Quick zoom toggle button
                IconButton(
                    onClick = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offset = Offset.Zero
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (scale > 1.2f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                        contentDescription = "Toggle Zoom",
                        tint = Color.White
                    )
                }
            }

            // Bottom hint bar
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Pinch or double tap to zoom • Swipe down to dismiss",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
