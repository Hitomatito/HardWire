package com.hitomatito.hardwire.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hitomatito.hardwire.data.model.*
import com.hitomatito.hardwire.ui.components.HardwireTopBar
import com.hitomatito.hardwire.ui.theme.HardwirePrimary
import kotlinx.coroutines.delay

private val CardBg = Color(0xFF1E1E1E)
private val AccentBlue = HardwirePrimary
private val AccentGreen = Color(0xFF4CAF50)
private val AccentAmber = Color(0xFFFFC107)
private val AccentRed = Color(0xFFF44336)
private val AccentCyan = Color(0xFF00BCD4)

private data class Section(val id: String, val title: String, val icon: ImageVector)

private val sections = listOf(
    Section("general", "General", Icons.Filled.Info),
    Section("cpu", "Procesador", Icons.Filled.Memory),
    Section("memory", "Memoria RAM", Icons.Filled.Storage),
    Section("battery", "Bateria", Icons.Filled.BatteryStd),
    Section("display", "Pantalla", Icons.Filled.PhoneAndroid),
    Section("storage", "Almacenamiento", Icons.Filled.SdStorage),
    Section("cameras", "Camaras", Icons.Filled.CameraAlt),
    Section("sensors", "Sensores", Icons.Filled.Sensors),
    Section("network", "Red", Icons.Filled.Wifi),
    Section("system", "Sistema", Icons.Filled.Code)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(
    deviceInfo: DeviceInfo,
    isLoading: Boolean,
    isSwitchingToWifi: Boolean = false,
    connectionMode: String,
    deviceIp: String,
    isOffline: Boolean = false,
    lastUpdated: Long = 0L,
    onRefresh: () -> Unit,
    onSwitchToWifi: () -> Unit,
    errorMessage: String? = null,
    onMenuClick: (() -> Unit)? = null
) {
    var expandedSections by remember { mutableStateOf(setOf("general")) }
    var dismissedNotifs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var shownNotifs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var netOkCount by remember { mutableIntStateOf(0) }
    var errorCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            shownNotifs = shownNotifs + "error"
            dismissedNotifs = dismissedNotifs - "error"
            errorCount++
        }
    }
    LaunchedEffect(connectionMode) {
        if (connectionMode == "WiFi" || connectionMode == "Red") {
            shownNotifs = shownNotifs + "net-ok"
            dismissedNotifs = dismissedNotifs - "net-ok"
            netOkCount++
        }
    }

    val displayName = deviceInfo.general.marketName.ifBlank {
        "${deviceInfo.general.manufacturer} ${deviceInfo.general.model}"
    }
    val statusText = if ((connectionMode == "WiFi" || connectionMode == "Red") && deviceIp.isNotBlank()) {
        "${if (connectionMode == "WiFi") "WiFi" else "Red"}: $deviceIp:5555"
    } else {
        null
    }

    Scaffold(
        topBar = {
            HardwireTopBar(
                onMenuClick = onMenuClick,
                subtitle = displayName,
                statusText = statusText,
                statusColor = AccentGreen,
                actions = {
                    if (connectionMode == "USB") {
                        IconButton(onClick = onSwitchToWifi) {
                            Icon(Icons.Filled.Wifi, "Cambiar a WiFi")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val hasData = deviceInfo.general.model.isNotBlank() ||
                deviceInfo.general.manufacturer.isNotBlank()
        val pullState = rememberPullToRefreshState()

        PullToRefreshBox(
            state = pullState,
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && !hasData) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (isSwitchingToWifi) "Conectando via WiFi..."
                            else "Obteniendo informacion del dispositivo...",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                item {
                    if (isOffline && hasData) {
                        OfflineBanner(lastUpdated)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = (connectionMode == "WiFi" || connectionMode == "Red") &&
                                errorMessage == null &&
                                "net-ok" in shownNotifs &&
                                "net-ok" !in dismissedNotifs,
                        enter = expandVertically(),
                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
                    ) {
                        TemporaryNotification(
                            timeoutMs = 5000,
                            dismissKey = netOkCount,
                            onDismiss = { dismissedNotifs = dismissedNotifs + "net-ok" },
                            color = Color(0xFF1B3A1B)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        if (connectionMode == "WiFi") "Conexion WiFi activa" else "Conexion de red activa",
                                        color = AccentGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (connectionMode == "WiFi")
                                            "Ya puedes desconectar el cable USB."
                                        else
                                            "Conectado via red (${deviceIp}:5555).",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = errorMessage != null && "error" in shownNotifs && "error" !in dismissedNotifs,
                        enter = expandVertically(),
                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically()
                    ) {
                        TemporaryNotification(
                            timeoutMs = 5000,
                            dismissKey = errorCount,
                            onDismiss = { dismissedNotifs = dismissedNotifs + "error" },
                            color = Color(0xFF3A1B1B)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = AccentRed, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Error de conexion", color = AccentRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(errorMessage ?: "", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                sections.forEach { section ->
                    item {
                        CollapsibleSection(
                            title = section.title,
                            icon = section.icon,
                            expanded = section.id in expandedSections,
                            onToggle = {
                                expandedSections = if (section.id in expandedSections)
                                    expandedSections - section.id
                                else
                                    expandedSections + section.id
                            }
                        ) {
                            when (section.id) {
                                "general" -> GeneralCard(deviceInfo.general)
                                "cpu" -> CpuCard(deviceInfo.cpu)
                                "memory" -> MemoryCard(deviceInfo.memory)
                                "battery" -> BatteryCard(deviceInfo.battery)
                                "display" -> DisplayCard(deviceInfo.display)
                                "storage" -> {
                                    deviceInfo.storage.filesystems.forEach { fs ->
                                        StorageCard(fs)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                                "cameras" -> {
                                    if (deviceInfo.cameras.isNotEmpty()) {
                                        deviceInfo.cameras.forEach { cam ->
                                            CameraCard(cam)
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    } else {
                                        Text("Sin datos", color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                                "sensors" -> {
                                    if (deviceInfo.sensors.isNotEmpty()) {
                                        deviceInfo.sensors.take(10).forEach { sensor ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(sensor.name, color = Color.White, fontSize = 13.sp)
                                                Text("${sensor.vendor}", color = Color.Gray, fontSize = 11.sp)
                                            }
                                        }
                                        if (deviceInfo.sensors.size > 10) {
                                            Text(
                                                "+ ${deviceInfo.sensors.size - 10} sensores mas",
                                                color = AccentBlue, fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        Text("Sin datos", color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                                "network" -> NetworkCard(deviceInfo.network)
                                "system" -> BuildCard(deviceInfo.build)
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun TemporaryNotification(
    timeoutMs: Long = 5000,
    dismissKey: Int = 0,
    onDismiss: () -> Unit,
    color: Color = CardBg,
    content: @Composable ColumnScope.() -> Unit
) {
    LaunchedEffect(dismissKey) {
        if (dismissKey > 0) {
            delay(timeoutMs)
            onDismiss()
        }
    }
    CardContainer(color = color) {
        Column(modifier = Modifier.padding(0.dp), content = content)
    }
}

@Composable
fun OfflineBanner(lastUpdated: Long) {
    CardContainer(color = Color(0xFF2A2418)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Datos guardados (sin conexion)",
                    color = Color(0xFFFFB300),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Informacion obtenida localmente. Actualizado ${formatRelativeTime(lastUpdated)}.",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatRelativeTime(updatedAt: Long): String {
    if (updatedAt <= 0L) return "fecha desconocida"
    val diff = System.currentTimeMillis() - updatedAt
    val min = diff / 60000
    return when {
        min < 1 -> "hace un momento"
        min < 60 -> "hace $min min"
        min < 1440 -> "hace ${min / 60} h"
        else -> "hace ${min / 1440} d"
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrowRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(rotationAngle)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun UsageBar(percent: Float, color: Color = AccentBlue) {
    val animatedProgress by animateFloatAsState(targetValue = percent / 100f, label = "progress")
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("${String.format(java.util.Locale.US, "%.1f", percent)}%", color = Color.Gray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF333333))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
            Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.6f))
        }
    }
}

@Composable
fun CopyableInfoRow(label: String, value: String, context: Context) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
            Row(modifier = Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                        Toast.makeText(context, "$label copiado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copiar $label",
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CardContainer(color: Color = CardBg, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

@Composable
fun CpuCard(info: CpuInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            if (info.socName.isNotBlank()) {
                Text(info.socName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (info.socManufacturer.isNotBlank()) InfoRow("Fabricante SoC", info.socManufacturer)
            if (info.cpuConfig.isNotBlank()) InfoRow("Configuracion", info.cpuConfig)
            InfoRow("Nucleos", info.processorCount.toString())
            if (info.gpu.isNotBlank()) InfoRow("GPU", info.gpu)
            if (info.hardware.isNotBlank()) InfoRow("Hardware", info.hardware)
            if (info.architecture.isNotBlank()) InfoRow("Arquitectura", info.architecture)
            else if (info.cpuAbi.isNotBlank()) InfoRow("Arquitectura", info.cpuAbi)
            if (info.processor.isNotBlank()) InfoRow("Procesador", info.processor)
            if (info.bogoMips.isNotBlank()) InfoRow("BogoMIPS", info.bogoMips)
        }
    }
}

@Composable
fun MemoryCard(info: MemoryInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(info.totalRamFormatted, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("RAM Total", color = Color.Gray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(info.availableRamFormatted, color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Disponible", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            UsageBar(info.usagePercent, when {
                info.usagePercent > 85 -> AccentRed
                info.usagePercent > 60 -> AccentAmber
                else -> AccentGreen
            })
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Libre (real)", info.freeRamFormatted)
            InfoRow("Cache", info.cachedFormatted)
            if (info.totalSwapBytes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow("Swap Total", info.totalSwapFormatted)
                InfoRow("Swap Libre", info.freeSwapFormatted)
            }
        }
    }
}

@Composable
fun BatteryCard(info: BatteryInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("${info.level}%", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Bateria", color = Color.Gray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(info.status, color = when (info.status) {
                        "Cargando" -> AccentGreen
                        "Completo" -> AccentCyan
                        else -> Color.White
                    }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            UsageBar(info.levelPercent, when {
                info.levelPercent > 60 -> AccentGreen
                info.levelPercent > 20 -> AccentAmber
                else -> AccentRed
            })
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Salud", info.health)
            InfoRow("Tecnologia", info.technology)
            InfoRow("Temperatura", "${info.temperature}")
            InfoRow("Voltaje", info.voltage)
            InfoRow("Cargador", info.plugged)
        }
    }
}

@Composable
fun DisplayCard(info: DisplayInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            if (info.resolution.isNotBlank()) InfoRow("Resolucion", info.resolution)
            if (info.density.isNotBlank()) InfoRow("Densidad", info.density)
            if (info.refreshRate.isNotBlank()) InfoRow("Refresh Rate", info.refreshRate)
        }
    }
}

@Composable
fun StorageCard(info: FileSystemInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(info.mountPoint, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(info.filesystem, color = Color.Gray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(info.sizeFormatted, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            UsageBar(info.usagePercent, when {
                info.usagePercent > 90 -> AccentRed
                info.usagePercent > 75 -> AccentAmber
                else -> AccentBlue
            })
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Usado: ${info.usedFormatted}", color = Color.Gray, fontSize = 11.sp)
                Text("Libre: ${info.availableFormatted}", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun GeneralCard(info: GeneralInfo) {
    val context = LocalContext.current
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Fabricante", info.manufacturer)
            InfoRow("Modelo", info.model)
            if (info.marketName.isNotBlank()) InfoRow("Nombre comercial", info.marketName)
            InfoRow("Dispositivo", info.device)
            if (info.board.isNotBlank()) InfoRow("Board", info.board)
            if (info.hardware.isNotBlank()) InfoRow("Hardware", info.hardware)
            InfoRow("Android", info.androidVersion)
            InfoRow("SDK", info.sdkVersion)
            InfoRow("Serial", info.serialNumber)
            for ((index, imei) in info.imeis.withIndex()) {
                if (imei.isNotBlank()) {
                    val label = if (info.imeis.size > 1) "IMEI ${index + 1}" else "IMEI"
                    CopyableInfoRow(label, imei, context)
                }
            }
            if (info.phone.isNotBlank()) InfoRow("Baseband", info.phone)
            InfoRow("Fingerprint", info.fingerprint.take(50))
        }
    }
}

@Composable
fun CameraCard(info: CameraInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = buildString {
                append("Camera ${info.id}")
                if (info.facing.isNotBlank()) append(" - ${info.facing}")
            }
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            if (info.megapixels.isNotBlank()) InfoRow("Resolucion", info.megapixels)
            if (info.resolution.isNotBlank()) InfoRow("Tamanos", info.resolution)
            if (info.focalLength.isNotBlank()) InfoRow("Distancia focal", info.focalLength)
            if (info.flash.isNotBlank()) InfoRow("Flash", info.flash)
        }
    }
}

@Composable
fun NetworkCard(info: NetworkInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            if (info.wifiInterface.isNotBlank()) InfoRow("WiFi", info.wifiInterface)
            for (iface in info.interfaces) {
                InfoRow(iface.name, iface.ipAddress)
                if (iface.macAddress.isNotBlank()) {
                    InfoRow("   MAC", iface.macAddress)
                }
            }
        }
    }
}

@Composable
fun BuildCard(info: BuildInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Brand", info.brand)
            InfoRow("Modelo", info.model)
            InfoRow("Board", info.board)
            if (info.bootloader.isNotBlank()) InfoRow("Bootloader", info.bootloader)
            if (info.device.isNotBlank()) InfoRow("Device", info.device)
            if (info.manufacturer.isNotBlank()) InfoRow("Fabricante", info.manufacturer)
            if (info.product.isNotBlank()) InfoRow("Producto", info.product)
            InfoRow("Display", info.display)
            InfoRow("ID", info.id)
            InfoRow("Tags", info.tags)
            InfoRow("Type", info.type)
            InfoRow("Baseband", info.baseband)
            if (info.host.isNotBlank()) InfoRow("Host", info.host)
            if (info.fingerprint.isNotBlank()) InfoRow("Fingerprint", info.fingerprint.take(50))
            InfoRow("Kernel", info.kernel.take(50))
        }
    }
}
