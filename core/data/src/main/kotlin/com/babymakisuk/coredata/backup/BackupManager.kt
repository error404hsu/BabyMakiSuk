package com.babymakisuk.coredata.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.babymakisuk.coredata.db.AppDatabase
import com.babymakisuk.coredata.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

// ── 備份容器 ──────────────────────────────────────────

@Serializable
data class BackupDto(
    val version: Int = 4,
    val exportedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val children: List<ChildProfileBackup> = emptyList(),
    val growthRecords: List<GrowthRecordBackup> = emptyList(),
    val medicalVisits: List<MedicalVisitBackup> = emptyList(),
    val vaccineRecords: List<VaccineRecordBackup> = emptyList(),
    val dailyLogs: List<DailyLogBackup> = emptyList(),
    val monthlyReports: List<MonthlyReportBackup> = emptyList(),
    val aiInsights: List<AiInsightBackup> = emptyList(),
    val memos: List<MemoBackup> = emptyList(),
    val toiletRecords: List<ToiletRecordBackup> = emptyList(),
    val vaccineReminders: List<VaccineReminderBackup> = emptyList(),
    val systemReminders: List<SystemReminderBackup> = emptyList(),
    val chatMessages: List<ChatMessageBackup> = emptyList(),
    val feverRecords: List<FeverRecordBackup> = emptyList(),
)

@Serializable data class ChildProfileBackup(
    val id: Long, val name: String, val gender: String,
    val birthday: String, val bloodType: String? = null,
    val allergies: String? = null, val note: String = "",
    val photoUri: String? = null,
    val defaultAiPrompt: String? = null
)

@Serializable data class GrowthRecordBackup(
    val id: Long, val childId: Long, val date: String,
    val heightCm: Float, val weightKg: Float,
    val headCircumferenceCm: Float? = null, val note: String = "",
    val aiSuggestion: String? = null
)

@Serializable data class MedicalVisitBackup(
    val id: Long, val childId: Long, val date: String,
    val hospital: String, val department: String = "",
    val diagnosis: String = "", val notes: String = "",
    val attachments: String = "",
    val diagnosisSummary: String = "",
    val prescriptions: String = "",
    val careInstructions: String = "",
    val isUrgent: Boolean = false,
    val imageStoragePath: String? = null,
    val aiPending: Boolean = false
)

@Serializable data class VaccineRecordBackup(
    val id: Long, val childId: Long, val vaccineName: String,
    val dose: Int = 1, val date: String, val note: String = ""
)

@Serializable data class DailyLogBackup(
    val id: Long, val childId: Long, val date: String,
    val sleepInfo: String = "", val mealsInfo: String = "",
    val poopCount: Int = 0, val mood: String = "", val freeText: String = ""
)

@Serializable data class MonthlyReportBackup(
    val id: String, val childId: Long,
    val monthStart: String, val monthEnd: String, val aiSummary: String,
    val snapshotWeight: Double?, val snapshotHeight: Double?,
    val snapshotHeadCirc: Double?, val medicalCount: Int,
    val systemReminderCount: Int, val searchKeywords: String,
    val driveFileId: String?, val syncedAt: Long
)

@Serializable data class AiInsightBackup(
    val id: String, val childId: String, val title: String,
    val content: String, val sourceDate: Long, val createdAt: Long
)

@Serializable data class MemoBackup(
    val id: Long, val childId: Long, val title: String,
    val content: String, val date: Long, val reminderAt: Long?,
    val createdAt: Long
)

@Serializable data class ToiletRecordBackup(
    val id: Long, val childId: Long, val timestamp: Long
)

@Serializable data class VaccineReminderBackup(
    val id: Long, val childId: Long, val name: String,
    val scheduledDate: Long, val isCompleted: Boolean, val note: String
)

@Serializable data class SystemReminderBackup(
    val id: String, val childId: Long, val type: String,
    val title: String, val content: String, val createdAt: Long,
    val resolvedAt: Long?
)

@Serializable data class ChatMessageBackup(
    val id: String, val role: String, val text: String, val timestampMs: Long
)

@Serializable data class FeverRecordBackup(
    val id: Long, val childId: Long, val temperatureCelsius: Float,
    val measuredAt: Long, val symptoms: String, val note: String,
    val isMedicineTaken: Boolean, val linkedVisitId: Long?
)

// ── BackupManager ──────────────────────────────────────────

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    // ── 匯出 ──────────────────────────────────────────

    /** 將所有 Room 資料封裝成 JSON 字串 */
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val dto = BackupDto(
            children         = db.childDao().getAllOnce().map { it.toBackup() },
            growthRecords    = db.growthDao().getAllOnce().map { it.toBackup() },
            medicalVisits    = db.medicalDao().getAllOnce().map { it.toBackup() },
            vaccineRecords   = db.vaccineDao().getAllOnce().map { it.toBackup() },
            dailyLogs        = db.dailyLogDao().getAllOnce().map { it.toBackup() },
            monthlyReports   = db.monthlyReportDao().getAllOnce().map { it.toBackup() },
            aiInsights       = db.aiInsightDao().getAllOnce().map { it.toBackup() },
            memos            = db.memoDao().getAllOnce().map { it.toBackup() },
            toiletRecords    = db.toiletDao().getAllOnce().map { it.toBackup() },
            vaccineReminders = db.vaccineReminderDao().getAllOnce().map { it.toBackup() },
            systemReminders  = db.systemReminderDao().getAllOnce().map { it.toBackup() },
            chatMessages     = db.chatMessageDao().getAllOnce().map { it.toBackup() },
            feverRecords     = db.feverDao().getAllOnce().map { it.toBackup() }
        )
        json.encodeToString(dto)
    }

    /**
     * 將 JSON 寫入暫存檔並透過 ShareSheet 分享。
     * 回傳可直接傳給 startActivity() 的 Intent。
     */
    suspend fun exportToShareIntent(): Intent = withContext(Dispatchers.IO) {
        val jsonString = exportToJson()
        val filename = "babymakisuk_backup_${LocalDate.now()}.json"
        val file = File(context.cacheDir, filename)
        file.writeText(jsonString)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "寶寶資料備份 - $filename")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── 匯入 ──────────────────────────────────────────

    /**
     * 從 Uri 讀取 JSON 備份檔並還原資料。
     * @param merge true = 合併資料（相同 id 覆蓋）；false = 清空後寫入。
     */
    suspend fun importFromUri(uri: Uri, merge: Boolean = true) = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver
            .openInputStream(uri)
            ?.bufferedReader()
            ?.readText()
            ?: error("無法讀取備份檔案")

        val dto = json.decodeFromString<BackupDto>(jsonString)

        db.runInTransaction {
            kotlinx.coroutines.runBlocking {
                if (!merge) {
                    db.childDao().deleteAll()
                    db.growthDao().deleteAll()
                    db.medicalDao().deleteAll()
                    db.vaccineDao().deleteAll()
                    db.dailyLogDao().deleteAll()
                    db.monthlyReportDao().deleteAll()
                    db.aiInsightDao().deleteAll()
                    db.memoDao().deleteAll()
                    db.toiletDao().deleteAll()
                    db.vaccineReminderDao().deleteAll()
                    db.systemReminderDao().deleteAll()
                    db.chatMessageDao().deleteAll()
                    db.feverDao().deleteAll()
                }
                db.childDao().upsertAll(dto.children.map { it.toEntity() })
                db.growthDao().upsertAll(dto.growthRecords.map { it.toEntity() })
                db.medicalDao().upsertAll(dto.medicalVisits.map { it.toEntity() })
                db.vaccineDao().upsertAll(dto.vaccineRecords.map { it.toEntity() })
                db.dailyLogDao().upsertAll(dto.dailyLogs.map { it.toEntity() })
                db.monthlyReportDao().upsertAll(dto.monthlyReports.map { it.toEntity() })
                db.aiInsightDao().upsertAll(dto.aiInsights.map { it.toEntity() })
                db.memoDao().upsertAll(dto.memos.map { it.toEntity() })
                db.toiletDao().upsertAll(dto.toiletRecords.map { it.toEntity() })
                db.vaccineReminderDao().upsertAll(dto.vaccineReminders.map { it.toEntity() })
                db.systemReminderDao().upsertAll(dto.systemReminders.map { it.toEntity() })
                db.chatMessageDao().upsertAll(dto.chatMessages.map { it.toEntity() })
                db.feverDao().upsertAll(dto.feverRecords.map { it.toEntity() })
            }
        }
    }

    // ── Entity → Backup DTO 映射 ───────────────────────────

    private fun ChildProfileEntity.toBackup() = ChildProfileBackup(
        id = id, name = name, gender = gender,
        birthday = birthday.toString(), bloodType = bloodType,
        allergies = allergies, note = note, photoUri = photoUri,
        defaultAiPrompt = defaultAiPrompt
    )

    private fun GrowthRecordEntity.toBackup() = GrowthRecordBackup(
        id = id, childId = childId, date = date.toString(),
        heightCm = heightCm, weightKg = weightKg,
        headCircumferenceCm = headCircumferenceCm, note = note,
        aiSuggestion = aiSuggestion
    )

    private fun MedicalVisitEntity.toBackup() = MedicalVisitBackup(
        id = id, childId = childId, date = date.toString(),
        hospital = hospital, department = department,
        diagnosis = diagnosis, notes = notes,
        attachments = attachments,
        diagnosisSummary = diagnosisSummary,
        prescriptions = prescriptions,
        careInstructions = careInstructions,
        isUrgent = isUrgent,
        imageStoragePath = imageStoragePath,
        aiPending = aiPending
    )

    private fun VaccineRecordEntity.toBackup() = VaccineRecordBackup(
        id = id, childId = childId, vaccineName = vaccineName,
        dose = dose, date = date.toString(), note = note
    )

    private fun DailyLogEntity.toBackup() = DailyLogBackup(
        id = id, childId = childId, date = date.toString(),
        sleepInfo = sleepInfo, mealsInfo = mealsInfo,
        poopCount = poopCount, mood = mood, freeText = freeText
    )

    private fun MonthlyReportEntity.toBackup() = MonthlyReportBackup(
        id = id, childId = childId,
        monthStart = monthStart, monthEnd = monthEnd, aiSummary = aiSummary,
        snapshotWeight = snapshotWeight, snapshotHeight = snapshotHeight,
        snapshotHeadCirc = snapshotHeadCirc, medicalCount = medicalCount,
        systemReminderCount = systemReminderCount, searchKeywords = searchKeywords,
        driveFileId = driveFileId, syncedAt = syncedAt
    )

    private fun AiInsightEntity.toBackup() = AiInsightBackup(
        id = id, childId = childId, title = title,
        content = content, sourceDate = sourceDate, createdAt = createdAt
    )

    private fun MemoEntity.toBackup() = MemoBackup(
        id = id, childId = childId, title = title,
        content = content, date = date, reminderAt = reminderAt,
        createdAt = createdAt
    )

    private fun ToiletRecordEntity.toBackup() = ToiletRecordBackup(
        id = id, childId = childId, timestamp = timestamp
    )

    private fun VaccineReminderEntity.toBackup() = VaccineReminderBackup(
        id = id, childId = childId, name = name,
        scheduledDate = scheduledDate, isCompleted = isCompleted, note = note
    )

    private fun SystemReminderEntity.toBackup() = SystemReminderBackup(
        id = id, childId = childId, type = type, title = title,
        content = content, createdAt = createdAt, resolvedAt = resolvedAt
    )

    private fun ChatMessageEntity.toBackup() = ChatMessageBackup(
        id = id, role = role, text = text, timestampMs = timestampMs
    )

    private fun FeverRecordEntity.toBackup() = FeverRecordBackup(
        id = id, childId = childId, temperatureCelsius = temperatureCelsius,
        measuredAt = measuredAt, symptoms = symptoms, note = note,
        isMedicineTaken = isMedicineTaken, linkedVisitId = linkedVisitId
    )

    // ── Backup DTO → Entity 映射 ───────────────────────────

    private fun ChildProfileBackup.toEntity() = ChildProfileEntity(
        id = id, name = name, gender = gender,
        birthday = LocalDate.parse(birthday), bloodType = bloodType,
        allergies = allergies, note = note, photoUri = photoUri,
        defaultAiPrompt = defaultAiPrompt
    )

    private fun GrowthRecordBackup.toEntity() = GrowthRecordEntity(
        id = id, childId = childId, date = LocalDate.parse(date),
        heightCm = heightCm, weightKg = weightKg,
        headCircumferenceCm = headCircumferenceCm, note = note,
        aiSuggestion = aiSuggestion
    )

    private fun MedicalVisitBackup.toEntity() = MedicalVisitEntity(
        id = id, childId = childId, date = LocalDate.parse(date),
        hospital = hospital, department = department,
        diagnosis = diagnosis, notes = notes,
        attachments = attachments,
        diagnosisSummary = diagnosisSummary,
        prescriptions = prescriptions,
        careInstructions = careInstructions,
        isUrgent = isUrgent,
        imageStoragePath = imageStoragePath,
        aiPending = aiPending
    )

    private fun VaccineRecordBackup.toEntity() = VaccineRecordEntity(
        id = id, childId = childId, vaccineName = vaccineName,
        dose = dose, date = LocalDate.parse(date), note = note
    )

    private fun DailyLogBackup.toEntity() = DailyLogEntity(
        id = id, childId = childId, date = LocalDate.parse(date),
        sleepInfo = sleepInfo, mealsInfo = mealsInfo,
        poopCount = poopCount, mood = mood, freeText = freeText
    )

    private fun MonthlyReportBackup.toEntity() = MonthlyReportEntity(
        id = id, childId = childId,
        monthStart = monthStart, monthEnd = monthEnd, aiSummary = aiSummary,
        snapshotWeight = snapshotWeight, snapshotHeight = snapshotHeight,
        snapshotHeadCirc = snapshotHeadCirc, medicalCount = medicalCount,
        systemReminderCount = systemReminderCount, searchKeywords = searchKeywords,
        driveFileId = driveFileId, syncedAt = syncedAt
    )

    private fun AiInsightBackup.toEntity() = AiInsightEntity(
        id = id, childId = childId, title = title,
        content = content, sourceDate = sourceDate, createdAt = createdAt
    )

    private fun MemoBackup.toEntity() = MemoEntity(
        id = id, childId = childId, title = title,
        content = content, date = date, reminderAt = reminderAt,
        createdAt = createdAt
    )

    private fun ToiletRecordBackup.toEntity() = ToiletRecordEntity(
        id = id, childId = childId, timestamp = timestamp
    )

    private fun VaccineReminderBackup.toEntity() = VaccineReminderEntity(
        id = id, childId = childId, name = name,
        scheduledDate = scheduledDate, isCompleted = isCompleted, note = note
    )

    private fun SystemReminderBackup.toEntity() = SystemReminderEntity(
        id = id, childId = childId, type = type, title = title,
        content = content, createdAt = createdAt, resolvedAt = resolvedAt
    )

    private fun ChatMessageBackup.toEntity() = ChatMessageEntity(
        id = id, role = role, text = text, timestampMs = timestampMs
    )

    private fun FeverRecordBackup.toEntity() = FeverRecordEntity(
        id = id, childId = childId, temperatureCelsius = temperatureCelsius,
        measuredAt = measuredAt, symptoms = symptoms, note = note,
        isMedicineTaken = isMedicineTaken, linkedVisitId = linkedVisitId
    )
}
