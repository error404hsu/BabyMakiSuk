package com.babymakisuk.coremodel

import kotlinx.serialization.Serializable

/**
 * 處方箋 AI 分析結果。
 */
@Serializable
data class PrescriptionAnalysisResult(
    val diagnosisSummary: String,
    val prescriptions: List<String>,
    val careInstructions: List<String>,
    /** AI 回傳的可信度分數 0-100；若 API 未提供則為 null */
    val confidence: Int? = null
)
