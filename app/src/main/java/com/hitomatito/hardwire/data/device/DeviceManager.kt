package com.hitomatito.hardwire.data.device

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.hitomatito.hardwire.adblib.AdbConnection
import com.hitomatito.hardwire.data.command.CommandParser
import com.hitomatito.hardwire.data.command.DeviceCommand
import com.hitomatito.hardwire.data.model.BatteryInfo
import com.hitomatito.hardwire.data.model.BuildInfo
import com.hitomatito.hardwire.data.model.ConnectionState
import com.hitomatito.hardwire.data.model.CpuInfo
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.data.model.DeviceInfoJson
import com.hitomatito.hardwire.data.model.DeviceType
import com.hitomatito.hardwire.data.model.DisplayInfo
import com.hitomatito.hardwire.data.model.GeneralInfo
import com.hitomatito.hardwire.data.model.ManagedDevice
import com.hitomatito.hardwire.data.model.MemoryInfo
import com.hitomatito.hardwire.data.model.NetworkInfo
import com.hitomatito.hardwire.data.model.StorageInfo
import com.hitomatito.hardwire.data.chipset.ChipsetRepository
import com.hitomatito.hardwire.data.history.HistoryRepository
import com.hitomatito.hardwire.data.network.NetworkScanner
import com.hitomatito.hardwire.data.usb.UsbAdbManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

import java.util.concurrent.ConcurrentHashMap

sealed class AddDeviceResult {
    data class Added(val device: ManagedDevice) : AddDeviceResult()
    data class AlreadyExists(val device: ManagedDevice) : AddDeviceResult()
}

class DeviceManager(private val context: Context, private val chipsetRepository: ChipsetRepository? = null, private val historyRepository: HistoryRepository? = null) {

    private val usbManager = UsbAdbManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val scopeJob = scope.coroutineContext[kotlinx.coroutines.Job]!!

    private val _devices = MutableStateFlow(loadDevices())
    val devices: StateFlow<List<ManagedDevice>> = _devices.asStateFlow()

    private val connections = ConcurrentHashMap<String, AdbConnection>()
    private val _states = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())
    val states: StateFlow<Map<String, ConnectionState>> = _states.asStateFlow()

    private val _saved = loadAllSaved()
    private val _infos = MutableStateFlow(_saved.first)
    val infos: StateFlow<Map<String, DeviceInfo?>> = _infos.asStateFlow()

    private val _updatedAt = MutableStateFlow(_saved.second)
    val updatedAt: StateFlow<Map<String, Long>> = _updatedAt.asStateFlow()

    private val _online = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val onlineStatus: StateFlow<Map<String, Boolean>> = _online.asStateFlow()

    private val _modes = MutableStateFlow<Map<String, String>>(emptyMap())
    val modes: StateFlow<Map<String, String>> = _modes.asStateFlow()

    private val _ips = MutableStateFlow<Map<String, String>>(emptyMap())
    val ips: StateFlow<Map<String, String>> = _ips.asStateFlow()

    private val _activeId = MutableStateFlow(_devices.value.firstOrNull()?.id)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _scanResults = MutableStateFlow<List<String>>(emptyList())
    val scanResults: StateFlow<List<String>> = _scanResults.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        Log.d("HW:DevMgr", "[init] iniciando, dispositivos cargados=${_devices.value.size}: ${_devices.value.map { it.id }}")
        usbManager.onPermissionGranted = { scope.launch { connectDevice(USB_ID) } }
        usbManager.onPermissionDenied = {
            Log.w("HW:DevMgr", "[init] permiso USB denegado por broadcast, verificando estado real...")
            scope.launch {
                delay(2000)
                val device = usbManager.findAdbDevice()
                if (device != null && usbManager.hasPermission(device)) {
                    Log.d("HW:DevMgr", "[init] permiso REALMENTE concedido (MIUI bug), conectando")
                    usbManager.setPendingDevice(device)
                    connectDevice(USB_ID)
                } else {
                    val msg = "Permiso USB denegado. Desconecta y reconecta el USB, luego toca 'Conectar USB'. Si el dialogo no aparece, revisa la configuracion USB del dispositivo."
                    setState(USB_ID, ConnectionState.Error(msg))
                }
            }
        }
        usbManager.registerReceiver()
        Log.d("HW:DevMgr", "[init] receptor USB registrado, callbacks configurados")

        _devices.value.forEach { device ->
            if (device.type == DeviceType.NETWORK) {
                Log.d("HW:DevMgr", "[init] auto-conectando red: ${device.id}")
                scope.launch { connectDevice(device.id) }
            }
        }
        if (_devices.value.isNotEmpty()) {
            _activeId.value = _devices.value.first().id
            Log.d("HW:DevMgr", "[init] activo inicial: ${_activeId.value}")
        }

        scope.launch { refreshOnlineStatus() }
    }

    fun dispose() {
        usbManager.unregisterReceiver()
        connections.values.forEach { try { it.close() } catch (_: Exception) {} }
        connections.clear()
        scopeJob.cancel()
    }

    private fun setState(id: String, state: ConnectionState) {
        val current = _states.value.toMutableMap()
        current[id] = state
        _states.value = current
    }

    private fun setInfo(id: String, info: DeviceInfo?) {
        val current = _infos.value.toMutableMap()
        current[id] = info
        _infos.value = current
    }

    fun getMode(id: String): String = _modes.value[id] ?: (devices.value.find { it.id == id }?.let {
        if (it.type == DeviceType.USB) "USB" else "Red"
    } ?: "")

    fun getIp(id: String): String = _ips.value[id] ?: (devices.value.find { it.id == id }?.host ?: "")

    private fun setMode(id: String, mode: String) {
        _modes.value = _modes.value.toMutableMap().apply { put(id, mode) }
    }

    private fun setIp(id: String, ip: String) {
        _ips.value = _ips.value.toMutableMap().apply { put(id, ip) }
    }

    fun findUsbDevice(): UsbDevice? {
        val d = usbManager.findAdbDevice()
        Log.d("HW:DevMgr", "[findUsbDevice] resultado=$d")
        return d
    }

    fun hasUsbPermission(): Boolean {
        val d = usbManager.findAdbDevice()
        val has = d?.let { usbManager.hasPermission(it) } ?: false
        Log.d("HW:DevMgr", "[hasUsbPermission] device=$d -> $has")
        return has
    }

    fun requestUsbPermission() {
        val device = usbManager.findAdbDevice() ?: run {
            Log.w("HW:DevMgr", "[requestUsbPermission] NO HAY dispositivo USB conectado")
            setState(USB_ID, ConnectionState.Error("No se detecto ningun dispositivo USB"))
            return
        }
        Log.d("HW:DevMgr", "[requestUsbPermission] dispositivo USB encontrado: ${device.deviceName}")

        if (usbManager.hasPermission(device)) {
            Log.d("HW:DevMgr", "[requestUsbPermission] permiso ya concedido, conectando")
            if (devices.value.none { it.id == USB_ID }) addUsbDevice(device)
            usbManager.setPendingDevice(device)
            scope.launch { connectDevice(USB_ID) }
            return
        }

        if (devices.value.none { it.id == USB_ID }) {
            addUsbDevice(device)
        }
        usbManager.requestPermission(device)
    }

    fun retryUsbPermission() {
        Log.d("HW:DevMgr", "[retryUsbPermission] reintentando permiso USB")
        requestUsbPermission()
    }

    private fun addUsbDevice(device: UsbDevice): ManagedDevice {
        val existing = devices.value.find { it.id == USB_ID }
        if (existing != null) return existing
        val managed = ManagedDevice(
            id = USB_ID,
            name = "Dispositivo USB",
            host = "",
            port = 0,
            type = DeviceType.USB
        )
        _devices.value = _devices.value + managed
        saveDevices(_devices.value)
        return managed
    }

    fun addNetworkDevice(host: String, port: Int = 5555): AddDeviceResult {
        val cleanHost = host.trim()
        val id = "$cleanHost:$port"
        Log.d("HW:DevMgr", "[addNetworkDevice] host=$cleanHost id=$id")
        val existing = devices.value.find { it.id == id }
        if (existing != null) {
            Log.d("HW:DevMgr", "[addNetworkDevice] ya existe (registrado), seleccionando")
            setActive(id)
            return AddDeviceResult.AlreadyExists(existing)
        }
        val managed = ManagedDevice(
            id = id,
            name = cleanHost,
            host = cleanHost,
            port = port,
            type = DeviceType.NETWORK
        )
        _devices.value = _devices.value + managed
        saveDevices(_devices.value)
        Log.d("HW:DevMgr", "[addNetworkDevice] nuevo dispositivo agregado: $id")
        scope.launch { connectDevice(id) }
        setActive(id)
        return AddDeviceResult.Added(managed)
    }

    fun setActive(id: String?) {
        Log.d("HW:DevMgr", "[setActive] id=$id")
        _activeId.value = id
    }

    fun selectDevice(id: String) {
        val device = devices.value.find { it.id == id } ?: return
        Log.d("HW:DevMgr", "[selectDevice] id=$id type=${device.type}")
        setActive(id)
        if (device.type == DeviceType.USB) {
            Log.d("HW:DevMgr", "[selectDevice] es USB -> requestUsbPermission")
            requestUsbPermission()
        } else {
            val state = _states.value[id]
            Log.d("HW:DevMgr", "[selectDevice] es RED, estado actual=$state")
            if (state == null || state is ConnectionState.Disconnected || state is ConnectionState.Error) {
                scope.launch { connectDevice(id) }
            }
        }
    }

    suspend fun connectDevice(id: String) {
        val device = devices.value.find { it.id == id } ?: return
        // Guard: skip if already connected and healthy
        val existing = connections[id]
        if (existing != null && existing.isHealthy) {
            Log.d("HW:DevMgr", "[connectDevice] SKIP id=$id ya conectado y saludable")
            return
        }
        // Close stale connection before creating a new one
        if (existing != null) {
            Log.d("HW:DevMgr", "[connectDevice] cerrando conexion anterior id=$id")
            try { existing.close() } catch (_: Exception) {}
            connections.remove(id)
        }
        Log.d("HW:DevMgr", "[connectDevice] ENTRADA id=$id type=${device.type} host=${device.host}:${device.port}")
        setState(id, ConnectionState.Connecting)
        try {
            val conn = if (device.type == DeviceType.USB) {
                Log.d("HW:DevMgr", "[connectDevice] ruta USB -> usbManager.connect()")
                usbManager.connect()
            } else {
                Log.d("HW:DevMgr", "[connectDevice] ruta RED -> openTcpConnection(${device.host}:${device.port})")
                usbManager.openTcpConnection(device.host, device.port)
            }
            connections[id] = conn
            setMode(id, if (device.type == DeviceType.USB) "USB" else "Red")
            setIp(id, if (device.type == DeviceType.USB) "" else device.host)
            Log.d("HW:DevMgr", "[connectDevice] ADB conectado, verificando salud...")

            if (!conn.isHealthy) {
                Log.e("HW:DevMgr", "[connectDevice] conexion NO saludable post-connect, abortando")
                connections.remove(id)
                setState(id, ConnectionState.Error("Conexion inestable. Intenta de nuevo."))
                return
            }

            Log.d("HW:DevMgr", "[connectDevice] conexion saludable, reuniendo info...")
            setState(id, ConnectionState.GatheringData)
            setActive(id)
            val info = gatherInfo(id, null)
            setInfo(id, info)
            if (isInfoValid(info)) {
                saveInfo(id, info)
                historyRepository?.saveSnapshot(id, info)
                _updatedAt.value = _updatedAt.value.toMutableMap().apply { put(id, System.currentTimeMillis()) }
            } else {
                Log.w("HW:DevMgr", "[connectDevice] info INVALIDA (posiblemente 0/vacia), NO se persiste")
            }
            val displayName = info.general.marketName.ifBlank {
                "${info.general.manufacturer} ${info.general.model}".trim()
            }.ifBlank { device.name }
            if (displayName.isNotBlank() && displayName != device.name) {
                renameDevice(id, displayName, persist = false)
            }
            Log.d("HW:DevMgr", "[connectDevice] CONECTADO id=$id nombre=$displayName")
            setState(id, ConnectionState.Connected)
            updateOnlineStatusFor(id)
        } catch (e: Exception) {
            connections.remove(id)
            val msg = classifyError(e, device)
            Log.e("HW:DevMgr", "[connectDevice] FALLO id=$id: ${e.message}", e)
            setState(id, ConnectionState.Error(msg))
        }
    }

    private fun classifyError(e: Exception, device: ManagedDevice): String {
        val raw = e.message ?: ""
        return when {
            raw.contains("unauthorized", true) ->
                "Acepta la autorizacion ADB en el dispositivo objetivo (pantalla del dispositivo) y vuelve a intentar."
            raw.contains("refused", true) || raw.contains("ECONNREFUSED", true) ||
                    raw.contains("unreachable", true) || raw.contains("ENETUNREACH", true) ||
                    raw.contains("network is unreachable", true) ->
                "No se alcanza el dispositivo (${device.host}:${device.port}). ¿Encendido y en la misma red?"
            raw.contains("timeout", true) || raw.contains("Connection failed", true) ->
                "No se pudo conectar. Acepta la autorizacion ADB en el dispositivo objetivo y vuelve a intentar, o verifica la depuracion por red."
            else -> raw
        }
    }

    fun disconnectDevice(id: String) {
        Log.d("HW:DevMgr", "[disconnectDevice] id=$id")
        try { connections[id]?.close() } catch (_: Exception) {}
        connections.remove(id)
        if (devices.value.find { it.id == id }?.type == DeviceType.USB) {
            usbManager.disconnect()
        }
        setState(id, ConnectionState.Disconnected)
        updateOnlineStatusFor(id)
    }

    fun removeDevice(id: String) {
        Log.d("HW:DevMgr", "[removeDevice] id=$id")
        disconnectDevice(id)
        _modes.value = _modes.value.minus(id)
        _ips.value = _ips.value.minus(id)
        val current = _infos.value.toMutableMap()
        current.remove(id)
        _infos.value = current
        _updatedAt.value = _updatedAt.value.minus(id)
        _online.value = _online.value.minus(id)
        deleteInfo(id)
        val states = _states.value.toMutableMap()
        states.remove(id)
        _states.value = states
        _devices.value = _devices.value.filter { it.id != id }
        saveDevices(_devices.value)
        if (_activeId.value == id) {
            _activeId.value = _devices.value.firstOrNull()?.id
        }
    }

    fun renameDevice(id: String, name: String, persist: Boolean = true) {
        _devices.value = _devices.value.map {
            if (it.id == id) it.copy(name = name) else it
        }
        if (persist) saveDevices(_devices.value)
    }

    suspend fun scanNetwork() {
        _isScanning.value = true
        _scanResults.value = emptyList()
        try {
            val localIp = NetworkScanner.getLocalNetwork(context)?.ip
            Log.d("HW:DevMgr", "[scanNetwork] IP local=$localIp")
            val found = NetworkScanner.scanAdbDevices(context)
            _scanResults.value = found
            Log.d("HW:DevMgr", "[scanNetwork] encontro ${found.size}: $found")
        } catch (e: Exception) {
            Log.e("HW:DevMgr", "[scanNetwork] fallo: ${e.message}", e)
        } finally {
            _isScanning.value = false
        }
    }

    fun refreshDevice(id: String) {
        Log.d("HW:DevMgr", "[refreshDevice] id=$id")
        scope.launch {
            val conn = connections[id]
            if (conn == null) {
                Log.d("HW:DevMgr", "[refreshDevice] sin conexion, reconectando")
                connectDevice(id)
                return@launch
            }

            if (!conn.isHealthy) {
                Log.w("HW:DevMgr", "[refreshDevice] conexion MUERTA (thread=${conn.isConnectionThreadAlive}, connected=${conn.isConnected}), reconectando")
                connections.remove(id)
                connectDevice(id)
                return@launch
            }

            try {
                Log.d("HW:DevMgr", "[refreshDevice] conexion saludable, reuniendo info...")
                val oldInfo = _infos.value[id]
                val info = gatherInfo(id, oldInfo)
                if (isInfoValid(info)) {
                    setInfo(id, info)
                    saveInfo(id, info)
                    historyRepository?.saveSnapshot(id, info)
                    _updatedAt.value = _updatedAt.value.toMutableMap().apply { put(id, System.currentTimeMillis()) }
                    Log.d("HW:DevMgr", "[refreshDevice] info actualizada OK")
                } else {
                    Log.w("HW:DevMgr", "[refreshDevice] info INVALIDA (posiblemente 0/vacia), se mantienen datos anteriores")
                    setInfo(id, oldInfo)
                }
                updateOnlineStatusFor(id)
            } catch (e: Exception) {
                Log.e("HW:DevMgr", "[refreshDevice] fallo gatherInfo: ${e.message}, manteniendo datos anteriores", e)
            }
        }
    }

    fun refreshActive() {
        val id = _activeId.value ?: return
        Log.d("HW:DevMgr", "[refreshActive] activo=$id")
        refreshDevice(id)
    }

    fun switchActiveToWifi() {
        val id = _activeId.value ?: return
        val device = devices.value.find { it.id == id } ?: return
        Log.d("HW:DevMgr", "[switchActiveToWifi] id=$id type=${device.type}")
        if (device.type != DeviceType.USB) {
            Log.d("HW:DevMgr", "[switchActiveToWifi] no es USB, ignorando")
            return
        }
        scope.launch {
            Log.d("HW:DevMgr", "[switchActiveToWifi] iniciando cambio a WiFi")
            setState(id, ConnectionState.ConnectingWifi)
            try {
                val conn = usbManager.enableTcpIpAndConnect()
                connections[id] = conn
                val ip = usbManager.getDeviceIp() ?: device.host
                setMode(id, "Red")
                setIp(id, ip)
                renameDevice(id, ip, persist = true)
                val info = gatherInfo(id)
                setInfo(id, info)
                if (isInfoValid(info)) {
                    saveInfo(id, info)
                    _updatedAt.value = _updatedAt.value.toMutableMap().apply { put(id, System.currentTimeMillis()) }
                }
                setState(id, ConnectionState.Connected)
                updateOnlineStatusFor(id)
                Log.d("HW:DevMgr", "[switchActiveToWifi] completado ip=$ip")
            } catch (e: Exception) {
                Log.e("HW:DevMgr", "[switchActiveToWifi] fallo: ${e.message}", e)
                setState(id, ConnectionState.Error(e.message ?: "Error al cambiar a WiFi"))
            }
        }
    }

    private suspend fun executeSafe(id: String, command: String): String {
        val conn = connections[id] ?: return ""
        return try {
            usbManager.executeCommandRaw(conn, command)
        } catch (e: Exception) {
            Log.w("HW:DevMgr", "[executeSafe] id=$id cmd='$command' fallo: ${e.message}")
            ""
        }
    }

    private suspend fun extractImeis(id: String): List<String> = coroutineScope {
        val results = mutableListOf<String>()

        // WiFi connections are fragile with concurrent commands - detect device type
        val device = devices.value.find { it.id == id }
        val isWifi = device?.type == DeviceType.NETWORK
        if (isWifi) {
            Log.d("HW:DevMgr", "[extractImeis] modo WiFi: comandos secuenciales")
        }

        Log.d("HW:DevMgr", "[extractImeis] FASE 1: cmd phone get-imei (Android 12+)")
        val hasCmd = executeSafe(id, "which cmd").trim()
        if (hasCmd.isNotBlank() && !hasCmd.contains("not found")) {
            val raw0: String
            val raw1: String
            if (isWifi) {
                // Serial for WiFi to avoid connection drops
                raw0 = executeSafe(id, DeviceCommand.IMEI_V2_0.command)
                raw1 = executeSafe(id, DeviceCommand.IMEI_V2_1.command)
            } else {
                // Parallel for USB (fast and reliable)
                val v2_0 = async { executeSafe(id, DeviceCommand.IMEI_V2_0.command) }
                val v2_1 = async { executeSafe(id, DeviceCommand.IMEI_V2_1.command) }
                raw0 = v2_0.await()
                raw1 = v2_1.await()
            }
            val imei0 = CommandParser.parseImeiV2(raw0)
            val imei1 = CommandParser.parseImeiV2(raw1)
            if (imei0.isNotBlank()) results.add(imei0)
            if (imei1.isNotBlank()) results.add(imei1)
            if (results.isNotEmpty()) {
                Log.d("HW:DevMgr", "[extractImeis] FASE 1 OK: ${results.size} IMEIs via cmd phone")
                return@coroutineScope results.distinct().take(2)
            }
        } else {
            Log.d("HW:DevMgr", "[extractImeis] cmd no disponible, saltando FASE 1")
        }

        Log.d("HW:DevMgr", "[extractImeis] FASE 1 vacio, FASE 2: service call iphonesubinfo")
        val legacy: List<String>
        if (isWifi) {
            // Serial for WiFi - these commands are especially fragile over TCP
            legacy = listOf(
                executeSafe(id, DeviceCommand.IMEI.command),
                executeSafe(id, DeviceCommand.IMEI_SLOT2.command),
                executeSafe(id, DeviceCommand.IMEI_SLOT2_A.command),
                executeSafe(id, DeviceCommand.IMEI_SLOT2_B.command)
            )
        } else {
            legacy = listOf(
                async { executeSafe(id, DeviceCommand.IMEI.command) },
                async { executeSafe(id, DeviceCommand.IMEI_SLOT2.command) },
                async { executeSafe(id, DeviceCommand.IMEI_SLOT2_A.command) },
                async { executeSafe(id, DeviceCommand.IMEI_SLOT2_B.command) }
            ).map { it.await() }
        }
        val legacyResults = legacy
            .mapNotNull { raw -> CommandParser.parseImei(raw) }
            .filter { it.isNotBlank() }
        results.addAll(legacyResults)
        if (results.isNotEmpty()) {
            Log.d("HW:DevMgr", "[extractImeis] FASE 2 OK: ${results.size} IMEIs via iphonesubinfo")
            return@coroutineScope results.distinct().take(2)
        }

        Log.d("HW:DevMgr", "[extractImeis] FASE 2 vacia, FASE 3: UIAutomator *#06#")
        try {
            val uiRaw = executeSafe(id, "input keyevent KEYCODE_WAKEUP; input keyevent KEYCODE_CALL; sleep 1; input text '*#06#'; uiautomator dump --compressed /dev/stdout")
            val imeiPattern = Regex("""\b(\d{15})\b""")
            val found = imeiPattern.findAll(uiRaw)
                .map { it.value }
                .filter { CommandParser.isValidImeiPublic(it) }
                .distinct()
                .take(2)
                .toList()
            results.addAll(found)
        } catch (e: Exception) {
            Log.w("HW:DevMgr", "[extractImeis] UIAutomator fallo: ${e.message}")
        }

        Log.d("HW:DevMgr", "[extractImeis] TOTAL: ${results.size} IMEIs")
        results.distinct().take(2)
    }

    private fun String.fallback(old: String?): String =
        if (isNotBlank()) this else (old ?: "")

    private val BULK_SEP = "__HW_BULK_SEP_7C3F__"

    private val BULK_COMMANDS = listOf(
        "getprop",
        "getprop ro.soc.model",
        "getprop ro.soc.manufacturer",
        "getprop ro.hardware.chipname",
        "getprop ro.board.platform",
        "getprop ro.vendor.soc.model",
        "getprop ro.product.marketname",
        "cat /sys/devices/soc0/family",
        "cat /sys/devices/soc0/machine",
        "getprop ro.product.cpu.abi",
        "cat /proc/cpuinfo",
        "cat /proc/meminfo",
        "dumpsys battery",
        "wm size",
        "wm density",
        "dumpsys display",
        "df",
        "dumpsys media.camera",
        "dumpsys sensorservice",
        "ip addr",
        "getprop wifi.interface",
        "getprop ro.hardware"
    )

    private fun buildBulkScript(): String =
        BULK_COMMANDS.joinToString("; echo \"$BULK_SEP\"; ") + "; echo \"$BULK_SEP\""

    private data class BulkRaw(
        val getprop: String = "",
        val socModel: String = "",
        val socManufacturer: String = "",
        val socChipname: String = "",
        val socPlatform: String = "",
        val socVendorModel: String = "",
        val marketName: String = "",
        val soc0Family: String = "",
        val soc0Machine: String = "",
        val cpuAbi: String = "",
        val cpuInfo: String = "",
        val memInfo: String = "",
        val battery: String = "",
        val displaySize: String = "",
        val displayDensity: String = "",
        val displayInfo: String = "",
        val df: String = "",
        val cameras: String = "",
        val sensors: String = "",
        val network: String = "",
        val wifiInterface: String = "",
        val hardware: String = ""
    )

    private fun splitBulk(out: String): BulkRaw {
        val parts = out.split(BULK_SEP)
        fun p(i: Int) = parts.getOrElse(i) { "" }.trim()
        return BulkRaw(
            getprop = p(0), socModel = p(1), socManufacturer = p(2), socChipname = p(3),
            socPlatform = p(4), socVendorModel = p(5), marketName = p(6), soc0Family = p(7),
            soc0Machine = p(8), cpuAbi = p(9), cpuInfo = p(10), memInfo = p(11),
            battery = p(12), displaySize = p(13), displayDensity = p(14), displayInfo = p(15),
            df = p(16), cameras = p(17), sensors = p(18), network = p(19),
            wifiInterface = p(20), hardware = p(21)
        )
    }

    private suspend fun reconnect(id: String): AdbConnection? {
        val device = devices.value.find { it.id == id } ?: return null
        // Close old connection before creating a new one
        val old = connections.remove(id)
        if (old != null) {
            Log.d("HW:DevMgr", "[reconnect] cerrando conexion anterior para $id")
            try { old.close() } catch (_: Exception) {}
        }
        return try {
            val conn = if (device.type == DeviceType.USB) usbManager.connect()
            else usbManager.openTcpConnection(device.host, device.port)
            connections[id] = conn
            conn
        } catch (e: Exception) {
            Log.w("HW:DevMgr", "[reconnect] fallo id=$id: ${e.message}")
            null
        }
    }

    private suspend fun collectBulk(id: String): BulkRaw {
        repeat(2) { attempt ->
            val conn = connections[id]?.takeIf { it.isHealthy }
                ?: run {
                    val reconnected = reconnect(id) ?: return@collectBulk BulkRaw()
                    connections[id] = reconnected
                    reconnected
                }
            try {
                val out = usbManager.executeCommandRaw(conn, buildBulkScript())
                Log.d("HW:DevMgr", "[collectBulk] intento $attempt OK (${out.length} chars)")
                return@collectBulk splitBulk(out)
            } catch (e: Exception) {
                Log.w("HW:DevMgr", "[collectBulk] intento $attempt fallo: ${e.message}")
                if (connections[id] === conn) connections.remove(id)
            }
        }
        return BulkRaw()
    }

    private suspend fun gatherInfo(id: String, oldInfo: DeviceInfo? = null): DeviceInfo = withContext(Dispatchers.IO) {
        val old = oldInfo
        val bulk = collectBulk(id)

        val getprop = bulk.getprop
        val socModel = bulk.socModel.trim().fallback(old?.cpu?.socName)
        val socManufacturer = bulk.socManufacturer.trim().fallback(old?.cpu?.socManufacturer)
        val socChipname = bulk.socChipname.trim().fallback(old?.cpu?.hardware)
        val socPlatform = bulk.socPlatform.trim()
        val socVendorModel = bulk.socVendorModel.trim()
        val marketName = bulk.marketName.trim().fallback(old?.general?.marketName)
        val soc0Family = bulk.soc0Family.trim()
        val soc0Machine = bulk.soc0Machine.trim()
        val cpuAbi = bulk.cpuAbi.trim().fallback(old?.cpu?.cpuAbi)

        val chipsetInfo = chipsetRepository?.let { repo ->
            // Try multiple codename sources: platform, socModel, chipname, vendorModel
            // ro.board.platform (e.g. "lahaina") may not match SOC_NAMES, but
            // ro.soc.model (e.g. "SM8350") will
            val candidates = listOf(
                bulk.socPlatform.trim(),
                bulk.socModel.trim(),
                bulk.socChipname.trim(),
                bulk.socVendorModel.trim()
            ).filter { it.isNotBlank() }.distinct()
            if (candidates.isNotEmpty()) {
                Log.d("HW:DevMgr", "[gatherInfo] chipset candidates: $candidates")
            }
            // Stop only when a non-empty chipset is found (ChipsetInfo() has blank chipset = not found)
            candidates.firstNotNullOfOrNull { codename ->
                val result = try { repo.resolve(codename) } catch (_: Exception) { null }
                if (result != null && result.chipset.isNotBlank()) result else null
            }
        }

        val imeis = runCatching { extractImeis(id) }.getOrNull().orEmpty()
            .ifEmpty { old?.general?.imeis ?: emptyList() }

        val general = if (getprop.isNotBlank()) {
            CommandParser.parseGeneralInfo(
                getprop, socModel, socManufacturer, socChipname, socPlatform,
                socVendorModel, marketName, soc0Family, soc0Machine, imeis
            )
        } else {
            old?.general ?: GeneralInfo()
        }

        val built = DeviceInfo(
            general = general,
            cpu = if (bulk.cpuInfo.isNotBlank())
                CommandParser.parseCpuInfo(
                    bulk.cpuInfo, socModel, socManufacturer, socChipname, socPlatform,
                    socVendorModel, soc0Family, soc0Machine, chipsetInfo = chipsetInfo, cpuAbi = cpuAbi
                ) else old?.cpu ?: CpuInfo(),
            memory = if (bulk.memInfo.isNotBlank()) CommandParser.parseMemoryInfo(bulk.memInfo) else old?.memory ?: MemoryInfo(),
            battery = if (bulk.battery.isNotBlank()) CommandParser.parseBatteryInfo(bulk.battery) else old?.battery ?: BatteryInfo(),
            display = if (bulk.displaySize.isNotBlank() || bulk.displayInfo.isNotBlank())
                CommandParser.parseDisplayInfo(bulk.displaySize, bulk.displayDensity, bulk.displayInfo)
            else old?.display ?: DisplayInfo(),
            storage = if (bulk.df.isNotBlank()) CommandParser.parseStorageInfo(bulk.df) else old?.storage ?: StorageInfo(),
            cameras = if (bulk.cameras.isNotBlank()) CommandParser.parseCameraInfo(bulk.cameras) else old?.cameras ?: emptyList(),
            sensors = if (bulk.sensors.isNotBlank()) CommandParser.parseSensorInfo(bulk.sensors) else old?.sensors ?: emptyList(),
            network = if (bulk.network.isNotBlank())
                CommandParser.parseNetworkInfo(bulk.network, bulk.wifiInterface.trim().fallback(old?.network?.wifiInterface))
            else old?.network ?: NetworkInfo(),
            build = if (getprop.isNotBlank() || bulk.hardware.isNotBlank())
                CommandParser.parseBuildInfo(getprop.ifBlank { old?.build?.id ?: "" }, bulk.hardware.trim().fallback(old?.build?.display))
            else old?.build ?: BuildInfo()
        )

        if (old != null && !isInfoValid(built)) {
            Log.w("HW:DevMgr", "[gatherInfo] resultado invalido (probablemente 0/vacio), conservando datos previos")
            old
        } else {
            built
        }
    }

    private fun devicesFile(): File = File(context.filesDir, "devices.json")

    private fun saveDevices(list: List<ManagedDevice>) {
        try {
            val array = JSONArray()
            list.forEach { d ->
                val obj = JSONObject()
                obj.put("id", d.id)
                obj.put("name", d.name)
                obj.put("host", d.host)
                obj.put("port", d.port)
                obj.put("type", if (d.type == DeviceType.USB) "USB" else "NETWORK")
                obj.put("addedAt", d.addedAt)
                array.put(obj)
            }
            devicesFile().writeText(array.toString())
        } catch (e: Exception) {
            Log.e("Hardwire", "[DeviceManager] saveDevices fallo: ${e.message}")
        }
    }

    private fun loadDevices(): List<ManagedDevice> {
        return try {
            val file = devicesFile()
            if (!file.exists()) return emptyList()
            val array = JSONArray(file.readText())
            val result = mutableListOf<ManagedDevice>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = if (obj.optString("type") == "USB") DeviceType.USB else DeviceType.NETWORK
                result.add(
                    ManagedDevice(
                        id = obj.getString("id"),
                        name = obj.optString("name", ""),
                        host = obj.optString("host", ""),
                        port = obj.optInt("port", 5555),
                        type = type,
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e("Hardwire", "[DeviceManager] loadDevices fallo: ${e.message}")
            emptyList()
        }
    }

    private fun isInfoValid(info: DeviceInfo): Boolean {
        return info.general.model.isNotBlank() ||
                info.general.manufacturer.isNotBlank() ||
                info.cpu.socName.isNotBlank() ||
                info.memory.totalRamBytes > 0 ||
                info.general.imeis.isNotEmpty() ||
                info.network.interfaces.isNotEmpty()
    }

    private fun infoFile(id: String): File =
        File(context.filesDir, "device_info_${id.replace(':', '_')}.json")

    private fun saveInfo(id: String, info: DeviceInfo) {
        try {
            val wrapper = JSONObject()
            wrapper.put("updatedAt", System.currentTimeMillis())
            wrapper.put("data", DeviceInfoJson.toJson(info))
            infoFile(id).writeText(wrapper.toString())
            Log.d("HW:DevMgr", "[saveInfo] persistido id=$id (${infoFile(id).length()} bytes)")
        } catch (e: Exception) {
            Log.e("HW:DevMgr", "[saveInfo] fallo id=$id: ${e.message}")
        }
    }

    private fun loadInfo(id: String): Pair<DeviceInfo, Long>? {
        return try {
            val file = infoFile(id)
            if (!file.exists()) return null
            val wrapper = JSONObject(file.readText())
            val data = DeviceInfoJson.fromJson(wrapper.optJSONObject("data"))
            val updatedAt = wrapper.optLong("updatedAt", 0L)
            data to updatedAt
        } catch (e: Exception) {
            Log.e("HW:DevMgr", "[loadInfo] fallo id=$id: ${e.message}")
            null
        }
    }

    private fun deleteInfo(id: String) {
        try { infoFile(id).delete() } catch (_: Exception) {}
    }

    private fun loadAllSaved(): Pair<Map<String, DeviceInfo?>, Map<String, Long>> {
        val infos = mutableMapOf<String, DeviceInfo?>()
        val times = mutableMapOf<String, Long>()
        devices.value.forEach { dev ->
            loadInfo(dev.id)?.let { (info, t) ->
                infos[dev.id] = info
                times[dev.id] = t
            }
        }
        Log.d("HW:DevMgr", "[loadAllSaved] infos cargadas de disco: ${infos.size}")
        return infos to times
    }

    fun isDeviceOnline(id: String): Boolean {
        val device = devices.value.find { it.id == id } ?: return false
        return if (device.type == DeviceType.USB) {
            connections[id]?.isHealthy == true
        } else {
            // WiFi: usar la conexion ADB existente en vez de crear nuevo socket
            // (adbd solo acepta 1 conexion en port 5555)
            val conn = connections[id]
            conn?.isHealthy == true
        }
    }

    private fun updateOnlineStatusFor(id: String) {
        val device = devices.value.find { it.id == id } ?: return
        val online = if (device.type == DeviceType.USB) {
            connections[id]?.isHealthy == true
        } else {
            // WiFi: usar la conexion ADB existente
            val conn = connections[id]
            conn?.isHealthy == true
        }
        _online.value = _online.value.toMutableMap().apply { put(id, online) }
    }

    suspend fun refreshOnlineStatus() {
        val map = mutableMapOf<String, Boolean>()
        devices.value.forEach { device ->
            map[device.id] = if (device.type == DeviceType.USB) {
                connections[device.id]?.isHealthy == true
            } else {
                // WiFi: usar la conexion ADB existente (no crear nuevo socket)
                connections[device.id]?.isHealthy == true
            }
        }
        _online.value = map
        Log.d("HW:DevMgr", "[refreshOnlineStatus] $map")
    }

    companion object {
        const val USB_ID = "usb"
    }
}