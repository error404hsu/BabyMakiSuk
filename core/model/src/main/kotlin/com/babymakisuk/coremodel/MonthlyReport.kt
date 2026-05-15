package com.babymakisuk.coremodel

data class MonthlyReport(
    val id: String,
    val childId: String,
    val monthStart: String,
    val monthEnd: String,
    val aiSummary: String,
    val growthSnapshot: GrowthSnapshot?,
    val medicalCount: Int,
    val systemReminderCount: Int,
    val searchKeywords: List<String>,
    val driveFileId: String?,
    val syncedAt: Long
)
