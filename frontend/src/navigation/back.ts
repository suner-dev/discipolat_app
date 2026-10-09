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
 * LOT 3 §BK — l’historique du navigateur offre-t-il une entrée exploitable
 * par l’application ?
 *
 * <p>react-router v6 range la position courante dans `window.history.state.idx`
 * (0 = première entrée de la session, donc **atterrissage direct** : `navigate(-1)`
 * ne fait rien, voire éjecte l’utilisateur du site). Un layout connu
 * (`MainLayout`) peut supposer que l’on vient de quelque part ; les zones sans
 * layout (`AuthLayout`, vitrine) reçoivent des liens directs — e-mails, QR codes,
 * deep links mobile — et doivent s’appuyer sur l’historique **réel**.
 *
 * <p>Fonction **pure** : l’état est injectable, donc testable sans DOM.
 */
export function hasUsableHistory(historyState: unknown = readHistoryState()): boolean {
  const idx = (historyState as { idx?: unknown } | null | undefined)?.idx;
  return typeof idx === 'number' && Number.isFinite(idx) && idx > 0;
}

/** Lit `window.history.state` en tolérant l’absence de `window` (SSR, tests). */
export function readHistoryState(): unknown {
  if (typeof window === 'undefined') return null;
  try {
    return window.history?.state ?? null;
  } catch {
    return null;
  }
}

/**
 * Chemin de fil d’Ariane : ancêtres déduits + route courante, du plus général
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

// ── LOT 3 §BK (T3.2) — zone vitrine PUBLIQUE, hors `ROUTE_ROLES` ────────────
// Ces routes sont volontairement absentes de la table d'accès (elles sont
// publiques et non gardées) : le déducteur standard `deriveParentPath` les
// ignore. Voici le pendant public — **pur**, testable, et qui ne touche
// AUCUNE fonction existante (A1/A6 : rien de partagé n'est modifié).

/** Hub de la vitrine : l'annuaire public. */
export const PUBLIC_DIRECTORY_PATH = '/eglises';

/** Route de vitrine publique (annuaire `/eglises`|`/churches` ou fiche `/e/:slug`) ? */
export function isPublicShowcaseRoute(pathname: string): boolean {
  const segs = pathSegments(pathname);
  if (segs.length === 2 && segs[0] === 'e') return true;
  return segs.length === 1 && (segs[0] === 'eglises' || segs[0] === 'churches');
}

/** Parent public d'une route de vitrine : fiche → annuaire ; annuaire → `/`. */
export function derivePublicParentPath(pathname: string): string | null {
  const segs = pathSegments(pathname);
  if (segs.length === 2 && segs[0] === 'e') return PUBLIC_DIRECTORY_PATH;
  if (segs.length === 1 && (segs[0] === 'eglises' || segs[0] === 'churches')) return '/';
  return null;
}

export interface PublicCrumb {
  href: string;
  /** Clé i18n à résoudre. */
  labelKey: string;
  /** Libellé explicite (p. ex. le nom de l'église) ; gagne sur `labelKey`. */
  label?: string;
}

/**
 * Fil d'Ariane public : Accueil → Annuaire → [page courante]. Vide hors vitrine.
 * `currentLabel` remplace le libellé de la fiche courante (le nom de l'église).
 */
export function publicBreadcrumbTrail(pathname: string, currentLabel?: string): PublicCrumb[] {
  const segs = pathSegments(pathname);
  const inDirectory = segs.length === 1 && (segs[0] === 'eglises' || segs[0] === 'churches');
  const inChurch = segs.length === 2 && segs[0] === 'e';
  if (!inDirectory && !inChurch) return [];
  const trail: PublicCrumb[] = [{ href: '/', labelKey: 'publicNav.home' }];
  if (inDirectory) {
    trail.push({ href: PUBLIC_DIRECTORY_PATH, labelKey: 'publicNav.directory' });
  } else if (inChurch) {
    trail.push({ href: PUBLIC_DIRECTORY_PATH, labelKey: 'publicNav.directory' });
    trail.push({ href: pathname, labelKey: 'publicNav.church', label: currentLabel });
  }
  return trail;
}