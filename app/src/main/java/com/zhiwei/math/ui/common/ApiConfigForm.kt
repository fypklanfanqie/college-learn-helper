package com.zhiwei.math.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhiwei.math.llm.core.Protocol
import com.zhiwei.math.llm.core.ProviderPreset
import com.zhiwei.math.llm.core.ProviderPresets

/** 厂商卡片行 */
@Composable
fun ProviderPicker(
    selectedId: String,
    onSelect: (ProviderPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ProviderPresets.ALL + ProviderPresets.CUSTOM) { preset ->
            Card(
                onClick = { onSelect(preset) },
                colors = CardDefaults.cardColors(
                    containerColor = if (preset.id == selectedId)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(preset.displayName, style = MaterialTheme.typography.titleSmall)
                    if (preset.supportsVision && preset.id != ProviderPresets.CUSTOM.id) {
                        Text("👁 视觉", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/** API 配置表单（Onboarding 与设置页共用） */
@Composable
fun ApiConfigForm(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    model: String,
    onModelChange: (String) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    protocol: Protocol,
    onProtocolChange: (Protocol) -> Unit,
    modelSuggestions: List<String>,
    supportsVision: Boolean,
    onSupportsVisionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = protocol == Protocol.OPENAI,
                onClick = { onProtocolChange(Protocol.OPENAI) },
                label = { Text("OpenAI 协议") },
            )
            FilterChip(
                selected = protocol == Protocol.ANTHROPIC,
                onClick = { onProtocolChange(Protocol.ANTHROPIC) },
                label = { Text("Anthropic 协议") },
            )
        }
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = model,
            onValueChange = onModelChange,
            label = { Text("模型 ID") },
            supportingText = {
                if (modelSuggestions.isNotEmpty()) {
                    Text("可选：" + modelSuggestions.take(4).joinToString(" / "))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key（加密存储在设备上）") },
            supportingText = { Text("💡 最好使用支持视觉输入的 API，拍照题目可以直接识别") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // 用户处方：填写 API 时显式声明是否支持视觉输入。
        // 开 → 拍照/选图直接把图片发给模型；关 → 本地 OCR 提取文字后发送。
        val modelLooksTextOnly = model.lowercase().contains("deepseek") &&
            !model.lowercase().contains("vision")
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("支持视觉输入", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (supportsVision) {
                                "开：拍照/选图直接发给模型识别（需视觉模型）"
                            } else {
                                "关：图片用本地 OCR 提取文字（数学公式效果差）"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (supportsVision && modelLooksTextOnly) {
                            Text(
                                "⚠ 模型「$model」疑似不支持视觉，建议改用 *-vision 型号或关闭此开关",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Switch(
                        checked = supportsVision,
                        onCheckedChange = onSupportsVisionChange,
                    )
                }
            }
        }
    }
}
