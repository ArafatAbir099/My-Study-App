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
                academicYear = "2026-2027",
                startDate = todayStr(),
                endDate = "",
                isArchived = false,
                notes = "Personal student workspace. Add your subjects and syllabus."
            )
        )
    }
}
