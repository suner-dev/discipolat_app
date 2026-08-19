import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '@/lib/api';
import type { PageResponse } from '@/types';
import {
  ArrowLeftRight, Plus, Search, Eye, Filter, CheckCircle, XCircle,
  Clock, Loader2, AlertTriangle, ChevronRight,
} from 'lucide-react';
import toast from 'react-hot-toast';

interface Transfer {
  id: string; type: string; statut: string; personneNom?: string; cible?: string;
  demandeurNom?: string; dateSoumission?: string; priorite?: string;
  typeTransfert?: string; motif?: string; createdAt: string;
}

export default function PasteurTransfersTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statutFilter, setStatutFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['transfers', 'pasteur', page, search, statutFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (search) params.set('search', search);
      if (statutFilter) params.set('statut', statutFilter);
      const res = await api.get(`/transfers?${params}`);
      return res.data as PageResponse<Transfer>;
    },
  });

  const statutBadge = (s: string) => {
    const map: Record<string, { cls: string; icon: any }> = {
      BROUILLON: { cls: 'badge-gray', icon: null },
      SOUMIS: { cls: 'badge-info', icon: Clock },
      EN_ATTENTE_VALIDATION: { cls: 'badge-warning', icon: AlertTriangle },
      VALIDE: { cls: 'badge-success', icon: CheckCircle },
      EXECUTE: { cls: 'badge-success', icon: CheckCircle },
      REFUSE: { cls: 'badge-error', icon: XCircle },
      ANNULE: { cls: 'badge-gray', icon: null },
      ARCHIVE: { cls: 'badge-info', icon: null },
    };
    const m = map[s] || { cls: 'badge-info', icon: null };
    const Icon = m.icon;
    return <span className={`badge text-[10px] ${m.cls}`}>{Icon && <Icon className="w-3 h-3 inline mr-1" />}{s?.replace(/_/g, ' ')}</span>;
  };

  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <ArrowLeftRight className="w-5 h-5 text-violet-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Transferts</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements})</span>}
        </div>
        <div className="flex gap-2">
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres
          </button>
          <Link to="/transfers/new" className="btn-primary btn-sm"><Plus className="w-4 h-4" /> Nouveau transfert</Link>
          <Link to="/admin/transfers" className="btn-secondary btn-sm"><Eye className="w-4 h-4" /> Admin workflows</Link>
        </div>
      </div>

      <div className="glass-card p-4 mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher un transfert..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
        </div>
        {showFilters && (
          <div className="flex gap-3 mt-4 pt-4 border-t border-white/20">
            <select value={statutFilter} onChange={e => { setStatutFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
              <option value="">Tous les statuts</option>
              <option value="SOUMIS">Soumis</option>
              <option value="EN_ATTENTE_VALIDATION">En attente</option>
              <option value="VALIDE">Validé</option>
              <option value="EXECUTE">Exécuté</option>
              <option value="REFUSE">Refusé</option>
            </select>
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">{[...Array(5)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="space-y-2">
          {(data?.content || []).map(t => (
            <Link key={t.id} to={`/transfers/${t.id}`} className="glass-card p-4 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors block">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3 min-w-0">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${
                    t.statut === 'EXECUTE' ? 'bg-green-50 dark:bg-green-900/20' :
                    t.statut === 'REFUSE' ? 'bg-red-50 dark:bg-red-900/20' :
                    t.statut === 'EN_ATTENTE_VALIDATION' ? 'bg-amber-50 dark:bg-amber-900/20' :
                    'bg-blue-50 dark:bg-blue-900/20'
                  }`}>
                    <ArrowLeftRight className={`w-5 h-5 ${
                      t.statut === 'EXECUTE' ? 'text-green-500' :
                      t.statut === 'REFUSE' ? 'text-red-500' :
                      t.statut === 'EN_ATTENTE_VALIDATION' ? 'text-amber-500' :
                      'text-blue-500'
                    }`} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                      {t.personneNom || '—'} → {t.cible || '—'}
                    </p>
                    <div className="flex items-center gap-2 text-[10px] text-gray-400 mt-0.5">
                      <span>{t.type?.replace(/_/g, ' ')}</span>
                      {t.demandeurNom && <span>Par {t.demandeurNom}</span>}
                      {t.dateSoumission && <span>{new Date(t.dateSoumission).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}</span>}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  {statutBadge(t.statut)}
                  {t.priorite === 'HAUTE' && <span className="text-[8px] bg-red-100 dark:bg-red-900/30 text-red-600 px-1.5 py-0.5 rounded-full font-semibold uppercase">Priorité</span>}
                  <ChevronRight className="w-4 h-4 text-gray-400" />
                </div>
              </div>
            </Link>
          ))}
          {(data?.content || []).length === 0 && <div className="glass-card p-14 text-center"><ArrowLeftRight className="w-10 h-10 text-gray-300 mx-auto mb-2" /><p className="text-sm text-gray-400">Aucun transfert</p></div>}
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
    </div>
  );
}
