package com.babymakisuk.coreai.di

import com.babymakisuk.coreai.CloudServiceAiClient
import com.babymakisuk.coreai.ServiceAiClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    /**
     * 將 ServiceAiClient 綁定至 CloudServiceAiClient（Gemini 雲端推論）。
     * 若需切回本地推論，將此處改為 LocalServiceAiClient 即可。
     */
    @Binds
    @Singleton
    abstract fun bindAiClient(impl: CloudServiceAiClient): ServiceAiClient
}
