package com.hitomatito.hardwire.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hitomatito.hardwire.data.device.AddDeviceResult
import com.hitomatito.hardwire.data.device.DeviceManager
import com.hitomatito.hardwire.data.model.ConnectionState
import com.hitomatito.hardwire.data.model.ManagedDevice
import com.hitomatito.hardwire.di.ServiceLocator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceManager = ServiceLocator.getContainer(application).deviceManager

    val devices: StateFlow<List<ManagedDevice>> = deviceManager.devices
    val activeId: StateFlow<String?> = deviceManager.activeId
    val states: StateFlow<Map<String, ConnectionState>> = deviceManager.states
    val scanResults: StateFlow<List<String>> = deviceManager.scanResults
    val isScanning: StateFlow<Boolean> = deviceManager.isScanning
    val onlineStatus: StateFlow<Map<String, Boolean>> = deviceManager.onlineStatus

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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
}
