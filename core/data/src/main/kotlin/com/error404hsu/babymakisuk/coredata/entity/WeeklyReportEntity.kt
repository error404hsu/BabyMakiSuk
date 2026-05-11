package com.error404hsu.babymakisuk.coredata.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.error404hsu.babymakisuk.coremodel.GrowthSnapshot
import com.error404hsu.babymakisuk.coremodel.WeeklyReport

@Entity(tableName = "weekly_reports")
data class WeeklyReportEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "week_start")
    val weekStart: String,

    @ColumnInfo(name = "week_end")
    val weekEnd: String,

    @ColumnInfo(name = "ai_summary")
    val aiSummary: String,

    /** 逗號分隔的 medicalVisitId 字串 */
    @ColumnInfo(name = "medical_visit_ids")
    val medicalVisitIds: String = "",

    // GrowthSnapshot 展平儲存
    @ColumnInfo(name = "snapshot_weight")
    val snapshotWeight: Double? = null,

    @ColumnInfo(name = "snapshot_height")
    val snapshotHeight: Double? = null,

    @ColumnInfo(name = "snapshot_head_circ")
    val snapshotHeadCirc: Double? = null,

    /** 逗號分隔 */
    @ColumnInfo(name = "vaccine_due")
    val vaccineDue: String = "",

    /** 逗號分隔，供 FTS 索引 */
    @ColumnInfo(name = "search_keywords")
    val searchKeywords: String = "",

    @ColumnInfo(name = "drive_file_id")
    val driveFileId: String? = null,

    @ColumnInfo(name = "synced_at")
    val syncedAt: Long = 0L
)

fun WeeklyReportEntity.toDomain() = WeeklyReport(
    id = id,
    childId = childId,
    weekStart = weekStart,
    weekEnd = weekEnd,
    aiSummary = aiSummary,
    medicalVisitIds = if (medicalVisitIds.isBlank()) emptyList() else medicalVisitIds.split(","),
    growthSnapshot = if (snapshotWeight == null && snapshotHeight == null && snapshotHeadCirc == null) null
                    else GrowthSnapshot(snapshotWeight, snapshotHeight, snapshotHeadCirc),
    vaccineDue = if (vaccineDue.isBlank()) emptyList() else vaccineDue.split(","),
    searchKeywords = if (searchKeywords.isBlank()) emptyList() else searchKeywords.split(","),
    driveFileId = driveFileId,
    syncedAt = syncedAt
)

fun WeeklyReport.toEntity() = WeeklyReportEntity(
    id = id,
    childId = childId,
    weekStart = weekStart,
    weekEnd = weekEnd,
    aiSummary = aiSummary,
    medicalVisitIds = medicalVisitIds.joinToString(","),
    snapshotWeight = growthSnapshot?.weight,
    snapshotHeight = growthSnapshot?.height,
    snapshotHeadCirc = growthSnapshot?.headCirc,
    vaccineDue = vaccineDue.joinToString(","),
    searchKeywords = searchKeywords.joinToString(","),
    driveFileId = driveFileId,
    syncedAt = syncedAt
)
