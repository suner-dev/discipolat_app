import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import { MetaProvider } from '@/contexts/MetaContext';
import LoginPage from '@/pages/LoginPage';
import JoinChurchPage from '@/pages/JoinChurchPage';

/**
 * LOT 1 §GLISE-D'ABORD — la chaîne COMPLÈTE, avec la VRAIE JoinChurchPage
 * montée (et non un stub), parce que la question qui compte n'est pas « le
 * picker trouve-t-il l'église ? » mais « après la connexion, l'utilisateur
 * trouve-t-il un BOUTON pour rejoindre ? ».
 *
 * <p>Le contrat testé ici est le cœur du produit :
 *   recherche → église trouvée → connexion → page de rejointure de CETTE église
 *   → bouton « Rejoindre maintenant » → `POST /tenant/join`.
 *
 * <p>Aucune adhésion n'est créée sans ce clic : c'est ce qui préserve la preuve
 * exigée (D1/R1). Le test verrouille donc l'EXISTENCE du bouton autant que son
 * absence d'automatisation.
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

const CHURCH = { name: 'Église Bethel de Cocody', slug: 'bethel-cocody', city: 'Abidjan' };

const LOGIN_USER = {
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
  twoFactorEnabled: false,
};

function DashboardStub() {
  return <div>Tableau de bord (stub)</div>;
}

function renderApp(entry = '/login') {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entry]}>
        <MetaProvider>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/dashboard" element={<DashboardStub />} />
              {/* La VRAIE page : c'est elle qui doit porter le bouton. */}
              <Route path="/j/:slug" element={<JoinChurchPage />} />
              <Route path="/join" element={<JoinChurchPage />} />
            </Routes>
          </AuthProvider>
        </MetaProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

/** Suggests + login, du premier écran au dernier. */
async function researchAndLogin(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByRole('combobox'), 'Bethel');
  await waitFor(() => expect(apiGet).toHaveBeenCalledWith(
    '/public/churches/suggest',
    expect.objectContaining({ params: { q: 'Bethel' } }),
  ));
  await user.click(await screen.findByRole('option', { name: /Église Bethel de Cocody/ }));
  await user.type(screen.getByPlaceholderText('vous@email.com'), 'membre@bethel.ci');
  await user.type(screen.getByPlaceholderText('••••••••'), 'password123');
  await user.click(screen.getByRole('button', { name: 'Se connecter' }));
}

describe('chaîne « église d’abord » — recherche → connexion → bouton Rejoindre', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return { data: { total: 1, items: [CHURCH] } };
      // Le serveur résout le slug sur la MÊME source que le picker (Tenant.slug).
      if (url === '/public/join/resolve') {
        return { data: { found: true, churchName: CHURCH.name, slug: CHURCH.slug, joinMode: 'OPEN', requiresApproval: false } };
      }
      return { data: {} };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiPost.mockImplementation(async (url: string) => {
      if (url === '/auth/login') return { data: LOGIN_USER };
      if (url === '/tenant/join') {
        return { data: { status: 'JOINED', tenantName: CHURCH.name, accessToken: 'access-2', refreshToken: 'refresh-2' } };
      }
      return { data: {} };
    });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('la page de rejointure de l’église choisie AFFICHE le bouton « Rejoindre maintenant »', async () => {
    const user = userEvent.setup();
    renderApp();
    await researchAndLogin(user);

    // Le serveur a bien été interrogé sur le slug de l'église choisie.
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith(
      '/public/join/resolve',
      expect.objectContaining({ params: { slug: 'bethel-cocody' } }),
    ));

    expect(await screen.findByText('Église Bethel de Cocody')).toBeInTheDocument();
    const joinButton = screen.getByRole('button', { name: /rejoindre maintenant/i });
    expect(joinButton).toBeInTheDocument();
    expect(joinButton).toBeEnabled();
    // Aucune adhésion avant le clic : la PREUVE reste obligatoire.
    expect(apiPost).not.toHaveBeenCalledWith('/tenant/join', expect.anything());
  });

  it('le bouton déclenche la rejointure : POST /tenant/join avec le slug, puis bascule', async () => {
    const user = userEvent.setup();
    renderApp();
    await researchAndLogin(user);

    await user.click(await screen.findByRole('button', { name: /rejoindre maintenant/i }));

    await waitFor(() => expect(apiPost).toHaveBeenCalledWith('/tenant/join', { slug: 'bethel-cocody' }));
    // Les jetons réémis sont adoptés puis on atterrit dans l'église rejointe.
    await waitFor(() => expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument());
  });

  it('?next= /j/<slug> mène au MÊME bouton (parcours « j’ai déjà un compte »)', async () => {
    localStorage.setItem('accessToken', 'access-1');
    localStorage.setItem('refreshToken', 'refresh-1');
    localStorage.setItem('user', JSON.stringify(LOGIN_USER));
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/auth/me') return { data: LOGIN_USER };
      if (url === '/public/join/resolve') {
        return { data: { found: true, churchName: CHURCH.name, slug: CHURCH.slug, joinMode: 'OPEN', requiresApproval: false } };
      }
      return { data: {} };
    });

    // Session déjà établie : plus de formulaire à l'écran, le garde de
    // `LoginPage` doit emmener directement à l'étape de rejointure.
    renderApp('/login?next=%2Fj%2Fbethel-cocody');

    expect(await screen.findByRole('button', { name: /rejoindre maintenant/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Se connecter' })).not.toBeInTheDocument();
  });

  it('église en validation : le bouton devient « Confirmer » et le bandeau d’approbation s’affiche', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return { data: { total: 1, items: [CHURCH] } };
      if (url === '/public/join/resolve') {
        return { data: { found: true, churchName: CHURCH.name, slug: CHURCH.slug, joinMode: 'APPROVAL', requiresApproval: true } };
      }
      return { data: {} };
    });
    const user = userEvent.setup();
    renderApp();
    await researchAndLogin(user);

    expect(await screen.findByText(/valide chaque demande d’adhésion/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /rejoindre maintenant/i })).toBeInTheDocument();
  });
  it('slug non résolvable : aucun bouton, mais JAMAIS de cul-de-sac', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return { data: { total: 1, items: [CHURCH] } };
      if (url === '/public/join/resolve') return { data: { found: false } };
      return { data: {} };
    });
    const user = userEvent.setup();
    renderApp();
    await researchAndLogin(user);

    await waitFor(() => expect(apiGet).toHaveBeenCalledWith(
      '/public/join/resolve',
      expect.objectContaining({ params: { slug: 'bethel-cocody' } }),
    ));

    // Pas d'adhésion promise...
    expect(screen.queryByRole('button', { name: /rejoindre maintenant/i })).not.toBeInTheDocument();
    // ...mais trois issues, sinon l'utilisateur connecté reste bloqué ici.
    expect(screen.getByRole('link', { name: /code d’entrée/i })).toHaveAttribute('href', '/join');
    expect(screen.getByRole('link', { name: /annuaire/i })).toHaveAttribute('href', '/eglises');
    expect(screen.getByRole('link', { name: /Créer mon église/i })).toHaveAttribute('href', '/register?mode=church');
  });

  it('résolution en 429 : même issue que « introuvable », et l’erreur reste visible', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return { data: { total: 1, items: [CHURCH] } };
      if (url === '/public/join/resolve') throw new Error('429');
      return { data: {} };
    });
    apiGetErrorMessage.mockReturnValue('Trop de tentatives. Réessayez dans une minute.');
    const user = userEvent.setup();
    renderApp();
    await researchAndLogin(user);

    // Pas de confusion avec « église inconnue » : le quota est nommé.
    expect(await screen.findByText('Trop de tentatives. Réessayez dans une minute.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /code d’entrée/i })).toHaveAttribute('href', '/join');
  });
});
