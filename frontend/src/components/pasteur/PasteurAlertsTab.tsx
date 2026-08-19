import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Alert, PageResponse } from '@/types';
import {
  Bell, BellRing, Plus, Eye, Search, Filter, CheckCircle2,
  AlertTriangle, Clock, Loader2, X, Church, Users, Building2,
  User as UserIcon, Send, CheckSquare, Square, History, ArrowLeft,
} from 'lucide-react';
import toast from 'react-hot-toast';

interface AlertDetail {
  id: string; typeAlerte: string; message: string; statut: string;
  priorite: string; cible: string; dateDeclenchement: string;
  entiteType?: string; entiteId?: string; traitePar?: string;
  dateTraitement?: string; createdAt: string;
}

export default function PasteurAlertsTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [showDetail, setShowDetail] = useState<Alert | AlertDetail | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [selectAll, setSelectAll] = useState(false);
  const [form, setForm] = useState({ titre: '', message: '', typeAlerteManuel: '', priorite: 'MOYENNE', cible: 'EGLISE' });

  const { data: alertStats } = useQuery({
    queryKey: ['alerts', 'stats'],
    queryFn: async () => { const res = await api.get('/alerts/stats'); return res.data as { actives: number; traitees: number; resolues: number; total: number }; },
  });

  const { data, isLoading } = useQuery({
    queryKey: ['alerts', 'pasteur', page, filter, priorityFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '15', page: String(page) });
      if (filter) params.set('statut', filter);
      const res = await api.get(`/alerts?${params}`);
      let result = res.data as PageResponse<Alert>;
      if (priorityFilter && result.content) {
        result = { ...result, content: result.content.filter((a: any) => a.priorite === priorityFilter) };
      }
      return result;
    },
  });

  const resolveMutation = useMutation({
    mutationFn: async (id: string) => { await api.patch(`/alerts/${id}/resolve`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['alerts'] }); toast.success('Alerte résolue'); },
    onError: () => toast.error('Erreur'),
  });

  const acknowledgeMutation = useMutation({
    mutationFn: async (id: string) => { await api.post(`/alerts/${id}/acknowledge`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['alerts'] }); toast.success('Alerte accusée de réception'); },
    onError: () => toast.error('Erreur'),
  });

  const batchResolveMutation = useMutation({
    mutationFn: async (ids: string[]) => { const res = await api.post('/alerts/resolve-batch', ids); return res.data; },
    onSuccess: (data: any) => { queryClient.invalidateQueries({ queryKey: ['alerts'] }); toast.success(`${data.resolved} alerte(s) résolue(s)`); setSelectedIds(new Set()); setSelectAll(false); },
    onError: () => toast.error('Erreur lors de la résolution en lot'),
  });

  const createMutation = useMutation({
    mutationFn: async (d: typeof form) => { await api.post('/alerts', { ...d, typeAlerteManuel: d.typeAlerteManuel || d.titre }); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['alerts'] }); toast.success('Alerte créée'); setShowCreate(false); setForm({ titre: '', message: '', typeAlerteManuel: '', priorite: 'MOYENNE', cible: 'EGLISE' }); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const toggleSelect = (id: string) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectAll) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set((data?.content || []).filter((a: any) => a.statut === 'ACTIVE').map((a: any) => a.id)));
    }
    setSelectAll(!selectAll);
  };

  const priorityBadge = (p: string) => {
    const m: Record<string, string> = {
      URGENTE: 'badge-error', HAUTE: 'badge-warning', MOYENNE: 'badge-info', BASSE: 'badge-gray',
    };
    return <span className={`badge text-[10px] ${m[p] || 'badge-info'}`}>{p}</span>;
  };

  const statutBadge = (s: string) => {
    if (s === 'ACTIVE') return <span className="badge-error text-[10px]"><AlertTriangle className="w-3 h-3 inline mr-1" />Active</span>;
    if (s === 'TRAITEE') return <span className="badge-warning text-[10px]"><Clock className="w-3 h-3 inline mr-1" />Traitée</span>;
    return <span className="badge-success text-[10px]"><CheckCircle2 className="w-3 h-3 inline mr-1" />Résolue</span>;
  };

  const cibleIcon = (c: string) => {
    const m: Record<string, any> = { EGLISE: Church, PERSONNE: UserIcon, FAMILLE: Users, DEPARTEMENT: Building2 };
    const Icon = m[c] || Bell;
    return <Icon className="w-3 h-3" />;
  };

  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <BellRing className="w-5 h-5 text-red-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Alertes</h2>
          {alertStats && <span className={`badge text-[10px] ${alertStats.actives > 0 ? 'badge-error' : 'badge-success'}`}>{alertStats.actives} actives</span>}
        </div>
        <div className="flex gap-2">
          {selectedIds.size > 0 && (
            <button onClick={() => batchResolveMutation.mutate(Array.from(selectedIds))} disabled={batchResolveMutation.isPending}
              className="btn-primary btn-sm bg-emerald-600 hover:bg-emerald-700">
              {batchResolveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckSquare className="w-4 h-4" />}
              Résoudre ({selectedIds.size})
            </button>
          )}
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <Link to="/alerts" className="btn-secondary btn-sm"><Eye className="w-4 h-4" /> Page complète</Link>
          <button onClick={() => setShowCreate(true)} className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Nouvelle alerte</button>
        </div>
      </div>

      {/* Stats */}
      {alertStats && (
        <div className="grid grid-cols-3 gap-4 mb-4">
          <div className="glass-card p-4 text-center cursor-pointer hover:shadow-md transition-shadow" onClick={() => { setFilter(filter === 'ACTIVE' ? '' : 'ACTIVE'); setPage(0); }}>
            <p className={`text-2xl font-bold ${filter === 'ACTIVE' ? 'text-red-600' : 'text-red-500'}`}>{alertStats.actives}</p>
            <p className="text-[10px] text-gray-400">Actives</p>
          </div>
          <div className="glass-card p-4 text-center cursor-pointer hover:shadow-md transition-shadow" onClick={() => { setFilter(filter === 'TRAITEE' ? '' : 'TRAITEE'); setPage(0); }}>
            <p className={`text-2xl font-bold ${filter === 'TRAITEE' ? 'text-amber-600' : 'text-amber-500'}`}>{alertStats.traitees}</p>
            <p className="text-[10px] text-gray-400">Traitées</p>
          </div>
          <div className="glass-card p-4 text-center cursor-pointer hover:shadow-md transition-shadow" onClick={() => { setFilter(filter === 'RESOLUE' ? '' : 'RESOLUE'); setPage(0); }}>
            <p className={`text-2xl font-bold ${filter === 'RESOLUE' ? 'text-green-600' : 'text-green-500'}`}>{alertStats.resolues}</p>
            <p className="text-[10px] text-gray-400">Résolues</p>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="glass-card p-4 mb-4">
        <div className="flex gap-3">
          <select value={filter} onChange={e => { setFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
            <option value="">Toutes</option>
            <option value="ACTIVE">Actives</option>
            <option value="TRAITEE">Traitées</option>
            <option value="RESOLUE">Résolues</option>
          </select>
          {showFilters && (
            <>
              <select value={priorityFilter} onChange={e => { setPriorityFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Toutes priorités</option>
                <option value="URGENTE">Urgente</option>
                <option value="HAUTE">Haute</option>
                <option value="MOYENNE">Moyenne</option>
                <option value="BASSE">Basse</option>
              </select>
            </>
          )}
        </div>
      </div>

      {/* Bulk select bar */}
      {filter === 'ACTIVE' && (data?.content || []).length > 0 && (
        <div className="flex items-center gap-3 mb-3 px-2">
          <button onClick={toggleSelectAll} className="flex items-center gap-1 text-xs text-gray-500 hover:text-gray-700">
            {selectAll ? <CheckSquare className="w-4 h-4 text-primary-500" /> : <Square className="w-4 h-4" />}
            {selectAll ? 'Tout désélectionner' : 'Tout sélectionner'}
          </button>
          {selectedIds.size > 0 && <span className="text-xs text-primary-600 font-medium">{selectedIds.size} sélectionnée(s)</span>}
        </div>
      )}

      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="space-y-2">
          {(data?.content || []).map(a => (
            <div key={a.id} className={`glass-card p-4 transition-colors ${
              a.statut === 'ACTIVE' ? 'border-l-4 border-l-red-500' : 'border-l-4 border-l-green-500 opacity-70'
            } ${selectedIds.has(a.id) ? 'ring-2 ring-primary-400' : ''}`}>
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-3">
                  {a.statut === 'ACTIVE' && (
                    <button onClick={() => toggleSelect(a.id)} className="mt-0.5">
                      {selectedIds.has(a.id) ? <CheckSquare className="w-4 h-4 text-primary-500" /> : <Square className="w-4 h-4 text-gray-300" />}
                    </button>
                  )}
                  <div className={`p-2 rounded-xl ${a.statut === 'ACTIVE' ? 'bg-red-100 dark:bg-red-900/30' : 'bg-green-100 dark:bg-green-900/30'}`}>
                    {a.statut === 'ACTIVE' ? <AlertTriangle className="w-4 h-4 text-red-600" /> : <CheckCircle2 className="w-4 h-4 text-green-600" />}
                  </div>
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <span className="badge-info text-[10px]">{a.typeAlerte}</span>
                      {priorityBadge(a.priorite)}
                      <span className="badge-info text-[10px] flex items-center gap-1">{cibleIcon(a.cible)}{a.cible}</span>
                    </div>
                    <p className="text-sm text-gray-900 dark:text-gray-100">{a.message}</p>
                    <div className="flex items-center gap-3 mt-1 text-[10px] text-gray-400">
                      <span>{new Date(a.dateDeclenchement).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                      {(a as any).traitePar && <span>Traité par {(a as any).traitePar}</span>}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  {statutBadge(a.statut)}
                  {a.statut === 'ACTIVE' && (
                    <>
                      <button onClick={() => acknowledgeMutation.mutate(a.id)} disabled={acknowledgeMutation.isPending}
                        className="p-1.5 rounded-lg hover:bg-blue-50 dark:hover:bg-blue-900/20" title="Accusé de réception">
                        <Clock className="w-3.5 h-3.5 text-blue-500" />
                      </button>
                      <button onClick={() => resolveMutation.mutate(a.id)} disabled={resolveMutation.isPending}
                        className="btn-glow btn-sm flex-shrink-0" title="Résoudre">
                        {resolveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                        Résoudre
                      </button>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
          {(data?.content || []).length === 0 && <div className="glass-card p-14 text-center"><Bell className="w-10 h-10 text-green-500 mx-auto mb-2" /><p className="text-sm text-gray-400">Aucune alerte</p></div>}
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
              <h3 className="text-lg font-semibold">Nouvelle alerte</h3>
              <button onClick={() => setShowCreate(false)} className="p-1 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"><X className="w-5 h-5" /></button>
            </div>
            <div className="space-y-4">
              <div><label className="label">Titre *</label><input className="input" value={form.titre} onChange={e => setForm({ ...form, titre: e.target.value })} placeholder="Titre de l'alerte" /></div>
              <div><label className="label">Message *</label><textarea className="input" rows={3} value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} placeholder="Détail..." /></div>
              <div className="grid grid-cols-2 gap-3">
                <div><label className="label">Priorité</label><select className="input" value={form.priorite} onChange={e => setForm({ ...form, priorite: e.target.value })}>
                  <option value="BASSE">Basse</option><option value="MOYENNE">Moyenne</option><option value="HAUTE">Haute</option><option value="URGENTE">Urgente</option>
                </select></div>
                <div><label className="label">Cible</label><select className="input" value={form.cible} onChange={e => setForm({ ...form, cible: e.target.value })}>
                  <option value="EGLISE">Église</option><option value="PERSONNE">Personne</option><option value="FAMILLE">Famille</option><option value="DEPARTEMENT">Département</option>
                </select></div>
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-6 pt-4 border-t border-gray-200 dark:border-gray-700">
              <button onClick={() => setShowCreate(false)} className="btn-secondary">Annuler</button>
              <button onClick={() => { if (!form.titre.trim() || !form.message.trim()) { toast.error('Remplissez les champs'); return; } createMutation.mutate(form); }} disabled={createMutation.isPending} className="btn-primary">
                {createMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />} <Send className="w-4 h-4" /> Créer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
