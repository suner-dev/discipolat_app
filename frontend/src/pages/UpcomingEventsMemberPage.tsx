import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Calendar, Check, Star, X } from 'lucide-react';

interface MemberEvent { eventId?: string; id?: string; titre?: string; title?: string; dateDebut?: string; date?: string; type?: string; rsvp?: string; }

const RSVP_LABEL: Record<string, string> = { GOING: '✅ J\'y vais', INTERESTED: '⭐ Intéressé', CANCEL: '— Annulé' };

/** P3 #113 — Calendrier personnel des événements à venir du membre avec RSVP (API réelle). */
export default function UpcomingEventsMemberPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['upcoming-events-mine'],
    queryFn: async () => (await api.get('/events/upcoming/mine', { params: { days: 60 } })).data as MemberEvent[],
  });

  const setRsvp = useMutation({
    mutationFn: async ({ eventId, rsvp }: { eventId: string; rsvp: string }) => api.put(`/events/${eventId}/rsvp`, { rsvp }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['upcoming-events-mine'] }),
  });

  if (isLoading) return <div className="p-6 text-gray-400">Chargement de votre calendrier…</div>;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Calendar className="text-cyan-400" /> Mes événements à venir</h1>
      {(data ?? []).length === 0 && <p className="text-sm text-gray-500">Aucun événement à venir dans les 60 prochains jours.</p>}
      <div className="space-y-3">
        {(data ?? []).map((e) => {
          const id = e.eventId ?? e.id ?? '';
          const dateStr = e.dateDebut ?? e.date ?? '';
          const d = dateStr ? new Date(dateStr) : null;
          return (
            <div key={id} className="bg-white/5 backdrop-blur rounded-xl p-4 border border-white/10 flex flex-wrap items-center gap-4 hover:border-cyan-500/30 transition">
              <div className="w-14 h-14 bg-gradient-to-br from-cyan-500/20 to-blue-500/20 rounded-xl flex flex-col items-center justify-center border border-cyan-500/20 shrink-0">
                <span className="text-xs text-cyan-400 font-bold">{d ? d.toLocaleDateString('fr-FR', { weekday: 'short' }) : '—'}</span>
                <span className="text-lg font-bold text-white">{d ? d.getDate() : '?'}</span>
              </div>
              <div className="flex-1 min-w-[140px]">
                <h3 className="text-white font-medium">{e.titre ?? e.title ?? 'Événement'}</h3>
                <p className="text-xs text-gray-400">{e.type ? `${e.type} • ` : ''}{dateStr}{d ? ` • ${d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}` : ''}</p>
              </div>
              <div className="flex items-center gap-1">
                <button onClick={() => setRsvp.mutate({ eventId: id, rsvp: 'GOING' })} disabled={setRsvp.isPending}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium ${e.rsvp === 'GOING' ? 'bg-green-500/30 text-green-300' : 'text-gray-400 hover:bg-green-500/10 hover:text-green-300'}`}><Check className="w-3 h-3 inline" /> Y vais</button>
                <button onClick={() => setRsvp.mutate({ eventId: id, rsvp: 'INTERESTED' })} disabled={setRsvp.isPending}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium ${e.rsvp === 'INTERESTED' ? 'bg-blue-500/30 text-blue-300' : 'text-gray-400 hover:bg-blue-500/10 hover:text-blue-300'}`}><Star className="w-3 h-3 inline" /> Intéressé</button>
                {e.rsvp && e.rsvp !== 'CANCEL' && (
                  <button onClick={() => setRsvp.mutate({ eventId: id, rsvp: 'CANCEL' })} disabled={setRsvp.isPending}
                    className="px-2 py-1.5 rounded-lg text-xs text-gray-500 hover:text-red-400" title="Annuler ma participation"><X className="w-3 h-3" /></button>
                )}
              </div>
              {e.rsvp === 'GOING' && <span className="text-xs text-green-400 w-full md:w-auto">{RSVP_LABEL.GOING}</span>}
            </div>
          );
        })}
      </div>
    </div>
  );
}
