package com.zhiwei.math.ui.subject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 科目选择「你要学什么」（主页页签）：V1 仅高等数学可用，线代/概率论"即将上线"置灰。
 */
@Composable
fun SubjectScreen(
    onStartMath: () -> Unit,
    onOpenSettings: () -> Unit = {},
    embedded: Boolean = false,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知微数学") },
                actions = {
                    if (!embedded) TextButton(onClick = onOpenSettings) { Text("设置") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Text("你要学什么？", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))

            SubjectCard(
                title = "高等数学",
                subtitle = "同济版 · 宋浩风格精讲 / 期末冲刺",
                enabled = true,
                onClick = onStartMath,
            )
            Spacer(Modifier.height(12.dp))
            SubjectCard(
                title = "线性代数",
                subtitle = "即将上线",
                enabled = false,
                onClick = {},
            )
            Spacer(Modifier.height(12.dp))
            SubjectCard(
                title = "概率论与数理统计",
                subtitle = "即将上线",
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Composable
private fun SubjectCard(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                if (!enabled) {
                    Text("🔒", style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
