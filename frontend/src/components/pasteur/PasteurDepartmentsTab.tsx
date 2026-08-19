import { useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import {
  Building2, Plus, Search, Eye, Edit3, Trash2, ArrowLeft,
  Loader2, Users, BarChart3, FileDown, Settings, Hammer,
  ChevronRight, Filter, X, History, FileText, Target,
} from 'lucide-react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell,
} from 'recharts';
import toast from 'react-hot-toast';

const COLORS = ['#22c55e', '#f59e0b', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6', '#ef4444'];

type ViewMode = 'liste' | 'detail' | 'create' | 'edit';

interface DepartmentItem {
  id: string;
  nom: string;
  description?: string;
  responsableId?: string;
  statut?: string;
  totalAmes?: number;
  totalFamilles?: number;
  responsableNom?: string;
  createdAt: string;
  updatedAt: string;
}

export default function PasteurDepartmentsTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [view, setView] = useState<ViewMode>('liste');
  const [selectedDept, setSelectedDept] = useState<DepartmentItem | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);

  const emptyForm = { nom: '', description: '', responsableId: '' };
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);

  // Fetch departments list
  const { data, isLoading } = useQuery({
    queryKey: ['departments', 'pasteur', page, search],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      const res = await api.get(`/departments?${params}`);
      return res.data as PageResponse<DepartmentItem>;
    },
    enabled: view === 'liste',
  });

  // Fetch users for responsable selector
  const { data: users } = useQuery({
    queryKey: ['users', 'responsables'],
    queryFn: async () => {
      const res = await api.get('/users?size=200');
      return res.data.content as { id: string; firstName: string; lastName: string; role: string }[];
    },
    enabled: view === 'create' || view === 'edit',
  });

  // Fetch department detail
  const { data: deptDetail, isLoading: detailLoading } = useQuery({
    queryKey: ['departments', selectedDept?.id, 'detail'],
    queryFn: async () => {
      const res = await api.get(`/departments/${selectedDept!.id}/detail`);
      return res.data as Record<string, any>;
    },
    enabled: !!selectedDept && view === 'detail',
  });

  // Fetch department KPIs
  const { data: deptKpi } = useQuery({
    queryKey: ['departments', selectedDept?.id, 'kpi'],
    queryFn: async () => {
      const res = await api.get(`/departments/${selectedDept!.id}/kpi`);
      return res.data as Record<string, any>;
    },
    enabled: !!selectedDept && view === 'detail',
  });

  // Fetch department teams
  const { data: deptTeams } = useQuery({
    queryKey: ['departments', selectedDept?.id, 'teams'],
    queryFn: async () => {
      const res = await api.get(`/departments/${selectedDept!.id}/teams`);
      return res.data as { id: string; nom: string; description?: string }[];
    },
    enabled: !!selectedDept && view === 'detail',
  });

  // Fetch department tasks
  const { data: deptTasks } = useQuery({
    queryKey: ['departments', selectedDept?.id, 'tasks'],
    queryFn: async () => {
      const res = await api.get(`/departments/${selectedDept!.id}/tasks`);
      return res.data as { id: string; titre: string; statut: string; priorite?: string }[];
    },
    enabled: !!selectedDept && view === 'detail',
  });

  // Mutations
  const deleteMutation = useMutation({
    mutationFn: async (id: string) => { await api.delete(`/departments/${id}`); },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['departments'] }); toast.success('Département supprimé'); setShowDeleteConfirm(null); },
    onError: () => toast.error('Erreur lors de la suppression'),
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof form) => {
      await api.post('/departments', { ...data, responsableId: data.responsableId || undefined });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['departments'] }); toast.success('Département créé'); setView('liste'); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la création'),
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: typeof form }) => {
      await api.put(`/departments/${id}`, { ...data, responsableId: data.responsableId || undefined });
    },
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ['departments'] }); toast.success('Département mis à jour'); setView('liste'); setEditingId(null); setForm(emptyForm); },
    onError: () => toast.error('Erreur lors de la mise à jour'),
  });

  const handleEdit = useCallback((dept: DepartmentItem) => {
    setForm({ nom: dept.nom, description: dept.description || '', responsableId: dept.responsableId || '' });
    setEditingId(dept.id);
    setView('edit');
  }, []);

  const handleSubmit = useCallback(() => {
    if (!form.nom.trim()) { toast.error('Le nom est obligatoire'); return; }
    if (editingId) { updateMutation.mutate({ id: editingId, data: form }); }
    else { createMutation.mutate(form); }
  }, [form, editingId, createMutation, updateMutation]);

  const responsableList = (users || []).filter(u => ['ADMIN', 'PASTEUR', 'RESPONSABLE'].includes(u.role));

  // === VUE DÉTAIL ===
  if (view === 'detail' && selectedDept) {
    return (
      <div className="animate-slide-up">
        <button onClick={() => { setView('liste'); setSelectedDept(null); }} className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700 mb-4">
          <ArrowLeft className="w-4 h-4" /> Retour à la liste
        </button>
        <div className="glass-card p-6">
          <div className="flex items-start justify-between mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">{selectedDept.nom}</h2>
              <p className="text-sm text-gray-500">{selectedDept.description || 'Pas de description'}</p>
              {selectedDept.responsableNom && (
                <p className="text-xs text-gray-400 mt-1">Responsable: {selectedDept.responsableNom}</p>
              )}
            </div>
            <div className="flex gap-2">
              <button onClick={() => handleEdit(selectedDept)} className="btn-secondary btn-sm"><Edit3 className="w-4 h-4" /> Modifier</button>
              <Link to={`/departments/${selectedDept.id}/manage`} className="btn-primary btn-sm"><Settings className="w-4 h-4" /> Gestion avancée</Link>
            </div>
          </div>

          {/* KPIs */}
          {deptKpi && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
              <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
                <Users className="w-4 h-4 mx-auto text-blue-500 mb-1" />
                <p className="text-xs text-gray-400">Membres</p>
                <p className="font-bold text-sm">{deptKpi.totalMembers ?? selectedDept.totalAmes ?? 0}</p>
              </div>
              <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
                <Hammer className="w-4 h-4 mx-auto text-amber-500 mb-1" />
                <p className="text-xs text-gray-400">Équipes</p>
                <p className="font-bold text-sm">{deptKpi.totalTeams ?? deptTeams?.length ?? 0}</p>
              </div>
              <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
                <Target className="w-4 h-4 mx-auto text-emerald-500 mb-1" />
                <p className="text-xs text-gray-400">Tâches</p>
                <p className="font-bold text-sm">{deptKpi.totalTasks ?? deptTasks?.length ?? 0}</p>
              </div>
              <div className="p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50 text-center">
                <BarChart3 className="w-4 h-4 mx-auto text-violet-500 mb-1" />
                <p className="text-xs text-gray-400">Familles</p>
                <p className="font-bold text-sm">{selectedDept.totalFamilles ?? 0}</p>
              </div>
            </div>
          )}

          {/* Équipes */}
          {deptTeams && deptTeams.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Équipes ({deptTeams.length})</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                {deptTeams.map(t => (
                  <div key={t.id} className="flex items-center gap-2 p-2 rounded-lg bg-blue-50/50 dark:bg-blue-900/10 border border-blue-100/30">
                    <Hammer className="w-4 h-4 text-blue-500" />
                    <div>
                      <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{t.nom}</p>
                      {t.description && <p className="text-[9px] text-gray-400 truncate">{t.description}</p>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tâches récentes */}
          {deptTasks && deptTasks.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Tâches ({deptTasks.length})</h3>
              <div className="space-y-1">
                {deptTasks.slice(0, 8).map(t => (
                  <div key={t.id} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
                    <div className="flex items-center gap-2">
                      <span className={`w-2 h-2 rounded-full ${
                        t.statut === 'TERMINEE' ? 'bg-green-500' :
                        t.statut === 'EN_COURS' ? 'bg-amber-500' : 'bg-gray-300'
                      }`} />
                      <span className="text-xs font-medium text-gray-900 dark:text-gray-100">{t.titre}</span>
                    </div>
                    <span className={`badge text-[9px] ${
                      t.statut === 'TERMINEE' ? 'badge-success' :
                      t.statut === 'EN_COURS' ? 'badge-warning' : 'badge-gray'
                    }`}>{t.statut?.replace(/_/g, ' ')}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Membres du département */}
          {deptDetail?.members && deptDetail.members.length > 0 && (
            <div className="mb-6">
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">Membres ({deptDetail.members.length})</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                {deptDetail.members.slice(0, 12).map((m: any) => (
                  <Link key={m.id || m.memberId} to={`/souls/${m.soulId || m.id}`} className="flex items-center gap-2 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors">
                    <div className="w-6 h-6 rounded-full bg-rose-100 dark:bg-rose-900/30 flex items-center justify-center text-[9px] font-bold text-rose-600">
                      {(m.nom || m.prenom || '?').charAt(0)}
                    </div>
                    <span className="text-xs font-medium">{m.prenom} {m.nom}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* Liens rapides */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <Link to={`/departments/${selectedDept.id}/manage`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <Settings className="w-4 h-4 text-blue-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Gestion</span>
                <ChevronRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
            <Link to={`/departments/${selectedDept.id}`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <BarChart3 className="w-4 h-4 text-emerald-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Performance</span>
                <ChevronRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
            <Link to={`/departments/${selectedDept.id}/manage`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <FileDown className="w-4 h-4 text-amber-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Export CSV</span>
                <ChevronRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
            </Link>
            <Link to={`/departments/${selectedDept.id}/manage`} className="glass-card p-3 hover:shadow-lg transition-all group">
              <div className="flex items-center gap-2">
                <History className="w-4 h-4 text-violet-500" />
                <span className="text-xs font-semibold text-gray-900 dark:text-gray-100">Activité</span>
                <ChevronRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
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
          <h2 className="text-lg font-bold text-gray-900 dark:text-gray-100 mb-4">{editingId ? 'Modifier le département' : 'Nouveau département'}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="label">Nom du département *</label>
              <input className="input" value={form.nom} onChange={e => setForm({ ...form, nom: e.target.value })} placeholder="Ex: Louange, Jeunesse, Évangélisation..." />
            </div>
            <div>
              <label className="label">Responsable</label>
              <select className="input" value={form.responsableId} onChange={e => setForm({ ...form, responsableId: e.target.value })}>
                <option value="">Sélectionner...</option>
                {responsableList.map(u => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="label">Description</label>
              <textarea className="input" rows={3} value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} placeholder="Description du département..." />
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

  // === VUE LISTE ===
  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Building2 className="w-5 h-5 text-amber-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Départements</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements} résultats)</span>}
        </div>
        <div className="flex gap-2">
          <Link to="/departments" className="btn-secondary btn-sm"><Eye className="w-4 h-4" /> Page complète</Link>
          <button onClick={() => { setForm(emptyForm); setEditingId(null); setView('create'); }} className="btn-primary btn-sm">
            <Plus className="w-4 h-4" /> Nouveau département
          </button>
        </div>
      </div>

      {/* Search */}
      <div className="glass-card p-4 mb-4">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <input type="text" placeholder="Rechercher un département..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
        </div>
      </div>

      {/* Chart répartition */}
      {data && data.content && data.content.length > 0 && (
        <div className="glass-card p-6 mb-6">
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Répartition des âmes par département</h3>
          <div className="h-48">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.content.filter(d => (d.totalAmes ?? 0) > 0)}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" vertical={false} />
                <XAxis dataKey="nom" tick={{ fontSize: 10 }} stroke="rgba(128,128,128,0.3)" angle={-20} textAnchor="end" height={60} />
                <YAxis tick={{ fontSize: 10 }} stroke="rgba(128,128,128,0.3)" />
                <Tooltip formatter={(v: number) => [v, 'Âmes']} />
                <Bar dataKey="totalAmes" radius={[6, 6, 0, 0]}>
                  {data.content.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Table */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}
        </div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="table w-full">
              <thead>
                <tr>
                  <th>Département</th>
                  <th>Responsable</th>
                  <th className="text-right">Familles</th>
                  <th className="text-right">Âmes</th>
                  <th>Statut</th>
                  <th className="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {(data?.content || []).map(dept => (
                  <tr key={dept.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                    <td>
                      <button onClick={() => { setSelectedDept(dept); setView('detail'); }} className="text-primary-600 hover:text-primary-700 font-medium hover:underline">
                        {dept.nom}
                      </button>
                      {dept.description && <p className="text-[10px] text-gray-400 truncate max-w-[200px]">{dept.description}</p>}
                    </td>
                    <td className="text-sm text-gray-500">{dept.responsableNom || '—'}</td>
                    <td className="text-right text-sm text-gray-700 dark:text-gray-300">{dept.totalFamilles ?? 0}</td>
                    <td className="text-right">
                      <span className="font-semibold text-primary-600 dark:text-primary-400">{dept.totalAmes ?? 0}</span>
                    </td>
                    <td>
                      <span className={`badge text-[10px] ${dept.statut === 'ACTIF' ? 'badge-success' : dept.statut === 'ARCHIVE' ? 'badge-gray' : 'badge-info'}`}>
                        {dept.statut || 'ACTIF'}
                      </span>
                    </td>
                    <td className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => { setSelectedDept(dept); setView('detail'); }} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Voir">
                          <Eye className="w-3.5 h-3.5 text-gray-500" />
                        </button>
                        <button onClick={() => handleEdit(dept)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Modifier">
                          <Edit3 className="w-3.5 h-3.5 text-gray-500" />
                        </button>
                        <Link to={`/departments/${dept.id}/manage`} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700" title="Gestion avancée">
                          <Settings className="w-3.5 h-3.5 text-gray-500" />
                        </Link>
                        <button onClick={() => setShowDeleteConfirm(dept.id)} className="p-1.5 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20" title="Supprimer">
                          <Trash2 className="w-3.5 h-3.5 text-red-400" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {(data?.content || []).length === 0 && (
                  <tr><td colSpan={6} className="py-12 text-center text-gray-400">Aucun département trouvé</td></tr>
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
            <h3 className="text-lg font-semibold mb-2">Supprimer ce département ?</h3>
            <p className="text-sm text-gray-500 mb-4">Les membres ne seront pas supprimés mais seront détachés.</p>
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
