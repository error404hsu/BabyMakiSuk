package com.babymakisuk.coreai

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AiDispatcher：Sprint 1 核心層。
 *
 * 職責：
 * 1. 依 [AiTask] 查找 Fallback Chain（模型優先順序）。
 * 2. 呼叫 [RateLimiter] 檢查配額，超限拋 [RateLimitException]。
 * 3. 逐一嘗試 Chain 中的模型，成功立即回傳；全部失敗拋 [AiDispatchException]。
 *
 * 刻意直接 new [GenerativeModel]，
 * 讓各任務可獨立選用不同模型 ID，保持最大彈性。
 */
@Singleton
class AiDispatcher @Inject constructor(
    private val aiConfig: AiConfig,
    private val rateLimiter: RateLimiter
) {

    companion object {
        private const val TAG = "AiDispatcher"

        /**
         * 任務路由表：每個 [AiTask] 對應按優先順序排列的 [GeminiModel] Fallback Chain。
         */
        val FALLBACK_CHAINS: Map<AiTask, List<GeminiModel>> = mapOf(
            AiTask.MEDICAL_CONSULTATION to listOf(
                GeminiModel.GEMMA_4_31B,
                GeminiModel.GEMINI_31_FLASH_LITE,
                GeminiModel.GEMMA_4_26B
            ),
            AiTask.MEDICAL_OCR to listOf(
                GeminiModel.GEMINI_3_FLASH,
                GeminiModel.GEMINI_31_FLASH_LITE,
                GeminiModel.GEMMA_4_31B
            ),
            AiTask.VOICE_INPUT to listOf(
                GeminiModel.GEMINI_31_FLASH_LITE,
                GeminiModel.GEMMA_4_31B
            ),
            AiTask.MONTHLY_REPORT to listOf(
                GeminiModel.GEMINI_3_FLASH,
                GeminiModel.GEMMA_4_31B,
                GeminiModel.GEMMA_4_26B
            ),
            AiTask.QUICK_CHAT to listOf(
                GeminiModel.GEMINI_31_FLASH_LITE,
                GeminiModel.GEMMA_4_31B,
                GeminiModel.GEMMA_4_26B
            ),
            AiTask.CUSTOM_PRESET to listOf(
                GeminiModel.GEMINI_31_FLASH_LITE,
                GeminiModel.GEMINI_25_FLASH,
                GeminiModel.GEMINI_3_FLASH
            )
        )
    }

    /**
     * 執行 AI 任務（純文字 prompt）。
     *
     * @param task   任務類型，決定 Fallback Chain 與 Rate Limit 配額
     * @param prompt 使用者 / 系統組合後的完整 prompt
     * @return       第一個成功模型回傳的文字結果
     * @throws RateLimitException  當前任務超出每分鐘配額
     * @throws AiDispatchException Fallback Chain 所有模型均失敗
     */
    suspend fun execute(task: AiTask, prompt: String): String {
        checkRateLimit(task)
        return tryChain(task, prompt, systemPrompt = null)
    }

    /**
     * 執行帶有 System Prompt 的 AI 任務，主要供 [AiTask.CUSTOM_PRESET] 使用。
     *
     * @param task         任務類型
     * @param systemPrompt 注入至 GenerativeModel 的 system instruction
     * @param userPrompt   使用者輸入的 prompt
     * @return             第一個成功模型回傳的文字結果
     * @throws RateLimitException  當前任務超出每分鐘配額
     * @throws AiDispatchException Fallback Chain 所有模型均失敗
     */
    suspend fun executeWithSystemPrompt(
        task: AiTask,
        systemPrompt: String,
        userPrompt: String,
        modelOverride: GeminiModel? = null
    ): String {
        checkRateLimit(task)
        return tryChain(task, userPrompt, systemPrompt = systemPrompt, modelOverride = modelOverride)
    }

    /**
     * 執行帶有圖片的 AI 任務（多模態）。
     *
     * @param task         任務類型
     * @param systemPrompt 注入至 GenerativeModel 的 system instruction
     * @param userPrompt   使用者輸入的文字 prompt（如症狀描述）
     * @param image        Bitmap 圖片資料
     * @return             第一個成功模型回傳的文字結果
     */
    suspend fun executeWithImage(
        task: AiTask,
        systemPrompt: String,
        userPrompt: String,
        image: Bitmap,
        modelOverride: GeminiModel? = null
    ): String {
        checkRateLimit(task)
        return tryChain(task, userPrompt, systemPrompt = systemPrompt, image = image, modelOverride = modelOverride)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** 檢查 Rate Limit，超限直接拋例外。 */
    private fun checkRateLimit(task: AiTask) {
        if (!rateLimiter.checkAndRecord(task)) {
            val wait = rateLimiter.secondsUntilAvailable(task)
            throw RateLimitException(task, wait)
        }
    }

    /**
     * 依序嘗試 [task] 的 Fallback Chain，回傳第一個成功的結果。
     * 全部失敗拋 [AiDispatchException]。
     */
    private suspend fun tryChain(
        task: AiTask,
        prompt: String,
        systemPrompt: String?,
        image: Bitmap? = null,
        modelOverride: GeminiModel? = null
    ): String {
        val chain = if (modelOverride != null)
            listOf(modelOverride)
        else
            FALLBACK_CHAINS[task]
                ?: throw AiDispatchException(task, "No fallback chain defined for task $task")

        var lastError: Throwable? = null

        for (model in chain) {
            try {
                val generativeModel = buildModel(model, systemPrompt)
                val response = if (image != null) {
                    generativeModel.generateContent(content {
                        image(image)
                        text(prompt)
                    })
                } else {
                    generativeModel.generateContent(prompt)
                }
                val text = response.text
                    ?: throw IllegalStateException("Empty response from ${model.modelId}")
                Log.d(TAG, "[$task] Success with model=${model.modelId}")
                return text
            } catch (e: Exception) {
                Log.w(TAG, "[$task] Model ${model.modelId} failed: ${e.message}")
                lastError = e
            }
        }

        throw AiDispatchException(
            task    = task,
            message = lastError?.message ?: "Unknown error",
            cause   = lastError
        )
    }

    /**
     * 根據是否提供 [systemPrompt] 建立對應的 [GenerativeModel]。
     */
    private fun buildModel(model: GeminiModel, systemPrompt: String?): GenerativeModel =
        if (systemPrompt != null) {
            GenerativeModel(
                modelName         = model.modelId,
                apiKey            = aiConfig.apiKey,
                systemInstruction = content { text(systemPrompt) }
            )
        } else {
            GenerativeModel(
                modelName = model.modelId,
                apiKey    = aiConfig.apiKey
            )
        }
}
