package com.hitomatito.hardwire.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hitomatito.hardwire.data.device.AddDeviceResult
import com.hitomatito.hardwire.data.device.DeviceManager
import com.hitomatito.hardwire.data.model.ConnectionState
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.data.model.ManagedDevice
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceManager = DeviceManager(application.applicationContext)

    val devices: StateFlow<List<ManagedDevice>> = deviceManager.devices
    val activeId: StateFlow<String?> = deviceManager.activeId
    val states: StateFlow<Map<String, ConnectionState>> = deviceManager.states
    val scanResults: StateFlow<List<String>> = deviceManager.scanResults
    val isScanning: StateFlow<Boolean> = deviceManager.isScanning
    val updatedAt: StateFlow<Map<String, Long>> = deviceManager.updatedAt
    val onlineStatus: StateFlow<Map<String, Boolean>> = deviceManager.onlineStatus

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val activeUpdatedAt: StateFlow<Long> = combine(deviceManager.updatedAt, activeId) { map, id ->
        id?.let { map[it] } ?: 0L
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val activeDevice: StateFlow<ManagedDevice?> = combine(devices, activeId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeState: StateFlow<ConnectionState> = combine(deviceManager.states, activeId) { states, id ->
        id?.let { states[it] } ?: ConnectionState.Disconnected
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    val activeInfo: StateFlow<DeviceInfo?> = combine(deviceManager.infos, activeId) { infos, id ->
        id?.let { infos[it] }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeMode: StateFlow<String> = combine(deviceManager.modes, activeId) { _, id ->
        id?.let { deviceManager.getMode(it) } ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val activeIp: StateFlow<String> = combine(deviceManager.ips, activeId) { _, id ->
        id?.let { deviceManager.getIp(it) } ?: ""
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val showHub: StateFlow<Boolean> = combine(devices, activeId) { list, id ->
        list.isEmpty() || id == null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val usbState: StateFlow<ConnectionState> = deviceManager.states.map { states ->
        states["usb"] ?: ConnectionState.Disconnected
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    fun connect() {
        Log.d("HW:VM", "[connect] usuario pide conectar USB")
        deviceManager.requestUsbPermission()
    }

    fun scanNetwork() {
        Log.d("HW:VM", "[scanNetwork] usuario pide escanear red")
        viewModelScope.launch { deviceManager.scanNetwork() }
    }

    fun addNetworkDevice(ip: String) {
        Log.d("HW:VM", "[addNetworkDevice] usuario agrega IP=$ip")
        val result = deviceManager.addNetworkDevice(ip)
        if (result is AddDeviceResult.AlreadyExists) {
            _message.value = "El dispositivo ${result.device.host} ya esta registrado"
        }
    }

    fun selectDevice(id: String) {
        Log.d("HW:VM", "[selectDevice] usuario selecciona id=$id")
        deviceManager.selectDevice(id)
    }

    fun disconnectDevice(id: String) {
        Log.d("HW:VM", "[disconnectDevice] usuario desconecta id=$id")
        deviceManager.disconnectDevice(id)
    }

    fun removeDevice(id: String) {
        Log.d("HW:VM", "[removeDevice] usuario elimina id=$id")
        deviceManager.removeDevice(id)
    }

    fun renameDevice(id: String, name: String) {
        Log.d("HW:VM", "[renameDevice] usuario renombra id=$id -> $name")
        deviceManager.renameDevice(id, name)
    }

    fun showAddScreen() {
        Log.d("HW:VM", "[showAddScreen] usuario abre pantalla agregar")
        deviceManager.setActive(null)
        viewModelScope.launch { deviceManager.refreshOnlineStatus() }
    }

    fun refreshOnlineStatus() {
        Log.d("HW:VM", "[refreshOnlineStatus] verificando estado de dispositivos")
        viewModelScope.launch { deviceManager.refreshOnlineStatus() }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refresh() {
        Log.d("HW:VM", "[refresh] usuario refresca info")
        deviceManager.refreshActive()
    }

    fun disconnect() {
        Log.d("HW:VM", "[disconnect] usuario desconecta activo")
        viewModelScope.launch {
            activeId.value?.let { deviceManager.disconnectDevice(it) }
        }
    }

    fun switchToWifi() {
        Log.d("HW:VM", "[switchToWifi] usuario cambia a WiFi")
        deviceManager.switchActiveToWifi()
    }

    override fun onCleared() {
        deviceManager.dispose()
    }
}