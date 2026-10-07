export interface User {
  id: number;
  username: string;
  name: string;
  email: string;
  passwordHash: string;
  university: string;
  studentId: string;
  avatarColor: string;
  createdAt: number;
}

export interface Semester {
  id: number;
  userId: number;
  name: string;
  academicYear: string;
  startDate: string;
  endDate: string;
  isArchived: boolean;
  notes: string;
  createdAt: number;
}

export interface Subject {
  id: number;
  semesterId: number;
  userId: number;
  name: string;
  courseCode: string;
  credits: number;
  teacherName: string;
  colorHex: string;
  iconName: string;
  notes: string;
  upcomingExamDate?: string | null;
  upcomingExamType?: string | null;
  upcomingExamTime?: string | null;
  syllabusPdfPath?: string | null;
  syllabusPdfName?: string | null;
  syllabusRawText?: string | null;
}

export interface Chapter {
  id: number;
  subjectId: number;
  userId: number;
  title: string;
  orderIndex: number;
}

export type TopicStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED' | 'NEED_REVISION';
export type TopicUnderstanding = 'STRONG' | 'OKAY' | 'WEAK';
export type TopicImportance = 'NORMAL' | 'IMPORTANT' | 'VERY_IMPORTANT';

export interface Topic {
  id: number;
  chapterId: number;
  subjectId: number;
  userId: number;
  name: string;
  description: string;
  status: TopicStatus;
  understanding: TopicUnderstanding;
  importance: TopicImportance;
  notes: string;
  completedAt?: number | null;
  createdAt: number;
}

export interface ExamCycle {
  id: number;
  semesterId: number;
  userId: number;
  subjectId: number;
  name: string;
  examType: string; // 'Midterm' | 'Quiz' | 'Final' | 'Term Test'
  prepStartDate: string;
  prepEndDate: string;
  examDate: string; // 'YYYY-MM-DD'
  examTime: string; // e.g. '10:30 AM'
  durationMinutes: number;
  syllabusPdfPath?: string | null;
  syllabusPdfName?: string | null;
  targetTopicIds?: string;
  notes: string;
  isCompleted: boolean;
}

export type TaskType = 'STUDY' | 'REVISION' | 'EXAM' | 'EXAM_PREP';

export interface CalendarTask {
  id: number;
  userId: number;
  semesterId: number;
  title: string;
  subjectId?: number | null;
  topicId?: number | null;
  pyqId?: number | null;
  examId?: number | null;
  date: string; // 'YYYY-MM-DD'
  startTime: string; // '09:00'
  endTime: string; // '10:00'
  durationMinutes: number;
  isCompleted: boolean;
  completedAt?: number | null;
  notes: string;
  taskType: TaskType;
}

export type RevisionStatus = 'PENDING' | 'ACCEPTED' | 'COMPLETED' | 'SKIPPED' | 'MISSED';

export interface RevisionItem {
  id: number;
  userId: number;
  topicId?: number | null;
  subjectId: number;
  semesterId: number;
  scheduledDate: string; // 'YYYY-MM-DD'
  revisionNumber: number;
  status: RevisionStatus;
  completedAt?: number | null;
  notes: string;
}

export type PYQDifficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type PYQStatus = 'UNSOLVED' | 'ATTEMPTED' | 'SOLVED' | 'NEED_PRACTICE';

export interface PYQQuestion {
  id: number;
  userId: number;
  subjectId: number;
  topicId?: number | null;
  examType: string;
  year: number;
  questionText: string;
  marks: number;
  difficulty: PYQDifficulty;
  status: PYQStatus;
  isImportant: boolean;
  isRepeated: boolean;
  solutionNotes: string;
  createdAt: number;
}

export type ResourceType = 'PDF' | 'SLIDES' | 'DOC' | 'LINK' | 'NOTE';

export interface StudyResource {
  id: number;
  userId: number;
  semesterId?: number | null;
  subjectId?: number | null;
  topicId?: number | null;
  title: string;
  resourceType: ResourceType;
  urlOrContent: string;
  isBookmarked: boolean;
  createdAt: number;
}

export interface StudyNote {
  id: number;
  userId: number;
  semesterId?: number | null;
  subjectId?: number | null;
  topicId?: number | null;
  pyqId?: number | null;
  title: string;
  content: string;
  isBookmarked: boolean;
  updatedAt: number;
}

export interface FocusSession {
  id: number;
  userId: number;
  subjectId?: number | null;
  topicId?: number | null;
  taskTitle: string;
  durationMinutes: number;
  understandingFeedback?: TopicUnderstanding | null;
  timestamp: number;
}

export type ActivityType =
  | 'STUDY_TASK_COMPLETED'
  | 'REVISION_COMPLETED'
  | 'FOCUS_SESSION_COMPLETED'
  | 'PYQ_PRACTICED'
  | 'TOPIC_COMPLETED';

export interface StudyActivity {
  id: number;
  userId: number;
  date: string; // 'YYYY-MM-DD'
  activityType: ActivityType;
  durationMinutes: number;
  subjectId?: number | null;
  topicId?: number | null;
  metadata: string;
  timestamp: number;
}

export interface DayActivitySummary {
  dateStr: string;
  count: number;
  totalMinutes: number;
  tasksCompleted: number;
  revisionsCompleted: number;
  pyqsPracticed: number;
  focusSessions: number;
  intensityLevel: number; // 0 to 4
}

export interface TopicPYQAnalysis {
  topicId: number;
  topicName: string;
  appearanceCount: number;
  yearsAppeared: number[];
  lastAppearance: string;
  solvedCount: number;
  unsolvedCount: number;
  importance: TopicImportance;
  understanding: TopicUnderstanding;
  priorityScore: number;
  priorityReason: string;
}

export interface ExamReadinessScore {
  examName: string;
  overallScore: number;
  syllabusScore: number;
  understandingScore: number;
  revisionScore: number;
  pyqScore: number;
  daysLeft: number | null;
  mainWeakness: string;
}

export type NavigationSection =
  | 'DASHBOARD'
  | 'CALENDAR'
  | 'SUBJECTS'
  | 'REVISION'
  | 'EXAMS'
  | 'PYQS'
  | 'PROGRESS'
  | 'RESOURCES'
  | 'SEMESTERS'
  | 'SETTINGS'
  | 'ASSISTANT';

export interface AIRevisionRecommendation {
  id: string;
  subjectId: number;
  subjectName: string;
  topicId?: number | null;
  topicName: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  reason: string;
  suggestedDate: string; // YYYY-MM-DD
  suggestedWindow: string; // e.g. "Next 2 days" or "Urgent before exam"
  actionType: 'REVISE_TOPIC' | 'PRACTICE_PYQS' | 'DEEP_DIVE_WEAK' | 'QUICK_FORMULA_CHECK';
  isCompleted: boolean;
  completedAt?: number | null;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  imageUrl?: string;
  sources?: { title: string; url: string }[];
  timestamp: number;
}

export interface ExtractedTopicDraft {
  id: string;
  name: string;
  description?: string;
  importance?: TopicImportance;
}

export interface ExtractedChapterDraft {
  id: string;
  title: string;
  topics: ExtractedTopicDraft[];
}

export interface ExtractedSyllabusDraft {
  courseName: string;
  courseCode: string;
  chapters: ExtractedChapterDraft[];
}

