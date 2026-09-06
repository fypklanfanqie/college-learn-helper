package com.zhiwei.math.ocr

import android.content.Context
import android.graphics.Bitmap
import com.benjaminwan.ocrlibrary.OcrEngine

/**
 * 本地 OCR 封装 + 兜底链（PROJECT-BRIEF.md 5.2 / 7.5）：
 * - 模型支持视觉 → 直接传图（不走 OCR）
 * - 模型不支持视觉 → 本地 OCR（UI 预先提示"效果不佳"）
 * - OCR 无有效文本 → "OCR 暂时不可用"卡片，若支持视觉则建议直传图片
 *
 * 引擎懒加载（PP-OCR 模型 + onnxruntime 首次初始化较重）。
 */
class OcrManager(context: Context) {

    private val appContext = context.applicationContext

    @Volatile
    private var engine: OcrEngine? = null

    @Volatile
    private var engineFailed = false

    private fun obtain(): OcrEngine? {
        if (engineFailed) return null
        return engine ?: synchronized(this) {
            engine ?: runCatching { OcrEngine(appContext) }
                .onFailure { engineFailed = true }
                .getOrNull()
                ?.also { engine = it }
        }
    }

    /**
     * 识别图片中的文字。返回 null 表示引擎不可用；空串表示无有效文本（低置信度/空结果）。
     */
    fun recognize(bitmap: Bitmap): String? {
        val e = obtain() ?: return null
        return runCatching {
            val result = e.detect(
                bmp = bitmap,
                scaleUp = false,
                maxSideLen = 1024,
                padding = 50,
                boxScoreThresh = 0.5f,
                boxThresh = 0.3f,
                unClipRatio = 2.0f,
                doCls = true,
                mostCls = false,
            )
            // 无有效文本判定：全文过短或平均字符置信度过低
            val allScores = result.recResults.flatMap { it.charScores }
            val avgScore = if (allScores.isEmpty()) 0.0 else allScores.average()
            val text = result.text.trim()
            if (text.length < 2 || avgScore < 0.35) "" else text
        }.getOrNull()
    }

    /** 兜底链决策 */
    sealed interface OcrOutcome {
        /** 拿到有效文本 */
        data class Success(val text: String) : OcrOutcome

        /** OCR 结果无效 */
        data object NoText : OcrOutcome

        /** OCR 引擎不可用 */
        data object Unavailable : OcrOutcome
    }

    fun recognizeWithOutcome(bitmap: Bitmap): OcrOutcome = when (val r = recognize(bitmap)) {
        null -> OcrOutcome.Unavailable
        "" -> OcrOutcome.NoText
        else -> OcrOutcome.Success(r)
    }
}
