package com.babymakisuk.coredata.entity

import androidx.room.*
import com.babymakisuk.coremodel.MedicalVisit
import java.time.LocalDate

@Entity(
    tableName = "medical_visit",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")],
)
data class MedicalVisitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "childId") val childId: Long,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "hospital") val hospital: String,
    @ColumnInfo(name = "department") val department: String = "",
    @ColumnInfo(name = "diagnosis") val diagnosis: String = "",
    @ColumnInfo(name = "notes") val notes: String = "",
    @ColumnInfo(name = "attachments") val attachments: String = "",
    @ColumnInfo(name = "diagnosis_summary") val diagnosisSummary: String = "",
    @ColumnInfo(name = "prescriptions") val prescriptions: String = "",
    @ColumnInfo(name = "care_instructions") val careInstructions: String = "",
    @ColumnInfo(name = "is_urgent") val isUrgent: Boolean = false,
    // Phase E-0 新增欄位
    @ColumnInfo(name = "imageStoragePath") val imageStoragePath: String? = null,
    @ColumnInfo(name = "aiPending") val aiPending: Boolean = false
)

fun MedicalVisitEntity.toDomain() = MedicalVisit(
    id, childId, date, hospital, department, diagnosis, notes,
    if (attachments.isBlank()) emptyList() else attachments.split(","),
    diagnosisSummary, prescriptions, careInstructions,
    isUrgent, imageStoragePath, aiPending
)

fun MedicalVisit.toEntity() = MedicalVisitEntity(
    id, childId, date, hospital, department, diagnosis, notes,
    attachments.joinToString(","), diagnosisSummary, prescriptions, careInstructions,
    isUrgent, imageStoragePath, aiPending
)
