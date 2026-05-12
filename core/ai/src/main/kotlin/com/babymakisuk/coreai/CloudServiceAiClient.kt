package com.babymakisuk.coreai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/** 幼兒健康記錄分析的系統 Prompt */
private const val MEDICAL_AGENT_PROMPT = """
你是一位專業的幼兒健康記錄分析助理。
請根據使用者輸入的就診記錄，整理並以 JSON 格式輸出以下欄位：
- diagnosis_summary: 診斷摘要（繁體中文）
- prescriptions: 用藥清單（若無則為空字串）
- care_instructions: 居家照護注意事項
請務必只回傳 JSON，不要附加任何說明文字。
"""

/**
 * Gemini 雲端推論實作。
 * API Key 由編譯時的 [AiConfig] 提供，使用者無法在 App 內變更。
 * [aiCloudEnabled] 供外部注入，來源為 DataStore 的開關狀態。
 */
class CloudServiceAiClient @Inject constructor(
    private val aiConfig: AiConfig,
    private val aiCloudEnabled: @JvmSuppressWildcards Flow<Boolean>
) : ServiceAiClient {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = aiConfig.apiKey,
            systemInstruction = content { text(MEDICAL_AGENT_PROMPT) }
        )
    }

    /**
     * 實作 ServiceAiClient 介面。
     * - 若 Key 渪有結（沒有在 local.properties 設定），拋出 ServiceAiException。
     * - 若使用者已關閉雲端 AI，拋出 ServiceAiException。
     */
    override suspend fun complete(prompt: String): String {
        if (!aiConfig.hasValidKey) {
            throw ServiceAiException("雲端 AI 尚未配置，請聯絡管理員設定 API Key")
        }
        val enabled = aiCloudEnabled.firstOrNull() ?: false
        if (!enabled) {
            throw ServiceAiException("雲端 AI 已由使用者關閉")
        }
        return try {
            val response = model.generateContent(prompt)
            response.text ?: "{}"
        } catch (e: Exception) {
            throw ServiceAiException("Gemini API 呼叫失敗：${e.message}", e)
        }
    }
}
