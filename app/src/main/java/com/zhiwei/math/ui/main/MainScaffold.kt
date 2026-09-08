package com.zhiwei.math.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhiwei.math.glass.AppMotion
import com.zhiwei.math.glass.GlassHost
import com.zhiwei.math.ui.components.DockItem
import com.zhiwei.math.ui.components.IosLargeTitleHeader
import com.zhiwei.math.ui.components.LiquidDock
import com.zhiwei.math.ui.components.SegmentedControl
import com.zhiwei.math.ui.icons.SfBookmark
import com.zhiwei.math.ui.icons.SfChartBar
import com.zhiwei.math.ui.icons.SfGearshape
import com.zhiwei.math.ui.icons.SfHouse
import com.zhiwei.math.ui.icons.SfPencilTip
import com.zhiwei.math.ui.misc.ExampleBookScreen
import com.zhiwei.math.ui.misc.HighlightsScreen
import com.zhiwei.math.ui.practice.PracticeScreen
import com.zhiwei.math.ui.report.ReportScreen
import com.zhiwei.math.ui.settings.SettingsScreen
import com.zhiwei.math.ui.subject.SubjectScreen

/**
 * 主界面（GlassHost 兄弟布局铁律）：
 * - 内容层 = 五个页签（被 layerBackdrop/hazeSource 录制）；
 * - overlay = 大标题头（IosLargeTitleHeader，玻璃底衬）+ LiquidDock，
 *   全部玻璃与内容互为兄弟节点（绝不在录制树内部放玻璃）。
 * - 页签切换 Crossfade + 微 slide；header 折叠态由当前页签滚动回调驱动。
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
    var headerCollapsed by remember { mutableStateOf(false) }

    val dockItems = remember {
        listOf(
            DockItem("主页", SfHouse),
            DockItem("AI练", SfPencilTip),
            DockItem("学习重点", SfBookmark),
            DockItem("学习报告", SfChartBar),
            DockItem("设置", SfGearshape),
        )
    }
    val tabTitles = remember {
        listOf("知微数学", "AI练", "学习重点", "学习报告", "设置")
    }

    // 页签切换时重置折叠态（新页签从顶部开始）
    LaunchedEffect(tab) { headerCollapsed = false }

    GlassHost(
        modifier = Modifier.fillMaxSize(),
        content = {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(AppMotion.snappy()) + androidx.compose.animation.slideInVertically(
                        AppMotion.snappy(),
                    ) { it / 24 }) togetherWith fadeOut(AppMotion.snappy())
                },
                label = "tabContent",
            ) { t ->
                Box(Modifier.fillMaxSize()) {
                    when (t) {
                        TAB_HOME -> SubjectScreen(
                            onStartMath = onOpenChatList,
                            onOpenChat = onOpenChat,
                            onCollapsedChanged = { headerCollapsed = it },
                        )
                        TAB_PRACTICE -> PracticeHub(
                            innerTab = aiPracticeTab,
                            onInnerTabChange = { aiPracticeTab = it },
                            onOpenChat = onOpenChat,
                        )
                        TAB_HIGHLIGHTS -> HighlightsScreen(
                            onBack = {},
                            onOpenChat = onOpenChat,
                            embedded = true,
                            onCollapsedChanged = { headerCollapsed = it },
                        )
                        TAB_REPORT -> ReportScreen(
                            onBack = {},
                            embedded = true,
                            onCollapsedChanged = { headerCollapsed = it },
                        )
                        else -> SettingsScreen(
                            onBack = {},
                            onOpenApiSettings = onOpenApiSettings,
                            onOpenTutorial = onOpenTutorial,
                            embedded = true,
                            onCollapsedChanged = { headerCollapsed = it },
                        )
                    }
                }
            }
        },
        overlay = {
            // 大标题头：玻璃底衬只在折叠后浮现（overlay = 兄弟玻璃）
            IosLargeTitleHeader(
                title = tabTitles[tab],
                collapsed = headerCollapsed,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            // 液态玻璃 dock（内部滑块用 exportedBackdrop 玻璃上玻璃）
            LiquidDock(
                items = dockItems,
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        },
    )
}

/** AI练：练题 + 例题本 内嵌页签（SegmentedControl 替代 FilterChip） */
@Composable
private fun PracticeHub(
    innerTab: Int,
    onInnerTabChange: (Int) -> Unit,
    onOpenChat: (Long) -> Unit,
) {
    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        // 大标题由 overlay 头提供；此处只放内嵌分段控件（顶部留出 header 空间）
        SegmentedControl(
            options = listOf(0, 1),
            selected = innerTab,
            onSelect = onInnerTabChange,
            label = { if (it == 0) "练题" else "例题本" },
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 96.dp, start = 20.dp, end = 20.dp),
        )
        Box(Modifier.fillMaxSize()) {
            if (innerTab == 0) {
                PracticeScreen(onBack = {}, embedded = true)
            } else {
                ExampleBookScreen(onBack = {}, onOpenChat = onOpenChat, embedded = true)
            }
        }
    }
}

private const val TAB_HOME = 0
private const val TAB_PRACTICE = 1
private const val TAB_HIGHLIGHTS = 2
private const val TAB_REPORT = 3
private const val TAB_SETTINGS = 4
