package com.hitomatito.hardwire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hitomatito.hardwire.data.local.entity.DeviceHistoryEntity

@Dao
interface HistoryDao {
    @Query("SELECT * FROM device_history WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getHistory(deviceId: String, limit: Int = 20): List<DeviceHistoryEntity>

    @Insert
    suspend fun insertSnapshot(snapshot: DeviceHistoryEntity)

    @Query("DELETE FROM device_history WHERE deviceId = :deviceId")
    suspend fun deleteHistory(deviceId: String)
}
