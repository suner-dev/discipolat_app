import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import type { Alert, PageResponse } from '@/types';
import {
  Bell,
  BellRing,
  CheckCircle2,
  AlertTriangle,
  Clock,
  Loader2,
  Filter,
  Shield,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import toast from 'react-hot-toast';

export default function AlertsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['alerts', page, filter],
    queryFn: async () => {
      const params = new URLSearchParams({ size: '20', page: String(page) });
      if (filter) params.set('statut', filter);
      const res = await api.get(`/alerts?${params}`);
      return res.data as PageResponse<Alert>;
    },
  });

  const resolveMutation = useMutation({
    mutationFn: async (id: string) => {
      await api.patch(`/alerts/${id}/resolve`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      toast.success('Alerte résolue avec succès');
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const activeAlerts = data?.content.filter(a => a.statut === 'ACTIVE') || [];
  const resolvedAlerts = data?.content.filter(a => a.statut === 'RESOLUE') || [];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <BellRing className="w-5 h-5 text-amber-500" />
            <h1 className="page-title">Alertes</h1>
          </div>
          <p className="page-subtitle">
            {activeAlerts.length > 0
              ? `${activeAlerts.length} alerte${activeAlerts.length > 1 ? 's' : ''} active${activeAlerts.length > 1 ? 's' : ''} nécessitant votre attention`
              : 'Toutes les alertes sont traitées'}
          </p>
        </div>
        <div className="flex items-center gap-2 animate-fade-in">
          <Filter className="w-4 h-4 text-gray-400" />
          <select
            value={filter}
            onChange={(e) => { setFilter(e.target.value); setPage(0); }}
            className="input w-auto"
          >
            <option value="">Toutes les alertes</option>
            <option value="ACTIVE">Actives uniquement</option>
            <option value="RESOLUE">Résolues uniquement</option>
          </select>
        </div>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8 animate-slide-up">
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-red-100 dark:bg-red-900/30">
              <BellRing className="w-5 h-5 text-red-600 dark:text-red-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">{activeAlerts.length}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Alertes actives</p>
            </div>
          </div>
        </div>
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-green-100 dark:bg-green-900/30">
              <ShieldCheck className="w-5 h-5 text-green-600 dark:text-green-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">{resolvedAlerts.length}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Alertes résolues</p>
            </div>
          </div>
        </div>
        <div className="glass-card p-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-blue-100 dark:bg-blue-900/30">
              <Shield className="w-5 h-5 text-blue-600 dark:text-blue-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100 font-mono">{data?.totalElements || 0}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">Total</p>
            </div>
          </div>
        </div>
      </div>

      {/* Alerts list */}
      {isLoading ? (
        <div className="space-y-3">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="glass-card p-4 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="flex items-center gap-4">
                <div className="skeleton w-10 h-10 rounded-xl" />
                <div className="flex-1">
                  <div className="skeleton h-4 w-48 mb-2" />
                  <div className="skeleton h-3 w-32" />
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : data?.content.length === 0 ? (
        <div className="glass-card p-14 text-center animate-scale-in">
          <div className="inline-flex p-4 rounded-2xl bg-green-100 dark:bg-green-900/20 mb-4">
            <Bell className="w-10 h-10 text-green-500" />
          </div>
          <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">Aucune alerte</h3>
          <p className="text-sm text-gray-500 dark:text-gray-400">Toutes les alertes ont été traitées</p>
        </div>
      ) : (
        <div className="space-y-3">
          {data?.content.map((alert, i) => (
            <div
              key={alert.id}
              className={`glass-card p-4 animate-slide-up hover-lift ${
                alert.statut === 'ACTIVE'
                  ? 'border-l-4 border-l-red-500'
                  : 'border-l-4 border-l-green-500 opacity-70'
              }`}
              style={{ animationDelay: `${i * 40}ms` }}
            >
              <div className="flex items-start gap-4">
                {/* Icon */}
                <div className={`p-2.5 rounded-xl ${
                  alert.statut === 'ACTIVE'
                    ? 'bg-red-100 dark:bg-red-900/30'
                    : 'bg-green-100 dark:bg-green-900/30'
                }`}>
                  {alert.statut === 'ACTIVE' ? (
                    <AlertTriangle className="w-5 h-5 text-red-600 dark:text-red-400" />
                  ) : (
                    <CheckCircle2 className="w-5 h-5 text-green-600 dark:text-green-400" />
                  )}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-1">
                    <span className={`badge text-[10px] ${
                      alert.typeAlerte === 'ABSENCE_48H' ? 'badge-danger' : 'badge-warning'
                    }`}>
                      {alert.typeAlerte === 'ABSENCE_48H' ? '🚨 Absence 48h' :
                       alert.typeAlerte === 'RAPPORT_NON_SOUMIS' ? '📋 Rapport non soumis' :
                       '📊 Rapport famille'}
                    </span>
                    <span className={`badge text-[10px] ${alert.statut === 'ACTIVE' ? 'badge-danger' : 'badge-success'}`}>
                      {alert.statut === 'ACTIVE' ? 'Active' : 'Résolue'}
                    </span>
                  </div>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100">
                    {alert.message}
                  </p>
                  <div className="flex items-center gap-2 mt-1.5">
                    <Clock className="w-3 h-3 text-gray-400" />
                    <span className="text-xs text-gray-400">
                      {new Date(alert.dateDeclenchement).toLocaleDateString('fr-FR', {
                        day: 'numeric', month: 'short', year: 'numeric',
                        hour: '2-digit', minute: '2-digit',
                      })}
                    </span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex-shrink-0">
                  {alert.statut === 'ACTIVE' && (
                    <button
                      onClick={() => resolveMutation.mutate(alert.id)}
                      disabled={resolveMutation.isPending}
                      className="btn-glow btn-sm"
                    >
                      {resolveMutation.isPending ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <CheckCircle2 className="w-4 h-4" />
                      )}
                      Résoudre
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between mt-6 animate-fade-in">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Page {data.number + 1} / {data.totalPages}
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
