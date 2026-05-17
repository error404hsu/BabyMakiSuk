package com.babymakisuk.coredata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.babymakisuk.coredata.entity.FeverRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeverDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FeverRecordEntity): Long

    @Update
    suspend fun update(entity: FeverRecordEntity)

    @Query("DELETE FROM fever_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 觀察某孩子的所有發燒紀錄，依量測時間倒序。 */
    @Query("SELECT * FROM fever_records WHERE childId = :childId ORDER BY measuredAt DESC")
    fun observeByChildId(childId: Long): Flow<List<FeverRecordEntity>>

    /** 查詢某孩子特定時間區間內的紀錄（用於就醫時帶入摘要）。 */
    @Query("""
        SELECT * FROM fever_records
        WHERE childId = :childId
          AND measuredAt >= :fromMs
          AND measuredAt <= :toMs
        ORDER BY measuredAt ASC
    """)
    suspend fun getByChildIdAndRange(
        childId: Long,
        fromMs: Long,
        toMs: Long
    ): List<FeverRecordEntity>

    /** 取得該孩子最新一筆發燒紀錄。 */
    @Query("SELECT * FROM fever_records WHERE childId = :childId ORDER BY measuredAt DESC LIMIT 1")
    suspend fun getLatestByChildId(childId: Long): FeverRecordEntity?

    /** 回填就醫關聯 ID。 */
    @Query("UPDATE fever_records SET linkedVisitId = :visitId WHERE id IN (:ids)")
    suspend fun linkToVisit(visitId: Long, ids: List<Long>)
}
