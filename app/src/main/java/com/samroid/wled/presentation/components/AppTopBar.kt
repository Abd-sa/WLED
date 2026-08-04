package com.samroid.wled.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.presentation.settings.AppViewModel
import com.samroid.wled.presentation.settings.Language
import com.samroid.wled.presentation.settings.ThemeMode
import com.samroid.wled.presentation.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    val appViewModel: AppViewModel = hiltViewModel()
    val uiState by appViewModel.uiState.collectAsStateWithLifecycle()

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },

        navigationIcon = {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },

        actions = {

            IconButton(
                onClick = {appViewModel.toggleTheme()}
            ) {
                Icon(
                    imageVector = when (uiState.themeMode) {
                        ThemeMode.LIGHT -> Icons.Default.DarkMode
                        ThemeMode.DARK -> Icons.Default.LightMode
                    },
                    contentDescription = "Theme"
                )
            }

            IconButton(
                onClick = {appViewModel.toggleLanguage()}
            ) {
                Text(
                    text = if (uiState.language == Language.PERSIAN) "EN" else "فا"
                )
            }

        }

    )
}