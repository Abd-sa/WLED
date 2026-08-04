package com.samroid.wled.presentation.settings


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<AppUiState> =
        combine(
            preferences.theme,
            preferences.language
        ) { theme, language ->

            AppUiState(
                themeMode = ThemeMode.valueOf(theme),
                language = Language.entries.first {
                    it.code == language
                }
            )

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppUiState()
        )

    fun toggleTheme() {

        viewModelScope.launch {

            val current = uiState.value.themeMode

            val next = when (current) {
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.LIGHT
            }

            preferences.saveTheme(next.name)
        }
    }

    fun toggleLanguage() {

        viewModelScope.launch {

            val next =
                if (uiState.value.language == Language.ENGLISH)
                    Language.PERSIAN
                else
                    Language.ENGLISH

            preferences.saveLanguage(next.code)
        }
    }
}