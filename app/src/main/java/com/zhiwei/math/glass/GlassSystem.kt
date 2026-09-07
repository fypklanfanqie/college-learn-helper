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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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

/** 应用级玻璃容器：内容层同时注册 backdrop（液态玻璃）与 haze 源（毛玻璃） */
@Composable
fun ProvideGlassContent(
    glass: GlassSettings,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
    val hazeState = remember { HazeState() }
    val mode = resolveGlassMode(glass.mode)

    // backdrop 保留注册（供未来 drawBackdrop 修复合机后切回真折射液态玻璃）；
    // 当前玻璃表面全部走 haze（本机安全），不挂 layerBackdrop 节点。
    val layerBackdrop = rememberLayerBackdrop()

    // 关键修复：此前 LocalLayerBackdrop / LocalHazeState 从未被 provide，
    // 所有玻璃表面都静默降级为纯色半透明矩形，滑杆参数无处生效。
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
            GlassMode.LIQUID -> {
                Box(Modifier.hazeSource(hazeState)) { content() }
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
            // 液态玻璃实现说明（本机实测结论）：
            // backdrop 库 drawBackdrop（任意效果，含纯 blur）会触发 HyperOS(Android 16)
            // 定制 libhwui 的 MiBackgroundBlurBlend 原生崩溃（RenderThread SIGSEGV，
            // RenderNode UAF），Cresto 同款 glass() 在本机同样会崩；haze 的
            // offscreen blur 实现实测稳定。
            // 因此 LIQUID = haze 强模糊 + 液态高光描边（rim light），观感接近 iOS 液态玻璃，
            // 且滑杆参数（模糊/不透明度/折射高度→模糊调制）全部生效。
            val blurBase = (tuning.blurRadiusDp + tuning.refractionHeightDp * 0.25f).coerceAtMost(60f)
            val state = LocalHazeState.current
            val tintAlpha = (tuning.opacity * 0.62f).coerceIn(0.2f, 0.8f)
            val tint = if (isDark) {
                HazeTint(Color(0xFF242428).copy(alpha = tintAlpha))
            } else {
                HazeTint(Color(0xFFF6F6FA).copy(alpha = tintAlpha))
            }
            val base = if (state != null) {
                Modifier.hazeEffect(
                    state = state,
                    style = HazeStyle(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        tints = listOf(tint),
                        blurRadius = blurBase.dp,
                    ),
                )
            } else {
                Modifier.background(scrimColor, composeShape)
            }
            this
                .then(base)
                .liquidRim(isDark = isDark, cornerRadius = cornerRadius)
        }
        GlassMode.FROSTED -> this.frostedOrPlain(tuning, composeShape, scrimColor)
        GlassMode.PLAIN -> this.background(scrimColor, composeShape)
    }
}

/**
 * 液态玻璃高光描边：顶部强高光渐隐 + 底部弱反光，纯 Canvas 绘制
 * （无 RenderEffect，MIUI 安全），模拟玻璃边缘的环境光折射。
 */
private fun Modifier.liquidRim(isDark: Boolean, cornerRadius: Dp): Modifier =
    this.drawWithContent {
        drawContent()
        val stroke = 1.2.dp.toPx()
        val radius = cornerRadius.toPx().coerceAtLeast(0f)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDark) 0.34f else 0.62f),
                    Color.Transparent,
                    Color.Transparent,
                    Color.White.copy(alpha = if (isDark) 0.10f else 0.18f),
                ),
            ),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke),
        )
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
