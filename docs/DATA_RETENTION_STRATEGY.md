# BabyMakiSuk 資料留存、儲存架構與同步策略

> **文件版本**: v2.1
> **適用模組**: `core/data`, `core/firebase`, `core/drive`
> **目標讀者**: AI Agents、開發者
> **最後更新**: 2026-05-18
> **整合來源**: `DATA_RETENTION_STRATEGY.md` v1.2 + `SYNC_ARCHITECTURE.md` v1.0
> **實作狀態**: Phase 1-3 ✅ 完成（Phase E / F ⏸ 暫緩）

---

## 總覽

本文件整合 BabyMakiSuk 的三層儲存架構、雙機角色模型、資料留存與清理策略。

```
┌─────────────────────────────────────────────────────────┐
│  L1：本機 Room DB + 圖片快取                              │
│  - 兩台手機各自維護本機 Room DB                           │
│  - 圖片下載至 App 私有目錄快取                            │
│  - FTS5 全文索引，支援離線搜尋                            │
└──────────────────┬──────────────────────────────────────┘
                   │ 雙向同步（增量，lastModified > lastSyncTime）
┌──────────────────▼──────────────────────────────────────┐
│  L2：Firebase（即時同步層）                               │
│  - Firestore：結構化文字資料                              │
│  - Firebase Storage：就診照片（壓縮後 < 200KB/張）        │
│  - Security Rules：data_manager / ai_operator 角色隔離   │
└──────────────────┬──────────────────────────────────────┘
                   │ 月報產出時觸發匯出
┌──────────────────▼──────────────────────────────────────┐
│  L3：Google Drive（永久主庫）                             │
│  - 月度備份 JSON（appDataFolder）                        │
│  - 月報 Markdown 文件                                    │
└─────────────────────────────────────────────────────────┘
```

**核心原則：先備份、後清除。** 任何本地清理動作必須確認已同步至對應雲端層次後才執行。
Worker 備份失敗時，**必須中止所有清除操作並回傳 `Result.retry()`**。

---

## 一、雙機角色模型

### 1.1 角色定義

| 角色 | 裝置條件 | Firebase Role | 主要職責 |
|------|---------|---------------|--------|
| **資料管理員** | 低階手機 | `data_manager` | 輸入所有孩童資訊（生長、就診、疫苗、日誌） |
| **AI 操作員** | 高階手機 | `ai_operator` | 執行 AI 分析、產生月報 |

### 1.2 設計原則

- **資料隔離**：AI 側對 Child 原始資料唯讀，只能寫入 AI 結果子集合，確保同一資料只有單方面輸入，避免衝突
- **離線優先**：兩機均可離線操作，上線後自動同步
- **長期保存**：Firebase 為即時同步媒介，Google Drive 為永久備份主庫

### 1.3 Firebase Security Rules

```javascript
// firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isDataManager() {
      return request.auth.token.role == "data_manager";
    }
    function isAiOperator() {
      return request.auth.token.role == "ai_operator";
    }
    function isAuthenticated() {
      return request.auth != null;
    }

    // Child 主資料：data_manager 讀寫，ai_operator 唯讀
    match /children/{childId} {
      allow read: if isAuthenticated();
      allow write: if isDataManager();
    }

    // 就診記錄：data_manager 讀寫；ai_operator 只可更新 aiPending 旗標
    match /children/{childId}/medicalVisits/{visitId} {
      allow read: if isAuthenticated();
      allow create, update: if isDataManager()
        || (isAiOperator()
            && request.resource.data.keys().hasOnly(["aiPending"]));
    }

    // 日誌：data_manager 讀寫；ai_operator 只可附加 aiSummary
    match /children/{childId}/dailyLogs/{logId} {
      allow read: if isAuthenticated();
      allow create, update: if isDataManager()
        || (isAiOperator()
            && request.resource.data.keys().hasOnly(["aiSummary"]));
    }

    // AI 結果：ai_operator 專屬寫入，data_manager 唯讀
    match /children/{childId}/aiResults/{resultId} {
      allow read: if isAuthenticated();
      allow write: if isAiOperator();
    }

    // 月報：ai_operator 寫入
    match /monthlyReports/{childId}/{reportId} {
      allow read: if isAuthenticated();
      allow write: if isAiOperator();
    }
  }
}
```

---

## 二、各 Entity 留存決策表

### 永久留存（不可刪除）

| Entity | 資料性質 | 雲端目標 | 備註 |
|--------|---------|---------|------|
| `ChildProfileEntity` | 孩童基本資料 | Firestore `children/{childId}` | 所有功能錨點，永遠不清除 |
| `GrowthRecordEntity` | 身高/體重/頭圍曲線 | Firestore `growthRecords` | 醫療追蹤依據，永遠不清除 |
| `VaccineRecordEntity` | 疫苗接種史 | Firestore `vaccineRecords` | 法定健康紀錄，永遠不清除 |
| `MedicalVisitEntity`（文字欄位） | 就診/診斷/醫囑文字 | Firestore `medicalVisits` | 永久保存文字欄位 |
| `MedicalVisitEntity`（照片） | 處方箋/X光圖片 | **Firebase Storage** `children/{childId}/medical/{visitId}.jpg` | 壓縮後 < 200KB；每年底滾動清除 |
| `MonthlyReportEntity` | 月度 AI 摘要 | Firestore `monthlyReports` + Drive JSON | 每月一筆，體積極小且價值高 |
| `MemoEntity` | 用戶手動備忘 | Firestore `memos` | 用戶主動輸入，不可自動刪除 |

### 定時清理（本地）

| Entity | 本地保留期限 | 清理觸發條件 | 清理後動作 |
|--------|------------|------------|---------|
| `DailyLogEntity` | 最近 **90 天** | 月報生成後立即清理對應月份；WorkManager 只清理「**2 個月以前**且已有對應 `MonthlyReport`」的資料 | 先確認 Firestore 已同步，再刪本地明細 |
| `ToiletRecordEntity` | 最近 **60 天** | 同上，月報生成後立即清理；WorkManager 加月報存在防護 | 直接刪除（已彙整至 `MonthlyReport`） |
| `ChatMessageEntity` | 最近 **30 筆/對話** | 每次啟動 App 時觸發；摘要 AI 呼叫失敗時**保留舊訊息，不刪除** | 舊訊息壓縮摘要存入 `AiInsightEntity` 後刪除；摘要失敗則跳過本次清理 |
| `AiInsightEntity` | 最近 **6 個月** | 每月 WorkManager 定時任務 | 摘要已在 `MonthlyReportEntity`，超齡可刪 |
| `SystemReminderEntity` | **已觸發後立即** | 提醒完成 callback | 無保存價值，立即清除 |
| `VaccineReminderEntity` | **接種完成後立即** | 接種操作完成 callback | 接種記錄已在 `VaccineRecordEntity` 中保存 |
| `MonthlyReportFts` | 跟隨 `MonthlyReportEntity` | FTS rebuild 觸發 | 虛擬索引表，定期 `REBUILD` 即可 |

### Firebase Storage 照片滾動清除

- **清除週期**：每年底（12 月 31 日前後）由 WorkManager 觸發
- **清除規則**：刪除 `uploadedAt` 早於當年 1 月 1 日的所有照片（滾動式，不備份至 Drive）
- **前提**：照片對應的 `MedicalVisitEntity` 文字欄位已永久保存於 Firestore，照片刪除後不影響就診紀錄完整性

```kotlin
// StorageCleanupWorker（每年執行一次）
val cutoffMillis = LocalDate.now().withDayOfYear(1).toEpochDay() * 86400 * 1000
storageRepository.deletePhotosBefore(cutoffMillis)
```

---

## 三、Firestore 資料結構設計

### 3.1 集合層級

```
Firestore
└── users/{uid}/
    ├── children/{childId}                   ← data_manager 讀寫
    │   ├── name, birthDate, gender ...
    │   ├── medicalVisits/{visitId}           ← data_manager 讀寫
    │   │   ├── date, hospital, diagnosis, prescription, notes
    │   │   ├── imageStoragePath             ← 只存 Storage 路徑字串
    │   │   └── aiPending: Boolean           ← AI 觸發旗標
    │   ├── growthRecords/{recordId}         ← data_manager 讀寫
    │   ├── dailyLogs/{logId}               ← data_manager 讀寫
    │   │   └── aiSummary                   ← 唯此欄位 ai_operator 可寫
    │   └── aiResults/{resultId}            ← ai_operator 專屬寫入
    │       ├── type: "medicalSummary" | "monthlyReport"
    │       ├── refId
    │       └── createdAt
    ├── growthRecords/{recordId}
    ├── vaccineRecords/{recordId}
    ├── monthlyReports/{reportId}            ← ai_operator 寫入
    │   ├── yearMonth: "2026-05"
    │   ├── aiSummary
    │   ├── searchKeywords: [...]
    │   └── driveFileId: String?
    └── memos/{memoId}
```

**不存入 Firestore 的資料**：
- `DailyLogEntity`（量大，依賴 `MonthlyReport` 摘要即可）
- `ToiletRecordEntity`（量大，已彙整至 `MonthlyReport`）
- `ChatMessageEntity`（純本地 AI 對話上下文）
- `SystemReminderEntity`（時效性提醒，無雲端同步必要）
- `VaccineReminderEntity`（完成後立即刪除）
- `MonthlyReportFts`（FTS 虛擬表，本地索引）

### 3.2 Firestore 配額保護

Firestore 免費方案限制：每天 50,000 次讀取、20,000 次寫入、20,000 次刪除。

- **批次寫入**：使用 `WriteBatch`，每次最多 500 筆
- **增量同步**：只同步 `lastModified > lastSyncTime` 的記錄
- **照片不走 Firestore**：只存 Firebase Storage 路徑字串
- **離線佇列**：Firestore 離線持久化已內建支援，確認啟用即可

---

## 四、照片儲存流程

### 4.1 上傳流程

```text
使用者拍照／選圖
        │
        ▼
  [本地暫存 Cache]
  context.cacheDir/prescriptions/
        │
        ▼
  PrescriptionImagePreprocessor
  壓縮至 < 200KB + 裁切 + EXIF 清除
        │
        ▼
  上傳至 Firebase Storage
  路徑: children/{childId}/medical/{visitId}.jpg
        │
        ▼
  取得 Storage 路徑
        │
        ├─► 更新 MedicalVisitEntity.imageStoragePath = ImageStoragePath.FirebaseStorage(path)
        │
        ├─► AI OCR 解析（若 aiPending = true）
        │         │
        │         ▼
        │   更新 MedicalVisitEntity（ai_operator 寫入 aiResults）
        │   aiPending = false
        │
        └─► 刪除本地暫存 (cacheDir 中的檔案)
```

### 4.2 `ImageStoragePath` 型別安全封裝

```kotlin
sealed class ImageStoragePath {
    data class Local(val absolutePath: String) : ImageStoragePath()
    data class FirebaseStorage(val storagePath: String) : ImageStoragePath()
    data object None : ImageStoragePath()
}

class ImageStoragePathConverter {
    @TypeConverter
    fun fromPath(value: String?): ImageStoragePath = when {
        value == null -> ImageStoragePath.None
        value.startsWith("local:") -> ImageStoragePath.Local(value.removePrefix("local:"))
        value.startsWith("firebase:") -> ImageStoragePath.FirebaseStorage(value.removePrefix("firebase:"))
        else -> ImageStoragePath.None
    }

    @TypeConverter
    fun toPath(path: ImageStoragePath): String? = when (path) {
        is ImageStoragePath.Local -> "local:${path.absolutePath}"
        is ImageStoragePath.FirebaseStorage -> "firebase:${path.storagePath}"
        ImageStoragePath.None -> null
    }
}
```

### 4.3 圖片壓縮規則

| 類型 | 目標大小 | 說明 |
|------|---------|------|
| 就診單照片 | < 200KB | JPEG，品質自動降質至符合 |
| 成長記錄照片（未來） | < 150KB | 縮圖優先 |

### 4.4 本機圖片快取路徑

```text
/data/data/{packageName}/files/
└── medical_images/
    └── {visitId}.jpg     ← 從 Firebase Storage 下載後快取（Glide/Coil 管理）
```

### 4.5 Firebase Storage 空間估算

- 200KB × 每週 2 次就診 × 52 週 = 約 **20MB / 年**
- 免費額度 5GB，**10 年以上不超標**
- 每年底滾動清除當年以前的照片（直接刪除，不備份）

---

## 五、月報生成流程與清理綁定

### 5.1 月報觸發入口

- **通知**：每月底由 WorkManager 推送首頁通知提醒使用者生成月報
- **操作位置**：使用者進入「書庫」→「月報頁」手動點擊生成
- **執行角色**：`ai_operator` 手機
- **網路需求**：月報生成**需要網路**（呼叫 AI API 產生摘要文字）

### 5.2 完整生成流程

```text
1. ai_operator 偵測到 aiPending: true 的 medicalVisit
   └─► 呼叫 ServiceAI.summarizeMedicalNote()
       └─► AI 結果寫入 aiResults 子集合，aiPending = false

2. ai_operator 手動觸發「生成本月月報」
   ├─► 讀取本月所有 DailyLog / MedicalVisit / GrowthRecord
   ├─► ServiceAI.summarizeMonthlyLog() → aiSummary 文字
   ├─► AI 萃取 searchKeywords（發燒、藥名、診斷等）
   ├─► 寫入 Firestore monthlyReports/{reportId}
   └─► 上傳月報 JSON 至 Google Drive BabyMakiSuk/backups/

3. 月報成功後立即清理對應月份原始資料
   ├─► DataRetentionRepository.cleanRawDataForMonth(yearMonth)
   └─► data_manager 手機收到 Firestore 更新 → 顯示月報卡片
```

### 5.3 月報頁 UI 狀態機

| 狀態 | 顯示內容 | 可操作項目 |
|------|---------|-----------|
| 本月資料充足、未生成 | 「本月有 X 筆日誌，點擊生成月報」 | 生成按鈕（主要 CTA） |
| 生成中 | Loading + 進度說明 | 不可中斷 |
| 已生成 | 顯示 AI 摘要內容 | 查看 / 分享 / 匯出 |
| 本月資料不足（< 3 筆） | 提示「記錄太少，建議繼續記錄」 | 可強制生成或略過 |
| 無網路 | 提示「需要網路連線才能生成摘要」 | 可稍後再試 |

### 5.4 失敗與逾期保護

```kotlin
// MonthlyReportViewModel
fun generateReport(yearMonth: YearMonth) {
    viewModelScope.launch {
        val result = runCatching { reportRepository.generate(yearMonth) }
        if (result.isSuccess) {
            // 月報生成與清理原子性綁定
            retentionRepository.cleanRawDataForMonth(yearMonth)
        } else {
            // AI API 失敗 → 不清理任何資料
            _uiState.update { it.copy(error = ReportError.GenerationFailed) }
        }
    }
}
```

**三層逾期保護：**
1. **WorkManager**：只清理「2 個月以前且已有對應 `MonthlyReport`」的資料
2. **月報頁**：月報成功後立即清理對應月份原始資料
3. **UI 角標**：月底通知發出後超過 7 天未生成，書庫入口顯示紅點提醒

---

## 六、Google Drive 資料夾結構

```text
Google Drive (appDataFolder — 對用戶不可見)
└── BabyMakiSuk/
    ├── backups/
    │   ├── babymakisuk_backup_2026-05.json  ← 月度 JSON 備份
    │   └── babymakisuk_backup_2026-04.json
    └── reports/
        ├── 2026-05_report.md               ← 月報 Markdown（人類可讀）
        └── 2026-04_report.md
```

---

## 七、欄位型別規範

| 欄位 | 建議型別 | 儲存格式 | 備註 |
|------|----------|----------|------|
| `DailyLogEntity.date` | `Long` | epoch day | `LocalDate.now().toEpochDay()` |
| `ToiletRecordEntity.timestamp` | `Long` | epoch millis | `System.currentTimeMillis()` |
| `AiInsightEntity.createdAt` | `Long` | epoch millis | 建立時間 |
| `MonthlyReportEntity.yearMonth` | `String` | `YYYY-MM` | 例如 `2026-05` |
| `MedicalVisitEntity.uploadedAt` | `Long` | epoch millis | 照片上傳時間，供年度清除使用 |
| `lastSyncTime` | `Long` | epoch millis | 同步基準時間 |

> **規則**：「日期」概念用 epoch day；「時間點」概念用 epoch millis；「月份聚合鍵」統一用 `YYYY-MM` 字串。

---

## 八、`DataRetentionWorker` 實作

放置路徑：`core/data/src/main/kotlin/com/babymakisuk/coredata/worker/DataRetentionWorker.kt`

> **實作狀態**：✅ 已完成。與規格差異：`backupManager` 為 Phase F 項目，目前以 `// TODO [Phase F]` 跳過。

```kotlin
@HiltWorker
class DataRetentionWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val retentionRepository: DataRetentionRepository,
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
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // TODO [Phase F] 加入 backupManager.exportAndUploadToDrive() 前置檢查
            // 目前無 BackupManager，先跳過備份步驟

            val dailyCutoffDay = LocalDate.now().minusDays(90).toEpochDay()
            retentionRepository.cleanDailyLogsWithReportGuard(dailyCutoffDay)

            val now = System.currentTimeMillis()
            retentionRepository.cleanToiletRecordsWithReportGuard(now - CUTOFF_60_DAYS)
            retentionRepository.cleanAiInsights(now - CUTOFF_180_DAYS)
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

## 九、`DataRetentionRepository`

放置路徑：`core/data/src/main/kotlin/com/babymakisuk/coredata/repository/DataRetentionRepository.kt`

> **實作狀態**：✅ 已完成。與規格差異：`cleanDailyLogsWithReportGuard()` 內部將 `Long epochDay` 轉為 `LocalDate.toString()` 後傳入 DAO；`cleanToiletRecordsWithReportGuard()` 使用 `toiletDao`（DAO 實際命名）。

```kotlin
@Singleton
class DataRetentionRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val toiletDao: ToiletDao,
    private val aiInsightDao: AiInsightDao,
    private val systemReminderDao: SystemReminderDao,
    private val vaccineReminderDao: VaccineReminderDao,
    private val monthlyReportDao: MonthlyReportDao,
) {
    suspend fun cleanDailyLogsWithReportGuard(cutoffEpochDay: Long) {
        val cutoffDate = LocalDate.ofEpochDay(cutoffEpochDay).toString()
        dailyLogDao.deleteOlderThanWithReportGuard(cutoffDate)
    }

    suspend fun cleanToiletRecordsWithReportGuard(cutoffMillis: Long) =
        toiletDao.deleteOlderThanWithReportGuard(cutoffMillis)

    @Transaction
    suspend fun cleanRawDataForMonth(yearMonth: YearMonth) {
        val yearMonthStr = yearMonth.toString()
        dailyLogDao.deleteByYearMonth(yearMonthStr)
        toiletDao.deleteByYearMonth(yearMonthStr)
    }

    suspend fun cleanAiInsights(cutoffMillis: Long) = aiInsightDao.deleteOlderThan(cutoffMillis)
    suspend fun cleanTriggeredReminders() = systemReminderDao.deleteTriggered()
    suspend fun cleanCompletedVaccineReminders() = vaccineReminderDao.deleteCompleted()
    suspend fun rebuildFts() = monthlyReportDao.rebuildFts()
}
```

---

## 十、DAO 已實作的清理方法

> **實作狀態**：✅ 已完成。以下程式碼為實際實作，已依資料庫實際欄位型別與名稱調整。

### `DailyLogDao`

`date` 欄位型別為 `LocalDate`，Room 以 TEXT `YYYY-MM-DD` 儲存，故使用 `substr()` 而非 epoch 轉換。

```kotlin
@Query("DELETE FROM daily_log WHERE date < :cutoffDate")
suspend fun deleteOlderThan(cutoffDate: String)

@Query("""
    DELETE FROM daily_log
    WHERE date < :cutoffDate
    AND substr(date, 1, 7) IN (
        SELECT DISTINCT substr(month_start, 1, 7) FROM monthly_reports
    )
""")
suspend fun deleteOlderThanWithReportGuard(cutoffDate: String)

@Query("DELETE FROM daily_log WHERE substr(date, 1, 7) = :yearMonth")
suspend fun deleteByYearMonth(yearMonth: String)
```

### `ToiletDao`

`timestamp` 為 epoch millis（`Long`）。實際 DAO 類別名稱為 `ToiletDao`，表格名稱為 `toilet_records`。

```kotlin
@Query("DELETE FROM toilet_records WHERE timestamp < :cutoffMillis")
suspend fun deleteOlderThan(cutoffMillis: Long)

@Query("""
    DELETE FROM toilet_records
    WHERE timestamp < :cutoffMillis
    AND strftime('%Y-%m', timestamp / 1000, 'unixepoch') IN (
        SELECT DISTINCT substr(month_start, 1, 7) FROM monthly_reports
    )
""")
suspend fun deleteOlderThanWithReportGuard(cutoffMillis: Long)

@Query("DELETE FROM toilet_records WHERE strftime('%Y-%m', timestamp / 1000, 'unixepoch') = :yearMonth")
suspend fun deleteByYearMonth(yearMonth: String)
```

### `AiInsightDao`

表格名稱為 `ai_insights`（複數），`createdAt` 為 epoch millis。

```kotlin
@Query("DELETE FROM ai_insights WHERE createdAt < :cutoffMillis")
suspend fun deleteOlderThan(cutoffMillis: Long)
```

### `SystemReminderDao`

實際無 `isTriggered` 欄位，改用 `resolvedAt: Long?`（`null` = 未觸發）。故刪除條件為 `resolvedAt IS NOT NULL`。

```kotlin
@Query("DELETE FROM system_reminders WHERE resolvedAt IS NOT NULL")
suspend fun deleteTriggered()
```

### `VaccineReminderDao`

表格名稱為 `vaccine_reminders`，`isCompleted` 為 `Boolean`（SQLite 存為 0/1）。

```kotlin
@Query("DELETE FROM vaccine_reminders WHERE isCompleted = 1")
suspend fun deleteCompleted()
```

### `MonthlyReportDao`

表格名稱為 `monthly_reports`，月份由 `month_start`（文字 `YYYY-MM-DD`）前 7 字元萃取。無獨立 `yearMonth` 欄位。

```kotlin
@Query("SELECT EXISTS(SELECT 1 FROM monthly_reports WHERE substr(month_start, 1, 7) = :yearMonth)")
suspend fun existsForMonth(yearMonth: String): Boolean

@Query("INSERT INTO monthly_reports_fts(monthly_reports_fts) VALUES('rebuild')")
suspend fun rebuildFts()
```

> **效能備註**：若 `daily_log` 與 `toilet_records` 筆數大幅增加，建議於 `daily_log(date)` 與 `toilet_records(timestamp)` 建立 index，並考慮額外儲存 `yearMonth` 衍生欄位以降低 `substr()` / `strftime()` 掃描成本。

---

## 十一、Room DB 模組規劃（留位）

### 11.1 模組結構

```
core/
├── model/        ← 已存在，需新增 MonthlyReport domain model
├── data/         ← 已存在，需新增 MonthlyReportEntity + FTS5
├── ai/           ← 已存在
├── firebase/     ← 待新增：FirestoreRepository, ImageUploadRepository
└── drive/        ← 待新增：DriveExportRepository
```

### 11.2 `MonthlyReportEntity`（Room）

```kotlin
@Entity(tableName = "monthly_report")
data class MonthlyReportEntity(
    @PrimaryKey val id: String,           // "{childId}_{YYYY-MM}"
    val childId: String,
    val yearMonth: String,                // "2026-05"
    val aiSummary: String,
    val searchKeywords: String,           // 逗號分隔，FTS5 索引用
    val driveFileId: String?,
    val syncedAt: Long
)

@Fts5(contentEntity = MonthlyReportEntity::class)
@Entity(tableName = "monthly_report_fts")
data class MonthlyReportFts(
    val aiSummary: String,
    val searchKeywords: String
)
```

### 11.3 搜尋 DAO 介面

```kotlin
@Dao
interface MonthlyReportDao {
    @Query("""
        SELECT mr.* FROM monthly_report mr
        INNER JOIN monthly_report_fts fts ON mr.rowid = fts.rowid
        WHERE monthly_report_fts MATCH :keyword
        AND mr.childId = :childId
        ORDER BY mr.yearMonth DESC
    """)
    fun searchByKeyword(childId: String, keyword: String): Flow<List<MonthlyReportEntity>>

    @Query("SELECT * FROM monthly_report WHERE childId = :childId ORDER BY yearMonth DESC")
    fun getAll(childId: String): Flow<List<MonthlyReportEntity>>

    @Upsert
    suspend fun upsert(report: MonthlyReportEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM monthly_report WHERE yearMonth = :yearMonth)")
    suspend fun existsForMonth(yearMonth: String): Boolean

    @Query("INSERT INTO monthly_report_fts(monthly_report_fts) VALUES('rebuild')")
    suspend fun rebuildFts()
}
```

---

## 十二、完整決策速查表

```text
每筆資料的生命週期決策樹：

是否為「不可再生的用戶原創資料」？
├─ 是 → 永久保留本地 + 同步 Firestore
│        是否含有二進位媒體（圖片）？
│        ├─ 是 → 上傳 Firebase Storage，DB 只存 ImageStoragePath.FirebaseStorage(path)
│        │        每年底滾動清除 Storage 中的照片（直接刪除）
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

## 十三、實作 Checklist

### Phase 1：DAO 清理方法 ✅ 已完成
- [x] `DailyLogDao.deleteOlderThan(cutoffDate: String)` — LocalDate TEXT 型別，使用 `<` 比較 ISO 字串
- [x] `DailyLogDao.deleteOlderThanWithReportGuard(cutoffDate: String)` — 子查詢 `substr(date,1,7) IN (SELECT substr(month_start,1,7) FROM monthly_reports)`
- [x] `DailyLogDao.deleteByYearMonth(yearMonth: String)` — `substr(date,1,7) = :yearMonth`
- [x] `ToiletDao.deleteOlderThan(millis: Long)` — 注意實際 DAO 名稱為 `ToiletDao` 非 `ToiletRecordDao`
- [x] `ToiletDao.deleteOlderThanWithReportGuard(millis: Long)` — `strftime` epoch millis 轉換
- [x] `ToiletDao.deleteByYearMonth(yearMonth: String)`
- [x] `AiInsightDao.deleteOlderThan(millis: Long)`
- [x] `SystemReminderDao.deleteTriggered()` — 實際無 `isTriggered` 欄位，改用 `resolvedAt IS NOT NULL`
- [x] `VaccineReminderDao.deleteCompleted()` — `isCompleted = 1`
- [x] `MonthlyReportDao.existsForMonth(yearMonth: String)` — 比對 `substr(month_start,1,7)`
- [x] `MonthlyReportDao.rebuildFts()`

### Phase 2：Repository 與 Worker ✅ 已完成
- [x] 建立 `DataRetentionRepository.kt`（`@Singleton` + `@Inject`，6 個清理方法）
- [x] 建立 `DataRetentionWorker.kt`（`@HiltWorker`，7 天週期，UNMETERED + 充電）
- [x] 引入 `androidx.hilt:hilt-work:1.2.0` 至 `core/data` 與 `app` 模組
- [x] `BabyMakiSukApplication` 實作 `Configuration.Provider` + 注入 `HiltWorkerFactory`
- [x] 在 `Application.onCreate()` 呼叫 `DataRetentionWorker.schedule(this)`

### Phase 3：月報生成與清理綁定 ✅ 已完成
- [x] `MonthlyReportViewModel.generateReport()` 成功後呼叫 `retentionRepository.cleanRawDataForMonth(yearMonth)`
- [x] AI API 失敗時不執行任何清理（`runCatching { ... }.onSuccess { clean }.onFailure { setError }`）
- [x] 月報頁 UI 狀態機 — 新增 `sealed interface ReportGenerationState`（Idle / Generating / NoNetwork / Error）
- [x] 書庫入口逾期未生成紅點角標 — `LibraryViewModel.showMonthlyReportBadge` + `Badge` composable

> **實作註記**：
> - `DailyLog.date` 為 `LocalDate`，Room 存為 TEXT `YYYY-MM-DD`，SQL 使用 `substr(date,1,7)` 萃取月份，非規格中 epoch day 算法。
> - `SystemReminderEntity` 使用 `resolvedAt: Long?`（null=未觸發）而非 `isTriggered` 欄位，`deleteTriggered()` 條件為 `resolvedAt IS NOT NULL`。
> - `DataRetentionWorker` 中的 `backupManager.exportAndUploadToDrive()` 為 Phase F 項目，目前以 `// TODO [Phase F]` 標記暫跳過。
> - `MonthlyReportDao.existsForMonth()` 比對 `month_start` 欄位前 7 字元，適用於實際資料格式 `"2026-05-01"`。

### Phase E：Firebase 同步（Phase E）
- [ ] 建立 `core/firebase` 模組
- [ ] `FirestoreChildRepository`（離線持久化啟用）
- [ ] `FirestoreMedicalRepository`（`aiPending` 旗標監聽）
- [ ] `ImageUploadRepository`（壓縮 + Storage 上傳）
- [ ] `MedicalImageCacheManager`（本機快取管理）
- [ ] `StorageCleanupWorker`（每年底滾動清除照片）
- [ ] Firebase Auth Custom Claims 設定（`data_manager` / `ai_operator`）
- [ ] Firestore Security Rules 部署（見第一章）
- [ ] `feature/settings`：Google 登入 UI
- [ ] `ImageStoragePath` sealed class + Room TypeConverter 實作

### Phase F：月報 + Drive 匯出（Phase F）
- [ ] 建立 `core/drive` 模組
- [ ] `MonthlyReportEntity` + `MonthlyReportFts`（FTS5）
- [ ] `MonthlyReportDao`（含 FTS5 全文搜尋）
- [ ] `DriveExportRepository`（JSON 備份 + Markdown 月報上傳）
- [ ] `BackupManager.exportAndUploadToDrive()` 實作
- [ ] `feature/monthlyreport`：`MonthlyReportScreen` UI
- [ ] `core/ai`：月報摘要 prompt schema（`searchKeywords` 萃取）

---

## 十四、非功能需求

| 項目 | 規格 |
|------|------|
| 離線可用 | Room 本機 DB + Firestore 離線持久化，兩機均可無網路操作 |
| 圖片壓縮上限 | 200KB / 張，超過自動降質 |
| Storage 免費額度 | 5GB（Blaze Plan），預估 10 年以上不超標 |
| Firestore 免費讀寫 | 50K 讀 / 20K 寫 / 日，家庭用量充裕 |
| 搜尋響應時間 | Room FTS5 本機搜尋 < 100ms |
| Drive 匯出格式 | Markdown（人類可讀）+ JSON（程式可重新匯入） |
| 資料最小化 | Firestore 文件不含圖片 Binary，只存 Storage 路徑 |

---

## 十五、注意事項

1. **Room `@Transaction` 保護**：`cleanRawDataForMonth()` 涉及多表，需包裝在 `@Transaction`。
2. **WorkManager 與 Hilt**：需引入 `androidx.hilt:hilt-work` 並在 `HiltWorkerFactory` 中正確註冊。
3. **Firebase Auth Custom Claims**：`data_manager` / `ai_operator` 角色需在後端設定，Security Rules 依賴此機制。
4. **圖片存取**：從 Firebase Storage 讀取時先下載到 `cacheDir`，再交給 Glide／Coil，避免每次重新下載。
5. **GDPR／個資法**：刪帳號時，需同步刪除 Firebase Storage 所有照片、Firestore 所有文件與本地 Room DB。
6. **AI API 失敗策略**：月報摘要失敗時不清理任何資料；Chat 摘要失敗時保留舊訊息，下次啟動 App 再試。
7. **`appDataFolder` vs. 一般資料夾**：備份 JSON 使用 `appDataFolder`（用戶不可見）；若未來月報 Markdown 需讓用戶自行存取，可改為一般資料夾並調整 DriveRepository API scope。
