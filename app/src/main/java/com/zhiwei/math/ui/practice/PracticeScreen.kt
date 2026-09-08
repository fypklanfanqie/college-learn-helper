package com.zhiwei.math.ui.practice

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhiwei.math.ui.chat.AssistantContent
import com.zhiwei.math.ui.components.CardGroup
import com.zhiwei.math.ui.components.IosAlertDialog
import com.zhiwei.math.ui.components.IosFilledButton
import com.zhiwei.math.ui.components.IosPlainButton
import com.zhiwei.math.ui.components.IosTextField
import com.zhiwei.math.ui.components.SectionHeader
import com.zhiwei.math.ui.components.SegmentedControl
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 练题页（iOS 观感）：主题输入 + SegmentedControl 难度 → 出题 →
 * 作答 → 批改（流式）→ 下一题；历史记录可回看（iOS 卡片）。
 */
@Composable
fun PracticeScreen(
    onBack: () -> Unit,
    embedded: Boolean = false,
    viewModel: PracticeViewModel = koinViewModel(),
) {
    val palette = LocalIosPalette.current
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
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) { viewModel.refreshHistory() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (stage) {
            PracticeViewModel.Stage.SETUP -> {
                IosTextField(
                    value = topic,
                    onValueChange = viewModel::setTopic,
                    label = "想练什么主题？",
                    placeholder = "如：第二类换元、格林公式、二阶常系数微分方程…",
                )
                SegmentedControl(
                    options = listOf("基础", "进阶", "挑战"),
                    selected = difficulty,
                    onSelect = viewModel::setDifficulty,
                    label = { it },
                )
                IosFilledButton(
                    text = if (streaming) "出题中…" else "出题",
                    onClick = viewModel::generateQuestion,
                    enabled = topic.isNotBlank() && !streaming,
                    loading = streaming,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) Text(error!!, color = palette.red, style = MaterialTheme.typography.bodySmall)

                if (history.isNotEmpty()) {
                    SectionHeader("练习记录")
                    CardGroup {
                        history.take(10).forEachIndexed { index, p ->
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        p.topic,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.label,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        "${dateFormat.format(Date(p.createdAt))} · ${p.difficulty}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondaryLabel,
                                    )
                                }
                                if (p.review != null) {
                                    IosPlainButton(text = "回看批改", onClick = {
                                        viewModel.showHistoryDetail(p)
                                    }, compact = true)
                                }
                            }
                            if (index < history.take(10).size - 1) {
                                Spacer(
                                    Modifier
                                        .padding(start = 16.dp)
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(palette.separator),
                                )
                            }
                        }
                    }
                }
            }
            PracticeViewModel.Stage.ANSWERING -> {
                CardGroup {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "题目",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.blue,
                        )
                        AssistantContent(question)
                        if (tags.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tags.split("[,，、]".toRegex()).take(3).forEach {
                                    Text(
                                        it.trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.blue,
                                        modifier = Modifier
                                            .background(palette.blue.copy(alpha = 0.1f), IosShapes.Capsule)
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                IosTextField(
                    value = answer,
                    onValueChange = viewModel::setAnswer,
                    label = "你的作答（可留空，AI 会先给详细解析）",
                    minLines = 4,
                    maxLines = 8,
                )
                IosFilledButton(
                    text = if (streaming) "批改中…" else "提交作答",
                    onClick = viewModel::submitAnswer,
                    enabled = !streaming,
                    loading = streaming,
                    modifier = Modifier.fillMaxWidth(),
                )
                IosPlainButton(text = "取消", onClick = viewModel::cancel)
            }
            PracticeViewModel.Stage.REVIEWING -> {
                CardGroup {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "批改与解析",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.blue,
                        )
                        Spacer(Modifier.height(8.dp))
                        review?.let { AssistantContent(it) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IosFilledButton(
                        text = "下一题",
                        onClick = viewModel::nextQuestion,
                        modifier = Modifier.weight(1f),
                    )
                    IosFilledButton(
                        text = "完成",
                        onClick = { if (embedded) viewModel.reset() else onBack() },
                        containerColor = palette.card,
                        textColor = palette.blue,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    // 历史回看（iOS 弹窗）
    val showingHistory = viewModel.historyDetail.collectAsState()
    showingHistory.value?.let { p ->
        IosAlertDialog(
            onDismiss = { viewModel.historyDetail.value = null },
            title = "练习回看 · ${p.topic}",
            message = "题目：${p.question.take(300)}\n\n作答：${p.answer?.take(200) ?: "（未作答）"}",
            confirmText = "关闭",
            onConfirm = { viewModel.historyDetail.value = null },
        )
    }
}
