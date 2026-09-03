import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  LayoutDashboard, Users, AlertTriangle, TrendingUp, Activity,
  Heart, BarChart3, Loader2, Clock,
} from 'lucide-react';

interface DashboardSummary {
  totalMembers: number;
  activeFamilies: number;
  totalSouls: number;
  recentActivity: number;
  upcomingEvents: number;
  pendingRequests: number;
}

interface FamilyRiskSummary {
  totalFamilies: number;
  normal: number;
  underSurveillance: number;
  atRisk: number;
  families: { id: string; name: string; riskLevel: string; reason?: string }[];
}

export default function DashboardSummaryPage() {
  const { data: summary, isLoading: summaryLoading } = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: async () => {
      const res = await api.get('/dashboard/summary');
      return res.data as DashboardSummary;
    },
  });

  const { data: risk, isLoading: riskLoading } = useQuery({
    queryKey: ['dashboard', 'family-risk'],
    queryFn: async () => {
      const res = await api.get('/dashboard/family-risk');
      return res.data as FamilyRiskSummary;
    },
  });

  const isLoading = summaryLoading || riskLoading;
  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <LayoutDashboard className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Résumé du tableau de bord</h1>
          </div>
          <p className="page-subtitle">Vue d'ensemble rapide de l'activité de l'organisation</p>
        </div>
      </div>

      {/* Summary stats */}
      {summary && (
        <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
          {[
            { label: 'Membres', value: summary.totalMembers, icon: Users, color: 'from-primary-500 to-primary-600' },
            { label: 'Familles actives', value: summary.activeFamilies, icon: Heart, color: 'from-green-500 to-emerald-500' },
            { label: 'Âmes suivies', value: summary.totalSouls, icon: Users, color: 'from-blue-500 to-indigo-500' },
            { label: 'Activité récente', value: summary.recentActivity, icon: Activity, color: 'from-purple-500 to-violet-500' },
            { label: 'Événements à venir', value: summary.upcomingEvents, icon: Clock, color: 'from-amber-500 to-orange-500' },
            { label: 'Demandes en attente', value: summary.pendingRequests, icon: AlertTriangle, color: 'from-red-500 to-rose-500' },
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

      {/* Family risk */}
      {risk && (
        <div className="space-y-4 animate-slide-up">
          <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-500" />
            Risques familiaux
          </h3>

          <div className="grid grid-cols-3 gap-3 mb-4">
            {[
              { label: 'Normales', value: risk.normal, color: 'bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-400' },
              { label: 'Sous surveillance', value: risk.underSurveillance, color: 'bg-yellow-100 dark:bg-yellow-900/20 text-yellow-700 dark:text-yellow-400' },
              { label: 'À risque', value: risk.atRisk, color: 'bg-red-100 dark:bg-red-900/20 text-red-700 dark:text-red-400' },
            ].map((stat) => (
              <div key={stat.label} className={`p-3 rounded-xl ${stat.color} text-center`}>
                <p className="text-2xl font-bold">{stat.value}</p>
                <p className="text-[10px]">{stat.label}</p>
              </div>
            ))}
          </div>

          {risk.families.length > 0 ? (
            <div className="space-y-2">
              {risk.families.map((f) => {
                const riskColor = f.riskLevel === 'A_RISQUE' ? 'border-red-500' : f.riskLevel === 'SOUS_SURVEILLANCE' ? 'border-yellow-500' : 'border-green-500';
                return (
                  <div key={f.id} className={`glass-card px-5 py-3 border-l-4 ${riskColor}`}>
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{f.name}</p>
                        {f.reason && <p className="text-[10px] text-gray-400 mt-0.5">{f.reason}</p>}
                      </div>
                      <span className={`badge text-[10px] ${f.riskLevel === 'A_RISQUE' ? 'badge-error' : f.riskLevel === 'SOUS_SURVEILLANCE' ? 'badge-warning' : 'badge-success'}`}>
                        {f.riskLevel}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="glass-card p-6 text-center">
              <Heart className="w-8 h-8 text-green-400 mx-auto mb-2" />
              <p className="text-sm text-gray-500">Toutes les familles sont en bon état.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
