package com.babymakisuk.coremodel

data class ToiletRecord(
    val id: Int = 0,
    val childId: Long,
    val timestamp: Long = System.currentTimeMillis()
)
