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
    val isGenerating: Boolean = false,
    val isSummarizing: Boolean = false,
    val isAwaitingInput: Boolean = true,

    val selectedPreset: AiPreset = AiPreset.default,
    val sortedPresets: List<AiPreset> = AiPreset.entries.toList(),

    val selectedModel: GeminiModel = AiPreset.default.preferredModel,
    val isModelOverridden: Boolean = false,

    val selectedChildId: Long = -1L,
    val children: List<ChildProfile> = emptyList(),

    val errorMessage: String? = null
) {
    val effectiveModel: GeminiModel
        get() = if (isModelOverridden) selectedModel else selectedPreset.preferredModel
}
