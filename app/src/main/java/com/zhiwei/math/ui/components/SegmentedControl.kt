package com.zhiwei.math.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.pressableScale
import com.zhiwei.math.glass.rememberHaptic
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS 分段控件（Segmented Control）：
 * 灰底胶囊容器 + 白色滑块 snappy spring 移动 + 选中文字变主色/黑。
 * 替代 FilterChip（主题/字体/难度/例题本切换全用它）。
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalIosPalette.current
    val haptic = rememberHaptic()
    val index = options.indexOf(selected).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .height(34.dp)
            .background(palette.fill, IosShapes.Capsule),
    ) {
        val segWidth = maxWidth / options.size.coerceAtLeast(1)
        val thumbOffset by animateDpAsState(
            targetValue = segWidth * index,
            animationSpec = AppMotion.snappy(),
            label = "segThumb",
        )
        // 白色滑块
        Box(
            Modifier
                .offset(x = thumbOffset)
                .width(segWidth)
                .fillMaxHeight()
                .padding(2.dp)
                .background(palette.card, IosShapes.Capsule),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val isSel = option == selected
                val textColor by animateColorAsState(
                    targetValue = if (isSel) palette.label else palette.secondaryLabel,
                    animationSpec = AppMotion.snappy(),
                    label = "segText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (!isSel) {
                                haptic()
                                onSelect(option)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
