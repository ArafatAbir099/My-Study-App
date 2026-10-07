import React, { useState } from 'react';
import {
  HelpCircle,
  Plus,
  CheckCircle2,
  Clock,
  Filter,
  Trash2,
  BookOpen,
  Award,
  AlertCircle,
  ChevronDown,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { PYQQuestion, PYQStatus, PYQDifficulty } from '../../types';

export const PYQScreen: React.FC = () => {
  const { pyqs, subjects, topics, addPYQ, updatePYQStatus, deletePYQ } = usePlanner();

  const [selectedSubject, setSelectedSubject] = useState<number | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<PYQStatus | 'ALL'>('ALL');
  const [difficultyFilter, setDifficultyFilter] = useState<PYQDifficulty | 'ALL'>('ALL');
  const [showAddModal, setShowAddModal] = useState(false);

  // Add PYQ Form
  const [formSubjectId, setFormSubjectId] = useState<number | ''>(subjects[0]?.id || '');
  const [formTopicId, setFormTopicId] = useState<number | ''>('');
  const [formExamType, setFormExamType] = useState('Midterm');
  const [formYear, setFormYear] = useState(2025);
  const [formQuestion, setFormQuestion] = useState('');
  const [formMarks, setFormMarks] = useState(10);
  const [formDifficulty, setFormDifficulty] = useState<PYQDifficulty>('MEDIUM');
  const [formIsImportant, setFormIsImportant] = useState(true);
  const [formSolution, setFormSolution] = useState('');

  const filteredPyqs = pyqs.filter((p) => {
    if (selectedSubject !== 'ALL' && p.subjectId !== selectedSubject) return false;
    if (statusFilter !== 'ALL' && p.status !== statusFilter) return false;
    if (difficultyFilter !== 'ALL' && p.difficulty !== difficultyFilter) return false;
    return true;
  });

  const totalCount = pyqs.length;
  const solvedCount = pyqs.filter((p) => p.status === 'SOLVED').length;
  const solvedPct = totalCount > 0 ? Math.round((solvedCount * 100) / totalCount) : 0;

  const handleCreatePYQ = (e: React.FormEvent) => {
    e.preventDefault();
    if (!formSubjectId || !formQuestion) return;

    addPYQ(
      Number(formSubjectId),
      formTopicId ? Number(formTopicId) : null,
      formExamType,
      formYear,
      formQuestion,
      formMarks,
      formDifficulty,
      'UNSOLVED',
      formIsImportant,
      formSolution
    );

    setFormQuestion('');
    setFormSolution('');
    setShowAddModal(false);
  };

  const subjectTopics = formSubjectId
    ? topics.filter((t) => t.subjectId === Number(formSubjectId))
    : [];

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-purple-700 to-indigo-700 rounded-3xl p-6 text-white shadow-lg">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <HelpCircle className="w-5 h-5" />
              <span className="text-xs font-bold uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full">
                Past Exam Analysis
              </span>
            </div>
            <h2 className="text-2xl font-extrabold">PYQ Practice & Question Bank</h2>
            <p className="text-xs text-purple-100 mt-1 max-w-xl">
              Track university previous year exam questions, prioritize high-frequency repeated questions, and verify your solving confidence.
            </p>
          </div>

          <button
            onClick={() => setShowAddModal(true)}
            className="px-4 py-2.5 rounded-xl bg-white text-purple-800 hover:bg-purple-50 font-bold text-xs shadow-md transition-all flex items-center gap-1.5 self-start sm:self-auto"
          >
            <Plus className="w-4 h-4" /> Add Question
          </button>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-3 gap-3 mt-4 pt-4 border-t border-white/20 text-xs">
          <div>
            <span className="text-purple-200 block">Total Questions</span>
            <span className="text-2xl font-black">{totalCount}</span>
          </div>
          <div>
            <span className="text-purple-200 block">Solved & Verified</span>
            <span className="text-2xl font-black text-emerald-300">{solvedCount}</span>
          </div>
          <div>
            <span className="text-purple-200 block">PYQ Mastery</span>
            <span className="text-2xl font-black">{solvedPct}%</span>
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-wrap items-center gap-2.5 bg-white p-3.5 rounded-2xl border border-slate-200/80 shadow-xs text-xs">
        <div className="flex items-center gap-1.5 text-slate-500 font-semibold mr-1">
          <Filter className="w-3.5 h-3.5" /> Filter:
        </div>

        <select
          value={selectedSubject}
          onChange={(e) => setSelectedSubject(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
          className="px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 font-medium text-slate-700"
        >
          <option value="ALL">All Subjects</option>
          {subjects.map((s) => (
            <option key={s.id} value={s.id}>{s.name}</option>
          ))}
        </select>

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as any)}
          className="px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 font-medium text-slate-700"
        >
          <option value="ALL">All Statuses</option>
          <option value="UNSOLVED">Unsolved</option>
          <option value="ATTEMPTED">Attempted</option>
          <option value="SOLVED">Solved</option>
        </select>

        <select
          value={difficultyFilter}
          onChange={(e) => setDifficultyFilter(e.target.value as any)}
          className="px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 font-medium text-slate-700"
        >
          <option value="ALL">All Difficulties</option>
          <option value="EASY">Easy</option>
          <option value="MEDIUM">Medium</option>
          <option value="HARD">Hard</option>
        </select>

        <span className="ml-auto text-slate-400 font-semibold">
          Showing {filteredPyqs.length} questions
        </span>
      </div>

      {/* Questions List */}
      <div className="space-y-3">
        {filteredPyqs.length === 0 ? (
          <div className="bg-white rounded-3xl p-12 border border-slate-200 text-center text-slate-400 space-y-3">
            <HelpCircle className="w-12 h-12 mx-auto text-purple-400 opacity-60" />
            <h3 className="text-base font-bold text-slate-800">
              {pyqs.length === 0 ? 'No PYQs added yet' : 'No PYQs matching filter'}
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              {pyqs.length === 0
                ? 'Upload or add previous-year questions to practice high-frequency topics, identify exam patterns, and power your AI revision plan.'
                : 'Try adjusting your subject or difficulty filters.'}
            </p>
            <button
              onClick={() => setShowAddModal(true)}
              className="mt-2 px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
            >
              + Add First PYQ
            </button>
          </div>
        ) : (
          filteredPyqs.map((pyq) => {
            const subject = subjects.find((s) => s.id === pyq.subjectId);
            const topic = topics.find((t) => t.id === pyq.topicId);
            const isSolved = pyq.status === 'SOLVED';

            return (
              <div
                key={pyq.id}
                className={`bg-white rounded-2xl p-5 border transition-all ${
                  isSolved
                    ? 'border-emerald-200 bg-emerald-50/20'
                    : 'border-slate-200/80 hover:border-purple-300'
                }`}
              >
                <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 mb-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="text-xs font-extrabold px-2.5 py-0.5 rounded-full bg-purple-100 text-purple-800">
                      {pyq.examType} {pyq.year}
                    </span>
                    <span className="text-xs font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700">
                      {pyq.marks} Marks
                    </span>
                    <span
                      className={`text-xs font-bold px-2 py-0.5 rounded-md ${
                        pyq.difficulty === 'HARD'
                          ? 'bg-rose-100 text-rose-700'
                          : pyq.difficulty === 'MEDIUM'
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-emerald-100 text-emerald-700'
                      }`}
                    >
                      {pyq.difficulty}
                    </span>
                    {pyq.isRepeated && (
                      <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-200">
                        Frequently Repeated
                      </span>
                    )}
                    {subject && (
                      <span className="text-xs font-bold text-indigo-700">
                        {subject.name}
                      </span>
                    )}
                  </div>

                  {/* Status toggle & delete */}
                  <div className="flex items-center gap-2 self-end sm:self-auto">
                    <select
                      value={pyq.status}
                      onChange={(e) => updatePYQStatus(pyq, e.target.value as PYQStatus)}
                      className={`text-xs font-bold px-2.5 py-1 rounded-xl border focus:outline-hidden ${
                        pyq.status === 'SOLVED'
                          ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                          : pyq.status === 'ATTEMPTED'
                          ? 'bg-amber-100 text-amber-800 border-amber-300'
                          : 'bg-slate-100 text-slate-700 border-slate-300'
                      }`}
                    >
                      <option value="UNSOLVED">Unsolved</option>
                      <option value="ATTEMPTED">Attempted</option>
                      <option value="SOLVED">Solved</option>
                    </select>

                    <button
                      onClick={() => deletePYQ(pyq.id)}
                      className="p-1.5 text-slate-300 hover:text-rose-600 rounded-lg transition-colors"
                      title="Delete PYQ"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                <p className="text-sm font-bold text-slate-900 leading-relaxed my-2">
                  {pyq.questionText}
                </p>

                {topic && (
                  <div className="text-xs text-slate-500 font-medium">
                    Topic: <span className="text-slate-700 font-semibold">{topic.name}</span>
                  </div>
                )}

                {pyq.solutionNotes && (
                  <div className="mt-3 p-3 rounded-xl bg-slate-50 border border-slate-100 text-xs text-slate-600 font-mono">
                    <span className="font-bold text-slate-800 block mb-1">Solution / Approach Notes:</span>
                    {pyq.solutionNotes}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Add PYQ Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-100 max-h-[90vh] overflow-y-auto">
            <h3 className="text-lg font-bold text-slate-900 mb-3">Add Past Year Question</h3>
            <form onSubmit={handleCreatePYQ} className="space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Subject *</label>
                  <select
                    required
                    value={formSubjectId}
                    onChange={(e) => setFormSubjectId(e.target.value ? Number(e.target.value) : '')}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Topic (Optional)</label>
                  <select
                    value={formTopicId}
                    onChange={(e) => setFormTopicId(e.target.value ? Number(e.target.value) : '')}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    <option value="">None / General</option>
                    {subjectTopics.map((t) => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Exam Type</label>
                  <input
                    type="text"
                    value={formExamType}
                    onChange={(e) => setFormExamType(e.target.value)}
                    placeholder="Midterm, Final"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Year</label>
                  <input
                    type="number"
                    value={formYear}
                    onChange={(e) => setFormYear(Number(e.target.value))}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Marks</label>
                  <input
                    type="number"
                    value={formMarks}
                    onChange={(e) => setFormMarks(Number(e.target.value))}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Difficulty</label>
                <select
                  value={formDifficulty}
                  onChange={(e) => setFormDifficulty(e.target.value as PYQDifficulty)}
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                >
                  <option value="EASY">Easy</option>
                  <option value="MEDIUM">Medium</option>
                  <option value="HARD">Hard</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Question Text *</label>
                <textarea
                  rows={3}
                  required
                  value={formQuestion}
                  onChange={(e) => setFormQuestion(e.target.value)}
                  placeholder="Paste the full past paper problem or prompt..."
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Solution / Approach Notes</label>
                <textarea
                  rows={2}
                  value={formSolution}
                  onChange={(e) => setFormSolution(e.target.value)}
                  placeholder="Key steps, formulas used, hints..."
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white rounded-xl shadow-xs"
                >
                  Save Question
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
