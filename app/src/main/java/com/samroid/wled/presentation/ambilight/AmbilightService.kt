package com.samroid.wled.presentation.ambilight

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.samroid.wled.MainActivity
import com.samroid.wled.R

class AmbilightService : Service() {

    companion object {
        const val CHANNEL_ID = "ambilight_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.samroid.wled.ambilight.START"
        const val ACTION_STOP = "com.samroid.wled.ambilight.STOP"

        const val EXTRA_FPS = "fps"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_SMOOTHING = "smoothing"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
    }

    private var fps: Int = 30
    private var quality: String = "High"
    private var smoothing: Float = 50f
    private var host: String = "192.168.1.255"
    private var port: Int = 7777

    private var capturer: ScreenCapturer? = null
    private var sender: UdpColorSender? = null
    private var running = false
    private var loopThread: Thread? = null
    private var previousColors: IntArray? = null
    private var mediaProjection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                fps = intent.getIntExtra(EXTRA_FPS, 30)
                quality = intent.getStringExtra(EXTRA_QUALITY) ?: "High"
                smoothing = intent.getFloatExtra(EXTRA_SMOOTHING, 50f)
                host = intent.getStringExtra(EXTRA_HOST) ?: host
                port = intent.getIntExtra(EXTRA_PORT, 7777)


                // روز ۱۲: اینجا MediaProjection + حلقه capture + UDP
                val projection = MediaProjectionHolder.projection
                if (projection != null) {
                    attachProjection(projection)
                    startForeground(NOTIFICATION_ID, buildNotification(running = true))
                    startLoop()
                }

            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ambient Light",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(running: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, AmbilightService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wled_ambient_light))
            .setContentText(
                if (running) "Streaming → $host:$port @ ${fps}fps"
                else "Stopped"
            )
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .setOngoing(running)
            .build()
    }

    fun attachProjection(projection: MediaProjection) {
        mediaProjection = projection
    }

    private fun startLoop() {
        val projection = mediaProjection ?: return
        val scale = when (quality) {
            "High" -> 0.35f
            "Low" -> 0.15f
            else -> 0.25f
        }
        capturer = ScreenCapturer(this, projection, scale).also { it.start() }
        sender = UdpColorSender(host, port)
        running = true

        val frameDelay = (1000L / fps.coerceIn(5, 60))
        loopThread = Thread {
            while (running) {
                val start = System.currentTimeMillis()
                val bmp = capturer?.captureBitmap()
                if (bmp != null) {
                    val raw = EdgeColorSampler.sample(bmp, ledsPerSide = 12)
                    val smoothed = EdgeColorSampler.smooth(previousColors, raw, smoothing)
                    previousColors = smoothed
                    sender?.sendColors(smoothed)
                    bmp.recycle()
                }
                val elapsed = System.currentTimeMillis() - start
                val sleep = frameDelay - elapsed
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }.also { it.start() }
    }

    private fun stopLoop() {
        running = false
        loopThread?.interrupt()
        loopThread = null
        capturer?.stop()
        capturer = null
        sender?.close()
        sender = null
        try {
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        mediaProjection = null
    }
}