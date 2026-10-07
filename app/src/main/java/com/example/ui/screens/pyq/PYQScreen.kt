package com.example.ui.screens.pyq

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PYQQuestionEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PYQScreen(
    viewModel: PlannerViewModel
) {
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedStatus by remember { mutableStateOf("ALL") } // ALL, UNSOLVED, ATTEMPTED, SOLVED
    var selectedDifficulty by remember { mutableStateOf("ALL") } // ALL, EASY, MEDIUM, HARD
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var pyqToDelete by remember { mutableStateOf<PYQQuestionEntity?>(null) }

    val filteredPyqs = remember(pyqs, selectedSubjectId, selectedStatus, selectedDifficulty, searchQuery) {
        pyqs.filter { q ->
            val matchSubject = selectedSubjectId == null || q.subjectId == selectedSubjectId
            val matchStatus = selectedStatus == "ALL" || q.status == selectedStatus
            val matchDiff = selectedDifficulty == "ALL" || q.difficulty == selectedDifficulty
            val matchSearch = searchQuery.isBlank() || q.questionText.contains(searchQuery, ignoreCase = true)
            matchSubject && matchStatus && matchDiff && matchSearch
        }
    }

    val totalCount = pyqs.size
    val solvedCount = pyqs.count { it.status == "SOLVED" }
    val solvedPct = if (totalCount > 0) (solvedCount * 100) / totalCount else 0

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add PYQ") },
                modifier = Modifier.testTag("fab_add_pyq")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("pyq_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PAST EXAM ANALYSIS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "PYQ Practice & Question Bank",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Track university previous-year questions and prioritize repeated high-mark problems.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$totalCount", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Solved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$solvedCount", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = StrongGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Mastery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$solvedPct%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Search & Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search question text...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Subject Chips
                    if (subjects.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedSubjectId == null,
                                    onClick = { selectedSubjectId = null },
                                    label = { Text("All Subjects") }
                                )
                            }
                            items(subjects) { sub ->
                                FilterChip(
                                    selected = selectedSubjectId == sub.id,
                                    onClick = { selectedSubjectId = sub.id },
                                    label = { Text(sub.name) }
                                )
                            }
                        }
                    }

                    // Status Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "ALL" to "All",
                            "UNSOLVED" to "Unsolved",
                            "ATTEMPTED" to "Attempted",
                            "SOLVED" to "Solved"
                        ).forEach { (statusKey, statusLabel) ->
                            FilterChip(
                                selected = selectedStatus == statusKey,
                                onClick = { selectedStatus = statusKey },
                                label = { Text(statusLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Questions List / Empty State
            if (filteredPyqs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No PYQs added yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Upload or add previous-year questions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add PYQ")
                            }
                        }
                    }
                }
            } else {
                items(filteredPyqs, key = { it.id }) { pyq ->
                    val subject = subjects.firstOrNull { it.id == pyq.subjectId }
                    val topic = topics.firstOrNull { it.id == pyq.topicId }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top metadata row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = subject?.courseCode ?: subject?.name ?: "Subject",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${pyq.examType} ${pyq.year}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "${pyq.marks} Marks",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { pyqToDelete = pyq },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (topic != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Topic: ${topic.name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = pyq.questionText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 20.sp
                            )

                            if (pyq.solutionNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Solution / Key Notes:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(pyq.solutionNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons: Unsolved, Attempted, Solved
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: ${pyq.status}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (pyq.status) {
                                        "SOLVED" -> StrongGreen
                                        "ATTEMPTED" -> TertiaryAmber
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (pyq.status != "SOLVED") {
                                        FilledTonalButton(
                                            onClick = { viewModel.updatePYQStatus(pyq, "SOLVED") },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark Solved", fontSize = 11.sp)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { viewModel.updatePYQStatus(pyq, "UNSOLVED") },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Reopen", fontSize = 11.sp)
                                        }
                                    }

                                    if (pyq.status == "UNSOLVED") {
                                        OutlinedButton(
                                            onClick = { viewModel.updatePYQStatus(pyq, "ATTEMPTED") },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Attempted", fontSize = 11.sp)
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

    // Add PYQ Dialog
    if (showAddDialog) {
        var formSubId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
        var formTopId by remember { mutableStateOf<Long?>(null) }
        var formExamType by remember { mutableStateOf("Midterm") }
        var formYear by remember { mutableStateOf("2026") }
        var formQuestion by remember { mutableStateOf("") }
        var formMarks by remember { mutableStateOf("10") }
        var formDiff by remember { mutableStateOf("MEDIUM") }
        var formSolution by remember { mutableStateOf("") }

        val subTopics = remember(formSubId, topics) {
            topics.filter { it.subjectId == formSubId }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Previous-Year Question", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text("Subject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (subjects.isEmpty()) {
                            Text("Please create a subject first.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(subjects) { s ->
                                    FilterChip(
                                        selected = formSubId == s.id,
                                        onClick = {
                                            formSubId = s.id
                                            formTopId = null
                                        },
                                        label = { Text(s.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    if (subTopics.isNotEmpty()) {
                        item {
                            Text("Topic (Optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    FilterChip(
                                        selected = formTopId == null,
                                        onClick = { formTopId = null },
                                        label = { Text("None", fontSize = 11.sp) }
                                    )
                                }
                                items(subTopics) { t ->
                                    FilterChip(
                                        selected = formTopId == t.id,
                                        onClick = { formTopId = t.id },
                                        label = { Text(t.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = formQuestion,
                            onValueChange = { formQuestion = it },
                            label = { Text("Question Text *") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formExamType,
                                onValueChange = { formExamType = it },
                                label = { Text("Exam (e.g. Midterm)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = formYear,
                                onValueChange = { formYear = it },
                                label = { Text("Year") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = formMarks,
                                onValueChange = { formMarks = it },
                                label = { Text("Marks") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = formDiff,
                                onValueChange = { formDiff = it },
                                label = { Text("Difficulty (EASY/MEDIUM/HARD)") },
                                modifier = Modifier.weight(1.5f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = formSolution,
                            onValueChange = { formSolution = it },
                            label = { Text("Solution Notes / Answer Key (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (formSubId > 0 && formQuestion.isNotBlank()) {
                            viewModel.addPYQ(
                                subjectId = formSubId,
                                topicId = formTopId,
                                examType = formExamType.ifBlank { "Midterm" },
                                year = formYear.toIntOrNull() ?: 2026,
                                questionText = formQuestion.trim(),
                                marks = formMarks.toIntOrNull() ?: 10,
                                difficulty = formDiff.uppercase().ifBlank { "MEDIUM" },
                                status = "UNSOLVED",
                                isImportant = true,
                                solutionNotes = formSolution.trim()
                            )
                            showAddDialog = false
                        }
                    },
                    enabled = formSubId > 0 && formQuestion.isNotBlank()
                ) {
                    Text("Save Question")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Delete Confirmation Dialog
    pyqToDelete?.let { pyq ->
        AlertDialog(
            onDismissRequest = { pyqToDelete = null },
            title = { Text("Delete Question?") },
            text = { Text("Are you sure you want to delete this previous-year question?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePYQ(pyq.id)
                        pyqToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pyqToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
