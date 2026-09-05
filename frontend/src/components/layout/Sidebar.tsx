import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import { X, ChevronLeft, Church, Star as StarIcon } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { navForRole, ROLE_META, type WorkspaceNavItem } from '@/workspaces';
import { filterNavByRole } from '@/lib/routeAccess';
import { useSettings } from '@/contexts/SettingsContext';
import { usePlatformConfig, menusToSections } from '@/contexts/PlatformContext';
import { resolveIcon } from '@/lib/menuIcons';
import type { MenuEntry } from '@/types';
import { useI18n } from '@/i18n';
import { navKeyMap } from '@/i18n/navKeys';

interface NavItemData {
  name: string;
  href: string;
  icon: LucideIcon;
  subtitle: string;
}

interface NavSectionData {
  title: string;
  items: NavItemData[];
}

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

function NavItem({ item, collapsed = false, onClick, t }: { item: NavItemData; collapsed?: boolean; onClick?: () => void; t: (key: string) => string }) {
  const location = useLocation();
  const isActive = location.pathname === item.href || location.pathname.startsWith(item.href + '/');
  const label = t(navKeyMap[item.name] ?? item.name);
  const sub = t(navKeyMap[item.subtitle] ?? item.subtitle);

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
          <span className="block truncate">{label}</span>
          <span className="block text-[10px] text-gray-400 dark:text-gray-500 truncate mt-0.5">
            {sub}
          </span>
        </div>
      )}

      {/* Hover tooltip for collapsed */}
      {collapsed && (
        <div className="absolute left-full ml-2 px-2.5 py-1.5 bg-gray-900 dark:bg-gray-700 text-white text-xs rounded-lg
                        opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200
                        whitespace-nowrap shadow-lg z-50">
          {label}
        </div>
      )}
    </NavLink>
  );
}

function menuToNav(menu: MenuEntry): NavItemData {
  return { name: menu.label, href: menu.href, icon: resolveIcon(menu.icon), subtitle: '' };
}

export default function Sidebar({ open, onClose }: SidebarProps) {
  const { user } = useAuth();
  const { branding } = useSettings();
  const { menus: configMenus } = usePlatformConfig();
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const { t } = useI18n();

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

  // Espace métier du rôle actif : menus strictement dédiés au métier.
  // Les menus sont pilotés par la configuration (backend) quand elle est
  // disponible ; sinon repli sur la navigation statique (dégradé sans régression).
  // FIX: les menus configurables sont TOUJOURS filtrés selon le rôle actif —
  // sans ce filtre, un compte dont le rôle actif est FAISEUR voyait des menus
  // Responsable/Admin qui rebondissaient vers son propre espace (boutons morts).
  const activeRole = user?.activeRole || user?.role || 'FAISEUR';
  const workspaceSections: NavSectionData[] = configMenus.length > 0
    ? (() => {
        const sections = menusToSections(configMenus).map((s) => ({
          title: s.title,
          items: filterNavByRole(s.items.map(menuToNav), activeRole),
        }));
        const nonEmpty = sections.filter((s) => s.items.length > 0);
        // Si la configuration ne laisse aucun menu accessible pour ce rôle,
        // repli sur la navigation statique du rôle (jamais vide).
        return nonEmpty.length > 0 ? nonEmpty : navForRole(activeRole) as NavSectionData[];
      })()
    : navForRole(activeRole).map((s) => ({
        title: s.title,
        items: filterNavByRole(s.items, activeRole),
      })).filter((s) => s.items.length > 0) as NavSectionData[];
  const meta = ROLE_META[activeRole as keyof typeof ROLE_META] || ROLE_META.FAISEUR;

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
                {branding.logoUrl ? (
                  <img src={branding.logoUrl} alt="" className="w-5 h-5 object-contain drop-shadow-sm" />
                ) : (
                  <Church className="w-5 h-5 text-white drop-shadow-sm" />
                )}
                <span className="absolute -top-1 -right-1 w-3 h-3 rounded-full bg-gold-400 border-2 border-white dark:border-gray-900 animate-pulse-soft" />
              </div>
              {!collapsed && (
                <div className="animate-fade-in">
                  <span className="text-base font-bold text-gray-900 dark:text-gray-100 font-display block leading-tight">
                    {branding.platformName}
                  </span>
                  <span className="text-[10px] text-gray-400 dark:text-gray-500 font-medium tracking-wider uppercase">
                    {branding.churchName}
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* Collapse toggle */}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="absolute -right-3 top-[8.5rem] z-10 w-6 h-6 rounded-full bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 
                       flex items-center justify-center shadow-sm hover:shadow-md transition-all duration-200
                       hover:scale-110 active:scale-95"
          >
            <ChevronLeft className={`w-3.5 h-3.5 text-gray-400 transition-transform duration-300 ${collapsed ? 'rotate-180' : ''}`} />
          </button>

          {/* Bandeau espace métier (desktop) */}
          <div className={`flex-shrink-0 px-3 pt-3 ${collapsed ? 'flex justify-center' : ''}`}>
            <div className={`flex items-center gap-2.5 rounded-xl px-3 py-2.5 border border-primary-500/15 dark:border-white/[0.06] bg-gradient-to-r from-primary-500/10 to-gold-500/5
              ${collapsed ? 'justify-center p-2.5' : ''}`}>
              <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${meta.gradient} flex items-center justify-center shadow-sm flex-shrink-0`}>
                <Church className="w-4 h-4 text-white drop-shadow-sm" />
              </div>
              {!collapsed && (
                <div className="min-w-0 animate-fade-in">
                  <p className="text-xs font-bold text-gray-900 dark:text-gray-100 truncate leading-tight">{t(`role.${activeRole}.label`) || meta.label}</p>
                  <p className="text-[10px] text-gray-400 dark:text-gray-500 truncate">{t(`role.${activeRole}.tagline`) || meta.tagline}</p>
                </div>
              )}
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-2.5 py-3 overflow-y-auto overflow-x-hidden">
            {workspaceSections.map((section) => (
              <div key={section.title} className="mb-1">
                {!collapsed && (
                  <p className="px-3 pt-3 pb-1.5 text-[10px] font-semibold uppercase tracking-wider text-gray-400/90 dark:text-gray-500">
                    {t(navKeyMap[section.title] ?? section.title)}
                  </p>
                )}
                {collapsed && <div className="pt-3" />}
                {section.items.map((item) => (
                  <NavItem key={item.href} item={item} collapsed={collapsed} t={t} />
                ))}
              </div>
            ))}
          </nav>

          {/* Footer user info */}
          <div className={`flex-shrink-0 border-t border-white/20 dark:border-white/[0.06] p-3
            ${collapsed ? 'flex justify-center' : ''}
          `}>
            <div
              onClick={() => { onClose(); navigate('/profile'); }}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onClose(); navigate('/profile'); } }}
              className={`flex items-center gap-3 rounded-xl transition-all duration-200 cursor-pointer
                ${collapsed ? 'justify-center p-2' : 'px-3 py-2.5 hover:bg-white/40 dark:hover:bg-gray-800/30'}
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
                      {t(`role.${user?.activeRole}.label`) || user?.activeRole === 'ADMIN' ? 'Admin' : user?.activeRole === 'PASTEUR' ? 'Pasteur' : user?.activeRole === 'RESPONSABLE' ? 'Responsable' : user?.activeRole === 'FAISEUR' ? 'Faiseur' : user?.activeRole === 'CHEF_DE_FAMILLE' ? 'Chef' : user?.role}
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
                {branding.logoUrl ? (
                  <img src={branding.logoUrl} alt="" className="w-5 h-5 object-contain" />
                ) : (
                  <Church className="w-5 h-5 text-white" />
                )}
              </div>
              <span className="text-base font-bold text-gray-900 dark:text-gray-100 font-display">
                {branding.platformName}
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
          <nav className="px-3 py-3 overflow-y-auto" style={{ height: 'calc(100% - 4rem)' }}>
            {/* Bandeau espace métier */}
            <div className="flex items-center gap-2.5 mx-1 mt-1 mb-3 px-3 py-2.5 rounded-xl bg-gradient-to-r from-primary-500/10 to-gold-500/5 border border-primary-500/15 dark:border-white/[0.06]">
              <div className={`w-8 h-8 rounded-lg bg-gradient-to-br ${meta.gradient} flex items-center justify-center shadow-sm flex-shrink-0`}>
                <Church className="w-4 h-4 text-white" />
              </div>
              <div className="min-w-0">
                <p className="text-xs font-bold text-gray-900 dark:text-gray-100 truncate">{t(`role.${activeRole}.label`) || meta.label}</p>
                <p className="text-[10px] text-gray-400 dark:text-gray-500 truncate">{t(`role.${activeRole}.tagline`) || meta.tagline}</p>
              </div>
            </div>
            {workspaceSections.map((section) => (
              <div key={section.title} className="mb-1">
                <p className="px-3 pt-2.5 pb-1.5 text-[10px] font-semibold uppercase tracking-wider text-gray-400/90 dark:text-gray-500">
                  {t(navKeyMap[section.title] ?? section.title)}
                </p>
                {section.items.map((item) => (
                  <NavItem key={item.href} item={item} onClick={onClose} t={t} />
                ))}
              </div>
            ))}
          </nav>
        </div>
      </aside>
    </>
  );
}
