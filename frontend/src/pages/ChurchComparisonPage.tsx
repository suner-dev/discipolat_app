import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  BarChart3, Users, Globe, Layers, RefreshCw, Loader2, TrendingUp, Search,
  Target, Award, ChevronDown, Info,
} from 'lucide-react';

interface ChurchData {
  id: string;
  nomEglise: string;
  pays: string;
  denomination: string;
  effectif: number;
  tauxPresence: number;
  tauxRetention: number;
  scoreSpirituelMoyen: number;
}

interface ClusterInfo {
  cluster: string;
  seuilEffectif: string;
  nbEglises: number;
  effectifMoyen: number;
  tauxPresenceMoyen: number;
  tauxRetentionMoyen: number;
  scoreSpirituelMoyen: number;
}

interface BenchmarkResult {
  nous: { effectif: number; tauxPresence: number; tauxRetention: number; scoreSpirituel: number };
  moyenne: { effectif: number; tauxPresence: number; tauxRetention: number; scoreSpirituel: number };
  rang: number;
  total: number;
}

export default function ChurchComparisonPage() {
  const [view, setView] = useState<'benchmark' | 'clusters' | 'byCountry' | 'byDenom'>('benchmark');
  const [ourId, setOurId] = useState('');

  const { data: churches = [], isLoading } = useQuery({
    queryKey: ['church-comparisons'],
    queryFn: async () => { const res = await api.get('/church-comparisons'); return res.data as ChurchData[]; },
  });

  const { data: benchmark, isLoading: benchLoading } = useQuery({
    queryKey: ['church-benchmark', ourId],
    queryFn: async () => {
      if (!ourId) return null;
      const res = await api.get(`/church-comparisons/${ourId}/benchmark`);
      return res.data as BenchmarkResult;
    },
    enabled: !!ourId,
  });

  const { data: clusterData } = useQuery({
    queryKey: ['church-clusters', ourId],
    queryFn: async () => {
      const params = ourId ? { ourId } : {};
      const res = await api.get('/church-comparisons/clusters', { params });
      return res.data as { clusters: ClusterInfo[]; notreEglise?: string; notreCluster?: string; notRang?: number; tailleCluster?: number };
    },
  });

  const byCountry = churches.reduce((acc, c) => {
    const key = c.pays || 'Inconnu';
    if (!acc[key]) acc[key] = [];
    acc[key].push(c);
    return acc;
  }, {} as Record<string, ChurchData[]>);

  const byDenom = churches.reduce((acc, c) => {
    const key = c.denomination || 'Inconnue';
    if (!acc[key]) acc[key] = [];
    acc[key].push(c);
    return acc;
  }, {} as Record<string, ChurchData[]>);

  const avg = (arr: number[]) => arr.length ? Math.round(arr.reduce((a, b) => a + b, 0) / arr.length * 100) / 100 : 0;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-indigo-500" />
            Comparaison inter-églises
          </h1>
          <p className="page-subtitle">Benchmark anonyme • Clustering par taille • Analyse par pays/dénomination</p>
        </div>
      </div>

      {/* Church selector */}
      <div className="glass-card p-4 mb-6 flex items-center gap-4">
        <Target className="w-5 h-5 text-indigo-500" />
        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Notre église :</span>
        <select className="input text-sm flex-1" value={ourId} onChange={e => setOurId(e.target.value)}>
          <option value="">Sélectionnez votre église</option>
          {churches.map(c => <option key={c.id} value={c.id}>{c.nomEglise} ({c.pays})</option>)}
        </select>
      </div>

      {/* Tabs */}
      <div className="flex gap-2 mb-6">
        {[
          { key: 'benchmark', label: 'Benchmark', icon: TrendingUp },
          { key: 'clusters', label: 'Clusters', icon: Layers },
          { key: 'byCountry', label: 'Par pays', icon: Globe },
          { key: 'byDenom', label: 'Par dénomination', icon: Users },
        ].map(tab => (
          <button key={tab.key} onClick={() => setView(tab.key as typeof view)}
            className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              view === tab.key
                ? 'bg-indigo-100 dark:bg-indigo-500/20 text-indigo-700 dark:text-indigo-400'
                : 'text-gray-500 hover:bg-gray-100 dark:hover:bg-white/5'
            }`}>
            <tab.icon className="w-4 h-4" /> {tab.label}
          </button>
        ))}
      </div>

      {/* Benchmark View */}
      {view === 'benchmark' && (
        <div className="space-y-4">
          {benchLoading ? (
            <div className="flex justify-center py-12"><Loader2 className="w-6 h-6 animate-spin" /></div>
          ) : !benchmark ? (
            <div className="glass-card p-12 text-center">
              <BarChart3 className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">Sélectionnez votre église pour voir le benchmark</p>
            </div>
          ) : (
            <>
              <div className="glass-card p-6 text-center">
                <Award className="w-8 h-8 text-indigo-500 mx-auto mb-2" />
                <p className="text-lg font-bold text-gray-900 dark:text-gray-100">
                  Rang {benchmark.rang} / {benchmark.total}
                </p>
                <p className="text-xs text-gray-400">parmi les églises comparables</p>
              </div>
              <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                {[
                  { label: 'Effectif', ours: benchmark.nous.effectif, avg: benchmark.moyenne.effectif, unit: '' },
                  { label: 'Présence', ours: `${benchmark.nous.tauxPresence}%`, avg: `${benchmark.moyenne.tauxPresence}%`, unit: '%' },
                  { label: 'Rétention', ours: `${benchmark.nous.tauxRetention}%`, avg: `${benchmark.moyenne.tauxRetention}%`, unit: '%' },
                  { label: 'Score spirituel', ours: benchmark.nous.scoreSpirituel, avg: benchmark.moyenne.scoreSpirituel, unit: '' },
                ].map(item => (
                  <div key={item.label} className="stat-card">
                    <span className="stat-label text-[10px]">{item.label}</span>
                    <div className="flex items-end gap-2 mt-1">
                      <span className="text-xl font-bold text-indigo-500">{item.ours}</span>
                      <span className="text-xs text-gray-400 mb-0.5">vs {item.avg}</span>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}

      {/* Clusters View */}
      {view === 'clusters' && clusterData && (
        <div className="space-y-4">
          {clusterData.notreEglise && (
            <div className="glass-card p-4 border-l-4 border-indigo-500">
              <p className="text-sm"><span className="font-bold">{clusterData.notreEglise}</span> est dans le cluster <span className="badge">{clusterData.notreCluster}</span></p>
              <p className="text-xs text-gray-400">Rang {clusterData.notRang} / {clusterData.tailleCluster} dans son cluster</p>
            </div>
          )}
          <div className="grid md:grid-cols-3 gap-4">
            {clusterData.clusters.map(c => (
              <div key={c.cluster} className="glass-card p-5">
                <div className="flex items-center gap-2 mb-3">
                  <Layers className="w-5 h-5 text-indigo-500" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">{c.cluster}</h3>
                </div>
                <p className="text-xs text-gray-400 mb-3">{c.seuilEffectif} membres</p>
                <div className="space-y-2 text-xs">
                  <div className="flex justify-between"><span className="text-gray-500">Églises</span><span className="font-medium">{c.nbEglises}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Effectif moyen</span><span className="font-medium">{c.effectifMoyen}</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Présence</span><span className="font-medium">{c.tauxPresenceMoyen}%</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Rétention</span><span className="font-medium">{c.tauxRetentionMoyen}%</span></div>
                  <div className="flex justify-between"><span className="text-gray-500">Score spirituel</span><span className="font-medium">{c.scoreSpirituelMoyen}</span></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* By Country View */}
      {view === 'byCountry' && (
        <div className="space-y-3">
          {Object.entries(byCountry).sort((a, b) => b[1].length - a[1].length).map(([country, list]) => (
            <div key={country} className="glass-card p-4">
              <div className="flex items-center gap-2 mb-2">
                <Globe className="w-4 h-4 text-indigo-500" />
                <h3 className="font-bold text-sm text-gray-900 dark:text-gray-100">{country}</h3>
                <span className="badge">{list.length} églises</span>
              </div>
              <div className="grid grid-cols-4 gap-2 text-[10px] text-gray-400">
                <span>Effectif moyen : {avg(list.map(c => c.effectif))}</span>
                <span>Présence : {avg(list.map(c => c.tauxPresence))}%</span>
                <span>Rétention : {avg(list.map(c => c.tauxRetention))}%</span>
                <span>Score : {avg(list.map(c => c.scoreSpirituelMoyen))}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* By Denomination View */}
      {view === 'byDenom' && (
        <div className="space-y-3">
          {Object.entries(byDenom).sort((a, b) => b[1].length - a[1].length).map(([denom, list]) => (
            <div key={denom} className="glass-card p-4">
              <div className="flex items-center gap-2 mb-2">
                <Users className="w-4 h-4 text-indigo-500" />
                <h3 className="font-bold text-sm text-gray-900 dark:text-gray-100">{denom}</h3>
                <span className="badge">{list.length} églises</span>
              </div>
              <div className="grid grid-cols-4 gap-2 text-[10px] text-gray-400">
                <span>Effectif moyen : {avg(list.map(c => c.effectif))}</span>
                <span>Présence : {avg(list.map(c => c.tauxPresence))}%</span>
                <span>Rétention : {avg(list.map(c => c.tauxRetention))}%</span>
                <span>Score : {avg(list.map(c => c.scoreSpirituelMoyen))}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
