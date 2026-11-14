package com.example.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CompressedReceiptImage(
    val mimeType: String,
    val base64: String,
    val byteSize: Int,
)

object ImageCompressor {
    private const val MAX_DIMENSION_PX = 1080
    private const val TARGET_MAX_BYTES = 220 * 1024

    suspend fun compressUriToBase64(context: Context, uri: Uri): Result<CompressedReceiptImage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                } ?: error("Unable to read selected image")

                val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: error("Unable to decode selected image")

                val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
                if (scaled !== decoded) decoded.recycle()

                var quality = 82
                var bytes: ByteArray
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    bytes = out.toByteArray()
                    quality -= 12
                } while (bytes.size > TARGET_MAX_BYTES && quality >= 30)

                scaled.recycle()

                if (bytes.size > 320 * 1024) {
                    error("Compressed image is still too large (${bytes.size / 1024} KB). Please choose a smaller screenshot.")
                }

                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                CompressedReceiptImage(
                    mimeType = "image/jpeg",
                    base64 = encoded,
                    byteSize = bytes.size,
                )
            }
        }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        while (width / inSampleSize > maxDim * 2 || height / inSampleSize > maxDim * 2) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w.toFloat(), maxDim.toFloat() / h.toFloat())
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
