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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            stringResource(R.string.wled_ambient_light),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.bluetoothConnected) stringResource(R.string.connected)
            else stringResource(R.string.bluetooth_is_not_connected),
            color = if (state.bluetoothConnected) Green else MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))

        SettingsCard {
            Label("Protocol")
            ChipRow(PROTOCOL_OPTIONS, state.protocol, viewModel::setProtocol)
            Spacer(Modifier.height(10.dp))
            Label("Color order")
            ChipRow(COLOR_ORDER_OPTIONS, state.colorOrder, viewModel::setColorOrder)
            Spacer(Modifier.height(10.dp))
            Label("FPS")
            ChipRow(FPS_OPTIONS.map { it.toString() }, state.fps.toString()) {
                viewModel.setFps(it.toInt())
            }
            Spacer(Modifier.height(10.dp))
            Label("Quality")
            ChipRow(QUALITY_OPTIONS, state.quality, viewModel::setQuality)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Smoothing", color = MaterialTheme.colorScheme.onBackground)
                Switch(state.smoothingEnabled, viewModel::setSmoothingEnabled)
            }
            if (state.smoothingEnabled) {
                Slider(state.smoothingPercent, viewModel::setSmoothingPercent, valueRange = 0f..100f)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Average color", color = MaterialTheme.colorScheme.onBackground)
                Switch(state.averageColor, viewModel::setAverageColor)
            }
            Text(
                "LED start: bottom center (employer)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(12.dp))
        SettingsCard {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Targets (nodes)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = viewModel::refreshTargets) { Text(stringResource(R.string.refresh)) }
            }
            if (state.isLoadingTargets) CircularProgressIndicator(Modifier.padding(8.dp))
            state.targets.forEach { t ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(t.selected, { viewModel.toggleTarget(t.nodeId) }, enabled = t.ip.isNotBlank())
                    Column {
                        Text("${t.name} (#${t.nodeId})", color = MaterialTheme.colorScheme.onBackground)
                        Text(
                            "${t.ip.ifBlank { "No IP" }} · ${if (t.online) "Online" else "Offline"} · ${t.startPixel}–${t.endPixel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!state.message.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(state.message.orEmpty(), color = Green)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = viewModel::onStartClicked,
            enabled = !state.isPreparing,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRunning) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
            )
        ) {
            if (state.isPreparing) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (state.isRunning) "Stop ambient" else "Start ambient")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = viewModel::startFakeUdpOnly,
            enabled = state.bluetoothConnected && !state.isPreparing && !state.isRunning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Fake test: UDP only (no screen)")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun Label(t: String) {
    Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun ChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(o) })
        }
    }
}