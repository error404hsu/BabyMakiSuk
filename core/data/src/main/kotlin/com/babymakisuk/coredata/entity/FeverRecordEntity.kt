package com.babymakisuk.coredata.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.FeverRecord
import com.babymakisuk.coremodel.FeverSymptom

@Entity(
    tableName = "fever_records",
    foreignKeys = [ForeignKey(
        entity = ChildProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["childId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("childId"), Index("measuredAt")]
)
data class FeverRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "childId") val childId: Long,
    @ColumnInfo(name = "temperatureCelsius") val temperatureCelsius: Float,
    @ColumnInfo(name = "measuredAt") val measuredAt: Long,
    @ColumnInfo(name = "durationMinutes") val durationMinutes: Int? = null,
    // 症狀以逗號分隔字串儲存，例如 "COUGH,RUNNY_NOSE"
    @ColumnInfo(name = "symptoms") val symptoms: String = "",
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "medicineGiven") val medicineGiven: String = "",
    @ColumnInfo(name = "linkedVisitId") val linkedVisitId: Long? = null
)

fun FeverRecordEntity.toDomain() = FeverRecord(
    id = id,
    childId = childId,
    temperatureCelsius = temperatureCelsius,
    measuredAt = measuredAt,
    durationMinutes = durationMinutes,
    symptoms = if (symptoms.isBlank()) emptyList()
               else symptoms.split(",").mapNotNull {
                   runCatching { FeverSymptom.valueOf(it.trim()) }.getOrNull()
               },
    note = note,
    medicineGiven = medicineGiven,
    linkedVisitId = linkedVisitId
)

fun FeverRecord.toEntity() = FeverRecordEntity(
    id = id,
    childId = childId,
    temperatureCelsius = temperatureCelsius,
    measuredAt = measuredAt,
    durationMinutes = durationMinutes,
    symptoms = symptoms.joinToString(",") { it.name },
    note = note,
    medicineGiven = medicineGiven,
    linkedVisitId = linkedVisitId
)
