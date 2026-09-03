import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  Brain, TrendingUp, Users, BarChart3, Activity, Target, Loader2,
} from 'lucide-react';

interface ExecutiveStats {
  totalMembers: number;
  activeFamilies: number;
  totalEvents: number;
  conversionRate: number;
  averageEngagement: number;
  growthRate: number;
  totalRevenue?: number;
  pendingRequests: number;
}

export default function ExecutiveInsightsPage() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['executive-insights', 'stats'],
    queryFn: async () => {
      const res = await api.get('/executive-insights/stats');
      return res.data as ExecutiveStats;
    },
  });

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Brain className="w-5 h-5 text-purple-500" />
            <h1 className="page-title">Aperçu exécutif</h1>
          </div>
          <p className="page-subtitle">Indicateurs stratégiques et synthèse de performance</p>
        </div>
      </div>

      {stats ? (
        <div className="space-y-6 animate-slide-up">
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: 'Membres', value: stats.totalMembers, icon: Users, color: 'from-primary-500 to-primary-600' },
              { label: 'Familles actives', value: stats.activeFamilies, icon: Users, color: 'from-green-500 to-emerald-500' },
              { label: 'Événements', value: stats.totalEvents, icon: Activity, color: 'from-blue-500 to-indigo-500' },
              { label: 'Demandes en attente', value: stats.pendingRequests, icon: Target, color: 'from-amber-500 to-orange-500' },
            ].map((stat, i) => (
              <div key={stat.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
                <div className="flex items-start justify-between mb-2">
                  <span className="stat-label text-[10px]">{stat.label}</span>
                  <div className={`p-1.5 rounded-lg bg-gradient-to-br ${stat.color} text-white shadow-sm`}>
                    <stat.icon className="w-3.5 h-3.5" />
                  </div>
                </div>
                <p className="stat-value text-xl">{stat.value}</p>
              </div>
            ))}
          </div>

          {/* Performance metrics */}
          <div className="glass-card p-6">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <BarChart3 className="w-4 h-4 text-purple-500" />
              Indicateurs de performance
            </h3>
            <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
              {[
                { label: 'Taux de conversion', value: `${stats.conversionRate}%`, desc: 'Nouveaux membres / visiteurs' },
                { label: 'Engagement moyen', value: `${stats.averageEngagement}%`, desc: 'Participation aux activités' },
                { label: 'Taux de croissance', value: `${stats.growthRate >= 0 ? '+' : ''}${stats.growthRate}%`, desc: 'Croissance mensuelle' },
                ...(stats.totalRevenue !== undefined ? [{ label: 'Revenus', value: `${stats.totalRevenue.toLocaleString()} €`, desc: 'Revenus totaux' }] : []),
              ].map((metric) => (
                <div key={metric.label} className="p-4 rounded-xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-700/60">
                  <p className="text-xs text-gray-400 mb-1">{metric.label}</p>
                  <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{metric.value}</p>
                  <p className="text-[10px] text-gray-400 mt-0.5">{metric.desc}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Insights */}
          <div className="glass-card p-6">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <Brain className="w-4 h-4 text-purple-500" />
              Insights IA
            </h3>
            <div className="space-y-3">
              {[
                {
                  title: 'Croissance',
                  insight: stats.growthRate > 0
                    ? `Croissance positive de ${stats.growthRate}%. Continuez les efforts d'évangélisation.`
                    : 'Croissance négative. Revoyez les stratégies d\'accueil et d\'intégration.',
                  color: stats.growthRate > 0 ? 'border-green-500' : 'border-red-500',
                },
                {
                  title: 'Engagement',
                  insight: stats.averageEngagement >= 60
                    ? 'Bon niveau d\'engagement. Les membres sont actifs et participent aux activités.'
                    : 'Engagement faible. Proposez des activités plus variées et interactives.',
                  color: stats.averageEngagement >= 60 ? 'border-green-500' : 'border-amber-500',
                },
                {
                  title: 'Recommandation',
                  insight: `Avec ${stats.activeFamilies} familles actives et ${stats.totalMembers} membres, ${stats.pendingRequests > 0 ? `${stats.pendingRequests} demande(s) nécessitent votre attention.` : 'tout est à jour.'}`,
                  color: 'border-primary-500',
                },
              ].map((item) => (
                <div key={item.title} className={`p-4 rounded-xl border-l-4 ${item.color} bg-gray-50 dark:bg-gray-800/40`}>
                  <p className="text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">{item.title}</p>
                  <p className="text-sm text-gray-600 dark:text-gray-400">{item.insight}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="glass-card p-10 text-center">
          <Brain className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucune donnée exécutive disponible.</p>
        </div>
      )}
    </div>
  );
}
