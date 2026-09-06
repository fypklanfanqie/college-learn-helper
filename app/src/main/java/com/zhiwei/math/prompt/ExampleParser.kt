package com.zhiwei.math.prompt

/**
 * 解析宋老师协议的 ```example 围栏（题目/解答/考点 三段式）为例题数据。
 * 宽容解析：缺段时对应字段为空。
 */
data class ExampleData(
    val question: String,
    val solution: String,
    val tags: String,
)

object ExampleParser {

    fun parse(fenceBody: String): ExampleData {
        var question = ""
        var solution = ""
        var tags = ""
        var current: String? = null
        val buffers = mutableMapOf<String, StringBuilder>()

        for (line in fenceBody.lines()) {
            val trimmed = line.trim()
            val header = when {
                trimmed.startsWith("题目：") || trimmed.startsWith("题目:") -> "题目"
                trimmed.startsWith("解答：") || trimmed.startsWith("解答:") -> "解答"
                trimmed.startsWith("考点：") || trimmed.startsWith("考点:") -> "考点"
                else -> null
            }
            if (header != null) {
                current = header
                buffers.getOrPut(current) { StringBuilder() }
                    .append(trimmed.removePrefix(header).removePrefix("：").removePrefix(":").trim())
                    .append('\n')
            } else if (current != null) {
                buffers.getOrPut(current) { StringBuilder() }.append(line).append('\n')
            }
        }
        question = buffers["题目"]?.toString()?.trim().orEmpty()
        solution = buffers["解答"]?.toString()?.trim().orEmpty()
        tags = buffers["考点"]?.toString()?.trim().orEmpty()
        return ExampleData(question, solution, tags)
    }
}
