package com.zhiwei.math.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.UnevenRoundedRectangle
import com.kyant.shapes.RectangleCornerRadii

/**
 * iOS 设计 token 单一来源（重设计方案）：
 * - 色板对齐 Apple HIG 系统色（#007AFF 蓝等）
 * - 形状全部走 shapes 库 G2 连续圆角（Continuous）
 * - Haptic 枚举供组件层统一调用
 */
object IosColors {
    // ── 系统蓝（primary）──
    val BlueLight = Color(0xFF007AFF)
    val BlueDark = Color(0xFF0A84FF)

    // ── 系统绿/红/橙/黄 ──
    val GreenLight = Color(0xFF34C759)
    val GreenDark = Color(0xFF30D158)
    val RedLight = Color(0xFFFF3B30)
    val RedDark = Color(0xFFFF453A)
    val OrangeLight = Color(0xFFFF9500)
    val OrangeDark = Color(0xFFFF9F0A)
    val YellowLight = Color(0xFFFFCC00)
    val YellowDark = Color(0xFFFFD60A)
    val TealLight = Color(0xFF30B0C7)
    val TealDark = Color(0xFF40C8E0)

    // ── 中性色（light）──
    val LightBackground = Color(0xFFF2F2F7)   // 分组背景
    val LightCard = Color(0xFFFFFFFF)          // 分组卡片
    val LightElevated = Color(0xFFFFFFFF)      // 弹层
    val LightLabel = Color(0xFF000000)
    val LightSecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.6f)
    val LightTertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.3f)
    val LightSeparator = Color(0xFF3C3C43).copy(alpha = 0.08f)
    val LightFill = Color(0xFF787880).copy(alpha = 0.12f)  // 输入框灰底/滑轨

    // ── 中性色（dark）──
    val DarkBackground = Color(0xFF000000)
    val DarkCard = Color(0xFF1C1C1E)
    val DarkElevated = Color(0xFF2C2C2E)
    val DarkLabel = Color(0xFFFFFFFF)
    val DarkSecondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.6f)
    val DarkTertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.3f)
    val DarkSeparator = Color(0xFFEBEBF5).copy(alpha = 0.08f)
    val DarkFill = Color(0xFF787880).copy(alpha = 0.24f)
}

/** 形状常量（全部 G2 连续圆角，见计划「形状」节） */
object IosShapes {
    /** 分组卡片 / 大卡片 */
    val Card = RoundedRectangle(16.dp, RoundedCornerStyle.Continuous)
    /** 按钮 / 输入框 */
    val Control = RoundedRectangle(14.dp, RoundedCornerStyle.Continuous)
    /** 弹窗 24dp */
    val Dialog = RoundedRectangle(24.dp, RoundedCornerStyle.Continuous)
    /** 小标签 / 缩略图 */
    val Small = RoundedRectangle(10.dp, RoundedCornerStyle.Continuous)
    /** 胶囊（dock、滑块、按钮圆角全高时用） */
    val Capsule = com.kyant.shapes.Capsule(RoundedCornerStyle.Continuous)
    /** 聊天气泡：用户 20dp + 右下 6dp；老师全 20dp */
    val BubbleUser = UnevenRoundedRectangle(
        RectangleCornerRadii(20.dp, 20.dp, 6.dp, 20.dp),
        RoundedCornerStyle.Continuous,
    )
    val BubbleTeacher = RoundedRectangle(20.dp, RoundedCornerStyle.Continuous)
    /** BottomSheet：顶 24 底 0 */
    val SheetTop = UnevenRoundedRectangle(
        RectangleCornerRadii(24.dp, 24.dp, 0.dp, 0.dp),
        RoundedCornerStyle.Continuous,
    )
    /** 卡片行内 29dp 图标圈 */
    val IconCircle = RoundedRectangle(8.dp, RoundedCornerStyle.Continuous)

    /** MaterialTheme.shapes 覆盖（旧代码 MaterialTheme.shapes 引用不炸，全走连续圆角） */
    val materialShapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )
}

/** 震动反馈分档（组件层经 LocalHapticsEnabled 开关后调用） */
enum class IosHaptic { Light, Medium, LongPress }
