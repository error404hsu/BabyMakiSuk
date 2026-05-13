package com.babymakisuk.coremodel

data class VaccineReminder(
    val id: Int = 0,
    val childId: Long,
    val name: String,
    val scheduledDate: Long,
    val isCompleted: Boolean = false,
    val note: String = ""
)
