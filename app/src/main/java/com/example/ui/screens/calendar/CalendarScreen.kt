package com.example.ui.screens.calendar

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CalendarTaskEntity
import com.example.ui.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: PlannerViewModel,
    onStartFocusSession: (title: String, duration: Int, topicId: Long?, subjectId: Long?) -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val haptic = LocalHapticFeedback.current

    var selectedDateStr by remember { mutableStateOf(viewModel.repository.todayStr()) }
    var calendarViewMode by remember { mutableStateOf("DAY") } // "DAY", "WEEK", "MONTH"
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToReschedule by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var taskToEdit by remember { mutableStateOf<CalendarTaskEntity?>(null) }

    // Generate dates for current week or month
    val daysList = remember(selectedDateStr) {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..14) {
            list.add(dateFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val filteredTasks = remember(tasks, selectedDateStr, calendarViewMode) {
        if (calendarViewMode == "DAY") {
            tasks.filter { it.date == selectedDateStr }
        } else {
            tasks
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTaskDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Study Task") },
                modifier = Modifier.testTag("fab_add_task")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .testTag("calendar_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Calendar View Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manual Study Calendar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("DAY", "WEEK", "ALL").forEach { mode ->
                        FilterChip(
                            selected = calendarViewMode == mode,
                            onClick = { calendarViewMode = mode },
                            label = { Text(mode, fontSize = 11.sp) },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You control your timetable. Plan when and what to study.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Date Scroller
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(daysList) { dStr ->
                    val isSelected = dStr == selectedDateStr
                    val dateObj = try { dateFormat.parse(dStr) } catch (_: Exception) { null }
                    val label = if (dateObj != null) displayFormat.format(dateObj) else dStr

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedDateStr = dStr
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label.substringBefore(","),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = label.substringAfter(", ").take(6),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tasks List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No study tasks for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showAddTaskDialog = true }) {
                            Text("Create Study Session")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val sub = subjects.firstOrNull { it.id == task.subjectId }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_card_${task.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.toggleTask(task)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${task.date} • ${task.startTime} - ${task.endTime} (${task.durationMinutes}m)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (sub != null) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Text(
                                                        text = sub.courseCode.ifEmpty { sub.name },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Action buttons: Focus Session / Edit / Reschedule / Delete
                                    IconButton(
                                        onClick = {
                                            onStartFocusSession(task.title, task.durationMinutes, task.topicId, task.subjectId)
                                        }
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = "Focus", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(onClick = { taskToEdit = task }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Task", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(onClick = { taskToReschedule = task }) {
                                        Icon(Icons.Default.Update, contentDescription = "Reschedule", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                if (task.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = task.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 48.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var selectedSubjectId by remember { mutableStateOf<Long?>(subjects.firstOrNull()?.id) }
        var selectedTopicId by remember { mutableStateOf<Long?>(null) }
        var date by remember { mutableStateOf(selectedDateStr) }
        var startTime by remember { mutableStateOf("09:00") }
        var endTime by remember { mutableStateOf("10:00") }
        var durationMinutes by remember { mutableIntStateOf(60) }
        var notes by remember { mutableStateOf("") }

        val subTopics = remember(selectedSubjectId, topics) {
            topics.filter { it.subjectId == selectedSubjectId }
        }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Add Manual Study Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        placeholder = { Text("e.g. Data Structures -> Linked List Practice") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Subject selector
                    if (subjects.isNotEmpty()) {
                        Text("Subject:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjects) { s ->
                                FilterChip(
                                    selected = selectedSubjectId == s.id,
                                    onClick = {
                                        selectedSubjectId = s.id
                                        selectedTopicId = null
                                    },
                                    label = { Text(s.name, maxLines = 1) }
                                )
                            }
                        }
                    }

                    // Topic selector
                    if (subTopics.isNotEmpty()) {
                        Text("Connect to Topic:", style = MaterialTheme.typography.labelSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subTopics) { t ->
                                FilterChip(
                                    selected = selectedTopicId == t.id,
                                    onClick = { selectedTopicId = t.id },
                                    label = { Text(t.name, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Time (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Target Exercises") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(
                                title = title,
                                subjectId = selectedSubjectId,
                                topicId = selectedTopicId,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                                durationMinutes = durationMinutes,
                                notes = notes
                            )
                            showAddTaskDialog = false
                        }
                    }
                ) {
                    Text("Save Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reschedule Dialog
    taskToReschedule?.let { task ->
        var newDate by remember { mutableStateOf(viewModel.repository.todayStr()) }
        AlertDialog(
            onDismissRequest = { taskToReschedule = null },
            title = { Text("Reschedule Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select when to move '${task.title}':", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = {
                            viewModel.rescheduleTask(task, viewModel.repository.todayStr())
                            taskToReschedule = null
                        }) {
                            Text("Today")
                        }
                        Button(onClick = {
                            val cal = Calendar.getInstance()
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            viewModel.rescheduleTask(task, dateFormat.format(cal.time))
                            taskToReschedule = null
                        }) {
                            Text("Tomorrow")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("Or enter Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rescheduleTask(task, newDate)
                    taskToReschedule = null
                }) {
                    Text("Move Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToReschedule = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        var editTitle by remember { mutableStateOf(task.title) }
        var editDate by remember { mutableStateOf(task.date) }
        var editStartTime by remember { mutableStateOf(task.startTime) }
        var editEndTime by remember { mutableStateOf(task.endTime) }
        var editDuration by remember { mutableIntStateOf(task.durationMinutes) }
        var editNotes by remember { mutableStateOf(task.notes) }

        AlertDialog(
            onDismissRequest = { taskToEdit = null },
            icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Edit Study Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editDate,
                            onValueChange = { editDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editStartTime,
                            onValueChange = { editStartTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editEndTime,
                            onValueChange = { editEndTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editDuration.toString(),
                            onValueChange = { editDuration = it.toIntOrNull() ?: 60 },
                            label = { Text("Duration (min)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes / Problem set") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateTask(
                                task.copy(
                                    title = editTitle,
                                    date = editDate,
                                    startTime = editStartTime,
                                    endTime = editEndTime,
                                    durationMinutes = editDuration,
                                    notes = editNotes
                                )
                            )
                            taskToEdit = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToEdit = null }) { Text("Cancel") }
            }
        )
    }
}
