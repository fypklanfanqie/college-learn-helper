package com.zhiwei.math.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * SF Symbols 风格细线图标全集（自绘，替换全部 Material Icons）：
 * - 24dp 网格，SF 视觉比例（图标主体 ~18dp 居中）
 * - 1.8dp 描边、圆头端点/圆角连接（SF 的 rounded 风格）
 * - 全部手写 path，不依赖 material-icons
 */

/** 统一描边参数 */
private const val SF_STROKE = 1.8f
private val SF_CAP = StrokeCap.Round
private val SF_JOIN = StrokeJoin.Round

/** 统一构造：24dp 画布 + 1.8dp 圆头描边（by lazy 自带缓存） */
private fun lineIcon(
    name: String,
    autoMirror: Boolean = false,
    builder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
    autoMirror = autoMirror,
).apply {
    path(
        name = "$name.line",
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = SF_STROKE,
        strokeLineCap = SF_CAP,
        strokeLineJoin = SF_JOIN,
    ) { builder() }
}.build()

/** 点图标（圆点用零长线段 + 圆头描边渲染） */
private fun dotsIcon(name: String, vararg positions: Pair<Float, Float>): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            positions.forEach { (x, y) -> moveTo(x, y); lineTo(x + 0.01f, y) }
        }
    }.build()

// ───────────────────────── 导航 ─────────────────────────

/** chevron.backward：返回（iOS 蓝字返回按钮的箭头） */
val SfChevronBackward: ImageVector by lazy { lineIcon("chevron.backward", autoMirror = true) {
    moveTo(15f, 4.5f); lineTo(7.5f, 12f); lineTo(15f, 19.5f)
} }

/** chevron.right：列表行尾箭头 */
val SfChevronRight: ImageVector by lazy { lineIcon("chevron.right", autoMirror = true) {
    moveTo(9f, 4.5f); lineTo(16.5f, 12f); lineTo(9f, 19.5f)
} }

/** chevron.down：展开指示 */
val SfChevronDown: ImageVector by lazy { lineIcon("chevron.down") {
    moveTo(4.5f, 9f); lineTo(12f, 16.5f); lineTo(19.5f, 9f)
} }

/** chevron.up：收起指示 */
val SfChevronUp: ImageVector by lazy { lineIcon("chevron.up") {
    moveTo(4.5f, 15f); lineTo(12f, 7.5f); lineTo(19.5f, 15f)
} }

// ───────────────────────── 页签 ─────────────────────────

/** house：主页 */
val SfHouse: ImageVector by lazy { lineIcon("house") {
    moveTo(3.5f, 10.5f)
    lineTo(12f, 3.5f)
    lineTo(20.5f, 10.5f)
    lineTo(20.5f, 19.5f)
    curveTo(20.5f, 20.05f, 20.05f, 20.5f, 19.5f, 20.5f)
    lineTo(15f, 20.5f)
    lineTo(15f, 14.5f)
    lineTo(9f, 14.5f)
    lineTo(9f, 20.5f)
    lineTo(4.5f, 20.5f)
    curveTo(3.95f, 20.5f, 3.5f, 20.05f, 3.5f, 19.5f)
    close()
} }

/** pencil.tip：AI 练（铅笔带笔尖连线，SF pencil.tip 风格） */
val SfPencilTip: ImageVector by lazy { lineIcon("pencil.tip") {
    // 铅笔主体（左上斜置）
    moveTo(4f, 16.5f)
    lineTo(4f, 20f)
    lineTo(7.5f, 20f)
    lineTo(17.5f, 10f)
    lineTo(14f, 6.5f)
    lineTo(4f, 16.5f)
    close()
    // 笔尖分线
    moveTo(14f, 6.5f)
    lineTo(17.5f, 10f)
    // 笔尖下的笔画线（pencil.tip 特征）
    moveTo(4f, 20f)
    lineTo(20f, 20f)
} }

/** bookmark：学习重点 */
val SfBookmark: ImageVector by lazy { lineIcon("bookmark") {
    moveTo(6f, 3.5f)
    lineTo(18f, 3.5f)
    curveTo(18.83f, 3.5f, 19.5f, 4.17f, 19.5f, 5f)
    lineTo(19.5f, 20.5f)
    lineTo(12f, 16f)
    lineTo(4.5f, 20.5f)
    lineTo(4.5f, 5f)
    curveTo(4.5f, 4.17f, 5.17f, 3.5f, 6f, 3.5f)
    close()
} }

/** chart.bar：学习报告 */
val SfChartBar: ImageVector by lazy { lineIcon("chart.bar") {
    moveTo(5f, 20.5f); lineTo(5f, 13f)
    moveTo(12f, 20.5f); lineTo(12f, 5f)
    moveTo(19f, 20.5f); lineTo(19f, 9.5f)
    moveTo(3f, 20.5f); lineTo(21f, 20.5f)
} }

/** gearshape：设置 */
val SfGearshape: ImageVector by lazy { lineIcon("gearshape") {
    moveTo(12f, 2.5f)
    curveTo(12.9f, 2.5f, 13.65f, 3.1f, 13.85f, 3.95f)
    lineTo(14.05f, 4.85f)
    lineTo(14.9f, 5.2f)
    lineTo(15.7f, 4.7f)
    curveTo(16.45f, 4.25f, 17.4f, 4.4f, 17.95f, 5.05f)
    lineTo(18.95f, 6.05f)
    curveTo(19.6f, 6.7f, 19.7f, 7.7f, 19.2f, 8.45f)
    lineTo(18.75f, 9.2f)
    lineTo(19.05f, 10f)
    lineTo(20f, 10.15f)
    curveTo(20.85f, 10.35f, 21.5f, 11.1f, 21.5f, 12f)
    curveTo(21.5f, 12.9f, 20.85f, 13.65f, 20f, 13.85f)
    lineTo(19.05f, 14f)
    lineTo(18.75f, 14.8f)
    lineTo(19.2f, 15.55f)
    curveTo(19.7f, 16.3f, 19.6f, 17.3f, 18.95f, 17.95f)
    lineTo(17.95f, 18.95f)
    curveTo(17.3f, 19.6f, 16.3f, 19.7f, 15.55f, 19.2f)
    lineTo(14.8f, 18.75f)
    lineTo(14f, 19.05f)
    lineTo(13.85f, 20f)
    curveTo(13.65f, 20.85f, 12.9f, 21.5f, 12f, 21.5f)
    curveTo(11.1f, 21.5f, 10.35f, 20.85f, 10.15f, 20f)
    lineTo(10f, 19.05f)
    lineTo(9.2f, 18.75f)
    lineTo(8.45f, 19.2f)
    curveTo(7.7f, 19.7f, 6.7f, 19.6f, 6.05f, 18.95f)
    lineTo(5.05f, 17.95f)
    curveTo(4.4f, 17.3f, 4.3f, 16.3f, 4.8f, 15.55f)
    lineTo(5.25f, 14.8f)
    lineTo(4.95f, 14f)
    lineTo(4f, 13.85f)
    curveTo(3.15f, 13.65f, 2.5f, 12.9f, 2.5f, 12f)
    curveTo(2.5f, 11.1f, 3.15f, 10.35f, 4f, 10.15f)
    lineTo(4.95f, 10f)
    lineTo(5.25f, 9.2f)
    lineTo(4.8f, 8.45f)
    curveTo(4.3f, 7.7f, 4.4f, 6.7f, 5.05f, 6.05f)
    lineTo(6.05f, 5.05f)
    curveTo(6.7f, 4.4f, 7.7f, 4.3f, 8.45f, 4.8f)
    lineTo(9.2f, 5.25f)
    lineTo(10f, 4.95f)
    lineTo(10.15f, 4f)
    curveTo(10.35f, 3.1f, 11.1f, 2.5f, 12f, 2.5f)
    close()
    // 中心孔
    moveTo(12f, 8.5f)
    curveTo(14.0f, 8.5f, 15.5f, 10.0f, 15.5f, 12f)
    curveTo(15.5f, 14.0f, 14.0f, 15.5f, 12f, 15.5f)
    curveTo(10.0f, 15.5f, 8.5f, 14.0f, 8.5f, 12f)
    curveTo(8.5f, 10.0f, 10.0f, 8.5f, 12f, 8.5f)
    close()
} }

// ───────────────────────── 操作 ─────────────────────────

/** plus：新建 */
val SfPlus: ImageVector by lazy { lineIcon("plus") {
    moveTo(12f, 5f); lineTo(12f, 19f)
    moveTo(5f, 12f); lineTo(19f, 12f)
} }

/** xmark：清除/关闭 */
val SfXmark: ImageVector by lazy { lineIcon("xmark") {
    moveTo(6f, 6f); lineTo(18f, 18f)
    moveTo(18f, 6f); lineTo(6f, 18f)
} }

/** ellipsis：⋯ 菜单（水平三点，零长线段+圆头=圆点） */
val SfEllipsis: ImageVector by lazy { dotsIcon("ellipsis", 5.5f to 12f, 12f to 12f, 18.5f to 12f) }

/** magnifyingglass：搜索 */
val SfMagnifyingglass: ImageVector by lazy { lineIcon("magnifyingglass") {
    moveTo(10.5f, 3.5f)
    curveTo(14.36f, 3.5f, 17.5f, 6.64f, 17.5f, 10.5f)
    curveTo(17.5f, 14.36f, 14.36f, 17.5f, 10.5f, 17.5f)
    curveTo(6.64f, 17.5f, 3.5f, 14.36f, 3.5f, 10.5f)
    curveTo(3.5f, 6.64f, 6.64f, 3.5f, 10.5f, 3.5f)
    close()
    moveTo(16f, 16f); lineTo(20.5f, 20.5f)
} }

/** checkmark：勾选 */
val SfCheckmark: ImageVector by lazy { lineIcon("checkmark") {
    moveTo(4.5f, 12.5f); lineTo(9.5f, 17.5f); lineTo(19.5f, 6.5f)
} }

/** arrow.clockwise：重试 */
val SfArrowClockwise: ImageVector by lazy { lineIcon("arrow.clockwise") {
    moveTo(20f, 11f)
    curveTo(19.4f, 6.8f, 16.0f, 3.5f, 12f, 3.5f)
    curveTo(7.3f, 3.5f, 3.5f, 7.3f, 3.5f, 12f)
    curveTo(3.5f, 16.7f, 7.3f, 20.5f, 12f, 20.5f)
    curveTo(15.5f, 20.5f, 18.5f, 18.4f, 19.7f, 15.3f)
    moveTo(20f, 4f)
    lineTo(20f, 11f)
    lineTo(13f, 11f)
} }

// ───────────────────────── 聊天 ─────────────────────────

/** paperplane：发送（SF paperplane 起飞姿态） */
val SfPaperplane: ImageVector by lazy { lineIcon("paperplane") {
    // 机身大三角
    moveTo(21f, 3f)
    lineTo(3f, 10.5f)
    lineTo(10.5f, 13.5f)
    lineTo(13.5f, 21f)
    close()
    // 尾翼
    moveTo(21f, 3f)
    lineTo(10.5f, 13.5f)
} }

/** stop.circle：停止（红色） */
val SfStopCircle: ImageVector by lazy { lineIcon("stop.circle") {
    moveTo(12f, 3.5f)
    curveTo(16.7f, 3.5f, 20.5f, 7.3f, 20.5f, 12f)
    curveTo(20.5f, 16.7f, 16.7f, 20.5f, 12f, 20.5f)
    curveTo(7.3f, 20.5f, 3.5f, 16.7f, 3.5f, 12f)
    curveTo(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f)
    close()
    // 内部方块（stop.fill 位）
    moveTo(9f, 9f); lineTo(15f, 9f); lineTo(15f, 15f); lineTo(9f, 15f); close()
} }

/** camera：拍照 */
val SfCamera: ImageVector by lazy { lineIcon("camera") {
    moveTo(3.5f, 8.5f)
    curveTo(3.5f, 7.4f, 4.4f, 6.5f, 5.5f, 6.5f)
    lineTo(7.5f, 6.5f)
    lineTo(9f, 4.5f)
    lineTo(15f, 4.5f)
    lineTo(16.5f, 6.5f)
    lineTo(18.5f, 6.5f)
    curveTo(19.6f, 6.5f, 20.5f, 7.4f, 20.5f, 8.5f)
    lineTo(20.5f, 17.5f)
    curveTo(20.5f, 18.6f, 19.6f, 19.5f, 18.5f, 19.5f)
    lineTo(5.5f, 19.5f)
    curveTo(4.4f, 19.5f, 3.5f, 18.6f, 3.5f, 17.5f)
    close()
    // 镜头
    moveTo(12f, 9f)
    curveTo(14.2f, 9f, 16f, 10.8f, 16f, 13f)
    curveTo(16f, 15.2f, 14.2f, 17f, 12f, 17f)
    curveTo(9.8f, 17f, 8f, 15.2f, 8f, 13f)
    curveTo(8f, 10.8f, 9.8f, 9f, 12f, 9f)
    close()
} }

/** photo：相册（照片矩形 + 山 + 太阳） */
val SfPhoto: ImageVector by lazy { lineIcon("photo") {
    moveTo(5.5f, 3.5f)
    lineTo(18.5f, 3.5f)
    curveTo(19.6f, 3.5f, 20.5f, 4.4f, 20.5f, 5.5f)
    lineTo(20.5f, 18.5f)
    curveTo(20.5f, 19.6f, 19.6f, 20.5f, 18.5f, 20.5f)
    lineTo(5.5f, 20.5f)
    curveTo(4.4f, 20.5f, 3.5f, 19.6f, 3.5f, 18.5f)
    lineTo(3.5f, 5.5f)
    curveTo(3.5f, 4.4f, 4.4f, 3.5f, 5.5f, 3.5f)
    close()
    // 山
    moveTo(4.5f, 16.5f)
    lineTo(9.5f, 11.5f)
    lineTo(13f, 15f)
    lineTo(15.5f, 12.5f)
    lineTo(19.5f, 16.5f)
    // 太阳
    moveTo(9f, 8.5f); lineTo(9.01f, 8.5f)
} }

/** doc：文档 */
val SfDoc: ImageVector by lazy { lineIcon("doc") {
    moveTo(6f, 3.5f)
    lineTo(14f, 3.5f)
    lineTo(18.5f, 8f)
    lineTo(18.5f, 19.5f)
    curveTo(18.5f, 20.05f, 18.05f, 20.5f, 17.5f, 20.5f)
    lineTo(6.5f, 20.5f)
    curveTo(5.95f, 20.5f, 5.5f, 20.05f, 5.5f, 19.5f)
    lineTo(5.5f, 4.5f)
    curveTo(5.5f, 3.95f, 5.95f, 3.5f, 6f, 3.5f)
    close()
    moveTo(14f, 3.5f); lineTo(14f, 8f); lineTo(18.5f, 8f)
    // 文字行
    moveTo(8.5f, 12f); lineTo(15.5f, 12f)
    moveTo(8.5f, 15.5f); lineTo(15.5f, 15.5f)
} }

/** paperclip：附件 */
val SfPaperclip: ImageVector by lazy { lineIcon("paperclip") {
    moveTo(17.5f, 7f)
    lineTo(10f, 14.5f)
    curveTo(9.2f, 15.3f, 9.2f, 16.7f, 10f, 17.5f)
    curveTo(10.8f, 18.3f, 12.2f, 18.3f, 13f, 17.5f)
    lineTo(19f, 11.5f)
    curveTo(20.6f, 9.9f, 20.6f, 7.4f, 19f, 5.8f)
    curveTo(17.4f, 4.2f, 14.9f, 4.2f, 13.3f, 5.8f)
    lineTo(6.5f, 12.6f)
    curveTo(4.3f, 14.8f, 4.3f, 18.3f, 6.5f, 20.5f)
    curveTo(8.7f, 22.7f, 12.2f, 22.7f, 14.4f, 20.5f)
    lineTo(18.5f, 16.5f)
} }

// ───────────────────────── 内容操作 ─────────────────────────

/** trash：删除 */
val SfTrash: ImageVector by lazy { lineIcon("trash") {
    moveTo(5.5f, 6.5f)
    lineTo(18.5f, 6.5f)
    moveTo(8.5f, 6.5f)
    lineTo(8.5f, 4.5f)
    curveTo(8.5f, 3.95f, 8.95f, 3.5f, 9.5f, 3.5f)
    lineTo(14.5f, 3.5f)
    curveTo(15.05f, 3.5f, 15.5f, 3.95f, 15.5f, 4.5f)
    lineTo(15.5f, 6.5f)
    moveTo(6.5f, 6.5f)
    lineTo(7.4f, 19.6f)
    curveTo(7.45f, 20.1f, 7.9f, 20.5f, 8.4f, 20.5f)
    lineTo(15.6f, 20.5f)
    curveTo(16.1f, 20.5f, 16.55f, 20.1f, 16.6f, 19.6f)
    lineTo(17.5f, 6.5f)
    // 竖线
    moveTo(10.2f, 10.5f); lineTo(10.5f, 17f)
    moveTo(13.8f, 10.5f); lineTo(13.5f, 17f)
} }

/** square.and.pencil：重命名 */
val SfSquareAndPencil: ImageVector by lazy { lineIcon("square.and.pencil") {
    moveTo(11.5f, 20.5f)
    lineTo(5f, 20.5f)
    curveTo(4.17f, 20.5f, 3.5f, 19.83f, 3.5f, 19f)
    lineTo(3.5f, 5f)
    curveTo(3.5f, 4.17f, 4.17f, 3.5f, 5f, 3.5f)
    lineTo(17.5f, 3.5f)
    curveTo(18.33f, 3.5f, 19f, 4.17f, 19f, 5f)
    lineTo(19f, 11f)
    // 铅笔
    moveTo(14.5f, 20.5f)
    lineTo(21f, 14f)
    curveTo(21.55f, 13.45f, 21.55f, 12.55f, 21f, 12f)
    curveTo(20.45f, 11.45f, 19.55f, 11.45f, 19f, 12f)
    lineTo(12.5f, 18.5f)
    lineTo(11.5f, 21.5f)
    close()
} }

/** eye：视觉标记 */
val SfEye: ImageVector by lazy { lineIcon("eye") {
    moveTo(2.5f, 12f)
    curveTo(5.0f, 7.5f, 8.3f, 5.5f, 12f, 5.5f)
    curveTo(15.7f, 5.5f, 19.0f, 7.5f, 21.5f, 12f)
    curveTo(19.0f, 16.5f, 15.7f, 18.5f, 12f, 18.5f)
    curveTo(8.3f, 18.5f, 5.0f, 16.5f, 2.5f, 12f)
    close()
    moveTo(12f, 8.5f)
    curveTo(14.0f, 8.5f, 15.5f, 10.0f, 15.5f, 12f)
    curveTo(15.5f, 14.0f, 14.0f, 15.5f, 12f, 15.5f)
    curveTo(10.0f, 15.5f, 8.5f, 14.0f, 8.5f, 12f)
    curveTo(8.5f, 10.0f, 10.0f, 8.5f, 12f, 8.5f)
    close()
} }

/** star：重点星标 */
val SfStar: ImageVector by lazy { lineIcon("star") {
    moveTo(12f, 3f)
    lineTo(14.6f, 8.7f)
    lineTo(20.8f, 9.4f)
    lineTo(16.2f, 13.6f)
    lineTo(17.5f, 19.8f)
    lineTo(12f, 16.8f)
    lineTo(6.5f, 19.8f)
    lineTo(7.8f, 13.6f)
    lineTo(3.2f, 9.4f)
    lineTo(9.4f, 8.7f)
    close()
} }

/** sparkles：AI 生成中 */
val SfSparkles: ImageVector by lazy { lineIcon("sparkles") {
    // 大星芒（四角星）
    moveTo(11f, 3f)
    lineTo(12.3f, 8.2f)
    lineTo(17.5f, 9.5f)
    lineTo(12.3f, 10.8f)
    lineTo(11f, 16f)
    lineTo(9.7f, 10.8f)
    lineTo(4.5f, 9.5f)
    lineTo(9.7f, 8.2f)
    close()
    // 小星芒
    moveTo(18f, 15f)
    lineTo(18.7f, 17.3f)
    lineTo(21f, 18f)
    lineTo(18.7f, 18.7f)
    lineTo(18f, 21f)
    lineTo(17.3f, 18.7f)
    lineTo(15f, 18f)
    lineTo(17.3f, 17.3f)
    close()
} }

/** function：数学函数 ƒ（app logo 感） */
val SfFunction: ImageVector by lazy { lineIcon("function") {
    // ƒ 曲线主体
    moveTo(14.5f, 4f)
    curveTo(12.5f, 4f, 11f, 5.5f, 11f, 7.5f)
    lineTo(11f, 16.5f)
    curveTo(11f, 18.5f, 9.5f, 20f, 7.5f, 20f)
    // 横杠
    moveTo(8.5f, 9.5f)
    lineTo(16f, 9.5f)
    moveTo(16.5f, 4f)
    curveTo(18.5f, 4f, 20f, 5.5f, 20f, 7.5f)
    lineTo(20f, 16.5f)
    curveTo(20f, 18.5f, 21.5f, 20f, 23f, 20f)
} }

/** info.circle：ⓘ 信息 */
val SfInfoCircle: ImageVector by lazy { lineIcon("info.circle") {
    moveTo(12f, 3.5f)
    curveTo(16.7f, 3.5f, 20.5f, 7.3f, 20.5f, 12f)
    curveTo(20.5f, 16.7f, 16.7f, 20.5f, 12f, 20.5f)
    curveTo(7.3f, 20.5f, 3.5f, 16.7f, 3.5f, 12f)
    curveTo(3.5f, 7.3f, 7.3f, 3.5f, 12f, 3.5f)
    close()
    moveTo(12f, 10.5f); lineTo(12f, 16.5f)
    moveTo(12f, 7.4f); lineTo(12.01f, 7.4f)
} }

/** share：分享/导出（方框加向上箭头） */
val SfShare: ImageVector by lazy { lineIcon("square.and.arrow.up") {
    // 上箭头
    moveTo(12f, 3f)
    lineTo(12f, 14.5f)
    moveTo(8.5f, 6.5f)
    lineTo(12f, 3f)
    lineTo(15.5f, 6.5f)
    // 托盘
    moveTo(7f, 11f)
    lineTo(5.5f, 11f)
    curveTo(4.4f, 11f, 3.5f, 11.9f, 3.5f, 13f)
    lineTo(3.5f, 18.5f)
    curveTo(3.5f, 19.6f, 4.4f, 20.5f, 5.5f, 20.5f)
    lineTo(18.5f, 20.5f)
    curveTo(19.6f, 20.5f, 20.5f, 19.6f, 20.5f, 18.5f)
    lineTo(20.5f, 13f)
    curveTo(20.5f, 11.9f, 19.6f, 11f, 18.5f, 11f)
    lineTo(17f, 11f)
} }
