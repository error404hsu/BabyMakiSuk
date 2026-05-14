# AGENT_GUIDELINES.md — BabyMakiSuk AI Agent 開發規範

> 供 AI Coding Agent（Codex、Copilot Workspace、Cursor 等）協助開發時使用。
> 最後更新：2026-05-14
>
> ⚠️ 本文件為 `AGENTS.md` 的擴充版，包含最新 Sprint 規範與設計決策，**以本文件為準**。

---

## 專案概述

**BabyMakiSuk** 是一款幼兒管理 Android App，以雙胞胎家庭為核心受眾。
目前使用對象孩子已滿 3 歲；功能規劃以此年齡段為基準。

- **技術棧**：Kotlin、Jetpack Compose、Hilt、Room、MVVM + MVI、Clean Architecture
- **AI 整合**：`core/ai` 透過 `AiDispatcher` Fallback Chain 統一分發 AI 任務

---

## 模組結構

```
app/                 - 入口、MainActivity、BabyMakiSukNavHost
core/model/          - 純 Kotlin domain model（無 Android 依賴）
core/data/           - Room Entity / DAO / Repository / Migration
core/ui/             - 共用 Composable、Theme、Typography
core/ai/             - AiDispatcher、AiTask、AiPreset、AiPromptBuilder、RateLimiter
core/firebase/       - Firebase（留位模組，勿新增實作）
core/drive/          - Google Drive（留位模組，勿新增實作）
feature/home/        - HomeScreen + ChildSummaryCard（可收折）
feature/growth/      - 成長紀錄 + 折線圖
feature/medical/     - 就醫紀錄 + AI 摘要
feature/library/     - 書庫：週報書架、AI 精華、Memo（兼每日日誌）
feature/weeklyreport/- 週報生成 + FTS 搜尋
feature/settings/    - 設定 + 備份 + 通知排程 + API 測試
feature/ai/          - AI Portal 對話介面
feature/vaccine/     - 疫苗就醫排程（目錄留位，暫緩實作）
```

---

## 架構規範

- **單向資料流（MVI）**：每個 Feature 必須有 `sealed interface UiState` + `HiltViewModel`。
- **Repository 模式**：ViewModel 只呼叫 Repository；禁止在 ViewModel 直接操作 DAO。
- **依賴注入**：全部使用 Hilt；Composable 內透過 `hiltViewModel()` 取得 ViewModel。
- **副作用隔離**：一次性事件（Toast、Navigation）透過 `SharedFlow<UiEvent>` 傳遞。

---

## 命名慣例

| 類型 | 規則 | 範例 |
|------|------|------|
| ViewModel | `{Feature}ViewModel` | `MedicalViewModel` |
| UiState | `{Feature}UiState` | `MedicalUiState` |
| Screen | `{Feature}Screen` | `MedicalScreen` |
| Room Entity | `{Domain}Entity` | `MedicalVisitEntity` |
| DAO | `{Domain}Dao` | `MedicalDao` |
| Repository 介面 | `{Domain}Repository` | `MedicalRepository` |
| Repository 實作 | `{Domain}RepositoryImpl` | `MedicalRepositoryImpl` |

---

## AI 模組規範（`core/ai`）

- 所有 AI 呼叫必須透過 **`AiDispatcher`**，禁止直接呼叫 SDK client。
- 新任務需先在 **`AiTask`** enum 登記，並設定 `RateLimiter` 配額（60s 滑動視窗）。
- System Prompt 統一由 **`AiPromptBuilder`** 生成；含孩子/就診上下文時使用 `buildSystemPromptWithContext()`。
- UI 呈現 AI 結果時，必須附帶「**AI 整理僅供參考**」安全提示。
- API Key 透過 `AiConfig` 從 BuildConfig 注入，**禁止硬編碼**。

---

## Room 資料庫規範

- 修改任何 Entity 或新增 Table，**必須** bump `AppDatabase` 版本號並撰寫對應 `MIGRATION_x_y`。
- 目前版本為 **v4**；新版 Migration 請依序命名 `MIGRATION_4_5`。

---

## Navigation 規範

- 所有路由字串定義於 `BabyMakiSukNavHost`；新增路由須在此統一登記。
- 子頁面須在 NavHost 中隱藏 BottomBar。
- AI Portal 路由格式：`ai_portal?presetHint={AiPreset.name}`。
- Memo 編輯路由：`library/memo/edit?memoId={id}&childId={childId}`（`memoId=-1` 為新增）
- 就醫編輯路由：`medical/edit?visitId={id}&childId={childId}`（`visitId=-1` 為新增）
- 成長編輯路由：`growth/edit?recordId={id}&childId={childId}`（`recordId=-1` 為新增）

---

## Sprint 開發規範（依優先順序）

### Sprint 4 — Memo 整合每日日誌

**目標**：取消獨立 DailyLogScreen；以 Memo 作為每日事記的唯一載體。

- `MemoEntity` 必須包含欄位：
  - `id: Long`
  - `childId: Long`（FK → ChildEntity）
  - `title: String`
  - `content: String`
  - `date: Long`（LocalDate.toEpochDay()，用於日期分組）
  - `reminderAt: Long?`（通知時間戳，Sprint 6 使用，可為 null）
  - `createdAt: Long`
- 若 `MemoEntity` 缺少 `childId` 或 `date` 欄位，**必須**執行 Room Migration（v4 → v5）
- `MemoShelfScreen`：以「日期」分組顯示，同日多筆並排；點擊跳轉 `library/memo/edit`
- `HomeScreen DailyLogOverviewCard`：讀取當日所有 childId 的 Memo 筆數與第一筆 content 預覽
- 新增 Memo 入口：HomeScreen 今日日誌區「＋」→ `library/memo/edit?memoId=-1&childId={id}`
- **禁止**新增 `DailyLogScreen`、`DailyLogViewModel`、`DailyLogEntity` 等獨立類別

---

### Sprint 5 — HomeScreen ChildSummaryCard 收折優化

**收折狀態（預設）** 顯示：
- 孩子姓名 + 性別色標
- 月齡（格式：`X歲Y個月`，自動計算）
- 最新身高 / 體重

**展開狀態**（點擊卡片任意位置切換）顯示：
- 上次就醫紀錄摘要（`MedicalVisit.diagnosisSummary`，最新一筆）
- 下次就醫或疫苗排程時間（`MedicalVisit.visitDate > today` 最近一筆；無資料時顯示「尚無排程」）
- 本日 Memo 完整內容（最多 3 筆；底部附「前往編輯」按鈕，導航至 `library/memo/edit`）

**實作注意**：
- 使用 `AnimatedVisibility + expandVertically` 做展開動畫
- 收折狀態用 `rememberSaveable { mutableStateOf(false) }` 管理，不進 ViewModel
- **禁止**使用獨立展開按鈕（箭頭 Icon）；整張卡片 `clickable`
- **禁止**在收折狀態顯示 TwinDiffBadge（移至展開區）
- **禁止**在收折狀態保留身高體重以外的額外資訊

---

### Sprint 6 — 通知排程

- 使用 `WorkManager` + `NotificationCompat`
- `MemoEntity.reminderAt` 不為 null 時，排程對應 `OneTimeWorkRequest`
- Memo 編輯畫面提供「設定提醒」DateTimePicker（可選填，可清除）
- `SettingsScreen` 提供通知總開關（DataStore key：`notifications_enabled`）
- Android 13+ 須在 Manifest 宣告 `POST_NOTIFICATIONS` 並在 Runtime 申請
- **不實作**多寶寶 Profile 切換 UI
- **不實作**語言切換

---

## 就醫畫面規範（feature/medical）

- `MedicalVisitCard`：**整張卡片 clickable 展開**，移除獨立展開按鈕
- 展開後顯示：`diagnosisSummary`、`prescriptions`、`careInstructions` 及操作按鈕列（編輯、刪除）
- **編輯畫面** `MedicalEditScreen`（路由 `medical/edit?visitId={id}&childId={childId}`）包含欄位：
  - `visitDate`：DatePickerDialog（Material3），預設今天
  - `hospital`：醫院名稱
  - `department`：科別
  - `diagnosis`：診斷 / 備註
  - `diagnosisSummary`：AI 摘要（可手動編輯，附「AI 整理僅供參考」提示）
  - `prescriptions`：用藥摘要（可手動編輯）
  - `careInstructions`：照護指示（可手動編輯）
- 新增與編輯共用同一 `MedicalEditScreen`（`visitId=-1` 為新增）
- **禁止**維持舊的 `NewMedicalVisitDialog`

---

## 成長紀錄規範（feature/growth）

- **編輯畫面** `GrowthEditScreen`（路由 `growth/edit?recordId={id}&childId={childId}`）包含欄位：
  - `recordedAt`：DatePickerDialog，預設今天
  - `weightKg`：體重（kg）
  - `heightCm`：身高（cm）
  - `headCircumferenceCm`：頭圍（cm，可選填）
- 新增與編輯共用同一 Screen（`recordId=-1` 為新增）
- **禁止**維持舊的 `NewGrowthRecordDialog`

---

## BottomNavigation 標籤規範

- 「疫苗」分頁標籤改為「**疫苗 就醫**」
- 對應 `BottomNavItem` 的 `label` 欄位修改即可；路由不變

---

## 暫緩 / 禁止實作項目

| 項目 | 狀態 |
|------|------|
| Firebase Auth / Firestore（Phase E） | 暫緩，留位模組維持空白 |
| Google Drive 備份（Phase F 剩餘） | 暫緩 |
| 多寶寶 Profile 切換 UI | 已刪除 |
| 語言切換（多語系） | 已刪除 |
| WHO 官方 LMS CSV 完整替換 | 暫緩（stub 對 3 歲精度足夠）|
| PDF 報表輸出 | 暫緩 |
| Widget | 暫緩 |
| feature/vaccine 完整功能 | 暫緩，目錄留位 |

---

## 禁止行為

- ❌ 修改 `core/model` 欄位但未同步更新 Room Entity 與 Migration
- ❌ 移除或繞過 `RateLimiter`
- ❌ 在 `core/firebase` / `core/drive` 留位模組中新增實作邏輯
- ❌ 在 Composable 內使用 `remember { ViewModel() }`
- ❌ 新增 `DailyLogScreen`、`DailyLogViewModel`、`DailyLogEntity` 等已廢棄設計
- ❌ 在就醫 / 成長紀錄維持 Dialog 新增流程（改用全頁 EditScreen）
- ❌ 在 `MedicalVisitCard` 或 `ChildSummaryCard` 使用獨立展開按鈕

---

## 常用指令

```bash
./gradlew assembleDebug   # 編譯
./gradlew test            # 單元測試
./gradlew lint            # Lint 檢查
./gradlew clean           # 清除 build cache（KSP 異常時使用）
```

---

## 參考文件

- [`AGENTS.md`](../AGENTS.md) — 基礎架構規範（本文件為擴充版）
- [`README.md`](../README.md) — 模組結構總覽
- [`TODO.md`](../TODO.md) — 各 Phase 任務清單
- [`docs/SYNC_ARCHITECTURE.md`](./SYNC_ARCHITECTURE.md) — Firebase + Drive 同步架構規格
