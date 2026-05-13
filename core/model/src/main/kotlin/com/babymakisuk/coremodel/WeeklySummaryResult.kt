package com.babymakisuk.coremodel

import kotlinx.serialization.Serializable

@Serializable
data class WeeklySummaryResult(
    val weekSummary: String,           // 整週總結，150字以內，溫馨語氣
    val highlights: List<String>,      // 本週亮點，3條以內
    val parentTips: List<String>,      // 給家長的具體建議，2條
    val searchKeywords: List<String>   // 3-5個關鍵字，供 FTS 搜尋
)
