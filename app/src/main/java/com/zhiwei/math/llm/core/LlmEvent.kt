package com.zhiwei.math.llm.core

import kotlinx.serialization.Serializable

/**
 * LLM 流式事件统一模型：两套协议（OpenAI/Anthropic）都归一化到这几个事件。
 */
sealed interface LlmEvent {
    /** 文本增量（token-by-token） */
    data class Delta(val text: String) : LlmEvent

    /** 用量统计（流结束时；缓存命中信息用于对话详情展示） */
    data class Usage(
        val inputTokens: Int,
        val outputTokens: Int,
        val cacheReadTokens: Int? = null,
        val cacheWriteTokens: Int? = null,
    ) : LlmEvent

    /** 流正常结束 */
    data object Done : LlmEvent

    /** 流错误（协议错误/网络错误；被打断用 CancellationException 表达，不走此事件） */
    data class Error(val message: String, val cause: Throwable? = null) : LlmEvent
}

/**
 * 一条对话消息（协议无关）。
 * 图片：base64 与 mime 只在用户消息中出现；文档提取出的文本直接拼进 content。
 */
@Serializable
data class LlmMessage(
    val role: Role,
    val content: String,
    val imageBase64: String? = null,
    val imageMime: String? = null,
) {
    enum class Role { USER, ASSISTANT }
}

/** 单次流式请求参数 */
data class LlmRequest(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    /** 系统提示词块（字节稳定分层，见 PromptAssembler）；Anthropic 映射为 system blocks + cache_control */
    val systemBlocks: List<SystemBlock>,
    val messages: List<LlmMessage>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 8192,
)

/**
 * 系统提示词块。
 * [stable] 为 true 的块在 Anthropic 协议下打 cache_control 断点（Layer 0+1、Layer 2 各一个，≤4 限制内）。
 */
data class SystemBlock(
    val text: String,
    val stable: Boolean,
)
