import React, { useState } from 'react';
import {
  LayoutDashboard,
  CalendarDays,
  BookOpen,
  RotateCcw,
  CalendarCheck,
  HelpCircle,
  TrendingUp,
  Folder,
  School,
  Settings,
  Search,
  Plus,
  Play,
  MoreHorizontal,
  X,
  GraduationCap,
  Bot,
  Sparkles,
} from 'lucide-react';
import { usePlanner } from './context/PlannerContext';
import { NavigationSection, Topic, Subject } from './types';
import { AuthScreen } from './components/screens/AuthScreen';
import { DashboardScreen } from './components/screens/DashboardScreen';
import { CalendarScreen } from './components/screens/CalendarScreen';
import { SubjectsScreen } from './components/screens/SubjectsScreen';
import { RevisionScreen } from './components/screens/RevisionScreen';
import { ExamsScreen } from './components/screens/ExamsScreen';
import { PYQScreen } from './components/screens/PYQScreen';
import { ProgressScreen } from './components/screens/ProgressScreen';
import { ResourcesScreen } from './components/screens/ResourcesScreen';
import { SemestersScreen } from './components/screens/SemestersScreen';
import { SettingsScreen } from './components/screens/SettingsScreen';
import { GlobalSearchModal } from './components/modals/GlobalSearchModal';
import { WhatShouldIStudyModal } from './components/modals/WhatShouldIStudyModal';
import { FocusSessionModal } from './components/modals/FocusSessionModal';
import { QuickAddModal } from './components/modals/QuickAddModal';
import { SyllabusUploadModal } from './components/modals/SyllabusUploadModal';
import { AIStudyAssistantModal } from './components/assistant/AIStudyAssistantModal';

export const App: React.FC = () => {
  const { currentUser, subjects } = usePlanner();

  const [currentSection, setCurrentSection] = useState<NavigationSection>('DASHBOARD');
  const [showSearch, setShowSearch] = useState(false);
  const [showQuickAdd, setShowQuickAdd] = useState(false);
  const [showWhatShouldIStudy, setShowWhatShouldIStudy] = useState(false);
  const [showMoreMobileMenu, setShowMoreMobileMenu] = useState(false);
  const [showAssistant, setShowAssistant] = useState(false);
  const [showSyllabusUpload, setShowSyllabusUpload] = useState(false);

  // Focus modal state
  const [showFocusModal, setShowFocusModal] = useState(false);
  const [focusTopic, setFocusTopic] = useState<Topic | null>(null);
  const [focusSubject, setFocusSubject] = useState<Subject | null>(null);

  if (!currentUser) {
    return <AuthScreen />;
  }

  const handleStartFocus = (topic?: Topic | null) => {
    if (topic) {
      setFocusTopic(topic);
      const sub = subjects.find((s) => s.id === topic.subjectId) || null;
      setFocusSubject(sub);
    } else {
      setFocusTopic(null);
      setFocusSubject(null);
    }
    setShowFocusModal(true);
  };

  const handleCalendarStartFocus = (
    title: string,
    duration: number,
    topicId?: number | null,
    subjectId?: number | null
  ) => {
    const sub = subjects.find((s) => s.id === subjectId) || null;
    setFocusTopic(null);
    setFocusSubject(sub);
    setShowFocusModal(true);
  };

  const navItems: { id: NavigationSection; label: string; icon: React.ReactNode }[] = [
    { id: 'DASHBOARD', label: 'Dashboard', icon: <LayoutDashboard className="w-5 h-5" /> },
    { id: 'CALENDAR', label: 'Calendar', icon: <CalendarDays className="w-5 h-5" /> },
    { id: 'SUBJECTS', label: 'Subjects', icon: <BookOpen className="w-5 h-5" /> },
    { id: 'REVISION', label: 'Revision', icon: <RotateCcw className="w-5 h-5" /> },
    { id: 'EXAMS', label: 'Exams', icon: <CalendarCheck className="w-5 h-5" /> },
    { id: 'PYQS', label: 'PYQs', icon: <HelpCircle className="w-5 h-5" /> },
    { id: 'PROGRESS', label: 'Progress', icon: <TrendingUp className="w-5 h-5" /> },
    { id: 'RESOURCES', label: 'Resources', icon: <Folder className="w-5 h-5" /> },
    { id: 'SEMESTERS', label: 'Semesters', icon: <School className="w-5 h-5" /> },
    { id: 'SETTINGS', label: 'Settings', icon: <Settings className="w-5 h-5" /> },
  ];

  const primaryMobileNav: NavigationSection[] = ['DASHBOARD', 'CALENDAR', 'SUBJECTS', 'REVISION'];

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col antialiased selection:bg-indigo-500 selection:text-white">
      {/* Top Navbar */}
      <header className="sticky top-0 z-40 bg-white/90 backdrop-blur-md border-b border-slate-200/80 px-4 sm:px-6 py-3">
        <div className="max-w-7xl mx-auto flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setCurrentSection('DASHBOARD')}
              className="flex items-center gap-2 text-left group"
            >
              <div className="w-9 h-9 rounded-xl bg-indigo-600 text-white flex items-center justify-center shadow-sm group-hover:scale-105 transition-transform">
                <GraduationCap className="w-5 h-5" />
              </div>
              <div>
                <span className="font-extrabold text-sm text-slate-900 tracking-tight block">
                  Semester Study OS
                </span>
                <span className="text-[10px] font-semibold text-indigo-600 block leading-none">
                  University Student Hub
                </span>
              </div>
            </button>
          </div>

          {/* Quick Header Actions */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowSearch(true)}
              className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs font-semibold transition-colors"
              title="Search everything"
            >
              <Search className="w-4 h-4 text-slate-500" />
              <span className="hidden sm:inline">Search...</span>
            </button>

            <button
              onClick={() => setShowAssistant(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-800 text-xs font-bold transition-colors border border-purple-200/60"
              title="AI Study Assistant"
            >
              <Bot className="w-4 h-4 text-purple-600" />
              <span className="hidden sm:inline">AI Study Assistant</span>
            </button>

            <button
              onClick={() => handleStartFocus(null)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-50 hover:bg-emerald-100 text-emerald-800 text-xs font-bold transition-colors"
              title="Focus Mode"
            >
              <Play className="w-3.5 h-3.5 fill-current" />
              <span className="hidden sm:inline">Focus</span>
            </button>

            <button
              onClick={() => setShowQuickAdd(true)}
              className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-xs transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Quick Add</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Body with Sidebar on Desktop */}
      <div className="max-w-7xl w-full mx-auto flex-1 flex px-4 sm:px-6 py-6 gap-6">
        {/* Desktop Navigation Sidebar */}
        <aside className="hidden lg:block w-56 shrink-0 space-y-1">
          <div className="p-2 mb-2 flex items-center justify-between">
            <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
              Workspaces
            </span>
          </div>

          {navItems.map((item) => {
            const isActive = currentSection === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setCurrentSection(item.id)}
                className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all ${
                  isActive
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-200'
                    : 'text-slate-600 hover:bg-slate-100/80 hover:text-slate-900'
                }`}
              >
                {item.icon}
                <span>{item.label}</span>
              </button>
            );
          })}

          <div className="pt-4 border-t border-slate-200/60 mt-4 space-y-2">
            <button
              onClick={() => setShowSyllabusUpload(true)}
              className="w-full flex items-center gap-2.5 px-3.5 py-2 rounded-xl text-xs font-bold text-purple-700 bg-purple-50 hover:bg-purple-100 transition-colors"
            >
              <Sparkles className="w-4 h-4 text-purple-600" />
              <span>Upload Syllabus (AI)</span>
            </button>
            <button
              onClick={() => setShowAssistant(true)}
              className="w-full flex items-center gap-2.5 px-3.5 py-2 rounded-xl text-xs font-bold text-slate-700 bg-slate-100 hover:bg-slate-200 transition-colors"
            >
              <Bot className="w-4 h-4 text-indigo-600" />
              <span>AI Study Chat</span>
            </button>
          </div>
        </aside>

        {/* Content Area */}
        <main className="flex-1 min-w-0 pb-20 lg:pb-8">
          {currentSection === 'DASHBOARD' && (
            <DashboardScreen
              onNavigate={setCurrentSection}
              onOpenWhatShouldIStudy={() => setShowWhatShouldIStudy(true)}
              onOpenFocusSession={handleStartFocus}
              onOpenQuickAdd={() => setShowQuickAdd(true)}
              onOpenSyllabusUpload={() => setShowSyllabusUpload(true)}
              onOpenAssistant={() => setShowAssistant(true)}
            />
          )}

          {currentSection === 'CALENDAR' && (
            <CalendarScreen onStartFocus={handleCalendarStartFocus} />
          )}

          {currentSection === 'SUBJECTS' && (
            <SubjectsScreen onStartFocus={handleStartFocus} />
          )}

          {currentSection === 'REVISION' && (
            <RevisionScreen onStartFocus={handleStartFocus} />
          )}

          {currentSection === 'EXAMS' && (
            <ExamsScreen onNavigate={setCurrentSection} />
          )}

          {currentSection === 'PYQS' && <PYQScreen />}

          {currentSection === 'PROGRESS' && <ProgressScreen />}

          {currentSection === 'RESOURCES' && <ResourcesScreen />}

          {currentSection === 'SEMESTERS' && <SemestersScreen />}

          {currentSection === 'SETTINGS' && <SettingsScreen />}
        </main>
      </div>

      {/* Floating AI Assistant Action Button */}
      <button
        onClick={() => setShowAssistant(true)}
        className="fixed right-6 bottom-20 lg:bottom-6 z-40 w-12 h-12 rounded-2xl bg-gradient-to-tr from-purple-600 to-indigo-600 text-white shadow-lg hover:shadow-xl hover:scale-105 transition-all flex items-center justify-center group"
        title="Open AI Study Assistant"
      >
        <Bot className="w-6 h-6" />
        <span className="sr-only">AI Study Assistant</span>
      </button>

      {/* Mobile Bottom Navigation Bar */}
      <nav className="lg:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200/80 px-2 py-1.5 flex items-center justify-around shadow-lg">
        {primaryMobileNav.map((secId) => {
          const item = navItems.find((n) => n.id === secId)!;
          const isActive = currentSection === secId;

          return (
            <button
              key={secId}
              onClick={() => setCurrentSection(secId)}
              className={`flex flex-col items-center py-1 px-3 rounded-xl transition-all ${
                isActive ? 'text-indigo-600 font-extrabold' : 'text-slate-500 font-medium'
              }`}
            >
              {item.icon}
              <span className="text-[10px] mt-0.5">{item.label}</span>
            </button>
          );
        })}

        <button
          onClick={() => setShowMoreMobileMenu(true)}
          className={`flex flex-col items-center py-1 px-3 rounded-xl transition-all ${
            !primaryMobileNav.includes(currentSection)
              ? 'text-indigo-600 font-extrabold'
              : 'text-slate-500 font-medium'
          }`}
        >
          <MoreHorizontal className="w-5 h-5" />
          <span className="text-[10px] mt-0.5">More</span>
        </button>
      </nav>

      {/* Mobile "More" Drawer Modal */}
      {showMoreMobileMenu && (
        <div className="lg:hidden fixed inset-0 z-50 flex items-end justify-center bg-slate-900/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-t-3xl w-full max-w-lg p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="font-extrabold text-slate-900 text-sm">Additional Workspaces</h3>
              <button
                onClick={() => setShowMoreMobileMenu(false)}
                className="p-1 rounded-full text-slate-400 hover:text-slate-600"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2">
              {navItems
                .filter((item) => !primaryMobileNav.includes(item.id))
                .map((item) => (
                  <button
                    key={item.id}
                    onClick={() => {
                      setCurrentSection(item.id);
                      setShowMoreMobileMenu(false);
                    }}
                    className={`flex items-center gap-2.5 p-3 rounded-2xl border text-xs font-bold transition-all text-left ${
                      currentSection === item.id
                        ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
                        : 'bg-slate-50 border-slate-200 text-slate-700 hover:bg-slate-100'
                    }`}
                  >
                    {item.icon}
                    <span>{item.label}</span>
                  </button>
                ))}
            </div>
          </div>
        </div>
      )}

      {/* Global Modals */}
      <GlobalSearchModal
        isOpen={showSearch}
        onClose={() => setShowSearch(false)}
        onNavigate={setCurrentSection}
      />

      <WhatShouldIStudyModal
        isOpen={showWhatShouldIStudy}
        onClose={() => setShowWhatShouldIStudy(false)}
        onStartFocus={handleStartFocus}
      />

      <FocusSessionModal
        isOpen={showFocusModal}
        onClose={() => setShowFocusModal(false)}
        initialTopic={focusTopic}
        initialSubject={focusSubject}
      />

      <QuickAddModal
        isOpen={showQuickAdd}
        onClose={() => setShowQuickAdd(false)}
      />

      <SyllabusUploadModal
        isOpen={showSyllabusUpload}
        onClose={() => setShowSyllabusUpload(false)}
      />

      <AIStudyAssistantModal
        isOpen={showAssistant}
        onClose={() => setShowAssistant(false)}
      />
    </div>
  );
};
