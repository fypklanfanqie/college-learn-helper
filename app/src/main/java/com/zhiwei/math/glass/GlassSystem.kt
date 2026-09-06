package com.zhiwei.math.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedCornerStyle
import com.nevoit.glasense.material.GlassStyle
import com.nevoit.glasense.material.glass
import com.nevoit.glasense.material.MaterialRecipes
import com.nevoit.glasense.material.rememberMaterialRenderEffectOrNull
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

/** 液态玻璃可调参数（PROJECT-BRIEF.md 5.1：折射高度/折射量/模糊/不透明度滑杆） */
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

/** 应用级玻璃容器：内容层同时注册 backdrop（液态玻璃）与 haze 源（毛玻璃） */
@Composable
fun ProvideGlassContent(
    glass: GlassSettings,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val hazeState = remember { HazeState() }
    val layerBackdrop = rememberLayerBackdrop()
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
    ) {
        when (mode) {
            GlassMode.LIQUID -> {
                // 液态玻璃为主，同时注册 haze 源以便降级
                Box(
                    Modifier
                        .layerBackdrop(layerBackdrop)
                        .hazeSource(hazeState)
                ) {
                    content()
                }
            }
            GlassMode.FROSTED -> {
                Box(Modifier.hazeSource(hazeState)) { content() }
            }
            GlassMode.PLAIN -> {
                Box { content() }
            }
        }
    }
}

/**
 * 玻璃表面修饰符：按当前模式/分档返回对应效果。
 * [cornerRadius] 同时用于液态玻璃（G2 连续曲率 Shapes）与普通圆角裁剪。
 * 失败自动降级：液态玻璃层不可用 → 毛玻璃；毛玻璃不可用 → 半透明 scrim。
 */
@Composable
fun Modifier.appGlass(cornerRadius: Dp = 0.dp): Modifier {
    val mode = LocalGlassMode.current
    val tuning = LocalGlassTuning.current
    val isDark = LocalIsDarkTheme.current
    val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    val composeShape: Shape = RoundedCornerShape(cornerRadius)

    return when (mode) {
        GlassMode.LIQUID -> {
            val backdrop = LocalLayerBackdrop.current
            if (backdrop != null) {
                val recipe = if (isDark) MaterialRecipes.ThinDark else MaterialRecipes.ThinLight
                val style = GlassStyle(
                    firstBlurRadius = (tuning.blurRadiusDp * 0.4f).dp,
                    firstOpacity = tuning.opacity,
                    firstRefractionHeight = tuning.refractionHeightDp.dp,
                    firstRefractionAmount = tuning.refractionAmountDp.dp,
                    secondBlurRadius = tuning.blurRadiusDp.dp,
                    secondOpacity = tuning.opacity * 0.9f,
                    secondRefractionHeight = tuning.refractionHeightDp.dp,
                    secondRefractionAmount = tuning.refractionAmountDp.dp,
                )
                val materialEffect = rememberMaterialRenderEffectOrNull(recipe)
                this.glass(
                    backdrop = backdrop,
                    shape = com.kyant.shapes.RoundedRectangle(
                        cornerRadius = cornerRadius,
                        style = com.kyant.shapes.RoundedCornerStyle.Continuous,
                    ),
                    style = style,
                    materialEffect = materialEffect,
                )
            } else {
                this.frostedOrPlain(tuning, composeShape, scrimColor)
            }
        }
        GlassMode.FROSTED -> this.frostedOrPlain(tuning, composeShape, scrimColor)
        GlassMode.PLAIN -> this.background(scrimColor, composeShape)
    }
}

@Composable
private fun Modifier.frostedOrPlain(tuning: GlassTuning, shape: Shape, scrimColor: Color): Modifier {
    val hazeState = LocalHazeState.current
    return if (hazeState != null) {
        val surface = MaterialTheme.colorScheme.surface
        val tint = if (LocalIsDarkTheme.current) {
            HazeTint(Color(0xFF1C1C1E).copy(alpha = tuning.opacity))
        } else {
            HazeTint(Color(0xFFF2F2F7).copy(alpha = tuning.opacity))
        }
        this.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = surface,
                tints = listOf(tint),
                blurRadius = tuning.blurRadiusDp.dp,
            ),
        )
    } else {
        this.background(scrimColor, shape)
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
