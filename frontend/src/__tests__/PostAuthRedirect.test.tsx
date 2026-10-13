import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import { MetaProvider } from '@/contexts/MetaContext';
import LoginPage from '@/pages/LoginPage';
import TwoFactorChallengePage from '@/pages/TwoFactorChallengePage';
import MagicLinkVerifyPage from '@/pages/MagicLinkVerifyPage';

/**
 * La destination après authentification ne peut pas s'arrêter à `/login` : la 2FA
 * et le magic link sont deux étapes INSÉRÉES dans le même parcours. Chacune
 * traversait le chemin en perdant la destination — c'est-à-dire en ramenant au
 * dashboard un utilisateur qui venait de choisir son église.
 *
 *   /login?next=/j/x → 2FA → /verify-2fa?next=/j/x → /j/x
 *   /login  (église choisie) → magic link → /j/x
 */

const { apiGet, apiPost, apiPut, apiGetErrorMessage } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiGetErrorMessage: vi.fn(() => 'Erreur'),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: apiPost,
    put: apiPut,
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: apiGetErrorMessage,
}));

vi.mock('react-hot-toast', () => ({
  __esModule: true,
  default: { success: vi.fn(), error: vi.fn() },
  Toaster: () => null,
}));

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

const CHURCH = { name: 'Église Bethel de Cocody', slug: 'bethel-cocody' };

const USER_2FA = {
  userId: 'u1',
  email: 'membre@bethel.ci',
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  role: 'MEMBRE',
  roles: ['MEMBRE'],
  activeRole: 'MEMBRE',
  firstName: 'Marie',
  lastName: 'Kouassi',
  platformSuperAdmin: false,
  estChefDeFamille: false,
  statut: 'ACTIVE',
  twoFactorEnabled: true,
};

function JoinStub({ label = 'Page de rejointure (stub)' }: { label?: string }) {
  return <div>{label}</div>;
}

function DashboardStub() {
  return <div>Tableau de bord (stub)</div>;
}

function renderApp(entry: string) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entry]}>
        <MetaProvider>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/verify-2fa" element={<TwoFactorChallengePage />} />
              <Route path="/auth/magic-link" element={<MagicLinkVerifyPage />} />
              <Route path="/dashboard" element={<DashboardStub />} />
              <Route path="/j/:slug" element={<JoinStub />} />
              <Route path="/join" element={<JoinStub label="Page de rejointure par code (stub)" />} />
            </Routes>
          </AuthProvider>
        </MetaProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

async function submitPassword(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByPlaceholderText('vous@email.com'), 'membre@bethel.ci');
  await user.type(screen.getByPlaceholderText('••••••••'), 'password123');
  await user.click(screen.getByRole('button', { name: 'Se connecter' }));
}

async function requestMagicLink(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: /Recevoir un lien de connexion par email/i }));
  // Le formulaire de connexion reste à l'écran et porte le même placeholder :
  // on cadre sur le formulaire du magic link plutôt que sur un placeholder.
  const sendButton = screen.getByRole('button', { name: 'Envoyer' });
  const form = sendButton.closest('form') as HTMLFormElement;
  await user.type(within(form).getByRole('textbox'), 'membre@bethel.ci');
  await user.click(sendButton);
}

describe('destination après authentification — 2FA', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return { data: { total: 1, items: [CHURCH] } };
      return { data: {} };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('?next= traverse le challenge 2FA', async () => {
    apiPost.mockImplementation(async (url: string) => {
      if (url === '/auth/login') return { data: USER_2FA };
      if (url === '/auth/2fa/verify') return { data: { valid: true } };
      return { data: {} };
    });
    const user = userEvent.setup();
    renderApp('/login?next=%2Fj%2Fbethel-cocody');
    await submitPassword(user);

    // Le challenge est bien atteint, et IL transporte la destination.
    await waitFor(() => expect(screen.getByPlaceholderText('000000')).toBeInTheDocument());
    expect(apiPost).not.toHaveBeenCalledWith('/auth/2fa/verify', expect.anything());

    await user.type(screen.getByPlaceholderText('000000'), '123456');
    await user.click(screen.getByRole('button', { name: /^Vérifier$/ }));

    await waitFor(() => {
      expect(screen.getByText('Page de rejointure (stub)')).toBeInTheDocument();
    });
    expect(screen.queryByText('Tableau de bord (stub)')).not.toBeInTheDocument();
  });

  it('l’église choisie traverse aussi le challenge 2FA', async () => {
    apiPost.mockImplementation(async (url: string) => {
      if (url === '/auth/login') return { data: USER_2FA };
      if (url === '/auth/2fa/verify') return { data: { valid: true } };
      return { data: {} };
    });
    const user = userEvent.setup();
    renderApp('/login');
    await user.type(screen.getByRole('combobox'), 'Bethel');
    await user.click(await screen.findByRole('option', { name: /Église Bethel de Cocody/ }));
    await submitPassword(user);

    await user.type(await screen.findByPlaceholderText('000000'), '123456');
    await user.click(screen.getByRole('button', { name: /^Vérifier$/ }));

    await waitFor(() => {
      expect(screen.getByText('Page de rejointure (stub)')).toBeInTheDocument();
    });
  });

  it('sans destination, le challenge retombe sur le dashboard (comportement historique)', async () => {
    apiPost.mockImplementation(async (url: string) => {
      if (url === '/auth/login') return { data: USER_2FA };
      if (url === '/auth/2fa/verify') return { data: { valid: true } };
      return { data: {} };
    });
    const user = userEvent.setup();
    renderApp('/login');
    await submitPassword(user);

    await user.type(await screen.findByPlaceholderText('000000'), '123456');
    await user.click(screen.getByRole('button', { name: /^Vérifier$/ }));

    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('?next= vers une adresse externe dans le challenge : ignoré, retour au dashboard', async () => {
    apiPost.mockImplementation(async (url: string) => {
      if (url === '/auth/login') return { data: USER_2FA };
      if (url === '/auth/2fa/verify') return { data: { valid: true } };
      return { data: {} };
    });
    const user = userEvent.setup();
    // Session déjà en place pour shun le formulaire et atterrir au challenge.
    localStorage.setItem('accessToken', 'access-1');
    localStorage.setItem('refreshToken', 'refresh-1');
    localStorage.setItem('user', JSON.stringify(USER_2FA));
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/auth/me') return { data: USER_2FA };
      return { data: {} };
    });

    renderApp('/verify-2fa?next=https%3A%2F%2Fevil.example');
    await waitFor(() => expect(screen.getByPlaceholderText('000000')).toBeInTheDocument());

    await user.type(screen.getByPlaceholderText('000000'), '123456');
    await user.click(screen.getByRole('button', { name: /^Vérifier$/ }));

    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('un code 2FA invalide ne change pas de destination', async () => {
    apiPost.mockImplementation(async (url: string) => {
      if (url === '/auth/login') return { data: USER_2FA };
      if (url === '/auth/2fa/verify') return { data: { valid: false } };
      return { data: {} };
    });
    const user = userEvent.setup();
    renderApp('/login?next=%2Fj%2Fbethel-cocody');
    await submitPassword(user);

    await user.type(await screen.findByPlaceholderText('000000'), '000000');
    await user.click(screen.getByRole('button', { name: /^Vérifier$/ }));

    expect(await screen.findByText(/Code invalide/i)).toBeInTheDocument();
    expect(screen.queryByText('Page de rejointure (stub)')).not.toBeInTheDocument();
  });
});

describe('destination après authentification — magic link', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return { data: { total: 1, items: [CHURCH] } };
      if (url === '/auth/magic-link/verify') {
        return { data: { token: 'jwt', refreshToken: 'r1', user: { id: 'u1', email: 'membre@bethel.ci', firstName: 'Marie', lastName: 'Kouassi', role: 'MEMBRE' } } };
      }
      return { data: {} };
    });
    apiPost.mockResolvedValue({ data: {} });
    apiPut.mockResolvedValue({ data: {} });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('l’église choisie est retenue pendant l’envoi, puis rejouée au clic sur le lien', async () => {
    const user = userEvent.setup();
    renderApp('/login');
    await user.type(screen.getByRole('combobox'), 'Bethel');
    await user.click(await screen.findByRole('option', { name: /Église Bethel de Cocody/ }));
    await requestMagicLink(user);

    await waitFor(() => expect(apiPost).toHaveBeenCalledWith('/auth/magic-link', { email: 'membre@bethel.ci' }));
    expect(sessionStorage.getItem('discipolat.postAuthPath')).toBe('/j/bethel-cocody');
  });

  it('le clic sur le lien récupère le relais et mène à l’église', async () => {
    sessionStorage.setItem('discipolat.postAuthPath', '/j/bethel-cocody');

    renderApp('/auth/magic-link?token=abc');
    await waitFor(() => expect(screen.getByText('Connecté !')).toBeInTheDocument());
    expect(screen.getByText(/Redirection vers votre église/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Page de rejointure (stub)')).toBeInTheDocument();
    }, { timeout: 3000 });
    // Le relais est consommé : il ne rejouera pas au prochain lien.
    expect(sessionStorage.getItem('discipolat.postAuthPath')).toBeNull();
  });

  it('?next= dans l’URL du lien prime sur le relais', async () => {
    sessionStorage.setItem('discipolat.postAuthPath', '/j/relais');
    renderApp('/auth/magic-link?token=abc&next=%2Fjoin%3Fcode%3DABC');

    await waitFor(() => {
      expect(screen.getByText('Page de rejointure par code (stub)')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  it('un relais externe est ignoré : jamais d’open-redirect par le magic link', async () => {
    sessionStorage.setItem('discipolat.postAuthPath', 'https://evil.example');
    renderApp('/auth/magic-link?token=abc');

    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  it('sans relais ni ?next=, le magic link retombe sur le dashboard', async () => {
    renderApp('/auth/magic-link?token=abc');
    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    }, { timeout: 3000 });
  });

  it('un lien sans jeton reste une erreur explicite', async () => {
    renderApp('/auth/magic-link');
    expect(await screen.findByText(/Lien incomplet/i)).toBeInTheDocument();
  });
});
