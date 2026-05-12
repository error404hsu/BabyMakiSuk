package com.babymakisuk.coreai

/**
 * AiPromptBuilder：組合動態 System Prompt，將個案資訊注入角色 prompt。
 */
object AiPromptBuilder {

    /**
     * 依 [preset] 與幼兒個案資訊組合完整 system prompt。
     *
     * - 若 [preset] 為 CUSTOM（systemPrompt 為空白），回傳 "" 讓呼叫端走 execute()。
     * - 否則將基礎角色 prompt 與個案資訊合併。
     */
    fun buildSystemPrompt(
        preset: AiPreset,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): String {
        if (preset.systemPrompt.isBlank()) return ""
        return buildString {
            append(preset.systemPrompt)
            append("\n\n【當前個案資訊】\n")
            append("當前月齡：${ageMonths} 個月\n")
            append("性別：${gender}\n")
            append("過敏史：${allergies ?: "無"}")
        }
    }
}
