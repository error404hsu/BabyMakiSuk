package com.babymakisuk.coredata.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.babymakisuk.coredata.DarkModeOption
import com.babymakisuk.coredata.SettingsPreferences
import com.babymakisuk.coredata.backup.BackupManager
import com.babymakisuk.coredata.di.IoDispatcher
import com.babymakisuk.coremodel.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// TODO [TEST] All DefaultXxxRepository: add fake implementation of interface for unit testing

private val Context.dataStore by preferencesDataStore(name = "settings")

interface SettingsRepository {
    val darkModeFlow: Flow<DarkModeOption>
    suspend fun setDarkMode(option: DarkModeOption)
    val userRoleFlow: Flow<UserRole>
    suspend fun setUserRole(role: UserRole)
    val aiCloudEnabled: Flow<Boolean>
    suspend fun setAiCloudEnabled(enabled: Boolean)
    val notificationsEnabled: Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)
    val autoBackupEnabled: Flow<Boolean>
    suspend fun setAutoBackupEnabled(enabled: Boolean)
    val lastBackupTime: Flow<String?>
    suspend fun updateLastBackupTime(timestamp: String)
    val developerModeEnabled: Flow<Boolean>
    suspend fun setDeveloperModeEnabled(enabled: Boolean)
    suspend fun buildExportIntent(): Intent
    suspend fun importFromUri(uri: Uri, merge: Boolean = true)
}

@Singleton
class DefaultSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SettingsRepository {

    override val darkModeFlow: Flow<DarkModeOption> = context.dataStore.data
        .map { prefs ->
            runCatching {
                DarkModeOption.valueOf(
                    prefs[SettingsPreferences.DARK_MODE_KEY] ?: DarkModeOption.SYSTEM.name
                )
            }.getOrDefault(DarkModeOption.SYSTEM)
        }
        .flowOn(ioDispatcher)

    override suspend fun setDarkMode(option: DarkModeOption) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                prefs[SettingsPreferences.DARK_MODE_KEY] = option.name
            }
        }
    }

    override val userRoleFlow: Flow<UserRole> = context.dataStore.data
        .map { prefs ->
            runCatching {
                UserRole.valueOf(
                    prefs[SettingsPreferences.USER_ROLE_KEY] ?: UserRole.NONE.name
                )
            }.getOrDefault(UserRole.NONE)
        }
        .flowOn(ioDispatcher)

    override suspend fun setUserRole(role: UserRole) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                prefs[SettingsPreferences.USER_ROLE_KEY] = role.name
            }
        }
    }

    override val aiCloudEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[SettingsPreferences.AI_CLOUD_ENABLED] ?: true
        }
        .flowOn(ioDispatcher)

    override suspend fun setAiCloudEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                prefs[SettingsPreferences.AI_CLOUD_ENABLED] = enabled
            }
        }
    }

    override val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[SettingsPreferences.NOTIFICATIONS_ENABLED] ?: true
        }
        .flowOn(ioDispatcher)

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                prefs[SettingsPreferences.NOTIFICATIONS_ENABLED] = enabled
            }
        }
    }

    override val autoBackupEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[SettingsPreferences.AUTO_BACKUP_ENABLED] ?: false
        }
        .flowOn(ioDispatcher)

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                prefs[SettingsPreferences.AUTO_BACKUP_ENABLED] = enabled
            }
        }
    }

    override val lastBackupTime: Flow<String?> = context.dataStore.data
        .map { prefs ->
            prefs[SettingsPreferences.LAST_BACKUP_TIME]
        }
        .flowOn(ioDispatcher)

    override suspend fun updateLastBackupTime(timestamp: String) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                prefs[SettingsPreferences.LAST_BACKUP_TIME] = timestamp
            }
        }
    }

    override val developerModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            prefs[SettingsPreferences.DEVELOPER_MODE_ENABLED] ?: false
        }
        .flowOn(ioDispatcher)

    override suspend fun setDeveloperModeEnabled(enabled: Boolean) {
        withContext(ioDispatcher) {
            context.dataStore.edit { prefs ->
                prefs[SettingsPreferences.DEVELOPER_MODE_ENABLED] = enabled
            }
        }
    }

    override suspend fun buildExportIntent(): Intent = withContext(ioDispatcher) {
        backupManager.exportToShareIntent()
    }

    override suspend fun importFromUri(uri: Uri, merge: Boolean) {
        withContext(ioDispatcher) {
            backupManager.importFromUri(uri, merge)
        }
    }
}
