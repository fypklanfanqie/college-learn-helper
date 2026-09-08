package com.zhiwei.math.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.zhiwei.math.glass.pressableScale
import com.zhiwei.math.glass.rememberHaptic
import com.zhiwei.math.ui.icons.SfChevronRight
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS 分组列表组件（Settings 式 Inset Grouped List）：
 * CardGroup = 16dp 连续圆角白卡 + hairline 描边；
 * CardRow = 52dp 高行（leading 29dp 图标圈 / title / trailing / chevron）；
 * 行间 divider 从文字缩进处起（跟随 leading 有无）。
 */

/** 分组卡片容器 */
@Composable
fun CardGroup(
    modifier: Modifier = Modifier,
    showBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalIosPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.card, IosShapes.Card)
            .then(
                if (showBorder) Modifier.border(0.5.dp, palette.separator, IosShapes.Card)
                else Modifier
            ),
        content = content,
    )
}

/**
 * 分组行：52dp 高。leading 存在时文字缩进与 divider 起点对齐图标区。
 */
@Composable
fun CardRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    leadingIcon: ImageVector? = null,
    leadingIconBg: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    titleColor: Color? = null,
) {
    val palette = LocalIosPalette.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .then(
                if (onClick != null && enabled) {
                    Modifier
                        .clickable(interactionSource = interaction, indication = null) {
                            haptic()
                            onClick()
                        }
                        .pressableScale(interaction)
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = if (subtitle != null) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        } else if (leadingIcon != null) {
            // 29dp 圆角图标圈（SF 图标白前景 + 主色底）
            Box(
                modifier = Modifier.size(29.dp).background(
                    leadingIconBg ?: palette.blue,
                    IosShapes.IconCircle,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor ?: palette.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondaryLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingText != null) {
            Text(
                trailingText,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.secondaryLabel,
                modifier = Modifier.padding(end = if (showChevron) 6.dp else 0.dp),
            )
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                imageVector = SfChevronRight,
                contentDescription = null,
                tint = palette.tertiaryLabel,
                modifier = Modifier.size(14.dp).padding(start = 2.dp),
            )
        }
    }
    if (showDivider) {
        // divider 从文字起点（图标区之后）起 —— iOS 分组列表特征
        val inset = if (leading != null || leadingIcon != null) 16 + 29 + 12 else 16
        Spacer(
            Modifier
                .padding(start = inset.dp)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(palette.separator),
        )
    }
}

/** 分组标题（13sp 灰字距，iOS section header） */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    val palette = LocalIosPalette.current
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = palette.secondaryLabel,
        modifier = modifier.padding(start = 22.dp, bottom = 6.dp),
    )
}

/** 分组脚注（灰色说明文字） */
@Composable
fun SectionFooter(text: String, modifier: Modifier = Modifier) {
    val palette = LocalIosPalette.current
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = palette.secondaryLabel,
        modifier = modifier.padding(start = 22.dp, top = 6.dp, end = 16.dp),
    )
}

/** 分组容器 + 标题的常用组合 */
@Composable
fun Section(
    title: String,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionHeader(title)
    CardGroup { content() }
    if (footer != null) SectionFooter(footer)
}
