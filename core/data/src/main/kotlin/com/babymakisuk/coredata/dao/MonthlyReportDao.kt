package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babymakisuk.coredata.entity.MonthlyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: MonthlyReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reports: List<MonthlyReportEntity>)

    @Query("SELECT * FROM monthly_reports")
    suspend fun getAllOnce(): List<MonthlyReportEntity>

    @Query("DELETE FROM monthly_reports")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM monthly_reports
        WHERE child_id = :childId
          AND id LIKE :childId || '_' || :year || '-%'
        ORDER BY month_start DESC
    """)
    fun getByYear(childId: Long, year: String): Flow<List<MonthlyReportEntity>>

    @Query("""
        SELECT * FROM monthly_reports
        WHERE rowid IN (
            SELECT rowid FROM monthly_reports_fts WHERE monthly_reports_fts MATCH :keyword
        )
          AND child_id = :childId
        ORDER BY month_start DESC
    """)
    fun searchByKeyword(childId: Long, keyword: String): Flow<List<MonthlyReportEntity>>

    @Query("SELECT * FROM monthly_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MonthlyReportEntity?

    @Query("""
        SELECT * FROM monthly_reports
        WHERE child_id = :childId
        ORDER BY month_start DESC
        LIMIT :limit
    """)
    fun getRecentReports(childId: Long, limit: Int = 20): Flow<List<MonthlyReportEntity>>

    /**
     * 取全部孩子的月報（書庫月報頁使用）
     * 月報為合併式設計（merged_yyyy-Mmm），不應依 childId 篩選。
     */
    @Query("""
        SELECT * FROM monthly_reports
        ORDER BY month_start DESC
        LIMIT :limit
    """)
    fun getAllRecent(limit: Int = 20): Flow<List<MonthlyReportEntity>>

    @Delete
    suspend fun delete(report: MonthlyReportEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM monthly_reports WHERE substr(month_start, 1, 7) = :yearMonth)")
    suspend fun existsForMonth(yearMonth: String): Boolean

    @Query("INSERT INTO monthly_reports_fts(monthly_reports_fts) VALUES('rebuild')")
    suspend fun rebuildFts()

    @Query("SELECT COUNT(*) FROM monthly_reports")
    suspend fun count(): Int
}
