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
    /**
     * childId: 0L in DB corresponds to global reminder (mapped to null in domain).
     * TODO: In a future MIGRATION_14_15, make this column NULLABLE in SQLite
     * and remove the 0L sentinel value convention.
     */
    val childId: Long,
    val type: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val resolvedAt: Long?
)

fun SystemReminderEntity.toDomain() = SystemReminder(
    id = id,
    // childId == 0L in DB → map to null in domain
    childId = if (childId == 0L) null else childId,
    type = try { SystemReminderType.valueOf(type) } catch (_: Exception) { SystemReminderType.LONG_NO_BM },
    title = title,
    content = content,
    createdAt = createdAt,
    resolvedAt = resolvedAt
)

fun SystemReminder.toEntity() = SystemReminderEntity(
    id = id,
    // null in domain → store as 0L in DB
    childId = childId ?: 0L,
    type = type.name,
    title = title,
    content = content,
    createdAt = createdAt,
    resolvedAt = resolvedAt
)
