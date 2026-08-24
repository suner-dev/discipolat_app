import React, { useState, useEffect } from 'react';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import Toast from '@/components/shared/Toast';
import { BarChart3, TrendingUp, TrendingDown, Minus, AlertTriangle, CheckCircle2, Lightbulb } from 'lucide-react';

interface KpiNarrative {
  id: string;
  typeKPI: string;
  valeurActuelle: number;
  valeurPrécédente: number;
  tendance: 'HAUSSE' | 'BAISSE' | 'STABLE' | 'SIGNIFICATIVE_HAUSSE' | 'SIGNIFICATIVE_BAISSE';
  variationPct: number;
  narration: string;
  causes: string;
  recommandations: string;
  généréLe: string;
}

const KPI_TYPES = [
  { key: 'PRÉSENCE', label: 'Présence', color: 'from-blue-500 to-cyan-500', icon: '📊' },
  { key: 'CROISSANCE', label: 'Croissance', color: 'from-green-500 to-emerald-500', icon: '🌱' },
  { key: 'RÉTENTION', label: 'Rétention', color: 'from-purple-500 to-violet-500', icon: '🔄' },
  { key: 'ENGAGEMENT', label: 'Engagement', color: 'from-orange-500 to-amber-500', icon: '🔥' },
  { key: 'SCORE_SPIRITUEL', label: 'Score spirituel', color: 'from-pink-500 to-rose-500', icon: '✝️' },
  { key: 'RAPPORTS', label: 'Rapports', color: 'from-teal-500 to-cyan-500', icon: '📝' },
  { key: 'PRIÈRES', label: 'Prières', color: 'from-indigo-500 to-blue-500', icon: '🙏' },
  { key: 'ÉVÉNEMENTS', label: 'Événements', color: 'from-red-500 to-orange-500', icon: '📅' },
];

export default function KpiDrillDownPage() {
  const { t } = useI18n();
  const [narratives, setNarratives] = useState<KpiNarrative[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedKpi, setSelectedKpi] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);

  useEffect(() => { loadNarratives(); }, []);

  const loadNarratives = async () => {
    try { setLoading(true); const res = await api.get('/kpi-narrative'); setNarratives(res.data || []); }
    catch { setNarratives([]); } finally { setLoading(false); }
  };

  const generateNarrative = async (typeKPI: string) => {
    try {
      setGenerating(true);
      setSelectedKpi(typeKPI);
      const valeur = Math.random() * 40 + 50; // Simulated current value
      const précédent = valeur + (Math.random() - 0.5) * 20; // Simulated previous
      const res = await api.post('/kpi-narrative/generate', {
        typeKPI,
        valeurActuelle: Math.round(valeur * 10) / 10,
        valeurPrécédente: Math.round(précédent * 10) / 10,
      });
      // Add to top of list
      setNarratives([res.data, ...narratives]);
      Toast.success('Narration générée !');
    } catch { Toast.error('Erreur'); }
    finally { setGenerating(false); }
  };

  const getTendanceIcon = (tendance: string) => {
    switch (tendance) {
      case 'SIGNIFICATIVE_HAUSSE':
      case 'HAUSSE': return <TrendingUp className="w-5 h-5 text-green-500" />;
      case 'SIGNIFICATIVE_BAISSE':
      case 'BAISSE': return <TrendingDown className="w-5 h-5 text-red-500" />;
      default: return <Minus className="w-5 h-5 text-gray-400" />;
    }
  };

  const getTendanceColor = (tendance: string) => {
    switch (tendance) {
      case 'SIGNIFICATIVE_HAUSSE': return 'text-green-600 bg-green-50 dark:bg-green-500/10';
      case 'HAUSSE': return 'text-green-500 bg-green-50 dark:bg-green-500/10';
      case 'SIGNIFICATIVE_BAISSE': return 'text-red-600 bg-red-50 dark:bg-red-500/10';
      case 'BAISSE': return 'text-red-500 bg-red-50 dark:bg-red-500/10';
      default: return 'text-gray-500 bg-gray-50 dark:bg-white/5';
    }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3">
            <BarChart3 className="w-8 h-8 text-blue-500" />
            Drill-down Narratif KPI
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1">Cliquez sur un KPI pour obtenir une analyse narrative automatique</p>
        </div>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        {KPI_TYPES.map(kpi => (
          <button key={kpi.key} onClick={() => generateNarrative(kpi.key)} disabled={generating}
            className={`relative overflow-hidden rounded-xl p-4 bg-gradient-to-br ${kpi.color} text-white text-left transition-all hover:scale-[1.02] hover:shadow-lg disabled:opacity-50`}>
            <div className="text-2xl mb-2">{kpi.icon}</div>
            <div className="text-sm font-semibold">{kpi.label}</div>
            <div className="text-xs opacity-80 mt-1">Cliquez pour analyser</div>
            {generating && selectedKpi === kpi.key && (
              <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              </div>
            )}
          </button>
        ))}
      </div>

      {/* Narratives */}
      <h2 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Analyses récentes</h2>
      {loading ? <SkeletonLoader lines={4} variant="card" /> :
        narratives.length === 0 ? (
          <div className="text-center py-12 text-gray-400">
            <BarChart3 className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>Cliquez sur un KPI ci-dessus pour générer une analyse narrative</p>
          </div>
        ) : (
          <div className="space-y-4">
            {narratives.map(n => {
              const kpiInfo = KPI_TYPES.find(k => k.key === n.typeKPI) || KPI_TYPES[0];
              return (
                <div key={n.id} className="bg-white dark:bg-white/5 rounded-xl border border-gray-200 dark:border-white/10 overflow-hidden">
                  {/* Header */}
                  <div className={`px-5 py-3 bg-gradient-to-r ${kpiInfo.color} text-white flex items-center justify-between`}>
                    <div className="flex items-center gap-2">
                      <span className="text-lg">{kpiInfo.icon}</span>
                      <span className="font-semibold text-sm">{kpiInfo.label}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {getTendanceIcon(n.tendance)}
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getTendanceColor(n.tendance)}`}>
                        {n.variationPct > 0 ? '+' : ''}{n.variationPct.toFixed(1)}%
                      </span>
                    </div>
                  </div>

                  {/* Narration */}
                  <div className="p-5">
                    <p className="text-sm text-gray-800 dark:text-gray-200 mb-4 leading-relaxed">{n.narration}</p>

                    <div className="grid md:grid-cols-2 gap-4">
                      {/* Causes */}
                      <div className="bg-amber-50 dark:bg-amber-500/10 rounded-lg p-4">
                        <div className="flex items-center gap-2 mb-2">
                          <AlertTriangle className="w-4 h-4 text-amber-500" />
                          <span className="text-xs font-semibold text-amber-700 dark:text-amber-400">Causes identifiées</span>
                        </div>
                        <div className="text-sm text-amber-800 dark:text-amber-300 space-y-1">
                          {n.causes.split(' | ').map((cause, i) => (
                            <div key={i} className="flex items-start gap-1">
                              <span className="text-amber-400 mt-0.5">•</span>
                              <span>{cause}</span>
                            </div>
                          ))}
                        </div>
                      </div>

                      {/* Recommandations */}
                      <div className="bg-green-50 dark:bg-green-500/10 rounded-lg p-4">
                        <div className="flex items-center gap-2 mb-2">
                          <Lightbulb className="w-4 h-4 text-green-500" />
                          <span className="text-xs font-semibold text-green-700 dark:text-green-400">Recommandations</span>
                        </div>
                        <div className="text-sm text-green-800 dark:text-green-300 space-y-1">
                          {n.recommandations.split(' | ').map((rec, i) => (
                            <div key={i} className="flex items-start gap-1">
                              <CheckCircle2 className="w-3 h-3 text-green-400 mt-0.5 shrink-0" />
                              <span>{rec}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-3 mt-3 text-xs text-gray-400">
                      <span>Valeur: {n.valeurActuelle.toFixed(1)}% (précédent: {n.valeurPrécédente.toFixed(1)}%)</span>
                      <span>•</span>
                      <span>Généré le {new Date(n.généréLe).toLocaleDateString('fr-FR')}</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
    </div>
  );
}
