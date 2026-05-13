package com.babymakisuk.coremodel

import java.time.LocalDate

data class MedicalVisit(
    val id: Long = 0,
    val childId: Long,
    val date: LocalDate,
    val hospital: String,
    val department: String = "",
    val diagnosis: String = "",
    val notes: String = "",
    val attachments: List<String> = emptyList(),
    // AI 分析欄位
    val diagnosisSummary: String = "",
    val prescriptions: String = "",
    val careInstructions: String = "",
    val isUrgent: Boolean = false,
    // Phase E-0：Firebase Storage 圖片路徑、AI 排程旗標
    val imageStoragePath: String? = null,
    val aiPending: Boolean = false
)
