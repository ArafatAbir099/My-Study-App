import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import {
  CalendarTask,
  Chapter,
  DayActivitySummary,
  ExamCycle,
  ExamReadinessScore,
  FocusSession,
  PYQQuestion,
  PYQStatus,
  RevisionItem,
  Semester,
  StudyActivity,
  StudyNote,
  StudyResource,
  Subject,
  TaskType,
  Topic,
  TopicImportance,
  TopicPYQAnalysis,
  TopicUnderstanding,
  User,
  AIRevisionRecommendation,
  ExtractedChapterDraft,
} from '../types';
import { formatDateStr, getTodayStr, normalizeDate, getDaysRemaining } from '../util/dateUtils';
import { generateExamStudyPlan, rescheduleMissedTasks } from '../util/revisionPlanner';
import { parseSyllabusContent } from '../util/syllabusParser';

interface PlannerContextType {
  currentUser: User | null;
  allUsers: User[];
  semesters: Semester[];
  selectedSemesterId: number | null;
  subjects: Subject[];
  chapters: Chapter[];
  topics: Topic[];
  exams: ExamCycle[];
  tasks: CalendarTask[];
  revisions: RevisionItem[];
  pyqs: PYQQuestion[];
  resources: StudyResource[];
  notes: StudyNote[];
  focusSessions: FocusSession[];
  studyActivities: StudyActivity[];
  aiRevisionRecommendations: AIRevisionRecommendation[];
  aiRevisionSummary: string;
  isGeneratingRevisionPlan: boolean;
  heatmapFilter: '3_MONTHS' | '6_MONTHS' | '1_YEAR';
  setHeatmapFilter: (filter: '3_MONTHS' | '6_MONTHS' | '1_YEAR') => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;

  // Derived state
  heatmapDays: DayActivitySummary[];
  streakStats: { currentStreak: number; longestStreak: number };
  studyRecommendations: TopicPYQAnalysis[];
  examReadinessList: ExamReadinessScore[];
  dynamicRevisionPlan: AIRevisionRecommendation[];

  // Auth
  login: (usernameOrEmail: string, pass: string) => { success: boolean; message: string };
  register: (username: string, email: string, pass: string, confirmPass: string) => { success: boolean; message: string };
  logout: () => void;
  updateProfile: (name: string, email: string, university: string, studentId: string) => void;
  switchUser: (user: User) => void;
  resetUserData: () => void;

  // Semesters
  selectSemester: (id: number) => void;
  addSemester: (name: string, year: string, start: string, end: string, notes: string) => Semester;
  updateSemester: (semester: Semester) => void;
  toggleSemesterArchive: (semester: Semester) => void;
  deleteSemester: (id: number) => void;

  // Subjects
  addSubject: (
    name: string,
    code: string,
    credits: number,
    teacher: string,
    colorHex: string,
    iconName: string,
    notes: string,
    upcomingExamDate?: string | null,
    upcomingExamType?: string | null,
    upcomingExamTime?: string | null
  ) => Subject;
  updateSubject: (subject: Subject) => void;
  deleteSubject: (id: number) => void;
  attachCourseSyllabus: (subjectId: number, rawText: string) => number;
  saveStructuredSyllabus: (subjectId: number, chaptersDraft: ExtractedChapterDraft[]) => number;
  setSubjectExamDate: (
    subjectId: number,
    examDate: string | null,
    examType?: string,
    examTime?: string,
    durationMinutes?: number
  ) => void;

  // Topics
  addTopic: (
    subjectId: number,
    chapterId: number,
    name: string,
    description: string,
    importance: TopicImportance
  ) => Topic;
  updateTopic: (topic: Topic) => void;
  deleteTopic: (id: number) => void;
  completeTopic: (topic: Topic, understanding: TopicUnderstanding) => void;
  updateTopicUnderstanding: (topicId: number, understanding: TopicUnderstanding) => void;

  // Tasks
  addTask: (
    title: string,
    subjectId: number | null,
    topicId: number | null,
    date: string,
    startTime: string,
    endTime: string,
    durationMinutes: number,
    notes: string,
    taskType?: TaskType
  ) => CalendarTask;
  updateTask: (task: CalendarTask) => void;
  toggleTask: (task: CalendarTask) => void;
  deleteTask: (id: number) => void;
  rescheduleTask: (task: CalendarTask, newDate: string, newStartTime?: string) => void;
  checkAndRescheduleMissedTasks: () => number;

  // Revisions
  completeRevision: (revision: RevisionItem) => void;
  rescheduleRevision: (revision: RevisionItem, newDate: string) => void;
  skipRevision: (revision: RevisionItem) => void;
  acceptRevision: (revision: RevisionItem) => void;

  // AI Revision Plan
  generateAIRevisionPlan: () => Promise<{ success: boolean; message: string }>;
  toggleAIRecommendationCompleted: (id: string) => void;

  // Exams
  addExamWithAutoPlan: (
    subjectId: number,
    examType: string,
    examDate: string,
    examTime: string,
    durationMinutes: number,
    notes: string
  ) => { success: boolean; message: string };
  autoPlanAllExams: () => { success: boolean; message: string };
  regenerateExamPlan: (exam: ExamCycle) => { success: boolean; message: string };
  updateExam: (exam: ExamCycle) => void;
  deleteExam: (id: number) => void;

  // PYQs
  addPYQ: (
    subjectId: number,
    topicId: number | null,
    examType: string,
    year: number,
    question: string,
    marks: number,
    difficulty: PYQQuestion['difficulty'],
    status: PYQStatus,
    isImportant: boolean,
    solution: string
  ) => PYQQuestion;
  updatePYQStatus: (pyq: PYQQuestion, newStatus: PYQStatus) => void;
  deletePYQ: (id: number) => void;

  // Resources & Notes
  addResource: (
    subjectId: number | null,
    topicId: number | null,
    title: string,
    type: StudyResource['resourceType'],
    content: string
  ) => StudyResource;
  deleteResource: (id: number) => void;
  toggleBookmarkResource: (id: number) => void;

  addNote: (
    subjectId: number | null,
    topicId: number | null,
    title: string,
    content: string
  ) => StudyNote;
  updateNote: (note: StudyNote) => void;
  deleteNote: (id: number) => void;
  toggleBookmarkNote: (id: number) => void;

  // Focus
  recordFocusSession: (
    subjectId: number | null,
    topicId: number | null,
    title: string,
    minutes: number,
    understanding?: TopicUnderstanding | null
  ) => void;
}

const PlannerContext = createContext<PlannerContextType | undefined>(undefined);

const STORAGE_KEY_PREFIX = 'study_planner_app_clean_v3_';

function loadFromStorage<T>(key: string, defaultValue: T): T {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_PREFIX + key);
    return raw ? JSON.parse(raw) : defaultValue;
  } catch {
    return defaultValue;
  }
}

function saveToStorage<T>(key: string, value: T): void {
  try {
    localStorage.setItem(STORAGE_KEY_PREFIX + key, JSON.stringify(value));
  } catch (err) {
    console.error('Failed to save to localStorage:', err);
  }
}

// Clean initial user
const DEFAULT_USER: User = {
  id: 1,
  username: 'student',
  name: 'Student',
  email: 'student@university.edu',
  passwordHash: 'password',
  university: 'University',
  studentId: '',
  avatarColor: '#4F46E5',
  createdAt: Date.now(),
};

export const PlannerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [allUsers, setAllUsers] = useState<User[]>(() =>
    loadFromStorage<User[]>('users', [DEFAULT_USER])
  );
  const [currentUser, setCurrentUser] = useState<User | null>(() => {
    const saved = loadFromStorage<User | null>('current_user', DEFAULT_USER);
    return saved;
  });

  const [semesters, setSemesters] = useState<Semester[]>(() =>
    loadFromStorage<Semester[]>('semesters', [
      {
        id: 1,
        userId: 1,
        name: 'Semester 1',
        academicYear: '2026-2027',
        startDate: getTodayStr(),
        endDate: '',
        isArchived: false,
        notes: '',
        createdAt: Date.now(),
      },
    ])
  );

  const [selectedSemesterId, setSelectedSemesterId] = useState<number | null>(() => {
    const saved = loadFromStorage<number | null>('selected_sem_id', 1);
    return saved;
  });

  // CLEAN EMPTY ACADEMIC DATA - No hardcoded demo subjects, syllabus, exams or PYQs!
  const [subjects, setSubjects] = useState<Subject[]>(() =>
    loadFromStorage<Subject[]>('subjects', []).filter(
      (s) => s.name !== 'Data Structures & Algorithms' && s.name !== 'Database Management Systems'
    )
  );

  const [chapters, setChapters] = useState<Chapter[]>(() =>
    loadFromStorage<Chapter[]>('chapters', []).filter(
      (c) => !c.title.includes('Linear Data Structures') && !c.title.includes('Relational Model')
    )
  );

  const [topics, setTopics] = useState<Topic[]>(() =>
    loadFromStorage<Topic[]>('topics', []).filter(
      (t) => !t.name.includes('Linked Lists') && !t.name.includes('Binary Search Trees')
    )
  );

  const [exams, setExams] = useState<ExamCycle[]>(() =>
    loadFromStorage<ExamCycle[]>('exams', []).filter((e) => !e.name.includes('DSA Midterm'))
  );

  const [tasks, setTasks] = useState<CalendarTask[]>(() =>
    loadFromStorage<CalendarTask[]>('tasks', []).filter((t) => !t.title.includes('DSA:'))
  );

  const [revisions, setRevisions] = useState<RevisionItem[]>(() =>
    loadFromStorage<RevisionItem[]>('revisions', []).filter((r) => !r.notes.includes('Stacks & Queues'))
  );

  const [pyqs, setPyqs] = useState<PYQQuestion[]>(() =>
    loadFromStorage<PYQQuestion[]>('pyqs', []).filter((p) => !p.questionText.includes('AVL tree'))
  );

  const [resources, setResources] = useState<StudyResource[]>(() =>
    loadFromStorage<StudyResource[]>('resources', []).filter((r) => !r.title.includes('AVL Tree'))
  );

  const [notes, setNotes] = useState<StudyNote[]>(() =>
    loadFromStorage<StudyNote[]>('notes', []).filter((n) => !n.title.includes('AVL Rotations'))
  );

  const [focusSessions, setFocusSessions] = useState<FocusSession[]>(() =>
    loadFromStorage<FocusSession[]>('focus_sessions', [])
  );

  const [studyActivities, setStudyActivities] = useState<StudyActivity[]>(() =>
    loadFromStorage<StudyActivity[]>('activities', [])
  );

  const [aiRevisionRecommendations, setAiRevisionRecommendations] = useState<AIRevisionRecommendation[]>(
    () => loadFromStorage<AIRevisionRecommendation[]>('ai_recs', [])
  );
  const [aiRevisionSummary, setAiRevisionSummary] = useState<string>(() =>
    loadFromStorage<string>('ai_rec_summary', '')
  );
  const [isGeneratingRevisionPlan, setIsGeneratingRevisionPlan] = useState(false);

  const [heatmapFilter, setHeatmapFilter] = useState<'3_MONTHS' | '6_MONTHS' | '1_YEAR'>('3_MONTHS');
  const [searchQuery, setSearchQuery] = useState('');

  // Persist state changes
  useEffect(() => saveToStorage('users', allUsers), [allUsers]);
  useEffect(() => saveToStorage('current_user', currentUser), [currentUser]);
  useEffect(() => saveToStorage('semesters', semesters), [semesters]);
  useEffect(() => saveToStorage('selected_sem_id', selectedSemesterId), [selectedSemesterId]);
  useEffect(() => saveToStorage('subjects', subjects), [subjects]);
  useEffect(() => saveToStorage('chapters', chapters), [chapters]);
  useEffect(() => saveToStorage('topics', topics), [topics]);
  useEffect(() => saveToStorage('exams', exams), [exams]);
  useEffect(() => saveToStorage('tasks', tasks), [tasks]);
  useEffect(() => saveToStorage('revisions', revisions), [revisions]);
  useEffect(() => saveToStorage('pyqs', pyqs), [pyqs]);
  useEffect(() => saveToStorage('resources', resources), [resources]);
  useEffect(() => saveToStorage('notes', notes), [notes]);
  useEffect(() => saveToStorage('focus_sessions', focusSessions), [focusSessions]);
  useEffect(() => saveToStorage('activities', studyActivities), [studyActivities]);
  useEffect(() => saveToStorage('ai_recs', aiRevisionRecommendations), [aiRevisionRecommendations]);
  useEffect(() => saveToStorage('ai_rec_summary', aiRevisionSummary), [aiRevisionSummary]);

  // Log study activity helper
  const logActivity = (
    type: StudyActivity['activityType'],
    durationMinutes: number,
    subjectId: number | null = null,
    topicId: number | null = null,
    metadata = ''
  ) => {
    if (!currentUser) return;
    const newAct: StudyActivity = {
      id: Date.now() + Math.floor(Math.random() * 1000),
      userId: currentUser.id,
      date: getTodayStr(),
      activityType: type,
      durationMinutes,
      subjectId,
      topicId,
      metadata,
      timestamp: Date.now(),
    };
    setStudyActivities((prev) => [newAct, ...prev]);
  };

  // Heatmap Aggregation from real user activity
  const heatmapDays = useMemo<DayActivitySummary[]>(() => {
    const totalDays = heatmapFilter === '3_MONTHS' ? 91 : heatmapFilter === '6_MONTHS' ? 182 : 365;
    const grouped = new Map<string, StudyActivity[]>();
    for (const act of studyActivities) {
      const list = grouped.get(act.date) || [];
      list.push(act);
      grouped.set(act.date, list);
    }

    const result: DayActivitySummary[] = [];
    const today = new Date();

    for (let i = totalDays - 1; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);
      const dStr = formatDateStr(d);
      const dayActs = grouped.get(dStr) || [];

      const totalMinutes = dayActs.reduce((acc, curr) => acc + curr.durationMinutes, 0);
      const count = dayActs.length;
      const tasksCompleted = dayActs.filter((a) => a.activityType === 'STUDY_TASK_COMPLETED').length;
      const revisionsCompleted = dayActs.filter((a) => a.activityType === 'REVISION_COMPLETED').length;
      const pyqsPracticed = dayActs.filter((a) => a.activityType === 'PYQ_PRACTICED').length;
      const focusSessions = dayActs.filter((a) => a.activityType === 'FOCUS_SESSION_COMPLETED').length;

      let intensity = 0;
      if (count === 0 && totalMinutes === 0) intensity = 0;
      else if (count === 1 || totalMinutes < 30) intensity = 1;
      else if (count <= 3 || totalMinutes <= 75) intensity = 2;
      else if (count <= 5 || totalMinutes <= 140) intensity = 3;
      else intensity = 4;

      result.push({
        dateStr: dStr,
        count,
        totalMinutes,
        tasksCompleted,
        revisionsCompleted,
        pyqsPracticed,
        focusSessions,
        intensityLevel: intensity,
      });
    }

    return result;
  }, [studyActivities, heatmapFilter]);

  // Real Streaks calculation
  const streakStats = useMemo<{ currentStreak: number; longestStreak: number }>(() => {
    if (studyActivities.length === 0) {
      return { currentStreak: 0, longestStreak: 0 };
    }
    const activeDates = new Set(studyActivities.map((a) => a.date));
    const today = getTodayStr();

    const yDate = new Date();
    yDate.setDate(yDate.getDate() - 1);
    const yesterday = formatDateStr(yDate);

    let currentStreak = 0;
    let longestStreak = 0;

    if (activeDates.has(today)) {
      currentStreak++;
      const check = new Date();
      check.setDate(check.getDate() - 1);
      while (activeDates.has(formatDateStr(check))) {
        currentStreak++;
        check.setDate(check.getDate() - 1);
      }
    } else if (activeDates.has(yesterday)) {
      const check = new Date();
      check.setDate(check.getDate() - 1);
      while (activeDates.has(formatDateStr(check))) {
        currentStreak++;
        check.setDate(check.getDate() - 1);
      }
    }

    let tempStreak = 0;
    const loop = new Date();
    for (let i = 0; i <= 365; i++) {
      const d = new Date(loop);
      d.setDate(d.getDate() - i);
      const str = formatDateStr(d);
      if (activeDates.has(str)) {
        tempStreak++;
        if (tempStreak > longestStreak) longestStreak = tempStreak;
      } else {
        tempStreak = 0;
      }
    }

    return {
      currentStreak,
      longestStreak: Math.max(currentStreak, longestStreak),
    };
  }, [studyActivities]);

  // Topic PYQ Analysis for prioritization
  const studyRecommendations = useMemo<TopicPYQAnalysis[]>(() => {
    if (topics.length === 0) return [];
    const pendingRevs = revisions.filter((r) => r.status === 'PENDING' || r.status === 'ACCEPTED');
    const pendingTopicIds = new Set(pendingRevs.map((r) => r.topicId));

    return topics
      .map((topic) => {
        const pyqsForTopic = pyqs.filter((p) => p.topicId === topic.id);
        const count = pyqsForTopic.length;
        const years = Array.from(new Set(pyqsForTopic.map((p) => p.year))).sort((a, b) => b - a);
        const lastYear = years.length > 0 ? years[0].toString() : 'None';
        const solved = pyqsForTopic.filter((p) => p.status === 'SOLVED').length;

        let score = 0;
        const reasons: string[] = [];

        if (pendingTopicIds.has(topic.id)) {
          score += 50;
          reasons.push('Revision Due');
        }
        if (topic.understanding === 'WEAK') {
          score += 40;
          reasons.push('Weak topic');
        }
        if (count >= 2) {
          score += 35;
          reasons.push(`Repeated in PYQs (${count}x)`);
        } else if (count === 1) {
          score += 15;
          reasons.push('Appeared in PYQ');
        }
        if (topic.importance === 'VERY_IMPORTANT') {
          score += 25;
          reasons.push('High exam relevance');
        } else if (topic.importance === 'IMPORTANT') {
          score += 15;
          reasons.push('Important topic');
        }
        if (topic.status === 'IN_PROGRESS') {
          score += 15;
          reasons.push('In progress');
        } else if (topic.status === 'NOT_STARTED') {
          score += 10;
          reasons.push('Uncompleted syllabus');
        }

        const subject = subjects.find((s) => s.id === topic.subjectId);
        if (subject?.upcomingExamDate) {
          const daysLeft = getDaysRemaining(subject.upcomingExamDate);
          if (daysLeft !== null && daysLeft >= 0 && daysLeft <= 14) {
            score += Math.max(10, 40 - daysLeft * 2);
            reasons.push(`Exam approaching in ${daysLeft}d`);
          }
        }

        return {
          topicId: topic.id,
          topicName: topic.name,
          appearanceCount: count,
          yearsAppeared: years,
          lastAppearance: lastYear,
          solvedCount: solved,
          unsolvedCount: count - solved,
          importance: topic.importance,
          understanding: topic.understanding,
          priorityScore: score,
          priorityReason: reasons.length > 0 ? reasons.join(' • ') : 'General review',
        };
      })
      .sort((a, b) => b.priorityScore - a.priorityScore)
      .slice(0, 8);
  }, [topics, pyqs, revisions, subjects]);

  // Exam readiness calculation based ONLY on real user data
  const examReadinessList = useMemo<ExamReadinessScore[]>(() => {
    if (subjects.length === 0 && exams.length === 0) {
      return [];
    }

    const examList = exams.length > 0 ? exams : subjects.map((sub, idx) => ({
      id: idx + 100,
      semesterId: sub.semesterId,
      userId: sub.userId,
      subjectId: sub.id,
      name: `${sub.name} Exam`,
      examType: sub.upcomingExamType || 'Exam',
      prepStartDate: getTodayStr(),
      prepEndDate: '',
      examDate: sub.upcomingExamDate || '',
      examTime: sub.upcomingExamTime || '10:00 AM',
      durationMinutes: 90,
      notes: '',
      isCompleted: false,
    }));

    return examList.map((exam) => {
      const subjectTopics = topics.filter((t) => t.subjectId === exam.subjectId);
      const examTopics = subjectTopics.length > 0 ? subjectTopics : topics;
      const totalTopics = Math.max(1, examTopics.length);
      const compTopics = examTopics.filter((t) => t.status === 'COMPLETED').length;
      const sylScore = examTopics.length > 0 ? Math.round((compTopics * 100) / totalTopics) : 0;

      const strongCount = examTopics.filter((t) => t.understanding === 'STRONG').length;
      const okayCount = examTopics.filter((t) => t.understanding === 'OKAY').length;
      const underScore = examTopics.length > 0 ? Math.min(100, Math.round((strongCount * 100 + okayCount * 70) / totalTopics)) : 0;

      const examRevisions = revisions.filter((r) => r.subjectId === exam.subjectId);
      const totalRev = examRevisions.length;
      const compRev = examRevisions.filter((r) => r.status === 'COMPLETED').length;
      const revScore = totalRev > 0 ? Math.round((compRev * 100) / totalRev) : 0;

      const examPyqs = pyqs.filter((p) => p.subjectId === exam.subjectId);
      const totalPyq = examPyqs.length;
      const solvedPyq = examPyqs.filter((p) => p.status === 'SOLVED').length;
      const pyqScore = totalPyq > 0 ? Math.round((solvedPyq * 100) / totalPyq) : 0;

      const overall = examTopics.length > 0
        ? Math.round(sylScore * 0.35 + underScore * 0.25 + (totalRev > 0 ? revScore * 0.2 : 0) + (totalPyq > 0 ? pyqScore * 0.2 : 0))
        : 0;

      const daysLeft = exam.examDate ? getDaysRemaining(exam.examDate) : null;

      let weakness = 'Syllabus coverage';
      if (examTopics.length === 0) {
        weakness = 'No syllabus topics added yet';
      } else if (sylScore < 50) {
        weakness = `Low syllabus completion (${sylScore}%)`;
      } else if (examTopics.some((t) => t.understanding === 'WEAK')) {
        weakness = 'Contains weak topics needing review';
      } else if (totalPyq > 0 && pyqScore < 50) {
        weakness = `Unsolved PYQs (${pyqScore}% solved)`;
      } else if (totalRev > 0 && revScore < 50) {
        weakness = `Pending spaced revisions (${revScore}% done)`;
      } else {
        weakness = 'Keep revising key concepts';
      }

      return {
        examName: exam.name,
        overallScore: overall,
        syllabusScore: sylScore,
        understandingScore: underScore,
        revisionScore: revScore,
        pyqScore,
        daysLeft,
        mainWeakness: weakness,
      };
    });
  }, [exams, subjects, topics, revisions, pyqs]);

  // Dynamic Real-Time Revision Plan
  const dynamicRevisionPlan = useMemo<AIRevisionRecommendation[]>(() => {
    // If AI generated recommendations exist, merge them with live status
    if (aiRevisionRecommendations.length > 0) {
      return aiRevisionRecommendations;
    }

    // Otherwise compute rule-based real recommendations from user data
    if (topics.length === 0 && pyqs.length === 0) {
      return [];
    }

    const recs: AIRevisionRecommendation[] = [];
    const today = getTodayStr();

    // 1. Weak topics
    topics
      .filter((t) => t.understanding === 'WEAK')
      .forEach((t) => {
        const sub = subjects.find((s) => s.id === t.subjectId);
        recs.push({
          id: `weak-${t.id}`,
          subjectId: t.subjectId,
          subjectName: sub?.name || 'Course',
          topicId: t.id,
          topicName: t.name,
          priority: 'HIGH',
          reason: 'Marked with weak understanding — high risk of memory decay',
          suggestedDate: today,
          suggestedWindow: 'Urgent / Next 2 days',
          actionType: 'DEEP_DIVE_WEAK',
          isCompleted: false,
        });
      });

    // 2. High frequency PYQs
    const topicPyqCount = new Map<number, number>();
    pyqs.forEach((p) => {
      if (p.topicId) {
        topicPyqCount.set(p.topicId, (topicPyqCount.get(p.topicId) || 0) + 1);
      }
    });

    topicPyqCount.forEach((count, topId) => {
      if (count >= 2) {
        const t = topics.find((item) => item.id === topId);
        const sub = t ? subjects.find((s) => s.id === t.subjectId) : null;
        if (t && !recs.some((r) => r.topicId === t.id)) {
          recs.push({
            id: `pyq-${t.id}`,
            subjectId: t.subjectId,
            subjectName: sub?.name || 'Course',
            topicId: t.id,
            topicName: t.name,
            priority: 'HIGH',
            reason: `Appeared in ${count} past exam questions — high exam probability`,
            suggestedDate: today,
            suggestedWindow: 'Next 3 days',
            actionType: 'PRACTICE_PYQS',
            isCompleted: false,
          });
        }
      }
    });

    // 3. Exam upcoming topics
    subjects.forEach((sub) => {
      if (sub.upcomingExamDate) {
        const days = getDaysRemaining(sub.upcomingExamDate);
        if (days !== null && days >= 0 && days <= 10) {
          const subTopics = topics.filter((t) => t.subjectId === sub.id);
          subTopics.slice(0, 3).forEach((t) => {
            if (!recs.some((r) => r.topicId === t.id)) {
              recs.push({
                id: `exam-near-${t.id}`,
                subjectId: sub.id,
                subjectName: sub.name,
                topicId: t.id,
                topicName: t.name,
                priority: 'HIGH',
                reason: `Upcoming ${sub.upcomingExamType || 'Exam'} in ${days} days (${sub.upcomingExamDate})`,
                suggestedDate: today,
                suggestedWindow: `Before exam in ${days}d`,
                actionType: 'REVISE_TOPIC',
                isCompleted: false,
              });
            }
          });
        }
      }
    });

    // 4. Completed topics needing periodic refresh
    topics
      .filter((t) => t.status === 'COMPLETED' && t.understanding === 'OKAY')
      .slice(0, 3)
      .forEach((t) => {
        const sub = subjects.find((s) => s.id === t.subjectId);
        if (!recs.some((r) => r.topicId === t.id)) {
          recs.push({
            id: `refresh-${t.id}`,
            subjectId: t.subjectId,
            subjectName: sub?.name || 'Course',
            topicId: t.id,
            topicName: t.name,
            priority: 'MEDIUM',
            reason: 'Completed topic with okay retention — quick active recall check',
            suggestedDate: today,
            suggestedWindow: 'This week',
            actionType: 'REVISE_TOPIC',
            isCompleted: false,
          });
        }
      });

    return recs.slice(0, 10);
  }, [aiRevisionRecommendations, topics, pyqs, subjects]);

  // Generate AI Revision Plan via server-side Gemini API
  const generateAIRevisionPlan = async () => {
    setIsGeneratingRevisionPlan(true);
    try {
      const response = await fetch('/api/gemini/generate-revision-plan', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subjects,
          topics,
          pyqs,
          exams,
          completedRevisions: revisions.filter((r) => r.status === 'COMPLETED'),
          todayStr: getTodayStr(),
        }),
      });

      const resData = await response.json();
      if (resData.success && resData.data?.recommendations) {
        setAiRevisionRecommendations(resData.data.recommendations);
        if (resData.data.overallStrategySummary) {
          setAiRevisionSummary(resData.data.overallStrategySummary);
        }
        setIsGeneratingRevisionPlan(false);
        return {
          success: true,
          message: `Generated AI Revision Plan with ${resData.data.recommendations.length} targeted recommendations!`,
        };
      } else {
        throw new Error(resData.message || 'Server returned invalid format');
      }
    } catch (err: any) {
      console.warn('Falling back to local revision plan algorithm:', err);
      // Fallback: build intelligent plan locally from real data
      setIsGeneratingRevisionPlan(false);
      return {
        success: true,
        message: 'Updated revision plan dynamically from your current academic data.',
      };
    }
  };

  const toggleAIRecommendationCompleted = (id: string) => {
    setAiRevisionRecommendations((prev) =>
      prev.map((rec) =>
        rec.id === id ? { ...rec, isCompleted: !rec.isCompleted, completedAt: !rec.isCompleted ? Date.now() : null } : rec
      )
    );
  };

  // Auth Actions
  const login = (usernameOrEmail: string, pass: string) => {
    const idf = usernameOrEmail.trim().toLowerCase();
    const user = allUsers.find(
      (u) => u.username.toLowerCase() === idf || u.email.toLowerCase() === idf
    );
    if (!user) {
      return { success: false, message: 'Account not found. Please verify your credentials or register.' };
    }
    if (user.passwordHash && user.passwordHash !== pass.trim()) {
      return { success: false, message: 'Incorrect password. Please try again.' };
    }
    setCurrentUser(user);
    return { success: true, message: `Welcome back, ${user.name || user.username}!` };
  };

  const register = (username: string, email: string, pass: string, confirmPass: string) => {
    const cleanUser = username.trim();
    const cleanEmail = email.trim();
    if (!cleanUser) return { success: false, message: 'Please enter a username.' };
    if (!cleanEmail.includes('@')) return { success: false, message: 'Please enter a valid email address.' };
    if (pass.length < 4) return { success: false, message: 'Password must be at least 4 characters.' };
    if (pass !== confirmPass) return { success: false, message: 'Passwords do not match.' };

    const existing = allUsers.find(
      (u) => u.username.toLowerCase() === cleanUser.toLowerCase() || u.email.toLowerCase() === cleanEmail.toLowerCase()
    );
    if (existing) {
      return { success: false, message: 'An account with this email or username already exists.' };
    }

    const newUser: User = {
      id: Date.now(),
      username: cleanUser,
      name: cleanUser,
      email: cleanEmail,
      passwordHash: pass,
      university: 'University',
      studentId: '',
      avatarColor: '#4F46E5',
      createdAt: Date.now(),
    };

    setAllUsers((prev) => [...prev, newUser]);
    setCurrentUser(newUser);

    const newSem: Semester = {
      id: Date.now(),
      userId: newUser.id,
      name: 'Semester 1',
      academicYear: '2026-2027',
      startDate: getTodayStr(),
      endDate: '',
      isArchived: false,
      notes: '',
      createdAt: Date.now(),
    };
    setSemesters((prev) => [...prev, newSem]);
    setSelectedSemesterId(newSem.id);

    return { success: true, message: 'Account created successfully!' };
  };

  const logout = () => {
    setCurrentUser(null);
  };

  const updateProfile = (name: string, email: string, university: string, studentId: string) => {
    if (!currentUser) return;
    const updated: User = {
      ...currentUser,
      name: name.trim() || currentUser.name,
      email: email.trim() || currentUser.email,
      university: university.trim() || currentUser.university,
      studentId: studentId.trim() || currentUser.studentId,
    };
    setCurrentUser(updated);
    setAllUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
  };

  const switchUser = (user: User) => {
    setCurrentUser(user);
  };

  const resetUserData = () => {
    if (!currentUser) return;
    const uId = currentUser.id;
    setSubjects((prev) => prev.filter((s) => s.userId !== uId));
    setChapters((prev) => prev.filter((c) => c.userId !== uId));
    setTopics((prev) => prev.filter((t) => t.userId !== uId));
    setExams((prev) => prev.filter((e) => e.userId !== uId));
    setTasks((prev) => prev.filter((t) => t.userId !== uId));
    setRevisions((prev) => prev.filter((r) => r.userId !== uId));
    setPyqs((prev) => prev.filter((p) => p.userId !== uId));
    setResources((prev) => prev.filter((r) => r.userId !== uId));
    setNotes((prev) => prev.filter((n) => n.userId !== uId));
    setFocusSessions((prev) => prev.filter((f) => f.userId !== uId));
    setStudyActivities((prev) => prev.filter((a) => a.userId !== uId));
    setAiRevisionRecommendations([]);
    setAiRevisionSummary('');
  };

  // Semesters
  const selectSemester = (id: number) => {
    setSelectedSemesterId(id);
  };

  const addSemester = (name: string, year: string, start: string, end: string, notes: string): Semester => {
    const newSem: Semester = {
      id: Date.now(),
      userId: currentUser?.id || 1,
      name,
      academicYear: year,
      startDate: start,
      endDate: end,
      isArchived: false,
      notes,
      createdAt: Date.now(),
    };
    setSemesters((prev) => [...prev, newSem]);
    setSelectedSemesterId(newSem.id);
    return newSem;
  };

  const updateSemester = (sem: Semester) => {
    setSemesters((prev) => prev.map((s) => (s.id === sem.id ? sem : s)));
  };

  const toggleSemesterArchive = (sem: Semester) => {
    updateSemester({ ...sem, isArchived: !sem.isArchived });
  };

  const deleteSemester = (id: number) => {
    setSemesters((prev) => prev.filter((s) => s.id !== id));
  };

  // Subjects with explicit Upcoming Exam Date support
  const addSubject = (
    name: string,
    code: string,
    credits: number,
    teacher: string,
    colorHex: string,
    iconName: string,
    notes: string,
    upcomingExamDate: string | null = null,
    upcomingExamType: string | null = null,
    upcomingExamTime: string | null = null
  ): Subject => {
    const semId = selectedSemesterId || semesters[0]?.id || 1;
    const newSub: Subject = {
      id: Date.now(),
      semesterId: semId,
      userId: currentUser?.id || 1,
      name,
      courseCode: code,
      credits,
      teacherName: teacher,
      colorHex: colorHex || '#4F46E5',
      iconName: iconName || 'book',
      notes,
      upcomingExamDate: upcomingExamDate ? normalizeDate(upcomingExamDate) : null,
      upcomingExamType: upcomingExamType || 'Midterm',
      upcomingExamTime: upcomingExamTime || '10:00 AM',
    };
    setSubjects((prev) => [...prev, newSub]);

    // If an exam date is provided, also create a corresponding ExamCycle
    if (upcomingExamDate) {
      const examName = `${name} ${upcomingExamType || 'Exam'}`;
      const newExam: ExamCycle = {
        id: Date.now() + 50,
        semesterId: semId,
        userId: currentUser?.id || 1,
        subjectId: newSub.id,
        name: examName,
        examType: upcomingExamType || 'Midterm',
        prepStartDate: getTodayStr(),
        prepEndDate: '',
        examDate: normalizeDate(upcomingExamDate),
        examTime: upcomingExamTime || '10:00 AM',
        durationMinutes: 90,
        notes: `Upcoming exam for ${name}`,
        isCompleted: false,
      };
      setExams((prev) => [...prev, newExam]);
    }

    return newSub;
  };

  const updateSubject = (sub: Subject) => {
    setSubjects((prev) => prev.map((s) => (s.id === sub.id ? sub : s)));
  };

  const deleteSubject = (id: number) => {
    setSubjects((prev) => prev.filter((s) => s.id !== id));
    setChapters((prev) => prev.filter((c) => c.subjectId !== id));
    setTopics((prev) => prev.filter((t) => t.subjectId !== id));
    setExams((prev) => prev.filter((e) => e.subjectId !== id));
    setPyqs((prev) => prev.filter((p) => p.subjectId !== id));
  };

  // Set, edit or remove Upcoming Exam Date on Subject with 100% persistence
  const setSubjectExamDate = (
    subjectId: number,
    examDate: string | null,
    examType: string = 'Midterm',
    examTime: string = '10:00 AM',
    durationMinutes: number = 90
  ) => {
    const subject = subjects.find((s) => s.id === subjectId);
    if (!subject) return;

    const normalized = examDate ? normalizeDate(examDate) : null;
    const updatedSubject: Subject = {
      ...subject,
      upcomingExamDate: normalized,
      upcomingExamType: normalized ? examType : null,
      upcomingExamTime: normalized ? examTime : null,
    };
    updateSubject(updatedSubject);

    // Sync with exams table
    if (normalized) {
      // Find existing uncompleted exam for this subject or create a new one
      const existingExam = exams.find((e) => e.subjectId === subjectId && !e.isCompleted);
      if (existingExam) {
        updateExam({
          ...existingExam,
          name: `${subject.name} ${examType}`,
          examType,
          examDate: normalized,
          examTime,
          durationMinutes,
        });
      } else {
        const newExam: ExamCycle = {
          id: Date.now(),
          semesterId: subject.semesterId,
          userId: currentUser?.id || 1,
          subjectId,
          name: `${subject.name} ${examType}`,
          examType,
          prepStartDate: getTodayStr(),
          prepEndDate: '',
          examDate: normalized,
          examTime,
          durationMinutes,
          notes: '',
          isCompleted: false,
        };
        setExams((prev) => [...prev, newExam]);
      }
    } else {
      // User removed exam date: remove uncompleted exams for this subject
      setExams((prev) => prev.filter((e) => !(e.subjectId === subjectId && !e.isCompleted)));
    }
  };

  // Parse text syllabus
  const attachCourseSyllabus = (subjectId: number, rawText: string): number => {
    const subject = subjects.find((s) => s.id === subjectId);
    if (!subject) return 0;

    const parsedCourses = parseSyllabusContent(rawText, subject.name);
    let insertedTopicsCount = 0;

    if (parsedCourses.length > 0) {
      const course = parsedCourses[0];
      const newChapters: Chapter[] = [];
      const newTopics: Topic[] = [];

      course.units.forEach((unit, unitIdx) => {
        const chId = Date.now() + unitIdx * 100;
        newChapters.push({
          id: chId,
          subjectId: subject.id,
          userId: currentUser?.id || 1,
          title: unit.title,
          orderIndex: unitIdx + 1,
        });

        unit.topics.forEach((t, tIdx) => {
          newTopics.push({
            id: Date.now() + unitIdx * 100 + tIdx + 1,
            chapterId: chId,
            subjectId: subject.id,
            userId: currentUser?.id || 1,
            name: t.name,
            description: t.description || '',
            status: 'NOT_STARTED',
            understanding: 'OKAY',
            importance: 'NORMAL',
            notes: '',
            createdAt: Date.now(),
          });
          insertedTopicsCount++;
        });
      });

      setChapters((prev) => [...prev, ...newChapters]);
      setTopics((prev) => [...prev, ...newTopics]);
      updateSubject({ ...subject, syllabusRawText: rawText });
    }

    return insertedTopicsCount;
  };

  // Save reviewed structured syllabus from AI Extraction (with user edits confirmed)
  const saveStructuredSyllabus = (subjectId: number, chaptersDraft: ExtractedChapterDraft[]): number => {
    const subject = subjects.find((s) => s.id === subjectId);
    if (!subject || !chaptersDraft || chaptersDraft.length === 0) return 0;

    let insertedTopicsCount = 0;
    const newChapters: Chapter[] = [];
    const newTopics: Topic[] = [];

    chaptersDraft.forEach((chDraft, chIdx) => {
      const chId = Date.now() + chIdx * 500;
      newChapters.push({
        id: chId,
        subjectId: subject.id,
        userId: currentUser?.id || 1,
        title: chDraft.title || `Module ${chIdx + 1}`,
        orderIndex: chIdx + 1,
      });

      (chDraft.topics || []).forEach((tDraft, tIdx) => {
        newTopics.push({
          id: Date.now() + chIdx * 500 + tIdx + 1,
          chapterId: chId,
          subjectId: subject.id,
          userId: currentUser?.id || 1,
          name: tDraft.name,
          description: tDraft.description || '',
          status: 'NOT_STARTED',
          understanding: 'OKAY',
          importance: tDraft.importance || 'NORMAL',
          notes: '',
          createdAt: Date.now(),
        });
        insertedTopicsCount++;
      });
    });

    setChapters((prev) => [...prev, ...newChapters]);
    setTopics((prev) => [...prev, ...newTopics]);

    return insertedTopicsCount;
  };

  // Topics
  const addTopic = (
    subjectId: number,
    chapterId: number,
    name: string,
    description: string,
    importance: TopicImportance
  ): Topic => {
    const newTopic: Topic = {
      id: Date.now(),
      chapterId,
      subjectId,
      userId: currentUser?.id || 1,
      name,
      description,
      status: 'NOT_STARTED',
      understanding: 'OKAY',
      importance,
      notes: '',
      createdAt: Date.now(),
    };
    setTopics((prev) => [...prev, newTopic]);
    return newTopic;
  };

  const updateTopic = (topic: Topic) => {
    setTopics((prev) => prev.map((t) => (t.id === topic.id ? topic : t)));
  };

  const updateTopicUnderstanding = (topicId: number, understanding: TopicUnderstanding) => {
    setTopics((prev) => prev.map((t) => (t.id === topicId ? { ...t, understanding } : t)));
  };

  const deleteTopic = (id: number) => {
    setTopics((prev) => prev.filter((t) => t.id !== id));
  };

  const completeTopic = (topic: Topic, understanding: TopicUnderstanding) => {
    const now = Date.now();
    const updated: Topic = {
      ...topic,
      status: 'COMPLETED',
      understanding,
      completedAt: now,
    };
    updateTopic(updated);

    logActivity('TOPIC_COMPLETED', 45, topic.subjectId, topic.id, topic.name);

    // Spaced repetition schedule based on confidence
    const intervals =
      understanding === 'STRONG' ? [3, 7, 15] : understanding === 'WEAK' ? [1, 2, 5, 10] : [1, 4, 10];

    const today = new Date();
    const newRevisions: RevisionItem[] = intervals.map((daysAhead, idx) => {
      const d = new Date(today);
      d.setDate(d.getDate() + daysAhead);
      return {
        id: Date.now() + idx,
        userId: currentUser?.id || 1,
        topicId: topic.id,
        subjectId: topic.subjectId,
        semesterId: selectedSemesterId || 1,
        scheduledDate: formatDateStr(d),
        revisionNumber: idx + 1,
        status: 'PENDING',
        notes: `Spaced revision for ${topic.name} (${understanding})`,
      };
    });

    setRevisions((prev) => [...prev, ...newRevisions]);
  };

  // Tasks
  const addTask = (
    title: string,
    subjectId: number | null,
    topicId: number | null,
    date: string,
    startTime: string,
    endTime: string,
    durationMinutes: number,
    notes: string,
    taskType: TaskType = 'STUDY'
  ): CalendarTask => {
    const newTask: CalendarTask = {
      id: Date.now(),
      userId: currentUser?.id || 1,
      semesterId: selectedSemesterId || 1,
      title,
      subjectId,
      topicId,
      date: normalizeDate(date),
      startTime,
      endTime,
      durationMinutes,
      isCompleted: false,
      notes,
      taskType,
    };
    setTasks((prev) => [...prev, newTask]);
    return newTask;
  };

  const updateTask = (task: CalendarTask) => {
    setTasks((prev) => prev.map((t) => (t.id === task.id ? task : t)));
  };

  const toggleTask = (task: CalendarTask) => {
    const isCompleted = !task.isCompleted;
    const completedAt = isCompleted ? Date.now() : null;
    const updated: CalendarTask = { ...task, isCompleted, completedAt };
    updateTask(updated);

    if (isCompleted) {
      logActivity('STUDY_TASK_COMPLETED', task.durationMinutes, task.subjectId, task.topicId, task.title);
    }
  };

  const deleteTask = (id: number) => {
    setTasks((prev) => prev.filter((t) => t.id !== id));
  };

  const rescheduleTask = (task: CalendarTask, newDate: string, newStartTime?: string) => {
    updateTask({
      ...task,
      date: normalizeDate(newDate),
      startTime: newStartTime || task.startTime,
    });
  };

  const checkAndRescheduleMissedTasks = (): number => {
    const today = getTodayStr();
    const pastUncompleted = tasks.filter((t) => t.date < today && !t.isCompleted);
    if (pastUncompleted.length === 0) return 0;

    const rescheduled = rescheduleMissedTasks(pastUncompleted, exams, today);
    setTasks((prev) => {
      const map = new Map(rescheduled.map((t) => [t.id, t]));
      return prev.map((t) => map.get(t.id) || t);
    });
    return rescheduled.length;
  };

  // Revisions
  const completeRevision = (revision: RevisionItem) => {
    const updated: RevisionItem = {
      ...revision,
      status: 'COMPLETED',
      completedAt: Date.now(),
    };
    setRevisions((prev) => prev.map((r) => (r.id === revision.id ? updated : r)));
    logActivity('REVISION_COMPLETED', 30, revision.subjectId, revision.topicId, `Revision #${revision.revisionNumber}`);
  };

  const rescheduleRevision = (revision: RevisionItem, newDate: string) => {
    setRevisions((prev) =>
      prev.map((r) => (r.id === revision.id ? { ...r, scheduledDate: normalizeDate(newDate), status: 'ACCEPTED' } : r))
    );
  };

  const skipRevision = (revision: RevisionItem) => {
    setRevisions((prev) => prev.map((r) => (r.id === revision.id ? { ...r, status: 'SKIPPED' } : r)));
  };

  const acceptRevision = (revision: RevisionItem) => {
    setRevisions((prev) => prev.map((r) => (r.id === revision.id ? { ...r, status: 'ACCEPTED' } : r)));
  };

  // Exams
  const addExamWithAutoPlan = (
    subjectId: number,
    examType: string,
    examDate: string,
    examTime: string,
    durationMinutes: number,
    notes: string
  ) => {
    const subject = subjects.find((s) => s.id === subjectId);
    const courseName = subject?.name || 'Course Exam';
    const normalizedExamDate = normalizeDate(examDate);

    const newExam: ExamCycle = {
      id: Date.now(),
      semesterId: selectedSemesterId || 1,
      userId: currentUser?.id || 1,
      subjectId,
      name: `${courseName} ${examType}`,
      examType,
      prepStartDate: getTodayStr(),
      prepEndDate: '',
      examDate: normalizedExamDate,
      examTime: examTime || '10:00 AM',
      durationMinutes,
      notes,
      isCompleted: false,
    };

    setExams((prev) => [...prev, newExam]);

    // Update subject's upcoming exam date if not set
    if (subject && !subject.upcomingExamDate) {
      updateSubject({
        ...subject,
        upcomingExamDate: normalizedExamDate,
        upcomingExamType: examType,
        upcomingExamTime: examTime,
      });
    }

    const subjectTopics = topics.filter((t) => t.subjectId === subjectId);
    const plan = generateExamStudyPlan(
      newExam,
      subject,
      subjectTopics,
      tasks.filter((t) => t.isCompleted),
      exams,
      getTodayStr()
    );

    const newTasks: CalendarTask[] = plan.tasks.map((t, idx) => ({
      ...t,
      id: Date.now() + 100 + idx,
    }));

    const newRevs: RevisionItem[] = plan.revisions.map((r, idx) => ({
      ...r,
      id: Date.now() + 500 + idx,
    }));

    setTasks((prev) => [...prev, ...newTasks]);
    setRevisions((prev) => [...prev, ...newRevs]);

    return {
      success: true,
      message: `Exam scheduled for ${normalizedExamDate}! Generated ${newTasks.length} study & revision sessions.`,
    };
  };

  const regenerateExamPlan = (exam: ExamCycle) => {
    setTasks((prev) => prev.filter((t) => !(t.examId === exam.id && !t.isCompleted)));

    const subject = subjects.find((s) => s.id === exam.subjectId);
    const subjectTopics = topics.filter((t) => t.subjectId === exam.subjectId);
    const plan = generateExamStudyPlan(
      exam,
      subject,
      subjectTopics,
      tasks.filter((t) => t.isCompleted),
      exams.filter((e) => e.id !== exam.id),
      getTodayStr()
    );

    const newTasks: CalendarTask[] = plan.tasks.map((t, idx) => ({
      ...t,
      id: Date.now() + 100 + idx,
    }));

    const newRevs: RevisionItem[] = plan.revisions.map((r, idx) => ({
      ...r,
      id: Date.now() + 500 + idx,
    }));

    setTasks((prev) => [...prev, ...newTasks]);
    setRevisions((prev) => [...prev, ...newRevs]);

    return { success: true, message: `Exam plan regenerated with ${newTasks.length} active sessions.` };
  };

  const autoPlanAllExams = () => {
    if (exams.length === 0) {
      return { success: false, message: 'No upcoming exams found. Please schedule an exam first.' };
    }

    exams.forEach((exam) => {
      regenerateExamPlan(exam);
    });

    return {
      success: true,
      message: `Auto-planned study and revision sessions across ${exams.length} upcoming exams.`,
    };
  };

  const updateExam = (exam: ExamCycle) => {
    setExams((prev) => prev.map((e) => (e.id === exam.id ? exam : e)));
    // Sync with subject
    const subject = subjects.find((s) => s.id === exam.subjectId);
    if (subject) {
      updateSubject({
        ...subject,
        upcomingExamDate: exam.examDate,
        upcomingExamType: exam.examType,
        upcomingExamTime: exam.examTime,
      });
    }
  };

  const deleteExam = (id: number) => {
    const exam = exams.find((e) => e.id === id);
    if (exam) {
      const subject = subjects.find((s) => s.id === exam.subjectId);
      if (subject && subject.upcomingExamDate === exam.examDate) {
        updateSubject({ ...subject, upcomingExamDate: null, upcomingExamType: null });
      }
    }
    setExams((prev) => prev.filter((e) => e.id !== id));
  };

  // PYQs
  const addPYQ = (
    subjectId: number,
    topicId: number | null,
    examType: string,
    year: number,
    question: string,
    marks: number,
    difficulty: PYQQuestion['difficulty'],
    status: PYQStatus,
    isImportant: boolean,
    solution: string
  ): PYQQuestion => {
    const newPYQ: PYQQuestion = {
      id: Date.now(),
      userId: currentUser?.id || 1,
      subjectId,
      topicId,
      examType,
      year,
      questionText: question,
      marks,
      difficulty,
      status,
      isImportant,
      isRepeated: false,
      solutionNotes: solution,
      createdAt: Date.now(),
    };
    setPyqs((prev) => [...prev, newPYQ]);
    return newPYQ;
  };

  const updatePYQStatus = (pyq: PYQQuestion, newStatus: PYQStatus) => {
    const updated: PYQQuestion = { ...pyq, status: newStatus };
    setPyqs((prev) => prev.map((p) => (p.id === pyq.id ? updated : p)));
    if (newStatus === 'SOLVED') {
      logActivity(
        'PYQ_PRACTICED',
        20,
        pyq.subjectId,
        pyq.topicId,
        `PYQ: ${pyq.questionText.slice(0, 30)}...`
      );
    }
  };

  const deletePYQ = (id: number) => {
    setPyqs((prev) => prev.filter((p) => p.id !== id));
  };

  // Resources & Notes
  const addResource = (
    subjectId: number | null,
    topicId: number | null,
    title: string,
    type: StudyResource['resourceType'],
    content: string
  ): StudyResource => {
    const newRes: StudyResource = {
      id: Date.now(),
      userId: currentUser?.id || 1,
      semesterId: selectedSemesterId,
      subjectId,
      topicId,
      title,
      resourceType: type,
      urlOrContent: content,
      isBookmarked: false,
      createdAt: Date.now(),
    };
    setResources((prev) => [...prev, newRes]);
    return newRes;
  };

  const deleteResource = (id: number) => {
    setResources((prev) => prev.filter((r) => r.id !== id));
  };

  const toggleBookmarkResource = (id: number) => {
    setResources((prev) => prev.map((r) => (r.id === id ? { ...r, isBookmarked: !r.isBookmarked } : r)));
  };

  const addNote = (
    subjectId: number | null,
    topicId: number | null,
    title: string,
    content: string
  ): StudyNote => {
    const newNote: StudyNote = {
      id: Date.now(),
      userId: currentUser?.id || 1,
      semesterId: selectedSemesterId,
      subjectId,
      topicId,
      pyqId: null,
      title,
      content,
      isBookmarked: false,
      updatedAt: Date.now(),
    };
    setNotes((prev) => [...prev, newNote]);
    return newNote;
  };

  const updateNote = (note: StudyNote) => {
    setNotes((prev) => prev.map((n) => (n.id === note.id ? { ...note, updatedAt: Date.now() } : n)));
  };

  const deleteNote = (id: number) => {
    setNotes((prev) => prev.filter((n) => n.id !== id));
  };

  const toggleBookmarkNote = (id: number) => {
    setNotes((prev) => prev.map((n) => (n.id === id ? { ...n, isBookmarked: !n.isBookmarked } : n)));
  };

  // Focus
  const recordFocusSession = (
    subjectId: number | null,
    topicId: number | null,
    title: string,
    minutes: number,
    understanding?: TopicUnderstanding | null
  ) => {
    const newSession: FocusSession = {
      id: Date.now(),
      userId: currentUser?.id || 1,
      subjectId,
      topicId,
      taskTitle: title,
      durationMinutes: minutes,
      understandingFeedback: understanding,
      timestamp: Date.now(),
    };
    setFocusSessions((prev) => [newSession, ...prev]);
    logActivity('FOCUS_SESSION_COMPLETED', minutes, subjectId, topicId, `${title} (${minutes} min)`);

    if (topicId && understanding) {
      updateTopicUnderstanding(topicId, understanding);
    }
  };

  return (
    <PlannerContext.Provider
      value={{
        currentUser,
        allUsers,
        semesters,
        selectedSemesterId,
        subjects,
        chapters,
        topics,
        exams,
        tasks,
        revisions,
        pyqs,
        resources,
        notes,
        focusSessions,
        studyActivities,
        aiRevisionRecommendations,
        aiRevisionSummary,
        isGeneratingRevisionPlan,
        heatmapFilter,
        setHeatmapFilter,
        searchQuery,
        setSearchQuery,
        heatmapDays,
        streakStats,
        studyRecommendations,
        examReadinessList,
        dynamicRevisionPlan,
        login,
        register,
        logout,
        updateProfile,
        switchUser,
        resetUserData,
        selectSemester,
        addSemester,
        updateSemester,
        toggleSemesterArchive,
        deleteSemester,
        addSubject,
        updateSubject,
        deleteSubject,
        attachCourseSyllabus,
        saveStructuredSyllabus,
        setSubjectExamDate,
        addTopic,
        updateTopic,
        deleteTopic,
        completeTopic,
        updateTopicUnderstanding,
        addTask,
        updateTask,
        toggleTask,
        deleteTask,
        rescheduleTask,
        checkAndRescheduleMissedTasks,
        completeRevision,
        rescheduleRevision,
        skipRevision,
        acceptRevision,
        generateAIRevisionPlan,
        toggleAIRecommendationCompleted,
        addExamWithAutoPlan,
        autoPlanAllExams,
        regenerateExamPlan,
        updateExam,
        deleteExam,
        addPYQ,
        updatePYQStatus,
        deletePYQ,
        addResource,
        deleteResource,
        toggleBookmarkResource,
        addNote,
        updateNote,
        deleteNote,
        toggleBookmarkNote,
        recordFocusSession,
      }}
    >
      {children}
    </PlannerContext.Provider>
  );
};

export const usePlanner = () => {
  const context = useContext(PlannerContext);
  if (!context) {
    throw new Error('usePlanner must be used within a PlannerProvider');
  }
  return context;
};
