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
    // AI 逕｢蜃ｺ谺・ｽ・
    val diagnosisSummary: String = "",
    val prescriptions: String = "",
    val careInstructions: String = "",
    // Phase E-0・哥irebase Storage 霍ｯ蠕題・ AI 陌慕炊譌玲ｨ・
    val imageStoragePath: String? = null,
    val aiPending: Boolean = false
)
