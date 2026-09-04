package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executor

/**
 * Production-ready Camera Hardware & Media Capture Helper for Yarkhoon.com.
 *
 * Handles:
 * - Direct camera hardware access & CameraX integration (photo capture & viewfinder)
 * - Safe FileProvider URI generation for high-resolution picture & video intent capture
 * - EXIF orientation correction and dimension optimization for User Posts & Marketplace Listings
 * - Video thumbnail generation and permission resolution
 */
object CameraCaptureHelper {

    private const val TAG = "CameraCaptureHelper"

    // ==========================================
    // 1. PERMISSIONS & SYSTEM CHECKS
    // ==========================================

    /**
     * Checks if the app currently has camera hardware permission.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the app currently has audio recording permission (needed for video capture).
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns the appropriate array of permissions required for photo or video capture.
     */
    fun getRequiredPermissions(isVideo: Boolean): Array<String> {
        return if (isVideo) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(Manifest.permission.CAMERA)
        }
    }

    /**
     * Checks if the device has a physical camera.
     */
    fun hasCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    // ==========================================
    // 2. TEMPORARY MEDIA FILE & URI GENERATION
    // ==========================================

    /**
     * Creates a new temporary image file in the app's external pictures cache and returns its FileProvider Uri.
     * Used for full-resolution photo capture in User Posts and Marketplace listings.
     */
    fun createPhotoCaptureUri(context: Context, prefix: String = "post_photo"): Pair<Uri, File>? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(context.getExternalFilesDir("Pictures"), "CameraCaptures").apply {
                if (!exists()) mkdirs()
            }
            val file = File(storageDir, "${prefix}_${timeStamp}_${UUID.randomUUID().toString().take(6)}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            Pair(uri, file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create photo capture URI: ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Creates a new temporary video file in the app's external movies cache and returns its FileProvider Uri.
     * Used for recording video for User Posts and Marketplace product demos.
     */
    fun createVideoCaptureUri(context: Context, prefix: String = "post_video"): Pair<Uri, File>? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = File(context.getExternalFilesDir("Movies"), "VideoCaptures").apply {
                if (!exists()) mkdirs()
            }
            val file = File(storageDir, "${prefix}_${timeStamp}_${UUID.randomUUID().toString().take(6)}.mp4")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            Pair(uri, file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video capture URI: ${e.localizedMessage}", e)
            null
        }
    }

    // ==========================================
    // 3. CAMERAX INTEGRATION
    // ==========================================

    /**
     * Binds CameraX Preview and ImageCapture use cases to a PreviewView and LifecycleOwner.
     */
    fun bindCameraX(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onReady: (ImageCapture) -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                onReady(imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "CameraX binding failed: ${e.localizedMessage}", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Captures a still photo directly using CameraX ImageCapture use case.
     */
    fun takeCameraXPhoto(
        context: Context,
        imageCapture: ImageCapture,
        prefix: String = "camera_capture",
        executor: Executor = ContextCompat.getMainExecutor(context),
        onImageSaved: (Uri, File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val targetPair = createPhotoCaptureUri(context, prefix)
        if (targetPair == null) {
            onError(IllegalStateException("Unable to allocate storage for camera capture"))
            return
        }

        val (uri, file) = targetPair
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onImageSaved(uri, file)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "CameraX photo capture failed: ${exception.message}", exception)
                    onError(exception)
                }
            }
        )
    }

    // ==========================================
    // 4. PHOTO & VIDEO OPTIMIZATION FOR POSTS & MARKETPLACE
    // ==========================================

    /**
     * Processes and optimizes a photo captured or chosen for a Marketplace listing.
     * Ensures clean EXIF orientation, balanced 1200x1200 max bounding box, and 85% JPEG compression.
     */
    suspend fun processMarketplacePhoto(
        context: Context,
        photoUri: Uri
    ): Pair<Uri, File>? = withContext(Dispatchers.IO) {
        processAndOptimizeImage(
            context = context,
            imageUri = photoUri,
            maxWidth = 1200,
            maxHeight = 1200,
            quality = 85,
            subDir = "marketplace_items"
        )
    }

    /**
     * Processes and optimizes a photo captured or chosen for a User Post / Story.
     * High fidelity (up to 1920x1920) with 88% JPEG compression.
     */
    suspend fun processPostPhoto(
        context: Context,
        photoUri: Uri
    ): Pair<Uri, File>? = withContext(Dispatchers.IO) {
        processAndOptimizeImage(
            context = context,
            imageUri = photoUri,
            maxWidth = 1920,
            maxHeight = 1920,
            quality = 88,
            subDir = "user_posts"
        )
    }

    /**
     * Internal image compression, EXIF auto-rotation, and local caching routine.
     */
    private suspend fun processAndOptimizeImage(
        context: Context,
        imageUri: Uri,
        maxWidth: Int,
        maxHeight: Int,
        quality: Int,
        subDir: String
    ): Pair<Uri, File>? = withContext(Dispatchers.IO) {
        try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return@withContext null

            var inSampleSize = 1
            if (origH > maxHeight || origW > maxWidth) {
                val halfH = origH / 2
                val halfW = origW / 2
                while ((halfH / inSampleSize) >= maxHeight && (halfW / inSampleSize) >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            inputStream = context.contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) return@withContext null

            // EXIF Orientation Correction
            try {
                val exifStream = context.contentResolver.openInputStream(imageUri)
                if (exifStream != null) {
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    exifStream.close()

                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    }

                    if (!matrix.isIdentity) {
                        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        if (rotated != bitmap) {
                            bitmap.recycle()
                            bitmap = rotated
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "EXIF parse skipped: ${e.localizedMessage}")
            }

            val outputDir = File(context.cacheDir, subDir).apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, "media_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            val outputStream = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            bitmap.recycle()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, outputFile)
            Pair(uri, outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Image optimization failed: ${e.localizedMessage}", e)
            null
        }
    }

    /**
     * Extracts a frame thumbnail from a recorded video Uri.
     */
    suspend fun extractVideoThumbnail(
        context: Context,
        videoUri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val frame = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            frame
        } catch (e: Exception) {
            Log.w(TAG, "Video thumbnail extraction failed: ${e.localizedMessage}")
            null
        }
    }
}
