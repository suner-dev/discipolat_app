import { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import { useI18n } from '@/i18n';
import {
  MessageSquare, Send, Search, Loader2, ChevronLeft, Users,
  CheckCheck, X, Mic, MicOff, Smile, Reply, Hash, Play, Pause, Volume2,
} from 'lucide-react';

interface Conversation { id: string; otherUserId: string; otherUserName: string; otherUserRole: string; lastMessage?: string; lastMessageSenderId?: string; lastMessageAt?: string; unreadCount: number; }
interface GroupConversation { id: string; name: string; description?: string; groupType: string; memberCount: number; unreadCount: number; lastMessage?: string; lastMessageAt?: string; }
interface ChatMessage { id: string; conversationId?: string; groupId?: string; senderId: string; senderName: string; content: string; messageType?: string; mediaUrl?: string; mediaDuration?: number; replyToId?: string; replyToSenderName?: string; replyToContent?: string; readAt?: string; createdAt: string; reactionCounts?: Record<string, number>; userReaction?: string; }
interface UserOption { id: string; firstName: string; lastName: string; role: string; }

const EMOJI_REACTIONS = ['❤️', '👍', '😊', '🙏', '😂', '😮'];

function formatTime(iso?: string) {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) + ' · ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

function formatDuration(seconds?: number) {
  if (!seconds) return '0:00';
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export default function MessagesPage() {
  const { t } = useI18n();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [activeGroupId, setActiveGroupId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'private' | 'groups'>('private');
  const [draft, setDraft] = useState('');
  const [newChatOpen, setNewChatOpen] = useState(false);
  const [newGroupOpen, setNewGroupOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [messageSearch, setMessageSearch] = useState('');
  const [replyTo, setReplyTo] = useState<ChatMessage | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const [showEmojiPicker, setShowEmojiPicker] = useState<string | null>(null);
  const [viewReplies, setViewReplies] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const recordingTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Queries
  const { data: conversations = [], isLoading } = useQuery({
    queryKey: ['messages', 'conversations'], queryFn: async () => (await api.get('/messages/conversations')).data as Conversation[], refetchInterval: 15000,
  });

  const { data: groups = [] } = useQuery({
    queryKey: ['messages', 'groups'], queryFn: async () => (await api.get('/messages/groups')).data as GroupConversation[], refetchInterval: 15000,
  });

  const convId = activeGroupId || activeConvId;
  const convType = activeGroupId ? 'group' : 'private';

  const { data: messages = [] } = useQuery({
    queryKey: ['messages', 'thread', convId, convType],
    queryFn: async () => {
      if (!convId) return [] as ChatMessage[];
      if (convType === 'group') {
        return (await api.get(`/messages/groups/${convId}/messages`)).data as ChatMessage[];
      }
      return (await api.get(`/messages/conversations/${convId}/messages/enhanced`)).data as ChatMessage[];
    },
    enabled: !!convId,
    refetchInterval: 10000,
  });

  const { data: users = [] } = useQuery({
    queryKey: ['messages', 'users'], queryFn: async () => (await api.get('/users?size=50')).data as UserOption[], enabled: newChatOpen,
  });

  const { data: searchResults } = useQuery({
    queryKey: ['messages', 'search', messageSearch], queryFn: async () => (await api.get(`/messages/search?q=${encodeURIComponent(messageSearch)}&size=20`)).data, enabled: messageSearch.length >= 2,
  });

  const { data: threadReplies = [] } = useQuery({
    queryKey: ['messages', 'replies', viewReplies], queryFn: async () => viewReplies ? (await api.get(`/messages/messages/${viewReplies}/replies`)).data as ChatMessage[] : [], enabled: !!viewReplies,
  });

  // Mutations
  const sendMutation = useMutation({
    mutationFn: async ({ convId: cId, content, replyToId }: { convId: string; content: string; replyToId?: string }) => {
      if (convType === 'group') {
        return (await api.post(`/messages/groups/${cId}/messages`, { content, replyToId })).data as ChatMessage;
      }
      if (replyToId) {
        return (await api.post(`/messages/conversations/${cId}/reply`, { replyToId, content })).data as ChatMessage;
      }
      return (await api.post(`/messages/conversations/${cId}/messages/enhanced`, { content })).data as ChatMessage;
    },
    onSuccess: () => { setDraft(''); setReplyTo(null); queryClient.invalidateQueries({ queryKey: ['messages'] }); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const reactMutation = useMutation({
    mutationFn: async ({ messageId, emoji }: { messageId: string; emoji: string }) => (await api.post(`/messages/messages/${messageId}/reactions`, { emoji })).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['messages', 'thread'] }),
  });

  const voiceMutation = useMutation({
    mutationFn: async ({ convId: cId, audioUrl, duration }: { convId: string; audioUrl: string; duration: number }) => {
      if (convType === 'group') {
        return (await api.post(`/messages/groups/${cId}/voice`, { audioUrl, duration })).data;
      }
      return (await api.post(`/messages/conversations/${cId}/voice`, { audioUrl, duration })).data;
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['messages'] }); toast.success('Message vocal envoyé'); },
  });

  const startMutation = useMutation({
    mutationFn: async (otherUserId: string) => (await api.post('/messages/conversations', { otherUserId })).data as Conversation,
    onSuccess: (conv) => { setActiveConvId(conv.id); setNewChatOpen(false); queryClient.invalidateQueries({ queryKey: ['messages', 'conversations'] }); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const createGroupMutation = useMutation({
    mutationFn: async (data: { name: string; memberIds: string[] }) => (await api.post('/messages/groups', data)).data as GroupConversation,
    onSuccess: (g) => { setActiveGroupId(g.id); setActiveTab('groups'); setNewGroupOpen(false); queryClient.invalidateQueries({ queryKey: ['messages', 'groups'] }); },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const activeConv = conversations.find(c => c.id === activeConvId);
  const activeGroup = groups.find(g => g.id === activeGroupId);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages, activeConvId, activeGroupId]);

  useEffect(() => {
    if (convId) {
      api.patch(`/messages/conversations/${convId}/read`).then(() => queryClient.invalidateQueries({ queryKey: ['messages'] })).catch(() => {});
    }
  }, [convId]);

  const send = () => {
    if (!convId || !draft.trim()) return;
    sendMutation.mutate({ convId, content: draft.trim(), replyToId: replyTo?.id });
  };

  // Voice recording
  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      mediaRecorderRef.current = recorder;
      recorder.start();
      setIsRecording(true);
      setRecordingTime(0);
      recordingTimerRef.current = setInterval(() => setRecordingTime(t => t + 1), 1000);
      recorder.ondataavailable = async (e) => {
        const blob = new Blob([e.data], { type: 'audio/webm' });
        const url = URL.createObjectURL(blob);
        // In production: upload to storage, get URL. Here we simulate.
        voiceMutation.mutate({ convId: convId!, audioUrl: url, duration: recordingTime });
      };
      recorder.onstop = () => { stream.getTracks().forEach(t => t.stop()); setIsRecording(false); if (recordingTimerRef.current) clearInterval(recordingTimerRef.current); };
    } catch { toast.error('Microphone non disponible'); }
  };

  const stopRecording = () => { mediaRecorderRef.current?.stop(); };

  const filteredConversations = activeTab === 'private' ? conversations.filter(c => !searchQuery || c.otherUserName.toLowerCase().includes(searchQuery.toLowerCase())) : [];
  const filteredGroups = activeTab === 'groups' ? groups.filter(g => !searchQuery || g.name.toLowerCase().includes(searchQuery.toLowerCase())) : [];

  return (
    <div className="page-container" style={{ height: 'calc(100vh - 8rem)' }}>
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <MessageSquare className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Messagerie <span className="text-gradient font-display">interne</span></h1>
          </div>
          <p className="page-subtitle">Discutez en privé ou en groupe avec les membres</p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => setMessageSearch(messageSearch ? '' : ' ')} className="btn-secondary btn-sm">
            <Search className="w-4 h-4" />
          </button>
          <button onClick={() => setNewGroupOpen(true)} className="btn-secondary btn-sm">
            <Users className="w-4 h-4" /> Groupe
          </button>
          <button onClick={() => setNewChatOpen(true)} className="btn-primary btn-sm animate-scale-in">
            <Send className="w-4 h-4" /> Nouvelle conversation
          </button>
        </div>
      </div>

      {/* Message search bar */}
      {messageSearch !== '' && (
        <div className="glass-card p-3 mb-3 animate-fade-in">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input className="input pl-10" placeholder="Rechercher dans les messages..." value={messageSearch} onChange={e => setMessageSearch(e.target.value)} autoFocus />
            <button onClick={() => setMessageSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2"><X className="w-4 h-4 text-gray-400" /></button>
          </div>
          {searchResults?.content && (
            <div className="mt-2 space-y-2 max-h-60 overflow-y-auto">
              {searchResults.content.map((r: ChatMessage) => (
                <div key={r.id} className="p-2 rounded-lg bg-white/30 text-sm">
                  <span className="text-xs text-gray-500 font-medium">{r.senderName}</span>
                  <p className="text-gray-800">{r.content}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="glass-card overflow-hidden flex flex-col" style={{ height: messageSearch ? 'calc(100% - 10rem)' : 'calc(100% - 5rem)' }}>
        <div className="flex h-full">
          {/* Sidebar */}
          <div className={`w-full sm:w-80 border-r border-white/20 flex flex-col ${(activeConvId || activeGroupId) ? 'hidden sm:flex' : 'flex'}`}>
            {/* Tab switcher */}
            <div className="flex border-b border-white/20">
              <button onClick={() => setActiveTab('private')} className={`flex-1 py-3 text-sm font-medium ${activeTab === 'private' ? 'text-primary-600 border-b-2 border-primary-500' : 'text-gray-500'}`}>
                <MessageSquare className="w-4 h-4 inline mr-1" /> Privé ({conversations.length})
              </button>
              <button onClick={() => setActiveTab('groups')} className={`flex-1 py-3 text-sm font-medium ${activeTab === 'groups' ? 'text-primary-600 border-b-2 border-primary-500' : 'text-gray-500'}`}>
                <Users className="w-4 h-4 inline mr-1" /> Groupes ({groups.length})
              </button>
            </div>

            <div className="p-3 border-b border-white/20">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input className="input pl-10 !py-2 text-sm" placeholder="Filtrer..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} />
              </div>
            </div>

            <div className="flex-1 overflow-y-auto">
              {activeTab === 'private' && filteredConversations.map(conv => (
                <button key={conv.id} onClick={() => { setActiveConvId(conv.id); setActiveGroupId(null); }}
                  className={`w-full flex items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-white/40 ${activeConvId === conv.id ? 'bg-primary-50/60' : ''}`}>
                  <div className="relative flex-shrink-0">
                    <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-sm font-bold">
                      {conv.otherUserName.split(' ').map(w => w[0]).slice(0, 2).join('')}
                    </div>
                    {conv.unreadCount > 0 && <span className="absolute -top-1 -right-1 min-w-5 h-5 px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">{conv.unreadCount}</span>}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-semibold text-gray-900 truncate">{conv.otherUserName}</p>
                      {conv.lastMessageAt && <span className="text-[10px] text-gray-400">{new Date(conv.lastMessageAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}</span>}
                    </div>
                    <p className={`text-xs truncate ${conv.unreadCount > 0 ? 'text-gray-800 font-medium' : 'text-gray-400'}`}>{conv.lastMessage ? (conv.lastMessageSenderId === user?.id ? 'Vous : ' : '') + conv.lastMessage : 'Nouvelle conversation'}</p>
                  </div>
                </button>
              ))}

              {activeTab === 'groups' && filteredGroups.map(g => (
                <button key={g.id} onClick={() => { setActiveGroupId(g.id); setActiveConvId(null); }}
                  className={`w-full flex items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-white/40 ${activeGroupId === g.id ? 'bg-primary-50/60' : ''}`}>
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-400 to-emerald-600 flex items-center justify-center text-white">
                    <Hash className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-semibold text-gray-900 truncate">{g.name}</p>
                      {g.unreadCount > 0 && <span className="min-w-5 h-5 px-1 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center">{g.unreadCount}</span>}
                    </div>
                    <p className="text-xs text-gray-400">{g.memberCount} membres · {g.groupType}</p>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Chat area */}
          <div className={`flex-1 flex flex-col min-w-0 ${(activeConvId || activeGroupId) ? 'flex' : 'hidden sm:flex'}`}>
            {(activeConvId || activeGroupId) ? (
              <>
                {/* Header */}
                <div className="flex items-center gap-3 px-4 py-3 border-b border-white/20">
                  <button onClick={() => { setActiveConvId(null); setActiveGroupId(null); }} className="sm:hidden p-1.5 rounded-lg hover:bg-gray-100">
                    <ChevronLeft className="w-5 h-5 text-gray-500" />
                  </button>
                  <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-sm font-bold">
                    {activeGroupId ? <Hash className="w-4 h-4" /> : activeConv?.otherUserName.split(' ').map(w => w[0]).slice(0, 2).join('')}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-gray-900 truncate">{activeGroupId ? activeGroup?.name : activeConv?.otherUserName}</p>
                    <p className="text-[11px] text-gray-400">{activeGroupId ? `${activeGroup?.memberCount} membres` : activeConv?.otherUserRole}</p>
                  </div>
                </div>

                {/* Messages */}
                <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
                  {messages.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-center">
                      <MessageSquare className="w-10 h-10 text-gray-300 mb-3" />
                      <p className="text-sm text-gray-500">Dites bonjour 👋</p>
                    </div>
                  ) : messages.map(m => {
                    const mine = m.senderId === user?.id;
                    return (
                      <div key={m.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                        <div className="max-w-[75%] group">
                          {/* Reply preview */}
                          {m.replyToId && m.replyToSenderName && (
                            <div className="mb-1 pl-3 border-l-2 border-primary-300 bg-primary-50/50 rounded-r-lg px-2 py-1">
                              <p className="text-[10px] text-primary-600 font-medium">{m.replyToSenderName}</p>
                              <p className="text-[10px] text-gray-500 truncate">{m.replyToContent}</p>
                            </div>
                          )}

                          <div className={`rounded-2xl px-4 py-2.5 shadow-sm ${mine ? 'bg-gradient-to-br from-primary-500 to-primary-600 text-white rounded-br-md' : 'bg-white/60 text-gray-900 rounded-bl-md border border-white/30'}`}>
                            {!mine && <p className="text-[10px] text-gray-400 font-medium mb-0.5">{m.senderName}</p>}

                            {/* Voice message */}
                            {m.messageType === 'VOICE' ? (
                              <div className="flex items-center gap-2 min-w-[180px]">
                                <button className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center"><Play className="w-4 h-4" /></button>
                                <div className="flex-1 h-1 bg-white/30 rounded-full"><div className="h-full bg-white/60 rounded-full" style={{ width: '30%' }} /></div>
                                <span className="text-[10px]">{formatDuration(m.mediaDuration)}</span>
                              </div>
                            ) : (
                              <p className="text-sm leading-relaxed whitespace-pre-wrap break-words">{m.content}</p>
                            )}

                            <div className={`flex items-center justify-end gap-1 mt-1 ${mine ? 'text-white/70' : 'text-gray-400'}`}>
                              <span className="text-[9px]">{formatTime(m.createdAt)}</span>
                              {mine && <CheckCheck className="w-3 h-3" />}
                            </div>
                          </div>

                          {/* Reactions */}
                          {m.reactionCounts && Object.keys(m.reactionCounts).length > 0 && (
                            <div className="flex flex-wrap gap-1 mt-1">
                              {Object.entries(m.reactionCounts).map(([emoji, count]) => (
                                <button key={emoji} onClick={() => reactMutation.mutate({ messageId: m.id, emoji })}
                                  className={`flex items-center gap-0.5 px-1.5 py-0.5 rounded-full text-xs ${m.userReaction === emoji ? 'bg-primary-100 border border-primary-300' : 'bg-gray-100 border border-gray-200'}`}>
                                  {emoji} <span className="text-[10px]">{count}</span>
                                </button>
                              ))}
                            </div>
                          )}

                          {/* Action buttons on hover */}
                          <div className="flex items-center gap-1 mt-0.5 opacity-0 group-hover:opacity-100 transition-opacity">
                            <button onClick={() => setShowEmojiPicker(showEmojiPicker === m.id ? null : m.id)} className="p-1 rounded hover:bg-gray-100"><Smile className="w-3.5 h-3.5 text-gray-400" /></button>
                            <button onClick={() => setReplyTo(m)} className="p-1 rounded hover:bg-gray-100"><Reply className="w-3.5 h-3.5 text-gray-400" /></button>
                            <button onClick={() => setViewReplies(viewReplies === m.id ? null : m.id)} className="p-1 rounded hover:bg-gray-100"><MessageSquare className="w-3.5 h-3.5 text-gray-400" /></button>
                          </div>

                          {/* Emoji picker */}
                          {showEmojiPicker === m.id && (
                            <div className="flex gap-1 mt-1 bg-white rounded-xl shadow-lg p-2 border">
                              {EMOJI_REACTIONS.map(emoji => (
                                <button key={emoji} onClick={() => { reactMutation.mutate({ messageId: m.id, emoji }); setShowEmojiPicker(null); }}
                                  className="w-8 h-8 rounded hover:bg-gray-100 flex items-center justify-center text-lg">{emoji}</button>
                              ))}
                            </div>
                          )}

                          {/* Thread replies */}
                          {viewReplies === m.id && threadReplies.length > 0 && (
                            <div className="mt-2 ml-4 space-y-2 border-l-2 border-gray-200 pl-3">
                              {threadReplies.map(r => (
                                <div key={r.id} className="text-xs bg-gray-50 rounded-lg p-2">
                                  <span className="font-medium text-primary-600">{r.senderName}</span>
                                  <span className="text-gray-400 ml-2">{formatTime(r.createdAt)}</span>
                                  <p className="text-gray-700 mt-0.5">{r.content}</p>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                  <div ref={bottomRef} />
                </div>

                {/* Reply preview */}
                {replyTo && (
                  <div className="px-4 py-2 bg-primary-50 border-t border-primary-200 flex items-center gap-2 animate-fade-in">
                    <Reply className="w-4 h-4 text-primary-500" />
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-medium text-primary-600">{replyTo.senderName}</p>
                      <p className="text-xs text-gray-500 truncate">{replyTo.content}</p>
                    </div>
                    <button onClick={() => setReplyTo(null)}><X className="w-4 h-4 text-gray-400" /></button>
                  </div>
                )}

                {/* Input */}
                <div className="p-3 border-t border-white/20">
                  <div className="flex items-end gap-2">
                    {/* Voice recording button */}
                    {isRecording ? (
                      <div className="flex items-center gap-2 bg-red-50 rounded-xl px-4 py-2 border border-red-200">
                        <div className="w-3 h-3 bg-red-500 rounded-full animate-pulse" />
                        <span className="text-sm text-red-600 font-medium">{formatDuration(recordingTime)}</span>
                        <button onClick={stopRecording} className="p-1"><MicOff className="w-5 h-5 text-red-500" /></button>
                      </div>
                    ) : (
                      <button onClick={startRecording} className="p-3 rounded-xl bg-gray-100 hover:bg-gray-200 transition">
                        <Mic className="w-5 h-5 text-gray-500" />
                      </button>
                    )}

                    <textarea className="input flex-1 !py-2.5 text-sm resize-none" rows={2}
                      placeholder={`Message ${activeGroupId ? 'au groupe' : ''}...`}
                      value={draft} onChange={e => setDraft(e.target.value)}
                      onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }} />

                    <button onClick={send} disabled={!draft.trim() || sendMutation.isPending}
                      className="btn-primary !p-3 rounded-xl disabled:opacity-40">
                      {sendMutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-center p-8">
                <div className="p-4 rounded-2xl bg-primary-50 mb-4"><Users className="w-12 h-12 text-primary-500" /></div>
                <h3 className="text-lg font-semibold text-gray-900 mb-1">Messagerie interne</h3>
                <p className="text-sm text-gray-500 max-w-sm">Sélectionnez une conversation ou créez un groupe pour échanger.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* New conversation modal */}
      {newChatOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setNewChatOpen(false)} />
          <div className="relative w-full max-w-md glass-card !bg-white/95 p-6 animate-scale-in">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-gray-900">Nouvelle conversation</h3>
              <button onClick={() => setNewChatOpen(false)} className="p-2 rounded-xl hover:bg-gray-100"><X className="w-5 h-5 text-gray-400" /></button>
            </div>
            <div className="relative mb-4">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input className="input pl-10" placeholder="Rechercher un membre..." value={searchQuery} onChange={e => setSearchQuery(e.target.value)} autoFocus />
            </div>
            <div className="max-h-72 overflow-y-auto space-y-1">
              {(users as UserOption[]).filter(u => u.id !== user?.id).filter(u => !searchQuery || `${u.firstName} ${u.lastName}`.toLowerCase().includes(searchQuery.toLowerCase())).map(u => (
                <button key={u.id} onClick={() => startMutation.mutate(u.id)}
                  className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-gray-100 transition text-left">
                  <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-white text-xs font-bold">{u.firstName?.[0]}{u.lastName?.[0]}</div>
                  <div><p className="text-sm font-medium text-gray-900">{u.firstName} {u.lastName}</p><p className="text-[11px] text-gray-400">{u.role}</p></div>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* New group modal */}
      {newGroupOpen && (
        <NewGroupModal users={users as UserOption[]} currentUser={user} onClose={() => setNewGroupOpen(false)} onCreate={(name, memberIds) => createGroupMutation.mutate({ name, memberIds })} isLoading={createGroupMutation.isPending} />
      )}
    </div>
  );
}

function NewGroupModal({ users, currentUser, onClose, onCreate, isLoading }: { users: UserOption[]; currentUser: any; onClose: () => void; onCreate: (name: string, memberIds: string[]) => void; isLoading: boolean }) {
  const [name, setName] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const toggle = (id: string) => setSelected(s => s.includes(id) ? s.filter(x => x !== id) : [...s, id]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md glass-card !bg-white/95 p-6 animate-scale-in">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-bold text-gray-900">Nouveau groupe</h3>
          <button onClick={onClose} className="p-2 rounded-xl hover:bg-gray-100"><X className="w-5 h-5 text-gray-400" /></button>
        </div>
        <input className="input mb-4" placeholder="Nom du groupe" value={name} onChange={e => setName(e.target.value)} autoFocus />
        <p className="text-sm font-medium text-gray-700 mb-2">Membres ({selected.length})</p>
        <div className="max-h-60 overflow-y-auto space-y-1">
          {users.filter(u => u.id !== currentUser?.id).map(u => (
            <button key={u.id} onClick={() => toggle(u.id)}
              className={`w-full flex items-center gap-3 p-2 rounded-lg text-left ${selected.includes(u.id) ? 'bg-primary-50 border border-primary-300' : 'hover:bg-gray-50'}`}>
              <input type="checkbox" checked={selected.includes(u.id)} readOnly className="rounded" />
              <span className="text-sm">{u.firstName} {u.lastName}</span>
            </button>
          ))}
        </div>
        <button onClick={() => { if (name.trim() && selected.length > 0) onCreate(name, selected); }}
          disabled={!name.trim() || selected.length === 0 || isLoading}
          className="btn-primary w-full mt-4 disabled:opacity-40">
          {isLoading ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Créer le groupe'}
        </button>
      </div>
    </div>
  );
}
