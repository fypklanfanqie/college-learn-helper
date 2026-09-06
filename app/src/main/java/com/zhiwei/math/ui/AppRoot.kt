package com.zhiwei.math.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.zhiwei.math.data.prefs.AppearanceSettings
import com.zhiwei.math.data.prefs.GlassSettings
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.ProvideGlassContent
import com.zhiwei.math.ui.theme.ZhiweiTheme
import org.koin.compose.koinInject

/**
 * 应用根：消费外观设置（日夜/字体）与玻璃设置（模式/参数），向下提供玻璃上下文。
 */
@Composable
fun AppRoot(settings: SettingsStore = koinInject()) {
    val appearance by settings.appearance.collectAsState(initial = AppearanceSettings())
    val glass by settings.glass.collectAsState(initial = GlassSettings())

    val dark = when (appearance.theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    ZhiweiTheme(darkTheme = dark, useSystemFont = appearance.font == "system") {
        ProvideGlassContent(glass = glass, isDark = dark) {
            AppNavHost(settings)
        }
    }
}
