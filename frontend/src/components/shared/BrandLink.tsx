import type { MouseEvent, ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';

export interface BrandLinkProps {
  children: ReactNode;
  /** Destination : la page d'accueil (landing) de la plateforme. */
  to?: string;
  /**
   * Classes du conteneur. On reprend **telles quelles** celles du bloc marque
   * remplacé : le rendu doit être identique avant/après, seule la sémantique
   * change (`<div>` → `<a>`).
   */
  className?: string;
  /** Nom accessible — le bloc marque n'a pas de libellé de lien explicite. */
  ariaLabel: string;
  title?: string;
  /**
   * Appelé quand on est **déjà** sur `to` (ex. : sur le landing, remonter au
   * héros). Sans callback, le clic devient une no-op plutôt qu'un empilage
   * d'entrées d'historique mortes.
   */
  onSameRoute?: () => void;
}

/**
 * LOT 2 §BR — la marque devient un lien vers le landing.
 *
 * <p>Trois hôtes rendaient la marque dans une `<div>` sans lien (`Sidebar`,
 * `AuthLayout`) ou dans un `<button>` qui remontait au héros même ailleurs
 * (`LandingNavbar`) : le logo n'était cliquable **nulle part**. Un seul atome
 * partagé, monté par chaque hôte, plutôt que trois implémentations qui
 * divergent — le même défaut que celui des 16 boutons « Retour » dupliqués.
 *
 * <p>Un vrai `<a>` : atteignable au clavier, ouverturable dans un nouvel onglet,
 * et compris par les lecteurs d'écran comme un lien vers l'accueil.
 */
export function BrandLink({
  children,
  to = '/',
  className,
  ariaLabel,
  title,
  onSameRoute,
}: BrandLinkProps) {
  const location = useLocation();

  const onClick = (event: MouseEvent<HTMLAnchorElement>) => {
    // Ailleurs que sur `to` : navigation normale du `<Link>`.
    if (location.pathname !== to) return;
    // Sur `to`, suivre le lien ne ferait qu'ajouter une entrée d'historique
    // identique — le « retour » du bouton précédent retomberait sur la même
    // page. On intercepte et on laisse l'hôte décider (remonter au héros).
    event.preventDefault();
    onSameRoute?.();
  };

  return (
    <Link to={to} className={className} aria-label={ariaLabel} title={title ?? ariaLabel} onClick={onClick}>
      {children}
    </Link>
  );
}

export default BrandLink;
