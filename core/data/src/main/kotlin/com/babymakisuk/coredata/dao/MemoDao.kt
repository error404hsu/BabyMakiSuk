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
    suspend fun insert(entity: MemoEntity)

    @Update
    suspend fun update(entity: MemoEntity)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM memos WHERE childId = :childId ORDER BY updatedAt DESC")
    fun getByChildId(childId: String): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos WHERE childId = :childId AND tags LIKE '%' || :tag || '%' ORDER BY updatedAt DESC")
    fun getByTag(childId: String, tag: String): Flow<List<MemoEntity>>
}
