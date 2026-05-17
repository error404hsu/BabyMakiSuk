package com.babymakisuk.coredata.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.babymakisuk.coremodel.VaccineReminder

@Entity(
    tableName = "vaccine_reminders",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")]
)
data class VaccineReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Changed from Int to Long
    val childId: Long,
    val name: String,
    val scheduledDate: Long,
    val isCompleted: Boolean = false,
    val note: String = ""
)

fun VaccineReminderEntity.toDomain() = VaccineReminder(id, childId, name, scheduledDate, isCompleted, note)
fun VaccineReminder.toEntity() = VaccineReminderEntity(id, childId, name, scheduledDate, isCompleted, note)
