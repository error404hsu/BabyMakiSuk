package com.babymakisuk.coreai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.babymakisuk.coredata.SettingsRepository
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
 * 動態取得 GenerativeModel 以確保 API Key 變更後立即生效，無需重啟 App。
 */
class CloudServiceAiClient @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ServiceAiClient {

    /**
     * 每次呼叫時動態建立 GenerativeModel。
     * API Key 若未設定則拋出 ServiceAiException。
     */
    private suspend fun getGenerativeModel(): GenerativeModel {
        val apiKey = settingsRepository.geminiApiKey.firstOrNull()
            ?: throw ServiceAiException("Gemini API Key 尚未設定，請至設定頁面輸入")

        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            systemInstruction = content { text(MEDICAL_AGENT_PROMPT) }
        )
    }

    /**
     * 實作 ServiceAiClient 介面：傳入 prompt，回傳 Gemini 的純文字回應。
     */
    override suspend fun complete(prompt: String): String {
        return try {
            val model = getGenerativeModel()
            val response = model.generateContent(prompt)
            response.text ?: "{}"
        } catch (e: ServiceAiException) {
            throw e  // Key 未設定的錯誤直接向上拋
        } catch (e: Exception) {
            throw ServiceAiException("Gemini API 呼叫失敗：${e.message}", e)
        }
    }

    /**
     * 供設定頁面使用的輕量 API Key 驗證。
     * 直接以傳入的 testKey 建立臨時 model，不會污染已儲存的 Key。
     *
     * @param testKey 待驗證的 API Key 字串
     * @return true = Key 有效；false = 驗證失敗
     */
    suspend fun testApiKeyValid(testKey: String): Boolean {
        return try {
            val testModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = testKey
            )
            testModel.generateContent("Ping")
            true
        } catch (e: Exception) {
            false
        }
    }
}
