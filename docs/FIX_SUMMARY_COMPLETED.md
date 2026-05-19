# 已完成修復摘要

> 審查+修復日期：2026-05-19
> 最終編譯狀態：BUILD SUCCESSFUL

---

## ✅ 已完成修復

### SubTask 1 — 核心資料層

| # | 問題 | 修復方式 | 檔案 |
|---|------|----------|------|
| 1 | MonthlyReport childId=0L 無法查詢 | 改用 `children.firstOrNull()?.id` | `MonthlyReportRepository.kt:246` |
| 2 | FTS 缺少同步 Trigger | 新增 `MIGRATION_15_16` 建立 3 個 trigger + rebuild | `AppDatabase.kt:487-503`, `DatabaseModule.kt:34` |
| 3 | FTS 搜尋參數未轉義 | 新增 `String.escapeFts()` 過濾特殊字元 | `MonthlyReportRepository.kt:33-36`, `MonthlyReportSearchViewModel.kt:30-31` |
| 4 | analyzePrescription 靜默吞錯誤 | 回傳型別改為 `Result<String>` | `MedicalAiRepository.kt:41,114-124` |

### SubTask 2 — AI 模組

| # | 問題 | 修復方式 | 檔案 |
|---|------|----------|------|
| 1 | User Prompt 注入風險 | 新增 `---BEGIN/END_CONTENT---` 定界符 + injection guard | `AiPromptBuilder.kt:120,161-168,213-221,246` |
| 2 | GLOBAL_CONSTRAINTS 補充注入防護 | 加入「輸入安全」規範 | `AiSystemConstraints.kt:52-55` |

### SubTask 4 — DI + ViewModels

| # | 問題 | 修復方式 | 檔案 |
|---|------|----------|------|
| 1 | ViewModel 直接操作 DAO | 建立 `MedicalRepository` + impl，重構 MedicalViewModel/MedicalEditViewModel/HomeViewModel/DefaultFirestoreMedicalRepository/StorageCleanupWorker | `MedicalRepository.kt` (新), `DataModule.kt`, `MedicalViewModel.kt`, `MedicalEditViewModel.kt`, `HomeViewModel.kt`, `DefaultFirestoreMedicalRepository.kt`, `StorageCleanupWorker.kt` |
| 2 | combine 使用 Array<Any?> + UNCHECKED_CAST | 巢狀用法，使用 typed data class 取代 untyped combine | `GrowthViewModel.kt:69-92,122-128`, `HomeViewModel.kt:145-163,217-223` |

### SubTask 5 — Firebase + Worker

| # | 問題 | 修復方式 | 檔案 |
|---|------|----------|------|
| 1 | observeVisitsWithAiPending(childId=0L) 永遠回傳空 | 新增 `observeAiPending()` DAO 方法，透過 Repository 使用 | `MedicalDao.kt:49-50`, `MedicalRepository.kt:33,67-70`, `DefaultFirestoreMedicalRepository.kt:92-93` |
| 2 | MemoReminderWorker 未用 @HiltWorker | 加上 `@HiltWorker` + `@AssistedInject` | `MemoReminderWorker.kt:19-23` |
| 3 | processAiPending gender/allergies 為空 | 注入 `ChildRepository`，取得孩子資訊 | `DefaultFirestoreMedicalRepository.kt:112-118` |

---

## 🟡 因低風險暫不變更

| # | 原始問題 | 原因 |
|---|----------|------|
| S1-9 | Migration 內部開發註解 | 不影響運作，Migration 不可修改 |
| S1-11 | fixExifRotation 兩次讀取 Uri | 效能影響極微 |
| S1-12 | saveToInternal 無自動清理 | `cleanupOldFiles()` 已在 MedicalEditViewModel.save() 末端呼叫 |
| S2-3 | BitmapUtils 雙重編碼 | 需要較大的重構 |
| S2-5 | Fallback Chain 無 Retry | 架構變更，需更多設計 |
| S3-1 | innerPadding 只處理 bottom | Scaffold 無 topBar，bottom padding 已正確 |
| S3-2 | BottomBar 判別邏輯脆弱 | 目前 route 命名設計下無 bug |
| S3-5 | childId type 不一致 (String vs Long) | 需要大規模協調重構，影響 6+ 個 Shelf Screen/ViewModel |
| S4-2 | 硬編碼預設雙胞胎 | 符合雙胞胎 App 設計初衷，變更會影響首次啟動體驗 |
| S5-4 | getUsedBytes 效能 | 未出現在任何 call site |

## 🆕 後續建議 (TODO)

1. **[TEST] 新增單元測試** — 所有 `DefaultXxxRepository` 應有對應的 fake implementation
2. **[REFACTOR] Library Shelf childId 型別統一** — 將 `String` → `Long`，建立 shared ShelfRepository
3. **[PERF] MonthlyReportRepository N+1** — 改用 `@Transaction` + JOIN 查詢
4. **[FEAT] Firebase 雙向同步** — `FirestoreChildRepository.observeAllChildren()` 目前 combine 邏輯簡單，可加入衝突解決策略
5. **[DX] 移除 `AiDispatcher.execute()` deprecated path** — 待所有呼叫端遷移至 `executeWithSystemPrompt()`
