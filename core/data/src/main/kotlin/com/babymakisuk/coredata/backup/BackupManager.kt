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
    val version: Int = 2,
    val exportedAt: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val children: List<ChildProfileBackup> = emptyList(),
    val growthRecords: List<GrowthRecordBackup> = emptyList(),
    val medicalVisits: List<MedicalVisitBackup> = emptyList(),
    val vaccineRecords: List<VaccineRecordBackup> = emptyList(),
    val dailyLogs: List<DailyLogBackup> = emptyList()
)

@Serializable data class ChildProfileBackup(
    val id: Long, val name: String, val gender: String,
    val birthday: String, val bloodType: String? = null, val note: String = ""
)

@Serializable data class GrowthRecordBackup(
    val id: Long, val childId: Long, val date: String,
    val weightKg: Double? = null, val heightCm: Double? = null,
    val headCircumferenceCm: Double? = null, val note: String = ""
)

@Serializable data class MedicalVisitBackup(
    val id: Long, val childId: Long, val visitDate: String,
    val hospital: String = "", val department: String = "",
    val diagnosis: String = "", val note: String = "",
    val diagnosisSummary: String? = null,
    val prescriptions: String? = null,
    val careInstructions: String? = null,
    val imageStoragePath: String? = null,
    val aiPending: Boolean = false
)

@Serializable data class VaccineRecordBackup(
    val id: Long, val childId: Long, val vaccineName: String,
    val scheduledDate: String, val completedDate: String? = null,
    val note: String = ""
)

@Serializable data class DailyLogBackup(
    val id: Long, val childId: Long, val date: String,
    val mealNote: String = "", val sleepNote: String = "",
    val moodEmoji: String = "", val note: String = ""
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
            children       = db.childDao().getAllOnce().map { it.toBackup() },
            growthRecords  = db.growthDao().getAllOnce().map { it.toBackup() },
            medicalVisits  = db.medicalDao().getAllOnce().map { it.toBackup() },
            vaccineRecords = db.vaccineDao().getAllOnce().map { it.toBackup() },
            dailyLogs      = db.dailyLogDao().getAllOnce().map { it.toBackup() }
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
                }
                db.childDao().upsertAll(dto.children.map { it.toEntity() })
                db.growthDao().upsertAll(dto.growthRecords.map { it.toEntity() })
                db.medicalDao().upsertAll(dto.medicalVisits.map { it.toEntity() })
                db.vaccineDao().upsertAll(dto.vaccineRecords.map { it.toEntity() })
                db.dailyLogDao().upsertAll(dto.dailyLogs.map { it.toEntity() })
            }
        }
    }

    // ── Entity → Backup DTO 映射 ───────────────────────────

    private fun ChildProfileEntity.toBackup() = ChildProfileBackup(
        id = id, name = name, gender = gender,
        birthday = birthday.toString(), bloodType = bloodType, note = note
    )

    private fun GrowthRecordEntity.toBackup() = GrowthRecordBackup(
        id = id, childId = childId, date = date.toString(),
        weightKg = weightKg, heightCm = heightCm,
        headCircumferenceCm = headCircumferenceCm, note = note
    )

    private fun MedicalVisitEntity.toBackup() = MedicalVisitBackup(
        id = id, childId = childId, visitDate = visitDate.toString(),
        hospital = hospital, department = department,
        diagnosis = diagnosis, note = note,
        diagnosisSummary = diagnosisSummary,
        prescriptions = prescriptions,
        careInstructions = careInstructions,
        imageStoragePath = imageStoragePath,
        aiPending = aiPending
    )

    private fun VaccineRecordEntity.toBackup() = VaccineRecordBackup(
        id = id, childId = childId, vaccineName = vaccineName,
        scheduledDate = scheduledDate.toString(),
        completedDate = completedDate?.toString(), note = note
    )

    private fun DailyLogEntity.toBackup() = DailyLogBackup(
        id = id, childId = childId, date = date.toString(),
        mealNote = mealNote, sleepNote = sleepNote,
        moodEmoji = moodEmoji, note = note
    )

    // ── Backup DTO → Entity 映射 ───────────────────────────

    private fun ChildProfileBackup.toEntity() = ChildProfileEntity(
        id = id, name = name, gender = gender,
        birthday = LocalDate.parse(birthday), bloodType = bloodType, note = note
    )

    private fun GrowthRecordBackup.toEntity() = GrowthRecordEntity(
        id = id, childId = childId, date = LocalDate.parse(date),
        weightKg = weightKg, heightCm = heightCm,
        headCircumferenceCm = headCircumferenceCm, note = note
    )

    private fun MedicalVisitBackup.toEntity() = MedicalVisitEntity(
        id = id, childId = childId, visitDate = LocalDate.parse(visitDate),
        hospital = hospital, department = department,
        diagnosis = diagnosis, note = note,
        diagnosisSummary = diagnosisSummary,
        prescriptions = prescriptions,
        careInstructions = careInstructions,
        imageStoragePath = imageStoragePath,
        aiPending = aiPending
    )

    private fun VaccineRecordBackup.toEntity() = VaccineRecordEntity(
        id = id, childId = childId, vaccineName = vaccineName,
        scheduledDate = LocalDate.parse(scheduledDate),
        completedDate = completedDate?.let { LocalDate.parse(it) },
        note = note
    )

    private fun DailyLogBackup.toEntity() = DailyLogEntity(
        id = id, childId = childId, date = LocalDate.parse(date),
        mealNote = mealNote, sleepNote = sleepNote,
        moodEmoji = moodEmoji, note = note
    )
}
