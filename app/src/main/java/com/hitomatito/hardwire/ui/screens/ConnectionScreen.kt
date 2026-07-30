package com.hitomatito.hardwire.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hitomatito.hardwire.R
import com.hitomatito.hardwire.data.model.ConnectionState
import com.hitomatito.hardwire.ui.theme.StatusConnected
import com.hitomatito.hardwire.ui.theme.StatusConnecting
import com.hitomatito.hardwire.ui.theme.StatusDisconnected

@Composable
fun ConnectionScreen(
    connectionState: ConnectionState,
    deviceName: String,
    isScanning: Boolean = false,
    scanResults: List<String> = emptyList(),
    onConnectClick: () -> Unit,
    onScanClick: () -> Unit = {},
    onDeviceClick: (String) -> Unit = {},
    onAddManual: (String) -> Unit = {}
) {
    var manualIp by remember { mutableStateOf("") }

    val isBusy = connectionState is ConnectionState.Connecting ||
            connectionState is ConnectionState.ConnectingWifi ||
            connectionState is ConnectionState.GatheringData ||
            connectionState is ConnectionState.Detecting ||
            connectionState is ConnectionState.RequestingPermission ||
            connectionState is ConnectionState.Scanning

    val isConnected = connectionState is ConnectionState.Connected

    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> StatusConnected
            is ConnectionState.Error -> StatusDisconnected
            else -> StatusConnecting
        },
        animationSpec = tween(400),
        label = "statusColor"
    )

    val statusIcon = when (connectionState) {
        is ConnectionState.Connected -> Icons.Filled.CheckCircle
        is ConnectionState.Error -> Icons.Filled.ErrorOutline
        is ConnectionState.Connecting,
        is ConnectionState.ConnectingWifi,
        is ConnectionState.GatheringData,
        is ConnectionState.Detecting,
        is ConnectionState.RequestingPermission,
        is ConnectionState.Scanning -> Icons.Filled.Sync
        else -> Icons.Filled.Usb
    }

    val statusTitle = when (connectionState) {
        is ConnectionState.Connected -> stringResource(R.string.status_connected)
        is ConnectionState.Connecting -> stringResource(R.string.status_connecting_usb)
        is ConnectionState.ConnectingWifi -> stringResource(R.string.status_connecting_wifi)
        is ConnectionState.GatheringData -> stringResource(R.string.status_gathering_data)
        is ConnectionState.Detecting -> stringResource(R.string.status_detecting)
        is ConnectionState.RequestingPermission -> stringResource(R.string.status_requesting_permission)
        is ConnectionState.Scanning -> stringResource(R.string.status_scanning)
        is ConnectionState.Error -> stringResource(R.string.status_error)
        else -> stringResource(R.string.status_no_connection)
    }

    val statusSubtitle = when (connectionState) {
        is ConnectionState.Connected -> deviceName.ifBlank { stringResource(R.string.device_ready) }
        is ConnectionState.Error -> connectionState.message
        is ConnectionState.RequestingPermission -> stringResource(R.string.accept_adb_invitation)
        else -> null
    }

    // ── Animations ─────────────────────────────────────────────
    val infinite = rememberInfiniteTransition(label = "spin")
    val iconRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (isBusy) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    // Pulse animation for status indicator when busy
    val pulseScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (isBusy) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ── Brand Header ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "HARDWIRE",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = MaterialTheme.typography.headlineMedium.letterSpacing
        )

        Text(
            text = stringResource(R.string.app_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Status Card ──────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        statusIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(26.dp)
                            .then(if (isBusy) Modifier.rotate(iconRotation) else Modifier),
                        tint = statusColor
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        statusTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!statusSubtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            statusSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }

        // ── Progress Bar ─────────────────────────────────────────
        AnimatedVisibility(
            visible = isBusy,
            enter = fadeIn() + slideInVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Connection Card ──────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.connect_device),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onConnectClick,
                        enabled = !isBusy && !isConnected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Usb, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                isConnected -> stringResource(R.string.status_connected)
                                isBusy -> stringResource(R.string.connecting)
                                else -> stringResource(R.string.connect_usb)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onScanClick,
                        enabled = !isBusy && !isConnected && !isScanning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isScanning) stringResource(R.string.scanning_network) else stringResource(R.string.scan_network),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Manual IP Card ───────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(400, delayMillis = 100))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.add_by_ip),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            placeholder = { Text(stringResource(R.string.ip_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val ip = manualIp.trim()
                                if (ip.isNotBlank()) {
                                    onAddManual(ip)
                                    manualIp = ""
                                }
                            },
                            enabled = manualIp.isNotBlank() && !isBusy && !isScanning,
                            modifier = Modifier.height(52.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // ── Scan Results ─────────────────────────────────────────
        AnimatedVisibility(
            visible = isScanning || scanResults.isNotEmpty(),
            enter = fadeIn(tween(300)) + slideInVertically(tween(300))
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            if (isScanning && scanResults.isEmpty())
                                stringResource(R.string.searching_adb_devices)
                            else
                                stringResource(R.string.devices_found, scanResults.size),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (scanResults.isEmpty() && isScanning) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        scanResults.forEach { ip ->
                            Surface(
                                onClick = { onDeviceClick(ip) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Wifi,
                                        contentDescription = null,
                                        tint = StatusConnected,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            ip,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            stringResource(R.string.port_5555_tap),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── How To Card ──────────────────────────────────────────
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400, delayMillis = 200)) + slideInVertically(tween(400, delayMillis = 200))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.how_to_start),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val steps = listOf(
                        stringResource(R.string.step_enable_debugging),
                        stringResource(R.string.step_connect_usb),
                        stringResource(R.string.step_use_wifi)
                    )
                    steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
