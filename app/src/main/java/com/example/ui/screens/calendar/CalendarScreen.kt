package com.example.ui.screens.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CalendarTaskEntity
import com.example.data.local.ExamCycleEntity
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
    val exams by viewModel.exams.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val haptic = LocalHapticFeedback.current

    var selectedDateStr by remember { mutableStateOf(viewModel.repository.todayStr()) }
    var calendarViewMode by remember { mutableStateOf("DAY") } // "DAY", "WEEK", "MONTH"
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddExamCalendarDialog by remember { mutableStateOf(false) }
    var taskToReschedule by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var taskToEdit by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var examForDetails by remember { mutableStateOf<ExamCycleEntity?>(null) }
    var examToEditInCalendar by remember { mutableStateOf<ExamCycleEntity?>(null) }
    var examToDeleteInCalendar by remember { mutableStateOf<ExamCycleEntity?>(null) }
    val todayStr = remember { viewModel.repository.todayStr() }

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

            // ==========================================
            // UPCOMING EXAMS ON SELECTED DATE (Part 5)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Exams",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { showAddExamCalendarDialog = true },
                    modifier = Modifier.testTag("btn_calendar_schedule_exam")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule Exam", fontSize = 12.sp)
                }
            }

            val examsForDate = exams.filter { if (calendarViewMode == "DAY") it.examDate == selectedDateStr else true }
            if (examsForDate.isNotEmpty()) {
                examsForDate.forEach { exam ->
                    val examSub = subjects.firstOrNull { it.id == exam.subjectId }
                    val countdownStr = calculateCalendarCountdown(exam.examDate, todayStr, dateFormat)
                    val examDateHeader = formatCalendarExamHeader(exam.examDate)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { examForDetails = exam }
                            .testTag("calendar_exam_card_${exam.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        ),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Date Header (e.g. 15 OCTOBER) & Countdown Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                ) {
                                    Text(
                                        text = examDateHeader,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = countdownStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Course Name
                            Text(
                                text = examSub?.name ?: exam.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            // Exam Type & Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = exam.examType,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    if (examSub?.courseCode?.isNotBlank() == true) {
                                        Text(
                                            text = " • ${examSub.courseCode}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Text(
                                    text = "${exam.examTime} (${exam.durationMinutes}m)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons row (Edit date/time, change exam type, delete, details)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { examForDetails = exam },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Details", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { examToEditInCalendar = exam },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Exam", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { examToDeleteInCalendar = exam },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Exam",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

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

    // ==========================================
    // EXAM DETAILS DIALOG (Part 5)
    // ==========================================
    examForDetails?.let { exam ->
        val sub = subjects.firstOrNull { it.id == exam.subjectId }
        val subTopics = topics.filter { it.subjectId == exam.subjectId }
        val countdown = calculateCalendarCountdown(exam.examDate, todayStr, dateFormat)

        AlertDialog(
            onDismissRequest = { examForDetails = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Column {
                    Text(sub?.name ?: exam.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${exam.examType} Exam Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Exam Countdown:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(countdown, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Date: ${formatCalendarExamHeader(exam.examDate)} (${exam.examDate})", style = MaterialTheme.typography.bodySmall)
                            Text("• Time: ${exam.examTime} (${exam.durationMinutes} min)", style = MaterialTheme.typography.bodySmall)
                            Text("• Course Code: ${sub?.courseCode?.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                            Text("• Connected Syllabus: ${subTopics.size} Topics", style = MaterialTheme.typography.bodySmall)
                            if (exam.notes.isNotBlank()) {
                                Text("• Notes: ${exam.notes}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentExam = exam
                        examForDetails = null
                        examToEditInCalendar = currentExam
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Exam")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val currentExam = exam
                        examForDetails = null
                        examToDeleteInCalendar = currentExam
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // ==========================================
    // EDIT EXAM DIALOG (Part 5)
    // ==========================================
    examToEditInCalendar?.let { exam ->
        var editDate by remember { mutableStateOf(exam.examDate) }
        var editTime by remember { mutableStateOf(exam.examTime) }
        var editBaseType by remember {
            mutableStateOf(if (exam.examType.startsWith("Quiz", ignoreCase = true)) "Quiz" else if (exam.examType.contains("Final", ignoreCase = true)) "Final" else "Midterm")
        }
        var editQuizNumber by remember {
            mutableStateOf(if (exam.examType.startsWith("Quiz", ignoreCase = true)) exam.examType else "Quiz 1")
        }
        var editDuration by remember { mutableIntStateOf(exam.durationMinutes) }

        val finalEditType = if (editBaseType == "Quiz") editQuizNumber else editBaseType

        AlertDialog(
            onDismissRequest = { examToEditInCalendar = null },
            icon = { Icon(Icons.Default.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Edit Exam Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Exam Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("Quiz", "Midterm", "Final").forEach { type ->
                            FilterChip(
                                selected = editBaseType == type,
                                onClick = { editBaseType = type },
                                label = { Text(type) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (editBaseType == "Quiz") {
                        Text("Quiz Number", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Quiz 1", "Quiz 2", "Quiz 3", "Quiz 4").forEach { qNum ->
                                FilterChip(
                                    selected = editQuizNumber == qNum,
                                    onClick = { editQuizNumber = qNum },
                                    label = { Text(qNum, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editDate,
                        onValueChange = { editDate = it },
                        label = { Text("Exam Date (YYYY-MM-DD)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Exam Time (e.g. 10:30 AM)") },
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(45 to "45m", 60 to "1h", 120 to "2h", 180 to "3h").forEach { (min, label) ->
                            FilterChip(
                                selected = editDuration == min,
                                onClick = { editDuration = min },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = exam.copy(
                            examDate = editDate.trim(),
                            examTime = editTime.trim(),
                            examType = finalEditType,
                            durationMinutes = editDuration
                        )
                        viewModel.updateExam(updated)
                        viewModel.regenerateExamPlan(updated) { _, _ -> }
                        examToEditInCalendar = null
                    }
                ) {
                    Text("Save & Update Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { examToEditInCalendar = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // DELETE EXAM CONFIRMATION (Part 5)
    // ==========================================
    examToDeleteInCalendar?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDeleteInCalendar = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Exam?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete this ${exam.examType} exam? Any scheduled revision and study tasks for this exam will also be removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExam(exam.id)
                        examToDeleteInCalendar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Exam")
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDeleteInCalendar = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==========================================
    // SCHEDULE EXAM ON CALENDAR DIALOG
    // ==========================================
    if (showAddExamCalendarDialog) {
        val context = LocalContext.current
        var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
        var customCourseName by remember { mutableStateOf("") }
        var isCustomCourse by remember { mutableStateOf(subjects.isEmpty()) }
        var selectedBaseExamType by remember { mutableStateOf("Midterm") }
        var selectedQuizNumber by remember { mutableStateOf("Quiz 1") }
        var examDateInput by remember { mutableStateOf(selectedDateStr) }
        var examTimeInput by remember { mutableStateOf("10:30 AM") }
        var durationMinutesInput by remember { mutableIntStateOf(60) }
        var examNotesInput by remember { mutableStateOf("") }
        var isDropdownExpanded by remember { mutableStateOf(false) }
        var inputError by remember { mutableStateOf<String?>(null) }

        val connectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }
        val finalExamType = if (selectedBaseExamType == "Quiz") selectedQuizNumber else selectedBaseExamType

        val initialCal = Calendar.getInstance().apply {
            val d = com.example.util.AutomaticRevisionPlanner.parseFlexibleDate(examDateInput)
            if (d != null) time = d
        }
        val datePickerDialog = remember(context, examDateInput) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    examDateInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selCal.time)
                    inputError = null
                },
                initialCal.get(Calendar.YEAR),
                initialCal.get(Calendar.MONTH),
                initialCal.get(Calendar.DAY_OF_MONTH)
            )
        }

        val timePickerDialog = remember(context, examTimeInput) {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val isPm = hourOfDay >= 12
                    val displayHour = when {
                        hourOfDay == 0 -> 12
                        hourOfDay > 12 -> hourOfDay - 12
                        else -> hourOfDay
                    }
                    val amPm = if (isPm) "PM" else "AM"
                    examTimeInput = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
                    inputError = null
                },
                10,
                30,
                false
            )
        }

        AlertDialog(
            onDismissRequest = { showAddExamCalendarDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Exam on Calendar", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (inputError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = inputError!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Text("Course / Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    if (subjects.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isCustomCourse,
                                onClick = { isCustomCourse = false },
                                label = { Text("Select Course") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isCustomCourse,
                                onClick = { isCustomCourse = true },
                                label = { Text("New Course") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!isCustomCourse && subjects.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { isDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = connectedSubject?.name ?: "Select Course ▼",
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                subjects.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = {
                                            selectedSubjectId = s.id
                                            isDropdownExpanded = false
                                            inputError = null
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customCourseName,
                            onValueChange = {
                                customCourseName = it
                                inputError = null
                            },
                            label = { Text("Course Name (e.g. Data Structures)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text("Exam Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Quiz", "Midterm", "Final").forEach { type ->
                            FilterChip(
                                selected = selectedBaseExamType == type,
                                onClick = { selectedBaseExamType = type },
                                label = { Text(type) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (selectedBaseExamType == "Quiz") {
                        Text("Quiz Number", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Quiz 1", "Quiz 2", "Quiz 3", "Quiz 4").forEach { qNum ->
                                FilterChip(
                                    selected = selectedQuizNumber == qNum,
                                    onClick = { selectedQuizNumber = qNum },
                                    label = { Text(qNum, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Text("Exam Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = examDateInput,
                        onValueChange = {
                            examDateInput = it
                            inputError = null
                        },
                        label = { Text("Exam Date (e.g. 15 October 2026)") },
                        leadingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { datePickerDialog.show() }) { Text("Pick") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Exam Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = examTimeInput,
                        onValueChange = {
                            examTimeInput = it
                            inputError = null
                        },
                        label = { Text("Exact Time (e.g. 10:30 AM)") },
                        leadingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { timePickerDialog.show() }) { Text("Pick") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("09:00 AM", "10:30 AM", "02:00 PM", "04:30 PM").forEach { t ->
                            FilterChip(
                                selected = examTimeInput.equals(t, ignoreCase = true),
                                onClick = { examTimeInput = t },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(45 to "45 min", 60 to "1 hour", 120 to "2 hours", 180 to "3 hours").forEach { (min, label) ->
                            FilterChip(
                                selected = durationMinutesInput == min,
                                onClick = { durationMinutesInput = min },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = examNotesInput,
                        onValueChange = { examNotesInput = it },
                        label = { Text("Notes / Venue (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedExamDate = com.example.util.AutomaticRevisionPlanner.parseFlexibleDate(examDateInput)
                        if (parsedExamDate == null) {
                            inputError = "Please enter or pick a valid exam date"
                            return@Button
                        }
                        val normalizedDateStr = com.example.util.AutomaticRevisionPlanner.normalizeDate(examDateInput)

                        if (isCustomCourse) {
                            if (customCourseName.isBlank()) {
                                inputError = "Please enter the course name"
                                return@Button
                            }
                            viewModel.addExamWithAutoPlanAndCourse(
                                courseName = customCourseName.trim(),
                                examType = finalExamType,
                                examDate = normalizedDateStr,
                                examTime = examTimeInput.trim(),
                                durationMinutes = durationMinutesInput,
                                notes = examNotesInput.trim()
                            ) { _, _ -> }
                            showAddExamCalendarDialog = false
                        } else {
                            if (selectedSubjectId == 0L) {
                                inputError = "Please select or enter a course"
                                return@Button
                            }
                            viewModel.addExamWithAutoPlan(
                                subjectId = selectedSubjectId,
                                examType = finalExamType,
                                examDate = normalizedDateStr,
                                examTime = examTimeInput.trim(),
                                durationMinutes = durationMinutesInput,
                                notes = examNotesInput.trim()
                            ) { _, _ -> }
                            showAddExamCalendarDialog = false
                        }
                    }
                ) {
                    Text("Schedule Exam & Auto-Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExamCalendarDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun calculateCalendarCountdown(examDateStr: String, todayStr: String, dateFormat: SimpleDateFormat): String {
    return try {
        val today = com.example.util.AutomaticRevisionPlanner.parseFlexibleDate(todayStr) ?: Date()
        val examDate = com.example.util.AutomaticRevisionPlanner.parseFlexibleDate(examDateStr) ?: Date()
        val diff = examDate.time - today.time
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        when {
            days < 0 -> "Exam completed"
            days == 0 -> "Today"
            days == 1 -> "Tomorrow"
            else -> "$days days remaining"
        }
    } catch (_: Exception) {
        examDateStr
    }
}

private fun formatCalendarExamHeader(dateStr: String): String {
    return try {
        val date = com.example.util.AutomaticRevisionPlanner.parseFlexibleDate(dateStr)
        if (date != null) {
            SimpleDateFormat("d MMMM", Locale.getDefault()).format(date).uppercase()
        } else {
            dateStr.uppercase()
        }
    } catch (_: Exception) {
        dateStr.uppercase()
    }
}
