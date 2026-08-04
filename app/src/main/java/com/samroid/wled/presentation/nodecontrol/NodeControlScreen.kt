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
import androidx.compose.foundation.layout.width
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
fun NodeControlScreen(
    onBack: () -> Unit,
    onOpenPresets: () -> Unit = {},
    viewModel: NodeControlViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = MaterialTheme.colorScheme.onBackground)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.node_control),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    stringResource(R.string.node_id, state.nodeId),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = if (state.online) stringResource(R.string.online) else stringResource(R.string.offline),
                color = if (state.online) Green else Color(0xFFE74C3C),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (state.online) Green.copy(0.12f) else Color(0xFFE74C3C).copy(0.12f)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Tabs ساده Control | Presets
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
            // ---- RGB ----
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.rgb), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                    PowerButton(on = state.rgbOn, onClick = { viewModel.setRgbOn(!state.rgbOn) })
                }

                Spacer(Modifier.height(14.dp))
                LabelValue(stringResource(R.string.brightness), state.brightnessRgb.toInt().toString())
                ControlSlider(
                    value = state.brightnessRgb,
                    onValueChange = viewModel::setBrightnessRgb,
                    onValueChangeFinished = viewModel::commitBrightnessRgb
                )

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.color), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                // پیش‌نمایش رنگ
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
                        .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(0.08f), RoundedCornerShape(10.dp))
                )

                Spacer(Modifier.height(10.dp))
                LabelValue(stringResource(R.string.r), state.colorR.toInt().toString())
                ControlSlider(
                    value = state.colorR,
                    color = Color(0xFFE74C3C),
                    onValueChange = { r -> viewModel.setColor(r, state.colorG, state.colorB) },
                    onValueChangeFinished = viewModel::commitColor
                )
                LabelValue(stringResource(R.string.g), state.colorG.toInt().toString())
                ControlSlider(
                    value = state.colorG,
                    color = Color(0xFF2ECC71),
                    onValueChange = { g -> viewModel.setColor(state.colorR, g, state.colorB) },
                    onValueChangeFinished = viewModel::commitColor
                )
                LabelValue(stringResource(R.string.b), state.colorB.toInt().toString())
                ControlSlider(
                    value = state.colorB,
                    color = Color(0xFF3498DB),
                    onValueChange = { b -> viewModel.setColor(state.colorR, state.colorG, b) },
                    onValueChangeFinished = viewModel::commitColor
                )

                Spacer(Modifier.height(14.dp))
                SectionCard {
                    Text(
                        "Effect",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(12.dp))

                    // ---- Effect dropdown ----
                    Text("Effect", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    DropdownSelector(
                        label = EffectCatalog.effectLabel(state.effectId),
                        expanded = state.effectExpanded,
                        onExpandedChange = viewModel::toggleEffectMenu,
                        items = (0..159).map { it to EffectCatalog.effectLabel(it) },
                        onSelect = viewModel::setEffectId
                    )

                    Spacer(Modifier.height(12.dp))

                    // ---- Palette dropdown ----
                    Text("Palette", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    DropdownSelector(
                        label = EffectCatalog.paletteLabel(state.paletteId),
                        expanded = state.paletteExpanded,
                        onExpandedChange = viewModel::togglePaletteMenu,
                        items = (0..71).map { it to EffectCatalog.paletteLabel(it) },
                        onSelect = viewModel::setPaletteId
                    )

                    Spacer(Modifier.height(14.dp))
                    LabelValue("Speed", state.effectSpeed.toInt().toString())
                    ControlSlider(
                        value = state.effectSpeed,
                        onValueChange = viewModel::setEffectSpeed,
                        onValueChangeFinished = viewModel::commitEffectSpeed
                    )

                    Spacer(Modifier.height(8.dp))
                    LabelValue("Intensity", state.effectIntensity.toInt().toString())
                    ControlSlider(
                        value = state.effectIntensity,
                        onValueChange = viewModel::setEffectIntensity,
                        onValueChangeFinished = viewModel::commitEffectIntensity
                    )
                }
            }

            // ---- CCT (شرطی) ----
            if (state.cctEnabled) {
                Spacer(Modifier.height(14.dp))
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.cct), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium)
                        PowerButton(on = state.cctOn, onClick = { viewModel.setCctOn(!state.cctOn) })
                    }

                    Spacer(Modifier.height(14.dp))
                    LabelValue(stringResource(R.string.brightness), state.brightnessCct.toInt().toString())
                    ControlSlider(
                        value = state.brightnessCct,
                        onValueChange = viewModel::setBrightnessCct,
                        onValueChangeFinished = viewModel::commitCct
                    )

                    Spacer(Modifier.height(10.dp))
                    LabelValue(stringResource(R.string.cct), state.cctValue.toInt().toString())
                    // گرادیان گرم → سرد
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFB74D), MaterialTheme.colorScheme.onPrimary, Color(0xFF90CAF9))
                                )
                            )
                    )
                    ControlSlider(
                        value = state.cctValue,
                        color = Color(0xFFFFB74D),
                        onValueChange = viewModel::setCctValue,
                        onValueChangeFinished = viewModel::commitCct
                    )
                }
            }
        } else {
            if (state.selectedTab == ControlTab.PRESETS) {
                PresetsSection(
                    savedPresets = state.savedPresets,
                    busy = state.isPresetBusy,
                    message = state.lastPresetAction,
                    onSave = viewModel::savePreset,
                    onLoad = viewModel::loadPreset
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(0.25f) else Color.Transparent)
            .clickableSafe(onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,

            style =MaterialTheme.typography.labelLarge.copy(fontWeight = if(selected) FontWeight.SemiBold else FontWeight.Normal )
        )
    }
}

@Composable
private fun PowerButton(on: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (on) MaterialTheme.colorScheme.primary.copy(0.25f) else MaterialTheme.colorScheme.onPrimary.copy(0.06f))
    ) {
        Icon(
            Icons.Outlined.PowerSettingsNew,
            null,
            tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
        Text(value, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ControlSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = 0f..255f,
        colors = SliderDefaults.colors(
            thumbColor = color,
            activeTrackColor = color,
            inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(0.1f)
        )
    )
}

@Composable

private fun Modifier.clickableSafe(
    onClick: () -> Unit
): Modifier =
    clip(RoundedCornerShape(10.dp))
        .clickable(onClick = onClick)