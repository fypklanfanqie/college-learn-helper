package com.zhiwei.math.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.prefs.AppearanceSettings
import com.zhiwei.math.data.prefs.GlassSettings
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.GlassMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {

    val appearance: StateFlow<AppearanceSettings> =
        settings.appearance.stateIn(viewModelScope, SharingStarted.Eagerly, AppearanceSettings())

    val liquidDisabled: StateFlow<Boolean> =
        settings.liquidGlassDisabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * 玻璃参数权威状态（滑杆防抽搐的核心配套）：
     * - UI 即时读写本地值：滑杆拖动跟手，不等 DataStore 异步往返；
     * - DataStore 只负责两件事：首次加载注入初始值、后台串行落盘（合并写）；
     * - 用户编辑过之后本地即权威，不再采纳 DataStore 回流——
     *   往返有延迟，滞后的旧值回流会覆盖新拖动值（预览/标签闪烁、
     *   滑块被拽回的直接根因之一）。
     */
    private val _glass = MutableStateFlow(GlassSettings())
    val glass: StateFlow<GlassSettings> = _glass.asStateFlow()

    /** 待落盘的最新参数：StateFlow 去重 + 合并，快速拖动时只写最新值 */
    private val pendingWrite = MutableStateFlow<GlassSettings?>(null)

    /** 用户是否已编辑过（编辑后 DataStore 回流不再采纳） */
    private var edited = false

    init {
        // DataStore → 本地：仅首次加载（edited 后跳过，防止滞后回流覆盖新值）
        viewModelScope.launch {
            settings.glass.collect { stored ->
                if (!edited) _glass.value = stored
            }
        }
        // 本地 → DataStore：单协程串行写盘，写完自动取 StateFlow 里合并后的最新值
        viewModelScope.launch {
            pendingWrite.filterNotNull().collect { latest ->
                settings.setGlass(latest)
            }
        }
    }

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
        // 基于【本地权威值】叠加（DataStore 回流可能滞后，旧底会把刚拖完的参数打回去）
        updateGlassInternal(_glass.value.copy(mode = mode.name.lowercase()))
    }

    fun updateGlass(value: GlassSettings) {
        updateGlassInternal(value)
    }

    /** 应用预设（整体覆盖液态参数，保留当前模式为 liquid） */
    fun applyPreset(preset: GlassSettings) {
        updateGlassInternal(preset)
    }

    /** 崩溃自动禁用后用户手动重试液态玻璃 */
    fun reenableLiquid() {
        viewModelScope.launch { settings.reenableLiquidGlass() }
    }

    private fun updateGlassInternal(value: GlassSettings) {
        if (value == _glass.value) return
        edited = true
        _glass.value = value
        pendingWrite.value = value
    }
}
