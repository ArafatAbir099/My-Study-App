import React, { useEffect, useState } from 'react';
import {
  Sparkles,
  Play,
  Calendar,
  CheckCircle2,
  Clock,
  BookOpen,
  RotateCcw,
  ArrowRight,
  PlusCircle,
  AlertCircle,
  GraduationCap,
  Bot,
  HelpCircle,
  CalendarDays,
  Upload,
  Check,
  RefreshCw,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { StudyActivityHeatmapCard } from '../common/StudyActivityHeatmapCard';
import { NavigationSection, Topic } from '../../types';
import { getTodayStr, formatReadableDate, formatExamDateStatus, getDaysRemaining } from '../../util/dateUtils';

interface DashboardScreenProps {
  onNavigate: (section: NavigationSection) => void;
  onOpenWhatShouldIStudy: () => void;
  onOpenFocusSession: (topic?: Topic | null) => void;
  onOpenQuickAdd: () => void;
  onOpenSyllabusUpload?: () => void;
  onOpenAssistant?: () => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  onNavigate,
  onOpenWhatShouldIStudy,
  onOpenFocusSession,
  onOpenQuickAdd,
  onOpenSyllabusUpload,
  onOpenAssistant,
}) => {
  const {
    currentUser,
    semesters,
    subjects,
    topics,
    tasks,
    revisions,
    exams,
    toggleTask,
    completeRevision,
    checkAndRescheduleMissedTasks,
    examReadinessList,
    dynamicRevisionPlan,
    generateAIRevisionPlan,
    isGeneratingRevisionPlan,
    toggleAIRecommendationCompleted,
  } = usePlanner();

  const today = getTodayStr();
  const [planNotice, setPlanNotice] = useState<string | null>(null);

  useEffect(() => {
    checkAndRescheduleMissedTasks();
  }, []);

  const activeSemester = semesters.find((s) => !s.isArchived) || semesters[0];
  const todayTasks = tasks.filter((t) => t.date === today);
  const pendingRevisions = revisions.filter(
    (r) => r.scheduledDate <= today && r.status !== 'COMPLETED' && r.status !== 'SKIPPED'
  );
  const readiness = examReadinessList[0];

  // Subjects with exam date set or exams scheduled
  const subjectsWithExams = subjects.filter((s) => s.upcomingExamDate);

  const handleUpdatePlan = async () => {
    const res = await generateAIRevisionPlan();
    setPlanNotice(res.message);
    setTimeout(() => setPlanNotice(null), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Banner Card */}
      <div className="bg-gradient-to-r from-indigo-700 via-indigo-600 to-blue-600 rounded-3xl p-6 text-white shadow-lg relative overflow-hidden">
        <div className="absolute right-0 top-0 bottom-0 opacity-10 pointer-events-none flex items-center pr-6">
          <GraduationCap className="w-64 h-64" />
        </div>

        <div className="relative z-10">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="text-xs font-bold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-white/20 backdrop-blur-xs">
                  {activeSemester?.name || 'Semester 1'} ({activeSemester?.academicYear || '2026-2027'})
                </span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
                Welcome, {currentUser?.name?.split(' ')[0] || 'Student'}!
              </h1>
              <p className="text-sm text-indigo-100 mt-1 max-w-xl">
                Your personal university workspace for syllabus tracking, dynamic revision recommendations, and exam readiness.
              </p>
            </div>

            {/* Quick action buttons */}
            <div className="flex flex-wrap items-center gap-2">
              {onOpenAssistant && (
                <button
                  onClick={onOpenAssistant}
                  className="px-4 py-2.5 rounded-2xl bg-white/20 hover:bg-white/30 border border-white/30 text-white font-bold text-xs backdrop-blur-xs transition-all flex items-center gap-2"
                >
                  <Bot className="w-4 h-4 text-white" />
                  AI Study Assistant
                </button>
              )}
              <button
                onClick={onOpenWhatShouldIStudy}
                className="px-4 py-2.5 rounded-2xl bg-white text-indigo-700 hover:bg-indigo-50 font-bold text-xs shadow-md transition-all flex items-center gap-2"
              >
                <Sparkles className="w-4 h-4 text-indigo-600" />
                What Should I Study?
              </button>
              <button
                onClick={() => onOpenFocusSession(null)}
                className="px-4 py-2.5 rounded-2xl bg-indigo-900/60 hover:bg-indigo-900/80 border border-white/20 text-white font-bold text-xs backdrop-blur-xs transition-all flex items-center gap-2"
              >
                <Play className="w-4 h-4 fill-white" />
                Deep Focus Mode
              </button>
              <button
                onClick={onOpenQuickAdd}
                className="px-4 py-2.5 rounded-2xl bg-white/10 hover:bg-white/20 border border-white/20 text-white font-bold text-xs backdrop-blur-xs transition-all flex items-center gap-1.5"
              >
                <PlusCircle className="w-4 h-4" />
                Quick Add
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Onboarding Guide if no subjects yet */}
      {subjects.length === 0 && (
        <div className="bg-white rounded-3xl p-6 border-2 border-dashed border-indigo-200 shadow-xs space-y-4">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="text-base font-extrabold text-slate-900 flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-indigo-600" />
                Getting Started with Semester Study OS
              </h3>
              <p className="text-xs text-slate-500 mt-0.5">
                Build your clean academic workspace in a few easy steps. The AI will adapt dynamically to your courses.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-6 gap-3 pt-2 text-xs">
            <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between">
              <div>
                <span className="w-5 h-5 rounded-full bg-indigo-100 text-indigo-700 font-extrabold flex items-center justify-center text-[10px] mb-2">
                  1
                </span>
                <span className="font-bold text-slate-800 block">Active Term</span>
                <span className="text-[11px] text-slate-500">{activeSemester?.name || 'Semester 1'} configured</span>
              </div>
              <Check className="w-4 h-4 text-emerald-600 mt-2 self-end" />
            </div>

            <div className="p-3.5 rounded-2xl bg-indigo-50/50 border border-indigo-200 flex flex-col justify-between">
              <div>
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white font-extrabold flex items-center justify-center text-[10px] mb-2">
                  2
                </span>
                <span className="font-bold text-slate-900 block">Add Subjects</span>
                <span className="text-[11px] text-slate-500">Add your university courses</span>
              </div>
              <button
                onClick={() => onNavigate('SUBJECTS')}
                className="mt-2 text-[11px] font-bold text-indigo-600 hover:underline text-left"
              >
                + Add Subject &rarr;
              </button>
            </div>

            <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between">
              <div>
                <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-700 font-extrabold flex items-center justify-center text-[10px] mb-2">
                  3
                </span>
                <span className="font-bold text-slate-800 block">Upload Syllabus</span>
                <span className="text-[11px] text-slate-500">Photo / screenshot AI extraction</span>
              </div>
              {onOpenSyllabusUpload && (
                <button
                  onClick={onOpenSyllabusUpload}
                  className="mt-2 text-[11px] font-bold text-indigo-600 hover:underline text-left"
                >
                  Upload &rarr;
                </button>
              )}
            </div>

            <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between">
              <div>
                <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-700 font-extrabold flex items-center justify-center text-[10px] mb-2">
                  4
                </span>
                <span className="font-bold text-slate-800 block">Set Exam Dates</span>
                <span className="text-[11px] text-slate-500">Countdown & days remaining</span>
              </div>
              <button
                onClick={() => onNavigate('EXAMS')}
                className="mt-2 text-[11px] font-bold text-indigo-600 hover:underline text-left"
              >
                Set Date &rarr;
              </button>
            </div>

            <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between">
              <div>
                <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-700 font-extrabold flex items-center justify-center text-[10px] mb-2">
                  5
                </span>
                <span className="font-bold text-slate-800 block">Add PYQs</span>
                <span className="text-[11px] text-slate-500">Past paper questions bank</span>
              </div>
              <button
                onClick={() => onNavigate('PYQS')}
                className="mt-2 text-[11px] font-bold text-indigo-600 hover:underline text-left"
              >
                Add PYQs &rarr;
              </button>
            </div>

            <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 flex flex-col justify-between">
              <div>
                <span className="w-5 h-5 rounded-full bg-slate-200 text-slate-700 font-extrabold flex items-center justify-center text-[10px] mb-2">
                  6
                </span>
                <span className="font-bold text-slate-800 block">AI Revision</span>
                <span className="text-[11px] text-slate-500">Personalized priorities</span>
              </div>
              <button
                onClick={() => onNavigate('REVISION')}
                className="mt-2 text-[11px] font-bold text-indigo-600 hover:underline text-left"
              >
                View Plan &rarr;
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SECTION: UPCOMING EXAMS (Fixed with exact days remaining & status) */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Calendar className="w-5 h-5 text-rose-600" />
            <h3 className="font-extrabold text-base text-slate-900">Upcoming Exams</h3>
          </div>
          <button
            onClick={() => onNavigate('EXAMS')}
            className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
          >
            Manage Dates <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        {subjectsWithExams.length === 0 && exams.length === 0 ? (
          <div className="py-6 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
            <CalendarDays className="w-8 h-8 mx-auto mb-1 opacity-50 text-rose-500" />
            <p className="text-xs font-bold text-slate-700">No upcoming exam dates set yet.</p>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Set your midterm or final exam date on any subject to activate the live countdown and priority revision planner.
            </p>
            <button
              onClick={() => onNavigate('SUBJECTS')}
              className="mt-3 px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
            >
              Set Exam Date in Subjects
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {subjectsWithExams.map((sub) => {
              const statusInfo = formatExamDateStatus(sub.upcomingExamDate);
              const days = statusInfo.days;

              return (
                <div
                  key={sub.id}
                  className="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/50 flex flex-col justify-between hover:border-rose-300 transition-all"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div>
                        <span className="text-sm font-extrabold text-slate-900 block leading-tight">
                          {sub.name}
                        </span>
                        <span className="text-xs text-slate-500 font-medium">
                          {sub.upcomingExamType || 'Exam'} • {sub.courseCode || 'Course'}
                        </span>
                      </div>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full border ${statusInfo.badgeClass}`}>
                        {statusInfo.status === 'TODAY'
                          ? 'Today!'
                          : statusInfo.status === 'TOMORROW'
                          ? 'Tomorrow'
                          : days !== null && days >= 0
                          ? `${days} days left`
                          : 'Completed'}
                      </span>
                    </div>

                    <div className="text-xs text-slate-600 font-semibold mt-1">
                      📅 {sub.upcomingExamDate ? formatReadableDate(sub.upcomingExamDate) : 'Not set'}
                    </div>
                  </div>

                  <div className="mt-3 pt-2 border-t border-slate-200/60 flex items-center justify-between text-[11px]">
                    <span className="text-slate-500">
                      {statusInfo.label}
                    </span>
                    <button
                      onClick={() => onNavigate('SUBJECTS')}
                      className="font-bold text-indigo-600 hover:underline"
                    >
                      Edit Date
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* SECTION: TODAY'S RECOMMENDED REVISION (Dynamic AI Plan) */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <div className="flex items-center gap-2">
              <RotateCcw className="w-5 h-5 text-amber-600" />
              <h3 className="font-extrabold text-base text-slate-900">
                Today&apos;s Recommended Revision
              </h3>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                AI Priority Engine
              </span>
            </div>
            <p className="text-xs text-slate-500 mt-0.5">
              Personalized suggestions based on upcoming exams, weak topics, and repeated PYQs. (You control your daily routine.)
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleUpdatePlan}
              disabled={isGeneratingRevisionPlan}
              className="px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-xs font-bold text-slate-700 transition-colors flex items-center gap-1.5"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${isGeneratingRevisionPlan ? 'animate-spin' : ''}`} />
              Update Revision Plan
            </button>
            <button
              onClick={() => onNavigate('REVISION')}
              className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              All Revisions <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {planNotice && (
          <div className="p-3 bg-indigo-50 border border-indigo-200 text-indigo-800 text-xs font-bold rounded-xl">
            {planNotice}
          </div>
        )}

        {dynamicRevisionPlan.length === 0 ? (
          <div className="py-8 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
            <CheckCircle2 className="w-8 h-8 mx-auto mb-1 opacity-50 text-emerald-500" />
            <p className="text-xs font-bold text-slate-700">No revision plan yet.</p>
            <p className="text-[11px] text-slate-400 mt-0.5 max-w-sm mx-auto">
              Add your subjects, upload syllabus topics, or add PYQs to generate intelligent priority recommendations.
            </p>
            <button
              onClick={() => onNavigate('SUBJECTS')}
              className="mt-3 px-3.5 py-1.5 bg-indigo-600 text-white rounded-xl text-xs font-bold shadow-xs"
            >
              Add Subjects & Syllabus
            </button>
          </div>
        ) : (
          <div className="space-y-2.5">
            {dynamicRevisionPlan.slice(0, 5).map((rec) => {
              const isHigh = rec.priority === 'HIGH';
              const isMed = rec.priority === 'MEDIUM';

              return (
                <div
                  key={rec.id}
                  className={`p-3.5 rounded-2xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                    rec.isCompleted
                      ? 'bg-slate-50 border-slate-200 opacity-60'
                      : isHigh
                      ? 'bg-rose-50/30 border-rose-200 hover:border-rose-300'
                      : 'bg-white border-slate-200/80 hover:border-amber-300'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <button
                      onClick={() => toggleAIRecommendationCompleted(rec.id)}
                      className={`mt-0.5 w-5 h-5 rounded-md border flex items-center justify-center transition-colors ${
                        rec.isCompleted
                          ? 'bg-emerald-600 border-emerald-600 text-white'
                          : 'border-slate-300 hover:border-amber-600'
                      }`}
                      title="Mark revised"
                    >
                      {rec.isCompleted && <CheckCircle2 className="w-4 h-4" />}
                    </button>

                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={`text-sm font-bold ${
                            rec.isCompleted ? 'line-through text-slate-400' : 'text-slate-900'
                          }`}
                        >
                          {rec.topicName}
                        </span>
                        <span
                          className={`text-[10px] font-extrabold px-2 py-0.2 rounded-full ${
                            isHigh
                              ? 'bg-rose-100 text-rose-800 border border-rose-200'
                              : isMed
                              ? 'bg-amber-100 text-amber-800 border border-amber-200'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {rec.priority} PRIORITY
                        </span>
                        <span className="text-[11px] font-semibold text-indigo-700">
                          {rec.subjectName}
                        </span>
                      </div>

                      <div className="text-xs text-slate-600 mt-1 font-medium">
                        💡 <b>Reason:</b> {rec.reason}
                      </div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        Target window: {rec.suggestedWindow}
                      </div>
                    </div>
                  </div>

                  {!rec.isCompleted && (
                    <div className="flex items-center gap-2 self-end sm:self-center">
                      <button
                        onClick={() => {
                          const topic = topics.find((t) => t.id === rec.topicId);
                          onOpenFocusSession(topic || null);
                        }}
                        className="px-2.5 py-1 text-xs font-bold rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors flex items-center gap-1"
                      >
                        <Play className="w-3 h-3 fill-indigo-700" /> Focus Recall
                      </button>
                      <button
                        onClick={() => toggleAIRecommendationCompleted(rec.id)}
                        className="px-3 py-1 text-xs font-bold bg-amber-600 hover:bg-amber-700 text-white rounded-lg shadow-xs transition-colors"
                      >
                        Done
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Main Grid: Today's Tasks + Activity Heatmap */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          {/* Today's Tasks */}
          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <h2 className="font-bold text-slate-900 text-base">Today&apos;s Study Schedule</h2>
                <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700">
                  {todayTasks.filter((t) => t.isCompleted).length} / {todayTasks.length} Completed
                </span>
              </div>
              <button
                onClick={() => onNavigate('CALENDAR')}
                className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
              >
                Full Calendar <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {todayTasks.length === 0 ? (
              <div className="py-8 text-center text-slate-400">
                <Calendar className="w-8 h-8 mx-auto mb-1 opacity-50" />
                <p className="text-xs font-semibold text-slate-700">No scheduled tasks for today.</p>
                <p className="text-[11px] mt-0.5">
                  You control your own routine. Schedule a study session via Quick Add whenever ready.
                </p>
                <button
                  onClick={onOpenQuickAdd}
                  className="mt-2.5 px-3 py-1 text-xs font-bold bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded-lg"
                >
                  + Add Study Block
                </button>
              </div>
            ) : (
              <div className="space-y-2.5">
                {todayTasks.map((task) => {
                  const subject = subjects.find((s) => s.id === task.subjectId);
                  return (
                    <div
                      key={task.id}
                      className={`p-3.5 rounded-xl border transition-all flex items-start gap-3 ${
                        task.isCompleted
                          ? 'bg-slate-50 border-slate-200 opacity-60'
                          : 'bg-white border-slate-200/80 hover:border-indigo-300'
                      }`}
                    >
                      <button
                        onClick={() => toggleTask(task)}
                        className={`mt-0.5 w-5 h-5 rounded-md border flex items-center justify-center transition-colors ${
                          task.isCompleted
                            ? 'bg-emerald-600 border-emerald-600 text-white'
                            : 'border-slate-300 hover:border-indigo-600'
                        }`}
                      >
                        {task.isCompleted && <CheckCircle2 className="w-4 h-4" />}
                      </button>

                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span
                            className={`text-sm font-bold truncate ${
                              task.isCompleted ? 'line-through text-slate-500' : 'text-slate-900'
                            }`}
                          >
                            {task.title}
                          </span>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">
                            {task.taskType}
                          </span>
                        </div>

                        <div className="flex flex-wrap items-center gap-3 mt-1.5 text-xs text-slate-500">
                          {subject && (
                            <span className="font-semibold text-slate-700">
                              {subject.name}
                            </span>
                          )}
                          <span className="flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5" />
                            {task.startTime} ({task.durationMinutes}m)
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Activity Heatmap Card */}
          <StudyActivityHeatmapCard />
        </div>

        {/* Right Column: Pending Spaced Revisions Due + Overview */}
        <div className="space-y-6">
          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <RotateCcw className="w-4 h-4 text-amber-600" />
                <h3 className="font-bold text-slate-900 text-base">Spaced Revisions</h3>
              </div>
              <span className="text-xs font-extrabold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                {pendingRevisions.length} Due
              </span>
            </div>

            {pendingRevisions.length === 0 ? (
              <div className="p-6 text-center text-slate-400">
                <CheckCircle2 className="w-8 h-8 mx-auto mb-1 text-emerald-500 opacity-80" />
                <p className="text-xs font-semibold text-slate-600">All caught up on revisions!</p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  Complete syllabus topics in Subjects to trigger memory intervals.
                </p>
              </div>
            ) : (
              <div className="space-y-2.5">
                {pendingRevisions.slice(0, 4).map((rev) => {
                  const subject = subjects.find((s) => s.id === rev.subjectId);
                  const topic = topics.find((t) => t.id === rev.topicId);

                  return (
                    <div
                      key={rev.id}
                      className="p-3 rounded-xl border border-amber-200/80 bg-amber-50/40 hover:bg-amber-50 transition-all flex items-start justify-between gap-2"
                    >
                      <div>
                        <div className="text-xs font-bold text-slate-900">
                          {topic?.name || rev.notes}
                        </div>
                        <div className="text-[11px] text-slate-500 mt-0.5">
                          {subject?.name} • Revision #{rev.revisionNumber}
                        </div>
                      </div>

                      <button
                        onClick={() => completeRevision(rev)}
                        className="px-2.5 py-1 rounded-lg text-xs font-bold bg-amber-600 hover:bg-amber-700 text-white shadow-xs shrink-0"
                      >
                        Done
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
