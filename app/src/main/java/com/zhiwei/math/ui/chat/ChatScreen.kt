package com.zhiwei.math.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.zhiwei.math.data.db.MessageEntity
import com.zhiwei.math.data.prefs.ApiConfig
import com.zhiwei.math.data.prefs.SettingsStore
import com.zhiwei.math.glass.GlassHost
import com.zhiwei.math.glass.liquidGlass
import com.zhiwei.math.prompt.ChatMode
import com.zhiwei.math.ui.components.IosBackButton
import com.zhiwei.math.ui.components.IosIconButton
import com.zhiwei.math.ui.components.IosMenuSheet
import com.zhiwei.math.ui.components.IosPlainButton
import com.zhiwei.math.ui.components.MenuItem
import com.zhiwei.math.ui.icons.SfCamera
import com.zhiwei.math.ui.icons.SfDoc
import com.zhiwei.math.ui.icons.SfEllipsis
import com.zhiwei.math.ui.icons.SfPaperclip
import com.zhiwei.math.ui.icons.SfPaperplane
import com.zhiwei.math.ui.icons.SfPhoto
import com.zhiwei.math.ui.icons.SfStopCircle
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import com.zhiwei.math.util.DocExtractor
import com.zhiwei.math.util.ImageUtils
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

/**
 * 聊天页（GlassHost 兄弟布局铁律）：
 * - 内容层 = 聊天背景图 + 消息流（被 layerBackdrop 录制，玻璃可折射它）；
 * - overlay = 玻璃顶栏 + 底部玻璃输入胶囊（全部为内容层兄弟节点）。
 * 顶栏：返回 + 标题/副标题 + 模式胶囊 + ⋯ 菜单（action sheet）。
 */
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onOpenConvoSettings: () -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
    settings: SettingsStore = koinInject(),
) {
    val context = LocalContext.current
    val palette = LocalIosPalette.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val pendingImage by viewModel.pendingImage.collectAsStateWithLifecycle()
    val apiConfig by settings.apiConfig.collectAsState(initial = ApiConfig())
    val appearance by settings.appearance.collectAsState(
        initial = com.zhiwei.math.data.prefs.AppearanceSettings()
    )

    val listState = rememberLazyListState()
    // 自动滚动：仅当用户位于底部附近才跟随新内容（向上翻阅时不强制拉回）
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
    var modeMenuOpen by remember { mutableStateOf(false) }
    var attachMenuOpen by remember { mutableStateOf(false) }
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

    GlassHost(
        modifier = Modifier.fillMaxSize(),
        content = {
            // 底层：背景图 + 消息列表（内容层，被录制）
            Box(Modifier.fillMaxSize()) {
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
        },
        overlay = {
            // 浮层：错误提示条（顶栏下方，非玻璃）
            notice?.let { msg ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp,
                            start = 12.dp, end = 12.dp,
                        )
                        .clip(IosShapes.Control)
                        .background(
                            if (palette.isDark) Color(0xFF44201C) else Color(0xFFFFECEA),
                            IosShapes.Control,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.red,
                            modifier = Modifier.weight(1f),
                        )
                        IosPlainButton(text = "知道了", onClick = viewModel::dismissNotice, compact = true)
                    }
                }
            }

            // 玻璃顶栏（兄弟节点）：返回 + 标题 + 模式胶囊 + ⋯
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = com.kyant.shapes.Rectangle,
                        surfaceColor = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                    )
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IosBackButton(onClick = onBack)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            conversation?.title ?: "对话",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "高等数学 · 宋老师",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryLabel,
                            maxLines = 1,
                        )
                    }
                    // 模式切换胶囊（点开 action sheet）
                    val mode = ChatMode.entries.firstOrNull {
                        it.name == (conversation?.mode ?: "DETAIL")
                    } ?: ChatMode.DETAIL
                    androidx.compose.material3.TextButton(
                        onClick = { modeMenuOpen = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.blue,
                        )
                    }
                    IosIconButton(
                        icon = SfEllipsis,
                        contentDescription = "更多",
                        onClick = { topMenuOpen = true },
                    )
                }
            }

            // 底部输入区（玻璃胶囊，兄弟节点）
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
                        // 附件缩略图：确认图片确实已附加
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
                                    .clip(IosShapes.Small),
                            )
                        }
                        Text(
                            "已附加图片（长边 1280）",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.secondaryLabel,
                            modifier = Modifier.weight(1f),
                        )
                        IosPlainButton(text = "移除", onClick = viewModel::clearPendingImage, compact = true)
                    }
                }

                // 玻璃胶囊输入条
                ChatInputGlass(
                    draft = draft,
                    isStreaming = streamingText != null,
                    visionSupported = apiConfig.supportsVision,
                    onDraftChange = viewModel::updateDraft,
                    onSend = viewModel::send,
                    onStop = viewModel::stop,
                    onAttach = { attachMenuOpen = true },
                )
            }
        },
    )

    // ── 弹层们 ──
    if (modeMenuOpen) {
        IosMenuSheet(
            title = "切换模式（追加消息，不改历史）",
            items = ChatMode.entries.map { MenuItem(it.label) },
            onDismiss = { modeMenuOpen = false },
            onSelect = { label ->
                modeMenuOpen = false
                ChatMode.entries.firstOrNull { it.label == label }?.let(viewModel::switchMode)
            },
        )
    }
    if (topMenuOpen) {
        IosMenuSheet(
            title = conversation?.title,
            items = listOf(
                MenuItem("重命名对话"),
                MenuItem("对话设置（考试重点/老师风格…）"),
            ),
            onDismiss = { topMenuOpen = false },
            onSelect = { label ->
                topMenuOpen = false
                when (label) {
                    "重命名对话" -> showRename = true
                    else -> onOpenConvoSettings()
                }
            },
        )
    }
    if (attachMenuOpen) {
        IosMenuSheet(
            title = "插入附件",
            items = listOf(
                MenuItem("拍照", SfCamera),
                MenuItem("相册", SfPhoto),
                MenuItem("文档（txt/md/pdf/docx）", SfDoc),
            ),
            onDismiss = { attachMenuOpen = false },
            onSelect = { label ->
                attachMenuOpen = false
                when (label) {
                    "拍照" -> {
                        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                        cameraUri = FileProvider.getUriForFile(
                            context, context.packageName + ".fileprovider", file
                        )
                        cameraLauncher.launch(cameraUri!!)
                    }
                    "相册" -> galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    else -> docLauncher.launch(
                        arrayOf(
                            "text/plain", "text/markdown", "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        )
                    )
                }
            },
        )
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

/**
 * 输入玻璃胶囊：左侧附件圆钮（+ 打开拍照/相册/文档菜单）+ iOS 灰底文本框 +
 * 右侧发送 paperplane 蓝色圆钮（流式中变 stop 红色圆钮）。
 */
@Composable
private fun ChatInputGlass(
    draft: String,
    isStreaming: Boolean,
    visionSupported: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
) {
    val palette = LocalIosPalette.current
    Column(Modifier.fillMaxWidth()) {
        if (!visionSupported) {
            Text(
                "当前模型不支持视觉输入：图片将使用本地 OCR 识别（数学公式效果不佳）",
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(56.dp)
                .liquidGlass(
                    shape = IosShapes.Control,
                    surfaceColor = if (palette.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
                    innerShadowRadius = 6.dp,
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 附件圆钮（灰底 + paperclip，点开附件 action sheet）
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(palette.fill, CircleShape)
                    .clickable { onAttach() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    SfPaperclip,
                    contentDescription = "附件",
                    tint = palette.secondaryLabel,
                    modifier = Modifier.size(19.dp),
                )
            }
            // 灰底无边框文本框
            ChatInputTextField(
                draft = draft,
                onDraftChange = onDraftChange,
                modifier = Modifier.weight(1f),
            )
            // 发送（蓝色 paperplane）/ 停止（红色 stop）圆钮
            if (isStreaming) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(palette.fill, CircleShape)
                        .clickable { onStop() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        SfStopCircle,
                        contentDescription = "停止",
                        tint = palette.red,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (draft.isNotBlank()) palette.blue else palette.fill,
                            CircleShape,
                        )
                        .clickable { if (draft.isNotBlank()) onSend() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        SfPaperplane,
                        contentDescription = "发送",
                        tint = if (draft.isNotBlank()) Color.White else palette.tertiaryLabel,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** 聊天输入框：透明底 + placeholder（玻璃胶囊内） */
@Composable
private fun ChatInputTextField(
    draft: String,
    onDraftChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalIosPalette.current
    androidx.compose.foundation.text.BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        maxLines = 4,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.label),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.blue),
        decorationBox = { inner ->
            Box(
                modifier = modifier,
                contentAlignment = Alignment.CenterStart,
            ) {
                if (draft.isEmpty()) {
                    Text(
                        "问点什么…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.tertiaryLabel,
                    )
                }
                inner()
            }
        },
    )
}

private fun queryFileName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
}.getOrNull()
