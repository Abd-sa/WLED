package com.samroid.wled.presentation.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.samroid.wled.R
import com.samroid.wled.presentation.ambilight.AmbilightScreen
import com.samroid.wled.presentation.ambilight.AmbilightViewModel
import com.samroid.wled.presentation.ambilight.MediaProjectionHolder
import com.samroid.wled.presentation.dashboard.DashboardScreen
import com.samroid.wled.presentation.nodecontrol.NodeControlScreen
import com.samroid.wled.presentation.nodes.NodeInfoScreen
import com.samroid.wled.presentation.nodes.NodeListScreen
import com.samroid.wled.presentation.provision.ProvisionScreen
import com.samroid.wled.presentation.settings.AppUiState
import com.samroid.wled.presentation.udp.UdpScreen



private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppRoot(appUiState: AppUiState) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        TabItem(Routes.DASHBOARD, stringResource(R.string.dashboard), Icons.Outlined.Dashboard),
        TabItem(Routes.NODES, stringResource(R.string.nodes), Icons.Outlined.DeviceHub),
        TabItem(Routes.PROVISION, stringResource(R.string.provision), Icons.Outlined.Tune),
        TabItem(Routes.UDP, stringResource(R.string.udp), Icons.Outlined.WifiTethering),
        TabItem(Routes.SETTINGS, stringResource(R.string.settings), Icons.Outlined.Settings)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(tab.icon, contentDescription = tab.label)
                        },
                        label = {
                            Text(tab.label, style = MaterialTheme.typography.labelSmall)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen (title = stringResource(R.string.dashboard))
//                onOpenAmbilight = { navController.navigate(Routes.AMBILIGHT) }
            }
            composable(Routes.NODES) {
                NodeListScreen(
                    onOpenNode = { id -> navController.navigate(Routes.nodeInfo(id)) },
                    onOpenControl = { id -> navController.navigate(Routes.nodeControl(id)) },
                    onAddNode = { navController.navigate(Routes.PROVISION) }
                )
            }

            composable(
                route = Routes.NODE_INFO,
                arguments = listOf(navArgument("nodeId") { type = NavType.IntType })
            ) {
                NodeInfoScreen(
                    onBack = { navController.popBackStack() },
                    onOpenControl = { id ->
                        navController.navigate(Routes.nodeControl(id))
                    }
                )
            }
            composable(
                route = Routes.NODE_CONTROL,
                arguments = listOf(navArgument("nodeId") { type = NavType.IntType })
            ) {
                NodeControlScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PROVISION) {
                ProvisionScreen(
                    onFinished = {
                        navController.navigate(Routes.NODES) {
                            popUpTo(Routes.DASHBOARD)
                        }
                    }
                )
            }
            composable(Routes.UDP) {
                UdpScreen()
            }
            composable(Routes.AMBILIGHT) {
                var projectionCode by remember { mutableStateOf<Int?>(null) }
                var projectionData by remember { mutableStateOf<Intent?>(null) }
                var projectionDenied by remember { mutableStateOf(false) }

                val projectionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        projectionDenied = false
                        projectionCode = result.resultCode
                        projectionData = result.data
                    } else {
                        projectionDenied = true
                        projectionCode = null
                        projectionData = null
                    }
                }

                val mpm = LocalContext.current.getSystemService(MediaProjectionManager::class.java)

                AmbilightScreen(
                    onRequestProjection = {
                        projectionDenied = false
                        projectionCode = null
                        projectionData = null
                        projectionLauncher.launch(mpm.createScreenCaptureIntent())
                    },
                    projectionResultCode = projectionCode,
                    projectionData = projectionData,
                    projectionDenied = projectionDenied
                )


            }
            composable(Routes.SETTINGS) {
                PlaceholderScreen("Settings")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(24.dp)
        )
    }
}