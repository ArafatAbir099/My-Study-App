import React, { useState } from 'react';
import {
  Calendar,
  Plus,
  Sparkles,
  Clock,
  BookOpen,
  Trash2,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  ArrowRight,
  Edit,
  X,
  Save,
  CalendarCheck,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { ExamCycle, NavigationSection } from '../../types';
import {
  getTodayStr,
  formatReadableDate,
  formatExamDateStatus,
  normalizeDate,
} from '../../util/dateUtils';

interface ExamsScreenProps {
  onNavigate: (section: NavigationSection) => void;
}

export const ExamsScreen: React.FC<ExamsScreenProps> = ({ onNavigate }) => {
  const {
    exams,
    subjects,
    topics,
    tasks,
    addExamWithAutoPlan,
    updateExam,
    regenerateExamPlan,
    deleteExam,
    autoPlanAllExams,
    examReadinessList,
    setSubjectExamDate,
  } = usePlanner();

  const [showAddModal, setShowAddModal] = useState(false);
  const [editingExam, setEditingExam] = useState<ExamCycle | null>(null);

  // Form State for Add / Edit
  const [subId, setSubId] = useState<number | ''>(subjects[0]?.id || '');
  const [examType, setExamType] = useState('Midterm');
  const [examDate, setExamDate] = useState(getTodayStr());
  const [examTime, setExamTime] = useState('10:00 AM');
  const [examDuration, setExamDuration] = useState(90);
  const [notes, setNotes] = useState('');
  const [planMessage, setPlanMessage] = useState<string | null>(null);

  const handleOpenAdd = () => {
    setSubId(subjects[0]?.id || '');
    setExamType('Midterm');
    setExamDate(getTodayStr());
    setExamTime('10:00 AM');
    setExamDuration(90);
    setNotes('');
    setShowAddModal(true);
  };

  const handleOpenEdit = (exam: ExamCycle) => {
    setEditingExam(exam);
    setSubId(exam.subjectId);
    setExamType(exam.examType);
    setExamDate(exam.examDate);
    setExamTime(exam.examTime);
    setExamDuration(exam.durationMinutes);
    setNotes(exam.notes);
  };

  const handleCreateExam = (e: React.FormEvent) => {
    e.preventDefault();
    if (!subId) return;

    const cleanDate = normalizeDate(examDate);
    const res = addExamWithAutoPlan(
      Number(subId),
      examType,
      cleanDate,
      examTime,
      examDuration,
      notes
    );

    // Keep subject exam date synced
    setSubjectExamDate(Number(subId), cleanDate, examType, examTime, examDuration);

    setPlanMessage(res.message);
    setTimeout(() => {
      setPlanMessage(null);
      setShowAddModal(false);
    }, 2000);
  };

  const handleSaveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingExam) return;

    const cleanDate = normalizeDate(examDate);
    const subject = subjects.find((s) => s.id === Number(subId));
    const updated: ExamCycle = {
      ...editingExam,
      subjectId: Number(subId),
      name: `${subject?.name || 'Subject'} ${examType}`,
      examType,
      examDate: cleanDate,
      examTime,
      durationMinutes: examDuration,
      notes,
    };

    updateExam(updated);
    setSubjectExamDate(Number(subId), cleanDate, examType, examTime, examDuration);

    setPlanMessage(`Updated exam schedule for ${formatReadableDate(cleanDate)}!`);
    setEditingExam(null);
    setTimeout(() => setPlanMessage(null), 2500);
  };

  const handleDeleteExam = (exam: ExamCycle) => {
    deleteExam(exam.id);
    // Also remove from subject if it was the upcoming exam
    setSubjectExamDate(exam.subjectId, null);
  };

  const handleAutoPlanAll = () => {
    const res = autoPlanAllExams();
    setPlanMessage(res.message);
    setTimeout(() => setPlanMessage(null), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Hero Banner */}
      <div className="bg-gradient-to-r from-rose-700 via-rose-600 to-pink-600 rounded-3xl p-6 text-white shadow-lg">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Calendar className="w-5 h-5" />
              <span className="text-xs font-bold uppercase tracking-wider bg-white/20 px-2.5 py-0.5 rounded-full">
                Exam Cycles & Readiness
              </span>
            </div>
            <h2 className="text-2xl font-extrabold">Exams & Automated Prep Schedules</h2>
            <p className="text-xs text-rose-100 mt-1 max-w-xl">
              Schedule your upcoming midterms, quizzes, and finals with exact local dates and countdowns. The planner computes syllabus readiness and schedules milestone revisions.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2 self-start sm:self-auto">
            {exams.length > 0 && (
              <button
                onClick={handleAutoPlanAll}
                className="px-3.5 py-2 rounded-xl bg-white text-rose-700 hover:bg-rose-50 font-bold text-xs shadow-md transition-all flex items-center gap-1.5"
              >
                <Sparkles className="w-4 h-4 text-rose-600" />
                Auto-Plan All Exams
              </button>
            )}
            <button
              onClick={handleOpenAdd}
              className="px-3.5 py-2 rounded-xl bg-rose-900/60 hover:bg-rose-900/80 border border-white/20 text-white font-bold text-xs transition-all flex items-center gap-1.5"
            >
              <Plus className="w-4 h-4" /> Add Exam
            </button>
          </div>
        </div>

        {planMessage && (
          <div className="mt-4 p-3 bg-white/20 backdrop-blur-xs rounded-xl border border-white/30 text-white text-xs font-bold animate-in fade-in">
            {planMessage}
          </div>
        )}
      </div>

      {/* Exam List */}
      <div className="space-y-4">
        {exams.length === 0 ? (
          <div className="bg-white rounded-3xl p-12 border border-slate-200 text-center text-slate-400 space-y-3">
            <Calendar className="w-12 h-12 mx-auto text-rose-400 opacity-60" />
            <h3 className="text-lg font-bold text-slate-800">No upcoming exams</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Add your exam date to track exact days remaining, syllabus coverage, topic weaknesses, and automatic study milestones.
            </p>
            <button
              onClick={handleOpenAdd}
              className="mt-2 px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors"
            >
              Add your exam date
            </button>
          </div>
        ) : (
          exams.map((exam, idx) => {
            const subject = subjects.find((s) => s.id === exam.subjectId);
            const readiness = examReadinessList.find((r) => r.examName === exam.name) || examReadinessList[idx];
            const dateStatus = formatExamDateStatus(exam.examDate);

            return (
              <div
                key={exam.id}
                className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs hover:border-rose-200 transition-all space-y-4"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-lg font-extrabold text-slate-900">{exam.name}</span>
                      <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-rose-100 text-rose-800">
                        {exam.examType}
                      </span>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full border ${dateStatus.badgeClass}`}>
                        {dateStatus.label}
                      </span>
                    </div>

                    <div className="flex flex-wrap items-center gap-3 mt-1.5 text-xs text-slate-500">
                      {subject && (
                        <span className="font-bold text-slate-800">
                          {subject.name} {subject.courseCode ? `(${subject.courseCode})` : ''}
                        </span>
                      )}
                      <span>
                        Date: <b className="text-slate-900">{formatReadableDate(exam.examDate)}</b> at {exam.examTime}
                      </span>
                      <span>Duration: {exam.durationMinutes} mins</span>
                    </div>

                    {/* Subject -> Exam Date -> Days Remaining string */}
                    <div className="text-xs font-semibold text-rose-700 mt-1">
                      {subject?.name || 'Subject'} &rarr; {formatReadableDate(exam.examDate)} &rarr;{' '}
                      <span className="font-bold">
                        {dateStatus.days !== null && dateStatus.days >= 0
                          ? `${dateStatus.days} days remaining`
                          : dateStatus.days === 0
                          ? 'Exam today'
                          : 'Exam completed'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleOpenEdit(exam)}
                      className="p-2 rounded-xl text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 transition-colors"
                      title="Edit exam date & info"
                    >
                      <Edit className="w-4 h-4" />
                    </button>

                    <button
                      onClick={() => regenerateExamPlan(exam)}
                      className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                      title="Regenerate adaptive study plan"
                    >
                      <RefreshCw className="w-4 h-4" />
                    </button>

                    <button
                      onClick={() => handleDeleteExam(exam)}
                      className="p-2 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
                      title="Delete exam"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Exam Readiness Score Breakdown */}
                {readiness && (
                  <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 p-4 rounded-2xl bg-slate-50/80 border border-slate-200/60 text-xs">
                    <div>
                      <span className="text-slate-400 block font-medium">Readiness Score</span>
                      <span className="text-xl font-black text-indigo-700">
                        {readiness.overallScore}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-medium">Syllabus Covered</span>
                      <span className="text-base font-extrabold text-blue-600">
                        {readiness.syllabusScore}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-medium">Topic Understanding</span>
                      <span className="text-base font-extrabold text-emerald-600">
                        {readiness.understandingScore}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-medium">Revisions Done</span>
                      <span className="text-base font-extrabold text-amber-600">
                        {readiness.revisionScore}%
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block font-medium">PYQs Solved</span>
                      <span className="text-base font-extrabold text-purple-600">
                        {readiness.pyqScore}%
                      </span>
                    </div>
                  </div>
                )}

                {readiness?.mainWeakness && (
                  <div className="flex items-center gap-2 text-xs text-slate-600 pt-1">
                    <span className="text-slate-400 font-semibold">Priority Area:</span>
                    <span className="font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-md border border-amber-200">
                      {readiness.mainWeakness}
                    </span>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Modal: Add Exam */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-extrabold text-base text-slate-900">Add Upcoming Exam Date</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="p-1 text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateExam} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Subject *</label>
                <select
                  value={subId}
                  onChange={(e) => setSubId(Number(e.target.value) || '')}
                  required
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                >
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.courseCode || 'Course'})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Exam Type</label>
                  <select
                    value={examType}
                    onChange={(e) => setExamType(e.target.value)}
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                  >
                    <option value="Midterm">Midterm Exam</option>
                    <option value="Final">Final Exam</option>
                    <option value="Quiz 1">Quiz 1</option>
                    <option value="Quiz 2">Quiz 2</option>
                    <option value="Lab Exam">Lab Exam</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Exam Date *</label>
                  <input
                    type="date"
                    value={examDate}
                    onChange={(e) => setExamDate(e.target.value)}
                    required
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600 font-semibold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Start Time</label>
                  <input
                    type="text"
                    value={examTime}
                    onChange={(e) => setExamTime(e.target.value)}
                    placeholder="10:00 AM"
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Duration (mins)</label>
                  <input
                    type="number"
                    value={examDuration}
                    onChange={(e) => setExamDuration(parseInt(e.target.value, 10) || 90)}
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Notes / Syllabus Focus</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="e.g. Modules 1-3, formula sheet allowed"
                  rows={2}
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-500 hover:text-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  Save Exam
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Edit Exam */}
      {editingExam && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-extrabold text-base text-slate-900">Edit Exam Details</h3>
              <button
                onClick={() => setEditingExam(null)}
                className="p-1 text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveEdit} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Subject</label>
                <select
                  value={subId}
                  onChange={(e) => setSubId(Number(e.target.value) || '')}
                  required
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                >
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} ({s.courseCode || 'Course'})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Exam Type</label>
                  <select
                    value={examType}
                    onChange={(e) => setExamType(e.target.value)}
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                  >
                    <option value="Midterm">Midterm Exam</option>
                    <option value="Final">Final Exam</option>
                    <option value="Quiz 1">Quiz 1</option>
                    <option value="Quiz 2">Quiz 2</option>
                    <option value="Lab Exam">Lab Exam</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Exam Date *</label>
                  <input
                    type="date"
                    value={examDate}
                    onChange={(e) => setExamDate(e.target.value)}
                    required
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600 font-semibold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Start Time</label>
                  <input
                    type="text"
                    value={examTime}
                    onChange={(e) => setExamTime(e.target.value)}
                    placeholder="10:00 AM"
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Duration (mins)</label>
                  <input
                    type="number"
                    value={examDuration}
                    onChange={(e) => setExamDuration(parseInt(e.target.value, 10) || 90)}
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Notes / Topics Focus</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={2}
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-rose-600"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setEditingExam(null)}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-500 hover:text-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
