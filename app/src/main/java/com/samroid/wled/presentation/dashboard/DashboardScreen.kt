package com.samroid.wled.presentation.dashboard


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.presentation.components.AppTopBar
import com.samroid.wled.presentation.settings.AppUiState
import com.samroid.wled.presentation.settings.ThemeMode
import com.samroid.wled.presentation.theme.AppColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector



@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    appUiState: AppUiState,
    onOpenBluetooth: () -> Unit = {},
    onOpenWifi: () -> Unit = {},
    onOpenNodes: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

// Top bar
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(

                MaterialTheme.colorScheme.background  )
    )
    {
        AppTopBar(
            title = stringResource(R.string.dashboard),
            showBackButton = false,
            onBackClick = {  },

            )
        DashboardContent(
            state = state,
            onRefreshNodes = {
                viewModel.refreshNodes()
                onOpenNodes()
            },
            onAmbientToggle = viewModel::setAmbientEnabled,
            onOpenBluetooth = onOpenBluetooth,
            onOpenWifi = onOpenWifi
        )
    }

}

@Composable
fun DashboardContent(
    state: DashboardUiState,
    onRefreshNodes: () -> Unit,
    onAmbientToggle: (Boolean) -> Unit,
    onOpenBluetooth: () -> Unit,
    onOpenWifi: () -> Unit
) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())

        )
        {


            Spacer(Modifier.height(10.dp))
            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 16.dp)

                ,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor =  MaterialTheme.colorScheme.surfaceContainer ),
                elevation = CardDefaults.cardElevation(0.dp)

            ) {
                Column (modifier = Modifier.padding(10.dp)){
                    Text(
                        text = "Connection",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(10.dp))

                    ConnectionCard(
                        icon = Icons.Outlined.Bluetooth,
                        title = "Bluetooth",
                        subtitle = state.bluetoothName,
                        connected = state.bluetoothState == TransportConnectionState.CONNECTED,
                        statusText = when (state.bluetoothState) {
                            TransportConnectionState.CONNECTED -> "Connected"
                            TransportConnectionState.CONNECTING -> "Connecting..."
                            TransportConnectionState.ERROR -> "Error"
                            TransportConnectionState.DISCONNECTED -> "Disconnected"
                        },
                        onClick = onOpenBluetooth
                    )

                    Spacer(Modifier.height(10.dp))

                    ConnectionCard(
                        icon = Icons.Outlined.Wifi,
                        title = "WiFi (Master)",
                        subtitle = buildString {
                            append(state.wifiSsid)
                            if (state.wifiIp != "—") append("\n").append(state.wifiIp)
                        },
                        connected = state.wifiConnected,
                        statusText = if (state.wifiConnected) "Connected" else "Disconnected",
                        onClick = onOpenWifi
                    )

                }

            }

            //Spacer(Modifier.height(0.dp))

            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 16.dp)

                ,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor =  MaterialTheme.colorScheme.surfaceContainer),
                elevation = CardDefaults.cardElevation(0.dp)

            ) {
                Column (modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Nodes Overview",
                        color =  MaterialTheme.colorScheme.onSurfaceVariant  ,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip(
                            label = "Total",
                            value = state.totalNodes.toString(),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "Online",
                            value = state.onlineNodes.toString(),
                            valueColor = AppColors.Brand.Green,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "Offline",
                            value = state.offlineNodes.toString(),
                            valueColor = AppColors.Brand.Red,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            onRefreshNodes()

                        },
                        enabled = !state.isRefreshingNodes &&
                                state.bluetoothState == TransportConnectionState.CONNECTED,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Brand.Purple,
                            disabledContainerColor = AppColors.Brand.PurpleDim.copy(alpha = 0.4f)
                        )
                    ) {
                        if (state.isRefreshingNodes) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = "Refresh Nodes",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            //Spacer(Modifier.height(24.dp))
            Card (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 16.dp)

                ,
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor =  MaterialTheme.colorScheme.surfaceContainer ),
                elevation = CardDefaults.cardElevation(0.dp)

            ) {
                Column (modifier = Modifier.padding(10.dp)) {
            AmbientCard(
                enabled = state.ambientEnabled,
                endpoint = state.ambientEndpoint,
                onToggle = onAmbientToggle
            )

                }
            }
            Spacer(Modifier.height(5.dp))

        }


}


@Composable
fun ConnectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    connected: Boolean,
    statusText: String,
    onClick: () -> Unit
) {
    Card (
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AppColors.Brand.Purple.copy(alpha = 0.35f),
                                AppColors.Brand.PurpleDim.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AppColors.Brand.Purple)
            }

            Spacer(Modifier.width(14.dp))

            Column (modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (connected) AppColors.Status.Success else AppColors.Status.Error)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = if (connected) AppColors.Status.Success else AppColors.Text.SecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = AppColors.Text.SecondaryDark)
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background( MaterialTheme.colorScheme.surfaceContainer )
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant , fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AmbientCard(
    enabled: Boolean,
    endpoint: String,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ambient Light (UDP)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (enabled) "Streaming" else "Stopped",
                    color = if (enabled) AppColors.Brand.Purple else{
                         MaterialTheme.colorScheme.onSurfaceVariant  },
                    fontSize = 12.sp
                )
                Text(
                    text = endpoint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) ,
                    fontSize = 11.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onSurface,
                    checkedTrackColor = AppColors.Brand.Purple,
                    uncheckedThumbColor =  MaterialTheme.colorScheme.onSurfaceVariant ,
                    uncheckedTrackColor = Color(0xFF2A2A3A)
                )
            )
        }
    }
}

@Preview(
    name = "Dashboard – Connected",
    showBackground = true,
    backgroundColor = 0xFF0B0B12,
    widthDp = 360,
    heightDp = 780
)
@Composable
private fun DashboardPreviewConnected() {
    DashboardContent(
        state = DashboardUiState(
            bluetoothState = TransportConnectionState.CONNECTED,
            bluetoothName = "HC-05",
            wifiConnected = true,
            wifiSsid = "Home Network",
            wifiIp = "192.168.1.10",
            totalNodes = 4,
            onlineNodes = 3,
            offlineNodes = 1,
            ambientEnabled = true,
            ambientEndpoint = "192.168.1.255:7777"
        ),
        onRefreshNodes = {},
        onAmbientToggle = {},
        onOpenBluetooth = {},
        onOpenWifi = {}
    )
}

@Preview(
    name = "Dashboard – Disconnected",
    showBackground = true,
    backgroundColor = 0xFF0B0B12,
    widthDp = 360,
    heightDp = 780
)
@Composable
private fun DashboardPreviewDisconnected() {
    DashboardContent(
        state = DashboardUiState(
            bluetoothState = TransportConnectionState.DISCONNECTED,
            bluetoothName = "—",
            wifiConnected = false,
            wifiSsid = "—",
            wifiIp = "—",
            totalNodes = 0,
            onlineNodes = 0,
            offlineNodes = 0,
            ambientEnabled = false
        ),
        onRefreshNodes = {},
        onAmbientToggle = {},
        onOpenBluetooth = {},
        onOpenWifi = {}
    )
}

@Preview(
    name = "Dashboard – Refreshing",
    showBackground = true,
    backgroundColor = 0xFF0B0B12,
    widthDp = 360,
    heightDp = 780
)
@Composable
private fun DashboardPreviewRefreshing() {
    DashboardContent(
        state = DashboardUiState(
            bluetoothState = TransportConnectionState.CONNECTED,
            bluetoothName = "HC-05",
            wifiConnected = true,
            wifiSsid = "Home Network",
            wifiIp = "192.168.1.10",
            totalNodes = 4,
            onlineNodes = 3,
            offlineNodes = 1,
            isRefreshingNodes = true,
            ambientEnabled = false
        ),
        onRefreshNodes = {},
        onAmbientToggle = {},
        onOpenBluetooth = {},
        onOpenWifi = {}
    )
}