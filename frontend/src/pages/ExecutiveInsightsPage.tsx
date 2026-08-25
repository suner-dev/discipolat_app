import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import { Sparkles, AlertTriangle, Lightbulb, Eye, X, RefreshCw } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface Insight {
  id: string;
  title: string;
  description: string;
  severity: string;
  category: string;
  recommendedAction: string;
  metricValue: string;
  metricChange: string;
  isRead: boolean;
  createdAt?: string;
}

export default function ExecutiveInsightsPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState('all');

  const { data: insights = [], isLoading, error, refetch } = useQuery({
    queryKey: ['executive-insights'],
    queryFn: async () => (await api.get('/executive-insights')).data as Insight[],
    retry: false,
  });

  const generateMutation = useMutation({
    mutationFn: async () => (await api.post('/executive-insights/generate')).data,
    onSuccess: () => { toast.success('Insights régénérés'); queryClient.invalidateQueries({ queryKey: ['executive-insights'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const markReadMutation = useMutation({
    mutationFn: async (id: string) => (await api.post(`/executive-insights/${id}/read`)).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['executive-insights'] }),
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const dismissMutation = useMutation({
    mutationFn: async (id: string) => (await api.post(`/executive-insights/${id}/dismiss`)).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['executive-insights'] }),
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const filtered = filter === 'all' ? insights : insights.filter(i => i.severity === filter);

  const severityIcon = (s: string) =>
    s === 'CRITICAL' ? <AlertTriangle className="w-5 h-5 text-red-400" />
    : s === 'WARNING' ? <AlertTriangle className="w-5 h-5 text-orange-400" />
    : s === 'OPPORTUNITY' ? <Lightbulb className="w-5 h-5 text-blue-400" />
    : <Sparkles className="w-5 h-5 text-gray-400" />;
  const severityBorder = (s: string) =>
    s === 'CRITICAL' ? 'border-red-500/40' : s === 'WARNING' ? 'border-orange-500/30' : s === 'OPPORTUNITY' ? 'border-blue-500/30' : 'border-white/10';

  if (isLoading) return <SkeletonLoader lines={5} variant="card" />;
  if (error) return <div className="p-6 text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Sparkles className="text-violet-400" /> {t('executiveInsights.title') || 'Insights exécutifs IA'}
        </h1>
        <div className="flex items-center gap-2">
          <button onClick={() => refetch()} className="px-3 py-2 rounded-xl bg-white/5 text-gray-400 text-sm hover:bg-white/10 transition flex items-center gap-2">
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
          <button onClick={() => generateMutation.mutate()}
            disabled={generateMutation.isPending}
            className="px-3 py-2 rounded-xl bg-violet-500 text-white text-sm font-medium hover:bg-violet-600 transition disabled:opacity-50">
            Régénérer
          </button>
        </div>
      </div>

      <div className="flex gap-2">
        {(['all', 'CRITICAL', 'WARNING', 'OPPORTUNITY', 'INFO'] as const).map((f) => (
          <button key={f} onClick={() => setFilter(f)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${filter === f ? 'bg-violet-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>
            {f === 'all' ? 'Tous' : f}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Sparkles className="w-6 h-6 text-violet-400" />}
          title="Aucun insight disponible"
          message="Générez des insights exécutifs IA pour analyser la santé de l'église."
        />
      ) : (
        <div className="space-y-4">
          {filtered.map(insight => (
            <div key={insight.id} className={`bg-white/5 backdrop-blur rounded-2xl p-5 border transition hover:scale-[1.01] ${severityBorder(insight.severity)}`}>
              <div className="flex items-start gap-3">
                {severityIcon(insight.severity)}
                <div className="flex-1">
                  <div className="flex items-center justify-between mb-1">
                    <h3 className={`font-semibold ${insight.isRead ? 'text-gray-300' : 'text-white'}`}>{insight.title}</h3>
                    <div className="flex items-center gap-3">
                      {insight.metricValue && <span className="text-lg font-bold text-white">{insight.metricValue}</span>}
                      {insight.metricChange && (
                        <span className={`text-sm font-medium ${insight.metricChange.startsWith('+') ? 'text-green-400' : 'text-red-400'}`}>{insight.metricChange}</span>
                      )}
                    </div>
                  </div>
                  <p className="text-sm text-gray-300 mb-2">{insight.description}</p>
                  {insight.recommendedAction && (
                    <div className="bg-blue-500/10 rounded-lg p-3 flex items-start gap-2">
                      <Lightbulb className="w-4 h-4 text-blue-400 mt-0.5" />
                      <p className="text-sm text-blue-300">{insight.recommendedAction}</p>
                    </div>
                  )}
                  <div className="flex items-center gap-2 mt-3">
                    <button onClick={() => markReadMutation.mutate(insight.id)}
                      className="flex items-center gap-1 px-2 py-1 rounded-lg bg-white/5 text-gray-400 text-xs hover:bg-white/10 transition">
                      <Eye className="w-3 h-3" /> Marquer comme lu
                    </button>
                    <button onClick={() => dismissMutation.mutate(insight.id)}
                      className="flex items-center gap-1 px-2 py-1 rounded-lg bg-white/5 text-gray-400 text-xs hover:bg-white/10 transition">
                      <X className="w-3 h-3" /> Ignorer
                    </button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
