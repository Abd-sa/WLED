package com.samroid.wled.presentation.provision

data class ProvisionUiState(
    val currentStep: ProvisionStep = ProvisionStep.GPIO,
    val bluetoothConnected: Boolean = false,
    val provisionStarted: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,

    // Step 1
    val gpioValue: String = "17",

    // Step 2 — 0=GRB ... 5=GBR
    val colorOrder: Int = 0,

    // Step 3
    val lengthValue: String = "150",

    // برای روز ۹
    val cctWarmGpio: String = "21",
    val cctCoolGpio: String = "22",
    val storeNodeId: String = "5"
)

val COLOR_ORDERS = listOf(
    0 to "GRB",
    1 to "RGB",
    2 to "BRG",
    3 to "RBG",
    4 to "BGR",
    5 to "GBR"
)