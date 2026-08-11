import { useState, useMemo } from 'react';
import { Outlet, useLocation, Navigate } from 'react-router-dom';
import Sidebar from '@/components/layout/Sidebar';
import Navbar from '@/components/layout/Navbar';
import FeedbackWidget from '@/components/beta/FeedbackWidget';
import { useSettings } from '@/contexts/SettingsContext';
import { usePlatformConfig } from '@/contexts/PlatformContext';
import { usePlatformMeta } from '@/contexts/MetaContext';
import { useAuth } from '@/contexts/AuthContext';
import { FlaskConical, X } from 'lucide-react';

const TESTER_BANNER_KEY = 'discipolat:tester-banner-dismissed';

export default function MainLayout() {
  const { branding } = useSettings();
  const { canAccessPath, isLoaded } = usePlatformConfig();
  const { meta } = usePlatformMeta();
  const { activeRole, user } = useAuth();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [testerBannerVisible, setTesterBannerVisible] = useState(
    () => localStorage.getItem(TESTER_BANNER_KEY) !== '1'
  );

  // Gating de module : redirige vers /module-unavailable si le module du
  // chemin courant est désactivé par l'administration.
  // On n'applique le gating qu'après le premier chargement des données
  // plateforme (isLoaded vrai) pour éviter une fausse redirection au montage.
  const gated = useMemo(() => {
    if (!isLoaded) return null;
    return canAccessPath(location.pathname) ? null : <Navigate to="/module-unavailable" replace />;
  }, [location.pathname, canAccessPath, isLoaded]);

  if (gated) return gated;

  return (
    <div className="min-h-screen flex bg-gradient-mesh">
      {/* Sidebar */}
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main content area */}
      <div className="flex-1 flex flex-col min-w-0 lg:ml-64 transition-all duration-300">
        <Navbar onMenuClick={() => setSidebarOpen(true)} />

        {/* Bandeau mode testeur — environnement bêta uniquement, masquable */}
        {meta.betaMode && testerBannerVisible && (
          <div className="mx-4 sm:mx-6 lg:mx-8 mt-4 animate-slide-up">
            <div className="flex items-center gap-3 px-4 py-2.5 rounded-xl border border-amber-500/25 bg-gradient-to-r from-amber-500/10 via-amber-500/5 to-transparent">
              <FlaskConical className="w-4 h-4 text-amber-500 flex-shrink-0" />
              <p className="text-xs text-amber-700 dark:text-amber-300 flex-1">
                <span className="font-semibold">Environnement de test public</span>
                {' '}— toutes les données sont fictives. Rôle actif :{' '}
                <span className="font-semibold">{activeRole || user?.role || '—'}</span>
                . Vous pouvez changer de rôle depuis votre profil, et signaler tout problème via le bouton
                « Un retour ? » en bas à droite.
              </p>
              <button
                onClick={() => {
                  localStorage.setItem(TESTER_BANNER_KEY, '1');
                  setTesterBannerVisible(false);
                }}
                className="p-1 rounded-lg text-amber-600/70 hover:text-amber-500 hover:bg-amber-500/10 transition-colors"
                aria-label="Masquer le bandeau"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        )}

        <main className="flex-1 p-4 sm:p-6 lg:p-8 animate-fade-in">
          <Outlet />
        </main>

        {/* Widget de feedback (environnement bêta) */}
        <FeedbackWidget />

        {/* Footer */}
        <footer className="relative px-6 sm:px-8 py-4">
          {/* Top gradient line */}
          <div className="absolute top-0 left-4 right-4 h-[1px] bg-gradient-to-r from-transparent via-gray-200/50 dark:via-gray-700/30 to-transparent" />
          <div className="flex items-center justify-between">
            <p className="text-xs text-gray-400 dark:text-gray-500">
              {branding.platformName} &copy; {new Date().getFullYear()} — {branding.churchName}
            </p>
            <div className="flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse-soft" />
              <span className="text-[10px] text-gray-400 dark:text-gray-500">Système opérationnel</span>
            </div>
          </div>
        </footer>
      </div>
    </div>
  );
}
