import { useQuery } from '@tanstack/react-query';
import { GitBranch, Users, Sparkles, TrendingUp, Loader2, RefreshCw } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface TwinSnapshot {
  totalSouls: number;
  faiseurs: number;
  leaders: number;
  ratioLeadersPerFaiseur: number;
  avgRisk: number;
  scoreSante: number;
  activeMembers: number;
  newConverts: number;
  lastUpdated: string;
}

export default function TwinSnapshotPage() {
  const { data: snapshot, isLoading, error, refetch } = useQuery({
    queryKey: ['twin-snapshot'],
    queryFn: async () => (await api.get<TwinSnapshot>('/twin/snapshot')).data,
  });

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="p-6 text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-slate-600 to-slate-800 text-white shadow-lg">
          <GitBranch className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Snapshot du Jumeau Numérique</h1>
          <p className="page-subtitle">État actuel de votre église en temps réel</p>
        </div>
        <button onClick={() => refetch()} className="ml-auto p-2 rounded-xl bg-white/10 hover:bg-white/20 transition">
          <RefreshCw className="w-4 h-4 text-gray-500" />
        </button>
      </div>

      {!snapshot ? (
        <EmptyState icon={<GitBranch className="w-8 h-8 text-gray-400" />}
          title="Aucune donnée disponible"
          message="Le snapshot du jumeau numérique sera affiché ici" />
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl p-5 text-white">
              <TrendingUp className="w-5 h-5 opacity-80 mb-2" />
              <div className="text-2xl font-bold">{snapshot.totalSouls}</div>
              <div className="text-xs opacity-80">Total âmes</div>
            </div>
            <div className="bg-gradient-to-br from-emerald-500 to-teal-600 rounded-xl p-5 text-white">
              <Sparkles className="w-5 h-5 opacity-80 mb-2" />
              <div className="text-2xl font-bold">{snapshot.faiseurs}</div>
              <div className="text-xs opacity-80">Faiseurs</div>
            </div>
            <div className="bg-gradient-to-br from-purple-500 to-violet-600 rounded-xl p-5 text-white">
              <Users className="w-5 h-5 opacity-80 mb-2" />
              <div className="text-2xl font-bold">{snapshot.leaders}</div>
              <div className="text-xs opacity-80">Leaders</div>
            </div>
            <div className="bg-gradient-to-br from-amber-500 to-orange-600 rounded-xl p-5 text-white">
              <GitBranch className="w-5 h-5 opacity-80 mb-2" />
              <div className="text-2xl font-bold">{snapshot.ratioLeadersPerFaiseur?.toFixed(1)}</div>
              <div className="text-xs opacity-80">Ratio leaders/faiseurs</div>
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
            <div className="glass-card p-5 text-center">
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{snapshot.activeMembers}</div>
              <div className="text-xs text-gray-500">Membres actifs</div>
            </div>
            <div className="glass-card p-5 text-center">
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{snapshot.newConverts}</div>
              <div className="text-xs text-gray-500">Nouveaux convertis</div>
            </div>
            <div className="glass-card p-5 text-center">
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{snapshot.scoreSante || Math.round(100 - (snapshot.avgRisk || 0) * 10)}/100</div>
              <div className="text-xs text-gray-500">Score santé</div>
            </div>
          </div>

          <div className="glass-card p-6">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Détails</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Dernière mise à jour</span>
                <span className="text-gray-900 dark:text-white">{snapshot.lastUpdated ? new Date(snapshot.lastUpdated).toLocaleString('fr-FR') : '—'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Score de risque moyen</span>
                <span className="text-gray-900 dark:text-white">{((snapshot.avgRisk || 0) * 100).toFixed(0)}%</span>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
