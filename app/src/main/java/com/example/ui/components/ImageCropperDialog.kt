package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.FbBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class CropAspectRatio(val label: String, val ratio: Float?, val isCircle: Boolean = false) {
    FREE("Free", null),
    SQUARE("1:1 (Square)", 1f / 1f),
    CIRCLE("Circle (Avatar)", 1f / 1f, isCircle = true),
    PORTRAIT_4_5("4:5 (Post)", 4f / 5f),
    STORY_9_16("9:16 (Story/Reel)", 9f / 16f),
    LANDSCAPE_16_9("16:9 (Cover)", 16f / 9f)
}

/**
 * Material 3 Image Cropping Dialog.
 * Allows users to adjust the aspect ratio (1:1, Circle, 4:5, 16:9, 9:16, Free),
 * pan and zoom, rotate 90 degrees, and save the cropped bitmap directly to cache storage.
 */
@Composable
fun ImageCropperDialog(
    imageUri: Uri,
    initialAspectRatio: CropAspectRatio = CropAspectRatio.SQUARE,
    allowedAspectRatios: List<CropAspectRatio> = CropAspectRatio.values().toList(),
    title: String = "Adjust & Crop Photo",
    onDismiss: () -> Unit,
    onCropSuccess: (croppedUri: Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedRatio by remember { mutableStateOf(initialAspectRatio) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var rotationDegrees by remember { mutableIntStateOf(0) }

    // Transformation state
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Dimensions
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Load original bitmap
    LaunchedEffect(imageUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val input = context.contentResolver.openInputStream(imageUri)
                val bmp = BitmapFactory.decodeStream(input)
                input?.close()
                loadedBitmap = bmp
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("image_cropper_dialog"),
            color = Color(0xFF101214) // Dark photo-editing canvas
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = {
                            val bmp = loadedBitmap ?: return@Button
                            if (viewportSize.width == 0 || viewportSize.height == 0) return@Button

                            isSaving = true
                            // Execute crop in background
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val croppedFile = cropAndSaveImage(
                                        context = context,
                                        sourceBitmap = bmp,
                                        rotation = rotationDegrees,
                                        viewportSize = viewportSize,
                                        scale = scale,
                                        pan = panOffset,
                                        aspectRatio = selectedRatio
                                    )
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        onCropSuccess(Uri.fromFile(croppedFile))
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue),
                        shape = RoundedCornerShape(20.dp),
                        enabled = loadedBitmap != null && !isSaving,
                        modifier = Modifier.testTag("crop_save_btn")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Done",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Interactive Cropper Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .onSizeChanged { viewportSize = it }
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                panOffset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = FbBlue)
                    } else if (loadedBitmap != null && viewportSize.width > 0 && viewportSize.height > 0) {
                        val bmp = loadedBitmap!!
                        val effectiveRot = (rotationDegrees % 360 + 360) % 360

                        // Calculate crop frame rectangle inside the viewport
                        val cropRect = remember(viewportSize, selectedRatio) {
                            calculateCropRect(viewportSize, selectedRatio)
                        }

                        Canvas(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            // Draw image with pan, zoom and rotation
                            val isSwapDims = effectiveRot == 90 || effectiveRot == 270
                            val origBmpW = if (isSwapDims) bmp.height else bmp.width
                            val origBmpH = if (isSwapDims) bmp.width else bmp.height

                            // Base fit scale to cover or nicely fit the crop rect
                            val baseScale = max(
                                cropRect.width / origBmpW.toFloat(),
                                cropRect.height / origBmpH.toFloat()
                            )
                            val finalScale = baseScale * scale

                            // Draw transformed bitmap
                            drawContext.canvas.save()
                            drawContext.canvas.translate(
                                canvasWidth / 2f + panOffset.x,
                                canvasHeight / 2f + panOffset.y
                            )
                            drawContext.canvas.rotate(effectiveRot.toFloat())
                            drawContext.canvas.scale(finalScale, finalScale)

                            // Center the bitmap around (0,0)
                            val dstX = -bmp.width / 2f
                            val dstY = -bmp.height / 2f
                            drawImage(
                                image = bmp.asImageBitmap(),
                                topLeft = Offset(dstX, dstY)
                            )
                            drawContext.canvas.restore()

                            // Draw semi-transparent dark overlay outside the crop rect
                            if (selectedRatio.isCircle) {
                                val circlePath = Path().apply {
                                    addOval(cropRect)
                                }
                                clipPath(circlePath, clipOp = ClipOp.Difference) {
                                    drawRect(color = Color.Black.copy(alpha = 0.72f))
                                }
                                // Draw circular outline
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.85f),
                                    radius = cropRect.width / 2f,
                                    center = cropRect.center,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )
                            } else {
                                clipRect(
                                    left = cropRect.left,
                                    top = cropRect.top,
                                    right = cropRect.right,
                                    bottom = cropRect.bottom,
                                    clipOp = ClipOp.Difference
                                ) {
                                    drawRect(color = Color.Black.copy(alpha = 0.72f))
                                }

                                // Draw crop rectangle frame border
                                drawRect(
                                    color = Color.White.copy(alpha = 0.85f),
                                    topLeft = cropRect.topLeft,
                                    size = cropRect.size,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                )

                                // Draw grid guideline rule of thirds
                                val thirdW = cropRect.width / 3f
                                val thirdH = cropRect.height / 3f
                                val gridColor = Color.White.copy(alpha = 0.25f)
                                val strokeWidth = 1.dp.toPx()

                                // Vertical lines
                                drawLine(
                                    color = gridColor,
                                    start = Offset(cropRect.left + thirdW, cropRect.top),
                                    end = Offset(cropRect.left + thirdW, cropRect.bottom),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = gridColor,
                                    start = Offset(cropRect.left + thirdW * 2, cropRect.top),
                                    end = Offset(cropRect.left + thirdW * 2, cropRect.bottom),
                                    strokeWidth = strokeWidth
                                )
                                // Horizontal lines
                                drawLine(
                                    color = gridColor,
                                    start = Offset(cropRect.left, cropRect.top + thirdH),
                                    end = Offset(cropRect.right, cropRect.top + thirdH),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    color = gridColor,
                                    start = Offset(cropRect.left, cropRect.top + thirdH * 2),
                                    end = Offset(cropRect.right, cropRect.top + thirdH * 2),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }
                    }
                }

                // Controls: Quick Rotate & Reset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            scale = 1f
                            panOffset = Offset.Zero
                            rotationDegrees = 0
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset", color = Color.White.copy(alpha = 0.8f))
                    }

                    IconButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90) % 360
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = Color.White
                        )
                    }
                }

                // Aspect Ratio Selector Chips
                Surface(
                    color = Color(0xFF1E2226),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Aspect Ratio",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )

                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            items(allowedAspectRatios.size) { index ->
                                val ratio = allowedAspectRatios[index]
                                val isSelected = selectedRatio == ratio
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedRatio = ratio
                                        // Reset pan and scale to fit new frame nicely
                                        scale = 1f
                                        panOffset = Offset.Zero
                                    },
                                    label = {
                                        Text(
                                            text = ratio.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FbBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF2B3036),
                                        labelColor = Color.White.copy(alpha = 0.8f)
                                    ),
                                    modifier = Modifier.testTag("crop_ratio_${ratio.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculates a centered crop rectangle given the container size and selected aspect ratio.
 */
private fun calculateCropRect(viewportSize: IntSize, ratio: CropAspectRatio): Rect {
    val padding = 24f
    val availW = (viewportSize.width - padding * 2).coerceAtLeast(100f)
    val availH = (viewportSize.height - padding * 2).coerceAtLeast(100f)

    val targetRatio = ratio.ratio ?: (availW / availH)

    val (w, h) = if (availW / availH > targetRatio) {
        // Limited by height
        val h = availH
        val w = h * targetRatio
        w to h
    } else {
        // Limited by width
        val w = availW
        val h = w / targetRatio
        w to h
    }

    val left = (viewportSize.width - w) / 2f
    val top = (viewportSize.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

/**
 * Crops the bitmap according to the viewport transformations and saves the result to a cached JPEG file.
 */
private fun cropAndSaveImage(
    context: Context,
    sourceBitmap: Bitmap,
    rotation: Int,
    viewportSize: IntSize,
    scale: Float,
    pan: Offset,
    aspectRatio: CropAspectRatio
): File {
    val effectiveRot = (rotation % 360 + 360) % 360

    // Rotate bitmap if needed
    val workingBitmap = if (effectiveRot != 0) {
        val matrix = Matrix().apply { postRotate(effectiveRot.toFloat()) }
        Bitmap.createBitmap(
            sourceBitmap,
            0,
            0,
            sourceBitmap.width,
            sourceBitmap.height,
            matrix,
            true
        )
    } else {
        sourceBitmap
    }

    val cropRect = calculateCropRect(viewportSize, aspectRatio)

    val origW = workingBitmap.width.toFloat()
    val origH = workingBitmap.height.toFloat()

    val baseScale = max(
        cropRect.width / origW,
        cropRect.height / origH
    )
    val finalScale = baseScale * scale

    // In canvas coordinates, bitmap center is at:
    // (viewportSize.width / 2 + pan.x, viewportSize.height / 2 + pan.y)
    val bmpCenterXInCanvas = viewportSize.width / 2f + pan.x
    val bmpCenterYInCanvas = viewportSize.height / 2f + pan.y

    // Top-left of displayed bitmap on canvas:
    val bmpLeftInCanvas = bmpCenterXInCanvas - (origW * finalScale) / 2f
    val bmpTopInCanvas = bmpCenterYInCanvas - (origH * finalScale) / 2f

    // CropRect relative to displayed bitmap:
    val cropLeftOnBmp = (cropRect.left - bmpLeftInCanvas) / finalScale
    val cropTopOnBmp = (cropRect.top - bmpTopInCanvas) / finalScale
    val cropWOnBmp = cropRect.width / finalScale
    val cropHOnBmp = cropRect.height / finalScale

    // Clamp coordinates to bitmap bounds
    val safeX = cropLeftOnBmp.coerceIn(0f, origW - 1f).roundToInt()
    val safeY = cropTopOnBmp.coerceIn(0f, origH - 1f).roundToInt()
    val safeW = cropWOnBmp.roundToInt().coerceIn(1, (origW - safeX).toInt())
    val safeH = cropHOnBmp.roundToInt().coerceIn(1, (origH - safeY).toInt())

    val cropped = Bitmap.createBitmap(workingBitmap, safeX, safeY, safeW, safeH)

    val outputFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
    val fos = FileOutputStream(outputFile)
    cropped.compress(Bitmap.CompressFormat.JPEG, 92, fos)
    fos.flush()
    fos.close()

    return outputFile
}
