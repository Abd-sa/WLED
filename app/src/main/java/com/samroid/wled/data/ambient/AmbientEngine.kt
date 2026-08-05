package com.samroid.wled.data.ambient

import android.content.Context
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Main ambient pipeline: capture → sample → smooth → UDP send.
 */
class AmbientEngine(
    private val context: Context
) {
    private val sampler = EdgeSampler()
    private val smoother = ColorSmoother()
    private val sender = WledUdpSender()
    private var capturer: ScreenCapturer? = null
    private var job: Job? = null

    @Volatile
    var config: AmbientConfig = AmbientConfig()
        set(value) {
            field = value
            sampler.updateLayout(value.layout)
            smoother.setAlpha(if (value.smoothing) value.smoothAlpha else 1f)
        }

    val isRunning: Boolean get() = job?.isActive == true

    fun start(projection: MediaProjection, scope: CoroutineScope) {
        stop()
        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        val q = config.qualityPx.coerceIn(64, 512)
        val aspect = metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat()
        val capW: Int
        val capH: Int
        if (metrics.widthPixels >= metrics.heightPixels) {
            capW = q
            capH = (q / aspect).toInt().coerceAtLeast(36)
        } else {
            capH = q
            capW = (q * aspect).toInt().coerceAtLeast(36)
        }

        capturer = ScreenCapturer(projection, capW, capH, metrics.densityDpi).also { it.start() }
        sender.open()
        sampler.updateLayout(config.layout)
        smoother.reset()
        smoother.setAlpha(if (config.smoothing) config.smoothAlpha else 1f)

        val frameDelay = (1000L / config.fps.coerceIn(5, 60))
        job = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val bmp = capturer?.captureBitmap()
                if (bmp != null) {
                    var colors = sampler.sample(bmp)
                    if (config.averageColor && colors.isNotEmpty()) {
                        colors = sampler.averageColor(colors)
                    }
                    if (config.smoothing) {
                        colors = smoother.apply(colors)
                    }
                    if (colors.isNotEmpty() && config.targets.isNotEmpty()) {
                        runCatching {
                            sender.send(
                                colors,
                                config.targets,
                                config.protocol,
                                config.colorOrder
                            )
                        }
                    }
                    if (!bmp.isRecycled) bmp.recycle()
                }
                delay(frameDelay)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        capturer?.stop()
        capturer = null
        sender.close()
        smoother.reset()
    }
}