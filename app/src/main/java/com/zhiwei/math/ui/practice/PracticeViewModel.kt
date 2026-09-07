package com.zhiwei.math.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.db.PracticeEntity
import com.zhiwei.math.data.prefs.ApiKeyStore
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.llm.LlmRepository
import com.zhiwei.math.llm.core.LlmEvent
import com.zhiwei.math.llm.core.LlmMessage
import com.zhiwei.math.llm.core.LlmRequest
import com.zhiwei.math.llm.core.SystemBlock
import com.zhiwei.math.prompt.PracticePrompts
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 练题页：主题+难度 → 出题 → 作答（文本/拍照 OCR）→ 批改（流式）→ 下一题；历史可回看。
 */
class PracticeViewModel(
    private val repo: ChatRepository,
    private val llm: LlmRepository,
    private val settings: SettingsStore,
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {

    val stage = MutableStateFlow(Stage.SETUP)
    val topic = MutableStateFlow("")
    val difficulty = MutableStateFlow("基础")
    val question = MutableStateFlow("")
    val tags = MutableStateFlow("")
    val answer = MutableStateFlow("")
    val reviewText = MutableStateFlow<String?>(null)
    val streaming = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val practiceHistory: MutableStateFlow<List<PracticeEntity>> = MutableStateFlow(emptyList())

    private var job: Job? = null
    private var currentId: Long = 0

    enum class Stage { SETUP, ANSWERING, REVIEWING }

    val historyDetail = MutableStateFlow<PracticeEntity?>(null)

    fun showHistoryDetail(entity: PracticeEntity) {
        historyDetail.value = entity
    }

    fun refreshHistory() {
        viewModelScope.launch { practiceHistory.value = repo.practiceHistory() }
    }

    fun setTopic(v: String) { topic.value = v }
    fun setDifficulty(v: String) { difficulty.value = v }
    fun setAnswer(v: String) { answer.value = v }

    /** 回到出题设置（嵌入式 AI练 页签中"完成"按钮使用） */
    fun reset() {
        stage.value = Stage.SETUP
        answer.value = ""
        reviewText.value = null
    }

    fun cancel() {
        job?.cancel()
        streaming.value = false
    }

    /** 生成题目 */
    fun generateQuestion() {
        if (streaming.value) return
        error.value = null
        streaming.value = true
        question.value = ""
        tags.value = ""
        job = viewModelScope.launch {
            runCatching {
                stream(
                    system = PracticePrompts.QUIZ_SYSTEM,
                    user = "主题：${topic.value.trim()}\n难度：${difficulty.value}",
                    onDelta = { question.value += it },
                )
            }.onFailure { error.value = it.message ?: "出题失败" }
            streaming.value = false
        }
    }

    /** 出题完成 → 进入作答阶段 */
    fun confirmQuestion() {
        if (question.value.isBlank()) return
        viewModelScope.launch {
            currentId = repo.insertPractice(
                PracticeEntity(
                    topic = topic.value.trim(),
                    difficulty = difficulty.value,
                    question = question.value.trim(),
                    createdAt = System.currentTimeMillis(),
                )
            )
            answer.value = ""
            reviewText.value = null
            stage.value = Stage.ANSWERING
        }
    }

    /** 提交作答 → 批改（流式） */
    fun submitAnswer(answerImageBase64: String? = null) {
        if (streaming.value || question.value.isBlank()) return
        streaming.value = true
        reviewText.value = ""
        job = viewModelScope.launch {
            runCatching {
                val ans = answer.value.trim()
                val userMsg = buildString {
                    appendLine("【题目】")
                    appendLine(question.value)
                    appendLine("【学生作答】")
                    append(ans.ifBlank { "（未作答）" })
                }
                stream(
                    system = PracticePrompts.REVIEW_SYSTEM,
                    user = userMsg,
                    imageBase64 = answerImageBase64,
                    onDelta = { reviewText.value = (reviewText.value ?: "") + it },
                )
            }.onSuccess {
                repo.updatePractice(
                    currentId,
                    answer = ans0(),
                    answerImage = answerImageBase64,
                    review = reviewText.value,
                )
                stage.value = Stage.REVIEWING
            }.onFailure { error.value = it.message ?: "批改失败" }
            streaming.value = false
        }
    }

    private fun ans0(): String = answer.value.trim()

    /** 下一题（回到出题设置，保留主题） */
    fun nextQuestion() {
        stage.value = Stage.SETUP
        question.value = ""
        tags.value = ""
        answer.value = ""
        reviewText.value = null
    }

    private suspend fun stream(
        system: String,
        user: String,
        imageBase64: String? = null,
        onDelta: (String) -> Unit,
    ) {
        val (config, apiKey) = llm.currentConfig()
        val request = LlmRequest(
            baseUrl = config.baseUrl,
            apiKey = apiKey,
            model = config.model,
            systemBlocks = listOf(SystemBlock(text = system, stable = true)),
            messages = listOf(LlmMessage(LlmMessage.Role.USER, user, imageBase64 = imageBase64, imageMime = imageBase64?.let { "image/jpeg" })),
            temperature = 0.5,
        )
        llm.stream(config, apiKey, request).collect { ev ->
            when (ev) {
                is LlmEvent.Delta -> onDelta(ev.text)
                is LlmEvent.Error -> throw IllegalStateException(ev.message, ev.cause)
                else -> Unit
            }
        }
    }
}
