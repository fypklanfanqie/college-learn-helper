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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
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
val LocalLayerBackdrop = compositionLocalOf<LayerBackdrop?> { null }
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
 * 应用级玻璃容器：内容层注册两种源，玻璃表面按模式取样。
 *
 * - LIQUID：注册 [LayerBackdrop]，表面走官方 lens() 折射（纯 AGSL RuntimeShader，
 *   不含 BlurEffect —— blur RenderEffect 会触发 HyperOS 定制 libhwui 的
 *   MiBackgroundBlurBlend 原生崩溃，实测 Adreno 830 必崩）。
 * - FROSTED：注册 haze 源（offscreen blur，实测本机稳定）。
 */
@Composable
fun ProvideGlassContent(
    glass: GlassSettings,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val hazeState = remember { HazeState() }
    val mode = resolveGlassMode(glass.mode)

    // 参照 Cresto：backdrop 先画 3 倍大的页面背景色再画内容，
    // 玻璃在内容边界外采样时取页面背景色而非透明，避免边缘空洞。
    val backdropBaseColor = MaterialTheme.colorScheme.background
    val layerBackdrop = rememberLayerBackdrop {
        drawRect(
            color = backdropBaseColor,
            size = Size(this.size.width * 3f, this.size.height * 3f),
            topLeft = Offset(-this.size.width, -this.size.height),
        )
        drawContent()
    }

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
        LocalLayerBackdrop provides layerBackdrop,
    ) {
        when (mode) {
            GlassMode.LIQUID -> Box(Modifier.layerBackdrop(layerBackdrop)) { content() }
            GlassMode.FROSTED -> Box(Modifier.hazeSource(hazeState)) { content() }
            GlassMode.PLAIN -> Box { content() }
        }
    }
}

/**
 * 玻璃表面修饰符（四滑杆全部实时生效）：
 *
 * LIQUID（官方 Kyant0 lens 链，纯 AGSL RuntimeShader）：
 * - 折射高度滑杆 → lens refractionHeight（边缘折射区宽度，官方示例 12dp）
 * - 折射量滑杆 → lens refractionAmount（边缘位移量）+ 色散开/关
 * - 模糊半径滑杆 → 表面白霜雾感（blur RenderEffect 在本机必崩，用霜层模拟）
 * - 不透明度滑杆 → 表面着色
 *
 * FROSTED（haze，真实内容模糊）：
 * - 模糊半径 → haze blurRadius（0 = 清晰透底）
 * - 不透明度 → 着色 veil
 */
fun Modifier.appGlass(cornerRadius: Dp = 0.dp): Modifier = composed {
    val mode = LocalGlassMode.current
    val tuning = LocalGlassTuning.current
    val isDark = LocalIsDarkTheme.current
    val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val composeShape: Shape = RoundedCornerShape(cornerRadius)
    val density = LocalDensity.current

    when (mode) {
        GlassMode.PLAIN -> this.background(scrimColor, composeShape)

        GlassMode.LIQUID -> {
            val backdrop = LocalLayerBackdrop.current
            if (backdrop != null) {
                val tintAlpha = (tuning.opacity * 0.55f).coerceIn(0.02f, 0.62f)
                val frostAlpha = tuning.blurRadiusDp / 40f * 0.40f
                val tintColor = if (isDark) Color(0xFF17171D) else Color(0xFFF7F7FB)
                val refractionHeightPx = with(density) { tuning.refractionHeightDp.dp.toPx() }
                val refractionAmountPx = with(density) { tuning.refractionAmountDp.dp.toPx() }
                this.drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedRectangle(cornerRadius, RoundedCornerStyle.Continuous) },
                    effects = {
                        // 官方液态折射（纯 AGSL；不使用 blur RenderEffect）
                        lens(
                            refractionHeight = refractionHeightPx,
                            refractionAmount = refractionAmountPx,
                            depthEffect = false,
                            chromaticAberration = tuning.refractionAmountDp > 2,
                        )
                    },
                    onDrawSurface = {
                        // 白霜（模糊滑杆）+ 着色（不透明度滑杆）
                        drawRect(Color.White.copy(alpha = if (isDark) frostAlpha * 0.45f else frostAlpha))
                        drawRect(tintColor.copy(alpha = tintAlpha))
                    },
                )
            } else {
                // backdrop 不可用 → 降级 haze 毛玻璃
                this.hazeGlass(tuning, isDark, composeShape, scrimColor)
            }
        }

        GlassMode.FROSTED -> this.hazeGlass(tuning, isDark, composeShape, scrimColor)
    }
}

/** 毛玻璃：haze 真实内容模糊 + 着色 veil（本机实测稳定的 offscreen 路径） */
private fun Modifier.hazeGlass(
    tuning: GlassTuning,
    isDark: Boolean,
    shape: Shape,
    scrimColor: Color,
): Modifier = composed {
    val hazeState = LocalHazeState.current
    val blurDp = tuning.blurRadiusDp * 1.2f
    val tintAlpha = (tuning.opacity * 0.7f).coerceIn(0.02f, 0.72f)
    val tint = if (isDark) {
        HazeTint(Color(0xFF1E1E26).copy(alpha = tintAlpha))
    } else {
        HazeTint(Color(0xFFF7F7FB).copy(alpha = tintAlpha))
    }
    if (hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = MaterialTheme.colorScheme.surface,
                tints = listOf(tint),
                blurRadius = blurDp.dp,
            ),
        )
    } else {
        Modifier.background(scrimColor, shape)
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
