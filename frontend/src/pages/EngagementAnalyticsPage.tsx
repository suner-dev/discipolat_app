import { useQuery } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import { BarChart3, TrendingUp, TrendingDown } from 'lucide-react';

interface Metric { name: string; category: string; value: number; change: number; unit: string; }

export default function EngagementAnalyticsPage() {
  const { t } = useI18n();
  const { data: metrics = [], isLoading, error } = useQuery({
    queryKey: ['engagement-analytics'],
    queryFn: async () => {
      const res = await api.get('/engagement-analytics');
      // EngagementAnalytics { metricName, metricCategory, metricValue, previousValue, changePercentage, recordedAt }
      return (res.data as Array<Record<string, unknown>>).map((m) => ({
        name: String(m.metricName ?? ''),
        category: String(m.metricCategory ?? 'general'),
        value: Number(m.metricValue ?? 0),
        change: Number(m.changePercentage ?? 0),
        unit: '',
      })) as Metric[];
    },
    retry: false,
  });

  if (isLoading) return <SkeletonLoader lines={4} variant="card" />;
  if (error) return <div className="text-red-500 p-6">{getErrorMessage(error)}</div>;
  if (!metrics || metrics.length === 0) return <EmptyState title="Aucune donnée" message="Aucune métrique d'engagement disponible." />;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><BarChart3 className="text-indigo-400" /> {t('engagement.title') || 'Analytics d\'engagement'}</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {metrics.map((m, i) => (
          <div key={i} className="bg-white/5 backdrop-blur rounded-2xl p-4 border border-white/10 hover:border-indigo-500/30 transition">
            <div className="text-xs text-gray-400 uppercase tracking-wide mb-1">{m.category}</div>
            <div className="text-2xl font-bold text-white">{m.value.toLocaleString()}<span className="text-sm font-normal text-gray-400 ml-1">{m.unit}</span></div>
            <div className="text-sm text-gray-300 mb-2">{m.name}</div>
            <div className={`text-xs flex items-center gap-1 ${m.change > 0 ? 'text-green-400' : 'text-red-400'}`}>
              {m.change > 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
              {m.change > 0 ? '+' : ''}{m.change}%
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
