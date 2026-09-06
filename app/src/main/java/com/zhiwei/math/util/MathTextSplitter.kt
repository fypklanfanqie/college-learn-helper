package com.zhiwei.math.util

/**
 * 把 LLM 输出文本切成渲染段：
 * - Text：普通 Markdown 文本（含行内 $...$，先按原文显示）
 * - DisplayMath：$$...$$ 独立公式块（闭合才渲染——缓存/性能策略，PROJECT-BRIEF.md 5.3）
 * - ExampleFence：```example 围栏（出例题协议，前端解析为例题卡片）
 *
 * 流式安全：结尾处未闭合的 $$ 或 ``` 一律按普通文本处理（等闭合后自动升级为公式/例题卡）。
 */
sealed interface TextSegment {
    data class Text(val content: String) : TextSegment
    data class DisplayMath(val latex: String) : TextSegment
    data class ExampleFence(val raw: String) : TextSegment
}

object MathTextSplitter {

    private val EXAMPLE_FENCE = Regex("```example\\s*\\n([\\s\\S]*?)(?:```|$)", RegexOption.IGNORE_CASE)

    fun split(raw: String): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        // 先拆 example 围栏
        val parts = splitByRegex(raw, EXAMPLE_FENCE)
        for (part in parts) {
            when (part) {
                is Part.FencePart -> segments.add(TextSegment.ExampleFence(part.content))
                is Part.Plain -> segments.addAll(splitMath(part.text))
            }
        }
        return segments.filter {
            it !is TextSegment.Text || it.content.isNotEmpty()
        }
    }

    private sealed interface Part {
        data class Plain(val text: String) : Part
        data class FencePart(val content: String) : Part
    }

    private fun splitByRegex(text: String, regex: Regex): List<Part> {
        val result = mutableListOf<Part>()
        var index = 0
        for (match in regex.findAll(text)) {
            if (match.range.first > index) {
                result.add(Part.Plain(text.substring(index, match.range.first)))
            }
            result.add(Part.FencePart(match.groupValues[1].trim()))
            index = match.range.last + 1
        }
        if (index < text.length) result.add(Part.Plain(text.substring(index)))
        if (result.isEmpty()) result.add(Part.Plain(text))
        return result
    }

    private fun splitMath(text: String): List<TextSegment> {
        val segments = mutableListOf<TextSegment>()
        var index = 0
        while (true) {
            val open = text.indexOf("$$", index)
            if (open < 0) break
            val close = text.indexOf("$$", open + 2)
            if (close < 0) break // 未闭合：剩余按普通文本（流式）
            if (open > index) segments.add(TextSegment.Text(text.substring(index, open)))
            segments.add(TextSegment.DisplayMath(text.substring(open + 2, close).trim()))
            index = close + 2
        }
        if (index < text.length) segments.add(TextSegment.Text(text.substring(index)))
        if (segments.isEmpty()) segments.add(TextSegment.Text(text))
        return segments
    }
}
