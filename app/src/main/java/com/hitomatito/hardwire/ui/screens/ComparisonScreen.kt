package com.hitomatito.hardwire.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hitomatito.hardwire.data.model.DeviceInfo
import com.hitomatito.hardwire.data.model.ManagedDevice

data class ComparisonField(
    val label: String,
    val device1Value: String,
    val device2Value: String,
    val isDifferent: Boolean = device1Value != device2Value
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    device1: ManagedDevice,
    info1: DeviceInfo,
    device2: ManagedDevice,
    info2: DeviceInfo,
    onBack: () -> Unit
) {
    val fields = remember(info1, info2) {
        listOf(
            ComparisonField("Fabricante", info1.general.manufacturer, info2.general.manufacturer),
            ComparisonField("Modelo", info1.general.model, info2.general.model),
            ComparisonField("Nombre Comercial", info1.general.marketName, info2.general.marketName),
            ComparisonField("SoC", info1.cpu.socName, info2.cpu.socName),
            ComparisonField("Fabricante SoC", info1.cpu.socManufacturer, info2.cpu.socManufacturer),
            ComparisonField("Arquitectura", info1.cpu.architecture, info2.cpu.architecture),
            ComparisonField("Nucleos", info1.cpu.processorCount.toString(), info2.cpu.processorCount.toString()),
            ComparisonField("RAM Total", info1.memory.totalRamFormatted, info2.memory.totalRamFormatted),
            ComparisonField("RAM Disponible", info1.memory.availableRamFormatted, info2.memory.availableRamFormatted),
            ComparisonField("Bateria", info1.battery.level, info2.battery.level),
            ComparisonField("Salud Bateria", info1.battery.health, info2.battery.health),
            ComparisonField("Tecnologia Bateria", info1.battery.technology, info2.battery.technology),
            ComparisonField("Resolucion", info1.display.resolution, info2.display.resolution),
            ComparisonField("Densidad", info1.display.density, info2.display.density),
            ComparisonField("Tasa Refresco", info1.display.refreshRate, info2.display.refreshRate),
            ComparisonField("Android", info1.general.androidVersion, info2.general.androidVersion),
            ComparisonField("SDK", info1.general.sdkVersion, info2.general.sdkVersion),
            ComparisonField("Camaras", info1.cameras.size.toString(), info2.cameras.size.toString()),
            ComparisonField("Sensores", info1.sensors.size.toString(), info2.sensors.size.toString()),
            ComparisonField("Interfaces Red", info1.network.interfaces.size.toString(), info2.network.interfaces.size.toString()),
            ComparisonField("Marca", info1.build.brand, info2.build.brand),
            ComparisonField("Bootloader", info1.build.bootloader, info2.build.bootloader),
            ComparisonField("Baseband", info1.build.baseband, info2.build.baseband)
        )
    }

    val differencesCount = fields.count { it.isDifferent }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comparar Dispositivos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device1.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${info1.general.manufacturer} ${info1.general.model}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = device2.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${info2.general.manufacturer} ${info2.general.model}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (differencesCount > 0) {
                Text(
                    text = "$differencesCount diferencias encontradas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(fields) { field ->
                    ComparisonRow(field)
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(field: ComparisonField) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (field.isDifferent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = field.device1Value.ifBlank { "-" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (field.isDifferent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = field.label,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = field.device2Value.ifBlank { "-" },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (field.isDifferent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
