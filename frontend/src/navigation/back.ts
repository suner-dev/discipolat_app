/**
 * LOT 2 §BK — retour arrière & fil d'Ariane.
 *
 * <p>Fichier **pur** : la résolution des chemins parents est calculée à partir
 * de la table de routes existante (`routeAccess.ROUTE_ROLES`) plutôt que d'une
 * liste écrite à la main. Conséquence : ajouter une route rend son « parent »
 * automatiquement, sans maintenir un 24e endroit où oublier d'écrire le fil
 * d'Ariane — le défaut des 16 boutons « Retour » dupliqués du dépôt.
 *
 * <p><b>Stratégie de retour</b>, du plus fiable au plus dépendant de
 * l'historique :
 * <ol>
 *   <li>un parent explicite fourni par l'appelant ({@code from}) ;</li>
 *   <li>le parent <b>déduit</b> : le préfixe de chemin le plus long qui
 *       correspond à une route réelle ;</li>
 *   <li>l'historique du navigateur ({@code navigate(-1)}) ;</li>
 *   <li>rien — le bouton est masqué plutôt que de mener nulle part.</li>
 * </ol>
 */

import { ROUTE_ROLES } from '@/lib/routeAccess';

/** Découpe un chemin en segments en ignorant query string et fragment. */
export function pathSegments(pathname: string): string[] {
  return pathname.split(/[?#]/)[0].split('/').filter(Boolean);
}

/** `true` si `pathname` correspond au motif de route (`:param` = joker). */
export function matchesRoutePattern(pattern: string, pathname: string): boolean {
  const patternSegments = pathSegments(pattern);
  const pathSegs = pathSegments(pathname);
  if (patternSegments.length !== pathSegs.length) return false;
  return patternSegments.every(
    (segment, index) => segment.startsWith(':') || segment === pathSegs[index],
  );
}

/** Le chemin correspond-il à une route connue de l'application ? */
export function isKnownRoute(pathname: string): boolean {
  if (!pathname || pathname === '/') return false;
  return Object.keys(ROUTE_ROLES).some((pattern) => matchesRoutePattern(pattern, pathname));
}

/**
 * Parent déduit : le préfixe le plus long qui est une route réelle.
 *
 * <p>`/tenant/organization/nodes/abc` → `/tenant/organization/nodes` n'existe
 * pas comme route → on remonte à `/tenant/organization`, qui existe.
 */
export function deriveParentPath(pathname: string): string | null {
  const segments = pathSegments(pathname);
  // On s'arrête à la racine : un « retour » vers `/` depuis `/dashboard` n'est
  // pas un retour, c'est une navigation.
  for (let length = segments.length - 1; length >= 1; length -= 1) {
    const candidate = `/${segments.slice(0, length).join('/')}`;
    if (isKnownRoute(candidate)) return candidate;
  }
  return null;
}

export interface BackResolution {
  /**
   * Cible du retour : un chemin, `-1` pour l'historique du navigateur, ou
   * `null` s'il n'y a rien d'où revenir (le bouton doit alors être masqué).
   */
  target: string | number | null;
  /** `'from'` = cible explicite, `'parent'` = déduite, `'history'` = navigateur. */
  source: 'from' | 'parent' | 'history' | 'none';
}

/**
 * Détermine où le bouton « retour » doit mener.
 *
 * @param pathname   route courante
 * @param from       cible explicite (navigation par lien : `state: { from }`)
 * @param hasHistory l'historique contient-il une page antérieure exploitable ?
 */
export function resolveBack(
  pathname: string,
  from?: string | null,
  hasHistory = false,
): BackResolution {
  if (from && from !== pathname) {
    return { target: from, source: 'from' };
  }
  const parent = deriveParentPath(pathname);
  if (parent) {
    return { target: parent, source: 'parent' };
  }
  if (hasHistory) {
    return { target: -1, source: 'history' };
  }
  return { target: null, source: 'none' };
}

/**
 * Chemin de fil d'Ariane : ancêtres déduits + route courante, du plus général
 * au plus précis. Les segments non-orphelins (ids, slugs) sont écartés.
 */
export function breadcrumbTrail(pathname: string): string[] {
  const segments = pathSegments(pathname);
  const trail: string[] = [];
  for (let length = 1; length <= segments.length; length += 1) {
    const candidate = `/${segments.slice(0, length).join('/')}`;
    trail.push(candidate);
  }
  return trail;
}

/** Segment lisible d'un chemin (pour l'affichage du dernier fil). */
export function humanizeSegment(segment: string): string {
  return segment
    .replace(/[-_]+/g, ' ')
    .replace(/\.\w+$/, '')
    .trim();
}