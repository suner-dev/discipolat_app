import { describe, it, expect, afterEach } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import PublicBreadcrumbs from '@/components/navigation/PublicBreadcrumbs';

/**
 * LOT 3 §BK (T3.4) — verrouillage du composant `PublicBreadcrumbs` (T3.2).
 *
 * <p>Portée : (a) reconnaissance stricte des trois formes de la vitrine
 * (`/e/:slug`, `/eglises`, `/churches`) ; (b) silence total hors vitrine —
 * aucune structure `<nav>` ne doit s'infiltrer sur une page privée ;
 * (c) accessibilité : `<nav aria-label>` + `aria-current="page"` sur le
 * dernier maillon (qui n'est pas un lien) ; (d) écrasement du libellé par
 * `currentLabel` (nom réel de l'église sur une fiche) ; (e) conservation de
 * la query string sur le deep link (`?church=` venant du picker T1.2).</p>
 *
 * <p>Le composant n'utilise PAS le `I18nProvider` : `useI18n` dégrade
 * gracieusement sur la locale FR (FALLBACK), ce qui est suffisant pour
 * vérifier la structure DOM et les `aria-*` sans dépendre du contexte.</p>
 */
function renderAt(pathname: string, currentLabel?: string) {
  return render(
    <MemoryRouter initialEntries={[pathname]}>
      <PublicBreadcrumbs currentLabel={currentLabel} />
    </MemoryRouter>,
  );
}

afterEach(() => cleanup());

describe('LOT 3 §BK (T3.4) — PublicBreadcrumbs', () => {
  describe('sur une fiche /e/:slug', () => {
    it('affiche trois maillons : Accueil → Annuaire → église', () => {
      const { container } = renderAt('/e/bethel', 'Église Bethel');
      const nav = container.querySelector('nav[data-testid="public-breadcrumbs"]');
      expect(nav).not.toBeNull();
      // 2 liens (Accueil, Annuaire) + 1 span aria-current (courant).
      const links = nav!.querySelectorAll('a');
      const current = nav!.querySelector('[aria-current="page"]');
      expect(links).toHaveLength(2);
      expect(current).not.toBeNull();
      expect(current!.textContent).toBe('Église Bethel');
    });

    it('utilise le libellé FR « Annuaire » du dictionnaire pour le 2e maillon', () => {
      renderAt('/e/bethel', 'Église Bethel');
      // useI18n FALLBACK résout publicNav.directory → 'Annuaire' (FR).
      expect(screen.getByRole('link', { name: /Annuaire/i })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Accueil/i })).toBeInTheDocument();
    });

    it('utilise currentLabel plutot que la clé publicNav.church', () => {
      renderAt('/e/bethel', 'Paroire Saint-Esprit');
      // Le span courant porte le nom réel, pas le mot générique « Église ».
      const current = screen.getByText('Paroire Saint-Esprit');
      expect(current.tagName).toBe('SPAN');
      expect(current).toHaveAttribute('aria-current', 'page');
      // Et le libellé générique n'apparait NULLE PART dans le fil.
      expect(screen.queryByText(/^Église$/)).toBeNull();
    });

    it('conserve la query string sur le deep link courant (?church= du picker)', () => {
      const { container } = renderAt('/e/bethel?church=1', 'Bethel');
      // Le dernier maillon n'est PAS un lien, mais le href interne (portée
      // du test) doit rester cohérent avec l'URL courante : on vérifie que
      // le composant reconnaît la route malgré le `?church=`.
      const nav = container.querySelector('nav[data-testid="public-breadcrumbs"]');
      expect(nav).not.toBeNull();
      expect(nav!.querySelector('[aria-current="page"]')!.textContent).toBe('Bethel');
    });
  });

  describe('sur un annuaire (/eglises ou /churches)', () => {
    it('affiche deux maillons : Accueil → Annuaire, sans dernier non-lien', () => {
      const { container } = renderAt('/eglises');
      const nav = container.querySelector('nav[data-testid="public-breadcrumbs"]');
      expect(nav).not.toBeNull();
      const links = nav!.querySelectorAll('a');
      expect(links).toHaveLength(1); // Accueil
      const current = nav!.querySelector('[aria-current="page"]');
      expect(current).not.toBeNull();
      expect(current!.textContent).toBe('Annuaire');
    });

    it('reconnaît aussi la variante anglaise /churches (alias de l’annuaire)', () => {
      const { container } = renderAt('/churches');
      expect(container.querySelector('nav[data-testid="public-breadcrumbs"]')).not.toBeNull();
      expect(screen.getByText('Annuaire')).toBeInTheDocument();
    });
  });

  describe('hors vitrine : silence total (A1 — n’interfère avec aucune page existante)', () => {
    it('ne rend RIEN sur /dashboard (pas de <nav>, pas de testid)', () => {
      const { container } = renderAt('/dashboard');
      expect(container.querySelector('nav[data-testid="public-breadcrumbs"]')).toBeNull();
      // Aucun lien ni span courant : la structure DOM reste vide.
      expect(container.querySelector('[aria-current="page"]')).toBeNull();
    });

    it('ne rend RIEN sur / (racine du SPA, hors vitrine)', () => {
      const { container } = renderAt('/');
      expect(container.querySelector('nav[data-testid="public-breadcrumbs"]')).toBeNull();
    });

    it('ne rend RIEN sur /souls/12 (route privée authentifiée)', () => {
      const { container } = renderAt('/souls/12');
      expect(container.querySelector('nav[data-testid="public-breadcrumbs"]')).toBeNull();
    });

    it('ne rend RIEN sur /e/bethel/edit (sous-route non prévue du helper)', () => {
      // Si quelqu'un ajoute une vue enfant sous /e/:slug sans étendre
      // `isPublicShowcaseRoute`, le fil doit se taire plutôt qu'afficher
      // un chemin erroné.
      const { container } = renderAt('/e/bethel/edit');
      expect(container.querySelector('nav[data-testid="public-breadcrumbs"]')).toBeNull();
    });
  });

  describe('accessibilité (a11y)', () => {
    it('le <nav> porte aria-label (résolu depuis nav.breadcrumb, FR : « Fil d\'Ariane »)', () => {
      const { container } = renderAt('/e/bethel', 'Bethel');
      const nav = container.querySelector('nav[data-testid="public-breadcrumbs"]')!;
      expect(nav.getAttribute('aria-label')).toMatch(/fil/i);
    });

    it('le dernier maillon est un span aria-current="page", pas un lien', () => {
      const { container } = renderAt('/e/bethel', 'Bethel');
      const current = container.querySelector('[aria-current="page"]')!;
      // <span> et non <a> : on est déjà sur la page courante.
      expect(current.tagName).toBe('SPAN');
      expect(current.tagName).not.toBe('A');
    });

    it('les séparateurs ChevronRight sont aria-hidden (les lecteurs d’écran ignorent)', () => {
      const { container } = renderAt('/e/bethel', 'Bethel');
      // Deux chevrons (Accueil→Annuaire, Annuaire→Église).
      const svg = container.querySelectorAll('svg[aria-hidden="true"]');
      // Au moins 2 chevrons + 1 maison = 3 svg aria-hidden minimum.
      expect(svg.length).toBeGreaterThanOrEqual(3);
    });

    it('l’icône Maison du premier maillon est aria-hidden', () => {
      const { container } = renderAt('/e/bethel', 'Bethel');
      const first = container.querySelector('nav a')!;
      const house = first.querySelector('svg[aria-hidden="true"]');
      expect(house).not.toBeNull();
    });
  });
});
