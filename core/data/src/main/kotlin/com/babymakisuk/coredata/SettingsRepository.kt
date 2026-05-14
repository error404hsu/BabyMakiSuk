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

    // ── 雲端 AI 開關 ─────────────────────────────────────

    /** 雲端 AI 開關狀態，預設啟用。Key 由編譯時注入，使用者只能開/關此功能 */
    val aiCloudEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SettingsPreferences.AI_CLOUD_ENABLED] ?: true
    }

    suspend fun setAiCloudEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsPreferences.AI_CLOUD_ENABLED] = enabled
        }
    }

    // ── 通知開關 ──────────────────────────────────────────

    /** 通知總開關，預設啟用 */
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SettingsPreferences.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SettingsPreferences.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // ── 匯出 / 匯入 ──────────────────────────────────────────

    suspend fun buildExportIntent(): Intent = backupManager.exportToShareIntent()

    suspend fun importFromUri(uri: Uri, merge: Boolean = true) =
        backupManager.importFromUri(uri, merge)
}
