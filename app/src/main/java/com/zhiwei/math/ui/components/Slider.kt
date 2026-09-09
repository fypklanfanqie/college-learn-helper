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
import kotlin.math.roundToInt

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
    snapToIntegers: Boolean = true,
) {
    val palette = LocalIosPalette.current
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    var dragging by remember { mutableStateOf(false) }
    // 本地 fraction：拖动中滑杆是唯一权威（外部 value 异步回流【不】覆盖，防抽搐）；
    // 非拖动时外部 value 变化（预设切换/重新进页）才同步进来
    var fraction by remember { mutableFloatStateOf(fractionOf(value, valueRange)) }
    // 最近一次上报的值：松手后 fraction 吸附到它（行级取整后恰好落在整数刻度上）
    var lastEmitted by remember { mutableFloatStateOf(value) }
    LaunchedEffect(value, valueRange) {
        if (!dragging) {
            fraction = fractionOf(value, valueRange)
            lastEmitted = value
        }
    }

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
                        // 取整上报：整数滑杆行（玻璃参数全部 integersOnly）持久值
                        // 与手指位置一致，回流不再产生截断偏差
                        val raw = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                        val emitted =
                            if (snapToIntegers) {
                                raw.roundToInt().toFloat().coerceIn(valueRange.start, valueRange.endInclusive)
                            } else {
                                raw
                            }
                        lastEmitted = emitted
                        onValueChange(emitted)
                    }
                },
                orientation = Orientation.Horizontal,
                startDragImmediately = true,
                onDragStarted = { dragging = true },
                onDragStopped = {
                    dragging = false
                    // 松手：吸附到最近一次上报值（整数刻度），回调收尾
                    fraction = fractionOf(lastEmitted, valueRange)
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
        IosSlider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange,
            snapToIntegers = integersOnly,
        )
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
