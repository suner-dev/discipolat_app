import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  BarChart3, TrendingUp, TrendingDown, Users, Activity, Eye, Heart,
  Loader2, Search,
} from 'lucide-react';

interface EngagementMetric {
  id: string;
  metricName: string;
  metricCategory: string;
  metricValue: number;
  previousValue?: number;
  changePercentage?: number;
  recordedAt: string;
}

export default function EngagementAnalyticsPage() {
  const [category, setCategory] = useState('');
  const [search] = useState('');

  const { data: metrics = [], isLoading } = useQuery({
    queryKey: ['engagement-analytics', 'dashboard'],
    queryFn: async () => {
      const res = await api.get('/engagement-analytics/dashboard');
      return (res.data.content || res.data || []) as EngagementMetric[];
    },
  });

  const { data: categoryMetrics = [] } = useQuery({
    queryKey: ['engagement-analytics', 'category', category],
    queryFn: async () => {
      const res = await api.get(`/engagement-analytics/category/${category}`);
      return (res.data.content || res.data || []) as EngagementMetric[];
    },
    enabled: !!category,
  });

  const displayMetrics = category ? categoryMetrics : metrics;

  const grouped = displayMetrics.reduce<Record<string, EngagementMetric[]>>((acc, m) => {
    const key = m.metricCategory || 'general';
    if (!acc[key]) acc[key] = [];
    acc[key].push(m);
    return acc;
  }, {});

  const totalValue = metrics.reduce((sum, m) => sum + (m.metricValue || 0), 0);
  const avgChange = metrics.length > 0
    ? metrics.reduce((sum, m) => sum + (m.changePercentage || 0), 0) / metrics.length
    : 0;

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <BarChart3 className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Analytics d'engagement</h1>
          </div>
          <p className="page-subtitle">Tableau de bord des métriques d'engagement</p>
        </div>
      </div>

      {/* Summary stats */}
      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        {[
          { label: 'Total métriques', value: metrics.length, icon: Activity, color: 'from-primary-500 to-primary-600' },
          { label: 'Valeur totale', value: totalValue.toLocaleString(), icon: BarChart3, color: 'from-blue-500 to-indigo-500' },
          { label: 'Variation moyenne', value: `${avgChange >= 0 ? '+' : ''}${avgChange.toFixed(1)}%`, icon: avgChange >= 0 ? TrendingUp : TrendingDown, color: avgChange >= 0 ? 'from-green-500 to-emerald-500' : 'from-red-500 to-rose-500' },
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

      {/* Category filter */}
      <div className="flex gap-2 mb-6 flex-wrap">
        <button
          onClick={() => setCategory('')}
          className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
            !category ? 'bg-primary-100 dark:bg-primary-500/20 text-primary-700 dark:text-primary-400' : 'bg-white/5 text-gray-400 hover:bg-white/10'
          }`}
        >
          Toutes
        </button>
        {Object.keys(grouped).map((cat) => (
          <button
            key={cat}
            onClick={() => setCategory(cat)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
              category === cat ? 'bg-primary-100 dark:bg-primary-500/20 text-primary-700 dark:text-primary-400' : 'bg-white/5 text-gray-400 hover:bg-white/10'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Metrics grid */}
      {displayMetrics.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <BarChart3 className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucune métrique disponible.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(grouped).map(([cat, catMetrics]) => (
            <div key={cat} className="animate-slide-up">
              <h3 className="text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">{cat}</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {catMetrics.map((m) => (
                  <div key={m.id} className="glass-card px-5 py-4 hover:shadow-lg transition-shadow">
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="text-xs font-semibold text-gray-700 dark:text-gray-300">{m.metricName}</h4>
                      {m.changePercentage !== undefined && (
                        <span className={`flex items-center gap-0.5 text-[10px] ${m.changePercentage >= 0 ? 'text-green-500' : 'text-red-500'}`}>
                          {m.changePercentage >= 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                          {m.changePercentage >= 0 ? '+' : ''}{m.changePercentage}%
                        </span>
                      )}
                    </div>
                    <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{m.metricValue.toLocaleString()}</p>
                    <p className="text-[10px] text-gray-400 mt-1">
                      {new Date(m.recordedAt).toLocaleString('fr-FR')}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
