package com.example.ui.screens.revision

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.RevisionItemEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    viewModel: PlannerViewModel
) {
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val recommendations by viewModel.studyRecommendations.collectAsStateWithLifecycle()

    val todayStr = remember { viewModel.repository.todayStr() }
    var mainViewMode by remember { mutableStateOf("AI_PLAN") } // "AI_PLAN" or "SPACED_CYCLES"
    var selectedCycleTab by remember { mutableStateOf("DUE") } // "DUE", "UPCOMING", "COMPLETED"
    var selectedPriorityFilter by remember { mutableStateOf("ALL") } // "ALL", "HIGH", "MEDIUM", "LOW"
    var revisionToReschedule by remember { mutableStateOf<RevisionItemEntity?>(null) }

    val dueRevisions = remember(revisions, todayStr) {
        revisions.filter { it.scheduledDate <= todayStr && it.status != "COMPLETED" && it.status != "SKIPPED" }
    }
    val upcomingRevisions = remember(revisions, todayStr) {
        revisions.filter { it.scheduledDate > todayStr && it.status != "COMPLETED" && it.status != "SKIPPED" }
    }
    val completedRevisions = remember(revisions) {
        revisions.filter { it.status == "COMPLETED" }
    }

    val displayCycleList = when (selectedCycleTab) {
        "DUE" -> dueRevisions
        "UPCOMING" -> upcomingRevisions
        else -> completedRevisions
    }

    // Filter AI recommendations
    val filteredRecommendations = remember(recommendations, selectedPriorityFilter) {
        if (selectedPriorityFilter == "ALL") recommendations
        else recommendations.filter { r ->
            val priority = when {
                r.priorityScore >= 70 -> "HIGH"
                r.priorityScore >= 40 -> "MEDIUM"
                else -> "LOW"
            }
            priority == selectedPriorityFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("revision_screen")
    ) {
        // Mode Selector: AI Priority Plan vs Spaced Cycles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (mainViewMode == "AI_PLAN") MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { mainViewMode = "AI_PLAN" }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (mainViewMode == "AI_PLAN") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Revision Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (mainViewMode == "AI_PLAN") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (mainViewMode == "SPACED_CYCLES") MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { mainViewMode = "SPACED_CYCLES" }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Autorenew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (mainViewMode == "SPACED_CYCLES") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Spaced Repetitions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (mainViewMode == "SPACED_CYCLES") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (mainViewMode == "AI_PLAN") {
            // AI REVISION PLAN VIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Revision Priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "AI analyzes exam dates, syllabus progress, repeated PYQs & weak spots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (All, High, Medium, Low)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("ALL" to "All Priorities", "HIGH" to "High", "MEDIUM" to "Medium", "LOW" to "Low").forEach { (key, label) ->
                    FilterChip(
                        selected = selectedPriorityFilter == key,
                        onClick = { selectedPriorityFilter = key },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredRecommendations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No revision plan yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add subjects, syllabus, or exam dates to generate your AI revision plan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredRecommendations, key = { it.topicId }) { rec ->
                        val topic = topics.firstOrNull { it.id == rec.topicId }
                        val subject = subjects.firstOrNull { it.id == topic?.subjectId }
                        val priorityLevel = when {
                            rec.priorityScore >= 70 -> "HIGH"
                            rec.priorityScore >= 40 -> "MEDIUM"
                            else -> "LOW"
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.topicName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = subject?.name ?: "Subject",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (priorityLevel) {
                                            "HIGH" -> WeakRed.copy(alpha = 0.15f)
                                            "MEDIUM" -> TertiaryAmber.copy(alpha = 0.15f)
                                            else -> StrongGreen.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = "$priorityLevel PRIORITY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (priorityLevel) {
                                                "HIGH" -> WeakRed
                                                "MEDIUM" -> TertiaryAmber
                                                else -> StrongGreen
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Reason badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = rec.priorityReason,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PYQs: ${rec.appearanceCount} (${rec.solvedCount} solved)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (topic != null) {
                                        FilledTonalButton(
                                            onClick = {
                                                viewModel.completeTopic(topic, "STRONG")
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark Revised", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // SPACED REPETITION CYCLES VIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCycleTab == "DUE",
                    onClick = { selectedCycleTab = "DUE" },
                    label = { Text("Due Now (${dueRevisions.size})", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedCycleTab == "UPCOMING",
                    onClick = { selectedCycleTab = "UPCOMING" },
                    label = { Text("Upcoming (${upcomingRevisions.size})", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedCycleTab == "COMPLETED",
                    onClick = { selectedCycleTab = "COMPLETED" },
                    label = { Text("Done (${completedRevisions.size})", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (displayCycleList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = StrongGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedCycleTab == "DUE") "All revisions up to date! Great consistency."
                            else "No items in this category.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayCycleList, key = { it.id }) { rev ->
                        val topic = topics.firstOrNull { it.id == rev.topicId }
                        val subject = subjects.firstOrNull { it.id == rev.subjectId }
                        val isDue = rev.scheduledDate <= todayStr && rev.status != "COMPLETED"

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDue) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = topic?.name ?: "Topic Revision",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "${subject?.name ?: "Subject"} • Cycle #${rev.revisionNumber} • Scheduled: ${rev.scheduledDate}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDue) TertiaryAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (topic?.understanding) {
                                            "STRONG" -> StrongGreen.copy(alpha = 0.15f)
                                            "WEAK" -> WeakRed.copy(alpha = 0.15f)
                                            else -> OkayYellow.copy(alpha = 0.2f)
                                        }
                                    ) {
                                        Text(
                                            text = "Topic: ${topic?.understanding ?: "OKAY"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (topic?.understanding) {
                                                "STRONG" -> StrongGreen
                                                "WEAK" -> WeakRed
                                                else -> Color(0xFFB45309)
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (rev.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rev.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (rev.status != "COMPLETED") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { viewModel.completeRevision(rev) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Mark Done", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { revisionToReschedule = rev },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reschedule", fontSize = 11.sp)
                                            }
                                        }

                                        TextButton(
                                            onClick = { viewModel.skipRevision(rev) },
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text("Skip", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Completed successfully. Logged to Study Activity.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StrongGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reschedule Revision Dialog
    revisionToReschedule?.let { rev ->
        var newDate by remember { mutableStateOf(todayStr) }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { revisionToReschedule = null },
            title = { Text("Reschedule Revision", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a new date to revise this topic:", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            viewModel.rescheduleRevision(rev, dateFormat.format(cal.time))
                            revisionToReschedule = null
                        }) {
                            Text("+1 Day")
                        }
                        Button(onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, 3)
                            viewModel.rescheduleRevision(rev, dateFormat.format(cal.time))
                            revisionToReschedule = null
                        }) {
                            Text("+3 Days")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("Or specify Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rescheduleRevision(rev, newDate)
                    revisionToReschedule = null
                }) {
                    Text("Confirm Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { revisionToReschedule = null }) { Text("Cancel") }
            }
        )
    }
}
