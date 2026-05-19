# AGENTS.md — BabyMakiSuk AI Agent 操作指南

> 供 AI Coding Agent（Codex、Copilot Workspace、Cursor 等）協助開發時使用。

---

## 專案概述

**BabyMakiSuk** 是一款幼兒管理 Android App，以雙胞胎家庭為核心受眾。

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
feature/{name}/      - 各功能模組，每個模組含 Screen / ViewModel / UiState
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
- 目前版本為 **v14**；新版 Migration 請依序命名 `MIGRATION_14_15`。

## Worker 規範

- 使用 `@HiltWorker` + `@AssistedInject` 注入依賴（範例：`DataRetentionWorker`）。
- Application 需實作 `Configuration.Provider` 並注入 `HiltWorkerFactory`。
- 週期性 Worker 使用 `PeriodicWorkRequestBuilder`，約束條件使用 `Constraints.Builder`。
- 所有 WorkManager 排程在 `BabyMakiSukApplication.onCreate()` 中統一初始化。

---

## Navigation 規範

- 所有路由字串定義於 `BabyMakiSukNavHost`；新增路由須在此統一登記。
- 子頁面須在 NavHost 中隱藏 BottomBar。
- AI Portal 路由格式：`ai_portal?presetHint={AiPreset.name}`。

---

## 禁止行為

- ❌ 修改 `core/model` 欄位但未同步更新 Room Entity 與 Migration。
- ❌ 移除或繞過 `RateLimiter`（防止 API 帳單異常的安全機制）。
- ❌ 在 `core/firebase` / `core/drive` 留位模組中新增實作邏輯。
- ❌ 在 Composable 內使用 `remember { ViewModel() }`。

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

- [`README.md`](./README.md) — 模組結構總覽
- [`TODO.md`](./TODO.md) — 各 Phase 任務清單
- [`docs/SYNC_ARCHITECTURE.md`](./docs/SYNC_ARCHITECTURE.md) — Firebase + Drive 同步架構規格
- [`docs/DATA_RETENTION_STRATEGY.md`](./docs/DATA_RETENTION_STRATEGY.md) — 資料留存、清理與 Worker 策略（Phase 1-3 ✅ 已完成）
