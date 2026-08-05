package com.samroid.wled.presentation.provision

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun ProvisionScreen(
    onFinished: () -> Unit = {},
    viewModel: ProvisionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.bluetoothConnected) {
        if (state.bluetoothConnected) viewModel.startProvisionIfNeeded()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(stringResource(R.string.provision), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))

        if (!state.bluetoothConnected) {
            Text(stringResource(R.string.first_connect_bluetooth_from_dashboard), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
            return
        }

        Stepper(current = state.currentStep.index)
        Spacer(Modifier.height(20.dp))

        when (state.currentStep) {
            ProvisionStep.GPIO -> StepGpio(
                state = state,
                onGpioChange = viewModel::onGpioChange,
                onTest = viewModel::testGpio,
                onConfirm = viewModel::confirmGpio
            )
            ProvisionStep.COLOR -> StepColor(
                state = state,
                onSelect = viewModel::onColorOrderSelect,
                onTest = viewModel::testColor,
                onConfirm = viewModel::confirmColor
            )
            ProvisionStep.LENGTH -> StepLength(
                state = state,
                onLengthChange = viewModel::onLengthChange,
                onTest = viewModel::testLength,
                onConfirm = viewModel::confirmLength
            )
            ProvisionStep.OUTPUT -> StepOutput(
                state = state,
                onWarmChange = viewModel::onCctWarmChange,
                onCoolChange = viewModel::onCctCoolChange,
                onTest = viewModel::testOutput,
                onConfirm = viewModel::confirmOutput,
                onSkip = viewModel::skipOutput
            )
            ProvisionStep.STORE -> StepStore(
                state = state,
                onNodeIdChange = viewModel::onStoreNodeIdChange,
                onStore = { viewModel.storeAndFinish(onFinished) }
            )
        }

        if (!state.message.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(state.message.orEmpty(), color = Green, style = MaterialTheme.typography.titleSmall)
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = viewModel::back,
                enabled = state.currentStep.index > 1 && !state.isBusy
            ) {
                Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = viewModel::cancel) {
                Text(stringResource(R.string.skip_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Stepper(current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { i ->
            val active = i <= current
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$i",
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            if (i < 5) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .background(if (i < current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
private fun StepGpio(
    state: ProvisionUiState,
    onGpioChange: (String) -> Unit,
    onTest: () -> Unit,
    onConfirm: () -> Unit
) {
    Text("Step 1 – GPIO", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.gpio_0_255_confirm),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
    }

    Field(
        value = state.gpioValue,
        onValueChange = onGpioChange,
        label = "GPIO Number"
    )

    Spacer(Modifier.height(20.dp))
    TestConfirmRow(busy = state.isBusy, onTest = onTest, onConfirm = onConfirm)
}

@Composable
private fun StepColor(
    state: ProvisionUiState,
    onSelect: (Int) -> Unit,
    onTest: () -> Unit,
    onConfirm: () -> Unit
) {
    Text("Step 2 – Color", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.led),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(20.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        COLOR_ORDERS.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (id, name) ->
                    val selected = state.colorOrder == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(0.25f) else MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary.copy(
                                    0.08f
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(id) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$id", color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(name, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                // اگر ردیف ناقص بود
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    TestConfirmRow(busy = state.isBusy, onTest = onTest, onConfirm = onConfirm)
}

@Composable
private fun StepLength(
    state: ProvisionUiState,
    onLengthChange: (String) -> Unit,
    onTest: () -> Unit,
    onConfirm: () -> Unit
) {
    val len = state.lengthValue.toIntOrNull()?.coerceIn(1, 300) ?: 1

    Text(stringResource(R.string.step_3_length), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.led_1_300), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(24.dp))

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            "$len",
            color = MaterialTheme.colorScheme.primary,
            style =MaterialTheme.typography.displayLarge,
        )
    }
    Text(
        "LEDs",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style =MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
        // center roughly
    )

    Slider(
        value = len.toFloat(),
        onValueChange = { onLengthChange(it.toInt().toString()) },
        valueRange = 1f..300f,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.onPrimary.copy(0.1f)
        )
    )

    Field(
        value = state.lengthValue,
        onValueChange = onLengthChange,
        label = "LED Count"
    )

    Spacer(Modifier.height(20.dp))
    TestConfirmRow(busy = state.isBusy, onTest = onTest, onConfirm = onConfirm)
}

@Composable
private fun StepOutput(
    state: ProvisionUiState,
    onWarmChange: (String) -> Unit,
    onCoolChange: (String) -> Unit,
    onTest: () -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit
) {
    Text("Step 4 – Output (CCT)", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.gpio_cct_warm_cool_0_255_cct_skip),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(20.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = state.cctWarmGpio,
            onValueChange = onWarmChange,
            modifier = Modifier.weight(1f),
            label = { Text("CCT Warm GPIO") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )
        OutlinedTextField(
            value = state.cctCoolGpio,
            onValueChange = onCoolChange,
            modifier = Modifier.weight(1f),
            label = { Text("CCT Cool GPIO") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = fieldColors()
        )
    }

    Spacer(Modifier.height(24.dp))
    TestConfirmRow(busy = state.isBusy, onTest = onTest, onConfirm = onConfirm)

    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Skip CCT (بدون خروجی CCT)", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}



@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(0.12f),
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
)
@Composable
private fun StepStore(
    state: ProvisionUiState,
    onNodeIdChange: (String) -> Unit,
    onStore: () -> Unit
) {
    Text("Step 5 – Store", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.node_id_master),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall
    )
    Spacer(Modifier.height(24.dp))

    OutlinedTextField(
        value = state.storeNodeName,
        onValueChange = onNodeIdChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.node_name)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors()
    )
    Spacer(Modifier.height(24.dp))

    OutlinedTextField(
        value = state.storeNodeId,
        onValueChange = onNodeIdChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.node_id_1_250)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors()
    )

    Spacer(Modifier.height(16.dp))

    // خلاصه سریع
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.summary), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(10.dp))
        SummaryRow(stringResource(R.string.gpio), state.gpioValue)
        SummaryRow(
            stringResource(R.string.color),
            COLOR_ORDERS.firstOrNull { it.first == state.colorOrder }?.second ?: "-"
        )
        SummaryRow(stringResource(R.string.length), state.lengthValue)
        SummaryRow(stringResource(R.string.cct_warm_cool), "${state.cctWarmGpio} / ${state.cctCoolGpio}")
        SummaryRow(stringResource(R.string.nodeid), state.storeNodeId)
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = onStore,
        enabled = !state.isBusy,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (state.isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
        }
        Text("Store & Finish", style =MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
        Text(value, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall)
    }
}


@Composable
private fun TestConfirmRow(
    busy: Boolean,
    onTest: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onTest,
            enabled = !busy,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Test")
        }
        Button(
            onClick = onConfirm,
            enabled = !busy,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.confirm), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary.copy(0.12f),
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}