package com.samroid.wled.presentation.nodecontrol

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samroid.wled.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    searchable: Boolean = true,
    searchHint: String = stringResource(R.string.search)
) {
    var query by remember { mutableStateOf("") }

    LaunchedEffect(expanded) {
        if (!expanded) query = ""
    }

    val filtered = remember(query, items) {
        if (query.isBlank()) items
        else {
            val q = query.trim()
            items.filter { (id, name) ->
                name.contains(q, ignoreCase = true) ||
                        id.toString().contains(q)
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(0.12f),
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .heightIn(max = 320.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (searchable) {
                // داخل منو: فیلد سرچ (کلیک روی آیتم‌ها را نمی‌بندد اگر focus درست باشد)
                Column(
                   modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                searchHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.4f),
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.no_results),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    onClick = {},
                    enabled = false
                )
            } else {
                filtered.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.labelLarge
                            )
//                            Spacer(modifier = Modifier.height(15.dp))
//                            EffectPreviewBar(
//                                effectId = state.rgbEffectId,
//                                paletteId = state.rgbPaletteId,
//                                speed = state.rgbEffectSpeed,
//                                intensity = state.rgbEffectIntensity,
//                                primary = Color(
//                                    state.colorR / 255f,
//                                    state.colorG / 255f,
//                                    state.colorB / 255f
//                                ),
//                                secondary = Color.Black,
//                                height = 24.dp,
//                                ledCount = 56,
//                                animate = true
//                            )
                        },
                        onClick = {
                            onSelect(id)
                            query = ""
                        }
                    )
                }
            }
        }
    }
}