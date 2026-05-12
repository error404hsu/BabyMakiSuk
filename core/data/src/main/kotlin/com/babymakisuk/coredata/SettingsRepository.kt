package com.babymakisuk.coredata

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.babymakisuk.coredata.backup.BackupManager
import com.babymakisuk.coremodel.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
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

    // ── 使用者角色 ──────────────────────────────────────────

    val userRoleFlow: Flow<UserRole> = context.dataStore.data.map { prefs ->
        runCatching {
            UserRole.valueOf(
                prefs[SettingsPreferences.USER_ROLE_KEY] ?: UserRole.NONE.name
            )
        }.getOrDefault(UserRole.NONE)
    }

    suspend fun setUserRole(role: UserRole) {
        context.dataStore.edit { prefs ->
            prefs[SettingsPreferences.USER_ROLE_KEY] = role.name
        }
    }

    // ── Gemini API Key ────────────────────────────────────

    /** 讀取已儲存的 Gemini API Key，若尚未設定則回傳 null */
    val geminiApiKey: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SettingsPreferences.GEMINI_API_KEY]
    }

    /** 儲存 Gemini API Key 至 DataStore */
    suspend fun setGeminiApiKey(key: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsPreferences.GEMINI_API_KEY] = key
        }
    }

    // ── 匯出 / 匯入 ──────────────────────────────────────────

    /** 建立可分享的 Intent，展示系統分享選單 */
    suspend fun buildExportIntent(): Intent = backupManager.exportToShareIntent()

    /** 從用戶選取的 Uri 匯入資料 */
    suspend fun importFromUri(uri: Uri, merge: Boolean = true) =
        backupManager.importFromUri(uri, merge)
}
