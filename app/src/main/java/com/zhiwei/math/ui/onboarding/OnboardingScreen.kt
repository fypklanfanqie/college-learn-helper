package com.zhiwei.math.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.prefs.ApiConfig
import com.zhiwei.math.data.prefs.ApiKeyStore
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.llm.core.Protocol
import com.zhiwei.math.llm.core.ProviderPreset
import com.zhiwei.math.ui.common.ApiConfigForm
import com.zhiwei.math.ui.common.ProviderPicker
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding（一次性）：欢迎 → API 接入向导（厂商卡片 + 自定义；👁 视觉标注；无视觉黄条提示 OCR）→ 完成。
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.step == 0) {
            Spacer(Modifier.height(120.dp))
            Text("知微数学", style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
            Text(
                "见微知著，循序渐进。\n先告诉我——你要学什么？",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            Button(onClick = { viewModel.nextStep() }, modifier = Modifier.fillMaxWidth()) {
                Text("开始配置")
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Text("接入你的 API", style = MaterialTheme.typography.headlineSmall)
            Text(
                "选择一家模型商，或使用自定义端点（BYOK，Key 只存在你的手机里）",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            ProviderPicker(
                selectedId = state.providerId,
                onSelect = viewModel::selectProvider,
            )
            Spacer(Modifier.height(16.dp))
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
            )
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { viewModel.save(onFinished) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.baseUrl.isNotBlank() && state.model.isNotBlank() && state.apiKey.isNotBlank(),
            ) {
                Text("完成")
            }
        }
    }
}

data class OnboardingUiState(
    val step: Int = 0,
    val providerId: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val protocol: Protocol = Protocol.OPENAI,
    val supportsVision: Boolean = true,
    val modelSuggestions: List<String> = emptyList(),
    val error: String? = null,
)

class OnboardingViewModel(
    private val settings: SettingsStore,
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {

    val state = kotlinx.coroutines.flow.MutableStateFlow(OnboardingUiState())

    fun nextStep() {
        state.value = state.value.copy(step = state.value.step + 1)
    }

    fun selectProvider(preset: ProviderPreset) {
        state.value = state.value.copy(
            providerId = preset.id,
            baseUrl = preset.baseUrl,
            model = preset.defaultModel,
            protocol = preset.protocol,
            supportsVision = preset.supportsVision,
            modelSuggestions = preset.models,
        )
    }

    fun setBaseUrl(v: String) { state.value = state.value.copy(baseUrl = v) }
    fun setModel(v: String) { state.value = state.value.copy(model = v) }
    fun setApiKey(v: String) { state.value = state.value.copy(apiKey = v) }
    fun setProtocol(v: Protocol) { state.value = state.value.copy(protocol = v) }

    fun save(onFinished: () -> Unit) {
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
                settings.setOnboardingDone()
            }.onSuccess {
                onFinished()
            }.onFailure { e ->
                state.value = state.value.copy(error = e.message ?: "保存失败")
            }
        }
    }
}
