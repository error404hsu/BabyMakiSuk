package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.VaccineRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccine_record WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<VaccineRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VaccineRecordEntity): Long

    @Delete
    suspend fun delete(entity: VaccineRecordEntity)
}
