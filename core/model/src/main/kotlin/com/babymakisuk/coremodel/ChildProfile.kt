package com.babymakisuk.coremodel

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ChildProfile(
    val id: Long = 0,
    val name: String,
    val gender: Gender,
    val birthday: LocalDate,
    val bloodType: BloodType? = null,
    val allergies: String? = null,
    val note: String = ""
) {
    /**
     * 計算當前月齡。
     */
    val ageMonths: Int
        get() = ChronoUnit.MONTHS.between(birthday, LocalDate.now()).toInt()
}

enum class Gender { MALE, FEMALE, OTHER }
enum class BloodType { A, B, AB, O }
