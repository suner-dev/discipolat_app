import { Outlet } from 'react-router-dom';
import { Church, Sparkles } from 'lucide-react';

export default function AuthLayout() {
  return (
    <div className="relative min-h-screen flex items-center justify-center overflow-hidden bg-gradient-to-br from-gray-950 via-gray-900 to-gray-950">
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

      {/* Main content */}
      <div className="relative z-10 w-full max-w-md mx-auto px-4 py-8">
        {/* Logo section */}
        <div className="text-center mb-8 animate-fade-in">
          <div className="relative inline-flex mb-5">
            {/* Glow behind logo */}
            <div className="absolute -inset-4 bg-gradient-to-br from-primary-500/20 to-gold-500/20 rounded-3xl blur-xl animate-pulse-soft" />

            <div className="relative w-20 h-20 rounded-2xl bg-gradient-to-br from-primary-500 via-primary-600 to-primary-700 
                          flex items-center justify-center shadow-glow-lg animate-float-slow">
              <Church className="w-10 h-10 text-white drop-shadow-lg" />
            </div>

            {/* Orbiting dots */}
            <div className="absolute -top-2 -right-2">
              <span className="flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-gold-400 opacity-75" />
                <span className="relative inline-flex rounded-full h-3 w-3 bg-gold-400" />
              </span>
            </div>
          </div>

          <h1 className="text-4xl font-bold text-white font-display tracking-tight">
            Discipolat
          </h1>
          <p className="mt-2 text-sm text-gray-400 flex items-center justify-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5 text-primary-400" />
            Application de Gestion du Discipolat
            <Sparkles className="w-3.5 h-3.5 text-primary-400" />
          </p>
        </div>

        {/* Glass card */}
        <div className="relative">
          {/* Border glow */}
          <div className="absolute -inset-[1px] rounded-2xl bg-gradient-to-br from-primary-500/20 via-transparent to-gold-500/20 opacity-50 animate-border-glow" />

          <div className="relative bg-gray-900/70 backdrop-blur-2xl rounded-2xl border border-white/10 p-6 sm:p-8 shadow-glass-lg">
            <Outlet />
          </div>
        </div>

        {/* Footer */}
        <p className="text-center mt-8 text-xs text-gray-500">
          &copy; {new Date().getFullYear()} Discipolat. Tous droits réservés.
        </p>
      </div>
    </div>
  );
}
