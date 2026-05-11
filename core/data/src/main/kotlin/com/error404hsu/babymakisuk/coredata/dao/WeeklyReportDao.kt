package com.error404hsu.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.error404hsu.babymakisuk.coredata.entity.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: WeeklyReportEntity)

    /**
     * 取得指定 child 某年度的所有週報，依 week_start 降冪排列。
     * year 格式："2026"
     * id 格式："{childId}_{year}-W{week}"
     */
    @Query("""
        SELECT * FROM weekly_reports
        WHERE child_id = :childId
          AND id LIKE :childId || '_' || :year || '-%'
        ORDER BY week_start DESC
    """)
    fun getByYear(childId: String, year: String): Flow<List<WeeklyReportEntity>>

    /**
     * FTS MATCH 全文搜尋（ai_summary + search_keywords）。
     * keyword 支援 FTS4 語法，例如 "發燒*"。
     */
    @Query("""
        SELECT wr.* FROM weekly_reports wr
        INNER JOIN weekly_reports_fts fts ON fts.rowid = wr.rowid
        WHERE wr.child_id = :childId
          AND weekly_reports_fts MATCH :keyword
        ORDER BY wr.week_start DESC
    """)
    fun searchByKeyword(childId: String, keyword: String): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WeeklyReportEntity?
}
