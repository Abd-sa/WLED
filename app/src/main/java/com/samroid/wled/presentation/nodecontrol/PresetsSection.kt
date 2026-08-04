package com.samroid.wled.presentation.nodecontrol

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samroid.wled.R
import com.samroid.wled.presentation.theme.AppColors.Brand.Green
import com.samroid.wled.presentation.theme.AppColors.Brand.Purple

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
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // ---- Save ----
        SectionCard {
            Text(
                "Save Preset",
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

        // ---- Load ----
        SectionCard {
            Text(
                "Load Preset",
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
            style =MaterialTheme.typography.labelSmall
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
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..6).forEach { id ->
            val isBoot = id == 1
            val saved = id in selectedHint

            Button (
                onClick = { onClick(id) },
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        highlightBoot && isBoot -> MaterialTheme.colorScheme.primary
                        saved -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (!saved && !(highlightBoot && isBoot)) {
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f)
                    )
                } else null
            ) {
                Text(
                    text = id.toString(),

                    style =MaterialTheme.typography.titleMedium.copy(fontWeight = if (isBoot) FontWeight.Bold else FontWeight.Medium)
                )
            }
        }
    }
}