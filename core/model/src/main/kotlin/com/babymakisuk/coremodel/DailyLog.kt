package com.babymakisuk.coremodel

import java.time.LocalDate

data class DailyLog(
    val id: Long = 0,
    val childId: Long,
    val date: LocalDate,
    val sleepInfo: String = "",
    val mealsInfo: String = "",
    val poopCount: Int = 0,
    val mood: Mood = Mood.NORMAL,
    val freeText: String = ""
)

enum class Mood { HAPPY, NORMAL, FUSSY, SICK }
