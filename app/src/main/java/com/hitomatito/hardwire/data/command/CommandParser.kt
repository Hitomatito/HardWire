package com.hitomatito.hardwire.data.command

import com.hitomatito.hardwire.data.chipset.ChipsetInfo
import com.hitomatito.hardwire.data.model.*
import java.util.Locale

object CommandParser {

    private val SOC_NAMES: Map<String, String> = buildMap {
        // Qualcomm Snapdragon - MSM series (older naming)
        put("msm7225", "Snapdragon S1")
        put("msm7227", "Snapdragon S1")
        put("msm7230", "Snapdragon S2")
        put("msm8255", "Snapdragon S2")
        put("msm8260", "Snapdragon S3")
        put("msm8660", "Snapdragon S3")
        put("msm8960", "Snapdragon S4 Pro")
        put("msm8974", "Snapdragon 800")
        put("msm8974pro", "Snapdragon 801")
        put("msm8226", "Snapdragon 400")
        put("msm8926", "Snapdragon 400")
        put("msm8928", "Snapdragon 400")
        put("msm8916", "Snapdragon 410")
        put("msm8918", "Snapdragon 425")
        put("msm8920", "Snapdragon 412")
        put("msm8929", "Snapdragon 615")
        put("msm8937", "Snapdragon 430")
        put("msm8939", "Snapdragon 615")
        put("msm8940", "Snapdragon 435")
        put("msm8952", "Snapdragon 617")
        put("msm8953", "Snapdragon 625")
        put("msm8953pro", "Snapdragon 626")
        put("msm8976", "Snapdragon 652")
        put("msm8976pro", "Snapdragon 653")
        put("msm8992", "Snapdragon 808")
        put("msm8994", "Snapdragon 810")
        put("msm8996", "Snapdragon 820")
        put("msm8996pro", "Snapdragon 821")
        put("msm8998", "Snapdragon 835")
        // Qualcomm Snapdragon - SDM series
        put("sdm450", "Snapdragon 450")
        put("sdm630", "Snapdragon 630")
        put("sdm636", "Snapdragon 636")
        put("sdm660", "Snapdragon 660")
        put("sdm670", "Snapdragon 670")
        put("sdm710", "Snapdragon 710")
        put("sdm845", "Snapdragon 845")
        // Qualcomm Snapdragon - SM series
        put("sm6125", "Snapdragon 665")
        put("sm6150", "Snapdragon 675")
        put("sm7125", "Snapdragon 720G")
        put("sm7150", "Snapdragon 730")
        put("sm7150-ab", "Snapdragon 730G")
        put("sm7150-ac", "Snapdragon 732G")
        put("sm7250", "Snapdragon 720G")
        put("sm7250-ab", "Snapdragon 730G")
        put("sm7250-ac", "Snapdragon 732G")
        put("sm7325", "Snapdragon 778G")
        put("sm7350", "Snapdragon 780G")
        put("sm8150", "Snapdragon 855")
        put("sm8150-ac", "Snapdragon 855+")
        put("sm8250", "Snapdragon 865")
        put("sm8250-ab", "Snapdragon 865+")
        put("sm8350", "Snapdragon 888")
        put("sm8450", "Snapdragon 8 Gen 1")
        put("sm8475", "Snapdragon 8+ Gen 1")
        put("sm8550", "Snapdragon 8 Gen 2")
        put("sm8650", "Snapdragon 8 Gen 3")
        put("sm6375", "Snapdragon 695")
        put("sm6475", "Snapdragon 778G+")
        put("sm4450", "Snapdragon 4 Gen 1")
        put("sm4375", "Snapdragon 480")
        put("sm4350", "Snapdragon 480+")
        put("sm6350", "Snapdragon 690")
        put("sm6325", "Snapdragon 720G")
        put("sm7250-aa", "Snapdragon 750G")
        put("sm7350", "Snapdragon 780G")
        // Qualcomm QCS series (automotive/IoT)
        put("qcs605", "QCS605")
        put("qcs6490", "QCS6490")
        // Qualcomm QM/QCS
        put("qcm6490", "QCM6490")
        // MediaTek Helio A series
        put("mt6739", "Helio A22")
        put("mt6761", "Helio A22")
        put("mt6762", "Helio P22")
        put("mt6765", "Helio P35")
        put("mt6768", "Helio G80")
        put("mt6769", "Helio P35")
        put("mt6779", "Helio G85")
        put("mt6781", "Helio G88")
        put("mt6785", "Helio G90T")
        put("mt6789", "Helio G99")
        // MediaTek Helio P series
        put("mt6750", "Helio P10")
        put("mt6752", "Helio P10")
        put("mt6753", "Helio P10")
        put("mt6755", "Helio P10")
        put("mt6757", "Helio P20")
        put("mt6758", "Helio P25")
        put("mt6763", "Helio P23")
        put("mt6771", "Helio P60")
        put("mt6775", "Helio G90")
        // MediaTek Helio X series
        put("mt6595", "Helio X10")
        put("mt6795", "Helio X10")
        put("mt6797", "Helio X20")
        put("mt6757cd", "Helio X20")
        put("mt6799", "Helio X30")
        // MediaTek Dimensity
        put("mt6833", "Dimensity 700")
        put("mt6853", "Dimensity 720")
        put("mt6873", "Dimensity 800U")
        put("mt6875", "Dimensity 700")
        put("mt6877", "Dimensity 920")
        put("mt6879", "Dimensity 930")
        put("mt6883", "Dimensity 1000+")
        put("mt6885", "Dimensity 1000")
        put("mt6889", "Dimensity 1000L")
        put("mt6891", "Dimensity 1200")
        put("mt6893", "Dimensity 1200")
        put("mt6983", "Dimensity 9000")
        put("mt6985", "Dimensity 9200")
        put("mt6991", "Dimensity 9300")
        // MediaTek older
        put("mt6572", "MT6572")
        put("mt6573", "MT6573")
        put("mt6580", "MT6580")
        put("mt6582", "MT6582")
        put("mt6589", "MT6589")
        put("mt6592", "MT6592")
        put("mt6735", "MT6735")
        put("mt6737", "MT6737")
        // Samsung Exynos
        put("exynos3475", "Exynos 3475")
        put("exynos5420", "Exynos 5420")
        put("exynos5422", "Exynos 5422")
        put("exynos7420", "Exynos 7420")
        put("exynos7570", "Exynos 7570")
        put("exynos7870", "Exynos 7870")
        put("exynos7880", "Exynos 7880")
        put("exynos7884", "Exynos 7884")
        put("exynos7885", "Exynos 7885")
        put("exynos7904", "Exynos 7904")
        put("exynos880", "Exynos 880")
        put("exynos8890", "Exynos 8890")
        put("exynos8895", "Exynos 8895")
        put("exynos9610", "Exynos 9610")
        put("exynos9611", "Exynos 9611")
        put("exynos980", "Exynos 980")
        put("exynos9810", "Exynos 9810")
        put("exynos9820", "Exynos 9820")
        put("exynos9825", "Exynos 9825")
        put("exynos990", "Exynos 990")
        put("exynos2100", "Exynos 2100")
        put("exynos2200", "Exynos 2200")
        // HiSilicon Kirin
        put("kirin655", "Kirin 655")
        put("kirin65x", "Kirin 65x")
        put("kirin710", "Kirin 710")
        put("kirin950", "Kirin 950")
        put("kirin955", "Kirin 955")
        put("kirin960", "Kirin 960")
        put("kirin970", "Kirin 970")
        put("kirin980", "Kirin 980")
        put("kirin990", "Kirin 990")
        put("kirin9000", "Kirin 9000")
        put("kirin9010", "Kirin 9010")
        // Unisoc/Spreadtrum
        put("t618", "Unisoc T618")
        put("t610", "Unisoc T610")
        put("t612", "Unisoc T612")
        put("t616", "Unisoc T616")
        put("t710", "Unisoc T710")
        put("t770", "Unisoc T770")
        put("s5p9818", "Exynos 7870")
        put("s5e9815", "Exynos 9815")
        put("s5e9825", "Exynos 9825")
        // Apple (for completeness, though rare on non-Apple)
        put("t8103", "Apple M1")
        put("t8112", "Apple M2")
    }

    fun lookupSocCommercialName(codename: String): String? {
        return SOC_NAMES[codename.lowercase(Locale.US)]
    }

    fun formatBytes(kb: Long): String {
        return when {
            kb >= 1_048_576 -> String.format(Locale.US, "%.1f GB", kb / 1_048_576.0)
            kb >= 1024 -> String.format(Locale.US, "%.0f MB", kb / 1024.0)
            else -> "$kb KB"
        }
    }

    fun formatBytesFromBytes(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format(Locale.US, "%.0f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    fun parseGeneralInfo(
        getpropOutput: String,
        socModel: String,
        socManufacturer: String,
        socChipname: String,
        socPlatform: String,
        socVendorModel: String,
        marketName: String,
        soc0Family: String = "",
        soc0Machine: String = "",
        imeis: List<String> = emptyList()
    ): GeneralInfo {
        val props = parseGetProp(getpropOutput)
        val socName = resolveSocName(socModel, socManufacturer, socChipname, socPlatform, socVendorModel, soc0Family, soc0Machine)
        return GeneralInfo(
            manufacturer = props["ro.product.manufacturer"] ?: "",
            model = props["ro.product.model"] ?: "",
            marketName = marketName.trim(),
            device = props["ro.product.device"] ?: "",
            board = props["ro.product.board"] ?: "",
            hardware = props["ro.hardware"] ?: "",
            serialNumber = props["ro.serialno"] ?: "",
            imeis = imeis,
            androidVersion = props["ro.build.version.release"] ?: "",
            sdkVersion = props["ro.build.version.sdk"] ?: "",
            fingerprint = props["ro.build.fingerprint"] ?: "",
            phone = props["gsm.version.baseband"] ?: ""
        )
    }

    fun parseCpuInfo(cpuOutput: String, socModel: String, socManufacturer: String, socChipname: String, socPlatform: String, socVendorModel: String, soc0Family: String = "", soc0Machine: String = "", chipsetInfo: ChipsetInfo? = null, cpuAbi: String = ""): CpuInfo {
        val lines = cpuOutput.lines()
        var processor = ""
        var hardware = ""
        var features = ""
        var bogoMips = ""
        var cpuArch = ""
        var processorCount = 0

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Processor", ignoreCase = true) && trimmed.startsWith("processor") -> {
                    processorCount++
                }
                trimmed.startsWith("Processor") && !trimmed.startsWith("processor") -> {
                    val value = trimmed.substringAfter(":").trim()
                    if (value.isNotBlank()) {
                        processor = value
                    }
                }
                trimmed.startsWith("CPU architecture") -> {
                    val archNum = trimmed.substringAfter(":").trim().replace(Regex("[^0-9]"), "")
                    cpuArch = when {
                        archNum.startsWith("8") -> "ARMv8 (64-bit)"
                        archNum.startsWith("7") -> "ARMv7 (32-bit)"
                        archNum.startsWith("6") -> "ARMv6"
                        archNum.startsWith("5") -> "ARMv5"
                        else -> ""
                    }
                }
                trimmed.startsWith("Hardware") -> hardware = trimmed.substringAfter(":").trim()
                trimmed.startsWith("Features") -> features = trimmed.substringAfter(":").trim()
                trimmed.startsWith("BogoMIPS") -> bogoMips = trimmed.substringAfter(":").trim()
            }
        }

        if (cpuArch.isBlank() && cpuAbi.isNotBlank()) {
            cpuArch = when {
                cpuAbi.contains("arm64", ignoreCase = true) -> "ARMv8 (64-bit)"
                cpuAbi.contains("arm", ignoreCase = true) -> "ARMv7 (32-bit)"
                cpuAbi.contains("x86_64", ignoreCase = true) -> "x86_64"
                cpuAbi.contains("x86", ignoreCase = true) -> "x86"
                else -> ""
            }
        }

        val socName = if (chipsetInfo != null && chipsetInfo.chipset.isNotBlank()) {
            if (chipsetInfo.brand.isNotBlank()) "${chipsetInfo.brand} ${chipsetInfo.chipset}" else chipsetInfo.chipset
        } else {
            resolveSocName(socModel, socManufacturer, socChipname, socPlatform, socVendorModel, soc0Family, soc0Machine)
        }
        val displayHardware = hardware.ifBlank { socName }

        return CpuInfo(
            socName = socName,
            socManufacturer = chipsetInfo?.brand?.takeIf { it.isNotBlank() } ?: socManufacturer.trim(),
            processor = processor,
            hardware = displayHardware,
            features = features,
            processorCount = processorCount,
            bogoMips = bogoMips,
            architecture = cpuArch,
            cpuAbi = cpuAbi,
            gpu = chipsetInfo?.gpu?.takeIf { it.isNotBlank() } ?: "",
            cpuConfig = chipsetInfo?.cpu?.takeIf { it.isNotBlank() } ?: ""
        )
    }

    fun parseMemoryInfo(output: String): MemoryInfo {
        val lines = output.lines()
        var memTotalKb = 0L
        var memFreeKb = 0L
        var memAvailableKb = 0L
        var cachedKb = 0L
        var buffersKb = 0L
        var swapTotalKb = 0L
        var swapFreeKb = 0L

        for (line in lines) {
            when {
                line.startsWith("MemTotal") -> memTotalKb = extractKbValue(line)
                line.startsWith("MemFree") -> memFreeKb = extractKbValue(line)
                line.startsWith("MemAvailable") -> memAvailableKb = extractKbValue(line)
                line.startsWith("Cached") && !line.startsWith("SwapCached") -> cachedKb = extractKbValue(line)
                line.startsWith("Buffers") -> buffersKb = extractKbValue(line)
                line.startsWith("SwapTotal") -> swapTotalKb = extractKbValue(line)
                line.startsWith("SwapFree") -> swapFreeKb = extractKbValue(line)
            }
        }

        val totalBytes = memTotalKb * 1024
        val freeBytes = memFreeKb * 1024
        val availableBytes = memAvailableKb * 1024
        val usagePercent = if (memTotalKb > 0) {
            ((memTotalKb - memAvailableKb).toFloat() / memTotalKb * 100).coerceIn(0f, 100f)
        } else 0f

        return MemoryInfo(
            totalRamBytes = totalBytes,
            freeRamBytes = freeBytes,
            availableRamBytes = availableBytes,
            cachedBytes = cachedKb * 1024,
            buffersBytes = buffersKb * 1024,
            totalSwapBytes = swapTotalKb * 1024,
            freeSwapBytes = swapFreeKb * 1024,
            totalRamFormatted = formatBytesFromBytes(totalBytes),
            freeRamFormatted = formatBytesFromBytes(freeBytes),
            availableRamFormatted = formatBytesFromBytes(availableBytes),
            cachedFormatted = formatBytesFromBytes(cachedKb * 1024),
            totalSwapFormatted = formatBytesFromBytes(swapTotalKb * 1024),
            freeSwapFormatted = formatBytesFromBytes(swapFreeKb * 1024),
            usagePercent = usagePercent
        )
    }

    fun parseBatteryInfo(output: String): BatteryInfo {
        val lines = output.lines()
        var level = ""
        var scale = ""
        var status = ""
        var health = ""
        var technology = ""
        var temperature = ""
        var voltage = ""
        var acPowered = false
        var usbPowered = false
        var wirelessPowered = false

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("level:") -> level = trimmed.substringAfter(":").trim()
                trimmed.startsWith("scale:") -> scale = trimmed.substringAfter(":").trim()
                trimmed.startsWith("status:") -> status = parseBatteryStatus(trimmed.substringAfter(":").trim().toIntOrNull() ?: 0)
                trimmed.startsWith("health:") -> health = parseBatteryHealth(trimmed.substringAfter(":").trim().toIntOrNull() ?: 0)
                trimmed.startsWith("technology:") -> technology = trimmed.substringAfter(":").trim()
                trimmed.startsWith("temperature:") -> temperature = trimmed.substringAfter(":").trim()
                trimmed.startsWith("voltage:") -> voltage = trimmed.substringAfter(":").trim()
                trimmed.startsWith("AC powered:") -> acPowered = trimmed.substringAfter(":").trim().equals("true", ignoreCase = true)
                trimmed.startsWith("USB powered:") -> usbPowered = trimmed.substringAfter(":").trim().equals("true", ignoreCase = true)
                trimmed.startsWith("Wireless powered:") -> wirelessPowered = trimmed.substringAfter(":").trim().equals("true", ignoreCase = true)
            }
        }

        val levelInt = level.toIntOrNull() ?: 0
        val scaleInt = scale.toIntOrNull() ?: 100
        val levelPercent = if (scaleInt > 0) (levelInt.toFloat() / scaleInt * 100).coerceIn(0f, 100f) else 0f

        val plugged = when {
            acPowered -> "AC"
            usbPowered -> "USB"
            wirelessPowered -> "Inalambrico"
            else -> "Sin carga"
        }

        val tempFormatted = temperature.toIntOrNull()?.let { String.format(Locale.US, "%.1f°C", it / 10.0) } ?: temperature
        val voltageFormatted = voltage.toIntOrNull()?.let { String.format(Locale.US, "%.2f V", it / 1000.0) } ?: voltage

        return BatteryInfo(
            level = level,
            scale = scale,
            status = status,
            health = health,
            technology = technology,
            temperature = tempFormatted,
            voltage = voltageFormatted,
            plugged = plugged,
            levelPercent = levelPercent
        )
    }

    fun parseDisplayInfo(sizeOutput: String, densityOutput: String, displayOutput: String, refreshRateAlt: String = "", sfLatency: String = ""): DisplayInfo {
        val resolution = sizeOutput.trim().removePrefix("Physical size: ")
        val density = densityOutput.trim().removePrefix("Physical density: ")
        val densityDpi = densityOutput.replace(Regex("[^0-9]"), "").trim()
        val displayLines = displayOutput.lines()
        var refreshRate = ""

        for (line in displayLines) {
            val trimmed = line.trim()
            when {
                trimmed.contains("renderFrameRate") && refreshRate.isBlank() -> {
                    val match = Regex("renderFrameRate\\s+(\\d+\\.?\\d*)").find(trimmed)
                    if (match != null) {
                        refreshRate = "${match.groupValues[1]} Hz"
                    }
                }
                trimmed.contains("peakRefreshRate") && refreshRate.isBlank() -> {
                    val match = Regex("peakRefreshRate[=]\\s*(\\d+\\.?\\d*)").find(trimmed)
                    if (match != null) {
                        refreshRate = "${match.groupValues[1]} Hz"
                    }
                }
                trimmed.matches(Regex(".*refresh.*rate.*\\d+.*")) && refreshRate.isBlank() -> {
                    val match = Regex("(\\d+\\.?\\d*)\\s*[Hh]z").find(trimmed)
                    if (match != null) {
                        refreshRate = "${match.groupValues[1]} Hz"
                    }
                }
            }
        }

        // Try SurfaceFlinger --latency: first line is refresh period in nanoseconds
        if (refreshRate.isBlank() && sfLatency.isNotBlank()) {
            val firstLine = sfLatency.lines().firstOrNull { it.trim().isNotEmpty() }?.trim()
            val periodNs = firstLine?.toLongOrNull()
            if (periodNs != null && periodNs > 0) {
                val hz = 1_000_000_000.0 / periodNs
                refreshRate = String.format(Locale.US, "%.0f Hz", hz)
            }
        }

        if (refreshRate.isBlank() && refreshRateAlt.isNotBlank()) {
            val altTrimmed = refreshRateAlt.trim()
            if (altTrimmed != "null" && altTrimmed.isNotBlank()) {
                refreshRate = "$altTrimmed Hz"
            }
        }

        return DisplayInfo(
            resolution = resolution,
            density = density,
            densityDpi = densityDpi,
            refreshRate = refreshRate,
            displayInfo = displayOutput.take(500)
        )
    }

    fun parseStorageInfo(output: String): StorageInfo {
        val lines = output.lines()
        val filesystems = mutableListOf<FileSystemInfo>()

        for (line in lines.drop(1)) {
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 6) {
                val sizeKb = parseSizeToKb(parts[1])
                val usedKb = parseSizeToKb(parts[2])
                val availKb = parseSizeToKb(parts[3])
                val sizeBytes = sizeKb * 1024
                val usedBytes = usedKb * 1024
                val availBytes = availKb * 1024
                val usagePercent = if (sizeKb > 0) {
                    (usedKb.toFloat() / sizeKb * 100).coerceIn(0f, 100f)
                } else 0f

                filesystems.add(
                    FileSystemInfo(
                        filesystem = parts[0],
                        sizeFormatted = formatBytesFromBytes(sizeBytes),
                        usedFormatted = formatBytesFromBytes(usedBytes),
                        availableFormatted = formatBytesFromBytes(availBytes),
                        mountPoint = parts[5],
                        sizeBytes = sizeBytes,
                        usedBytes = usedBytes,
                        availableBytes = availBytes,
                        usagePercent = usagePercent
                    )
                )
            }
        }

        return StorageInfo(filesystems = filesystems.sortedByDescending { it.mountPoint.contains("emulated") || it.mountPoint.contains("/storage/emulated") })
    }

    fun parseCameraInfo(output: String): List<CameraInfo> {
        val cameras = mutableMapOf<String, CameraInfo>()
        val lines = output.lines()
        var currentCameraId = ""
        var currentFacing = ""
        var currentFlash = ""
        var currentMaxWidth = 0
        var currentMaxHeight = 0
        var currentFocalLength = ""
        var expectingFocalValues = false

        fun flushCamera() {
            if (currentCameraId.isNotEmpty()) {
                val existing = cameras[currentCameraId]
                val newCam = buildCameraInfo(currentCameraId, currentFacing, currentFlash, currentMaxWidth, currentMaxHeight, currentFocalLength)
                if (existing == null ||
                    (newCam.resolution.isNotBlank() && existing.resolution.isBlank()) ||
                    (newCam.focalLength.isNotBlank() && existing.focalLength.isBlank())) {
                    cameras[currentCameraId] = newCam
                }
            }
        }

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.contains("static information") && Regex("Camera HAL device.*legacy/\\d+").containsMatchIn(trimmed) -> {
                    flushCamera()
                    val idMatch = Regex("legacy/(\\d+)").find(trimmed)
                    currentCameraId = idMatch?.groupValues?.get(1) ?: ""
                    currentFacing = ""
                    currentFlash = ""
                    currentMaxWidth = 0
                    currentMaxHeight = 0
                    currentFocalLength = ""
                    expectingFocalValues = false
                }
                trimmed.startsWith("Facing:") -> {
                    currentFacing = when (trimmed.substringAfter("Facing:").trim().lowercase()) {
                        "back" -> "Trasera"
                        "front" -> "Frontal"
                        else -> trimmed.substringAfter("Facing:").trim()
                    }
                }
                trimmed.startsWith("Has a flash unit:") -> {
                    currentFlash = if (trimmed.contains("true")) "Si" else "No"
                }
                trimmed.matches(Regex("\\[\\d+\\s+\\d+\\s+\\d+\\s+OUTPUT.*")) && currentCameraId.isNotEmpty() -> {
                    val sizeMatch = Regex("\\[\\d+\\s+(\\d+)\\s+(\\d+)\\s+OUTPUT").find(trimmed)
                    if (sizeMatch != null) {
                        val w = sizeMatch.groupValues[1].toIntOrNull() ?: 0
                        val h = sizeMatch.groupValues[2].toIntOrNull() ?: 0
                        if (w > currentMaxWidth) {
                            currentMaxWidth = w
                            currentMaxHeight = h
                        }
                    }
                }
                trimmed.contains("availableFocalLengths") -> {
                    expectingFocalValues = true
                }
                trimmed.matches(Regex("\\[\\d+\\.\\d+.*\\]")) && expectingFocalValues && currentCameraId.isNotEmpty() -> {
                    val flMatch = Regex("\\[(\\d+\\.\\d+)").find(trimmed)
                    if (flMatch != null) {
                        currentFocalLength = "${flMatch.groupValues[1]} mm"
                    }
                    expectingFocalValues = false
                }
            }
        }
        flushCamera()

        val result = cameras.values.sortedBy { it.id.toIntOrNull() ?: 0 }.toMutableList()

        if (result.isEmpty()) {
            val countMatch = Regex("Number of camera devices:\\s*(\\d+)").find(output)
            val count = countMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            for (i in 0 until count) {
                result.add(
                    CameraInfo(
                        id = i.toString(),
                        facing = if (i % 2 == 0) "Trasera" else "Frontal"
                    )
                )
            }
        }

        return result
    }

    private fun buildCameraInfo(id: String, facing: String, flash: String, maxWidth: Int, maxHeight: Int, focalLength: String): CameraInfo {
        val resolution = if (maxWidth > 0 && maxHeight > 0) "${maxWidth}x${maxHeight}" else ""
        val mp = if (resolution.isNotBlank()) {
            String.format(Locale.US, "%.1f MP", (maxWidth.toLong() * maxHeight) / 1_000_000.0)
        } else ""
        return CameraInfo(
            id = id,
            facing = facing.ifBlank { "Desconocido" },
            megapixels = mp,
            resolution = resolution,
            flash = flash,
            focalLength = focalLength
        )
    }

    fun parseSensorInfo(output: String): List<SensorInfo> {
        val sensors = mutableListOf<SensorInfo>()
        val lines = output.lines()

        for (line in lines) {
            val trimmed = line.trim()
            val match = Regex("^0x[0-9a-f]+\\)\\s+(.+?)\\s+\\|\\s+(\\S+)\\s+\\|\\s+ver:\\s*(\\d+)\\s+\\|\\s+type:\\s+(.+?)\\s*\\|").find(trimmed)
            if (match != null) {
                val name = match.groupValues[1].trim()
                val vendor = match.groupValues[2].trim()
                val version = match.groupValues[3].trim()
                val type = match.groupValues[4].trim()

                if (name.isNotBlank()) {
                    sensors.add(
                        SensorInfo(
                            name = name,
                            vendor = vendor,
                            version = version,
                            type = type,
                            maxRange = "",
                            resolution = "",
                            power = ""
                        )
                    )
                }
            }
        }

        return sensors
    }

    fun parseNetworkInfo(output: String, wifiInterface: String): NetworkInfo {
        val interfaces = mutableListOf<NetworkInterface>()
        val lines = output.lines()
        var currentName = ""
        var currentFlags = ""
        var currentMac = ""

        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.matches(Regex("^\\d+:.*")) -> {
                    val colonIdx = trimmed.indexOf(':')
                    if (colonIdx >= 0) {
                        val afterNum = trimmed.substring(colonIdx + 1).trim()
                        currentName = afterNum.substringBefore(":").trim()
                        currentFlags = ""
                        currentMac = ""
                    }
                }
                trimmed.contains("link/ether") -> {
                    val macMatch = Regex("link/ether\\s+([0-9a-f:]{17})").find(trimmed)
                    currentMac = macMatch?.groupValues?.get(1)?.uppercase(Locale.US) ?: ""
                }
                trimmed.contains("flags=") -> {
                    currentFlags = trimmed.substringAfter("flags=").substringBefore("<").trim()
                }
                trimmed.contains("inet ") -> {
                    val ip = trimmed.substringAfter("inet ").substringBefore("/").substringBefore(" ").trim()
                    if (ip.isNotEmpty()) {
                        interfaces.add(
                            NetworkInterface(
                                name = currentName,
                                ipAddress = ip,
                                macAddress = currentMac,
                                flags = currentFlags
                            )
                        )
                    }
                }
            }
        }

        return NetworkInfo(
            interfaces = interfaces,
            wifiInterface = wifiInterface.trim()
        )
    }

    fun parseBuildInfo(output: String, hardware: String): BuildInfo {
        val props = parseGetProp(output)
        return BuildInfo(
            board = props["ro.product.board"] ?: "",
            bootloader = props["ro.bootloader"] ?: "",
            brand = props["ro.product.brand"] ?: "",
            device = props["ro.product.device"] ?: "",
            display = props["ro.build.display.id"] ?: "",
            fingerprint = props["ro.build.fingerprint"] ?: "",
            host = props["ro.build.host"] ?: "",
            id = props["ro.build.id"] ?: "",
            manufacturer = props["ro.product.manufacturer"] ?: "",
            model = props["ro.product.model"] ?: "",
            product = props["ro.product.name"] ?: "",
            tags = props["ro.build.tags"] ?: "",
            type = props["ro.build.type"] ?: "",
            baseband = props["gsm.version.baseband"] ?: "",
            kernel = hardware
        )
    }

    fun parseImei(output: String): String {
        val sb = StringBuilder()
        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.contains("'")) {
                val parts = trimmed.split("'")
                if (parts.size >= 2) {
                    val asciiPart = parts[1]
                    val digits = asciiPart.filter { it.isDigit() }
                    if (digits.isNotEmpty()) sb.append(digits)
                }
            }
        }
        val result = sb.toString()
        return if (isValidImei(result)) result else ""
    }

    fun parseImeiV2(output: String): String {
        for (line in output.lines()) {
            val trimmed = line.trim()
            val match = Regex("""IMEI:\s*\[?(\d{15})\]?""").find(trimmed)
                ?: Regex("""\b(\d{15})\b""").find(trimmed)
            if (match != null) {
                val imei = match.groupValues[1]
                if (isValidImei(imei)) return imei
            }
        }
        return ""
    }

    fun isValidImeiPublic(s: String): Boolean = isValidImei(s)

    private fun isValidImei(s: String): Boolean {
        if (s.length !in 14..17) return false
        if (s.any { !it.isDigit() }) return false
        if (s.all { it == s[0] }) return false
        var sum = 0
        var alt = false
        for (i in s.length - 1 downTo 0) {
            var d = s[i].digitToInt()
            if (alt) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            alt = !alt
        }
        return sum % 10 == 0
    }

    private fun resolveSocName(
        socModel: String,
        socManufacturer: String,
        socChipname: String,
        socPlatform: String,
        socVendorModel: String,
        soc0Family: String = "",
        soc0Machine: String = ""
    ): String {
        val model = socModel.trim().ifEmpty { socVendorModel.trim() }
        val manufacturer = resolveSocManufacturer(socManufacturer.trim())
        val chipname = socChipname.trim()
        val platform = socPlatform.trim().lowercase(Locale.US)
        val family = soc0Family.trim()
        val machine = soc0Machine.trim().lowercase(Locale.US)

        // Try database lookup with all available identifiers
        // Priority: platform -> socModel (SM8350) -> machine -> chipname
        // socModel is often the best match (e.g. "SM8350" -> "Snapdragon 888")
        val commercialName = lookupSocCommercialName(platform)
            ?: lookupSocCommercialName(model.lowercase(Locale.US))
            ?: lookupSocCommercialName(machine)
            ?: lookupSocCommercialName(chipname)

        if (commercialName != null) {
            val vendor = when {
                manufacturer.isNotBlank() -> manufacturer
                family.isNotBlank() -> resolveSocManufacturer(family)
                else -> when {
                    commercialName.startsWith("Snapdragon") -> "Qualcomm"
                    commercialName.startsWith("Helio") || commercialName.startsWith("Dimensity") -> "MediaTek"
                    commercialName.startsWith("Exynos") -> "Samsung"
                    commercialName.startsWith("Kirin") -> "HiSilicon"
                    commercialName.startsWith("Unisoc") -> "Unisoc"
                    else -> ""
                }
            }
            return if (vendor.isNotBlank()) "$vendor $commercialName" else commercialName
        }

        // Fallback to original logic
        return when {
            model.isNotBlank() && manufacturer.isNotBlank() -> "$manufacturer $model"
            model.isNotBlank() -> model
            chipname.isNotBlank() && manufacturer.isNotBlank() -> "$manufacturer $chipname"
            chipname.isNotBlank() -> chipname
            platform.isNotBlank() && manufacturer.isNotBlank() -> "$manufacturer ${socPlatform.trim()}"
            platform.isNotBlank() -> socPlatform.trim()
            else -> ""
        }
    }

    private fun resolveSocManufacturer(raw: String): String {
        return when (raw.uppercase(Locale.US)) {
            "QTI", "QUALCOMM" -> "Qualcomm"
            "SAMSUNG" -> "Samsung"
            "MTK", "MEDIATEK" -> "MediaTek"
            "HISILICON", "HI-SILICON" -> "HiSilicon"
            "UNISOC", "SPREADTRUM" -> "Unisoc"
            "SNAPDRAGON" -> "Qualcomm"
            "HELIO", "DIMENSITY" -> "MediaTek"
            "EXYNOS" -> "Samsung"
            "KIRIN" -> "HiSilicon"
            else -> raw
        }
    }

    private fun parseGetProp(output: String): Map<String, String> {
        val props = mutableMapOf<String, String>()
        for (line in output.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("[") && trimmed.contains("]:")) {
                val key = trimmed.substringAfter("[").substringBefore("]:").trim()
                val value = trimmed.substringAfter("]:").trim().removeSurrounding("[", "]")
                if (key.isNotEmpty() && !props.containsKey(key)) {
                    props[key] = value
                }
            }
        }
        return props
    }

    private fun extractKbValue(line: String): Long {
        val value = line.substringAfter(":").trim().replace(Regex("[^0-9]"), "")
        return value.toLongOrNull() ?: 0L
    }

    private fun parseSizeToKb(sizeStr: String): Long {
        val trimmed = sizeStr.trim()
        return when {
            trimmed.endsWith("G") || trimmed.endsWith("GB") -> {
                val num = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                (num * 1024 * 1024).toLong()
            }
            trimmed.endsWith("M") || trimmed.endsWith("MB") -> {
                val num = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                (num * 1024).toLong()
            }
            trimmed.endsWith("K") || trimmed.endsWith("KB") -> {
                trimmed.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            }
            trimmed.endsWith("T") || trimmed.endsWith("TB") -> {
                val num = trimmed.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                (num * 1024 * 1024 * 1024).toLong()
            }
            else -> trimmed.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
        }
    }

    private fun parseBatteryStatus(status: Int): String = when (status) {
        1 -> "Desconocido"
        2 -> "Cargando"
        3 -> "Descargando"
        4 -> "Sin carga"
        5 -> "Completo"
        else -> "Desconocido"
    }

    private fun parseBatteryHealth(health: Int): String = when (health) {
        1 -> "Desconocido"
        2 -> "Buena"
        3 -> "Sobrecalentada"
        4 -> "Muerta"
        5 -> "Sobrevoltaje"
        6 -> "Fallo"
        7 -> "Fria"
        else -> "Desconocido"
    }
}
