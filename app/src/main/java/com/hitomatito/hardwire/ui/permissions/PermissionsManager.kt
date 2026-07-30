package com.hitomatito.hardwire.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class PermissionEntry(
    val permission: String,
    val label: String,
    val description: String,
    val required: Boolean = true
)

fun getRequiredPermissions(): List<PermissionEntry> {
    val list = mutableListOf(
        PermissionEntry(
            Manifest.permission.ACCESS_FINE_LOCATION,
            "Ubicacion",
            "Necesaria para escanear dispositivos ADB en la red WiFi"
        )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(
            PermissionEntry(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                "Dispositivos WiFi cercanos",
                "Necesaria en Android 13+ para escanear la red WiFi"
            )
        )
    }
    return list
}

fun allPermissionsGranted(context: Context): Boolean {
    return getRequiredPermissions().all { entry ->
        ContextCompat.checkSelfPermission(context, entry.permission) == PackageManager.PERMISSION_GRANTED
    }
}

fun getPermissionStatuses(context: Context): List<Pair<PermissionEntry, Boolean>> {
    return getRequiredPermissions().map { entry ->
        entry to (ContextCompat.checkSelfPermission(context, entry.permission) == PackageManager.PERMISSION_GRANTED)
    }
}
