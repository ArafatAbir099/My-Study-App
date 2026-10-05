package com.example.ui.screens.progress

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.StudyActivityHeatmapCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel

@Composable
fun ProgressScreen(
    viewModel: PlannerViewModel
) {
    val heatmapDays by viewModel.heatmapDays.collectAsStateWithLifecycle()
    val heatmapFilter by viewModel.heatmapFilter.collectAsStateWithLifecycle()
    val streakStats by viewModel.streakStats.collectAsStateWithLifecycle()
    val readinessList by viewModel.examReadinessList.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()

    val totalTopics = if (topics.isNotEmpty()) topics.size else 1
    val completedTopics = topics.count { it.status == "COMPLETED" }
    val syllabusPercent = (completedTopics * 100) / totalTopics

    val totalPyqs = if (pyqs.isNotEmpty()) pyqs.size else 1
    val solvedPyqs = pyqs.count { it.status == "SOLVED" }
    val pyqPercent = (solvedPyqs * 100) / totalPyqs

    val totalRevs = if (revisions.isNotEmpty()) revisions.size else 1
    val completedRevs = revisions.count { it.status == "COMPLETED" }
    val revPercent = (completedRevs * 100) / totalRevs

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("progress_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Academic Progress & Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Track your semester consistency, exam readiness, and study activity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // STUDY ACTIVITY HEATMAP
        item {
            StudyActivityHeatmapCard(
                days = heatmapDays,
                currentFilter = heatmapFilter,
                onFilterChange = { viewModel.heatmapFilter.value = it },
                currentStreak = streakStats.first,
                longestStreak = streakStats.second
            )
        }

        // EXAM READINESS SECTION
        item {
            Text(
                text = "Exam Readiness Intelligence",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(readinessList) { readiness ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = readiness.examName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (readiness.daysLeft != null) {
                                Text(
                                    text = "${readiness.daysLeft} days until exam",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (readiness.daysLeft <= 14) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${readiness.overallScore}% Ready",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown bars: Syllabus, Understanding, Revision, PYQ
                    ReadinessMetricBar("Syllabus Completion", readiness.syllabusScore, MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    ReadinessMetricBar("Topic Understanding", readiness.understandingScore, StrongGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    ReadinessMetricBar("Revision Practice", readiness.revisionScore, TertiaryAmber)
                    Spacer(modifier = Modifier.height(6.dp))
                    ReadinessMetricBar("PYQ Problems Solved", readiness.pyqScore, SecondaryTeal)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Weakness Callout
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = readiness.mainWeakness,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // SUBJECT PROGRESS BREAKDOWN
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Subject Syllabus Coverage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    subjects.forEach { sub ->
                        val subTopics = topics.filter { it.subjectId == sub.id }
                        val comp = subTopics.count { it.status == "COMPLETED" }
                        val total = if (subTopics.isNotEmpty()) subTopics.size else 1
                        val pct = (comp * 100) / total

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(sub.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("$comp / ${subTopics.size} ($pct%)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (pct / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessMetricBar(label: String, score: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$score%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color
        )
    }
}
