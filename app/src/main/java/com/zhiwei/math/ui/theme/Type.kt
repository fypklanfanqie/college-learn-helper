package com.zhiwei.math.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zhiwei.math.R

/**
 * MiSans 字体（免费商用，HyperOS 官方字体，决策 4）。
 * 关于页需展示 "Font: MiSans © Xiaomi"。
 */
val MiSans = FontFamily(
    Font(R.font.misans_regular, FontWeight.Normal),
    Font(R.font.misans_medium, FontWeight.Medium),
    Font(R.font.misans_semibold, FontWeight.SemiBold),
)

val ZhiweiTypography = buildZhiweiTypography(MiSans)

/** 支持字体切换（MiSans / 系统字体，决策 4 的设置项） */
fun buildZhiweiTypography(fontFamily: FontFamily): Typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
    )
}

/** 大标题（iOS Large Title 风格） */
val LargeTitle = TextStyle(
    fontFamily = MiSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 34.sp,
    lineHeight = 41.sp,
)
