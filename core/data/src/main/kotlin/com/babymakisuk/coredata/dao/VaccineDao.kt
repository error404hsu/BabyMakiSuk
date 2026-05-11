package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.VaccineRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccine_record WHERE childId = :childId ORDER BY date ASC")
    fun observeByChild(childId: Long): Flow<List<VaccineRecordEntity>>

    @Query("SELECT * FROM vaccine_record ORDER BY date ASC")
    suspend fun getAllOnce(): List<VaccineRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VaccineRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VaccineRecordEntity>)

    @Delete
    suspend fun delete(entity: VaccineRecordEntity)

    @Query("DELETE FROM vaccine_record")
    suspend fun deleteAll()
}
