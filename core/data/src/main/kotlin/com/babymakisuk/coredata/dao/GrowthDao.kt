package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.GrowthRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthDao {
    @Query("SELECT * FROM growth_record WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<GrowthRecordEntity>>

    @Query("SELECT * FROM growth_record ORDER BY date DESC")
    suspend fun getAllOnce(): List<GrowthRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GrowthRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<GrowthRecordEntity>)

    @Delete
    suspend fun delete(entity: GrowthRecordEntity)

    @Query("DELETE FROM growth_record")
    suspend fun deleteAll()

    @Query("SELECT * FROM growth_record WHERE id = :id")
    suspend fun getById(id: Long): GrowthRecordEntity?

    @Query("SELECT * FROM growth_record WHERE childId = :childId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestByChild(childId: Long): GrowthRecordEntity?

    @Query("UPDATE growth_record SET aiSuggestion = :suggestion WHERE id = :id")
    suspend fun updateAiSuggestion(id: Long, suggestion: String)
}
