package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babymakisuk.coredata.entity.ToiletRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ToiletDao {
    @Query("SELECT * FROM toilet_records WHERE childId = :childId ORDER BY timestamp DESC")
    fun getByChild(childId: Long): Flow<List<ToiletRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ToiletRecordEntity)

    @Delete
    suspend fun delete(record: ToiletRecordEntity)
}
