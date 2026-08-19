import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import {
  FileText, Users, FileSpreadsheet, ArrowRight, Download, FileDown,
  Loader2, BarChart3, Sparkles, ChevronRight, Printer, Search, Filter,
  Calendar, Eye, Clock, CheckCircle2, AlertTriangle, RefreshCw, X,
  TrendingUp, PieChart as PieChartIcon, List,
} from 'lucide-react';
import toast from 'react-hot-toast';
import {
  PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer,
} from 'recharts';

interface MakerReportEntry {
  id: string;
  faiseurId: string;
  faiseurNom?: string;
  familleId?: string;
  familleNom?: string;
  semaine: string;
  statut: string;
  tauxPresence?: number;
  nbDisciples?: number;
  difficultes?: number;
  soumisLe?: string;
  createdAt: string;
}

interface FamilyReportEntry {
  id: string;
  familleId: string;
  familleNom?: string;
  chefNom?: string;
  semaine: string;
  statut: string;
  nbDisciples?: number;
  nbFaiseurs?: number;
  tauxPresence?: number;
  soumisLe?: string;
  createdAt: string;
}

const COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899'];

export default function ReportsPage() {
  const { user } = useAuth();
  const { exportReport, isExporting } = useExportReport();
  const [view, setView] = useState<'overview' | 'maker' | 'family'>('overview');
  const [searchTerm, setSearchTerm] = useState('');
  const [statutFilter, setStatutFilter] = useState('');
  const [detailReport, setDetailReport] = useState<MakerReportEntry | FamilyReportEntry | null>(null);
  const [detailType, setDetailType] = useState<'maker' | 'family'>('maker');

  const isPasteurOrAdmin = user?.activeRole === 'PASTEUR' || user?.activeRole === 'ADMIN';

  const { data: completion } = useQuery({
    queryKey: ['reports', 'completion'],
    queryFn: async () => (await api.get('/dashboard/report-completion')).data as {
      totalRapports: number;
      rapportsSoumis: number;
      tauxCompletion: number;
    },
  });

  const { data: makerReports = [], isLoading: loadingMaker } = useQuery({
    queryKey: ['reports', 'maker-weekly'],
    queryFn: async () => {
      const res = await api.get('/reports/maker-weekly');
      return Array.isArray(res.data) ? res.data as MakerReportEntry[] : (res.data?.content || []) as MakerReportEntry[];
    },
  });

  const { data: familyReports = [], isLoading: loadingFamily } = useQuery({
    queryKey: ['reports', 'family-weekly'],
    queryFn: async () => {
      const res = await api.get('/reports/family-weekly');
      return Array.isArray(res.data) ? res.data as FamilyReportEntry[] : (res.data?.content || []) as FamilyReportEntry[];
    },
  });

  const totalRapports = completion?.totalRapports ?? 0;
  const enAttente = Math.max(0, (completion?.totalRapports ?? 0) - (completion?.rapportsSoumis ?? 0));
  const taux = completion?.tauxCompletion ?? 0;

  // Stats for maker reports
  const makerStats = useMemo(() => {
    const soumis = makerReports.filter((r) => r.statut === 'SOUMIS' || r.statut === 'VALIDE');
    const enAttente = makerReports.filter((r) => r.statut === 'EN_ATTENTE' || r.statut === 'BROUILLON');
    const enRetard = makerReports.filter((r) => r.statut === 'EN_RETARD');
    const avgPresence = makerReports.length > 0
      ? Math.round(makerReports.reduce((acc, r) => acc + (r.tauxPresence || 0), 0) / makerReports.length)
      : 0;
    return { soumis: soumis.length, enAttente: enAttente.length, enRetard: enRetard.length, avgPresence, total: makerReports.length };
  }, [makerReports]);

  // Stats for family reports
  const familyStats = useMemo(() => {
    const soumis = familyReports.filter((r) => r.statut === 'SOUMIS' || r.statut === 'VALIDE');
    const enAttente = familyReports.filter((r) => r.statut === 'EN_ATTENTE' || r.statut === 'BROUILLON');
    return { soumis: soumis.length, enAttente: enAttente.length, total: familyReports.length };
  }, [familyReports]);

  // Filtered data
  const filteredMaker = useMemo(() => {
    let items = makerReports;
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      items = items.filter((r) =>
        r.faiseurNom?.toLowerCase().includes(q) ||
        r.familleNom?.toLowerCase().includes(q) ||
        r.semaine?.toLowerCase().includes(q)
      );
    }
    if (statutFilter) items = items.filter((r) => r.statut === statutFilter);
    return items;
  }, [makerReports, searchTerm, statutFilter]);

  const filteredFamily = useMemo(() => {
    let items = familyReports;
    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      items = items.filter((r) =>
        r.familleNom?.toLowerCase().includes(q) ||
        r.chefNom?.toLowerCase().includes(q) ||
        r.semaine?.toLowerCase().includes(q)
      );
    }
    if (statutFilter) items = items.filter((r) => r.statut === statutFilter);
    return items;
  }, [familyReports, searchTerm, statutFilter]);

  const handleExport = async (type: 'maker' | 'family') => {
    try {
      const response = await api.get(`/reports/export/${type}-weekly`, { responseType: 'blob' });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `rapports-${type}-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success('Export téléchargé avec succès');
    } catch {
      toast.error("Erreur lors de l'export");
    }
  };

  const statusBadge = (statut: string) => {
    const map: Record<string, string> = {
      SOUMIS: 'badge-success',
      VALIDE: 'badge-success',
      EN_ATTENTE: 'badge-warning',
      BROUILLON: 'badge-gray',
      EN_RETARD: 'badge-error',
    };
    const label: Record<string, string> = {
      SOUMIS: 'Soumis',
      VALIDE: 'Validé',
      EN_ATTENTE: 'En attente',
      BROUILLON: 'Brouillon',
      EN_RETARD: 'En retard',
    };
    return (
      <span className={`badge text-[10px] ${map[statut] || 'badge-gray'}`}>
        {label[statut] || statut}
      </span>
    );
  };

  const clearFilters = () => { setSearchTerm(''); setStatutFilter(''); };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <FileText className="w-5 h-5 text-primary-500" />
            <h1 className="page-title">Rapports</h1>
          </div>
          <p className="page-subtitle">Gestion des rapports hebdomadaires — faiseurs et familles</p>
        </div>
        <div className="flex flex-wrap gap-2 animate-fade-in">
          {isPasteurOrAdmin && (
            <>
              <button onClick={() => handleExport('maker')} className="btn-secondary btn-sm">
                <Download className="w-4 h-4" /> CSV faiseur
              </button>
              <button onClick={() => handleExport('family')} className="btn-secondary btn-sm">
                <Download className="w-4 h-4" /> CSV famille
              </button>
              <button
                onClick={() => exportReport({ endpoint: '/reports/export/consolidated-pdf', filename: `rapport-consolide-${new Date().toISOString().split('T')[0]}.html` })}
                disabled={isExporting}
                className="btn-glow btn-sm"
              >
                {isExporting ? <Loader2 className="w-4 h-4 animate-spin" /> : <FileDown className="w-4 h-4" />}
                PDF consolidé
              </button>
            </>
          )}
        </div>
      </div>

      {/* View tabs */}
      <div className="flex items-center gap-2 mb-6 -mx-4 px-4 overflow-x-auto scrollbar-hide">
        {[
          { key: 'overview' as const, label: 'Vue d\'ensemble', icon: BarChart3 },
          { key: 'maker' as const, label: 'Rapports faiseur', icon: FileSpreadsheet, count: makerReports.length },
          { key: 'family' as const, label: 'Rapports famille', icon: Users, count: familyReports.length },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = view === tab.key;
          return (
            <button
              key={tab.key}
              onClick={() => { setView(tab.key); setSearchTerm(''); setStatutFilter(''); }}
              className={`flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium transition-all whitespace-nowrap ${
                isActive
                  ? 'bg-primary-500/10 text-primary-600 dark:text-primary-400 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700 dark:hover:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800/50'
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              {tab.label}
              {tab.count !== undefined && (
                <span className={`ml-1 px-1.5 py-0.5 rounded-full text-[9px] font-bold ${isActive ? 'bg-primary-500/20' : 'bg-gray-200 dark:bg-gray-700'}`}>
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Overview */}
      {view === 'overview' && (
        <div className="space-y-6 animate-slide-up">
          {/* KPIs */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: 'Rapports cette semaine', value: totalRapports, icon: FileSpreadsheet, color: 'from-emerald-500 to-teal-500' },
              { label: 'Taux de soumission', value: `${taux}%`, icon: TrendingUp, color: 'from-blue-500 to-indigo-500' },
              { label: 'En attente', value: enAttente, icon: Clock, color: 'from-amber-500 to-orange-500' },
              { label: 'Présence moy.', value: `${makerStats.avgPresence}%`, icon: PieChartIcon, color: 'from-purple-500 to-violet-500' },
            ].map((kpi, i) => (
              <div key={kpi.label} className="stat-card animate-slide-up" style={{ animationDelay: `${i * 60}ms` }}>
                <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${kpi.color} opacity-60`} />
                <div className="flex items-start justify-between mb-2">
                  <span className="stat-label text-[10px]">{kpi.label}</span>
                  <div className={`p-1.5 rounded-lg bg-gradient-to-br ${kpi.color} text-white shadow-sm`}>
                    <kpi.icon className="w-3.5 h-3.5" />
                  </div>
                </div>
                <p className="stat-value text-xl">{kpi.value}</p>
              </div>
            ))}
          </div>

          {/* Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Statut des rapports faiseur</h3>
              <div className="h-56">
                {makerStats.total > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={[
                          { name: 'Soumis', value: makerStats.soumis, color: '#22c55e' },
                          { name: 'En attente', value: makerStats.enAttente, color: '#f59e0b' },
                          { name: 'En retard', value: makerStats.enRetard, color: '#ef4444' },
                        ].filter(d => d.value > 0)}
                        cx="50%" cy="50%" innerRadius={50} outerRadius={80} paddingAngle={3} dataKey="value" startAngle={90} endAngle={-270}
                      >
                        {[
                          { name: 'Soumis', value: makerStats.soumis, color: '#22c55e' },
                          { name: 'En attente', value: makerStats.enAttente, color: '#f59e0b' },
                          { name: 'En retard', value: makerStats.enRetard, color: '#ef4444' },
                        ].filter(d => d.value > 0).map((entry, i) => <Cell key={i} fill={entry.color} />)}
                      </Pie>
                      <Tooltip />
                      <Legend formatter={(v: string) => <span className="text-xs text-gray-600 dark:text-gray-400">{v}</span>} />
                    </PieChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex items-center justify-center h-full text-gray-400 text-sm">Aucune donnée</div>
                )}
              </div>
            </div>
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '180ms' }}>
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">Résumé rapide</h3>
              <div className="space-y-4">
                <div className="flex items-center justify-between p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <span className="text-sm text-gray-600 dark:text-gray-400">Rapports faiseur</span>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{makerStats.total}</span>
                    {makerStats.total > 0 && (
                      <div className="w-16 h-1.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                        <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${(makerStats.soumis / makerStats.total) * 100}%` }} />
                      </div>
                    )}
                  </div>
                </div>
                <div className="flex items-center justify-between p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <span className="text-sm text-gray-600 dark:text-gray-400">Rapports famille</span>
                  <span className="text-sm font-bold text-gray-900 dark:text-gray-100">{familyStats.total}</span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <span className="text-sm text-gray-600 dark:text-gray-400">En retard faiseur</span>
                  <span className={`text-sm font-bold ${makerStats.enRetard > 0 ? 'text-red-500' : 'text-gray-900 dark:text-gray-100'}`}>{makerStats.enRetard}</span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <span className="text-sm text-gray-600 dark:text-gray-400">Taux soumission</span>
                  <span className={`text-sm font-bold ${taux >= 80 ? 'text-emerald-500' : taux >= 50 ? 'text-amber-500' : 'text-red-500'}`}>{taux}%</span>
                </div>
              </div>

              <div className="flex flex-wrap gap-2 mt-5">
                <button onClick={() => setView('maker')} className="btn-secondary btn-sm flex-1">
                  <FileSpreadsheet className="w-4 h-4" /> Voir faiseurs
                </button>
                <button onClick={() => setView('family')} className="btn-secondary btn-sm flex-1">
                  <Users className="w-4 h-4" /> Voir familles
                </button>
              </div>
            </div>
          </div>

          {/* Quick nav cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {[
              { title: 'Rapport du faiseur', description: 'Saisir le rapport hebdomadaire pour chaque disciple suivi.', icon: FileSpreadsheet, link: '/reports/maker', gradient: 'from-emerald-500 to-teal-500' },
              { title: 'Rapport de famille', description: 'Consulter ou soumettre le rapport consolidé de la famille.', icon: Users, link: '/reports/family', gradient: 'from-blue-500 to-indigo-500' },
            ].map((section, i) => {
              const Icon = section.icon;
              return (
                <Link key={section.link} to={section.link} className="group glass-card p-6 animate-slide-up hover-lift" style={{ animationDelay: `${i * 100 + 300}ms` }}>
                  <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${section.gradient} opacity-0 group-hover:opacity-100 transition-opacity rounded-t-2xl`} />
                  <div className="flex items-start gap-5">
                    <div className={`p-3.5 rounded-xl bg-gradient-to-br ${section.gradient} text-white shadow-lg group-hover:shadow-xl transition-all group-hover:scale-105`}>
                      <Icon className="w-6 h-6" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">{section.title}</h3>
                        <ChevronRight className="w-5 h-5 text-gray-300 group-hover:text-primary-500 group-hover:translate-x-1 transition-all" />
                      </div>
                      <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 leading-relaxed">{section.description}</p>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      )}

      {/* Maker reports view */}
      {view === 'maker' && (
        <div className="space-y-4 animate-slide-up">
          {/* Filters */}
          <div className="glass-card p-4">
            <div className="flex flex-wrap items-center gap-3">
              <div className="relative flex-1 min-w-[200px]">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Rechercher par faiseur, famille, semaine..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="input pl-9"
                />
              </div>
              <div className="flex items-center gap-1.5">
                <Filter className="w-4 h-4 text-gray-400" />
                <select value={statutFilter} onChange={(e) => setStatutFilter(e.target.value)} className="input !w-auto">
                  <option value="">Tous les statuts</option>
                  <option value="SOUMIS">Soumis</option>
                  <option value="EN_ATTENTE">En attente</option>
                  <option value="EN_RETARD">En retard</option>
                  <option value="BROUILLON">Brouillon</option>
                </select>
              </div>
              {(searchTerm || statutFilter) && (
                <button onClick={clearFilters} className="btn-ghost btn-sm"><X className="w-3.5 h-3.5" /> Réinitialiser</button>
              )}
            </div>
          </div>

          {loadingMaker ? (
            <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
          ) : filteredMaker.length === 0 ? (
            <div className="glass-card p-14 text-center">
              <FileSpreadsheet className="w-12 h-12 mx-auto mb-3 text-gray-300" />
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
                {makerReports.length === 0 ? 'Aucun rapport faiseur' : 'Aucun résultat'}
              </h3>
              <p className="text-sm text-gray-500">
                {makerReports.length === 0 ? 'Les rapports hebdomadaires des faiseurs apparaîtront ici.' : 'Essayez de modifier les filtres.'}
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {filteredMaker.map((r, i) => (
                <div key={r.id} className="glass-card px-5 py-4 flex items-center gap-4 hover:shadow-md transition-all hover-lift"
                  style={{ animationDelay: `${i * 30}ms` }}>
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
                    <FileSpreadsheet className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{r.faiseurNom || 'Faiseur'}</p>
                      {statusBadge(r.statut)}
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                      <span><Calendar className="w-3 h-3 inline" /> {r.semaine || '—'}</span>
                      {r.familleNom && <span><Users className="w-3 h-3 inline" /> {r.familleNom}</span>}
                      {r.tauxPresence !== undefined && <span>Présence: {r.tauxPresence}%</span>}
                    </div>
                  </div>
                  <button
                    onClick={() => { setDetailReport(r); setDetailType('maker'); }}
                    className="btn-icon text-gray-400 hover:text-primary-600 hover:bg-primary-50 dark:hover:bg-primary-900/20"
                    title="Voir le détail"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Family reports view */}
      {view === 'family' && (
        <div className="space-y-4 animate-slide-up">
          {/* Filters */}
          <div className="glass-card p-4">
            <div className="flex flex-wrap items-center gap-3">
              <div className="relative flex-1 min-w-[200px]">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Rechercher par famille, chef, semaine..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="input pl-9"
                />
              </div>
              <div className="flex items-center gap-1.5">
                <Filter className="w-4 h-4 text-gray-400" />
                <select value={statutFilter} onChange={(e) => setStatutFilter(e.target.value)} className="input !w-auto">
                  <option value="">Tous les statuts</option>
                  <option value="SOUMIS">Soumis</option>
                  <option value="EN_ATTENTE">En attente</option>
                  <option value="BROUILLON">Brouillon</option>
                </select>
              </div>
              {(searchTerm || statutFilter) && (
                <button onClick={clearFilters} className="btn-ghost btn-sm"><X className="w-3.5 h-3.5" /> Réinitialiser</button>
              )}
            </div>
          </div>

          {loadingFamily ? (
            <div className="flex justify-center py-12"><Loader2 className="w-8 h-8 animate-spin text-primary-500" /></div>
          ) : filteredFamily.length === 0 ? (
            <div className="glass-card p-14 text-center">
              <Users className="w-12 h-12 mx-auto mb-3 text-gray-300" />
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-1">
                {familyReports.length === 0 ? 'Aucun rapport famille' : 'Aucun résultat'}
              </h3>
              <p className="text-sm text-gray-500">
                {familyReports.length === 0 ? 'Les rapports consolidés de famille apparaîtront ici.' : 'Essayez de modifier les filtres.'}
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {filteredFamily.map((r, i) => (
                <div key={r.id} className="glass-card px-5 py-4 flex items-center gap-4 hover:shadow-md transition-all hover-lift"
                  style={{ animationDelay: `${i * 30}ms` }}>
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white flex-shrink-0 shadow-sm">
                    <Users className="w-5 h-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">{r.familleNom || 'Famille'}</p>
                      {statusBadge(r.statut)}
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-xs text-gray-400">
                      <span><Calendar className="w-3 h-3 inline" /> {r.semaine || '—'}</span>
                      {r.chefNom && <span><Users className="w-3 h-3 inline" /> {r.chefNom}</span>}
                      {r.nbFaiseurs !== undefined && <span>{r.nbFaiseurs} faiseur(s)</span>}
                      {r.nbDisciples !== undefined && <span>{r.nbDisciples} disciple(s)</span>}
                    </div>
                  </div>
                  <button
                    onClick={() => { setDetailReport(r); setDetailType('family'); }}
                    className="btn-icon text-gray-400 hover:text-primary-600 hover:bg-primary-50 dark:hover:bg-primary-900/20"
                    title="Voir le détail"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Detail Modal */}
      {detailReport && (
        <div className="modal-overlay" onClick={() => setDetailReport(null)}>
          <div className="modal-content max-w-lg" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="flex items-center gap-3">
                <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-white shadow-sm ${detailType === 'maker' ? 'bg-gradient-to-br from-emerald-500 to-teal-600' : 'bg-gradient-to-br from-blue-500 to-indigo-600'}`}>
                  {detailType === 'maker' ? <FileSpreadsheet className="w-5 h-5" /> : <Users className="w-5 h-5" />}
                </div>
                <div>
                  <h3 className="text-base font-bold text-gray-900 dark:text-gray-100">
                    {detailType === 'maker' ? 'Rapport faiseur' : 'Rapport famille'}
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {detailType === 'maker'
                      ? (detailReport as MakerReportEntry).faiseurNom || 'Faiseur'
                      : (detailReport as FamilyReportEntry).familleNom || 'Famille'}
                  </p>
                </div>
              </div>
              <button onClick={() => setDetailReport(null)} className="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800">
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>
            <div className="modal-body space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Semaine</p>
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mt-0.5">{detailReport.semaine || '—'}</p>
                </div>
                <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                  <p className="text-[10px] text-gray-400 uppercase font-semibold">Statut</p>
                  <div className="mt-0.5">{statusBadge(detailReport.statut)}</div>
                </div>
              </div>
              {detailType === 'maker' && (
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                    <p className="text-[10px] text-gray-400 uppercase font-semibold">Famille</p>
                    <p className="text-sm text-gray-900 dark:text-gray-100 mt-0.5">{(detailReport as MakerReportEntry).familleNom || '—'}</p>
                  </div>
                  <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                    <p className="text-[10px] text-gray-400 uppercase font-semibold">Taux de présence</p>
                    <p className="text-sm font-medium text-gray-900 dark:text-gray-100 mt-0.5">{(detailReport as MakerReportEntry).tauxPresence ?? '—'}%</p>
                  </div>
                </div>
              )}
              {detailType === 'family' && (
                <div className="grid grid-cols-3 gap-3">
                  <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 text-center">
                    <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{(detailReport as FamilyReportEntry).nbFaiseurs ?? '—'}</p>
                    <p className="text-[10px] text-gray-400">Faiseurs</p>
                  </div>
                  <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 text-center">
                    <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{(detailReport as FamilyReportEntry).nbDisciples ?? '—'}</p>
                    <p className="text-[10px] text-gray-400">Disciples</p>
                  </div>
                  <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30 text-center">
                    <p className="text-lg font-bold text-gray-900 dark:text-gray-100">{(detailReport as FamilyReportEntry).tauxPresence ?? '—'}%</p>
                    <p className="text-[10px] text-gray-400">Présence</p>
                  </div>
                </div>
              )}
              <div className="p-3 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                <p className="text-[10px] text-gray-400 uppercase font-semibold">Créé le</p>
                <p className="text-sm text-gray-900 dark:text-gray-100 mt-0.5">
                  {new Date(detailReport.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
                </p>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn-ghost btn-sm" onClick={() => setDetailReport(null)}>Fermer</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
