package com.samroid.wled.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.samroid.wled.presentation.dashboard.DashboardScreen
import com.samroid.wled.presentation.navigation.Routes
import com.samroid.wled.presentation.settings.AppUiState
import com.samroid.wled.presentation.theme.AppColors

@Composable
fun AppRoot(appUiState: AppUiState) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = AppColors.Dashboard.CardDark,
        bottomBar = {
            // بعداً NavigationBar
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable (Routes.DASHBOARD) {
                DashboardScreen (
                    appUiState =appUiState,
                    onOpenBluetooth = {
                        //navController.navigate(Routes.CONNECTION)
                                      },
                    onOpenWifi = {
                        //navController.navigate(Routes.NETWORK)
                                 },
                    onOpenNodes = {
                        //navController.navigate(Routes.NODES)
                    }
                )
            }
            // بقیه مسیرها...
        }
    }
}