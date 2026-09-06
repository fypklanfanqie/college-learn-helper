package com.zhiwei.math.data.repo

import com.zhiwei.math.data.db.ConversationDao
import com.zhiwei.math.data.db.ConversationEntity
import com.zhiwei.math.data.db.ExampleDao
import com.zhiwei.math.data.db.ExampleEntity
import com.zhiwei.math.data.db.HighlightDao
import com.zhiwei.math.data.db.HighlightEntity
import com.zhiwei.math.data.db.MessageDao
import com.zhiwei.math.data.db.MessageEntity
import com.zhiwei.math.data.db.PracticeDao
import com.zhiwei.math.data.db.PracticeEntity
import com.zhiwei.math.data.prefs.ApiKeyStore
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.llm.LlmRepository
import com.zhiwei.math.llm.core.LlmEvent
import com.zhiwei.math.llm.core.LlmMessage
import com.zhiwei.math.llm.core.LlmRequest
import com.zhiwei.math.prompt.ChatMode
import com.zhiwei.math.prompt.ExampleData
import com.zhiwei.math.prompt.PromptAssembler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * 对话/消息仓库：发送流程 = 插入用户消息 → 组装分层请求 → 流式收集 → 落库（含打断保留部分文本）。
 */
class ChatRepository(
    private val llm: LlmRepository,
    private val settings: SettingsStore,
    private val apiKeyStore: ApiKeyStore,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val highlightDao: HighlightDao,
    private val exampleDao: ExampleDao,
    private val practiceDao: PracticeDao,
) {
    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun observeConversation(id: Long): Flow<ConversationEntity?> = conversationDao.observeById(id)

    fun observeMessages(convoId: Long): Flow<List<MessageEntity>> = messageDao.observeByConversation(convoId)

    suspend fun getMessagesOnce(convoId: Long): List<MessageEntity> = messageDao.getByConversation(convoId)

    suspend fun createConversation(title: String = "新的对话"): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(title = title, createdAt = now, updatedAt = now)
        )
    }

    suspend fun renameConversation(id: Long, title: String) {
        conversationDao.rename(id, title, System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: Long) {
        messageDao.deleteByConversation(id)
        conversationDao.getById(id)?.let { conversationDao.delete(it) }
    }

    suspend fun getConversation(id: Long): ConversationEntity? = conversationDao.getById(id)

    /** 对话设置保存（Layer 2 字节级入库） */
    suspend fun saveConvoSettings(
        convoId: Long,
        layer2: String,
        examFocus: String,
        nonExam: String,
        persona: String,
        level: String,
    ) {
        conversationDao.updateConvoSettings(
            id = convoId,
            layer2 = layer2,
            examFocus = examFocus,
            nonExam = nonExam,
            persona = persona,
            level = level,
            now = System.currentTimeMillis(),
        )
    }

    suspend fun deleteMessage(id: Long) {
        messageDao.deleteById(id)
    }

    fun observeHighlights() = highlightDao.observeAll()

    suspend fun deleteHighlight(id: Long) = highlightDao.deleteById(id)

    fun observeExamples() = exampleDao.observeAll()

    suspend fun deleteExample(id: Long) = exampleDao.deleteById(id)

    // 练题
    suspend fun insertPractice(entity: PracticeEntity): Long = practiceDao.insert(entity)

    suspend fun updatePractice(id: Long, answer: String?, answerImage: String?, review: String?) {
        practiceDao.getById(id)?.let {
            practiceDao.update(
                it.copy(answer = answer, answerImageBase64 = answerImage, review = review)
            )
        }
    }

    suspend fun practiceHistory(): List<PracticeEntity> = practiceDao.getAll()

    suspend fun deletePractice(id: Long) = practiceDao.deleteById(id)

    /** 例题卡片「加入例题本」 */
    suspend fun addExample(convoId: Long, data: ExampleData) {
        exampleDao.insert(
            ExampleEntity(
                conversationId = convoId,
                question = data.question,
                solution = data.solution,
                tags = data.tags,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun switchMode(convoId: Long, mode: ChatMode) {
        val convo = conversationDao.getById(convoId) ?: return
        if (convo.mode == mode.name) return
        conversationDao.updateMode(convoId, mode.name, System.currentTimeMillis())
        // append-only：模式切换作为追加消息，绝不改写历史（缓存核心设计 6.3-3）
        messageDao.insert(
            MessageEntity(
                conversationId = convoId,
                role = "user",
                content = mode.marker,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    /**
     * 发送并流式接收回复。
     * @param extraMessages 追加的功能标记消息（追问/详细解答/出例题/划重点），随用户消息一起入库
     * @param sourceText 划重点时的原文（完成后保存到划重点本）
     */
    fun sendAndStream(
        convoId: Long,
        userText: String,
        imageBase64: String? = null,
        imageMime: String? = null,
        attachmentName: String? = null,
        extraMessages: List<LlmMessage> = emptyList(),
        sourceText: String? = null,
    ): Flow<LlmEvent> = flow {
        val now = System.currentTimeMillis()
        if (userText.isNotBlank() || imageBase64 != null || attachmentName != null) {
            messageDao.insert(
                MessageEntity(
                    conversationId = convoId,
                    role = "user",
                    content = userText,
                    imageBase64 = imageBase64,
                    imageMime = imageMime,
                    attachmentName = attachmentName,
                    createdAt = now,
                )
            )
        }
        for (extra in extraMessages) {
            messageDao.insert(
                MessageEntity(
                    conversationId = convoId,
                    role = "user",
                    content = extra.content,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }

        val convo = conversationDao.getById(convoId)!!
        val history = messageDao.getByConversation(convoId)
            .filter { it.role == "user" || it.role == "assistant" }
            .map {
                LlmMessage(
                    role = if (it.role == "user") LlmMessage.Role.USER else LlmMessage.Role.ASSISTANT,
                    content = it.content,
                    imageBase64 = it.imageBase64,
                    imageMime = it.imageMime,
                )
            }
        val (config, apiKey) = llm.currentConfig()
        val request = PromptAssembler.buildRequest(
            baseUrl = config.baseUrl,
            apiKey = apiKey,
            model = config.model,
            layer2Text = convo.layer2Prompt,
            history = history,
        )

        val replyBuilder = StringBuilder()
        var usage: LlmEvent.Usage? = null
        try {
            llm.stream(config, apiKey, request).collect { event ->
                when (event) {
                    is LlmEvent.Delta -> replyBuilder.append(event.text)
                    is LlmEvent.Usage -> usage = event
                    else -> Unit
                }
                emit(event)
            }
        } finally {
            // 用户打断（收集方取消）也要落库已生成部分
            withContext(NonCancellable) {
                persistReply(convoId, replyBuilder.toString(), usage, sourceText)
            }
        }
    }

    private suspend fun persistReply(
        convoId: Long,
        text: String,
        usage: LlmEvent.Usage?,
        sourceText: String?,
    ) = withContext(NonCancellable) {
        // 打断时也保留已生成部分（PROJECT-BRIEF.md 六：打断保留已生成部分）
        val finalText = text.trim()
        if (finalText.isNotEmpty()) {
            messageDao.insert(
                MessageEntity(
                    conversationId = convoId,
                    role = "assistant",
                    content = finalText,
                    usageInput = usage?.inputTokens,
                    usageOutput = usage?.outputTokens,
                    usageCached = usage?.cacheReadTokens,
                    createdAt = System.currentTimeMillis(),
                )
            )
            conversationDao.updateMode(convoId, conversationDao.getById(convoId)!!.mode, System.currentTimeMillis())
            if (sourceText != null) {
                highlightDao.insert(
                    HighlightEntity(
                        conversationId = convoId,
                        sourceText = sourceText,
                        points = finalText,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }
}

/** kotlinx.coroutines.flow.Flow 别名辅助（避免与 DB Flow 混淆的可读性扩展点） */
private fun Flow<LlmEvent>.kotlinxFlow(): Flow<LlmEvent> = this
