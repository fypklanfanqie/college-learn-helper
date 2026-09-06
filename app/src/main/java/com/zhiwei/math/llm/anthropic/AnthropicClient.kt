package com.zhiwei.math.llm.anthropic

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
 * Anthropic Messages 协议客户端（Anthropic 官方 + DeepSeek/Kimi 的 /anthropic 端点）。
 *
 * 协议要点（docs-research-llm-protocols.md 2.2）：
 * - 头：x-api-key + anthropic-version: 2023-06-01；max_tokens 必填
 * - 事件流：message_start → content_block_delta(text_delta)* → message_delta(累计 usage) → message_stop
 *   另有 ping / error 传输层事件需容错处理
 * - 缓存断点：system blocks 上放 cache_control ephemeral（stable 块末尾；≤4 限制内用 2 个）
 * - 视觉：{type:"image", source:{type:"base64", media_type, data}}
 * - 打断：call.cancel()，保留已生成部分
 */
class AnthropicClient(private val http: OkHttpClient) {

    fun stream(request: LlmRequest): Flow<LlmEvent> = callbackFlow {
        val body = buildRequestBody(request).toRequestBody("application/json; charset=utf-8".toMediaType())
        val callRef = AtomicReference<Call?>()

        val httpRequest = Request.Builder()
            .url(request.baseUrl.trimEnd('/') + "/v1/messages")
            .header("x-api-key", request.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Accept", "text/event-stream")
            .post(body)
            .build()

        val call = http.newCall(httpRequest)
        callRef.set(call)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (call.isCanceled()) {
                    close()
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
                    var usage: LlmEvent.Usage? = null
                    try {
                        loop@ while (true) {
                            val line = source.readUtf8Line() ?: break
                            for (ev in parser.feed(line + "\n")) {
                                when (ev) {
                                    SseEvent.Done -> break@loop
                                    is SseEvent.Data -> {
                                        val out = ev.toLlmEvents()
                                        out.firstOrNull { it is LlmEvent.Usage }?.let { usage = it as LlmEvent.Usage }
                                        out.forEach { trySend(it) }
                                    }
                                }
                            }
                        }
                        usage?.let { trySend(it) } // message_delta 的累计 output_tokens 兜底
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
            callRef.get()?.cancel()
        }
    }.flowOn(Dispatchers.IO)

    private fun SseEvent.Data.toLlmEvents(): List<LlmEvent> {
        val eventName = eventName
        // 传输层事件（不在 SDK 类型 union 中）：ping 忽略、error 报错
        if (eventName == "ping") return emptyList()
        if (eventName == "error") {
            val msg = try {
                JSON.decodeFromString<ErrorEvent>(data).error?.message ?: data.take(300)
            } catch (_: Exception) { data.take(300) }
            return listOf(LlmEvent.Error(msg))
        }
        return try {
            val event = JSON.decodeFromString<StreamEvent>(data)
            when (event.type) {
                "message_start" -> {
                    val u = event.message?.usage
                    if (u != null) listOf(
                        LlmEvent.Usage(
                            inputTokens = u.inputTokens ?: 0,
                            outputTokens = 0,
                            cacheReadTokens = u.cacheReadInputTokens,
                            cacheWriteTokens = u.cacheCreationInputTokens,
                        )
                    ) else emptyList()
                }
                "content_block_delta" -> {
                    val d = event.delta
                    if (d?.type == "text_delta" && !d.text.isNullOrEmpty()) listOf(LlmEvent.Delta(d.text))
                    else emptyList()
                }
                "message_delta" -> {
                    val u = event.usage
                    if (u?.outputTokens != null) listOf(
                        LlmEvent.Usage(
                            inputTokens = 0,
                            outputTokens = u.outputTokens,
                            cacheReadTokens = u.cacheReadInputTokens,
                            cacheWriteTokens = u.cacheCreationInputTokens,
                        )
                    ) else emptyList()
                }
                "message_stop" -> listOf(LlmEvent.Done)
                else -> emptyList() // content_block_start/stop 等无文本增量
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun buildRequestBody(request: LlmRequest): String {
        // system blocks：stable 块各打一个 cache_control 断点（Layer0+1 末尾、Layer2 末尾，共 ≤2 个，限制 4）
        val systemBlocks = request.systemBlocks.filter { it.text.isNotEmpty() }.map { b ->
            SystemBlockReq(
                text = b.text,
                cacheControl = if (b.stable) CacheControl(type = "ephemeral") else null,
            )
        }

        val messages = request.messages.map { m ->
            val blocks = buildList {
                if (m.imageBase64 != null && m.role == LlmMessage.Role.USER) {
                    add(
                        ContentBlock(
                            type = "image",
                            source = ImageSource(
                                type = "base64",
                                mediaType = m.imageMime ?: "image/jpeg",
                                data = m.imageBase64,
                            ),
                        )
                    )
                }
                if (m.content.isNotEmpty()) {
                    add(ContentBlock(type = "text", text = m.content))
                }
            }
            MessageReq(
                role = when (m.role) {
                    LlmMessage.Role.USER -> "user"
                    LlmMessage.Role.ASSISTANT -> "assistant"
                },
                content = blocks,
            )
        }

        val req = AnthropicRequest(
            model = request.model,
            maxTokens = request.maxTokens,
            system = systemBlocks,
            messages = messages,
            stream = true,
            temperature = request.temperature,
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
internal data class AnthropicRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: List<SystemBlockReq>,
    val messages: List<MessageReq>,
    val stream: Boolean,
    val temperature: Double,
)

@Serializable
internal data class SystemBlockReq(
    val type: String = "text",
    val text: String,
    @SerialName("cache_control") val cacheControl: CacheControl? = null,
)

@Serializable
internal data class CacheControl(val type: String)

@Serializable
internal data class MessageReq(
    val role: String,
    val content: List<ContentBlock>,
)

@Serializable
internal data class ContentBlock(
    val type: String,
    val text: String? = null,
    val source: ImageSource? = null,
)

@Serializable
internal data class ImageSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String,
)

// ---------- 响应事件模型 ----------

@Serializable
internal data class StreamEvent(
    val type: String,
    val message: MessageStartMessage? = null,
    val delta: StreamDelta? = null,
    val usage: MessageDeltaUsage? = null,
)

@Serializable
internal data class MessageStartMessage(
    val usage: MessageStartUsage? = null,
)

@Serializable
internal data class MessageStartUsage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("cache_creation_input_tokens") val cacheCreationInputTokens: Int? = null,
    @SerialName("cache_read_input_tokens") val cacheReadInputTokens: Int? = null,
)

@Serializable
internal data class StreamDelta(
    val type: String? = null,
    val text: String? = null,
)

@Serializable
internal data class MessageDeltaUsage(
    @SerialName("output_tokens") val outputTokens: Int? = null,
    @SerialName("cache_creation_input_tokens") val cacheCreationInputTokens: Int? = null,
    @SerialName("cache_read_input_tokens") val cacheReadInputTokens: Int? = null,
)

@Serializable
internal data class ErrorEvent(
    val error: ErrorDetail? = null,
)

@Serializable
internal data class ErrorDetail(
    val message: String? = null,
)
