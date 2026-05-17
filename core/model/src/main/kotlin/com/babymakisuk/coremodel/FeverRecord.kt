package com.babymakisuk.coremodel

/**
 * 發燒症狀枚舉，支援多選。
 */
enum class FeverSymptom {
    RUNNY_NOSE,    // 流鼻水
    COUGH,         // 咳嗽
    VOMITING,      // 嘔吐
    DIARRHEA,      // 腹瀉
    RASH,          // 出疹
    POOR_APPETITE, // 食慾不振
    FATIGUE        // 精神不佳
}

fun FeverSymptom.displayName() = when (this) {
    FeverSymptom.RUNNY_NOSE    -> "流鼻水"
    FeverSymptom.COUGH         -> "咳嗽"
    FeverSymptom.VOMITING      -> "嘔吐"
    FeverSymptom.DIARRHEA      -> "腹瀉"
    FeverSymptom.RASH          -> "出疹"
    FeverSymptom.POOR_APPETITE -> "食慾不振"
    FeverSymptom.FATIGUE       -> "精神不佳"
}

/**
 * 發燒單筆紀錄 Domain Model。
 *
 * @param temperatureCelsius 攝氏體溫，例如 38.5
 * @param measuredAt         量測時間（epoch millis）
 * @param symptoms           多選症狀列表
 * @param note               備註
 * @param isMedicineTaken    是否已服用退燒藥
 * @param linkedVisitId      關聯就醫紀錄 ID（就醫後可回填，null 表示尚未就醫）
 */
data class FeverRecord(
    val id: Long = 0,
    val childId: Long,
    val temperatureCelsius: Float,
    val measuredAt: Long = System.currentTimeMillis(),
    val symptoms: List<FeverSymptom> = emptyList(),
    val note: String = "",
    val isMedicineTaken: Boolean = false,
    val linkedVisitId: Long? = null
)
