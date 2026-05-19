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
            .addMigrations(AppDatabase.MIGRATION_8_9)
            .addMigrations(AppDatabase.MIGRATION_9_10)
            .addMigrations(AppDatabase.MIGRATION_10_11)
            .addMigrations(AppDatabase.MIGRATION_11_12)
            .addMigrations(AppDatabase.MIGRATION_12_13)
            .addMigrations(AppDatabase.MIGRATION_13_14)
            .addMigrations(AppDatabase.MIGRATION_14_15)
            .build()

    @Provides fun provideChildDao(db: AppDatabase) = db.childDao()
    @Provides fun provideGrowthDao(db: AppDatabase) = db.growthDao()
    @Provides fun provideMedicalDao(db: AppDatabase) = db.medicalDao()
    @Provides fun provideVaccineDao(db: AppDatabase) = db.vaccineDao()
    @Provides fun provideDailyLogDao(db: AppDatabase) = db.dailyLogDao()
    @Provides fun provideMonthlyReportDao(db: AppDatabase) = db.monthlyReportDao()
    @Provides fun provideSystemReminderDao(db: AppDatabase) = db.systemReminderDao()
    @Provides fun provideAiInsightDao(db: AppDatabase) = db.aiInsightDao()
    @Provides fun provideMemoDao(db: AppDatabase) = db.memoDao()
    @Provides fun provideToiletDao(db: AppDatabase) = db.toiletDao()
    @Provides fun provideVaccineReminderDao(db: AppDatabase) = db.vaccineReminderDao()
    @Provides fun provideChatMessageDao(db: AppDatabase) = db.chatMessageDao()
    @Provides fun provideFeverDao(db: AppDatabase) = db.feverDao()
}
