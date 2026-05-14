package com.babymakisuk.featuremedical

import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.MedicalVisit

sealed interface MedicalUiState {
    data object Loading : MedicalUiState
    data class Success(
        val children: List<ChildProfile>,
        val selectedChildId: Long,
        val visits: List<MedicalVisit>
    ) : MedicalUiState
    data class Error(val message: String) : MedicalUiState
}

/**
 * 藥單圖片 AI 分析的狀態機。
 * [Idle]       — 初始 / 已重置
 * [Analyzing]  — 分析進行中（顯示 loading）
 * [Reviewing]  — 分析完成，顯示原始 OCR 文字供使用者確認
 * [Success]    — 使用者確認後，自動填入三欄位
 * [Error]      — 分析失敗，顯示錯誤訊息
 */
sealed interface AiAnalysisState {
    data object Idle : AiAnalysisState
    data object Analyzing : AiAnalysisState
    data class Reviewing(
        /** AI 回傳的原始文字或 JSON 內容 */
        val rawText: String,
        /** 解析後的診斷摘要 */
        val diagnosisSummary: String,
        /** 解析後的處方清單（以 ・ 分隔） */
        val prescriptions: String,
        /** 解析後的照護建議（以 ・ 分隔） */
        val careInstructions: String,
        /** 信心分數 0-100 */
        val confidence: Int = 85,
        /** 留存圖片的絕對路徑 */
        val imagePath: String
    ) : AiAnalysisState
    data class Success(
        val diagnosisSummary: String,
        val prescriptions: String,
        val careInstructions: String,
        /** AI 回傳的可信度分數 0-100；若 API 未提供則預設 85 */
        val confidence: Int = 85
    ) : AiAnalysisState
    data class Error(val message: String) : AiAnalysisState
}
