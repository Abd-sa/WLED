package com.samroid.wled.presentation.provision

enum class ProvisionStep(val index: Int, val title: String) {
    GPIO(1, "GPIO"),
    COLOR(2, "Color"),
    LENGTH(3, "Length"),
    OUTPUT(4, "Output"),
    STORE(5, "Store");

    companion object {
        fun fromIndex(i: Int) = entries.firstOrNull { it.index == i } ?: GPIO
    }
}