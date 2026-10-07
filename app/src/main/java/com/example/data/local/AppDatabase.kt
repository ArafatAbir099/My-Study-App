package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        SemesterEntity::class,
        SubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        ExamCycleEntity::class,
        CalendarTaskEntity::class,
        RevisionItemEntity::class,
        PYQQuestionEntity::class,
        StudyResourceEntity::class,
        StudyNoteEntity::class,
        FocusSessionEntity::class,
        StudyActivityEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun semesterDao(): SemesterDao
    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun topicDao(): TopicDao
    abstract fun examCycleDao(): ExamCycleDao
    abstract fun calendarTaskDao(): CalendarTaskDao
    abstract fun revisionDao(): RevisionDao
    abstract fun pyqDao(): PYQDao
    abstract fun resourceDao(): ResourceDao
    abstract fun noteDao(): NoteDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun studyActivityDao(): StudyActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "semester_study_planner.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
