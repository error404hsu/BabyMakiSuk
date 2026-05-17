package com.babymakisuk.featureai

import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.GeminiModel
import com.babymakisuk.coremodel.ChildProfile
import java.util.UUID

enum class Role { USER, AI }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class AiPortalUiState(
    val messages: List<ChatMessage> = emptyList(),

    // AI 呼叫進行中（Result 回傳前）；成功或失敗後均設回 false
    val isAiLoading: Boolean = false,
    // AI 語意錯誤訊息（RateLimited / AllModelsFailed / 其他 AiError）
    val aiError: String? = null,

    // 打字動畫進行中
    val isGenerating: Boolean = false,
    val isSummarizing: Boolean = false,
    val isAwaitingInput: Boolean = true,

    val selectedPreset: AiPreset = AiPreset.default,
    val sortedPresets: List<AiPreset> = AiPreset.entries.toList(),

    val selectedModel: GeminiModel = AiPreset.default.preferredModel,
    val isModelOverridden: Boolean = false,

    val selectedChildId: Long = -1L,
    val children: List<ChildProfile> = emptyList(),

    // 通用 UI 訊息（非 AI 錯誤，例如儲存成功、開始新對話等）
    val errorMessage: String? = null
) {
    val effectiveModel: GeminiModel
        get() = if (isModelOverridden) selectedModel else selectedPreset.preferredModel
}
