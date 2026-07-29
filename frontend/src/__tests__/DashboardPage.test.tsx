import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/contexts/AuthContext';
import DashboardPage from '@/pages/DashboardPage';

// Mock recharts to avoid rendering issues in jsdom
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  AreaChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Area: () => null,
  LineChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Line: () => null,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: () => null,
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: () => null,
  Cell: () => null,
  Legend: () => null,
}));

// Mock axios to prevent actual API calls
vi.mock('@/lib/api', () => {
  const mockKpiData = {
    tauxPresenceGlobal: 75.5,
    tauxPresenceNouveauxArrivants: 60.0,
    tauxPresenceNouveauxConvertis: 85.0,
    totalAmes: 45,
    totalFaiseurs: 8,
    totalFamilles: 4,
    totalDepartements: 2,
    totalSorties: 3,
    totalMaintenus: 42,
    suivisParallelesActifs: 2,
    alertesActives: 1,
    rapportsSoumis: 5,
    rapportsEnAttente: 2,
    famillesARisque: 0,
    tendancePresence: 2.5,
  };
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/dashboard/kpi')) return Promise.resolve({ data: mockKpiData });
      if (url.includes('/alerts')) return Promise.resolve({ data: { content: [] } });
      return Promise.resolve({ data: { content: [] } });
    }),
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
    defaults: { headers: { common: {} } },
  };
  return {
    default: mockApiInstance,
    getErrorMessage: vi.fn().mockReturnValue('Erreur'),
  };
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: false },
    mutations: { retry: false },
  },
});

function renderWithProviders() {
  // Set up a mock user in localStorage
  const mockUser = {
    id: 'test-user-1',
    email: 'pasteur@discipolat.com',
    role: 'PASTEUR',
    firstName: 'Pierre',
    lastName: 'Apôtre',
  };
  localStorage.setItem('user', JSON.stringify(mockUser));
  localStorage.setItem('accessToken', 'mock-token');

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <DashboardPage />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('DashboardPage', () => {
  it('renders the dashboard title', () => {
    renderWithProviders();
    expect(screen.getByText('Tableau de bord')).toBeInTheDocument();
  });

  it('displays welcome message with user name', () => {
    renderWithProviders();
    expect(screen.getByText(/^(Bonjour|Bon après-midi|Bonsoir), Pierre$/)).toBeInTheDocument();
  });

  it('renders stat cards with labels', () => {
    renderWithProviders();
    expect(screen.getByText('Âmes suivies')).toBeInTheDocument();
    expect(screen.getByText('Faiseurs actifs')).toBeInTheDocument();
    expect(screen.getByText('Familles')).toBeInTheDocument();
    expect(screen.getByText('Départements')).toBeInTheDocument();
  });

  it('renders charts section headings', () => {
    renderWithProviders();
    expect(screen.getByText('Présence par type de disciple')).toBeInTheDocument();
    expect(screen.getByText('Tendance de présence')).toBeInTheDocument();
  });

  it('shows non-pasteur message for other roles', () => {
    // Clear and set a non-pasteur user
    localStorage.clear();
    const faiseurUser = {
      id: 'test-faiseur',
      email: 'faiseur@discipolat.com',
      role: 'FAISEUR',
      firstName: 'Luc',
      lastName: 'Faiseur',
    };
    localStorage.setItem('user', JSON.stringify(faiseurUser));
    localStorage.setItem('accessToken', 'mock-token');

    render(
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <AuthProvider>
            <DashboardPage />
          </AuthProvider>
        </BrowserRouter>
      </QueryClientProvider>
    );

    expect(screen.getByText(/indicateurs détaillés.*Pasteur/)).toBeInTheDocument();
  });

  it('has links to souls, families, and other sections', () => {
    renderWithProviders();
    const links = screen.getAllByRole('link');
    expect(links.length).toBeGreaterThan(0);
  });
});
