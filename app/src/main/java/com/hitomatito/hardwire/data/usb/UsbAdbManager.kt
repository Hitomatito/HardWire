package com.hitomatito.hardwire.data.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hitomatito.hardwire.adblib.AdbBase64
import com.hitomatito.hardwire.adblib.AdbConnection
import com.hitomatito.hardwire.adblib.AdbCrypto
import com.hitomatito.hardwire.adblib.AdbStream
import com.hitomatito.hardwire.adblib.TcpChannel
import com.hitomatito.hardwire.adblib.UsbChannel
import com.hitomatito.hardwire.data.command.DeviceCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.net.Socket

private fun makeBase64(): AdbBase64 = object : AdbBase64 {
    override fun encodeToString(data: ByteArray): String {
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
    }
}

class UsbAdbManager(private val context: Context) {

    companion object {
        private const val ACTION_USB_PERMISSION = "com.hitomatito.hardwire.USB_PERMISSION"
        private const val ADB_CLASS = 0xFF
        private const val ADB_SUBCLASS = 0x42
        private const val ADB_PROTOCOL = 0x01
    }

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var adbConnection: AdbConnection? = null
    private var crypto: AdbCrypto? = null
    private var pendingDevice: UsbDevice? = null
    private var permissionRequestCode = 0

    private fun getCrypto(): AdbCrypto {
        if (crypto != null) return crypto!!
        val base64 = makeBase64()
        val privFile = File(context.filesDir, "adbkey")
        val pubFile = File(context.filesDir, "adbkey.pub")
        crypto = try {
            if (privFile.exists() && pubFile.exists()) {
                AdbCrypto.loadAdbKeyPair(base64, privFile, pubFile)
            } else {
                val c = AdbCrypto.generateAdbKeyPair(base64)
                c.saveAdbKeyPair(privFile, pubFile)
                c
            }
        } catch (e: Exception) {
            AdbCrypto.generateAdbKeyPair(base64)
        }
        return crypto!!
    }

    var onPermissionGranted: (() -> Unit)? = null
    var onPermissionDenied: (() -> Unit)? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            Log.d("HW:Usb", "[onReceive] action=${intent.action}")
            if (intent.action == ACTION_USB_PERMISSION) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                val resolvedDevice = device ?: pendingDevice
                Log.d("HW:Usb", "[onReceive] granted=$granted device=${resolvedDevice?.deviceName}")
                if (granted && resolvedDevice != null) {
                    pendingDevice = resolvedDevice
                    onPermissionGranted?.invoke()
                } else if (!granted && resolvedDevice != null) {
                    Log.w("HW:Usb", "[onReceive] permiso DENEGADO para ${resolvedDevice.deviceName}")
                    onPermissionDenied?.invoke()
                } else {
                    Log.w("HW:Usb", "[onReceive] permiso DENEGADO sin dispositivo conocido")
                    onPermissionDenied?.invoke()
                }
            }
        }
    }

    fun registerReceiver() {
        Log.d("HW:Usb", "[registerReceiver] registrando receptor USB")
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        Log.d("HW:Usb", "[registerReceiver] receptor registrado OK")
    }

    fun unregisterReceiver() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }

    fun findAdbDevice(): UsbDevice? {
        val list = usbManager.deviceList
        Log.d("HW:Usb", "[findAdbDevice] dispositivos USB conectados=${list.size}: " +
            list.values.joinToString { "${it.deviceName} vid=${it.vendorId} pid=${it.productId}" })
        for (device in list.values) {
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == ADB_CLASS &&
                    iface.interfaceSubclass == ADB_SUBCLASS &&
                    iface.interfaceProtocol == ADB_PROTOCOL) {
                    Log.d("HW:Usb", "[findAdbDevice] dispositivo ADB encontrado: ${device.deviceName}")
                    return device
                }
            }
        }
        Log.d("HW:Usb", "[findAdbDevice] NINGUN dispositivo ADB encontrado")
        return null
    }

    fun requestPermission(device: UsbDevice) {
        pendingDevice = device
        if (usbManager.hasPermission(device)) {
            Log.d("HW:Usb", "[requestPermission] permiso YA CONCEDIDO para ${device.deviceName}")
            onPermissionGranted?.invoke()
            return
        }
        Log.d("HW:Usb", "[requestPermission] solicitando permiso para ${device.deviceName} vid=${device.vendorId} pid=${device.productId}")
        try {
            val code = ++permissionRequestCode
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val intent = Intent(ACTION_USB_PERMISSION)
            Log.d("HW:Usb", "[requestPermission] creando PendingIntent code=$code action=$ACTION_USB_PERMISSION")
            val permissionIntent = PendingIntent.getBroadcast(
                context, code, intent, flags
            )
            Log.d("HW:Usb", "[requestPermission] PendingIntent creado, llamando usbManager.requestPermission...")
            usbManager.requestPermission(device, permissionIntent)
            Log.d("HW:Usb", "[requestPermission] usbManager.requestPermission() llamado OK")
        } catch (e: Exception) {
            Log.e("HW:Usb", "[requestPermission] FALLO: ${e.message}", e)
        }
    }

    fun hasPermission(device: UsbDevice): Boolean {
        val has = usbManager.hasPermission(device)
        Log.d("HW:Usb", "[hasPermission] ${device.deviceName} -> $has")
        return has
    }

    fun setPendingDevice(device: UsbDevice) {
        pendingDevice = device
        Log.d("HW:Usb", "[setPendingDevice] ${device.deviceName}")
    }

    suspend fun connect(): AdbConnection = withContext(Dispatchers.IO) {
        // Re-find ADB device to handle USB re-enumeration (device path may have changed)
        val freshDevice = findAdbDevice()
        if (freshDevice != null) {
            if (freshDevice != pendingDevice) {
                Log.d("HW:Usb", "[connect] USB re-enumerado: ${pendingDevice?.deviceName} -> ${freshDevice.deviceName}")
                pendingDevice = freshDevice
            }
        }
        val device = pendingDevice ?: throw IllegalStateException("No device selected")
        Log.d("HW:Usb", "[connect] iniciando para ${device.deviceName}")
        val usbInterface = findAdbInterface(device)
            ?: throw IllegalStateException("ADB interface not found")

        if (!usbManager.hasPermission(device)) {
            Log.w("HW:Usb", "[connect] sin permiso para ${device.deviceName}, solicitando...")
            throw IllegalStateException("USB permission required")
        }

        val connection = usbManager.openDevice(device)
            ?: throw IllegalStateException("Cannot open USB device - device may be busy or disconnected")
        Log.d("HW:Usb", "[connect] openDevice OK")

        if (!connection.claimInterface(usbInterface, true)) {
            connection.close()
            throw IllegalStateException("Cannot claim USB interface")
        }
        Log.d("HW:Usb", "[connect] claimInterface OK")

        val channel = UsbChannel(connection, usbInterface)
        crypto = getCrypto()

        val adbConn = AdbConnection.create(channel, crypto!!)
        Log.d("HW:Usb", "[connect] AdbConnection creada, llamando connect()")
        adbConn.connect()
        adbConnection = adbConn
        Log.d("HW:Usb", "[connect] ADB conectado por USB")
        adbConn
    }

    suspend fun executeCommand(command: DeviceCommand): String = withContext(Dispatchers.IO) {
        val conn = adbConnection ?: throw IllegalStateException("Not connected")
        executeCommandRaw(conn, command.command)
    }

    suspend fun executeCommandRaw(conn: AdbConnection, command: String, retries: Int = 2): String = withContext(Dispatchers.IO) {
        for (attempt in 1..retries) {
            Log.v("HW:Usb", "[execCmd] intento $attempt/$retries abriendo shell:'$command'")
            try {
                val stream = conn.open("shell:$command")
                val output = StringBuilder()
                try {
                    while (!stream.isClosed) {
                        try {
                            val data = stream.read(1000) ?: continue
                            output.append(String(data))
                        } catch (_: Exception) {
                            break
                        }
                    }
                    if (output.isNotEmpty()) {
                        Log.v("HW:Usb", "[execCmd] OK (${output.length} chars)")
                        return@withContext output.toString()
                    }
                    Log.w("HW:Usb", "[execCmd] VACIO en intento $attempt")
                } finally {
                    try { stream.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w("HW:Usb", "[execCmd] intento $attempt fallo: ${e.message}")
            }
            if (attempt < retries) delay(300)
        }
        Log.w("HW:Usb", "[execCmd] vacio tras $retries intentos cmd='$command', retornando vacio")
        ""
    }

    fun disconnect() {
        Log.d("HW:Usb", "[disconnect] desconectando USB")
        try { adbConnection?.close() } catch (_: Exception) {}
        adbConnection = null
    }

    fun isConnected(): Boolean {
        val c = adbConnection != null
        Log.d("HW:Usb", "[isConnected] -> $c")
        return c
    }

    fun isHealthy(): Boolean {
        val conn = adbConnection
        if (conn == null) {
            Log.d("HW:Usb", "[isHealthy] adbConnection=null -> false")
            return false
        }
        val healthy = conn.isHealthy
        Log.d("HW:Usb", "[isHealthy] connected=${conn.isConnected} threadAlive=${conn.isConnectionThreadAlive} -> $healthy")
        return healthy
    }

    fun getActiveConnection(): AdbConnection? = adbConnection

    suspend fun connectToNetworkDevice(host: String, port: Int = 5555): AdbConnection = withContext(Dispatchers.IO) {
        Log.d("HW:Usb", "[connectToNetworkDevice] $host:$port")
        disconnect()
        connectTcp(host, port)
    }

    suspend fun openTcpConnection(host: String, port: Int = 5555): AdbConnection = withContext(Dispatchers.IO) {
        Log.d("HW:Usb", "[openTcp] abriendo socket TCP $host:$port")
        disconnect()
        val socket = Socket(host, port)
        socket.soTimeout = 0
        Log.d("HW:Usb", "[openTcp] socket conectado a $host:$port")
        val channel = TcpChannel(socket)
        val adbCrypto = getCrypto()
        val adbConn = AdbConnection.create(channel, adbCrypto)
        try {
            adbConn.connect(15000)
        } catch (e: Exception) {
            Log.e("HW:Usb", "[openTcp] connect() fallo para $host:$port", e)
            try { adbConn.close() } catch (_: Exception) {}
            throw e
        }
        Log.d("HW:Usb", "[openTcp] ADB conectado a $host:$port")
        adbConn
    }

    suspend fun connectTcp(host: String, port: Int = 5555): AdbConnection = withContext(Dispatchers.IO) {
        Log.d("HW:Usb", "[connectTcp] $host:$port")
        val adbConn = openTcpConnection(host, port)
        adbConnection = adbConn
        Log.d("HW:Usb", "[connectTcp] adbConnection establecida")
        adbConn
    }

    suspend fun enableTcpIpAndConnect(): AdbConnection = withContext(Dispatchers.IO) {
        Log.d("HW:Usb", "[enableTcpIp] BEGIN")
        val conn = adbConnection ?: throw IllegalStateException("Not connected via USB")
        Log.d("HW:Usb", "[enableTcpIp] conn=$conn")

        val ipOutput = executeCommandRaw(conn, "ip route")
        Log.d("HW:Usb", "[enableTcpIp] ipOutput(${ipOutput.length} chars)")
        val wlanIp = Regex("src\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)").find(ipOutput)?.groupValues?.get(1)
            ?: throw IllegalStateException("No WiFi IP found on output: " + ipOutput.take(200))
        Log.d("HW:Usb", "[enableTcpIp] IP=$wlanIp")

        var tcpIpOk = false
        try {
            Log.d("HW:Usb", "[enableTcpIp] abriendo tcpip:5555")
            val stream = conn.open("tcpip:5555")
            stream.close()
            tcpIpOk = true
            Log.d("HW:Usb", "[enableTcpIp] tcpip:5555 OK")
        } catch (e: Exception) {
            Log.e("HW:Usb", "[enableTcpIp] tcpip:5555 fallo: ${e.message}", e)
        }

        if (!tcpIpOk) {
            Log.d("HW:Usb", "[enableTcpIp] fallback shell setprop")
            try {
                executeCommandRaw(conn, "setprop service.adb.tcp.port 5555; setprop persist.adb.tcp.port 5555; (sleep 1 && start adbd) & stop adbd")
            } catch (e: Exception) {
                Log.e("HW:Usb", "[enableTcpIp] shell fallback fallo: ${e.message}")
            }
        }

        try { adbConnection?.close() } catch (_: Exception) {}
        adbConnection = null

        delay(5000)

        Log.d("HW:Usb", "[enableTcpIp] conectando a $wlanIp")
        val res = connectTcp(wlanIp, 5555)
        Log.d("HW:Usb", "[enableTcpIp] completado")
        res
    }

    suspend fun getDeviceIp(): String? = withContext(Dispatchers.IO) {
        val conn = adbConnection ?: return@withContext null
        Log.d("HW:Usb", "[getDeviceIp] conn=$conn")
        try {
            val ipOutput = executeCommandRaw(conn, "ip route")
            val ip = Regex("src\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)").find(ipOutput)?.groupValues?.get(1)
            Log.d("HW:Usb", "[getDeviceIp] ip=$ip")
            ip
        } catch (e: Exception) {
            Log.e("HW:Usb", "[getDeviceIp] fallo: ${e.message}")
            null
        }
    }

    private fun findAdbInterface(device: UsbDevice): UsbInterface? {
        Log.d("HW:Usb", "[findAdbInterface] buscando interface ADB en ${device.deviceName}")
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == ADB_CLASS &&
                iface.interfaceSubclass == ADB_SUBCLASS &&
                iface.interfaceProtocol == ADB_PROTOCOL) {
                Log.d("HW:Usb", "[findAdbInterface] encontrada en slot=$i")
                return iface
            }
        }
        Log.d("HW:Usb", "[findAdbInterface] NO encontrada")
        return null
    }
}
