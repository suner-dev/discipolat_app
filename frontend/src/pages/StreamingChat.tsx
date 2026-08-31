import { useState, useRef, useEffect } from 'react';
import { Send, MessageCircle } from 'lucide-react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiRaw from '@/lib/apiRaw';

interface ChatMessage {
  id: string;
  senderName: string;
  content: string;
  messageType: string;
  emoji?: string;
  createdAt: string;
  isSystem?: boolean;
}

interface StreamingChatProps {
  streamId?: number;
}

export default function StreamingChat({ streamId }: StreamingChatProps) {
  const queryClient = useQueryClient();
  const [newMsg, setNewMsg] = useState('');
  const bottomRef = useRef<HTMLDivElement>(null);

  // Fetch messages from real API
  const { data: messages = [] } = useQuery<ChatMessage[]>({
    queryKey: ['stream-chat', streamId],
    queryFn: async () => {
      if (!streamId) return [];
      const res = await apiRaw.get(`/stream-chat/${streamId}`);
      return Array.isArray(res.data) ? res.data : [];
    },
    enabled: !!streamId,
    refetchInterval: 3000, // Poll every 3s for real-time feel
  });

  // Send message mutation
  const sendMessageMutation = useMutation({
    mutationFn: async (content: string) => {
      if (!streamId) return;
      await apiRaw.post(`/stream-chat/${streamId}`, {
        content,
        senderName: 'Vous',
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stream-chat', streamId] });
    },
  });

  // Send reaction mutation
  const sendReactionMutation = useMutation({
    mutationFn: async (emoji: string) => {
      if (!streamId) return;
      await apiRaw.post(`/stream-chat/${streamId}`, {
        content: emoji,
        emoji,
        senderName: 'Vous',
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stream-chat', streamId] });
    },
  });

  // Viewer count query
  const { data: viewerData } = useQuery<{ count: number }>({
    queryKey: ['stream-viewers', streamId],
    queryFn: async () => {
      if (!streamId) return { count: 0 };
      const res = await apiRaw.get(`/stream-chat/${streamId}/count`);
      return res.data;
    },
    enabled: !!streamId,
    refetchInterval: 5000,
  });

  const viewers = viewerData?.count ?? 0;

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = () => {
    if (!newMsg.trim() || !streamId) return;
    sendMessageMutation.mutate(newMsg.trim());
    setNewMsg('');
  };

  const quickReact = (emoji: string) => {
    if (!streamId) return;
    sendReactionMutation.mutate(emoji);
  };

  const formatTime = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  };

  if (!streamId) {
    return (
      <div className="flex flex-col h-[600px] glass rounded-2xl border border-white/20 dark:border-white/[0.06] overflow-hidden items-center justify-center">
        <MessageCircle className="w-12 h-12 text-gray-300 dark:text-gray-600 mb-3" />
        <p className="text-sm text-gray-400">Sélectionnez un stream pour accéder au chat</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[600px] glass rounded-2xl border border-white/20 dark:border-white/[0.06] overflow-hidden">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-100 dark:border-white/[0.06]">
        <div className="flex items-center gap-2">
          <MessageCircle className="w-4 h-4 text-pink-500" />
          <span className="text-sm font-semibold text-gray-900 dark:text-white">Chat en direct</span>
          <span className="flex items-center gap-1 px-2 py-0.5 rounded-lg bg-red-100 dark:bg-red-900/30 text-[10px] font-bold text-red-600">
            🔴 {viewers} messages
          </span>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.length === 0 && (
          <div className="text-center text-gray-400 text-sm py-8">
            Aucun message pour ce stream. Soyez le premier à écrire !
          </div>
        )}
        {messages.map((msg) => (
          <div key={msg.id} className={`flex flex-col ${msg.senderName === 'Vous' ? 'items-end' : 'items-start'}`}>
            {msg.messageType === 'REACTION' ? (
              <div className="px-3 py-1.5 rounded-lg bg-violet-100 dark:bg-violet-900/20 text-xs text-violet-600 dark:text-violet-300 text-center mx-auto">
                {msg.senderName} a réagi {msg.emoji || msg.content}
              </div>
            ) : (
              <>
                <div className="flex items-center gap-1.5 mb-0.5">
                  <span className="text-[10px] font-medium text-gray-400">{msg.senderName}</span>
                  <span className="text-[9px] text-gray-300 dark:text-gray-600">{formatTime(msg.createdAt)}</span>
                </div>
                <div className={`px-3 py-2 rounded-2xl max-w-[80%] text-sm ${
                  msg.senderName === 'Vous'
                    ? 'bg-pink-500 text-white rounded-br-md'
                    : 'bg-gray-100 dark:bg-white/5 text-gray-700 dark:text-gray-300 rounded-bl-md'
                }`}>
                  {msg.content}
                </div>
              </>
            )}
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Quick reactions */}
      <div className="flex gap-2 px-4 py-2 border-t border-gray-100 dark:border-white/[0.06]">
        {['🙏', '❤️', '🔥', '👏', '✨', '💯'].map((emoji) => (
          <button key={emoji} onClick={() => quickReact(emoji)}
            className="w-8 h-8 rounded-lg bg-gray-50 dark:bg-white/5 hover:bg-white/10 text-sm flex items-center justify-center transition hover:scale-110">
            {emoji}
          </button>
        ))}
      </div>

      {/* Input */}
      <div className="flex gap-2 px-4 py-3 border-t border-gray-100 dark:border-white/[0.06]">
        <input value={newMsg} onChange={(e) => setNewMsg(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && sendMessage()}
          placeholder="Écrire un message..."
          className="flex-1 px-4 py-2 rounded-xl bg-gray-50 dark:bg-white/5 border border-gray-200 dark:border-white/10 text-sm outline-none focus:ring-2 focus:ring-pink-500/20" />
        <button onClick={sendMessage} disabled={sendMessageMutation.isPending}
          className="w-10 h-10 rounded-xl bg-pink-500 text-white flex items-center justify-center hover:bg-pink-600 transition disabled:opacity-50">
          <Send className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
}
