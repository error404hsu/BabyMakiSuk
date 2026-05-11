package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.MedicalVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDao {
    @Query("SELECT * FROM medical_visit WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<MedicalVisitEntity>>

    @Query("SELECT * FROM medical_visit ORDER BY date DESC")
    suspend fun getAllOnce(): List<MedicalVisitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MedicalVisitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MedicalVisitEntity>)

    @Delete
    suspend fun delete(entity: MedicalVisitEntity)

    @Query("DELETE FROM medical_visit")
    suspend fun deleteAll()
}
