package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.Memo

@Entity(
    tableName = "memos",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId"), Index("date")]
)
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val title: String,
    val content: String,
    val date: Long,
    val reminderAt: Long? = null,
    val createdAt: Long
)

fun MemoEntity.toDomain() = Memo(
    id = id,
    childId = childId,
    title = title,
    content = content,
    date = date,
    reminderAt = reminderAt,
    createdAt = createdAt
)

fun Memo.toEntity() = MemoEntity(
    id = id,
    childId = childId,
    title = title,
    content = content,
    date = date,
    reminderAt = reminderAt,
    createdAt = createdAt
)
