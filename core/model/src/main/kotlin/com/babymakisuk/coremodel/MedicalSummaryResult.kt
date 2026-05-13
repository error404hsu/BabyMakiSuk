package com.babymakisuk.coremodel

import kotlinx.serialization.Serializable

@Serializable
data class MedicalSummaryResult(
    val diagnosisSummary: String,
    val prescriptions: List<String>,
    val careInstructions: List<String>,
    val safetyFlag: String // "normal" | "attention" | "urgent"
)
