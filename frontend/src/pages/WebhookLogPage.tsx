import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import api from '@/lib/api';
import { useI18n } from '@/i18n/index';
import {
  Loader2,
  Activity,
  CheckCircle2,
  XCircle,
  Clock,
  AlertTriangle,
  Filter,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  Globe,
  Shield,
  ShieldAlert,
  Zap,
} from 'lucide-react';

interface WebhookLogEntry {
  id: string;
  provider: string;
  endpoint: string;
  sourceIp: string | null;
  statusCode: number | null;
  statusLabel: string;
  reference: string | null;
  signatureValid: boolean | null;
  durationMs: number | null;
  errorMessage: string | null;
  requestBody: string | null;
  responseBody: string | null;
  createdAt: string;
}

interface LogStats {
  total: number;
  received: number;
  verified: number;
  processed: number;
  rejected: number;
  errors: number;
}

const PROVIDERS = ['', 'M_PESA', 'MTN_MOMO', 'ORANGE_MONEY', 'GENERIC'];
const STATUSES = ['', 'RECEIVED', 'VERIFIED', 'PROCESSED', 'REJECTED', 'ERROR'];

const STATUS_COLORS: Record<string, { bg: string; text: string; icon: typeof CheckCircle2 }> = {
  RECEIVED: { bg: 'bg-blue-100 dark:bg-blue-900/30', text: 'text-blue-600 dark:text-blue-400', icon: Globe },
  VERIFIED: { bg: 'bg-green-100 dark:bg-green-900/30', text: 'text-green-600 dark:text-green-400', icon: Shield },
  PROCESSED: { bg: 'bg-emerald-100 dark:bg-emerald-900/30', text: 'text-emerald-600 dark:text-emerald-400', icon: CheckCircle2 },
  REJECTED: { bg: 'bg-red-100 dark:bg-red-900/30', text: 'text-red-600 dark:text-red-400', icon: ShieldAlert },
  ERROR: { bg: 'bg-orange-100 dark:bg-orange-900/30', text: 'text-orange-600 dark:text-orange-400', icon: AlertTriangle },
};

const PROVIDER_LABELS: Record<string, string> = {
  M_PESA: 'M-Pesa',
  MTN_MOMO: 'MTN MoMo',
  ORANGE_MONEY: 'Orange Money',
  GENERIC: 'Générique',
};

export default function WebhookLogPage() {
  const { t } = useI18n();
  const [provider, setProvider] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['webhooks', 'logs', provider, status, page],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (provider) params.set('provider', provider);
      if (status) params.set('status', status);
      return (await api.get(`/payments/webhooks/logs?${params}`)).data;
    },
  });

  const { data: stats } = useQuery({
    queryKey: ['webhooks', 'logs', 'stats'],
    queryFn: async () => (await api.get<LogStats>('/payments/webhooks/logs/stats')).data,
  });

  const logs: WebhookLogEntry[] = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  const fmt = (n: number | null) => n != null ? `${n}ms` : '—';

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white shadow-lg">
          <Activity className="w-6 h-6" />
        </div>
        <div className="flex-1">
          <h1 className="page-title">Webhook Logs</h1>
          <p className="page-subtitle">Historique des callbacks opérateurs reçus</p>
        </div>
        <button onClick={() => refetch()} className="btn-secondary flex items-center gap-2">
          <RefreshCw className="w-4 h-4" /> Actualiser
        </button>
      </div>

      {/* KPI Cards */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-3 mb-6 animate-slide-up">
          {[
            { label: 'Total', value: stats.total, icon: Activity, color: 'text-gray-600 dark:text-gray-400' },
            { label: 'Reçus', value: stats.received, icon: Globe, color: 'text-blue-600 dark:text-blue-400' },
            { label: 'Vérifiés', value: stats.verified, icon: Shield, color: 'text-green-600 dark:text-green-400' },
            { label: 'Traités', value: stats.processed, icon: CheckCircle2, color: 'text-emerald-600 dark:text-emerald-400' },
            { label: 'Rejetés', value: stats.rejected + stats.errors, icon: XCircle, color: 'text-red-600 dark:text-red-400' },
          ].map((kpi) => (
            <div key={kpi.label} className="glass-card p-3 text-center">
              <kpi.icon className={`w-5 h-5 mx-auto mb-1 ${kpi.color}`} />
              <p className={`text-xl font-bold ${kpi.color}`}>{kpi.value}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400">{kpi.label}</p>
            </div>
          ))}
        </div>
      )}

      {/* Filters */}
      <div className="glass-card p-4 mb-4 flex flex-wrap gap-3 items-center animate-slide-up">
        <Filter className="w-4 h-4 text-gray-400" />
        <select className="input text-sm" value={provider} onChange={(e) => { setProvider(e.target.value); setPage(0); }}>
          <option value="">Tous les opérateurs</option>
          {PROVIDERS.filter(Boolean).map((p) => (
            <option key={p} value={p}>{PROVIDER_LABELS[p] ?? p}</option>
          ))}
        </select>
        <select className="input text-sm" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          <option value="">Tous les statuts</option>
          {STATUSES.filter(Boolean).map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        <span className="text-xs text-gray-400 ml-auto">{data?.totalElements ?? 0} résultats</span>
      </div>

      {/* Log Entries */}
      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : logs.length === 0 ? (
        <div className="glass-card p-12 text-center text-gray-400">
          <Activity className="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p>Aucun log webhook trouvé</p>
        </div>
      ) : (
        <div className="space-y-2 mb-6">
          {logs.map((entry) => {
            const sc = STATUS_COLORS[entry.statusLabel] ?? STATUS_COLORS.RECEIVED;
            const Icon = sc.icon;
            const isExpanded = expandedId === entry.id;
            return (
              <div key={entry.id} className="glass-card overflow-hidden animate-slide-up">
                <button
                  onClick={() => setExpandedId(isExpanded ? null : entry.id)}
                  className="w-full px-4 py-3 flex items-center gap-3 text-left hover:bg-gray-50 dark:hover:bg-gray-800/30 transition-colors"
                >
                  <div className={`p-1.5 rounded-lg ${sc.bg}`}>
                    <Icon className={`w-4 h-4 ${sc.text}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                        {PROVIDER_LABELS[entry.provider] ?? entry.provider}
                      </span>
                      <span className="text-xs text-gray-400">{entry.endpoint}</span>
                    </div>
                    <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-gray-400">
                      <span>{new Date(entry.createdAt).toLocaleString('fr-FR')}</span>
                      {entry.sourceIp && <span>• {entry.sourceIp}</span>}
                      {entry.reference && <span>• {entry.reference}</span>}
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    {entry.signatureValid !== null && (
                      <span className={`flex items-center gap-1 text-xs ${entry.signatureValid ? 'text-green-500' : 'text-red-500'}`}>
                        {entry.signatureValid ? <Shield className="w-3 h-3" /> : <ShieldAlert className="w-3 h-3" />}
                        {entry.signatureValid ? '✓' : '✗'}
                      </span>
                    )}
                    <span className="text-xs text-gray-400">{fmt(entry.durationMs)}</span>
                    {entry.statusCode && (
                      <span className={`text-xs font-mono ${entry.statusCode < 300 ? 'text-green-500' : 'text-red-500'}`}>
                        {entry.statusCode}
                      </span>
                    )}
                  </div>
                </button>

                {isExpanded && (
                  <div className="px-4 pb-4 border-t border-gray-100 dark:border-gray-800 pt-3 space-y-3">
                    {entry.errorMessage && (
                      <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 text-sm text-red-700 dark:text-red-400">
                        <AlertTriangle className="w-4 h-4 inline mr-1" />
                        {entry.errorMessage}
                      </div>
                    )}
                    {entry.requestBody && (
                      <div>
                        <p className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">Request Body</p>
                        <pre className="text-xs bg-gray-50 dark:bg-gray-800/50 p-3 rounded-lg overflow-x-auto max-h-40 text-gray-700 dark:text-gray-300">
                          {(() => { try { return JSON.stringify(JSON.parse(entry.requestBody), null, 2); } catch { return entry.requestBody; } })()}
                        </pre>
                      </div>
                    )}
                    {entry.responseBody && (
                      <div>
                        <p className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-1">Response</p>
                        <pre className="text-xs bg-gray-50 dark:bg-gray-800/50 p-3 rounded-lg overflow-x-auto max-h-24 text-gray-700 dark:text-gray-300">
                          {(() => { try { return JSON.stringify(JSON.parse(entry.responseBody), null, 2); } catch { return entry.responseBody; } })()}
                        </pre>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-4">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="btn-secondary btn-sm"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
          <span className="text-sm text-gray-500">
            Page {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="btn-secondary btn-sm"
          >
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      )}
    </div>
  );
}
