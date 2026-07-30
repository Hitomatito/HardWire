package com.hitomatito.hardwire.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hitomatito.hardwire.data.model.ConnectionState
import com.hitomatito.hardwire.data.model.DeviceType
import com.hitomatito.hardwire.data.model.ManagedDevice
import com.hitomatito.hardwire.ui.components.DeviceDrawerContent
import com.hitomatito.hardwire.ui.components.HardwireTopBar
import com.hitomatito.hardwire.ui.screens.ComparisonScreen
import com.hitomatito.hardwire.ui.screens.ConnectionScreen
import com.hitomatito.hardwire.ui.screens.InfoScreen
import com.hitomatito.hardwire.ui.theme.HardwirePrimary
import com.hitomatito.hardwire.ui.theme.HardwireTheme
import com.hitomatito.hardwire.ui.permissions.PermissionsScreen
import com.hitomatito.hardwire.ui.permissions.allPermissionsGranted
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HardwireTheme {
                HardwireApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HardwireApp(viewModel: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionsGranted by remember { mutableStateOf(allPermissionsGranted(context)) }

    if (!permissionsGranted) {
        PermissionsScreen(onAllGranted = { permissionsGranted = true })
        return
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val devices by viewModel.devices.collectAsState()
    val activeId by viewModel.activeId.collectAsState()
    val activeState by viewModel.activeState.collectAsState()
    val activeInfo by viewModel.activeInfo.collectAsState()
    val activeMode by viewModel.activeMode.collectAsState()
    val activeIp by viewModel.activeIp.collectAsState()
    val showHub by viewModel.showHub.collectAsState()
    val scanResults by viewModel.scanResults.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val stateMap by viewModel.states.collectAsState()
    val onlineStatus by viewModel.onlineStatus.collectAsState()
    val activeUpdatedAt by viewModel.activeUpdatedAt.collectAsState()
    val usbState by viewModel.usbState.collectAsState()
    val message by viewModel.message.collectAsState()
    val showComparison by viewModel.showComparison.collectAsState()
    val comparisonDev1 by viewModel.comparisonDevice1.collectAsState()
    val comparisonDev2 by viewModel.comparisonDevice2.collectAsState()
    val comparisonInfo1 by viewModel.comparisonInfo1.collectAsState()
    val comparisonInfo2 by viewModel.comparisonInfo2.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val openDrawer = {
        scope.launch { drawerState.open() }
        Unit
    }

    var renameTarget by remember { mutableStateOf<ManagedDevice?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showCompareDialog by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DeviceDrawerContent(
                devices = devices,
                activeId = activeId,
                stateMap = stateMap,
                online = onlineStatus,
                onSelect = { id ->
                    viewModel.selectDevice(id)
                    scope.launch { drawerState.close() }
                },
                onAdd = {
                    viewModel.showAddScreen()
                    scope.launch { drawerState.close() }
                },
                onBackToHub = {
                    viewModel.showAddScreen()
                    scope.launch { drawerState.close() }
                },
                onDisconnect = { viewModel.disconnectDevice(it) },
                onRemove = { viewModel.removeDevice(it) },
                onRename = { device ->
                    renameTarget = device
                    renameText = device.name
                },
                onCompare = {
                    showCompareDialog = true
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                if (showHub) {
                    HardwireTopBar(
                        onMenuClick = openDrawer,
                        subtitle = "Agregar dispositivo"
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->

            LaunchedEffect(message) {
                message?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearMessage()
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (showComparison && comparisonDev1 != null && comparisonDev2 != null &&
                    comparisonInfo1 != null && comparisonInfo2 != null
                ) {
                    ComparisonScreen(
                        device1 = comparisonDev1!!,
                        info1 = comparisonInfo1!!,
                        device2 = comparisonDev2!!,
                        info2 = comparisonInfo2!!,
                        onBack = { viewModel.exitComparison() }
                    )
                } else if (showHub) {
                    val usbDeviceName = devices.find { it.id == "usb" }?.name ?: ""
                    ConnectionScreen(
                        connectionState = usbState,
                        deviceName = usbDeviceName,
                        isScanning = isScanning,
                        scanResults = scanResults,
                        onConnectClick = { viewModel.connect() },
                        onScanClick = { viewModel.scanNetwork() },
                        onDeviceClick = { viewModel.addNetworkDevice(it) },
                        onAddManual = { viewModel.addNetworkDevice(it) }
                    )
                } else {
                    val isLoading = activeState is ConnectionState.Connecting ||
                            activeState is ConnectionState.ConnectingWifi ||
                            activeState is ConnectionState.GatheringData
                    val errorMessage = when (activeState) {
                        is ConnectionState.Error -> (activeState as ConnectionState.Error).message
                        is ConnectionState.Disconnected -> "Dispositivo desconectado"
                        else -> null
                    }
                    InfoScreen(
                        deviceInfo = activeInfo ?: com.hitomatito.hardwire.data.model.DeviceInfo(),
                        isLoading = isLoading,
                        connectionMode = activeMode,
                        deviceIp = activeIp,
                        isOffline = activeState !is ConnectionState.Connected,
                        lastUpdated = activeUpdatedAt,
                        onRefresh = { viewModel.refresh() },
                        onSwitchToWifi = { viewModel.switchToWifi() },
                        errorMessage = errorMessage,
                        onMenuClick = openDrawer
                    )
                }
            }
        }
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renombrar dispositivo") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameTarget?.let { viewModel.renameDevice(it.id, renameText.trim().ifBlank { it.name }) }
                    renameTarget = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancelar") }
            }
        )
    }

    if (showCompareDialog) {
        var selected1 by remember { mutableStateOf<String?>(null) }
        var selected2 by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showCompareDialog = false },
            title = { Text("Seleccionar dispositivos") },
            text = {
                Column {
                    Text("Dispositivo 1:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    devices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selected1 = if (selected1 == device.id) null else device.id
                                }
                                .background(
                                    if (selected1 == device.id) HardwirePrimary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected1 == device.id,
                                onClick = { selected1 = device.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(device.name, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Dispositivo 2:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    devices.filter { it.id != selected1 }.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selected2 = if (selected2 == device.id) null else device.id
                                }
                                .background(
                                    if (selected2 == device.id) HardwirePrimary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected2 == device.id,
                                onClick = { selected2 = device.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(device.name, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selected1 != null && selected2 != null) {
                            viewModel.startComparison(selected1!!, selected2!!)
                            showCompareDialog = false
                        }
                    },
                    enabled = selected1 != null && selected2 != null
                ) { Text("Comparar") }
            },
            dismissButton = {
                TextButton(onClick = { showCompareDialog = false }) { Text("Cancelar") }
            }
        )
    }
}


