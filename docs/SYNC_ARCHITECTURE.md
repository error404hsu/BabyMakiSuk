# BabyMakiSuk 雙機協作與儲存同步架構計劃書

> 版本：v1.0  
> 更新日期：2026-05-11  
> 狀態：規劃中，預計 Phase E / F 實作

---

## 1. 背景與目標

### 1.1 雙人協作情境

| 角色 | 裝置條件 | Firebase Role | 主要職責 |
|------|---------|---------------|--------|
| **資料管理員** | 低階手機 | `data_manager` | 輸入小孩所有資訊（生長、就診、疫苗、日誌） |
| **AI 操作員** | 高階手機 | `ai_operator` | 執行 AI 分析（本地 LLM + Cloud API）、產生週報 |

### 1.2 核心設計原則

- **資料隔離**：AI 側對 Child 原始資料唯讀，只能寫入 AI 結果子集合
- **離線優先**：兩機均可離線操作，上線後自動同步
- **長期保存**：Firebase 為暫存與即時同步媒介，Google Drive 為永久主庫
- **可搜尋**：本機 Room FTS4 支援離線全文搜尋就醫紀錄

---

## 2. 整體儲存架構（三層）

```
┌─────────────────────────────────────────────────────────┐
│  Layer 1：本機 Room DB + 圖片快取                         │
│  - 兩台手機各自維護本機 Room DB                           │
│  - 圖片下載至 App 私有目錄快取                            │
│  - FTS4 全文索引，支援離線搜尋                            │
└──────────────────┬──────────────────────────────────────┘
                   │ 雙向同步
┌──────────────────▼──────────────────────────────────────┐
│  Layer 2：Firebase（暫存 + 即時同步）                     │
│  - Firestore：結構化資料（Child、MedicalVisit、週報）     │
│  - Firebase Storage：就診照片（壓縮後 < 200KB/張）        │
│  - Security Rules：角色權限隔離                          │
└──────────────────┬──────────────────────────────────────┘
                   │ 週報產出時觸發匯出
┌──────────────────▼──────────────────────────────────────┐
│  Layer 3：Google Drive（永久主庫）                        │
│  - 每週週報 Markdown 文件                                │
│  - 就診照片索引清單（JSON）                               │
│  - 全年資料 weekly_reports.json（可重新匯入）             │
└─────────────────────────────────────────────────────────┘
```

---

## 3. Firestore 資料結構設計

### 3.1 集合層級

```
Firestore
├── children/{childId}                          ← data_manager 讀寫
│   ├── name, birthDate, gender ...
│   ├── medicalVisits/{visitId}                 ← data_manager 讀寫
│   │   ├── date, hospital, department
│   │   ├── diagnosis, prescription, notes
│   │   ├── imageStoragePath                    ← 只存 Storage 路徑
│   │   └── aiPending: Boolean                  ← AI 觸發旗標
│   ├── growthRecords/{recordId}               ← data_manager 讀寫
│   ├── dailyLogs/{logId}                      ← data_manager 讀寫
│   │   └── aiSummary (可由 ai_operator 附加)  ← 唯獨此欄位 AI 可寫
│   └── aiResults/{resultId}                   ← ai_operator 專屬寫入
│       ├── type: "medicalSummary" | "weeklyReport"
│       ├── refId: visitId or weekId
│       ├── diagnosisSummary, prescriptions, careInstructions
│       └── createdAt
│
└── weeklyReports/{childId}/{year}-W{week}     ← 一週一筆
    ├── weekStart, weekEnd
    ├── aiSummary                              ← AI 產生的週報文字
    ├── medicalVisitIds: [...]                 ← 引用 visitId 清單
    ├── growthSnapshot: { weight, height, headCirc }
    ├── vaccineDue: ["MMR 第二劑"]
    ├── searchKeywords: ["發燒", "上呼吸道", "阿莫西林"]  ← AI 萃取
    ├── driveExported: Boolean
    └── driveFileId: String?
```

### 3.2 週資料聚合規則

- **週定義**：ISO 8601，週一為第一天（`WeekFields.ISO`）
- **一年上限**：52 或 53 筆（閏週年）
- **聚合時機**：每週報產出時（由 AI 操作員手機觸發），不即時聚合
- **歷史資料**：`medicalVisits` 原始資料永久保留，週報為摘要索引層

---

## 4. Firebase Security Rules

```javascript
// firestore.rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // 工具函式
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

    // 就診 / 生長 / 日誌：data_manager 讀寫
    // ai_operator 只能更新 aiPending 旗標與 dailyLog.aiSummary
    match /children/{childId}/medicalVisits/{visitId} {
      allow read: if isAuthenticated();
      allow create, update: if isDataManager()
        || (isAiOperator()
            && request.resource.data.keys().hasOnly(["aiPending"]));
    }

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

    // 週報：ai_operator 寫入（資料管理員觸發後由 AI 側產生）
    match /weeklyReports/{childId}/{weekId} {
      allow read: if isAuthenticated();
      allow write: if isAiOperator();
    }
  }
}
```

---

## 5. 圖片儲存策略

### 5.1 Firebase Storage 路徑設計

```
Firebase Storage
└── children/
    └── {childId}/
        └── medical/
            └── {visitId}.jpg     ← 壓縮後 < 200KB
```

### 5.2 圖片壓縮規則

| 類型 | 目標大小 | 說明 |
|------|---------|------|
| 就診單照片 | < 200 KB | JPEG，品質自動調降至符合 |
| 成長記錄照片（未來） | < 150 KB | 縮圖優先 |

### 5.3 本機圖片快取路徑

```
/data/data/{packageName}/files/
└── medical_images/
    └── {visitId}.jpg     ← 從 Storage 下載後快取
```

### 5.4 空間管理策略

- Firebase Storage 免費額度（Blaze Plan）：5 GB / 月
- 預估：200 KB × 每週 2 次就診 × 52 週 = 約 20 MB / 年，多年不超標
- **超過 6 個月的舊圖片**：週報匯出時一併上傳 Google Drive，可從 Storage 刪除
- Firestore 文件內**不存放圖片 Binary**，只存 `imageStoragePath` 字串

---

## 6. 本機 Room DB 設計（留位）

### 6.1 需新增模組：`core/firebase`、`core/drive`

```
core/
├── model/        ← 已存在，需新增 WeeklyReport domain model
├── data/         ← 已存在，需新增 WeeklyReportDao + FTS
├── ai/           ← 已存在
├── ui/           ← 已存在
├── firebase/     ← 待新增：FirestoreRepository, ImageUploadRepository
└── drive/        ← 待新增：DriveExportRepository
```

### 6.2 WeeklyReportEntity（Room）- 留位

```kotlin
// TODO Phase F：實作於 core/data
@Entity(tableName = "weekly_reports")
data class WeeklyReportEntity(
    @PrimaryKey val id: String,           // "{childId}_{year}-W{week}"
    val childId: String,
    val weekStart: String,                // "2026-05-04"
    val weekEnd: String,                  // "2026-05-10"
    val aiSummary: String,
    val medicalVisitIdsJson: String,      // JSON 陣列
    val growthSnapshotJson: String,
    val vaccineDueJson: String,
    val searchKeywords: String,           // 逗號分隔，FTS 索引用
    val driveFileId: String?,             // Google Drive 匯出後存 fileId
    val syncedAt: Long
)

// FTS4 全文搜尋（支援離線關鍵字搜尋就醫紀錄）
@Fts4(contentEntity = WeeklyReportEntity::class)
@Entity(tableName = "weekly_reports_fts")
data class WeeklyReportFts(
    val aiSummary: String,
    val searchKeywords: String
)
```

### 6.3 搜尋 DAO 介面（留位）

```kotlin
// TODO Phase F：實作於 core/data
@Dao
interface WeeklyReportDao {

    // FTS 全文搜尋（離線可用）
    @Query("""
        SELECT wr.* FROM weekly_reports wr
        INNER JOIN weekly_reports_fts fts ON wr.rowid = fts.rowid
        WHERE weekly_reports_fts MATCH :keyword
        AND wr.childId = :childId
        ORDER BY wr.weekStart DESC
    """)
    fun searchByKeyword(childId: String, keyword: String): Flow<List<WeeklyReportEntity>>

    @Query("""
        SELECT * FROM weekly_reports
        WHERE childId = :childId AND weekStart LIKE :year || '%'
        ORDER BY weekStart DESC
    """)
    fun getByYear(childId: String, year: String): Flow<List<WeeklyReportEntity>>

    @Upsert
    suspend fun upsert(report: WeeklyReportEntity)

    @Query("SELECT * FROM weekly_reports WHERE id = :id")
    suspend fun getById(id: String): WeeklyReportEntity?
}
```

---

## 7. 週報產出與 Google Drive 匯出流程

### 7.1 觸發時機

- **手動觸發**：AI 操作員在 `WeeklyReportScreen` 按下「產生本週週報」
- **自動觸發**（未來）：WorkManager 每週日 22:00 自動執行

### 7.2 完整流程

```
1. 高階機偵測到 aiPending: true 的 medicalVisit
   └─► 呼叫 ServiceAI.summarizeMedicalNote()
       └─► AI 結果寫入 aiResults 子集合

2. AI 操作員手動觸發「產生週報」
   ├─► 讀取本週所有 DailyLog / MedicalVisit / GrowthRecord
   ├─► ServiceAI.summarizeWeeklyLog() → aiSummary 文字
   ├─► AI 萃取 searchKeywords（發燒、藥名、診斷等）
   ├─► 寫入 Firestore weeklyReports/{childId}/{year}-W{week}
   └─► 清除已處理的 aiPending 旗標

3. Google Drive 匯出
   ├─► 產生 Markdown 週報文件（含所有摘要）
   ├─► 產生 weekly_reports.json（全年累積，可重新匯入）
   ├─► 上傳本週就診照片至 Drive 備份資料夾
   ├─► 更新 Firestore weeklyReport.driveExported = true
   └─► 可選：刪除 Storage 中已備份的舊照片（> 6 個月）

4. 低階機自動收到 Firestore 更新 → 顯示週報卡片
```

### 7.3 Google Drive 資料夾結構

```
Google Drive
└── BabyMakiSuk/
    ├── weekly_reports.json          ← 全年累積，每週 append
    ├── 2026/
    │   ├── W19_2026-05-04.md        ← 週報 Markdown
    │   ├── W19_2026-05-04.md
    │   └── photos/
    │       └── mv_001.jpg           ← 就診照片備份
    └── 2027/
        └── ...
```

---

## 8. 現有程式碼需要的調整清單

### 8.1 立即可執行（不影響現有功能）

- [ ] `core/model`：新增 `WeeklyReport.kt` domain model（data class）
- [ ] `core/model`：`MedicalVisit` 新增欄位 `imageStoragePath: String?`、`aiPending: Boolean`
- [ ] `core/data`：新增 `WeeklyReportEntity.kt`、`WeeklyReportFts.kt`
- [ ] `core/data`：新增 `WeeklyReportDao.kt`（含 FTS 搜尋介面）
- [ ] `core/data`：`AppDatabase` 新增 `weeklyReports` / `weekly_reports_fts` 表，bump 版本號
- [ ] 新增模組 `core/firebase`（build.gradle.kts 留位，功能 Phase E 實作）
- [ ] 新增模組 `core/drive`（build.gradle.kts 留位，功能 Phase F 實作）
- [ ] `settings.gradle.kts` 注冊新模組

### 8.2 Phase E 實作（Firebase 同步）

- [ ] `core/firebase`：`FirestoreChildRepository`（離線持久化啟用）
- [ ] `core/firebase`：`FirestoreMedicalRepository`（aiPending 旗標監聽）
- [ ] `core/firebase`：`ImageUploadRepository`（壓縮 + Storage 上傳）
- [ ] `core/firebase`：`MedicalImageCacheManager`（本機快取管理）
- [ ] Firebase Auth Custom Claims 設定（`data_manager` / `ai_operator` 角色）
- [ ] Firestore Security Rules 部署（見第 4 節）
- [ ] `feature/settings`：Google 登入 UI

### 8.3 Phase F 實作（週報 + Drive 匯出）

- [ ] `feature/weeklyreport`：`WeeklyReportScreen` UI
- [ ] `feature/weeklyreport`：`WeeklyReportViewModel`（觸發 AI + 聚合資料）
- [ ] `core/drive`：`DriveExportRepository`（Markdown 上傳、JSON append）
- [ ] `core/drive`：`DriveImageBackupManager`（舊照片遷移）
- [ ] `core/ai`：`weekly_baby_log_summary` prompt schema（searchKeywords 萃取）
- [ ] WorkManager 週期任務（可選：週日 22:00 自動觸發）

---

## 9. 依賴關係圖（模組層級）

```
feature/weeklyreport
  ├── core/ai           (ServiceAI 呼叫)
  ├── core/firebase     (Firestore 讀寫)
  ├── core/drive        (Drive 匯出)
  └── core/data         (Room 本機快取)

feature/medical
  ├── core/firebase     (ImageUpload)
  └── core/data         (MedicalVisitDao)

core/firebase
  └── core/model

core/drive
  └── core/model
```

---

## 10. 非功能需求

| 項目 | 規格 |
|------|------|
| 離線可用 | Room 本機 DB + Firestore 離線持久化，兩機均可無網路操作 |
| 圖片壓縮上限 | 200 KB / 張，超過自動降質 |
| Storage 免費額度 | 5 GB（Blaze Plan），預估 10 年以上不超標 |
| Firestore 免費讀寫 | Blaze 50K 讀 / 20K 寫 / 日，家庭用量充裕 |
| 搜尋響應時間 | Room FTS4 本機搜尋 < 100ms |
| Drive 匯出格式 | Markdown（人類可讀）+ JSON（程式可重新匯入） |
| 資料最小化 | Firestore 文件不含圖片 Binary，只存 Storage 路徑 |

---

*本計劃書由 Perplexity AI 根據 BabyMakiSuk 專案現有架構生成，作為 Phase E / F 開發的知識庫基準。*
