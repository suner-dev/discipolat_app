import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { AuthProvider } from '@/contexts/AuthContext';
import TwoFactorChallengePage from '@/pages/TwoFactorChallengePage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiPost, apiGetErrorMessage } = vi.hoisted(() => ({
  apiPost: vi.fn(),
  apiGetErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: {} }),
    post: apiPost,
    put: vi.fn(),
    delete: vi.fn(),
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

function LoginStub() {
  return <div>Page de connexion (stub)</div>;
}

const TWO_FA_USER = {
  id: 'u1',
  email: 'pasteur@discipolat.com',
  role: 'PASTEUR',
  roles: ['PASTEUR'],
  activeRole: 'PASTEUR',
  firstName: 'Paul',
  lastName: 'Martin',
  twoFactorEnabled: true,
  statut: 'ACTIVE',
};

function renderPage(user: Record<string, unknown> | null = TWO_FA_USER) {
  localStorage.setItem('user', JSON.stringify(user));
  localStorage.setItem('accessToken', 'access-token');
  localStorage.setItem('refreshToken', 'refresh-token');
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/verify-2fa']}>
        <AuthProvider>
          <Routes>
            <Route path="/verify-2fa" element={<TwoFactorChallengePage />} />
            <Route path="/dashboard" element={<DashboardStub />} />
            <Route path="/login" element={<LoginStub />} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('TwoFactorChallengePage — vérification en deux étapes', () => {
  beforeEach(() => {
    localStorage.clear();
    queryClient.clear();
    vi.clearAllMocks();
    apiPost.mockResolvedValue({ data: { valid: true } });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('affiche le challenge 2FA pour un utilisateur qui la activée', () => {
    renderPage();
    expect(screen.getByText('Authentification à deux facteurs')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('000000')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /vérifier/i })).toBeInTheDocument();
  });

  it('redirige vers /dashboard si la 2FA est désactivée', async () => {
    renderPage({ ...TWO_FA_USER, twoFactorEnabled: false });

    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('le champ code ne conserve que les chiffres et est limité à 6', async () => {
    const user = userEvent.setup();
    renderPage();

    const input = screen.getByPlaceholderText('000000') as HTMLInputElement;
    // Bouton désactivé tant que le code n'a pas exactement 6 chiffres
    expect(screen.getByRole('button', { name: /vérifier/i })).toBeDisabled();

    await user.type(input, '12a45x789');

    expect(input.value).toBe('124578');
    expect(input.value).toHaveLength(6);
    // Le bouton n'est actif qu'avec exactement 6 chiffres
    expect(screen.getByRole('button', { name: /vérifier/i })).toBeEnabled();
  });

  it('code valide → POST /auth/2fa/verify, toast de bienvenue et navigation /dashboard', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('000000'), '123456');
    fireEvent.click(screen.getByRole('button', { name: /vérifier/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/auth/2fa/verify', { code: '123456' });
    });
    expect(toast.success).toHaveBeenCalledWith('Bienvenue, Paul!');
    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('code invalide (valid=false) → message derreur sans navigation', async () => {
    apiPost.mockResolvedValueOnce({ data: { valid: false } });
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('000000'), '999999');
    fireEvent.click(screen.getByRole('button', { name: /vérifier/i }));

    await waitFor(() => {
      expect(screen.getByText('Code invalide. Veuillez réessayer.')).toBeInTheDocument();
    });
    expect(screen.queryByText('Tableau de bord (stub)')).not.toBeInTheDocument();
  });

  it('erreur API → message getErrorMessage affiché', async () => {
    apiPost.mockRejectedValueOnce(new Error('rate limited'));
    apiGetErrorMessage.mockReturnValue('Trop de tentatives');
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('000000'), '123456');
    fireEvent.click(screen.getByRole('button', { name: /vérifier/i }));

    await waitFor(() => {
      expect(screen.getByText('Trop de tentatives')).toBeInTheDocument();
    });
  });

  it('"Retour à la connexion" déconnecte et navigue vers /login', async () => {
    const user = userEvent.setup();
    renderPage();

    fireEvent.click(screen.getByRole('button', { name: /retour à la connexion/i }));

    await waitFor(() => {
      expect(screen.getByText('Page de connexion (stub)')).toBeInTheDocument();
    });
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(toast.success).toHaveBeenCalledWith('Déconnexion réussie');
  });
});
