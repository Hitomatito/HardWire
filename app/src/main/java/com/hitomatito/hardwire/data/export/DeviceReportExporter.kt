package com.hitomatito.hardwire.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.data.model.ManagedDevice
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeviceReportExporter {

    fun exportToJson(
        context: Context,
        device: ManagedDevice,
        info: DeviceInfo
    ): Uri? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "hardwire_${device.name.replace(" ", "_")}_${timestamp}.json"
            val file = File(context.cacheDir, "reports")
            file.mkdirs()
            val outputFile = File(file, filename)

            val json = buildJsonObject(info, device)
            outputFile.writeText(json)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: Exception) {
            null
        }
    }

    fun shareReport(
        context: Context,
        device: ManagedDevice,
        info: DeviceInfo
    ) {
        val uri = exportToJson(context, device, info) ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Hardwire Report - ${device.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir informe"))
    }

    fun copyToClipboard(
        context: Context,
        device: ManagedDevice,
        info: DeviceInfo
    ) {
        val text = buildPlainTextReport(info, device)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Device Report", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun buildJsonObject(info: DeviceInfo, device: ManagedDevice): String {
        return buildString {
            appendLine("{")
            appendLine("  \"device\": {")
            appendLine("    \"name\": \"${device.name}\",")
            appendLine("    \"host\": \"${device.host}\",")
            appendLine("    \"type\": \"${device.type}\"")
            appendLine("  },")
            appendLine("  \"general\": {")
            appendLine("    \"manufacturer\": \"${info.general.manufacturer}\",")
            appendLine("    \"model\": \"${info.general.model}\",")
            appendLine("    \"marketName\": \"${info.general.marketName}\",")
            appendLine("    \"androidVersion\": \"${info.general.androidVersion}\",")
            appendLine("    \"sdkVersion\": \"${info.general.sdkVersion}\",")
            appendLine("    \"serialNumber\": \"${info.general.serialNumber}\",")
            appendLine("    \"imeis\": [${info.general.imeis.joinToString(",") { "\"$it\"" }}]")
            appendLine("  },")
            appendLine("  \"cpu\": {")
            appendLine("    \"socName\": \"${info.cpu.socName}\",")
            appendLine("    \"socManufacturer\": \"${info.cpu.socManufacturer}\",")
            appendLine("    \"architecture\": \"${info.cpu.architecture}\",")
            appendLine("    \"processorCount\": ${info.cpu.processorCount}")
            appendLine("  },")
            appendLine("  \"memory\": {")
            appendLine("    \"totalRamFormatted\": \"${info.memory.totalRamFormatted}\",")
            appendLine("    \"availableRamFormatted\": \"${info.memory.availableRamFormatted}\",")
            appendLine("    \"usagePercent\": ${info.memory.usagePercent}")
            appendLine("  },")
            appendLine("  \"battery\": {")
            appendLine("    \"level\": \"${info.battery.level}\",")
            appendLine("    \"status\": \"${info.battery.status}\",")
            appendLine("    \"health\": \"${info.battery.health}\",")
            appendLine("    \"technology\": \"${info.battery.technology}\"")
            appendLine("  },")
            appendLine("  \"display\": {")
            appendLine("    \"resolution\": \"${info.display.resolution}\",")
            appendLine("    \"density\": \"${info.display.density}\",")
            appendLine("    \"refreshRate\": \"${info.display.refreshRate}\"")
            appendLine("  },")
            appendLine("  \"build\": {")
            appendLine("    \"brand\": \"${info.build.brand}\",")
            appendLine("    \"display\": \"${info.build.display}\",")
            appendLine("    \"fingerprint\": \"${info.build.fingerprint}\"")
            appendLine("  }")
            append("}")
        }
    }

    private fun buildPlainTextReport(info: DeviceInfo, device: ManagedDevice): String {
        return buildString {
            appendLine("=== Hardwire Device Report ===")
            appendLine("Device: ${device.name}")
            appendLine("Host: ${device.host}")
            appendLine("Type: ${device.type}")
            appendLine()
            appendLine("--- General ---")
            appendLine("Manufacturer: ${info.general.manufacturer}")
            appendLine("Model: ${info.general.model}")
            appendLine("Market Name: ${info.general.marketName}")
            appendLine("Android: ${info.general.androidVersion} (SDK ${info.general.sdkVersion})")
            appendLine("Serial: ${info.general.serialNumber}")
            if (info.general.imeis.isNotEmpty()) {
                appendLine("IMEI: ${info.general.imeis.joinToString(", ")}")
            }
            appendLine()
            appendLine("--- CPU ---")
            appendLine("SoC: ${info.cpu.socName}")
            appendLine("Manufacturer: ${info.cpu.socManufacturer}")
            appendLine("Architecture: ${info.cpu.architecture}")
            appendLine("Cores: ${info.cpu.processorCount}")
            appendLine()
            appendLine("--- Memory ---")
            appendLine("Total RAM: ${info.memory.totalRamFormatted}")
            appendLine("Available: ${info.memory.availableRamFormatted}")
            appendLine("Usage: ${String.format("%.1f", info.memory.usagePercent)}%")
            appendLine()
            appendLine("--- Battery ---")
            appendLine("Level: ${info.battery.level}%")
            appendLine("Status: ${info.battery.status}")
            appendLine("Health: ${info.battery.health}")
            appendLine("Technology: ${info.battery.technology}")
            appendLine("Temperature: ${info.battery.temperature}")
            appendLine()
            appendLine("--- Display ---")
            appendLine("Resolution: ${info.display.resolution}")
            appendLine("Density: ${info.display.density}")
            appendLine("Refresh Rate: ${info.display.refreshRate}")
            appendLine()
            appendLine("--- Build ---")
            appendLine("Brand: ${info.build.brand}")
            appendLine("Display: ${info.build.display}")
            appendLine("Fingerprint: ${info.build.fingerprint}")
        }
    }
}
