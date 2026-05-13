package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val title: String,
    val content: String,
    val tags: String,
    val createdAt: Long,
    val updatedAt: Long
)
