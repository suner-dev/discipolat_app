import { useEffect, useState } from 'react';

/**
 * LOT 2 §AN — jetons de mouvement.
 *
 * <p>Le dépôt comptait 25 animations Tailwind et 10+ styles de barres d'onglets
 * différents, tous écrits « à la main » dans les pages : d'où des transitions
 * irrégulières, et aucune garantie que le réglage système « réduire les
 * animations » soit respecté de façon cohérente.
 *
 * <p>Ce module centralise : durées, courbes, variants, et une détection unique
 * du mouvement réduit. Toute animation introduced par la suite s'y branche.
 */

/** Durées en millisecondes. Trois régimes, pas plus : la lisibilité paie. */
export const DURATION = {
  /** Changement d'état discret : survol, focus, icône. */
  fast: 150,
  /** Transition d'interface : repli d'un groupe, changement d'onglet. */
  base: 250,
  /** Entrée/sortie d'un bloc : page, panneau, modale. */
  slow: 380,
} as const;

/**
 * Courbes. `smooth` est celle déjà utilisée par toute l'application
 * (`transitionTimingFunction` de Tailwind) — on ne réinvente pas une nouvelle
 * courbe pour un seul composant.
 */
export const EASING = {
  /** Entrée et sortie standards. */
  smooth: 'cubic-bezier(0.4, 0, 0.2, 1)',
  /** Sortie rapide : l'élément quitte vite, l'entrée est plus douce. */
  exit: 'cubic-bezier(0.4, 0, 1, 1)',
  /** Entrée franche, pour les éléments qui apparaissent. */
  enter: 'cubic-bezier(0, 0, 0.2, 1)',
  /** Léger rebond, réservé aux confirmations et aux badges. */
  spring: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
} as const;

/** Styles réutilisables, à poser en `style` plutôt qu'en classe arbitraire. */
export const MOTION_STYLE = {
  fast: { transitionDuration: `${DURATION.fast}ms`, transitionTimingFunction: EASING.smooth },
  base: { transitionDuration: `${DURATION.base}ms`, transitionTimingFunction: EASING.smooth },
  slow: { transitionDuration: `${DURATION.slow}ms`, transitionTimingFunction: EASING.smooth },
} as const;

/**
 * Le système demande-il de réduire les animations ?
 *
 * <p>Accessibilité : pour une part-des gens, une animation de 400 ms est
 * nauséeuse ou déclenche un vertige. Le CSS du projet coupe déjà tout via
 * `prefers-reduced-motion` (index.css) ; ce hook sert aux composants qui
 * doivent, eux, <b>ne pas déclencher</b> l'animation (pas seulement la
 * raccourcir) : scroll automatique, animation de compteur.
 */
export function usePrefersReducedMotion(): boolean {
  const [prefersReduced, setPrefersReduced] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return undefined;
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    setPrefersReduced(query.matches);
    const onChange = (event: MediaQueryListEvent) => setPrefersReduced(event.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  return prefersReduced;
}

/**
 * Retard de cascading (« enchaînement ») pour une liste d'éléments.
 *
 * <p> plafonné à 8 éléments : au-delà, l'attente devient plus longue que le
 * bénéfice, et l'utilisateur perçoit un retard plutôt qu'une animation.
 */
export function staggerDelay(index: number, stepMs = 40, cap = 8): number {
  return Math.min(index, cap) * stepMs;
}