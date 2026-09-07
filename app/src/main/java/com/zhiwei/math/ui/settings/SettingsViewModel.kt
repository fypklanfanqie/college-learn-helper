package com.zhiwei.math.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.prefs.AppearanceSettings
import com.zhiwei.math.data.prefs.GlassSettings
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.GlassMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {

    val appearance: StateFlow<AppearanceSettings> =
        settings.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    val glass: StateFlow<GlassSettings> =
        settings.glass.stateIn(viewModelScope, SharingStarted.Eagerly, GlassSettings())

    fun setTheme(theme: String) {
        viewModelScope.launch { settings.setAppearance(appearance.value.copy(theme = theme)) }
    }

    fun setFont(font: String) {
        viewModelScope.launch { settings.setAppearance(appearance.value.copy(font = font)) }
    }

    fun setChatBackground(uri: String) {
        viewModelScope.launch { settings.setAppearance(appearance.value.copy(chatBackgroundUri = uri)) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { settings.setAppearance(appearance.value.copy(haptics = enabled)) }
    }

    fun setGlassMode(mode: GlassMode) {
        viewModelScope.launch {
            settings.setGlass(glass.value.copy(mode = mode.name.lowercase()))
        }
    }

    fun updateGlass(value: GlassSettings) {
        viewModelScope.launch { settings.setGlass(value) }
    }
}
