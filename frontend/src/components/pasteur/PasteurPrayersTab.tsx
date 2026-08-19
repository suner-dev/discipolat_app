import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import {
  BookOpen, Plus, Search, Trash2, Loader2, X, Filter,
  CheckCircle, Clock, Eye, Edit3, ArrowLeft, Heart, CheckCircle2, Star,
} from 'lucide-react';
import toast from 'react-hot-toast';

interface Prayer {
  id: string; titre: string; description?: string; message?: string; auteurId: string; auteurNom?: string;
  statut: string; priorite: string; visibilite: string; categorie?: string;
  nbPrieres: number; reponse?: string; temoignage?: string; createdAt: string;
}

type ViewMode = 'liste' | 'detail' | 'create' | 'edit' | 'grace';

export default function PasteurPrayersTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statutFilter, setStatutFilter] = useState('');
  const [prioriteFilter, setPrioriteFilter] = useState('');
  const [visibiliteFilter, setVisibiliteFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [view, setView] = useState<ViewMode>('liste');
  const [selectedPrayer, setSelectedPrayer] = useState<Prayer | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const emptyForm = { titre: '', message: '', priorite: 'MOYENNE', visibilite: 'EGLISE', categorie: '' };
  const [form, setForm] = useState(emptyForm);
  const [answerForm, setAnswerForm] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['prayers', page, search, statutFilter, prioriteFilter, visibiliteFilter, view],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (statutFilter) params.set('statut', statutFilter);
      if (prioriteFilter) params.set('priorite', prioriteFilter);
      if (visibiliteFilter) params.set('visibilite', visibiliteFilter);
      const res = await api.get(`/prayers?${params}`);
      return res.data as PageResponse<Prayer>;
    },
    enabled: view === 'liste',
  });

  const { data: graceData, isLoading: graceLoading } = useQuery({
    queryKey: ['prayers', 'grace'],
    queryFn: async () => { const res = await api.get('/prayers/actions-de-grace'); return res.data as Prayer[]; },
    enabled: view === 'grace',
  });

  const createMutation = useMutation({
    mutationFn: async (d: typeof form) => { await api.post('/prayers', { ...d, description: d.message }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['prayers'] }); toast.success('Demande de prière créée'); setView('liste'); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof form }) => { await api.put(`/prayers/${id}`, { ...data, description: data.message }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['prayers'] }); toast.success('Prière mise à jour'); setView('liste'); setEditingId(null); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/prayers/${id}`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['prayers'] }); toast.success('Supprimé'); },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const answerMutation = useMutation({
    mutationFn: async ({ id, temoignage }: { id: string; temoignage: string }) => { await api.patch(`/prayers/${id}/answer`, { temoignage }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['prayers'] }); toast.success('Prière marquée comme exaucée 🙏'); setView('liste'); setSelectedPrayer(null); setAnswerForm(''); },
    onError: () => toast.error('Erreur'),
  });

  const prioriteBadge = (p: string) => {
    if (p === 'HAUTE' || p === 'URGENTE') return <span className="badge-error text-[10px]">{p}</span>;
    if (p === 'MOYENNE') return <span className="badge-warning text-[10px]">{p}</span>;
    return <span className="badge-info text-[10px]">{p}</span>;
  };

  const statutIcon = (s: string) => {
    if (s === 'ACTIVE' || s === 'EN_COURS') return <Clock className="w-4 h-4 text-amber-500" />;
    if (s === 'EXAUCEE' || s === 'RESOLUE') return <CheckCircle className="w-4 h-4 text-green-500" />;
    return <BookOpen className="w-4 h-4 text-blue-500" />;
  };

  const handleEdit = (p: Prayer) => {
    setForm({ titre: p.titre, message: p.description || p.message || '', priorite: p.priorite, visibilite: p.visibilite, categorie: p.categorie || '' });
    setEditingId(p.id);
    setView('edit');
  };

  const handleSubmit = () => {
    if (!form.titre.trim()) { toast.error('Le titre est obligatoire'); return; }
    if (editingId) updateMutation.mutate({ id: editingId, data: form });
    else createMutation.mutate(form);
  };

  const prayers = view === 'grace' ? (graceData || []).map(p => ({ ...p, __isGrace: true })) : [];
  const displayData = view === 'grace' ? prayers : (data?.content || []);

  // === VUE DÉTAIL ===
  if (view === 'detail' && selectedPrayer) {
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setSelectedPrayer(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-4">
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">{selectedPrayer.titre}</h2>
              <div className="flex items-center gap-2 mt-2">
                {prioriteBadge(selectedPrayer.priorite)}
                <span className="badge-info text-[10px]">{selectedPrayer.visibilite}</span>
                <span className={`badge text-[10px] ${selectedPrayer.statut === 'EXAUCEE' || selectedPrayer.statut === 'RESOLUE' ? 'badge-success' : 'badge-warning'}`}>{selectedPrayer.statut}</span>
              </div>
            </div>
            <div className="flex gap-2">
              <button onClick={() => handleEdit(selectedPrayer)} className="btn-secondary btn-sm"><Edit3 className="w-4 h-4" /> Modifier</button>
            </div>
          </div>
          <p className="text-sm text-gray-700 dark:text-gray-300 mb-4 whitespace-pre-wrap">{selectedPrayer.description || selectedPrayer.message}</p>
          {selectedPrayer.reponse && (
            <div className="p-3 rounded-xl bg-green-50/50 dark:bg-green-900/10 border border-green-200/30 mb-4">
              <p className="text-xs font-medium text-green-600 mb-1">Réponse</p>
              <p className="text-sm">{selectedPrayer.reponse}</p>
            </div>
          )}
          {selectedPrayer.temoignage && (
            <div className="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/30 mb-4">
              <p className="text-xs font-medium text-amber-600 mb-1">Témoignage</p>
              <p className="text-sm">{selectedPrayer.temoignage}</p>
            </div>
          )}
          <div className="flex items-center gap-3 text-xs text-gray-400 mt-4">
            <span>Par {selectedPrayer.auteurNom || '—'}</span>
            <span>{new Date(selectedPrayer.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
          </div>
          {selectedPrayer.statut !== 'EXAUCEE' && selectedPrayer.statut !== 'RESOLUE' && (
            <div className="mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
              <h4 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">Marquer comme exaucée</h4>
              <textarea className="input mb-2" rows={2} value={answerForm} onChange={e => setAnswerForm(e.target.value)} placeholder="Témoignage (optionnel)..." />
              <button onClick={() => answerMutation.mutate({ id: selectedPrayer.id, temoignage: answerForm })} disabled={answerMutation.isPending}
                className="btn-primary bg-emerald-600 hover:bg-emerald-700">
                {answerMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                🙏 Exaucée
              </button>
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
          <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 mb-4">{editingId ? 'Modifier la prière' : 'Nouvelle demande de prière'}</h2>
          <div className="space-y-4">
            <div><label className="label">Titre *</label><input className="input" value={form.titre} onChange={e => setForm({ ...form, titre: e.target.value })} placeholder="Titre de la demande" /></div>
            <div><label className="label">Message *</label><textarea className="input" rows={4} value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} placeholder="Détail de la prière..." /></div>
            <div className="grid grid-cols-3 gap-3">
              <div><label className="label">Priorité</label><select className="input" value={form.priorite} onChange={e => setForm({ ...form, priorite: e.target.value })}>
                <option value="BASSE">Basse</option><option value="MOYENNE">Moyenne</option><option value="HAUTE">Haute</option><option value="URGENTE">Urgente</option>
              </select></div>
              <div><label className="label">Visibilité</label><select className="input" value={form.visibilite} onChange={e => setForm({ ...form, visibilite: e.target.value })}>
                <option value="EGLISE">Église</option><option value="EQUIPE">Équipe</option><option value="PRIVE">Privé</option><option value="PARTAGEE">Partagée</option>
              </select></div>
              <div><label className="label">Catégorie</label><select className="input" value={form.categorie} onChange={e => setForm({ ...form, categorie: e.target.value })}>
                <option value="">—</option><option value="SANTE">Santé</option><option value="FAMILLE">Famille</option><option value="SPIRITUEL">Spirituel</option><option value="MATÉRIEL">Matériel</option><option value="AUTRE">Autre</option>
              </select></div>
            </div>
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

  // === VUE LISTE / GRACE ===
  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <BookOpen className="w-5 h-5 text-indigo-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
            {view === 'grace' ? 'Actions de Grâce' : 'Prières'}
          </h2>
          {data && view !== 'grace' && <span className="text-xs text-gray-400">({data.totalElements})</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => { setView(view === 'grace' ? 'liste' : 'grace'); setPage(0); }}
            className={`btn-secondary btn-sm ${view === 'grace' ? 'bg-amber-50 dark:bg-amber-900/20 border-amber-300' : ''}`}>
            <Star className="w-4 h-4" /> {view === 'grace' ? 'Prières' : 'Actions de grâce'}
          </button>
          {view !== 'grace' && (
            <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
              <Filter className="w-4 h-4" /> Filtres
            </button>
          )}
          <button onClick={() => { setForm(emptyForm); setEditingId(null); setView('create'); }} className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Nouvelle prière</button>
        </div>
      </div>

      {/* Search + Filters */}
      {view !== 'grace' && (
        <div className="glass-card p-4 mb-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
          {showFilters && (
            <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
              <select value={statutFilter} onChange={e => { setStatutFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Tous statuts</option>
                <option value="ACTIVE">Active</option>
                <option value="EN_COURS">En cours</option>
                <option value="EXAUCEE">Exaucée</option>
              </select>
              <select value={prioriteFilter} onChange={e => { setPrioriteFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Toutes priorités</option>
                <option value="URGENTE">Urgente</option>
                <option value="HAUTE">Haute</option>
                <option value="MOYENNE">Moyenne</option>
                <option value="BASSE">Basse</option>
              </select>
              <select value={visibiliteFilter} onChange={e => { setVisibiliteFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Toutes visibilités</option>
                <option value="EGLISE">Église</option>
                <option value="EQUIPE">Équipe</option>
                <option value="PRIVE">Privé</option>
              </select>
            </div>
          )}
        </div>
      )}

      {/* List */}
      {(view === 'grace' ? graceLoading : isLoading) ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="space-y-3">
          {displayData.map((p: any) => (
            <div key={p.id} className={`glass-card p-4 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors ${
              p.statut === 'EXAUCEE' || p.statut === 'RESOLUE' ? 'border-l-4 border-l-green-500' : ''
            }`}>
              <div className="flex items-start gap-4">
                <div className={`p-2.5 rounded-xl flex-shrink-0 ${
                  p.statut === 'EXAUCEE' || p.statut === 'RESOLUE' ? 'bg-green-50 dark:bg-green-900/20' : 'bg-indigo-50 dark:bg-indigo-900/20'
                }`}>{statutIcon(p.statut)}</div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1 flex-wrap">
                    <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">{p.titre}</span>
                    {prioriteBadge(p.priorite)}
                    {p.categorie && <span className="badge-gray text-[10px]">{p.categorie}</span>}
                    <span className="badge-info text-[10px]">{p.visibilite}</span>
                  </div>
                  <p className="text-xs text-gray-500 line-clamp-2">{p.description || p.message}</p>
                  <div className="flex items-center gap-3 mt-2 text-[10px] text-gray-400">
                    <span>Par {p.auteurNom || '—'}</span>
                    <span>{new Date(p.createdAt).toLocaleDateString('fr-FR')}</span>
                    {p.nbPrieres > 0 && <span>{p.nbPrieres} prières</span>}
                  </div>
                </div>
                <div className="flex items-center gap-1 flex-shrink-0">
                  <button onClick={() => { setSelectedPrayer(p); setView('detail'); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><Eye className="w-3.5 h-3.5 text-gray-500" /></button>
                  {!view.includes('grace') && (
                    <button onClick={() => handleEdit(p)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><Edit3 className="w-3.5 h-3.5 text-gray-500" /></button>
                  )}
                  <button onClick={() => deleteMutation.mutate(p.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20"><Trash2 className="w-3.5 h-3.5 text-red-400" /></button>
                </div>
              </div>
            </div>
          ))}
          {displayData.length === 0 && <div className="glass-card p-14 text-center"><BookOpen className="w-10 h-10 text-gray-300 mx-auto mb-2" /><p className="text-sm text-gray-400">{view === 'grace' ? 'Aucune action de grâce' : 'Aucune prière'}</p></div>}
        </div>
      )}

      {/* Pagination */}
      {view !== 'grace' && data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">Page {data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}
    </div>
  );
}
