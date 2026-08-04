package com.samroid.wled.presentation.udp


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R
import com.samroid.wled.presentation.theme.AppColors.Brand.Green


@Composable
fun UdpScreen(
    viewModel: UdpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            stringResource(R.string.udp_management),
            color = MaterialTheme.colorScheme.onBackground,
            style =MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(16.dp))

        // Stream master toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.udp_stream), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                Text(
                    if (state.streamEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled),
                    color = if (state.streamEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = state.streamEnabled,
                onCheckedChange = viewModel::setStreamEnabled,
                enabled = state.bluetoothConnected && !state.isBusy,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        // Local endpoint info (برای Ambilight روز بعد)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource(R.string.local_ip), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(state.localIp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.port), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(state.port, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.nodes_mapping), color = MaterialTheme.colorScheme.onSurfaceVariant, style =  MaterialTheme.typography.titleSmall)
            TextButton(onClick = viewModel::refreshNodes) {
                Text(stringResource(R.string.refresh), color = MaterialTheme.colorScheme.primary,style = MaterialTheme.typography.bodySmall)
            }
        }

        if (!state.bluetoothConnected) {
            Text(
                "بلوتوث را وصل کنید تا نودها لود شوند",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall
            )
        }

        if (!state.message.isNullOrBlank()) {
            Text(state.message.orEmpty(), color = Green, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.nodes, key = { it.nodeId }) { node ->
                UdpNodeCard(
                    node = node,
                    enabledControls = state.bluetoothConnected && !state.isBusy,
                    onToggle = { viewModel.toggleNodeUdp(node.nodeId, it) },
                    onStartChange = { viewModel.onStartPixelChange(node.nodeId, it) },
                    onEndChange = { viewModel.onEndPixelChange(node.nodeId, it) },
                    onProcessor = { viewModel.onProcessorChange(node.nodeId, it) },
                    onApplyMap = { viewModel.applyMapForNode(node.nodeId) }
                )
            }
        }
    }
}

@Composable
private fun UdpNodeCard(
    node: UdpNodeMapUi,
    enabledControls: Boolean,
    onToggle: (Boolean) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onProcessor: (Int) -> Unit,
    onApplyMap: () -> Unit
) {
    val colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(0.1f),
        focusedTextColor = MaterialTheme.colorScheme.onBackground,
        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(node.name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.id_nodeid, node.nodeId), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = node.enabled,
                onCheckedChange = onToggle,
                enabled = enabledControls,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = node.startPixel,
                onValueChange = onStartChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.start)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                colors = colors
            )
            OutlinedTextField(
                value = node.endPixel,
                onValueChange = onEndChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.end)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(10.dp),
                colors = colors
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.processor), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(10.dp))
            FilterChip(
                selected = node.processorId == 0,
                onClick = { onProcessor(0) },
                label = { Text("Copy") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.3f),
                    selectedLabelColor = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(Modifier.width(6.dp))
            FilterChip(
                selected = node.processorId == 1,
                onClick = { onProcessor(1) },
                label = { Text("Average") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(0.3f),
                    selectedLabelColor = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onApplyMap, enabled = enabledControls) {
                Text("Apply", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}