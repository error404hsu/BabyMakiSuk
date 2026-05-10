package com.error404hsu.babymakisuk.coredata.dao

import androidx.room.*
import com.error404hsu.babymakisuk.coredata.entity.ChildProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM child_profile ORDER BY name ASC")
    fun observeAll(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profile WHERE id = :id")
    suspend fun getById(id: Long): ChildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChildProfileEntity): Long

    @Delete
    suspend fun delete(entity: ChildProfileEntity)
}
