package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.ToiletRecord

@Entity(
    tableName = "toilet_records",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")]
)
data class ToiletRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Changed from Int to Long
    val childId: Long,
    val timestamp: Long
)

fun ToiletRecordEntity.toDomain() = ToiletRecord(id, childId, timestamp)
fun ToiletRecord.toEntity() = ToiletRecordEntity(id, childId, timestamp)
