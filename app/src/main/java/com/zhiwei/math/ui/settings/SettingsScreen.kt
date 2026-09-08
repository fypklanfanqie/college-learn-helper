package com.zhiwei.math.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.prefs.AppearanceSettings
import com.zhiwei.math.data.prefs.GlassSettings
import com.zhiwei.math.data.prefs.LIQUID_PRESETS
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.GlassHost
import com.zhiwei.math.glass.GlassMode
import com.zhiwei.math.glass.liquidGlass
import com.zhiwei.math.ui.components.CardGroup
import com.zhiwei.math.ui.components.CardRow
import com.zhiwei.math.ui.components.GlassSliderRow
import com.zhiwei.math.ui.components.IosSwitch
import com.zhiwei.math.ui.components.Section
import com.zhiwei.math.ui.components.SectionFooter
import com.zhiwei.math.ui.components.SectionHeader
import com.zhiwei.math.ui.components.SegmentedControl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 设置页（iOS 分组列表 + 玻璃参数大扩充）：
 * - 外观：主题/字体（SegmentedControl）、聊天背景、震动（iOS Switch）；
 * - 玻璃效果：模式 SegmentedControl + 预设胶囊行 + 实时预览卡 +
 *   LIQUID 10 滑杆 / FROSTED 4 滑杆（全部实时生效）；
 * - API / 功能 / 关于。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenApiSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
    embedded: Boolean = false,
    onCollapsedChanged: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val palette = com.zhiwei.math.ui.theme.LocalIosPalette.current
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val glass by viewModel.glass.collectAsStateWithLifecycle()
    val liquidDisabled by viewModel.liquidDisabled.collectAsStateWithLifecycle()
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setChatBackground(uri.toString())
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.value) { onCollapsedChanged(scrollState.value > 4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scrollState),
    ) {
        Spacer(Modifier.statusBarsPadding().height(96.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {

            // ── 外观 ─────────────────────────────────────────
            Section("外观") {
                CardRow(
                    title = "主题",
                    showDivider = true,
                    trailing = {
                        SegmentedControl(
                            options = listOf("system", "light", "dark"),
                            selected = appearance.theme,
                            onSelect = viewModel::setTheme,
                            label = { mapOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色")[it] ?: it },
                            modifier = Modifier.width(210.dp),
                        )
                    },
                )
                CardRow(
                    title = "字体",
                    showDivider = true,
                    trailing = {
                        SegmentedControl(
                            options = listOf("misans", "system"),
                            selected = appearance.font,
                            onSelect = viewModel::setFont,
                            label = { if (it == "misans") "MiSans" else "系统" },
                            modifier = Modifier.width(160.dp),
                        )
                    },
                )
                CardRow(
                    title = "聊天背景",
                    trailingText = if (appearance.chatBackgroundUri.isNotBlank()) "已设置" else "默认",
                    showChevron = true,
                    showDivider = true,
                    onClick = {
                        backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
                if (appearance.chatBackgroundUri.isNotBlank()) {
                    CardRow(
                        title = "清除聊天背景",
                        titleColor = palette.red,
                        showDivider = true,
                        onClick = { viewModel.setChatBackground("") },
                    )
                }
                CardRow(
                    title = "震动反馈",
                    showDivider = false,
                    trailing = {
                        IosSwitch(checked = appearance.haptics, onCheckedChange = viewModel::setHaptics)
                    },
                )
            }

            // ── 玻璃效果 ─────────────────────────────────────
            SectionHeader("玻璃效果")
            val modeOptions = listOf(GlassMode.FROSTED, GlassMode.LIQUID, GlassMode.PLAIN)
            CardGroup {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    SegmentedControl(
                        options = modeOptions,
                        selected = GlassMode.fromId(glass.mode),
                        onSelect = viewModel::setGlassMode,
                        label = { it.label },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (liquidDisabled && GlassMode.fromId(glass.mode) == GlassMode.LIQUID) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "此设备液态玻璃曾渲染崩溃，已自动降级为毛玻璃。可点「重新启用」重试。",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.orange,
                        )
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.material3.TextButton(onClick = viewModel::reenableLiquid) {
                            Text("重新启用液态玻璃", color = palette.blue)
                        }
                    }
                }
                // 实时预览卡（彩色光斑 + 玻璃面板，实时反映全部参数）
                LiquidGlassPreview(glass)
                // 预设胶囊行（仅液态模式）
                if (GlassMode.fromId(glass.mode) == GlassMode.LIQUID) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LIQUID_PRESETS.forEach { (name, preset) ->
                            PresetCapsule(
                                text = name,
                                selected = glass == preset,
                                onClick = { viewModel.applyPreset(preset) },
                            )
                        }
                    }
                }
                // 滑杆区
                when (GlassMode.fromId(glass.mode)) {
                    GlassMode.LIQUID -> {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            GlassSliderRow("模糊", glass.blurRadius.toFloat(), 0f..40f, { v ->
                                viewModel.updateGlass(glass.copy(blurRadius = v.toInt()))
                            }, valueSuffix = "dp")
                            GlassSliderRow("折射高度", glass.refractionHeight.toFloat(), 0f..48f, { v ->
                                viewModel.updateGlass(glass.copy(refractionHeight = v.toInt()))
                            }, valueSuffix = "dp")
                            GlassSliderRow("折射量", glass.refractionAmount.toFloat(), 0f..96f, { v ->
                                viewModel.updateGlass(glass.copy(refractionAmount = v.toInt()))
                            }, valueSuffix = "dp")
                            GlassSliderRow("色散", glass.chromaticAberration.toFloat(), 0f..100f, { v ->
                                viewModel.updateGlass(glass.copy(chromaticAberration = v.toInt()))
                            }, valueSuffix = "%", footnote = "超过 50% 开启色散")
                            GlassSliderRow("景深", glass.depthEffect.toFloat(), 0f..100f, { v ->
                                viewModel.updateGlass(glass.copy(depthEffect = v.toInt()))
                            }, valueSuffix = "%", footnote = "超过 50% 开启景深")
                            GlassSliderRow("饱和", glass.vibrancy.toFloat(), 0f..200f, { v ->
                                viewModel.updateGlass(glass.copy(vibrancy = v.toInt()))
                            }, valueSuffix = "%")
                            GlassSliderRow("亮度", glass.brightness.toFloat(), -50f..50f, { v ->
                                viewModel.updateGlass(glass.copy(brightness = v.toInt()))
                            }, valueSuffix = "%")
                            GlassSliderRow("表面色调", glass.surfaceTint.toFloat(), 0f..100f, { v ->
                                viewModel.updateGlass(glass.copy(surfaceTint = v.toInt()))
                            }, valueSuffix = "%")
                            GlassSliderRow("边缘高光", glass.highlightAlpha.toFloat(), 0f..100f, { v ->
                                viewModel.updateGlass(glass.copy(highlightAlpha = v.toInt()))
                            }, valueSuffix = "%")
                            GlassSliderRow("阴影", glass.shadowAlpha.toFloat(), 0f..100f, { v ->
                                viewModel.updateGlass(glass.copy(shadowAlpha = v.toInt()))
                            }, valueSuffix = "%")
                        }
                    }
                    GlassMode.FROSTED -> {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            GlassSliderRow("模糊", glass.frostedBlur.toFloat(), 0f..40f, { v ->
                                viewModel.updateGlass(glass.copy(frostedBlur = v.toInt()))
                            }, valueSuffix = "dp")
                            GlassSliderRow("不透明度", glass.opacity.toFloat(), 30f..100f, { v ->
                                viewModel.updateGlass(glass.copy(opacity = v.toInt()))
                            }, valueSuffix = "%")
                            GlassSliderRow("色温", glass.frostedTint.toFloat(), 0f..100f, { v ->
                                viewModel.updateGlass(glass.copy(frostedTint = v.toInt()))
                            }, valueSuffix = "", footnote = "0 冷 ↔ 100 暖")
                            GlassSliderRow("顶部高光", glass.frostedHighlight.toFloat(), 0f..100f, { v ->
                                viewModel.updateGlass(glass.copy(frostedHighlight = v.toInt()))
                            }, valueSuffix = "%")
                        }
                    }
                    else -> Unit
                }
            }
            SectionFooter("液态玻璃需 Android 13+，毛玻璃需 Android 12+；低版本自动降级。参数实时生效，可在预览卡观察。")

            // ── API ─────────────────────────────────────────
            Section("API") {
                CardRow(
                    title = "API 配置",
                    subtitle = "模型商 / 密钥（BYOK，Key 只存在设备上）",
                    showChevron = true,
                    showDivider = false,
                    onClick = onOpenApiSettings,
                )
            }

            // ── 功能 ─────────────────────────────────────────
            Section("功能") {
                CardRow(
                    title = "使用教程",
                    showChevron = true,
                    showDivider = false,
                    onClick = onOpenTutorial,
                )
            }

            // ── 关于 ─────────────────────────────────────────
            Section("关于") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("知微数学 v0.1.0", style = MaterialTheme.typography.bodyLarge, color = palette.label)
                    Text("Font: MiSans © Xiaomi", style = MaterialTheme.typography.bodySmall, color = palette.secondaryLabel)
                    Text(
                        "Glass: AndroidLiquidGlass (backdrop) · Shapes · Haze",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondaryLabel,
                    )
                }
            }
            Spacer(Modifier.height(132.dp))
        }
    }
}

/** 预设胶囊（选中蓝底白字） */
@Composable
private fun PresetCapsule(text: String, selected: Boolean, onClick: () -> Unit) {
    val palette = com.zhiwei.math.ui.theme.LocalIosPalette.current
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else palette.blue,
        modifier = Modifier
            .androidClickable(interaction, onClick)
            .background(
                if (selected) palette.blue else palette.blue.copy(alpha = 0.1f),
                com.zhiwei.math.ui.theme.IosShapes.Capsule,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

private fun Modifier.androidClickable(
    interaction: androidx.compose.foundation.interaction.MutableInteractionSource,
    onClick: () -> Unit,
): Modifier = this.clickable(
    interactionSource = interaction,
    indication = null,
    onClick = onClick,
)

/**
 * 液态玻璃实时预览卡：彩色光斑打底（内容层，被本卡自己的 GlassHost 录制），
 * 玻璃面板为兄弟 overlay —— 10 项参数全部实时反映。
 */
@Composable
private fun LiquidGlassPreview(glass: GlassSettings) {
    val palette = com.zhiwei.math.ui.theme.LocalIosPalette.current
    GlassHost(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(140.dp),
        content = {
            // 光斑场背景：滑杆的模糊/饱和度变化在这里一目了然
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(Color(0xFF101423))
                        val blobs = listOf(
                            Triple(0.22f, 0.30f, Color(0xFFC9A87C)),
                            Triple(0.78f, 0.62f, Color(0xFF4FA5A0)),
                            Triple(0.50f, 0.92f, Color(0xFF2F6F8A)),
                        )
                        blobs.forEach { (cx, cy, color) ->
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(color.copy(alpha = 0.85f), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(
                                        cx * size.width,
                                        cy * size.height,
                                    ),
                                    radius = size.width * 0.55f,
                                ),
                                radius = size.width * 0.55f,
                                center = androidx.compose.ui.geometry.Offset(cx * size.width, cy * size.height),
                            )
                        }
                    },
            )
        },
        overlay = {
            // 玻璃面板（兄弟节点）
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .liquidGlass(
                        shape = com.zhiwei.math.ui.theme.IosShapes.Control,
                        surfaceColor = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                        innerShadowRadius = 6.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "玻璃预览 · 移动滑杆看变化",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.label,
                )
            }
        },
    )
}
