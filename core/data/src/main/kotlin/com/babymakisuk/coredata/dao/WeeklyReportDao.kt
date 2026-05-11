package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babymakisuk.coredata.entity.WeeklyReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: WeeklyReportEntity)

    /**
     * 蜿門ｾ玲欠螳・child 譟仙ｹｴ蠎ｦ逧・園譛蛾ｱ蝣ｱ・御ｾ・week_start 髯榊・謗貞・縲・
     * year 譬ｼ蠑擾ｼ・2026"
     * id 譬ｼ蠑擾ｼ・{childId}_{year}-W{week}"
     */
    @Query("""
        SELECT * FROM weekly_reports
        WHERE child_id = :childId
          AND id LIKE :childId || '_' || :year || '-%'
        ORDER BY week_start DESC
    """)
    fun getByYear(childId: String, year: String): Flow<List<WeeklyReportEntity>>

    /**
     * FTS MATCH 蜈ｨ譁・頗蟆具ｼ・i_summary + search_keywords・峨・
     * keyword 謾ｯ謠ｴ FTS4 隱樊ｳ包ｼ御ｾ句ｦ・"逋ｼ辯・"縲・
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
