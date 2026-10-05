package com.example.ui.screens.pyq

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.PYQQuestionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel

@Composable
fun PYQScreen(
    viewModel: PlannerViewModel
) {
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("QUESTIONS") } // "QUESTIONS", "ANALYSIS", "PRACTICE"
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var showAddPYQDialog by remember { mutableStateOf(false) }
    var pyqToDelete by remember { mutableStateOf<PYQQuestionEntity?>(null) }

    // Filtered questions
    val filteredPyqs = remember(pyqs, selectedSubjectId, selectedDifficulty, selectedStatus) {
        pyqs.filter { pyq ->
            (selectedSubjectId == null || pyq.subjectId == selectedSubjectId) &&
            (selectedDifficulty == null || pyq.difficulty == selectedDifficulty) &&
            (selectedStatus == null || pyq.status == selectedStatus)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (activeTab != "PRACTICE") {
                ExtendedFloatingActionButton(
                    onClick = { showAddPYQDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add PYQ") },
                    modifier = Modifier.testTag("fab_add_pyq")
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("pyq_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Main Tab Switcher: Questions, Analysis, Practice Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Previous Year Questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("QUESTIONS", "ANALYSIS", "PRACTICE").forEach { tab ->
                        FilterChip(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            label = { Text(tab.lowercase().capitalize(), fontSize = 11.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips (Subject, Difficulty, Status)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedSubjectId == null,
                        onClick = { selectedSubjectId = null },
                        label = { Text("All Subjects", fontSize = 11.sp) }
                    )
                }
                items(subjects) { sub ->
                    FilterChip(
                        selected = selectedSubjectId == sub.id,
                        onClick = { selectedSubjectId = sub.id },
                        label = { Text(sub.name, fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                "QUESTIONS" -> {
                    // Questions View
                    if (filteredPyqs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No PYQs found matching filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredPyqs, key = { it.id }) { pyq ->
                                val sub = subjects.firstOrNull { it.id == pyq.subjectId }
                                val top = topics.firstOrNull { it.id == pyq.topicId }
                                var showSolution by remember { mutableStateOf(false) }

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = "${pyq.examType} ${pyq.year}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = "${pyq.marks} Marks",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (pyq.isRepeated) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = TertiaryAmber.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "Repeated",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TertiaryAmber,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { pyqToDelete = pyq },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = pyq.questionText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )

                                        if (top != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Topic: ${top.name}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (showSolution && pyq.solutionNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "Solution / Key Points:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = pyq.solutionNotes,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Status update row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (pyq.solutionNotes.isNotEmpty()) {
                                                TextButton(
                                                    onClick = { showSolution = !showSolution },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(if (showSolution) "Hide Solution" else "Show Solution", fontSize = 11.sp)
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(1.dp))
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("UNSOLVED", "ATTEMPTED", "SOLVED", "NEED_PRACTICE").forEach { st ->
                                                    FilterChip(
                                                        selected = pyq.status == st,
                                                        onClick = { viewModel.updatePYQStatus(pyq, st) },
                                                        label = { Text(st.replace("_", " ").lowercase().capitalize(), fontSize = 10.sp) },
                                                        modifier = Modifier.height(28.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "ANALYSIS" -> {
                    // Real PYQ Analysis per topic
                    val currentTopics = remember(topics, selectedSubjectId) {
                        if (selectedSubjectId != null) topics.filter { it.subjectId == selectedSubjectId } else topics
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            Text(
                                text = "Topic Appearance & Frequency Analysis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Calculated from real exam question records. Identify high-yield exam topics.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(currentTopics) { topic ->
                            val analysis = viewModel.analyzePYQForTopic(topic)

                            Card(
                                shape = RoundedCornerShape(14.dp),
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
                                                text = analysis.topicName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Understanding: ${analysis.understanding} • Importance: ${analysis.importance}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (analysis.appearanceCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (analysis.appearanceCount >= 2) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "${analysis.appearanceCount}x in Exams",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (analysis.appearanceCount >= 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (analysis.appearanceCount == 0) {
                                        Text(
                                            text = "Not enough PYQ data.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Years: ${analysis.yearsAppeared.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "Solved: ${analysis.solvedCount} / ${analysis.appearanceCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (analysis.solvedCount == analysis.appearanceCount) StrongGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Study Priority: ${analysis.priorityReason}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TertiaryAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "PRACTICE" -> {
                    // Dedicated PYQ Practice Mode
                    var currentPracticeIndex by remember { mutableIntStateOf(0) }
                    var showSolution by remember { mutableStateOf(false) }

                    if (filteredPyqs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No questions available for practice under current filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val activePyq = filteredPyqs.getOrNull(currentPracticeIndex) ?: filteredPyqs.first()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { ((currentPracticeIndex + 1).toFloat() / filteredPyqs.size) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Question ${currentPracticeIndex + 1} of ${filteredPyqs.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${activePyq.examType} Exam (${activePyq.year})",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${activePyq.marks} Marks",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = activePyq.questionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 24.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (showSolution) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Solution & Key Steps:",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = activePyq.solutionNotes.ifEmpty { "No solution text provided." },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { showSolution = true },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Reveal Solution / Clues")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Mark as solved/need practice buttons
                                    Text("How did your answer go?", style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.updatePYQStatus(activePyq, "SOLVED")
                                                if (currentPracticeIndex < filteredPyqs.size - 1) {
                                                    currentPracticeIndex++
                                                    showSolution = false
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Solved!")
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                viewModel.updatePYQStatus(activePyq, "NEED_PRACTICE")
                                                if (currentPracticeIndex < filteredPyqs.size - 1) {
                                                    currentPracticeIndex++
                                                    showSolution = false
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Need Practice")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Prev / Next Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (currentPracticeIndex > 0) {
                                            currentPracticeIndex--
                                            showSolution = false
                                        }
                                    },
                                    enabled = currentPracticeIndex > 0
                                ) {
                                    Text("Previous")
                                }

                                Button(
                                    onClick = {
                                        if (currentPracticeIndex < filteredPyqs.size - 1) {
                                            currentPracticeIndex++
                                            showSolution = false
                                        }
                                    },
                                    enabled = currentPracticeIndex < filteredPyqs.size - 1
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add PYQ Dialog
    if (showAddPYQDialog) {
        var subjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 1L) }
        var topicId by remember { mutableStateOf<Long?>(null) }
        var examType by remember { mutableStateOf("Midterm") }
        var year by remember { mutableStateOf("2026") }
        var question by remember { mutableStateOf("") }
        var marks by remember { mutableStateOf("5") }
        var difficulty by remember { mutableStateOf("MEDIUM") }
        var isImportant by remember { mutableStateOf(false) }
        var solution by remember { mutableStateOf("") }

        val subTopics = remember(subjectId, topics) {
            topics.filter { it.subjectId == subjectId }
        }

        AlertDialog(
            onDismissRequest = { showAddPYQDialog = false },
            title = { Text("Add Previous Year Question", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Question Text") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = examType,
                            onValueChange = { examType = it },
                            label = { Text("Exam (Midterm/Final)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = marks,
                            onValueChange = { marks = it },
                            label = { Text("Marks") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (subTopics.isNotEmpty()) {
                        Text("Connect to Topic:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(subTopics) { t ->
                                FilterChip(
                                    selected = topicId == t.id,
                                    onClick = { topicId = t.id },
                                    label = { Text(t.name, maxLines = 1) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = solution,
                        onValueChange = { solution = it },
                        label = { Text("Solution / Model Answer") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (question.isNotBlank()) {
                            viewModel.addPYQ(
                                subjectId = subjectId,
                                topicId = topicId,
                                examType = examType,
                                year = year.toIntOrNull() ?: 2026,
                                question = question,
                                marks = marks.toIntOrNull() ?: 5,
                                difficulty = difficulty,
                                status = "UNSOLVED",
                                isImportant = isImportant,
                                solution = solution
                            )
                            showAddPYQDialog = false
                        }
                    }
                ) {
                    Text("Save PYQ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPYQDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Confirm Delete PYQ
    pyqToDelete?.let { pyq ->
        AlertDialog(
            onDismissRequest = { pyqToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete this PYQ?") },
            text = { Text("Are you sure you want to delete this question?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePYQ(pyq.id)
                        pyqToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
