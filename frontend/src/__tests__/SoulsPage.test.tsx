import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SoulsPage from '@/pages/SoulsPage';

const mockGet = vi.fn();

vi.mock('@/lib/api', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: vi.fn().mockResolvedValue({ data: { favorite: true } }),
    patch: vi.fn().mockResolvedValue({ data: {} }),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn().mockReturnValue('Erreur'),
}));

const SOULS = [
  {
    id: 's-1', nom: 'Dupont', prenom: 'Marie', typeDisciple: 'NOUVEAU_CONVERTI',
    statut: 'ACTIF', email: 'marie@test.com', telephone: '+243123',
    dateIntegration: '2026-01-15', dateDernierContact: '2026-08-15',
  },
  {
    id: 's-2', nom: 'Martin', prenom: 'Pierre', typeDisciple: 'NOUVEL_ARRIVANT',
    statut: 'EN_INTEGRATION', email: 'pierre@test.com', telephone: null,
    dateIntegration: '2026-07-01', dateDernierContact: '2026-08-10',
  },
  {
    id: 's-3', nom: 'Lukusa', prenom: 'Grâce', typeDisciple: 'NOUVEAU_CONVERTI',
    statut: 'DECROCHE', email: null, telephone: '+243456',
    dateIntegration: '2025-06-01', dateDernierContact: '2026-03-15',
  },
];

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

function renderWithRoute(route: string) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <SoulsPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

beforeEach(() => {
  queryClient.clear();
  mockGet.mockReset();
  mockGet.mockImplementation(async (url: string) => {
    if (url.startsWith('/favorites/souls')) return { data: [] };
    if (url.startsWith('/dictionaries')) return { data: {} };
    if (url.startsWith('/souls/trash')) {
      return { data: { content: [], totalElements: 0, totalPages: 1, number: 0, size: 20, first: true, last: true } };
    }
    if (url.startsWith('/souls')) {
      return {
        data: {
          content: SOULS,
          totalElements: SOULS.length,
          totalPages: 1,
          number: 0,
          size: 20,
          first: true,
          last: true,
        },
      };
    }
    return { data: { content: [] } };
  });
});

describe('SoulsPage — query params (KPI cliquables des dashboards)', () => {
  it('applique le filtre statut présent dans l\'URL', async () => {
    renderWithRoute('/souls?statut=DECROCHE');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).toContain('statut=DECROCHE');
    });
    expect(screen.getByDisplayValue('Décroché')).toBeInTheDocument();
  });

  it('applique le filtre typeDisciple présent dans l\'URL', async () => {
    renderWithRoute('/souls?typeDisciple=NOUVEAU_CONVERTI');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).toContain('typeDisciple=NOUVEAU_CONVERTI');
    });
    expect(screen.getByDisplayValue('Nouveau converti')).toBeInTheDocument();
  });

  it('applique la recherche présente dans l\'URL', async () => {
    renderWithRoute('/souls?search=jean');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).toContain('search=jean');
    });
    expect(screen.getByDisplayValue('jean')).toBeInTheDocument();
  });

  it('affiche la corbeille quand l\'URL porte view=corbeille', async () => {
    renderWithRoute('/souls?view=corbeille');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls/trash'));
      expect(call).toBeTruthy();
    });
    expect(screen.getByText('Voir les âmes')).toBeInTheDocument();
  });

  it('reste sur la liste quand aucun filtre n\'est présent', async () => {
    renderWithRoute('/souls');

    await waitFor(() => {
      const call = mockGet.mock.calls.find(([url]) => String(url).startsWith('/souls'));
      expect(String(call?.[0])).not.toContain('statut=');
      expect(String(call?.[0])).not.toContain('typeDisciple=');
    });
  });
});

describe('SoulsPage — affichage', () => {
  it('affiche le titre et le compteur d\'âmes', async () => {
    renderWithRoute('/souls');
    await waitFor(() => {
      expect(screen.getByText(/3 âme/)).toBeInTheDocument();
    });
  });

  it('affiche les noms des âmes dans la liste', async () => {
    renderWithRoute('/souls');
    await waitFor(() => {
      expect(screen.getByText('Marie Dupont')).toBeInTheDocument();
    });
    expect(screen.getByText('Pierre Martin')).toBeInTheDocument();
    expect(screen.getByText('Grâce Lukusa')).toBeInTheDocument();
  });

  it('affiche les badges de type disciple', async () => {
    renderWithRoute('/souls');
    await waitFor(() => {
      expect(screen.getAllByText('Nouveau converti').length).toBeGreaterThanOrEqual(1);
    });
    expect(screen.getAllByText('Nouvel arrivant').length).toBeGreaterThanOrEqual(1);
  });

  it('affiche les emails et téléphones', async () => {
    renderWithRoute('/souls');
    await waitFor(() => {
      expect(screen.getByText('marie@test.com')).toBeInTheDocument();
    });
    expect(screen.getByText('+243123')).toBeInTheDocument();
  });

  it('affiche les boutons de navigation (Corbeille, Filtres, Nouvelle âme)', async () => {
    renderWithRoute('/souls');
    await waitFor(() => {
      expect(screen.getByText('Corbeille')).toBeInTheDocument();
    });
    expect(screen.getByText('Filtres')).toBeInTheDocument();
    expect(screen.getByText(/Nouvelle âme/)).toBeInTheDocument();
  });
});

describe('SoulsPage — corbeille', () => {
  it('affiche la corbeille vide', async () => {
    renderWithRoute('/souls?view=corbeille');
    await waitFor(() => {
      expect(screen.getByText('Corbeille vide')).toBeInTheDocument();
    });
    expect(screen.getByText(/Aucune âme supprimée/)).toBeInTheDocument();
  });

  it('affiche le bouton "Voir les âmes" pour revenir à la liste', async () => {
    renderWithRoute('/souls?view=corbeille');
    await waitFor(() => {
      expect(screen.getByText('Voir les âmes')).toBeInTheDocument();
    });
  });
});
