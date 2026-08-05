package com.samroid.wled.presentation.provision

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samroid.wled.R
import com.samroid.wled.data.repository.LocalNodeRepository
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.NodeListItem
import com.samroid.wled.domain.model.TransportConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProvisionViewModel @Inject constructor(
    private val transport: DeviceTransport,
    private val localNodes: LocalNodeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProvisionUiState())
    val uiState: StateFlow<ProvisionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transport.transportConnectionState.collect { state ->
                _uiState.update {
                    it.copy(bluetoothConnected = state == TransportConnectionState.CONNECTED)
                }
            }
        }
        viewModelScope.launch {
            transport.lastResponse.collect { msg ->
                if (msg != null) _uiState.update { it.copy(message = msg) }
            }
        }
    }

    /**
     * Starts provision session: PROVISION then optional SCAN_WLED.
     */
    fun startProvisionIfNeeded() {
        val s = _uiState.value
        if (s.provisionStarted || !s.bluetoothConnected) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, isSearching = true, message = null) }

            val ok = transport.provision()
            if (!ok) {
                _uiState.update {
                    it.copy(isBusy = false, message = context.getString(R.string.provision_failed))
                }
                return@launch
            }
            runCatching { transport.scanWled() }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    provisionStarted = true,
                    currentStep = ProvisionStep.GPIO,
                    message = context.getString(R.string.provision_started)
                )
            }
        }
    }

    // ---- inputs ----

    fun onGpioChange(v: String) {
        _uiState.update { it.copy(gpioValue = v.filter { c -> c.isDigit() }.take(3)) }
    }

    fun onColorOrderSelect(order: Int) {
        _uiState.update { it.copy(colorOrder = order.coerceIn(0, 5)) }
    }

    fun onLengthChange(v: String) {
        _uiState.update { it.copy(lengthValue = v.filter { c -> c.isDigit() }.take(3)) }
    }

    fun onCctWarmChange(v: String) {
        _uiState.update { it.copy(cctWarmGpio = v.filter { c -> c.isDigit() }.take(3)) }
    }

    fun onCctCoolChange(v: String) {
        _uiState.update { it.copy(cctCoolGpio = v.filter { c -> c.isDigit() }.take(3)) }
    }

    fun onStoreNodeIdChange(v: String) {
        _uiState.update { it.copy(storeNodeId = v.filter { c -> c.isDigit() }.take(3)) }
    }

    fun onStoreNodeNameChange(v: String) {
        _uiState.update { it.copy(storeNodeName = v.take(32)) }
    }

    fun goToStep(step: ProvisionStep) {
        _uiState.update { it.copy(currentStep = step, message = null) }
    }

    fun back() {
        val idx = _uiState.value.currentStep.index
        if (idx > 1) goToStep(ProvisionStep.fromIndex(idx - 1))
    }

    // ---- Step 1 GPIO ----

    fun testGpio() {
        val pin = _uiState.value.gpioValue.toIntOrNull()
        if (pin == null || pin !in 0..255) {
            _uiState.update {
                it.copy(message = context.getString(R.string.gpio_range_error))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.gpioValue(pin)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = context.getString(R.string.gpio_value_sent)
                )
            }
        }
    }

    /** Confirm allowed without Test (employer note steps 1–4). */
    fun confirmGpio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.gpioConfirm()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    currentStep = if (ok) ProvisionStep.COLOR else it.currentStep,
                    message = if (ok) {
                        context.getString(R.string.gpio_confirmed)
                    } else {
                        context.getString(R.string.gpio_confirm_failed)
                    }
                )
            }
        }
    }

    // ---- Step 2 Color ----

    fun testColor() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.colorValue(_uiState.value.colorOrder.coerceIn(0, 5))
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = context.getString(R.string.color_value_sent)
                )
            }
        }
    }

    fun confirmColor() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.colorConfirm()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    currentStep = if (ok) ProvisionStep.LENGTH else it.currentStep,
                    message = if (ok) {
                        context.getString(R.string.color_confirmed)
                    } else {
                        context.getString(R.string.color_confirm_failed)
                    }
                )
            }
        }
    }

    // ---- Step 3 Length ----

    fun testLength() {
        val len = _uiState.value.lengthValue.toIntOrNull()
        if (len == null || len !in 1..300) {
            _uiState.update {
                it.copy(message = context.getString(R.string.length_range_error))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.lengthValue(len)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = context.getString(R.string.length_value_sent)
                )
            }
        }
    }

    fun confirmLength() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.lengthConfirm()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    currentStep = if (ok) ProvisionStep.OUTPUT else it.currentStep,
                    message = if (ok) {
                        context.getString(R.string.length_confirmed)
                    } else {
                        context.getString(R.string.length_confirm_failed)
                    }
                )
            }
        }
    }

    // ---- Step 4 Output (CCT pins) ----

    fun testOutput() {
        val warm = _uiState.value.cctWarmGpio.toIntOrNull()
        val cool = _uiState.value.cctCoolGpio.toIntOrNull()
        if (warm == null || cool == null || warm !in 0..255 || cool !in 0..255) {
            _uiState.update {
                it.copy(message = context.getString(R.string.cct_pins_range_error))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.outputValue(warm, cool)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = context.getString(R.string.output_value_sent)
                )
            }
        }
    }

    fun confirmOutput() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.outputConfirm()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    currentStep = if (ok) ProvisionStep.STORE else it.currentStep,
                    message = if (ok) {
                        context.getString(R.string.output_confirmed)
                    } else {
                        context.getString(R.string.output_confirm_failed)
                    }
                )
            }
        }
    }

    /** No CCT on node → go to Store without OUTPUT_VALUE. */
    fun skipOutput() {
        _uiState.update {
            it.copy(
                currentStep = ProvisionStep.STORE,
                message = context.getString(R.string.cct_skipped)
            )
        }
    }

    // ---- Step 5 Store ----

    fun storeAndFinish(onFinished: () -> Unit = {}) {
        val id = _uiState.value.storeNodeId.toIntOrNull()
        val name = _uiState.value.storeNodeName.trim()
            .ifBlank { "Node${_uiState.value.storeNodeId}" }

        if (id == null || id !in 1..250) {
            _uiState.update {
                it.copy(message = context.getString(R.string.node_id_range_error))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.storeValue(id)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    provisionStarted = if (ok) false else it.provisionStarted,
                    message = if (ok) {
                        context.getString(R.string.node_stored, id)
                    } else {
                        context.getString(R.string.store_failed)
                    }
                )
            }
            if (ok) {
                localNodes.cacheList(
                    listOf(NodeListItem(nodeName = name, nodeId = id, online = true))
                )
                transport.nodeListCmd()
                onFinished()
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            runCatching { transport.cancelProvision() }
            _uiState.update {
                ProvisionUiState(bluetoothConnected = it.bluetoothConnected)
            }
        }
    }
}