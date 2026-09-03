import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Activity, AlertTriangle, TrendingUp, TrendingDown, Minus, Zap, RefreshCw, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import api, { getErrorMessage } from '@/lib/api';
import SkeletonLoader from '@/components/shared/SkeletonLoader';
import EmptyState from '@/components/shared/EmptyState';

interface KpiData {
  id: string;
  name: string;
  category: string;
  currentValue?: number;
  previousValue?: number;
  percentageChange?: number;
  trend: string;
  unit?: string;
  narrative?: string;
  isAlert?: boolean;
}

interface DashboardData {
  totalKpis: number;
  alertsCount: number;
  categories: string[];
}

export default function IntelligenceCenterPage() {
  const queryClient = useQueryClient();
  const [selectedCat, setSelectedCat] = useState<string>('ALL');

  const { data: kpis = [], isLoading, error, refetch } = useQuery({
    queryKey: ['intelligence-kpis'],
    queryFn: async () => (await api.get('/intelligence')).data as KpiData[],
    retry: false,
  });

  const { data: dashboard } = useQuery({
    queryKey: ['intelligence-dashboard'],
    queryFn: async () => (await api.get('/intelligence/dashboard')).data as DashboardData,
  });

  const { data: alerts = [] } = useQuery({
    queryKey: ['intelligence-alerts'],
    queryFn: async () => (await api.get('/intelligence/alerts')).data as KpiData[],
  });

  const initializeMutation = useMutation({
    mutationFn: async () => api.post('/intelligence/initialize'),
    onSuccess: () => { toast.success('KPIs initialisés'); queryClient.invalidateQueries({ queryKey: ['intelligence-kpis'] }); },
    onError: (e: unknown) => toast.error(getErrorMessage(e)),
  });

  const categories = ['ALL', ...Array.from(new Set(kpis.map(k => k.category)))];
  const filtered = selectedCat === 'ALL' ? kpis : kpis.filter(k => k.category === selectedCat);

  const trendIcon = (trend: string) =>
    trend === 'UP' ? <TrendingUp className="w-4 h-4 text-green-400" />
    : trend === 'DOWN' ? <TrendingDown className="w-4 h-4 text-red-400" />
    : <Minus className="w-4 h-4 text-gray-400" />;

  if (isLoading) return <SkeletonLoader lines={6} variant="card" />;
  if (error) return <div className="p-6 text-red-400">{getErrorMessage(error)}</div>;

  return (
    <div className="space-y-6 p-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Activity className="text-cyan-400" /> Centre d'Intelligence (50+ KPIs)
        </h1>
        <div className="flex gap-2">
          <button onClick={() => initializeMutation.mutate()} disabled={initializeMutation.isPending}
            className="flex items-center gap-2 px-3 py-2 rounded-xl bg-cyan-600 text-white text-sm hover:bg-cyan-700 transition">
            {initializeMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Zap className="w-4 h-4" />}
            Initialiser
          </button>
          <button onClick={() => refetch()} className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/5 text-gray-400 text-sm hover:bg-white/10 transition">
            <RefreshCw className="w-4 h-4" /> Actualiser
          </button>
        </div>
      </div>

      {alerts.length > 0 && (
        <div className="bg-red-500/10 border border-red-500/30 rounded-xl p-4 flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-red-400" />
          <span className="text-red-300 text-sm font-medium">{alerts.length} alerte{alerts.length > 1 ? 's' : ''} active{alerts.length > 1 ? 's' : ''}</span>
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        {categories.map(cat => (
          <button key={cat} onClick={() => setSelectedCat(cat)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${selectedCat === cat ? 'bg-blue-600 text-white' : 'bg-white/5 text-gray-400 hover:bg-white/10'}`}>
            {cat === 'ALL' ? `Tout (${kpis.length})` : `${cat} (${kpis.filter(k => k.category === cat).length})`}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          icon={<Zap className="w-6 h-6 text-cyan-400" />}
          title="Aucun KPI"
          message="Initialisez les KPIs pour commencer le suivi."
          action={{ label: 'Initialiser', onClick: () => initializeMutation.mutate() }}
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {filtered.map(kpi => (
            <div key={kpi.id} className={`bg-white/5 backdrop-blur rounded-2xl p-4 border transition hover:scale-[1.02] ${kpi.isAlert ? 'border-red-500/40' : 'border-white/10 hover:border-white/20'}`}>
              <div className="flex items-start justify-between mb-2">
                <span className="text-xs text-gray-400 font-medium uppercase tracking-wide">{kpi.category}</span>
                {trendIcon(kpi.trend)}
              </div>
              <div className="text-2xl font-bold text-white mb-1">
                {kpi.currentValue?.toLocaleString() ?? '—'}
                <span className="text-sm font-normal text-gray-400 ml-1">{kpi.unit || ''}</span>
              </div>
              <div className="text-sm text-gray-300 mb-2">{kpi.name}</div>
              <div className={`text-xs ${(kpi.percentageChange ?? 0) > 0 ? 'text-green-400' : (kpi.percentageChange ?? 0) < 0 ? 'text-red-400' : 'text-gray-400'}`}>
                {(kpi.percentageChange ?? 0) > 0 ? '+' : ''}{kpi.percentageChange ?? 0}%
              </div>
              {kpi.isAlert && <div className="mt-2 text-xs text-red-400 flex items-center gap-1"><AlertTriangle className="w-3 h-3" /> Alerte</div>}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
