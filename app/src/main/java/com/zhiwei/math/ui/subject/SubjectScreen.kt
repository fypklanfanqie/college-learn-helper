package com.zhiwei.math.ui.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiwei.math.data.repo.ChatRepository
import com.zhiwei.math.glass.pressableScale
import com.zhiwei.math.ui.components.CardGroup
import com.zhiwei.math.ui.components.CardRow
import com.zhiwei.math.ui.components.SectionHeader
import com.zhiwei.math.ui.icons.SfChevronRight
import com.zhiwei.math.ui.icons.SfFunction
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 主页（iOS 观感）：LargeTitle 由 MainScaffold overlay 头提供；
 * 屏幕内容 = 科目大卡片（60dp 圆角图标区 + chevron）+ 最近对话分组列表。
 * V1 仅高等数学可用，线代/概率论"即将上线"置灰不可点。
 */
@Composable
fun SubjectScreen(
    onStartMath: () -> Unit,
    onOpenChat: (Long) -> Unit = {},
    onCollapsedChanged: (Boolean) -> Unit = {},
) {
    val palette = LocalIosPalette.current
    val repo = koinInject<ChatRepository>()
    val recent by repo.observeConversations().collectAsState(initial = emptyList())
    val scroll = rememberScrollState()

    // 滚动驱动大标题折叠（overlay 头收缩 + 玻璃底衬浮现）
    LaunchedEffect(scroll.isScrollInProgress, scroll.value) {
        onCollapsedChanged(scroll.value > 4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .verticalScroll(scroll),
    ) {
        Spacer(Modifier.statusBarsPadding().height(96.dp))

        // ── 科目大卡片 ──
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SubjectBigCard(
                title = "高等数学",
                subtitle = "同济版 · 宋浩风格精讲 / 期末冲刺",
                iconBg = Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5AC8FA))),
                enabled = true,
                onClick = onStartMath,
            )
            SubjectBigCard(
                title = "线性代数",
                subtitle = "即将上线",
                iconBg = Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFFC7C7CC))),
                enabled = false,
                onClick = {},
            )
            SubjectBigCard(
                title = "概率论与数理统计",
                subtitle = "即将上线",
                iconBg = Brush.linearGradient(listOf(Color(0xFF8E8E93), Color(0xFFC7C7CC))),
                enabled = false,
                onClick = {},
            )
        }

        // ── 最近对话 ──
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionHeader("最近对话")
            CardGroup(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 132.dp),
            ) {
                val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
                recent.take(3).forEachIndexed { index, convo ->
                    CardRow(
                        title = convo.title,
                        subtitle = fmt.format(Date(convo.updatedAt)),
                        showChevron = true,
                        showDivider = index < (recent.take(3).size - 1),
                        onClick = { onOpenChat(convo.id) },
                    )
                }
            }
        } else {
            Spacer(Modifier.height(132.dp))
        }
    }
}

/** 科目大卡片：60dp 渐变圆角图标区 + 标题/副标题 + chevron；不可用置灰 */
@Composable
private fun SubjectBigCard(
    title: String,
    subtitle: String,
    iconBg: Brush,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalIosPalette.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, IosShapes.Card)
            .then(
                if (enabled) Modifier
                    .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                    .pressableScale(interaction)
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 60dp 图标区（渐变底 + SF function 图标）
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(IosShapes.Control)
                .background(if (enabled) iconBg else Brush.linearGradient(listOf(palette.fill, palette.fill))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SfFunction,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) palette.label else palette.secondaryLabel,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) palette.secondaryLabel else palette.tertiaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (enabled) {
            Icon(
                imageVector = SfChevronRight,
                contentDescription = null,
                tint = palette.tertiaryLabel,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
