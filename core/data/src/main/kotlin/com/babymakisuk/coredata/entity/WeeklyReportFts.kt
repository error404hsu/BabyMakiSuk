package com.babymakisuk.coredata.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * FTS4 外部內容表，對應 WeeklyReportEntity。
 * Room 中 FTS external content 實體的屬性名稱需與外部實體的 ColumnInfo name 一致。
 */
@Fts4(contentEntity = WeeklyReportEntity::class)
@Entity(tableName = "weekly_reports_fts")
data class WeeklyReportFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,

    @ColumnInfo(name = "ai_summary")
    val aiSummary: String = "",

    @ColumnInfo(name = "search_keywords")
    val searchKeywords: String = ""
)
