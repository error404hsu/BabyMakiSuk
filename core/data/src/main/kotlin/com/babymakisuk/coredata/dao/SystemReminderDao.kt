package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babymakisuk.coredata.entity.SystemReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SystemReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SystemReminderEntity>)

    @Query("SELECT * FROM system_reminders WHERE childId = :childId ORDER BY createdAt DESC")
    fun getByChildId(childId: Long): Flow<List<SystemReminderEntity>>

    @Query("SELECT * FROM system_reminders WHERE childId = :childId OR childId = 0 ORDER BY createdAt DESC")
    fun getByChildIdIncludingGlobal(childId: Long): Flow<List<SystemReminderEntity>>

    @Query("SELECT * FROM system_reminders WHERE childId = :childId AND type = :type AND resolvedAt IS NULL ORDER BY createdAt DESC")
    fun getUnresolvedByType(childId: Long, type: String): Flow<List<SystemReminderEntity>>

    @Query("SELECT * FROM system_reminders ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<SystemReminderEntity>

    @Query("DELETE FROM system_reminders")
    suspend fun deleteAll()

    @Query("DELETE FROM system_reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE system_reminders SET resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun markResolved(id: String, resolvedAt: Long)

    @Query("UPDATE system_reminders SET resolvedAt = :resolvedAt WHERE childId = :childId AND type = :type AND resolvedAt IS NULL")
    suspend fun markAllResolvedByType(childId: Long, type: String, resolvedAt: Long)

    @Query("DELETE FROM system_reminders WHERE resolvedAt IS NOT NULL")
    suspend fun deleteTriggered()

    @Query("SELECT COUNT(*) FROM system_reminders")
    suspend fun count(): Int
}
