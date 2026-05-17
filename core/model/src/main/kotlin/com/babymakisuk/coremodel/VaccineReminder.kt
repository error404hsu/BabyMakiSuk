package com.babymakisuk.coremodel

data class VaccineReminder(
    val id: Long = 0, // Changed from Int to Long
    val childId: Long,
    val name: String,
    val scheduledDate: Long,
    val isCompleted: Boolean = false,
    val note: String = ""
)
