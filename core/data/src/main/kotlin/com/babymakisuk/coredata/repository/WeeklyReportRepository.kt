package com.babymakisuk.coredata.repository

import android.util.Log
import com.babymakisuk.coredata.ai.AiContextInjector
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.DailyLogDao
import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.dao.WeeklyReportDao
import com.babymakisuk.coredata.entity.AiInsightEntity
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coredata.repository.ChildRepository
import com.babymakisuk.coremodel.GrowthSnapshot
import com.babymakisuk.coremodel.WeeklyReport
import com.babymakisuk.coremodel.WeeklySummaryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WeeklyReportRepository — Sprint 4 AI 整合版
 *
 * 使用 AiDispatcher + AiPromptBuilder 產生 AI 週報，
 * 並將結果持久化至 Room。
 */
@Singleton
class WeeklyReportRepository @Inject constructor(
    private val weeklyReportDao: WeeklyReportDao,
    private val medicalDao: MedicalDao,
    private val growthDao: GrowthDao,
    private val dailyLogDao: DailyLogDao,
    private val childRepository: ChildRepository,
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector,
    private val aiInsightDao: AiInsightDao
) {

    companion object {
        private const val TAG = "WeeklyReportRepository"
        private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** 觀察指定孩子、指定年份的所有週報 */
    fun observeByYear(childId: String, year: String): Flow<List<WeeklyReport>> =
        weeklyReportDao.getByYear(childId, year).map { list -> list.map { it.toDomain() } }

    /** FTS 關鍵字搜尋 */
    fun searchByKeyword(childId: String, keyword: String): Flow<List<WeeklyReport>> =
        weeklyReportDao.searchByKeyword(childId, keyword).map { list -> list.map { it.toDomain() } }

    /**
     * 產生並儲存 AI 週報。
     */
    suspend fun generateWeeklyReport(childId: Long, weekStart: LocalDate): WeeklyReport {
        val weekEnd = weekStart.plusDays(6)
        val weekStartStr = weekStart.format(dateFmt)
        val weekEndStr = weekEnd.format(dateFmt)
        val childIdStr = childId.toString()
        val reportId = "${childId}_${weekStart.year}-W${weekStart}"

        val child = childRepository.getById(childId)
        val childName = child?.name ?: "寶寶"
        val ageMonths = child?.ageMonths ?: 0
        val weekLabel = "${weekStart.year} 年第 ${weekStart.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)} 週（${weekStartStr.substring(5)}–${weekEndStr.substring(5)}）"

        val dailyLogs = dailyLogDao.getAllOnce()
            .filter { it.childId == childId && !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
            .sortedBy { it.date }

        val dailyLogsBlock = if (dailyLogs.isEmpty()) {
            "本週尚無日誌紀錄"
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
            .filter { it.childId == childId && !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }

        val recentMedical = if (medicalVisits.isEmpty()) null else {
            medicalVisits.joinToString("\n") { visit ->
                "${visit.date.format(dateFmt)} ${visit.hospital} ${visit.department}：${visit.diagnosis}"
            }
        }

        val latestGrowth = growthDao.getAllOnce()
            .filter { it.childId == childId }
            .maxByOrNull { it.date }

        val (systemPrompt, userPrompt) = AiPromptBuilder.buildWeeklyLogSummaryPrompt(
            childName = childName,
            ageMonths = ageMonths,
            weekLabel = weekLabel,
            dailyLogsBlock = dailyLogsBlock,
            recentMedical = recentMedical
        )

        val (aiSummary, searchKeywords) = try {
            val raw = aiDispatcher.executeWithSystemPrompt(
                task = AiTask.WEEKLY_REPORT,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            )
            val result = try {
                json.decodeFromString<WeeklySummaryResult>(raw)
            } catch (_: Exception) {
                WeeklySummaryResult(
                    weekSummary = raw.take(200),
                    highlights = emptyList(),
                    parentTips = emptyList(),
                    searchKeywords = emptyList()
                )
            }
            val summary = buildString {
                appendLine(result.weekSummary)
                if (result.highlights.isNotEmpty()) {
                    appendLine()
                    appendLine("▌本週亮點")
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
            Log.e(TAG, "generateWeeklyReport AI failed: ${e.message}")
            Pair("（本週週報 AI 生成失敗，請稍後重試）", emptyList<String>())
        }

        // 自動產出 AI 精華卡
        try {
            val insight = AiInsightEntity(
                id         = "weekly_${reportId}",
                childId    = childIdStr,
                title      = "週報摘要｜$weekLabel",
                content    = aiSummary.take(200),
                sourceDate = System.currentTimeMillis(),
                createdAt  = System.currentTimeMillis()
            )
            aiInsightDao.insert(insight)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create AiInsight from weekly report: ${e.message}")
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

        val report = WeeklyReport(
            id = reportId,
            childId = childIdStr,
            weekStart = weekStartStr,
            weekEnd = weekEndStr,
            aiSummary = aiSummary,
            medicalVisitIds = medicalVisits.map { it.id.toString() },
            growthSnapshot = growthSnapshot,
            vaccineDue = emptyList(),
            searchKeywords = keywords,
            driveFileId = null,
            syncedAt = System.currentTimeMillis()
        )

        weeklyReportDao.upsert(report.toEntity())
        return report
    }
}
