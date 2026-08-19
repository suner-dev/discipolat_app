import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import {
  Plus, Pencil, Trash2, Building2, Loader2, Save, Globe, Calendar,
  Search, Filter, Users, Database, Activity, Eye, ChevronRight,
  Shield, Zap, X, RefreshCw, BarChart3, Mail,
} from 'lucide-react';
import type { Tenant, TenantStatus } from '@/types';

const STATUS_OPTIONS: { value: TenantStatus; label: string; color: string; dot: string }[] = [
  { value: 'ACTIVE', label: 'Active', color: 'badge-success', dot: 'bg-green-500' },
  { value: 'SUSPENDED', label: 'Suspendue', color: 'badge-warning', dot: 'bg-amber-500' },
  { value: 'CANCELLED', label: 'Annulée', color: 'badge-error', dot: 'bg-red-500' },
  { value: 'PENDING_SETUP', label: 'En attente', color: 'badge-info', dot: 'bg-blue-500' },
];

const PLAN_OPTIONS = [
  { value: 'free', label: 'Free', color: 'text-gray-500', badge: 'badge-gray' },
  { value: 'starter', label: 'Starter', color: 'text-blue-500', badge: 'badge-info' },
  { value: 'pro', label: 'Pro', color: 'text-primary-500', badge: 'badge-primary' },
  { value: 'enterprise', label: 'Enterprise', color: 'text-purple-500', badge: 'badge-purple' },
];

interface TenantForm {
  name: string;
  slug: string;
  plan: string;
  status?: TenantStatus;
}

const EMPTY_FORM: TenantForm = { name: '', slug: '', plan: 'free' };

export default function AdminTenantsPage() {
  const queryClient = useQueryClient();
  const [modalOpen, setModalOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState<TenantForm>(EMPTY_FORM);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<TenantStatus | ''>('');
  const [planFilter, setPlanFilter] = useState('');
  const [detailTenant, setDetailTenant] = useState<Tenant | null>(null);

  const { data: tenants = [], isLoading, refetch } = useQuery({
    queryKey: ['admin', 'tenants'],
    queryFn: async () => {
      const res = await api.get('/tenants');
      return res.data as Tenant[];
    },
  });

  const { data: tenantStats } = useQuery({
    queryKey: ['admin', 'tenants', 'stats'],
    queryFn: async () => {
      const res = await api.get('/admin/system-health');
      return res.data as any;
    },
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'tenants'] });
    queryClient.invalidateQueries({ queryKey: ['admin', 'tenants', 'stats'] });
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (editId) {
        await api.put(`/tenants/${editId}`, { name: form.name, status: form.status, plan: form.plan });
      } else {
        await api.post('/tenants', { name: form.name, slug: form.slug, plan: form.plan });
      }
    },
    onSuccess: () => {
      invalidate();
      setModalOpen(false);
      setEditId(null);
      toast.success(editId ? 'Église mise à jour' : 'Église créée');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: async (id: string) => api.delete(`/tenants/${id}`),
    onSuccess: () => {
      invalidate();
      setDetailTenant(null);
      toast.success('Église supprimée');
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const openCreate = () => { setEditId(null); setForm(EMPTY_FORM); setModalOpen(true); };
  const openEdit = (t: Tenant) => {
    setEditId(t.id);
    setForm({ name: t.name, slug: t.slug, plan: t.plan, status: t.status });
    setModalOpen(true);
  };

  const autoSlug = (name: string) =>
    name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');

  const filtered = tenants.filter((t) => {
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      if (!t.name.toLowerCase().includes(q) && !t.slug.toLowerCase().includes(q)) return false;
    }
    if (statusFilter && t.status !== statusFilter) return false;
    if (planFilter && t.plan !== planFilter) return false;
    return true;
  });

  const statsByStatus = STATUS_OPTIONS.map((s) => ({
    ...s,
    count: tenants.filter((t) => t.status === s.value).length,
  }));

  const statsByPlan = PLAN_OPTIONS.map((p) => ({
    ...p,
    count: tenants.filter((t) => t.plan === p.value).length,
  }));

  const getPlanInfo = (plan: string) => PLAN_OPTIONS.find((p) => p.value === plan) || PLAN_OPTIONS[0];
  const getStatusInfo = (status: TenantStatus) => STATUS_OPTIONS.find((s) => s.value === status) || STATUS_OPTIONS[0];

  if (isLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <div className="spinner h-8 w-8" />
      </div>
    );
  }

  return (
    <div className="page-container max-w-6xl">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <Building2 className="w-5 h-5 text-primary-500" />
            Églises (Tenants)
          </h1>
          <p className="page-subtitle">
            Gérez les églises de la plateforme — chaque église possède ses propres
            données et utilisateurs isolés.
          </p>
        </div>
        <div className="page-header-actions">
          <button onClick={() => refetch()} className="btn-ghost btn-sm">
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
          <button className="btn-primary btn-sm" onClick={openCreate}>
            <Plus className="w-4 h-4" /> Nouvelle église
          </button>
        </div>
      </div>

      {/* Stats Overview */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {statsByStatus.map((s, i) => (
          <button
            key={s.value}
            type="button"
            onClick={() => setStatusFilter(statusFilter === s.value ? '' : s.value)}
            className={`stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 ${statusFilter === s.value ? 'ring-2 ring-primary-500/50' : ''}`}
            style={{ animationDelay: `${i * 60}ms` }}
          >
            <div className="flex items-start justify-between mb-2">
              <span className="stat-label text-[10px]">{statusFilter === s.value ? `${s.label} (filtré)` : s.label}</span>
              <span className={`w-3 h-3 rounded-full ${s.dot} shadow-[0_0_6px_rgba(0,0,0,0.15)]`} />
            </div>
            <p className="stat-value text-2xl">{s.count}</p>
            <p className="text-[10px] text-gray-400 mt-0.5">
              {s.count === 1 ? 'église' : 'églises'}
            </p>
          </button>
        ))}
      </div>

      {/* Plan distribution */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
        <div className="flex items-center gap-2 mb-3">
          <BarChart3 className="w-4 h-4 text-primary-500" />
          <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Répartition par plan</h3>
        </div>
        <div className="flex items-center gap-3">
          {statsByPlan.map((p) => {
            const pct = tenants.length > 0 ? Math.round((p.count / tenants.length) * 100) : 0;
            return (
              <button
                key={p.value}
                type="button"
                onClick={() => setPlanFilter(planFilter === p.value ? '' : p.value)}
                className={`flex-1 p-3 rounded-xl border text-center transition-all ${
                  planFilter === p.value
                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20 ring-2 ring-primary-500/20'
                    : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'
                }`}
              >
                <p className={`text-lg font-bold ${p.color}`}>{p.count}</p>
                <p className="text-[10px] text-gray-400">{p.label} ({pct}%)</p>
              </button>
            );
          })}
        </div>
      </div>

      {/* Search bar */}
      <div className="glass-card p-4 mb-6 animate-slide-up" style={{ animationDelay: '180ms' }}>
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Rechercher une église par nom ou slug..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input pl-9"
            />
          </div>
          {(searchTerm || statusFilter || planFilter) && (
            <button
              onClick={() => { setSearchTerm(''); setStatusFilter(''); setPlanFilter(''); }}
              className="btn-ghost btn-sm"
            >
              <X className="w-3.5 h-3.5" /> Réinitialiser
            </button>
          )}
        </div>
        {(statusFilter || planFilter) && (
          <div className="flex items-center gap-2 mt-2 pt-2 border-t border-gray-200/60 dark:border-gray-700/60">
            <Filter className="w-3.5 h-3.5 text-gray-400" />
            <span className="text-xs text-gray-500">Filtres actifs :</span>
            {statusFilter && (
              <span className="badge text-[10px] badge-primary">
                {getStatusInfo(statusFilter).label}
                <button onClick={() => setStatusFilter('')} className="ml-1 hover:text-red-500">×</button>
              </span>
            )}
            {planFilter && (
              <span className="badge text-[10px] badge-info">
                {getPlanInfo(planFilter).label}
                <button onClick={() => setPlanFilter('')} className="ml-1 hover:text-red-500">×</button>
              </span>
            )}
          </div>
        )}
      </div>

      {/* Tenant list */}
      {filtered.length === 0 ? (
        <div className="glass-card p-10 text-center animate-scale-in">
          <Building2 className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">
            {tenants.length === 0 ? 'Aucune église configurée.' : 'Aucune église ne correspond aux filtres.'}
          </p>
          {tenants.length === 0 ? (
            <button className="text-primary-500 hover:underline text-sm mt-2" onClick={openCreate}>
              Créer la première église
            </button>
          ) : (
            <button className="text-primary-500 hover:underline text-sm mt-2" onClick={() => { setSearchTerm(''); setStatusFilter(''); setPlanFilter(''); }}>
              Réinitialiser les filtres
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((t, i) => {
            const statusInfo = getStatusInfo(t.status);
            const planInfo = getPlanInfo(t.plan);
            return (
              <div
                key={t.id}
                className="glass-card px-5 py-4 flex items-center gap-4 animate-slide-up hover:shadow-md transition-shadow"
                style={{ animationDelay: `${i * 40}ms` }}
              >
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white text-base font-bold flex-shrink-0 shadow-sm">
                  {t.name.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{t.name}</span>
                    <span className={`badge text-[10px] ${statusInfo.color}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${statusInfo.dot}`} />
                      {statusInfo.label}
                    </span>
                    <span className={`badge text-[10px] ${planInfo.badge}`}>{planInfo.label}</span>
                  </div>
                  <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                    <span className="flex items-center gap-1">
                      <Globe className="w-3 h-3" /> {t.slug}
                    </span>
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3" />
                      Créée le {new Date(t.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    className="btn-icon text-gray-400 hover:text-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20"
                    onClick={() => setDetailTenant(t)}
                    title="Voir le détail"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                  <button
                    className="btn-icon text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-100/70"
                    onClick={() => openEdit(t)}
                    title="Modifier"
                  >
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button
                    className="btn-icon text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20"
                    onClick={() => { if (confirm(`Supprimer l'église « ${t.name} » ? Cette action est irréversible.`)) deleteMutation.mutate(t.id); }}
                    title="Supprimer"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create / Edit Modal */}
      {modalOpen && (
        <div className="modal-overlay" onClick={() => setModalOpen(false)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-sm">
                  <Building2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">
                    {editId ? 'Modifier l\'église' : 'Nouvelle église'}
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {editId ? 'Mettez à jour les informations' : 'Configurez une nouvelle église sur la plateforme'}
                  </p>
                </div>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setModalOpen(false)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4 max-h-[60vh] overflow-y-auto">
              <div>
                <label className="label">Nom de l'église</label>
                <input
                  className="input"
                  value={form.name}
                  onChange={(e) => {
                    const name = e.target.value;
                    setForm({ ...form, name, slug: editId ? form.slug : autoSlug(name) });
                  }}
                  placeholder="Ex. : Église de la Grâce"
                />
              </div>
              <div>
                <label className="label">Slug (identifiant URL)</label>
                <input
                  className="input font-mono"
                  value={form.slug}
                  onChange={(e) => setForm({ ...form, slug: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '') })}
                  disabled={!!editId}
                  placeholder="eglise-de-la-grace"
                />
                <p className="text-[10px] text-gray-400 mt-1">
                  {editId ? 'Le slug ne peut pas être modifié.' : 'Minuscules, chiffres et tirets uniquement.'}
                </p>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="label">Plan</label>
                  <select className="input" value={form.plan} onChange={(e) => setForm({ ...form, plan: e.target.value })}>
                    {PLAN_OPTIONS.map((p) => (
                      <option key={p.value} value={p.value}>{p.label}</option>
                    ))}
                  </select>
                </div>
                {editId && (
                  <div>
                    <label className="label">Statut</label>
                    <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as TenantStatus })}>
                      {STATUS_OPTIONS.map((s) => (
                        <option key={s.value} value={s.value}>{s.label}</option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setModalOpen(false)}>Annuler</button>
              <button
                className="btn-primary btn-sm"
                onClick={() => saveMutation.mutate()}
                disabled={saveMutation.isPending || !form.name.trim() || (!editId && !form.slug.trim())}
              >
                {saveMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                {editId ? 'Enregistrer' : 'Créer'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {detailTenant && (
        <div className="modal-overlay" onClick={() => setDetailTenant(null)}>
          <div className="modal-content max-w-xl" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-500 to-emerald-600 flex items-center justify-center text-white text-lg font-bold shadow-sm">
                  {detailTenant.name.charAt(0).toUpperCase()}
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">{detailTenant.name}</h3>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className={`badge text-[10px] ${getStatusInfo(detailTenant.status).color}`}>
                      {getStatusInfo(detailTenant.status).label}
                    </span>
                    <span className={`badge text-[10px] ${getPlanInfo(detailTenant.plan).badge}`}>
                      {getPlanInfo(detailTenant.plan).label}
                    </span>
                  </div>
                </div>
              </div>
              <button className="btn-icon text-gray-400 hover:text-gray-600" onClick={() => setDetailTenant(null)}>
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <div className="flex items-center gap-2 mb-1">
                    <Globe className="w-3.5 h-3.5 text-gray-400" />
                    <span className="text-[10px] text-gray-400 uppercase font-semibold">Slug</span>
                  </div>
                  <p className="text-sm font-mono text-gray-900 dark:text-gray-100">{detailTenant.slug}</p>
                </div>
                <div className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-100 dark:border-gray-700/40">
                  <div className="flex items-center gap-2 mb-1">
                    <Calendar className="w-3.5 h-3.5 text-gray-400" />
                    <span className="text-[10px] text-gray-400 uppercase font-semibold">Créée le</span>
                  </div>
                  <p className="text-sm text-gray-900 dark:text-gray-100">
                    {new Date(detailTenant.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div className="p-3 rounded-xl bg-blue-50/50 dark:bg-blue-900/10 border border-blue-200/40 dark:border-blue-700/30 text-center">
                  <Users className="w-4 h-4 text-blue-500 mx-auto mb-1" />
                  <p className="text-[10px] text-blue-500 font-semibold uppercase">ID</p>
                  <p className="text-xs font-mono text-gray-900 dark:text-gray-100 truncate">{detailTenant.id.slice(0, 8)}…</p>
                </div>
                <div className="p-3 rounded-xl bg-emerald-50/50 dark:bg-emerald-900/10 border border-emerald-200/40 dark:border-emerald-700/30 text-center">
                  <Shield className="w-4 h-4 text-emerald-500 mx-auto mb-1" />
                  <p className="text-[10px] text-emerald-500 font-semibold uppercase">Plan</p>
                  <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{getPlanInfo(detailTenant.plan).label}</p>
                </div>
                <div className="p-3 rounded-xl bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/40 dark:border-amber-700/30 text-center">
                  <Activity className="w-4 h-4 text-amber-500 mx-auto mb-1" />
                  <p className="text-[10px] text-amber-500 font-semibold uppercase">Statut</p>
                  <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{getStatusInfo(detailTenant.status).label}</p>
                </div>
              </div>

              {detailTenant.status === 'SUSPENDED' && (
                <div className="p-3 rounded-xl bg-amber-50/70 dark:bg-amber-900/10 border border-amber-200/50 dark:border-amber-800/30">
                  <p className="text-xs text-amber-700 dark:text-amber-400 flex items-center gap-2">
                    <Shield className="w-4 h-4" />
                    Cette église est suspendue — les utilisateurs ne peuvent pas se connecter.
                  </p>
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setDetailTenant(null)}>Fermer</button>
              <button
                className="btn-secondary btn-sm"
                onClick={() => { setDetailTenant(null); openEdit(detailTenant); }}
              >
                <Pencil className="w-3.5 h-3.5" /> Modifier
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
