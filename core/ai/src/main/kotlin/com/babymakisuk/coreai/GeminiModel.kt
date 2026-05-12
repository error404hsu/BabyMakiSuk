package com.babymakisuk.coreai

/**
 * AI 引擎類型：雲端 API 或本機 NPU 推論。
 */
enum class AiEngineType {
    CLOUD_API,
    LOCAL_NPU
}

/**
 * 強類型化的 Gemini / Gemma 可選模型清單。
 * [modelId]      為傳入 GenerativeModel 的正式 ID。
 * [displayName]  為在 UI 顯示的友善名稱。
 * [badge]        為副標籤（速度 / 品質提示）。
 * [isDefault]    標記預設選擇。
 * [engineType]   推論引擎（CLOUD_API / LOCAL_NPU）。
 */
enum class GeminiModel(
    val modelId: String,
    val displayName: String,
    val badge: String,
    val isDefault: Boolean = false,
    val engineType: AiEngineType = AiEngineType.CLOUD_API
) {
    GEMINI_3_FLASH(
        modelId     = "gemini-3-flash",
        displayName = "Gemini 3 Flash",
        badge       = "最新"
    ),
    GEMINI_31_FLASH_LITE(
        modelId     = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash Lite",
        badge       = "省 Quota",
        isDefault   = true
    ),
    GEMINI_25_FLASH(
        modelId     = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        badge       = "穩定"
    ),
    GEMMA_4_31B(
        modelId     = "gemma-4-31b-it",
        displayName = "Gemma 4 31B",
        badge       = "高品質"
    ),
    GEMMA_4_26B(
        modelId     = "gemma-4-26b-a4b-it",   // 修正：原錯誤值已更正
        displayName = "Gemma 4 26B",
        badge       = "均衡"
    );

    companion object {
        val default: GeminiModel get() = entries.first { it.isDefault }
    }
}
