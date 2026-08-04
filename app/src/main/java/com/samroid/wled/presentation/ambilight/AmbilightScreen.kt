package com.samroid.wled.presentation.ambilight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samroid.wled.R

import com.samroid.wled.presentation.theme.AppColors.Brand.Green



@Composable
fun AmbilightScreen(
    onRequestProjection: () -> Unit,
    viewModel: AmbilightViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "Ambient Light",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(16.dp))

        // پیش‌نمایش تزئینی (روز ۱۲ فریم واقعی)
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(160.dp)
//                .clip(RoundedCornerShape(18.dp))
//                .background(
//                    Brush.horizontalGradient(
//                        listOf(Color(0xFF3F51B5), Color(0xFF9C27B0), Color(0xFFFF5722))
//                    )
//                ),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                if (state.isRunning) "Streaming…" else "Preview",
//                color = Color.White.copy(alpha = 0.9f),
//                fontWeight = FontWeight.Medium
//            )
//        }
        Button(modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF3F51B5), Color(0xFF9C27B0), Color(0xFFFF5722))
                )
            ),
            onClick = {
            if (state.isRunning) viewModel.stop()
            else onRequestProjection()
        }){
            Text(
                if (state.isRunning) "Streaming…" else "Preview",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(18.dp))

        SettingsCard {
            Label(stringResource(R.string.capture_source))
            ChipRow(
                options = SOURCE_OPTIONS,
                selected = state.captureSource,
                onSelect = viewModel::setCaptureSource
            )

            Spacer(Modifier.height(14.dp))
            Label(stringResource(R.string.fps))
            ChipRow(
                options = FPS_OPTIONS.map { it.toString() },
                selected = state.fps.toString(),
                onSelect = { viewModel.setFps(it.toInt()) }
            )

            Spacer(Modifier.height(14.dp))
            Label(stringResource(R.string.quality))
            ChipRow(
                options = QUALITY_OPTIONS,
                selected = state.quality,
                onSelect = viewModel::setQuality
            )

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Label(stringResource(R.string.smoothing))
                Text(
                    "${state.smoothing.toInt()}%",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Slider(
                value = state.smoothing,
                onValueChange = viewModel::setSmoothing,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(0.1f)
                )
            )
        }

        Spacer(Modifier.height(12.dp))

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.target), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${state.targetHost}:${state.targetPort}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    if (state.isRunning) stringResource(R.string.running) else stringResource(R.string.idle),
                    color = if (state.isRunning) Green else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!state.message.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(state.message.orEmpty(), color = Green, style = MaterialTheme.typography.titleSmall)
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = viewModel::toggle,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRunning) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (state.isRunning) "Stop" else "Start",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        content()
    }
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