import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth } from '@/contexts/AuthContext';
import Sidebar from '@/components/layout/Sidebar';

// Mock useAuth to control role for each test
vi.mock('@/contexts/AuthContext', async () => {
  const actual = await vi.importActual('@/contexts/AuthContext');
  return {
    ...actual,
    useAuth: vi.fn(),
  };
});

// Mock recharts
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  AreaChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Area: () => null,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
}));

// Mock api to prevent actual calls for evaluation score
vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: { statistiques: {} } }),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderSidebar(open = false) {
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Sidebar open={open} onClose={vi.fn()} />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

// Helper: since Sidebar renders navigation items in both desktop (hidden lg:...)
// and mobile (lg:hidden) asides, getByText finds duplicates.
// Use these helpers to handle multi-element results.
function expectTextPresent(text: string) {
  expect(screen.getAllByText(text).length).toBeGreaterThanOrEqual(1);
}

function expectTextAbsent(text: string) {
  expect(screen.queryAllByText(text).length).toBe(0);
}

describe('Sidebar - Multi-Role Navigation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nav items accessible by PASTEUR role', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
      user: {
        id: '1',
        email: 'pasteur@test.com',
        role: 'PASTEUR',
        roles: ['PASTEUR'],
        activeRole: 'PASTEUR',
        firstName: 'Pierre',
        lastName: 'Pasteur',
        estChefDeFamille: false,
      },
      isLoading: false,
    });

    renderSidebar();

    expectTextPresent('Pilotage Pasteur');
    expectTextPresent('Tableau de bord');
    expectTextPresent('Âmes');
  });

  it('renders nav items accessible by FAISEUR role', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
      user: {
        id: '2',
        email: 'faiseur@test.com',
        role: 'FAISEUR',
        roles: ['FAISEUR'],
        activeRole: 'FAISEUR',
        firstName: 'Fabrice',
        lastName: 'Faiseur',
        estChefDeFamille: false,
      },
      isLoading: false,
    });

    renderSidebar();

    expectTextPresent('CRM Faiseur');

    // Faiseur should NOT see admin/pasteur-specific items
    expectTextAbsent('Pilotage Pasteur');
    expectTextAbsent('Permissions');
    expectTextAbsent('Départements');
  });

  it('renders nav items accessible by CHEF_DE_FAMILLE role', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
      user: {
        id: '3',
        email: 'chef@test.com',
        role: 'FAISEUR',
        roles: ['FAISEUR', 'CHEF_DE_FAMILLE'],
        activeRole: 'CHEF_DE_FAMILLE',
        firstName: 'Jean',
        lastName: 'Chef',
        estChefDeFamille: true,
      },
      isLoading: false,
    });

    renderSidebar();

    expectTextPresent('Ma famille');

    // Chef should NOT see pasteur-only items
    expectTextAbsent('Pilotage Pasteur');
    expectTextAbsent('Permissions');
  });

  it('renders nav items accessible by RESPONSABLE role', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
      user: {
        id: '4',
        email: 'resp@test.com',
        role: 'RESPONSABLE',
        roles: ['RESPONSABLE'],
        activeRole: 'RESPONSABLE',
        firstName: 'Rachel',
        lastName: 'Resp',
        estChefDeFamille: false,
      },
      isLoading: false,
    });

    renderSidebar();

    expectTextPresent('Mon département');
    expectTextPresent('Départements');

    expectTextAbsent('Pilotage Pasteur');
    expectTextAbsent('Permissions');
  });

  it('switches navigation when activeRole changes from PASTEUR to FAISEUR', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
      user: {
        id: '5',
        email: 'paul@test.com',
        role: 'PASTEUR',
        roles: ['PASTEUR', 'FAISEUR'],
        activeRole: 'FAISEUR',
        firstName: 'Paul',
        lastName: 'Apôtre',
        estChefDeFamille: false,
      },
      isLoading: false,
    });

    renderSidebar();

    expectTextPresent('CRM Faiseur');
    expectTextAbsent('Pilotage Pasteur');
    expectTextAbsent('Permissions');
  });

  it('displays user initials in the footer', () => {
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
      user: {
        id: '1',
        email: 'pasteur@test.com',
        role: 'PASTEUR',
        roles: ['PASTEUR'],
        activeRole: 'PASTEUR',
        firstName: 'Pierre',
        lastName: 'Pasteur',
        estChefDeFamille: false,
      },
      isLoading: false,
    });

    renderSidebar();

    expectTextPresent('PP');
  });

  it('MEMBRE nav keys are unique (anti-regression QA-001: duplicate href keys)', async () => {
    // La section MEMBRE contenait 2 items pointant vers le même href
    // ('Mes présences' + 'Ma progression' → '/dashboard/membre'), ce qui
    // produisait "Encountered two children with the same key" dans les tests
    // RoleWorkspaceRouting (MEMBRE). Chaque item doit avoir une clé unique.
    const { navForRole } = await import('@/workspaces');
    for (const role of ['MEMBRE', 'PASTEUR', 'ADMIN', 'FAISEUR', 'CHEF_DE_FAMILLE', 'RESPONSABLE']) {
      for (const section of navForRole({ role })) {
        const keys = section.items.map((i) => `${section.title}::${i.href}`);
        expect(new Set(keys).size, `duplicate nav key in ${role}/${section.title}`).toBe(keys.length);
      }
    }
    // MEMBRE : les entrées spirituelles sont ancrées, pas dupliquées.
    await import('@/workspaces'); // ensure side-effects registered
    const membre = navForRole({ role: 'MEMBRE' }).flatMap((s) => s.items.map((i) => i.href));
    expect(membre.filter((h) => h === '/dashboard/membre').length).toBeLessThanOrEqual(1);
  });

  it('SUPER ADMIN plateforme : espace isolé, jamais les menus d\'église (FE-1)', () => {
    // Reproduit le pain point n°2 : un compte portant le flag backend
    // platformSuperAdmin ne doit JAMAIS retomber sur la nav d'église, même
    // si son rôle tenant actif est ADMIN (qui ouvre d'ordinaire FULL_NAV).
    (useAuth as ReturnType<typeof vi.fn>).mockReturnValue({
      isAuthenticated: true,
      user: {
        id: '9',
        email: 'root@discipolat.com',
        role: 'ADMIN',
        roles: ['ADMIN'],
        activeRole: 'ADMIN',
        platformSuperAdmin: true,
        firstName: 'Su',
        lastName: 'Per',
        estChefDeFamille: false,
      },
      isLoading: false,
    });

    renderSidebar();

    // Menus plateforme présents.
    expectTextPresent('Gouvernance');
    expectTextPresent('Annonces publiques');
    // Menus d'église absents (fuite corrigée par la bascule platformAdmin).
    expectTextAbsent('Âmes');
    expectTextAbsent('Départements');
    expectTextAbsent('Permissions');
  });

  it('T-W1 (F12) : la garde est STRUCTURELLE — même via la config backend de menus', async () => {
    // La correction initiale court-circuitait dans Sidebar avec
    // `platformAdmin ? PLATFORM_NAV : ...`. Ce cas prouve que navForRole
    // porte la garde lui-même : un appel direct, sans passer par un
    // composant, ne peut plus rendre FULL_NAV à un platform admin.
    const { navForRole } = await import('@/workspaces');
    const hrefs = navForRole({ platformSuperAdmin: true, role: 'ADMIN', activeRole: 'ADMIN' })
      .flatMap((s) => s.items.map((i) => i.href));
    expect(hrefs).not.toContain('/souls');
    expect(hrefs).not.toContain('/departments');
    expect(hrefs).toContain('/platform/dashboard');
  });
});
