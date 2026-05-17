package com.babymakisuk.coreai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * AI 圖片前處理工具。
 *
 * 提供 [Bitmap.compressForAi] extension function，在將圖片傳入 AI API 前
 * 進行縮放與壓縮，減少網路傳輸量與 API 超限風險。
 *
 * ⚠️ **所有函式必須在 IO Dispatcher 上呼叫，不得在 Main Thread 執行。**
 */
object BitmapUtils {

    /** 長邊上限 (px)：超出此値才進行縮放 */
    const val MAX_DIMENSION = 1280

    /** JPEG 壓縮品質 0-100 */
    const val JPEG_QUALITY = 85

    /** 傳給 AI 前的最大位元組數：4 MB */
    const val MAX_AI_BYTE_SIZE = 4 * 1024 * 1024

    /** 內部日誌標籤 */
    const val TAG = "BitmapUtils"
}

/**
 * 縮放並壓縮 Bitmap 以供 AI API 使用。
 *
 * - 長邊超過 [BitmapUtils.MAX_DIMENSION]（1280px）時等比縮放；否則原樣返回，不重新編碼。
 * - 輸出格式：JPEG，品質 [BitmapUtils.JPEG_QUALITY]（85）。
 * - 記錄原始與輸出尺寸及壓縮後大小，供除錯使用。
 *
 * **必須在 IO Dispatcher 呼叫，不得在 Main Thread 執行。**
 *
 * @return 壓縮後的 Bitmap（若已在限制內則直接返回原始實例）
 */
fun Bitmap.compressForAi(): Bitmap {
    val originalW = width
    val originalH = height
    val longerSide = maxOf(originalW, originalH)

    if (longerSide <= BitmapUtils.MAX_DIMENSION) {
        Log.d(BitmapUtils.TAG, "compressForAi: within bounds (${originalW}x$originalH), skip resize")
        return this
    }

    val scale = BitmapUtils.MAX_DIMENSION.toFloat() / longerSide
    val targetW = (originalW * scale).toInt()
    val targetH = (originalH * scale).toInt()

    Log.d(BitmapUtils.TAG, "compressForAi: resizing ${originalW}x$originalH → ${targetW}x$targetH")

    val scaled = Bitmap.createScaledBitmap(this, targetW, targetH, true)

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, BitmapUtils.JPEG_QUALITY, out)
    val bytes = out.toByteArray()

    Log.d(BitmapUtils.TAG, "compressForAi: compressed size = ${bytes.size / 1024} KB")

    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: scaled
}

/**
 * 量測 Bitmap 的 JPEG 位元組大小，供 4MB 守衛使用。
 *
 * **必須在 IO Dispatcher 呼叫。**
 *
 * @param quality JPEG 品質，預設 [BitmapUtils.JPEG_QUALITY]
 * @return 壓縮後的位元組數（bytes）
 */
fun Bitmap.jpegByteSize(quality: Int = BitmapUtils.JPEG_QUALITY): Int {
    val out = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.size()
}
