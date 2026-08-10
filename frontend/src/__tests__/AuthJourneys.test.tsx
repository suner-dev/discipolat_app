import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import AuthLayout from '@/layouts/AuthLayout';
import LandingPage from '@/pages/LandingPage';
import LoginPage from '@/pages/LoginPage';
import ForgotPasswordPage from '@/pages/ForgotPasswordPage';
import ResetPasswordPage from '@/pages/ResetPasswordPage';

const { apiGet, apiPost, apiPut, apiDelete, apiGetErrorMessage } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiDelete: vi.fn(),
  apiGetErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: apiPut,
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: apiGetErrorMessage,
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

function DashboardStub() {
  return <div>Tableau de bord (stub)</div>;
}

const SINGLE_ROLE_USER = {
  userId: 'u1',
  email: 'faiseur@discipolat.com',
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  role: 'FAISEUR',
  roles: ['FAISEUR'],
  activeRole: 'FAISEUR',
  firstName: 'Paul',
  lastName: 'Martin',
  estChefDeFamille: false,
  statut: 'ACTIVE',
  twoFactorEnabled: false,
};

function renderJourney(initialPath = '/') {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/reset-password" element={<ResetPasswordPage />} />
            </Route>
            <Route path="/dashboard" element={<DashboardStub />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('Parcours d authentification — bout en bout', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockResolvedValue({ data: {} });
    apiPut.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
    apiPost.mockResolvedValue({ data: SINGLE_ROLE_USER });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('landing → Connexion → le formulaire de connexion est affiché', async () => {
    const user = userEvent.setup();
    renderJourney('/');

    // CTA « Connexion » de la landing
    const cta = screen.getAllByRole('link', { name: /connexion/i })[0];
    await user.click(cta);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /se connecter/i })).toBeInTheDocument();
    });
    expect(screen.getByPlaceholderText('vous@email.com')).toBeInTheDocument();
  });

  it('connexion → mot de passe oublié → demande envoyée → retour à la connexion', async () => {
    const user = userEvent.setup();
    renderJourney('/login');

    await user.click(screen.getByRole('link', { name: /mot de passe oublié/i }));

    await waitFor(() => {
      expect(screen.getByText('Mot de passe oublié')).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText('vous@email.com'), 'pasteur@discipolat.com');
    await user.click(screen.getByRole('button', { name: /envoyer le lien/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/auth/forgot-password', { email: 'pasteur@discipolat.com' });
    });
    await waitFor(() => {
      expect(screen.getByText('Email envoyé !')).toBeInTheDocument();
    });

    // Retour à la connexion → le formulaire redevient visible
    await user.click(screen.getByRole('link', { name: /retour à la connexion/i }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /se connecter/i })).toBeInTheDocument();
    });
  });

  it('réinitialisation complète : lien avec token → nouveau mot de passe → connexion', async () => {
    const user = userEvent.setup();
    renderJourney('/reset-password?token=token-abc');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Nouveau mot de passe' })).toBeInTheDocument();
    });

    await user.type(screen.getByPlaceholderText('Minimum 8 caractères'), 'Password123');
    await user.type(screen.getByPlaceholderText('Confirmez le mot de passe'), 'Password123');
    await user.click(screen.getByRole('button', { name: /réinitialiser le mot de passe/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/auth/reset-password', {
        token: 'token-abc',
        newPassword: 'Password123',
      });
    });
    await waitFor(() => {
      expect(screen.getByText('Mot de passe réinitialisé !')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /se connecter/i }));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /se connecter/i })).toBeInTheDocument();
      expect(screen.getByPlaceholderText('vous@email.com')).toBeInTheDocument();
    });
  });

  it('connexion réussie → dashboard (jwt persisté et accès espace métier)', async () => {
    const user = userEvent.setup();
    renderJourney('/login');

    await user.type(screen.getByPlaceholderText('vous@email.com'), 'faiseur@discipolat.com');
    await user.type(screen.getByPlaceholderText('••••••••'), 'password123');
    await user.click(screen.getByRole('button', { name: /se connecter/i }));

    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
    expect(localStorage.getItem('accessToken')).toBe('access-1');
    expect(localStorage.getItem('user')).toContain('"activeRole":"FAISEUR"');
  });
});
