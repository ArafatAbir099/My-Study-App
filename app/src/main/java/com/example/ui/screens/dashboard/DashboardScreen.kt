package com.example.ui.screens.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.*
import com.example.ui.components.StudyActivityHeatmapCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel

@Composable
fun DashboardScreen(
    viewModel: PlannerViewModel,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToPYQ: () -> Unit = {},
    onNavigateToRevision: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onOpenWhatShouldIStudy: () -> Unit,
    onStartFocus: (TopicEntity) -> Unit,
    onNavigateToExams: () -> Unit = {},
    onNavigateToSyllabus: () -> Unit = {}
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val heatmapDays by viewModel.heatmapDays.collectAsStateWithLifecycle()
    val heatmapFilter by viewModel.heatmapFilter.collectAsStateWithLifecycle()
    val streakStats by viewModel.streakStats.collectAsStateWithLifecycle()
    val readinessList by viewModel.examReadinessList.collectAsStateWithLifecycle()
    val recommendations by viewModel.studyRecommendations.collectAsStateWithLifecycle()

    val todayStr = remember { viewModel.repository.todayStr() }
    val todayTasks = remember(tasks, todayStr) {
        tasks.filter { it.date == todayStr }
    }
    val pendingRevisions = remember(revisions, todayStr) {
        revisions.filter { it.scheduledDate <= todayStr && it.status != "COMPLETED" && it.status != "SKIPPED" }
    }

    val activeSemester = semesters.firstOrNull { !it.isArchived } ?: semesters.firstOrNull()
    val readiness = readinessList.firstOrNull()

    val haptic = LocalHapticFeedback.current
    var showEditSemesterDialog by remember { mutableStateOf(false) }

    val totalTopics = if (topics.isNotEmpty()) topics.size else 1
    val completedTopics = topics.count { it.status == "COMPLETED" }
    val syllabusPercent = (completedTopics * 100) / totalTopics

    val totalPyqs = if (pyqs.isNotEmpty()) pyqs.size else 1
    val solvedPyqs = pyqs.count { it.status == "SOLVED" }
    val pyqPercent = (solvedPyqs * 100) / totalPyqs

    val totalRevs = if (revisions.isNotEmpty()) revisions.size else 1
    val completedRevs = revisions.count { it.status == "COMPLETED" }
    val revPercent = (completedRevs * 100) / totalRevs

    val targetOverall = ((readiness?.overallScore ?: syllabusPercent) / 100f).coerceIn(0f, 1f)
    val animatedOverallProgress by animateFloatAsState(
        targetValue = targetOverall,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "overall_progress_anim"
    )
    val animatedOverallScore by animateIntAsState(
        targetValue = readiness?.overallScore ?: syllabusPercent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "overall_score_int_anim"
    )

    // Automatically check and reschedule any missed past study/revision tasks into upcoming schedule before exams
    LaunchedEffect(Unit) {
        viewModel.checkAndRescheduleMissedTasks()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP GREETING & SEMESTER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back, ${currentUser?.name?.substringBefore(" ") ?: "Student"}!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SEMESTER: ${activeSemester?.name ?: "No Semester"} (${activeSemester?.academicYear ?: ""})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                                if (activeSemester != null) {
                                    IconButton(
                                        onClick = { showEditSemesterDialog = true },
                                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Semester",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = onOpenWhatShouldIStudy,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_what_should_i_study")
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("What to study?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ROW 1: OVERALL PROGRESS & EXAM COUNTDOWN
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Readiness / Overall Progress
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToProgress() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Overall Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$animatedOverallScore%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Readiness",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { animatedOverallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Syllabus: $syllabusPercent%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Exams: ${exams.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Rev: $revPercent%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Exam Countdown
                val nearestExam = exams.minByOrNull { it.examDate }
                val daysLeft = readiness?.daysLeft

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToExams() }
                        .testTag("dashboard_exam_countdown_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (daysLeft != null && daysLeft <= 14) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = nearestExam?.name?.uppercase() ?: "UPCOMING EXAM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (daysLeft != null && daysLeft <= 14) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "View Exams",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (daysLeft != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$daysLeft",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (daysLeft <= 14) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "days left",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Exam: ${nearestExam?.examDate ?: ""} (${nearestExam?.examTime ?: ""})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = "Schedule Exam",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap to schedule exam date & time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // TODAY'S STUDY TASKS & REVISIONS DUE
        item {
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
                        Text(
                            text = "Today's Study Plan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToCalendar) {
                            Text("Open Calendar", fontSize = 12.sp)
                        }
                    }

                    if (todayTasks.isEmpty() && pendingRevisions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks scheduled for today. You control your timetable!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Pending Revisions First
                        if (pendingRevisions.isNotEmpty()) {
                            Text(
                                text = "Revisions Due (${pendingRevisions.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = TertiaryAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            pendingRevisions.take(2).forEach { rev ->
                                val topic = topics.firstOrNull { it.id == rev.topicId }
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = topic?.name ?: "Topic Revision",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Auto-scheduled Cycle #${rev.revisionNumber}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.completeRevision(rev)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Done", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Today's Study Tasks
                        if (todayTasks.isNotEmpty()) {
                            Text(
                                text = "Tasks (${todayTasks.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            todayTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleTask(task)
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${task.startTime} - ${task.endTime} (${task.durationMinutes}m)",
                                            style = MaterialTheme.typography.labelSmall,
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

        // SUBJECT PROGRESS LIST
        item {
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
                        Text(
                            text = "Subject Workspaces",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToSubjects) {
                            Text("View All", fontSize = 12.sp)
                        }
                    }

                    if (subjects.isEmpty()) {
                        Text(
                            text = "No subjects added yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
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
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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

        // PYQ QUICK ACCESS CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPYQ() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.HelpOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Previous-Year Questions (PYQ)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "${pyqs.size} Questions • ${pyqs.count { it.status == "SOLVED" }} Solved ($pyqPercent% Mastery)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open PYQ",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // TOP PRIORITY RECOMMENDATIONS PREVIEW
        item {
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
                        Text(
                            text = "Priority Study Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onOpenWhatShouldIStudy) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Open recommendations")
                        }
                    }

                    recommendations.take(3).forEach { rec ->
                        val topic = topics.firstOrNull { it.id == rec.topicId }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rec.topicName,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = rec.priorityReason,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TertiaryAmber
                                )
                            }

                            topic?.let { t ->
                                FilledTonalIconButton(
                                    onClick = { onStartFocus(t) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Focus", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditSemesterDialog && activeSemester != null) {
        var semName by remember { mutableStateOf(activeSemester.name) }
        var semYear by remember { mutableStateOf(activeSemester.academicYear) }
        var semStart by remember { mutableStateOf(activeSemester.startDate) }
        var semEnd by remember { mutableStateOf(activeSemester.endDate) }
        var semNotes by remember { mutableStateOf(activeSemester.notes) }

        AlertDialog(
            onDismissRequest = { showEditSemesterDialog = false },
            icon = { Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Edit Current Semester", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = semName,
                        onValueChange = { semName = it },
                        label = { Text("Semester Name (e.g. Spring 2026)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = semYear,
                        onValueChange = { semYear = it },
                        label = { Text("Academic Year (e.g. 2025-2026)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = semStart,
                            onValueChange = { semStart = it },
                            label = { Text("Start Date") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = semEnd,
                            onValueChange = { semEnd = it },
                            label = { Text("End Date") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = semNotes,
                        onValueChange = { semNotes = it },
                        label = { Text("Semester Notes / Target CGPA") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (semName.isNotBlank()) {
                            viewModel.updateSemester(
                                activeSemester.copy(
                                    name = semName,
                                    academicYear = semYear,
                                    startDate = semStart,
                                    endDate = semEnd,
                                    notes = semNotes
                                )
                            )
                            showEditSemesterDialog = false
                        }
                    }
                ) {
                    Text("Save Semester")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSemesterDialog = false }) { Text("Cancel") }
            }
        )
    }
}
