package com.zhiwei.math.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zhiwei.math.llm.core.Protocol
import kotlinx.coroutines.flow.Flow
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

/** 玻璃效果（决策 2/6：默认毛玻璃；API 33+ 可开液态玻璃；参数可调） */
data class GlassSettings(
    /** frosted=毛玻璃（默认） / liquid=液态玻璃 / plain=半透明降级 */
    val mode: String = "frosted",
    val refractionHeight: Int = 40,
    val refractionAmount: Int = 24,
    val blurRadius: Int = 18,
    val opacity: Int = 70,
)

/** 外观设置 */
data class AppearanceSettings(
    /** light / dark / system */
    val theme: String = "system",
    /** MiSans / system 字体 */
    val font: String = "misans",
    /** 聊天背景：空 = 默认；其他为图片 URI */
    val chatBackgroundUri: String = "",
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val providerId = stringPreferencesKey("api_provider_id")
        val baseUrl = stringPreferencesKey("api_base_url")
        val model = stringPreferencesKey("api_model")
        val protocol = stringPreferencesKey("api_protocol")
        val supportsVision = booleanPreferencesKey("api_supports_vision")
        val apiConfigured = booleanPreferencesKey("api_configured")
        val glassMode = stringPreferencesKey("glass_mode")
        val glassRefractionHeight = intPreferencesKey("glass_refraction_height")
        val glassRefractionAmount = intPreferencesKey("glass_refraction_amount")
        val glassBlur = intPreferencesKey("glass_blur")
        val glassOpacity = intPreferencesKey("glass_opacity")
        val theme = stringPreferencesKey("theme")
        val font = stringPreferencesKey("font")
        val chatBackgroundUri = stringPreferencesKey("chat_background_uri")
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
            refractionHeight = p[Keys.glassRefractionHeight] ?: 40,
            refractionAmount = p[Keys.glassRefractionAmount] ?: 24,
            blurRadius = p[Keys.glassBlur] ?: 18,
            opacity = p[Keys.glassOpacity] ?: 70,
        )
    }

    val appearance: Flow<AppearanceSettings> = context.dataStore.data.map { p ->
        AppearanceSettings(
            theme = p[Keys.theme] ?: "system",
            font = p[Keys.font] ?: "misans",
            chatBackgroundUri = p[Keys.chatBackgroundUri] ?: "",
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
            p[Keys.glassOpacity] = settings.opacity
        }
    }

    suspend fun setAppearance(settings: AppearanceSettings) {
        context.dataStore.edit { p ->
            p[Keys.theme] = settings.theme
            p[Keys.font] = settings.font
            p[Keys.chatBackgroundUri] = settings.chatBackgroundUri
        }
    }
}
