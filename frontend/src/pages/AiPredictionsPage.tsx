import { useState } from 'react';
import { useI18n } from '@/i18n';
import { Brain, TrendingUp, TrendingDown, AlertTriangle, Sparkles, BarChart3, RefreshCw, Shield } from 'lucide-react';

interface Prediction {
  id: string;
  type: 'growth' | 'churn' | 'attendance' | 'giving' | 'engagement';
  metric: string;
  current: number;
  predicted: number;
  confidence: number;
  explanation: string;
  risk: 'low' | 'medium' | 'high' | 'critical';
}

const MOCK_PREDICTIONS: Prediction[] = [
  { id: '1', type: 'growth', metric: 'Nouvelles conversions/mois', current: 15, predicted: 18.3, confidence: 0.82, explanation: "Tendance à la hausse sur 3 mois. Le pipeline d\'évangélisation montre +20% de nouveaux contacts.", risk: 'low' },
  { id: '2', type: 'churn', metric: 'Membres à risque', current: 8, predicted: 12.1, confidence: 0.71, explanation: '8 membres montrent un schéma de fréquentation décroissante. Suivi personnel recommandé dans les 2 semaines.', risk: 'high' },
  { id: '3', type: 'attendance', metric: 'Taux de fréquentation', current: 72, predicted: 75.4, confidence: 0.88, explanation: 'Le taux de présence est en progression. Les cultes du dimanche montrent une croissance constante.', risk: 'low' },
  { id: '4', type: 'giving', metric: 'Dons mensuels (FCFA)', current: 450000, predicted: 483200, confidence: 0.76, explanation: 'Tendance positive des dons. Les dîmes stables, les offrandes en hausse grâce à Mobile Money.', risk: 'low' },
  { id: '5', type: 'engagement', metric: 'Score d\'engagement global', current: 68, predicted: 71.8, confidence: 0.74, explanation: 'L\'engagement s\'améliore avec les nouvelles fonctionnalités (quêtes, badges, fil communautaire).', risk: 'medium' },
];

export default function AiPredictionsPage() {
  const { t } = useI18n();
  const [filter, setFilter] = useState<'all' | 'high' | 'growth'>('all');
  const [generating, setGenerating] = useState(false);

  const filtered = filter === 'all' ? MOCK_PREDICTIONS
    : filter === 'high' ? MOCK_PREDICTIONS.filter(p => p.risk === 'high' || p.risk === 'critical')
    : MOCK_PREDICTIONS.filter(p => p.type === 'growth' || p.type === 'attendance');

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

  const handleGenerate = () => {
    setGenerating(true);
    setTimeout(() => setGenerating(false), 2000);
  };

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
        <button onClick={handleGenerate}
          disabled={generating}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-violet-500 text-white font-medium text-sm hover:bg-violet-600 transition disabled:opacity-50">
          <RefreshCw className={`w-4 h-4 ${generating ? 'animate-spin' : ''}`} />
          {generating ? 'Analyse en cours...' : 'Régénérer les prédictions'}
        </button>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-green-100 dark:bg-green-900/30 flex items-center justify-center"><TrendingUp className="w-5 h-5 text-green-500" /></div>
            <div><p className="text-2xl font-bold text-green-500">+22%</p><p className="text-xs text-gray-500">Croissance prévue</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-red-100 dark:bg-red-900/30 flex items-center justify-center"><AlertTriangle className="w-5 h-5 text-red-500" /></div>
            <div><p className="text-2xl font-bold text-red-500">12</p><p className="text-xs text-gray-500">À risque</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center"><BarChart3 className="w-5 h-5 text-blue-500" /></div>
            <div><p className="text-2xl font-bold text-blue-500">75.4%</p><p className="text-xs text-gray-500">Présence prévue</p></div>
          </div>
        </div>
        <div className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center"><Sparkles className="w-5 h-5 text-violet-500" /></div>
            <div><p className="text-2xl font-bold text-violet-500">78%</p><p className="text-xs text-gray-500">Confiance moyenne</p></div>
          </div>
        </div>
      </div>

      {/* Filter */}
      <div className="flex gap-2">
        <button onClick={() => setFilter('all')} className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === 'all' ? 'bg-violet-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>Toutes</button>
        <button onClick={() => setFilter('high')} className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === 'high' ? 'bg-red-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>🔴 Risques élevés</button>
        <button onClick={() => setFilter('growth')} className={`px-4 py-2 rounded-xl text-sm font-medium transition ${filter === 'growth' ? 'bg-green-500 text-white' : 'bg-white/5 text-gray-500 hover:bg-white/10'}`}>📈 Croissance</button>
      </div>

      {/* Predictions list */}
      <div className="space-y-3">
        {filtered.map((pred) => (
          <div key={pred.id} className="glass rounded-2xl p-5 border border-white/20 dark:border-white/[0.06] hover:shadow-lg transition">
            <div className="flex items-start justify-between">
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 rounded-xl bg-gray-50 dark:bg-white/5 flex items-center justify-center">{typeIcon(pred.type)}</div>
                <div>
                  <h3 className="font-semibold text-gray-900 dark:text-white text-sm">{pred.metric}</h3>
                  <div className="flex items-center gap-3 mt-1">
                    <span className="text-xs text-gray-400">Actuel: <span className="text-gray-600 dark:text-gray-300 font-medium">{pred.current.toLocaleString()}</span></span>
                    <span className="text-xs text-gray-400">→</span>
                    <span className="text-xs text-gray-400">Prévu: <span className="text-gray-900 dark:text-white font-bold">{pred.predicted.toLocaleString(undefined, { maximumFractionDigits: 1 })}</span></span>
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
            <div className="mt-3 p-3 rounded-xl bg-gray-50 dark:bg-white/[0.02] border border-gray-100 dark:border-white/[0.03]">
              <p className="text-xs text-gray-500 leading-relaxed">{pred.explanation}</p>
            </div>
            <div className="mt-2 h-1.5 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden">
              <div className="h-full bg-gradient-to-r from-violet-400 to-violet-600 rounded-full" style={{ width: `${pred.confidence * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
