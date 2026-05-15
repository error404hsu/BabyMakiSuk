package com.babymakisuk.coredata.repository

import android.util.Log
import com.babymakisuk.coredata.ai.AiContextInjector
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiSystemConstraints
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.DailyLogDao
import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.dao.SystemReminderDao
import com.babymakisuk.coredata.entity.AiInsightEntity
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.GrowthSnapshot
import com.babymakisuk.coremodel.MonthlyReport
import com.babymakisuk.coremodel.MonthlySummaryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonthlyReportRepository @Inject constructor(
    private val monthlyReportDao: MonthlyReportDao,
    private val medicalDao: MedicalDao,
    private val growthDao: GrowthDao,
    private val dailyLogDao: DailyLogDao,
    private val systemReminderDao: SystemReminderDao,
    private val childRepository: ChildRepository,
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector,
    private val aiInsightDao: AiInsightDao
) {

    companion object {
        private const val TAG = "MonthlyReportRepository"
        private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun observeByYear(childId: String, year: String): Flow<List<MonthlyReport>> =
        monthlyReportDao.getByYear(childId, year).map { list -> list.map { it.toDomain() } }

    fun searchByKeyword(childId: String, keyword: String): Flow<List<MonthlyReport>> =
        monthlyReportDao.searchByKeyword(childId, keyword).map { list -> list.map { it.toDomain() } }

    suspend fun generateMonthlyReport(childId: Long, yearMonth: YearMonth): MonthlyReport {
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        val monthStartStr = monthStart.format(dateFmt)
        val monthEndStr = monthEnd.format(dateFmt)
        val childIdStr = childId.toString()
        val yearValue = yearMonth.year
        val monthValue = yearMonth.monthValue
        val reportId = "${childId}_${yearValue}-M${String.format("%02d", monthValue)}"

        val child = childRepository.getById(childId)
        val childName = child?.name ?: "寶寶"
        val ageMonths = child?.ageMonths ?: 0
        val monthLabel = "${yearValue} 年 ${monthValue} 月（${monthStartStr.substring(5)}–${monthEndStr.substring(5)}）"

        val dailyLogs = dailyLogDao.getAllOnce()
            .filter { it.childId == childId && !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }
            .sortedBy { it.date }

        val dailyLogsBlock = if (dailyLogs.isEmpty()) {
            "本月尚無日誌紀錄"
        } else {
            dailyLogs.joinToString("\n") { log ->
                val dateStr = log.date.format(dateFmt)
                val moodEmoji = when (log.mood) {
                    "HAPPY" -> "😊"
                    "NORMAL" -> "😐"
                    "FUSSY" -> "😤"
                    "SICK" -> "🤒"
                    else -> ""
                }
                "📅 $dateStr 睡眠：${log.sleepInfo} 飲食：${log.mealsInfo} 便便：${log.poopCount}次 心情：$moodEmoji ${log.freeText}"
            }
        }

        val medicalVisits = medicalDao.getAllOnce()
            .filter { it.childId == childId && !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }

        val recentMedical = if (medicalVisits.isEmpty()) null else {
            medicalVisits.joinToString("\n") { visit ->
                "${visit.date.format(dateFmt)} ${visit.hospital} ${visit.department}：${visit.diagnosis}"
            }
        }

        val latestGrowth = growthDao.getAllOnce()
            .filter { it.childId == childId }
            .maxByOrNull { it.date }

        val monthStartEpoch = monthStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEndEpoch = monthEnd.atStartOfDay(java.time.ZoneId.systemDefault()).plusDays(1).toInstant().toEpochMilli()
        val systemReminders = systemReminderDao.getAllOnce()
            .filter { it.childId == childId && it.createdAt in monthStartEpoch until monthEndEpoch }
        val systemReminderBlock = if (systemReminders.isEmpty()) null else {
            systemReminders.joinToString("\n") { r ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(r.createdAt))
                "$dateStr ${r.title}：${r.content}"
            }
        }

        val (systemPrompt, userPrompt) = buildString {
            appendLine("你是一位幼兒成長月報 AI 撰稿員，專門為家長生成溫馨且具體的每月成長總結。")
            appendLine()
            appendLine("【輸出規則】")
            appendLine("- 只輸出一個合法的 JSON 物件，不得有任何前綴、後綴、說明文字")
            appendLine("- 禁止 Markdown 包裝")
            appendLine("- JSON schema：")
            appendLine(
                """
{
  "monthSummary": "string（整月總結，200字以內，溫馨語氣）",
  "highlights": ["string（本月亮點，3條以內）"],
  "parentTips": ["string（給家長的具體建議，2條）"],
  "searchKeywords": ["string（3-5個關鍵字，供 FTS 搜尋）"]
}
                """.trimIndent()
            )
            appendLine(AiSystemConstraints.GLOBAL_CONSTRAINTS)
        }.let { system ->
            val user = buildString {
                appendLine("請根據以下資料，為 ${childName}（${ageMonths}個月）生成 ${monthLabel} 的成長月報：")
                appendLine()
                appendLine("【本月日誌】")
                appendLine(dailyLogsBlock)
                if (!recentMedical.isNullOrBlank()) {
                    appendLine()
                    appendLine("【本月就診紀錄】")
                    append(recentMedical)
                }
                if (!systemReminderBlock.isNullOrBlank()) {
                    appendLine()
                    appendLine("【本月系統提醒】")
                    append(systemReminderBlock)
                }
            }
            Pair(system, user)
        }

        val (aiSummary, searchKeywords) = try {
            val raw = aiDispatcher.executeWithSystemPrompt(
                task = AiTask.MONTHLY_REPORT,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            )
            val result = try {
                json.decodeFromString<MonthlySummaryResult>(raw)
            } catch (_: Exception) {
                MonthlySummaryResult(
                    monthSummary = raw.take(200),
                    highlights = emptyList(),
                    parentTips = emptyList(),
                    searchKeywords = emptyList()
                )
            }
            val summary = buildString {
                appendLine(result.monthSummary)
                if (result.highlights.isNotEmpty()) {
                    appendLine()
                    appendLine("▌本月亮點")
                    result.highlights.forEach { appendLine("- $it") }
                }
                if (result.parentTips.isNotEmpty()) {
                    appendLine()
                    appendLine("▌給家長的建議")
                    result.parentTips.forEach { appendLine("- $it") }
                }
            }
            Pair(summary, result.searchKeywords)
        } catch (e: Exception) {
            Log.e(TAG, "generateMonthlyReport AI failed: ${e.message}")
            Pair("（本月月報 AI 生成失敗，請稍後重試）", emptyList<String>())
        }

        try {
            val insight = AiInsightEntity(
                id = "monthly_$reportId",
                childId = childIdStr,
                title = "月報摘要｜$monthLabel",
                content = aiSummary.take(200),
                sourceDate = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            aiInsightDao.insert(insight)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create AiInsight from monthly report: ${e.message}")
        }

        val keywords = if (searchKeywords.isNotEmpty()) searchKeywords else {
            medicalVisits.flatMap {
                listOf(it.department, it.diagnosis)
            }.filter { it.isNotBlank() }.distinct().take(5)
        }

        val growthSnapshot = latestGrowth?.let {
            GrowthSnapshot(
                weight = it.weightKg.toDouble(),
                height = it.heightCm.toDouble(),
                headCirc = it.headCircumferenceCm?.toDouble()
            )
        }

        val report = MonthlyReport(
            id = reportId,
            childId = childIdStr,
            monthStart = monthStartStr,
            monthEnd = monthEndStr,
            aiSummary = aiSummary,
            growthSnapshot = growthSnapshot,
            medicalCount = medicalVisits.size,
            systemReminderCount = systemReminders.size,
            searchKeywords = keywords,
            driveFileId = null,
            syncedAt = System.currentTimeMillis()
        )

        monthlyReportDao.upsert(report.toEntity())
        return report
    }
}
