package com.babymakisuk.coredata.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Fts4(contentEntity = MonthlyReportEntity::class)
@Entity(tableName = "monthly_reports_fts")
data class MonthlyReportFts(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,

    @ColumnInfo(name = "ai_summary")
    val aiSummary: String = "",

    @ColumnInfo(name = "search_keywords")
    val searchKeywords: String = ""
)
