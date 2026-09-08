package com.zhiwei.math.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zhiwei.math.llm.core.Protocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** API 配置（厂商预设 + 可覆盖的 base_url/model/key；key 另存加密区） */
data class ApiConfig(
    val providerId: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val protocol: Protocol = Protocol.OPENAI,
    val supportsVision: Boolean = true,
    val configured: Boolean = false,
)

/**
 * 玻璃效果参数（重设计：10 项液态滑杆 + 4 项毛玻璃，向后兼容旧 key）。
 * 液态 10 参数：blur/refractionHeight/refractionAmount/chromatic/depth/vibrancy/
 * brightness/surfaceTint/highlightAlpha/shadowAlpha（其中前 3 个沿用旧 key）。
 * FROSTED 4 参数：opacity（旧 key）/frostedBlur/frostedTint/frostedHighlight。
 */
data class GlassSettings(
    /** frosted=毛玻璃（默认） / liquid=液态玻璃 / plain=半透明降级 */
    val mode: String = "frosted",
    // ── 液态参数（前 3 个为旧 key，老用户存的值继续生效）──
    val blurRadius: Int = 8,           // 0-40dp
    val refractionHeight: Int = 24,    // 0-48dp
    val refractionAmount: Int = 24,    // 0-96dp
    val chromaticAberration: Int = 60, // 0-100%，>50 开启色散
    val depthEffect: Int = 0,          // 0-100%，>50 开启景深
    val vibrancy: Int = 100,           // 0-200%
    val brightness: Int = 0,           // -50~+50
    val surfaceTint: Int = 40,         // 0-100%
    val highlightAlpha: Int = 100,     // 0-100%
    val shadowAlpha: Int = 100,        // 0-100%
    // ── 毛玻璃参数（opacity 沿用旧 key）──
    val opacity: Int = 70,             // 0-100%
    val frostedBlur: Int = 20,         // 0-40dp
    val frostedTint: Int = 50,         // 0-100（0 冷 ↔ 100 暖）
    val frostedHighlight: Int = 100,   // 0-100%
)

/** 液态玻璃预设（iOS 原版 / 清透 / 浓郁 / 夸张） */
val LIQUID_PRESETS: Map<String, GlassSettings> = mapOf(
    "iOS 原版" to GlassSettings(
        mode = "liquid", blurRadius = 8, refractionHeight = 24, refractionAmount = 24,
        chromaticAberration = 60, depthEffect = 0, vibrancy = 100, brightness = 0,
        surfaceTint = 40, highlightAlpha = 100, shadowAlpha = 100,
    ),
    "清透" to GlassSettings(
        mode = "liquid", blurRadius = 4, refractionHeight = 12, refractionAmount = 12,
        chromaticAberration = 30, depthEffect = 0, vibrancy = 120, brightness = 5,
        surfaceTint = 20, highlightAlpha = 80, shadowAlpha = 60,
    ),
    "浓郁" to GlassSettings(
        mode = "liquid", blurRadius = 16, refractionHeight = 32, refractionAmount = 48,
        chromaticAberration = 80, depthEffect = 20, vibrancy = 150, brightness = -5,
        surfaceTint = 60, highlightAlpha = 100, shadowAlpha = 100,
    ),
    "夸张" to GlassSettings(
        mode = "liquid", blurRadius = 28, refractionHeight = 48, refractionAmount = 96,
        chromaticAberration = 100, depthEffect = 50, vibrancy = 180, brightness = 10,
        surfaceTint = 80, highlightAlpha = 100, shadowAlpha = 100,
    ),
)

/** 外观设置 */
data class AppearanceSettings(
    /** light / dark / system */
    val theme: String = "system",
    /** MiSans / system 字体 */
    val font: String = "misans",
    /** 聊天背景：空 = 默认；其他为图片 URI */
    val chatBackgroundUri: String = "",
    /** 震动反馈开关（长按/按钮），默认开 */
    val haptics: Boolean = true,
)

class SettingsStore(private val context: Context) : com.zhiwei.math.glass.GlassProbe {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val providerId = stringPreferencesKey("api_provider_id")
        val baseUrl = stringPreferencesKey("api_base_url")
        val model = stringPreferencesKey("api_model")
        val protocol = stringPreferencesKey("api_protocol")
        val supportsVision = booleanPreferencesKey("api_supports_vision")
        val apiConfigured = booleanPreferencesKey("api_configured")
        val glassMode = stringPreferencesKey("glass_mode")
        // 旧 key（继续读写，向后兼容）
        val glassRefractionHeight = intPreferencesKey("glass_refraction_height")
        val glassRefractionAmount = intPreferencesKey("glass_refraction_amount")
        val glassBlur = intPreferencesKey("glass_blur")
        val glassOpacity = intPreferencesKey("glass_opacity")
        // 液态新 key
        val glassChromatic = intPreferencesKey("glass_chromatic")
        val glassDepth = intPreferencesKey("glass_depth")
        val glassVibrancy = intPreferencesKey("glass_vibrancy")
        val glassBrightness = intPreferencesKey("glass_brightness")
        val glassSurfaceTint = intPreferencesKey("glass_surface_tint")
        val glassHighlight = intPreferencesKey("glass_highlight")
        val glassShadow = intPreferencesKey("glass_shadow")
        // 毛玻璃新 key
        val frostedBlur = intPreferencesKey("frosted_blur")
        val frostedTint = intPreferencesKey("frosted_tint")
        val frostedHighlight = intPreferencesKey("frosted_highlight")
        // 崩溃探针 / 禁用标记
        val glassRenderProbe = booleanPreferencesKey("glass_render_probe")
        val liquidGlassDisabled = booleanPreferencesKey("liquid_glass_disabled")
        val theme = stringPreferencesKey("theme")
        val font = stringPreferencesKey("font")
        val chatBackgroundUri = stringPreferencesKey("chat_background_uri")
        val haptics = booleanPreferencesKey("haptics_enabled")
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingDone] ?: false }

    val apiConfig: Flow<ApiConfig> = context.dataStore.data.map { p ->
        ApiConfig(
            providerId = p[Keys.providerId] ?: "",
            baseUrl = p[Keys.baseUrl] ?: "",
            model = p[Keys.model] ?: "",
            protocol = if (p[Keys.protocol] == "ANTHROPIC") Protocol.ANTHROPIC else Protocol.OPENAI,
            supportsVision = p[Keys.supportsVision] ?: true,
            configured = p[Keys.apiConfigured] ?: false,
        )
    }

    val glass: Flow<GlassSettings> = context.dataStore.data.map { p ->
        GlassSettings(
            mode = p[Keys.glassMode] ?: "frosted",
            // 旧 key 读旧默认值缺失时用新默认（40/24/18 为老默认，老用户已有值不受影响）
            refractionHeight = p[Keys.glassRefractionHeight] ?: 24,
            refractionAmount = p[Keys.glassRefractionAmount] ?: 24,
            blurRadius = p[Keys.glassBlur] ?: 8,
            chromaticAberration = p[Keys.glassChromatic] ?: 60,
            depthEffect = p[Keys.glassDepth] ?: 0,
            vibrancy = p[Keys.glassVibrancy] ?: 100,
            brightness = p[Keys.glassBrightness] ?: 0,
            surfaceTint = p[Keys.glassSurfaceTint] ?: 40,
            highlightAlpha = p[Keys.glassHighlight] ?: 100,
            shadowAlpha = p[Keys.glassShadow] ?: 100,
            opacity = p[Keys.glassOpacity] ?: 70,
            frostedBlur = p[Keys.frostedBlur] ?: 20,
            frostedTint = p[Keys.frostedTint] ?: 50,
            frostedHighlight = p[Keys.frostedHighlight] ?: 100,
        )
    }

    /** 液态玻璃是否因渲染崩溃被自动禁用（SIGSEGV 探针残留 → true，用户可手动重试） */
    val liquidGlassDisabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.liquidGlassDisabled] ?: false }

    val appearance: Flow<AppearanceSettings> = context.dataStore.data.map { p ->
        AppearanceSettings(
            theme = p[Keys.theme] ?: "system",
            font = p[Keys.font] ?: "misans",
            chatBackgroundUri = p[Keys.chatBackgroundUri] ?: "",
            haptics = p[Keys.haptics] ?: true,
        )
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.onboardingDone] = true }
    }

    suspend fun setApiConfig(config: ApiConfig) {
        context.dataStore.edit { p ->
            p[Keys.providerId] = config.providerId
            p[Keys.baseUrl] = config.baseUrl
            p[Keys.model] = config.model
            p[Keys.protocol] = config.protocol.name
            p[Keys.supportsVision] = config.supportsVision
            p[Keys.apiConfigured] = config.configured
        }
    }

    suspend fun setGlass(settings: GlassSettings) {
        context.dataStore.edit { p ->
            p[Keys.glassMode] = settings.mode
            p[Keys.glassRefractionHeight] = settings.refractionHeight
            p[Keys.glassRefractionAmount] = settings.refractionAmount
            p[Keys.glassBlur] = settings.blurRadius
            p[Keys.glassChromatic] = settings.chromaticAberration
            p[Keys.glassDepth] = settings.depthEffect
            p[Keys.glassVibrancy] = settings.vibrancy
            p[Keys.glassBrightness] = settings.brightness
            p[Keys.glassSurfaceTint] = settings.surfaceTint
            p[Keys.glassHighlight] = settings.highlightAlpha
            p[Keys.glassShadow] = settings.shadowAlpha
            p[Keys.glassOpacity] = settings.opacity
            p[Keys.frostedBlur] = settings.frostedBlur
            p[Keys.frostedTint] = settings.frostedTint
            p[Keys.frostedHighlight] = settings.frostedHighlight
        }
    }

    suspend fun setAppearance(settings: AppearanceSettings) {
        context.dataStore.edit { p ->
            p[Keys.theme] = settings.theme
            p[Keys.font] = settings.font
            p[Keys.chatBackgroundUri] = settings.chatBackgroundUri
            p[Keys.haptics] = settings.haptics
        }
    }

    // ── SIGSEGV 探针（native 崩溃 catch 不住，靠「渲染前打点、重启后检测残留」）──

    /**
     * 启动时消费探针：若残留 true = 上次 LIQUID 首帧渲染前进程被杀（RenderThread
     * SIGSEGV）→ 写 liquidGlassDisabled=true 并返回 true（提示用户已自动降级）。
     */
    suspend fun consumeBootProbe(): Boolean {
        val probeStuck = context.dataStore.data
            .map { it[Keys.glassRenderProbe] ?: false }
            .first()
        if (probeStuck) {
            context.dataStore.edit {
                it[Keys.liquidGlassDisabled] = true
                it[Keys.glassRenderProbe] = false
            }
        }
        return probeStuck
    }

    /** LIQUID 首帧渲染前置位打点（GlassHost 调用，须在首帧绘制前完成落盘） */
    override suspend fun armRenderProbe() {
        context.dataStore.edit { it[Keys.glassRenderProbe] = true }
    }

    /** 首帧渲染完成 → 清除打点 */
    override suspend fun clearRenderProbe() {
        context.dataStore.edit { it[Keys.glassRenderProbe] = false }
    }

    /** 用户在设置页手动重新启用液态玻璃 */
    suspend fun reenableLiquidGlass() {
        context.dataStore.edit { it[Keys.liquidGlassDisabled] = false }
    }
}
