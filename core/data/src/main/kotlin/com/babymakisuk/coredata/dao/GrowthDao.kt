package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.GrowthRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthDao {
    @Query("SELECT * FROM growth_record WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<GrowthRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GrowthRecordEntity): Long

    @Delete
    suspend fun delete(entity: GrowthRecordEntity)
}
