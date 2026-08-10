import { useState, useMemo } from 'react';
import { Outlet, useLocation, Navigate } from 'react-router-dom';
import Sidebar from '@/components/layout/Sidebar';
import Navbar from '@/components/layout/Navbar';
import { useSettings } from '@/contexts/SettingsContext';
import { usePlatformConfig } from '@/contexts/PlatformContext';

export default function MainLayout() {
  const { branding } = useSettings();
  const { canAccessPath, isLoaded } = usePlatformConfig();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

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

        <main className="flex-1 p-4 sm:p-6 lg:p-8 animate-fade-in">
          <Outlet />
        </main>

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
