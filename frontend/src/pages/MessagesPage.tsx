import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  MessageSquare, Send, Search, Loader2, ChevronLeft, Users,
  CheckCheck, X,
} from 'lucide-react';

interface Conversation {
  id: string;
  otherUserId: string;
  otherUserName: string;
  otherUserRole: string;
  lastMessage?: string;
  lastMessageSenderId?: string;
  lastMessageAt?: string;
  unreadCount: number;
  createdAt: string;
}

interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  content: string;
  readAt?: string;
  createdAt: string;
}

interface UserOption {
  id: string;
  firstName: string;
  lastName: string;
  role: string;
}

const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Admin', PASTEUR: 'Pasteur', RESPONSABLE: 'Responsable',
  CHEF_DE_FAMILLE: 'Chef de famille', FAISEUR: 'Faiseur', MEMBRE: 'Membre',
};

function formatTime(iso?: string) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) +
    ' · ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

export default function MessagesPage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [newChatOpen, setNewChatOpen] = useState(false);
  const [userSearch, setUserSearch] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  const { data: conversations = [], isLoading } = useQuery({
    queryKey: ['messages', 'conversations'],
    queryFn: async () => (await api.get('/messages/conversations')).data as Conversation[],
    refetchInterval: 15000,
  });

  const { data: messages = [] } = useQuery({
    queryKey: ['messages', 'thread', activeConvId],
    queryFn: async () => {
      if (!activeConvId) return [] as ChatMessage[];
      const res = await api.get(`/messages/conversations/${activeConvId}/messages`);
      return res.data as ChatMessage[];
    },
    enabled: !!activeConvId,
    refetchInterval: 10000,
  });

  const { data: users = [] } = useQuery({
    queryKey: ['messages', 'users'],
    queryFn: async () => {
      const res = await api.get('/users?size=50');
      return (res.data?.content || res.data || []) as UserOption[];
    },
    enabled: newChatOpen,
  });

  const sendMutation = useMutation({
    mutationFn: async ({ convId, content }: { convId: string; content: string }) => {
      const res = await api.post(`/messages/conversations/${convId}/messages`, { content });
      return res.data as ChatMessage;
    },
    onSuccess: () => {
      setDraft('');
      queryClient.invalidateQueries({ queryKey: ['messages', 'conversations'] });
      queryClient.invalidateQueries({ queryKey: ['messages', 'thread', activeConvId] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const startMutation = useMutation({
    mutationFn: async (otherUserId: string) => {
      const res = await api.post('/messages/conversations', { otherUserId });
      return res.data as Conversation;
    },
    onSuccess: (conv) => {
      setActiveConvId(conv.id);
      setNewChatOpen(false);
      queryClient.invalidateQueries({ queryKey: ['messages', 'conversations'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const activeConv = conversations.find((c) => c.id === activeConvId);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, activeConvId]);

  // Marquage lu au changement de conversation
  useEffect(() => {
    if (activeConvId) {
      api.patch(`/messages/conversations/${activeConvId}/read`).then(() => {
        queryClient.invalidateQueries({ queryKey: ['messages', 'conversations'] });
      }).catch(() => {});
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeConvId]);

  const send = () => {
    if (!activeConvId || !draft.trim()) return;
    sendMutation.mutate({ convId: activeConvId, content: draft.trim() });
  };

  const otherUsers = (users as UserOption[])
    .filter(
      (u) => u.id !== user?.id &&
      !conversations.some((c) => c.otherUserId === u.id)
    )
    .filter((u) => {
      if (!userSearch.trim()) return true;
      const q = userSearch.toLowerCase();
      return ((u.firstName || '') + ' ' + (u.lastName || '')).toLowerCase().includes(q);
    });

  return (
    <div className="page-container" style={{ height: 'calc(100vh - 8rem)' }}>
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <MessageSquare className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Messagerie <span className="text-gradient font-display">interne</span></h1>
          </div>
          <p className="page-subtitle">Discutez en privé avec les membres, faiseurs, chefs et responsables</p>
        </div>
        <button onClick={() => setNewChatOpen(true)} className="btn-primary btn-sm animate-scale-in">
          <Send className="w-4 h-4" /> Nouvelle conversation
        </button>
      </div>

      <div className="glass-card overflow-hidden flex flex-col" style={{ height: 'calc(100% - 5rem)' }}>
        <div className="flex h-full">
          {/* Liste des conversations */}
          <div className={`w-full sm:w-80 border-r border-white/20 dark:border-white/[0.06] flex flex-col ${activeConv ? 'hidden sm:flex' : 'flex'}`}>
            <div className="p-3 border-b border-white/20 dark:border-white/[0.06]">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                id="filter-conversations"
                className="input pl-10 !py-2 text-sm"
                placeholder="Filtrer les conversations..."
              />
            </div>
            </div>
            <div className="flex-1 overflow-y-auto">
              {isLoading ? (
                <div className="p-4 space-y-3">
                  {[...Array(4)].map((_, i) => (
                    <div key={i} className="flex items-center gap-3">
                      <div className="skeleton w-10 h-10 rounded-xl" />
                      <div className="flex-1">
                        <div className="skeleton h-3 w-32 mb-2" />
                        <div className="skeleton h-2 w-24" />
                      </div>
                    </div>
                  ))}
                </div>
              ) : conversations.length === 0 ? (
                <div className="p-8 text-center">
                  <MessageSquare className="w-10 h-10 text-gray-300 dark:text-gray-600 mx-auto mb-3" />
                  <p className="text-sm text-gray-500 dark:text-gray-400">Aucune conversation</p>
                  <p className="text-xs text-gray-400 mt-1">Cliquez sur « Nouvelle conversation » pour discuter</p>
                </div>
              ) : (
                conversations.map((conv) => (
                  <button
                    key={conv.id}
                    onClick={() => setActiveConvId(conv.id)}
                    className={`w-full flex items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-white/40 dark:hover:bg-gray-800/30 ${
                      activeConvId === conv.id ? 'bg-primary-50/60 dark:bg-primary-900/20' : ''
                    }`}
                  >
                    <div className="relative flex-shrink-0">
                      <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-sm font-bold shadow-sm">
                        {conv.otherUserName.split(' ').map(w => w[0]).slice(0, 2).join('')}
                      </div>
                      {conv.unreadCount > 0 && (
                        <span className="absolute -top-1 -right-1 min-w-5 h-5 px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">
                          {conv.unreadCount}
                        </span>
                      )}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
                          {conv.otherUserName}
                        </p>
                        {conv.lastMessageAt && (
                          <span className="text-[10px] text-gray-400 flex-shrink-0">
                            {new Date(conv.lastMessageAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
                          </span>
                        )}
                      </div>
                      <p className={`text-xs truncate ${conv.unreadCount > 0 ? 'text-gray-800 dark:text-gray-200 font-medium' : 'text-gray-400'}`}>
                        {conv.lastMessage ? (conv.lastMessageSenderId === user?.id ? 'Vous : ' : '') + conv.lastMessage : 'Nouvelle conversation'}
                      </p>
                      <span className="text-[10px] text-gray-400">
                        {ROLE_LABELS[conv.otherUserRole] || conv.otherUserRole}
                      </span>
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>

          {/* Fil de discussion */}
          <div className={`flex-1 flex flex-col min-w-0 ${activeConv ? 'flex' : 'hidden sm:flex'}`}>
            {activeConvId && activeConv ? (
              <>
                {/* En-tête du fil */}
                <div className="flex items-center gap-3 px-4 py-3 border-b border-white/20 dark:border-white/[0.06]">
                  <button
                    onClick={() => setActiveConvId(null)}
                    className="sm:hidden p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800"
                  >
                    <ChevronLeft className="w-5 h-5 text-gray-500" />
                  </button>
                  <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-sm font-bold">
                    {activeConv.otherUserName.split(' ').map(w => w[0]).slice(0, 2).join('')}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 truncate">
                      {activeConv.otherUserName}
                    </p>
                    <p className="text-[11px] text-gray-400">
                      {ROLE_LABELS[activeConv.otherUserRole] || activeConv.otherUserRole}
                    </p>
                  </div>
                </div>

                {/* Messages */}
                <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
                  {messages.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-center">
                      <MessageSquare className="w-10 h-10 text-gray-300 dark:text-gray-600 mb-3" />
                      <p className="text-sm text-gray-500 dark:text-gray-400">Dites bonjour à {activeConv.otherUserName.split(' ')[0]} 👋</p>
                    </div>
                  ) : (
                    messages.map((m) => {
                      const mine = m.senderId === user?.id;
                      return (
                        <div key={m.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                          <div className={`max-w-[75%] rounded-2xl px-4 py-2.5 shadow-sm ${
                            mine
                              ? 'bg-gradient-to-br from-primary-500 to-primary-600 text-white rounded-br-md'
                              : 'bg-white/60 dark:bg-gray-800/60 text-gray-900 dark:text-gray-100 rounded-bl-md border border-white/30 dark:border-white/[0.06]'
                          }`}>
                            {!mine && (
                              <p className="text-[10px] text-gray-400 dark:text-gray-500 font-medium mb-0.5">{m.senderName}</p>
                            )}
                            <p className="text-sm leading-relaxed whitespace-pre-wrap break-words">{m.content}</p>
                            <div className={`flex items-center justify-end gap-1 mt-1 ${mine ? 'text-white/70' : 'text-gray-400'}`}>
                              <span className="text-[9px]">{formatTime(m.createdAt)}</span>
                              {mine && <CheckCheck className="w-3 h-3" />}
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={bottomRef} />
                </div>

                {/* Zone de saisie */}
                <div className="p-3 border-t border-white/20 dark:border-white/[0.06]">
                  <div className="flex items-end gap-2">
                    <textarea
                      className="input flex-1 !py-2.5 text-sm resize-none"
                      rows={2}
                      placeholder={`Message à ${activeConv.otherUserName.split(' ')[0]}...`}
                      value={draft}
                      onChange={(e) => setDraft(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
                      }}
                    />
                    <button
                      onClick={send}
                      disabled={!draft.trim() || sendMutation.isPending}
                      className="btn-primary !p-3 rounded-xl disabled:opacity-40"
                    >
                      {sendMutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-center p-8">
                <div className="p-4 rounded-2xl bg-primary-50 dark:bg-primary-900/20 mb-4">
                  <Users className="w-12 h-12 text-primary-500" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Messagerie interne</h3>
                <p className="text-sm text-gray-500 dark:text-gray-400 max-w-sm">
                  Sélectionnez une conversation ou démarrez-en une nouvelle pour échanger avec un membre de l'église.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modal nouvelle conversation */}
      {newChatOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setNewChatOpen(false)} />
          <div className="relative w-full max-w-md glass-card !bg-white/95 dark:!bg-gray-900/95 p-6 animate-scale-in">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100 font-display">Nouvelle conversation</h3>
              <button onClick={() => setNewChatOpen(false)} className="p-2 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="relative mb-4">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                id="search-contact"
                className="input pl-10"
                placeholder="Rechercher un membre..."
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
                autoFocus
              />
            </div>
            <div className="max-h-72 overflow-y-auto space-y-1">
              {startMutation.isPending ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="w-6 h-6 animate-spin text-primary-500" />
                </div>
              ) : otherUsers.length === 0 ? (
                <p className="text-sm text-gray-400 text-center py-8">Aucun membre trouvé</p>
              ) : (
                otherUsers.map((u) => (
                  <button
                    key={u.id}
                    onClick={() => startMutation.mutate(u.id)}
                    className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800/50 transition-colors text-left"
                  >
                    <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
                      {u.firstName?.[0]}{u.lastName?.[0]}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                        {u.firstName} {u.lastName}
                      </p>
                      <p className="text-[11px] text-gray-400">
                        {ROLE_LABELS[u.role] || u.role}
                      </p>
                    </div>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
