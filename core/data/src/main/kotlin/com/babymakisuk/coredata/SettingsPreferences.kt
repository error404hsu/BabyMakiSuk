package com.babymakisuk.coredata

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore Preferences 的 Key 定義中心。
 * 所有設定項目的 key 集中管理，避免字串散落各處。
 */
object SettingsPreferences {
    /** 深色模式選項："SYSTEM" | "LIGHT" | "DARK" */
    val DARK_MODE_KEY = stringPreferencesKey("dark_mode")

    /** 使用者角色："DATA_MANAGER" | "AI_OPERATOR" | "ADMIN" | "NONE" */
    val USER_ROLE_KEY = stringPreferencesKey("user_role")

    /** 雲端 AI 開關（預設啟用）；Key 由編譯時注入，App 內不可變更 */
    val AI_CLOUD_ENABLED = booleanPreferencesKey("ai_cloud_enabled")

    /** 通知總開關（預設啟用） */
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

    /** 自動建立本地備份（預設關閉） */
    val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")

    /** 上次備份成功的時間戳記 (ISO 8601 格式) */
    val LAST_BACKUP_TIME = stringPreferencesKey("last_backup_time")

    /** 開發者模式開關 */
    val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
}
