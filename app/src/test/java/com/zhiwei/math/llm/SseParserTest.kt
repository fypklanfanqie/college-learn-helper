package com.zhiwei.math.llm

import com.zhiwei.math.llm.sse.SseEvent
import com.zhiwei.math.llm.sse.SseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    @Test
    fun `openai data-only events`() {
        val parser = SseParser()
        val input = "data: {\"a\":1}\n\ndata: {\"b\":2}\n\ndata: [DONE]\n\n"
        val events = parser.feed(input)
        assertEquals(3, events.size)
        assertTrue(events[0] is SseEvent.Data)
        assertEquals("{\"a\":1}", (events[0] as SseEvent.Data).data)
        assertTrue(events[2] is SseEvent.Done)
    }

    @Test
    fun `anthropic event with name and ping`() {
        val parser = SseParser()
        val input = "event: ping\ndata: {\"type\":\"ping\"}\n\n" +
            "event: content_block_delta\ndata: {\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"你好\"}}\n\n"
        val events = parser.feed(input)
        assertEquals(2, events.size)
        val delta = events[1] as SseEvent.Data
        assertEquals("content_block_delta", delta.eventName)
        assertTrue(delta.data.contains("text_delta"))
    }

    @Test
    fun `chunked feed reassembles event split across chunks`() {
        val parser = SseParser()
        val e1 = parser.feed("data: {\"choi")
        assertTrue(e1.isEmpty())
        val e2 = parser.feed("ces\":[{}]}\n")
        assertTrue(e2.isEmpty())
        val e3 = parser.feed("\n")
        assertEquals(1, e3.size)
        assertEquals("{\"choices\":[{}]}", (e3[0] as SseEvent.Data).data)
    }

    @Test
    fun `crlf and comment lines tolerated`() {
        val parser = SseParser()
        val events = parser.feed(": keep-alive\r\ndata: x\r\n\r\n")
        assertEquals(1, events.size)
        assertEquals("x", (events[0] as SseEvent.Data).data)
    }

    @Test
    fun `multi-line data joins with newline`() {
        val parser = SseParser()
        val events = parser.feed("data: line1\ndata: line2\n\n")
        assertEquals(1, events.size)
        assertEquals("line1\nline2", (events[0] as SseEvent.Data).data)
    }

    @Test
    fun `finish flushes unterminated event`() {
        val parser = SseParser()
        parser.feed("data: tail")
        val events = parser.finish()
        assertEquals(1, events.size)
        assertEquals("tail", (events[0] as SseEvent.Data).data)
    }
}
