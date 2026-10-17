import { useEffect, useRef, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

/**
 * Modal — fenêtre modale rendue via un React portal sur <body>.
 *
 * Pourquoi un portal ? Les pages sont enveloppées par `MainLayout` dans un
 * conteneur `.page-enter-active` qui applique une animation de `transform`.
 * Un ancêtre avec `transform`/`will-change: transform` devient le *containing
 * block* de tout descendant `position: fixed` : les anciennes modales
 * `.modal-overlay` (fixed inset-0) s'ancraient alors sur la hauteur totale de
 * la page au lieu du viewport — la fenêtre surgissait « au milieu » et il
 * fallait scroller pour l'atteindre (bug constaté en démo). Rendre la modale
 * directement sous <body>, hors de cet ancêtre, élimine la classe de bug, quel
 * que soit le conteneur parent.
 *
 * Le composant réutilise les classes CSS globales existantes (`modal-overlay`,
 * `modal-content`, `modal-header`, `modal-body`, `modal-footer`) pour une
 * parité visuelle stricte avec le reste de l'application : migrer une page =
 * remplacer le bloc `<div className="modal-overlay">…</div>` par `<Modal>…</Modal>`
 * en gardant l'entête/corps/pied internes.
 *
 * Accessibilité & comportement : rôle `dialog` + `aria-modal`, fermeture par
 * Escape et par clic sur l'overlay (désactivables), verrou du scroll du body,
 * focus posé sur la modale à l'ouverture et renvoyé à l'élément déclencheur à
 * la fermeture.
 */
export interface ModalProps {
  open: boolean;
  onClose: () => void;
  children: ReactNode;
  /** Taille max de la boîte (correspond aux utilitaires Tailwind `max-w-*`). */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** Libellé accessible de la boîte (titre implicite, lu par les lecteurs d'écran). */
  ariaLabel?: string;
  /** Identifiant de l'élément titre pour `aria-labelledby` (prioritaire sur ariaLabel). */
  ariaLabelledBy?: string;
  /** Ferme au clic sur l'overlay (défaut : true). */
  closeOnOverlay?: boolean;
  /** Ferme sur la touche Escape (défaut : true). */
  closeOnEsc?: boolean;
  /** Classes supplémentaires appliquées à `.modal-content` (surcharge de largeur, etc.). */
  contentClassName?: string;
}

const SIZE_CLASS: Record<NonNullable<ModalProps['size']>, string> = {
  sm: 'max-w-sm',
  md: 'max-w-md',
  lg: 'max-w-lg',
  xl: 'max-w-2xl',
};

export function Modal({
  open,
  onClose,
  children,
  size = 'md',
  ariaLabel,
  ariaLabelledBy,
  closeOnOverlay = true,
  closeOnEsc = true,
  contentClassName = '',
}: ModalProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  // Verrou du scroll du body + restauration, et gestion du focus, uniquement
  // tant que la modale est montée (open === true).
  useEffect(() => {
    if (!open) return;
    previouslyFocused.current = document.activeElement as HTMLElement | null;

    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    // Pose le focus initial dans la boîte (a11y : le lecteur d'écran « entre »
    // dans la modale au lieu de rester derrière).
    const node = panelRef.current;
    const focusable = node?.querySelector<HTMLElement>(
      'input:not([type="hidden"]), select, textarea, button:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
    );
    (focusable ?? node)?.focus({ preventScroll: true });

    return () => {
      document.body.style.overflow = prevOverflow;
      // Rend le focus au déclencheur pour ne pas « perdre » la position clavier.
      previouslyFocused.current?.focus?.({ preventScroll: true });
    };
  }, [open]);

  // Fermeture par Escape.
  useEffect(() => {
    if (!open || !closeOnEsc) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        onClose();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, closeOnEsc, onClose]);

  if (!open) return null;
  if (typeof document === 'undefined') return null; // SSR-safe

  return createPortal(
    <div
      className="modal-overlay"
      onClick={closeOnOverlay ? onClose : undefined}
      role="presentation"
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabelledBy ? undefined : ariaLabel}
        aria-labelledby={ariaLabelledBy}
        tabIndex={-1}
        className={`modal-content ${SIZE_CLASS[size]} ${contentClassName}`.trim()}
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>,
    document.body,
  );
}

export default Modal;
