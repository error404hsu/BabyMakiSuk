package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.GrowthRecord
import java.time.LocalDate

@Entity(
    tableName = "growth_record",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")]
)
data class GrowthRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val date: LocalDate,
    val heightCm: Float,
    val weightKg: Float,
    val headCircumferenceCm: Float? = null,
    val note: String = "",
    val aiSuggestion: String = ""
)

fun GrowthRecordEntity.toDomain() = GrowthRecord(id, childId, date, heightCm, weightKg, headCircumferenceCm, note, aiSuggestion)
fun GrowthRecord.toEntity() = GrowthRecordEntity(id, childId, date, heightCm, weightKg, headCircumferenceCm, note, aiSuggestion)
