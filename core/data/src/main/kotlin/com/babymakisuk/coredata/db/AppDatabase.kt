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
        WeeklyReportFts::class,
        AiInsightEntity::class,
        MemoEntity::class
    ],
    version = 5,
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
    abstract fun aiInsightDao(): AiInsightDao
    abstract fun memoDao(): MemoDao

    companion object {
        /**
         * Migration 1 -> 2
         * 1. medical_visit 新增 imageStoragePath, aiPending
         * 2. 建立 weekly_reports 主表
         * 3. 建立 weekly_reports_fts FTS4 虛擬表
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- MedicalVisit 新增欄位 ---
                db.execSQL(
                    "ALTER TABLE medical_visit ADD COLUMN imageStoragePath TEXT"
                )
                db.execSQL(
                    "ALTER TABLE medical_visit ADD COLUMN aiPending INTEGER NOT NULL DEFAULT 0"
                )

                // --- WeeklyReports 主表 ---
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

                // --- WeeklyReports FTS4 虛擬表 ---
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS weekly_reports_fts
                    USING fts4(
                        content=`weekly_reports`,
                        ai_summary,
                        search_keywords
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration 2 -> 3
         * 1. 新增 child_profile 的 allergies 欄位
         * 2. 重建 weekly_reports 表以包含 FTS4 必需的 rowid 欄位
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 處理 child_profile 的 allergies 欄位 (上一個修復的)
                db.execSQL("ALTER TABLE child_profile ADD COLUMN allergies TEXT")

                // 2. 建立包含 rowid 的新 weekly_reports 表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS weekly_reports_new (
                        id                  TEXT NOT NULL PRIMARY KEY,
                        child_id            TEXT NOT NULL,
                        week_start          TEXT NOT NULL,
                        week_end            TEXT NOT NULL,
                        ai_summary          TEXT NOT NULL,
                        medical_visit_ids   TEXT NOT NULL,
                        snapshot_weight     REAL,
                        snapshot_height     REAL,
                        snapshot_head_circ  REAL,
                        vaccine_due         TEXT NOT NULL,
                        search_keywords     TEXT NOT NULL,
                        drive_file_id       TEXT,
                        synced_at           INTEGER NOT NULL,
                        `rowid`             INTEGER NOT NULL
                    )
                """.trimIndent())

                // 3. 將原本的資料倒進新表
                db.execSQL("""
                    INSERT INTO weekly_reports_new (
                        id, child_id, week_start, week_end, ai_summary, medical_visit_ids, 
                        snapshot_weight, snapshot_height, snapshot_head_circ, 
                        vaccine_due, search_keywords, drive_file_id, synced_at
                    )
                    SELECT id, child_id, week_start, week_end, ai_summary, medical_visit_ids, 
                           snapshot_weight, snapshot_height, snapshot_head_circ, 
                           vaccine_due, search_keywords, drive_file_id, synced_at 
                    FROM weekly_reports
                """.trimIndent())

                // 4. 刪除舊表
                db.execSQL("DROP TABLE weekly_reports")

                // 5. 將新表改名為原名
                db.execSQL("ALTER TABLE weekly_reports_new RENAME TO weekly_reports")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_insights (
                        id TEXT NOT NULL PRIMARY KEY,
                        childId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        sourceDate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS memos (
                        id TEXT NOT NULL PRIMARY KEY,
                        childId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 建立符合新 schema 的臨時表 (包含 is_urgent, 並將 camelCase 轉為 snake_case)
                // 註：Room 預期 defaultValue='undefined'，故 CREATE TABLE 不寫 DEFAULT
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS medical_visit_new (
                        id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        childId             INTEGER NOT NULL,
                        date                TEXT NOT NULL,
                        hospital            TEXT NOT NULL,
                        department          TEXT NOT NULL,
                        diagnosis           TEXT NOT NULL,
                        notes               TEXT NOT NULL,
                        attachments         TEXT NOT NULL,
                        diagnosis_summary   TEXT NOT NULL,
                        prescriptions       TEXT NOT NULL,
                        care_instructions   TEXT NOT NULL,
                        is_urgent           INTEGER NOT NULL,
                        imageStoragePath    TEXT,
                        aiPending           INTEGER NOT NULL,
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // 2. 轉移資料 (注意：舊表欄位名是 diagnosisSummary, careInstructions)
                db.execSQL("""
                    INSERT INTO medical_visit_new (
                        id, childId, date, hospital, department, diagnosis, notes, attachments,
                        diagnosis_summary, prescriptions, care_instructions, is_urgent,
                        imageStoragePath, aiPending
                    )
                    SELECT 
                        id, childId, date, hospital, department, diagnosis, notes, attachments,
                        diagnosisSummary, prescriptions, careInstructions, 0,
                        imageStoragePath, aiPending
                    FROM medical_visit
                """.trimIndent())

                // 3. 刪除舊表
                db.execSQL("DROP TABLE medical_visit")

                // 4. 重新命名新表
                db.execSQL("ALTER TABLE medical_visit_new RENAME TO medical_visit")

                // 5. 重新建立索引 (Room 會驗證索引名稱與欄位)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medical_visit_childId ON medical_visit(childId)")
            }
        }
    }
}