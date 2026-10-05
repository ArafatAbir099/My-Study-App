package com.example.ui.screens.subjects

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
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val revisions by viewModel.revisions.collectAsStateWithLifecycle()

    var selectedSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var topicToComplete by remember { mutableStateOf<TopicEntity?>(null) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicEntity?>(null) }

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
                        Text(
                            text = "No topics in this syllabus yet. Tap + Add Topic to build your curriculum!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
}
