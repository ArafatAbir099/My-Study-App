import React, { useState, useRef } from 'react';
import {
  X,
  Upload,
  Sparkles,
  Layers,
  Plus,
  Trash2,
  CheckCircle2,
  AlertCircle,
  FileText,
  Image as ImageIcon,
  Edit2,
  ArrowRight,
  BookOpen,
  ArrowUp,
  ArrowDown,
  FileCode,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { ExtractedChapterDraft, ExtractedTopicDraft, TopicImportance } from '../../types';
import { parseSyllabusContent } from '../../util/syllabusParser';

interface SyllabusUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  targetSubjectId?: number | null;
}

export const SyllabusUploadModal: React.FC<SyllabusUploadModalProps> = ({
  isOpen,
  onClose,
  targetSubjectId = null,
}) => {
  const { subjects, saveStructuredSyllabus, addSubject } = usePlanner();

  const [step, setStep] = useState<'UPLOAD' | 'ANALYZING' | 'REVIEW'>('UPLOAD');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | 'NEW'>(
    targetSubjectId || (subjects[0]?.id ?? 'NEW')
  );
  const [newSubjectName, setNewSubjectName] = useState('');
  const [newSubjectCode, setNewSubjectCode] = useState('');

  // Uploaded media state
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [imageBase64, setImageBase64] = useState<string | null>(null);
  const [imageMime, setImageMime] = useState<string>('image/jpeg');
  const [fileName, setFileName] = useState<string>('');
  const [rawTextInput, setRawTextInput] = useState('');
  const [uploadMode, setUploadMode] = useState<'IMAGE' | 'TEXT'>('IMAGE');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Extracted draft state for user review & editing
  const [draftCourseName, setDraftCourseName] = useState('');
  const [draftCourseCode, setDraftCourseCode] = useState('');
  const [chaptersDraft, setChaptersDraft] = useState<ExtractedChapterDraft[]>([]);

  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const isImage = file.type.startsWith('image/');
    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');

    if (!isImage && !isPdf) {
      setErrorMessage('Please select an image file (PNG, JPG, WebP) or a PDF document.');
      return;
    }

    setErrorMessage(null);
    setFileName(file.name);
    setImageMime(file.type || (isPdf ? 'application/pdf' : 'image/jpeg'));

    const reader = new FileReader();
    reader.onload = () => {
      const result = reader.result as string;
      if (isImage) {
        setImagePreview(result);
      } else {
        setImagePreview(null);
      }
      const base64Data = result.split(',')[1];
      setImageBase64(base64Data);
    };
    reader.readAsDataURL(file);
  };

  const handleStartAnalysis = async () => {
    if (uploadMode === 'IMAGE' && !imageBase64) {
      setErrorMessage('Please select a syllabus image or document first.');
      return;
    }
    if (uploadMode === 'TEXT' && !rawTextInput.trim()) {
      setErrorMessage('Please enter or paste your syllabus text first.');
      return;
    }

    setStep('ANALYZING');
    setErrorMessage(null);

    const activeSub = subjects.find((s) => s.id === selectedSubjectId);
    const fallbackCourse = activeSub?.name || 'Academic Course';

    try {
      const res = await fetch('/api/gemini/extract-syllabus', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageBase64: uploadMode === 'IMAGE' ? imageBase64 : undefined,
          mimeType: imageMime,
          rawText: uploadMode === 'TEXT' ? rawTextInput : undefined,
          defaultCourseName: fallbackCourse,
        }),
      });

      const data = await res.json();
      if (!data.success || !data.data) {
        throw new Error(data.message || 'AI Extraction returned empty response');
      }

      const extracted = data.data;
      setDraftCourseName(extracted.courseName || activeSub?.name || 'University Course');
      setDraftCourseCode(extracted.courseCode || activeSub?.courseCode || '');

      const formattedChapters: ExtractedChapterDraft[] = (extracted.chapters || []).map(
        (ch: any, idx: number) => ({
          id: `chap-${Date.now()}-${idx}`,
          title: ch.title || `Module ${idx + 1}`,
          topics: (ch.topics || []).map((t: any, tIdx: number) => ({
            id: `top-${Date.now()}-${idx}-${tIdx}`,
            name: t.name || 'Topic',
            description: t.description || '',
            importance: (t.importance as TopicImportance) || 'NORMAL',
          })),
        })
      );

      if (formattedChapters.length === 0) {
        formattedChapters.push({
          id: `chap-${Date.now()}-0`,
          title: 'Module 1: Foundations',
          topics: [
            { id: `top-${Date.now()}-0`, name: 'Introduction to Course', description: '', importance: 'NORMAL' },
          ],
        });
      }

      setChaptersDraft(formattedChapters);
      setStep('REVIEW');
    } catch (err: any) {
      console.warn('AI analysis fallback:', err);
      // Fallback: heuristic extraction from text or placeholder for image
      if (uploadMode === 'TEXT' && rawTextInput.trim()) {
        const parsed = parseSyllabusContent(rawTextInput, fallbackCourse);
        if (parsed.length > 0) {
          const draftList: ExtractedChapterDraft[] = parsed[0].units.map((u, idx) => ({
            id: `chap-${Date.now()}-${idx}`,
            title: u.title,
            topics: u.topics.map((t, tIdx) => ({
              id: `top-${Date.now()}-${idx}-${tIdx}`,
              name: t.name,
              description: t.description,
              importance: 'NORMAL',
            })),
          }));
          setDraftCourseName(fallbackCourse);
          setChaptersDraft(draftList);
          setStep('REVIEW');
          return;
        }
      }

      // Default review screen so user is never blocked from editing & saving
      setDraftCourseName(fallbackCourse);
      setChaptersDraft([
        {
          id: `chap-${Date.now()}-0`,
          title: 'Module 1: Core Units',
          topics: [
            { id: `top-${Date.now()}-0`, name: 'Unit 1: Foundations', description: '', importance: 'NORMAL' },
            { id: `top-${Date.now()}-1`, name: 'Unit 2: Key Concepts', description: '', importance: 'IMPORTANT' },
          ],
        },
      ]);
      setStep('REVIEW');
    }
  };

  // Editing helpers on review screen
  const handleChapterTitleChange = (chapterIndex: number, newTitle: string) => {
    setChaptersDraft((prev) =>
      prev.map((c, i) => (i === chapterIndex ? { ...c, title: newTitle } : c))
    );
  };

  const handleAddChapter = () => {
    setChaptersDraft((prev) => [
      ...prev,
      {
        id: `chap-${Date.now()}-${prev.length}`,
        title: `Module ${prev.length + 1}: New Unit`,
        topics: [
          {
            id: `top-${Date.now()}-${prev.length}-0`,
            name: 'New Topic',
            description: '',
            importance: 'NORMAL',
          },
        ],
      },
    ]);
  };

  const handleDeleteChapter = (chapterIndex: number) => {
    setChaptersDraft((prev) => prev.filter((_, i) => i !== chapterIndex));
  };

  const handleTopicChange = (
    chapterIndex: number,
    topicIndex: number,
    field: 'name' | 'description' | 'importance',
    val: string
  ) => {
    setChaptersDraft((prev) =>
      prev.map((ch, cIdx) => {
        if (cIdx !== chapterIndex) return ch;
        return {
          ...ch,
          topics: ch.topics.map((top, tIdx) => {
            if (tIdx !== topicIndex) return top;
            return { ...top, [field]: val };
          }),
        };
      })
    );
  };

  const handleAddTopic = (chapterIndex: number) => {
    setChaptersDraft((prev) =>
      prev.map((ch, cIdx) => {
        if (cIdx !== chapterIndex) return ch;
        return {
          ...ch,
          topics: [
            ...ch.topics,
            {
              id: `top-${Date.now()}-${cIdx}-${ch.topics.length}`,
              name: 'New Topic',
              description: '',
              importance: 'NORMAL',
            },
          ],
        };
      })
    );
  };

  const handleDeleteTopic = (chapterIndex: number, topicIndex: number) => {
    setChaptersDraft((prev) =>
      prev.map((ch, cIdx) => {
        if (cIdx !== chapterIndex) return ch;
        return {
          ...ch,
          topics: ch.topics.filter((_, tIdx) => tIdx !== topicIndex),
        };
      })
    );
  };

  // Reorder topics within a chapter
  const handleMoveTopic = (chapterIndex: number, topicIndex: number, direction: 'UP' | 'DOWN') => {
    setChaptersDraft((prev) =>
      prev.map((ch, cIdx) => {
        if (cIdx !== chapterIndex) return ch;
        const targetIdx = direction === 'UP' ? topicIndex - 1 : topicIndex + 1;
        if (targetIdx < 0 || targetIdx >= ch.topics.length) return ch;

        const newTopics = [...ch.topics];
        const temp = newTopics[topicIndex];
        newTopics[topicIndex] = newTopics[targetIdx];
        newTopics[targetIdx] = temp;
        return { ...ch, topics: newTopics };
      })
    );
  };

  // Final confirmation & save
  const handleConfirmAndSave = () => {
    let targetId: number;

    if (selectedSubjectId === 'NEW') {
      const created = addSubject(
        draftCourseName || newSubjectName || 'New Course',
        draftCourseCode || newSubjectCode || '',
        3.0,
        '',
        '#4F46E5',
        'book',
        'Imported from syllabus'
      );
      targetId = created.id;
    } else {
      targetId = selectedSubjectId;
    }

    saveStructuredSyllabus(targetId, chaptersDraft);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-2xl w-full p-6 shadow-2xl border border-slate-100 max-h-[92vh] flex flex-col relative overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
              <Sparkles className="w-5 h-5" />
            </div>
            <div>
              <h3 className="font-extrabold text-base text-slate-900">
                {step === 'REVIEW' ? 'Review & Edit Extracted Syllabus' : 'Upload Syllabus with Gemini AI'}
              </h3>
              <p className="text-xs text-slate-500">
                {step === 'REVIEW'
                  ? 'Verify, rename, add, delete, or reorder topics before confirming.'
                  : 'Upload syllabus photos, screenshots, or documents to automatically detect units & topics.'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-slate-400 hover:text-slate-600 hover:bg-slate-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {errorMessage && (
          <div className="my-3 p-3 bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold rounded-xl flex items-center gap-2 shrink-0">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* STEP 1: UPLOAD */}
        {step === 'UPLOAD' && (
          <div className="overflow-y-auto py-4 space-y-4 flex-1">
            {/* Subject Target Selector */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Assign to Subject / Course *
              </label>
              <select
                value={selectedSubjectId}
                onChange={(e) =>
                  setSelectedSubjectId(e.target.value === 'NEW' ? 'NEW' : Number(e.target.value))
                }
                className="w-full text-xs font-semibold px-3 py-2 rounded-xl border border-slate-200 bg-slate-50 focus:outline-hidden"
              >
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.courseCode || 'Course'})
                  </option>
                ))}
                <option value="NEW">+ Create New Subject from this Syllabus</option>
              </select>
            </div>

            {selectedSubjectId === 'NEW' && (
              <div className="grid grid-cols-2 gap-3 p-3 rounded-2xl bg-indigo-50/50 border border-indigo-100">
                <div>
                  <label className="block text-[11px] font-semibold text-indigo-900 mb-1">
                    Subject Name (Optional fallback)
                  </label>
                  <input
                    type="text"
                    value={newSubjectName}
                    onChange={(e) => setNewSubjectName(e.target.value)}
                    placeholder="e.g. Artificial Intelligence"
                    className="w-full text-xs px-3 py-2 rounded-xl border border-indigo-200 bg-white"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-semibold text-indigo-900 mb-1">
                    Course Code
                  </label>
                  <input
                    type="text"
                    value={newSubjectCode}
                    onChange={(e) => setNewSubjectCode(e.target.value)}
                    placeholder="e.g. CS 402"
                    className="w-full text-xs px-3 py-2 rounded-xl border border-indigo-200 bg-white"
                  />
                </div>
              </div>
            )}

            {/* Mode Selector */}
            <div className="flex bg-slate-100 p-1 rounded-xl w-fit">
              <button
                type="button"
                onClick={() => setUploadMode('IMAGE')}
                className={`px-3 py-1 text-xs font-bold rounded-lg transition-all flex items-center gap-1.5 ${
                  uploadMode === 'IMAGE'
                    ? 'bg-white text-indigo-700 shadow-xs'
                    : 'text-slate-600'
                }`}
              >
                <ImageIcon className="w-3.5 h-3.5" /> Syllabus Photo / Screenshot / PDF
              </button>
              <button
                type="button"
                onClick={() => setUploadMode('TEXT')}
                className={`px-3 py-1 text-xs font-bold rounded-lg transition-all flex items-center gap-1.5 ${
                  uploadMode === 'TEXT'
                    ? 'bg-white text-indigo-700 shadow-xs'
                    : 'text-slate-600'
                }`}
              >
                <FileText className="w-3.5 h-3.5" /> Paste Raw Text
              </button>
            </div>

            {uploadMode === 'IMAGE' ? (
              <div
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed border-slate-300 hover:border-purple-500 rounded-2xl p-6 text-center cursor-pointer transition-colors bg-slate-50/50 hover:bg-purple-50/20"
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  accept="image/*,application/pdf"
                  onChange={handleFileChange}
                  className="hidden"
                />

                {imagePreview ? (
                  <div className="space-y-2">
                    <img
                      src={imagePreview}
                      alt="Syllabus Preview"
                      className="max-h-48 mx-auto rounded-xl shadow-xs border border-slate-200 object-contain"
                    />
                    <p className="text-xs text-purple-700 font-semibold">
                      {fileName ? `${fileName} selected.` : ''} Click to change image
                    </p>
                  </div>
                ) : fileName ? (
                  <div className="space-y-2 py-4">
                    <FileCode className="w-10 h-10 text-purple-600 mx-auto" />
                    <p className="text-xs font-bold text-slate-800">{fileName}</p>
                    <p className="text-[11px] text-slate-400">Click to choose a different file</p>
                  </div>
                ) : (
                  <div className="space-y-2 py-4">
                    <Upload className="w-10 h-10 text-purple-600 mx-auto opacity-80" />
                    <p className="text-xs font-bold text-slate-800">
                      Click to upload syllabus screenshot, photo, or PDF
                    </p>
                    <p className="text-[11px] text-slate-400">
                      Supports JPG, PNG, WebP, PDF
                    </p>
                  </div>
                )}
              </div>
            ) : (
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Paste Syllabus Outline or Module List
                </label>
                <textarea
                  rows={7}
                  value={rawTextInput}
                  onChange={(e) => setRawTextInput(e.target.value)}
                  placeholder={`Example:\nModule 1: Introduction\n- Topic A\n- Topic B\n\nModule 2: Advanced Topics\n- Topic C\n- Topic D`}
                  className="w-full text-xs p-3 rounded-xl border border-slate-200 font-mono focus:outline-hidden"
                />
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleStartAnalysis}
                className="px-5 py-2 text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white rounded-xl shadow-xs flex items-center gap-1.5 transition-colors"
              >
                <Sparkles className="w-3.5 h-3.5" /> Analyze with Gemini
              </button>
            </div>
          </div>
        )}

        {/* STEP 2: ANALYZING SPINNER */}
        {step === 'ANALYZING' && (
          <div className="py-16 text-center space-y-3 flex-1 flex flex-col items-center justify-center">
            <div className="w-12 h-12 rounded-full border-4 border-purple-200 border-t-purple-600 animate-spin" />
            <h4 className="text-base font-extrabold text-slate-900">
              Gemini is analyzing syllabus...
            </h4>
            <p className="text-xs text-slate-500 max-w-sm">
              Extracting course titles, chapters, unit hierarchy, and individual study topics for your review screen.
            </p>
          </div>
        )}

        {/* STEP 3: INTERACTIVE REVIEW & EDIT SCREEN */}
        {step === 'REVIEW' && (
          <div className="overflow-y-auto py-3 space-y-4 flex-1 pr-1">
            <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center justify-between text-xs text-emerald-800">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                <span className="font-semibold">
                  Detected {chaptersDraft.length} chapters and{' '}
                  {chaptersDraft.reduce((acc, c) => acc + c.topics.length, 0)} topics!
                </span>
              </div>
              <span className="text-[11px] font-medium text-emerald-700">
                You can edit, add, delete, or reorder below.
              </span>
            </div>

            {/* Course Title & Code */}
            <div className="grid grid-cols-2 gap-3 p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80">
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Course Name
                </label>
                <input
                  type="text"
                  value={draftCourseName}
                  onChange={(e) => setDraftCourseName(e.target.value)}
                  className="w-full text-xs font-bold px-3 py-1.5 rounded-xl border border-slate-200 bg-white"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Course Code
                </label>
                <input
                  type="text"
                  value={draftCourseCode}
                  onChange={(e) => setDraftCourseCode(e.target.value)}
                  placeholder="e.g. CS 301"
                  className="w-full text-xs font-bold px-3 py-1.5 rounded-xl border border-slate-200 bg-white"
                />
              </div>
            </div>

            {/* Editable Chapters List */}
            <div className="space-y-4">
              {chaptersDraft.map((chapter, cIdx) => (
                <div
                  key={chapter.id}
                  className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-2xs"
                >
                  {/* Chapter Header */}
                  <div className="p-3 bg-slate-50/80 border-b border-slate-200 flex items-center justify-between gap-2">
                    <input
                      type="text"
                      value={chapter.title}
                      onChange={(e) => handleChapterTitleChange(cIdx, e.target.value)}
                      className="text-xs font-extrabold text-slate-900 bg-transparent border-b border-dashed border-slate-300 focus:border-indigo-600 focus:outline-hidden w-full max-w-sm px-1 py-0.5"
                    />
                    <div className="flex items-center gap-1 shrink-0">
                      <button
                        type="button"
                        onClick={() => handleAddTopic(cIdx)}
                        className="text-[11px] font-bold text-indigo-600 hover:text-indigo-800 px-2 py-1 rounded-lg hover:bg-indigo-50 flex items-center gap-1"
                      >
                        <Plus className="w-3.5 h-3.5" /> Topic
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDeleteChapter(cIdx)}
                        className="p-1 text-slate-300 hover:text-rose-600 rounded-lg"
                        title="Delete chapter"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Topics List with Reordering */}
                  <div className="p-2.5 divide-y divide-slate-100 space-y-1">
                    {chapter.topics.length === 0 ? (
                      <div className="py-2 text-center text-[11px] text-slate-400">
                        No topics in this chapter yet.
                      </div>
                    ) : (
                      chapter.topics.map((topic, tIdx) => (
                        <div key={topic.id} className="pt-2 pb-1.5 flex items-center gap-2">
                          {/* Reorder Buttons */}
                          <div className="flex flex-col shrink-0">
                            <button
                              type="button"
                              disabled={tIdx === 0}
                              onClick={() => handleMoveTopic(cIdx, tIdx, 'UP')}
                              className="p-0.5 text-slate-400 hover:text-indigo-600 disabled:opacity-20"
                              title="Move up"
                            >
                              <ArrowUp className="w-3 h-3" />
                            </button>
                            <button
                              type="button"
                              disabled={tIdx === chapter.topics.length - 1}
                              onClick={() => handleMoveTopic(cIdx, tIdx, 'DOWN')}
                              className="p-0.5 text-slate-400 hover:text-indigo-600 disabled:opacity-20"
                              title="Move down"
                            >
                              <ArrowDown className="w-3 h-3" />
                            </button>
                          </div>

                          <span className="text-[10px] text-slate-400 font-bold w-4">
                            {tIdx + 1}.
                          </span>

                          <input
                            type="text"
                            value={topic.name}
                            onChange={(e) =>
                              handleTopicChange(cIdx, tIdx, 'name', e.target.value)
                            }
                            placeholder="Topic name"
                            className="flex-1 text-xs font-semibold px-2 py-1 rounded-lg border border-slate-200 focus:outline-hidden focus:border-indigo-500"
                          />

                          <select
                            value={topic.importance || 'NORMAL'}
                            onChange={(e) =>
                              handleTopicChange(cIdx, tIdx, 'importance', e.target.value)
                            }
                            className="text-[10px] font-bold px-2 py-1 rounded-lg border border-slate-200 bg-slate-50 text-slate-700"
                          >
                            <option value="NORMAL">Normal</option>
                            <option value="IMPORTANT">Important</option>
                            <option value="VERY_IMPORTANT">High Focus</option>
                          </select>

                          <button
                            type="button"
                            onClick={() => handleDeleteTopic(cIdx, tIdx)}
                            className="p-1 text-slate-300 hover:text-rose-600 rounded-lg"
                            title="Remove topic"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              ))}

              <button
                type="button"
                onClick={handleAddChapter}
                className="w-full py-2.5 rounded-2xl border-2 border-dashed border-slate-200 hover:border-indigo-400 text-xs font-bold text-slate-600 hover:text-indigo-600 flex items-center justify-center gap-2 transition-colors"
              >
                <Plus className="w-4 h-4" /> Add Chapter / Module
              </button>
            </div>

            {/* Bottom Actions */}
            <div className="flex items-center justify-between pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setStep('UPLOAD')}
                className="text-xs font-bold text-slate-500 hover:text-slate-700"
              >
                &larr; Back to Upload
              </button>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
                >
                  Discard
                </button>
                <button
                  type="button"
                  onClick={handleConfirmAndSave}
                  className="px-5 py-2 text-xs font-bold bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl shadow-xs flex items-center gap-1.5 transition-colors"
                >
                  <CheckCircle2 className="w-4 h-4" /> Confirm & Save Syllabus
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
