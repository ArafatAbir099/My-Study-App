import React, { useState } from 'react';
import {
  Settings,
  User,
  GraduationCap,
  Mail,
  Users,
  RotateCcw,
  LogOut,
  Save,
  CheckCircle2,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';

export const SettingsScreen: React.FC = () => {
  const {
    currentUser,
    allUsers,
    updateProfile,
    switchUser,
    resetUserData,
    logout,
  } = usePlanner();

  const [name, setName] = useState(currentUser?.name || '');
  const [email, setEmail] = useState(currentUser?.email || '');
  const [university, setUniversity] = useState(currentUser?.university || '');
  const [studentId, setStudentId] = useState(currentUser?.studentId || '');
  const [saveNotice, setSaveNotice] = useState(false);
  const [showResetConfirm, setShowResetConfirm] = useState(false);

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    updateProfile(name, email, university, studentId);
    setSaveNotice(true);
    setTimeout(() => setSaveNotice(false), 2000);
  };

  return (
    <div className="space-y-6 max-w-2xl mx-auto">
      {/* Title */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Settings className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-extrabold text-slate-900">Student Profile & Settings</h2>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Manage your personal academic identity and workspace preferences.
          </p>
        </div>
      </div>

      {/* Profile Card */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs">
        <div className="flex items-center gap-4 pb-6 border-b border-slate-100">
          <div
            className="w-16 h-16 rounded-2xl flex items-center justify-center text-white text-2xl font-black shadow-md"
            style={{ backgroundColor: currentUser?.avatarColor || '#1E40AF' }}
          >
            {currentUser?.name?.charAt(0).toUpperCase() || 'S'}
          </div>
          <div>
            <h3 className="text-lg font-extrabold text-slate-900">{currentUser?.name}</h3>
            <p className="text-xs text-slate-500">{currentUser?.university}</p>
            <div className="text-[11px] text-slate-400 mt-0.5">
              ID: {currentUser?.studentId || 'Not set'} • {currentUser?.email}
            </div>
          </div>
        </div>

        {saveNotice && (
          <div className="my-4 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold rounded-xl flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Profile updated successfully!
          </div>
        )}

        <form onSubmit={handleSaveProfile} className="space-y-4 mt-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Full Name</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Student Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">University / Institute</label>
              <input
                type="text"
                value={university}
                onChange={(e) => setUniversity(e.target.value)}
                className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">Student ID / Roll No</label>
              <input
                type="text"
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
              />
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center gap-1.5"
            >
              <Save className="w-4 h-4" /> Save Profile
            </button>
          </div>
        </form>
      </div>

      {/* Switch Account (Multi-user demo support) */}
      {allUsers.length > 1 && (
        <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-3">
          <h4 className="text-sm font-bold text-slate-900 flex items-center gap-2">
            <Users className="w-4 h-4 text-indigo-600" /> Switch Student Profile
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {allUsers.map((u) => (
              <button
                key={u.id}
                onClick={() => switchUser(u)}
                className={`p-3 rounded-2xl border text-left transition-all flex items-center gap-3 ${
                  currentUser?.id === u.id
                    ? 'border-indigo-500 bg-indigo-50/30'
                    : 'border-slate-200 hover:bg-slate-50'
                }`}
              >
                <div
                  className="w-8 h-8 rounded-xl flex items-center justify-center text-white text-xs font-bold"
                  style={{ backgroundColor: u.avatarColor || '#2563EB' }}
                >
                  {u.name.charAt(0)}
                </div>
                <div>
                  <div className="text-xs font-bold text-slate-900">{u.name}</div>
                  <div className="text-[10px] text-slate-400">{u.email}</div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Account Operations */}
      <div className="bg-white rounded-3xl p-6 border border-slate-200/80 shadow-xs space-y-4">
        <h4 className="text-sm font-bold text-slate-900">Workspace Management</h4>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 rounded-2xl border border-rose-100 bg-rose-50/30">
          <div>
            <div className="text-xs font-bold text-rose-900">Reset Semester Data</div>
            <p className="text-[11px] text-slate-500 mt-0.5">
              Clears current courses, topics, and revisions to start a clean semester.
            </p>
          </div>
          <button
            onClick={() => setShowResetConfirm(true)}
            className="px-3.5 py-1.5 rounded-xl border border-rose-300 text-rose-700 hover:bg-rose-100 font-bold text-xs transition-colors self-start sm:self-auto shrink-0"
          >
            Reset Data
          </button>
        </div>

        <div className="pt-2 flex justify-end">
          <button
            onClick={logout}
            className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs transition-colors flex items-center gap-1.5"
          >
            <LogOut className="w-4 h-4" /> Sign Out
          </button>
        </div>
      </div>

      {/* Reset Confirmation Modal */}
      {showResetConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 text-center">
            <h3 className="text-base font-bold text-slate-900 mb-2">Are you sure?</h3>
            <p className="text-xs text-slate-500 mb-5">
              This will remove all current semester data (subjects, topics, revisions, exams, and notes) and seed an empty workspace.
            </p>
            <div className="flex items-center justify-center gap-3">
              <button
                onClick={() => setShowResetConfirm(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  resetUserData();
                  setShowResetConfirm(false);
                }}
                className="px-4 py-2 text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white rounded-xl shadow-xs"
              >
                Yes, Reset All
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
