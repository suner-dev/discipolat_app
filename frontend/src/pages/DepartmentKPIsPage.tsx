import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  Target, BarChart3, TrendingUp, TrendingDown, Users, Award, Loader2,
} from 'lucide-react';

interface DepartmentKpi {
  id: string;
  name: string;
  departmentId: string;
  departmentName: string;
  currentValue: number;
  targetValue: number;
  unit: string;
  trend: 'UP' | 'DOWN' | 'STABLE';
  lastUpdated: string;
}

export default function DepartmentKpisPage() {
  const { data: kpis = [], isLoading } = useQuery({
    queryKey: ['department-kpis'],
    queryFn: async () => {
      const res = await api.get('/department-kpis');
      return (res.data.content || res.data || []) as DepartmentKpi[];
    },
  });

  const grouped = kpis.reduce<Record<string, DepartmentKpi[]>>((acc, kpi) => {
    const key = kpi.departmentName || 'Autre';
    if (!acc[key]) acc[key] = [];
    acc[key].push(kpi);
    return acc;
  }, {});

  if (isLoading) return <div className="min-h-[40vh] flex items-center justify-center"><Loader2 className="w-6 h-6 animate-spin text-gray-400" /></div>;

  return (
    <div className="page-container max-w-6xl">
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Target className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">KPIs par département</h1>
          </div>
          <p className="page-subtitle">Indicateurs de performance clés par département</p>
        </div>
      </div>

      {kpis.length === 0 ? (
        <div className="glass-card p-10 text-center">
          <Target className="w-10 h-10 text-gray-300 mb-3 mx-auto" />
          <p className="text-gray-500 font-medium">Aucun KPI configuré.</p>
        </div>
      ) : (
        <div className="space-y-6">
          {Object.entries(grouped).map(([deptName, deptKpis]) => (
            <div key={deptName} className="animate-slide-up">
              <div className="flex items-center gap-2 mb-3">
                <Users className="w-4 h-4 text-primary-500" />
                <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100">{deptName}</h3>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {deptKpis.map((kpi) => {
                  const progress = kpi.targetValue > 0 ? Math.min((kpi.currentValue / kpi.targetValue) * 100, 100) : 0;
                  const TrendIcon = kpi.trend === 'UP' ? TrendingUp : kpi.trend === 'DOWN' ? TrendingDown : null;
                  return (
                    <div key={kpi.id} className="glass-card px-5 py-4">
                      <div className="flex items-start justify-between mb-2">
                        <h4 className="text-xs font-semibold text-gray-700 dark:text-gray-300">{kpi.name}</h4>
                        {TrendIcon && (
                          <TrendIcon className={`w-4 h-4 ${kpi.trend === 'UP' ? 'text-green-500' : 'text-red-500'}`} />
                        )}
                      </div>
                      <div className="flex items-baseline gap-1 mb-2">
                        <span className="text-xl font-bold text-gray-900 dark:text-gray-100">{kpi.currentValue}</span>
                        <span className="text-xs text-gray-400">/ {kpi.targetValue} {kpi.unit}</span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-gray-200 dark:bg-gray-700 overflow-hidden">
                        <div
                          className={`h-full rounded-full ${progress >= 80 ? 'bg-green-500' : progress >= 50 ? 'bg-yellow-500' : 'bg-red-500'}`}
                          style={{ width: `${progress}%` }}
                        />
                      </div>
                      <p className="text-[10px] text-gray-400 mt-2">
                        Mis à jour le {new Date(kpi.lastUpdated).toLocaleDateString('fr-FR')}
                      </p>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
