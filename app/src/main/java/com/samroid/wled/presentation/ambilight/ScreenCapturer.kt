package com.samroid.wled.presentation.ambilight


import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCapturer(
    context: Context,
    private val mediaProjection: MediaProjection,
    private val qualityScale: Float = 0.25f // High≈0.35, Medium=0.25, Low=0.15
) {
    private val metrics = DisplayMetrics().also {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(it)
    }

    private val fullWidth = metrics.widthPixels
    private val fullHeight = metrics.heightPixels
    private val density = metrics.densityDpi

    val width: Int = (fullWidth * qualityScale).toInt().coerceAtLeast(64)
    val height: Int = (fullHeight * qualityScale).toInt().coerceAtLeast(64)

    private val imageReader: ImageReader =
        ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

    private var virtualDisplay: VirtualDisplay? = null
    private val thread = HandlerThread("ScreenCapturer").also { it.start() }
    private val handler = Handler(thread.looper)

    fun start() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "WledAmbilight",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            handler
        )
    }

    fun captureBitmap(): Bitmap? {
        var image: Image? = null
        return try {
            image = imageReader.acquireLatestImage() ?: return null
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width

            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            if (rowPadding == 0) bitmap
            else Bitmap.createBitmap(bitmap, 0, 0, width, height)
        } catch (_: Exception) {
            null
        } finally {
            image?.close()
        }
    }

    fun stop() {
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        try {
            imageReader.close()
        } catch (_: Exception) {
        }
        thread.quitSafely()
    }
}