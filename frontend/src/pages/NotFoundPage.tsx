import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';
import {
  Home, Search, ArrowLeft, Heart, Users, Building2,
  LayoutDashboard, BookOpen, Calendar, FileText, Church,
  Activity, Sparkles, Compass, Map,
} from 'lucide-react';

const quickLinks = [
  { name: 'Tableau de bord', href: '/dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'] },
  { name: 'Âmes', href: '/souls', icon: Heart, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'] },
  { name: 'Familles', href: '/families', icon: Users, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'] },
  { name: 'Événements', href: '/events', icon: Calendar, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'] },
  { name: 'Rapports', href: '/reports', icon: FileText, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'] },
  { name: 'Documents', href: '/documents', icon: BookOpen, roles: ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'] },
];

export default function NotFoundPage() {
  const { user, hasRole } = useAuth();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const filteredLinks = quickLinks.filter(
    (link) => user && link.roles.some((r) => hasRole(r))
  );

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-mesh flex items-center justify-center p-4">
      <div className="w-full max-w-2xl animate-scale-in">
        {/* Main card */}
        <div className="glass-strong rounded-3xl p-8 sm:p-12 text-center relative overflow-hidden">
          {/* Decorative elements */}
          <div className="absolute -top-20 -right-20 w-40 h-40 bg-primary-500/10 rounded-full blur-3xl" />
          <div className="absolute -bottom-20 -left-20 w-40 h-40 bg-gold-500/10 rounded-full blur-3xl" />

          {/* Icon with glow */}
          <div className="relative inline-flex mb-6">
            <div className="absolute inset-0 bg-primary-500/20 rounded-full blur-2xl animate-pulse-soft" />
            <div className="relative w-24 h-24 rounded-3xl bg-gradient-to-br from-primary-500 via-primary-600 to-primary-700 flex items-center justify-center shadow-glow">
              <Compass className="w-12 h-12 text-white" />
            </div>
          </div>

          {/* Error code */}
          <h1 className="text-8xl sm:text-9xl font-bold text-gradient font-display mb-2">
            404
          </h1>

          {/* Subtitle */}
          <div className="space-y-2 mb-8">
            <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
              Page introuvable
            </h2>
            <p className="text-gray-500 dark:text-gray-400 max-w-md mx-auto">
              La page que vous cherchez n'existe pas ou a été déplacée.
              Vérifiez l'URL ou utilisez la recherche ci-dessous.
            </p>
          </div>

          {/* Search bar */}
          <form onSubmit={handleSearch} className="max-w-md mx-auto mb-8">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Rechercher dans l'application..."
                className="input pl-12 pr-4 py-3 text-base"
              />
            </div>
          </form>

          {/* Quick links */}
          <div className="mb-8">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4 flex items-center justify-center gap-2">
              <Map className="w-3 h-3" />
              Pages disponibles
            </p>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              {filteredLinks.map((link) => {
                const Icon = link.icon;
                return (
                  <Link
                    key={link.href}
                    to={link.href}
                    className="flex items-center gap-2.5 p-3 rounded-xl bg-white/40 dark:bg-gray-800/40 
                             hover:bg-white/60 dark:hover:bg-gray-800/60 border border-white/20 dark:border-white/[0.06]
                             transition-all duration-200 hover:scale-[1.02] active:scale-[0.98] group"
                  >
                    <div className="p-1.5 rounded-lg bg-primary-50 dark:bg-primary-900/20 group-hover:bg-primary-100 dark:group-hover:bg-primary-900/30 transition-colors">
                      <Icon className="w-4 h-4 text-primary-600 dark:text-primary-400" />
                    </div>
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300 truncate">
                      {link.name}
                    </span>
                  </Link>
                );
              })}
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <button
              onClick={() => navigate(-1)}
              className="btn-secondary btn-sm"
            >
              <ArrowLeft className="w-4 h-4" />
              Page précédente
            </button>
            <Link
              to="/dashboard"
              className="btn-primary btn-sm"
            >
              <Home className="w-4 h-4" />
              Retour à l'accueil
            </Link>
          </div>

          {/* Footer */}
          <div className="mt-8 pt-6 border-t border-white/20 dark:border-white/[0.06]">
            <div className="flex items-center justify-center gap-2 text-xs text-gray-400">
              <Church className="w-3 h-3" />
              <span>Discipolat · Application de Gestion du Discipolat</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
