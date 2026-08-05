// domain/usecase/AutoConnectBluetoothUseCase.kt
package com.samroid.wled.domain.usecase

import com.samroid.wled.data.repository.LocalSettingsRepository
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.domain.model.TransportConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoConnectBluetoothUseCase @Inject constructor(
    private val transport: DeviceTransport,
    private val localSettings: LocalSettingsRepository
) {

    suspend operator fun invoke(): Boolean {
        when (transport.transportConnectionState.value) {
            TransportConnectionState.CONNECTED -> return true
            TransportConnectionState.CONNECTING -> {
                // صبر کن، دوباره connect نزن
                val result = withTimeoutOrNull(10_000) {
                    transport.transportConnectionState.first {
                        it == TransportConnectionState.CONNECTED ||
                                it == TransportConnectionState.ERROR ||
                                it == TransportConnectionState.DISCONNECTED
                    }
                }
                return result == TransportConnectionState.CONNECTED
            }
            else -> Unit
        }
        if (transport.transportConnectionState.value == TransportConnectionState.CONNECTED) {
            return true
        }

        val saved = localSettings.getLastBtDevice() ?: return false
        val targetId = saved.address
        val targetName = saved.name

        transport.startScan()
        withTimeoutOrNull(8_000) {
            transport.devices.first { devices ->
                devices.any {
                    it.id.equals(targetId, true) ||
                            it.address.equals(targetId, true) ||
                            (targetName.isNotBlank() && it.name.equals(targetName, true))
                }
            }
        }
        transport.stopScan()

        val connectId = transport.devices.value.firstOrNull {
            it.id.equals(targetId, true) ||
                    it.address.equals(targetId, true) ||
                    it.name.equals(targetName, true)
        }?.id ?: targetId

        transport.connect(connectId)

        // ← اینجا
        val result = withTimeoutOrNull(10_000) {
            transport.transportConnectionState.first {
                it == TransportConnectionState.CONNECTED ||
                        it == TransportConnectionState.ERROR
            }
        }

        return result == TransportConnectionState.CONNECTED
    }
}