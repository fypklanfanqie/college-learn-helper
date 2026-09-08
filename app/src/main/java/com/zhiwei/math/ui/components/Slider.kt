package com.zhiwei.math.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.LocalHapticsEnabled
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS 滑杆：4dp 轨道（灰底 + 蓝色填充）+ 22dp 白色圆钮（阴影 + 拖动放大 1.1）。
 * 全高 44dp 命中区（iOS hit target），拖动跨刻度打点震感。
 */
@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val palette = LocalIosPalette.current
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    var dragging by remember { mutableStateOf(false) }
    // 本地 fraction：拖动中即时跟手，外部 value 变化（预设切换）时同步
    var fraction by remember { mutableFloatStateOf(fractionOf(value, valueRange)) }
    LaunchedEffect(value, valueRange) { fraction = fractionOf(value, valueRange) }

    val knobScale by animateFloatAsState(
        targetValue = if (dragging) 1.1f else 1f,
        animationSpec = AppMotion.bouncy(),
        label = "sliderKnob",
    )

    var widthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val knobSizePx = with(density) { 22.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .draggable(
                state = rememberDraggableState { delta ->
                    if (widthPx > knobSizePx) {
                        val newFraction = (fraction + delta / (widthPx - knobSizePx)).coerceIn(0f, 1f)
                        if (hapticsEnabled && (fraction * 100).toInt() != (newFraction * 100).toInt()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        fraction = newFraction
                        onValueChange(
                            valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                        )
                    }
                },
                orientation = Orientation.Horizontal,
                startDragImmediately = true,
                onDragStarted = { dragging = true },
                onDragStopped = {
                    dragging = false
                    onValueChangeFinished?.invoke()
                },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 灰色轨道
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(palette.fill, IosShapes.Capsule),
        )
        // 蓝色填充（fraction 比例宽）
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .background(palette.blue, IosShapes.Capsule),
        )
        // 白色圆钮：中心 x = knob/2 + fraction*(width-knob)
        val knobX = with(density) {
            (knobSizePx / 2f + fraction * (widthPx - knobSizePx)).toDp() - 11.dp
        }
        Box(
            Modifier
                .offset(x = knobX)
                .size(22.dp)
                .graphicsLayer {
                    scaleX = knobScale
                    scaleY = knobScale
                }
                .shadow(3.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f))
                .background(Color.White, CircleShape),
        )
    }
}

private fun fractionOf(value: Float, range: ClosedFloatingPointRange<Float>): Float =
    if (range.endInclusive == range.start) 0f
    else ((value - range.start) / (valueRangeSpan(range))).coerceIn(0f, 1f)

private fun valueRangeSpan(range: ClosedFloatingPointRange<Float>): Float =
    range.endInclusive - range.start

/**
 * 设置页滑杆行：label + 当前值 + IosSlider + 脚注说明（玻璃参数 10 项全用它）。
 */
@Composable
fun GlassSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueSuffix: String = "",
    footnote: String? = null,
    integersOnly: Boolean = true,
) {
    val palette = LocalIosPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.label,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (integersOnly) "${value.toInt()}$valueSuffix" else "%.1f$valueSuffix".format(value),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryLabel,
            )
        }
        IosSlider(value = value, onValueChange = onChange, valueRange = valueRange)
        if (footnote != null) {
            Text(
                footnote,
                style = MaterialTheme.typography.labelSmall,
                color = palette.tertiaryLabel,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
