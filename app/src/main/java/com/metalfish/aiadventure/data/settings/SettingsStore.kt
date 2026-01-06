package com.metalfish.aiadventure.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val KEY_DARK = booleanPreferencesKey("dark_theme")
        private val KEY_TEXT_SIZE = intPreferencesKey("text_size") // 0 small, 1 medium, 2 large
        private val KEY_AUTOSAVE = booleanPreferencesKey("autosave")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            darkTheme = prefs[KEY_DARK] ?: false,
            textSize = prefs[KEY_TEXT_SIZE] ?: 1,
            autosave = prefs[KEY_AUTOSAVE] ?: true
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK] = enabled }
    }

    suspend fun setTextSize(size: Int) {
        context.dataStore.edit { it[KEY_TEXT_SIZE] = size.coerceIn(0, 2) }
    }

    suspend fun setAutosave(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTOSAVE] = enabled }
    }
}

data class AppSettings(
    val darkTheme: Boolean,
    val textSize: Int,
    val autosave: Boolean
)
