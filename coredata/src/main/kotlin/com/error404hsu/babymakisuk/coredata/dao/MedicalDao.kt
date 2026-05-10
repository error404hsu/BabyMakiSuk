package com.error404hsu.babymakisuk.coredata.dao

import androidx.room.*
import com.error404hsu.babymakisuk.coredata.entity.MedicalVisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDao {
    @Query("SELECT * FROM medical_visit WHERE childId = :childId ORDER BY date DESC")
    fun observeByChild(childId: Long): Flow<List<MedicalVisitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MedicalVisitEntity): Long

    @Delete
    suspend fun delete(entity: MedicalVisitEntity)
}
