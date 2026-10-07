import React, { useState, useRef, useEffect } from 'react';
import {
  X,
  Send,
  Mic,
  MicOff,
  Volume2,
  VolumeX,
  Image as ImageIcon,
  Sparkles,
  Search,
  ExternalLink,
  Bot,
  User,
  Trash2,
  Loader2,
  BookOpen,
} from 'lucide-react';
import { usePlanner } from '../../context/PlannerContext';
import { ChatMessage } from '../../types';

interface AIStudyAssistantModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AIStudyAssistantModal: React.FC<AIStudyAssistantModalProps> = ({
  isOpen,
  onClose,
}) => {
  const {
    subjects,
    topics,
    pyqs,
    exams,
    revisions,
    currentUser,
    studyRecommendations,
  } = usePlanner();

  const [messages, setMessages] = useState<ChatMessage[]>(() => {
    try {
      const saved = localStorage.getItem('study_planner_chat_msgs');
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  const [inputPrompt, setInputPrompt] = useState('');
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [imageMime, setImageMime] = useState('image/jpeg');
  const [useSearch, setUseSearch] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  // Voice recording state
  const [isListening, setIsListening] = useState(false);
  const [speechSupported, setSpeechSupported] = useState(true);
  const [ttsEnabled, setTtsEnabled] = useState(false);

  const recognitionRef = useRef<any>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const imageInputRef = useRef<HTMLInputElement>(null);

  // Persist messages
  useEffect(() => {
    localStorage.setItem('study_planner_chat_msgs', JSON.stringify(messages));
  }, [messages]);

  // Scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  // Setup Web Speech Recognition
  useEffect(() => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setSpeechSupported(false);
      return;
    }

    try {
      const recognizer = new SpeechRecognition();
      recognizer.continuous = false;
      recognizer.interimResults = false;
      recognizer.lang = 'en-US';

      recognizer.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        setInputPrompt((prev) => (prev ? `${prev} ${transcript}` : transcript));
        setIsListening(false);
      };

      recognizer.onerror = (e: any) => {
        console.warn('Speech recognition error:', e);
        setIsListening(false);
      };

      recognizer.onend = () => {
        setIsListening(false);
      };

      recognitionRef.current = recognizer;
    } catch {
      setSpeechSupported(false);
    }
  }, []);

  if (!isOpen) return null;

  const toggleListening = () => {
    if (!speechSupported || !recognitionRef.current) {
      alert('Speech recognition is not supported in this browser. Please use text input.');
      return;
    }

    if (isListening) {
      recognitionRef.current.stop();
      setIsListening(false);
    } else {
      try {
        recognitionRef.current.start();
        setIsListening(true);
      } catch (err) {
        console.warn('Failed to start speech recognition:', err);
        setIsListening(false);
      }
    }
  };

  const speakText = (text: string) => {
    if (!('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text.slice(0, 300));
    utterance.rate = 1.0;
    utterance.pitch = 1.0;
    window.speechSynthesis.speak(utterance);
  };

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setImageMime(file.type);
    const reader = new FileReader();
    reader.onload = () => {
      setSelectedImage(reader.result as string);
    };
    reader.readAsDataURL(file);
  };

  const handleSendMessage = async (textToSend?: string) => {
    const prompt = (textToSend || inputPrompt).trim();
    if (!prompt && !selectedImage) return;

    const userMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      sender: 'user',
      text: prompt,
      imageUrl: selectedImage || undefined,
      timestamp: Date.now(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputPrompt('');
    const base64Data = selectedImage ? selectedImage.split(',')[1] : undefined;
    setSelectedImage(null);
    setIsLoading(true);

    // Build real user academic context
    const userContext = {
      studentName: currentUser?.name || 'Student',
      university: currentUser?.university || 'University',
      enrolledSubjects: subjects.map((s) => ({
        id: s.id,
        name: s.name,
        code: s.courseCode,
        upcomingExamDate: s.upcomingExamDate,
        upcomingExamType: s.upcomingExamType,
      })),
      syllabusTopicsCount: topics.length,
      weakTopics: topics.filter((t) => t.understanding === 'WEAK').map((t) => t.name),
      completedTopics: topics.filter((t) => t.status === 'COMPLETED').map((t) => t.name),
      pyqsCount: pyqs.length,
      unsolvedPyqs: pyqs.filter((p) => p.status !== 'SOLVED').slice(0, 5).map((p) => p.questionText),
      highPriorityRecommendations: studyRecommendations.slice(0, 3).map((r) => `${r.topicName} (${r.priorityReason})`),
    };

    try {
      const history = messages.slice(-6).map((m) => ({
        role: m.sender,
        text: m.text,
      }));

      const res = await fetch('/api/gemini/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: prompt,
          history,
          userContext,
          imageBase64: base64Data,
          mimeType: imageMime,
          useSearch,
        }),
      });

      const data = await res.json();
      if (!data.success) {
        throw new Error(data.message || 'Failed to fetch response');
      }

      const assistantMsg: ChatMessage = {
        id: `msg-${Date.now()}-ai`,
        sender: 'assistant',
        text: data.text || 'I analyzed your request.',
        sources: data.sources || [],
        timestamp: Date.now(),
      };

      setMessages((prev) => [...prev, assistantMsg]);

      if (ttsEnabled) {
        speakText(assistantMsg.text);
      }
    } catch (err: any) {
      console.error(err);
      setMessages((prev) => [
        ...prev,
        {
          id: `msg-${Date.now()}-err`,
          sender: 'assistant',
          text: `⚠️ Error: ${err.message || 'Could not connect to Gemini service.'}`,
          timestamp: Date.now(),
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const quickPrompts = [
    'What should I study today based on my upcoming exams?',
    'Which topics are currently marked weak?',
    'Show me repeated PYQs to practice first.',
    'How is my overall exam readiness right now?',
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl max-w-2xl w-full h-[90vh] shadow-2xl border border-slate-100 flex flex-col relative overflow-hidden">
        {/* Header */}
        <div className="p-4 bg-gradient-to-r from-indigo-700 via-indigo-600 to-purple-700 text-white flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/20 backdrop-blur-xs flex items-center justify-center">
              <Bot className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-extrabold text-sm text-white">AI Study Assistant</h3>
                <span className="text-[10px] font-bold px-2 py-0.2 rounded-full bg-white/20 text-indigo-100">
                  Gemini 3.8
                </span>
              </div>
              <p className="text-[11px] text-indigo-100">
                Grounded in your actual subjects, syllabus, exams & PYQs
              </p>
            </div>
          </div>

          <div className="flex items-center gap-1.5">
            <button
              onClick={() => setTtsEnabled(!ttsEnabled)}
              className={`p-1.5 rounded-lg transition-colors ${
                ttsEnabled ? 'bg-white text-indigo-700' : 'text-white/80 hover:bg-white/10'
              }`}
              title={ttsEnabled ? 'Voice output enabled' : 'Voice output disabled'}
            >
              {ttsEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            </button>
            <button
              onClick={() => setMessages([])}
              className="p-1.5 text-white/80 hover:bg-white/10 rounded-lg transition-colors"
              title="Clear conversation"
            >
              <Trash2 className="w-4 h-4" />
            </button>
            <button
              onClick={onClose}
              className="p-1.5 text-white/80 hover:bg-white/10 rounded-lg transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Messages scroll area */}
        <div className="flex-1 overflow-y-auto p-4 space-y-3.5 bg-slate-50/50">
          {messages.length === 0 ? (
            <div className="py-8 text-center space-y-4">
              <div className="w-12 h-12 bg-indigo-100 text-indigo-600 rounded-2xl flex items-center justify-center mx-auto">
                <Sparkles className="w-6 h-6" />
              </div>
              <div>
                <h4 className="font-extrabold text-slate-800 text-sm">
                  Welcome to your AI Academic Tutor
                </h4>
                <p className="text-xs text-slate-500 max-w-sm mx-auto mt-1">
                  Ask questions about your courses, upload problem screenshots or diagrams, practice past exam questions, or plan revision sessions.
                </p>
              </div>

              {/* Quick suggestion pills */}
              <div className="flex flex-col gap-2 max-w-md mx-auto pt-2">
                {quickPrompts.map((q, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSendMessage(q)}
                    className="p-2.5 rounded-xl border border-slate-200/80 bg-white hover:border-indigo-400 hover:bg-indigo-50/40 text-xs text-left text-slate-700 font-medium transition-all shadow-2xs"
                  >
                    💬 {q}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((m) => (
              <div
                key={m.id}
                className={`flex gap-2.5 ${m.sender === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                {m.sender === 'assistant' && (
                  <div className="w-7 h-7 rounded-lg bg-indigo-600 text-white flex items-center justify-center shrink-0 mt-0.5">
                    <Bot className="w-4 h-4" />
                  </div>
                )}

                <div
                  className={`max-w-[85%] rounded-2xl p-3.5 text-xs shadow-2xs ${
                    m.sender === 'user'
                      ? 'bg-indigo-600 text-white rounded-br-xs'
                      : 'bg-white border border-slate-200/80 text-slate-800 rounded-bl-xs'
                  }`}
                >
                  {m.imageUrl && (
                    <img
                      src={m.imageUrl}
                      alt="Uploaded study snippet"
                      className="max-h-48 rounded-lg mb-2 object-contain border border-white/20"
                    />
                  )}
                  <div className="whitespace-pre-wrap leading-relaxed font-sans">
                    {m.text}
                  </div>

                  {/* Grounding web search citations */}
                  {m.sources && m.sources.length > 0 && (
                    <div className="mt-2.5 pt-2 border-t border-slate-100 space-y-1">
                      <span className="text-[10px] font-bold text-slate-400 block">
                        Web Sources:
                      </span>
                      {m.sources.map((src, i) => (
                        <a
                          key={i}
                          href={src.url}
                          target="_blank"
                          rel="noreferrer"
                          className="text-[10px] text-indigo-600 hover:underline flex items-center gap-1 truncate"
                        >
                          <ExternalLink className="w-3 h-3 shrink-0" /> {src.title}
                        </a>
                      ))}
                    </div>
                  )}
                </div>

                {m.sender === 'user' && (
                  <div className="w-7 h-7 rounded-lg bg-slate-300 text-slate-700 flex items-center justify-center shrink-0 mt-0.5 font-bold text-xs">
                    {currentUser?.name?.charAt(0) || 'U'}
                  </div>
                )}
              </div>
            ))
          )}

          {isLoading && (
            <div className="flex items-center gap-2 text-xs text-indigo-600 font-semibold p-2">
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>Analyzing courses and formulating guidance...</span>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Selected image preview thumbnail */}
        {selectedImage && (
          <div className="p-2.5 bg-slate-100 border-t border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <img
                src={selectedImage}
                alt="Upload preview"
                className="w-10 h-10 object-cover rounded-lg border border-slate-300"
              />
              <span className="text-xs font-semibold text-slate-700">
                Attached study image
              </span>
            </div>
            <button
              onClick={() => setSelectedImage(null)}
              className="p-1 rounded-full text-slate-400 hover:text-slate-600"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Input Bar */}
        <div className="p-3 bg-white border-t border-slate-100 space-y-2 shrink-0">
          <div className="flex items-center justify-between text-[11px] px-1">
            <button
              onClick={() => setUseSearch(!useSearch)}
              className={`flex items-center gap-1 font-semibold px-2 py-0.5 rounded-md transition-colors ${
                useSearch
                  ? 'bg-blue-100 text-blue-800'
                  : 'text-slate-500 hover:text-slate-800'
              }`}
              title="Ground query with Google Search for recent facts"
            >
              <Search className="w-3 h-3" />
              <span>Google Search Grounding {useSearch ? 'ON' : 'OFF'}</span>
            </button>

            {isListening && (
              <span className="text-rose-600 font-bold animate-pulse flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-rose-600" /> Listening... Speak now
              </span>
            )}
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
            className="flex items-center gap-2"
          >
            <input
              type="file"
              ref={imageInputRef}
              accept="image/*"
              onChange={handleImageSelect}
              className="hidden"
            />

            <button
              type="button"
              onClick={() => imageInputRef.current?.click()}
              className="p-2.5 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 transition-colors"
              title="Upload question / note image"
            >
              <ImageIcon className="w-4 h-4" />
            </button>

            <button
              type="button"
              onClick={toggleListening}
              className={`p-2.5 rounded-xl border transition-colors ${
                isListening
                  ? 'bg-rose-100 border-rose-300 text-rose-700 animate-pulse'
                  : 'border-slate-200 hover:bg-slate-100 text-slate-600'
              }`}
              title={isListening ? 'Stop listening' : 'Speak message'}
            >
              {isListening ? <MicOff className="w-4 h-4" /> : <Mic className="w-4 h-4" />}
            </button>

            <input
              type="text"
              value={inputPrompt}
              onChange={(e) => setInputPrompt(e.target.value)}
              placeholder="Ask anything about your syllabus, weak topics, PYQs..."
              className="flex-1 text-xs px-3.5 py-2.5 rounded-xl border border-slate-200 focus:outline-hidden focus:ring-2 focus:ring-indigo-500 font-medium"
            />

            <button
              type="submit"
              disabled={isLoading || (!inputPrompt.trim() && !selectedImage)}
              className="p-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white shadow-xs transition-colors"
            >
              <Send className="w-4 h-4" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
