package com.zhiwei.math.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhiwei.math.glass.GlassHost
import com.zhiwei.math.ui.components.CardGroup
import com.zhiwei.math.ui.components.IosAlertDialog
import com.zhiwei.math.ui.components.IosNavBar
import com.zhiwei.math.ui.components.IosTextField
import com.zhiwei.math.ui.theme.IosShapes
import com.zhiwei.math.ui.theme.LocalIosPalette

/**
 * 内置适应教程（PROJECT-BRIEF.md 7.4-11）：分类（8 类）+ 全文搜索 + 卡片详情。
 * iOS 观感：玻璃顶栏 + 灰底搜索框 + 卡片列表 + iOS 弹窗详情。
 */
data class TutorialItem(
    val category: String,
    val title: String,
    val body: String,
)

object TutorialContent {
    val CATEGORIES = listOf("快速上手", "API 配置", "对话", "练题", "例题本", "划重点", "学习报告", "玻璃效果")

    val ITEMS = listOf(
        TutorialItem(
            "快速上手", "第一次用，从这里开始",
            "1. 打开 App，选择「高等数学」开始学习。\n" +
                "2. 第一次使用需要接入你自己的 API（BYOK）：填入模型商、模型 ID 和 API Key。\n" +
                "3. 进入对话，直接问问题——宋老师会用讲课的方式回答你。\n" +
                "4. 回答下方有四个按钮：删除 / 追问 / 详细解答 / 出例题。\n" +
                "5. 顶部胶囊可随时切换「精讲」和「期末冲刺」模式。",
        ),
        TutorialItem(
            "快速上手", "精讲 vs 期末冲刺",
            "精讲模式：完整教学流程——生活化类比 → 严格定义 → 公式推导 → 例题挖坑 → 小结划重点，允许小段子，节奏舒缓。\n\n" +
                "期末冲刺模式：直奔考点（\"这个考/这个不考\"），例题贴真题题型，强调答题书写顺序与判卷给分点，省略推导（\"这个直接记\"），结尾给考前提醒。\n\n" +
                "切换模式不会重置对话，也不会浪费缓存。",
        ),
        TutorialItem(
            "API 配置", "接入你的 API（BYOK）",
            "设置 → API 配置：\n" +
                "1. 选择模型商卡片（DeepSeek / 智谱 / Kimi / 通义 / SiliconFlow / OpenRouter / Gemini / OpenAI / Anthropic），或选「自定义」。\n" +
                "2. Base URL 与模型 ID 会自动预填，可修改；协议分 OpenAI / Anthropic 两种。\n" +
                "3. API Key 加密保存在你的手机里，不会上传。\n" +
                "4. 带 👁 标记的模型支持视觉输入：拍照题目可直接识别。不支持视觉的模型会用本地 OCR 读图（数学公式效果不佳）。\n" +
                "5. 最好使用支持视觉输入的 API。",
        ),
        TutorialItem(
            "对话", "追问与选中追问",
            "对某次回答不满意或想深入？\n" +
                "1. 点回答下方的「追问」，会引用整段回答让你补充问题。\n" +
                "2. 长按老师的气泡：复制全文 / 针对这段追问 / 划重点。\n" +
                "3. 「详细解答」会让老师把当前内容完整推导一遍，不跳步。\n\n" +
                "追问以追加消息的形式进入历史，不会破坏已有的对话缓存。",
        ),
        TutorialItem(
            "对话", "图片与文档上传",
            "输入栏左侧附件按钮（回形针）：\n" +
                "📷 拍照：拍题目直接问；🖼 相册：选已有图片；📄 文档：txt/md 直传，docx/pdf 自动提取文字。\n" +
                "图片会自动压缩到长边 1280 再上传（省 token）。\n" +
                "模型不支持视觉时：图片走本地 OCR；OCR 也读不出来会提示「OCR 暂时不可用」。",
        ),
        TutorialItem(
            "对话", "对话管理",
            "对话列表：左滑卡片可直接删除；点卡片右侧 ⋯ 可重命名。\n" +
                "右上角「+」随时开新线程。\n" +
                "对话右上角 ⋯ → 重命名 / 对话设置。",
        ),
        TutorialItem(
            "对话", "对话设置（考试重点/老师风格/我的水平）",
            "对话右上角 ⋯ → 对话设置，有四项：\n" +
                "1. 考试重点：老师划的重点，讲相关章节时优先覆盖。\n" +
                "2. 非考点：不需要掌握的内容，老师会明确说\"不考\"。\n" +
                "3. 老师风格（个性）：你自己老师的个性习惯，叠加在宋浩讲课风格之上（两层互不冲突）。\n" +
                "4. 我的水平：哪里不会，老师会主动补齐断档。\n\n" +
                "保存时会调用 LLM 把这四项改写成一段全新的「定制提示词」，而不是机械填充；之后每次对话都会带上。",
        ),
        TutorialItem(
            "练题", "出题 → 作答 → 批改",
            "AI练 → 练题：\n" +
                "1. 输入主题（如\"格林公式\"）和难度（基础/进阶/挑战），点出题。\n" +
                "2. 看到题目后作答（打字或拍照）。\n" +
                "3. 提交后老师逐步批改：判定 → 点评 → 详细解析 → 得分。\n" +
                "4. 「下一题」继续；练习记录可随时回看。",
        ),
        TutorialItem(
            "例题本", "把例题攒起来",
            "聊天里点「出例题」，老师会输出一张例题卡片（题目/解答/考点标签）。\n" +
                "卡片下点「加入例题本」收藏。例题本里可回看、左滑删除、跳回来源对话。",
        ),
        TutorialItem(
            "划重点", "把重点记在手机上",
            "长按老师的回答 → 「划重点」，老师会把这段内容提炼成要点列表（必考★/理解即可/不用掌握），\n" +
                "并自动保存到划重点本，按时间线管理，左滑删除。",
        ),
        TutorialItem(
            "学习报告", "一次对话学得怎么样？",
            "学习报告页签：选一个对话，AI 根据你们的问答生成报告：\n" +
                "学习内容 / 掌握情况 / 薄弱点与易错点 / 复习建议。\n" +
                "可导出为长图（分享）或 Word 文档（Downloads 目录）。",
        ),
        TutorialItem(
            "玻璃效果", "毛玻璃 / 液态玻璃",
            "设置 → 玻璃效果：\n" +
                "毛玻璃（默认）：背景模糊，Android 12+，四滑杆（模糊/不透明度/色温/顶部高光）。\n" +
                "液态玻璃：backdrop 库真实折射 + 色散 + 连续曲率圆角，Android 13+，10 个滑杆全可调，四个预设（iOS 原版/清透/浓郁/夸张），预览卡实时生效。\n" +
                "低版本自动降级；真机若渲染崩溃会自动降级毛玻璃并提示。\n" +
                "玻璃效果以真机效果为准。",
        ),
        TutorialItem(
            "玻璃效果", "聊天背景与字体",
            "设置 → 外观：聊天背景可选相册图片；主题支持浅色/深色/跟随系统；字体支持 MiSans（默认，免费商用）与系统字体切换。",
        ),
    )
}

@Composable
fun TutorialScreen(onBack: () -> Unit) {
    val palette = LocalIosPalette.current
    var query by remember { mutableStateOf("") }
    var openItem by remember { mutableStateOf<TutorialItem?>(null) }

    val filtered = remember(query) {
        val q = query.trim()
        if (q.isEmpty()) TutorialContent.ITEMS
        else TutorialContent.ITEMS.filter {
            it.title.contains(q) || it.body.contains(q) || it.category.contains(q)
        }
    }

    GlassHost(
        modifier = Modifier.fillMaxSize(),
        content = {
            Column(Modifier.fillMaxSize().background(palette.background)) {
                Spacer(Modifier.height(100.dp))
                IosTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "搜索功能关键词…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 32.dp,
                    ),
                ) {
                    items(filtered, key = { it.title + it.category }) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.card, IosShapes.Card)
                                .clickable { openItem = item }
                                .padding(14.dp),
                        ) {
                            Text(
                                "【${item.category}】${item.title}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.label,
                            )
                            Text(
                                item.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryLabel,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        overlay = {
            IosNavBar(
                title = "使用教程",
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    )

    openItem?.let { item ->
        IosAlertDialog(
            onDismiss = { openItem = null },
            title = "【${item.category}】${item.title}",
            message = item.body,
            confirmText = "知道了",
            onConfirm = { openItem = null },
        )
    }
}
