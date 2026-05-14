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
 * [Idle]     ─ 初始 / 已重置
 * [Analyzing] ─ 分析進行中（顯示 loading）
 * [Success]  ─ 分析完成，帶信心分數；UI 自動填入三欄位
 * [Error]    ─ 分析失敗，顯示錯誤訊息
 *
 * ⚠️ TODO: MedicalAiRepository.analyzePrescriptionImage() 尚未實作。
 *   目前 ViewModel 呼叫時會直接進入 Error 狀態。
 *   實作完成後需移除此 TODO。
 */
sealed interface AiAnalysisState {
    data object Idle : AiAnalysisState
    data object Analyzing : AiAnalysisState
    data class Success(
        val diagnosisSummary: String,
        val prescriptions: String,
        val careInstructions: String,
        /** AI 回傳的可信度分數 0-100；若 API 未提供則預設 85 */
        val confidence: Int = 85
    ) : AiAnalysisState
    data class Error(val message: String) : AiAnalysisState
}
