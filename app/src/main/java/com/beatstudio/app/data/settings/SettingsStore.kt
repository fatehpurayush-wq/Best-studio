package com.beatstudio.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "beat_studio_prefs")

class SettingsStore(private val context: Context) {
    private val KEY_TOUR_COMPLETED = booleanPreferencesKey("guided_tour_completed")
    private val KEY_PRO_MODE = booleanPreferencesKey("pro_mode_enabled")
    private val KEY_EXPORT_BIT_DEPTH = intPreferencesKey("export_bit_depth")

    val hasCompletedTour: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOUR_COMPLETED] ?: false
    }

    val isProMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PRO_MODE] ?: false
    }

    val exportBitDepth: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_EXPORT_BIT_DEPTH] ?: 16
    }

    suspend fun setTourCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOUR_COMPLETED] = completed
        }
    }

    suspend fun setProMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PRO_MODE] = enabled
        }
    }

    suspend fun setExportBitDepth(bitDepth: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EXPORT_BIT_DEPTH] = bitDepth
        }
    }
}
