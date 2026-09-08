package com.zhiwei.math.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * iOS 输入框：灰底（fill）12dp 连续圆角、无边框、聚焦变蓝描边（1.5dp）。
 * 替代 OutlinedTextField；label 文案置于上方（iOS Form 风格）。
 */
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 4,
    fontSize: Int = 17,
) {
    val palette = LocalIosPalette.current
    var focused by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryLabel,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = TextStyle(
                fontSize = androidx.compose.ui.unit.TextUnit(fontSize.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp),
                color = palette.label,
            ),
            cursorBrush = SolidColor(palette.blue),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .background(palette.fill, IosShapes.Control)
                        .then(
                            if (focused) Modifier.border(1.5.dp, palette.blue, IosShapes.Control)
                            else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = if (minLines <= 1 && singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.tertiaryLabel,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
    }
}
