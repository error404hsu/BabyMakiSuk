# BabyMakiSuk TODO

更新日期：2026-05-19

> 📌 開發規範詳見 [`docs/AGENT_GUIDELINES.md`](./docs/AGENT_GUIDELINES.md)

---

## 專案整體進度

| Phase | 名稱 | 狀態 |
|-------|------|------|
| A | 多模組骨架 + 資料模型 | ✅ 完成 |
| B | 成長紀錄 + WHO 圖表 | ✅ 完成 |
| C | 醫療紀錄 + AI 整合 | ✅ 完成 |
| H | 書庫（週報 / AI 精華 / Memo） | ✅ 完成 |
| UI | 設計系統優化 | ✅ 完成 |
| D | 每日日誌 × Memo 整合 | ✅ 完成 |
| 5 | HomeScreen 收折優化 | 🔄 進行中（AiMorningBriefingCard 暫緩） |
| 6 | 通知排程 | ✅ 完成 |
| R | 資料留存與清理 | ✅ 完成 |
| E-1 | Firebase 基礎設施 + Auth | ✅ 完成 |
| E-2 | Firestore 同步（基礎版） | ✅ 完成 |
| E-3 | Firebase 圖片儲存 + 快取 | ✅ 完成 |
| E-4 | Firebase 深度整合 + 安全規則 | 🔲 下一步 |
| F | Google Drive 備份 | ⏸ 暫緩 |
| G | Settings 功能 | ✅ 完成 |

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
- [x] **`MedicalAiRepository.analyzePrescriptionImage`** (Gemini Vision 藥單辨識)

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

### Phase G — Settings
- [x] SettingsScreen 分區 UI
- [x] SettingsViewModel MVI + BackupUiState (支援進度顯示)
- [x] DarkModeOption + DataStore 深色模式
- [x] BackupManager JSON 備份 / 匯入升級 v3 (全模組補齊：書庫、疫苗提醒、如廁紀錄)
- [x] 數據管理 UI 優化 (自動備份開關、上次備份時間、雲端同步預留)
- [x] 匯入匯出安全提示與進度遮罩 (Loading Overlay)
- [x] ApiTestScreen（API 連線測試子頁面）
- [x] FileProvider 設定
- [x] 開發者選項擴充（Auth狀態/Firestore離線/即時通知/DataRetention/DB快照）

### Sprint 4 — Memo 整合每日日誌
- [x] 確認 MemoEntity 是否含 `childId`、`date`、`reminderAt` 欄位
- [x] MemoShelfScreen 改為日期分組顯示
- [x] MemoDao 補充 `getByChildAndDate()` 查詢
- [x] HomeScreen DailyLogOverviewCard 改讀當日 Memo
- [x] 新增 Memo 入口從 HomeScreen 今日日誌區「＋」觸發
- [x] Memo 編輯全頁 Screen（`library/memo/edit` 路由）

### Sprint 6 — 通知排程 ✅ 完成
- [x] WorkManager + NotificationCompat
- [x] Memo 編輯畫面「設定提醒」DateTimePicker
- [x] SettingsScreen 通知總開關（DataStore）
- [x] POST_NOTIFICATIONS 權限申請 Runtime 確認（Android 13+）

### Phase R — 資料留存與清理（DATA_RETENTION_STRATEGY.md）
- [x] Phase 1 — DAO 清理方法：DailyLogDao / ToiletDao / AiInsightDao / SystemReminderDao / VaccineReminderDao / MonthlyReportDao
- [x] Phase 2 — DataRetentionRepository + DataRetentionWorker（PeriodicWorkRequest 7 天）
- [x] Phase 2 — hilt-work 依賴 + HiltWorkerFactory 設定 + Application 排程
- [x] Phase 3 — MonthlyReportViewModel 成功後 cleanup 綁定（失敗不清理）
- [x] Phase 3 — ReportGenerationState sealed interface 狀態機
- [x] Phase 3 — LibraryScreen 月報逾期紅點角標

### UI 與重構優化
- [x] 色票系統更新（Primary / Dark / Tertiary / Background / Text）
- [x] Lora + Raleway Typography scale
- [x] Empty States 補強（Home / Growth / Medical / Search）
- [x] Growth Chart 色盲友善配色
- [x] AI Chat Bubble 樣式優化
- [x] HTML 原型產出（ui-ux-preview.html）
- [x] MedicalEditScreen 全頁編輯（廢棄 NewMedicalVisitDialog）
- [x] GrowthEditScreen 全頁編輯（廢棄 NewGrowthRecordDialog）
- [x] 成長圖表深色模式白色區塊修正
- [x] 「健護」標籤改為「醫護」

### Phase E-0 — Firebase 留位
- [x] MedicalVisit 新增 imageStoragePath / aiPending 欄位
- [x] WeeklyReport domain model
- [x] WeeklyReportEntity / WeeklyReportFts / WeeklyReportDao
- [x] AppDatabase bump v2
- [x] core/firebase 空模組留位
- [x] core/drive 空模組留位

### Phase E-1 — Firebase 基礎設施 + 匿名認證
- [x] libs.versions.toml：firebase-bom / firebase-firestore / firebase-auth / firebase-storage
- [x] app/build.gradle.kts：google-services plugin + firebase-auth 依賴
- [x] core/firebase/build.gradle.kts：完整 Firebase 依賴
- [x] ImageStoragePath sealed class（Local / FirebaseStorage / None）
- [x] ImageStoragePathConverter（Room TypeConverter，向後相容舊資料）
- [x] FirebaseModule（Hilt DI：FirebaseFirestore / FirebaseAuth / FirebaseStorage）
- [x] FirebaseAuthRepository + DefaultFirebaseAuthRepository（匿名登入）
- [x] BabyMakiSukApplication.onCreate()：啟動時自動匿名登入

### Phase E-2 — Firestore 同步（基礎）
- [x] FirestoreChildRepository + DefaultFirestoreChildRepository（children 集合 CRUD）
- [x] FirestoreMedicalRepository + DefaultFirestoreMedicalRepository（medicalVisits 集合 + aiPending 監聽）
- [x] Firestore 離線持久化啟用（firestoreSettings.setPersistenceEnabled(true)）

### Phase E-3 — Firebase 圖片儲存 + 快取 ✅ 完成
- [x] `ImageUploadRepository` — 壓縮至 <200KB + Firebase Storage 上傳
- [x] `StorageRepository` — 上傳/下載/刪除 Firebase Storage
- [x] `MedicalImageCacheManager` — Storage 路徑 ↔ 本機快取 URI
- [x] `MedicalVisitEntity.imageStoragePath` 改為 `ImageStoragePath` sealed class（TypeConverter 處理，無需 Migration）
- [x] `MedicalVisit` domain model 同步變更
- [x] `BackupManager` 適配 ImageStoragePath 序列化
- [x] `DefaultFirestoreMedicalRepository` 適配 ImageStoragePath → Firestore
- [x] `MedicalEditViewModel` 儲存後自動上傳圖片至 Storage + 更新 aiPending
- [x] `MedicalEditViewModel.prescriptionImageUri` StateFlow（支援 Firebase 圖片載入）
- [x] `feature/medical` 加入 `:core:firebase` 依賴

---

## 🔲 待開發

### Sprint 5 — HomeScreen 收折優化（部分完成）
> 詳細規格見 `docs/AGENT_GUIDELINES.md` → Sprint 5 章節

- [x] ChildSummaryCard 收折 / 展開（AnimatedVisibility / animateFloatAsState）
- [x] 展開區：上次就醫摘要 + 下次排程 + 本日 Memo
- [ ] AiMorningBriefingCard（收折版，串接 AiDispatcher）⏸ **暫緩**

### E-4 — Firebase 深度整合 + 安全規則（下一步）
> 詳細規格見 `docs/DATA_RETENTION_STRATEGY.md` → SYNC_ARCHITECTURE.md

> ⚠️ **Auth 初始化時序問題**：`BabyMakiSukApplication.onCreate()` 的 `signInAnonymously()` 為非同步，
> `SettingsViewModel.init {}` 的 `observeAuthState()` 可能在匿名登入完成前訂閱，導致第一筆為 null。
> 修正方式：在 `init {}` 訂閱前先補 `_firebaseUser.value = authRepository.getCurrentUser()`，
> 並在 `FirebaseAuthRepository` 補上 `fun getCurrentUser(): FirebaseUser?`。

- [ ] **`SettingsViewModel` Auth 初始化時序修正**（補 `getCurrentUser()` 冷啟動值）
- [ ] `StorageCleanupWorker`（每年底滾動清除當年以前照片）
  > ⚠️ **執行順序**：年度總結產生 → StorageCleanupWorker → DataRetentionWorker，不可反序
- [ ] Firebase Auth Custom Claims（data_manager / ai_operator 角色）
  > 兩台手機需登入**同一 Google 帳號**才能同步；匿名帳號務必先 `linkWithGoogleCredential()` 再升級，否則資料不延續
- [ ] Firestore Security Rules 部署
- [ ] ChildRepository + FirestoreChildRepository 合併（local+remote combine）
- [ ] aiPending 監聽 → 觸發 ServiceAI 分析
- [ ] Google 登入 UI（feature/settings）
- [ ] Storage 配額顯示 UI

### 年度總結（Annual Report）— 建議方案 C（月報二次摘要）
> Phase F 完成前：方案 A（App 內讀取全年 WeeklyReport → Gemini 二次摘要 → 存入 AiInsightEntity）
> Phase F 完成後：方案 C（每月月報自動上傳 Drive，年底一鍵觸發年度整合）

- [ ] `AnnualReportGenerator`（讀取 `WeeklyReportDao` 全年摘要 → Gemini prompt → 存入 `AiInsightEntity`）
  > ⚠️ **執行順序**：年度總結 → StorageCleanupWorker（照片清除）→ DataRetentionWorker（明細清除）
- [ ] 年度總結入口 UI（書庫 or Settings）
- [ ] Phase F 完成後升級為方案 C（月報 MD 上傳 Drive → 外部 AI 整合）

---

## ⏸ 暫緩 / Backlog

| 項目 | 備註 |
|------|------|
| AiMorningBriefingCard | Sprint 5 最後一塊，暫緩 |
| Google Drive 備份（Phase F 剩餘） | 暫緩 |
| feature/vaccine 完整功能 | 目錄留位，孩子 3 歲暫不需要 |
| WHO 官方 0-60 月 LMS CSV 替換 | stub 精度足夠，上架前再升級 |
| PDF 報表輸出 | 暫緩 |
| Widget 今日待辦 | 暫緩 |
| LINE / 短訊分享就診摘要 | 暫緩 |
| 多語系（繁中 / English） | 已刪除 |
| 多寶寶 Profile 切換 UI | 已刪除 |
| 里程碑氣泡震動提示 | 暫緩 |
