import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import type { DashboardKPI, Alert } from '@/types';
import {
  Heart,
  Users,
  Building2,
  FileText,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Activity,
  Bell,
  UserCheck,
  BarChart3,
  FileDown,
  Loader2,
  Sparkles,
  ChevronRight,
  Star,
  ThumbsUp,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
  Area,
  AreaChart,
} from 'recharts';

const DEFAULT_KPI: DashboardKPI = {
  tauxPresenceGlobal: 0,
  tauxPresenceNouveauxArrivants: 0,
  tauxPresenceNouveauxConvertis: 0,
  totalAmes: 0,
  totalFaiseurs: 0,
  totalFamilles: 0,
  totalDepartements: 0,
  totalSorties: 0,
  totalMaintenus: 0,
  suivisParallelesActifs: 0,
  alertesActives: 0,
  rapportsSoumis: 0,
  rapportsEnAttente: 0,
  famillesARisque: 0,
  tendancePresence: 0,
};

const COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899'];

// Animation helper: staggered delay per card index
const cardDelay = (i: number) => ({ animationDelay: `${i * 80}ms` });

// Welcome messages by hour
const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

export default function DashboardPage() {
  const { user, hasRole } = useAuth();
  const { exportReport, isExporting } = useExportReport();

  const isPasteurOrAdmin = hasRole('PASTEUR') || hasRole('ADMIN');

  const { data: kpi, isLoading: kpiLoading } = useQuery({
    queryKey: ['dashboard', 'kpi'],
    queryFn: async () => {
      const res = await api.get('/dashboard/kpi');
      return res.data as DashboardKPI;
    },
    enabled: isPasteurOrAdmin,
  });

  const { data: myMetrics, isLoading: myMetricsLoading } = useQuery({
    queryKey: ['dashboard', 'my-metrics'],
    queryFn: async () => {
      const res = await api.get('/dashboard/my-metrics');
      return res.data as Record<string, unknown>;
    },
    enabled: !isPasteurOrAdmin,
  });

  const { data: myEvalScores } = useQuery({
    queryKey: ['evaluations', 'me'],
    queryFn: async () => {
      const res = await api.get('/evaluations/me');
      return res.data as { statistiques: Record<string, { moyenne: number | null; total: number }> };
    },
    enabled: !isPasteurOrAdmin,
  });

  const { data: alerts } = useQuery({
    queryKey: ['alerts', 'active'],
    queryFn: async () => {
      const res = await api.get('/alerts?size=5&sort=dateDeclenchement,desc');
      return res.data.content as Alert[];
    },
  });

  const kpiData = kpi || DEFAULT_KPI;

  const statCards = [
    {
      label: 'Âmes suivies',
      value: kpiData.totalAmes,
      icon: Heart,
      gradient: 'from-rose-500 to-pink-500',
      link: '/souls',
    },
    {
      label: 'Faiseurs actifs',
      value: kpiData.totalFaiseurs,
      icon: UserCheck,
      gradient: 'from-blue-500 to-indigo-500',
      link: '/users',
    },
    {
      label: 'Familles',
      value: kpiData.totalFamilles,
      icon: Users,
      gradient: 'from-violet-500 to-purple-500',
      link: '/families',
    },
    {
      label: 'Départements',
      value: kpiData.totalDepartements,
      icon: Building2,
      gradient: 'from-amber-500 to-orange-500',
      link: '/departments',
    },
    {
      label: 'Taux de présence',
      value: `${kpiData.tauxPresenceGlobal.toFixed(1)}%`,
      icon: TrendingUp,
      gradient: kpiData.tauxPresenceGlobal >= 70 ? 'from-green-500 to-emerald-500' : 'from-red-500 to-rose-500',
      trend: kpiData.tendancePresence,
    },
    {
      label: 'Alertes actives',
      value: kpiData.alertesActives,
      icon: Bell,
      gradient: kpiData.alertesActives > 0 ? 'from-red-500 to-rose-500' : 'from-green-500 to-emerald-500',
      link: '/alerts',
    },
    {
      label: 'Rapports soumis',
      value: kpiData.rapportsSoumis,
      icon: FileText,
      gradient: 'from-emerald-500 to-teal-500',
      link: '/reports',
    },
    {
      label: 'Familles à risque',
      value: kpiData.famillesARisque,
      icon: AlertTriangle,
      gradient: kpiData.famillesARisque > 0 ? 'from-orange-500 to-red-500' : 'from-green-500 to-emerald-500',
    },
  ];

  const presenceData = [
    { name: 'Nouveaux arrivants', taux: kpiData.tauxPresenceNouveauxArrivants },
    { name: 'Nouveaux convertis', taux: kpiData.tauxPresenceNouveauxConvertis },
  ];

  const movementData = [
    { name: 'Sorties', value: kpiData.totalSorties, color: '#ef4444' },
    { name: 'Maintenus', value: kpiData.totalMaintenus, color: '#22c55e' },
    { name: 'Suivis parallèles', value: kpiData.suivisParallelesActifs, color: '#3b82f6' },
  ];

  const weeklyTrendData = [
    { semaine: 'S-3', taux: kpiData.tauxPresenceGlobal - 5 },
    { semaine: 'S-2', taux: kpiData.tauxPresenceGlobal - 2 },
    { semaine: 'S-1', taux: kpiData.tauxPresenceGlobal },
    { semaine: 'Cette S', taux: kpiData.tauxPresenceGlobal },
  ];

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Sparkles className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              {getGreeting()}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            Tableau de bord{' '}
            <span className="text-gradient font-display">Discipolat</span>
          </h1>
          <p className="page-subtitle">
            Vision consolidée de l'état du discipolat · {new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        </div>
        {isPasteurOrAdmin && (
          <button
            onClick={() => exportReport({ endpoint: '/reports/export/consolidated-pdf', filename: `rapport-consolide-${new Date().toISOString().split('T')[0]}.html` })}
            disabled={isExporting}
            className="btn-glow btn-sm animate-scale-in"
          >
            {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />}
            {isExporting ? 'Génération...' : 'Exporter PDF'}
          </button>
        )}
      </div>

      {/* KPI Cards */}
      {kpiLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[...Array(8)].map((_, i) => (
            <div key={i} className="glass-card p-5 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
              <div className="skeleton h-4 w-24 mb-3 rounded" />
              <div className="skeleton h-8 w-20 rounded" />
              <div className="skeleton h-3 w-16 mt-3 rounded" />
            </div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {statCards.map((stat, i) => {
            const Icon = stat.icon;
            const content = (
              <div
                className="stat-card animate-slide-up"
                style={cardDelay(i)}
              >
                {/* Gradient decorative top bar */}
                <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${stat.gradient} opacity-60`} />

                <div className="flex items-start justify-between mb-3">
                  <span className="stat-label">{stat.label}</span>
                  <div className={`p-2.5 rounded-xl bg-gradient-to-br ${stat.gradient} text-white shadow-lg`}>
                    <Icon className="w-4 h-4" />
                  </div>
                </div>

                <div className="flex items-baseline gap-2">
                  <span className="stat-value animate-count-up">{stat.value}</span>
                  {'trend' in stat && stat.trend !== undefined && (
                    <span className={`inline-flex items-center gap-0.5 text-xs font-semibold px-1.5 py-0.5 rounded-full ${
                      stat.trend >= 0
                        ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400'
                        : 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400'
                    }`}>
                      {stat.trend >= 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                      {Math.abs(stat.trend).toFixed(1)}%
                    </span>
                  )}
                </div>

                {/* Mini sparkline indicator */}
                <div className="flex items-center gap-1.5 mt-3">
                  <span className={`w-1.5 h-1.5 rounded-full ${stat.trend !== undefined && stat.trend >= 0 ? 'bg-green-500' : 'bg-gray-300 dark:bg-gray-600'}`} />
                  <span className="text-[10px] text-gray-400 dark:text-gray-500">
                    {'trend' in stat ? 'vs semaine dernière' : 'En temps réel'}
                  </span>
                </div>
              </div>
            );

            return stat.link ? (
              <Link key={stat.label} to={stat.link} className="block">
                {content}
              </Link>
            ) : (
              <div key={stat.label}>{content}</div>
            );
          })}
        </div>
      )}

      {/* Charts section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        {/* Tendance hebdomadaire */}
        <div className="glass-card p-6 animate-slide-up">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Tendance de présence
            </h3>
            <span className="badge-info text-[10px]">Hebdomadaire</span>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={weeklyTrendData}>
                <defs>
                  <linearGradient id="colorTaux" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#22c55e" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" />
                <XAxis dataKey="semaine" tick={{ fontSize: 12 }} stroke="rgba(128,128,128,0.3)" />
                <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} unit="%" stroke="rgba(128,128,128,0.3)" />
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'rgba(255,255,255,0.85)',
                    backdropFilter: 'blur(12px)',
                    borderRadius: '12px',
                    border: '1px solid rgba(255,255,255,0.2)',
                    boxShadow: '0 8px 32px rgba(0,0,0,0.1)',
                  }}
                  formatter={(value: number) => [`${value.toFixed(1)}%`, 'Taux de présence']}
                />
                <Area type="monotone" dataKey="taux" stroke="#22c55e" fill="url(#colorTaux)" strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Présence par type */}
        <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '100ms' }}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Présence par type de disciple
            </h3>
            <span className="badge-info text-[10px]">Comparaison</span>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={presenceData} barCategoryGap="40%">
                <defs>
                  {presenceData.map((_, index) => (
                    <linearGradient key={index} id={`barGrad${index}`} x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor={COLORS[index]} stopOpacity={0.9} />
                      <stop offset="100%" stopColor={COLORS[index]} stopOpacity={0.5} />
                    </linearGradient>
                  ))}
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} stroke="rgba(128,128,128,0.3)" />
                <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} unit="%" stroke="rgba(128,128,128,0.3)" />
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'rgba(255,255,255,0.85)',
                    backdropFilter: 'blur(12px)',
                    borderRadius: '12px',
                    border: '1px solid rgba(255,255,255,0.2)',
                  }}
                  formatter={(value: number) => [`${value.toFixed(1)}%`, 'Taux de présence']}
                />
                <Bar dataKey="taux" radius={[8, 8, 0, 0]} maxBarSize={60}>
                  {presenceData.map((_, index) => (
                    <Cell key={index} fill={`url(#barGrad${index})`} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Mouvements du suivi - Donut */}
        <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Mouvements du suivi
            </h3>
            <span className="badge-info text-[10px]">Période en cours</span>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={movementData}
                  cx="50%"
                  cy="50%"
                  innerRadius={65}
                  outerRadius={100}
                  paddingAngle={4}
                  dataKey="value"
                  startAngle={90}
                  endAngle={-270}
                >
                  {movementData.map((entry, index) => (
                    <Cell key={index} fill={entry.color}>
                      <animate attributeName="opacity" from="0" to="1" dur="0.5s" begin={`${index * 0.15}s`} />
                    </Cell>
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: 'rgba(255,255,255,0.85)',
                    backdropFilter: 'blur(12px)',
                    borderRadius: '12px',
                    border: '1px solid rgba(255,255,255,0.2)',
                  }}
                />
                <Legend
                  formatter={(value: string) => (
                    <span className="text-sm text-gray-600 dark:text-gray-400">{value}</span>
                  )}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Alertes récentes */}
        <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '300ms' }}>
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
              Alertes récentes
            </h3>
            <Link to="/alerts" className="text-xs font-medium text-primary-600 hover:text-primary-700 flex items-center gap-1">
              Voir tout <ChevronRight className="w-3 h-3" />
            </Link>
          </div>

          {alerts && alerts.length > 0 ? (
            <div className="space-y-2">
              {alerts.slice(0, 5).map((alert, i) => (
                <div
                  key={alert.id}
                  className="flex items-start gap-3 p-3 rounded-xl bg-amber-50/70 dark:bg-amber-900/10 border border-amber-200/50 dark:border-amber-800/30 animate-slide-up"
                  style={{ animationDelay: `${i * 50}ms` }}
                >
                  <div className="p-1.5 rounded-lg bg-amber-100 dark:bg-amber-900/30">
                    <AlertTriangle className="w-4 h-4 text-amber-600 dark:text-amber-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-amber-800 dark:text-amber-300 truncate">
                      {alert.message}
                    </p>
                    <p className="text-xs text-amber-600 dark:text-amber-400 mt-0.5">
                      {new Date(alert.dateDeclenchement).toLocaleDateString('fr-FR', {
                        day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
                      })}
                    </p>
                  </div>
                  <span className="glow-dot-red flex-shrink-0 mt-1.5" />
                </div>
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center py-8 text-center">
              <div className="p-3 rounded-full bg-green-100 dark:bg-green-900/20 mb-3">
                <Bell className="w-6 h-6 text-green-500" />
              </div>
              <p className="text-sm font-medium text-gray-600 dark:text-gray-400">Aucune alerte active</p>
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">Tout est sous contrôle</p>
            </div>
          )}
        </div>
      </div>

      {/* Role-aware dashboard for non-PASTEUR roles */}
      {!isPasteurOrAdmin && (
        <div className="glass-card p-6 animate-scale-in">
          <div className="flex items-center gap-3 mb-6">
            <div className="p-2.5 rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 text-white shadow-lg">
              <BarChart3 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                {hasRole('RESPONSABLE') ? 'Vue Responsable' : 'Mon tableau de bord'}
              </h3>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                {hasRole('RESPONSABLE')
                  ? 'Aperçu de vos départements et familles'
                  : 'Vos statistiques personnelles de suivi'}
              </p>
            </div>
          </div>

          {myMetricsLoading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="glass-card p-5 animate-fade-in" style={{ animationDelay: `${i * 50}ms` }}>
                  <div className="skeleton h-4 w-24 mb-3 rounded" />
                  <div className="skeleton h-8 w-20 rounded" />
                </div>
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {hasRole('FAISEUR') && (
                <>
                  <div className="stat-card">
                    <span className="stat-label">Mes âmes</span>
                    <span className="stat-value">{(myMetrics as any)?.totalAmes || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Âmes actives</span>
                    <span className="stat-value">{(myMetrics as any)?.amesActives || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">En intégration</span>
                    <span className="stat-value text-amber-500">{(myMetrics as any)?.amesEnIntegration || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">En veille</span>
                    <span className="stat-value text-gray-400">{(myMetrics as any)?.amesEnVeille || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Décrochées</span>
                    <span className={`stat-value ${((myMetrics as any)?.amesDecrochees || 0) > 0 ? 'text-red-500' : 'text-gray-400'}`}>
                      {(myMetrics as any)?.amesDecrochees || 0}
                    </span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Rapport soumis</span>
                    <span className={`stat-value ${(myMetrics as any)?.rapportSoumisCetteSemaine ? 'text-green-500' : 'text-amber-500'}`}>
                      {(myMetrics as any)?.rapportSoumisCetteSemaine ? 'Oui' : 'Non'}
                    </span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Rapports total</span>
                    <span className="stat-value">{(myMetrics as any)?.totalRapportsSoumis || 0}</span>
                  </div>
                  {/* Évaluation score */}
                  <div className="stat-card">
                    <div className="flex items-center gap-2 mb-2">
                      <ThumbsUp className="w-4 h-4 text-amber-500" />
                      <span className="stat-label">Évaluation</span>
                    </div>
                    {myEvalScores && Object.keys(myEvalScores.statistiques).length > 0 ? (
                      Object.entries(myEvalScores.statistiques).map(([cat, s]) => (
                        <div key={cat} className="flex items-center justify-between text-xs mb-1">
                          <span className="text-gray-400">{cat === 'RESPONSABLE' ? 'Resp.' : cat === 'CHEF_FAMILLE' ? 'Chef' : 'Faiseur'}</span>
                          <div className="flex items-center gap-1">
                            <span className="font-semibold text-gray-900 dark:text-gray-100">{s.moyenne ?? '—'}</span>
                            {s.moyenne != null && [1,2,3,4,5].map(i => (
                              <Star key={i} className={`w-2.5 h-2.5 ${i <= Math.round(s.moyenne!) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                            ))}
                          </div>
                        </div>
                      ))
                    ) : (
                      <p className="text-xs text-gray-400">Aucune évaluation</p>
                    )}
                  </div>
                </>
              )}
              {hasRole('RESPONSABLE') && (
                <>
                  <div className="stat-card">
                    <span className="stat-label">Départements</span>
                    <span className="stat-value">{(myMetrics as any)?.totalDepartements || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Familles</span>
                    <span className="stat-value">{(myMetrics as any)?.totalFamilles || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Âmes total</span>
                    <span className="stat-value">{(myMetrics as any)?.totalAmes || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Âmes actives</span>
                    <span className="stat-value">{(myMetrics as any)?.amesActives || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Faiseurs</span>
                    <span className="stat-value">{(myMetrics as any)?.totalFaiseurs || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Rapports soumis</span>
                    <span className="stat-value">{(myMetrics as any)?.rapportsSoumis || 0}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-label">Rapports attendus</span>
                    <span className="stat-value">{(myMetrics as any)?.rapportsAttendus || 0}</span>
                  </div>
                </>
              )}
            </div>
          )}

          <div className="divider-glow my-5" />
          <p className="text-xs text-gray-400 dark:text-gray-500 text-center">
            Utilisez la navigation latérale pour accéder à vos sections
          </p>
        </div>
      )}
    </div>
  );
}
