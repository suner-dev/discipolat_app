import { useState, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import {
  Calendar, Plus, Search, Trash2, ArrowLeft, Edit3, Eye,
  Loader2, CheckCircle, Clock, X, Filter, AlertTriangle, MapPin, FileText,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Aligné sur VisitResponse (backend) : soulId/soulNom, datePrevue, dateRealisee, statut, motif, objectif, compteRendu, present. */
interface Visit {
  id: string; soulId: string; soulNom?: string; visiteurId: string; visiteurNom?: string;
  datePrevue?: string; dateRealisee?: string; statut: string;
  motif?: string; objectif?: string; compteRendu?: string; photoUrl?: string; present?: boolean;
  createdAt: string;
}

type ViewMode = 'liste' | 'detail' | 'create' | 'edit';

const STATUTS: Record<string, { label: string; cls: string }> = {
  PLANIFIEE: { label: 'Planifiée', cls: 'badge-info' },
  REALISEE: { label: 'Réalisée', cls: 'badge-success' },
  REPORTEE: { label: 'Reportée', cls: 'badge-warning' },
  ANNULEE: { label: 'Annulée', cls: 'badge-gray' },
};

export default function PasteurVisitsTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statutFilter, setStatutFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [view, setView] = useState<ViewMode>('liste');
  const [selectedVisit, setSelectedVisit] = useState<Visit | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);

  const emptyForm = { soulId: '', datePrevue: '', statut: 'PLANIFIEE', motif: '', objectif: '', compteRendu: '' };
  const [form, setForm] = useState(emptyForm);

  const { data, isLoading } = useQuery({
    queryKey: ['visits', page, search, statutFilter, view],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/visits?${params}`);
      return res.data as PageResponse<Visit>;
    },
    enabled: view === 'liste',
  });

  const { data: upcomingVisits } = useQuery({
    queryKey: ['visits', 'upcoming'],
    queryFn: async () => { const res = await api.get('/visits/upcoming'); return res.data as Visit[]; },
    enabled: view === 'liste',
  });

  const { data: souls } = useQuery({
    queryKey: ['souls', 'select'],
    queryFn: async () => { const res = await api.get('/souls?size=200'); return res.data.content as { id: string; nom: string; prenom?: string }[]; },
    enabled: view === 'create' || view === 'edit',
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof emptyForm) => {
      // CreateVisitRequest : soulId, datePrevue, motif, objectif (statut PLANIFIEE par défaut côté backend)
      await api.post('/visits', { soulId: data.soulId, datePrevue: data.datePrevue, motif: data.motif, objectif: data.objectif });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['visits'] }); toast.success('Visite créée'); setView('liste'); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof emptyForm }) => {
      // UpdateVisitRequest : statut (requis), datePrevue, dateRealisee, compteRendu
      const payload: Record<string, unknown> = { statut: data.statut };
      if (data.datePrevue) payload.datePrevue = data.datePrevue;
      if (data.compteRendu) payload.compteRendu = data.compteRendu;
      if (data.statut === 'REALISEE') payload.dateRealisee = new Date().toISOString().slice(0, 10);
      await api.patch(`/visits/${id}`, payload);
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['visits'] }); toast.success('Visite mise à jour'); setView('liste'); setEditingId(null); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/visits/${id}`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['visits'] }); toast.success('Visite supprimée'); setShowDeleteConfirm(null); },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const handleEdit = useCallback((v: Visit) => {
    setForm({
      soulId: v.soulId,
      datePrevue: v.datePrevue?.slice(0, 10) || '',
      statut: v.statut,
      motif: v.motif || '',
      objectif: v.objectif || '',
      compteRendu: v.compteRendu || '',
    });
    setEditingId(v.id);
    setView('edit');
  }, []);

  const handleSubmit = useCallback(() => {
    if (!form.soulId) { toast.error('Sélectionnez une âme'); return; }
    if (!form.datePrevue) { toast.error('Sélectionnez une date prévue'); return; }
    if (editingId) { updateMutation.mutate({ id: editingId, data: form }); }
    else { createMutation.mutate(form); }
  }, [form, editingId, createMutation, updateMutation]);

  const statutBadge = (s: string) => {
    const item = STATUTS[s] || { label: s, cls: 'badge-info' };
    const icons: Record<string, any> = { PLANIFIEE: Clock, REALISEE: CheckCircle, REPORTEE: AlertTriangle, ANNULEE: X };
    const Icon = icons[s] || Clock;
    return <span className={`badge text-[10px] ${item.cls}`}><Icon className="w-3 h-3 inline mr-1" />{item.label}</span>;
  };

  const isUpcoming = (d?: string) => !!d && new Date(d) > new Date();

  // === VUE DÉTAIL ===
  if (view === 'detail' && selectedVisit) {
    const v = selectedVisit;
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setSelectedVisit(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">Visite — {v.soulNom || '—'}</h2>
              <p className="text-sm text-gray-500">Par {v.visiteurNom || '—'}</p>
            </div>
            <div className="flex gap-2">
              {statutBadge(v.statut)}
              <button onClick={() => handleEdit(v)} className="btn-secondary btn-sm"><Edit3 className="w-4 h-4" /> Modifier</button>
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Calendar className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Date prévue</p>
              <p className="font-semibold text-sm">{v.datePrevue ? new Date(v.datePrevue).toLocaleDateString('fr-FR') : '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <CheckCircle className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Réalisée le</p>
              <p className="font-semibold text-sm">{v.dateRealisee ? new Date(v.dateRealisee).toLocaleDateString('fr-FR') : '—'}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <Eye className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Présence</p>
              <p className="font-semibold text-sm">{v.present == null ? '—' : (v.present ? 'Oui' : 'Non')}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <AlertTriangle className="w-4 h-4 mx-auto text-gray-400 mb-1" />
              <p className="text-xs text-gray-400">Motif</p>
              <p className="font-semibold text-sm">{v.motif || '—'}</p>
            </div>
          </div>
          {v.objectif && (
            <div className="p-4 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/30 mb-4">
              <p className="text-xs font-medium text-blue-600 mb-1">Objectif</p>
              <p className="text-sm text-gray-700 dark:text-gray-300">{v.objectif}</p>
            </div>
          )}
          {v.compteRendu && (
            <div className="p-4 rounded-xl bg-green-50/50 dark:bg-green-900/10 border border-green-200/30">
              <p className="text-xs font-medium text-green-600 mb-1">Compte-rendu</p>
              <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{v.compteRendu}</p>
            </div>
          )}
        </div>
      </div>
    );
  }

  // === VUE CREATE / EDIT ===
  if (view === 'create' || view === 'edit') {
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setEditingId(null); setForm(emptyForm); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 mb-4">{editingId ? 'Modifier la visite' : 'Nouvelle visite'}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="label">Âme *</label>
              <select className="input" value={form.soulId} onChange={e => setForm({ ...form, soulId: e.target.value })}>
                <option value="">Sélectionner...</option>
                {(souls || []).map(s => <option key={s.id} value={s.id}>{s.prenom} {s.nom}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Date prévue *</label>
              <input className="input" type="date" value={form.datePrevue} onChange={e => setForm({ ...form, datePrevue: e.target.value })} />
            </div>
            {editingId && (
              <div>
                <label className="label">Statut</label>
                <select className="input" value={form.statut} onChange={e => setForm({ ...form, statut: e.target.value })}>
                  {Object.entries(STATUTS).map(([value, { label }]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </div>
            )}
            <div>
              <label className="label">Motif</label>
              <input className="input" value={form.motif} onChange={e => setForm({ ...form, motif: e.target.value })} placeholder="Motif de la visite..." />
            </div>
            <div className="md:col-span-2">
              <label className="label">Objectif</label>
              <textarea className="input" rows={2} value={form.objectif} onChange={e => setForm({ ...form, objectif: e.target.value })} placeholder="Objectif de la visite..." />
            </div>
            {editingId && (
              <div className="md:col-span-2">
                <label className="label">Compte-rendu</label>
                <textarea className="input" rows={4} value={form.compteRendu} onChange={e => setForm({ ...form, compteRendu: e.target.value })} placeholder="Compte-rendu de la visite réalisée..." />
              </div>
            )}
          </div>
          <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button onClick={() => { setView('liste'); setEditingId(null); setForm(emptyForm); }} className="btn-secondary">Annuler</button>
            <button onClick={handleSubmit} disabled={createMutation.isPending || updateMutation.isPending} className="btn-primary">
              {(createMutation.isPending || updateMutation.isPending) && <Loader2 className="w-4 h-4 animate-spin" />}
              {editingId ? 'Enregistrer' : 'Créer'}
            </button>
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
          <Calendar className="w-5 h-5 text-teal-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Visites Pastorales</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements})</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => { setForm(emptyForm); setEditingId(null); setView('create'); }} className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Nouvelle visite</button>
        </div>
      </div>

      {/* Upcoming visits quick view */}
      {upcomingVisits && upcomingVisits.length > 0 && (
        <div className="glass-card p-4 mb-4 border-l-4 border-l-blue-500">
          <div className="flex items-center gap-2 mb-2">
            <Clock className="w-4 h-4 text-blue-500" />
            <h3 className="text-xs font-semibold text-gray-700 dark:text-gray-300">Prochaines visites ({upcomingVisits.length})</h3>
          </div>
          <div className="flex gap-2 overflow-x-auto pb-1">
            {upcomingVisits.slice(0, 5).map(v => (
              <div key={v.id} className="flex-shrink-0 p-2 rounded-lg bg-blue-50/50 dark:bg-blue-900/10 min-w-[140px]">
                <p className="text-[10px] font-medium text-gray-900 dark:text-gray-100">{v.soulNom || '—'}</p>
                <p className="text-[9px] text-gray-400">{v.datePrevue ? new Date(v.datePrevue).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }) : '—'}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Search + Filters */}
      <div className="glass-card p-4 mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input type="text" placeholder="Rechercher..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
        </div>
        {showFilters && (
          <div className="flex gap-3 mt-4 pt-4 border-t border-white/20">
            <select value={statutFilter} onChange={e => { setStatutFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
              <option value="">Tous statuts</option>
              {Object.entries(STATUTS).map(([value, { label }]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </div>
        )}
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Âme</th>
                  <th>Visiteur</th>
                  <th>Date prévue</th>
                  <th>Réalisée</th>
                  <th>Statut</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {(data?.content || []).map(v => (
                  <tr key={v.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                    <td>
                      <button onClick={() => { setSelectedVisit(v); setView('detail'); }} className="text-primary-600 hover:text-primary-700 font-medium hover:underline">
                        {v.soulNom || v.soulId.slice(0, 8)}
                      </button>
                    </td>
                    <td className="text-sm text-gray-500">{v.visiteurNom || '—'}</td>
                    <td className="text-sm text-gray-500">{v.datePrevue ? new Date(v.datePrevue).toLocaleDateString('fr-FR') : '—'}</td>
                    <td className="text-sm text-gray-500">{v.dateRealisee ? new Date(v.dateRealisee).toLocaleDateString('fr-FR') : '—'}</td>
                    <td>{statutBadge(v.statut)}</td>
                    <td className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => { setSelectedVisit(v); setView('detail'); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Voir"><Eye className="w-3.5 h-3.5 text-gray-500" /></button>
                        <button onClick={() => handleEdit(v)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Modifier"><Edit3 className="w-3.5 h-3.5 text-gray-500" /></button>
                        <button onClick={() => setShowDeleteConfirm(v.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20" title="Supprimer"><Trash2 className="w-3.5 h-3.5 text-red-400" /></button>
                      </div>
                    </td>
                  </tr>
                ))}
                {(data?.content || []).length === 0 && <tr><td colSpan={6} className="py-12 text-center text-gray-400">Aucune visite</td></tr>}
              </tbody>
            </table>
          </div>
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

      {/* Delete modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-sm w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-2">Supprimer cette visite ?</h3>
            <div className="flex justify-end gap-3 mt-4">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary">Annuler</button>
              <button onClick={() => deleteMutation.mutate(showDeleteConfirm)} className="btn-primary bg-red-600 hover:bg-red-700"><Trash2 className="w-4 h-4" /> Supprimer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
