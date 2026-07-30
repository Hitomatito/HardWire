package com.hitomatito.hardwire.data.command

import com.hitomatito.hardwire.data.chipset.ChipsetInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandParserTest {

    // ── parseGeneralInfo (tests parseGetProp indirectly) ──────────────────────

    @Test
    fun `parseGeneralInfo extracts standard getprop fields`() {
        val getprop = """
            [ro.product.manufacturer]: [Samsung]
            [ro.product.model]: [SM-G991B]
            [ro.product.device]: [o1s]
            [ro.product.board]: [exynos2100]
            [ro.hardware]: [exynos2100]
            [ro.serialno]: [RF8N90XXXXXX]
            [ro.build.version.release]: [12]
            [ro.build.version.sdk]: [31]
            [ro.build.fingerprint]: [samsung/o1sxx/o1s:12/SP1A.210812.016/G991BXXS3CVF5:user/release-keys]
            [gsm.version.baseband]: [G991BXXS3CVF5]
            [ro.build.display.id]: [SP1A.210812.016.G991BXXS3CVF5]
        """.trimIndent()

        val result = CommandParser.parseGeneralInfo(
            getpropOutput = getprop,
            socModel = "exynos2100",
            socManufacturer = "Samsung",
            socChipname = "exynos2100",
            socPlatform = "exynos2100",
            socVendorModel = "exynos2100",
            marketName = "Galaxy S21"
        )

        assertEquals("Samsung", result.manufacturer)
        assertEquals("SM-G991B", result.model)
        assertEquals("Galaxy S21", result.marketName)
        assertEquals("o1s", result.device)
        assertEquals("exynos2100", result.board)
        assertEquals("exynos2100", result.hardware)
        assertEquals("RF8N90XXXXXX", result.serialNumber)
        assertEquals("12", result.androidVersion)
        assertEquals("31", result.sdkVersion)
        assertEquals("samsung/o1sxx/o1s:12/SP1A.210812.016/G991BXXS3CVF5:user/release-keys", result.fingerprint)
        assertEquals("G991BXXS3CVF5", result.phone)
    }

    @Test
    fun `parseGeneralInfo passes imeis through`() {
        val getprop = "[ro.product.manufacturer]: [Google]\n[ro.product.model]: [Pixel 6]"
        val imeis = listOf("353456789012348")

        val result = CommandParser.parseGeneralInfo(
            getpropOutput = getprop,
            socModel = "",
            socManufacturer = "",
            socChipname = "",
            socPlatform = "",
            socVendorModel = "",
            marketName = "",
            imeis = imeis
        )

        assertEquals(1, result.imeis.size)
        assertEquals("353456789012348", result.imeis[0])
    }

    @Test
    fun `parseGeneralInfo handles empty getprop output`() {
        val result = CommandParser.parseGeneralInfo(
            getpropOutput = "",
            socModel = "",
            socManufacturer = "",
            socChipname = "",
            socPlatform = "",
            socVendorModel = "",
            marketName = ""
        )

        assertEquals("", result.manufacturer)
        assertEquals("", result.model)
    }

    @Test
    fun `parseGeneralInfo ignores duplicate keys`() {
        val getprop = """
            [ro.product.model]: [First]
            [ro.product.model]: [Second]
        """.trimIndent()

        val result = CommandParser.parseGeneralInfo(
            getpropOutput = getprop,
            socModel = "",
            socManufacturer = "",
            socChipname = "",
            socPlatform = "",
            socVendorModel = "",
            marketName = ""
        )

        assertEquals("First", result.model)
    }

    // ── parseCpuInfo ──────────────────────────────────────────────────────────

    @Test
    fun `parseCpuInfo parses standard 8-core ARM output`() {
        val cpuOutput = """
            processor	: 5
            model name	: ARMv8 Processor rev 1 (v8l)
            BogoMIPS	: 38.40
            Features	: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm jscvt fcma dcpop asimdsha3 asimdfhm
            CPU implementer	: 0x51
            CPU architecture: 8
            CPU variant	: 0x8
            CPU part	: 0x805
            CPU revision	: 4

            Hardware	: Qualcomm Technologies, Inc SM8150

            processor	: 0
            processor	: 1
            processor	: 2
            processor	: 3
            processor	: 4
            processor	: 5
            processor	: 6
            processor	: 7
        """.trimIndent()

        val result = CommandParser.parseCpuInfo(
            cpuOutput = cpuOutput,
            socModel = "sm8150",
            socManufacturer = "Qualcomm",
            socChipname = "sm8150",
            socPlatform = "sm8150",
            socVendorModel = "sm8150"
        )

        assertEquals(9, result.processorCount)
        assertEquals("Qualcomm Technologies, Inc SM8150", result.hardware)
        assertEquals("fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm jscvt fcma dcpop asimdsha3 asimdfhm", result.features)
        assertEquals("38.40", result.bogoMips)
        assertEquals("ARMv8 (64-bit)", result.architecture)
        assertTrue(result.socName.contains("Snapdragon 855"))
    }

    @Test
    fun `parseCpuInfo detects ARMv7 architecture`() {
        val cpuOutput = """
            Processor	: ARMv7 Processor rev 4 (v7l)
            BogoMIPS	: 24.00
            Features	: swp half thumb fastmult vfp edsp thumbee neon vfpv3 tls vfpv4 idiva idivt
            CPU architecture: 7
            CPU implementer	: 0x51
            Hardware	: Qualcomm MSM8916
        """.trimIndent()

        val result = CommandParser.parseCpuInfo(
            cpuOutput = cpuOutput,
            socModel = "msm8916",
            socManufacturer = "Qualcomm",
            socChipname = "msm8916",
            socPlatform = "msm8916",
            socVendorModel = "msm8916"
        )

        assertEquals("ARMv7 (32-bit)", result.architecture)
        assertEquals(0, result.processorCount)
    }

    @Test
    fun `parseCpuInfo falls back to cpuAbi when CPU architecture line is missing`() {
        val cpuOutput = """
            processor	: 0
            Features	: fp asimd evtstrm
            BogoMIPS	: 48.00
        """.trimIndent()

        val result = CommandParser.parseCpuInfo(
            cpuOutput = cpuOutput,
            socModel = "",
            socManufacturer = "",
            socChipname = "",
            socPlatform = "",
            socVendorModel = "",
            cpuAbi = "arm64-v8a"
        )

        assertEquals("ARMv8 (64-bit)", result.architecture)
    }

    @Test
    fun `parseCpuInfo uses chipsetInfo when provided`() {
        val cpuOutput = "processor	: 0\nHardware	: Qualcomm Technologies, Inc\nCPU architecture: 8"

        val chipset = ChipsetInfo(
            brand = "Qualcomm",
            chipset = "Snapdragon 8 Gen 2",
            cpu = "1x3.2 GHz Cortex-X3 + 2x2.8 GHz Cortex-A715 + 2x2.8 GHz Cortex-A710 + 3x2.0 GHz Cortex-A510",
            gpu = "Adreno 740"
        )

        val result = CommandParser.parseCpuInfo(
            cpuOutput = cpuOutput,
            socModel = "sm8550",
            socManufacturer = "Qualcomm",
            socChipname = "sm8550",
            socPlatform = "sm8550",
            socVendorModel = "sm8550",
            chipsetInfo = chipset
        )

        assertEquals("Qualcomm Snapdragon 8 Gen 2", result.socName)
        assertEquals("Qualcomm", result.socManufacturer)
        assertEquals("Adreno 740", result.gpu)
        assertEquals("1x3.2 GHz Cortex-X3 + 2x2.8 GHz Cortex-A715 + 2x2.8 GHz Cortex-A710 + 3x2.0 GHz Cortex-A510", result.cpuConfig)
    }

    // ── parseMemoryInfo ───────────────────────────────────────────────────────

    @Test
    fun `parseMemoryInfo parses standard meminfo output`() {
        val meminfo = """
            MemTotal:        7938104 kB
            MemFree:          212456 kB
            MemAvailable:    3456789 kB
            Buffers:          123456 kB
            Cached:          2345678 kB
            SwapCached:            0 kB
            Active:          3456789 kB
            Inactive:        2345678 kB
            SwapTotal:       2097152 kB
            SwapFree:        1987654 kB
        """.trimIndent()

        val result = CommandParser.parseMemoryInfo(meminfo)

        assertEquals(7938104L * 1024, result.totalRamBytes)
        assertEquals(212456L * 1024, result.freeRamBytes)
        assertEquals(3456789L * 1024, result.availableRamBytes)
        assertEquals(2345678L * 1024, result.cachedBytes)
        assertEquals(123456L * 1024, result.buffersBytes)
        assertEquals(2097152L * 1024, result.totalSwapBytes)
        assertEquals(1987654L * 1024, result.freeSwapBytes)

        // UsagePercent = (MemTotal - MemAvailable) / MemTotal * 100
        val expectedUsage = ((7938104f - 3456789f) / 7938104f * 100f).coerceIn(0f, 100f)
        assertEquals(expectedUsage, result.usagePercent, 0.1f)

        assertTrue(result.totalRamFormatted.contains("GB"))
        assertTrue(result.freeRamFormatted.contains("MB") || result.freeRamFormatted.contains("GB"))
    }

    @Test
    fun `parseMemoryInfo does not confuse SwapCached with Cached`() {
        val meminfo = "MemTotal: 4096 kB\nCached: 1024 kB\nSwapCached: 512 kB\nSwapTotal: 2048 kB\nSwapFree: 2048 kB"

        val result = CommandParser.parseMemoryInfo(meminfo)

        assertEquals(1024L * 1024, result.cachedBytes)
        assertEquals(2048L * 1024, result.totalSwapBytes)
    }

    @Test
    fun `parseMemoryInfo handles zero values`() {
        val meminfo = "MemTotal: 0 kB\nMemFree: 0 kB\nMemAvailable: 0 kB\nSwapTotal: 0 kB\nSwapFree: 0 kB"

        val result = CommandParser.parseMemoryInfo(meminfo)

        assertEquals(0L, result.totalRamBytes)
        assertEquals(0f, result.usagePercent)
    }

    // ── parseBatteryInfo ──────────────────────────────────────────────────────

    @Test
    fun `parseBatteryInfo parses charging battery`() {
        val battery = """
            Current Battery Service state:
              AC powered: true
              USB powered: false
              Wireless powered: false
              Max charging current: 500000
              Max charging voltage: 9000000
              Battery Controller temperature: 270
              status: 2
              health: 2
              present: true
              level: 65
              scale: 100
              voltage: 4200
              temperature: 270
              technology: Li-ion
        """.trimIndent()

        val result = CommandParser.parseBatteryInfo(battery)

        assertEquals("65", result.level)
        assertEquals("100", result.scale)
        assertEquals("Cargando", result.status)
        assertEquals("Buena", result.health)
        assertEquals("Li-ion", result.technology)
        assertEquals("27.0°C", result.temperature)
        assertEquals("4.20 V", result.voltage)
        assertEquals("AC", result.plugged)
        assertEquals(65f, result.levelPercent, 0.1f)
    }

    @Test
    fun `parseBatteryInfo parses USB powered battery`() {
        val battery = "AC powered: false\nUSB powered: true\nWireless powered: false\nstatus: 2\nlevel: 42\nscale: 100\nvoltage: 4100\ntemperature: 290\ntechnology: Li-ion"

        val result = CommandParser.parseBatteryInfo(battery)

        assertEquals("USB", result.plugged)
        assertEquals("42", result.level)
    }

    @Test
    fun `parseBatteryInfo parses wireless powered battery`() {
        val battery = "AC powered: false\nUSB powered: false\nWireless powered: true\nstatus: 5\nlevel: 100\nscale: 100\ntemperature: 250\nvoltage: 4300"

        val result = CommandParser.parseBatteryInfo(battery)

        assertEquals("Inalambrico", result.plugged)
        assertEquals("Completo", result.status)
    }

    @Test
    fun `parseBatteryInfo parses discharging battery`() {
        val battery = "AC powered: false\nUSB powered: false\nWireless powered: false\nstatus: 3\nlevel: 28\nscale: 100\ntemperature: 310\nvoltage: 3700"

        val result = CommandParser.parseBatteryInfo(battery)

        assertEquals("Sin carga", result.plugged)
        assertEquals("Descargando", result.status)
        assertEquals("31.0°C", result.temperature)
        assertEquals("3.70 V", result.voltage)
    }

    @Test
    fun `parseBatteryInfo handles dead battery health`() {
        val battery = "status: 1\nhealth: 4\nlevel: 0\nscale: 100"

        val result = CommandParser.parseBatteryInfo(battery)

        assertEquals("Desconocido", result.status)
        assertEquals("Muerta", result.health)
        assertEquals("0", result.level)
    }

    @Test
    fun `parseBatteryInfo calculates levelPercent from level and scale`() {
        val battery = "level: 75\nscale: 100\nstatus: 2\nhealth: 2"

        val result = CommandParser.parseBatteryInfo(battery)

        assertEquals(75f, result.levelPercent, 0.1f)
    }

    @Test
    fun `parseBatteryInfo calculates levelPercent when scale is not 100`() {
        val battery = "level: 5\nscale: 7\nstatus: 2\nhealth: 2"

        val result = CommandParser.parseBatteryInfo(battery)

        // 5/7 * 100 ≈ 71.43
        assertEquals(71.4f, result.levelPercent, 0.5f)
    }

    // ── parseDisplayInfo ──────────────────────────────────────────────────────

    @Test
    fun `parseDisplayInfo parses resolution density and refresh rate`() {
        val sizeOutput = "Physical size: 1080x2400"
        val densityOutput = "Physical density: 420"
        val displayOutput = """
            DisplayDeviceInfo{"Built-in Screen": uniqueId="Built-in Screen", ...
              renderFrameRate  120.0
              peakRefreshRate= 90.0}
        """.trimIndent()

        val result = CommandParser.parseDisplayInfo(sizeOutput, densityOutput, displayOutput)

        assertEquals("1080x2400", result.resolution)
        assertEquals("420", result.density)
        assertEquals("420", result.densityDpi)
        assertEquals("120.0 Hz", result.refreshRate)
    }

    @Test
    fun `parseDisplayInfo uses peakRefreshRate as fallback`() {
        val sizeOutput = "Physical size: 1440x3200"
        val densityOutput = "Physical density: 560"
        val displayOutput = """
            DisplayDeviceInfo:
              peakRefreshRate= 144.0
        """.trimIndent()

        val result = CommandParser.parseDisplayInfo(sizeOutput, densityOutput, displayOutput)

        assertEquals("1440x3200", result.resolution)
        assertEquals("560", result.densityDpi)
        assertEquals("144.0 Hz", result.refreshRate)
    }

    @Test
    fun `parseDisplayInfo uses SurfaceFlinger latency as fallback`() {
        val sizeOutput = "Physical size: 1080x2340"
        val densityOutput = "Physical density: 400"
        val displayOutput = "DisplayDeviceInfo: something"
        val sfLatency = "16666666\n0\n0\n0"

        val result = CommandParser.parseDisplayInfo(sizeOutput, densityOutput, displayOutput, sfLatency = sfLatency)

        // 1e9 / 16666666 ≈ 60
        assertEquals("60 Hz", result.refreshRate)
    }

    @Test
    fun `parseDisplayInfo uses refreshRateAlt when other sources are empty`() {
        val sizeOutput = "Physical size: 720x1600"
        val densityOutput = "Physical density: 270"
        val displayOutput = ""

        val result = CommandParser.parseDisplayInfo(sizeOutput, densityOutput, displayOutput, refreshRateAlt = "90")

        assertEquals("90 Hz", result.refreshRate)
    }

    @Test
    fun `parseDisplayInfo strips Physical size prefix`() {
        val result = CommandParser.parseDisplayInfo("Physical size: 2560x1440", "Physical density: 320", "")

        assertEquals("2560x1440", result.resolution)
    }

    // ── parseStorageInfo ──────────────────────────────────────────────────────

    @Test
    fun `parseStorageInfo parses df output with multiple filesystems`() {
        val df = """
            Filesystem             Size   Used  Avail Use% Mounted on
            /dev/block/sda1         30G   18G    11G  63% /system
            /dev/block/sda29        12G    8G     4G  66% /data
            /dev/block/sda31        16G    2G    14G  12% /cache
            tmpfs                  3.9G     0   3.9G   0% /dev
        """.trimIndent()

        val result = CommandParser.parseStorageInfo(df)

        assertEquals(4, result.filesystems.size)

        val system = result.filesystems.first { it.mountPoint == "/system" }
        assertEquals("/dev/block/sda1", system.filesystem)
        assertTrue(system.sizeBytes > 0)
        assertTrue(system.usedBytes > 0)
        assertEquals(60f, system.usagePercent, 1f)

        val tmpfs = result.filesystems.first { it.mountPoint == "/dev" }
        assertEquals(0f, tmpfs.usagePercent, 0.1f)
    }

    @Test
    fun `parseStorageInfo sorts emulated storage first`() {
        val df = """
            Filesystem             Size   Used  Avail Use% Mounted on
            /dev/block/sda1         30G   18G    11G  63% /system
            /dev/block/sda29        12G    8G     4G  66% /storage/emulated/0
        """.trimIndent()

        val result = CommandParser.parseStorageInfo(df)

        assertEquals("/storage/emulated/0", result.filesystems.first().mountPoint)
    }

    @Test
    fun `parseStorageInfo handles GB MB and KB units`() {
        val df = """
            Filesystem     1K-blocks  Used Available Use% Mounted on
            /dev/sda1        10485760 5242880   5242880  50% /system
            tmpfs              524288  262144    262144  50% /tmp
        """.trimIndent()

        val result = CommandParser.parseStorageInfo(df)

        val system = result.filesystems.first { it.mountPoint == "/system" }
        assertTrue(system.sizeBytes > 0)
    }

    // ── parseCameraInfo ───────────────────────────────────────────────────────

    @Test
    fun `parseCameraInfo parses dual camera output`() {
        val cameraOutput = """
            Camera HAL device legacy/0
            static information about Camera HAL device legacy/0:
            Facing: back
            Has a flash unit: true
            availableFocalLengths:
            [4.32]
            [0 4032 3024 OUTPUT]

            Camera HAL device legacy/1
            static information about Camera HAL device legacy/1:
            Facing: front
            Has a flash unit: false
            availableFocalLengths:
            [2.92]
            [0 3264 2448 OUTPUT]
        """.trimIndent()

        val result = CommandParser.parseCameraInfo(cameraOutput)

        assertEquals(2, result.size)

        val back = result.first { it.id == "0" }
        assertEquals("Trasera", back.facing)
        assertEquals("Si", back.flash)
        assertEquals("4032x3024", back.resolution)
        assertEquals("4.32 mm", back.focalLength)
        assertTrue(back.megapixels.contains("MP"))

        val front = result.first { it.id == "1" }
        assertEquals("Frontal", front.facing)
        assertEquals("No", front.flash)
        assertEquals("3264x2448", front.resolution)
        assertEquals("2.92 mm", front.focalLength)
    }

    @Test
    fun `parseCameraInfo falls back to device count when no HAL info`() {
        val output = "Number of camera devices: 3"

        val result = CommandParser.parseCameraInfo(output)

        assertEquals(3, result.size)
        assertEquals("Trasera", result[0].facing)
        assertEquals("Frontal", result[1].facing)
        assertEquals("Trasera", result[2].facing)
    }

    @Test
    fun `parseCameraInfo handles empty output`() {
        val result = CommandParser.parseCameraInfo("")
        assertTrue(result.isEmpty())
    }

    // ── parseSensorInfo ───────────────────────────────────────────────────────

    @Test
    fun `parseSensorInfo parses sensorservice output`() {
        val sensorOutput = buildString {
            appendLine("0x00000001) LSM6DSO Accelerometer | STMicroelectronics | ver: 1 | type: 1 (Acceleration-axis) |")
            appendLine("0x00000002) LSM6DSO Gyroscope | STMicroelectronics | ver: 1 | type: 4 (Gyroscope) |")
            appendLine("0x00000003) LSM6DSO Significant Motion | STMicroelectronics | ver: 1 | type: 17 (Significant motion) |")
            appendLine("0x00000004) TMD4903 Proximity | AMS | ver: 1 | type: 8 (Proximity) |")
            appendLine("0x00000005) TMD4903 Light | AMS | ver: 1 | type: 5 (Light) |")
            appendLine("0x00000006) AK09918 Magnetometer | AKM | ver: 1 | type: 2 (Magnetic-field) |")
        }

        val result = CommandParser.parseSensorInfo(sensorOutput)

        assertEquals(6, result.size)

        val accel = result[0]
        assertEquals("LSM6DSO Accelerometer", accel.name)
        assertEquals("STMicroelectronics", accel.vendor)
        assertEquals("1", accel.version)
        assertEquals("1 (Acceleration-axis)", accel.type)

        val magnetometer = result[5]
        assertEquals("AK09918 Magnetometer", magnetometer.name)
        assertEquals("AKM", magnetometer.vendor)
    }

    @Test
    fun `parseSensorInfo returns empty list for empty output`() {
        val result = CommandParser.parseSensorInfo("")
        assertTrue(result.isEmpty())
    }

    // ── parseNetworkInfo ──────────────────────────────────────────────────────

    @Test
    fun `parseNetworkInfo parses ip addr output`() {
        val ipAddr = """
            1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536 qdisc noqueue state UNKNOWN
                link/loopback 00:00:00:00:00:00 brd 00:00:00:00:00:00
                inet 127.0.0.1/8 scope host lo
                inet6 ::1/128 scope host
            2: wlan0: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500 qdisc pfifo_fast state UP
                link/ether 3c:5a:37:aa:bb:cc brd ff:ff:ff:ff:ff:ff
                inet 192.168.1.105/24 brd 192.168.1.255 scope global wlan0
                inet6 fe80::3e5a:37ff:feaa:bbcc/64 scope link
            3: rmnet0: <BROADCAST,MULTICAST,UP> mtu 1500 qdisc pfifo_fast state UP
                link/ether 00:00:00:00:00:00 brd ff:ff:ff:ff:ff:ff
                inet 10.132.128.83/28 brd 10.132.128.95 scope global rmnet0
        """.trimIndent()

        val result = CommandParser.parseNetworkInfo(ipAddr, wifiInterface = "wlan0")

        assertEquals("wlan0", result.wifiInterface)
        assertEquals(3, result.interfaces.size)

        val lo = result.interfaces.first { it.name == "lo" }
        assertEquals("127.0.0.1", lo.ipAddress)
        assertEquals("", lo.macAddress)

        val wlan = result.interfaces.first { it.name == "wlan0" }
        assertEquals("192.168.1.105", wlan.ipAddress)
        assertEquals("3C:5A:37:AA:BB:CC", wlan.macAddress)

        val rmnet = result.interfaces.first { it.name == "rmnet0" }
        assertEquals("10.132.128.83", rmnet.ipAddress)
    }

    @Test
    fun `parseNetworkInfo handles output with no interfaces`() {
        val result = CommandParser.parseNetworkInfo("", wifiInterface = "wlan0")
        assertTrue(result.interfaces.isEmpty())
        assertEquals("wlan0", result.wifiInterface)
    }

    // ── parseBuildInfo ────────────────────────────────────────────────────────

    @Test
    fun `parseBuildInfo extracts all build properties`() {
        val getprop = """
            [ro.product.board]: [exynos2100]
            [ro.bootloader]: [G991BXXS3CVF5]
            [ro.product.brand]: [samsung]
            [ro.product.device]: [o1s]
            [ro.build.display.id]: [SP1A.210812.016.G991BXXS3CVF5]
            [ro.build.fingerprint]: [samsung/o1sxx/o1s:12/SP1A.210812.016/G991BXXS3CVF5:user/release-keys]
            [ro.build.host]: [SRPPEM27B007]
            [ro.build.id]: [SP1A.210812.016]
            [ro.product.manufacturer]: [samsung]
            [ro.product.model]: [SM-G991B]
            [ro.product.name]: [o1sxx]
            [ro.build.tags]: [release-keys]
            [ro.build.type]: [user]
            [gsm.version.baseband]: [G991BXXS3CVF5]
        """.trimIndent()

        val result = CommandParser.parseBuildInfo(getprop, hardware = "exynos2100")

        assertEquals("exynos2100", result.board)
        assertEquals("G991BXXS3CVF5", result.bootloader)
        assertEquals("samsung", result.brand)
        assertEquals("o1s", result.device)
        assertEquals("SP1A.210812.016.G991BXXS3CVF5", result.display)
        assertEquals("samsung/o1sxx/o1s:12/SP1A.210812.016/G991BXXS3CVF5:user/release-keys", result.fingerprint)
        assertEquals("SRPPEM27B007", result.host)
        assertEquals("SP1A.210812.016", result.id)
        assertEquals("samsung", result.manufacturer)
        assertEquals("SM-G991B", result.model)
        assertEquals("o1sxx", result.product)
        assertEquals("release-keys", result.tags)
        assertEquals("user", result.type)
        assertEquals("G991BXXS3CVF5", result.baseband)
        assertEquals("exynos2100", result.kernel)
    }

    @Test
    fun `parseBuildInfo handles empty getprop`() {
        val result = CommandParser.parseBuildInfo("", hardware = "")
        assertEquals("", result.board)
        assertEquals("", result.model)
    }

    // ── parseImei ─────────────────────────────────────────────────────────────

    @Test
    fun `parseImei extracts IMEI from service call output`() {
        val serviceCall = """
            Result: Parcel(
              0x00000000: 00000000 0000000f 00000033 00000035 '......35'
              0x00000010: 00000033 00000034 00000035 00000036 '3456'
              0x00000020: 00000037 00000038 00000039 00000030 '7890'
              0x00000030: 00000031 00000032 00000033 00000034 '1234'
              0x00000040: 00000038 0000000a 00000000 00000000 '8...')
        """.trimIndent()

        val result = CommandParser.parseImei(serviceCall)
        assertEquals("353456789012348", result)
    }

    @Test
    fun `parseImei returns empty for invalid IMEI`() {
        val output = """
            Result: Parcel(
              0x00000000: 00000000 00000000 00313131 00313131 '......111.111'
              0x00000010: 00313131 00313131 00313131 00313131 '1111111111111')
        """.trimIndent()

        val result = CommandParser.parseImei(output)
        assertEquals("", result)
    }

    // ── parseImeiV2 ───────────────────────────────────────────────────────────

    @Test
    fun `parseImeiV2 extracts IMEI from cmd phone output`() {
        val cmdOutput = """
            Phone Information:
            Device ID = 353456789012348
            IMEI: 353456789012348
        """.trimIndent()

        val result = CommandParser.parseImeiV2(cmdOutput)
        assertEquals("353456789012348", result)
    }

    @Test
    fun `parseImeiV2 extracts IMEI with brackets`() {
        val cmdOutput = "IMEI: [353456789012348]"

        val result = CommandParser.parseImeiV2(cmdOutput)
        assertEquals("353456789012348", result)
    }

    @Test
    fun `parseImeiV2 returns empty for invalid output`() {
        val result = CommandParser.parseImeiV2("no imei here")
        assertEquals("", result)
    }

    // ── isValidImeiPublic (Luhn algorithm) ────────────────────────────────────

    @Test
    fun `isValidImeiPublic accepts valid 15-digit IMEI`() {
        assertTrue(CommandParser.isValidImeiPublic("353456789012348"))
    }

    @Test
    fun `isValidImeiPublic accepts valid 14-digit IMEI`() {
        assertTrue(CommandParser.isValidImeiPublic("35345678901230"))
    }

    @Test
    fun `isValidImeiPublic rejects all-same-digit IMEI`() {
        assertFalse(CommandParser.isValidImeiPublic("111111111111111"))
        assertFalse(CommandParser.isValidImeiPublic("000000000000000"))
    }

    @Test
    fun `isValidImeiPublic rejects IMEI that fails Luhn check`() {
        assertFalse(CommandParser.isValidImeiPublic("353456789012347"))
        assertFalse(CommandParser.isValidImeiPublic("123456789012345"))
    }

    @Test
    fun `isValidImeiPublic rejects non-numeric strings`() {
        assertFalse(CommandParser.isValidImeiPublic("35345678901234A"))
        assertFalse(CommandParser.isValidImeiPublic("3534567890123"))
    }

    @Test
    fun `isValidImeiPublic rejects too short strings`() {
        assertFalse(CommandParser.isValidImeiPublic("353456"))
        assertFalse(CommandParser.isValidImeiPublic(""))
    }

    @Test
    fun `isValidImeiPublic rejects too long strings`() {
        assertFalse(CommandParser.isValidImeiPublic("353456789012345678"))
    }

    // ── lookupSocCommercialName ───────────────────────────────────────────────

    @Test
    fun `lookupSocCommercialName finds Snapdragon 835`() {
        assertEquals("Snapdragon 835", CommandParser.lookupSocCommercialName("msm8998"))
    }

    @Test
    fun `lookupSocCommercialName finds Dimensity 920`() {
        assertEquals("Dimensity 920", CommandParser.lookupSocCommercialName("mt6877"))
    }

    @Test
    fun `lookupSocCommercialName finds Exynos 2100`() {
        assertEquals("Exynos 2100", CommandParser.lookupSocCommercialName("exynos2100"))
    }

    @Test
    fun `lookupSocCommercialName is case-insensitive`() {
        assertEquals("Snapdragon 835", CommandParser.lookupSocCommercialName("MSM8998"))
        assertEquals("Dimensity 920", CommandParser.lookupSocCommercialName("MT6877"))
        assertEquals("Exynos 2100", CommandParser.lookupSocCommercialName("EXYNOS2100"))
    }

    @Test
    fun `lookupSocCommercialName finds Snapdragon 8 Gen 3`() {
        assertEquals("Snapdragon 8 Gen 3", CommandParser.lookupSocCommercialName("sm8650"))
    }

    @Test
    fun `lookupSocCommercialName finds Kirin 990`() {
        assertEquals("Kirin 990", CommandParser.lookupSocCommercialName("kirin990"))
    }

    @Test
    fun `lookupSocCommercialName finds Unisoc T618`() {
        assertEquals("Unisoc T618", CommandParser.lookupSocCommercialName("t618"))
    }

    @Test
    fun `lookupSocCommercialName returns null for unknown codename`() {
        assertEquals(null, CommandParser.lookupSocCommercialName("unknown_soc"))
    }

    @Test
    fun `lookupSocCommercialName finds older Qualcomm chips`() {
        assertEquals("Snapdragon 800", CommandParser.lookupSocCommercialName("msm8974"))
        assertEquals("Snapdragon 410", CommandParser.lookupSocCommercialName("msm8916"))
        assertEquals("Snapdragon 625", CommandParser.lookupSocCommercialName("msm8953"))
    }

    @Test
    fun `lookupSocCommercialName finds MediaTek Helio chips`() {
        assertEquals("Helio G99", CommandParser.lookupSocCommercialName("mt6789"))
        assertEquals("Helio P60", CommandParser.lookupSocCommercialName("mt6771"))
        assertEquals("Dimensity 9000", CommandParser.lookupSocCommercialName("mt6983"))
    }

    // ── formatBytes ───────────────────────────────────────────────────────────

    @Test
    fun `formatBytes returns KB for small values`() {
        assertEquals("512 KB", CommandParser.formatBytes(512))
        assertEquals("1023 KB", CommandParser.formatBytes(1023))
    }

    @Test
    fun `formatBytes returns MB for medium values`() {
        assertEquals("1 MB", CommandParser.formatBytes(1024))
        assertEquals("2.0 GB", CommandParser.formatBytes(2097152))
    }

    @Test
    fun `formatBytes returns GB for large values`() {
        assertEquals("1.0 GB", CommandParser.formatBytes(1048576))
        assertEquals("4.0 GB", CommandParser.formatBytes(4194304))
        assertEquals("8.5 GB", CommandParser.formatBytes(8912896))
    }

    @Test
    fun `formatBytes handles zero`() {
        assertEquals("0 KB", CommandParser.formatBytes(0))
    }

    // ── formatBytesFromBytes ──────────────────────────────────────────────────

    @Test
    fun `formatBytesFromBytes returns B for tiny values`() {
        assertEquals("512 B", CommandParser.formatBytesFromBytes(512))
        assertEquals("1023 B", CommandParser.formatBytesFromBytes(1023))
    }

    @Test
    fun `formatBytesFromBytes returns KB for small values`() {
        assertEquals("1 KB", CommandParser.formatBytesFromBytes(1024))
        assertEquals("5 KB", CommandParser.formatBytesFromBytes(5120))
    }

    @Test
    fun `formatBytesFromBytes returns MB for medium values`() {
        assertEquals("1 MB", CommandParser.formatBytesFromBytes(1048576))
        assertEquals("2 MB", CommandParser.formatBytesFromBytes(2097152))
    }

    @Test
    fun `formatBytesFromBytes returns GB for large values`() {
        assertEquals("1.0 GB", CommandParser.formatBytesFromBytes(1073741824))
        assertEquals("4.0 GB", CommandParser.formatBytesFromBytes(4294967296))
        assertEquals("8.5 GB", CommandParser.formatBytesFromBytes(9126805504))
    }

    @Test
    fun `formatBytesFromBytes handles zero`() {
        assertEquals("0 B", CommandParser.formatBytesFromBytes(0))
    }

    @Test
    fun `formatBytesFromBytes handles exact boundaries`() {
        assertEquals("1 KB", CommandParser.formatBytesFromBytes(1024))
        assertEquals("1 MB", CommandParser.formatBytesFromBytes(1048576))
        assertEquals("1.0 GB", CommandParser.formatBytesFromBytes(1073741824))
    }
}
