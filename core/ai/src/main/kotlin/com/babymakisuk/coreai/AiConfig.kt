package com.babymakisuk.coreai

/**
 * AI 設定容器。
 * API Key 在編譯時由 app 模組的 BuildConfig 提供，
 * 透過 Hilt 的 @Provides 注入此物件，避免 core:ai 直接依賴 BuildConfig。
 *
 * 使用方式（在 app/di/AppAiModule.kt 中）:
 *   @Provides fun provideAiConfig() = AiConfig(apiKey = BuildConfig.GEMINI_API_KEY)
 */
data class AiConfig(
    val apiKey: String,
) {
    /** 編譯時是否有注入有效 Key */
    val hasValidKey: Boolean get() = apiKey.isNotBlank()
}
