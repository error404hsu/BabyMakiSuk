package com.babymakisuk.coredata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrescriptionImagePreprocessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val MAX_DIMENSION = 2048
        private const val JPEG_QUALITY = 85
    }

    fun process(uri: Uri): Bitmap {
        val rotated = fixExifRotation(uri)
        return resize(rotated)
    }

    private fun fixExifRotation(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("無法讀取圖片 Uri: $uri")

        val bitmap = inputStream.use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("無法解碼圖片: $uri")

        return try {
            val exifStream = context.contentResolver.openInputStream(uri)
            val exif = exifStream?.use { ExifInterface(it) }
            val rotation = when (exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotation == 0f) return bitmap

            Matrix().apply { postRotate(rotation) }.let { matrix ->
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    .also { if (it != bitmap) bitmap.recycle() }
            }
        } catch (_: Exception) {
            bitmap
        }
    }

    private fun resize(source: Bitmap): Bitmap {
        val (srcW, srcH) = source.width to source.height
        if (srcW <= MAX_DIMENSION && srcH <= MAX_DIMENSION) return source

        val scale = minOf(
            MAX_DIMENSION.toFloat() / srcW,
            MAX_DIMENSION.toFloat() / srcH
        )
        val destW = (srcW * scale).toInt()
        val destH = (srcH * scale).toInt()

        return Bitmap.createScaledBitmap(source, destW, destH, true)
            .also { if (it != source) source.recycle() }
    }

    fun saveToInternal(bitmap: Bitmap): File {
        val dir = File(context.filesDir, "prescriptions")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "rx_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return file
    }

    fun cleanupOldFiles(retentionDays: Int = 7) {
        val dir = File(context.filesDir, "prescriptions")
        if (!dir.exists()) return

        val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}
