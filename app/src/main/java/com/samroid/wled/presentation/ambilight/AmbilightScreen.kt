package com.samroid.wled.presentation.ambilight

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R
import com.samroid.wled.presentation.theme.AppColors.Brand.Green

@Composable
fun AmbilightScreen(
    onRequestProjection: () -> Unit,
    projectionResultCode: Int? = null,
    projectionData: android.content.Intent? = null,
    projectionDenied: Boolean = false,
    viewModel: AmbilightViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.needsProjection) {
        if (state.needsProjection) onRequestProjection()
    }

    LaunchedEffect(projectionResultCode, projectionData) {
        if (projectionResultCode != null && projectionData != null) {
            viewModel.onProjectionGranted(projectionResultCode, projectionData)
        }
    }

    LaunchedEffect(projectionDenied) {
        if (projectionDenied) viewModel.onProjectionDenied()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            stringResource(R.string.wled_ambient_light),
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (state.bluetoothConnected) {
                stringResource(R.string.connected)
            } else {
                stringResource(R.string.bluetooth_is_not_connected)
            },
            color = if (state.bluetoothConnected) Green else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        // Protocol
        SettingsCard {
            Label(stringResource(R.string.ambient_protocol))
            ChipRow(
                options = PROTOCOL_OPTIONS,
                selected = state.protocol,
                onSelect = viewModel::setProtocol
            )
            Spacer(Modifier.height(12.dp))
            Label(stringResource(R.string.ambient_color_order))
            ChipRow(
                options = COLOR_ORDER_OPTIONS,
                selected = state.colorOrder,
                onSelect = viewModel::setColorOrder
            )
            Spacer(Modifier.height(12.dp))
            Label(stringResource(R.string.fps))
            ChipRow(
                options = FPS_OPTIONS.map { it.toString() },
                selected = state.fps.toString(),
                onSelect = { viewModel.setFps(it.toInt()) }
            )
            Spacer(Modifier.height(12.dp))
            Label(stringResource(R.string.quality))
            ChipRow(
                options = QUALITY_OPTIONS,
                selected = state.quality,
                onSelect = viewModel::setQuality
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ambient_smoothing), color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = state.smoothingEnabled,
                    onCheckedChange = viewModel::setSmoothingEnabled
                )
            }
            if (state.smoothingEnabled) {
                Text(
                    "${state.smoothingPercent.toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = state.smoothingPercent,
                    onValueChange = viewModel::setSmoothingPercent,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.ambient_average_color), color = MaterialTheme.colorScheme.onBackground)
                Switch(
                    checked = state.averageColor,
                    onCheckedChange = viewModel::setAverageColor
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Targets from nodes
        SettingsCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.nodes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = viewModel::refreshTargets,
                    enabled = state.bluetoothConnected && !state.isLoadingTargets
                ) {
                    Text(stringResource(R.string.refresh))
                }
            }

            if (state.isLoadingTargets) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }

            if (state.targets.isEmpty() && !state.isLoadingTargets) {
                Text(
                    stringResource(R.string.for_seeing_nodes_bluetooth_must_be_connected),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            state.targets.forEach { t ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = t.selected,
                        onCheckedChange = { viewModel.toggleTarget(t.nodeId) },
                        enabled = t.ip.isNotBlank()
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${t.name} (#${t.nodeId})",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = buildString {
                                append(if (t.ip.isBlank()) "No IP" else t.ip)
                                append(" · ")
                                append(if (t.online) "Online" else "Offline")
                                append(" · LEDs ${t.startPixel}–${t.endPixel}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!state.message.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                state.message.orEmpty(),
                color = Green,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = viewModel::onStartClicked,
            enabled = !state.isPreparing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRunning) Color(0xFFC62828)
                else MaterialTheme.colorScheme.primary
            )
        ) {
            if (state.isPreparing) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(24.dp)
                )
            } else {
                Text(
                    if (state.isRunning) stringResource(R.string.ambient_stop)
                    else stringResource(R.string.ambient_start),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun Label(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    selectedLabelColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}