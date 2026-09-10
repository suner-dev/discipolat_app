import { useState, useRef, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import { useNavigate } from 'react-router-dom';
import {
  Mic, MicOff, Send, Loader2, Bot, Volume2, HelpCircle,
  Sparkles, ArrowRight, RefreshCw, X, Copy, Check,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

import { getI18nLocale } from '@/i18n';
import { tText } from '@/i18n';
interface VoiceMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  transcription?: string;
  intent?: string;
  suggestions?: Array<{ command: string; icon: string }>;
  timestamp: string;
}

interface VoiceCommand {
  command: string;
  description: string;
  category: string;
}

const CATEGORY_COLORS: Record<string, string> = {
  SUIVI: 'bg-blue-100 text-blue-700 dark:bg-blue-500/20 dark:text-blue-400',
  STATISTIQUES: 'bg-emerald-100 text-emerald-700 dark:bg-emerald-500/20 dark:text-emerald-400',
  RAPPORTS: 'bg-purple-100 text-purple-700 dark:bg-purple-500/20 dark:text-purple-400',
  ÉVÉNEMENTS: 'bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-400',
  COMMUNICATION: 'bg-pink-100 text-pink-700 dark:bg-pink-500/20 dark:text-pink-400',
  ALERTES: 'bg-red-100 text-red-700 dark:bg-red-500/20 dark:text-red-400',
};

const QUICK_COMMANDS = [
  'Montre-moi les familles en décrochement',
  'Combien de nouveaux convertis ce mois ?',
  'Génère un rapport de la semaine',
  'Quels sont les prochains événements ?',
  'Montre le taux de présence',
  'Quelles sont les alertes actives ?',
];

export default function VoiceAssistantPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [messages, setMessages] = useState<VoiceMessage[]>([]);
  const [input, setInput] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [sessionId] = useState(() => crypto.randomUUID());
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [showCommands, setShowCommands] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);

  // Load available voice commands
  const { data: commands = [] } = useQuery({
    queryKey: ['voice', 'commands'],
    queryFn: async () => {
      try {
        const res = await api.get('/voice/commands');
        return res.data as VoiceCommand[];
      } catch {
        return [];
      }
    },
  });

  // Process voice message - text mode
  const processTextMutation = useMutation({
    mutationFn: async (text: string) => {
      const res = await api.post('/voice/process', {
        transcription: text,
        sessionId,
      });
      return res.data as {
        intent: string;
        reply: string;
        suggestions?: Array<{ command: string; icon: string }>;
      };
    },
    onSuccess: (data) => {
      const assistantMsg: VoiceMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: data.reply,
        intent: data.intent,
        suggestions: data.suggestions,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
    },
    onError: () => {
      toast.error(tText('Erreur lors du traitement vocal'));
    },
  });

  // Process voice message - audio mode (multipart)
  const processAudioMutation = useMutation({
    mutationFn: async (formData: FormData) => {
      const res = await api.post('/voice/transcribe', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      return res.data as {
        transcription: string;
        intent: string;
        reply: string;
        sources?: string[];
        suggestions?: Array<{ command: string; icon: string }>;
      };
    },
    onSuccess: (data) => {
      const assistantMsg: VoiceMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: data.reply,
        transcription: data.transcription,
        intent: data.intent,
        suggestions: data.suggestions,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
    },
    onError: () => {
      toast.error(tText('Erreur lors du traitement vocal'));
    },
  });

  const handleSend = () => {
    if (!input.trim() || processTextMutation.isPending) return;
    const userMsg: VoiceMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: input.trim(),
      timestamp: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    processTextMutation.mutate(input.trim());
    setInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleQuickCommand = (cmd: string) => {
    setInput(cmd);
    inputRef.current?.focus();
  };

  const handleCopy = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  // Real voice recording using MediaRecorder
  const toggleRecording = async () => {
    if (isRecording) {
      // Stop recording
      if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
        mediaRecorderRef.current.stop();
      }
      setIsRecording(false);
    } else {
      // Start recording
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
        const mimeType = MediaRecorder.isTypeSupported('audio/webm') ? 'audio/webm' : 'audio/mp4';
        const recorder = new MediaRecorder(stream, { mimeType });
        mediaRecorderRef.current = recorder;
        audioChunksRef.current = [];

        recorder.ondataavailable = (event) => {
          if (event.data.size > 0) audioChunksRef.current.push(event.data);
        };

        recorder.onstop = async () => {
          stream.getTracks().forEach(track => track.stop());
          if (audioChunksRef.current.length > 0) {
            const audioBlob = new Blob(audioChunksRef.current, { type: mimeType });
            const file = new File([audioBlob], `recording.${mimeType === 'audio/webm' ? 'webm' : 'mp4'}`, { type: mimeType });
            const formData = new FormData();
            formData.append('file', file);
            formData.append('sessionId', sessionId);
            formData.append('language', 'fr');
            try {
              await processAudioMutation.mutateAsync(formData);
              toast.success('Transcription envoyée au serveur');
            } catch (e) {
              toast.error(tText('Erreur lors de l\'envoi de la transcription'));
            }
          }
        };

        recorder.start();
        setIsRecording(true);
        toast('🎙️ Enregistrement en cours...', { icon: '🎤' });

        // Auto-stop after 30 seconds
        setTimeout(() => {
          if (recorder.state !== 'inactive') {
            recorder.stop();
            setIsRecording(false);
          }
        }, 30000);
      } catch (err) {
        setIsRecording(false);
        toast.error('Impossible d\'accéder au microphone : ' + (err instanceof Error ? err.message : 'Erreur inconnue'));
        console.error('Microphone error:', err);
      }
    }
  };

  // Scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col">
      <header className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-6 py-4 sticky top-0 z-10">
        <div className="max-w-4xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(-1)}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              aria-label="Retour"
            >
              <X size={24} className="text-gray-600 dark:text-gray-300" />
            </button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                <Bot className="w-8 h-8 inline-block mr-2 text-indigo-600" />
                PasteurBot Vocal
              </h1>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {user?.activeRole || user?.role || 'Utilisateur'} - Assistant IA conversationnel
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowCommands(!showCommands)}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
              aria-label={showCommands ? 'Masquer commandes' : 'Afficher commandes'}
            >
              <HelpCircle size={24} className="text-gray-600 dark:text-gray-300" />
            </button>
          </div>
        </div>
      </header>

      <main className="flex-1 flex flex-col max-w-4xl mx-auto w-full p-6">
        {/* Commands panel */}
        {showCommands && commands.length > 0 && (
          <div className="mb-6 p-4 bg-indigo-50 dark:bg-indigo-900/20 rounded-xl border border-indigo-100 dark:border-indigo-800 animate-slide-down">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-indigo-700 dark:text-indigo-300">
                <Sparkles className="w-5 h-5 inline mr-1" />
                Commandes vocales disponibles
              </h3>
              <button
                onClick={() => setShowCommands(false)}
                className="p-1 rounded hover:bg-indigo-100 dark:hover:bg-indigo-800"
              >
                <X size={18} className="text-indigo-600 dark:text-indigo-400" />
              </button>
            </div>
            <div className="flex flex-wrap gap-2">
              {commands.map((cmd) => (
                <button
                  key={cmd.command}
                  onClick={() => handleQuickCommand(cmd.command)}
                  className="px-3 py-1.5 text-sm rounded-full bg-white dark:bg-gray-700 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-50 dark:hover:bg-indigo-900/30 transition-colors"
                >
                  {cmd.command}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Quick commands when no API commands loaded */}
        {showCommands && commands.length === 0 && (
          <div className="mb-6 p-4 bg-indigo-50 dark:bg-indigo-900/20 rounded-xl border border-indigo-100 dark:border-indigo-800 animate-slide-down">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-indigo-700 dark:text-indigo-300">
                <Sparkles className="w-5 h-5 inline mr-1" />
                Commandes rapides
              </h3>
              <button
                onClick={() => setShowCommands(false)}
                className="p-1 rounded hover:bg-indigo-100 dark:hover:bg-indigo-800"
              >
                <X size={18} className="text-indigo-600 dark:text-indigo-400" />
              </button>
            </div>
            <div className="flex flex-wrap gap-2">
              {QUICK_COMMANDS.map((cmd, i) => (
                <button
                  key={i}
                  onClick={() => handleQuickCommand(cmd)}
                  className="px-3 py-1.5 text-sm rounded-full bg-white dark:bg-gray-700 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-50 dark:hover:bg-indigo-900/30 transition-colors"
                >
                  {cmd}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Messages */}
        <div className="flex-1 overflow-y-auto space-y-4 mb-6" ref={messagesEndRef}>
          {messages.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full min-h-[300px] text-center">
              <Bot size={64} className="text-indigo-300 dark:text-indigo-700 mb-4 opacity-50" />
              <h2 className="text-xl font-semibold text-gray-700 dark:text-gray-300 mb-2">
                Bienvenue dans PasteurBot Vocal
              </h2>
              <p className="text-gray-500 dark:text-gray-400 mb-6 max-w-md">
                Parlez ou tapez votre question. Je peux vous aider avec le suivi pastoral,
                les statistiques, les alertes, les rapports et bien plus encore.
              </p>
              <div className="flex flex-wrap gap-2 justify-center">
                {QUICK_COMMANDS.map((cmd, i) => (
                  <button
                    key={i}
                    onClick={() => handleQuickCommand(cmd)}
                    className="px-4 py-2 text-sm rounded-lg bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors text-left w-64"
                  >
                    <Volume2 className="w-4 h-4 inline mr-1 text-indigo-500" />
                    {cmd}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <>
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[80%] rounded-2xl p-4 ${
                      msg.role === 'user'
                        ? 'bg-indigo-600 text-white rounded-br-md'
                        : 'bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-bl-md shadow-sm border border-gray-100 dark:border-gray-700'
                    }`}
                  >
                    <div className="flex items-start gap-2">
                      {msg.role === 'assistant' && (
                        <Bot size={20} className="text-indigo-500 mt-0.5 flex-shrink-0" />
                      )}
                      <div className="flex-1">
                        <p className="whitespace-pre-wrap">{msg.content}</p>
                        {msg.transcription && (
                          <p className="text-xs text-gray-400 dark:text-gray-500 mt-1 italic">
                            « {msg.transcription} »
                          </p>
                        )}
                        {msg.intent && (
                          <span className="inline-block mt-2 px-2 py-0.5 text-xs rounded-full bg-indigo-100 dark:bg-indigo-900/30 text-indigo-700 dark:text-indigo-300">
                            Intent : {msg.intent}
                          </span>
                        )}
                        {msg.suggestions && msg.suggestions.length > 0 && (
                          <div className="mt-2 flex flex-wrap gap-1.5">
                            {msg.suggestions.map((s, idx) => (
                              <button
                                key={idx}
                                onClick={() => handleQuickCommand(s.command)}
                                className="px-2 py-1 text-xs rounded bg-indigo-50 dark:bg-indigo-900/20 text-indigo-700 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900/40 transition-colors"
                              >
                                {s.icon} {s.command}
                              </button>
                            ))}
                          </div>
                        )}
                        <div className="flex items-center gap-2 mt-2">
                          <span className="text-xs text-gray-400 dark:text-gray-500">
                            {new Date(msg.timestamp).toLocaleTimeString()}
                          </span>
                          <button
                            onClick={() => handleCopy(msg.id, msg.content)}
                            className="p-1 rounded hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                            aria-label="Copier"
                          >
                            {copiedId === msg.id ? (
                              <Check size={14} className="text-green-500" />
                            ) : (
                              <Copy size={14} className="text-gray-400" />
                            )}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              {(processTextMutation.isPending || processAudioMutation.isPending) && (
                <div className="flex justify-start">
                  <div className="bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-2xl rounded-bl-md shadow-sm border border-gray-100 dark:border-gray-700 p-4 max-w-[80%]">
                    <div className="flex items-center gap-2">
                      <Bot size={20} className="text-indigo-500" />
                      <div className="flex gap-1">
                        <div className="w-2 h-2 bg-indigo-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                        <div className="w-2 h-2 bg-indigo-500 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                        <div className="w-2 h-2 bg-indigo-500 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Input area */}
        <div className="bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-4 shadow-lg">
          <div className="flex items-end gap-3">
            <button
              onClick={toggleRecording}
              disabled={processTextMutation.isPending || processAudioMutation.isPending}
              className={`p-3 rounded-xl flex-shrink-0 transition-all ${
                isRecording
                  ? 'bg-red-500 text-white animate-pulse shadow-lg shadow-red-500/25'
                  : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
              }`}
              aria-label={isRecording ? 'Arrêter l\'enregistrement' : 'Démarrer l\'enregistrement'}
            >
              {isRecording ? <MicOff size={24} /> : <Mic size={24} />}
            </button>

            <textarea
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Tapez votre question ou utilisez le micro..."
              className="flex-1 min-h-[48px] max-h-32 px-4 py-2.5 bg-gray-50 dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl text-gray-900 dark:text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
              rows={1}
            />

            <button
              onClick={handleSend}
              disabled={!input.trim() || processTextMutation.isPending || processAudioMutation.isPending}
              className={`p-3 rounded-xl flex-shrink-0 transition-all ${
                !input.trim() || processTextMutation.isPending || processAudioMutation.isPending
                  ? 'bg-gray-300 dark:bg-gray-600 text-gray-500 cursor-not-allowed'
                  : 'bg-indigo-600 text-white hover:bg-indigo-700'
              }`}
              aria-label="Envoyer"
            >
              {processTextMutation.isPending || processAudioMutation.isPending ? (
                <Loader2 size={24} className="animate-spin" />
              ) : (
                <Send size={24} />
              )}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}