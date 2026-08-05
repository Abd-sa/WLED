package com.samroid.wled.presentation.provision

enum class ProvisionStep(val index: Int, val titleKey: String) {
    GPIO(1, "step_gpio"),
    COLOR(2, "step_color"),
    LENGTH(3, "step_length"),
    OUTPUT(4, "step_output"),
    STORE(5, "step_store");

    companion object {
        fun fromIndex(i: Int) = entries.firstOrNull { it.index == i } ?: GPIO
    }
}