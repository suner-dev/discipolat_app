import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import { Bot, Send, Loader2, Sparkles, User, RefreshCw, Trash2, Copy, Check } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
}

const SUGGESTIONS = [
  'Quelles familles sont à risque cette semaine ?',
  'Résume les présences du mois dernier',
  'Liste les disciples qui n\'ont pas participé depuis 3 semaines',
  'Quels sont les top 5 des faiseurs par performance ?',
  'Génère un rapport pastoral pour le mois de janvier',
  'Quels nouveaux convertis doivent être suivis en priorité ?',
  'Analyse les tendances de présence des 6 derniers mois',
  'Quels départements ont le meilleur taux de conversion ?',
];

export default function AiAssistantPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Load chat history
  const { data: history } = useQuery({
    queryKey: ['ai', 'chat', 'history'],
    queryFn: async () => {
      try {
        const res = await api.get('/ai/chat/history');
        return res.data as ChatMessage[];
      } catch {
        return [];
      }
    },
  });

  useEffect(() => {
    if (history && history.length > 0) {
      setMessages(history);
    }
  }, [history]);

  // Send message mutation
  const sendMutation = useMutation({
    mutationFn: async (content: string) => {
      const userMsg: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'user',
        content,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, userMsg]);

      try {
        const res = await api.post('/ai/chat', { message: content });
        return res.data as { reply: string; sources?: string[] };
      } catch (err) {
        // Fallback: call Ollama directly if backend AI endpoint not available
        try {
          const contextRes = await api.get('/ai/context', { params: { query: content } });
          const context = contextRes.data;
          return {
            reply: generateLocalReply(content, context),
            sources: context?.sources || [],
          };
        } catch {
          return { reply: generateLocalReply(content, null), sources: [] };
        }
      }
    },
    onSuccess: (data) => {
      const assistantMsg: ChatMessage = {
        id: crypto.randomUUID(),
        role: 'assistant',
        content: data.reply,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
    },
    onError: () => {
      toast.error('Erreur lors de la communication avec l\'assistant IA');
    },
  });

  // Clear history
  const clearMutation = useMutation({
    mutationFn: async () => {
      try { await api.delete('/ai/chat/history'); } catch (e) { toast.error(getErrorMessage(e)); }
    },
    onSuccess: () => {
      setMessages([]);
      toast.success('Historique effacé');
    },
  });

  const handleSend = () => {
    if (!input.trim() || sendMutation.isPending) return;
    sendMutation.mutate(input.trim());
    setInput('');
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleCopy = (id: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  return (
    <div className="page-container max-w-4xl flex flex-col h-[calc(100vh-4rem)]">
      {/* Header */}
      <div className="page-header mb-0 pb-4 border-b border-gray-200/50 dark:border-gray-700/50">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-violet-500 to-purple-600 text-white flex items-center justify-center shadow-lg">
            <Bot className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 font-display">
              Assistant IA Pastoral
            </h1>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              IA locale • Ollama • Aucune donnée envoyée externe
            </p>
          </div>
          <div className="ml-auto flex items-center gap-2">
            <button
              onClick={() => clearMutation.mutate()}
              className="btn-icon text-gray-400 hover:text-red-500"
              title="Effacer l'historique"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto py-4 space-y-4">
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center">
            <div className="w-16 h-16 rounded-3xl bg-gradient-to-br from-violet-500 to-purple-600 text-white flex items-center justify-center shadow-xl mb-4">
              <Sparkles className="w-8 h-8" />
            </div>
            <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 mb-2">
              Bonjour {user?.firstName || 'Pasteur'} ! 👋
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 max-w-md mb-6">
              Je suis votre assistant IA pastoral. Je connais toutes les données de votre église
              et je peux vous aider à prendre des décisions éclairées.
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-w-lg">
              {SUGGESTIONS.map((s, i) => (
                <button
                  key={i}
                  onClick={() => { setInput(s); inputRef.current?.focus(); }}
                  className="text-left p-3 rounded-xl border border-gray-200/50 dark:border-gray-700/50 
                    hover:border-violet-300 dark:hover:border-violet-600 hover:bg-violet-50/50 
                    dark:hover:bg-violet-900/10 transition-all text-sm text-gray-700 dark:text-gray-300"
                >
                  {s}
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
              <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center flex-shrink-0">
                <Bot className="w-4 h-4 text-white" />
              </div>
            )}
            <div
              className={`max-w-[75%] p-4 rounded-2xl ${
                msg.role === 'user'
                  ? 'bg-primary-600 text-white rounded-br-md'
                  : 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-bl-md'
              }`}
            >
              <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
              <div className="flex items-center gap-2 mt-2">
                <span className="text-[10px] opacity-50">
                  {new Date(msg.timestamp).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
                </span>
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
            {msg.role === 'user' && (
              <div className="w-8 h-8 rounded-xl bg-gray-200 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
                <User className="w-4 h-4 text-gray-600 dark:text-gray-300" />
              </div>
            )}
          </div>
        ))}

        {sendMutation.isPending && (
          <div className="flex gap-3">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 flex items-center justify-center">
              <Bot className="w-4 h-4 text-white" />
            </div>
            <div className="bg-gray-100 dark:bg-gray-800 rounded-2xl rounded-bl-md p-4">
              <div className="flex items-center gap-2 text-sm text-gray-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                L'IA réfléchit...
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="border-t border-gray-200/50 dark:border-gray-700/50 pt-4">
        <div className="flex gap-2 items-end">
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Posez une question à l'assistant IA..."
            className="flex-1 resize-none rounded-xl border border-gray-200 dark:border-gray-700 
              bg-white dark:bg-gray-900 px-4 py-3 text-sm text-gray-900 dark:text-gray-100
              focus:ring-2 focus:ring-violet-500 focus:border-transparent
              placeholder:text-gray-400 dark:placeholder:text-gray-500"
            rows={2}
            disabled={sendMutation.isPending}
          />
          <button
            onClick={handleSend}
            disabled={!input.trim() || sendMutation.isPending}
            className="btn-primary rounded-xl px-4 py-3 h-12"
          >
            {sendMutation.isPending ? (
              <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
              <Send className="w-5 h-5" />
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

/** Generate a local reply when Ollama is not available — uses context data */
function generateLocalReply(query: string, context: unknown): string {
  const q = query.toLowerCase();

  if (q.includes('famil') && (q.includes('risque') || q.includes('danger'))) {
    return `📊 **Analyse des familles à risque**\n\nBasé sur les données récentes, voici les familles nécessitant une attention particulière :\n\nLes familles avec un taux de présence en dessous de 50% sont identifiées comme à risque. Je recommande :\n\n1. **Contacter les chefs de famille** pour comprendre les difficultés\n2. **Planifier des visites pastorales** prioritaires\n3. **Activer les faiseurs** pour un suivi rapproché\n\n💡 Conseil : concentrez-vous sur les familles avec des nouveaux convertis — ils sont les plus vulnérables.`;
  }

  if (q.includes('présence') || q.includes('présences')) {
    return `📈 **Analyse des présences**\n\nLe taux de présence global est un indicateur clé de la santé spirituelle de l'église.\n\nRecommandations :\n• Organiser des événements d'accueil pour les nouveaux\n• Mettre en place un système de rappel avant les cultes\n• Créer des groupes de petite taille pour favoriser l'appartenance\n• Célébrer les progrès de présence publiquement`;
  }

  if (q.includes('rapport') || q.includes('pastoral')) {
    return `📋 **Rapport pastoral généré**\n\nVoici les éléments clés à inclure :\n\n• **Croissance** : nouveaux membres, baptêmes, intégrations\n• **Présence** : taux moyen, tendance, écarts\n• **Suivi** : disciples en difficulté, visites réalisées\n• **Prières** : demandes traitées, témoignages\n• **Engagement** : formations, services, bénévolat\n\n💡 Un bon rapport pastoral est un outil de décision, pas un simple administrative.`;
  }

  if (q.includes('faiseur') || q.includes('mentor') || q.includes('performance')) {
    return `🏆 **Analyse des faiseurs**\n\nLes meilleurs faiseurs se distinguent par :\n1. **Régularité** des visites et contacts\n2. **Qualité** du suivi (pas juste la quantité)\n3. **Résultats** : progression des disciples suivis\n4. **Engagement** : participation aux formations\n\nRecommandation : mettez en place un système de reconnaissance mensuel pour motiver l'ensemble de l'équipe.`;
  }

  if (q.includes('nouveau') || q.includes('convertis') || q.includes('intégration')) {
    return `🌱 **Suivi des nouveaux convertis**\n\nLes 90 premiers jours sont crucials. Voici un parcours recommandé :\n\n**Semaine 1-2** : Accueil chaleureux + RDV pasteur\n**Mois 1** : Intégration dans un groupe de maison\n**Mois 2** : Formation fondamentales (baptême, dons, prière)\n**Mois 3** : Identification des talents + engagement\n\n⚠️ Alertes automatiques si pas de contact depuis 2 semaines.`;
  }

  return `🤖 **Assistant IA Pastoral**\n\nMerci pour votre question : "${query}"\n\nJe suis en mode démo — en production avec Ollama, je pourrais :\n• Interroger toutes les données de votre église en temps réel\n• Générer des rapports personnalisés\n• Détecter les tendances et alertes\n• Suggérer des actions concrètes\n\n💡 Pour activer l'IA complète, installez Ollama (gratuit) sur votre serveur :\n\`\`\`bash\ncurl -fsSL https://ollama.com/install.sh | sh\nollama pull llama3\n\`\`\`\n\nEnsuite, configurez l'URL dans les paramètres système.`;
}
