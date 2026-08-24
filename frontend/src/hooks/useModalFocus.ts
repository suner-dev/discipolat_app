import { useEffect, useRef } from 'react';

/**
 * P3 #98 — Focus management pour les modales.
 * - Sauvegarde l'élément focalisé avant l'ouverture et lui restitue le focus à la fermeture.
 * - Piège la navigation Tab à l'intérieur du conteneur tant qu'il est ouvert.
 *
 * Usage :
 *   const containerRef = useModalFocus<HTMLDivElement>(isOpen);
 *   return <div ref={containerRef} role="dialog" aria-modal="true">…</div>
 */
export function useModalFocus<T extends HTMLElement>(isActive: boolean) {
  const containerRef = useRef<T | null>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!isActive) return;
    previouslyFocused.current = document.activeElement as HTMLElement | null;

    // Focus initial dans la modale
    const focusables = () =>
      Array.from(
        containerRef.current?.querySelectorAll<HTMLElement>(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        ) ?? []
      ).filter((el) => !el.hasAttribute('disabled'));

    focusables()[0]?.focus();

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;
      const list = focusables();
      if (list.length === 0) return;
      const first = list[0];
      const last = list[list.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };

    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      previouslyFocused.current?.focus();
    };
  }, [isActive]);

  return containerRef;
}
