package com.samroid.wled.presentation.ambilight

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.samroid.wled.MainActivity
import com.samroid.wled.R
import com.samroid.wled.data.ambient.AmbientConfig
import com.samroid.wled.data.ambient.AmbientEngine
import com.samroid.wled.data.ambient.AmbientTarget
import com.samroid.wled.data.ambient.ColorOrder
import com.samroid.wled.data.ambient.LedLayout
import com.samroid.wled.data.ambient.MediaProjectionHolder
import com.samroid.wled.data.ambient.WledProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AmbilightService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var engine: AmbientEngine? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEngine()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startFg()
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA) ?: return START_NOT_STICKY
                val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = mpm.getMediaProjection(resultCode, data) ?: return START_NOT_STICKY
                MediaProjectionHolder.projection = projection

                val cfg = readConfig(intent)
                engine = AmbientEngine(applicationContext).also {
                    it.config = cfg
                    it.start(projection, scope)
                }
            }
        }
        return START_STICKY
    }

    private fun readConfig(intent: Intent): AmbientConfig {
        val hosts = intent.getStringArrayListExtra(EXTRA_HOSTS) ?: arrayListOf("192.168.1.255")
        val port = intent.getIntExtra(EXTRA_PORT, 4048)
        val starts = intent.getIntArrayExtra(EXTRA_START_LEDS)
        val ends = intent.getIntArrayExtra(EXTRA_END_LEDS)
        val protocol = if (intent.getStringExtra(EXTRA_PROTOCOL) == "UDP_RAW") {
            WledProtocol.UDP_RAW
        } else WledProtocol.DDP
        val order = runCatching {
            ColorOrder.valueOf(intent.getStringExtra(EXTRA_COLOR_ORDER) ?: "GRB")
        }.getOrDefault(ColorOrder.GRB)

        val targets = hosts.mapIndexed { i, host ->
            AmbientTarget(
                host = host,
                port = port,
                startLed = starts?.getOrNull(i) ?: 0,
                endLed = ends?.getOrNull(i) ?: Int.MAX_VALUE
            )
        }

        return AmbientConfig(
            protocol = protocol,
            colorOrder = order,
            fps = intent.getIntExtra(EXTRA_FPS, 30),
            qualityPx = intent.getIntExtra(EXTRA_QUALITY, 128),
            smoothing = intent.getBooleanExtra(EXTRA_SMOOTHING, true),
            smoothAlpha = intent.getFloatExtra(EXTRA_SMOOTH_ALPHA, 0.35f),
            averageColor = intent.getBooleanExtra(EXTRA_AVERAGE, false),
            layout = LedLayout(
                top = intent.getIntExtra(EXTRA_LED_TOP, 60),
                right = intent.getIntExtra(EXTRA_LED_RIGHT, 34),
                bottom = intent.getIntExtra(EXTRA_LED_BOTTOM, 60),
                left = intent.getIntExtra(EXTRA_LED_LEFT, 34),
                startBottomCenter = true
            ),
            targets = targets
        )
    }

    private fun startFg() {
        val id = "ambilight_channel"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(id, getString(R.string.ambient_notification_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, id)
            .setContentTitle(getString(R.string.ambient_notification_title))
            .setContentText(getString(R.string.ambient_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(42, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else startForeground(42, n)
    }

    private fun stopEngine() {
        engine?.stop()
        engine = null
        MediaProjectionHolder.projection?.stop()
        MediaProjectionHolder.projection = null
    }

    override fun onDestroy() {
        stopEngine()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.samroid.wled.ambilight.START"
        const val ACTION_STOP = "com.samroid.wled.ambilight.STOP"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val EXTRA_HOSTS = "hosts"
        const val EXTRA_PORT = "port"
        const val EXTRA_PROTOCOL = "protocol"
        const val EXTRA_COLOR_ORDER = "colorOrder"
        const val EXTRA_FPS = "fps"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_SMOOTHING = "smoothing"
        const val EXTRA_SMOOTH_ALPHA = "smoothAlpha"
        const val EXTRA_AVERAGE = "average"
        const val EXTRA_LED_TOP = "ledTop"
        const val EXTRA_LED_RIGHT = "ledRight"
        const val EXTRA_LED_BOTTOM = "ledBottom"
        const val EXTRA_LED_LEFT = "ledLeft"
        const val EXTRA_START_LEDS = "startLeds"
        const val EXTRA_END_LEDS = "endLeds"

        fun start(
            context: Context,
            resultCode: Int,
            data: Intent,
            hosts: ArrayList<String>,
            port: Int,
            protocol: String = "DDP",
            colorOrder: String = "GRB",
            fps: Int = 30,
            quality: Int = 128,
            smoothing: Boolean = true,
            smoothAlpha: Float = 0.35f,
            average: Boolean = false,
            ledTop: Int = 60,
            ledRight: Int = 34,
            ledBottom: Int = 60,
            ledLeft: Int = 34,
            startLeds: IntArray = intArrayOf(),
            endLeds: IntArray = intArrayOf()
        ) {
            val i = Intent(context, AmbilightService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
                putStringArrayListExtra(EXTRA_HOSTS, hosts)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_PROTOCOL, protocol)
                putExtra(EXTRA_COLOR_ORDER, colorOrder)
                putExtra(EXTRA_FPS, fps)
                putExtra(EXTRA_QUALITY, quality)
                putExtra(EXTRA_SMOOTHING, smoothing)
                putExtra(EXTRA_SMOOTH_ALPHA, smoothAlpha)
                putExtra(EXTRA_AVERAGE, average)
                putExtra(EXTRA_LED_TOP, ledTop)
                putExtra(EXTRA_LED_RIGHT, ledRight)
                putExtra(EXTRA_LED_BOTTOM, ledBottom)
                putExtra(EXTRA_LED_LEFT, ledLeft)
                putExtra(EXTRA_START_LEDS, startLeds)
                putExtra(EXTRA_END_LEDS, endLeds)
            }
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, AmbilightService::class.java).apply { action = ACTION_STOP })
        }
    }
}