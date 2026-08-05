package com.samroid.wled.presentation.nodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R
import com.samroid.wled.presentation.theme.AppColors.Brand.Green

@Composable
fun NodeInfoScreen(
    onBack: () -> Unit,
    onOpenControl: (Int) -> Unit = {},
    viewModel: NodeInfoViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = state.info
    val dash = stringResource(R.string.empty_dash)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                stringResource(R.string.node_info),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = viewModel::refresh) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.refresh),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = info?.nodeName?.ifBlank { null }
                ?: stringResource(R.string.node, state.nodeId),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleLarge
        )

        if (!state.message.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(state.message.orEmpty(), color = Green, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.isLoading && info == null) {
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            return
        }

        Spacer(Modifier.height(16.dp))

        InfoSection(title = stringResource(R.string.information)) {
            // Employer required fields
            InfoRow(stringResource(R.string.id_nodeid), state.nodeId.toString())
            InfoRow(stringResource(R.string.device_id), info?.deviceId?.ifBlank { dash } ?: dash)
            InfoRow(stringResource(R.string.ip_address), info?.ip?.ifBlank { dash } ?: dash)
            InfoRow(
                stringResource(R.string.rssi),
                info?.let { "${it.rssi} dBm" } ?: dash
            )
            InfoRow(
                stringResource(R.string.led_count),
                info?.ledCount?.toString() ?: dash
            )
            InfoRow(
                stringResource(R.string.cct_enabled),
                info?.let {
                    if (it.cctEnabled) stringResource(R.string.yes) else stringResource(R.string.no)
                } ?: dash
            )
            InfoRow(
                stringResource(R.string.udp_enabled),
                info?.let {
                    if (it.udpEnabled) stringResource(R.string.yes) else stringResource(R.string.no)
                } ?: dash
            )
            InfoRow(
                stringResource(R.string.processor_id),
                info?.let {
                    val name = if (it.processorId == 1) {
                        stringResource(R.string.average)
                    } else {
                        stringResource(R.string.copy)
                    }
                    "${it.processorId} ($name)"
                } ?: dash
            )
            InfoRow(
                stringResource(R.string.start_pixel),
                info?.startPixel?.toString() ?: dash
            )
            InfoRow(
                stringResource(R.string.end_pixel),
                info?.endPixel?.toString() ?: dash
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { onOpenControl(state.nodeId) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                stringResource(R.string.open_control),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleSmall
        )
    }
}