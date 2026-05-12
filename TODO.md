# BabyMakiSuk TODO

更新日期：2026-05-12

---

## 專案狀態

- [x] Phase A - 多模組專案骨架
- [x] Phase A - coremodel 資料模型
- [x] Phase A - coredata Room / DAO / Repository 基礎層
- [x] Phase A - Navigation + Bottom Navigation UI 骨架
- [x] Phase B - GrowthListScreen
- [x] Phase B - NewGrowthRecordDialog 表單與輸入驗證
- [x] Phase B - percentile 計算介面（stub LMS）
- [x] Phase B - 成長折線圖
- [x] Phase B - WHO 參考曲線疊加（P3 / P15 / P50 / P85 / P97）
- [x] Phase B - P15-P85 區間帶
- [x] Phase B - 首頁 Child 卡片顯示最近一次成長摘要（LatestGrowthBanner）
- [x] Phase B - GrowthScreen 圖表切換 Icon 修正（ShowChart）
- [x] Phase B - head circumference percentile 與圖表
- [ ] Phase B - 替換成 WHO 官方完整 0-60 月 LMS/CSV 資料（目前為 stub，可升級）

> ✅ Phase A & B 核心功能全部完成。WHO 官方完整 CSV 替換列為 Backlog 優化項。

---

## Phase C - 醫療紀錄與 ServiceAI 整合 v1

- [x] MedicalUiState（sealed interface）
- [x] MedicalViewModel（ChildRepository + MedicalDao，flatMapLatest 孩子切換）
- [x] MedicalScreen（ChildFilterChip + LazyColumn + MedicalVisitCard 可展開）
- [x] MedicalVisitCard AI 三欄展示（diagnosisSummary / prescriptions / careInstructions）
- [x] NewMedicalVisitDialog（醫院、科別、診斷、備註表單）
- [ ] coreai / ServiceAI 真實 SDK 串接
- [x] MedicalAiRepository（summarizeMedicalVisit + analyzePrescription）
- [ ] medical_note_summarizer prompt schema
- [ ] AI JSON 解析與寫入 MedicalVisit
- [ ] 「AI 整理僅供參考」安全提示
- [ ] AI 結果手動編輯 UI
- [ ] 📷 掃描病歷 → OCR → 自動填入 AI 欄位

### Sprint 1 — AiDispatcher 核心層（2026-05-12）✅

- [x] `GeminiModel` 加入 `AiEngineType`，修正 Gemma 4 26B modelId 為 `gemma-4-26b-a4b-it`
- [x] `AiTask` enum（6 個任務：MEDICAL_CONSULTATION / MEDICAL_OCR / VOICE_INPUT / WEEKLY_REPORT / QUICK_CHAT / CUSTOM_PRESET）
- [x] `RateLimiter`（in-memory 滑動視窗 60s，per-task 獨立配額）
- [x] `RateLimitException` / `AiDispatchException`
- [x] `AiDispatcher`（Fallback Chain + Rate Limit 整合，支援 System Prompt）

### Sprint 2 — AI Portal 與情境入口（2026-05-13）✅

- [x] `AiPreset` enum（5 個角色，含 task 欄位對應 AiTask）
- [x] `AiPromptBuilder`（buildSystemPrompt，CUSTOM 早期返回）
- [x] `AiPortalScreen`（對話視窗 UI + PresetSelector + ChatHistory）
- [x] `AiPortalViewModel`（情境感知排序、RateLimitException 處理）
- [x] Navigation：`ai_portal?presetHint={hint}` 路由
- [x] `MedicalScreen` / `GrowthScreen` / `HomeScreen` 加入 AI FAB

### Sprint 3 — MedicalAiRepository + 規則式 RAG Phase 1.5 + FTS 搜尋 UI（2026-05-12）✅

- [x] `AiContextInjector`（規則式 RAG Phase 1.5，注入最近 3 筆就醫 + 1 筆成長紀錄）
- [x] `AiPromptBuilder.buildSystemPromptWithContext()`（Sprint 3 新增 overload）
- [x] `MedicalAiRepository`（summarizeMedicalVisit + analyzePrescription）
- [x] `WeeklyReportRepository` AI 整合（generateWeeklyReport 使用 AiDispatcher + AiContextInjector）
- [x] `WeeklyReportSearchScreen`（FTS 全文搜尋 + AnnotatedString keyword highlight）
- [x] `WeeklyReportSearchViewModel`（HiltViewModel + debounce 300ms + FTS StateFlow）
- [x] `BabyMakiSukNavHost`：新增 `weekly_report_search?childId={childId}` 路由

---

## HomeScreen

- [x] HomeUiState / HomeViewModel（已 merge to main）
- [x] 雙 ChildSummaryCard 並排（男藍 #4A90D9 / 女粉 #E07BBD）
- [x] TwinDiffBadge 雙胞胎身高體重差距顯示
- [x] DailyLogOverviewCard 今日日誌快覽（吃飯、睡眠、心情 emoji）
- [x] HomeTopBar 日期問候
- [x] HomeScreen 接入 feat/home-ui branch → merge to main
- [ ] AI 晨報 Card（AiMorningBriefingCard，接 ServiceAI summarizeBabyDailyLog）
- [ ] 疫苗提醒 Card（VaccineReminderCard）
- [ ] 下次回診 Card（NextVisitCard）

---

## Phase D - 每日日誌與 AI 每週總結

- [ ] DailyLogScreen
- [ ] NewDailyLogScreen
- [ ] weekly_baby_log_summary 任務
- [ ] WeeklySummaryScreen
- [ ] 重新生成 / 編輯摘要

---

## Phase E - Firebase 同步（雙機協作）

> 詳細規格見 docs/SYNC_ARCHITECTURE.md

### E-0 立即執行（Model 留位，不影響現有功能）—— ✅ 全部完成

- [x] `core/model`：`MedicalVisit` 新增欄位 `imageStoragePath: String?`、`aiPending: Boolean`
- [x] `core/model`：新增 `WeeklyReport.kt` domain model
- [x] `core/data`：新增 `WeeklyReportEntity.kt`、`WeeklyReportFts.kt`
- [x] `core/data`：新增 `WeeklyReportDao.kt`（FTS 搜尋介面）
- [x] `core/data`：`AppDatabase` 新增週報表，bump 版本號（v2）
- [x] 新增空模組 `core/firebase`（build.gradle.kts 留位）
- [x] 新增空模組 `core/drive`（build.gradle.kts 留位）
- [x] `settings.gradle.kts` 注冊新模組

### E-1 Firebase 基礎

- [ ] Firebase Auth + Google 登入
- [ ] Custom Claims 設定（`data_manager` / `ai_operator` 角色）
- [ ] Firestore Security Rules 部署
- [ ] Firestore 離線持久化啟用（`isPersistenceEnabled = true`）
- [ ] `feature/settings` Google 登入 UI

### E-2 資料同步

- [ ] `core/firebase`：`FirestoreChildRepository`
- [ ] `core/firebase`：`FirestoreMedicalRepository`（含 `aiPending` 旗標監聽）
- [ ] `core/firebase`：`ImageUploadRepository`（圖片壓縮 + Storage 上傳）
- [ ] `core/firebase`：`FirestoreMedicalImageCacheManager`（本機圖片快取）
- [ ] 雙機同步測試（低階機寫入 → 高階機 AI 觸發 → 結果同步回低階機）

---

## Phase F - 週報 + Google Drive 永久備份

> 詳細規格見 docs/SYNC_ARCHITECTURE.md

- [x] `feature/weeklyreport`：`WeeklyReportSearchScreen`（FTS 搜尋 + keyword highlight）
- [x] `feature/weeklyreport`：`WeeklyReportSearchViewModel`
- [x] `core/data`：`WeeklyReportRepository`（AI 整合 generateWeeklyReport）
- [ ] `feature/weeklyreport`：`WeeklyReportScreen` UI
- [ ] `feature/weeklyreport`：`WeeklyReportViewModel`
- [ ] `core/ai`：`weekly_baby_log_summary` prompt schema（含 searchKeywords 萃取）
- [ ] `core/drive`：`DriveExportRepository`（Markdown 週報 + JSON 匯出）
- [ ] `core/drive`：`DriveImageBackupManager`（舊照片遷移 > 6 個月）
- [ ] Firestore `driveExported` / `driveFileId` 欄位寫回
- [ ] WorkManager 週期任務（週日 22:00 自動觸發，可選）

---

## Phase G - Settings 頁功能擴充

> Settings 頁基礎 UI 已於 2026-05-11 完成。

### G-0 已完成 ✅

- [x] `SettingsScreen`：分區 UI（外觀 / 資料管理 / 關於）
- [x] `SettingsViewModel`：MVI + `BackupUiState` sealed interface
- [x] `SettingsRepository`：DataStore 深色模式 + 委派 BackupManager
- [x] `DarkModeOption` enum：SYSTEM / LIGHT / DARK
- [x] `SettingsPreferences`：DataStore Key 統一定義
- [x] `BabyMakiSukTheme` 支援 `darkTheme: Boolean`
- [x] `MainActivity` 讀取 `SettingsViewModel.darkMode`，動態传入主題

### G-1 已完成 ✅

- [x] `BackupManager`：Room 全資料 → JSON 備份 DTO
- [x] `BackupManager.exportToShareIntent()`：JSON 寫入 cache，透過 FileProvider + ShareSheet 分享
- [x] `BackupManager.importFromUri()`：讀取 JSON → merge 或覆蓋寫入 Room（`runInTransaction`）
- [x] 全部 DAO 新增 `getAllOnce()` / `upsertAll()` / `deleteAll()`
- [x] `SettingsScreen` 匯入前確認 Dialog、載入中過場、成功 / 錯誤 Alert

### G-2 已完成 ✅

- [x] **`ApiTestScreen` — API 連線測試子頁面** ✅ 2026-05-12
  - Key 狀態顯示（`AiConfig.hasValidKey` → 已注入 / 未注入）
  - 發送固定 prompt 並計時（`System.currentTimeMillis()`）
  - `sealed interface ApiTestUiState`（Idle / Loading / Success / Error）
  - Loading `CircularProgressIndicator` 動畫
  - 成功：等寬字體原始回應 + primaryContainer 卡片
  - 失敗：紅色錯誤訊息 + errorContainer 卡片
  - 回應耗時（ms）顯示於 `AssistChip`
  - 路由：`settings/api_test`，TopAppBar 返回按鈕
- [x] `ApiTestViewModel`：注入 `ServiceAiClient`（介面）+ `AiConfig`
- [x] `SettingsScreen`：新增 `onNavigateToApiTest` callback + BugReport SettingsItem
- [x] `BabyMakiSukNavHost`：新增 `settings/api_test` 子路由，子頁面隱藏 BottomBar
- [x] `feature/settings/build.gradle.kts`：新增 `core:ai` 依賴
- [ ] `AndroidManifest.xml` 新增 FileProvider `<provider>` 設定
- [ ] `res/xml/file_paths.xml` 定義 cache-path
- [ ] 多寶寶 Profile 管理（新增 / 切換 / 刪除）
- [ ] 通知排程設定（餵奶提醒、疫苗到期推播）
- [ ] 語言切換（繁中 / English）

---

## Backlog

- [ ] 替換成 WHO 官方完整 0-60 月 LMS/CSV 資料（目前 stub 精度足夠，正式上架前升級）
- [ ] 多家長共用 Child（OWNER / CAREGIVER）
- [ ] PDF 報表輸出
- [ ] Widget 顯示今日待辦
- [ ] LINE / 短訊分享就診摘要
- [ ] 多語系（繁中 / 英文）
- [ ] 里程碑氣泡（成長百分位跨區震動提示）
- [ ] Storage 舊照片自動清理（> 6 個月 → 遷移至 Drive）
- [ ] `feature/vaccine` 疫苗模組實作（目錄已建立，待功能開發）
