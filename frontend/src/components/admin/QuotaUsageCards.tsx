import { useQuery } from '@tanstack/react-query';
import { AlertCircle, RefreshCw } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { normalizeQuotaUsage } from '@/types/quota';
import type { QuotaUsageMetric } from '@/types/quota';

interface QuotaUsageCardsProps {
  metrics: QuotaUsageMetric[];
  title?: string;
  loading?: boolean;
  error?: string | null;
  onRetry?: () => void;
}

interface QuotaUsagePanelProps {
  enabled?: boolean;
  title?: string;
}

const formatNumber = (value: number): string => new Intl.NumberFormat('fr-FR').format(value);

const formatMetricValue = (metric: QuotaUsageMetric, value: number): string => {
  if (metric.unit?.toUpperCase() === 'BYTES') {
    return `${formatNumber(value / (1024 * 1024))} Mo`;
  }
  return formatNumber(value);
};

const formatMetricUsage = (metric: QuotaUsageMetric): string => (
  metric.available && metric.usage !== null ? formatMetricValue(metric, metric.usage) : 'Indisponible'
);

const formatLimit = (metric: QuotaUsageMetric): string => {
  if (metric.unlimited) return 'Illimité';
  return metric.limit === null ? 'Indisponible' : formatMetricValue(metric, metric.limit);
};

export function QuotaUsageCards({ metrics, title = 'Quotas et usage', loading = false, error = null, onRetry }: QuotaUsageCardsProps) {
  return (
    <section className="space-y-4" aria-labelledby="quota-usage-title">
      <div className="flex items-center justify-between gap-4">
        <h2 id="quota-usage-title" className="text-lg font-semibold">{title}</h2>
        {onRetry && !loading && (
          <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 text-sm text-indigo-600 hover:text-indigo-800">
            <RefreshCw className="h-4 w-4" /> Réessayer
          </button>
        )}
      </div>
      {loading && <p role="status" className="text-sm text-gray-500">Chargement des quotas…</p>}
      {error && (
        <div role="alert" className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}
      {!loading && !error && metrics.length === 0 && <p className="text-sm text-gray-500">Aucune donnée de quota disponible.</p>}
      {!loading && !error && metrics.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {metrics.map((metric) => (
            <QuotaMetricCard key={metric.key} metric={metric} />
          ))}
        </div>
      )}
    </section>
  );
}

function QuotaMetricCard({ metric }: { metric: QuotaUsageMetric }) {
  const percent = metric.percent === null ? 'Indisponible' : `${formatNumber(metric.percent)} %`;
  const enforcement = metric.enforced === true
    ? 'Limite appliquée'
    : metric.enforced === false
      ? 'Limite non appliquée'
      : 'Application de la limite indisponible';

  return (
    <article className={`rounded-lg border bg-white p-5 shadow-sm ${metric.enforced === false ? 'border-gray-200 opacity-75' : 'border-indigo-100'}`}>
      <p className="text-sm font-medium text-gray-600">{metric.label}</p>
      <p className="mt-2 text-2xl font-bold text-gray-900">{formatMetricUsage(metric)}</p>
      <p className="mt-1 text-sm text-gray-500">Limite : {formatLimit(metric)}</p>
      <p className="text-sm text-gray-500">Pourcentage : {percent}</p>
      {metric.source && <p className="mt-2 text-xs text-gray-400">Source : {metric.source}</p>}
      <p className="mt-1 text-xs text-gray-400">{enforcement}</p>
    </article>
  );
}

export function QuotaUsagePanel({ enabled = true, title }: QuotaUsagePanelProps) {
  const query = useQuery({
    queryKey: ['admin', 'quotas', 'usage'],
    queryFn: async () => {
      const response = await api.get('/admin/quotas/usage');
      return normalizeQuotaUsage(response.data);
    },
    enabled,
    retry: false,
  });

  if (!enabled) return null;

  return (
    <QuotaUsageCards
      metrics={query.data?.metrics ?? []}
      title={title}
      loading={query.isLoading}
      error={query.isError ? getErrorMessage(query.error) : null}
      onRetry={() => { void query.refetch(); }}
    />
  );
}
