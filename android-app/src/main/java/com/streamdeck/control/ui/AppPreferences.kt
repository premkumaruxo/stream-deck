package com.streamdeck.control.ui

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class AppThemeMode { DARK, LIGHT, SYSTEM }

private val Context.appPreferencesStore by preferencesDataStore(name = "streamdeck_appearance")

class AppPreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val onlyButtonModeKey = booleanPreferencesKey("only_button_mode")
    private val autoIconsKey = booleanPreferencesKey("auto_icons")
    private val hapticsKey = booleanPreferencesKey("haptics")
    private val automaticUpdatesKey = booleanPreferencesKey("automatic_update_check")
    private val lastSuccessfulUpdateCheckKey = longPreferencesKey("last_successful_update_check")
    private val lastUpdateCheckAttemptKey = longPreferencesKey("last_update_check_attempt")
    private val cachedUpdateMetadataKey = stringPreferencesKey("cached_update_metadata")
    private val reminderSnoozeUntilKey = longPreferencesKey("update_reminder_snooze_until")
    private val ignoredUpdateVersionKey = stringPreferencesKey("ignored_update_version")

    val theme: Flow<AppThemeMode> = context.appPreferencesStore.data.map { preferences ->
        when (preferences[themeKey]) {
            "dark" -> AppThemeMode.DARK
            "light" -> AppThemeMode.LIGHT
            else -> AppThemeMode.SYSTEM
        }
    }
    val onlyButtonMode: Flow<Boolean> =
        context.appPreferencesStore.data.map { it[onlyButtonModeKey] ?: false }
    val autoIcons: Flow<Boolean> =
        context.appPreferencesStore.data.map { it[autoIconsKey] ?: true }
    val haptics: Flow<Boolean> =
        context.appPreferencesStore.data.map { it[hapticsKey] ?: true }
    val automaticUpdateCheck: Flow<Boolean> =
        context.appPreferencesStore.data.map { it[automaticUpdatesKey] ?: true }

    suspend fun setTheme(mode: AppThemeMode) {
        context.appPreferencesStore.edit { it[themeKey] = mode.name.lowercase() }
    }

    suspend fun setOnlyButtonMode(enabled: Boolean) {
        context.appPreferencesStore.edit { it[onlyButtonModeKey] = enabled }
    }

    suspend fun setAutoIcons(enabled: Boolean) {
        context.appPreferencesStore.edit { it[autoIconsKey] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.appPreferencesStore.edit { it[hapticsKey] = enabled }
    }

    suspend fun setAutomaticUpdateCheck(enabled: Boolean) {
        context.appPreferencesStore.edit { it[automaticUpdatesKey] = enabled }
    }

    suspend fun lastSuccessfulUpdateCheck(): Long =
        context.appPreferencesStore.data.first()[lastSuccessfulUpdateCheckKey] ?: 0L

    suspend fun lastUpdateCheckAttempt(): Long =
        context.appPreferencesStore.data.first()[lastUpdateCheckAttemptKey] ?: 0L

    suspend fun recordUpdateCheckAttempt(timestamp: Long) {
        context.appPreferencesStore.edit { it[lastUpdateCheckAttemptKey] = timestamp }
    }

    suspend fun recordSuccessfulUpdateCheck(timestamp: Long, metadata: String) {
        context.appPreferencesStore.edit {
            it[lastSuccessfulUpdateCheckKey] = timestamp
            it[lastUpdateCheckAttemptKey] = timestamp
            it[cachedUpdateMetadataKey] = metadata
        }
    }

    suspend fun cachedUpdateMetadata(): String? =
        context.appPreferencesStore.data.first()[cachedUpdateMetadataKey]

    suspend fun updateReminderSnoozeUntil(): Long =
        context.appPreferencesStore.data.first()[reminderSnoozeUntilKey] ?: 0L

    suspend fun ignoredUpdateVersion(): String =
        context.appPreferencesStore.data.first()[ignoredUpdateVersionKey].orEmpty()

    suspend fun snoozeUpdateReminder(until: Long) {
        context.appPreferencesStore.edit { it[reminderSnoozeUntilKey] = until }
    }

    suspend fun ignoreUpdateVersion(version: String) {
        context.appPreferencesStore.edit {
            it[ignoredUpdateVersionKey] = version
            it.remove(reminderSnoozeUntilKey)
        }
    }

    suspend fun clearUpdateReminderSnooze() {
        context.appPreferencesStore.edit { it.remove(reminderSnoozeUntilKey) }
    }
}
