package com.babymakisuk.coreai

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AiDispatcher：AI 呼叫的唯一入口。
 *
 * ## 架構原則
 * 1. 所有 AI 網路呼叫強制跑在 [Dispatchers.IO]，不阻塞 Main Thread。
 * 2. 透過 [RateLimiter] 強制限流，不可繞過。
 * 3. 依 [AiTask] 的 Fallback Chain 逐一嘗試，回傳 [Result] 封裝結果或 [AiError]。
 * 4. [CancellationException] 不被吞掉，讓 coroutine 正常取消。
 *
 * ## 給呼叫端（ViewModel）的使用模式
 * ```kotlin
 * val result = aiDispatcher.executeWithSystemPrompt(AiTask.QUICK_CHAT, systemPrompt, userPrompt)
 * result.fold(
 *     onSuccess = { text -> _uiState.update { it.copy(aiResult = text, isLoading = false) } },
 *     onFailure = { err -> handleAiError(err as? AiError) }
 * )
 * ```
 *
 * ⚠️ AI 結果僅供參考，非醫療診斷依據。
 * UI 層必須搭配 [AiSystemConstraints.REFERENCE_DISCLAIMER] 顯示免責聲明。
 *
 * TODO (Step 2)：將 execute() 廢棄，強制所有呼叫端改用 executeWithSystemPrompt()，
 *               確保所有 prompt 都經過 AiPromptBuilder 組裝。完成後刪除 execute()。
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
         * 新增任務時，必須在此表加入對應 chain，否則 dispatcher 會回傳 [AiError.AllModelsFailed]。
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

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * 執行純文字 AI 任務。
     *
     * ⚠️ Deprecated path：prompt 應由 [AiPromptBuilder] 組裝後透過
     * [executeWithSystemPrompt] 傳入。此函式保留供現有呼叫端過渡使用。
     * TODO (Step 2)：各 feature ViewModel 遷移後刪除此函式。
     */
    suspend fun execute(task: AiTask, prompt: String): Result<String> =
        dispatch(task, userPrompt = prompt, systemPrompt = null)

    /**
     * 執行帶有 System Prompt 的 AI 任務。
     *
     * System prompt 必須由 [AiPromptBuilder] 組裝，不得由呼叫端手動拼接。
     *
     * @param task           任務類型，決定 Fallback Chain 與限流配額
     * @param systemPrompt   由 [AiPromptBuilder] 產生的 system instruction
     * @param userPrompt     使用者輸入的 prompt
     * @param modelOverride  強制指定模型（跳過 Fallback Chain，僅供測試/Debug 使用）
     */
    suspend fun executeWithSystemPrompt(
        task: AiTask,
        systemPrompt: String,
        userPrompt: String,
        modelOverride: GeminiModel? = null
    ): Result<String> = dispatch(task, userPrompt, systemPrompt, modelOverride = modelOverride)

    /**
     * 執行多模態（文字 + 圖片）AI 任務。
     *
     * @param task         任務類型（通常為 [AiTask.MEDICAL_OCR]）
     * @param systemPrompt 由 [AiPromptBuilder] 產生的 system instruction
     * @param userPrompt   使用者輸入的文字描述
     * @param image        Bitmap 圖片，呼叫端需確保已壓縮至合理大小（建議 ≤ 1280px）
     */
    suspend fun executeWithImage(
        task: AiTask,
        systemPrompt: String,
        userPrompt: String,
        image: Bitmap,
        modelOverride: GeminiModel? = null
    ): Result<String> = dispatch(task, userPrompt, systemPrompt, image, modelOverride)

    // -------------------------------------------------------------------------
    // Private core
    // -------------------------------------------------------------------------

    /**
     * 統一調度入口：驗證 Config → 檢查限流 → 逐一嘗試 Fallback Chain。
     * 所有 IO 操作強制跑在 [Dispatchers.IO]。
     */
    private suspend fun dispatch(
        task: AiTask,
        userPrompt: String,
        systemPrompt: String?,
        image: Bitmap? = null,
        modelOverride: GeminiModel? = null
    ): Result<String> {
        // 1. 入口驗證：API Key
        if (!aiConfig.hasValidKey) {
            Log.e(TAG, "AiConfig.apiKey is blank — check BuildConfig.GEMINI_API_KEY")
            return Result.failure(AiError.InvalidConfig)
        }

        // 2. 限流檢查（suspend，coroutine-safe）
        if (!rateLimiter.checkAndRecord(task)) {
            val wait = rateLimiter.secondsUntilAvailable(task)
            Log.w(TAG, "[$task] Rate limited, retry in ${wait}s")
            return Result.failure(AiError.RateLimited(task, wait))
        }

        // 3. 在 IO 執行緒上執行 Fallback Chain
        return withContext(Dispatchers.IO) {
            tryChain(task, userPrompt, systemPrompt, image, modelOverride)
        }
    }

    /**
     * 逐一嘗試 Fallback Chain，回傳第一個成功的 [Result.success]。
     * 全部失敗回傳 [Result.failure] 包裝的 [AiError.AllModelsFailed]。
     *
     * [CancellationException] 直接重新拋出，不被當作模型失敗處理。
     */
    private suspend fun tryChain(
        task: AiTask,
        userPrompt: String,
        systemPrompt: String?,
        image: Bitmap?,
        modelOverride: GeminiModel?
    ): Result<String> {
        val chain = if (modelOverride != null) listOf(modelOverride)
        else FALLBACK_CHAINS[task]
            ?: return Result.failure(
                AiError.AllModelsFailed(task, IllegalStateException("No fallback chain for $task"))
            )

        var lastError: Throwable? = null

        for (model in chain) {
            try {
                val generativeModel = buildModel(model, systemPrompt)
                val response = if (image != null) {
                    generativeModel.generateContent(content {
                        image(image)
                        text(userPrompt)
                    })
                } else {
                    generativeModel.generateContent(userPrompt)
                }
                val text = response.text
                    ?: throw IllegalStateException("Empty response from ${model.modelId}")

                Log.d(TAG, "[$task] ✅ Success with ${model.modelId}")
                return Result.success(text)

            } catch (e: CancellationException) {
                // 不吞掉取消信號，讓 coroutine 正常結束
                Log.d(TAG, "[$task] Cancelled during ${model.modelId}")
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "[$task] ❌ ${model.modelId} failed: ${e.message}")
                lastError = e
            }
        }

        return Result.failure(AiError.AllModelsFailed(task, lastError))
    }

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
