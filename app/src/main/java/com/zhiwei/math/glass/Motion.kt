package com.zhiwei.math.glass

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * 苹果风动画 tokens（PROJECT-BRIEF.md 7.1：iOS 风格 spring/emphasized 曲线）。
 * emphasized easing ≈ iOS 的手势驱动曲线；spring 用于元素级动效。
 */
object AppMotion {
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val StandardEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** iOS push 式 spring */
    fun <T> iOSspring() = spring<T>(
        dampingRatio = 0.86f,
        stiffness = 420f,
    )

    const val NAV_DURATION = 340
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
