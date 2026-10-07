import { CalendarTask, ExamCycle, RevisionItem, Subject, Topic } from '../types';
import { calculateEndTime, formatDateStr, normalizeDate, parseFlexibleDate } from './dateUtils';

export interface GeneratedPlan {
  tasks: Omit<CalendarTask, 'id'>[];
  revisions: Omit<RevisionItem, 'id'>[];
  warnings: string[];
}

export function generateExamStudyPlan(
  exam: ExamCycle,
  subject: Subject | undefined,
  topics: Topic[],
  existingCompletedTasks: CalendarTask[],
  _otherExams: ExamCycle[],
  todayStr: string
): GeneratedPlan {
  const warnings: string[] = [];
  const courseName = subject?.name || exam.name;

  const todayDate = parseFlexibleDate(todayStr) || new Date();
  todayDate.setHours(0, 0, 0, 0);

  const examDate = parseFlexibleDate(exam.examDate) || new Date();
  examDate.setHours(0, 0, 0, 0);

  const diffMillis = examDate.getTime() - todayDate.getTime();
  const daysUntilExam = Math.round(diffMillis / (1000 * 60 * 60 * 24));

  if (daysUntilExam < 0) {
    return {
      tasks: [],
      revisions: [],
      warnings: ['Exam date has already passed.'],
    };
  }

  const generatedTasks: Omit<CalendarTask, 'id'>[] = [];
  const generatedRevisions: Omit<RevisionItem, 'id'>[] = [];

  const datePlus = (days: number): string => {
    const d = new Date(todayDate);
    d.setDate(d.getDate() + days);
    return formatDateStr(d);
  };

  const examDateNormalized = normalizeDate(exam.examDate);

  // Case: Exam is Today (Day 0)
  if (daysUntilExam === 0) {
    generatedTasks.push({
      userId: exam.userId,
      semesterId: exam.semesterId,
      title: `⚡ ${courseName} ${exam.examType}: Quick Formula & Key Notes Review`,
      subjectId: exam.subjectId,
      examId: exam.id,
      date: examDateNormalized,
      startTime: '08:00',
      endTime: '08:45',
      durationMinutes: 45,
      isCompleted: false,
      notes: `Light warm-up only. Rest and stay focused for exam at ${exam.examTime}.`,
      taskType: 'EXAM_PREP',
    });

    generatedTasks.push({
      userId: exam.userId,
      semesterId: exam.semesterId,
      title: `🎓 ${courseName} ${exam.examType} (at ${exam.examTime})`,
      subjectId: exam.subjectId,
      examId: exam.id,
      date: examDateNormalized,
      startTime: exam.examTime || '10:30 AM',
      endTime: calculateEndTime(exam.examTime || '10:30 AM', exam.durationMinutes),
      durationMinutes: exam.durationMinutes,
      isCompleted: false,
      notes: 'Exam Day! Arrive early, stay calm, and review all answers.',
      taskType: 'EXAM',
    });

    return { tasks: generatedTasks, revisions: generatedRevisions, warnings };
  }

  const availableStudyDays = Math.max(1, daysUntilExam);

  if (daysUntilExam >= 1 && daysUntilExam <= 2) {
    warnings.push(`Exam is in ${daysUntilExam} day(s)! Accelerated intensive study and revision schedule generated.`);
  }

  // CASE: Empty topics -> generate progressive syllabus milestone plan
  if (topics.length === 0) {
    warnings.push(
      'No detailed syllabus topics added yet. Progressive preparation milestones have been scheduled. Add or paste syllabus topics to automatically refine with exact topic names!'
    );

    const phaseCount = Math.min(availableStudyDays, 5);
    const step = Math.max(1, Math.floor(availableStudyDays / phaseCount));

    const milestoneTitles = [
      'Core Foundations & Definitions — Study',
      'Key Algorithms & Concepts Deep Dive — Study',
      'Problem Solving & Past Paper Practice — Study',
      'Comprehensive Spaced Revision — Revision',
      'Final Formula & Weak Topics Review — Revision',
    ];

    for (let i = 0; i < phaseCount; i++) {
      const dayOffset = Math.min(i * step, availableStudyDays - 1);
      const mDate = datePlus(dayOffset);
      const mTitle = milestoneTitles[i] || `Module ${i + 1} Study & Practice`;
      const isRev = mTitle.includes('Revision');

      generatedTasks.push({
        userId: exam.userId,
        semesterId: exam.semesterId,
        title: `${courseName}: ${mTitle}`,
        subjectId: exam.subjectId,
        examId: exam.id,
        date: mDate,
        startTime: i % 2 === 0 ? '10:00' : '15:00',
        endTime: calculateEndTime(i % 2 === 0 ? '10:00' : '15:00', 60),
        durationMinutes: 60,
        isCompleted: false,
        notes: `Targeted milestone prep for ${exam.examType}. Add syllabus topics to auto-populate exact topics.`,
        taskType: isRev ? 'REVISION' : 'STUDY',
      });

      if (isRev) {
        generatedRevisions.push({
          userId: exam.userId,
          topicId: null,
          subjectId: exam.subjectId,
          semesterId: exam.semesterId,
          scheduledDate: mDate,
          revisionNumber: i + 1,
          status: 'PENDING',
          notes: `${courseName}: ${mTitle}`,
        });
      }
    }

    if (availableStudyDays >= 2) {
      const dayBeforeExam = datePlus(availableStudyDays - 1);
      generatedTasks.push({
        userId: exam.userId,
        semesterId: exam.semesterId,
        title: `🎯 ${courseName} ${exam.examType}: Final Quick Revision`,
        subjectId: exam.subjectId,
        examId: exam.id,
        date: dayBeforeExam,
        startTime: '16:00',
        endTime: '17:30',
        durationMinutes: 90,
        isCompleted: false,
        notes: "Final checklist review and formula recall before tomorrow's exam.",
        taskType: 'REVISION',
      });

      generatedRevisions.push({
        userId: exam.userId,
        topicId: null,
        subjectId: exam.subjectId,
        semesterId: exam.semesterId,
        scheduledDate: dayBeforeExam,
        revisionNumber: 1,
        status: 'PENDING',
        notes: `🎯 ${courseName} ${exam.examType}: Final Quick Revision`,
      });
    }

    // Exam Day Task
    generatedTasks.push({
      userId: exam.userId,
      semesterId: exam.semesterId,
      title: `🎓 ${courseName} ${exam.examType} (at ${exam.examTime})`,
      subjectId: exam.subjectId,
      examId: exam.id,
      date: examDateNormalized,
      startTime: exam.examTime || '10:30 AM',
      endTime: calculateEndTime(exam.examTime || '10:30 AM', exam.durationMinutes),
      durationMinutes: exam.durationMinutes,
      isCompleted: false,
      notes: 'Exam Day! Arrive early with all required materials.',
      taskType: 'EXAM',
    });

    return { tasks: generatedTasks, revisions: generatedRevisions, warnings };
  }

  // CASE: Topics are present
  const completedTopicIds = new Set(
    existingCompletedTasks.filter((t) => t.isCompleted && t.topicId).map((t) => t.topicId as number)
  );
  const remainingTopics = topics.filter((t) => t.status !== 'COMPLETED' && !completedTopicIds.has(t.id));
  const coveredTopics = topics.filter((t) => completedTopicIds.has(t.id) || t.status === 'COMPLETED');

  const examTypeUpper = exam.examType.toUpperCase();
  const isQuiz = examTypeUpper.includes('QUIZ');
  const isFinal = examTypeUpper.includes('FINAL');

  let studyDaysBudget: number;
  let revisionDaysBudget: number;

  if (availableStudyDays <= 2) {
    studyDaysBudget = 1;
    revisionDaysBudget = 1;
  } else if (availableStudyDays <= 5) {
    studyDaysBudget = Math.max(1, availableStudyDays - 2);
    revisionDaysBudget = availableStudyDays - studyDaysBudget;
  } else if (isQuiz) {
    studyDaysBudget = Math.max(1, Math.floor(availableStudyDays * 0.6));
    revisionDaysBudget = availableStudyDays - studyDaysBudget;
  } else if (isFinal) {
    studyDaysBudget = Math.max(1, Math.floor(availableStudyDays * 0.5));
    revisionDaysBudget = availableStudyDays - studyDaysBudget;
  } else {
    studyDaysBudget = Math.max(1, Math.floor(availableStudyDays * 0.55));
    revisionDaysBudget = availableStudyDays - studyDaysBudget;
  }

  const prioritizedRemaining = [...remainingTopics].sort((a, b) => {
    if (a.importance === 'VERY_IMPORTANT' && b.importance !== 'VERY_IMPORTANT') return -1;
    if (b.importance === 'VERY_IMPORTANT' && a.importance !== 'VERY_IMPORTANT') return 1;
    if (a.understanding === 'WEAK' && b.understanding !== 'WEAK') return -1;
    if (b.understanding === 'WEAK' && a.understanding !== 'WEAK') return 1;
    return a.id - b.id;
  });

  const topicsPerStudyDay = Math.max(
    1,
    Math.ceil(prioritizedRemaining.length / Math.max(1, studyDaysBudget))
  );

  let currentDayOffset = 0;
  for (let i = 0; i < prioritizedRemaining.length; i += topicsPerStudyDay) {
    if (currentDayOffset >= availableStudyDays - 1) break;

    const chunk = prioritizedRemaining.slice(i, i + topicsPerStudyDay);
    const dayDate = datePlus(currentDayOffset);
    const startTime = currentDayOffset % 2 === 0 ? '10:00' : '15:00';

    for (const topic of chunk) {
      generatedTasks.push({
        userId: exam.userId,
        semesterId: exam.semesterId,
        title: `${courseName}: ${topic.name} — Study`,
        subjectId: exam.subjectId,
        topicId: topic.id,
        examId: exam.id,
        date: dayDate,
        startTime,
        endTime: calculateEndTime(startTime, 60),
        durationMinutes: 60,
        isCompleted: false,
        notes: 'Initial syllabus study. Master key concepts & definitions.',
        taskType: 'STUDY',
      });

      // Spaced Revision 1 scheduled 2-4 days later
      const rev1Day = Math.min(currentDayOffset + 3, availableStudyDays - 1);
      if (rev1Day < availableStudyDays) {
        const revDate = datePlus(rev1Day);
        generatedRevisions.push({
          userId: exam.userId,
          topicId: topic.id,
          subjectId: exam.subjectId,
          semesterId: exam.semesterId,
          scheduledDate: revDate,
          revisionNumber: 1,
          status: 'PENDING',
          notes: `Spaced Revision for ${topic.name}`,
        });
      }
    }
    currentDayOffset++;
  }

  // Interleaved Revision Sessions
  const allTopicsToReview = prioritizedRemaining.length > 0 ? prioritizedRemaining : coveredTopics;
  if (allTopicsToReview.length > 0 && currentDayOffset < availableStudyDays - 1) {
    const revChunkSize = Math.max(1, Math.ceil(allTopicsToReview.length / Math.max(1, revisionDaysBudget)));
    for (let i = 0; i < allTopicsToReview.length; i += revChunkSize) {
      if (currentDayOffset >= availableStudyDays - 1) break;

      const chunk = allTopicsToReview.slice(i, i + revChunkSize);
      const rDate = datePlus(currentDayOffset);
      const summaryTopicNames = chunk
        .slice(0, 3)
        .map((t) => t.name)
        .join(' + ');

      generatedTasks.push({
        userId: exam.userId,
        semesterId: exam.semesterId,
        title: `${courseName}: ${summaryTopicNames} — Revision`,
        subjectId: exam.subjectId,
        topicId: chunk[0]?.id,
        examId: exam.id,
        date: rDate,
        startTime: '18:00',
        endTime: '19:00',
        durationMinutes: 60,
        isCompleted: false,
        notes: 'Spaced Revision: Test yourself without looking at notes. Practice active recall.',
        taskType: 'REVISION',
      });
      currentDayOffset++;
    }
  }

  // Day before exam quick revision
  if (availableStudyDays >= 2) {
    const dayBeforeExam = datePlus(availableStudyDays - 1);
    generatedTasks.push({
      userId: exam.userId,
      semesterId: exam.semesterId,
      title: `🎯 ${courseName} ${exam.examType}: Final Quick Revision`,
      subjectId: exam.subjectId,
      examId: exam.id,
      date: dayBeforeExam,
      startTime: '16:00',
      endTime: '17:30',
      durationMinutes: 90,
      isCompleted: false,
      notes: 'Final syllabus checklist review. Review cheat-sheets, formulas, and diagrams.',
      taskType: 'REVISION',
    });
  }

  // Exam Day Task
  generatedTasks.push({
    userId: exam.userId,
    semesterId: exam.semesterId,
    title: `🎓 ${courseName} ${exam.examType} (at ${exam.examTime})`,
    subjectId: exam.subjectId,
    examId: exam.id,
    date: examDateNormalized,
    startTime: exam.examTime || '10:30 AM',
    endTime: calculateEndTime(exam.examTime || '10:30 AM', exam.durationMinutes),
    durationMinutes: exam.durationMinutes,
    isCompleted: false,
    notes: 'Exam Day! Stay calm, review high-level formulas only, and arrive early.',
    taskType: 'EXAM',
  });

  return { tasks: generatedTasks, revisions: generatedRevisions, warnings };
}

export function rescheduleMissedTasks(
  pastUncompletedTasks: CalendarTask[],
  exams: ExamCycle[],
  todayStr: string
): CalendarTask[] {
  const updated: CalendarTask[] = [];
  let dayOffset = 0;

  const datePlus = (days: number): string => {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return formatDateStr(d);
  };

  for (const task of pastUncompletedTasks) {
    const relevantExam = exams.find(
      (e) => e.id === task.examId || (task.subjectId && e.subjectId === task.subjectId)
    );
    const examDateStr = relevantExam?.examDate || datePlus(7);

    const targetDate = datePlus(dayOffset);
    if (targetDate < examDateStr) {
      updated.push({
        ...task,
        date: targetDate,
        notes: `${task.notes} (Auto-rescheduled from ${task.date})`,
      });
      dayOffset = (dayOffset + 1) % 4;
    } else {
      updated.push({
        ...task,
        date: todayStr,
        notes: 'URGENT: Rescheduled for today before upcoming exam!',
      });
    }
  }
  return updated;
}
