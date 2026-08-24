import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import api from '@/lib/api';
import {
  BarChart3, TrendingUp, Users, Clock, RefreshCw, Loader2, Eye, MousePointer,
  Smartphone, Monitor, Globe, Calendar, ArrowUpRight, ArrowDownRight,
} from 'lucide-react';

interface UsageSummary {
  totalEvents: number;
  activeUsers: number;
  topPages: { page: string; views: number; uniqueUsers: number }[];
  topActions: { action: string; count: number }[];
  byDevice: { mobile: number; desktop: number; tablet: number };
  byHour: { hour: number; count: number }[];
  dailyActiveUsers: { date: string; count: number }[];
  period: string;
}

export default function UsageAnalyticsPage() {
  const [period, setPeriod] = useState('7d');

  const { data: summary, isLoading, refetch } = useQuery({
    queryKey: ['usage-analytics', period],
    queryFn: async () => {
      const res = await api.get('/usage-analytics/summary', { params: { period } });
      return res.data as UsageSummary;
    },
  });

  const trackMutation = useMutation({
    mutationFn: async () => {
      await api.post('/usage-analytics/track', {
        eventType: 'PAGE_VIEW',
        page: '/usage-analytics',
        metadata: { manual: true },
      });
    },
    onSuccess: () => toast.success('Événement tracké'),
  });

  const maxHourlyCount = summary ? Math.max(...summary.byHour.map(h => h.count), 1) : 1;
  const totalDevices = summary ? summary.byDevice.mobile + summary.byDevice.desktop + summary.byDevice.tablet : 1;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div>
          <h1 className="page-title flex items-center gap-2">
            <BarChart3 className="w-5 h-5 text-cyan-500" />
            Analytics d'usage
          </h1>
          <p className="page-subtitle">Pages vues • Actions • Appareils • Heatmap horaire</p>
        </div>
        <div className="flex gap-2">
          <select className="input text-sm" value={period} onChange={e => setPeriod(e.target.value)}>
            <option value="1d">24 heures</option>
            <option value="7d">7 jours</option>
            <option value="30d">30 jours</option>
            <option value="90d">90 jours</option>
          </select>
          <button onClick={() => refetch()} className="btn-ghost btn-sm"><RefreshCw className="w-4 h-4" /></button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12"><Loader2 className="w-6 h-6 animate-spin" /></div>
      ) : !summary ? (
        <div className="glass-card p-12 text-center">
          <BarChart3 className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500">Aucune donnée d'usage disponible</p>
        </div>
      ) : (
        <div className="space-y-6">
          {/* Top Stats */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: 'Événements totaux', value: summary.totalEvents, icon: Eye, color: 'cyan' },
              { label: 'Utilisateurs actifs', value: summary.activeUsers, icon: Users, color: 'green' },
              { label: 'Mobile', value: `${Math.round(summary.byDevice.mobile / totalDevices * 100)}%`, icon: Smartphone, color: 'blue' },
              { label: 'Desktop', value: `${Math.round(summary.byDevice.desktop / totalDevices * 100)}%`, icon: Monitor, color: 'purple' },
            ].map((stat, i) => (
              <div key={stat.label} className="stat-card" style={{ animationDelay: `${i * 50}ms` }}>
                <div className="flex items-center justify-between mb-1">
                  <span className="stat-label text-[10px]">{stat.label}</span>
                  <stat.icon className={`w-3.5 h-3.5 text-${stat.color}-500`} />
                </div>
                <p className="stat-value text-xl">{stat.value}</p>
              </div>
            ))}
          </div>

          {/* Hourly Heatmap */}
          <div className="glass-card p-6">
            <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
              <Clock className="w-4 h-4 text-cyan-500" /> Activité par heure
            </h3>
            <div className="flex items-end gap-1 h-32">
              {summary.byHour.map(h => (
                <div key={h.hour} className="flex-1 flex flex-col items-center gap-1">
                  <div className="w-full rounded-t bg-cyan-500 transition-all"
                    style={{ height: `${(h.count / maxHourlyCount) * 100}%`, minHeight: h.count > 0 ? 4 : 0, opacity: 0.3 + (h.count / maxHourlyCount) * 0.7 }} />
                  <span className="text-[8px] text-gray-400">{h.hour}h</span>
                </div>
              ))}
            </div>
          </div>

          <div className="grid lg:grid-cols-2 gap-6">
            {/* Top Pages */}
            <div className="glass-card p-6">
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <Eye className="w-4 h-4 text-cyan-500" /> Pages les plus vues
              </h3>
              <div className="space-y-2">
                {summary.topPages.slice(0, 10).map((p, i) => (
                  <div key={p.page} className="flex items-center gap-3 text-sm">
                    <span className="w-5 text-[10px] text-gray-400 text-right">{i + 1}</span>
                    <span className="flex-1 text-gray-700 dark:text-gray-300 font-mono text-xs truncate">{p.page}</span>
                    <span className="text-gray-500 font-medium">{p.views}</span>
                    <span className="text-[10px] text-gray-400">({p.uniqueUsers} users)</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Top Actions */}
            <div className="glass-card p-6">
              <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-4 flex items-center gap-2">
                <MousePointer className="w-4 h-4 text-cyan-500" /> Actions les plus fréquentes
              </h3>
              <div className="space-y-2">
                {summary.topActions.slice(0, 10).map((a, i) => (
                  <div key={a.action} className="flex items-center gap-3 text-sm">
                    <span className="w-5 text-[10px] text-gray-400 text-right">{i + 1}</span>
                    <span className="flex-1 text-gray-700 dark:text-gray-300 font-mono text-xs">{a.action}</span>
                    <span className="text-gray-500 font-medium">{a.count}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
