package com.babymakisuk.coredata.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.babymakisuk.coredata.dao.*
import com.babymakisuk.coredata.entity.*

@Database(
    entities = [
        ChildProfileEntity::class,
        GrowthRecordEntity::class,
        MedicalVisitEntity::class,
        VaccineRecordEntity::class,
        DailyLogEntity::class,
        WeeklyReportEntity::class,
        WeeklyReportFts::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun growthDao(): GrowthDao
    abstract fun medicalDao(): MedicalDao
    abstract fun vaccineDao(): VaccineDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun weeklyReportDao(): WeeklyReportDao

    companion object {
        /**
         * Migration 1 竊・2
         * 1. medical_visit 譁ｰ蠅・imageStoragePath, aiPending
         * 2. 蟒ｺ遶・weekly_reports 荳ｻ陦ｨ
         * 3. 蟒ｺ遶・weekly_reports_fts FTS4 陌帶闘陦ｨ
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- MedicalVisit 譁ｰ谺・ｽ・---
                db.execSQL(
                    "ALTER TABLE medical_visit ADD COLUMN imageStoragePath TEXT"
                )
                db.execSQL(
                    "ALTER TABLE medical_visit ADD COLUMN aiPending INTEGER NOT NULL DEFAULT 0"
                )

                // --- WeeklyReports 荳ｻ陦ｨ ---
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_reports (
                        id                  TEXT NOT NULL PRIMARY KEY,
                        child_id            TEXT NOT NULL,
                        week_start          TEXT NOT NULL,
                        week_end            TEXT NOT NULL,
                        ai_summary          TEXT NOT NULL,
                        medical_visit_ids   TEXT NOT NULL DEFAULT '',
                        snapshot_weight     REAL,
                        snapshot_height     REAL,
                        snapshot_head_circ  REAL,
                        vaccine_due         TEXT NOT NULL DEFAULT '',
                        search_keywords     TEXT NOT NULL DEFAULT '',
                        drive_file_id       TEXT,
                        synced_at           INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // --- WeeklyReports FTS4 陌帶闘陦ｨ ---
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS weekly_reports_fts
                    USING fts4(
                        content=`weekly_reports`,
                        aiSummary,
                        searchKeywords
                    )
                """.trimIndent())
            }
        }
    }
}
