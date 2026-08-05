package com.samroid.wled.data.ambient

data class AmbientTarget(
    val host: String,
    val port: Int,
    /** Optional slice of the global strip for this node (employer UDP map). */
    val startLed: Int = 0,
    val endLed: Int = Int.MAX_VALUE
)