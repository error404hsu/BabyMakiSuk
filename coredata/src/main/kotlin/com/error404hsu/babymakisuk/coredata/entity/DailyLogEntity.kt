package com.error404hsu.babymakisuk.coredata.entity

import androidx.room.*
import com.error404hsu.babymakisuk.coremodel.DailyLog
import com.error404hsu.babymakisuk.coremodel.Mood
import java.time.LocalDate

@Entity(
    tableName = "daily_log",
    foreignKeys = [ForeignKey(ChildProfileEntity::class, ["id"], ["childId"], ForeignKey.CASCADE)],
    indices = [Index("childId")]
)
data class DailyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val date: LocalDate,
    val sleepInfo: String = "",
    val mealsInfo: String = "",
    val poopCount: Int = 0,
    val mood: String = Mood.NORMAL.name,
    val freeText: String = ""
)

fun DailyLogEntity.toDomain() = DailyLog(id, childId, date, sleepInfo, mealsInfo, poopCount, Mood.valueOf(mood), freeText)
fun DailyLog.toEntity() = DailyLogEntity(id, childId, date, sleepInfo, mealsInfo, poopCount, mood.name, freeText)
