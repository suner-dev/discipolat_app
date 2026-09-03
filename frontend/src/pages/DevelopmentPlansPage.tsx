import { useQuery } from '@tanstack/react-query';
import api, { getErrorMessage } from '@/lib/api';
import {
  Layers, Target, TrendingUp, Users, Calendar, BarChart3, Loader2,
} from 'lucide-react';
import { useParams } from 'react-router-dom';

interface DevPlan {
  id: string;
  title: string;
  description: string;
  departmentId?: string;
  departmentName?: string;
  objectives: number;
  completedObjectives: number;
  startDate: string;
  endDate?: string;
  status: string;
}

interface DevPlanStats {
  totalPlans: number;
  activePlans: number;
  totalObjectives: number;
  completedObjectives: number;
  completionRate: number;
}

export default function DevelopmentPlansPage() {
  const { membreId, deptId } = useParams<{ membreId?: string; deptId?: string }>();

  const { data: plans = [], isLoading } = useQuery({
    queryKey: ['development-plans', deptId],
    queryFn: async () => {
      const url = deptId ? `/development-plans/by-department/${deptId}` : '/development-plans';
      const res = await api.get(url);
      return (res.data.content || res.data || []) as DevPlan[];
    },
  });

  const { data: stats } = useQuery({
    queryKey: ['development-plans', 'stats', membreId],
    queryFn: async () => {
      const res = await api.get(`/development-plans/stats/${membreId}`);
      return res.data as DevPlanStats;
    },
    enabled: !!membreId,
  });

  const statusColor = (s: string) => {
    switch (s) {
      case 'ACTIVE': return 'badge-success';
      case 'COMPLETED': return 'badge-info';
      case 'ARCHIVED': return 'badge-gray';
      default: return 'badge-warning';
    }
  };

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Layers className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Plans de développement</h1>
          </div>
          <p className="page-subtitle">Plans de croissance personnelle et objectifs</p>
        </div>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
          {[
            { label: 'Plans totaux', value: stats.totalPlans, icon: Layers, color: 'from-primary-500 to-primary-600' },
            { label: 'Plans actifs', value: stats.activePlans, icon: Target, color: 'from-green-500 to-emerald-500' },
            { label: 'Objectifs', value: stats.totalObjectives, icon: BarChart3, color: 'from-blue-500 to-indigo-500' },
            { label: 'Taux complétion', value: `${stats.completionRate}%`, icon: TrendingUp, color: 'from-purple-500 to-violet-500' },
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
      )}

      {/* Plans list */}
      {plans.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Layers className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucun plan de développement.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {plans.map((plan) => {
            const progress = plan.objectives > 0 ? Math.round((plan.completedObjectives / plan.objectives) * 100) : 0;
            return (
              <div key={plan.id} className="glass-card px-5 py-4">
                <div className="flex items-start justify-between mb-2">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{plan.title}</h3>
                      <span className={`badge text-[10px] ${statusColor(plan.status)}`}>{plan.status}</span>
                    </div>
                    <p className="text-xs text-gray-400 line-clamp-2">{plan.description}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4 mt-3 text-[10px] text-gray-400">
                  {plan.departmentName && (
                    <span className="flex items-center gap-1"><Users className="w-3 h-3" /> {plan.departmentName}</span>
                  )}
                  <span className="flex items-center gap-1"><Calendar className="w-3 h-3" /> {new Date(plan.startDate).toLocaleDateString('fr-FR')}</span>
                  {plan.endDate && <span>→ {new Date(plan.endDate).toLocaleDateString('fr-FR')}</span>}
                  <span>{plan.completedObjectives}/{plan.objectives} objectifs</span>
                </div>
                <div className="w-full h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden mt-3">
                  <div className="h-full rounded-full bg-gradient-to-r from-primary-500 to-primary-600" style={{ width: `${progress}%` }} />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
