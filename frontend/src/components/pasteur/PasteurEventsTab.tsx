import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import {
  CalendarClock, Plus, Search, Trash2, Loader2, X, Filter,
  Eye, MapPin, Users as UsersIcon, Calendar, ChevronRight, BarChart3,
  Edit3, CheckCircle, Clock, ArrowLeft, CalendarDays, Users,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useI18n } from '@/i18n';

interface EventItem {
  id: string; titre: string; description?: string; typeEvenement?: string;
  dateDebut: string; dateFin?: string; lieu?: string; statut?: string;
  nbInscrits?: number; limitePlaces?: number; organisateurNom?: string;
  compteRendu?: string; familleId?: string; departmentId?: string;
  createdAt: string;
}

export default function PasteurEventsTab() {
  const { locale } = useI18n();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statutFilter, setStatutFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [showDetail, setShowDetail] = useState<EventItem | null>(null);
  const [form, setForm] = useState({ titre: '', description: '', typeEvenement: 'CULTE', dateDebut: '', lieu: '', limitePlaces: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['events', 'pasteur', page, search, typeFilter, statutFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (typeFilter) params.set('typeEvenement', typeFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/events?${params}`);
      return res.data as PageResponse<EventItem>;
    },
  });

  const { data: upcomingEvents } = useQuery({
    queryKey: ['events', 'upcoming'],
    queryFn: async () => { const res = await api.get('/events/consolidated', { params: { days: 14 } }); return res.data as any[]; },
  });

  const { data: stats } = useQuery({
    queryKey: ['events', 'stats'],
    queryFn: async () => { const res = await api.get('/events/statistics'); return res.data as Record<string, any>; },
  });

  const { data: registrations } = useQuery({
    queryKey: ['events', 'registrations', showDetail?.id],
    queryFn: async () => { const res = await api.get(`/events/${showDetail!.id}/registrations`); return res.data as any[]; },
    enabled: !!showDetail?.id,
  });

  const createMutation = useMutation({
    mutationFn: async (d: typeof form) => {
      const payload: any = { ...d, dateDebut: d.dateDebut + 'T09:00:00' };
      if (d.limitePlaces) payload.limitePlaces = parseInt(d.limitePlaces);
      await api.post('/events', payload);
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); toast.success('Événement créé'); setShowCreate(false); setForm({ titre: '', description: '', typeEvenement: 'CULTE', dateDebut: '', lieu: '', limitePlaces: '' }); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/events/${id}`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['events'] }); toast.success('Événement supprimé'); setShowDetail(null); },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const isUpcoming = (d: string) => new Date(d) > new Date();

  const typeLabel = (t?: string) => {
    const m: Record<string, string> = { CULTE: 'Culte', REUNION: 'Réunion', FORMATION: 'Formation', EVANGELISATION: 'Évangélisation', SOCIAL: 'Social', PRIERE: 'Prière', AUTRE: 'Autre' };
    return m[t || ''] || t || '—';
  };
  const typeColors: Record<string, string> = { CULTE: 'badge-info', REUNION: 'badge-warning', FORMATION: 'badge-success', EVANGELISATION: 'badge-error', SOCIAL: 'badge-gray', PRIERE: 'badge-info' };

  // === VUE DÉTAIL ===
  if (showDetail) {
    const ev = showDetail;
    return (
      <div className="animate-slide-up">
        <button onClick={() => setShowDetail(null)} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              <div className="flex items-center gap-2 mb-2">
                {isUpcoming(ev.dateDebut) ? <span className="badge-success text-[10px]">À venir</span> : <span className="badge-gray text-[10px]">Terminé</span>}
                <span className={`badge text-[10px] ${typeColors[ev.typeEvenement || ''] || 'badge-gray'}`}>{typeLabel(ev.typeEvenement)}</span>
              </div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">{ev.titre}</h2>
              {ev.description && <p className="text-sm text-gray-500 mt-1">{ev.description}</p>}
            </div>
            <div className="flex gap-2">
              <Link to="/events/statistics" className="btn-secondary btn-sm"><BarChart3 className="w-4 h-4" /> Stats</Link>
              <button onClick={() => deleteMutation.mutate(ev.id)} className="btn-secondary btn-sm text-red-600"><Trash2 className="w-4 h-4" /></button>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Calendar className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Date</p>
              <p className="font-semibold text-sm">{new Date(ev.dateDebut).toLocaleDateString(locale, { day: 'numeric', month: 'long', year: 'numeric' })}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <MapPin className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Lieu</p>
              <p className="font-semibold text-sm">{ev.lieu || '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <UsersIcon className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Inscriptions</p>
              <p className="font-semibold text-sm">{ev.nbInscrits ?? 0}{ev.limitePlaces ? `/${ev.limitePlaces}` : ''}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Clock className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Heure</p>
              <p className="font-semibold text-sm">{new Date(ev.dateDebut).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })}</p>
            </div>
          </div>

          {ev.compteRendu && (
            <div className="p-4 rounded-xl bg-green-50/50 dark:bg-green-900/10 border border-green-200/30 mb-4">
              <p className="text-xs font-medium text-green-600 mb-1">Compte-rendu</p>
              <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{ev.compteRendu}</p>
            </div>
          )}

          {/* Inscriptions */}
          {registrations && registrations.length > 0 && (
            <div className="mb-4">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3 flex items-center gap-2">
                <Users className="w-4 h-4" /> Inscrits ({registrations.length})
              </h3>
              <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2">
                {registrations.map((r: any) => (
                  <div key={r.id} className="flex items-center gap-2 p-2 rounded-lg bg-gray-50/50 dark:bg-gray-800/30">
                    <div className="w-6 h-6 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-[9px] font-bold text-primary-600">
                      {(r.utilisateurNom || r.nom || '?').charAt(0)}
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-medium truncate">{r.utilisateurNom || r.nom || '—'}</p>
                      <p className="text-[9px] text-gray-400">{r.present ? '✅ Présent' : r.statutInscription || 'Inscrit'}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
          {registrations && registrations.length === 0 && (
            <div className="p-4 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 text-center mb-4">
              <p className="text-xs text-gray-400">Aucune inscription</p>
            </div>
          )}

          <div className="flex items-center gap-3 text-xs text-gray-400 pt-4 border-t border-gray-200 dark:border-gray-700">
            {ev.organisateurNom && <span>Organisé par {ev.organisateurNom}</span>}
            <span>Créé le {new Date(ev.createdAt).toLocaleDateString(locale)}</span>
          </div>
        </div>
      </div>
    );
  }

  // === VUE LISTE ===
  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <CalendarClock className="w-5 h-5 text-orange-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Événements</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements})</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <Link to="/events/statistics" className="btn-secondary btn-sm"><BarChart3 className="w-4 h-4" /> Statistiques</Link>
          <Link to="/events/program" className="btn-secondary btn-sm"><CalendarDays className="w-4 h-4" /> Programme</Link>
          <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Nouvel événement</button>
        </div>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
          <div className="glass-card p-3 text-center"><p className="text-xl font-bold text-gray-900 dark:text-gray-100">{stats.totalEvents ?? 0}</p><p className="text-[10px] text-gray-400">Total</p></div>
          <div className="glass-card p-3 text-center"><p className="text-xl font-bold text-blue-500">{stats.upcomingEvents ?? 0}</p><p className="text-[10px] text-gray-400">À venir</p></div>
          <div className="glass-card p-3 text-center"><p className="text-xl font-bold text-green-500">{stats.completedEvents ?? 0}</p><p className="text-[10px] text-gray-400">Terminés</p></div>
          <div className="glass-card p-3 text-center"><p className="text-xl font-bold text-amber-500">{stats.totalRegistrations ?? 0}</p><p className="text-[10px] text-gray-400">Inscriptions</p></div>
        </div>
      )}

      {/* Upcoming consolidated */}
      {upcomingEvents && upcomingEvents.length > 0 && (
        <div className="glass-card p-4 mb-4 border-l-4 border-l-blue-500">
          <div className="flex items-center gap-2 mb-3">
            <CalendarDays className="w-4 h-4 text-blue-500" />
            <h3 className="text-xs font-semibold text-gray-700 dark:text-gray-300">Prochains 14 jours — Vue consolidée</h3>
          </div>
          <div className="flex gap-2 overflow-x-auto pb-1">
            {upcomingEvents.slice(0, 8).map((ev: any) => (
              <div key={ev.id} className="flex-shrink-0 p-2 rounded-lg bg-blue-50/50 dark:bg-blue-900/10 min-w-[150px]">
                <p className="text-[10px] font-medium text-gray-900 dark:text-gray-100 truncate">{ev.titre}</p>
                <p className="text-[9px] text-gray-400">{new Date(ev.dateDebut).toLocaleDateString(locale, { day: 'numeric', month: 'short' })}</p>
                <div className="flex items-center gap-1 mt-0.5">
                  <span className={`badge text-[8px] ${typeColors[ev.typeEvenement] || 'badge-gray'}`}>{typeLabel(ev.typeEvenement)}</span>
                  {ev.familleNom && <span className="text-[8px] text-gray-400 truncate">{ev.familleNom}</span>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Search + Filters */}
      <div className="glass-card p-4 mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input type="text" placeholder="Rechercher un événement..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
            <select value={typeFilter} onChange={e => { setTypeFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
              <option value="">Tous les types</option>
              <option value="CULTE">Culte</option><option value="REUNION">Réunion</option><option value="FORMATION">Formation</option>
              <option value="EVANGELISATION">Évangélisation</option><option value="SOCIAL">Social</option><option value="PRIERE">Prière</option>
            </select>
            <select value={statutFilter} onChange={e => { setStatutFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
              <option value="">Tous statuts</option>
              <option value="PLANIFIE">Planifié</option><option value="EN_COURS">En cours</option><option value="TERMINE">Terminé</option><option value="ANNULE">Annulé</option>
            </select>
          </div>
        )}
      </div>

      {/* List */}
      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-16 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="space-y-3">
          {(data?.content || []).map(ev => (
            <div key={ev.id} className={`glass-card p-4 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors cursor-pointer ${isUpcoming(ev.dateDebut) ? 'border-l-4 border-l-blue-500' : 'border-l-4 border-l-gray-300'}`}
              onClick={() => setShowDetail(ev)}>
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 ${isUpcoming(ev.dateDebut) ? 'bg-blue-50 dark:bg-blue-900/20' : 'bg-gray-50 dark:bg-gray-800/50'}`}>
                    <CalendarClock className={`w-5 h-5 ${isUpcoming(ev.dateDebut) ? 'text-blue-500' : 'text-gray-400'}`} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 hover:text-primary-600">{ev.titre}</p>
                    {ev.description && <p className="text-xs text-gray-500 line-clamp-1 mt-0.5">{ev.description}</p>}
                    <div className="flex items-center gap-3 mt-1.5 text-[10px] text-gray-400 flex-wrap">
                      <span className="flex items-center gap-1"><Calendar className="w-3 h-3" />{new Date(ev.dateDebut).toLocaleDateString(locale, { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                      {ev.lieu && <span className="flex items-center gap-1"><MapPin className="w-3 h-3" />{ev.lieu}</span>}
                      <span className={`badge text-[9px] ${typeColors[ev.typeEvenement || ''] || 'badge-gray'}`}>{typeLabel(ev.typeEvenement)}</span>
                      {ev.organisateurNom && <span>Par {ev.organisateurNom}</span>}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  {ev.nbInscrits !== undefined && (
                    <span className="text-[10px] text-gray-400 flex items-center gap-1"><UsersIcon className="w-3 h-3" />{ev.nbInscrits}{ev.limitePlaces ? `/${ev.limitePlaces}` : ''}</span>
                  )}
                  {isUpcoming(ev.dateDebut) && <span className="badge-success text-[9px]">À venir</span>}
                  <ChevronRight className="w-4 h-4 text-gray-400" />
                </div>
              </div>
            </div>
          ))}
          {(data?.content || []).length === 0 && <div className="glass-card p-14 text-center"><CalendarClock className="w-10 h-10 text-gray-300 mx-auto mb-2" /><p className="text-sm text-gray-400">Aucun événement</p></div>}
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">Page {data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {/* Create modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowCreate(false)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-lg w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Nouvel événement</h3>
              <button onClick={() => setShowCreate(false)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><X className="w-5 h-5" /></button>
            </div>
            <div className="space-y-4">
              <div><label className="label">Titre *</label><input className="input" value={form.titre} onChange={e => setForm({ ...form, titre: e.target.value })} placeholder="Titre de l'événement" /></div>
              <div><label className="label">Description</label><textarea className="input" rows={3} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Description..." /></div>
              <div className="grid grid-cols-2 gap-3">
                <div><label className="label">Type</label><select className="input" value={form.typeEvenement} onChange={e => setForm({ ...form, typeEvenement: e.target.value })}>
                  <option value="CULTE">Culte</option><option value="REUNION">Réunion</option><option value="FORMATION">Formation</option><option value="EVANGELISATION">Évangélisation</option><option value="SOCIAL">Social</option><option value="PRIERE">Prière</option><option value="AUTRE">Autre</option>
                </select></div>
                <div><label className="label">Date *</label><input className="input" type="date" value={form.dateDebut} onChange={e => setForm({ ...form, dateDebut: e.target.value })} /></div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div><label className="label">Lieu</label><input className="input" value={form.lieu} onChange={e => setForm({ ...form, lieu: e.target.value })} placeholder="Lieu..." /></div>
                <div><label className="label">Places max</label><input className="input" type="number" value={form.limitePlaces} onChange={e => setForm({ ...form, limitePlaces: e.target.value })} placeholder="Illimité" /></div>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
              <button onClick={() => setShowCreate(false)} className="btn-secondary">Annuler</button>
              <button onClick={() => { if (!form.titre.trim() || !form.dateDebut) { toast.error('Remplissez les champs obligatoires'); return; } createMutation.mutate(form); }} disabled={createMutation.isPending} className="btn-primary">
                {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />} Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
