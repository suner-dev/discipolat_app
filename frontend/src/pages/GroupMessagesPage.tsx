import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import { MessageCircle, Send, Image, Users, Search, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import apiRaw from '@/lib/apiRaw';
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

// Exemple de groupes (à remplacer par un endpoint de listing des groupes).
// Le module backend `groupMessages` ne fournit que /group-messages/group/{groupId}
// et /search, pas la liste des groupes — les IDs doivent provenir des départements/familles.
const SAMPLE_GROUPS = [
  { id: '00000000-0000-0000-0000-00000000000a', name: 'Équipe Louange', groupType: 'DEPARTMENT' },
  { id: '00000000-0000-0000-0000-00000000000b', name: 'Équipe Accueil', groupType: 'DEPARTMENT' },
  { id: '00000000-0000-0000-0000-00000000000c', name: 'Département Jeunesse', groupType: 'DEPARTMENT' },
];

export default function GroupMessagesPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [selectedGroup, setSelectedGroup] = useState(SAMPLE_GROUPS[0].id);
  const [newMsg, setNewMsg] = useState('');
  const [query, setQuery] = useState('');

  const activeGroup = SAMPLE_GROUPS.find(g => g.id === selectedGroup);

  const { data: messages = [], isLoading, error } = useQuery({
    queryKey: ['group-messages', selectedGroup],
    queryFn: async () => (await apiRaw.get(`/group-messages/group/${selectedGroup}`)).data as GroupMsg[],
    retry: false,
  });

  const { data: searchResults } = useQuery({
    queryKey: ['group-messages-search', selectedGroup, query],
    queryFn: async () =>
      (await apiRaw.get('/group-messages/search', { params: { groupId: selectedGroup, q: query } })).data as GroupMsg[],
    enabled: query.trim().length > 0,
    retry: false,
  });

  const sendMutation = useMutation({
    mutationFn: async () => {
      if (!newMsg.trim()) return;
      return (await apiRaw.post('/group-messages', {
        groupId: selectedGroup,
        groupType: activeGroup?.groupType || 'DEPARTMENT',
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
  const senderName = (m: GroupMsg) => (m.senderId ? `#${m.senderId.slice(0, 6)}` : 'Membre');
  const timeLabel = (m: GroupMsg) => new Date(m.createdAt).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });

  return (
    <div className="flex h-[calc(100vh-120px)] gap-4 p-6">
      {/* Group list */}
      <div className="w-64 bg-white/5 backdrop-blur rounded-2xl border border-white/10 overflow-hidden flex flex-col">
        <div className="p-4 border-b border-white/10"><h2 className="text-white font-semibold flex items-center gap-2"><Users className="w-4 h-4" /> {t('groupMessages.groups') || 'Groupes'}</h2></div>
        <div className="flex-1 overflow-y-auto">
          {SAMPLE_GROUPS.map(g => (
            <button key={g.id} onClick={() => { setSelectedGroup(g.id); setQuery(''); }}
              className={`w-full text-left p-3 border-b border-white/5 transition ${selectedGroup === g.id ? 'bg-blue-600/20 border-l-2 border-l-blue-400' : 'hover:bg-white/5'}`}>
              <div className="text-white text-sm font-medium">{g.name}</div>
              <div className="text-xs text-gray-400">{g.groupType}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Chat area */}
      <div className="flex-1 bg-white/5 backdrop-blur rounded-2xl border border-white/10 flex flex-col">
        <div className="p-4 border-b border-white/10 flex items-center justify-between">
          <h3 className="text-white font-semibold">{activeGroup?.name}</h3>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500" />
            <input value={query} onChange={(e) => setQuery(e.target.value)}
              placeholder="Rechercher..."
              className="bg-white/10 rounded-lg pl-9 pr-3 py-1.5 text-white text-sm placeholder-gray-500 outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
        </div>
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
            placeholder={t('groupMessages.placeholder') || 'Écrire un message...'} />
          <button className="p-2 text-gray-400 hover:text-white"><Image className="w-5 h-5" /></button>
          <button onClick={() => sendMutation.mutate()} className="p-2 bg-blue-600 hover:bg-blue-700 rounded-xl text-white"><Send className="w-5 h-5" /></button>
        </div>
      </div>
    </div>
  );
}
