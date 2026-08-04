package com.samroid.wled.presentation.ambilight


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * ارسال ساده رنگ‌ها روی UDP.
 * فرمت MVP: [0x9C][count][R,G,B × count]  — قابل تطبیق با پروتکل واقعی نود
 */
class UdpColorSender(
    private var host: String,
    private var port: Int
) {
    private var socket: DatagramSocket? = null

    fun updateTarget(host: String, port: Int) {
        this.host = host
        this.port = port
    }

    @Synchronized
    fun sendColors(colors: IntArray) {
        if (colors.isEmpty()) return
        try {
            val sock = socket ?: DatagramSocket().also { socket = it }
            val payload = ByteArray(2 + colors.size * 3)
            payload[0] = 0x9C.toByte()
            payload[1] = colors.size.toByte()
            var i = 2
            for (c in colors) {
                payload[i++] = ((c shr 16) and 0xFF).toByte()
                payload[i++] = ((c shr 8) and 0xFF).toByte()
                payload[i++] = (c and 0xFF).toByte()
            }
            val address = InetAddress.getByName(host)
            sock.send(DatagramPacket(payload, payload.size, address, port))
        } catch (_: Exception) {
            // برای MVP خطا را قورت می‌دهیم؛ بعداً لاگ کن
        }
    }

    @Synchronized
    fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }
}