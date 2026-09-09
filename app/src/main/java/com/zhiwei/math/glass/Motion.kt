package com.zhiwei.math.glass

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavBackStackEntry

/**
 * 苹果风动画 tokens（重写：四档 spring 体系，参考 glasense Springs + PAL）。
 * - smooth/snappy/bouncy 对应 PAL 三档；crisp 为快速小元素档
 * - 全局按下反馈：PressableScale（0.96 缩放）
 * - 导航转场保留原 push/pop（已达标）
 */
object AppMotion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** PAL smooth：无弹跳，大位移/透明度过渡（500ms bounce 0） */
    fun <T> smooth(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 133f)

    /** PAL snappy：轻微弹性，常规交互（500ms bounce 0.15） */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.85f, stiffness = 180f)

    /** PAL bouncy：明显弹性，强调性动效（500ms bounce 0.3） */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.7f, stiffness = 250f)

    /** 快速干脆：小元素即时反馈（300ms bounce 0.1） */
    fun <T> crisp(): SpringSpec<T> = spring(dampingRatio = 0.9f, stiffness = 700f)

    /** iOS push 式 spring（旧导航沿用） */
    fun <T> iOSspring() = spring<T>(dampingRatio = 0.86f, stiffness = 420f)

    // 转场时长（淡入淡出 + 微缩放）：200/240ms，短促干脆
    const val NAV_FADE = 200
    const val NAV_SCALE = 240
}

/**
 * 全局按下反馈（iOS 观感）：按压缩放 0.96 + 变暗（DimIndication 式）。
 * 与 clickable(interactionSource) 配合：把同一个 interactionSource 传进来。
 */
fun Modifier.pressableScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    graphicsLayer {
        scaleX = if (pressed) pressedScale else 1f
        scaleY = if (pressed) pressedScale else 1f
        alpha = if (pressed) 0.85f else 1f
    }
}

/** 统一震动入口：尊重 LocalHapticsEnabled 总开关 */
@Composable
fun rememberHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptic, enabled) {
        if (enabled) {
            { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        } else {
            {}
        }
    }
}

/** 长按震动 */
@Composable
fun rememberLongPressHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptic, enabled) {
        if (enabled) {
            { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        } else {
            {}
        }
    }
}

/**
 * 导航转场（性能优先）：淡入淡出 + 微缩放（fade-through）。
 * 旧的全宽滑动会拖着整屏内容（含液态玻璃）逐帧位移——玻璃采样随位移逐帧
 * 重绘，是转场掉帧的大头；淡入淡出下页面内容不位移，已渲染的玻璃层直接
 * 以图层变换复用，转场近似零玻璃重绘成本，观感轻快（iOS sheet 式层次感）。
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
    fadeIn(tween(AppMotion.NAV_FADE)) +
        scaleIn(
            initialScale = 0.94f,
            animationSpec = tween(AppMotion.NAV_SCALE, easing = AppMotion.EmphasizedEasing),
        )

fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit(): ExitTransition =
    fadeOut(tween(AppMotion.NAV_FADE)) +
        scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(AppMotion.NAV_SCALE, easing = AppMotion.EmphasizedEasing),
        )

fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    fadeIn(tween(AppMotion.NAV_FADE)) +
        scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(AppMotion.NAV_SCALE, easing = AppMotion.EmphasizedEasing),
        )

fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    fadeOut(tween(AppMotion.NAV_FADE)) +
        scaleOut(
            targetScale = 0.94f,
            animationSpec = tween(AppMotion.NAV_SCALE, easing = AppMotion.EmphasizedEasing),
        )
