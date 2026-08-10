import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter, MemoryRouter, Routes, Route } from 'react-router-dom';
import LandingPage from '@/pages/LandingPage';
import AuthLayout from '@/layouts/AuthLayout';
import LoginPage from '@/pages/LoginPage';

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
