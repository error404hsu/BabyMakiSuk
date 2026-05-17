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
    // Phase E-0：Firebase Storage 圖片路徑
    val imageStoragePath: String? = null,
    /**
     * AI Processing Flag.
     * True means an image has been uploaded and is currently awaiting AI analysis.
     * TODO: Consider moving this to a separate MedicalAiJob table once WorkManager
     * or Firebase Functions architecture is finalized.
     */
    val aiPending: Boolean = false
)
