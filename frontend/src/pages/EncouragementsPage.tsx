import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  Heart, Send, Inbox, Star, RefreshCw, Loader2, Plus, X, HeartHandshake,
  MessageCircle, ThumbsUp, Smile, Quote,
} from 'lucide-react';

interface Encouragement {
  id: string;
  senderName: string;
  recipientName: string;
  message: string;
  type: string;
  createdAt: string;
}

interface TeamMember {
  userId: string;
  name: string;
  role: string;
  encouragementsReceived: number;
}

const ENCOURAGEMENT_TYPES = [
  { value: 'PRAYER', label: 'Prière', icon: '🙏', color: 'purple' },
  { value: 'PRAISE', label: 'Louange', icon: '⭐', color: 'yellow' },
  { value: 'THANKS', label: 'Remerciement', icon: '❤️', color: 'red' },
  { value: 'SUPPORT', label: 'Soutien', icon: '💪', color: 'blue' },
  { value: 'WELCOME', label: 'Bienvenue', icon: '👋', color: 'green' },
  { value: 'SCRIPTURE', label: 'Verset', icon: '📖', color: 'indigo' },
];

export default function EncouragementsPage() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<'received' | 'sent' | 'team'>('received');
  const [showCompose, setShowCompose] = useState(false);
  const [newEnc, setNewEnc] = useState({ recipientId: '', message: '', type: 'PRAYER' });

  const { data: received = [], isLoading: receivedLoading } = useQuery({
    queryKey: ['encouragements', 'received'],
    queryFn: async () => { const res = await api.get('/encouragements/received'); return res.data as Encouragement[]; },
  });

  const { data: sent = [], isLoading: sentLoading } = useQuery({
    queryKey: ['encouragements', 'sent'],
    queryFn: async () => { const res = await api.get('/encouragements/sent'); return res.data as Encouragement[]; },
  });

  const { data: team = [], isLoading: teamLoading } = useQuery({
    queryKey: ['encouragements', 'team'],
    queryFn: async () => { const res = await api.get('/encouragements/my-team'); return res.data as TeamMember[]; },
  });

  const sendMutation = useMutation({
    mutationFn: async () => {
      await api.post('/encouragements', {
        recipientId: newEnc.recipientId,
        message: newEnc.message,
        type: newEnc.type,
      });
    },
    onSuccess: () => {
      toast.success('Encouragement envoyé ! 🙏');
      queryClient.invalidateQueries({ queryKey: ['encouragements'] });
      setShowCompose(false);
      setNewEnc({ recipientId: '', message: '', type: 'PRAYER' });
    },
    onError: () => toast.error('Erreur lors de l\'envoi'),
  });

  const TypeIcon = ({ type }: { type: string }) => {
    const t = ENCOURAGEMENT_TYPES.find(e => e.value === type);
    return <span className="text-lg">{t?.icon || '💝'}</span>;
  };

  const EncouragementCard = ({ enc }: { enc: Encouragement }) => {
    const typeInfo = ENCOURAGEMENT_TYPES.find(t => t.value === enc.type);
    return (
      <div className="glass-card px-5 py-4 animate-slide-up">
        <div className="flex items-start gap-3">
          <div className={`w-10 h-10 rounded-xl bg-${typeInfo?.color || 'gray'}-100 dark:bg-${typeInfo?.color || 'gray'}-900/30 flex items-center justify-center flex-shrink-0`}>
            <TypeIcon type={enc.type} />
          </div>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{enc.senderName}</span>
              <span className="text-xs text-gray-400">→ {enc.recipientName}</span>
              <span className="badge text-[10px]">{typeInfo?.label || enc.type}</span>
            </div>
            <p className="text-sm text-gray-700 dark:text-gray-300 italic">"{enc.message}"</p>
            <p className="text-[10px] text-gray-400 mt-1">{new Date(enc.createdAt).toLocaleString('fr-FR')}</p>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="page-container max-w-4xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <HeartHandshake className="w-5 h-5 text-pink-500" />
            Encouragements
          </h1>
          <p className="page-subtitle">Envoyez des mots d'encouragement à votre équipe</p>
        </div>
        <button onClick={() => setShowCompose(true)} className="btn-primary btn-sm">
          <Plus className="w-4 h-4" /> Nouveau
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: 'received', label: 'Reçus', icon: Inbox, count: received.length },
          { key: 'sent', label: 'Envoyés', icon: Send, count: sent.length },
          { key: 'team', label: 'Mon équipe', icon: Heart, count: team.length },
        ].map(t => (
          <button key={t.key} onClick={() => setTab(t.key as typeof tab)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              tab === t.key
                ? 'bg-pink-100 dark:bg-pink-500/20 text-pink-700 dark:text-pink-400'
                : 'text-gray-500 hover:bg-gray-100 dark:hover:bg-white/5'
            }`}>
            <t.icon className="w-4 h-4" /> {t.label}
            {t.count > 0 && <span className="badge text-[10px]">{t.count}</span>}
          </button>
        ))}
      </div>

      {/* Content */}
      {(receivedLoading || sentLoading || teamLoading) ? (
        <div className="flex justify-center py-12"><Loader2 className="w-6 h-6 animate-spin" /></div>
      ) : tab === 'team' ? (
        <div className="space-y-3">
          {team.map(member => (
            <div key={member.userId} className="glass-card px-5 py-4 flex items-center gap-4">
              <div className="w-11 h-11 rounded-full bg-gradient-to-br from-pink-500 to-rose-600 flex items-center justify-center text-white font-bold text-sm">
                {member.name.split(' ').map(n => n[0]).join('').slice(0, 2)}
              </div>
              <div className="flex-1 min-w-0">
                <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{member.name}</span>
                <p className="text-xs text-gray-400">{member.role} • {member.encouragementsReceived} encouragements reçus</p>
              </div>
              <button onClick={() => { setNewEnc({ ...newEnc, recipientId: member.userId }); setShowCompose(true); }}
                className="btn-primary btn-sm">
                <Heart className="w-3.5 h-3.5" /> Encourager
              </button>
            </div>
          ))}
          {team.length === 0 && (
            <div className="glass-card p-10 text-center">
              <Heart className="w-10 h-10 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Aucun membre dans votre équipe</p>
            </div>
          )}
        </div>
      ) : tab === 'received' ? (
        <div className="space-y-3">
          {received.map(enc => <EncouragementCard key={enc.id} enc={enc} />)}
          {received.length === 0 && (
            <div className="glass-card p-10 text-center">
              <Inbox className="w-10 h-10 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Aucun encouragement reçu</p>
            </div>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {sent.map(enc => <EncouragementCard key={enc.id} enc={enc} />)}
          {sent.length === 0 && (
            <div className="glass-card p-10 text-center">
              <Send className="w-10 h-10 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Aucun encouragement envoyé</p>
            </div>
          )}
        </div>
      )}

      {/* Compose Modal */}
      {showCompose && (
        <div className="modal-overlay" onClick={() => setShowCompose(false)}>
          <div className="modal-content max-w-md" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">Envoyer un encouragement</h3>
              <button className="btn-icon" onClick={() => setShowCompose(false)}><X className="w-5 h-5 text-gray-400" /></button>
            </div>
            <div className="modal-body space-y-4">
              <div>
                <label className="label">Destinataire</label>
                <select className="input" value={newEnc.recipientId} onChange={e => setNewEnc({ ...newEnc, recipientId: e.target.value })}>
                  <option value="">Choisir un membre</option>
                  {team.map(m => <option key={m.userId} value={m.userId}>{m.name}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Type</label>
                <div className="grid grid-cols-3 gap-2">
                  {ENCOURAGEMENT_TYPES.map(t => (
                    <button key={t.value} onClick={() => setNewEnc({ ...newEnc, type: t.value })}
                      className={`flex items-center gap-2 p-2 rounded-xl text-xs transition-all ${
                        newEnc.type === t.value
                          ? `bg-${t.color}-100 dark:bg-${t.color}-900/30 ring-2 ring-${t.color}-500`
                          : 'bg-gray-50 dark:bg-gray-800/40 hover:bg-gray-100'
                      }`}>
                      <span>{t.icon}</span> {t.label}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="label">Message</label>
                <textarea className="input min-h-[100px]" placeholder="Écrivez un mot d'encouragement..."
                  value={newEnc.message} onChange={e => setNewEnc({ ...newEnc, message: e.target.value })} />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setShowCompose(false)}>Annuler</button>
              <button className="btn-primary btn-sm" onClick={() => sendMutation.mutate()}
                disabled={!newEnc.recipientId || !newEnc.message || sendMutation.isPending}>
                {sendMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                Envoyer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
