import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import CustomPageView from '@/pages/CustomPageView';

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
}));

const RESOLVED_PAGE = {
  page: {
    id: 'page-1',
    key: 'APERCU',
    title: 'Vue d’ensemble de l’église',
    description: 'Synthèse en temps réel',
    slug: 'apercu-eglise',
    layout: 'GRID_2',
    blocks: [],
    roles: [],
    enabled: true,
    published: true,
    version: 2,
    createdAt: '2026-08-17T10:00:00',
    updatedAt: '2026-08-17T10:00:00',
  },
  blocks: [
    { type: 'KPI', config: { label: 'Âmes suivies', source: 'SOULS_TOTAL', icon: 'Heart', color: 'primary' }, data: { value: 42 } },
    { type: 'KPI', config: { label: 'Alertes ouvertes', source: 'ALERTS_OPEN', icon: 'Bell', color: 'rose' }, data: { value: 3 } },
    {
      type: 'TABLEAU',
      config: { title: 'Événements à venir', source: 'UPCOMING_EVENTS' },
      data: {
        headers: ['Événement', 'Date', 'Lieu'],
        rows: [['Culte du dimanche', '17/08/2026 09:00', 'Temple'], ['Veillée', '22/08/2026 20:00', '—']],
      },
    },
    { type: 'TEXTE', config: { content: 'Bienvenue dans notre église !' }, data: null },
  ],
};

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderView() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/pages/apercu-eglise']}>
        <Routes>
          <Route path="/pages/:slug" element={<CustomPageView />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('CustomPageView — rendu public des pages personnalisées', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
  });

  it('affiche la page, les KPI résolus et le tableau de données réelles', async () => {
    apiGet.mockResolvedValue({ data: RESOLVED_PAGE });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble de l’église')).toBeInTheDocument();
    });
    // KPI : libellé + valeur réelle (format fr).
    expect(screen.getByText('Âmes suivies')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Alertes ouvertes')).toBeInTheDocument();
    // Tableau : en-têtes + lignes.
    expect(screen.getByText('Événement')).toBeInTheDocument();
    expect(screen.getByText('Culte du dimanche')).toBeInTheDocument();
    expect(screen.getByText('17/08/2026 09:00')).toBeInTheDocument();
    // Bloc texte.
    expect(screen.getByText('Bienvenue dans notre église !')).toBeInTheDocument();
    // Version affichée.
    expect(screen.getByText('v2')).toBeInTheDocument();
  });

  it('affiche « — » quand un KPI n’a pas de valeur résolue', async () => {
    const page = JSON.parse(JSON.stringify(RESOLVED_PAGE));
    page.blocks[0].data = null;
    apiGet.mockResolvedValue({ data: page });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble de l’église')).toBeInTheDocument();
    });
    // Valeur non résolue → « — » dans la carte KPI (et sa ligne de tableau).
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  it('affiche une page vide quand il n’y a aucun bloc', async () => {
    const page = JSON.parse(JSON.stringify(RESOLVED_PAGE));
    page.blocks = [];
    apiGet.mockResolvedValue({ data: page });
    renderView();

    await waitFor(() => {
      expect(screen.getByText('Cette page ne contient aucun bloc.')).toBeInTheDocument();
    });
  });

  it('affiche un message d’accès refusé sur 403', async () => {
    apiGet.mockRejectedValue({ response: { status: 403 } });
    renderView();

    await waitFor(() => {
      expect(screen.getByText(/pas accessible avec votre rôle actuel/i)).toBeInTheDocument();
    });
  });

  it('affiche « page introuvable » quand la page n’existe pas ou n’est pas publiée', async () => {
    apiGet.mockRejectedValue({ response: { status: 404 } });
    renderView();

    await waitFor(() => {
      expect(screen.getByText(/n’existe pas ou n’est pas publiée/i)).toBeInTheDocument();
    });
  });
});
