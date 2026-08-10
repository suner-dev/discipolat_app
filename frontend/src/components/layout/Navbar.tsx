import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth, roleLabels } from '@/contexts/AuthContext';
import {
  Menu,
  Sun,
  Moon,
  Bell,
  BellRing,
  LogOut,
  User,
  RotateCw,
  ChevronDown,
  Settings,
  Sparkles,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import { WORKSPACE_HOME, ROLE_META, roleIcon } from '@/workspaces';
import { useTheme } from '@/hooks/useTheme';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Notification } from '@/types';

interface NavbarProps {
  onMenuClick: () => void;
}

export default function Navbar({ onMenuClick }: NavbarProps) {
  const navigate = useNavigate();
  const { darkMode, toggleTheme } = useTheme();
  const { user, logout, switchRole, roles, activeRole } = useAuth();
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showRoleMenu, setShowRoleMenu] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const [transitionRole, setTransitionRole] = useState<string | null>(null);
  const transitionTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const roleMenuRef = useRef<HTMLDivElement>(null);

  // Track scroll for shadow effect
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 10);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Close menus on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowProfileMenu(false);
      }
      if (roleMenuRef.current && !roleMenuRef.current.contains(e.target as Node)) {
        setShowRoleMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const { data: notifications } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: async () => {
      const res = await api.get('/notifications?size=5&sort=createdAt,desc');
      return res.data.content as Notification[];
    },
    refetchInterval: 60000,
    enabled: !!user,
  });

  const unreadCount = notifications?.filter((n) => !n.lu).length || 0;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleRoleSwitch = async (newRole: string) => {
    setShowRoleMenu(false);
    setShowProfileMenu(false);
    await switchRole(newRole);

    // Transition vers le nouvel espace métier : overlay + redirection vers son dashboard.
    setTransitionRole(newRole);
    navigate(WORKSPACE_HOME[newRole as keyof typeof WORKSPACE_HOME] || '/dashboard');
    if (transitionTimer.current) clearTimeout(transitionTimer.current);
    transitionTimer.current = setTimeout(() => setTransitionRole(null), 1400);
  };

  // Nettoyage du timer de transition
  useEffect(() => {
    return () => {
      if (transitionTimer.current) clearTimeout(transitionTimer.current);
    };
  }, []);

  const roleColor = (r: string) => {
    switch (r) {
      case 'ADMIN': return 'bg-red-500/20 text-red-400 border-red-500/30';
      case 'PASTEUR': return 'bg-primary-500/20 text-primary-400 border-primary-500/30';
      case 'RESPONSABLE': return 'bg-amber-500/20 text-amber-400 border-amber-500/30';
      case 'FAISEUR': return 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30';
      case 'CHEF_DE_FAMILLE': return 'bg-gold-500/20 text-gold-400 border-gold-500/30';
      default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  return (
    <header
      className={`sticky top-0 z-20 flex-shrink-0 transition-all duration-300 ease-smooth
        ${scrolled
          ? 'bg-white/80 dark:bg-gray-900/80 backdrop-blur-2xl shadow-glass'
          : 'bg-white/60 dark:bg-gray-900/60 backdrop-blur-md'
        }
      `}
    >
      {/* Top gradient line */}
      <div className="absolute top-0 left-0 right-0 h-[1px] bg-gradient-to-r from-transparent via-primary-500/30 to-transparent" />

      {/* Overlay de transition entre espaces métiers */}
      {transitionRole && (() => {
        const meta = ROLE_META[transitionRole as keyof typeof ROLE_META] || ROLE_META.FAISEUR;
        const Icon = roleIcon(transitionRole);
        return (
          <div className="fixed inset-0 z-[100] flex items-center justify-center bg-gray-950/70 dark:bg-gray-950/80 backdrop-blur-xl animate-fade-in">
            <div className="flex flex-col items-center gap-3 animate-scale-in">
              <div className="relative">
                <span className={`absolute inset-0 rounded-2xl bg-gradient-to-br ${meta.gradient} opacity-40 animate-ping`} />
                <div className={`relative w-20 h-20 rounded-2xl bg-gradient-to-br ${meta.gradient} flex items-center justify-center shadow-glow`}>
                  <Icon className="w-9 h-9 text-white drop-shadow-sm" />
                </div>
              </div>
              <p className="text-xs uppercase tracking-[0.2em] text-gray-400 font-medium mt-1">Espace métier</p>
              <p className="text-2xl font-bold text-white font-display">{meta.label}</p>
              <p className="flex items-center gap-1.5 text-xs font-semibold text-primary-400">
                <CheckCircle2 className="w-4 h-4" />
                {meta.tagline}
              </p>
            </div>
          </div>
        );
      })()}

      <div className="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
        {/* Left section */}
        <div className="flex items-center gap-3">
          <button
            onClick={onMenuClick}
            className="lg:hidden p-2 rounded-xl text-gray-400 hover:bg-gray-100/80 dark:hover:bg-gray-800/50
                       transition-all duration-200 hover:scale-105 active:scale-95"
            aria-label="Menu"
          >
            <Menu className="w-5 h-5" />
          </button>

          <div className="hidden sm:flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-primary-500" />
            <span className="text-sm text-gray-500 dark:text-gray-400">
              Bon retour, <span className="font-semibold text-gray-900 dark:text-gray-100">{user?.firstName}</span>
            </span>
          </div>
        </div>

        {/* Right section */}
        <div className="flex items-center gap-1 sm:gap-2">
          {/* Dark mode toggle */}
          <button
            onClick={toggleTheme}
            className="relative p-2.5 rounded-xl text-gray-400 hover:text-gray-600 dark:hover:text-gray-300
                       hover:bg-gray-100/80 dark:hover:bg-gray-800/50 transition-all duration-200
                       hover:scale-105 active:scale-95 group"
            title={darkMode ? 'Mode clair' : 'Mode sombre'}
          >
            {darkMode ? (
              <Sun className="w-[18px] h-[18px] transition-transform duration-500 group-hover:rotate-90" />
            ) : (
              <Moon className="w-[18px] h-[18px] transition-transform duration-500 group-hover:-rotate-12" />
            )}
          </button>

          {/* Notifications */}
          <Link
            to="/alerts"
            className="relative p-2.5 rounded-xl text-gray-400 hover:text-gray-600 dark:hover:text-gray-300
                       hover:bg-gray-100/80 dark:hover:bg-gray-800/50 transition-all duration-200
                       hover:scale-105 active:scale-95 group"
          >
            {unreadCount > 0 ? (
              <BellRing className="w-4.5 h-4.5 animate-pulse-soft" />
            ) : (
              <Bell className="w-4.5 h-4.5" />
            )}

            {unreadCount > 0 && (
              <span className="absolute -top-0.5 -right-0.5 flex items-center justify-center
                             min-w-[18px] h-[18px] px-1
                             bg-gradient-to-r from-red-500 to-red-400 text-white text-[10px] font-bold
                             rounded-full shadow-lg shadow-red-500/30
                             animate-bounce-in">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </Link>

          {/* Profile dropdown */}
          <div className="relative" ref={menuRef}>
            <button
              onClick={() => setShowProfileMenu(!showProfileMenu)}
              className="flex items-center gap-2 p-1.5 pr-3 rounded-xl
                         hover:bg-gray-100/80 dark:hover:bg-gray-800/50
                         transition-all duration-200 hover:scale-[1.02] active:scale-[0.98]
                         border border-transparent hover:border-gray-200/50 dark:hover:border-gray-700/30"
            >
              <div className="relative">
                <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 
                              flex items-center justify-center shadow-sm">
                  <span className="text-xs font-bold text-white drop-shadow-sm">
                    {user?.firstName?.[0]}
                  </span>
                </div>
                <span className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full bg-green-500 border-2 border-white dark:border-gray-900" />
              </div>
              <ChevronDown className={`w-3.5 h-3.5 text-gray-400 transition-transform duration-200 ${showProfileMenu ? 'rotate-180' : ''}`} />
            </button>

            {showProfileMenu && (
              <div className="absolute right-0 mt-2 w-56 glass-strong rounded-2xl shadow-glass-lg border border-white/20 dark:border-white/[0.06]
                            py-1.5 animate-scale-in overflow-hidden">
                {/* User preview */}
                <div className="px-4 py-3 border-b border-gray-100/50 dark:border-gray-700/30">
                  <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                    {user?.firstName} {user?.lastName}
                  </p>
                  <p className="text-xs text-gray-400">{user?.email}</p>
                  {/* Active role badge */}
                  <div className="mt-2 flex items-center gap-1.5">
                    <ShieldCheck className="w-3 h-3 text-primary-400" />
                    <span className={`text-[10px] font-semibold uppercase tracking-wider px-1.5 py-0.5 rounded-full border ${roleColor(activeRole || user?.role || '')}`}>
                      {roleLabels[activeRole || user?.role || ''] || activeRole || user?.role}
                    </span>
                  </div>
                </div>

                <div className="py-1">
                  <Link
                    to="/profile"
                    className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300
                             hover:bg-gray-100/60 dark:hover:bg-gray-800/40 transition-colors"
                    onClick={() => setShowProfileMenu(false)}
                  >
                    <User className="w-4 h-4 text-gray-400" />
                    Mon profil
                  </Link>
                  <Link
                    to="/profile"
                    className="flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300
                             hover:bg-gray-100/60 dark:hover:bg-gray-800/40 transition-colors"
                    onClick={() => setShowProfileMenu(false)}
                  >
                    <Settings className="w-4 h-4 text-gray-400" />
                    Paramètres
                  </Link>

                  {/* Role Switcher — only show if user has multiple roles */}
                  {roles && roles.length > 1 && (
                    <div ref={roleMenuRef} className="border-t border-gray-100/50 dark:border-gray-700/30 mt-1 pt-1">
                      <button
                        onClick={(e) => { e.stopPropagation(); setShowRoleMenu(!showRoleMenu); }}
                        className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-gray-700 dark:text-gray-300
                                 hover:bg-gray-100/60 dark:hover:bg-gray-800/40 transition-colors"
                      >
                        <RotateCw className="w-4 h-4 text-gray-400" />
                        <span className="flex-1 text-left">Changer de rôle</span>
                        <ChevronDown className={`w-3 h-3 text-gray-400 transition-transform ${showRoleMenu ? 'rotate-180' : ''}`} />
                      </button>

                      {showRoleMenu && (
                        <div className="px-2 pb-1 space-y-0.5 animate-slide-up">
                          {roles.map((r: string) => (
                            <button
                              key={r}
                              onClick={() => handleRoleSwitch(r)}
                              disabled={r === (activeRole || user?.role)}
                              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs font-medium transition-all
                                ${r === (activeRole || user?.role)
                                  ? 'bg-primary-500/10 text-primary-400 cursor-default'
                                  : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800/40'
                                }`}
                            >
                              <span className={`w-2 h-2 rounded-full ${r === (activeRole || user?.role) ? 'bg-primary-400' : 'bg-gray-500'}`} />
                              <span className="flex-1 text-left">{roleLabels[r] || r}</span>
                              {r === (activeRole || user?.role) && (
                                <span className="text-[9px] text-primary-400 font-semibold">ACTIF</span>
                              )}
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <div className="border-t border-gray-100/50 dark:border-gray-700/30 pt-1">
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-red-600 dark:text-red-400
                             hover:bg-red-50/50 dark:hover:bg-red-900/20 transition-colors"
                  >
                    <LogOut className="w-4 h-4" />
                    Déconnexion
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
