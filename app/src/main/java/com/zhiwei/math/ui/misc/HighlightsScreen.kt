package com.zhiwei.math.ui.misc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.db.ExampleEntity
import com.zhiwei.math.data.db.HighlightEntity
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.ui.chat.AssistantContent
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

/** 划重点笔记：按对话分组时间线，可删除 */
@Composable
fun HighlightsScreen(
    onBack: () -> Unit,
    viewModel: HighlightsViewModel = koinViewModel(),
) {
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = { TopAppBar(navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }, title = { Text("划重点") }) },
    ) { padding ->
        if (highlights.isEmpty()) {
            EmptyHint("还没有划过重点。\n在聊天里长按老师的回答 → 划重点。", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(highlights, key = { it.id }) { h ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "⭐ ${dateFormat.format(Date(h.createdAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.delete(h.id) }) { Text("删除") }
                            }
                            if (h.sourceText.isNotBlank()) {
                                Text(
                                    "原文：" + h.sourceText.take(80) + if (h.sourceText.length > 80) "…" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** 例题本：卡片流（题干/解答/考点/来源对话），删除/跳回原对话 */
@Composable
fun ExampleBookScreen(
    onBack: () -> Unit,
    onOpenChat: (Long) -> Unit,
    viewModel: ExampleBookViewModel = koinViewModel(),
) {
    val examples by viewModel.examples.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        topBar = { TopAppBar(navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }, title = { Text("例题本") }) },
    ) { padding ->
        if (examples.isEmpty()) {
            EmptyHint("例题本还是空的。\n聊天里点「出例题」，例题卡片下可加入例题本。", Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(examples, key = { it.id }) { e ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "📐 ${dateFormat.format(Date(e.createdAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.delete(e.id) }) { Text("删除") }
                            }
                            Text("题目", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                            AssistantContent(e.question)
                            if (e.solution.isNotBlank()) {
                                Text("解答", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                                AssistantContent(e.solution)
                            }
                            if (e.tags.isNotBlank()) {
                                Text("考点：${e.tags}", style = MaterialTheme.typography.labelSmall)
                            }
                            e.conversationId?.let { cid ->
                                TextButton(onClick = { onOpenChat(cid) }) { Text("跳回原对话") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(32.dp),
        )
    }
}
