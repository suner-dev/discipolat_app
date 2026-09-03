import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MessageCircle, Send, Image, Users, Search, Loader2, RefreshCw, BarChart3 } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import EmptyState from '@/components/shared/EmptyState';

interface GroupMsg {
  id: string;
  groupId: string;
  groupType: string;
  senderId?: string;
  content: string;
  messageType?: string;
  reactionCount?: number;
  createdAt: string;
}

interface Department {
  id: string;
  nom: string;
  description?: string;
  responsableId?: string;
}

interface GroupStats {
  totalMessages: number;
  activeMembers: number;
  messagesThisWeek: number;
  topSender?: string;
}

export default function GroupMessagesPage() {
  const queryClient = useQueryClient();
  const [selectedGroup, setSelectedGroup] = useState<string>('');
  const [newMsg, setNewMsg] = useState('');
  const [query, setQuery] = useState('');
  const [showStats, setShowStats] = useState(false);

  const { data: departments = [], isLoading: isLoadingDepts, error: deptsError } = useQuery({
    queryKey: ['departments-for-messages'],
    queryFn: async () => {
      const res = await api.get('/departments?page=0&size=50');
      return (res.data?.content || res.data || []) as Department[];
    },
    retry: false,
  });

  const activeGroup = departments.find(d => d.id === selectedGroup);

  useEffect(() => {
    if (departments.length > 0 && !selectedGroup) {
      setSelectedGroup(departments[0].id);
    }
  }, [departments, selectedGroup]);

  const { data: messages = [], isLoading: isLoadingMsgs, error: msgsError } = useQuery({
    queryKey: ['group-messages', selectedGroup],
    queryFn: async () => {
      if (!selectedGroup) return [];
      const res = await api.get(`/group-messages/group/${selectedGroup}`);
      return res.data as GroupMsg[];
    },
    enabled: !!selectedGroup,
    retry: false,
  });

  const { data: stats } = useQuery({
    queryKey: ['group-messages-stats', selectedGroup],
    queryFn: async () => {
      if (!selectedGroup) return null;
      const res = await api.get(`/group-messages/group/${selectedGroup}/stats`);
      return res.data as GroupStats;
    },
    enabled: !!selectedGroup && showStats,
    retry: false,
  });

  const { data: searchResults } = useQuery({
    queryKey: ['group-messages-search', selectedGroup, query],
    queryFn: async () => {
      if (!selectedGroup) return [];
      const res = await api.get('/group-messages/search', { params: { groupId: selectedGroup, q: query } });
      return res.data as GroupMsg[];
    },
    enabled: query.trim().length > 0 && !!selectedGroup,
    retry: false,
  });

  const sendMutation = useMutation({
    mutationFn: async () => {
      if (!newMsg.trim() || !selectedGroup) return;
      return (await api.post('/group-messages', {
        groupId: selectedGroup,
        groupType: 'DEPARTMENT',
        content: newMsg,
        messageType: 'TEXT',
      })).data as GroupMsg;
    },
    onSuccess: () => {
      setNewMsg('');
      toast.success('Message envoyé');
      queryClient.invalidateQueries({ queryKey: ['group-messages', selectedGroup] });
    },
    onError: (e: unknown) => toast.error(e instanceof Error ? e.message : 'Erreur lors de l\'envoi'),
  });

  const displayed = query.trim().length > 0 ? (searchResults || []) : messages;
  const isLoading = isLoadingMsgs || isLoadingDepts;
  const error = msgsError || deptsError;
  const senderName = (m: GroupMsg) => (m.senderId ? `#${m.senderId.slice(0, 6)}` : 'Membre');
  const timeLabel = (m: GroupMsg) => new Date(m.createdAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="flex h-[calc(100vh-120px)] gap-4 p-6">
      {/* Group list */}
      <div className="w-64 bg-white/5 backdrop-blur rounded-2xl border border-white/10 overflow-hidden flex flex-col">
        <div className="p-4 border-b border-white/10"><h2 className="text-white font-semibold flex items-center gap-2"><Users className="w-4 h-4" /> Groupes</h2></div>
        <div className="flex-1 overflow-y-auto">
          {isLoadingDepts ? (
            <div className="flex justify-center py-4"><Loader2 className="w-5 h-5 animate-spin text-blue-400" /></div>
          ) : deptsError ? (
            <div className="p-3 text-center">
              <p className="text-xs text-red-400 mb-2">Erreur de chargement</p>
              <button onClick={() => queryClient.invalidateQueries({ queryKey: ['departments-for-messages'] })} className="text-xs text-blue-400 hover:underline flex items-center gap-1 mx-auto">
                <RefreshCw className="w-3 h-3" /> Réessayer
              </button>
            </div>
          ) : departments.length === 0 ? (
            <div className="p-3 text-center text-xs text-gray-400">Aucun département</div>
          ) : (
            departments.map(g => (
              <button key={g.id} onClick={() => { setSelectedGroup(g.id); setQuery(''); }}
                className={`w-full text-left p-3 border-b border-white/5 transition ${selectedGroup === g.id ? 'bg-blue-600/20 border-l-2 border-l-blue-400' : 'hover:bg-white/5'}`}>
                <div className="text-white text-sm font-medium">{g.nom}</div>
                <div className="text-xs text-gray-400">DEPARTMENT</div>
              </button>
            ))
          )}
        </div>
      </div>

      {/* Chat area */}
      <div className="flex-1 bg-white/5 backdrop-blur rounded-2xl border border-white/10 flex flex-col">
        <div className="p-4 border-b border-white/10 flex items-center justify-between">
          <h3 className="text-white font-semibold">{activeGroup?.nom || 'Sélectionnez un groupe'}</h3>
          <div className="flex items-center gap-2">
            <button onClick={() => setShowStats(!showStats)} className="p-1.5 rounded-lg hover:bg-white/10 transition" title="Statistiques">
              <BarChart3 className="w-4 h-4 text-gray-400" />
            </button>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
              <input value={query} onChange={(e) => setQuery(e.target.value)}
                placeholder="Rechercher..."
                className="bg-white/10 rounded-lg pl-9 pr-3 py-1.5 text-white text-sm placeholder-gray-500 outline-none focus:ring-2 focus:ring-blue-500" />
            </div>
          </div>
        </div>

        {showStats && stats && (
          <div className="p-4 border-b border-white/10 bg-white/5">
            <div className="grid grid-cols-4 gap-4 text-center">
              <div><div className="text-lg font-bold text-white">{stats.totalMessages}</div><div className="text-xs text-gray-400">Messages</div></div>
              <div><div className="text-lg font-bold text-white">{stats.activeMembers}</div><div className="text-xs text-gray-400">Membres actifs</div></div>
              <div><div className="text-lg font-bold text-white">{stats.messagesThisWeek}</div><div className="text-xs text-gray-400">Cette semaine</div></div>
              {stats.topSender && <div><div className="text-lg font-bold text-white truncate">{stats.topSender}</div><div className="text-xs text-gray-400">Top envoyeur</div></div>}
            </div>
          </div>
        )}

        <div className="flex-1 overflow-y-auto p-4 space-y-3">
          {isLoading ? (
            <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-blue-400" /></div>
          ) : error ? (
            <div className="text-center text-red-400 py-10 text-sm">Erreur de chargement des messages</div>
          ) : displayed.length === 0 ? (
            <EmptyState icon={<MessageCircle className="w-6 h-6 text-blue-400" />}
              title={query ? 'Aucun résultat' : 'Aucun message dans ce groupe'}
              message="Ecrivez un message pour lancer la conversation." />
          ) : (
            displayed.map(msg => (
              <div key={msg.id} className="flex gap-3">
                <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-purple-500 rounded-full flex items-center justify-center text-white text-xs font-bold">{senderName(msg)[0]}</div>
                <div>
                  <div className="flex items-center gap-2"><span className="text-white text-sm font-medium">{senderName(msg)}</span><span className="text-xs text-gray-500">{timeLabel(msg)}</span></div>
                  <p className="text-gray-300 text-sm">{msg.content}</p>
                  {msg.reactionCount && msg.reactionCount > 0 && <span className="text-xs text-gray-500">❤️ {msg.reactionCount}</span>}
                </div>
              </div>
            ))
          )}
        </div>
        <div className="p-4 border-t border-white/10 flex items-center gap-2">
          <input value={newMsg} onChange={(e) => setNewMsg(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && sendMutation.mutate()}
            className="flex-1 bg-white/10 rounded-xl px-4 py-2 text-white text-sm placeholder-gray-500 outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="Écrire un message..." />
          <button className="p-2 text-gray-400 hover:text-white"><Image className="w-5 h-5" /></button>
          <button onClick={() => sendMutation.mutate()} className="p-2 bg-blue-600 hover:bg-blue-700 rounded-xl text-white"><Send className="w-5 h-5" /></button>
        </div>
      </div>
    </div>
  );
}
