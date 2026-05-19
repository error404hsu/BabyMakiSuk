package com.babymakisuk.coremodel

sealed interface ImageStoragePath {
    data class Local(val absolutePath: String) : ImageStoragePath
    data class FirebaseStorage(val storagePath: String) : ImageStoragePath
    data object None : ImageStoragePath
}
