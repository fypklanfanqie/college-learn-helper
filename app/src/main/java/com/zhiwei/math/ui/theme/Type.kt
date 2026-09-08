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

/**
 * iOS Text Styles 全量排版（计划「排版」节，数值参考 glasense Type.kt + HIG）。
 * 大字号负字距（34sp → -0.4sp）；强调变体 SemiBold。
 */
object IosTextStyles {
    val LargeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.4).sp, fontWeight = FontWeight.Normal)
    val LargeTitleEmphasized = LargeTitle.copy(fontWeight = FontWeight.SemiBold)
    val Title1 = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.sp)
    val Title1Emphasized = Title1.copy(fontWeight = FontWeight.SemiBold)
    val Title2 = TextStyle(fontSize = 24.sp, lineHeight = 28.sp)
    val Title2Emphasized = Title2.copy(fontWeight = FontWeight.SemiBold)
    val Title3 = TextStyle(fontSize = 20.sp, lineHeight = 24.sp)
    val Title3Emphasized = Title3.copy(fontWeight = FontWeight.SemiBold)
    val Headline = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val Body = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp)
    val Callout = TextStyle(fontSize = 16.sp, lineHeight = 21.sp)
    val Subheadline = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    val SubheadlineEmphasized = Subheadline.copy(fontWeight = FontWeight.SemiBold)
    val Footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val Caption1 = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val Caption2 = TextStyle(fontSize = 11.sp, lineHeight = 13.sp)
}

/**
 * iOS Text Styles 映射进 MaterialTheme.typography：
 * 屏幕代码继续用 MaterialTheme.typography 引用，主题层替换实现（计划要求）。
 */
fun buildZhiweiTypography(fontFamily: FontFamily): Typography {
    fun t(style: TextStyle) = style.copy(fontFamily = fontFamily)
    return Typography(
        displayLarge = t(IosTextStyles.LargeTitle),
        displayMedium = t(IosTextStyles.Title1),
        displaySmall = t(IosTextStyles.Title2),
        headlineLarge = t(IosTextStyles.Title1Emphasized),
        headlineMedium = t(IosTextStyles.Title2Emphasized),
        headlineSmall = t(IosTextStyles.Title3Emphasized),
        titleLarge = t(IosTextStyles.Title3Emphasized),
        titleMedium = t(IosTextStyles.Headline),
        titleSmall = t(IosTextStyles.SubheadlineEmphasized),
        bodyLarge = t(IosTextStyles.Body),
        bodyMedium = t(IosTextStyles.Subheadline),
        bodySmall = t(IosTextStyles.Footnote),
        labelLarge = t(IosTextStyles.Callout),
        labelMedium = t(IosTextStyles.Caption1),
        labelSmall = t(IosTextStyles.Caption2),
    )
}

val ZhiweiTypography = buildZhiweiTypography(MiSans)

/** 兼容旧引用（LargeTitle 34sp SemiBold） */
val LargeTitle = IosTextStyles.LargeTitleEmphasized.copy(fontFamily = MiSans)
