import React, { useState } from 'react';
import {
  Folder,
  FileText,
  Link2,
  Bookmark,
  Plus,
  Trash2,
  ExternalLink,
  Search,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { StudyNote, StudyResource } from '../../types';

export const ResourcesScreen: React.FC = () => {
  const {
    resources,
    notes,
    subjects,
    addResource,
    deleteResource,
    toggleBookmarkResource,
    addNote,
    deleteNote,
    toggleBookmarkNote,
  } = usePlanner();

  const [activeTab, setActiveTab] = useState<'RESOURCES' | 'NOTES'>('RESOURCES');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | 'ALL'>('ALL');
  const [search, setSearch] = useState('');

  const [showAddResourceModal, setShowAddResourceModal] = useState(false);
  const [showAddNoteModal, setShowAddNoteModal] = useState(false);

  // New Resource Form
  const [resTitle, setResTitle] = useState('');
  const [resType, setResType] = useState<StudyResource['resourceType']>('LINK');
  const [resContent, setResContent] = useState('');
  const [resSubId, setResSubId] = useState<number | ''>('');

  // New Note Form
  const [noteTitle, setNoteTitle] = useState('');
  const [noteContent, setNoteContent] = useState('');
  const [noteSubId, setNoteSubId] = useState<number | ''>('');

  const filteredResources = resources.filter((r) => {
    if (selectedSubjectId !== 'ALL' && r.subjectId !== selectedSubjectId) return false;
    if (search && !r.title.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const filteredNotes = notes.filter((n) => {
    if (selectedSubjectId !== 'ALL' && n.subjectId !== selectedSubjectId) return false;
    if (search && !n.title.toLowerCase().includes(search.toLowerCase()) && !n.content.toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });

  const handleCreateResource = (e: React.FormEvent) => {
    e.preventDefault();
    if (!resTitle) return;
    addResource(
      resSubId ? Number(resSubId) : null,
      null,
      resTitle,
      resType,
      resContent
    );
    setResTitle('');
    setResContent('');
    setShowAddResourceModal(false);
  };

  const handleCreateNote = (e: React.FormEvent) => {
    e.preventDefault();
    if (!noteTitle) return;
    addNote(
      noteSubId ? Number(noteSubId) : null,
      null,
      noteTitle,
      noteContent
    );
    setNoteTitle('');
    setNoteContent('');
    setShowAddNoteModal(false);
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <h2 className="text-xl font-extrabold text-slate-900">Academic Resources & Notes</h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Store lecture slides, cheatsheets, formulas, and external references.
          </p>
        </div>

        <button
          onClick={() => (activeTab === 'RESOURCES' ? setShowAddResourceModal(true) : setShowAddNoteModal(true))}
          className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs shadow-xs transition-colors flex items-center gap-1.5 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" /> {activeTab === 'RESOURCES' ? 'Add Resource' : 'New Note'}
        </button>
      </div>

      {/* Tabs and Filter Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-3 rounded-2xl border border-slate-200/80 shadow-xs">
        <div className="flex bg-slate-100 p-1 rounded-xl">
          <button
            onClick={() => setActiveTab('RESOURCES')}
            className={`px-4 py-1.5 text-xs font-bold rounded-lg transition-all ${
              activeTab === 'RESOURCES' ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600'
            }`}
          >
            Lecture Resources ({resources.length})
          </button>
          <button
            onClick={() => setActiveTab('NOTES')}
            className={`px-4 py-1.5 text-xs font-bold rounded-lg transition-all ${
              activeTab === 'NOTES' ? 'bg-white text-indigo-700 shadow-xs' : 'text-slate-600'
            }`}
          >
            Study Notes ({notes.length})
          </button>
        </div>

        <div className="flex items-center gap-2">
          {/* Subject Filter */}
          <select
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value))}
            className="text-xs px-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 font-medium text-slate-700"
          >
            <option value="ALL">All Subjects</option>
            {subjects.map((s) => (
              <option key={s.id} value={s.id}>{s.name}</option>
            ))}
          </select>

          {/* Search */}
          <div className="relative">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-2.5 top-2.5" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search..."
              className="text-xs pl-8 pr-3 py-1.5 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
            />
          </div>
        </div>
      </div>

      {/* Content Rendering */}
      {activeTab === 'RESOURCES' ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredResources.length === 0 ? (
            <div className="col-span-2 bg-white rounded-3xl p-12 text-center text-slate-400 border border-slate-200 space-y-3">
              <Folder className="w-12 h-12 mx-auto text-indigo-400 opacity-60" />
              <h3 className="text-base font-bold text-slate-800">
                {resources.length === 0 ? 'No resources yet' : 'No resources matching filter'}
              </h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                {resources.length === 0
                  ? 'Add lecture notes or resources such as lecture slides, cheatsheets, formulas, and textbook references.'
                  : 'Try selecting a different subject filter.'}
              </p>
              <button
                onClick={() => setShowAddResourceModal(true)}
                className="mt-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
              >
                + Add Resource
              </button>
            </div>
          ) : (
            filteredResources.map((res) => {
              const sub = subjects.find((s) => s.id === res.subjectId);

              return (
                <div
                  key={res.id}
                  className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs flex flex-col justify-between hover:border-indigo-300 transition-all"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700">
                          {res.resourceType}
                        </span>
                        {sub && (
                          <span className="text-xs font-semibold text-slate-500">
                            {sub.name}
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => toggleBookmarkResource(res.id)}
                          className={`p-1.5 rounded-lg transition-colors ${
                            res.isBookmarked ? 'text-amber-500' : 'text-slate-300 hover:text-slate-500'
                          }`}
                        >
                          <Bookmark className="w-4 h-4 fill-current" />
                        </button>
                        <button
                          onClick={() => deleteResource(res.id)}
                          className="p-1.5 text-slate-300 hover:text-rose-600 rounded-lg transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    <h4 className="text-sm font-bold text-slate-900 mb-2">{res.title}</h4>

                    <div className="p-3 rounded-xl bg-slate-50 border border-slate-100 text-xs text-slate-600 font-mono break-all whitespace-pre-wrap">
                      {res.urlOrContent}
                    </div>
                  </div>

                  {res.urlOrContent.startsWith('http') && (
                    <div className="mt-3 pt-3 border-t border-slate-100 flex justify-end">
                      <a
                        href={res.urlOrContent}
                        target="_blank"
                        rel="noreferrer"
                        className="text-xs font-bold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                      >
                        Open Link <ExternalLink className="w-3.5 h-3.5" />
                      </a>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredNotes.length === 0 ? (
            <div className="col-span-2 bg-white rounded-3xl p-12 text-center text-slate-400 border border-slate-200 space-y-3">
              <FileText className="w-12 h-12 mx-auto text-indigo-400 opacity-60" />
              <h3 className="text-base font-bold text-slate-800">
                {notes.length === 0 ? 'No study notes yet' : 'No notes matching filter'}
              </h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                {notes.length === 0
                  ? 'Add your course notes, summary points, formulas, or reminders for quick recall.'
                  : 'Try selecting a different subject filter.'}
              </p>
              <button
                onClick={() => setShowAddNoteModal(true)}
                className="mt-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold shadow-xs transition-colors"
              >
                + Create First Note
              </button>
            </div>
          ) : (
            filteredNotes.map((note) => {
              const sub = subjects.find((s) => s.id === note.subjectId);

              return (
                <div
                  key={note.id}
                  className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-xs flex flex-col justify-between hover:border-indigo-300 transition-all"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2 mb-2">
                      {sub && (
                        <span className="text-xs font-semibold text-indigo-700">
                          {sub.name}
                        </span>
                      )}

                      <div className="flex items-center gap-1 ml-auto">
                        <button
                          onClick={() => toggleBookmarkNote(note.id)}
                          className={`p-1.5 rounded-lg transition-colors ${
                            note.isBookmarked ? 'text-amber-500' : 'text-slate-300 hover:text-slate-500'
                          }`}
                        >
                          <Bookmark className="w-4 h-4 fill-current" />
                        </button>
                        <button
                          onClick={() => deleteNote(note.id)}
                          className="p-1.5 text-slate-300 hover:text-rose-600 rounded-lg transition-colors"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    <h4 className="text-sm font-bold text-slate-900 mb-2">{note.title}</h4>
                    <div className="p-3.5 rounded-xl bg-amber-50/40 border border-amber-200/50 text-xs text-slate-700 whitespace-pre-wrap leading-relaxed font-mono">
                      {note.content}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      )}

      {/* Add Resource Modal */}
      {showAddResourceModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="text-base font-bold text-slate-900 mb-3">Add Study Resource</h3>
            <form onSubmit={handleCreateResource} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Title *</label>
                <input
                  type="text"
                  required
                  value={resTitle}
                  onChange={(e) => setResTitle(e.target.value)}
                  placeholder="e.g. Chapter 4 Slide Deck"
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
                    <option value="LINK">Link</option>
                    <option value="PDF">PDF</option>
                    <option value="SLIDES">Slides</option>
                    <option value="DOC">Document</option>
                    <option value="NOTE">Note</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Subject</label>
                  <select
                    value={resSubId}
                    onChange={(e) => setResSubId(e.target.value ? Number(e.target.value) : '')}
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
                <label className="block text-xs font-semibold text-slate-600 mb-1">URL / Content</label>
                <textarea
                  rows={3}
                  value={resContent}
                  onChange={(e) => setResContent(e.target.value)}
                  placeholder="Paste URL or quick lecture content..."
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddResourceModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs"
                >
                  Save Resource
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Note Modal */}
      {showAddNoteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-100">
            <h3 className="text-base font-bold text-slate-900 mb-3">Create Academic Note</h3>
            <form onSubmit={handleCreateNote} className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Note Title *</label>
                <input
                  type="text"
                  required
                  value={noteTitle}
                  onChange={(e) => setNoteTitle(e.target.value)}
                  placeholder="e.g. Master Theorem Cases & Proofs"
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden"
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Subject (Optional)</label>
                <select
                  value={noteSubId}
                  onChange={(e) => setNoteSubId(e.target.value ? Number(e.target.value) : '')}
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
                >
                  <option value="">General Note</option>
                  {subjects.map((s) => (
                    <option key={s.id} value={s.id}>{s.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Content</label>
                <textarea
                  rows={5}
                  value={noteContent}
                  onChange={(e) => setNoteContent(e.target.value)}
                  placeholder="Write formulas, proofs, reminders..."
                  className="w-full text-sm px-3 py-2 rounded-xl border border-slate-200 focus:outline-hidden font-mono"
                />
              </div>
              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowAddNoteModal(false)}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs"
                >
                  Save Note
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
