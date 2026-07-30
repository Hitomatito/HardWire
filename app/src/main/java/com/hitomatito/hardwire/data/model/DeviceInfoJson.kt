package com.hitomatito.hardwire.data.model

import org.json.JSONArray
import org.json.JSONObject

object DeviceInfoJson {

    fun toJson(info: DeviceInfo): JSONObject {
        return JSONObject().apply {
            put("general", generalToJson(info.general))
            put("cpu", cpuToJson(info.cpu))
            put("memory", memoryToJson(info.memory))
            put("battery", batteryToJson(info.battery))
            put("display", displayToJson(info.display))
            put("storage", storageToJson(info.storage))
            put("cameras", listToJson(info.cameras) { cameraToJson(it) })
            put("sensors", listToJson(info.sensors) { sensorToJson(it) })
            put("network", networkToJson(info.network))
            put("build", buildToJson(info.build))
        }
    }

    fun fromJson(obj: JSONObject?): DeviceInfo {
        if (obj == null) return DeviceInfo()
        return DeviceInfo(
            general = generalFromJson(obj.optJSONObject("general")),
            cpu = cpuFromJson(obj.optJSONObject("cpu")),
            memory = memoryFromJson(obj.optJSONObject("memory")),
            battery = batteryFromJson(obj.optJSONObject("battery")),
            display = displayFromJson(obj.optJSONObject("display")),
            storage = storageFromJson(obj.optJSONObject("storage")),
            cameras = jsonToList(obj.optJSONArray("cameras")) { cameraFromJson(it) },
            sensors = jsonToList(obj.optJSONArray("sensors")) { sensorFromJson(it) },
            network = networkFromJson(obj.optJSONObject("network")),
            build = buildFromJson(obj.optJSONObject("build"))
        )
    }

    private fun generalToJson(g: GeneralInfo) = JSONObject().apply {
        put("manufacturer", g.manufacturer)
        put("model", g.model)
        put("marketName", g.marketName)
        put("device", g.device)
        put("board", g.board)
        put("hardware", g.hardware)
        put("serialNumber", g.serialNumber)
        put("imeis", JSONArray(g.imeis))
        put("androidVersion", g.androidVersion)
        put("sdkVersion", g.sdkVersion)
        put("fingerprint", g.fingerprint)
        put("phone", g.phone)
    }

    private fun generalFromJson(o: JSONObject?): GeneralInfo {
        if (o == null) return GeneralInfo()
        return GeneralInfo(
            manufacturer = o.optString("manufacturer", ""),
            model = o.optString("model", ""),
            marketName = o.optString("marketName", ""),
            device = o.optString("device", ""),
            board = o.optString("board", ""),
            hardware = o.optString("hardware", ""),
            serialNumber = o.optString("serialNumber", ""),
            imeis = jsonArrayToStrings(o.optJSONArray("imeis")),
            androidVersion = o.optString("androidVersion", ""),
            sdkVersion = o.optString("sdkVersion", ""),
            fingerprint = o.optString("fingerprint", ""),
            phone = o.optString("phone", "")
        )
    }

    private fun cpuToJson(c: CpuInfo) = JSONObject().apply {
        put("socName", c.socName)
        put("socManufacturer", c.socManufacturer)
        put("processor", c.processor)
        put("hardware", c.hardware)
        put("features", c.features)
        put("processorCount", c.processorCount)
        put("bogoMips", c.bogoMips)
        put("architecture", c.architecture)
        put("cpuAbi", c.cpuAbi)
        put("gpu", c.gpu)
        put("cpuConfig", c.cpuConfig)
    }

    private fun cpuFromJson(o: JSONObject?): CpuInfo {
        if (o == null) return CpuInfo()
        return CpuInfo(
            socName = o.optString("socName", ""),
            socManufacturer = o.optString("socManufacturer", ""),
            processor = o.optString("processor", ""),
            hardware = o.optString("hardware", ""),
            features = o.optString("features", ""),
            processorCount = o.optInt("processorCount", 0),
            bogoMips = o.optString("bogoMips", ""),
            architecture = o.optString("architecture", ""),
            cpuAbi = o.optString("cpuAbi", ""),
            gpu = o.optString("gpu", ""),
            cpuConfig = o.optString("cpuConfig", "")
        )
    }

    private fun memoryToJson(m: MemoryInfo) = JSONObject().apply {
        put("totalRamBytes", m.totalRamBytes)
        put("freeRamBytes", m.freeRamBytes)
        put("availableRamBytes", m.availableRamBytes)
        put("cachedBytes", m.cachedBytes)
        put("buffersBytes", m.buffersBytes)
        put("totalSwapBytes", m.totalSwapBytes)
        put("freeSwapBytes", m.freeSwapBytes)
        put("totalRamFormatted", m.totalRamFormatted)
        put("freeRamFormatted", m.freeRamFormatted)
        put("availableRamFormatted", m.availableRamFormatted)
        put("cachedFormatted", m.cachedFormatted)
        put("totalSwapFormatted", m.totalSwapFormatted)
        put("freeSwapFormatted", m.freeSwapFormatted)
        put("usagePercent", m.usagePercent.toDouble())
    }

    private fun memoryFromJson(o: JSONObject?): MemoryInfo {
        if (o == null) return MemoryInfo()
        return MemoryInfo(
            totalRamBytes = o.optLong("totalRamBytes", 0L),
            freeRamBytes = o.optLong("freeRamBytes", 0L),
            availableRamBytes = o.optLong("availableRamBytes", 0L),
            cachedBytes = o.optLong("cachedBytes", 0L),
            buffersBytes = o.optLong("buffersBytes", 0L),
            totalSwapBytes = o.optLong("totalSwapBytes", 0L),
            freeSwapBytes = o.optLong("freeSwapBytes", 0L),
            totalRamFormatted = o.optString("totalRamFormatted", ""),
            freeRamFormatted = o.optString("freeRamFormatted", ""),
            availableRamFormatted = o.optString("availableRamFormatted", ""),
            cachedFormatted = o.optString("cachedFormatted", ""),
            totalSwapFormatted = o.optString("totalSwapFormatted", ""),
            freeSwapFormatted = o.optString("freeSwapFormatted", ""),
            usagePercent = o.optDouble("usagePercent", 0.0).toFloat()
        )
    }

    private fun batteryToJson(b: BatteryInfo) = JSONObject().apply {
        put("level", b.level)
        put("scale", b.scale)
        put("status", b.status)
        put("health", b.health)
        put("technology", b.technology)
        put("temperature", b.temperature)
        put("voltage", b.voltage)
        put("plugged", b.plugged)
        put("levelPercent", b.levelPercent.toDouble())
    }

    private fun batteryFromJson(o: JSONObject?): BatteryInfo {
        if (o == null) return BatteryInfo()
        return BatteryInfo(
            level = o.optString("level", ""),
            scale = o.optString("scale", ""),
            status = o.optString("status", ""),
            health = o.optString("health", ""),
            technology = o.optString("technology", ""),
            temperature = o.optString("temperature", ""),
            voltage = o.optString("voltage", ""),
            plugged = o.optString("plugged", ""),
            levelPercent = o.optDouble("levelPercent", 0.0).toFloat()
        )
    }

    private fun displayToJson(d: DisplayInfo) = JSONObject().apply {
        put("resolution", d.resolution)
        put("density", d.density)
        put("densityDpi", d.densityDpi)
        put("refreshRate", d.refreshRate)
        put("displayInfo", d.displayInfo)
    }

    private fun displayFromJson(o: JSONObject?): DisplayInfo {
        if (o == null) return DisplayInfo()
        return DisplayInfo(
            resolution = o.optString("resolution", ""),
            density = o.optString("density", ""),
            densityDpi = o.optString("densityDpi", ""),
            refreshRate = o.optString("refreshRate", ""),
            displayInfo = o.optString("displayInfo", "")
        )
    }

    private fun storageToJson(s: StorageInfo) = JSONObject().apply {
        put("filesystems", listToJson(s.filesystems) { fsToJson(it) })
    }

    private fun storageFromJson(o: JSONObject?): StorageInfo {
        if (o == null) return StorageInfo()
        return StorageInfo(
            filesystems = jsonToList(o.optJSONArray("filesystems")) { fsFromJson(it) }
        )
    }

    private fun fsToJson(f: FileSystemInfo) = JSONObject().apply {
        put("filesystem", f.filesystem)
        put("sizeFormatted", f.sizeFormatted)
        put("usedFormatted", f.usedFormatted)
        put("availableFormatted", f.availableFormatted)
        put("mountPoint", f.mountPoint)
        put("sizeBytes", f.sizeBytes)
        put("usedBytes", f.usedBytes)
        put("availableBytes", f.availableBytes)
        put("usagePercent", f.usagePercent.toDouble())
    }

    private fun fsFromJson(o: JSONObject?): FileSystemInfo {
        if (o == null) return FileSystemInfo()
        return FileSystemInfo(
            filesystem = o.optString("filesystem", ""),
            sizeFormatted = o.optString("sizeFormatted", ""),
            usedFormatted = o.optString("usedFormatted", ""),
            availableFormatted = o.optString("availableFormatted", ""),
            mountPoint = o.optString("mountPoint", ""),
            sizeBytes = o.optLong("sizeBytes", 0L),
            usedBytes = o.optLong("usedBytes", 0L),
            availableBytes = o.optLong("availableBytes", 0L),
            usagePercent = o.optDouble("usagePercent", 0.0).toFloat()
        )
    }

    private fun cameraToJson(c: CameraInfo) = JSONObject().apply {
        put("id", c.id)
        put("facing", c.facing)
        put("megapixels", c.megapixels)
        put("resolution", c.resolution)
        put("flash", c.flash)
        put("focalLength", c.focalLength)
    }

    private fun cameraFromJson(o: JSONObject?): CameraInfo {
        if (o == null) return CameraInfo()
        return CameraInfo(
            id = o.optString("id", ""),
            facing = o.optString("facing", ""),
            megapixels = o.optString("megapixels", ""),
            resolution = o.optString("resolution", ""),
            flash = o.optString("flash", ""),
            focalLength = o.optString("focalLength", "")
        )
    }

    private fun sensorToJson(s: SensorInfo) = JSONObject().apply {
        put("name", s.name)
        put("type", s.type)
        put("vendor", s.vendor)
        put("version", s.version)
        put("maxRange", s.maxRange)
        put("resolution", s.resolution)
        put("power", s.power)
    }

    private fun sensorFromJson(o: JSONObject?): SensorInfo {
        if (o == null) return SensorInfo()
        return SensorInfo(
            name = o.optString("name", ""),
            type = o.optString("type", ""),
            vendor = o.optString("vendor", ""),
            version = o.optString("version", ""),
            maxRange = o.optString("maxRange", ""),
            resolution = o.optString("resolution", ""),
            power = o.optString("power", "")
        )
    }

    private fun networkToJson(n: NetworkInfo) = JSONObject().apply {
        put("wifiInterface", n.wifiInterface)
        put("interfaces", listToJson(n.interfaces) { ifaceToJson(it) })
    }

    private fun networkFromJson(o: JSONObject?): NetworkInfo {
        if (o == null) return NetworkInfo()
        return NetworkInfo(
            wifiInterface = o.optString("wifiInterface", ""),
            interfaces = jsonToList(o.optJSONArray("interfaces")) { ifaceFromJson(it) }
        )
    }

    private fun ifaceToJson(i: NetworkInterface) = JSONObject().apply {
        put("name", i.name)
        put("ipAddress", i.ipAddress)
        put("macAddress", i.macAddress)
        put("flags", i.flags)
    }

    private fun ifaceFromJson(o: JSONObject?): NetworkInterface {
        if (o == null) return NetworkInterface()
        return NetworkInterface(
            name = o.optString("name", ""),
            ipAddress = o.optString("ipAddress", ""),
            macAddress = o.optString("macAddress", ""),
            flags = o.optString("flags", "")
        )
    }

    private fun buildToJson(b: BuildInfo) = JSONObject().apply {
        put("board", b.board)
        put("bootloader", b.bootloader)
        put("brand", b.brand)
        put("device", b.device)
        put("display", b.display)
        put("fingerprint", b.fingerprint)
        put("host", b.host)
        put("id", b.id)
        put("manufacturer", b.manufacturer)
        put("model", b.model)
        put("product", b.product)
        put("tags", b.tags)
        put("type", b.type)
        put("baseband", b.baseband)
        put("kernel", b.kernel)
    }

    private fun buildFromJson(o: JSONObject?): BuildInfo {
        if (o == null) return BuildInfo()
        return BuildInfo(
            board = o.optString("board", ""),
            bootloader = o.optString("bootloader", ""),
            brand = o.optString("brand", ""),
            device = o.optString("device", ""),
            display = o.optString("display", ""),
            fingerprint = o.optString("fingerprint", ""),
            host = o.optString("host", ""),
            id = o.optString("id", ""),
            manufacturer = o.optString("manufacturer", ""),
            model = o.optString("model", ""),
            product = o.optString("product", ""),
            tags = o.optString("tags", ""),
            type = o.optString("type", ""),
            baseband = o.optString("baseband", ""),
            kernel = o.optString("kernel", "")
        )
    }

    private fun <T> listToJson(list: List<T>, block: (T) -> JSONObject): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(block(it)) }
        return arr
    }

    private fun <T> jsonToList(arr: JSONArray?, block: (JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        val result = mutableListOf<T>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            result.add(block(obj))
        }
        return result
    }

    private fun jsonArrayToStrings(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val result = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            result.add(arr.optString(i, ""))
        }
        return result
    }
}
