import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from '@/App';
import { useAuth } from '@/contexts/AuthContext';
import { ImpersonationProvider } from '@/contexts/ImpersonationContext';

/* ============================================================================
 * T-W2 / F2 — ISOLATION DE LA CONSOLE PLATEFORME.
 *
 * Régression ciblée : un Super Admin qui saisit `/souls`, `/dashboard` ou
 * `/crm/faiseur` en dur ne doit voir AUCUN dashboard d'église. Avant la garde,
 * ces routes rendaient le dashboard du chef de famille à un compte qui n'a
 * aucune église — le reproche exact du produit.
 *
 * La garde vit dans `ProtectedRoute` (App.tsx) :
 *   if (user?.platformSuperAdmin === true && scope !== 'platform')
 *     return <Navigate to="/platform/dashboard" replace />;
 *
 * Ce test verrouille le COMPORTEMENT, pas l'implémentation : si la garde est
 * déplacée ou inversée, il échoue.
 * ========================================================================== */

vi.mock('@/contexts/AuthContext', async () => {
  const actual = await vi.importActual('@/contexts/AuthContext');
  return { ...actual, useAuth: vi.fn() };
});

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

vi.mock('@/pages/MapPage', () => ({ default: () => null }));

// `vi.mock` est HOISTÉ en haut du fichier : le factory ne peut pas referencer
// une variable de niveau module (elle ne serait pas encore initialisée au
// moment de l'évaluation). On construit donc l'API factice à l'intérieur.
vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn().mockImplementation(() => Promise.resolve({ data: {} })),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="route-probe">{location.pathname}</div>;
}

function renderApp(initialPath: string) {
  (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
    isAuthenticated: true,
    isLoading: false,
    user: {
      id: 'p1',
      email: 'root@discipolat.com',
      role: 'ADMIN',
      roles: ['ADMIN'],
      activeRole: 'ADMIN',
      platformSuperAdmin: true,          // ← la condition de la garde
      statut: 'ACTIVE',
    },
    activeRole: 'ADMIN',
    roles: ['ADMIN'],
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
        {/* Même arbre de providers qu'en production (main.tsx) : App contient
            <ImpersonationBanner> qui exige ImpersonationProvider. */}
        <ImpersonationProvider>
          <LocationProbe />
          <App />
        </ImpersonationProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('Console plateforme — un Super Admin n\'a AUCUNE interface d\'église (F2)', () => {
  beforeEach(() => vi.clearAllMocks());

  it.each([
    ['/souls'],
    ['/crm/faiseur'],
    ['/dashboard/chef-famille'],
    ['/dashboard/responsable'],
  ])('redirige %s vers /platform/dashboard', async (path) => {
    renderApp(path);
    await waitFor(
      () => expect(screen.getByTestId('route-probe').textContent).toBe('/platform/dashboard'),
      { timeout: 10000 },
    );
  });

  it('laisse la console plateforme elle-même accessible (pas de boucle)', async () => {
    renderApp('/platform/dashboard');
    // La route porte scope="platform" : la garde ne doit PAS s'appliquer.
    await waitFor(
      () => expect(screen.getByTestId('route-probe').textContent).toBe('/platform/dashboard'),
      { timeout: 10000 },
    );
  });
});