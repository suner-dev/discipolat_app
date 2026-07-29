import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import DataTable from '@/components/shared/DataTable';
import type { Family, Department, PageResponse } from '@/types';
import type { ColumnDef } from '@/types/table';
import { Users, Plus, Building2, ChevronRight, Sparkles } from 'lucide-react';

export default function FamiliesPage() {
  const [page, setPage] = useState(0);
  const [deptFilter, setDeptFilter] = useState('');

  const { data: departments } = useQuery({
    queryKey: ['departments', 'all'],
    queryFn: async () => {
      const res = await api.get('/departments?size=100');
      return res.data.content as Department[];
    },
  });

  const { data, isLoading } = useQuery({
    queryKey: ['families', page, deptFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (deptFilter) params.set('departementId', deptFilter);
      const res = await api.get(`/families?${params}`);
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
              {family.departementId ? 'Département rattaché' : 'Aucun département'}
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
          <Link to="/families/new" className="btn-primary btn-sm">
            <Plus className="w-4 h-4" />
            Nouvelle famille
          </Link>
        </div>
      </div>

      {/* Filter */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex items-center gap-3">
          <Building2 className="w-4 h-4 text-gray-400" />
          <select
            value={deptFilter}
            onChange={(e) => { setDeptFilter(e.target.value); setPage(0); }}
            className="input w-auto"
          >
            <option value="">Tous les départements</option>
            {departments?.map((dept) => (
              <option key={dept.id} value={dept.id}>{dept.nom}</option>
            ))}
          </select>
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
