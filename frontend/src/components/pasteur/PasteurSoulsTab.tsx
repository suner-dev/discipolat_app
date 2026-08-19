import { useState, useMemo, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Soul, PageResponse, TypeDisciple, StatutAme } from '@/types';
import {
  Heart, Plus, Search, Filter, Trash2, RotateCcw, Edit3,
  Eye, Loader2, X, ChevronDown, Star, ArrowLeft, Archive,
  History, UserPlus, UserX, Clock, CheckCircle, XCircle,
  FileDown, Download, Calendar, ArrowRight,
} from 'lucide-react';
import toast from 'react-hot-toast';

const STATUT_FALLBACK: Record<string, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti', NOUVEL_ARRIVANT: 'Nouvel arrivant',
  EN_INTEGRATION: 'En intégration', ACTIF: 'Actif', EN_VEILLE: 'En veille', DECROCHE: 'Décroché',
};
const TYPE_FALLBACK: Record<string, string> = {
  NOUVEL_ARRIVANT: 'Nouvel arrivant', NOUVEAU_CONVERTI: 'Nouveau converti',
};

type ViewMode = 'liste' | 'corbeille' | 'detail' | 'create' | 'edit';

export default function PasteurSoulsTab() {
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeDisciple | ''>('');
  const [statutFilter, setStatutFilter] = useState<StatutAme | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [view, setView] = useState<ViewMode>('liste');
  const [selectedSoul, setSelectedSoul] = useState<Soul | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);

  // Create/Edit form
  const emptyForm = { nom: '', prenom: '', email: '', telephone: '', typeDisciple: 'NOUVEL_ARRIVANT' as TypeDisciple, faiseurId: '', familleId: '', statut: 'EN_INTEGRATION' as StatutAme, notesPasteur: '' };
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);

  const typeLabel = (code: string) => dictionaries.label('SOUL_TYPE', code) || TYPE_FALLBACK[code] || code;
  const statusLabel = (code: string) => dictionaries.label('SOUL_STATUS', code) || STATUT_FALLBACK[code] || code;

  const { data, isLoading } = useQuery({
    queryKey: ['souls', 'pasteur', page, search, typeFilter, statutFilter, view],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (typeFilter) params.set('typeDisciple', typeFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const url = view === 'corbeille' ? '/souls/trash' : '/souls';
      const res = await api.get(`${url}?${params}`);
      return res.data as PageResponse<Soul>;
    },
    enabled: view === 'liste' || view === 'corbeille',
  });

  const { data: users } = useQuery({
    queryKey: ['users', 'faiseurs'],
    queryFn: async () => {
      const res = await api.get('/users?size=200');
      return res.data.content as { id: string; firstName: string; lastName: string; role: string }[];
    },
    enabled: view === 'create' || view === 'edit',
  });

  const { data: families } = useQuery({
    queryKey: ['families', 'list'],
    queryFn: async () => {
      const res = await api.get('/families?size=200');
      return res.data.content as { id: string; nom: string }[];
    },
    enabled: view === 'create' || view === 'edit',
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/souls/${id}`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['souls'] }); toast.success('Âme supprimée'); setShowDeleteConfirm(null); },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const restoreMutation = useMutation({
    mutationFn: async (id: string) => { await api.patch(`/souls/${id}/restore`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['souls'] }); toast.success('Âme restaurée ✨'); },
    onError: () => toast.error('Erreur lors de la restauration'),
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof form) => {
      await api.post('/souls', { ...data, faiseurId: data.faiseurId || undefined, familleId: data.familleId || undefined });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['souls'] }); toast.success('Âme créée avec succès'); setView('liste'); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof form }) => {
      await api.put(`/souls/${id}`, data);
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['souls'] }); toast.success('Âme mise à jour'); setView('liste'); setEditingId(null); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const handleEdit = useCallback((soul: Soul) => {
    setForm({ nom: soul.nom, prenom: soul.prenom || '', email: soul.email || '', telephone: soul.telephone || '', typeDisciple: soul.typeDisciple, faiseurId: soul.faiseurId, familleId: soul.familleId || '', statut: soul.statut, notesPasteur: soul.notesPasteur || '' });
    setEditingId(soul.id);
    setView('edit');
  }, []);

  const handleSubmit = useCallback(() => {
    if (!form.nom.trim()) { toast.error('Le nom est obligatoire'); return; }
    if (editingId) { updateMutation.mutate({ id: editingId, data: form }); }
    else { createMutation.mutate(form); }
  }, [form, editingId, createMutation, updateMutation]);

  const faiseurList = useMemo(() => (users || []).filter(u => u.role === 'FAISEUR' || u.role === 'PASTEUR'), [users]);

  // === VUE DÉTAIL ===
  if (view === 'detail' && selectedSoul) {
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setSelectedSoul(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">{selectedSoul.prenom} {selectedSoul.nom}</h2>
              <p className="text-sm text-gray-500">{selectedSoul.email || '—'} · {selectedSoul.telephone || '—'}</p>
            </div>
            <div className="flex gap-2">
              <button onClick={() => handleEdit(selectedSoul)} className="btn-secondary btn-sm"><Edit3 className="w-4 h-4" /> Modifier</button>
              <Link to={`/souls/${selectedSoul.id}`} className="btn-secondary btn-sm"><Eye className="w-4 h-4" /> Vue complète</Link>
              <Link to={`/souls/${selectedSoul.id}/pastoral-360`} className="btn-primary btn-sm"><Heart className="w-4 h-4" /> Pastoral 360</Link>
            </div>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Type</p>
              <p className="font-semibold text-sm">{typeLabel(selectedSoul.typeDisciple)}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Statut</p>
              <p className="font-semibold text-sm">{statusLabel(selectedSoul.statut)}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Intégration</p>
              <p className="font-semibold text-sm">{new Date(selectedSoul.dateIntegration).toLocaleDateString('fr-FR')}</p>
            </div>
            <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
              <p className="text-xs text-gray-400">Dernier contact</p>
              <p className="font-semibold text-sm">{selectedSoul.dateDernierContact ? new Date(selectedSoul.dateDernierContact).toLocaleDateString('fr-FR') : '—'}</p>
            </div>
          </div>
          {selectedSoul.notesPasteur && (
            <div className="p-4 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/30">
              <p className="text-xs font-medium text-amber-600 mb-1">Notes du pasteur</p>
              <p className="text-sm text-gray-700 dark:text-gray-300">{selectedSoul.notesPasteur}</p>
            </div>
          )}

          {/* Liens rapides */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-6">
            <Link to={`/souls/${selectedSoul.id}`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <Eye className="w-4 h-4 text-blue-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Fiche complète</span>
                <ArrowRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
            <Link to={`/souls/${selectedSoul.id}/pastoral-360`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <Heart className="w-4 h-4 text-rose-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Pastoral 360</span>
                <ArrowRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
            <Link to={`/visits?ameId=${selectedSoul.id}`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-teal-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Visites</span>
                <ArrowRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
            <Link to={`/transfers?ameId=${selectedSoul.id}`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <History className="w-4 h-4 text-violet-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Historique</span>
                <ArrowRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
          </div>
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
          <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 mb-4">{editingId ? 'Modifier l\'âme' : 'Nouvelle âme'}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="label">Nom *</label>
              <input className="input" value={form.nom} onChange={e => setForm({ ...form, nom: e.target.value })} placeholder="Nom de famille" />
            </div>
            <div>
              <label className="label">Prénom</label>
              <input className="input" value={form.prenom} onChange={e => setForm({ ...form, prenom: e.target.value })} placeholder="Prénom" />
            </div>
            <div>
              <label className="label">Email</label>
              <input className="input" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} placeholder="email@exemple.com" />
            </div>
            <div>
              <label className="label">Téléphone</label>
              <input className="input" value={form.telephone} onChange={e => setForm({ ...form, telephone: e.target.value })} placeholder="+243..." />
            </div>
            <div>
              <label className="label">Type de disciple *</label>
              <select className="input" value={form.typeDisciple} onChange={e => setForm({ ...form, typeDisciple: e.target.value as TypeDisciple })}>
                <option value="NOUVEL_ARRIVANT">Nouvel arrivant</option>
                <option value="NOUVEAU_CONVERTI">Nouveau converti</option>
              </select>
            </div>
            <div>
              <label className="label">Statut</label>
              <select className="input" value={form.statut} onChange={e => setForm({ ...form, statut: e.target.value as StatutAme })}>
                {Object.keys(STATUT_FALLBACK).map(s => <option key={s} value={s}>{statusLabel(s)}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Faiseur</label>
              <select className="input" value={form.faiseurId} onChange={e => setForm({ ...form, faiseurId: e.target.value })}>
                <option value="">Sélectionner...</option>
                {faiseurList.map(f => <option key={f.id} value={f.id}>{f.firstName} {f.lastName}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Famille</label>
              <select className="input" value={form.familleId} onChange={e => setForm({ ...form, familleId: e.target.value })}>
                <option value="">Sélectionner...</option>
                {(families || []).map(f => <option key={f.id} value={f.id}>{f.nom}</option>)}
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="label">Notes pasteur</label>
              <textarea className="input" rows={3} value={form.notesPasteur} onChange={e => setForm({ ...form, notesPasteur: e.target.value })} placeholder="Notes privées sur cette âme..." />
            </div>
          </div>
          <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
            <button onClick={() => { setView('liste'); setEditingId(null); setForm(emptyForm); }} className="btn-secondary">Annuler</button>
            <button onClick={handleSubmit} disabled={createMutation.isPending || updateMutation.isPending} className="btn-primary">
              {(createMutation.isPending || updateMutation.isPending) && <Loader2 className="w-4 h-4 animate-spin" />}
              {editingId ? 'Enregistrer' : 'Créer l\'âme'}
            </button>
          </div>
        </div>
      </div>
    );
  }

  // === VUE LISTE / CORBEILLE ===
  return (
    <div className="animate-slide-up">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Heart className="w-5 h-5 text-rose-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Âmes</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements} résultats)</span>}
        </div>
        <div className="flex gap-2">
          {view !== 'corbeille' && (
            <a href={`/api/v1/souls/export?search=${search}${typeFilter ? '&typeDisciple=' + typeFilter : ''}${statutFilter ? '&statut=' + statutFilter : ''}`} target="_blank" rel="noreferrer" className="btn-secondary btn-sm">
              <Download className="w-4 h-4" /> Export CSV
            </a>
          )}
          <button
            onClick={() => setView(view === 'corbeille' ? 'liste' : 'corbeille')}
            className={`btn-secondary btn-sm ${view === 'corbeille' ? 'bg-red-50 dark:bg-red-900/20 border-red-300' : ''}`}
          >
            <Trash2 className="w-4 h-4" /> {view === 'corbeille' ? 'Voir les âmes' : 'Corbeille'}
          </button>
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          {view !== 'corbeille' && (
            <button onClick={() => { setForm(emptyForm); setEditingId(null); setView('create'); }} className="btn-primary btn-sm">
              <Plus className="w-4 h-4" /> Nouvelle âme
            </button>
          )}
        </div>
      </div>

      {/* Search */}
      <div className="glass-card p-4 mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text" placeholder="Rechercher par nom, email..."
              value={search} onChange={e => { setSearch(e.target.value); setPage(0); }}
              className="input pl-10"
            />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Type</span>
              <select value={typeFilter} onChange={e => { setTypeFilter(e.target.value as TypeDisciple | ''); setPage(0); }} className="input w-auto text-sm">
                <option value="">Tous</option>
                <option value="NOUVEL_ARRIVANT">Nouvel arrivant</option>
                <option value="NOUVEAU_CONVERTI">Nouveau converti</option>
              </select>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Statut</span>
              <select value={statutFilter} onChange={e => { setStatutFilter(e.target.value as StatutAme | ''); setPage(0); }} className="input w-auto text-sm">
                <option value="">Tous</option>
                {Object.entries(STATUT_FALLBACK).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
              </select>
            </div>
          </div>
        )}
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4 animate-fade-in"><div className="skeleton h-12 w-full rounded-xl" /></div>)}
        </div>
      ) : view === 'corbeille' ? (
        <div className="glass-card overflow-hidden">
          {(data?.content || []).length === 0 ? (
            <div className="p-14 text-center">
              <RotateCcw className="w-10 h-10 text-green-500 mx-auto mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Corbeille vide</h3>
              <p className="text-sm text-gray-500">Aucune âme supprimée.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table w-full">
                <thead><tr><th>Nom</th><th>Type</th><th>Email</th><th className="text-right">Action</th></tr></thead>
                <tbody>
                  {(data?.content || []).map(soul => (
                    <tr key={soul.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20">
                      <td className="font-medium text-gray-900 dark:text-gray-100">{soul.prenom ? `${soul.prenom} ${soul.nom}` : soul.nom}</td>
                      <td className="text-sm text-gray-500">{typeLabel(soul.typeDisciple)}</td>
                      <td className="text-sm text-gray-500">{soul.email || '—'}</td>
                      <td className="text-right">
                        <button onClick={() => restoreMutation.mutate(soul.id)} className="btn-secondary btn-xs">
                          <RotateCcw className="w-3.5 h-3.5" /> Restaurer
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Nom</th>
                  <th>Type</th>
                  <th>Statut</th>
                  <th>Email</th>
                  <th>Téléphone</th>
                  <th>Intégration</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {(data?.content || []).map(soul => (
                  <tr key={soul.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                    <td>
                      <button onClick={() => { setSelectedSoul(soul); setView('detail'); }} className="text-primary-600 hover:text-primary-700 font-medium hover:underline">
                        {soul.prenom ? `${soul.prenom} ${soul.nom}` : soul.nom}
                      </button>
                    </td>
                    <td><span className={soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'badge-success' : 'badge-info'}>{typeLabel(soul.typeDisciple)}</span></td>
                    <td><span className="badge-info">{statusLabel(soul.statut)}</span></td>
                    <td className="text-sm text-gray-500">{soul.email || '—'}</td>
                    <td className="text-sm text-gray-500">{soul.telephone || '—'}</td>
                    <td className="text-sm text-gray-500">{new Date(soul.dateIntegration).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}</td>
                    <td className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => { setSelectedSoul(soul); setView('detail'); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Voir"><Eye className="w-3.5 h-3.5 text-gray-500" /></button>
                        <button onClick={() => handleEdit(soul)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Modifier"><Edit3 className="w-3.5 h-3.5 text-gray-500" /></button>
                        <button onClick={() => setShowDeleteConfirm(soul.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20" title="Supprimer"><Trash2 className="w-3.5 h-3.5 text-red-400" /></button>
                        <Link to={`/souls/${soul.id}/pastoral-360`} className="p-1.5 rounded-lg hover:bg-purple-50 dark:hover:bg-purple-900/20" title="Pastoral 360"><Heart className="w-3.5 h-3.5 text-purple-400" /></Link>
                      </div>
                    </td>
                  </tr>
                ))}
                {(data?.content || []).length === 0 && (
                  <tr><td colSpan={7} className="py-12 text-center text-gray-400">Aucune âme trouvée</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">
            {data.number * data.size + 1} à {Math.min((data.number + 1) * data.size, data.totalElements)} sur {data.totalElements}
          </p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {/* Delete confirmation modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowDeleteConfirm(null)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-sm w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-2">Supprimer cette âme ?</h3>
            <p className="text-sm text-gray-500 mb-4">Cette action est réversible via la corbeille.</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setShowDeleteConfirm(null)} className="btn-secondary">Annuler</button>
              <button onClick={() => deleteMutation.mutate(showDeleteConfirm)} className="btn-primary bg-red-600 hover:bg-red-700">
                <Trash2 className="w-4 h-4" /> Supprimer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
