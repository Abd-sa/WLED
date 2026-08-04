package com.samroid.wled.presentation.connection.wifi


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R
import com.samroid.wled.presentation.theme.AppColors.Brand.Green
import com.samroid.wled.presentation.theme.AppColors.Brand.Red



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiConfigSheet(
    onDismiss: () -> Unit,
    viewModel: WifiConfigViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        WifiConfigContent(
            state = state,
            onSsidChange = viewModel::onSsidChange,
            onPasswordChange = viewModel::onPasswordChange,
            onBaseIp1Change = viewModel::onBaseIp1Change,
            onBaseIp2Change = viewModel::onBaseIp2Change,
            onBaseIp3Change = viewModel::onBaseIp3Change,
            onSendConfig = viewModel::sendNetworkConfig,
            onConnectWifi = viewModel::connectWifi,
            onClose = onDismiss
        )
    }
}

@Composable
fun WifiConfigContent(
    state: WifiConfigUiState,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onBaseIp1Change: (String) -> Unit,
    onBaseIp2Change: (String) -> Unit,
    onBaseIp3Change: (String) -> Unit,
    onSendConfig: () -> Unit,
    onConnectWifi: () -> Unit,
    onClose: () -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f),
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.wifi_master),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(6.dp))

        // وضعیت بلوتوث
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (state.bluetoothConnected) Green else Red)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (state.bluetoothConnected) stringResource(R.string.bluetooth_is_connected_you_are_ready_to_set_config)
                else stringResource(R.string.bluetooth_is_not_connected),
                color = if (state.bluetoothConnected) Green else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.ssid,
            onValueChange = onSsidChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.ssid)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors
        )

        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.base_ip_192_168_1), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IpOctetField(state.baseIp1, onBaseIp1Change, fieldColors, Modifier.weight(1f))
            Text(".", color = MaterialTheme.colorScheme.onSurfaceVariant)
            IpOctetField(state.baseIp2, onBaseIp2Change, fieldColors, Modifier.weight(1f))
            Text(".", color = MaterialTheme.colorScheme.onSurfaceVariant)
            IpOctetField(state.baseIp3, onBaseIp3Change, fieldColors, Modifier.weight(1f))
            Text(".x", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
        }

        if (!state.lastMessage.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.lastMessage.orEmpty(),
                color = Green,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onSendConfig,
            enabled = state.bluetoothConnected && !state.isSendingConfig,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (state.isSendingConfig) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("ارسال NETWORK_CONFIG", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onConnectWifi,
            enabled = state.bluetoothConnected && !state.isConnectingWifi,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (state.isConnectingWifi) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("WIFI_CONNECT", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IpOctetField(
    value: String,
    onValueChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = colors
    )
}