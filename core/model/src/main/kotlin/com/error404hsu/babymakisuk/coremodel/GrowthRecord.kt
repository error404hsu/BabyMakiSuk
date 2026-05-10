package com.error404hsu.babymakisuk.coremodel

import java.time.LocalDate

data class GrowthRecord(
    val id: Long = 0,
    val childId: Long,
    val date: LocalDate,
    val heightCm: Float,
    val weightKg: Float,
    val headCircumferenceCm: Float? = null,
    val note: String = ""
)
