package com.samroid.wled.presentation.connection.wifi

data class WifiConfigUiState(
    val ssid: String = "",
    val password: String = "",
    val baseIp1: String = "192",
    val baseIp2: String = "168",
    val baseIp3: String = "1",
    val isSendingConfig: Boolean = false,
    val isConnectingWifi: Boolean = false,
    val configSent: Boolean = false,
    /** Master connected to modem (only after WIFI_CONNECT ACK) */
    val wifiConnected: Boolean = false,
    val configSaved: Boolean = false,
    val lastMessage: String? = null,
    val bluetoothConnected: Boolean = false
)