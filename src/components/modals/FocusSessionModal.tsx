import React, { useEffect, useState } from 'react';
import { X, Play, Pause, RotateCcw, CheckCircle2, Sparkles, BookOpen } from 'lucide-react';
import confetti from 'canvas-confetti';
import { usePlanner } from '../../context/PlannerContext';
import { Subject, Topic, TopicUnderstanding } from '../../types';

interface FocusSessionModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialTopic?: Topic | null;
  initialSubject?: Subject | null;
}

export const FocusSessionModal: React.FC<FocusSessionModalProps> = ({
  isOpen,
  onClose,
  initialTopic = null,
  initialSubject = null,
}) => {
  const { subjects, topics, recordFocusSession } = usePlanner();

  const [selectedSubjectId, setSelectedSubjectId] = useState<number | null>(
    initialSubject?.id || initialTopic?.subjectId || null
  );
  const [selectedTopicId, setSelectedTopicId] = useState<number | null>(
    initialTopic?.id || null
  );
  const [customTitle, setCustomTitle] = useState(initialTopic?.name || '');
  const [durationMinutes, setDurationMinutes] = useState(25);
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isActive, setIsActive] = useState(false);
  const [showFeedback, setShowFeedback] = useState(false);

  useEffect(() => {
    if (initialTopic) {
      setSelectedTopicId(initialTopic.id);
      setSelectedSubjectId(initialTopic.subjectId);
      setCustomTitle(initialTopic.name);
    } else if (initialSubject) {
      setSelectedSubjectId(initialSubject.id);
    }
  }, [initialTopic, initialSubject]);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isActive && timeLeft > 0) {
      interval = setInterval(() => {
        setTimeLeft((prev) => prev - 1);
      }, 1000);
    } else if (timeLeft === 0 && isActive) {
      setIsActive(false);
      confetti({ particleCount: 80, spread: 60 });
      setShowFeedback(true);
    }
    return () => clearInterval(interval);
  }, [isActive, timeLeft]);

  if (!isOpen) return null;

  const handleSelectPreset = (minutes: number) => {
    setDurationMinutes(minutes);
    setTimeLeft(minutes * 60);
    setIsActive(false);
  };

  const handleToggleTimer = () => {
    setIsActive(!isActive);
  };

  const handleResetTimer = () => {
    setIsActive(false);
    setTimeLeft(durationMinutes * 60);
  };

  const handleCompleteSession = () => {
    setIsActive(false);
    setShowFeedback(true);
  };

  const handleFeedbackSubmit = (understanding: TopicUnderstanding) => {
    const elapsedMinutes = Math.max(1, Math.round((durationMinutes * 60 - timeLeft) / 60));
    const titleToUse =
      customTitle.trim() ||
      topics.find((t) => t.id === selectedTopicId)?.name ||
      'Deep Focus Session';

    recordFocusSession(selectedSubjectId, selectedTopicId, titleToUse, elapsedMinutes, understanding);
    setShowFeedback(false);
    onClose();
    confetti({ particleCount: 50, spread: 70 });
  };

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const progressPercent = ((durationMinutes * 60 - timeLeft) / (durationMinutes * 60)) * 100;

  const subjectTopics = selectedSubjectId
    ? topics.filter((t) => t.subjectId === selectedSubjectId)
    : topics;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100 relative">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
        >
          <X className="w-5 h-5" />
        </button>

        {!showFeedback ? (
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
              <h3 className="text-lg font-bold text-slate-900">Deep Focus Mode</h3>
            </div>
            <p className="text-xs text-slate-500 mb-5">
              Eliminate distractions and absorb complex concepts with deliberate focus.
            </p>

            {/* Subject / Topic Select */}
            <div className="space-y-3 mb-6">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Subject (Optional)
                </label>
                <select
                  value={selectedSubjectId || ''}
                  onChange={(e) => {
                    const id = e.target.value ? Number(e.target.value) : null;
                    setSelectedSubjectId(id);
                    setSelectedTopicId(null);
                  }}
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50/50 focus:outline-hidden focus:ring-2 focus:ring-indigo-500 font-medium"
                >
                  <option value="">General Study Session</option>
                  {subjects.map((sub) => (
                    <option key={sub.id} value={sub.id}>
                      {sub.name} ({sub.courseCode || 'Course'})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Topic (Optional)
                </label>
                <select
                  value={selectedTopicId || ''}
                  onChange={(e) => {
                    const id = e.target.value ? Number(e.target.value) : null;
                    setSelectedTopicId(id);
                    if (id) {
                      const t = topics.find((item) => item.id === id);
                      if (t) setCustomTitle(t.name);
                    }
                  }}
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50/50 focus:outline-hidden focus:ring-2 focus:ring-indigo-500 font-medium"
                >
                  <option value="">Select a specific topic...</option>
                  {subjectTopics.map((topic) => (
                    <option key={topic.id} value={topic.id}>
                      {topic.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Session Goal / Title
                </label>
                <input
                  type="text"
                  value={customTitle}
                  onChange={(e) => setCustomTitle(e.target.value)}
                  placeholder="e.g. Solve 5 AVL rotation problems"
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>

            {/* Duration Presets */}
            <div className="flex items-center justify-center gap-2 mb-6">
              {[15, 25, 45, 60].map((min) => (
                <button
                  key={min}
                  onClick={() => handleSelectPreset(min)}
                  className={`px-3 py-1.5 text-xs font-bold rounded-xl transition-all ${
                    durationMinutes === min
                      ? 'bg-indigo-600 text-white shadow-xs'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {min} min
                </button>
              ))}
            </div>

            {/* Timer Display */}
            <div className="relative flex flex-col items-center justify-center mb-6">
              <div className="w-48 h-48 rounded-full border-4 border-slate-100 flex flex-col items-center justify-center relative overflow-hidden bg-gradient-to-b from-indigo-50/50 to-transparent">
                <div
                  className="absolute bottom-0 left-0 right-0 bg-indigo-500/10 transition-all duration-1000"
                  style={{ height: `${progressPercent}%` }}
                />
                <span className="text-4xl font-extrabold tracking-tight text-slate-900 font-mono relative z-10">
                  {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
                </span>
                <span className="text-xs font-semibold text-indigo-600 mt-1 uppercase tracking-wider relative z-10">
                  {isActive ? 'In Flow State' : 'Paused'}
                </span>
              </div>
            </div>

            {/* Controls */}
            <div className="flex items-center justify-center gap-3">
              <button
                onClick={handleResetTimer}
                title="Reset timer"
                className="p-3 rounded-2xl bg-slate-100 text-slate-600 hover:bg-slate-200 transition-colors"
              >
                <RotateCcw className="w-5 h-5" />
              </button>
              <button
                onClick={handleToggleTimer}
                className={`flex-1 flex items-center justify-center gap-2 py-3 px-6 rounded-2xl font-bold text-white transition-all shadow-md ${
                  isActive
                    ? 'bg-amber-600 hover:bg-amber-700 shadow-amber-200'
                    : 'bg-indigo-600 hover:bg-indigo-700 shadow-indigo-200'
                }`}
              >
                {isActive ? (
                  <>
                    <Pause className="w-5 h-5" /> Pause
                  </>
                ) : (
                  <>
                    <Play className="w-5 h-5 fill-current" /> Start Focus
                  </>
                )}
              </button>
              <button
                onClick={handleCompleteSession}
                title="Finish early"
                className="p-3 rounded-2xl bg-emerald-100 text-emerald-700 hover:bg-emerald-200 transition-colors"
              >
                <CheckCircle2 className="w-5 h-5" />
              </button>
            </div>
          </div>
        ) : (
          /* Feedback Prompt when session completes */
          <div className="text-center py-4">
            <div className="w-16 h-16 rounded-3xl bg-indigo-100 text-indigo-600 flex items-center justify-center mx-auto mb-4">
              <Sparkles className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-extrabold text-slate-900 mb-1">Session Complete!</h3>
            <p className="text-sm text-slate-600 mb-6">
              Great job staying focused. How do you feel about your understanding of this topic?
            </p>

            <div className="grid grid-cols-3 gap-3 mb-6">
              <button
                onClick={() => handleFeedbackSubmit('STRONG')}
                className="p-4 rounded-2xl border-2 border-emerald-200 hover:border-emerald-500 bg-emerald-50/50 hover:bg-emerald-50 text-emerald-800 flex flex-col items-center gap-1.5 transition-all group"
              >
                <span className="text-2xl group-hover:scale-110 transition-transform">💪</span>
                <span className="text-xs font-bold">Strong</span>
                <span className="text-[10px] text-emerald-600">Crystal clear</span>
              </button>

              <button
                onClick={() => handleFeedbackSubmit('OKAY')}
                className="p-4 rounded-2xl border-2 border-amber-200 hover:border-amber-500 bg-amber-50/50 hover:bg-amber-50 text-amber-800 flex flex-col items-center gap-1.5 transition-all group"
              >
                <span className="text-2xl group-hover:scale-110 transition-transform">👍</span>
                <span className="text-xs font-bold">Okay</span>
                <span className="text-[10px] text-amber-600">Got basics</span>
              </button>

              <button
                onClick={() => handleFeedbackSubmit('WEAK')}
                className="p-4 rounded-2xl border-2 border-rose-200 hover:border-rose-500 bg-rose-50/50 hover:bg-rose-50 text-rose-800 flex flex-col items-center gap-1.5 transition-all group"
              >
                <span className="text-2xl group-hover:scale-110 transition-transform">🤔</span>
                <span className="text-xs font-bold">Weak</span>
                <span className="text-[10px] text-rose-600">Need revision</span>
              </button>
            </div>

            <button
              onClick={() => handleFeedbackSubmit('OKAY')}
              className="text-xs text-slate-400 hover:text-slate-600"
            >
              Skip feedback & save session
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
