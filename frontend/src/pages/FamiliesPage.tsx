import { useState, useMemo, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api, { getErrorMessage } from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import type { Family, PageResponse } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Users, Plus, Building2, ChevronRight, BarChart3, AlertTriangle,
  Search, Filter, Trash2, RotateCcw, Heart, CheckCircle, XCircle,
  Loader2, Calendar, Crown, Eye,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { useI18n } from '@/i18n';

type RiskFilter = 'NORMAL' | 'SOUS_SURVEILLANCE' | 'A_RISQUE' | '';
type StatutFilter = 'ACTIVE' | 'INACTIVE' | '';

const toRiskFilter = (v: string): RiskFilter =>
  v === 'NORMAL' || v === 'SOUS_SURVEILLANCE' || v === 'A_RISQUE' ? v : '';
const toStatutFilter = (v: string): StatutFilter =>
  v === 'ACTIVE' || v === 'INACTIVE' ? v : '';

export default function FamiliesPage() {
  const { user } = useAuth();
  const { locale } = useI18n();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  const initialSearch = searchParams.get('search') ?? '';
  const initialRisk = searchParams.get('risk') ?? '';
  const initialStatut = searchParams.get('statut') ?? '';
  const initialView = searchParams.get('view') === 'corbeille' ? 'corbeille' : 'liste';

  const [page, setPage] = useState(0);
  const [search, setSearch] = useState(initialSearch);
  const [riskFilter, setRiskFilter] = useState<RiskFilter>(toRiskFilter(initialRisk));
  const [statutFilter, setStatutFilter] = useState<StatutFilter>(toStatutFilter(initialStatut));
  const [showFilters, setShowFilters] = useState(Boolean(initialRisk || initialStatut));
  const [view, setView] = useState<'liste' | 'corbeille'>(initialView);

  const syncUrl = useCallback((next: { search?: string; risk?: string; statut?: string; view?: string }) => {
    const params = new URLSearchParams();
    const s = next.search !== undefined ? next.search : search;
    const r = next.risk !== undefined ? next.risk : riskFilter;
    const st = next.statut !== undefined ? next.statut : statutFilter;
    const v = next.view !== undefined ? next.view : view;
    if (s) params.set('search', s);
    if (r) params.set('risk', r);
    if (st) params.set('statut', st);
    if (v === 'corbeille') params.set('view', 'corbeille');
    setSearchParams(params.toString() ? `?${params.toString()}` : '', { replace: true });
  }, [search, riskFilter, statutFilter, view, setSearchParams]);

  const { data, isLoading } = useQuery({
    queryKey: ['families', page, search, riskFilter, statutFilter, view],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (riskFilter) params.set('niveauRisque', riskFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const url = view === 'corbeille' ? '/families/trash' : '/families';
      const res = await api.get(`${url}?${params}`);
      return res.data as PageResponse<Family>;
    },
  });

  // Stats for all families (for the overview cards)
  const { data: allFamilies } = useQuery({
    queryKey: ['families', 'all-for-stats'],
    queryFn: async () => {
      const res = await api.get('/families?size=200');
      return (res.data as PageResponse<Family>).content;
    },
  });

  const stats = useMemo(() => {
    if (!allFamilies) return { total: 0, active: 0, inactive: 0, risque: 0, surveillance: 0 };
    return {
      total: allFamilies.length,
      active: allFamilies.filter(f => f.statut === 'ACTIVE').length,
      inactive: allFamilies.filter(f => f.statut !== 'ACTIVE').length,
      risque: allFamilies.filter(f => f.niveauRisque === 'A_RISQUE').length,
      surveillance: allFamilies.filter(f => f.niveauRisque === 'SOUS_SURVEILLANCE').length,
    };
  }, [allFamilies]);

  const restoreMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.patch(`/families/${id}/restore`);
    },
    onSuccess: () => {
      toast.success('Famille restaurée');
      queryClient.invalidateQueries({ queryKey: ['families'] });
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const columns: ColumnDef<Family>[] = [
    {
      header: 'Famille',
      cell: (family) => (
        <Link
          to={`/families/${family.id}`}
          className="flex items-center gap-3 group"
        >
          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center shadow-sm group-hover:shadow-md transition-shadow">
            <Users className="w-5 h-5 text-white" />
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-900 dark:text-gray-100 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
              {family.nom}
            </p>
            <p className="text-xs text-gray-400">
              Créée le {new Date(family.dateCreation).toLocaleDateString(locale, { day: 'numeric', month: 'short', year: 'numeric' })}
            </p>
          </div>
        </Link>
      ),
    },
    {
      header: 'Chef',
      cell: (family) => (
        <div className="flex items-center gap-1.5">
          <Crown className="w-3 h-3 text-amber-500" />
          <span className="text-sm text-gray-600 dark:text-gray-300">
            {family.chefFamilleNom || family.chefFamilleId?.slice(0, 8) + '…'}
          </span>
        </div>
      ),
    },
    {
      header: 'Statut',
      cell: (family) => (
        <span className={`inline-flex items-center gap-1.5 ${
          family.statut === 'ACTIVE' ? 'badge-success' : 'badge-gray'
        }`}>
          <span className={`w-1.5 h-1.5 rounded-full ${
            family.statut === 'ACTIVE' ? 'bg-green-500 shadow-[0_0_4px_rgba(34,197,94,0.5)]' : 'bg-gray-400'
          }`} />
          {family.statut === 'ACTIVE' ? 'Active' : 'Inactive'}
        </span>
      ),
    },
    {
      header: 'Risque',
      cell: (family) => {
        const niveau = family.niveauRisque || 'NORMAL';
        const styles = {
          NORMAL: 'badge-success',
          SOUS_SURVEILLANCE: 'badge-warning',
          A_RISQUE: 'badge-danger',
        } as const;
        const labels = {
          NORMAL: 'Normale',
          SOUS_SURVEILLANCE: 'Sous surveillance',
          A_RISQUE: 'À risque',
        } as const;
        return (
          <span className={`inline-flex items-center gap-1.5 ${styles[niveau]}`}>
            {niveau !== 'NORMAL' && <AlertTriangle className="w-3 h-3" />}
            {labels[niveau]}
          </span>
        );
      },
    },
    {
      header: 'Création',
      cell: (family) => (
        <span className="text-sm text-gray-500 dark:text-gray-400">
          {new Date(family.dateCreation).toLocaleDateString(locale, {
            day: 'numeric', month: 'short', year: 'numeric',
          })}
        </span>
      ),
    },
    {
      header: '',
      cell: (family) => (
        <Link to={`/families/${family.id}`} className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
          <ChevronRight className="w-4 h-4 text-gray-300 dark:text-gray-600" />
        </Link>
      ),
    },
  ];

  const trashColumns: ColumnDef<Family>[] = [
    {
      header: 'Famille',
      cell: (family) => (
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
            <Users className="w-5 h-5 text-gray-400" />
          </div>
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-gray-100">{family.nom}</p>
            <p className="text-xs text-gray-400">
              {family.chefFamilleNom || '—'}
            </p>
          </div>
        </div>
      ),
    },
    {
      header: 'Date création',
      cell: (family) => (
        <span className="text-sm text-gray-500">
          {new Date(family.dateCreation).toLocaleDateString(locale)}
        </span>
      ),
    },
    {
      header: 'Action',
      cell: (family) => (
        <button
          onClick={() => restoreMutation.mutate(family.id)}
          disabled={restoreMutation.isPending}
          className="btn-secondary btn-xs"
        >
          <RotateCcw className="w-3.5 h-3.5" /> Restaurer
        </button>
      ),
    },
  ];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Users className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Familles de disciples</h1>
          </div>
          <p className="page-subtitle">Organisation en familles — CRUD complet, suivi et alertes</p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          <button
            onClick={() => { const next = view === 'corbeille' ? 'liste' : 'corbeille'; setView(next); setPage(0); syncUrl({ view: next }); }}
            className={`btn-secondary btn-sm ${view === 'corbeille' ? 'bg-red-50 dark:bg-red-900/20 border-red-300 dark:border-red-700' : ''}`}
          >
            <Trash2 className="w-4 h-4" />
            {view === 'corbeille' ? 'Voir les familles' : 'Corbeille'}
          </button>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300 dark:border-primary-700' : ''}`}
          >
            <Filter className="w-4 h-4" />
            Filtres
          </button>
          {(user?.activeRole === 'ADMIN' || user?.activeRole === 'PASTEUR') && (
            <Link to="/families/compare" className="btn-secondary btn-sm">
              <BarChart3 className="w-4 h-4" />
              Comparer
            </Link>
          )}
          {(user?.activeRole === 'ADMIN' || user?.activeRole === 'PASTEUR' || user?.activeRole === 'CHEF_DE_FAMILLE') && (
            <Link to="/families/new" className="btn-primary btn-sm">
              <Plus className="w-4 h-4" />
              Nouvelle famille
            </Link>
          )}
        </div>
      </div>

      {/* Stats Cards */}
      {view === 'liste' && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Total', value: stats.total, icon: Users, color: 'from-primary-500 to-primary-600', filter: '' },
            { label: 'Actives', value: stats.active, icon: CheckCircle, color: 'from-emerald-500 to-green-500', filter: 'ACTIVE' },
            { label: 'À risque', value: stats.risque, icon: AlertTriangle, color: 'from-red-500 to-rose-500', filter: 'A_RISQUE' },
            { label: 'Surveillance', value: stats.surveillance, icon: Eye, color: 'from-amber-500 to-orange-500', filter: 'SOUS_SURVEILLANCE' },
          ].map((stat, i) => (
            <button
              key={stat.label}
              type="button"
              onClick={() => {
                if (stat.label === 'Total') {
                  setRiskFilter(''); setStatutFilter(''); setPage(0); syncUrl({ risk: '', statut: '' });
                } else if (stat.label === 'Actives') {
                  setStatutFilter('ACTIVE'); setPage(0); syncUrl({ statut: 'ACTIVE' });
                } else if (stat.label === 'À risque') {
                  setRiskFilter('A_RISQUE'); setPage(0); syncUrl({ risk: 'A_RISQUE' });
                } else {
                  setRiskFilter('SOUS_SURVEILLANCE'); setPage(0); syncUrl({ risk: 'SOUS_SURVEILLANCE' });
                }
              }}
              className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
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
      )}

      {/* Search & Filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-primary-500 transition-colors" />
            <input
              type="text"
              placeholder="Rechercher par nom de famille, chef..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); syncUrl({ search: e.target.value }); }}
              className="input pl-10"
            />
          </div>
        </div>

        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20 dark:border-white/[0.06] animate-slide-up">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Risque</span>
              <select
                value={riskFilter}
                onChange={(e) => { setRiskFilter(toRiskFilter(e.target.value)); setPage(0); syncUrl({ risk: e.target.value }); }}
                className="input w-auto text-sm"
              >
                <option value="">Tous</option>
                <option value="NORMAL">Normale</option>
                <option value="SOUS_SURVEILLANCE">Sous surveillance</option>
                <option value="A_RISQUE">À risque</option>
              </select>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Statut</span>
              <select
                value={statutFilter}
                onChange={(e) => { setStatutFilter(toStatutFilter(e.target.value)); setPage(0); syncUrl({ statut: e.target.value }); }}
                className="input w-auto text-sm"
              >
                <option value="">Tous</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* Table */}
      {view === 'corbeille' ? (
        <div className="glass-card overflow-hidden animate-slide-up">
          {isLoading ? (
            <div className="p-8 space-y-3">
              {[...Array(4)].map((_, i) => <div key={i} className="skeleton h-12 w-full rounded-xl" />)}
            </div>
          ) : data && data.content.length === 0 ? (
            <div className="p-14 text-center">
              <div className="inline-flex p-4 rounded-2xl bg-green-100 dark:bg-green-900/20 mb-4">
                <RotateCcw className="w-10 h-10 text-green-500" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Corbeille vide</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">Aucune famille supprimée. Les familles supprimées apparaissent ici pour restauration.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table w-full min-w-[560px]">
                <thead>
                  <tr>
                    <th>Famille</th>
                    <th>Date création</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {(data?.content || []).map((family) => (
                    <tr key={family.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                      <td className="font-medium text-gray-900 dark:text-gray-100">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-lg bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                            <Users className="w-4 h-4 text-gray-400" />
                          </div>
                          {family.nom}
                        </div>
                      </td>
                      <td className="text-sm text-gray-500">
                        {new Date(family.dateCreation).toLocaleDateString(locale)}
                      </td>
                      <td className="text-right">
                        <button
                          onClick={() => restoreMutation.mutate(family.id)}
                          disabled={restoreMutation.isPending}
                          className="btn-secondary btn-xs"
                        >
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
        <DataTable<Family>
          columns={columns}
          data={data?.content || []}
          isLoading={isLoading}
          emptyMessage="Aucune famille trouvée"
          emptyIcon={<Users className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
        />
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4 animate-fade-in">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            <span className="font-medium">{data.number * data.size + 1}</span>
            {' '}à{' '}
            <span className="font-medium">{Math.min((data.number + 1) * data.size, data.totalElements)}</span>
            {' '}sur{' '}
            <span className="font-medium">{data.totalElements}</span> résultats
          </p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">
              ← Précédent
            </button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">
              Suivant →
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
