package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lifeguard_prefs")

class LifeGuardPreferences(private val context: Context) {

    companion object {
        val KEY_PROTECTION_PROFILE = stringPreferencesKey("protection_profile")
        val KEY_WEB_SHIELD_ENABLED = booleanPreferencesKey("web_shield_enabled")
        val KEY_BREATHING_DELAY_SECONDS = intPreferencesKey("breathing_delay_seconds")
        val KEY_STRICT_MODE_COOLDOWN = intPreferencesKey("strict_mode_cooldown")
        val KEY_NIGHT_SLEEP_PROTECTION = booleanPreferencesKey("night_sleep_protection")
        val KEY_PIN_PROTECTION_ENABLED = booleanPreferencesKey("pin_protection_enabled")
        val KEY_SECURITY_PIN = stringPreferencesKey("security_pin")
        val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }

    val protectionProfile: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROTECTION_PROFILE] ?: "BALANCED"
    }

    val isWebShieldEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WEB_SHIELD_ENABLED] ?: false
    }

    val breathingDelaySeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_BREATHING_DELAY_SECONDS] ?: 10
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_DARK_THEME] ?: true
    }

    val isNightSleepProtection: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NIGHT_SLEEP_PROTECTION] ?: true
    }

    suspend fun setProtectionProfile(profile: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PROTECTION_PROFILE] = profile
        }
    }

    suspend fun setWebShieldEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WEB_SHIELD_ENABLED] = enabled
        }
    }

    suspend fun setBreathingDelaySeconds(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BREATHING_DELAY_SECONDS] = seconds
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_DARK_THEME] = enabled
        }
    }

    suspend fun setNightSleepProtection(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NIGHT_SLEEP_PROTECTION] = enabled
        }
    }
}
