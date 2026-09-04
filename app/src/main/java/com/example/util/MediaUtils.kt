package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.media.ExifInterface
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object MediaUtils {
    private const val TAG = "MediaUtils"

    /**
     * Compresses an image Uri into an optimized JPEG byte array and local file.
     * Limits max dimension to 1280px and applies 80% JPEG compression.
     */
    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = 1280,
        maxHeight: Int = 1280,
        quality: Int = 80
    ): Pair<ByteArray, File?> = withContext(Dispatchers.IO) {
        try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            val originalWidth = boundsOptions.outWidth
            val originalHeight = boundsOptions.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                return@withContext Pair(ByteArray(0), null)
            }

            var inSampleSize = 1
            if (originalHeight > maxHeight || originalWidth > maxWidth) {
                val halfHeight = originalHeight / 2
                val halfWidth = originalWidth / 2
                while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            inputStream = context.contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (bitmap == null) {
                return@withContext Pair(ByteArray(0), null)
            }

            // Correct orientation from EXIF if present
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
                Log.w(TAG, "EXIF read error: ${e.localizedMessage}")
            }

            // Compress to byte array
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()

            // Save to cached file
            val outputDir = File(context.cacheDir, "compressed_media").apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, "story_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            val fos = FileOutputStream(outputFile)
            fos.write(bytes)
            fos.flush()
            fos.close()

            Pair(bytes, outputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Image compression failed: ${e.localizedMessage}", e)
            Pair(ByteArray(0), null)
        }
    }

    /**
     * Uploads media bytes to Firebase Storage under folder 'stories/' or 'uploads/'.
     * Falls back gracefully to local file Uri if Firebase Storage is unavailable.
     */
    suspend fun uploadToFirebaseStorage(
        context: Context,
        bytes: ByteArray,
        folder: String = "stories",
        contentType: String = "image/jpeg",
        fallbackFile: File? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val storage = FirebaseStorage.getInstance()
            val filename = "${folder}/${UUID.randomUUID()}.${if (contentType.contains("video")) "mp4" else "jpg"}"
            val ref = storage.reference.child(filename)
            val metadata = StorageMetadata.Builder()
                .setContentType(contentType)
                .build()

            val uploadTask = ref.putBytes(bytes, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Uploaded media to Firebase Storage successfully: $downloadUrl")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload fallback (offline or not configured): ${e.localizedMessage}")
            // Return local file path or cache uri so app remains 100% functional
            return@withContext fallbackFile?.absolutePath ?: ""
        }
    }
}
