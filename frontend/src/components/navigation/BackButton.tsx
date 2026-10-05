import { useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { resolveBack } from '@/navigation/back';
import { useI18n } from '@/i18n';

/**
 * LOT 2 §BK — bouton/flèche de retour, disponible sur **toute** page.
 *
 * <p>Objectif produit : « lorsqu'on est sur une page, on puisse avoir la flèche
 * retour pour revenir à la page précédente ». Plutôt que d'ajouter un 17e bouton
 * « Retour » copié-collé (le dépôt en compte 16, incohérents : les uns font
 * `navigate(-1)`, les autres naviguent vers un parent codé en dur), ce composant
 * est monté une seule fois dans {@code MainLayout} et sert toutes les pages.
 *
 * <p>Dégradation maîtrisée : quand aucune destination n'est déductible, le
 * bouton est **masqué** plutôt que de mener dans le vide — un retour qui ne
 * fait rien est pire que pas de retour du tout.
 */
interface BackButtonProps {
  /** Libellé du bouton. À fournir si l'écran n'a pas besoin de i18n. */
  label?: string;
  className?: string;
  /** Force l'affichage même sans destination connue (utile pour tests/écrans connus). */
  fallbackTo?: string;
}

export function BackButton({ label, className, fallbackTo }: BackButtonProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const { t } = useI18n();

  const state = location.state as { from?: string } | null;
  const { target, source } = resolveBack(location.pathname, state?.from, true);

  const onClick = useCallback(() => {
    if (target === null) {
      navigate(fallbackTo ?? '/');
      return;
    }
    if (typeof target === 'number') {
      navigate(target);
      return;
    }
    navigate(target);
  }, [navigate, target, fallbackTo]);

  if (target === null && !fallbackTo) return null;

  const text = label ?? t('nav.back') ?? 'Retour';

  return (
    <button
      type="button"
      onClick={onClick}
      data-back-source={source}
      data-testid="back-button"
      aria-label={text}
      className={`group inline-flex items-center gap-2 rounded-xl px-2.5 py-1.5 -ml-1 text-sm font-medium
        text-gray-500 dark:text-gray-400 transition-all duration-200 ease-smooth
        hover:text-gray-900 dark:hover:text-gray-100 hover:bg-white/60 dark:hover:bg-gray-800/40
        active:scale-[0.97] ${className ?? ''}`}
    >
      <ArrowLeft
        className="w-4 h-4 transition-transform duration-200 ease-smooth group-hover:-translate-x-0.5"
      />
      <span>{text}</span>
    </button>
  );
}

export default BackButton;