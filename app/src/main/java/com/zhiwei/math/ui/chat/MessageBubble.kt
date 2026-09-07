package com.zhiwei.math.ui.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.zhiwei.math.data.db.MessageEntity
import com.zhiwei.math.glass.LocalHapticsEnabled
import com.zhiwei.math.prompt.ExampleData
import com.zhiwei.math.prompt.ExampleParser
import com.zhiwei.math.util.MathTextSplitter
import com.zhiwei.math.util.TextSegment

/**
 * 消息气泡：用户（右侧）/ 老师（左侧，Markdown+LaTeX）/ 模式切换系统提示 / 例题卡片。
 * 长按老师气泡（带震动反馈）→ 底部选择面板：圈选一段文字后可 追问 / 划重点 / 复制（只对选中文字生效）。
 * 用户追问消息渲染为豆包式引用卡片（摘录 + 问题），不再展示全文。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isStreamingPlaceholder: Boolean = false,
    actionsEnabled: Boolean = true,
    onFollowUp: (quotedText: String) -> Unit,
    onDetailedSolution: () -> Unit,
    onHighlight: (source: String) -> Unit,
    onAskExample: () -> Unit,
    onDelete: (MessageEntity) -> Unit,
    onAddExample: (ExampleData) -> Unit,
    addedExample: Boolean,
) {
    // 模式切换标记 → 居中小提示条（append-only 历史行）
    if (message.role == "user" && message.content.startsWith("【切换模式：")) {
        val modeName = message.content.removePrefix("【切换模式：").removeSuffix("】")
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "已切换到「$modeName」模式",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        return
    }

    // 追问消息 → 豆包式引用卡片（摘录 + 问题），不展示全文
    if (message.role == "user" && message.content.startsWith("【追问：")) {
        val rest = message.content.removePrefix("【追问：")
        val closeIdx = rest.indexOf("】")
        val quoted = if (closeIdx >= 0) rest.take(closeIdx) else rest
        val question = if (closeIdx >= 0 && closeIdx + 1 < rest.length) rest.substring(closeIdx + 1).trim() else ""
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.widthIn(max = 310.dp),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        quoted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (question.isNotBlank()) {
                        Text(question, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        return
    }

    val isUser = message.role == "user"
    val hapticsEnabled = LocalHapticsEnabled.current
    val haptic = LocalHapticFeedback.current
    var showTextPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (!isUser && !isStreamingPlaceholder) {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                showTextPicker = true
                            }
                        },
                    ),
            ) {
                BubbleCard(isUser = isUser) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.imageBase64?.let { b64 ->
                            val bmp = remember(b64) {
                                val bytes = Base64.decode(b64, Base64.NO_WRAP)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                            bmp?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "题目图片",
                                    modifier = Modifier
                                        .heightIn(max = 220.dp)
                                        .fillMaxWidth(),
                                )
                            }
                        }
                        message.attachmentName?.let { name ->
                            AssistChip(onClick = {}, label = { Text("📄 $name") })
                        }
                        if (isUser) {
                            Text(message.content, style = MaterialTheme.typography.bodyMedium)
                        } else {
                            AssistantContent(content = message.content)
                        }
                    }
                }

                // 回答下方按钮组：删除｜追问｜详细解答｜出例题（划重点改为长按圈选，不再整段）
                // 流式进行中禁用（此前静默 return 导致"点了没反应"，现在按钮明确置灰）
                if (!isUser && !isStreamingPlaceholder && message.content.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionText("删除", enabled = actionsEnabled) { onDelete(message) }
                        ActionText("追问", enabled = actionsEnabled) { onFollowUp("") }
                        ActionText("详细解答", enabled = actionsEnabled) { onDetailedSolution() }
                        ActionText("出例题", enabled = actionsEnabled) { onAskExample() }
                    }
                }
            }
        }
    }

    // 选择面板：圈选一段文字 → 追问 / 划重点 / 复制（只对选中文字生效）
    if (showTextPicker) {
        TextSelectionSheet(
            text = message.content,
            onDismiss = { showTextPicker = false },
            onFollowUp = { selected ->
                showTextPicker = false
                onFollowUp(selected)
            },
            onHighlight = { selected ->
                showTextPicker = false
                onHighlight(selected)
            },
        )
    }
}

/**
 * 文字选择面板：只读可圈选文本（原生长按手势在按压位置起选，可拖动调整句柄），
 * 下方操作条只作用于选中的文字。参照豆包"引用"交互。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSelectionSheet(
    text: String,
    onDismiss: () -> Unit,
    onFollowUp: (selected: String) -> Unit,
    onHighlight: (selected: String) -> Unit,
) {
    var value by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val selectedText = remember(value.selection, value.text) {
        if (value.selection.collapsed) "" else {
            val s = value.selection.min.coerceIn(0, value.text.length)
            val e = value.selection.max.coerceIn(0, value.text.length)
            value.text.substring(s, e)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("长按文字选择一段（引用最多 600 字）", style = MaterialTheme.typography.titleSmall)
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(selectedText.ifBlank { text }))
                    },
                    enabled = true,
                ) { Text(if (selectedText.isBlank()) "复制全文" else "复制所选") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFollowUp(selectedText.take(600))
                    },
                    enabled = selectedText.isNotBlank(),
                ) { Text("追问") }
                Button(
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHighlight(selectedText.take(1200))
                    },
                    enabled = selectedText.isNotBlank(),
                ) { Text("划重点") }
            }
        }
    }
}

@Composable
private fun BubbleCard(isUser: Boolean, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isUser)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        content()
    }
}

/** 老师 Markdown + 公式块内容（流式安全：未闭合 $$ 按普通文本显示） */
@Composable
fun AssistantContent(content: String) {
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MathTextSplitter.split(content).forEach { segment ->
                when (segment) {
                    is TextSegment.Text -> Markdown(content = segment.content)
                    is TextSegment.DisplayMath -> MathBlock(latex = segment.latex)
                    is TextSegment.ExampleFence -> Unit // 例题卡在气泡外层渲染
                }
            }
        }
    }
}

@Composable
fun ActionText(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** 例题卡片（题目/解答/考点 + 加入例题本） */
@Composable
fun ExampleCard(
    data: ExampleData,
    added: Boolean,
    onAdd: (ExampleData) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("例题", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (data.question.isNotEmpty()) {
                Text("题目：${data.question}", style = MaterialTheme.typography.bodyMedium)
            }
            if (data.solution.isNotEmpty()) {
                Text("解答：${data.solution}", style = MaterialTheme.typography.bodyMedium)
            }
            if (data.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    data.tags.split("[,，、]".toRegex())
                        .filter { it.isNotBlank() }
                        .take(4)
                        .forEach { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag.trim(), style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                }
            }
            TextButton(onClick = { onAdd(data) }, enabled = !added) {
                Text(if (added) "已加入例题本 ✓" else "加入例题本")
            }
        }
    }
}

/** 追问对话框：有引用时展示摘录，无引用时直接提问 */
@Composable
fun FollowUpDialog(
    quotedText: String,
    onDismiss: () -> Unit,
    onConfirm: (question: String) -> Unit,
) {
    var question by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("追问") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (quotedText.isNotBlank()) {
                    Text(
                        "引用：" + quotedText.take(120) + if (quotedText.length > 120) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                    )
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text(if (quotedText.isBlank()) "想继续问什么？" else "想针对引用的哪一点深入？") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(question) }) { Text("发送") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
