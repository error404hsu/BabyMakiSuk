package com.babymakisuk.coreai.di

import com.babymakisuk.coreai.LocalServiceAiClient
import com.babymakisuk.coreai.ServiceAiClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {
    @Binds @Singleton
    abstract fun bindAiClient(impl: LocalServiceAiClient): ServiceAiClient
}
