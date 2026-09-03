import { useQuery } from '@tanstack/react-query';
import { useI18n } from '@/i18n';
import { Brain, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import api from '@/lib/api';
import { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface Prediction {
  id: string;
  predictionType: string;
  currentValue?: number;
  predictedValue?: number;
  growthRate?: number;
  trend: string;
  confidence?: string;
  narrative?: string;
  periodMonths?: number;
}

const TYPE_LABEL: Record<string, string> = {
  WORKFORCE_GROWTH: 'EFFECTIFS',
  ATTENDANCE: 'PRÉSENCES',
  BAPTISMS: 'BAPTÊMES',
  DROPOUT: 'DÉCROCHAGES',
  ENGAGEMENT: 'ENGAGEMENT',
  FINANCES: 'FINANCES',
};

const CONFIDENCE_LABEL: Record<string, string> = {
  HIGH: 'Élevée', MEDIUM: 'Moyenne', LOW: 'Faible',
};

export default function PredictionsMLPage() {
  const { t } = useI18n();

  const { data: predictions = [], isLoading, error } = useQuery({
    queryKey: ['ml-predictions'],
    queryFn: async () => (await api.get('/predictions')).data as Prediction[],
    retry: false,
  });

  const trendIcon = (tr: string) =>
    tr === 'UP' ? <TrendingUp className="w-5 h-5 text-green-400" />
    : tr === 'DOWN' ? <TrendingDown className="w-5 h-5 text-red-400" />
    : <Minus className="w-5 h-5 text-gray-400" />;
  const confColor = (c: string) =>
    c === 'HIGH' ? 'text-green-400 bg-green-500/20' : c === 'MEDIUM' ? 'text-yellow-400 bg-yellow-500/20' : 'text-red-400 bg-red-500/20';

  if (isLoading) return <SkeletonLoader lines={5} variant="card" />;
  if (error) return <div className="p-6 text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Brain className="text-purple-400" /> {t('predictions.title') || 'Prédictions ML'}</h1>
      {predictions.length === 0 ? (
        <EmptyState
          icon={<Brain className="w-6 h-6 text-purple-400" />}
          title="Aucune prédiction"
          message="Générez des prédictions ML pour anticiper la croissance de l'église."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {predictions.map(p => {
            const growth = p.growthRate ?? 0;
            return (
              <div key={p.id} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10 hover:border-purple-500/30 transition">
                <div className="flex items-center justify-between mb-3">
                  <span className="text-white font-semibold">{TYPE_LABEL[p.predictionType] || p.predictionType}</span>
                  {trendIcon(p.trend)}
                </div>
                <div className="grid grid-cols-2 gap-4 mb-3">
                  <div><div className="text-xs text-gray-400">Actuel</div><div className="text-xl font-bold text-white">{(p.currentValue ?? 0).toLocaleString()}</div></div>
                  <div><div className="text-xs text-gray-400">Prédit ({p.periodMonths ?? 6}mo)</div><div className="text-xl font-bold text-white">{(p.predictedValue ?? 0).toLocaleString()}</div></div>
                </div>
                <div className="flex items-center gap-2 mb-3">
                  <span className={`text-sm font-bold ${growth > 0 ? 'text-green-400' : 'text-red-400'}`}>{growth > 0 ? '+' : ''}{growth}%</span>
                  {p.confidence && (
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${confColor(p.confidence)}`}>Confiance {CONFIDENCE_LABEL[p.confidence] || p.confidence}</span>
                  )}
                </div>
                {p.narrative && <p className="text-xs text-gray-400 leading-relaxed">{p.narrative}</p>}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
