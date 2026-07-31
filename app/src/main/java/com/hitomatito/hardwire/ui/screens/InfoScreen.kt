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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hitomatito.hardwire.R
import com.hitomatito.hardwire.data.model.*
import com.hitomatito.hardwire.ui.components.HardwireTopBar
import com.hitomatito.hardwire.ui.theme.AccentBlue
import com.hitomatito.hardwire.ui.theme.AccentGreen
import com.hitomatito.hardwire.ui.theme.AccentAmber
import com.hitomatito.hardwire.ui.theme.AccentRed
import com.hitomatito.hardwire.ui.theme.AccentCyan
import kotlinx.coroutines.delay

private data class Section(val id: String, val titleResId: Int, val icon: ImageVector)

private val sections = listOf(
    Section("general", R.string.section_general, Icons.Filled.Info),
    Section("cpu", R.string.section_cpu, Icons.Filled.Memory),
    Section("memory", R.string.section_memory, Icons.Filled.Storage),
    Section("battery", R.string.section_battery, Icons.Filled.BatteryStd),
    Section("display", R.string.section_display, Icons.Filled.PhoneAndroid),
    Section("storage", R.string.section_storage, Icons.Filled.SdStorage),
    Section("cameras", R.string.section_cameras, Icons.Filled.CameraAlt),
    Section("sensors", R.string.section_sensors, Icons.Filled.Sensors),
    Section("network", R.string.section_network, Icons.Filled.Wifi),
    Section("system", R.string.section_system, Icons.Filled.Code)
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
    onShare: (() -> Unit)? = null,
    onCopyReport: (() -> Unit)? = null,
    onViewHistory: (() -> Unit)? = null,
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
                    if (onViewHistory != null) {
                        IconButton(onClick = onViewHistory) {
                            Icon(Icons.Filled.History, stringResource(R.string.theme_label))
                        }
                    }
                    if (onShare != null) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Filled.Share, stringResource(R.string.share_report))
                        }
                    }
                    if (onCopyReport != null) {
                        IconButton(onClick = onCopyReport) {
                            Icon(Icons.Filled.ContentCopy, stringResource(R.string.copy_to_clipboard))
                        }
                    }
                    if (connectionMode == "USB") {
                        IconButton(onClick = onSwitchToWifi) {
                            Icon(Icons.Filled.Wifi, stringResource(R.string.switch_to_wifi))
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
                            if (isSwitchingToWifi) stringResource(R.string.loading_switching_wifi)
                            else stringResource(R.string.loading_device_info),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        if (connectionMode == "WiFi") stringResource(R.string.wifi_active) else stringResource(R.string.network_active),
                                        color = AccentGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (connectionMode == "WiFi")
                                            stringResource(R.string.disconnect_usb_hint)
                                        else
                                            stringResource(R.string.connected_via_network, deviceIp),
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
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = AccentRed, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.connection_error), color = AccentRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(errorMessage ?: "", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                sections.forEach { section ->
                    item {
                        CollapsibleSection(
                            title = stringResource(section.titleResId),
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
                                        Text(stringResource(R.string.no_data), color = Color.Gray, fontSize = 13.sp)
                                    }
                                }
                                "sensors" -> {
                                    if (deviceInfo.sensors.isNotEmpty()) {
                                        deviceInfo.sensors.take(10).forEach { sensor ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(sensor.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                                    Text(sensor.vendor, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                                    if (sensor.resolution.isNotBlank() || sensor.power.isNotBlank()) {
                                                        val detail = listOfNotNull(
                                                            sensor.resolution.takeIf { it.isNotBlank() },
                                                            sensor.power.takeIf { it.isNotBlank() }
                                                        ).joinToString(" | ")
                                                        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    if (sensor.maxRate.isNotBlank()) {
                                                        Text(sensor.maxRate, color = AccentGreen, fontSize = 11.sp)
                                                    }
                                                    if (sensor.wakeUp) {
                                                        Text("WakeUp", color = AccentAmber, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                        if (deviceInfo.sensors.size > 10) {
                                            Text(
                                                stringResource(R.string.more_sensors, deviceInfo.sensors.size - 10),
                                                color = AccentBlue, fontSize = 12.sp
                                            )
                                        }
                                    } else {
                                        Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
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
    val relativeTime = formatRelativeTime(lastUpdated)
    CardContainer(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.offline_banner_title),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.offline_banner_detail, relativeTime),
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun formatRelativeTime(updatedAt: Long): String {
    if (updatedAt <= 0L) return stringResource(R.string.unknown_date)
    val diff = System.currentTimeMillis() - updatedAt
    val min = diff / 60000
    return when {
        min < 1 -> stringResource(R.string.time_moment)
        min < 60 -> stringResource(R.string.time_minutes, min.toInt())
        min < 1440 -> stringResource(R.string.time_hours, (min / 60).toInt())
        else -> stringResource(R.string.time_days, (min / 1440).toInt())
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large
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
                    contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
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
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
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
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.6f))
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
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.weight(0.4f))
            Row(modifier = Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    value,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                        Toast.makeText(context, context.getString(R.string.copied_label, label), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.copy_label, label),
                        tint = AccentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CardContainer(color: Color = MaterialTheme.colorScheme.surfaceContainerLow, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.large,
        content = content
    )
}

@Composable
fun CpuCard(info: CpuInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            if (info.socName.isNotBlank()) {
                Text(info.socName, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (info.socManufacturer.isNotBlank()) InfoRow(stringResource(R.string.label_soc_manufacturer), info.socManufacturer)
            if (info.cpuConfig.isNotBlank()) InfoRow(stringResource(R.string.label_config), info.cpuConfig)
            InfoRow(stringResource(R.string.label_cores), info.processorCount.toString())
            if (info.maxFrequency.isNotBlank()) InfoRow(stringResource(R.string.label_max_freq), info.maxFrequency)
            if (info.minFrequency.isNotBlank()) InfoRow(stringResource(R.string.label_min_freq), info.minFrequency)
            if (info.gpu.isNotBlank()) InfoRow(stringResource(R.string.label_gpu), info.gpu)
            if (info.hardware.isNotBlank()) InfoRow(stringResource(R.string.label_hardware), info.hardware)
            if (info.architecture.isNotBlank()) InfoRow(stringResource(R.string.label_architecture), info.architecture)
            else if (info.cpuAbi.isNotBlank()) InfoRow(stringResource(R.string.label_architecture), info.cpuAbi)
            if (info.processor.isNotBlank()) InfoRow(stringResource(R.string.label_processor), info.processor)
            if (info.bogoMips.isNotBlank()) InfoRow(stringResource(R.string.label_bogomips), info.bogoMips)
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
                    Text(info.totalRamFormatted, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.label_ram_total), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(info.availableRamFormatted, color = AccentGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.label_available), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            UsageBar(info.usagePercent, when {
                info.usagePercent > 85 -> AccentRed
                info.usagePercent > 60 -> AccentAmber
                else -> AccentGreen
            })
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(stringResource(R.string.label_free_real), info.freeRamFormatted)
            InfoRow(stringResource(R.string.label_cache), info.cachedFormatted)
            if (info.totalSwapBytes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                InfoRow(stringResource(R.string.label_swap_total), info.totalSwapFormatted)
                InfoRow(stringResource(R.string.label_swap_free), info.freeSwapFormatted)
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
                    Text("${info.level}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.label_battery), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
            InfoRow(stringResource(R.string.label_health), info.health)
            InfoRow(stringResource(R.string.label_technology), info.technology)
            InfoRow(stringResource(R.string.label_temperature), "${info.temperature}")
            InfoRow(stringResource(R.string.label_voltage), info.voltage)
            InfoRow(stringResource(R.string.label_plugged), info.plugged)
        }
    }
}

@Composable
fun DisplayCard(info: DisplayInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            if (info.resolution.isNotBlank()) InfoRow(stringResource(R.string.label_resolution), info.resolution)
            if (info.physicalSize.isNotBlank()) InfoRow(stringResource(R.string.label_physical_size), info.physicalSize)
            if (info.density.isNotBlank()) InfoRow(stringResource(R.string.label_density), info.density)
            if (info.refreshRate.isNotBlank()) InfoRow(stringResource(R.string.label_refresh_rate), info.refreshRate)
            if (info.colorMode.isNotBlank()) InfoRow(stringResource(R.string.label_color_mode), info.colorMode)
            if (info.hdrSupport.isNotBlank()) InfoRow(stringResource(R.string.label_hdr), info.hdrSupport)
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
                    Text(info.mountPoint, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(info.filesystem, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(info.sizeFormatted, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                Text(stringResource(R.string.label_used, info.usedFormatted), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(stringResource(R.string.label_free, info.availableFormatted), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun GeneralCard(info: GeneralInfo) {
    val context = LocalContext.current
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(stringResource(R.string.label_manufacturer), info.manufacturer)
            InfoRow(stringResource(R.string.label_model), info.model)
            if (info.marketName.isNotBlank()) InfoRow(stringResource(R.string.label_market_name), info.marketName)
            if (info.customOs.isNotBlank()) InfoRow(stringResource(R.string.label_custom_os), info.customOs)
            InfoRow(stringResource(R.string.label_device), info.device)
            if (info.board.isNotBlank()) InfoRow(stringResource(R.string.label_board), info.board)
            if (info.hardware.isNotBlank()) InfoRow(stringResource(R.string.label_hardware), info.hardware)
            InfoRow(stringResource(R.string.label_android), info.androidVersion)
            if (info.securityPatch.isNotBlank()) InfoRow(stringResource(R.string.label_security_patch), info.securityPatch)
            if (info.baseOs.isNotBlank()) InfoRow(stringResource(R.string.label_base_os), info.baseOs)
            InfoRow(stringResource(R.string.label_sdk), info.sdkVersion)
            InfoRow(stringResource(R.string.label_serial), info.serialNumber)
            for ((index, imei) in info.imeis.withIndex()) {
                if (imei.isNotBlank()) {
                    val label = if (info.imeis.size > 1) stringResource(R.string.label_imei_number, index + 1) else stringResource(R.string.label_imei)
                    CopyableInfoRow(label, imei, context)
                }
            }
            if (info.phone.isNotBlank()) InfoRow(stringResource(R.string.label_baseband), info.phone)
            InfoRow(stringResource(R.string.label_fingerprint), info.fingerprint.take(50))
        }
    }
}

@Composable
fun CameraCard(info: CameraInfo) {
    CardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = buildString {
                append(stringResource(R.string.camera_id, info.id.toString()))
                if (info.facing.isNotBlank()) append(" - ${info.facing}")
            }
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            if (info.megapixels.isNotBlank()) InfoRow(stringResource(R.string.label_resolution), info.megapixels)
            if (info.resolution.isNotBlank()) InfoRow(stringResource(R.string.label_sizes), info.resolution)
            if (info.focalLength.isNotBlank()) InfoRow(stringResource(R.string.label_focal_length), info.focalLength)
            if (info.aperture.isNotBlank()) InfoRow(stringResource(R.string.label_aperture), info.aperture)
            if (info.sensorSize.isNotBlank()) InfoRow(stringResource(R.string.label_sensor_size), info.sensorSize)
            if (info.hardwareLevel.isNotBlank()) InfoRow(stringResource(R.string.label_hardware_level), info.hardwareLevel)
            if (info.flash.isNotBlank()) InfoRow(stringResource(R.string.label_flash), info.flash)
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
            InfoRow(stringResource(R.string.label_brand), info.brand)
            InfoRow(stringResource(R.string.label_model), info.model)
            InfoRow(stringResource(R.string.label_board), info.board)
            if (info.bootloader.isNotBlank()) InfoRow("Bootloader", info.bootloader)
            if (info.device.isNotBlank()) InfoRow(stringResource(R.string.label_device), info.device)
            if (info.manufacturer.isNotBlank()) InfoRow(stringResource(R.string.label_manufacturer), info.manufacturer)
            if (info.product.isNotBlank()) InfoRow(stringResource(R.string.label_product), info.product)
            InfoRow(stringResource(R.string.label_display_id), info.display)
            if (info.incremental.isNotBlank()) InfoRow(stringResource(R.string.label_incremental), info.incremental)
            InfoRow(stringResource(R.string.label_id), info.id)
            if (info.securityPatch.isNotBlank()) InfoRow(stringResource(R.string.label_security_patch), info.securityPatch)
            InfoRow(stringResource(R.string.label_tags), info.tags)
            InfoRow(stringResource(R.string.label_type), info.type)
            InfoRow(stringResource(R.string.label_baseband), info.baseband)
            if (info.host.isNotBlank()) InfoRow(stringResource(R.string.label_host), info.host)
            if (info.fingerprint.isNotBlank()) InfoRow(stringResource(R.string.label_fingerprint), info.fingerprint.take(50))
            InfoRow(stringResource(R.string.label_kernel), info.kernel.take(50))
        }
    }
}
