package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DayActivitySummary

@Composable
fun StudyActivityHeatmapCard(
    days: List<DayActivitySummary>,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<DayActivitySummary?>(null) }
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(days.size, currentFilter) {
        if (days.isNotEmpty()) {
            // Smoothly animate scroll to end so recent activities are visible
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Calculate totals
    val totalActivities = days.sumOf { it.count }
    val totalMinutes = days.sumOf { it.totalMinutes }
    val totalTasks = days.sumOf { it.tasksCompleted }
    val totalRevisions = days.sumOf { it.revisionsCompleted }
    val totalPYQs = days.sumOf { it.pyqsPracticed }

    val hours = totalMinutes / 60
    val remainingMins = totalMinutes % 60
    val formattedTime = if (hours > 0) "${hours}h ${remainingMins}m" else "${remainingMins}m"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_activity_heatmap_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Study Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$totalActivities study activities in the selected period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Filter dropdown / chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = currentFilter == "3_MONTHS",
                        onClick = { onFilterChange("3_MONTHS") },
                        label = { Text("3M", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                    FilterChip(
                        selected = currentFilter == "6_MONTHS",
                        onClick = { onFilterChange("6_MONTHS") },
                        label = { Text("6M", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                    FilterChip(
                        selected = currentFilter == "1_YEAR",
                        onClick = { onFilterChange("1_YEAR") },
                        label = { Text("1Y", fontSize = 11.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Heatmap Grid: 7 rows (days of week), columns for weeks
            // Chunk into columns of 7
            val columns = remember(days) {
                days.chunked(7)
            }

            // Scrollable Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    columns.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { day ->
                                val cellColor = getIntensityColor(day.intensityLevel, isDark)
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cellColor)
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedDay = day
                                        }
                                        .testTag("heatmap_day_${day.dateStr}")
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                listOf(0, 1, 2, 3, 4).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(getIntensityColor(level, isDark))
                            .border(
                                width = 0.5.dp,
                                color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Summary Statistics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(
                    label = "Total Time",
                    value = formattedTime,
                    icon = Icons.Default.Timer
                )
                SummaryMetricItem(
                    label = "Sessions",
                    value = "$totalActivities",
                    icon = Icons.Default.EventNote
                )
                SummaryMetricItem(
                    label = "Tasks Done",
                    value = "$totalTasks",
                    icon = Icons.Default.CheckCircle
                )
                SummaryMetricItem(
                    label = "Streak",
                    value = "$currentStreak d (max $longestStreak)",
                    icon = Icons.Default.LocalFireDepartment,
                    highlight = true
                )
            }
        }
    }

    // Day Details Dialog
    selectedDay?.let { day ->
        val dayHours = day.totalMinutes / 60
        val dayMins = day.totalMinutes % 60
        val dayTime = if (dayHours > 0) "${dayHours}h ${dayMins}m" else "${dayMins}m"

        AlertDialog(
            onDismissRequest = { selectedDay = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = day.dateStr,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(title = "Study Time", value = dayTime)
                    DetailRow(title = "Study Sessions", value = "${day.count}")
                    DetailRow(title = "Tasks Completed", value = "${day.tasksCompleted}")
                    DetailRow(title = "Revisions Finished", value = "${day.revisionsCompleted}")
                    DetailRow(title = "PYQs Practiced", value = "${day.pyqsPracticed}")
                    DetailRow(title = "Focus Sessions", value = "${day.focusSessions}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDay = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (highlight) TertiaryAmber else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (highlight) TertiaryAmber else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DetailRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun getIntensityColor(level: Int, isDark: Boolean): Color {
    return when (level) {
        0 -> if (isDark) HeatmapLevel0Dark else HeatmapLevel0Light
        1 -> HeatmapLevel1
        2 -> HeatmapLevel2
        3 -> HeatmapLevel3
        4 -> HeatmapLevel4
        else -> if (isDark) HeatmapLevel0Dark else HeatmapLevel0Light
    }
}
