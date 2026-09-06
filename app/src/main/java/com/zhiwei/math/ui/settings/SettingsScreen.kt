package com.zhiwei.math.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.prefs.AppearanceSettings
import com.zhiwei.math.data.prefs.GlassSettings
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.GlassMode
import com.zhiwei.math.glass.GlassSurface
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 设置页（iOS 分组列表）：
 * 外观（日/夜/跟随系统、玻璃模式+参数滑杆、聊天背景、字体）、API 配置、功能入口、关于。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenApiSettings: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenExampleBook: () -> Unit,
    onOpenHighlights: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenTutorial: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val glass by viewModel.glass.collectAsStateWithLifecycle()
    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setChatBackground(uri.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                title = { Text("设置") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── 外观 ─────────────────────────────────────────────
            SectionTitle("外观")
            GroupCard {
                ChipRow("主题") {
                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (id, label) ->
                        FilterChip(
                            selected = appearance.theme == id,
                            onClick = { viewModel.setTheme(id) },
                            label = { Text(label) },
                        )
                    }
                }
                HorizontalDivider()
                ChipRow("字体") {
                    listOf("misans" to "MiSans", "system" to "系统字体").forEach { (id, label) ->
                        FilterChip(
                            selected = appearance.font == id,
                            onClick = { viewModel.setFont(id) },
                            label = { Text(label) },
                        )
                    }
                }
                HorizontalDivider()
                RowSetting("聊天背景") {
                    TextButton(onClick = {
                        backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("选择图片") }
                    if (appearance.chatBackgroundUri.isNotBlank()) {
                        TextButton(onClick = { viewModel.setChatBackground("") }) { Text("清除") }
                    }
                }
            }

            // ── 玻璃效果 ─────────────────────────────────────────
            SectionTitle("玻璃效果")
            GroupCard {
                ChipRow("模式") {
                    listOf(
                        GlassMode.FROSTED to "毛玻璃（默认）",
                        GlassMode.LIQUID to "液态玻璃",
                        GlassMode.PLAIN to "半透明",
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = GlassMode.fromId(glass.mode) == mode,
                            onClick = { viewModel.setGlassMode(mode) },
                            label = { Text(label) },
                        )
                    }
                }
                HorizontalDivider()
                LiquidGlassPreview(glass)
                if (GlassMode.fromId(glass.mode) == GlassMode.LIQUID) {
                    GlassSlider("折射高度", glass.refractionHeight.toFloat(), 0f..80f) { v ->
                        viewModel.updateGlass(glass.copy(refractionHeight = v.toInt()))
                    }
                    GlassSlider("折射量", glass.refractionAmount.toFloat(), 0f..96f) { v ->
                        viewModel.updateGlass(glass.copy(refractionAmount = v.toInt()))
                    }
                    GlassSlider("模糊半径", glass.blurRadius.toFloat(), 0f..40f) { v ->
                        viewModel.updateGlass(glass.copy(blurRadius = v.toInt()))
                    }
                    GlassSlider("不透明度", glass.opacity.toFloat(), 30f..100f) { v ->
                        viewModel.updateGlass(glass.copy(opacity = v.toInt()))
                    }
                }
                Text(
                    "液态玻璃需 Android 13+；低版本自动降级为毛玻璃 / 半透明",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            // ── API ─────────────────────────────────────────────
            SectionTitle("API")
            GroupCard {
                RowSetting("API 配置（模型商 / 密钥）") {
                    TextButton(onClick = onOpenApiSettings) { Text("进入") }
                }
            }

            // ── 功能 ─────────────────────────────────────────────
            SectionTitle("功能")
            GroupCard {
                RowSetting("练题") { TextButton(onClick = onOpenPractice) { Text("进入") } }
                HorizontalDivider()
                RowSetting("例题本") { TextButton(onClick = onOpenExampleBook) { Text("进入") } }
                HorizontalDivider()
                RowSetting("划重点") { TextButton(onClick = onOpenHighlights) { Text("进入") } }
                HorizontalDivider()
                RowSetting("学习报告") { TextButton(onClick = onOpenReport) { Text("进入") } }
                HorizontalDivider()
                RowSetting("使用教程") { TextButton(onClick = onOpenTutorial) { Text("进入") } }
            }

            // ── 关于 ─────────────────────────────────────────────
            SectionTitle("关于")
            GroupCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("知微数学 v0.1.0", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Font: MiSans © Xiaomi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Glass: AndroidLiquidGlass · glasense-ui · Haze",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 液态玻璃实时预览卡：彩色渐变打底，玻璃面板浮在上面 */
@Composable
private fun LiquidGlassPreview(glass: GlassSettings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
            .height(120.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF7B93FF),
                            Color(0xFF67C6B0),
                            Color(0xFFF2A65A),
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1200f, 400f),
                    )
                ),
        ) {
            // 玻璃面板（本卡片被 appNavHost 的内容层 backdrop/haze 捕获）
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(64.dp),
                cornerRadius = 24.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "玻璃预览 · 移动滑杆看变化",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp),
    )
}

/** iOS 风格分组卡片 */
@Composable
private fun GroupCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(18.dp),
            )
            .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)),
    ) { content() }
}

@Composable
private fun ChipRow(label: String, content: @Composable RowScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) { content() }
    }
}

@Composable
private fun RowSetting(label: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun GlassSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${value.toInt()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
