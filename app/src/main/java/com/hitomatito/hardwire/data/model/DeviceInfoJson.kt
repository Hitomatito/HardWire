package com.hitomatito.hardwire.data.model

import kotlinx.serialization.json.Json
import org.json.JSONObject

object DeviceInfoJson {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun toJson(info: DeviceInfo): JSONObject {
        val jsonString = json.encodeToString(DeviceInfo.serializer(), info)
        return JSONObject(jsonString)
    }

    fun fromJson(obj: JSONObject?): DeviceInfo {
        if (obj == null) return DeviceInfo()
        return try {
            json.decodeFromString(obj.toString())
        } catch (e: Exception) {
            DeviceInfo()
        }
    }
}
