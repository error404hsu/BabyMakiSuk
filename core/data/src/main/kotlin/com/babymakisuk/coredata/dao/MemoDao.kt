package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.babymakisuk.coredata.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MemoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MemoEntity>)

    @Query("SELECT * FROM memos")
    suspend fun getAllOnce(): List<MemoEntity>

    @Query("DELETE FROM memos")
    suspend fun deleteAll()

    @Update
    suspend fun update(entity: MemoEntity)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM memos ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE childId = :childId ORDER BY date DESC, createdAt DESC")
    fun observeByChildId(childId: Long): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE childId = :childId ORDER BY date DESC, createdAt DESC")
    suspend fun getByChildId(childId: Long): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE childId = :childId AND date = :date ORDER BY createdAt DESC")
    fun observeByChildIdAndDate(childId: Long, date: Long): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE childId = :childId AND date = :date ORDER BY createdAt DESC")
    suspend fun getByChildIdAndDate(childId: Long, date: Long): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE date = :date ORDER BY createdAt DESC")
    suspend fun getByDate(date: Long): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE childId = :childId AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun observeByChildIdAndDateRange(childId: Long, startDate: Long, endDate: Long): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE childId = :childId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestByChildId(childId: Long): MemoEntity?
}
