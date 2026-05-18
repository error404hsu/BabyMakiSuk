# BabyMakiSuk 資料留存與清理策略

> **文件版本**: v1.2  
> **適用模組**: `core/data`, `core/firebase`, `core/drive`  
> **目標讀者**: AI Agents、開發者  
> **最後更新**: 2026-05-18

---

## 總覽

本文件定義 BabyMakiSuk 所有資料表的分層儲存策略，分為三個層次：

| 層次 | 位置 | 說明 |
|------|------|------|
| **L1 — 本地 Room DB** | 裝置本機 SQLite | 高頻讀寫、離線可用 |
| **L2 — Firebase Firestore** | 雲端文字資料 | 跨裝置同步、即時檢索 |
| **L3 — Google Drive** | 雲端二進位檔案 | 照片、JSON 備份、大型附件 |

**核心原則：先備份、後清除。** 任何本地清理動作必須確認已同步至對應雲端層次後才執行。  
清理任務若備份失敗，**必須中止所有清除操作並回傳 `Result.retry()`**。

---

## 一、各 Entity 留存決策表

### 永久留存（不可刪除）

| Entity | 資料性質 | 雲端目標 | 備註 |
|--------|---------|---------|------|
| `ChildProfileEntity` | 孩童基本資料 | Firestore `children/{childId}` | 所有功能錨點，永遠不清除 |
| `GrowthRecordEntity` | 身高/體重/頭圍曲線 | Firestore `growthRecords` | 醫療追蹤依據，永遠不清除 |
| `VaccineRecordEntity` | 疫苗接種史 | Firestore `vaccineRecords` | 法定健康紀錄，永遠不清除 |
| `MedicalVisitEntity`（結構欄位） | 就診/診斷/醫囑文字 | Firestore `medicalVisits` | 永久保存文字欄位 |
| `MedicalVisitEntity`（照片） | 處方箋/X光圖片 | **Google Drive** `/BabyMakiSuk/{childId}/prescriptions/` | 照片走 Drive，節省 Firestore 配額 |
| `MonthlyReportEntity` | 月度 AI 摘要 | Firestore `monthlyReports` + Drive JSON | 每月一筆，體積極小且價值高 |
| `MemoEntity` | 用戶手動備忘 | Firestore `memos` | 用戶主動輸入，不可自動刪除 |

### 定時清理（本地）

| Entity | 本地保留期限 | 清理觸發條件 | 清理後動作 |
|--------|------------|------------|---------|
| `DailyLogEntity` | 最近 **90 天** | 月報生成後立即清理對應月份；WorkManager 只清理「**2 個月以前**且已有對應 MonthlyReport」的資料 | 先確認 Firestore 已同步，再刪本地明細 |
| `ToiletRecordEntity` | 最近 **60 天** | 同上，月報生成後立即清理；WorkManager 加月報存在防護 | 直接刪除（已彙整至 MonthlyReport） |
| `ChatMessageEntity` | 最近 **30 筆/對話** | 每次啟動 App 時觸發；摘要 AI 呼叫失敗時**保留舊訊息，不刪除** | 舊訊息壓縮摘要存入 `AiInsightEntity` 後刪除；若摘要失敗則跳過本次清理 |
| `AiInsightEntity` | 最近 **6 個月** | 每月 WorkManager 定時任務 | 摘要已在 `MonthlyReportEntity`，超齡可刪 |
| `SystemReminderEntity` | **已觸發後立即** | 提醒完成 callback | 無保存價值，立即清除 |
| `VaccineReminderEntity` | **接種完成後立即** | 接種操作完成 callback | 接種記錄已在 `VaccineRecordEntity` 中保存 |
| `MonthlyReportFts` | 跟隨 `MonthlyReportEntity` | FTS rebuild 觸發 | 虛擬索引表，定期 `REBUILD` 即可 |

---

## 二、月報生成流程與清理綁定

### 2.1 月報觸發入口

- **通知**：每月底由 WorkManager 推送首頁通知提醒使用者生成月報
- **操作位置**：使用者進入「書庫」→「月報頁」手動點擊生成
- **網路需求**：月報生成**需要網路**（呼叫 AI API 產生摘要文字）

### 2.2 月報頁 UI 狀態機

| 狀態 | 顯示內容 | 可操作項目 |
|------|---------|-----------|
| 本月資料充足、未生成 | 「本月有 X 筆日誌，點擊生成月報」 | 生成按鈕（主要 CTA） |
| 生成中 | Loading + 進度說明 | 不可中斷 |
| 已生成 | 顯示 AI 摘要內容 | 查看 / 分享 / 匯出 |
| 本月資料不足（< 3 筆） | 提示「記錄太少，建議繼續記錄」 | 可強制生成或略過 |
| 無網路 | 提示「需要網路連線才能生成摘要」 | 可稍後再試 |

### 2.3 月報生成失敗處理

```kotlin
fun generateReport(yearMonth: YearMonth) {
    viewModelScope.launch {
        val result = runCatching { reportRepository.generate(yearMonth) }
        if (result.isSuccess) {
            retentionRepository.cleanRawDataForMonth(yearMonth)
        } else {
            _uiState.update { it.copy(error = ReportError.GenerationFailed) }
        }
    }
}
```

> **重要**：月報生成與清理為原子性操作。AI API 呼叫失敗時，**不執行任何清理動作**，原始資料完整保留。

### 2.4 逾期未生成的三層保護

1. **第一層（WorkManager）**：自動清理任務只清理「**2 個月以前**且對應月份已有 `MonthlyReportEntity`」的資料，完全跳過最近一個月
2. **第二層（月報頁）**：月報生成成功後立即觸發對應月份的原始資料清理
3. **第三層（UI 提示）**：若月底通知發出後超過 7 天仍未生成，書庫入口顯示紅點角標強化提醒

---

## 三、照片（處方箋／附件）完整流程

### 3.1 流程圖

```text
使用者拍照／選圖
        │
        ▼
  [本地暫存 Cache]
  context.cacheDir/prescriptions/
        │
        ▼
  PrescriptionImagePreprocessor
  壓縮 + 裁切 + EXIF 清除
        │
        ▼
  上傳至 Google Drive
  路徑: /BabyMakiSuk/{childId}/prescriptions/{visitId}_{timestamp}.jpg
        │
        ▼
  取得 Drive fileId
        │
        ├─► 更新 MedicalVisitEntity.imageStoragePath = ImageStoragePath.Drive(fileId)
        │
        ├─► AI OCR 解析（若 aiPending = true）
        │         │
        │         ▼
        │   更新 MedicalVisitEntity
        │   prescriptions, diagnosis, careInstructions
        │   aiPending = false
        │
        └─► 刪除本地暫存 (cacheDir 中的檔案)
```

### 3.2 `ImageStoragePath` 型別安全封裝

原始 `imageStoragePath: String?` 欄位改以 sealed class 封裝，避免呼叫端手動解析前綴字串：

```kotlin
sealed class ImageStoragePath {
    data class Local(val absolutePath: String) : ImageStoragePath()
    data class Drive(val fileId: String) : ImageStoragePath()
    data object None : ImageStoragePath()
}

class ImageStoragePathConverter {
    @TypeConverter
    fun fromPath(value: String?): ImageStoragePath = when {
        value == null -> ImageStoragePath.None
        value.startsWith("local:") -> ImageStoragePath.Local(value.removePrefix("local:"))
        value.startsWith("drive:") -> ImageStoragePath.Drive(value.removePrefix("drive:"))
        else -> ImageStoragePath.None
    }

    @TypeConverter
    fun toPath(path: ImageStoragePath): String? = when (path) {
        is ImageStoragePath.Local -> "local:${path.absolutePath}"
        is ImageStoragePath.Drive -> "drive:${path.fileId}"
        ImageStoragePath.None -> null
    }
}
```

> **注意**：`firebase:` 前綴保留為備用，目前照片優先使用 Google Drive。

### 3.3 Drive 資料夾結構

```text
Google Drive (appDataFolder — 對用戶不可見，適合備份 JSON)
└── BabyMakiSuk/
    ├── {childId}/
    │   ├── prescriptions/
    │   │   ├── {visitId}_{timestamp}.jpg
    │   │   └── {visitId}_{timestamp}.jpg
    │   └── profile/
    │       └── avatar.jpg
    └── backups/
        ├── babymakisuk_backup_2026-05.json
        └── babymakisuk_backup_2026-04.json
```

> **Drive 資料夾選擇**：備份 JSON 使用 `appDataFolder`；處方箋照片若未來考慮讓使用者自行管理，可改為一般資料夾，需調整 `DriveRepository` 的 API scope。

---

## 四、欄位型別規範

為避免 DAO 與 Worker 在時間單位上出現不一致，以下欄位型別需固定：

| 欄位 | 建議型別 | 儲存格式 | 備註 |
|------|----------|----------|------|
| `DailyLogEntity.date` | `Long` | epoch day | 例如 `LocalDate.now().toEpochDay()` |
| `ToiletRecordEntity.timestamp` | `Long` | epoch millis | 例如 `System.currentTimeMillis()` |
| `AiInsightEntity.createdAt` | `Long` | epoch millis | 建立時間 |
| `MonthlyReportEntity.yearMonth` | `String` | `YYYY-MM` | 例如 `2026-05` |
| `lastSyncTime` | `Long` | epoch millis | 同步基準時間 |

> **規則**：凡是「日期」概念使用 epoch day；凡是「時間點」概念使用 epoch millis；凡是「月份聚合鍵」統一使用 `YYYY-MM` 字串。

---

## 五、`DataRetentionWorker` 實作

放置路徑：`core/data/src/main/kotlin/com/babymakisuk/coredata/worker/DataRetentionWorker.kt`

```kotlin
@HiltWorker
class DataRetentionWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val retentionRepository: DataRetentionRepository,
    private val backupManager: BackupManager,
) : CoroutineWorker(ctx, params) {

    companion object {
        const val WORK_NAME = "DataRetentionWorker"
        private val CUTOFF_60_DAYS = 60L * 24 * 3600 * 1000
        private val CUTOFF_180_DAYS = 180L * 24 * 3600 * 1000

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DataRetentionWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()

            val backupSuccess = runCatching {
                backupManager.exportAndUploadToDrive()
            }.isSuccess
            if (!backupSuccess) return Result.retry()

            val dailyCutoffDay = LocalDate.now().minusDays(90).toEpochDay()
            retentionRepository.cleanDailyLogsWithReportGuard(dailyCutoffDay)

            val toiletCutoff = now - CUTOFF_60_DAYS
            retentionRepository.cleanToiletRecordsWithReportGuard(toiletCutoff)

            val insightCutoff = now - CUTOFF_180_DAYS
            retentionRepository.cleanAiInsights(insightCutoff)

            retentionRepository.cleanTriggeredReminders()
            retentionRepository.cleanCompletedVaccineReminders()
            retentionRepository.rebuildFts()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
```

---

## 六、`DataRetentionRepository`

放置路徑：`core/data/src/main/kotlin/com/babymakisuk/coredata/repository/DataRetentionRepository.kt`

```kotlin
class DataRetentionRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val toiletRecordDao: ToiletRecordDao,
    private val aiInsightDao: AiInsightDao,
    private val systemReminderDao: SystemReminderDao,
    private val vaccineReminderDao: VaccineReminderDao,
    private val monthlyReportDao: MonthlyReportDao,
) {
    suspend fun cleanDailyLogsWithReportGuard(cutoffEpochDay: Long) =
        dailyLogDao.deleteOlderThanWithReportGuard(cutoffEpochDay)

    suspend fun cleanToiletRecordsWithReportGuard(cutoffMillis: Long) =
        toiletRecordDao.deleteOlderThanWithReportGuard(cutoffMillis)

    @Transaction
    suspend fun cleanRawDataForMonth(yearMonth: YearMonth) {
        val yearMonthStr = yearMonth.toString()
        dailyLogDao.deleteByYearMonth(yearMonthStr)
        toiletRecordDao.deleteByYearMonth(yearMonthStr)
    }

    suspend fun cleanAiInsights(cutoffMillis: Long) =
        aiInsightDao.deleteOlderThan(cutoffMillis)

    suspend fun cleanTriggeredReminders() =
        systemReminderDao.deleteTriggered()

    suspend fun cleanCompletedVaccineReminders() =
        vaccineReminderDao.deleteCompleted()

    suspend fun rebuildFts() =
        monthlyReportDao.rebuildFts()
}
```

---

## 七、DAO 需補充的清理方法

### `DailyLogDao`

```kotlin
@Query("DELETE FROM daily_log WHERE date < :cutoffEpochDay")
suspend fun deleteOlderThan(cutoffEpochDay: Long)

@Query("""
    DELETE FROM daily_log
    WHERE date < :cutoffEpochDay
    AND strftime('%Y-%m', date(date * 86400, 'unixepoch')) IN (
        SELECT yearMonth FROM monthly_report
    )
""")
suspend fun deleteOlderThanWithReportGuard(cutoffEpochDay: Long)

@Query("DELETE FROM daily_log WHERE strftime('%Y-%m', date(date * 86400, 'unixepoch')) = :yearMonth")
suspend fun deleteByYearMonth(yearMonth: String)
```

### `ToiletRecordDao`

```kotlin
@Query("DELETE FROM toilet_record WHERE timestamp < :cutoffMillis")
suspend fun deleteOlderThan(cutoffMillis: Long)

@Query("""
    DELETE FROM toilet_record
    WHERE timestamp < :cutoffMillis
    AND strftime('%Y-%m', datetime(timestamp / 1000, 'unixepoch')) IN (
        SELECT yearMonth FROM monthly_report
    )
""")
suspend fun deleteOlderThanWithReportGuard(cutoffMillis: Long)

@Query("DELETE FROM toilet_record WHERE strftime('%Y-%m', datetime(timestamp / 1000, 'unixepoch')) = :yearMonth")
suspend fun deleteByYearMonth(yearMonth: String)
```

### `AiInsightDao`

```kotlin
@Query("DELETE FROM ai_insight WHERE createdAt < :cutoffMillis")
suspend fun deleteOlderThan(cutoffMillis: Long)
```

### `SystemReminderDao`

```kotlin
@Query("DELETE FROM system_reminder WHERE isTriggered = 1")
suspend fun deleteTriggered()
```

### `VaccineReminderDao`

```kotlin
@Query("DELETE FROM vaccine_reminder WHERE isCompleted = 1")
suspend fun deleteCompleted()
```

### `MonthlyReportDao`

```kotlin
@Query("SELECT EXISTS(SELECT 1 FROM monthly_report WHERE yearMonth = :yearMonth)")
suspend fun existsForMonth(yearMonth: String): Boolean

@Query("INSERT INTO monthly_report_fts(monthly_report_fts) VALUES('rebuild')")
suspend fun rebuildFts()
```

> **效能備註**：若後續 `daily_log` 與 `toilet_record` 筆數大幅增加，建議在表中額外儲存 `yearMonth` 欄位並建立 index，以降低 `strftime()` 在大量資料上的掃描成本。

---

## 八、`BackupManager` 擴充：上傳 Drive

```kotlin
suspend fun exportAndUploadToDrive() = withContext(Dispatchers.IO) {
    val jsonString = exportToJson()
    val filename = "babymakisuk_backup_${YearMonth.now()}.json"
    driveRepository.uploadFile(
        folderPath = "BabyMakiSuk/backups",
        filename = filename,
        content = jsonString.toByteArray(Charsets.UTF_8),
        mimeType = "application/json"
    )
}

suspend fun uploadPrescriptionImage(
    childId: Long,
    visitId: Long,
    imageBytes: ByteArray
): String = withContext(Dispatchers.IO) {
    val timestamp = System.currentTimeMillis()
    val filename = "${visitId}_${timestamp}.jpg"
    driveRepository.uploadFile(
        folderPath = "BabyMakiSuk/$childId/prescriptions",
        filename = filename,
        content = imageBytes,
        mimeType = "image/jpeg"
    )
}
```

---

## 九、`PrescriptionImagePreprocessor` 使用規範

1. **壓縮至 800px 長邊、JPEG quality 85**，平均一張 ≤ 300KB
2. **清除 EXIF GPS 資訊**
3. **輸出至 `context.cacheDir/prescriptions/`**
4. **上傳完成後立即刪除 cacheDir 中的暫存檔**

```kotlin
val compressedBytes = prescriptionImagePreprocessor.process(uri)
val fileId = backupManager.uploadPrescriptionImage(childId, visitId, compressedBytes)
medicalVisitDao.updateImagePath(visitId, ImageStoragePath.Drive(fileId))
prescriptionImagePreprocessor.clearCache()
```

---

## 十、Firebase Firestore 配額保護規則

Firestore 免費方案限制：每天 50,000 次讀取、20,000 次寫入、20,000 次刪除。

### 寫入優化策略

- **批次寫入**：使用 `WriteBatch`，每次最多 500 筆
- **增量同步**：只同步 `lastModified > lastSyncTime` 的記錄
- **照片不走 Firestore**：只存 Drive fileId
- **`ChatMessageEntity` 不同步至 Firestore`**

### Firestore 文件路徑規範

```text
users/{uid}/
├── children/{childId}
├── growthRecords/{recordId}
├── medicalVisits/{visitId}
├── vaccineRecords/{recordId}
├── monthlyReports/{reportId}
└── memos/{memoId}
```

**不存入 Firestore 的資料**：
- `DailyLogEntity`
- `ToiletRecordEntity`
- `ChatMessageEntity`
- `SystemReminderEntity`
- `VaccineReminderEntity`
- `MonthlyReportFts`

---

## 十一、完整決策速查表

```text
每筆資料的生命週期決策樹：

是否為「不可再生的用戶原創資料」？
├─ 是 → 永久保留本地 + 同步 Firestore
│        是否含有二進位媒體（圖片）？
│        ├─ 是 → 照片額外上傳 Drive，DB 只存 ImageStoragePath.Drive(fileId)
│        └─ 否 → 只存 Firestore 即可
│
└─ 否 → 是否為「高頻產生的行為日誌」？
         ├─ 是 → 保留 N 天本地，月報生成後立即清理對應月份；
         │        WorkManager 作為第二層保護，只清理 2 個月以前且有月報的資料
         └─ 否 → 是否為「已完成／已觸發的提醒」？
                  ├─ 是 → 完成後立即清除
                  └─ 否 → 依業務邏輯決定（預設 6 個月）
```

---

## 十二、實作 Checklist

### Phase 1：DAO 清理方法（本週）
- [ ] `DailyLogDao.deleteOlderThan(epochDay)`
- [ ] `DailyLogDao.deleteOlderThanWithReportGuard(epochDay)`
- [ ] `DailyLogDao.deleteByYearMonth(yearMonth)`
- [ ] `ToiletRecordDao.deleteOlderThan(millis)`
- [ ] `ToiletRecordDao.deleteOlderThanWithReportGuard(millis)`
- [ ] `ToiletRecordDao.deleteByYearMonth(yearMonth)`
- [ ] `AiInsightDao.deleteOlderThan(millis)`
- [ ] `SystemReminderDao.deleteTriggered()`
- [ ] `VaccineReminderDao.deleteCompleted()`
- [ ] `MonthlyReportDao.existsForMonth(yearMonth)`
- [ ] `MonthlyReportDao.rebuildFts()`

### Phase 2：Repository 與 Worker（本週）
- [ ] 建立 `DataRetentionRepository.kt`
- [ ] 建立 `DataRetentionWorker.kt`
- [ ] 在 `DataModule` 中綁定 Hilt Worker Factory
- [ ] 在 `Application.onCreate()` 呼叫 `DataRetentionWorker.schedule()`

### Phase 3：月報生成與清理綁定（本週）
- [ ] `MonthlyReportViewModel.generateReport()` 成功後呼叫 `retentionRepository.cleanRawDataForMonth()`
- [ ] AI API 失敗時不執行任何清理
- [ ] 月報頁 UI 狀態機實作
- [ ] 書庫入口逾期未生成紅點角標邏輯

### Phase 4：照片上傳流程（下週）
- [ ] `ImageStoragePath` sealed class + Room TypeConverter 實作
- [ ] `BackupManager.uploadPrescriptionImage()` 實作
- [ ] `BackupManager.exportAndUploadToDrive()` 實作
- [ ] Drive 資料夾結構初始化邏輯
- [ ] `medicalVisitDao.updateImagePath(visitId, path: ImageStoragePath)` 更新
- [ ] `PrescriptionImagePreprocessor.clearCache()` 實作

### Phase 5：Firestore 增量同步（下下週）
- [ ] 各 Repository 的 `lastSyncTime` 追蹤機制
- [ ] `WriteBatch` 批次寫入封裝
- [ ] 離線佇列確認啟用

---

## 十三、注意事項

1. **Room `@Transaction` 保護**：`cleanRawDataForMonth()` 涉及多表，需包裝在 `@Transaction`。
2. **WorkManager 與 Hilt**：需引入 `androidx.hilt:hilt-work` 並正確註冊。
3. **Drive `appDataFolder` vs. 一般資料夾**：備份 JSON 建議 `appDataFolder`；照片是否對使用者可見可後續再定。
4. **圖片存取**：從 Drive 讀取時先下載到 `cacheDir`，再交給 Glide／Coil。
5. **GDPR／個資法**：刪帳號時，需同步刪除 Drive、Firestore 與本地 DB。
6. **AI API 失敗策略**：月報摘要失敗時不清理任何資料；Chat 摘要失敗時保留舊訊息，下次再試。
