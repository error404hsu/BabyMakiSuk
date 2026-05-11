# BabyMakiSuk TODO

更新日期：2026-05-11

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
- [ ] Phase B - head circumference percentile 與圖表
- [ ] Phase B - 替換成 WHO 官方完整 0-60 月 LMS/CSV 資料

---

## Phase C - 醫療紀錄與 ServiceAI 整合 v1

- [x] MedicalUiState（sealed interface）
- [x] MedicalViewModel（ChildRepository + MedicalDao，flatMapLatest 孩子切換）
- [x] MedicalScreen（ChildFilterChip + LazyColumn + MedicalVisitCard 可展開）
- [x] MedicalVisitCard AI 三欄展示（diagnosisSummary / prescriptions / careInstructions）
- [x] NewMedicalVisitDialog（醫院、科別、診斷、備註表單）
- [ ] coreai / ServiceAI 真實 SDK 串接
- [ ] MedicalAiRepository
- [ ] medical_note_summarizer prompt schema
- [ ] AI JSON 解析與寫入 MedicalVisit
- [ ] 「AI 整理僅供參考」安全提示
- [ ] AI 結果手動編輯 UI
- [ ] 📷 掃描病歷 → OCR → 自動填入 AI 欄位

---

## HomeScreen

- [x] HomeUiState / HomeViewModel（feat/home-ui branch）
- [x] 雙 ChildSummaryCard 並排（男藍 #4A90D9 / 女粉 #E07BBD）
- [x] TwinDiffBadge 雙胞胎身高體重差距顯示
- [x] DailyLogOverviewCard 今日日誌快覽（吃飯、睡眠、心情 emoji）
- [x] HomeTopBar 日期問候
- [ ] AI 晨報 Card（AiMorningBriefingCard，接 ServiceAI summarizeBabyDailyLog）
- [ ] HomeScreen 接入 feat/home-ui branch → merge to main
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

### E-0 立即執行（Model 留位，不影響現有功能）——✅ 全部完成

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
- [ ] `core/firebase`：`MedicalImageCacheManager`（本機圖片快取）
- [ ] 雙機同步測試（低階機寫入 → 高階機 AI 觸發 → 結果同步回低階機）

---

## Phase F - 週報 + Google Drive 永久備份

> 詳細規格見 docs/SYNC_ARCHITECTURE.md

- [ ] `feature/weeklyreport`：`WeeklyReportScreen` UI
- [ ] `feature/weeklyreport`：`WeeklyReportViewModel`
- [ ] `core/ai`：`weekly_baby_log_summary` prompt schema（含 searchKeywords 萃取）
- [ ] `core/drive`：`DriveExportRepository`（Markdown 週報 + JSON 匯出）
- [ ] `core/drive`：`DriveImageBackupManager`（舊照片遷移 > 6 個月）
- [ ] Firestore `driveExported` / `driveFileId` 欄位寫回
- [ ] `WeeklyReportSearchScreen`（FTS 搜尋就醫紀錄）
- [ ] WorkManager 週期任務（週日 22:00 自動觸發，可選）

---

## Backlog

- [ ] 多家長共用 Child（OWNER / CAREGIVER）
- [ ] PDF 報表輸出
- [ ] Widget 顯示今日待辦
- [ ] LINE / 短訊分享就診摘要
- [ ] 多語系（繁中 / 英文）
- [ ] 里程碑氣泡（成長百分位跨區震動提示）
- [ ] Storage 舊照片自動清理（> 6 個月 → 遷移至 Drive）
- [ ] `feature/vaccine` 疫苗模組實作（目錄已建立，待功能開發）
