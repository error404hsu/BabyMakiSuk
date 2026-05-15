package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.SystemReminder
import com.babymakisuk.coremodel.SystemReminderType

@Entity(
    tableName = "system_reminders",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")]
)
data class SystemReminderEntity(
    @PrimaryKey val id: String,
    val childId: Long,
    val type: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val resolvedAt: Long?
)

fun SystemReminderEntity.toDomain() = SystemReminder(
    id = id,
    childId = childId,
    type = try { SystemReminderType.valueOf(type) } catch (_: Exception) { SystemReminderType.LONG_NO_BM },
    title = title,
    content = content,
    createdAt = createdAt,
    resolvedAt = resolvedAt
)

fun SystemReminder.toEntity() = SystemReminderEntity(
    id = id,
    childId = childId,
    type = type.name,
    title = title,
    content = content,
    createdAt = createdAt,
    resolvedAt = resolvedAt
)
