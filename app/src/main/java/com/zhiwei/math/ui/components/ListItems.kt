package com.zhiwei.math.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.pressableScale
import com.zhiwei.math.glass.rememberHaptic
import com.zhiwei.math.glass.rememberLongPressHaptic
import com.zhiwei.math.ui.icons.SfTrash
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * iOS 左滑删除：内容向左拖出红色删除按钮（跟手 + 松手 snappy spring 吸附）；
 * 点删除按钮执行 onDelete 并回弹；点内容区收起（未滑动时执行 onClick，
 * 供对话列表点击进对话等场景）。
 * 外层按 shape 裁剪：红色删除层与卡片圆角完全对齐，杜绝红边透出。
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IosShapes.Card,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalIosPalette.current
    val longPressHaptic = rememberLongPressHaptic()
    val lightHaptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val deleteWidthPx = with(density) { 76.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    var open by remember { mutableStateOf(false) }
    val tapInteraction = remember { MutableInteractionSource() }

    Box(modifier = modifier.fillMaxWidth().clip(shape)) {
        // 删除按钮：右侧底层，全高 76dp（与卡片同形状，被外层 clip 兜底）
        Box(
            modifier = Modifier
                .matchParentSize(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .fillMaxHeight()
                    .background(palette.red, shape)
                    .clickable {
                        longPressHaptic()
                        open = false
                        scope.launch {
                            offsetX.animateTo(0f, androidx.compose.animation.core.tween(120))
                            onDelete()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        SfTrash,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("删除", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        // 内容：跟随水平偏移（按压缩放反馈；点击=收起/onClick）
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pressableScale(tapInteraction, pressedScale = 0.985f)
                .then(
                    if (enabled) Modifier.draggable(
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                offsetX.snapTo((offsetX.value + delta).coerceIn(-deleteWidthPx, 0f))
                            }
                        },
                        orientation = Orientation.Horizontal,
                        onDragStarted = { lightHaptic() },
                        onDragStopped = {
                            val shouldOpen = offsetX.value < -deleteWidthPx / 2
                            open = shouldOpen
                            scope.launch {
                                offsetX.animateTo(
                                    if (shouldOpen) -deleteWidthPx else 0f,
                                    AppMotion.snappy(),
                                )
                            }
                        },
                    ) else Modifier
                )
                .clickable(
                    interactionSource = tapInteraction,
                    indication = null,
                    enabled = open || onClick != null,
                ) {
                    if (open) {
                        open = false
                        scope.launch { offsetX.animateTo(0f, AppMotion.snappy()) }
                    } else {
                        lightHaptic()
                        onClick?.invoke()
                    }
                },
        ) {
            content()
        }
    }
}

/**
 * 空态提示：SF 风格灰色图标位 + 标题 + 副标题（居中）。
 */
@Composable
fun EmptyHint(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
) {
    val palette = LocalIosPalette.current
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = palette.tertiaryLabel,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.secondaryLabel,
                textAlign = TextAlign.Center,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.tertiaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
