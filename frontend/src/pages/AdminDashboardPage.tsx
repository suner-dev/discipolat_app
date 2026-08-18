import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  Palette, Boxes, Menu as MenuIcon, Shield, UserCog, Activity, FileText,
  ArrowRight, Sparkles, MessageSquareText, BookOpen, LayoutTemplate, Building2,
  Users, Heart, Bell, UserCheck, AlertTriangle, TrendingUp, BarChart3,
} from 'lucide-react';
import { useSettings } from '@/contexts/SettingsContext';
import type { Tenant } from '@/types';
import {
  ResponsiveContainer, Tooltip,
  PieChart, Pie, Cell,
} from 'recharts';

const COLORS = ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'];

const ADMIN_SECTIONS = [
  { href: '/admin/tenants', icon: Building2, title: 'Églises (tenants)', desc: 'Gérer les églises de la plateforme : création, statut, plan et isolation multi-tenant.', gradient: 'from-emerald-500 to-teal-600' },
  { href: '/admin/notifications', icon: Bell, title: 'Notifications', desc: 'Personnaliser les modèles de notification : titres, messages, canaux et rôles destinataires.', gradient: 'from-rose-500 to-red-600' },
  { href: '/admin/settings', icon: Palette, title: 'Identité & marque', desc: 'Nom, logo, couleurs, typographie et coordonnées de l\'église.', gradient: 'from-primary-500 to-emerald-600' },
  { href: '/admin/modules', icon: Boxes, title: 'Modules', desc: 'Activer ou désactiver les grands modules de la plateforme.', gradient: 'from-violet-500 to-purple-600' },
  { href: '/admin/menus', icon: MenuIcon, title: 'Menus', desc: 'Configurer la navigation : ordre, libellé, icônes et rôles visibles.', gradient: 'from-amber-500 to-orange-600' },
  { href: '/admin/pages', icon: LayoutTemplate, title: 'Pages', desc: 'Créer des pages personnalisées avec tableaux, graphiques, formulaires et widgets.', gradient: 'from-teal-500 to-cyan-600' },
  { href: '/admin/custom-fields', icon: FileText, title: 'Champs personnalisés', desc: 'Ajouter des champs aux entités (âmes, utilisateurs, départements, familles).', gradient: 'from-sky-500 to-blue-600' },
  { href: '/admin/dictionaries', icon: BookOpen, title: 'Dictionnaires', desc: 'Types d\'événement, statuts, raisons d\'absence et catégories — adaptez chaque liste.', gradient: 'from-fuchsia-500 to-pink-600' },
  { href: '/permissions', icon: Shield, title: 'Rôles & permissions', desc: 'Gérer les rôles, créer des rôles personnalisés et éditer la matrice.', gradient: 'from-indigo-500 to-violet-600' },
  { href: '/users', icon: UserCog, title: 'Utilisateurs', desc: 'Créer, modifier et gérer les comptes utilisateurs.', gradient: 'from-teal-500 to-cyan-600' },
  { href: '/admin/feedback', icon: MessageSquareText, title: 'Retours testeurs', desc: 'Bugs, suggestions et retours UX des testeurs — suivi et statuts.', gradient: 'from-blue-500 to-indigo-600' },
  { href: '/audit', icon: Activity, title: 'Audit', desc: 'Consulter le journal de bord complet des actions système.', gradient: 'from-gray-500 to-slate-600' },
];

export default function AdminDashboardPage() {
  const { branding } = useSettings();

  const { data: summary } = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: async () => {
      const res = await api.get('/dashboard/summary');
      return res.data as {
        totalSouls: number; nouveauxArrivants: number; nouveauxConvertis: number;
        soulsActives: number; soulsEnIntegration: number; totalFaiseurs: number;
        totalChefsDeFamille: number; totalFamilles: number; activeAlerts: number;
        suivisParallelesActifs: number;
      };
    },
  });

  const { data: tenants } = useQuery({
    queryKey: ['admin', 'tenants'],
    queryFn: async () => {
      const res = await api.get('/tenants');
      return res.data as Tenant[];
    },
  });

  const { data: alertStats } = useQuery({
    queryKey: ['alerts', 'stats'],
    queryFn: async () => {
      const res = await api.get('/alerts/stats');
      return res.data as { actives: number; traitees: number; resolues: number; total: number };
    },
  });

  const activeTenants = tenants?.filter(t => t.status === 'ACTIVE').length ?? 0;
  const totalTenants = tenants?.length ?? 0;

  const soulDistribution = summary ? [
    { name: 'Actifs', value: summary.soulsActives },
    { name: 'En intégration', value: summary.soulsEnIntegration },
    { name: 'Nouveaux arrivants', value: summary.nouveauxArrivants },
    { name: 'Nouveaux convertis', value: summary.nouveauxConvertis },
  ].filter(d => d.value > 0) : [];

  return (
    <div className="page-container max-w-6xl">
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-1">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 text-white flex items-center justify-center shadow-lg">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-gray-100 font-display tracking-tight">
              Administration
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
              Centre de configuration de {branding.platformName} — tout paramétrer sans écrire de code.
            </p>
          </div>
        </div>
      </div>

      {/* Métriques santé */}
      {summary && (
        <div className="mb-8">
          <div className="flex items-center gap-2 mb-4">
            <BarChart3 className="w-4 h-4 text-primary-500" />
            <h2 className="text-sm font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider">
              Santé de la plateforme
            </h2>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 mb-4">
            {[
              { label: 'Total âmes', value: summary.totalSouls, icon: Heart, color: 'from-rose-500 to-pink-500', route: '/souls' },
              { label: 'Familles', value: summary.totalFamilles, icon: Users, color: 'from-blue-500 to-indigo-500', route: '/families' },
              { label: 'Faiseurs', value: summary.totalFaiseurs, icon: UserCheck, color: 'from-emerald-500 to-teal-500', route: '/users' },
              { label: 'Alertes actives', value: summary.activeAlerts, icon: AlertTriangle, color: summary.activeAlerts > 0 ? 'from-red-500 to-rose-500' : 'from-green-500 to-emerald-500', route: '/alerts' },
              { label: 'Églises (tenants)', value: `${activeTenants}/${totalTenants}`, icon: Building2, color: 'from-violet-500 to-purple-500', route: '/admin/tenants' },
            ].map((stat, i) => {
              const Icon = stat.icon;
              return (
                <Link key={stat.label} to={stat.route} className="stat-card animate-slide-up text-left hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200"
                  style={{ animationDelay: `${i * 50}ms` }}>
                  <div className={`absolute top-0 left-0 right-0 h-0.5 bg-gradient-to-r ${stat.color} opacity-60`} />
                  <div className="flex items-start justify-between mb-2">
                    <span className="stat-label">{stat.label}</span>
                    <div className={`p-1.5 rounded-lg bg-gradient-to-br ${stat.color} text-white shadow-sm`}>
                      <Icon className="w-3.5 h-3.5" />
                    </div>
                  </div>
                  <p className="stat-value text-xl">{stat.value}</p>
                </Link>
              );
            })}
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            {/* Répartition âmes */}
            {soulDistribution.length > 0 && (
              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '250ms' }}>
                <h3 className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-3">Répartition âmes</h3>
                <div className="h-36">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie data={soulDistribution} cx="50%" cy="50%" innerRadius={22} outerRadius={50}
                        paddingAngle={3} dataKey="value" strokeWidth={0}>
                        {soulDistribution.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                      </Pie>
                      <Tooltip formatter={(v: number, name: string) => [v, name]} contentStyle={{ fontSize: 11, borderRadius: 8 }} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="flex flex-wrap gap-2 mt-2">
                  {soulDistribution.map((d, i) => (
                    <span key={d.name} className="flex items-center gap-1 text-[9px] text-gray-500">
                      <span className="w-2 h-2 rounded-full" style={{ backgroundColor: COLORS[i % COLORS.length] }} />
                      {d.name} ({d.value})
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Alertes globales */}
            {alertStats && (
              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '300ms' }}>
                <h3 className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-3">Alertes globales</h3>
                <div className="space-y-3">
                  {[
                    { label: 'Actives', value: alertStats.actives, color: 'text-red-500' },
                    { label: 'Traitées', value: alertStats.traitees, color: 'text-amber-500' },
                    { label: 'Résolues', value: alertStats.resolues, color: 'text-green-500' },
                  ].map(a => (
                    <div key={a.label} className="flex items-center justify-between p-2.5 rounded-xl bg-gray-50/50 dark:bg-gray-800/30">
                      <span className="text-xs text-gray-400">{a.label}</span>
                      <span className={`text-sm font-bold ${a.color}`}>{a.value}</span>
                    </div>
                  ))}
                  <div className="pt-2 border-t border-gray-100 dark:border-gray-700/30">
                    <div className="flex items-center justify-between text-[10px] text-gray-400">
                      <span>Total</span>
                      <span className="font-semibold">{alertStats.total}</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Tenants */}
            {tenants && tenants.length > 0 && (
              <div className="glass-card p-5 animate-slide-up" style={{ animationDelay: '350ms' }}>
                <h3 className="text-xs font-semibold text-gray-700 dark:text-gray-300 uppercase tracking-wider mb-3">Églises</h3>
                <div className="space-y-2 max-h-40 overflow-y-auto">
                  {tenants.map(t => (
                    <Link key={t.id} to="/admin/tenants" className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50/50 dark:hover:bg-gray-800/30 transition-colors">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center text-[10px] font-bold text-white">
                          {t.name.charAt(0)}
                        </div>
                        <div>
                          <p className="text-xs font-medium text-gray-900 dark:text-gray-100">{t.name}</p>
                          <p className="text-[9px] text-gray-400">{t.plan} · {t.slug}</p>
                        </div>
                      </div>
                      <span className={`badge text-[9px] ${t.status === 'ACTIVE' ? 'badge-success' : t.status === 'SUSPENDED' ? 'badge-warning' : 'badge-info'}`}>
                        {t.status === 'ACTIVE' ? 'Active' : t.status === 'SUSPENDED' ? 'Suspendue' : t.status === 'CANCELLED' ? 'Annulée' : 'En attente'}
                      </span>
                    </Link>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {ADMIN_SECTIONS.map((s) => (
          <Link key={s.href} to={s.href} className="group glass-card p-6 hover:-translate-y-1.5 transition-all duration-300">
            <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${s.gradient} text-white flex items-center justify-center shadow-lg mb-4 transition-transform duration-300 group-hover:scale-110 group-hover:-rotate-6`}>
              <s.icon className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-gray-900 dark:text-gray-100 mb-1">{s.title}</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed mb-4">{s.desc}</p>
            <div className="flex items-center gap-1 text-xs font-medium text-primary-600 dark:text-primary-400 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-x-[-6px] group-hover:translate-x-0">
              Accéder <ArrowRight className="w-3.5 h-3.5" />
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
