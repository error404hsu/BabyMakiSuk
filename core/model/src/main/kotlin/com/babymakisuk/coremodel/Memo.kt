package com.babymakisuk.coremodel

data class Memo(
    val id: Long = 0,
    val childId: Long,
    val title: String,
    val content: String,
    val date: Long,
    val reminderAt: Long? = null,
    val createdAt: Long
)
