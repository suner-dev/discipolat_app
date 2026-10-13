import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import { MetaProvider } from '@/contexts/MetaContext';
import LoginPage from '@/pages/LoginPage';

/**
 * LOT 1 §GLISE-D'ABORD (T1.4) — le sélecteur d'église TELLEMENT qu'il est rendu
 * sur la page de connexion, et la fermeture du flux « j'ai trouvé mon église →
 * je me connecte / je m'inscris DANS cette église ».
 *
 * <p>Ces tests manquaient : `LoginPage.test.tsx` ne disait rien du picker et
 * `ChurchPicker.test.tsx` ne le montait que nu. La régression qu'ils verrouillent
 * est réelle et déjà arrivée en production : `/j/:slug` émettait
 * `/login?next=/j/<slug>`, `LoginPage` ignorait `next`, et la connexion
 * atterrissait sur `/dashboard` — l'utilisateur perdait son église entre les deux
 * écrans sans jamais le savoir.
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

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

const SUGGESTS = {
  data: {
    total: 1,
    items: [{ name: 'Église Bethel de Cocody', slug: 'bethel-cocody', city: 'Abidjan', country: "Côte d'Ivoire" }],
  },
};

const SINGLE_ROLE_USER = {
  userId: 'u1',
  email: 'membre@bethel.ci',
  accessToken: 'access-1',
  refreshToken: 'refresh-1',
  role: 'MEMBRE',
  roles: ['MEMBRE'],
  activeRole: 'MEMBRE',
  firstName: 'Marie',
  lastName: 'Kouassi',
  estChefDeFamille: false,
  statut: 'ACTIVE',
  twoFactorEnabled: false,
};

function DashboardStub() {
  return <div>Tableau de bord (stub)</div>;
}

function JoinStub() {
  return <div>Page de rejointure (stub)</div>;
}

function renderPage(entry = '/login') {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entry]}>
        <MetaProvider>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/dashboard" element={<DashboardStub />} />
              <Route path="/join/*" element={<JoinStub />} />
              <Route path="/j/:slug" element={<JoinStub />} />
            </Routes>
          </AuthProvider>
        </MetaProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

/** Tape dans le picker et attends la liste de suggestions (debounce 300 ms). */
async function pickChurch(user: ReturnType<typeof userEvent.setup>, query = 'Bethel') {
  const input = screen.getByRole('combobox');
  await user.type(input, query);
  await waitFor(() => expect(apiGet).toHaveBeenCalledWith(
    '/public/churches/suggest',
    expect.objectContaining({ params: { q: query } }),
  ));
  return screen.findByRole('option', { name: /Église Bethel de Cocody/ });
}

async function login(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByPlaceholderText('vous@email.com'), 'membre@bethel.ci');
  await user.type(screen.getByPlaceholderText('••••••••'), 'password123');
  await user.click(screen.getByRole('button', { name: 'Se connecter' }));
}

describe('LoginPage — sélecteur d’église (LOT 1 §GLISE-D’ABORD)', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return SUGGESTS;
      return { data: {} };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiPost.mockResolvedValue({ data: SINGLE_ROLE_USER });
    apiGetErrorMessage.mockReturnValue('Erreur');
  });

  it('affiche la recherche d’église avec son libellé sur la page de connexion', () => {
    renderPage();
    expect(screen.getByText('Vous cherchez votre église ?')).toBeInTheDocument();
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('la saisie interroge /suggest (listées seulement) et rend les suggestions', async () => {
    const user = userEvent.setup();
    renderPage();
    const option = await pickChurch(user);

    expect(screen.getByRole('listbox')).toBeInTheDocument();
    // La ville et le pays discriminent deux églises homonymes.
    expect(screen.getByText(/Abidjan/)).toBeInTheDocument();

    await user.click(option);
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  });

  it('église trouvée : propose la rejointure ET l’inscription directe dans CETTE église', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(await pickChurch(user));

    expect(screen.getByText(/Église trouvée : Église Bethel de Cocody/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Ouvrir la page de rejointure/i }))
      .toHaveAttribute('href', '/j/bethel-cocody');
    // Le lien d'inscription porte le contrat déjà implémenté par RegisterPage
    // (`?tenant=` rattache le compte ; `?church=` pré-affiche le nom).
    expect(screen.getByRole('link', { name: /S'inscrire dans cette église/i }))
      .toHaveAttribute('href', '/register?tenant=bethel-cocody&church=%C3%89glise%20Bethel%20de%20Cocody');
    expect(screen.getByRole('link', { name: /J'ai un code d'entrée/i })).toHaveAttribute('href', '/join');
  });

  it('APRÈS connexion, l’église choisie est celle qui attend l’utilisateur', async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(await pickChurch(user));
    await login(user);

    // Ni le dashboard ni une page inconnue : la page de rejointure de l'église.
    await waitFor(() => {
      expect(screen.getByText('Page de rejointure (stub)')).toBeInTheDocument();
    });
    expect(screen.queryByText('Tableau de bord (stub)')).not.toBeInTheDocument();
  });

  it('sans église choisie, la connexion garde son comportement historique (dashboard)', async () => {
    const user = userEvent.setup();
    renderPage();
    await login(user);

    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('?next= est honoré : l’utilisateur revient à l’étape de rejointure interrompue', async () => {
    const user = userEvent.setup();
    renderPage('/login?next=%2Fj%2Fbethel-cocody');
    await login(user);

    await waitFor(() => {
      expect(screen.getByText('Page de rejointure (stub)')).toBeInTheDocument();
    });
    expect(screen.queryByText('Tableau de bord (stub)')).not.toBeInTheDocument();
  });

  it('?next= affiche un bandeau : sans lui, l’utilisateur ignore ce qui l’attend', () => {
    renderPage('/login?next=%2Fj%2Fbethel-cocody');
    expect(screen.getByText(/vous serez ramené à l'étape précédente/i)).toBeInTheDocument();
  });

  it('?next= prime sur l’église choisie dans le picker (le flux en cours gagne)', async () => {
    const user = userEvent.setup();
    renderPage('/login?next=%2Fjoin%2Fcode%3DABC');
    await user.click(await pickChurch(user));
    await login(user);

    await waitFor(() => {
      expect(screen.getByText('Page de rejointure (stub)')).toBeInTheDocument();
    });
  });

  it('multi-rôles : ?next= survit au sélecteur de rôle', async () => {
    apiPost.mockResolvedValueOnce({
      data: { ...SINGLE_ROLE_USER, roles: ['MEMBRE', 'FAISEUR'], activeRole: 'MEMBRE' },
    });
    const user = userEvent.setup();
    renderPage('/login?next=%2Fj%2Fbethel-cocody');
    await login(user);

    await waitFor(() => expect(screen.getByText(/Bienvenue, Marie/i)).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /espace membre/i }));

    await waitFor(() => {
      expect(screen.getByText('Page de rejointure (stub)')).toBeInTheDocument();
    });
  });

  it.each([
    ['//evil.example/steal', 'protocol-relative //'],
    ['/\\evil.example/steal', 'protocol-relative /\\'],
    ['https://evil.example', 'absolu https'],
    ['/https://evil.example', 'schéma remonté en chemin'],
  ])('?next=%s est refusé (%s) : aucune open-redirect', async (evil) => {
    const user = userEvent.setup();
    renderPage(`/login?next=${encodeURIComponent(evil)}`);
    await login(user);

    // Retombée sur le dashboard, jamais sur la cible externe.
    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });

  it('église non listée : on ne promet rien et on renvoie vers les preuves', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') return { data: { total: 0, items: [] } };
      return { data: {} };
    });
    const user = userEvent.setup();
    renderPage();
    await user.type(screen.getByRole('combobox'), 'Eglise Inexistante');
    await waitFor(() => expect(apiGet).toHaveBeenCalledWith(
      '/public/churches/suggest',
      expect.objectContaining({ params: { q: 'Eglise Inexistante' } }),
    ));

    expect(await screen.findByText(/n'existe pas ou n'est pas publiée/i)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /Ouvrir la page de rejointure/i })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /code d'entrée/i })).toHaveAttribute('href', '/join');
    expect(screen.getByRole('link', { name: /lien d'invitation/i })).toHaveAttribute('href', '/accept-invitation');
    expect(screen.getByRole('link', { name: /Créer mon église/i })).toHaveAttribute('href', '/register?mode=church');
  });

  it('le picker ne bloque pas la connexion : quota épuisé → on continue le formulaire', async () => {
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/public/meta') return { data: { betaMode: true, demoAccountsEnabled: false } };
      if (url === '/public/churches/suggest') throw new Error('rate limited');
      return { data: {} };
    });
    apiGetErrorMessage.mockReturnValue('Trop de requêtes');
    const user = userEvent.setup();
    renderPage();
    await user.type(screen.getByRole('combobox'), 'Bethel');
    expect(await screen.findByText('Trop de requêtes')).toBeInTheDocument();

    await login(user);
    await waitFor(() => {
      expect(screen.getByText('Tableau de bord (stub)')).toBeInTheDocument();
    });
  });
});
