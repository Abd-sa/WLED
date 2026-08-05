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

    fun startProvisionIfNeeded() {
        val s = _uiState.value
        if (s.provisionStarted || !s.bluetoothConnected) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.provision()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    provisionStarted = ok,
                    message = if (ok) context.getString(R.string.provision_started) else context.getString(
                        R.string.provision_failed
                    )
                )
            }
        }
    }

    // ---- inputs ----
    fun onGpioChange(v: String) {
        _uiState.update {
            it.copy(gpioValue = v.filter { c -> c.isDigit() }.take(3))
        }
    }

    fun onColorOrderSelect(order: Int) {
        _uiState.update { it.copy(colorOrder = order.coerceIn(0, 5)) }
    }

    fun onLengthChange(v: String) {
        _uiState.update {
            it.copy(lengthValue = v.filter { c -> c.isDigit() }.take(3))
        }
    }

    fun goToStep(step: ProvisionStep) {
        _uiState.update { it.copy(currentStep = step, message = null) }
    }

    fun back() {
        val idx = _uiState.value.currentStep.index
        if (idx > 1) goToStep(ProvisionStep.fromIndex(idx - 1))
    }

    // ---- Step actions ----

    fun testGpio() {
        val pin = _uiState.value.gpioValue.toIntOrNull()
        if (pin == null || pin !in 0..255) {
            _uiState.update { it.copy(message = "GPIO باید 0 تا 255 باشد") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.gpioValue(pin)
            _uiState.update { it.copy(isBusy = false, message = "GPIO_VALUE ارسال شد — LED را چک کن") }
        }
    }

    fun confirmGpio() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.gpioConfirm()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    currentStep = if (ok) ProvisionStep.COLOR else it.currentStep,
                    message = if (ok) "GPIO تأیید شد" else "خطا در GPIO_CONFIRM"
                )
            }
        }
    }

    fun testColor() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.colorValue(_uiState.value.colorOrder)
            _uiState.update { it.copy(isBusy = false, message = "COLOR_VALUE ارسال شد") }
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
                    message = if (ok) "Color تأیید شد" else "خطا در COLOR_CONFIRM"
                )
            }
        }
    }

    fun testLength() {
        val len = _uiState.value.lengthValue.toIntOrNull()
        if (len == null || len !in 1..300) {
            _uiState.update { it.copy(message = "طول باید 1 تا 300 باشد") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.lengthValue(len)
            _uiState.update { it.copy(isBusy = false, message = "LENGTH_VALUE ارسال شد") }
        }
    }

    fun confirmLength() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.lengthConfirm()
            _uiState.update {
                it.copy(
                    isBusy = false,
                    // روز ۹: برو به OUTPUT
                    currentStep = if (ok) ProvisionStep.OUTPUT else it.currentStep,
                    message = if (ok) "Length تأیید شد" else "خطا در LENGTH_CONFIRM"
                )
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            transport.cancelProvision()
            _uiState.update {
                ProvisionUiState(bluetoothConnected = it.bluetoothConnected)
            }
        }
    }

    fun onCctWarmChange(v: String) {
        _uiState.update {
            it.copy(cctWarmGpio = v.filter { c -> c.isDigit() }.take(3))
        }
    }

    fun onCctCoolChange(v: String) {
        _uiState.update {
            it.copy(cctCoolGpio = v.filter { c -> c.isDigit() }.take(3))
        }
    }

    fun onStoreNodeIdChange(v: String) {
        _uiState.update {
            it.copy(storeNodeId = v.filter { c -> c.isDigit() }.take(3))
        }
    }

    fun testOutput() {
        val warm = _uiState.value.cctWarmGpio.toIntOrNull()
        val cool = _uiState.value.cctCoolGpio.toIntOrNull()
        if (warm == null || cool == null || warm !in 0..255 || cool !in 0..255) {
            _uiState.update { it.copy(message = "پین‌های CCT باید 0 تا 255 باشند") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            transport.outputValue(warm, cool)
            _uiState.update { it.copy(isBusy = false, message = "OUTPUT_VALUE ارسال شد") }
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
                    message = if (ok) "Output تأیید شد" else "خطا در OUTPUT_CONFIRM"
                )
            }
        }
    }

    /** اگر نود CCT ندارد می‌توانی مستقیم بروی Store */
    fun skipOutput() {
        _uiState.update {
            it.copy(currentStep = ProvisionStep.STORE, message = "CCT رد شد")
        }
    }

    fun storeAndFinish(onFinished: () -> Unit = {}) {
        val id = _uiState.value.storeNodeId.toIntOrNull()
        val name = _uiState.value.storeNodeName
        if (id == null || id !in 1..250) {
            _uiState.update { it.copy(message = "Node ID باید 1 تا 250 باشد") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            val ok = transport.storeValue(id)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = if (ok) "نود #$id ذخیره شد ✓" else "خطا در STORE_VALUE",
                    provisionStarted = if (ok) false else it.provisionStarted
                )
            }
            if (ok) {
                localNodes.cacheList(listOf(NodeListItem(name, id, online = true)))
                // لیست نودها را تازه کن
                transport.nodeListCmd()
                onFinished()
            }

        }
    }
}