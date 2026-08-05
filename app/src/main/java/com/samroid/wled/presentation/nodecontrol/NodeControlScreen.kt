package com.samroid.wled.presentation.nodecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.samroid.wled.R
import com.samroid.wled.presentation.theme.AppColors.Brand.Green

@Composable
fun NodeControlScreen(
    onBack: () -> Unit,
    viewModel: NodeControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colorController = rememberColorPickerController()

    val effectItems = remember { (0..159).map { it to EffectCatalog.effectLabel(it) } }
    val paletteItems = remember { (0..71).map { it to EffectCatalog.paletteLabel(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.node_control),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    stringResource(R.string.node, state.nodeId),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = if (state.online) stringResource(R.string.online)
                else stringResource(R.string.offline),
                color = if (state.online) Green else Color(0xFFE74C3C),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (state.online) Green.copy(0.12f)
                        else Color(0xFFE74C3C).copy(0.12f)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        ) {
            TabChip(
                text = stringResource(R.string.control),
                selected = state.selectedTab == ControlTab.CONTROL,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.selectTab(ControlTab.CONTROL) }
            )
            TabChip(
                text = stringResource(R.string.presets),
                selected = state.selectedTab == ControlTab.PRESETS,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.selectTab(ControlTab.PRESETS) }
            )
        }

        Spacer(Modifier.height(20.dp))

        if (state.selectedTab == ControlTab.CONTROL) {
            // ---------- RGB ----------
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.rgb),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                    PowerButton(
                        on = state.rgbOn,
                        onClick = { viewModel.setRgbOn(!state.rgbOn) }
                    )
                }

                if (state.rgbOn) {
                    Spacer(Modifier.height(14.dp))
                    LabelValue(
                        stringResource(R.string.brightness),
                        state.brightnessRgb.toInt().toString()
                    )
                    ControlSlider(
                        value = state.brightnessRgb,
                        onValueChange = viewModel::setBrightnessRgb,
                        onValueChangeFinished = viewModel::commitBrightnessRgb
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.color),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))

                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 8.dp),
                        controller = colorController,
                        initialColor = Color(
                            red = state.colorR / 255f,
                            green = state.colorG / 255f,
                            blue = state.colorB / 255f
                        ),
                        onColorChanged = { envelope ->
                            val c = envelope.color
                            viewModel.setColor(
                                c.red * 255f,
                                c.green * 255f,
                                c.blue * 255f
                            )
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Color(
                                    red = state.colorR / 255f,
                                    green = state.colorG / 255f,
                                    blue = state.colorB / 255f
                                )
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(0.3f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { viewModel.commitColor() }
                    )

                    Spacer(Modifier.height(10.dp))
                    LabelValue(stringResource(R.string.r), state.colorR.toInt().toString())
                    ControlSlider(
                        value = state.colorR,
                        color = Color(0xFFE74C3C),
                        onValueChange = { r ->
                            viewModel.setColor(r, state.colorG, state.colorB)
                        },
                        onValueChangeFinished = viewModel::commitColor
                    )
                    LabelValue(stringResource(R.string.g), state.colorG.toInt().toString())
                    ControlSlider(
                        value = state.colorG,
                        color = Color(0xFF2ECC71),
                        onValueChange = { g ->
                            viewModel.setColor(state.colorR, g, state.colorB)
                        },
                        onValueChangeFinished = viewModel::commitColor
                    )
                    LabelValue(stringResource(R.string.b), state.colorB.toInt().toString())
                    ControlSlider(
                        value = state.colorB,
                        color = Color(0xFF3498DB),
                        onValueChange = { b ->
                            viewModel.setColor(state.colorR, state.colorG, b)
                        },
                        onValueChangeFinished = viewModel::commitColor
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.rgb_effect),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    DropdownSelector(
                        label = EffectCatalog.effectLabel(state.rgbEffectId),
                        expanded = state.rgbEffectExpanded,
                        onExpandedChange = viewModel::toggleRgbEffectMenu,
                        items = effectItems,
                        onSelect = viewModel::setRgbEffectId
                    )
                    Spacer(Modifier.height(10.dp))
                    DropdownSelector(
                        label = EffectCatalog.paletteLabel(state.rgbPaletteId),
                        expanded = state.rgbPaletteExpanded,
                        onExpandedChange = viewModel::toggleRgbPaletteMenu,
                        items = paletteItems,
                        onSelect = viewModel::setRgbPaletteId
                    )
                    Spacer(Modifier.height(10.dp))
                    LabelValue(
                        stringResource(R.string.speed),
                        state.rgbEffectSpeed.toInt().toString()
                    )
                    ControlSlider(
                        value = state.rgbEffectSpeed,
                        onValueChange = viewModel::setRgbEffectSpeed,
                        onValueChangeFinished = viewModel::commitRgbEffectSpeed
                    )
                    LabelValue(
                        stringResource(R.string.intensity),
                        state.rgbEffectIntensity.toInt().toString()
                    )
                    ControlSlider(
                        value = state.rgbEffectIntensity,
                        onValueChange = viewModel::setRgbEffectIntensity,
                        onValueChangeFinished = viewModel::commitRgbEffectIntensity
                    )
                }
            }

            // ---------- CCT (only if enabled) ----------
            if (state.cctEnabled) {
                Spacer(Modifier.height(14.dp))
                SectionCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.cct),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium
                        )
                        PowerButton(
                            on = state.cctOn,
                            onClick = { viewModel.setCctOn(!state.cctOn) }
                        )
                    }

                    if (state.cctOn) {
                        Spacer(Modifier.height(14.dp))
                        LabelValue(
                            stringResource(R.string.brightness),
                            state.brightnessCct.toInt().toString()
                        )
                        ControlSlider(
                            value = state.brightnessCct,
                            onValueChange = viewModel::setBrightnessCct,
                            onValueChangeFinished = viewModel::commitCct
                        )

                        Spacer(Modifier.height(10.dp))
                        LabelValue(
                            stringResource(R.string.cct),
                            state.cctValue.toInt().toString()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFFFB74D),
                                            Color.White,
                                            Color(0xFF90CAF9)
                                        )
                                    )
                                )
                        )
                        ControlSlider(
                            value = state.cctValue,
                            color = Color(0xFFFFB74D),
                            onValueChange = viewModel::setCctValue,
                            onValueChangeFinished = viewModel::commitCct
                        )

                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.cct_effect),
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        DropdownSelector(
                            label = EffectCatalog.effectLabel(state.cctEffectId),
                            expanded = state.cctEffectExpanded,
                            onExpandedChange = viewModel::toggleCctEffectMenu,
                            items = effectItems,
                            onSelect = viewModel::setCctEffectId
                        )
                        Spacer(Modifier.height(10.dp))
                        DropdownSelector(
                            label = EffectCatalog.paletteLabel(state.cctPaletteId),
                            expanded = state.cctPaletteExpanded,
                            onExpandedChange = viewModel::toggleCctPaletteMenu,
                            items = paletteItems,
                            onSelect = viewModel::setCctPaletteId
                        )
                        Spacer(Modifier.height(10.dp))
                        LabelValue(
                            stringResource(R.string.speed),
                            state.cctEffectSpeed.toInt().toString()
                        )
                        ControlSlider(
                            value = state.cctEffectSpeed,
                            onValueChange = viewModel::setCctEffectSpeed,
                            onValueChangeFinished = viewModel::commitCctEffectSpeed
                        )
                        LabelValue(
                            stringResource(R.string.intensity),
                            state.cctEffectIntensity.toInt().toString()
                        )
                        ControlSlider(
                            value = state.cctEffectIntensity,
                            onValueChange = viewModel::setCctEffectIntensity,
                            onValueChangeFinished = viewModel::commitCctEffectIntensity
                        )
                    }
                }
            }
        } else {
            PresetsSection(
                savedPresets = state.savedPresets,
                busy = state.isPresetBusy,
                message = state.lastPresetAction,
                onSave = viewModel::savePreset,
                onLoad = viewModel::loadPreset
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
    Spacer(Modifier.height(4.dp))
}

@Composable
fun ControlSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    valueRange: ClosedFloatingPointRange<Float> = 0f..255f
) {
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color,
            inactiveTrackColor = color.copy(alpha = 0.24f)
        )
    )
}

@Composable
fun PowerButton(on: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (on) MaterialTheme.colorScheme.primary.copy(0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Icon(
            Icons.Outlined.PowerSettingsNew,
            contentDescription = null,
            tint = if (on) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(0.15f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall
        )
    }
}