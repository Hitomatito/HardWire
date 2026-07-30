package com.hitomatito.hardwire.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_info")
data class DeviceInfoEntity(
    @PrimaryKey val deviceId: String,
    val json: String,
    val updatedAt: Long
)
