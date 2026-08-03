import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import {
  LayoutDashboard,
  Users,
  Heart,
  Building2,
  FileText,
  Activity,
  Bell,
  UserCog,
  X,
  CrossIcon,
  BookOpen,
  Calendar,
  FolderOpen,
  ChevronLeft,
  Sparkles,
  Church,
  BarChart3,
  AlertTriangle,
  Search,
  Shield,
  Star as StarIcon,
  User,
} from 'lucide-react';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

const navigation = [
  {
    name: 'Espace Membre',
    href: '/dashboard/membre',
    icon: User,
    roles: ['MEMBRE'],
    subtitle: 'Mes informations',
  },
  {
    name: 'Tableau de bord',
    href: '/dashboard',
    icon: LayoutDashboard,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Vue d\'ensemble',
  },
  {
    name: 'Recherche',
    href: '/search',
    icon: Search,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Recherche intelligente',
  },
  {
    name: 'Âmes',
    href: '/souls',
    icon: Heart,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Disciples suivis',
  },
  {
    name: 'Familles',
    href: '/families',
    icon: Users,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Groupes de disciples',
  },
  {
    name: 'Pilotage Pasteur',
    href: '/dashboard/pasteur',
    icon: LayoutDashboard,
    roles: ['ADMIN', 'PASTEUR'],
    subtitle: 'Centre de commandement',
  },
  {
    name: 'Dashboard Responsable',
    href: '/dashboard/responsable',
    icon: Building2,
    roles: ['PASTEUR', 'RESPONSABLE'],
    subtitle: 'Mon département',
  },
  {
    name: 'CRM Faiseur',
    href: '/crm/faiseur',
    icon: Heart,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Suivi des disciples',
  },
  {
    name: 'Dashboard Chef',
    href: '/dashboard/chef-famille',
    icon: Users,
    roles: ['PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Ma famille',
  },
  {
    name: 'Départements',
    href: '/departments',
    icon: Building2,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
    subtitle: 'Structure',
  },
  {
    name: 'Rapports',
    href: '/reports',
    icon: FileText,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Hebdomadaires',
  },
  {
    name: 'Aide urgente',
    href: '/reports/urgent-aid',
    icon: AlertTriangle,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
    subtitle: 'Demandes d\'aide',
  },
  {
    name: 'Prières',
    href: '/prayers',
    icon: BookOpen,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Sujets & témoignages',
  },
  {
    name: 'Espaces prière',
    href: '/prayers/spaces',
    icon: Shield,
    roles: ['ADMIN', 'PASTEUR'],
    subtitle: 'Par niveau de visibilité',
  },
  {
    name: 'Actions de grâce',
    href: '/prayers/actions-de-grace',
    icon: Heart,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Prières exaucées',
  },
  {
    name: 'Événements',
    href: '/events',
    icon: Calendar,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Calendrier',
  },
  {
    name: 'Programme',
    href: '/events/program',
    icon: Calendar,
    roles: ['ADMIN', 'PASTEUR'],
    subtitle: 'Programme hebdomadaire',
  },
  {
    name: 'Statistiques événements',
    href: '/events/statistics',
    icon: BarChart3,
    roles: ['PASTEUR'],
    subtitle: 'Indicateurs',
  },
  {
    name: 'Documents',
    href: '/documents',
    icon: FolderOpen,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Fichiers & rapports',
  },
  {
    name: 'Évaluations',
    href: '/evaluations',
    icon: StarIcon,
    roles: ['PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Anonymes & feedback',
  },
  {
    name: 'Retraits',
    href: '/souls/retractions',
    icon: AlertTriangle,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
    subtitle: 'Demandes de retrait',
  },
  {
    name: 'Suivis parallèles',
    href: '/parallel-followups',
    icon: Activity,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Accompagnements',
  },
  {
    name: 'Alertes',
    href: '/alerts',
    icon: Bell,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
    subtitle: 'Notifications',
  },
  {
    name: 'Utilisateurs',
    href: '/users',
    icon: UserCog,
    roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
    subtitle: 'Gestion des comptes',
  },
  {
    name: 'Audit',
    href: '/audit',
    icon: Activity,
    roles: ['ADMIN', 'PASTEUR'],
    subtitle: 'Journal de bord',
  },
  {
    name: 'Permissions',
    href: '/permissions',
    icon: UserCog,
    roles: ['ADMIN'],
    subtitle: 'Matrice des rôles',
  },
];

function NavItem({ item, collapsed = false, onClick }: { item: typeof navigation[0]; collapsed?: boolean; onClick?: () => void }) {
  const location = useLocation();
  const isActive = location.pathname === item.href || location.pathname.startsWith(item.href + '/');

  return (
    <NavLink
      to={item.href}
      onClick={onClick}
      className={`group relative flex items-center gap-3 rounded-xl text-sm font-medium transition-all duration-200 ease-smooth
        ${collapsed ? 'px-3 py-3 justify-center' : 'px-3 py-2.5'}
        ${isActive
          ? 'bg-gradient-to-r from-primary-500/10 to-primary-600/5 text-primary-700 dark:text-primary-400 shadow-sm'
          : 'text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-white/50 dark:hover:bg-gray-800/30'
        }`}
    >
      {/* Active indicator */}
      {isActive && (
        <span className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-6 bg-gradient-to-b from-primary-500 to-primary-400 rounded-r-full" />
      )}

      {/* Icon with glow on active */}
      <div className={`relative flex-shrink-0 transition-transform duration-200 group-hover:scale-110
        ${isActive ? 'text-primary-600 dark:text-primary-400' : ''}
      `}>
        <item.icon className={`w-5 h-5 transition-all duration-200 ${isActive ? 'drop-shadow-[0_0_3px_rgba(22,163,74,0.3)]' : ''}`} />
        {/* Glow dot on active */}
        {isActive && (
          <span className="absolute -top-0.5 -right-0.5 w-2 h-2">
            <span className="absolute inset-0 rounded-full bg-primary-500 animate-ping opacity-40" />
            <span className="absolute inset-0 rounded-full bg-primary-500" />
          </span>
        )}
      </div>

      {!collapsed && (
        <div className="flex-1 min-w-0">
          <span className="block truncate">{item.name}</span>
          <span className="block text-[10px] text-gray-400 dark:text-gray-500 truncate mt-0.5">
            {item.subtitle}
          </span>
        </div>
      )}

      {/* Hover tooltip for collapsed */}
      {collapsed && (
        <div className="absolute left-full ml-2 px-2.5 py-1.5 bg-gray-900 dark:bg-gray-700 text-white text-xs rounded-lg
                        opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200
                        whitespace-nowrap shadow-lg z-50">
          {item.name}
        </div>
      )}
    </NavLink>
  );
}

export default function Sidebar({ open, onClose }: SidebarProps) {
  const { user } = useAuth();
  const [collapsed, setCollapsed] = useState(false);

  // Fetch evaluation score for the current user
  const { data: myEval } = useQuery({
    queryKey: ['sidebar', 'eval'],
    queryFn: async () => {
      const res = await api.get('/evaluations/me');
      return res.data as { statistiques: Record<string, { moyenne: number | null }> };
    },
    enabled: !!user && user.activeRole !== 'ADMIN' && user.activeRole !== 'MEMBRE',
  });

  // Compute overall average from all categories
  const evalAvg = myEval && Object.keys(myEval.statistiques).length > 0
    ? Object.values(myEval.statistiques).reduce((acc, s) => acc + (s.moyenne || 0), 0) / Object.keys(myEval.statistiques).length
    : null;

  // Filter navigation by activeRole
  const activeRole = user?.activeRole || user?.role;
  const filteredNav = navigation.filter((item) => activeRole && item.roles.includes(activeRole));

  return (
    <>
      {/* Desktop sidebar */}
      <aside className={`hidden lg:fixed lg:inset-y-0 lg:z-30 lg:flex lg:flex-col transition-all duration-300 ease-smooth
        ${collapsed ? 'lg:w-20' : 'lg:w-64'}
      `}>
        <div className="flex flex-col flex-1 min-h-0 glass-strong border-r border-white/20 dark:border-white/[0.06]">
          {/* Logo */}
          <div className={`flex items-center flex-shrink-0 border-b border-white/20 dark:border-white/[0.06]
            ${collapsed ? 'justify-center h-16 px-2' : 'h-16 px-5'}
          `}>
            <div className="flex items-center gap-3">
              <div className="relative w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-glow">
                <Church className="w-5 h-5 text-white drop-shadow-sm" />
                <span className="absolute -top-1 -right-1 w-3 h-3 rounded-full bg-gold-400 border-2 border-white dark:border-gray-900 animate-pulse-soft" />
              </div>
              {!collapsed && (
                <div className="animate-fade-in">
                  <span className="text-base font-bold text-gray-900 dark:text-gray-100 font-display block leading-tight">
                    Discipolat
                  </span>
                  <span className="text-[10px] text-gray-400 dark:text-gray-500 font-medium tracking-wider uppercase">
                    Gestion
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Collapse toggle */}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="absolute -right-3 top-20 z-10 w-6 h-6 rounded-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 
                       flex items-center justify-center shadow-sm hover:shadow-md transition-all duration-200
                       hover:scale-110 active:scale-95"
          >
            <ChevronLeft className={`w-3.5 h-3.5 text-gray-400 transition-transform duration-300 ${collapsed ? 'rotate-180' : ''}`} />
          </button>

          {/* Navigation */}
          <nav className="flex-1 px-2.5 py-4 space-y-0.5 overflow-y-auto overflow-x-hidden">
            {filteredNav.map((item) => (
              <NavItem key={item.href} item={item} collapsed={collapsed} />
            ))}
          </nav>

          {/* Footer user info */}
          <div className={`flex-shrink-0 border-t border-white/20 dark:border-white/[0.06] p-3
            ${collapsed ? 'flex justify-center' : ''}
          `}>
            <div className={`flex items-center gap-3 rounded-xl transition-all duration-200
              ${collapsed ? 'justify-center p-2' : 'px-3 py-2.5 hover:bg-white/40 dark:hover:bg-gray-800/30 cursor-pointer'}
            `}>
              <div className="relative flex-shrink-0">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center shadow-sm">
                  <span className="text-sm font-bold text-white drop-shadow-sm">
                    {user?.firstName?.[0]}{user?.lastName?.[0]}
                  </span>
                </div>
                <span className="absolute -bottom-0.5 -right-0.5 w-3 h-3 rounded-full bg-green-500 border-2 border-white dark:border-gray-900" />
              </div>

              {!collapsed && (
                <div className="flex-1 min-w-0 animate-fade-in">
                  <p className="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                    {user?.firstName} {user?.lastName}
                  </p>
                  <div className="flex items-center gap-1.5 mt-0.5">
                    <span className="text-[10px] font-medium text-gray-400 dark:text-gray-500 uppercase tracking-wider">
                      {user?.activeRole === 'ADMIN' ? 'Admin' : user?.activeRole === 'PASTEUR' ? 'Pasteur' : user?.activeRole === 'RESPONSABLE' ? 'Responsable' : user?.activeRole === 'FAISEUR' ? 'Faiseur' : user?.activeRole === 'CHEF_DE_FAMILLE' ? 'Chef' : user?.role}
                    </span>
                    {user?.estChefDeFamille && (
                      <span className="px-1.5 py-0.5 text-[9px] font-semibold bg-gold-100 dark:bg-gold-900/30 text-gold-700 dark:text-gold-400 rounded-full uppercase tracking-wider">
                        Chef
                      </span>
                    )}
                  </div>
                  {evalAvg != null && (
                    <div className="flex items-center gap-1 mt-1">
                      {[1,2,3,4,5].map(i => (
                        <StarIcon key={i} className={`w-2.5 h-2.5 ${i <= Math.round(evalAvg!) ? 'fill-amber-400 text-amber-400' : 'text-gray-300 dark:text-gray-600'}`} />
                      ))}
                      <span className="text-[9px] font-medium text-gray-400 ml-0.5">{evalAvg!.toFixed(1)}</span>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </aside>

      {/* Mobile sidebar overlay */}
      <aside
        className={`fixed inset-0 z-50 lg:hidden transition-opacity duration-300 ${
          open ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
      >
        <div className="absolute inset-0 bg-black/40 dark:bg-black/60 backdrop-blur-sm" onClick={onClose} />

        <div className={`absolute left-0 top-0 bottom-0 w-72 bg-white/95 dark:bg-gray-900/95 backdrop-blur-2xl border-r border-white/20 dark:border-white/[0.06]
          shadow-2xl transform transition-transform duration-300 ease-smooth ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}>
          {/* Mobile header */}
          <div className="flex items-center justify-between h-16 px-5 border-b border-white/20 dark:border-white/[0.06]">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-primary-500 to-primary-700 flex items-center justify-center shadow-glow">
                <Church className="w-5 h-5 text-white" />
              </div>
              <span className="text-base font-bold text-gray-900 dark:text-gray-100 font-display">
                Discipolat
              </span>
            </div>
            <button
              onClick={onClose}
              className="p-2 rounded-xl hover:bg-gray-100/80 dark:hover:bg-gray-800/50 transition-colors"
            >
              <X className="w-5 h-5 text-gray-400" />
            </button>
          </div>

          {/* Mobile navigation */}
          <nav className="px-3 py-4 space-y-0.5 overflow-y-auto" style={{ height: 'calc(100% - 4rem)' }}>
            {filteredNav.map((item) => (
              <NavItem key={item.href} item={item} onClick={onClose} />
            ))}
          </nav>
        </div>
      </aside>
    </>
  );
}
