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
        Room.databaseBuilder(ctx, AppDatabase::class.java, "babymakisuk.db").build()

    @Provides fun provideChildDao(db: AppDatabase) = db.childDao()
    @Provides fun provideGrowthDao(db: AppDatabase) = db.growthDao()
    @Provides fun provideMedicalDao(db: AppDatabase) = db.medicalDao()
    @Provides fun provideVaccineDao(db: AppDatabase) = db.vaccineDao()
    @Provides fun provideDailyLogDao(db: AppDatabase) = db.dailyLogDao()
}
