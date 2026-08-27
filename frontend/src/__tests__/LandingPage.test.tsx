import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, MemoryRouter, Routes, Route } from 'react-router-dom';
import LandingPage from '@/pages/LandingPage';
import AuthLayout from '@/layouts/AuthLayout';
import LoginPage from '@/pages/LoginPage';
import DemoModal from '@/components/landing/DemoModal';

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

vi.mock('@/contexts/AuthContext', async () => {
  const actual = await vi.importActual('@/contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn().mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
      user: null,
      login: vi.fn(),
      logout: vi.fn(),
      roles: [],
      roleLabels: {},
    }),
  };
});

describe('LandingPage — page accueil', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    vi.spyOn(window, 'open').mockImplementation(() => null);
  });

  it('rend la marque et le titre du hero', () => {
    render(
      <BrowserRouter>
        <LandingPage />
      </BrowserRouter>
    );
    expect(screen.getAllByText('Discipolat').length).toBeGreaterThanOrEqual(1);
    // Le titre du hero peut être découpé en plusieurs spans (décor + gradient).
    expect(screen.getAllByText(/croissance spirituelle/i).length).toBeGreaterThanOrEqual(1);
  });

  it('affiche les boutons de connexion et les fonctionnalites', () => {
    render(
      <BrowserRouter>
        <LandingPage />
      </BrowserRouter>
    );
    expect(screen.getAllByRole('link', { name: /connexion/i }).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Suivi des disciples')).toBeInTheDocument();
    expect(screen.getByText('Reporting hebdomadaire')).toBeInTheDocument();
  });

  it('le lien Connexion pointe vers /login', () => {
    render(
      <BrowserRouter>
        <LandingPage />
      </BrowserRouter>
    );
    const links = screen.getAllByRole('link', { name: /connexion/i });
    expect(links[0]).toHaveAttribute('href', '/login');
  });

  it('le toggle de theme bascule la classe dark sur html', async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <LandingPage />
      </BrowserRouter>
    );

    expect(document.documentElement.classList.contains('dark')).toBe(false);

    await user.click(screen.getByRole('button', { name: /passer en mode sombre/i }));

    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem('darkMode')).toBe('true');

    await user.click(screen.getByRole('button', { name: /passer en mode clair/i }));

    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem('darkMode')).toBe('false');
  });

  it('la modale démo s\'ouvre, valide les champs et affiche la confirmation', async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <LandingPage />
      </BrowserRouter>
    );

    // Ouvre la démo depuis le CTA du hero.
    await user.click(screen.getAllByRole('button', { name: /demander une démonstration/i })[0]);
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    // Envoi sans remplir → message d'erreur.
    await user.click(screen.getByRole('button', { name: /envoyer la demande/i }));
    expect(screen.getByText(/remplir votre nom, votre email et le nom/i)).toBeInTheDocument();

    // Remplissage + envoi → succès.
    await user.type(screen.getByLabelText(/votre nom/i), 'Jean Dupont');
    await user.type(screen.getByLabelText(/email professionnel/i), 'jean@eglise.org');
    await user.type(screen.getByLabelText(/nom de l’église/i), 'Église Locale');
    await user.click(screen.getByRole('button', { name: /envoyer la demande/i }));

    expect(await screen.findByText(/demande envoyée/i)).toBeInTheDocument();
  });

  it('l’explorateur de rôles change le contenu selon le rôle sélectionné', async () => {
    const user = userEvent.setup();
    render(
      <BrowserRouter>
        <LandingPage />
      </BrowserRouter>
    );

    // Rôle par défaut : Pasteur.
    expect(screen.getByText(/vue complète de toute l’église/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /membre/i }));
    expect(screen.getByText(/espace personnel\. activités, prières, événements/i)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /faiseur/i }));
    expect(screen.getByText(/accompagnement personnalisé de chaque disciple/i)).toBeInTheDocument();
  });

  it('la modale démo standalone fonctionne en isolation', async () => {
    const { container } = render(
      <DemoModal open onClose={() => {}} />
    );
    expect(container.querySelector('[role="dialog"]')).toBeInTheDocument();
  });
});

describe('AuthLayout — mode clair/sombre de la connexion', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
  });

  it('affiche le toggle et bascule le theme avec LoginPage', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    // Le formulaire de connexion est rendu dans la carte verre
    expect(screen.getByText('Connexion')).toBeInTheDocument();

    // Par defaut : clair
    expect(document.documentElement.classList.contains('dark')).toBe(false);

    await user.click(screen.getByRole('button', { name: /passer en mode sombre/i }));

    expect(document.documentElement.classList.contains('dark')).toBe(true);

    await user.click(screen.getByRole('button', { name: /passer en mode clair/i }));

    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('restaure le theme sombre persiste au montage', () => {
    localStorage.setItem('darkMode', 'true');
    render(
      <BrowserRouter>
        <AuthLayout />
      </BrowserRouter>
    );
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });
});
