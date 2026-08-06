import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import type { Family, PageResponse } from '@/types';
import type { ColumnDef } from '@/types/table';
import { Users, Plus, Building2, ChevronRight, BarChart3, AlertTriangle } from 'lucide-react';

export default function FamiliesPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['families', page],
    queryFn: async () => {
      const res = await api.get(`/families?size=20&page=${page}`);
      return res.data as PageResponse<Family>;
    },
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
              Famille indépendante des départements
            </p>
          </div>
        </Link>
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
          {new Date(family.dateCreation).toLocaleDateString('fr-FR', {
            day: 'numeric', month: 'short', year: 'numeric',
          })}
        </span>
      ),
    },
    {
      header: '',
      cell: () => (
        <ChevronRight className="w-4 h-4 text-gray-300 dark:text-gray-600" />
      ),
    },
  ];

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Users className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Familles de disciples</h1>
          </div>
          <p className="page-subtitle">Organisation en familles de disciples</p>
        </div>
        <div className="flex gap-2 animate-fade-in">
          {user?.activeRole === 'PASTEUR' && (
            <Link to="/families/compare" className="btn-secondary btn-sm">
              <BarChart3 className="w-4 h-4" />
              Comparer
            </Link>
          )}
          <Link to="/families/new" className="btn-primary btn-sm">
            <Plus className="w-4 h-4" />
            Nouvelle famille
          </Link>
        </div>
      </div>

      <DataTable<Family>
        columns={columns}
        data={data?.content || []}
        isLoading={isLoading}
        emptyMessage="Aucune famille trouvée"
        emptyIcon={<Users className="w-16 h-16 text-gray-300 dark:text-gray-600" />}
      />

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-gray-500">{data.number + 1} / {data.totalPages}</p>
          <div className="flex gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={data.first} className="btn-secondary btn-sm">← Précédent</button>
            <button onClick={() => setPage(p => p + 1)} disabled={data.last} className="btn-primary btn-sm">Suivant →</button>
          </div>
        </div>
      )}
    </div>
  );
}
