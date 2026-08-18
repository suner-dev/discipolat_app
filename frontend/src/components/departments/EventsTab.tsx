import { useState } from 'react';
import { createPortal } from 'react-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import {
  CalendarDays, Plus, Loader2, Users2, Network, Briefcase,
  ListTodo, UserCheck, UserX, Download, X,
} from 'lucide-react';
import { STATUT_TASK_BADGE } from './types';

function GlobalSearchResults({ results, deptId, onClear }: { results: any; deptId: string; onClear: () => void }) {
  const navigate = useNavigate();
  if (!results) {
    return (
      <div className="glass-card p-5 mb-5 flex items-center gap-3">
        <Loader2 className="w-4 h-4 animate-spin text-primary-500" /> Recherche en cours…
      </div>
    );
  }
  const total = results.total ?? 0;
  const sections = [
    { key: 'membres', label: 'Membres', items: results.membres ?? [], render: (m: any) => (
      <button key={m.id} onClick={() => { navigate(`/departments/${deptId}/members/${m.id}`); onClear(); }}
        className="w-full text-left px-3 py-2 hover:bg-gray-100 dark:hover:bg-gray-700/60 rounded-lg flex items-center justify-between gap-2 cursor-pointer">
        <span className="flex items-center gap-2 text-sm">
          <Users2 className="w-4 h-4 text-primary-500" /> {m.nomComplet}
        </span>
        <span className="text-[11px] text-gray-400">{m.statut}</span>
      </button>
    ) },
    { key: 'equipes', label: 'Équipes', items: results.equipes ?? [], render: (t: any) => (
      <div key={t.id} className="px-3 py-2 flex items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-sm"><Network className="w-4 h-4 text-amber-500" /> {t.nom}</span>
        <span className="badge badge-gray text-[10px]">{t.nbMembres} membre{t.nbMembres > 1 ? 's' : ''}</span>
      </div>
    ) },
    { key: 'postes', label: 'Postes', items: results.postes ?? [], render: (p: any) => (
      <div key={p.id} className="px-3 py-2 flex items-center gap-2 text-sm">
        <Briefcase className="w-4 h-4 text-sky-500" /> {p.nom}
      </div>
    ) },
    { key: 'taches', label: 'Tâches', items: results.taches ?? [], render: (t: any) => (
      <div key={t.id} className="px-3 py-2 flex items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-sm"><ListTodo className="w-4 h-4 text-emerald-500" /> {t.titre}</span>
        <span className={`badge text-[10px] ${STATUT_TASK_BADGE[t.statut] || 'badge-gray'}`}>{t.statut}</span>
      </div>
    ) },
    { key: 'evenements', label: 'Événements', items: results.evenements ?? [], render: (e: any) => (
      <div key={e.id} className="px-3 py-2 flex items-center justify-between gap-2">
        <span className="flex items-center gap-2 text-sm"><CalendarDays className="w-4 h-4 text-violet-500" /> {e.titre}</span>
        {e.dateDebut && <span className="text-[11px] text-gray-400">{formatEventDate(e.dateDebut)}</span>}
      </div>
    ) },
  ];
  return (
    <div className="glass-card p-5 mb-5">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">
          {total > 0 ? `${total} résultat${total > 1 ? 's' : ''}` : 'Aucun résultat'} pour cette recherche
        </h3>
        <button onClick={onClear} className="text-xs text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 cursor-pointer">
          <X className="w-4 h-4" />
        </button>
      </div>
      {total === 0 ? (
        <p className="text-sm text-gray-400">Essayez un nom, un poste, une équipe, une tâche ou un événement.</p>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {sections.filter((s) => s.items.length > 0).map((s) => (
            <div key={s.key}>
              <p className="text-[11px] uppercase tracking-wide text-gray-400 mb-1">{s.label} ({s.items.length})</p>
              <div className="space-y-0.5">{s.items.map(s.render)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function formatEventDate(value: string) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }) +
    ' · ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
}

// ============================================================
// ÉVÉNEMENTS DU DÉPARTEMENT
// ============================================================

type DeptEvent = {
  id: string; titre: string; typeEvenement: string; lieu?: string;
  dateDebut: string; dateFin?: string; statut: string; nbInscrits: number; description?: string;
};

const EVENT_TYPES = ['SORTIE', 'RETRAITE', 'EVANGELISATION', 'REUNION', 'VISITE', 'CONFERENCE', 'FORMATION', 'ANNIVERSAIRE', 'CULTE', 'ETUDE_BIBLIQUE', 'VEILLEE', 'PRIERE', 'AUTRE'];
const EVENT_TYPE_LABELS: Record<string, string> = {
  SORTIE: 'Sortie', RETRAITE: 'Retraite', EVANGELISATION: 'Évangélisation', REUNION: 'Réunion',
  VISITE: 'Visite', CONFERENCE: 'Conférence', FORMATION: 'Formation', ANNIVERSAIRE: 'Anniversaire',
  CULTE: 'Culte', ETUDE_BIBLIQUE: 'Étude biblique', VEILLEE: 'Veillée', PRIERE: 'Temps de prière', AUTRE: 'Autre',
};
const EVENT_STATUT_BADGE: Record<string, string> = {
  PLANIFIE: 'badge-info', EN_COURS: 'badge-warning', TERMINE: 'badge-success', ANNULE: 'badge-inactive',
};

export function EventsTab({ deptId, onChanged }: { deptId: string; onChanged: () => void }) {
  const queryClient = useQueryClient();
  const [titre, setTitre] = useState('');
  const [typeEvenement, setTypeEvenement] = useState('REUNION');
  const [lieu, setLieu] = useState('');
  const [dateDebut, setDateDebut] = useState('');
  const [description, setDescription] = useState('');

  const { data: events = [], isLoading } = useQuery({
    queryKey: ['department', deptId, 'events'],
    queryFn: async () => {
      const res = await api.get(`/events/department/${deptId}?size=100`);
      return (res.data?.content ?? []) as DeptEvent[];
    },
    enabled: !!deptId,
  });

  const [attendanceEvent, setAttendanceEvent] = useState<DeptEvent | null>(null);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['department', deptId, 'events'] });
    onChanged();
  };

  const createMutation = useMutation({
    mutationFn: async () => {
      await api.post('/events', {
        typeEvenement, titre, lieu, description,
        dateDebut: new Date(dateDebut).toISOString(),
        departmentId: deptId,
      });
    },
    onSuccess: () => {
      toast.success('Événement créé ✅');
      setTitre(''); setLieu(''); setDateDebut(''); setDescription('');
      invalidate();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const upcoming = events
    .filter((e) => e.statut === 'PLANIFIE' || e.statut === 'EN_COURS')
    .sort((a, b) => new Date(a.dateDebut).getTime() - new Date(b.dateDebut).getTime());
  const past = events
    .filter((e) => e.statut === 'TERMINE' || e.statut === 'ANNULE')
    .sort((a, b) => new Date(b.dateDebut).getTime() - new Date(a.dateDebut).getTime());

  if (isLoading) {
    return <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>;
  }

  return (
    <div className="space-y-4">
      <div className="glass-card p-5">
        <div className="flex items-center gap-2 mb-4">
          <CalendarDays className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouvel événement du département</h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3">
          <div>
            <label className="label">Titre *</label>
            <input className="input" value={titre} onChange={(e) => setTitre(e.target.value)}
              placeholder="Ex : Convention départementale 2026" />
          </div>
          <div>
            <label className="label">Type</label>
            <select className="input" value={typeEvenement} onChange={(e) => setTypeEvenement(e.target.value)}>
              {EVENT_TYPES.map((t) => <option key={t} value={t}>{EVENT_TYPE_LABELS[t]}</option>)}
            </select>
          </div>
          <div>
            <label className="label">Date et heure *</label>
            <input type="datetime-local" className="input" value={dateDebut} onChange={(e) => setDateDebut(e.target.value)} />
          </div>
          <div>
            <label className="label">Lieu</label>
            <input className="input" value={lieu} onChange={(e) => setLieu(e.target.value)} placeholder="Ex : Temple principal" />
          </div>
        </div>
        <textarea className="input mb-3 min-h-[70px]" value={description} onChange={(e) => setDescription(e.target.value)}
          placeholder="Description, participants attendus, équipes mobilisées…" />
        <button onClick={() => createMutation.mutate()} disabled={!titre.trim() || !dateDebut || createMutation.isPending}
          className="btn-primary btn-sm cursor-pointer">
          <Plus className="w-4 h-4" /> Créer l'événement
        </button>
      </div>

      <div className="glass-card p-5">
        <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">
          À venir ({upcoming.length})
        </h3>
        {upcoming.length === 0 ? (
          <p className="text-sm text-gray-400">Aucun événement planifié. Créez le premier événement de votre département.</p>
        ) : (
          <div className="space-y-2">
            {upcoming.map((e) => (
              <div key={e.id} className="flex items-center justify-between gap-3 p-3 rounded-xl border border-gray-100 dark:border-gray-700/60 hover:bg-gray-50 dark:hover:bg-gray-800/50">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="p-2 rounded-lg bg-primary-500/10 text-primary-600 dark:text-primary-300 shrink-0">
                    <CalendarDays className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{e.titre}</p>
                    <p className="text-[11px] text-gray-400">
                      {EVENT_TYPE_LABELS[e.typeEvenement] ?? e.typeEvenement} · {formatEventDate(e.dateDebut)}
                      {e.lieu ? ` · ${e.lieu}` : ''}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    onClick={() => setAttendanceEvent(e)}
                    className="btn-ghost btn-xs inline-flex cursor-pointer"
                    title="Pointer la présence des membres à cet événement"
                  >
                    <UserCheck className="w-3.5 h-3.5" /> Présences
                  </button>
                  <span className={`badge text-[10px] ${EVENT_STATUT_BADGE[e.statut] || 'badge-gray'}`}>{e.statut}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {past.length > 0 && (
        <div className="glass-card p-5">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">Passés ({past.length})</h3>
          <div className="space-y-1.5">
            {past.map((e) => (
              <div key={e.id} className="flex items-center justify-between gap-3 px-3 py-2 text-sm">
                <span className="text-gray-700 dark:text-gray-200 truncate">{e.titre}</span>
                <div className="flex items-center gap-2 shrink-0">
                  <button
                    onClick={() => setAttendanceEvent(e)}
                    className="btn-ghost btn-xs inline-flex cursor-pointer"
                    title="Pointer la présence des membres à cet événement"
                  >
                    <UserCheck className="w-3.5 h-3.5" /> Présences
                  </button>
                  <span className="text-[11px] text-gray-400">{formatEventDate(e.dateDebut)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {attendanceEvent && (
        <EventAttendanceModal deptId={deptId} event={attendanceEvent} onClose={() => setAttendanceEvent(null)} />
      )}
    </div>
  );
}

// ============================================================
// PRÉSENCE DES MEMBRES À UN ÉVÉNEMENT
// ============================================================

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function EventAttendanceModal({ deptId, event, onClose }: { deptId: string; event: DeptEvent; onClose: () => void }) {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['department', deptId, 'events', event.id, 'attendance'],
    queryFn: async () => (await api.get(`/departments/${deptId}/events/${event.id}/attendance`)).data as any,
    enabled: !!deptId,
  });

  const markMutation = useMutation({
    mutationFn: async ({ soulId, present }: { soulId: string; present: boolean }) =>
      (await api.put(`/departments/${deptId}/events/${event.id}/attendance`, { soulId, present })).data,
    onSuccess: () => {
      toast.success('Présence enregistrée ✅');
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'events', event.id, 'attendance'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const markAllMutation = useMutation({
    mutationFn: async () =>
      (await api.post(`/departments/${deptId}/events/${event.id}/attendance/mark-all?present=true`)).data,
    onSuccess: (r) => {
      toast.success(`${r?.marques ?? 0} membres marqués présents ✅`);
      queryClient.invalidateQueries({ queryKey: ['department', deptId, 'events', event.id, 'attendance'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const downloadCsv = () => {
    api.get(`/departments/${deptId}/events/${event.id}/attendance/export`, { responseType: 'blob' })
      .then((res) => {
        const url = URL.createObjectURL(res.data as Blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `feuille-presence-${event.titre.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 40) || 'evenement'}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        toast.success('Feuille de présence exportée 📥');
      })
      .catch((err) => toast.error(getErrorMessage(err)));
  };

  const membres: any[] = data?.membres ?? [];

  return createPortal(
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div className="glass-card p-5 w-full max-w-xl max-h-[85vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center justify-between mb-1">
          <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <UserCheck className="w-4 h-4 text-primary-500" />
            Présences — {event.titre}
          </h3>
          <button onClick={onClose} className="p-1 rounded-lg text-gray-400 hover:text-gray-600 cursor-pointer">
            <X className="w-5 h-5" />
          </button>
        </div>
        <p className="text-[11px] text-gray-400 mb-4">
          Pointez chaque membre présent ou absent. Le responsable du département, le chef de famille
          ou le faiseur de l'âme peuvent marquer la présence.
        </p>

        {isLoading ? (
          <div className="flex justify-center py-10"><Loader2 className="w-6 h-6 animate-spin text-primary-500" /></div>
        ) : (
          <>
            <div className="flex flex-wrap gap-3 mb-4 text-xs text-gray-500">
              <span>Total : <strong className="text-gray-800 dark:text-gray-100">{data?.total ?? 0}</strong></span>
              <span>Présents : <strong className="text-green-600">{data?.presents ?? 0}</strong></span>
              <span>Absents : <strong className="text-red-500">{data?.absents ?? 0}</strong></span>
              <span>Non pointés : <strong className="text-amber-600">{data?.nonMarques ?? 0}</strong></span>
            </div>
            {membres.length === 0 ? (
              <p className="text-sm text-gray-400 text-center py-6">Aucun membre dans ce département</p>
            ) : (
              <div className="space-y-1.5 max-h-[50vh] overflow-y-auto pr-1">
                {membres.map((m: any) => {
                  const isPresent = m.present === true;
                  const isAbsent = m.present === false;
                  return (
                    <div key={m.soulId} className="flex items-center justify-between gap-3 p-2.5 rounded-xl bg-gray-50 dark:bg-gray-800/40">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
                          isPresent ? 'bg-emerald-100 text-emerald-700' : isAbsent ? 'bg-red-100 text-red-700' : 'bg-gray-200 text-gray-500'
                        }`}>
                          {initials(m.nom)}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{m.nom}</p>
                          <p className={`text-[10px] font-medium ${isPresent ? 'text-emerald-600' : isAbsent ? 'text-red-500' : 'text-gray-400'}`}>
                            {isPresent ? '✓ Présent' : isAbsent ? '✗ Absent' : 'Non pointé'}
                          </p>
                        </div>
                      </div>
                      <div className="flex gap-1 shrink-0">
                        <button
                          onClick={() => markMutation.mutate({ soulId: m.soulId, present: true })}
                          disabled={markMutation.isPending}
                          className={`p-2 rounded-lg transition-colors cursor-pointer ${
                            isPresent
                              ? 'bg-emerald-100 text-emerald-700'
                              : 'text-gray-400 hover:bg-emerald-50 hover:text-emerald-600'
                          }`}
                          title="Marquer présent"
                        >
                          <UserCheck className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => markMutation.mutate({ soulId: m.soulId, present: false })}
                          disabled={markMutation.isPending}
                          className={`p-2 rounded-lg transition-colors cursor-pointer ${
                            isAbsent
                              ? 'bg-red-100 text-red-700'
                              : 'text-gray-400 hover:bg-red-50 hover:text-red-600'
                          }`}
                          title="Marquer absent"
                        >
                          <UserX className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            <div className="flex flex-wrap items-center justify-between gap-2 mt-4 pt-3 border-t border-gray-100 dark:border-gray-700/60">
              <button
                onClick={() => markAllMutation.mutate()}
                disabled={markAllMutation.isPending || membres.length === 0}
                className="btn-secondary btn-sm cursor-pointer"
                title="Marquer tous les membres présents à cet événement"
              >
                {markAllMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <UserCheck className="w-4 h-4" />}
                Marquer tous présents
              </button>
              <button
                onClick={downloadCsv}
                className="btn-ghost btn-sm cursor-pointer"
                title="Exporter la feuille de présence en CSV"
              >
                <Download className="w-4 h-4" /> Exporter CSV
              </button>
            </div>
          </>
        )}
      </div>
    </div>,
    document.body
  );
}

