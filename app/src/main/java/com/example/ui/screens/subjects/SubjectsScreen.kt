package com.example.ui.screens.subjects

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel

@Composable
fun SubjectsScreen(
    viewModel: PlannerViewModel,
    onStartFocus: (TopicEntity) -> Unit
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()

    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showExamDatePicker by remember { mutableStateOf(false) }
    var topicToComplete by remember { mutableStateOf<TopicEntity?>(null) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicEntity?>(null) }

    // State for Image Syllabus Upload & AI Extraction Preview
    var isProcessingSyllabusImage by remember { mutableStateOf(false) }
    var showSyllabusPreviewDialog by remember { mutableStateOf(false) }
    var extractedTopicList by remember { mutableStateOf<List<String>>(emptyList()) }
    var previewSubjectTarget by remember { mutableStateOf<SubjectEntity?>(null) }
    var syllabusUploadSnackbar by remember { mutableStateOf<String?>(null) }

    // Multiple Images Launcher
    val syllabusImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        val target = selectedSubject ?: subjects.firstOrNull()
        if (uris.isNotEmpty() && target != null) {
            isProcessingSyllabusImage = true
            previewSubjectTarget = target
            viewModel.extractSyllabusFromImages(target, uris) { extractedNames ->
                isProcessingSyllabusImage = false
                extractedTopicList = extractedNames
                showSyllabusPreviewDialog = true
            }
        }
    }

    // If selected subject is null and subjects exist, default to first
    LaunchedEffect(subjects) {
        if (selectedSubject == null && subjects.isNotEmpty()) {
            selectedSubject = subjects.first()
        }
    }

    val currentSubject = selectedSubject ?: subjects.firstOrNull()
    val subjectTopics = remember(topics, currentSubject) {
        if (currentSubject != null) topics.filter { it.subjectId == currentSubject.id } else emptyList()
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (currentSubject != null) showAddTopicDialog = true else showAddSubjectDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (currentSubject != null) "Add Topic" else "Add Subject") },
                modifier = Modifier.testTag("fab_add_subject_or_topic")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("subjects_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header & Subject Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subjects & Syllabus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = { showAddSubjectDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Subject", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subject Tabs / Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects) { sub ->
                    val isSelected = currentSubject?.id == sub.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSubject = sub },
                        label = { Text(sub.name, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (currentSubject == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No subjects found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showAddSubjectDialog = true }) {
                            Text("Create First Subject")
                        }
                    }
                }
            } else {
                // Subject Workspace Overview Card
                val completedCount = subjectTopics.count { it.status == "COMPLETED" }
                val totalCount = if (subjectTopics.isNotEmpty()) subjectTopics.size else 1
                val progressPct = (completedCount * 100) / totalCount
                val animatedSubjectProgress by animateFloatAsState(
                    targetValue = (progressPct / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "subject_progress_anim"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSubject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentSubject.courseCode} • ${currentSubject.credits} Credits • ${currentSubject.teacherName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { subjectToEdit = currentSubject }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Subject", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { subjectToDelete = currentSubject }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Subject", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Syllabus Progress: $completedCount / ${subjectTopics.size} topics ($progressPct%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { animatedSubjectProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Upcoming Exam Date Card
                        val subjectExam = exams.filter { it.subjectId == currentSubject.id }.minByOrNull { it.examDate }
                        val todayStr = remember { viewModel.repository.todayStr() }
                        val dateFormat = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Upcoming Exam Date",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (subjectExam != null) {
                                        Row {
                                            IconButton(
                                                onClick = { showExamDatePicker = true },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Date", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeSubjectUpcomingExamDate(currentSubject.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove Date", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                if (subjectExam != null) {
                                    val daysDiff = try {
                                        val d1 = dateFormat.parse(todayStr)?.time ?: 0L
                                        val d2 = dateFormat.parse(subjectExam.examDate)?.time ?: 0L
                                        ((d2 - d1) / (1000 * 60 * 60 * 24)).toInt()
                                    } catch (_: Exception) { 0 }

                                    val statusText = when {
                                        daysDiff < 0 -> "Exam completed"
                                        daysDiff == 0 -> "Exam today"
                                        daysDiff == 1 -> "Exam tomorrow"
                                        daysDiff <= 7 -> "Exam approaching"
                                        else -> "Exam in $daysDiff days"
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${currentSubject.name} • ${subjectExam.examDate}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (daysDiff >= 0) "$daysDiff days remaining" else "Completed",
                                                fontSize = 11.sp,
                                                color = if (daysDiff <= 7 && daysDiff >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when {
                                                daysDiff < 0 -> MaterialTheme.colorScheme.surfaceVariant
                                                daysDiff <= 3 -> MaterialTheme.colorScheme.errorContainer
                                                daysDiff <= 7 -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            }
                                        ) {
                                            Text(
                                                text = statusText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "No upcoming exam date set",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        OutlinedButton(
                                            onClick = { showExamDatePicker = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Set Exam Date", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Upload Syllabus Image Action Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { syllabusImagesLauncher.launch("image/*") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_upload_syllabus_image"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Syllabus Image", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showAddTopicDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Topic", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Topics List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Syllabus Topics (${subjectTopics.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (subjectTopics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "No topics in ${currentSubject.name} yet.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Take a photo or upload screenshot(s) of your syllabus.\nAI will read the topics automatically!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { syllabusImagesLauncher.launch("image/*") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Syllabus Image(s)")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(subjectTopics, key = { it.id }) { topic ->
                            val topicPyqs = pyqs.filter { it.topicId == topic.id }
                            val topicRevs = revisions.filter { it.topicId == topic.id }
                            val isCompleted = topic.status == "COMPLETED"

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("topic_card_${topic.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = topic.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            if (topic.description.isNotEmpty()) {
                                                Text(
                                                    text = topic.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Status Pill
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (topic.status) {
                                                "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer
                                                "IN_PROGRESS" -> TertiaryAmber.copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ) {
                                            Text(
                                                text = topic.status.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = when (topic.status) {
                                                    "COMPLETED" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    "IN_PROGRESS" -> TertiaryAmber
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Topic Metadata Badges: Understanding, Importance, PYQs count, Revisions count
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Understanding Badge
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (topic.understanding) {
                                                "STRONG" -> StrongGreen.copy(alpha = 0.15f)
                                                "WEAK" -> WeakRed.copy(alpha = 0.15f)
                                                else -> OkayYellow.copy(alpha = 0.2f)
                                            }
                                        ) {
                                            Text(
                                                text = "Understanding: ${topic.understanding}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when (topic.understanding) {
                                                    "STRONG" -> StrongGreen
                                                    "WEAK" -> WeakRed
                                                    else -> Color(0xFFB45309)
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Importance
                                        if (topic.importance != "NORMAL") {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = TertiaryAmber.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = topic.importance.replace("_", " "),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TertiaryAmber,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "PYQs: ${topicPyqs.size} | Revs: ${topicRevs.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Buttons: Complete Topic (triggers automatic revision), Focus Session, Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (!isCompleted) {
                                                Button(
                                                    onClick = { topicToComplete = topic },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Mark Completed", fontSize = 11.sp)
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = { topicToComplete = topic },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("Update Status", fontSize = 11.sp)
                                                }
                                            }

                                            FilledTonalButton(
                                                onClick = { onStartFocus(topic) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Focus", fontSize = 11.sp)
                                            }
                                        }

                                        IconButton(
                                            onClick = { topicToDelete = topic },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Complete Topic Dialog with Automatic Revision Engine trigger
    topicToComplete?.let { topic ->
        var selectedUnderstanding by remember { mutableStateOf(topic.understanding) }

        AlertDialog(
            onDismissRequest = { topicToComplete = null },
            icon = { Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Complete Topic & Plan Revision", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Topic: ${topic.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "How was your understanding? The Automatic Revision Engine will schedule spaced revisions based on your rating:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    listOf(
                        Pair("STRONG", "Strong - Revision in +3, +7, +15 days"),
                        Pair("OKAY", "Okay - Revision in +1, +4, +10 days"),
                        Pair("WEAK", "Weak - Frequent revision in +1, +2, +5, +10 days")
                    ).forEach { (level, desc) ->
                        OutlinedCard(
                            onClick = { selectedUnderstanding = level },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (selectedUnderstanding == level) {
                                CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else CardDefaults.outlinedCardColors()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = level, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeTopic(topic, selectedUnderstanding)
                        topicToComplete = null
                    }
                ) {
                    Text("Complete & Generate Revisions")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToComplete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Subject Dialog
    if (showAddSubjectDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var credits by remember { mutableStateOf("3.0") }
        var teacher by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("Create Subject", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Course Code (e.g. CSE 201)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = credits, onValueChange = { credits = it }, label = { Text("Credits") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("Teacher Name (Optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes / Goals") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addSubject(
                                name = name,
                                code = code,
                                credits = credits.toDoubleOrNull() ?: 3.0,
                                teacher = teacher,
                                colorHex = "#3B82F6",
                                iconName = "menu_book",
                                notes = notes
                            )
                            showAddSubjectDialog = false
                        }
                    }
                ) {
                    Text("Add Subject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add Topic Dialog
    if (showAddTopicDialog && currentSubject != null) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var importance by remember { mutableStateOf("NORMAL") }

        AlertDialog(
            onDismissRequest = { showAddTopicDialog = false },
            title = { Text("Add Syllabus Topic", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Topic Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description / Subtopics") }, modifier = Modifier.fillMaxWidth())
                    Text("Importance:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NORMAL", "IMPORTANT", "VERY_IMPORTANT").forEach { imp ->
                            FilterChip(
                                selected = importance == imp,
                                onClick = { importance = imp },
                                label = { Text(imp.replace("_", " "), fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addTopic(
                                subjectId = currentSubject.id,
                                chapterId = 1L,
                                name = name,
                                description = desc,
                                importance = importance
                            )
                            showAddTopicDialog = false
                        }
                    }
                ) {
                    Text("Add Topic")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTopicDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Confirm Delete Subject Dialog
    subjectToDelete?.let { sub ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Subject?") },
            text = { Text("Are you sure you want to delete '${sub.name}'? This will also remove its associated syllabus topics.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSubject(sub.id)
                        subjectToDelete = null
                        selectedSubject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Confirm Delete Topic Dialog
    topicToDelete?.let { top ->
        AlertDialog(
            onDismissRequest = { topicToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Topic?") },
            text = { Text("Are you sure you want to delete '${top.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTopic(top.id)
                        topicToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { topicToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Edit Subject Dialog
    subjectToEdit?.let { sub ->
        var editName by remember { mutableStateOf(sub.name) }
        var editCode by remember { mutableStateOf(sub.courseCode) }
        var editCredits by remember { mutableStateOf(sub.credits.toString()) }
        var editTeacher by remember { mutableStateOf(sub.teacherName) }
        var editNotes by remember { mutableStateOf(sub.notes) }

        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Edit Subject", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Subject Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("Course Code (e.g. CSE 201)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCredits,
                        onValueChange = { editCredits = it },
                        label = { Text("Credits") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTeacher,
                        onValueChange = { editTeacher = it },
                        label = { Text("Teacher Name (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes / Goals") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateSubject(
                                sub.copy(
                                    name = editName,
                                    courseCode = editCode,
                                    credits = editCredits.toDoubleOrNull() ?: 3.0,
                                    teacherName = editTeacher,
                                    notes = editNotes
                                )
                            )
                            subjectToEdit = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // AI EXTRACTED SYLLABUS PREVIEW & EDIT DIALOG
    // ==========================================
    if (showSyllabusPreviewDialog && previewSubjectTarget != null) {
        val targetSub = previewSubjectTarget!!
        val hasExtracted = extractedTopicList.isNotEmpty()
        var editableTopicsText by remember {
            mutableStateOf(extractedTopicList.joinToString("\n"))
        }
        var isEditingRaw by remember { mutableStateOf(!hasExtracted) }

        AlertDialog(
            onDismissRequest = { showSyllabusPreviewDialog = false },
            icon = {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Column {
                    Text("AI Extracted Syllabus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Subject: ${targetSub.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasExtracted) {
                        Text(
                            text = "Review extracted topics below. The AI extracted content from your uploaded image(s) without inventing unpresent topics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Could not automatically read topics from image. You can type or paste your syllabus topics below (one topic per line) and confirm.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditingRaw) "Edit Topics (one per line):" else "Extracted Topics (${editableTopicsText.lines().filter { it.isNotBlank() }.size}):",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        TextButton(onClick = { isEditingRaw = !isEditingRaw }) {
                            Icon(
                                if (isEditingRaw) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEditingRaw) "Preview" else "Edit", fontSize = 11.sp)
                        }
                    }

                    if (isEditingRaw) {
                        OutlinedTextField(
                            value = editableTopicsText,
                            onValueChange = { editableTopicsText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(min = 160.dp, max = 260.dp),
                            placeholder = { Text("Topic 1\nTopic 2\nTopic 3") }
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val currentLines = editableTopicsText.lines().filter { it.isNotBlank() }
                                items(currentLines) { topicLine ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = topicLine.trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTopics = editableTopicsText.lines().map { it.trim() }.filter { it.isNotBlank() }
                        viewModel.confirmExtractedSyllabus(targetSub, finalTopics) { savedCount ->
                            syllabusUploadSnackbar = "Confirmed! $savedCount topics added and study plan updated."
                        }
                        showSyllabusPreviewDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_syllabus")
                ) {
                    Text("Confirm Syllabus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSyllabusPreviewDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Exam Date Picker Dialog
    if (showExamDatePicker && currentSubject != null) {
        val cal = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = java.util.Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val formatted = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(selectedCal.time)
                viewModel.setSubjectUpcomingExamDate(currentSubject.id, formatted)
                showExamDatePicker = false
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showExamDatePicker = false }
            show()
        }
    }
}
