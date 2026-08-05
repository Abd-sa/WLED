package com.samroid.wled.di

import android.content.Context
import androidx.room.Room
import com.samroid.wled.data.local.WledDatabase
import com.samroid.wled.data.local.dao.BtDeviceDao
import com.samroid.wled.data.local.dao.NetworkConfigDao
import com.samroid.wled.data.local.dao.NodeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WledDatabase {
        return Room.databaseBuilder(
            context,
            WledDatabase::class.java,
            "wled_master.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNodeDao(db: WledDatabase): NodeDao = db.nodeDao()

    @Provides
    fun provideNetworkConfigDao(db: WledDatabase): NetworkConfigDao = db.networkConfigDao()

    @Provides
    fun provideBtDeviceDao(db: WledDatabase): BtDeviceDao = db.btDeviceDao()
}