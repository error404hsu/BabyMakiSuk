package com.babymakisuk.di

import com.babymakisuk.BuildConfig
import com.babymakisuk.coreai.AiConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
}
