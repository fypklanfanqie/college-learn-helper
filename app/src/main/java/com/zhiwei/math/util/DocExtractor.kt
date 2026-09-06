package com.zhiwei.math.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.zip.ZipInputStream

/**
 * 文档上传解析（PROJECT-BRIEF.md 5.6）：
 * - txt / md：直传原文
 * - docx：zip + w:t 节点提取（自研，零依赖）
 * - pdf：pdfbox-android 提取文本；扫描版（提取结果过短）返回 null，提示走拍照流程
 */
object DocExtractor {

    data class ExtractResult(val fileName: String, val text: String?)

    fun extract(context: Context, uri: Uri, fileName: String): ExtractResult {
        val lower = fileName.lowercase()
        val text = when {
            lower.endsWith(".txt") || lower.endsWith(".md") -> readText(context, uri)
            lower.endsWith(".docx") -> extractDocx(context, uri)
            lower.endsWith(".pdf") -> extractPdf(context, uri)
            else -> readText(context, uri) // 其他文本类文件尽力直读
        }
        return ExtractResult(fileName, text)
    }

    private fun readText(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()

    /** docx = zip + word/document.xml，提取 <w:t> 文本节点（零依赖解析） */
    private fun extractDocx(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        return@runCatching extractWtText(xml)
                    }
                    entry = zip.nextEntry
                }
                null
            }
        }
    }.getOrNull()

    private fun extractWtText(xml: String): String {
        val sb = StringBuilder()
        var index = 0
        while (true) {
            val open = xml.indexOf("<w:t", index)
            if (open < 0) break
            val contentStart = xml.indexOf('>', open)
            if (contentStart < 0) break
            val close = xml.indexOf("</w:t>", contentStart)
            if (close < 0) break
            sb.append(xml.substring(contentStart + 1, close))
            index = close + 6
        }
        return sb.toString().trim()
    }

    private fun extractPdf(context: Context, uri: Uri): String? = runCatching {
        // PDFBoxResourceLoader 已在 Application 初始化
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { doc ->
                val stripper = PDFTextStripper()
                stripper.getText(doc).trim()
            }
        }
    }.getOrNull()
        // 提取文本过短 → 大概率扫描版 PDF，提示走拍照流程
        ?.takeIf { it.length > 20 }
}
