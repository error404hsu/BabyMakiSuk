package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_insights")
data class AiInsightEntity(
    @PrimaryKey val id: String,
    val childId: String,
    val title: String,
    val content: String,
    val sourceDate: Long,
    val createdAt: Long
)
