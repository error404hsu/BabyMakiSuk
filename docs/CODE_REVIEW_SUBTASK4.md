# Code Review — 子任務 4：依賴注入 + ViewModels

> 審查日期：2026-05-19
> 審查範圍：所有 Hilt Modules + MedicalViewModel、GrowthViewModel、HomeViewModel、MedicalEditViewModel

---

## 🔴 嚴重問題

### 1. ViewModel 直接操作 DAO，違反 Repository 模式

**違規檔案：**
- `MedicalViewModel.kt:37` — injects `medicalDao: MedicalDao`
- `HomeViewModel.kt:31` — injects `medicalDao: MedicalDao`
- `MedicalEditViewModel.kt:46` — injects `medicalDao: MedicalDao`

**Agents.md 明確規範：**
> ViewModel 只呼叫 Repository；禁止在 ViewModel 直接操作 DAO。

多個 ViewModel 直接呼叫 `medicalDao.upsert()`、`medicalDao.delete()`、`medicalDao.observeByChild()`，跳過了 Repository 層。

**建議：** 建立 `MedicalRepository` interface + `DefaultMedicalRepository`，將 DAO 操作封裝在 Repository 中。ViewModel 只依賴 `MedicalRepository`。

---

### 2. HomeViewModel 硬編碼預設雙胞胎資料

**檔案：** `HomeViewModel.kt:50-64`

```kotlin
val defaultBoy = ChildProfile(id = 1L, name = "小明", ...)
val defaultGirl = ChildProfile(id = 2L, name = "小美", ...)
```

- 硬編碼 `id = 1L / 2L`，但 `ChildProfileEntity` 使用 `@PrimaryKey(autoGenerate = true)`
- Room 可能自動生成衝突的 ID（若使用者手動新增孩子後 ID 遞增撞上 1/2）
- 資料夾在 ViewModel 中不適合，應移至 `ChildRepository` 或透過 Seed Data Worker 處理

**建議：** 移除硬編碼，改由首次啟動引導（Onboarding Screen）讓使用者自定義孩子資訊；或將 seed 邏輯移至 Repository 層。

---

### 3. combine 使用 Array<Any?> + UNCHECKED_CAST

**檔案：**
- `GrowthViewModel.kt:80-89`
- `HomeViewModel.kt:153-164`

```kotlin
combine(flow1, flow2, flow3, ...) { array ->
    @Suppress("UNCHECKED_CAST")
    DataSnapshot(children = array[0] as List<ChildProfile>, ...)
}
```

`combine` 用於 4+ 個 Flow 時回傳 `Array<Any?>`。索引順序若維護不善，極易造成執行期 `ClassCastException`。

**建議：** 將過多的 combine 拆解為巢狀 combine 或使用 `combine` 的 typed 重載（最多 5 個參數）。或為資料定義專用 data class 並使用 `combine { a, b, c, d -> ... }`。

---

### 4. HomeViewModel 構造函式超過 7 個依賴 (God Constructor)

**檔案：** `HomeViewModel.kt:26-34`

```kotlin
class HomeViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val growthRepository: GrowthRepository,
    private val toiletRepository: ToiletRepository,
    private val vaccineReminderRepository: VaccineReminderRepository,
    private val memoRepository: MemoRepository,
    private val medicalDao: MedicalDao,          // ← 應替換為 Repository
    private val systemReminderRepository: SystemReminderRepository,
    private val monthlyReportRepository: MonthlyReportRepository,
)
```

8 個建構子參數 + 複雜的資料組裝邏輯（`loadData()` 中組合 6 個 Flow），違反 SRP。

**建議：** 將資料組裝邏輯萃取為 `HomeDataAggregator` UseCase，或在 Feature 層建立專用的 `HomeRepository` 封裝多資料源。MedicalDao 替換為 MedicalRepository 後可減少一個參數。

---

## 🟡 架構與設計問題

### 5. @ApplicationScope 使用 Dispatchers.Default

**檔案：** `DispatcherModule.kt:33-34`

```kotlin
fun provideAppScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

此 scope 被 `DefaultChildRepository` 用於 `shareIn(appScope, ...)`。`Dispatchers.Default` 是 CPU-bound thread pool，但 Room 操作是 IO-bound。可能導致 Default 執行緒被 IO 阻塞。

**建議：** 改為 `Dispatchers.IO` 或建立兩個獨立的 Scope qualifier。

---

### 6. canEditData / canUseLocalAi 重複 pattern

**檔案：** `MedicalViewModel.kt:45-51`, `GrowthViewModel.kt:65-67`

```kotlin
val canEditData: StateFlow<Boolean> = settingsRepo.userRoleFlow
    .map { it.canEditData }
    .stateIn(viewModelScope, WhileSubscribed(5_000), false)
```

三個 ViewModel 重複相同的 `settingsRepo.userRoleFlow` 映射邏輯。若權限邏輯變更，需修改所有 ViewModel。

**建議：** 建立一個共享的 `UserRoleManager`（`@Singleton`），封裝 `userRoleFlow` 的 mapping 邏輯。

---

### 7. MedicalEditViewModel 中 save() 方法過長 (60+ 行)

**檔案：** `MedicalEditViewModel.kt:196-261`

單一 `save()` 方法中同時處理：驗證、Entity 轉換、Room 存檔、Firebase 上傳、圖片路徑更新、檔案清理。違反 SRP。

**建議：** 拆分為 `saveToLocal()` → `uploadImageIfNeeded()` → `cleanup()`。

---

## ✅ 做得好的地方

- **Hilt 模組結構清晰**：5 個模組各自負責 Database / Data / Dispatcher / Ai / Firebase，職責分離
- **@Binds 正確使用**：`DataModule` 和 `FirebaseModule` 用 `@Binds` 綁定 interface 到 impl
- **@Singleton 範圍合理**：DAO、Dispatcher、Repository 均正確標註 Singleton scope
- **UiState sealed interface**：MedicalUiState、GrowthListUiState 正確實作 MVI sealed interface
- **SharedFlow for UiEvent**：MedicalEditViewModel 的 `savedEvent` 正確使用 SharedFlow 傳遞一次性事件

---

**建議修復優先級：** #1 (DAO 直接存取) > #2 (硬編碼 ID) > #3 (Array combine) > #4 (God constructor)
