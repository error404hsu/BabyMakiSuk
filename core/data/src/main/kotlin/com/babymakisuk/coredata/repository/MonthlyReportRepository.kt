package com.babymakisuk.coredata.repository

import android.util.Log
import com.babymakisuk.coredata.ai.AiContextInjector
import com.babymakisuk.coreai.AiConfig
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiPromptBuilder
import com.babymakisuk.coreai.AiSystemConstraints
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.dao.AiInsightDao
import com.babymakisuk.coredata.dao.DailyLogDao
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.dao.MonthlyReportDao
import com.babymakisuk.coredata.dao.SystemReminderDao
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coredata.entity.MonthlyReportEntity
import com.babymakisuk.coredata.entity.SystemReminderEntity
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.MonthlyReport
import com.babymakisuk.coremodel.MonthlySummaryResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private fun String.escapeFts(): String {
    val special = setOf('*', '"', '-', '+', '(', ')')
    return filter { it !in special }
}

/*
 * REFACTOR PLAN:
 * Step 1 [NOW]    — Wrap all suspend funs with withContext(ioDispatcher).
 *                   Add flowOn(ioDispatcher) to all Flow pipelines.
 * Step 2 [NEXT]   — Extract aggregation logic into a MonthlyReportAggregator use-case.
 *                   Repository should only perform CRUD; aggregation belongs in domain layer.
 * Step 3 [FUTURE] — Replace multiple DAO calls per month with a single @Transaction JOIN query
 *                   to eliminate N+1 reads. Requires DB migration if schema changes.
 * Step 4 [FUTURE] — Add Paging3 for report list if data exceeds 12 months.
 */

// TODO [PERF] MonthlyReportRepository Step 2: extract aggregation to use-case layer
// TODO [PERF] MonthlyReportRepository Step 3: replace N+1 with @Transaction JOIN query
// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

interface MonthlyReportRepository {
    suspend fun checkAndCreateMonthlyReportReminder(force: Boolean = false)
    fun observeByYear(childId: Long, year: String): Flow<List<MonthlyReport>>
    fun getRecentReports(childId: Long, limit: Int = 50): Flow<List<MonthlyReport>>
    fun searchByKeyword(childId: Long, keyword: String): Flow<List<MonthlyReport>>
    suspend fun deleteReport(reportId: String)
    suspend fun generateMonthlyReport(yearMonth: YearMonth): MonthlyReport
}

@Singleton
class DefaultMonthlyReportRepository @Inject constructor(
    private val monthlyReportDao: MonthlyReportDao,
    private val medicalDao: MedicalDao,
    private val dailyLogDao: DailyLogDao,
    private val systemReminderDao: SystemReminderDao,
    private val childRepository: ChildRepository,
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector,
    private val aiInsightDao: AiInsightDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MonthlyReportRepository {

    companion object {
        private const val TAG = "MonthlyReportRepository"
        private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** 檢查並建立月底提醒 (於每月最後 7 天觸發) */
    override suspend fun checkAndCreateMonthlyReportReminder(force: Boolean) = withContext(ioDispatcher) {
        val today = java.time.LocalDate.now()
        val lastDayOfMonth = today.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
        val isLastWeek = today.isAfter(lastDayOfMonth.minusDays(7)) || today.isEqual(lastDayOfMonth)

        if (isLastWeek || force) {
            val monthLabel = today.format(DateTimeFormatter.ofPattern("yyyy/MM"))
            val reminderId = "monthly_remind_${today.year}_${today.monthValue}"
            
            // 檢查是否已存在 (不論 childId，月報提醒為全域)
            val existing = systemReminderDao.getAllOnce().find { it.id == reminderId }
            if (existing == null) {
                // 取得第一個孩子作為關聯 ID (由於 DB 限制 childId 為 Long 且有 FK 限制，無法使用 0 或 null)
                val children = childRepository.getChildren()
                val targetChildId = children.firstOrNull()?.id ?: return@withContext

                val reminder = SystemReminderEntity(
                    id = reminderId,
                    childId = targetChildId,
                    type = com.babymakisuk.coremodel.SystemReminderType.MONTHLY_REPORT_PENDING.name,
                    title = "月底月報預報",
                    content = "${monthLabel} 即將結束，AI 將於下月初為您整理本月成長點滴。請確保這幾天的日誌已填寫完整喔！",
                    createdAt = System.currentTimeMillis(),
                    resolvedAt = null
                )
                systemReminderDao.insert(reminder)
            } else if (force) {
                // 如果是強制測試，且已存在，則將其設為未解決狀態 (Resolved -> Unresolved)
                val updated = existing.copy(resolvedAt = null, createdAt = System.currentTimeMillis())
                systemReminderDao.insert(updated)
            }
        }
    }

    override fun observeByYear(childId: Long, year: String): Flow<List<MonthlyReport>> =
        monthlyReportDao.getByYear(childId, year)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun getRecentReports(childId: Long, limit: Int): Flow<List<MonthlyReport>> =
        monthlyReportDao.getRecentReports(childId, limit)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun searchByKeyword(childId: Long, keyword: String): Flow<List<MonthlyReport>> =
        monthlyReportDao.searchByKeyword(childId, keyword.escapeFts())
            .map { list -> list.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun deleteReport(reportId: String) = withContext(ioDispatcher) {
        monthlyReportDao.getById(reportId)?.let { entity ->
            monthlyReportDao.delete(entity)
        } ?: Unit
    }

    override suspend fun generateMonthlyReport(yearMonth: YearMonth): MonthlyReport = withContext(ioDispatcher) {
        val monthStart = yearMonth.atDay(1)
        val monthEnd = yearMonth.atEndOfMonth()
        val monthStartStr = monthStart.format(dateFmt)
        val monthEndStr = monthEnd.format(dateFmt)
        val yearValue = yearMonth.year
        val monthValue = yearMonth.monthValue
        val reportId = "merged_${yearValue}-M${String.format("%02d", monthValue)}"

        val children = childRepository.getChildren()
        val monthLabel = "${yearValue} 年 ${monthValue} 月（${monthStartStr.substring(5)}–${monthEndStr.substring(5)}）"

        val dailyLogs = dailyLogDao.getAllOnce()
            .filter { !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }
            .sortedBy { it.date }

        val dailyLogsBlock = if (dailyLogs.isEmpty()) {
            "本月尚無日誌紀錄"
        } else {
            dailyLogs.joinToString("\n") { log ->
                val dateStr = log.date.format(dateFmt)
                val childName = children.find { it.id == log.childId }?.name ?: "寶寶"
                val moodEmoji = when (log.mood) {
                    "HAPPY" -> "😊"
                    "NORMAL" -> "😐"
                    "FUSSY" -> "😤"
                    "SICK" -> "🤒"
                    else -> ""
                }
                "📅 $dateStr [$childName] 睡眠：${log.sleepInfo} 飲食：${log.mealsInfo} 便便：${log.poopCount}次 心情：$moodEmoji ${log.freeText}"
            }
        }

        val medicalVisits = medicalDao.getAllOnce()
            .filter { !it.date.isBefore(monthStart) && !it.date.isAfter(monthEnd) }

        val recentMedical = if (medicalVisits.isEmpty()) null else {
            medicalVisits.joinToString("\n") { visit ->
                val childName = children.find { it.id == visit.childId }?.name ?: "寶寶"
                "${visit.date.format(dateFmt)} [$childName] ${visit.hospital} ${visit.department}：${visit.diagnosis}"
            }
        }

        val monthStartEpoch = monthStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEndEpoch = monthEnd.atStartOfDay(java.time.ZoneId.systemDefault()).plusDays(1).toInstant().toEpochMilli()
        val systemReminders = systemReminderDao.getAllOnce()
            .filter { it.createdAt in monthStartEpoch until monthEndEpoch }
        
        val systemReminderBlock = if (systemReminders.isEmpty()) null else {
            systemReminders.joinToString("\n") { r ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(r.createdAt))
                val childName = children.find { it.id == r.childId }?.name ?: "寶寶"
                "$dateStr [$childName] ${r.title}：${r.content}"
            }
        }

        val (systemPrompt, userPrompt) = AiPromptBuilder.buildMonthlyLogSummaryPrompt(
            monthLabel = monthLabel,
            dailyLogsBlock = dailyLogsBlock,
            recentMedical = recentMedical,
            systemReminderBlock = systemReminderBlock
        )

        val resultPair = try {
            val raw = aiDispatcher.executeWithSystemPrompt(
                task = AiTask.MONTHLY_REPORT,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt
            ).getOrElse { throw it }
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
        val aiSummary = resultPair.first
        val searchKeywords = resultPair.second

        val keywords = if (searchKeywords.isNotEmpty()) searchKeywords else {
            medicalVisits.flatMap {
                listOf(it.department, it.diagnosis)
            }.filter { it.isNotBlank() }.distinct().take(5)
        }

        val report = MonthlyReport(
            id = reportId,
            childId = children.firstOrNull()?.id ?: 0L,
            monthStart = monthStartStr,
            monthEnd = monthEndStr,
            aiSummary = aiSummary,
            growthSnapshot = null, // 合併報告不顯示單一成長快照
            medicalCount = medicalVisits.size,
            systemReminderCount = systemReminders.size,
            searchKeywords = keywords,
            driveFileId = null,
            syncedAt = System.currentTimeMillis()
        )

        monthlyReportDao.upsert(report.toEntity())

        // 成功產生後，嘗試關閉對應月份的提醒
        val reminderId = "monthly_remind_${yearValue}_${monthValue}"
        systemReminderDao.markResolved(reminderId, System.currentTimeMillis())

        report
    }
}
