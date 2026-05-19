package com.babymakisuk.corefirebase.storage

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
) {
    suspend fun uploadBytes(remotePath: String, data: ByteArray) {
        val ref = storage.reference.child(remotePath)
        ref.putBytes(data).await()
    }

    suspend fun downloadToCache(remotePath: String, cacheDir: File): File {
        val localFile = File(cacheDir, remotePath.replace("/", "_"))
        if (localFile.exists()) return localFile

        val ref = storage.reference.child(remotePath)
        ref.getBytes(Long.MAX_VALUE).await().let { bytes ->
            localFile.parentFile?.mkdirs()
            FileOutputStream(localFile).use { it.write(bytes) }
        }
        return localFile
    }

    suspend fun delete(remotePath: String) {
        storage.reference.child(remotePath).delete().await()
    }
}
