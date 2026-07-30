package com.hitomatito.hardwire.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hitomatito.hardwire.data.device.DeviceManager
import com.hitomatito.hardwire.data.model.ConnectionState
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.data.model.ManagedDevice
import com.hitomatito.hardwire.di.ServiceLocator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InfoViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceManager = ServiceLocator.getContainer(application).deviceManager

    val activeId: StateFlow<String?> = deviceManager.activeId
    val devices: StateFlow<List<ManagedDevice>> = deviceManager.devices

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

    val activeUpdatedAt: StateFlow<Long> = combine(deviceManager.updatedAt, activeId) { map, id ->
        id?.let { map[it] } ?: 0L
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

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

    fun selectDevice(id: String) {
        deviceManager.selectDevice(id)
    }
}
