import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Family } from '@/types';
import { BarChart3, Calendar, Users, TrendingUp, Loader2, Filter } from 'lucide-react';

export default function EventStatisticsPage() {
  const [familleFilter, setFamilleFilter] = useState('');

  const { data: families } = useQuery({
    queryKey: ['families', 'all'],
    queryFn: async () => {
      const res = await api.get('/families?size=100');
      return res.data.content as Family[];
    },
  });

  const { data: stats, isLoading } = useQuery({
    queryKey: ['events', 'statistics', familleFilter],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (familleFilter) params.set('familleId', familleFilter);
      const res = await api.get(`/events/statistics?${params}`);
      return res.data as {
        totalEvenements: number;
        totalInscrits: number;
        totalPresents: number;
        totalAbsents: number;
        tauxParticipation: number;
        parType: Record<string, number>;
      };
    },
  });

  const typeLabels: Record<string, string> = {
    SORTIE: 'Sorties',
    RETRAITE: 'Retraites',
    EVANGELISATION: 'Évangélisations',
    REUNION: 'Réunions',
    VISITE: 'Visites',
    CONFERENCE: 'Conférences',
    FORMATION: 'Formations',
    ANNIVERSAIRE: 'Anniversaires',
    AUTRE: 'Autres',
  };

  const typeColors: Record<string, string> = {
    SORTIE: 'bg-blue-500', RETRAITE: 'bg-purple-500', EVANGELISATION: 'bg-green-500',
    REUNION: 'bg-amber-500', VISITE: 'bg-rose-500', CONFERENCE: 'bg-indigo-500',
    FORMATION: 'bg-cyan-500', ANNIVERSAIRE: 'bg-pink-500', AUTRE: 'bg-gray-500',
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div className="flex items-center gap-3">
          <div className="p-3 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-500 text-white shadow-lg">
            <BarChart3 className="w-6 h-6" />
          </div>
          <div>
            <h1 className="page-title">Statistiques des événements</h1>
            <p className="page-subtitle">Indicateurs de participation et répartition par type</p>
          </div>
        </div>
      </div>

      <div className="glass-card p-4 mb-6 flex items-center gap-3">
        <Filter className="w-4 h-4 text-gray-400" />
        <select className="input flex-1" value={familleFilter} onChange={(e) => setFamilleFilter(e.target.value)}>
          <option value="">Toutes les familles</option>
          {(families || []).map((f) => (<option key={f.id} value={f.id}>{f.nom}</option>))}
        </select>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : !stats ? (
        <div className="glass-card p-10 text-center"><BarChart3 className="w-12 h-12 mx-auto mb-3 text-gray-300" /><p className="text-gray-500">Aucune statistique disponible</p></div>
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div className="glass-card p-5 text-center">
              <Calendar className="w-5 h-5 mx-auto mb-2 text-primary-500" />
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{stats.totalEvenements}</p>
              <p className="text-xs text-gray-500">Événements</p>
            </div>
            <div className="glass-card p-5 text-center">
              <Users className="w-5 h-5 mx-auto mb-2 text-blue-500" />
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{stats.totalInscrits}</p>
              <p className="text-xs text-gray-500">Inscrits</p>
            </div>
            <div className="glass-card p-5 text-center">
              <TrendingUp className="w-5 h-5 mx-auto mb-2 text-green-500" />
              <p className="text-2xl font-bold text-green-600">{stats.totalPresents}</p>
              <p className="text-xs text-gray-500">Présents</p>
            </div>
            <div className="glass-card p-5 text-center">
              <BarChart3 className="w-5 h-5 mx-auto mb-2 text-amber-500" />
              <p className="text-2xl font-bold text-gray-900 dark:text-gray-100">{stats.tauxParticipation}%</p>
              <p className="text-xs text-gray-500">Taux de participation</p>
            </div>
          </div>

          {Object.keys(stats.parType).length > 0 && (
            <div className="glass-card p-6">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Répartition par type d'événement</h3>
              <div className="space-y-3">
                {Object.entries(stats.parType)
                  .sort(([, a], [, b]) => b - a)
                  .map(([type, count]) => {
                    const maxCount = Math.max(...Object.values(stats.parType));
                    const pct = (count / maxCount) * 100;
                    return (
                      <div key={type}>
                        <div className="flex justify-between text-sm mb-1">
                          <span className="text-gray-700 dark:text-gray-300">{typeLabels[type] || type}</span>
                          <span className="font-semibold text-gray-900 dark:text-gray-100">{count}</span>
                        </div>
                        <div className="w-full h-2 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                          <div className={`h-full rounded-full ${typeColors[type] || 'bg-gray-500'} transition-all`} style={{ width: `${pct}%` }} />
                        </div>
                      </div>
                    );
                  })}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
