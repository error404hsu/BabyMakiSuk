package com.babymakisuk.featureai

import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.GeminiModel
import java.util.UUID

/** AI 對話角色（User / AI 回覆）*/
enum class Role { USER, AI }

/** 單則聊天訊息。 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * AiPortalScreen 的 UI 狀態。
 *
 * ## 模型選擇策略
 * - [isModelOverridden] = false（預設）：使用 [selectedPreset].preferredModel 作為優先模型。
 * - [isModelOverridden] = true：使用者手動選擇，[selectedModel] 強制覆蓋 preferredModel。
 *
 * 切換角色時會自動清除 override 並帶入新角色的 preferredModel，
 * 使用者可隨時透過下拉選單再次強制指定。
 */
data class AiPortalUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isAwaitingInput: Boolean = true,

    /** 目前選擇的角色，對應 core/ai 的 AiPreset。 */
    val selectedPreset: AiPreset = AiPreset.default,

    /** UI 顯示用的排序後角色清單，presetHint 對應的角色置頂。 */
    val sortedPresets: List<AiPreset> = AiPreset.entries.toList(),

    /**
     * 目前選擇的模型。
     * - isModelOverridden = false 時，此值反映 selectedPreset.preferredModel（角色建議）。
     * - isModelOverridden = true 時，此值為使用者手動指定，優先級最高。
     */
    val selectedModel: GeminiModel = AiPreset.default.preferredModel,

    /**
     * 是否為使用者手動強制覆蓋模型。
     * false = 跟隨 AiPreset.preferredModel 建議。
     * true  = 使用者強制，切換角色前不會自動重置。
     */
    val isModelOverridden: Boolean = false,

    val errorMessage: String? = null
) {
    /**
     * 實際傳入 AiDispatcher 的有效模型。
     * override 時使用 selectedModel，否則使用角色建議的 preferredModel。
     */
    val effectiveModel: GeminiModel
        get() = if (isModelOverridden) selectedModel else selectedPreset.preferredModel
}
