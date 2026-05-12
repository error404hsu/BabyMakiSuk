package com.babymakisuk.coreai

/**
 * AiPromptBuilder：組合動態 System Prompt，將個案資訊與全域限制注入角色 prompt。
 *
 * ## 組合順序
 * 1. [AiPreset.systemPrompt]（角色人格與專業定義）
 * 2. 個案資訊 / Context Block（月齡、性別、過敏史等）
 * 3. [AiSystemConstraints.GLOBAL_CONSTRAINTS]（語言、風格、安全、格式限制）
 *
 * 此順序確保角色定義在前、限制在後，LLM 傾向以最末端的 instruction 為優先。
 */
object AiPromptBuilder {

    /**
     * 依 [preset] 與幼兒個案資訊組合完整 system prompt。
     * 末尾自動附加 [AiSystemConstraints.GLOBAL_CONSTRAINTS]。
     *
     * - 若 [preset.systemPrompt] 為空白（CUSTOM 模式），仍附加全域限制。
     */
    fun buildSystemPrompt(
        preset: AiPreset,
        ageMonths: Int,
        gender: String,
        allergies: String?
    ): String {
        return buildString {
            if (preset.systemPrompt.isNotBlank()) {
                append(preset.systemPrompt)
                append("\n\n")
            }
            append("【當前個案資訊】\n")
            append("當前月齡：${ageMonths} 個月\n")
            append("性別：${gender}\n")
            append("過敏史：${allergies ?: "無"}")
            append("\n\n")
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
    }

    /**
     * 依 [preset] 與已組裝好的 [contextBlock] 合併 system prompt。
     * Sprint 3 overload：供 AiContextInjector 注入完整 RAG context 使用。
     * 末尾自動附加 [AiSystemConstraints.GLOBAL_CONSTRAINTS]。
     */
    fun buildSystemPromptWithContext(
        preset: AiPreset,
        contextBlock: String
    ): String {
        return buildString {
            if (preset.systemPrompt.isNotBlank()) {
                append(preset.systemPrompt)
                append("\n\n")
            }
            append(contextBlock)
            append("\n\n")
            append(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }
    }
}
