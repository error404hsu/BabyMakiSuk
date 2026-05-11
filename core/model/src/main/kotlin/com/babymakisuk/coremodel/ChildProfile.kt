package com.babymakisuk.coremodel

import java.time.LocalDate

data class ChildProfile(
    val id: Long = 0,
    val name: String,
    val gender: Gender,
    val birthday: LocalDate,
    val bloodType: BloodType? = null,
    val note: String = ""
)

enum class Gender { MALE, FEMALE, OTHER }
enum class BloodType { A, B, AB, O }
