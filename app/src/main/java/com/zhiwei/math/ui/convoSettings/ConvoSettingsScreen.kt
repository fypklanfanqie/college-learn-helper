package com.zhiwei.math.ui.convoSettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.llm.LlmRepository
import com.zhiwei.math.ui.chat.SimpleTextDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * 对话设置（聊天页右上角二级入口）：
 * 考试重点 / 非考点 / 老师风格（个性）/ 我的水平。
 * 保存时调用 LLM 生成全新定制提示词段（Layer 2），字节级存库；
 * 失败可重试或用模板兜底（不机械填充用户原文）。
 */
@Composable
fun ConvoSettingsScreen(
    onBack: () -> Unit,
    viewModel: ConvoSettingsViewModel = org.koin.androidx.compose.koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                title = { Text("对话设置") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "这四项会与宋老师的讲课风格叠加（个性是独立的一层，不冲突）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.examFocus,
                onValueChange = viewModel::setExamFocus,
                label = { Text("考试重点") },
                placeholder = { Text("老师划的重点，如：第三章 中值定理、第十二章 幂级数求和…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.nonExamPoints,
                onValueChange = viewModel::setNonExamPoints,
                label = { Text("非考点") },
                placeholder = { Text("不需要掌握的内容，如：曲率、傅里叶级数…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.teacherPersona,
                onValueChange = viewModel::setTeacherPersona,
                label = { Text("老师风格（个性）") },
                placeholder = { Text("你自己老师的说话习惯/要求，如：喜欢板书推导、口头禅…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.myLevel,
                onValueChange = viewModel::setMyLevel,
                label = { Text("我的水平") },
                placeholder = { Text("哪里不会：高中导数基础薄弱、积分换元不熟…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (state.saved) {
                Text("已保存，定制提示词已生效", color = MaterialTheme.colorScheme.primary)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.save(onDone = onBack) }, enabled = !state.saving) {
                    if (state.saving) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(16.dp))
                    }
                    Text(if (state.saving) "生成定制提示词中…" else "保存")
                }
                TextButton(onClick = onBack) { Text("取消") }
            }

            if (state.layer2Preview.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("当前定制提示词段（Layer 2）", style = MaterialTheme.typography.labelMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            state.layer2Preview,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 8,
                        )
                    }
                }
            }
        }
    }
}

data class ConvoSettingsUiState(
    val loaded: Boolean = false,
    val examFocus: String = "",
    val nonExamPoints: String = "",
    val teacherPersona: String = "",
    val myLevel: String = "",
    val layer2Preview: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class ConvoSettingsViewModel(
    private val repo: ChatRepository,
    private val llm: LlmRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val convoId: Long = savedStateHandle.get<String>("convoId")?.toLongOrNull()
        ?: savedStateHandle.get<Long>("convoId") ?: -1L

    val state = MutableStateFlow(ConvoSettingsUiState())

    fun load() {
        viewModelScope.launch {
            repo.getConversation(convoId)?.let { c ->
                state.value = state.value.copy(
                    loaded = true,
                    examFocus = c.examFocus,
                    nonExamPoints = c.nonExamPoints,
                    teacherPersona = c.teacherPersona,
                    myLevel = c.myLevel,
                    layer2Preview = c.layer2Prompt,
                )
            }
        }
    }

    fun setExamFocus(v: String) { state.value = state.value.copy(examFocus = v, saved = false) }
    fun setNonExamPoints(v: String) { state.value = state.value.copy(nonExamPoints = v, saved = false) }
    fun setTeacherPersona(v: String) { state.value = state.value.copy(teacherPersona = v, saved = false) }
    fun setMyLevel(v: String) { state.value = state.value.copy(myLevel = v, saved = false) }

    fun save(onDone: () -> Unit) {
        val s = state.value
        state.value = s.copy(saving = true, error = null)
        viewModelScope.launch {
            // 先调 LLM 生成全新定制段；失败 → 重试一次 → 再失败用模板兜底
            val generated = runCatching { llm.generateLayer2(s.examFocus, s.nonExamPoints, s.teacherPersona, s.myLevel) }
                .recoverCatching {
                    llm.generateLayer2(s.examFocus, s.nonExamPoints, s.teacherPersona, s.myLevel)
                }
                .getOrElse { fallbackLayer2(s.examFocus, s.nonExamPoints, s.teacherPersona, s.myLevel) }
            repo.saveConvoSettings(
                convoId = convoId,
                layer2 = generated,
                examFocus = s.examFocus,
                nonExam = s.nonExamPoints,
                persona = s.teacherPersona,
                level = s.myLevel,
            )
            state.value = state.value.copy(saving = false, saved = true, layer2Preview = generated)
            onDone()
        }
    }

    /** 模板兜底：LLM 不可用时不机械拼接用户原文为"指令"——由固定模板改写 */
    private fun fallbackLayer2(examFocus: String, nonExam: String, persona: String, level: String): String =
        buildString {
            if (examFocus.isNotBlank()) appendLine("【考试重点】讲到相关章节时优先覆盖：${examFocus.trim()}，明确标注必考并多配例题。")
            if (nonExam.isNotBlank()) appendLine("【非考点】以下内容一句话带过并明确说明不考：${nonExam.trim()}。")
            if (persona.isNotBlank()) appendLine("【老师个性】在保持宋老师讲课方法的同时，融入这些个性特征：${persona.trim()}。")
            if (level.isNotBlank()) appendLine("【学生水平】学生自述：${level.trim()}。讲解时主动补齐断档，避免跳步。")
        }.trim().ifBlank { "" }
}
