package com.samroid.wled.presentation.navigation

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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.samroid.wled.presentation.dashboard.DashboardScreen

private val Bg = Color(0xFF0B0B12)
private val BarBg = Color(0xFF12121C)
private val Purple = Color(0xFF7C4DFF)
private val Muted = Color(0xFF9A9AB0)

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        TabItem(Routes.DASHBOARD, "Dashboard", Icons.Outlined.Dashboard),
        TabItem(Routes.NODES, "Nodes", Icons.Outlined.DeviceHub),
        TabItem(Routes.PROVISION, "Provision", Icons.Outlined.Tune),
        TabItem(Routes.UDP, "UDP", Icons.Outlined.WifiTethering),
        TabItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings)
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(
                containerColor = BarBg,
                contentColor = Muted
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
                            Text(tab.label, fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Purple,
                            selectedTextColor = Purple,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                            indicatorColor = Purple.copy(alpha = 0.15f)
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
                DashboardScreen()
            }
            composable(Routes.NODES) {
                PlaceholderScreen("Nodes")
            }
            composable(Routes.PROVISION) {
                PlaceholderScreen("Provision")
            }
            composable(Routes.UDP) {
                PlaceholderScreen("UDP")
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
            color = Color.White,
            modifier = Modifier.padding(24.dp)
        )
    }
}