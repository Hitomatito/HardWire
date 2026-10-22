package com.hitomatito.hardwire.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.hitomatito.hardwire.R

data class PermissionEntry(
    val permission: String,
    @StringRes val labelResId: Int,
    @StringRes val descriptionResId: Int,
    val required: Boolean = true
)

fun getRequiredPermissions(): List<PermissionEntry> {
    val list = mutableListOf(
        PermissionEntry(
            Manifest.permission.ACCESS_FINE_LOCATION,
            R.string.permission_location_label,
            R.string.permission_location_description
        )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(
            PermissionEntry(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                R.string.permission_nearby_wifi_label,
                R.string.permission_nearby_wifi_description
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
