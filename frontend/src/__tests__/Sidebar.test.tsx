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
});
