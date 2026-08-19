import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { AuditRecentActivity } from '@/types';
import {
  Shield, Search, Filter, FileDown, Eye, Clock, User as UserIcon,
  Calendar, ArrowLeft, History, ChevronRight,
} from 'lucide-react';

export default function PasteurAuditTab() {
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [actionFilter, setActionFilter] = useState('');
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(0);
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['audit', 'pasteur', page, search, typeFilter, actionFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '30', page: String(page) });
      if (search) params.set('search', search);
      if (typeFilter) params.set('entiteType', typeFilter);
      if (actionFilter) params.set('action', actionFilter);
      const res = await api.get(`/audit?${params}`);
      return res.data;
    },
  });

  const entityTypeIcon = (type: string) => {
    const icons: Record<string, { emoji: string; color: string }> = {
      SOUL: { emoji: '💜', color: 'bg-purple-100 dark:bg-purple-900/30' },
      USER: { emoji: '👤', color: 'bg-blue-100 dark:bg-blue-900/30' },
      FAMILY: { emoji: '👨‍👩‍👧', color: 'bg-green-100 dark:bg-green-900/30' },
      DEPARTMENT: { emoji: '🏢', color: 'bg-amber-100 dark:bg-amber-900/30' },
      TRANSFER: { emoji: '🔄', color: 'bg-violet-100 dark:bg-violet-900/30' },
      ALERT: { emoji: '🔔', color: 'bg-red-100 dark:bg-red-900/30' },
      REPORT: { emoji: '📋', color: 'bg-emerald-100 dark:bg-emerald-900/30' },
      EVENT: { emoji: '📅', color: 'bg-orange-100 dark:bg-orange-900/30' },
      VISIT: { emoji: '🏠', color: 'bg-teal-100 dark:bg-teal-900/30' },
      PRAYER: { emoji: '🙏', color: 'bg-indigo-100 dark:bg-indigo-900/30' },
    };
    return icons[type] || { emoji: '📝', color: 'bg-gray-100 dark:bg-gray-700' };
  };

  const actionColor = (action: string) => {
    if (action?.includes('CREATE') || action?.includes('ADD')) return 'text-green-600';
    if (action?.includes('UPDATE') || action?.includes('MODIFY')) return 'text-amber-600';
    if (action?.includes('DELETE') || action?.includes('REMOVE')) return 'text-red-600';
    if (action?.includes('TRANSFER')) return 'text-violet-600';
    if (action?.includes('RESOLVE')) return 'text-blue-600';
    return 'text-gray-600';
  };

  // Build export URL with current filters
  const exportUrl = `/api/v1/audit/export?${new URLSearchParams({
    ...(search && { search }),
    ...(typeFilter && { entiteType: typeFilter }),
    ...(actionFilter && { action: actionFilter }),
  }).toString()}`;

  return (
    <div className="animate-slide-up">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Shield className="w-5 h-5 text-gray-500" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Journal d'Audit</h2>
          {data && <span className="text-xs text-gray-400">({data.totalElements || 0})</span>}
        </div>
        <div className="flex gap-2">
          <a href={exportUrl} target="_blank" rel="noreferrer" className="btn-primary btn-sm">
            <FileDown className="w-4 h-4" /> Export CSV
          </a>
          <Link to="/audit" className="btn-secondary btn-sm"><Eye className="w-4 h-4" /> Page complète</Link>
        </div>
      </div>

      {/* Search + Filters */}
      <div className="glass-card p-4 mb-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input type="text" placeholder="Rechercher dans l'audit..." value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} className="input pl-10" />
          </div>
          <button onClick={() => setShowFilters(!showFilters)} className={`btn-secondary btn-sm ${showFilters ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-300' : ''}`}>
            <Filter className="w-4 h-4" /> Filtres avancés
          </button>
        </div>
        {showFilters && (
          <div className="flex flex-wrap gap-3 mt-4 pt-4 border-t border-white/20">
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Type</span>
              <select value={typeFilter} onChange={e => { setTypeFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Tous</option>
                <option value="SOUL">Âme</option>
                <option value="USER">Utilisateur</option>
                <option value="FAMILY">Famille</option>
                <option value="DEPARTMENT">Département</option>
                <option value="TRANSFER">Transfert</option>
                <option value="ALERT">Alerte</option>
                <option value="REPORT">Rapport</option>
                <option value="EVENT">Événement</option>
                <option value="VISIT">Visite</option>
                <option value="PRAYER">Prière</option>
              </select>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Action</span>
              <select value={actionFilter} onChange={e => { setActionFilter(e.target.value); setPage(0); }} className="input w-auto text-sm">
                <option value="">Toutes</option>
                <option value="CREATE">Création</option>
                <option value="UPDATE">Modification</option>
                <option value="DELETE">Suppression</option>
                <option value="TRANSFER">Transfert</option>
                <option value="RESOLVE">Résolution</option>
              </select>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Du</span>
              <input type="date" className="input w-auto text-sm" value={dateFrom} onChange={e => setDateFrom(e.target.value)} />
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-gray-400 font-medium">Au</span>
              <input type="date" className="input w-auto text-sm" value={dateTo} onChange={e => setDateTo(e.target.value)} />
            </div>
          </div>
        )}
      </div>

      {/* Activity timeline */}
      {isLoading ? (
        <div className="space-y-3">{[...Array(8)].map((_, i) => <div key={i} className="glass-card p-4"><div className="skeleton h-12 w-full rounded-xl" /></div>)}</div>
      ) : (
        <div className="space-y-2">
          {(data?.content || []).map((a: AuditRecentActivity) => {
            const typeInfo = entityTypeIcon(a.entiteType);
            const color = actionColor(a.action);
            // Client-side date filtering
            if (dateFrom && new Date(a.createdAt) < new Date(dateFrom)) return null;
            if (dateTo && new Date(a.createdAt) > new Date(dateTo + 'T23:59:59')) return null;

            return (
              <div key={a.id} className="glass-card p-3 hover:bg-white/60 dark:hover:bg-gray-800/20 transition-colors">
                <div className="flex items-start gap-3">
                  <div className={`w-8 h-8 rounded-full ${typeInfo.color} flex items-center justify-center text-sm flex-shrink-0 mt-0.5`}>
                    {typeInfo.emoji}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-medium text-gray-900 dark:text-gray-100">
                      <span className="text-primary-600 dark:text-primary-400">{a.utilisateurNom}</span>
                      {' '}<span className={`${color} font-semibold`}>{a.action?.toLowerCase().replace(/_/g, ' ')}</span>
                    </p>
                    <div className="flex items-center gap-2 mt-1 text-[10px] text-gray-400">
                      <span className={`badge text-[9px] ${typeInfo.color}`}>{typeInfo.emoji} {a.entiteType}</span>
                      <span>·</span>
                      <Clock className="w-3 h-3" />
                      <span>{new Date(a.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                  </div>
                  <ChevronRight className="w-4 h-4 text-gray-300 flex-shrink-0 mt-1" />
                </div>
              </div>
            );
          })}

          {(!data?.content || data.content.length === 0) && (
            <div className="glass-card p-14 text-center">
              <Shield className="w-10 h-10 text-gray-300 mx-auto mb-2" />
              <p className="text-sm text-gray-400">Aucune activité d'audit</p>
            </div>
          )}
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
