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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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

    const val NAV_DURATION = 340
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

/** 导航转场：新页从右滑入 + 淡入；返回时反向（iOS push/pop 观感） */
fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
    slideInHorizontally(animationSpec = tween(AppMotion.NAV_DURATION, easing = AppMotion.EmphasizedEasing)) { it }
        .plus(fadeIn(tween(AppMotion.NAV_DURATION)))

fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExit(): ExitTransition =
    slideOutHorizontally(animationSpec = tween(AppMotion.NAV_DURATION, easing = AppMotion.EmphasizedEasing)) { -it / 4 }
        .plus(fadeOut(tween(AppMotion.NAV_DURATION)))

fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideInHorizontally(animationSpec = tween(AppMotion.NAV_DURATION, easing = AppMotion.EmphasizedEasing)) { -it / 4 }
        .plus(fadeIn(tween(AppMotion.NAV_DURATION)))

fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutHorizontally(animationSpec = tween(AppMotion.NAV_DURATION, easing = AppMotion.EmphasizedEasing)) { it }
        .plus(fadeOut(tween(AppMotion.NAV_DURATION)))
