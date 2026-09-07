package com.zhiwei.math.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.zhiwei.math.data.prefs.GlassSettings
import kotlin.math.roundToInt

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
 * 应用级玻璃容器：极光背板（Backdrop.kt）+ 玻璃面板采样。
 *
 * 实测结论：HyperOS(Android 16) 定制 libhwui 对同层级 RenderNode 采样
 * （backdrop 库 drawBackdrop）会 RenderThread SIGSEGV；haze 的 offscreen blur 虽稳，
 * 但模糊半径等参数与本方案相比不够可调。故全面改用聊天终端安卓本地验证过的
 * 「程序化极光背板 + CPU 位图预模糊 + drawImage 采样」方案 —— 全设备稳定，
 * 模糊半径/饱和度/ veil 不透明度/高光描边全部由设置滑杆实时驱动。
 */
@Composable
fun ProvideGlassContent(
    glass: GlassSettings,
    isDark: Boolean,
    content: @Composable () -> Unit,
) {
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
        GlassBackdrop {
            Box { content() }
        }
    }
}

/**
 * 玻璃表面修饰符：极光背板采样 + 白霜 veil + 顶部高光 + 边缘描边。
 * - 液态玻璃：更强的液态高光描边（宽度/亮度随「折射高度」滑杆）+ 更高背板饱和度
 * - 毛玻璃：标准顶部高光
 * - 半透明：纯 scrim（无背板）
 * 全部参数由 [LocalGlassTuning] 实时驱动。
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
            val backdrop = LocalBackdropState.current
            val ok = backdrop != null && backdrop.isSameWindow(LocalView.current)
            var bounds by remember { mutableStateOf<Rect?>(null) }

            val veilColor = if (isDark) {
                Color(0xFF1C1C22).copy(alpha = (tuning.opacity * 0.52f).coerceIn(0.12f, 0.75f))
            } else {
                Color(0xFFF4F4F8).copy(alpha = (tuning.opacity * 0.52f).coerceIn(0.12f, 0.75f))
            }
            val isLiquid = mode == GlassMode.LIQUID
            val rimWidth = (0.9f + tuning.refractionHeightDp / 80f * 1.8f).dp
            val topSheen = if (isLiquid) 0.28f else 0.14f

            this
                .clip(composeShape)
                .onGloballyPositioned { bounds = it.boundsInRoot() }
                .drawWithContent {
                    val bmp = backdrop?.bitmap
                    var drewBackdrop = false
                    if (ok && bmp != null && bmp.width > 0 && bounds != null) {
                        val b = bounds!!
                        val s = backdrop.downscale
                        // 面板在根坐标系下的区域 → 映射到背板位图（越界安全收窄）
                        val cL = (b.left / s).coerceIn(0f, bmp.width.toFloat())
                        val cT = (b.top / s).coerceIn(0f, bmp.height.toFloat())
                        val cR = (b.right / s).coerceIn(0f, bmp.width.toFloat())
                        val cB = (b.bottom / s).coerceIn(0f, bmp.height.toFloat())
                        if (cR - cL > 1f && cB - cT > 1f) {
                            drawImage(
                                image = bmp,
                                srcOffset = IntOffset(cL.roundToInt(), cT.roundToInt()),
                                srcSize = IntSize((cR - cL).roundToInt(), (cB - cT).roundToInt()),
                                dstOffset = IntOffset.Zero,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                filterQuality = FilterQuality.Low,
                            )
                            drewBackdrop = true
                        }
                    }
                    if (drewBackdrop) {
                        // 白霜 veil：让背板颜色透出时仍保持蒙眬与通透
                        drawRect(veilColor)
                    } else {
                        // 背板不可用回退（跨窗口 Dialog/Sheet）：半透明叠层
                        drawRect(scrimColor)
                    }
                    drawContent()
                    // 顶部高光（玻璃上缘环境光）
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.White.copy(alpha = topSheen),
                            0.25f to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    )
                    // 液态玻璃：全周液态高光描边（宽度/亮度随折射高度滑杆）
                    if (isLiquid) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.34f else 0.58f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isDark) 0.10f else 0.18f),
                                ),
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            style = Stroke(width = rimWidth.toPx()),
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
