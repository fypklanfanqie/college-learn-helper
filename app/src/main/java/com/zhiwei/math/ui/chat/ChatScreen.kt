package com.zhiwei.math.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zhiwei.math.data.db.MessageEntity
import com.zhiwei.math.data.prefs.ApiConfig
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.GlassSurface
import com.zhiwei.math.prompt.ChatMode
import com.zhiwei.math.util.DocExtractor
import com.zhiwei.math.util.ImageUtils
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

/**
 * 聊天页（核心屏幕）：顶栏（返回/标题/模式切换/⋯）、消息流（流式）、输入区（文本+图片+文档）。
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onOpenConvoSettings: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
    settings: SettingsStore = koinInject(),
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val pendingImage by viewModel.pendingImage.collectAsStateWithLifecycle()
    val apiConfig by settings.apiConfig.collectAsState(initial = ApiConfig())
    val appearance by settings.appearance.collectAsState(initial = com.zhiwei.math.data.prefs.AppearanceSettings())

    val listState = rememberLazyListState()
    // 自动滚动修复：仅当用户位于列表底部附近时才跟随新内容滚动；
    // 用户向上翻阅历史时不再被强制拉回底部。
    val nearBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= info.totalItemsCount - 2
        }
    }
    LaunchedEffect(messages.size, streamingText) {
        if ((messages.isNotEmpty() || streamingText != null) && nearBottom) {
            listState.animateScrollToItem((messages.size.coerceAtLeast(1)) - 1)
        }
    }

    var showFollowUpFor by remember { mutableStateOf<String?>(null) }
    var topMenuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    // 拍照：FileProvider 缓存 Uri
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) {
            ImageUtils.loadScaledBitmap(context, uri)?.let { bmp ->
                viewModel.pendingBitmap = bmp
                viewModel.onImagePicked(
                    ImageUtils.toJpegBase64(bmp),
                    "image/jpeg",
                    apiConfig.supportsVision,
                )
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            ImageUtils.loadScaledBitmap(context, uri)?.let { bmp ->
                viewModel.pendingBitmap = bmp
                viewModel.onImagePicked(
                    ImageUtils.toJpegBase64(bmp),
                    "image/jpeg",
                    apiConfig.supportsVision,
                )
            }
        }
    }
    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = queryFileName(context, uri) ?: "document"
            val result = DocExtractor.extract(context, uri, name)
            when {
                result.text == null -> viewModel.sendDocumentNotice("「${result.fileName}」是扫描版/图片型文档，文字提取失败，建议直接拍照发送。")
                else -> viewModel.sendDocument(result.fileName, result.text)
            }
        }
    }

    // 悬浮覆盖布局（参照 Cresto）：内容铺满全屏，玻璃顶栏/输入条浮在内容上方，
    // 消息从玻璃面板下方滚过 —— 液态玻璃才有东西可折射。
    Box(Modifier.fillMaxSize()) {
        // 底层：背景图 + 消息列表
        Box(Modifier.fillMaxSize()) {
            // 聊天背景图（设置 → 外观 → 聊天背景）
            appearance.chatBackgroundUri.takeIf { it.isNotBlank() }?.let { bgUri ->
                val bgBmp = remember(bgUri) { ImageUtils.loadScaledBitmap(context, Uri.parse(bgUri)) }
                bgBmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 76.dp,
                    bottom = 140.dp,
                ),
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        actionsEnabled = streamingText == null,
                        onFollowUp = { quoted -> showFollowUpFor = quoted },
                        onDetailedSolution = viewModel::detailedSolution,
                        onHighlight = viewModel::highlight,
                        onAskExample = viewModel::askExample,
                        onDelete = viewModel::deleteMessage,
                        onAddExample = viewModel::addExampleToBook,
                        addedExample = false,
                    )
                }
                streamingText?.let { st ->
                    item(key = "streaming") {
                        MessageBubble(
                            message = MessageEntity(
                                conversationId = viewModel.convoId,
                                role = "assistant",
                                content = st,
                                createdAt = 0,
                            ),
                            isStreamingPlaceholder = true,
                            onFollowUp = {}, onDetailedSolution = {}, onHighlight = {},
                            onAskExample = {}, onDelete = {}, onAddExample = {},
                            addedExample = false,
                        )
                    }
                }
            }
        }

        // 浮层：错误提示条（顶栏下方）
        notice?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 66.dp),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::dismissNotice) { Text("知道了") }
                }
            }
        }

        // 浮层：玻璃顶栏（毛玻璃默认 / 液态玻璃 / 半透明降级）
        GlassSurface(Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Column {
                        Text(conversation?.title ?: "对话", style = MaterialTheme.typography.titleMedium)
                        Text("高等数学 · 宋老师", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    ModeSwitchChip(
                        current = conversation?.mode ?: "DETAIL",
                        onSwitch = viewModel::switchMode,
                    )
                    IconButton(onClick = { topMenuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = topMenuOpen, onDismissRequest = { topMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("重命名对话") },
                            onClick = { topMenuOpen = false; showRename = true },
                        )
                        DropdownMenuItem(
                            text = { Text("对话设置（考试重点/老师风格…）") },
                            onClick = { topMenuOpen = false; onOpenConvoSettings() },
                        )
                    }
                },
            )
        }

        // 浮层：底部输入区（safeDrawing Bottom 同时覆盖导航栏与 IME，取最大值）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 12.dp),
        ) {
            pendingImage?.let { (b64, _) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 附件缩略图：让用户确认图片确实已附加
                    val previewBmp = remember(b64) {
                        runCatching {
                            val raw = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
                            android.graphics.BitmapFactory.decodeByteArray(raw, 0, raw.size)
                        }.getOrNull()
                    }
                    previewBmp?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "待发送图片",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(44.dp)
                                .width(44.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    Text("已附加图片（长边 1280）", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::clearPendingImage) { Text("移除") }
                }
            }

            GlassSurface(
                cornerRadius = 24.dp,
            ) {
                ChatInputBar(
                    draft = draft,
                    isStreaming = streamingText != null,
                    visionSupported = apiConfig.supportsVision,
                    onDraftChange = viewModel::updateDraft,
                    onSend = viewModel::send,
                    onStop = viewModel::stop,
                    onCamera = {
                        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                        cameraUri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                        cameraLauncher.launch(cameraUri!!)
                    },
                    onGallery = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onDocument = {
                        docLauncher.launch(arrayOf("text/plain", "text/markdown", "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    },
                )
            }
        }
    }

    showFollowUpFor?.let { quoted ->
        FollowUpDialog(
            quotedText = quoted,
            onDismiss = { showFollowUpFor = null },
            onConfirm = { question ->
                showFollowUpFor = null
                viewModel.followUp(quoted, question)
            },
        )
    }

    if (showRename) {
        SimpleTextDialog(
            title = "重命名对话",
            initial = conversation?.title ?: "",
            onDismiss = { showRename = false },
            onConfirm = { title ->
                viewModel.rename(title)
                showRename = false
            },
        )
    }
}

/** 输入栏：文本框 + 相机/相册/文档 + 发送/停止 */
@Composable
private fun ChatInputBar(
    draft: String,
    isStreaming: Boolean,
    visionSupported: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDocument: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (!visionSupported) {
            Text(
                "当前模型不支持视觉输入：图片将使用本地 OCR 识别（数学公式效果不佳）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCamera) { Icon(Icons.Filled.PhotoCamera, "拍照") }
            IconButton(onClick = onGallery) { Icon(Icons.Filled.PhotoLibrary, "相册") }
            IconButton(onClick = onDocument) { Icon(Icons.Filled.AttachFile, "文档") }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("问点什么…") },
                maxLines = 4,
            )
            if (isStreaming) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, "停止", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(Icons.Filled.Send, "发送")
                }
            }
        }
    }
}

/** 模式切换胶囊（精讲/期末冲刺），切换 = 追加消息不改 system */
@Composable
private fun ModeSwitchChip(current: String, onSwitch: (ChatMode) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val mode = ChatMode.entries.firstOrNull { it.name == current } ?: ChatMode.DETAIL
    Box {
        FilterChip(
            selected = true,
            onClick = { open = true },
            label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ChatMode.entries.forEach { m ->
                DropdownMenuItem(
                    text = { Text(if (m == mode) "✓ ${m.label}" else m.label) },
                    onClick = {
                        open = false
                        onSwitch(m)
                    },
                )
            }
        }
    }
}

@Composable
fun SimpleTextDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun queryFileName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}.getOrNull()
