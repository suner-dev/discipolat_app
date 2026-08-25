import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Brain, TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface Prediction { id: string; type: string; currentValue: number; predictedValue: number; growthRate: number; trend: string; confidence: string; narrative: string; periodMonths: number; }

export default function PredictionsMLPage() {
  const { t } = useI18n();
  const MOCK: Prediction[] = [
    { id: '1', type: 'EFFECTIFS', currentValue: 245, predictedValue: 278, growthRate: 13.5, trend: 'UP', confidence: 'HIGH', narrative: 'Forte croissance prévue pour les effectifs : +13.5% sur 6 mois.', periodMonths: 6 },
    { id: '2', type: 'PRÉSENCES', currentValue: 180, predictedValue: 168, growthRate: -6.7, trend: 'DOWN', confidence: 'MEDIUM', narrative: 'Légère baisse prévue pour les présences : -6.7% sur 6 mois.', periodMonths: 6 },
    { id: '3', type: 'BAPTÊMES', currentValue: 12, predictedValue: 18, growthRate: 50.0, trend: 'UP', confidence: 'MEDIUM', narrative: 'Forte croissance prévue pour les baptêmes : +50.0% sur 6 mois.', periodMonths: 6 },
    { id: '4', type: 'DÉCROCHAGES', currentValue: 5, predictedValue: 8, growthRate: 60.0, trend: 'UP', confidence: 'LOW', narrative: 'Baisse significative prévue pour les décrochages : intervention recommandée.', periodMonths: 6 },
    { id: '5', type: 'FINANCES', currentValue: 4250, predictedValue: 4680, growthRate: 10.1, trend: 'UP', confidence: 'HIGH', narrative: 'Croissance modérée prévue pour les finances : +10.1% sur 6 mois.', periodMonths: 6 },
  ];
  const trendIcon = (t: string) => t === 'UP' ? <TrendingUp className="w-5 h-5 text-green-400" /> : t === 'DOWN' ? <TrendingDown className="w-5 h-5 text-red-400" /> : <Minus className="w-5 h-5 text-gray-400" />;
  const confColor = (c: string) => c === 'HIGH' ? 'text-green-400 bg-green-500/20' : c === 'MEDIUM' ? 'text-yellow-400 bg-yellow-500/20' : 'text-red-400 bg-red-500/20';

  return (
    <div className="space-y-6 p-6">
      <h1 className="text-2xl font-bold text-white flex items-center gap-2"><Brain className="text-purple-400" /> {t('predictions.title') || 'Prédictions ML'}</h1>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {MOCK.map(p => (
          <div key={p.id} className="bg-white/5 backdrop-blur rounded-2xl p-5 border border-white/10 hover:border-purple-500/30 transition">
            <div className="flex items-center justify-between mb-3">
              <span className="text-white font-semibold">{p.type}</span>
              {trendIcon(p.trend)}
            </div>
            <div className="grid grid-cols-2 gap-4 mb-3">
              <div><div className="text-xs text-gray-400">Actuel</div><div className="text-xl font-bold text-white">{p.currentValue.toLocaleString()}</div></div>
              <div><div className="text-xs text-gray-400">Prédit ({p.periodMonths}mo)</div><div className="text-xl font-bold text-white">{p.predictedValue.toLocaleString()}</div></div>
            </div>
            <div className="flex items-center gap-2 mb-3">
              <span className={`text-sm font-bold ${p.growthRate > 0 ? 'text-green-400' : 'text-red-400'}`}>{p.growthRate > 0 ? '+' : ''}{p.growthRate}%</span>
              <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${confColor(p.confidence)}`}>Confiance {p.confidence}</span>
            </div>
            <p className="text-xs text-gray-400 leading-relaxed">{p.narrative}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
