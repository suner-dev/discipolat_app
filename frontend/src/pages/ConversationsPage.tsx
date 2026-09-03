import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { MessageSquare, Send, Loader2, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';

interface Conversation {
  id: string;
  participantName: string;
  lastMessage?: string;
  unreadCount: number;
  updatedAt: string;
}

interface Message {
  id: string;
  senderName: string;
  content: string;
  createdAt: string;
}

export default function ConversationsPage() {
  const qc = useQueryClient();
  const [selectedConvo, setSelectedConvo] = useState<string | null>(null);
  const [newMessage, setNewMessage] = useState('');

  const { data: conversations = [], isLoading } = useQuery({
    queryKey: ['conversations'],
    queryFn: async () => (await api.get('/conversations')).data as Conversation[],
  });

  const { data: messages = [], isLoading: loadingMessages } = useQuery({
    queryKey: ['conversations', selectedConvo, 'messages'],
    queryFn: async () => (await api.get(`/conversations/${selectedConvo}/messages`)).data as Message[],
    enabled: !!selectedConvo,
  });

  const sendMutation = useMutation({
    mutationFn: async () => {
      if (!selectedConvo || !newMessage.trim()) return;
      await api.post(`/conversations/${selectedConvo}/reply`, { content: newMessage });
      setNewMessage('');
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['conversations', selectedConvo, 'messages'] }); },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-white shadow-lg">
          <MessageSquare className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Conversations</h1>
          <p className="page-subtitle">Messagerie privée</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-1 space-y-2">
          {isLoading ? (
            <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
          ) : conversations.length === 0 ? (
            <div className="glass-card p-6 text-center text-gray-500">Aucune conversation</div>
          ) : (
            conversations.map((c) => (
              <button
                key={c.id}
                onClick={() => setSelectedConvo(c.id)}
                className={`w-full text-left glass-card p-4 transition ${selectedConvo === c.id ? 'ring-2 ring-primary-500' : 'hover:bg-white/5'}`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-800 dark:text-gray-200">{c.participantName}</span>
                  {c.unreadCount > 0 && (
                    <span className="px-2 py-0.5 rounded-full bg-primary-500 text-white text-xs">{c.unreadCount}</span>
                  )}
                </div>
                {c.lastMessage && <p className="text-xs text-gray-500 mt-1 truncate">{c.lastMessage}</p>}
              </button>
            ))
          )}
        </div>

        <div className="lg:col-span-2">
          {selectedConvo ? (
            <div className="glass-card p-4 h-[600px] flex flex-col">
              <div className="flex-1 overflow-y-auto space-y-3 mb-4">
                {loadingMessages ? (
                  <div className="flex justify-center py-12"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
                ) : (
                  messages.map((m) => (
                    <div key={m.id} className="bg-white/5 rounded-lg p-3">
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{m.senderName}</span>
                        <span className="text-xs text-gray-400">{new Date(m.createdAt).toLocaleString('fr-FR')}</span>
                      </div>
                      <p className="text-sm text-gray-600 dark:text-gray-300">{m.content}</p>
                    </div>
                  ))
                )}
              </div>
              <div className="flex gap-2">
                <input
                  className="input flex-1"
                  placeholder="Votre message..."
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && sendMutation.mutate()}
                />
                <button onClick={() => sendMutation.mutate()} disabled={sendMutation.isPending} className="btn-primary">
                  <Send className="w-4 h-4" />
                </button>
              </div>
            </div>
          ) : (
            <div className="glass-card p-12 text-center text-gray-500">
              Sélectionnez une conversation
            </div>
          )}
        </div>
      </div>
    </div>
  );
}