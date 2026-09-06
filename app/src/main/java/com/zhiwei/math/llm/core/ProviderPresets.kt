package com.zhiwei.math.llm.core

/**
 * 内置厂商预设表（PROJECT-BRIEF.md 6.2，2026-09 核实）。
 * 每个预设允许用户改 base_url / model / api_key；[supportsVision] 用于黄条提示 OCR 降级。
 */
enum class Protocol { OPENAI, ANTHROPIC }

data class ProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val protocol: Protocol,
    val defaultModel: String,
    val visionModelExample: String?,
    val supportsVision: Boolean,
    val models: List<String>,
)

object ProviderPresets {

    val ALL: List<ProviderPreset> = listOf(
        ProviderPreset(
            id = "deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            protocol = Protocol.OPENAI,
            defaultModel = "deepseek-v4-flash",
            visionModelExample = "deepseek-v4-flash-vision-exp",
            supportsVision = true,
            models = listOf("deepseek-v4-flash", "deepseek-v4-pro", "deepseek-v4-flash-vision-exp"),
        ),
        ProviderPreset(
            id = "zhipu",
            displayName = "智谱 GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            protocol = Protocol.OPENAI,
            defaultModel = "glm-5.3-flash",
            visionModelExample = "glm-5.3-flash",
            supportsVision = true,
            models = listOf("glm-5.3-flash", "glm-4.6v", "glm-4.5v"),
        ),
        ProviderPreset(
            id = "kimi",
            displayName = "Kimi 月之暗面",
            baseUrl = "https://api.moonshot.cn/v1",
            protocol = Protocol.OPENAI,
            defaultModel = "kimi-k3",
            visionModelExample = "kimi-k3",
            supportsVision = true,
            models = listOf("kimi-k3", "kimi-k2.6", "kimi-k2.7-code", "kimi-k2.7-highspeed"),
        ),
        ProviderPreset(
            id = "qwen",
            displayName = "阿里通义",
            baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            protocol = Protocol.OPENAI,
            defaultModel = "qwen3.8-max",
            visionModelExample = "qwen3.5-ocr",
            supportsVision = true,
            models = listOf("qwen3.8-max", "qwen3.8-flash", "qwen3.7-plus", "qwen3.5-ocr", "qwen3-vl-plus"),
        ),
        ProviderPreset(
            id = "siliconflow",
            displayName = "SiliconFlow 硅基流动",
            baseUrl = "https://api.siliconflow.cn/v1",
            protocol = Protocol.OPENAI,
            defaultModel = "Qwen/Qwen3-VL-72B-Instruct",
            visionModelExample = "Qwen/Qwen3-VL-72B-Instruct",
            supportsVision = true,
            models = listOf("Qwen/Qwen3-VL-72B-Instruct", "Qwen/Qwen2.5-VL-72B-Instruct", "deepseek-ai/DeepSeek-V4"),
        ),
        ProviderPreset(
            id = "openrouter",
            displayName = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            protocol = Protocol.OPENAI,
            defaultModel = "deepseek/deepseek-v4-flash-vision-exp",
            visionModelExample = "moonshotai/kimi-k3",
            supportsVision = true,
            models = listOf(
                "deepseek/deepseek-v4-flash-vision-exp",
                "moonshotai/kimi-k3",
                "z-ai/glm-5.3-flash",
                "qwen/qwen3.8-max-0902",
                "google/gemini-3.8-flash",
            ),
        ),
        ProviderPreset(
            id = "gemini",
            displayName = "Gemini 兼容端点",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/",
            protocol = Protocol.OPENAI,
            defaultModel = "gemini-2.5-flash",
            visionModelExample = "gemini-2.5-flash",
            supportsVision = true,
            models = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.5-flash-lite"),
        ),
        ProviderPreset(
            id = "openai",
            displayName = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            protocol = Protocol.OPENAI,
            defaultModel = "gpt-5.6-sol",
            visionModelExample = "gpt-5.6-sol",
            supportsVision = true,
            models = listOf("gpt-6-astra", "gpt-5.6-sol", "gpt-5.6-terra", "gpt-5.6-luna", "gpt-5.5", "gpt-4.1", "gpt-4o"),
        ),
        ProviderPreset(
            id = "anthropic",
            displayName = "Anthropic",
            baseUrl = "https://api.anthropic.com",
            protocol = Protocol.ANTHROPIC,
            defaultModel = "claude-sonnet-5",
            visionModelExample = "claude-sonnet-5",
            supportsVision = true,
            models = listOf(
                "claude-sonnet-5",
                "claude-opus-5",
                "claude-fable-5-1",
                "claude-mythos-5-1",
                "claude-haiku-4-5",
            ),
        ),
    )

    /** 自定义（用户填 base_url/model/key，二选一协议） */
    val CUSTOM = ProviderPreset(
        id = "custom",
        displayName = "自定义",
        baseUrl = "",
        protocol = Protocol.OPENAI,
        defaultModel = "",
        visionModelExample = null,
        supportsVision = true,
        models = emptyList(),
    )

    fun byId(id: String): ProviderPreset = ALL.firstOrNull { it.id == id } ?: CUSTOM
}
