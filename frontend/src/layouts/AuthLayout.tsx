import { Outlet } from 'react-router-dom';
import { Church, Sparkles, Sun, Moon } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useSettings } from '@/contexts/SettingsContext';

export default function AuthLayout() {
  const { darkMode, toggleTheme } = useTheme();
  const { branding } = useSettings();

  return (
    <div className="relative min-h-screen flex items-center justify-center overflow-hidden bg-gradient-to-br from-gray-100 via-white to-gray-100 dark:from-gray-950 dark:via-gray-900 dark:to-gray-950 transition-colors duration-500">
      {/* Animated background gradient */}
      <div className="absolute inset-0 bg-gradient-mesh animate-gradient-shift bg-[length:200%_200%]" />

      {/* Decorative particles */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {[...Array(6)].map((_, i) => (
          <div
            key={i}
            className="particle"
            style={{
              width: `${Math.random() * 4 + 2}px`,
              height: `${Math.random() * 4 + 2}px`,
              left: `${Math.random() * 100}%`,
              bottom: '-10px',
              animation: `particleDrift ${Math.random() * 15 + 10}s linear infinite`,
              animationDelay: `${Math.random() * 10}s`,
            }}
          />
        ))}
      </div>

      {/* Top-right decorative glow */}
      <div className="absolute -top-40 -right-40 w-80 h-80 rounded-full bg-primary-500/10 blur-3xl animate-pulse-soft" />
      <div className="absolute -bottom-40 -left-40 w-80 h-80 rounded-full bg-gold-500/10 blur-3xl animate-pulse-soft" style={{ animationDelay: '1s' }} />

      {/* Theme toggle */}
      <button
        onClick={toggleTheme}
        className="absolute top-4 right-4 z-20 p-2.5 rounded-xl glass-strong text-gray-500 dark:text-gray-300 hover:scale-105 active:scale-95 transition-all duration-200 group"
        title={darkMode ? 'Mode clair' : 'Mode sombre'}
        aria-label={darkMode ? 'Passer en mode clair' : 'Passer en mode sombre'}
      >
        {darkMode ? (
          <Sun className="w-[18px] h-[18px] transition-transform duration-500 group-hover:rotate-90" />
        ) : (
          <Moon className="w-[18px] h-[18px] transition-transform duration-500 group-hover:-rotate-12" />
        )}
      </button>

      {/* Main content */}
      <div className="relative z-10 w-full max-w-md mx-auto px-4 py-8">
        {/* Logo section */}
        <div className="text-center mb-8 animate-fade-in">
          <div className="relative inline-flex mb-5">
            {/* Glow behind logo */}
            <div className="absolute -inset-4 bg-gradient-to-br from-primary-500/20 to-gold-500/20 rounded-3xl blur-xl animate-pulse-soft" />

            <div className="relative w-20 h-20 rounded-2xl bg-gradient-to-br from-primary-500 via-primary-600 to-primary-700
                          flex items-center justify-center shadow-glow-lg animate-float-slow">
              {branding.logoUrl ? (
                <img src={branding.logoUrl} alt="" className="w-10 h-10 object-contain drop-shadow-lg" />
              ) : (
                <Church className="w-10 h-10 text-white drop-shadow-lg" />
              )}
            </div>

            {/* Orbiting dots */}
            <div className="absolute -top-2 -right-2">
              <span className="flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-gold-400 opacity-75" />
                <span className="relative inline-flex rounded-full h-3 w-3 bg-gold-400" />
              </span>
            </div>
          </div>

          <h1 className="text-4xl font-bold text-gray-900 dark:text-white font-display tracking-tight transition-colors">
            {branding.platformName}
          </h1>
          <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 flex items-center justify-center gap-1.5 text-center px-4">
            <Sparkles className="w-3.5 h-3.5 text-primary-500 dark:text-primary-400 flex-shrink-0" />
            {branding.slogan || branding.description || 'Application de Gestion du Discipolat'}
            <Sparkles className="w-3.5 h-3.5 text-primary-500 dark:text-primary-400 flex-shrink-0" />
          </p>
        </div>

        {/* Glass card */}
        <div className="relative">
          {/* Border glow */}
          <div className="absolute -inset-[1px] rounded-2xl bg-gradient-to-br from-primary-500/20 via-transparent to-gold-500/20 opacity-50 animate-border-glow" />

          <div className="relative bg-white/80 dark:bg-gray-900/70 backdrop-blur-2xl rounded-2xl border border-white/40 dark:border-white/10 p-6 sm:p-8 shadow-glass-lg transition-colors duration-500">
            <Outlet />
          </div>
        </div>

        {/* Footer */}
        <p className="text-center mt-8 text-xs text-gray-400 dark:text-gray-500">
          &copy; {new Date().getFullYear()} {branding.platformName}. Tous droits réservés.
        </p>
      </div>
    </div>
  );
}
