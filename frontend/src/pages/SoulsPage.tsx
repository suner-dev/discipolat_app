import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import { useDictionaries } from '@/hooks/useDictionaries';
import type { Soul, PageResponse, TypeDisciple, StatutAme } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Heart,
  Plus,
  Search,
  Filter,
  ChevronDown,
  Sparkles,
  Star,
  Trash2,
  RotateCcw,
} from 'lucide-react';
import toast from 'react-hot-toast';

/** Replis (dictionnaires indisponibles) — les valeurs réelles viennent de la base. */
const TYPE_FALLBACK: Record<string, string> = {
  NOUVEL_ARRIVANT: 'Nouvel arrivant',
  NOUVEAU_CONVERTI: 'Nouveau converti',
};

const STATUT_FALLBACK: Record<string, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti',
  NOUVEL_ARRIVANT: 'Nouvel arrivant',
  EN_INTEGRATION: 'En intégration',
  ACTIF: 'Actif',
  EN_VEILLE: 'En veille',
  DECROCHE: 'Décroché',
};

export default function SoulsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeDisciple | ''>('');
  const [statutFilter, setStatutFilter] = useState<StatutAme | ''>('');
  const [showFilters, setShowFilters] = useState(false);
  const [view, setView] = useState<'liste' | 'corbeille'>('liste');
  const queryClient = useQueryClient();
  const dictionaries = useDictionaries();

  const typeEntries = useMemo(() => {
    const configured = dictionaries.options('SOUL_TYPE');
    return configured.length > 0 ? configured.map((e) => ({ code: e.code, label: e.label })) : Object.entries(TYPE_FALLBACK).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const statusEntries = useMemo(() => {
    const configured = dictionaries.options('SOUL_STATUS');
    return configured.length > 0 ? configured.map((e) => ({ code: e.code, label: e.label })) : Object.entries(STATUT_FALLBACK).map(([code, label]) => ({ code, label }));
  }, [dictionaries]);

  const typeLabel = (code: string) => dictionaries.label('SOUL_TYPE', code) || TYPE_FALLBACK[code] || code;
  const statusLabel = (code: string) => dictionaries.label('SOUL_STATUS', code) || STATUT_FALLBACK[code] || code;

  const { data, isLoading } = useQuery({
    queryKey: ['souls', page, search, typeFilter, statutFilter, view],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (typeFilter) params.set('typeDisciple', typeFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const url = view === 'corbeille' ? '/souls/trash' : '/souls';
      const res = await api.get(`${url}?${params}`);
      return res.data as PageResponse<Soul>;
    },
  });

  const { data: favorites = [] } = useQuery({
    queryKey: ['favorites', 'souls'],
    queryFn: async () => (await api.get('/favorites/souls')).data as { entityId: string; nom: string }[],
  });
  const favoriteIds = new Set(favorites.map((f) => f.entityId));

  const favoriteMutation = useMutation({
    mutationFn: async (soulId: string) => {
      const res = await api.post('/favorites/toggle', { entityType: 'SOUL', entityId: soulId });
      return res.data as { favorite: boolean };
    },
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['favorites', 'souls'] }),
    onError: () => toast.error("Impossible de mettre à jour le favori"),
  });

  const restoreMutation = useMutation({
    mutationFn: async (soulId: string) => {
      await api.patch(`/souls/${soulId}/restore`);
    },
    onSuccess: () => {
      toast.success('Âme restaurée avec succès ✨');
      queryClient.invalidateQueries({ queryKey: ['souls'] });
    },
    onError: () => toast.error("Erreur lors de la restauration"),
  });

  const columns: ColumnDef<Soul>[] = [
    {
      header: '',
      cell: (soul) => (
        <button
          onClick={(e) => { e.preventDefault(); favoriteMutation.mutate(soul.id); }}
          title={favoriteIds.has(soul.id) ? 'Retirer des favoris' : 'Ajouter aux favoris'}
          className={`p-1.5 rounded-lg transition-all hover:scale-110 ${
            favoriteIds.has(soul.id)
              ? 'text-amber-500 fill-amber-400'
              : 'text-gray-300 dark:text-gray-600 hover:text-amber-400'
          }`}
        >
          <Star className="w-4 h-4" />
        </button>
      ),
    },
    {
      header: 'Nom',
      cell: (soul) => (
        <Link
          to={`/souls/${soul.id}`}
          className="text-primary-600 hover:text-primary-700 font-medium transition-colors group"
        >
          <span className="group-hover:underline">
            {soul.prenom ? `${soul.prenom} ${soul.nom}` : soul.nom}
          </span>
        </Link>
      ),
    },
    {
      header: 'Type',
      cell: (soul) => (
        <span className={soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'badge-success' : 'badge-info'}>
          <span className={`inline-block w-1.5 h-1.5 rounded-full mr-1.5 ${soul.typeDisciple === 'NOUVEAU_CONVERTI' ? 'bg-green-500' : 'bg-blue-500'}`} />
          {typeLabel(soul.typeDisciple)}
        </span>
      ),
    },
    {
      header: 'Statut',
      cell: (soul) => (
        <span className="badge-info">
          {statusLabel(soul.statut)}
        </span>
      ),
    },
    {
      header: 'Email',
      accessor: 'email',
    },
    {
      header: 'Téléphone',
      accessor: 'telephone',
    },
    {
      header: 'Intégration',
      cell: (soul) => (
        <span className="text-gray-500 dark:text-gray-400 text-sm">
          {new Date(soul.dateIntegration).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })}
        </span>
      ),
    },
    {
      header: 'Dernier contact',
      cell: (soul) =>
        soul.dateDernierContact ? (
          <span className="inline-flex items-center gap-1 text-sm text-gray-500 dark:text-gray-400">
            <span className={`w-1.5 h-1.5 rounded-full ${new Date(soul.dateDernierContact) > new Date(Date.now() - 7 * 24 * 60 * 60 * 1000) ? 'bg-green-500' : 'bg-amber-500'}`} />
            {new Date(soul.dateDernierContact).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
          </span>
        ) : (
          <span className="text-gray-400 dark:text-gray-600 text-sm">—</span>
        ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Heart className="w-5 h-5 text-rose-500" />
            <h1 className="page-title">Âmes</h1>
          </div>
          <p className="page-subtitle">Gestion des disciples suivis par les faiseurs</p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          <button
            onClick={() => { setView(view === 'corbeille' ? 'liste' : 'corbeille'); setPage(0); }}
            className={`btn-secondary btn-sm ${view === 'corbeille' ? 'bg-red-50 dark:bg-red-900/20 border-red-300 dark:border-red-700' : ''}`}
            title="Âmes supprimées (restauration possible)"
          >
            <Trash2 className="w-4 h-4" />
            {view === 'corbeille' ? 'Voir les âmes' : 'Corbeille'}
          </button>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300 dark:border-primary-700' : ''}`}
          >
            <Filter className="w-4 h-4" />
            Filtres
          </button>
          {view !== 'corbeille' && (
            <Link to="/souls/new" className="btn-primary btn-sm">
              <Plus className="w-4 h-4" />
              Nouvelle âme
            </Link>
          )}
        </div>
      </div>

      {/* Search & Filters */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-primary-500 transition-colors" />
            <input
              type="text"
              placeholder="Rechercher par nom, email..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="input pl-10"
            />
          </div>
        </div>

        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20 dark:border-white/[0.06] animate-slide-up">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Type</span>
              <select
                value={typeFilter}
                onChange={(e) => { setTypeFilter(e.target.value as TypeDisciple | ''); setPage(0); }}
                className="input w-auto text-sm"
              >
                <option value="">Tous</option>
                {typeEntries.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
              </select>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Statut</span>
              <select
                value={statutFilter}
                onChange={(e) => { setStatutFilter(e.target.value as StatutAme | ''); setPage(0); }}
                className="input w-auto text-sm"
              >
                <option value="">Tous</option>
                {statusEntries.map((o) => (<option key={o.code} value={o.code}>{o.label}</option>))}
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
              <p className="text-sm text-gray-500 dark:text-gray-400">Aucune âme supprimée. Les âmes supprimées apparaissent ici pour restauration.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="table w-full min-w-[560px]">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Type</th>
                    <th>Email</th>
                    <th>Téléphone</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {(data?.content || []).map((soul) => (
                    <tr key={soul.id} className="hover:bg-white/40 dark:hover:bg-gray-800/20 transition-colors">
                      <td className="font-medium text-gray-900 dark:text-gray-100">
                        {soul.prenom ? `${soul.prenom} ${soul.nom}` : soul.nom}
                      </td>
                      <td className="text-sm text-gray-500">{typeLabel(soul.typeDisciple)}</td>
                      <td className="text-sm text-gray-500">{soul.email || '—'}</td>
                      <td className="text-sm text-gray-500">{soul.telephone || '—'}</td>
                      <td className="text-right">
                        <button
                          onClick={() => restoreMutation.mutate(soul.id)}
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
        <DataTable<Soul>
          columns={columns}
          data={data?.content || []}
          isLoading={isLoading}
          emptyMessage="Aucune âme trouvée"
          emptyIcon={<Heart className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
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
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={data.first}
              className="btn-secondary btn-sm"
            >
              ← Précédent
            </button>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={data.last}
              className="btn-primary btn-sm"
            >
              Suivant →
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
