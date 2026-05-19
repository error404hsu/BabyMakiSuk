package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_log WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_log ORDER BY date DESC")
    suspend fun getAllOnce(): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<DailyLogEntity>)

    @Delete
    suspend fun delete(entity: DailyLogEntity)

    @Query("DELETE FROM daily_log WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("""
        DELETE FROM daily_log
        WHERE date < :cutoffDate
        AND substr(date, 1, 7) IN (
            SELECT DISTINCT substr(month_start, 1, 7) FROM monthly_reports
        )
    """)
    suspend fun deleteOlderThanWithReportGuard(cutoffDate: String)

    @Query("DELETE FROM daily_log WHERE substr(date, 1, 7) = :yearMonth")
    suspend fun deleteByYearMonth(yearMonth: String)

    @Query("DELETE FROM daily_log")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM daily_log")
    suspend fun count(): Int
}
