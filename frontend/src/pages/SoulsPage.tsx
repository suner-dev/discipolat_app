import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import type { Soul, PageResponse, TypeDisciple, StatutAme } from '@/types';
import type { ColumnDef } from '@/types/table';
import {
  Heart,
  Plus,
  Search,
  Filter,
  ChevronDown,
  Sparkles,
} from 'lucide-react';

const TYPE_LABELS: Record<TypeDisciple, string> = {
  NOUVEL_ARRIVANT: 'Nouvel arrivant',
  NOUVEAU_CONVERTI: 'Nouveau converti',
};

const STATUT_LABELS: Record<StatutAme, string> = {
  NOUVEAU_CONVERTI: 'Nouveau converti',
  NOUVEL_ARRIVANT: 'Nouvel arrivant',
  EN_INTEGRATION: 'En intégration',
  ACTIF: 'Actif',
  EN_VEILLE: 'En veille',
  DECROCHE: 'Décroché',
};

const STATUT_STYLES: Record<StatutAme, string> = {
  NOUVEAU_CONVERTI: 'badge-success',
  NOUVEL_ARRIVANT: 'badge-info',
  EN_INTEGRATION: 'badge-warning',
  ACTIF: 'badge-success',
  EN_VEILLE: 'badge-gray',
  DECROCHE: 'badge-danger',
};

export default function SoulsPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TypeDisciple | ''>('');
  const [statutFilter, setStatutFilter] = useState<StatutAme | ''>('');
  const [showFilters, setShowFilters] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['souls', page, search, typeFilter, statutFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (typeFilter) params.set('typeDisciple', typeFilter);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/souls?${params}`);
      return res.data as PageResponse<Soul>;
    },
  });

  const columns: ColumnDef<Soul>[] = [
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
          {TYPE_LABELS[soul.typeDisciple]}
        </span>
      ),
    },
    {
      header: 'Statut',
      cell: (soul) => (
        <span className={STATUT_STYLES[soul.statut]}>
          {STATUT_LABELS[soul.statut]}
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
            onClick={() => setShowFilters(!showFilters)}
            className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300 dark:border-primary-700' : ''}`}
          >
            <Filter className="w-4 h-4" />
            Filtres
          </button>
          <Link to="/souls/new" className="btn-primary btn-sm">
            <Plus className="w-4 h-4" />
            Nouvelle âme
          </Link>
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
                <option value="NOUVEL_ARRIVANT">Nouvel arrivant</option>
                <option value="NOUVEAU_CONVERTI">Nouveau converti</option>
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
                <option value="ACTIF">Actif</option>
                <option value="EN_INTEGRATION">En intégration</option>
                <option value="EN_VEILLE">En veille</option>
                <option value="DECROCHE">Décroché</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* Table */}
      <DataTable<Soul>
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        emptyMessage="Aucune âme trouvée"
        emptyIcon={<Heart className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
      />

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
