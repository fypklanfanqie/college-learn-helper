package com.zhiwei.math.util

import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 自研极简 docx 生成器（PROJECT-BRIEF.md 5.5：docx = zip + document.xml）。
 * 支持标题/正文段落；公式文本以 Unicode 文本写入。
 */
object DocxWriter {

    data class Paragraph(val text: String, val heading: Boolean = false)

    fun write(paragraphs: List<Paragraph>, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(CONTENT_TYPES.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(RELS.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(buildDocument(paragraphs).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
    }

    private fun buildDocument(paragraphs: List<Paragraph>): String {
        val body = StringBuilder()
        for (p in paragraphs) {
            val rPr = if (p.heading) "<w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr>" else ""
            body.append("<w:p><w:r>")
            if (rPr.isNotEmpty()) body.append(rPr)
            body.append("<w:t xml:space=\"preserve\">")
            body.append(xmlEscape(p.text))
            body.append("</w:t></w:r></w:p>")
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
            "<w:body>$body<w:sectPr/></w:body></w:document>"
    }

    private fun xmlEscape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private const val CONTENT_TYPES = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

    private const val RELS = """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
}
