package com.samroid.wled.presentation.connection.bluetooth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R
import com.samroid.wled.domain.model.TransportConnectionState
import com.samroid.wled.domain.model.TransportDevice
import com.samroid.wled.presentation.theme.AppColors.Brand.Green
import com.samroid.wled.presentation.theme.AppColors.Brand.Red
import com.samroid.wled.utils.LocationHelper


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothConnectionSheet(
    onDismiss: () -> Unit,
    viewModel: BluetoothConnectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        BluetoothConnectionContent(
            state = state,
            onStartScan = viewModel::startScan,
            onStopScan = viewModel::stopScan,
            onConnect = viewModel::connect,
            onDisconnect = viewModel::disconnect,
            onPing = viewModel::ping,
            onClearLog = viewModel::clearLog,
            onClose = onDismiss,
            viewModel = viewModel
        )
    }
}

@Composable
fun BluetoothConnectionContent(
    state: BluetoothConnectionUiState,
    viewModel: BluetoothConnectionViewModel,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onPing: () -> Unit,
    onClearLog: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bluetooth, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.bluetooth_connection),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
        StatusRow(state.connectionState)

        if (!state.lastResponse.isNullOrBlank()) {
            Text(
                text = state.lastResponse.orEmpty(),
                color = Green,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (state.isScanning) onStopScan() else onStartScan()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (state.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (state.isScanning) stringResource(R.string.stop) else stringResource(R.string.scan))
            }

            if (state.connectionState == TransportConnectionState.CONNECTED) {
                Button(
                    onClick = onPing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),

                ) {
                    Text(stringResource(R.string.ping))
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.disconnect))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.devices), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.devices.isEmpty()) {
                item {
                    Text(
                        text = if (state.isScanning) stringResource(R.string.searching) else stringResource(
                            R.string.start_scan
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
            items(state.devices, key = { it.id }) { device ->
                DeviceRow(
                    device = device,
                    busy = state.connectionState == TransportConnectionState.CONNECTING,
                    isConnecting = state.connectingDeviceId == device.id,
                    onConnect = { onConnect(device.id) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.log), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onClearLog) {
                Text(stringResource(R.string.clear), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(10.dp)
        ) {
            state.logLines.asReversed().forEach { line ->
                Text(
                    text = line,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
        val context = LocalContext.current
        if (state.showEnableLocationPrompt) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissLocationPrompt() },
                title = { Text(stringResource(R.string.location_required_title)) },
                text = { Text(stringResource(R.string.location_required_body)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissLocationPrompt()
                        LocationHelper.openLocationSettings(context)
                    }) {
                        Text(stringResource(R.string.open_location_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissLocationPrompt() }) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusRow(state: TransportConnectionState) {
    val (text, color) = when (state) {
        TransportConnectionState.DISCONNECTED -> stringResource(R.string.disconnected) to MaterialTheme.colorScheme.onSurfaceVariant
        TransportConnectionState.CONNECTING -> stringResource(R.string.connecting) to Color(0xFFF9A825)
        TransportConnectionState.CONNECTED -> stringResource(R.string.connected) to Green
        TransportConnectionState.ERROR -> stringResource(R.string.error) to Red
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun DeviceRow(
    device: TransportDevice,
    busy: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                Text(device.address, color = MaterialTheme.colorScheme.onSurfaceVariant, style =MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = onConnect,
                enabled = !busy,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.connect), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}