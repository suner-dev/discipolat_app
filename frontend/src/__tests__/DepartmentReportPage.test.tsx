import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import DepartmentReportPage from '@/pages/DepartmentReportPage';

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'dept-1' }),
  };
});

const mockReport = {
  departementId: 'dept-1',
  semaine: '2026-08-03',
  totalFamilles: 2,
  familyReportsSoumis: 1,
  totalPresents: 5,
  totalAbsents: 1,
  totalSorties: 0,
  totalMaintenus: 1,
  presenceMoyenne: 83.3,
  statsParFamille: {
    'fam-1': {
      familleNom: 'Famille Alpha',
      familleId: 'fam-1',
      soumis: true,
      presenceMoyenne: 90,
      totalPresents: 4,
      totalAbsents: 0,
      totalSorties: 0,
      totalMaintenus: 0,
      piecesJointes: [
        { id: 'att-1', fileId: 'f1', nom: 'Synthèse.pdf', url: 'https://drive/1.pdf' },
      ],
    },
    'fam-2': {
      familleNom: 'Famille Beta',
      familleId: 'fam-2',
      soumis: false,
      piecesJointes: [],
    },
  },
};

const mockKpi = {
  tauxSoumission: 50,
  tauxPresence: 80,
  rapportsSoumisSemaine: 1,
  rapportsAttendusSemaine: 2,
  totalFaiseurs: 3,
};

vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/report')) return Promise.resolve({ data: mockReport });
      if (url.includes('/kpi')) return Promise.resolve({ data: mockKpi });
      return Promise.resolve({ data: { id: 'dept-1', nom: 'Département A' } });
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
        <DepartmentReportPage />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('DepartmentReportPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('renders the report title after loading', async () => {
    renderPage();
    expect(await screen.findByText('Rapport du département')).toBeInTheDocument();
    expect(await screen.findByText('Département A')).toBeInTheDocument();
  });

  it('renders the per-family table with the Pièces column', async () => {
    renderPage();
    expect(await screen.findByText('Famille Alpha')).toBeInTheDocument();
    expect(screen.getByText('Pièces')).toBeInTheDocument();
  });

  it('shows clickable attachment links for families with pieces', async () => {
    renderPage();
    const link = await screen.findByTitle('Synthèse.pdf');
    expect(link).toHaveAttribute('href', 'https://drive/1.pdf');
    expect(link).toHaveAttribute('target', '_blank');
  });

  it('does not render a link cell for families without pieces', async () => {
    renderPage();
    await screen.findByText('Famille Beta');
    // Aucun lien pour la famille sans pièces jointes : le seul lien est celui de Famille Alpha
    const links = screen.queryAllByTitle(/\.pdf/);
    expect(links).toHaveLength(1);
  });
});
