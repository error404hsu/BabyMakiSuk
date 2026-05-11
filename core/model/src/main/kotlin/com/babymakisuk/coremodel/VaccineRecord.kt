package com.babymakisuk.coremodel

import java.time.LocalDate

data class VaccineRecord(
    val id: Long = 0,
    val childId: Long,
    val vaccineName: String,
    val dose: Int = 1,
    val date: LocalDate,
    val note: String = ""
)
