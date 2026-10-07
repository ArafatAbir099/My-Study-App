package com.example.ui.screens.exams

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.CalendarTaskEntity
import com.example.data.local.ExamCycleEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import com.example.ui.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    viewModel: PlannerViewModel,
    onNavigateToCalendar: () -> Unit = {}
) {
    val context = LocalContext.current
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    var showAddExamDialog by remember { mutableStateOf(false) }
    var selectedExamForDetails by remember { mutableStateOf<ExamCycleEntity?>(null) }
    var examToEdit by remember { mutableStateOf<ExamCycleEntity?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    var customExamPdfPath by remember { mutableStateOf<String?>(null) }
    var customExamPdfName by remember { mutableStateOf<String?>(null) }

    val examCustomPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "Exam_Syllabus.pdf"
            customExamPdfName = fileName
            val localFile = com.example.util.SyllabusPdfParser.savePdfLocally(context, uri, fileName)
            customExamPdfPath = localFile?.absolutePath
        }
    }

    val todayStr = remember { viewModel.repository.todayStr() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // If viewing an exam details page
    if (selectedExamForDetails != null) {
        val currentExam = exams.firstOrNull { it.id == selectedExamForDetails!!.id } ?: selectedExamForDetails!!
        ExamDetailsView(
            exam = currentExam,
            subjects = subjects,
            topics = topics,
            tasks = tasks,
            todayStr = todayStr,
            onBack = { selectedExamForDetails = null },
            onEdit = {
                examToEdit = currentExam
                selectedExamForDetails = null
            },
            onDelete = {
                viewModel.deleteExam(currentExam.id)
                selectedExamForDetails = null
            },
            onRegenerate = {
                viewModel.regenerateExamPlan(currentExam) { success, msg ->
                    snackbarMessage = msg
                }
            },
            onToggleTask = { task ->
                viewModel.toggleTask(task)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Upcoming Exams", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "${exams.size} Exam(s) Scheduled • Automatic Revision Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAddExamDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_add_exam")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Exam", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("exams_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Snackbar message if any
            snackbarMessage?.let { msg ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { snackbarMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (exams.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.EventNote,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text("No Upcoming Exams Scheduled", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Add your midterm, quiz, or final exam date. The system will automatically build your study and revision schedule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { showAddExamDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Schedule Exam")
                            }
                        }
                    }
                }
            } else {
                // Sorted by exam date ascending
                val sortedExams = exams.sortedBy { it.examDate }

                items(sortedExams, key = { it.id }) { exam ->
                    val subject = subjects.firstOrNull { it.id == exam.subjectId }
                    val subjectTopics = topics.filter { it.subjectId == exam.subjectId }
                    val completedTopicsCount = subjectTopics.count { it.status == "COMPLETED" }
                    val progressPct = if (subjectTopics.isNotEmpty()) (completedTopicsCount * 100) / subjectTopics.size else 0

                    val countdownText = calculateCountdown(exam.examDate, todayStr, dateFormat)
                    val isUrgent = countdownText.contains("Today") || countdownText.contains("Tomorrow") || countdownText.contains("1 day") || countdownText.contains("2 days")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedExamForDetails = exam }
                            .testTag("exam_card_${exam.id}"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Top Row: Course name & Countdown badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (exam.examType.uppercase()) {
                                                "FINAL" -> MaterialTheme.colorScheme.errorContainer
                                                "QUIZ" -> MaterialTheme.colorScheme.tertiaryContainer
                                                else -> MaterialTheme.colorScheme.primaryContainer
                                            }
                                        ) {
                                            Text(
                                                text = exam.examType.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = when (exam.examType.uppercase()) {
                                                    "FINAL" -> MaterialTheme.colorScheme.onErrorContainer
                                                    "QUIZ" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = subject?.courseCode ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = subject?.name ?: exam.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Countdown Badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = countdownText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Date & Time row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formatExamDateDisplay(exam.examDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${exam.examTime} (${exam.durationMinutes}m)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Syllabus Progress
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Syllabus Progress: $completedTopicsCount / ${subjectTopics.size} Topics",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$progressPct%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "View Details & Study Plan →",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // ADD EXAM DIALOG (Part 4)
    // ==========================================
    if (showAddExamDialog) {
        var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0L) }
        var customCourseName by remember { mutableStateOf("") }
        var isCustomCourse by remember { mutableStateOf(subjects.isEmpty()) }
        var selectedBaseExamType by remember { mutableStateOf("Midterm") }
        var selectedQuizNumber by remember { mutableStateOf("Quiz 1") }
        var examDateInput by remember { mutableStateOf(todayStr) }
        var examTimeInput by remember { mutableStateOf("10:30 AM") }
        var durationMinutesInput by remember { mutableIntStateOf(60) }
        var examNotesInput by remember { mutableStateOf("") }
        var isDropdownExpanded by remember { mutableStateOf(false) }
        var inputError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(subjects) {
            if (selectedSubjectId == 0L && subjects.isNotEmpty()) {
                selectedSubjectId = subjects.first().id
            }
            if (subjects.isEmpty()) {
                isCustomCourse = true
            }
        }

        val connectedSubject = subjects.firstOrNull { it.id == selectedSubjectId }
        val finalExamType = if (selectedBaseExamType == "Quiz") selectedQuizNumber else selectedBaseExamType

        // Native DatePicker
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

        // Native TimePicker
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
            onDismissRequest = { showAddExamDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Schedule Upcoming Exam", fontWeight = FontWeight.Bold)
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

                    // Course / Subject selection
                    Text("Course / Subject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                    if (subjects.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isCustomCourse,
                                onClick = { isCustomCourse = false },
                                label = { Text("Select Existing") },
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
                            leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Exam Type (Quiz, Midterm, Final)
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

                    // Quiz Number Selector if Quiz is chosen
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

                    // Exam Date
                    Text("Exam Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = examDateInput,
                        onValueChange = {
                            examDateInput = it
                            inputError = null
                        },
                        label = { Text("Date (e.g. 15 October 2026 or YYYY-MM-DD)") },
                        leadingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { datePickerDialog.show() }) {
                                Text("Pick", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Date Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val cal = Calendar.getInstance()
                        val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
                        )
                        val in1WeekStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }.time
                        )
                        val in2WeeksStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 14) }.time
                        )

                        FilterChip(
                            selected = examDateInput == tomorrowStr,
                            onClick = { examDateInput = tomorrowStr; inputError = null },
                            label = { Text("Tomorrow", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = examDateInput == in1WeekStr,
                            onClick = { examDateInput = in1WeekStr; inputError = null },
                            label = { Text("In 1 Wk", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = examDateInput == in2WeeksStr,
                            onClick = { examDateInput = in2WeeksStr; inputError = null },
                            label = { Text("In 2 Wks", fontSize = 10.sp) }
                        )
                    }

                    // Exam Time
                    Text("Exam Time", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = examTimeInput,
                        onValueChange = {
                            examTimeInput = it
                            inputError = null
                        },
                        label = { Text("Exact Exam Time (e.g. 10:30 AM)") },
                        leadingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { timePickerDialog.show() }) {
                                Text("Pick", fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Time Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("09:00 AM", "10:30 AM", "02:00 PM", "04:30 PM").forEach { t ->
                            FilterChip(
                                selected = examTimeInput.equals(t, ignoreCase = true),
                                onClick = { examTimeInput = t; inputError = null },
                                label = { Text(t, fontSize = 10.sp) }
                            )
                        }
                    }

                    // Duration
                    Text("Duration (Optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(45 to "45 min", 60 to "1 hour", 120 to "2 hours", 180 to "3 hours").forEach { (min, label) ->
                            FilterChip(
                                selected = durationMinutesInput == min,
                                onClick = { durationMinutesInput = min },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Notes
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
                                inputError = "Please enter the course name (e.g. Data Structures)"
                                return@Button
                            }
                            viewModel.addExamWithAutoPlanAndCourse(
                                courseName = customCourseName.trim(),
                                examType = finalExamType,
                                examDate = normalizedDateStr,
                                examTime = examTimeInput.trim(),
                                durationMinutes = durationMinutesInput,
                                notes = examNotesInput.trim(),
                                customPdfPath = customExamPdfPath,
                                customPdfName = customExamPdfName
                            ) { success, msg ->
                                snackbarMessage = msg
                            }
                            showAddExamDialog = false
                        } else {
                            if (selectedSubjectId == 0L) {
                                inputError = "Please select a course or enter a new course name"
                                return@Button
                            }
                            viewModel.addExamWithAutoPlan(
                                subjectId = selectedSubjectId,
                                examType = finalExamType,
                                examDate = normalizedDateStr,
                                examTime = examTimeInput.trim(),
                                durationMinutes = durationMinutesInput,
                                notes = examNotesInput.trim(),
                                customPdfPath = customExamPdfPath,
                                customPdfName = customExamPdfName
                            ) { success, msg ->
                                snackbarMessage = msg
                            }
                            showAddExamDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_exam")
                ) {
                    Text("Schedule Exam & Auto-Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExamDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // EDIT EXAM DIALOG
    // ==========================================
    examToEdit?.let { exam ->
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

        val editCal = Calendar.getInstance().apply {
            val d = com.example.util.AutomaticRevisionPlanner.parseFlexibleDate(editDate)
            if (d != null) time = d
        }
        val editDatePicker = remember(context, editDate) {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selCal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                    editDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selCal.time)
                },
                editCal.get(Calendar.YEAR),
                editCal.get(Calendar.MONTH),
                editCal.get(Calendar.DAY_OF_MONTH)
            )
        }

        val editTimePicker = remember(context, editTime) {
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
                    editTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
                },
                10,
                30,
                false
            )
        }

        AlertDialog(
            onDismissRequest = { examToEdit = null },
            title = { Text("Edit Exam Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                        label = { Text("Exam Date") },
                        leadingIcon = {
                            IconButton(onClick = { editDatePicker.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { editDatePicker.show() }) { Text("Pick") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Exam Time") },
                        leadingIcon = {
                            IconButton(onClick = { editTimePicker.show() }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { editTimePicker.show() }) { Text("Pick") }
                        },
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
                        val normDate = com.example.util.AutomaticRevisionPlanner.normalizeDate(editDate)
                        val updated = exam.copy(
                            examDate = normDate,
                            examTime = editTime.trim(),
                            examType = finalEditType,
                            durationMinutes = editDuration
                        )
                        viewModel.updateExam(updated)
                        viewModel.regenerateExamPlan(updated) { _, msg ->
                            snackbarMessage = msg
                        }
                        examToEdit = null
                    }
                ) {
                    Text("Save & Update Plan")
                }
            },
            dismissButton = {
                TextButton(onClick = { examToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

// ==========================================
// EXAM DETAILS VIEW (Part 15)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDetailsView(
    exam: ExamCycleEntity,
    subjects: List<SubjectEntity>,
    topics: List<TopicEntity>,
    tasks: List<CalendarTaskEntity>,
    todayStr: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onToggleTask: (CalendarTaskEntity) -> Unit
) {
    val subject = subjects.firstOrNull { it.id == exam.subjectId }
    val subjectTopics = topics.filter { it.subjectId == exam.subjectId }
    val examTasks = tasks.filter { it.examId == exam.id || (it.subjectId == exam.subjectId && it.examId != null) }
    val todayTasks = examTasks.filter { it.date == todayStr }

    val completedCount = subjectTopics.count { it.status == "COMPLETED" }
    val totalCount = subjectTopics.size
    val progressPct = if (totalCount > 0) (completedCount * 100) / totalCount else 0

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val countdown = calculateCountdown(exam.examDate, todayStr, dateFormat)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exam.name.ifEmpty { "Exam Details" }, fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Exam")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Exam", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Header Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = exam.examType.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = countdown,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = subject?.name ?: exam.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (subject?.courseCode?.isNotBlank() == true) {
                            Text(
                                text = subject.courseCode,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(formatExamDateDisplay(exam.examDate), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${exam.examTime} (${exam.durationMinutes} min)", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Associated Syllabus Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Associated Syllabus", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        val syllabusName = exam.syllabusPdfName ?: subject?.syllabusPdfName
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = if (syllabusName != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syllabusName ?: "Using default course syllabus",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$totalCount Topics",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Syllabus Progress Card
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Syllabus Mastery Progress", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("$progressPct%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Completed: $completedCount Topics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Remaining: ${totalCount - completedCount} Topics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Today's Tasks for this Exam
            if (todayTasks.isNotEmpty()) {
                item {
                    Text("Today's Tasks for this Exam", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                items(todayTasks) { task ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onToggleTask(task) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall,
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

            // Regenerate Plan Action Banner
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Need to adjust your timetable?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Recalculate remaining sessions based on latest progress.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = onRegenerate,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Regenerate Plan", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Full Study Plan Timeline
            item {
                Text("Study & Revision Roadmap (${examTasks.size} Sessions)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            if (examTasks.isEmpty()) {
                item {
                    Text(
                        "No study plan sessions generated. Click 'Regenerate Plan' above to build a study roadmap from the course syllabus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val groupedTasks = examTasks.sortedBy { it.date }.groupBy { it.date }
                groupedTasks.forEach { (dateStr, dayTasks) ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = formatExamDateDisplay(dateStr),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            dayTasks.forEach { task ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { onToggleTask(task) }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = task.title,
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (task.notes.isNotBlank()) {
                                                Text(
                                                    text = task.notes,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (task.taskType) {
                                                "EXAM" -> MaterialTheme.colorScheme.errorContainer
                                                "REVISION" -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        ) {
                                            Text(
                                                text = task.taskType,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    }
}

private fun calculateCountdown(examDateStr: String, todayStr: String, dateFormat: SimpleDateFormat): String {
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

private fun formatExamDateDisplay(dateStr: String): String {
    return try {
        val date = com.example.util.AutomaticRevisionPlanner.parseFlexibleDate(dateStr)
        if (date != null) {
            SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH).format(date)
        } else {
            dateStr
        }
    } catch (_: Exception) {
        dateStr
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    name = it.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path?.let { p ->
            val cut = p.lastIndexOf('/')
            if (cut != -1) p.substring(cut + 1) else p
        }
    }
    return name
}
