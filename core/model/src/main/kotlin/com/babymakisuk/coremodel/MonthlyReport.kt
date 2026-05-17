package com.babymakisuk.coremodel

data class MonthlyReport(
    val id: String,
    val childId: Long, // Changed from String to Long (0L = global/merged)
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

data class GrowthSnapshot(
    val weight: Double?,
    val height: Double?,
    val headCirc: Double?
)
