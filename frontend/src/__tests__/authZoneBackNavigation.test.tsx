import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import BackButton from '@/components/navigation/BackButton';
import AuthLayout from '@/layouts/AuthLayout';
import ar from '@/i18n/ar';
import en from '@/i18n/en';
import es from '@/i18n/es';
import fr from '@/i18n/fr';
import pt from '@/i18n/pt';
import sw from '@/i18n/sw';

// AuthLayout lit le thème et la marque : on les rend déterministes sans
// provider, à l'identique des autres tests de pages du dépôt.
vi.mock('@/hooks/useTheme', () => ({
  useTheme: () => ({ darkMode: false, toggleTheme: vi.fn() }),
}));
vi.mock('@/contexts/SettingsContext', () => ({
  useSettings: () => ({
    branding: {
      platformName: 'Discipolat',
      logoUrl: '',
      slogan: '',
      description: '',
    },
  }),
}));

/** Témoin de la route réellement rendue après un clic. */
function LocationProbe() {
  const location = useLocation();
  return <div data-testid="probe">{location.pathname}</div>;
}

type BackButtonProps = Parameters<typeof BackButton>[0];

function renderBack(at: string, props: BackButtonProps = {}) {
  return render(
    <MemoryRouter initialEntries={[at]}>
      <Routes>
        <Route path="/register" element={<BackButton {...props} />} />
        <Route path="/departments/12/report" element={<BackButton {...props} />} />
        <Route path="*" element={<LocationProbe />} />
      </Routes>
    </MemoryRouter>,
  );
}

/** Simule l'état que react-router écrit dans `window.history.state`. */
function setHistoryIdx(idx: number | null) {
  window.history.replaceState(idx === null ? null : { idx }, '', '/prefill');
}

describe('LOT 3 §BK — retour dans la zone sans layout (AuthLayout)', () => {
  beforeEach(() => {
    setHistoryIdx(null);
  });

  it('monte un contrôle de retour dans la zone auth (le trou mesuré)', () => {
    setHistoryIdx(0); // atterrissage direct sur /login
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<AuthLayout />} />
          <Route path="*" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByTestId('back-button')).toBeInTheDocument();
  });

  it('atterrissage direct : le bouton dit « Retour à l’accueil » et mène au landing', async () => {
    setHistoryIdx(0);
    renderBack('/register', { detectHistory: true, fallbackTo: '/' });

    const button = screen.getByRole('button', { name: "Retour à l'accueil" });
    expect(button).toHaveAttribute('data-back-source', 'none');

    await userEvent.click(button);
    // Assertion exacte : tout chemin contient « / », un substring ne prouverait rien.
    expect(screen.getByTestId('probe').textContent).toBe('/');
  });

  it('historique réel : le bouton reste un « Retour » vers la page précédente', () => {
    setHistoryIdx(3);
    renderBack('/register', { detectHistory: true, fallbackTo: '/' });

    const button = screen.getByRole('button', { name: 'Retour' });
    expect(button).toHaveAttribute('data-back-source', 'history');
  });

  it('NON-RÉGRESSION : sans la prop, le comportement de MainLayout est intact', () => {
    // `<BackButton />` est la seule forme utilisée par MainLayout.tsx:106.
    // Ce test échoue dès que la détection d'historique devient le défaut.
    setHistoryIdx(0);
    renderBack('/register');

    const button = screen.getByRole('button', { name: 'Retour' });
    expect(button).toHaveAttribute('data-back-source', 'history');
  });

  it('la cible forcée ne remplace jamais un parent déductible', async () => {
    setHistoryIdx(0);
    renderBack('/departments/12/report', { detectHistory: true, fallbackTo: '/' });

    const button = screen.getByRole('button', { name: 'Retour' });
    expect(button).toHaveAttribute('data-back-source', 'parent');

    await userEvent.click(button);
    expect(screen.getByTestId('probe').textContent).toBe('/departments/12');
  });

  it('la clé nav.backHome existe dans les six locales (parité i18n)', () => {
    const dictionaries = { fr, en, es, pt, sw, ar } as const;
    for (const [locale, dict] of Object.entries(dictionaries)) {
      const value = (dict as Record<string, string>)['nav.backHome'];
      expect(value, `locale ${locale}`).toBeTruthy();
      // Jamais la clé brute : elle aurait été rendue telle quelle à l'écran.
      expect(value).not.toBe('nav.backHome');
    }
  });
});
