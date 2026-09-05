import { useQuery } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import { useI18n } from '@/i18n';
import api from '@/lib/api';
import { useExportReport } from '@/hooks/useExportReport';
import {
  Users, Heart, UserCheck, FileText, Activity, Bell, Calendar,
  BookOpen, Star, AlertTriangle, TrendingUp, Loader2, ChevronRight,
  Church, Eye, CheckCircle, Clock, UserX, Search, GitBranch, BarChart3,
  ArrowLeftRight, MapPin, Send, Zap, ClipboardList, Home, Cake,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from 'recharts';

const COLORS = ['#22c55e', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6', '#ec4899'];

const getGreeting = (t: (k: string) => string) => {
  const h = new Date().getHours();
  if (h < 12) return t('chef.greetingMorning');
  if (h < 17) return t('chef.greetingAfternoon');
  return t('chef.greetingEvening');
};

export default function ChefFamilleDashboardPage() {
  const { t, locale } = useI18n();
  const { user, activeRole } = useAuth();
  const navigate = useNavigate();
  const { exportReport, isExporting } = useExportReport();
  const canManage = activeRole === 'CHEF_DE_FAMILLE';
  const familleId = user?.familleGereeId;

  const { data: dashboard, isLoading } = useQuery({
    queryKey: ['dashboard', 'chef-famille', familleId],
    queryFn: async () => { const res = await api.get('/dashboard/chef-famille', { params: familleId ? { familleId } : undefined }); return res.data as any; },
    enabled: !!user,
  });

  const { data: workload } = useQuery({
    queryKey: ['users', 'workload', familleId],
    queryFn: async () => { const res = await api.get('/users/faiseur-workload', { params: familleId ? { familleId } : undefined }); return res.data as any[]; },
    enabled: !!familleId,
  });

  const { data: alerts } = useQuery({
    queryKey: ['alerts', 'famille', familleId],
    queryFn: async () => { const res = await api.get('/alerts', { params: { familleId, size: 5 } }); return res.data?.content || []; },
    enabled: !!familleId,
  });

  const { data: upcomingVisits } = useQuery({
    queryKey: ['visits', 'upcoming', familleId],
    queryFn: async () => { const res = await api.get('/visits/upcoming'); return res.data as any[]; },
    enabled: !!familleId,
  });

  const { data: prayers } = useQuery({
    queryKey: ['prayers', 'famille', familleId],
    queryFn: async () => { const res = await api.get('/prayers', { params: { familleId, size: 5 } }); return res.data?.content || []; },
    enabled: !!familleId,
  });

  const { data: events } = useQuery({
    queryKey: ['events', 'famille', familleId],
    queryFn: async () => { const res = await api.get('/events', { params: { size: 6 } }); return res.data?.content || []; },
    enabled: !!familleId,
  });

  const famille = dashboard?.famille ?? {};
  const faiseurs = dashboard?.faiseurs ?? [];
  const disciples = dashboard?.disciples ?? [];
  const stats = dashboard?.statistiques ?? {};

  const birthdaysThisMonth = disciples.filter((d: any) => {
    if (!d.dateNaissance) return false;
    const bday = new Date(d.dateNaissance);
    const now = new Date();
    return bday.getMonth() === now.getMonth();
  }).length;

  const avgGrowth = disciples.length > 0
    ? (disciples.reduce((sum: number, d: any) => sum + (d.niveauCroissance || 1), 0) / disciples.length).toFixed(1)
    : '0';

  const growthDistribution = [
    { name: `${t('chef.level')} 1`, value: disciples.filter((d: any) => d.niveauCroissance === 1).length, color: '#ef4444' },
    { name: `${t('chef.level')} 2`, value: disciples.filter((d: any) => d.niveauCroissance === 2).length, color: '#f59e0b' },
    { name: `${t('chef.level')} 3`, value: disciples.filter((d: any) => d.niveauCroissance === 3).length, color: '#3b82f6' },
    { name: `${t('chef.level')} 4`, value: disciples.filter((d: any) => d.niveauCroissance === 4).length, color: '#22c55e' },
    { name: `${t('chef.level')} 5`, value: disciples.filter((d: any) => d.niveauCroissance === 5).length, color: '#8b5cf6' },
  ].filter(d => d.value > 0);

  const disciplesByStatut = [
    { name: t('chef.actifs'), value: stats.actifs ?? 0, color: '#22c55e' },
    { name: t('chef.integration'), value: stats.enIntegration ?? 0, color: '#f59e0b' },
    { name: t('chef.sleep'), value: stats.enVeille ?? 0, color: '#3b82f6' },
    { name: t('chef.dropped'), value: stats.decroches ?? 0, color: '#ef4444' },
  ].filter(d => d.value > 0);

  const networkNodes = faiseurs.map((f: any) => ({ ...f, disciples: disciples.filter((d: any) => d.faiseurId === f.id) }));

  const activeAlerts = (alerts || []).filter((a: any) => a.statut === 'ACTIVE');

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div className="animate-fade-in">
          <div className="flex items-center gap-2 mb-1">
            <Users className="w-5 h-5 text-gold-500" />
            <span className="text-sm font-medium text-gold-600 dark:text-gold-400 uppercase tracking-wider">
              {getGreeting(t)}, {user?.firstName}
            </span>
          </div>
          <h1 className="page-title">
            {t('chef.family')}{' '}
            <span className="text-gradient font-display">{famille.nom || t('chef.ofDisciples')}</span>
          </h1>
          <p className="page-subtitle">
            {t('chef.subtitle')} · {new Date().toLocaleDateString(locale, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
        </div>
        <div className="flex gap-2">
          <button onClick={() => exportReport({ endpoint: '/reports/export/consolidated-pdf', filename: `famille-${famille.nom || 'export'}-${new Date().toISOString().split('T')[0]}.html` })} disabled={isExporting}
            className="btn-glow btn-sm">
            <FileText className="w-4 h-4" /> {t('chef.export')}
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[...Array(6)].map((_, i) => <div key={i} className="glass-card p-5"><div className="skeleton h-4 w-24 mb-3 rounded" /><div className="skeleton h-8 w-20 rounded" /></div>)}
        </div>
      ) : !dashboard?.famille?.id ? (
        <div className="glass-card p-12 text-center animate-fade-in">
          <Church className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h2 className="text-lg font-semibold text-gray-600 dark:text-gray-400 mb-2">{t('chef.noFamily')}</h2>
          <p className="text-sm text-gray-400">{t('chef.noFamilyDesc')}</p>
        </div>
      ) : (
        <>
          {/* Stats Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
            {[
              { label: t('chef.disciples'), value: stats.totalDisciples ?? 0, icon: Heart, color: 'from-gold-500 to-amber-500', tab: '#liste-disciples-famille' },
              { label: t('chef.faiseurs'), value: stats.totalFaiseurs ?? 0, icon: UserCheck, color: 'from-emerald-500 to-green-500', tab: '#network' },
              { label: t('chef.actifs'), value: stats.actifs ?? 0, icon: CheckCircle, color: 'from-emerald-500 to-teal-500', tab: '#liste-disciples-famille' },
              { label: t('chef.alertes'), value: activeAlerts.length, icon: AlertTriangle, color: 'from-red-500 to-rose-500', tab: '#alertes-famille', alert: activeAlerts.length > 0 },
              { label: t('chef.visites'), value: upcomingVisits?.length ?? 0, icon: Calendar, color: 'from-teal-500 to-cyan-500', tab: '#visites-famille' },
              { label: t('chef.prieres'), value: prayers?.length ?? 0, icon: BookOpen, color: 'from-indigo-500 to-blue-500', tab: '#prieres-famille' },
            ].map((s, i) => (
              <button key={s.label} type="button" onClick={() => document.querySelector(s.tab)?.scrollIntoView({ behavior: 'smooth' })}
                className="glass-card p-3.5 text-left hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 relative overflow-hidden">
                <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${s.color} opacity-60`} />
                <div className="flex items-start justify-between mb-2">
                  <span className="text-[10px] text-gray-400 font-medium">{s.label}</span>
                  <div className={`p-1.5 rounded-lg bg-gradient-to-br ${s.color} text-white`}><s.icon className="w-3.5 h-3.5" /></div>
                </div>
                <span className="text-xl font-bold text-gray-900 dark:text-gray-100">{s.value}</span>
                {s.alert && <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-red-500 animate-pulse" />}
              </button>
            ))}
          </div>

          {/* Active Alerts */}
          {activeAlerts.length > 0 && (
            <div id="alertes-famille" className="glass-card p-6 mb-6 border-l-4 border-l-red-500 animate-slide-up">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2"><AlertTriangle className="w-4 h-4 text-red-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.activeAlerts')} ({activeAlerts.length})</h3></div>
                <Link to="/alerts" className="text-[10px] font-medium text-primary-600">{t('chef.viewAll')}</Link>
              </div>
              <div className="space-y-2">
                {activeAlerts.slice(0, 3).map((a: any) => (
                  <div key={a.id} className="flex items-center gap-3 p-2 rounded-lg bg-red-50/30 dark:bg-red-900/10 border border-red-100/50">
                    <AlertTriangle className="w-4 h-4 text-red-500 flex-shrink-0" />
                    <div className="min-w-0 flex-1"><p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{a.message}</p><p className="text-[9px] text-gray-400">{a.typeAlerte} · {a.priorite}</p></div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Quick Actions */}
          {canManage && (
            <div className="glass-card p-5 mb-6 animate-slide-up" style={{ animationDelay: '50ms' }}>
              <div className="flex items-center gap-2 mb-3"><Zap className="w-4 h-4 text-primary-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.quickActions')}</h3></div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                {[
                  { href: '/reports/family', icon: ClipboardList, label: t('chef.familyReport'), color: 'from-blue-500 to-indigo-500' },
                  { href: '/souls', icon: Heart, label: t('chef.disciples'), color: 'from-gold-500 to-amber-500' },
                  { href: '/events', icon: Calendar, label: t('chef.events'), color: 'from-emerald-500 to-teal-500' },
                  { href: '/prayers', icon: BookOpen, label: t('chef.prieres'), color: 'from-indigo-500 to-violet-500' },
                ].map((a) => (
                  <Link key={a.href} to={a.href} className="flex items-center gap-2 p-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800/30 border border-gray-100 dark:border-gray-700/30 transition-all hover:shadow-md group">
                    <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${a.color} text-white flex items-center justify-center shadow-sm group-hover:scale-110 transition-transform`}><a.icon className="w-4 h-4" /></div>
                    <span className="text-xs font-medium text-gray-700 dark:text-gray-300">{a.label}</span>
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div className="glass-card p-6 animate-slide-up">
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100 mb-4">{t('chef.disciplesRepartition')}</h3>
              <div className="h-56">
                {disciplesByStatut.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart><Pie data={disciplesByStatut} cx="50%" cy="50%" innerRadius={50} outerRadius={80} paddingAngle={3} dataKey="value" startAngle={90} endAngle={-270}>
                      {disciplesByStatut.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                    </Pie><Tooltip /><Legend formatter={(v: string) => <span className="text-xs text-gray-600 dark:text-gray-400">{v}</span>} /></PieChart>
                  </ResponsiveContainer>
                ) : <div className="flex items-center justify-center h-full text-gray-400 text-sm">{t('chef.noData')}</div>}
              </div>
            </div>
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '60ms' }}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.weekReports')}</h3>
                <Link to="/reports" className="text-[10px] font-medium text-primary-600">{t('chef.detail')}</Link>
              </div>
              <div className="h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={[
                    { name: t('chef.submitted'), value: stats.rapportsSoumisSemaine ?? 0, color: '#22c55e' },
                    { name: t('chef.pending'), value: stats.rapportsEnAttente ?? 0, color: '#f59e0b' },
                  ]}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" vertical={false} />
                    <XAxis dataKey="name" tick={{ fontSize: 12 }} stroke="rgba(128,128,128,0.3)" />
                    <YAxis tick={{ fontSize: 12 }} stroke="rgba(128,128,128,0.3)" />
                    <Tooltip /><Bar dataKey="value" radius={[8, 8, 0, 0]}><Cell fill="#22c55e" /><Cell fill="#f59e0b" /></Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>

          {/* Upcoming Visits + Prayers */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <div id="visites-famille" className="glass-card p-6 animate-slide-up" style={{ animationDelay: '80ms' }}>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2"><Calendar className="w-4 h-4 text-teal-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.upcomingVisits')}</h3></div>
                <Link to="/visits" className="text-[10px] font-medium text-primary-600">{t('chef.viewAll')}</Link>
              </div>
              {upcomingVisits && upcomingVisits.length > 0 ? (
                <div className="space-y-2">
                  {upcomingVisits.slice(0, 4).map((v: any) => (
                    <div key={v.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
                      <div className="w-8 h-8 rounded-lg bg-teal-50 dark:bg-teal-900/20 flex items-center justify-center"><MapPin className="w-4 h-4 text-teal-500" /></div>
                      <div className="min-w-0 flex-1"><p className="text-xs font-medium text-gray-900 dark:text-gray-100">{v.ameNom || '—'}</p>                    <p className="text-[9px] text-gray-400">{v.datePrevue ? new Date(v.datePrevue).toLocaleDateString(locale, { day: 'numeric', month: 'short' }) : '—'} · {v.typeVisite}</p></div>
                    </div>
                  ))}
                </div>
              ) : <p className="text-xs text-gray-400 text-center py-4">{t('chef.noVisit')}</p>}
            </div>
            <div id="prieres-famille" className="glass-card p-6 animate-slide-up" style={{ animationDelay: '100ms' }}>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2"><BookOpen className="w-4 h-4 text-indigo-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.familyPrayers')}</h3></div>
                <Link to="/prayers" className="text-[10px] font-medium text-primary-600">{t('chef.viewAll')}</Link>
              </div>
              {prayers && prayers.length > 0 ? (
                <div className="space-y-2">
                  {prayers.slice(0, 4).map((p: any) => (
                    <div key={p.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${p.statut === 'EXAUCEE' ? 'bg-green-50 dark:bg-green-900/20' : 'bg-indigo-50 dark:bg-indigo-900/20'}`}>
                        {p.statut === 'EXAUCEE' ? <CheckCircle className="w-4 h-4 text-green-500" /> : <BookOpen className="w-4 h-4 text-indigo-500" />}
                      </div>
                      <div className="min-w-0 flex-1"><p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{p.titre}</p><p className="text-[9px] text-gray-400">{p.priorite} · {p.visibilite}</p></div>
                    </div>
                  ))}
                </div>
              ) : <p className="text-xs text-gray-400 text-center py-4">{t('chef.noPrayer')}</p>}
            </div>
          </div>

          {/* Workload */}
          {canManage && workload && workload.length > 0 && (
            <div className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '110ms' }}>
              <div className="flex items-center gap-2 mb-3"><BarChart3 className="w-4 h-4 text-primary-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.workload')}</h3></div>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
                {workload.map((w: any) => {
                  const style = w.charge === 'LEGER' ? 'bg-green-100/80 dark:bg-green-900/30 text-green-700 dark:text-green-400' : w.charge === 'SURCHARGÉ' ? 'bg-red-100/80 dark:bg-red-900/30 text-red-700 dark:text-red-300' : 'bg-blue-100/80 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300';
                  const label = w.charge === 'LEGER' ? t('chef.light') : w.charge === 'SURCHARGÉ' ? t('chef.overloaded') : t('chef.normal');
                  return (
                    <div key={w.faiseurId} className="p-3 rounded-xl bg-gradient-to-br from-gray-50 to-white dark:from-gray-800/50 dark:to-gray-800/30 border border-gray-100 dark:border-gray-700/50">
                      <p className="text-xs text-gray-500 truncate" title={w.faiseurName}>{w.faiseurName}</p>
                      <p className="text-lg font-bold text-gray-900 dark:text-gray-100 mt-1">{w.soulCount}</p>
                      <p className="text-[10px] text-gray-400">{t('chef.soulsTracked')}</p>
                      <span className={`inline-flex items-center px-1.5 py-0.5 rounded-full text-[9px] font-semibold border mt-1 ${style}`}>{label}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Network View */}
          {canManage && (
            <div id="network" className="glass-card p-6 mb-6 animate-slide-up" style={{ animationDelay: '120ms' }}>
              <div className="flex items-center gap-2 mb-4"><GitBranch className="w-4 h-4 text-primary-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.networkView')}</h3></div>
              {networkNodes.length > 0 ? (
                <div className="space-y-4">
                  {networkNodes.map((faiseur: any) => (
                    <div key={faiseur.id} className="border border-gray-100 dark:border-gray-700/30 rounded-xl overflow-hidden">
                      <div className="flex items-center justify-between p-3 bg-gradient-to-r from-primary-500/5 to-primary-600/5 border-b border-gray-100 dark:border-gray-700/30">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-xs font-bold text-white">{faiseur.nom?.charAt(0)}</div>
                          <div><p className="text-sm font-medium text-gray-900 dark:text-gray-100">{faiseur.nom}</p><p className="text-[10px] text-gray-400">{faiseur.totalAmes} {t('chef.disciplesCount')}</p></div>
                        </div>
                        <span className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${faiseur.rapportSoumis ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400' : 'bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400'}`}>
                          {faiseur.rapportSoumis ? t('chef.reportOk') : t('chef.pending')}
                        </span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2 p-3">
                        {faiseur.disciples?.slice(0, 6).map((soul: any) => (
                          <div key={soul.id} className="flex items-center gap-2 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800/30 cursor-pointer" onClick={() => navigate(`/souls/${soul.id}`)}>
                            <div className={`w-2 h-2 rounded-full ${soul.statut === 'ACTIF' ? 'bg-green-500' : soul.statut === 'EN_INTEGRATION' ? 'bg-amber-500' : soul.statut === 'EN_VEILLE' ? 'bg-blue-500' : 'bg-red-500'}`} />
                            <div className="flex-1 min-w-0"><p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{soul.nom}</p></div>
                            <span className="text-[8px] font-semibold px-1.5 py-0.5 rounded-full bg-gray-100 dark:bg-gray-800/50 text-gray-500">Niv.{soul.niveauCroissance}</span>
                          </div>
                        ))}
                        {faiseur.disciples?.length > 6 && <div className="flex items-center justify-center p-2 text-[10px] text-primary-500">+{faiseur.disciples.length - 6} {t('chef.others')}</div>}
                      </div>
                    </div>
                  ))}
                </div>
              ) : <div className="text-center py-8"><Users className="w-8 h-8 text-gray-300 mx-auto mb-2" /><p className="text-sm text-gray-400">{t('chef.noFaiseur')}</p></div>}
            </div>
          )}

          {/* Progression & Events Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* Progression */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '140ms' }}>
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2"><TrendingUp className="w-4 h-4 text-emerald-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.spiritualProgress')}</h3></div>
                <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-amber-50 dark:bg-amber-900/20 border border-amber-200/40">
                  <Star className="w-3 h-3 text-amber-400 fill-amber-400" />
                  <span className="text-[10px] font-bold text-amber-600 dark:text-amber-400">{avgGrowth}</span>
                </div>
              </div>
              {growthDistribution.length > 0 ? (
                <div className="h-48">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={growthDistribution} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(128,128,128,0.15)" horizontal={false} />
                      <XAxis type="number" tick={{ fontSize: 11 }} stroke="rgba(128,128,128,0.3)" />
                      <YAxis type="category" dataKey="name" tick={{ fontSize: 10 }} stroke="rgba(128,128,128,0.3)" width={60} />
                      <Tooltip />
                      <Bar dataKey="value" radius={[0, 6, 6, 0]}>
                        {growthDistribution.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : <div className="flex items-center justify-center h-48 text-gray-400 text-sm">{t('chef.noData')}</div>}
            </div>

            {/* Events */}
            <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '160ms' }}>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2"><Calendar className="w-4 h-4 text-emerald-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.upcomingEvents')}</h3></div>
                <Link to="/events" className="text-[10px] font-medium text-primary-600">{t('chef.viewAll')}</Link>
              </div>
              {events && events.length > 0 ? (
                <div className="space-y-2">
                  {events.slice(0, 4).map((e: any) => (
                    <div key={e.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30">
                      <div className="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-emerald-900/20 flex flex-col items-center justify-center flex-shrink-0">
                        <span className="text-[9px] text-emerald-500 font-semibold leading-none">{new Date(e.dateEvenement || e.date).toLocaleDateString(locale, { month: 'short' })}</span>
                        <span className="text-sm font-bold text-emerald-700 dark:text-emerald-300 leading-none">{new Date(e.dateEvenement || e.date).getDate()}</span>
                      </div>
                      <div className="min-w-0 flex-1"><p className="text-xs font-medium text-gray-900 dark:text-gray-100 truncate">{e.titre}</p><p className="text-[9px] text-gray-400">{e.lieu || '—'} · {e.heureDebut || ''}</p></div>
                    </div>
                  ))}
                </div>
              ) : <p className="text-xs text-gray-400 text-center py-4">{t('chef.noEvent')}</p>}
            </div>
          </div>

          {/* Birthdays */}
          {birthdaysThisMonth > 0 && (
            <div className="glass-card p-5 mb-6 animate-slide-up" style={{ animationDelay: '170ms' }}>
              <div className="flex items-center gap-2 mb-3"><Cake className="w-4 h-4 text-pink-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.birthdays')} ({birthdaysThisMonth})</h3></div>
              <div className="flex flex-wrap gap-2">
                {disciples.filter((d: any) => d.dateNaissance && new Date(d.dateNaissance).getMonth() === new Date().getMonth()).slice(0, 8).map((d: any) => (
                  <div key={d.id} className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-pink-50 dark:bg-pink-900/20 border border-pink-200/40">
                    <span className="text-[10px]">🎂</span>
                    <span className="text-[11px] font-medium text-gray-700 dark:text-gray-300">{d.nom}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* All Disciples List */}
          <div id="liste-disciples-famille" className="glass-card p-6 animate-slide-up scroll-mt-24" style={{ animationDelay: '180ms' }}>
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2"><Heart className="w-4 h-4 text-primary-500" /><h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">{t('chef.allDisciples')} ({disciples.length})</h3></div>
              <Link to={`/souls?familleId=${dashboard?.famille?.id}`} className="text-[10px] font-medium text-primary-600">{t('chef.viewAll')}</Link>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead><tr className="text-left text-[10px] text-gray-400 uppercase tracking-wider border-b border-gray-100 dark:border-gray-700/30">
                  <th className="pb-2 font-medium">{t('chef.name')}</th><th className="pb-2 font-medium">{t('chef.faiseur')}</th><th className="pb-2 font-medium">{t('chef.status')}</th><th className="pb-2 font-medium">{t('chef.type')}</th><th className="pb-2 font-medium">{t('chef.level')}</th><th className="pb-2 font-medium">{t('chef.report')}</th>
                </tr></thead>
                <tbody>
                  {disciples.map((soul: any) => (
                    <tr key={soul.id} className="border-b border-gray-50 dark:border-gray-800/30 hover:bg-gray-50/50 dark:hover:bg-gray-800/20 cursor-pointer" onClick={() => navigate(`/souls/${soul.id}`)}>
                      <td className="py-2.5 font-medium text-gray-900 dark:text-gray-100">{soul.nom}</td>
                      <td className="py-2.5 text-gray-500 text-xs">{soul.faiseurNom}</td>
                      <td className="py-2.5"><span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded-full ${soul.statut === 'ACTIF' ? 'badge-success' : soul.statut === 'EN_INTEGRATION' ? 'badge-warning' : 'badge-error'}`}>{soul.statut === 'ACTIF' ? t('chef.active') : soul.statut === 'EN_INTEGRATION' ? t('chef.integration') : soul.statut === 'EN_VEILLE' ? t('chef.sleep') : t('chef.dropped')}</span></td>
                      <td className="py-2.5 text-gray-500 text-xs">{soul.type === 'NOUVEAU_CONVERTI' ? t('chef.newConvert') : t('chef.newArrival')}</td>
                      <td className="py-2.5"><div className="flex gap-0.5">{[1,2,3,4,5].map(i => <Star key={i} className={`w-2.5 h-2.5 ${i <= (soul.niveauCroissance || 1) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />)}</div></td>
                      <td className="py-2.5"><span className={`text-[9px] font-semibold ${soul.rapportSemaine ? 'text-green-500' : 'text-amber-500'}`}>{soul.rapportSemaine ? t('chef.submitted') : t('chef.pending')}</span></td>
                    </tr>
                  ))}
                  {disciples.length === 0 && <tr><td colSpan={6} className="py-6 text-center text-gray-400">{t('chef.noDisciple')}</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
