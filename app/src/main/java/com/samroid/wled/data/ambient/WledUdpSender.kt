package com.samroid.wled.data.ambient

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger

class WledUdpSender {
    private var socket: DatagramSocket? = null
    private val seq = AtomicInteger(0)

    fun open() {
        if (socket == null || socket?.isClosed == true) {
            socket = DatagramSocket().apply { soTimeout = 0 }
        }
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }

    fun send(
        colors: IntArray,
        targets: List<AmbientTarget>,
        protocol: WledProtocol,
        order: ColorOrder
    ) {
        val s = socket ?: return
        for (t in targets) {
            if (t.host.isBlank()) continue
            val slice = sliceColors(colors, t.startLed, t.endLed)
            if (slice.isEmpty()) continue
            val addr = InetAddress.getByName(t.host)
            when (protocol) {
                WledProtocol.UDP_RAW -> {
                    val data = WledPacketBuilder.buildUdpRaw(slice, order)
                    s.send(DatagramPacket(data, data.size, addr, t.port))
                }
                WledProtocol.DDP -> {
                    val packets = WledPacketBuilder.buildDdpPackets(
                        slice,
                        order,
                        seq.getAndIncrement()
                    )
                    for (p in packets) {
                        s.send(DatagramPacket(p, p.size, addr, t.port))
                    }
                }
            }
        }
    }

    private fun sliceColors(colors: IntArray, start: Int, end: Int): IntArray {
        if (colors.isEmpty()) return colors
        val s = start.coerceIn(0, colors.lastIndex)
        val e = end.coerceIn(s, colors.lastIndex)
        return colors.copyOfRange(s, e + 1)
    }
}