package com.zhiwei.math.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.GlassMode
import com.zhiwei.math.glass.LocalGlassMode
import com.zhiwei.math.glass.LocalLayerBackdrop
import com.zhiwei.math.glass.liquidGlass
import com.zhiwei.math.glass.pressableScale
import com.zhiwei.math.glass.rememberHaptic
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 液态玻璃 dock（LiquidBottomTabs 模式）：
 * - 整条 Capsule 玻璃（vibrancy + blur + lens 真实折射），innerShadow 8dp；
 * - 当前页指示滑块是【玻璃上玻璃】：dock 的 drawBackdrop 传
 *   exportedBackdrop = subBackdrop，滑块采样 subBackdrop（绝不给 dock 套
 *   layerBackdrop —— 崩溃规避铁律 2）；
 * - 选中图标放大 1.15 + 蓝色 tint + label 只显示选中项（iOS 18 式）；
 * - 支持水平拖拽换页（滑块跟手 + 松手 snappy 吸附最近页签）；
 * - FROSTED/PLAIN 模式降级为同布局半透明胶囊。
 */
data class DockItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun LiquidDock(
    items: List<DockItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalIosPalette.current
    val mode = LocalGlassMode.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
            .height(62.dp),
    ) {
        val segWidth = maxWidth / items.size.coerceAtLeast(1)
        val sliderWidth = (segWidth - 8.dp).coerceAtMost(60.dp)
        val segPx = with(density) { segWidth.toPx() }
        val sliderPx = with(density) { sliderWidth.toPx() }
        fun sliderCenterOf(index: Int): Float = segPx * index + (segPx - sliderPx) / 2f

        // 滑块位置 px：非拖拽时 spring 到选中段中心；拖拽时跟手
        var draggingAccum by remember { mutableFloatStateOf(0f) }
        val sliderX = remember { Animatable(sliderCenterOf(selected)) }
        LaunchedEffect(maxWidth, items.size, selected) {
            if (abs(draggingAccum) < 0.5f) {
                sliderX.animateTo(sliderCenterOf(selected), AppMotion.bouncy())
            }
        }

        // ── dock 整条玻璃（玻璃上玻璃：导出 subBackdrop 给滑块采样）──
        val subBackdrop = rememberLayerBackdrop()
        val dockSurface = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
        val isLiquid = mode == GlassMode.LIQUID && LocalLayerBackdrop.current != null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when {
                        isLiquid -> Modifier.liquidGlass(
                            shape = IosShapes.Capsule,
                            surfaceColor = dockSurface,
                            innerShadowRadius = 8.dp,
                            exportedBackdrop = subBackdrop,
                        )
                        mode == GlassMode.FROSTED -> Modifier.liquidGlass(
                            shape = IosShapes.Capsule,
                            surfaceColor = dockSurface,
                        )
                        else -> Modifier
                            .clip(IosShapes.Capsule)
                            .background(dockSurface.copy(alpha = 0.92f), IosShapes.Capsule)
                    }
                )
                .draggable(
                    state = rememberDraggableState { delta ->
                        draggingAccum += delta
                        scope.launch { sliderX.snapTo(sliderX.value + delta) }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStarted = { draggingAccum = 0f },
                    onDragStopped = {
                        val target = (((sliderX.value - (segPx - sliderPx) / 2f) / segPx)
                            .roundToInt()).coerceIn(0, items.size - 1)
                        if (target != selected) {
                            haptic()
                            onSelect(target)
                        }
                        draggingAccum = 0f
                        scope.launch {
                            sliderX.animateTo(sliderCenterOf(target), AppMotion.snappy())
                        }
                    },
                ),
        ) {
            // 选中滑块（先声明 → 画在图标层之下）：玻璃上玻璃采样 subBackdrop
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(sliderX.value.roundToInt(), 0) }
                    .width(sliderWidth)
                    .height(46.dp)
                    .then(
                        if (isLiquid) {
                            Modifier.liquidGlass(
                                shape = IosShapes.Capsule,
                                surfaceColor = Color.White.copy(alpha = 0.35f),
                            )
                        } else {
                            Modifier
                                .clip(IosShapes.Capsule)
                                .background(
                                    if (palette.isDark) {
                                        Color.White.copy(alpha = 0.12f)
                                    } else {
                                        Color.White.copy(alpha = 0.55f)
                                    },
                                    IosShapes.Capsule,
                                )
                        }
                    ),
            )

            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selected
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1f,
                        animationSpec = AppMotion.bouncy(),
                        label = "dockIcon$index",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickableNoRipple {
                                if (!isSelected) {
                                    haptic()
                                    onSelect(index)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) palette.blue else palette.secondaryLabel,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                            // iOS 18 式：label 只显示选中项
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(AppMotion.snappy()),
                                exit = fadeOut(AppMotion.snappy()),
                            ) {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.blue,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 无涟漪点击 + 按压缩放（复用 Motion.kt 的 pressableScale） */
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    this
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .then(Modifier.pressableScale(interaction, pressedScale = 0.92f))
}
