import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TrainingsPage from '@/pages/TrainingsPage';
import { PlatformContext } from '@/contexts/PlatformContext';

// Rôle mutable (vi.mock hoisté) : ADMIN (KPIs) ou MEMBRE (lecture seule).
const { activeRole } = vi.hoisted(() => ({ activeRole: { value: 'ADMIN' } }));
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 'u1', role: activeRole.value, roles: [activeRole.value], activeRole: activeRole.value },
  }),
}));

const { apiGet } = vi.hoisted(() => ({ apiGet: vi.fn() }));

vi.mock('@/lib/api', () => ({
  default: {
    get: apiGet,
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } },
    defaults: { headers: { common: {} } },
  },
  getErrorMessage: vi.fn(() => 'Erreur'),
}));

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

function renderPage(disabledModules: string[] = []) {
  const moduleEnabled = (key: string) => !disabledModules.includes(key);
  return render(
    <QueryClientProvider client={queryClient}>
      <PlatformContext.Provider
        value={{
          menus: [], modules: [], isLoaded: true, moduleEnabled,
          canAccessPath: () => true, refetch: () => undefined,
        }}
      >
        <MemoryRouter initialEntries={['/trainings']}>
          <TrainingsPage />
        </MemoryRouter>
      </PlatformContext.Provider>
    </QueryClientProvider>
  );
}

const STATS = {
  nbCours: 3, nbInscrits: 12, nbCertificats: 4, progressionMoyenne: 67,
  parCategorie: { DISCIPOLAT: 2, MINISTERE: 1 },
  parStatut: { INSCRIT: 5, EN_COURS: 5, TERMINE: 2 },
};

describe('TrainingsPage — outil métier Formations', () => {
  beforeEach(() => {
    activeRole.value = 'ADMIN';
    queryClient.clear();
    vi.clearAllMocks();
    apiGet.mockImplementation(async (url: string) => {
      if (url === '/trainings/stats') return { data: STATS };
      if (url === '/trainings/courses') return { data: [] };
      if (url === '/trainings/my-enrollments') return { data: [] };
      if (url === '/trainings/my-certificates') return { data: [] };
      return { data: [] };
    });
  });

  it('affiche les KPIs réels de la formation pour un ADMIN', async () => {
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Progression moyenne')).toBeInTheDocument();
    });

    // Valeurs issues de GET /trainings/stats (aucune donnée fictive).
    expect(screen.getByText('3')).toBeInTheDocument(); // cours
    expect(screen.getByText('12')).toBeInTheDocument(); // inscrits
    expect(screen.getByText('4')).toBeInTheDocument(); // certificats
    expect(screen.getByText('67%')).toBeInTheDocument(); // progression moyenne
    expect(screen.getByText('2 terminé(s)')).toBeInTheDocument();
  });

  it('ne charge pas les stats pour un rôle non administrateur', async () => {
    activeRole.value = 'MEMBRE';
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Formations')).toBeInTheDocument();
    });
    expect(apiGet).not.toHaveBeenCalledWith('/trainings/stats');
    expect(screen.queryByText('Progression moyenne')).not.toBeInTheDocument();
  });

  it('affiche l’état explicite quand le module est désactivé', async () => {
    renderPage(['TRAININGS']);

    await waitFor(() => {
      expect(screen.getByText('Module Formations désactivé')).toBeInTheDocument();
    });
    // Aucun appel API formations (le garde-fou serveur renverrait 403 de toute façon).
    expect(apiGet).not.toHaveBeenCalled();
  });
});
