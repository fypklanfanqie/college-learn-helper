package com.zhiwei.math.llm

import com.zhiwei.math.data.prefs.ApiConfig
import com.zhiwei.math.data.prefs.ApiKeyStore
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.llm.anthropic.AnthropicClient
import com.zhiwei.math.llm.core.LlmEvent
import com.zhiwei.math.llm.core.LlmMessage
import com.zhiwei.math.llm.core.LlmRequest
import com.zhiwei.math.llm.core.Protocol
import com.zhiwei.math.llm.core.SystemBlock
import com.zhiwei.math.llm.openai.OpenAiClient
import com.zhiwei.math.prompt.MetaPrompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient

/**
 * LLM 门面：按 API 配置选择协议客户端；提供 Layer 2 定制提示词生成。
 */
class LlmRepository(
    http: OkHttpClient,
    private val settings: SettingsStore,
    private val apiKeyStore: ApiKeyStore,
) {
    private val openAi = OpenAiClient(http)
    private val anthropic = AnthropicClient(http)

    /** 当前生效的 API 配置（含解密后的 key，供调用方组装请求） */
    suspend fun currentConfig(): Pair<ApiConfig, String> {
        val config = settings.apiConfig.first()
        return config to apiKeyStore.apiKey()
    }

    fun stream(config: ApiConfig, apiKey: String, request: LlmRequest): Flow<LlmEvent> =
        when (config.protocol) {
            Protocol.OPENAI -> openAi.stream(request)
            Protocol.ANTHROPIC -> anthropic.stream(request)
        }

    /**
     * 生成 Layer 2 定制提示词段（对话设置保存时调用）。
     * 元提示词本身按「固定前缀 + 末尾变量」组织（缓存优化 6.3-7）。
     * 失败抛异常，由 UI 层重试/模板兜底。
     */
    suspend fun generateLayer2(
        examFocus: String,
        nonExamPoints: String,
        teacherPersona: String,
        myLevel: String,
    ): String {
        val (config, apiKey) = currentConfig()
        require(config.configured && apiKey.isNotBlank()) { "API 未配置" }
        val request = LlmRequest(
            baseUrl = config.baseUrl,
            apiKey = apiKey,
            model = config.model,
            systemBlocks = listOf(SystemBlock(text = MetaPrompt.TEXT, stable = true)),
            messages = listOf(
                LlmMessage(
                    LlmMessage.Role.USER,
                    MetaPrompt.buildUserMessage(examFocus, nonExamPoints, teacherPersona, myLevel),
                )
            ),
            temperature = 0.4,
            maxTokens = 2048,
        )
        val sb = StringBuilder()
        stream(config, apiKey, request).collect { ev ->
            when (ev) {
                is LlmEvent.Delta -> sb.append(ev.text)
                is LlmEvent.Error -> throw IllegalStateException(ev.message, ev.cause)
                else -> Unit
            }
        }
        val text = sb.toString().trim()
        require(text.isNotBlank()) { "生成结果为空" }
        return text
    }
}
