import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';
import { Brain, TrendingUp, TrendingDown, AlertTriangle, Sparkles, BarChart3, RefreshCw, Shield } from 'lucide-react';
import toast from 'react-hot-toast';

interface Prediction {
  id: string | number;
  predictionType: string;
  metricName: string;
  currentValue: number;
  predictedValue: number;
  confidenceScore: number;
  explanation: string;
  riskLevel: string;
  createdAt?: string;
}

const TYPE_MAP: Record<string, string> = {
  GROWTH_FORECAST: 'growth', CHURN_RISK: 'churn', ATTENDANCE_TREND: 'attendance',
  GIVING_TREND: 'giving', ENGAGEMENT_SCORE: 'engagement', DEPARTMENT_PERFORMANCE: 'engagement',
};
const RISK_MAP: Record<string, string> = { LOW: 'low', MEDIUM: 'medium', HIGH: 'high', CRITICAL: 'critical' };

export default function AiPredictionsPage() {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const tenantId = Number(localStorage.getItem('tenantId') || localStorage.getItem('orgId') || 0);
  const [filter, setFilter] = useState<'all' | 'high' | 'growth'>('all');

  const { data: rawPredictions = [], isLoading, error } = useQuery({
    queryKey: ['ai-predictions', tenantId],
    queryFn: async () => (await api.get('/ai-predictions', { params: { tenantId } })).data as Prediction[],
    enabled: tenantId > 0,
    retry: false,
  });

  const predictions = rawPredictions.map((p) => ({
    ...p,
    id: String(p.id),
    type: TYPE_MAP[p.predictionType] || 'engagement',
    metric: p.metricName,
    predicted: p.predictedValue,
    confidence: p.confidenceScore,
    risk: RISK_MAP[p.riskLevel] || 'low',
  }));

  const generateMutation = useMutation({
    mutationFn: async () => (await api.post(`/ai-predictions/generate?tenantId=${tenantId}`)).data,
    onSuccess: () => { toast.success('Prédictions régénérées'); queryClient.invalidateQueries({ queryKey: ['ai-predictions'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const filtered = filter === 'all' ? predictions
    : filter === 'high' ? predictions.filter(p => p.risk === 'high' || p.risk === 'critical')
    : predictions.filter(p => p.type === 'growth' || p.type === 'attendance');

  const riskColor = (r: string) => {
    if (r === 'critical') return 'text-red-600 bg-red-100 dark:bg-red-900/30 dark:text-red-300';
    if (r === 'high') return 'text-orange-600 bg-orange-100 dark:bg-orange-900/30 dark:text-orange-300';
    if (r === 'medium') return 'text-amber-600 bg-amber-100 dark:bg-amber-900/30 dark:text-amber-300';
    return 'text-green-600 bg-green-100 dark:bg-green-900/30 dark:text-green-300';
  };

  const typeIcon = (type: string) => {
    if (type === 'growth') return <TrendingUp className="w-5 h-5 text-green-500" />;
    if (type === 'churn') return <AlertTriangle className="w-5 h-5 text-red-500" />;
    if (type === 'attendance') return <BarChart3 className="w-5 h-5 text-blue-500" />;
    if (type === 'giving') return <TrendingUp className="w-5 h-5 text-emerald-500" />;
    return <Sparkles className="w-5 h-5 text-purple-500" />;
  };

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="text-red-500 p-6">{getErrorMessage(error)}</div>;
  if (!predictions || predictions.length === 0) return <EmptyState title="Aucune prédiction disponible" message="Générez des prédictions IA pour analyser la croissance de votre église" />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <Brain className="w-7 h-7 text-violet-500" />
            {t('aiPredictions.title') ?? 'Prédictions & Analyse IA'}
          </h1>
          <p className="text-sm text-gray-500 mt-1">Modèles prédictifs pour la croissance et la gestion pastorale</p>
        </div>
        <button onClick={() => generateMutation.mutate()}
          disabled={generateMutation.isPending}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-violet-500 text-white font-medium text-sm hover:bg-violet-600 transition disabled:opacity-50">
          <RefreshCw className={`w-4 h-4 ${generateMutation.isPending ? 'animate-spin' : ''}`} />
          {generateMutation.isPending ? 'Analyse en cours...' : 'Régénérer les prédictions'}
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center"><TrendingUp className="w-5 h-5 text-green-500" /></div>
            <div><p className="text-2xl font-bold text-green-500">+{predictions.filter(p => p.type === 'growth').length}</p><p className="text-xs text-gray-500">Croissance prévue</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center"><AlertTriangle className="w-5 h-5 text-red-500" /></div>
            <div><p className="text-2xl font-bold text-red-500">{predictions.filter(p => p.risk === 'high' || p.risk === 'critical').length}</p><p className="text-xs text-gray-500">À risque</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center"><BarChart3 className="w-5 h-5 text-blue-500" /></div>
            <div><p className="text-2xl font-bold text-blue-500">{predictions.filter(p => p.type === 'attendance').length}</p><p className="text-xs text-gray-500">Présence</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center"><Sparkles className="w-5 h-5 text-violet-500" /></div>
            <div><p className="text-2xl font-bold text-violet-500">{predictions.length > 0 ? Math.round(predictions.reduce((a, p) => a + p.confidence, 0) / predictions.length * 100) : 0}%</p><p className="text-xs text-gray-500">Confiance moyenne</p></div>
          </div>
        </div>
      </div>

      <div className="flex gap-2">
        <button onClick={() => setFilter('all')} className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === 'all' ? 'bg-violet-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>Toutes</button>
        <button onClick={() => setFilter('high')} className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === 'high' ? 'bg-red-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>Risques élevés</button>
        <button onClick={() => setFilter('growth')} className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === 'growth' ? 'bg-green-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>Croissance</button>
      </div>

      <div className="space-y-3">
        {filtered.map((pred) => (
          <div key={pred.id} className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
            <div className="flex items-start justify-between">
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-xl bg-gray-50 dark:bg-white/5 flex items-center justify-center">{typeIcon(pred.type)}</div>
                <div>
                  <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{pred.metric}</h3>
                  <div className="flex items-center gap-3 mt-1">
                    <span className="text-xs text-gray-400">Actuel: <span className="text-gray-600 dark:text-gray-300 font-medium">{pred.currentValue?.toLocaleString()}</span></span>
                    <span className="text-xs text-gray-400">&rarr;</span>
                    <span className="text-xs text-gray-400">Prévu: <span className="text-gray-900 dark:text-white font-bold">{pred.predicted?.toLocaleString(undefined, { maximumFractionDigits: 1 })}</span></span>
                    <span className={`px-2 py-0.5 rounded-lg text-[10px] font-bold uppercase ${riskColor(pred.risk)}`}>{pred.risk}</span>
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="flex items-center gap-1">
                  <Shield className="w-3 h-3 text-gray-400" />
                  <span className="text-xs font-bold text-gray-500">{Math.round(pred.confidence * 100)}%</span>
                </div>
                <p className="text-[10px] text-gray-400 mt-0.5">confiance</p>
              </div>
            </div>
            {pred.explanation && (
              <div className="mt-3 p-3 rounded-xl bg-gray-50 dark:bg-white/[0.02] border border-gray-100 dark:border-white/[0.03]">
                <p className="text-xs text-gray-500 leading-relaxed">{pred.explanation}</p>
              </div>
            )}
            <div className="mt-2 h-1.5 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-violet-400 to-violet-600 rounded-full" style={{ width: `${pred.confidence * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
