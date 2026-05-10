package com.error404hsu.babymakisuk.coredata.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.error404hsu.babymakisuk.coredata.entity.*
import com.error404hsu.babymakisuk.coredata.dao.*

@Database(
    entities = [
        ChildProfileEntity::class,
        GrowthRecordEntity::class,
        MedicalVisitEntity::class,
        VaccineRecordEntity::class,
        DailyLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun growthDao(): GrowthDao
    abstract fun medicalDao(): MedicalDao
    abstract fun vaccineDao(): VaccineDao
    abstract fun dailyLogDao(): DailyLogDao
}
