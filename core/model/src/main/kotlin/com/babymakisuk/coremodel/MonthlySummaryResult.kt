package com.babymakisuk.coremodel

import kotlinx.serialization.Serializable

@Serializable
data class MonthlySummaryResult(
    val monthSummary: String,
    val highlights: List<String>,
    val parentTips: List<String>,
    val searchKeywords: List<String>
)
