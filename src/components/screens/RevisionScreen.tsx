import React, { useState } from 'react';
import {
  RotateCcw,
  CheckCircle2,
  Calendar,
  Clock,
  Play,
  CalendarDays,
  SkipForward,
  Info,
  Sparkles,
  RefreshCw,
  Flame,
  AlertTriangle,
  Brain,
  HelpCircle,
  Filter,
  ArrowRight,
  BookOpen,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { RevisionItem, Topic, AIRevisionRecommendation } from '../../types';
import { getTodayStr, formatReadableDate, getDaysRemaining } from '../../util/dateUtils';

interface RevisionScreenProps {
  onStartFocus: (topic: Topic) => void;
}

export const RevisionScreen: React.FC<RevisionScreenProps> = ({ onStartFocus }) => {
  const {
    revisions,
    topics,
    subjects,
    pyqs,
    exams,
    completeRevision,
    rescheduleRevision,
    skipRevision,
    dynamicRevisionPlan,
    generateAIRevisionPlan,
    isGeneratingRevisionPlan,
    toggleAIRecommendationCompleted,
    aiRevisionSummary,
  } = usePlanner();

  const [mainView, setMainView] = useState<'AI_PLAN' | 'SPACED_REPETITIONS'>('AI_PLAN');
  const [activeSpacedTab, setActiveSpacedTab] = useState<'DUE' | 'UPCOMING' | 'COMPLETED'>('DUE');
  const [priorityFilter, setPriorityFilter] = useState<'ALL' | 'HIGH' | 'MEDIUM' | 'LOW'>('ALL');
  const [subjectFilter, setSubjectFilter] = useState<number | 'ALL'>('ALL');

  const [rescheduleItem, setRescheduleItem] = useState<RevisionItem | null>(null);
  const [targetDate, setTargetDate] = useState('');
  const [updateNotice, setUpdateNotice] = useState<string | null>(null);

  const todayStr = getTodayStr();

  // Spaced repetitions lists
  const dueRevisions = revisions.filter(
    (r) => r.scheduledDate <= todayStr && r.status !== 'COMPLETED' && r.status !== 'SKIPPED'
  );
  const upcomingRevisions = revisions.filter(
    (r) => r.scheduledDate > todayStr && r.status !== 'COMPLETED' && r.status !== 'SKIPPED'
  );
  const completedRevisions = revisions.filter((r) => r.status === 'COMPLETED');

  // Filtered AI recommendations
  const filteredAIPlan = dynamicRevisionPlan.filter((rec) => {
    if (priorityFilter !== 'ALL' && rec.priority !== priorityFilter) return false;
    if (subjectFilter !== 'ALL' && rec.subjectId !== subjectFilter) return false;
    return true;
  });

  const highPriorityCount = dynamicRevisionPlan.filter((r) => r.priority === 'HIGH' && !r.isCompleted).length;
  const mediumPriorityCount = dynamicRevisionPlan.filter((r) => r.priority === 'MEDIUM' && !r.isCompleted).length;
  const completedRecCount = dynamicRevisionPlan.filter((r) => r.isCompleted).length;

  const handleTriggerGenerate = async () => {
    const res = await generateAIRevisionPlan();
    setUpdateNotice(res.message);
    setTimeout(() => setUpdateNotice(null), 3500);
  };

  const handleConfirmReschedule = () => {
    if (rescheduleItem && targetDate) {
      rescheduleRevision(rescheduleItem, targetDate);
      setRescheduleItem(null);
      setTargetDate('');
    }
  };

  const handleLaunchFocus = (rec: AIRevisionRecommendation) => {
    if (rec.topicId) {
      const targetTopic = topics.find((t) => t.id === rec.topicId);
      if (targetTopic) {
        onStartFocus(targetTopic);
        return;
      }
    }
    // Fallback pseudo topic for subject
    const fallbackTopic: Topic = {
      id: rec.topicId || Date.now(),
      subjectId: rec.subjectId,
      chapterId: 1,
      userId: 1,
      name: rec.topicName,
      description: rec.reason,
      importance: rec.priority === 'HIGH' ? 'VERY_IMPORTANT' : 'NORMAL',
      status: 'IN_PROGRESS',
      understanding: rec.priority === 'HIGH' ? 'WEAK' : 'OKAY',
      notes: '',
      createdAt: Date.now(),
    };
    onStartFocus(fallbackTopic);
  };

  return (
    <div className="space-y-6">
      {/* Hero Banner */}
      <div className="bg-gradient-to-r from-amber-700 via-amber-600 to-orange-600 rounded-3xl p-6 text-white shadow-lg">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <RotateCcw className="w-5 h-5" />
              <span className="text-xs font-bold uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full">
                Adaptive University Revision OS
              </span>
            </div>
            <h2 className="text-2xl font-extrabold">AI Revision Plan & Practice Priorities</h2>
            <p className="text-xs text-amber-100 mt-1 max-w-xl">
              You stay in control of your daily routine. The AI analyzes your upcoming exams, weak topics, and repeated PYQs to recommend what to revise, practice, and prioritize today.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 self-start sm:self-auto">
            <button
              onClick={handleTriggerGenerate}
              disabled={isGeneratingRevisionPlan}
              className="px-4 py-2.5 rounded-2xl bg-white text-amber-800 hover:bg-amber-50 font-bold text-xs shadow-md transition-all flex items-center gap-2"
            >
              <RefreshCw className={`w-4 h-4 text-amber-700 ${isGeneratingRevisionPlan ? 'animate-spin' : ''}`} />
              {dynamicRevisionPlan.length === 0 ? 'Generate Revision Plan' : 'Update Revision Plan'}
            </button>
          </div>
        </div>

        {updateNotice && (
          <div className="mt-4 p-3 bg-white/20 backdrop-blur-xs rounded-xl border border-white/30 text-white text-xs font-bold flex items-center gap-2 animate-in fade-in">
            <Sparkles className="w-4 h-4 text-yellow-300 shrink-0" />
            <span>{updateNotice}</span>
          </div>
        )}

        {/* Real-time stats summary */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4 pt-4 border-t border-white/20 text-xs">
          <div>
            <span className="text-amber-200 block font-medium">High Priority</span>
            <span className="text-2xl font-black text-white">{highPriorityCount}</span>
          </div>
          <div>
            <span className="text-amber-200 block font-medium">Medium Priority</span>
            <span className="text-2xl font-black text-white">{mediumPriorityCount}</span>
          </div>
          <div>
            <span className="text-amber-200 block font-medium">Spaced Due Today</span>
            <span className="text-2xl font-black text-white">{dueRevisions.length}</span>
          </div>
          <div>
            <span className="text-amber-200 block font-medium">Total Revised</span>
            <span className="text-2xl font-black text-white">{completedRevisions.length + completedRecCount}</span>
          </div>
        </div>
      </div>

      {/* Main Switcher: AI Revision Plan vs Ebbinghaus Spaced Intervals */}
      <div className="flex bg-slate-200/80 p-1 rounded-2xl w-fit">
        <button
          onClick={() => setMainView('AI_PLAN')}
          className={`px-4 py-2 text-xs font-extrabold rounded-xl transition-all flex items-center gap-2 ${
            mainView === 'AI_PLAN'
              ? 'bg-white text-amber-800 shadow-xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <Sparkles className="w-3.5 h-3.5 text-amber-600" />
          AI Revision Plan ({dynamicRevisionPlan.length})
        </button>
        <button
          onClick={() => setMainView('SPACED_REPETITIONS')}
          className={`px-4 py-2 text-xs font-extrabold rounded-xl transition-all flex items-center gap-2 ${
            mainView === 'SPACED_REPETITIONS'
              ? 'bg-white text-amber-800 shadow-xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          <RotateCcw className="w-3.5 h-3.5 text-amber-600" />
          Spaced Repetitions ({dueRevisions.length + upcomingRevisions.length})
        </button>
      </div>

      {/* VIEW 1: AI REVISION PLAN (Requested Feature) */}
      {mainView === 'AI_PLAN' && (
        <div className="space-y-4">
          {/* Strategy Summary if present */}
          {aiRevisionSummary && (
            <div className="p-4 bg-indigo-50/80 border border-indigo-200 rounded-2xl flex items-start gap-3">
              <Brain className="w-5 h-5 text-indigo-600 shrink-0 mt-0.5" />
              <div>
                <span className="text-xs font-extrabold text-indigo-900 block">
                  AI Strategy Analysis
                </span>
                <p className="text-xs text-indigo-800 mt-0.5 leading-relaxed">
                  {aiRevisionSummary}
                </p>
              </div>
            </div>
          )}

          {/* Filters Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-slate-200/80 shadow-xs">
            <div className="flex items-center gap-1.5 overflow-x-auto">
              <span className="text-[11px] font-bold text-slate-400 mr-1 flex items-center gap-1">
                <Filter className="w-3 h-3" /> Priority:
              </span>
              {(['ALL', 'HIGH', 'MEDIUM', 'LOW'] as const).map((pri) => (
                <button
                  key={pri}
                  onClick={() => setPriorityFilter(pri)}
                  className={`px-2.5 py-1 rounded-lg text-xs font-bold transition-all ${
                    priorityFilter === pri
                      ? pri === 'HIGH'
                        ? 'bg-rose-600 text-white shadow-2xs'
                        : pri === 'MEDIUM'
                        ? 'bg-amber-600 text-white shadow-2xs'
                        : 'bg-slate-900 text-white shadow-2xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {pri}
                </button>
              ))}
            </div>

            {subjects.length > 0 && (
              <select
                value={subjectFilter}
                onChange={(e) => setSubjectFilter(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
                className="text-xs font-semibold px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 text-slate-700"
              >
                <option value="ALL">All Subjects</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Plan Items */}
          {dynamicRevisionPlan.length === 0 ? (
            <div className="bg-white rounded-3xl p-12 border border-slate-200 text-center text-slate-400 space-y-3">
              <RotateCcw className="w-12 h-12 mx-auto text-amber-400 opacity-60" />
              <h3 className="text-base font-bold text-slate-800">No revision plan yet</h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Complete your setup to generate your AI revision plan. Add subjects, syllabus topics, upcoming exam dates, or PYQs to let the engine recommend what to revise first.
              </p>
              <button
                onClick={handleTriggerGenerate}
                disabled={isGeneratingRevisionPlan}
                className="mt-2 px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors"
              >
                Generate Revision Plan
              </button>
            </div>
          ) : filteredAIPlan.length === 0 ? (
            <div className="bg-white rounded-2xl p-8 border border-slate-200 text-center text-xs text-slate-400">
              No recommendations match the selected priority filter.
            </div>
          ) : (
            <div className="space-y-3">
              {filteredAIPlan.map((rec) => {
                const isHigh = rec.priority === 'HIGH';
                const isMedium = rec.priority === 'MEDIUM';

                return (
                  <div
                    key={rec.id}
                    className={`bg-white rounded-2xl p-4.5 border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xs ${
                      rec.isCompleted
                        ? 'border-slate-200 bg-slate-50/60 opacity-60'
                        : isHigh
                        ? 'border-rose-200/90 bg-rose-50/20 hover:border-rose-300'
                        : isMedium
                        ? 'border-amber-200/90 bg-amber-50/20 hover:border-amber-300'
                        : 'border-slate-200 hover:border-indigo-300'
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
                        title="Mark as revised"
                      >
                        {rec.isCompleted && <CheckCircle2 className="w-4 h-4" />}
                      </button>

                      <div className="space-y-1">
                        <div className="flex flex-wrap items-center gap-2">
                          <span
                            className={`text-sm font-bold ${
                              rec.isCompleted ? 'line-through text-slate-400' : 'text-slate-900'
                            }`}
                          >
                            {rec.topicName}
                          </span>

                          <span
                            className={`text-[10px] font-extrabold px-2 py-0.2 rounded-full border ${
                              isHigh
                                ? 'bg-rose-100 text-rose-800 border-rose-300'
                                : isMedium
                                ? 'bg-amber-100 text-amber-800 border-amber-300'
                                : 'bg-slate-100 text-slate-700 border-slate-200'
                            }`}
                          >
                            {rec.priority} PRIORITY
                          </span>

                          <span className="text-xs font-semibold text-indigo-700">
                            {rec.subjectName}
                          </span>
                        </div>

                        {/* Clear Reason Display */}
                        <div className="text-xs text-slate-600 font-medium">
                          💡 <span className="font-bold text-slate-800">Reason:</span> {rec.reason}
                        </div>

                        <div className="flex flex-wrap items-center gap-3 text-[11px] text-slate-400">
                          <span>
                            🗓️ Suggested Window: <b>{rec.suggestedWindow || 'Urgent'}</b>
                          </span>
                          {rec.suggestedDate && (
                            <span>Target: {formatReadableDate(rec.suggestedDate)}</span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                      <button
                        onClick={() => handleLaunchFocus(rec)}
                        className="px-3 py-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-800 font-bold text-xs transition-colors flex items-center gap-1.5"
                      >
                        <Play className="w-3.5 h-3.5 fill-current" />
                        Start Focus
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* VIEW 2: EBBINGHAUS SPACED REPETITIONS */}
      {mainView === 'SPACED_REPETITIONS' && (
        <div className="space-y-4">
          <div className="flex items-center gap-2 bg-white p-2 rounded-2xl border border-slate-200/80 shadow-xs">
            <button
              onClick={() => setActiveSpacedTab('DUE')}
              className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all ${
                activeSpacedTab === 'DUE'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              Due Today ({dueRevisions.length})
            </button>
            <button
              onClick={() => setActiveSpacedTab('UPCOMING')}
              className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all ${
                activeSpacedTab === 'UPCOMING'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              Upcoming ({upcomingRevisions.length})
            </button>
            <button
              onClick={() => setActiveSpacedTab('COMPLETED')}
              className={`flex-1 py-2 text-xs font-bold rounded-xl transition-all ${
                activeSpacedTab === 'COMPLETED'
                  ? 'bg-amber-600 text-white shadow-xs'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              Mastered ({completedRevisions.length})
            </button>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
            {(activeSpacedTab === 'DUE' ? dueRevisions : activeSpacedTab === 'UPCOMING' ? upcomingRevisions : completedRevisions).length === 0 ? (
              <div className="py-12 text-center text-slate-400">
                <CheckCircle2 className="w-12 h-12 mx-auto mb-2 opacity-50 text-emerald-500" />
                <p className="text-sm font-semibold text-slate-700">No spaced repetitions in this queue.</p>
                <p className="text-xs text-slate-400 mt-1">
                  When you complete a topic in Subjects, memory intervals (1, 3, 7, 15 days) automatically appear here.
                </p>
              </div>
            ) : (
              <div className="space-y-3">
                {(activeSpacedTab === 'DUE' ? dueRevisions : activeSpacedTab === 'UPCOMING' ? upcomingRevisions : completedRevisions).map((rev) => {
                  const subject = subjects.find((s) => s.id === rev.subjectId);
                  const topic = topics.find((t) => t.id === rev.topicId);
                  const isOverdue = rev.scheduledDate < todayStr && rev.status !== 'COMPLETED';

                  return (
                    <div
                      key={rev.id}
                      className={`p-4 rounded-xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                        rev.status === 'COMPLETED'
                          ? 'bg-slate-50 border-slate-200 opacity-60'
                          : isOverdue
                          ? 'bg-rose-50/40 border-rose-200'
                          : 'bg-white border-slate-200/80 hover:border-amber-300'
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        <button
                          onClick={() => completeRevision(rev)}
                          disabled={rev.status === 'COMPLETED'}
                          className={`mt-0.5 w-5 h-5 rounded-md border flex items-center justify-center transition-colors ${
                            rev.status === 'COMPLETED'
                              ? 'bg-emerald-600 border-emerald-600 text-white'
                              : 'border-slate-300 hover:border-amber-600'
                          }`}
                        >
                          {rev.status === 'COMPLETED' && <CheckCircle2 className="w-4 h-4" />}
                        </button>

                        <div>
                          <div className="flex items-center gap-2">
                            <span
                              className={`text-sm font-bold ${
                                rev.status === 'COMPLETED' ? 'line-through text-slate-400' : 'text-slate-900'
                              }`}
                            >
                              {topic?.name || 'Study Topic'}
                            </span>
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                              Interval #{rev.revisionNumber}
                            </span>
                            {isOverdue && (
                              <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-rose-100 text-rose-800">
                                Overdue
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-slate-500 mt-0.5">
                            {subject?.name} • Scheduled: {formatReadableDate(rev.scheduledDate)}
                          </div>
                          {rev.notes && (
                            <p className="text-xs text-slate-400 mt-1 italic">{rev.notes}</p>
                          )}
                        </div>
                      </div>

                      {rev.status !== 'COMPLETED' && (
                        <div className="flex items-center gap-2 self-end sm:self-auto">
                          {topic && (
                            <button
                              onClick={() => onStartFocus(topic)}
                              className="px-3 py-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-800 font-bold text-xs transition-colors flex items-center gap-1.5"
                            >
                              <Play className="w-3.5 h-3.5 fill-current" /> Focus
                            </button>
                          )}
                          <button
                            onClick={() => {
                              setRescheduleItem(rev);
                              setTargetDate(todayStr);
                            }}
                            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
                            title="Reschedule"
                          >
                            <CalendarDays className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => skipRevision(rev)}
                            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
                            title="Skip this cycle"
                          >
                            <SkipForward className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Reschedule Modal */}
      {rescheduleItem && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl space-y-4">
            <h4 className="font-extrabold text-base text-slate-900">Reschedule Revision</h4>
            <p className="text-xs text-slate-500">
              Pick a new date for this spaced repetition milestone.
            </p>
            <input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
              className="w-full text-xs p-2.5 rounded-xl border border-slate-200"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setRescheduleItem(null)}
                className="px-3 py-1.5 text-xs font-semibold text-slate-500"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmReschedule}
                className="px-4 py-1.5 text-xs font-bold bg-amber-600 text-white rounded-xl"
              >
                Save Date
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
