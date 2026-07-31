package com.hitomatito.hardwire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hitomatito.hardwire.R
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class HistorySnapshot(
    val timestamp: Long,
    val info: DeviceInfo
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    deviceName: String,
    snapshots: List<HistorySnapshot>,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deviceName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (snapshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.no_history),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(snapshots) { snapshot ->
                    HistoryCard(snapshot)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(snapshot: HistorySnapshot) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(snapshot.timestamp) { dateFormat.format(Date(snapshot.timestamp)) }
    val info = snapshot.info

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Timestamp header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (info.battery.level.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.BatteryStd,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AccentGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${info.battery.level}%",
                            fontSize = 12.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Key metrics in a grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // RAM
                if (info.memory.totalRamFormatted.isNotBlank()) {
                    MetricChip(
                        label = stringResource(R.string.label_ram_total),
                        value = "${info.memory.availableRamFormatted} / ${info.memory.totalRamFormatted}",
                        modifier = Modifier.weight(1f)
                    )
                }
                // Battery temp
                if (info.battery.temperature.isNotBlank()) {
                    MetricChip(
                        label = "Temp",
                        value = info.battery.temperature,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (info.memory.totalRamFormatted.isNotBlank() || info.battery.temperature.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Battery status
                if (info.battery.status.isNotBlank()) {
                    MetricChip(
                        label = stringResource(R.string.label_status_state),
                        value = info.battery.status,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Storage
                val mainFs = info.storage.filesystems.firstOrNull { it.mountPoint == "/data" || it.mountPoint == "/" }
                if (mainFs != null && mainFs.sizeFormatted.isNotBlank()) {
                    MetricChip(
                        label = stringResource(R.string.label_storage_section),
                        value = "${mainFs.usedFormatted} / ${mainFs.sizeFormatted}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
