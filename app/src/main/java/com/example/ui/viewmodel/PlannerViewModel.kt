package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.StudyPlannerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

data class DayActivitySummary(
    val dateStr: String,
    val count: Int,
    val totalMinutes: Int,
    val tasksCompleted: Int,
    val revisionsCompleted: Int,
    val pyqsPracticed: Int,
    val focusSessions: Int,
    val intensityLevel: Int // 0 to 4
)

data class TopicPYQAnalysis(
    val topicId: Long,
    val topicName: String,
    val appearanceCount: Int,
    val yearsAppeared: List<Int>,
    val lastAppearance: String,
    val solvedCount: Int,
    val unsolvedCount: Int,
    val importance: String,
    val understanding: String,
    val priorityScore: Int, // Calculated for "What should I study?"
    val priorityReason: String
)

data class ExamReadinessScore(
    val examName: String,
    val overallScore: Int, // 0 - 100
    val syllabusScore: Int,
    val understandingScore: Int,
    val revisionScore: Int,
    val pyqScore: Int,
    val daysLeft: Int?,
    val mainWeakness: String
)

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = StudyPlannerRepository(db)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Current User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    // Active Semester
    private val _selectedSemesterId = MutableStateFlow<Long?>(null)
    val selectedSemesterId = _selectedSemesterId.asStateFlow()

    // Data streams
    val allUsers: StateFlow<List<UserEntity>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val semesters: StateFlow<List<SemesterEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getSemesters(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getSubjectsByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topics: StateFlow<List<TopicEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getTopicsByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<ExamCycleEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getExamsByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<CalendarTaskEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getTasksByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revisions: StateFlow<List<RevisionItemEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getRevisionsByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pyqs: StateFlow<List<PYQQuestionEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getPYQsByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val resources: StateFlow<List<StudyResourceEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getResourcesByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<StudyNoteEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getNotesByUser(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyActivities: StateFlow<List<StudyActivityEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getStudyActivities(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Heatmap filter range (3_MONTHS, 6_MONTHS, 1_YEAR, ALL)
    val heatmapFilter = MutableStateFlow("3_MONTHS")

    // Global Search State
    val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // Remove previous demo user "Alex Johnson" if present
            val demoUser = repository.getUserByEmail("student@university.edu")
            if (demoUser != null && demoUser.name == "Alex Johnson") {
                repository.deleteUser(demoUser.id)
            }

            // Check if user already registered on this device
            val allUsers = repository.getAllUsersList()
            if (allUsers.isNotEmpty()) {
                _currentUser.value = allUsers.first()
            } else {
                _currentUser.value = null
            }
        }
    }

    // ---------------- AUTH ACTIONS ----------------
    fun login(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = repository.getUserByEmail(email.trim())
            if (user != null) {
                if (user.passwordHash == pass.trim() || user.passwordHash.isEmpty()) {
                    _currentUser.value = user
                    onResult(true, "Welcome back, ${user.name}!")
                } else {
                    onResult(false, "Incorrect password. Please try again.")
                }
            } else {
                onResult(false, "User not found. Please register your profile.")
            }
        }
    }

    fun register(
        name: String,
        email: String,
        pass: String,
        uni: String,
        studentId: String = "",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isEmpty()) {
                onResult(false, "Please enter your email.")
                return@launch
            }
            val existing = repository.getUserByEmail(trimmedEmail)
            if (existing != null) {
                onResult(false, "Email is already registered. Please sign in.")
                return@launch
            }
            val id = repository.insertUser(
                UserEntity(
                    name = name.trim().ifEmpty { "Student" },
                    email = trimmedEmail,
                    passwordHash = pass.trim().ifEmpty { "123456" },
                    university = uni.trim().ifEmpty { "University" },
                    studentId = studentId.trim().ifEmpty { "STU-${Random().nextInt(9000) + 1000}" }
                )
            )
            repository.seedInitialDataForUser(id) // Sets up clean empty semester container
            _currentUser.value = repository.getUserById(id)
            onResult(true, "Profile created! You can now configure your subjects.")
        }
    }

    fun clearAllCurrentUserData(onComplete: () -> Unit = {}) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.clearAllUserData(user.id)
            repository.seedInitialDataForUser(user.id)
            onComplete()
        }
    }

    fun updateProfile(name: String, email: String, university: String, studentId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = user.copy(
                name = name.trim().ifEmpty { user.name },
                email = email.trim().ifEmpty { user.email },
                university = university.trim().ifEmpty { user.university },
                studentId = studentId.trim().ifEmpty { user.studentId }
            )
            repository.updateUser(updated)
            _currentUser.value = updated
        }
    }

    fun switchUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
    }

    fun selectSemester(semesterId: Long) {
        _selectedSemesterId.value = semesterId
    }

    // ---------------- TASK ACTIONS ----------------
    fun addTask(
        title: String,
        subjectId: Long?,
        topicId: Long?,
        date: String,
        startTime: String,
        endTime: String,
        durationMinutes: Int,
        notes: String,
        taskType: String = "STUDY"
    ) {
        val user = _currentUser.value ?: return
        val currentSemId = _selectedSemesterId.value ?: semesters.value.firstOrNull { !it.isArchived }?.id ?: 0L
        viewModelScope.launch {
            repository.insertTask(
                CalendarTaskEntity(
                    userId = user.id,
                    semesterId = currentSemId,
                    title = title,
                    subjectId = subjectId,
                    topicId = topicId,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    notes = notes,
                    taskType = taskType
                )
            )
        }
    }

    fun updateTask(task: CalendarTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTask(task: CalendarTaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun rescheduleTask(task: CalendarTaskEntity, newDate: String, newStartTime: String = task.startTime) {
        viewModelScope.launch {
            repository.updateTask(task.copy(date = newDate, startTime = newStartTime))
        }
    }

    // ---------------- REVISION ACTIONS ----------------
    fun completeRevision(revision: RevisionItemEntity) {
        viewModelScope.launch {
            repository.markRevisionCompleted(revision)
        }
    }

    fun rescheduleRevision(revision: RevisionItemEntity, newDate: String) {
        viewModelScope.launch {
            repository.rescheduleRevision(revision, newDate)
        }
    }

    fun skipRevision(revision: RevisionItemEntity) {
        viewModelScope.launch {
            repository.updateRevision(revision.copy(status = "SKIPPED"))
        }
    }

    fun acceptRevision(revision: RevisionItemEntity) {
        viewModelScope.launch {
            repository.updateRevision(revision.copy(status = "ACCEPTED"))
        }
    }

    // ---------------- TOPIC & SYLLABUS ACTIONS ----------------
    fun addTopic(
        subjectId: Long,
        chapterId: Long,
        name: String,
        description: String,
        importance: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertTopic(
                TopicEntity(
                    chapterId = chapterId,
                    subjectId = subjectId,
                    userId = user.id,
                    name = name,
                    description = description,
                    importance = importance
                )
            )
        }
    }

    fun completeTopic(topic: TopicEntity, understanding: String) {
        viewModelScope.launch {
            repository.completeTopicWithRevisions(topic, understanding)
        }
    }

    fun updateTopic(topic: TopicEntity) {
        viewModelScope.launch {
            repository.updateTopic(topic)
        }
    }

    fun deleteTopic(topicId: Long) {
        viewModelScope.launch {
            repository.deleteTopic(topicId)
        }
    }

    // ---------------- SUBJECT & SEMESTER ACTIONS ----------------
    fun addSubject(
        name: String,
        code: String,
        credits: Double,
        teacher: String,
        colorHex: String,
        iconName: String,
        notes: String
    ) {
        val user = _currentUser.value ?: return
        val semId = _selectedSemesterId.value ?: semesters.value.firstOrNull { !it.isArchived }?.id ?: 1L
        viewModelScope.launch {
            val subId = repository.insertSubject(
                SubjectEntity(
                    semesterId = semId,
                    userId = user.id,
                    name = name,
                    courseCode = code,
                    credits = credits,
                    teacherName = teacher,
                    colorHex = colorHex,
                    iconName = iconName,
                    notes = notes
                )
            )
            // Add default Chapter 1
            repository.insertChapter(
                ChapterEntity(
                    subjectId = subId,
                    userId = user.id,
                    title = "Module 1: Foundations",
                    orderIndex = 1
                )
            )
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            repository.deleteSubject(id)
        }
    }

    fun addSemester(name: String, year: String, start: String, end: String, notes: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertSemester(
                SemesterEntity(
                    userId = user.id,
                    name = name,
                    academicYear = year,
                    startDate = start,
                    endDate = end,
                    notes = notes
                )
            )
        }
    }

    fun updateSemester(semester: SemesterEntity) {
        viewModelScope.launch {
            repository.updateSemester(semester)
        }
    }

    fun toggleSemesterArchive(semester: SemesterEntity) {
        viewModelScope.launch {
            repository.updateSemester(semester.copy(isArchived = !semester.isArchived))
        }
    }

    fun deleteSemester(id: Long) {
        viewModelScope.launch {
            repository.deleteSemester(id)
        }
    }

    // ---------------- PYQ ACTIONS ----------------
    fun addPYQ(
        subjectId: Long,
        topicId: Long?,
        examType: String,
        year: Int,
        question: String,
        marks: Int,
        difficulty: String,
        status: String,
        isImportant: Boolean,
        solution: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertPYQ(
                PYQQuestionEntity(
                    userId = user.id,
                    subjectId = subjectId,
                    topicId = topicId,
                    examType = examType,
                    year = year,
                    questionText = question,
                    marks = marks,
                    difficulty = difficulty,
                    status = status,
                    isImportant = isImportant,
                    solutionNotes = solution
                )
            )
        }
    }

    fun updatePYQStatus(pyq: PYQQuestionEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updatePYQ(pyq.copy(status = newStatus))
        }
    }

    fun deletePYQ(id: Long) {
        viewModelScope.launch {
            repository.deletePYQ(id)
        }
    }

    // ---------------- EXAM CYCLE ACTIONS ----------------
    fun addExamCycle(name: String, prepStart: String, prepEnd: String, examDate: String, notes: String) {
        val user = _currentUser.value ?: return
        val semId = _selectedSemesterId.value ?: semesters.value.firstOrNull { !it.isArchived }?.id ?: 1L
        viewModelScope.launch {
            repository.insertExam(
                ExamCycleEntity(
                    semesterId = semId,
                    userId = user.id,
                    name = name,
                    prepStartDate = prepStart,
                    prepEndDate = prepEnd,
                    examDate = examDate,
                    notes = notes
                )
            )
        }
    }

    fun deleteExam(id: Long) {
        viewModelScope.launch {
            repository.deleteExam(id)
        }
    }

    // ---------------- FOCUS SESSION ----------------
    fun recordFocusSession(
        subjectId: Long?,
        topicId: Long?,
        title: String,
        minutes: Int,
        understanding: String?
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.recordFocusSession(user.id, subjectId, topicId, title, minutes, understanding)
        }
    }

    // ---------------- RESOURCES & NOTES ----------------
    fun addResource(subjectId: Long?, topicId: Long?, title: String, type: String, content: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertResource(
                StudyResourceEntity(
                    userId = user.id,
                    subjectId = subjectId,
                    topicId = topicId,
                    title = title,
                    resourceType = type,
                    urlOrContent = content
                )
            )
        }
    }

    fun deleteResource(id: Long) {
        viewModelScope.launch {
            repository.deleteResource(id)
        }
    }

    fun addNote(subjectId: Long?, topicId: Long?, title: String, content: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertNote(
                StudyNoteEntity(
                    userId = user.id,
                    subjectId = subjectId,
                    topicId = topicId,
                    title = title,
                    content = content
                )
            )
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    // ---------------- INTELLIGENT ANALYSES & RECOMMENDATIONS ----------------

    // 1. PYQ Analysis per topic
    fun analyzePYQForTopic(topic: TopicEntity): TopicPYQAnalysis {
        val allPyqs = pyqs.value.filter { it.topicId == topic.id }
        val count = allPyqs.size
        val years = allPyqs.map { it.year }.distinct().sortedDescending()
        val lastYear = if (years.isNotEmpty()) years.first().toString() else "None"
        val solved = allPyqs.count { it.status == "SOLVED" }
        val unsolved = count - solved

        // Priority calculation based on frequency and weakness
        var priority = 0
        val reasons = mutableListOf<String>()

        if (topic.understanding == "WEAK") {
            priority += 40
            reasons.add("Weak understanding")
        }
        if (count >= 3) {
            priority += 35
            reasons.add("High PYQ frequency ($count times)")
        } else if (count in 1..2) {
            priority += 15
            reasons.add("PYQ appeared ($count times)")
        }
        if (topic.importance == "VERY_IMPORTANT") {
            priority += 20
            reasons.add("Core syllabus topic")
        }

        return TopicPYQAnalysis(
            topicId = topic.id,
            topicName = topic.name,
            appearanceCount = count,
            yearsAppeared = years,
            lastAppearance = lastYear,
            solvedCount = solved,
            unsolvedCount = unsolved,
            importance = topic.importance,
            understanding = topic.understanding,
            priorityScore = priority,
            priorityReason = if (reasons.isNotEmpty()) reasons.joinToString(" + ") else "Regular practice"
        )
    }

    // 2. "What should I study?" Recommendation Engine
    val studyRecommendations: StateFlow<List<TopicPYQAnalysis>> = combine(topics, pyqs, revisions) { topList, pyqList, revList ->
        val pendingRevs = revList.filter { it.status == "PENDING" || it.status == "ACCEPTED" }
        val pendingTopicIds = pendingRevs.map { it.topicId }.toSet()

        topList.map { topic ->
            val pyqsForTopic = pyqList.filter { it.topicId == topic.id }
            val count = pyqsForTopic.size
            val years = pyqsForTopic.map { it.year }.distinct().sortedDescending()
            val lastYear = if (years.isNotEmpty()) years.first().toString() else "None"
            val solved = pyqsForTopic.count { it.status == "SOLVED" }

            var score = 0
            val reasons = mutableListOf<String>()

            if (pendingTopicIds.contains(topic.id)) {
                score += 50
                reasons.add("Revision Due")
            }
            if (topic.understanding == "WEAK") {
                score += 35
                reasons.add("Weak topic")
            }
            if (count >= 2) {
                score += 30
                reasons.add("High PYQ frequency ($count appearances)")
            }
            if (topic.status == "IN_PROGRESS") {
                score += 20
                reasons.add("In progress")
            } else if (topic.status == "NOT_STARTED") {
                score += 15
                reasons.add("Unstarted")
            }
            if (topic.importance == "VERY_IMPORTANT") {
                score += 15
                reasons.add("High importance")
            }

            TopicPYQAnalysis(
                topicId = topic.id,
                topicName = topic.name,
                appearanceCount = count,
                yearsAppeared = years,
                lastAppearance = lastYear,
                solvedCount = solved,
                unsolvedCount = count - solved,
                importance = topic.importance,
                understanding = topic.understanding,
                priorityScore = score,
                priorityReason = if (reasons.isNotEmpty()) reasons.joinToString(" • ") else "General study"
            )
        }.sortedByDescending { it.priorityScore }.take(6)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Exam Readiness Calculations
    val examReadinessList: StateFlow<List<ExamReadinessScore>> = combine(exams, topics, pyqs, revisions) { examList, topList, pyqList, revList ->
        if (examList.isEmpty()) {
            // General semester readiness if no specific exams
            val totalTopics = max(1, topList.size)
            val compTopics = topList.count { it.status == "COMPLETED" }
            val sylScore = (compTopics * 100) / totalTopics

            val strongCount = topList.count { it.understanding == "STRONG" }
            val okayCount = topList.count { it.understanding == "OKAY" }
            val underScore = ((strongCount * 100 + okayCount * 75) / totalTopics).coerceIn(0, 100)

            val totalRev = max(1, revList.size)
            val compRev = revList.count { it.status == "COMPLETED" }
            val revScore = (compRev * 100) / totalRev

            val totalPyq = max(1, pyqList.size)
            val solvedPyq = pyqList.count { it.status == "SOLVED" }
            val pyqScore = (solvedPyq * 100) / totalPyq

            val overall = ((sylScore * 0.35) + (underScore * 0.25) + (revScore * 0.20) + (pyqScore * 0.20)).toInt()

            val weakness = when {
                pyqScore <= sylScore && pyqScore <= revScore && pyqScore <= underScore -> "Main weakness: PYQ practice ($pyqScore%)"
                revScore <= sylScore && revScore <= underScore -> "Main weakness: Pending revisions ($revScore%)"
                underScore <= sylScore -> "Main weakness: Weak topic understanding ($underScore%)"
                else -> "Main weakness: Syllabus coverage ($sylScore%)"
            }

            listOf(
                ExamReadinessScore(
                    examName = "Overall Semester Readiness",
                    overallScore = overall,
                    syllabusScore = sylScore,
                    understandingScore = underScore,
                    revisionScore = revScore,
                    pyqScore = pyqScore,
                    daysLeft = null,
                    mainWeakness = weakness
                )
            )
        } else {
            examList.map { exam ->
                val totalTopics = max(1, topList.size)
                val compTopics = topList.count { it.status == "COMPLETED" }
                val sylScore = (compTopics * 100) / totalTopics

                val strongCount = topList.count { it.understanding == "STRONG" }
                val okayCount = topList.count { it.understanding == "OKAY" }
                val underScore = ((strongCount * 100 + okayCount * 75) / totalTopics).coerceIn(0, 100)

                val totalRev = max(1, revList.size)
                val compRev = revList.count { it.status == "COMPLETED" }
                val revScore = (compRev * 100) / totalRev

                val examPyqs = pyqs.value.filter { it.examType.contains(exam.name.take(4), ignoreCase = true) }
                val totalPyq = max(1, if (examPyqs.isNotEmpty()) examPyqs.size else pyqList.size)
                val solvedPyq = (if (examPyqs.isNotEmpty()) examPyqs else pyqList).count { it.status == "SOLVED" }
                val pyqScore = (solvedPyq * 100) / totalPyq

                val overall = ((sylScore * 0.35) + (underScore * 0.25) + (revScore * 0.20) + (pyqScore * 0.20)).toInt()

                val days = try {
                    val targetDate = dateFormat.parse(exam.examDate)
                    val diff = (targetDate?.time ?: 0L) - System.currentTimeMillis()
                    (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                } catch (_: Exception) {
                    null
                }

                val weakness = when {
                    pyqScore <= sylScore && pyqScore <= revScore && pyqScore <= underScore -> "Main weakness: PYQ practice ($pyqScore%)"
                    revScore <= sylScore && revScore <= underScore -> "Main weakness: Revision completion ($revScore%)"
                    underScore <= sylScore -> "Main weakness: Topic understanding ($underScore%)"
                    else -> "Main weakness: Remaining syllabus ($sylScore% covered)"
                }

                ExamReadinessScore(
                    examName = exam.name,
                    overallScore = overall,
                    syllabusScore = sylScore,
                    understandingScore = underScore,
                    revisionScore = revScore,
                    pyqScore = pyqScore,
                    daysLeft = days,
                    mainWeakness = weakness
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Study Activity Heatmap Aggregation
    val heatmapDays: StateFlow<List<DayActivitySummary>> = combine(studyActivities, heatmapFilter) { activities, filter ->
        val totalDays = when (filter) {
            "3_MONTHS" -> 91
            "6_MONTHS" -> 182
            "1_YEAR" -> 365
            else -> 91
        }

        val grouped = activities.groupBy { it.date }
        val list = mutableListOf<DayActivitySummary>()
        val cal = Calendar.getInstance()

        for (i in totalDays - 1 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = dateFormat.format(cal.time)

            val dayActs = grouped[dStr] ?: emptyList()
            val totalMin = dayActs.sumOf { it.durationMinutes }
            val tasks = dayActs.count { it.activityType == "STUDY_TASK_COMPLETED" }
            val revs = dayActs.count { it.activityType == "REVISION_COMPLETED" }
            val pyqsCount = dayActs.count { it.activityType == "PYQ_PRACTICED" }
            val focusCount = dayActs.count { it.activityType == "FOCUS_SESSION_COMPLETED" }
            val count = dayActs.size

            val intensity = when {
                count == 0 && totalMin == 0 -> 0
                count == 1 || totalMin < 30 -> 1
                count in 2..3 || totalMin in 30..75 -> 2
                count in 4..5 || totalMin in 76..140 -> 3
                else -> 4
            }

            list.add(
                DayActivitySummary(
                    dateStr = dStr,
                    count = count,
                    totalMinutes = totalMin,
                    tasksCompleted = tasks,
                    revisionsCompleted = revs,
                    pyqsPracticed = pyqsCount,
                    focusSessions = focusCount,
                    intensityLevel = intensity
                )
            )
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Streaks
    val streakStats: StateFlow<Pair<Int, Int>> = studyActivities.map { activities ->
        val activeDates = activities.map { it.date }.toSet()
        val cal = Calendar.getInstance()
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        // Check today & yesterday for current streak
        val today = dateFormat.format(Date())
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(cal.time)

        var checkCal = Calendar.getInstance()
        if (activeDates.contains(today)) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (activeDates.contains(dateFormat.format(checkCal.time))) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (activeDates.contains(yesterday)) {
            checkCal.time = Date()
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (activeDates.contains(dateFormat.format(checkCal.time))) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        // Calculate longest streak over the past 365 days
        val loopCal = Calendar.getInstance()
        for (i in 0..365) {
            loopCal.time = Date()
            loopCal.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = dateFormat.format(loopCal.time)
            if (activeDates.contains(dStr)) {
                tempStreak++
                if (tempStreak > longestStreak) longestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        Pair(currentStreak, max(currentStreak, longestStreak))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0, 0))
}
