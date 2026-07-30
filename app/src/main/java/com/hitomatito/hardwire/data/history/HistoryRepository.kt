package com.hitomatito.hardwire.data.history

import com.hitomatito.hardwire.data.local.dao.HistoryDao
import com.hitomatito.hardwire.data.local.entity.DeviceHistoryEntity
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.data.model.DeviceInfoJson

class HistoryRepository(
    private val historyDao: HistoryDao
) {
    suspend fun saveSnapshot(deviceId: String, info: DeviceInfo) {
        val json = DeviceInfoJson.toJson(info).toString()
        historyDao.insertSnapshot(
            DeviceHistoryEntity(
                deviceId = deviceId,
                timestamp = System.currentTimeMillis(),
                json = json
            )
        )
    }

    suspend fun getHistory(deviceId: String, limit: Int = 20): List<Pair<Long, DeviceInfo>> {
        return historyDao.getHistory(deviceId, limit).mapNotNull { entity ->
            try {
                val obj = org.json.JSONObject(entity.json)
                val info = DeviceInfoJson.fromJson(obj)
                entity.timestamp to info
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun deleteHistory(deviceId: String) {
        historyDao.deleteHistory(deviceId)
    }
}
