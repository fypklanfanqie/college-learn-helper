package com.zhiwei.math.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhiwei.math.ui.chat.AssistantContent
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 练题页：主题+难度 → 出题 → 作答（文本）→ 批改（流式）→ 下一题；历史记录可回看。
 */
@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    embedded: Boolean = false,
    viewModel: PracticeViewModel = koinViewModel(),
) {
    val stage by viewModel.stage.collectAsState()
    val topic by viewModel.topic.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val question by viewModel.question.collectAsState()
    val answer by viewModel.answer.collectAsState()
    val review by viewModel.reviewText.collectAsState()
    val streaming by viewModel.streaming.collectAsState()
    val error by viewModel.error.collectAsState()
    val history by viewModel.practiceHistory.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val dateFormat = remember2()

    LaunchedEffect(Unit) { viewModel.refreshHistory() }

    Scaffold(
        topBar = {
            // 嵌入 AI练 页签时由外层提供标题与页签，隐藏自带顶栏
            if (!embedded) {
                TopAppBar(
                    navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                    title = { Text("练题") },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (stage) {
                PracticeViewModel.Stage.SETUP -> {
                    OutlinedTextField(
                        value = topic,
                        onValueChange = viewModel::setTopic,
                        label = { Text("想练什么主题？") },
                        placeholder = { Text("如：第二类换元、格林公式、二阶常系数微分方程…") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("基础", "进阶", "挑战").forEach { d ->
                            FilterChip(
                                selected = difficulty == d,
                                onClick = { viewModel.setDifficulty(d) },
                                label = { Text(d) },
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::generateQuestion,
                        enabled = topic.isNotBlank() && !streaming,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (streaming) "出题中…" else "出题") }
                    if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)

                    if (history.isNotEmpty()) {
                        Text("练习记录", style = MaterialTheme.typography.titleSmall)
                        history.take(10).forEach { p ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(p.topic, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${dateFormat.format(Date(p.createdAt))} · ${p.difficulty}",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                    if (p.review != null) {
                                        TextButton(onClick = {
                                            viewModel.showHistoryDetail(p)
                                        }) { Text("回看批改") }
                                    }
                                }
                            }
                        }
                    }
                }
                PracticeViewModel.Stage.ANSWERING -> {
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("题目", fontWeight = FontWeight.SemiBold)
                            AssistantContent(question)
                            if (tags.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    tags.split("[,，、]".toRegex()).take(3).forEach {
                                        AssistChip(onClick = {}, label = { Text(it.trim()) })
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = answer,
                        onValueChange = viewModel::setAnswer,
                        label = { Text("你的作答（可留空，AI 会先给详细解析）") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                    Button(
                        onClick = { viewModel.submitAnswer() },
                        enabled = !streaming,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (streaming) "批改中…" else "提交作答") }
                    TextButton(onClick = viewModel::cancel) { Text("取消") }
                }
                PracticeViewModel.Stage.REVIEWING -> {
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text("批改与解析", fontWeight = FontWeight.SemiBold)
                            review?.let { AssistantContent(it) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::nextQuestion) { Text("下一题") }
                        TextButton(onClick = {
                            if (embedded) viewModel.reset() else onBack()
                        }) { Text("完成") }
                    }
                }
            }
        }
    }

    // 历史回看（简化：切到 REVIEWING 展示）
    val showingHistory = viewModel.historyDetail.collectAsState()
    showingHistory.value?.let { p ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.historyDetail.value = null },
            title = { Text("练习回看 · ${p.topic}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("题目：${p.question}", style = MaterialTheme.typography.bodySmall)
                    p.answer?.let { Text("作答：$it", style = MaterialTheme.typography.bodySmall) }
                    p.review?.let { AssistantContent(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.historyDetail.value = null }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun remember2() = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
