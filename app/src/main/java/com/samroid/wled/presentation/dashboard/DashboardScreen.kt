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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.presentation.components.AppTopBar
import com.samroid.wled.presentation.theme.AppColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.samroid.wled.R
import com.samroid.wled.presentation.connection.bluetooth.BluetoothConnectionSheet
import com.samroid.wled.presentation.connection.wifi.WifiConfigSheet
import com.samroid.wled.presentation.theme.AppColors.Brand.Green
import com.samroid.wled.presentation.theme.AppColors.Brand.Purple
import com.samroid.wled.presentation.theme.AppColors.Brand.PurpleDim
import com.samroid.wled.presentation.theme.AppColors.Brand.Red




@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    title: String

) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBluetoothSheet by remember { mutableStateOf(false) }
    var showWifiSheet by remember { mutableStateOf(false) }
// Top bar
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(

                MaterialTheme.colorScheme.background
            )
    )
    {
        AppTopBar(
            title = title,
            showBackButton = false,
            onBackClick = {  },

            )
        DashboardContent(
            state = state,
            onRefreshNodes = {
                viewModel.refreshNodes()
                //onOpenNodes()
            },
            onAmbientToggle = viewModel::setAmbientEnabled,
            onOpenBluetooth = { showBluetoothSheet = true },
            onOpenWifi = { showWifiSheet = true }
        )
        if (showBluetoothSheet) {
            BluetoothConnectionSheet (onDismiss = { showBluetoothSheet = false })
        }
        if (showWifiSheet) {
            WifiConfigSheet(onDismiss = { showWifiSheet = false })
        }
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
                        text = stringResource(R.string.connection),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(10.dp))

                    ConnectionCard(
                        icon = Icons.Outlined.Bluetooth,
                        title = stringResource(R.string.bluetooth),
                        subtitle = state.bluetoothName,
                        connected = state.bluetoothState == TransportConnectionState.CONNECTED,
                        statusText = when (state.bluetoothState) {
                            TransportConnectionState.CONNECTED -> stringResource(R.string.connected)
                            TransportConnectionState.CONNECTING -> stringResource(R.string.connecting)
                            TransportConnectionState.ERROR -> stringResource(R.string.error)
                            TransportConnectionState.DISCONNECTED -> stringResource(R.string.disconnected)
                        },
                        onClick = onOpenBluetooth
                    )

                    Spacer(Modifier.height(10.dp))

                    ConnectionCard(
                        icon = Icons.Outlined.Wifi,
                        title = stringResource(R.string.wifi_master),
                        subtitle = buildString {
                            append(state.wifiSsid)
                            if (state.wifiIp != stringResource(R.string.empt_dash)) append("\n").append(state.wifiIp)
                        },
                        connected = state.wifiConnected,
                        statusText = if (state.wifiConnected) stringResource(R.string.connected) else stringResource(R.string.disconnected),
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
                        text = stringResource(R.string.nodes_overview),
                        color =  MaterialTheme.colorScheme.onSurfaceVariant  ,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChip(
                            label = stringResource(R.string.total),
                            value = state.totalNodes.toString(),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = stringResource(R.string.online),
                            value = state.onlineNodes.toString(),
                            valueColor = Green,
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = stringResource(R.string.offline),
                            value = state.offlineNodes.toString(),
                            valueColor = Red,
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
                            containerColor = Purple,
                            disabledContainerColor = PurpleDim.copy(alpha = 0.4f)
                        )
                    ) {
                        if (state.isRefreshingNodes) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(
                            text = stringResource(R.string.refresh_nodes),
                            style =MaterialTheme.typography.titleMedium
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
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
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
                                Purple.copy(alpha = 0.35f),
                                PurpleDim.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Purple)
            }

            Spacer(Modifier.width(14.dp))

            Column (modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, lineHeight = 16.sp)
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
                        style = MaterialTheme.typography.bodySmall
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
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant , style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(value, color = valueColor, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun AmbientCard(
    enabled: Boolean,
    endpoint: String,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp)
            ),
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
                    text = stringResource(R.string.ambient_light_udp),
                    color = MaterialTheme.colorScheme.onSurface,
                    style =MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (enabled) stringResource(R.string.streaming) else stringResource(R.string.stopped),
                    color = if (enabled) Purple else{
                         MaterialTheme.colorScheme.onSurfaceVariant  },
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = endpoint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) ,
                    style =MaterialTheme.typography.labelSmall
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
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