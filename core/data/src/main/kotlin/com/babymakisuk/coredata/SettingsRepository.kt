package com.babymakisuk.coredata

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * 設定資料存取層。
 * - 深色模式：透過 DataStore<Preferences> 持久化。
 * - 匯出 / 匯入：委派 BackupManager（Phase E-2 實作）。
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── 深色模式 ──────────────────────────────────────────

    val darkModeFlow: Flow<DarkModeOption> = context.dataStore.data.map { prefs ->
        runCatching {
            DarkModeOption.valueOf(
                prefs[SettingsPreferences.DARK_MODE_KEY] ?: DarkModeOption.SYSTEM.name
            )
        }.getOrDefault(DarkModeOption.SYSTEM)
    }

    suspend fun setDarkMode(option: DarkModeOption) {
        context.dataStore.edit { prefs ->
            prefs[SettingsPreferences.DARK_MODE_KEY] = option.name
        }
    }

    // ── 匯出 / 匯入 (stub，Phase E-2 正式實作) ─────────────

    /**
     * 將所有 Room 資料序列化為 JSON 字串。
     * TODO(Phase E-2)：注入 BackupManager，實作真正的 Room → JSON 邏輯。
     */
    suspend fun exportAllDataAsJson(): String {
        // stub：回傳空物件，正式實作時由 BackupManager 填充
        return "{\"version\":1,\"data\":{}}"
    }

    /**
     * 從 JSON 字串還原資料至 Room。
     * TODO(Phase E-2)：注入 BackupManager，實作 JSON → Room merge 邏輯。
     */
    suspend fun importDataFromJson(json: String) {
        // stub：Phase E-2 實作
    }
}
