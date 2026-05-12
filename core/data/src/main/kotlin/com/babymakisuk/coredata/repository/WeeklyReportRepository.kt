package com.babymakisuk.coredata.repository

import android.util.Log
import com.babymakisuk.coreai.AiContextInjector
import com.babymakisuk.coreai.AiDispatcher
import com.babymakisuk.coreai.AiTask
import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.dao.WeeklyReportDao
import com.babymakisuk.coredata.entity.toDomain
import com.babymakisuk.coredata.entity.toEntity
import com.babymakisuk.coremodel.GrowthSnapshot
import com.babymakisuk.coremodel.WeeklyReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WeeklyReportRepository — Sprint 3 AI 整合版
 *
 * 使用 AiDispatcher + AiContextInjector 產生 AI 週報，
 * 並將結果持久化至 Room。
 */
@Singleton
class WeeklyReportRepository @Inject constructor(
    private val weeklyReportDao: WeeklyReportDao,
    private val medicalDao: MedicalDao,
    private val growthDao: GrowthDao,
    private val aiDispatcher: AiDispatcher,
    private val aiContextInjector: AiContextInjector
) {

    companion object {
        private const val TAG = "WeeklyReportRepository"
        private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /** 觀察指定孩子、指定年份的所有週報 */
    fun observeByYear(childId: String, year: String): Flow<List<WeeklyReport>> =
        weeklyReportDao.getByYear(childId, year).map { list -> list.map { it.toDomain() } }

    /** FTS 關鍵字搜尋 */
    fun searchByKeyword(childId: String, keyword: String): Flow<List<WeeklyReport>> =
        weeklyReportDao.searchByKeyword(childId, keyword).map { list -> list.map { it.toDomain() } }

    /**
     * 產生並儲存 AI 週報。
     *
     * 1. 查詢該週 MedicalVisit
     * 2. 查詢最新 GrowthRecord
     * 3. buildContext 注入背景資訊
     * 4. 組合 prompt 送入 AiDispatcher
     * 5. 解析回應，建立 WeeklyReport 並儲存
     * 6. 失敗時建立含錯誤訊息的 fallback WeeklyReport
     */
    suspend fun generateWeeklyReport(childId: Long, weekStart: LocalDate): WeeklyReport {
        val weekEnd = weekStart.plusDays(6)
        val weekStartStr = weekStart.format(dateFmt)
        val weekEndStr = weekEnd.format(dateFmt)
        val childIdStr = childId.toString()
        val reportId = "${childId}_${weekStart.year}-W${weekStart}"

        // 查詢該週就醫紀錄
        val medicalVisits = medicalDao.getAllOnce()
            .filter { it.childId == childId && !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }

        // 查詢最新成長紀錄
        val latestGrowth = growthDao.getAllOnce()
            .filter { it.childId == childId }
            .maxByOrNull { it.date }

        // 建立 context block
        val contextBlock = try {
            aiContextInjector.buildContext(childId)
        } catch (e: Exception) {
            Log.w(TAG, "buildContext failed: ${e.message}")
            ""
        }

        // 組合 prompt
        val prompt = buildString {
            if (contextBlock.isNotBlank()) {
                appendLine(contextBlock)
                appendLine()
            }
            appendLine("請根據以下本週資料，為幼兒生成一份繁體中文週報摘要（200 字以內）：")
            appendLine()

            // DailyLog 尚未實作，以佔位文字顯示
            appendLine("【本週日誌】")
            appendLine("本週尚無日誌紀錄")
            appendLine()

            appendLine("【本週就醫紀錄】")
            if (medicalVisits.isEmpty()) {
                appendLine("本週無就醫紀錄")
            } else {
                medicalVisits.forEach { visit ->
                    appendLine("${visit.date.format(dateFmt)} ${visit.department}：${visit.diagnosis}")
                }
            }
            appendLine()

            appendLine("【最新成長紀錄】")
            if (latestGrowth != null) {
                appendLine("${latestGrowth.date.format(dateFmt)} 體重：${"%,.1f".format(latestGrowth.weightKg)} kg 身高：${"%,.1f".format(latestGrowth.heightCm)} cm")
            } else {
                appendLine("尚無成長紀錄")
            }
        }

        val aiSummary = try {
            aiDispatcher.execute(AiTask.WEEKLY_REPORT, prompt)
        } catch (e: Exception) {
            Log.e(TAG, "generateWeeklyReport AI failed: ${e.message}")
            "（本週週報 AI 生成失敗，請稍後重試）"
        }

        // 萃取 searchKeywords（取 AI 摘要前 5 個詞作為備援關鍵字）
        val keywords = medicalVisits.flatMap {
            listOf(it.department, it.diagnosis)
        }.filter { it.isNotBlank() }.distinct().take(5)

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
