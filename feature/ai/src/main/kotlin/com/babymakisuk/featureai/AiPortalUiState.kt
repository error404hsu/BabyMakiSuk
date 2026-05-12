package com.babymakisuk.featureai

/**
 * AiPortalScreen 的 UI 狀態機。
 */
sealed interface AiPortalUiState {
    data object Idle    : AiPortalUiState
    data object Loading : AiPortalUiState
    data class  Success(val response: String, val elapsedMs: Long) : AiPortalUiState
    data class  Error(val message: String)                         : AiPortalUiState
}

/**
 * 單則聊天訊息。
 * @param isUser true = 使用者訊息；false = AI 回應
 */
data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)
