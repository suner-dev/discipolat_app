import { useRef, useCallback, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import type { PasteurDashboardData, PresenceTrendData, AuditRecentActivity, PasteurKpis } from '@/types';
import {
  Heart, Users, Building2, TrendingUp, Bell, UserCheck,
  FileDown, Loader2, ChevronRight, Church, Calendar,
  Shield, Star, Search, UserPlus, UserX, Clock,
  CheckCircle, XCircle, ArrowLeftRight, History, Flame,
  Zap, Target, UserCog, CalendarClock, AlertOctagon,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell,
  Area, AreaChart,
} from 'recharts';
import { useState } from 'react';

const COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6'];

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

interface Props {
  onNavigateToTab: (tab: string) => void;
}

export default function PasteurDashboardTab({ onNavigateToTab }: Props) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { exportReport, isExporting } = useExportReport();
  const [searchQuery, setSearchQuery] = useState('');
  const searchTimerRef = useRef<NodeJS.Timeout | null>(null);

  const handleSearchChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchQuery(value);
    if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    if (value.length >= 2) {
      searchTimerRef.current = setTimeout(() => {
        navigate(`/search?q=${encodeURIComponent(value)}`);
      }, 400);
    }
  }, [navigate]);

  useEffect(() => {
    return () => { if (searchTimerRef.current) clearTimeout(searchTimerRef.current); };
  }, []);

  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard', 'pasteur'],
    queryFn: async () => {
      const res = await api.get('/dashboard/pasteur');
      return res.data as PasteurDashboardData;
    },
  });

  const { data: presenceTrend } = useQuery({
    queryKey: ['dashboard', 'pasteur', 'presence-trend'],
    queryFn: async () => {
      const res = await api.get('/dashboard/pasteur/presence-trend', { params: { mois: 12 } });
      return res.data as PresenceTrendData;
    },
  });

  const { data: recentActivity } = useQuery({
    queryKey: ['audit', 'recent'],
    queryFn: async () => {
      const res = await api.get('/audit/recent', { params: { limit: 15 } });
      return res.data as AuditRecentActivity[];
    },
  });

  const { data: alertStats } = useQuery({
    queryKey: ['alerts', 'stats'],
    queryFn: async () => {
      const res = await api.get('/alerts/stats');
      return res.data as { actives: number; traitees: number; resolues: number; total: number };
    },
  });

  const { data: kpis } = useQuery({
    queryKey: ['dashboard', 'pasteur', 'kpis'],
    queryFn: async () => {
      const res = await api.get('/dashboard/pasteur/kpis');
      return res.data as PasteurKpis;
    },
  });

  const croissance = dashboard?.croissance ?? {} as PasteurDashboardData['croissance'];
  const departements = dashboard?.departements ?? [];
  const familles = dashboard?.familles ?? [];
  const faiseurs = dashboard?.faiseurs ?? [];
  const presences = dashboard?.presences ?? {} as PasteurDashboardData['presences'];
  const rapports = dashboard?.rapports ?? {} as PasteurDashboardData['rapports'];
  const transfertsEnAttente = dashboard?.transfertsEnAttente ?? [];

  // Navigation rapide vers chaque module — cliquable vers les tabs internes
  const moduleNav = [
    { label: 'Âmes', tab: 'ames', icon: Heart, color: 'from-rose-500 to-pink-500', description: 'Liste, CRUD, corbeille' },
    { label: 'Familles', tab: 'familles', icon: Users, color: 'from-primary-500 to-primary-600', description: 'CRUD, risques, historique' },
    { label: 'Utilisateurs', tab: 'utilisateurs', icon: UserCog, color: 'from-blue-500 to-indigo-500', description: 'Gestion des comptes' },
    { label: 'Départements', tab: 'departements', icon: Building2, color: 'from-amber-500 to-orange-500', description: 'Équipes, tâches, docs' },
    { label: 'Rapports', tab: 'rapports', icon: Zap, color: 'from-emerald-500 to-teal-500', description: 'Hebdo, famille, export' },
    { label: 'Alertes', tab: 'alertes', icon: Bell, color: 'from-red-500 to-rose-500', description: 'Gestion et résolution' },
    { label: 'Transferts', tab: 'transferts', icon: ArrowLeftRight, color: 'from-violet-500 to-purple-500', description: 'Workflow, validation' },
    { label: 'Visites', tab: 'visites', icon: Calendar, color: 'from-teal-500 to-cyan-500', description: 'Planification, compte-rendu' },
    { label: 'Audit', tab: 'audit', icon: Shield, color: 'from-gray-500 to-slate-600', description: 'Journal, export CSV' },
    { label: 'CRM Faiseur', tab: 'crm', icon: Target, color: 'from-pink-500 to-rose-500', description: 'Suivi disciples, KPIs' },
    { label: 'Prières', tab: 'prieres', icon: Church, color: 'from-indigo-500 to-blue-500', description: 'Demandes, espaces' },
    { label: 'Événements', tab: 'evenements', icon: CalendarClock, color: 'from-orange-500 to-red-500', description: 'Calendrier, inscriptions' },
    { label: 'Évaluations', tab: 'evaluations', icon: Star, color: 'from-yellow-500 to-amber-500', description: 'Scores, comparaison' },
  ];

  const growthCards = [
    { label: 'Âmes totales', value: croissance.totalAmes ?? 0, icon: Heart, color: 'from-rose-500 to-pink-500', tab: 'ames' },
    { label: 'Nouveaux convertis', value: croissance.nouveauxConvertis ?? 0, icon: UserPlus, color: 'from-green-500 to-emerald-500', tab: 'ames' },
    { label: 'Nouveaux arrivants', value: croissance.nouveauxArrivants ?? 0, icon: UserCheck, color: 'from-blue-500 to-indigo-500', tab: 'ames' },
    { label: 'Actifs', value: croissance.actifs ?? 0, icon: CheckCircle, color: 'from-emerald-500 to-teal-500', tab: 'ames' },
    { label: 'En intégration', value: croissance.enIntegration ?? 0, icon: Clock, color: 'from-amber-500 to-orange-500', tab: 'ames' },
    { label: 'En veille', value: croissance.enVeille ?? 0, icon: UserX, color: 'from-yellow-500 to-amber-500', tab: 'ames' },
    { label: 'Décrochés', value: croissance.decroches ?? 0, icon: XCircle, color: 'from-red-500 to-rose-500', tab: 'ames' },
    { label: 'Taux conversion', value: `${croissance.tauxConversion ?? 0}%`, icon: TrendingUp, color: 'from-violet-500 to-purple-500', tab: 'rapports' },
  ];

  return (
    <>
      {/* Barre de recherche 360° */}
      <div className="glass-card p-4 mb-6 animate-slide-up">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-primary-500/10">
            <Search className="w-5 h-5 text-primary-500" />
          </div>
          <div className="flex-1">
            <input
              type="text"
              placeholder="Rechercher n'importe quel membre, faiseur, famille..."
              value={searchQuery}
              onChange={handleSearchChange}
              className="w-full bg-transparent border-none outline-none text-sm text-gray-900 dark:text-gray-100 placeholder-gray-400"
            />
          </div>
          <span className="text-[10px] text-gray-400">Recherche 360°</span>
        </div>
      </div>

      {/* Navigation rapide modules — cliquable vers les tabs */}
      <div className="mb-6">
        <div className="flex items-center gap-2 mb-4">
          <Zap className="w-4 h-4 text-primary-500" />
          <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
            Accès rapide aux modules
          </h2>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6 gap-3">
          {moduleNav.map((mod, i) => {
            const Icon = mod.icon;
            return (
              <button
                key={mod.tab}
                type="button"
                onClick={() => onNavigateToTab(mod.tab)}
                className="glass-card p-3.5 animate-slide-up hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 group cursor-pointer text-left"
                style={{ animationDelay: `${i * 30}ms` }}
              >
                <div className="flex items-center gap-2.5">
                  <div className={`p-2 rounded-xl bg-gradient-to-br ${mod.color} text-white shadow-md group-hover:shadow-lg transition-shadow`}>
                    <Icon className="w-4 h-4" />
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-semibold text-gray-900 dark:text-gray-100 truncate">{mod.label}</p>
                    <p className="text-[9px] text-gray-400 truncate">{mod.description}</p>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {isLoading ? (
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
        <>
          {/* Growth Cards — cliquables vers le tab Âmes */}
          <div className="mb-6">
            <div className="flex items-center gap-2 mb-4">
              <TrendingUp className="w-4 h-4 text-primary-500" />
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                Croissance de l'Église
              </h2>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              {growthCards.map((stat, i) => {
                const Icon = stat.icon;
                return (
                  <button
                    key={stat.label}
                    type="button"
                    onClick={() => onNavigateToTab(stat.tab)}
                    className="stat-card animate-slide-up text-left cursor-pointer hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
                    style={{ animationDelay: `${i * 60}ms` }}
                    title={`Voir la liste des ${stat.label.toLowerCase()}`}
                  >
                    <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${stat.color} opacity-60`} />
                    <div className="flex items-start justify-between mb-3">
                      <span className="stat-label">{stat.label}</span>
                      <div className={`p-2 rounded-xl bg-gradient-to-br ${stat.color} text-white shadow-lg`}>
                        <Icon className="w-4 h-4" />
                      </div>
                    </div>
                    <span className="stat-value animate-count-up">{stat.value}</span>
                    <span className="text-[10px] text-gray-400 mt-1 block">Cliquer pour explorer</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* KPIs de Santé de l'Église */}
          {kpis?.health && (
            <div className="mb-6">
              <div className="flex items-center gap-2 mb-4">
                <Target className="w-4 h-4 text-primary-500" />
                <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                  Indicateurs de Santé
                </h2>
              </div>
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* Health Score Ring */}
                <div className="glass-card p-6 animate-slide-up">
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Score de santé global</h3>
                  <div className="flex items-center justify-center">
                    <div className="relative w-36 h-36">
                      <svg className="w-36 h-36 -rotate-90" viewBox="0 0 120 120">
                        <circle cx="60" cy="60" r="50" fill="none" stroke="rgba(128,128,128,0.1)" strokeWidth="12" />
                        <circle
                          cx="60" cy="60" r="50" fill="none"
                          stroke={kpis.health.score >= 70 ? '#22c55e' : kpis.health.score >= 40 ? '#f59e0b' : '#ef4444'}
                          strokeWidth="12"
                          strokeDasharray={`${kpis.health.score * 3.14} 314`}
                          strokeLinecap="round"
                          className="transition-all duration-1000"
                        />
                      </svg>
                      <div className="absolute inset-0 flex flex-col items-center justify-center">
                        <span className="text-3xl font-bold text-gray-900 dark:text-gray-100">{kpis.health.score}</span>
                        <span className="text-[10px] text-gray-400">/100</span>
                      </div>
                    </div>
                  </div>
                  <div className="mt-4 space-y-2">
                    {[
                      { label: 'Présence', value: kpis.health.tauxPresence, color: 'text-blue-500' },
                      { label: 'Rapports', value: kpis.health.tauxRapports, color: 'text-amber-500' },
                      { label: 'Fidélisation', value: kpis.health.tauxFidelisation, color: 'text-emerald-500' },
                      { label: 'Croissance', value: kpis.health.tauxCroissance, color: 'text-violet-500' },
                    ].map((item) => (
                      <div key={item.label} className="flex items-center justify-between">
                        <span className="text-xs text-gray-500">{item.label}</span>
                        <span className={`text-xs font-bold ${item.color}`}>{item.value}%</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Growth KPIs */}
                <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '100ms' }}>
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Croissance mensuelle</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="text-center p-3 rounded-xl bg-emerald-50/50 dark:bg-emerald-900/10">
                      <Flame className="w-6 h-6 text-emerald-500 mx-auto mb-2" />
                      <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">{kpis.health.nouveauxMois}</p>
                      <p className="text-[10px] text-gray-400">Nouveaux ce mois</p>
                    </div>
                    <div className="text-center p-3 rounded-xl bg-blue-50/50 dark:bg-blue-900/10">
                      <TrendingUp className="w-6 h-6 text-blue-500 mx-auto mb-2" />
                      <p className="text-2xl font-bold text-blue-600 dark:text-blue-400">+{kpis.health.croissanceNette}</p>
                      <p className="text-[10px] text-gray-400">Croissance nette</p>
                    </div>
                  </div>
                  <div className="mt-4 p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs text-gray-500">Taux de croissance</span>
                      <span className="text-sm font-bold text-primary-500">{kpis.health.tauxCroissance}%</span>
                    </div>
                    <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                      <div
                        className="h-2 rounded-full bg-gradient-to-r from-primary-500 to-emerald-500 transition-all duration-500"
                        style={{ width: `${Math.min(kpis.health.tauxCroissance * 10, 100)}%` }}
                      />
                    </div>
                  </div>
                  <div className="mt-3">
                    <div className="flex items-center justify-between text-xs">
                      <span className="text-gray-500">Fidélisation</span>
                      <span className="font-semibold text-emerald-500">{kpis.health.tauxFidelisation}%</span>
                    </div>
                    <div className="flex items-center justify-between text-xs mt-1">
                      <span className="text-gray-500">Présence globale</span>
                      <span className="font-semibold text-blue-500">{kpis.health.tauxPresence}%</span>
                    </div>
                  </div>
                </div>

                {/* Workload par Faiseur */}
                <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-4">Charge de travail — Faiseurs</h3>
                  <div className="space-y-2 max-h-72 overflow-y-auto">
                    {kpis.workload.map((w) => (
                      <div key={w.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors cursor-pointer"
                        onClick={() => onNavigateToTab('utilisateurs')}>
                        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-[10px] font-bold text-white">
                          {w.nom.charAt(0)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between">
                            <span className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{w.nom}</span>
                            <span className="text-[10px] text-gray-400">{w.totalAmes} âmes</span>
                          </div>
                          <div className="flex items-center gap-2 mt-1">
                            <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-1.5">
                              <div
                                className={`h-1.5 rounded-full transition-all duration-500 ${
                                  w.charge > 80 ? 'bg-red-500' : w.charge > 50 ? 'bg-amber-500' : 'bg-emerald-500'
                                }`}
                                style={{ width: `${Math.min(w.charge, 100)}%` }}
                              />
                            </div>
                            <span className="text-[10px] font-semibold text-gray-500 w-8 text-right">{w.charge}%</span>
                          </div>
                        </div>
                        {w.rapportSoumis ? (
                          <span title="Rapport soumis"><CheckCircle className="w-4 h-4 text-emerald-500 flex-shrink-0" /></span>
                        ) : (
                          <span title="Rapport non soumis"><XCircle className="w-4 h-4 text-red-500 flex-shrink-0" /></span>
                        )}
                      </div>
                    ))}
                    {kpis.workload.length === 0 && <p className="text-xs text-gray-400 text-center py-4">Aucun faiseur</p>}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Événements à venir + Rapports en retard */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '250ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <CalendarClock className="w-4 h-4 text-blue-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Événements à venir</h3>
                </div>
                <button onClick={() => onNavigateToTab('evenements')} className="text-[10px] font-medium text-primary-600">Voir tout</button>
              </div>
              <div className="space-y-2">
                {kpis?.upcomingEvents?.map((ev) => (
                  <div key={ev.id} className="flex items-center justify-between p-2.5 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-10 h-10 rounded-xl bg-blue-50 dark:bg-blue-900/20 flex items-center justify-center flex-shrink-0">
                        <Calendar className="w-5 h-5 text-blue-500" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{ev.titre}</p>
                        <p className="text-[10px] text-gray-400">
                          {new Date(ev.dateDebut).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}
                          {ev.lieu ? ` · ${ev.lieu}` : ''}
                        </p>
                      </div>
                    </div>
                    <span className="badge-info text-[9px] flex-shrink-0">{ev.inscrits} inscrits</span>
                  </div>
                ))}
                {(!kpis?.upcomingEvents || kpis.upcomingEvents.length === 0) && (
                  <p className="text-xs text-gray-400 text-center py-4">Aucun événement à venir</p>
                )}
              </div>
            </div>

            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '300ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <AlertOctagon className="w-4 h-4 text-red-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Rapports en retard</h3>
                  {(kpis?.overdueReportsCount ?? 0) > 0 && (
                    <span className="badge-error text-[10px]">{kpis?.overdueReportsCount}</span>
                  )}
                </div>
                <button onClick={() => onNavigateToTab('rapports')} className="text-[10px] font-medium text-primary-600">Voir</button>
              </div>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {kpis?.overdueReports?.map((r) => (
                  <div key={r.faiseurId} className="flex items-center justify-between p-2.5 rounded-lg bg-red-50/30 dark:bg-red-900/10 border border-red-100/50 dark:border-red-900/20">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-red-100 dark:bg-red-900/30 flex items-center justify-center">
                        <UserCog className="w-4 h-4 text-red-500" />
                      </div>
                      <div>
                        <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{r.faiseurNom}</p>
                        <p className="text-[10px] text-gray-400">{r.nbAmes} âmes suivies</p>
                      </div>
                    </div>
                    <span className="text-[9px] text-red-500 font-semibold">Non soumis</span>
                  </div>
                ))}
                {(!kpis?.overdueReports || kpis.overdueReports.length === 0) && (
                  <div className="text-center py-4">
                    <CheckCircle className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
                    <p className="text-xs text-gray-400">Tous les rapports sont à jour !</p>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="glass-card p-6 animate-slide-up">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Taux de présence</h3>
                <span className="badge-info text-[10px]">Semaine en cours</span>
              </div>
              <div className="grid grid-cols-3 gap-3 mb-4">
                <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-2xl font-bold text-primary-500">{presences.tauxGlobal ?? 0}%</p>
                  <p className="text-[10px] text-gray-400">Global</p>
                </div>
                <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-2xl font-bold text-amber-500">{presences.tauxNouveauxArrivants ?? 0}%</p>
                  <p className="text-[10px] text-gray-400">Nouveaux arrivants</p>
                </div>
                <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-2xl font-bold text-emerald-500">{presences.tauxNouveauxConvertis ?? 0}%</p>
                  <p className="text-[10px] text-gray-400">Nouveaux convertis</p>
                </div>
              </div>
              <div className="h-40">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={[
                    { name: 'Nouveaux arrivants', taux: presences.tauxNouveauxArrivants ?? 0 },
                    { name: 'Nouveaux convertis', taux: presences.tauxNouveauxConvertis ?? 0 },
                  ]}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 11 }} stroke="rgba(128,128,128,0.3)" />
                    <YAxis domain={[0, 100]} tick={{ fontSize: 11 }} unit="%" stroke="rgba(128,128,128,0.3)" />
                    <Tooltip formatter={(v: number) => [`${v.toFixed(1)}%`, 'Taux']} />
                    <Bar dataKey="taux" radius={[6, 6, 0, 0]}>
                      <Cell fill="#22c55e" />
                      <Cell fill="#f59e0b" />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '100ms' }}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Rapports hebdomadaires</h3>
                <button onClick={() => onNavigateToTab('rapports')} className="text-xs font-medium text-primary-600 hover:text-primary-700 flex items-center gap-1">
                  Voir <ChevronRight className="w-3 h-3" />
                </button>
              </div>
              <div className="grid grid-cols-2 gap-3 mb-4">
                <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-2xl font-bold text-emerald-500">{rapports.soumis ?? 0}</p>
                  <p className="text-[10px] text-gray-400">Soumis</p>
                </div>
                <div className="text-center p-3 rounded-xl bg-gray-50 dark:bg-gray-800/50">
                  <p className="text-2xl font-bold text-amber-500">{rapports.enAttente ?? 0}</p>
                  <p className="text-[10px] text-gray-400">En attente</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <div className="flex-1 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                  <div
                    className="h-2 rounded-full bg-gradient-to-r from-emerald-500 to-green-500 transition-all duration-500"
                    style={{ width: `${Math.min((rapports.tauxCompletion ?? 0), 100)}%` }}
                  />
                </div>
                <span className="text-xs font-semibold text-gray-600 dark:text-gray-400">
                  {rapports.tauxCompletion ?? 0}%
                </span>
              </div>
              <p className="text-[10px] text-gray-400 mt-2">
                {rapports.faiseursAyantRapporte ?? 0} / {rapports.totalFaiseurs ?? 0} faiseurs ont rapporté
              </p>
            </div>
          </div>

          {/* Tendance de présence (LineChart 12 semaines) */}
          {presenceTrend?.tendance && presenceTrend.tendance.length > 0 && (
            <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-primary-500" />
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
                    Tendance de présence (12 semaines)
                  </h3>
                </div>
                <span className={`text-xs font-semibold ${presenceTrend.tendanceGlobale > 0 ? 'text-green-500' : 'text-red-500'}`}>
                  {presenceTrend.tendanceGlobale > 0 ? '+' : ''}{presenceTrend.tendanceGlobale}%
                </span>
              </div>
              <div className="h-48">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart data={presenceTrend.tendance}>
                    <defs>
                      <linearGradient id="presenceGradient" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="0%" stopColor="#3b82f6" stopOpacity={0.3} />
                        <stop offset="100%" stopColor="#3b82f6" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" vertical={false} />
                    <XAxis dataKey="semaine" tick={{ fontSize: 10 }} stroke="rgba(128,128,128,0.3)"
                      tickFormatter={(v) => v.slice(5)} />
                    <YAxis domain={[0, 100]} tick={{ fontSize: 10 }} unit="%" stroke="rgba(128,128,128,0.3)" />
                    <Tooltip formatter={(v: number) => [`${v.toFixed(1)}%`, 'Taux']}
                      labelFormatter={(l) => `Semaine ${l}`} />
                    <Area type="monotone" dataKey="taux" stroke="#3b82f6" strokeWidth={2}
                      fill="url(#presenceGradient)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}

          {/* Départements — cliquable vers tab */}
          <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '150ms' }}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Building2 className="w-5 h-5 text-primary-500" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Départements</h3>
              </div>
              <button onClick={() => onNavigateToTab('departements')} className="text-xs font-medium text-primary-600 hover:text-primary-700 flex items-center gap-1">
                Gérer <ChevronRight className="w-3 h-3" />
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[10px] text-gray-400 uppercase tracking-wider border-b border-gray-100 dark:border-gray-700/30">
                    <th className="pb-2 font-medium">Département</th>
                    <th className="pb-2 font-medium">Responsable</th>
                    <th className="pb-2 font-medium text-right">Familles</th>
                    <th className="pb-2 font-medium text-right">Âmes</th>
                  </tr>
                </thead>
                <tbody>
                  {departements.map((dept) => (
                    <tr key={dept.id} className="border-b border-gray-50 dark:border-gray-800/30 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 cursor-pointer transition-colors"
                      onClick={() => onNavigateToTab('departements')}>
                      <td className="py-3 font-medium text-gray-900 dark:text-gray-100">{dept.nom}</td>
                      <td className="py-3 text-gray-500">{dept.responsableNom}</td>
                      <td className="py-3 text-right text-gray-700 dark:text-gray-300">{dept.totalFamilles}</td>
                      <td className="py-3 text-right">
                        <span className="font-semibold text-primary-600 dark:text-primary-400">{dept.totalAmes}</span>
                      </td>
                    </tr>
                  ))}
                  {departements.length === 0 && (
                    <tr><td colSpan={4} className="py-6 text-center text-gray-400">Aucun département</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Familles — cliquable vers tab */}
          <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Users className="w-5 h-5 text-primary-500" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Familles de disciples</h3>
              </div>
              <button onClick={() => onNavigateToTab('familles')} className="text-xs font-medium text-primary-600 hover:text-primary-700 flex items-center gap-1">
                Voir tout <ChevronRight className="w-3 h-3" />
              </button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-[10px] text-gray-400 uppercase tracking-wider border-b border-gray-100 dark:border-gray-700/30">
                    <th className="pb-2 font-medium">Famille</th>
                    <th className="pb-2 font-medium">Chef</th>
                    <th className="pb-2 font-medium text-right">Âmes</th>
                    <th className="pb-2 font-medium text-right">Actifs</th>
                    <th className="pb-2 font-medium text-right">Présence</th>
                    <th className="pb-2 font-medium text-right">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  {familles.map((fam) => (
                    <tr key={fam.id} className="border-b border-gray-50 dark:border-gray-800/30 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 cursor-pointer transition-colors"
                      onClick={() => onNavigateToTab('familles')}>
                      <td className="py-3 font-medium text-gray-900 dark:text-gray-100">{fam.nom}</td>
                      <td className="py-3 text-gray-500">{fam.chefNom}</td>
                      <td className="py-3 text-right text-gray-700 dark:text-gray-300">{fam.totalAmes}</td>
                      <td className="py-3 text-right text-emerald-600 dark:text-emerald-400">{fam.actifs}</td>
                      <td className="py-3 text-right">
                        <span className={`font-semibold ${fam.aRisque ? 'text-red-500' : 'text-green-500'}`}>
                          {fam.tauxPresence}%
                        </span>
                      </td>
                      <td className="py-3 text-right">
                        {fam.aRisque ? (
                          <span className="badge-error text-[10px]">À risque</span>
                        ) : (
                          <span className="badge-success text-[10px]">OK</span>
                        )}
                      </td>
                    </tr>
                  ))}
                  {familles.length === 0 && (
                    <tr><td colSpan={6} className="py-6 text-center text-gray-400">Aucune famille</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Transferts à traiter */}
          {transfertsEnAttente.length > 0 && (
            <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '400ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <ArrowLeftRight className="w-4 h-4 text-amber-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Transferts à traiter</h3>
                  <span className="badge-warning text-[10px]">{transfertsEnAttente.length} en attente</span>
                </div>
                <button onClick={() => onNavigateToTab('transferts')} className="text-[10px] font-medium text-primary-600">Voir tout</button>
              </div>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {transfertsEnAttente.slice(0, 6).map((t) => (
                  <div
                    key={t.id}
                    onClick={() => onNavigateToTab('transferts')}
                    className="flex items-center justify-between p-2.5 rounded-lg bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/30 dark:border-amber-800/20 hover:bg-amber-100/50 dark:hover:bg-amber-900/20 transition-colors cursor-pointer"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <ArrowLeftRight className="w-3.5 h-3.5 text-amber-500 flex-shrink-0" />
                      <div className="min-w-0">
                        <p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">
                          {t.personneNom || '—'} → {t.cible || '—'}
                        </p>
                        <p className="text-[9px] text-gray-400">
                          {t.type === 'SOUL_TRANSFERT' ? 'Âme' : t.type === 'FAISEUR_TRANSFERT' ? 'Faiseur' : t.type === 'CHEF_FAMILLE_TRANSFERT' ? 'Chef' : t.type}
                          {t.dateSoumission ? ` · ${new Date(t.dateSoumission).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })}` : ''}
                        </p>
                      </div>
                    </div>
                    {t.priorite === 'HAUTE' && (
                      <span className="text-[8px] bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 px-1.5 py-0.5 rounded-full font-semibold uppercase flex-shrink-0">Priorité</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Bottom: Faiseurs + Alertes + Activité */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '250ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <UserCheck className="w-4 h-4 text-primary-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Faiseurs</h3>
                </div>
                <button onClick={() => onNavigateToTab('utilisateurs')} className="text-[10px] font-medium text-primary-600">Voir</button>
              </div>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {faiseurs.slice(0, 10).map((f) => (
                  <div key={f.id} onClick={() => onNavigateToTab('utilisateurs')} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors cursor-pointer">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-[9px] font-bold text-white">
                        {f.nom.charAt(0)}
                      </div>
                      <div>
                        <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{f.nom}</p>
                        <p className="text-[9px] text-gray-400">{f.totalAmes} âmes · {f.actifs} actifs</p>
                      </div>
                    </div>
                    {f.estChef && (
                      <span className="text-[8px] bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400 px-1.5 py-0.5 rounded-full font-semibold uppercase">Chef</span>
                    )}
                  </div>
                ))}
                {faiseurs.length === 0 && <p className="text-xs text-gray-400 text-center py-4">Aucun faiseur</p>}
              </div>
            </div>

            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '300ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Bell className="w-4 h-4 text-amber-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Alertes</h3>
                </div>
                <button onClick={() => onNavigateToTab('alertes')} className="text-[10px] font-medium text-primary-600">Voir</button>
              </div>
              <div className="text-center py-4">
                <div className={`text-4xl font-bold mb-2 ${(alertStats?.actives ?? dashboard?.alertesActives ?? 0) > 0 ? 'text-red-500' : 'text-green-500'}`}>
                  {alertStats?.actives ?? dashboard?.alertesActives ?? 0}
                </div>
                <p className="text-xs text-gray-400">Alertes actives</p>
              </div>
              {alertStats && alertStats.total > 0 && (
                <div className="h-36 mt-2">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={[
                          { name: 'Actives', value: alertStats.actives, fill: '#ef4444' },
                          { name: 'Traitées', value: alertStats.traitees, fill: '#f59e0b' },
                          { name: 'Résolues', value: alertStats.resolues, fill: '#22c55e' },
                        ].filter(d => d.value > 0)}
                        cx="50%" cy="50%" innerRadius={28} outerRadius={50}
                        paddingAngle={3} dataKey="value" strokeWidth={0}
                      />
                      <Tooltip formatter={(v: number, name: string) => [v, name]}
                        contentStyle={{ fontSize: 11, borderRadius: 8 }} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              )}
              <div className="flex items-center gap-2 justify-center">
                <span className={`w-2 h-2 rounded-full ${(alertStats?.actives ?? dashboard?.alertesActives ?? 0) > 0 ? 'bg-red-500 animate-pulse' : 'bg-green-500'}`} />
                <span className="text-[10px] text-gray-400">
                  {(alertStats?.actives ?? dashboard?.alertesActives ?? 0) > 0 ? 'Attention requise' : 'Tout est sous contrôle'}
                </span>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 dark:border-gray-700/30">
                <div className="flex items-center justify-between text-[10px] text-gray-400">
                  <span>Suivis parallèles actifs</span>
                  <span className="font-semibold text-primary-500">{dashboard?.suivisParallelesActifs ?? 0}</span>
                </div>
              </div>
            </div>

            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '350ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <History className="w-4 h-4 text-blue-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Activité récente</h3>
                </div>
                <button onClick={() => onNavigateToTab('audit')} className="text-[10px] font-medium text-primary-600">Voir tout</button>
              </div>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {recentActivity?.slice(0, 8).map((a) => (
                  <div key={a.id} className="flex items-start gap-2 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors cursor-pointer"
                    onClick={() => onNavigateToTab('audit')}>
                    <div className="w-6 h-6 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-[8px] font-bold text-white flex-shrink-0 mt-0.5">
                      {a.utilisateurNom?.charAt(0) || '?'}
                    </div>
                    <div className="min-w-0">
                      <p className="text-[11px] font-medium text-gray-900 dark:text-gray-100 truncate">
                        <span className="text-primary-600 dark:text-primary-400">{a.utilisateurNom}</span>
                        {' '}{a.action?.toLowerCase().replace(/_/g, ' ')}
                      </p>
                      <p className="text-[9px] text-gray-400">
                        {a.entiteType} · {new Date(a.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                      </p>
                    </div>
                  </div>
                ))}
                {(!recentActivity || recentActivity.length === 0) && (
                  <p className="text-xs text-gray-400 text-center py-4">Aucune activité récente</p>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
