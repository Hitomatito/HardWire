package com.hitomatito.hardwire.data.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import com.hitomatito.hardwire.data.command.CommandParser
import com.hitomatito.hardwire.data.model.BatteryInfo
import com.hitomatito.hardwire.data.model.BuildInfo
import com.hitomatito.hardwire.data.model.CameraInfo
import com.hitomatito.hardwire.data.model.CpuInfo
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.data.model.DisplayInfo
import com.hitomatito.hardwire.data.model.FileSystemInfo
import com.hitomatito.hardwire.data.model.GeneralInfo
import com.hitomatito.hardwire.data.model.MemoryInfo
import com.hitomatito.hardwire.data.model.NetworkInfo
import com.hitomatito.hardwire.data.model.NetworkInterface
import com.hitomatito.hardwire.data.model.SensorInfo
import com.hitomatito.hardwire.data.model.StorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

suspend fun collectLocalDeviceInfo(context: Context): DeviceInfo = withContext(Dispatchers.IO) {
    val general = collectGeneralInfo()
    val cpu = collectCpuInfo()
    val memory = collectMemoryInfo(context)
    val battery = collectBatteryInfo(context)
    val display = collectDisplayInfo(context)
    val storage = collectStorageInfo()
    val cameras = collectCameraInfo(context)
    val sensors = collectSensorInfo(context)
    val network = collectNetworkInfo()
    val build = collectBuildInfo()

    DeviceInfo(
        general = general,
        cpu = cpu,
        memory = memory,
        battery = battery,
        display = display,
        storage = storage,
        cameras = cameras,
        sensors = sensors,
        network = network,
        build = build
    )
}

private fun collectGeneralInfo(): GeneralInfo {
    val serial = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        "unknown"
    } else {
        @Suppress("DEPRECATION")
        Build.SERIAL ?: "unknown"
    }

    return GeneralInfo(
        manufacturer = Build.MANUFACTURER ?: "",
        model = Build.MODEL ?: "",
        marketName = Build.PRODUCT ?: "",
        device = Build.DEVICE ?: "",
        board = Build.BOARD ?: "",
        hardware = Build.HARDWARE ?: "",
        serialNumber = serial,
        imeis = emptyList(),
        androidVersion = Build.VERSION.RELEASE ?: "",
        sdkVersion = Build.VERSION.SDK_INT.toString(),
        fingerprint = Build.FINGERPRINT ?: "",
        phone = ""
    )
}

private fun collectCpuInfo(): CpuInfo {
    var processor = ""
    var hardware = ""
    var features = ""
    var bogoMips = ""
    var cpuConfig = StringBuilder()
    var processorCount = 0
    val cpuParts = mutableListOf<String>() // per-core CPU part codes
    val cpuImplementers = mutableListOf<String>() // per-core implementer codes

    try {
        val cpuInfoFile = File("/proc/cpuinfo")
        if (cpuInfoFile.exists()) {
            val lines = cpuInfoFile.readLines()
            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("processor", ignoreCase = true)
                            && trimmed.substringAfter(":").trim().toIntOrNull() != null -> {
                        processorCount++
                    }
                    trimmed.startsWith("Processor") && !trimmed.startsWith("processor") -> {
                        val value = trimmed.substringAfter(":").trim()
                        if (value.isNotBlank()) processor = value
                    }
                    trimmed.startsWith("Hardware") -> hardware = trimmed.substringAfter(":").trim()
                    trimmed.startsWith("Features") -> features = trimmed.substringAfter(":").trim()
                    trimmed.startsWith("BogoMIPS") -> {
                        val value = trimmed.substringAfter(":").trim()
                        if (bogoMips.isBlank()) bogoMips = value
                    }
                    trimmed.startsWith("CPU implementer") -> {
                        cpuImplementers.add(trimmed.substringAfter(":").trim())
                    }
                    trimmed.startsWith("CPU part") -> {
                        cpuParts.add(trimmed.substringAfter(":").trim())
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Ignore read errors
    }

    if (processorCount == 0) {
        processorCount = Runtime.getRuntime().availableProcessors()
    }

    val architecture = Build.SUPPORTED_ABIS?.firstOrNull() ?: ""
    val cpuAbi = Build.CPU_ABI ?: Build.SUPPORTED_ABIS?.firstOrNull() ?: ""
    val gpu = "" // GLES20.glGetString is unsafe off GL thread (causes SIGSEGV)

    // --- SoC detection via system properties + /sys/devices/soc0/ ---
    val socModel = getSystemProperty("ro.soc.model")
    val socManufacturerRaw = getSystemProperty("ro.soc.manufacturer")
    val socPlatform = getSystemProperty("ro.board.platform")
    val socChipname = getSystemProperty("ro.hardware.chipname")
    val socVendorModel = getSystemProperty("ro.vendor.soc.model")

    val soc0Family = readFileTrimmed("/sys/devices/soc0/family")
    val soc0Machine = readFileTrimmed("/sys/devices/soc0/machine")

    // Use CommandParser's SOC_NAMES database for lookup
    val platform = socPlatform.trim().lowercase(Locale.US)
    val model = socModel.trim().ifBlank { socVendorModel.trim() }
    val machine = soc0Machine.trim().lowercase(Locale.US)

    val commercialName = CommandParser.lookupSocCommercialName(platform)
        ?: CommandParser.lookupSocCommercialName(model.lowercase(Locale.US))
        ?: CommandParser.lookupSocCommercialName(machine)
        ?: CommandParser.lookupSocCommercialName(socChipname.trim().lowercase(Locale.US))

    val socName: String
    val socManufacturer: String

    if (commercialName != null) {
        socName = commercialName
        // Resolve manufacturer from raw prop, soc0 family, or commercial name prefix
        socManufacturer = when {
            socManufacturerRaw.isNotBlank() -> normalizeSocManufacturer(socManufacturerRaw)
            soc0Family.isNotBlank() -> normalizeSocManufacturer(soc0Family)
            else -> when {
                commercialName.startsWith("Snapdragon") -> "Qualcomm"
                commercialName.startsWith("Helio") || commercialName.startsWith("Dimensity") -> "MediaTek"
                commercialName.startsWith("Exynos") -> "Samsung"
                commercialName.startsWith("Kirin") -> "HiSilicon"
                commercialName.startsWith("Unisoc") -> "Unisoc"
                else -> ""
            }
        }
    } else {
        // Fallback: try Hardware line (e.g. "Qualcomm Technologies, Inc SM8150")
        val hwSoc = parseSocFromHardwareLine(hardware)
        if (hwSoc != null) {
            socName = hwSoc
            socManufacturer = hwSoc.substringBefore(" ").trim()
        } else {
            socName = model.ifBlank { socPlatform.trim() }
            socManufacturer = normalizeSocManufacturer(socManufacturerRaw)
        }
    }

    // Build CPU config with core cluster info
    val clusterInfo = buildClusterInfo(cpuParts, cpuImplementers)
    val formattedProcessor = if (socName.isNotBlank()) {
        "$socName (${cpuParts.size} cores)"
    } else {
        processor.ifBlank { "$processorCount cores" }
    }

    return CpuInfo(
        socName = socName,
        socManufacturer = socManufacturer,
        processor = formattedProcessor,
        hardware = hardware.ifBlank { Build.HARDWARE },
        features = features,
        processorCount = processorCount,
        bogoMips = bogoMips,
        architecture = architecture,
        cpuAbi = cpuAbi,
        gpu = gpu,
        cpuConfig = clusterInfo.ifBlank { cpuConfig.toString().trim() }
    )
}

private fun collectMemoryInfo(context: Context): MemoryInfo {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memoryInfo)

    val totalBytes = memoryInfo.totalMem
    val availableBytes = memoryInfo.availMem
    val freeBytes = memoryInfo.availMem

    var swapTotalBytes = totalBytes
    var swapFreeBytes = 0L

    try {
        val memInfoFile = File("/proc/meminfo")
        if (memInfoFile.exists()) {
            val lines = memInfoFile.readLines()
            for (line in lines) {
                when {
                    line.startsWith("SwapTotal") -> {
                        val kb = extractKbValue(line)
                        swapTotalBytes = kb * 1024
                    }
                    line.startsWith("SwapFree") -> {
                        val kb = extractKbValue(line)
                        swapFreeBytes = kb * 1024
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Ignore read errors
    }

    val usagePercent = if (totalBytes > 0) {
        ((1 - availableBytes.toFloat() / totalBytes) * 100).coerceIn(0f, 100f)
    } else 0f

    return MemoryInfo(
        totalRamBytes = totalBytes,
        freeRamBytes = freeBytes,
        availableRamBytes = availableBytes,
        cachedBytes = 0,
        buffersBytes = 0,
        totalSwapBytes = swapTotalBytes,
        freeSwapBytes = swapFreeBytes,
        totalRamFormatted = formatBytesFromBytes(totalBytes),
        freeRamFormatted = formatBytesFromBytes(freeBytes),
        availableRamFormatted = formatBytesFromBytes(availableBytes),
        cachedFormatted = "0 B",
        totalSwapFormatted = formatBytesFromBytes(swapTotalBytes),
        freeSwapFormatted = formatBytesFromBytes(swapFreeBytes),
        usagePercent = usagePercent
    )
}

private fun collectBatteryInfo(context: Context): BatteryInfo {
    val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    if (batteryStatus == null) {
        return BatteryInfo()
    }

    val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
    val technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""
    val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
    val voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
    val plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

    val statusString = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Cargando"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descargando"
        BatteryManager.BATTERY_STATUS_FULL -> "Completo"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Sin carga"
        else -> "Desconocido"
    }

    val healthString = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Buena"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Sobrecalentada"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Muerta"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobrevoltaje"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Fallo"
        BatteryManager.BATTERY_HEALTH_COLD -> "Fria"
        else -> "Desconocido"
    }

    val pluggedString = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Inalambrico"
        else -> "Sin carga"
    }

    val tempFormatted = if (temperature > 0) {
        String.format(Locale.US, "%.1f °C", temperature / 10.0)
    } else ""

    val voltageFormatted = if (voltage > 0) {
        "$voltage mV"
    } else ""

    val levelPercent = if (scale > 0) {
        (level.toFloat() / scale.toFloat() * 100).coerceIn(0f, 100f)
    } else 0f

    return BatteryInfo(
        level = level.toString(),
        scale = scale.toString(),
        status = statusString,
        health = healthString,
        technology = technology,
        temperature = tempFormatted,
        voltage = voltageFormatted,
        plugged = pluggedString,
        levelPercent = levelPercent
    )
}

private fun collectDisplayInfo(context: Context): DisplayInfo {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = windowManager.defaultDisplay
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    display.getMetrics(metrics)

    val resolution = "${metrics.widthPixels}x${metrics.heightPixels}"

    val density = when (metrics.densityDpi) {
        DisplayMetrics.DENSITY_LOW -> "ldpi"
        DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
        DisplayMetrics.DENSITY_TV -> "tvdpi"
        DisplayMetrics.DENSITY_HIGH -> "hdpi"
        DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
        DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
        DisplayMetrics.DENSITY_XXXHIGH -> "xxxhdpi"
        else -> "${metrics.densityDpi}dpi"
    }

    val refreshRate = "${display.refreshRate.toInt()} Hz"

    return DisplayInfo(
        resolution = resolution,
        density = density,
        densityDpi = metrics.densityDpi.toString(),
        refreshRate = refreshRate,
        displayInfo = ""
    )
}

private fun collectStorageInfo(): StorageInfo {
    val stat = StatFs(Environment.getDataDirectory().path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong

    val totalBytes = totalBlocks * blockSize
    val availableBytes = availableBlocks * blockSize
    val usedBytes = totalBytes - availableBytes

    val usagePercent = if (totalBytes > 0) {
        (usedBytes.toFloat() / totalBytes * 100).coerceIn(0f, 100f)
    } else 0f

    val mainStorage = FileSystemInfo(
        filesystem = "userdata",
        sizeFormatted = formatBytesFromBytes(totalBytes),
        usedFormatted = formatBytesFromBytes(usedBytes),
        availableFormatted = formatBytesFromBytes(availableBytes),
        mountPoint = "/data",
        sizeBytes = totalBytes,
        usedBytes = usedBytes,
        availableBytes = availableBytes,
        usagePercent = usagePercent
    )

    return StorageInfo(filesystems = listOf(mainStorage))
}

private fun collectCameraInfo(context: Context): List<CameraInfo> {
    val cameras = mutableListOf<CameraInfo>()

    try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList

        for (id in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(id)

            val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                CameraCharacteristics.LENS_FACING_FRONT -> "Frontal"
                CameraCharacteristics.LENS_FACING_BACK -> "Trasera"
                CameraCharacteristics.LENS_FACING_EXTERNAL -> "Externa"
                else -> "Desconocido"
            }

            val pixelArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            val resolution = if (pixelArray != null) {
                "${pixelArray.width}x${pixelArray.height}"
            } else ""

            val megapixels = if (pixelArray != null) {
                val mp = (pixelArray.width.toLong() * pixelArray.height) / 1_000_000.0
                String.format(Locale.US, "%.1f MP", mp)
            } else ""

            val flash = if (characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                "Si"
            } else {
                "No"
            }

            cameras.add(
                CameraInfo(
                    id = id,
                    facing = facing,
                    megapixels = megapixels,
                    resolution = resolution,
                    flash = flash,
                    focalLength = ""
                )
            )
        }
    } catch (e: Exception) {
        // Ignore camera errors
    }

    return cameras
}

private fun collectSensorInfo(context: Context): List<SensorInfo> {
    val sensors = mutableListOf<SensorInfo>()

    try {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)

        for (sensor in sensorList) {
            sensors.add(
                SensorInfo(
                    name = sensor.name,
                    vendor = sensor.vendor,
                    type = getSensorTypeName(sensor.type),
                    version = sensor.version.toString(),
                    maxRate = "${sensor.maximumRange} ${getSensorUnit(sensor.type)}",
                    fifoSize = sensor.fifoReservedEventCount.toString(),
                    wakeUp = sensor.isWakeUpSensor
                )
            )
        }
    } catch (e: Exception) {
        // Ignore sensor errors
    }

    return sensors
}

private fun getSensorTypeName(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER -> "Acelerometro"
    Sensor.TYPE_GYROSCOPE -> "Giroscopio"
    Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometro"
    Sensor.TYPE_LIGHT -> "Sensor de luz"
    Sensor.TYPE_PROXIMITY -> "Sensor de proximidad"
    Sensor.TYPE_PRESSURE -> "Barometro"
    Sensor.TYPE_GRAVITY -> "Gravedad"
    Sensor.TYPE_LINEAR_ACCELERATION -> "Aceleracion lineal"
    Sensor.TYPE_ROTATION_VECTOR -> "Vector de rotacion"
    Sensor.TYPE_STEP_COUNTER -> "Contador de pasos"
    Sensor.TYPE_STEP_DETECTOR -> "Detector de pasos"
    Sensor.TYPE_SIGNIFICANT_MOTION -> "Movimiento significativo"
    Sensor.TYPE_GAME_ROTATION_VECTOR -> "Vector de rotacion (juego)"
    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Vector de rotacion geomagnetico"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperatura ambiente"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "Humedad relativa"
    else -> "Tipo $type"
}

private fun getSensorUnit(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"
    Sensor.TYPE_GYROSCOPE -> "rad/s"
    Sensor.TYPE_MAGNETIC_FIELD -> "μT"
    Sensor.TYPE_LIGHT -> "lux"
    Sensor.TYPE_PROXIMITY -> "cm"
    Sensor.TYPE_PRESSURE -> "hPa"
    Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
    Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
    else -> ""
}

private fun collectNetworkInfo(): NetworkInfo {
    val interfaces = mutableListOf<com.hitomatito.hardwire.data.model.NetworkInterface>()

    try {
        val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()
        if (networkInterfaces != null) {
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val name = networkInterface.name
                val mac = networkInterface.hardwareAddress?.joinToString(":") {
                    String.format(Locale.US, "%02X", it)
                } ?: ""
                val flags = buildString {
                    if (networkInterface.isUp) append("UP")
                    if (networkInterface.isLoopback) append(",LOOPBACK")
                    if (networkInterface.isPointToPoint) append(",POINTOPOINT")
                    if (networkInterface.supportsMulticast()) append(",MULTICAST")
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        interfaces.add(
                            com.hitomatito.hardwire.data.model.NetworkInterface(
                                name = name,
                                ipAddress = address.hostAddress ?: "",
                                macAddress = mac,
                                flags = flags
                            )
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Ignore network errors
    }

    return NetworkInfo(
        interfaces = interfaces,
        wifiInterface = ""
    )
}

private fun collectBuildInfo(): BuildInfo {
    var kernel = ""
    try {
        val versionFile = File("/proc/version")
        if (versionFile.exists()) {
            kernel = versionFile.readText().trim()
        }
    } catch (e: Exception) {
        // Ignore read errors
    }

    return BuildInfo(
        board = Build.BOARD ?: "",
        bootloader = Build.BOOTLOADER ?: "",
        brand = Build.BRAND ?: "",
        device = Build.DEVICE ?: "",
        display = Build.DISPLAY ?: "",
        fingerprint = Build.FINGERPRINT ?: "",
        host = Build.HOST ?: "",
        id = Build.ID ?: "",
        manufacturer = Build.MANUFACTURER ?: "",
        model = Build.MODEL ?: "",
        product = Build.PRODUCT ?: "",
        tags = Build.TAGS ?: "",
        type = Build.TYPE ?: "",
        baseband = try {
            Build.getRadioVersion() ?: ""
        } catch (e: Exception) {
            ""
        },
        kernel = kernel
    )
}

private fun getSystemProperty(key: String): String {
    return try {
        val clazz = Class.forName("android.os.SystemProperties")
        val get = clazz.getMethod("get", String::class.java)
        get.invoke(null, key) as? String ?: ""
    } catch (e: Exception) {
        ""
    }
}

private fun readFileTrimmed(path: String): String {
    return try {
        val file = File(path)
        if (file.exists()) file.readText().trim() else ""
    } catch (e: Exception) {
        ""
    }
}

private fun normalizeSocManufacturer(raw: String): String {
    return when (raw.trim().uppercase(Locale.US)) {
        "QTI", "QUALCOMM" -> "Qualcomm"
        "SAMSUNG" -> "Samsung"
        "MTK", "MEDIATEK" -> "MediaTek"
        "HISILICON", "HI-SILICON" -> "HiSilicon"
        "UNISOC", "SPREADTRUM" -> "Unisoc"
        "SNAPDRAGON" -> "Qualcomm"
        "HELIO", "DIMENSITY" -> "MediaTek"
        "EXYNOS" -> "Samsung"
        "KIRIN" -> "HiSilicon"
        else -> raw.trim()
    }
}

private fun parseSocFromHardwareLine(hardware: String): String? {
    // "Qualcomm Technologies, Inc SM8150" -> "Qualcomm SM8150"
    // Try to extract model number (e.g. SM8150, MT6785, Exynos 990)
    val modelRegex = Regex("""(SM\d{4}[A-Z]?|MTK\d{4}|Exynos\s*\d+|SDM\d{3,4}|MSM\d{3,4}|Dimensity\s*\d+|Kirin\s*\d+)""", RegexOption.IGNORE_CASE)
    val match = modelRegex.find(hardware) ?: return null
    val model = match.value
    val manufacturer = when {
        hardware.contains("Qualcomm", ignoreCase = true) -> "Qualcomm"
        hardware.contains("MediaTek", ignoreCase = true) -> "MediaTek"
        hardware.contains("Samsung", ignoreCase = true) -> "Samsung"
        hardware.contains("HiSilicon", ignoreCase = true) -> "HiSilicon"
        else -> ""
    }
    // Try the SOC_NAMES database with the extracted model
    val commercial = CommandParser.lookupSocCommercialName(model.lowercase(Locale.US))
    return if (commercial != null) {
        if (manufacturer.isNotBlank()) "$manufacturer $commercial" else commercial
    } else {
        if (manufacturer.isNotBlank()) "$manufacturer $model" else model
    }
}

/** CPU implementer hex -> manufacturer, CPU part hex -> core name */
private fun buildClusterInfo(cpuParts: List<String>, cpuImplementers: List<String>): String {
    if (cpuParts.isEmpty()) return ""

    // Group cores by CPU part to identify clusters
    val clusters = cpuParts.groupingBy { it }.eachCount()
    if (clusters.isEmpty()) return ""

    val implementer = cpuImplementers.firstOrNull()?.uppercase(Locale.US) ?: ""
    val vendorName = when (implementer) {
        "0x51" -> "Qualcomm"
        "0x41" -> "ARM"
        "0x53" -> "Samsung"
        "0x46" -> "Faraday"
        "0x69" -> "Intel"
        else -> ""
    }

    val sb = StringBuilder()
    if (vendorName.isNotBlank()) {
        sb.appendLine("CPU Vendor: $vendorName")
    }

    for ((partHex, count) in clusters.entries.sortedByDescending { it.value }) {
        val coreName = lookupCpuCoreName(partHex)
        val partDecimal = partHex.removePrefix("0x").toIntOrNull(16)
        val partLabel = if (partDecimal != null) "$partHex ($partDecimal)" else partHex
        sb.appendLine("$count x $coreName [$partLabel]")
    }

    return sb.toString().trim()
}

private fun lookupCpuCoreName(partHex: String): String {
    val part = partHex.removePrefix("0x").lowercase(Locale.US)
    return when (part) {
        // ARM Cortex cores
        "0xd03", "d03" -> "Cortex-A53"
        "0xd04", "d04" -> "Cortex-A35"
        "0xd05", "d05" -> "Cortex-A55"
        "0xd06", "d06" -> "Cortex-A65"
        "0xd07", "d07" -> "Cortex-A57"
        "0xd08", "d08" -> "Cortex-A72"
        "0xd09", "d09" -> "Cortex-A73"
        "0xd0a", "d0a" -> "Cortex-A75"
        "0xd0b", "d0b" -> "Cortex-A76"
        "0xd0c", "d0c" -> "Neoverse-N1"
        "0xd0d", "d0d" -> "Cortex-A77"
        "0xd40", "d40" -> "Neoverse-V1"
        "0xd41", "d41" -> "Cortex-A78"
        "0xd42", "d42" -> "Cortex-A78AE"
        "0xd43", "d43" -> "Cortex-A65AE"
        "0xd44", "d44" -> "Cortex-X1"
        "0xd46", "d46" -> "Cortex-A510"
        "0xd47", "d47" -> "Cortex-A710"
        "0xd48", "d48" -> "Cortex-X2"
        "0xd49", "d49" -> "Neoverse-N2"
        "0xd4a", "d4a" -> "Neoverse-E1"
        "0xd4b", "d4b" -> "Cortex-A78C"
        "0xd4c", "d4c" -> "Cortex-X1C"
        "0xd4d", "d4d" -> "Cortex-A715"
        "0xd4e", "d4e" -> "Cortex-X3"
        "0xd4f", "d4f" -> "Neoverse-V2"
        "0xd80", "d80" -> "Cortex-A520"
        "0xd81", "d81" -> "Cortex-A720"
        "0xd82", "d82" -> "Cortex-X4"
        "0xd84", "d84" -> "Neoverse-V3"
        "0xd85", "d85" -> "Cortex-X925"
        "0xd87", "d87" -> "Cortex-A725"
        "0xd89", "d89" -> "Cortex-A520AE"
        // Qualcomm custom cores
        "800" -> "Kryo (Silver)"
        "801" -> "Kryo (Gold)"
        "802" -> "Kryo 200 (Gold)"
        "803" -> "Kryo 200 (Silver)"
        "804" -> "Kryo 485 (Silver)"
        "805" -> "Kryo 485 (Gold)"
        // Samsung
        "0x001" -> "Mongoose M1"
        "0x002" -> "Mongoose M2"
        "0x003" -> "Mongoose M3"
        "0x004" -> "Mongoose M4"
        "0x005" -> "Mongoose M5"
        // Apple
        "7000", "7001", "7002", "7003" -> "Apple Lightning"
        "8000", "8001", "8002", "8003" -> "Apple Monsoon/Mistral"
        "8010", "8011", "8012", "8015" -> "Apple Vortex/Tempest"
        "8020", "8021", "8022", "8028" -> "Apple Icestorm/Firestorm"
        "8030", "8031", "8032", "8033", "8034", "8035" -> "Apple Blizzard/Avalanche"
        else -> "CPU part $partHex"
    }
}

private fun formatBytesFromBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format(Locale.US, "%.0f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun extractKbValue(line: String): Long {
    val value = line.substringAfter(":").trim().replace(Regex("[^0-9]"), "")
    return value.toLongOrNull() ?: 0L
}
