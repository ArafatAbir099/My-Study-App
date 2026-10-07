import React, { useState } from 'react';
import {
  BookOpen,
  Plus,
  Play,
  CheckCircle2,
  FileText,
  HelpCircle,
  Trash2,
  Edit,
  Upload,
  ChevronDown,
  ChevronRight,
  Layers,
  Sparkles,
  Calendar,
  Clock,
  AlertCircle,
  CalendarCheck,
  Check,
  Save,
  X,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { Subject, Topic, TopicImportance, TopicUnderstanding } from '../../types';
import {
  getTodayStr,
  formatReadableDate,
  formatExamDateStatus,
  getDaysRemaining,
  normalizeDate,
} from '../../util/dateUtils';
import { SyllabusUploadModal } from '../modals/SyllabusUploadModal';

interface SubjectsScreenProps {
  onStartFocus: (topic: Topic) => void;
}

export const SubjectsScreen: React.FC<SubjectsScreenProps> = ({ onStartFocus }) => {
  const {
    subjects,
    chapters,
    topics,
    pyqs,
    addSubject,
    deleteSubject,
    addTopic,
    completeTopic,
    deleteTopic,
    attachCourseSyllabus,
    setSubjectExamDate,
    updateTopicUnderstanding,
  } = usePlanner();

  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(
    subjects[0]?.id || null
  );

  // Modals
  const [showAddSubject, setShowAddSubject] = useState(false);
  const [showAddTopic, setShowAddTopic] = useState(false);
  const [showAttachSyllabus, setShowAttachSyllabus] = useState(false);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [topicToComplete, setTopicToComplete] = useState<Topic | null>(null);

  // Forms for Add Subject
  const [subName, setSubName] = useState('');
  const [subCode, setSubCode] = useState('');
  const [subCredits, setSubCredits] = useState(3.0);
  const [subTeacher, setSubTeacher] = useState('');
  const [subColor, setSubColor] = useState('#4F46E5');
  const [subExamDate, setSubExamDate] = useState('');
  const [subExamType, setSubExamType] = useState('Midterm');
  const [subExamTime, setSubExamTime] = useState('10:00 AM');

  // Exam Date Edit Form on Active Subject
  const [editingExamDate, setEditingExamDate] = useState(false);
  const [examDateInput, setExamDateInput] = useState('');
  const [examTypeInput, setExamTypeInput] = useState('Midterm');
  const [examTimeInput, setExamTimeInput] = useState('10:00 AM');
  const [examDateNotice, setExamDateNotice] = useState<string | null>(null);

  // Topic creation
  const [newTopicName, setNewTopicName] = useState('');
  const [newTopicDesc, setNewTopicDesc] = useState('');
  const [newTopicImportance, setNewTopicImportance] = useState<TopicImportance>('NORMAL');
  const [targetChapterId, setTargetChapterId] = useState<number | null>(null);

  // Syllabus text paste
  const [syllabusText, setSyllabusText] = useState('');
  const [syllabusNotice, setSyllabusNotice] = useState<string | null>(null);

  const activeSubject = subjects.find((s) => s.id === selectedSubjectId) || subjects[0];
  const subjectChapters = activeSubject
    ? chapters.filter((c) => c.subjectId === activeSubject.id)
    : [];
  const subjectTopics = activeSubject
    ? topics.filter((t) => t.subjectId === activeSubject.id)
    : [];
  const subjectPyqs = activeSubject
    ? pyqs.filter((p) => p.subjectId === activeSubject.id)
    : [];

  const handleCreateSubject = (e: React.FormEvent) => {
    e.preventDefault();
    if (!subName.trim()) return;
    const newSub = addSubject(
      subName.trim(),
      subCode.trim(),
      subCredits,
      subTeacher.trim(),
      subColor,
      'book',
      '',
      subExamDate ? normalizeDate(subExamDate) : null,
      subExamDate ? subExamType : null,
      subExamDate ? subExamTime : null
    );
    setSelectedSubjectId(newSub.id);
    setSubName('');
    setSubCode('');
    setSubTeacher('');
    setSubExamDate('');
    setShowAddSubject(false);
  };

  const handleStartEditExamDate = () => {
    if (activeSubject) {
      setExamDateInput(activeSubject.upcomingExamDate || getTodayStr());
      setExamTypeInput(activeSubject.upcomingExamType || 'Midterm');
      setExamTimeInput(activeSubject.upcomingExamTime || '10:00 AM');
      setEditingExamDate(true);
    }
  };

  const handleSaveExamDate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeSubject) return;

    if (!examDateInput) {
      // Remove exam date
      setSubjectExamDate(activeSubject.id, null);
      setExamDateNotice('Upcoming exam date removed.');
    } else {
      const cleanDate = normalizeDate(examDateInput);
      setSubjectExamDate(activeSubject.id, cleanDate, examTypeInput, examTimeInput, 90);
      setExamDateNotice(`Upcoming exam date saved for ${formatReadableDate(cleanDate)}!`);
    }

    setEditingExamDate(false);
    setTimeout(() => setExamDateNotice(null), 3000);
  };

  const handleRemoveExamDate = () => {
    if (!activeSubject) return;
    setSubjectExamDate(activeSubject.id, null);
    setEditingExamDate(false);
    setExamDateNotice('Upcoming exam date removed.');
    setTimeout(() => setExamDateNotice(null), 3000);
  };

  const handleCreateTopic = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTopicName.trim() || !activeSubject) return;
    const chId = targetChapterId || subjectChapters[0]?.id || Date.now();
    addTopic(activeSubject.id, chId, newTopicName.trim(), newTopicDesc.trim(), newTopicImportance);
    setNewTopicName('');
    setNewTopicDesc('');
    setShowAddTopic(false);
  };

  const handleAttachSyllabus = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeSubject || !syllabusText.trim()) return;
    const count = attachCourseSyllabus(activeSubject.id, syllabusText);
    setSyllabusNotice(`Successfully parsed and added ${count} syllabus topics into structured modules!`);
    setTimeout(() => {
      setSyllabusNotice(null);
      setShowAttachSyllabus(false);
      setSyllabusText('');
    }, 1500);
  };

  const handleCompleteTopicWithFeedback = (understanding: TopicUnderstanding) => {
    if (topicToComplete) {
      completeTopic(topicToComplete, understanding);
      setTopicToComplete(null);
    }
  };

  // Exam date status for active subject
  const examStatus = activeSubject ? formatExamDateStatus(activeSubject.upcomingExamDate) : null;
  const daysRemaining = activeSubject?.upcomingExamDate ? getDaysRemaining(activeSubject.upcomingExamDate) : null;

  return (
    <div className="space-y-6">
      {/* Subject Selector Tabs & Add Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-white p-4 rounded-2xl border border-slate-200/80 shadow-xs">
        <div className="flex items-center gap-2 overflow-x-auto pb-1 sm:pb-0">
          {subjects.length === 0 ? (
            <span className="text-xs font-semibold text-slate-400 italic py-1">
              No subjects yet. Click &quot;+ Add Subject&quot; to get started.
            </span>
          ) : (
            subjects.map((sub) => {
              const isSelected = activeSubject?.id === sub.id;
              const subTopicCount = topics.filter((t) => t.subjectId === sub.id).length;
              const completedCount = topics.filter(
                (t) => t.subjectId === sub.id && t.status === 'COMPLETED'
              ).length;
              const pct = subTopicCount > 0 ? Math.round((completedCount * 100) / subTopicCount) : 0;

              return (
                <button
                  key={sub.id}
                  onClick={() => {
                    setSelectedSubjectId(sub.id);
                    setEditingExamDate(false);
                  }}
                  className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 whitespace-nowrap border ${
                    isSelected
                      ? 'bg-slate-900 text-white border-slate-900 shadow-xs'
                      : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  <span
                    className="w-2.5 h-2.5 rounded-full"
                    style={{ backgroundColor: sub.colorHex || '#4F46E5' }}
                  />
                  <span>{sub.name}</span>
                  {sub.upcomingExamDate && (
                    <span className="text-[10px] px-1 py-0.2 rounded bg-rose-500/20 text-rose-300 font-medium">
                      📅
                    </span>
                  )}
                  <span
                    className={`text-[10px] px-1.5 py-0.2 rounded-md ${
                      isSelected ? 'bg-white/20' : 'bg-slate-200'
                    }`}
                  >
                    {pct}%
                  </span>
                </button>
              );
            })
          )}
        </div>

        <button
          onClick={() => setShowAddSubject(true)}
          className="px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-xs transition-colors flex items-center gap-1.5 self-start sm:self-auto shrink-0"
        >
          <Plus className="w-4 h-4" /> Add Subject
        </button>
      </div>

      {examDateNotice && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-xl animate-in fade-in flex items-center gap-2">
          <Check className="w-4 h-4 text-emerald-600" />
          {examDateNotice}
        </div>
      )}

      {/* Proper Empty State when user has no subjects yet */}
      {subjects.length === 0 ? (
        <div className="bg-white rounded-3xl p-12 border border-slate-200 text-center text-slate-400 space-y-3">
          <BookOpen className="w-12 h-12 mx-auto text-indigo-400 opacity-60" />
          <h3 className="text-lg font-bold text-slate-800">No subjects yet</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            Add your first university course to start tracking syllabus topics, scheduling exams, and generating your AI revision plan.
          </p>
          <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
            <button
              onClick={() => setShowAddSubject(true)}
              className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors"
            >
              Add your first subject
            </button>
            <button
              onClick={() => setShowUploadModal(true)}
              className="px-4 py-2 bg-purple-50 hover:bg-purple-100 text-purple-700 text-xs font-bold rounded-xl border border-purple-200 transition-colors flex items-center gap-1.5"
            >
              <Upload className="w-3.5 h-3.5" />
              Upload Syllabus Image
            </button>
          </div>
        </div>
      ) : activeSubject ? (
        <div className="space-y-6">
          {/* Active Subject Hero Card */}
          <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
              <div className="flex items-start gap-3.5">
                <div
                  className="w-12 h-12 rounded-2xl text-white flex items-center justify-center font-bold text-lg shadow-sm"
                  style={{ backgroundColor: activeSubject.colorHex || '#4F46E5' }}
                >
                  <BookOpen className="w-6 h-6" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h2 className="text-xl font-extrabold text-slate-900">{activeSubject.name}</h2>
                    {activeSubject.courseCode && (
                      <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700">
                        {activeSubject.courseCode}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    {activeSubject.credits} Credits • Instructor: {activeSubject.teacherName || 'TBA'}
                  </p>
                </div>
              </div>

              {/* Action buttons */}
              <div className="flex flex-wrap items-center gap-2">
                <button
                  onClick={() => setShowUploadModal(true)}
                  className="px-3.5 py-1.5 text-xs font-bold rounded-xl bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200 transition-colors flex items-center gap-1.5"
                >
                  <Sparkles className="w-3.5 h-3.5 text-purple-600" /> Upload Syllabus (AI)
                </button>
                <button
                  onClick={() => setShowAttachSyllabus(true)}
                  className="px-3 py-1.5 text-xs font-bold rounded-xl bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors flex items-center gap-1.5"
                >
                  <Upload className="w-3.5 h-3.5" /> Paste Syllabus Text
                </button>
                <button
                  onClick={() => {
                    setTargetChapterId(subjectChapters[0]?.id || null);
                    setShowAddTopic(true);
                  }}
                  className="px-3 py-1.5 text-xs font-bold rounded-xl bg-slate-900 text-white hover:bg-slate-800 transition-colors flex items-center gap-1.5"
                >
                  <Plus className="w-3.5 h-3.5" /> Add Topic
                </button>
                <button
                  onClick={() => deleteSubject(activeSubject.id)}
                  className="p-2 text-slate-400 hover:text-rose-600 rounded-xl hover:bg-rose-50 transition-colors"
                  title="Delete subject"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* UPCOMING EXAM DATE SECTION (Exact requested requirement) */}
            <div className="bg-gradient-to-r from-rose-50/70 via-pink-50/50 to-orange-50/60 rounded-2xl p-4 border border-rose-200/70">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-xl bg-rose-600 text-white flex items-center justify-center shrink-0 shadow-xs">
                    <CalendarCheck className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-extrabold uppercase tracking-wider text-rose-800">
                        Upcoming Exam Date
                      </span>
                      {examStatus && (
                        <span className={`text-[10px] px-2 py-0.5 rounded-full border ${examStatus.badgeClass}`}>
                          {examStatus.label}
                        </span>
                      )}
                    </div>

                    {activeSubject.upcomingExamDate ? (
                      <div className="mt-1">
                        <div className="flex flex-wrap items-baseline gap-2">
                          <span className="text-base font-extrabold text-slate-900">
                            {formatReadableDate(activeSubject.upcomingExamDate)}
                          </span>
                          <span className="text-xs font-bold text-rose-700">
                            ({activeSubject.upcomingExamType || 'Exam'} at {activeSubject.upcomingExamTime || '10:00 AM'})
                          </span>
                        </div>
                        <p className="text-xs font-bold text-slate-600 mt-0.5">
                          {activeSubject.name} &rarr; {formatReadableDate(activeSubject.upcomingExamDate)} &rarr;{' '}
                          <span className="text-rose-700">
                            {daysRemaining !== null && daysRemaining >= 0
                              ? `${daysRemaining} days remaining`
                              : daysRemaining === 0
                              ? 'Exam today'
                              : 'Exam completed'}
                          </span>
                        </p>
                      </div>
                    ) : (
                      <p className="text-xs text-slate-500 mt-1">
                        No upcoming exam date scheduled for this course. Set one to enable automatic revision priorities and exam countdown.
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2 self-start sm:self-center">
                  <button
                    onClick={handleStartEditExamDate}
                    className="px-3.5 py-1.5 rounded-xl bg-white hover:bg-rose-50 text-rose-700 border border-rose-200 text-xs font-bold shadow-xs transition-colors flex items-center gap-1.5"
                  >
                    <Edit className="w-3.5 h-3.5" />
                    {activeSubject.upcomingExamDate ? 'Edit Exam Date' : 'Set Exam Date'}
                  </button>
                  {activeSubject.upcomingExamDate && (
                    <button
                      onClick={handleRemoveExamDate}
                      className="px-3 py-1.5 rounded-xl text-slate-400 hover:text-rose-600 hover:bg-rose-50 text-xs font-semibold transition-colors"
                      title="Remove exam date"
                    >
                      Remove
                    </button>
                  )}
                </div>
              </div>

              {/* Inline Edit Exam Date Form */}
              {editingExamDate && (
                <form
                  onSubmit={handleSaveExamDate}
                  className="mt-4 pt-4 border-t border-rose-200/80 bg-white p-4 rounded-xl shadow-xs space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-extrabold text-slate-900">
                      Configure Upcoming Exam Date for {activeSubject.name}
                    </span>
                    <button
                      type="button"
                      onClick={() => setEditingExamDate(false)}
                      className="p-1 text-slate-400 hover:text-slate-600"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div>
                      <label className="block text-[11px] font-bold text-slate-600 mb-1">
                        Exam Date (Local Calendar) *
                      </label>
                      <input
                        type="date"
                        value={examDateInput}
                        onChange={(e) => setExamDateInput(e.target.value)}
                        required
                        className="w-full text-xs p-2 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white focus:border-rose-500 font-semibold text-slate-800"
                      />
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-slate-600 mb-1">
                        Exam Type / Name
                      </label>
                      <select
                        value={examTypeInput}
                        onChange={(e) => setExamTypeInput(e.target.value)}
                        className="w-full text-xs p-2 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white text-slate-800 font-semibold"
                      >
                        <option value="Midterm">Midterm Exam</option>
                        <option value="Final">Final Exam</option>
                        <option value="Quiz 1">Quiz 1</option>
                        <option value="Quiz 2">Quiz 2</option>
                        <option value="Lab Exam">Lab Exam</option>
                        <option value="Assessment">Assessment</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-slate-600 mb-1">
                        Exam Start Time
                      </label>
                      <input
                        type="text"
                        value={examTimeInput}
                        onChange={(e) => setExamTimeInput(e.target.value)}
                        placeholder="e.g. 10:00 AM"
                        className="w-full text-xs p-2 rounded-xl border border-slate-200 bg-slate-50 focus:bg-white text-slate-800 font-semibold"
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-end gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setEditingExamDate(false)}
                      className="px-3 py-1.5 text-xs font-semibold text-slate-500 hover:text-slate-700"
                    >
                      Cancel
                    </button>
                    {activeSubject.upcomingExamDate && (
                      <button
                        type="button"
                        onClick={handleRemoveExamDate}
                        className="px-3 py-1.5 text-xs font-bold text-rose-600 hover:bg-rose-50 rounded-xl"
                      >
                        Clear Date
                      </button>
                    )}
                    <button
                      type="submit"
                      className="px-4 py-1.5 text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white rounded-xl shadow-xs transition-colors flex items-center gap-1.5"
                    >
                      <Save className="w-3.5 h-3.5" /> Save Exam Date
                    </button>
                  </div>
                </form>
              )}
            </div>

            {/* Subject Summary Stats */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-4 text-xs">
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block font-medium">Total Modules</span>
                <span className="text-base font-extrabold text-slate-900">
                  {subjectChapters.length}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block font-medium">Syllabus Topics</span>
                <span className="text-base font-extrabold text-slate-900">
                  {subjectTopics.length}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block font-medium">Completed Topics</span>
                <span className="text-base font-extrabold text-emerald-700">
                  {subjectTopics.filter((t) => t.status === 'COMPLETED').length}
                </span>
              </div>
              <div className="p-3 rounded-xl bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block font-medium">PYQ Bank</span>
                <span className="text-base font-extrabold text-purple-700">
                  {subjectPyqs.length} questions
                </span>
              </div>
            </div>
          </div>

          {/* Chapters & Topics List */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-extrabold text-slate-900 flex items-center gap-2">
                <Layers className="w-5 h-5 text-indigo-600" /> Syllabus Modules & Topics
              </h3>
              <button
                onClick={() => setShowUploadModal(true)}
                className="text-xs font-bold text-purple-700 hover:text-purple-800 flex items-center gap-1"
              >
                <Sparkles className="w-3.5 h-3.5" /> Upload Image / PDF
              </button>
            </div>

            {subjectChapters.length === 0 ? (
              <div className="bg-white p-8 rounded-2xl border border-slate-200 text-center text-slate-400 space-y-2">
                <Layers className="w-10 h-10 mx-auto text-indigo-300 opacity-60" />
                <p className="text-sm font-semibold text-slate-700">No syllabus added yet</p>
                <p className="text-xs text-slate-400">
                  Upload a photo of your syllabus, or paste course outline text to structure your units and topics.
                </p>
                <div className="flex flex-wrap items-center justify-center gap-2 pt-2">
                  <button
                    onClick={() => setShowUploadModal(true)}
                    className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors flex items-center gap-1.5"
                  >
                    <Upload className="w-3.5 h-3.5" /> Upload Syllabus Image
                  </button>
                  <button
                    onClick={() => setShowAttachSyllabus(true)}
                    className="px-3.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-bold transition-colors"
                  >
                    Paste Text Syllabus
                  </button>
                </div>
              </div>
            ) : (
              subjectChapters.map((ch) => {
                const chapterTopics = subjectTopics.filter((t) => t.chapterId === ch.id);

                return (
                  <div
                    key={ch.id}
                    className="bg-white rounded-2xl border border-slate-200/80 overflow-hidden shadow-xs"
                  >
                    <div className="p-4 bg-slate-50/80 border-b border-slate-200/60 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="font-extrabold text-sm text-slate-900">{ch.title}</span>
                        <span className="text-xs font-semibold text-slate-400">
                          ({chapterTopics.length} topics)
                        </span>
                      </div>
                      <button
                        onClick={() => {
                          setTargetChapterId(ch.id);
                          setShowAddTopic(true);
                        }}
                        className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                      >
                        <Plus className="w-3.5 h-3.5" /> Topic
                      </button>
                    </div>

                    <div className="p-3 divide-y divide-slate-100">
                      {chapterTopics.length === 0 ? (
                        <div className="py-4 text-center text-xs text-slate-400">
                          No topics under this module yet.
                        </div>
                      ) : (
                        chapterTopics.map((topic) => {
                          const isDone = topic.status === 'COMPLETED';

                          return (
                            <div
                              key={topic.id}
                              className="py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 group"
                            >
                              <div className="flex items-start gap-3">
                                <button
                                  onClick={() => setTopicToComplete(topic)}
                                  className={`mt-0.5 w-5 h-5 rounded-md border flex items-center justify-center transition-colors ${
                                    isDone
                                      ? 'bg-emerald-600 border-emerald-600 text-white'
                                      : 'border-slate-300 hover:border-indigo-600'
                                  }`}
                                  title="Mark as completed & schedule revision"
                                >
                                  {isDone && <CheckCircle2 className="w-4 h-4" />}
                                </button>

                                <div>
                                  <div className="flex flex-wrap items-center gap-2">
                                    <span
                                      className={`text-sm font-bold ${
                                        isDone ? 'line-through text-slate-400' : 'text-slate-900'
                                      }`}
                                    >
                                      {topic.name}
                                    </span>

                                    {/* Importance Badge */}
                                    {topic.importance === 'VERY_IMPORTANT' && (
                                      <span className="text-[10px] font-extrabold px-2 py-0.2 rounded-full bg-rose-100 text-rose-700 border border-rose-200">
                                        High Exam Focus
                                      </span>
                                    )}

                                    {/* Understanding Badge */}
                                    <span
                                      className={`text-[10px] font-bold px-2 py-0.2 rounded-full ${
                                        topic.understanding === 'STRONG'
                                          ? 'bg-emerald-100 text-emerald-800'
                                          : topic.understanding === 'OKAY'
                                          ? 'bg-amber-100 text-amber-800'
                                          : 'bg-rose-100 text-rose-800'
                                      }`}
                                    >
                                      {topic.understanding}
                                    </span>
                                  </div>

                                  {topic.description && (
                                    <p className="text-xs text-slate-500 mt-0.5">{topic.description}</p>
                                  )}
                                </div>
                              </div>

                              <div className="flex items-center gap-2 self-end sm:self-auto">
                                <button
                                  onClick={() => onStartFocus(topic)}
                                  className="px-2.5 py-1 text-xs font-bold rounded-lg bg-emerald-50 text-emerald-700 hover:bg-emerald-100 transition-colors flex items-center gap-1"
                                >
                                  <Play className="w-3 h-3 fill-current" /> Study
                                </button>
                                <button
                                  onClick={() => deleteTopic(topic.id)}
                                  className="p-1 text-slate-300 hover:text-rose-600 rounded-lg transition-colors"
                                  title="Delete topic"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            </div>
                          );
                        })
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      ) : null}

      {/* Modal: Add Subject */}
      {showAddSubject && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-extrabold text-base text-slate-900">Add University Subject</h3>
              <button
                onClick={() => setShowAddSubject(false)}
                className="p-1 text-slate-400 hover:text-slate-600 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSubject} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Subject / Course Name *
                </label>
                <input
                  type="text"
                  value={subName}
                  onChange={(e) => setSubName(e.target.value)}
                  placeholder="e.g. Operating Systems"
                  required
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Course Code</label>
                  <input
                    type="text"
                    value={subCode}
                    onChange={(e) => setSubCode(e.target.value)}
                    placeholder="e.g. CS 304"
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Credits</label>
                  <input
                    type="number"
                    step="0.5"
                    value={subCredits}
                    onChange={(e) => setSubCredits(parseFloat(e.target.value) || 0)}
                    className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Instructor / Teacher</label>
                <input
                  type="text"
                  value={subTeacher}
                  onChange={(e) => setSubTeacher(e.target.value)}
                  placeholder="e.g. Prof. Davis"
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                />
              </div>

              {/* Optional Upcoming Exam Date right in creation */}
              <div className="p-3 bg-rose-50/60 rounded-2xl border border-rose-200/60 space-y-2">
                <span className="text-[11px] font-extrabold text-rose-800 block">
                  Upcoming Exam Date (Optional)
                </span>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] font-bold text-slate-600 mb-0.5">Date</label>
                    <input
                      type="date"
                      value={subExamDate}
                      onChange={(e) => setSubExamDate(e.target.value)}
                      className="w-full text-xs p-1.5 rounded-lg border border-slate-200 bg-white"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-bold text-slate-600 mb-0.5">Type</label>
                    <select
                      value={subExamType}
                      onChange={(e) => setSubExamType(e.target.value)}
                      className="w-full text-xs p-1.5 rounded-lg border border-slate-200 bg-white"
                    >
                      <option value="Midterm">Midterm</option>
                      <option value="Final">Final</option>
                      <option value="Quiz">Quiz</option>
                    </select>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddSubject(false)}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-500 hover:text-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  Create Subject
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Topic */}
      {showAddTopic && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-extrabold text-base text-slate-900">Add Topic to Syllabus</h3>
              <button
                onClick={() => setShowAddTopic(false)}
                className="p-1 text-slate-400 hover:text-slate-600 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateTopic} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Topic Name *</label>
                <input
                  type="text"
                  value={newTopicName}
                  onChange={(e) => setNewTopicName(e.target.value)}
                  placeholder="e.g. Virtual Memory & Page Replacement"
                  required
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Module / Unit</label>
                <select
                  value={targetChapterId || ''}
                  onChange={(e) => setTargetChapterId(Number(e.target.value) || null)}
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                >
                  {subjectChapters.map((ch) => (
                    <option key={ch.id} value={ch.id}>
                      {ch.title}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Description / Notes</label>
                <textarea
                  value={newTopicDesc}
                  onChange={(e) => setNewTopicDesc(e.target.value)}
                  placeholder="Brief summary or textbook reference"
                  rows={2}
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Exam Weight</label>
                <select
                  value={newTopicImportance}
                  onChange={(e) => setNewTopicImportance(e.target.value as TopicImportance)}
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-200 focus:border-indigo-600"
                >
                  <option value="NORMAL">Normal</option>
                  <option value="IMPORTANT">Important</option>
                  <option value="VERY_IMPORTANT">High Exam Focus (Very Important)</option>
                </select>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3">
                <button
                  type="button"
                  onClick={() => setShowAddTopic(false)}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-500 hover:text-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  Save Topic
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Text Syllabus Paste */}
      {showAttachSyllabus && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-extrabold text-base text-slate-900">
                Paste Syllabus for {activeSubject?.name}
              </h3>
              <button
                onClick={() => setShowAttachSyllabus(false)}
                className="p-1 text-slate-400 hover:text-slate-600 rounded-lg"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAttachSyllabus} className="space-y-3">
              <p className="text-xs text-slate-500">
                Paste your syllabus or curriculum below. The parser automatically detects modules, units, and individual topic bullet points.
              </p>

              <textarea
                value={syllabusText}
                onChange={(e) => setSyllabusText(e.target.value)}
                placeholder="Unit 1: Introduction&#10;- Basic Principles&#10;- Architecture&#10;&#10;Unit 2: Core Concepts&#10;- Algorithms&#10;- Performance Analysis"
                rows={8}
                required
                className="w-full text-xs p-3 rounded-xl border border-slate-200 focus:border-indigo-600 font-mono"
              />

              {syllabusNotice && (
                <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-xl">
                  {syllabusNotice}
                </div>
              )}

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAttachSyllabus(false)}
                  className="px-3.5 py-2 text-xs font-semibold text-slate-500 hover:text-slate-700"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
                >
                  Parse & Attach
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Topic Feedback Modal on completion */}
      {topicToComplete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl text-center space-y-4">
            <h4 className="font-extrabold text-base text-slate-900">Topic Understanding Level</h4>
            <p className="text-xs text-slate-500">
              How well did you understand &quot;<b>{topicToComplete.name}</b>&quot;? This drives spaced revision intervals.
            </p>

            <div className="grid grid-cols-3 gap-2 pt-2">
              <button
                onClick={() => handleCompleteTopicWithFeedback('STRONG')}
                className="p-3 rounded-2xl border border-emerald-200 bg-emerald-50 hover:bg-emerald-100 flex flex-col items-center gap-1 transition-all"
              >
                <span className="text-xl">💪</span>
                <span className="text-xs font-bold text-emerald-800">Strong</span>
                <span className="text-[10px] text-emerald-600">7-day interval</span>
              </button>

              <button
                onClick={() => handleCompleteTopicWithFeedback('OKAY')}
                className="p-3 rounded-2xl border border-amber-200 bg-amber-50 hover:bg-amber-100 flex flex-col items-center gap-1 transition-all"
              >
                <span className="text-xl">👍</span>
                <span className="text-xs font-bold text-amber-800">Okay</span>
                <span className="text-[10px] text-amber-600">3-day interval</span>
              </button>

              <button
                onClick={() => handleCompleteTopicWithFeedback('WEAK')}
                className="p-3 rounded-2xl border border-rose-200 bg-rose-50 hover:bg-rose-100 flex flex-col items-center gap-1 transition-all"
              >
                <span className="text-xl">🤔</span>
                <span className="text-xs font-bold text-rose-800">Weak</span>
                <span className="text-[10px] text-rose-600">1-day interval</span>
              </button>
            </div>

            <button
              onClick={() => handleCompleteTopicWithFeedback('OKAY')}
              className="text-xs text-slate-400 hover:text-slate-600 pt-2"
            >
              Skip rating
            </button>
          </div>
        </div>
      )}

      {/* AI Syllabus Upload & Vision Modal */}
      <SyllabusUploadModal
        isOpen={showUploadModal}
        onClose={() => setShowUploadModal(false)}
        targetSubjectId={activeSubject?.id || null}
      />
    </div>
  );
};
