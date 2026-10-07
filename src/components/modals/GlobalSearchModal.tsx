import React, { useState } from 'react';
import { Search, X, BookOpen, CheckSquare, FileText, Calendar, HelpCircle, Layers } from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { NavigationSection } from '../../types';

interface GlobalSearchModalProps {
  isOpen: boolean;
  onClose: () => void;
  onNavigate: (section: NavigationSection) => void;
}

export const GlobalSearchModal: React.FC<GlobalSearchModalProps> = ({
  isOpen,
  onClose,
  onNavigate,
}) => {
  const { subjects, topics, tasks, pyqs, exams, notes } = usePlanner();
  const [query, setQuery] = useState('');

  if (!isOpen) return null;

  const q = query.trim().toLowerCase();

  const matchedSubjects = q
    ? subjects.filter((s) => s.name.toLowerCase().includes(q) || s.courseCode.toLowerCase().includes(q))
    : [];
  const matchedTopics = q
    ? topics.filter((t) => t.name.toLowerCase().includes(q) || t.description.toLowerCase().includes(q))
    : [];
  const matchedTasks = q
    ? tasks.filter((t) => t.title.toLowerCase().includes(q) || t.notes.toLowerCase().includes(q))
    : [];
  const matchedPyqs = q
    ? pyqs.filter((p) => p.questionText.toLowerCase().includes(q) || p.solutionNotes.toLowerCase().includes(q))
    : [];
  const matchedExams = q
    ? exams.filter((e) => e.name.toLowerCase().includes(q) || e.examType.toLowerCase().includes(q))
    : [];
  const matchedNotes = q
    ? notes.filter((n) => n.title.toLowerCase().includes(q) || n.content.toLowerCase().includes(q))
    : [];

  const totalResults =
    matchedSubjects.length +
    matchedTopics.length +
    matchedTasks.length +
    matchedPyqs.length +
    matchedExams.length +
    matchedNotes.length;

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center p-4 pt-16 sm:pt-24 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-2xl w-full shadow-2xl border border-slate-100 overflow-hidden flex flex-col max-h-[80vh]">
        {/* Search Input Bar */}
        <div className="flex items-center gap-3 p-4 border-b border-slate-100 bg-slate-50/50">
          <Search className="w-5 h-5 text-indigo-600 shrink-0" />
          <input
            type="text"
            autoFocus
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search subjects, topics, PYQs, tasks, exams, notes..."
            className="w-full text-base bg-transparent border-none focus:outline-hidden placeholder:text-slate-400 font-medium"
          />
          {query && (
            <button
              onClick={() => setQuery('')}
              className="p-1 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-200"
            >
              <X className="w-4 h-4" />
            </button>
          )}
          <button
            onClick={onClose}
            className="text-xs font-semibold px-2.5 py-1 rounded-lg bg-slate-200/80 text-slate-700 hover:bg-slate-300"
          >
            Esc
          </button>
        </div>

        {/* Results Area */}
        <div className="overflow-y-auto p-4 space-y-4">
          {!q ? (
            <div className="text-center py-8 text-slate-400 text-sm">
              Type keywords to search across all your semester courses and study records.
            </div>
          ) : totalResults === 0 ? (
            <div className="text-center py-8 text-slate-400 text-sm">
              No results found for &ldquo;<span className="font-semibold text-slate-600">{query}</span>&rdquo;
            </div>
          ) : (
            <>
              {/* Subjects */}
              {matchedSubjects.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                    <BookOpen className="w-3.5 h-3.5 text-blue-600" /> Subjects ({matchedSubjects.length})
                  </h4>
                  <div className="space-y-1.5">
                    {matchedSubjects.map((sub) => (
                      <button
                        key={sub.id}
                        onClick={() => {
                          onNavigate('SUBJECTS');
                          onClose();
                        }}
                        className="w-full text-left p-2.5 rounded-xl hover:bg-indigo-50/70 border border-transparent hover:border-indigo-100 flex items-center justify-between transition-colors"
                      >
                        <div>
                          <div className="text-sm font-bold text-slate-900">{sub.name}</div>
                          <div className="text-xs text-slate-500">{sub.courseCode} • {sub.credits} credits</div>
                        </div>
                        <span className="text-xs text-indigo-600 font-medium">View Course &rarr;</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Topics */}
              {matchedTopics.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                    <Layers className="w-3.5 h-3.5 text-emerald-600" /> Syllabus Topics ({matchedTopics.length})
                  </h4>
                  <div className="space-y-1.5">
                    {matchedTopics.map((top) => (
                      <button
                        key={top.id}
                        onClick={() => {
                          onNavigate('SUBJECTS');
                          onClose();
                        }}
                        className="w-full text-left p-2.5 rounded-xl hover:bg-emerald-50/70 border border-transparent hover:border-emerald-100 flex items-center justify-between transition-colors"
                      >
                        <div>
                          <div className="text-sm font-semibold text-slate-900">{top.name}</div>
                          <div className="text-xs text-slate-500">Status: {top.status} • Understanding: {top.understanding}</div>
                        </div>
                        <span className="text-xs text-emerald-600 font-medium">Jump &rarr;</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Tasks */}
              {matchedTasks.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                    <CheckSquare className="w-3.5 h-3.5 text-amber-600" /> Study Tasks ({matchedTasks.length})
                  </h4>
                  <div className="space-y-1.5">
                    {matchedTasks.map((task) => (
                      <button
                        key={task.id}
                        onClick={() => {
                          onNavigate('CALENDAR');
                          onClose();
                        }}
                        className="w-full text-left p-2.5 rounded-xl hover:bg-amber-50/70 border border-transparent hover:border-amber-100 flex items-center justify-between transition-colors"
                      >
                        <div>
                          <div className="text-sm font-semibold text-slate-900">{task.title}</div>
                          <div className="text-xs text-slate-500">{task.date} ({task.startTime} - {task.endTime})</div>
                        </div>
                        <span className="text-xs text-amber-600 font-medium">Calendar &rarr;</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* PYQs */}
              {matchedPyqs.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                    <HelpCircle className="w-3.5 h-3.5 text-purple-600" /> PYQ Questions ({matchedPyqs.length})
                  </h4>
                  <div className="space-y-1.5">
                    {matchedPyqs.map((pyq) => (
                      <button
                        key={pyq.id}
                        onClick={() => {
                          onNavigate('PYQS');
                          onClose();
                        }}
                        className="w-full text-left p-2.5 rounded-xl hover:bg-purple-50/70 border border-transparent hover:border-purple-100 flex items-center justify-between transition-colors"
                      >
                        <div className="max-w-[80%]">
                          <div className="text-sm font-semibold text-slate-900 truncate">{pyq.questionText}</div>
                          <div className="text-xs text-slate-500">{pyq.examType} {pyq.year} • {pyq.marks} Marks • {pyq.difficulty}</div>
                        </div>
                        <span className="text-xs text-purple-600 font-medium">PYQ Bank &rarr;</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Exams */}
              {matchedExams.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-rose-600" /> Exams ({matchedExams.length})
                  </h4>
                  <div className="space-y-1.5">
                    {matchedExams.map((exam) => (
                      <button
                        key={exam.id}
                        onClick={() => {
                          onNavigate('EXAMS');
                          onClose();
                        }}
                        className="w-full text-left p-2.5 rounded-xl hover:bg-rose-50/70 border border-transparent hover:border-rose-100 flex items-center justify-between transition-colors"
                      >
                        <div>
                          <div className="text-sm font-semibold text-slate-900">{exam.name}</div>
                          <div className="text-xs text-slate-500">Date: {exam.examDate} at {exam.examTime}</div>
                        </div>
                        <span className="text-xs text-rose-600 font-medium">Exams &rarr;</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Notes */}
              {matchedNotes.length > 0 && (
                <div>
                  <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400 mb-2 flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5 text-indigo-600" /> Study Notes ({matchedNotes.length})
                  </h4>
                  <div className="space-y-1.5">
                    {matchedNotes.map((note) => (
                      <button
                        key={note.id}
                        onClick={() => {
                          onNavigate('RESOURCES');
                          onClose();
                        }}
                        className="w-full text-left p-2.5 rounded-xl hover:bg-indigo-50/70 border border-transparent hover:border-indigo-100 flex items-center justify-between transition-colors"
                      >
                        <div>
                          <div className="text-sm font-semibold text-slate-900">{note.title}</div>
                          <div className="text-xs text-slate-500 truncate max-w-sm">{note.content}</div>
                        </div>
                        <span className="text-xs text-indigo-600 font-medium">Notes &rarr;</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
