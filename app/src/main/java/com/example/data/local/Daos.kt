package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getUserByName(name: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(TRIM(email)) = LOWER(TRIM(:identifier)) OR LOWER(TRIM(username)) = LOWER(TRIM(:identifier)) OR LOWER(TRIM(name)) = LOWER(TRIM(:identifier)) LIMIT 1")
    suspend fun getUserByUsernameOrEmail(identifier: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters WHERE userId = :userId ORDER BY createdAt DESC")
    fun getSemestersByUser(userId: Long): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE id = :id LIMIT 1")
    fun getSemesterById(id: Long): Flow<SemesterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteSemesterById(id: Long)

    @Query("DELETE FROM semesters WHERE userId = :userId")
    suspend fun deleteSemestersByUser(userId: Long)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE semesterId = :semesterId ORDER BY name ASC")
    fun getSubjectsBySemester(semesterId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    fun getSubjectsByUser(userId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    fun getSubjectById(id: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectByIdDirect(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)

    @Query("DELETE FROM subjects WHERE userId = :userId")
    suspend fun deleteSubjectsByUser(userId: Long)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY orderIndex ASC")
    fun getChaptersBySubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)

    @Query("DELETE FROM chapters WHERE userId = :userId")
    suspend fun deleteChaptersByUser(userId: Long)
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY id ASC")
    fun getTopicsBySubject(subjectId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE userId = :userId ORDER BY id ASC")
    fun getTopicsByUser(userId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY id ASC")
    fun getTopicsByChapter(chapterId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    fun getTopicById(id: Long): Flow<TopicEntity?>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicByIdDirect(id: Long): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)

    @Query("DELETE FROM topics WHERE userId = :userId")
    suspend fun deleteTopicsByUser(userId: Long)
}

@Dao
interface ExamCycleDao {
    @Query("SELECT * FROM exam_cycles WHERE semesterId = :semesterId ORDER BY examDate ASC")
    fun getExamsBySemester(semesterId: Long): Flow<List<ExamCycleEntity>>

    @Query("SELECT * FROM exam_cycles WHERE userId = :userId ORDER BY examDate ASC")
    fun getExamsByUser(userId: Long): Flow<List<ExamCycleEntity>>

    @Query("SELECT * FROM exam_cycles WHERE id = :id LIMIT 1")
    fun getExamById(id: Long): Flow<ExamCycleEntity?>

    @Query("SELECT * FROM exam_cycles WHERE id = :id LIMIT 1")
    suspend fun getExamByIdDirect(id: Long): ExamCycleEntity?

    @Query("SELECT * FROM exam_cycles WHERE subjectId = :subjectId ORDER BY examDate ASC")
    fun getExamsBySubject(subjectId: Long): Flow<List<ExamCycleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamCycleEntity): Long

    @Update
    suspend fun updateExam(exam: ExamCycleEntity)

    @Query("DELETE FROM exam_cycles WHERE id = :id")
    suspend fun deleteExamById(id: Long)

    @Query("DELETE FROM exam_cycles WHERE userId = :userId")
    suspend fun deleteExamsByUser(userId: Long)
}

@Dao
interface CalendarTaskDao {
    @Query("SELECT * FROM calendar_tasks WHERE userId = :userId ORDER BY date ASC, startTime ASC")
    fun getTasksByUser(userId: Long): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks WHERE userId = :userId AND date = :date ORDER BY startTime ASC")
    fun getTasksByDate(userId: Long, date: String): Flow<List<CalendarTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CalendarTaskEntity): Long

    @Update
    suspend fun updateTask(task: CalendarTaskEntity)

    @Query("DELETE FROM calendar_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM calendar_tasks WHERE examId = :examId AND isCompleted = 0")
    suspend fun deleteUncompletedTasksByExamId(examId: Long)

    @Query("DELETE FROM calendar_tasks WHERE userId = :userId")
    suspend fun deleteTasksByUser(userId: Long)
}

@Dao
interface RevisionDao {
    @Query("SELECT * FROM revision_items WHERE userId = :userId ORDER BY scheduledDate ASC")
    fun getRevisionsByUser(userId: Long): Flow<List<RevisionItemEntity>>

    @Query("SELECT * FROM revision_items WHERE topicId = :topicId ORDER BY scheduledDate ASC")
    fun getRevisionsByTopic(topicId: Long): Flow<List<RevisionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: RevisionItemEntity): Long

    @Update
    suspend fun updateRevision(revision: RevisionItemEntity)

    @Query("DELETE FROM revision_items WHERE id = :id")
    suspend fun deleteRevisionById(id: Long)

    @Query("DELETE FROM revision_items WHERE userId = :userId")
    suspend fun deleteRevisionsByUser(userId: Long)
}

@Dao
interface PYQDao {
    @Query("SELECT * FROM pyq_questions WHERE subjectId = :subjectId ORDER BY year DESC, id DESC")
    fun getPYQsBySubject(subjectId: Long): Flow<List<PYQQuestionEntity>>

    @Query("SELECT * FROM pyq_questions WHERE userId = :userId ORDER BY year DESC, id DESC")
    fun getPYQsByUser(userId: Long): Flow<List<PYQQuestionEntity>>

    @Query("SELECT * FROM pyq_questions WHERE topicId = :topicId ORDER BY year DESC")
    fun getPYQsByTopic(topicId: Long): Flow<List<PYQQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPYQ(pyq: PYQQuestionEntity): Long

    @Update
    suspend fun updatePYQ(pyq: PYQQuestionEntity)

    @Query("DELETE FROM pyq_questions WHERE id = :id")
    suspend fun deletePYQById(id: Long)

    @Query("DELETE FROM pyq_questions WHERE userId = :userId")
    suspend fun deletePYQsByUser(userId: Long)
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM study_resources WHERE userId = :userId ORDER BY createdAt DESC")
    fun getResourcesByUser(userId: Long): Flow<List<StudyResourceEntity>>

    @Query("SELECT * FROM study_resources WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getResourcesBySubject(subjectId: Long): Flow<List<StudyResourceEntity>>

    @Query("SELECT * FROM study_resources WHERE topicId = :topicId ORDER BY createdAt DESC")
    fun getResourcesByTopic(topicId: Long): Flow<List<StudyResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: StudyResourceEntity): Long

    @Update
    suspend fun updateResource(resource: StudyResourceEntity)

    @Query("DELETE FROM study_resources WHERE id = :id")
    suspend fun deleteResourceById(id: Long)

    @Query("DELETE FROM study_resources WHERE userId = :userId")
    suspend fun deleteResourcesByUser(userId: Long)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM study_notes WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getNotesByUser(userId: Long): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes WHERE subjectId = :subjectId ORDER BY updatedAt DESC")
    fun getNotesBySubject(subjectId: Long): Flow<List<StudyNoteEntity>>

    @Query("SELECT * FROM study_notes WHERE topicId = :topicId ORDER BY updatedAt DESC")
    fun getNotesByTopic(topicId: Long): Flow<List<StudyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: StudyNoteEntity): Long

    @Update
    suspend fun updateNote(note: StudyNoteEntity)

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM study_notes WHERE userId = :userId")
    suspend fun deleteNotesByUser(userId: Long)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSessionsByUser(userId: Long): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("DELETE FROM focus_sessions WHERE userId = :userId")
    suspend fun deleteFocusSessionsByUser(userId: Long)
}

@Dao
interface StudyActivityDao {
    @Query("SELECT * FROM study_activities WHERE userId = :userId ORDER BY timestamp DESC")
    fun getActivitiesByUser(userId: Long): Flow<List<StudyActivityEntity>>

    @Query("SELECT * FROM study_activities WHERE userId = :userId AND date = :date")
    fun getActivitiesByDate(userId: Long, date: String): Flow<List<StudyActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: StudyActivityEntity): Long

    @Query("DELETE FROM study_activities WHERE userId = :userId")
    suspend fun deleteActivitiesByUser(userId: Long)
}
