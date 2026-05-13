package com.babymakisuk.coredata.dao

import androidx.room.*
import com.babymakisuk.coredata.entity.ChildProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM child_profile ORDER BY name ASC")
    fun observeAll(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profile ORDER BY name ASC")
    suspend fun getAllOnce(): List<ChildProfileEntity>

    @Query("SELECT * FROM child_profile WHERE id = :id")
    suspend fun getById(id: Long): ChildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChildProfileEntity): Long

    @Update
    suspend fun update(entity: ChildProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ChildProfileEntity>)

    @Delete
    suspend fun delete(entity: ChildProfileEntity)

    @Query("DELETE FROM child_profile")
    suspend fun deleteAll()
}
