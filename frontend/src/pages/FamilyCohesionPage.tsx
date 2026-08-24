import React, { useState, useEffect } from 'react';
import api from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import Toast from '@/components/shared/Toast';
import { Heart, TrendingUp, Users, BarChart3 } from 'lucide-react';

interface Cohesion {
  familleId: string;
  tauxParticipation: number;
  diversitéÂmes: number;
  équilibreCharges: number;
  scoreCohésion: number;
  recommandations?: string;
  calculéLe: string;
}

export default function FamilyCohesionPage() {
  const [familleId, setFamilleId] = useState('');
  const [cohesion, setCohesion] = useState<Cohesion | null>(null);
  const [loading, setLoading] = useState(false);

  const loadCohesion = async () => {
    if (!familleId) return;
    try { setLoading(true); const res = await api.get(`/family-cohesion/${familleId}`); setCohesion(res.data); }
    catch { setCohesion(null); } finally { setLoading(false); }
  };

  const calculate = async () => {
    if (!familleId) return;
    try {
      const res = await api.post(`/family-cohesion/calculate/${familleId}`, { tauxParticipation: 0.7, diversité: 5, équilibre: 6 });
      setCohesion(res.data);
      Toast.success('Cohésion calculée');
    } catch { Toast.error('Erreur'); }
  };

  return (
    <div className="min-h-screen p-4 md:p-8 max-w-7xl mx-auto">
      <h1 className="text-2xl md:text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-3 mb-8">
        <Heart className="w-8 h-8 text-pink-500" /> Cohésion Familiale
      </h1>

      <div className="flex gap-4 mb-6">
        <input type="text" value={familleId} onChange={e => setFamilleId(e.target.value)}
          className="flex-1 max-w-md px-4 py-2.5 rounded-xl border border-gray-200 dark:border-white/10 bg-white dark:bg-white/5 text-sm"
          placeholder="ID de la famille" />
        <button onClick={loadCohesion} disabled={!familleId}
          className="px-4 py-2 rounded-xl bg-pink-500 text-white text-sm font-medium hover:bg-pink-600 disabled:opacity-50">Voir</button>
        <button onClick={calculate} disabled={!familleId}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-pink-500 to-rose-500 text-white text-sm font-medium hover:from-pink-600 hover:to-rose-600 disabled:opacity-50">Calculer</button>
      </div>

      {loading ? <SkeletonLoader lines={3} variant="card" /> :
        cohesion ? (
          <div className="space-y-6">
            {/* Score */}
            <div className="bg-white dark:bg-white/5 rounded-2xl p-8 border border-gray-200 dark:border-white/10 text-center">
              <div className="text-6xl font-bold text-pink-600 mb-2">{(cohesion.scoreCohésion * 100).toFixed(0)}%</div>
              <div className="text-gray-500 text-sm">Score de cohésion</div>
              <div className="mt-4 h-3 bg-gray-100 dark:bg-white/5 rounded-full overflow-hidden max-w-md mx-auto">
                <div className="h-full bg-gradient-to-r from-pink-400 to-rose-400 rounded-full"
                  style={{ width: `${cohesion.scoreCohésion * 100}%` }} />
              </div>
            </div>

            {/* Indicators */}
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
                <Users className="w-5 h-5 mx-auto mb-1 text-blue-500" />
                <div className="text-2xl font-bold text-blue-600">{(cohesion.tauxParticipation * 100).toFixed(0)}%</div>
                <div className="text-xs text-gray-500">Taux participation</div>
              </div>
              <div className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
                <BarChart3 className="w-5 h-5 mx-auto mb-1 text-green-500" />
                <div className="text-2xl font-bold text-green-600">{cohesion.diversitéÂmes}</div>
                <div className="text-xs text-gray-500">Diversité</div>
              </div>
              <div className="bg-white dark:bg-white/5 rounded-xl p-4 border border-gray-200 dark:border-white/10 text-center">
                <TrendingUp className="w-5 h-5 mx-auto mb-1 text-purple-500" />
                <div className="text-2xl font-bold text-purple-600">{cohesion.équilibreCharges}</div>
                <div className="text-xs text-gray-500">Équilibre charges</div>
              </div>
            </div>

            {cohesion.recommandations && (
              <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
                <h3 className="font-medium text-gray-900 dark:text-white text-sm mb-2">Recommandations</h3>
                <div className="text-sm text-gray-600 dark:text-gray-400 space-y-1">
                  {cohesion.recommandations.split(' | ').map((r, i) => (
                    <div key={i} className="flex items-start gap-2">
                      <span className="text-pink-500 mt-0.5">•</span>
                      <span>{r}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        ) : null
      }
    </div>
  );
}
