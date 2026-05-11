package com.error404hsu.babymakisuk.coremodel

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
    // AI 產出欄位
    val diagnosisSummary: String = "",
    val prescriptions: String = "",
    val careInstructions: String = "",
    // Phase E-0：Firebase Storage 路徑與 AI 處理旗標
    val imageStoragePath: String? = null,
    val aiPending: Boolean = false
)
