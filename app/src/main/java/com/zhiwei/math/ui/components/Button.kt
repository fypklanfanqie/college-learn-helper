package com.zhiwei.math.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.pressableScale
import com.zhiwei.math.glass.rememberHaptic
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS 按钮组：Filled（蓝底白字 50dp 高 14dp 连续圆角，按压缩放 0.96 + 变暗）、
 * Plain（蓝字无底）、Destructive（红字）、IconButton（44dp 圆形透明，SF 图标）。
 * loading 时按钮内转圈（自动禁点）。
 */

@Composable
fun IosFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color? = null,
    textColor: Color? = null,
    leadingIcon: ImageVector? = null,
) {
    val palette = LocalIosPalette.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val bg = containerColor ?: palette.blue
    val fg = textColor ?: Color.White
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 50.dp)
            .then(
                if (enabled && !loading) {
                    Modifier
                        .clickable(interactionSource = interaction, indication = null) {
                            haptic()
                            onClick()
                        }
                        .pressableScale(interaction)
                } else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = fg,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else if (leadingIcon != null) {
            Icon(leadingIcon, null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 胶囊按钮（如设置页预设行） */
@Composable
fun IosCapsuleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val palette = LocalIosPalette.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null) {
                        haptic()
                        onClick()
                    }
                    .pressableScale(interaction, pressedScale = 0.94f)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color.White else palette.blue,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .then(
                    if (selected) Modifier.padding(0.dp) else Modifier.padding(0.dp)
                ),
        )
    }
}

/** 蓝色文字按钮（iOS plain 按钮） */
@Composable
fun IosPlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    compact: Boolean = false,
) {
    val palette = LocalIosPalette.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    Text(
        text,
        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
        color = when {
            destructive -> palette.red
            !enabled -> palette.tertiaryLabel
            else -> palette.blue
        },
        maxLines = 1,
        modifier = modifier
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null) {
                        haptic()
                        onClick()
                    }
                    .pressableScale(interaction, pressedScale = 0.97f)
                else Modifier
            )
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 4.dp else 6.dp),
    )
}

/** 红色小字按钮（消息气泡下的操作行用） */
@Composable
fun IosActionText(
    text: String,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    IosPlainButton(text = text, onClick = onClick, enabled = enabled, destructive = destructive, compact = true)
}

/** 44dp 圆形图标按钮（顶栏动作/返回），SF 图标 */
@Composable
fun IosIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    enabled: Boolean = true,
    size: Int = 44,
) {
    val palette = LocalIosPalette.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size.dp)
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null) {
                        haptic()
                        onClick()
                    }
                    .pressableScale(interaction, pressedScale = 0.88f)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: palette.blue,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** 顶栏返回按钮：chevron.backward + 蓝色文字（iOS 风格） */
@Composable
fun IosBackButton(onClick: () -> Unit, label: String = "返回") {
    val palette = LocalIosPalette.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clickable(interactionSource = interaction, indication = null) {
                haptic()
                onClick()
            }
            .pressableScale(interaction, pressedScale = 0.96f)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = com.zhiwei.math.ui.icons.SfChevronBackward,
            contentDescription = label,
            tint = palette.blue,
            modifier = Modifier.size(18.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.blue,
            modifier = Modifier.padding(start = 2.dp),
        )
    }
}
