import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  Palette, Boxes, Menu as MenuIcon, Shield, UserCog, Activity, FileText,
  ArrowRight, Sparkles, MessageSquareText, BookOpen, LayoutTemplate, Building2,
  Users, Bell, Settings, Workflow, Database, Zap, Globe, Sliders, Server,
  Gauge, RefreshCw, Clock, AlertTriangle, CheckCircle2, Cpu, HardDrive,
  Search, TrendingUp, BarChart3, FileDown, UserPlus, Eye, Smartphone,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useSettings } from '@/contexts/SettingsContext';
import { useI18n } from '@/i18n';

interface SystemHealth {
  jvm: { heapUsed: number; heapMax: number; nonHeapUsed: number; threads: number; };
  database: { status: string; totalUsers: number; totalSouls: number; tableCount: number; };
  uptime: string;
}

interface CacheStats {
  caches: Record<string, { hits: number; misses: number; ratio: string; health: string }>;
  summary: { totalHits: number; totalMisses: number; overallRatio: string; healthStatus: string };
}

interface AuditEntry {
  id: string; utilisateurNom: string; action: string; entiteType: string; createdAt: string;
}

const CONFIG_SECTIONS = [
  {
    title: 'Identité & apparence',
    description: 'Nom, logo, couleurs, thème et typographie',
    items: [
      { href: '/admin/settings', icon: Palette, title: 'Identité & marque', desc: 'Nom, logo, couleurs, polices, coordonnées', gradient: 'from-primary-500 to-emerald-600' },
    ],
  },
  {
    title: 'Navigation & structure',
    description: 'Menus, modules activables et pages personnalisées',
    items: [
      { href: '/admin/modules', icon: Boxes, title: 'Modules', desc: 'Activer/désactiver les fonctionnalités', gradient: 'from-violet-500 to-purple-600' },
      { href: '/admin/menus', icon: MenuIcon, title: 'Menus', desc: 'Navigation : ordre, libellé, icônes, rôles', gradient: 'from-amber-500 to-orange-600' },
      { href: '/admin/pages', icon: LayoutTemplate, title: 'Pages', desc: 'Pages personnalisées avec blocs, KPI, formulaires', gradient: 'from-teal-500 to-cyan-600' },
    ],
  },
  {
    title: 'Données & référentiels',
    description: 'Champs personnalisés, dictionnaires et traductions',
    items: [
      { href: '/admin/custom-fields', icon: FileText, title: 'Champs personnalisés', desc: 'Ajouter des champs aux entités', gradient: 'from-sky-500 to-blue-600' },
      { href: '/admin/dictionaries', icon: BookOpen, title: 'Dictionnaires', desc: 'Types, statuts, catégories — référentiels configurables', gradient: 'from-fuchsia-500 to-pink-600' },
    ],
  },
  {
    title: 'Sécurité & contrôle',
    description: 'Rôles, permissions, audit et gestion des utilisateurs',
    items: [
      { href: '/permissions', icon: Shield, title: 'Rôles & permissions', desc: 'Matrice granulaire R/W/D + scopes', gradient: 'from-indigo-500 to-violet-600' },
      { href: '/users', icon: UserCog, title: 'Utilisateurs', desc: 'Comptes, sessions, sécurité, historique', gradient: 'from-teal-500 to-cyan-600' },
      { href: '/audit', icon: Activity, title: 'Audit', desc: 'Journal immuable de toutes les actions', gradient: 'from-gray-500 to-slate-600' },
    ],
  },
  {
    title: 'Paiements & finances',
    description: 'Dashboard paiements, webhooks, dîmes & offrandes',
    items: [
      { href: '/admin/payments', icon: BarChart3, title: 'Dashboard paiements', desc: 'Stats, graphiques, dons récurrents', gradient: 'from-green-500 to-emerald-600' },
      { href: '/admin/webhook-logs', icon: Activity, title: 'Logs webhooks', desc: 'Historique des callbacks opérateurs', gradient: 'from-indigo-500 to-purple-600' },
      { href: '/giving', icon: Smartphone, title: 'Dîmes & offrandes', desc: 'Module de dons Mobile Money', gradient: 'from-blue-500 to-cyan-600' },
    ],
  },
  {
    title: 'Automatisation',
    description: 'Workflows, notifications et intégrations',
    items: [
      { href: '/admin/transfers', icon: Workflow, title: 'Workflows', desc: 'Circuits de validation multi-étapes', gradient: 'from-orange-500 to-amber-600' },
      { href: '/admin/notifications', icon: Bell, title: 'Notifications', desc: 'Modèles, canaux, événements et destinataires', gradient: 'from-rose-500 to-red-600' },
      { href: '/admin/integrations', icon: Globe, title: 'Intégrations', desc: 'SMTP, stockage, clés API, services externes', gradient: 'from-blue-500 to-indigo-600' },
    ],
  },
  {
    title: 'Infrastructure',
    description: 'Système, performance, cache et santé technique',
    items: [
      { href: '/admin/system', icon: Server, title: 'Système', desc: 'Santé JVM, cache, performances, paramètres', gradient: 'from-cyan-500 to-blue-600' },
      { href: '/admin/tenants', icon: Building2, title: 'Églises (tenants)', desc: 'Gestion multi-tenant de la plateforme', gradient: 'from-emerald-500 to-teal-600' },
      { href: '/admin/feedback', icon: MessageSquareText, title: 'Retours testeurs', desc: 'Bugs, suggestions et retours UX', gradient: 'from-blue-500 to-indigo-600' },
    ],
  },
];

const entityTypeIcon: Record<string, string> = { SOUL: '💜', USER: '👤', FAMILY: '👨‍👩‍👧', DEPARTMENT: '🏢', TRANSFER: '🔄', ALERT: '🔔', REPORT: '📋', EVENT: '📅', VISIT: '🏠', PRAYER: '🙏' };

export default function AdminDashboardPage() {
  const { locale } = useI18n();
  const { activeRole } = useAuth();
  const { branding } = useSettings();

  const { data: health } = useQuery({
    queryKey: ['admin', 'system-health'],
    queryFn: async () => { const res = await api.get('/admin/system-health'); return res.data as SystemHealth; },
    refetchInterval: 30000,
  });

  const { data: cache } = useQuery({
    queryKey: ['admin', 'cache-stats'],
    queryFn: async () => { const res = await api.get('/admin/cache-stats'); return res.data as CacheStats; },
    refetchInterval: 30000,
  });

  const { data: recentAudit } = useQuery({
    queryKey: ['audit', 'recent', 'admin'],
    queryFn: async () => { const res = await api.get('/audit/recent', { params: { limit: 10 } }); return res.data as AuditEntry[]; },
  });

  const { data: alertStats } = useQuery({
    queryKey: ['alerts', 'stats'],
    queryFn: async () => { const res = await api.get('/alerts/stats'); return res.data as { actives: number; traitees: number; resolues: number; total: number }; },
  });

  const totalConfigItems = CONFIG_SECTIONS.reduce((acc, s) => acc + s.items.length, 0);

  const actionColor = (action: string) => {
    if (action?.includes('CREATE')) return 'text-green-600';
    if (action?.includes('UPDATE')) return 'text-amber-600';
    if (action?.includes('DELETE')) return 'text-red-600';
    return 'text-gray-600';
  };

  return (
    <div className="page-container max-w-6xl">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-1">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 text-white flex items-center justify-center shadow-lg">
            <Sliders className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-gray-100 font-display tracking-tight">
              Centre d'administration
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
              Configurez {branding.platformName} — aucun code requis. {totalConfigItems} outils disponibles.
            </p>
          </div>
        </div>
      </div>

      {/* System health bar — real-time */}
      {(health || cache) && (
        <div className="glass-card p-4 mb-6 animate-slide-up">
          <div className="flex items-center gap-2 mb-3">
            <Gauge className="w-4 h-4 text-primary-500" />
            <h2 className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">Santé du système</h2>
            <div className="ml-auto flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5 text-green-500" />
              <span className="text-[10px] text-green-600 dark:text-green-400 font-medium">Opérationnel</span>
            </div>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {health?.jvm && (
              <div className="p-3 rounded-xl bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/10 border border-blue-200/40 dark:border-blue-700/30">
                <div className="flex items-center gap-2 mb-2"><Cpu className="w-3.5 h-3.5 text-blue-500" /><span className="text-[10px] font-semibold text-blue-700 dark:text-blue-300 uppercase">JVM</span></div>
                <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{Math.round(health.jvm.heapUsed / 1024 / 1024)} Mo</p>
                <p className="text-[10px] text-gray-400">/ {Math.round(health.jvm.heapMax / 1024 / 1024)} Mo · {health.jvm.threads} threads</p>
              </div>
            )}
            {health?.database && (
              <div className="p-3 rounded-xl bg-gradient-to-br from-emerald-50 to-teal-50 dark:from-emerald-900/20 dark:to-teal-900/10 border border-emerald-200/40 dark:border-emerald-700/30">
                <div className="flex items-center gap-2 mb-2"><Database className="w-3.5 h-3.5 text-emerald-500" /><span className="text-[10px] font-semibold text-emerald-700 dark:text-emerald-300 uppercase">Base</span></div>
                <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{health.database.totalUsers} users</p>
                <p className="text-[10px] text-gray-400">{health.database.tableCount} tables · {health.database.totalSouls} âmes</p>
              </div>
            )}
            {cache?.summary && (
              <div className="p-3 rounded-xl bg-gradient-to-br from-amber-50 to-orange-50 dark:from-amber-900/20 dark:to-orange-900/10 border border-amber-200/40 dark:border-amber-700/30">
                <div className="flex items-center gap-2 mb-2"><Zap className="w-3.5 h-3.5 text-amber-500" /><span className="text-[10px] font-semibold text-amber-700 dark:text-amber-300 uppercase">Cache</span></div>
                <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{cache.summary.overallRatio}</p>
                <p className="text-[10px] text-gray-400">{cache.summary.totalHits} hits · {cache.summary.totalMisses} misses</p>
              </div>
            )}
            {health?.uptime && (
              <div className="p-3 rounded-xl bg-gradient-to-br from-purple-50 to-violet-50 dark:from-purple-900/20 dark:to-violet-900/10 border border-purple-200/40 dark:border-purple-700/30">
                <div className="flex items-center gap-2 mb-2"><Clock className="w-3.5 h-3.5 text-purple-500" /><span className="text-[10px] font-semibold text-purple-700 dark:text-purple-300 uppercase">Uptime</span></div>
                <p className="text-sm font-bold text-gray-900 dark:text-gray-100 truncate">{health.uptime}</p>
                <p className="text-[10px] text-gray-400">Fonctionnement</p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Quick stats row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        <Link to="/users" className="glass-card p-4 hover:shadow-lg hover:-translate-y-0.5 transition-all group animate-slide-up">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-blue-100 dark:bg-blue-900/30"><Users className="w-5 h-5 text-blue-500" /></div>
            <div>
              <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{health?.database?.totalUsers ?? '—'}</p>
              <p className="text-[10px] text-gray-400">Utilisateurs</p>
            </div>
            <ArrowRight className="w-4 h-4 text-gray-300 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
        </Link>
        <Link to="/audit" className="glass-card p-4 hover:shadow-lg hover:-translate-y-0.5 transition-all group animate-slide-up" style={{ animationDelay: '40ms' }}>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-gray-100 dark:bg-gray-700/50"><Activity className="w-5 h-5 text-gray-500" /></div>
            <div>
              <p className="text-xl font-bold text-gray-900 dark:text-gray-100">{recentAudit?.length ?? 0}</p>
              <p className="text-[10px] text-gray-400">Actions récentes</p>
            </div>
            <ArrowRight className="w-4 h-4 text-gray-300 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
        </Link>
        <Link to="/alerts" className="glass-card p-4 hover:shadow-lg hover:-translate-y-0.5 transition-all group animate-slide-up" style={{ animationDelay: '80ms' }}>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-red-100 dark:bg-red-900/30"><Bell className="w-5 h-5 text-red-500" /></div>
            <div>
              <p className="text-xl font-bold text-red-600 dark:text-red-400">{alertStats?.actives ?? 0}</p>
              <p className="text-[10px] text-gray-400">Alertes actives</p>
            </div>
            <ArrowRight className="w-4 h-4 text-gray-300 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
        </Link>
        <Link to="/admin/system" className="glass-card p-4 hover:shadow-lg hover:-translate-y-0.5 transition-all group animate-slide-up" style={{ animationDelay: '120ms' }}>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-emerald-100 dark:bg-emerald-900/30"><Server className="w-5 h-5 text-emerald-500" /></div>
            <div>
              <p className="text-xl font-bold text-emerald-600 dark:text-emerald-400">{health?.database?.tableCount ?? '—'}</p>
              <p className="text-[10px] text-gray-400">Tables DB</p>
            </div>
            <ArrowRight className="w-4 h-4 text-gray-300 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
          </div>
        </Link>
      </div>

      {/* Recent Audit + Quick Actions side by side */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        {/* Recent Audit */}
        <div className="lg:col-span-2 glass-card p-6 animate-slide-up" style={{ animationDelay: '140ms' }}>
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Activity className="w-4 h-4 text-gray-500" />
              <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Activité récente</h3>
            </div>
            <Link to="/audit" className="text-[10px] font-medium text-primary-600 hover:underline">Voir tout</Link>
          </div>
          <div className="space-y-2 max-h-72 overflow-y-auto">
            {recentAudit && recentAudit.length > 0 ? recentAudit.map(a => (
              <div key={a.id} className="flex items-start gap-3 p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors">
                <div className="w-7 h-7 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center text-[9px] font-bold text-white flex-shrink-0 mt-0.5">
                  {a.utilisateurNom?.charAt(0) || '?'}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-[11px] font-medium text-gray-900 dark:text-gray-100 truncate">
                    <span className="text-primary-600 dark:text-primary-400">{a.utilisateurNom}</span>
                    {' '}<span className={actionColor(a.action)}>{a.action?.toLowerCase().replace(/_/g, ' ')}</span>
                  </p>
                  <p className="text-[9px] text-gray-400">
                    {entityTypeIcon[a.entiteType] || '📝'} {a.entiteType} · {new Date(a.createdAt).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}
                  </p>
                </div>
              </div>
            )) : (
              <div className="text-center py-6"><Activity className="w-8 h-8 text-gray-300 mx-auto mb-2" /><p className="text-xs text-gray-400">Aucune activité récente</p></div>
            )}
          </div>
        </div>

        {/* Quick Actions */}
        <div className="glass-card p-6 animate-slide-up" style={{ animationDelay: '180ms' }}>
          <div className="flex items-center gap-2 mb-4">
            <Zap className="w-4 h-4 text-primary-500" />
            <h3 className="text-sm font-semibold text-gray-900 dark:text-gray-100">Actions rapides</h3>
          </div>
          <div className="space-y-2">
            {[
              { href: '/users', icon: UserPlus, label: 'Ajouter un utilisateur', color: 'text-blue-500 bg-blue-50 dark:bg-blue-900/20' },
              { href: '/permissions', icon: Shield, label: 'Gérer les rôles', color: 'text-indigo-500 bg-indigo-50 dark:bg-indigo-900/20' },
              { href: '/admin/settings', icon: Palette, label: 'Modifier le thème', color: 'text-amber-500 bg-amber-50 dark:bg-amber-900/20' },
              { href: '/admin/modules', icon: Boxes, label: 'Activer un module', color: 'text-violet-500 bg-violet-50 dark:bg-violet-900/20' },
              { href: '/admin/notifications', icon: Bell, label: 'Configurer notifications', color: 'text-rose-500 bg-rose-50 dark:bg-rose-900/20' },
              { href: '/admin/system', icon: Server, label: 'Santé du système', color: 'text-emerald-500 bg-emerald-50 dark:bg-emerald-900/20' },
            ].map(q => (
              <Link key={q.href} to={q.href} className="flex items-center gap-3 p-2.5 rounded-xl hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors group">
                <div className={`p-1.5 rounded-lg ${q.color}`}><q.icon className="w-4 h-4" /></div>
                <span className="text-xs font-medium text-gray-700 dark:text-gray-300 group-hover:text-primary-600">{q.label}</span>
                <ArrowRight className="w-3 h-3 text-gray-400 ml-auto opacity-0 group-hover:opacity-100 transition-opacity" />
              </Link>
            ))}
          </div>
        </div>
      </div>

      {/* Configuration sections */}
      <div className="space-y-8">
        {CONFIG_SECTIONS.map((section, si) => (
          <div key={section.title} className="animate-slide-up" style={{ animationDelay: `${si * 60}ms` }}>
            <div className="flex items-center gap-2 mb-3">
              <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">{section.title}</h2>
              <span className="text-[10px] text-gray-400">· {section.description}</span>
            </div>
            <div className={`grid grid-cols-1 sm:grid-cols-2 ${section.items.length >= 3 ? 'lg:grid-cols-3' : ''} gap-4`}>
              {section.items.map(s => (
                <Link key={s.href} to={s.href} className="group glass-card p-5 hover:-translate-y-1 transition-all duration-300">
                  <div className="flex items-start gap-3">
                    <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${s.gradient} text-white flex items-center justify-center shadow-md flex-shrink-0 transition-transform duration-300 group-hover:scale-110 group-hover:-rotate-3`}>
                      <s.icon className="w-5 h-5" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h3 className="text-sm font-bold text-gray-900 dark:text-gray-100 mb-0.5">{s.title}</h3>
                      <p className="text-xs text-gray-500 dark:text-gray-400 leading-relaxed">{s.desc}</p>
                    </div>
                    <ArrowRight className="w-4 h-4 text-gray-300 dark:text-gray-600 opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all duration-300 flex-shrink-0 mt-1" />
                  </div>
                </Link>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
