package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_log WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_log WHERE childId = :childId AND date >= :since ORDER BY date ASC")
    suspend fun getByChildSince(childId: Long, since: LocalDate): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyLogEntity): Long

    @Delete
    suspend fun delete(entity: DailyLogEntity)
}
