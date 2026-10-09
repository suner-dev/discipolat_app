import { Fragment } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ChevronRight, House } from 'lucide-react';
import { publicBreadcrumbTrail } from '@/navigation/back';
import { useI18n } from '@/i18n';

/**
 * LOT 3 §BK (T3.2) — fil d'Ariane de la **zone vitrine publique**
 * (`/eglises`, `/churches`, `/e/:slug`).
 *
 * <p>Dédié et additif : il ne réutilise PAS `Breadcrumbs` (le composant du
 * `MainLayout` authentifié), qui dépend de la navigation groupée par rôle et
 * renvoie `null` pour les routes plates non listées dans `ROUTE_ROLES`. Ici, la
 * structure est calculée par `publicBreadcrumbTrail` (pur, testable) et la
 * logique de retour est totalement découplée de l'espace connecté.</p>
 *
 * <p>Accessibilité : `<nav aria-label>` + `aria-current="page"` sur le dernier
 * maillon (qui n'est pas un lien, puisqu'on y est déjà).</p>
 */
interface PublicBreadcrumbsProps {
  /** Libellé du maillon courant (ex. le nom de l'église sur une fiche). */
  currentLabel?: string;
  className?: string;
}

export function PublicBreadcrumbs({ currentLabel, className }: PublicBreadcrumbsProps) {
  const location = useLocation();
  const { t } = useI18n();

  const trail = publicBreadcrumbTrail(location.pathname, currentLabel);
  // Un seul maillon (Accueil) ne dit rien : on ne montre le fil qu'en contexte.
  if (trail.length <= 1) return null;

  return (
    <nav
      aria-label={t('nav.breadcrumb') || "Fil d'Ariane"}
      data-testid="public-breadcrumbs"
      className={`flex items-center gap-1 flex-wrap text-xs text-gray-500 dark:text-gray-400 ${className ?? ''}`}
    >
      {trail.map((crumb, index) => {
        const isLast = index === trail.length - 1;
        const label = crumb.label ?? t(crumb.labelKey);
        return (
          <Fragment key={`${crumb.href}-${index}`}>
            {index > 0 && <ChevronRight className="w-3 h-3 opacity-60" aria-hidden="true" />}
            {isLast ? (
              <span aria-current="page" className="px-1 py-0.5 font-medium text-gray-700 dark:text-gray-200 truncate max-w-[18rem]">
                {label}
              </span>
            ) : (
              <Link
                to={crumb.href}
                className="inline-flex items-center gap-1 px-1 py-0.5 rounded-md hover:text-gray-900 dark:hover:text-gray-100"
              >
                {index === 0 && <House className="w-3.5 h-3.5" aria-hidden="true" />}
                {label}
              </Link>
            )}
          </Fragment>
        );
      })}
    </nav>
  );
}

export default PublicBreadcrumbs;
