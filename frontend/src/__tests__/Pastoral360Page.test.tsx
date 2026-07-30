import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Pastoral360Page from '@/pages/Pastoral360Page';

// Mock useParams
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'soul-123' }),
  };
});

// Mock recharts
vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  RadarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Radar: () => null,
  PolarGrid: () => null,
  PolarAngleAxis: () => null,
  PolarRadiusAxis: () => null,
  Tooltip: () => null,
}));

const mockDossier = {
  informations: {
    id: 'soul-123',
    nom: 'Dupont',
    prenom: 'Marie',
    email: 'marie@email.com',
    telephone: '0123456789',
    adresse: '10 Rue de la Paix, Paris',
    dateNaissance: '1990-01-15',
    profession: 'Ingénieure',
    situationFamiliale: 'Marié(e)',
  },
  spirituel: {
    typeDisciple: 'NOUVEAU_CONVERTI',
    statut: 'ACTIF',
    etatSpirituel: 'CROISSANCE',
    niveauCroissance: 3,
    dateIntegration: '2024-01-10',
    dateConversion: '2024-01-01',
    dateDernierContact: '2024-07-28',
  },
  indices: {
    santeSpirituelle: 85,
    fidelite: 70,
    engagement: 60,
    participation: 75,
    global: 72,
  },
  alertesAutomatiques: [
    { type: 'DIFFICULTE', message: 'Membre en difficulté', priorite: 'HAUTE' },
  ],
  encadrement: {
    faiseurId: 'faiseur-1',
    faiseurNom: 'Jean Faiseur',
    familleId: 'famille-1',
  },
  evaluations: {
    FAISEUR: { moyenne: 4.2, total: 5 },
  },
  timeline: [
    {
      id: 'evt-1',
      type: 'CREATION',
      description: 'Âme créée',
      date: '2024-01-10T10:00:00Z',
    },
    {
      id: 'evt-2',
      type: 'CHANGEMENT_STATUT',
      description: 'De EN_INTEGRATION Ã  ACTIF',
      ancienStatut: 'EN_INTEGRATION',
      nouveauStatut: 'ACTIF',
      date: '2024-03-15T08:30:00Z',
    },
  ],
  notes: [
    { id: 'note-1', contenu: 'Excellent suivi', auteurId: 'user-1', date: '2024-07-20T14:00:00Z' },
  ],
};

// Mock api
vi.mock('@/lib/api', () => {
  const mockApiInstance = {
    get: vi.fn().mockImplementation((url: string) => {
      if (url.includes('/pastoral-360')) return Promise.resolve({ data: mockDossier });
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
        <Pastoral360Page />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

describe('Pastoral360Page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    queryClient.clear();
  });

  it('shows loading skeleton during initial render', () => {
    renderPage();
    // Loading state shows skeleton elements (no text content yet)
    const skeletons = document.querySelectorAll('.skeleton');
    expect(skeletons.length).toBeGreaterThan(0);
  });

  it('renders the page container', () => {
    renderPage();
    expect(document.querySelector('.page-container')).toBeInTheDocument();
  });

  it('renders the dossier title with member name after loading', async () => {
    renderPage();
    expect(await screen.findByText('Marie Dupont')).toBeInTheDocument();
  });

  it('renders the 360° badge after loading', async () => {
    renderPage();
    expect(await screen.findByText('Dossier Pastoral 360°')).toBeInTheDocument();
  });

  it('renders indices section labels after loading', async () => {
    renderPage();
    expect(await screen.findByText('Indices de santé')).toBeInTheDocument();
    expect(await screen.findByText('Santé spirituelle')).toBeInTheDocument();
  });

  it('renders auto-alerts after loading', async () => {
    renderPage();
    expect(await screen.findByText('Membre en difficulté')).toBeInTheDocument();
    expect(await screen.findByText('HAUTE')).toBeInTheDocument();
  });

  it('renders contact info after loading', async () => {
    renderPage();
    expect(await screen.findByText('marie@email.com')).toBeInTheDocument();
    expect(await screen.findByText('0123456789')).toBeInTheDocument();
    expect(await screen.findByText('10 Rue de la Paix, Paris')).toBeInTheDocument();
  });

  it('renders spiritual journey info after loading', async () => {
    renderPage();
    expect(await screen.findByText('CROISSANCE')).toBeInTheDocument();
    expect(await screen.findByText('Actif')).toBeInTheDocument();
  });

  it('renders encadrement section after loading', async () => {
    renderPage();
    expect(await screen.findByText('Jean Faiseur')).toBeInTheDocument();
  });

  it('renders timeline events after loading', async () => {
    renderPage();
    expect(await screen.findByText('CREATION')).toBeInTheDocument();
    expect(await screen.findByText('Âme créée')).toBeInTheDocument();
  });

  it('renders private notes after loading', async () => {
    renderPage();
    expect(await screen.findByText('Excellent suivi')).toBeInTheDocument();
  });
});
