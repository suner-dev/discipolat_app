import { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import {
  Menu,
  Sun,
  Moon,
  Bell,
  BellRing,
  LogOut,
  User,
  ChevronDown,
  Settings,
  Sparkles,
} from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import api from '@/lib/api';
import type { Notification } from '@/types';

interface NavbarProps {
  onMenuClick: () => void;
}

export default function Navbar({ onMenuClick }: NavbarProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [darkMode, setDarkMode] = useState(() => {
    if (typeof window !== 'undefined') {
      return localStorage.getItem('darkMode') === 'true';
    }
    return false;
  });
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Track scroll for shadow effect
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 10);
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Close menu on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowProfileMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    if (darkMode) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
    localStorage.setItem('darkMode', String(darkMode));
  }, [darkMode]);

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
            onClick={() => setDarkMode(!darkMode)}
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
