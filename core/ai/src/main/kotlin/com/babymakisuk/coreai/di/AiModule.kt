package com.babymakisuk.coreai.di

import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.CloudServiceAiClient
import com.babymakisuk.coreai.RateLimiter
import com.babymakisuk.coreai.ServiceAiClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI 模組：core/ai 層所有 Singleton 的綁定與提供。
 *
 * - [ServiceAiClient] → [CloudServiceAiClient]（Gemini 雲端推論）
 * - [RateLimiter]：in-memory 滑動視窗限流，@Inject constructor 自動提供
 * - [AiDispatcher]：Fallback Chain 核心層，@Inject constructor 自動提供
 *
 * RateLimiter 與 AiDispatcher 均使用 @Singleton + @Inject constructor，
 * Hilt 可直接建構，無需在此 Module 顯式宣告 @Provides。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    /**
     * 將 [ServiceAiClient] 介面綁定至 [CloudServiceAiClient] 實作。
     * 若需切回本地推論，將此處改為 LocalServiceAiClient 即可。
     */
    @Binds
    @Singleton
    abstract fun bindAiClient(impl: CloudServiceAiClient): ServiceAiClient
}
