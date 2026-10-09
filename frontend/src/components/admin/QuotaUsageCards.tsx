import { useId } from 'react';
import { useQuery } from '@tanstack/react-query';
import { AlertCircle, RefreshCw } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import { normalizeQuotaUsage } from '@/types/quota';
import type { QuotaUsageMetric } from '@/types/quota';
import { getI18nLocale, useI18n } from '@/i18n';
import { useTenantOptional } from '@/contexts/TenantContext';

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

/**
 * Le nombre est formaté dans la locale ACTIVE.
 *
 * Avant, ce module appelait `Intl.NumberFormat('fr-FR')` en dur : un utilisateur
 * `ar` ou `sw` lisait « 1 234 » pour un montant en chiffres occidentaux, et le
 * séparateur de milliers ne suivait pas la locale. La locale se lit au moment du
 * rendu (comme partout ailleurs dans l'application) : c'est le seul moment où
 * elle est garantie à jour.
 */
const formatNumber = (value: number): string =>
  new Intl.NumberFormat(getI18nLocale()).format(value);

export function QuotaUsageCards({ metrics, title, loading = false, error = null, onRetry }: QuotaUsageCardsProps) {
  const titleId = useId();
  const { t } = useI18n();
  const resolvedTitle = title ?? t('quotas.title');

  return (
    <section className="space-y-4" aria-labelledby={titleId}>
      <div className="flex items-center justify-between gap-4">
        <h2 id={titleId} className="text-lg font-semibold">{resolvedTitle}</h2>
        {onRetry && !loading && (
          <button type="button" onClick={onRetry} className="inline-flex items-center gap-2 text-sm text-indigo-600 hover:text-indigo-800">
            <RefreshCw className="h-4 w-4" /> {t('quotas.retry')}
          </button>
        )}
      </div>
      {loading && <p role="status" className="text-sm text-gray-500">{t('quotas.loading')}</p>}
      {error && (
        <div role="alert" className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}
      {!loading && !error && metrics.length === 0 && <p className="text-sm text-gray-500">{t('quotas.empty')}</p>}
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
  const { t } = useI18n();

  const formatMetricValue = (value: number): string => {
    if (metric.unit?.toUpperCase() === 'BYTES') {
      return t('quotas.megabytes', { value: formatNumber(value / (1024 * 1024)) });
    }
    return formatNumber(value);
  };

  const usage = metric.available && metric.usage !== null
    ? formatMetricValue(metric.usage)
    : t('quotas.unavailable');

  const limit = metric.unlimited
    ? t('quotas.unlimited')
    : metric.limit === null
      ? t('quotas.unavailable')
      : formatMetricValue(metric.limit);

  const percent = metric.percent === null
    ? t('quotas.unavailable')
    : t('quotas.percentLabel', { value: `${formatNumber(metric.percent)} %` });

  const enforcement = metric.enforced === true
    ? t('quotas.enforced')
    : metric.enforced === false
      ? t('quotas.notEnforced')
      : t('quotas.enforcementUnknown');

  return (
    <article className={`rounded-lg border bg-white p-5 shadow-sm ${metric.enforced === false ? 'border-gray-200 opacity-75' : 'border-indigo-100'}`}>
      <p className="text-sm font-medium text-gray-600">{metric.label}</p>
      <p className="mt-2 text-2xl font-bold text-gray-900">{usage}</p>
      <p className="mt-1 text-sm text-gray-500">{t('quotas.limitLabel', { value: limit })}</p>
      <p className="text-sm text-gray-500">{percent}</p>
      {metric.source && <p className="mt-2 text-xs text-gray-400">{t('quotas.sourceLabel', { value: metric.source })}</p>}
      <p className="mt-1 text-xs text-gray-400">{enforcement}</p>
    </article>
  );
}

export function QuotaUsagePanel({ enabled = true, title }: QuotaUsagePanelProps) {
  // Variante tolérante : ce composant est aussi rendu dans des tests et des
  // contexts partiels sans TenantProvider. Lever ici ferait planter un rendu
  // quiDisplayed déjà correctement avant.
  const tenant = useTenantOptional();
  const tenantKey = tenant?.currentTenant?.id ?? 'no-tenant';

  const query = useQuery({
    // Clé préfixée par le tenant : sans cela, basculer d'église réutilisait le
    // quota de l'église précédente pendant le `staleTime` (fuite inter-églises).
    queryKey: ['t', tenantKey, 'admin', 'quotas', 'usage'] as const,
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