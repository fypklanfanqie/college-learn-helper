package com.zhiwei.math.ui.chatlist

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhiwei.math.data.db.ConversationEntity
import com.zhiwei.math.ui.chat.SimpleTextDialog
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 对话列表：新建 / 重命名 / 删除（长按菜单，iOS 风格） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    onOpenChat: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ChatListViewModel = koinViewModel(),
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameFor by remember { mutableStateOf<ConversationEntity?>(null) }
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知微数学") },
                actions = {
                    TextButton(onClick = onOpenSettings) { Text("设置") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.create(onCreated = onOpenChat) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("新的对话") },
            )
        },
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "还没有对话。\n点右下角开始你的第一节高数课！",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(conversations, key = { it.id }) { convo ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onOpenChat(convo.id) },
                                onLongClick = { menuFor = convo },
                            ),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(convo.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${dateFormat.format(Date(convo.updatedAt))} · ${modeLabel(convo.mode)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        menuFor?.let { convo ->
            DropdownMenu(expanded = true, onDismissRequest = { menuFor = null }) {
                DropdownMenuItem(
                    text = { Row { Icon(Icons.Filled.Edit, null); Text("  重命名") } },
                    onClick = {
                        menuFor = null
                        renameFor = convo
                    },
                )
                DropdownMenuItem(
                    text = { Row { Icon(Icons.Filled.Delete, null); Text("  删除") } },
                    onClick = {
                        menuFor = null
                        viewModel.delete(convo.id)
                    },
                )
            }
        }

        renameFor?.let { convo ->
            SimpleTextDialog(
                title = "重命名对话",
                initial = convo.title,
                onDismiss = { renameFor = null },
                onConfirm = { title ->
                    viewModel.rename(convo.id, title)
                    renameFor = null
                },
            )
        }
    }
}

private fun modeLabel(mode: String) = if (mode == "EXAM_SPRINT") "期末冲刺" else "精讲"
