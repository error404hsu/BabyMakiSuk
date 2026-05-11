package com.error404hsu.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 虛擬表，contentEntity 指向 WeeklyReportEntity。
 * Room 會自動建立觸發器以保持內容同步。
 */
@Fts4(contentEntity = WeeklyReportEntity::class)
@Entity(tableName = "weekly_reports_fts")
data class WeeklyReportFts(
    val aiSummary: String,
    val searchKeywords: String
)
