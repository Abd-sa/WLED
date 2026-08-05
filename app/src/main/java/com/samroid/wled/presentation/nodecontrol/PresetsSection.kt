package com.samroid.wled.presentation.nodecontrol

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samroid.wled.R
import com.samroid.wled.presentation.theme.AppColors.Brand.Green

@Composable
fun PresetsSection(
    savedPresets: Set<Int>,
    busy: Boolean,
    message: String?,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit
) {
    Column {
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                color = Green,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        SectionCard {
            Text(
                stringResource(R.string.save_preset),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.preset_1_will_apply_on_boot),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(14.dp))
            PresetButtonRow(
                selectedHint = savedPresets,
                highlightBoot = true,
                enabled = !busy,
                onClick = onSave
            )
        }

        Spacer(Modifier.height(14.dp))

        SectionCard {
            Text(
                stringResource(R.string.load_preset),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(14.dp))
            PresetButtonRow(
                selectedHint = savedPresets,
                highlightBoot = false,
                enabled = !busy,
                onClick = onLoad
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.apply_on_boot),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun PresetButtonRow(
    selectedHint: Set<Int>,
    highlightBoot: Boolean,
    enabled: Boolean,
    onClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..6).forEach { id ->
            val isBoot = id == 1
            val saved = id in selectedHint
            val filled = saved || (highlightBoot && isBoot)

            if (filled) {
                Button(
                    onClick = { onClick(id) },
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (highlightBoot && isBoot) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        },
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        id.toString(),
                        fontWeight = if (isBoot) FontWeight.Bold else FontWeight.Medium
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onClick(id) },
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.4f))
                ) {
                    Text(id.toString())
                }
            }
        }
    }
}