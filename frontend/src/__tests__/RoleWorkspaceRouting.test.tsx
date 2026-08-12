import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from '@/App';
import { useAuth } from '@/contexts/AuthContext';

/* ============================================================================
 * Tests de transition de rôle (espaces métiers)
 * 1. DashboardGate redirige chaque rôle vers SON espace métier.
 * 2. Les routes des espaces sont isolées : un rôle ne peut pas ouvrir le
 *    dashboard d'un autre métier (même avec plusieurs rôles possédés).
 * ========================================================================== */

// --- Mocks des dépendances lourdes / réseau ---

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  AreaChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Area: () => null,
  LineChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Line: () => null,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: () => null,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: () => null,
  Cell: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

// Leaflet n'est pas compatible jsdom : on remplace la page cartographie.
vi.mock('@/pages/MapPage', () => ({ default: () => null }));

// API factice
vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/notifications')) return Promise.resolve({ data: { content: [] } });
      if (url.includes('/evaluations/me')) return Promise.resolve({ data: { statistiques: {} } });
      if (url.includes('/dashboard/kpi')) return Promise.resolve({
        data: {
          tauxPresenceGlobal: 75, tauxPresenceNouveauxArrivants: 60, tauxPresenceNouveauxConvertis: 85,
          totalAmes: 45, totalFaiseurs: 8, totalFamilles: 4, totalDepartements: 2,
          totalSorties: 3, totalMaintenus: 42, suivisParallelesActifs: 2, alertesActives: 1,
          rapportsSoumis: 5, rapportsEnAttente: 2, famillesARisque: 0, tendancePresence: 2.5,
        },
      });
      if (url.includes('/members/me/dashboard')) return Promise.resolve({
        data: {
          user: { id: 'u1', firstName: 'Test', lastName: 'User', email: 'test@test.com', phone: '', photoUrl: '', dateNaissance: '', situationFamiliale: '' },
          statutMembre: 'MEMBRE', estFaiseur: false, departements: [],
        },
      });
      if (url.includes('/souls') || url.includes('/alerts')) return Promise.resolve({ data: { content: [] } });
      if (url.includes('/members/me/presences')) return Promise.resolve({ data: [] });
      if (url.includes('/members/me/requests')) return Promise.resolve({ data: [] });
      if (url.includes('/programs/active')) return Promise.resolve({ data: [] });
      if (url.includes('/dashboard/crm-faiseur')) return Promise.resolve({ data: { disciples: [], statistiques: {}, alertes: [] } });
      if (url.includes('/dashboard/responsable')) return Promise.resolve({
        data: { statistiques: {}, departements: [], departement: {}, selectedDeptNom: '' },
      });
      if (url.includes('/dashboard/chef-famille')) return Promise.resolve({ data: { famille: {}, faiseurs: [], disciples: [], statistiques: {} } });
      return Promise.resolve({ data: {} });
    }),
    post: vi.fn(),
    put: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  };
  return { default: mockApiInstance, getErrorMessage: vi.fn().mockReturnValue('Erreur') };
});

// AuthContext mocké pour piloter le rôle actif de chaque test
vi.mock('@/contexts/AuthContext', async () => {
  const actual = await vi.importActual('@/contexts/AuthContext');
  return { ...actual, useAuth: vi.fn() };
});

// --- Helpers ---

function makeUser(activeRole: string, roles: string[] = [activeRole]) {
  return {
    id: 'u1',
    email: 'test@discipolat.com',
    firstName: 'Test',
    lastName: 'User',
    role: activeRole,
    roles,
    activeRole,
    estChefDeFamille: activeRole === 'CHEF_DE_FAMILLE',
    statut: 'ACTIVE',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };
}

/** Affiche la route courante du routeur (pour vérifier les redirections). */
function LocationProbe() {
  const location = useLocation();
  return <div data-testid="route-probe">{location.pathname}</div>;
}

function renderApp(initialPath: string, activeRole: string, roles?: string[]) {
  (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
    isAuthenticated: true,
    isLoading: false,
    user: makeUser(activeRole, roles),
    activeRole,
    roles: roles ?? [activeRole],
    hasRole: vi.fn(() => true),
    logout: vi.fn(),
    switchRole: vi.fn(),
    updateUser: vi.fn(),
  });

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <LocationProbe />
        <App />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const currentPath = () => screen.getByTestId('route-probe').textContent;

async function expectRedirectTo(initialPath: string, activeRole: string, expected: string, roles?: string[]) {
  renderApp(initialPath, activeRole, roles);
  await waitFor(() => expect(currentPath()).toBe(expected), { timeout: 3000 });
}

describe('DashboardGate — redirection vers l’espace métier du rôle actif', () => {
  beforeEach(() => vi.clearAllMocks());

  it.each([
    ['RESPONSABLE', '/dashboard/responsable'],
    ['FAISEUR', '/crm/faiseur'],
    ['CHEF_DE_FAMILLE', '/dashboard/chef-famille'],
    ['MEMBRE', '/dashboard/membre'],
  ])('redirige un %s depuis /dashboard vers son espace (%s)', async (role, home) => {
    await expectRedirectTo('/dashboard', role, home);
  });

  it('laisse PASTEUR sur le dashboard général', async () => {
    await expectRedirectTo('/dashboard', 'PASTEUR', '/dashboard');
    // Le titre du dashboard + l'item de navigation sont présents
    // (waitFor : le chunk de la page est chargé à la demande)
    await waitFor(() => {
      expect(screen.getAllByText('Tableau de bord').length).toBeGreaterThan(0);
    });
  });

  it('laisse ADMIN sur le dashboard général', async () => {
    await expectRedirectTo('/dashboard', 'ADMIN', '/dashboard');
    await waitFor(() => {
      expect(screen.getAllByText('Tableau de bord').length).toBeGreaterThan(0);
    });
  });

  it('rend l’espace métier ciblé (CRM Faiseur) après la redirection', async () => {
    renderApp('/dashboard', 'FAISEUR');
    await waitFor(() => expect(currentPath()).toBe('/crm/faiseur'), { timeout: 3000 });
    expect(screen.getByRole('heading', { name: /CRM/i })).toBeInTheDocument();
  });
});

describe('Isolation des routes — un espace métier n’ouvre pas celui d’un autre', () => {
  beforeEach(() => vi.clearAllMocks());

  it('bloque un FAISEUR sur /dashboard/chef-famille (redirigé vers /crm/faiseur)', async () => {
    await expectRedirectTo('/dashboard/chef-famille', 'FAISEUR', '/crm/faiseur');
  });

  it('bloque un FAISEUR multi-rôles (FAISEUR + CHEF_DE_FAMILLE) sur /dashboard/chef-famille : le rôle actif prime', async () => {
    await expectRedirectTo('/dashboard/chef-famille', 'FAISEUR', '/crm/faiseur', ['FAISEUR', 'CHEF_DE_FAMILLE']);
  });

  it('bloque un CHEF_DE_FAMILLE sur /crm/faiseur (redirigé vers /dashboard/chef-famille)', async () => {
    await expectRedirectTo('/crm/faiseur', 'CHEF_DE_FAMILLE', '/dashboard/chef-famille');
  });

  it('bloque un RESPONSABLE sur /crm/faiseur (redirigé vers /dashboard/responsable)', async () => {
    await expectRedirectTo('/crm/faiseur', 'RESPONSABLE', '/dashboard/responsable');
  });

  it('bloque un MEMBRE sur /souls (redirigé vers /dashboard/membre)', async () => {
    await expectRedirectTo('/souls', 'MEMBRE', '/dashboard/membre');
  });

  it('bloque un MEMBRE sur /dashboard/chef-famille (redirigé vers /dashboard/membre)', async () => {
    await expectRedirectTo('/dashboard/chef-famille', 'MEMBRE', '/dashboard/membre');
  });

  it('bloque un FAISEUR sur /departments (espace responsable) (redirigé vers /crm/faiseur)', async () => {
    await expectRedirectTo('/departments', 'FAISEUR', '/crm/faiseur');
  });

  it('bloque un CHEF_DE_FAMILLE sur /departments (redirigé vers /dashboard/chef-famille)', async () => {
    await expectRedirectTo('/departments', 'CHEF_DE_FAMILLE', '/dashboard/chef-famille');
  });

  it('permet à PASTEUR (super-utilisateur) d’ouvrir /crm/faiseur sans redirection', async () => {
    await expectRedirectTo('/crm/faiseur', 'PASTEUR', '/crm/faiseur');
  });

  it('permet à PASTEUR (super-utilisateur) d’ouvrir /dashboard/chef-famille sans redirection', async () => {
    await expectRedirectTo('/dashboard/chef-famille', 'PASTEUR', '/dashboard/chef-famille');
  });

  it('conserve l’accès de CHEF_DE_FAMILLE aux fiches de données /souls (pas de sur-redirection)', async () => {
    await expectRedirectTo('/souls', 'CHEF_DE_FAMILLE', '/souls');
  });

  it('conserve l’accès de FAISEUR aux fiches de données /souls (pas de sur-redirection)', async () => {
    await expectRedirectTo('/souls', 'FAISEUR', '/souls');
  });
});
