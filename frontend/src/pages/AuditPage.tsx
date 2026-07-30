import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import { Activity, Search, Filter, Loader2, Clock, User, Shield } from 'lucide-react';

interface AuditEntry {
  id: string;
  utilisateurId: string;
  emailUtilisateur?: string;
  action: string;
  entiteType: string;
  entiteId?: string;
  details?: string;
  createdAt: string;
}

export default function AuditPage() {
  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [entityFilter, setEntityFilter] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['audit', page, entityFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (entityFilter) params.set('entiteType', entityFilter);
      const res = await api.get(`/audit?${params}`);
      return res.data as PageResponse<AuditEntry>;
    },
  });

  const getActionColor = (action: string) => {
    if (action.includes('DELETE') || action.includes('SUPPR')) return 'text-red-500 bg-red-50 dark:bg-red-900/20';
    if (action.includes('CREATE') || action.includes('CREATION')) return 'text-green-500 bg-green-50 dark:bg-green-900/20';
    if (action.includes('UPDATE') || action.includes('MODIF')) return 'text-blue-500 bg-blue-50 dark:bg-blue-900/20';
    return 'text-gray-500 bg-gray-50 dark:bg-gray-800/30';
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-gray-600 to-gray-700 text-white shadow-lg">
            <Activity className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Journal d'audit</h1>
            <p className="page-subtitle">Traçabilité de toutes les actions sensibles</p>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-3 mb-6">
        <div className="flex-1 min-w-[200px]">
          <div className="input-group">
            <Search className="w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Rechercher dans l'audit..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="input-field"
            />
          </div>
        </div>
        <select
          value={entityFilter}
          onChange={(e) => { setEntityFilter(e.target.value); setPage(0); }}
          className="input-field w-48"
        >
          <option value="">Toutes les entités</option>
          <option value="USER">Utilisateurs</option>
          <option value="FAMILY">Familles</option>
          <option value="SOUL">Âmes</option>
          <option value="REPORT">Rapports</option>
          <option value="DEPARTMENT">Départements</option>
        </select>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
        </div>
      ) : (
        <div className="glass-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Utilisateur</th>
                  <th>Action</th>
                  <th>Entité</th>
                  <th>Détails</th>
                </tr>
              </thead>
              <tbody>
                {data?.content.map((entry) => (
                  <tr key={entry.id}>
                    <td className="text-sm text-gray-500">
                      <div className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {new Date(entry.createdAt).toLocaleString('fr-FR')}
                      </div>
                    </td>
                    <td>
                      <div className="flex items-center gap-1">
                        <User className="w-3 h-3 text-gray-400" />
                        <span className="text-sm">{entry.emailUtilisateur || entry.utilisateurId?.slice(0, 8)}</span>
                      </div>
                    </td>
                    <td>
                      <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-md text-xs font-medium ${getActionColor(entry.action)}`}>
                        <Shield className="w-3 h-3" />
                        {entry.action}
                      </span>
                    </td>
                    <td className="text-sm">{entry.entiteType}</td>
                    <td className="text-sm text-gray-500 max-w-xs truncate">{entry.details || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {data && (
            <div className="flex items-center justify-between px-6 py-4 border-t border-gray-100 dark:border-gray-800">
              <span className="text-sm text-gray-500">
                Page {page + 1} sur {data.totalPages} ({data.totalElements} entrées)
              </span>
              <div className="flex gap-2">
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="btn-ghost btn-sm">
                  Précédent
                </button>
                <button onClick={() => setPage(p => p + 1)} disabled={page >= data.totalPages - 1} className="btn-ghost btn-sm">
                  Suivant
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
