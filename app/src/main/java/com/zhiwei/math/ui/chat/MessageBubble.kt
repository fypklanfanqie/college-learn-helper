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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.zhiwei.math.data.db.MessageEntity
import com.zhiwei.math.prompt.ExampleData
import com.zhiwei.math.prompt.ExampleParser
import com.zhiwei.math.util.MathTextSplitter
import com.zhiwei.math.util.TextSegment

/**
 * 消息气泡：用户（右侧）/ 老师（左侧，Markdown+LaTeX）/ 模式切换系统提示 / 例题卡片。
 * 长按老师气泡弹出菜单：复制全文 / 追问 / 划重点。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isStreamingPlaceholder: Boolean = false,
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

    val isUser = message.role == "user"
    val menuOpen = remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

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
                        onLongClick = { if (!isUser) menuOpen.value = true },
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

                // 回答下方按钮组：删除｜追问｜详细解答｜划重点｜出例题
                if (!isUser && !isStreamingPlaceholder && message.content.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionText("删除") { onDelete(message) }
                        ActionText("追问") { onFollowUp(message.content) }
                        ActionText("详细解答") { onDetailedSolution() }
                        ActionText("划重点") { onHighlight(message.content) }
                        ActionText("出例题") { onAskExample() }
                    }
                }
            }

            DropdownMenu(expanded = menuOpen.value, onDismissRequest = { menuOpen.value = false }) {
                DropdownMenuItem(
                    text = { Text("复制全文") },
                    onClick = {
                        menuOpen.value = false
                        clipboard.setText(AnnotatedString(message.content))
                    },
                )
                DropdownMenuItem(
                    text = { Text("针对这段追问") },
                    onClick = {
                        menuOpen.value = false
                        onFollowUp(message.content)
                    },
                )
                DropdownMenuItem(
                    text = { Text("划重点") },
                    onClick = {
                        menuOpen.value = false
                        onHighlight(message.content)
                    },
                )
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
fun ActionText(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
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

/** 追问对话框：引用原文 + 补充问题 */
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
                Text(
                    quotedText.take(160) + if (quotedText.length > 160) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("想针对哪一点继续深入？") },
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
