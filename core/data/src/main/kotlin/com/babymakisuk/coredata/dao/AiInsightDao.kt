package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babymakisuk.coredata.entity.AiInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AiInsightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<AiInsightEntity>)

    @Query("SELECT * FROM ai_insights")
    suspend fun getAllOnce(): List<AiInsightEntity>

    @Query("SELECT * FROM ai_insights ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<AiInsightEntity>>

    @Query("DELETE FROM ai_insights")
    suspend fun deleteAll()

    @Query("SELECT * FROM ai_insights WHERE childId = :childId ORDER BY createdAt DESC")
    fun getByChildId(childId: String): Flow<List<AiInsightEntity>>

    @Query("DELETE FROM ai_insights WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM ai_insights WHERE createdAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
