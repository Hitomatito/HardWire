package com.hitomatito.hardwire.di

import android.content.Context
import com.hitomatito.hardwire.data.chipset.ChipsetRepository
import com.hitomatito.hardwire.data.device.DeviceManager
import com.hitomatito.hardwire.data.history.HistoryRepository
import com.hitomatito.hardwire.data.local.HardwireDatabase
import com.hitomatito.hardwire.data.local.dao.DeviceDao
import com.hitomatito.hardwire.data.local.dao.DeviceInfoDao
import com.hitomatito.hardwire.data.local.dao.HistoryDao
import com.hitomatito.hardwire.data.usb.UsbAdbManager

object ServiceLocator {
    @Volatile
    private var instance: AppContainer? = null

    fun getContainer(context: Context): AppContainer {
        return instance ?: synchronized(this) {
            instance ?: AppContainer(context.applicationContext).also { instance = it }
        }
    }
}

class AppContainer(context: Context) {
    val database: HardwireDatabase by lazy { HardwireDatabase.getDatabase(context) }
    val deviceDao: DeviceDao by lazy { database.deviceDao() }
    val deviceInfoDao: DeviceInfoDao by lazy { database.deviceInfoDao() }
    val historyDao: HistoryDao by lazy { database.historyDao() }

    val chipsetRepository: ChipsetRepository by lazy { ChipsetRepository(context) }
    val historyRepository: HistoryRepository by lazy { HistoryRepository(historyDao) }
    val usbAdbManager: UsbAdbManager by lazy { UsbAdbManager(context) }
    val deviceManager: DeviceManager by lazy {
        DeviceManager(context, chipsetRepository, historyRepository)
    }
}
