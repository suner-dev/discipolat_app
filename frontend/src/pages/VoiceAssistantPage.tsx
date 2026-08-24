import { useState, useRef, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  Mic, MicOff, Send, Loader2, Bot, Volume2, HelpCircle,
  Sparkles, ArrowRight, RefreshCw, X, Copy, Check,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

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
  const [messages, setMessages] = useState<VoiceMessage[]>([]);
  const [input, setInput] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [sessionId] = useState(() => crypto.randomUUID());
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [showCommands, setShowCommands] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

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

  // Process voice message
  const processMutation = useMutation({
    mutationFn: async (transcription: string) => {
      const res = await api.post('/voice/process', {
        transcription,
        sessionId,
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
      toast.error('Erreur lors du traitement vocal');
    },
  });

  const handleSend = () => {
    if (!input.trim() || processMutation.isPending) return;
    const userMsg: VoiceMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: input.trim(),
      timestamp: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    processMutation.mutate(input.trim());
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

  // Voice recording simulation (in production: Web Speech API)
  const toggleRecording = () => {
    if (isRecording) {
      setIsRecording(false);
      // In production: stop recording and send to Whisper
      toast.success('Transcription terminée (mode démo)');
    } else {
      setIsRecording(true);
      toast('🎙️ Enregistrement en cours... (mode démo)', { icon: '🎤' });
      // Auto-stop after 5 seconds in demo mode
      setTimeout(() => {
        setIsRecording(false);
        const demoTranscription = QUICK_COMMANDS[Math.floor(Math.random() * QUICK_COMMANDS.length)];
        setInput(demoTranscription);
      }, 2000);
    }
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="page-container max-w-4xl flex flex-col h-[calc(100vh-4rem)]">
      {/* Header */}
      <div className="page-header mb-0 pb-4 border-b border-gray-200/50 dark:border-gray-700/50">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-600 text-white flex items-center justify-center shadow-lg">
            <Mic className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 font-display">
              PasteurBot Vocal
            </h1>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              Assistant vocal conversationnel • Commandes vocales • Offline-ready
            </p>
          </div>
          <div className="ml-auto flex items-center gap-2">
            <button
              onClick={() => setShowCommands(!showCommands)}
              className="btn-icon text-gray-400 hover:text-cyan-500"
              title="Commandes disponibles"
            >
              <HelpCircle className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Commands Panel (collapsible) */}
      {showCommands && (
        <div className="bg-cyan-50/50 dark:bg-cyan-900/10 border border-cyan-200/50 dark:border-cyan-700/30 rounded-xl p-4 mb-4 animate-slide-up">
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-sm font-bold text-cyan-700 dark:text-cyan-400">
              🎤 Commandes vocales disponibles
            </h3>
            <button onClick={() => setShowCommands(false)} className="btn-icon text-gray-400 hover:text-gray-600">
              <X className="w-4 h-4" />
            </button>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {commands.map((cmd, i) => (
              <button
                key={i}
                onClick={() => handleQuickCommand(cmd.command)}
                className="text-left p-3 rounded-xl bg-white/60 dark:bg-white/5 border border-cyan-100 dark:border-cyan-800/30 hover:border-cyan-300 dark:hover:border-cyan-600 transition-all"
              >
                <div className="flex items-center gap-2 mb-1">
                  <span className={`px-1.5 py-0.5 rounded text-[10px] font-semibold ${CATEGORY_COLORS[cmd.category] || 'bg-gray-100 text-gray-600'}`}>
                    {cmd.category}
                  </span>
                </div>
                <p className="text-sm font-medium text-gray-900 dark:text-gray-100">"{cmd.command}"</p>
                <p className="text-[10px] text-gray-500 dark:text-gray-400 mt-0.5">{cmd.description}</p>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto py-4 space-y-4">
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <div className="w-20 h-20 rounded-3xl bg-gradient-to-br from-cyan-500 to-blue-600 text-white flex items-center justify-center shadow-2xl mb-6">
              <Mic className="w-10 h-10" />
            </div>
            <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100 mb-2">
              Bonjour {user?.firstName || 'Pasteur'} ! 🎙️
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 max-w-md mb-8">
              Je suis votre assistant vocal. Dites-moi ce que vous voulez savoir
              ou cliquez sur une commande rapide ci-dessous.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-w-lg">
              {QUICK_COMMANDS.map((cmd, i) => (
                <button
                  key={i}
                  onClick={() => handleQuickCommand(cmd)}
                  className="text-left p-3 rounded-xl border border-gray-200/50 dark:border-gray-700/50
                    hover:border-cyan-300 dark:hover:border-cyan-600 hover:bg-cyan-50/50
                    dark:hover:bg-cyan-900/10 transition-all text-sm text-gray-700 dark:text-gray-300"
                >
                  🗣️ {cmd}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
          >
            {msg.role === 'assistant' && (
              <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center flex-shrink-0">
                <Bot className="w-4 h-4 text-white" />
              </div>
            )}
            <div className="max-w-[75%]">
              <div
                className={`p-4 rounded-2xl ${
                  msg.role === 'user'
                    ? 'bg-primary-600 text-white rounded-br-md'
                    : 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-bl-md'
                }`}
              >
                {msg.transcription && msg.role === 'user' && (
                  <div className="text-[10px] opacity-70 mb-1 flex items-center gap-1">
                    <Volume2 className="w-3 h-3" /> Transcription : "{msg.transcription}"
                  </div>
                )}
                <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                <div className="flex items-center gap-2 mt-2">
                  <span className="text-[10px] opacity-50">
                    {new Date(msg.timestamp).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                  {msg.intent && (
                    <span className="text-[10px] opacity-50 bg-white/10 px-1.5 py-0.5 rounded">
                      {msg.intent}
                    </span>
                  )}
                  {msg.role === 'assistant' && (
                    <button
                      onClick={() => handleCopy(msg.id, msg.content)}
                      className="text-[10px] opacity-50 hover:opacity-100"
                    >
                      {copiedId === msg.id ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                    </button>
                  )}
                </div>
              </div>
              {/* Suggestions */}
              {msg.suggestions && msg.suggestions.length > 0 && (
                <div className="flex flex-wrap gap-1.5 mt-2">
                  {msg.suggestions.map((s, i) => (
                    <button
                      key={i}
                      onClick={() => handleQuickCommand(s.command)}
                      className="text-[11px] px-2.5 py-1 rounded-full bg-cyan-50 dark:bg-cyan-900/20 text-cyan-700 dark:text-cyan-400 border border-cyan-200/50 dark:border-cyan-700/30 hover:bg-cyan-100 dark:hover:bg-cyan-900/30 transition-all flex items-center gap-1"
                    >
                      <span>{s.icon}</span> {s.command}
                      <ArrowRight className="w-3 h-3" />
                    </button>
                  ))}
                </div>
              )}
            </div>
            {msg.role === 'user' && (
              <div className="w-8 h-8 rounded-xl bg-gray-200 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                <Mic className="w-4 h-4 text-gray-600 dark:text-gray-300" />
              </div>
            )}
          </div>
        ))}

        {processMutation.isPending && (
          <div className="flex gap-3">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center">
              <Bot className="w-4 h-4 text-white" />
            </div>
            <div className="bg-gray-100 dark:bg-gray-800 rounded-2xl rounded-bl-md p-4">
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                Analyse en cours...
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input + Record Button */}
      <div className="border-t border-gray-200/50 dark:border-gray-700/50 pt-4">
        <div className="flex gap-2 items-end">
          {/* Record Button */}
          <button
            onClick={toggleRecording}
            className={`rounded-xl px-4 py-3 h-12 flex items-center justify-center transition-all ${
              isRecording
                ? 'bg-red-500 text-white animate-pulse shadow-lg shadow-red-500/30'
                : 'bg-gray-100 dark:bg-gray-800 text-gray-500 hover:bg-cyan-50 dark:hover:bg-cyan-900/20 hover:text-cyan-500'
            }`}
            title={isRecording ? 'Arrêter l\'enregistrement' : 'Enregistrer un message vocal'}
          >
            {isRecording ? <MicOff className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
          </button>

          {/* Text Input */}
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Tapez ou dictez votre question..."
            className="flex-1 resize-none rounded-xl border border-gray-200 dark:border-gray-700
              bg-white dark:bg-gray-900 px-4 py-3 text-sm text-gray-900 dark:text-gray-100
              focus:ring-2 focus:ring-cyan-500 focus:border-transparent
              placeholder:text-gray-400 dark:placeholder:text-gray-500"
            rows={2}
            disabled={processMutation.isPending}
          />

          {/* Send Button */}
          <button
            onClick={handleSend}
            disabled={!input.trim() || processMutation.isPending}
            className="rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white px-4 py-3 h-12 flex items-center justify-center shadow-lg shadow-cyan-500/25 hover:from-cyan-600 hover:to-blue-700 transition-all disabled:opacity-50"
          >
            {processMutation.isPending ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Send className="w-5 h-5" />
            )}
          </button>
        </div>

        {/* Status bar */}
        <div className="flex items-center justify-between mt-2 px-1">
          <div className="flex items-center gap-2 text-[10px] text-gray-400">
            <Sparkles className="w-3 h-3" />
            <span>Mode vocal : tapez ou enregistrez</span>
            {isRecording && (
              <span className="flex items-center gap-1 text-red-500 font-medium">
                <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
                ENREGISTREMENT
              </span>
            )}
          </div>
          <span className="text-[10px] text-gray-400">
            {messages.length} message{messages.length !== 1 ? 's' : ''}
          </span>
        </div>
      </div>
    </div>
  );
}
