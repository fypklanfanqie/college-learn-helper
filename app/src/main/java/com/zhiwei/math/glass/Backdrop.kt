package com.zhiwei.math.glass

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.Log
import android.view.View
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// ===== 极光背板（移植自聊天终端安卓本地的 Backdrop.kt，实测全设备兼容） =====
//
// 设计要点：玻璃面板折射的不是真实下层内容，而是程序化的"极光光斑"低分辨率位图
// （CPU BlurMaskFilter 预模糊，宽高各 1/4，双缓冲 ~30fps）。这样完全绕开
// RenderNode/RenderEffect 采样 —— HyperOS(Android 16) 定制 libhwui 的
// MiBackgroundBlurBlend 对同层级 RenderNode 采样会 SIGSEGV（Adreno 上已知问题），
// 纯位图路径 100% 稳定，且所有参数（模糊半径/饱和度/光斑强度）均可调。

/** 一个极光光斑：颜色 + 不透明度。圆心/半径按尺寸比例由 [meshBlobPositions] 动态计算。 */
internal data class MeshBlobSpec(val color: Color, val alpha: Float)

internal data class MeshSpec(val base: Color, val blobs: List<MeshBlobSpec>)

/** 明暗两套极光配色（知微墨蓝 × 宋浩金）。 */
internal fun meshSpec(dark: Boolean): MeshSpec =
    if (dark) MeshSpec(
        base = Color(0xFF0A0A0F),
        blobs = listOf(
            MeshBlobSpec(Color(0xFFC9A87C), 0.24f),  // 暖金
            MeshBlobSpec(Color(0xFF4FA5A0), 0.22f),  // 青
            MeshBlobSpec(Color(0xFF2F6F8A), 0.26f),  // 深蓝
            MeshBlobSpec(Color(0xFF8A7355), 0.20f),  // 暗金
        ),
    ) else MeshSpec(
        base = Color(0xFFF2F3F8),
        blobs = listOf(
            MeshBlobSpec(Color(0xFF6E4DFF), 0.20f), // 淡紫
            MeshBlobSpec(Color(0xFF0A84FF), 0.16f), // 冰蓝
            MeshBlobSpec(Color(0xFFFF6B9D), 0.16f), // 粉
            MeshBlobSpec(Color(0xFF6E4DFF), 0.12f), // 淡紫第二团（铺底）
        ),
    )

/** 光斑圆心/半径：相对尺寸比例 × 相位三角函数（22s 周期流动）。 */
internal fun meshBlobPositions(phase: Float, w: Float, h: Float): List<Triple<Float, Float, Float>> {
    val a = phase * 2f * PI.toFloat()
    return listOf(
        Triple(w * (0.25f + 0.18f * sin(a)), h * (0.28f + 0.18f * cos(a)), w * 0.55f),
        Triple(w * (0.80f + 0.16f * sin(a + 2.1f)), h * (0.62f + 0.16f * cos(a + 2.1f)), w * 0.50f),
        Triple(w * (0.50f + 0.20f * sin(a + 4.2f)), h * (0.88f + 0.12f * cos(a + 4.2f)), w * 0.45f),
        Triple(w * (0.10f + 0.10f * sin(a + 1.0f)), h * (0.85f + 0.10f * cos(a + 1.0f)), w * 0.35f),
    )
}

// ===== 背板状态：共享给所有玻璃面板采样 =====

/**
 * 预模糊背板持有者：玻璃面板在 draw 阶段按自身位置采样 [bitmap]。
 *
 * - [bitmap] 用 mutableStateOf 包住：组合期只读引用，draw 期读取才失效绘制。
 * - 双缓冲避免上一帧仍被绘制时原地改写位图。
 * - 纯 CPU 位图路径（低分辨率 + BlurMaskFilter），无 RenderEffect，全设备稳定。
 */
class BackdropState {
    var bitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    var rootSize by mutableStateOf(IntSize.Zero)
    var hostComposeView: AbstractComposeView? = null
    val downscale: Float = 4f

    private var meshBitmap: Bitmap? = null
    private val blurBitmaps = arrayOfNulls<Bitmap>(2)
    private var frameIndex = 0
    private var cachedDark: Boolean? = null
    private var cachedSpec: MeshSpec? = null

    /** 玻璃面板所在窗口是否与背板同窗口（Dialog/BottomSheet 是独立窗口，需回退半透明）。 */
    fun isSameWindow(view: View): Boolean {
        val host = hostComposeView ?: return false
        var v: View? = view
        while (v != null) {
            if (v is AbstractComposeView) return v == host
            v = v.parent as? View
        }
        return false
    }

    /** 渲染一帧：画清晰网格 → 模糊+调饱和度 → 发布双缓冲位图。 */
    fun renderFrame(phase: Float, dark: Boolean, blurRadiusPx: Float, saturation: Float) {
        val size = rootSize
        if (size.width <= 0 || size.height <= 0) return
        val w = max(1, (size.width / downscale).roundToInt())
        val h = max(1, (size.height / downscale).roundToInt())
        if (meshBitmap == null || meshBitmap!!.width != w || meshBitmap!!.height != h) {
            meshBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            blurBitmaps[0] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            blurBitmaps[1] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            frameIndex = 0
        }
        val target = blurBitmaps[frameIndex]!!
        renderBackdrop(meshBitmap!!, target, specFor(dark), phase, blurRadiusPx, saturation)
        bitmap = target.asImageBitmap()
        frameIndex = 1 - frameIndex
    }

    private fun specFor(dark: Boolean): MeshSpec {
        if (cachedDark != dark) {
            cachedDark = dark
            cachedSpec = meshSpec(dark)
        }
        return cachedSpec!!
    }
}

val LocalBackdropState = staticCompositionLocalOf<BackdropState?> { null }

private fun renderBackdrop(
    meshBitmap: Bitmap,
    blurBitmap: Bitmap,
    spec: MeshSpec,
    phase: Float,
    blurRadiusPx: Float,
    saturation: Float,
) {
    val w = meshBitmap.width.toFloat()
    val h = meshBitmap.height.toFloat()
    val positions = meshBlobPositions(phase, w, h)

    val meshCanvas = android.graphics.Canvas(meshBitmap)
    meshCanvas.drawColor(spec.base.toArgb())
    positions.forEachIndexed { i, (cx, cy, radius) ->
        val blob = spec.blobs[i]
        val argb = blob.color.copy(alpha = blob.alpha).toArgb()
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, radius, argb, android.graphics.Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        meshCanvas.drawRect(0f, 0f, w, h, p)
    }

    val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
        colorFilter = ColorMatrixColorFilter(saturationMatrix(saturation))
    }
    val blurCanvas = android.graphics.Canvas(blurBitmap)
    blurCanvas.drawColor(android.graphics.Color.TRANSPARENT)
    blurCanvas.drawBitmap(meshBitmap, 0f, 0f, blurPaint)
}

/** 标准亮度感知饱和度矩阵。 */
private fun saturationMatrix(s: Float): FloatArray {
    val r = 0.2126f
    val g = 0.7152f
    val b = 0.0722f
    val i = 1f - s
    return floatArrayOf(
        r + (1f - r) * s, g * i, b * i, 0f, 0f,
        r * i, g + (1f - g) * s, b * i, 0f, 0f,
        r * i, g * i, b + (1f - b) * s, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

/**
 * 应用级背板容器：包住整个应用内容。
 *
 * - 唯一 rememberInfiniteTransition（22s 线性循环）驱动光斑流动。
 * - 后台 ~30fps 渲染模糊背板到 [BackdropState.bitmap]，供 [LocalBackdropState] 采样。
 * - 模糊半径/饱和度由玻璃设置（LocalGlassTuning）实时驱动 —— 设置滑杆立即生效。
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = remember { BackdropState() }
    val dark = LocalIsDarkTheme.current
    val tuning = LocalGlassTuning.current
    val density = LocalDensity.current
    val view = LocalView.current

    // 背板模糊半径 = 设置的模糊半径（除以 downscale 等效换算）；折射量映射为背板饱和度
    val blurRadiusPx = with(density) { tuning.blurRadiusDp.dp.toPx() } / state.downscale
    val saturation = (1f + tuning.refractionAmountDp / 96f * 0.8f).coerceIn(0.8f, 1.8f)

    val transition = rememberInfiniteTransition(label = "mesh")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "meshPhase",
    )

    // 记录背板所在窗口的根 ComposeView，供 isSameWindow 守卫（Dialog/Sheet 独立窗口）
    LaunchedEffect(view) {
        var v: View? = view
        var host: AbstractComposeView? = null
        while (v != null) {
            if (v is AbstractComposeView) host = v
            v = v.parent as? View
        }
        state.hostComposeView = host
    }

    // 应用可见才渲染，避免后台空转耗电
    var isVisible by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isVisible = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ~30fps 后台渲染循环（CPU 位图不占主线程；失败仅跳帧绝不崩溃）
    LaunchedEffect(state, dark, blurRadiusPx, saturation) {
        withContext(Dispatchers.Default) {
            while (isActive) {
                if (isVisible) {
                    runCatching {
                        state.renderFrame(phase.value, dark, blurRadiusPx, saturation)
                    }.onFailure { Log.w("GlassBackdrop", "renderFrame failed: ${it.message}") }
                }
                delay(33L)
            }
        }
    }

    CompositionLocalProvider(
        LocalBackdropState provides state,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { state.rootSize = it },
        ) {
            content()
        }
    }
}
