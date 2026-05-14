package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.babymakisuk.coredata.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages ORDER BY timestampMs ASC")
    suspend fun getAllOnce(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY timestampMs ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
