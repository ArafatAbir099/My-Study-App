package com.example.ui.screens.syllabus

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import com.example.ui.viewmodel.PlannerViewModel
import com.example.util.SyllabusParseResult
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySyllabusScreen(
    viewModel: PlannerViewModel,
    onNavigateToSubjectDetail: (SubjectEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val semesters by viewModel.semesters.collectAsStateWithLifecycle()
    val activeSemester = semesters.firstOrNull { !it.isArchived } ?: semesters.firstOrNull()

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResultMsg by remember { mutableStateOf<String?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var lastParseResult by remember { mutableStateOf<SyllabusParseResult?>(null) }

    // Dialogs
    var showUploadSemesterDialog by remember { mutableStateOf(false) }
    var showManualTextDialog by remember { mutableStateOf(false) }
    var manualTextCourseName by remember { mutableStateOf("") }
    var manualTextContent by remember { mutableStateOf("") }

    var selectedSubjectForTopics by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToReplacePdf by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToViewSyllabus by remember { mutableStateOf<SubjectEntity?>(null) }

    // PDF Launcher for Complete Semester PDF
    val semesterPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isAnalyzing = true
            val fileName = getFileName(context, uri) ?: "Semester_Syllabus.pdf"
            viewModel.uploadSemesterSyllabus(uri, fileName) { result ->
                isAnalyzing = false
                lastParseResult = result
                analysisResultMsg = result.message
                showResultDialog = true
            }
        }
    }

    // PDF Launcher for Single Course PDF
    val coursePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val targetSubject = subjectToReplacePdf
        if (uri != null && targetSubject != null) {
            isAnalyzing = true
            val fileName = getFileName(context, uri) ?: "${targetSubject.name}_Syllabus.pdf"
            viewModel.attachCourseSyllabus(targetSubject, uri, fileName) { result ->
                isAnalyzing = false
                lastParseResult = result
                analysisResultMsg = result.message
                showResultDialog = true
                subjectToReplacePdf = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("My Syllabus", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(
                            text = "${activeSemester?.name ?: "Current Semester"} • Syllabus Library",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showUploadSemesterDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_upload_syllabus")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Syllabus", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                .testTag("my_syllabus_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Overview Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "SEMESTER SYLLABUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                    )
                                    Text(
                                        text = "${subjects.size} Courses Enrolled",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            val coursesWithSyllabus = subjects.count { !it.syllabusPdfName.isNullOrEmpty() || !it.syllabusRawText.isNullOrEmpty() }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = "$coursesWithSyllabus / ${subjects.size} Syllabi",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Provide your semester syllabus PDF or individual course PDFs. The system analyzes units and topics to automatically calculate study and revision roadmaps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Loading / Analyzing Banner
            if (isAnalyzing) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Analyzing Syllabus Content...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Identifying courses, units, and topics...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            // Empty state when no courses
            if (subjects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No courses in this semester yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Upload a Complete Semester PDF or paste syllabus to get started.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { showUploadSemesterDialog = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Semester Syllabus PDF")
                            }
                        }
                    }
                }
            } else {
                // List of Courses with Syllabus status
                items(subjects, key = { it.id }) { subject ->
                    val subjectTopics = topics.filter { it.subjectId == subject.id }
                    val hasPdf = !subject.syllabusPdfName.isNullOrEmpty()
                    val hasText = !subject.syllabusRawText.isNullOrEmpty()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("course_syllabus_card_${subject.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Course Name & Code Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (subject.courseCode.isNotBlank()) {
                                        Text(
                                            text = subject.courseCode,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Topic Count Pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (subjectTopics.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${subjectTopics.size} Topics",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (subjectTopics.isNotEmpty()) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Syllabus PDF Badge (Clickable to View)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (hasPdf || hasText) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = hasPdf || hasText) {
                                        subjectToViewSyllabus = subject
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (hasPdf) Icons.Default.PictureAsPdf else if (hasText) Icons.Default.Description else Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        tint = if (hasPdf) MaterialTheme.colorScheme.error else if (hasText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (hasPdf) "Syllabus: ${subject.syllabusPdfName}" else if (hasText) "Syllabus: Text Analyzed" else "No Syllabus Attached",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (hasPdf || hasText) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (hasPdf || hasText) {
                                        Text(
                                            text = "View",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // View Extracted Topics
                                FilledTonalButton(
                                    onClick = { selectedSubjectForTopics = subject },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Topics", fontSize = 12.sp)
                                }

                                // Open / View PDF
                                if (hasPdf || hasText) {
                                    OutlinedButton(
                                        onClick = { subjectToViewSyllabus = subject },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View PDF", fontSize = 12.sp)
                                    }
                                }

                                // Upload / Replace Syllabus
                                OutlinedButton(
                                    onClick = {
                                        subjectToReplacePdf = subject
                                        coursePdfLauncher.launch("application/pdf")
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (hasPdf) Icons.Default.Sync else Icons.Default.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (hasPdf) "Replace" else "Upload PDF", fontSize = 12.sp)
                                }

                                // Re-analyze syllabus
                                if (hasPdf || hasText) {
                                    IconButton(
                                        onClick = {
                                            isAnalyzing = true
                                            viewModel.reanalyzeCourseSyllabus(subject) { result ->
                                                isAnalyzing = false
                                                lastParseResult = result
                                                analysisResultMsg = result.message
                                                showResultDialog = true
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Re-analyze syllabus",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Delete syllabus option if attached
                                if (hasPdf || hasText) {
                                    IconButton(
                                        onClick = { viewModel.deleteCourseSyllabus(subject) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete syllabus",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
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

    // ==========================================
    // UPLOAD SEMESTER SYLLABUS MODAL
    // ==========================================
    if (showUploadSemesterDialog) {
        AlertDialog(
            onDismissRequest = { showUploadSemesterDialog = false },
            title = {
                Text("Upload Semester Syllabus", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "You can provide your syllabus using either method below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option A: Complete Semester PDF
                    Card(
                        onClick = {
                            showUploadSemesterDialog = false
                            semesterPdfLauncher.launch("application/pdf")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Option A: Complete Semester PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Upload 1 PDF with all courses. AI automatically separates and extracts units & topics.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Option B: Manual text entry
                    Card(
                        onClick = {
                            showUploadSemesterDialog = false
                            showManualTextDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Option B: Paste / Type Syllabus Text", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Paste course units and topics directly if your PDF is scanned or image-based.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUploadSemesterDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // MANUAL SYLLABUS TEXT INPUT DIALOG
    // ==========================================
    if (showManualTextDialog) {
        AlertDialog(
            onDismissRequest = { showManualTextDialog = false },
            title = { Text("Paste / Type Syllabus Text", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Paste your syllabus units and topics below. The analyzer will parse units, chapters, and topics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = manualTextCourseName,
                        onValueChange = { manualTextCourseName = it },
                        label = { Text("Course Name (e.g. Data Structures)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualTextContent,
                        onValueChange = { manualTextContent = it },
                        label = { Text("Syllabus Content") },
                        placeholder = {
                            Text("Unit 1: Introduction\n- Algorithm\n- Complexity\n- Big O\n\nUnit 2: Linear Data Structures\n- Array\n- Linked List\n- Stack\n- Queue")
                        },
                        minLines = 8,
                        maxLines = 14,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = com.example.util.SyllabusPdfParser.parseSyllabusContent(
                            manualTextContent,
                            manualTextCourseName
                        )
                        if (parsed.isSuccess && parsed.courses.isNotEmpty()) {
                            // Insert course, chapters, topics
                            val course = parsed.courses.first()
                            viewModel.addSubject(
                                name = course.courseName.ifEmpty { manualTextCourseName.ifEmpty { "New Course" } },
                                code = course.courseCode,
                                credits = 3.0,
                                teacher = "",
                                colorHex = "#3B82F6",
                                iconName = "menu_book",
                                notes = "Analyzed from manual syllabus entry"
                            )
                            analysisResultMsg = parsed.message
                            showResultDialog = true
                        } else {
                            analysisResultMsg = parsed.message
                            showResultDialog = true
                        }
                        showManualTextDialog = false
                    }
                ) {
                    Text("Analyze & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualTextDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ==========================================
    // PARSE RESULT NOTIFICATION DIALOG
    // ==========================================
    if (showResultDialog) {
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            icon = {
                Icon(
                    imageVector = if (lastParseResult?.isSuccess == true) Icons.Default.CheckCircle else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (lastParseResult?.isSuccess == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(if (lastParseResult?.isSuccess == true) "Syllabus Analyzed" else "Notice", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(analysisResultMsg ?: "")
                    if (lastParseResult?.courses?.isNotEmpty() == true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Extracted Courses:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        lastParseResult!!.courses.forEach { c ->
                            Text("• ${c.courseName}: ${c.totalTopicsCount} topics across ${c.units.size} units", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showResultDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // ==========================================
    // VIEW EXTRACTED TOPICS DIALOG
    // ==========================================
    selectedSubjectForTopics?.let { subject ->
        val subjectTopics = topics.filter { it.subjectId == subject.id }

        AlertDialog(
            onDismissRequest = { selectedSubjectForTopics = null },
            title = {
                Column {
                    Text(subject.name, fontWeight = FontWeight.Bold)
                    Text(
                        "${subjectTopics.size} Topics Extracted",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (subjectTopics.isEmpty()) {
                        Text(
                            "No topics extracted yet. Upload a syllabus PDF or add topics in the Subjects workspace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        subjectTopics.forEachIndexed { index, topic ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(topic.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                                        if (topic.description.isNotBlank()) {
                                            Text(topic.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (topic.status == "COMPLETED") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ) {
                                        Text(
                                            text = if (topic.status == "COMPLETED") "Done" else "Pending",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedSubjectForTopics = null }) {
                    Text("Close")
                }
            }
        )
    }

    // ==========================================
    // VIEW SYLLABUS PDF / CONTENT DIALOG
    // ==========================================
    subjectToViewSyllabus?.let { subject ->
        val subjectTopics = topics.filter { it.subjectId == subject.id }
        AlertDialog(
            onDismissRequest = { subjectToViewSyllabus = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Column {
                    Text(subject.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = subject.syllabusPdfName ?: "Syllabus Document",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Syllabus Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Course: ${subject.name} (${subject.courseCode})", style = MaterialTheme.typography.bodySmall)
                            Text("• File: ${subject.syllabusPdfName ?: "Manual/Raw text"}", style = MaterialTheme.typography.bodySmall)
                            Text("• Extracted Topics: ${subjectTopics.size} topics", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text("Extracted Syllabus Content:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                    val contentToShow = subject.syllabusRawText ?: subjectTopics.joinToString("\n") { "• ${it.name}: ${it.description.ifEmpty { "Curriculum topic" }}" }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (contentToShow.isNotBlank()) contentToShow else "No raw syllabus text saved. Topics are available in the Topics tab.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isAnalyzing = true
                        val targetSub = subject
                        subjectToViewSyllabus = null
                        viewModel.reanalyzeCourseSyllabus(targetSub) { result ->
                            isAnalyzing = false
                            lastParseResult = result
                            analysisResultMsg = result.message
                            showResultDialog = true
                        }
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-Analyze")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToViewSyllabus = null }) {
                    Text("Close")
                }
            }
        )
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
