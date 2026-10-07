import React, { useState } from 'react';
import {
  School,
  Plus,
  Archive,
  ArchiveRestore,
  Trash2,
  CheckCircle2,
  BookOpen,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { Semester } from '../../types';
import { getTodayStr, formatReadableDate } from '../../util/dateUtils';

export const SemestersScreen: React.FC = () => {
  const {
    semesters,
    selectedSemesterId,
    selectSemester,
    addSemester,
    toggleSemesterArchive,
    deleteSemester,
    subjects,
  } = usePlanner();

  const [showAddModal, setShowAddModal] = useState(false);
  const [name, setName] = useState('');
  const [year, setYear] = useState('2026-2027');
  const [startDate, setStartDate] = useState(getTodayStr());
  const [endDate, setEndDate] = useState('');
  const [notes, setNotes] = useState('');

  const activeSemesters = semesters.filter((s) => !s.isArchived);
  const archivedSemesters = semesters.filter((s) => s.isArchived);

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name) return;
    addSemester(name, year, startDate, endDate, notes);
    setName('');
    setNotes('');
    setShowAddModal(false);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2">
            <School className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-extrabold text-slate-900">Semester Management</h2>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Organize terms and academic years. Past semesters can be archived with all notes and syllabus data safely preserved.
          </p>
        </div>

        <button
          onClick={() => setShowAddModal(true)}
          className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-xs transition-colors flex items-center gap-1.5 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" /> New Semester
        </button>
      </div>

      {/* Active Semesters */}
      <div className="space-y-3">
        <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
          Active Terms ({activeSemesters.length})
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {activeSemesters.map((sem) => {
            const isSelected = selectedSemesterId === sem.id;
            const semSubjects = subjects.filter((s) => s.semesterId === sem.id);

            return (
              <div
                key={sem.id}
                className={`p-5 rounded-3xl border transition-all ${
                  isSelected
                    ? 'border-indigo-500 ring-2 ring-indigo-500/20 bg-indigo-50/20'
                    : 'border-slate-200/80 bg-white hover:border-slate-300'
                }`}
              >
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-base font-extrabold text-slate-900">{sem.name}</h4>
                      {isSelected && (
                        <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-700">
                          Current Selected
                        </span>
                      )}
                    </div>
                    <span className="text-xs text-slate-500 font-medium">{sem.academicYear}</span>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => toggleSemesterArchive(sem)}
                      className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg transition-colors"
                      title="Archive semester"
                    >
                      <Archive className="w-4 h-4" />
                    </button>
                    {semesters.length > 1 && (
                      <button
                        onClick={() => deleteSemester(sem.id)}
                        className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg transition-colors"
                        title="Delete semester"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>

                <p className="text-xs text-slate-600 mb-4">{sem.notes || 'No description added.'}</p>

                <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs text-slate-500">
                  <span>{semSubjects.length} enrolled subjects</span>

                  {!isSelected && (
                    <button
                      onClick={() => selectSemester(sem.id)}
                      className="text-xs font-bold text-indigo-600 hover:underline"
                    >
                      Switch to Term &rarr;
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Archived Semesters */}
      {archivedSemesters.length > 0 && (
        <div className="space-y-3 pt-4 border-t border-slate-200">
          <h3 className="text-sm font-bold text-slate-500 uppercase tracking-wider">
            Archived Terms ({archivedSemesters.length})
          </h3>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {archivedSemesters.map((sem) => (
              <div
                key={sem.id}
                className="p-4 rounded-2xl border border-slate-200 bg-slate-50/50 flex items-center justify-between gap-3 text-slate-600"
              >
                <div>
                  <div className="text-sm font-bold text-slate-800">{sem.name}</div>
                  <span className="text-xs text-slate-500">{sem.academicYear} (Archived)</span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => toggleSemesterArchive(sem)}
                    className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 transition-colors"
                    title="Restore semester"
                  >
                    <ArchiveRestore className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => deleteSemester(sem.id)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 transition-colors"
                    title="Delete permanently"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Add Semester Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="text-base font-bold text-slate-900 mb-3">Add Academic Semester</h3>
            <form onSubmit={handleCreate} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Semester Name *</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Spring 2027"
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Academic Year</label>
                  <input
                    type="text"
                    value={year}
                    onChange={(e) => setYear(e.target.value)}
                    placeholder="2026-2027"
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Start Date</label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Term Notes / Goals</label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Target GPA, credits load, thesis/internship plans"
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
                  className="px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs"
                >
                  Create Term
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
