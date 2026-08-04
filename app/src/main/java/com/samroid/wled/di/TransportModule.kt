package com.samroid.wled.di

import android.content.Context
import android.os.Build
import com.samroid.wled.data.transport.BleDeviceTransport
import com.samroid.wled.data.transport.DeviceTransport
import com.samroid.wled.data.transport.FakeDeviceTransport
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransportModule {

    @Provides
    @Singleton
    fun provideDeviceTransport(
        @ApplicationContext context: Context
    ): DeviceTransport {
        return if (isEmulator()) {
            FakeDeviceTransport()
        } else {
            BleDeviceTransport(context)
        }
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic", true)
                || Build.MODEL.contains("Emulator", true)
                || Build.PRODUCT.contains("sdk", true)
    }
}