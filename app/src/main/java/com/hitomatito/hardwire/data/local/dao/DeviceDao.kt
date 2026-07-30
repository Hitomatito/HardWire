package com.hitomatito.hardwire.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hitomatito.hardwire.data.local.entity.ManagedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM managed_devices")
    fun getAllDevices(): Flow<List<ManagedDeviceEntity>>

    @Query("SELECT * FROM managed_devices WHERE id = :id")
    suspend fun getDeviceById(id: String): ManagedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: ManagedDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<ManagedDeviceEntity>)

    @Update
    suspend fun updateDevice(device: ManagedDeviceEntity)

    @Delete
    suspend fun deleteDevice(device: ManagedDeviceEntity)

    @Query("DELETE FROM managed_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: String)
}
