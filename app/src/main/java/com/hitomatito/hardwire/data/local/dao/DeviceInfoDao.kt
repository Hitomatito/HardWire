package com.hitomatito.hardwire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hitomatito.hardwire.data.local.entity.DeviceInfoEntity

@Dao
interface DeviceInfoDao {
    @Query("SELECT * FROM device_info WHERE deviceId = :deviceId")
    suspend fun getDeviceInfo(deviceId: String): DeviceInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(info: DeviceInfoEntity)

    @Query("DELETE FROM device_info WHERE deviceId = :deviceId")
    suspend fun deleteDeviceInfo(deviceId: String)
}
