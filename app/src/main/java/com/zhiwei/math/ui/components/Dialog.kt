package com.zhiwei.math.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * iOS 弹窗（24dp 连续圆角 elevated 卡 + 标题/正文/按钮行上下分隔线，
 * destructive 红字确认）。extraContent 可放输入框。
 */
@Composable
fun IosAlertDialog(
    onDismiss: () -> Unit,
    title: String,
    message: String? = null,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    dismissText: String = "取消",
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = LocalIosPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(palette.elevated, IosShapes.Dialog),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(22.dp))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.label,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (message != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            if (extraContent != null) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.padding(horizontal = 20.dp)) { extraContent() }
            }
            Spacer(Modifier.height(18.dp))
            // 分隔线 + 按钮行（iOS alert 全宽等分按钮）
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(palette.separator),
            )
            Row(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(dismissText, style = MaterialTheme.typography.bodyLarge, color = palette.blue)
                }
                Spacer(
                    Modifier
                        .width(0.5.dp)
                        .height(48.dp)
                        .background(palette.separator),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = {
                            onConfirm()
                            onDismiss()
                        })
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (destructive) palette.red else palette.blue,
                    )
                }
            }
        }
    }
}

/**
 * iOS 底部弹层（24dp 顶圆角 + 拖拽把手 + 手势下滑 dismiss + snappy spring）。
 * 内容需可滚动时把滚动容器放 content 内。
 */
@Composable
fun IosBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalIosPalette.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Dialog 内容默认居中；外层铺满 + BottomCenter 对齐实现底部弹层
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(palette.elevated, IosShapes.SheetTop),
            ) {
                SheetDragHandle(onDismiss = onDismiss)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp)
                        .imePadding(),
                    content = content,
                )
            }
        }
    }
}

/** 顶部拖拽把手：下滑超过阈值 dismiss */
@Composable
private fun SheetDragHandle(onDismiss: () -> Unit) {
    val palette = LocalIosPalette.current
    val dragY = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val thresholdPx = with(density) { 90.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .offset { IntOffset(0, dragY.value.roundToInt()) }
            .draggable(
                state = rememberDraggableState { delta ->
                    scope.launch { dragY.snapTo((dragY.value + delta).coerceAtLeast(0f)) }
                },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (dragY.value > thresholdPx || velocity > 800f) {
                        onDismiss()
                    } else {
                        scope.launch { dragY.animateTo(0f, AppMotion.snappy()) }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(36.dp, 5.dp)
                .background(palette.separator.copy(alpha = 0.6f), IosShapes.Capsule),
        )
    }
}
