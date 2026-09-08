package com.zhiwei.math.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhiwei.math.data.prefs.GlassSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** 玻璃模式（决策 6）：毛玻璃默认；液态玻璃 API 33+ 可选；半透明为降级档 */
enum class GlassMode(val label: String) {
    PLAIN("半透明"),
    FROSTED("毛玻璃"),
    LIQUID("液态玻璃");

    companion object {
        fun fromId(id: String): GlassMode =
            entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: FROSTED
    }
}

/** 液态玻璃可调参数（PROJECT-BRIEF.md 5.1：四滑杆实时生效） */
data class GlassTuning(
    val refractionHeightDp: Int = 24,
    val refractionAmountDp: Int = 56,
    val blurRadiusDp: Int = 20,
    val opacity: Float = 0.7f,
)

val LocalGlassMode = staticCompositionLocalOf { GlassMode.FROSTED }
val LocalGlassTuning = staticCompositionLocalOf { GlassTuning() }
val LocalIsDarkTheme = compositionLocalOf { false }
val LocalHazeState = compositionLocalOf<HazeState?> { null }
/** 震动反馈总开关（设置 → 震动），默认开启 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * 实际玻璃模式解析（决策 2 的 API 分档）：
 * 用户选择优先；超过硬件能力时降级（液态玻璃需 API 33+，毛玻璃需 API 31+，否则半透明）。
 */
fun resolveGlassMode(userModeId: String, sdkInt: Int = Build.VERSION.SDK_INT): GlassMode {
    val user = GlassMode.fromId(userModeId)
    return when (user) {
        GlassMode.LIQUID -> if (sdkInt >= 33) GlassMode.LIQUID else if (sdkInt >= 31) GlassMode.FROSTED else GlassMode.PLAIN
        GlassMode.FROSTED -> if (sdkInt >= 31) GlassMode.FROSTED else GlassMode.PLAIN
        GlassMode.PLAIN -> GlassMode.PLAIN
    }
}

/**
 * 应用级玻璃容器：haze 真实内容模糊（本机实测稳定）+ 液态边缘装饰。
 *
 * 实测结论（小米 2410DPN6CC / HyperOS Android 16 / Adreno 830）：
 * - backdrop 库 drawBackdrop（任何效果）→ RenderThread SIGSEGV（MiBackgroundBlurBlend UAF）；
 * - haze 的 offscreen blur（hazeSource + hazeEffect）→ 稳定，且底层真实内容透过玻璃可见
 *   （参考应用同款观感）。
 *
 * 液态玻璃 = haze 真实内容模糊 + 液态高光光带 + 青/金色散双描边，四滑杆实时生效。
 */
@Composable
fun ProvideGlassContent(
    glass: GlassSettings,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val hazeState = remember { HazeState() }
    val mode = resolveGlassMode(glass.mode)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalGlassMode provides mode,
        LocalGlassTuning provides GlassTuning(
            refractionHeightDp = glass.refractionHeight,
            refractionAmountDp = glass.refractionAmount,
            blurRadiusDp = glass.blurRadius,
            opacity = glass.opacity / 100f,
        ),
        LocalIsDarkTheme provides isDark,
        LocalHazeState provides hazeState,
    ) {
        when (mode) {
            GlassMode.PLAIN -> Box { content() }
            else -> Box(Modifier.hazeSource(hazeState)) { content() }
        }
    }
}

/**
 * 玻璃表面修饰符：真实内容透过玻璃被模糊（haze），叠加液态边缘装饰。
 * - 模糊半径滑杆 → haze 模糊半径（0 = 清晰透底）
 * - 不透明度滑杆 → 着色 veil
 * - 折射高度滑杆 → 液态光带宽度
 * - 折射量滑杆 → 青/金色散双描边错位 + 顶部光泽
 */
fun Modifier.appGlass(cornerRadius: Dp = 0.dp): Modifier = composed {
    val mode = LocalGlassMode.current
    val tuning = LocalGlassTuning.current
    val isDark = LocalIsDarkTheme.current
    val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val composeShape: Shape = RoundedCornerShape(cornerRadius)

    when (mode) {
        GlassMode.PLAIN -> this.background(scrimColor, composeShape)
        else -> {
            val hazeState = LocalHazeState.current
            // 模糊半径：0 = 清晰透底（与最大值对比强烈）
            val blurDp = tuning.blurRadiusDp * 1.2f
            val tintAlpha = (tuning.opacity * 0.7f).coerceIn(0.02f, 0.72f)
            val tint = if (isDark) {
                HazeTint(Color(0xFF1E1E26).copy(alpha = tintAlpha))
            } else {
                HazeTint(Color(0xFFF7F7FB).copy(alpha = tintAlpha))
            }
            val base = if (hazeState != null) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        tints = listOf(tint),
                        blurRadius = blurDp.dp,
                    ),
                )
            } else {
                Modifier.background(scrimColor, composeShape)
            }
            val isLiquid = mode == GlassMode.LIQUID
            val bandPx = (2.2f + tuning.refractionHeightDp / 80f * 8.5f).dp // 折射光带宽度
            val disp = tuning.refractionAmountDp / 96f * 3.2f               // 色散错位 dp
            val dispAlpha = tuning.refractionAmountDp / 96f
            val topSheen = if (isLiquid) 0.22f else 0.12f

            this
                .clip(composeShape)
                .then(base)
                .drawWithContent {
                    drawContent()
                    // 顶部环境光高光
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = topSheen),
                            0.3f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    )
                    if (isLiquid) {
                        val r = cornerRadius.toPx()
                        val band = bandPx.toPx()
                        val d = disp.dp.toPx()
                        // 冷色散：青蓝，向外扩
                        if (d > 0.3f) {
                            drawRoundRect(
                                color = Color(0xFF6EC1FF).copy(alpha = 0.10f + dispAlpha * 0.38f),
                                topLeft = Offset(-d, -d),
                                size = Size(size.width + d * 2f, size.height + d * 2f),
                                cornerRadius = CornerRadius(r + d, r + d),
                                style = Stroke(width = band * 0.55f),
                            )
                            // 暖色散：金橙，向内缩
                            drawRoundRect(
                                color = Color(0xFFFFB27A).copy(alpha = 0.08f + dispAlpha * 0.32f),
                                topLeft = Offset(d, d),
                                size = Size(size.width - d * 2f, size.height - d * 2f),
                                cornerRadius = CornerRadius((r - d).coerceAtLeast(0f), (r - d).coerceAtLeast(0f)),
                                style = Stroke(width = band * 0.55f),
                            )
                        }
                        // 主液态高光带：顶部最亮、底部弱反光
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.50f else 0.75f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = if (isDark) 0.18f else 0.30f),
                                ),
                            ),
                            cornerRadius = CornerRadius(r, r),
                            style = Stroke(width = band),
                        )
                    }
                }
        }
    }
}

/**
 * 玻璃表面容器：普通模式下有 scrim 背景；玻璃模式下透明由效果本身呈现。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val mode = LocalGlassMode.current
    val composeShape: Shape = RoundedCornerShape(cornerRadius)
    val base = if (mode == GlassMode.PLAIN) {
        Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), composeShape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(base)
            .appGlass(cornerRadius),
        content = content,
    )
}
