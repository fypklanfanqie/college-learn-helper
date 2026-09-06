package com.zhiwei.math.prompt

import com.zhiwei.math.llm.core.LlmMessage
import com.zhiwei.math.llm.core.LlmRequest
import com.zhiwei.math.llm.core.SystemBlock

/**
 * 提示词分层组装（PROJECT-BRIEF.md 7.3 / 6.3）：
 * - Layer 0：宋浩风格人设（App 级，字节稳定）
 * - Layer 1：高数考点大纲（科目级，字节稳定）
 * - Layer 2：元提示词生成的定制段（对话级，仅用户编辑设置时变化，字节级存库）
 *
 * 缓存规则：
 * - systemBlocks 顺序永不变化：stable 层在前（Anthropic 断点位置也固定）
 * - 模式切换 / 追问 / 详细解答 = 追加消息，绝不改写历史（append-only）
 * - 动态内容不进 system
 */
object PromptAssembler {

    /** Layer 0 + Layer 1 合成一个 stable 块（两份内容都永不变化） */
    private val baseBlock = SystemBlock(
        text = PromptLayer0.TEXT + "\n\n" + PromptLayer1.TEXT,
        stable = true,
    )

    /**
     * 组装一次对话请求。
     * @param layer2Text 该对话已生成的定制提示词段（Room 中字节级存储；空表示未设置）
     * @param mode 当前模式（精讲/期末冲刺），作为追加消息传给模型（不改 system）
     * @param pendingModeSwitch 非 null 表示本轮要先追加一条模式切换标记
     */
    fun buildRequest(
        baseUrl: String,
        apiKey: String,
        model: String,
        layer2Text: String,
        history: List<LlmMessage>,
        pendingModeSwitch: ChatMode? = null,
        maxTokens: Int = 8192,
    ): LlmRequest {
        val blocks = buildList {
            add(baseBlock)
            if (layer2Text.isNotBlank()) add(SystemBlock(text = layer2Text, stable = true))
        }
        val msgs = buildList {
            if (pendingModeSwitch != null) {
                add(LlmMessage(LlmMessage.Role.USER, pendingModeSwitch.marker))
            }
            addAll(history)
        }
        return LlmRequest(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
            systemBlocks = blocks,
            messages = msgs,
            maxTokens = maxTokens,
        )
    }

    /** 用户选中文字 → 追问标记（append-only 追加消息） */
    fun followUp(selectedText: String, question: String): LlmMessage =
        LlmMessage(
            LlmMessage.Role.USER,
            buildString {
                append("【追问：")
                append(selectedText.trim())
                append("】")
                if (question.isNotBlank()) {
                    append(question)
                }
            }
        )

    /** 详细解答标记 */
    val detailedSolution: LlmMessage =
        LlmMessage(LlmMessage.Role.USER, "【详细解答】")

    /** 出例题标记 */
    val askExample: LlmMessage =
        LlmMessage(LlmMessage.Role.USER, "请根据当前讨论的内容【出例题】。")

    /** 划重点标记 */
    fun highlight(sourceText: String): LlmMessage =
        LlmMessage(LlmMessage.Role.USER, "【划重点请求】请把下面内容提炼成要点列表：\n$sourceText")
}

/** 对话模式（精讲/期末冲刺）；切换不改 system prompt，只追加标记消息 */
enum class ChatMode(val label: String, val marker: String) {
    DETAIL("精讲", "【切换模式：精讲】"),
    EXAM_SPRINT("期末冲刺", "【切换模式：期末冲刺】"),
}
