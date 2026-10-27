package com.hitomatito.hardwire.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hitomatito.hardwire.R
import java.util.Locale

private fun normalize(value: String): String = value.trim().lowercase(Locale.US)

@Composable
fun batteryStatusText(value: String): String = when (normalize(value)) {
    "cargando", "charging" -> stringResource(R.string.battery_status_charging)
    "descargando", "discharging" -> stringResource(R.string.battery_status_discharging)
    "completo", "full" -> stringResource(R.string.battery_status_full)
    "sin carga", "not charging" -> stringResource(R.string.battery_status_not_charging)
    "desconocido", "unknown", "" -> stringResource(R.string.battery_status_unknown)
    else -> value
}

@Composable
fun batteryHealthText(value: String): String = when (normalize(value)) {
    "buena", "good" -> stringResource(R.string.battery_health_good)
    "sobrecalentada", "overheat" -> stringResource(R.string.battery_health_overheat)
    "muerta", "dead" -> stringResource(R.string.battery_health_dead)
    "sobrevoltaje", "over voltage" -> stringResource(R.string.battery_health_over_voltage)
    "fallo", "failure" -> stringResource(R.string.battery_health_failure)
    "fria", "cold" -> stringResource(R.string.battery_health_cold)
    "desconocido", "desconocida", "unknown", "" -> stringResource(R.string.battery_health_unknown)
    else -> value
}

@Composable
fun batteryPluggedText(value: String): String = when (normalize(value)) {
    "ac" -> stringResource(R.string.battery_plugged_ac)
    "usb" -> stringResource(R.string.battery_plugged_usb)
    "inalambrico", "wireless" -> stringResource(R.string.battery_plugged_wireless)
    "sin carga", "ninguno", "none" -> stringResource(R.string.battery_plugged_none)
    "desconocido", "unknown", "" -> stringResource(R.string.battery_plugged_unknown)
    else -> value
}

@Composable
fun cameraFacingText(value: String): String = when (normalize(value)) {
    "trasera", "back" -> stringResource(R.string.camera_facing_back)
    "frontal", "front" -> stringResource(R.string.camera_facing_front)
    "externa", "external" -> stringResource(R.string.camera_facing_external)
    "desconocido", "desconocida", "unknown", "" -> stringResource(R.string.camera_facing_unknown)
    else -> value
}

@Composable
fun cameraFlashText(value: String): String = when (normalize(value)) {
    "si", "sí", "yes" -> stringResource(R.string.yes)
    "no" -> stringResource(R.string.no)
    "desconocido", "unknown", "" -> stringResource(R.string.unknown)
    else -> value
}
