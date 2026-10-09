import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, cleanup } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import BrandLink, { type BrandLinkProps } from '@/components/shared/BrandLink';
import LandingNavbar from '@/components/landing/LandingNavbar';

// LandingNavbar lit le thème et la marque ; on les rend déterministes.
vi.mock('@/hooks/useTheme', () => ({
  useTheme: () => ({ darkMode: false, toggleTheme: vi.fn() }),
}));
vi.mock('@/contexts/SettingsContext', () => ({
  useSettings: () => ({
    branding: { platformName: 'Discipolat', logoUrl: '', slogan: '', description: '', churchName: '' },
  }),
}));

/** Témoin de la route rendue, et des entrées d'historique réellement créées. */
function makeProbe(seenKeys: string[]) {
  return function Probe() {
    const location = useLocation();
    if (!seenKeys.includes(location.key)) seenKeys.push(location.key);
    return <div data-testid="probe">{location.pathname}</div>;
  };
}

describe('LOT 2 §BR — la marque est un lien vers le landing', () => {
  let seenKeys: string[];

  beforeEach(() => {
    seenKeys = [];
    cleanup();
  });

  function renderBrand(at: string, props: Omit<BrandLinkProps, 'children'>) {
    const Probe = makeProbe(seenKeys);
    return render(
      <MemoryRouter initialEntries={[at]}>
        <Routes>
          <Route
            path="/register"
            element={
              <BrandLink {...props}>
                <span>Discipolat</span>
              </BrandLink>
            }
          />
          <Route path="/" element={<Probe />} />
          <Route path="*" element={<Probe />} />
        </Routes>
      </MemoryRouter>,
    );
  }

  it('rend un VRAI lien accessible, et non une div cliquable', () => {
    renderBrand('/register', { ariaLabel: "Retour à l'accueil" });
    const link = screen.getByRole('link', { name: "Retour à l'accueil" });
    expect(link).toHaveAttribute('href', '/');
  });

  it('le lien est atteignable au clavier', async () => {
    renderBrand('/register', { ariaLabel: "Retour à l'accueil" });
    await userEvent.tab();
    expect(screen.getByRole('link', { name: "Retour à l'accueil" })).toHaveFocus();
  });

  it('depuis une autre page, le clic mène au landing', async () => {
    renderBrand('/register', { ariaLabel: "Retour à l'accueil" });
    await userEvent.click(screen.getByRole('link', { name: "Retour à l'accueil" }));
    expect(screen.getByTestId('probe').textContent).toBe('/');
  });

  it('sur le landing déjà chargé, on appelle onSameRoute sans empiler d’historique', async () => {
    const onSameRoute = vi.fn();
    // Le bloc est rendu par la route « / » : la marque elle-même ne s'affiche
    // qu'à travers l'hôte, on vérifie donc le comportement du lien à '/' via
    // un second montage sur la route racine.
    const Probe = makeProbe(seenKeys);
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route
            path="/"
            element={
              <>
                <BrandLink ariaLabel="Accueil" onSameRoute={onSameRoute}>
                  <span>Discipolat</span>
                </BrandLink>
                <Probe />
              </>
            }
          />
          <Route path="*" element={<Probe />} />
        </Routes>
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole('link', { name: 'Accueil' }));

    expect(onSameRoute).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId('probe').textContent).toBe('/');
    // Un `<Link>` non intercepté créerait une nouvelle entrée d'historique sur
    // la même page : le « retour » précédent retomberait sur l'identique.
    expect(seenKeys).toHaveLength(1);
  });

  it('LandingNavbar : sur le landing, le clic garde l’ancre héro (comportement d’avant)', async () => {
    const onNavigate = vi.fn();
    const Probe = makeProbe(seenKeys);
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<><LandingNavbar onNavigate={onNavigate} onDemo={vi.fn()} /><Probe /></>} />
          <Route path="*" element={<Probe />} />
        </Routes>
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole('link', { name: "Retour à l'accueil" }));
    expect(onNavigate).toHaveBeenCalledWith('hero');
    expect(screen.getByTestId('probe').textContent).toBe('/');
  });

  it('LandingNavbar : ailleurs que sur le landing, le clic ramène au landing', async () => {
    const onNavigate = vi.fn();
    const Probe = makeProbe(seenKeys);
    render(
      <MemoryRouter initialEntries={['/eglises']}>
        <Routes>
          <Route path="/eglises" element={<><LandingNavbar onNavigate={onNavigate} onDemo={vi.fn()} /><Probe /></>} />
          <Route path="/" element={<Probe />} />
          <Route path="*" element={<Probe />} />
        </Routes>
      </MemoryRouter>,
    );

    await userEvent.click(screen.getByRole('link', { name: "Retour à l'accueil" }));
    expect(onNavigate).not.toHaveBeenCalled();
    expect(screen.getByTestId('probe').textContent).toBe('/');
  });
});
