package com.hitomatito.hardwire.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.hitomatito.hardwire.R
import com.hitomatito.hardwire.data.model.*
import com.hitomatito.hardwire.ui.theme.*

// ── Comparison Data Models ───────────────────────────────────

data class ComparisonSection(
    val title: String,
    val icon: ImageVector,
    val fields: List<ComparisonField>
)

data class ComparisonField(
    val label: String,
    val device1Value: String,
    val device2Value: String,
    val isDifferent: Boolean = device1Value != device2Value,
    val device1Wins: Boolean? = null, // null = neutral, true = d1 better, false = d2 better
    val showAsBar: Boolean = false, // for RAM, battery, etc.
    val barValue1: Float = 0f, // 0..100
    val barValue2: Float = 0f,
)

enum class Side(val label: String, val color: Color, val containerColor: Color) {
    LEFT("A", AccentBlue, AccentBlue.copy(alpha = 0.12f)),
    RIGHT("B", AccentGreen, AccentGreen.copy(alpha = 0.12f)),
    TIE("=", Color.Gray, Color.Gray.copy(alpha = 0.08f))
}

// ── Main Screen ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    device1: ManagedDevice,
    info1: DeviceInfo,
    device2: ManagedDevice,
    info2: DeviceInfo,
    onBack: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    var showDifferencesOnly by remember { mutableStateOf(false) }

    val sections = buildSections(info1, info2)

    val totalDifferences = sections.flatMap { it.fields }.count { it.isDifferent }
    val totalFields = sections.flatMap { it.fields }.size
    val matchPercent = if (totalFields > 0) ((totalFields - totalDifferences).toFloat() / totalFields * 100).toInt() else 0

    val displaySections = sections.map { section ->
        if (showDifferencesOnly) section.copy(fields = section.fields.filter { it.isDifferent })
        else section
    }.filter { it.fields.isNotEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.compare_title), style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.compare_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showDifferencesOnly = !showDifferencesOnly }) {
                        Icon(
                            if (showDifferencesOnly) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                            contentDescription = null,
                            tint = if (showDifferencesOnly) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Device Headers ──────────────────────────────────
            item {
                DeviceComparisonHeader(
                    device1 = device1, info1 = info1.general,
                    device2 = device2, info2 = info2.general,
                    matchPercent = matchPercent,
                    totalDifferences = totalDifferences,
                    totalFields = totalFields
                )
            }

            // ── Quick Stats Row ─────────────────────────────────
            item {
                QuickStatsRow(info1 = info1, info2 = info2)
            }

            // ── Section Navigation Chips ────────────────────────
            item {
                SectionChips(
                    sections = sections,
                    selectedIndex = selectedSection,
                    onSelect = { selectedSection = it }
                )
            }

            // ── Filter Chip ─────────────────────────────────────
            if (totalDifferences > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showDifferencesOnly,
                            onClick = { showDifferencesOnly = !showDifferencesOnly },
                            label = {
                                Text(
                                    if (showDifferencesOnly) stringResource(R.string.compare_show_all) else stringResource(R.string.compare_differences, totalDifferences),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (showDifferencesOnly) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            // ── Sections ────────────────────────────────────────
            displaySections.forEachIndexed { sectionIndex, section ->
                item {
                    ComparisonSectionHeader(
                        section = section,
                        isExpanded = sectionIndex == selectedSection || showDifferencesOnly,
                        onClick = { selectedSection = sectionIndex }
                    )
                }

                if (sectionIndex == selectedSection || showDifferencesOnly) {
                    itemsIndexed(section.fields) { fieldIndex, field ->
                        ComparisonFieldRow(
                            field = field,
                            animDelay = fieldIndex * 20
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── Bottom spacer ───────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ── Device Header ───────────────────────────────────────────

@Composable
private fun DeviceComparisonHeader(
    device1: ManagedDevice, info1: GeneralInfo,
    device2: ManagedDevice, info2: GeneralInfo,
    matchPercent: Int, totalDifferences: Int, totalFields: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device 1
        DeviceSideCard(
            modifier = Modifier.weight(1f),
            deviceName = device1.name,
            subtitle = "${info1.manufacturer} ${info1.model}".trim(),
            color = Side.LEFT.color,
            containerColor = Side.LEFT.containerColor
        )

        // Center badge
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .width(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$matchPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.compare_diff_short, totalDifferences),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Device 2
        DeviceSideCard(
            modifier = Modifier.weight(1f),
            deviceName = device2.name,
            subtitle = "${info2.manufacturer} ${info2.model}".trim(),
            color = Side.RIGHT.color,
            containerColor = Side.RIGHT.containerColor
        )
    }
}

@Composable
private fun DeviceSideCard(
    modifier: Modifier = Modifier,
    deviceName: String,
    subtitle: String,
    color: Color,
    containerColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Smartphone,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Quick Stats ─────────────────────────────────────────────

@Composable
private fun QuickStatsRow(info1: DeviceInfo, info2: DeviceInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickStatChip(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.label_ram_total),
            value1 = info1.memory.totalRamFormatted,
            value2 = info2.memory.totalRamFormatted
        )
        QuickStatChip(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.label_android),
            value1 = info1.general.androidVersion,
            value2 = info2.general.androidVersion
        )
        QuickStatChip(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.label_soc),
            value1 = info1.cpu.socName.ifBlank { "?" },
            value2 = info2.cpu.socName.ifBlank { "?" }
        )
    }
}

@Composable
private fun QuickStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value1: String,
    value2: String
) {
    val isDifferent = value1 != value2
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isDifferent)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value1,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "vs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = value2,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Section Chips ───────────────────────────────────────────

@Composable
private fun SectionChips(
    sections: List<ComparisonSection>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(sections) { index, section ->
            val isSelected = index == selectedIndex
            val diffCount = section.fields.count { it.isDifferent }

            AssistChip(
                onClick = { onSelect(index) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            section.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(section.title, style = MaterialTheme.typography.labelMedium)
                        if (diffCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text("$diffCount", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                },
                leadingIcon = null,
                border = null
            )
        }
    }
}

// ── Section Header ──────────────────────────────────────────

@Composable
private fun ComparisonSectionHeader(
    section: ComparisonSection,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val diffCount = section.fields.count { it.isDifferent }
    val totalCount = section.fields.size

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                section.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Diff badge
            if (diffCount > 0) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "$diffCount/$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.compare_equal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Field Row ───────────────────────────────────────────────

@Composable
private fun ComparisonFieldRow(
    field: ComparisonField,
    animDelay: Int = 0
) {
    val isDifferent = field.isDifferent
    val surfaceColor = if (isDifferent)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Label
        Text(
            text = field.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (field.showAsBar) {
            // ── Progress Bar Row ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Device 1 bar
                Column(modifier = Modifier.weight(1f)) {
                    ComparisonValueText(
                        value = field.device1Value,
                        isDifferent = isDifferent,
                        wins = field.device1Wins,
                        align = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ComparisonProgressBar(
                        value = field.barValue1,
                        color = if (field.device1Wins == true) Side.LEFT.color
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }
                // Device 2 bar
                Column(modifier = Modifier.weight(1f)) {
                    ComparisonValueText(
                        value = field.device2Value,
                        isDifferent = isDifferent,
                        wins = field.device2Wins(),
                        align = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ComparisonProgressBar(
                        value = field.barValue2,
                        color = if (field.device2Wins() == true) Side.RIGHT.color
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        } else {
            // ── Text Row ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device 1 value
                ComparisonValueText(
                    value = field.device1Value,
                    isDifferent = isDifferent,
                    wins = field.device1Wins,
                    align = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )

                // Winner indicator
                if (isDifferent) {
                    WinnerBadge(
                        winner = when {
                            field.device1Wins == true -> Side.LEFT
                            field.device2Wins() == true -> Side.RIGHT
                            else -> Side.TIE
                        }
                    )
                } else {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.compare_equal),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Device 2 value
                ComparisonValueText(
                    value = field.device2Value,
                    isDifferent = isDifferent,
                    wins = field.device2Wins(),
                    align = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}

@Composable
private fun ComparisonValueText(
    value: String,
    isDifferent: Boolean,
    wins: Boolean?,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    val displayValue = value.ifBlank { "-" }
    val textColor = when {
        wins == true -> Side.LEFT.color // winner always gets accent
        wins == false -> Side.RIGHT.color
        isDifferent -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = displayValue,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (wins != null) FontWeight.SemiBold else FontWeight.Normal,
        color = textColor,
        textAlign = align,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun ComparisonProgressBar(
    value: Float,
    color: Color
) {
    val animatedValue by animateFloatAsState(
        targetValue = value / 100f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bar"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = animatedValue)
                .clip(MaterialTheme.shapes.small)
                .background(color)
        )
    }
}

@Composable
private fun WinnerBadge(winner: Side) {
    val (icon, color, bg) = when (winner) {
        Side.LEFT -> Triple("◀", Side.LEFT.color, Side.LEFT.containerColor)
        Side.RIGHT -> Triple("▶", Side.RIGHT.color, Side.RIGHT.containerColor)
        Side.TIE -> Triple("=", Color.Gray, Color.Gray.copy(alpha = 0.1f))
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ── Helper Extension ────────────────────────────────────────

private fun ComparisonField.device2Wins(): Boolean? = when {
    device1Wins == null -> null
    device1Wins -> false
    else -> true
}

// ── Build Sections ──────────────────────────────────────────

@Composable
private fun buildSections(info1: DeviceInfo, info2: DeviceInfo): List<ComparisonSection> = listOf(
    // ── General ────────────────────────────────────────────
    ComparisonSection(
        title = stringResource(R.string.section_general),
        icon = Icons.Filled.Info,
        fields = listOf(
            ComparisonField(stringResource(R.string.label_manufacturer), info1.general.manufacturer, info2.general.manufacturer),
            ComparisonField(stringResource(R.string.label_model), info1.general.model, info2.general.model),
            ComparisonField(stringResource(R.string.label_market_name), info1.general.marketName, info2.general.marketName),
            ComparisonField(stringResource(R.string.label_device), info1.general.device, info2.general.device),
            ComparisonField(stringResource(R.string.label_serial), info1.general.serialNumber, info2.general.serialNumber),
        )
    ),

    // ── CPU / SoC ──────────────────────────────────────────
    ComparisonSection(
        title = stringResource(R.string.section_cpu),
        icon = Icons.Filled.Memory,
        fields = listOf(
            ComparisonField(stringResource(R.string.label_soc), info1.cpu.socName, info2.cpu.socName),
            ComparisonField(stringResource(R.string.label_soc_manufacturer), info1.cpu.socManufacturer, info2.cpu.socManufacturer),
            ComparisonField(stringResource(R.string.label_processor), info1.cpu.processor, info2.cpu.processor),
            ComparisonField(stringResource(R.string.label_cores), info1.cpu.processorCount.toString(), info2.cpu.processorCount.toString(),
                device1Wins = if (info1.cpu.processorCount != info2.cpu.processorCount)
                    info1.cpu.processorCount > info2.cpu.processorCount else null
            ),
            ComparisonField(stringResource(R.string.label_architecture), info1.cpu.architecture, info2.cpu.architecture),
            ComparisonField(stringResource(R.string.label_cpu_abi), info1.cpu.cpuAbi, info2.cpu.cpuAbi),
            ComparisonField(stringResource(R.string.label_gpu), info1.cpu.gpu, info2.cpu.gpu),
            ComparisonField(stringResource(R.string.label_bogomips), info1.cpu.bogoMips, info2.cpu.bogoMips),
            ComparisonField(stringResource(R.string.compare_cpu_config), info1.cpu.cpuConfig, info2.cpu.cpuConfig),
            ComparisonField(stringResource(R.string.label_hardware), info1.cpu.hardware, info2.cpu.hardware),
        )
    ),

    // ── Memory ─────────────────────────────────────────────
    ComparisonSection(
        title = stringResource(R.string.section_memory),
        icon = Icons.Filled.SdStorage,
        fields = listOf(
            ComparisonField(
                stringResource(R.string.label_ram_total), info1.memory.totalRamFormatted, info2.memory.totalRamFormatted,
                showAsBar = true,
                barValue1 = if (info1.memory.totalRamBytes > 0) 100f else 0f,
                barValue2 = if (info2.memory.totalRamBytes > 0) 100f else 0f,
                device1Wins = if (info1.memory.totalRamBytes != info2.memory.totalRamBytes)
                    info1.memory.totalRamBytes > info2.memory.totalRamBytes else null
            ),
            ComparisonField(stringResource(R.string.compare_ram_available), info1.memory.availableRamFormatted, info2.memory.availableRamFormatted),
            ComparisonField(stringResource(R.string.compare_ram_free), info1.memory.freeRamFormatted, info2.memory.freeRamFormatted),
            ComparisonField(stringResource(R.string.label_cache), info1.memory.cachedFormatted, info2.memory.cachedFormatted),
            ComparisonField(stringResource(R.string.compare_ram_usage), "${info1.memory.usagePercent.toInt()}%", "${info2.memory.usagePercent.toInt()}%",
                showAsBar = true,
                barValue1 = info1.memory.usagePercent,
                barValue2 = info2.memory.usagePercent,
                device1Wins = if (info1.memory.usagePercent != info2.memory.usagePercent)
                    info1.memory.usagePercent < info2.memory.usagePercent else null // less usage is better
            ),
            ComparisonField(stringResource(R.string.label_swap_total), info1.memory.totalSwapFormatted, info2.memory.totalSwapFormatted),
            ComparisonField(stringResource(R.string.label_swap_free), info1.memory.freeSwapFormatted, info2.memory.freeSwapFormatted),
        )
    ),

    // ── Battery ────────────────────────────────────────────
    ComparisonSection(
        title = stringResource(R.string.section_battery),
        icon = Icons.Filled.BatteryStd,
        fields = listOf(
            ComparisonField(stringResource(R.string.compare_battery_level), info1.battery.level, info2.battery.level,
                showAsBar = true,
                barValue1 = info1.battery.levelPercent,
                barValue2 = info2.battery.levelPercent,
                device1Wins = if (info1.battery.levelPercent != info2.battery.levelPercent)
                    info1.battery.levelPercent > info2.battery.levelPercent else null
            ),
            ComparisonField(stringResource(R.string.compare_battery_status), info1.battery.status, info2.battery.status),
            ComparisonField(stringResource(R.string.label_health), info1.battery.health, info2.battery.health),
            ComparisonField(stringResource(R.string.label_technology), info1.battery.technology, info2.battery.technology),
            ComparisonField(stringResource(R.string.label_temperature), info1.battery.temperature, info2.battery.temperature),
            ComparisonField(stringResource(R.string.label_voltage), info1.battery.voltage, info2.battery.voltage),
            ComparisonField(stringResource(R.string.compare_charge), info1.battery.plugged, info2.battery.plugged),
        )
    ),

    // ── Display ────────────────────────────────────────────
    ComparisonSection(
        title = stringResource(R.string.section_display),
        icon = Icons.Filled.PhoneAndroid,
        fields = listOf(
            ComparisonField(stringResource(R.string.label_resolution), info1.display.resolution, info2.display.resolution),
            ComparisonField(stringResource(R.string.label_density), info1.display.density, info2.display.density),
            ComparisonField(stringResource(R.string.label_dpi), info1.display.densityDpi, info2.display.densityDpi),
            ComparisonField(stringResource(R.string.compare_refresh_rate), info1.display.refreshRate, info2.display.refreshRate),
            ComparisonField(stringResource(R.string.compare_display_info), info1.display.displayInfo, info2.display.displayInfo),
        )
    ),

    // ── System ─────────────────────────────────────────────
    ComparisonSection(
        title = stringResource(R.string.section_system),
        icon = Icons.Filled.Settings,
        fields = listOf(
            ComparisonField(stringResource(R.string.label_android), info1.general.androidVersion, info2.general.androidVersion),
            ComparisonField(stringResource(R.string.label_sdk), info1.general.sdkVersion, info2.general.sdkVersion),
            ComparisonField(stringResource(R.string.label_brand), info1.build.brand, info2.build.brand),
            ComparisonField("Bootloader", info1.build.bootloader, info2.build.bootloader),
            ComparisonField(stringResource(R.string.label_baseband), info1.build.baseband, info2.build.baseband),
            ComparisonField(stringResource(R.string.compare_firmware), info1.build.display, info2.build.display),
            ComparisonField(stringResource(R.string.compare_build_type), info1.build.type, info2.build.type),
            ComparisonField(stringResource(R.string.compare_build_tags), info1.build.tags, info2.build.tags),
        )
    ),

    // ── Hardware ───────────────────────────────────────────
    ComparisonSection(
        title = stringResource(R.string.section_hardware),
        icon = Icons.Filled.Build,
        fields = listOf(
            ComparisonField(stringResource(R.string.section_cameras), info1.cameras.size.toString(), info2.cameras.size.toString(),
                device1Wins = if (info1.cameras.size != info2.cameras.size)
                    info1.cameras.size > info2.cameras.size else null
            ),
            ComparisonField(stringResource(R.string.section_sensors), info1.sensors.size.toString(), info2.sensors.size.toString(),
                device1Wins = if (info1.sensors.size != info2.sensors.size)
                    info1.sensors.size > info2.sensors.size else null
            ),
            ComparisonField(stringResource(R.string.compare_network_interfaces), info1.network.interfaces.size.toString(), info2.network.interfaces.size.toString()),
            ComparisonField(stringResource(R.string.label_wifi_interface), info1.network.wifiInterface, info2.network.wifiInterface),
        )
    ),
)
