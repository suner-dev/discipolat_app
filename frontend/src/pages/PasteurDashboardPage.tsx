import { useState, useRef, useCallback, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import type { PasteurDashboardData, PresenceTrendData, AuditRecentActivity } from '@/types';
import {
  Heart, Users, Building2, FileText, AlertTriangle, TrendingUp,
  TrendingDown, Activity, Bell, UserCheck, BarChart3, FileDown,
  Loader2, Sparkles, ChevronRight,  Church, BookOpen, Calendar,
  Shield, Star, Search, UserPlus, UserX, Clock,
  CheckCircle, XCircle, ArrowLeftRight, History,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import {
  LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid,
  Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend,
  Area, AreaChart, RadialBarChart, RadialBar,
} from 'recharts';

const COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6'];

const getGreeting = () => {
  const h = new Date().getHours();
  if (h < 12) return 'Bonjour';
  if (h < 17) return 'Bon après-midi';
  return 'Bonsoir';
};

export default function PasteurDashboardPage() {
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
    return () => {
      if (searchTimerRef.current) clearTimeout(searchTimerRef.current);
    };
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

  const croissance = dashboard?.croissance ?? {} as PasteurDashboardData['croissance'];
  const departements = dashboard?.departements ?? [];
  const familles = dashboard?.familles ?? [];
  const faiseurs = dashboard?.faiseurs ?? [];
  const presences = dashboard?.presences ?? {} as PasteurDashboardData['presences'];
  const rapports = dashboard?.rapports ?? {} as PasteurDashboardData['rapports'];
  const famillesARisque = dashboard?.famillesARisque ?? [];
  const transfertsEnAttente = dashboard?.transfertsEnAttente ?? [];

  // Chaque KPI ouvre la liste Âmes déjà filtrée (filtres portés par l'URL,
  // lus par SoulsPage) — jamais une liste vide non filtrée.
  const growthCards = [
    { label: 'Âmes totales', value: croissance.totalAmes ?? 0, icon: Heart, color: 'from-rose-500 to-pink-500', route: '/souls' },
    { label: 'Nouveaux convertis', value: croissance.nouveauxConvertis ?? 0, icon: UserPlus, color: 'from-green-500 to-emerald-500', route: '/souls?typeDisciple=NOUVEAU_CONVERTI' },
    { label: 'Nouveaux arrivants', value: croissance.nouveauxArrivants ?? 0, icon: UserCheck, color: 'from-blue-500 to-indigo-500', route: '/souls?typeDisciple=NOUVEL_ARRIVANT' },
    { label: 'Actifs', value: croissance.actifs ?? 0, icon: CheckCircle, color: 'from-emerald-500 to-teal-500', route: '/souls?statut=ACTIF' },
    { label: 'En intégration', value: croissance.enIntegration ?? 0, icon: Clock, color: 'from-amber-500 to-orange-500', route: '/souls?statut=EN_INTEGRATION' },
    { label: 'En veille', value: croissance.enVeille ?? 0, icon: UserX, color: 'from-yellow-500 to-amber-500', route: '/souls?statut=EN_VEILLE' },
    { label: 'Décrochés', value: croissance.decroches ?? 0, icon: XCircle, color: 'from-red-500 to-rose-500', route: '/souls?statut=DECROCHE' },
    { label: 'Taux conversion', value: `${croissance.tauxConversion ?? 0}%`, icon: TrendingUp, color: 'from-violet-500 to-purple-500', route: '/reports' },
  ];

  // Lignes du dashboard cliquables vers des fonctionnalités réelles.
  const openFamille = (id: string) => navigate(`/families/${id}`);
  const openFaiseur = () => navigate('/users');

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Church className="w-5 h-5 text-primary-500" />
            <span className="text-sm font-medium text-primary-600 dark:text-primary-400 uppercase tracking-wider">
              {getGreeting()}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            Centre de Pilotage{' '}
            <span className="text-gradient font-display">Pasteur</span>
          </h1>
          <p className="page-subtitle">
            Vision 360° de l'église · {new Date().toLocaleDateString('fr-FR', {
              weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
            })}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => exportReport({ endpoint: '/reports/export/consolidated-pdf', filename: `rapport-pasteur-${new Date().toISOString().split('T')[0]}.html` })}
            disabled={isExporting}
            className="btn-glow btn-sm animate-scale-in"
          >
            {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />}
            {isExporting ? 'Génération...' : 'Exporter rapport'}
          </button>
        </div>
      </div>

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
          {/* Growth Cards */}
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
                    onClick={() => navigate(stat.route)}
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

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* Présence */}
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

            {/* Rapports */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '100ms' }}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Rapports hebdomadaires</h3>
                <Link to="/reports" className="text-xs font-medium text-primary-600 hover:text-primary-700 flex items-center gap-1">
                  Voir <ChevronRight className="w-3 h-3" />
                </Link>
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

          {/* Départements */}
          <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '150ms' }}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Building2 className="w-5 h-5 text-primary-500" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Départements</h3>
              </div>
              <Link to="/departments" className="text-xs font-medium text-primary-600 hover:text-primary-700 flex items-center gap-1">
                Gérer <ChevronRight className="w-3 h-3" />
              </Link>
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
                  {departements.map((dept, i) => (
                    <tr key={dept.id} className="border-b border-gray-50 dark:border-gray-800/30 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 cursor-pointer transition-colors"
                      onClick={() => navigate(`/departments/${dept.id}`)}>
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

          {/* Familles */}
          <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '200ms' }}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Users className="w-5 h-5 text-primary-500" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">Familles de disciples</h3>
              </div>
              <Link to="/families" className="text-xs font-medium text-primary-600 hover:text-primary-700 flex items-center gap-1">
                Voir tout <ChevronRight className="w-3 h-3" />
              </Link>
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
                  {familles.map((fam, i) => (
                    <tr key={fam.id} className="border-b border-gray-50 dark:border-gray-800/30 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 cursor-pointer transition-colors"
                      onClick={() => navigate(`/families/${fam.id}`)}>
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
                <Link to="/transfers" className="text-[10px] font-medium text-primary-600">Voir tout</Link>
              </div>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {transfertsEnAttente.slice(0, 6).map((t) => (
                  <Link
                    key={t.id}
                    to={`/transfers/${t.id}`}
                    className="flex items-center justify-between p-2.5 rounded-lg bg-amber-50/50 dark:bg-amber-900/10 border border-amber-200/30 dark:border-amber-800/20 hover:bg-amber-100/50 dark:hover:bg-amber-900/20 transition-colors"
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
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* Bottom: Faiseurs + Alertes + Familles à risque */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
            {/* Faiseurs */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '250ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <UserCheck className="w-4 h-4 text-primary-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Faiseurs</h3>
                </div>
                <Link to="/users" className="text-[10px] font-medium text-primary-600">Voir</Link>
              </div>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {faiseurs.slice(0, 10).map((f) => (
                  <div key={f.id} onClick={openFaiseur} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors cursor-pointer" title="Gérer les utilisateurs / faiseurs">
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

            {/* Alertes */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '300ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <Bell className="w-4 h-4 text-amber-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Alertes</h3>
                </div>
                <Link to="/alerts" className="text-[10px] font-medium text-primary-600">Voir</Link>
              </div>
              <div className="text-center py-6">
                <div className={`text-4xl font-bold mb-2 ${(dashboard?.alertesActives ?? 0) > 0 ? 'text-red-500' : 'text-green-500'}`}>
                  {dashboard?.alertesActives ?? 0}
                </div>
                <p className="text-xs text-gray-400">Alertes actives</p>
              </div>
              <div className="flex items-center gap-2 justify-center">
                <span className={`w-2 h-2 rounded-full ${(dashboard?.alertesActives ?? 0) > 0 ? 'bg-red-500 animate-pulse' : 'bg-green-500'}`} />
                <span className="text-[10px] text-gray-400">
                  {(dashboard?.alertesActives ?? 0) > 0 ? 'Attention requise' : 'Tout est sous contrôle'}
                </span>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-100 dark:border-gray-700/30">
                <div className="flex items-center justify-between text-[10px] text-gray-400">
                  <span>Suivis parallèles actifs</span>
                  <span className="font-semibold text-primary-500">{dashboard?.suivisParallelesActifs ?? 0}</span>
                </div>
              </div>
            </div>

            {/* Fil d'activité récente */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '350ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <History className="w-4 h-4 text-blue-500" />
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Activité récente</h3>
                </div>
                <Link to="/audit" className="text-[10px] font-medium text-primary-600">Voir tout</Link>
              </div>
              <div className="space-y-2 max-h-64 overflow-y-auto">
                {recentActivity?.slice(0, 8).map((a) => (
                  <div key={a.id} className="flex items-start gap-2 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors">
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
    </div>
  );
}
