import React, { useState } from 'react';
import { Flame, Trophy, Calendar as CalendarIcon, Info } from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { DayActivitySummary } from '../../types';
import { formatReadableDate } from '../../util/dateUtils';

export const StudyActivityHeatmapCard: React.FC = () => {
  const { heatmapDays, heatmapFilter, setHeatmapFilter, streakStats } = usePlanner();
  const [selectedDay, setSelectedDay] = useState<DayActivitySummary | null>(null);

  // Group days into columns of 7 (weeks)
  const weeks: DayActivitySummary[][] = [];
  let currentWeek: DayActivitySummary[] = [];

  heatmapDays.forEach((day, index) => {
    currentWeek.push(day);
    if (currentWeek.length === 7 || index === heatmapDays.length - 1) {
      weeks.push(currentWeek);
      currentWeek = [];
    }
  });

  const getIntensityColor = (level: number): string => {
    switch (level) {
      case 1:
        return 'bg-emerald-200 border-emerald-300';
      case 2:
        return 'bg-emerald-400 border-emerald-500';
      case 3:
        return 'bg-emerald-600 border-emerald-700';
      case 4:
        return 'bg-emerald-800 border-emerald-900';
      default:
        return 'bg-slate-100 border-slate-200 hover:border-slate-300';
    }
  };

  return (
    <div className="bg-white rounded-2xl border border-slate-200/80 p-5 shadow-sm">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="font-bold text-slate-900 text-base">Study Activity Heatmap</h3>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
              Live Tracker
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Consistent daily revision, completed tasks, and focus sessions build mastery.
          </p>
        </div>

        {/* Range Filter Buttons */}
        <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-xl self-start sm:self-auto">
          {(['3_MONTHS', '6_MONTHS', '1_YEAR'] as const).map((filter) => (
            <button
              key={filter}
              onClick={() => setHeatmapFilter(filter)}
              className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition-all ${
                heatmapFilter === filter
                  ? 'bg-white text-indigo-700 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {filter === '3_MONTHS' ? '3 Mo' : filter === '6_MONTHS' ? '6 Mo' : '1 Year'}
            </button>
          ))}
        </div>
      </div>

      {/* Streaks Banner */}
      <div className="grid grid-cols-2 gap-3 mb-4 p-3 bg-slate-50/80 rounded-xl border border-slate-200/60">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-orange-100 text-orange-600 flex items-center justify-center shrink-0">
            <Flame className="w-5 h-5 fill-orange-500" />
          </div>
          <div>
            <div className="text-xs font-medium text-slate-500">Current Streak</div>
            <div className="text-base font-extrabold text-slate-900">
              {streakStats.currentStreak}{' '}
              <span className="text-xs font-normal text-slate-500">
                {streakStats.currentStreak === 1 ? 'day' : 'days'}
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-amber-100 text-amber-600 flex items-center justify-center shrink-0">
            <Trophy className="w-5 h-5 fill-amber-500" />
          </div>
          <div>
            <div className="text-xs font-medium text-slate-500">Longest Streak</div>
            <div className="text-base font-extrabold text-slate-900">
              {streakStats.longestStreak}{' '}
              <span className="text-xs font-normal text-slate-500">
                {streakStats.longestStreak === 1 ? 'day' : 'days'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* The Heatmap Grid */}
      <div className="overflow-x-auto pb-2 -mx-1 px-1">
        <div className="flex gap-1.5 min-w-max items-end">
          {weeks.map((week, wIdx) => (
            <div key={wIdx} className="flex flex-col gap-1.5">
              {week.map((day) => (
                <button
                  key={day.dateStr}
                  onClick={() => setSelectedDay(day)}
                  title={`${day.dateStr}: ${day.totalMinutes} min (${day.count} activities)`}
                  className={`w-3.5 h-3.5 rounded-[3px] border transition-transform hover:scale-125 focus:outline-hidden ${getIntensityColor(
                    day.intensityLevel
                  )} ${selectedDay?.dateStr === day.dateStr ? 'ring-2 ring-indigo-500 ring-offset-1' : ''}`}
                />
              ))}
            </div>
          ))}
        </div>
      </div>

      {/* Legend & Details */}
      <div className="flex flex-wrap items-center justify-between gap-2 mt-3 pt-3 border-t border-slate-100 text-xs text-slate-500">
        <div className="flex items-center gap-1.5">
          <span>Less</span>
          <span className="w-3 h-3 rounded-[2px] bg-slate-100 border border-slate-200" />
          <span className="w-3 h-3 rounded-[2px] bg-emerald-200 border border-emerald-300" />
          <span className="w-3 h-3 rounded-[2px] bg-emerald-400 border border-emerald-500" />
          <span className="w-3 h-3 rounded-[2px] bg-emerald-600 border border-emerald-700" />
          <span className="w-3 h-3 rounded-[2px] bg-emerald-800 border border-emerald-900" />
          <span>More</span>
        </div>

        {selectedDay && (
          <div className="flex items-center gap-2 font-medium text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
            <CalendarIcon className="w-3.5 h-3.5 text-indigo-600" />
            <span>{formatReadableDate(selectedDay.dateStr)}:</span>
            <span className="text-emerald-700 font-bold">
              {selectedDay.totalMinutes}m studied
            </span>
            <span className="text-slate-400">({selectedDay.count} sessions)</span>
          </div>
        )}
      </div>

      {/* Selected Day Popover Breakdown */}
      {selectedDay && selectedDay.count > 0 && (
        <div className="mt-3 p-3 bg-slate-50 rounded-xl border border-slate-200/60 text-xs text-slate-700 grid grid-cols-2 sm:grid-cols-4 gap-2">
          <div>
            <span className="text-slate-400 block">Tasks Completed:</span>
            <span className="font-bold text-slate-900">{selectedDay.tasksCompleted}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Revisions Done:</span>
            <span className="font-bold text-slate-900">{selectedDay.revisionsCompleted}</span>
          </div>
          <div>
            <span className="text-slate-400 block">PYQs Solved:</span>
            <span className="font-bold text-slate-900">{selectedDay.pyqsPracticed}</span>
          </div>
          <div>
            <span className="text-slate-400 block">Focus Sessions:</span>
            <span className="font-bold text-slate-900">{selectedDay.focusSessions}</span>
          </div>
        </div>
      )}
    </div>
  );
};
