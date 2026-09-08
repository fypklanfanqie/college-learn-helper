package com.zhiwei.math.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.prefs.ApiConfig
import com.zhiwei.math.data.prefs.ApiKeyStore
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.GlassHost
import com.zhiwei.math.llm.core.Protocol
import com.zhiwei.math.llm.core.ProviderPreset
import com.zhiwei.math.ui.common.ApiConfigForm
import com.zhiwei.math.ui.common.ProviderPicker
import com.zhiwei.math.ui.components.IosFilledButton
import com.zhiwei.math.ui.components.IosNavBar
import com.zhiwei.math.ui.components.IosPlainButton
import com.zhiwei.math.ui.components.SectionHeader
import com.zhiwei.math.ui.theme.LocalIosPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** 设置 · API 配置（聊天页右上角与设置页共用入口，iOS 观感 + 玻璃顶栏） */
@Composable
fun ApiSettingsScreen(
    onBack: () -> Unit,
    viewModel: ApiSettingsViewModel = koinViewModel(),
) {
    val palette = LocalIosPalette.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    GlassHost(
        modifier = Modifier.fillMaxSize(),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(top = 100.dp, start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProviderPicker(selectedId = state.providerId, onSelect = viewModel::selectProvider)
                ApiConfigForm(
                    baseUrl = state.baseUrl,
                    onBaseUrlChange = viewModel::setBaseUrl,
                    model = state.model,
                    onModelChange = viewModel::setModel,
                    apiKey = state.apiKey,
                    onApiKeyChange = viewModel::setApiKey,
                    protocol = state.protocol,
                    onProtocolChange = viewModel::setProtocol,
                    modelSuggestions = state.modelSuggestions,
                    supportsVision = state.supportsVision,
                    onSupportsVisionChange = viewModel::setSupportsVision,
                )
                if (state.error != null) {
                    Text(state.error!!, color = palette.red, style = MaterialTheme.typography.bodySmall)
                }
                if (state.saved) {
                    Text("已保存", color = palette.blue, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                IosFilledButton(
                    text = "保存",
                    onClick = { viewModel.save() },
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
                IosPlainButton(text = "取消", onClick = onBack, modifier = Modifier.fillMaxWidth())
            }
        },
        overlay = {
            IosNavBar(
                title = "API 配置",
                onBack = onBack,
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter),
            )
        },
    )
}

data class ApiSettingsUiState(
    val loaded: Boolean = false,
    val providerId: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val protocol: Protocol = Protocol.OPENAI,
    val supportsVision: Boolean = true,
    val modelSuggestions: List<String> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank() && apiKey.isNotBlank()
}

class ApiSettingsViewModel(
    private val settings: SettingsStore,
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {

    val state = MutableStateFlow(ApiSettingsUiState())

    fun load() {
        viewModelScope.launch {
            val cfg = settings.apiConfig.first()
            val key = apiKeyStore.apiKey()
            state.value = state.value.copy(
                loaded = true,
                providerId = cfg.providerId,
                baseUrl = cfg.baseUrl,
                model = cfg.model,
                apiKey = key,
                protocol = cfg.protocol,
                supportsVision = cfg.supportsVision,
            )
        }
    }

    fun selectProvider(preset: ProviderPreset) {
        state.value = state.value.copy(
            providerId = preset.id,
            baseUrl = preset.baseUrl,
            model = preset.defaultModel,
            protocol = preset.protocol,
            supportsVision = preset.supportsVision,
            modelSuggestions = preset.models,
            saved = false,
        )
    }

    fun setBaseUrl(v: String) { state.value = state.value.copy(baseUrl = v, saved = false) }
    fun setModel(v: String) { state.value = state.value.copy(model = v, saved = false) }
    fun setApiKey(v: String) { state.value = state.value.copy(apiKey = v, saved = false) }
    fun setProtocol(v: Protocol) { state.value = state.value.copy(protocol = v, saved = false) }
    fun setSupportsVision(v: Boolean) { state.value = state.value.copy(supportsVision = v, saved = false) }

    fun save() {
        viewModelScope.launch {
            runCatching {
                val s = state.value
                settings.setApiConfig(
                    ApiConfig(
                        providerId = s.providerId,
                        baseUrl = s.baseUrl.trim(),
                        model = s.model.trim(),
                        protocol = s.protocol,
                        supportsVision = s.supportsVision,
                        configured = true,
                    )
                )
                apiKeyStore.saveApiKey(s.apiKey.trim())
            }.onSuccess {
                state.value = state.value.copy(saved = true, error = null)
            }.onFailure { e ->
                state.value = state.value.copy(error = e.message ?: "保存失败")
            }
        }
    }
}
