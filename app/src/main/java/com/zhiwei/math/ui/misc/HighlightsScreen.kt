package com.zhiwei.math.ui.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.db.ExampleEntity
import com.zhiwei.math.data.db.HighlightEntity
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.ui.chat.AssistantContent
import com.zhiwei.math.ui.components.CardGroup
import com.zhiwei.math.ui.components.EmptyHint
import com.zhiwei.math.ui.components.IosPlainButton
import com.zhiwei.math.ui.components.SwipeToDelete
import com.zhiwei.math.ui.icons.SfBookmark
import com.zhiwei.math.ui.icons.SfStar
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── 划重点 ─────────────────────────

class HighlightsViewModel(private val repo: ChatRepository) : ViewModel() {
    val highlights: StateFlow<List<HighlightEntity>> =
        repo.observeHighlights().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(id: Long) = viewModelScope.launch { repo.deleteHighlight(id) }
}

/** 划重点笔记：iOS 时间线列表（SwipeToDelete 左滑删除） */
@Composable
fun HighlightsScreen(
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit = {},
    embedded: Boolean = false,
    onCollapsedChanged: (Boolean) -> Unit = {},
    viewModel: HighlightsViewModel = koinViewModel(),
) {
    val palette = LocalIosPalette.current
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 滚动驱动大标题折叠
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    LaunchedEffect(collapsed) { onCollapsedChanged(collapsed) }

    Box(Modifier.fillMaxSize().background(palette.background)) {
        if (highlights.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.statusBarsPadding().padding(top = 96.dp)) {
                    EmptyHint(
                        title = "还没有划过重点",
                        subtitle = "在聊天里长按老师的回答 → 划重点",
                        icon = SfBookmark,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 132.dp,
                ),
            ) {
                item { Spacer(Modifier.statusBarsPadding().height(96.dp)) }
                items(highlights, key = { it.id }) { h ->
                    SwipeToDelete(
                        onDelete = { viewModel.delete(h.id) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(palette.card, IosShapes.Card)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    SfStar,
                                    contentDescription = null,
                                    tint = palette.orange,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    "  ${dateFormat.format(Date(h.createdAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryLabel,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (h.sourceText.isNotBlank()) {
                                Text(
                                    "原文：" + h.sourceText.take(80) + if (h.sourceText.length > 80) "…" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.secondaryLabel,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            AssistantContent(h.points)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 例题本 ─────────────────────────

class ExampleBookViewModel(private val repo: ChatRepository) : ViewModel() {
    val examples: StateFlow<List<ExampleEntity>> =
        repo.observeExamples().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun delete(id: Long) = viewModelScope.launch { repo.deleteExample(id) }
}

/** 例题本：iOS 卡片流（题目/解答/考点/来源对话；SwipeToDelete + 跳回原对话） */
@Composable
fun ExampleBookScreen(
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
    embedded: Boolean = false,
    viewModel: ExampleBookViewModel = koinViewModel(),
) {
    val palette = LocalIosPalette.current
    val examples by viewModel.examples.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Box(Modifier.fillMaxSize().background(palette.background)) {
        if (examples.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.statusBarsPadding().padding(top = 96.dp)) {
                    EmptyHint(
                        title = "例题本还是空的",
                        subtitle = "聊天里点「出例题」，例题卡片下可加入例题本",
                        icon = SfBookmark,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 132.dp,
                ),
            ) {
                item { Spacer(Modifier.statusBarsPadding().height(110.dp)) }
                items(examples, key = { it.id }) { e ->
                    SwipeToDelete(
                        onDelete = { viewModel.delete(e.id) },
                        modifier = Modifier.padding(vertical = 6.dp),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(palette.card, IosShapes.Card)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    dateFormat.format(Date(e.createdAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryLabel,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                "题目",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondaryLabel,
                            )
                            AssistantContent(e.question)
                            if (e.solution.isNotBlank()) {
                                Text(
                                    "解答",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.secondaryLabel,
                                )
                                AssistantContent(e.solution)
                            }
                            if (e.tags.isNotBlank()) {
                                Text(
                                    "考点：${e.tags}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            e.conversationId?.let { cid ->
                                IosPlainButton(text = "跳回原对话", onClick = { onOpenChat(cid) }, compact = true)
                            }
                        }
                    }
                }
            }
        }
    }
}
