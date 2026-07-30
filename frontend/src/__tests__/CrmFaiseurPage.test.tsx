import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CrmFaiseurPage from '@/pages/CrmFaiseurPage';

// Mock useAuth
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: {
      id: 'faiseur-1',
      firstName: 'Fabrice',
      lastName: 'Faiseur',
      role: 'FAISEUR',
      roles: ['FAISEUR'],
      activeRole: 'FAISEUR',
    },
    isAuthenticated: true,
    isLoading: false,
  }),
}));

// Mock recharts
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: () => null,
  Cell: () => null,
  Tooltip: () => null,
  Legend: () => null,
}));

const mockCrm = {
  statistiques: {
    totalDisciples: 5,
    actifs: 3,
    enIntegration: 1,
    enVeille: 1,
    decroches: 0,
    rapportsSoumisSemaine: 2,
    enDifficulte: 1,
    semaine: '2024-07-29',
  },
  disciples: [
    {
      id: 'soul-1',
      nom: 'Pierre Dupont',
      statut: 'ACTIF',
      etatSpirituel: 'CROISSANCE',
      niveauCroissance: 4,
      rapportSoumis: true,
      dateDernierContact: '2024-07-28T10:00:00Z',
      telephone: '0612345678',
      nbNotes: 2,
    },
    {
      id: 'soul-2',
      nom: 'Marie Martin',
      statut: 'EN_INTEGRATION',
      etatSpirituel: 'NOUVEAU_CONVERTI',
      niveauCroissance: 1,
      rapportSoumis: false,
      dateDernierContact: '2024-07-25T14:00:00Z',
      telephone: '0698765432',
      nbNotes: 0,
      difficultes: 'Difficultés à venir aux cultes',
    },
    {
      id: 'soul-3',
      nom: 'Jean Petit',
      statut: 'EN_VEILLE',
      etatSpirituel: 'EN_DIFFICULTE',
      niveauCroissance: 2,
      rapportSoumis: false,
      nbNotes: 1,
    },
    {
      id: 'soul-4',
      nom: 'Luc Bernard',
      statut: 'ACTIF',
      etatSpirituel: 'MATURE',
      niveauCroissance: 5,
      rapportSoumis: true,
      nbNotes: 0,
    },
    {
      id: 'soul-5',
      nom: 'Sophie Legrand',
      statut: 'ACTIF',
      etatSpirituel: 'CROISSANCE',
      niveauCroissance: 3,
      rapportSoumis: false,
      nbNotes: 3,
    },
  ],
  alertes: [
    { type: 'DIFFICULTE', soulId: 'soul-3', soulNom: 'Jean Petit', message: 'En difficulté spirituelle', priorite: 'HAUTE' },
    { type: 'ABSENCE_CONTACT', soulId: 'soul-2', soulNom: 'Marie Martin', message: 'Pas de contact depuis 14+ jours', priorite: 'MOYENNE' },
  ],
};

// Mock api
vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/dashboard/crm-faiseur')) return Promise.resolve({ data: mockCrm });
      return Promise.resolve({ data: {} });
    }),
    post: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  };
  return {
    default: mockApiInstance,
    getErrorMessage: vi.fn().mockReturnValue('Erreur'),
  };
});

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <CrmFaiseurPage />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('CrmFaiseurPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('shows loading skeleton during initial render', () => {
    renderPage();
    // Initially loading with skeletons
    const skeletons = document.querySelectorAll('.skeleton');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders the CRM title "Faiseur" after loading', async () => {
    renderPage();
    // "CRM Faiseur" is split across elements: "CRM " + <span>Faiseur</span>
    // Use a regex to match across element boundaries
    expect(await screen.findByText(/Faiseur/)).toBeInTheDocument();
  });

  it('displays greeting with user first name', async () => {
    renderPage();
    // Greeting text is "{getGreeting()}, {firstName}" in a span
    expect(await screen.findByText(/Fabrice/)).toBeInTheDocument();
  });

  it('renders stat cards with values', async () => {
    renderPage();
    // Labels that appear in stat cards. 'Actifs' also appears in filter buttons,
    // so use getAllByText for elements that may duplicate.
    expect(await screen.findByText('Disciples')).toBeInTheDocument();
    expect(screen.getAllByText('Actifs').length).toBeGreaterThanOrEqual(1);
    expect(await screen.findByText('Rapports soumis')).toBeInTheDocument();
    expect(await screen.findByText('En difficulté')).toBeInTheDocument();
  });

  it('renders the repartition chart heading', async () => {
    renderPage();
    expect(await screen.findByText('Répartition')).toBeInTheDocument();
  });

  it('renders alerts section with disciple names', async () => {
    renderPage();
    expect(await screen.findByText('Alertes')).toBeInTheDocument();
    // Jean Petit appears in both disciple list AND alerts
    expect(screen.getAllByText('Jean Petit').length).toBeGreaterThanOrEqual(1);
  });

  it('shows empty state when filter yields 0 results', async () => {
    renderPage();
    // Wait for data to load
    await screen.findByText('Disciples');

    // Click filter 'Décrochés' (0 disciples match this status)
    const decrocheBtn = screen.getByText('Décrochés');
    fireEvent.click(decrocheBtn);

    expect(screen.getByText('Aucun disciple trouvé')).toBeInTheDocument();
  });

  it('filters disciples when clicking a status filter button', async () => {
    renderPage();
    await screen.findByText('Disciples');

    // Click 'Intégration' filter button (use getByRole to avoid text encoding issues)
    const integrationBtn = screen.getByRole('button', { name: /Intégration/ });
    fireEvent.click(integrationBtn);

    // Marie Martin (EN_INTEGRATION) stays visible (both disciples list and alerts)
    expect(screen.getAllByText('Marie Martin').length).toBeGreaterThanOrEqual(1);

    // Pierre Dupont (ACTIF) removed from disciples list, not in alerts → 0
    expect(screen.queryAllByText('Pierre Dupont').length).toBe(0);

    // Sophie Legrand (ACTIF) also removed → 0
    expect(screen.queryAllByText('Sophie Legrand').length).toBe(0);
  });
});
