package com.zhiwei.math.ui.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhiwei.math.data.db.ConversationEntity
import com.zhiwei.math.glass.GlassHost
import com.zhiwei.math.ui.chat.SimpleTextDialog
import com.zhiwei.math.ui.components.EmptyHint
import com.zhiwei.math.ui.components.IosIconButton
import com.zhiwei.math.ui.components.IosLargeTitleHeader
import com.zhiwei.math.ui.components.IosMenuSheet
import com.zhiwei.math.ui.components.MenuItem
import com.zhiwei.math.ui.components.SwipeToDelete
import com.zhiwei.math.ui.icons.SfPlus
import com.zhiwei.math.ui.icons.SfSquareAndPencil
import com.zhiwei.math.ui.icons.SfTrash
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 对话列表（iOS 观感）：LargeTitle"对话" + iOS 列表（SwipeToDelete 左滑删除；
 * ⋯ 菜单保留重命名）+ 右上 plus 新建（替代 FAB）。
 * 玻璃顶衬与列表内容互为兄弟（GlassHost）。
 */
@Composable
fun ChatListScreen(
    onOpenChat: (Long) -> Unit,
    viewModel: ChatListViewModel = koinViewModel(),
) {
    val palette = LocalIosPalette.current
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<ConversationEntity?>(null) }
    var renameFor by remember { mutableStateOf<ConversationEntity?>(null) }
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val listState = rememberLazyListState()
    val collapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    GlassHost(
        modifier = Modifier.fillMaxSize(),
        content = {
            Box(Modifier.fillMaxSize().background(palette.background)) {
                if (conversations.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.statusBarsPadding().padding(top = 96.dp)) {
                            EmptyHint(
                                title = "还没有对话",
                                subtitle = "点右上角「+」开始你的第一节高数课",
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, bottom = 32.dp,
                        ),
                    ) {
                        item { Spacer(Modifier.statusBarsPadding().height(96.dp)) }
                        items(conversations, key = { it.id }) { convo ->
                            SwipeToDelete(
                                onDelete = { viewModel.delete(convo.id) },
                                onClick = { onOpenChat(convo.id) },
                                modifier = Modifier.padding(vertical = 6.dp),
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(palette.card, IosShapes.Card)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                convo.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = palette.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "${dateFormat.format(Date(convo.updatedAt))} · ${modeLabel(convo.mode)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.secondaryLabel,
                                            )
                                        }
                                        // ⋯ 可见入口（点击弹 iOS action sheet）
                                        IosIconButton(
                                            icon = com.zhiwei.math.ui.icons.SfEllipsis,
                                            contentDescription = "更多操作",
                                            onClick = { menuFor = convo },
                                            size = 36,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        overlay = {
            IosLargeTitleHeader(
                title = "对话",
                collapsed = collapsed,
                modifier = Modifier.align(Alignment.TopCenter),
                trailingActions = {
                    IosIconButton(
                        icon = SfPlus,
                        contentDescription = "新的对话",
                        onClick = { viewModel.create(onCreated = onOpenChat) },
                    )
                },
                collapsedActions = {
                    IosIconButton(
                        icon = SfPlus,
                        contentDescription = "新的对话",
                        onClick = { viewModel.create(onCreated = onOpenChat) },
                        size = 36,
                    )
                },
            )
        },
    )

    // ⋯ 菜单（iOS action sheet 式）
    menuFor?.let { convo ->
        IosMenuSheet(
            title = convo.title,
            items = listOf(
                MenuItem("重命名", SfSquareAndPencil),
                MenuItem("删除", SfTrash, destructive = true),
            ),
            onDismiss = { menuFor = null },
            onSelect = { label ->
                menuFor = null
                when (label) {
                    "重命名" -> renameFor = convo
                    "删除" -> viewModel.delete(convo.id)
                }
            },
        )
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

private fun modeLabel(mode: String) = if (mode == "EXAM_SPRINT") "期末冲刺" else "精讲"
