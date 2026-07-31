package com.hitomatito.hardwire.data.model

enum class DeviceType {
    USB,
    NETWORK,
    LOCAL
}

data class ManagedDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val type: DeviceType,
    val addedAt: Long = System.currentTimeMillis()
)
