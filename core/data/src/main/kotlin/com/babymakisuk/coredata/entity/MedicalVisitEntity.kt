package com.babymakisuk.coredata.entity

import androidx.room.*
import com.babymakisuk.coremodel.MedicalVisit
import java.time.LocalDate

@Entity(
    tableName = "medical_visit",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")]
)
data class MedicalVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val date: LocalDate,
    val hospital: String,
    val department: String = "",
    val diagnosis: String = "",
    val notes: String = "",
    val attachments: String = "", // 騾苓辧蛻・囈蟄嶺ｸｲ
    val diagnosisSummary: String = "",
    val prescriptions: String = "",
    val careInstructions: String = "",
    // Phase E-0 譁ｰ蠅樊ｬ・ｽ・
    val imageStoragePath: String? = null,
    val aiPending: Boolean = false
)

fun MedicalVisitEntity.toDomain() = MedicalVisit(
    id, childId, date, hospital, department, diagnosis, notes,
    if (attachments.isBlank()) emptyList() else attachments.split(","),
    diagnosisSummary, prescriptions, careInstructions,
    imageStoragePath, aiPending
)

fun MedicalVisit.toEntity() = MedicalVisitEntity(
    id, childId, date, hospital, department, diagnosis, notes,
    attachments.joinToString(","), diagnosisSummary, prescriptions, careInstructions,
    imageStoragePath, aiPending
)
