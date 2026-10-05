import { Fragment } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ChevronRight, House } from 'lucide-react';
import { useGroupedNavigation } from '@/navigation/NavigationContext';
import { deriveParentPath } from '@/navigation/back';
import { useI18n } from '@/i18n';

/**
 * LOT 2 §BK — fil d'Ariane.
 *
 * <p>Monté dans {@code MainLayout}, il affiche le chemin réel de l'utilisateur :
 * les <b>groupes</b> de navigation traversés (« Vie de l'église › Régions »),
 * puis la page courante. Le nom du groupe vient de l'affichage réel — donc
 * d'un nom renommé par l'église — et non d'une chaîne figée dans le code.
 *
 * <p>Il est volontairement discret : masqué sur une page « racine », il ne sert
 * qu'à donner le contexte quand l'utilisateur s'est enfoui dans un détail.
 */
interface BreadcrumbsProps {
  /** Racine affichée en premier. */
  homeHref?: string;
  className?: string;
}

export function Breadcrumbs({ homeHref = '/', className }: BreadcrumbsProps) {
  const location = useLocation();
  const { trailFor } = useGroupedNavigation();
  const { t } = useI18n();

  // Un seul niveau de détail : le groupe parent et le parent de chemin suffisent.
  const parent = deriveParentPath(location.pathname);
  if (!parent) return null;

  const trail = trailFor(location.pathname);
  // Le dernier nœud porte la page courante : inutile dans le fil.
  const ancestors = trail.length > 1 ? trail.slice(0, -1) : [];
  const pageLabel = trail.length > 0 ? trail[trail.length - 1].label : null;

  return (
    <nav
      aria-label={t('nav.breadcrumb') || 'Fil d’Ariane'}
      className={`flex items-center gap-1 flex-wrap text-xs text-gray-400 dark:text-gray-500 animate-fade-in ${className ?? ''}`}
    >
      <Link
        to={homeHref}
        className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md hover:text-gray-700 dark:hover:text-gray-200 hover:bg-white/60 dark:hover:bg-gray-800/40 transition-all duration-200"
      >
        <House className="w-3.5 h-3.5" />
      </Link>

      {ancestors.map((node, index) => (
        <Fragment key={node.id}>
          <ChevronRight className="w-3 h-3 opacity-60" />
          <span className="px-1.5 py-0.5 truncate max-w-[16rem]">{node.label}</span>
          {index === ancestors.length - 1 && parent && (
            <>
              <ChevronRight className="w-3 h-3 opacity-60" />
              <Link
                to={parent}
                className="px-1.5 py-0.5 rounded-md hover:text-gray-700 dark:hover:text-gray-200 hover:bg-white/60 dark:hover:bg-gray-800/40 transition-all duration-200"
              >
                {t('nav.backToList') || 'Retour à la liste'}
              </Link>
            </>
          )}
        </Fragment>
      ))}

      {pageLabel && (
        <>
          <ChevronRight className="w-3 h-3 opacity-60" />
          <span className="px-1.5 py-0.5 font-medium text-gray-600 dark:text-gray-300 truncate max-w-[18rem]">
            {pageLabel}
          </span>
        </>
      )}
    </nav>
  );
}

export default Breadcrumbs;