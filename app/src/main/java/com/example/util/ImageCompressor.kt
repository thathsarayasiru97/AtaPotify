package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

data class CompressedReceipt(
    val mimeType: String,
    val base64: String,
    val sizeBytes: Int
)

object ImageCompressor {
    private const val MAX_DIMENSION_PX = 1080
    private const val TARGET_MAX_BYTES = 220_000

    suspend fun compressUriToBase64(context: Context, uri: Uri): Result<CompressedReceipt> =
        withContext(Dispatchers.IO) {
            runCatching {
                val contentResolver = context.contentResolver

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, bounds)
                } ?: error("Unable to open selected receipt image")

                val longestSide = max(bounds.outWidth, bounds.outHeight)
                var sampleSize = 1
                while (longestSide / sampleSize > MAX_DIMENSION_PX * 2) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val rawBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: error("Failed to decode selected receipt image")

                val scaled = scaleDownIfNeeded(rawBitmap, MAX_DIMENSION_PX)
                if (scaled !== rawBitmap) {
                    rawBitmap.recycle()
                }

                val out = ByteArrayOutputStream()
                var quality = 85
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                while (out.size() > TARGET_MAX_BYTES && quality > 35) {
                    out.reset()
                    quality -= 10
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }

                val bytes = out.toByteArray()
                scaled.recycle()
                require(bytes.size <= 320_000) {
                    "Receipt image is too large (${bytes.size / 1024} KB). Please pick a smaller screenshot."
                }
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                CompressedReceipt(
                    mimeType = "image/jpeg",
                    base64 = encoded,
                    sizeBytes = bytes.size
                )
            }
        }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = max(width, height)
        if (longest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / longest.toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
