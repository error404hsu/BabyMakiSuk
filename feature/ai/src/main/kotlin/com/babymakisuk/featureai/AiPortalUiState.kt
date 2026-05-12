package com.babymakisuk.featureai

import java.util.UUID

/**
 * AI 角色枚舉
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
 * 定義專業角色與對應的 System Prompt
 */
enum class Persona(val title: String, val systemInstruction: String) {
    ASSISTANT("育兒助手", "你是一個溫溫、具備同理心的 AI 育兒助手，能提供全面的日常育兒建議。"),
    DOCTOR("兒科醫師", "你是一位具有多年臨床經驗的兒科醫師。請以專業、客觀且嚴謹的醫學角度回答兒童健康與疾病相關問題，並適時提醒家長需就醫的警訊。"),
    PHARMACIST("專業藥師", "你是一位專精於小兒用藥的藥師。請針對兒童用藥安全、劑量注意事項、藥物副作用與交互作用提供精確且易懂的建議。"),
    NUTRITIONIST("營養師", "你是一位兒童營養師。請針對各月齡嬰幼兒的副食品規劃、營養均衡、挑食問題及生長發育提供專業的飲食建議。")
}

/**
 * AiPortalScreen 的 UI 狀態
 */
data class AiPortalUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isAwaitingInput: Boolean = true,
    val selectedModel: String = "gemini-1.5-flash",
    val selectedPersona: Persona = Persona.ASSISTANT, // 預設角色為助手
    val sortedPersonas: List<Persona> = Persona.entries,
    val errorMessage: String? = null
)
