package com.hitomatito.hardwire.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.hitomatito.hardwire.R
import com.hitomatito.hardwire.data.model.ConnectionState
import com.hitomatito.hardwire.data.model.DeviceType
import com.hitomatito.hardwire.data.model.ManagedDevice
import com.hitomatito.hardwire.ui.theme.StatusConnected
import com.hitomatito.hardwire.ui.theme.StatusConnecting
import com.hitomatito.hardwire.ui.theme.StatusDisconnected
import com.hitomatito.hardwire.ui.theme.StatusOnline
import com.hitomatito.hardwire.ui.theme.ThemeMode

@Composable
fun DeviceDrawerContent(
    devices: List<ManagedDevice>,
    activeId: String?,
    stateMap: Map<String, ConnectionState>,
    online: Map<String, Boolean> = emptyMap(),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onBackToHub: () -> Unit,
    onDisconnect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onRename: (ManagedDevice) -> Unit,
    onCompare: () -> Unit = {}
) {
    var menuOpenId by remember { mutableStateOf<String?>(null) }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 360.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBackToHub() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "HARDWIRE",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        stringResource(R.string.drawer_devices),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.DeviceUnknown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.drawer_no_devices),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Text(
                            stringResource(R.string.drawer_add_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(devices, key = { it.id }) { device ->
                        val state = stateMap[device.id] ?: ConnectionState.Disconnected
                        DeviceDrawerItem(
                            device = device,
                            state = state,
                            isOnline = online[device.id] == true,
                            isActive = device.id == activeId,
                            menuOpen = menuOpenId == device.id,
                            isLocal = device.type == DeviceType.LOCAL,
                            onSelect = { onSelect(device.id) },
                            onToggleMenu = {
                                menuOpenId = if (menuOpenId == device.id) null else device.id
                            },
                            onDismissMenu = { menuOpenId = null },
                            onDisconnect = {
                                onDisconnect(device.id)
                                menuOpenId = null
                            },
                            onRename = {
                                onRename(device)
                                menuOpenId = null
                            },
                            onRemove = {
                                onRemove(device.id)
                                menuOpenId = null
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (devices.size >= 2) {
                OutlinedButton(
                    onClick = onCompare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.compare_devices), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_device), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Theme toggle
            var themeMenuOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { themeMenuOpen = true }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.theme_label),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        when (themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = themeMenuOpen,
                    onDismissRequest = { themeMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_system)) },
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM); themeMenuOpen = false },
                        leadingIcon = { if (themeMode == ThemeMode.SYSTEM) Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) else Spacer(modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_light)) },
                        onClick = { onThemeModeChange(ThemeMode.LIGHT); themeMenuOpen = false },
                        leadingIcon = { if (themeMode == ThemeMode.LIGHT) Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) else Spacer(modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_dark)) },
                        onClick = { onThemeModeChange(ThemeMode.DARK); themeMenuOpen = false },
                        leadingIcon = { if (themeMode == ThemeMode.DARK) Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) else Spacer(modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceDrawerItem(
    device: ManagedDevice,
    state: ConnectionState,
    isOnline: Boolean,
    isActive: Boolean,
    menuOpen: Boolean,
    isLocal: Boolean = false,
    onSelect: () -> Unit,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onDisconnect: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit
) {
    val dotColor = when {
        state is ConnectionState.Connected -> StatusConnected
        state is ConnectionState.Connecting ||
                state is ConnectionState.ConnectingWifi -> StatusConnecting
        state is ConnectionState.Error -> StatusDisconnected
        isOnline -> StatusConnecting
        else -> Color(0xFF555555)
    }
    val sub = when (device.type) {
        DeviceType.USB -> "USB"
        DeviceType.NETWORK -> "${device.host}:${device.port}"
        DeviceType.LOCAL -> stringResource(R.string.local_device_subtitle)
    }

    NavigationDrawerItem(
        selected = isActive,
        onClick = onSelect,
        icon = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        },
        label = {
            Column {
                Text(
                    device.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    sub,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        badge = {
            Box {
                IconButton(onClick = onToggleMenu) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.device_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = onDismissMenu
                ) {
                    if (!isLocal) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.LinkOff, contentDescription = null) },
                            text = { Text(stringResource(R.string.disconnect)) },
                            onClick = onDisconnect
                        )
                    }
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = onRename
                    )
                    if (!isLocal) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = onRemove
                        )
                    }
                }
            }
        },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
