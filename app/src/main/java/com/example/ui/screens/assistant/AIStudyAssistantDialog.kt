package com.example.ui.screens.assistant

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatMessageItem(
    val id: String,
    val sender: String, // "user" or "assistant"
    val text: String,
    val imageBitmap: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIStudyAssistantDialog(
    viewModel: PlannerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    val pyqs by viewModel.pyqs.collectAsStateWithLifecycle()
    val recommendations by viewModel.studyRecommendations.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessageItem(
                id = "welcome",
                sender = "assistant",
                text = "Hello ${currentUser?.name ?: "Student"}! I'm your Semester Study OS Assistant. Ask me anything about your syllabus, exam dates, study priorities, or attach a photo of your lecture notes/questions."
            )
        )
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    selectedBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }
    }

    // Speech to Text Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputPrompt = if (inputPrompt.isBlank()) spoken else "$inputPrompt $spoken"
            }
        }
    }

    fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to your Study Assistant...")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {}
    }

    fun sendMessage(textToSend: String? = null) {
        val prompt = (textToSend ?: inputPrompt).trim()
        if (prompt.isBlank() && selectedBitmap == null) return

        val currentBmp = selectedBitmap
        val userMsg = ChatMessageItem(
            id = "user_${System.currentTimeMillis()}",
            sender = "user",
            text = prompt,
            imageBitmap = currentBmp
        )
        messages.add(userMsg)
        inputPrompt = ""
        selectedBitmap = null
        selectedImageUri = null
        isLoading = true

        coroutineScope.launch {
            val responseText = withContext(Dispatchers.IO) {
                // Build student academic context
                val academicContext = buildString {
                    appendLine("STUDENT ACADEMIC CONTEXT:")
                    appendLine("Student Name: ${currentUser?.name ?: "Student"}")
                    appendLine("University: ${currentUser?.university ?: "University"}")
                    appendLine("Enrolled Subjects (${subjects.size}):")
                    subjects.forEach { s ->
                        appendLine("- ${s.name} (${s.courseCode}): ${s.credits} credits")
                    }
                    appendLine("Upcoming Exams (${exams.size}):")
                    exams.forEach { e ->
                        val sub = subjects.firstOrNull { it.id == e.subjectId }
                        appendLine("- ${sub?.name ?: e.name} on ${e.examDate} (${e.examType})")
                    }
                    val weakTopics = topics.filter { it.understanding == "WEAK" }
                    appendLine("Weak Topics (${weakTopics.size}): ${weakTopics.joinToString { it.name }}")
                    val completedTopics = topics.filter { it.status == "COMPLETED" }
                    appendLine("Completed Topics (${completedTopics.size}): ${completedTopics.take(10).joinToString { it.name }}")
                    appendLine("PYQs Count: ${pyqs.size} (Solved: ${pyqs.count { it.status == "SOLVED" }})")
                    if (recommendations.isNotEmpty()) {
                        appendLine("Top Priority Recommendations:")
                        recommendations.take(3).forEach { r ->
                            appendLine("- ${r.topicName}: ${r.priorityReason}")
                        }
                    }
                }

                val apiKey = try {
                    val k = BuildConfig.GEMINI_API_KEY
                    if (k.isNotBlank() && k != "MY_GEMINI_API_KEY") k else null
                } catch (_: Exception) {
                    null
                }

                if (apiKey != null) {
                    try {
                        val client = OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .build()

                        val jsonPayload = JSONObject().apply {
                            val contents = JSONArray().apply {
                                val contentObj = JSONObject().apply {
                                    val parts = JSONArray().apply {
                                        val sysPart = JSONObject().apply {
                                            put("text", "You are an expert AI academic study tutor for 'Semester Study OS'. Ground your answers in the student's actual enrolled subjects, exam dates, and syllabus topics.\n$academicContext\n\nStudent asks: $prompt")
                                        }
                                        parts.put(sysPart)

                                        if (currentBmp != null) {
                                            val baos = ByteArrayOutputStream()
                                            currentBmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                                            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                                            val imgPart = JSONObject().apply {
                                                val inlineData = JSONObject().apply {
                                                    put("mimeType", "image/jpeg")
                                                    put("data", b64)
                                                }
                                                put("inlineData", inlineData)
                                            }
                                            parts.put(imgPart)
                                        }
                                    }
                                    put("parts", parts)
                                }
                                put(contentObj)
                            }
                            put("contents", contents)
                        }

                        val request = Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        val response = client.newCall(request).execute()
                        val responseBody = response.body?.string() ?: ""
                        val rootJson = JSONObject(responseBody)
                        val candidates = rootJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            parts.getJSONObject(0).getString("text")
                        } else {
                            "I received your question. Could you clarify what specific topic or exam you'd like to prepare for?"
                        }
                    } catch (e: Exception) {
                        // Fallback response grounded in local state
                        generateLocalContextAnswer(prompt, subjects, exams, topics, pyqs, recommendations)
                    }
                } else {
                    generateLocalContextAnswer(prompt, subjects, exams, topics, pyqs, recommendations)
                }
            }

            messages.add(
                ChatMessageItem(
                    id = "assistant_${System.currentTimeMillis()}",
                    sender = "assistant",
                    text = responseText
                )
            )
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(PrimaryIndigo, Color(0xFF6366F1), Color(0xFF7C3AED))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "AI Study Assistant",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Gemini Flash • Multi-modal Tutor",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { messages.clear() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Color.White.copy(alpha = 0.8f))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Quick Prompts Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val promptList = listOf(
                        "What should I study today based on exams?",
                        "Which topics are currently weak?",
                        "Show repeated PYQs to practice.",
                        "How is my exam readiness?"
                    )
                    items(promptList) { qp ->
                        SuggestionChip(
                            onClick = { sendMessage(qp) },
                            label = { Text(qp, fontSize = 11.sp) }
                        )
                    }
                }

                // Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (msg.imageBitmap != null) {
                                        Image(
                                            bitmap = msg.imageBitmap.asImageBitmap(),
                                            contentDescription = "Attached Image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .padding(bottom = 6.dp)
                                        )
                                    }
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text("AI thinking...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Image Preview if selected
                if (selectedBitmap != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    bitmap = selectedBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Photo attached", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            IconButton(onClick = {
                                selectedBitmap = null
                                selectedImageUri = null
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }

                // Input Bar
                Surface(
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Image attachment button
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach image", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Mic button
                        IconButton(onClick = { startVoiceInput() }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Text input
                        OutlinedTextField(
                            value = inputPrompt,
                            onValueChange = { inputPrompt = it },
                            placeholder = { Text("Ask study question...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 120.dp),
                            shape = RoundedCornerShape(20.dp),
                            maxLines = 3
                        )

                        // Send button
                        FilledIconButton(
                            onClick = { sendMessage() },
                            enabled = (inputPrompt.isNotBlank() || selectedBitmap != null) && !isLoading,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun generateLocalContextAnswer(
    query: String,
    subjects: List<com.example.data.local.SubjectEntity>,
    exams: List<com.example.data.local.ExamCycleEntity>,
    topics: List<com.example.data.local.TopicEntity>,
    pyqs: List<com.example.data.local.PYQQuestionEntity>,
    recommendations: List<com.example.ui.viewmodel.TopicPYQAnalysis>
): String {
    val q = query.lowercase()
    return when {
        q.contains("exam") || q.contains("countdown") || q.contains("date") -> {
            if (exams.isEmpty()) {
                "You currently have no upcoming exams scheduled. Tap 'Exams' to add your midterm or final exam date!"
            } else {
                val next = exams.minByOrNull { it.examDate }
                val sub = subjects.firstOrNull { it.id == next?.subjectId }
                "Your next exam is ${sub?.name ?: next?.name} scheduled on ${next?.examDate} (${next?.examType}). You have ${exams.size} exam(s) scheduled in total."
            }
        }
        q.contains("weak") -> {
            val weaks = topics.filter { it.understanding == "WEAK" }
            if (weaks.isEmpty()) {
                "Great news! You don't have any topics marked as 'WEAK' right now."
            } else {
                "You have ${weaks.size} topic(s) marked as WEAK:\n" +
                        weaks.take(5).joinToString("\n") { "• ${it.name}" } +
                        "\nI recommend starting a 25-minute Deep Focus Session on these topics."
            }
        }
        q.contains("pyq") || q.contains("past") || q.contains("question") -> {
            if (pyqs.isEmpty()) {
                "No previous year questions added yet. You can add PYQs in the PYQ section to practice high-frequency questions."
            } else {
                val unsolved = pyqs.filter { it.status != "SOLVED" }
                "You have ${pyqs.size} total PYQs (${unsolved.size} unsolved). Topics with repeated questions are flagged with high priority."
            }
        }
        q.contains("study") || q.contains("today") || q.contains("priorit") -> {
            if (recommendations.isEmpty()) {
                "You have a clean slate! Add your syllabus topics and upcoming exam date so I can compute your prioritized study plan."
            } else {
                "Here are your highest priority study recommendations:\n" +
                        recommendations.take(3).joinToString("\n") { "• ${it.topicName}: ${it.priorityReason}" }
            }
        }
        else -> {
            "I've analyzed your academic OS data: You have ${subjects.size} subject(s), ${topics.size} topic(s), and ${exams.size} exam(s) scheduled. Ask me about your study plan, exam countdown, or attach a photo of notes or problems to solve!"
        }
    }
}
