import { useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import {
  Star, Search, Filter, Eye, Plus, Loader2, X,
  BarChart3, TrendingUp, Users, CheckCircle,
} from 'lucide-react';
import toast from 'react-hot-toast';

interface Evaluation {
  id: string; evalueId: string; evalueNom?: string; evaluateurId: string; evaluateurNom?: string;
  categorie: string; note: number; commentaire?: string; anonyme: boolean; createdAt: string;
}

export default function PasteurEvaluationsTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [showDetail, setShowDetail] = useState<Evaluation | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ evalueId: '', categorie: 'MEMBRE', note: 3, commentaire: '', anonyme: true });

  const { data, isLoading } = useQuery({
    queryKey: ['evaluations', page, search, catFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (catFilter) params.set('categorie', catFilter);
      const res = await api.get(`/evaluations?${params}`);
      return res.data as PageResponse<Evaluation>;
    },
  });

  const { data: users } = useQuery({
    queryKey: ['users', 'select'],
    queryFn: async () => {
      const res = await api.get('/users?size=200');
      return res.data.content as { id: string; firstName: string; lastName: string }[];
    },
    enabled: showCreate,
  });

  const createMutation = useMutation({
    mutationFn: async (d: typeof form) => { await api.post('/evaluations', { ...d, evalueId: d.evalueId || undefined }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['evaluations'] }); toast.success('Évaluation créée'); setShowCreate(false); setForm({ evalueId: '', categorie: 'MEMBRE', note: 3, commentaire: '', anonyme: true }); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const catLabel = (c: string) => {
    const m: Record<string, string> = { RESPONSABLE: 'Responsable', CHEF_FAMILLE: 'Chef de famille', FAISEUR: 'Faiseur', MEMBRE: 'Membre' };
    return m[c] || c;
  };

  const stars = (n: number, interactive = false, onRate?: (n: number) => void) => Array.from({ length: 5 }, (_, i) => (
    <Star key={i}
      className={`w-4 h-4 ${i < n ? 'text-amber-400 fill-amber-400' : 'text-gray-300 dark:text-gray-600'} ${interactive ? 'cursor-pointer hover:scale-110 transition-transform' : ''}`}
      onClick={interactive ? () => onRate?.(i + 1) : undefined}
    />
  ));

  // Compute stats from current page data
  const allEvals = data?.content || [];
  const avgNote = allEvals.length > 0 ? (allEvals.reduce((s, e) => s + e.note, 0) / allEvals.length).toFixed(1) : '0';
  const byCategory = allEvals.reduce((acc, e) => { acc[e.categorie] = (acc[e.categorie] || 0) + 1; return acc; }, {} as Record<string, number>);

  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Star className="w-5 h-5 text-yellow-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Évaluations</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements})</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Nouvelle évaluation</button>
          <Link to="/evaluations" className="btn-secondary btn-sm"><Eye className="w-4 h-4" /> Page complète</Link>
        </div>
      </div>

      {/* Stats résumé */}
      {allEvals.length > 0 && (
        <div className="grid grid-cols-3 gap-4 mb-4">
          <div className="glass-card p-4 text-center">
            <div className="flex items-center justify-center gap-0.5 mb-2">{stars(Math.round(Number(avgNote)))}</div>
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{avgNote}</p>
            <p className="text-[10px] text-gray-400">Note moyenne</p>
          </div>
          <div className="glass-card p-4 text-center">
            <BarChart3 className="w-5 h-5 text-blue-500 mx-auto mb-2" />
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{data?.totalElements || 0}</p>
            <p className="text-[10px] text-gray-400">Total évaluations</p>
          </div>
          <div className="glass-card p-4 text-center">
            <Users className="w-5 h-5 text-emerald-500 mx-auto mb-2" />
            <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{Object.keys(byCategory).length}</p>
            <p className="text-[10px] text-gray-400">Catégories</p>
          </div>
        </div>
      )}

      {/* Search + Filters */}
      <div className="glass-card p-4 mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher par commentaire, évalué..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Catégorie</span>
              <select value={catFilter} onChange={e => { setCatFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Toutes</option>
                <option value="RESPONSABLE">Responsable</option>
                <option value="CHEF_FAMILLE">Chef de famille</option>
                <option value="FAISEUR">Faiseur</option>
                <option value="MEMBRE">Membre</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* List */}
      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="space-y-3">
          {allEvals.map(ev => (
            <div key={ev.id} className="glass-card p-4 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors cursor-pointer" onClick={() => setShowDetail(ev)}>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-0.5">{stars(ev.note)}</div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="badge-info text-[10px]">{catLabel(ev.categorie)}</span>
                      {ev.evalueNom && <span className="text-xs font-medium text-gray-900 dark:text-gray-100">{ev.evalueNom}</span>}
                    </div>
                    <span className="text-[10px] text-gray-400">{ev.anonyme ? 'Anonyme' : (ev.evaluateurNom || '—')}</span>
                  </div>
                </div>
                <span className="text-xs text-gray-400">{new Date(ev.createdAt).toLocaleDateString('fr-FR')}</span>
              </div>
              {ev.commentaire && <p className="text-xs text-gray-500 mt-2 line-clamp-2">{ev.commentaire}</p>}
            </div>
          ))}
          {allEvals.length === 0 && <div className="glass-card p-14 text-center"><Star className="w-10 h-10 text-gray-300 mx-auto mb-2" /><p className="text-sm text-gray-400">Aucune évaluation</p></div>}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">Page {data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}

      {/* Detail modal */}
      {showDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowDetail(null)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-lg w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Évaluation</h3>
              <button onClick={() => setShowDetail(null)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><X className="w-5 h-5" /></button>
            </div>
            <div className="flex items-center gap-2 mb-3">
              <div className="flex gap-0.5">{stars(showDetail.note)}</div>
              <span className="text-lg font-bold">{showDetail.note}/5</span>
            </div>
            <div className="flex gap-2 mb-3">
              <span className="badge-info text-[10px]">{catLabel(showDetail.categorie)}</span>
              {showDetail.evalueNom && <span className="badge-info text-[10px]">Évalué: {showDetail.evalueNom}</span>}
              {showDetail.anonyme && <span className="badge-gray text-[10px]">Anonyme</span>}
            </div>
            {showDetail.commentaire && <p className="text-sm text-gray-700 dark:text-gray-300 mt-3 whitespace-pre-wrap bg-gray-50 dark:bg-gray-800/50 p-3 rounded-xl">{showDetail.commentaire}</p>}
            <div className="flex items-center gap-3 text-xs text-gray-400 mt-4">
              <span>{showDetail.anonyme ? 'Anonyme' : (showDetail.evaluateurNom || '—')}</span>
              <span>{new Date(showDetail.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}</span>
            </div>
          </div>
        </div>
      )}

      {/* Create modal */}
      {showCreate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowCreate(false)}>
          <div className="bg-white dark:bg-gray-800 rounded-xl p-6 max-w-lg w-full animate-slide-up" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Nouvelle évaluation</h3>
              <button onClick={() => setShowCreate(false)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><X className="w-5 h-5" /></button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="label">Évalué</label>
                <select className="input" value={form.evalueId} onChange={e => setForm({ ...form, evalueId: e.target.value })}>
                  <option value="">Sélectionner...</option>
                  {(users || []).map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}
                </select>
              </div>
              <div>
                <label className="label">Catégorie *</label>
                <select className="input" value={form.categorie} onChange={e => setForm({ ...form, categorie: e.target.value })}>
                  <option value="MEMBRE">Membre</option>
                  <option value="FAISEUR">Faiseur</option>
                  <option value="CHEF_FAMILLE">Chef de famille</option>
                  <option value="RESPONSABLE">Responsable</option>
                </select>
              </div>
              <div>
                <label className="label">Note *</label>
                <div className="flex items-center gap-2 mt-1">
                  <div className="flex gap-1">{stars(form.note, true, (n) => setForm({ ...form, note: n }))}</div>
                  <span className="text-sm font-bold text-gray-600">{form.note}/5</span>
                </div>
              </div>
              <div>
                <label className="label">Commentaire</label>
                <textarea className="input" rows={3} value={form.commentaire} onChange={e => setForm({ ...form, commentaire: e.target.value })} placeholder="Commentaire optionnel..." />
              </div>
              <div className="flex items-center gap-2">
                <input type="checkbox" id="anonyme" checked={form.anonyme} onChange={e => setForm({ ...form, anonyme: e.target.checked })} className="rounded" />
                <label htmlFor="anonyme" className="text-sm text-gray-600 dark:text-gray-400">Évaluation anonyme</label>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
              <button onClick={() => setShowCreate(false)} className="btn-secondary">Annuler</button>
              <button onClick={() => { if (!form.evalueId) { toast.error('Sélectionnez une personne'); return; } createMutation.mutate(form); }} disabled={createMutation.isPending} className="btn-primary">
                {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />} Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
