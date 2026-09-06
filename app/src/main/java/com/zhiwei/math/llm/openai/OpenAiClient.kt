package com.zhiwei.math.llm.openai

import com.zhiwei.math.llm.core.LlmEvent
import com.zhiwei.math.llm.core.LlmMessage
import com.zhiwei.math.llm.core.LlmRequest
import com.zhiwei.math.llm.sse.SseEvent
import com.zhiwei.math.llm.sse.SseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

/**
 * OpenAI Chat Completions 协议客户端（覆盖 DeepSeek/GLM/Kimi/通义/SiliconFlow/OpenRouter/Gemini 兼容端点）。
 *
 * 协议要点（docs-research-llm-protocols.md 2.1）：
 * - SSE data-only，文本在 choices[0].delta.content，data: [DONE] 结尾
 * - stream_options:{"include_usage":true}：末尾多收一个 usage chunk（choices 为空数组）
 * - max_tokens 已 deprecated → max_completion_tokens
 * - 视觉：content 数组 + image_url（base64 data URL），detail=auto
 * - 打断：OkHttp call.cancel()，保留已生成部分（由收集方持有）
 * - JSON key 顺序 = kotlinx-serialization 声明序（前缀缓存字节稳定）
 */
class OpenAiClient(private val http: OkHttpClient) {

    fun stream(request: LlmRequest): Flow<LlmEvent> = callbackFlow {
        val body = buildRequestBody(request).toRequestBody("application/json; charset=utf-8".toMediaType())
        val callRef = AtomicReference<Call?>()

        val httpRequest = Request.Builder()
            .url(request.baseUrl.trimEnd('/') + "/chat/completions")
            .header("Authorization", "Bearer ${request.apiKey}")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        val call = http.newCall(httpRequest)
        callRef.set(call)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) {
                    close() // 用户打断：正常收尾
                } else {
                    trySend(LlmEvent.Error("网络请求失败: ${e.message}", e))
                    close()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        val err = runCatching { resp.body?.string() }.getOrNull()
                        trySend(LlmEvent.Error("HTTP ${resp.code}: ${err?.take(500) ?: resp.message}"))
                        close()
                        return
                    }
                    val source = resp.body?.source()
                    if (source == null) {
                        trySend(LlmEvent.Error("响应体为空"))
                        close()
                        return
                    }
                    val parser = SseParser()
                    try {
                        loop@ while (true) {
                            val line = source.readUtf8Line() ?: break
                            for (ev in parser.feed(line + "\n")) {
                                when (ev) {
                                    SseEvent.Done -> break@loop
                                    is SseEvent.Data -> ev.toLlmEvents().forEach { trySend(it) }
                                }
                            }
                        }
                        for (ev in parser.finish()) {
                            if (ev is SseEvent.Data) ev.toLlmEvents().forEach { trySend(it) }
                        }
                        close()
                    } catch (e: IOException) {
                        if (call.isCanceled()) close()
                        else {
                            trySend(LlmEvent.Error("流读取中断: ${e.message}", e))
                            close()
                        }
                    }
                }
            }
        })

        awaitClose {
            callRef.get()?.cancel() // 打断 = call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private fun SseEvent.Data.toLlmEvents(): List<LlmEvent> {
        return try {
            val chunk = JSON.decodeFromString<ChatChunk>(data)
            val events = mutableListOf<LlmEvent>()
            chunk.choices?.firstOrNull()?.delta?.content?.takeIf { it.isNotEmpty() }?.let {
                events.add(LlmEvent.Delta(it))
            }
            chunk.usage?.let { u ->
                events.add(
                    LlmEvent.Usage(
                        inputTokens = u.promptTokens ?: 0,
                        outputTokens = u.completionTokens ?: 0,
                        cacheReadTokens = u.promptCacheHitTokens ?: u.promptTokensDetails?.cachedTokens,
                    )
                )
            }
            events
        } catch (_: Exception) {
            // 方言容错：解析失败的 chunk 忽略
            emptyList()
        }
    }

    internal fun buildRequestBody(request: LlmRequest): String {
        val messages = buildList {
            // system prompt 字节稳定分层：stable 层在前，顺序永不变化（缓存核心设计）
            val systemText = request.systemBlocks.joinToString("\n\n") { it.text }
            if (systemText.isNotEmpty()) add(RequestMessage.system(systemText))
            for (m in request.messages) {
                when {
                    m.imageBase64 != null && m.role == LlmMessage.Role.USER -> add(
                        RequestMessage.userParts(
                            listOf(
                                ContentPart.text(m.content),
                                ContentPart.image("data:${m.imageMime ?: "image/jpeg"};base64,${m.imageBase64}"),
                            )
                        )
                    )
                    m.role == LlmMessage.Role.USER -> add(RequestMessage.user(m.content))
                    m.role == LlmMessage.Role.ASSISTANT -> add(RequestMessage.assistant(m.content))
                }
            }
        }
        val req = ChatRequest(
            model = request.model,
            messages = messages,
            stream = true,
            streamOptions = StreamOptions(includeUsage = true),
            temperature = request.temperature,
            maxCompletionTokens = request.maxTokens,
        )
        return JSON.encodeToString(req)
    }

    companion object {
        internal val JSON = Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

// ---------- 请求模型 ----------

@Serializable
internal data class ChatRequest(
    val model: String,
    val messages: List<RequestMessage>,
    val stream: Boolean,
    @SerialName("stream_options") val streamOptions: StreamOptions? = null,
    val temperature: Double,
    @SerialName("max_completion_tokens") val maxCompletionTokens: Int,
)

@Serializable
internal data class StreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean,
)

/** content = 纯文本 JSON 字符串，或视觉请求下的 content part 数组 */
@Serializable
internal data class RequestMessage(
    val role: String,
    val content: JsonElement,
) {
    companion object {
        fun system(text: String) = RequestMessage("system", JsonPrimitive(text))
        fun user(text: String) = RequestMessage("user", JsonPrimitive(text))
        fun assistant(text: String) = RequestMessage("assistant", JsonPrimitive(text))
        fun userParts(parts: List<ContentPart>): RequestMessage {
            val arr = buildJsonArray {
                parts.forEach { add(OpenAiClient.JSON.encodeToJsonElement(it)) }
            }
            return RequestMessage("user", arr)
        }
    }
}

@Serializable
internal data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null,
) {
    companion object {
        fun text(t: String) = ContentPart(type = "text", text = t)
        fun image(dataUrl: String) =
            ContentPart(type = "image_url", imageUrl = ImageUrl(url = dataUrl, detail = "auto"))
    }
}

@Serializable
internal data class ImageUrl(
    val url: String,
    val detail: String? = null,
)

// ---------- 响应模型 ----------

@Serializable
internal data class ChatChunk(
    val choices: List<ChunkChoice>? = null,
    val usage: ChunkUsage? = null,
)

@Serializable
internal data class ChunkChoice(
    val delta: ChunkDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
internal data class ChunkDelta(
    val content: String? = null,
)

@Serializable
internal data class ChunkUsage(
    @SerialName("prompt_tokens") val promptTokens: Int? = null,
    @SerialName("completion_tokens") val completionTokens: Int? = null,
    /** DeepSeek KV 缓存字段 */
    @SerialName("prompt_cache_hit_tokens") val promptCacheHitTokens: Int? = null,
    @SerialName("prompt_tokens_details") val promptTokensDetails: PromptTokensDetails? = null,
)

@Serializable
internal data class PromptTokensDetails(
    @SerialName("cached_tokens") val cachedTokens: Int? = null,
)
