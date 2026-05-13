package com.babymakisuk.coredata.di

import android.content.Context
import androidx.room.Room
import com.babymakisuk.coredata.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "babymakisuk.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .addMigrations(AppDatabase.MIGRATION_4_5)
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .addMigrations(AppDatabase.MIGRATION_6_7)
            .addMigrations(AppDatabase.MIGRATION_7_8)
            .build()

    @Provides fun provideChildDao(db: AppDatabase) = db.childDao()
    @Provides fun provideGrowthDao(db: AppDatabase) = db.growthDao()
    @Provides fun provideMedicalDao(db: AppDatabase) = db.medicalDao()
    @Provides fun provideVaccineDao(db: AppDatabase) = db.vaccineDao()
    @Provides fun provideDailyLogDao(db: AppDatabase) = db.dailyLogDao()
    @Provides fun provideWeeklyReportDao(db: AppDatabase) = db.weeklyReportDao()
    @Provides fun provideAiInsightDao(db: AppDatabase) = db.aiInsightDao()
    @Provides fun provideMemoDao(db: AppDatabase) = db.memoDao()
    @Provides fun provideToiletDao(db: AppDatabase) = db.toiletDao()
    @Provides fun provideVaccineReminderDao(db: AppDatabase) = db.vaccineReminderDao()
}
