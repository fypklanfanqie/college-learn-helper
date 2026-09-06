package com.zhiwei.math.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 图片预处理：统一压到长边 1280 再上传（约 1,200 tokens/图，docs-research 2.2 建议）。
 */
object ImageUtils {

    private const val MAX_SIDE = 1280
    private const val JPEG_QUALITY = 85

    fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxSide = maxOf(info.size.width, info.size.height)
                if (maxSide > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / maxSide
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
            decoded
        } catch (_: Exception) {
            // ImageDecoder 失败时退回 BitmapFactory
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }.getOrNull()
        }
    }

    fun toJpegBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }
}
