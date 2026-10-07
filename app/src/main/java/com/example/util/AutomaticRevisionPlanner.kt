package com.example.util

import com.example.data.local.CalendarTaskEntity
import com.example.data.local.ExamCycleEntity
import com.example.data.local.RevisionItemEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TopicEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class GeneratedPlan(
    val tasks: List<CalendarTaskEntity>,
    val revisions: List<RevisionItemEntity>,
    val warnings: List<String>
)

object AutomaticRevisionPlanner {

    private val standardDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val flexibleDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()),
        SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMMM dd yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMMM d yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH)
    )

    /**
     * Parses arbitrary date string into Date, or null if unparseable.
     */
    fun parseFlexibleDate(dateStr: String): Date? {
        val raw = dateStr.trim()
        if (raw.isBlank()) return null

        // Handle natural phrases
        val lower = raw.lowercase()
        val todayCal = Calendar.getInstance()
        when {
            lower == "today" -> return todayCal.time
            lower == "tomorrow" -> {
                todayCal.add(Calendar.DAY_OF_YEAR, 1)
                return todayCal.time
            }
            lower.startsWith("in ") && lower.contains("day") -> {
                val num = Regex("\\d+").find(lower)?.value?.toIntOrNull() ?: 1
                todayCal.add(Calendar.DAY_OF_YEAR, num)
                return todayCal.time
            }
            lower.startsWith("in ") && (lower.contains("week") || lower.contains("wk")) -> {
                val num = Regex("\\d+").find(lower)?.value?.toIntOrNull() ?: 1
                todayCal.add(Calendar.DAY_OF_YEAR, num * 7)
                return todayCal.time
            }
        }

        // Clean ordinal suffixes: e.g. "15th October 2026" -> "15 October 2026"
        val clean = raw.replace(Regex("(?<=\\d)(st|nd|rd|th)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[,.]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        for (fmt in flexibleDateFormats) {
            try {
                val parsed = fmt.parse(raw) ?: fmt.parse(clean)
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }

        // Try appending current year if user typed e.g. "15 October" or "Oct 15"
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        for (fmt in listOf(
            SimpleDateFormat("dd MMMM", Locale.ENGLISH),
            SimpleDateFormat("d MMMM", Locale.ENGLISH),
            SimpleDateFormat("MMMM dd", Locale.ENGLISH),
            SimpleDateFormat("MMMM d", Locale.ENGLISH),
            SimpleDateFormat("dd MMM", Locale.ENGLISH),
            SimpleDateFormat("d MMM", Locale.ENGLISH),
            SimpleDateFormat("MMM dd", Locale.ENGLISH),
            SimpleDateFormat("MMM d", Locale.ENGLISH),
            SimpleDateFormat("dd/MM", Locale.getDefault()),
            SimpleDateFormat("MM/dd", Locale.getDefault())
        )) {
            try {
                val parsed = fmt.parse(clean) ?: fmt.parse(raw)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.YEAR, currentYear)
                    }
                    // If month is in past compared to today, assume next year
                    if (cal.timeInMillis < System.currentTimeMillis() - 86400000L * 30) {
                        cal.add(Calendar.YEAR, 1)
                    }
                    return cal.time
                }
            } catch (_: Exception) {}
        }

        // Extremely lenient fallback: try finding any numbers
        val numbers = Regex("\\d+").findAll(raw).map { it.value.toInt() }.toList()
        if (numbers.size >= 3) {
            val y = numbers.find { it > 1900 } ?: currentYear
            val remaining = numbers.filter { it != y }
            if (remaining.size >= 2) {
                val m = (remaining[1] - 1).coerceIn(0, 11)
                val d = remaining[0].coerceIn(1, 31)
                val cal = Calendar.getInstance().apply {
                    set(y, m, d)
                }
                return cal.time
            }
        }

        return null
    }

    /**
     * Normalizes any valid date string into standard yyyy-MM-dd format.
     */
    fun normalizeDate(dateStr: String): String {
        val parsed = parseFlexibleDate(dateStr)
        return if (parsed != null) {
            standardDateFormat.format(parsed)
        } else {
            dateStr.trim()
        }
    }

    /**
     * Intelligently computes the study & revision schedule for an exam.
     * Respects exam type, available days, topic status, and balances workload.
     */
    fun generateExamStudyPlan(
        exam: ExamCycleEntity,
        subject: SubjectEntity?,
        topics: List<TopicEntity>,
        existingCompletedTasks: List<CalendarTaskEntity>,
        otherExams: List<ExamCycleEntity>,
        todayStr: String,
        preserveCompleted: Boolean = true
    ): GeneratedPlan {
        val warnings = mutableListOf<String>()
        val courseName = subject?.name ?: exam.name

        val todayDate = parseFlexibleDate(todayStr) ?: Date()
        val examDate = parseFlexibleDate(exam.examDate) ?: Date()

        val diffMillis = examDate.time - todayDate.time
        val daysUntilExam = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        if (daysUntilExam < 0) {
            return GeneratedPlan(emptyList(), emptyList(), listOf("Exam date has already passed."))
        }

        val generatedTasks = mutableListOf<CalendarTaskEntity>()
        val generatedRevisions = mutableListOf<RevisionItemEntity>()

        fun datePlus(days: Int): String {
            val cal = Calendar.getInstance().apply {
                time = todayDate
                add(Calendar.DAY_OF_YEAR, days)
            }
            return standardDateFormat.format(cal.time)
        }

        val examDateNormalized = standardDateFormat.format(examDate)

        // Case: Exam is Today (Day 0)
        if (daysUntilExam == 0) {
            generatedTasks.add(
                CalendarTaskEntity(
                    userId = exam.userId,
                    semesterId = exam.semesterId,
                    title = "⚡ ${courseName} ${exam.examType}: Quick Formula & Key Notes Review",
                    subjectId = exam.subjectId,
                    examId = exam.id,
                    date = examDateNormalized,
                    startTime = "08:00",
                    endTime = "08:45",
                    durationMinutes = 45,
                    notes = "Light warm-up only. Rest and stay focused for exam at ${exam.examTime}.",
                    taskType = "EXAM_PREP"
                )
            )
            generatedTasks.add(
                CalendarTaskEntity(
                    userId = exam.userId,
                    semesterId = exam.semesterId,
                    title = "🎓 ${courseName} ${exam.examType} (at ${exam.examTime})",
                    subjectId = exam.subjectId,
                    examId = exam.id,
                    date = examDateNormalized,
                    startTime = exam.examTime.ifBlank { "10:30 AM" },
                    endTime = calculateEndTime(exam.examTime.ifBlank { "10:30 AM" }, exam.durationMinutes),
                    durationMinutes = exam.durationMinutes,
                    notes = "Exam Day! Arrive early, stay calm, and review all answers.",
                    taskType = "EXAM"
                )
            )
            return GeneratedPlan(generatedTasks, generatedRevisions, warnings)
        }

        // Available study days strictly before exam day
        val availableStudyDays = max(1, daysUntilExam)

        if (daysUntilExam in 1..2) {
            warnings.add("Exam is in $daysUntilExam day(s)! Accelerated intensive study and revision schedule generated.")
        }

        // -------------------------------------------------------------
        // IF TOPICS ARE EMPTY: Generate progressive syllabus milestone plan
        // -------------------------------------------------------------
        if (topics.isEmpty()) {
            warnings.add("No detailed syllabus topics added yet. Progressive preparation milestones have been scheduled. Upload your syllabus picture to automatically refine with exact topic names!")

            val phaseCount = minOf(availableStudyDays, 5)
            val step = max(1, availableStudyDays / phaseCount)

            val milestoneTitles = listOf(
                "Core Foundations & Definitions — Study",
                "Key Algorithms & Concepts Deep Dive — Study",
                "Problem Solving & Past Paper Practice — Study",
                "Comprehensive Spaced Revision — Revision",
                "Final Formula & Weak Topics Review — Revision"
            )

            for (i in 0 until phaseCount) {
                val dayOffset = minOf(i * step, availableStudyDays - 1)
                val mDate = datePlus(dayOffset)
                val mTitle = milestoneTitles.getOrElse(i) { "Module ${i + 1} Study & Practice" }
                val isRev = mTitle.contains("Revision")

                generatedTasks.add(
                    CalendarTaskEntity(
                        userId = exam.userId,
                        semesterId = exam.semesterId,
                        title = "${courseName}: $mTitle",
                        subjectId = exam.subjectId,
                        examId = exam.id,
                        date = mDate,
                        startTime = if (i % 2 == 0) "10:00" else "15:00",
                        endTime = calculateEndTime(if (i % 2 == 0) "10:00" else "15:00", 60),
                        durationMinutes = 60,
                        notes = "Targeted milestone prep for ${exam.examType}. Upload syllabus picture to auto-populate exact topics.",
                        taskType = if (isRev) "REVISION" else "STUDY"
                    )
                )

                if (isRev) {
                    generatedRevisions.add(
                        RevisionItemEntity(
                            userId = exam.userId,
                            topicId = null,
                            subjectId = exam.subjectId,
                            semesterId = exam.semesterId,
                            scheduledDate = mDate,
                            revisionNumber = i + 1,
                            status = "PENDING",
                            notes = "$courseName: $mTitle"
                        )
                    )
                }
            }

            // Day before exam final review
            if (availableStudyDays >= 2) {
                val dayBeforeExam = datePlus(availableStudyDays - 1)
                generatedTasks.add(
                    CalendarTaskEntity(
                        userId = exam.userId,
                        semesterId = exam.semesterId,
                        title = "🎯 ${courseName} ${exam.examType}: Final Quick Revision",
                        subjectId = exam.subjectId,
                        examId = exam.id,
                        date = dayBeforeExam,
                        startTime = "16:00",
                        endTime = "17:30",
                        durationMinutes = 90,
                        notes = "Final checklist review and formula recall before tomorrow's exam.",
                        taskType = "REVISION"
                    )
                )
                generatedRevisions.add(
                    RevisionItemEntity(
                        userId = exam.userId,
                        topicId = null,
                        subjectId = exam.subjectId,
                        semesterId = exam.semesterId,
                        scheduledDate = dayBeforeExam,
                        revisionNumber = 1,
                        status = "PENDING",
                        notes = "🎯 $courseName ${exam.examType}: Final Quick Revision"
                    )
                )
            }

            // Exam Day Task
            generatedTasks.add(
                CalendarTaskEntity(
                    userId = exam.userId,
                    semesterId = exam.semesterId,
                    title = "🎓 ${courseName} ${exam.examType} (at ${exam.examTime})",
                    subjectId = exam.subjectId,
                    examId = exam.id,
                    date = examDateNormalized,
                    startTime = exam.examTime.ifBlank { "10:30 AM" },
                    endTime = calculateEndTime(exam.examTime.ifBlank { "10:30 AM" }, exam.durationMinutes),
                    durationMinutes = exam.durationMinutes,
                    notes = "Exam Day! Arrive early with all required materials.",
                    taskType = "EXAM"
                )
            )

            return GeneratedPlan(generatedTasks, generatedRevisions, warnings)
        }

        // -------------------------------------------------------------
        // TOPICS ARE PRESENT: Distribute exact syllabus topics
        // -------------------------------------------------------------
        val completedTopicIds = existingCompletedTasks.filter { it.isCompleted }.mapNotNull { it.topicId }.toSet()
        val remainingTopics = topics.filter { it.status != "COMPLETED" && it.id !in completedTopicIds }
        val coveredTopics = topics.filter { it.id in completedTopicIds || it.status == "COMPLETED" }

        val examType = exam.examType.uppercase()
        val isQuiz = examType.contains("QUIZ")
        val isFinal = examType.contains("FINAL")

        val studyDaysBudget: Int
        val revisionDaysBudget: Int

        when {
            availableStudyDays <= 2 -> {
                studyDaysBudget = 1
                revisionDaysBudget = 1
            }
            availableStudyDays <= 5 -> {
                studyDaysBudget = max(1, availableStudyDays - 2)
                revisionDaysBudget = availableStudyDays - studyDaysBudget
            }
            isQuiz -> {
                studyDaysBudget = max(1, (availableStudyDays * 0.6).toInt())
                revisionDaysBudget = availableStudyDays - studyDaysBudget
            }
            isFinal -> {
                studyDaysBudget = max(1, (availableStudyDays * 0.5).toInt())
                revisionDaysBudget = availableStudyDays - studyDaysBudget
            }
            else -> { // Midterm
                studyDaysBudget = max(1, (availableStudyDays * 0.55).toInt())
                revisionDaysBudget = availableStudyDays - studyDaysBudget
            }
        }

        val prioritizedRemaining = remainingTopics.sortedWith(
            compareByDescending<TopicEntity> { it.importance == "VERY_IMPORTANT" }
                .thenByDescending { it.understanding == "WEAK" }
                .thenBy { it.id }
        )

        // Study session distribution: Day by Day topic study
        val topicsPerStudyDay = max(1, (prioritizedRemaining.size + studyDaysBudget - 1) / max(1, studyDaysBudget))
        var currentDayOffset = 0
        val studiedTopicChunks = mutableListOf<List<TopicEntity>>()

        prioritizedRemaining.chunked(topicsPerStudyDay).forEachIndexed { index, chunk ->
            if (currentDayOffset < availableStudyDays - 1) {
                val dayDate = datePlus(currentDayOffset)
                val startTime = if (index % 2 == 0) "10:00" else "15:00"
                studiedTopicChunks.add(chunk)

                for (topic in chunk) {
                    generatedTasks.add(
                        CalendarTaskEntity(
                            userId = exam.userId,
                            semesterId = exam.semesterId,
                            title = "${courseName}: ${topic.name} — Study",
                            subjectId = exam.subjectId,
                            topicId = topic.id,
                            examId = exam.id,
                            date = dayDate,
                            startTime = startTime,
                            endTime = calculateEndTime(startTime, 60),
                            durationMinutes = 60,
                            notes = "Initial syllabus study. Master key concepts & definitions.",
                            taskType = "STUDY"
                        )
                    )

                    // Spaced Revision 1 scheduled 2 to 4 days later
                    val rev1Day = minOf(currentDayOffset + 3, availableStudyDays - 1)
                    if (rev1Day < availableStudyDays) {
                        val revDate = datePlus(rev1Day)
                        generatedRevisions.add(
                            RevisionItemEntity(
                                userId = exam.userId,
                                topicId = topic.id,
                                subjectId = exam.subjectId,
                                semesterId = exam.semesterId,
                                scheduledDate = revDate,
                                revisionNumber = 1,
                                status = "PENDING",
                                notes = "Spaced Revision for ${topic.name}"
                            )
                        )
                    }
                }
                currentDayOffset++
            }
        }

        // Interleaved Revision Sessions (combining previously studied topics, e.g. Topic A + Topic B — Revision)
        val allTopicsToReview = if (prioritizedRemaining.isNotEmpty()) prioritizedRemaining else coveredTopics
        if (allTopicsToReview.isNotEmpty() && currentDayOffset < availableStudyDays - 1) {
            val revChunks = allTopicsToReview.chunked(max(1, (allTopicsToReview.size / max(1, revisionDaysBudget))))
            for (chunk in revChunks) {
                if (currentDayOffset >= availableStudyDays - 1) break
                val rDate = datePlus(currentDayOffset)
                val summaryTopicNames = chunk.take(3).joinToString(" + ") { it.name }

                generatedTasks.add(
                    CalendarTaskEntity(
                        userId = exam.userId,
                        semesterId = exam.semesterId,
                        title = "${courseName}: ${summaryTopicNames} — Revision",
                        subjectId = exam.subjectId,
                        topicId = chunk.firstOrNull()?.id,
                        examId = exam.id,
                        date = rDate,
                        startTime = "18:00",
                        endTime = "19:00",
                        durationMinutes = 60,
                        notes = "Spaced Revision: Test yourself without looking at notes. Practice active recall.",
                        taskType = "REVISION"
                    )
                )
                currentDayOffset++
            }
        }

        // Final Quick Revision on the Day Before Exam
        if (availableStudyDays >= 2) {
            val dayBeforeExam = datePlus(availableStudyDays - 1)
            generatedTasks.add(
                CalendarTaskEntity(
                    userId = exam.userId,
                    semesterId = exam.semesterId,
                    title = "🎯 ${courseName} ${exam.examType}: Final Quick Revision",
                    subjectId = exam.subjectId,
                    examId = exam.id,
                    date = dayBeforeExam,
                    startTime = "16:00",
                    endTime = "17:30",
                    durationMinutes = 90,
                    notes = "Final syllabus checklist review. Review cheat-sheets, formulas, and diagrams.",
                    taskType = "REVISION"
                )
            )
        }

        // Exam Day Task at exact exam time
        generatedTasks.add(
            CalendarTaskEntity(
                userId = exam.userId,
                semesterId = exam.semesterId,
                title = "🎓 ${courseName} ${exam.examType} (at ${exam.examTime})",
                subjectId = exam.subjectId,
                examId = exam.id,
                date = examDateNormalized,
                startTime = exam.examTime.ifBlank { "10:30 AM" },
                endTime = calculateEndTime(exam.examTime.ifBlank { "10:30 AM" }, exam.durationMinutes),
                durationMinutes = exam.durationMinutes,
                notes = "Exam Day! Stay calm, review high-level formulas only, and arrive early.",
                taskType = "EXAM"
            )
        )

        return GeneratedPlan(generatedTasks, generatedRevisions, warnings)
    }

    /**
     * Reschedules any missed past tasks into future available days before their exam.
     */
    fun rescheduleMissedTasks(
        pastUncompletedTasks: List<CalendarTaskEntity>,
        exams: List<ExamCycleEntity>,
        todayStr: String
    ): List<CalendarTaskEntity> {
        val updated = mutableListOf<CalendarTaskEntity>()
        var dayOffset = 0

        fun datePlus(days: Int): String {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
            return standardDateFormat.format(cal.time)
        }

        for (task in pastUncompletedTasks) {
            val relevantExam = exams.firstOrNull { it.id == task.examId || (task.subjectId != null && it.subjectId == task.subjectId) }
            val examDateStr = relevantExam?.examDate ?: datePlus(7)

            val targetDate = datePlus(dayOffset)
            if (targetDate < examDateStr) {
                updated.add(
                    task.copy(
                        date = targetDate,
                        notes = "${task.notes} (Auto-rescheduled from ${task.date})"
                    )
                )
                dayOffset = (dayOffset + 1) % 4
            } else {
                updated.add(
                    task.copy(
                        date = todayStr,
                        notes = "URGENT: Rescheduled for today before upcoming exam!"
                    )
                )
            }
        }
        return updated
    }

    fun calculateEndTime(startTime: String, durationMinutes: Int): String {
        return try {
            val parts = startTime.split(":")
            val hour = parts[0].trim().toInt()
            val min = parts[1].take(2).trim().toInt()
            val isPm = startTime.contains("PM", ignoreCase = true)
            val isAm = startTime.contains("AM", ignoreCase = true)
            var normalizedHour = hour
            if (isPm && hour != 12) normalizedHour += 12
            if (isAm && hour == 12) normalizedHour = 0

            var totalMin = normalizedHour * 60 + min + durationMinutes
            val endHour = (totalMin / 60) % 24
            val endMin = totalMin % 60
            String.format(Locale.getDefault(), "%02d:%02d", endHour, endMin)
        } catch (_: Exception) {
            "12:00"
        }
    }
}
