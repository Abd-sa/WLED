package com.samroid.wled.data.ambient

/**
 * WLED realtime protocols used by Universal Ambient Light.
 * DDP: UDP port 4048 (recommended for WLED 0.11+)
 * UDP_RAW: port 19446 (raw RGB stream, Hyperion-compatible style)
 */
enum class WledProtocol(val defaultPort: Int) {
    DDP(4048),
    UDP_RAW(19446)
}