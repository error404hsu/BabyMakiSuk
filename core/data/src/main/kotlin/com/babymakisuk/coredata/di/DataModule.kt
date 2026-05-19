package com.babymakisuk.coredata.di

import com.babymakisuk.coredata.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindChildRepository(impl: DefaultChildRepository): ChildRepository

    @Binds
    @Singleton
    abstract fun bindGrowthRepository(impl: DefaultGrowthRepository): GrowthRepository

    @Binds
    @Singleton
    abstract fun bindMemoRepository(impl: DefaultMemoRepository): MemoRepository

    @Binds
    @Singleton
    abstract fun bindMonthlyReportRepository(impl: DefaultMonthlyReportRepository): MonthlyReportRepository

    @Binds
    @Singleton
    abstract fun bindSystemReminderRepository(impl: DefaultSystemReminderRepository): SystemReminderRepository

    @Binds
    @Singleton
    abstract fun bindToiletRepository(impl: DefaultToiletRepository): ToiletRepository

    @Binds
    @Singleton
    abstract fun bindVaccineReminderRepository(impl: DefaultVaccineReminderRepository): VaccineReminderRepository

    @Binds
    @Singleton
    abstract fun bindMedicalAiRepository(impl: DefaultMedicalAiRepository): MedicalAiRepository

    @Binds
    @Singleton
    abstract fun bindMedicalRepository(impl: DefaultMedicalRepository): MedicalRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DefaultSettingsRepository): SettingsRepository
}
