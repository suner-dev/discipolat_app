import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import toast from 'react-hot-toast';
import PlatformPagesPage from '@/pages/PlatformPagesPage';

vi.mock('react-hot-toast', () => ({
  default: { success: vi.fn(), error: vi.fn() },
}));

const { apiGet, apiPut, apiPost, apiDelete } = vi.hoisted(() => ({
  apiGet: vi.fn(),
  apiPut: vi.fn(),
  apiPost: vi.fn(),
  apiDelete: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    put: apiPut,
    post: apiPost,
    delete: apiDelete,
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
}));

const PAGES = [
  {
    id: 'page-1', key: 'APERCU', title: 'Vue d’ensemble', description: 'Synthèse',
    slug: 'apercu-eglise', layout: 'GRID_2',
    blocks: [{ type: 'KPI', config: { label: 'Âmes', source: 'SOULS_TOTAL' } }],
    roles: ['ADMIN', 'PASTEUR'], enabled: true, published: true, version: 2,
    createdAt: '2026-08-17T10:00:00', updatedAt: '2026-08-17T10:00:00',
  },
  {
    id: 'page-2', key: 'BROUILLON', title: 'Brouillon', description: '',
    slug: 'brouillon', layout: 'STACK', blocks: [], roles: [],
    enabled: true, published: false, version: 1,
    createdAt: '2026-08-17T10:00:00', updatedAt: '2026-08-17T10:00:00',
  },
];

const SOURCES = [
  { key: 'SOULS_TOTAL', label: 'Âmes suivies', type: 'KPI', description: '', sensitive: false },
  { key: 'RECENT_SOULS', label: 'Dernières âmes', type: 'TABLEAU', description: '', sensitive: false },
  { key: 'RECENT_ALERTS', label: 'Alertes récentes', type: 'LISTE', description: '', sensitive: false },
  { key: 'SOULS_BY_STATUT', label: 'Âmes par statut', type: 'GRAPHIQUE', description: '', sensitive: false },
  { key: 'CALENDAR_EVENTS', label: 'Prochains événements', type: 'CALENDRIER', description: '', sensitive: false },
  { key: 'SOULS_TIMELINE', label: 'Dernières âmes', type: 'TIMELINE', description: '', sensitive: false },
];

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderPage() {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PlatformPagesPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function changeInputByLabel(labelText: string, value: string) {
  const label = screen.getByText(labelText);
  const container = label.closest('div');
  const input = container?.querySelector('input, select, textarea') as HTMLInputElement | null;
  if (!input) throw new Error(`Input introuvable pour "${labelText}"`);
  fireEvent.change(input, { target: { value } });
}

describe('PlatformPagesPage — Page Builder', () => {
  beforeEach(() => {
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/pages') return { data: PAGES };
      if (url === '/pages/sources') return { data: SOURCES };
      return { data: [] };
    });
    apiPut.mockResolvedValue({ data: {} });
    apiPost.mockResolvedValue({ data: {} });
    apiDelete.mockResolvedValue({ data: {} });
  });

  it('affiche la liste des pages avec leurs statuts', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Pages personnalisées')).toBeInTheDocument();
    });
    expect(screen.getByText('Vue d’ensemble')).toBeInTheDocument();
    // « Brouillon » apparaît comme titre ET comme badge → au moins un badge.
    expect(screen.getAllByText('Brouillon').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/Publiée · v2/)).toBeInTheDocument();
    expect(screen.getByText('/pages/apercu-eglise')).toBeInTheDocument();
  });

  it('crée une page avec un bloc KPI configuré', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Pages personnalisées')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouvelle page/i }));
    await waitFor(() => {
      expect(screen.getByText('Adresse (/pages/…)')).toBeInTheDocument();
    });

    changeInputByLabel('Titre', 'Ma page');
    changeInputByLabel('Adresse (/pages/…)', 'ma-page');
    changeInputByLabel('Clé (unique, technique)', 'MA_PAGE');
    // Le bloc KPI par défaut est déjà présent.
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/pages', expect.objectContaining({
        title: 'Ma page',
        slug: 'ma-page',
        key: 'MA_PAGE',
        blocks: expect.arrayContaining([
          expect.objectContaining({ type: 'KPI' }),
        ]),
      }));
    });
  });

  it('ajoute un bloc TABLEAU à la page en cours d’édition', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /modifier vue d’ensemble/i }));
    await waitFor(() => {
      expect(screen.getByText(/Modifier la page/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /tableau de données/i }));
    // Deux blocs maintenant (KPI + TABLEAU) ; le sélecteur de source du tableau est visible.
    await waitFor(() => {
      expect(screen.getAllByText('Source de données').length).toBe(2);
    });

    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/pages/page-1', expect.objectContaining({
        blocks: expect.arrayContaining([
          expect.objectContaining({ type: 'TABLEAU', config: expect.objectContaining({ source: 'RECENT_SOULS' }) }),
        ]),
      }));
    });
  });

  it('publie / dépublie une page via le toggle', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble')).toBeInTheDocument();
    });

    // Dépublier la page publiée.
    const toggle = screen.getByRole('switch', { name: /publier vue d’ensemble/i });
    fireEvent.click(toggle);

    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/pages/page-1/publish', { published: false });
    });

    // Publier le brouillon.
    const toggle2 = screen.getByRole('switch', { name: /publier brouillon/i });
    fireEvent.click(toggle2);
    await waitFor(() => {
      expect(apiPost).toHaveBeenCalledWith('/pages/page-2/publish', { published: true });
    });
  });

  it('supprime une page après confirmation', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /supprimer vue d’ensemble/i }));

    await waitFor(() => {
      expect(apiDelete).toHaveBeenCalledWith('/pages/page-1');
    });
    vi.restoreAllMocks();
  });

  it('ajoute un bloc GRAPHIQUE avec source et type de graphique', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /modifier vue d’ensemble/i }));
    await waitFor(() => {
      expect(screen.getByText(/Modifier la page/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Graphique' }));
    // Titre du graphique + source + type de graphique.
    await waitFor(() => {
      expect(screen.getAllByText('Source de données').length).toBe(2);
    });
    expect(screen.getByText('Type de graphique')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/pages/page-1', expect.objectContaining({
        blocks: expect.arrayContaining([
          expect.objectContaining({
            type: 'GRAPHIQUE',
            config: expect.objectContaining({ source: 'SOULS_BY_STATUT', chartType: 'PIE' }),
          }),
        ]),
      }));
    });
  });

  it('configure une checklist avec plusieurs éléments', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Vue d’ensemble')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /modifier vue d’ensemble/i }));
    await waitFor(() => {
      expect(screen.getByText(/Modifier la page/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Checklist' }));
    await waitFor(() => {
      expect(screen.getByText('Éléments à cocher')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: /ajouter un élément/i }));

    fireEvent.click(screen.getByRole('button', { name: 'Enregistrer' }));

    await waitFor(() => {
      expect(apiPut).toHaveBeenCalledWith('/pages/page-1', expect.objectContaining({
        blocks: expect.arrayContaining([
          expect.objectContaining({
            type: 'CHECKLIST',
            config: expect.objectContaining({ items: expect.arrayContaining(['Nouvel élément']) }),
          }),
        ]),
      }));
    });
  });

  it('affiche une erreur quand la création échoue (message serveur)', async () => {
    apiPost.mockRejectedValue({ response: { data: { detail: 'Une page avec cette adresse existe déjà.' } } });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Pages personnalisées')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /nouvelle page/i }));
    await waitFor(() => {
      expect(screen.getByText('Adresse (/pages/…)')).toBeInTheDocument();
    });
    changeInputByLabel('Titre', 'Doublon');
    changeInputByLabel('Adresse (/pages/…)', 'doublon');
    changeInputByLabel('Clé (unique, technique)', 'DOUBLON');
    fireEvent.click(screen.getByRole('button', { name: 'Créer' }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Une page avec cette adresse existe déjà.');
    });
  });
});
