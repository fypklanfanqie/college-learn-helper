package com.zhiwei.math.ui.chat

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.zhiwei.math.ui.components.IosActionText
import com.zhiwei.math.ui.components.IosAlertDialog
import com.zhiwei.math.ui.components.IosBottomSheet
import com.zhiwei.math.ui.components.IosFilledButton
import com.zhiwei.math.ui.components.IosPlainButton
import com.zhiwei.math.ui.components.IosTextField
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import com.zhiwei.math.util.MathTextSplitter
import com.zhiwei.math.util.TextSegment

/**
 * 消息气泡（iOS 观感）：
 * - 用户 = 蓝底白字右对齐（Uneven 20/6 连续圆角）；
 * - 老师 = 浅灰气泡左对齐（16dp 连续圆角）+ Markdown/LaTeX；
 * - 长按老师气泡（震动）→ 底部选择面板：圈选文字 → 追问 / 划重点 / 复制；
 * - 追问消息渲染为引用卡片（摘录 + 问题）；
 * - 例题卡片 iOS 化。
 */
@OptIn(ExperimentalFoundationApi::class)
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
    val palette = LocalIosPalette.current

    // 模式切换标记 → 居中小提示条（append-only 历史行）
    if (message.role == "user" && message.content.startsWith("【切换模式：")) {
        val modeName = message.content.removePrefix("【切换模式：").removeSuffix("】")
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "已切换到「$modeName」模式",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryLabel,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        return
    }

    // 追问消息 → 引用卡片（摘录 + 问题，蓝底白字气泡内引用块）
    if (message.role == "user" && message.content.startsWith("【追问：")) {
        val rest = message.content.removePrefix("【追问：")
        val closeIdx = rest.indexOf("】")
        val quoted = if (closeIdx >= 0) rest.take(closeIdx) else rest
        val question = if (closeIdx >= 0 && closeIdx + 1 < rest.length) {
            rest.substring(closeIdx + 1).trim()
        } else ""
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Column(
                Modifier
                    .widthIn(max = 300.dp)
                    .background(palette.blue, IosShapes.BubbleUser)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (quoted.isNotBlank()) {
                    // 引用块：白底半透明（iOS 引用样式）
                    Text(
                        quoted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.18f), IosShapes.Small)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
                if (question.isNotBlank()) {
                    Text(question, style = MaterialTheme.typography.bodyMedium, color = Color.White)
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
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
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
            // 气泡体
            Column(
                modifier = Modifier
                    .background(
                        if (isUser) palette.blue else palette.card,
                        if (isUser) IosShapes.BubbleUser else IosShapes.BubbleTeacher,
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
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
                    Text(
                        "📄 $name",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) Color.White.copy(alpha = 0.85f) else palette.secondaryLabel,
                    )
                }
                if (isUser) {
                    Text(message.content, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                } else {
                    AssistantContent(content = message.content)
                }
            }

            // 回答下方按钮组：删除｜追问｜详细解答｜出例题（蓝色小字，流式中置灰）
            if (!isUser && !isStreamingPlaceholder && message.content.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IosActionText("删除", enabled = actionsEnabled) { onDelete(message) }
                    IosActionText("追问", enabled = actionsEnabled) { onFollowUp("") }
                    IosActionText("详细解答", enabled = actionsEnabled) { onDetailedSolution() }
                    IosActionText("出例题", enabled = actionsEnabled) { onAskExample() }
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
 * 文字选择面板（IosBottomSheet + 只读可圈选文本 + 操作条）。
 */
@Composable
fun TextSelectionSheet(
    text: String,
    onDismiss: () -> Unit,
    onFollowUp: (selected: String) -> Unit,
    onHighlight: (selected: String) -> Unit,
) {
    val palette = LocalIosPalette.current
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

    IosBottomSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "长按文字选择一段（引用最多 600 字）",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.label,
            )
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                readOnly = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.label),
                cursorBrush = SolidColor(palette.blue),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IosPlainButton(
                    text = if (selectedText.isBlank()) "复制全文" else "复制所选",
                    onClick = { clipboard.setText(AnnotatedString(selectedText.ifBlank { text })) },
                )
                Spacer(Modifier.weight(1f))
                IosPlainButton(
                    text = "追问",
                    enabled = selectedText.isNotBlank(),
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFollowUp(selectedText.take(600))
                    },
                )
                IosPlainButton(
                    text = "划重点",
                    enabled = selectedText.isNotBlank(),
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHighlight(selectedText.take(1200))
                    },
                )
            }
        }
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

/** 例题卡片（题目/解答/考点 + 加入例题本） */
@Composable
fun ExampleCard(
    data: ExampleData,
    added: Boolean,
    onAdd: (ExampleData) -> Unit,
) {
    val palette = LocalIosPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .background(palette.card, IosShapes.Card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "例题",
            style = MaterialTheme.typography.labelMedium,
            color = palette.blue,
        )
        if (data.question.isNotEmpty()) {
            Text("题目：${data.question}", style = MaterialTheme.typography.bodyMedium, color = palette.label)
        }
        if (data.solution.isNotEmpty()) {
            Text("解答：${data.solution}", style = MaterialTheme.typography.bodyMedium, color = palette.label)
        }
        if (data.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                data.tags.split("[,，、]".toRegex())
                    .filter { it.isNotBlank() }
                    .take(4)
                    .forEach { tag ->
                        Text(
                            tag.trim(),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.blue,
                            modifier = Modifier
                                .background(palette.blue.copy(alpha = 0.1f), IosShapes.Capsule)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
            }
        }
        IosPlainButton(
            text = if (added) "已加入例题本 ✓" else "加入例题本",
            enabled = !added,
            onClick = { onAdd(data) },
        )
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
    IosAlertDialog(
        onDismiss = onDismiss,
        title = "追问",
        confirmText = "发送",
        onConfirm = { onConfirm(question) },
        extraContent = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (quotedText.isNotBlank()) {
                    Text(
                        "引用：" + quotedText.take(120) + if (quotedText.length > 120) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalIosPalette.current.secondaryLabel,
                        maxLines = 3,
                    )
                }
                IosTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = if (quotedText.isBlank()) "想继续问什么？" else "想针对引用的哪一点深入？",
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
    )
}

/** 单行文本对话框（重命名等），iOS 弹窗样式 */
@Composable
fun SimpleTextDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    IosAlertDialog(
        onDismiss = onDismiss,
        title = title,
        confirmText = "确定",
        onConfirm = { if (text.isNotBlank()) onConfirm(text.trim()) },
        extraContent = {
            IosTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
            )
        },
    )
}
