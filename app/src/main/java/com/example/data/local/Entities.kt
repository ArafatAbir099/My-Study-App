package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val university: String = "University",
    val studentId: String = "",
    val avatarColor: Long = 0xFF1E40AF,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val academicYear: String,
    val startDate: String,
    val endDate: String,
    val isArchived: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val userId: Long,
    val name: String,
    val courseCode: String,
    val credits: Double = 3.0,
    val teacherName: String = "",
    val colorHex: String = "#3B82F6",
    val iconName: String = "menu_book",
    val notes: String = ""
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val userId: Long,
    val title: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val subjectId: Long,
    val userId: Long,
    val name: String,
    val description: String = "",
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED, NEED_REVISION
    val understanding: String = "OKAY", // STRONG, OKAY, WEAK
    val importance: String = "NORMAL", // NORMAL, IMPORTANT, VERY_IMPORTANT
    val notes: String = "",
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_cycles")
data class ExamCycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val userId: Long,
    val name: String, // "Midterm", "Final", etc.
    val prepStartDate: String,
    val prepEndDate: String,
    val examDate: String,
    val targetTopicIds: String = "", // Comma-separated or "ALL"
    val notes: String = ""
)

@Entity(tableName = "calendar_tasks")
data class CalendarTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val semesterId: Long,
    val title: String,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val pyqId: Long? = null,
    val date: String, // "yyyy-MM-dd"
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val durationMinutes: Int = 60,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String = "",
    val taskType: String = "STUDY" // STUDY, REVISION, PYQ_PRACTICE, EXAM_PREP
)

@Entity(tableName = "revision_items")
data class RevisionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val topicId: Long,
    val subjectId: Long,
    val semesterId: Long,
    val scheduledDate: String, // "yyyy-MM-dd"
    val revisionNumber: Int = 1,
    val status: String = "PENDING", // PENDING, ACCEPTED, COMPLETED, SKIPPED, MISSED
    val completedAt: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "pyq_questions")
data class PYQQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val subjectId: Long,
    val topicId: Long? = null,
    val examType: String = "Midterm", // Midterm, Final, Term Test
    val year: Int = 2026,
    val questionText: String,
    val marks: Int = 5,
    val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val status: String = "UNSOLVED", // UNSOLVED, ATTEMPTED, SOLVED, NEED_PRACTICE
    val isImportant: Boolean = false,
    val isRepeated: Boolean = false,
    val solutionNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_resources")
data class StudyResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val semesterId: Long? = null,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val title: String,
    val resourceType: String = "NOTE", // PDF, SLIDES, DOC, LINK, NOTE
    val urlOrContent: String,
    val isBookmarked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val semesterId: Long? = null,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val pyqId: Long? = null,
    val title: String,
    val content: String,
    val isBookmarked: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val taskTitle: String,
    val durationMinutes: Int,
    val understandingFeedback: String? = null, // STRONG, OKAY, WEAK
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_activities")
data class StudyActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val date: String, // "yyyy-MM-dd"
    val activityType: String, // STUDY_TASK_COMPLETED, REVISION_COMPLETED, FOCUS_SESSION_COMPLETED, PYQ_PRACTICED, TOPIC_COMPLETED
    val durationMinutes: Int = 0,
    val subjectId: Long? = null,
    val topicId: Long? = null,
    val metadata: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
