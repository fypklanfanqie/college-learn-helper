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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.qmdeve.liquidglass.widget.LiquidGlassView
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
 * 应用级玻璃容器：内容层注册 haze 源，玻璃表面按模式取样。
 *
 * ⚠ 本机（小米 2410DPN6CC / HyperOS Android 16 / Adreno 830）实测结论：
 * backdrop 库的 drawBackdrop/LayerBackdrop 采样链（无论 blur 还是官方 lens
 * 纯 RuntimeShader 折射）都会导致 RenderThread 栈溢出 SIGSEGV
 * （"stack pointer is close to top of stack"，RenderNode 自引用递归，
 * fault addr 恒定 0x7b1e6c0ff0）—— HyperOS 定制 libhwui 无法处理
 * "玻璃节点采样其祖先注册层"的循环结构。聊天终端安卓本地 同样因此
 * 弃用 view 树捕获（见其 LiquidGlassRenderer 注释）。
 *
 * 因此液态玻璃最终实现 = haze 真实内容模糊（offscreen，实测稳定）+
 * 液态光带/色散描边（纯 Canvas）。四滑杆全部实时生效。
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
 * 玻璃表面修饰符（四滑杆全部实时生效）：
 * - 模糊半径滑杆 → 真实内容模糊度（0 = 清晰透底）
 * - 不透明度滑杆 → 着色 veil
 * - 折射高度滑杆 → 液态光带宽度（2.2~10.7dp）
 * - 折射量滑杆 → 青/金色散双描边错位（0~3.2dp）+ 光泽
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
 * 玻璃表面容器。
 *
 * LIQUID：QmDeve LiquidGlassView（聊天终端安卓本地性能浮窗同款库，本机实测优秀）。
 * 配置基线取自该浮窗的实测值（refractionHeight 14dp / offset 60dp / blur 2.5 /
 * dispersion 0.4 / 淡冷色 tint 0.10），四滑杆直通其 setter。
 *
 * FROSTED：haze 真实内容模糊。PLAIN：scrim。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val mode = LocalGlassMode.current
    val tuning = LocalGlassTuning.current
    val isDark = LocalIsDarkTheme.current
    val composeShape: Shape = RoundedCornerShape(cornerRadius)

    when {
        // ── 液态玻璃：QmDeve LiquidGlassView（API 33+）──
        mode == GlassMode.LIQUID && Build.VERSION.SDK_INT >= 33 -> {
            val density = LocalDensity.current
            val cornerPx = with(density) { cornerRadius.toPx().coerceAtLeast(8f) }
            // 实测基线：refractionHeight 14dp、offset 60dp；滑杆在基线邻域映射
            val refractionH = with(density) { tuning.refractionHeightDp.coerceIn(4, 28).dp.toPx() }
            val refractionOff = with(density) { tuning.refractionAmountDp.coerceIn(0, 96).dp.toPx() }
            // 实测基线 blur 2.5 = 滑杆 40 满值；0 = 无模糊
            val blur = (tuning.blurRadiusDp / 16f).coerceIn(0f, 4f)
            val tintAlpha = (tuning.opacity * 0.5f).coerceIn(0f, 0.55f)
            val tintR = if (isDark) 0.05f else 0.97f
            val tintG = if (isDark) 0.07f else 0.97f
            val tintB = if (isDark) 0.12f else 1f

            Box(modifier.clip(composeShape)) {
                AndroidView(
                    modifier = Modifier.matchParentSize(),
                    factory = { ctx ->
                        try {
                            LiquidGlassView(ctx).apply {
                                setCornerRadius(cornerPx)
                                setRefractionHeight(refractionH)
                                setRefractionOffset(refractionOff)
                                setBlurRadius(blur)
                                setDispersion(0.4f)
                                setTintColorRed(tintR)
                                setTintColorGreen(tintG)
                                setTintColorBlue(tintB)
                                setTintAlpha(tintAlpha)
                                // 关键：绑定兄弟采样源（GlassHost，MainActivity 装配的
                                // ComposeView 之外的背景层）—— 源不含玻璃 → record() 无
                                // 自引用 → 不递归不崩。结构同聊天终端安卓本地的性能浮窗
                                // （玻璃 bind 兄弟 ComposeView、面板为其兄弟）。
                                // 不 bind 时库的 ensureGlass() 直接 return，玻璃不渲染。
                                // ⚠ 不可 bind 玻璃的祖先（如 android.R.id.content）——
                                // 快照会包含玻璃自身 → RenderThread 栈溢出（实测 fault
                                // addr 恒定 0x7b1e6c0ff0）。
                                val root = com.zhiwei.math.GlassHost.source()
                                if (root != null) {
                                    post {
                                        try {
                                            bind(root)
                                        } catch (e: Exception) {
                                            android.util.Log.w("GlassSurface", "bind failed: ${e.message}")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // GPU 不兼容兜底：透明占位（内容仍可读）
                            android.view.View(ctx)
                        }
                    },
                    update = { glass ->
                        if (glass is LiquidGlassView) {
                            glass.setCornerRadius(cornerPx)
                            glass.setRefractionHeight(refractionH)
                            glass.setRefractionOffset(refractionOff)
                            glass.setBlurRadius(blur)
                            glass.setTintAlpha(tintAlpha)
                        }
                    },
                )
                Box(Modifier.matchParentSize()) { content() }
            }
        }

        // ── 液态玻璃但 API < 33（AGSL 不可用）→ 回退毛玻璃 ──
        mode == GlassMode.LIQUID -> {
            val scrimColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            Box(
                modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), composeShape)
                    .hazeGlass(tuning, isDark, composeShape, scrimColor)
            ) {
                content()
            }
        }

        // ── 半透明 ──
        mode == GlassMode.PLAIN -> {
            Box(modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f), composeShape)) {
                content()
            }
        }

        // ── 毛玻璃：haze 真实内容模糊 ──
        else -> {
            Box(modifier.appGlass(cornerRadius)) {
                content()
            }
        }
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
