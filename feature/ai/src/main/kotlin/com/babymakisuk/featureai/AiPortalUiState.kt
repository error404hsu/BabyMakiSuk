package com.babymakisuk.featureai

import com.babymakisuk.coreai.AiPreset
import com.babymakisuk.coreai.GeminiModel
import java.util.UUID

/**
 * AI 角色枚舉（UI 層）
 */
enum class Role { USER, AI }

/**
 * 單則聊天訊息。
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * AiPortalScreen 的 UI 狀態。
 *
 * 角色定義統一使用 core/ai 的 [AiPreset]，feature 層不再重複定義 Persona。
 * 模型選擇使用強型別 [GeminiModel]，與 AiDispatcher Fallback Chain 完全對齊。
 */
data class AiPortalUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isAwaitingInput: Boolean = true,
    /** 目前選擇的角色，對應 core/ai 的 AiPreset */
    val selectedPreset: AiPreset = AiPreset.default,
    /** UI 顯示用的排序後角色清單，presetHint 對應的角色置頂 */
    val sortedPresets: List<AiPreset> = AiPreset.entries.toList(),
    /** 目前選擇的模型，預設使用 GeminiModel 標記的 isDefault 項目 */
    val selectedModel: GeminiModel = GeminiModel.default,
    val errorMessage: String? = null
)
