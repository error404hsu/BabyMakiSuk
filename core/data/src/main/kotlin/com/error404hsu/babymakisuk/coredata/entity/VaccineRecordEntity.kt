package com.error404hsu.babymakisuk.coredata.entity

import androidx.room.*
import com.error404hsu.babymakisuk.coremodel.VaccineRecord
import java.time.LocalDate

@Entity(
    tableName = "vaccine_record",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")]
)
data class VaccineRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val vaccineName: String,
    val dose: Int = 1,
    val date: LocalDate,
    val note: String = ""
)

fun VaccineRecordEntity.toDomain() = VaccineRecord(id, childId, vaccineName, dose, date, note)
fun VaccineRecord.toEntity() = VaccineRecordEntity(id, childId, vaccineName, dose, date, note)
