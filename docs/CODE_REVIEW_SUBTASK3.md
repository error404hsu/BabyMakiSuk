# Code Review — 子任務 3：應用入口與導航 (app/)

> 審查日期：2026-05-19
> 審查範圍：MainActivity、BabyMakiSukApplication、BabyMakiSukNavHost

---

## 🔴 嚴重問題

### 1. applicationScope 未清理，可能造成短暫生命週期任務洩漏

**檔案：** `BabyMakiSukApplication.kt:26,38-40`

```kotlin
private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

applicationScope.launch {
    authRepository.signInAnonymously()
}
```

`applicationScope` 綁定 Application 生命週期，永不取消。如果 `signInAnonymously()` 內部有任何永續的 Flow collection 或 callback，會造成記憶體洩漏。

**建議：** Firebase 匿名登入是「fire-and-forget」任務，可用 `GlobalScope` 或確保 `authRepository.signInAnonymously()` 不持有長效參考。

---

### 2. MainActivity 保留 ViewModel 參考，違反 Lifecycle 最佳實踐

**檔案：** `MainActivity.kt:23`

```kotlin
private val settingsViewModel: SettingsViewModel by viewModels()
```

Activity 層級持有 ViewModel 參考是可以的，但 `settingsViewModel.darkMode` 在 `setContent{}` 內被收集。若 ViewModel 被 recreate（例如 configuration change），Activity 的屬性不會更新，但因為 Jetpack Compose 的 StateFlow 收集會自動重訂閱，目前無明顯 bug。不過設計上略顯脆弱。

**建議：** 保持現狀（⚠️ 低風險，不需立即修改）。

---

### 3. NavHost 的 innerPadding 只套用在 NavHost 本身

**檔案：** `BabyMakiSukNavHost.kt:164-166`

```kotlin
NavHost(
    navController = navController,
    startDestination = BottomNavItem.Home.route,
    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
)
```

當 BottomBar 顯示時，NavHost 內容被墊高避開 BottomBar。但若目的地頁面本身也需要 `innerPadding`（例如 StatusBar），此處只處理了 `bottom`，缺少 `top` 的系統列處理。

**建議：** 使用 `Scaffold` 已經自動處理 `WindowInsets`，應保留 `innerPadding` 給所有內容使用，而非只選擇 bottom：

```kotlin
content = { innerPadding ->
    NavHost(
        modifier = Modifier.padding(innerPadding),
        ...
    )
}
```

---

### 4. BottomBar 判別邏輯脆弱

**檔案：** `BabyMakiSukNavHost.kt:79-81`

```kotlin
val showBottomBar = currentDestination?.route?.let { route ->
    items.any { it.route == route }
} ?: true
```


只檢查頂層 route 精確匹配。如果某個 composable 的 route 不屬於 `items`（例如 `ai_portal`、`settings/api_test`），BottomBar 會被隱藏，這是預期行為。

但若 route 包含參數（如 `medical/edit?visitId=1&childId=2`），此邏輯仍會正確隱藏 BottomBar，因為 `items.any { it.route == route }` 會是 false。目前正確。

⚠️ 隱藏問題：若未來某個子頁面 route 恰好與某個 BottomNavItem route 同名（例如新增一個 `growth` 子路由但帶參數），此邏輯會誤顯示 BottomBar。

**建議：** 改為檢查 `currentDestination?.route?.startsWith(prefix)` 或使用層級化的 route 命名空間（例如 `sub_growth/view`），從命名上區分父子路由。

---

### 5. 多種 childId 型別 (String vs Long) 散落路由定義

**檔案：** `BabyMakiSukNavHost.kt:268-274,283-289,298-311`

- `library/system-reminder`：`childId` 為 `String`
- `library/aiinsight`：`childId` 為 `String`
- `library/memo`：`childId` 為 `String`
- `library/memo/edit`：`childId` 為 `Long`
- `growth/edit`：`childId` 為 `Long`
- `medical/edit`：`childId` 為 `Long`

三個 library 子路由使用 `String`（`""` 為預設空值），但其他所有子路由使用 `Long`（`-1L` 表示無效值）。型別不一致會在 repository 層引起序列化/解析問題。

**建議：** 統一為 `Long`，其中 `-1L` 表示「不指定 child」，統一 Child ID 型別。

---

## 🟡 架構與設計問題

### 6. ModalNavigationDrawer 和 NavHost 的 BackHandler 不完整

**檔案：** `BabyMakiSukNavHost.kt:87-89`

```kotlin
BackHandler(enabled = drawerState.isOpen) {
    scope.launch { drawerState.close() }
}
```

只處理了 Drawer 開啟時的返回行為。當在子頁面（如 `medical/edit`）時，系統返回鍵會觸發 `navController.popBackStack()` 的預設行為，這是好的。

但如果同時 Drawer 打開又在子頁面？目前的 BackHandler 有效，因為 Drawer 開啟時 BackHandler enabled，關閉後才會到 NavController 的 back stack。

**建議：** 無 immediate issue，但可考慮加入 back stack 為空時退出 App 的雙擊邏輯（optional）。

---

### 7. Preview 包含完整 NavHost 無法正常渲染

**檔案：** `BabyMakiSukNavHost.kt:382-388`

```kotlin
@Preview(showBackground = true)
@Composable
fun BabyMakiSukNavHostPreview() {
    BabyMakiSukTheme {
        BabyMakiSukNavHost()
    }
}
```

Preview 嘗試創建完整 `NavHost` + `rememberNavController()`，但 `LocalContext.current` 在 Preview 中會有特殊行為。`NavHost` 需要 `Context` 才能運作，通常無法在 Preview 中正常渲染。

**建議：** 改用 `@Preview` 單一 Screen 元件，或使用 `NavHostPreview` helper。

---

## ⚠️ 安全與權限

### 8. 通知權限請求結果被靜默處理

**檔案：** `MainActivity.kt:28-33`

```kotlin
private val requestNotificationPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            // 用戶拒絕通知權限，靜默處理
        }
    }
```

拒絕後沒有任何 UI 提示或引導。雖然註解說 SettingsScreen 的開關會引導至系統設定，但初次拒絕時使用者完全沒有回饋，可能造成困惑。

**建議：** 拒絕後顯示一個 short Snackbar 提示使用者可至「設定」中手動開啟通知。

---

## ✅ 做得好的地方

- **Edge-to-Edge 啟用**：正確呼叫 `enableEdgeToEdge()`
- **Worker 初始化集中於 Application.onCreate()**：符合 AGENTS.md 規範
- **Drawer + BottomBar 共存設計**：`ModalNavigationDrawer` 包住 `Scaffold` + `NavHost`，結構清晰
- **Navigation 狀態保存**：使用 `saveState = true / restoreState = true` 保留 Tab 切換時的狀態
- **HiltWorkerFactory 整合**：正確實作 `Configuration.Provider`

---

**建議修復優先級：** #1 (applicationScope leak) > #5 (childId type) > #3 (innerPadding) > #8 (permission UX)
