package com.babymakisuk.coremodel

data class SystemReminder(
    val id: String,
    val childId: Long?, // Changed from Long to Long? (null = global/merged)
    val type: SystemReminderType,
    val title: String,
    val content: String,
    val createdAt: Long,
    val resolvedAt: Long?
)

enum class SystemReminderType {
    LONG_NO_BM,
    VACCINE_DUE_SOON,
    MEDICAL_FOLLOW_UP,
    MONTHLY_REPORT_PENDING
}
