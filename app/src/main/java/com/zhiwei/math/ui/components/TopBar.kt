package com.zhiwei.math.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Rectangle
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.LocalGlassMode
import com.zhiwei.math.glass.GlassMode
import com.zhiwei.math.glass.liquidGlass
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS 顶栏（作为 GlassHost overlay 的兄弟玻璃）：
 * - 根页签页用 IosLargeTitleHeader：LargeTitle 34sp 左对齐，滚动后收缩成
 *   17sp 居中小标题 + 玻璃底衬浮现（collapsed 由屏幕根据滚动状态传入）。
 * - push 详情页用 IosNavBar：固定小标题栏（返回 + 标题 + actions），玻璃底衬常显。
 */

/** 顶栏玻璃底衬（全宽矩形玻璃，alpha 渐显） */
@Composable
private fun Modifier.topBarGlass(visibleAlpha: Float): Modifier {
    val palette = LocalIosPalette.current
    val mode = LocalGlassMode.current
    // 矩形玻璃（Rectangle 为 object，0 圆角 SDF，lens 可用）
    val shape: Shape = Rectangle
    return this.then(
        Modifier
            .graphicsLayer { alpha = visibleAlpha }
            .liquidGlass(
                shape = shape,
                surfaceColor = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
            )
    )
}

/** push 详情页固定顶栏：返回按钮 + 标题（17sp SemiBold）+ actions，玻璃底衬常显 */
@Composable
fun IosNavBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalIosPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .topBarGlass(1f)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IosBackButton(onClick = onBack)
            } else {
                Spacer(Modifier.width(44.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.secondaryLabel,
                        maxLines = 1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }
    }
}

/**
 * 根页签页大标题头：未折叠 = LargeTitle 34sp 左对齐；折叠 = 44dp 小标题行居中 +
 * 玻璃底衬（snappy 渐显）。trailingActions 挂在大标题行右侧（如右上 plus）。
 */
@Composable
fun IosLargeTitleHeader(
    title: String,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    trailingActions: @Composable RowScope.() -> Unit = {},
    collapsedActions: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalIosPalette.current
    val collapseProgress by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = AppMotion.snappy(),
        label = "largeTitleCollapse",
    )
    val glassAlpha = 0.94f * collapseProgress

    Column(
        modifier = modifier
            .fillMaxWidth()
            .topBarGlass(glassAlpha)
            .statusBarsPadding(),
    ) {
        // 小标题行（iOS：44pt 导航行常驻，折叠后显示标题；未折叠时透明占位）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp)
                .graphicsLayer { alpha = collapseProgress },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.label,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically, content = collapsedActions)
            Spacer(Modifier.weight(1f))
        }
        // 大标题区（折叠时高度→0 + 淡出）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((52.dp) * (1f - collapseProgress))
                .padding(horizontal = 20.dp)
                .graphicsLayer { alpha = 1f - collapseProgress },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.displayLarge,
                color = palette.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) { trailingActions() }
        }
    }
}
