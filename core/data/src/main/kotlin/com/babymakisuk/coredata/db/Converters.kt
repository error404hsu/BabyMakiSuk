package com.babymakisuk.coredata.db

import androidx.room.TypeConverter
import com.babymakisuk.coremodel.ImageStoragePath
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromLocalDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromImageStoragePath(path: ImageStoragePath): String? = when (path) {
        is ImageStoragePath.Local -> "local:${path.absolutePath}"
        is ImageStoragePath.FirebaseStorage -> "firebase:${path.storagePath}"
        ImageStoragePath.None -> null
    }

    @TypeConverter
    fun toImageStoragePath(value: String?): ImageStoragePath = when {
        value == null -> ImageStoragePath.None
        value.startsWith("local:") -> ImageStoragePath.Local(value.removePrefix("local:"))
        value.startsWith("firebase:") -> ImageStoragePath.FirebaseStorage(value.removePrefix("firebase:"))
        else -> ImageStoragePath.Local(value)
    }
}
