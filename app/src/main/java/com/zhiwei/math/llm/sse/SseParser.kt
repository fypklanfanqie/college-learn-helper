package com.zhiwei.math.llm.sse

/**
 * 手写 SSE 解析器（OkHttp 源流 → 事件）。
 *
 * 兼容两种形态（PROJECT-BRIEF.md 六）：
 * - OpenAI data-only：`data: {...}` / `data: [DONE]`
 * - Anthropic：`event: <type>` 行 + `data: <json>` 行（ping / error 传输层事件也要处理）
 *
 * 宽容性（风险 6：厂商 SSE 方言）：缺字段容错、跨 chunk 的事件拆分、CRLF、注释行(:)、
 * 多行 data 拼接、末尾无换行的最后一个事件。
 */
class SseParser {

    private val dataLines = StringBuilder()
    private var eventName: String? = null
    private var pending = ""

    /** 一次喂入任意长度的文本（可为半个事件），返回其中完整的事件。 */
    fun feed(chunk: String): List<SseEvent> {
        val events = mutableListOf<SseEvent>()
        val text = pending + chunk
        pending = ""
        var lineStart = 0
        while (true) {
            val nl = text.indexOf('\n', lineStart)
            if (nl < 0) {
                pending = text.substring(lineStart)
                break
            }
            val line = text.substring(lineStart, nl).trimEnd('\r')
            lineStart = nl + 1
            processLine(line)?.let { events.add(it) }
        }
        return events
    }

    /** 流结束时调用：先处理残留的未换行行，再冲出未终止的最后一个事件。 */
    fun finish(): List<SseEvent> {
        val rest = pending
        pending = ""
        if (rest.isNotEmpty()) processLine(rest.trimEnd('\r'))?.let { return listOf(it) }
        return flushEvent()?.let { listOf(it) } ?: emptyList()
    }

    private fun processLine(line: String): SseEvent? {
        return when {
            line.isEmpty() -> flushEvent()
            line.startsWith(":") -> null // 注释/心跳
            line.startsWith("event:") -> {
                eventName = line.removePrefix("event:").trim()
                null
            }
            line.startsWith("data:") -> {
                if (dataLines.isNotEmpty()) dataLines.append('\n')
                dataLines.append(line.removePrefix("data:").trim())
                null
            }
            else -> null
        }
    }

    private fun flushEvent(): SseEvent? {
        val data = dataLines.toString()
        val event = eventName
        dataLines.clear()
        eventName = null
        if (data.isEmpty()) return null
        return if (data == "[DONE]") SseEvent.Done
        else SseEvent.Data(eventName = event, data = data)
    }
}

sealed interface SseEvent {
    /** 带 event: 行名的 data 事件（OpenAI 流 event 名为 null） */
    data class Data(val eventName: String?, val data: String) : SseEvent
    data object Done : SseEvent
}
