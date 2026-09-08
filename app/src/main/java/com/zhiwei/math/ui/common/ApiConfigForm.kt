package com.zhiwei.math.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhiwei.math.llm.core.Protocol
import com.zhiwei.math.llm.core.ProviderPreset
import com.zhiwei.math.llm.core.ProviderPresets
import com.zhiwei.math.ui.components.CardGroup
import com.zhiwei.math.ui.components.CardRow
import com.zhiwei.math.ui.components.IosSwitch
import com.zhiwei.math.ui.components.IosTextField
import com.zhiwei.math.ui.components.SectionHeader
import com.zhiwei.math.ui.components.SegmentedControl
import com.zhiwei.math.ui.icons.SfEye
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * 厂商选择卡片行（iOS 观感：可选中卡片，选中蓝描边 + 眼睛视觉标记）。
 */
@Composable
fun ProviderPicker(
    selectedId: String,
    onSelect: (ProviderPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalIosPalette.current
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ProviderPresets.ALL + ProviderPresets.CUSTOM) { preset ->
            val selected = preset.id == selectedId
            Column(
                modifier = Modifier
                    .then(
                        Modifier
                            .background(
                                if (selected) palette.blue.copy(alpha = 0.10f) else palette.card,
                                IosShapes.Control,
                            )
                            .let { m ->
                                if (selected) {
                                    m.border(1.5.dp, palette.blue, IosShapes.Control)
                                } else m
                            }
                    )
                    .clickable { onSelect(preset) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    preset.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) palette.blue else palette.label,
                )
                if (preset.supportsVision && preset.id != ProviderPresets.CUSTOM.id) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            SfEye,
                            contentDescription = "视觉",
                            tint = palette.secondaryLabel,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            " 视觉",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryLabel,
                        )
                    }
                }
            }
        }
    }
}

/** API 配置表单（Onboarding 与设置页共用，iOS 观感） */
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
    val palette = LocalIosPalette.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader("协议")
        SegmentedControl(
            options = listOf(Protocol.OPENAI, Protocol.ANTHROPIC),
            selected = protocol,
            onSelect = onProtocolChange,
            label = { if (it == Protocol.OPENAI) "OpenAI 协议" else "Anthropic 协议" },
        )

        IosTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = "Base URL",
            placeholder = "https://api.example.com/v1",
        )
        IosTextField(
            value = model,
            onValueChange = onModelChange,
            label = "模型 ID",
            placeholder = "如 deepseek-chat",
        )
        if (modelSuggestions.isNotEmpty()) {
            Text(
                "可选：" + modelSuggestions.take(4).joinToString(" / "),
                style = MaterialTheme.typography.labelSmall,
                color = palette.tertiaryLabel,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        IosTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = "API Key（加密存储在设备上）",
            placeholder = "sk-…",
        )
        Text(
            "💡 最好使用支持视觉输入的 API，拍照题目可以直接识别",
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondaryLabel,
            modifier = Modifier.padding(start = 4.dp),
        )

        // 用户处方：显式声明是否支持视觉输入（开 → 直接发图；关 → 本地 OCR）
        val modelLooksTextOnly = model.lowercase().contains("deepseek") &&
            !model.lowercase().contains("vision")
        CardGroup {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "支持视觉输入",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.label,
                        )
                        Text(
                            if (supportsVision) {
                                "开：拍照/选图直接发给模型识别（需视觉模型）"
                            } else {
                                "关：图片用本地 OCR 提取文字（数学公式效果差）"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.secondaryLabel,
                        )
                    }
                    IosSwitch(checked = supportsVision, onCheckedChange = onSupportsVisionChange)
                }
                if (supportsVision && modelLooksTextOnly) {
                    Text(
                        "⚠ 模型「$model」疑似不支持视觉，建议改用 *-vision 型号或关闭此开关",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.red,
                    )
                }
            }
        }
    }
}
