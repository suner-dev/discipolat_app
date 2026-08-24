import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Users, Heart, Send } from 'lucide-react';
import { useModalFocus } from '@/hooks/useModalFocus';

interface TeamMember { soulId?: string; userId?: string; nom?: string; prenom?: string; etatSpirituel?: string; familleNom?: string; encouragementsRecus?: number; estMoi?: boolean; }
interface Encouragement { id?: string; message: string; kind?: string; createdAt?: string; }

/** P3 #115 — Mon équipe / ma famille : membres + envoi d'encouragements (API réelle). */
export default function MyTeamFamilyPage() {
  const qc = useQueryClient();
  const [selected, setSelected] = useState<TeamMember | null>(null);
  const [message, setMessage] = useState('');
  const modalRef = useModalFocus<HTMLDivElement>(!!selected);

  const teamQ = useQuery({ queryKey: ['my-team'], queryFn: async () => (await api.get('/encouragements/my-team')).data as TeamMember[] });
  const receivedQ = useQuery({ queryKey: ['encouragements-received'], queryFn: async () => (await api.get('/encouragements/received')).data as Encouragement[] });

  const send = useMutation({
    mutationFn: async () => api.post('/encouragements', { toUserId: selected?.userId, message }),
    onSuccess: () => { setMessage(''); setSelected(null); qc.invalidateQueries({ queryKey: ['my-team'] }); qc.invalidateQueries({ queryKey: ['encouragements-received'] }); },
  });

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Users className="text-blue-400" /> Mon équipe / ma famille</h1>

      {(teamQ.data ?? []).length === 0 ? (
        <p className="text-sm text-gray-500">Vous n'êtes rattaché à aucune famille spirituelle pour le moment.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {(teamQ.data ?? []).map((m, i) => (
            <div key={m.soulId ?? i} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10 hover:border-blue-500/30 transition">
              <div className="flex items-center justify-between mb-2">
                <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500/30 to-indigo-500/30 flex items-center justify-center text-white font-bold">
                  {(m.prenom ?? m.nom ?? '?').charAt(0).toUpperCase()}
                </div>
                <span className="text-xs text-pink-300 flex items-center gap-1"><Heart className="w-3 h-3" /> {m.encouragementsRecus ?? 0}</span>
              </div>
              <h3 className="text-white font-medium">{(m.prenom ?? '') + ' ' + (m.nom ?? '')}{m.estMoi && <span className="text-gray-500 text-xs"> (moi)</span>}</h3>
              <p className="text-xs text-gray-400">{m.etatSpirituel ?? '—'}{m.familleNom ? ` • ${m.familleNom}` : ''}</p>
              {!m.estMoi && m.userId && (
                <button onClick={() => setSelected(m)} className="mt-3 w-full py-1.5 rounded-lg text-xs font-medium text-pink-300 bg-pink-500/10 hover:bg-pink-500/20 transition flex items-center justify-center gap-1">
                  <Heart className="w-3 h-3" /> Envoyer un encouragement
                </button>
              )}
            </div>
          ))}
        </div>
      )}

      {(receivedQ.data ?? []).length > 0 && (
        <div className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10">
          <h2 className="text-white font-semibold mb-3">Encouragements reçus</h2>
          <div className="space-y-2 max-h-60 overflow-auto">
            {(receivedQ.data ?? []).map((e, i) => (
              <div key={e.id ?? i} className="bg-black/20 rounded-lg px-3 py-2 text-sm">
                <p className="text-gray-200">{e.message}</p>
                {e.createdAt && <p className="text-[11px] text-gray-500 mt-0.5">{new Date(e.createdAt).toLocaleDateString('fr-FR')}</p>}
              </div>
            ))}
          </div>
        </div>
      )}

      {selected && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50" onClick={() => setSelected(null)}>
          <div ref={modalRef} role="dialog" aria-modal="true" aria-label="Envoyer un encouragement" className="bg-gray-900 rounded-2xl p-6 w-full max-w-md border border-white/10" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-white font-semibold mb-3">Encourager {(selected.prenom ?? '') + ' ' + (selected.nom ?? '')}</h3>
            <textarea value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Un mot d'encouragement…" rows={4}
              className="w-full bg-black/30 border border-white/10 rounded-xl p-3 text-sm text-gray-200 focus:outline-none focus:border-pink-500/50" />
            <div className="flex justify-end gap-2 mt-4">
              <button onClick={() => setSelected(null)} className="px-4 py-2 rounded-xl text-sm text-gray-400 hover:text-white">Annuler</button>
              <button onClick={() => send.mutate()} disabled={send.isPending || !message.trim()}
                className="flex items-center gap-2 px-4 py-2 rounded-xl bg-gradient-to-r from-pink-600 to-rose-600 text-white text-sm font-medium disabled:opacity-50">
                <Send className="w-4 h-4" /> Envoyer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
