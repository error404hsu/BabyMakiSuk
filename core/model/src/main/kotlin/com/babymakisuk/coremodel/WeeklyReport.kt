package com.babymakisuk.coremodel

/**
 * Phase E-0 WeeklyReport domain model
 * id 譬ｼ蠑擾ｼ・{childId}_{year}-W{week}"・井ｾ具ｼ・1_2026-W20"・・
 */
data class WeeklyReport(
    val id: String,                       // "{childId}_{year}-W{week}"
    val childId: String,
    val weekStart: String,                // "2026-05-04"
    val weekEnd: String,                  // "2026-05-10"
    val aiSummary: String,
    val medicalVisitIds: List<String>,
    val growthSnapshot: GrowthSnapshot?,
    val vaccineDue: List<String>,
    val searchKeywords: List<String>,     // AI 關・叙・御ｾ・FTS 邏｢蠑・
    val driveFileId: String?,
    val syncedAt: Long
)

data class GrowthSnapshot(
    val weight: Double?,
    val height: Double?,
    val headCirc: Double?
)
