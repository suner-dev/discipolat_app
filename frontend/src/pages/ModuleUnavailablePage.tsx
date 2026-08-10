import { Link } from 'react-router-dom';
import { PackageX, ArrowLeft, LayoutDashboard } from 'lucide-react';
import { useSettings } from '@/contexts/SettingsContext';

/**
 * Page affichée proprement lorsqu'un utilisateur tente d'accéder à un module
 * désactivé par l'administration : pas de bouton mort, pas de page vide,
 * un message clair et une navigation de retour fonctionnelle.
 */
export default function ModuleUnavailablePage() {
  const { branding } = useSettings();

  return (
    <div className="min-h-[60vh] flex items-center justify-center">
      <div className="glass-card max-w-md w-full p-10 text-center">
        <div className="relative inline-flex mb-6">
          <div className="absolute -inset-3 bg-gradient-to-br from-primary-500/15 to-gold-500/15 rounded-3xl blur-xl" />
          <div className="relative w-20 h-20 rounded-3xl bg-gradient-to-br from-gray-200 to-gray-300 dark:from-gray-700 dark:to-gray-800 flex items-center justify-center">
            <PackageX className="w-10 h-10 text-gray-500 dark:text-gray-300" />
          </div>
        </div>

        <h1 className="text-xl font-bold text-gray-900 dark:text-gray-100 font-display">
          Module désactivé
        </h1>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400 leading-relaxed">
          Cette fonctionnalité n'est pas active pour votre église.
          L'administrateur peut l'activer depuis la configuration de {branding.platformName}.
        </p>

        <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
          <Link to="/dashboard" className="btn-primary btn-sm">
            <LayoutDashboard className="w-4 h-4" /> Retour au tableau de bord
          </Link>
          <button onClick={() => window.history.back()} className="btn-ghost btn-sm">
            <ArrowLeft className="w-4 h-4" /> Page précédente
          </button>
        </div>
      </div>
    </div>
  );
}
