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
    val diagnosisSummary: String = "",
    val prescriptions: String = "",
    val careInstructions: String = "",
    val isUrgent: Boolean = false,
    val imageStoragePath: ImageStoragePath = ImageStoragePath.None,
    val aiPending: Boolean = false
)
