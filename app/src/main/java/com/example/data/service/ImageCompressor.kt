package com.example.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
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
    private const val HARD_MAX_BYTES = 320 * 1024

    suspend fun compressUriToBase64(context: Context, uri: Uri): Result<CompressedReceiptImage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawBytes = readUriBytes(context, uri)
                if (rawBytes.isEmpty()) {
                    error("Selected image file was empty.")
                }

                val decoded = decodeBitmapFromBytes(rawBytes)
                    ?: error("Unsupported image format. Please choose a JPG, PNG, or screenshot.")

                var working = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
                if (working !== decoded) decoded.recycle()

                var bytes = compressJpegWithinBudget(working, TARGET_MAX_BYTES)
                if (bytes.size > HARD_MAX_BYTES) {
                    val smaller = scaleDownIfNeeded(working, 720)
                    if (smaller !== working) {
                        working.recycle()
                        working = smaller
                    }
                    bytes = compressJpegWithinBudget(working, TARGET_MAX_BYTES)
                }
                working.recycle()

                if (bytes.size > HARD_MAX_BYTES) {
                    error("Image is still too large (${bytes.size / 1024} KB). Please choose a smaller screenshot.")
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

    private fun readUriBytes(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val fromStream = runCatching {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
        if (fromStream != null && fromStream.isNotEmpty()) {
            return fromStream
        }

        val fromAfd = runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                afd.createInputStream()?.use { it.readBytes() }
            }
        }.getOrNull()
        if (fromAfd != null && fromAfd.isNotEmpty()) {
            return fromAfd
        }

        error("Could not open the selected image. Please pick an image stored on your device.")
    }

    private fun decodeBitmapFromBytes(rawBytes: ByteArray): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val viaDecoder = runCatching {
                val source = ImageDecoder.createSource(ByteBuffer.wrap(rawBytes))
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    if (w > MAX_DIMENSION_PX || h > MAX_DIMENSION_PX) {
                        val ratio = minOf(
                            MAX_DIMENSION_PX.toFloat() / w.coerceAtLeast(1),
                            MAX_DIMENSION_PX.toFloat() / h.coerceAtLeast(1),
                        )
                        decoder.setTargetSize(
                            (w * ratio).toInt().coerceAtLeast(1),
                            (h * ratio).toInt().coerceAtLeast(1),
                        )
                    }
                }
            }.getOrNull()
            if (viaDecoder != null) return viaDecoder
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
        val sampleSize = if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
        } else {
            1
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
    }

    private fun compressJpegWithinBudget(bitmap: Bitmap, targetBytes: Int): ByteArray {
        var quality = 82
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 12
        } while (bytes.size > targetBytes && quality >= 25)
        return bytes
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
