package com.zhiwei.math.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * iOS 语义色 CompositionLocal：屏幕直接取用（比 MaterialTheme 色槽更贴 HIG 命名）。
 * 由 ZhiweiTheme 按深浅色提供。
 */
data class IosPalette(
    val background: Color,
    val card: Color,
    val elevated: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val fill: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val isDark: Boolean,
)

val LocalIosPalette = staticCompositionLocalOf {
    IosPalette(
        background = IosColors.LightBackground,
        card = IosColors.LightCard,
        elevated = IosColors.LightElevated,
        label = IosColors.LightLabel,
        secondaryLabel = IosColors.LightSecondaryLabel,
        tertiaryLabel = IosColors.LightTertiaryLabel,
        separator = IosColors.LightSeparator,
        fill = IosColors.LightFill,
        blue = IosColors.BlueLight,
        green = IosColors.GreenLight,
        red = IosColors.RedLight,
        orange = IosColors.OrangeLight,
        yellow = IosColors.YellowLight,
        isDark = false,
    )
}

private val LightColorScheme = lightColorScheme(
    primary = IosColors.BlueLight,
    onPrimary = Color.White,
    primaryContainer = IosColors.BlueLight.copy(alpha = 0.12f),
    onPrimaryContainer = IosColors.BlueLight,
    secondary = IosColors.TealLight,
    error = IosColors.RedLight,
    onError = Color.White,
    errorContainer = IosColors.RedLight.copy(alpha = 0.12f),
    background = IosColors.LightBackground,
    onBackground = IosColors.LightLabel,
    surface = IosColors.LightCard,
    onSurface = IosColors.LightLabel,
    surfaceVariant = IosColors.LightFill,
    onSurfaceVariant = IosColors.LightSecondaryLabel,
    outline = IosColors.LightSeparator,
)

private val DarkColorScheme = darkColorScheme(
    primary = IosColors.BlueDark,
    onPrimary = Color.White,
    primaryContainer = IosColors.BlueDark.copy(alpha = 0.18f),
    onPrimaryContainer = IosColors.BlueDark,
    secondary = IosColors.TealDark,
    error = IosColors.RedDark,
    onError = Color.White,
    errorContainer = IosColors.RedDark.copy(alpha = 0.18f),
    background = IosColors.DarkBackground,
    onBackground = IosColors.DarkLabel,
    surface = IosColors.DarkCard,
    onSurface = IosColors.DarkLabel,
    surfaceVariant = IosColors.DarkFill,
    onSurfaceVariant = IosColors.DarkSecondaryLabel,
    outline = IosColors.DarkSeparator,
)

private val LightIosPalette = IosPalette(
    background = IosColors.LightBackground, card = IosColors.LightCard,
    elevated = IosColors.LightElevated, label = IosColors.LightLabel,
    secondaryLabel = IosColors.LightSecondaryLabel, tertiaryLabel = IosColors.LightTertiaryLabel,
    separator = IosColors.LightSeparator, fill = IosColors.LightFill,
    blue = IosColors.BlueLight, green = IosColors.GreenLight, red = IosColors.RedLight,
    orange = IosColors.OrangeLight, yellow = IosColors.YellowLight, isDark = false,
)

private val DarkIosPalette = IosPalette(
    background = IosColors.DarkBackground, card = IosColors.DarkCard,
    elevated = IosColors.DarkElevated, label = IosColors.DarkLabel,
    secondaryLabel = IosColors.DarkSecondaryLabel, tertiaryLabel = IosColors.DarkTertiaryLabel,
    separator = IosColors.DarkSeparator, fill = IosColors.DarkFill,
    blue = IosColors.BlueDark, green = IosColors.GreenDark, red = IosColors.RedDark,
    orange = IosColors.OrangeDark, yellow = IosColors.YellowDark, isDark = true,
)

@Composable
fun ZhiweiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useSystemFont: Boolean = false,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalIosPalette provides if (darkTheme) DarkIosPalette else LightIosPalette,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = if (useSystemFont) buildZhiweiTypography(FontFamily.Default) else ZhiweiTypography,
            shapes = IosShapes.materialShapes,
            content = content,
        )
    }
}
