package com.hitomatito.hardwire.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "managed_devices")
data class ManagedDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val type: String,
    val addedAt: Long
)
