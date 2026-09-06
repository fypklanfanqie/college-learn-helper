package com.zhiwei.math.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.db.ConversationEntity
import com.zhiwei.math.data.db.MessageEntity
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.llm.core.LlmEvent
import com.zhiwei.math.ocr.OcrManager
import com.zhiwei.math.prompt.ChatMode
import com.zhiwei.math.prompt.PromptAssembler
import com.zhiwei.math.util.ImageUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 聊天页 ViewModel。
 * 流式输出：Delta 事件直接累加进 [streamingText]；收集结束（完成/出错/打断）后
 * 仓库层负责落库，messages Flow 自动刷新，streamingText 清空。
 */
class ChatViewModel(
    private val repo: ChatRepository,
    private val ocrManager: OcrManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val convoId: Long = savedStateHandle.get<String>("convoId")?.toLongOrNull()
        ?: savedStateHandle.get<Long>("convoId")
        ?: -1L

    val conversation: StateFlow<ConversationEntity?> =
        repo.observeConversation(convoId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val messages: StateFlow<List<MessageEntity>> =
        repo.observeMessages(convoId).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 非 null = 正在流式输出的文本 */
    val streamingText = MutableStateFlow<String?>(null)

    /** 输入框草稿（换模式/切对话保留） */
    val draft = MutableStateFlow("")

    /** OCR / 文档解析提示卡（"OCR 暂时不可用" 等），null = 不显示 */
    val notice = MutableStateFlow<String?>(null)

    /** 待发送图片（已压缩 base64；发送前预览） */
    val pendingImage = MutableStateFlow<Pair<String, String>?>(null) // base64 to mime

    private var job: Job? = null

    val isStreaming: Boolean get() = streamingText.value != null

    fun updateDraft(text: String) {
        draft.value = text
    }

    fun dismissNotice() {
        notice.value = null
    }

    fun setPendingImage(base64: String, mime: String) {
        pendingImage.value = base64 to mime
    }

    fun clearPendingImage() {
        pendingImage.value = null
    }

    /** 直接发送（文本 / 文本+图片） */
    fun send(text: String = draft.value) {
        if (isStreaming) return
        val image = pendingImage.value
        draft.value = ""
        pendingImage.value = null
        startStream(
            userText = text.trim(),
            imageBase64 = image?.first,
            imageMime = image?.second,
        )
    }

    /** 追问（用户选中上次回答的文字后提问） */
    fun followUp(selectedText: String, question: String) {
        if (isStreaming) return
        startStream(
            userText = "",
            extra = listOf(PromptAssembler.followUp(selectedText, question)),
        )
    }

    /** 详细解答 */
    fun detailedSolution() {
        if (isStreaming) return
        startStream(userText = "", extra = listOf(PromptAssembler.detailedSolution))
    }

    /** 出例题 */
    fun askExample() {
        if (isStreaming) return
        startStream(userText = "", extra = listOf(PromptAssembler.askExample))
    }

    /** 划重点（完成后自动存入划重点本） */
    fun highlight(sourceText: String) {
        if (isStreaming) return
        startStream(
            userText = "",
            extra = listOf(PromptAssembler.highlight(sourceText)),
            sourceText = sourceText,
        )
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch { repo.deleteMessage(message.id) }
    }

    /** 文档上传：提取文本后作为消息发送 */
    fun sendDocument(fileName: String, text: String) {
        if (isStreaming) return
        startStream(
            userText = "【文档：$fileName】\n$text",
            attachmentName = fileName,
        )
    }

    fun sendDocumentNotice(message: String) {
        notice.value = message
    }

    fun switchMode(mode: ChatMode) {
        viewModelScope.launch { repo.switchMode(convoId, mode) }
    }

    fun rename(title: String) {
        viewModelScope.launch { repo.renameConversation(convoId, title) }
    }

    fun addExampleToBook(data: com.zhiwei.math.prompt.ExampleData) {
        viewModelScope.launch { repo.addExample(convoId, data) }
    }

    /** 图片进入兜底链：支持视觉→直传；不支持→OCR（提示效果不佳）；无文本→"OCR 暂时不可用" */
    fun onImagePicked(base64: String, mime: String, visionSupported: Boolean) {
        if (visionSupported) {
            setPendingImage(base64, mime)
            return
        }
        viewModelScope.launch {
            val bitmap = pendingBitmap
            val outcome = bitmap?.let { ocrManager.recognizeWithOutcome(it) } ?: OcrManager.OcrOutcome.Unavailable
            when (outcome) {
                is OcrManager.OcrOutcome.Success -> {
                    notice.value = "已用本地 OCR 识别图片文字（数学公式识别效果不佳，建议换用支持视觉的模型）"
                    draft.value = draft.value + (if (draft.value.isBlank()) "" else "\n") + outcome.text
                }
                OcrManager.OcrOutcome.NoText -> {
                    notice.value = "OCR 暂时不可用：未能从图片中识别出有效文字。若你的模型支持视觉输入，可直接发送图片。"
                }
                OcrManager.OcrOutcome.Unavailable -> {
                    notice.value = "OCR 暂时不可用：本地识别引擎初始化失败。建议在 API 设置中改用支持视觉输入的模型，直接发图。"
                }
            }
        }
    }

    /** 拍照/选图后的原始 Bitmap（OCR 用；转 base64 用后即弃） */
    var pendingBitmap: android.graphics.Bitmap? = null

    private fun startStream(
        userText: String,
        imageBase64: String? = null,
        imageMime: String? = null,
        attachmentName: String? = null,
        extra: List<com.zhiwei.math.llm.core.LlmMessage> = emptyList(),
        sourceText: String? = null,
    ) {
        val sb = StringBuilder()
        streamingText.value = ""
        job = viewModelScope.launch {
            repo.sendAndStream(
                convoId = convoId,
                userText = userText,
                imageBase64 = imageBase64,
                imageMime = imageMime,
                attachmentName = attachmentName,
                extraMessages = extra,
                sourceText = sourceText,
            ).collect { event ->
                when (event) {
                    is LlmEvent.Delta -> {
                        sb.append(event.text)
                        streamingText.value = sb.toString()
                    }
                    is LlmEvent.Error -> {
                        streamingText.value = null
                        notice.value = "出错了：${event.message}"
                    }
                    else -> Unit
                }
            }
            streamingText.value = null
        }
        job?.invokeOnCompletion {
            // 收集结束（正常/打断/异常）后清空流式状态；落库由仓库层 finally 负责
            streamingText.value = null
        }
    }

    /** 图片压缩辅助（供 UI 层调用） */
    fun imageToBase64(bitmap: android.graphics.Bitmap): String = ImageUtils.toJpegBase64(bitmap)
}
