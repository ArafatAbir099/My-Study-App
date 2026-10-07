package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class StudyPlannerRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val semesterDao = db.semesterDao()
    private val subjectDao = db.subjectDao()
    private val chapterDao = db.chapterDao()
    private val topicDao = db.topicDao()
    private val examDao = db.examCycleDao()
    private val calendarDao = db.calendarTaskDao()
    private val revisionDao = db.revisionDao()
    private val pyqDao = db.pyqDao()
    private val resourceDao = db.resourceDao()
    private val noteDao = db.noteDao()
    private val focusDao = db.focusSessionDao()
    private val activityDao = db.studyActivityDao()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun todayStr(): String = dateFormat.format(Date())

    // ---------------- AUTH & USERS ----------------
    suspend fun getUserById(id: Long) = userDao.getUserById(id)
    suspend fun getUserByEmail(email: String) = userDao.getUserByEmail(email)
    suspend fun getUserByUsernameOrEmail(identifier: String) = userDao.getUserByUsernameOrEmail(identifier)
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)

    // ---------------- SEMESTERS ----------------
    fun getSemesters(userId: Long): Flow<List<SemesterEntity>> = semesterDao.getSemestersByUser(userId)
    fun getSemesterById(id: Long): Flow<SemesterEntity?> = semesterDao.getSemesterById(id)
    suspend fun insertSemester(semester: SemesterEntity): Long = semesterDao.insertSemester(semester)
    suspend fun updateSemester(semester: SemesterEntity) = semesterDao.updateSemester(semester)
    suspend fun deleteSemester(id: Long) = semesterDao.deleteSemesterById(id)

    // ---------------- SUBJECTS ----------------
    fun getSubjects(semesterId: Long): Flow<List<SubjectEntity>> = subjectDao.getSubjectsBySemester(semesterId)
    fun getSubjectsByUser(userId: Long): Flow<List<SubjectEntity>> = subjectDao.getSubjectsByUser(userId)
    fun getSubjectById(id: Long): Flow<SubjectEntity?> = subjectDao.getSubjectById(id)
    suspend fun getSubjectByIdDirect(id: Long) = subjectDao.getSubjectByIdDirect(id)
    suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(id: Long) = subjectDao.deleteSubjectById(id)

    // ---------------- CHAPTERS & TOPICS ----------------
    fun getChapters(subjectId: Long): Flow<List<ChapterEntity>> = chapterDao.getChaptersBySubject(subjectId)
    suspend fun insertChapter(chapter: ChapterEntity) = chapterDao.insertChapter(chapter)
    suspend fun deleteChapter(id: Long) = chapterDao.deleteChapterById(id)

    fun getTopicsBySubject(subjectId: Long): Flow<List<TopicEntity>> = topicDao.getTopicsBySubject(subjectId)
    fun getTopicsByUser(userId: Long): Flow<List<TopicEntity>> = topicDao.getTopicsByUser(userId)
    fun getTopicById(id: Long): Flow<TopicEntity?> = topicDao.getTopicById(id)
    suspend fun getTopicByIdDirect(id: Long) = topicDao.getTopicByIdDirect(id)
    suspend fun insertTopic(topic: TopicEntity): Long = topicDao.insertTopic(topic)
    suspend fun updateTopic(topic: TopicEntity) = topicDao.updateTopic(topic)
    suspend fun deleteTopic(id: Long) = topicDao.deleteTopicById(id)

    // ---------------- EXAM CYCLES ----------------
    fun getExams(semesterId: Long): Flow<List<ExamCycleEntity>> = examDao.getExamsBySemester(semesterId)
    fun getExamsByUser(userId: Long): Flow<List<ExamCycleEntity>> = examDao.getExamsByUser(userId)
    fun getExamById(id: Long): Flow<ExamCycleEntity?> = examDao.getExamById(id)
    suspend fun getExamByIdDirect(id: Long) = examDao.getExamByIdDirect(id)
    fun getExamsBySubject(subjectId: Long): Flow<List<ExamCycleEntity>> = examDao.getExamsBySubject(subjectId)
    suspend fun insertExam(exam: ExamCycleEntity): Long = examDao.insertExam(exam)
    suspend fun updateExam(exam: ExamCycleEntity) = examDao.updateExam(exam)
    suspend fun deleteExam(id: Long) = examDao.deleteExamById(id)

    // ---------------- CALENDAR TASKS ----------------
    fun getTasksByUser(userId: Long): Flow<List<CalendarTaskEntity>> = calendarDao.getTasksByUser(userId)
    fun getTasksByDate(userId: Long, date: String): Flow<List<CalendarTaskEntity>> = calendarDao.getTasksByDate(userId, date)
    suspend fun insertTask(task: CalendarTaskEntity): Long = calendarDao.insertTask(task)
    suspend fun updateTask(task: CalendarTaskEntity) = calendarDao.updateTask(task)
    suspend fun deleteTask(id: Long) = calendarDao.deleteTaskById(id)
    suspend fun deleteUncompletedTasksByExamId(examId: Long) = calendarDao.deleteUncompletedTasksByExamId(examId)

    suspend fun toggleTaskCompletion(task: CalendarTaskEntity) {
        val completed = !task.isCompleted
        val now = if (completed) System.currentTimeMillis() else null
        calendarDao.updateTask(task.copy(isCompleted = completed, completedAt = now))

        if (completed) {
            logActivity(
                userId = task.userId,
                type = "STUDY_TASK_COMPLETED",
                duration = task.durationMinutes,
                subjectId = task.subjectId,
                topicId = task.topicId,
                metadata = task.title
            )
        }
    }

    // ---------------- REVISIONS (AUTOMATIC ENGINE) ----------------
    fun getRevisionsByUser(userId: Long): Flow<List<RevisionItemEntity>> = revisionDao.getRevisionsByUser(userId)
    fun getRevisionsByTopic(topicId: Long): Flow<List<RevisionItemEntity>> = revisionDao.getRevisionsByTopic(topicId)
    suspend fun insertRevision(revision: RevisionItemEntity): Long = revisionDao.insertRevision(revision)
    suspend fun updateRevision(revision: RevisionItemEntity) = revisionDao.updateRevision(revision)
    suspend fun deleteRevision(id: Long) = revisionDao.deleteRevisionById(id)

    suspend fun markRevisionCompleted(revision: RevisionItemEntity) {
        revisionDao.updateRevision(
            revision.copy(status = "COMPLETED", completedAt = System.currentTimeMillis())
        )
        logActivity(
            userId = revision.userId,
            type = "REVISION_COMPLETED",
            duration = 30,
            subjectId = revision.subjectId,
            topicId = revision.topicId,
            metadata = "Revision #${revision.revisionNumber}"
        )
    }

    suspend fun rescheduleRevision(revision: RevisionItemEntity, newDate: String) {
        revisionDao.updateRevision(
            revision.copy(scheduledDate = newDate, status = "ACCEPTED")
        )
    }

    // ---------------- TOPIC COMPLETION & AUTOMATIC REVISION ENGINE ----------------
    suspend fun completeTopicWithRevisions(
        topic: TopicEntity,
        understanding: String
    ) {
        val now = System.currentTimeMillis()
        topicDao.updateTopic(
            topic.copy(
                status = "COMPLETED",
                understanding = understanding,
                completedAt = now
            )
        )

        // Log Study Activity
        logActivity(
            userId = topic.userId,
            type = "TOPIC_COMPLETED",
            duration = 45,
            subjectId = topic.subjectId,
            topicId = topic.id,
            metadata = topic.name
        )

        // Generate Spaced Repetition Revisions
        val intervals = when (understanding) {
            "STRONG" -> listOf(3, 7, 15)
            "WEAK" -> listOf(1, 2, 5, 10)
            else -> listOf(1, 4, 10) // OKAY
        }

        val cal = Calendar.getInstance()
        intervals.forEachIndexed { index, daysAhead ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, daysAhead)
            val revDate = dateFormat.format(cal.time)

            val revisionItem = RevisionItemEntity(
                userId = topic.userId,
                topicId = topic.id,
                subjectId = topic.subjectId,
                semesterId = 0L, // resolved if needed
                scheduledDate = revDate,
                revisionNumber = index + 1,
                status = "PENDING",
                notes = "Auto-suggested revision for ${topic.name} ($understanding understanding)"
            )
            revisionDao.insertRevision(revisionItem)
        }
    }

    // ---------------- PYQ QUESTIONS ----------------
    fun getPYQsBySubject(subjectId: Long): Flow<List<PYQQuestionEntity>> = pyqDao.getPYQsBySubject(subjectId)
    fun getPYQsByUser(userId: Long): Flow<List<PYQQuestionEntity>> = pyqDao.getPYQsByUser(userId)
    fun getPYQsByTopic(topicId: Long): Flow<List<PYQQuestionEntity>> = pyqDao.getPYQsByTopic(topicId)
    suspend fun insertPYQ(pyq: PYQQuestionEntity): Long = pyqDao.insertPYQ(pyq)
    suspend fun updatePYQ(pyq: PYQQuestionEntity) {
        val wasSolved = pyq.status == "SOLVED"
        pyqDao.updatePYQ(pyq)
        if (wasSolved) {
            logActivity(
                userId = pyq.userId,
                type = "PYQ_PRACTICED",
                duration = 20,
                subjectId = pyq.subjectId,
                topicId = pyq.topicId,
                metadata = "PYQ: ${pyq.questionText.take(30)}..."
            )
        }
    }
    suspend fun deletePYQ(id: Long) = pyqDao.deletePYQById(id)

    // ---------------- RESOURCES & NOTES ----------------
    fun getResourcesByUser(userId: Long): Flow<List<StudyResourceEntity>> = resourceDao.getResourcesByUser(userId)
    fun getResourcesBySubject(subjectId: Long): Flow<List<StudyResourceEntity>> = resourceDao.getResourcesBySubject(subjectId)
    fun getResourcesByTopic(topicId: Long): Flow<List<StudyResourceEntity>> = resourceDao.getResourcesByTopic(topicId)
    suspend fun insertResource(resource: StudyResourceEntity) = resourceDao.insertResource(resource)
    suspend fun updateResource(resource: StudyResourceEntity) = resourceDao.updateResource(resource)
    suspend fun deleteResource(id: Long) = resourceDao.deleteResourceById(id)

    fun getNotesByUser(userId: Long): Flow<List<StudyNoteEntity>> = noteDao.getNotesByUser(userId)
    fun getNotesBySubject(subjectId: Long): Flow<List<StudyNoteEntity>> = noteDao.getNotesBySubject(subjectId)
    fun getNotesByTopic(topicId: Long): Flow<List<StudyNoteEntity>> = noteDao.getNotesByTopic(topicId)
    suspend fun insertNote(note: StudyNoteEntity) = noteDao.insertNote(note)
    suspend fun updateNote(note: StudyNoteEntity) = noteDao.updateNote(note)
    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    // ---------------- FOCUS SESSION ----------------
    fun getFocusSessions(userId: Long): Flow<List<FocusSessionEntity>> = focusDao.getSessionsByUser(userId)
    suspend fun recordFocusSession(
        userId: Long,
        subjectId: Long?,
        topicId: Long?,
        title: String,
        minutes: Int,
        understanding: String?
    ) {
        focusDao.insertSession(
            FocusSessionEntity(
                userId = userId,
                subjectId = subjectId,
                topicId = topicId,
                taskTitle = title,
                durationMinutes = minutes,
                understandingFeedback = understanding
            )
        )
        logActivity(
            userId = userId,
            type = "FOCUS_SESSION_COMPLETED",
            duration = minutes,
            subjectId = subjectId,
            topicId = topicId,
            metadata = "$title ($minutes min)"
        )

        // If topic feedback given, update topic understanding
        if (topicId != null && topicId > 0 && understanding != null) {
            val topic = topicDao.getTopicByIdDirect(topicId)
            if (topic != null) {
                topicDao.updateTopic(topic.copy(understanding = understanding))
            }
        }
    }

    // ---------------- STUDY ACTIVITY & HEATMAP ----------------
    fun getStudyActivities(userId: Long): Flow<List<StudyActivityEntity>> = activityDao.getActivitiesByUser(userId)

    suspend fun logActivity(
        userId: Long,
        type: String,
        duration: Int,
        subjectId: Long? = null,
        topicId: Long? = null,
        metadata: String = ""
    ) {
        val today = todayStr()
        activityDao.insertActivity(
            StudyActivityEntity(
                userId = userId,
                date = today,
                activityType = type,
                durationMinutes = duration,
                subjectId = subjectId,
                topicId = topicId,
                metadata = metadata
            )
        )
    }

    suspend fun clearAllUserData(userId: Long) {
        semesterDao.deleteSemestersByUser(userId)
        subjectDao.deleteSubjectsByUser(userId)
        chapterDao.deleteChaptersByUser(userId)
        topicDao.deleteTopicsByUser(userId)
        examDao.deleteExamsByUser(userId)
        calendarDao.deleteTasksByUser(userId)
        revisionDao.deleteRevisionsByUser(userId)
        pyqDao.deletePYQsByUser(userId)
        resourceDao.deleteResourcesByUser(userId)
        noteDao.deleteNotesByUser(userId)
        focusDao.deleteFocusSessionsByUser(userId)
        activityDao.deleteActivitiesByUser(userId)
    }

    suspend fun deleteUser(userId: Long) {
        clearAllUserData(userId)
        userDao.deleteUserById(userId)
    }

    suspend fun getAllUsersList(): List<UserEntity> = userDao.getAllUsersList()

    // Clean user workspace initialization - NO mock data seeded!
    suspend fun seedInitialDataForUser(userId: Long) {
        semesterDao.insertSemester(
            SemesterEntity(
                userId = userId,
                name = "Semester 1",
                academicYear = "2025-2026",
                startDate = todayStr(),
                endDate = "",
                isArchived = false,
                notes = "Personal student workspace. Add your subjects and syllabus."
            )
        )
        return
    }

    // Deprecated demo seeder kept private for safety
    private suspend fun oldSeedDemoData(userId: Long) {
        val semId = semesterDao.insertSemester(
            SemesterEntity(
                userId = userId,
                name = "Spring 2026",
                academicYear = "2025-2026",
                startDate = "2026-01-10",
                endDate = "2026-05-30",
                isArchived = false,
                notes = "Core computer science & engineering semester."
            )
        )

        // Subject 1: Data Structures & Algorithms
        val sub1Id = subjectDao.insertSubject(
            SubjectEntity(
                semesterId = semId,
                userId = userId,
                name = "Data Structures & Algorithms",
                courseCode = "CSE 201",
                credits = 3.0,
                teacherName = "Dr. Alan Turing",
                colorHex = "#2563EB",
                iconName = "account_tree",
                notes = "Master trees, graphs, sorting, and linked lists."
            )
        )

        val ch1Id = chapterDao.insertChapter(
            ChapterEntity(subjectId = sub1Id, userId = userId, title = "Linear Structures", orderIndex = 1)
        )
        val ch2Id = chapterDao.insertChapter(
            ChapterEntity(subjectId = sub1Id, userId = userId, title = "Non-Linear Structures", orderIndex = 2)
        )

        val t1Id = topicDao.insertTopic(
            TopicEntity(
                chapterId = ch1Id,
                subjectId = sub1Id,
                userId = userId,
                name = "Linked Lists (Singly & Doubly)",
                description = "Node pointer manipulation, insertion, and reversal.",
                status = "COMPLETED",
                understanding = "WEAK",
                importance = "VERY_IMPORTANT",
                notes = "Practice pointer reversal edge cases before exam.",
                completedAt = System.currentTimeMillis() - 86400000L * 2
            )
        )

        val t2Id = topicDao.insertTopic(
            TopicEntity(
                chapterId = ch1Id,
                subjectId = sub1Id,
                userId = userId,
                name = "Stack & Queue Applications",
                description = "Monotonic stack, expression evaluation, DFS recursion.",
                status = "COMPLETED",
                understanding = "STRONG",
                importance = "IMPORTANT",
                completedAt = System.currentTimeMillis() - 86400000L * 4
            )
        )

        val t3Id = topicDao.insertTopic(
            TopicEntity(
                chapterId = ch2Id,
                subjectId = sub1Id,
                userId = userId,
                name = "Binary Search Trees & AVL",
                description = "Self-balancing tree rotations, tree traversals.",
                status = "IN_PROGRESS",
                understanding = "OKAY",
                importance = "VERY_IMPORTANT"
            )
        )

        val t4Id = topicDao.insertTopic(
            TopicEntity(
                chapterId = ch2Id,
                subjectId = sub1Id,
                userId = userId,
                name = "Graph Algorithms (BFS/DFS, Dijkstra)",
                description = "Shortest path, adjacency list, topological sorting.",
                status = "NOT_STARTED",
                understanding = "OKAY",
                importance = "VERY_IMPORTANT"
            )
        )

        // Subject 2: Discrete Mathematics
        val sub2Id = subjectDao.insertSubject(
            SubjectEntity(
                semesterId = semId,
                userId = userId,
                name = "Discrete Mathematics",
                courseCode = "CSE 103",
                credits = 3.0,
                teacherName = "Prof. George Boole",
                colorHex = "#059669",
                iconName = "functions",
                notes = "Propositional logic, set theory, graph theory."
            )
        )

        val ch3Id = chapterDao.insertChapter(
            ChapterEntity(subjectId = sub2Id, userId = userId, title = "Logic & Proofs", orderIndex = 1)
        )

        val t5Id = topicDao.insertTopic(
            TopicEntity(
                chapterId = ch3Id,
                subjectId = sub2Id,
                userId = userId,
                name = "Propositional & Predicate Logic",
                description = "Truth tables, quantifiers, logical equivalence.",
                status = "COMPLETED",
                understanding = "OKAY",
                importance = "IMPORTANT",
                completedAt = System.currentTimeMillis() - 86400000L * 6
            )
        )

        val t6Id = topicDao.insertTopic(
            TopicEntity(
                chapterId = ch3Id,
                subjectId = sub2Id,
                userId = userId,
                name = "Mathematical Induction & Recursion",
                description = "Weak vs Strong Induction, base step and inductive step.",
                status = "IN_PROGRESS",
                understanding = "WEAK",
                importance = "VERY_IMPORTANT"
            )
        )

        // Subject 3: Database Systems
        val sub3Id = subjectDao.insertSubject(
            SubjectEntity(
                semesterId = semId,
                userId = userId,
                name = "Database Management Systems",
                courseCode = "CSE 301",
                credits = 3.0,
                teacherName = "Dr. Edgar Codd",
                colorHex = "#D97706",
                iconName = "storage",
                notes = "Relational algebra, SQL, B+ Trees, ACID transactions."
            )
        )

        val ch4Id = chapterDao.insertChapter(
            ChapterEntity(subjectId = sub3Id, userId = userId, title = "Relational Model", orderIndex = 1)
        )

        val t7Id = topicDao.insertTopic(
            TopicEntity(
                chapterId = ch4Id,
                subjectId = sub3Id,
                userId = userId,
                name = "Normalization (1NF, 2NF, 3NF, BCNF)",
                description = "Functional dependencies, lossless join decomposition.",
                status = "IN_PROGRESS",
                understanding = "WEAK",
                importance = "VERY_IMPORTANT"
            )
        )

        // Exams
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 12)
        val midtermDate = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, 45)
        val finalDate = dateFormat.format(cal.time)

        examDao.insertExam(
            ExamCycleEntity(
                semesterId = semId,
                userId = userId,
                name = "Midterm Examination",
                prepStartDate = "2026-10-01",
                prepEndDate = midtermDate,
                examDate = midtermDate,
                notes = "Covers Linear Data Structures, Logic, and SQL Queries."
            )
        )

        examDao.insertExam(
            ExamCycleEntity(
                semesterId = semId,
                userId = userId,
                name = "Final Examination",
                prepStartDate = "2026-11-15",
                prepEndDate = finalDate,
                examDate = finalDate,
                notes = "Comprehensive semester final."
            )
        )

        // PYQs
        pyqDao.insertPYQ(
            PYQQuestionEntity(
                userId = userId,
                subjectId = sub1Id,
                topicId = t1Id,
                examType = "Midterm",
                year = 2025,
                questionText = "Write an algorithm in C/Java to reverse a singly linked list iteratively and analyze its time and space complexity.",
                marks = 7,
                difficulty = "MEDIUM",
                status = "SOLVED",
                isImportant = true,
                isRepeated = true,
                solutionNotes = "Use 3 pointers: prev, curr, next. O(n) time, O(1) space."
            )
        )

        pyqDao.insertPYQ(
            PYQQuestionEntity(
                userId = userId,
                subjectId = sub1Id,
                topicId = t1Id,
                examType = "Final",
                year = 2024,
                questionText = "Explain how to detect and remove a cycle in a linked list using Floyd's Tortoise and Hare algorithm.",
                marks = 8,
                difficulty = "HARD",
                status = "NEED_PRACTICE",
                isImportant = true,
                isRepeated = true,
                solutionNotes = "Fast and slow pointer meet at cycle. Reset slow to head, advance both by 1 to find entry."
            )
        )

        pyqDao.insertPYQ(
            PYQQuestionEntity(
                userId = userId,
                subjectId = sub1Id,
                topicId = t3Id,
                examType = "Midterm",
                year = 2025,
                questionText = "Insert the keys [21, 26, 30, 9, 4, 14, 28] into an initially empty AVL tree. Show all rotations.",
                marks = 10,
                difficulty = "HARD",
                status = "UNSOLVED",
                isImportant = true,
                isRepeated = false,
                solutionNotes = "Requires RR and LR rotations."
            )
        )

        pyqDao.insertPYQ(
            PYQQuestionEntity(
                userId = userId,
                subjectId = sub2Id,
                topicId = t5Id,
                examType = "Midterm",
                year = 2025,
                questionText = "Prove using mathematical induction that 1 + 2 + ... + n = n(n+1)/2 for all integers n >= 1.",
                marks = 6,
                difficulty = "EASY",
                status = "SOLVED",
                isImportant = true,
                isRepeated = true,
                solutionNotes = "Base case n=1. Inductive hypothesis assume true for k, show for k+1."
            )
        )

        pyqDao.insertPYQ(
            PYQQuestionEntity(
                userId = userId,
                subjectId = sub3Id,
                topicId = t7Id,
                examType = "Midterm",
                year = 2025,
                questionText = "Given schema R(A, B, C, D) with F = {A -> B, B -> C, C -> D}. Identify candidate key and normalize to 3NF and BCNF.",
                marks = 9,
                difficulty = "HARD",
                status = "ATTEMPTED",
                isImportant = true,
                isRepeated = true,
                solutionNotes = "Candidate key is A. Transitive dependencies present."
            )
        )

        // Seed Revision Items
        val today = todayStr()
        revisionDao.insertRevision(
            RevisionItemEntity(
                userId = userId,
                topicId = t1Id,
                subjectId = sub1Id,
                semesterId = semId,
                scheduledDate = today,
                revisionNumber = 1,
                status = "PENDING",
                notes = "Review Linked List pointer manipulation (Weak understanding)."
            )
        )

        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, 2)
        val in2Days = dateFormat.format(cal.time)
        revisionDao.insertRevision(
            RevisionItemEntity(
                userId = userId,
                topicId = t5Id,
                subjectId = sub2Id,
                semesterId = semId,
                scheduledDate = in2Days,
                revisionNumber = 2,
                status = "ACCEPTED",
                notes = "Second cycle revision for Propositional Logic truth tables."
            )
        )

        // Calendar Tasks (Manual calendar)
        calendarDao.insertTask(
            CalendarTaskEntity(
                userId = userId,
                semesterId = semId,
                title = "Data Structures -> Linked List Practice",
                subjectId = sub1Id,
                topicId = t1Id,
                date = today,
                startTime = "18:00",
                endTime = "19:00",
                durationMinutes = 60,
                isCompleted = false,
                notes = "Practice Floyd's cycle detection question.",
                taskType = "STUDY"
            )
        )

        calendarDao.insertTask(
            CalendarTaskEntity(
                userId = userId,
                semesterId = semId,
                title = "Discrete Math -> Induction Proofs",
                subjectId = sub2Id,
                topicId = t6Id,
                date = today,
                startTime = "20:00",
                endTime = "21:00",
                durationMinutes = 60,
                isCompleted = false,
                notes = "Review textbook problem set chapter 3.",
                taskType = "STUDY"
            )
        )

        // Resources
        resourceDao.insertResource(
            StudyResourceEntity(
                userId = userId,
                semesterId = semId,
                subjectId = sub1Id,
                topicId = t1Id,
                title = "MIT 6.006 Lecture Notes: Linked Data Structures",
                resourceType = "PDF",
                urlOrContent = "https://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-006-introduction-to-algorithms/",
                isBookmarked = true
            )
        )

        resourceDao.insertResource(
            StudyResourceEntity(
                userId = userId,
                semesterId = semId,
                subjectId = sub3Id,
                topicId = t7Id,
                title = "Database Normalization Cheat Sheet (1NF to BCNF)",
                resourceType = "DOC",
                urlOrContent = "Rules and examples for functional dependency decomposition.",
                isBookmarked = true
            )
        )

        // Notes
        noteDao.insertNote(
            StudyNoteEntity(
                userId = userId,
                semesterId = semId,
                subjectId = sub1Id,
                topicId = t1Id,
                title = "Linked List Edge Cases To Remember",
                content = "1. Empty list (head == null)\n2. Single node list\n3. Cycle detection with 2-speed pointers\n4. Reversal without dummy head memory leak",
                isBookmarked = true
            )
        )

        // Seed Activity History for Heatmap (past few weeks)
        val activityCal = Calendar.getInstance()
        val random = Random(42)
        for (i in 0..45) {
            activityCal.time = Date()
            activityCal.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = dateFormat.format(activityCal.time)
            // Generate realistic study sessions on some days
            val sessionsToday = when {
                i == 0 -> 2
                i % 6 == 0 -> 0 // rest day
                i % 4 == 0 -> 1
                i % 3 == 0 -> 3
                else -> 2
            }

            for (s in 0 until sessionsToday) {
                val dur = 30 + random.nextInt(4) * 20
                activityDao.insertActivity(
                    StudyActivityEntity(
                        userId = userId,
                        date = dStr,
                        activityType = if (s % 2 == 0) "STUDY_TASK_COMPLETED" else "PYQ_PRACTICED",
                        durationMinutes = dur,
                        subjectId = sub1Id,
                        metadata = "Session #${s + 1}",
                        timestamp = activityCal.timeInMillis
                    )
                )
            }
        }
    }
}
