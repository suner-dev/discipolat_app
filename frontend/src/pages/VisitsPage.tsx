import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import toast from 'react-hot-toast';
import { useAuth } from '@/contexts/AuthContext';
import {
  Plus, CalendarCheck, DoorOpen, CheckCircle2, XCircle, Clock, Camera, FileText,
  Search, Filter, RotateCcw, Loader2, MapPin, Calendar, User as UserIcon,
  ChevronRight, Sparkles, Eye,
} from 'lucide-react';
import type { Visit, CreateVisitRequest, UpdateVisitRequest, Soul } from '@/types';

const STATUT_STYLE: Record<string, { label: string; cls: string; dot: string }> = {
  PLANIFIEE: { label: 'Planifiée', cls: 'bg-sky-500/10 text-sky-600 dark:text-sky-400 border-sky-200/50 dark:border-sky-800/30', dot: 'bg-sky-500' },
  REALISEE: { label: 'Réalisée', cls: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border-emerald-200/50 dark:border-emerald-800/30', dot: 'bg-emerald-500' },
  ANNULEE: { label: 'Annulée', cls: 'bg-red-500/10 text-red-600 dark:text-red-400 border-red-200/50 dark:border-red-800/30', dot: 'bg-red-500' },
  REPORTEE: { label: 'Reportée', cls: 'bg-amber-500/10 text-amber-600 dark:text-amber-400 border-amber-200/50 dark:border-amber-800/30', dot: 'bg-amber-500' },
};

const MOTIF_LABELS: Record<string, string> = {
  SUIVI: 'Suivi',
  CONSOLIDATION: 'Consolidation',
  CONSEIL: 'Conseil',
  PRIERE: 'Prière',
  ACCUEIL: 'Accueil',
  AUTRE: 'Autre',
};

export default function VisitsPage() {
  const { user } = useAuth();
  const isLeader = !!user && (user.roles.includes('ADMIN') || user.roles.includes('PASTEUR')
    || user.roles.includes('RESPONSABLE') || user.roles.includes('CHEF_DE_FAMILLE'));
  const queryClient = useQueryClient();

  const [showCreate, setShowCreate] = useState(false);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [form, setForm] = useState<CreateVisitRequest>({
    soulId: '',
    datePrevue: new Date().toISOString().slice(0, 10),
    motif: 'SUIVI',
    objectif: '',
  });
  const [editForm, setEditForm] = useState<{ id: string; statut: Visit['statut']; compteRendu: string; present: boolean } | null>(null);

  const myVisitsQuery = useQuery({
    queryKey: ['visits', 'my', statusFilter],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (statusFilter) params.set('statut', statusFilter);
      const url = `/visits/my${params.toString() ? `?${params}` : ''}`;
      const res = await api.get(url);
      return res.data as Visit[];
    },
  });

  const upcomingQuery = useQuery({
    queryKey: ['visits', 'upcoming'],
    queryFn: async () => {
      const res = await api.get('/visits/upcoming');
      return res.data as Visit[];
    },
    enabled: isLeader,
  });

  const soulsQuery = useQuery({
    queryKey: ['souls', 'light'],
    queryFn: async () => {
      const res = await api.get('/souls', { params: { size: 100 } });
      return (res.data as { content: Soul[] }).content;
    },
    enabled: showCreate,
  });

  const createMutation = useMutation({
    mutationFn: async (payload: CreateVisitRequest) => {
      const res = await api.post('/visits', payload);
      return res.data as Visit;
    },
    onSuccess: () => {
      toast.success('Visite planifiée');
      setShowCreate(false);
      queryClient.invalidateQueries({ queryKey: ['visits'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, payload }: { id: string; payload: UpdateVisitRequest }) => {
      const res = await api.patch(`/visits/${id}`, payload);
      return res.data as Visit;
    },
    onSuccess: () => {
      toast.success('Visite mise à jour');
      setEditForm(null);
      queryClient.invalidateQueries({ queryKey: ['visits'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const myVisits = myVisitsQuery.data ?? [];
  const upcoming = upcomingQuery.data ?? [];

  // Client-side search filter
  const filteredVisits = useMemo(() => {
    if (!search.trim()) return myVisits;
    const q = search.toLowerCase();
    return myVisits.filter(v =>
      v.soulNom?.toLowerCase().includes(q) ||
      v.visiteurNom?.toLowerCase().includes(q) ||
      v.motif?.toLowerCase().includes(q) ||
      v.objectif?.toLowerCase().includes(q)
    );
  }, [myVisits, search]);

  // Stats
  const stats = useMemo(() => ({
    total: myVisits.length,
    planifiees: myVisits.filter(v => v.statut === 'PLANIFIEE').length,
    realisees: myVisits.filter(v => v.statut === 'REALISEE').length,
    annulees: myVisits.filter(v => v.statut === 'ANNULEE').length,
  }), [myVisits]);

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <DoorOpen className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Visites pastorales</h1>
          </div>
          <p className="page-subtitle">Planification, compte rendu et suivi de chaque visite</p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300 dark:border-primary-700' : ''}`}
          >
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => setShowCreate(v => !v)} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Planifier une visite
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        {[
          { label: 'Total', value: stats.total, icon: DoorOpen, color: 'from-primary-500 to-primary-600', filter: '' },
          { label: 'Planifiées', value: stats.planifiees, icon: Clock, color: 'from-sky-500 to-blue-500', filter: 'PLANIFIEE' },
          { label: 'Réalisées', value: stats.realisees, icon: CheckCircle2, color: 'from-emerald-500 to-green-500', filter: 'REALISEE' },
          { label: 'Annulées', value: stats.annulees, icon: XCircle, color: 'from-red-500 to-rose-500', filter: 'ANNULEE' },
        ].map((stat, i) => (
          <button
            key={stat.label}
            type="button"
            onClick={() => setStatusFilter(statusFilter === stat.filter ? '' : stat.filter)}
            className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${statusFilter === stat.filter ? 'ring-2 ring-primary-500/50' : ''}`}
            style={{ animationDelay: `${i * 60}ms` }}
          >
            <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${stat.color} opacity-60`} />
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{stat.label}</span>
              <div className={`p-1.5 rounded-lg bg-gradient-to-br ${stat.color} text-white shadow-sm`}>
                <stat.icon className="w-3.5 h-3.5" />
              </div>
            </div>
            <p className="stat-value text-xl">{stat.value}</p>
          </button>
        ))}
      </div>

      {/* Search & Filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-primary-500 transition-colors" />
            <input
              type="text"
              placeholder="Rechercher par nom, motif, visiteur..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="input pl-10"
            />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20 dark:border-white/[0.06]">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Statut</span>
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="input w-auto text-sm"
              >
                <option value="">Tous</option>
                <option value="PLANIFIEE">Planifiée</option>
                <option value="REALISEE">Réalisée</option>
                <option value="ANNULEE">Annulée</option>
                <option value="REPORTEE">Reportée</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* Create form */}
      {showCreate && (
        <div className="glass-card p-4 mb-6 animate-slide-up">
          <h2 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-3">Nouvelle visite</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
            <div>
              <label className="label">Âme à visiter</label>
              <select
                value={form.soulId}
                onChange={e => setForm({ ...form, soulId: e.target.value })}
                className="input w-full"
              >
                <option value="">— Choisir —</option>
                {(soulsQuery.data ?? []).map(s => (
                  <option key={s.id} value={s.id}>
                    {[s.prenom, s.nom].filter(Boolean).join(' ')}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Date prévue</label>
              <input
                type="date"
                value={form.datePrevue}
                onChange={e => setForm({ ...form, datePrevue: e.target.value })}
                className="input w-full"
              />
            </div>
            <div>
              <label className="label">Motif</label>
              <select
                value={form.motif}
                onChange={e => setForm({ ...form, motif: e.target.value })}
                className="input w-full"
              >
                {Object.entries(MOTIF_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Objectif</label>
              <input
                type="text"
                placeholder="Objectif de la visite…"
                value={form.objectif}
                onChange={e => setForm({ ...form, objectif: e.target.value })}
                className="input w-full"
              />
            </div>
          </div>
          <div className="flex gap-2 mt-3">
            <button
              onClick={() => form.soulId && createMutation.mutate(form)}
              disabled={!form.soulId || createMutation.isPending}
              className="btn-primary btn-sm"
            >
              {createMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
              Créer
            </button>
            <button onClick={() => setShowCreate(false)} className="btn-secondary btn-sm">Annuler</button>
          </div>
        </div>
      )}

      {/* Upcoming (vue leader) */}
      {isLeader && upcoming.length > 0 && (
        <div className="glass-card p-5 mb-6 animate-slide-up">
          <div className="flex items-center gap-2 mb-4">
            <CalendarCheck className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Prochaines visites (toutes équipes)</h3>
            <span className="badge-info text-[10px]">{upcoming.filter(v => v.statut === 'PLANIFIEE').length}</span>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {upcoming.filter(v => v.statut === 'PLANIFIEE').slice(0, 6).map(v => (
              <div key={v.id} className="flex items-center gap-3 p-3 rounded-xl bg-sky-50/50 dark:bg-sky-900/10 border border-sky-200/30 dark:border-sky-800/20">
                <span className="p-2 rounded-lg bg-sky-500/10 text-sky-500"><Clock className="w-4 h-4" /></span>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{v.soulNom}</p>
                  <p className="text-xs text-gray-400">
                    {new Date(v.datePrevue).toLocaleDateString('fr-FR')} · {v.visiteurNom}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* My visits list */}
      <div className="glass-card overflow-hidden animate-slide-up">
        <div className="px-5 py-4 flex items-center justify-between border-b border-gray-100 dark:border-gray-800/50">
          <div className="flex items-center gap-2">
            <DoorOpen className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Mes visites</h3>
            <span className="text-xs text-gray-400">({filteredVisits.length})</span>
          </div>
        </div>

        {myVisitsQuery.isLoading ? (
          <div className="p-8 space-y-3">
            {[...Array(4)].map((_, i) => <div key={i} className="skeleton h-16 w-full rounded-xl" />)}
          </div>
        ) : filteredVisits.length === 0 ? (
          <div className="p-12 text-center">
            <DoorOpen className="w-12 h-12 text-gray-300 dark:text-gray-600 mx-auto mb-4" />
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {search ? 'Aucune visite ne correspond à votre recherche' : 'Aucune visite planifiée'}
            </p>
          </div>
        ) : (
          <div className="divide-y divide-gray-100 dark:divide-gray-800/50">
            {filteredVisits.map((v) => {
              const st = STATUT_STYLE[v.statut] || STATUT_STYLE.PLANIFIEE;
              return (
                <div key={v.id} className="px-5 py-3.5 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 transition-colors">
                  {editForm?.id === v.id ? (
                    // Edit mode
                    <div className="space-y-3">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-medium text-gray-500">Statut :</span>
                        <select
                          className="input text-xs py-1"
                          value={editForm.statut}
                          onChange={e => setEditForm({ ...editForm, statut: e.target.value as Visit['statut'] })}
                        >
                          <option value="PLANIFIEE">Planifiée</option>
                          <option value="REALISEE">Réalisée</option>
                          <option value="ANNULEE">Annulée</option>
                          <option value="REPORTEE">Reportée</option>
                        </select>
                        <label className="flex items-center gap-1 text-xs text-gray-600 dark:text-gray-400">
                          <input type="checkbox" checked={editForm.present} onChange={e => setEditForm({ ...editForm, present: e.target.checked })} className="rounded" />
                          Présent
                        </label>
                      </div>
                      <textarea
                        className="input text-xs"
                        rows={2}
                        placeholder="Compte rendu de la visite..."
                        value={editForm.compteRendu}
                        onChange={e => setEditForm({ ...editForm, compteRendu: e.target.value })}
                      />
                      <div className="flex gap-2">
                        <button
                          onClick={() => updateMutation.mutate({ id: v.id, payload: { statut: editForm.statut, compteRendu: editForm.compteRendu, present: editForm.present } })}
                          disabled={updateMutation.isPending}
                          className="btn-primary btn-xs"
                        >
                          {updateMutation.isPending ? <Loader2 className="w-3 h-3 animate-spin" /> : <CheckCircle2 className="w-3 h-3" />}
                          Sauvegarder
                        </button>
                        <button onClick={() => setEditForm(null)} className="btn-secondary btn-xs">Annuler</button>
                      </div>
                    </div>
                  ) : (
                    // Display mode
                    <div className="flex items-center justify-between gap-4">
                      <div className="flex items-center gap-3 min-w-0">
                        <div className={`w-2 h-2 rounded-full ${st.dot} flex-shrink-0`} />
                        <div className="min-w-0">
                          <div className="flex items-center gap-2">
                            <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">{v.soulNom}</p>
                            <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium border ${st.cls}`}>
                              {st.label}
                            </span>
                          </div>
                          <div className="flex items-center gap-3 mt-0.5 text-xs text-gray-400">
                            <span className="flex items-center gap-1">
                              <Calendar className="w-3 h-3" />
                              {new Date(v.datePrevue).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
                            </span>
                            <span>{(v.motif && MOTIF_LABELS[v.motif]) || v.motif || '—'}</span>
                            {v.visiteurNom && <span>par {v.visiteurNom}</span>}
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center gap-1 flex-shrink-0">
                        {v.compteRendu && (
                          <span className="p-1.5 rounded-lg bg-gray-100 dark:bg-gray-800 text-gray-400" title="Compte rendu disponible">
                            <FileText className="w-3.5 h-3.5" />
                          </span>
                        )}
                        <button
                          onClick={() => setEditForm({ id: v.id, statut: v.statut, compteRendu: v.compteRendu || '', present: v.present ?? false })}
                          className="btn-ghost btn-xs text-primary-600"
                        >
                          Modifier
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
