package com.samroid.wled.presentation.navigation

import com.samroid.wled.presentation.ambilight.AmbilightScreen

object Routes {
    const val NODE_INFO = "node_info/{nodeId}"

    fun nodeInfo(nodeId: Int) = "node_info/$nodeId"

    const val NODE_CONTROL = "node_control/{nodeId}"
    fun nodeControl(nodeId: Int) = "node_control/$nodeId"

    const val AMBILIGHT = "ambilight"



    const val DASHBOARD = "dashboard"
    const val NODES = "nodes"
    const val PROVISION = "provision"
    const val UDP = "udp"
    const val SETTINGS = "settings"
}