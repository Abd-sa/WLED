package com.samroid.wled.presentation.settings


data class AppUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val language: Language = Language.PERSIAN
)