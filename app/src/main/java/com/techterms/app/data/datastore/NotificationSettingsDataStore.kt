package com.techterms.app.data.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val TAG = "TechTerms"

private val Context.notificationSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { exception ->
        Log.e(TAG, "DataStore corrupted, resetting to defaults", exception)
        emptyPreferences()
    }
)

enum class ThemeMode { SYSTEM, LIGHT, PAPER, DARK }

data class NotificationSettings(
    val enabled: Boolean = DEFAULT_ENABLED,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
    val devTermsOnly: Boolean = DEFAULT_DEV_TERMS_ONLY
) {
    companion object {
        const val DEFAULT_ENABLED = true
        const val DEFAULT_HOUR = 8
        const val DEFAULT_MINUTE = 0
        const val DEFAULT_DEV_TERMS_ONLY = false
    }
}

class NotificationSettingsDataStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("notifications_enabled")
        val HOUR = intPreferencesKey("notification_hour")
        val MINUTE = intPreferencesKey("notification_minute")
        val DEV_TERMS_ONLY = booleanPreferencesKey("dev_terms_only")
        val DATA_VERSION = intPreferencesKey("terms_data_version")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<NotificationSettings> = context.notificationSettingsDataStore.data
        .catch { exception ->
            Log.e(TAG, "Failed to read DataStore settings", exception)
            emit(emptyPreferences())
        }
        .map { prefs ->
            NotificationSettings(
                enabled = prefs[Keys.ENABLED] ?: NotificationSettings.DEFAULT_ENABLED,
                hour = (prefs[Keys.HOUR] ?: NotificationSettings.DEFAULT_HOUR).coerceIn(0, 23),
                minute = (prefs[Keys.MINUTE] ?: NotificationSettings.DEFAULT_MINUTE).coerceIn(0, 59),
                devTermsOnly = prefs[Keys.DEV_TERMS_ONLY] ?: NotificationSettings.DEFAULT_DEV_TERMS_ONLY
            )
        }

    val themeModeFlow: Flow<ThemeMode> = context.notificationSettingsDataStore.data
        .catch { exception ->
            Log.e(TAG, "Failed to read theme mode", exception)
            emit(emptyPreferences())
        }
        .map { prefs ->
            runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: "") }
                .getOrDefault(ThemeMode.SYSTEM)
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.notificationSettingsDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun getSettings(): NotificationSettings = settingsFlow.first()

    suspend fun setEnabled(enabled: Boolean) {
        context.notificationSettingsDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = enabled
        }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.notificationSettingsDataStore.edit { prefs ->
            prefs[Keys.HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setDevTermsOnly(devTermsOnly: Boolean) {
        context.notificationSettingsDataStore.edit { prefs ->
            prefs[Keys.DEV_TERMS_ONLY] = devTermsOnly
        }
    }

    suspend fun getDataVersion(): Int {
        return context.notificationSettingsDataStore.data
            .catch { exception ->
                Log.e(TAG, "Failed to read DataStore dataVersion", exception)
                emit(emptyPreferences())
            }
            .map { prefs -> prefs[Keys.DATA_VERSION] ?: 0 }
            .first()
    }

    suspend fun setDataVersion(version: Int) {
        context.notificationSettingsDataStore.edit { prefs ->
            prefs[Keys.DATA_VERSION] = version
        }
    }
}
