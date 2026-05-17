package com.babymakisuk.coredata.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.GrowthSnapshot
import com.babymakisuk.coremodel.MonthlyReport

@Entity(tableName = "monthly_reports")
data class MonthlyReportEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "child_id")
    val childId: Long, // Changed from String to Long

    @ColumnInfo(name = "month_start")
    val monthStart: String,

    @ColumnInfo(name = "month_end")
    val monthEnd: String,

    @ColumnInfo(name = "ai_summary")
    val aiSummary: String,

    @ColumnInfo(name = "snapshot_weight")
    val snapshotWeight: Double? = null,

    @ColumnInfo(name = "snapshot_height")
    val snapshotHeight: Double? = null,

    @ColumnInfo(name = "snapshot_head_circ")
    val snapshotHeadCirc: Double? = null,

    @ColumnInfo(name = "medical_count")
    val medicalCount: Int = 0,

    @ColumnInfo(name = "system_reminder_count")
    val systemReminderCount: Int = 0,

    @ColumnInfo(name = "search_keywords")
    val searchKeywords: String = "",

    @ColumnInfo(name = "drive_file_id")
    val driveFileId: String? = null,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long = 0L
)

fun MonthlyReportEntity.toDomain() = MonthlyReport(
    id = id,
    childId = childId,
    monthStart = monthStart,
    monthEnd = monthEnd,
    aiSummary = aiSummary,
    growthSnapshot = if (snapshotWeight == null && snapshotHeight == null && snapshotHeadCirc == null) null
    else GrowthSnapshot(snapshotWeight, snapshotHeight, snapshotHeadCirc),
    medicalCount = medicalCount,
    systemReminderCount = systemReminderCount,
    searchKeywords = if (searchKeywords.isBlank()) emptyList() else searchKeywords.split(","),
    driveFileId = driveFileId,
    syncedAt = syncedAt
)

fun MonthlyReport.toEntity() = MonthlyReportEntity(
    id = id,
    childId = childId,
    monthStart = monthStart,
    monthEnd = monthEnd,
    aiSummary = aiSummary,
    snapshotWeight = growthSnapshot?.weight,
    snapshotHeight = growthSnapshot?.height,
    snapshotHeadCirc = growthSnapshot?.headCirc,
    medicalCount = medicalCount,
    systemReminderCount = systemReminderCount,
    searchKeywords = searchKeywords.joinToString(","),
    driveFileId = driveFileId,
    syncedAt = syncedAt
)
