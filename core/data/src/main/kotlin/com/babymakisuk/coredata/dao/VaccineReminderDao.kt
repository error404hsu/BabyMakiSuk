package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.babymakisuk.coredata.entity.VaccineReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineReminderDao {
    @Query("SELECT * FROM vaccine_reminders WHERE childId = :childId ORDER BY scheduledDate ASC")
    fun observeByChild(childId: Long): Flow<List<VaccineReminderEntity>>

    @Query("SELECT * FROM vaccine_reminders WHERE childId = :childId AND isCompleted = 0 AND scheduledDate > :now ORDER BY scheduledDate ASC LIMIT 1")
    suspend fun getNextDue(childId: Long, now: Long): VaccineReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaccineReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VaccineReminderEntity>)

    @Update
    suspend fun update(entity: VaccineReminderEntity)

    @Delete
    suspend fun delete(entity: VaccineReminderEntity)
}
