package com.zhiwei.math.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.zhiwei.math.glass.GlassSurface
import com.zhiwei.math.glass.LocalHapticsEnabled
import com.zhiwei.math.ui.misc.ExampleBookScreen
import com.zhiwei.math.ui.misc.HighlightsScreen
import com.zhiwei.math.ui.practice.PracticeScreen
import com.zhiwei.math.ui.report.ReportScreen
import com.zhiwei.math.ui.settings.SettingsScreen
import com.zhiwei.math.ui.subject.SubjectScreen

/**
 * 主界面：内容层 + 悬浮玻璃 dock（参照参考应用与 Cresto NavigationBar）。
 * 五个页签：主页 / AI练（练题+例题本）/ 学习重点 / 学习报告 / 设置。
 * 页签间切换不走路由（保留状态），详情页（对话/教程/API）从其上 push。
 */
@Composable
fun MainScaffold(
    onOpenChatList: () -> Unit,
    onOpenChat: (Long) -> Unit,
    onOpenApiSettings: () -> Unit,
    onOpenTutorial: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(TAB_HOME) }
    var aiPracticeTab by rememberSaveable { mutableIntStateOf(0) } // 0 练题 1 例题本

    Box(Modifier.fillMaxSize()) {
        // 内容层：底部留出 dock 覆盖高度，列表滚动到底不被遮挡
        Box(
            Modifier
                .fillMaxSize()
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 84.dp,
                ),
        ) {
            when (tab) {
                TAB_HOME -> SubjectScreen(onStartMath = onOpenChatList, embedded = true)
                TAB_PRACTICE -> PracticeHub(
                    innerTab = aiPracticeTab,
                    onInnerTabChange = { aiPracticeTab = it },
                    onOpenChat = onOpenChat,
                )
                TAB_HIGHLIGHTS -> HighlightsScreen(onBack = {}, embedded = true)
                TAB_REPORT -> ReportScreen(onBack = {}, embedded = true)
                else -> SettingsScreen(
                    onBack = {},
                    onOpenApiSettings = onOpenApiSettings,
                    onOpenTutorial = onOpenTutorial,
                    embedded = true,
                )
            }
        }

        GlassDock(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** AI练：练题 + 例题本 内嵌页签 */
@Composable
private fun PracticeHub(
    innerTab: Int,
    onInnerTabChange: (Int) -> Unit,
    onOpenChat: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AI练", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            FilterChip(
                selected = innerTab == 0,
                onClick = { onInnerTabChange(0) },
                label = { Text("练题") },
            )
            FilterChip(
                selected = innerTab == 1,
                onClick = { onInnerTabChange(1) },
                label = { Text("例题本") },
            )
        }
        Box(Modifier.fillMaxSize()) {
            if (innerTab == 0) {
                PracticeScreen(onBack = {}, embedded = true)
            } else {
                ExampleBookScreen(onBack = {}, onOpenChat = onOpenChat, embedded = true)
            }
        }
    }
}

// ───────────────────────── 玻璃 dock ─────────────────────────

private const val TAB_HOME = 0
private const val TAB_PRACTICE = 1
private const val TAB_HIGHLIGHTS = 2
private const val TAB_REPORT = 3
private const val TAB_SETTINGS = 4

private data class DockItem(
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
)

@Composable
fun GlassDock(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticsEnabled = LocalHapticsEnabled.current
    val haptic = LocalHapticFeedback.current
    val items = remember {
        listOf(
            DockItem("主页", Icons.Filled.Home, Icons.Outlined.Home),
            DockItem("AI练", Icons.Filled.EditNote, Icons.Outlined.EditNote),
            DockItem("学习重点", Icons.Filled.Bookmarks, Icons.Outlined.Bookmarks),
            DockItem("学习报告", Icons.Filled.Analytics, Icons.Outlined.Analytics),
            DockItem("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
        )
    }

    GlassSurface(
        cornerRadius = 31.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 8.dp)
            .height(62.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSelect(index)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) item.iconSelected else item.iconUnselected,
                        contentDescription = item.label,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
