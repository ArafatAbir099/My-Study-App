import React, { useState } from 'react';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Plus,
  Clock,
  CheckCircle2,
  Play,
  Trash2,
  CalendarDays,
  Filter,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { CalendarTask, TaskType } from '../../types';
import { getTodayStr, formatDateStr, addDays, formatReadableDate } from '../../util/dateUtils';

interface CalendarScreenProps {
  onStartFocus: (taskTitle: string, duration: number, topicId?: number | null, subjectId?: number | null) => void;
}

export const CalendarScreen: React.FC<CalendarScreenProps> = ({ onStartFocus }) => {
  const {
    tasks,
    subjects,
    topics,
    addTask,
    toggleTask,
    deleteTask,
    rescheduleTask,
    checkAndRescheduleMissedTasks,
  } = usePlanner();

  const [selectedDate, setSelectedDate] = useState(getTodayStr());
  const [viewMode, setViewMode] = useState<'DAY' | 'WEEK'>('DAY');
  const [subjectFilter, setSubjectFilter] = useState<number | 'ALL'>('ALL');
  const [showAddTaskModal, setShowAddTaskModal] = useState(false);

  // New task form
  const [newTitle, setNewTitle] = useState('');
  const [newSubId, setNewSubId] = useState<number | ''>('');
  const [newStartTime, setNewStartTime] = useState('10:00');
  const [newDuration, setNewDuration] = useState(60);
  const [newTaskType, setNewTaskType] = useState<TaskType>('STUDY');
  const [newNotes, setNewNotes] = useState('');

  // Reschedule state
  const [reschedulingTask, setReschedulingTask] = useState<CalendarTask | null>(null);
  const [newRescheduleDate, setNewRescheduleDate] = useState('');

  const todayStr = getTodayStr();

  // Calculate week days
  const weekDays = React.useMemo(() => {
    const [y, m, d] = selectedDate.split('-').map(Number);
    const curr = new Date(y, m - 1, d);
    const firstDay = new Date(curr);
    firstDay.setDate(curr.getDate() - curr.getDay()); // Sunday

    const days: string[] = [];
    for (let i = 0; i < 7; i++) {
      const next = new Date(firstDay);
      next.setDate(firstDay.getDate() + i);
      days.push(formatDateStr(next));
    }
    return days;
  }, [selectedDate]);

  const filteredTasks = tasks.filter((t) => {
    if (subjectFilter !== 'ALL' && t.subjectId !== subjectFilter) return false;
    if (viewMode === 'DAY') return t.date === selectedDate;
    return weekDays.includes(t.date);
  });

  const handlePrev = () => {
    if (viewMode === 'DAY') {
      setSelectedDate(addDays(selectedDate, -1));
    } else {
      setSelectedDate(addDays(selectedDate, -7));
    }
  };

  const handleNext = () => {
    if (viewMode === 'DAY') {
      setSelectedDate(addDays(selectedDate, 1));
    } else {
      setSelectedDate(addDays(selectedDate, 7));
    }
  };

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle) return;
    const endH = Math.floor(newDuration / 60);
    const startH = parseInt(newStartTime.split(':')[0], 10);
    const endStr = `${String((startH + endH) % 24).padStart(2, '0')}:00`;

    addTask(
      newTitle,
      newSubId ? Number(newSubId) : null,
      null,
      selectedDate,
      newStartTime,
      endStr,
      newDuration,
      newNotes,
      newTaskType
    );

    setNewTitle('');
    setNewNotes('');
    setShowAddTaskModal(false);
  };

  const handleConfirmReschedule = () => {
    if (reschedulingTask && newRescheduleDate) {
      rescheduleTask(reschedulingTask, newRescheduleDate);
      setReschedulingTask(null);
    }
  };

  return (
    <div className="space-y-5">
      {/* Top Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <h2 className="text-xl font-extrabold text-slate-900">Study Calendar</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Organize study blocks, revision milestones, and upcoming exams.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Day / Week Toggle */}
          <div className="flex bg-slate-100 p-1 rounded-xl">
            <button
              onClick={() => setViewMode('DAY')}
              className={`px-3 py-1 text-xs font-bold rounded-lg transition-all ${
                viewMode === 'DAY' ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600'
              }`}
            >
              Day
            </button>
            <button
              onClick={() => setViewMode('WEEK')}
              className={`px-3 py-1 text-xs font-bold rounded-lg transition-all ${
                viewMode === 'WEEK' ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600'
              }`}
            >
              Week
            </button>
          </div>

          {/* Subject Filter */}
          <select
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
            className="text-xs px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 font-medium text-slate-700 focus:outline-hidden"
          >
            <option value="ALL">All Subjects</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>

          {/* New Task Button */}
          <button
            onClick={() => setShowAddTaskModal(true)}
            className="px-3.5 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-xs transition-all flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" /> Add Task
          </button>
        </div>
      </div>

      {/* Date Navigation Strip */}
      <div className="bg-white p-3.5 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            onClick={handlePrev}
            className="p-1.5 rounded-xl hover:bg-slate-100 text-slate-600 transition-colors"
          >
            <ChevronLeft className="w-5 h-5" />
          </button>
          <button
            onClick={() => setSelectedDate(todayStr)}
            className="px-3 py-1 rounded-lg text-xs font-bold bg-slate-100 hover:bg-slate-200 text-slate-700"
          >
            Today
          </button>
          <button
            onClick={handleNext}
            className="p-1.5 rounded-xl hover:bg-slate-100 text-slate-600 transition-colors"
          >
            <ChevronRight className="w-5 h-5" />
          </button>
          <span className="text-sm font-extrabold text-slate-900 ml-2">
            {formatReadableDate(selectedDate)}
          </span>
        </div>

        {/* Quick Date Picker */}
        <input
          type="date"
          value={selectedDate}
          onChange={(e) => setSelectedDate(e.target.value)}
          className="text-xs px-2.5 py-1 rounded-lg border border-slate-200 bg-slate-50 text-slate-700"
        />
      </div>

      {/* Week Day Pills if in Week Mode */}
      {viewMode === 'WEEK' && (
        <div className="grid grid-cols-7 gap-2">
          {weekDays.map((dateStr) => {
            const [y, m, d] = dateStr.split('-').map(Number);
            const dt = new Date(y, m - 1, d);
            const dayName = dt.toLocaleDateString('en-US', { weekday: 'short' });
            const dayNum = dt.getDate();
            const count = tasks.filter((t) => t.date === dateStr).length;
            const isSelected = selectedDate === dateStr;
            const isToday = dateStr === todayStr;

            return (
              <button
                key={dateStr}
                onClick={() => setSelectedDate(dateStr)}
                className={`p-3 rounded-2xl border text-center transition-all ${
                  isSelected
                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-md'
                    : isToday
                    ? 'bg-indigo-50 border-indigo-200 text-indigo-900'
                    : 'bg-white border-slate-200 hover:border-slate-300 text-slate-700'
                }`}
              >
                <div className="text-[11px] font-semibold opacity-80">{dayName}</div>
                <div className="text-base font-extrabold my-0.5">{dayNum}</div>
                <div className={`text-[10px] font-bold ${isSelected ? 'text-indigo-100' : 'text-slate-400'}`}>
                  {count} tasks
                </div>
              </button>
            );
          })}
        </div>
      )}

      {/* Task List */}
      <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-xs">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-bold text-slate-900 text-base">
            Tasks for {formatReadableDate(selectedDate)}
          </h3>
          <span className="text-xs text-slate-500 font-semibold">
            {filteredTasks.length} scheduled
          </span>
        </div>

        {filteredTasks.length === 0 ? (
          <div className="py-12 text-center text-slate-400">
            <CalendarDays className="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p className="text-sm font-semibold">No study tasks scheduled for this day.</p>
            <p className="text-xs text-slate-400 mt-1">Click &quot;Add Task&quot; above to schedule a block.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredTasks.map((task) => {
              const subject = subjects.find((s) => s.id === task.subjectId);

              return (
                <div
                  key={task.id}
                  className={`p-4 rounded-xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                    task.isCompleted
                      ? 'bg-slate-50 border-slate-200 opacity-60'
                      : 'bg-white border-slate-200/80 hover:border-indigo-300'
                  }`}
                >
                  <div className="flex items-start gap-3">
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

                    <div>
                      <div className="flex items-center gap-2">
                        <span
                          className={`text-sm font-bold ${
                            task.isCompleted ? 'line-through text-slate-500' : 'text-slate-900'
                          }`}
                        >
                          {task.title}
                        </span>
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            task.taskType === 'REVISION'
                              ? 'bg-amber-100 text-amber-800'
                              : task.taskType === 'EXAM'
                              ? 'bg-rose-100 text-rose-800'
                              : 'bg-blue-100 text-blue-800'
                          }`}
                        >
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
                          {task.startTime} ({task.durationMinutes} min)
                        </span>
                        {task.notes && (
                          <span className="text-slate-400">{task.notes}</span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 self-end sm:self-center">
                    {!task.isCompleted && (
                      <button
                        onClick={() =>
                          onStartFocus(
                            task.title,
                            task.durationMinutes,
                            task.topicId,
                            task.subjectId
                          )
                        }
                        className="px-2.5 py-1 text-xs font-bold rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors flex items-center gap-1"
                      >
                        <Play className="w-3 h-3 fill-indigo-700" /> Focus
                      </button>
                    )}

                    <button
                      onClick={() => {
                        setReschedulingTask(task);
                        setNewRescheduleDate(task.date);
                      }}
                      className="text-xs font-semibold px-2.5 py-1 rounded-lg text-slate-600 hover:bg-slate-100 transition-colors"
                    >
                      Reschedule
                    </button>

                    <button
                      onClick={() => deleteTask(task.id)}
                      className="p-1 rounded-lg text-slate-400 hover:text-rose-600 transition-colors"
                      title="Delete task"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Add Task Modal */}
      {showAddTaskModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="text-lg font-bold text-slate-900 mb-4">Add Study Task</h3>
            <form onSubmit={handleCreateTask} className="space-y-3.5">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Title *</label>
                <input
                  type="text"
                  required
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  placeholder="e.g. Master Monotonic Queue & Stack"
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Subject</label>
                  <select
                    value={newSubId}
                    onChange={(e) => setNewSubId(e.target.value ? Number(e.target.value) : '')}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    <option value="">General</option>
                    {subjects.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Type</label>
                  <select
                    value={newTaskType}
                    onChange={(e) => setNewTaskType(e.target.value as TaskType)}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  >
                    <option value="STUDY">Study</option>
                    <option value="REVISION">Revision</option>
                    <option value="EXAM_PREP">Exam Prep</option>
                    <option value="EXAM">Exam</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Start Time</label>
                  <input
                    type="time"
                    value={newStartTime}
                    onChange={(e) => setNewStartTime(e.target.value)}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Duration (Min)</label>
                  <input
                    type="number"
                    value={newDuration}
                    onChange={(e) => setNewDuration(Number(e.target.value))}
                    className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Notes</label>
                <input
                  type="text"
                  value={newNotes}
                  onChange={(e) => setNewNotes(e.target.value)}
                  placeholder="Goals, links, questions to solve"
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddTaskModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs"
                >
                  Add Task
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reschedule Modal */}
      {reschedulingTask && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="text-base font-bold text-slate-900 mb-2">Reschedule Task</h3>
            <p className="text-xs text-slate-500 mb-4">{reschedulingTask.title}</p>

            <label className="block text-xs font-semibold text-slate-600 mb-1">Target Date</label>
            <input
              type="date"
              value={newRescheduleDate}
              onChange={(e) => setNewRescheduleDate(e.target.value)}
              className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden mb-5"
            />

            <div className="flex items-center justify-end gap-2">
              <button
                onClick={() => setReschedulingTask(null)}
                className="px-3.5 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                onClick={handleConfirmReschedule}
                className="px-4 py-1.5 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs"
              >
                Update Date
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
