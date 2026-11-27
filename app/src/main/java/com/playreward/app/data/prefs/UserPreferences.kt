package com.playreward.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.playreward.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "playreward_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_KEEP_LOGGED_IN = booleanPreferencesKey("keep_logged_in")
        private val KEY_ADMIN_BYPASS_UID = stringPreferencesKey("admin_bypass_uid")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_BG_SERVICE_ENABLED = booleanPreferencesKey("bg_service_enabled")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            ThemeMode.DARK.name -> ThemeMode.DARK
            ThemeMode.SYSTEM.name -> ThemeMode.SYSTEM
            else -> ThemeMode.LIGHT
        }
    }

    val onboardingDoneFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val keepLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEEP_LOGGED_IN] ?: true }
    val localAdminBypassUidFlow: Flow<String> = context.dataStore.data.map { it[KEY_ADMIN_BYPASS_UID] ?: "" }
    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_SOUND_ENABLED] ?: true }
    val hapticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAPTICS_ENABLED] ?: true }
    val bgServiceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_BG_SERVICE_ENABLED] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[KEY_THEME] = mode.name } }
    suspend fun setOnboardingDone(done: Boolean) { context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done } }
    suspend fun setKeepLoggedIn(keep: Boolean) { context.dataStore.edit { it[KEY_KEEP_LOGGED_IN] = keep } }
    suspend fun isKeepLoggedIn(): Boolean = keepLoggedInFlow.first()
    suspend fun setLocalAdminBypassUid(uid: String) { context.dataStore.edit { it[KEY_ADMIN_BYPASS_UID] = uid.trim() } }
    suspend fun setSoundEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled } }
    suspend fun setHapticsEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_HAPTICS_ENABLED] = enabled } }
    suspend fun setBgServiceEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_BG_SERVICE_ENABLED] = enabled } }
}
