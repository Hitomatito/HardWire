package com.hitomatito.hardwire.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hitomatito.hardwire.data.local.dao.DeviceDao
import com.hitomatito.hardwire.data.local.dao.DeviceInfoDao
import com.hitomatito.hardwire.data.local.dao.HistoryDao
import com.hitomatito.hardwire.data.local.entity.DeviceHistoryEntity
import com.hitomatito.hardwire.data.local.entity.DeviceInfoEntity
import com.hitomatito.hardwire.data.local.entity.ManagedDeviceEntity

@Database(
    entities = [ManagedDeviceEntity::class, DeviceInfoEntity::class, DeviceHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class HardwireDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun deviceInfoDao(): DeviceInfoDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: HardwireDatabase? = null

        fun getDatabase(context: Context): HardwireDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HardwireDatabase::class.java,
                    "hardwire_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
