package com.zhiwei.math.ui.report

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.db.ConversationEntity
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.llm.LlmRepository
import com.zhiwei.math.llm.core.LlmEvent
import com.zhiwei.math.llm.core.LlmMessage
import com.zhiwei.math.llm.core.LlmRequest
import com.zhiwei.math.llm.core.SystemBlock
import com.zhiwei.math.util.DocxWriter
import com.zhiwei.math.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val REPORT_SYSTEM = """你是「宋老师」的高数学习报告生成器。用户会提供一段师生对话记录，请根据对话内容生成学习报告。输出格式（Markdown）：
# 学习报告
## 一、本次学习内容
（概括涉及的知识点与题型）
## 二、掌握情况
（从问答质量推断：哪些掌握较好 / 哪些理解有偏差）
## 三、薄弱点与易错点
（列出暴露出的具体薄弱环节）
## 四、复习建议
（给出 3-5 条可执行的下一步建议，贴合期末考点）
全程简体中文；内容只依据对话，不要编造。"""

/**
 * 学习报告：选一个对话 → LLM 生成（流式预览）→ 导出图片 / Word。
 */
class ReportViewModel(
    private val repo: ChatRepository,
    private val llm: LlmRepository,
) : ViewModel() {

    val conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val selectedConversationId = MutableStateFlow<Long?>(null)
    val reportText = MutableStateFlow("")
    val generating = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch { conversations.value = repo.observeConversations().first() }
    }

    fun select(id: Long?) {
        selectedConversationId.value = id
    }

    fun generate() {
        val convoId = selectedConversationId.value ?: return
        if (generating.value) return
        generating.value = true
        error.value = null
        reportText.value = ""
        viewModelScope.launch {
            runCatching {
                val messages = repo.getMessagesOnce(convoId)
                val transcript = messages
                    .filter { it.role == "user" || it.role == "assistant" }
                    .joinToString("\n") { m ->
                        (if (m.role == "user") "学生：" else "宋老师：") + m.content.take(1500)
                    }
                    .take(24000)
                val (config, apiKey) = llm.currentConfig()
                val request = LlmRequest(
                    baseUrl = config.baseUrl,
                    apiKey = apiKey,
                    model = config.model,
                    systemBlocks = listOf(SystemBlock(text = REPORT_SYSTEM, stable = true)),
                    messages = listOf(
                        LlmMessage(LlmMessage.Role.USER, "【对话记录】\n$transcript\n\n请生成本次学习报告。")
                    ),
                    temperature = 0.4,
                    maxTokens = 4096,
                )
                llm.stream(config, apiKey, request).collect { ev ->
                    when (ev) {
                        is LlmEvent.Delta -> reportText.value += ev.text
                        is LlmEvent.Error -> throw IllegalStateException(ev.message, ev.cause)
                        else -> Unit
                    }
                }
            }.onFailure { error.value = it.message ?: "生成失败" }
            generating.value = false
        }
    }

    /** 导出 Word（MediaStore Downloads） */
    fun exportWord(context: Context, onResult: (String) -> Unit) {
        val text = reportText.value
        if (text.isBlank()) return
        viewModelScope.launch {
            val paragraphs = text.lines().map { line ->
                DocxWriter.Paragraph(line, heading = line.startsWith("#"))
            }
            val name = "知微数学-学习报告-${System.currentTimeMillis()}.docx"
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { out -> DocxWriter.write(paragraphs, out) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            onResult("已导出到 Downloads/$name")
        }
    }

    /** 导出图片：报告文本渲染成位图（MiSans 排版）→ MediaStore Downloads */
    fun exportImage(context: Context, onResult: (String) -> Unit) {
        val text = reportText.value
        if (text.isBlank()) return
        viewModelScope.launch {
            val name = "知微数学-学习报告-${System.currentTimeMillis()}.png"
            val bitmap = renderTextBitmap(context, text)
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                resolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            onResult("已保存长图 Downloads/$name")
        }
    }

    private fun renderTextBitmap(context: Context, text: String): Bitmap {
        val widthPx = 1080
        val paint = TextPaint().apply {
            color = Color.rgb(28, 28, 30)
            textSize = 40f
            isAntiAlias = true
        }
        val titlePaint = TextPaint(paint).apply {
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthPx - 120)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(12f, 1.2f)
            .build()
        val height = layout.height + 260
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawText("学习报告 · 知微数学", 60f, 100f, titlePaint)
        canvas.save()
        canvas.translate(60f, 160f)
        layout.draw(canvas)
        canvas.restore()
        return bitmap
    }
}

/** 学习报告屏：选对话 → 生成（流式预览）→ 导出图片/Word */
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = org.koin.androidx.compose.koinViewModel(),
) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val reportText by viewModel.reportText.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf<String?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }, title = { Text("学习报告") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                OutlinedButton(onClick = { pickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        conversations.firstOrNull { it.id == selectedId }?.title ?: "选择一个对话"
                    )
                }
                DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                    conversations.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.title) },
                            onClick = {
                                pickerOpen = false
                                viewModel.select(c.id)
                            },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::generate, enabled = selectedId != null && !generating) {
                    Text(if (generating) "生成中…" else "生成报告")
                }
                OutlinedButton(
                    onClick = { viewModel.exportImage(context) { notice = it } },
                    enabled = reportText.isNotBlank(),
                ) { Text("导出图片") }
                OutlinedButton(
                    onClick = { viewModel.exportWord(context) { notice = it } },
                    enabled = reportText.isNotBlank(),
                ) { Text("导出 Word") }
            }

            if (generating && reportText.isBlank()) {
                CircularProgressIndicator()
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (reportText.isNotBlank()) {
                Card {
                    Column(Modifier.padding(12.dp)) {
                        com.zhiwei.math.ui.chat.AssistantContent(reportText)
                    }
                }
            }
        }
    }
}
