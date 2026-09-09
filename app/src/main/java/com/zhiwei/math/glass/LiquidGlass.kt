package com.zhiwei.math.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangularShape
import com.zhiwei.math.data.prefs.GlassSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import android.os.Build

/**
 * 玻璃系统（backdrop 库真实折射，重设计核心；替代旧 GlassSystem）。
 *
 * ⚠ 崩溃规避铁律（backdrop 官方 FAQ 已证实根因是循环绘制依赖，非 HyperOS 缺陷）：
 * 1. 玻璃元素绝不能出现在 Modifier.layerBackdrop() 包裹的内容树内部——
 *    玻璃必须是被录制内容的【兄弟节点】（GlassHost 保证该布局）。
 * 2. 玻璃上玻璃（dock 里的滑块）：父玻璃 drawBackdrop 传 exportedBackdrop，
 *    子玻璃采样它；绝不给父玻璃套 layerBackdrop。
 * 3. SIGSEGV 为 native 崩溃 catch 不住：LIQUID 首帧渲染前 DataStore 打点
 *    glass_render_probe，首帧渲染完清除；启动检测残留 → 禁用液态玻璃并降级
 *    FROSTED（SettingsStore.consumeBootProbe）。
 */

/** 玻璃模式：LIQUID（backdrop 真实折射，API 33+）/ FROSTED（haze）/ PLAIN（scrim） */
enum class GlassMode(val label: String) {
    PLAIN("半透明"),
    FROSTED("毛玻璃"),
    LIQUID("液态玻璃");

    companion object {
        fun fromId(id: String): GlassMode =
            entries.firstOrNull { it.name.equals(id, ignoreCase = true) } ?: FROSTED
    }
}

val LocalGlassMode = staticCompositionLocalOf { GlassMode.FROSTED }
val LocalIsDarkTheme = compositionLocalOf { false }
val LocalHazeState = compositionLocalOf<HazeState?> { null }
/** 震动反馈总开关（设置 → 震动），默认开启 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * 实际玻璃模式解析（API 分档）：用户选择优先；超过硬件能力时降级
 * （液态玻璃需 API 33+，毛玻璃需 API 31+，否则半透明）。
 */
fun resolveGlassMode(userModeId: String, sdkInt: Int = Build.VERSION.SDK_INT): GlassMode {
    val user = GlassMode.fromId(userModeId)
    return when (user) {
        GlassMode.LIQUID -> if (sdkInt >= 33) GlassMode.LIQUID else if (sdkInt >= 31) GlassMode.FROSTED else GlassMode.PLAIN
        GlassMode.FROSTED -> if (sdkInt >= 31) GlassMode.FROSTED else GlassMode.PLAIN
        GlassMode.PLAIN -> GlassMode.PLAIN
    }
}

/** 玻璃运行时配置（液态 10 参数 px 形态 + 毛玻璃 4 参数，由 GlassSettings 构建） */
data class LiquidGlassConfig(
    // ── 液态（LIQUID）──
    val blurPx: Float = 8f,
    val refractionHeightPx: Float = 24f,
    val refractionAmountPx: Float = 24f,
    val chromaticAberration: Int = 60,     // >50 开启色散（lens 参数是 Boolean）
    val depthEffect: Int = 0,              // >50 开启景深（同上）
    val vibrancy: Int = 100,               // 0-200% → colorControls saturation
    val brightness: Int = 0,               // -50~+50 → colorControls brightness
    val surfaceTintAlpha: Float = 0.40f,   // onDrawSurface 遮罩 alpha
    val highlightAlpha: Float = 1f,        // Highlight.Default.copy(alpha)
    val shadowAlpha: Float = 1f,           // Shadow.Default.copy(alpha)
    // ── 毛玻璃（FROSTED）──
    val frostedBlurDp: Float = 20f,
    val frostedOpacity: Float = 0.70f,
    val frostedWarmth: Int = 50,           // 0 冷 ↔ 100 暖（着色色温微调）
    val frostedHighlight: Float = 1f,      // 顶部高光强度 0-1
) {
    companion object {
        /** 从持久化设置构建（density 做真实 px 换算） */
        fun from(g: GlassSettings, density: Float): LiquidGlassConfig = LiquidGlassConfig(
            blurPx = g.blurRadius * density,
            refractionHeightPx = g.refractionHeight * density,
            refractionAmountPx = g.refractionAmount * density,
            chromaticAberration = g.chromaticAberration,
            depthEffect = g.depthEffect,
            vibrancy = g.vibrancy,
            brightness = g.brightness,
            surfaceTintAlpha = g.surfaceTint / 100f,
            highlightAlpha = g.highlightAlpha / 100f,
            shadowAlpha = g.shadowAlpha / 100f,
            frostedBlurDp = g.frostedBlur.toFloat(),
            frostedOpacity = (g.opacity.coerceAtLeast(30)) / 100f,
            frostedWarmth = g.frostedTint,
            frostedHighlight = g.frostedHighlight / 100f,
        )
    }
}

/** 当前屏幕的 backdrop 录制层（GlassHost 提供；玻璃表面只读它，绝不嵌进内容树） */
val LocalLayerBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** 液态参数（AppRoot 级提供） */
val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }

/** 渲染探针（SIGSEGV 自动降级用；null = 不打点，如已禁用） */
val LocalGlassProbe = staticCompositionLocalOf<GlassProbe?> { null }

interface GlassProbe {
    suspend fun armRenderProbe()
    suspend fun clearRenderProbe()
}

/**
 * 应用级玻璃上下文（替代旧 ProvideGlassContent）：
 * 只提供 CompositionLocals，不做任何录制——每屏自己的 GlassHost 负责录制，
 * 保证玻璃永远是被录制内容的兄弟节点（铁律 1）。
 */
@Composable
fun ProvideAppGlass(
    glass: GlassSettings,
    liquidDisabled: Boolean,
    isDark: Boolean,
    probe: GlassProbe?,
    content: @Composable () -> Unit,
) {
    // 用户选液态但已因崩溃被禁用 → 强制 FROSTED
    val mode = resolveGlassMode(
        if (liquidDisabled && GlassMode.fromId(glass.mode) == GlassMode.LIQUID) {
            GlassMode.FROSTED.name
        } else {
            glass.mode
        }
    )
    val density = LocalDensity.current.density
    val config = remember(glass, density) { LiquidGlassConfig.from(glass, density) }

    CompositionLocalProvider(
        LocalGlassMode provides mode,
        LocalLiquidGlassConfig provides config,
        LocalIsDarkTheme provides isDark,
        LocalGlassProbe provides (if (mode == GlassMode.LIQUID) probe else null),
    ) {
        content()
    }
}

/**
 * 屏幕级玻璃宿主：内容层 + 兄弟玻璃挂载区（铁律 1 的结构性保证）。
 * - LIQUID：内容树挂 layerBackdrop 离屏录制；overlay 里的玻璃采样 LocalLayerBackdrop。
 *   首帧渲染前打点探针、渲染完成清除（铁律 3）——打点未落盘前以 scrim 形态渲染，
 *   确保崩溃发生在打点之后才能被检测到。
 * - FROSTED：内容树挂 hazeSource（haze 布局本就安全）。
 * - PLAIN：纯 scrim。
 */
@Composable
fun GlassHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    when (LocalGlassMode.current) {
        GlassMode.LIQUID -> {
            val backdrop = rememberLayerBackdrop()
            val probe = LocalGlassProbe.current
            // 打点门闩：armRenderProbe() 落盘完成前不渲染液态（首帧必须发生在打点之后）
            var probeArmed by remember { mutableStateOf(probe == null) }
            LaunchedEffect(Unit) {
                if (probe != null) {
                    probe.armRenderProbe()
                    probeArmed = true
                }
            }
            if (!probeArmed) {
                // 打点未落盘：内容照常，玻璃以 scrim 占位（一帧左右）
                Box(modifier) { Box(Modifier) { content() }; overlay() }
                return
            }
            // 首帧渲染完成后清除打点：
            // 延迟 3 帧再清——单帧 withFrameNanos 在 UI 线程帧回调即恢复，
            // 而 RenderThread 的崩溃发生在其后（真机实测竞态导致漏检），
            // 3 帧（~50ms）后 RenderThread 已完成渲染，崩溃必然发生在清除之前。
            LaunchedEffect(Unit) {
                repeat(3) { withFrameNanos { } }
                probe?.clearRenderProbe()
            }
            CompositionLocalProvider(LocalLayerBackdrop provides backdrop) {
                Box(modifier) {
                    Box(Modifier.layerBackdrop(backdrop)) { content() }
                    overlay()
                }
            }
        }
        GlassMode.FROSTED -> {
            val hazeState = remember { HazeState() }
            CompositionLocalProvider(LocalHazeState provides hazeState) {
                Box(modifier) {
                    Box(Modifier.hazeSource(hazeState)) { content() }
                    overlay()
                }
            }
        }
        else -> Box(modifier) { content(); overlay() }
    }
}

/**
 * 玻璃表面修饰符（替代旧 appGlass）：
 * - LIQUID：drawBackdrop 真实折射（vibrancy/brightness → colorControls，
 *   blur → BlurEffect，lens → SDF 折射+色散；lens 要求形状为
 *   RoundedRectangularShape / CornerBasedShape，不支持则跳过 lens 只模糊）。
 * - FROSTED：hazeEffect。
 * - PLAIN / 无录制层：scrim 兜底。
 *
 * @param shape 玻璃裁剪形状（推荐 com.kyant.shapes 系列连续圆角）
 * @param surfaceColor 表面着色底色（浅色用浅灰白、深色用深灰，由调用方传）
 * @param innerShadowRadius 内阴影半径（null = 无内阴影；dock 等大玻璃用 8dp）
 * @param exportedBackdrop 玻璃上玻璃：把本玻璃的合成结果导出给子玻璃采样
 * @param backdropOverride 采样源覆盖（默认 LocalLayerBackdrop）。
 *   设置页预览卡用它传 CanvasBackdrop（自绘光斑、零录制层）——
 *   预览卡位于外层 GlassHost 录制树【内部】，若采样外层 layerBackdrop
 *   会形成"层内容包含自身"的递归嵌套（真机 RenderThread 512 帧栈溢出
 *   SIGSEGV，MiBackgroundBlurBlend 栈），故必须用无录制层的 Backdrop。
 */
fun Modifier.liquidGlass(
    shape: Shape,
    surfaceColor: Color,
    innerShadowRadius: Dp? = null,
    exportedBackdrop: LayerBackdrop? = null,
    blurOverride: Dp? = null,
    backdropOverride: com.kyant.backdrop.Backdrop? = null,
): Modifier = composed {
    val mode = LocalGlassMode.current
    val config = LocalLiquidGlassConfig.current
    val isDark = LocalIsDarkTheme.current
    val backdrop = backdropOverride ?: LocalLayerBackdrop.current
    val hazeState = LocalHazeState.current
    val density = LocalDensity.current
    val supportsLens = shape is RoundedRectangularShape || shape is androidx.compose.foundation.shape.CornerBasedShape

    when {
        mode == GlassMode.LIQUID && backdrop != null -> {
            val blurPx = if (blurOverride != null) blurOverride.value * density.density else config.blurPx
            this.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    // 顺序固定（官方链）：色彩 → 模糊 → 折射
                    if (config.vibrancy != 100 || config.brightness != 0) {
                        colorControls(
                            brightness = config.brightness / 100f,
                            saturation = config.vibrancy / 100f,
                        )
                    }
                    if (blurPx > 0f) blur(blurPx)
                    if (supportsLens &&
                        config.refractionHeightPx > 0f && config.refractionAmountPx > 0f
                    ) {
                        lens(
                            refractionHeight = config.refractionHeightPx,
                            refractionAmount = config.refractionAmountPx,
                            depthEffect = config.depthEffect > 50,
                            chromaticAberration = config.chromaticAberration > 50,
                        )
                    }
                },
                highlight = { Highlight.Default.copy(alpha = config.highlightAlpha) },
                shadow = { Shadow.Default.copy(alpha = config.shadowAlpha) },
                innerShadow = if (innerShadowRadius != null) {
                    { InnerShadow(radius = innerShadowRadius, alpha = 0.15f) }
                } else null,
                exportedBackdrop = exportedBackdrop,
                onDrawSurface = {
                    drawRect(surfaceColor.copy(alpha = config.surfaceTintAlpha))
                },
            )
        }
        mode == GlassMode.FROSTED && hazeState != null -> {
            // 毛玻璃 4 参数：模糊 / 不透明度 / 色温（冷↔暖微调）/ 顶部高光
            // 色温：warmth<50 混入冷蓝、>50 混入暖橙，强度按偏移量
            val warmthOffset = (config.frostedWarmth - 50) / 50f // -1..1
            val baseTint = if (isDark) Color(0xFF1E1E26) else Color(0xFFF7F7FB)
            val warmTint = if (isDark) Color(0xFF26201A) else Color(0xFFF7F2E9)
            val coolTint = if (isDark) Color(0xFF1A1E26) else Color(0xFFEDF2F7)
            val tint = if (warmthOffset >= 0) {
                lerpColor(baseTint, warmTint, warmthOffset)
            } else {
                lerpColor(baseTint, coolTint, -warmthOffset)
            }
            this
                .clip(shape)
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = surfaceColor,
                        tints = listOf(HazeTint(tint.copy(alpha = config.frostedOpacity * 0.72f))),
                        blurRadius = config.frostedBlurDp.dp,
                    ),
                )
                .frostedSheen(shape, config.frostedHighlight)
        }
        else -> {
            // PLAIN 或无录制层：scrim 兜底
            this.then(background(surfaceColor.copy(alpha = 0.72f), shape))
        }
    }
}

/** 毛玻璃顶部环境光高光（strength 0-1） */
private fun Modifier.frostedSheen(shape: Shape, strength: Float): Modifier =
    if (strength <= 0.01f) this else this.drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.12f * strength),
                0.3f to Color.Transparent,
                1f to Color.Transparent,
            )
        )
    }

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
