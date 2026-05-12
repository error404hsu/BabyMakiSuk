package com.babymakisuk.coredata

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
}
