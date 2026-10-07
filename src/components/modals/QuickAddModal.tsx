import React, { useState } from 'react';
import {
  X,
  CalendarCheck,
  CalendarDays,
  BookOpen,
  ListPlus,
  RefreshCw,
  FileText,
  Link2,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { TopicImportance, PYQStatus } from '../../types';
import { getTodayStr } from '../../util/dateUtils';

type QuickAddOption = 'TASK' | 'EXAM' | 'SUBJECT' | 'TOPIC' | 'REVISION' | 'NOTE' | 'RESOURCE';

interface QuickAddModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const QuickAddModal: React.FC<QuickAddModalProps> = ({ isOpen, onClose }) => {
  const {
    subjects,
    chapters,
    addTask,
    addExamWithAutoPlan,
    addSubject,
    addTopic,
    addNote,
    addResource,
    selectedSemesterId,
  } = usePlanner();

  const [selectedType, setSelectedType] = useState<QuickAddOption | null>(null);

  // Form states
  // Task
  const [taskTitle, setTaskTitle] = useState('');
  const [taskSubjectId, setTaskSubjectId] = useState<number | ''>('');
  const [taskDate, setTaskDate] = useState(getTodayStr());
  const [taskStartTime, setTaskStartTime] = useState('10:00');
  const [taskDuration, setTaskDuration] = useState(60);

  // Exam
  const [examSubjectId, setExamSubjectId] = useState<number | ''>('');
  const [examType, setExamType] = useState('Midterm');
  const [examDate, setExamDate] = useState(getTodayStr());
  const [examTime, setExamTime] = useState('10:30 AM');
  const [examDuration, setExamDuration] = useState(90);

  // Subject
  const [subName, setSubName] = useState('');
  const [subCode, setSubCode] = useState('');
  const [subCredits, setSubCredits] = useState(3.0);
  const [subTeacher, setSubTeacher] = useState('');
  const [subColor, setSubColor] = useState('#2563EB');

  // Topic
  const [topicSubjectId, setTopicSubjectId] = useState<number | ''>('');
  const [topicName, setTopicName] = useState('');
  const [topicImportance, setTopicImportance] = useState<TopicImportance>('NORMAL');

  // Note
  const [noteTitle, setNoteTitle] = useState('');
  const [noteContent, setNoteContent] = useState('');
  const [noteSubjectId, setNoteSubjectId] = useState<number | ''>('');

  // Resource
  const [resTitle, setResTitle] = useState('');
  const [resType, setResType] = useState<'PDF' | 'LINK' | 'NOTE' | 'DOC'>('LINK');
  const [resContent, setResContent] = useState('');
  const [resSubjectId, setResSubjectId] = useState<number | ''>('');

  if (!isOpen) return null;

  const handleClose = () => {
    setSelectedType(null);
    onClose();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedType === 'TASK' && taskTitle) {
      const endHour = Math.floor(taskDuration / 60);
      addTask(
        taskTitle,
        taskSubjectId ? Number(taskSubjectId) : null,
        null,
        taskDate,
        taskStartTime,
        `${parseInt(taskStartTime.split(':')[0], 10) + endHour}:00`,
        taskDuration,
        'Quick added task'
      );
    } else if (selectedType === 'EXAM' && examSubjectId) {
      addExamWithAutoPlan(
        Number(examSubjectId),
        examType,
        examDate,
        examTime,
        examDuration,
        'Quick scheduled exam'
      );
    } else if (selectedType === 'SUBJECT' && subName) {
      addSubject(subName, subCode, subCredits, subTeacher, subColor, 'book', '');
    } else if (selectedType === 'TOPIC' && topicName && topicSubjectId) {
      const subChapters = chapters.filter((c) => c.subjectId === Number(topicSubjectId));
      const chId = subChapters[0]?.id || Date.now();
      addTopic(Number(topicSubjectId), chId, topicName, '', topicImportance);
    } else if (selectedType === 'NOTE' && noteTitle) {
      addNote(noteSubjectId ? Number(noteSubjectId) : null, null, noteTitle, noteContent);
    } else if (selectedType === 'RESOURCE' && resTitle) {
      addResource(
        resSubjectId ? Number(resSubjectId) : null,
        null,
        resTitle,
        resType,
        resContent
      );
    }
    handleClose();
  };

  const options: { type: QuickAddOption; label: string; desc: string; icon: React.ReactNode; color: string }[] = [
    {
      type: 'TASK',
      label: 'Study Task',
      desc: 'Schedule study or revision block',
      icon: <CalendarCheck className="w-5 h-5" />,
      color: 'bg-blue-50 text-blue-700 border-blue-200',
    },
    {
      type: 'EXAM',
      label: 'Upcoming Exam',
      desc: 'Quiz, Midterm, Final with auto-plan',
      icon: <CalendarDays className="w-5 h-5" />,
      color: 'bg-rose-50 text-rose-700 border-rose-200',
    },
    {
      type: 'SUBJECT',
      label: 'Subject / Course',
      desc: 'Add university course or module',
      icon: <BookOpen className="w-5 h-5" />,
      color: 'bg-indigo-50 text-indigo-700 border-indigo-200',
    },
    {
      type: 'TOPIC',
      label: 'Syllabus Topic',
      desc: 'Key concept to study and track',
      icon: <ListPlus className="w-5 h-5" />,
      color: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    },
    {
      type: 'NOTE',
      label: 'Academic Note',
      desc: 'Summary, formula sheet, cheat sheet',
      icon: <FileText className="w-5 h-5" />,
      color: 'bg-amber-50 text-amber-700 border-amber-200',
    },
    {
      type: 'RESOURCE',
      label: 'Lecture Resource',
      desc: 'External link, slides, document',
      icon: <Link2 className="w-5 h-5" />,
      color: 'bg-purple-50 text-purple-700 border-purple-200',
    },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-100 max-h-[90vh] overflow-y-auto relative">
        <button
          onClick={handleClose}
          className="absolute top-5 right-5 p-2 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100"
        >
          <X className="w-5 h-5" />
        </button>

        {!selectedType ? (
          <div>
            <div className="mb-4">
              <h3 className="text-lg font-bold text-slate-900">Quick Add to Student OS</h3>
              <p className="text-xs text-slate-500">Capture items directly into your active semester workspace.</p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {options.map((opt) => (
                <button
                  key={opt.type}
                  onClick={() => setSelectedType(opt.type)}
                  className="flex items-start gap-3 p-3.5 rounded-2xl border border-slate-200 hover:border-indigo-400 hover:shadow-sm text-left transition-all group"
                >
                  <div className={`p-2.5 rounded-xl border shrink-0 ${opt.color}`}>
                    {opt.icon}
                  </div>
                  <div>
                    <div className="text-sm font-bold text-slate-900 group-hover:text-indigo-600">
                      {opt.label}
                    </div>
                    <div className="text-xs text-slate-500">{opt.desc}</div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <button
                type="button"
                onClick={() => setSelectedType(null)}
                className="text-xs font-semibold text-indigo-600 hover:underline"
              >
                &larr; Choose different item
              </button>
              <h4 className="text-sm font-bold text-slate-900">
                New {options.find((o) => o.type === selectedType)?.label}
              </h4>
            </div>

            {selectedType === 'TASK' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Task Title *</label>
                  <input
                    type="text"
                    required
                    value={taskTitle}
                    onChange={(e) => setTaskTitle(e.target.value)}
                    placeholder="e.g. Graph Traversal BFS/DFS Practice"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Subject</label>
                    <select
                      value={taskSubjectId}
                      onChange={(e) => setTaskSubjectId(e.target.value ? Number(e.target.value) : '')}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    >
                      <option value="">None (General)</option>
                      {subjects.map((s) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Date</label>
                    <input
                      type="date"
                      value={taskDate}
                      onChange={(e) => setTaskDate(e.target.value)}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Start Time</label>
                    <input
                      type="time"
                      value={taskStartTime}
                      onChange={(e) => setTaskStartTime(e.target.value)}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Duration (Min)</label>
                    <input
                      type="number"
                      value={taskDuration}
                      onChange={(e) => setTaskDuration(Number(e.target.value))}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    />
                  </div>
                </div>
              </div>
            )}

            {selectedType === 'EXAM' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Subject *</label>
                  <select
                    required
                    value={examSubjectId}
                    onChange={(e) => setExamSubjectId(e.target.value ? Number(e.target.value) : '')}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    <option value="">Select subject...</option>
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name} ({s.courseCode || 'Course'})</option>
                    ))}
                  </select>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Exam Type</label>
                    <select
                      value={examType}
                      onChange={(e) => setExamType(e.target.value)}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    >
                      <option value="Midterm">Midterm</option>
                      <option value="Quiz">Quiz</option>
                      <option value="Final">Final Exam</option>
                      <option value="Lab Test">Lab Test</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Date</label>
                    <input
                      type="date"
                      required
                      value={examDate}
                      onChange={(e) => setExamDate(e.target.value)}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Time</label>
                    <input
                      type="text"
                      value={examTime}
                      onChange={(e) => setExamTime(e.target.value)}
                      placeholder="e.g. 10:30 AM"
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Duration (Min)</label>
                    <input
                      type="number"
                      value={examDuration}
                      onChange={(e) => setExamDuration(Number(e.target.value))}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    />
                  </div>
                </div>
              </div>
            )}

            {selectedType === 'SUBJECT' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Course Name *</label>
                  <input
                    type="text"
                    required
                    value={subName}
                    onChange={(e) => setSubName(e.target.value)}
                    placeholder="e.g. Operating Systems"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Course Code</label>
                    <input
                      type="text"
                      value={subCode}
                      onChange={(e) => setSubCode(e.target.value)}
                      placeholder="e.g. CS 304"
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Credits</label>
                    <input
                      type="number"
                      step="0.5"
                      value={subCredits}
                      onChange={(e) => setSubCredits(Number(e.target.value))}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Instructor</label>
                    <input
                      type="text"
                      value={subTeacher}
                      onChange={(e) => setSubTeacher(e.target.value)}
                      placeholder="e.g. Dr. Roberts"
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Theme Color</label>
                    <input
                      type="color"
                      value={subColor}
                      onChange={(e) => setSubColor(e.target.value)}
                      className="w-full h-10 p-1 rounded-xl border border-slate-200 cursor-pointer"
                    />
                  </div>
                </div>
              </div>
            )}

            {selectedType === 'TOPIC' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Subject *</label>
                  <select
                    required
                    value={topicSubjectId}
                    onChange={(e) => setTopicSubjectId(e.target.value ? Number(e.target.value) : '')}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    <option value="">Select subject...</option>
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Topic Name *</label>
                  <input
                    type="text"
                    required
                    value={topicName}
                    onChange={(e) => setTopicName(e.target.value)}
                    placeholder="e.g. Process Scheduling Algorithms"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Importance</label>
                  <select
                    value={topicImportance}
                    onChange={(e) => setTopicImportance(e.target.value as TopicImportance)}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    <option value="NORMAL">Normal</option>
                    <option value="IMPORTANT">Important</option>
                    <option value="VERY_IMPORTANT">Very Important</option>
                  </select>
                </div>
              </div>
            )}

            {selectedType === 'NOTE' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Subject (Optional)</label>
                  <select
                    value={noteSubjectId}
                    onChange={(e) => setNoteSubjectId(e.target.value ? Number(e.target.value) : '')}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    <option value="">General Note</option>
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Note Title *</label>
                  <input
                    type="text"
                    required
                    value={noteTitle}
                    onChange={(e) => setNoteTitle(e.target.value)}
                    placeholder="e.g. Memory Management Cheat Sheet"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Note Content</label>
                  <textarea
                    rows={4}
                    value={noteContent}
                    onChange={(e) => setNoteContent(e.target.value)}
                    placeholder="Key concepts, formulas, definitions..."
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden font-mono text-xs"
                  />
                </div>
              </div>
            )}

            {selectedType === 'RESOURCE' && (
              <div className="space-y-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Resource Title *</label>
                  <input
                    type="text"
                    required
                    value={resTitle}
                    onChange={(e) => setResTitle(e.target.value)}
                    placeholder="e.g. MIT OpenCourseWare Lecture Slides"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Type</label>
                    <select
                      value={resType}
                      onChange={(e) => setResType(e.target.value as any)}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    >
                      <option value="LINK">Web Link</option>
                      <option value="PDF">PDF File / Document</option>
                      <option value="SLIDES">Slides</option>
                      <option value="NOTE">Quick Note</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Subject</label>
                    <select
                      value={resSubjectId}
                      onChange={(e) => setResSubjectId(e.target.value ? Number(e.target.value) : '')}
                      className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                    >
                      <option value="">General</option>
                      {subjects.map((s) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      ))}
                    </select>
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">URL or Content</label>
                  <input
                    type="text"
                    value={resContent}
                    onChange={(e) => setResContent(e.target.value)}
                    placeholder="https://... or summary"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
              </div>
            )}

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={handleClose}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs"
              >
                Save & Add
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
