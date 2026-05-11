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

    @Query("DELETE FROM daily_log")
    suspend fun deleteAll()
}
