package com.metalfish.aiadventure.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metalfish.aiadventure.data.settings.AppSettings
import com.metalfish.aiadventure.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings(
                darkTheme = false,
                textSize = 1,
                autosave = true,
                gigaChatModel = 0,
                musicVolume = 1f,
                sfxVolume = 1f
            )
        )

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { store.setDarkTheme(enabled) }
    }

    fun setTextSize(size: Int) {
        viewModelScope.launch { store.setTextSize(size) }
    }

    fun setAutosave(enabled: Boolean) {
        viewModelScope.launch { store.setAutosave(enabled) }
    }

    fun setGigaChatModel(model: Int) {
        viewModelScope.launch { store.setGigaChatModel(model) }
    }

    fun setMusicVolume(volume: Float) {
        viewModelScope.launch { store.setMusicVolume(volume) }
    }

    fun setSfxVolume(volume: Float) {
        viewModelScope.launch { store.setSfxVolume(volume) }
    }
}
