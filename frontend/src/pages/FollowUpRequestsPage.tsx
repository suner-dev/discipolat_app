import { formatEnum } from '@/lib/labels';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { HandHeart, Send, Loader2, CheckCircle2 } from 'lucide-react';

interface FollowUpRequest { id: string; requesterName?: string; type: string; message: string; status: string; assignedToName?: string; createdAt: string; }

const TYPES = ['FAISEUR', 'ACCOMPAGNEMENT_SPIRITUEL', 'CONSEIL_PASTORAL'] as const;
const STATUS_STYLE: Record<string, string> = {
  EN_ATTENTE: 'text-yellow-400 bg-yellow-500/20', ASSIGNEE: 'text-blue-400 bg-blue-500/20',
  EN_COURS: 'text-cyan-400 bg-cyan-500/20', TERMINEE: 'text-green-400 bg-green-500/20', REJETEE: 'text-red-400 bg-red-500/20',
};

/** P3 #112 — Demandes de suivi : le membre demande un faiseur ou un accompagnement spirituel depuis l'app. */
export default function FollowUpRequestsPage() {
  const qc = useQueryClient();
  const [type, setType] = useState<string>('FAISEUR');
  const [message, setMessage] = useState('');
  const [tab, setTab] = useState<'mine' | 'assigned'>('mine');

  const mineQ = useQuery({ queryKey: ['follow-up', 'mine'], queryFn: async () => (await api.get('/follow-up-requests/mine')).data as FollowUpRequest[] });
  const assignedQ = useQuery({ queryKey: ['follow-up', 'assigned'], queryFn: async () => (await api.get('/follow-up-requests/assigned-to-me')).data as FollowUpRequest[] });

  const create = useMutation({
    mutationFn: async () => api.post('/follow-up-requests', { type, message }),
    onSuccess: () => { setMessage(''); qc.invalidateQueries({ queryKey: ['follow-up'] }); },
  });
  const complete = useMutation({
    mutationFn: async (id: string) => api.patch(`/follow-up-requests/${id}/status`, { status: 'TERMINEE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['follow-up'] }),
  });

  const list = tab === 'mine' ? mineQ.data ?? [] : assignedQ.data ?? [];

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><HandHeart className="text-pink-400" /> Demandes de suivi spirituel</h1>

      <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
        <h2 className="text-white font-semibold mb-3">Nouvelle demande</h2>
        <div className="flex flex-col md:flex-row gap-3">
          <select value={type} onChange={(e) => setType(e.target.value)} className="bg-black/30 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-200">
            {TYPES.map((t) => <option key={t} value={t}>{t === 'FAISEUR' ? 'Demander un faiseur' : t === 'ACCOMPAGNEMENT_SPIRITUEL' ? 'Accompagnement spirituel' : 'Conseil pastoral'}</option>)}
          </select>
          <input value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Décrivez votre besoin en quelques mots…"
            className="flex-1 bg-black/30 border border-white/10 rounded-lg px-3 py-2 text-sm text-gray-200 focus:outline-none focus:border-pink-500/50" />
          <button onClick={() => create.mutate()} disabled={create.isPending || !type}
            className="flex items-center justify-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-pink-600 to-rose-600 text-white text-sm font-medium hover:opacity-90 disabled:opacity-50">
            {create.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />} Envoyer
          </button>
        </div>
      </div>

      <div className="flex gap-2">
        <button onClick={() => setTab('mine')} className={`px-4 py-2 rounded-xl text-sm font-medium ${tab === 'mine' ? 'bg-white/10 text-white' : 'text-gray-400 hover:text-white'}`}>Mes demandes ({mineQ.data?.length ?? 0})</button>
        <button onClick={() => setTab('assigned')} className={`px-4 py-2 rounded-xl text-sm font-medium ${tab === 'assigned' ? 'bg-white/10 text-white' : 'text-gray-400 hover:text-white'}`}>Assignées à moi ({assignedQ.data?.length ?? 0})</button>
      </div>

      <div className="space-y-2">
        {list.length === 0 && <p className="text-sm text-gray-500">{tab === 'mine' ? "Vous n'avez pas encore fait de demande." : 'Aucune demande vous est assignée.'}</p>}
        {list.map((r) => (
          <div key={r.id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10 flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-white font-medium">{r.type === 'FAISEUR' ? 'Demande de faiseur' : r.type === 'ACCOMPAGNEMENT_SPIRITUEL' ? 'Accompagnement spirituel' : 'Conseil pastoral'}</p>
              {r.message && <p className="text-xs text-gray-400 mt-0.5">{r.message}</p>}
              <p className="text-[11px] text-gray-600 mt-1">{new Date(r.createdAt).toLocaleDateString('fr-FR')}{r.requesterName ? ` • ${r.requesterName}` : ''}{r.assignedToName ? ` • assignée à ${r.assignedToName}` : ''}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className={`px-2 py-0.5 rounded-full text-xs ${STATUS_STYLE[r.status] ?? 'text-gray-400 bg-gray-500/20'}`}>{formatEnum(r.status)}</span>
              {tab === 'assigned' && r.status !== 'TERMINEE' && (
                <button onClick={() => complete.mutate(r.id)} disabled={complete.isPending} className="flex items-center gap-1 text-green-400 hover:text-green-300 text-xs"><CheckCircle2 className="w-4 h-4" /> Marquer terminée</button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
