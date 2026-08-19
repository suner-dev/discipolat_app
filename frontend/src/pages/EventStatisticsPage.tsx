import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Family } from '@/types';
import { BarChart3, Calendar, Users, TrendingUp, Loader2, Filter, X, Clock, CheckCircle2 } from 'lucide-react';

const PERIODS = [
  { value: '3', label: '3 mois' },
  { value: '6', label: '6 mois' },
  { value: '12', label: '1 an' },
  { value: '24', label: '2 ans' },
];

export default function EventStatisticsPage() {
  const [familleFilter, setFamilleFilter] = useState('');
  const [periode, setPeriode] = useState('12');

  const { data: families } = useQuery({
    queryKey: ['families', 'all'],
    queryFn: async () => { const res = await api.get('/families?size=100'); return res.data.content as Family[]; },
  });

  const { data: stats, isLoading } = useQuery({
    queryKey: ['events', 'statistics', familleFilter, periode],
    queryFn: async () => {
      const params = new URLSearchParams({ mois: periode });
      if (familleFilter) params.set('familleId', familleFilter);
      const res = await api.get(`/events/statistics?${params}`);
      return res.data as {
        totalEvenements: number;
        totalInscrits: number;
        totalPresents: number;
        totalAbsents: number;
        tauxParticipation: number;
        parType: Record<string, number>;
        parMois?: Record<string, number>;
      };
    },
  });

  const typeLabels: Record<string, string> = {
    SORTIE: 'Sorties', RETRAITE: 'Retraites', EVANGELISATION: 'Évangélisations',
    REUNION: 'Réunions', VISITE: 'Visites', CONFERENCE: 'Conférences',
    FORMATION: 'Formations', ANNIVERSAIRE: 'Anniversaires', CULTE: 'Cultes',
    ETUDE_BIBLIQUE: 'Études bib.', VEILLEE: 'Veillées', PRIERE: 'Prières', AUTRE: 'Autres',
  };

  const typeColors: Record<string, string> = {
    SORTIE: 'from-blue-500 to-indigo-500', RETRAITE: 'from-purple-500 to-violet-500',
    EVANGELISATION: 'from-green-500 to-emerald-500', REUNION: 'from-amber-500 to-orange-500',
    VISITE: 'from-rose-500 to-pink-500', CONFERENCE: 'from-indigo-500 to-blue-500',
    FORMATION: 'from-cyan-500 to-teal-500', ANNIVERSAIRE: 'from-pink-500 to-rose-500',
    CULTE: 'from-yellow-500 to-amber-500', AUTRE: 'from-gray-500 to-slate-500',
  };

  const hasFilter = Boolean(familleFilter);

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

      {/* Filters */}
      <div className="glass-card p-4 mb-6 flex flex-wrap items-center gap-3 animate-slide-up">
        <div className="flex items-center gap-1.5">
          <Filter className="w-4 h-4 text-gray-400" />
          <select className="input !w-auto text-xs" value={familleFilter} onChange={(e) => setFamilleFilter(e.target.value)}>
            <option value="">Toutes les familles</option>
            {(families || []).map((f) => (<option key={f.id} value={f.id}>{f.nom}</option>))}
          </select>
        </div>
        <div className="flex items-center gap-1.5 ml-auto">
          <Clock className="w-4 h-4 text-gray-400" />
          {PERIODS.map((p) => (
            <button key={p.value}
              onClick={() => setPeriode(p.value)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-all ${
                periode === p.value
                  ? 'bg-primary-500/15 text-primary-600 dark:text-primary-400 border border-primary-500/30'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 border border-transparent'
              }`}>
              {p.label}
            </button>
          ))}
        </div>
        {hasFilter && (
          <button onClick={() => setFamilleFilter('')} className="btn-ghost btn-xs">
            <X className="w-3 h-3" /> Réinitialiser
          </button>
        )}
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
      ) : !stats ? (
        <div className="glass-card p-10 text-center">
          <BarChart3 className="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p className="text-gray-500 font-medium">Aucune statistique disponible</p>
        </div>
      ) : (
        <>
          {/* Stats KPIs */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            {[
              { label: 'Événements', value: stats.totalEvenements, icon: Calendar, color: 'from-primary-500 to-primary-600' },
              { label: 'Inscrits', value: stats.totalInscrits, icon: Users, color: 'from-blue-500 to-indigo-500' },
              { label: 'Présents', value: stats.totalPresents, icon: CheckCircle2, color: 'from-emerald-500 to-green-500' },
              { label: 'Taux participation', value: `${stats.tauxParticipation}%`, icon: TrendingUp, color: stats.tauxParticipation >= 70 ? 'from-emerald-500 to-green-500' : stats.tauxParticipation >= 40 ? 'from-amber-500 to-orange-500' : 'from-red-500 to-rose-500' },
            ].map((kpi, i) => (
              <div key={kpi.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
                <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${kpi.color} opacity-60`} />
                <div className="flex items-start justify-between mb-2">
                  <span className="stat-label text-[10px]">{kpi.label}</span>
                  <div className={`p-1.5 rounded-lg bg-gradient-to-br ${kpi.color} text-white shadow-sm`}>
                    <kpi.icon className="w-3.5 h-3.5" />
                  </div>
                </div>
                <p className="stat-value text-xl">{kpi.value}</p>
              </div>
            ))}
          </div>

          {/* Attendance bars */}
          {(stats.totalInscrits > 0 || stats.totalPresents > 0) && (
            <div className="glass-card p-5 mb-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Présence vs inscriptions</h3>
              <div className="space-y-3">
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-gray-500">Inscrits</span>
                    <span className="font-semibold text-gray-900 dark:text-gray-100">{stats.totalInscrits}</span>
                  </div>
                  <div className="h-3 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                    <div className="h-full rounded-full bg-gradient-to-r from-blue-500 to-indigo-500 transition-all duration-700" style={{ width: '100%' }} />
                  </div>
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-gray-500">Présents</span>
                    <span className="font-semibold text-gray-900 dark:text-gray-100">{stats.totalPresents}</span>
                  </div>
                  <div className="h-3 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                    <div className="h-full rounded-full bg-gradient-to-r from-emerald-500 to-green-500 transition-all duration-700"
                      style={{ width: `${stats.totalInscrits > 0 ? (stats.totalPresents / stats.totalInscrits) * 100 : 0}%` }} />
                  </div>
                </div>
                {stats.totalAbsents > 0 && (
                  <div>
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-gray-500">Absents</span>
                      <span className="font-semibold text-gray-900 dark:text-gray-100">{stats.totalAbsents}</span>
                    </div>
                    <div className="h-3 rounded-full bg-gray-100 dark:bg-gray-800 overflow-hidden">
                      <div className="h-full rounded-full bg-gradient-to-r from-red-500 to-rose-500 transition-all duration-700"
                        style={{ width: `${stats.totalInscrits > 0 ? (stats.totalAbsents / stats.totalInscrits) * 100 : 0}%` }} />
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Type distribution */}
          {Object.keys(stats.parType).length > 0 && (
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '260ms' }}>
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
                          <div className={`h-full rounded-full bg-gradient-to-r ${typeColors[type] || 'from-gray-500 to-slate-500'} transition-all duration-700`}
                            style={{ width: `${pct}%` }} />
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
