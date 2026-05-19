package com.babymakisuk.corefirebase.storage

import android.content.Context
import android.net.Uri
import com.babymakisuk.coremodel.ImageStoragePath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicalImageCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageRepository: StorageRepository,
) {
    suspend fun getImageUri(path: ImageStoragePath): Uri? = when (path) {
        is ImageStoragePath.Local -> {
            val file = File(path.absolutePath)
            if (file.exists()) Uri.fromFile(file) else null
        }
        is ImageStoragePath.FirebaseStorage -> {
            val cacheDir = File(context.cacheDir, "medical_images")
            val localFile = storageRepository.downloadToCache(path.storagePath, cacheDir)
            Uri.fromFile(localFile)
        }
        ImageStoragePath.None -> null
    }
}
