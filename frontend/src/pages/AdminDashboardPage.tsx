import { Link } from 'react-router-dom';
import {
  Palette, Boxes, Menu as MenuIcon, Shield, UserCog, Activity, FileText,
  ArrowRight, Sparkles, MessageSquareText, BookOpen, LayoutTemplate,
} from 'lucide-react';
import { useSettings } from '@/contexts/SettingsContext';

const ADMIN_SECTIONS = [
  { href: '/admin/settings', icon: Palette, title: 'Identité & marque', desc: 'Nom, logo, couleurs, typographie et coordonnées de l\'église.', gradient: 'from-primary-500 to-emerald-600' },
  { href: '/admin/modules', icon: Boxes, title: 'Modules', desc: 'Activer ou désactiver les grands modules de la plateforme.', gradient: 'from-violet-500 to-purple-600' },
  { href: '/admin/menus', icon: MenuIcon, title: 'Menus', desc: 'Configurer la navigation : ordre, libellé, icônes et rôles visibles.', gradient: 'from-amber-500 to-orange-600' },
  { href: '/admin/pages', icon: LayoutTemplate, title: 'Pages', desc: 'Créer des pages personnalisées avec tableaux, graphiques, formulaires et widgets.', gradient: 'from-teal-500 to-cyan-600' },
  { href: '/admin/custom-fields', icon: FileText, title: 'Champs personnalisés', desc: 'Ajouter des champs aux entités (âmes, utilisateurs, départements, familles).', gradient: 'from-sky-500 to-blue-600' },
  { href: '/admin/dictionaries', icon: BookOpen, title: 'Dictionnaires', desc: 'Types d\'événement, statuts, raisons d\'absence et catégories — adaptez chaque liste.', gradient: 'from-fuchsia-500 to-pink-600' },
  { href: '/permissions', icon: Shield, title: 'Rôles & permissions', desc: 'Gérer les rôles, créer des rôles personnalisés et éditer la matrice.', gradient: 'from-indigo-500 to-violet-600' },
  { href: '/users', icon: UserCog, title: 'Utilisateurs', desc: 'Créer, modifier et gérer les comptes utilisateurs.', gradient: 'from-teal-500 to-cyan-600' },
  { href: '/admin/feedback', icon: MessageSquareText, title: 'Retours testeurs', desc: 'Bugs, suggestions et retours UX des testeurs — suivi et statuts.', gradient: 'from-blue-500 to-indigo-600' },
  { href: '/audit', icon: Activity, title: 'Audit', desc: 'Consulter le journal de bord complet des actions système.', gradient: 'from-gray-500 to-slate-600' },
];

export default function AdminDashboardPage() {
  const { branding } = useSettings();

  return (
    <div className="page-container max-w-6xl">
      <div className="mb-8">
        <div className="flex items-center gap-3 mb-1">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-red-500 to-rose-600 text-white flex items-center justify-center shadow-lg">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-gray-100 font-display tracking-tight">
              Administration
            </h1>
            <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
              Centre de configuration de {branding.platformName} — tout paramétrer sans écrire de code.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        {ADMIN_SECTIONS.map((s) => (
          <Link key={s.href} to={s.href} className="group glass-card p-6 hover:-translate-y-1.5 transition-all duration-300">
            <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${s.gradient} text-white flex items-center justify-center shadow-lg mb-4 transition-transform duration-300 group-hover:scale-110 group-hover:-rotate-6`}>
              <s.icon className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-gray-900 dark:text-gray-100 mb-1">{s.title}</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400 leading-relaxed mb-4">{s.desc}</p>
            <div className="flex items-center gap-1 text-xs font-medium text-primary-600 dark:text-primary-400 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-x-[-6px] group-hover:translate-x-0">
              Accéder <ArrowRight className="w-3.5 h-3.5" />
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
