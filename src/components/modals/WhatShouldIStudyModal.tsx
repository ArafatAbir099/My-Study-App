import React from 'react';
import { X, Sparkles, Brain, ArrowRight, Play, BookCheck } from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { Topic } from '../../types';

interface WhatShouldIStudyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onStartFocus: (topic: Topic) => void;
}

export const WhatShouldIStudyModal: React.FC<WhatShouldIStudyModalProps> = ({
  isOpen,
  onClose,
  onStartFocus,
}) => {
  const { studyRecommendations, topics, subjects, completeTopic } = usePlanner();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-xl w-full p-6 shadow-2xl border border-slate-100 max-h-[90vh] flex flex-col relative">
        <div className="flex items-start justify-between gap-4 mb-4 pb-3 border-b border-slate-100 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0">
              <Brain className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h3 className="text-lg font-bold text-slate-900">What Should I Study?</h3>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                  AI Prioritized
                </span>
              </div>
              <p className="text-xs text-slate-500">
                Topics ranked dynamically by upcoming exam cycles, overdue revisions, and past paper question frequency.
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="overflow-y-auto space-y-3 pr-1 py-1 flex-1">
          {studyRecommendations.length === 0 ? (
            <div className="text-center py-12 text-slate-400">
              <Sparkles className="w-12 h-12 mx-auto mb-2 opacity-50" />
              <p className="text-sm font-medium">No pending topics need attention right now!</p>
              <p className="text-xs text-slate-400 mt-1">Add subjects or syllabus topics to receive smart recommendations.</p>
            </div>
          ) : (
            studyRecommendations.map((rec, index) => {
              const fullTopic = topics.find((t) => t.id === rec.topicId);
              const subject = fullTopic ? subjects.find((s) => s.id === fullTopic.subjectId) : null;

              return (
                <div
                  key={rec.topicId}
                  className="p-4 rounded-2xl border border-slate-200/80 bg-slate-50/50 hover:bg-white hover:border-indigo-300 hover:shadow-md transition-all group"
                >
                  <div className="flex items-start justify-between gap-3 mb-2">
                    <div className="flex items-center gap-2">
                      <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[11px] font-extrabold flex items-center justify-center shrink-0">
                        {index + 1}
                      </span>
                      <div>
                        <h4 className="text-sm font-bold text-slate-900 leading-snug">
                          {rec.topicName}
                        </h4>
                        {subject && (
                          <span className="text-[11px] font-medium text-indigo-700">
                            {subject.name} ({subject.courseCode || 'Course'})
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <span className="text-xs font-extrabold px-2 py-0.5 rounded-lg bg-indigo-100 text-indigo-800">
                        Score: {rec.priorityScore}
                      </span>
                    </div>
                  </div>

                  {/* Priority Reasons Tag Badges */}
                  <div className="flex flex-wrap items-center gap-1.5 mb-3">
                    {rec.priorityReason.split('•').map((reason, rIdx) => {
                      const trimmed = reason.trim();
                      const isRev = trimmed.includes('Revision');
                      const isWeak = trimmed.includes('Weak');
                      const isPyq = trimmed.includes('PYQ');

                      return (
                        <span
                          key={rIdx}
                          className={`text-[10px] font-semibold px-2 py-0.5 rounded-md ${
                            isRev
                              ? 'bg-rose-100 text-rose-700 border border-rose-200'
                              : isWeak
                              ? 'bg-amber-100 text-amber-700 border border-amber-200'
                              : isPyq
                              ? 'bg-purple-100 text-purple-700 border border-purple-200'
                              : 'bg-slate-200/70 text-slate-700'
                          }`}
                        >
                          {trimmed}
                        </span>
                      );
                    })}

                    {rec.appearanceCount > 0 && (
                      <span className="text-[10px] font-medium text-slate-500 ml-auto">
                        Appeared in {rec.appearanceCount} PYQs (Last: {rec.lastAppearance})
                      </span>
                    )}
                  </div>

                  {/* Quick Action Buttons */}
                  <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-200/50">
                    {fullTopic && fullTopic.status !== 'COMPLETED' && (
                      <button
                        onClick={() => {
                          completeTopic(fullTopic, 'STRONG');
                          onClose();
                        }}
                        className="px-2.5 py-1 text-xs font-semibold text-emerald-700 hover:bg-emerald-50 rounded-lg transition-colors flex items-center gap-1"
                      >
                        <BookCheck className="w-3.5 h-3.5" />
                        Mark Completed
                      </button>
                    )}

                    <button
                      onClick={() => {
                        if (fullTopic) {
                          onStartFocus(fullTopic);
                          onClose();
                        }
                      }}
                      className="px-3 py-1.5 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs transition-colors flex items-center gap-1.5"
                    >
                      <Play className="w-3.5 h-3.5 fill-current" />
                      Start Focus Session
                    </button>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
