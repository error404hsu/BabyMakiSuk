package com.babymakisuk.di

import com.babymakisuk.BuildConfig
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coredata.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

/**
 * 在 app 模組提供 AiConfig，是唯一能存取 BuildConfig.GEMINI_API_KEY 的地方。
 * core:ai 不會依賴 app 層，保持模組層次經清潔。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppAiModule {

    /** 將 local.properties 中的 Key 包裝為 AiConfig */
    @Provides
    @Singleton
    fun provideAiConfig(): AiConfig = AiConfig(
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    /**
     * 提供 AI 雲端開關狀態的 Flow，來源為 DataStore。
     * CloudServiceAiClient 透過此 Flow 檢查使用者是否已關閉雲端推論。
     */
    @Provides
    @Singleton
    fun provideAiCloudEnabledFlow(
        settingsRepository: SettingsRepository
    ): @JvmSuppressWildcards Flow<Boolean> = settingsRepository.aiCloudEnabled
}
