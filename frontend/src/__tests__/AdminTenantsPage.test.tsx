import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, fireEvent, cleanup, within } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError } from 'axios';
import AdminTenantsPage from '@/pages/AdminTenantsPage';

const { apiGet, apiPost, apiPut, apiDelete, getErrorMessage, toastSuccess, toastError } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPost: vi.fn(),
  apiPut: vi.fn(),
  apiDelete: vi.fn(),
  getErrorMessage: vi.fn(() => 'Erreur réseau'),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
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
  getErrorMessage,
}));

vi.mock('react-hot-toast', () => ({
  default: { success: toastSuccess, error: toastError },
}));

// Réponse réelle de `GET /platform/admin/plans` : un tableau JSON brut de
// `toPlanSummary(...)` (SuperAdminController:463), clés `key`/`name`.
const PLANS = [
  { key: 'DISCOVERY', name: 'Découverte', priceMonthly: 0 },
  { key: 'GROWTH', name: 'Croissance', priceMonthly: 49000 },
];

const GRACE = '11111111-1111-1111-1111-111111111111';
const PORT = '22222222-2222-2222-2222-222222222222';

const TENANTS = [
  {
    id: GRACE,
    name: 'Eglise de la Grace',
    slug: 'grace',
    status: 'ACTIVE',
    plan: 'GROWTH',
    createdAt: '2026-01-05T10:00:00Z',
    updatedAt: '2026-01-05T10:00:00Z',
    onboardingCompletedAt: '2026-02-11T08:30:00Z',
  },
  {
    id: PORT,
    name: 'Eglise du Port',
    slug: 'port',
    status: 'SUSPENDED',
    plan: 'DISCOVERY',
    createdAt: '2026-01-06T10:00:00Z',
    updatedAt: '2026-01-06T10:00:00Z',
  },
];

/** Routeur de réponses par URL ; les `overrides` priment, tout le reste suit le jeu nominal. */
const route = (overrides: Record<string, unknown> = {}) => {
  apiGet.mockImplementation((url: string) => {
    if (url in overrides) {
      const value = overrides[url];
      if (value instanceof Error) return Promise.reject(value);
      return Promise.resolve({ data: value });
    }
    if (url === '/tenants') return Promise.resolve({ data: TENANTS });
    if (url === '/platform/admin/plans') return Promise.resolve({ data: PLANS });
    if (url === '/platform/admin/quota-usage/tenants') {
      return Promise.resolve({ data: { content: [{ tenantId: GRACE, users: 12, aiCredits: 3 }] } });
    }
    return Promise.reject(new Error(`Chemin non mocke : ${url}`));
  });
};

const axiosError = (status: number) => new AxiosError(
  'Request failed',
  'ERR_BAD_REQUEST',
  undefined,
  undefined,
  { status, data: {}, headers: {}, config: {} } as never,
);

const renderPage = () => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <AdminTenantsPage />
    </QueryClientProvider>,
  );
};

// REVENIR
describe('AdminTenantsPage (B5 : alignement sur la realite du backend)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getErrorMessage.mockReturnValue('Erreur reseau');
    apiPost.mockResolvedValue({ data: {} });
    apiPut.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
    vi.spyOn(window, 'confirm').mockImplementation(() => true);
    route();
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('charge les plans depuis le backend, sans cle codee en dur', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Eglise de la Grace')).toBeInTheDocument());

    expect(apiGet).toHaveBeenCalledWith('/platform/admin/plans');
    // Le libelle vient du backend (« Croissance »), pas d'une liste locale.
    expect(screen.getAllByText('Croissance').length).toBeGreaterThan(0);
    // L'ancien menu local (free/starter/pro/enterprise) a disparu du rendu.
    expect(screen.queryByText('Enterprise')).not.toBeInTheDocument();
    expect(screen.queryByText('Free')).not.toBeInTheDocument();
  });

  it('avertit quand le catalogue des plans est indisponible au lieu de mentir', async () => {
    route({ '/platform/admin/plans': axiosError(503) });
    renderPage();

    // Le bandeau n'apparait qu'une fois la requete des plans resolue : il faut
    // l'attendre (le squelette de chargement porte aussi role="status").
    await waitFor(() => {
      expect(screen.getAllByRole('status').some((el) => /provisoire/.test(el.textContent || ''))).toBe(true);
    });
  });

  it('propose « Réactiver » seulement sur une eglise suspendue, et jamais « Supprimer »', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Eglise du Port')).toBeInTheDocument());

    expect(screen.queryByTitle(/Supprimer/)).not.toBeInTheDocument();
    expect(screen.queryByTitle(/Delete/i)).not.toBeInTheDocument();

    const reactivate = screen.getAllByRole('button', { name: /Réactiver/ });
    expect(reactivate.length).toBe(1);

    fireEvent.click(reactivate[0]);

    await waitFor(() => expect(apiPost).toHaveBeenCalledWith(`/tenants/${PORT}/reactivate`));
    expect(toastSuccess).toHaveBeenCalledWith('Église réactivée');
  });

  it('suspend une eglise active avec des libelles honnetes', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Eglise de la Grace')).toBeInTheDocument());

    const suspend = screen.getAllByRole('button', { name: /Suspendre/ });
    expect(suspend.length).toBe(1);
    fireEvent.click(suspend[0]);

    await waitFor(() => expect(apiDelete).toHaveBeenCalledWith(`/tenants/${GRACE}`));
    expect(toastSuccess).toHaveBeenCalledWith('Église suspendue');
    expect(toastSuccess).not.toHaveBeenCalledWith('Église supprimée');
  });

  it('affiche le badge d\u2019onboarding termine ou en configuration', async () => {
    renderPage();
    await waitFor(() => expect(screen.getByText('Eglise de la Grace')).toBeInTheDocument());

    expect(screen.getAllByText(/Onboarding terminé le/).length).toBeGreaterThan(0);
    expect(screen.getAllByText('Onboarding en configuration').length).toBeGreaterThan(0);
  });

  it('reste tolerant a un statut inconnu : valeur brute affichee, aucun crash', async () => {
    route({ '/tenants': [{ ...TENANTS[0], status: 'ARCHIVED_LEGACY' }] });
    renderPage();

    await waitFor(() => expect(screen.getByText('Eglise de la Grace')).toBeInTheDocument());
    // L'assertion doit etre portee sur la LIGNE du tenant : les cartes de
    // repartition par statut (statsByStatus) affichent volontairement les 4
    // statuts connus, y compris « Active » a 0 — ce n'est pas un mensonge,
    // c'est un compteur de la plateforme.
    const row = screen.getByText('Eglise de la Grace').closest('.glass-card');
    expect(row).not.toBeNull();
    expect(within(row as HTMLElement).getByText('ARCHIVED_LEGACY')).toBeInTheDocument();
    expect(within(row as HTMLElement).queryByText('Active')).not.toBeInTheDocument();
  });

  it('affiche « — » quand l\u2019usage est indisponible, jamais 0', async () => {
    route({ '/platform/admin/quota-usage/tenants': axiosError(500) });
    renderPage();

    await waitFor(() => expect(screen.getByText('Eglise de la Grace')).toBeInTheDocument());
    // L'echec de l'appel d'usage ne casse pas le tableau…
    expect(screen.getByText('Eglise du Port')).toBeInTheDocument();
    // …et l'usage affiche « — » au lieu de 0.
    expect(screen.getAllByText(/Utilisateurs\s*:\s*—/).length).toBe(2);
  });

  it('affiche l\u2019usage reel quand l\u2019appel agrege reussit', async () => {
    renderPage();
    await waitFor(() => expect(screen.getAllByText(/Utilisateurs\s*:\s*12/).length).toBeGreaterThan(0));
    expect(apiGet).toHaveBeenCalledWith('/platform/admin/quota-usage/tenants', expect.anything());
  });

  it('distingue un 403 (session/role revoke) d\u2019une panne generique', async () => {
    route({ '/tenants': axiosError(403) });
    renderPage();

    await waitFor(() => expect(screen.getByText('Impossible de charger les églises')).toBeInTheDocument());
    expect(screen.getByText(/super-admin a été révoqué/)).toBeInTheDocument();
  });

  it('propose un retry actionnable quand la liste echoue', async () => {
    route({ '/tenants': axiosError(500) });
    renderPage();

    await waitFor(() => expect(screen.getByText('Impossible de charger les églises')).toBeInTheDocument());
    const retry = screen.getByRole('button', { name: 'Réessayer' });

    apiGet.mockImplementation((url: string) => (url === '/tenants'
      ? Promise.resolve({ data: TENANTS })
      : Promise.resolve({ data: [] })));
    fireEvent.click(retry);

    await waitFor(() => expect(screen.getByText('Eglise de la Grace')).toBeInTheDocument());
  });
});

