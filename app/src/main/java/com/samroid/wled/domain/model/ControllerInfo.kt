package com.samroid.wled.domain.model

data class ControllerInfo(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val nodeCount: Int
) {
    val version: String get() = "$major.$minor.$patch"
}