package com.babymakisuk.corefirebase.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface ImageUploadRepository {
    suspend fun uploadPrescription(childId: Long, visitId: Long, localPath: String): String
}

@Singleton
class DefaultImageUploadRepository @Inject constructor(
    private val storageRepository: StorageRepository,
) : ImageUploadRepository {

    companion object {
        private const val MAX_SIZE_BYTES = 200 * 1024
        private const val MAX_DIMENSION = 2048
        private const val INITIAL_QUALITY = 85
    }

    override suspend fun uploadPrescription(childId: Long, visitId: Long, localPath: String): String {
        val imageBytes = compressToTargetSize(java.io.File(localPath))
        val remotePath = "children/$childId/medical/$visitId.jpg"
        storageRepository.uploadBytes(remotePath, imageBytes)
        return remotePath
    }

    private fun compressToTargetSize(file: java.io.File): ByteArray {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val scale = minOf(
            MAX_DIMENSION.toFloat() / options.outWidth,
            MAX_DIMENSION.toFloat() / options.outHeight,
            1f
        )
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = (1f / scale).toInt().coerceAtLeast(1)
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
            ?: throw IllegalStateException("Failed to decode image: $file")

        var quality = INITIAL_QUALITY
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)

        while (output.size() > MAX_SIZE_BYTES && quality > 10) {
            output.reset()
            quality -= 5
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        }
        bitmap.recycle()
        return output.toByteArray()
    }
}
