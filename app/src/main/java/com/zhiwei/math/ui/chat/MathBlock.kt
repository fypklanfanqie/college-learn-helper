package com.zhiwei.math.ui.chat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zhiwei.math.ui.theme.MiSans
import io.ratex.RaTeXView

/**
 * 独立公式块渲染：RaTeX（Rust 核心，>99.5% KaTeX 覆盖，无 WebView）。
 * 渲染失败（onError）时降级显示 LaTeX 原文——保证内容不丢。
 */
@Composable
fun MathBlock(
    latex: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 16f,
) {
    var failed by remember(latex) { mutableStateOf(false) }
    val textColorArgb = MaterialTheme.colorScheme.onSurface.toArgb()

    if (failed) {
        Text(
            text = latex,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MiSans),
            modifier = modifier,
        )
        return
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    ) {
        AndroidView(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            factory = { context ->
                RaTeXView(context).apply {
                    this.fontSize = fontSize
                    displayMode = true
                    onError = { failed = true }
                }
            },
            update = { view ->
                view.latex = latex
                view.color = textColorArgb
            },
        )
    }
}
