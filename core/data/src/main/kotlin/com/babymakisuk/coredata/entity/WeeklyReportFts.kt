package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 外部內容表，對應 WeeklyReportEntity。
 * Room 中 FTS external content 實體的屬性名稱需與外部實體的 ColumnInfo name 一致。
 */
@Fts4(contentEntity = WeeklyReportEntity::class)
@Entity(tableName = "weekly_reports_fts")
data class WeeklyReportFts(
    val ai_summary: String = "",
    val search_keywords: String = ""
)
