import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import { MetaProvider } from '@/contexts/MetaContext';
import LoginPage from '@/pages/LoginPage';

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

function Verify2faStub() {
  return <div>Vérification 2FA (stub)</div>;
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

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/login']}>
        <MetaProvider>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/dashboard" element={<DashboardStub />} />
              <Route path="/verify-2fa" element={<Verify2faStub />} />
            </Routes>
          </AuthProvider>
        </MetaProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

async function submitCredentials(email = 'faiseur@discipolat.com', password = 'password123') {
  const user = userEvent.setup();
  await user.type(screen.getByPlaceholderText('vous@email.com'), email);
  await user.type(screen.getByPlaceholderText('••••••••'), password);
  await user.click(screen.getByRole('button', { name: /se connecter/i }));
}

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') {
        return { data: { betaMode: true, demoAccountsEnabled: true } };
      }
      return { data: {} };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
    apiPost.mockImplementation(async (url: string) => {
      if (url === '/auth/switch-role') return { data: { accessToken: 'access-2', refreshToken: 'refresh-2' } };
      return { data: SINGLE_ROLE_USER };
    });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('renders login form with title', () => {
    renderPage();
    expect(screen.getByText('Connexion')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /se connecter/i })).toBeInTheDocument();
  });

  it('renders email and password inputs', () => {
    renderPage();
    expect(screen.getByPlaceholderText('vous@email.com')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('••••••••')).toBeInTheDocument();
  });

  it('shows demo accounts + admin + multi-role accounts when the server enables them (beta)', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText(/pasteur@discipolat.com/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/admin@discipolat.com/i)).toBeInTheDocument();
    expect(screen.getByText(/paul@discipolat.com/i)).toBeInTheDocument();
    expect(screen.getByText(/password123/)).toBeInTheDocument();
  });

  it('hides demo accounts when the server disables them (production)', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') {
        return { data: { betaMode: false, demoAccountsEnabled: false } };
      }
      return { data: {} };
    });
    renderPage();
    // Le fetch du meta doit avoir eu le temps de s'appliquer (affichage conditionnel).
    await waitFor(() => {
      expect(screen.queryByText(/pasteur@discipolat.com/i)).not.toBeInTheDocument();
    });
    expect(screen.queryByText(/Comptes de démonstration/i)).not.toBeInTheDocument();
  });

  it('shows the BÊTA badge in beta mode', async () => {
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Bêta')).toBeInTheDocument();
    });
  });

  it('affiche une erreur de validation sans appel API si l email est invalide', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.type(screen.getByPlaceholderText('vous@email.com'), 'pas-un-email');
    await user.type(screen.getByPlaceholderText('••••••••'), 'password123');

    // fireEvent.submit contourne la validation native jsdom du type=email pour
    // laisser zodResolver produire son erreur (le submit du bouton serait bloqué).
    const form = screen.getByPlaceholderText('vous@email.com').closest('form');
    fireEvent.submit(form!);

    await waitFor(() => {
      expect(screen.getByText('Email invalide')).toBeInTheDocument();
    });
    expect(apiPost).not.toHaveBeenCalled();
  });

  it('se connecte avec succès (rôle unique) : POST /auth/login puis navigation vers /dashboard', async () => {
    renderPage();
    await submitCredentials();

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/auth/login', {
        email: 'faiseur@discipolat.com',
        password: 'password123',
      });
    });
    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('navigue vers /verify-2fa quand la 2FA est activée sur le compte', async () => {
    apiPost.mockResolvedValueOnce({ data: { ...SINGLE_ROLE_USER, twoFactorEnabled: true } });
    renderPage();
    await submitCredentials();

    await waitFor(() => {
      expect(screen.getByText('Vérification 2FA (stub)')).toBeInTheDocument();
    });
  });

  it('affiche le sélecteur de rôle pour un utilisateur multi-rôles et navigue après sélection', async () => {
    apiPost.mockResolvedValueOnce({
      data: { ...SINGLE_ROLE_USER, roles: ['FAISEUR', 'RESPONSABLE'], activeRole: 'FAISEUR' },
    });
    renderPage();
    await submitCredentials();

    // Le sélecteur de rôle s'affiche (pas de redirection automatique)
    await waitFor(() => {
      expect(screen.getByText(/Bienvenue, Paul/i)).toBeInTheDocument();
    });
    // Seuls les rôles du compte sont proposés (pas de rôle non possédé)
    expect(screen.queryByRole('button', { name: /chef de famille/i })).not.toBeInTheDocument();

    // Sélection du rôle FAISEUR → switch-role puis dashboard
    fireEvent.click(screen.getByRole('button', { name: /suivi de vos disciples/i }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/auth/switch-role', { role: 'FAISEUR' });
    });
    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('affiche le message d erreur quand la connexion échoue', async () => {
    apiPost.mockRejectedValueOnce(new Error('bad credentials'));
    apiGetErrorMessage.mockReturnValue('Email ou mot de passe incorrect');
    renderPage();
    await submitCredentials();

    await waitFor(() => {
      expect(screen.getByText('Email ou mot de passe incorrect')).toBeInTheDocument();
    });
    expect(screen.queryByText('Tableau de bord (stub)')).not.toBeInTheDocument();
  });

  it('le lien "Mot de passe oublié ?" pointe vers /forgot-password', () => {
    renderPage();
    expect(screen.getByRole('link', { name: /mot de passe oublié/i })).toHaveAttribute('href', '/forgot-password');
  });

  it('le bouton œil bascule la visibilité du mot de passe', async () => {
    renderPage();
    const passwordInput = screen.getByPlaceholderText('••••••••') as HTMLInputElement;
    expect(passwordInput.type).toBe('password');

    const eyeButton = document.querySelector('button svg.lucide-eye')?.closest('button') as HTMLButtonElement;
    expect(eyeButton).toBeTruthy();
    fireEvent.click(eyeButton);

    expect((screen.getByPlaceholderText('••••••••') as HTMLInputElement).type).toBe('text');
  });
});
