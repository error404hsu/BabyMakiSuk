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
        MonthlyReportEntity::class,
        MonthlyReportFts::class,
        SystemReminderEntity::class,
        AiInsightEntity::class,
        MemoEntity::class,
        ToiletRecordEntity::class,
        VaccineReminderEntity::class,
        ChatMessageEntity::class,
        FeverRecordEntity::class
    ],
    version = 17,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun childDao(): ChildDao
    abstract fun growthDao(): GrowthDao
    abstract fun medicalDao(): MedicalDao
    abstract fun vaccineDao(): VaccineDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun monthlyReportDao(): MonthlyReportDao
    abstract fun systemReminderDao(): SystemReminderDao
    abstract fun aiInsightDao(): AiInsightDao
    abstract fun memoDao(): MemoDao
    abstract fun toiletDao(): ToiletDao
    abstract fun vaccineReminderDao(): VaccineReminderDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun feverDao(): FeverDao

    companion object {
        /**
         * Migration 16 -> 17
         * Drop the manually-created FTS triggers from MIGRATION_15_16.
         *
         * Room's @Fts4(contentEntity = MonthlyReportEntity::class) annotation
         * already auto-generates these triggers (monthly_reports_ai, monthly_reports_ad,
         * monthly_reports_au). Having both the manual triggers and Room's auto-generated
         * triggers causes duplicate trigger execution on INSERT/UPDATE/DELETE, leading to
         * "SQL logic error" when upserting or deleting monthly reports.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TRIGGER IF EXISTS monthly_reports_ai")
                db.execSQL("DROP TRIGGER IF EXISTS monthly_reports_ad")
                db.execSQL("DROP TRIGGER IF EXISTS monthly_reports_au")
                // Rebuild FTS index to ensure consistency (Room will manage it from now on)
                db.execSQL("INSERT INTO monthly_reports_fts(monthly_reports_fts) VALUES('rebuild')")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Fix medical_visit table (imageStoragePath was marked as NOT NULL in schema 14,
                // but Entity uses a converter that can return null, and migration 4_5 didn't specify NOT NULL correctly for some paths)
                // Actually, schema 14 says imageStoragePath is NOT NULL.
                // Let's check the error: found identity hash 8c496bdfc87a1ee60305310ac151d954.
                // This hash usually means something in the current code (Entities) differs from schema 14.

                // One common discrepancy is 'imageStoragePath' being NOT NULL in schema but potentially NULL in code or vice-versa.
                // In MedicalVisitEntity, imageStoragePath is NOT NULL (val imageStoragePath: ImageStoragePath = ImageStoragePath.None).
                // However, the TypeConverter returns null for ImageStoragePath.None.
                // Room treats @TypeConverter return type nullability as the column nullability.
                // Since fromImageStoragePath returns String?, the column should be nullable.
                // But schema 14.json says it's NOT NULL.

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
                db.execSQL("""
                    INSERT INTO medical_visit_new (
                        id, childId, date, hospital, department, diagnosis, notes, attachments,
                        diagnosis_summary, prescriptions, care_instructions, is_urgent,
                        imageStoragePath, aiPending
                    )
                    SELECT
                        id, childId, date, hospital, department, diagnosis, notes, attachments,
                        diagnosis_summary, prescriptions, care_instructions, is_urgent,
                        imageStoragePath, aiPending
                    FROM medical_visit
                """.trimIndent())
                db.execSQL("DROP TABLE medical_visit")
                db.execSQL("ALTER TABLE medical_visit_new RENAME TO medical_visit")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medical_visit_childId ON medical_visit(childId)")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medical_visit ADD COLUMN imageStoragePath TEXT")
                db.execSQL("ALTER TABLE medical_visit ADD COLUMN aiPending INTEGER NOT NULL DEFAULT 0")
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE child_profile ADD COLUMN allergies TEXT")
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
                db.execSQL("DROP TABLE weekly_reports")
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
                db.execSQL("DROP TABLE medical_visit")
                db.execSQL("ALTER TABLE medical_visit_new RENAME TO medical_visit")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medical_visit_childId ON medical_visit(childId)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS toilet_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        childId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_toilet_records_childId ON toilet_records(childId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS vaccine_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        childId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        scheduledDate INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        note TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vaccine_reminders_childId ON vaccine_reminders(childId)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE child_profile ADD COLUMN photoUri TEXT")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS memos_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        childId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        reminderAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_childId ON memos_new(childId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_memos_date ON memos_new(date)")
                db.execSQL("DROP TABLE IF EXISTS memos")
                db.execSQL("ALTER TABLE memos_new RENAME TO memos")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        role TEXT NOT NULL,
                        text TEXT NOT NULL,
                        timestampMs INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE growth_record ADD COLUMN aiSuggestion TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS weekly_reports_fts")
                db.execSQL("DROP TABLE IF EXISTS weekly_reports")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monthly_reports (
                        id                  TEXT NOT NULL PRIMARY KEY,
                        `rowid`             INTEGER NOT NULL,
                        child_id            TEXT NOT NULL,
                        month_start         TEXT NOT NULL,
                        month_end           TEXT NOT NULL,
                        ai_summary          TEXT NOT NULL,
                        snapshot_weight     REAL,
                        snapshot_height     REAL,
                        snapshot_head_circ  REAL,
                        medical_count       INTEGER NOT NULL DEFAULT 0,
                        system_reminder_count INTEGER NOT NULL DEFAULT 0,
                        search_keywords     TEXT NOT NULL DEFAULT '',
                        drive_file_id       TEXT,
                        synced_at           INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS monthly_reports_fts
                    USING fts4(
                        content=`monthly_reports`,
                        ai_summary,
                        search_keywords
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS system_reminders (
                        id TEXT NOT NULL PRIMARY KEY,
                        childId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        resolvedAt INTEGER,
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_system_reminders_childId ON system_reminders(childId)")
                db.execSQL("ALTER TABLE child_profile ADD COLUMN defaultAiPrompt TEXT")
            }
        }

        /**
         * Migration 12 -> 13
         * 新增 fever_records 發燒紀錄表。
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS fever_records (
                        id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        childId             INTEGER NOT NULL,
                        temperatureCelsius  REAL NOT NULL,
                        measuredAt          INTEGER NOT NULL,
                        symptoms            TEXT NOT NULL DEFAULT '',
                        note                TEXT NOT NULL DEFAULT '',
                        isMedicineTaken     INTEGER NOT NULL DEFAULT 0,
                        linkedVisitId       INTEGER,
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fever_records_childId ON fever_records(childId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fever_records_measuredAt ON fever_records(measuredAt)")
            }
        }

        /**
         * Migration 13 -> 14
         * 1. MonthlyReport.child_id conversion: TEXT -> INTEGER.
         * 2. GrowthRecord.aiSuggestion conversion: NOT NULL -> NULLABLE.
         * Safe multi-step approach using temporary tables.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // --- MonthlyReport conversion (Remove explicit rowid) ---
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monthly_reports_new (
                        id                  TEXT NOT NULL PRIMARY KEY,
                        child_id            INTEGER NOT NULL,
                        month_start         TEXT NOT NULL,
                        month_end           TEXT NOT NULL,
                        ai_summary          TEXT NOT NULL,
                        snapshot_weight     REAL,
                        snapshot_height     REAL,
                        snapshot_head_circ  REAL,
                        medical_count       INTEGER NOT NULL DEFAULT 0,
                        system_reminder_count INTEGER NOT NULL DEFAULT 0,
                        search_keywords     TEXT NOT NULL DEFAULT '',
                        drive_file_id       TEXT,
                        synced_at           INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO monthly_reports_new (
                        id, child_id, month_start, month_end, ai_summary,
                        snapshot_weight, snapshot_height, snapshot_head_circ,
                        medical_count, system_reminder_count, search_keywords,
                        drive_file_id, synced_at
                    )
                    SELECT
                        id, CAST(child_id AS INTEGER), month_start, month_end, ai_summary,
                        snapshot_weight, snapshot_height, snapshot_head_circ,
                        medical_count, system_reminder_count, search_keywords,
                        drive_file_id, synced_at
                    FROM monthly_reports
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS monthly_reports_fts")
                db.execSQL("DROP TABLE monthly_reports")
                db.execSQL("ALTER TABLE monthly_reports_new RENAME TO monthly_reports")

                // Recreate FTS table
                db.execSQL("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS monthly_reports_fts
                    USING fts4(
                        content=`monthly_reports`,
                        ai_summary,
                        search_keywords
                    )
                """.trimIndent())

                // --- GrowthRecord conversion (making aiSuggestion nullable) ---
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS growth_record_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        childId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        heightCm REAL NOT NULL,
                        weightKg REAL NOT NULL,
                        headCircumferenceCm REAL,
                        note TEXT NOT NULL,
                        aiSuggestion TEXT,
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO growth_record_new (id, childId, date, heightCm, weightKg, headCircumferenceCm, note, aiSuggestion)
                    SELECT id, childId, date, heightCm, weightKg, headCircumferenceCm, note, aiSuggestion FROM growth_record
                """.trimIndent())
                db.execSQL("DROP TABLE growth_record")
                db.execSQL("ALTER TABLE growth_record_new RENAME TO growth_record")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_growth_record_childId ON growth_record(childId)")

                // --- FeverRecord fix (Fixing broken MIGRATION_12_13) ---
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS fever_records_new (
                        id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        childId             INTEGER NOT NULL,
                        temperatureCelsius  REAL NOT NULL,
                        measuredAt          INTEGER NOT NULL,
                        symptoms            TEXT NOT NULL DEFAULT '',
                        note                TEXT NOT NULL DEFAULT '',
                        isMedicineTaken     INTEGER NOT NULL DEFAULT 0,
                        linkedVisitId       INTEGER,
                        FOREIGN KEY(childId) REFERENCES child_profile(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // 嘗試搬移舊資料 (若舊資料表欄位名稱不符則略過該欄位)
                // 註：symptoms 在舊表是 'TEXT NOT NULL DEFAULT \'\''，在 Entity 也是 String
                db.execSQL("""
                    INSERT INTO fever_records_new (id, childId, temperatureCelsius, measuredAt, symptoms, note, linkedVisitId)
                    SELECT id, childId, temperatureCelsius, measuredAt, symptoms, note, linkedVisitId FROM fever_records
                """.trimIndent())

                db.execSQL("DROP TABLE fever_records")
                db.execSQL("ALTER TABLE fever_records_new RENAME TO fever_records")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fever_records_childId ON fever_records(childId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_fever_records_measuredAt ON fever_records(measuredAt)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS monthly_reports_ai AFTER INSERT ON monthly_reports BEGIN
                        INSERT INTO monthly_reports_fts(docid, ai_summary, search_keywords)
                        VALUES (new.rowid, new.ai_summary, new.search_keywords);
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS monthly_reports_ad AFTER DELETE ON monthly_reports BEGIN
                        INSERT INTO monthly_reports_fts(monthly_reports_fts, docid, ai_summary, search_keywords)
                        VALUES ('delete', old.rowid, old.ai_summary, old.search_keywords);
                    END
                """.trimIndent())
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS monthly_reports_au AFTER UPDATE ON monthly_reports BEGIN
                        INSERT INTO monthly_reports_fts(monthly_reports_fts, docid, ai_summary, search_keywords)
                        VALUES ('delete', old.rowid, old.ai_summary, old.search_keywords);
                        INSERT INTO monthly_reports_fts(docid, ai_summary, search_keywords)
                        VALUES (new.rowid, new.ai_summary, new.search_keywords);
                    END
                """.trimIndent())
                db.execSQL("INSERT INTO monthly_reports_fts(monthly_reports_fts) VALUES('rebuild')")
            }
        }
    }
}
