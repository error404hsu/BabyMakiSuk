package com.babymakisuk.coremodel

data class ToiletRecord(
    val id: Long = 0, // Changed from Int to Long
    val childId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
