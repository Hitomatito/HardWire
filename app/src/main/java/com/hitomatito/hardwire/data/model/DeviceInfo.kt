package com.hitomatito.hardwire.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val general: GeneralInfo = GeneralInfo(),
    val cpu: CpuInfo = CpuInfo(),
    val memory: MemoryInfo = MemoryInfo(),
    val battery: BatteryInfo = BatteryInfo(),
    val display: DisplayInfo = DisplayInfo(),
    val storage: StorageInfo = StorageInfo(),
    val cameras: List<CameraInfo> = emptyList(),
    val sensors: List<SensorInfo> = emptyList(),
    val network: NetworkInfo = NetworkInfo(),
    val build: BuildInfo = BuildInfo()
)

@Serializable
data class GeneralInfo(
    val manufacturer: String = "",
    val model: String = "",
    val marketName: String = "",
    val device: String = "",
    val board: String = "",
    val hardware: String = "",
    val serialNumber: String = "",
    val imeis: List<String> = emptyList(),
    val androidVersion: String = "",
    val sdkVersion: String = "",
    val fingerprint: String = "",
    val phone: String = ""
)

@Serializable
data class CpuInfo(
    val socName: String = "",
    val socManufacturer: String = "",
    val processor: String = "",
    val hardware: String = "",
    val features: String = "",
    val processorCount: Int = 0,
    val bogoMips: String = "",
    val architecture: String = "",
    val cpuAbi: String = "",
    val gpu: String = "",
    val cpuConfig: String = ""
)

@Serializable
data class MemoryInfo(
    val totalRamBytes: Long = 0,
    val freeRamBytes: Long = 0,
    val availableRamBytes: Long = 0,
    val cachedBytes: Long = 0,
    val buffersBytes: Long = 0,
    val totalSwapBytes: Long = 0,
    val freeSwapBytes: Long = 0,
    val totalRamFormatted: String = "",
    val freeRamFormatted: String = "",
    val availableRamFormatted: String = "",
    val cachedFormatted: String = "",
    val totalSwapFormatted: String = "",
    val freeSwapFormatted: String = "",
    val usagePercent: Float = 0f
)

@Serializable
data class BatteryInfo(
    val level: String = "",
    val scale: String = "",
    val status: String = "",
    val health: String = "",
    val technology: String = "",
    val temperature: String = "",
    val voltage: String = "",
    val plugged: String = "",
    val levelPercent: Float = 0f
)

@Serializable
data class DisplayInfo(
    val resolution: String = "",
    val density: String = "",
    val densityDpi: String = "",
    val refreshRate: String = "",
    val displayInfo: String = ""
)

@Serializable
data class StorageInfo(
    val filesystems: List<FileSystemInfo> = emptyList()
)

@Serializable
data class FileSystemInfo(
    val filesystem: String = "",
    val sizeFormatted: String = "",
    val usedFormatted: String = "",
    val availableFormatted: String = "",
    val mountPoint: String = "",
    val sizeBytes: Long = 0,
    val usedBytes: Long = 0,
    val availableBytes: Long = 0,
    val usagePercent: Float = 0f
)

@Serializable
data class CameraInfo(
    val id: String = "",
    val facing: String = "",
    val megapixels: String = "",
    val resolution: String = "",
    val flash: String = "",
    val focalLength: String = ""
)

@Serializable
data class SensorInfo(
    val name: String = "",
    val type: String = "",
    val vendor: String = "",
    val version: String = "",
    val maxRange: String = "",
    val resolution: String = "",
    val power: String = ""
)

@Serializable
data class NetworkInfo(
    val interfaces: List<NetworkInterface> = emptyList(),
    val wifiInterface: String = ""
)

@Serializable
data class NetworkInterface(
    val name: String = "",
    val ipAddress: String = "",
    val macAddress: String = "",
    val flags: String = ""
)

@Serializable
data class BuildInfo(
    val board: String = "",
    val bootloader: String = "",
    val brand: String = "",
    val device: String = "",
    val display: String = "",
    val fingerprint: String = "",
    val host: String = "",
    val id: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val product: String = "",
    val tags: String = "",
    val type: String = "",
    val baseband: String = "",
    val kernel: String = ""
)

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Detecting : ConnectionState()
    data object RequestingPermission : ConnectionState()
    data object Connecting : ConnectionState()
    data object ConnectingWifi : ConnectionState()
    data object GatheringData : ConnectionState()
    data object Scanning : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
