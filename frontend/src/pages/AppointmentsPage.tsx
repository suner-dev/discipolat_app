import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import toast from 'react-hot-toast';
import {
  CalendarPlus,
  CalendarClock,
  CheckCircle2,
  XCircle,
  Inbox,
  Clock,
  CalendarCheck2,
  Search,
  Filter,
  Download,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import type {
  Appointment,
  AppointmentMotif,
  AppointmentStatut,
  CreateAppointmentRequest,
  User,
} from '@/types';

/** Repli (dictionnaire indisponible) — les valeurs réelles viennent de la base. */
const MOTIF_FALLBACK: Record<AppointmentMotif, string> = {
  CONSEIL: 'Conseil',
  CONFESSION: 'Confession',
  SUIVI: 'Suivi',
  FORMATION: 'Formation',
  AUTRE: 'Autre',
};

const STATUT_STYLE: Record<AppointmentStatut, { label: string; cls: string }> = {
  EN_ATTENTE: { label: 'En attente', cls: 'bg-amber-500/10 text-amber-600 dark:text-amber-400' },
  CONFIRME: { label: 'Confirmé', cls: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400' },
  REFUSE: { label: 'Refusé', cls: 'bg-red-500/10 text-red-600 dark:text-red-400' },
  ANNULE: { label: 'Annulé', cls: 'bg-gray-500/10 text-gray-500 dark:text-gray-400' },
  TERMINE: { label: 'Terminé', cls: 'bg-primary-500/10 text-primary-600 dark:text-primary-400' },
};

export default function AppointmentsPage() {
  const { user } = useAuth();
  const dictionaries = useDictionaries();
  const queryClient = useQueryClient();

  const [showCreate, setShowCreate] = useState(false);
  const [search, setSearch] = useState('');
  const [statutFilter, setStatutFilter] = useState<AppointmentStatut | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [form, setForm] = useState<CreateAppointmentRequest>({
    recepteurId: '', motif: 'CONSEIL', objet: '', datePrevue: '', dureeMinutes: 30,
  });

  const requestsQuery = useQuery({
    queryKey: ['appointments', 'my'],
    queryFn: async () => {
      const res = await api.get('/appointments/my');
      return res.data as Appointment[];
    },
  });

  const inboxQuery = useQuery({
    queryKey: ['appointments', 'inbox'],
    queryFn: async () => {
      const res = await api.get('/appointments/inbox');
      return res.data as Appointment[];
    },
  });

  const usersQuery = useQuery({
    queryKey: ['users', 'light'],
    queryFn: async () => {
      const res = await api.get('/users', { params: { size: 100 } });
      return (res.data as { content: User[] }).content;
    },
    enabled: showCreate,
  });

  const createMutation = useMutation({
    mutationFn: async (payload: CreateAppointmentRequest) => {
      const res = await api.post('/appointments', payload);
      return res.data as Appointment;
    },
    onSuccess: () => {
      toast.success('Rendez-vous demandé !');
      setShowCreate(false);
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: { statut: AppointmentStatut; reponse?: string } }) => {
      const res = await api.patch(`/appointments/${id}/status`, payload);
      return res.data as Appointment;
    },
    onSuccess: () => {
      toast.success('Rendez-vous mis à jour');
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const allRequests = requestsQuery.data ?? [];
  const allInbox = inboxQuery.data ?? [];

  // Filter
  const requests = allRequests.filter(a => {
    if (search && !a.recepteurNom?.toLowerCase().includes(search.toLowerCase()) && !a.objet?.toLowerCase().includes(search.toLowerCase())) return false;
    if (statutFilter && a.statut !== statutFilter) return false;
    return true;
  });
  const inbox = allInbox.filter(a => {
    if (search && !a.demandeurNom?.toLowerCase().includes(search.toLowerCase()) && !a.objet?.toLowerCase().includes(search.toLowerCase())) return false;
    if (statutFilter && a.statut !== statutFilter) return false;
    return true;
  });

  // Stats
  const allApps = [...allRequests, ...allInbox];
  const appStats = {
    total: allApps.length,
    enAttente: allApps.filter(a => a.statut === 'EN_ATTENTE').length,
    confirms: allApps.filter(a => a.statut === 'CONFIRME').length,
    termines: allApps.filter(a => a.statut === 'TERMINE').length,
  };

  // Export CSV
  const exportCsv = () => {
    const rows = [['Type', 'Avec', 'Motif', 'Date', 'Durée', 'Statut', 'Objet']];
    allApps.forEach((a) => {
      rows.push([
        a.demandeurId === user?.id ? 'Envoyé' : 'Reçu',
        a.demandeurId === user?.id ? (a.recepteurNom || '') : (a.demandeurNom || ''),
        a.motif,
        formatDate(a.datePrevue),
        `${a.dureeMinutes} min`,
        STATUT_STYLE[a.statut].label,
        a.objet || '',
      ]);
    });
    const csv = rows.map(r => r.map(c => `"${c}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `rendez_vous_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };
  const recipients = (usersQuery.data ?? []).filter(u =>
    u.roles.some(r => ['PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'].includes(r)));

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleString('fr-FR', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });

  const AppointmentRow = ({ a, side }: { a: Appointment; side: 'sent' | 'received' }) => {
    const st = STATUT_STYLE[a.statut];
    const isReceived = side === 'received';
    return (
      <div className="px-4 py-3 hover:bg-gray-50/60 dark:hover:bg-gray-800/30 transition-colors">
        <div className="flex flex-wrap items-center gap-3">
          <span className="p-2 rounded-lg bg-primary-500/10 text-primary-500">
            <CalendarClock className="w-4 h-4" />
          </span>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                {isReceived ? a.demandeurNom : a.recepteurNom ?? '—'}
              </p>
              <span className={`inline-flex items-center text-[10px] font-bold px-2 py-0.5 rounded-full ${st.cls}`}>
                {st.label}
              </span>
            </div>
            <p className="text-xs text-gray-400 mt-0.5 flex items-center gap-1.5">
              <Clock className="w-3 h-3" /> {formatDate(a.datePrevue)} · {dictionaries.label('APPOINTMENT_MOTIF', a.motif) || MOTIF_FALLBACK[a.motif] || a.motif} · {a.dureeMinutes} min
            </p>
            {a.objet && <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{a.objet}</p>}
            {a.reponse && (
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 bg-gray-50 dark:bg-gray-800/50 rounded-lg p-1.5">
                <span className="font-medium">Réponse :</span> {a.reponse}
              </p>
            )}
          </div>
          <div className="flex items-center gap-1">
            {isReceived && a.statut === 'EN_ATTENTE' && (
              <>
                <button
                  onClick={() => updateMutation.mutate({ id: a.id, payload: { statut: 'CONFIRME' } })}
                  className="p-2 rounded-lg text-emerald-500 hover:bg-emerald-50 dark:hover:bg-emerald-500/10 transition-colors"
                  title="Confirmer"
                >
                  <CheckCircle2 className="w-4 h-4" />
                </button>
                <button
                  onClick={() => updateMutation.mutate({ id: a.id, payload: { statut: 'REFUSE' } })}
                  className="p-2 rounded-lg text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                  title="Refuser"
                >
                  <XCircle className="w-4 h-4" />
                </button>
              </>
            )}
            {!isReceived && a.statut === 'EN_ATTENTE' && (
              <button
                onClick={() => updateMutation.mutate({ id: a.id, payload: { statut: 'ANNULE' } })}
                className="p-2 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors"
                title="Annuler ma demande"
              >
                <XCircle className="w-4 h-4" />
              </button>
            )}
            {a.statut === 'CONFIRME' && (
              <button
                onClick={() => updateMutation.mutate({ id: a.id, payload: { statut: 'TERMINE' } })}
                className="p-2 rounded-lg text-primary-500 hover:bg-primary-50 dark:hover:bg-primary-500/10 transition-colors"
                title="Marquer comme terminé"
              >
                <CalendarCheck2 className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="animate-fade-in space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">Rendez-vous</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            Prenez rendez-vous avec le pasteur, votre chef de famille ou votre faiseur
          </p>
        </div>
        <div className="flex gap-2">
          <button onClick={exportCsv} className="btn-secondary btn-sm">
            <Download className="w-4 h-4" /> Export
          </button>
          <button onClick={() => setShowCreate(v => !v)} className="btn btn-primary flex items-center gap-2">
            <CalendarPlus className="w-4 h-4" /> Prendre rendez-vous
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
        {[
          { label: 'Total', value: appStats.total, color: 'from-primary-500 to-primary-600' },
          { label: 'En attente', value: appStats.enAttente, color: 'from-amber-500 to-orange-500' },
          { label: 'Confirmés', value: appStats.confirms, color: 'from-emerald-500 to-green-500' },
          { label: 'Terminés', value: appStats.termines, color: 'from-violet-500 to-purple-500' },
        ].map((s, i) => (
          <div key={s.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 50}ms` }}>
            <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${s.color} opacity-60`} />
            <span className="stat-label text-[10px]">{s.label}</span>
            <p className="stat-value text-xl">{s.value}</p>
          </div>
        ))}
      </div>

      {/* Search & Filters */}
      <div className="glass-card p-4 mb-6">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher un rendez-vous..." value={search}
              onChange={e => setSearch(e.target.value)} className="input pl-10" />
          </div>
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
            <select value={statutFilter} onChange={e => setStatutFilter(e.target.value as AppointmentStatut | '')} className="input w-auto">
              <option value="">Tous les statuts</option>
              <option value="EN_ATTENTE">En attente</option>
              <option value="CONFIRME">Confirmé</option>
              <option value="REFUSE">Refusé</option>
              <option value="ANNULE">Annulé</option>
              <option value="TERMINE">Terminé</option>
            </select>
          </div>
        )}
      </div>

      {showCreate && (
        <div className="glass-card p-4 space-y-4">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Nouveau rendez-vous</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Avec</label>
              <select
                value={form.recepteurId}
                onChange={e => setForm({ ...form, recepteurId: e.target.value })}
                className="input w-full"
              >
                <option value="">— Choisir —</option>
                {recipients.map(u => (
                  <option key={u.id} value={u.id}>
                    {u.firstName} {u.lastName} ({u.roles.join(' / ')})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Motif</label>
              <select
                value={form.motif}
                onChange={e => setForm({ ...form, motif: e.target.value as AppointmentMotif })}
                className="input w-full"
              >
                {(dictionaries.selectOptions('APPOINTMENT_MOTIF').length > 0
                  ? dictionaries.selectOptions('APPOINTMENT_MOTIF')
                  : Object.entries(MOTIF_FALLBACK).map(([value, label]) => ({ value, label }))
                ).map((o) => (
                  <option key={o.value} value={o.value}>{o.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Date & heure</label>
              <input
                type="datetime-local"
                value={form.datePrevue}
                onChange={e => setForm({ ...form, datePrevue: e.target.value })}
                className="input w-full"
              />
            </div>
            <div>
              <label className="text-xs font-medium text-gray-500 mb-1 block">Durée (min)</label>
              <select
                value={form.dureeMinutes}
                onChange={e => setForm({ ...form, dureeMinutes: Number(e.target.value) })}
                className="input w-full"
              >
                <option value={15}>15 min</option>
                <option value={30}>30 min</option>
                <option value={60}>60 min</option>
              </select>
            </div>
          </div>
          <div>
            <label className="text-xs font-medium text-gray-500 mb-1 block">Objet</label>
            <textarea
              rows={2}
              value={form.objet}
              onChange={e => setForm({ ...form, objet: e.target.value })}
              className="input w-full"
              placeholder="Objet du rendez-vous…"
            />
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => form.recepteurId && form.datePrevue && createMutation.mutate(form)}
              disabled={!form.recepteurId || !form.datePrevue}
              className="btn btn-primary text-sm"
            >
              Demander
            </button>
            <button onClick={() => setShowCreate(false)} className="btn text-sm">Annuler</button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Demandes envoyées */}
        <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
          <div className="px-4 py-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <CalendarPlus className="w-4 h-4 text-primary-500" /> Mes demandes
            </h2>
            <span className="text-xs text-gray-400">{requests.length}</span>
          </div>
          {requestsQuery.isLoading ? (
            <div className="p-6 text-center text-sm text-gray-400">Chargement…</div>
          ) : requests.length === 0 ? (
            <div className="p-10 text-center text-sm text-gray-400">
              Aucune demande de rendez-vous pour le moment.
            </div>
          ) : (
            requests.map(a => <AppointmentRow key={a.id} a={a} side="sent" />)
          )}
        </div>

        {/* Boîte de réception */}
        <div className="glass-card divide-y divide-gray-100 dark:divide-gray-800">
          <div className="px-4 py-3 flex items-center justify-between">
            <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 flex items-center gap-2">
              <Inbox className="w-4 h-4 text-primary-500" /> Demandes reçues
            </h2>
            <span className="text-xs text-gray-400">{inbox.length}</span>
          </div>
          {inboxQuery.isLoading ? (
            <div className="p-6 text-center text-sm text-gray-400">Chargement…</div>
          ) : inbox.length === 0 ? (
            <div className="p-10 text-center text-sm text-gray-400">
              Aucune demande de rendez-vous reçue.
            </div>
          ) : (
            inbox.map(a => <AppointmentRow key={a.id} a={a} side="received" />)
          )}
        </div>
      </div>
    </div>
  );
}
