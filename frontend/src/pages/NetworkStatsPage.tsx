import { useQuery } from '@tanstack/react-query';
import { Globe, RefreshCw, Users, MapPin, Calendar, BookOpen } from 'lucide-react';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface NetworkStats {
  totalChurches: number;
  totalMembers: number;
  totalEvents: number;
  totalResources: number;
  activeCountries: string[];
  recentActivity: string;
}

export default function NetworkStatsPage() {
  const { data: stats, isLoading, error, refetch } = useQuery({
    queryKey: ['network-stats'],
    queryFn: async () => (await api.get<NetworkStats>('/network/stats')).data,
  });

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="p-6 text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="p-3 rounded-xl bg-gradient-to-br from-blue-500 to-cyan-600 text-white shadow-lg">
          <Globe className="w-6 h-6" />
        </div>
        <div>
          <h1 className="page-title">Statistiques du Réseau</h1>
          <p className="page-subtitle">Vue d'ensemble du réseau Discipolat</p>
        </div>
        <button onClick={() => refetch()} className="ml-auto p-2 rounded-xl bg-white/10 hover:bg-white/20 transition">
          <RefreshCw className="w-4 h-4 text-gray-500" />
        </button>
      </div>

      {!stats ? (
        <EmptyState icon={<Globe className="w-8 h-8 text-gray-400" />}
          title="Aucune donnée"
          message="Les statistiques du réseau seront affichées ici" />
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10 text-center">
              <MapPin className="w-6 h-6 text-blue-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.totalChurches}</div>
              <div className="text-xs text-gray-500">Églises</div>
            </div>
            <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10 text-center">
              <Users className="w-6 h-6 text-emerald-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.totalMembers}</div>
              <div className="text-xs text-gray-500">Membres</div>
            </div>
            <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10 text-center">
              <Calendar className="w-6 h-6 text-purple-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.totalEvents}</div>
              <div className="text-xs text-gray-500">Événements</div>
            </div>
            <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10 text-center">
              <BookOpen className="w-6 h-6 text-amber-500 mx-auto mb-2" />
              <div className="text-2xl font-bold text-gray-900 dark:text-white">{stats.totalResources}</div>
              <div className="text-xs text-gray-500">Ressources</div>
            </div>
          </div>

          {stats.activeCountries && stats.activeCountries.length > 0 && (
            <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10 mb-6">
              <h3 className="font-semibold text-gray-900 dark:text-white mb-3">Pays actifs</h3>
              <div className="flex flex-wrap gap-2">
                {stats.activeCountries.map(c => (
                  <span key={c} className="px-3 py-1 rounded-full bg-blue-100 dark:bg-blue-500/20 text-blue-700 dark:text-blue-400 text-sm">
                    {c}
                  </span>
                ))}
              </div>
            </div>
          )}

          {stats.recentActivity && (
            <div className="bg-white dark:bg-white/5 rounded-xl p-5 border border-gray-200 dark:border-white/10">
              <h3 className="font-semibold text-gray-900 dark:text-white mb-2">Activité récente</h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">{stats.recentActivity}</p>
            </div>
          )}
        </>
      )}
    </div>
  );
}
