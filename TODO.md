# BabyMakiSuk TODO

更新日期：2026-05-14

> 📌 開發規範詳見 [`docs/AGENT_GUIDELINES.md`](./docs/AGENT_GUIDELINES.md)

---

## 專案整體進度

| Phase | 名稱 | 狀態 |
|-------|------|------|
| A | 多模組骨架 + 資料模型 | ✅ 完成 |
| B | 成長紀錄 + WHO 圖表 | ✅ 完成 |
| C | 醫療紀錄 + AI 整合 | 🔄 進行中 |
| H | 書庫（週報 / AI 精華 / Memo） | ✅ 完成 |
| UI | 設計系統優化 | ✅ 完成 |
| D | 每日日誌 × Memo 整合 | 🔲 Sprint 4 |
| 5 | HomeScreen 收折優化 | 🔲 Sprint 5 |
| 6 | 通知排程 | 🔲 Sprint 6 |
| E | Firebase 同步 | ⏸ 暫緩 |
| F | Google Drive 備份 | ⏸ 暫緩 |
| G | Settings 功能 | 🔲 部分完成，見下方 |

---

## ✅ 已完成

### Phase A — 基礎骨架
- [x] 多模組專案骨架
- [x] core/model 資料模型
- [x] core/data Room / DAO / Repository 基礎層
- [x] Navigation + Bottom Navigation UI 骨架

### Phase B — 成長紀錄
- [x] GrowthListScreen
- [x] NewGrowthRecordDialog 表單與輸入驗證
- [x] Percentile 計算介面（stub LMS）
- [x] 成長折線圖
- [x] WHO 參考曲線疊加（P3 / P15 / P50 / P85 / P97）
- [x] P15-P85 區間帶
- [x] LatestGrowthBanner（首頁最近一次成長摘要）
- [x] GrowthScreen 圖表切換 Icon 修正
- [x] Head circumference percentile 與圖表

### Phase C — 醫療紀錄 + AI
- [x] MedicalUiState / MedicalViewModel / MedicalScreen
- [x] MedicalVisitCard（AI 三欄展示）
- [x] NewMedicalVisitDialog（已升級為 ModalBottomSheet 全欄位版）
- [x] core/ai ServiceAI 真實 SDK 串接
- [x] MedicalAiRepository（summarizeMedicalVisit + analyzePrescription stub）
- [x] AI JSON 解析與寫入 MedicalVisit
- [x] AI 結果手動編輯 UI
- [x] AiDispatcher 核心層（Sprint 1）
- [x] AI Portal + 情境入口（Sprint 2）
- [x] AiContextInjector 規則式 RAG Phase 1.5（Sprint 3）
- [x] WeeklyReportRepository AI 整合
- [x] WeeklyReportSearchScreen（FTS + keyword highlight）
- [x] NewMedicalVisitDialog — 就診日期 DatePickerDialog
- [x] NewMedicalVisitDialog — AI 診斷 / 處方 / 居家照護三欄位整合
- [x] NewMedicalVisitDialog — 藥單拍照（相機 / 相簿）+ 圖片預覽
- [x] NewMedicalVisitDialog — AI 分析按鈕 + 信心分數 Banner + 自動填入
- [x] AiAnalysisState sealed class（Idle / Analyzing / Success / Error）
- [x] MedicalViewModel.analyzeImageWithAi() + resetAiState()
- [x] MedicalVisitCard — 整張卡片 clickable 展開，移除獨立展開按鈕
- [x] coil-compose 3.x 加入 libs.versions.toml + feature/medical build.gradle

### Phase H — 書庫
- [x] BottomNavItem 配置
- [x] AiInsightEntity + MemoEntity
- [x] AiInsightDao + MemoDao + WeeklyReportDao
- [x] AppDatabase v3→v4 + MIGRATION_3_4
- [x] LibraryScreen（3 書架卡片）
- [x] WeeklyShelfScreen
- [x] AiInsightShelfScreen（長按刪除）
- [x] MemoShelfScreen（CRUD + ModalBottomSheet）
- [x] NavHost library 子路由補齊

### Phase G — Settings（已完成部分）
- [x] SettingsScreen 分區 UI
- [x] SettingsViewModel MVI + BackupUiState (支援進度顯示)
- [x] DarkModeOption + DataStore 深色模式
- [x] BackupManager JSON 備份 / 匯入升級 v3 (全模組補齊：書庫、疫苗提醒、如廁紀錄)
- [x] 數據管理 UI 優化 (自動備份開關、上次備份時間、雲端同步預留)
- [x] 匯入匯出安全提示與進度遮罩 (Loading Overlay)
- [x] ApiTestScreen（API 連線測試子頁面）
- [x] FileProvider 設定

### UI 設計系統
- [x] 色票系統更新（Primary / Dark / Tertiary / Background / Text）
- [x] Lora + Raleway Typography scale
- [x] Empty States 補強（Home / Growth / Medical / Search）
- [x] Growth Chart 色盲友善配色
- [x] AI Chat Bubble 樣式優化
- [x] HTML 原型產出（ui-ux-preview.html）

### Phase E-0 — Firebase 留位（不含實作）
- [x] MedicalVisit 新增 imageStoragePath / aiPending 欄位
- [x] WeeklyReport domain model
- [x] WeeklyReportEntity / WeeklyReportFts / WeeklyReportDao
- [x] AppDatabase bump v2
- [x] core/firebase 空模組留位
- [x] core/drive 空模組留位

---

## 🔲 待開發

### Phase C — 醫療 AI 圖片分析（待接 API）
> 前置 UI 與 ViewModel 已完成，等待後端 API 實作後串接。

- [x] **`MedicalAiRepository.analyzePrescriptionImage(imageUri, symptomHint, ageMonths, gender, allergies)`**
  - 呼叫 Gemini Vision API（multimodal）辨識藥單圖片
  - 回傳結構：`diagnosisSummary`, `prescriptions: List<String>`, `careInstructions: List<String>`, `confidence: Int`
  - 實作位置：`core/ai` 模組
  - 已移除 `MedicalViewModel` 與 `AiAnalysisState` 中的 TODO 標注

### Sprint 4 — Memo 整合每日日誌
> 詳細規格見 `docs/AGENT_GUIDELINES.md` → Sprint 4 章節

- [x] 確認 MemoEntity 是否含 `childId`、`date`、`reminderAt` 欄位 (已於 Migration v9 完成)
- [x] MemoShelfScreen 改為日期分組顯示
- [x] MemoDao 補充 `getByChildAndDate()` 查詢
- [x] HomeScreen DailyLogOverviewCard 改讀當日 Memo
- [x] 新增 Memo 入口從 HomeScreen 今日日誌區「＋」觸發
- [x] Memo 編輯全頁 Screen（`library/memo/edit` 路由）

### Sprint 5 — HomeScreen 收折優化
> 詳細規格見 `docs/AGENT_GUIDELINES.md` → Sprint 5 章節

- [x] ChildSummaryCard 收折 / 展開（AnimatedVisibility / animateFloatAsState）
- [x] 展開區：上次就醫摘要 + 下次排程 + 本日 Memo
- [x] AiMorningBriefingCard（收折版，串接 AiDispatcher）

### Sprint 6 — 通知排程
> 詳細規格見 `docs/AGENT_GUIDELINES.md` → Sprint 6 章節

- [x] WorkManager + NotificationCompat
- [x] Memo 編輯畫面「設定提醒」DateTimePicker
- [x] SettingsScreen 通知總開關（DataStore）
- [ ] POST_NOTIFICATIONS 權限申請 Runtime 確認（Android 13+）

### 就醫畫面重構
- [x] MedicalEditScreen 全頁編輯 Screen（`medical/edit` 路由）
- [x] 廢棄 NewMedicalVisitDialog 並遷移至 MedicalEditScreen

### 成長紀錄重構
- [x] GrowthEditScreen 全頁編輯（含 recordedAt DatePickerDialog）
- [x] 廢棄 NewGrowthRecordDialog
- [ ] 成長新增/編輯畫面加入日期欄位（與就醫同步）
- [x] 成長圖表深色模式白色區塊修正（使用 MaterialTheme.colorScheme.surface）

### BottomNavigation
- [x] 「健護」標籤改為「醫護」

---

## ⏸ 暫緩 / Backlog

| 項目 | 備註 |
|------|------|
| Firebase Auth / Firestore（Phase E-1 以後） | 暫緩，留位模組已建立 |
| Google Drive 備份（Phase F 剩餘） | 暫緩 |
| feature/vaccine 完整功能 | 目錄留位，孩子 3 歲暫不需要 |
| WHO 官方 0-60 月 LMS CSV 替換 | stub 精度足夠，上架前再升級 |
| PDF 報表輸出 | 暫緩 |
| Widget 今日待辦 | 暫緩 |
| LINE / 短訊分享就診摘要 | 暫緩 |
| 多語系（繁中 / English） | 已刪除 |
| 多寶寶 Profile 切換 UI | 已刪除 |
| 里程碑氣泡震動提示 | 暫緩 |
