package com.metalfish.aiadventure.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metalfish.aiadventure.data.settings.AppSettings
import com.metalfish.aiadventure.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    settingsStore: SettingsStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsStore.settings
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
}
