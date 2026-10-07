import React from 'react';
import {
  TrendingUp,
  Flame,
  Trophy,
  CheckCircle2,
  BookOpen,
  RotateCcw,
  HelpCircle,
  Brain,
  AlertCircle,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { StudyActivityHeatmapCard } from '../common/StudyActivityHeatmapCard';

export const ProgressScreen: React.FC = () => {
  const {
    topics,
    revisions,
    pyqs,
    streakStats,
    examReadinessList,
    studyRecommendations,
  } = usePlanner();

  const totalTopics = Math.max(1, topics.length);
  const completedTopics = topics.filter((t) => t.status === 'COMPLETED').length;
  const syllabusPct = Math.round((completedTopics * 100) / totalTopics);

  const strongTopics = topics.filter((t) => t.understanding === 'STRONG').length;
  const okayTopics = topics.filter((t) => t.understanding === 'OKAY').length;
  const weakTopics = topics.filter((t) => t.understanding === 'WEAK').length;

  const totalRevs = Math.max(1, revisions.length);
  const completedRevs = revisions.filter((r) => r.status === 'COMPLETED').length;
  const revPct = Math.round((completedRevs * 100) / totalRevs);

  const totalPyqs = Math.max(1, pyqs.length);
  const solvedPyqs = pyqs.filter((p) => p.status === 'SOLVED').length;
  const pyqPct = Math.round((solvedPyqs * 100) / totalPyqs);

  const readiness = examReadinessList[0];

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-extrabold text-slate-900">Academic Progress & Analytics</h2>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Holistic view of syllabus mastery, active recall consistency, and exam preparedness.
          </p>
        </div>
      </div>

      {/* Heatmap Card */}
      <StudyActivityHeatmapCard />

      {/* 4 Pillars Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Pillar 1: Syllabus */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Syllabus Coverage
            </span>
            <div className="p-2 rounded-xl bg-blue-50 text-blue-700">
              <BookOpen className="w-4 h-4" />
            </div>
          </div>
          <div className="text-3xl font-black text-slate-900">{syllabusPct}%</div>
          <p className="text-xs text-slate-500">
            {completedTopics} of {topics.length} topics finished
          </p>
          <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
            <div className="bg-blue-600 h-full rounded-full" style={{ width: `${syllabusPct}%` }} />
          </div>
        </div>

        {/* Pillar 2: Understanding */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Mastery Score
            </span>
            <div className="p-2 rounded-xl bg-emerald-50 text-emerald-700">
              <Brain className="w-4 h-4" />
            </div>
          </div>
          <div className="text-3xl font-black text-slate-900">
            {readiness?.understandingScore || 0}%
          </div>
          <p className="text-xs text-slate-500">
            {strongTopics} Strong • {okayTopics} Okay • {weakTopics} Weak
          </p>
          <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden flex">
            <div
              className="bg-emerald-500 h-full"
              style={{ width: `${(strongTopics / totalTopics) * 100}%` }}
              title="Strong"
            />
            <div
              className="bg-amber-400 h-full"
              style={{ width: `${(okayTopics / totalTopics) * 100}%` }}
              title="Okay"
            />
            <div
              className="bg-rose-500 h-full"
              style={{ width: `${(weakTopics / totalTopics) * 100}%` }}
              title="Weak"
            />
          </div>
        </div>

        {/* Pillar 3: Spaced Revisions */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Revision Retention
            </span>
            <div className="p-2 rounded-xl bg-amber-50 text-amber-700">
              <RotateCcw className="w-4 h-4" />
            </div>
          </div>
          <div className="text-3xl font-black text-slate-900">{revPct}%</div>
          <p className="text-xs text-slate-500">
            {completedRevs} of {revisions.length} spaced intervals done
          </p>
          <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
            <div className="bg-amber-500 h-full rounded-full" style={{ width: `${revPct}%` }} />
          </div>
        </div>

        {/* Pillar 4: PYQs */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              PYQ Solving Rate
            </span>
            <div className="p-2 rounded-xl bg-purple-50 text-purple-700">
              <HelpCircle className="w-4 h-4" />
            </div>
          </div>
          <div className="text-3xl font-black text-slate-900">{pyqPct}%</div>
          <p className="text-xs text-slate-500">
            {solvedPyqs} of {pyqs.length} past problems solved
          </p>
          <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
            <div className="bg-purple-600 h-full rounded-full" style={{ width: `${pyqPct}%` }} />
          </div>
        </div>
      </div>

      {/* Priority Recommendations Box */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
        <h3 className="font-bold text-slate-900 text-base mb-3 flex items-center gap-2">
          <Brain className="w-4 h-4 text-indigo-600" />
          Topics Needing Reinforcement
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {studyRecommendations.slice(0, 6).map((rec) => (
            <div
              key={rec.topicId}
              className="p-3.5 rounded-xl border border-slate-200/80 bg-slate-50/50 space-y-1.5"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-900 truncate">{rec.topicName}</span>
                <span className="text-[10px] font-extrabold px-1.5 py-0.5 rounded-md bg-indigo-100 text-indigo-800">
                  P-{rec.priorityScore}
                </span>
              </div>
              <div className="text-[11px] text-slate-500 flex flex-wrap gap-1">
                {rec.priorityReason.split('•').map((r, i) => (
                  <span key={i} className="px-1.5 py-0.2 rounded bg-slate-200 text-slate-700">
                    {r.trim()}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
