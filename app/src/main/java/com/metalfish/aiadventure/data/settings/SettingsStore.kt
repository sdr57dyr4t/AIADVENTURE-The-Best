package com.metalfish.aiadventure.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
        private val KEY_GIGACHAT_MODEL = intPreferencesKey("gigachat_model") // 0 lite, 1 pro, 2 max
        private val KEY_MUSIC_VOLUME = floatPreferencesKey("music_volume") // 0..1
        private val KEY_SFX_VOLUME = floatPreferencesKey("sfx_volume") // 0..1
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            darkTheme = prefs[KEY_DARK] ?: false,
            textSize = prefs[KEY_TEXT_SIZE] ?: 1,
            autosave = prefs[KEY_AUTOSAVE] ?: true,
            gigaChatModel = prefs[KEY_GIGACHAT_MODEL] ?: 0,
            musicVolume = prefs[KEY_MUSIC_VOLUME] ?: 1f,
            sfxVolume = prefs[KEY_SFX_VOLUME] ?: 1f
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

    suspend fun setGigaChatModel(model: Int) {
        context.dataStore.edit { it[KEY_GIGACHAT_MODEL] = model.coerceIn(0, 2) }
    }

    suspend fun setMusicVolume(volume: Float) {
        context.dataStore.edit { it[KEY_MUSIC_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSfxVolume(volume: Float) {
        context.dataStore.edit { it[KEY_SFX_VOLUME] = volume.coerceIn(0f, 1f) }
    }
}

data class AppSettings(
    val darkTheme: Boolean,
    val textSize: Int,
    val autosave: Boolean,
    val gigaChatModel: Int,
    val musicVolume: Float,
    val sfxVolume: Float
)
