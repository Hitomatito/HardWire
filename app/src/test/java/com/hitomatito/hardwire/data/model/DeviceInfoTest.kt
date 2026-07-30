package com.hitomatito.hardwire.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInfoTest {

    @Test
    fun `default DeviceInfo has empty defaults for all sub-objects`() {
        val info = DeviceInfo()
        assertEquals(GeneralInfo(), info.general)
        assertEquals(CpuInfo(), info.cpu)
        assertEquals(MemoryInfo(), info.memory)
        assertEquals(BatteryInfo(), info.battery)
        assertEquals(DisplayInfo(), info.display)
        assertEquals(StorageInfo(), info.storage)
        assertEquals(emptyList<CameraInfo>(), info.cameras)
        assertEquals(emptyList<SensorInfo>(), info.sensors)
        assertEquals(NetworkInfo(), info.network)
        assertEquals(BuildInfo(), info.build)
    }

    @Test
    fun `default GeneralInfo has empty strings and empty list`() {
        val g = GeneralInfo()
        assertEquals("", g.manufacturer)
        assertEquals("", g.model)
        assertEquals("", g.marketName)
        assertEquals("", g.device)
        assertEquals("", g.board)
        assertEquals("", g.hardware)
        assertEquals("", g.serialNumber)
        assertEquals(emptyList<String>(), g.imeis)
        assertEquals("", g.androidVersion)
        assertEquals("", g.sdkVersion)
        assertEquals("", g.fingerprint)
        assertEquals("", g.phone)
    }

    @Test
    fun `default CpuInfo has empty strings and zero counts`() {
        val c = CpuInfo()
        assertEquals("", c.socName)
        assertEquals("", c.socManufacturer)
        assertEquals("", c.processor)
        assertEquals("", c.hardware)
        assertEquals("", c.features)
        assertEquals(0, c.processorCount)
        assertEquals("", c.bogoMips)
        assertEquals("", c.architecture)
        assertEquals("", c.cpuAbi)
        assertEquals("", c.gpu)
        assertEquals("", c.cpuConfig)
    }

    @Test
    fun `default MemoryInfo has zeros and empty strings`() {
        val m = MemoryInfo()
        assertEquals(0L, m.totalRamBytes)
        assertEquals(0L, m.freeRamBytes)
        assertEquals(0L, m.availableRamBytes)
        assertEquals(0L, m.cachedBytes)
        assertEquals(0L, m.buffersBytes)
        assertEquals(0L, m.totalSwapBytes)
        assertEquals(0L, m.freeSwapBytes)
        assertEquals("", m.totalRamFormatted)
        assertEquals("", m.freeRamFormatted)
        assertEquals("", m.availableRamFormatted)
        assertEquals("", m.cachedFormatted)
        assertEquals("", m.totalSwapFormatted)
        assertEquals("", m.freeSwapFormatted)
        assertEquals(0f, m.usagePercent)
    }

    @Test
    fun `default BatteryInfo has empty strings and zero level`() {
        val b = BatteryInfo()
        assertEquals("", b.level)
        assertEquals("", b.scale)
        assertEquals("", b.status)
        assertEquals("", b.health)
        assertEquals("", b.technology)
        assertEquals("", b.temperature)
        assertEquals("", b.voltage)
        assertEquals("", b.plugged)
        assertEquals(0f, b.levelPercent)
    }

    @Test
    fun `default DisplayInfo has empty strings`() {
        val d = DisplayInfo()
        assertEquals("", d.resolution)
        assertEquals("", d.density)
        assertEquals("", d.densityDpi)
        assertEquals("", d.refreshRate)
        assertEquals("", d.displayInfo)
    }

    @Test
    fun `default StorageInfo has empty filesystems list`() {
        val s = StorageInfo()
        assertEquals(emptyList<FileSystemInfo>(), s.filesystems)
    }

    @Test
    fun `default CameraInfo has empty strings`() {
        val c = CameraInfo()
        assertEquals("", c.id)
        assertEquals("", c.facing)
        assertEquals("", c.megapixels)
        assertEquals("", c.resolution)
        assertEquals("", c.flash)
        assertEquals("", c.focalLength)
    }

    @Test
    fun `default SensorInfo has empty strings`() {
        val s = SensorInfo()
        assertEquals("", s.name)
        assertEquals("", s.type)
        assertEquals("", s.vendor)
        assertEquals("", s.version)
        assertEquals("", s.maxRange)
        assertEquals("", s.resolution)
        assertEquals("", s.power)
    }

    @Test
    fun `default NetworkInfo has empty list and empty wifiInterface`() {
        val n = NetworkInfo()
        assertEquals(emptyList<NetworkInterface>(), n.interfaces)
        assertEquals("", n.wifiInterface)
    }

    @Test
    fun `default BuildInfo has empty strings`() {
        val b = BuildInfo()
        assertEquals("", b.board)
        assertEquals("", b.bootloader)
        assertEquals("", b.brand)
        assertEquals("", b.device)
        assertEquals("", b.display)
        assertEquals("", b.fingerprint)
        assertEquals("", b.host)
        assertEquals("", b.id)
        assertEquals("", b.manufacturer)
        assertEquals("", b.model)
        assertEquals("", b.product)
        assertEquals("", b.tags)
        assertEquals("", b.type)
        assertEquals("", b.baseband)
        assertEquals("", b.kernel)
    }

    @Test
    fun `ConnectionState sealed class has all expected variants`() {
        val states: List<ConnectionState> = listOf(
            ConnectionState.Disconnected,
            ConnectionState.Detecting,
            ConnectionState.RequestingPermission,
            ConnectionState.Connecting,
            ConnectionState.ConnectingWifi,
            ConnectionState.GatheringData,
            ConnectionState.Scanning,
            ConnectionState.Connected,
            ConnectionState.Error("test error")
        )
        assertEquals(9, states.size)

        assertTrue(states[0] is ConnectionState.Disconnected)
        assertTrue(states[1] is ConnectionState.Detecting)
        assertTrue(states[2] is ConnectionState.RequestingPermission)
        assertTrue(states[3] is ConnectionState.Connecting)
        assertTrue(states[4] is ConnectionState.ConnectingWifi)
        assertTrue(states[5] is ConnectionState.GatheringData)
        assertTrue(states[6] is ConnectionState.Scanning)
        assertTrue(states[7] is ConnectionState.Connected)
        assertTrue(states[8] is ConnectionState.Error)
    }

    @Test
    fun `ConnectionState Error holds message`() {
        val error = ConnectionState.Error("connection refused")
        assertEquals("connection refused", error.message)
    }

    @Test
    fun `DeviceInfoJson toJson and fromJson roundtrip preserves all fields`() {
        val original = DeviceInfo(
            general = GeneralInfo(
                manufacturer = "Acme",
                model = "X1",
                marketName = "Acme X1 Pro",
                device = "x1",
                board = "msm8998",
                hardware = "qcom",
                serialNumber = "SN12345",
                imeis = listOf("111111111111111", "222222222222222"),
                androidVersion = "14",
                sdkVersion = "34",
                fingerprint = "acme/x1/x1:14/UP1A.231005.007/V1:userdebug/test-keys",
                phone = "+15551234567"
            ),
            cpu = CpuInfo(
                socName = "Snapdragon 8 Gen 3",
                socManufacturer = "Qualcomm",
                processor = "Cortex-X4",
                hardware = "qcom,lahaina",
                features = "fp asimd evtstrm",
                processorCount = 8,
                bogoMips = "38.40",
                architecture = "aarch64",
                cpuAbi = "arm64-v8a",
                gpu = "Adreno 750",
                cpuConfig = "4 3 1"
            ),
            memory = MemoryInfo(
                totalRamBytes = 8589934592L,
                freeRamBytes = 2147483648L,
                availableRamBytes = 4294967296L,
                cachedBytes = 1073741824L,
                buffersBytes = 536870912L,
                totalSwapBytes = 2147483648L,
                freeSwapBytes = 1073741824L,
                totalRamFormatted = "8 GB",
                freeRamFormatted = "2 GB",
                availableRamFormatted = "4 GB",
                cachedFormatted = "1 GB",
                totalSwapFormatted = "2 GB",
                freeSwapFormatted = "1 GB",
                usagePercent = 75.5f
            ),
            battery = BatteryInfo(
                level = "85",
                scale = "100",
                status = "Charging",
                health = "Good",
                technology = "Li-ion",
                temperature = "320",
                voltage = "4200",
                plugged = "AC",
                levelPercent = 85f
            ),
            display = DisplayInfo(
                resolution = "1080x2340",
                density = "2.75",
                densityDpi = "440",
                refreshRate = "120.0",
                displayInfo = "Surface(display-id=0)"
            ),
            storage = StorageInfo(
                filesystems = listOf(
                    FileSystemInfo(
                        filesystem = "/dev/block/sda1",
                        sizeFormatted = "128 GB",
                        usedFormatted = "80 GB",
                        availableFormatted = "48 GB",
                        mountPoint = "/data",
                        sizeBytes = 137438953472L,
                        usedBytes = 85899345920L,
                        availableBytes = 51539607552L,
                        usagePercent = 62.5f
                    )
                )
            ),
            cameras = listOf(
                CameraInfo(
                    id = "0",
                    facing = "back",
                    megapixels = "108",
                    resolution = "12000x9000",
                    flash = "yes",
                    focalLength = "6.7"
                ),
                CameraInfo(
                    id = "1",
                    facing = "front",
                    megapixels = "32",
                    resolution = "6528x4896",
                    flash = "no",
                    focalLength = "2.4"
                )
            ),
            sensors = listOf(
                SensorInfo(
                    name = "LSM6DSO Accelerometer",
                    type = "Accelerometer",
                    vendor = "STMicroelectronics",
                    version = "1",
                    maxRange = "156.90955",
                    resolution = "0.0047851562",
                    power = "0.14"
                )
            ),
            network = NetworkInfo(
                interfaces = listOf(
                    NetworkInterface(
                        name = "wlan0",
                        ipAddress = "192.168.1.50",
                        macAddress = "AA:BB:CC:DD:EE:FF",
                        flags = "up broadcast running multicast"
                    )
                ),
                wifiInterface = "wlan0"
            ),
            build = BuildInfo(
                board = "msm8998",
                bootloader = "unknown",
                brand = "Acme",
                device = "x1",
                display = "UP1A.231005.007",
                fingerprint = "acme/x1/x1:14/UP1A.231005.007/V1:userdebug/test-keys",
                host = "build-host",
                id = "UP1A.231005.007",
                manufacturer = "Acme",
                model = "X1",
                product = "x1",
                tags = "test-keys",
                type = "userdebug",
                baseband = "g5300q-230913",
                kernel = "5.15.104-android14-11-gc4e3f0e"
            )
        )

        val jsonString = Json.encodeToString(DeviceInfo.serializer(), original)
        val restored = Json.decodeFromString(DeviceInfo.serializer(), jsonString)

        assertEquals(original.general, restored.general)
        assertEquals(original.cpu, restored.cpu)
        assertEquals(original.memory, restored.memory)
        assertEquals(original.battery, restored.battery)
        assertEquals(original.display, restored.display)
        assertEquals(original.storage, restored.storage)
        assertEquals(original.cameras, restored.cameras)
        assertEquals(original.sensors, restored.sensors)
        assertEquals(original.network, restored.network)
        assertEquals(original.build, restored.build)
    }

    @Test
    fun `DeviceInfoJson fromJson returns default DeviceInfo for null`() {
        val result = DeviceInfoJson.fromJson(null)
        assertEquals(DeviceInfo(), result)
    }

    @Test
    fun `DeviceInfoJson fromJson handles partial JSON gracefully`() {
        val jsonString = """
            {
                "general": {"manufacturer": "TestCorp", "model": "Phone1"},
                "cpu": {"socName": "Snapdragon", "processorCount": 8}
            }
        """.trimIndent()

        val result = Json.decodeFromString<DeviceInfo>(jsonString)

        assertEquals("TestCorp", result.general.manufacturer)
        assertEquals("Phone1", result.general.model)
        assertEquals("", result.general.androidVersion)
        assertEquals(emptyList<String>(), result.general.imeis)

        assertEquals("Snapdragon", result.cpu.socName)
        assertEquals(8, result.cpu.processorCount)
        assertEquals("", result.cpu.gpu)

        assertEquals(MemoryInfo(), result.memory)
        assertEquals(BatteryInfo(), result.battery)
        assertEquals(DisplayInfo(), result.display)
        assertEquals(StorageInfo(), result.storage)
        assertEquals(emptyList<CameraInfo>(), result.cameras)
        assertEquals(emptyList<SensorInfo>(), result.sensors)
        assertEquals(NetworkInfo(), result.network)
        assertEquals(BuildInfo(), result.build)
    }

    @Test
    fun `DeviceInfoJson fromJson handles completely empty JSON`() {
        val result = Json.decodeFromString<DeviceInfo>("{}")
        assertEquals(DeviceInfo(), result)
    }

    @Test
    fun `ManagedDevice creation with DeviceType USB`() {
        val device = ManagedDevice(
            id = "usb-1",
            name = "Pixel 7",
            host = "",
            port = 0,
            type = DeviceType.USB,
            addedAt = 1000L
        )
        assertEquals("usb-1", device.id)
        assertEquals("Pixel 7", device.name)
        assertEquals("", device.host)
        assertEquals(0, device.port)
        assertEquals(DeviceType.USB, device.type)
        assertEquals(1000L, device.addedAt)
    }

    @Test
    fun `ManagedDevice creation with DeviceType NETWORK`() {
        val device = ManagedDevice(
            id = "net-1",
            name = "Galaxy S24",
            host = "192.168.1.50",
            port = 5555,
            type = DeviceType.NETWORK
        )
        assertEquals("net-1", device.id)
        assertEquals("Galaxy S24", device.name)
        assertEquals("192.168.1.50", device.host)
        assertEquals(5555, device.port)
        assertEquals(DeviceType.NETWORK, device.type)
        assertTrue(device.addedAt > 0)
    }

    @Test
    fun `DeviceType enum has exactly USB and NETWORK`() {
        val values = DeviceType.values()
        assertEquals(2, values.size)
        assertEquals(DeviceType.USB, values[0])
        assertEquals(DeviceType.NETWORK, values[1])
    }

    @Test
    fun `data class copy works on GeneralInfo`() {
        val original = GeneralInfo(manufacturer = "Acme", model = "X1")
        val copied = original.copy(model = "X2")
        assertEquals("Acme", copied.manufacturer)
        assertEquals("X2", copied.model)
        assertEquals("Acme", original.manufacturer)
        assertEquals("X1", original.model)
    }

    @Test
    fun `data class copy works on CpuInfo`() {
        val original = CpuInfo(socName = "Old", processorCount = 4)
        val copied = original.copy(socName = "New", processorCount = 8)
        assertEquals("New", copied.socName)
        assertEquals(8, copied.processorCount)
        assertEquals("Old", original.socName)
    }

    @Test
    fun `data class copy works on MemoryInfo`() {
        val original = MemoryInfo(totalRamBytes = 4096L, usagePercent = 50f)
        val copied = original.copy(totalRamBytes = 8192L)
        assertEquals(8192L, copied.totalRamBytes)
        assertEquals(50f, copied.usagePercent)
        assertEquals(4096L, original.totalRamBytes)
    }

    @Test
    fun `data class copy works on DeviceInfo`() {
        val original = DeviceInfo(
            general = GeneralInfo(manufacturer = "Acme"),
            cpu = CpuInfo(socName = "Snapdragon")
        )
        val copied = original.copy(
            general = GeneralInfo(manufacturer = "Other"),
            cpu = CpuInfo(socName = "Exynos")
        )
        assertEquals("Other", copied.general.manufacturer)
        assertEquals("Exynos", copied.cpu.socName)
        assertEquals("Acme", original.general.manufacturer)
        assertEquals("Snapdragon", original.cpu.socName)
    }

    @Test
    fun `data class copy works on ManagedDevice`() {
        val original = ManagedDevice(
            id = "1", name = "Phone", host = "1.2.3.4", port = 5555, type = DeviceType.NETWORK
        )
        val copied = original.copy(name = "Renamed Phone", port = 5556)
        assertEquals("Renamed Phone", copied.name)
        assertEquals(5556, copied.port)
        assertEquals("Phone", original.name)
        assertEquals(5555, original.port)
    }
}
