package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babymakisuk.coredata.entity.ToiletRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToiletDao {
    @Query("SELECT * FROM toilet_records WHERE childId = :childId ORDER BY timestamp DESC")
    fun getByChild(childId: Long): Flow<List<ToiletRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ToiletRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<ToiletRecordEntity>)

    @Query("SELECT * FROM toilet_records")
    suspend fun getAllOnce(): List<ToiletRecordEntity>

    @Query("DELETE FROM toilet_records")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(record: ToiletRecordEntity)

    @Query("DELETE FROM toilet_records WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)

    @Query("""
        DELETE FROM toilet_records
        WHERE timestamp < :cutoffMillis
        AND strftime('%Y-%m', timestamp / 1000, 'unixepoch') IN (
            SELECT DISTINCT substr(month_start, 1, 7) FROM monthly_reports
        )
    """)
    suspend fun deleteOlderThanWithReportGuard(cutoffMillis: Long)

    @Query("DELETE FROM toilet_records WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = :yearMonth")
    suspend fun deleteByYearMonth(yearMonth: String)

    @Query("SELECT COUNT(*) FROM toilet_records")
    suspend fun count(): Int
}
