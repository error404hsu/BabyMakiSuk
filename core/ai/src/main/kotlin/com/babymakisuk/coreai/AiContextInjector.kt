package com.babymakisuk.coreai

import com.babymakisuk.coredata.dao.GrowthDao
import com.babymakisuk.coredata.dao.MedicalDao
import com.babymakisuk.coredata.repository.ChildRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AiContextInjector — 規則式 RAG Phase 1.5
 *
 * 純資料組裝層，不呼叫 LLM。
 * 從 Room 撈取最近就醫紀錄、最新成長紀錄、ChildProfile，
 * 組合成結構化 context block 供各 AI Repository 注入 prompt。
 */
@Singleton
class AiContextInjector @Inject constructor(
    private val medicalDao: MedicalDao,
    private val growthDao: GrowthDao,
    private val childRepository: ChildRepository
) {

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 組合個案背景 context block。
     *
     * @param childId 目標幼兒 ID
     * @return 格式化的 context 字串，含月齡、性別、過敏史、就醫紀錄、成長紀錄
     */
    suspend fun buildContext(childId: Long): String = withContext(Dispatchers.IO) {
        val child = childRepository.getById(childId)
        val allMedical = medicalDao.getAllOnce()
            .filter { it.childId == childId }
            .sortedByDescending { it.date }
            .take(3)
        val allGrowth = growthDao.getAllOnce()
            .filter { it.childId == childId }
            .sortedByDescending { it.date }

        // ── 個案基本資訊 ──────────────────────────────────────────────────────
        val ageMonths = child?.let {
            val today = java.time.LocalDate.now()
            val bd = it.birthday
            (today.year - bd.year) * 12 + (today.monthValue - bd.monthValue)
        } ?: 0
        val gender = child?.gender?.name ?: "未知"
        val allergies = child?.allergies?.takeIf { it.isNotBlank() } ?: "無"

        // ── 就醫紀錄區塊 ─────────────────────────────────────────────────────
        val medicalBlock = if (allMedical.isEmpty()) {
            "尚無就醫紀錄"
        } else {
            allMedical.joinToString("\n") { visit ->
                val dateStr = visit.date.format(dateFmt)
                val dept = visit.department.takeIf { it.isNotBlank() } ?: "-"
                val diag = visit.diagnosis.takeIf { it.isNotBlank() } ?: "-"
                val notes = visit.notes.takeIf { it.isNotBlank() } ?: "-"
                "$dateStr ${dept}｜診斷：$diag｜備註：$notes"
            }
        }

        // ── 成長紀錄區塊 ─────────────────────────────────────────────────────
        val growthBlock = allGrowth.firstOrNull()?.let { rec ->
            val dateStr = rec.date.format(dateFmt)
            "$dateStr 體重：${"%,.1f".format(rec.weightKg)} kg 身高：${"%,.1f".format(rec.heightCm)} cm"
        } ?: "尚無成長紀錄"

        buildString {
            appendLine("【當前個案資訊】")
            appendLine("當前月齡：$ageMonths 個月")
            appendLine("性別：$gender")
            appendLine("過敏史：$allergies")
            appendLine()
            appendLine("【最近就醫紀錄（最多 3 筆）】")
            appendLine(medicalBlock)
            appendLine()
            appendLine("【最新成長紀錄】")
            append(growthBlock)
        }
    }
}
