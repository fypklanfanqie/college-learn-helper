package com.zhiwei.math.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS action sheet 式菜单（底部弹层 + 菜单行），替代 Material DropdownMenu。
 * destructive 行红字（删除等）。
 */
data class MenuItem(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
)

@Composable
fun IosMenuSheet(
    items: List<MenuItem>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val palette = LocalIosPalette.current
    IosBottomSheet(onDismiss = onDismiss, modifier = modifier) {
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(item.label) }
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.icon != null) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (item.destructive) palette.red else palette.blue,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                }
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.destructive) palette.red else palette.label,
                )
            }
            if (index < items.size - 1) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(palette.separator),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
